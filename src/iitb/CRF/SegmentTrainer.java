package iitb.CRF;

import java.lang.*;
import java.io.*;
import java.util.*;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class SegmentTrainer extends SparseTrainer {
    public SegmentTrainer(CrfParams p) {
        super(p);
        logTrainer = true;
    }
    DoubleMatrix1D alpha_Y_Array[];
    LogSparseDoubleMatrix1D allZeroVector;
    void init(CRF model, DataIter data, double[] l) {
    	super.init(model,data,l);
    	allZeroVector = new LogSparseDoubleMatrix1D(numY);
    	allZeroVector.assign(0);
    }
    
    
    protected double computeFunctionGradient(double lambda[], double grad[]) {
        try {
            FeatureGeneratorNested featureGenNested = (FeatureGeneratorNested)featureGenerator;
            double logli = 0;
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*lambda[f]*params.invSigmaSquare;
                logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
            }
            diter.startScan();
            int numRecord;
            for (numRecord = 0; diter.hasNext(); numRecord++) {
                CandSegDataSequence dataSeq = (CandSegDataSequence)diter.next();
                if (params.debugLvl > 1)
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = RobustMath.LOG0;
                
                int base = -1;
                if ((alpha_Y_Array == null) || (alpha_Y_Array.length < dataSeq.length()-base)) {
                    alpha_Y_Array = new DoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < alpha_Y_Array.length; i++)
                        alpha_Y_Array[i] = new LogSparseDoubleMatrix1D(numY);
                }
                if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
                    beta_Y = new DoubleMatrix1D[2*dataSeq.length()];
                    for (int i = 0; i < beta_Y.length; i++)
                        beta_Y[i] = new LogSparseDoubleMatrix1D(numY);
                }
                int dataSize = dataSeq.length();
                DoubleMatrix1D oldBeta =  beta_Y[dataSeq.length()-1];
                beta_Y[dataSeq.length()-1] = allZeroVector;
                for (int i = dataSeq.length()-2; i >= 0; i--) {
                    beta_Y[i].assign(RobustMath.LOG0);
                }
                CandidateSegments candidateSegs = (CandidateSegments)dataSeq;
                for (int segEnd = dataSeq.length()-1; segEnd >= 0; segEnd--) {
                    for (int nc = candidateSegs.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                        int segStart = candidateSegs.candSegmentStart(segEnd,nc);
                        int ell = segEnd-segStart+1;
                        int i = segStart-1;
                        if (i < 0)
                            continue;
                        // compute the Mi matrix
                        computeLogMi(dataSeq,i,i+ell,featureGenNested,lambda,Mi_YY,Ri_Y);
                        tmp_Y.assign(Ri_Y);
                        if (i+ell < dataSize-1) tmp_Y.assign(beta_Y[i+ell], sumFunc);
                        Mi_YY.zMult(tmp_Y, beta_Y[i],1,1,false);
                    }
                }
                double thisSeqLogli = 0;
                boolean noneFired=true;
                alpha_Y_Array[0] = allZeroVector; //.assign(0);
                
                int trainingSegmentEnd=-1;
                int trainingSegmentStart = 0;
                boolean trainingSegmentFound = true;
                
                for (int segEnd = 0; segEnd < dataSize; segEnd++) {
                    alpha_Y_Array[segEnd-base].assign(RobustMath.LOG0);
                    
            		if (trainingSegmentEnd < segEnd) {
            			if ((!trainingSegmentFound)&& noneFired) {
            				System.out.println("Error: Training segment ("+trainingSegmentStart + " "+ trainingSegmentEnd + ") not found amongst candidate segments");
            			}
            			trainingSegmentFound = false;
            		    trainingSegmentStart = segEnd;
            		    trainingSegmentEnd = ((SegmentDataSequence)dataSeq).getSegmentEnd(segEnd);
            		}
                    
                    for (int nc = candidateSegs.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                        int ell = segEnd - candidateSegs.candSegmentStart(segEnd,nc)+1;
                        // compute the Mi matrix
                        computeLogMi(dataSeq,segEnd-ell,segEnd,featureGenNested,lambda,Mi_YY,Ri_Y);
                        boolean mAdded = false, rAdded = false;
                        if (segEnd-ell >= 0) {
                            Mi_YY.zMult(alpha_Y_Array[segEnd-ell-base],newAlpha_Y,1,0,true);
                            newAlpha_Y.assign(Ri_Y,sumFunc);
                        } else 
                            newAlpha_Y.assign(Ri_Y);
                        alpha_Y_Array[segEnd-base].assign(newAlpha_Y, RobustMath.logSumExpFunc);
                        
                        // find features that fire at this position..
                        featureGenNested.startScanFeaturesAt(dataSeq, segEnd-ell,segEnd);
                        while (featureGenNested.hasNext()) { 
                            Feature feature = featureGenNested.next();
                            int f = feature.index();
                            int yp = feature.y();
                            int yprev = feature.yprev();
                            float val = feature.value();
                            if (dataSeq.holdsInTrainingData(feature,segEnd-ell,segEnd)) {
                                grad[f] += val;
                                thisSeqLogli += val*lambda[f];
                                noneFired=false;
                                /*
                                double lZx = alpha_Y_Array[i-base].zSum();
                                if ((thisSeqLogli > lZx) || (numRecord == 3)) {
                                    System.out.println("This is shady: something is wrong Pr(y|x) > 1!");
                                    System.out.println("Sequence likelihood "  + thisSeqLogli + " " + lZx + " " + Math.exp(lZx));
                                    System.out.println("This Alpha-i " + alpha_Y_Array[i-base].toString());
                                }
                                */
                            }
                            if (yprev < 0) {
                                ExpF[f] = RobustMath.logSumExp(ExpF[f], (newAlpha_Y.get(yp)+Math.log(val)+beta_Y[segEnd].get(yp)));
                            } else {
                                ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[segEnd-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+Math.log(val)+beta_Y[segEnd].get(yp)));
                            }
                            
                            if (params.debugLvl > 3) {
                            	System.out.println(f + " " + feature + " " + dataSeq.holdsInTrainingData(feature,segEnd-ell,segEnd));
                            }
                        }
                        if ((segEnd == trainingSegmentEnd) && (segEnd-ell+1==trainingSegmentStart)) {
                        	trainingSegmentFound = true;
                        	double val1 = Ri_Y.get(dataSeq.y(trainingSegmentEnd));
                        	double val2 = 0;
                        	if (trainingSegmentStart > 0) {
                        		val2 = Mi_YY.get(dataSeq.y(trainingSegmentStart-1), dataSeq.y(trainingSegmentEnd));
                        	}
                        	if ((val1 == RobustMath.LOG0) || (val2 == RobustMath.LOG0)) {
                        		System.out.println("Error: training labels not covered in generated features " + val1 + " "+val2
                        				+ " yprev " + dataSeq.y(trainingSegmentStart-1) + " y " + dataSeq.y(trainingSegmentEnd));
                        	}
                        }
                    }
                    if (params.debugLvl > 2) {
                        System.out.println("Alpha-i " + alpha_Y_Array[segEnd-base].toString());
                        System.out.println("Ri " + Ri_Y.toString());
                        System.out.println("Mi " + Mi_YY.toString());
                        System.out.println("Beta-i " + beta_Y[segEnd].toString());
                    }
                    
                }
                double lZx = alpha_Y_Array[dataSeq.length()-1-base].zSum();
                thisSeqLogli -= lZx;
                logli += thisSeqLogli;
                // update grad.
                for (int f = 0; f < grad.length; f++)
                    grad[f] -= Math.exp(ExpF[f]-lZx);
                if (noneFired) {
                	System.out.println("WARNING: no features fired in the training set");
                }
                if (thisSeqLogli > 0) {
                    System.out.println("ERROR: something is wrong Pr(y|x) > 1!");
                }
                if (params.debugLvl > 1 || (thisSeqLogli > 0)) {
                    System.out.println("Sequence likelihood "  + thisSeqLogli + " " + lZx + " " + Math.exp(lZx));
                    System.out.println("Last Alpha-i " + alpha_Y_Array[dataSeq.length()-1-base].toString());
                }
                beta_Y[dataSeq.length()-1] = oldBeta;
            }
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(grad[f] + " ");
                System.out.println(" :g");
            }
            
            if (params.debugLvl > 0) {
            	if (icall == 0) {
            		Util.printDbg("Number of training records " + numRecord);
            	}
                Util.printDbg("Iter " + icall + " loglikelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));
            }
            return logli;
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return 0;
    }

	static void computeLogMi(CandSegDataSequence dataSeq, int prevPos, int pos, 
			FeatureGeneratorNested featureGenNested, double[] lambda, DoubleMatrix2D Mi, DoubleMatrix1D Ri) {
		featureGenNested.startScanFeaturesAt(dataSeq,prevPos,pos);
		Iterator constraints = dataSeq.constraints(prevPos,pos);
		double defaultValue = 0;
		if (constraints != null) {
			for (; constraints.hasNext();) {
				Constraint constraint = (Constraint)constraints.next();
				if (constraint.type() == Constraint.ALLOW_ONLY) {
					defaultValue = RobustMath.LOG0;
					Mi.assign(defaultValue);
					Ri.assign(defaultValue);
					RestrictConstraint cons = (RestrictConstraint)constraint;
					for (int c = cons.numAllowed()-1; c >= 0; c--) {
						Ri.set(cons.allowed(c),0);
					}
				}
			}
		} else {
			Mi.assign(defaultValue);
			Ri.assign(defaultValue);	
		}
		SparseTrainer.computeLogMiInitDone(featureGenNested,lambda,Mi,Ri,defaultValue);
	}
};
