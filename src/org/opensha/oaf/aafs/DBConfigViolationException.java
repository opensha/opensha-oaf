package org.opensha.oaf.aafs;

/**
 * Exception class indicating a request that violates the database configuration.
 * Author: Michael Barall.
 *
 * This is primarily a base class for more specific exceptions.
 */
public class DBConfigViolationException extends DBException {

	// Constructors.

	public DBConfigViolationException () {
		super ();
	}

	public DBConfigViolationException (String s) {
		super (s);
	}

	public DBConfigViolationException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBConfigViolationException (Throwable cause) {
		super (cause);
	}

}
