package org.opensha.oaf.comcat;

import org.opensha.commons.data.comcat.ComcatException;

/**
 * Exception class for Comcat query failures.
 * Author: Michael Barall 12/28/2021.
 *
 * This exception indicates that a Comcat query failed.  This indicates an
 * inability to communicate with Comcat, or the receipt of malformed data
 * from Comcat.
 */
public class ComcatQueryException extends ComcatException {

	// Constructors.

	public ComcatQueryException () {
		super ();
	}

	public ComcatQueryException (String s) {
		super (s);
	}

	public ComcatQueryException (String message, Throwable cause) {
		super (message, cause);
	}

	public ComcatQueryException (Throwable cause) {
		super (cause);
	}

}
