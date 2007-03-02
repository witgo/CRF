package iitb.MaxentClassifier;
import java.util.*;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class DataSet implements DataIter {
    Vector allRecords;
    int currPos = 0;
    public DataSet(Vector recs) {allRecords = recs;}
    public void startScan() {currPos = 0;}
    public boolean hasNext() {return (currPos < allRecords.size());}
    public DataSequence next() {currPos++;return (DataRecord)allRecords.elementAt(currPos-1);}
};
