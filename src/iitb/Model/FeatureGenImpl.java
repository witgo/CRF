package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 * The FeatureGenerator is an aggregator over all these different
 * feature types. You can inherit from the FeatureGenImpl class and
 * after calling one of the constructors that does not make a call to
 * (addFeatures()) you can then implement your own addFeatures
 * class. There you will typically add the EdgeFeatures feature first
 * and then the rest.  So, for example if you wanted to add some
 * parameter for each label (like a prior), you can create a new
 * FeatureTypes class that will create as many featureids as the
 * number of labels. You will have to create a new class that is
 * derived from FeatureGenImpl and just have a different
 * implementation of the addFeatures subroutine. The rest will be
 * handled by the parent class.  
 * This class  is responsible for converting the
 * string-ids that the FeatureTypes assign to their features into
 * distinct numbers. It has a inner class called FeatureMap that will
 * make one pass over the training data and create the map of
 * featurenames->integer id and as a side effect count the number of
 * features.
 *
 * @author Sunita Sarawagi
 * */

public class FeatureGenImpl implements FeatureGenerator {
    Vector features;
    Iterator featureIter;
    FeatureTypes currentFeatureType;
    FeatureImpl featureToReturn, feature;
    public Model model;
    int totalFeatures;
    
    DataSequence data;
    int cpos;
    WordsInTrain dict;

    public void addFeature(FeatureTypes fType) {
	features.add(fType);
    }
    public void setDict(WordsInTrain d) {
	dict = d;
    }
    protected void addFeatures() { 
	features.add(new EdgeFeatures(model));
	features.add(new StartFeatures(model));
	features.add(new EndFeatures(model));

	dict = new WordsInTrain();
	features.add(new UnknownFeature(model,dict));
	// features.add(new KnownInOtherState(model, dict));
	features.add(new WordFeatures(model, dict));
	//	features.add(new WordScoreFeatures(model, dict));
    }
    FeatureTypes getFeature(int i) {
	return (FeatureTypes)features.elementAt(i);
    }

    boolean featureCollectMode = false;
    class FeatureMap {
	Hashtable strToInt;
	FeatureIdentifier idToName[];
	public int getId(FeatureImpl f) {
	    return getId(f.identifier());
	}
	public int getId(Object key) {
	    if (strToInt.get(key) != null) {
		return ((Integer)strToInt.get(key)).intValue();
	    }
	    return -1;
	}
	private void collectNames() {
	    idToName = new FeatureIdentifier[strToInt.size()];
	    for (Enumeration e = strToInt.keys() ; e.hasMoreElements() ;) {
		Object key = e.nextElement();
		idToName[getId(key)] = (FeatureIdentifier)key;
	    }
	}
	public int collectFeatureIdentifiers(DataIter trainData, int maxMem) {
	    featureCollectMode = true;
	    strToInt = new Hashtable();
	    for (trainData.startScan(); trainData.hasNext();) {
		DataSequence seq = trainData.next();
		for (int l = 0; l < seq.length(); l++) {
		    for (int m = 1; (m <= maxMem) && (l-m >= -1); m++) {
			for (startScanFeaturesAt(seq,l-m,l); hasNext(); ) {
			    FeatureImpl feature = nextNoId();
			    if (getId(feature) < 0) {
				strToInt.put(feature.identifier().clone(), new Integer(strToInt.size()));
			    }
			}
		    }
		}
	    }
	    featureCollectMode = false;
	    collectNames();
	    return strToInt.size();
	}
	public void write(PrintWriter out) throws IOException {
	    out.println(strToInt.size());
	    for (Enumeration e = strToInt.keys() ; e.hasMoreElements() ;) {
		Object key = e.nextElement();
		out.println(key + " " + ((Integer)strToInt.get(key)).intValue());
	    }
	}
	public int read(BufferedReader in) throws IOException {
	    int len = Integer.parseInt(in.readLine());
	    String line;
	    for(int l = 0; (l < len) && ((line=in.readLine())!=null); l++) {
		StringTokenizer entry = new StringTokenizer(line," ");
		String key = entry.nextToken();
		int pos = Integer.parseInt(entry.nextToken());
		strToInt.put(key,new Integer(pos));
	    }
	    collectNames();
	    return strToInt.size();
	}
	public FeatureIdentifier getIdentifier(int id) {return idToName[id];} 
	public String getName(int id) {return idToName[id].toString();} 
    };
    FeatureMap featureMap;
    static Model getModel(String modelSpecs, int numLabels) throws Exception {
	// create model..
	if (modelSpecs.equalsIgnoreCase("naive")) {
	    return new CompleteModel(numLabels);
	} else {
	    return new NestedModel(numLabels, modelSpecs);
	}
    }
    public FeatureGenImpl(String modelSpecs, int numLabels) throws Exception {
	this(modelSpecs,numLabels,true);
    }
    public FeatureGenImpl(String modelSpecs, int numLabels, boolean addFeatureNow) throws Exception {
	this(getModel(modelSpecs,numLabels),numLabels,addFeatureNow);
    }
    public FeatureGenImpl(Model m, int numLabels, boolean addFeatureNow) throws Exception {
	model = m;
	features = new Vector();
	featureToReturn = new FeatureImpl();
	feature = new FeatureImpl();
	featureMap = new FeatureMap();
	if (addFeatureNow) addFeatures();
    }

