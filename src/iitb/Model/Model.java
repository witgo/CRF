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

class Edge {
    int start;
    int end;
    Edge() {;}
    Edge(int s, int e) {
	start = s;
	end = e;
    }
    String tostring() {
	return (start + " -> " + end);
    }
};

interface EdgeIterator {
    void start();
    boolean hasNext();
    Edge next();
};

public interface Model {
    public int numStates();
    public int label(int stateNum);
    public int numEdges();
    public EdgeIterator edgeIterator();
    public int numStartStates();
    public int numEndStates();
    public boolean isEndState(int i);
    public boolean isStartState(int i);
    /**
       return the i-th start state
     */
    public int startState(int i);
    public int endState(int i);
    public void stateMappings(DataSequence data) throws Exception;
    public void stateMappings(DataSequence data, int len, int start) throws Exception;
};
