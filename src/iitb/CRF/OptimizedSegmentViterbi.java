/*
 * Created on Mar 9, 2005
 *
 */
package iitb.CRF;

import iitb.CRF.Viterbi.Entry;

import java.util.Vector;

import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseObjectMatrix2D;


/**
 * @author sunita
 *
 */
public class OptimizedSegmentViterbi extends SegmentViterbi {
    public interface FeatureLUB extends Feature {
        float valueUB();  /** upper bound on value -- for expensive features **/
        float valueLB();  /** lower bound on value when value is not ready **/
    };
    public interface FeatureGeneratorLUB extends FeatureGeneratorNested {
        FeatureLUB nextLUBFeature();
        void refineFeature(FeatureLUB f);
        //void refineFeatures(DataSequence data, int prevPos, int pos);
    };
    /**
     * @param nestedModel
     * @param bs
     */
    SparseDoubleMatrix2D MiUB;
    LogSparseDoubleMatrix1D RiUB;
    ObjectMatrix2D incompleteFeaturesM;
    ObjectMatrix1D incompleteFeaturesR;
    
    public OptimizedSegmentViterbi(SegmentCRF nestedModel, int bs) {
        super(nestedModel, bs);
    }
    void allocateScratch(int numY) {
        super.allocateScratch(numY);
        MiUB = new LogSparseDoubleMatrix2D(numY,numY);
        RiUB = new LogSparseDoubleMatrix1D(numY);
        finalSoln = new EntryLUB(beamsize,0,0);
        contextUpdate = new ContextUpdateLUB();
        contextUpdate.iter = getIter();
        incompleteFeaturesM = new SparseObjectMatrix2D(numY,numY);
        incompleteFeaturesR = new DenseObjectMatrix1D(numY);
    }
    class SolnLUB extends Soln {
        Vector incompleteFeatures;
        SolnLUB(int id, int p) {
            super(id, p);
        }
    };
    class EntryLUB extends Entry {
        Vector<Soln> ubSolns = new Vector<Soln>();
        void clear() {
            super.clear();
            ubSolns.clear();
        }
        EntryLUB(int beamsize, int id, int pos) {
            super(beamsize, id,pos);
        }
        void add(EntryLUB e, float thisScoreLB, float thisScoreUB
                , Vector incompleteFeaturesM, Vector incompleteFeaturesR) {
            super.add(e,thisScoreLB);
            float topKLB = solns[solns.length-1].score;
            if (e == null) {
                if (thisScoreUB > topKLB) {
                    // TODO -- insert ub solns 
                    // make sure first node of a path gets correctly inserted.
                    add(thisScoreUB);
                }
                return;
            }
            for (int i = 0; (i < e.ubSolns.size()); i++) {
                float score = e.ubSolns.get(i).score + thisScoreUB;
                if (score > topKLB) {
                    SolnLUB newSoln = new SolnLUB(get(0).label, get(0).pos);
                    newSoln.setPrevSoln(e.ubSolns.get(i), score);
                    if ((incompleteFeaturesM  != null) || (incompleteFeaturesR != null)) {
                        newSoln.incompleteFeatures = new Vector();
                        if (incompleteFeaturesM != null)
                            newSoln.incompleteFeatures.addAll(incompleteFeaturesM);
                        if (incompleteFeaturesR != null)
                            newSoln.incompleteFeatures.addAll(incompleteFeaturesR);
                    }
                    ubSolns.add(newSoln);
                }
            }
        }
        void prune(int maxNumberUBs) {
            // TODO -- remove solns from ubSolns whose score < topKLB
            // if ubSolns >= maxNumberUBs -- refine
        }
    }
    
    class ContextUpdateLUB extends ContextUpdate {
        public double apply(int yp, int yi, double val) {
            if (context[i-ell].entryNotNull(yp))
                ((ContextLUB)context[i]).add(yi, context[i-ell].getEntry(yp),(float)(Mi.get(yp,yi)+Ri.get(yi)), 
                        (float)(MiUB.get(yp,yi)+RiUB.get(yi)),
                        (Vector)incompleteFeaturesM.get(yp,yi), (Vector)incompleteFeaturesR.get(yi));
            return val;
        }
        public double apply(int yi, double val) {
            ((ContextLUB)context[i]).add(yi,null,(float)Ri.get(yi), (float) RiUB.get(yi)
                    , null, (Vector)incompleteFeaturesR.get(yi));
            return val;
        } 
        // TODO -- decide when to prune and how much.
    };
    class ContextLUB extends Context {
        ContextLUB(int numY, int beamsize, int pos){
            super(numY,beamsize,pos);
        }
        void add(int y, Entry prevEntry, float thisScoreLB, float thisScoreUB, 
                Vector featuresM, Vector featuresR) {
            if (getQuick(y) == null) {
                setQuick(y, new EntryLUB((pos==0)?1:beamsize, y, pos));
            }
            getEntry(y).valid = true;
            ((EntryLUB)getEntry(y)).add((EntryLUB)prevEntry,thisScoreLB, thisScoreUB, featuresM, featuresR);
        }
    };
    Context newContext(int numY, int beamsize, int pos){
        return  new ContextLUB(numY,(beamsize==1)?20:beamsize,pos); 
    }
    void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        double defaultValue = SegmentTrainer.initLogMi((CandSegDataSequence)dataSeq,i-ell,i,
                segmentModel.featureGenNested,lambda,Mi,Ri);
        MiUB.assign(Mi);
        RiUB.assign(Ri);
        
        FeatureGeneratorLUB featureGen = (FeatureGeneratorLUB)segmentModel.featureGenNested;
        while (featureGen.hasNext()) { 
            FeatureLUB feature = featureGen.nextLUBFeature();
            int f = feature.index();
            int yp = feature.y();
            int yprev = feature.yprev();
            float val = feature.valueLB();
            float valUB = feature.valueUB();
            if ((val != valUB) && (Math.abs(lambda[f]) > Double.MIN_VALUE)) {
                if (yprev == -1) {
                    // TODO -- allocate a new vector if null and add feature.
                  //  incompleteFeaturesR.set(yp,feature.clone());
                } else {
                    // incompleteFeaturesM.set(yprev,yp,feature.clone());
                }
            }
            if (yprev == -1) {
                // this is a single state feature.
                // if default value was a negative_infinity, need to
                // reset to.
                // TODO --- look at sign of lambda and do the right thing
                double oldVal = Ri.get(yp);
                if (oldVal == defaultValue)
                    oldVal = 0;
                Ri.set(yp,oldVal+lambda[f]*val);
                RiUB.set(yp,oldVal+lambda[f]*valUB);
            } else {
                double oldVal = Mi.get(yprev,yp);
                if (oldVal == defaultValue) {
                    oldVal = 0;
                    if (Ri.get(yp) == defaultValue) {
                        Ri.set(yp,0);
                        RiUB.set(yp,0);
                    }
                }
                Mi.set(yprev,yp,oldVal+lambda[f]*val);
                MiUB.set(yprev,yp,oldVal+lambda[f]*valUB);
            }
        }
    }
    public double viterbiSearch(DataSequence dataSeq, double[] lambda,
            boolean calcCorrectScore) {
        double corrScore =  super.viterbiSearch(dataSeq, lambda, calcCorrectScore);
        // TODO -- do pruning where needed on finalSoln and retain top-K solutions.
        return corrScore;
    }
};
