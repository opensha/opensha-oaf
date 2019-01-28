package org.opensha.oaf.util;

/**
 * Exception class thrown when an object detects that it has in invalid internal state.
 * Author: Michael Barall.
 */
public class InvariantViolationException extends RuntimeException {

	// Constructors.

	public InvariantViolationException () {
		super ();
	}

	public InvariantViolationException (String s) {
		super (s);
	}

	public InvariantViolationException (String message, Throwable cause) {
		super (message, cause);
	}

	public InvariantViolationException (Throwable cause) {
		super (cause);
	}

}
