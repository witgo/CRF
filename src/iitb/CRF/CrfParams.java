package iitb.CRF;

import java.lang.*;
import java.io.*;
import java.util.*;

/**
 * This class holds all parameters to control various aspects of the CRF model
 *
 * @author Sunita Sarawagi
 *
 */ 


public class CrfParams {
    /** initial value for all the lambda arrays */
    public double initValue = 0;
    /** penalty term for likelihood function is ||lambda||^2*invSigmaSquare/2
	set this to zero, if no penalty needed
    */
    public double invSigmaSquare = 1.0;
    /** Maximum number of iterations over the training data during training */
    public int maxIters = 50;
    /** Convergence criteria for finding optimum lambda using BFGS */
    public double epsForConvergence = 0.0001;
    /** The number of corrections used in the BFGS update. */
    public int mForHessian = 7;   

    public String trainerType = "";

    public int debugLvl = 1; // controls amount of status information output

    public boolean doScaling = true;
    /**
     * constructor with default values as follows.
     * initValue = 0;
     * invSigmaSquare = 1.0;
     * maxIters = 50;
     * epsForConvergence = 0.0001;
     * mForHessian = 7;   
     * trainerType = "";
     * debugLvl = 1;
     */
    public CrfParams() {}

    /** Initialize any parameter using space separated list of name value pairs
     *  Example: "initValue 0.1 maxIters 20" 
     *  will set the initValue param to 0.1 and maxIters param to 20.
     */
    public CrfParams(String args) {
	StringTokenizer tok = new StringTokenizer(args, " ");
	while (tok.hasMoreTokens()) {
	    String name = tok.nextToken();
	    if (name.equals("initValue")) {
		initValue = Double.parseDouble(tok.nextToken());
	    } else if (name.equals("maxIters")) {
		maxIters = Integer.parseInt(tok.nextToken());
	    } else if (name.equals("invSigmaSquare")) {
	        invSigmaSquare = Double.parseDouble(tok.nextToken());
	    } else if (name.equals("debugLvl")) {
		debugLvl = Integer.parseInt(tok.nextToken());
		System.out.println("Debug level :" + debugLvl);
	    } else if (name.equals("trainer")) {
		trainerType = tok.nextToken();
	    } else if (name.equals("scale")) {
		doScaling = tok.nextToken().equalsIgnoreCase("true");
	    }
	}
    }
    
    public CrfParams(java.util.Properties opts) {
	if (opts.getProperty("initValue") != null) {
	    initValue = Double.parseDouble(opts.getProperty("initValue"));
	} 
	if (opts.getProperty("maxIters") != null) {
	    maxIters = Integer.parseInt(opts.getProperty("maxIters"));
	} 
	if (opts.getProperty("invSigmaSquare") != null) {
	    invSigmaSquare = Double.parseDouble(opts.getProperty("invSigmaSquare"));
	} 
	if (opts.getProperty("debugLvl") != null) {
	    debugLvl = Integer.parseInt(opts.getProperty("debugLvl"));
	    System.out.println("Debug level :" + debugLvl);
	} 
	if (opts.getProperty("trainer") != null) {
	    trainerType = opts.getProperty("trainer");
	}
	if (opts.getProperty("scale") != null) {
	    doScaling = opts.getProperty("scale").equalsIgnoreCase("true");
	}
	
    }
};
