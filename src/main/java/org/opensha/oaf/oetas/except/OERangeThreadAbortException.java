package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS ranging due to an abort in a worker thread.
// Author: Michael Barall 03/15/2023.

public class OERangeThreadAbortException extends OERangeException {

	// Constructors.

	public OERangeThreadAbortException () {
		super ();
	}

	public OERangeThreadAbortException (String s) {
		super (s);
	}

	public OERangeThreadAbortException (String message, Throwable cause) {
		super (message, cause);
	}

	public OERangeThreadAbortException (Throwable cause) {
		super (cause);
	}

}
