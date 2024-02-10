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

import org.opensha.oaf.aafs.DBCorruptException;
import org.opensha.oaf.aafs.RecordIteratorMongo;
import org.opensha.oaf.aafs.MongoDBCollRet;
import org.opensha.oaf.aafs.MongoDBCollHandle;




/**
 * Holds an AAFS log entry in the MongoDB database.
 * Author: Michael Barall 03/15/2018.
 *
 * The collection "log" holds the log entries.
 */
public class LogEntry extends DBEntity implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this log entry.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.
	// This is set to the same value as the id of the task that generated the log entry.

	private ObjectId id;

	// Time that this log entry was created, in milliseconds since the epoch.
	// The collection is indexed on this field, so log entries in a given
	// time span can be obtained with a database query efficiently.

	private long log_time;

	//----- Task information -----

	// Event ID that this task pertains to.
	// Entries not referring to an event should put an empty string here (not null).
	// This can be used to find all log entries pertaining to an event.

	private String event_id;

	// Time this task was originally scheduled to execute, in milliseconds since the epoch.

	private long sched_time;

	// Time this task was submitted, in milliseconds since the epoch.

	private long submit_time;

	// Person or entity that submitted the task.

	private String submit_id;

	// Operation code for this task.

	private int opcode;

	// Stage number, assigned by the user.
	// This can be used for a sequence of commands with the same PendingTask.

	private int stage;

	// Details of this task.
	// Any additional information needed is stored as a JSON string containing marshaled data.
	// If none, this should be an empty string (not null).

	private String details;

