package iitb.MaxentClassifier;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class DataRecord implements DataSequence {
    int label;
    float vals[];
    DataRecord (int ncols) {
	vals = new float[ncols];
    }
    public DataRecord(DataRecord dr) {
	vals = new float[dr.vals.length];
	for (int i = 0; i < vals.length; vals[i] = dr.vals[i],i++);
	label = dr.label;
    }
    public DataRecord(float v[], int l) {
	vals = v;
	label = l;
    }
    public int length() {return 1;}
    public int y() {return label;}
    public int y(int i) {return label;}
    public Object x(int i) {return vals;}
    public void set_y(int i, int l) {label = l;}
    public float getColumn(int col) {return vals[col];}
};
