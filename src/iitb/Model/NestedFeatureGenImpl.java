package iitb.Model;
import iitb.CRF.*;

public class NestedFeatureGenImpl extends FeatureGenImpl implements FeatureGeneratorNested {
    int maxMem[];
    int maxMemOverall;

    public NestedFeatureGenImpl(int numLabels,java.util.Properties options, boolean addFeatureNow) throws Exception {
	super("naive",numLabels,false);
	if (options.getProperty("MaxMemory") != null) {
	    maxMemOverall = Integer.parseInt(options.getProperty("MaxMemory"));
	} 
	if (addFeatureNow) {
		features.add(new EdgeFeatures(model));
		features.add(new StartFeatures(model));
		features.add(new EndFeatures(model));
		dict = new WordsInTrain();
		features.add(new FeatureTypesMulti(new UnknownFeature(model,dict)));
		features.add(new FeatureTypesMulti(new WordFeatures(model, dict)));
		features.add(new FeatureTypesEachLabel(model, new FeatureTypesSegmentLength(model)));
		}
    }
    public NestedFeatureGenImpl(int numLabels,java.util.Properties options) throws Exception {
	this(numLabels,options,true);
    }
    public void startScanFeaturesAt(DataSequence data, int pos) {
	startScanFeaturesAt(data,pos-1,pos);
    }
    public int maxMemory() {
	return maxMemOverall;
    }
    public void setMaxMemory(int i) {
	maxMemOverall = i;
    }

    // we assume each label is associated with a maximum length for which it
    // is willing to output a grouped probability.  That is, different y-s
    // have different value of maxMem.
    public void startScanFeaturesAt(DataSequence d, int prevPos, int pos) {
	data = d;
	cpos = pos;
	for (int i = 0; i < features.size(); i++) {
	    getFeature(i).startScanFeaturesAt(data,prevPos,cpos);
	}
	currentFeatureType = null;
	featureIter = features.iterator();
	advance();
	// if no word features activated, do not send the edge and
	// start/end features.
	/*	if ((currentFeatureType != getFeature(0)) && (cpos-prevPos > 1)) {
	    featureToReturn.id = -1;
	}
	*/
	
    }
    // TODO do not send any features where the maxMem property of the y
    // is violated.
};
