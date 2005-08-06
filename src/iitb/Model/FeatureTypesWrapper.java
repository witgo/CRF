/*
 * Created on Jan 17, 2005
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureGenImpl.FeatureMap;

/**
 * @author Sunita
 *
 */
public class FeatureTypesWrapper extends FeatureTypes {
	protected FeatureTypes single;
	/**
	 * @param m
	 */
	public FeatureTypesWrapper(FeatureTypes ftype) {
		super(ftype);
		this.single = ftype;
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		return single.startScanFeaturesAt(data,prevPos,pos);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return single.hasNext();
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
		single.next(f);
	}
	/*public void print(FeatureMap strToInt, double[] crfWs) {
		ftype.print(strToInt, crfWs);
	}
	*/
	int labelIndependentId(FeatureImpl f) {
		return single.labelIndependentId(f);
	}
	public int maxFeatureId() {
		return single.maxFeatureId();
	}
	int offsetLabelIndependentId(FeatureImpl f) {
		return single.offsetLabelIndependentId(f);
	}
	public void setFeatureIdentifier(int fId, FeatureImpl f) {
		single.setFeatureIdentifier(fId, f);
	}
	public void setFeatureIdentifier(int fId, int stateId, Object name,
			FeatureImpl f) {
		single.setFeatureIdentifier(fId, stateId, name, f);
	}
	public void setFeatureIdentifier(int fId, int stateId, String name,
			FeatureImpl f) {
		single.setFeatureIdentifier(fId, stateId, name, f);
	}
	public boolean requiresTraining() {
		return single.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		single.train(data, pos);
	}
	 public boolean fixedTransitionFeatures() {
	        return single.fixedTransitionFeatures();
	 }
	 public boolean needsCaching() {
	        return single.needsCaching();
	 }
}
