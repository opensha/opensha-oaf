package org.opensha.oaf.etas;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opensha.commons.data.function.ArbDiscrEmpiricalDistFunc;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import com.google.common.base.Stopwatch;

/**
 * This class is a stochastic ETAS catalog that represents an extension of the observed catalog,
 * supplied as mainshock and aftershocks objects. Time is relative to the mainshock. If nSims is supplied,
 * a suite of catalogs will be built. Only the last catalog to be built is stored for retrieval.
 * 
 * @param genericETAS_parameters or a,a_sigma,b,p,c,alpha,refMag
 * @param mainshock	
 * @param aftershocks
 * @param startTime
 * @param endTime
 * @param magMag
 * @param maxGeneration
 * @param [nSims]
 * 
 * @author Nicholas van der Elst
 *
 */

public class ETAScatalog {

	private final static boolean D = true; //debug
	
	double[] ams_vec, a_vec, p_vec, c_vec;
	double[][][][] likelihood;
	double alpha;
	double b;
	double refMag;
	double forecastStart;
	double forecastEnd;
	double Mc;
	double minMagLimit;
	double maxMagLimit;
	int nSims;
	int maxGenerations; //simulation depth
//	private float[] maxMags;	
	public int[] numEventsFinal;
//	private int[] numGenerations;
	
	private List<List<float[]>> catalogList;	//list of catalogs
//	private List<List<float[]>> catalogTimesList;	//list of catalog times
	private boolean validate;
	
