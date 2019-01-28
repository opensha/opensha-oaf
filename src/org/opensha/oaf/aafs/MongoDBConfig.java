package org.opensha.oaf.aafs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.InvariantViolationException;

import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.WriteConcern;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.ClientSessionOptions;
import com.mongodb.TransactionOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;



/**
 * Configuration file for MongoDB.
 * Author: Michael Barall.
 *
 * This is an immutable object.
 *
 * This contains all the information needed to connect to multiple MongoDB hosts.
 *
 * JSON file format:
 *
 *	"MongoDBConfig" = Integer giving file version number, currently 54001.
 *  "default_db_handle" = String giving the handle of the default database.
 *	"hosts" = [ Array giving a list of MongoDB hosts.
 *		element = { Structure giving host configuration.
 *			"host_handle" = String giving host handle.
 *			"write_concern" = String giving write concern ("" | "majority" | "journaled" | "majority+journaled").
 *			"read_concern" = String giving read concern ("" | "majority" | "snapshot").
 *			"read_preference" = String giving read preference ("" | "primary" | "primary-pref" | "secondary" | "secondary-pref" | "nearest").
 *			"retry_writes" = String giving retry writes ("" | "enable" | "disable").
 *			"connection_mode" = String giving connection mode ("" | "multiple" | "single").
 *			"cluster_type" = String giving cluster type ("" | "replica_set" | "sharded" | "standalone").
 *			"replica_set_name" = String giving replica set name, or "" if not specified.
 *			"connection_string" = String giving MongoDB connection string, or "" if not specified.
 *			"username" = String giving username for logging in to MongoDB.
 *			"auth_db" = String giving authentication database for logging in to MongoDB.
 *			"password" = String giving password for logging in to MongoDB.
 *			"session_level" = Integer giving session support level (0 = disable, 1 = enable, 2 = enable+transactions).
 *			"causal_consistency" = String giving session causal consistency ("" | "enable" | "disable").
 *			"transact_write_concern" = String giving write concern for transactions ("" | "majority" | "journaled" | "majority+journaled").
 *			"transact_read_concern" = String giving read concern for transactions ("" | "majority" | "snapshot").
 *			"transact_read_preference" = String giving read preference for transactions ("" | "primary" | "primary-pref" | "secondary" | "secondary-pref" | "nearest").
 *			"transact_retries" = Integer giving transaction retry limit, or -1 for no limit.
 *			"commit_retries" = Integer giving commit retry limit, or -1 for no limit.
 *			"addresses" = [ Array giving a list host addresses.
 *				element = { Structure giving host address.
 *					"host_name" = String giving host address name or IP address.
 *					"host_port" = Integer giving host port number.
 *				}
 *			]
 *			"databases" = [ Array giving a list of databases for the host.
 *				element = { Structure giving database configuration.
 *					"db_handle" = String giving database handle (the identifier used in code).
 *					"db_name" = String giving database name (as known to MongoDB).
 *					"write_concern" = String giving write concern ("" | "majority" | "journaled" | "majority+journaled").
 *					"read_concern" = String giving read concern ("" | "majority" | "snapshot").
 *					"read_preference" = String giving read preference ("" | "primary" | "primary-pref" | "secondary" | "secondary-pref" | "nearest").
 *					"collections" = [ Array giving a list of collections for the database.
 *						element = { Structure giving collection configuration.
 *							"coll_name" = String giving collection name (as known to MongoDB).
 *							"access_level" = Integer giving access level (0 = read, 1 = write, 2 = update).
 *						}
 *					]
 *				}
 *			]
 *		}
 *	]
 */
public class MongoDBConfig {

	//----- Constants -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 54001;

	private static final String M_VERSION_NAME = "MongoDBConfig";


	// Access levels.

	public static final int ACCESS_LEVEL_READ = 0;
	public static final int ACCESS_LEVEL_WRITE = 1;
	public static final int ACCESS_LEVEL_UPDATE = 2;


	// Session support levels.

	public static final int SESSION_LEVEL_DISABLE = 0;
	public static final int SESSION_LEVEL_ENABLE = 1;
	public static final int SESSION_LEVEL_TRANSACT = 2;




	//----- String parameters -----




	// String values for write concern.

	public static final String WRCONC_DEFAULT = "";					// default write concern
	public static final String WRCONC_MAJORITY = "majority";		// majority write concern
	public static final String WRCONC_JOURNALED = "journaled";		// journaled write concern
	public static final String WRCONC_MAJORITY_JOURNALED = "majority+journaled";		// majority write concern, with journal required

