package iitb.MaxentClassifier;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class FeatureGenRecord implements FeatureGenerator {
    int numCols;
    int numLabels;
    DataRecord dataRecord;
    class FeatureColumn implements Feature {
	int colId;
	int _y;
	float val;
	void copy(FeatureColumn f) {
	    colId = f.colId;
	    _y = f._y;
	    val = f.val;
	}
	public int index() {return colId + _y*numCols;}
	public int y() {return _y;}
	public int yprev() {return -1;}
	public float value() {return val;}
    };
    FeatureColumn feature, featureToReturn;
    FeatureGenRecord(int numColumns, int numYs) {
	numCols =  numColumns;
	numLabels = numYs;
	feature = new FeatureColumn();
	featureToReturn = new FeatureColumn(); 
    }
    public int numFeatures() {return numCols*numLabels;}
    public void startScanFeaturesAt(DataSequence data, int pos) {
	dataRecord = (DataRecord)data;
	assert (pos == 0);
	feature.colId = 0;
	feature._y = 0;
    }
    public boolean hasNext() {
	return (feature.y() < numLabels);
    }
    public Feature next() {
	featureToReturn.copy(feature);
	feature.colId++;
	if (feature.colId >= numCols) {
	    feature.colId = 0;
	    feature._y++;
	}
	featureToReturn.val = dataRecord.getColumn(featureToReturn.colId);
	return featureToReturn;
    }
};
