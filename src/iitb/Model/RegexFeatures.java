package iitb.Model;
import iitb.CRF.*;
import java.util.regex.*;
import java.io.*;
/**
 *
 * @author Imran Mansuri
 *
 */ 

public class RegexFeatures extends FeatureTypes {

	String patternString[][] = {
		{"isDigit", "\\d+"},
		{"isInitCapital", "[A-Z][a-zA-Z]+"},
		{"isAllCapital", "[A-Z]+"},
		{"isAllSmallCase", "[a-z]+"},
		{"isAlpha", "[a-zA-Z]+"},
		{"isAlphaNumeric", "[a-zA-Z0-9]+"},
		{"endsWithDot", "[^.]+[.]"}, 
		{"endsWithComma", "[^,]+[,]"}, 
		{"containsSpecialCharacters", ".*[#.;:\\-/<>'\"()&].*"} 
	};

	Pattern p[];
	int i;
	String curToken;

	public RegexFeatures(Model m) {
		super(m);
		assert(patternString != null);
		p = new Pattern[patternString.length];
		for(i = 0; i < patternString.length; i++){
			p[i] = Pattern.compile(patternString[i][1]);
		}
	}
	
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		assert(patternString != null);
		i = 0;
		curToken = new String((String) data.x(pos));
		return true;
	}
	
	public boolean hasNext() {
		return i < patternString.length;
	}
	
	public void next(FeatureImpl f) {
		Matcher m = p[i].matcher(curToken);	
		f.ystart = -1;
		f.val = (m.matches()? 1:0); 
		f.strId.id = i;
		f.strId.name = patternString[i++][0];
	}
};
