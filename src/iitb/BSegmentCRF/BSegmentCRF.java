/*
 * Created on Apr 14, 2005
 *
 */
package iitb.BSegmentCRF;

import java.util.Properties;

import iitb.CRF.*;

/**
*
* BSegmentCRF (A significantly faster version of Semi-CRFs that employs a compact feature representation) 
* for fast training and inference of semi-Markov models.
*  
* @author Sunita Sarawagi
*
*/ 

public class BSegmentCRF extends SegmentCRF {
    BFeatureGenerator bfgen;
    /**
     * @param numLabels
     * @param fgen
     * @param arg
     */
    public BSegmentCRF(int numLabels, BFeatureGenerator fgen, String arg) {
        super(numLabels, fgen, arg);
        bfgen = fgen;
    }
    /**
     * @param numLabels
     * @param fgen
     * @param configOptions
     */
    public BSegmentCRF(int numLabels, BFeatureGenerator fgen,
            Properties configOptions) {
        super(numLabels, fgen, configOptions);
        bfgen = fgen;
    }
    protected Trainer getTrainer() {
        return new BSegmentTrainer(params);
    }
    
    public Viterbi getViterbi(int beamsize) {
        return params.miscOptions.getProperty("segmentViterbi", "false").equals("true")?
                super.getViterbi(beamsize):new BSegmentViterbi(this,numY,beamsize);
    }
}
