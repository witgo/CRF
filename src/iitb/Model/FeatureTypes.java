package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * Inherit from the FeatureTypes class for creating any kind of
 * feature. You will see various derived classes from them,
 * EdgeFeatures, StartFeatures, etc, etc.  The ".id" field of
 * FeatureImpl does not need to be set by the FEatureTypes.next()
 * methods.
 *
 * @author Sunita Sarawagi
 */

public abstract class FeatureTypes {
    int offset;
    public Model model;
    public FeatureTypes(Model m) {
	model = m;
    }
    public  boolean startScanFeaturesAt(DataSequence data, int pos) {
	return startScanFeaturesAt(data,pos-1,pos);
    }
    public abstract boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos);
    public abstract boolean hasNext();
    public abstract void next(FeatureImpl f);
};


class FeatureTypesMulti extends FeatureTypes {
    FeatureTypes single;
    int currPos;
    int maxPos;
    DataSequence dataSeq;

    public FeatureTypesMulti(FeatureTypes s) {
	super(null);
	single = s;
    }
    void advance() {
	while (true) {
	    if (single.hasNext())
		return;
	    currPos++;
	    if (currPos > maxPos)
		return;
	    single.startScanFeaturesAt(dataSeq,currPos-1,currPos);
	}
    }
    public  boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	currPos = prevPos+1;
	maxPos = pos;
	dataSeq = data;
	single.startScanFeaturesAt(data,prevPos,prevPos+1);
	advance();
	return single.hasNext();
    }
    public boolean hasNext() {
	return (currPos <= maxPos) && single.hasNext();
    }
    public void next(FeatureImpl f) {
	single.next(f);
	advance();
    }
};

  
