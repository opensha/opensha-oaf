package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.special.Gamma;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;

/**
 * This class takes an array of total earthquake counts and produces the MND quantiles assuming each count gives the value of a GR distribution at M = Mc.
 * 
 * @author Nicholas van der Elst
 *
 **/

public class StackedMND {
//	private double[][] prob;
	private boolean D = false;
	
	public EvenlyDiscretizedFunc[] mfdArray;
	public EvenlyDiscretizedFunc probability;
	public int[] eventCounts;
	public double magComplete;
	private double[] q;
	private int minQuant = 1;
		
	
	public StackedMND(EvenlyDiscretizedFunc[] mfdArray, int[] eventCounts, double magComplete, double b, double[] quantiles){
		this.eventCounts = eventCounts;
		this.mfdArray = mfdArray;
		EvenlyDiscretizedFunc probability =
				new EvenlyDiscretizedFunc(mfdArray[0].getMinX(), mfdArray[0].getMaxX(), mfdArray[0].size());
		this.probability = probability;
		this.magComplete = magComplete;
		
		generateQuantiles(b, quantiles);
		
	}
	
	public StackedMND(double mag, int[] eventCounts, double magComplete, double b, double[] quantiles){
		this.eventCounts = eventCounts;
		
//		EvenlyDiscretizedFunc probability =
//				new EvenlyDiscretizedFunc(mfdArray[0].getMinX(), mfdArray[0].getMaxX(), mfdArray[0].size());
//		this.probability = probability;
		this.magComplete = magComplete;
		
		generateSingleQuantile(mag, b, quantiles);
	}
	
	
	private void generateQuantiles(double b, double[] quantiles){
//		EvenlyDiscretizedFunc[] mfdArray = new EvenlyDiscretizedFunc[quantiles.length];
		
		double dM = mfdArray[0].getDelta();
		int numMbins = mfdArray[0].size();
		
		double p = Math.pow(10, -b*dM);
		double[][] q = new double[quantiles.length][numMbins]; 
		
		if(D) System.out.println("total number of entries = " + (int) eventCounts.length);
		
		int[] N = eventCounts;
				
		// do the mags above magComplete first and then scale the fractiles to lower ranges
		double u;
		int n;
		double quantTemp, lambda;
		int indexComplete = mfdArray[0].getClosestXIndex(magComplete);
		
		for (int j = indexComplete; j < numMbins; j++) {
			// sort the vector of Ns
			Arrays.sort(N); 
			
			// compute probability of at least one
			n = 0;
			for (int i = 0; i < N.length; i++) {
				if (N[i] > 0) n++; 
			}
			probability.set(j, (double) n / N.length);

			// take the quantiles from the sorted vector
			for (int i = 0; i < quantiles.length; i++) {
				quantTemp = N[(int) (quantiles[i]*N.length)];
				// make a correction to a fractional quantile if the quantile is small.
				// ...
				if(quantTemp <= minQuant && probability.getY(j) > 1d/(N.length+1d)){
					if(D) System.out.println("using Poisson approximation for fractile");
					lambda = Math.abs(-Math.log(1 - probability.getY(j)));
					quantTemp = poissQuantile(lambda, quantiles[i]);
				} 
				q[i][j] = quantTemp;
				
				
				mfdArray[i].set(j, (int)(q[i][j]));
			}

			// generate a new vector of N, reduced by probability of remaining in the next M column
			for (int i = 0; i < N.length; i++) {
				// generate a random vector
				n = 0;
				for (int k = 0; k < N[i]; k++) {
					u = Math.random();
					if (u < p) n++;
				}
				N[i] = n;
			}
		}

		//now do the quantiles below the simulated magnitude   
		for (int j = 0; j < indexComplete; j++) { //(If i use smaller than or equal to indexComplete, it redoes it, and gets wrong numbers for M>Mcomplete. If I use smaller than, it gets the wrong numbers for M<Mcomplete...
			double f = Math.pow(10,  -b*(mfdArray[0].getX(j) - magComplete));
			
			for (int i = 0; i < quantiles.length; i++) {
				// just extend the quantiles from the magComplete
				q[i][j] = q[i][indexComplete] * f;
				mfdArray[i].set(j, (int)(q[i][j]));
			}
			
			double pComplete = probability.getY(indexComplete);
			probability.set(j, 1d - Math.pow(1d - pComplete, f));
		}

		
		if(D) {
			for (int i = 0; i < quantiles.length; i++) {
				for (int j = 0; j < numMbins; j++)
					System.out.printf("%d ", (int)(q[i][j]+0.5));
				System.out.printf("\n");
			}
		}
		
	}
	
