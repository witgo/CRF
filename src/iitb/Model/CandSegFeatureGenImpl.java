/*
 * Created on May 5, 2005
 *
 */
package iitb.Model;

import gnu.trove.TIntHashSet;
import iitb.CRF.CandSegDataSequence;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;

import java.util.Properties;

/**
 * @author sunita
 *
 */
public class CandSegFeatureGenImpl extends NestedFeatureGenImpl {
    
    public CandSegFeatureGenImpl(String modelSpecs, int numLabels,
            boolean addFeatureNow) throws Exception {
        super(modelSpecs, numLabels, addFeatureNow);
    }
    public CandSegFeatureGenImpl(int numLabels,java.util.Properties options, boolean addFeatureNow) throws Exception {
        super(numLabels,options,addFeatureNow);
    }

    /**
     * @param numLabels
     * @param options
     * @param addFeatureNow
     * @throws Exception
     */
   
    protected boolean retainFeature(DataSequence seq, FeatureImpl f) {
      
        return ((CandSegDataSequence)seq).holdsInTrainingData(f,cposStart-1,cposEnd);
    }
    public void addTrainRecord(DataSequence data) {
      //  if (!addOnlyTrainFeatures) {
            CandSegDataSequence dataRecord = (CandSegDataSequence)data;
            for (int segEnd = dataRecord.length()-1; segEnd >= 0; segEnd--) {
                for (int nc = dataRecord.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                    int segStart = dataRecord.candSegmentStart(segEnd,nc);
                    startScanFeaturesAt(dataRecord, segStart-1, segEnd);
                    while (hasNext()) {
                        next();
                    }		                    
                } 
            }
       /* } else {
            SegmentDataSequence seq = (SegmentDataSequence)data;
            int segEnd;
            for (int l = 0; l < seq.length(); l = segEnd+1) {
                segEnd = seq.getSegmentEnd(l);
                for (startScanFeaturesAt(seq,l-1,segEnd); hasNext(); next());
            }
        } */
    }
}