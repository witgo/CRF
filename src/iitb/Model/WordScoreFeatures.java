package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;

/**
 * These return one feature per state.  The value of the feature is the
 * fraction of training instances passing through this state that contain
 * the word
 *
 * @author Sunita Sarawagi
 */ 
public class WordScoreFeatures extends FeatureTypes {
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