    public void stateMappings(DataIter trainData) throws Exception {
	for (trainData.startScan(); trainData.hasNext();) {
	    DataSequence seq = trainData.next();
	    model.stateMappings(seq);
	}
    }
    public int maxMemory() {return 1;}
    public void train(DataIter trainData) throws Exception {
	// map the y-values in the training set.
        stateMappings(trainData);
	if (dict != null) dict.train(trainData,model.numStates());
	totalFeatures = featureMap.collectFeatureIdentifiers(trainData,maxMemory());
    };
    public void printStats() {
	System.out.println("Num states " + model.numStates());
	System.out.println("Num edges " + model.numEdges());
	if (dict != null) System.out.println("Num words in dictionary " + dict.dictionaryLength());
	System.out.println("Num features " + numFeatures());
    }
    protected FeatureImpl nextNoId() {
	feature.copy(featureToReturn);
	advance(false);
	return feature;
    }
    protected void advance() {
	advance(!featureCollectMode);
    }
    protected void advance(boolean returnWithId) {
	while (true) {
	    for (;((currentFeatureType == null) || !currentFeatureType.hasNext()) && featureIter.hasNext();) {
		currentFeatureType = (FeatureTypes)featureIter.next();
	    }
	    if (!currentFeatureType.hasNext())
		break;
	    while (currentFeatureType.hasNext()) {
		featureToReturn.init();
		currentFeatureType.next(featureToReturn);
		if (returnWithId) {
		    featureToReturn.id = featureMap.getId(featureToReturn);
		    if (featureToReturn.id < 0)
			continue;
		}
		if ((cpos > 0) && (cpos < data.length()-1))
		    return;
		if ((cpos == 0) && (model.isStartState(featureToReturn.y())))
		    return;
		if ((cpos == data.length()-1) && (model.isEndState(featureToReturn.y())))
		    return;
	    }
	}
	featureToReturn.id = -1;
    }
    public void startScanFeaturesAt(DataSequence d, int prev, int p) {
	startScanFeaturesAt(d,p);
    }
    public void startScanFeaturesAt(DataSequence d, int p) {
	data = d;
	cpos = p;
	for (int i = 0; i < features.size(); i++) {
	    getFeature(i).startScanFeaturesAt(data,cpos);
	}
	currentFeatureType = null;
	featureIter = features.iterator();
	advance();
    }
    public boolean hasNext() {
	return (featureToReturn.id >= 0);
    }

    public Feature next() {
	feature.copy(featureToReturn);
	advance();
	//	System.out.println(feature);
	return feature;
    }
    public int numFeatures() {
	return totalFeatures;
    }
    public FeatureIdentifier featureIdentifier(int id) {return featureMap.getIdentifier(id);}
    public String featureName(int featureIndex) {
	return featureMap.getName(featureIndex);
    }
    public int numStates() {
	return model.numStates();
    }
    public int label(int stateNum) {
	return model.label(stateNum);
    }
    public void mapStatesToLabels(DataSequence data) {
	for (int i = 0; i < data.length(); i++) {
	    data.set_y(i, label(data.y(i)));
	}
    }
    public void read(String fileName) throws IOException {
	BufferedReader in=new BufferedReader(new FileReader(fileName));
	if (dict != null) dict.read(in, model.numStates());
	totalFeatures = featureMap.read(in);
    }
    public void write(String fileName) throws IOException {
	PrintWriter out=new PrintWriter(new FileOutputStream(fileName));
	if (dict != null) dict.write(out);
	featureMap.write(out);
	out.close();
    }
};
