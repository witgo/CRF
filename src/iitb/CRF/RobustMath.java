package iitb.CRF;

import java.io.Serializable;
import java.util.*;
import cern.colt.function.*;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

class RobustMath {
    public static double LOG0 = -1*Double.MAX_VALUE;
    public static double LOG2 = 0.69314718055;
    static final double MINUS_LOG_EPSILON = 50; //-1*Math.log(Double.MIN_VALUE);
    
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
    static class LogSumExp implements DoubleDoubleFunction {
        public double apply(double v1, double v2) {
            return logSumExp(v1,v2);
        }
    };
    static LogSumExp logSumExpFunc = new LogSumExp();
    static void addNoDups(TreeSet vec, double v) {
        Double val = new Double(v);
        if (!vec.add(val)) {
            vec.remove(val);
            addNoDups(vec, val.doubleValue()+LOG2);
        }
    }
    static double logSumExp(TreeSet logProbVector) {
        while ( logProbVector.size() > 1 ) {
            double lp0 = ((Double)logProbVector.first()).doubleValue();
            logProbVector.remove(logProbVector.first());
            double lp1 = ((Double)logProbVector.first()).doubleValue();
            logProbVector.remove(logProbVector.first());
            addNoDups(logProbVector,logSumExp(lp0,lp1));
        }
        if (logProbVector.size() > 0)
            return ((Double)logProbVector.first()).doubleValue();
        return RobustMath.LOG0;
    }
    
    // matrix stuff for the older version..
    static double logSumExp(DoubleMatrix1D logProb) {
        TreeSet logProbVector = new TreeSet();
        for ( int lpx = 0; lpx < logProb.size(); lpx++ )
            addNoDups(logProbVector,logProb.get(lpx));
        return logSumExp(logProbVector);
    }
    static void logSumExp(DoubleMatrix1D v1, DoubleMatrix1D v2) {
        for (int i = 0; i < v1.size(); i++) {
            v1.set(i,logSumExp(v1.get(i), v2.get(i)));
        }
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
    
    
    public static void main(String args[]) {
//      double vals[] = new double[]{10.172079, 7.452882, 2.429751, 7.452882, 10.818797, 8.573773, 19.215824};
        double vals[] = new double[]{2.883626, 1.670196, 0.553112, 1.670196, -0.935964, 1.864568, 2.064754};
        TreeSet vec = new TreeSet();
        double trueSum = 0;
        for (int i = 0; i < vals.length; i++) {
            addNoDups(vec,vals[i]);
            trueSum += Math.exp(vals[i]);
        }
        double sum = logSumExp(vec);
        System.out.println(Math.exp(sum) + " " + trueSum + " " + sum);
    }
};
