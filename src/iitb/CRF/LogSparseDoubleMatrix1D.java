package iitb.CRF;

import java.util.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
// this needs to be done to support an efficient sparse implementation
// of matrices in the log-space

class LogSparseDoubleMatrix1D extends SparseDoubleMatrix1D {
    static double map(double val) {
	if (val == RobustMath.LOG0)
	    return 0;
	if (val == 0) 
	    return Double.MIN_VALUE;
	return val;
    }
    static double reverseMap(double val) {
	if (val == 0) {
	    return RobustMath.LOG0;
	}
	if (val == Double.MIN_VALUE)
	    return 0;
	return val;
    }
    LogSparseDoubleMatrix1D(int numY) {super(numY);}
    public DoubleMatrix1D assign(double val) {
	return super.assign(map(val));
    }
    public void  set(int row, double val) {
	super.set(row,map(val));
    }
    public double  get(int row) {
	return reverseMap(super.get(row));
    }
    public double zSum() {
	TreeSet logProbVector = new TreeSet();
	// TODO
	for (int row = 0; row < size(); row++) {
	    if (getQuick(row) != 0)
		RobustMath.addNoDups(logProbVector,get(row));
	}
	return RobustMath.logSumExp(logProbVector);

    }
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public SparseDoubleMatrix1D forEachNonZero(IntDoubleFunction func) {
	for (int y = 0; y < size(); y++) {
	    if (getQuick(y) != 0) 
		setQuick(y,func.apply(y,get(y)));
	}
	return this;
    }
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public DoubleMatrix1D assign(DoubleMatrix1D v2, DoubleDoubleFunction func) {
	// TODO..
	for (int row = 0; row < size(); row++) {
	    // if (v2.getQuick(row) != 0)
	    set(row,func.apply(get(row), v2.get(row)));
	}
	return this;
    }
};

class LogSparseDoubleMatrix2D extends SparseDoubleMatrix2D {
    static double map(double val) { return LogSparseDoubleMatrix1D.map(val);}
    static double reverseMap(double val) { return LogSparseDoubleMatrix1D.reverseMap(val);}
    LogSparseDoubleMatrix2D(int numR, int numC) {super(numR,numC);
    }
    public DoubleMatrix2D assign(double val) {
	return super.assign(map(val));
    }
    public void  set(int row, int column, double val) {
	super.set(row,column,map(val));
    }
    public double  get(int row, int column) {
	return reverseMap(super.get(row,column));
    }
    static class LogMult implements IntIntDoubleFunction {
	DoubleMatrix2D M;
	DoubleMatrix1D z;
	double lalpha;
	boolean transposeA;
	DoubleMatrix1D y;
	public double apply(int i, int j, double val) {
	    int r = i;
	    int c = j;
	    if (transposeA) {
		r = j;
		c = i;
	    }
	    z.set(r, RobustMath.logSumExp(z.get(r), M.get(i,j)+y.get(c)+lalpha));
	    return val;
	}
    };
    static LogMult logMult = new LogMult();
    public DoubleMatrix1D zMult(DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA) {
	// z = alpha * A * y + beta*z
	double lalpha = 0;
	if (alpha != 1)
	    lalpha = Math.log(alpha);
	if (beta != 0) {
	    if (beta != 1) {
		double lbeta = Math.log(beta);
		for (int i = 0; i < z.size(); z.set(i,z.get(i)+lbeta),i++);
	    }
	} else {
	    z.assign(RobustMath.LOG0);
	}
	// in log domain this becomes: 
	logMult.M = this;
	logMult.z = z;
	logMult.lalpha = lalpha;
	logMult.transposeA = transposeA;
	logMult.y = y;
	forEachNonZero(logMult);
	return z;
    }
};
