package iitb.Model;
import iitb.CRF.*;
import java.util.*;

public class NestedModel extends Model {
    int _numStates;
    int _numEdges;
    int nodeOffsets[]; // the number of states in the labels before this.
    Model inner[];
    Model outer;
    int startStates[];
    int endStates[];

    public NestedModel(int num, String specs) throws Exception {
	super(num);
	name = "Nested";
	nodeOffsets = new int[numLabels];
	inner = new Model[numLabels];
	
	StringTokenizer start = new StringTokenizer(specs, ",");
	assert start.hasMoreTokens();
	outer = Model.getNewBaseModel(numLabels, (String)start.nextToken());
	for(int i=0 ; i<numLabels ; i++) {
	    assert start.hasMoreTokens();
	    String thisStruct = start.nextToken();
	    inner[i] = new GenericModel(thisStruct,i);
	}
	_numEdges = 0;
	_numStates = 0;
	for (int l = 0; l < numLabels; l++) {
	    nodeOffsets[l] += _numStates;
	    _numStates += inner[l].numStates();
	    _numEdges += inner[l].numEdges();
	}
	EdgeIterator outerIter = outer.edgeIterator();
	while (outerIter.hasNext()) {
	    Edge e = outerIter.next();
	    _numEdges += inner[e.end].numStartStates()*inner[e.start].numEndStates();
	}
	
	int numStart = 0;
	for (int i = 0; i < outer.numStartStates(); i++) {
	    numStart += inner[outer.startState(i)].numStartStates();
	}
	startStates = new int[numStart];
	int index = 0;
	for (int i = 0; i < outer.numStartStates(); i++) {
	    for (int j = 0; j < inner[outer.startState(i)].numStartStates(); j++) {
		startStates[index++] = inner[outer.startState(i)].startState(j) + nodeOffsets[outer.startState(i)];
	    }
	}

	int numEnd = 0;
	for (int i = 0; i < outer.numEndStates(); i++) {
	    numEnd += inner[outer.endState(i)].numEndStates();
	}
	endStates = new int[numEnd];
	index = 0;
	for (int i = 0; i < outer.numEndStates(); i++) {
	    for (int j = 0; j < inner[outer.endState(i)].numEndStates(); j++) {
		endStates[index++] = inner[outer.endState(i)].endState(j) + nodeOffsets[outer.endState(i)];
	    }
	}
	
    }    
    public int numStates() {return _numStates;}
    public int numEdges() {return _numEdges;}
    public int label(int stateNum) {
	assert (stateNum >= 0) && (stateNum < numStates());
	// TODO -- convert to binary scan.
	for (int i = 0; i < nodeOffsets.length; i++) {
	    if (stateNum < nodeOffsets[i])
		return i-1;
	}
	return nodeOffsets.length-1;
    }
    public int numStartStates() {
	return startStates.length;
    } 
    public int numEndStates() {
	return endStates.length;
    }
    public int startState(int i) {
	return ((i < numStartStates())?startStates[i]:-1);
    }
    public int endState(int i) {
	return ((i < numEndStates())?endStates[i]:-1);// endStates[i];
    }
    public boolean isEndState(int i) {
	// TODO -- convert this to binary search
	for (int k = 0; k < endStates.length; k++)
	    if (endStates[k] == i)
		return true;
	return false;
    }
    public boolean isStartState(int i) {
	// TODO -- convert this to binary search
	for (int k = 0; k < startStates.length; k++)
	    if (startStates[k] == i)
		return true;
	return false;
    }
    public void stateMappings(DataSequence data, int len, int start) throws Exception 
    {
	assert false;
    }

