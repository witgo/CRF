package iitb.Segment;
import java.io.*;
import java.util.*;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 


class DCTrainRecord implements TrainRecord {
    int[] ls;
    String[][] _tokens;

    int[] labelsPerToken;
    int[] snum, spos;

    DCTrainRecord(int[] ts, String[][] toks) {
	ls = ts;
	_tokens = toks;

	int len = 0;
	for (int i = 0; i < numSegments(); i++) {
	    len+= _tokens[i].length;
	}
	labelsPerToken = new int[len];
	snum = new int[len];
	spos = new int[len];
	int pos = 0;
	for (int i = 0; i < ls.length; i++) {
	    for (int p = 0; p < _tokens[i].length; p++) {
		spos[pos] = p;
		snum[pos] = i;
		labelsPerToken[pos++] = ls[i];
	    }
	}
    }
    public int[] labels() {
	return ls;
    }
    public void set_y(int i, int l) {labelsPerToken[i] = l;} // not applicable for training data.
    public int length() {return labelsPerToken.length;}
    public Object x(int i) {return _tokens[snum[i]][spos[i]];}
    public int y(int i) {return  labelsPerToken[i];}

    public int numSegments() {
	return ls.length;
    }
    public int numSegments(int l) {
	int sz = 0;
	for (int i = 0; i < ls.length; i++)
	    if (ls[i] == l) sz++;
	return sz;
    }
    public String[] tokens(int snum) {
	return _tokens[snum];
    }
    public String[] tokens(int l, int p) {
	int pos = 0;
	for (int i = 0; i < ls.length; i++)
	    if (ls[i] == l) {
		if (pos == p)
		    return _tokens[i];
		pos++;
	    }
	return null;
    }
};

class DCTrainData implements TrainData {
    Vector trainRecs;
    int pos;
    DCTrainData(Vector trs) {
	trainRecs = trs;
    }
    public int size() {
	return trainRecs.size();
    }
    public void startScan() {
	pos = 0;
    }
    public TrainRecord nextRecord() {
	return (TrainRecord)trainRecs.elementAt(pos++);
    }
    public boolean hasMoreRecords() {
	return (pos < size());
    }
    public boolean hasNext() {
	return hasMoreRecords();
    }
    public DataSequence next() {
	return nextRecord();
    }
};

class TestData {
    BufferedReader rin;
    String line;
    String seq[];
    String fname;
    String delimit, impDelimit;
    TestData(String file,String delimitP,String impDelimitP, String grpDelimit) {
	try {
	    fname = file;
	    rin =new BufferedReader(new FileReader(file+".raw"));
	    delimit = delimitP;
	    impDelimit = impDelimitP;
	}  catch(IOException e) {
	    System.out.println("I/O Error"+e);
	    System.exit(-1);
	}
    }
    void startScan() {
	try {
	    rin =new BufferedReader(new FileReader(fname+".raw"));
	}  catch(IOException e) {
	    System.out.println("I/O Error"+e);
	    System.exit(-1);
	}   
    }
    int[] groupedTokens() {
	/*
	if (grp == null)
	    return null;
	return grp.groupingArray(seq.length);
	*/
	return null;
    }
    String[] nextRecord() {
	try {
	    if ((line=rin.readLine())!=null) {
		StringTokenizer tok=new StringTokenizer(line.toLowerCase(),delimit,true);
		int len = tok.countTokens();
		if ((seq == null) || (seq.length < len))
		    seq =new String[len];
		int count=0;
		for(int i=0 ; i<len; i++) {
		    String tokStr=tok.nextToken();
		    if (delimit.indexOf(tokStr)==-1 || impDelimit.indexOf(tokStr)!=-1) {
			seq[count++]=new String(tokStr);
		    } 
		}
		String aseq[]=new String[count];
		for(int i=0 ; i<count ; i++) {
		    aseq[i]=seq[i];
		}
		return aseq;
	    } else {
		rin.close();
		return null;
	    }
	} catch(IOException e) {
	    System.out.println("I/O Error"+e);
	    System.exit(-1);
	}
	return null;
    }
};

