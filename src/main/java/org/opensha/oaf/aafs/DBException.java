package org.opensha.oaf.aafs;

/**
 * Base class for exceptions related to database access.
 * Author: Michael Barall.
 *
 * Note: com.mongodb.MongoException is the base class for exceptions originating in the MongoDB driver.
 */
public class DBException extends RuntimeException {

	// Constructors.

	public DBException () {
		super ();
	}

	public DBException (String s) {
		super (s);
	}

	public DBException (String message, Throwable cause) {
		super (message, cause);
	}

	public DBException (Throwable cause) {
		super (cause);
	}

}
