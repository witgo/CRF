package iitb.CRF;

import java.util.*;
import riso.numerical.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class Trainer {
    int numF,numY;
    double gradLogli[];
    double diag[];
    double lambda[];

    DenseDoubleMatrix2D Mi_YY;
    DenseDoubleMatrix1D Ri_Y;
    DenseDoubleMatrix1D alpha_Y;
    DenseDoubleMatrix1D beta_Y[];
    DenseDoubleMatrix1D tmp_Y;
    double ExpF[];
    double scale[];
    SparseDoubleMatrix2D fMi_YY;
    //    SparseDoubleMatrix1D fRi_Y;

    class  MultFunc implements DoubleDoubleFunction {
	public double apply(double a, double b) {return a*b;}
    };
    class  SumFunc implements DoubleDoubleFunction {
	public double apply(double a, double b) {return a+b;}
    };
    MultFunc multFunc = new MultFunc(); 
    SumFunc sumFunc = new SumFunc(); 
    
    class MultSingle implements DoubleFunction {
	public double multiplicator = 1.0;
	public double apply(double a) {return a*multiplicator;}
    };
    MultSingle constMultiplier = new MultSingle();

    DataIter diter;
    FeatureGenerator featureGenerator;
    CrfParams params;
    int icall;

    double norm(double ar[]) {
	double v = 0;
	for (int f = 0; f < ar.length; f++)
	    v += ar[f]*ar[f];
	return Math.sqrt(v);
    }
    public Trainer(CrfParams p) {
	params = p; 
    }
    public void train(CRF model, DataIter data, double[] l) {
	init(model,data,l);
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
    void init(CRF model, DataIter data, double[] l) {
	lambda = l;
	numY = model.numY;
	diter = data;
	featureGenerator = model.featureGenerator;
	numF = featureGenerator.numFeatures();

	gradLogli = new double[numF];
	diag = new double [ numF ]; // needed by the optimizer

	Mi_YY = new DenseDoubleMatrix2D(numY,numY);
	Ri_Y = new DenseDoubleMatrix1D(numY);

	alpha_Y = new DenseDoubleMatrix1D(numY);
	tmp_Y = new DenseDoubleMatrix1D(numY);

	ExpF = new double[lambda.length];
	fMi_YY = new SparseDoubleMatrix2D(numY,numY);
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
	    try	{
		LBFGS.lbfgs (numF, params.mForHessian, lambda, f, gradLogli, false, diag, iprint, params.epsForConvergence, xtol, iflag);
	    } catch (LBFGS.ExceptionWithIflag e)  {
		System.err.println( "CRF: lbfgs failed.\n"+e );
		return;
	    }
	    icall += 1;
	} while (( iflag[0] != 0) && (icall <= params.maxIters));
    }
    protected double computeFunctionGradient(double lambda[], double grad[]) {
	double logli = 0;
	for (int f = 0; f < lambda.length; f++) {
	    grad[f] = -1*lambda[f]*params.invSigmaSquare;
	    logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
	}
	boolean doScaling = params.doScaling;
	// TODO -- optimize code for 1d features.
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
		    featureGenerator.startScanFeaturesAt(dataSeq, i);    
		    while (featureGenerator.hasNext()) { 
			Feature feature = featureGenerator.next();
			Util.printDbg(feature.toString());
		    }
		}


		// compute the Mi matrix
		CRF.computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true);
		tmp_Y.assign(beta_Y[i]);
		tmp_Y.assign(Ri_Y,multFunc);
		Mi_YY.zMult(tmp_Y, beta_Y[i-1]);

		// need to scale the beta-s to avoid overflow
		scale[i-1] = doScaling?beta_Y[i-1].zSum():1;
		constMultiplier.multiplicator = 1.0/scale[i-1];
		beta_Y[i-1].assign(constMultiplier);
	    }


	    double thisSeqLogli = 0;
	    for (int i = 0; i < dataSeq.length(); i++) {
		// compute the Mi matrix
		CRF.computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true);
		// find features that fire at this position..
		featureGenerator.startScanFeaturesAt(dataSeq, i);

		while (featureGenerator.hasNext()) { 
		    Feature feature = featureGenerator.next();
		    int f = feature.index();
		    
		    fMi_YY.assign(0);
		    int yp = feature.y();
		    int yprev = feature.yprev();
		    float val = feature.value();
		    if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
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
		    fMi_YY.zMult(alpha_Y, tmp_Y, 1,0,true);
		    //		    tmp_Y.assign(fRi_Y, multFunc);
		    
		    double expFi = tmp_Y.zDotProduct(beta_Y[i]);
		    ExpF[f] += expFi;
		}
		tmp_Y.assign(alpha_Y);
		Mi_YY.zMult(tmp_Y, alpha_Y,1,0,true);
		alpha_Y.assign(Ri_Y,multFunc); 

		// now scale the alpha-s to avoid overflow problems.
		constMultiplier.multiplicator = 1.0/scale[i];
		alpha_Y.assign(constMultiplier);

		
		if (params.debugLvl > 1) {
		    System.out.println("Alpha-i " + alpha_Y.toString());
		    System.out.println("Ri " + Ri_Y.toString());
		    System.out.println("Mi " + Mi_YY.toString());
		    System.out.println("Beta-i " + beta_Y[i].toString());
		}
	    }
	    double Zx = alpha_Y.zSum();
	    thisSeqLogli -= Math.log(Zx);
	    // correct for the fact that alpha-s were scaled.
	    for (int i = 0; i < dataSeq.length(); i++) {
	      thisSeqLogli -= Math.log(scale[i]);
	    }

	    logli += thisSeqLogli;
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
	    Util.printDbg("Iter " + icall + " log likelihood "+logli + " norm(grad logli) " + norm(grad) + " norm(x) "+ norm(lambda));

	return logli;
    }
}
