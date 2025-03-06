package org.opensha.oaf.util;

/**
 * Exception class for parameter/data marshaling/unmarshaling.
 * Indicates the function is not supported by the marshaling implementation.
 * Author: Michael Barall.
 */
public class MarshalUnsupportedException extends MarshalException {

	// Constructors.

	public MarshalUnsupportedException () {
		super ();
	}

	public MarshalUnsupportedException (String s) {
		super (s);
	}

	public MarshalUnsupportedException (String message, Throwable cause) {
		super (message, cause);
	}

	public MarshalUnsupportedException (Throwable cause) {
		super (cause);
	}

}
