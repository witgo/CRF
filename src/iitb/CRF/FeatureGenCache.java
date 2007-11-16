/*
 * Created on Jul 20, 2005
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *   
 *   For each distinct feature-id there is a IntHashArray of variants of 
 *   the values and labels of the feature seen throughout the data.  This list is a vector of
 *   variantIds.  There is hash-map from variantIds to FeatureImpl.
 *   
 *   TODO: keeping vector of featureIds implies that insertion is quadratic--- need to make this efficient.
 */
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import cern.colt.matrix.DoubleMatrix1D;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

public class FeatureGenCache implements FeatureGeneratorNested {
	private static final long serialVersionUID = 1L;
	FeatureGeneratorNested fgen;
	FeatureGenerator sfgen;
	TIntArrayList featureIds = new TIntArrayList();
	Vector perSegmentFeatureOffsets = new Vector();
	protected boolean firstScan=true;
	int dataIndex=-1;
	int scanNum=0;
	int dataIndexStart=0;
	
	public static class AllFeatureCache {
	    Vector distinctFeatures;
		Vector featureVariants;
//		public FeatureVector edgeFeatureIds = new FeatureVector();
		public EdgeFeatures edgeFeatures = new EdgeFeatures();
		public boolean edgeFeaturesXIndependent = false;
		//public boolean cacheEdgeFeaturesDone = false;
		class FeatureImpl implements Feature {
			int _index;
			int _y;
			float _value;
			void init(int _index, int _y, float _value) {
				this._index = _index;
				this._y = _y;
				this._value = _value;
			}
			void copy(Feature f) {
			    this._index = f.index();
				this._y = f.y();
				this._value = f.value();
			}
	        public FeatureImpl(Feature f) {
	            if (f != null) {
                    copy(f);
                }
	        }
			public int index() {
				return _index;
			}
			public int y() {
				return _y;
			}
			public int yprev() {
				return -1;
			}
			public float value() {
				return _value;
			}
			public int[] yprevArray() {
				return null;
			}
            public boolean allButValueEqual(Object obj) {
                Feature feature = (Feature)obj;
                return (_y==feature.y()); 
            }
            public int add(Feature f){return index();}
			@Override
			public int hashCode() {
				final int PRIME = 31;
				int result = 1;
				result = PRIME * result + Float.floatToIntBits(_value);
				result = PRIME * result + _y;
				return result;
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				return (allButValueEqual(obj) 
	                    && (Math.abs(_value-((Feature)obj).value()) < Float.MIN_VALUE));
			}
		}
        class FeatureImplWithYPrev extends FeatureImpl {
            public FeatureImplWithYPrev(Feature f) {
                super(f);
                _yprev=f.yprev();
            }
            int _yprev;
            void init(int _index, int _y, int yprev, float _value) {
                super.init(_index, _y, _value);
                this._yprev=yprev;
                
            }
            void copy(Feature f) {
                super.copy(f);
                _yprev=f.yprev();
            }
            public boolean allButValueEqual(Object obj) {
                return (super.allButValueEqual(obj) && (_yprev==((Feature)obj).yprev())); 
            }
            public int yprev() {
                return _yprev;
            }
            @Override
			public int hashCode() {
				final int PRIME = 31;
				int result = 1;
				result = PRIME * result + Float.floatToIntBits(_value);
				result = PRIME * result + _y;
				result = PRIME * result + _yprev;
				return result;
			}
        }
		class FeatureCache extends FeatureImpl {
		    Hashtable<FeatureImpl,Integer> featureVariantIds = null;
		    FeatureCache(Feature f) {super(f);}
	        /**
	         * @param f
	         * @return
	         */
	        public int add(Feature f) {
	            if (equals(f)) {
	                return f.index();
	            }
	            if (featureVariantIds == null) {
	                featureVariantIds = new Hashtable<FeatureImpl,Integer>();
	            }
                //Object diffObject = createDiff(f);
                FeatureImpl diffObject = new FeatureImpl(f);
                int variantId;
                if (featureVariantIds.containsKey(diffObject))
                    variantId = featureVariantIds.get(diffObject); //findFeatureInVariantList(f);
                else {
	                variantId = featureVariants.size();
	                featureVariantIds.put(diffObject,variantId);
	                featureVariants.add(diffObject);
	            }
	            return -1*variantId-1;
	        }
        }
        class FeatureCacheWithYPrev extends FeatureImplWithYPrev {
            Hashtable<FeatureImplWithYPrev,Integer> featureVariantIds = null;
            public FeatureCacheWithYPrev(Feature f) {super(f);}
            /**
             * @param f
             * @return
             */
            public int add(Feature f) {
                if (equals(f)) {
                    return f.index();
                }
                if (featureVariantIds == null) {
                    featureVariantIds = new Hashtable<FeatureImplWithYPrev,Integer>();
                }
                //Object diffObject = createDiff(f);
                FeatureImplWithYPrev diffObject = new FeatureImplWithYPrev(f);
                int variantId;
                if (featureVariantIds.containsKey(diffObject))
                    variantId = featureVariantIds.get(diffObject); //findFeatureInVariantList(f);
                else {
                    variantId = featureVariants.size();
                    featureVariantIds.put(diffObject,variantId);
                    featureVariants.add(diffObject);
                }
                return -1*variantId-1;
            }
        }
        
