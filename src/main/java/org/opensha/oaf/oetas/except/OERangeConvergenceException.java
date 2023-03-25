package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS ranging due to a failure to converge.
// Author: Michael Barall 03/15/2023.

public class OERangeConvergenceException extends OERangeException {

	// Constructors.

	public OERangeConvergenceException () {
		super ();
	}

	public OERangeConvergenceException (String s) {
		super (s);
	}

	public OERangeConvergenceException (String message, Throwable cause) {
		super (message, cause);
	}

	public OERangeConvergenceException (Throwable cause) {
		super (cause);
	}

}
