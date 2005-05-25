package iitb.Segment;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class LabelMap {
    int map(int l) {return l-1;}
    int revMap(int l) {return l+1;}
};

class BinaryLabelMap extends LabelMap {
    int posClass;
    BinaryLabelMap(int sel) {
	posClass = sel;
    }
    int map(int el) {return (posClass == el)?1:0;}
    int revMap(int label) {return (label==1)?posClass:0;}
};
