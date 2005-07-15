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
	transient SegmentAStar segmentAStar;
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, String arg) {
		super(numLabels,fgen,arg);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
		segmentAStar = new SegmentAStar(this, 1);
	}
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
		super(numLabels,fgen,configOptions);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
		segmentAStar = new SegmentAStar(this, 1);
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
		return new SegmentViterbi(this,beamsize);
	}

	public void apply(CandSegDataSequence dataSeq, int rank) {
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, 1);
	        segmentAStar.bestLabelSequence(dataSeq, lambda);
	    }else{//default
		    if (segmentViterbi==null)
		        segmentViterbi = new SegmentViterbi(this,1);
		    segmentViterbi.bestLabelSequence(dataSeq,lambda);
	    }
	}

	public void apply(DataSequence dataSeq) {
	    apply((CandSegDataSequence)dataSeq);
	}
	public void apply(CandSegDataSequence dataSeq) {
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, 1);
	        segmentAStar.bestLabelSequence(dataSeq, lambda);
	    }else{//default
		    if (segmentViterbi==null)
		        segmentViterbi = new SegmentViterbi(this,1);
		    segmentViterbi.bestLabelSequence(dataSeq,lambda);
	    }
	}

	public void singleSegmentClassScores(CandSegDataSequence dataSeq, TIntFloatHashMap scores) {
	    if (segmentViterbi==null)
	        segmentViterbi = (SegmentViterbi)getViterbi(1);
		segmentViterbi.singleSegmentClassScores(dataSeq,lambda,scores); 
	}
	 public Segmentation[] segmentSequences(CandSegDataSequence dataSeq, int numLabelSeqs) {
	     return segmentSequences(dataSeq,numLabelSeqs,null);
	 }
	 public Segmentation[] segmentSequences(CandSegDataSequence dataSeq, int numLabelSeqs, double scores[]) {
	     if ((segmentViterbi==null) || (segmentViterbi.beamsize < numLabelSeqs))
		        segmentViterbi = (SegmentViterbi)getViterbi(numLabelSeqs);
	     return segmentViterbi.segmentSequences(dataSeq,lambda,numLabelSeqs,scores);
	 }
	 
	/*
	public void apply(DataSequence dataSeq) {
		apply((CandSegDataSequence)dataSeq);
	}
	
	public void apply(CandSegDataSequence dataSeq) {
		if (params.debugLvl > 2) 
			Util.printDbg("SegmentCRF: Applying on " + dataSeq);
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, params.beamSize);
	        segmentAStar.bestLabelSequence(dataSeq, lambda);
	    }else{
		    if (segmentViterbi==null)
		        segmentViterbi = new SegmentViterbi(this,params.beamSize);
		    segmentViterbi.bestLabelSequence(dataSeq,lambda);
	    }
	}
	*/
	/*
	public double score(DataSequence dataSeq) {
	    if (segmentViterbi==null)
	        segmentViterbi = new SegmentViterbi(this,1);
		return segmentViterbi.viterbiSearch(dataSeq,lambda,true);
	}
	*/

}
