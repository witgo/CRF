package iitb.CRF;

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

    protected double computeFunctionGradient(double lambda[], double grad[]) {
	if (params.doScaling)
	    return computeFunctionGradientLL(lambda,  grad);
	try {
	FeatureGeneratorNested featureGenNested = (FeatureGeneratorNested)featureGenerator;
	double logli = 0;
	for (int f = 0; f < lambda.length; f++) {
	    grad[f] = -1*lambda[f]*params.invSigmaSquare;
	    logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
	}
	boolean doScaling = false; // scaling as implemented below does not work.
	diter.startScan();
	for (int numRecord = 0; diter.hasNext(); numRecord++) {
	    SegmentDataSequence dataSeq = (SegmentDataSequence)diter.next();
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
		    computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,true);
		    tmp_Y.assign(beta_Y[i+ell]);
		    tmp_Y.assign(Ri_Y,multFunc);
		    Mi_YY.zMult(tmp_Y, beta_Y[i],1,1,false);
		}
	    }
	    double thisSeqLogli = 0;
	    alpha_Y_Array[0].assign(1);
	    int segmentStart = 0;
	    int segmentEnd = -1;
	    for (int i = 0; i < dataSeq.length(); i++) {
		if (segmentEnd < i) {
		    segmentStart = i;
		    segmentEnd = dataSeq.getSegmentEnd(i);
		}
		alpha_Y_Array[i-base].assign(0);
		// compute the scale adjustment.
		float scaleProduct = 1;
		for (int j = i-featureGenNested.maxMemory()-base; j <= i-1; j++)
		    if (j >= 0) scaleProduct *= scale[j];
		for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i-ell >= base); ell++) {
		    // compute the Mi matrix
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		    if (!featureGenNested.hasNext())
			break;
		    computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,true);
		    // find features that fire at this position..
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		    boolean isSegment = ((i-ell+1==segmentStart) && (i == segmentEnd));
		    while (featureGenNested.hasNext()) { 
			Feature feature = featureGenNested.next();
			int f = feature.index();
		    
			int yp = feature.y();
			int yprev = feature.yprev();
			float val = feature.value();
			boolean allEllMatch = isSegment && (dataSeq.y(i) == yp);
			if (allEllMatch && (((i-ell >= 0) && (yprev == dataSeq.y(i-ell))) || (yprev < 0))) {
			    grad[f] += val;
			    thisSeqLogli += val*lambda[f];
			}
			if (yprev < 0) {
			    for (yprev = 0; yprev < Mi_YY.rows(); yprev++) 
				ExpF[f] += (alpha_Y_Array[i-ell-base].get(yprev)*Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val*beta_Y[i].get(yp))/scaleProduct;
			} else {
			    ExpF[f] += (alpha_Y_Array[i-ell-base].get(yprev)*Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val*beta_Y[i].get(yp))/scaleProduct;
			}
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

		if (params.debugLvl > 2) {
		    System.out.println("Alpha-i " + alpha_Y_Array[i-base].toString());
		    System.out.println("Ri " + Ri_Y.toString());
		    System.out.println("Mi " + Mi_YY.toString());
		    System.out.println("Beta-i " + beta_Y[i].toString());
		}
		if (params.debugLvl > 1) {
		    System.out.println(" pos "  + i + " " + thisSeqLogli);
		}
	    }
	    
	    double Zx = alpha_Y_Array[dataSeq.length()-1-base].zSum();
	    thisSeqLogli -= log(Zx);
	    // correct for the fact that alpha-s were scaled.
	    for (int i = 0; i < dataSeq.length()-base-featureGenNested.maxMemory(); i++) {
	      thisSeqLogli -= log(scale[i]);
	    }
	    logli += thisSeqLogli;
	    // update grad.
	    for (int f = 0; f < grad.length; f++)
		grad[f] -= ExpF[f]/Zx;
	    
	    if (params.debugLvl > 1) {
		System.out.println("Sequence "  + thisSeqLogli + " " + logli + " " + Zx);
		System.out.println("Last Alpha-i " + alpha_Y_Array[dataSeq.length()-1-base].toString());
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
	    Util.printDbg("Iter " + icall + " loglikelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));
	return logli;

	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}
	return 0;
    }


    protected double computeFunctionGradientLL(double lambda[], double grad[]) {
	try {
	FeatureGeneratorNested featureGenNested = (FeatureGeneratorNested)featureGenerator;
	double logli = 0;
	for (int f = 0; f < lambda.length; f++) {
	    grad[f] = -1*lambda[f]*params.invSigmaSquare;
	    logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
	}
	diter.startScan();
	for (int numRecord = 0; diter.hasNext(); numRecord++) {
	    SegmentDataSequence dataSeq = (SegmentDataSequence)diter.next();
	    if (params.debugLvl > 1)
		Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
	    for (int f = 0; f < lambda.length; f++)
		ExpF[f] = RobustMath.LOG0;

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
	    }
	    // compute beta values in a backward scan.
	    // also scale beta-values as much as possible to avoid numerical overflows
	    beta_Y[dataSeq.length()-1].assign(0);
	    for (int i = dataSeq.length()-2; i >= 0; i--) {
		beta_Y[i].assign(RobustMath.LOG0);
		for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i+ell < dataSeq.length()); ell++) {		    
		    // compute the Mi matrix
		    featureGenNested.startScanFeaturesAt(dataSeq, i, i+ell);
		    if (! featureGenNested.hasNext())
			break;
		    computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,false);
		    tmp_Y.assign(beta_Y[i+ell]);
		    tmp_Y.assign(Ri_Y,sumFunc);
		    RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i],1,1,false,edgeGen);
		}
	    }
	    double thisSeqLogli = 0;
	    alpha_Y_Array[0].assign(0);
	    int segmentStart = 0;
	    int segmentEnd = -1;
	    for (int i = 0; i < dataSeq.length(); i++) {
		if (segmentEnd < i) {
		    segmentStart = i;
		    segmentEnd = dataSeq.getSegmentEnd(i);
		}
		alpha_Y_Array[i-base].assign(RobustMath.LOG0);
		for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i-ell >= base); ell++) {
		    // compute the Mi matrix
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		    if (!featureGenNested.hasNext())
			break;
		    computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,false);
		    // find features that fire at this position..
		    featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		    boolean isSegment = ((i-ell+1==segmentStart) && (i == segmentEnd));
		    while (featureGenNested.hasNext()) { 
			Feature feature = featureGenNested.next();
			int f = feature.index();
			int yp = feature.y();
			int yprev = feature.yprev();
			float val = feature.value();
			boolean allEllMatch = isSegment && (dataSeq.y(i) == yp);
			if (allEllMatch && (((i-ell >= 0) && (yprev == dataSeq.y(i-ell))) || (yprev < 0))) {
			    grad[f] += val;
			    thisSeqLogli += val*lambda[f];
			}
			if ((yprev < 0) && (i-ell >= 0)) {
			    for (yprev = 0; yprev < Mi_YY.rows(); yprev++) 
				ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[i-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+Math.log(val)+beta_Y[i].get(yp)));
			} else if (i-ell < 0) {
			    ExpF[f] = RobustMath.logSumExp(ExpF[f], (Ri_Y.get(yp)+Math.log(val)+beta_Y[i].get(yp)));
			} else {
			    ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[i-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+Math.log(val)+beta_Y[i].get(yp)));
			}
		    }
		    if (i-ell >= 0) {
			RobustMath.logMult(Mi_YY, alpha_Y_Array[i-ell-base],tmp_Y,1,0,true,edgeGen);
			tmp_Y.assign(Ri_Y,sumFunc);
			RobustMath.logSumExp(alpha_Y_Array[i-base],tmp_Y);
		    } else {
			RobustMath.logSumExp(alpha_Y_Array[i-base],Ri_Y);
		    }
		}
		if (params.debugLvl > 2) {
		    System.out.println("Alpha-i " + alpha_Y_Array[i-base].toString());
		    System.out.println("Ri " + Ri_Y.toString());
		    System.out.println("Mi " + Mi_YY.toString());
		    System.out.println("Beta-i " + beta_Y[i].toString());
		}
		if (params.debugLvl > 1) {
		    System.out.println(" pos "  + i + " " + thisSeqLogli);
		}
	    }
	    
	    double lZx = RobustMath.logSumExp(alpha_Y_Array[dataSeq.length()-1-base]);
	    thisSeqLogli -= lZx;
	    logli += thisSeqLogli;
	    // update grad.
	    for (int f = 0; f < grad.length; f++)
		grad[f] -= Math.exp(ExpF[f]-lZx);
	    
	    if (params.debugLvl > 1) {
		System.out.println("Sequence "  + thisSeqLogli + " " + logli + " " + Math.exp(lZx));
		System.out.println("Last Alpha-i " + alpha_Y_Array[dataSeq.length()-1-base].toString());
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
	    Util.printDbg("Iter " + icall + " loglikelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));
	return logli;

	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}
	return 0;
    }
};