//	// Details of this task.
//	// Any additional information needed is stored as marshaled data.
//	// Each array should have at least one element.
//
//	private long[] details_l;
//	private double[] details_d;
//	private String[] details_s;

	//----- Result information -----

	// Result code for this task.

	private int rescode;

	// Results for this task.
	// If none, this should be an empty string (not null).

	private String results;




	//----- Getters and setters -----

	private ObjectId get_id() {
		return id;
	}

	private void set_id (ObjectId id) {
		this.id = id;
	}

	public long get_log_time() {
		return log_time;
	}

	private void set_log_time (long log_time) {
		this.log_time = log_time;
	}

	public String get_event_id() {
		return event_id;
	}

	private void set_event_id (String event_id) {
		this.event_id = event_id;
	}

	public long get_sched_time() {
		return sched_time;
	}

	private void set_sched_time (long sched_time) {
		this.sched_time = sched_time;
	}

	public long get_submit_time() {
		return submit_time;
	}

	private void set_submit_time (long submit_time) {
		this.submit_time = submit_time;
	}

	public String get_submit_id() {
		return submit_id;
	}

	private void set_submit_id (String submit_id) {
		this.submit_id = submit_id;
	}

	public int get_opcode() {
		return opcode;
	}

	private void set_opcode (int opcode) {
		this.opcode = opcode;
	}

	public int get_stage() {
		return stage;
	}

	private void set_stage (int stage) {
		this.stage = stage;
	}

	public int get_rescode() {
		return rescode;
	}

	private void set_rescode (int rescode) {
		this.rescode = rescode;
	}

	public String get_results() {
		return results;
	}

	private void set_results (String results) {
		this.results = results;
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

		throw new IllegalArgumentException("LogEntry.set_details: Incorrect type of marshal writer");
	}


	/**
	 * begin_details - Get a writer to use for marhaling details.
	 */
	public static MarshalWriter begin_details() {
		return new MarshalImpJsonWriter ();
	}


	/**
	 * get_details_as_payload - Get a writer containing the details.
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




//	/**
//	 * get_details - Get a reader for the details.
//	 */
//    public MarshalReader get_details() {
//        return new MarshalImpArray (details_l, details_d, details_s);
//    }
//
//
//	/**
//	 * set_details - Set details from the marshaled data, can be null for none.
//	 */
//    private void set_details (MarshalWriter writer) {
//
//		if (writer == null) {
//			details_l = new long[1];
//			details_l[0] = 0L;
//			details_d = new double[1];
//			details_d[0] = 0.0;
//			details_s = new String[1];
//			details_s[0] = "";
//			return;
//		}
//
//		if (writer instanceof MarshalImpArray) {
//			MarshalImpArray w = (MarshalImpArray)writer;
//			if (w.check_write_complete()) {
//				details_l = w.get_long_store();
//				details_d = w.get_double_store();
//				details_s = w.get_string_store();
//			} else {
//				details_l = new long[1];
//				details_l[0] = 0L;
//				details_d = new double[1];
//				details_d[0] = 0.0;
//				details_s = new String[1];
//				details_s[0] = "";
//			}
//			return;
//		}
//
//		if (writer instanceof RecordPayload) {
//			RecordPayload p = (RecordPayload)writer;
//			details_l = p.get_long_store();
//			details_d = p.get_double_store();
//			details_s = p.get_string_store();
//			return;
//		}
//
//		throw new IllegalArgumentException("PendingTask.set_details: Incorrect type of marshal writer");
//    }
//
//
//	/**
//	 * begin_details - Get a writer to use for marhaling details.
//	 */
//    public static MarshalWriter begin_details() {
//        return new MarshalImpArray ();
//    }
//
//
//	/**
//	 * get_details_as_payload - Get a writer containing the details.
//	 */
//    RecordPayload get_details_as_payload() {
//        return new RecordPayload (details_l, details_d, details_s);
//    }
//
//
//	/**
//	 * get_details_description - Get a string describing the details.
//	 */
//	private String get_details_description () {
//		return "llen = " + details_l.length + ", dlen = " + details_d.length + ", slen = " + details_s.length;
//	}
//
//
//	///**
//	// * These getters are for use by other package members for direct copy of the details.
//	// */
//    //long[] get_details_l() {
//    //    return details_l;
//    //}
//    //double[] get_details_d() {
//    //    return details_d;
//    //}
//    //String[] get_details_s() {
//    //    return details_s;
//    //}




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "LogEntry\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\tlog_time: " + log_time + "\n"
			+ "\tevent_id: " + event_id + "\n"
			+ "\tsched_time: " + sched_time + "\n"
			+ "\tsubmit_time: " + submit_time + "\n"
			+ "\tsubmit_id: " + submit_id + "\n"
			+ "\topcode: " + opcode + "\n"
			+ "\tstage: " + stage + "\n"
			+ "\tdetails: " + get_details_description() + "\n"
			+ "\trescode: " + rescode + "\n"
			+ "\tresults: " + results;
		return str;
	}




	/**
	 * get_record_key - Get the record key for this log entry.
	 */
	public RecordKey get_record_key () {
		return new RecordKey(id);
	}




	/**
	 * set_record_key - Set the record key for this log entry.
	 * @param key = Record key. Can be null.
	 */
	private void set_record_key (RecordKey key) {
		if (key == null) {
			id = null;
		} else {
			id = key.getId();
		}
		return;
	}




	//----- MongoDB Java driver access -----




	//  // Get the collection.
	//  
	//  private static synchronized MongoCollection<Document> get_collection () {
	//  
	//  	// Get the collection
	//  
	//  	MongoDBCollRet coll_ret = MongoDBUtil.getCollection ("log");
	//  
	//  	// Create the indexes
	//  
	//  	if (coll_ret.f_new) {
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "event_id", "logevid");
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "log_time", "logtime");
	//  	}
	//  
	//  	// Return the collection
	//  
	//  	return coll_ret.collection;
	//  }




	// Get the collection handle.
	// If db_handle is null or empty, then use the current default database.

	private static MongoDBCollHandle get_coll_handle (String db_handle) {
		return MongoDBUtil.get_coll_handle (db_handle, "log");
	}




	// Make indexes for our collection.

	public static void make_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		//  // Make the indexes
		//  
		//  coll_handle.make_simple_index ("event_id", "logevid");
		//  coll_handle.make_simple_index ("log_time", "logtime");

		// Production code does no queries or sorts, and therefore no indexes are needed.
		// However, we include one index which would allow querying and sorting by log_time:

		coll_handle.make_simple_index ("log_time", "logtime");

		// Note: If no indexes are created, then it is necessary to create the collection explicitly, like this:

		//coll_handle.createCollection();

		return;
	}




	// Drop all indexes our collection.

	public static void drop_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Drop the collection

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
						.append ("log_time"   , Long.valueOf(log_time))
						.append ("event_id"   , event_id)
						.append ("sched_time" , Long.valueOf(sched_time))
						.append ("submit_time", Long.valueOf(submit_time))
						.append ("submit_id"  , submit_id)
						.append ("opcode"     , Integer.valueOf(opcode))
						.append ("stage"      , Integer.valueOf(stage))
						.append ("details"    , details)
						.append ("rescode"    , Integer.valueOf(rescode))
						.append ("results"    , results);

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private LogEntry from_bson_doc (Document doc) {

		id          = MongoDBUtil.doc_get_object_id (doc, "_id"        );
		log_time    = MongoDBUtil.doc_get_long      (doc, "log_time"   );
		event_id    = MongoDBUtil.doc_get_string    (doc, "event_id"   );
		sched_time  = MongoDBUtil.doc_get_long      (doc, "sched_time" );
		submit_time = MongoDBUtil.doc_get_long      (doc, "submit_time");
		submit_id   = MongoDBUtil.doc_get_string    (doc, "submit_id"  );
		opcode      = MongoDBUtil.doc_get_int       (doc, "opcode"     );
		stage       = MongoDBUtil.doc_get_int       (doc, "stage"      );
		details     = MongoDBUtil.doc_get_string    (doc, "details"    );
		rescode     = MongoDBUtil.doc_get_int       (doc, "rescode"    );
		results     = MongoDBUtil.doc_get_string    (doc, "results"    );

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<LogEntry> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected LogEntry hook_convert (Document doc) {
			return (new LogEntry()).from_bson_doc (doc);
		}
	}




	//  // Make the natural sort for this collection.
	//  // The natural sort is in decreasing order of log time (most recent first).
	//  
	//  private static Bson natural_sort () {
	//  	return Sorts.descending ("log_time");
	//  }




	// Sort order options.

	public static final int ASCENDING = 1;
	public static final int DESCENDING = -1;
	public static final int UNSORTED = 0;

	private static final int DEFAULT_SORT = DESCENDING;




	// Make the natural sort for this collection.
	// The natural sort is in ascending or descending order of log_time.
	// If sort_order is UNSORTED, then return null, which means unsorted.

	private static Bson natural_sort (int sort_order) {
		switch (sort_order) {
		case UNSORTED:
			return null;
		case DESCENDING:
			return Sorts.descending ("log_time");
		case ASCENDING:
			return Sorts.ascending ("log_time");
		}
		throw new IllegalArgumentException ("LogEntry.natural_sort: Invalid sort order: " + sort_order);
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// @param log_time_lo = Minimum log time, in milliseconds since the epoch.
	//                      Can be 0L for no minimum.
	// @param log_time_hi = Maximum log time, in milliseconds since the epoch.
	//                      Can be 0L for no maximum.
	// event_id = Event id. Can be null to return entries for all events.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long log_time_lo, long log_time_hi, String event_id) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by event_id

		if (event_id != null) {
			filters.add (Filters.eq ("event_id", event_id));
		}

		// Select entries with log_time >= log_time_lo

		if (log_time_lo > 0L) {
			filters.add (Filters.gte ("log_time", Long.valueOf(log_time_lo)));
		}

		// Select entries with log_time <= log_time_hi

		if (log_time_hi > 0L) {
			filters.add (Filters.lte ("log_time", Long.valueOf(log_time_hi)));
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




	/**
	 * submit_log_entry - Submit a log entry.
	 * @param key = Record key associated with this task. Can be null to assign a new one.
	 * @param log_time = Time of this log entry, in milliseconds
	 *                   since the epoch. Must be positive.
	 * @param event_id = Event associated with this task, or "" if none. Cannot be null.
	 * @param sched_time = Time at which task should execute, in milliseconds
	 *                     since the epoch. Must be positive.
	 * @param submit_time = Time at which the task is submitted, in milliseconds
	 *                      since the epoch. Must be positive.
	 * @param submit_id = Person or entity submitting this task. Cannot be empty or null.
	 * @param opcode = Operation code used to dispatch the task.
	 * @param stage = Stage number, user-defined, effectively an extension of the opcode.
	 * @param details = Further details of this task. Can be null if there are none.
	 * @param rescode = Result code.
	 * @param results = Further results of this task, or "" if none. Cannot be null.
	 * @return
	 * Returns the new entry.
	 */
	public static LogEntry submit_log_entry (RecordKey key, long log_time, String event_id,
			long sched_time, long submit_time, String submit_id, int opcode, int stage,
			MarshalWriter details, int rescode, String results) {

		// Check conditions

		if (!( log_time > 0L
			&& event_id != null
			&& sched_time > 0L
			&& submit_time > 0L
			&& submit_id != null && submit_id.length() > 0
			&& results != null )) {
			throw new IllegalArgumentException("LogEntry.submit_log_entry: Invalid log parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Construct the log entry object

		LogEntry lentry = new LogEntry();
		lentry.set_record_key (key);
		lentry.set_log_time (log_time);
		lentry.set_event_id (event_id);
		lentry.set_sched_time (sched_time);
		lentry.set_submit_time (submit_time);
		lentry.set_submit_id (submit_id);
		lentry.set_opcode (opcode);
		lentry.set_stage (stage);
		lentry.set_details (details);
		lentry.set_rescode (rescode);
		lentry.set_results (results);

		// Call MongoDB to store into database

		coll_handle.insertOne (lentry.to_bson_doc());
		
		return lentry;
	}




	/**
	 * submit_log_entry - Submit a log entry.
	 * @param ptask = Pending task record associated with this task. Cannot be null.
	 * @param log_time = Time of this log entry, in milliseconds
	 *                     since the epoch. Must be positive.
	 * @param rescode = Result code.
	 * @param results = Further results of this task, or "" if none. Cannot be null.
	 * @return
	 * Other log parameters are copied from ptask.
	 * Returns the new entry.
	 */
	public static LogEntry submit_log_entry (PendingTask ptask, long log_time, int rescode, String results) {

		// Check conditions

		if (!( ptask != null
			&& log_time > 0L
			&& results != null )) {
			throw new IllegalArgumentException("LogEntry.submit_log_entry: Invalid log parameters");
		}

		// Submit the log entry

		LogEntry lentry = submit_log_entry (
			ptask.get_record_key(),
			log_time,
			ptask.get_event_id(),
			ptask.get_sched_time(),
			ptask.get_submit_time(),
			ptask.get_submit_id(),
			ptask.get_opcode(),
			ptask.get_stage(),
			ptask.get_details_as_payload(),
			rescode,
			results);
		
		return lentry;
	}




	/**
	 * store_log_entry - Store a log entry into the database.
	 * This is primarily for restoring from backup.
	 */
	public static LogEntry store_log_entry (LogEntry lentry) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Call MongoDB to store into database

		coll_handle.insertOne (lentry.to_bson_doc());
		
		return lentry;
	}




	/**
	 * store_entity - Store this entity into the database.
	 * This is primarily for restoring from backup.
	 */
	@Override
	public LogEntry store_entity () {
		store_log_entry (this);
		return this;
	}




	/**
	 * get_log_entry_for_key - Get the log entry with the given key.
	 * @param key = Record key. Cannot be null or empty.
	 * Returns the log entry, or null if not found.
	 *
	 * Current usage: Production.
	 */
	public static LogEntry get_log_entry_for_key (RecordKey key) {

		if (!( key != null && key.getId() != null )) {
			throw new IllegalArgumentException("LogEntry.get_log_entry_for_key: Missing or empty record key");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == key.getId()

		Bson filter = id_filter (key.getId());

		// Get the document

		Document doc = coll_handle.find_first (filter);

		// Convert to log entry

		if (doc == null) {
			return null;
		}

		return (new LogEntry()).from_bson_doc (doc);
	}




	/**
	 * get_log_entry_range - Get a range of log entries, reverse-sorted by log time.
	 * @param log_time_lo = Minimum log time, in milliseconds since the epoch.
	 *                      Can be 0L for no minimum.
	 * @param log_time_hi = Maximum log time, in milliseconds since the epoch.
	 *                      Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param sort_order = DESCENDING to sort in descending order by log_time (most recent first),
	 *                     ASCENDING to sort in ascending order by log_time (oldest first),
	 *                     UNSORTED to return unsorted results.  Defaults to DESCENDING.
	 *
	 * Current usage: Test only.
	 */
	public static List<LogEntry> get_log_entry_range (long log_time_lo, long log_time_hi, String event_id) {
		return get_log_entry_range (log_time_lo, log_time_hi, event_id, DEFAULT_SORT);
	}

	public static List<LogEntry> get_log_entry_range (long log_time_lo, long log_time_hi, String event_id, int sort_order) {
		ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (log_time_lo, log_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order));
		try(
			MyRecordIterator iter = new MyRecordIterator (cursor, coll_handle);
		){
			// Dump into the list

			while (iter.hasNext()) {
				entries.add (iter.next());
			}
		}

		return entries;
	}




	/**
	 * fetch_log_entry_range - Iterate a range of log entries, reverse-sorted by log time.
	 * @param log_time_lo = Minimum log time, in milliseconds since the epoch.
	 *                      Can be 0L for no minimum.
	 * @param log_time_hi = Maximum log time, in milliseconds since the epoch.
	 *                      Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param sort_order = DESCENDING to sort in descending order by log_time (most recent first),
	 *                     ASCENDING to sort in ascending order by log_time (oldest first),
	 *                     UNSORTED to return unsorted results.  Defaults to DESCENDING.
	 *
	 * Current usage: Backup only, with no filters and UNSORTED.
	 */
	public static RecordIterator<LogEntry> fetch_log_entry_range (long log_time_lo, long log_time_hi, String event_id) {
		return fetch_log_entry_range (log_time_lo, log_time_hi, event_id, DEFAULT_SORT);
	}

	public static RecordIterator<LogEntry> fetch_log_entry_range (long log_time_lo, long log_time_hi, String event_id, int sort_order) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (log_time_lo, log_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order));
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * delete_log_entry - Delete a log entry.
	 * @param entry = Existing log entry to delete.
	 * @return
	 */
	public static void delete_log_entry (LogEntry entry) {

		// Check conditions

		if (!( entry != null && entry.get_id() != null )) {
			throw new IllegalArgumentException("LogEntry.delete_log_entry: Invalid parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == entry.id

		Bson filter = id_filter (entry.get_id());

		// Run the delete

		coll_handle.deleteOne (filter);
		
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 9001;

	private static final String M_VERSION_NAME = "LogEntry";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_LOG_ENTRY;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		MongoDBUtil.marshal_object_id (writer, "id", id);
		writer.marshalLong        ("log_time"   , log_time   );
		writer.marshalString      ("event_id"   , event_id   );
		writer.marshalLong        ("sched_time" , sched_time );
		writer.marshalLong        ("submit_time", submit_time);
		writer.marshalString      ("submit_id"  , submit_id  );
		writer.marshalInt         ("opcode"     , opcode     );
		writer.marshalInt         ("stage"      , stage      );
		writer.marshalString      ("details"    , details    );
//		writer.marshalLongArray   ("details_l"  , details_l  );
//		writer.marshalDoubleArray ("details_d"  , details_d  );
//		writer.marshalStringArray ("details_s"  , details_s  );
		writer.marshalInt         ("rescode"    , rescode    );
		writer.marshalString      ("results"    , results    );
	
		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		id          = MongoDBUtil.unmarshal_object_id (reader, "id");
		log_time    = reader.unmarshalLong        ("log_time"   );
		event_id    = reader.unmarshalString      ("event_id"   );
		sched_time  = reader.unmarshalLong        ("sched_time" );
		submit_time = reader.unmarshalLong        ("submit_time");
		submit_id   = reader.unmarshalString      ("submit_id"  );
		opcode      = reader.unmarshalInt         ("opcode"     );
		stage       = reader.unmarshalInt         ("stage"      );
		details     = reader.unmarshalString      ("details"    );
//		details_l   = reader.unmarshalLongArray   ("details_l"  );
//		details_d   = reader.unmarshalDoubleArray ("details_d"  );
//		details_s   = reader.unmarshalStringArray ("details_s"  );
		rescode     = reader.unmarshalInt         ("rescode"    );
		results     = reader.unmarshalString      ("results"    );

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

	public LogEntry unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, LogEntry obj) {

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

	public static LogEntry unmarshal_poly (MarshalReader reader, String name) {
		LogEntry result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("LogEntry.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_LOG_ENTRY:
			result = new LogEntry();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