	public ETAScatalog(double[] ams_vec, double[] a_vec, double[] p_vec, double[] c_vec, double[][][][] likelihood, double alpha, double b, double refMag,
			ObsEqkRupture mainshock, ObsEqkRupList aftershocks,
			double dataStart, double dataEnd, double forecastStart, double forecastEnd, double Mc, double minMagLimit, double maxMagLimit, int maxGenerations, int nSims, boolean validate){
	
		this.ams_vec = ams_vec;
		this.a_vec = a_vec;
		this.p_vec = p_vec;
		this.c_vec = c_vec;
		this.likelihood = likelihood;
		this.alpha = alpha;
		this.b = b;
		this.refMag = refMag;
		this.forecastStart = forecastStart;
		this.forecastEnd = forecastEnd;
		this.Mc = Mc;
		this.minMagLimit = minMagLimit;
		this.maxMagLimit = maxMagLimit;
		this.maxGenerations = maxGenerations;
		this.nSims = nSims;
		this.validate = validate;
		
		if(D) System.out.println("ETAS simulation params: alpha=" + alpha + " b=" + b + " Mref=" + refMag + " Mc=" + Mc + " minSimMag=" + minMagLimit + " Mmax=" + maxMagLimit + " nSims=" + nSims); 
				
		
		List<float[]> newEqList = new ArrayList<float[]>();	//catalog containing list of {time, mag, gen}
//		List<float[]> newEqTimesList = new ArrayList<float[]>(); //catalog containing only {time}
		
		List<List<float[]>> catalogList = new ArrayList<List<float[]>>(); //list of catalogs
//		List<List<float[]>> catalogTimesList = new ArrayList<List<float[]>>(); // just the times
		
//		int[] eqInt = new int[nSims];
//		float[] maxMags = new float[nSims];
		int[] nEvents = new int[nSims];
//		int[] nGens = new int[nSims];
		
		if(D) System.out.println("Calculating " + nSims + " " + (int)(forecastEnd - forecastStart) + "-day ETAS catalogs...");
		
		double[][] paramList;
		if(nSims>0){
			//get the list of parameters to supply to each simulation
			
			paramList = sampleParams(nSims, maxMagLimit);
			
			
			
			Stopwatch watch = Stopwatch.createStarted();
			int warnTime = 3;
			long toc;
			double timeEstimate;
			String initialMessageString = "Calculating " + nSims + " " + (int)(forecastEnd - forecastStart) + "-day ETAS catalogs. ";
			
			for(int i = 0; i < nSims ; i++){
				toc = watch.elapsed(TimeUnit.SECONDS);
				if (toc > warnTime){
					warnTime += 10;
					timeEstimate = (double)toc * (double)(nSims)/(double)i;
					System.out.format(initialMessageString + "Approximately %d seconds remaining...\n", (int) ((timeEstimate - toc)));
					initialMessageString = "...";
				}

				double[] params = paramList[i];
				double ams_sample, a_sample, p_sample, c_sample;
				ams_sample = params[0];
				a_sample = params[1];
				p_sample = params[2];
				c_sample = params[3];
				
				if (D && Math.floorMod(i, nSims/10) == 0) System.out.println("Parameter set " + i + ": " + ams_sample + " " + a_sample + " " + p_sample + " " + c_sample);
//				if (D) System.out.println("Parameter set " + i + ": " + ams_sample + " " + a_sample + " " + p_sample + " " + c_sample);
				
				// Currently sets the first event as mainshock and adjusts magnitude
				// todo step1: change magnitude of LARGEST earthquake
				// todo step2: depending on the total number of events, adjust N-largest magnitudes
				ObsEqkRupture simulationMainshock = (ObsEqkRupture) mainshock.clone();
				simulationMainshock.setMag(mainshock.getMag() + (ams_sample - a_sample));

				newEqList = getNewETAScatalog(simulationMainshock, aftershocks, a_sample, p_sample, c_sample, i);
//				newEqTimesList = getNewETAScatalogTimes(simulationMainshock, aftershocks, a_sample, p_sample, c_sample, i);
				
//				maxMags[i] = get_maxMag(newEqList);
				nEvents[i] = get_nEvents(newEqList);
				
				
				
//				nGens[i] = get_nGenerations(newEqList);
				
//				eqInt = compress(newEqList);
//				catalogTimesList.add(i, newEqTimesList);			
				catalogList.add(i, newEqList);			//this is super memory intensive... need a compressed representation
				// instead of adding the new catalog to the list, just compute a histogram of event times. (Magnitude isn't even required)
				
				
				
			}
			toc = watch.elapsed(TimeUnit.SECONDS);
			if(D) System.out.println("It took " + toc + " seconds to generate stochastic catalogs.");
			watch.stop();
			
//			double dM = 0.1;
//			int numMbins = (int)((maxMagLimit - Mc)/dM + 1);
//			double[] quantiles = new double[]{0.025,0.5,0.975};
//			StackedMND mnd = new StackedMND(nEvents, dM, numMbins, b, quantiles);
			
		}
		
		//this.eqList = getLastETAScatalog();
		this.catalogList = catalogList;
//		this.catalogTimesList = catalogTimesList;
//		this.maxMags = maxMags;
		this.numEventsFinal = nEvents;
//		this.numGenerations = nGens;
	}
	
	
//	public List<float[]> getNewETAScatalogTimes(ObsEqkRupture mainshock, ObsEqkRupList aftershocks, double a_sample, double p_sample, double c_sample, int simNumber){
//		
//		//extract magnitudes and times from supplied Eqk rupture objects to make catalog (combine MS and AS's)
//		List<float[]> newEqList = new ArrayList<float[]>();
////		List<float[]> finalEqList = new ArrayList<float[]>();
//		List<float[]> finalEqTimesList = new ArrayList<float[]>();
//		
//		//double[] event = {0, mainshock.getMag(), 0};	//utility variable : {relative time, magnitude, generation number}
//
//		double t0 = mainshock.getOriginTime(); //in milliseconds
//		
//		//combine lists
//		ObsEqkRupList seedQuakes = new ObsEqkRupList();
//		seedQuakes.add(mainshock);
//		Collections.reverse(aftershocks);
//		seedQuakes.addAll(aftershocks);
//
//		//int counter = 0;
//		//go through seed (observed) earthquake list, add each event to a pared-down eventList and add simulated children
//		float[] event = new float[3];
//		for(ObsEqkRupture rup : seedQuakes){
//			event[0] = (float) ((rup.getOriginTime() - t0)/ETAS_StatsCalc.MILLISEC_PER_DAY);	//elapsed time in days
//			event[1] = (float) rup.getMag();	
//			event[2] = 0;	//generation number
//			
//			//check whether event is prior to forecast start, and larger than Mc
//			if( event[0] <= forecastStart && event[0] >= 0 && event[1] >= Mc){
//				//System.out.println("Seed "+counter++);
//				//newEqList.add(event); 
//
//				//add children
//				newEqList = getChildren(newEqList, event[0], event[1], (int)event[2], a_sample, p_sample, c_sample, simNumber);
//						
//			}else{
//				//System.out.println("Skipping Seed "+counter++);
//			}
//			
//		}
//		
//		// sort catalog
//		Collections.sort(newEqList, new java.util.Comparator<float[]>() {
//		    public int compare(float[] a, float[] b) {
//		        return Double.compare(a[0], b[0]);
//		    }
//		});
//		
//		// remove events under mc
//		for(float[] eq : newEqList){
//			if(eq[1] >= Mc) {
//				finalEqTimesList.add(new float[]{eq[0]});
////				finalEqList.add(eq);
//			}
//		}
//		
//		//this.eqList = newEqList;
////		return finalEqList;
//		return finalEqTimesList;
//	}
	
