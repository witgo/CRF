/*
 * Created on May 31, 2005
 *
 */
package iitb.BSegmentCRF;

import java.util.Vector;

import iitb.CRF.DataSequence;
import iitb.CRF.FeatureGenCache;

/**
 * @author sunita
 *
 */
public class FeatureStoreCache extends FeatureStore {
    boolean cache= false;
    Vector fstores = new Vector();
    int numIters = 0;
    public FeatureStoreCache(boolean cache, boolean edgeFeatureXIndependent) {
        super(edgeFeatureXIndependent);
        this.cache=cache;
    }
    /**
     * @param dataSeq
     * @param bfgen
     * @param lambda
     * @param numY
     * @param numRecord
     */
    public void init(DataSequence dataSeq, BFeatureGenerator bfgen, double[] lambda, int numY, int numRecord) {
        if (numRecord==0) numIters++;
        if (!cache || (numRecord < 0)) { 
            super.init(dataSeq,bfgen,lambda,numY); 
            return;
        }
        
        if (numIters == 1) {
            FeatureStore fstore = new FeatureStore(allFeatureCache);
            fstore.init(dataSeq,bfgen,lambda,numY);
            fstores.add(fstore);
        }
        copy((FeatureStore)fstores.get(numRecord));
        if (numIters > 1) setLambda(lambda);
    }
}
