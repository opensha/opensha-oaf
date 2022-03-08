package org.opensha.oaf.rj;


// Exception class for generating USGS forecasts.
// Author: Michael Barall 03/07/2022.
//
// This exception indicates inability to produce the data required for a forecast.

public class USGS_ForecastException extends RuntimeException {

	// Constructors.

	public USGS_ForecastException () {
		super ();
	}

	public USGS_ForecastException (String s) {
		super (s);
	}

	public USGS_ForecastException (String message, Throwable cause) {
		super (message, cause);
	}

	public USGS_ForecastException (Throwable cause) {
		super (cause);
	}

}
