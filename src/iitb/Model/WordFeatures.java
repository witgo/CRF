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
    protected int stateId;
    int statePos;
    Object token;
    int tokenId;
    protected WordsInTrain dict;
    int _numWordStatePairs;
    public static int RARE_THRESHOLD=0;
    protected int frequency_cutOff;
    public WordFeatures(FeatureGenImpl m, WordsInTrain d) {
	super(m);
	dict = d;
    frequency_cutOff=RARE_THRESHOLD;
    }
    public WordFeatures(FeatureGenImpl m, WordsInTrain d, int freqCuttOff) {
        this(m,d);
        if (freqCuttOff >= 0) frequency_cutOff=freqCuttOff;
    }
    private void nextStateId() {       
	stateId = dict.nextStateWithWord(token, stateId);
	statePos++;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	stateId = -1;
	if (dict.count(data.x(pos)) > frequency_cutOff) {
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
        if (featureCollectMode())
            setFeatureIdentifier(tokenId*model.numStates()+stateId,stateId,name()+token,f);
        else
            setFeatureIdentifier(tokenId*model.numStates()+stateId,stateId,token,f); 
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	nextStateId();
    }
	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#maxFeatureId()
	 */
	public int maxFeatureId() {
		return dict.dictionaryLength()*model.numStates();
	}
    public String name() {
        return "W_";
    }
};


