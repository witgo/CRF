package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class UnknownFeature extends FeatureTypes {
    int stateId;
    WordsInTrain dict;
    public UnknownFeature(Model m, WordsInTrain d) {
	super(m);
	dict = d;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	if (dict.count(data.x(pos)) > WordFeatures.RARE_THRESHOLD+1) {
	    stateId = model.numStates();
	    return false;
	} else {
	    stateId = 0;
	    return true;
	}
    }
    public boolean hasNext() {
	return (stateId < model.numStates());
    }
    public void next(FeatureImpl f) {
	setFeatureIdentifier(stateId,stateId,"U",f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	stateId++;
    }
};


