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
    Object fname;
    public StartFeatures(FeatureGenImpl m) {
	super(m);
	fname = "S.";
    }
    public StartFeatures(FeatureGenImpl m, Object name) {
	super(m);
	fname=name;
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
	setFeatureIdentifier(stateId,stateId,fname,f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	startStateNum++;
	stateId = model.startState(startStateNum);	
    }
};
