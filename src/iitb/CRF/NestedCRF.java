package iitb.CRF;

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
	nestedViterbi = new NestedViterbi(this,1);
    }
    public NestedCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
	super(numLabels,fgen,configOptions);
	featureGenNested = fgen;
	nestedViterbi = new NestedViterbi(this,1);
    }
    protected Trainer getTrainer() {
	if (params.trainerType.startsWith("SegmentCollins"))
	    return new NestedCollinsTrainer(params);
	return new NestedTrainer(params);
    }
    public void apply(DataSequence dataSeq) {
    	apply((SegmentDataSequence)dataSeq);
    }
    public void apply(SegmentDataSequence dataSeq) {
	if (params.debugLvl > 2) 
	    Util.printDbg("NestedCRF: Applying on " + dataSeq);
	nestedViterbi.bestLabelSequence(dataSeq,lambda);
    }
};
