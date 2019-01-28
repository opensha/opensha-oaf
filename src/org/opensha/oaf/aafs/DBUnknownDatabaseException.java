package org.opensha.oaf.aafs;

/**
 * Exception class indicating a request for an unknown database.
 * Author: Michael Barall.
 */
public class DBUnknownDatabaseException extends DBConfigViolationException {

	// Constructors.

	public DBUnknownDatabaseException () {
		super ();
	}

	public DBUnknownDatabaseException (String s) {
		super (s);
	}

	public DBUnknownDatabaseException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBUnknownDatabaseException (Throwable cause) {
		super (cause);
	}

}
