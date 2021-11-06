package org.opensha.oaf.comcat;

import java.io.IOException;

/**
 * Exception class for Comcat access that fails to obtain a content file.
 * Author: Michael Barall 10/31/2021.
 *
 * This exception indicates the failure of an attempt to retrieve a file
 * listed in a Comcat product.  It also indicates that the failure is likely
 * to be permanent, and therefore the caller should not retry the attempt.
 *
 * This does not indicate a problem with accessing the Comcat service.
 *
 * Note that this is a checked exception, a subclass of IOException.
 */
public class ComcatContentFileException extends IOException {

	// Constructors.

	public ComcatContentFileException () {
		super ();
	}

	public ComcatContentFileException (String s) {
		super (s);
	}

	public ComcatContentFileException (String message, Throwable cause) {
		super (message, cause);
	}

	public ComcatContentFileException (Throwable cause) {
		super (cause);
	}

}
