package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS ranging due to a timeout.
// Author: Michael Barall 03/15/2023.

public class OERangeTimeoutException extends OERangeException {

	// Constructors.

	public OERangeTimeoutException () {
		super ();
	}

	public OERangeTimeoutException (String s) {
		super (s);
	}

	public OERangeTimeoutException (String message, Throwable cause) {
		super (message, cause);
	}

	public OERangeTimeoutException (Throwable cause) {
		super (cause);
	}

}
