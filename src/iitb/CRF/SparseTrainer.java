package iitb.CRF;

import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SparseTrainer extends Trainer {
    boolean logTrainer;
    static class  ExpFunc implements DoubleFunction {
        public double apply(double a) {return Math.exp(a);}
    };
    static class ExpFunc2D implements IntIntDoubleFunction {
        public double apply(int first, int second, double third) {
            return Math.exp(third);
        }
    };
    static class ExpFunc1D implements IntDoubleFunction {
        public double apply(int first, double third) {
            return Math.exp(third);
        }
    };
    
    static ExpFunc expFunc = new ExpFunc(); 
    static IntDoubleFunction expFunc1D = new ExpFunc1D();
    static IntIntDoubleFunction expFunc2D = new ExpFunc2D();
    

    
    public SparseTrainer(CrfParams p) {
        super(p);
        params = p;
        logTrainer = params.trainerType.equals("ll");
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval) {
        init(model,data,l);
        evaluator = eval;
        if (params.debugLvl > 0) {
            Util.printDbg("Number of features :" + lambda.length);	    
        }
        doTrain();
    }
    void initMatrices() {        
        if (!logTrainer) {
            Mi_YY = new SparseDoubleMatrix2D(numY,numY);
            Ri_Y = new SparseDoubleMatrix1D(numY);
            alpha_Y = new SparseDoubleMatrix1D(numY);
            newAlpha_Y = new SparseDoubleMatrix1D(numY);
            tmp_Y = new SparseDoubleMatrix1D(numY);
        } else {
            Mi_YY = new LogSparseDoubleMatrix2D(numY,numY);
            Ri_Y = new LogSparseDoubleMatrix1D(numY);
            alpha_Y = new LogSparseDoubleMatrix1D(numY);
            newAlpha_Y = new LogSparseDoubleMatrix1D(numY);
            tmp_Y = new LogSparseDoubleMatrix1D(numY);
            
        }
    }

    
    protected double computeFunctionGradient(double lambda[], double grad[]) {
        if (params.trainerType.equals("ll"))
            return computeFunctionGradientLL(lambda,  grad);
        double logli = 0;
        try {
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*lambda[f]*params.invSigmaSquare;
                logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
            }
            boolean doScaling = params.doScaling;
            
            diter.startScan();
            for (int numRecord = 0; diter.hasNext(); numRecord++) {
                DataSequence dataSeq = (DataSequence)diter.next();
                if (params.debugLvl > 1) {
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                }
                alpha_Y.assign(1);
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = 0;
                
                if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
                    beta_Y = new DoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < beta_Y.length; i++)
                        beta_Y[i] = new SparseDoubleMatrix1D(numY);
                    
                    scale = new double[2*dataSeq.length()];
                }
                // compute beta values in a backward scan.
                // also scale beta-values to 1 to avoid numerical problems.
                scale[dataSeq.length()-1] = (doScaling)?numY:1;
                beta_Y[dataSeq.length()-1].assign(1.0/scale[dataSeq.length()-1]);
                for (int i = dataSeq.length()-1; i > 0; i--) {
                    if (params.debugLvl > 2) {
                        Util.printDbg("Features fired");
                        //featureGenerator.startScanFeaturesAt(dataSeq, i);    
                        //while (featureGenerator.hasNext()) { 
                        //Feature feature = featureGenerator.next();
                        //Util.printDbg(feature.toString());
                        //}
                    }
                    
                    // compute the Mi matrix
                    computeMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y);
                    tmp_Y.assign(beta_Y[i]);
                    tmp_Y.assign(Ri_Y,multFunc);
                    // RobustMath.Mult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
                    Mi_YY.zMult(tmp_Y, beta_Y[i-1]);
                    
                    // need to scale the beta-s to avoid overflow
                    scale[i-1] = doScaling?beta_Y[i-1].zSum():1;
                    if ((scale[i-1] < 1) && (scale[i-1] > -1))
                        scale[i-1] = 1;
                    constMultiplier.multiplicator = 1.0/scale[i-1];
                    beta_Y[i-1].assign(constMultiplier);
                }
                
                double thisSeqLogli = 0;
                for (int i = 0; i < dataSeq.length(); i++) {
                    // compute the Mi matrix
                    computeMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y);
                    // find features that fire at this position..
                    featureGenerator.startScanFeaturesAt(dataSeq, i);
                    
                    if (i > 0) {
                        //		    tmp_Y.assign(alpha_Y);
                        //		    RobustMath.Mult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                        Mi_YY.zMult(alpha_Y, newAlpha_Y,1,0,true);
                        newAlpha_Y.assign(Ri_Y,multFunc); 
                    } else {
                        newAlpha_Y.assign(Ri_Y);     
                    }
                    while (featureGenerator.hasNext()) { 
                        Feature feature = featureGenerator.next();
                        int f = feature.index();
                        
                        int yp = feature.y();
                        int yprev = feature.yprev();
                        float val = feature.value();
                        if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                            grad[f] += val;
                            thisSeqLogli += val*lambda[f];
                        }
                        if (yprev < 0) {
                            ExpF[f] += newAlpha_Y.get(yp)*val*beta_Y[i].get(yp);
                        } else {
                            ExpF[f] += alpha_Y.get(yprev)*Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val*beta_Y[i].get(yp);
                        }
                    }
                    
                    alpha_Y.assign(newAlpha_Y);
                    // now scale the alpha-s to avoid overflow problems.
                    constMultiplier.multiplicator = 1.0/scale[i];
                    alpha_Y.assign(constMultiplier);
                    
                    if (params.debugLvl > 2) {
                        System.out.println("Alpha-i " + alpha_Y.toString());
                        System.out.println("Ri " + Ri_Y.toString());
                        System.out.println("Mi " + Mi_YY.toString());
                        System.out.println("Beta-i " + beta_Y[i].toString());
                    }
                    //badVector(alpha_Y);
                }
                double Zx = alpha_Y.zSum();
                //if (Zx == 0) {
                //Zx = (Double.MIN_VALUE*100000000);
                //}
                thisSeqLogli -= log(Zx);
                // correct for the fact that alpha-s were scaled.
                for (int i = 0; i < dataSeq.length(); i++) {
                    thisSeqLogli -= log(scale[i]);
                }
                if (thisSeqLogli > 0) {
                    System.out.println("This is shady: something is wrong Pr(y|x) > 1!");
                }
                logli += thisSeqLogli;
                // update grad.
                for (int f = 0; f < grad.length; f++)
                    grad[f] -= ExpF[f]/Zx;
                
                if (params.debugLvl > 1) {
                    System.out.println("Sequence "  + thisSeqLogli + " " + logli);
                }
                
            }
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(grad[f] + " ");
                System.out.println(" :g");
            }
            
            if (params.debugLvl > 0)
                Util.printDbg("Iter " + icall + " log likelihood "+logli + " norm(grad logli) " + norm(grad) + " norm(x) "+ norm(lambda));
            
        } catch (Exception e) {
            System.out.println("Alpha-i " + alpha_Y.toString());
            System.out.println("Ri " + Ri_Y.toString());
            System.out.println("Mi " + Mi_YY.toString());
            
            e.printStackTrace();
            System.exit(0);
        }
        return logli;
    }
    
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y) {
        double DEFAULT_VALUE = 0;
        Mi_YY.assign(DEFAULT_VALUE);
        Ri_Y.assign(DEFAULT_VALUE);
        computeLogMiInitDone(featureGen,lambda,Mi_YY,Ri_Y, DEFAULT_VALUE);
    }
    static void computeLogMiInitDone(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, double DEFAULT_VALUE) {
        while (featureGen.hasNext()) { 
            Feature feature = featureGen.next();
            int f = feature.index();
            int yp = feature.y();
            int yprev = feature.yprev();
            float val = feature.value();
            if (yprev == -1) {
                // this is a single state feature.
                
                // if default value was a negative_infinity, need to
                // reset to.
                double oldVal = Ri_Y.get(yp);
                if (oldVal == DEFAULT_VALUE)
                    oldVal = 0;
                Ri_Y.set(yp,oldVal+lambda[f]*val);
            } else if (Mi_YY != null) {
                double oldVal = Mi_YY.get(yprev,yp);
                if (oldVal == DEFAULT_VALUE) {
                    oldVal = 0;
                    if (Ri_Y.get(yp) == DEFAULT_VALUE)
                        Ri_Y.set(yp,0);
                }
                Mi_YY.set(yprev,yp,oldVal+lambda[f]*val);
            }
        }
    }
    static void computeMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y) {
        featureGen.startScanFeaturesAt(dataSeq, i);
        computeLogMi(featureGen, lambda, Mi_YY, Ri_Y);	
        Ri_Y.assign(expFunc);
        Mi_YY.assign(expFunc);
        //	Mi_YY.forEachNonZero(expFunc2D);
    }
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y) {
        featureGen.startScanFeaturesAt(dataSeq, i);
        computeLogMi(featureGen, lambda, Mi_YY, Ri_Y);	
    }
    
    protected double computeFunctionGradientLL(double lambda[], double grad[]) {
        double logli = 0;
        try {
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*lambda[f]*params.invSigmaSquare;
                logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
            }
            diter.startScan();
            for (int numRecord = 0; diter.hasNext(); numRecord++) {
                DataSequence dataSeq = (DataSequence)diter.next();
                if (params.debugLvl > 1) {
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                }
                alpha_Y.assign(0);
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = RobustMath.LOG0;
                
                if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
                    beta_Y = new DoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < beta_Y.length; i++)
                        beta_Y[i] = new LogSparseDoubleMatrix1D(numY);
                }
                // compute beta values in a backward scan.
                // also scale beta-values to 1 to avoid numerical problems.
                beta_Y[dataSeq.length()-1].assign(0);
                for (int i = dataSeq.length()-1; i > 0; i--) {
                    if (params.debugLvl > 3) {
                        Util.printDbg("Features fired");
                        featureGenerator.startScanFeaturesAt(dataSeq, i);    
                        while (featureGenerator.hasNext()) { 
                            Feature feature = featureGenerator.next();
                            Util.printDbg(feature.toString());
                        }
                    }
                    
                    // compute the Mi matrix
                    computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y);
                    tmp_Y.assign(beta_Y[i]);
                    tmp_Y.assign(Ri_Y,sumFunc);
                    Mi_YY.zMult(tmp_Y, beta_Y[i-1],1,0,false);
                }
                
                
                double thisSeqLogli = 0;
                for (int i = 0; i < dataSeq.length(); i++) {
                    // compute the Mi matrix
                    computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y);
                    // find features that fire at this position..
                    featureGenerator.startScanFeaturesAt(dataSeq, i);
                    
                    if (i > 0) {
                        //tmp_Y.assign(alpha_Y);
                        Mi_YY.zMult(alpha_Y, newAlpha_Y,1,0,true);
                        newAlpha_Y.assign(Ri_Y,sumFunc); 
                    } else {
                        newAlpha_Y.assign(Ri_Y);
                    }
                    
                    while (featureGenerator.hasNext()) { 
                        Feature feature = featureGenerator.next();
                        int f = feature.index();
                        
                        int yp = feature.y();
                        int yprev = feature.yprev();
                        float val = feature.value();
                        if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                            grad[f] += val;
                            thisSeqLogli += val*lambda[f];
                        }
                        if (yprev < 0) {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], newAlpha_Y.get(yp) + Math.log(val) + beta_Y[i].get(yp));
                        } else {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+Math.log(val)+beta_Y[i].get(yp));
                        }
                    }
                    alpha_Y.assign(newAlpha_Y);
                    
                    if (params.debugLvl > 2) {
                        System.out.println("Alpha-i " + alpha_Y.toString());
                        System.out.println("Ri " + Ri_Y.toString());
                        System.out.println("Mi " + Mi_YY.toString());
                        System.out.println("Beta-i " + beta_Y[i].toString());
                    }
                }
                double lZx = alpha_Y.zSum();
                thisSeqLogli -= lZx;
                logli += thisSeqLogli;
                // update grad.
                for (int f = 0; f < grad.length; f++)
                    grad[f] -= Math.exp(ExpF[f]-lZx);
                
                if (params.debugLvl > 1) {
                    System.out.println("Sequence "  + thisSeqLogli + " " + logli );
                }
                if (thisSeqLogli > 0) {
                    System.out.println("This is shady: something is wrong Pr(y|x) > 1!");
                }
            }
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(grad[f] + " ");
                System.out.println(" :g");
            }
            
            if (params.debugLvl > 0)
                Util.printDbg("Iteration " + icall + " log-likelihood "+logli + " norm(grad logli) " + norm(grad) + " norm(x) "+ norm(lambda));
            
        } catch (Exception e) {
            System.out.println("Alpha-i " + alpha_Y.toString());
            System.out.println("Ri " + Ri_Y.toString());
            System.out.println("Mi " + Mi_YY.toString());
            
            e.printStackTrace();
            System.exit(0);
        }
        return logli;
    }
    
    static double log(double val) {
        try {
            return logE(val);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return -1*Double.MAX_VALUE;
    }
    static double exp(double val) {
        try {
            return expE(val);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return Double.MAX_VALUE;
    }
    static double logE(double val) throws Exception {
        double pr = Math.log(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            throw new Exception("Overflow error when taking log of " + val);
        }
        return pr;
    } 
    static double expE(double val) throws Exception {
        double pr = Math.exp(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            throw new Exception("Overflow error when taking exp of " + val + "\n Try running the CRF with the following option \"trainer ll\" to perform computations in the log-space.");
        }
        return pr;
    }
    
    /*
     static boolean badVector(DoubleMatrix1D vec) throws Exception {
     for (int i = 0; i < vec.size(); i++)
     if (Double.isNaN(vec.get(i)) || Double.isInfinite(vec.get(i)))
     throw new Exception("Bad vector"); 
     return false;
     }
     static final float MINUS_LOG_EPSILON = 100;
     // Controlled underflow adder of very small numbers expressed as
      // logs.  Returns log of their sum.
       static float logSumProb(DoubleMatrix1D logProb) {
       Vector logProbVector = new Vector();
       for ( int lpx = 0; lpx < logProb.size(); lpx++ )
       logProbVector.add(new Double(logProb.get(lpx)));
       return logSumProb(logProbVector);
       }
       static float logSumProb(Vector logProbVector) {
       while ( logProbVector.size() > 1 ) {
       Collections.sort(logProbVector);
       double lp0 = ((Double)logProbVector.remove(0)).doubleValue();
       double lp1 = ((Double)logProbVector.remove(0)).doubleValue();
       // lp0 is smaller (more negative)
        if ( lp1 > lp0 + MINUS_LOG_EPSILON ) {
        logProbVector.add(new Double(lp1));
        }
        else {
        double sum = lp1 + Math.log(Math.exp(lp0-lp1) + 1.0);
        logProbVector.add(new Double(sum));
        }
        }
        return ((Double)logProbVector.remove(0)).floatValue();
        }
        double scaledExp(double lpr, double logSumProb) {
        if (( logSumProb > lpr + MINUS_LOG_EPSILON ) && params.doRobustScale)
        return 0;
        else {
        return exp(lpr - logSumProb);
        }
        }
        double robustExp(DenseDoubleMatrix2D Mi_YY,DenseDoubleMatrix1D Ri_Y) {	
        float logSumProbM = 0;
        if (params.doRobustScale) {
        Vector vec = new Vector();
        for(int r = 0; r < Mi_YY.rows(); r++) {
        for(int c = 0; c < Mi_YY.columns(); c++) {
        vec.add(new Double(Mi_YY.get(r,c))); // + Ri_Y.get(c)));
        }
        }
        logSumProbM = logSumProb(vec);
        }
        for(int r = 0; r < Mi_YY.rows(); r++) {
        for(int c = 0; c < Mi_YY.columns(); c++) {
        Mi_YY.set(r,c,scaledExp(Mi_YY.get(r,c), logSumProbM));
        }
        }
        float logSumProbR = logSumProbM;
        if (params.doRobustScale)
        logSumProbR = logSumProb(Ri_Y);
        for ( int lx = 0; lx < Ri_Y.size(); lx++ ) {
        Ri_Y.set(lx,scaledExp(Ri_Y.get(lx), logSumProbR));
        }
        return logSumProbM+logSumProbR;
        }
        */
}
