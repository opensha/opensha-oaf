package org.opensha.oaf.aafs;

import com.mongodb.MongoException;

/**
 * Exception class for exceptions originating in the database driver.
 * Author: Michael Barall.
 *
 * Note: com.mongodb.MongoException is the base class for exceptions originating in the MongoDB driver.
 * This class (or a subclass) can be used to re-throw MongoException (or a subclass), or itself.
 */
public class DBDriverException extends DBException {

	// Error locus, or null if unknown.

	private MongoDBErrorLocus locus;

	public MongoDBErrorLocus get_locus () {
		return locus;
	}

	// Constructors, from MongoException.

	public DBDriverException (String message, MongoException cause) {
		super (message, cause);
		this.locus = new MongoDBErrorLocus (null, null, null, cause);
	}

	public DBDriverException (MongoException cause) {
		super (cause);
		this.locus = new MongoDBErrorLocus (null, null, null, cause);
	}

	public DBDriverException (MongoDBErrorLocus locus, String message, MongoException cause) {
		super (message, cause);
		this.locus = locus;
	}

	public DBDriverException (MongoDBErrorLocus locus, MongoException cause) {
		super (cause);
		this.locus = locus;
	}

	// Constructors, from DBDriverException.

	public DBDriverException (String message, DBDriverException cause) {
		super (message, cause);
		this.locus = cause.get_locus();
	}

	public DBDriverException (DBDriverException cause) {
		super (cause);
		this.locus = cause.get_locus();
	}

	public DBDriverException (MongoDBErrorLocus locus, String message, DBDriverException cause) {
		super (message, cause);
		this.locus = locus;
	}

	public DBDriverException (MongoDBErrorLocus locus, DBDriverException cause) {
		super (cause);
		this.locus = locus;
	}

}
