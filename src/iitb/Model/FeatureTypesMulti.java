/*
 * Created on Dec 4, 2004
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * @author Sunita Sarawagi
 *
 */

/*
 * Implements the bag of features model for a given input sequence
 */
public class FeatureTypesMulti extends FeatureTypesWrapper {
    private static final long serialVersionUID = 10L;
    int currPos;
    int segEnd;
    transient DataSequence dataSeq;
    
    public FeatureTypesMulti(FeatureTypes s) {
        super(s);
    }
    void advance() {
        while (true) {
            if (ftype.hasNext())
                return;
            currPos++;
            if (currPos > segEnd)
                return;
            ftype.startScanFeaturesAt(dataSeq,currPos-1,currPos);
        }
    }
    public  boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        currPos = prevPos+1;
        segEnd = pos;
        dataSeq = data;
        ftype.startScanFeaturesAt(data,prevPos,prevPos+1);
        advance();
        return ftype.hasNext();
    }
    public boolean hasNext() {
        return (currPos <= segEnd) && ftype.hasNext();
    }
    public void next(FeatureImpl f) {
        ftype.next(f);
        advance();
    }
};


