package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to complete ETAS simulations due to an abort in a worker thread.
// Author: Michael Barall 03/15/2023.

public class OESimThreadAbortException extends OESimException {

	// Constructors.

	public OESimThreadAbortException () {
		super ();
	}

	public OESimThreadAbortException (String s) {
		super (s);
	}

	public OESimThreadAbortException (String message, Throwable cause) {
		super (message, cause);
	}

	public OESimThreadAbortException (Throwable cause) {
		super (cause);
	}

}
