package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class EdgeFeatures extends FeatureTypes {
    EdgeIterator edgeIter;
    int edgeNum;
    public EdgeFeatures(Model m) {
	super(m);
	edgeIter = m.edgeIterator();
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
	f.type = "Edge";
	f.strId = "E"+model.label(e.start)+"_"+model.label(e.end);
	//	f.strId = "E"+(e.start)+"_"+(e.end);
	f.ystart = e.start;
	f.yend = e.end;
	f.val = 1;
	edgeNum++;
    }
};
