package iitb.Model;
import iitb.CRF.*;
import java.util.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 



public class FeatureImpl implements Feature {
    public String type;
    public String strId;
    public int id;
    public int ystart, yend;
    public float val = 1;
    
    public void copy(FeatureImpl featureToReturn) {
	strId = featureToReturn.strId;
    	id = featureToReturn.id;
	type = featureToReturn.type;
	ystart = featureToReturn.ystart;
	yend = featureToReturn.yend;
	val = featureToReturn.val;
    }
    public int index() {return id;} 
    public int y() {return yend;}
    public int yprev() {return ystart;}
    public float value() {return val;}
    public String toString() {return strId + " " + val;}
};

