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

public class NestedCRF extends CRF {
    FeatureGeneratorNested featureGenNested;
    public NestedCRF(int numLabels, FeatureGeneratorNested fgen, String arg) {
	super(numLabels,fgen,arg);
	featureGenNested = fgen;
    }
    public NestedCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
	super(numLabels,fgen,configOptions);
	featureGenNested = fgen;
    }
    double gammaArray[][];
    int winningEll[][];
    void allocateScratch() {
	super.allocateScratch();
	winningEll = new int[numY][];
	gammaArray = new double[numY][];
    }
    public void apply(DataSequence dataSeq) {
	if (params.debugLvl > 2) 
	    Util.printDbg("NestedCRF: Applying on " + dataSeq);

	if (gammaArray == null) {
	    allocateScratch();
	}
	if ((winningLabel[0] == null) || (winningLabel[0].length < dataSeq.length())) {
	    for (int yi = 0; yi < winningLabel.length; 
		 winningLabel[yi++]=new int[dataSeq.length()]);
	    for (int yi = 0; yi < winningEll.length; 
		 winningEll[yi++]=new int[dataSeq.length()]);
	    for (int yi = 0; yi < gammaArray.length; 
		 gammaArray[yi++]=new double[dataSeq.length()]);
	}	
	for (int i = 0; i < dataSeq.length(); i++) {
	    for (int y = 0; y < numY; gammaArray[y][i] = Double.NEGATIVE_INFINITY,y++);
	}
	for (int i = 0; i < dataSeq.length(); i++) {
	    for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i-ell >= -1); ell++) {
		if (params.debugLvl > 1) 
		    Util.printDbg("Getting features for " + (i-ell) + " to " + i);
		// compute Mi.
		featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
		//  if (!featureGenNested.hasNext())
		//    break;
		computeLogMi(featureGenNested,lambda,Mi,Ri,false);
		if (params.debugLvl > 1) {
		    System.out.println("Ri " + Ri.toString());
		    System.out.println("Mi " + Mi.toString());
		}
		for (int yi = 0; yi < numY; yi++) {
		    if (i-ell < 0) {
			double val = Ri.get(yi);
			if (val > gammaArray[yi][i]) {
			    gammaArray[yi][i] = val;
			    winningLabel[yi][i] = -1;
			    winningEll[yi][i] = ell;
			}
		    } else {
			for (int yp = 0; yp < numY; yp++) {
			    double val = gammaArray[yp][i-ell]+Mi.get(yp,yi)+Ri.get(yi);
			    // System.out.println("new val at " + yp + " " + yi + " " + i + " " + val);
			    if (gammaArray[yi][i] < val) {
				gammaArray[yi][i] = val;
				winningLabel[yi][i] = yp;
				winningEll[yi][i] = ell;
			    } 
			}
		    }
		    if (params.debugLvl > 1) 
			Util.printDbg(i +" y " + yi + " gamma " + gammaArray[yi][i] + " winningEll " + winningEll[yi][i]);
		}
	    }
	}
	// find best gamma..
	int ybest = 0;
	int lastI =  dataSeq.length()-1;
	for (int yInd = 0; yInd < numY; yInd++) {
	    if (gammaArray[ybest][lastI] < gammaArray[yInd][lastI])
		ybest = yInd;
	}
	for(int i = dataSeq.length()-1; i >= 0; ) {
	    for (int l = i; l > i-winningEll[ybest][i]; l--) {
		dataSeq.set_y(l, ybest);
	    }
	    int ybestTmp = winningLabel[ybest][i];
	    assert (winningEll[ybest][i] > 0);
	    i -= winningEll[ybest][i];
	    ybest = ybestTmp;
	}
	if (params.debugLvl > 1)
	    Util.printDbg("Returning sequence labels ");
    }
};
