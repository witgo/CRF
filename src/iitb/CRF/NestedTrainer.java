package iitb.CRF;

import java.lang.*;
import java.io.*;
import java.util.*;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class NestedTrainer extends Trainer {
    public NestedTrainer(CrfParams p) {
	super(p);
    }
    DenseDoubleMatrix1D alpha_Y_Array[];

    boolean isASegment(int start, int end, DataSequence dataSeq) {
	if ((end < dataSeq.length()-1) && (dataSeq.y(end+1) == dataSeq.y(end)))
	    return false;
	if ((start > 0) && (dataSeq.y(start-1) == dataSeq.y(start)))
	    return false;
	for (int i = start; i < end; i++) {
	    if (dataSeq.y(i) != dataSeq.y(i+1))
		return false;
	}
	return true;
    }
    protected double computeFunctionGradient(double lambda[], double grad[]) {
	FeatureGeneratorNested featureGenNested = (FeatureGeneratorNested)featureGenerator;
	double logli = 0;
	for (int f = 0; f < lambda.length; f++) {
	    grad[f] = -1*lambda[f]*params.invSigmaSquare;
	    logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
	}
	boolean doScaling = params.doScaling;
	diter.startScan();
	for (int numRecord = 0; diter.hasNext(); numRecord++) {
	    DataSequence dataSeq = (DataSequence)diter.next();
	    if (params.debugLvl > 1)
		Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
	    for (int f = 0; f < lambda.length; f++)
		ExpF[f] = 0;

	    int base = -1;
	    if ((alpha_Y_Array == null) || (alpha_Y_Array.length < dataSeq.length()-base)) {
		alpha_Y_Array = new DenseDoubleMatrix1D[2*dataSeq.length()];
		for (int i = 0; i < alpha_Y_Array.length; i++)
		    alpha_Y_Array[i] = new DenseDoubleMatrix1D(numY);
	    }
	    if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
		beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
		for (int i = 0; i < beta_Y.length; i++)
		    beta_Y[i] = new DenseDoubleMatrix1D(numY);

		scale = new double[2*dataSeq.length()];
	    }
	    // compute beta values in a backward scan.
	    // also scale beta-values as much as possible to avoid numerical overflows
	    beta_Y[dataSeq.length()-1].assign(1.0);
	    scale[dataSeq.length()-1]=1;
	    for (int i = dataSeq.length()-2; i >= 0; i--) {
		// we need to do delayed scaling, so scale everything by the first element of the current window.
		if (doScaling && (i + featureGenNested.maxMemory() < dataSeq.length())) {
		    int iL = i + featureGenNested.maxMemory();
		    scale[iL] = beta_Y[iL].zSum();
		    constMultiplier.multiplicator = 1.0/scale[iL];
		    for (int j = i+1; j <= iL; j++) {
			beta_Y[j].assign(constMultiplier);
		    }
		}
		beta_Y[i].assign(0);
		scale[i] = 1;
		for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i+ell < dataSeq.length()); ell++) {		    
		    // compute the Mi matrix
		    featureGenNested.startScanFeaturesAt(dataSeq, i, i+ell);
		    if (! featureGenNested.hasNext())
			break;
		    CRF.computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,true);
		    tmp_Y.assign(beta_Y[i+ell]);
		    tmp_Y.assign(Ri_Y,multFunc);
		    Mi_YY.zMult(tmp_Y, beta_Y[i],1,1,false);
		}
	    }
	    double thisSeqLogli = 0;
	    alpha_Y_Array[0].assign(1);
	    for (int i = 0; i < dataSeq.length(); i++) {
		alpha_Y_Array[i-base].assign(0);
		// compute the scale adjustment.
		float scaleProduct = 1;
		for (int j = i-featureGenNested.maxMemory()-base; j <= i-1; j++)
		    scaleProduct *= scale[j];
		for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i-ell >= base); ell++) {
		    // compute the Mi matrix
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		    if (!featureGenNested.hasNext())
			break;
		    CRF.computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,true);
		    // find features that fire at this position..
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);

		    while (featureGenNested.hasNext()) { 
			Feature feature = featureGenNested.next();
			int f = feature.index();
		    
			fMi_YY.assign(0);
			int yp = feature.y();
			int yprev = feature.yprev();
			float val = feature.value();
			boolean allEllMatch = isASegment(i-ell+1,i,dataSeq) && (dataSeq.y(i) == yp);
			if (allEllMatch && (((i-ell >= 0) && (yprev == dataSeq.y(i-ell))) || (yprev < 0))) {
			    grad[f] += val;
			    thisSeqLogli += val*lambda[f];
			}
			if (yprev < 0) {
			    for (yprev = 0; yprev < Mi_YY.rows(); yprev++) 
				fMi_YY.set(yprev,yp, Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val);
			} else {
			    fMi_YY.set(yprev,yp, Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val);
			}
			// now compute the i-th term to be added.
			fMi_YY.zMult(alpha_Y_Array[i-ell-base], tmp_Y, 1,0,true);
			//		    tmp_Y.assign(fRi_Y, multFunc);		    
			ExpF[f] += tmp_Y.zDotProduct(beta_Y[i])/scaleProduct;
		    }
		    Mi_YY.zMult(alpha_Y_Array[i-ell-base],tmp_Y,1,0,true);
		    tmp_Y.assign(Ri_Y,multFunc);
		    alpha_Y_Array[i-base].assign(tmp_Y, sumFunc);
		}
		
		// now do the delayed scaling of  the alpha-s to avoid overflow problems.
		if (i-base-featureGenNested.maxMemory() >= 0) {
		    int iL = i-base-featureGenNested.maxMemory();
		    constMultiplier.multiplicator = 1.0/scale[iL];
		    for (int j = iL; j <= i-base; j++) {
			alpha_Y_Array[j].assign(constMultiplier);
		    }
		}
		if (params.debugLvl > 1) {
		    System.out.println("Alpha-i " + alpha_Y_Array[i-base].toString());
		    System.out.println("Ri " + Ri_Y.toString());
		    System.out.println("Mi " + Mi_YY.toString());
		    System.out.println("Beta-i " + beta_Y[i].toString());
		}
	    }
	    
	    double Zx = alpha_Y_Array[dataSeq.length()-1-base].zSum();
	    thisSeqLogli -= Math.log(Zx);
	    logli += thisSeqLogli;
	    // correct for the fact that alpha-s were scaled.
	    for (int i = 0; i < dataSeq.length()-base-featureGenNested.maxMemory(); i++) {
	      thisSeqLogli -= Math.log(scale[i]);
	    }
	    // update grad.
	    for (int f = 0; f < grad.length; f++)
		grad[f] -= ExpF[f]/Zx;
	}
	if (params.debugLvl > 1) {
	    for (int f = 0; f < lambda.length; f++)
		System.out.print(lambda[f] + " ");
	    System.out.println(" :x");
	    for (int f = 0; f < lambda.length; f++)
		System.out.print(grad[f] + " ");
	    System.out.println(" :g");
	}
	
	if (params.debugLvl > 0)
	    Util.printDbg("Iter " + icall + " likelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));

	return logli;
    }
};
