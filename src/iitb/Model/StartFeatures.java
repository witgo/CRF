package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 



public class StartFeatures  extends FeatureTypes {
    int stateId;
    int startStateNum;
    public StartFeatures(Model m) {
	super(m);
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	if (prevPos >= 0) {
	    stateId = -1;
	    return false;
	} else {
	    startStateNum = 0;
	    stateId = model.startState(startStateNum);	
	    return true;
	}
    }
    public boolean hasNext() {
	return (stateId >= 0);
    }
    public void next(FeatureImpl f) {
	f.type = "Start";
	f.strId = "S"+stateId;
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	startStateNum++;
	stateId = model.startState(startStateNum);	
    }
};
