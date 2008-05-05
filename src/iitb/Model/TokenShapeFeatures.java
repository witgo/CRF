/*
 * Created on May 4, 2008
 * @author sunita
 */
package iitb.Model;

import iitb.CRF.DataSequence;

public class TokenShapeFeatures extends FeatureTypes {

    public TokenShapeFeatures(FeatureGenImpl fgen) {
        super(fgen);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void next(FeatureImpl f) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        // TODO Auto-generated method stub
        return false;
    }

}
