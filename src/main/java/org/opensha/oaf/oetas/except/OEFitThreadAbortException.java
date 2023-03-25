package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to fit ETAS parameters due to an abort in a worker thread.
// Author: Michael Barall 03/15/2023.

public class OEFitThreadAbortException extends OEFitException {

	// Constructors.

	public OEFitThreadAbortException () {
		super ();
	}

	public OEFitThreadAbortException (String s) {
		super (s);
	}

	public OEFitThreadAbortException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEFitThreadAbortException (Throwable cause) {
		super (cause);
	}

}
