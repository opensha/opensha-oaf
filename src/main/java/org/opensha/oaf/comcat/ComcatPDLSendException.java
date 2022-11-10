package org.opensha.oaf.comcat;

import org.opensha.commons.data.comcat.ComcatException;

/**
 * Exception class for Comcat PDL send failures.
 * Author: Michael Barall 11/09/2022.
 *
 * This exception indicates that a PDL send failed, and it is being
 * processed as if it was a Comcat failure.
 */
public class ComcatPDLSendException extends ComcatException {

	// Constructors.

	public ComcatPDLSendException () {
		super ();
	}

	public ComcatPDLSendException (String s) {
		super (s);
	}

	public ComcatPDLSendException (String message, Throwable cause) {
		super (message, cause);
	}

	public ComcatPDLSendException (Throwable cause) {
		super (cause);
	}

}
