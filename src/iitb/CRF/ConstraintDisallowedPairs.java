/*
 * Created on Dec 31, 2004
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public interface ConstraintDisallowedPairs extends Constraint {
    boolean conflictingPair(int label1, int label2, boolean adjacent);
    /**
     * 
     * @param label
     * @return true if the label is disallowed with any other label
     */
    boolean conflicting(int label);
}