	private void generateSingleQuantile(double mag, double b, double[] quantiles){
		//		EvenlyDiscretizedFunc[] mfdArray = new EvenlyDiscretizedFunc[quantiles.length];

		//		double dM = mfdArray[0].getDelta();
		//		int numMbins = mfdArray[0].size();
		//		
		//		double p = Math.pow(10, -b*dM);
		//		double[][] q = new double[quantiles.length][numMbins]; 

		double[] q = new double[quantiles.length];

		//		if(D) System.out.println("total number of entries = " + (int) eventCounts.length);

		int[] N = eventCounts;

		// do the mags above magComplete first and then scale the fractiles to lower ranges
		double u;
		int n;
		double quantTemp, lambda;
		//		int indexComplete = mfdArray[0].getClosestXIndex(magComplete);

		//		for (int j = indexComplete; j < numMbins; j++) {
		// sort the vector of Ns
		Arrays.sort(N); 

		// compute probability of at least one
		n = 0;
		for (int i = 0; i < N.length; i++) {
			if (N[i] > 0) n++; 
		}
		double probability = (double) n/N.length;
//		probability.set(0, (double) n / N.length);

		// take the quantiles from the sorted vector
		for (int i = 0; i < quantiles.length; i++) {
			quantTemp = N[(int) (quantiles[i]*N.length)];
			// make a correction to a fractional quantile if the quantile is small.
			// ...
			if(quantTemp <= minQuant && probability > 1d/(N.length+1d)){
				if(D) System.out.println("using Poisson approximation for fractile");
				lambda = Math.abs(-Math.log(1 - probability));
				quantTemp = poissQuantile(lambda, quantiles[i]);
			} 
			q[i] = quantTemp;


			//				mfdArray[i].set(j, (int)(q[i][j]));
		}

		// generate a new vector of N, reduced by probability of remaining in the next M column
		//			for (int i = 0; i < N.length; i++) {
		//				// generate a random vector
		//				n = 0;
		//				for (int k = 0; k < N[i]; k++) {
		//					u = Math.random();
		//					if (u < p) n++;
		//				}
		//				N[i] = n;
		//			}
		//		}

		//now do the quantiles below the simulated magnitude   
		//		for (int j = 0; j < indexComplete; j++) { //(If i use smaller than or equal to indexComplete, it redoes it, and gets wrong numbers for M>Mcomplete. If I use smaller than, it gets the wrong numbers for M<Mcomplete...
		if (mag < magComplete) {
			double f = Math.pow(10,  -b*(mag - magComplete));

			for (int i = 0; i < quantiles.length; i++) {
				// just extend the quantiles from the magComplete
				q[i] = q[i] * f;

			}

			//			double pComplete = probability.getY(indexComplete);
			//			probability.set(j, 1d - Math.pow(1d - pComplete, f));
		}


		//		if(D) {
		//			for (int i = 0; i < quantiles.length; i++) {
		//				for (int j = 0; j < numMbins; j++)
		//					System.out.printf("%d ", (int)(q[i][j]+0.5));
		//				System.out.printf("\n");
		//			}
		//		}
//		return q;
		this.q = q;
	}
	
	public EvenlyDiscretizedFunc[]  getQuantiles() {
		return mfdArray;
	}
	
	public double[]  getQuantilesAsDouble() {
		double[] f = new double[mfdArray.length];
		for (int i = 0; i < f.length; i++) {
			f[i] = mfdArray[i].getY(0);
		}
		return f;
	}
	
	public double[] getSingleQuantile() {
		return q;
	}
	
	
	public EvenlyDiscretizedFunc getProbabilities(){
		return probability;
	}
	
	/** Computes the "Poissonian" quantile using a continuous gamma distribution to get non-integer quantiles.
	 *  This is approximate. Ideas welcomed.
	 *  
	 *  @author Nicholas van der Elst
	 **/
	private double poissQuantile(double lambda, double fractile){
		GammaIncInverse gammaIncInverse = new GammaIncInverse();
		gammaIncInverse.setParams(lambda, fractile);
		UnivariateOptimizer fminbnd = new BrentOptimizer(1e-6,1e-9);

		UnivariatePointValuePair optimum = fminbnd.optimize(
				new UnivariateObjectiveFunction(gammaIncInverse), new MaxEval(100), GoalType.MINIMIZE,
				new SearchInterval(0,20));
		return optimum.getPoint();
	}

	private class GammaIncInverse implements UnivariateFunction{
		private double lambda, fractile;

		public double value(double x) {
			return Math.abs(fractile - Gamma.regularizedGammaQ(x, lambda));
			
		}

		public void setParams(double lambda, double fractile){
			this.lambda = lambda;
			this.fractile = fractile;
		}

	}

	
}