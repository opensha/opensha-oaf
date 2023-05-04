package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to fit ETAS parameters due to a failure to converge.
// Author: Michael Barall 03/15/2023.

public class OEFitConvergenceException extends OEFitException {

	// Constructors.

	public OEFitConvergenceException () {
		super ();
	}

	public OEFitConvergenceException (String s) {
		super (s);
	}

	public OEFitConvergenceException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEFitConvergenceException (Throwable cause) {
		super (cause);
	}

}
