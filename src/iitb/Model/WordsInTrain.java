package iitb.Model;
import java.util.*;
import java.io.*;
import iitb.CRF.*;
/**
 *
 * This is created by FeatureGenTypes and is available for any
 * featureTypes class to use. What it does is provide you counts of
 * the number of times a word occurs in a state.
 * 
 * @author Sunita Sarawagi
 * */


class WordsInTrain {
    class HEntry {
	int index;
	int cnt;
	HEntry(int v) {
	    index = v;
	    cnt = 0;
	}
    };
    private Hashtable dictionary;
    private int cntsArray[][];
    private int cntsOverAllWords[];
    private int allTotal;

    int getIndex(Object w) {
	return ((HEntry)(dictionary.get(w))).index;
    }
    boolean inDictionary(Object w) {
	return (dictionary.get(w) != null);
    }
    int count(Object w) {
	HEntry entry = (HEntry)dictionary.get(w);
	return ((entry != null)?entry.cnt:0);
    }
    int count(int wordPos, int state) {
	return cntsArray[wordPos][state];
    }
    int count(int state) {
	return cntsOverAllWords[state];
    }
    int totalCount() {return allTotal;}

    int dictionaryLength() {return dictionary.size();}

    int nextStateWithWord(Object w, int prev) {
	if (!inDictionary(w))
	    return -1;
	int pos = getIndex(w);
	return nextStateWithWord(pos,prev);
    }
    int nextStateWithWord(int pos, int prev) {
	int k = 0;
	if (prev >= 0)
	    k = prev + 1;
	for (; k < cntsArray[pos].length; k++) {
	    if (cntsArray[pos][k] > 0)
		return k;
	}
	return -1;
    }

    private void addDictElem(Object x, int y) {
	HEntry index = (HEntry)dictionary.get(x);
	if (index == null) {
	    index = new HEntry(dictionary.size());
	    dictionary.put(x, index);
	}
	index.cnt++;
    }
    WordsInTrain() {
	dictionary = new Hashtable();
    }
    void setAggregateCnts(int numStates) {
	cntsOverAllWords = new int[numStates];
	for (int i = 0; i < numStates; i++) {
	    cntsOverAllWords[i] = 0;
	    for (int m = 0; m < cntsArray.length; m++)
		cntsOverAllWords[i] += cntsArray[m][i];
	    allTotal += cntsOverAllWords[i];
	}
    }
    void train(DataIter trainData, int numStates) {
	for (trainData.startScan(); trainData.hasNext();) {
	    DataSequence seq = trainData.next();
	    for (int l = 0; l < seq.length(); l++) {
		//		System.out.println(seq.x(l));
		addDictElem(seq.x(l),seq.y(l));
	    }
	}
	cntsArray = new int[dictionary.size()][numStates];
	for (trainData.startScan(); trainData.hasNext();) {
	    DataSequence seq = trainData.next();
	    for (int l = 0; l < seq.length(); l++) {
		cntsArray[getIndex(seq.x(l))][seq.y(l)]++;
	    }
	}
	setAggregateCnts(numStates);
    }
    public void read(BufferedReader in, int numStates) throws IOException {
	int dictLen = Integer.parseInt(in.readLine());
	cntsArray = new int[dictLen][numStates];
	String line;
	for(int l = 0; (l < dictLen) && ((line=in.readLine())!=null); l++) {
	    StringTokenizer entry = new StringTokenizer(line," ");
	    String key = entry.nextToken();
	    int pos = Integer.parseInt(entry.nextToken());
	    HEntry hEntry = new HEntry(pos);
	    dictionary.put(key,hEntry);
	    while (entry.hasMoreTokens()) {
		StringTokenizer scp = new StringTokenizer(entry.nextToken(),":");
		int state = Integer.parseInt(scp.nextToken());
		int cnt = Integer.parseInt(scp.nextToken());
		cntsArray[pos][state] = cnt;
		hEntry.cnt += cnt;
	    }
	}
	setAggregateCnts(numStates);
    }
    public void write(PrintWriter out) throws IOException {
	out.println(dictionary.size());
	for (Enumeration e = dictionary.keys() ; e.hasMoreElements() ;) {
	    Object key = e.nextElement();
	    int pos = getIndex(key);
	    out.print(key + " " + pos);
	    for (int s = nextStateWithWord(pos,-1); s != -1; 
		 s = nextStateWithWord(pos,s)) {
		out.print(" " + s + ":" + cntsArray[pos][s]);
	    }
	    out.println("");
	}	
    }
};
