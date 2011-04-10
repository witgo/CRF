package iitb.Model;
import java.io.Serializable;

import javax.net.ssl.HostnameVerifier;

import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 
public class FeatureImpl implements Feature, Serializable {
    public String type;
    public FeatureIdentifier strId = new FeatureIdentifier();
    public int id;
    public int ystart, yend;
    public float val = 1;
    public int historyArray[] = null;
    public void init() {
        val = 1;
        historyArray = null;
        ystart = -1;
        id = 0;
    }
    public FeatureImpl() {init();}
    public FeatureImpl(FeatureImpl f) {copy(f);}
    public FeatureImpl(Feature f) {copy(f);}
    public FeatureImpl(int id, int ystart, int yend, float val) {
        this.id = id;
        this.ystart = ystart;
        this.yend = yend;
        this.val = val;
    }
    public void copy(Feature featureToReturn) {
        id = featureToReturn.index();
        ystart = featureToReturn.yprev();
        yend = featureToReturn.y();
        val = featureToReturn.value();
        int arr[] = featureToReturn.yprevArray();
        if (arr == null && historyArray != null)
        	historyArray = null;
        else if (arr != null) {
        	if (historyArray == null || historyArray.length != arr.length)
        		historyArray = arr.clone();
        	else {
        		for (int i = 0; i < arr.length; i++) {
					historyArray[i] = arr[i];
				}
        	}
        }
    }
    public void copy(FeatureImpl featureToReturn) {
        copy((Feature)featureToReturn);
        strId.copy(featureToReturn.strId);
        type = featureToReturn.type;
    }
    public FeatureImpl(int id, FeatureIdentifier strId) {
        this.id = id;
        this.strId = strId;
    }
    public int index() {return id;} 
    public int y() {return yend;}
    public int yprev() {return ystart;}
    public float value() {return val;}
    public String toString() {return strId + " " + val;}
    public FeatureIdentifier identifier() {return strId;}
    public int[] yprevArray() {return historyArray;}
    public Object clone() {
        return new FeatureImpl(this);
    }
};

