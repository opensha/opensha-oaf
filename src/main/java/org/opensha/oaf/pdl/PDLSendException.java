package org.opensha.oaf.pdl;

/**
 * Exception class for PDL send errors.
 * Author: Michael Barall 12/28/2021.
 *
 * This exception is thrown to indicate that a PDL send failed.
 */
public class PDLSendException extends RuntimeException {

	// Constructors.

	public PDLSendException () {
		super ();
	}

	public PDLSendException (String s) {
		super (s);
	}

	public PDLSendException (String message, Throwable cause) {
		super (message, cause);
	}

	public PDLSendException (Throwable cause) {
		super (cause);
	}

}
