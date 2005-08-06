/*
 * Created on Apr 2, 2005
 *
 */
package iitb.BSegmentCRF;

import iitb.CRF.DataSequence;
import iitb.CRF.FeatureGenerator;
import iitb.CRF.FeatureGeneratorNested;

/**
 * @author sunita
 *
 */
public interface BFeatureGenerator extends FeatureGeneratorNested {
    /**
     * @return: the maximum gap between start and end boundary of features
     */
    int maxBoundaryGap();
    void startScanFeaturesAt(DataSequence data);
    BFeature nextFeature();
};
