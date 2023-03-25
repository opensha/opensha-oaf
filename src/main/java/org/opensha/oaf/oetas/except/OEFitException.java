package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to fit ETAS parameters.
// Author: Michael Barall 03/15/2023.

public class OEFitException extends OEException {

	// Constructors.

	public OEFitException () {
		super ();
	}

	public OEFitException (String s) {
		super (s);
	}

	public OEFitException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEFitException (Throwable cause) {
		super (cause);
	}

}
