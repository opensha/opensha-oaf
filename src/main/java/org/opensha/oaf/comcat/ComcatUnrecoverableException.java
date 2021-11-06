package org.opensha.oaf.comcat;

import org.opensha.commons.data.comcat.ComcatException;

/**
 * Exception class for Comcat access that fails due to an unrecoverable condition.
 * Author: Michael Barall 11/01/2021.
 *
 * This exception indicates a failure that is likely to be permanent,
 * and is not likely to be remedied by retrying the operation.  The
 * appropriate action is to abandon any attempt to perform the operation.
 *
 * This does not indicate a problem with accessing the Comcat service.
 */
public class ComcatUnrecoverableException extends ComcatException {

	// Constructors.

	public ComcatUnrecoverableException () {
		super ();
	}

	public ComcatUnrecoverableException (String s) {
		super (s);
	}

	public ComcatUnrecoverableException (String message, Throwable cause) {
		super (message, cause);
	}

	public ComcatUnrecoverableException (Throwable cause) {
		super (cause);
	}

}