	public List<float[]> getNewETAScatalog(ObsEqkRupture mainshock, ObsEqkRupList aftershocks, double a_sample, double p_sample, double c_sample, int simNumber){
		
		//extract magnitudes and times from supplied Eqk rupture objects to make catalog (combine MS and AS's)
		List<float[]> newEqList = new ArrayList<float[]>();
		List<float[]> finalEqList = new ArrayList<float[]>();
		
		//double[] event = {0, mainshock.getMag(), 0};	//utility variable : {relative time, magnitude, generation number}

		double t0 = mainshock.getOriginTime(); //in milliseconds
		
		//combine lists
		ObsEqkRupList seedQuakes = new ObsEqkRupList();
		seedQuakes.add(mainshock);
		Collections.reverse(aftershocks);
		seedQuakes.addAll(aftershocks);

		//int counter = 0;
		//go through seed (observed) earthquake list, add each event to a pared-down eventList and add simulated children
		for(ObsEqkRupture rup : seedQuakes){
			float[] event = new float[3];
			event[0] = (float) ((rup.getOriginTime() - t0)/ETAS_StatsCalc.MILLISEC_PER_DAY);	//elapsed time in days
			event[1] = (float) rup.getMag();	
			event[2] = 0;	//generation number
			
			//check whether event is prior to forecast start, and larger than Mc
//			if( event[0] <= forecastStart && event[0] >= 0 && event[1] >= Mc){
			if( event[0] <= forecastStart && event[0] >= 0 && event[1] >= minMagLimit){
				//System.out.println("Seed "+counter++);
				//newEqList.add(event); 

				//add children
				newEqList = getChildren(newEqList, event[0], event[1], (int)event[2], a_sample, p_sample, c_sample, simNumber);
						
			}else{
				//System.out.println("Skipping Seed "+counter++);
			}
			
		}
		
		// sort catalog
		Collections.sort(newEqList, new java.util.Comparator<float[]>() {
		    public int compare(float[] a, float[] b) {
		        return Double.compare(a[0], b[0]);
		    }
		});
		
		// remove events under mc
		for(float[] eq : newEqList){
			if(eq[1] >= Mc);
				finalEqList.add(eq);
		}
		
		//this.eqList = newEqList;
		return finalEqList;
	}
	
