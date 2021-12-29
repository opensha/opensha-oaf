package org.opensha.oaf.pdl;

/**
 * Exception class for PDL signing errors.
 * Author: Michael Barall 12/28/2021.
 *
 * This exception is thrown to indicate failure in signing a PDL product.
 */
public class PDLSigningException extends RuntimeException {

	// Constructors.

	public PDLSigningException () {
		super ();
	}

	public PDLSigningException (String s) {
		super (s);
	}

	public PDLSigningException (String message, Throwable cause) {
		super (message, cause);
	}

	public PDLSigningException (Throwable cause) {
		super (cause);
	}

}
