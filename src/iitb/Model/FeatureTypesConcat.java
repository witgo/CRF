/*
 * Created on Dec 6, 2004
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;
import iitb.Utils.*;
/**
 * @author Administrator
 *
 */
public class FeatureTypesConcat extends FeatureTypes {
	FeatureTypes single;
	int numBits = 0;
	FeatureImpl feature = new FeatureImpl();
	private static final long serialVersionUID = 612L;
	private int maxConcatLength;
	
	/**
	 * @param m
	 */
	public FeatureTypesConcat(Model model, FeatureTypes single, int maxMemory) {
		super(model);
		this.single = single;
		// TODO: set this properly for different classes.
		int maxId = single.maxFeatureId()+1; // for the feature not firing.
		numBits = Utils.log2Ceil(maxId);
		thisTypeId = single.thisTypeId;
		maxConcatLength = maxMemory;
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		int bitMap = 0;
		String name = "";
		for (int i = 0; (i < pos-prevPos) && (i < maxConcatLength); i++) {
			if (single.startScanFeaturesAt(data,pos-i-1,pos-i) && single.hasNext()) {
				single.next(feature);
				int thisId = offsetLabelIndependentId(feature)+1;
				bitMap = bitMap | (thisId << i*numBits);
				if (featureCollectMode) {
					name = feature.strId.name + "." + name;
					if (thisId > (1 << numBits)) {
						System.out.println("Error in max-feature-id value " + feature);
					}
					if (single.hasNext()) {
						System.out.println("FeatureTypesConcat: Taking only the first feature: others to be ignored");
					}
				}
			}
		}
		setFeatureIdentifier(bitMap,feature.strId.stateId,name,feature);
		return (bitMap != 0);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return feature.strId.id > 0;
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
		f.copy(feature);
		feature.strId.id = -1;
	}

	public boolean requiresTraining() {
		return single.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		single.train(data, pos);
	}
}
