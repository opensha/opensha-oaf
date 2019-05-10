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
 * Holds a pending AAFS task in the MongoDB database.
 * Author: Michael Barall 03/15/2018.
 *
 * The collection "tasks" holds the queue of pending tasks.
 */
public class PendingTask implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this task.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.

	private ObjectId id;

	// Time that this task is scheduled to execute, in milliseconds since the epoch.
	// The collection is indexed on this field, so the task with the earliest
	// time can be obtained with a database query efficiently.
	// When beginning to execute a task, this field is set to zero, which guarantees
	// it will be seen again if the task is interrupted and restarted.

	private long exec_time;

	//----- Task information -----

	// Event ID that this task pertains to.
	// Tasks not referring to an event should put an empty string here (not null).
	// This can be used to find all queued tasks pertaining to an event.

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




	//----- Getters and setters -----

	private ObjectId get_id() {
		return id;
	}

	private void set_id (ObjectId id) {
		this.id = id;
	}

	public long get_exec_time() {
		return exec_time;
	}

	private void set_exec_time (long exec_time) {
		this.exec_time = exec_time;
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

		throw new IllegalArgumentException("PendingTask.set_details: Incorrect type of marshal writer");
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
		String str = "PendingTask\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\texec_time: " + exec_time + "\n"
			+ "\tevent_id: " + event_id + "\n"
			+ "\tsched_time: " + sched_time + "\n"
			+ "\tsubmit_time: " + submit_time + "\n"
			+ "\tsubmit_id: " + submit_id + "\n"
			+ "\topcode: " + opcode + "\n"
			+ "\tstage: " + stage + "\n"
			+ "\tdetails: " + get_details_description();
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




	/**
	 * is_restarted - Return true if this task has been restarted.
	 * Restarted means that a previous attempt was made to execute the task,
	 * but it failed before deleting the task from the queue.
	 */
	public boolean is_restarted () {
		return exec_time == 0L;
	}




	/**
	 * get_apparent_time - Return the apparent execution time of this command.
	 * Note: This is primarily for accelerated testing.
	 */
	public long get_apparent_time () {
		return (exec_time == 0L) ? sched_time : exec_time;
	}




	//----- MongoDB Java driver access -----




	//  // Get the collection.
	//  
	//  private static synchronized MongoCollection<Document> get_collection () {
	//  
	//  	// Get the collection
	//  
	//  	MongoDBCollRet coll_ret = MongoDBUtil.getCollection ("tasks");
	//  
	//  	// Create the indexes
	//  
	//  	if (coll_ret.f_new) {
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "event_id", "eventid");
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "exec_time", "extime");
	//  	}
	//  
	//  	// Return the collection
	//  
	//  	return coll_ret.collection;
	//  }




	// Get the collection handle.
	// If db_handle is null or empty, then use the current default database.

	private static MongoDBCollHandle get_coll_handle (String db_handle) {
		return MongoDBUtil.get_coll_handle (db_handle, "tasks");
	}




	// Make indexes for our collection.

	public static void make_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		//  // Make the indexes (original version)
		//  
		//  coll_handle.make_simple_index ("event_id", "eventid");
		//  coll_handle.make_simple_index ("exec_time", "extime");

		// Production code does queries which include an equality test on event_id
		// and/or a range test on exec_time, followed by an ascending sort on exec_time.
		// This index covers query and sort which does not reference event_id:

		coll_handle.make_simple_index ("exec_time", "extime");

		// This index covers query and sort which includes an equality test on event_id:
		// (It would work regardless of whether the exec_time index is ascending or descending.)

		coll_handle.make_compound_index_asc_asc ("event_id", "exec_time", "eventidtm");

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




	// Convert this object to a document.
	// If id is null, it is filled in with a newly allocated id.

	private Document to_bson_doc () {
	
		// Supply the id if needed

		if (id == null) {
			id = MongoDBUtil.make_object_id();
		}

		// Construct the document

		Document doc = new Document ("_id", id)
						.append ("exec_time"  , new Long(exec_time))
						.append ("event_id"   , event_id)
						.append ("sched_time" , new Long(sched_time))
						.append ("submit_time", new Long(submit_time))
						.append ("submit_id"  , submit_id)
						.append ("opcode"     , new Integer(opcode))
						.append ("stage"      , new Integer(stage))
						.append ("details"    , details);

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private PendingTask from_bson_doc (Document doc) {

		id          = MongoDBUtil.doc_get_object_id (doc, "_id"        );
		exec_time   = MongoDBUtil.doc_get_long      (doc, "exec_time"  );
		event_id    = MongoDBUtil.doc_get_string    (doc, "event_id"   );
		sched_time  = MongoDBUtil.doc_get_long      (doc, "sched_time" );
		submit_time = MongoDBUtil.doc_get_long      (doc, "submit_time");
		submit_id   = MongoDBUtil.doc_get_string    (doc, "submit_id"  );
		opcode      = MongoDBUtil.doc_get_int       (doc, "opcode"     );
		stage       = MongoDBUtil.doc_get_int       (doc, "stage"      );
		details     = MongoDBUtil.doc_get_string    (doc, "details"    );

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<PendingTask> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected PendingTask hook_convert (Document doc) {
			return (new PendingTask()).from_bson_doc (doc);
		}
	}




	// Make the natural sort for this collection.
	// The natural sort is in increasing order of execution time.

	private static Bson natural_sort () {
		return Sorts.ascending ("exec_time");
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// exec_time_lo = Minimum execution time, in milliseconds since the epoch.
	//                Can be 0L for no minimum.
	// exec_time_hi = Maximum execution time, in milliseconds since the epoch.
	//                Can be 0L for no maximum.
	// event_id = Event id. Can be null to return entries for all events.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long exec_time_lo, long exec_time_hi, String event_id) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by event_id

		if (event_id != null) {
			filters.add (Filters.eq ("event_id", event_id));
		}

		// Select entries with exec_time >= exec_time_lo

		if (exec_time_lo > 0L) {
			filters.add (Filters.gte ("exec_time", new Long(exec_time_lo)));
		}

		// Select entries with exec_time <= exec_time_hi

		if (exec_time_hi > 0L) {
			filters.add (Filters.lte ("exec_time", new Long(exec_time_hi)));
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




	// Make a cutoff filter for this collection.
	// cutoff_time = Cutoff time, in milliseconds since the epoch.
	// The filter selects tasks with exec_time <= cutoff_time.

	private static Bson cutoff_filter (long cutoff_time) {
		return Filters.lte ("exec_time", new Long(cutoff_time));
	}




	/**
	 * submit_task - Submit a task.
	 * @param event_id = Event associated with this task, or "" if none. Cannot be null.
	 * @param sched_time = Time at which task should execute, in milliseconds
	 *                     since the epoch. Must be positive.
	 * @param submit_time = Time at which the task is submitted, in milliseconds
	 *                      since the epoch. Must be positive.
	 * @param submit_id = Person or entity submitting this task. Cannot be empty or null.
	 * @param opcode = Operation code used to dispatch the task.
	 * @param stage = Stage number, user-defined, effectively an extension of the opcode.
	 * @param details = Further details of this task. Can be null if there are none.
	 * @return
	 * Returns the new entry.
	 */
	public static PendingTask submit_task (String event_id, long sched_time, long submit_time,
								String submit_id, int opcode, int stage, MarshalWriter details) {

		// Check conditions

		if (!( event_id != null
			&& sched_time > 0L
			&& submit_time > 0L
			&& submit_id != null && submit_id.length() > 0 )) {
			throw new IllegalArgumentException("PendingTask.submit_task: Invalid task parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Construct the pending task object

		PendingTask ptask = new PendingTask();
		ptask.set_id (null);
		ptask.set_exec_time (sched_time);
		ptask.set_event_id (event_id);
		ptask.set_sched_time (sched_time);
		ptask.set_submit_time (submit_time);
		ptask.set_submit_id (submit_id);
		ptask.set_opcode (opcode);
		ptask.set_stage (stage);
		ptask.set_details (details);

		// Call MongoDB to store into database

		coll_handle.insertOne (ptask.to_bson_doc());
		
		return ptask;
	}




	/**
	 * store_task - Store a task into the database.
	 * This is primarily for restoring from backup.
	 */
	public static PendingTask store_task (PendingTask ptask) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Call MongoDB to store into database

		coll_handle.insertOne (ptask.to_bson_doc());
		
		return ptask;
	}




	/**
	 * get_all_tasks_unsorted - Get a list of all pending tasks, without sorting.
	 * This is primarily for testing and monitoring.
	 *
	 * Current usage: Test only.
	 */
	public static List<PendingTask> get_all_tasks_unsorted () {
		ArrayList<PendingTask> tasks = new ArrayList<PendingTask>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		MongoCursor<Document> cursor = coll_handle.find_iterator();
		try (
			MyRecordIterator iter = new MyRecordIterator (cursor, coll_handle);
		){
			// Dump into the list

			while (iter.hasNext()) {
				tasks.add (iter.next());
			}
		}

		return tasks;
	}




	/**
	 * get_all_tasks - Get a list of all pending tasks, sorted by execution time.
	 * This is primarily for testing and monitoring.
	 *
	 * Current usage: Test only.
	 */
	public static List<PendingTask> get_all_tasks () {
		ArrayList<PendingTask> tasks = new ArrayList<PendingTask>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		MongoCursor<Document> cursor = coll_handle.find_iterator (null, natural_sort());
		try (
			MyRecordIterator iter = new MyRecordIterator (cursor, coll_handle);
		){
			// Dump into the list

			while (iter.hasNext()) {
				tasks.add (iter.next());
			}
		}

		return tasks;
	}




	/**
	 * fetch_all_tasks - Iterate all pending tasks, sorted by execution time.
	 * This is primarily for testing and monitoring.
	 *
	 * Current usage: Test only.
	 */
	public static RecordIterator<PendingTask> fetch_all_tasks () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		MongoCursor<Document> cursor = coll_handle.find_iterator (null, natural_sort());
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * get_task_entry_range - Get a range of task entries, sorted by execution time.
	 * @param exec_time_lo = Minimum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no minimum.
	 * @param exec_time_hi = Maximum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 *
	 * Current usage: Production.
	 * Production code calls this with non-null event_id.
	 * There may or may not be filters on exec_time.  These filters could easily be done
	 * by the caller, if there were an advantage to not supporting them here.
	 * The production code does not rely on the results being sorted.
	 */
	public static List<PendingTask> get_task_entry_range (long exec_time_lo, long exec_time_hi, String event_id) {
		ArrayList<PendingTask> tasks = new ArrayList<PendingTask>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (exec_time_lo, exec_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort());
		try (
			MyRecordIterator iter = new MyRecordIterator (cursor, coll_handle);
		){
			// Dump into the list

			while (iter.hasNext()) {
				tasks.add (iter.next());
			}
		}

		return tasks;
	}




	/**
	 * fetch_task_entry_range - Iterate a range of task entries, sorted by execution time.
	 * @param exec_time_lo = Minimum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no minimum.
	 * @param exec_time_hi = Maximum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 *
	 * Current usage: Test only.
	 */
	public static RecordIterator<PendingTask> fetch_task_entry_range (long exec_time_lo, long exec_time_hi, String event_id) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (exec_time_lo, exec_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort());
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * get_first_task_entry - Get the first in a range of task entries.
	 * @param exec_time_lo = Minimum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no minimum.
	 * @param exec_time_hi = Maximum execution time, in milliseconds since the epoch.
	 *                       Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * Returns the matching task entry with the smallest exec_time (first to execute),
	 * or null if there is no matching task entry.
	 *
	 * Current usage: Production.
	 * Production code calls this with null event_id.
	 * Production code requires that the result be sorted (so it returns the first to execute).
	 */
	public static PendingTask get_first_task_entry (long exec_time_lo, long exec_time_hi, String event_id) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the document

		Bson filter = natural_filter (exec_time_lo, exec_time_hi, event_id);
		Document doc = coll_handle.find_first (filter, natural_sort());

		// Convert to task

		if (doc == null) {
			return null;
		}

		return (new PendingTask()).from_bson_doc (doc);
	}




	/**
	 * get_first_task - Get the first task, that is, the task with smallest execution time.
	 * This is primarily for testing and monitoring.
	 *
	 * Current usage: Test only.
	 */
	public static PendingTask get_first_task () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the document

		Document doc = coll_handle.find_first (null, natural_sort());

		// Convert to task

		if (doc == null) {
			return null;
		}

		return (new PendingTask()).from_bson_doc (doc);
	}




	/**
	 * get_first_ready_task - Get the first ready task, according to execution time.
	 * @param cutoff_time = Cutoff time, in milliseconds since the epoch.
	 * Only tasks with exec_time <= cutoff_time are considered.
	 * Return is null if there are no such tasks.
	 *
	 * Current usage: Production.
	 * Production code requires that the result be sorted (so it returns the first to execute).
	 */
	public static PendingTask get_first_ready_task (long cutoff_time) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the document

		Document doc = coll_handle.find_first (cutoff_filter (cutoff_time), natural_sort());

		// Convert to task

		if (doc == null) {
			return null;
		}

		return (new PendingTask()).from_bson_doc (doc);
	}




	/**
	 * activate_first_ready_task - Get and activate the first ready task, according to execution time.
	 * @param cutoff_time = Cutoff time, in milliseconds since the epoch.
	 * Only tasks with exec_time <= cutoff_time are considered.
	 * Return is null if there are no such tasks.
	 * The task is marked active by setting exec_time = 0 in the database.
	 *
	 * Current usage: Production.
	 * Production code requires that the result be sorted (so it returns the first to execute).
	 */
	public static PendingTask activate_first_ready_task (long cutoff_time) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: exec_time <= cutoff_time

		Bson filter = cutoff_filter (cutoff_time);

		// Update: Set exec_time to 0L

		Bson update = Updates.set ("exec_time", new Long(0L));

		// Options: sort by exec_time, return original document value

		FindOneAndUpdateOptions options = (new FindOneAndUpdateOptions()).sort(natural_sort()).returnDocument(ReturnDocument.BEFORE);

		// Get the document

		Document doc = coll_handle.findOneAndUpdate (filter, update, options);

		// Convert to task

		if (doc == null) {
			return null;
		}

		return (new PendingTask()).from_bson_doc (doc);
	}




	/**
	 * stage_task - Begin a new stage of a task.
	 * @param ptask = Existing pending task to stage.
	 * @param exec_time = Time at which task should execute, in milliseconds
	 *                    since the epoch. Must be positive.
	 * @param event_id = New event ID, or null to leave it unchanged.
	 * @param stage = Stage number, user-defined, effectively an extension of the opcode.
	 * @return
	 */
	public static void stage_task (PendingTask ptask, long exec_time, int stage, String event_id) {

		// Check conditions

		if (!( ptask != null && ptask.get_id() != null
			&& exec_time > 0L )) {
			throw new IllegalArgumentException("PendingTask.stage_task: Invalid task parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == ptask.id

		Bson filter = id_filter (ptask.get_id());

		// Construct the update operations: Set exec_time and stage

		ArrayList<Bson> updates = new ArrayList<Bson>();

		updates.add (Updates.set ("exec_time", new Long(exec_time)));
		updates.add (Updates.set ("stage", new Integer(stage)));

		// Update event ID if desired

		if (event_id != null) {
			updates.add (Updates.set ("event_id", event_id));
		}

		Bson update = Updates.combine (updates);

		// Run the update

		coll_handle.updateOne (filter, update);
		
		return;
	}




	/**
	 * delete_task - Delete a task.
	 * @param ptask = Existing pending task to delete.
	 * @return
	 */
	public static void delete_task (PendingTask ptask) {

		// Check conditions

		if (!( ptask != null && ptask.get_id() != null )) {
			throw new IllegalArgumentException("PendingTask.delete_task: Invalid task parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == ptask.id

		Bson filter = id_filter (ptask.get_id());

		// Run the delete

		coll_handle.deleteOne (filter);
		
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 8001;

	private static final String M_VERSION_NAME = "PendingTask";

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		String sid = id.toHexString();
		writer.marshalString      ("id"         , sid        );
		writer.marshalLong        ("exec_time"  , exec_time  );
		writer.marshalString      ("event_id"   , event_id   );
		writer.marshalLong        ("sched_time" , sched_time );
		writer.marshalLong        ("submit_time", submit_time);
		writer.marshalString      ("submit_id"  , submit_id  );
		writer.marshalInt         ("opcode"     , opcode     );
		writer.marshalInt         ("stage"      , stage      );
		writer.marshalJsonString  ("details"    , details    );
//		writer.marshalLongArray   ("details_l"  , details_l  );
//		writer.marshalDoubleArray ("details_d"  , details_d  );
//		writer.marshalStringArray ("details_s"  , details_s  );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		String sid;
		sid         = reader.unmarshalString      ("id"         );
		exec_time   = reader.unmarshalLong        ("exec_time"  );
		event_id    = reader.unmarshalString      ("event_id"   );
		sched_time  = reader.unmarshalLong        ("sched_time" );
		submit_time = reader.unmarshalLong        ("submit_time");
		submit_id   = reader.unmarshalString      ("submit_id"  );
		opcode      = reader.unmarshalInt         ("opcode"     );
		stage       = reader.unmarshalInt         ("stage"      );
		details     = reader.unmarshalJsonString  ("details"    );
//		details_l   = reader.unmarshalLongArray   ("details_l"  );
//		details_d   = reader.unmarshalDoubleArray ("details_d"  );
//		details_s   = reader.unmarshalStringArray ("details_s"  );
		id = new ObjectId(sid);

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

	public PendingTask unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}


}
