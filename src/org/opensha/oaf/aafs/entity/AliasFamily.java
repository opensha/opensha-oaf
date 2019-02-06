package org.opensha.oaf.aafs.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bson.types.ObjectId;

import org.opensha.oaf.aafs.AliasAssignment;
import org.opensha.oaf.aafs.AliasAssignmentList;
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
 * Holds an AAFS alias family in the MongoDB database.
 * Author: Michael Barall 06/23/2018.
 *
 * The collection "alias" holds the alias families.
 *
 * The purpose of this class is to contain the mapping from Comcat IDs to timeline IDs.
 * This is relatively complicated because each timeline ID can correspond to several Comcat IDs;
 * the set of Comcat IDs associated with a timeline ID can change over time;  the mapping has
 * to be inferred from id lists returned by Comcat;  events can be split (Comcat IDs that refer
 * to the same event can later refer to different events);  and events can be merged (Comcat IDs
 * that refer to different events can later refer to the same event).
 *
 * Introduce some terminology.  Call two Comcat IDs "siblings" if at some time they were
 * associated to the same event.  (A Comcat ID is considered to be its own sibling.)
 * Call two Comcat IDs C1 and CN "cousins" if there are Comcat IDs C2, C3, ... such that
 * C1 and C2 are siblings, and C2 and C3 are siblings, etc.  Then the "cousin" relation is
 * the transitive closure of the "sibling" relation and hence is an equivalence relation.
 * An equivalence class of the "cousin" relation is called a "family".
 *
 * Each object of this class contains:
 * -- All the Comcat IDs making up a family at a given time.
 * -- All the timeline IDs that have ever been associated with any of the Comcat IDs.
 * -- A time stamp.
 * -- A map, which specifies which of the timeline IDs is currently associated to each Comcat ID.
 * -- For each timeline ID with associated Comcat IDs, an indication of which Comcat ID is primary.
 * (It is possible for a Comcat ID to not be associated with any timeline ID, e.g., if it has
 * been deleted from Comcat.  It is also possible for a timeline ID to not be associated with
 * any Comcat ID, e.g., as a result of merging.)
 *
 * If object O1 has a later time stamp than object O2, and if they have at least one Comcat ID
 * in common, then we say that O1 "supersedes" O2.  If O1 supersedes O2 then it follows that
 * O1 contains all the Comcat IDs that are in O2, that is, O1 is a superset of O2.  We say that
 * an object O is "active" if it is not superseded by any other object.  Because of the superset
 * property, a given Comcat ID will appear in exactly one active object, which can be found by
 * searching for the most recent object that contains the Comcat ID.
 *
 * By definition, the Comcat IDs assocated with a given timeline ID at a given time are all
 * siblings (and hence cousins).  We impose the following continuity condition: For a given
 * timeline ID T, whenever the set of Comcat IDs associated with T changes, the new set must
 * either be empty or else contain at least one Comcat ID that was previously associated with T.
 * The continuity condition implies that all the Comcat IDs ever associated with T are cousins,
 * and hence lie in a single family.  So a given timeline ID will appear in exactly one active
 * object, which contains all the Comcat IDs ever associated to that object, and can be found
 * by searching for the most recent object that contains the timeline ID.
 *
 * This structure is designed so that changes to the mapping can be effected with a single
 * write to the database, eliminating the need for any sort of transactions.  Also, the mapping
 * in effect at any time in the past can be recovered by disregarding any more recent objects.
 *
 * It should be noted that the most common case is for the set of Comcat IDs associated with
 * a given event to never change.  In this common case, the corresponding object contains a
 * single timeline ID and is never superseded.
 */
