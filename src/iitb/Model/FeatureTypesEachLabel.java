package iitb.Model;
/**
 * This can be used as a wrapper around a FeatureTypes class that wants to
 * generate a feature for each label. 
 */
public class  FeatureTypesEachLabel extends FeatureTypes {
    FeatureTypes single;
    int numStates;
    int stateId;
    FeatureImpl featureImpl;
    public FeatureTypesEachLabel(Model m, FeatureTypes single) {
	super(m);
	numStates = model.numStates();
	this.single = single;
	featureImpl = new FeatureImpl();
    }
    boolean advance() {
	stateId++;
	if (stateId < numStates)
	    return true;
	if (single.hasNext()) {
	    single.next(featureImpl);
	    stateId = 0;
	} else {
	    stateId=numStates;
	    return false;
	}
	return true;
    }
    public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
	stateId = numStates;
	single.startScanFeaturesAt(data,prevPos,pos);
	return advance();
    }
    public boolean hasNext() {
	return (stateId  < numStates);
    }
    public  void next(iitb.Model.FeatureImpl f) {
	f.copy(featureImpl);
	f.yend = stateId;
	single.setFeatureIdentifier(featureImpl.strId.id*numStates+stateId, stateId, featureImpl.strId.name, f);
	advance();
    }
};
	
