package org.opensha.oaf.oetas.except;

// Checked exception class that is the root of all Operational ETAS exceptions.
// Author: Michael Barall 03/15/2023.
//
// This exception, or one of its subclasses, is thrown to indicate that
// and ETAS forecast could not be produced.

public class OEException extends Exception {

	// Constructors.

	public OEException () {
		super ();
	}

	public OEException (String s) {
		super (s);
	}

	public OEException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEException (Throwable cause) {
		super (cause);
	}

}