		public AllFeatureCache(boolean edgeFeaturesXIndependent) {
		    this.edgeFeaturesXIndependent = edgeFeaturesXIndependent;
		    distinctFeatures = new Vector();
		    featureVariants = new Vector();
		}
        public int add(Feature f) {
            int numAdd = f.index()+1-distinctFeatures.size();
            for (int i = 0; i < numAdd; i++) {
                distinctFeatures.add(null);
            }
            if (distinctFeatures.get(f.index())==null) {
                if (f.yprev() >= 0)
                    distinctFeatures.setElementAt(new FeatureCacheWithYPrev(f),f.index());
                else 
                    distinctFeatures.setElementAt(new FeatureCache(f),f.index());
                return f.index();
            } else {
                return ((FeatureImpl)(distinctFeatures.get(f.index()))).add(f);
            }
        }
		
		public class EdgeFeatures {
		    FeatureVector edgeFeatureIds[];
		    boolean edgeTypeCached[];
		    EdgeFeatures() {
		        edgeFeatureIds = new FeatureVector[4];
		        edgeTypeCached = new boolean[4];
			    for (int i = 0; i < 4; edgeFeatureIds[i] = new FeatureVector(), edgeTypeCached[i++]=false);
		    }
		    int getEdgeType(int prevPos, int pos, int dataLen) {
		        return  2*((prevPos == 0)?1:0) + ((dataLen == pos+1)?1:0);
		    }
		    public void addEdgeFeature(int edgeId, int prevPos, int pos, int dataLen) {
		        int edgeType = getEdgeType(prevPos,pos, dataLen);
		        if (edgeTypeCached[edgeType])
		            return;
		        edgeFeatureIds[edgeType].add(edgeId);
		    }
		    public FeatureVector getEdgeIds(int prevPos, int pos, int dataLen) {
		        return edgeFeatureIds[getEdgeType(prevPos, pos,dataLen)];
		    }
		    public void doneOneRoundEdges() {
		        for (int i = 0; i < 4; i++) {
		            if (edgeFeatureIds[i].size() > 0) {
		                edgeTypeCached[i] = true;
		        	}
		        }
		    }
		}
        public Feature get(int featureId) {
            if (featureId >= 0) {
                return (Feature) distinctFeatures.get(featureId); // distinctFeatures[featureId];
            } else {
                return (Feature) featureVariants.get(-1*featureId-1);
            }
        }
        public class FIterator implements Iterator {
            int index;
            int sz;
            TIntArrayList intArr;
            FIterator(TIntArrayList intArr) {
                index = 0;
                this.intArr = intArr;
                sz = intArr.size();
            }
            public boolean hasNext() {
                return index < sz;
            }
            public Object next() {
                return AllFeatureCache.this.get(intArr.get(index++));
            }
            public void remove() {
            }
        }
        public class FeatureVector extends TIntArrayList {
            public void add(Feature f) {
                add(AllFeatureCache.this.add(f));
            }
            public Iterator iterator() {
                return new FIterator(this);
            }
        }
        double DEFAULT_VALUE = RobustMath.LOG0;
        public class Flist extends FeatureVector {
            public DoubleMatrix1D mat;
            Flist(int numLabels) {
                mat = new LogDenseDoubleMatrix1D(numLabels);
                mat.assign(DEFAULT_VALUE);
            }
            public void clear() {super.clear();mat.assign(DEFAULT_VALUE);}
            public void add(Feature f, double lambda[]) {
                super.add(f);
                double oldVal = mat.get(f.y());
                if (oldVal == DEFAULT_VALUE)
                    oldVal = 0;
                mat.set(f.y(),oldVal+f.value()*lambda[f.index()]);
            }
            public void calcMatrix(double lambda[]) {
                if (size()==0) return;
                mat.assign(DEFAULT_VALUE);
                for (Iterator iter = iterator(); iter.hasNext();) {
                    Feature f = (Feature) iter.next();
                    double oldVal = mat.get(f.y());
                    if (oldVal == DEFAULT_VALUE)
                        oldVal = 0;
                    mat.set(f.y(),oldVal+f.value()*lambda[f.index()]);
                }
            }
        }
        public FeatureVector newFeatureVector() {return new FeatureVector();}
        public Flist newFlist(int numLabels) {return new Flist(numLabels);}
	}
	AllFeatureCache featureCache;
	
