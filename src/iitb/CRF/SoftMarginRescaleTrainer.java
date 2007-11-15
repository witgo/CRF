/*
 * Created on Nov 14, 2007
 * @author sunita
 * Objective is log (sum_y exp(W.(F(xi,y)-F(xi,yi))+hammingLoss(y)))
 */
package iitb.CRF;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class SoftMarginRescaleTrainer extends Trainer {
    public SoftMarginRescaleTrainer(CrfParams p) {
        super(p);
    }
    @Override
    protected double sumProductInner(DataSequence dataSeq, FeatureGenerator featureGenerator, double[] lambda, double[] grad, 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            allocateAlphaBeta(2*dataSeq.length()+1);
         }
         // compute beta values in a backward scan.
         // also scale beta-values to 1 to avoid numerical problems.
         beta_Y = computeBetaArray(dataSeq,lambda,featureGenerator);
         
         double logZ=0;
         
         alpha_Y.assign(0);
         for (int i = 0; i < dataSeq.length(); i++) {
             // compute the Mi matrix
             initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
             
             if (i > 0) {
                 tmp_Y.assign(alpha_Y);
                 RobustMath.logMult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                 newAlpha_Y.assign(Ri_Y,sumFunc); 
             } else {
                 newAlpha_Y.assign(Ri_Y);
                 
                 // compute logZ from beta[0]&R0
                 tmp_Y.assign(beta_Y[0]);
                 tmp_Y.assign(Ri_Y,sumFunc);
                 logZ = RobustMath.logSumExp(tmp_Y);
             }

             if (fgenForExpVals != null) {
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
                         grad[f] += val*Math.exp(newAlpha_Y.get(yp) + beta_Y[i].get(yp)-logZ);
                     } else {
                         grad[f] += val*Math.exp(alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+beta_Y[i].get(yp)-logZ);
                     }
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
         lZx = 0;
         assert(Math.abs(RobustMath.logSumExp(alpha_Y)-logZ)<1e-6);
         return logZ;
    }
    @Override
    protected boolean computeLogMiTrainMode(FeatureGenerator featureGenerator, double[] lambda, DataSequence dataSeq, int i, DoubleMatrix2D mi_YY, DoubleMatrix1D ri_Y, boolean b, boolean reuseM, boolean initMDone) {
        boolean initDoneNow = super.computeLogMiTrainMode(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, b, reuseM, initMDone);
        for (int y = 0; y < numY; y++) {
            int loss=(y==dataSeq.y(i))?0:1;
            Ri_Y.set(y, Ri_Y.get(y)-Ri_Y.get(dataSeq.y(i))+loss);
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
