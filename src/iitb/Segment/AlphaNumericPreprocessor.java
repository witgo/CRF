package iitb.Segment;
import java.util.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class AlphaNumericPreprocessor extends Preprocessor {
    //    public static String DIGIT = "р";
        public static String DIGIT = new String("DIGIT");

	public int getCode() {
	    return 1;
	}
	public static String preprocess(String s) {
	    if (isNumber(s)) {
		return DIGIT;
//		if (s.length()==1) {
//			if (!s.equals("c") && !s.equals("o")) {
//			char x[]=s.toCharArray();
//			if (Character.isLetter(x[0])) {
//				return new String("е");
//			}
//			} else {
//				return s;
//			}
//		}
/*		char sarr[]=s.toCharArray();
		char rarr[]=new char[sarr.length];
		int lettercount=0,digitcount=0;
		for(int i=0 ; i<rarr.length ; i++) {
		    if (!Character.isDigit(sarr[i])) {
			//     rarr[i]='е';
			rarr[i]=sarr[i];
			lettercount++;
		    }
		    if (Character.isDigit(sarr[i])) {
			rarr[i]='р';
			digitcount++;
		    }
*/
		}

	    return s;
		//		return (digitcount>0)?new String(rarr):s;
//		return (digitcount>0)?new String("ер"):new String("ее");
	    }
    public AlphaNumericPreprocessor() {;}
    public static boolean isNumber(String s) {
	try {
	    Integer i=Integer.valueOf(s);
	} catch(NumberFormatException e) {
	    return false;
	}
	return true;
    }
    public static TrainData  preprocess(TrainData tokens, int numLabels) {
	for (tokens.startScan(); tokens.hasMoreRecords(); ) {
	    TrainRecord tr = tokens.nextRecord();
	    for (int s = 0; s < tr.numSegments(); s++) {
		String[] words = tr.tokens(s);
		for (int j = 0; j < words.length; j++) {
		    words[j] = preprocess(words[j]);
		}
	    }
	}
	return tokens;
    }
};

