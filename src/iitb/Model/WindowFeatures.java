/*
 * Created on Dec 6, 2004
 *
 */
package iitb.Model;

import java.io.Serializable;

import iitb.CRF.DataSequence;

/**
 * @author Administrator
 *
 * Define features for windows of ranges
 */
public class WindowFeatures extends FeatureTypes {
    private static final long serialVersionUID = 6123;
    FeatureTypes single;
	int currentWindow;
	int prevPos;
	int pos;
	transient DataSequence dataSeq;
	
	public static class Window implements Serializable {
	    int start;
	    boolean startRelativeToLeft;
	    int end;
	    boolean endRelativeToLeft;
	    String winName=null;
	       public Window(int start, boolean startRelativeToLeft, int end,
                boolean endRelativeToLeft) {
			this(start,startRelativeToLeft,end,endRelativeToLeft,null);
	       	String startB = startRelativeToLeft?"L":"R";
	       	String endB = endRelativeToLeft?"L":"R";
	       	winName = startB + start + endB + end;
	    }	
        public Window(int start, boolean startRelativeToLeft, int end,
                boolean endRelativeToLeft, String winName) {
            this.start = start;
            this.startRelativeToLeft = startRelativeToLeft;
            this.end = end;
            this.endRelativeToLeft = endRelativeToLeft;
            this.winName = winName;
        }
        
        int leftBoundary(int segStart, int segEnd, int maxLen) {
            if (startRelativeToLeft)
                return boundary(segStart,start, maxLen);
             return boundary(segEnd,start,maxLen);
        }

        int rightBoundary(int segStart, int segEnd, int maxLen) {
            if (endRelativeToLeft)
                return boundary(segStart,end, maxLen);
             return boundary(segEnd,end,maxLen);
        }
        /**
         * @param segStart
         * @param start2
         * @param maxLen
         * @return
         */
        private int boundary(int boundary, int offset, int maxLen) {
           // return Math.max(0,Math.min(boundary+offset,maxLen));
            return boundary+offset;
        }
        public String toString() {
        	return winName;
        }
	}
	Window windows[];
    private int dataLen;
	/**
	 * 
	 */
	public WindowFeatures(Window windows[], FeatureTypes single) {
		super(single.model);
		this.single = single;
		this.windows = windows;
	}

	boolean advance(boolean firstCall) {
	    while (firstCall || !single.hasNext()) {
	        currentWindow--;
	        if (currentWindow < 0)
	            return false;
	        int rightB = windows[currentWindow].rightBoundary(prevPos+1,pos,dataLen-1);
	        int leftB = windows[currentWindow].leftBoundary(prevPos+1,pos,rightB);
	 
	        if ((leftB < dataLen) && (rightB >= 0) && (leftB <= rightB)) {
	            single.startScanFeaturesAt(dataSeq,Math.max(leftB,0)-1, Math.min(rightB,dataLen-1));
	            firstCall = false;
	        }
	    }
	    return true;
	}
	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	    currentWindow = windows.length;
	    dataSeq = data;
	    dataLen = dataSeq.length();
	    this.prevPos = prevPos;
	    this.pos = pos;
		return advance(true);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return single.hasNext();
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
	    single.next(f);
	    String name = "";
	    if (featureCollectMode) {
	        name += f.strId.name + ".W." + windows[currentWindow];
	    }
	    setFeatureIdentifier(f.strId.id*windows.length+currentWindow, f.strId.stateId, name, f);
	    advance(false);
	}

	public boolean requiresTraining() {
		return single.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		single.train(data, pos);
	}
}
