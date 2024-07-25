/**
 * 
 */
package org.opensha.oaf.etas;

import java.util.List;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.util.FaultUtils;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.faultSurface.FaultTrace;

import com.google.common.collect.Lists;



/**
 * @author field
 * @author van der Elst
 *
 */
public class ETAS_StatsCalc {
	
	public final static double MILLISEC_PER_YEAR = 1000*60*60*24*365.25;
	public final static long MILLISEC_PER_DAY = 1000*60*60*24;
	
	private static final boolean D = true;

	
	/**
	 * This does not check for negative values
	 * @param mainShock
	 * @param aftershockList
	 * @return
	 */
	public static double[] getDaysSinceMainShockArray(ObsEqkRupture mainShock, ObsEqkRupList aftershockList) {
		double[] relativeEventTimesDays = new double[aftershockList.size()];
		for(int i=0; i<aftershockList.size();i++) {
			long epochDiff = aftershockList.get(i).getOriginTime()-mainShock.getOriginTime();
			relativeEventTimesDays[i] = (double)(epochDiff) / (double)MILLISEC_PER_DAY;
		}
		return relativeEventTimesDays;
	}
	

	
	public static double[] linspace(double min, double max, int npts){
		double[] vec = new double[npts];
		double dx = (max-min)/(npts-1);
		for(int i = 0; i < npts-1; i++){
			vec[i] = min + i*dx;
		}
		vec[npts-1] = max;
		return vec;
	}
	
	public static double[] logspace(double min, double max, int npts){
		double[] vec = new double[npts];
		double log_min = Math.log10(min);
		double log_max = Math.log10(max);
		double dx = (log_max-log_min)/npts;
		for(int i = 0; i < npts-1; i++){
			vec[i] = Math.pow(10, log_min + i*dx);
			
		}
		vec[npts-1] = max;
		
		return vec;
	}
	
    public static Location getCentroid(ObsEqkRupture mainshock, ObsEqkRupList aftershocks) {
		// now works across prime meridian
		List<Location> locs = Lists.newArrayList(mainshock.getHypocenterLocation());
		for (ObsEqkRupture aftershock : aftershocks)
			locs.add(aftershock.getHypocenterLocation());
		List<Double> lats = Lists.newArrayList();
		List<Double> lons = Lists.newArrayList();
		for (Location loc : locs) {
			lats.add(loc.getLatitude());
			lons.add(loc.getLongitude());
		}
		double lat = FaultUtils.getAngleAverage(lats);
		while (lat > 90)
			lat -= 360;
		double lon = FaultUtils.getAngleAverage(lons);
		while (lon > 180)
			lon -= 360;
		if (Math.abs(lon - mainshock.getHypocenterLocation().getLongitude()) > 270)
			lon +=360;
		
		Location centroid = new Location(lat, lon);
		double dist = LocationUtils.horzDistanceFast(mainshock.getHypocenterLocation(), centroid);
		if(D) System.out.println("Centroid: "+(float)lat+", "+(float)lon+" ("+(float)dist+" km from epicenter)");
		return centroid;
	}

    /** returns radius in km for magnitude and stressDrop in MPa 
     * 
     */
    public static double magnitude2radius(double magnitude, double stressDrop){
    	double r = Math.pow( 7.0/16.0, 1.0/3.0 ) * Math.pow(10.0, 0.5*magnitude - 1.0/3.0*Math.log10(stressDrop) - 2.0);
    	
		return r; 
	}
    
    /** returns magnitude for radius in km and stressDrop in MPa
     * 
     */
    public static double radius2magnitude(double radius, double stressDrop){
		//return (Math.log10(Math.pow(radius*1000,3) * stressDrop * 1e6 * 16/7) - 9)/1.5;
		double mag = 2.0 * (Math.log10(radius * Math.pow(16.0/7.0, 1.0/3.0)) + 1.0/3.0*Math.log10(stressDrop) + 2.0) ;
		return mag ;
	}
    

