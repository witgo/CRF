/*
 * Created on Dec 4, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/*
 * Implements the bag of features model for a given input sequence
 */
public class FeatureTypesMulti extends FeatureTypes {
    private static final long serialVersionUID = 10L;
	FeatureTypes single;
    int currPos;
    int segEnd;
    transient DataSequence dataSeq;

    public FeatureTypesMulti(FeatureTypes s) {
	super(null);
	single = s;
    }
    void advance() {
	while (true) {
	    if (single.hasNext())
		return;
	    currPos++;
	    if (currPos > segEnd)
		return;
	    single.startScanFeaturesAt(dataSeq,currPos-1,currPos);
	}
    }
    public  boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	currPos = prevPos+1;
	segEnd = pos;
	dataSeq = data;
	single.startScanFeaturesAt(data,prevPos,prevPos+1);
	advance();
	return single.hasNext();
    }
    public boolean hasNext() {
	return (currPos <= segEnd) && single.hasNext();
    }
    public void next(FeatureImpl f) {
	single.next(f);
	advance();
    }
};

  
