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

import static org.opensha.oaf.aafs.MongoDBConfig.CollectionConfig;
import static org.opensha.oaf.aafs.MongoDBConfig.DatabaseConfig;
import static org.opensha.oaf.aafs.MongoDBConfig.HostConfig;

import static org.opensha.oaf.aafs.MongoDBConnect.HostState;



/**
 * Access to content of MongoDB.
 * Author: Michael Barall.
 *
 * This object manages connections to databases and collections.
 *
 * Each of these objects can be used only by a single thread.
 */
public class MongoDBContent implements AutoCloseable {




	//----- Tracing -----




	// Flag to trace connections.  (Not synchronized because it should only be set at startup.)

	private static boolean trace_conn = false;

	public static void set_trace_conn (boolean b) {
		trace_conn = b;
		return;
	}

	// Flag to trace sessions.  (Not synchronized because it should only be set at startup.)

	private static boolean trace_session = false;

	public static void set_trace_session (boolean b) {
		trace_session = b;
		return;
	}

	// Flag to trace transactions.  (Not synchronized because it should only be set at startup.)

	private static boolean trace_transact = false;

	public static void set_trace_transact (boolean b) {
		trace_transact = b;
		return;
	}




	//----- Collections -----




	// CollectionState - Holds current state for a collection.

	public static class CollectionState implements MongoDBCollHandle {

		// Parent database state for this collection.

		private final DatabaseState database_state;

		public DatabaseState get_database_state () {
			return database_state;
		}

		// Configuration for this collection.

		private final CollectionConfig collection_config;

		public CollectionConfig get_collection_config () {
			return collection_config;
		}

		// The MongoDB collection, or null if none.

		private MongoCollection<Document> mongo_collection;

		// Constructor.

		public CollectionState (DatabaseState database_state, CollectionConfig collection_config) {
			this.database_state = database_state;
			this.collection_config = collection_config;
			mongo_collection = null;
		}

		// Release all resources.
		// If f_call_upstream is true, can make upstream calls to release upstream resources (should be false if called from upstream).
		// If this throws an exception, then the resources HAVE been released.

		public void teardown_collection (boolean f_call_upstream) {
			mongo_collection = null;
			return;
		}

		//--- Subroutines for MongoDB functions ---

		// Make a string that identifies this collection, for use in exception messages.

		public String make_coll_id_message () {
			return "coll_name = " + collection_config.get_coll_name()
				+ ", db_name = " + database_state.get_database_config().get_db_name()
				+ ", db_handle = " + database_state.get_database_config().get_db_handle();
		}

		// Make the error locus for this collection, for use in exceptions.

		public MongoDBErrorLocus make_locus (MongoException mongo_exception) {
			return new MongoDBErrorLocus (
				database_state.get_session_state().get_host_state().get_host_config().get_host_handle(),
				database_state.get_database_config().get_db_handle(),
				collection_config.get_coll_name(),
				mongo_exception);
		}

		// Make the MongoCollection, if needed.
		// Note: Also checks if the database is connected, throws exception if not.

		private void make_mongo_collection () {

			// If we don't have the MongoDB collection, make it
			// Note: If mongo_collection is non-null, then the database is connected.

			if (mongo_collection == null) {
				if (!( database_state.is_database_connected() )) {
					throw new DBUnavailableDatabaseException ("CollectionState.make_mongo_collection: Database is not connected: " + make_coll_id_message());
				}
				MongoCollection<Document> my_mongo_collection;
				try {
					my_mongo_collection = collection_config.make_collection (database_state.get_mongo_database());
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "CollectionState.make_mongo_collection: MongoDB exception: " + make_coll_id_message(), e);
				}
				mongo_collection = my_mongo_collection;
			}
		
			return;
		}

		// Get session for a read operation.
		// Also checks access level, checks that database is connected, creates the MongoCollection if needed.
		// Returns null if no session currently active.

		private ClientSession get_op_session_read () {
		
			// Access level check

			collection_config.require_access_read();

			// If we don't have the MongoDB collection, make it

			make_mongo_collection();

			// Check database connected, get session

			return database_state.get_client_session();
		}

		// Get session for a write operation.
		// Also checks access level, checks that database is connected.
		// Returns null if no session currently active.

		private ClientSession get_op_session_write () {
		
			// Access level check

			collection_config.require_access_write();

			// If we don't have the MongoDB collection, make it

			make_mongo_collection();

			// Check database connected, get session

			return database_state.get_client_session();
		}

		// Get session for an update operation.
		// Also checks access level, checks that database is connected.
		// Returns null if no session currently active.

		private ClientSession get_op_session_update () {
		
			// Access level check

			collection_config.require_access_update();

			// If we don't have the MongoDB collection, make it

			make_mongo_collection();

			// Check database connected, get session

			return database_state.get_client_session();
		}

		//--- MongoDB functions ---

		// Create an index.
		// Parameters:
		//  keys = Index keys (constructed by Indexes), cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns the index name.

