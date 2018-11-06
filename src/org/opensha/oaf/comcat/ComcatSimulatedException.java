package org.opensha.oaf.comcat;

import org.opensha.commons.data.comcat.ComcatException;

/**
 * Exception class for simulated Comcat errors.
 * Author: Michael Barall 09/17/2018.
 *
 * This exception is thrown to simulate a Comcat access error.
 */
public class ComcatSimulatedException extends ComcatException {

	// Constructors.

	public ComcatSimulatedException () {
		super ();
	}

	public ComcatSimulatedException (String s) {
		super (s);
	}

	public ComcatSimulatedException (String message, Throwable cause) {
		super (message, cause);
	}

	public ComcatSimulatedException (Throwable cause) {
		super (cause);
	}

}
