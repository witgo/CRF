/*
 * Created on Nov 25, 2004
 *
 */
package iitb.CRF;

import java.util.Iterator;


/**
 * @author sunita
 *
 */
public interface CandSegDataSequence extends DataSequence, CandidateSegments {
    boolean holdsInTrainingData(Feature feature, int prevPos, int pos);
    void setSegment(int segStart, int segEnd, int label);
    public Iterator constraints(int prevPos, int pos);
}
