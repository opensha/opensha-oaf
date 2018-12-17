package org.opensha.oaf.aafs;

/**
 * Exception class for simulated database errors.
 * Author: Michael Barall 12/15/2018.
 */
public class DBSimulatedException extends DBCorruptException {

	// Constructors.

	public DBSimulatedException () {
		super ();
	}

	public DBSimulatedException (String s) {
		super (s);
	}

	public DBSimulatedException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBSimulatedException (Throwable cause) {
		super (cause);
	}

}
