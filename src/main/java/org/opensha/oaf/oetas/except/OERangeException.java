package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS ranging.
// Author: Michael Barall 03/15/2023.

public class OERangeException extends OEException {

	// Constructors.

	public OERangeException () {
		super ();
	}

	public OERangeException (String s) {
		super (s);
	}

	public OERangeException (String message, Throwable cause) {
		super (message, cause);
	}

	public OERangeException (Throwable cause) {
		super (cause);
	}

}
