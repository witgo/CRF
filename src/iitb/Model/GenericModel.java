package iitb.Model;
import iitb.CRF.*;
import java.util.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 


public class GenericModel implements Model {
    int _numStates;
    Edge _edges[];  // edges have to be sorted by their starting node id.
    int edgeStart[]; // the index in the edges array where edges out of node i start.
    int startStates[];
    int endStates[];
    public int label(int s) {assert false; return 1;}
    public GenericModel(String spec) throws Exception {
	if (spec.endsWith("-chain")) {
	    StringTokenizer tok = new StringTokenizer(spec,"-");
	    int len = Integer.parseInt(tok.nextToken());
	    _numStates = len;
	    _edges = new Edge[len];
	    edgeStart = new int[len];
	    for (int i = 0; i < len-1; i++) {
		_edges[i] = new Edge(i,i+1);
		edgeStart[i] = i;
	    }
	    if (len > 1)
		_edges[len-1] = new Edge(len-2,len-2);
	    else
		_edges[0] = new Edge(0,0);
	    startStates = new int[1];
	    startStates[0] = 0;
	    endStates = new int[1];
	    endStates[0] = len-1;
	} else if (spec.endsWith("parallel")) {
	    StringTokenizer tok = new StringTokenizer(spec,"-");
	    int len = Integer.parseInt(tok.nextToken());
	    _numStates = len*(len+1)/2;
	    _edges = new Edge[len*(len-1)/2 + 1];
	    edgeStart = new int[_numStates];
	    startStates = new int[len];
	    endStates = new int[len];
	    int node = 0;
	    int e = 0;
	    for (int i = 0; i < len; i++) {
		node += i;
		for (int j = 0; j < i; j++) {
		    _edges[e++] = new Edge(node+j,node+j+1);
		    edgeStart[node+j] = e-1;
		}
		startStates[i] = node;
		endStates[i] = node + i;
	    }
	    node += len;
	    _edges[e++] = new Edge(_numStates-2, _numStates-2);
	    assert (e == _edges.length);
	    assert (node == _numStates);
	} else if (spec.equals("boundary")) {
	    // this implements a model where each label is either of a
	    // Unique word (state 0) or broken into a Start state
	    // (state 1) with a single token, Continuation state
	    // (state 2) with multiple tokens (only state with
	    // self-loop) and end state (state 3) with a single token.
	    // The number of states is thus 4, and number of edges 4
	    _numStates = 4;
	    _edges = new Edge[4];
	    _edges[0] = new Edge(1,2);
	    _edges[1] = new Edge(1,3);
	    _edges[2] = new Edge(2,2);
	    _edges[3] = new Edge(2,3);
	    startStates = new int[2];
	    startStates[0] = 0;
	    startStates[1] = 1;
	    endStates = new int[2];
	    endStates[0] = 0;
	    endStates[1] = 3;
	    edgeStart = new int[_numStates];
	    edgeStart[0] = 4;
	    edgeStart[1] = 0;
	    edgeStart[2] = 2;
	    edgeStart[3] = 4;
	} else {
	    throw new Exception("Unknown graph type: " + spec); 
	}
    }
    public int numStates() {return _numStates;}
    public int numEdges() {return _edges.length;}
    public int numStartStates() {return startStates.length;}
    public int startState(int i) {return startStates[i];}
    public int numEndStates() {
	return endStates.length;
    }
    public int endState(int i) {
	return endStates[i];
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
    public void stateMappings(DataSequence data) throws Exception {
	stateMappings(data,data.length(),0);
    }
    public void stateMappings(DataSequence data, int len, int start) throws Exception {
	for (int i = 0; i < numStartStates(); i++) {
	    if (pathToEnd(data,startState(i),len-1,start+1)) {
		data.set_y(start,startState(i));
		return;
	    }
	}
	throw new Exception("No path in graph");
    }
    boolean pathToEnd(DataSequence data, int s, int lenLeft, int start) {
	if (lenLeft == 0) {
	    return isEndState(s);
	}
	for (int e = edgeStart[s]; (e < numEdges()) && (_edges[e].start == s); e++) {
	    int child = _edges[e].end;
	    if (pathToEnd(data,child,lenLeft-1,start+1)) {
		data.set_y(start,child);
		return true;
	    }
	}
	return false;
    }
    
public class GenericEdgeIterator implements EdgeIterator {
    int edgeNum;
    Edge edges[];
    GenericEdgeIterator(Edge[] e) {
	edges = e;
	start();
    };
    public void start() {
	edgeNum = 0;
    }
    public boolean hasNext() {
	return (edgeNum < edges.length);
    }
    public Edge next() {
	edgeNum++;
	return edges[edgeNum-1];
    }
};
    public EdgeIterator edgeIterator() {
	return new GenericEdgeIterator(_edges);
    }

};