    /** fitMainshockLineSource fits a mainshock line source to the aftershocks in aftershockFitList, consisting of npts points
     * 
     */
    public static ETASEqkRupture fitMainshockLineSource(ObsEqkRupture mainshock, FaultTrace faultTrace, double stressDrop){
    	ETASEqkRupture equivalentMainshock = new ETASEqkRupture(mainshock, stressDrop);
    	equivalentMainshock.setFaultTrace(faultTrace);
    	
    	//reconstruct ellipse
    	double d = magnitude2radius(mainshock.getMag(), stressDrop);
    	double a = LocationUtils.horzDistance(faultTrace.first(), faultTrace.last())/2.0;
    	double c = Math.pow(a/d,2.0);
    	double b = a/c;
    	
//    		 we need to represent this as a set of sources:

            double radiusMS, rMSequivalent;

            if (a < b){
            	rMSequivalent = a; //this is going to be SpatialKernelDistance
            	radiusMS = b;		//this is going to be rupture length
            } else {
            	rMSequivalent = b;
            	radiusMS = a;
            }
            equivalentMainshock.setSpatialKernelDistance(rMSequivalent);	

          if(D) System.out.println("Dimensions of ellipse: " + a + " " + " " + b + " aspect: " + String.format("%3.2f", c) + "; EQ mag: "
    		+ mainshock.getMag() + "; DS:" + stressDrop + "; EQ radius: " + magnitude2radius(mainshock.getMag(), stressDrop) );

        	return equivalentMainshock; 
    	
    	
    	
//    	return fitMainshockLineSource( mainshock,  aftershockFitList,  stressDrop,  q, false);
    }

    /** fitMainshockLineSource fits a mainshock line source to the aftershocks in aftershockFitList, consisting of npts points
     * 
     */
    public static ETASEqkRupture fitMainshockLineSource(ObsEqkRupture mainshock, ObsEqkRupList aftershockFitList, double stressDrop){
    	double q = 0.68; //quantile of the AS zone to try to capture
    	return fitMainshockLineSource( mainshock,  aftershockFitList,  stressDrop,  q, true);
    }
    	
