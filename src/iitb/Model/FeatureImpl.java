package iitb.Model;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 
public class FeatureImpl implements Feature {
    public String type;
    public FeatureIdentifier strId = new FeatureIdentifier();
    public int id;
    public int ystart, yend;
    public float val = 1;
    public int historyArray[] = null;
    void init() {
	val = 1;
	historyArray = null;
	ystart = -1;
	id = 0;
    }
    public FeatureImpl() {;}
    public FeatureImpl(FeatureImpl f) {copy(f);}
    public void copy(Feature featureToReturn) {
    	id = featureToReturn.index();
	ystart = featureToReturn.yprev();
	yend = featureToReturn.y();
	val = featureToReturn.value();
	historyArray = featureToReturn.yprevArray();
    }
    public void copy(FeatureImpl featureToReturn) {
	copy((Feature)featureToReturn);
	strId.copy(featureToReturn.strId);
	type = featureToReturn.type;
    }
    public int index() {return id;} 
    public int y() {return yend;}
    public int yprev() {return ystart;}
    public float value() {return val;}
    public String toString() {return strId + " " + val;}
    public FeatureIdentifier identifier() {return strId;}
    public int[] yprevArray() {return historyArray;}
};

