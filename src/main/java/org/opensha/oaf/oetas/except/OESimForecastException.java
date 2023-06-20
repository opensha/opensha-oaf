package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to accumulate ETAS simulations into a forecast.
// Author: Michael Barall 03/15/2023.

public class OESimForecastException extends OESimException {

	// Constructors.

	public OESimForecastException () {
		super ();
	}

	public OESimForecastException (String s) {
		super (s);
	}

	public OESimForecastException (String message, Throwable cause) {
		super (message, cause);
	}

	public OESimForecastException (Throwable cause) {
		super (cause);
	}

}