class TestDataWrite {
    PrintWriter out;
    BufferedReader rin;
    String outputBuffer;
    String rawLine;
    String delimit, tagDelimit, impDelimit;
    LabelMap labelmap;
    TestDataWrite(String outfile,String rawfile,String delimitP,String tagDelimitP,String impDelimitP, LabelMap linfo) {
	try {
	    labelmap = linfo;
	    out=new PrintWriter(new FileOutputStream(outfile+".tagged"));
	    rin=new BufferedReader(new FileReader(rawfile+".raw"));
	    outputBuffer=new String();
	    delimit = delimitP;
	    tagDelimit = tagDelimitP;
	    impDelimit = impDelimitP;
	} catch(IOException e) {
	    System.err.println("I/O Error"+e);
	    System.exit(-1);
	}
    }	
    void writeRecord(int[] tok, int tokLen) {
	try {
		rawLine=rin.readLine();
		StringTokenizer rawTok=new StringTokenizer(rawLine,delimit,true);
		String tokArr[]=new String[rawTok.countTokens()];
		for(int j=0 ; j<tokArr.length ; j++) {
		    tokArr[j]=rawTok.nextToken();
		}
		int ptr=0;
		int t=tok[0];
		for(int j=0 ; j<=tokLen ; j++) {
		    if ((j < tokLen) && (t==tok[j])) {
			while(ptr<tokArr.length && delimit.indexOf(tokArr[ptr])!=-1 && impDelimit.indexOf(tokArr[ptr])==-1) {
			    outputBuffer=new String(outputBuffer+tokArr[ptr]);

			    ptr++;
			}
			if (ptr<tokArr.length) {
			    outputBuffer=new String(outputBuffer+tokArr[ptr]);
			    ptr++;
			}
			while(ptr<tokArr.length && delimit.indexOf(tokArr[ptr])!=-1 && impDelimit.indexOf(tokArr[ptr])==-1) {
			    outputBuffer=new String(outputBuffer+tokArr[ptr]);
			    ptr++;
			}
		    } else {
			int revScanPtr=outputBuffer.length()-1;
			int goBackPtr=0;
			boolean foundOpenChar=false;
			while(outputBuffer.charAt(revScanPtr)==' ' || outputBuffer.charAt(revScanPtr)=='(' || outputBuffer.charAt(revScanPtr)=='{' || outputBuffer.charAt(revScanPtr)=='[') {
			    char currChar=outputBuffer.charAt(revScanPtr);
			    if (impDelimit.indexOf(currChar)!=-1) {
				break;
			    }
			    if (currChar=='{' || currChar=='[' || currChar=='(') {
				foundOpenChar=true;
			    }
			    revScanPtr--;
			    goBackPtr++;
			}
			if (foundOpenChar) {
			    outputBuffer=outputBuffer.substring(0,revScanPtr+1);
			    ptr-=goBackPtr;
			}
			outputBuffer=new String(outputBuffer+tagDelimit+labelmap.revMap(t));
			out.println(outputBuffer);
			outputBuffer=new String();
			//						out.println(tagDelimit+t);
			//						System.out.println(tagDelimit+t);
			if (j < tokLen) {
			    t=tok[j];
			    j--;
			}
		    }
		}
		out.println();
	}  catch(IOException e) {
	    System.err.println("I/O Error"+e);
	    System.exit(-1);
	}
    }
    void close() {
	try {
	    rin.close();
	    out.close();
	}  catch(IOException e) {
	    System.err.println("I/O Error"+e);
	    System.exit(-1);
	} 
    }
};


    
public class DataCruncher {

    static String[]  getTokenList(String text, String delimit, String impDelimit) {
	StringTokenizer textTok=new StringTokenizer(text.toLowerCase(),delimit,true);
	int tlen = 0;
	while (textTok.hasMoreTokens()) {
	    String tokStr=textTok.nextToken();
	    if (delimit.indexOf(tokStr)==-1 || impDelimit.indexOf(tokStr)!=-1){
		tlen++;
	    }
	}
	String[] cArray = new String[tlen];
	tlen = 0;
	textTok=new StringTokenizer(text.toLowerCase(),delimit,true);
	while (textTok.hasMoreTokens()) {
	    String tokStr=textTok.nextToken();
	    if (delimit.indexOf(tokStr)==-1 || impDelimit.indexOf(tokStr)!=-1) {	    
		cArray[tlen++] = tokStr;
	    }
	}
	return cArray;
    }
    static int readRowVarCol(int numLabels, BufferedReader tin, String tagDelimit, String delimit, String impDelimit, int[] t, String[][] cArray) throws IOException 
    {
	int ptr=0;
	int previousLabel = -1;
	while(true) {
	    String line=tin.readLine();
	    StringTokenizer firstSplit=null;
	    if (line!=null) {
		firstSplit=new StringTokenizer(line.toLowerCase(),tagDelimit);
	    }
	    if ((line==null) || (firstSplit.countTokens()<2)) {
		// Empty Line
		return ptr;
	    }
	    String w = firstSplit.nextToken();
	    int label=Integer.parseInt(firstSplit.nextToken()); 
	    //if ((!c[label].equals(" ")) && (previousLabel != label)) {
	    //	System.out.println("WARNING: duplicate tags in training data are not allowed: " + w);
	    //}
	    t[ptr] = label;
	    cArray[ptr++] = getTokenList(w,delimit,impDelimit);
	    previousLabel = label;
	}
    }

