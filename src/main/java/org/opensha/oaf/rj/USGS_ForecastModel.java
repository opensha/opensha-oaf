package org.opensha.oaf.rj;

import java.util.Map;


// Interface for models capable of providing USGS forecasts.
// Author: Michael Barall 03/07/2022.
//
// This interface represents a model that can be used to generate
// a USGS forecast, for display on the USGS event page.

public interface USGS_ForecastModel {

	// Return true if the model contains a mainshock magnitude.

	public boolean hasMainShockMag ();

	// Return the magnitude of the mainshock.
	// Throw USGS_ForecastException if the model does not contain a mainshock magnitude.
	
	public double getMainShockMag ();




	// Get fractiles for the probability distribution of forecasted number of aftershocks.
	// Parameters:
	//  fractileArray = Desired fractiles (percentile/100) of the probability distribution.
	//  mag = Minimum magnitude of aftershocks considered.
	//  tMinDays = Start of time range, in days after some origin time.
	//  tMaxDays = End of time range, in days after some origin time.
	// The return value is an array whose length equals fractileArray.length.
	// The i-th element of the return value is the fractileArray[i] fractile of the
	// probability distribution, which is defined to be the minimum number n of
	// aftershocks with magnitude >= mag, occurring between times tMinDays and tMaxDays,
	// such that the probability of n or fewer aftershocks is >= fractileArray[i].
	// Note that although the return type is double[], the return values are integers.
	//
	// Note: The fractiles should account for both aleatory and epistemic uncertaintly.
	//
	// Note: It is acceptable for the function to return a value based on the duration
	// (defined to be tMaxDays - tMinDays), rather than examining tMinDays and tMaxDays
	// separately.  In this case, the start time of the forecasts must be coordinated
	// externally to this object.
	//
	// Note: Some models (e.g., RJ) are able to calculate results on-the-fly for any
	// given parameter values, while others (e.g., ETAS) can supply results only for
	// values that were pre-selected during model creation.  In the latter case,
	// external coordination is needed to ensure that the model can supply all
	// requested results.  This function should throw USGS_ForecastException if it
	// cannot supply the requested results.

	public double[] getCumNumFractileWithAleatory (double[] fractileArray, double mag, double tMinDays, double tMaxDays);
	



	// Get the probability that one or more aftershocks will occur.
	// Parameters:
	//  mag = Minimum magnitude of aftershocks considered.
	//  tMinDays = Start of time range, in days after some origin time.
	//  tMaxDays = End of time range, in days after some origin time.
	// The return value is the probability that one or more aftershocks with
	// magnitude >= mag will occur between times tMinDays and tMaxDays.
	//
	// Note: The function should account for both aleatory and epistemic uncertaintly.
	//
	// Note: It is acceptable for the function to return a value based on the duration
	// (defined to be tMaxDays - tMinDays), rather than examining tMinDays and tMaxDays
	// separately.  In this case, the start time of the forecasts must be coordinated
	// externally to this object.
	//
	// Note: Some models (e.g., RJ) are able to calculate results on-the-fly for any
	// given parameter values, while others (e.g., ETAS) can supply results only for
	// values that were pre-selected during model creation.  In the latter case,
	// external coordination is needed to ensure that the model can supply all
	// requested results.  This function should throw USGS_ForecastException if it
	// cannot supply the requested results.

	public double getProbOneOrMoreEvents (double magMin, double tMinDays, double tMaxDays);




	// Return the name of this model.
	// The return value must be non-null and non-empty.

	public String getModelName ();




	// Return the reference for this model.
	// The return value must be non-null and non-empty.
	// The value "#url" should be returned if there is no reference.

	public default String getModelReference () {
		return "#url";
	}




	// Get the list of model parameters.
	// The function returns a map, where each key is the name of a parameter, and
	// each value is the value of the parameter.
	// The return value can be null if there are no parameters.
	//
	// Note: Parameters are listed on the event page in the order that they are
	// supplied by the iterator of the map.  It is therefore recommended to use a
	// map such as LinkedHashMap to control the iteration order.
	//
	// Note: For consistency, it is recommended that parameter names use camelCase.
	//
	// Note: For floating-point parameter values, it is usually desireable to round
	// values to a small number of significant digits.

	public Map<String, Object> getModelParamMap ();

}
