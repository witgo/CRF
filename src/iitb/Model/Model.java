package iitb.Model;
import iitb.CRF.*;
/**
 *
 * This class and its children provide various graph classes. This
 * allows you to create CRFs where you can have more than one state
 * per label.
 *
 * @author Sunita Sarawagi
 */

interface EdgeIterator {
    void start();
    boolean hasNext();
    Edge next();
};

public abstract class Model {
    int numLabels;
    Model(int nlabels) {
	numLabels = nlabels;
    }
    public int numberOfLabels() {return numLabels;}
    public abstract int numStates();
    public abstract int label(int stateNum);
    public abstract int numEdges();
    public abstract EdgeIterator edgeIterator();
    public abstract int numStartStates();
    public abstract int numEndStates();
    public abstract boolean isEndState(int i);
    public abstract boolean isStartState(int i);
    /**
       return the i-th start state
     */
    public abstract int startState(int i);
    public abstract int endState(int i);
    public abstract void stateMappings(DataSequence data) throws Exception;
    public abstract void stateMappings(DataSequence data, int len, int start) throws Exception;
    
    public void printGraph() {
	System.out.println("Numnodes = " + numStates() + " NumEdges " + numEdges());
	EdgeIterator iter = edgeIterator();
	for (iter.start(); iter.hasNext(); ) {
	    Edge edge = iter.next();
	    System.out.println(edge.start + "-->" + edge.end);
	}
	System.out.print("Start states");
	for (int i = 0; i< numStartStates(); i++)
	    System.out.print(" " + startState(i));
	System.out.println("");

	System.out.print("End states");
	for (int i = 0; i< numEndStates(); i++)
	    System.out.print(" " + endState(i));
	System.out.println("");


    }
};
