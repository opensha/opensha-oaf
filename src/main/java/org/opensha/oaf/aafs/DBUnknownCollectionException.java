package org.opensha.oaf.aafs;

/**
 * Exception indicating a request for an unknown collection.
 * Author: Michael Barall.
 */
public class DBUnknownCollectionException extends DBConfigViolationException {

	// Constructors.

	public DBUnknownCollectionException () {
		super ();
	}

	public DBUnknownCollectionException (String s) {
		super (s);
	}

	public DBUnknownCollectionException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBUnknownCollectionException (Throwable cause) {
		super (cause);
	}

}
