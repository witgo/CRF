/*
 * Created on May 5, 2005
 *
 */
package iitb.Model;

import iitb.CRF.CandSegDataSequence;
import iitb.CRF.CandidateSegments;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;

import java.util.Properties;

/**
 * @author sunita
 *
 */
public class CandSegFeatureGenImpl extends NestedFeatureGenImpl {
    
    /**
     * @param numLabels
     * @param options
     * @param addFeatureNow
     * @throws Exception
     */
    public CandSegFeatureGenImpl(int numLabels, Properties options,
            boolean addFeatureNow) throws Exception {
        super(numLabels, options, addFeatureNow);
    }
    
    /**
     * @param numLabels
     * @param options
     * @throws Exception
     */
    public CandSegFeatureGenImpl(int numLabels, Properties options)
    throws Exception {
        super(numLabels, options);
    }
    
    protected boolean holdsInData(DataSequence seq, FeatureImpl f) {
        return super.holdsInData(seq, f);
    }
    public boolean train(DataIter trainData, boolean cachedLabels,
            boolean collectIds) throws Exception {
        boolean retval = super.train(trainData,cachedLabels,false);
        if (!collectIds)
            return retval;
        if (addOnlyTrainFeatures) {
            for (trainData.startScan(); trainData.hasNext();) {
                SegmentDataSequence seq = (SegmentDataSequence)trainData.next();
                int segEnd;
                for (int l = 0; l < seq.length(); l = segEnd+1) {
                    segEnd = seq.getSegmentEnd(l);
                    for (startScanFeaturesAt(seq,l-1,segEnd); hasNext(); next());
                }
            }
        } else {
            for (trainData.startScan(); trainData.hasNext();) {
                CandSegDataSequence dataRecord = (CandSegDataSequence)trainData.next();
                for (int segEnd = dataRecord.length()-1; segEnd >= 0; segEnd--) {
                    for (int nc = dataRecord.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                        int segStart = dataRecord.candSegmentStart(segEnd,nc);
                        startScanFeaturesAt(dataRecord, segStart-1, segEnd);
                        while (hasNext()) {
                            next();
                        }		                    
                    } 
                }
            }
        }
        freezeFeatures();
        return retval;
    }
}