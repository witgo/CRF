/*
 * Created on Jul 19, 2005
 *
 */
package iitb.Model;

import gnu.trove.TIntIntHashMap;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.FeatureGenCache;

/**
 * @author sunita
 *
 */
public class FeatureGenSelectiveCache extends FeatureGenCache {
    FeatureGenImpl fgenImpl;
    interface DataSequenceWithId {
        int id();
    }
    /**
     * @param fgen
     */
    public FeatureGenSelectiveCache(FeatureGenImpl fgen, boolean edgeFeaturesXInd) {
        super(fgen,edgeFeaturesXInd);
        fgenImpl = fgen;
    }
    int prevDataIndex=0;
    protected int getDataIndex(DataSequence data) {
        int thisId = ((DataSequenceWithId)data).id();
        if (thisId != prevId) {
            prevDataIndex = idIndexMap.get(thisId);
            prevId = thisId;
        }
        return prevDataIndex;
    }
	public boolean hasNext() {
		return super.hasNext()?true:fgenImpl.hasNext();
	}
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#next()
	 */
	public Feature next() {
		if (firstScan) {
			boolean needsCaching = fgenImpl.currentFeatureType.needsCaching();
			Feature f = fgenImpl.next();
			if (needsCaching)
			    cacheFeature(f);
			return f;
		} else {
		    if (super.hasNext())
		        return super.next();
			return fgenImpl.next();
		}
	}
	TIntIntHashMap idIndexMap = new TIntIntHashMap();
	int prevId=-1;
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGeneratorNested#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public void startScanFeaturesAt(DataSequence data, int prevPos, int pos, boolean nested) {
	    super.startScanFeaturesAt(data,prevPos,pos,nested);
	    if (firstScan) {
	        int thisId = ((DataSequenceWithId)data).id();
	        if (thisId != prevId) {
	            idIndexMap.put(thisId, super.getDataIndex(data));
	            prevId = thisId;
	        }
	    } else {
			if (nested) 
				fgenImpl.startScanFeaturesAtOnlyNonCached(data,prevPos,pos);
			else 
				fgenImpl.startScanFeaturesAtOnlyNonCached(data,pos);
		}
	}	
}
