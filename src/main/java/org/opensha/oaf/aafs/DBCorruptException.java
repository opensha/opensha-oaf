package org.opensha.oaf.aafs;

/**
 * Exception class for database contents inconsistent or corrupted.
 * Author: Michael Barall 04/02/2018.
 */
public class DBCorruptException extends DBException {

	// Constructors.

	public DBCorruptException () {
		super ();
	}

	public DBCorruptException (String s) {
		super (s);
	}

	public DBCorruptException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBCorruptException (Throwable cause) {
		super (cause);
	}

}
