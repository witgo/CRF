package iitb.Model;
import iitb.CRF.*;
import java.util.regex.*;
import java.io.*;

/**
 * LeftRightContextConcatRegexFeatures encodes dependancy of tokens around the current token in consideration.
 * Thus, it considers tokens around a window of given token and uses them to generate features.
 * It uses regular expression to match a sequence of character pattern and generates features 
 * accordingly.
 * <p> 
 * The object of this class should be wrap around {@link FeatureTypesEachLabel} as follows:
 * <pre>
 * 	new FeatureTypesEachLabel(model, new LeftRightContextConcatRegexFeatures(model, window));
 * </pre>
 * </p>
 * 
 * @author 	Imran Mansuri
 */ 

public class LeftRightContextConcatRegexFeatures extends FeatureTypes {

	/**
	 *	Various patterns are defined here. 
	 *	First dimension of this two dimensional array is feature name and second value is the
	 *	regular expression pattern to be matched against tokens. You can add your own patterns
	 *	in this array.
	 */
	String patternString[][] = {
		{"isDigitContextLeftRight", "\\d+"},
		//{"isInitCapitalContextLeftRight", 	"[A-Z][a-zA-Z]+"	},
		//{"isAllCapitalContextLeftRight", 	"[A-Z]+"		},
		{"isAllSmallCaseContextLeftRight", 	"[a-z]+"		},
		//{"isAlphaContextLeftRight", 		"[a-zA-Z]+"		},
		{"isAlphaNumericContextLeftRight", 	"[a-zA-Z0-9]+"		},
		{"endsWithDotContextLeftRight", 		"[^.]+\\."		}, 
		//{"endsWithCommaContextLeftRight", 	"[^,]+[,]"		}, 
		{"containsSpecialCharactersContextLeftRight", ".*[#.;:\\-/<>'\"()&].*"} 
	};

	Pattern p[];
	int i;			
	int idbase; 	//number of possible ids for each pattern
	int window; 	//size of the window  left and right to the current token
	int curOffset;	//to be used to generate feature ids for collection mode
	int left, right;	//window boundary
	DataSequence data;	//current sequence being scanned

	/**
	 * Constructs a feature types with context window size as specified.
	 *
	 * @param m		a {@link Model} object
	 * @param window	number of tokens to left and right of current token to be used
	 * 			for generating features.
	 */
	public LeftRightContextConcatRegexFeatures(Model m, int window) {
		super(m);
		this.window = window;
		idbase = (int) Math.pow(2, 2* window + 1);
		assert(patternString != null);
		p = new Pattern[patternString.length];
		for(i = 0; i < patternString.length; i++){
			p[i] = Pattern.compile(patternString[i][1]);
		}
	}

	/**
	 * Constructs a feature types with default context window size(1).
	 *
	 * @param Model		object
	 */
	public LeftRightContextConcatRegexFeatures(Model m){
		this(m, 1);
	}

	/**
	 * Initaites scanning of features in a sequence at specified position.
	 *
	 * @param data		a training sequence 
	 * @param prevPos	the previous label postion
	 * @param pos		Current token postion
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		assert(patternString != null);
		i = 0;
		if(FeatureGenImpl.featureCollectMode){
			curOffset = 0;
		}else {
			this.data = data;
			left = ((pos - window) > 0 ? (pos - window) : 0);
			right = ((pos + window) < data.length() ? (pos + window) : (data.length()-1));
		}
		return true;
	}

	/**
	 * Returns true if there are any more feature(s) for the current scan.
	 *
	 */
	public boolean hasNext() {
		return i < patternString.length;
	}

	/**
	 * Generates the next feature for the current scan.
	 *
	 * @param f	Copies the feature generated to the argument 
	 */
	public void next(FeatureImpl f) {

		if(FeatureGenImpl.featureCollectMode){

			//This is just a feature collection mode, so return id and name
			f.strId.id = idbase * i + curOffset++;
			f.strId.name = patternString[i][0];

			if(curOffset >= idbase){
				curOffset = 0;
				i++;
			}
		}else{
			//Return feature on token window
			int base = 1;
			f.strId.id = 0;
			for(int k = left; k <= right; k++){
				boolean match = p[i].matcher((String)data.x(k)).matches();	
				f.strId.id += base * (match? 1:0);
				base = base * 2;
			}
                        f.val = (f.strId.id >0) ? 1:0; //In case of no match return 0 as feature value
			f.ystart = -1;
			f.strId.id += idbase * i++;
		}
	}
};
