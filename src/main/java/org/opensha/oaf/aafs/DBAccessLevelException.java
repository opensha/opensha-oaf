package org.opensha.oaf.aafs;

/**
 * Exception indicating an access level request exceeding the configured access level.
 * Author: Michael Barall.
 */
public class DBAccessLevelException extends DBConfigViolationException {

	// Constructors.

	public DBAccessLevelException () {
		super ();
	}

	public DBAccessLevelException (String s) {
		super (s);
	}

	public DBAccessLevelException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBAccessLevelException (Throwable cause) {
		super (cause);
	}

}
