package iitb.Model;
import gnu.trove.TIntIntHashMap;
import iitb.CRF.*;

/**
 *
 * @author Sunita Sarawagi
 *
 * Suppose if history size is H and number of edges in the model graph
 * is E, this will generate HE features (for each history position
 * generate features for each possible edge)
 */ 

public class EdgeLinearHistFeatures extends FeatureTypes {
	Object fnames[][];
	int histsize;
	boolean allDone;
	int histArr[];
	int histPos;
	int pos;
	transient EdgeIterator edgeIter;
	Edge edge;
	int edgeNum;

	public EdgeLinearHistFeatures(FeatureGenImpl m, Object labels[][], int histsize) {
		super(m);
		fnames=labels;
		edgeIter = model.edgeIterator();
		this.histsize = histsize;
		histArr = new int[histsize];
	}
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		this.pos = pos;
		allDone = false;
		edgeIter.start();
		if ((pos < 2) || !edgeIter.hasNext())
			allDone = true;
		histPos = 1;
		edge = edgeIter.next();
		edgeNum=0;
		return allDone;
	}
	public boolean hasNext() {
		return (histsize > 1) && !allDone;
	}	
	public void next(FeatureImpl f) {
		// zero all other pos..
		for (int i = 0; i < histArr.length; histArr[i++] = -1);
		histArr[histPos] = edge.start;
		f.yend = edge.end;
		f.historyArray = histArr;
		f.val = 1;
		Object fname = null;
		if (featureCollectMode()) {
			if (fnames == null) {
				fname = "H."+histPos+"."+edgeNum;
			} else {
				fname = fnames[histPos][f.yend];
			}
		}
		setFeatureIdentifier(edgeNum*histsize + histPos, model.label(f.yend),fname,f);
		if (edgeIter.hasNext()) {
			edge = edgeIter.next();
			edgeNum++;
		} else {
			histPos++;	
			if (histPos+1 > histsize || pos - histPos <= 0) {
				allDone = true;
			} else {
				edgeIter.start();
				edgeNum = 0;
				edge = edgeIter.next();
			}
		}
	}
};
