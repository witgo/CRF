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
    int thisTypeId;
    private FeatureGenImpl fgen;
    public Model model;
    public boolean cache = false;
    public FeatureTypes(FeatureGenImpl fgen) {
        model = fgen.model;
        this.fgen = fgen;
        thisTypeId = fgen.numFeatureTypes++;
    }
    /**
     * @param s
     */
    public FeatureTypes(FeatureTypes s) {
        this(s.fgen);
        thisTypeId = s.thisTypeId;
        fgen.numFeatureTypes--;
        
    }
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
        f.strId.init(fId*fgen.numFeatureTypes + thisTypeId,stateId,name);
    }
    public void setFeatureIdentifier(int fId, FeatureImpl f) {
        f.strId.init(fId*fgen.numFeatureTypes + thisTypeId);
    }
    public int labelIndependentId(FeatureImpl f) {
        return ((f.strId.id-thisTypeId)-f.strId.stateId*fgen.numFeatureTypes)/model.numStates()+thisTypeId;
    }
    int offsetLabelIndependentId(FeatureImpl f) {
        return (labelIndependentId(f)-thisTypeId)/fgen.numFeatureTypes;
    }
    public static int featureTypeId(FeatureImpl f, FeatureGenImpl fgen) {
        return featureTypeId(f.strId,fgen);
    }
    public static int featureTypeId(FeatureIdentifier strId, FeatureGenImpl fgen) {
        return strId.id % fgen.numFeatureTypes;
    }
    //public void print(FeatureGenImpl.FeatureMap strToInt, double crfWs[]) {;}
    public int maxFeatureId() {
		LogMessage.issueWarning("WARNING : Class " + getClass().getName() + " does not implement maxFeatureId(). Returning default value. Please refer to the documentation.");
		return Integer.MAX_VALUE;
	}
    /*  private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
     s.defaultReadObject();
     offset = Math.max(fgen.numFeatureTypes,thisTypeId+1);
     }
     */
    public int getTypeId() {return thisTypeId;}
    public boolean featureCollectMode() {return fgen.featureCollectMode;}
    // returns false if transition features change with x or position
    public boolean fixedTransitionFeatures() {
        return true;
    }
    public boolean requiresTraining(){return false;}
    public void train(DataSequence data, int pos) {
		if(requiresTraining()) {
			LogMessage.issueWarning("WARNING : Class " + getClass().getName() + " does not implement the train(DataSequence, int) method. Please implement the train() methods properly.");
		}
	}	
    /**
     * Training for semi-Markov features
     * @param sequence
     * @param segStart
     * @param segEnd
     */
    public void train(SegmentDataSequence sequence, int segStart, int segEnd) {
		if(requiresTraining()) {
			LogMessage.issueWarning("WARNING : Class " + getClass().getName() + " does not implement the train(SegmentDataSequence, int, int) method. Calling train(DataSequence, int) instead. Please implement the train() methods properly.");
			train((DataSequence)sequence, segEnd);
        }
	}	
    /**
     * @return
     */
    public boolean needsCaching() {
        return cache;
    }
};

