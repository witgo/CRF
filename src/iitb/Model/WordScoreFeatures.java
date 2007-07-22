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
    int wordCnt;
    int scoreType;
    int numScoreType=3;
    WordsInTrain dict;
    public WordScoreFeatures(FeatureGenImpl m, WordsInTrain d) {
	super(m);
	dict = d;
    }
    private void nextStateId() {
	stateId = dict.nextStateWithWord(wordPos, stateId);
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	stateId = -1;
    scoreType=0;
    wordCnt = dict.count(data.x(pos));
	if (wordCnt > WordFeatures.RARE_THRESHOLD) {
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
	if (featureCollectMode())
        setFeatureIdentifier(stateId*numScoreType+scoreType,stateId,"S_"+scoreType,f);
    else
        setFeatureIdentifier(stateId*numScoreType+scoreType,stateId,null,f);
	f.yend = stateId;
	f.ystart = -1;
    switch (scoreType) {
    case 0:
        f.val = (float) (1 + Math.log(1+Math.log(dict.count(wordPos,stateId))));
        break;
    case 1:
        f.val = (float) Math.log(1+(double)dict.count(wordPos,stateId)/(double)wordCnt);
        break;
    default:
        f.val = (float)Math.log(1+((double)dict.count(wordPos,stateId))/dict.count(stateId));
    //f.val = (float) (1 + Math.log(1+Math.log(dict.count(wordPos,stateId))));
    }
    //for (int s = 0; s < model.numStates(); s = dict.nextStateWithWord(wordPos, s)) {}
	// System.out.println(f.toString());
    if (scoreType<numScoreType-1) {
        scoreType++;
    } else {
        scoreType=0;
        nextStateId();
    }
    }
};