		@Override
		public String createIndex (Bson keys, IndexOptions options) {
			String result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.createIndex (client_session, keys, options);
					} else {
						result = mongo_collection.createIndex (client_session, keys);
					}
				} else {
					if (options != null) {
						result = mongo_collection.createIndex (keys, options);
					} else {
						result = mongo_collection.createIndex (keys);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.createIndex: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Delete one document.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns delete result object.
		// Note: This function cannot do a sort.  Use findOneAndDelete if a sort is needed.

		@Override
		public DeleteResult deleteOne (Bson filter, DeleteOptions options) {
			DeleteResult result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.deleteOne (client_session, filter, options);
					} else {
						result = mongo_collection.deleteOne (client_session, filter);
					}
				} else {
					if (options != null) {
						result = mongo_collection.deleteOne (filter, options);
					} else {
						result = mongo_collection.deleteOne (filter);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.deleteOne: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Find documents, and return the first matching document, or null if no matching document.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), or null if no filter, defaults to null.
		//  sort = Sort to use for query (constructed by Sorts), or null if no sort, defaults to null

		@Override
		public Document find_first (Bson filter, Bson sort) {
			Document result;
			try {
				ClientSession client_session = get_op_session_read();

				FindIterable<Document> fit;
				if (client_session != null) {
					if (filter != null) {
						fit = mongo_collection.find (client_session, filter);
					} else {
						fit = mongo_collection.find (client_session);
					}
				} else {
					if (filter != null) {
						fit = mongo_collection.find (filter);
					} else {
						fit = mongo_collection.find ();
					}
				}

				if (sort != null) {
					fit = fit.sort (sort);
				}

				result = fit.first();

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.find_first: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Find documents, and return an iterator over matching documents.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), or null if no filter, defaults to null.
		//  sort = Sort to use for query (constructed by Sorts), or null if no sort, defaults to null

		@Override
		public MongoCursor<Document> find_iterator (Bson filter, Bson sort) {
			MongoCursor<Document> result;
			try {
				ClientSession client_session = get_op_session_read();

				FindIterable<Document> fit;
				if (client_session != null) {
					if (filter != null) {
						fit = mongo_collection.find (client_session, filter);
					} else {
						fit = mongo_collection.find (client_session);
					}
				} else {
					if (filter != null) {
						fit = mongo_collection.find (filter);
					} else {
						fit = mongo_collection.find ();
					}
				}

				if (sort != null) {
					fit = fit.sort (sort);
				}

				result = fit.iterator();

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.find_iterator: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Find one document, and delete it.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns the document that was delete, or null if no matching document is found.
		// Note: A sort can be included within options.

		@Override
		public Document findOneAndDelete (Bson filter, FindOneAndDeleteOptions options) {
			Document result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.findOneAndDelete (client_session, filter, options);
					} else {
						result = mongo_collection.findOneAndDelete (client_session, filter);
					}
				} else {
					if (options != null) {
						result = mongo_collection.findOneAndDelete (filter, options);
					} else {
						result = mongo_collection.findOneAndDelete (filter);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.findOneAndDelete: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Find one document, and replace it.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  replacement = Replacement document, cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns the document before replacement (or after replacement if ReturnDocument.AFTER is used in options),
		//  or null if no matching document is found.
		// Note: A sort or upsert can be included within options.
		// Note: It is recommended that either ReturnDocument.BEFORE or ReturnDocument.AFTER be explicitly given.

		@Override
		public Document findOneAndReplace (Bson filter, Document replacement, FindOneAndReplaceOptions options) {
			Document result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.findOneAndReplace (client_session, filter, replacement, options);
					} else {
						result = mongo_collection.findOneAndReplace (client_session, filter, replacement);
					}
				} else {
					if (options != null) {
						result = mongo_collection.findOneAndReplace (filter, replacement, options);
					} else {
						result = mongo_collection.findOneAndReplace (filter, replacement);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.findOneAndReplace: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Find one document, and update it.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  update = Update operation to perform (constructed by Updates), cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns the document before update (or after update if ReturnDocument.AFTER is used in options),
		//  or null if no matching document is found.
		// Note: A sort or upsert can be included within options.
		// Note: It is recommended that either ReturnDocument.BEFORE or ReturnDocument.AFTER be explicitly given.

		@Override
		public Document findOneAndUpdate (Bson filter, Bson update, FindOneAndUpdateOptions options) {
			Document result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.findOneAndUpdate (client_session, filter, update, options);
					} else {
						result = mongo_collection.findOneAndUpdate (client_session, filter, update);
					}
				} else {
					if (options != null) {
						result = mongo_collection.findOneAndUpdate (filter, update, options);
					} else {
						result = mongo_collection.findOneAndUpdate (filter, update);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.findOneAndUpdate: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Insert one document into the collection.
		// Parameters:
		//  document = Document to insert.  If it does not contain an id, then an id is created.
		//  options = Options for the insert operation, or null if none, defaults to null.

		@Override
		public void insertOne (Document document, InsertOneOptions options) {
			try {
				ClientSession client_session = get_op_session_write();

				if (client_session != null) {
					if (options != null) {
						mongo_collection.insertOne (client_session, document, options);
					} else {
						mongo_collection.insertOne (client_session, document);
					}
				} else {
					if (options != null) {
						mongo_collection.insertOne (document, options);
					} else {
						mongo_collection.insertOne (document);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.insertOne: MongoDB exception: " + make_coll_id_message(), e);
			}
			return;
		}

		// Replace one document.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  replacement = Replacement document, cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns update result object.
		// Note: An upsert can be included within options.
		// Note: This function cannot do a sort.  Use findOneAndReplace if a sort is needed.

		@Override
		public UpdateResult replaceOne (Bson filter, Document replacement, ReplaceOptions options) {
			UpdateResult result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.replaceOne (client_session, filter, replacement, options);
					} else {
						result = mongo_collection.replaceOne (client_session, filter, replacement);
					}
				} else {
					if (options != null) {
						result = mongo_collection.replaceOne (filter, replacement, options);
					} else {
						result = mongo_collection.replaceOne (filter, replacement);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.replaceOne: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		// Update one document.
		// Parameters:
		//  filter = Filter to use for query (constructed by Filters), cannot be null.
		//  update = Update operation to perform (constructed by Updates), cannot be null.
		//  options = Options, or null if none, defaults to null.
		// Returns update result object.
		// Note: An upsert can be included within options.
		// Note: This function cannot do a sort.  Use findOneAndUpdate if a sort is needed.

		@Override
		public UpdateResult updateOne (Bson filter, Bson update, UpdateOptions options) {
			UpdateResult result;
			try {
				ClientSession client_session = get_op_session_update();

				if (client_session != null) {
					if (options != null) {
						result = mongo_collection.updateOne (client_session, filter, update, options);
					} else {
						result = mongo_collection.updateOne (client_session, filter, update);
					}
				} else {
					if (options != null) {
						result = mongo_collection.updateOne (filter, update, options);
					} else {
						result = mongo_collection.updateOne (filter, update);
					}
				}

			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "MongoDBCollHandle.updateOne: MongoDB exception: " + make_coll_id_message(), e);
			}
			return result;
		}

		//--- Handle functions ---

		// Get the name of the collection.

		@Override
		public String get_coll_name () {
			return collection_config.get_coll_name();
		}

		// Get the name of the database.

		@Override
		public String get_db_name () {
			return database_state.get_database_config().get_db_name();
		}

		// Get the handle of the database.

		@Override
		public String get_db_handle () {
			return database_state.get_database_config().get_db_handle();
		}

		// Get the handle of the host.

		@Override
		public String get_host_handle () {
			return database_state.get_session_state().get_host_state().get_host_config().get_host_handle();
		}

		// Get the MongoDB collection.

		@Override
		public MongoCollection<Document> get_mongo_collection () {

			// Make the MongoDB collection if needed, check if database is connected

			make_mongo_collection();

			return mongo_collection;
		}

		// Add a resource that must be auto-closed during database disconnect.
		// This is typically used for iterators.
		// If an exception is thrown, the resource is NOT added.
		// The following notes apply to resource.close():
		// - It must release all resources and call remove_resource().
		// - It must do so even if it throws an exception.
		// - It must be idempotent; second and subsequent calls should do nothing.
		// - When called, the database is still connected.
		// - It may not change content state (connect or disconnect databases, etc.).

		@Override
		public void add_resource (AutoCloseable resource) {
			database_state.add_resource (resource);
			return;
		}

		// Remove a resource that must be auto-closed during database disconnect.
		// This is typically used for iterators.
		// This function should not throw any exception.
		// Performs no operation if the resource is not currently listed.

		@Override
		public void remove_resource (AutoCloseable resource) {
			database_state.remove_resource (resource);
			return;
		}

	}




	//----- Databases -----




	// DatabaseState - Holds current state for a database.

	public static class DatabaseState {

		// Parent session state for this collection.

		private final SessionState session_state;

		public SessionState get_session_state () {
			return session_state;
		}

		// Configuration for this database.

		private final DatabaseConfig database_config;

		public DatabaseConfig get_database_config () {
			return database_config;
		}

		// The MongoDB database, or null if none.

		private MongoDatabase mongo_database;

		// The current number of connections.

		private int conn_count;

		// The current number of sessions; if nonzero then conn_count must also be nonzero.

		private int session_count;

		// The current number of transactions, 0 or 1; if nonzero then session_count must also be nonzero.

		private int transact_count;

		// The contained collection states, indexed by collection name.

		private LinkedHashMap<String, CollectionState> collection_states;

		public Collection<CollectionState> get_collection_states () {
			return Collections.unmodifiableCollection (collection_states.values());
		}

		public CollectionState get_collection_state (String coll_name) {
			CollectionState result = collection_states.get (coll_name);
			if (result == null) {
				throw new DBUnknownCollectionException ("Unknown collection: coll_name = " + coll_name + ", " + make_db_id_message());
			}
			return result;
		}

		// The set of resources that must be auto-closed on database disconnect (typically cursors into the database).

		private LinkedHashSet<AutoCloseable> auto_resources;

		// Constructor.

		public DatabaseState (SessionState session_state, DatabaseConfig database_config) {
			this.session_state = session_state;
			this.database_config = database_config;
			mongo_database = null;
			conn_count = 0;
			session_count = 0;
			transact_count = 0;
			collection_states = new LinkedHashMap<String, CollectionState>();
			auto_resources = new LinkedHashSet<AutoCloseable>();

			// Construct the contained collection states

			List<CollectionConfig> collection_configs =  this.database_config.get_collections();
			for (CollectionConfig collection_config : collection_configs) {
				collection_states.put (collection_config.get_coll_name(), new CollectionState (this, collection_config));
			}
		}

		// Return true if the database is currently connected.

		public boolean is_database_connected () {
			return (conn_count != 0);
		}

		// Get the MongoDB database.

		public MongoDatabase get_mongo_database () {

			// Make the MongoDB database if needed, check if database is connected

			make_mongo_database();

			// Return the database

			return mongo_database;
		}

		// Undo-able connection establishment.

		public class UndoConnect implements AutoCloseable {

			// Flag indicates if undo is desired.

			public boolean f_undo;

			// Turn off undo, to keep the connection.

			public void keep () {
				f_undo = false;
			}
		
			// Connect during constructor, if requested.

			public UndoConnect (boolean f_make) {
				f_undo = true;
				if (f_make) {

					// If not currently connected ...

					if (conn_count == 0) {
			
						// Connect the session

						session_state.conn_session();
					}

					// Count the connection

					++conn_count;
				}
			}

			// Disconnect during close, if requested.

			@Override
			public void close () {
				if (f_undo) {
					f_undo = false;
		
					// If currently connected ...

					if (conn_count != 0) {

						// If we're not disconnecting, just update the count

						if (conn_count != 1) {
							--conn_count;
						}

						// Otherwise, tear down the database

						else {
							teardown_database (true);
						}
					}
				}

				return;
			}
		}

		// Undo-able session establishment.

		public class UndoSession implements AutoCloseable {

			// Flag indicates if undo is desired.

			public boolean f_undo;

			// Turn off undo, to keep the connection.

			public void keep () {
				f_undo = false;
			}
		
			// Create session during constructor, if requested.

			public UndoSession (boolean f_make) {
				f_undo = true;
				if (f_make) {

					// Error if not connected

					if (conn_count == 0) {
						throw new InvariantViolationException ("DatabaseState.UndoSession.UndoSession: Not connected: " + make_db_id_message());
					}

					// If no client session currently ...

					if (session_count == 0) {
			
						// Open the client session
						// Note exceptions here need not be caught

						session_state.open_client_session();
					}
		
					// Count the client session

					++session_count;
				}
			}

			// Close session during close, if requested.

			@Override
			public void close () {
				if (f_undo) {
					f_undo = false;
		
					// Error if not connected

					if (conn_count == 0) {
						throw new DBUnavailableDatabaseException ("DatabaseState.UndoSession.close: Not connected: " + make_db_id_message());
					}
		
					// If currently have a session ...

					if (session_count != 0) {

						// If we're not closing client_session, just update the count

						if (session_count != 1) {
							--session_count;
						}

						// Otherwise, tear down client_session

						else {
							teardown_client_session (true);
						}
					}
				}

				return;
			}
		}

		//  // Connect the database.
		//  // If this throws an exception, then the connection has NOT occurred.
		//  
		//  public void conn_database () {
		//  
		//  	// Error if there is a session
		//  
		//  	if (session_count != 0) {
		//  		throw new DBUnavailableDatabaseException ("DatabaseState.conn_database: Cannot connect because a session is open: " + make_db_id_message());
		//  	}
		//  
		//  	try (
		//  		UndoConnect undo_connect = new UndoConnect (true);
		//  	) {
		//  		undo_connect.keep();
		//  	}
		//  
		//  	return;
		//  }
		//  
		//  // Disconnect the database.
		//  // If this throws an exception, then the disconnection HAS occurred.
		//  // Performs no operation if not currently connected.
		//  
		//  public void disc_database () {
		//  
		//  	// Error if there is a session
		//  
		//  	if (session_count != 0) {
		//  		throw new DBUnavailableDatabaseException ("DatabaseState.disc_database: Cannot disconnect because a session is open: " + make_db_id_message());
		//  	}
		//  
		//  	try (
		//  		UndoConnect undo_connect = new UndoConnect (false);
		//  	) {
		//  	}
		//  
		//  	return;
		//  }

		// Connect the database.
		// If this throws an exception, then the connection has NOT occurred.
		// Returns the connection count after the connect.

		public int conn_database () {

			if (trace_conn) {
				System.out.println ("DatabaseState.conn_database: Enter: " + make_db_id_message() + ", conn_count = " + conn_count);
			}

			// If not currently connected ...

			if (conn_count == 0) {
			
				// Connect the session
				// Note we're still torn down, so exceptions here need not be caught

				session_state.conn_session();
			}

			// Count the connection

			++conn_count;

			if (trace_conn) {
				System.out.println ("DatabaseState.conn_database: Exit: " + make_db_id_message() + ", conn_count = " + conn_count);
			}

			return conn_count;
		}

		// Disconnect the database.
		// If this throws an exception, then the disconnection HAS occurred.
		// Performs no operation if not currently connected.
		// Returns the connection count before the disconnect.

		public int disc_database () {

			if (trace_conn) {
				System.out.println ("DatabaseState.disc_database: Enter: " + make_db_id_message() + ", conn_count = " + conn_count);
			}

			int result = conn_count;
		
			// If currently connected ...

			if (conn_count != 0) {

				// If we're not disconnecting, just update the count

				if (conn_count != 1) {
					--conn_count;
				}

				// Otherwise, tear down the database

				else {
					teardown_database (true);
				}
			}

			if (trace_conn) {
				System.out.println ("DatabaseState.disc_database: Exit: " + make_db_id_message() + ", conn_count = " + conn_count);
			}

			return result;
		}

		// Release all resources.
		// If f_call_upstream is true, can make upstream calls to release upstream resources (should be false if called from upstream).
		// If this throws an exception, then the resources HAVE been released.

		public void teardown_database (boolean f_call_upstream) {

			// Accumulate exceptions to re-throw

			DBDriverException my_driver_exception = null;
			MongoException my_mongo_exception = null;
			Exception my_exception = null;

			// If there are auto-closeable resources ...

			if (!( auto_resources.isEmpty() )) {
		
				// Make a copy of the current set, preserving the order

				ArrayList<AutoCloseable> auto_list = new ArrayList<AutoCloseable> (auto_resources);

				// Erase the set, so calls to remove_resource() will have no effect

				auto_resources.clear();

				// Close the resources, in reverse order

				for (int k = auto_list.size() - 1; k >= 0; --k) {
					try {
						auto_list.get(k).close();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// Teardown the collections

			for (CollectionState collection_state : collection_states.values()) {
				try {
					collection_state.teardown_collection (false);
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// Clear our own resources

			mongo_database = null;

			// If we have a transaction, clear the count, and report it upstream if desired

			if (transact_count != 0) {
				transact_count = 0;

				if (f_call_upstream) {
					try {
						session_state.stop_transaction();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// If we have a session, clear the count, and report it upstream if desired

			if (session_count != 0) {
				session_count = 0;

				if (f_call_upstream) {
					try {
						session_state.close_client_session();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// If we have a connection, clear the count, and report it upstream if desired

			if (conn_count != 0) {
				conn_count = 0;

				if (f_call_upstream) {
					try {
						session_state.disc_session();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// Propagate exceptions

			if (my_driver_exception != null) {
				throw new DBDriverException (my_driver_exception.get_locus(), "DatabaseState.teardown_database: Driver exception: " + make_db_id_message(), my_driver_exception);
			}
			if (my_mongo_exception != null) {
				throw new DBDriverException (make_locus(my_mongo_exception), "DatabaseState.teardown_database: MongoDB exception: " + make_db_id_message(), my_mongo_exception);
			}
			if (my_exception != null) {
				throw new DBException ("DatabaseState.teardown_database: Exception during tear-down: " + make_db_id_message(), my_exception);
			}
		
			return;
		}

		// Release all resources related to client_session (but not the connection, database, iterators, or collections).
		// If f_call_upstream is true, can make upstream calls to release upstream resources (should be false if called from upstream).
		// If this throws an exception, then the resources HAVE been released.

		public void teardown_client_session (boolean f_call_upstream) {

			// Accumulate exceptions to re-throw

			DBDriverException my_driver_exception = null;
			MongoException my_mongo_exception = null;
			Exception my_exception = null;

			// If we have a transaction, clear the count, and report it upstream if desired

			if (transact_count != 0) {
				transact_count = 0;

				if (f_call_upstream) {
					try {
						session_state.stop_transaction();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// If we have a session, clear the count, and report it upstream if desired

			if (session_count != 0) {
				session_count = 0;

				if (f_call_upstream) {
					try {
						session_state.close_client_session();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// Propagate exceptions

			if (my_driver_exception != null) {
				throw new DBDriverException (my_driver_exception.get_locus(), "DatabaseState.teardown_database: Driver exception: " + make_db_id_message(), my_driver_exception);
			}
			if (my_mongo_exception != null) {
				throw new DBDriverException (make_locus(my_mongo_exception), "DatabaseState.teardown_database: MongoDB exception: " + make_db_id_message(), my_mongo_exception);
			}
			if (my_exception != null) {
				throw new DBException ("DatabaseState.teardown_database: Exception during tear-down: " + make_db_id_message(), my_exception);
			}
		
			return;
		}

		//  // Open a client session.
		//  // If this throws an exception, the client session is NOT opened.
		//  
		//  public void open_client_session () {
		//  
		//  	// Error if there is a transaction
		//  
		//  	if (transact_count != 0) {
		//  		throw new DBUnavailableDatabaseException ("DatabaseState.open_client_session: Cannot open session because a transaction is in progress: " + make_db_id_message());
		//  	}
		//  
		//  	try (
		//  		UndoConnect undo_connect = new UndoConnect (true);
		//  		UndoSession undo_session = new UndoSession (true);
		//  	) {
		//  		undo_connect.keep();
		//  		undo_session.keep();
		//  	}
		//  
		//  	return;
		//  }
		//  
		//  // Close a client session.
		//  // If this throws an exception, the client session IS closed.
		//  // Performs no operation if the client session is not open.
		//  
		//  public void close_client_session () {
		//  
		//  	// Error if there is a transaction
		//  
		//  	if (transact_count != 0) {
		//  		throw new DBUnavailableDatabaseException ("DatabaseState.close_client_session: Cannot close session because a transaction is in progress: " + make_db_id_message());
		//  	}
		//  
		//  	try (
		//  		UndoConnect undo_connect = new UndoConnect (false);
		//  		UndoSession undo_session = new UndoSession (false);
		//  	) {
		//  	}
		//  
		//  	return;
		//  }

		// Open a client session.
		// If this throws an exception, the client session is NOT opened.
		// Returns the session count after the open.

		public int open_client_session () {

			if (trace_session) {
				System.out.println ("DatabaseState.open_client_session: Enter: " + make_db_id_message() + ", session_count = " + session_count);
			}

			// Error if not connected

			if (conn_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.open_client_session: Not connected: " + make_db_id_message());
			}

			// If no client session currently ...

			if (session_count == 0) {
			
				// Open the client session
				// Note exceptions here need not be caught

				session_state.open_client_session();
			}
		
			// Count the client session

			++session_count;

			if (trace_session) {
				System.out.println ("DatabaseState.open_client_session: Exit: " + make_db_id_message() + ", session_count = " + session_count);
			}

			return session_count;
		}

		// Close a client session.
		// If this throws an exception, the client session IS closed.
		// Performs no operation if the client session is not open.
		// Returns the session count before the close.

		public int close_client_session () {

			if (trace_session) {
				System.out.println ("DatabaseState.close_client_session: Enter: " + make_db_id_message() + ", session_count = " + session_count);
			}

			int result = session_count;

			// Error if not connected

			if (conn_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.close_client_session: Not connected: " + make_db_id_message());
			}
		
			// If currently have a session ...

			if (session_count != 0) {

				// If we're not closing client_session, just update the count

				if (session_count != 1) {
					--session_count;
				}

				// Otherwise, tear down client_session

				else {
					teardown_client_session (true);
				}
			}

			if (trace_session) {
				System.out.println ("DatabaseState.close_client_session: Exit: " + make_db_id_message() + ", session_count = " + session_count);
			}

			return result;
		}

		// Get the current client session.
		// Note it is legal to call this function even if a session has not been opened.
		// Note the return value can be null even if a session has been opened, if sessions are disabled in the configuration.

		public ClientSession get_client_session () {

			// Error if not connected

			if (conn_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.get_client_session: Not connected: " + make_db_id_message());
			}

			// If we have no session, return null

			if (session_count == 0) {
				return null;
			}

			// Return the client session

			return session_state.get_client_session();
		}

		// Start a transaction.
		// If this throws an exception, the transaction is NOT started.

		public void start_transaction (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("DatabaseState.start_transaction: Enter: " + make_db_id_message() + ", transact_count = " + transact_count);
			}

			// Error if no session

			if (session_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.start_transaction: No session: " + make_db_id_message());
			}

			// Error if there is already a transaction

			if (transact_count != 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.start_transaction: Transaction already started: " + make_db_id_message());
			}
			
			// Start the transaction
			// Note exceptions here need not be caught

			session_state.start_transaction (f_commit);
		
			// Count the transaction

			++transact_count;

			if (trace_transact) {
				System.out.println ("DatabaseState.start_transaction: Exit: " + make_db_id_message() + ", transact_count = " + transact_count);
			}

			return;
		}

		// Stop a transaction.
		// If this throws an exception, the transaction IS stopped.

		public void stop_transaction () {

			if (trace_transact) {
				System.out.println ("DatabaseState.stop_transaction: Enter: " + make_db_id_message() + ", transact_count = " + transact_count);
			}

			// Error if no session

			if (session_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.stop_transaction: No session: " + make_db_id_message());
			}

			// If there is a transaction in progress ...

			if (transact_count != 0) {
			
				// Clear the counter

				transact_count = 0;

				// Stop the transaction
				// Note exceptions here need not be caught

				session_state.stop_transaction ();
			}

			if (trace_transact) {
				System.out.println ("DatabaseState.stop_transaction: Exit: " + make_db_id_message() + ", transact_count = " + transact_count);
			}

			return;
		}

		public void stop_transaction (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("DatabaseState.stop_transaction: Enter: " + make_db_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			// Error if no session

			if (session_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.stop_transaction: No session: " + make_db_id_message());
			}

			// If there is a transaction in progress ...

			if (transact_count != 0) {
			
				// Clear the counter

				transact_count = 0;

				// Stop the transaction
				// Note exceptions here need not be caught

				session_state.stop_transaction (f_commit);
			}

			if (trace_transact) {
				System.out.println ("DatabaseState.stop_transaction: Exit: " + make_db_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			return;
		}

		// Set the transaction commit flag.

		public void set_transaction_commit (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("DatabaseState.set_transaction_commit: Enter: " + make_db_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			// Error if no transaction

			if (transact_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.set_transaction_commit: No transaction: " + make_db_id_message());
			}

			// Pass thru

			session_state.set_transaction_commit (f_commit);

			if (trace_transact) {
				System.out.println ("DatabaseState.set_transaction_commit: Exit: " + make_db_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			return;
		}

		// Add a resource that must be auto-closed during database disconnect.
		// This is typically used for iterators.
		// If an exception is thrown, the resource is NOT added.

		public void add_resource (AutoCloseable resource) {

			// Error if not connected

			if (conn_count == 0) {
				throw new DBUnavailableDatabaseException ("DatabaseState.add_resource: Not connected: " + make_db_id_message());
			}

			// Add to list

			auto_resources.add (resource);
			return;
		}

		// Remove a resource that must be auto-closed during database disconnect.
		// This is typically used for iterators.
		// This function should not throw any exception.
		// Performs no operation if the resource is not currently listed.

		public void remove_resource (AutoCloseable resource) {

			// Remove resource if the database is connected

			if (conn_count != 0) {
				auto_resources.remove (resource);
			}
			return;
		}

		//--- Subroutines ---

		// Make a string that identifies this database, for use in exception messages.

		public String make_db_id_message () {
			return "db_name = " + database_config.get_db_name()
				+ ", db_handle = " + database_config.get_db_handle();
		}

		// Make the error locus for this database, for use in exceptions.

		public MongoDBErrorLocus make_locus (MongoException mongo_exception) {
			return new MongoDBErrorLocus (
				get_session_state().get_host_state().get_host_config().get_host_handle(),
				database_config.get_db_handle(),
				null,
				mongo_exception);
		}

		// Make the MongoDatabase, if needed.
		// Note: Also checks if the database is connected, throws exception if not.

		private void make_mongo_database () {

			// If we don't have the MongoDB database, make it
			// Note: If mongo_database is non-null, then the database is connected.

			if (mongo_database == null) {
				if (conn_count == 0) {
					throw new DBUnavailableDatabaseException ("DatabaseState.make_mongo_database: Database is not connected: " + make_db_id_message());
				}
				MongoDatabase my_mongo_database;
				try {
					my_mongo_database = database_config.make_database (session_state.get_host_state().get_mongo_client());
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "DatabaseState.make_mongo_database: MongoDB exception: " + make_db_id_message(), e);
				}
				mongo_database = my_mongo_database;
			}
		
			return;
		}

	}




	//----- Sessions -----




	// SessionState - Holds current state for a session (a connection between a thread and a host).

	public static class SessionState {

		// Parent host state for this channel.

		private final HostState host_state;

		public HostState get_host_state () {
			return host_state;
		}

		// The MongoDB client session, or null if none.
		// Note: This can be null even if session_count is non-zero, if sessions are disabled.

		private ClientSession client_session;

		// The current number of connections.

		private int conn_count;

		// The current number of sessions; if nonzero then conn_count must also be nonzero.
		// Note: All sessions are multiplexed onto a single MongoDB session.

		private int session_count;

		// The current number of transactions, 0 or 1; if nonzero then session_count must also be nonzero,
		// and client_session must be non-null if tranactions are supported.

		private int transact_count;

		// Whether to commit (true) or abort (false) the transaction when it is stopped.

		private boolean transact_commit;

		// Database state list.

		private List<DatabaseState> database_states;

		public List<DatabaseState> get_database_states () {
			return Collections.unmodifiableList (database_states);
		}

		// Constructor.

		public SessionState (HostState host_state) {
			this.host_state = host_state;
			client_session = null;
			conn_count = 0;
			session_count = 0;
			transact_count = 0;
			transact_commit = false;
			database_states = new ArrayList<DatabaseState>();

			// Create the contained databases

			HostConfig host_config = this.host_state.get_host_config();
			List<DatabaseConfig> database_configs =  host_config.get_databases();
			for (DatabaseConfig database_config : database_configs) {
				database_states.add (new DatabaseState (this, database_config));
			}
		}

		// Connect the session.
		// If this throws an exception, then the connection has NOT occurred.

		public void conn_session () {

			if (trace_conn) {
				System.out.println ("SessionState.conn_session: Enter: " + make_sess_id_message() + ", conn_count = " + conn_count);
			}

			// If not currently connected ...

			if (conn_count == 0) {
			
				// Connect the host
				// Note we're still torn down, so not all exceptions here need to be caught

				try {
					host_state.conn_host();
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "SessionState.conn_session: MongoDB exception: " + make_sess_id_message(), e);
				}
			}

			// Count the connection

			++conn_count;

			if (trace_conn) {
				System.out.println ("SessionState.conn_session: Exit: " + make_sess_id_message() + ", conn_count = " + conn_count);
			}

			return;
		}

		// Disconnect the session.
		// If this throws an exception, then the disconnection HAS occurred.
		// Performs no operation if not currently connected.

		public void disc_session () {

			if (trace_conn) {
				System.out.println ("SessionState.disc_session: Enter: " + make_sess_id_message() + ", conn_count = " + conn_count);
			}
		
			// If currently connected ...

			if (conn_count != 0) {

				// If we're not disconnecting, just update the count

				if (conn_count != 1) {
					--conn_count;
				}

				// Otherwise, tear down the session

				else {
					teardown_session (true);
				}
			}

			if (trace_conn) {
				System.out.println ("SessionState.disc_session: Exit: " + make_sess_id_message() + ", conn_count = " + conn_count);
			}

			return;
		}

		// Release all resources.
		// If f_call_upstream is true, can make upstream calls to release upstream resources (should be false if called from upstream).
		// If this throws an exception, then the resources HAVE been released.

		public void teardown_session (boolean f_call_upstream) {

			// Accumulate exceptions to re-throw

			DBDriverException my_driver_exception = null;
			MongoException my_mongo_exception = null;
			Exception my_exception = null;

			// Tear down the databases

			for (DatabaseState database_state : database_states) {
				try {
					database_state.teardown_database (false);
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// If there is a transaction, stop it

			if (transact_count != 0) {
				transact_count = 0;

				try {
					if (transact_commit) {
						host_state.get_host_config().commit_transaction (client_session);
					}
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// If there is a client session, close it

			ClientSession my_client_session = client_session;
			client_session = null;
			session_count = 0;

			if (my_client_session != null) {
				try {
					my_client_session.close();
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// If connected, clear the count, and disconnect from host if desired

			if (conn_count != 0) {
				conn_count = 0;

				if (f_call_upstream) {
					try {
						host_state.disc_host();
					} catch (DBDriverException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
					} catch (MongoException e) {
						if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
					} catch (Exception e) {
						if (my_exception == null) {my_exception = e;}
					}
				}
			}

			// Propagate exceptions

			if (my_driver_exception != null) {
				throw new DBDriverException (my_driver_exception.get_locus(), "SessionState.teardown_session: Driver exception: " + make_sess_id_message(), my_driver_exception);
			}
			if (my_mongo_exception != null) {
				throw new DBDriverException (make_locus(my_mongo_exception), "SessionState.teardown_session: MongoDB exception: " + make_sess_id_message(), my_mongo_exception);
			}
			if (my_exception != null) {
				throw new DBException ("SessionState.teardown_session: Exception during tear-down: " + make_sess_id_message(), my_exception);
			}
		
			return;
		}

		// Release all resources related to client_session (but not the connection or databases).
		// If this throws an exception, then the resources HAVE been released.

		public void teardown_client_session () {

			// Accumulate exceptions to re-throw

			DBDriverException my_driver_exception = null;
			MongoException my_mongo_exception = null;
			Exception my_exception = null;

			// If there is a transaction, stop it

			if (transact_count != 0) {
				transact_count = 0;

				try {
					if (transact_commit) {
						host_state.get_host_config().commit_transaction (client_session);
					}
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// If there is a client session, close it

			ClientSession my_client_session = client_session;
			client_session = null;
			session_count = 0;

			if (my_client_session != null) {
				try {
					my_client_session.close();
				} catch (DBDriverException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_driver_exception = e;}
				} catch (MongoException e) {
					if (my_driver_exception == null && my_mongo_exception == null) {my_mongo_exception = e;}
				} catch (Exception e) {
					if (my_exception == null) {my_exception = e;}
				}
			}

			// Propagate exceptions

			if (my_driver_exception != null) {
				throw new DBDriverException (my_driver_exception.get_locus(), "SessionState.teardown_session: Driver exception: " + make_sess_id_message(), my_driver_exception);
			}
			if (my_mongo_exception != null) {
				throw new DBDriverException (make_locus(my_mongo_exception), "SessionState.teardown_session: MongoDB exception: " + make_sess_id_message(), my_mongo_exception);
			}
			if (my_exception != null) {
				throw new DBException ("SessionState.teardown_session: Exception during tear-down: " + make_sess_id_message(), my_exception);
			}
		
			return;
		}

		// Open a client session.
		// If this throws an exception, the client session is NOT opened.

		public void open_client_session () {

			if (trace_session) {
				System.out.println ("SessionState.open_client_session: Enter: " + make_sess_id_message() + ", session_count = " + session_count);
			}

			// Error if not connected

			if (conn_count == 0) {
				throw new InvariantViolationException ("SessionState.open_client_session: Not connected: " + make_sess_id_message());
			}

			// If no client session currently ...

			if (session_count == 0) {
			
				// Open the client session
				// Note not all exceptions here need to be caught

				ClientSession my_client_session;
				try {
					my_client_session = host_state.get_host_config().make_session (host_state.get_mongo_client());
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "SessionState.open_client_session: MongoDB exception: " + make_sess_id_message(), e);
				}
				client_session = my_client_session;
			}
		
			// Count the client session

			++session_count;

			if (trace_session) {
				System.out.println ("SessionState.open_client_session: Exit: " + make_sess_id_message() + ", session_count = " + session_count);
			}

			return;
		}

		// Close a client session.
		// If this throws an exception, the client session IS closed.
		// Performs no operation if the client session is not open.

		public void close_client_session () {

			if (trace_session) {
				System.out.println ("SessionState.close_client_session: Enter: " + make_sess_id_message() + ", session_count = " + session_count);
			}

			// Error if not connected

			if (conn_count == 0) {
				throw new InvariantViolationException ("SessionState.close_client_session: Not connected: " + make_sess_id_message());
			}
		
			// If currently have a session ...

			if (session_count != 0) {

				// If we're not closing client_session, just update the count

				if (session_count != 1) {
					--session_count;
				}

				// Otherwise, tear down client_session

				else {
					teardown_client_session ();
				}
			}

			if (trace_session) {
				System.out.println ("SessionState.close_client_session: Exit: " + make_sess_id_message() + ", session_count = " + session_count);
			}

			return;
		}

		// Get the current client session.
		// Note it is legal to call this function even if a session has not been opened.
		// Note the return value can be null even if a session has been opened, if sessions are disabled in the configuration.

		public ClientSession get_client_session () {

			// Error if not connected

			if (conn_count == 0) {
				throw new InvariantViolationException ("SessionState.get_client_session: Not connected: " + make_sess_id_message());
			}

			// If we have a session, but not a MongoDB session ...

			if (session_count != 0 && client_session == null) {
			
				// Open the client session
				// Note not all exceptions here need to be caught

				ClientSession my_client_session;
				try {
					my_client_session = host_state.get_host_config().make_session (host_state.get_mongo_client());
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "SessionState.get_client_session: MongoDB exception: " + make_sess_id_message(), e);
				}
				client_session = my_client_session;
			}

			// Return the client session

			return client_session;
		}

		// Class to close MongoDB session during exception processing.

		public class ErrorCloseSession implements AutoCloseable {

			// Flag indicates if close is desired.

			public boolean f_close;

			// Turn off close, to keep the session.

			public void keep () {
				f_close = false;
			}
		
			// Constructor

			public ErrorCloseSession () {
				f_close = true;
			}

			// Close session during close, if requested.

			@Override
			public void close () {
				if (f_close) {
					f_close = false;

					// If there is a client session, close it
					// Note that client_session can be null, depending on the configuration.
					// Note not all exceptions here need to be caught here.

					ClientSession my_client_session = client_session;
					client_session = null;

					if (my_client_session != null) {
						my_client_session.close();
					}
				}

				return;
			}
		}

		// Start a transaction.
		// If this throws an exception, the transaction is NOT started.

		public void start_transaction (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("SessionState.start_transaction: Enter: " + make_sess_id_message() + ", transact_count = " + transact_count);
			}

			// Error if no session

			if (session_count == 0) {
				throw new InvariantViolationException ("SessionState.start_transaction: No session: " + make_sess_id_message());
			}

			// Error if there is already a transaction

			if (transact_count != 0) {
				throw new InvariantViolationException ("SessionState.start_transaction: Transaction already started: " + make_sess_id_message());
			}

			// Get the current session

			ClientSession my_client_session = get_client_session();

			// Save the commit flag

			transact_commit = f_commit;

			// Start the transaction, close the MongoDB session if there is an exception

			try (
				ErrorCloseSession error_close_session = new ErrorCloseSession();
			) {
				host_state.get_host_config().start_transaction (my_client_session);
				error_close_session.keep();
			}
			catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "SessionState.start_transaction: MongoDB exception: " + make_sess_id_message(), e);
			}
		
			// Count the transaction

			++transact_count;

			if (trace_transact) {
				System.out.println ("SessionState.start_transaction: Exit: " + make_sess_id_message() + ", transact_count = " + transact_count);
			}

			return;
		}

		// Stop a transaction.
		// If this throws an exception, the transaction IS stopped.

		public void stop_transaction () {
			stop_transaction (transact_commit);
		}

		public void stop_transaction (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("SessionState.stop_transaction: Enter: " + make_sess_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			// Error if no session

			if (session_count == 0) {
				throw new InvariantViolationException ("SessionState.stop_transaction: No session: " + make_sess_id_message());
			}

			// If there is a transaction in progress ...

			if (transact_count != 0) {
			
				// Clear the counter

				transact_count = 0;

				// Commit the transaction, close the MongoDB session if there is an exception or abort

				try (
					ErrorCloseSession error_close_session = new ErrorCloseSession();
				) {
					if (f_commit) {
						host_state.get_host_config().commit_transaction (client_session);
						error_close_session.keep();
					}
					else {
						host_state.get_host_config().abort_transaction (client_session);
						error_close_session.keep();
					}
				}
				catch (MongoException e) {
					throw new DBDriverException (make_locus(e), "SessionState.stop_transaction: MongoDB exception: " + make_sess_id_message(), e);
				}
			}

			if (trace_transact) {
				System.out.println ("SessionState.stop_transaction: Exit: " + make_sess_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			return;
		}

		// Set the transaction commit flag.

		public void set_transaction_commit (boolean f_commit) {

			if (trace_transact) {
				System.out.println ("SessionState.set_transaction_commit: Enter: " + make_sess_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			// Error if no transaction

			if (transact_count == 0) {
				throw new InvariantViolationException ("SessionState.set_transaction_commit: No transaction: " + make_sess_id_message());
			}

			// Save the commit flag

			transact_commit = f_commit;

			if (trace_transact) {
				System.out.println ("SessionState.set_transaction_commit: Exit: " + make_sess_id_message() + ", transact_count = " + transact_count + ", f_commit = " + f_commit);
			}

			return;
		}

		//--- Subroutines ---

		// Make a string that identifies this session, for use in exception messages.

		public String make_sess_id_message () {
			return "host_handle = " + host_state.get_host_config().get_host_handle();
		}

		// Make the error locus for this session, for use in exceptions.

		public MongoDBErrorLocus make_locus (MongoException mongo_exception) {
			return new MongoDBErrorLocus (
				host_state.get_host_config().get_host_handle(),
				null,
				null,
				mongo_exception);
		}

	}




	//----- Mongo -----

	


	// Connections for MongoDB

	private MongoDBConnect mongo_connect;

	public MongoDBConnect get_mongo_connect () {
		return mongo_connect;
	}

	// Handle for the default database, or "" if none.

	private String default_db_handle;

	public String get_default_db_handle () {
		return default_db_handle;
	}

	// Set the default database.
	// Returns the old default database handle.

	public String set_default_db_handle (String new_default_db_handle) {
		String old_default_db_handle = default_db_handle;
		default_db_handle = new_default_db_handle;
		return old_default_db_handle;
	}

	// The session states, indexed by host handle.

	private LinkedHashMap<String, SessionState> session_states;

	public Collection<SessionState> get_session_states () {
		return Collections.unmodifiableCollection (session_states.values());
	}

	public SessionState get_session_state (String host_handle) {
		SessionState result = session_states.get (host_handle);
		if (result == null) {
			throw new DBUnknownHostException ("Unknown host: host_handle = " + host_handle);
		}
		return result;
	}

	// All the database states, indexed by database handle.

	private LinkedHashMap<String, DatabaseState> all_database_states;

	public Collection<DatabaseState> get_all_database_states () {
		return Collections.unmodifiableCollection (all_database_states.values());
	}

	public DatabaseState get_all_database_state (String db_handle) {
		DatabaseState result = all_database_states.get (db_handle);
		if (result == null) {
			throw new DBUnknownDatabaseException ("Unknown database: db_handle = " + db_handle);
		}
		return result;
	}

	// Constructor.

	public MongoDBContent (MongoDBConnect mongo_connect) {
		this.mongo_connect = mongo_connect;
		this.default_db_handle = mongo_connect.get_mongo_config().get_default_db_handle();
		this.session_states = new LinkedHashMap<String, SessionState>();
		this.all_database_states = new LinkedHashMap<String, DatabaseState>();

		// Loop over hosts

		for (HostState host_state : this.mongo_connect.get_host_states()) {
		
			// Make the corresponding session state

			SessionState session_state = new SessionState (host_state);

			// Add it to the map

			if (session_states.put (host_state.get_host_config().get_host_handle(), session_state) != null) {
				throw new InvariantViolationException ("MongoDBContent.MongoDBContent: Duplicate host handle: host_handle = " + host_state.get_host_config().get_host_handle());
			}

			// Loop over its databases, and add them to the map

			for (DatabaseState database_state : session_state.get_database_states()) {
				if (all_database_states.put (database_state.get_database_config().get_db_handle(), database_state) != null) {
					throw new InvariantViolationException ("MongoDBContent.MongoDBContent: Duplicate database handle: db_handle = " + database_state.get_database_config().get_db_handle());
				}
			}
		}
	}

	// Get the collection handle, given the database handle and the collection name.
	// If db_handle is null or empty, then the default database handle is used.

	public MongoDBCollHandle get_coll_handle (String db_handle, String coll_name) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}
	
		return get_all_database_state(my_db_handle).get_collection_state(coll_name);
	}

	// Connect to the given database, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the connection did NOT occur.
	// Multiple connections are allowed, and an equal number of disconnections
	// is required to terminate access to the database.
	// Returns the connection count after the connect.

	public int connect_database (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		int result = get_all_database_state(my_db_handle).conn_database();
		return result;
	}

	// Disconnect from the given database, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the disconnection DID occur, except for
	// DBUnknownDatabaseException and DBUnavailableDatabaseException.
	// Multiple connections are allowed, and an equal number of disconnections
	// is required to terminate access to the database.
	// Performs no operation if the database is currently not connected.
	// Returns the connection count before the disconnect.

	public int disconnect_database (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		int result = get_all_database_state(my_db_handle).disc_database();
		return result;
	}

	// Forcibly disconnect from the given database, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the disconnection DID occur.
	// This function immediately terminates all connections, sessions, transactions,
	// and iterators on the database.
	// Performs no operation if the database is currently not connected.

	public void abort_database (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		get_all_database_state(my_db_handle).teardown_database(true);
		return;
	}

	// Start a client session for a given database, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the session was NOT started.
	// Multiple starts are allowed, and an equal number of stops
	// is required to terminate the client session.
	// Performs no operation if configured to disable sessions.
	// Returns the session count after the open.

	public int start_session (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		int result = get_all_database_state(my_db_handle).open_client_session();
		return result;
	}

	// Stop a client session for a given database, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the session WAS stopped, except for
	// DBUnknownDatabaseException and DBUnavailableDatabaseException.
	// Multiple starts are allowed, and an equal number of stops
	// is required to terminate the client session.
	// Performs no operation if configured to disable sessions.
	// Performs no operation if the session has not been started.
	// Returns the session count before the close.

	public int stop_session (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		int result = get_all_database_state(my_db_handle).close_client_session();
		return result;
	}

	// Start a transaction for a given database, given the database handle and commit flag.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the transaction was NOT started.
	// Only one transaction at a time can be active.
	// Performs no operation if configured to disable transactions.

	public void start_transact (String db_handle, boolean f_commit) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		get_all_database_state(my_db_handle).start_transaction (f_commit);
		return;
	}

	// Stop a transaction for a given database, given the database handle and optional commit flag.
	// If db_handle is null or empty, then the default database handle is used.
	// If an exception is thrown, then the transaction WAS stopped, except for
	// DBUnknownDatabaseException and DBUnavailableDatabaseException.
	// Only one transaction at a time can be active.
	// Performs no operation if configured to disable transactions.

	public void stop_transact (String db_handle, boolean f_commit) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		get_all_database_state(my_db_handle).stop_transaction (f_commit);
		return;
	}

	public void stop_transact (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		get_all_database_state(my_db_handle).stop_transaction ();
		return;
	}

	// Set the transaction commit flag for a given database, given the database handle and commit flag.
	// If db_handle is null or empty, then the default database handle is used.
	// Only one transaction at a time can be active.
	// Throws an exception if no transaction is active.
	// The actual commit or abort occurs when the transaction is stopped (not immediately).

	public void set_transact_commit (String db_handle, boolean f_commit) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		get_all_database_state(my_db_handle).set_transaction_commit (f_commit);
		return;
	}

	// Return true if sessions are enabled, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.

	public boolean is_session_enabled (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		boolean result = get_all_database_state(my_db_handle).get_session_state().get_host_state().get_host_config().is_session_enabled();
		return result;
	}

	// Return true if transactions are enabled, given the database handle.
	// If db_handle is null or empty, then the default database handle is used.

	public boolean is_transaction_enabled (String db_handle) {
		String my_db_handle = db_handle;
		if (my_db_handle == null || my_db_handle.isEmpty()) {
			my_db_handle = default_db_handle;
		}

		boolean result = get_all_database_state(my_db_handle).get_session_state().get_host_state().get_host_config().is_transaction_enabled();
		return result;
	}

	// Release all resources and clear all data structures.

	@Override
	public void close () {

		// If not closed ...

		if (mongo_connect != null) {
		
			// Tear down all sessions

			for (SessionState session_state : session_states.values()) {
				session_state.teardown_session (true);
			}

			// Clear global data

			mongo_connect = null;
			default_db_handle = null;
			session_states = null;
			all_database_states = null;
		}

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MongoDBContent : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("MongoDBContent : Unrecognized subcommand : " + args[0]);
		return;

	}

}
