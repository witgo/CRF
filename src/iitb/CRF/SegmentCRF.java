/*
 * Created on Nov 21, 2004
 *
 * This is a version of the CRF model that applies the semi-markov
 * model on data where the candidate segments are provided by the dataset.
 */
package iitb.CRF;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntFloatHashMap;

/**
 * @author sunita
 *
 */
public class SegmentCRF extends CRF {
	FeatureGeneratorNested featureGenNested;
	transient SegmentViterbi segmentViterbi;
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, String arg) {
		super(numLabels,fgen,arg);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
	}
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
		super(numLabels,fgen,configOptions);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
	}
	public interface ModelGraph {
	    public int numStates();
	    public void stateMappingGivenLength(int label, int len, TIntArrayList stateIds) 
	    throws Exception;
	};
	/*
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, FeatureGenerator markovGen, 
	        ModelGraph modelGraph, java.util.Properties configOptions) {
		super(numLabels,fgen,configOptions);
		combinedFeatureCache = new CombinedFeatureCache(numLabels,fgen,markovGen, modelGraph);
		featureGenNested = combinedFeatureCache;
		segmentViterbi = new SegmentViterbi(this,1);
	}*/
	protected Trainer getTrainer() {
		if (params.trainerType.startsWith("SegmentCollins"))
			return new NestedCollinsTrainer(params);
		return new SegmentTrainer(params);
	}
	protected Viterbi getViterbi(int beamsize) {
	    if (params.miscOptions.getProperty("featuresWithBounds") != null)
	        return new OptimizedSegmentViterbi(this,beamsize);
		return new SegmentViterbi(this,beamsize);
	}
	public void apply(DataSequence dataSeq) {
		apply((CandSegDataSequence)dataSeq);
	}
	public void apply(CandSegDataSequence dataSeq) {
	    if (segmentViterbi==null)
	        segmentViterbi = new SegmentViterbi(this,1);
		if (params.debugLvl > 2) 
			Util.printDbg("SegmentCRF: Applying on " + dataSeq);
		segmentViterbi.bestLabelSequence(dataSeq,lambda);
	}
	public double score(DataSequence dataSeq) {
	    if (segmentViterbi==null)
	        segmentViterbi = new SegmentViterbi(this,1);
		return segmentViterbi.viterbiSearch(dataSeq,lambda,true);
	}
	public void singleSegmentClassScores(CandSegDataSequence dataSeq, TIntFloatHashMap scores) {
	    if (segmentViterbi==null)
	        segmentViterbi = new SegmentViterbi(this,1);
		segmentViterbi.singleSegmentClassScores(dataSeq,lambda,scores); 
	}
}
