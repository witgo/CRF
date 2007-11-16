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
        logProcessing=true;
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
        beta_Y[dataSeq.length()-1].assign(0);
        betaLoss[dataSeq.length()-1].assign(RobustMath.LOG0);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
            
            int ycorr = dataSeq.y(i);
            for (int yprev=0; yprev < numY; yprev++) {
                betaLoss[i-1].set(yprev,beta_Y[i].get(ycorr)+Ri_Y.get(ycorr)+Mi_YY.get(yprev,ycorr));
            }
            
            tmp_Y.assign(betaLoss[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, betaLoss[i-1],1,1,false,edgeGen);
        }
        double betaLogZ=0;
        double logZ=0;
        double obj = 0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            if (i > 0) {
                tmp_Y.assign(alphas[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphas[i],1,0,true,edgeGen);
                alphas[i].assign(Ri_Y,sumFunc); 
            } else {
                alphas[i].assign(Ri_Y);
                
                tmp_Y.assign(beta_Y[0]);
                tmp_Y.assign(Ri_Y, sumFunc);
                double t1 = RobustMath.logSumExp(tmp_Y)+Math.log(dataSeq.length());
                
                tmp_Y.assign(betaLoss[i]);
                tmp_Y.assign(Ri_Y,sumFunc);
                int ycorr=dataSeq.y(0);
                tmp_Y.set(ycorr, RobustMath.logSumExp(tmp_Y.get(ycorr)
                        ,beta_Y[0].get(ycorr)+Ri_Y.get(ycorr)));
                double t2= RobustMath.logSumExp(tmp_Y);
                try {
                    betaLogZ = RobustMath.logMinusExp(t1,t2);
                    logZ=betaLogZ;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (i > 0) {
                tmp_Y.assign(alphaLoss[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphaLoss[i],1,0,true,edgeGen);
                alphaLoss[i].assign(Ri_Y,sumFunc);
            } else {
                alphaLoss[i].assign(RobustMath.LOG0);
            }
            int ycorr = dataSeq.y(i);
            alphaLoss[i].set(ycorr, RobustMath.logSumExp(alphaLoss[i].get(ycorr),alphas[i].get(ycorr)));
            
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
                    grad[f] += val;
                    obj += val*lambda[f];
                }
                double logpr=beta_Y[i].get(yp)-logZ;
                if (yprev < 0) {
                        logpr += alphas[i].get(yp);
                        grad[f] += val*(Math.exp(alphaLoss[i].get(yp)+beta_Y[i].get(yp)-logZ)+Math.exp(alphas[i].get(yp)+betaLoss[i].get(yp)-logZ));
                } else {
                        logpr += alphas[i-1].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp);
                        grad[f] += val*(Math.exp(alphaLoss[i-1].get(yprev)+beta_Y[i].get(yp)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)-logZ)
                                +Math.exp(alphas[i-1].get(yprev)+betaLoss[i].get(yp)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)-logZ));
                        if (yp==dataSeq.y(i)) {
                            grad[f] += val*Math.exp(alphas[i-1].get(yprev)+beta_Y[i].get(yp)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)-logZ);
                        }
                }
                grad[f] -= val*Math.exp(logpr)*dataSeq.length();
            }
        }
        double t1 = RobustMath.logSumExp(alphas[dataSeq.length()-1])+Math.log(dataSeq.length());
        double t2= RobustMath.logSumExp(alphaLoss[dataSeq.length()-1]);
        try {
            logZ = RobustMath.logMinusExp(t1,t2);
            assert(Math.abs(logZ-betaLogZ)< 1e-2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj-logZ;
    }
}
