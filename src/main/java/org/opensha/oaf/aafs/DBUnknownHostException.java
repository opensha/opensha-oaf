package org.opensha.oaf.aafs;

/**
 * Exception class indicating a request for an unknown host.
 * Author: Michael Barall.
 */
public class DBUnknownHostException extends DBConfigViolationException {

	// Constructors.

	public DBUnknownHostException () {
		super ();
	}

	public DBUnknownHostException (String s) {
		super (s);
	}

	public DBUnknownHostException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBUnknownHostException (Throwable cause) {
		super (cause);
	}

}
