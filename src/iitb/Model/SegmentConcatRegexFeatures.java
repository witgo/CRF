package iitb.Model;
import iitb.CRF.*;
import java.util.regex.*;
import java.io.*;

/**
 * SegmentConcatRegexFeatures concatinates pattern sequence features for a given segment.
 * Thus, it considers tokens within the segment under consideration. 
 * Since, it operates on a segment, it should be used for {@link NestedCRF} model.
 * It uses regular expression to match a sequence of character pattern and generates features 
 * accordingly.
 * The number of features generated are numState * 2^window. Where window is typically 
 * maxMemory for {@link NestedCRF}.
 * <p> 
 * The object of this class should be wrap around {@link FeatureTypesEachLabel} as follows:
 * <pre>
 * 	new FeatureTypesEachLabel(model, new SegmentConcatRegexFeatures(model, window));
 * </pre>
 * </p>
 * 
 * @see		FeatureTypesEachLabel
 * @see		RegexFeatures
 * @see		ContextRegexFeatures
 * @see		LeftContextRegexFeatures
 * @see		RightContextRegexFeatures
 *
 * @author 	Imran Mansuri
 */
public class SegmentConcatRegexFeatures extends FeatureTypes {

	String patternString[][] = {
		{"isDigitConcatSegment",		"\\d+"				},
		{"isInitCapitalConcatSegment",		"[A-Z][a-zA-Z]+"		},
		{"isAllCapitalConcatSegment",		"[A-Z]+"			},
		{"isAllSmallCaseConcatSegment",	"[a-z]+"			},
		{"isAlphaConcatSegment",		"[a-zA-Z]+"			},
		{"isAlphaNumericConcatSegment",	"[a-zA-Z0-9]+"			},
		{"endsWithDotConcatSegmnet",		"[^.]+\\."			}, 
		//{"endsWithCommaConcatSegment", 	"[^,]+[,]"			}, 
		{"containsSpecialCharactersConcatSegment", ".*[#.;:\\-/<>'\"()&].*"	} 
	};

	Pattern p[];
	int i;		//index into pattern array
	int idbase; 	//number of possible ids for each pattern
	int window; 		//size of the window -- must be constant for the feature types class
	int curOffset; 		//to be used to generate feature ids for collection mode
	int left, right;	//segment boundary
	DataSequence data;	//current sequence being scanned

	/**
	 * Constructs a cancat feature types on segment with maximum length equalt to window.
	 *
	 * @param m		a {@link Model} object
	 * @param window	maximum segment size.
	 */
	public SegmentConcatRegexFeatures(Model m, int window) {
		super(m);
		this.window = window;
		idbase = (int) Math.pow(2, window);
		assert(patternString != null);
		p = new Pattern[patternString.length];
		for(i = 0; i < patternString.length; i++){
			p[i] = Pattern.compile(patternString[i][1]);
		}
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
			left = ((prevPos > 0) ? prevPos : 0);
			right = pos;
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
			//Return features on token sequence
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
