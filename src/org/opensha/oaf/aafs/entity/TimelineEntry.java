package org.opensha.oaf.aafs.entity;

import java.util.Arrays;
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




/**
 * Holds an AAFS timeline entry in the MongoDB database.
 * Author: Michael Barall 04/08/2018.
 *
 * The collection "timeline" holds the timeline entries.
 */
public class TimelineEntry implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this timeline entry.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.
	// This is set to the same value as the id of the task that generated the timeline entry.

	private ObjectId id;

	// Time that this timeline entry was created, in milliseconds since the epoch.
	// The collection is indexed on this field, so timeline entries in a given
	// time span can be obtained with a database query efficiently.
	// In practice, every entry in the collection must have a different value of action_time
	// (so time values may need to be altered slightly), strictly monotonically increasing
	// in the order that entries are written.
	// The low-order part may have a few bits of additional information, allowing searches
	// qualified by the additional information by querying on the action_time modulus.

	private long action_time;

	//----- Action information -----

	// Event ID that this action pertains to.
	// Entries not referring to an event should put an empty string here (not null).
	// This can be used to find all timeline entries pertaining to an event.
	// In practice, this is not a Comcat event ID, but rather a similar string that serves
	// to identify an earthquake uniquely even if the Comcat IDs change.

	private String event_id;

	// List of Comcat IDs for this timeline.
	// This cannot be null or empty, and the array elements cannot be null.
	// This can be used to find all timeline entries pertaining to one or more Comcat IDs.

	private String[] comcat_ids;

	// Action code for this timeline entry.

	private int actcode;

	// Details of this action.
	// Any additional information needed is stored as a JSON string containing marshaled data.
	// If none, this should be an empty string (not null).

	private String details;