    /** fitMainshockLineSource fits a mainshock line source to the aftershocks in aftershockFitList, consisting of npts points
     * 
     */
    public static ETASEqkRupture fitMainshockLineSource(ObsEqkRupture mainshock, ObsEqkRupList aftershockFitList, double stressDrop, double q, boolean weightByDistance){
    	// This works but probably has unknown failure modes.
    	
    	ETASEqkRupture equivalentMainshock = new ETASEqkRupture(mainshock, stressDrop);
        
    	//mainshock coords (lat0 and lon0 not actaully used...)
    	double mag0, lat0, lon0;
        mag0 = equivalentMainshock.getMag();
        lat0 = equivalentMainshock.getHypocenterLocation().getLatitude();
        lon0 = equivalentMainshock.getHypocenterLocation().getLongitude();
        
        // aftershock coords
        double[] mag = new double[aftershockFitList.size()];
        double[] lat = new double[mag.length];
        double[] lon = new double[mag.length];
        double[] w = new double[mag.length];
        for(int i = 0; i < mag.length; i++){
        	ObsEqkRupture as = aftershockFitList.get(i);
        	mag[i] = as.getMag();
        	lat[i] = as.getHypocenterLocation().getLatitude();
        	lon[i] = as.getHypocenterLocation().getLongitude();
        }

        // compute weighted centroid location
        double[] dx = new double[mag.length];
        double[] dy = new double[mag.length];
        double[] r = new double[mag.length];
        double[] th = new double[mag.length];
        double[] dt = new double[mag.length];
                
        double lonwsum = 0;
        double latwsum = 0;
        double wsum = 0;
        double r0 = equivalentMainshock.getSpatialKernelDistance();
        
    	for(int i = 0; i < mag.length; i++){
    		// compute x, y locs relative to centroid
    		dy[i] = (lat[i] - lat0) * 111.111;
    		
        	dx[i] = (lon[i] - lon0) * Math.cos(Math.toRadians(lat0)) * 111.111;
        	if (lon[i] - lon0 < -180) dx[i] += 360;
        	if (lon[i] - lon0 > 180) dx[i] -= 360;
        	dt[i] = (aftershockFitList.get(i).getOriginTime() - mainshock.getOriginTime())/ETAS_StatsCalc.MILLISEC_PER_DAY;
        	th[i] = Math.atan2(dy[i], dx[i]);
        	r[i] = Math.sqrt(dx[i]*dx[i] + dy[i]*dy[i]);
        	
        	// Assign weights to each aftershock to decide how much it contributes to the source fit
        	if (weightByDistance){
        		// weight by distance. Aftershocks get a weight of one if they are within 2 * the vdE and Shaw radius,
        		// which assigns a 1km radius to a reference M4.6 earthquake with a 3Mpa stress drop. 
        		// Source scaling follows the assumption that M0 = stressdrop*L^3. Aftershocks outside this diameter are 
        		// downweighted following a cosine taper to double the diameter. Weights are zero beyond.
        		//  
        		if (r[i] < 2*r0)
        			w[i] = 1d;
        		else if (r[i] < 4*r0)
        			w[i] = 0.5 + 0.5*Math.cos( (r[i]-2*r0) * Math.PI/r0/2);
        		else
        			w[i] = 0.0;
        	} else {
        		w[i] = 1.0;
        	}

        	//weight by time
        	// time weighting follows omori's law, with a c-parameter of 1 day. The weight falls of as a power law beyond 1 day.
        	// This should be made an accessible option.
        	w[i] *= 1d/(dt[i] + 1d);

//    		w[i] = Math.pow(10, 0.5*mag[i]);	//weight by magntiude of the aftershock?
    		lonwsum += lon[i]*w[i];
    		latwsum += lat[i]*w[i];
    		wsum += w[i];
    	}
        
        double lonc = lonwsum / wsum;
        double latc = latwsum / wsum;
        if(D) System.out.println("Centroid location: " + latc + " " + lonc + " " + r0);

        // recompute distances relative to centroid, and reweight (this should be made object-oriented). See comments above.
        for(int i = 0; i < mag.length; i++){
    		// compute x, y locs relative to centroid
    		dy[i] = (lat[i] - latc) * 111.111;
        	dx[i] = (lon[i] - lonc) * Math.cos(Math.toRadians(latc)) * 111.111;
        	th[i] = Math.atan2(dy[i], dx[i]);
        	r[i] = Math.sqrt(dx[i]*dx[i] + dy[i]*dy[i]);
        	
        	if (weightByDistance){
        		// weight by distance
        		if (r[i] < 2*r0)
        			w[i] = 1d;
        		else if (r[i] < 4*r0)
        			w[i] = 0.5 + 0.5*Math.cos( (r[i]-2*r0) * Math.PI/r0/2);
        		else
        			w[i] = 0.0;
        	} else {
        		w[i] = 1.0;
        	}
        	
//    		w[i] = Math.pow(10, 0.5*mag[i]);	//weight by lengthscale
    		lonwsum += lon[i]*w[i];
    		latwsum += lat[i]*w[i];
    		wsum += w[i];
    	}
        
        // initial guess is taking the average angle (is this averaging right?)
        double omega0, sinsum = 0, cossum = 0;
        for(int i = 0; i< mag.length; i++){
        	 sinsum += Math.sin(dy[i]/dx[i]);
        	 cossum += Math.cos(dy[i]/dx[i]);
        }
        omega0 = 0.5*Math.atan2(sinsum/mag.length, cossum/mag.length);	// initial guess
        
        
        // find the best-fitting line with a grid search
        int searchDepth = 3; //number of times to refine grid
        double deltaOm = Math.PI;	//search range
        double dOm = deltaOm/10;	//search increment
        double[] SSE = new double[21];
        
        double[] phi = new double[th.length];
        double[] p = new double[th.length];
        double[] d = new double[th.length];
    	
        if(D) System.out.println("Seeking the optimal fit line");
        for (int n = 0; n < searchDepth; n++){
        	if(D) System.out.println(n + " out of " + searchDepth + " grid refinements...");
        	double SSEmin = Double.POSITIVE_INFINITY;
        	int iBest = 1;
        	for(int i = 0; i < SSE.length; i++){
            	double om = omega0 - deltaOm + dOm*i;
                
                // compute Sum of Squared distances from test line
                for(int j = 0; j < th.length; j++){
            		phi[j] = th[j] - om;
            	  	p[j] = r[j] * Math.sin(phi[j]);
//            	  	d[j] = r[j] * Math.cos(phi[j]);
            	  	SSE[i] += p[j]*p[j] * w[j];
            	}
                
            	if(SSE[i] < SSEmin){
            		SSEmin = SSE[i];
            		iBest = i;
            	}
        	}
        	omega0 = omega0 - deltaOm + dOm*iBest;
        	deltaOm = dOm;
        	dOm = dOm/10;
        }
         
        if(D) System.out.println("Best fit mainshock strike: " + omega0*180/Math.PI + " north of east");
        
        // now we're going to get the coordinates of the aftershocks in mainshock space and come up with an 
        // ellipse that will lasso some fraction of the early aftershocks 
        
        // compute coordinates relative to best-fit line (mainshock source)
        for(int i = 0; i < th.length; i++){
    		phi[i] = th[i] - omega0;
    	  	p[i] = r[i] * Math.sin(phi[i]);
    	  	d[i] = r[i] * Math.cos(phi[i]);
    	}

        //coordinate transformation matrix
        double[][] coeff = new double[2][2];
        coeff[0][0] = Math.cos(omega0);
        coeff[0][1] = -Math.sin(omega0);
        coeff[1][0] = Math.sin(omega0);
        coeff[1][1] = Math.cos(omega0);

        //find quantiles of x and y relative to mainshock (width and length)
        java.util.Arrays.sort(d);
        double xmin = d[(int) Math.floor( (0.5 - q/2)*d.length )];
        double xmax = d[(int) Math.floor( (0.5 + q/2)*d.length )];
        java.util.Arrays.sort(p);
        double ymin = p[(int) Math.floor( (0.5 - q/2)*p.length )];
        double ymax = p[(int) Math.floor( (0.5 + q/2)*p.length )];
       
        if(D) System.out.println("Quantiles " + xmin + " " + xmax + " " + ymin + " " + ymax);
        
        //compute aspect ratio of aftershocks
        double Dx = xmax - xmin;
        double Dy = ymax - ymin;
        double c = Dx/Dy;
        double a = Math.sqrt(c) * magnitude2radius(mag0, stressDrop);
        double b = a/c;
        
        // We start by treating the source as an ellipse.
        // The ellipse has the same area as the circle for the isotropic source, but has the aspect ratio of the early aftershocks. 
        // the long axis of the ellipse is the rupture length. The short axis is the internal length scale.
        if(D) System.out.println("Dimensions of ellipse: " + a + " " + " " + b + "; EQ mag: "
        		+ mag0 + "; DS:" + stressDrop + "; EQ radius: " + magnitude2radius(mag0, stressDrop) );
        
        // we need to represent this as a set of sources:
        // build set of equivalent sources to represent line source
        double radiusMS, rMSequivalent;
        int nEquivalent;
        
        if (a < b){
        	rMSequivalent = a; //this is going to be SpatialKernelDistance
        	radiusMS = b;		//this is going to be rupture length
        } else {
        	rMSequivalent = b;
        	radiusMS = a;
        }
        
        // make a bunch of sources (10 per degree) to represent the mainshock, using the length scale from the kernel.
        equivalentMainshock.setSpatialKernelDistance(rMSequivalent);
    	nEquivalent = (int) (radiusMS/rMSequivalent*10);
    	
    	if(D) System.out.println("Building rupture trace with " + nEquivalent + " points...");
    	
        // now find the equivalent magnitude for an integer number of sources? or is this done somewhere else? In the 2D rate model for example?
    	if(nEquivalent < 1)
    		nEquivalent = 1;
    
    	// set up equivalent sources
    	double dEquivalent, dxEquivalent, dyEquivalent, latEquivalent, lonEquivalent;
    	FaultTrace rup = new FaultTrace("Mainshock");
    	
    	for(int n = 0; n < nEquivalent; n++){
    		if (nEquivalent == 1)
    			dEquivalent = 0;
    		else
    			dEquivalent = -radiusMS + 2*radiusMS/(nEquivalent - 1) * n;

    		// rotate p and d into dx and dy, convert to lat lon
    		dxEquivalent = dEquivalent * Math.cos(omega0);
    		dyEquivalent = dEquivalent * Math.sin(omega0);

    		latEquivalent = latc + dyEquivalent/111.111;
    		lonEquivalent = lonc + dxEquivalent/111.111 / Math.cos(Math.toRadians(latc));

    		Location loc = new Location(latEquivalent,lonEquivalent);
    		rup.add(loc);
    	}
    	
    	equivalentMainshock.setFaultTrace(rup);
    	return equivalentMainshock; 
    }
   
 
    /**
     * calculateBranchingRatio(full analytical solution with variable alpha, b, truncation magnitudes)
     * @param a
     * @param p
     * @param c
     * @param alpha
     * @param b
     * @param tMax
     * @param magComplete
     * @param maxMag
     * @return
     */
    public static double calculateBranchingRatio(double a, double p, double c, double alpha, double b,
    		double tMax, double magComplete, double maxMag){
   
    double K0;
    double n;
	if (Math.abs(1-p) < 1e-6)
		K0 = Math.pow(10, a) * ( Math.log(tMax + c) - Math.log(c) );
	else
		K0 = Math.pow(10, a)/(1-p) * ( Math.pow(tMax + c, 1-p) - Math.pow(c, 1-p) );

	if (alpha == b)
		n = K0 * b * Math.log(10) * (maxMag - magComplete);
	else
		n = K0 * b/(alpha - b) * (Math.pow(10, (alpha - b) * (maxMag - magComplete)) - 1);
	
	return n;
    }
    		
    public static double[] getAftershockMags(ObsEqkRupList rupList){

    	//		System.out.println(rupList);
    	int Nas = rupList.size();

    	double[] magnitudes = new double[Nas];
    	ObsEqkRupture rup;

    	for(int i=0 ; i<Nas ; i++){
    		rup = rupList.get(i);
    		magnitudes[i] = rup.getMag();
    	}
    	return magnitudes;
    }
    
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Hello, this is ETAS_StatsCalc. Who's calling please?");
	}

}
