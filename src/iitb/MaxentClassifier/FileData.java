/** FileData.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.MaxentClassifier;
import java.io.*;
import java.util.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 


// interface for reading and writing fixed column record data
public class FileData {
    BufferedReader inpStream;
    DataDesc dataDescriptor;
    public void openForRead(String fileName, DataDesc data) throws IOException {
	 inpStream=new BufferedReader(new FileReader(fileName));
	 dataDescriptor = data;
    }
    boolean readNext(DataRecord dataRecord)  throws IOException {
	return readNext(inpStream,dataDescriptor,dataRecord);
    }
    static boolean readNext(BufferedReader in, DataDesc dataDesc, DataRecord dataRecord) throws IOException {
	String line;
	if ((line=in.readLine())!=null) {
	    StringTokenizer strTok = new StringTokenizer(line, dataDesc.colSep);
	    
	    for (int colNum = 0; strTok.hasMoreTokens() && (colNum < dataDesc.numColumns); colNum++) {
		dataRecord.vals[colNum] = (float)Double.parseDouble(strTok.nextToken());
	    }
	    assert (strTok.hasMoreTokens());
	    dataRecord.label = Integer.parseInt(strTok.nextToken());
	    assert ((dataRecord.label >= 0) && (dataRecord.label < dataDesc.numLabels));
	    return true;
	}
	return false;
    }
    public static Vector read(String fileName, DataDesc dataDesc) throws IOException {
	Vector allRecords = new Vector();
	BufferedReader in=new BufferedReader(new FileReader(fileName));
	DataRecord dataRecord = new DataRecord(dataDesc.numColumns);
	while (readNext(in,dataDesc,dataRecord)) {
	    allRecords.add(new DataRecord(dataRecord));
	}
	return allRecords;
    }
    static void write(String fileName, Vector<DataRecord> allRecords, int numColumns, String colSep) throws IOException {
	PrintWriter out=new PrintWriter(new FileOutputStream(fileName));
	for(Enumeration<DataRecord> e = allRecords.elements(); e.hasMoreElements();) {
	    DataRecord dataRecord = (DataRecord)e.nextElement();
	    for (int i = 0; i < numColumns; i++) {
		out.print(dataRecord.getColumn(i) + colSep);
	    }
	    out.println(dataRecord.y(0));
	}
	out.close();
    }
    class FileIterator implements Iterator {
        DataRecord dataRecord;
        FileIterator() {
            dataRecord = new DataRecord(dataDescriptor.numColumns);
        }
        public boolean hasNext() {
            try {
                return readNext(dataRecord);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        public Object next() {
            return dataRecord;
        }
        public void remove() {
        }
    }
    public Iterator iterator() {
        return new FileIterator();
    }
};
    
