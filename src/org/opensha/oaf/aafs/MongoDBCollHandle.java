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
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.UpdateDescription;
//import com.mongodb.client.model.changestream.ChangeStreamLevel;	// removed in driver version 4.0
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


/**
 * This interface is a handle for a MongoDB collection.
 * Author: Michael Barall.
 *
 * This interface provides access to (a subset of) the functions provided by MongoCollection<Document>.
 * Its benefits are that it knows context information (e.g., database and client session), implements
 * access control, centralizes the handling of exceptions, and provides some common combinations of operations.
 */
public interface MongoDBCollHandle {

	//--- MongoDB functions ---

	// Test if a collection exists.
	// Returns true if the collection exists, false if not.

	public boolean collection_exists ();

	// Create a collection.
	// Parameters:
	//  options = Options, or null if none, defaults to null.
	// Returns true if collection was created, false if it already existed.

	public boolean createCollection (CreateCollectionOptions options);

	public default boolean createCollection () {
		return createCollection (null);
	}

	// Create an index.
	// Parameters:
	//  keys = Index keys (constructed by Indexes), cannot be null.
	//  options = Options, or null if none, defaults to null.
	// Returns the index name.

	public String createIndex (Bson keys, IndexOptions options);

	public default String createIndex (Bson keys) {
		return createIndex (keys, null);
	}

	// Make an index on one field, with the given name.

	public default void make_simple_index (String field, String name) {
		createIndex (Indexes.ascending (field), (new IndexOptions()).name (name));
		return;
	}

	// Make a compound index on two fields, with the given name.
	// The first field is ascending, and the second field is ascending.

	public default void make_compound_index_asc_asc (String field1, String field2, String name) {
		createIndex (Indexes.compoundIndex (Indexes.ascending (field1), Indexes.ascending (field2)), (new IndexOptions()).name (name));
		return;
	}

	// Make a compound index on two fields, with the given name.
	// The first field is ascending, and the second field is descending.

	public default void make_compound_index_asc_desc (String field1, String field2, String name) {
		createIndex (Indexes.compoundIndex (Indexes.ascending (field1), Indexes.descending (field2)), (new IndexOptions()).name (name));
		return;
	}

	// Make a unique index on one field, with the given name.

	public default void make_unique_index (String field, String name) {
		createIndex (Indexes.ascending (field), (new IndexOptions()).name (name).unique (true));
		return;
	}

	// Delete one document.
	// Parameters:
	//  filter = Filter to use for query (constructed by Filters), cannot be null.
	//  options = Options, or null if none, defaults to null.
	// Returns delete result object.
	// Note: This function cannot do a sort.  Use findOneAndDelete if a sort is needed.

	public DeleteResult deleteOne (Bson filter, DeleteOptions options);

	public default DeleteResult deleteOne (Bson filter) {
		return deleteOne (filter, null);
	}

	// Drop a collection.

	public void drop ();

	// Drop all indexes for a collection.

	public void drop_indexes ();

	// Find documents, and return the first matching document, or null if no matching document.
	// Parameters:
	//  filter = Filter to use for query (constructed by Filters), or null if no filter, defaults to null.
	//  sort = Sort to use for query (constructed by Sorts), or null if no sort, defaults to null

	public Document find_first (Bson filter, Bson sort);

	public default Document find_first (Bson filter) {
		return find_first (filter, null);
	}

	public default Document find_first () {
		return find_first (null, null);
	}

	// Find documents, and return an iterator over matching documents.
	// Parameters:
	//  filter = Filter to use for query (constructed by Filters), or null if no filter, defaults to null.
	//  sort = Sort to use for query (constructed by Sorts), or null if no sort, defaults to null

	public MongoCursor<Document> find_iterator (Bson filter, Bson sort);

	public default MongoCursor<Document> find_iterator (Bson filter) {
		return find_iterator (filter, null);
	}

	public default MongoCursor<Document> find_iterator () {
		return find_iterator (null, null);
	}

	// Find one document, and delete it.
	// Parameters:
	//  filter = Filter to use for query (constructed by Filters), cannot be null.
	//  options = Options, or null if none, defaults to null.
	// Returns the document that was delete, or null if no matching document is found.
	// Note: A sort can be included within options.

	public Document findOneAndDelete (Bson filter, FindOneAndDeleteOptions options);

	public default Document findOneAndDelete (Bson filter) {
		return findOneAndDelete (filter, null);
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

	public Document findOneAndReplace (Bson filter, Document replacement, FindOneAndReplaceOptions options);

	public default Document findOneAndReplace (Bson filter, Document replacement) {
		return findOneAndReplace (filter, replacement, null);
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

	public Document findOneAndUpdate (Bson filter, Bson update, FindOneAndUpdateOptions options);

	public default Document findOneAndUpdate (Bson filter, Bson update) {
		return findOneAndUpdate (filter, update, null);
	}

	// Insert one document into the collection.
	// Parameters:
	//  document = Document to insert.  If it does not contain an id, then an id is created.
	//  options = Options for the insert operation, or null if none, defaults to null.

	public void insertOne (Document document, InsertOneOptions options);

	public default void insertOne (Document document) {
		insertOne (document, null);
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

	public UpdateResult replaceOne (Bson filter, Document replacement, ReplaceOptions options);

	public default UpdateResult replaceOne (Bson filter, Document replacement) {
		return replaceOne (filter, replacement, null);
	}

	// Update one document.
	// Parameters:
	//  filter = Filter to use for query (constructed by Filters), cannot be null.
	//  update = Update operation to perform (constructed by Updates), cannot be null.
	//  options = Options, or null if none, defaults to null.
	// Returns update result object.
	// Note: An upsert can be included within options.
	// Note: This function cannot do a sort.  Use findOneAndUpdate if a sort is needed.

	public UpdateResult updateOne (Bson filter, Bson update, UpdateOptions options);

	public default UpdateResult updateOne (Bson filter, Bson update) {
		return updateOne (filter, update, null);
	}

	// Open a change stream iterator on the collection.
	// Parameters:
	//  filter = Filter to use for change stream (constructed by Filters), or null if no filter, defaults to null.
	// The change stream is configured with the full document option set.
	// Note: This function is only supported on replica sets.

	public MongoCursor<ChangeStreamDocument<Document>> watch (Bson filter);

	public default MongoCursor<ChangeStreamDocument<Document>> watch () {
		return watch (null);
	}

	//--- Handle functions ---

	// Get the name of the collection.

	public String get_coll_name ();

	// Get the name of the database.

	public String get_db_name ();

	// Get the handle of the database.

	public String get_db_handle ();

	// Get the handle of the host.

	public String get_host_handle ();

	// Get the MongoDB collection.

	public MongoCollection<Document> get_mongo_collection ();

	// Add a resource that must be auto-closed during database disconnect.
	// This is typically used for iterators.
	// If an exception is thrown, the resource is NOT added.
	// The following notes apply to resource.close():
	// - It must release all resources and call remove_resource().
	// - It must do so even if it throws an exception.
	// - It must be idempotent; second and subsequent calls should do nothing.
	// - When called, the database is still connected.
	// - It may not change content state (connect or disconnect databases, etc.).

	public void add_resource (AutoCloseable resource);

	// Remove a resource that must be auto-closed during database disconnect.
	// This is typically used for iterators.
	// This function should not throw any exception.
	// Performs no operation if the resource is not currently listed.

	public void remove_resource (AutoCloseable resource);

}
