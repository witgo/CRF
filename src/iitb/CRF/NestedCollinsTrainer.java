package iitb.CRF;

import java.util.*;
import riso.numerical.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
/**
 * Implements the CollinsVotedPerceptron training algorithm
 *
 * @author Sunita Sarawagi
 *
 */ 

class NestedCollinsTrainer extends CollinsTrainer {
    public NestedCollinsTrainer(CrfParams p) {
	super(p);
    }
    int getSegmentEnd(DataSequence dataSeq, int ss) {
	return ((SegmentDataSequence)dataSeq).getSegmentEnd(ss);
    }
    void startFeatureGenerator(FeatureGenerator featureGenerator, DataSequence dataSeq, Soln soln) {
	((FeatureGeneratorNested)featureGenerator).startScanFeaturesAt(dataSeq, soln.prevPos(), soln.pos);
    }
    Viterbi getViterbi(CRF model) {
	NestedViterbi viterbiSearcher = new NestedViterbi((NestedCRF)model, beamsize);
	return viterbiSearcher;
    }
};
