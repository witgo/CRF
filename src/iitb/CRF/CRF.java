package iitb.CRF;

import java.lang.*;
import java.io.*;
import java.util.*;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
/**
 *
 * CRF (conditional random fields) This class provides support for
 * training and applying a conditional random field for sequence
 * labeling problems.   
 *
 * @author Sunita Sarawagi
 *
 */ 


public class CRF {
    double lambda[];
    int numY;
    Trainer trainer;
    FeatureGenerator featureGenerator;

    public CrfParams params;
    
    /**
     * @param numLabels is the number of distinct class labels or y-labels
     * @param fgen is the class that is responsible for providing 
     * the features for a particular position on the sequence.
     * @param arg is a string that can be used to control various 
     * parameters of the CRF, these are space separated name-value pairs 
     * described in 
     * @see iitb.CRF.CrfParams 
     */
    public CRF(int numLabels, FeatureGenerator fgen, String arg) {
	featureGenerator = fgen;
	numY = numLabels;

	params = new CrfParams(arg);
    }

    public CRF(int numLabels, FeatureGenerator fgen, java.util.Properties configOptions) {
	featureGenerator = fgen;
	numY = numLabels;
	params = new CrfParams(configOptions);
    }

    /**
     * write the trained parameters of the CRF to the file
     */
    public void write(String fileName)  throws IOException {
	PrintWriter out=new PrintWriter(new FileOutputStream(fileName));
	out.println(lambda.length);
	for (int i = 0; i < lambda.length; i++)
	    out.println(lambda[i]);
	out.close();
    }
    /**
     * read the parameters of the CRF from a file
     */
    public void read(String fileName) throws IOException {
	BufferedReader in=new BufferedReader(new FileReader(fileName));
	int numF = Integer.parseInt(in.readLine());
	lambda = new double[numF];
	int pos = 0;
	String line;
	while((line=in.readLine())!=null) {
	    lambda[pos++] = Double.parseDouble(line);
	}
    }
    public void train(DataIter trainData) {
	lambda = new double[featureGenerator.numFeatures()];	
	trainer = new Trainer(params);
	trainer.train(this, trainData, lambda);
    }

    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
			     DenseDoubleMatrix2D Mi_YY,
			     DenseDoubleMatrix1D Ri_Y, boolean takeExp) {
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
		Ri_Y.set(r,Math.exp(Ri_Y.get(r)));
		for(int c = 0; c < Mi_YY.columns(); c++) {
		    Mi_YY.set(r,c,Math.exp(Mi_YY.get(r,c)));
		}
	    }
	}
    }
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
			     DataSequence dataSeq, int i, 
			     DenseDoubleMatrix2D Mi_YY,
			     DenseDoubleMatrix1D Ri_Y, boolean takeExp) {
	featureGen.startScanFeaturesAt(dataSeq, i);
	computeLogMi(featureGen, lambda, Mi_YY, Ri_Y, takeExp);
    }

    double gamma[]; // scratch space for temporary computation.
    double gammaPrev[]; // scratch space for temporary computation.
    int winningLabel[][];
    DenseDoubleMatrix2D Mi;
    DenseDoubleMatrix1D Ri;
    void allocateScratch() {
	gamma = new double[numY];
	gammaPrev = new double[numY];
	Mi = new DenseDoubleMatrix2D(numY,numY);
	Ri = new DenseDoubleMatrix1D(numY);
	winningLabel = new int[numY][];
    }
    public void apply(DataSequence dataSeq) {
	if (params.debugLvl > 1) 
	    Util.printDbg("CRF: Applying on " + dataSeq);
	if (gamma == null) {
	    allocateScratch();
	}
	for (int y = 0; y < gammaPrev.length; gamma[y] = 0, gammaPrev[y++] = 0);
	if ((winningLabel[0] == null) || (winningLabel[0].length < dataSeq.length())) {
	    for (int yi = 0; yi < winningLabel.length; winningLabel[yi++]=new int[dataSeq.length()]);
	}
	for (int i = 0; i < dataSeq.length(); i++) {
	    // compute Mi.
	    computeLogMi(featureGenerator,lambda,dataSeq,i,Mi,Ri,false);
	    for (int yi = 0; yi < numY; yi++) {
		gamma[yi] = Ri.get(yi);
		if (i > 0) {
		for (int yp = 0; yp < numY; yp++) {
		    double val = gammaPrev[yp]+Mi.get(yp,yi)+Ri.get(yi);
		    if (gamma[yi] < val) {
			gamma[yi] = val;
			winningLabel[yi][i] = yp;
		    } 
		}
		}
		if (params.debugLvl > 1) 
		    Util.printDbg(i +" y " + yi + " gamma " + gamma[yi]);
	    }
	    double temp[] = gammaPrev;
	    gammaPrev = gamma;
	    gamma = temp;
	}
	// find best gamma..
	int ybest = 0;
	for (int yInd = 0; yInd < gammaPrev.length; yInd++) {
	    if (gammaPrev[ybest] < gammaPrev[yInd])
		ybest = yInd;
	}
	for(int i = dataSeq.length()-1; i >= 0; i--) {
	    dataSeq.set_y(i, ybest);
	    ybest = winningLabel[ybest][i];
	}
	if (params.debugLvl > 1)
	    Util.printDbg("Returning sequence labels ");
    }
};
