package iitb.Model;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 
public class NoEdgeModel extends CompleteModel {
    class EmptyEdgeIter implements EdgeIterator {
	public void start(){}
	public boolean hasNext(){return false;}
	public Edge next(){return null;}
    };
    EmptyEdgeIter emptyIter;
    public NoEdgeModel(int nlabels) {
	super(nlabels);
	emptyIter = new EmptyEdgeIter();
    }
    public int numEdges() {return 0;}
    public EdgeIterator edgeIterator() {
	return emptyIter;
    }
};
