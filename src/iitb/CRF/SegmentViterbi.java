package iitb.CRF;

import java.util.Iterator;

import iitb.CRF.SparseViterbi.Context;
import iitb.CRF.SparseViterbi.Entry;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntProcedure;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SegmentViterbi extends SparseViterbi {
    SegmentCRF segmentModel;
    static class LabelConstraints  {
        private static final long serialVersionUID = 1L;
        ConstraintDisallowedPairs disallowedPairs;
        class Intersects implements TIntProcedure {
            int label;
            int prevLabel;
            public boolean execute(int arg0) {
                return !disallowedPairs.conflictingPair(label,arg0,(arg0==prevLabel));
            }
        }
        Intersects intersectTest = new Intersects();
        /**
         * @param pairs
         */
        public LabelConstraints(ConstraintDisallowedPairs pairs) {
            disallowedPairs = pairs;
        }
        /**
         * @param set
         * @param prevLabel
         * @param i
         * @return
         */
        private boolean valid(TIntHashSet set, int label, int prevLabel) {
            if (!conflicting(label))
                return true;
             if (disallowedPairs.conflictingPair(label,prevLabel,true))
                 return false;
             intersectTest.label = label;
             intersectTest.prevLabel = prevLabel;
             return set.forEach(intersectTest);
        }
        /**
         * @param dataSeq
         * @return
         */
        public static LabelConstraints checkConstraints(CandSegDataSequence dataSeq, LabelConstraints labelCons) {
            Iterator constraints = dataSeq.constraints(-1,dataSeq.length());
            if (constraints != null) {
    			for (; constraints.hasNext();) {
    				Constraint constraint = (Constraint)constraints.next();
    				if (constraint.type() == Constraint.PAIR_DISALLOW) {
    				    if (labelCons != null) {
    				        labelCons.disallowedPairs = (ConstraintDisallowedPairs)constraint;
    				        return labelCons;
    				    } else
    				        return new LabelConstraints((ConstraintDisallowedPairs)constraint);
    				}
    			}
            }
            return null;
        }
        /**
         * @param label
         * @return
         */
        public boolean conflicting(int label) {
            return disallowedPairs.conflicting(label);
        }
    }
    LabelConstraints labelConstraints=null;
    class SolnWithLabelsOnPath extends Soln {
        void clear() {
            super.clear();
            labelsOnPath.clear();
        }
        void copy(Soln soln) {
            super.copy(soln);
            labelsOnPath.clear();
            labelsOnPath.addAll(((SolnWithLabelsOnPath)soln).labelsOnPath.toArray());
        }
        private static final long serialVersionUID = 1L;
        TIntHashSet labelsOnPath;
        /**
         * @param id
         * @param p
         */
        SolnWithLabelsOnPath(int id, int p) {
            super(id, p);
            labelsOnPath = new TIntHashSet();
        }
        protected void setPrevSoln(Soln prevSoln, double score) {
            super.setPrevSoln(prevSoln,score);
            if ((prevSoln != null) && (labelConstraints != null)) {
                labelsOnPath.clear();
            	labelsOnPath.addAll(((SolnWithLabelsOnPath)prevSoln).labelsOnPath.toArray());
            	assert(labelConstraints.valid(labelsOnPath,label,prevSoln.label));
            	if (labelConstraints.conflicting(prevSoln.label))
            		labelsOnPath.add(prevSoln.label);
            }
        }       
    }
    class EntryForLabelConstraints extends Entry {
        /**
         * @param beamsize
         * @param id
         * @param pos
         */
        EntryForLabelConstraints(int beamsize, int id, int pos) {
            super();
            solns = new Soln[beamsize];
            for (int i = 0; i < solns.length; i++)
                solns[i] = new SolnWithLabelsOnPath(id, pos);
        }
        int findInsert(int insertPos, double score, Soln prev) {
            for (; insertPos < size(); insertPos++) {
                if (score >= get(insertPos).score) {
                    if ((prev == null) || labelConstraints.valid(((SolnWithLabelsOnPath)prev).labelsOnPath,get(insertPos).label, prev.label)) {
                        insert(insertPos, score, prev);
                        insertPos++;
                    }
                    break;
                }
            }
            return insertPos;
        }
    }
    class ContextForLabelConstraints extends Context {
        ContextForLabelConstraints(int numY, int beamsize, int pos) {
            super(numY, beamsize, pos);
        }
        private static final long serialVersionUID = 1L;
        void add(int y, Entry prevSoln, double thisScore) {
            if (labelConstraints==null) {
                super.add(y,prevSoln,thisScore);
            } else {
                if (getQuick(y) == null) {
                    setQuick(y, new EntryForLabelConstraints((pos==0)?1:beamsize, y, pos)); 
                }
                getEntry(y).add(prevSoln,thisScore);
            }
        }
    }
    SegmentViterbi(SegmentCRF nestedModel, int bs) {
        super(nestedModel, bs);
        this.segmentModel = nestedModel;
    }
    void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        SegmentTrainer.computeLogMi((CandSegDataSequence)dataSeq,i-ell,i,segmentModel.featureGenNested,lambda,Mi,Ri);
    }
    class SegmentIter extends Iter {
        int nc;
        CandidateSegments candidateSegs;
        void start(int i, DataSequence dataSeq) {
            candidateSegs = (CandidateSegments)dataSeq;
            nc = candidateSegs.numCandSegmentsEndingAt(i);
        }
        int nextEll(int i) {
            nc--;
            if (nc >= 0)
                return i -  candidateSegs.candSegmentStart(i,nc) + 1;
            return -1;
        }
    }	
    Iter getIter(){return new SegmentIter();}
    /**
     * @return
     */
    int prevSegEnd = -1;
    protected double getCorrectScore(DataSequence dataSeq, int i, int ell) {
    	SegmentDataSequence data = (SegmentDataSequence)dataSeq;
    	if (data.getSegmentEnd(i-ell+1) != i)
    		return 0;
    	if ((i - ell >= 0) && (prevSegEnd != i-ell))
    		return RobustMath.LOG0;
    	prevSegEnd = i;
    	if ((labelConstraints != null) && labelConstraints.conflicting(data.y(i))) {
    		for (int segStart = 0; segStart < i-ell+1; segStart = data.getSegmentEnd(segStart)+1) {
    			int segEnd = data.getSegmentEnd(segStart);
    			if (labelConstraints.disallowedPairs.conflictingPair(data.y(i),data.y(segStart),segEnd==i-ell))
    				return RobustMath.LOG0;
    		}
    	}
    	return	(Ri.getQuick(dataSeq.y(i)) + ((i-ell >= 0)?Mi.get(dataSeq.y(i-ell),dataSeq.y(i)):0));
    }
    public void bestLabelSequence(CandSegDataSequence dataSeq, double lambda[]) {
        viterbiSearch(dataSeq, lambda,false);
        Soln ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        while (ybest != null) {
            dataSeq.setSegment(ybest.prevPos()+1,ybest.pos,ybest.label);
            ybest = ybest.prevSoln;
        }
    }
    Context newContext(int numY, int beamsize, int pos){
        if (labelConstraints == null)
            return new Context(numY,beamsize,pos);        
        return  new ContextForLabelConstraints(numY,(beamsize==1)?10:beamsize,pos); 
    }
    public double viterbiSearch(DataSequence dataSeq, double[] lambda,
            boolean calcCorrectScore) {
        labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        return super.viterbiSearch(dataSeq, lambda, calcCorrectScore);
    }
};
