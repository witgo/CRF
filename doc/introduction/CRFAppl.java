package iitb.CRFAppl;
import iitb.CRF.*;
import iitb.Model.*;
import iitb.Utils.*;
...;


public class CRFAppl {
    Properties options;
    CRF crfModel;
    FeatureGenImpl featureGen;

    public static void main(String argv[]) throws Exception {
	    /* 
	     * Initialization:
	     * Get the required arguements for the application here.
	     * Also, you will need to create a Properties object for arguements to be 
	     * passed to the CRF. You do not need to worry about this object, 
	     * because there are default values for all the parameters in the CRF package.
	     * You may need to pass your own parameters values for tuning the application 
	     * performance.
	     */

	    /*
	     * There are mainly two phases for a learning application: Training and Testing.
	     * Implement two routines for each of the phases and call them appropriately here.
	     */
	    train();
	    test();
    }

    public void train() throws Exception {
	    /*
	     * Read the training dataset into an object which implements DataIter 
	     * interface(trainData). Each of the training instance is encapsulated in the 
	     * object which provides DataSequence interface. The DataIter interface
	     * returns object of DataSequence (training instance) in next() routine.
	     */

	    /*
	     * Once you have loaded the training dataset, you need to allocate objects 
	     * for the model to be learned. allocmodel() method does that allocation.
	     */
		allocModel();
	
	    /*
	     * You may need to train some of the feature types class. This training is 
	     * needed for features which need to learn from the training data for instance
	     * dictionary features build generated from the training set.
	     */
	    featureGen.train(trainData);

	    /*
	     * Call train routine of the CRF model to train the model using the 
	     * train data. This routine returns the learned weight for the features.
	     */
	    double featureWts[] = crfModel.train(trainData);

	    /*
	     * You can store the learned model for later use into disk.
	     * For this you will have to store features as well as their 
	     * corresponding weights.
	     */
	    crfModel.write(baseDir+"/learntModels/"+outDir+"/crf");
	    featureGen.write(baseDir+"/learntModels/"+outDir+"/features");

    }

    public void test() throws Exception {
	    /*
	     * Read the test dataset. Each of the test instance is encapsulated in the 
	     * object which provides DataSequence interface. 
	     */

	    /*
	     * Once you have loaded the test dataset, you need to allocate objects 
	     * for the model to be learned. allocmodel() method does that allocation.
	     * Also, you need to read learned parameters from the disk stored after
	     * training. If the model is already available in the memory, then you do 
	     * not need to reallocate the model i.e. you can skip the next step in that
	     * case.
	     */
		allocModel();
		featureGen.read(baseDir+"/learntModels/"+outDir+"/features");
		crfModel.read(baseDir+"/learntModels/"+outDir+"/crf");
	
	    /*
	     * Iterate over test data set and apply the crf model to each test instance.
	     */
	    while(...) { 
	    	/*
		 * Now apply CRF model to each test instance.
		 */
		crfModel.apply(testRecord);

		/*
		 * The labeled instance have value of the states as labels. 
		 * These state values are not labels as supplied during training.
		 * To map this state to one of the labels you need to call following
		 * method on the labled testRecord.
		 */
		featureGen.mapStatesToLabels(testRecord);
	    }
    }

    void  allocModel() throws Exception {
	    /*
	     * A CRF model consists of features and corresponding weights.
	     * The features are stored in FeatureGenImpl and weights and other
	     * CRF parameters are encapsulated in CRF object.
	     *
	     * Here, you will call appropriate constructor for a feature generator 
	     * and a CRF model. You can use feature generator available in the 
	     * package or use your own implemented feature generator.
	     *
	     * There are two CRF model classes: CRF and NestedCRF. The CRF class is
	     * flat CRF model while NestedCRF is a segment(semi-)CRF model.
	     */ 
	    featureGen = new FeatureGenImpl(...);
	    crfModel=new CRF(...);
    }
 
};
