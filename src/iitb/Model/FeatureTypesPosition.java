/*
 * Created on Mar 21, 2005
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * @author sunita
 *
 */
public class FeatureTypesPosition extends FeatureTypes {
    /**
     * @param ftype
     */
    FeatureTypes ftype;
    boolean squareSent;
    int segStart;
    int segEnd;
    int currPos;
    FeatureImpl savedFeature = new FeatureImpl();
    int dataLen;
    transient DataSequence dataSeq;
    public FeatureTypesPosition(FeatureGenImpl fgen, FeatureTypes ftype) {
        super(fgen);
        this.ftype = ftype;
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
        segStart = prevPos+1;
        segEnd = pos;
        currPos = prevPos+1;
        squareSent=true;
        dataSeq = data;
        dataLen = data.length();
        ftype.startScanFeaturesAt(data,prevPos,prevPos+1);
        advance();
        return ftype.hasNext();
    }
    public boolean hasNext() {
        return !squareSent || ((currPos <= segEnd) && ftype.hasNext());
    }
    public void next(FeatureImpl f) {
        if (!squareSent) {
            squareSent = true;
            f.copy(savedFeature);
            // saved feature with value change to square.
            f.val *= f.val;
            advance();
           
            String name="";
            if (featureCollectMode()) {
                name = "POS^2" + f.strId.name;
            }
            setFeatureIdentifier(f.strId.id*2+1,f.strId.stateId, name, f);
        } else {
            ftype.next(f);
            f.val = (float)(currPos-segStart+1)/(segEnd-segStart+1); //dataLen;
            savedFeature.copy(f);
            squareSent = false;
            int bin = (int)(f.val*10);
            //f.val = 1;
            //int fid = f.strId.id*10+bin;
            int fid = f.strId.id;
            String name="";
            if (featureCollectMode()) {
                name = "POS_" + f.strId.name;
            }
            setFeatureIdentifier(fid, f.strId.stateId, name, f);
        }
        //if (featureCollectMode())
        //    System.out.println(f + " " + f.val);
    }
    public boolean requiresTraining() {
		return ftype.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		ftype.train(data, pos);
	}
	int labelIndependentId(FeatureImpl f) {
		return ftype.labelIndependentId(f);
	}
	public int maxFeatureId() {
		return ftype.maxFeatureId()*2;
	}
}
