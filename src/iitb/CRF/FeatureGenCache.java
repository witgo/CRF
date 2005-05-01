/*
 * Created on Feb 15, 2005
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public class FeatureGenCache implements FeatureGeneratorNested {
    /**
     * @author sunita
     *
     */
    class FeatureImpl implements Feature {
        int _index;
        int _y;
        int _yprev;
        float _value;
        void init(int _index, int _y, int _yprev, float _value) {
            this._index = _index;
            this._y = _y;
            this._yprev = _yprev;
            this._value = _value;
        }
  
        /**
         * 
         */
        public FeatureImpl() {
            super();
            // TODO Auto-generated constructor stub
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Feature#index()
         */
        public int index() {
            return _index;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Feature#y()
         */
        public int y() {
            return _y;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Feature#yprev()
         */
        public int yprev() {
            return _yprev;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Feature#value()
         */
        public float value() {
            return _value;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Feature#yprevArray()
         */
        public int[] yprevArray() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    private static final long serialVersionUID = 1L;
    FeatureGeneratorNested fgen;
    FeatureGenerator sfgen;
    FeatureImpl feature = new FeatureImpl();
    int numFeatures = 0;
    int scanNum = 0;
    int dataIndex = 0;
    int y[];
    int yprev[];
    int index[];
    float val[];
    int offset[], endOffset[];
    int indexPtr;
    int thisKey;
    int maxKey=0;
    int numData = 0;
    int maxDataLen=0;
    int maxSegLen=0;
    /**
     * 
     */
    public FeatureGenCache(FeatureGeneratorNested fgen) {
        this.fgen = fgen;
        sfgen = fgen;
        numFeatures = 0;
    }
    public FeatureGenCache(FeatureGenerator fgen) {
        this.sfgen = fgen;
        numFeatures = 0;
    }

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGeneratorNested#maxMemory()
     */
    public int maxMemory() {
        return (fgen==null)?1:fgen.maxMemory();
    }

  
    public void startDataScan() {
        scanNum++;
        if (scanNum==2) {
           // System.out.println("Numfeatures overall "+numFeatures);
            y = new int[numFeatures];
            yprev = new int[numFeatures];
            index = new int[numFeatures];
            val = new float[numFeatures];
            maxKey = maxSegLen*numData*maxDataLen;
            offset = new int[maxKey+1];
            endOffset = new int[maxKey+1];
            numFeatures = 0;
        }
        dataIndex = -1;
    }
    public void nextDataIndex() {
        dataIndex++;
        if (scanNum==1) numData++;
    }
    public void startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        startScanFeaturesAt(data,prevPos,pos,true);
    }
    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGeneratorNested#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
     */
    public void startScanFeaturesAt(DataSequence data, int prevPos, int pos, boolean nested) {
        assert(scanNum > 0);
        thisKey = dataIndex*maxDataLen + pos + (pos-prevPos-1)*maxDataLen*numData;
        if (scanNum == 1) {
            maxSegLen = Math.max(maxSegLen,pos-prevPos);
            maxDataLen = Math.max(maxDataLen,data.length());
        } else
            assert(thisKey < maxKey+1);
        if (scanNum <= 2) {
            if (nested) 
                ((FeatureGeneratorNested) sfgen).startScanFeaturesAt(data,prevPos,pos);
            else 
                sfgen.startScanFeaturesAt(data,pos);
        } else {
            indexPtr = offset[thisKey];
        }
        if (scanNum==2) {
            offset[thisKey]=numFeatures;
            endOffset[thisKey]=numFeatures;
        }
    }	

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#numFeatures()
     */
    public int numFeatures() {
        return fgen.numFeatures();
    }

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#startScanFeaturesAt(iitb.CRF.DataSequence, int)
     */
    public void startScanFeaturesAt(DataSequence data, int pos) {
        startScanFeaturesAt(data,pos-1,pos,false);
    }

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#hasNext()
     */
    public boolean hasNext() {
        return (scanNum<=2)?sfgen.hasNext():endOffset[thisKey]>indexPtr;
    }

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#next()
     */
    public Feature next() {
        if (scanNum <= 2) {
            Feature f = sfgen.next();
            if (scanNum==2) {
                y[numFeatures]=f.y();
                yprev[numFeatures]=f.yprev();
                val[numFeatures]=f.value();
                index[numFeatures] = f.index();
                endOffset[thisKey]++;
            }
            numFeatures++;
            return f;
        } else {
            feature.init(index[indexPtr],y[indexPtr],yprev[indexPtr],val[indexPtr]);
            indexPtr++;
            return feature;
        }
    }

    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#featureName(int)
     */
    public String featureName(int featureIndex) {
        return sfgen.featureName(featureIndex);
    }

}