    static int readRowFixedCol(int numLabels, BufferedReader tin, String tagDelimit, String delimit, String impDelimit, int[] t, String[][] cArray, int labels[], StringTokenizer rawTok) throws IOException {
	String line=tin.readLine();
	if (line == null)
	    return 0;
	StringTokenizer firstSplit=new StringTokenizer(line.toLowerCase(),tagDelimit,true);
	int ptr = 0;
	for (int i = 0; (i < labels.length) 
		 && firstSplit.hasMoreTokens(); i++) {
	    int label = labels[i];
	    String w = firstSplit.nextToken();
	    if (tagDelimit.indexOf(w)!=-1) {
		continue;
	    } else {
		if (firstSplit.hasMoreTokens())
		    // read past the delimiter.
		    firstSplit.nextToken();
	    }
	    if ((label > 0) && (label <= numLabels)) {
		t[ptr] = label;
		cArray[ptr++] = getTokenList(w,delimit,impDelimit);
	    }
	}
	return ptr;
    }
    static int[] readHeaderInfo(int numLabels, BufferedReader tin, String tagDelimit) throws IOException {
	tin.mark(1000);
	String line = tin.readLine();
	if (line == null)
	    throw new IOException("Header row not present in tagged file");
	// System.out.println(line);
	if (! line.toLowerCase().startsWith("fixed-column-format")) {
	    tin.reset();
	    return null;
	}
	line = tin.readLine();
	StringTokenizer firstSplit=new StringTokenizer(line,tagDelimit);
	int labels[] = new int[numLabels];
	for (int i = 0; (i < numLabels)&& firstSplit.hasMoreTokens();) {
	    labels[i++] = Integer.parseInt(firstSplit.nextToken());
	}
	return labels;
    }
    public static TrainData readTagged(int numLabels,String tfile,String rfile,String delimit,String tagDelimit,String impDelimit, LabelMap labelMap) {
	try {
	    Vector td = new Vector();
	    BufferedReader tin=new BufferedReader(new FileReader(tfile+".tagged"));
	    BufferedReader rin=new BufferedReader(new FileReader(rfile+".raw"));
	    boolean fixedColFormat = false;
	    String rawLine;
	    StringTokenizer rawTok,temp;
	    int t[] = new int[0];
	    String[] zeroString = new String[0];
	    String cArray[][] = new String[0][0];
	    int[] labels = null;
	    // read list of columns in the header of the tag file
	    labels = readHeaderInfo(numLabels,tin,tagDelimit);
	    if (labels != null)
		fixedColFormat = true;
	    while((rawLine=rin.readLine())!=null) {
		rawTok=new StringTokenizer(rawLine,delimit,true);
		int len = rawTok.countTokens();
		if (len > t.length) {
		    t=new int[len];
		    cArray=new String[len][0];
		}
		int ptr = 0;
		if (fixedColFormat) {
		    ptr = readRowFixedCol(numLabels, tin, tagDelimit, delimit, impDelimit,t,cArray,labels,rawTok);
		} else {
		    ptr = readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray);
		}
		if (ptr == 0)
		    break;
		int at[]=new int[ptr];
		String[][] c = new String[ptr][0];
		for(int i=0 ; i<ptr ; i++) {
		    at[i]=labelMap.map(t[i]);
		    c[i] = cArray[i];
		}
		td.add(new DCTrainRecord(at,c));
	    }
	    return new DCTrainData(td);
	} catch(IOException e) {
	    System.err.println("I/O Error"+e);
	    System.exit(-1);
	}
	return null;
    }
    public static void readRaw(Vector data,String file,String delimit,String impDelimit) {
	try {
	    BufferedReader rin=new BufferedReader(new FileReader(file+".raw"));
	    String line;
	    while((line=rin.readLine())!=null) {
		StringTokenizer tok=new StringTokenizer(line.toLowerCase(),delimit,true);
		String seq[]=new String[tok.countTokens()];
		int count=0;
		for(int i=0 ; i<seq.length ; i++) {
		    String tokStr=tok.nextToken();
		    if (delimit.indexOf(tokStr)==-1 || impDelimit.indexOf(tokStr)!=-1) {
			seq[count++]=new String(tokStr);
		    } 
		}
		String aseq[]=new String[count];
		for(int i=0 ; i<count ; i++) {
		    aseq[i]=seq[i];
		}
		data.add(aseq);
	    }
	    rin.close();
	} catch(IOException e) {
	    System.out.println("I/O Error"+e);
	    System.exit(-1);
	}
    }

    public static void createRaw(String file,String tagDelimit) {
	try {
	    BufferedReader in=new BufferedReader(new FileReader(file+".tagged"));
	    PrintWriter out=new PrintWriter(new FileOutputStream(file+".raw"));
	    String line,rawLine;
	    rawLine=new String("");
	    while((line=in.readLine())!=null) {
		StringTokenizer t=new StringTokenizer(line,tagDelimit);
		if(t.countTokens()<2) {
		    out.println(rawLine);
		    rawLine=new String("");
		} else {
		    rawLine=new String(rawLine+" "+t.nextToken());
		}
	    }
	    out.println(rawLine);
	    in.close();
	    out.close();
	} catch(IOException e) {
	    System.out.println("I/O Error"+e);
	    System.exit(-1);
	}
    }
};
