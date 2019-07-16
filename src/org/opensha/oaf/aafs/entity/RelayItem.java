package org.opensha.oaf.aafs.entity;

import java.util.List;

import org.bson.types.ObjectId;

import org.opensha.oaf.aafs.MongoDBUtil;
import org.opensha.oaf.aafs.RecordKey;
import org.opensha.oaf.aafs.RecordPayload;
import org.opensha.oaf.aafs.RecordIterator;

import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


import java.util.ArrayList;
import java.util.Comparator;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;

import org.opensha.oaf.aafs.DBCorruptException;
import org.opensha.oaf.aafs.RecordIteratorMongo;
import org.opensha.oaf.aafs.RecordChangeIteratorMongo;
import org.opensha.oaf.aafs.MongoDBCollRet;
import org.opensha.oaf.aafs.MongoDBCollHandle;



/**
 * Holds an item relayed between the two servers.
 * Author: Michael Barall 05/11/2019.
 *
 * The collection "relay" holds the current set of relay items.
 *
 * The set of relay items is a dynamic set that contains state information
 * which must be synchronized between the two servers.  It only contains
 * current state;  older items are replaced or deleted.
 */
public class RelayItem extends DBEntity implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this relay item.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.

	private ObjectId id;

	// Time stamp for this relay item, in milliseconds since the epoch.
	// When a relay item is replaced, the new item must have a later (greater) time stamp.
	// (Equal time stamps are permitted, provided that a tie-breaker is satisfied.)
	// The time stamp (plus tie-breaker) is used to discard duplicate or delayed items.

	private long relay_time;

	// Identifier for this relay item.
	// Each relay item in the database must have a different identifier, so that the
	// database always contains (only) the most recent state for the identifier.
	// Must be non-null.
	// Each kind of relay item must have a separate identifier namespace, e.g., the
	// first character of the identifier can specify the kind of relay item.

	private String relay_id;

	// Origin stamp for this relay item.
	// This is set when a relay item is written into the database, and indicates its source.
	// The change stream is configured to only pass items with relay_stamp >= 0L.
	// This prevents round-tripping of relay items when each server monitors the other.

	private long relay_stamp;

	//----- Item information -----

	// Details of this relay item.
	// Any additional information needed is stored as a JSON string containing marshaled data.
	// If none, this should be an empty string (not null).

	private String details;




	//----- Getters and setters -----

	private ObjectId get_id() {
		return id;
	}

	private void set_id (ObjectId id) {
		this.id = id;
	}

	public long get_relay_time() {
		return relay_time;
	}

	private void set_relay_time (long relay_time) {
		this.relay_time = relay_time;
	}

	public String get_relay_id() {
		return relay_id;
	}

	private void set_relay_id (String relay_id) {
		this.relay_id = relay_id;
	}

	public long get_relay_stamp() {
		return relay_stamp;
	}

	private void set_relay_stamp (long relay_stamp) {
		this.relay_stamp = relay_stamp;
	}




	/**
	 * get_details - Get a reader for the details.
	 */
	public MarshalReader get_details() {
		Object json_source;
		if (details == null) {
			json_source = null;
		}
		else if (details.equals("")) {
			json_source = null;
		}
		else {
			json_source = details;
		}
		return new MarshalImpJsonReader (json_source);
	}


	/**
	 * set_details - Set details from the marshaled data, can be null for none.
	 */
	private void set_details (MarshalWriter writer) {

		if (writer == null) {
			details = "";
			return;
		}

		if (writer instanceof MarshalImpJsonWriter) {
			MarshalImpJsonWriter w = (MarshalImpJsonWriter)writer;
			if (w.check_write_complete()) {
				details = w.get_json_string();
			} else {
				details = "";
			}
			return;
		}

		if (writer instanceof RecordPayload) {
			RecordPayload p = (RecordPayload)writer;
			details = p.get_json_string();
			return;
		}

		throw new IllegalArgumentException("RelayItem.set_details: Incorrect type of marshal writer");
	}


	/**
	 * begin_details - Get a writer to use for marhaling details.
	 */
	public static MarshalWriter begin_details() {
		return new MarshalImpJsonWriter ();
	}


	/**
	 * get_details_as_payload - Get a payload containing the details.
	 */
	RecordPayload get_details_as_payload() {
		return new RecordPayload (details);
	}


	/**
	 * get_details_description - Get a string describing the details.
	 */
	private String get_details_description () {
		return ((details == null) ? "null" : ("len = " + details.length()));
	}


	/**
	 * dump_details - Dump details into a string, for trouble-shooting.
	 */
	public String dump_details () {
		return ((details == null) ? "null" : details);
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "RelayItem\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\trelay_time: " + relay_time + "\n"
			+ "\trelay_id: " + relay_id + "\n"
			+ "\trelay_stamp: " + relay_stamp + "\n"
			+ "\tdetails: " + get_details_description();
		return str;
	}




	// dumpString - Dump to string.

	public String dumpString() {
		String str = "RelayItem\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\trelay_time: " + relay_time + "\n"
			+ "\trelay_id: " + relay_id + "\n"
			+ "\trelay_stamp: " + relay_stamp + "\n"
			+ "\tdetails: " + dump_details();
		return str;
	}




	/**
	 * get_record_key - Get the record key for this pending task.
	 * Each pending task is assigned a unique record key when the task is submitted.
	 * The key remains the same when the task is activated or when a new stage begins.
	 */
	public RecordKey get_record_key () {
		return new RecordKey(id);
	}




	// Compare two relay items.
	// Returns 0 if relit1 is identical to relit2.
	// Returns < 0 if relit1 is before (earlier than) relit2.
	// Returns > 0 if relit1 is after (later than) relit2.
	// The comparison is primarily based on relay_time.
	// If the value of relay_time is equal, then relay_id and details are used as tie-breakers.
	// Note that relay_stamp and the object id do not enter into the comparison.

	public static int compare (RelayItem relit1, RelayItem relit2) {
		int result = Long.compare (relit1.relay_time, relit2.relay_time);
		if (result == 0) {
			result = relit1.relay_id.compareTo (relit2.relay_id);
			if (result == 0) {
				result = relit1.details.compareTo (relit2.details);
			}
		}
		return result;
	}




	// A comparator that can be used for sorting lists of relay items in ascending order (earliest first).

	public static class AscendingComparator implements Comparator<RelayItem>, java.io.Serializable {
	
		// Compare two items

		@Override
		public int compare (RelayItem relit1, RelayItem relit2) {
			return RelayItem.compare (relit1, relit2);
		}
	}




	// A comparator that can be used for sorting lists of relay items in descending order (most recent first).

	public static class DescendingComparator implements Comparator<RelayItem>, java.io.Serializable {
	
		// Compare two items

		@Override
		public int compare (RelayItem relit1, RelayItem relit2) {
			return RelayItem.compare (relit2, relit1);
		}
	}




	//----- MongoDB Java driver access -----




	// Get the collection handle.
	// If db_handle is null or empty, then use the current default database.

	private static MongoDBCollHandle get_coll_handle (String db_handle) {
		return MongoDBUtil.get_coll_handle (db_handle, "relay");
	}




	// Make indexes for our collection.

	public static void make_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		//  // Make the indexes (original version)
		//  
		//  coll_handle.make_simple_index ("relay_id", "riid");
		//  coll_handle.make_simple_index ("relay_time", "ritime");

		// Production code does queries which include an equality test or in-test on relay_id,
		// or a range test on relay_time, followed by a descending sort on relay_time.
		// This index covers query and sort which does not reference relay_id:

		coll_handle.make_simple_index ("relay_time", "ritime");

		// This index covers query which includes a test on relay_id:
		// (Such queries produce few results, so we let MongoDB do the sort in-memory.)
		
		coll_handle.make_unique_index ("relay_id", "riid");

		return;
	}




	// Drop all indexes our collection.

	public static void drop_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Drop the indexes

		coll_handle.drop_indexes ();

		return;
	}




	// Drop our collection.

	public static void drop_collection () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Drop the collection

		coll_handle.drop ();

		return;
	}




	// Test if our collection exists.
	// Return true if the collection exists

	public static boolean collection_exists () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Test if the collection exists

		return coll_handle.collection_exists ();
	}




	// Convert this object to a document.
	// If id is null, it is filled in with a newly allocated id.

	private Document to_bson_doc () {
	
		// Supply the id if needed

		if (id == null) {
			id = MongoDBUtil.make_object_id();
		}

		// Construct the document

		Document doc = new Document ("_id", id)
						.append ("relay_time" , new Long(relay_time))
						.append ("relay_id"   , relay_id)
						.append ("relay_stamp", new Long(relay_stamp))
						.append ("details"    , details);

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private RelayItem from_bson_doc (Document doc) {

		id          = MongoDBUtil.doc_get_object_id (doc, "_id"        );
		relay_time  = MongoDBUtil.doc_get_long      (doc, "relay_time" );
		relay_id    = MongoDBUtil.doc_get_string    (doc, "relay_id"   );
		relay_stamp = MongoDBUtil.doc_get_long      (doc, "relay_stamp");
		details     = MongoDBUtil.doc_get_string    (doc, "details"    );

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<RelayItem> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected RelayItem hook_convert (Document doc) {
			return (new RelayItem()).from_bson_doc (doc);
		}
	}




	// Our change stream iterator class.

	private static class MyChangeStreamIterator extends RecordChangeIteratorMongo<RelayItem> {

		// Constructor passes thru the cursor.

		public MyChangeStreamIterator (MongoCursor<ChangeStreamDocument<Document>> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected RelayItem hook_convert (Document doc, OperationType optype) {
			return (new RelayItem()).from_bson_doc (doc);
		}
	}




	// Sort order options.

	public static final int ASCENDING = 1;
	public static final int DESCENDING = -1;
	public static final int UNSORTED = 0;




	// Make the natural sort for this collection.
	// The natural sort is in ascending or descending order of relay_time.
	// If sort_order is UNSORTED, then return null, which means unsorted.

	private static Bson natural_sort (int sort_order) {
		switch (sort_order) {
		case UNSORTED:
			return null;
		case DESCENDING:
			return Sorts.descending ("relay_time");
		case ASCENDING:
			return Sorts.ascending ("relay_time");
		}
		throw new IllegalArgumentException ("RelayItem.natural_sort: Invalid sort order: " + sort_order);
	}




	// Make the natural sort for this collection, for a given query on relay ids.
	// The natural sort is in ascending or descending order of relay_time.
	// Except, if the query is for a single relay id, then return null
	// because no sort is needed (because there is only one entry with a given id).

	private static Bson natural_sort (int sort_order, String[] relay_id) {
		if (relay_id != null) {
			if (relay_id.length == 1) {
				return null;
			}
		}
		return natural_sort (sort_order);
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// relay_time_lo = Minimum execution time, in milliseconds since the epoch.
	//                 Can be 0L for no minimum.
	// relay_time_hi = Maximum execution time, in milliseconds since the epoch.
	//                 Can be 0L for no maximum.
	// relay_id = Relay item id list. Can be null or empty to return entries for all ids.
	//            If specified, return entries associated with any of the given ids.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long relay_time_lo, long relay_time_hi, String... relay_id) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by relay_id

		if (relay_id != null) {
			if (relay_id.length > 0) {
				if (relay_id.length == 1) {
					filters.add (Filters.eq ("relay_id", relay_id[0]));
				} else {
					filters.add (Filters.in ("relay_id", relay_id));
				}
			}
		}

		// Select entries with relay_time >= relay_time_lo

		if (relay_time_lo > 0L) {
			filters.add (Filters.gte ("relay_time", new Long(relay_time_lo)));
		}

		// Select entries with relay_time <= relay_time_hi

		if (relay_time_hi > 0L) {
			filters.add (Filters.lte ("relay_time", new Long(relay_time_hi)));
		}

		// Return combination of filters

		if (filters.size() == 0) {
			return null;
		}
		if (filters.size() == 1) {
			return filters.get(0);
		}
		return Filters.and (filters);
	}




	// submit_relay_item - Submit a relay item.
	// Parameters:
	//  relit = Relay item to write into database.
	//  f_force = True to force item to be written.
	//  relay_stamp = Origin stamp to indicate source.
	// This function first tests if there is an existing item with the same relay_id.
	// If there is no existing item, then:
	//  - relit.id is filled with a new object id (overwriting relit.id).
	//  - relit.relay_stamp is filled with the supplied relay_stamp (overwriting it).
	//  - relit is written to the database.
	//  - The function returns 2.
	// If there is an existing item, then:
	//  - relit.id is filled with the object id of the existing item (overwriting relit.id).
	//  - relit.relay_stamp is filled with the supplied relay_stamp (overwriting it).
	//  - If f_force is true, then relit.relay_time is adjusted, if necessary, so it is
	//    larger then the value of relay_time in the existing item.
	//  - If relit equals the existing item, then return 0.
	//  - If relit is earlier than the existing item, then return -1.
	//  - Otherwise (relit is later then the existing item):
	//    - Update the existing item with the value of relit.
	//    - The function returns 1.
	// Note that a return value > 0 means a new item was inserted or an existing item was updated.
	// Note: This is not an atomic operation, because MongoDB has no atomic operation that performs
	// this function.  Therefore, it should be called only if there is a single thread that writes
	// to this collection, or within a transaction.

	public static int submit_relay_item (RelayItem relit, boolean f_force, long relay_stamp) {

		// Set the origin stamp

		relit.set_relay_stamp (relay_stamp);

		// Check if it can be inserted into the database

		int chkres = check_relay_item (relit, f_force);

		// Insert the relay item, if possible

		if (chkres > 0) {
			insert_relay_item (relit);
		}

		return chkres;
	}




	// submit_relay_item - Submit a relay item.
	// Parameters:
	//  relay_id = Relay item id, or "" if none. Cannot be null.
	//  relay_time = Relay item time stamp, in milliseconds since the epoch. Must be positive.
	//  details = Further details of this relay item. Can be null if there are none.
	//  f_force = True to force item to be written.
	//  relay_stamp = Origin stamp to indicate source.
	// If an item is written or updated, then the new or updated item is returned.
	// If an item is not written, then null is returned.
	// If f_force is true, then the time stamp is increased, if necessary,
	//  to ensure that the item is written.  Otherwise, if there is an existing item
	//  that is equal or later to the proposed new item, then nothing is written.

	public static RelayItem submit_relay_item (String relay_id, long relay_time, MarshalWriter details, boolean f_force, long relay_stamp) {

		// Build the relay item

		RelayItem relit = build_relay_item (relay_id, relay_time, details);

		// Do the insert

		if (submit_relay_item (relit, f_force, relay_stamp) <= 0) {
			relit = null;		// item not written
		}
		
		return relit;
	}




	// build_relay_item - Build a relay item.
	// Parameters:
	//  relay_id = Relay item id, or "" if none. Cannot be null.
	//  relay_time = Relay item time stamp, in milliseconds since the epoch. Must be positive.
	//  details = Further details of this relay item. Can be null if there are none.
	// Returns a newly created RelayItem, with null object id.
	// Note: relay_stamp is initialized to 0L (it receives its final value when written to the database).

	public static RelayItem build_relay_item (String relay_id, long relay_time, MarshalWriter details) {

		// Check conditions

		if (!( relay_id != null
			&& relay_time > 0L )) {
			throw new IllegalArgumentException("RelayItem.submit_relay_item: Invalid relay item parameters");
		}

		// Construct the relay item object

		RelayItem relit = new RelayItem();
		relit.set_id (null);
		relit.set_relay_time (relay_time);
		relit.set_relay_id (relay_id);
		relit.set_relay_stamp (0L);
		relit.set_details (details);
		
		return relit;
	}




	// check_relay_item - Check if a relay item can be inserted into the database.
	// Parameters:
	//  relit = Relay item to check.
	//  f_force = True to force item to be insertable.
	// This function tests if there is an existing item with the same relay_id.
	// If there is no existing item, then:
	//  - relit.id is set to null (overwriting relit.id).
	//  - The function returns 2.
	// If there is an existing item, then:
	//  - relit.id is filled with the object id of the existing item (overwriting relit.id).
	//  - If f_force is true, then relit.relay_time is adjusted, if necessary, so it is
	//    larger then the value of relay_time in the existing item.
	//  - If relit equals the existing item, then return 0.
	//  - If relit is earlier than the existing item, then return -1.
	//  - Otherwise (relit is later then the existing item), return 1.
	// Note that a return value > 0 means the relay item can be inserted into the database.

	public static int check_relay_item (RelayItem relit, boolean f_force) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Search for an existing item with the same relay_id

		Bson filter = natural_filter (0L, 0L, relit.get_relay_id());
		Document doc = coll_handle.find_first (filter);

		// If not found ...

		if (doc == null) {

			// Erase existing object id, if any

			relit.set_id (null);

			// Return that item can be inserted

			return 2;
		}

		// Get existing relay item

		RelayItem existing = (new RelayItem()).from_bson_doc (doc);

		// Copy the object id

		relit.set_id (existing.get_id());

		// If force, increase relay_time if necessary

		if (f_force) {
			if (relit.get_relay_time() <= existing.get_relay_time()) {
				relit.set_relay_time (existing.get_relay_time() + 1L);
			}
		}

		// Otherwise, compare to existing item

		else {

			int cmp = compare (relit, existing);

			// If identical, return identical

			if (cmp == 0) {
				return 0;
			}

			// If earlier than existing item, return that

			if (cmp < 0) {
				return -1;
			}
		}

		// Return later than existing item

		return 1;
	}




	// insert_relay_item - Insert a relay item into the database.
	// Parameters:
	//  relit = Relay item to write into database.
	// If relit.id is null, then there must not be any existing item with the same relay id,
	//  and then relit.id is filled with a new object id and the item is inserted into the database.
	// If relit.id is non-null, then it must be the object id of an existing relay item,
	//  and the existing relay item is replaced with this one.
	// Note: Before calling this function, you must call check_relay_item and get a return value > 0.
	// Note: The caller must ensure that any existing item is not modified after the call to check_relay_item.
	//  This can be done by limit writes to a single thread, or though the use of locks.

	public static void insert_relay_item (RelayItem relit) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Search for an existing item with the same relay_id

		Bson filter = natural_filter (0L, 0L, relit.get_relay_id());
		Document doc = coll_handle.find_first (filter);

		// If not replacing an existing item ...

		if (relit.get_id() == null) {

			// Call MongoDB to store into database

			coll_handle.insertOne (relit.to_bson_doc());

			return;
		}

		// Update existing item

		// Filter: id == relit.id

		filter = id_filter (relit.get_id());

		// Construct the update operations: Set relay_time, relay_stamp, and details

		ArrayList<Bson> updates = new ArrayList<Bson>();

		updates.add (Updates.set ("relay_time", new Long(relit.relay_time)));
		updates.add (Updates.set ("relay_stamp", new Long(relit.relay_stamp)));
		updates.add (Updates.set ("details", relit.details));

		// Update item id if desired (this would send the item id thru the changestream)

		//updates.add (Updates.set ("relay_id", relit.relay_id));

		Bson update = Updates.combine (updates);

		// Run the update

		coll_handle.updateOne (filter, update);

		return;
	}




	/**
	 * store_relay_item - Store a relay item into the database.
	 * This is primarily for restoring from backup.
	 */
	public static RelayItem store_relay_item (RelayItem relit) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Call MongoDB to store into database

		coll_handle.insertOne (relit.to_bson_doc());
		
		return relit;
	}




	/**
	 * store_entity - Store this entity into the database.
	 * This is primarily for restoring from backup.
	 */
	@Override
	public RelayItem store_entity () {
		store_relay_item (this);
		return this;
	}




	/**
	 * get_relay_item_for_key - Get the relay item with the given key.
	 * @param key = Record key. Cannot be null or empty.
	 * Returns the relay item, or null if not found.
	 *
	 * Current usage: Production.
	 */
	public static RelayItem get_relay_item_for_key (RecordKey key) {

		if (!( key != null && key.getId() != null )) {
			throw new IllegalArgumentException("RelayItem.get_relay_item_for_key: Missing or empty record key");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == key.getId()

		Bson filter = id_filter (key.getId());

		// Get the document

		Document doc = coll_handle.find_first (filter);

		// Convert to timeline entry

		if (doc == null) {
			return null;
		}

		return (new RelayItem()).from_bson_doc (doc);
	}




	/**
	 * get_relay_item_range - Get a range of relay items, sorted by relay item time.
	 * @param sort_order = DESCENDING to sort in descending order by time (most recent first),
	 *                     ASCENDING to sort in ascending order by time (oldest first),
	 *                     UNSORTED to return unsorted results.
	 * @param relay_time_lo = Minimum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no minimum.
	 * @param relay_time_hi = Maximum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no maximum.
	 * @param relay_id = List of relay item ids. Can be null or empty to return items for all ids.
	 *
	 * Expected usage: Test only.
	 */
	public static List<RelayItem> get_relay_item_range (int sort_order, long relay_time_lo, long relay_time_hi, String... relay_id) {
		ArrayList<RelayItem> relits = new ArrayList<RelayItem>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (relay_time_lo, relay_time_hi, relay_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order, relay_id));
		try (
			MyRecordIterator iter = new MyRecordIterator (cursor, coll_handle);
		){
			// Dump into the list

			while (iter.hasNext()) {
				relits.add (iter.next());
			}
		}

		return relits;
	}




	/**
	 * fetch_relay_item_range - Iterate a range of task entries, sorted by relay item time.
	 * @param sort_order = DESCENDING to sort in descending order by time (most recent first),
	 *                     ASCENDING to sort in ascending order by time (oldest first),
	 *                     UNSORTED to return unsorted results.
	 * @param relay_time_lo = Minimum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no minimum.
	 * @param relay_time_hi = Maximum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no maximum.
	 * @param relay_id = List of relay item ids. Can be null or empty to return items for all ids.
	 *
	 * Expected usage: Production.
	 * Production code calls this in the form fetch_relay_item_range (sort_order, relay_time_lo, relay_time_hi).
	 */
	public static RecordIterator<RelayItem> fetch_relay_item_range (int sort_order, long relay_time_lo, long relay_time_hi, String... relay_id) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (relay_time_lo, relay_time_hi, relay_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order, relay_id));
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * get_first_relay_item - Get the first in a range of relay items.
	 * @param sort_order = DESCENDING to sort in descending order by time (most recent first),
	 *                     ASCENDING to sort in ascending order by time (oldest first),
	 *                     UNSORTED to return unsorted results.
	 * @param relay_time_lo = Minimum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no minimum.
	 * @param relay_time_hi = Maximum relay item time, in milliseconds since the epoch.
	 *                        Can be 0L for no maximum.
	 * @param relay_id = List of relay item ids. Can be null or empty to return items for all ids.
	 * Returns the matching relay item with the largest relay_time (most recent),
	 * or null if there is no matching relay item.
	 *
	 * Expected usage: Production.
	 * Production code calls this in the form get_first_relay_item (sort_order, 0L, 0L, relay_id).
	 * Production code requires that the result be sorted (so it returns the most recent among all given ids).
	 */
	public static RelayItem get_first_relay_item (int sort_order, long relay_time_lo, long relay_time_hi, String... relay_id) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the document

		Bson filter = natural_filter (relay_time_lo, relay_time_hi, relay_id);
		Document doc = coll_handle.find_first (filter, natural_sort (sort_order, relay_id));

		// Convert to task

		if (doc == null) {
			return null;
		}

		return (new RelayItem()).from_bson_doc (doc);
	}




	/**
	 * delete_relay_item - Delete a relay item.
	 * @param relit = Existing pending task to delete.
	 * @return
	 */
	public static void delete_relay_item (RelayItem relit) {

		// Check conditions

		if (!( relit != null && relit.get_id() != null )) {
			throw new IllegalArgumentException("RelayItem.delete_task: Invalid task parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == relit.id

		Bson filter = id_filter (relit.get_id());

		// Run the delete

		coll_handle.deleteOne (filter);
		
		return;
	}




	/**
	 * watch_relay_item_changes - Iterate changes in the relay item collection.
	 *
	 * Expected usage: Production.
	 */
	public static RecordIterator<RelayItem> watch_relay_item_changes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter to watch relay_stamp >= 0L, in the fullDocument subdocument of the change stream document

		long stamp_cutoff = 0L;
		//Bson filter = Filters.gte ("fullDocument.relay_stamp", new Long(stamp_cutoff));

		// The "not" form of the filter also passes changes that don't contain a full document, such as delete and invalidate

		Bson filter = Filters.not (Filters.lt ("fullDocument.relay_stamp", new Long(stamp_cutoff)));

		// Get the cursor and iterator

		MongoCursor<ChangeStreamDocument<Document>> cursor = coll_handle.watch (filter);
		return new MyChangeStreamIterator (cursor, coll_handle);
	}

	


	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 55001;
	private static final int MARSHAL_VER_2 = 55002;

	private static final String M_VERSION_NAME = "RelayItem";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_RELAY_ITEM;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		String sid;

		switch (ver) {

		case MARSHAL_VER_1:

			sid = id.toHexString();
			writer.marshalString      ("id"         , sid        );
			writer.marshalLong        ("relay_time" , relay_time );
			writer.marshalString      ("relay_id"   , relay_id   );
			writer.marshalString      ("details"    , details    );

			break;

		case MARSHAL_VER_2:

			sid = id.toHexString();
			writer.marshalString      ("id"         , sid        );
			writer.marshalLong        ("relay_time" , relay_time );
			writer.marshalString      ("relay_id"   , relay_id   );
			writer.marshalLong        ("relay_stamp", relay_stamp);
			writer.marshalString      ("details"    , details    );

			break;
		}
	
		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		String sid;

		switch (ver) {

		case MARSHAL_VER_1:

			sid         = reader.unmarshalString      ("id"         );
			relay_time  = reader.unmarshalLong        ("relay_time" );
			relay_id    = reader.unmarshalString      ("relay_id"   );
			details     = reader.unmarshalString      ("details"    );
			id = new ObjectId(sid);

			relay_stamp = 0L;

			break;

		case MARSHAL_VER_2:

			sid         = reader.unmarshalString      ("id"         );
			relay_time  = reader.unmarshalLong        ("relay_time" );
			relay_id    = reader.unmarshalString      ("relay_id"   );
			relay_stamp = reader.unmarshalLong        ("relay_stamp");
			details     = reader.unmarshalString      ("details"    );
			id = new ObjectId(sid);

			break;
		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public RelayItem unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, RelayItem obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static RelayItem unmarshal_poly (MarshalReader reader, String name) {
		RelayItem result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("RelayItem.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_RELAY_ITEM:
			result = new RelayItem();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
