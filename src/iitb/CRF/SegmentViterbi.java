package iitb.CRF;


/**
 *
 * NestedViterbi search
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SegmentViterbi extends SparseViterbi {
    SegmentCRF segmentModel;

    SegmentViterbi(SegmentCRF nestedModel, int bs) {
	super(nestedModel, bs);
	this.segmentModel = nestedModel;
    }
    void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
	SegmentTrainer.computeLogMi((CandSegDataSequence)dataSeq,i-ell,i,segmentModel.featureGenNested,lambda,Mi,Ri);
    }
    class SegmentIter extends Iter {
	int nc;
	CandidateSegments candidateSegs;
	void start(int i, DataSequence dataSeq) {
	    candidateSegs = (CandidateSegments)dataSeq;
	    nc = candidateSegs.numCandSegmentsEndingAt(i);
	}
	int nextEll(int i) {
	    nc--;
	    if (nc >= 0)
		return i -  candidateSegs.candSegmentStart(i,nc) + 1;
	    return -1;
	}
    }	
    Iter getIter(){return new SegmentIter();}
    public void bestLabelSequence(CandSegDataSequence dataSeq, double lambda[]) {
	viterbiSearch(dataSeq, lambda,false);
	Soln ybest = finalSoln.get(0);
	ybest = ybest.prevSoln;
	while (ybest != null) {
	    dataSeq.setSegment(ybest.prevPos()+1,ybest.pos,ybest.label);
	    ybest = ybest.prevSoln;
	}
    }
};
