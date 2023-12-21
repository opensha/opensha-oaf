package org.opensha.oaf.aafs;

// Exception class for errors encountered in MongoDB SSL parameters.
// Author: Michael Barall 12/15/2023.

public class MongoDBSSLParamsException extends RuntimeException {

	// Constructors.

	public MongoDBSSLParamsException () {
		super ();
	}

	public MongoDBSSLParamsException (String s) {
		super (s);
	}

	public MongoDBSSLParamsException (String message, Throwable cause) {
		super (message, cause);
	}

	public MongoDBSSLParamsException (Throwable cause) {
		super (cause);
	}

}