    public void stateMappings(SegmentDataSequence data) throws Exception {
	if (data.length() == 0)
	    return;
	for (int lstart = 0; lstart < data.length();) {
	    int lend = data.getSegmentEnd(lstart)+1;
	    if (lend == 0) {
	       throw new Exception("Invalid segment end value");
	    }
	    int label = data.y(lstart);
	    inner[label].stateMappings(data,lend-lstart, lstart);
	    for (int k = lstart; k < lend; k++) {
		data.set_y(k, nodeOffsets[label]+data.y(k));
	    }
	    lstart=lend;
	}
    }
    public void stateMappings(DataSequence data) throws Exception {
	if (data.length() == 0)
	    return;
	for (int lstart = 0; lstart < data.length();) {
	    int lend = lstart+1;
	    for (;(lend < data.length()) && (data.y(lend) == data.y(lstart)); lend++);
	    int label = data.y(lstart);
	    inner[label].stateMappings(data,lend-lstart, lstart);
	    for (int k = lstart; k < lend; k++) {
		data.set_y(k, nodeOffsets[label]+data.y(k));
	    }
	    lstart=lend;
	}
    }
    public int stateMappingGivenLength(int label, int len, int posFromStart) 
    throws Exception {
    	return inner[label].stateMappingGivenLength(label,len,posFromStart);
	}
public class NestedEdgeIterator implements EdgeIterator {
    NestedModel model;
    int label;
    Edge edge;
    EdgeIterator edgeIter[], outerEdgeIter;
    Edge outerEdge;
    boolean outerEdgesSent;
    int index1, index2;
    
    NestedEdgeIterator(NestedModel m) {
	model = m;
	edge = new Edge();
	edgeIter = new EdgeIterator[model.numLabels];
	for (int l = 0; l < model.numLabels; l++) {
	    edgeIter[l] = model.inner[l].edgeIterator();
	}
	outerEdgeIter = model.outer.edgeIterator();
	start();
    }
    public void start() {
	label = 0;
	for (int l = 0; l < model.numLabels; l++) {
	    edgeIter[l].start();
	} 
	outerEdgeIter.start();
	
	outerEdge = outerEdgeIter.next();

	//check for the null edge
	if(outerEdge == null)
		outerEdgesSent = true;
	else
		outerEdgesSent = false;
	index1 = index2 = 0;
    }
    public boolean hasNext() {
	return (label < model.numLabels) || !outerEdgesSent;
    }
    public Edge nextOuterEdge() {
	edge.start = model.inner[outerEdge.start].endState(index1) + model.nodeOffsets[outerEdge.start];
	edge.end = model.inner[outerEdge.end].startState(index2) + model.nodeOffsets[outerEdge.end];
	index2++;
	if (index2 == model.inner[outerEdge.end].numStartStates()) {
	    index2 = 0;
	    index1++;
	    if (index1  == model.inner[outerEdge.start].numEndStates()) {
		if (outerEdgeIter.hasNext()) {
		    outerEdge = outerEdgeIter.next();
		    index1 = index2 = 0;
		} else {
		    outerEdgesSent = true;
		}
	    }
	}
	return edge;
    }
    public Edge nextInnerEdge() {
	Edge edgeToRet = edgeIter[label].next();
	edge.start = edgeToRet.start;
	edge.end = edgeToRet.end;
	assert (edge != null);
	assert (model.nodeOffsets != null);
	assert (label < model.nodeOffsets.length);
	edge.start += model.nodeOffsets[label];
	edge.end += model.nodeOffsets[label];
	if (!edgeIter[label].hasNext())
	    label++;
	return edge;
    }
    public Edge next() {
	if (!nextIsOuter()) {
	    return nextInnerEdge();
	} else {
	    return nextOuterEdge();
	}
    }
	/* (non-Javadoc)
	 * @see iitb.Model.EdgeIterator#nextIsOuter()
	 */
	public boolean nextIsOuter() {
		return (label >= model.numLabels);
	}
};
    public EdgeIterator edgeIterator() {
	return new NestedEdgeIterator(this);
    }
   
    public static void main(String args[]) {
	try {
	    System.out.println(args[0]);
	    System.out.println(args[1]);
	Model model = new NestedModel(Integer.parseInt(args[0]), args[1]);
	System.out.println(model.numStates());
	System.out.println(model.numEdges());
	System.out.println(model.numStartStates());
	System.out.println(model.numEndStates());
	EdgeIterator edgeIter = model.edgeIterator();
	//	EdgeIterator edgeIter2 = model.edgeIterator();
	for (int edgeNum = 0; edgeIter.hasNext(); edgeNum++) {
	    boolean edgeIsOuter = edgeIter.nextIsOuter();
	    Edge e = edgeIter.next();
	    System.out.println(e.start + "("+ model.label(e.start) + ")" + " -> " + e.end + ":" + edgeIsOuter+ ";");
	}
	} catch (Exception e) {
	    System.out.println(e.getMessage());
	    e.printStackTrace();
	    //	    System.out.println(e.getStackTrace().getLineNumber());
	}
    }
};

