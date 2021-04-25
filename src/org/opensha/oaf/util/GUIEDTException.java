package org.opensha.oaf.util;

/**
 * Checked exception class that indicates a function must execute in the event dispatch thread (EDT).
 * Author: Michael Barall 04/05/2021.
 *
 * This exception is not intended to be thrown.
 * If a function must execute in the EDT, then it is declared as throwing GUIEDTException.
 * Code which is invoked in the EDT is wrapped in a try block that catches GUIEDTException.
 * The result is that the compiler catches any case where a function that must execute
 * in the EDT is called by code that may not be running in the EDT.
 */
public class GUIEDTException extends Exception {

	// Constructors.

	public GUIEDTException () {
		super ();
	}

	public GUIEDTException (String s) {
		super (s);
	}

	public GUIEDTException (String message, Throwable cause) {
		super (message, cause);
	}

	public GUIEDTException (Throwable cause) {
		super (cause);
	}

}
