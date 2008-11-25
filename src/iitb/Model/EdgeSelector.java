/*
 * Created on Nov 9, 2008
 * @author sunita
 * 
 * Feature that can be used to decide if an edge should be added between two words.
 */
package iitb.Model;

import iitb.CRF.DataSequence;

public class EdgeSelector extends RegexCountFeatures {
    int windowSize=0;
    int segLen;
    public EdgeSelector(FeatureGenImpl fgen, int width, String patternFile) {
        super(fgen,2*width+2,patternFile);
        windowSize=width;
    }
    public EdgeSelector(FeatureGenImpl fgen,String patternFile) {
        this(fgen,0,patternFile);
    }
    public EdgeSelector(FeatureGenImpl fgen) {
        this(fgen,0,null);
    }
    @Override
    public boolean hasNext() {
        return (segLen > 1) && super.hasNext();
    }

    @Override
    public void next(FeatureImpl f) {
        f.val = (float)patternOccurence[index]/segLen;
        assert(f.val>0);
        f.strId.id =  (index+1);
        f.ystart = -1;
        if(featureCollectMode()){
            f.strId.name = name() + "_"+patternString[index][0];
            //System.out.println((String)f.strId.name +" " +index + " " + f.strId.id);
        }
        advance();
    }

    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        segLen = Math.min(pos+windowSize,data.length()-1)-Math.max(pos-windowSize-2,-1)+1;
        return super.startScanFeaturesAt(data, Math.max(pos-windowSize-2,-1), Math.min(pos+windowSize,data.length()-1));
    }
    @Override
    public int labelIndependentId(FeatureImpl f) {
        return f.id;
    }

    @Override
    public int maxFeatureId() {
        return (patternString.length+1);
    }

    @Override
    public String name() {
        return "EdgeSel";
    }
}
