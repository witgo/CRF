package iitb.MaxentClassifier;
import java.util.*;
import java.io.*;
import iitb.CRF.*;
import iitb.Utils.*;
/**
 *
 * This class shows how to use the CRF package iitb.CRF for basic maxent 
 * classification where the features are provided as attributes of the 
 * instances to be classified. The number of classes can be more than two.
 *
 * @author Sunita Sarawagi
 *
 */ 


public class MaxentClassifier {
    FeatureGenRecord featureGen;
    CRF crfModel;
    DataDesc dataDesc;
    MaxentClassifier(Options opts) throws Exception {
	dataDesc = new DataDesc(opts);
	// read all parameters
	featureGen = new FeatureGenRecord(dataDesc.numColumns, dataDesc.numLabels);
	crfModel = new CRF(dataDesc.numLabels,featureGen,opts);
    }
    void train(String trainFile) throws IOException {
	// read training data from the  given file.
	crfModel.train(new DataSet(FileData.read(trainFile,dataDesc)));
    }
    void test(String testFile)  throws IOException {
	FileData fData = new FileData();
	fData.openForRead(testFile,dataDesc);
	DataRecord dataRecord = new DataRecord(dataDesc.numColumns);
	int confMat[][] = new int[dataDesc.numLabels][dataDesc.numLabels];
	while (fData.readNext(dataRecord)) {
	    int trueLabel = dataRecord.y();
	    crfModel.apply(dataRecord);
	    //	    System.out.println(trueLabel + " true:pred " + dataRecord.y());
	    confMat[trueLabel][dataRecord.y()]++;
	}
	// output confusion matrix etc directly.
	System.out.println("Confusion matrix ");
	for(int i=0 ; i<dataDesc.numLabels ; i++) {
	    System.out.print(i);
	    for(int j=0 ; j<dataDesc.numLabels ; j++) {
		System.out.print("\t"+confMat[i][j]);
	    }
	    System.out.println();
	}
    }
    public static void main(String args[]) {
	try {
	    Options opts = new Options(args);
	    MaxentClassifier maxent = new MaxentClassifier(opts);
	    maxent.train(opts.getMandatoryProperty("trainFile"));
	    System.out.println("Finished training...Starting test");
	    maxent.test(opts.getMandatoryProperty("testFile"));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
};
