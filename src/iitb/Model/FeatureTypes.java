package iitb.Model;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import iitb.CRF.*;
/**
 *
 * Inherit from the FeatureTypes class for creating any kind of
 * feature. You will see various derived classes from them,
 * EdgeFeatures, StartFeatures, etc, etc.  The ".id" field of
 * FeatureImpl does not need to be set by the FEatureTypes.next()
 * methods.
 *
 * @author Sunita Sarawagi
 */

public abstract class FeatureTypes implements Serializable {
    static int offset = 0;
    int thisTypeId;
    public static boolean featureCollectMode = false;
    public Model model;
    public FeatureTypes(Model m) {
	model = m;
	thisTypeId = offset++;
    }
    public boolean requiresTraining(){return false;}
    public void train(DataSequence data, int pos) {;}
    public  boolean startScanFeaturesAt(DataSequence data, int pos) {
	return startScanFeaturesAt(data,pos-1,pos);
    }
    public abstract boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos);
    public abstract boolean hasNext();
    public abstract void next(FeatureImpl f);
    public void setFeatureIdentifier(int fId, int stateId, String name, FeatureImpl f) {
	setFeatureIdentifier( fId,  stateId, (Object)name,  f);
    }
    public void setFeatureIdentifier(int fId, int stateId, Object name, FeatureImpl f) {
	f.strId.init(fId*offset + thisTypeId,stateId,name);
    }
    public void setFeatureIdentifier(int fId, FeatureImpl f) {
	f.strId.init(fId*offset + thisTypeId);
    }
    int labelIndependentId(FeatureImpl f) {
	return ((f.strId.id-thisTypeId)-f.strId.stateId*offset)/model.numStates()+thisTypeId;
    }
    int offsetLabelIndependentId(FeatureImpl f) {
    	return (labelIndependentId(f)-thisTypeId)/offset;
    }
    public void print(FeatureGenImpl.FeatureMap strToInt, double crfWs[]) {;}
    public int maxFeatureId() {return Integer.MAX_VALUE;}
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
        s.defaultReadObject();
        offset = Math.max(offset,thisTypeId+1);
    }
};