	private List<float[]> getChildren(List<float[]> newEqList, float t, float mag, int ngen, 
			double a_sample, double p_sample, double c_sample, int simNumber){//, double forecastStart, double forecastEnd,
			//double a, double b, double p, double c, double alpha, double refMag, double maxMag, int maxGen){

		float newMag;
		float newTime;
		double prod;
		 
		double c;
		if (validate) { //forecasts cataloged earthquakes (rather than felt) -- disabled.
//			double k2 = Math.pow(10, a_sample + alpha*(mag - minMagLimit));
//			c = c_sample*Math.pow(k2, 1d/p_sample);
//			if (c < c_sample)
				c = c_sample;
			
		} else
			c = c_sample;
		
		//calculate productivity of this quake
		if (ngen == 1) {
			prod = calculateProductivity(t, mag, forecastStart, forecastEnd, a_sample, b, p_sample, c, alpha, minMagLimit);
		} else {
			double prodCorrection = Math.log10( (maxMagLimit - Mc)/(maxMagLimit - minMagLimit) );
			prod = calculateProductivity(t, mag, forecastStart, forecastEnd, a_sample + prodCorrection, b, p_sample, c, alpha, minMagLimit);
		}
		long numNew = assignNumberOfOffspring(prod); 
		
//		if(D) System.out.format("Parent Mag: %.2f Time: %5.2f Generation: %d Number of offspring: %d %n", mag, t, (int)ngen, (int)numNew);
		if(numNew > 0 && ngen < maxGenerations){
			//for each new child, assign a magnitude and time
			for(long i=0; i<numNew; i++){
				float[] event = new float[3];		//this must be declared within for block, in order to generate a new address
				
				// assign a magnitude
//				newMag = (float) assignMagnitude(b, Mc, maxMagLimit);
				newMag = (float) assignMagnitude(b, minMagLimit, maxMagLimit);
				// assign a time
				newTime = (float) assignTime(t, forecastStart, forecastEnd, p_sample, c);

				// add new child to the list
				event[0] = newTime;
				event[1] = newMag;
				event[2] = ngen + 1;
				
				newEqList.add(event);	
			
				// recursively get children of new child
				newEqList = getChildren(newEqList, newTime, newMag, ngen + 1, a_sample, p_sample, c_sample, simNumber);//, forecastStart, forecastEnd, a, b, p, c, alpha, refMag, maxMag, maxGen);
				
			}
		} else if(ngen == maxGenerations) {
			if(D) System.out.println("Sim=" + simNumber + " t=" + t + " has reached " + maxGenerations + " generations. Cutting it short.");
//			if(D) System.out.println("n = " + ETAS_StatsCalc.calculateBranchingRatio(a_sample, p_sample, c_sample, alpha, b, forecastEnd, Mc, maxMagLimit)
			if(D) System.out.println("n = " + ETAS_StatsCalc.calculateBranchingRatio(a_sample, p_sample, c, alpha, b, forecastEnd, minMagLimit, maxMagLimit)
					+ " a=" + a_sample + " p=" + p_sample + " c=" + c +" (" + c_sample + ")" + " al=" + alpha + " b=" + b + " T=" + forecastEnd + " Mmin=" + minMagLimit + " Mmax=" + maxMagLimit);
		}

		return newEqList;
		
	}
	