	public FeatureGenCache(FeatureGeneratorNested fgen, boolean edgeFeaturesXIndependent) {
	    alloc(fgen,edgeFeaturesXIndependent);
	    
	}
	public FeatureGenCache(FeatureGenCache sharedCache, int startDataIndex) {
		assert (sharedCache.scanNum>0);
		firstScan=false;
		dataIndexStart = startDataIndex;
		scanNum = sharedCache.scanNum;
		fgen = sharedCache.fgen;
		sfgen = sharedCache.sfgen;
		featureCache = sharedCache.featureCache;
		featureIds = sharedCache.featureIds;
		perSegmentFeatureOffsets = sharedCache.perSegmentFeatureOffsets;
		stats = sharedCache.stats;
	}
    /**
     * @param fgen2
     * @param edgeFeaturesXIndependent
     */
    private void alloc(FeatureGenerator fgen, boolean edgeFeaturesXIndependent) {
        this.sfgen = fgen;
		if (sfgen instanceof FeatureGeneratorNested)
			this.fgen = ((FeatureGeneratorNested)sfgen);
		else 
		    this.fgen = null;
		featureCache = new AllFeatureCache(edgeFeaturesXIndependent);
    }
    public FeatureGenCache(FeatureGenerator fgen, boolean edgeFeaturesXIndependent) {
        alloc(fgen,edgeFeaturesXIndependent);
	}
	// for each distinct feature-id this stores all various forms of the features.
	class Stats {
		int dataLen;
		int maxSegSize;
		int pos, prevPos;
		int thisSegmentOffsets[];
        TIntObjectHashMap segmentFeatureOffsets;
        boolean cacheEdgeFeatures = false;
        Stats() {
            segmentFeatureOffsets = new TIntObjectHashMap();
        }
        public void clear() {
            maxSegSize = 1;
        }
        int getKey(int prevPos, int pos) {
            return pos*dataLen+pos-prevPos-1;
        }
        int[] getStartEndOffsets(int prevPos, int pos) {
            return (int[]) segmentFeatureOffsets.get(getKey(prevPos,pos));
        }
        /**
         * @param data
         * @param pos
         * @param prevPos
         */
        public void initSegment(DataSequence data, int prevPos, int pos) {
            dataLen = data.length();
			maxSegSize = Math.max(maxSegSize, pos-prevPos);
			this.pos = pos;
			this.prevPos = prevPos;
			thisSegmentOffsets = (int[]) segmentFeatureOffsets.get(getKey(prevPos,pos));
			if (thisSegmentOffsets==null) {
			    thisSegmentOffsets = new int[2];
			    segmentFeatureOffsets.put(getKey(prevPos,pos),thisSegmentOffsets);
			}
			thisSegmentOffsets[0] = thisSegmentOffsets[1] = featureIds.size();
			featureCache.edgeFeatures.doneOneRoundEdges();
			/*
			if (cacheEdgeFeatures) {
			    // features already cached in previous segment.
			    cacheEdgeFeatures = false;
			    featureCache.cacheEdgeFeaturesDone = true;
			} else if ((prevPos >= 0) && !featureCache.cacheEdgeFeaturesDone) {
			    cacheEdgeFeatures = true;
			}
			*/
        }
        /**
         * @param f
         */
        public void add(Feature f) {
            if (featureCache.edgeFeaturesXIndependent && (f.yprev() >= 0)) {
                featureCache.edgeFeatures.addEdgeFeature(featureCache.add(f),prevPos, pos,dataLen);
                return;
            }
            featureIds.add(featureCache.add(f));
            thisSegmentOffsets[1]++;
        }
	}
	Stats stats = new Stats();

