package iitb.Utils;

import java.util.*;

public class Counters {
    int cnts[] = null;
    int maxVal;
    BitSet fixedVals;
    public Counters(int numCtrs, int maxVal) {
	cnts = new int[numCtrs];
	fixedVals = new BitSet(numCtrs+1);
	this.maxVal = maxVal;
    }
    public void fix(int index, int val) {cnts[index ] =val; fixedVals.set(index);}
    public void clear() {
	for (int i = 0; i < cnts.length; cnts[i++] = 0);
	fixedVals.clear();
    }
    int nextNonFixed(int i) {return fixedVals.nextClearBit(i);}
    public boolean isFixed(int index) {return fixedVals.get(index);}
    public boolean advance() {
	for (int i = 0; (i < cnts.length); i++) {
	    i = nextNonFixed(i);
	    if (i < cnts.length) {
		cnts[i]++;
		if (cnts[i] < maxVal)
		    return true;
		else
		    cnts[i] = 0;
	    }
	}
	return false;
    }
    public int value(int endIndex, int startIndex) {
	int val = 0;
	for (int i = endIndex; i >= startIndex; i--) {
	    val = (val*maxVal + cnts[i]);
	}
	return val;
    }
    public int value() {return value(cnts.length-1,0);}
    public void arrayCopy(int endIndex, int startIndex, int arr[]) {
	for (int i = endIndex; i >= startIndex; i--) {
	    arr[i-startIndex] = cnts[i];
	}
    }
}; 
