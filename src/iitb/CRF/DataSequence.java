package iitb.CRF;
/**
 * A basic training/test instance needs to support the DataSequence interface.
 * @author Sunita Sarawagi
 *
 */ 


public interface DataSequence {
    public int length();
    public int y(int i);
    /** The type of x is never interpreted by the CRF package. This could be useful for your FeatureGenerator class */ 
    public Object x(int i);
    public void set_y(int i, int label);
};
