package org.opensha.oaf.aafs;

import com.mongodb.MongoException;

/**
 * Exception class for exceptions originating in the database driver.
 * Author: Michael Barall.
 *
 * Note: com.mongodb.MongoException is the base class for exceptions originating in the MongoDB driver.
 * This class (or a subclass) can be used to re-throw MongoException (or a subclass).
 */
public class DBDriverException extends DBException {

	// Error locus, or null if unknown.

	private MongoDBErrorLocus locus;

	public MongoDBErrorLocus get_locus () {
		return locus;
	}

	// Constructors.

	public DBDriverException () {
		super ();
		this.locus = null;
	}

	public DBDriverException (String s) {
		super (s);
		this.locus = null;
	}

	public DBDriverException (String message, Throwable cause) {
		super (message, cause);
		this.locus = null;
	}

	public DBDriverException (Throwable cause) {
		super (cause);
		this.locus = null;
	}

	public DBDriverException (MongoDBErrorLocus locus) {
		super ();
		this.locus = locus;
	}

	public DBDriverException (MongoDBErrorLocus locus, String s) {
		super (s);
		this.locus = locus;
	}

	public DBDriverException (MongoDBErrorLocus locus, String message, Throwable cause) {
		super (message, cause);
		this.locus = locus;
	}

	public DBDriverException (MongoDBErrorLocus locus, Throwable cause) {
		super (cause);
		this.locus = locus;
	}

}
