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
	protected FeatureTypes ftype;
	/**
	 * @param m
	 */
	public FeatureTypesWrapper(FeatureTypes ftype) {
		super(ftype);
		this.ftype = ftype;
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		return ftype.startScanFeaturesAt(data,prevPos,pos);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return ftype.hasNext();
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
		ftype.next(f);
	}
	public boolean requiresTraining() {
		return ftype.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		ftype.train(data, pos);
	}
	int labelIndependentId(FeatureImpl f) {
		return ftype.labelIndependentId(f);
	}
	public int maxFeatureId() {
		return ftype.maxFeatureId();
	}
	int offsetLabelIndependentId(FeatureImpl f) {
		return ftype.offsetLabelIndependentId(f);
	}
	public void print(FeatureMap strToInt, double[] crfWs) {
		ftype.print(strToInt, crfWs);
	}
	public void setFeatureIdentifier(int fId, FeatureImpl f) {
		ftype.setFeatureIdentifier(fId, f);
	}
	public void setFeatureIdentifier(int fId, int stateId, Object name,
			FeatureImpl f) {
		ftype.setFeatureIdentifier(fId, stateId, name, f);
	}
	public void setFeatureIdentifier(int fId, int stateId, String name,
			FeatureImpl f) {
		ftype.setFeatureIdentifier(fId, stateId, name, f);
	}
	public boolean startScanFeaturesAt(DataSequence data, int pos) {
		return ftype.startScanFeaturesAt(data, pos);
	}
}
