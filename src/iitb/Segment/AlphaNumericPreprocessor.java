package iitb.Segment;
import java.util.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class AlphaNumericPreprocessor extends Preprocessor {

        public static String DIGIT = new String("DIGIT");

	public int getCode() {
	    return 1;
	}
	public static String preprocess(String s) {
	    if (isNumber(s)) {
		return DIGIT;
		}

	    return s;
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

