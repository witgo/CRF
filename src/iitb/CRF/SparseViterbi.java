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
    CRF model;
    int beamsize;
    
  
    SparseViterbi(CRF model, int bs) {
        super(model,bs);
        this.model = model;
        beamsize=bs;
    }
    class Entry {
        Soln solns[];
        Entry() {}
        Entry(int beamsize, int id, int pos) {
            solns = new Soln[beamsize];
            for (int i = 0; i < solns.length; i++)
                solns[i] = new Soln(id, pos);
        }
        void clear() {
            for (int i = 0; i < solns.length; i++)
                solns[i].clear();
        }
        int size() {return solns.length;}
        Soln get(int i) {return solns[i];}
        void insert(int i, double score, Soln prev) {
            for (int k = size()-1; k > i; k--) {
                solns[k].copy(solns[k-1]);
            }
            solns[i].setPrevSoln(prev,score);
        }
        void add(Entry e, double thisScore) {
            if (e == null) {
                add(thisScore);
                return;
            }
            int insertPos = 0;
            for (int i = 0; (i < e.size()) && (insertPos < size()); i++) {
                double score = e.get(i).score + thisScore;
                insertPos = findInsert(insertPos, score, e.get(i));
            }
            //	    print();
        }
        int findInsert(int insertPos, double score, Soln prev) {
            for (; insertPos < size(); insertPos++) {
                if (score >= get(insertPos).score) {
                    insert(insertPos, score, prev);
                    insertPos++;
                    break;
                }
            }
            return insertPos;
        }
        void add(double thisScore) {
            findInsert(0, thisScore, null);
        }
        int numSolns() {
            for (int i = 0; i < solns.length; i++)
                if (solns[i].isClear())
                    return i+1;
            return size();
        }
        void print() {
            String str = "";
            for (int i = 0; i < size(); i++)
                str += ("["+i + " " + solns[i].score + " i:" + solns[i].pos + " y:" + solns[i].label+"]");
            System.out.println(str);
        }
    };
 
    class Context extends SparseObjectMatrix1D {
        int pos;
        int beamsize;
        Context(int numY, int beamsize, int pos){
            super(numY);
            this.pos = pos;
            this.beamsize = beamsize;
        }
        void add(int y, Entry prevSoln, double thisScore) {
            if (getQuick(y) == null) {
                    setQuick(y, new Entry((pos==0)?1:beamsize, y, pos));
            }
            getEntry(y).add(prevSoln,thisScore);
        }
        void clear() {
            // TODO -- save allocated Entry class.
            assign((Object)null);
        }
        Entry getEntry(int y) {return (Entry)getQuick(y);}
    };
    
    Context context[];
    Entry finalSoln;
    SparseDoubleMatrix2D Mi;
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
    class ContextUpdate implements IntIntDoubleFunction, IntDoubleFunction {
        int i, ell;
        Iter iter;
        public double apply(int yp, int yi, double val) {
            if (context[i-ell].getEntry(yp) != null)
                context[i].add(yi, context[i-ell].getEntry(yp),Mi.get(yp,yi)+Ri.get(yi));
            return val;
        }
        public double apply(int yi, double val) {
            context[i].add(yi,null,Ri.get(yi));
            return val;
        }
        double fillArray(DataSequence dataSeq, double lambda[], boolean calcScore) {
            double corrScore = 0;
            for (i = 0; i < dataSeq.length(); i++) {
                context[i].clear();
                for (iter.start(i,dataSeq); (ell = iter.nextEll(i)) > 0;) {
                    // compute Mi.
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
                }
            if (calcScore)
                corrScore += (Ri.getQuick(dataSeq.y(i)) + ((i > 0)?Mi.get(dataSeq.y(i-1),dataSeq.y(i)):0));
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
    public void bestLabelSequence(DataSequence dataSeq, double lambda[]) {
        double corrScore = viterbiSearch(dataSeq, lambda,false);
        System.out.println("Correct score " + corrScore);
        Soln ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        int pos=-1;
        while (ybest != null) {
            pos = ybest.pos;
            dataSeq.set_y(ybest.pos, ybest.label);
            ybest = ybest.prevSoln;
        }
        assert(pos==0);
    }
    Context newContext(int numY, int beamsize, int pos){
        return new Context(numY,beamsize,pos);        
    }
    public double viterbiSearch(DataSequence dataSeq, double lambda[], boolean calcCorrectScore) {
        if (Mi == null) {
            allocateScratch(model.numY);
        }
        if (context.length < dataSeq.length()) {
            context = new Context[dataSeq.length()];
            
            for (int l = 0; l < context.length; l++) {
                context[l] = newContext(model.numY,beamsize,l);
            }
        }
        double corrScore = contextUpdate.fillArray(dataSeq, lambda,calcCorrectScore);
        
        finalSoln.clear();
        int i = dataSeq.length()-1;
        if (i >= 0) {
            context[i].getNonZeros(validPrevYs, prevContext);
            for (int prevPx = 0; prevPx < validPrevYs.size(); prevPx++) {
                finalSoln.add((Entry)prevContext.getQuick(prevPx),0);
            }
        }
        if (model.params.debugLvl > 1) {
            System.out.println("Score of best sequence "+finalSoln.get(0).score + " corrScore " + corrScore);
        }
        return corrScore;
    }
    int numSolutions() {return finalSoln.numSolns();}
    Soln getBestSoln(int k) {
        return finalSoln.get(k).prevSoln;
    }
};
