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
    NestedViterbi nestedViterbi;
    public NestedCRF(int numLabels, FeatureGeneratorNested fgen, String arg) {
	super(numLabels,fgen,arg);
	featureGenNested = fgen;
    }
    public NestedCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
	super(numLabels,fgen,configOptions);
	featureGenNested = fgen;
    }
    protected Trainer getTrainer() {
	nestedViterbi = new NestedViterbi(this,1);
	if (params.trainerType.startsWith("SegmentCollins"))
	    return new NestedCollinsTrainer(params);
	return new NestedTrainer(params);
    }
    public void apply(SegmentDataSequence dataSeq) {
	if (params.debugLvl > 2) 
	    Util.printDbg("NestedCRF: Applying on " + dataSeq);
	nestedViterbi.bestLabelSequence(dataSeq,lambda);
    }
};