	private double calculateProductivity(float t, float mag, double forecastStart, double forecastEnd,
			double a_sample, double b, double p, double c, double alpha, double refMag){
		
		double unscaledProductivity;
		
		if(t < forecastStart) {
			if (Math.abs(1-p) < 1e-6)
				unscaledProductivity = Math.pow(10,a_sample)*( Math.log(forecastEnd - t + c) - Math.log(forecastStart - t + c) );
			else
				unscaledProductivity = Math.pow(10,a_sample)/(1-p)*( Math.pow(forecastEnd - t + c, 1-p) - Math.pow(forecastStart - t + c, 1-p) );
		} else if (t < forecastEnd) {
			if (Math.abs(1-p) < 1e-6)
				unscaledProductivity = Math.pow(10,a_sample)*( Math.log(forecastEnd - t + c) - Math.log(t + c) );
			else
				unscaledProductivity = Math.pow(10,a_sample)/(1-p)*( Math.pow(forecastEnd - t + c, 1-p) - Math.pow(c, 1-p) );
		} else {
			unscaledProductivity = 0;
		}
		
		double prod = unscaledProductivity * Math.pow(10,(alpha*(mag-refMag)));
		return prod;
	}
	
	
	/*
	 * Returns a sample of a,p,c using the likelihood array provided. This method inverts the cumulative likelihood function, 
	 * so the sum of the likelihood array needs to be normalized to 1, which it should be if using the likelihood calculator 
	 * in this package. 
	 * 
	 * @author Nicholas van der Elst
	 */
	private double[][] sampleParams(int nsamples, double maxMag){
			
		int h = 0, i = 0, j = 0, k = 0;
		int num_ams = ams_vec.length, num_a = a_vec.length, num_p = p_vec.length, num_c = c_vec.length;
		
		double[][] params = new double[nsamples][4];
		
		// generate vector of random numbers
		double[] uRand = new double[nsamples];
		for(int n = 0; n<nsamples; n++){
			uRand[n] = Math.random();
		}
		// sort vector
		Arrays.sort(uRand);
		
		double nbranch;
		double [][][][] likelihoodTrunc = likelihood.clone();
		
		// set up timer/time estimator
		long toc, timeEstimate;
		Stopwatch watch = Stopwatch.createStarted();
		int warnTime = 3;
		String initialMessageString = "Generating parameter sets. ";
		//truncate likelihood based on criticality
		double cumSum = 0;
		
		for(h = 0; h < num_ams; h++ ){
			for(i = 0; i < num_a; i++ ){
				for(j = 0; j < num_p; j++ ){
					for(k = 0; k < num_c; k++ ){
						nbranch = ETAS_StatsCalc.calculateBranchingRatio(a_vec[i], p_vec[j], c_vec[k], alpha, b, forecastEnd, Mc, maxMag);
						if(nbranch < 1)
							cumSum += likelihoodTrunc[h][i][j][k];
						else {
							likelihoodTrunc[h][i][j][k] = 0;
						
						}
						
						// run the timer to see how long this is going to take
						toc = watch.elapsed(TimeUnit.SECONDS);
						if(toc > warnTime){
							warnTime += 10;

							timeEstimate = toc * (num_p*num_c*num_ams*num_a)/((h)*(num_p*num_c*num_a) + (i)*(num_c*num_p) + (j)*(num_c) + k);
							System.out.format(initialMessageString + "Approximately %d seconds remaining...\n", (int) ((timeEstimate - toc)));
							initialMessageString = "...";
						}

					}
				}
			}
		}	// else {
		watch.stop();
		
		
		
		//renormalize the random vector to match the likelihood sum
		for(int n = 0; n < nsamples; n++) uRand[n] /= cumSum;
		
		//get cumulative likelihood array
		int n = 0;
		cumSum = 0;
		for(h = 0; h < ams_vec.length; h++ ){
			for(i = 0; i < a_vec.length; i++ ){
				for(j = 0; j < p_vec.length; j++ ){
					for(k = 0; k < c_vec.length; k++ ){
						cumSum += likelihoodTrunc[h][i][j][k];
						while(n < nsamples && cumSum > uRand[n]){
							//found a hit
							params[n] = new double[]{ams_vec[h],a_vec[i], p_vec[j], c_vec[k]};
							n++;
						}
					}
				}
			}
		}	
		
		//shuffle those parameters for a more accurate duration estimate
		java.util.Collections.shuffle(Arrays.asList(params));
		return params;
	}
	
	
	private long assignNumberOfOffspring(double lambda){
		//return Math.round(lambda); //replace with Poisson random number
		return cern.jet.random.tdouble.Poisson.staticNextInt(lambda);
	}
	
	private double assignMagnitude(double b, double minMag, double Mmax){
//		double u = Math.random();
//		double mag = minMag - Math.log10(1.0 - u*(1.0 - Math.pow(10, -b*(Mmax-minMag))))/b;
//		return mag;
		
		double u = Math.random();
		double mag = minMag - 1/b*Math.log10(u);
		
		if(mag>Mmax)
			return Mmax;
		else
			return mag;
	}
	
	private double assignTime(double t0, double tmin, double tmax, double p, double c){
		
		 double u = Math.random();
		 double a1, a2, a3;
		 double t;
		 
		 if (Math.abs(1-p) < 1e-6) {

			 if(t0 < tmin){
				 a1= Math.log(tmax - t0 + c);
				 a2= Math.log(tmin - t0 + c);
			 } else if(t0 < tmax) {
				 a1= Math.log(tmax - t0 + c);
				 a2= Math.log(c);
			 } else {
				 a1= Double.NaN;
				 a2= Double.NaN;
			 }

			 a3 = u*a1 + (1d-u)*a2;
			 t = Math.exp(a3) - c + t0;
			 
		 } else {
			 if(t0 < tmin){
				 a1= Math.pow(tmax - t0 + c, 1d-p);
				 a2= Math.pow(tmin - t0 + c, 1d-p);
			 } else if(t0 < tmax) {
				 a1= Math.pow(tmax - t0 + c, 1d-p);
				 a2= Math.pow(c, 1d-p);
			 } else {
				 a1= Double.NaN;
				 a2= Double.NaN;
			 }

			 a3 = u*a1 + (1d-u)*a2;
			 t = Math.pow(a3, 1d/(1d-p)) - c + t0;
		 }
		 

		 return t;
	}
	
