package iitb.CRF;

import java.util.*;
import cern.colt.matrix.*;

class RobustMath {
    public static double LOG0 = -1*Double.MAX_VALUE;
    public static double LOG2 = 0.69314718055;
    static final float MINUS_LOG_EPSILON = 50;

    static double logSumExp(double v1, double v2) {
	if (v1 == v2)
	    return v1 + LOG2;
	double vmin = Math.min(v1,v2);
	double vmax = Math.max(v1,v2);
	if ( vmax > vmin + MINUS_LOG_EPSILON ) {
	    return vmax;
	} else {
	    return vmax + Math.log(Math.exp(vmin-vmax) + 1.0);
	}
    }
    static void logSumExp(DoubleMatrix1D v1, DoubleMatrix1D v2) {
	for (int i = 0; i < v1.size(); i++) {
	    v1.set(i,logSumExp(v1.get(i), v2.get(i)));
	}
    }
    static void addNoDups(TreeSet vec, Double val) {
	if (!vec.add(val)) {
	    vec.remove(val);
	    addNoDups(vec, new Double(val.doubleValue()+LOG2));
	}
    }
    // Controlled underflow adder of very small numbers expressed as
    // logs.  Returns log of their sum.
    static double logSumExp(DoubleMatrix1D logProb) {
	TreeSet logProbVector = new TreeSet();
	for ( int lpx = 0; lpx < logProb.size(); lpx++ )
	    addNoDups(logProbVector,new Double(logProb.get(lpx)));
	return logSumExp(logProbVector);
    }
    static double logSumExp(TreeSet logProbVector) {
	while ( logProbVector.size() > 1 ) {
	    double lp0 = ((Double)logProbVector.first()).doubleValue();
	    logProbVector.remove(logProbVector.first());
	    double lp1 = ((Double)logProbVector.first()).doubleValue();
	    logProbVector.remove(logProbVector.first());
	    addNoDups(logProbVector,new Double(logSumExp(lp0,lp1)));
	}
	return ((Double)logProbVector.first()).doubleValue();
    }
    static DoubleMatrix1D logMult(DoubleMatrix2D M, DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA, EdgeGenerator edgeGen) {
	// z = alpha * A * y + beta*z
	// in log domain this becomes: 
	
	double lalpha = 0;
	if (alpha != 1)
	    lalpha = Math.log(alpha);
	if (beta != 0) {
	    if (beta != 1) {
		for (int i = 0; i < z.size(); z.set(i,z.get(i)+Math.log(beta)),i++);
	    }
	} else {
	    z.assign(LOG0);
	}
	for (int j = 0; j < M.columns(); j++) {
	    for (int i = edgeGen.first(j); i < M.rows(); i = edgeGen.next(j,i)) {
		int r = i;
		int c = j;
		if (transposeA) {
		    r = j;
		    c = i;
		}
		z.set(r, logSumExp(z.get(r), M.get(i,j)+y.get(c)+lalpha));
	    }
	}
	return z;
    }
    static DoubleMatrix1D Mult(DoubleMatrix2D M, DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA, EdgeGenerator edgeGen) {
	// z = alpha * A * y + beta*z
	for (int i = 0; i < z.size(); z.set(i,z.get(i)*beta),i++);
	for (int j = 0; j < M.columns(); j++) {
	    for (int i = edgeGen.first(j); i < M.rows(); i = edgeGen.next(j,i)) {
		int r = i;
		int c = j;
		if (transposeA) {
		    r = j;
		    c = i;
		}
		z.set(r, z.get(r) + M.get(i,j)*y.get(c)*alpha);
	    }
	}
	return z;
    }
 };
