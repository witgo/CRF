/*
 * Created on Oct 16, 2007
 * @author sunita
 * 
 * Objective is log (sum_y hammingLoss(y)*exp(W.(F(xi,y)-F(xi,yi))))
 * 
 */
package iitb.CRF;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class SoftLogMarginTrainer extends Trainer {
    DoubleMatrix1D alphas[]= new DenseDoubleMatrix1D[0];
    DoubleMatrix1D alphaLoss[]= new DenseDoubleMatrix1D[0];
    DoubleMatrix1D betaLoss[]= new DenseDoubleMatrix1D[0];
    public SoftLogMarginTrainer(CrfParams p) {
        super(p);
    }
    @Override
    protected double sumProductInner(DataSequence dataSeq, FeatureGenerator featureGenerator, double[] lambda, double[] grad, 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < beta_Y.length; i++)
                beta_Y[i] = new DenseDoubleMatrix1D(numY);
            alphas= new DenseDoubleMatrix1D[beta_Y.length];
            alphaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            betaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            for (int i = 0; i < betaLoss.length; i++) {
                betaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphas[i] = new DenseDoubleMatrix1D(numY);
            }
        }
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            assert(Ri_Y.get(dataSeq.y(i))==0);
            if (i > 0) {
                tmp_Y.assign(alphas[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphas[i],1,0,true,edgeGen);
                alphas[i].assign(Ri_Y,sumFunc); 
            } else {
                alphas[i].assign(Ri_Y);
            }
            if (i > 0) {
                tmp_Y.assign(alphaLoss[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphaLoss[i],1,0,true,edgeGen);
                alphaLoss[i].assign(Ri_Y,sumFunc);
            } else {
                alphaLoss[i].assign(Ri_Y);
            }
            int ycorr = dataSeq.y(i);
            alphaLoss[i].set(ycorr, RobustMath.logSumExp(alphaLoss[i].get(ycorr),alphas[i].get(ycorr)));
        }
        double logZx = RobustMath.logSumExp(alphas[dataSeq.length()-1]);
        double sumExpFDiff=RobustMath.logSumExp(alphaLoss[dataSeq.length()]);
        double logZ=0;
        try {
            logZ = RobustMath.logMinusExp(dataSeq.length()*logZx,sumExpFDiff);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        beta_Y[dataSeq.length()-1].assign(0);
        betaLoss[dataSeq.length()-1].assign(0);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
            
            tmp_Y.assign(betaLoss[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, betaLoss[i-1],1,0,false,edgeGen);
            
            int ycorr = dataSeq.y(i);
            for (int yprev=0; yprev < numY; yprev++) {
                betaLoss[i-1].set(yprev,RobustMath.logSumExp(betaLoss[i-1].get(yprev)
                        ,beta_Y[i].get(ycorr)+Ri_Y.get(ycorr)+Mi_YY.get(yprev,ycorr)));
            }
        }
        for (int i = 0; i < dataSeq.length(); i++) {
            computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            // find features that fire at this position..
            fgenForExpVals.startScanFeaturesAt(dataSeq, i);
            while (fgenForExpVals.hasNext()) { 
                Feature feature = fgenForExpVals.next();
                int f = feature.index();
                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if (Math.abs(val) < Double.MIN_VALUE) continue;
                if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                    val *= -1;
                }
                if (yprev < 0) {
                    double logpr = alphas[i].get(yp) + beta_Y[i].get(yp)-logZ;
                    grad[f] += dataSeq.length()*val*Math.exp(logpr);
                    grad[f] -= val*Math.exp(alphaLoss[i].get(yp)+betaLoss[i].get(yp)-logZ);
                } else {
                    double logpr = alphas[i-1].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+beta_Y[i].get(yp)-logZ;
                    grad[f] += dataSeq.length()*val*Math.exp(logpr);
                    grad[f] -= val*Math.exp(alphaLoss[i-1].get(yprev)+betaLoss[i].get(yp)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)-logZ);
                }
            }
        }
        return logZ;
    }
    
    
    @Override
    protected boolean computeLogMiTrainMode(FeatureGenerator featureGenerator, double[] lambda, DataSequence dataSeq, int i, DoubleMatrix2D mi_YY, DoubleMatrix1D ri_Y, boolean b, boolean reuseM, boolean initMDone) {
        boolean initDoneNow = super.computeLogMiTrainMode(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, b, reuseM, initMDone);
        for (int y = 0; y < numY; y++) {
            Ri_Y.set(y, Ri_Y.get(y)-Ri_Y.get(dataSeq.y(i)));
            if (!reuseM || (!initMDone && initDoneNow)) {
                assert(i>0);
                for (int yp = 0; yp < numY; yp++) {
                    Mi_YY.set(yp, y, Mi_YY.get(yp,y)-Mi_YY.get(dataSeq.y(i-1), dataSeq.y(i)));
                }
            }
        }
        return initDoneNow;
    }
}
