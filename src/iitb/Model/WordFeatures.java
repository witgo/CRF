package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class WordFeatures extends FeatureTypes {
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


/**
 * These return one feature per state.  The value of the feature is the
 * fraction of training instances passing through this state that contain
 * the word
 */ 
class WordScoreFeatures extends FeatureTypes {
    int stateId;
    int wordPos;
    
    WordsInTrain dict;
    public WordScoreFeatures(Model m, WordsInTrain d) {
	super(m);
	dict = d;
    }
    private void nextStateId() {       
	stateId = dict.nextStateWithWord(wordPos, stateId);
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	stateId = -1;
	if (dict.count(data.x(pos)) > WordFeatures.RARE_THRESHOLD) {
	    Object token = (data.x(pos));
	    wordPos = dict.getIndex(token);
	    stateId = -1;
	    nextStateId();
	    return true;
	} 
	return false;
    }
    public boolean hasNext() {
	return (stateId < model.numStates()) && (stateId >= 0);
    }
    public void next(FeatureImpl f) {
	setFeatureIdentifier(stateId,stateId,"S",f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = (float)Math.log(((double)dict.count(wordPos,stateId))/dict.count(stateId));
	// System.out.println(f.toString());
	nextStateId();
    }
};


class KnownInOtherState extends FeatureTypes {
    int stateId;
    WordsInTrain dict;
    float wordFreq;
    int wordPos;
    public KnownInOtherState(Model m, WordsInTrain d) {
	super(m);
	dict = d;
    }
    void nextStateId() {
	for (stateId++; (stateId < model.numStates()); stateId++)
	    if (dict.count(wordPos,stateId) == 0)
		return;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	if (dict.count(data.x(pos)) <= WordFeatures.RARE_THRESHOLD+1) {
	    stateId = model.numStates();
	    return false;
	} else {
	    wordPos = dict.getIndex(data.x(pos));
	    stateId = -1;
	    nextStateId();
	    wordFreq = (float)Math.log((double)dict.count(data.x(pos))/dict.totalCount());
	    return true;
	}
    }
    public boolean hasNext() {
	return (stateId < model.numStates());
    }
    public void next(FeatureImpl f) {
	setFeatureIdentifier(stateId,stateId,"K",f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = wordFreq;
	nextStateId();
    }
};
    
class UnknownFeature extends FeatureTypes {
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


