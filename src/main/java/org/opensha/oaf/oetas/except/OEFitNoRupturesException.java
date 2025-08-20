package org.opensha.oaf.oetas.except;

// Checked exception class that indicates a failure to fit ETAS parameters due to there being no accepted ruptures in the history.
// Author: Michael Barall.

public class OEFitNoRupturesException extends OEFitException {

	// Constructors.

	public OEFitNoRupturesException () {
		super ();
	}

	public OEFitNoRupturesException (String s) {
		super (s);
	}

	public OEFitNoRupturesException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEFitNoRupturesException (Throwable cause) {
		super (cause);
	}

}
