package org.opensha.oaf.aafs;

/**
 * Exception class indicating an attempt to use an iterator which has been closed.
 * Author: Michael Barall.
 */
public class DBClosedIteratorException extends DBException {

	// Constructors.

	public DBClosedIteratorException () {
		super ();
	}

	public DBClosedIteratorException (String s) {
		super (s);
	}

	public DBClosedIteratorException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBClosedIteratorException (Throwable cause) {
		super (cause);
	}

}
