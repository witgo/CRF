package iitb.Model;
import iitb.CRF.*;
import java.util.regex.*;
import java.io.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class ConcatRegexFeatures extends FeatureTypes {

	String patternString[][] = {
		{"isDigitConcat", "\\d+"},
		{"isInitCapitalConcat", "[A-Z][a-zA-Z]+"},
		{"isAllCapitalConcat", "[A-Z]+"},
		{"isAllSmallCaseConcat", "[a-z]+"},
		{"isAlphaConcat", "[a-zA-Z]+"},
		{"isAlphaNumericConcat", "[a-zA-Z0-9]+"},
		{"isEndsWithDotConcat", "[^.]+\\."} //Reconsider this pattern
	};

	Pattern p[];
	int i;	//index into pattern array
	int idbase; //number of possible ids for each pattern
	String curTokens[]; //current token sequence being scanned
	int maxMemory; //size of the window -- must be constant for the feature types class
	int curOffset; //to be used to generate feature ids for collection mode

	public ConcatRegexFeatures(Model m, int maxMemory) {
		super(m);
		this.maxMemory = maxMemory;
		idbase = (int) Math.pow(2, maxMemory);
		System.out.println("Maxmemory :"+ maxMemory + " " + idbase);
		assert(patternString != null);
		p = new Pattern[patternString.length];
		for(i = 0; i < patternString.length; i++){
			p[i] = Pattern.compile(patternString[i][1]);
		}
	}

	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		assert(patternString != null);
		i = 0;

		if(FeatureGenImpl.featureCollectMode){
			curOffset = 0;
		}else {
			
			int window;
			if(prevPos == -1)
				window = 1;
			else 
				window = pos - prevPos + 1; 
			if(window > maxMemory){
				i = patternString.length;
				return false;
			}
				
			curTokens = new String[window];
			for(int k = pos,j = 0; j < window; k--, j++){
				curTokens[j] = (String) data.x(k);
			}
		}
		return true;
	}

	public boolean hasNext() {
		return i < patternString.length;
	}

	public void next(FeatureImpl f) {

		if(FeatureGenImpl.featureCollectMode){

			//This is just a feature collection mode, so return id and name
			f.strId.id = idbase * i + curOffset++;
			f.strId.name = patternString[i][0];
			
			//System.out.println("Feature#" + f.strId.name +":" + f.strId.id);

			if(curOffset >= idbase){
				curOffset = 0;
				i++;
			}
		}else{
			//Return features on token sequence
			int base = 1;
			f.strId.id = 0;
			for(int k = 0; k < curTokens.length; k++){
				boolean match = p[i].matcher(curTokens[k]).matches();	
				//boolean match = m.matches();
				f.strId.id += base * (match? 1:0);
				base = base * 2;
			}
			f.val = 1; //or any other value
			f.ystart = -1;
			f.strId.id += idbase * i++;
		}
	}
};
