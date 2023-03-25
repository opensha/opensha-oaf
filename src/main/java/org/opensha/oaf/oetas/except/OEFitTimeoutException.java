package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to fit ETAS parameters due to a timeout.
// Author: Michael Barall 03/15/2023.

public class OEFitTimeoutException extends OEFitException {

	// Constructors.

	public OEFitTimeoutException () {
		super ();
	}

	public OEFitTimeoutException (String s) {
		super (s);
	}

	public OEFitTimeoutException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEFitTimeoutException (Throwable cause) {
		super (cause);
	}

}
