package iitb.CRF;

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
    DenseDoubleMatrix1D alpha_Y, newAlpha_Y;
    DenseDoubleMatrix1D beta_Y[];
    DenseDoubleMatrix1D tmp_Y;
    double ExpF[];
    double scale[], rLogScale[];
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
    EdgeGenerator edgeGen;
    int icall;
    Evaluator evaluator = null;

    double norm(double ar[]) {
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
    void init(CRF model, DataIter data, double[] l) {
	edgeGen = model.edgeGen;
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
	newAlpha_Y = new DenseDoubleMatrix1D(numY);
	tmp_Y = new DenseDoubleMatrix1D(numY);

	ExpF = new double[lambda.length];
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
		return;
	    }
	    icall += 1;
	} while (( iflag[0] != 0) && (icall <= params.maxIters));
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
	int numRecord = 0;
	for (numRecord = 0; diter.hasNext(); numRecord++) {
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
		    //featureGenerator.startScanFeaturesAt(dataSeq, i);    
		    //while (featureGenerator.hasNext()) { 
		    //Feature feature = featureGenerator.next();
		    //Util.printDbg(feature.toString());
		    //}
		}

		// compute the Mi matrix
		computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true);
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
		computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true);
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
	Mi_YY.assign(0);
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
		double oldVal = Ri_Y.get(yp);
		Ri_Y.set(yp,oldVal+lambda[f]*val);
	    } else {
		Mi_YY.set(yprev,yp,Mi_YY.get(yprev,yp)+lambda[f]*val);
	    }
	}
	if (takeExp) {
	    for(int r = 0; r < Mi_YY.rows(); r++) {
		Ri_Y.set(r,exp(Ri_Y.get(r)));
		for(int c = 0; c < Mi_YY.columns(); c++) {
		    Mi_YY.set(r,c,exp(Mi_YY.get(r,c)));
		}
	    }
	}
    }
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
			     DataSequence dataSeq, int i, 
			     DoubleMatrix2D Mi_YY,
			     DoubleMatrix1D Ri_Y, boolean takeExp) {
	featureGen.startScanFeaturesAt(dataSeq, i);
	computeLogMi(featureGen, lambda, Mi_YY, Ri_Y, takeExp);
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
		beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
		for (int i = 0; i < beta_Y.length; i++)
		    beta_Y[i] = new DenseDoubleMatrix1D(numY);
	    }
	    // compute beta values in a backward scan.
	    // also scale beta-values to 1 to avoid numerical problems.
	    beta_Y[dataSeq.length()-1].assign(0);
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
		computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false);
		tmp_Y.assign(beta_Y[i]);
		tmp_Y.assign(Ri_Y,sumFunc);
		RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
	    }


	    double thisSeqLogli = 0;
	    for (int i = 0; i < dataSeq.length(); i++) {
		// compute the Mi matrix
		computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false);
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
	    double lZx = RobustMath.logSumExp(alpha_Y);
	    thisSeqLogli -= lZx;
	    logli += thisSeqLogli;
	    // update grad.
	    for (int f = 0; f < grad.length; f++)
		grad[f] -= Math.exp(ExpF[f]-lZx);
	    
	    if (params.debugLvl > 1) {
		System.out.println("Sequence "  + thisSeqLogli + " " + logli );
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

    static double myLog(double val) {
	return (Math.abs(val-1) < Double.MIN_VALUE)?0:Math.log(val);
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
