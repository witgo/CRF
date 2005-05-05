package iitb.CRF;

import java.util.*;
import cern.colt.function.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
//this needs to be done to support an efficient sparse implementation
//of matrices in the log-space

public class LogDenseDoubleMatrix1D extends DenseDoubleMatrix1D {
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
    public LogDenseDoubleMatrix1D(int numY) {super(numY);}
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
    public DoubleMatrix1D forEachNonZero(IntDoubleFunction func) {
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
            if ((v2.getQuick(row) != 0) || (getQuick(row) != 0))
                set(row,func.apply(get(row), v2.get(row)));
        }
        return this;
    }
    public boolean equals(Object arg) {
        DoubleMatrix1D mat = (DoubleMatrix1D)arg;
        for (int row = size()-1; row >= 0; row--)
            if (Math.abs(mat.get(row)-get(row))/Math.abs(mat.get(row)) > 0.0001)
                return false;
        return true;
    }
};

class LogDenseDoubleMatrix2D extends DenseDoubleMatrix2D {
    static double map(double val) { return LogSparseDoubleMatrix1D.map(val);}
    static double reverseMap(double val) { return LogSparseDoubleMatrix1D.reverseMap(val);}
    LogDenseDoubleMatrix2D(int numR, int numC) {super(numR,numC);
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
    
    public DoubleMatrix1D zMult(DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA) {
        return RobustMath.logMult(this,y,z,alpha,beta,transposeA);
    }
};

