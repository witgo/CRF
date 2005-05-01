package iitb.Model;
/**
 * This can be used as a wrapper around a FeatureTypes class that wants to
 * generate a feature for each label. 
 */
public class  FeatureTypesSegmentLength extends FeatureTypes {
    protected int segLen;
    protected int maxLen = Integer.MAX_VALUE;
    public FeatureTypesSegmentLength(FeatureGenImpl m) {
        super(m);
    }
    public FeatureTypesSegmentLength(FeatureGenImpl m, int maxSegLen) {
        super(m);
        maxLen = maxSegLen;
    }
    public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
        segLen = Math.min(pos-prevPos,maxLen);
        return true;
    }
    public boolean hasNext() {
        return segLen > 0;
    }
    public  void next(iitb.Model.FeatureImpl f) {
        f.val = 1;
        f.ystart = -1;
        if (featureCollectMode())
            f.strId.init(segLen, 0, "Length" + ((segLen==maxLen)?">=":"=") + segLen);
        else
            f.strId.init(segLen);
        segLen = 0;
    }
};

