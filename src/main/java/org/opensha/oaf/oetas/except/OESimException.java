package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS simulations.
// Author: Michael Barall 03/15/2023.

public class OESimException extends OEException {

	// Constructors.

	public OESimException () {
		super ();
	}

	public OESimException (String s) {
		super (s);
	}

	public OESimException (String message, Throwable cause) {
		super (message, cause);
	}

	public OESimException (Throwable cause) {
		super (cause);
	}

}