public class AliasFamily implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this alias family.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.
	// This is set to the same value as the id of the task that generated the alias family.

	private ObjectId id;

	// Time that this alias family was created, in milliseconds since the epoch.
	// The collection is indexed on this field, so alias families in a given
	// time span can be obtained with a database query efficiently.
	// In practice, every family in the collection must have a different value of family_time
	// (so time values may need to be altered slightly), strictly monotonically increasing
	// in the order that entries are written.

	private long family_time;

	//----- Action information -----

	// List of timeline IDs for this family.
	// This cannot be null or empty, and the array elements cannot be null.
	// This can be used to find all alias families pertaining to a timeline.

	private String[] timeline_ids;

	// List of Comcat IDs for this family.
	// This cannot be null or empty, and the array elements cannot be null.
	// This can be used to find all alias families pertaining to one or more Comcat IDs.
	// The ordering of Comcat IDs is significant.  First come the current Comcat IDs for the
	// first timeline, with the primary ID first.  Then come the current Comcat IDs for the
	// second timeline, with the primary ID first.  And so on.  After all the current Comcat
	// IDs for all the timelines, then come the Comcat IDs that were previously associated
	// with one or more timelines but are not currently associated with any timeline.

	private String[] comcat_ids;

	// Encoded list of bindings for this alias family.
	// Let N be the number of timelines (i.e., the length of timeline_ids).  The length
	// of enc_bindings is 2*N plus the total number of removed IDs for all timelines.
	// Then enc_bindings consists of three parts:
	// 1. Elements 0 thru N-1 are indexes into comcat_ids.  The value of enc_bindings[i]
	// is the end+1 index of the list of current Comcat IDs for timeline i.  Notice that it
	// is possible for a timeline to have zero current Comcat IDs.
	// 2. Elements N thru 2*N-1 are indexes into enc_bindings.  The value of enc_bindings[N+i]
	// is the end+1 index of the list of removed ID indexes for timeline i.  As a
	// consequence, the value of enc_bindings[2*N-1] equals the length of enc_bindings.
	// 3. Elements 2*N onward are indexes into comcat_ids.  Each of these identifies a
	// removed ID for a timeline.  First come indexes of the removed IDs for the first
	// timeline, then come the indexes of the removed IDs for the second timeline, and so on.
	// Notice that a given ID can be a removed ID for multiple timelines, and can be
	// a current ID for one timeline while simultaneously being a removed ID for one or
	// more other timelines.

	private int[] enc_bindings;




	//----- Getters and setters -----

	private ObjectId get_id() {
		return id;
	}

	private void set_id (ObjectId id) {
		this.id = id;
	}

	public long get_family_time() {
		return family_time;
	}

	private void set_family_time (long family_time) {
		this.family_time = family_time;
	}

	public String[] get_timeline_ids() {
		return timeline_ids.clone();
	}

	private void set_timeline_ids (String[] timeline_ids) {
		this.timeline_ids = timeline_ids.clone();
	}

	public String[] get_comcat_ids() {
		return comcat_ids.clone();
	}

	private void set_comcat_ids (String[] comcat_ids) {
		this.comcat_ids = comcat_ids.clone();
	}

	public int[] get_enc_bindings() {
		return enc_bindings.clone();
	}

	private void set_enc_bindings (int[] enc_bindings) {
		this.enc_bindings = enc_bindings.clone();
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "AliasFamily\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\tfamily_time: " + family_time + "\n"
			+ "\ttimeline_ids: " + Arrays.toString (timeline_ids) + "\n"
			+ "\tcomcat_ids: " + Arrays.toString (comcat_ids) + "\n"
			+ "\tenc_bindings: " + Arrays.toString (enc_bindings);
		return str;
	}




	/**
	 * get_record_key - Get the record key for this alias family.
	 */
	public RecordKey get_record_key () {
		return new RecordKey(id);
	}




	/**
	 * set_record_key - Set the record key for this alias family.
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




	// Set the timeline IDs, Comcat IDs, and bindings from a list of assignments.
	// The family time in the assigment list is not used.

	private void set_assignments (AliasAssignmentList assignments) {

		if (!( assignments != null )) {
			throw new IllegalArgumentException("AliasFamily.set_assigments: No assignment list supplied");
		}

		// Get the required lengths of our arrays

		int timeline_ids_length = assignments.get_assignment_count();
		if (!( timeline_ids_length > 0 )) {
			throw new IllegalArgumentException("AliasFamily.set_assigments: No timeline IDs supplied");
		}

		int comcat_ids_length = assignments.get_all_comcat_ids_count() + assignments.get_all_removed_ids_count();
		if (!( comcat_ids_length > 0 )) {
			throw new IllegalArgumentException("AliasFamily.set_assigments: No Comcat IDs supplied");
		}

		int enc_bindings_length = 2*timeline_ids_length + assignments.get_total_removed_ids_count();

		// Allocate the arrays

		timeline_ids = new String[timeline_ids_length];
		comcat_ids = new String[comcat_ids_length];
		enc_bindings = new int[enc_bindings_length];

		// Indexes into the arrays

		int tlix = 0;
		int ccix = 0;
		int ebix = 2*timeline_ids_length;

		// Maps that invert the timeline and Comcat arrays

		HashMap<String, Integer> tlmap = new HashMap<String, Integer>();
		HashMap<String, Integer> ccmap = new HashMap<String, Integer>();

		// This array will hold the list of removed IDs for each timeline, in the same slots as enc_bindings

		String[] removed_ids = new String[enc_bindings_length];

		// Loop over assignments

		for (Iterator<AliasAssignment> it = assignments.get_assigment_iterator(); it.hasNext(); ) {
			AliasAssignment assignment = it.next();

			// Get the timeline ID

			timeline_ids[tlix] = assignment.get_timeline_id();
			if (!( timeline_ids[tlix] != null )) {
				throw new IllegalArgumentException("AliasFamily.set_assigments: Found assignment with no timeline ID");
			}

			// Insert into reverse map and check for duplicate

			if (tlmap.put (timeline_ids[tlix], new Integer(tlix)) != null) {
				throw new IllegalArgumentException("AliasFamily.set_assigments: Found duplicate timeline ID : " + timeline_ids[tlix]);
			}

			// Get the list of Comcat IDs

			ccix = assignment.get_comcat_ids_as_array (comcat_ids, ccix);

			// Get the list of removed IDs

			ebix = assignment.get_removed_ids_as_array (removed_ids, ebix);

			// Save end-of-list indexes

			enc_bindings[tlix] = ccix;
			enc_bindings[tlix + timeline_ids_length] = ebix;

			// Next timeline

			++tlix;
		}

		// Now append the list of all removed IDs

		ccix = assignments.get_all_removed_ids_as_array (comcat_ids, ccix);

		// Check final Indexes

		if (!( tlix == timeline_ids_length && ccix == comcat_ids_length && ebix == enc_bindings_length )) {
			throw new IllegalArgumentException("AliasFamily.set_assigments: Element count mismatch");
		}

		// Construct the reverse map of Comcat IDs and check for duplicates

		for (ccix = 0; ccix < comcat_ids_length; ++ccix) {
			if (ccmap.put (comcat_ids[ccix], new Integer(ccix)) != null) {
				throw new IllegalArgumentException("AliasFamily.set_assigments: Found duplicate Comcat ID : " + comcat_ids[ccix]);
			}
		}

		// Convert the removed IDs to indexes

		for (ebix = 2*timeline_ids_length; ebix < enc_bindings_length; ++ebix) {
			enc_bindings[ebix] = ccmap.get (removed_ids[ebix]);
		}

		return;
	}




	// Get the timeline IDs, Comcat IDs, and bindings as a list of assignments.
	// Also sets the family time in the assigment list.

	public AliasAssignmentList get_assignments () {

		AliasAssignmentList assignments = new AliasAssignmentList();

		// Get the number of timelines

		int timeline_ids_length = timeline_ids.length;

		// Indexes into the arrays

		int ccix = 0;
		int ebix = 2*timeline_ids_length;

		// Loop over timelines

		for (int tlix = 0; tlix < timeline_ids_length; ++tlix) {

			AliasAssignment assignment = new AliasAssignment();

			// Set the timeline ID

			assignment.set_timeline_id (timeline_ids[tlix]);

			// Set the Comcat IDs

			int hi = enc_bindings[tlix];
			assignment.set_comcat_ids_from_array (comcat_ids, ccix, hi);
			ccix = hi;

			// Set the removed IDs

			hi = enc_bindings[tlix + timeline_ids_length];
			for ( ; ebix < hi; ++ebix) {
				assignment.add_removed_id (comcat_ids[enc_bindings[ebix]]);
			}

			// Add to the list

			assignments.add_assignment (assignment);
		}

		// Set the time stamp

		assignments.set_family_time (family_time);

		return assignments;
	}




	//----- MongoDB Java driver access -----




	//  // Get the collection.
	//  
	//  private static synchronized MongoCollection<Document> get_collection () {
	//  
	//  	// Get the collection
	//  
	//  	MongoDBCollRet coll_ret = MongoDBUtil.getCollection ("alias");
	//  
	//  	// Create the indexes
	//  
	//  	if (coll_ret.f_new) {
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "timeline_ids", "famtlid");
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "comcat_ids", "famccid");
	//  		MongoDBUtil.make_simple_index (coll_ret.collection, "family_time", "famtime");
	//  	}
	//  
	//  	// Return the collection
	//  
	//  	return coll_ret.collection;
	//  }




	// Get the collection handle.
	// If db_handle is null or empty, then use the current default database.

	private static MongoDBCollHandle get_coll_handle (String db_handle) {
		return MongoDBUtil.get_coll_handle (db_handle, "alias");
	}




	// Make indexes for our collection.

	public static void make_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Make the indexes

		coll_handle.make_simple_index ("timeline_ids", "famtlid");
		coll_handle.make_simple_index ("comcat_ids", "famccid");
		coll_handle.make_simple_index ("family_time", "famtime");

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
						.append ("family_time" , new Long(family_time))
						.append ("timeline_ids", Arrays.asList(timeline_ids.clone()))
						.append ("comcat_ids"  , Arrays.asList(comcat_ids.clone()))
						.append ("enc_bindings", MongoDBUtil.int_array_to_list (enc_bindings));

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private AliasFamily from_bson_doc (Document doc) {

		id           = MongoDBUtil.doc_get_object_id    (doc, "_id"         );
		family_time  = MongoDBUtil.doc_get_long         (doc, "family_time" );
		timeline_ids = MongoDBUtil.doc_get_string_array (doc, "timeline_ids");
		comcat_ids   = MongoDBUtil.doc_get_string_array (doc, "comcat_ids"  );
		enc_bindings = MongoDBUtil.doc_get_int_array    (doc, "enc_bindings");

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<AliasFamily> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected AliasFamily hook_convert (Document doc) {
			return (new AliasFamily()).from_bson_doc (doc);
		}
	}




	// Make the natural sort for this collection.
	// The natural sort is in decreasing order of family_time (most recent first).

	private static Bson natural_sort () {
		return Sorts.descending ("family_time");
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// @param family_time_lo = Minimum action time, in milliseconds since the epoch.
	//                         Can be 0L for no minimum.
	// @param family_time_hi = Maximum action time, in milliseconds since the epoch.
	//                         Can be 0L for no maximum.
	// @param timeline_id = Timeline id. Can be null to return entries for all timelines.
	// @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	//                     If specified, return entries associated with any of the given ids.
	// @param family_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	//                              action time modulus. Can be null, or contain zeros, for no modulus test.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long family_time_lo, long family_time_hi, String timeline_id, String[] comcat_ids, long[] family_time_div_rem) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by timeline_id

		if (timeline_id != null) {
			filters.add (Filters.eq ("timeline_ids", timeline_id));
		}

		// Select by comcat_ids

		if (comcat_ids != null) {
			if (comcat_ids.length > 0) {
				filters.add (Filters.in ("comcat_ids", comcat_ids));
			}
		}

		// Select entries with family_time >= family_time_lo

		if (family_time_lo > 0L) {
			filters.add (Filters.gte ("family_time", new Long(family_time_lo)));
		}

		// Select entries with family_time <= family_time_hi

		if (family_time_hi > 0L) {
			filters.add (Filters.lte ("family_time", new Long(family_time_hi)));
		}

		// Select entries with family_time % family_time_div_rem[0] == family_time_div_rem[1]

		if (family_time_div_rem != null) {
			if (family_time_div_rem[0] > 0L) {
				filters.add (Filters.mod ("family_time", new Long(family_time_div_rem[0]), new Long(family_time_div_rem[1])));
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
	 * submit_alias_family - Submit an alias family.
	 * @param key = Record key associated with this alias family. Can be null to assign a new one.
	 * @param family_time = Time of this alias family, in milliseconds
	 *                      since the epoch. Must be positive.
	 * @param timeline_ids = List of timeline IDs associated with this alias family. Cannot be null or empty.
	 * @param comcat_ids = List of Comcat IDs associated with this alias family. Cannot be null or empty.
	 * @param enc_bindings = Encoded bindings for the list of assignments. Cannot be null or empty.
	 * @return
	 * Returns the new family.
	 */
	private static AliasFamily submit_alias_family (RecordKey key, long family_time, String[] timeline_ids,
			String[] comcat_ids, int[] enc_bindings) {

		// Check conditions

		if (!( family_time > 0L
			&& timeline_ids != null
			&& timeline_ids.length > 0
			&& comcat_ids != null
			&& comcat_ids.length > 0
			&& enc_bindings != null
			&& enc_bindings.length >= 2*timeline_ids.length )) {
			throw new IllegalArgumentException("AliasFamily.submit_alias_family: Invalid alias family parameters");
		}

		for (String timeline_id : timeline_ids) {
			if (!( timeline_id != null )) {
				throw new IllegalArgumentException("AliasFamily.submit_alias_family: Invalid alias family parameters");
			}
		}

		for (String comcat_id : comcat_ids) {
			if (!( comcat_id != null )) {
				throw new IllegalArgumentException("AliasFamily.submit_alias_family: Invalid alias family parameters");
			}
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Construct the alias family object

		AliasFamily alfam = new AliasFamily();
		alfam.set_record_key (key);
		alfam.set_family_time (family_time);
		alfam.set_timeline_ids (timeline_ids);
		alfam.set_comcat_ids (comcat_ids);
		alfam.set_enc_bindings (enc_bindings);

		// Call MongoDB to store into database

		coll_handle.insertOne (alfam.to_bson_doc());
		
		return alfam;
	}




	/**
	 * submit_alias_family - Submit an alias family.
	 * @param key = Record key associated with this alias family. Can be null to assign a new one.
	 * @param family_time = Time of this alias family, in milliseconds
	 *                      since the epoch. Must be positive.
	 * @param assignments = List of assignments for this alias family. Cannot be null or empty.
	 * @return
	 * Returns the new family.
	 */
	public static AliasFamily submit_alias_family (RecordKey key, long family_time, AliasAssignmentList assignments) {

		// Check conditions

		if (!( family_time > 0L
			&& assignments != null )) {
			throw new IllegalArgumentException("AliasFamily.submit_alias_family: Invalid alias family parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Construct the alias family object

		AliasFamily alfam = new AliasFamily();
		alfam.set_record_key (key);
		alfam.set_family_time (family_time);
		alfam.set_assignments (assignments);

		// Call MongoDB to store into database

		coll_handle.insertOne (alfam.to_bson_doc());
		
		return alfam;
	}




	/**
	 * store_alias_family - Store an alias family into the database.
	 * This is primarily for restoring from backup.
	 */
	public static AliasFamily store_alias_family (AliasFamily alfam) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Call MongoDB to store into database

		coll_handle.insertOne (alfam.to_bson_doc());
		
		return alfam;
	}




	/**
	 * get_alias_family_for_key - Get the alias family with the given key.
	 * @param key = Record key. Cannot be null or empty.
	 * Returns the alias family, or null if not found.
	 */
	public static AliasFamily get_alias_family_for_key (RecordKey key) {

		if (!( key != null && key.getId() != null )) {
			throw new IllegalArgumentException("AliasFamily.get_alias_family_for_key: Missing or empty record key");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == key.getId()

		Bson filter = id_filter (key.getId());

		// Get the document

		Document doc = coll_handle.find_first (filter);

		// Convert to alias family

		if (doc == null) {
			return null;
		}

		return (new AliasFamily()).from_bson_doc (doc);
	}




	/**
	 * get_alias_family_range - Get a range of alias families, reverse-sorted by action time.
	 * @param family_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param family_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param timeline_id = Timeline id. Can be null to return entries for all timelines.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param family_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 */
	public static List<AliasFamily> get_alias_family_range (long family_time_lo, long family_time_hi, String timeline_id, String[] comcat_ids, long[] family_time_div_rem) {
		ArrayList<AliasFamily> entries = new ArrayList<AliasFamily>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (family_time_lo, family_time_hi, timeline_id, comcat_ids, family_time_div_rem);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort());
		try (
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
	 * fetch_alias_family_range - Iterate a range of alias families, reverse-sorted by action time.
	 * @param family_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param family_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param timeline_id = Timeline id. Can be null to return entries for all timelines.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param family_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 */
	public static RecordIterator<AliasFamily> fetch_alias_family_range (long family_time_lo, long family_time_hi, String timeline_id, String[] comcat_ids, long[] family_time_div_rem) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (family_time_lo, family_time_hi, timeline_id, comcat_ids, family_time_div_rem);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort());
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * get_recent_alias_family - Get the most recent in a range of alias families.
	 * @param family_time_lo = Minimum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no minimum.
	 * @param family_time_hi = Maximum action time, in milliseconds since the epoch.
	 *                         Can be 0L for no maximum.
	 * @param timeline_id = Timeline id. Can be null to return entries for all timelines.
	 * @param comcat_ids = Comcat id list. Can be null or empty to return entries for all Comcat ids.
	 *                     If specified, return entries associated with any of the given ids.
	 * @param family_time_div_rem = 2-element array containing divisor (element 0) and remainder (element 1) for
	 *                              action time modulus. Can be null, or contain zeros, for no modulus test.
	 * Returns the matching alias family with the greatest family_time (most recent),
	 * or null if there is no matching alias family.
	 */
	public static AliasFamily get_recent_alias_family (long family_time_lo, long family_time_hi, String timeline_id, String[] comcat_ids, long[] family_time_div_rem) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the document

		Bson filter = natural_filter (family_time_lo, family_time_hi, timeline_id, comcat_ids, family_time_div_rem);
		Document doc = coll_handle.find_first (filter, natural_sort());

		// Convert to alias family

		if (doc == null) {
			return null;
		}

		return (new AliasFamily()).from_bson_doc (doc);
	}




	/**
	 * delete_alias_family - Delete an alias family.
	 * @param alfam = Existing alias family to delete.
	 * @return
	 */
	public static void delete_alias_family (AliasFamily alfam) {

		// Check conditions

		if (!( alfam != null && alfam.get_id() != null )) {
			throw new IllegalArgumentException("AliasFamily.delete_alias_family: Invalid parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == alfam.id

		Bson filter = id_filter (alfam.get_id());

		// Run the delete

		coll_handle.deleteOne (filter);
		
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 11001;

	private static final String M_VERSION_NAME = "AliasFamily";

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		String sid = id.toHexString();
		writer.marshalString      ("id"          , sid         );
		writer.marshalLong        ("family_time" , family_time );
		writer.marshalStringArray ("timeline_ids", timeline_ids);
		writer.marshalStringArray ("comcat_ids"  , comcat_ids  );
		writer.marshalIntArray    ("enc_bindings", enc_bindings);
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		String sid;
		sid          = reader.unmarshalString      ("id"          );
		family_time  = reader.unmarshalLong        ("family_time" );
		timeline_ids = reader.unmarshalStringArray ("timeline_ids");
		comcat_ids   = reader.unmarshalStringArray ("comcat_ids"  );
		enc_bindings = reader.unmarshalIntArray    ("enc_bindings");
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

	public AliasFamily unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}


}
