package org.opensha.oaf.aafs;

/**
 * Exception class indicating a request for a database which is known but not currently available.
 * Author: Michael Barall.
 */
public class DBUnavailableDatabaseException extends DBException {

	// Constructors.

	public DBUnavailableDatabaseException () {
		super ();
	}

	public DBUnavailableDatabaseException (String s) {
		super (s);
	}

	public DBUnavailableDatabaseException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBUnavailableDatabaseException (Throwable cause) {
		super (cause);
	}

}
