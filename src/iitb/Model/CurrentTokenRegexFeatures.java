package iitb.Model;
import iitb.CRF.*;
import java.util.regex.*;
import java.io.*;

/**
 * CurrentTokenRegexFeatures generates features by matching the token with character patterns.
 * Character patterns are regular expressions for checking whether the token is capitalized word, 
 * a number, small case word, whether the token contians any special characters and like.
 * It uses regular expression to match a sequence of character pattern and generates features 
 * accordingly.
 * <p> 
 * The object of this class should be wrap around {@link FeatureTypesEachLabel} as follows:
 * <pre>
 * 	new FeatureTypesEachLabel(model, new CurrentTokenRegexFeatures(model));
 * </pre>
 * </p>
 * 
 * @author 	Imran Mansuri
 */
 
public class CurrentTokenRegexFeatures extends FeatureTypes {

	/**
	 *	Various patterns are defined here. 
	 *	First dimension of this two dimensional array is feature name and second value is the
	 *	regular expression pattern to be matched against a token. You can add your own patterns
	 *	in this array.
	 */
	String patternString[][] = {
		//{"isInitCapital", 	"[A-Z][a-zA-Z]+"	},
		//{"isAllCapital", 	"[A-Z]+"		},
		{"isAllSmallCase", 	"[a-z]+"		},
		//{"isAlpha", 		"[a-zA-Z]+"		},
		{"isAlphaNumeric", 	"[a-zA-Z0-9]+"		},
		{"isAlphaDash", 		"([a-z]+[\\-])+[a-z]+"	},
		{"endsWithDot", 		"[^.]+\\."		}, 
		{"endsWithDotSingle", 	"[a-z]\\."		}, 
		{"endsWithComma", 	"[^,]+[,]"		}, 
		//{"endsWithColon", 	"[^:]+[:]"		}, 
		{"endsWithQuestion", 	"[^?]+[?]"		}, 
		{"containsSpecialCharacters", ".*[#;:\\-/<>'\"()&].*"},
		{"contains&;Characters", ".*[&;].*"},
		{"containsDigitRange", "\\d{1,3}[\\-]\\d{1,3}"}, 
		{"isMax3DigitNumber", "\\d{1,3}"}, 
		{"is4DigitNumber", "\\d{4}"},
		{"isMoreThan5Digit", "\\d{5,}"}
	};

	Pattern p[];
	int i;
	DataSequence data;
	int index;
	/**
	 * Constructs an object of CurrentTokenRegexFeatures to be used to generate features for current token.
	 *
	 * @param m		a {@link Model} object
	 */
	public CurrentTokenRegexFeatures(Model m) {
		super(m);
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
		index = pos;
		this.data = data;
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
		Matcher m = p[i].matcher((String)data.x(index));	
		f.ystart = -1;
		f.val = (m.matches()? 1:0); 
		f.strId.id = i;
		f.strId.name = patternString[i++][0];
	}
};
