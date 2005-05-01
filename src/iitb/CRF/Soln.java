/*
 * Created on Apr 25, 2005
 *
 */
package iitb.CRF;

import java.io.Serializable;

/**
 * @author sunita
 *
 */
class Soln implements Serializable {
    private static final long serialVersionUID = 812L;
    float score=-1*Float.MAX_VALUE;
    Soln prevSoln=null;
    int label = -1;
    int pos;
    
    Soln(int id, int p) {label = id;pos = p;}
    void clear() {
        score=-1*Float.MAX_VALUE;
        prevSoln=null;
    }
    boolean isClear() {
        return (score == -1*Double.MAX_VALUE);
    }
    void copy(Soln soln) {
        score = soln.score;
        prevSoln = soln.prevSoln;
    }
    int prevPos() {
        return (prevSoln == null)?-1:prevSoln.pos;
    }
    int prevLabel() {
        return (prevSoln == null)?-1:prevSoln.label;
    }
    boolean equals(Soln s) {
        return (label == s.label) && (pos == s.pos) && (prevPos() == s.prevPos()) && (prevLabel() == s.prevLabel());
    }
    /**
     * @param prevSoln2
     * @param score2
     */
    protected void setPrevSoln(Soln prevSoln, float score) {
        this.prevSoln = prevSoln;
        this.score = score;
    }
};
