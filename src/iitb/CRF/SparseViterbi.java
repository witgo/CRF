package iitb.CRF;

import gnu.trove.*;
import cern.colt.function.*;
import cern.colt.list.*;
import cern.colt.matrix.impl.*;
/**
 *
 * Viterbi search
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SparseViterbi extends Viterbi {
    protected SparseViterbi(CRF model, int bs) {
        super(model,bs);
    }
    protected class Context extends DenseObjectMatrix1D {
        protected int pos;
        protected int beamsize;
       
        protected Context(int numY, int beamsize, int pos){
            super(numY);
            this.pos = pos;
            this.beamsize = beamsize;
        }
        protected Entry newEntry(int beamsize, int label, int pos) {
            return new Entry(beamsize,label,pos);
        }
        public void add(int y, Entry prevEntry, float thisScore) {
            if (getQuick(y) == null) {
                    setQuick(y, newEntry((pos==0)?1:beamsize, y, pos));
            }
            getEntry(y).valid = true;
            getEntry(y).add(prevEntry,thisScore);
        }
        public void clear() {
//            assign((Object)null);
            for (int i = 0; i < size(); i++)
                if (getQuick(i) != null)
                    getEntry(i).clear();
        }
        public Entry getEntry(int y) {return (Entry)getQuick(y);}
        /**
         * @param y
         * @return
         */
        public boolean entryNotNull(int y) {
            return ((getQuick(y) != null) && getEntry(y).valid);
        }
        void assign(LogSparseDoubleMatrix1D Ri) {
            for (int y = 0; y < Ri.size(); y++) {
        	  if (Ri.getQuick(y) != 0) 
        	      add(y,null,(float)Ri.get(y));
            }
        }	
    };
    
    protected Context context[];
    //    SparseDoubleMatrix2D Mi;
    protected LogSparseDoubleMatrix1D Ri;
    ObjectArrayList prevContext = new ObjectArrayList();
    IntArrayList validYs = new IntArrayList();
    IntArrayList validPrevYs  = new IntArrayList();
    DoubleArrayList values = new DoubleArrayList();
    
    protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        model.featureGenerator.startScanFeaturesAt(dataSeq, i);
        SparseTrainer.computeLogMi(model.featureGenerator,lambda,Mi,Ri);
    }
    protected class Iter {
        protected int ell;
        protected void start(int i, DataSequence dataSeq) {ell = 1;}
        protected int nextEll(int i) {return ell--;}
    }
    protected Iter getIter(){return new Iter();}
    protected void finishContext(int i2) {;}
    /**
     * @return
     */
    protected double getCorrectScore(DataSequence dataSeq, int i, int ell) {
    	return	(Ri.getQuick(dataSeq.y(i)) + ((i > 0)?Mi.get(dataSeq.y(i-1),dataSeq.y(i)):0));
    }
    protected class ContextUpdate implements IntIntDoubleFunction, IntDoubleFunction {
        protected int i, ell;
        protected Iter iter;
        public double apply(int yp, int yi, double val) {
            if (context[i-ell].entryNotNull(yp))
                context[i].add(yi, context[i-ell].getEntry(yp),(float)(Mi.get(yp,yi)+Ri.get(yi)));
            return val;
        }
        public double apply(int yi, double val) {
            context[i].add(yi,null,(float)Ri.get(yi));
            return val;
        }
        double fillArray(DataSequence dataSeq, double lambda[], boolean calcScore) {
            double corrScore = 0;
            for (i = 0; i < dataSeq.length(); i++) {
                context[i].clear();
                for (iter.start(i,dataSeq); (ell = iter.nextEll(i)) > 0;) {
                    // compute Mi.
                    // i - ell = i'
                    computeLogMi(dataSeq, i, ell, lambda);
                    if (i - ell < 0) {
                        Ri.forEachNonZero(this);
                    } else {
                        Mi.forEachNonZero(this);
                    }
                    
                    if (model.params.debugLvl > 1) {
                        System.out.println("Ri "+Ri);
                        System.out.println("Mi "+ Mi);
                    }
                    
                    if (calcScore) {
                    	corrScore += getCorrectScore(dataSeq, i, ell);
                    }
                }
                finishContext(i);
            }
            i = dataSeq.length();
            context[i].clear();
            if (i >= 1) {
                for (int yp = 0; yp < context[i-1].size(); yp++) {
                    if (context[i-1].entryNotNull(yp))
                        context[i].add(0, context[i-1].getEntry(yp),0);
                }
            }
            return corrScore;
        }
        
    };    
    protected ContextUpdate contextUpdate;
    protected ContextUpdate newContextUpdate() {
        return new ContextUpdate();
    }
    protected void allocateScratch(int numY) {
        Mi = new LogSparseDoubleMatrix2D(numY,numY);
        Ri = new LogSparseDoubleMatrix1D(numY);
        context = new Context[0];
        finalSoln = new Entry(beamsize,0,0);
        contextUpdate = newContextUpdate();
        contextUpdate.iter = getIter();
    }
    protected Context newContext(int numY, int beamsize, int pos){
        return new Context(numY,beamsize,pos);        
    }
    public double viterbiSearch(DataSequence dataSeq, double lambda[], boolean calcCorrectScore) {
        if (Mi == null) {
            allocateScratch(model.numY);
        }
        if (context.length < dataSeq.length()+1) {
            Context oldContext[] = context;
            context = new Context[dataSeq.length()+1];
            for (int l = 0; l < oldContext.length; l++) {
                context[l] = oldContext[l];
            }
            for (int l = oldContext.length; l < context.length; l++) {
                context[l] = newContext(model.numY,beamsize,l);
            }
        }
        double corrScore = contextUpdate.fillArray(dataSeq, lambda,calcCorrectScore);
       /* finalSoln.clear();
        finalSoln.valid = true;
        int i = dataSeq.length()-1;
        if (i >= 0) {
            for (int y = 0; y < context[i].size(); y++) {
                if (context[i].entryNotNull(y))
                    finalSoln.add((Entry)context[i].getQuick(y),0);
            }
        }
        */
        finalSoln = (Entry)context[dataSeq.length()].getQuick(0);
        if (model.params.debugLvl > 1) {
            System.out.println("Score of best sequence "+finalSoln.get(0).score + " corrScore " + corrScore);
        }
        return corrScore;
    }
};