//	// Details of this action.
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

	public long get_action_time() {
		return action_time;
	}

	private void set_action_time (long action_time) {
		this.action_time = action_time;
	}

	public String get_event_id() {
		return event_id;
	}

	private void set_event_id (String event_id) {
		this.event_id = event_id;
	}

	public String[] get_comcat_ids() {
		return comcat_ids.clone();
	}

	private void set_comcat_ids (String[] comcat_ids) {
		this.comcat_ids = comcat_ids.clone();
	}

	public int get_actcode() {
		return actcode;
	}

	private void set_actcode (int actcode) {
		this.actcode = actcode;
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

		throw new IllegalArgumentException("TimelineEntry.set_details: Incorrect type of marshal writer");
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
		String str = "TimelineEntry\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\taction_time: " + action_time + "\n"
			+ "\tevent_id: " + event_id + "\n"
			+ "\tcomcat_ids: " + Arrays.toString (comcat_ids) + "\n"
			+ "\tactcode: " + actcode + "\n"
			+ "\tdetails: " + get_details_description();
		return str;
	}




	/**
	 * get_record_key - Get the record key for this timeline entry.
	 */
	public RecordKey get_record_key () {
		return new RecordKey(id);
	}




	/**
	 * set_record_key - Set the record key for this timeline entry.
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




	// Get the collection.

	private static synchronized MongoCollection<Document> get_collection () {
	
		// Get the collection

		MongoDBCollRet coll_ret = MongoDBUtil.getCollection ("timeline");

		// Create the indexes

		if (coll_ret.f_new) {
			MongoDBUtil.make_simple_index (coll_ret.collection, "event_id", "actevid");
			MongoDBUtil.make_simple_index (coll_ret.collection, "comcat_ids", "actccid");
			MongoDBUtil.make_simple_index (coll_ret.collection, "action_time", "acttime");
		}

		// Return the collection

		return coll_ret.collection;
	}




	// Convert this object to a document.
	// If id is null, it is filled in with a newly allocated id.

	private Document to_bson_doc () {
	
		// Supply the id if needed

		if (id == null) {
			id = new ObjectId();
		}

		// Construct the document

		Document doc = new Document ("_id", id)
						.append ("action_time", new Long(action_time))
						.append ("event_id"   , event_id)
						.append ("comcat_ids" , Arrays.asList(comcat_ids.clone()))
						.append ("actcode"    , new Integer(actcode))
						.append ("details"    , details);

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private TimelineEntry from_bson_doc (Document doc) {

		id          = MongoDBUtil.doc_get_object_id    (doc, "_id"        );
		action_time = MongoDBUtil.doc_get_long         (doc, "action_time");
		event_id    = MongoDBUtil.doc_get_string       (doc, "event_id"   );
		comcat_ids  = MongoDBUtil.doc_get_string_array (doc, "comcat_ids" );
		actcode     = MongoDBUtil.doc_get_int          (doc, "actcode"    );
		details     = MongoDBUtil.doc_get_string       (doc, "details"    );

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<TimelineEntry> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor) {
			super (mongo_cursor);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected TimelineEntry hook_convert (Document doc) {
			return (new TimelineEntry()).from_bson_doc (doc);
		}
	}




	// Make the natural sort for this collection.
	// The natural sort is in decreasing order of action_time (most recent first).

	private static Bson natural_sort () {
		return Sorts.descending ("action_time");
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// @param action_time_lo = Minimum action time, in milliseconds since the epoch.
	//                         Can be 0L for no minimum.
	// @param action_time_hi = Maximum action time, in milliseconds since the epoch.
	//                         Can be 0L for no maximum.
	// @param event_id = Event id. Can be null to return entries for all events.
	// @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	//                     If specified, return entries associated with any of the given ids.
	// @param action_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	//                              action time modulus. Can be null, or contain zeros, for no modulus test.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long action_time_lo, long action_time_hi, String event_id, String[] comcat_ids, long[] action_time_div_rem) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by event_id

		if (event_id != null) {
			filters.add (Filters.eq ("event_id", event_id));
		}

		// Select by comcat_ids

		if (comcat_ids != null) {
			if (comcat_ids.length > 0) {
				filters.add (Filters.in ("comcat_ids", comcat_ids));
			}
		}

		// Select entries with action_time >= action_time_lo

		if (action_time_lo > 0L) {
			filters.add (Filters.gte ("action_time", new Long(action_time_lo)));
		}

		// Select entries with action_time <= action_time_hi

		if (action_time_hi > 0L) {
			filters.add (Filters.lte ("action_time", new Long(action_time_hi)));
		}

		// Select entries with action_time % action_time_div_rem[0] == action_time_div_rem[1]

		if (action_time_div_rem != null) {
			if (action_time_div_rem[0] > 0L) {
				filters.add (Filters.mod ("action_time", new Long(action_time_div_rem[0]), new Long(action_time_div_rem[1])));
			}
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
	 * submit_timeline_entry - Submit a timeline entry.
	 * @param key = Record key associated with this timeline entry. Can be null to assign a new one.
	 * @param action_time = Time of this timeline entry, in milliseconds
	 *                      since the epoch. Must be positive.
	 * @param event_id = ID associated with this timeline, or "" if none. Cannot be null.
	 *                   It may be an event ID, or an invented ID that represents the timeline
	 *                   as distinct from any particular event ID.
	 * @param actcode = Action code, which identifies that action that occurred on the timeline.
	 * @param details = Further details of this timeline entry. Can be null if there are none.
	 * @return
	 * Returns the new entry.
	 */
	public static TimelineEntry submit_timeline_entry (RecordKey key, long action_time, String event_id,
			String[] comcat_ids, int actcode, MarshalWriter details) {

		// Check conditions

		if (!( action_time > 0L
			&& event_id != null
			&& comcat_ids != null
			&& comcat_ids.length > 0 )) {
			throw new IllegalArgumentException("TimelineEntry.submit_timeline_entry: Invalid timeline parameters");
		}

		for (String comcat_id: comcat_ids) {
			if (!( comcat_id != null )) {
				throw new IllegalArgumentException("TimelineEntry.submit_timeline_entry: Invalid timeline parameters");
			}
		}

		// Construct the timeline entry object

		TimelineEntry tentry = new TimelineEntry();
		tentry.set_record_key (key);
		tentry.set_action_time (action_time);
		tentry.set_event_id (event_id);
		tentry.set_comcat_ids (comcat_ids);
		tentry.set_actcode (actcode);
		tentry.set_details (details);

		// Call MongoDB to store into database

		get_collection().insertOne (tentry.to_bson_doc());
		
		return tentry;
	}




	/**
	 * store_timeline_entry - Store a timeline entry into the database.
	 * This is primarily for restoring from backup.
	 */
	public static TimelineEntry store_timeline_entry (TimelineEntry tentry) {

		// Call MongoDB to store into database

		get_collection().insertOne (tentry.to_bson_doc());
		
		return tentry;
	}




	/**
	 * get_timeline_entry_for_key - Get the timeline entry with the given key.
	 * @param key = Record key. Cannot be null or empty.
	 * Returns the timeline entry, or null if not found.
	 */
	public static TimelineEntry get_timeline_entry_for_key (RecordKey key) {

		if (!( key != null && key.getId() != null )) {
			throw new IllegalArgumentException("TimelineEntry.get_timeline_entry_for_key: Missing or empty record key");
		}

		// Filter: id == key.getId()

		Bson filter = id_filter (key.getId());

		// Get the document

		Document doc = get_collection().find(filter).first();

		// Convert to timeline entry

		if (doc == null) {
			return null;
		}

		return (new TimelineEntry()).from_bson_doc (doc);
	}




	/**
	 * get_timeline_entry_range - Get a range of timeline entries, reverse-sorted by action time.
	 * @param action_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param action_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param action_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 */
	public static List<TimelineEntry> get_timeline_entry_range (long action_time_lo, long action_time_hi, String event_id, String[] comcat_ids, long[] action_time_div_rem) {
		ArrayList<TimelineEntry> entries = new ArrayList<TimelineEntry>();

		// Get the cursor and iterator

		Bson filter = natural_filter (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);
		MongoCursor<Document> cursor;
		if (filter == null) {
			cursor = get_collection().find().sort(natural_sort()).iterator();
		} else {
			cursor = get_collection().find(filter).sort(natural_sort()).iterator();
		}
		try (
			MyRecordIterator iter = new MyRecordIterator (cursor);
		){
			// Dump into the list

			while (iter.hasNext()) {
				entries.add (iter.next());
			}
		}

		return entries;
	}




	/**
	 * fetch_timeline_entry_range - Iterate a range of timeline entries, reverse-sorted by action time.
	 * @param action_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param action_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param action_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 */
	public static RecordIterator<TimelineEntry> fetch_timeline_entry_range (long action_time_lo, long action_time_hi, String event_id, String[] comcat_ids, long[] action_time_div_rem) {

		// Get the cursor and iterator

		Bson filter = natural_filter (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);
		MongoCursor<Document> cursor;
		if (filter == null) {
			cursor = get_collection().find().sort(natural_sort()).iterator();
		} else {
			cursor = get_collection().find(filter).sort(natural_sort()).iterator();
		}
		return new MyRecordIterator (cursor);
	}




	/**
	 * get_recent_timeline_entry - Get the most recent in a range of timeline entries.
	 * @param action_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param action_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param action_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 * Returns the matching timeline entry with the greatest action_time (most recent),
	 * or null if there is no matching timeline entry.
	 */
	public static TimelineEntry get_recent_timeline_entry (long action_time_lo, long action_time_hi, String event_id, String[] comcat_ids, long[] action_time_div_rem) {

		// Get the document

		Bson filter = natural_filter (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);
		Document doc;
		if (filter == null) {
			doc = get_collection().find().sort(natural_sort()).first();
		} else {
			doc = get_collection().find(filter).sort(natural_sort()).first();
		}

		// Convert to timeline entry

		if (doc == null) {
			return null;
		}

		return (new TimelineEntry()).from_bson_doc (doc);
	}




	/**
	 * delete_timeline_entry - Delete a timeline entry.
	 * @param entry = Existing timeline entry to delete.
	 * @return
	 */
	public static void delete_timeline_entry (TimelineEntry entry) {

		// Check conditions

		if (!( entry != null && entry.get_id() != null )) {
			throw new IllegalArgumentException("TimelineEntry.delete_timeline_entry: Invalid parameters");
		}

		// Filter: id == entry.id

		Bson filter = id_filter (entry.get_id());

		// Run the delete

		get_collection().deleteOne (filter);
		
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 11001;

	private static final String M_VERSION_NAME = "TimelineEntry";

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		String sid = id.toHexString();
		writer.marshalString      ("id"         , sid        );
		writer.marshalLong        ("action_time", action_time);
		writer.marshalString      ("event_id"   , event_id   );
		writer.marshalStringArray ("comcat_ids" , comcat_ids );
		writer.marshalInt         ("actcode"    , actcode    );
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
		action_time = reader.unmarshalLong        ("action_time");
		event_id    = reader.unmarshalString      ("event_id"   );
		comcat_ids  = reader.unmarshalStringArray ("comcat_ids" );
		actcode     = reader.unmarshalInt         ("actcode"    );
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

	public TimelineEntry unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}


}
