package iitb.Model;
import iitb.CRF.*;

public class NestedFeatureGenImpl extends FeatureGenImpl implements FeatureGeneratorNested {
    int maxMem[];
    int maxMemOverall=1;

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
		WindowFeatures.Window windows[] = new WindowFeatures.Window[] {
		        	new WindowFeatures.Window(0,true,0,true,"start"), 
		        	new WindowFeatures.Window(0,false,0,false,"end"),
		        	new WindowFeatures.Window(1,true,-1,false,"continue"),
					new WindowFeatures.Window(-1,true,-1,true,"left-1"),
					new WindowFeatures.Window(1,false,1,false,"right+1"),
					};
/*		features.add(new FeatureTypesEachLabel(model, 
				new WindowFeatures(windows, new FeatureTypesConcat(model,
						new ConcatRegexFeatures(model,0,0), maxMemOverall))));		
*/		features.add(new FeatureTypesEachLabel(model, 
				new WindowFeatures(windows, new FeatureTypesMulti(
						new ConcatRegexFeatures(model,0,0)))));

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
	cposEnd = pos;
	cposStart = prevPos+1;
	for (int i = 0; i < features.size(); i++) {
	    getFeature(i).startScanFeaturesAt(data,prevPos,cposEnd);
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
