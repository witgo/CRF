/*
 * Created on Nov 21, 2004
 *
 * This is a version of the CRF model that applies the semi-markov
 * model on data where the candidate segments are provided by the dataset.
 */
package iitb.CRF;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntFloatHashMap;

/**
 * @author sunita
 *
 */
public class SegmentCRF extends CRF {
	protected FeatureGeneratorNested featureGenNested;
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

	protected Trainer getTrainer() {
        Trainer thisTrainer = dynamicallyLoadedTrainer();
        if (thisTrainer != null)
            return thisTrainer;
		if (params.trainerType.startsWith("SegmentCollins"))
			return new NestedCollinsTrainer(params);
		return new SegmentTrainer(params);
	}
	public Viterbi getViterbi(int beamsize) {
		return new SegmentViterbi(this,beamsize);
	}
    public void apply(CandSegDataSequence dataSeq, int rank) {
      System.out.println("Not implemented yet");
    }
	public double apply(DataSequence dataSeq) {
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, 1);
	        return segmentAStar.bestLabelSequence((CandSegDataSequence)dataSeq, lambda);
	    }else{//default
            return super.apply(dataSeq);
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
     public double segmentMarginalProbabilities(DataSequence dataSequence, TIntDoubleHashMap segmentMarginals[][]) {
            if (trainer==null) {
                trainer = getTrainer();
                trainer.init(this,null,lambda);
            }
            return -1*((SegmentTrainer)trainer).sumProductInner(dataSequence,featureGenerator,lambda,null,false, -1, null,segmentMarginals);
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