	// Check if a string is a valid write concern.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_write_concern (String wrconc) {
		if (wrconc != null) {
			switch (wrconc) {
			case WRCONC_DEFAULT:
				return false;
			case WRCONC_MAJORITY:
			case WRCONC_JOURNALED:
			case WRCONC_MAJORITY_JOURNALED:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_write_concern: Invalid write concern: " + ((wrconc == null) ? "<null>" : wrconc));
	}

	public static boolean quick_check_write_concern (String wrconc) {
		return !(wrconc.equals (WRCONC_DEFAULT));
	}

	// Apply a write concern to a client.

	public static MongoClientSettings.Builder apply_write_concern (MongoClientSettings.Builder builder, String wrconc) {
		if (wrconc != null) {
			switch (wrconc) {
			case WRCONC_DEFAULT:
				return builder;
			case WRCONC_MAJORITY:
				return builder.writeConcern (WriteConcern.MAJORITY);
			case WRCONC_JOURNALED:
				return builder.writeConcern (WriteConcern.JOURNALED);
			case WRCONC_MAJORITY_JOURNALED:
				return builder.writeConcern (WriteConcern.MAJORITY.withJournal (true));
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_write_concern: Invalid write concern: " + ((wrconc == null) ? "<null>" : wrconc));
	}

	// Apply a write concern to a database.

	public static MongoDatabase apply_write_concern (MongoDatabase database, String wrconc) {
		if (wrconc != null) {
			switch (wrconc) {
			case WRCONC_DEFAULT:
				return database;
			case WRCONC_MAJORITY:
				return database.withWriteConcern (WriteConcern.MAJORITY);
			case WRCONC_JOURNALED:
				return database.withWriteConcern (WriteConcern.JOURNALED);
			case WRCONC_MAJORITY_JOURNALED:
				return database.withWriteConcern (WriteConcern.MAJORITY.withJournal (true));
			}
		}
		throw new IllegalArgumentException ("MongoDBConfig.apply_write_concern: Invalid write concern: " + ((wrconc == null) ? "<null>" : wrconc));
	}

	// Apply a write concern to a transaction.

	public static TransactionOptions.Builder apply_write_concern (TransactionOptions.Builder builder, String wrconc) {
		if (wrconc != null) {
			switch (wrconc) {
			case WRCONC_DEFAULT:
				return builder;
			case WRCONC_MAJORITY:
				return builder.writeConcern (WriteConcern.MAJORITY);
			case WRCONC_JOURNALED:
				return builder.writeConcern (WriteConcern.JOURNALED);
			case WRCONC_MAJORITY_JOURNALED:
				return builder.writeConcern (WriteConcern.MAJORITY.withJournal (true));
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_write_concern: Invalid write concern: " + ((wrconc == null) ? "<null>" : wrconc));
	}




	// String values for read concern.

	public static final String RDCONC_DEFAULT = "";					// default read concern
	public static final String RDCONC_MAJORITY = "majority";		// majority read concern
	public static final String RDCONC_SNAPSHOT = "snapshot";		// snapshot read concern

	// Check if a string is a valid read concern.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_read_concern (String rdconc) {
		if (rdconc != null) {
			switch (rdconc) {
			case RDCONC_DEFAULT:
				return false;
			case RDCONC_MAJORITY:
			case RDCONC_SNAPSHOT:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_read_concern: Invalid read concern: " + ((rdconc == null) ? "<null>" : rdconc));
	}

	public static boolean quick_check_read_concern (String rdconc) {
		return !(rdconc.equals (RDCONC_DEFAULT));
	}

	// Apply a read concern to a client.

	public static MongoClientSettings.Builder apply_read_concern (MongoClientSettings.Builder builder, String rdconc) {
		if (rdconc != null) {
			switch (rdconc) {
			case RDCONC_DEFAULT:
				return builder;
			case RDCONC_MAJORITY:
				return builder.readConcern (ReadConcern.MAJORITY);
			case RDCONC_SNAPSHOT:
				return builder.readConcern (ReadConcern.SNAPSHOT);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_read_concern: Invalid read concern: " + ((rdconc == null) ? "<null>" : rdconc));
	}

	// Apply a read concern to a database.

	public static MongoDatabase apply_read_concern (MongoDatabase database, String rdconc) {
		if (rdconc != null) {
			switch (rdconc) {
			case RDCONC_DEFAULT:
				return database;
			case RDCONC_MAJORITY:
				return database.withReadConcern (ReadConcern.MAJORITY);
			case RDCONC_SNAPSHOT:
				return database.withReadConcern (ReadConcern.SNAPSHOT);
			}
		}
		throw new IllegalArgumentException ("MongoDBConfig.apply_read_concern: Invalid read concern: " + ((rdconc == null) ? "<null>" : rdconc));
	}

	// Apply a read concern to a transaction.

	public static TransactionOptions.Builder apply_read_concern (TransactionOptions.Builder builder, String rdconc) {
		if (rdconc != null) {
			switch (rdconc) {
			case RDCONC_DEFAULT:
				return builder;
			case RDCONC_MAJORITY:
				return builder.readConcern (ReadConcern.MAJORITY);
			case RDCONC_SNAPSHOT:
				return builder.readConcern (ReadConcern.SNAPSHOT);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_read_concern: Invalid read concern: " + ((rdconc == null) ? "<null>" : rdconc));
	}




	// String values for read preference.

	public static final String RDPREF_DEFAULT = "";							// default read preference
	public static final String RDPREF_PRIMARY = "primary";					// primary read preference
	public static final String RDPREF_PRIMARY_PREF = "primary-pref";		// primary-preferred read preference
	public static final String RDPREF_SECONDARY = "secondary";				// secondary read preference
	public static final String RDPREF_SECONDARY_PREF = "secondary-pref";	// secondary-preferred read preference
	public static final String RDPREF_NEAREST = "nearest";					// nearest read preference

	// Check if a string is a valid read preference.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_read_preference (String rdpref) {
		if (rdpref != null) {
			switch (rdpref) {
			case RDPREF_DEFAULT:
				return false;
			case RDPREF_PRIMARY:
			case RDPREF_PRIMARY_PREF:
			case RDPREF_SECONDARY:
			case RDPREF_SECONDARY_PREF:
			case RDPREF_NEAREST:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_read_preference: Invalid read preference: " + ((rdpref == null) ? "<null>" : rdpref));
	}

	public static boolean quick_check_read_preference (String rdpref) {
		return !(rdpref.equals (RDPREF_DEFAULT));
	}

	// Apply a read preference to a client.

	public static MongoClientSettings.Builder apply_read_preference (MongoClientSettings.Builder builder, String rdpref) {
		if (rdpref != null) {
			switch (rdpref) {
			case RDPREF_DEFAULT:
				return builder;
			case RDPREF_PRIMARY:
				return builder.readPreference (ReadPreference.primary());
			case RDPREF_PRIMARY_PREF:
				return builder.readPreference (ReadPreference.primaryPreferred());
			case RDPREF_SECONDARY:
				return builder.readPreference (ReadPreference.secondary());
			case RDPREF_SECONDARY_PREF:
				return builder.readPreference (ReadPreference.secondaryPreferred());
			case RDPREF_NEAREST:
				return builder.readPreference (ReadPreference.nearest());
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_read_preference: Invalid read preference: " + ((rdpref == null) ? "<null>" : rdpref));
	}

	// Apply a read preference to a database.

	public static MongoDatabase apply_read_preference (MongoDatabase database, String rdpref) {
		if (rdpref != null) {
			switch (rdpref) {
			case RDPREF_DEFAULT:
				return database;
			case RDPREF_PRIMARY:
				return database.withReadPreference (ReadPreference.primary());
			case RDPREF_PRIMARY_PREF:
				return database.withReadPreference (ReadPreference.primaryPreferred());
			case RDPREF_SECONDARY:
				return database.withReadPreference (ReadPreference.secondary());
			case RDPREF_SECONDARY_PREF:
				return database.withReadPreference (ReadPreference.secondaryPreferred());
			case RDPREF_NEAREST:
				return database.withReadPreference (ReadPreference.nearest());
			}
		}
		throw new IllegalArgumentException ("MongoDBConfig.apply_read_preference: Invalid read preference: " + ((rdpref == null) ? "<null>" : rdpref));
	}

	// Apply a read preference to a transaction.

	public static TransactionOptions.Builder apply_read_preference (TransactionOptions.Builder builder, String rdpref) {
		if (rdpref != null) {
			switch (rdpref) {
			case RDPREF_DEFAULT:
				return builder;
			case RDPREF_PRIMARY:
				return builder.readPreference (ReadPreference.primary());
			case RDPREF_PRIMARY_PREF:
				return builder.readPreference (ReadPreference.primaryPreferred());
			case RDPREF_SECONDARY:
				return builder.readPreference (ReadPreference.secondary());
			case RDPREF_SECONDARY_PREF:
				return builder.readPreference (ReadPreference.secondaryPreferred());
			case RDPREF_NEAREST:
				return builder.readPreference (ReadPreference.nearest());
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_read_preference: Invalid read preference: " + ((rdpref == null) ? "<null>" : rdpref));
	}




	// String values for retry writes.

	public static final String RETRYWR_DEFAULT = "";			// default retry writes
	public static final String RETRYWR_ENABLE = "enable";		// enable retry writes
	public static final String RETRYWR_DISABLE = "disable";		// disable retry writes

	// Check if a string is a valid retry writes.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_retry_writes (String retwr) {
		if (retwr != null) {
			switch (retwr) {
			case RETRYWR_DEFAULT:
				return false;
			case RETRYWR_ENABLE:
			case RETRYWR_DISABLE:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_causal_consistency: Invalid retry writes: " + ((retwr == null) ? "<null>" : retwr));
	}

	public static boolean quick_check_retry_writes (String retwr) {
		return !(retwr.equals (RETRYWR_DEFAULT));
	}

	// Apply a retry writes to a client.

	public static MongoClientSettings.Builder apply_retry_writes (MongoClientSettings.Builder builder, String retwr) {
		if (retwr != null) {
			switch (retwr) {
			case RETRYWR_DEFAULT:
				return builder;
			case RETRYWR_ENABLE:
				return builder.retryWrites (true);
			case RETRYWR_DISABLE:
				return builder.retryWrites (false);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_causal_consistency: Invalid retry writes: " + ((retwr == null) ? "<null>" : retwr));
	}




	// String values for cluster connection mode.

	public static final String CONMODE_DEFAULT = "";				// default connection mode
	public static final String CONMODE_MULTIPLE = "multiple";		// multiple connection mode
	public static final String CONMODE_SINGLE = "single";			// single connection mode

	// Check if a string is a valid connection mode.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_connection_mode (String conmode) {
		if (conmode != null) {
			switch (conmode) {
			case CONMODE_DEFAULT:
				return false;
			case CONMODE_MULTIPLE:
			case CONMODE_SINGLE:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_connection_mode: Invalid connection mode: " + ((conmode == null) ? "<null>" : conmode));
	}

	public static boolean quick_check_connection_mode (String conmode) {
		return !(conmode.equals (CONMODE_DEFAULT));
	}

	// Apply a connection mode.

	public static ClusterSettings.Builder apply_connection_mode (ClusterSettings.Builder builder, String conmode) {
		if (conmode != null) {
			switch (conmode) {
			case CONMODE_DEFAULT:
				return builder;
			case CONMODE_MULTIPLE:
				return builder.mode (ClusterConnectionMode.MULTIPLE);
			case CONMODE_SINGLE:
				return builder.mode (ClusterConnectionMode.SINGLE);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_connection_mode: Invalid connection mode: " + ((conmode == null) ? "<null>" : conmode));
	}




	// String values for cluster type.

	public static final String CTYPE_DEFAULT = "";					// default cluster type
	public static final String CTYPE_REPLICA_SET = "replica_set";	// replica set cluster
	public static final String CTYPE_SHARDED = "sharded";			// sharded cluster
	public static final String CTYPE_STANDALONE = "standalone";		// standalone server

	// Check if a string is a valid cluster type.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_cluster_type (String ctype) {
		if (ctype != null) {
			switch (ctype) {
			case CTYPE_DEFAULT:
				return false;
			case CTYPE_REPLICA_SET:
			case CTYPE_SHARDED:
			case CTYPE_STANDALONE:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_cluster_type: Invalid cluster type: " + ((ctype == null) ? "<null>" : ctype));
	}

	public static boolean quick_check_cluster_type (String ctype) {
		return !(ctype.equals (CTYPE_DEFAULT));
	}

	// Apply a cluster type.

	public static ClusterSettings.Builder apply_cluster_type (ClusterSettings.Builder builder, String ctype) {
		if (ctype != null) {
			switch (ctype) {
			case CTYPE_DEFAULT:
				return builder;
			case CTYPE_REPLICA_SET:
				return builder.requiredClusterType (ClusterType.REPLICA_SET);
			case CTYPE_SHARDED:
				return builder.requiredClusterType (ClusterType.SHARDED);
			case CTYPE_STANDALONE:
				return builder.requiredClusterType (ClusterType.STANDALONE);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_cluster_type: Invalid cluster type: " + ((ctype == null) ? "<null>" : ctype));
	}




	// Check if a string is a valid replica set name.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_replica_set_name (String rsname) {
		if (rsname != null) {
			if (rsname.isEmpty()) {
				return false;
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_replica_set_name: Invalid replica set name: " + ((rsname == null) ? "<null>" : rsname));
	}

	public static boolean quick_check_replica_set_name (String rsname) {
		return !(rsname.isEmpty());
	}

	// Apply a replica set name.

	public static ClusterSettings.Builder apply_replica_set_name (ClusterSettings.Builder builder, String rsname) {
		if (rsname != null) {
			if (rsname.isEmpty()) {
				return builder;
			}
			return builder.requiredReplicaSetName (rsname);
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_replica_set_name: Invalid replica set name: " + ((rsname == null) ? "<null>" : rsname));
	}




	// Check if a string is a valid connection string.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_connection_string (String constr) {
		if (constr != null) {
			if (constr.isEmpty()) {
				return false;
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_connection_string: Invalid connection string: " + ((constr == null) ? "<null>" : constr));
	}

	public static boolean quick_check_connection_string (String constr) {
		return !(constr.isEmpty());
	}

	// Apply a connection string.

	public static ClusterSettings.Builder apply_connection_string (ClusterSettings.Builder builder, String constr) {
		if (constr != null) {
			if (constr.isEmpty()) {
				return builder;
			}
			return builder.applyConnectionString (new ConnectionString (constr));
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_connection_string: Invalid connection string: " + ((constr == null) ? "<null>" : constr));
	}

	// Apply a connection string to a client.

	public static MongoClientSettings.Builder apply_connection_string (MongoClientSettings.Builder builder, String constr) {
		if (constr != null) {
			if (constr.isEmpty()) {
				return builder;
			}
			return builder.applyConnectionString (new ConnectionString (constr));
		}
		throw new IllegalArgumentException ("MongoDBConfig.apply_connection_string: Invalid connection string: " + ((constr == null) ? "<null>" : constr));
	}




	// Check if a username, authentication database, and password are valid.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the arguments are valid.

	public static boolean check_credential (String username, String auth_db, String password) {
		if (username != null && auth_db != null && password != null) {
			if (username.isEmpty() && auth_db.isEmpty() && password.isEmpty()) {
				return false;
			}
			if (!( username.isEmpty() || auth_db.isEmpty() || password.isEmpty() )) {
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_credential: Invalid credentials");
	}

	public static boolean quick_check_credential (String username, String auth_db, String password) {
		return !(username.isEmpty() && auth_db.isEmpty() && password.isEmpty());
	}

	// Apply a username, authentication database, and password.

	public static MongoClientSettings.Builder apply_credential (MongoClientSettings.Builder builder, String username, String auth_db, String password) {
		if (username != null && auth_db != null && password != null) {
			if (username.isEmpty() && auth_db.isEmpty() && password.isEmpty()) {
				return builder;
			}
			if (!( username.isEmpty() || auth_db.isEmpty() || password.isEmpty() )) {
				return builder.credential (MongoCredential.createCredential (username, auth_db, password.toCharArray()));
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_credential: Invalid credentials");
	}




	// String values for session causal consistency.

	public static final String CAUSAL_DEFAULT = "";				// default causal consistency
	public static final String CAUSAL_ENABLE = "enable";		// enable causal consistency
	public static final String CAUSAL_DISABLE = "disable";		// disable causal consistency

	// Check if a string is a valid causal consistency.
	// Returns true if valid and non-default, false if valid and default, exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_causal_consistency (String causcon) {
		if (causcon != null) {
			switch (causcon) {
			case CAUSAL_DEFAULT:
				return false;
			case CAUSAL_ENABLE:
			case CAUSAL_DISABLE:
				return true;
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.check_causal_consistency: Invalid causal consistency: " + ((causcon == null) ? "<null>" : causcon));
	}

	public static boolean quick_check_causal_consistency (String causcon) {
		return !(causcon.equals (CAUSAL_DEFAULT));
	}

	// Apply a causal consistency to a session.

	public static ClientSessionOptions.Builder apply_causal_consistency (ClientSessionOptions.Builder builder, String causcon) {
		if (causcon != null) {
			switch (causcon) {
			case CAUSAL_DEFAULT:
				return builder;
			case CAUSAL_ENABLE:
				return builder.causallyConsistent (true);
			case CAUSAL_DISABLE:
				return builder.causallyConsistent (false);
			}
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_causal_consistency: Invalid causal consistency: " + ((causcon == null) ? "<null>" : causcon));
	}




	//----- Host Addresses -----




	// HostAddress - Holds the address of a host machine.
	// This is an immutable object.

	public static class HostAddress {
	
		// Host name or IP address.

		private String host_name;

		public String get_host_name () {
			return host_name;
		}

		// Host port number.

		private int host_port;

		public int get_host_port () {
			return host_port;
		}

		// Constructor.

		public HostAddress (String host_name, int host_port) {
			this.host_name = host_name;
			this.host_port = host_port;
		}

		// Check that values are valid, throw an exception if not.

		public void check_invariant () {

			if (!( host_name != null && host_name.trim().length() > 0 )) {
				throw new InvariantViolationException ("MongoDBConfig.HostAddress: Invalid host_name: " + ((host_name == null) ? "<null>" : host_name));
			}

			if (!( host_port >= 1024 && host_port <= 65535 )) {
				throw new InvariantViolationException ("MongoDBConfig.HostAddress: Invalid host_port: " + host_port);
			}
			
			return;
		}

		// Make the MongoDB ServerAddress object for this host address.

		public ServerAddress make_server_address () {
			return new ServerAddress (host_name, host_port);
		}

		// Display our contents.

		public String toString (String prefix) {
			StringBuilder result = new StringBuilder();

			result.append (prefix + "host_name = " + ((host_name == null) ? "<null>" : host_name) + "\n");
			result.append (prefix + "host_port = " + host_port + "\n");

			return result.toString();
		}

		@Override
		public String toString() {
			return toString ("");
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalString ("host_name", host_name);
			writer.marshalInt    ("host_port", host_port);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public HostAddress (MarshalReader reader, String name, int ver) {
			reader.unmarshalMapBegin (name);
			host_name = reader.unmarshalString ("host_name");
			host_port = reader.unmarshalInt    ("host_port");
			reader.unmarshalMapEnd ();
		}
	}

	// Check if a host address list is valid.
	// Returns true if valid and non-default (non-empty list), false if valid and default (empty list), exception if invalid.
	// The quick version can be used if it is known that the argument is valid.

	public static boolean check_host_address_list (List<HostAddress> host_address_list) {
		if (host_address_list != null) {
			if (host_address_list.isEmpty()) {
				return false;
			}
			for (HostAddress host_address : host_address_list) {
				host_address.check_invariant();
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_host_address_list: Invalid host address list: <null>");
	}

	public static boolean quick_check_host_address_list (List<HostAddress> host_address_list) {
		return !(host_address_list.isEmpty());
	}

	// Apply a host address list.

	public static ClusterSettings.Builder apply_host_address_list (ClusterSettings.Builder builder, List<HostAddress> host_address_list) {
		if (host_address_list != null) {
			if (host_address_list.isEmpty()) {
				return builder;
			}
			List<ServerAddress> server_address_list = new ArrayList<ServerAddress>();
			for (HostAddress host_address : host_address_list) {
				server_address_list.add (host_address.make_server_address());
			}
			return builder.hosts (server_address_list);
		}
		throw new InvariantViolationException ("MongoDBConfig.apply_host_address_list: Invalid host address list: <null>");
	}

	// Marshal a host address list.

	public static void marshal_host_address_list (MarshalWriter writer, String name, int ver, List<HostAddress> host_address_list) {
		int n = host_address_list.size();
		writer.marshalArrayBegin (name, n);
		for (HostAddress host_address : host_address_list) {
			host_address.marshal (writer, null, ver);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a host address list.

	public static ArrayList<HostAddress> unmarshal_host_address_list (MarshalReader reader, String name, int ver) {
		ArrayList<HostAddress> host_address_list = new ArrayList<HostAddress>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			host_address_list.add (new HostAddress (reader, null, ver));
		}
		reader.unmarshalArrayEnd ();
		return host_address_list;
	}




	//----- Collections -----




	// CollectionConfig - Holds configuration for a collection.
	// This is an immutable object.

	public static class CollectionConfig {
	
		// Collection name.

		private String coll_name;

		public String get_coll_name () {
			return coll_name;
		}

		// Access level.

		private int access_level;

		public int get_access_level () {
			return access_level;
		}

		// Constructor.

		public CollectionConfig (String coll_name, int access_level) {
			this.coll_name    = coll_name;
			this.access_level = access_level;
		}

		// Check that values are valid, throw an exception if not.

		public void check_invariant () {

			if (!( coll_name != null && coll_name.trim().length() > 0 )) {
				throw new InvariantViolationException ("MongoDBConfig.CollectionConfig: Invalid coll_name: " + ((coll_name == null) ? "<null>" : coll_name));
			}

			if (!( access_level == ACCESS_LEVEL_READ || access_level == ACCESS_LEVEL_WRITE || access_level == ACCESS_LEVEL_UPDATE )) {
				throw new InvariantViolationException ("MongoDBConfig.CollectionConfig: Invalid access_level: " + access_level);
			}
			
			return;
		}

		// Make the MongoDB MongoCollection<Document> object for this collection.

		public MongoCollection<Document> make_collection (MongoDatabase database) {
			return database.getCollection (coll_name);
		}

		// Return true if the requested access level is permitted for this collection.

		public boolean check_access (int req_access) {
			switch (req_access) {
				case ACCESS_LEVEL_READ:
					return (access_level == ACCESS_LEVEL_READ || access_level == ACCESS_LEVEL_UPDATE);
				case ACCESS_LEVEL_WRITE:
					return (access_level == ACCESS_LEVEL_WRITE || access_level == ACCESS_LEVEL_UPDATE);
				case ACCESS_LEVEL_UPDATE:
					return (access_level == ACCESS_LEVEL_UPDATE);
			}
			throw new IllegalArgumentException ("MongoDBConfig.CollectionConfig.check_access: Invalid requested access level: req_access = " + req_access);
		}

		public boolean check_access_read () {
			return check_access (ACCESS_LEVEL_READ);
		}

		public boolean check_access_write () {
			return check_access (ACCESS_LEVEL_WRITE);
		}

		public boolean check_access_update () {
			return check_access (ACCESS_LEVEL_UPDATE);
		}

		// Check if the requested access level is permitted for this collection, throw exception if not.

		public void require_access (int req_access) {
			if (!( check_access (req_access) )) {
				throw new DBAccessLevelException ("MongoDBConfig.CollectionConfig.require_access: Access level violation: req_access = " + req_access + ", access_level = " + access_level + ", coll_name = " + coll_name);
			}
			return;
		}

		public void require_access_read () {
			require_access (ACCESS_LEVEL_READ);
			return;
		}

		public void require_access_write () {
			require_access (ACCESS_LEVEL_WRITE);
			return;
		}

		public void require_access_update () {
			require_access (ACCESS_LEVEL_UPDATE);
			return;
		}

		// Display our contents.

		public String toString (String prefix) {
			StringBuilder result = new StringBuilder();

			result.append (prefix + "coll_name = " + ((coll_name == null) ? "<null>" : coll_name) + "\n");
			result.append (prefix + "access_level = " + access_level + "\n");

			return result.toString();
		}

		@Override
		public String toString() {
			return toString ("");
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalString ("coll_name"   , coll_name   );
			writer.marshalInt    ("access_level", access_level);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public CollectionConfig (MarshalReader reader, String name, int ver) {
			reader.unmarshalMapBegin (name);
			coll_name    = reader.unmarshalString ("coll_name"   );
			access_level = reader.unmarshalInt    ("access_level");
			reader.unmarshalMapEnd ();
		}
	}

	// Check if a collection configuration list is valid.
	// Returns true if valid and non-empty, false if valid and empty, exception if invalid.

	public static boolean check_collection_config_list (List<CollectionConfig> collection_config_list) {
		if (collection_config_list != null) {
			HashSet<String> name_set = new HashSet<String>();
			if (collection_config_list.isEmpty()) {
				return false;
			}
			for (CollectionConfig collection_config : collection_config_list) {
				collection_config.check_invariant();
				if (!( name_set.add (collection_config.get_coll_name()) )) {
					throw new InvariantViolationException ("MongoDBConfig.check_collection_config_list: Duplicate collection name: " + collection_config.get_coll_name());
				}
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_collection_config_list: Invalid collection configuration list: <null>");
	}

	// Marshal a collection configuration list.

	public static void marshal_collection_config_list (MarshalWriter writer, String name, int ver, List<CollectionConfig> collection_config_list) {
		int n = collection_config_list.size();
		writer.marshalArrayBegin (name, n);
		for (CollectionConfig collection_config : collection_config_list) {
			collection_config.marshal (writer, null, ver);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a collection configuration list.

	public static ArrayList<CollectionConfig> unmarshal_collection_config_list (MarshalReader reader, String name, int ver) {
		ArrayList<CollectionConfig> collection_config_list = new ArrayList<CollectionConfig>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			collection_config_list.add (new CollectionConfig (reader, null, ver));
		}
		reader.unmarshalArrayEnd ();
		return collection_config_list;
	}




	//----- Databases -----




	// DatabaseConfig - Holds configuration for a database.
	// This is an immutable object.

	public static class DatabaseConfig {
	
		// Database handle (name used in program code, must be unique across all databases).

		private String db_handle;

		public String get_db_handle () {
			return db_handle;
		}
	
		// Database name (the name known to MongoDB).

		private String db_name;

		public String get_db_name () {
			return db_name;
		}
	
		// Write concern.

		private String write_concern;

		public String get_write_concern () {
			return write_concern;
		}
	
		// Read concern.

		private String read_concern;

		public String get_read_concern () {
			return read_concern;
		}
	
		// Read preference.

		private String read_preference;

		public String get_read_preference () {
			return read_preference;
		}

		// Collection configuration list.

		private List<CollectionConfig> collections;

		public List<CollectionConfig> get_collections () {
			return Collections.unmodifiableList (collections);
		}

		// Constructor.

		public DatabaseConfig (String db_handle, String db_name, String write_concern, String read_concern, String read_preference, List<CollectionConfig> collections) {
			this.db_handle       = db_handle;
			this.db_name         = db_name;
			this.write_concern   = write_concern;
			this.read_concern    = read_concern;
			this.read_preference = read_preference;
			this.collections     = new ArrayList<CollectionConfig> (collections);
		}

		// Check that values are valid, throw an exception if not.

		public void check_invariant () {

			if (!( db_handle != null && db_handle.trim().length() > 0 )) {
				throw new InvariantViolationException ("MongoDBConfig.DatabaseConfig: Invalid db_handle: " + ((db_handle == null) ? "<null>" : db_handle));
			}

			if (!( db_name != null && db_name.trim().length() > 0 )) {
				throw new InvariantViolationException ("MongoDBConfig.DatabaseConfig: Invalid db_name: " + ((db_name == null) ? "<null>" : db_name));
			}

			check_write_concern (write_concern);

			check_read_concern (read_concern);

			check_read_preference (read_preference);

			if (!( check_collection_config_list (collections) )) {
				throw new InvariantViolationException ("MongoDBConfig.DatabaseConfig: Empty collections list");
			}
			
			return;
		}

		// Make the MongoDB MongoDatabase object for this host address.

		public MongoDatabase make_database (MongoClient client) {
			MongoDatabase database = client.getDatabase (db_name);
			database = apply_write_concern (database, write_concern);
			database = apply_read_concern (database, read_concern);
			database = apply_read_preference (database, read_preference);
			return database;
		}

		// Display our contents.

		public String toString (String prefix) {
			StringBuilder result = new StringBuilder();

			result.append (prefix + "db_handle = " + ((db_handle == null) ? "<null>" : db_handle) + "\n");
			result.append (prefix + "db_name = " + ((db_name == null) ? "<null>" : db_name) + "\n");
			result.append (prefix + "write_concern = " + ((write_concern == null) ? "<null>" : write_concern) + "\n");
			result.append (prefix + "read_concern = " + ((read_concern == null) ? "<null>" : read_concern) + "\n");
			result.append (prefix + "read_preference = " + ((read_preference == null) ? "<null>" : read_preference) + "\n");

			for (int k = 0; k < collections.size(); ++k) {
				result.append (prefix + "collections[" + k + "]:\n");
				result.append (collections.get(k).toString (prefix + "  "));
			}

			return result.toString();
		}

		@Override
		public String toString() {
			return toString ("");
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalString ("db_handle"      , db_handle      );
			writer.marshalString ("db_name"        , db_name        );
			writer.marshalString ("write_concern"  , write_concern  );
			writer.marshalString ("read_concern"   , read_concern   );
			writer.marshalString ("read_preference", read_preference);
			marshal_collection_config_list (writer, "collections", ver, collections);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public DatabaseConfig (MarshalReader reader, String name, int ver) {
			reader.unmarshalMapBegin (name);
			db_handle       = reader.unmarshalString ("db_handle"      );
			db_name         = reader.unmarshalString ("db_name"        );
			write_concern   = reader.unmarshalString ("write_concern"  );
			read_concern    = reader.unmarshalString ("read_concern"   );
			read_preference = reader.unmarshalString ("read_preference");
			collections     = unmarshal_collection_config_list (reader, "collections", ver);
			reader.unmarshalMapEnd ();
		}
	}

	// Check if a database configuration list is valid.
	// Returns true if valid and non-empty, false if valid and empty, exception if invalid.

	public static boolean check_database_config_list (List<DatabaseConfig> database_config_list) {
		if (database_config_list != null) {
			HashSet<String> handle_set = new HashSet<String>();
			HashSet<String> name_set = new HashSet<String>();
			if (database_config_list.isEmpty()) {
				return false;
			}
			for (DatabaseConfig database_config : database_config_list) {
				database_config.check_invariant();
				if (!( handle_set.add (database_config.get_db_handle()) )) {
					throw new InvariantViolationException ("MongoDBConfig.check_database_config_list: Duplicate database handle: " + database_config.get_db_handle());
				}
				if (!( name_set.add (database_config.get_db_name()) )) {
					throw new InvariantViolationException ("MongoDBConfig.check_database_config_list: Duplicate database name: " + database_config.get_db_name());
				}
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_database_config_list: Invalid database configuration list: <null>");
	}

	// Marshal a database configuration list.

	public static void marshal_database_config_list (MarshalWriter writer, String name, int ver, List<DatabaseConfig> database_config_list) {
		int n = database_config_list.size();
		writer.marshalArrayBegin (name, n);
		for (DatabaseConfig database_config : database_config_list) {
			database_config.marshal (writer, null, ver);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a database configuration list.

	public static ArrayList<DatabaseConfig> unmarshal_database_config_list (MarshalReader reader, String name, int ver) {
		ArrayList<DatabaseConfig> database_config_list = new ArrayList<DatabaseConfig>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			database_config_list.add (new DatabaseConfig (reader, null, ver));
		}
		reader.unmarshalArrayEnd ();
		return database_config_list;
	}




	//----- Hosts -----




	// HostConfig - Holds configuration for a host.
	// This is an immutable object.

	public static class HostConfig {
	
		// Host handle (name used in program code, must be unique across all hosts).

		private String host_handle;

		public String get_host_handle () {
			return host_handle;
		}
	
		// Write concern.

		private String write_concern;

		public String get_write_concern () {
			return write_concern;
		}
	
		// Read concern.

		private String read_concern;

		public String get_read_concern () {
			return read_concern;
		}
	
		// Read preference.

		private String read_preference;

		public String get_read_preference () {
			return read_preference;
		}
	
		// Retry writes.

		private String retry_writes;

		public String get_retry_writes () {
			return retry_writes;
		}
	
		// Cluster connection mode.

		private String connection_mode;

		public String get_connection_mode () {
			return connection_mode;
		}
	
		// Cluster type.

		private String cluster_type;

		public String get_cluster_type () {
			return cluster_type;
		}
	
		// Replica set name.

		private String replica_set_name;

		public String get_replica_set_name () {
			return replica_set_name;
		}
	
		// Connection string.

		private String connection_string;

		public String get_connection_string () {
			return connection_string;
		}
	
		// Username (for logging in to MongoDB).

		private String username;

		public String get_username () {
			return username;
		}
	
		// Authentication database (for logging in to MongoDB).

		private String auth_db;

		public String get_auth_db () {
			return auth_db;
		}
	
		// Password (for logging in to MongoDB).

		private String password;

		public String get_password () {
			return password;
		}

		// Session support level.

		private int session_level;

		public int get_session_level () {
			return session_level;
		}

		// Session causal consistency.

		private String causal_consistency;

		public String get_causal_consistency () {
			return causal_consistency;
		}
	
		// Transaction write concern.

		private String transact_write_concern;

		public String get_transact_write_concern () {
			return transact_write_concern;
		}
	
		// Transaction read concern.

		private String transact_read_concern;

		public String get_transact_read_concern () {
			return transact_read_concern;
		}
	
		// Transaction read preference.

		private String transact_read_preference;

		public String get_transact_read_preference () {
			return transact_read_preference;
		}

		// Transaction retry limit, -1 for no limit.

		private int transact_retries;

		public int get_transact_retries () {
			return transact_retries;
		}

		// Commit retry limit, -1 for no limit.

		private int commit_retries;

		public int get_commit_retries () {
			return commit_retries;
		}

		// Host address list.

		private List<HostAddress> addresses;

		public List<HostAddress> get_addresses () {
			return Collections.unmodifiableList (addresses);
		}

		// Database configuration list.

		private List<DatabaseConfig> databases;

		public List<DatabaseConfig> get_databases () {
			return Collections.unmodifiableList (databases);
		}

		// Constructor.

		public HostConfig (String host_handle, String write_concern, String read_concern, String read_preference, String retry_writes,
							String connection_mode, String cluster_type, String replica_set_name, String connection_string, 
							String username, String auth_db, String password,
							int session_level, String causal_consistency,  String transact_write_concern, String transact_read_concern,
							String transact_read_preference, int transact_retries, int commit_retries,
							List<HostAddress> addresses, List<DatabaseConfig> databases) {
			this.host_handle              = host_handle;
			this.write_concern            = write_concern;
			this.read_concern             = read_concern;
			this.read_preference          = read_preference;
			this.retry_writes             = retry_writes;
			this.connection_mode          = connection_mode;
			this.cluster_type             = cluster_type;
			this.replica_set_name         = replica_set_name;
			this.connection_string        = connection_string;
			this.username                 = username;
			this.auth_db                  = auth_db;
			this.password                 = password;
			this.session_level            = session_level;
			this.causal_consistency       = causal_consistency;
			this.transact_write_concern   = transact_write_concern;
			this.transact_read_concern    = transact_read_concern;
			this.transact_read_preference = transact_read_preference;
			this.transact_retries         = transact_retries;
			this.commit_retries           = commit_retries;
			this.addresses                = new ArrayList<HostAddress> (addresses);
			this.databases                = new ArrayList<DatabaseConfig> (databases);
		}

		// Check that values are valid, throw an exception if not.

		public void check_invariant () {

			if (!( host_handle != null && host_handle.trim().length() > 0 )) {
				throw new InvariantViolationException ("MongoDBConfig.HostConfig: Invalid host_handle: " + ((host_handle == null) ? "<null>" : host_handle));
			}

			check_write_concern (write_concern);

			check_read_concern (read_concern);

			check_read_preference (read_preference);

			check_retry_writes (retry_writes);

			check_connection_mode (connection_mode);

			check_cluster_type (cluster_type);

			check_replica_set_name (replica_set_name);

			check_connection_string (connection_string);

			check_credential (username, auth_db, password);

			if (!( session_level == SESSION_LEVEL_DISABLE || session_level == SESSION_LEVEL_ENABLE || session_level == SESSION_LEVEL_TRANSACT )) {
				throw new InvariantViolationException ("MongoDBConfig.HostConfig: Invalid session support level: " + session_level);
			}

			check_causal_consistency (causal_consistency);

			check_write_concern (transact_write_concern);

			check_read_concern (transact_read_concern);

			check_read_preference (transact_read_preference);

			if (!( transact_retries >= -1 )) {
				throw new InvariantViolationException ("MongoDBConfig.HostConfig: Invalid transaction retry limit: " + transact_retries);
			}

			if (!( commit_retries >= -1 )) {
				throw new InvariantViolationException ("MongoDBConfig.HostConfig: Invalid commit retry limit: " + commit_retries);
			}

			check_host_address_list (addresses);

			if (!( check_database_config_list (databases) )) {
				throw new InvariantViolationException ("MongoDBConfig.HostConfig: Empty databases list");
			}
			
			return;
		}

		// Make the MongoDB MongoClientSettings object for this host address.

		public MongoClientSettings make_client_settings () {
			MongoClientSettings.Builder builder = MongoClientSettings.builder();

			builder = apply_connection_string (builder, connection_string);

			if (   quick_check_connection_mode (connection_mode)
				|| quick_check_cluster_type (cluster_type)
				|| quick_check_replica_set_name (replica_set_name)
				|| quick_check_host_address_list (addresses)) {
				
				builder = builder.applyToClusterSettings (cluster_builder -> {
					apply_host_address_list (cluster_builder, addresses);
					apply_connection_mode (cluster_builder, connection_mode);
					apply_cluster_type (cluster_builder, cluster_type);
					apply_replica_set_name (cluster_builder, replica_set_name);
				});
			}

			builder = apply_credential (builder, username, auth_db, password);
			builder = apply_write_concern (builder, write_concern);
			builder = apply_read_concern (builder, read_concern);
			builder = apply_read_preference (builder, read_preference);
			builder = apply_retry_writes (builder, retry_writes);

			return builder.build();
		}

		// Make the MongoDB MongoClient object for this host.

		public MongoClient make_client () {
			MongoClientSettings settings = make_client_settings ();
			return MongoClients.create (settings);
		}

		// Return true if sessions are enabled.

		public boolean is_session_enabled () {
			switch (session_level) {
				case SESSION_LEVEL_ENABLE:
				case SESSION_LEVEL_TRANSACT:
					return true;
			}
			return false;
		}

		// Return true if transactions are enabled.

		public boolean is_transaction_enabled () {
			switch (session_level) {
				case SESSION_LEVEL_TRANSACT:
					return true;
			}
			return false;
		}

		// Make the MongoDB ClientSession object for this host.
		// Note: Return is null if session support level is disable.

		public ClientSession make_session (MongoClient mongo_client) {

			ClientSession client_session = null;

			// If sessions are enabled ...

			if (session_level != SESSION_LEVEL_DISABLE) {

				// Default transaction options

				TransactionOptions transaction_options = null;

				if (   quick_check_write_concern (transact_write_concern)
					|| quick_check_read_concern (transact_read_concern)
					|| quick_check_read_preference (transact_read_preference)) {
					
					TransactionOptions.Builder builder = TransactionOptions.builder();
					builder = apply_write_concern (builder, transact_write_concern);
					builder = apply_read_concern (builder, transact_read_concern);
					builder = apply_read_preference (builder, transact_read_preference);
					transaction_options = builder.build();
				}

				// Client session options

				ClientSessionOptions session_options = null;

				if (   transaction_options != null
					|| quick_check_causal_consistency (causal_consistency)) {
					
					ClientSessionOptions.Builder builder = ClientSessionOptions.builder();
					builder = apply_causal_consistency (builder, causal_consistency);
					if (transaction_options != null) {
						builder = builder.defaultTransactionOptions (transaction_options);
					}
					session_options = builder.build();
				}

				// Create the session

				if (session_options != null) {
					client_session = mongo_client.startSession (session_options);
				} else {
					client_session = mongo_client.startSession ();
				}
			
			}

			return client_session;
		}

		// Display our contents.

		public String toString (String prefix) {
			StringBuilder result = new StringBuilder();

			result.append (prefix + "host_handle = " + ((host_handle == null) ? "<null>" : host_handle) + "\n");
			result.append (prefix + "write_concern = " + ((write_concern == null) ? "<null>" : write_concern) + "\n");
			result.append (prefix + "read_concern = " + ((read_concern == null) ? "<null>" : read_concern) + "\n");
			result.append (prefix + "read_preference = " + ((read_preference == null) ? "<null>" : read_preference) + "\n");
			result.append (prefix + "retry_writes = " + ((retry_writes == null) ? "<null>" : retry_writes) + "\n");
			result.append (prefix + "connection_mode = " + ((connection_mode == null) ? "<null>" : connection_mode) + "\n");
			result.append (prefix + "cluster_type = " + ((cluster_type == null) ? "<null>" : cluster_type) + "\n");
			result.append (prefix + "replica_set_name = " + ((replica_set_name == null) ? "<null>" : replica_set_name) + "\n");
			result.append (prefix + "connection_string = " + ((connection_string == null) ? "<null>" : connection_string) + "\n");
			result.append (prefix + "username = " + ((username == null) ? "<null>" : username) + "\n");
			result.append (prefix + "auth_db = " + ((auth_db == null) ? "<null>" : auth_db) + "\n");
			result.append (prefix + "password = " + ((password == null) ? "<null>" : password) + "\n");
			result.append (prefix + "session_level = " + session_level + "\n");
			result.append (prefix + "causal_consistency = " + ((causal_consistency == null) ? "<null>" : causal_consistency) + "\n");
			result.append (prefix + "transact_write_concern = " + ((transact_write_concern == null) ? "<null>" : transact_write_concern) + "\n");
			result.append (prefix + "transact_read_concern = " + ((transact_read_concern == null) ? "<null>" : transact_read_concern) + "\n");
			result.append (prefix + "transact_read_preference = " + ((transact_read_preference == null) ? "<null>" : transact_read_preference) + "\n");
			result.append (prefix + "transact_retries = " + transact_retries + "\n");
			result.append (prefix + "commit_retries = " + commit_retries + "\n");

			if (addresses.size() == 0) {
				result.append (prefix + "addresses = <empty>\n");
			}
			for (int k = 0; k < addresses.size(); ++k) {
				result.append (prefix + "addresses[" + k + "]:\n");
				result.append (addresses.get(k).toString (prefix + "  "));
			}

			for (int k = 0; k < databases.size(); ++k) {
				result.append (prefix + "databases[" + k + "]:\n");
				result.append (databases.get(k).toString (prefix + "  "));
			}

			return result.toString();
		}

		@Override
		public String toString() {
			return toString ("");
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalString ("host_handle"             , host_handle             );
			writer.marshalString ("write_concern"           , write_concern           );
			writer.marshalString ("read_concern"            , read_concern            );
			writer.marshalString ("read_preference"         , read_preference         );
			writer.marshalString ("retry_writes"            , retry_writes            );
			writer.marshalString ("connection_mode"         , connection_mode         );
			writer.marshalString ("cluster_type"            , cluster_type            );
			writer.marshalString ("replica_set_name"        , replica_set_name        );
			writer.marshalString ("connection_string"       , connection_string       );
			writer.marshalString ("username"                , username                );
			writer.marshalString ("auth_db"                 , auth_db                 );
			writer.marshalString ("password"                , password                );
			writer.marshalInt    ("session_level"           , session_level           );
			writer.marshalString ("causal_consistency"      , causal_consistency      );
			writer.marshalString ("transact_write_concern"  , transact_write_concern  );
			writer.marshalString ("transact_read_concern"   , transact_read_concern   );
			writer.marshalString ("transact_read_preference", transact_read_preference);
			writer.marshalInt    ("transact_retries"        , transact_retries        );
			writer.marshalInt    ("commit_retries"          , commit_retries          );
			marshal_host_address_list    (writer, "addresses", ver, addresses);
			marshal_database_config_list (writer, "databases", ver, databases);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public HostConfig (MarshalReader reader, String name, int ver) {
			reader.unmarshalMapBegin (name);
			host_handle              = reader.unmarshalString ("host_handle"             );
			write_concern            = reader.unmarshalString ("write_concern"           );
			read_concern             = reader.unmarshalString ("read_concern"            );
			read_preference          = reader.unmarshalString ("read_preference"         );
			retry_writes             = reader.unmarshalString ("retry_writes"            );
			connection_mode          = reader.unmarshalString ("connection_mode"         );
			cluster_type             = reader.unmarshalString ("cluster_type"            );
			replica_set_name         = reader.unmarshalString ("replica_set_name"        );
			connection_string        = reader.unmarshalString ("connection_string"       );
			username                 = reader.unmarshalString ("username"                );
			auth_db                  = reader.unmarshalString ("auth_db"                 );
			password                 = reader.unmarshalString ("password"                );
			session_level            = reader.unmarshalInt    ("session_level"           );
			causal_consistency       = reader.unmarshalString ("causal_consistency"      );
			transact_write_concern   = reader.unmarshalString ("transact_write_concern"  );
			transact_read_concern    = reader.unmarshalString ("transact_read_concern"   );
			transact_read_preference = reader.unmarshalString ("transact_read_preference");
			transact_retries         = reader.unmarshalInt    ("transact_retries"        );
			commit_retries           = reader.unmarshalInt    ("commit_retries"          );
			addresses                = unmarshal_host_address_list    (reader, "addresses", ver);
			databases                = unmarshal_database_config_list (reader, "databases", ver);
			reader.unmarshalMapEnd ();
		}
	}

	// Check if a host configuration list is valid.
	// Returns true if valid and non-empty, false if valid and empty, exception if invalid.

	public static boolean check_host_config_list (List<HostConfig> host_config_list) {
		if (host_config_list != null) {
			HashSet<String> host_handle_set = new HashSet<String>();
			HashSet<String> db_handle_set = new HashSet<String>();
			if (host_config_list.isEmpty()) {
				return false;
			}
			for (HostConfig host_config : host_config_list) {
				host_config.check_invariant();
				if (!( host_handle_set.add (host_config.get_host_handle()) )) {
					throw new InvariantViolationException ("MongoDBConfig.check_host_config_list: Duplicate host handle: " + host_config.get_host_handle());
				}
				for (DatabaseConfig database_config : host_config.get_databases()) {
					if (!( db_handle_set.add (database_config.get_db_handle()) )) {
						throw new InvariantViolationException ("MongoDBConfig.check_host_config_list: Duplicate database handle: " + database_config.get_db_handle());
					}
				}
			}
			return true;
		}
		throw new InvariantViolationException ("MongoDBConfig.check_host_config_list: Invalid host configuration list: <null>");
	}

	// Marshal a host configuration list.

	public static void marshal_host_config_list (MarshalWriter writer, String name, int ver, List<HostConfig> host_config_list) {
		int n = host_config_list.size();
		writer.marshalArrayBegin (name, n);
		for (HostConfig host_config : host_config_list) {
			host_config.marshal (writer, null, ver);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a host configuration list.

	public static ArrayList<HostConfig> unmarshal_host_config_list (MarshalReader reader, String name, int ver) {
		ArrayList<HostConfig> host_config_list = new ArrayList<HostConfig>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			host_config_list.add (new HostConfig (reader, null, ver));
		}
		reader.unmarshalArrayEnd ();
		return host_config_list;
	}




	//----- Mongo configuration -----

	


	// Handle for the default database, or "" if none.

	private String default_db_handle;

	public String get_default_db_handle () {
		return default_db_handle;
	}

	// Host configuration list.

	private List<HostConfig> hosts;

	public List<HostConfig> get_hosts () {
		return Collections.unmodifiableList (hosts);
	}

	// Constructor.

	public MongoDBConfig (String default_db_handle, List<HostConfig> hosts) {
		this.default_db_handle = default_db_handle;
		this.hosts = new ArrayList<HostConfig> (hosts);
	}

	// Check that values are valid, throw an exception if not.

	public void check_invariant () {

		if (!( default_db_handle != null )) {
			throw new InvariantViolationException ("MongoDBConfig.MongoDBConfig: Invalid default_db_handle: " + ((default_db_handle == null) ? "<null>" : default_db_handle));
		}

		check_host_config_list (hosts);

		HashSet<String> db_handle_set = new HashSet<String>();
		for (HostConfig host_config : hosts) {
			for (DatabaseConfig database_config : host_config.get_databases()) {
				if (!( db_handle_set.add (database_config.get_db_handle()) )) {
					throw new InvariantViolationException ("MongoDBConfig.check_invariant: Duplicate database handle: " + database_config.get_db_handle());
				}
			}
		}
		if (!( default_db_handle.isEmpty() )) {
			if (!( db_handle_set.contains (default_db_handle) )) {
				throw new InvariantViolationException ("MongoDBConfig.check_invariant: Undefined default database handle: " + default_db_handle);
			}
		}
			
		return;
	}

	// Display our contents.

	public String toString (String prefix) {
		StringBuilder result = new StringBuilder();

		result.append (prefix + "default_db_handle = " + ((default_db_handle == null) ? "<null>" : default_db_handle) + "\n");

		for (int k = 0; k < hosts.size(); ++k) {
			result.append (prefix + "hosts[" + k + "]:\n");
			result.append (hosts.get(k).toString (prefix + "  "));
		}

		return result.toString();
	}

	@Override
	public String toString() {
		return toString ("");
	}

	// Marshal.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		int ver = MARSHAL_VER_1;
		writer.marshalInt (M_VERSION_NAME, ver);
		writer.marshalString ("default_db_handle", default_db_handle);
		marshal_host_config_list (writer, "hosts", ver, hosts);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal.

	public MongoDBConfig (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);
		default_db_handle = reader.unmarshalString ("default_db_handle");
		hosts = unmarshal_host_config_list (reader, "hosts", ver);
		reader.unmarshalMapEnd ();
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MongoDBConfig : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("MongoDBConfig : Unrecognized subcommand : " + args[0]);
		return;

	}

}