	public void startDataScan() {
		dataIndex = dataIndexStart-1;
		scanNum++;
		if (scanNum ==2) {
		    firstScan = false;
		        // cache the last data item.
		        cachePreviousDataSequence();
		        System.out.println("First scan done..distinct features "+(featureCache.featureVariants.size()+featureCache.distinctFeatures.size()));
		}
	}

	/**
     * 
     */
    private void cachePreviousDataSequence() {
		int dataLen = stats.dataLen;
		int[][] featureOffsets[] = new int[dataLen][stats.maxSegSize][2];
		for (int p = 0; p < dataLen; p++) {
			for (int l = 0; (l < stats.maxSegSize) && (p-l >= 0); l++) {
			    int offsets[] = stats.getStartEndOffsets(p-l-1,p);
			        featureOffsets[p][l][0] = (offsets==null)?0:offsets[0];
			        featureOffsets[p][l][1] = (offsets==null)?0:offsets[1];
			}
		}
		perSegmentFeatureOffsets.add(featureOffsets);
    }
    protected void cacheFeature(Feature f)  {
        stats.add(f);
    }
    public void nextDataIndex() {
		dataIndex++;
		if (!firstScan) {
		    return;
		}
		if (dataIndex > 0) {
		    cachePreviousDataSequence();
		}
		stats.clear();
	}
    public void setDataIndex(int dIndex) {
		dataIndex = dIndex;
		if (!firstScan) {
		    return;
		}
		if (dataIndex > 0) {
		    cachePreviousDataSequence();
		}
		stats.clear();
	}
    /**
     * @param data
     * @return
     */
    protected int getDataIndex(DataSequence data) {
        return dataIndex;
    }
	class Cursor {
        int currentFeatureOffset;
        int featureOffsetEnd;
        int edgeFeatureId = 0;
        AllFeatureCache.FeatureVector edgeFeatureIds;
        /**
         * @param data
         * @param pos
         * @param prevPos
         */
        public void init(DataSequence data, int prevPos, int pos) {
            int[][] tfeatures[] = (int[][][]) perSegmentFeatureOffsets.get(getDataIndex(data));
            currentFeatureOffset = tfeatures[pos][pos-prevPos-1][0];
            featureOffsetEnd = tfeatures[pos][pos-prevPos-1][1];
            
            edgeFeatureId = -1;
            if ((prevPos >= 0) && (featureCache.edgeFeaturesXIndependent)) {
                edgeFeatureIds = featureCache.edgeFeatures.getEdgeIds(prevPos,pos,data.length());
                edgeFeatureId = edgeFeatureIds.size()-1;
            }
        }
        /**
         * @return
         */
        public boolean hasNext() {
            return ((currentFeatureOffset < featureOffsetEnd) || (edgeFeatureId >= 0));
        }
        /**
         * @param feature
         */
        public Feature nextFeature() {
            int featureId = (currentFeatureOffset < featureOffsetEnd)?
                    featureIds.get(currentFeatureOffset++):edgeFeatureIds.get(edgeFeatureId--);
            return featureCache.get(featureId);
        }
	}
	Cursor cursor = new Cursor();
	
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGeneratorNested#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public void startScanFeaturesAt(DataSequence data, int prevPos, int pos, boolean nested) {
		if (firstScan) {
			stats.initSegment(data,prevPos,pos);
			if (nested) 
				fgen.startScanFeaturesAt(data,prevPos,pos);
			else 
				sfgen.startScanFeaturesAt(data,pos);
		} else {
		    cursor.init(data,prevPos,pos); 
		}
	}	
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#hasNext()
	 */
	public boolean hasNext() {
		return (firstScan)?sfgen.hasNext():cursor.hasNext();
	}
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#next()
	 */
	public Feature next() {
		if (firstScan) {
			Feature f = sfgen.next();
			stats.add(f);
			return f;
		} else {
		    return cursor.nextFeature();
		}
	}
    /* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#featureName(int)
	 */
	public String featureName(int featureIndex) {
		return fgen.featureName(featureIndex);
	}
	public void startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		startScanFeaturesAt(data,prevPos,pos,true);
	}
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#numFeatures()
	 */
	public int numFeatures() {
		return sfgen.numFeatures();
	}
	
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#startScanFeaturesAt(iitb.CRF.DataSequence, int)
	 */
	public void startScanFeaturesAt(DataSequence data, int pos) {
		startScanFeaturesAt(data,pos-1,pos,false);
	}
	
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGeneratorNested#maxMemory()
	 */
	public int maxMemory() {
		return (sfgen instanceof FeatureGeneratorNested)?((FeatureGeneratorNested)sfgen).maxMemory():1;
	}
}
