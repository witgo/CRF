package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class WordFeatures extends FeatureTypes {
    int stateId;
    int statePos;
    Object token;
    int tokenId;
    WordsInTrain dict;
    int _numWordStatePairs;
    public static int RARE_THRESHOLD=0;
    public WordFeatures(Model m, WordsInTrain d) {
	super(m);
	dict = d;
    }
    private void nextStateId() {       
	stateId = dict.nextStateWithWord(token, stateId);
	statePos++;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	stateId = -1;
	if (dict.count(data.x(pos)) > RARE_THRESHOLD) {
	    token = (data.x(pos));
	    tokenId = dict.getIndex(token);
	    statePos = -1;
	    nextStateId();
	    return true;
	} 
	return false;
    }
    public boolean hasNext() {
	return (stateId != -1);
    }
    public void next(FeatureImpl f) {
	setFeatureIdentifier(tokenId*model.numStates()+stateId,stateId,token,f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	nextStateId();
    }
};


