/*
 * Created on Feb 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package iitb.Model;

import java.util.regex.Pattern;

import iitb.CRF.DataSequence;

/**
 * @author imran
 *
 */
public class RegexLengthFeatures extends FeatureTypes {

    String patternString[][] = {
    		{"isInitCapitalWord",     		"[A-Z][a-z]+"        },
    		{"isAllCapitalWord",      		"[A-Z][A-Z]+"                },
    		{"isAllSmallCase",      	"[a-z]+"                },
    		{"isWord",           		"[a-zA-Z][a-zA-Z]+"     },
    		{"isAlphaNumeric",      	"[a-zA-Z0-9]+"          },
    		{"singleCapLetter",  		"[A-Z]"  				},
    		{"isSpecialCharacter",		"[#;:\\-/<>'\"()&]"},
    		//{"singlePunctuation", 		"\\p{Punct}"			},
    		{"singleDot", 				"[.]"			},
    		{"singleComma", 			"[,]"			},
    		{"containsDigit", 			".*\\d+.*"		},				
    		{"isDigits", 				"\\d+"			},
    	};
    Pattern p[];
	int patternOccurence[], index, maxSegmentLength;
    /**
     * @param m
     */
    public RegexLengthFeatures(Model m, int maxSegmentLength) {
        super(m);
        this.maxSegmentLength = maxSegmentLength;
        p = new Pattern[patternString.length];
		for(int i = 0; i < patternString.length; i++)
			p[i] = Pattern.compile(patternString[i][1]);
		patternOccurence = new int[patternString.length];
    }

    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {        
        int i, j;
		for(j = 0; j < patternOccurence.length; j++)
		    patternOccurence[j] = 0;
		for(i = prevPos + 1; i <= pos; i++){
		    for(j = 0; j < p.length; j++){
		        if(p[j].matcher((String)data.x(i)).matches())
		            patternOccurence[j]++;
		    }
		}
		index = -1;
        return advance();
    }

    private boolean advance() {        
        while(++index < (patternOccurence.length) && patternOccurence[index] <= 0);
        return index < patternOccurence.length;
    }

    public boolean hasNext() {
        return index < patternOccurence.length;
    }

    public void next(FeatureImpl f) {
		f.val = 1;
		f.strId.id = maxSegmentLength * (index+1) + patternOccurence[index];
		f.ystart = -1;
		if(FeatureGenImpl.featureCollectMode){
			f.strId.name = patternString[index][0] + "_Length_" + patternOccurence[index];
			System.out.println((String)f.strId.name +" " +index + " " + f.strId.id);
		}
    	advance();
    }
}
