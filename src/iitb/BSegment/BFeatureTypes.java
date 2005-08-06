/*
 * Created on Apr 12, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureGenImpl;
import iitb.Model.FeatureImpl;
import iitb.Model.FeatureTypes;

/**
 * @author sunita
 *
 */
public abstract class BFeatureTypes extends FeatureTypes implements BoundaryFeatureFunctions {
    /**
     * @param fgen
     */
    public BFeatureTypes(FeatureGenImpl fgen) {
        super(fgen);
    }
    /**
     * @param single
     */
    public BFeatureTypes(FeatureTypes single) {
        super(single);
    }
    public abstract boolean startScanFeaturesAt(DataSequence arg);
    public abstract void next(BFeatureImpl arg0);
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
     */
    //public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
    //    return false;
    //}
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
     */
    public void next(FeatureImpl f) {;}
}
