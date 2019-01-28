package org.opensha.oaf.aafs;

import com.mongodb.MongoException;


/**
 * This object represents the location at which a MongoDB error occurred.
 * Author: Michael Barall.
 *
 * This is an immutable object.
 *
 * Note: com.mongodb.MongoException is the base class for exceptions originating in the MongoDB driver.
 */
public class MongoDBErrorLocus {

	// The host handle, or null if unknown.

	private String host_handle;

	public String get_host_handle () {
		return host_handle;
	}

	// The database handle, or null if unknown.

	private String db_handle;

	public String get_db_handle () {
		return db_handle;
	}

	// The collection name, or null if unknown.

	private String coll_name;

	public String get_coll_name () {
		return coll_name;
	}

	// The MongoDB driver exception, or null if unknown.

	private MongoException mongo_exception;

	public MongoException get_mongo_exception () {
		return mongo_exception;
	}

	// Constructor.

	public MongoDBErrorLocus (String host_handle, String db_handle, String coll_name, MongoException mongo_exception) {
		this.host_handle = host_handle;
		this.db_handle = db_handle;
		this.coll_name = coll_name;
		this.mongo_exception = mongo_exception;
	}

}
