package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS simulations due to a timeout.
// Author: Michael Barall 03/15/2023.

public class OESimTimeoutException extends OESimException {

	// Constructors.

	public OESimTimeoutException () {
		super ();
	}

	public OESimTimeoutException (String s) {
		super (s);
	}

	public OESimTimeoutException (String message, Throwable cause) {
		super (message, cause);
	}

	public OESimTimeoutException (Throwable cause) {
		super (cause);
	}

}
