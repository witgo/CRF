package iitb.CRF;

import riso.numerical.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
import iitb.CRF.HistoryManager.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class Trainer {
    protected int numF,numY;
    double gradLogli[];
    double diag[];
    double lambda[];
    protected boolean reuseM, initMDone=false;

    protected double ExpF[];
    double scale[], rLogScale[];
    
    protected DoubleMatrix2D Mi_YY;
    protected DoubleMatrix1D Ri_Y;
    protected DoubleMatrix1D alpha_Y, newAlpha_Y;
    protected DoubleMatrix1D beta_Y[];
    protected DoubleMatrix1D tmp_Y;
    
    
    static class  MultFunc implements DoubleDoubleFunction {
        public double apply(double a, double b) {return a*b;}
    };
    static class  SumFunc implements DoubleDoubleFunction {
        public double apply(double a, double b) {return a+b;}
    };
    static MultFunc multFunc = new MultFunc(); 
    protected static SumFunc sumFunc = new SumFunc(); 
    
    class MultSingle implements DoubleFunction {
        public double multiplicator = 1.0;
        public double apply(double a) {return a*multiplicator;}
    };
    MultSingle constMultiplier = new MultSingle();
    
    protected DataIter diter;
    FeatureGenerator featureGenerator;
    protected CrfParams params;
    EdgeGenerator edgeGen;
    protected int icall;
    Evaluator evaluator = null;
    
    FeatureGenCache featureGenCache;
    
    protected double norm(double ar[]) {
        double v = 0;
        for (int f = 0; f < ar.length; f++)
            v += ar[f]*ar[f];
        return Math.sqrt(v);
    }
    public Trainer(CrfParams p) {
        params = p; 
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval) {
        init(model,data,l);
        evaluator = eval;
        if (params.debugLvl > 0) {
            Util.printDbg("Number of features :" + lambda.length);	    
        }
        doTrain();
    }
    
    double getInitValue() { 
        // returns a negative value to avoid overflow in the initial stages.
        //      if (params.initValue == 0)
        //	return -1*Math.log(numY);
        return params.initValue;
    }
    protected void init(CRF model, DataIter data, double[] l) {
        edgeGen = model.edgeGen;
        lambda = l;
        numY = model.numY;
        diter = data;
        featureGenerator = model.featureGenerator;
        numF = featureGenerator.numFeatures();
        
        gradLogli = new double[numF];
        diag = new double [ numF ]; // needed by the optimizer
        ExpF = new double[lambda.length];
        initMatrices();
        reuseM = params.reuseM; 
        if (params.miscOptions.getProperty("cache", "false").equals("true")) {
            featureGenCache = new FeatureGenCache(featureGenerator);
            featureGenerator = featureGenCache;
        } else
            featureGenCache = null;
    }
    void initMatrices() {
        Mi_YY = new DenseDoubleMatrix2D(numY,numY);
        Ri_Y = new DenseDoubleMatrix1D(numY);
        
        alpha_Y = new DenseDoubleMatrix1D(numY);
        newAlpha_Y = new DenseDoubleMatrix1D(numY);
        tmp_Y = new DenseDoubleMatrix1D(numY);
        
    }
    
    void doTrain() {
        double f, xtol = 1.0e-16; // machine precision
        int iprint[] = new int [2], iflag[] = new int[1];
        icall=0;
        
        iprint [0] = params.debugLvl-2;
        iprint [1] = params.debugLvl-1;
        iflag[0]=0;
        
        for (int j = 0 ; j < lambda.length ; j ++) {
            // lambda[j] = 1.0/lambda.length;
            lambda[j] = getInitValue();
        }
        do {
            f = computeFunctionGradient(lambda,gradLogli); 
            f = -1*f; // since the routine below minimizes and we want to maximize logli
            for (int j = 0 ; j < lambda.length ; j ++) {
                gradLogli[j] *= -1;
            } 
            
            if ((evaluator != null) && (evaluator.evaluate() == false))
                break;
            try	{
                LBFGS.lbfgs (numF, params.mForHessian, lambda, f, gradLogli, false, diag, iprint, params.epsForConvergence, xtol, iflag);
            } catch (LBFGS.ExceptionWithIflag e)  {
                System.err.println( "CRF: lbfgs failed.\n"+e );
                if (e.iflag == -1) {
                    System.err.println("Possible reasons could be: \n \t 1. Bug in the feature generation or data handling code\n\t 2. Not enough features to make observed feature value==expected value\n");
                }
                return;
            }
            icall += 1;
        } while (( iflag[0] != 0) && (icall <= params.maxIters));
    }
    protected double computeFunctionGradient(double lambda[], double grad[]) {
        return computeFunctionGradient(lambda,grad,null);
    }
    protected double computeFunctionGradient(double lambda[], double grad[], double expFVals[]) {
        initMDone=false;
       
        if (params.trainerType.equals("ll"))
            return computeFunctionGradientLL(lambda,  grad, expFVals);
        double logli = 0;
        try {
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*lambda[f]*params.invSigmaSquare;
                logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
            }
            boolean doScaling = params.doScaling;
            
            diter.startScan();
            if (featureGenCache != null) featureGenCache.startDataScan();
            int numRecord = 0;
            for (numRecord = 0; diter.hasNext(); numRecord++) {
                DataSequence dataSeq = (DataSequence)diter.next();
                if (featureGenCache != null) featureGenCache.nextDataIndex();
                if (params.debugLvl > 1) {
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                }
                alpha_Y.assign(1);
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = 0;
                
                if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
                    beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < beta_Y.length; i++)
                        beta_Y[i] = new DenseDoubleMatrix1D(numY);
                    
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
                    initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true,reuseM,initMDone);
                    tmp_Y.assign(beta_Y[i]);
                    tmp_Y.assign(Ri_Y,multFunc);
                    RobustMath.Mult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
                    //		Mi_YY.zMult(tmp_Y, beta_Y[i-1]);
                    
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
                    initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true,reuseM,initMDone);
                    // find features that fire at this position..
                    featureGenerator.startScanFeaturesAt(dataSeq, i);
                    
                    if (i > 0) {
                        tmp_Y.assign(alpha_Y);
                        RobustMath.Mult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                        //		Mi_YY.zMult(tmp_Y, newAlpha_Y,1,0,true);
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
                }
                double Zx = alpha_Y.zSum();
                thisSeqLogli -= log(Zx);
                // correct for the fact that alpha-s were scaled.
                for (int i = 0; i < dataSeq.length(); i++) {
                    thisSeqLogli -= log(scale[i]);
                }
                
                logli += thisSeqLogli;
                // update grad.
                for (int f = 0; f < grad.length; f++)
                    grad[f] -= ExpF[f]/Zx;
                
                if (params.debugLvl > 1) {
                    System.out.println("Sequence "  + thisSeqLogli + " logli " + logli + " log(Zx) " + Math.log(Zx) + " Zx " + Zx);
                }
                
            }
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                for (int f = 0; f < lambda.length; f++)
                    System.out.println(featureGenerator.featureName(f) + " " + grad[f] + " ");
                System.out.println(" :g");
            }
            
            if (params.debugLvl > 0)
                Util.printDbg("Iter " + icall + " log likelihood "+logli + " norm(grad logli) " + norm(grad) + " norm(x) "+ norm(lambda));
            if (icall == 0) {
                System.out.println("Number of training records" + numRecord);
            }
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
            DoubleMatrix1D Ri_Y, boolean takeExp) {
        computeLogMi(featureGen,lambda,Mi_YY,Ri_Y,takeExp,false,false);
    }
    static boolean computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp,boolean reuseM, boolean initMDone) {
        
        if (reuseM && initMDone) {
            Mi_YY = null;
        } else
            initMDone = false;
        if (Mi_YY != null) Mi_YY.assign(0);
        Ri_Y.assign(0);
        while (featureGen.hasNext()) { 
            Feature feature = featureGen.next();
            int f = feature.index();
            int yp = feature.y();
            int yprev = feature.yprev();
            float val = feature.value();
            //	    System.out.println(feature.toString());
            
            if (yprev < 0) {
                // this is a single state feature.
                double oldVal = Ri_Y.getQuick(yp);
                Ri_Y.setQuick(yp,oldVal+lambda[f]*val);
            } else if (Mi_YY != null) {
                Mi_YY.setQuick(yprev,yp,Mi_YY.getQuick(yprev,yp)+lambda[f]*val);
                initMDone = true;
            }
        }
        if (takeExp) {
            for(int r = Ri_Y.size()-1; r >= 0; r--) {
                Ri_Y.setQuick(r,expE(Ri_Y.getQuick(r)));
                if (Mi_YY != null)
                    for(int c = Mi_YY.columns()-1; c >= 0; c--) {
                        Mi_YY.setQuick(r,c,expE(Mi_YY.getQuick(r,c)));
                    }
            }
        }
        return initMDone;
    }
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp) {
        computeLogMi(featureGen, lambda, dataSeq, i, Mi_YY, Ri_Y, takeExp,false,false);
    }
    static boolean computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp, boolean reuseM, boolean initMDone) {
        featureGen.startScanFeaturesAt(dataSeq, i);
        return computeLogMi(featureGen, lambda, Mi_YY, Ri_Y, takeExp,reuseM, initMDone);
    }
    
    protected double computeFunctionGradientLL(double lambda[], double grad[], double expFVals[]) {
        double logli = 0;
        try {
            if (grad != null) {
                for (int f = 0; f < lambda.length; f++) {
                    grad[f] = -1*lambda[f]*params.invSigmaSquare;
                    logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
                }
            }
            diter.startScan();
            if (featureGenCache != null) featureGenCache.startDataScan();
            for (int numRecord = 0; diter.hasNext(); numRecord++) {
                DataSequence dataSeq = (DataSequence)diter.next();
                if (featureGenCache != null) featureGenCache.nextDataIndex();
                if (params.debugLvl > 1) {
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                }
                alpha_Y.assign(0);
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = RobustMath.LOG0;
                
                if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
                    beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < beta_Y.length; i++)
                        beta_Y[i] = new DenseDoubleMatrix1D(numY);
                }
                // compute beta values in a backward scan.
                // also scale beta-values to 1 to avoid numerical problems.
                beta_Y[dataSeq.length()-1].assign(0);
                for (int i = dataSeq.length()-1; i > 0; i--) {
                    if (params.debugLvl > 2) {
                        /*  Util.printDbg("Features fired");
                         featureGenerator.startScanFeaturesAt(dataSeq, i);    
                         while (featureGenerator.hasNext()) { 
                         Feature feature = featureGenerator.next();
                         Util.printDbg(feature.toString());
                         }
                         */
                    }
                    
                    // compute the Mi matrix
                    initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
                    tmp_Y.assign(beta_Y[i]);
                    tmp_Y.assign(Ri_Y,sumFunc);
                    RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
                }
                
                
                double thisSeqLogli = 0;
                for (int i = 0; i < dataSeq.length(); i++) {
                    // compute the Mi matrix
                    initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
                    // find features that fire at this position..
                    featureGenerator.startScanFeaturesAt(dataSeq, i);
                    
                    if (i > 0) {
                        tmp_Y.assign(alpha_Y);
                        RobustMath.logMult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
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
                        
                       if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                            grad[f] += val;
                            thisSeqLogli += val*lambda[f];
                            if (params.debugLvl > 2) {
                                System.out.println("Feature fired " + f + " " + feature);
                            } 
                        }
                       
                        
                        if (yprev < 0) {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], newAlpha_Y.get(yp) + RobustMath.log(val) + beta_Y[i].get(yp));
                        } else {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[i].get(yp));
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
                double lZx = RobustMath.logSumExp(alpha_Y);
                thisSeqLogli -= lZx;
                logli += thisSeqLogli;
                
                // update grad.
                if (grad != null) {
                for (int f = 0; f < grad.length; f++) {
                    grad[f] -= RobustMath.exp(ExpF[f]-lZx);
                }
                }
                if (expFVals!=null) {
                    for (int f = 0; f < lambda.length; f++) {
                        expFVals[f] += RobustMath.exp(ExpF[f]-lZx);
                    }
                }
                if (params.debugLvl > 1) {
                    System.out.println("Sequence "  + thisSeqLogli + " logli " + logli + " log(Zx) " + lZx + " Zx " + Math.exp(lZx));
                }
                
            }
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                if (grad != null) for (int f = 0; f < lambda.length; f++)
                    System.out.print(grad[f] + " ");
                System.out.println(" :g");
            }
            
            if ((params.debugLvl > 0) && (grad != null))
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
    
    static double logE(double val) throws Exception {
        double pr = Math.log(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            throw new Exception("Overflow error when taking log of " + val);
        }
        return pr;
    } 
    static double expE(double val)  {
        double pr = RobustMath.exp(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            try {
                throw new Exception("Overflow error when taking exp of " + val + "\n Try running the CRF with the following option \"trainer ll\" to perform computations in the log-space.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }
        return pr;
    }
    static double expLE(double val) {
        double pr = RobustMath.exp(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            try {
                throw new Exception("Overflow error when taking exp of " + val 
                        + " you might need to redesign feature values so as to not reach such high values");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }
        return pr;
    }
}