	public List<float[]> getETAScatalog(int index){
		return catalogList.get(index); 
	}

// //not so useful...
//	public List<float[]> getAllETAScatalogs(){
//		List<float[]> allCatalogList = new ArrayList<float[]>(); //list of catalogs
//		List<float[]> eqCat = new ArrayList<float[]>();
//
//		//cycle through the simulated catalogs
//		for(int i = 0; i < nSims; i++){
//			eqCat = getETAScatalog(i); 	//double[] eqCat = {relativeTime, magnitude, generationNumber}
//			for(int j = 0; j < eqCat.size(); j++) {
//				allCatalogList.add(eqCat.get(j));
//			}
//		}
//		if(D) System.out.println("allCatalogList contains " + allCatalogList.size() + " elements.");
//		if(D) System.out.println("last eqCat contains " + eqCat.size() + " elements.");
//		return allCatalogList;
//	}
//	
	
	
//	public int[] getETASintCatalog(int index){
//		return intCatalogList.get(index); 
//		// return eqList;
//	}
	
//	public int[] get_nEvents(){
//		return this.numEventsFinal;
//	}
	
	public int get_nEvents(List<float[]> eqList){
		return eqList.size();
	}
	
//	public float[] get_maxMag(){
//		return this.maxMags;
//	}
	
//	public float get_maxMag(List<float[]> eqList){ 
//		float maxMag = Float.NEGATIVE_INFINITY;
//		float mag;
//
//		for(float[] ev : eqList){
//			mag = ev[1];
//			if( mag > maxMag )
//				maxMag = mag;
//		}
//		return maxMag; 
//	}
	
//	/* try thjs to reduce memory usage */
//	private int[] compress(List<float[]> eqList) {
//		int[] eqInt = new int[eqList.size()];
//		float[] ev =  new float[3];
//		
//		int maxMinutes = (int)Math.pow(2,22);
//		
//		for (int i = 0; i < eqList.size(); i++) {
//			 ev = eqList.get(i);
//			 int mag = (int)((ev[1] - Mc)*100 + 0.5);
//			 int minutes = Math.round(ev[0]*60*24);
//			 
//			 if (minutes < maxMinutes) 
//			 	eqInt[i] = mag + (minutes << 10);
//			 else
//				eqInt[i] = mag + (maxMinutes << 10);
//			 
//		}
//		return eqInt;
//	}

//	public int[] get_nGenerations(){
//		return this.numGenerations;
//	}
	
//	public int get_nGenerations(List<float[]> eqList){
//		double maxGen = 0;
//		double ngen;
//
//		for(float[] ev : eqList){
//			ngen = ev[2];
//			if( ngen > maxGen )
//				maxGen = ngen;
//		}
//		return (int)maxGen; 
//	}
//	
	public double[][][][] getLikelihood(){
		return likelihood;
	}
	
	
//	public String printCatalog(int index){
//		List<float[]> eqList = getETAScatalog(index);
//		
//		StringBuffer paragraph = new StringBuffer("Time Mag Gen\n");
//		for(float[] eq: eqList){
//			 paragraph.append(String.format("%5.2f %5.2f %d %n", eq[0], eq[1], (int)eq[2]));
//		}
//		return paragraph.toString();
//	}
	
	public String printCatalog(int index){
		List<float[]> eqList = getETAScatalog(index);
		
		StringBuffer paragraph = new StringBuffer("Time Mag Gen\n");
		for(float[] eq: eqList){
//			paragraph.append(String.format("%5.2f%n", eq[0]));
			paragraph.append(String.format("%4.3f\t %3.2f\t %1.0f\n", eq[0], eq[1], eq[2]));
		}
		return paragraph.toString();
	}


	public int[] getEventCounts(double tMinDays, double tMaxDays, double forecastMag) {
		// TODO Auto-generated method stub
		
		int[] numM = new int[nSims];

		List<float[]> eqCat = new ArrayList<float[]>();

		//cycle through the simulated catalogs
		for(int i = 0; i < nSims; i++){
			eqCat = getETAScatalog(i); 	//double[] eqCat = {relativeTime, magnitude, generationNumber}
			numM[i] = 0;

			//count all events in time window and magnitude range in this catalog
			for(float[] eq : eqCat){
				if(eq[0] > tMinDays && eq[0] <= tMaxDays && eq[1] >= forecastMag)
					numM[i] ++;
			}							
		}
		return numM;
	}
	
	private double getMeanAms() { return ams_vec[(int)(ams_vec.length/2)]; }
}
