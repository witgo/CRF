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

class SparseViterbi extends Viterbi {
    SparseViterbi(CRF model, int bs) {
        super(model,bs);
    }
    class Context extends DenseObjectMatrix1D {
        int pos;
        int beamsize;
       
        Context(int numY, int beamsize, int pos){
            super(numY);
            this.pos = pos;
            this.beamsize = beamsize;
        }
        void add(int y, Entry prevEntry, float thisScore) {
            if (getQuick(y) == null) {
                    setQuick(y, new Entry((pos==0)?1:beamsize, y, pos));
            }
            getEntry(y).valid = true;
            getEntry(y).add(prevEntry,thisScore);
        }
        void clear() {
//            assign((Object)null);
            for (int i = 0; i < size(); i++)
                if (getQuick(i) != null)
                    getEntry(i).clear();
        }
        Entry getEntry(int y) {return (Entry)getQuick(y);}
        /**
         * @param y
         * @return
         */
        public boolean entryNotNull(int y) {
            return ((getQuick(y) != null) && getEntry(y).valid);
        }
    };
    
    Context context[];
    Entry finalSoln;
    //    SparseDoubleMatrix2D Mi;
    LogSparseDoubleMatrix1D Ri;
    ObjectArrayList prevContext = new ObjectArrayList();
    IntArrayList validYs = new IntArrayList();
    IntArrayList validPrevYs  = new IntArrayList();
    DoubleArrayList values = new DoubleArrayList();
    
    void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        model.featureGenerator.startScanFeaturesAt(dataSeq, i);
        SparseTrainer.computeLogMi(model.featureGenerator,lambda,Mi,Ri);
    }
    class Iter {
        int ell;
        void start(int i, DataSequence dataSeq) {ell = 1;}
        int nextEll(int i) {return ell--;}
    }
    Iter getIter(){return new Iter();}
    /**
     * @return
     */
    protected double getCorrectScore(DataSequence dataSeq, int i, int ell) {
    	return	(Ri.getQuick(dataSeq.y(i)) + ((i > 0)?Mi.get(dataSeq.y(i-1),dataSeq.y(i)):0));
    }
    class ContextUpdate implements IntIntDoubleFunction, IntDoubleFunction {
        int i, ell;
        Iter iter;
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
            }
            return corrScore;
        }
    };    
	ContextUpdate contextUpdate;
    void allocateScratch(int numY) {
        Mi = new LogSparseDoubleMatrix2D(numY,numY);
        Ri = new LogSparseDoubleMatrix1D(numY);
        context = new Context[0];
        finalSoln = new Entry(beamsize,0,0);
        contextUpdate = new ContextUpdate();
        contextUpdate.iter = getIter();
    }
    Context newContext(int numY, int beamsize, int pos){
        return new Context(numY,beamsize,pos);        
    }
    public double viterbiSearch(DataSequence dataSeq, double lambda[], boolean calcCorrectScore) {
        if (Mi == null) {
            allocateScratch(model.numY);
        }
        if (context.length < dataSeq.length()) {
            Context oldContext[] = context;
            context = new Context[dataSeq.length()];
            for (int l = 0; l < oldContext.length; l++) {
                context[l] = oldContext[l];
            }
            for (int l = oldContext.length; l < context.length; l++) {
                context[l] = newContext(model.numY,beamsize,l);
            }
        }
        double corrScore = contextUpdate.fillArray(dataSeq, lambda,calcCorrectScore);
        
        finalSoln.clear();
        finalSoln.valid = true;
        int i = dataSeq.length()-1;
        if (i >= 0) {
/*            context[i].getNonZeros(validPrevYs, prevContext);
            for (int prevPx = 0; prevPx < validPrevYs.size(); prevPx++) {
                finalSoln.add((Entry)prevContext.getQuick(prevPx),0);
            }
            */
            for (int y = 0; y < context[i].size(); y++) {
                if (context[i].entryNotNull(y))
                    finalSoln.add((Entry)context[i].getQuick(y),0);
            }
        }
        if (model.params.debugLvl > 1) {
            System.out.println("Score of best sequence "+finalSoln.get(0).score + " corrScore " + corrScore);
        }
        return corrScore;
    }
};
