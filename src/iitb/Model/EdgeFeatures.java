package iitb.Model;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class EdgeFeatures extends FeatureTypes {
    EdgeIterator edgeIter;
    int edgeNum;
    Object labelNames[];
    public EdgeFeatures(Model m, Object labels[]) {
	super(m);
	edgeIter = m.edgeIterator();
	labelNames=labels;
    }
    public EdgeFeatures(Model m) {
	this(m,null);
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	if (prevPos < 0) {
	    edgeNum = model.numEdges();
	    return false;
	} else {
	    edgeNum = 0;
	    edgeIter.start();
	    return true;
	}
    }
    public boolean hasNext() {
	return (edgeNum < model.numEdges());
    }	
    public void next(FeatureImpl f) {
	Edge e = edgeIter.next();
	Object name="";
	if (featureCollectMode) {
	    if (labelNames == null) {
		name = "E."+model.label(e.start);
	    } else {
		name = labelNames[model.label(e.start)];
	    }
	}
	if (model.isOuterEdge(e,edgeNum)) {
	    setFeatureIdentifier(model.label(e.start)*model.numberOfLabels()+model.label(e.end), model.label(e.end),name,f);
	} else {
	    setFeatureIdentifier(edgeNum,e.end,name,f);
	}
	f.ystart = e.start;
	f.yend = e.end;
	f.val = 1;
	edgeNum++;
    }
};
