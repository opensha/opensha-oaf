package org.opensha.oaf.util;

import java.io.IOException;

/**
 * Checked exception indicating that an I/O operation failed because data was not in the expected format.
 * Author: Michael Barall.
 */
public class IODataFormatException extends IOException {

	// Constructors.

	public IODataFormatException () {
		super ();
	}

	public IODataFormatException (String s) {
		super (s);
	}

	public IODataFormatException (String message, Throwable cause) {
		super (message, cause);
	}

	public IODataFormatException (Throwable cause) {
		super (cause);
	}

}
