package org.opensha.oaf.aafs.entity;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bson.types.ObjectId;

import org.opensha.oaf.aafs.DBCorruptException;
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

import org.opensha.oaf.rj.CompactEqkRupList;


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
 * Holds an AAFS earthquake catalog snapshot in the MongoDB database.
 * Author: Michael Barall 04/02/2018.
 *
 * The collection "catalog" holds the earthquake catalog snapshots.
 */
public class CatalogSnapshot extends DBEntity implements java.io.Serializable {

	//----- Envelope information -----

	// Globally unique identifier for this catalog snapshot.
	// This is the MongoDB identifier.
	// Note that ObjectId implements java.io.Serializable.
	// This is set to the same value as the id of the task that generated the snapshot.

	private ObjectId id;

	//----- Catalog information -----

	// Event ID that this earthquake sequence pertains to.

	private String event_id;

	// Start time of this earthquake sequence, in milliseconds since the epoch.
	// This is not the time of the first earthquake, because there may be an interval of no earthquakes.

	private long start_time;

	// End time of this earthquake sequence, in milliseconds since the epoch.
	// This is not the time of the last earthquake, because there may be an interval of no earthquakes.

	private long end_time;

	// eqk_count - Number of earthquakes.

	private int eqk_count;

	// lat_lon_depth_list - Compressed latitude, longitude, and depth for each earthquake.
	// An entry may be zero if there is no location data for the particular earthquake.
	// The length must equal max(eqk_count,1).

	private long[] lat_lon_depth_list;

	// mag_time_list - Compressed magnitude and time for each earthquake.
	// An entry may be zero if there is no time and magnitude data for the particular earthquake.
	// The length must equal max(eqk_count,1).

	private long[] mag_time_list;




	//----- Getters and setters -----

	private ObjectId get_id() {
		return id;
	}

	private void set_id (ObjectId id) {
		this.id = id;
	}

	public String get_event_id() {
		return event_id;
	}

	private void set_event_id (String event_id) {
		this.event_id = event_id;
	}

	public long get_start_time() {
		return start_time;
	}

	private void set_start_time (long start_time) {
		this.start_time = start_time;
	}

	public long get_end_time() {
		return end_time;
	}

	private void set_end_time (long end_time) {
		this.end_time = end_time;
	}

	public int get_eqk_count() {
		return eqk_count;
	}

	private void set_eqk_count (int eqk_count) {
		this.eqk_count = eqk_count;
	}

	private long[] get_lat_lon_depth_list() {
		return lat_lon_depth_list;
	}

	private void set_lat_lon_depth_list (long[] lat_lon_depth_list) {
		this.lat_lon_depth_list = lat_lon_depth_list;
	}

	private long[] get_mag_time_list() {
		return mag_time_list;
	}

	private void set_mag_time_list (long[] mag_time_list) {
		this.mag_time_list = mag_time_list;
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "CatalogSnapshot\n"
			+ "\tid: " + ((id == null) ? ("null") : (id.toHexString())) + "\n"
			+ "\tevent_id: " + event_id + "\n"
			+ "\tstart_time: " + start_time + "\n"
			+ "\tend_time: " + end_time + "\n"
			+ "\teqk_count: " + eqk_count + "\n"
			+ "\tlat_lon_depth_list: " + ((lat_lon_depth_list == null) ? ("null") : ("len=" + lat_lon_depth_list.length)) + "\n"
			+ "\tmag_time_list: " + ((mag_time_list == null) ? ("null") : ("len=" + mag_time_list.length));
		return str;
	}




	/**
	 * get_record_key - Get the record key for this catalog snapshot.
	 */
	public RecordKey get_record_key () {
		return new RecordKey(id);
	}




	/**
	 * set_record_key - Set the record key for this catalog snapshot.
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




	/**
	 * get_rupture_list - Get the earthquake rupture list for this catalog snapshot.
	 */
	public CompactEqkRupList get_rupture_list () {

		// For empty list, pass in zero-size arrays

		if (eqk_count == 0) {
			return new CompactEqkRupList (eqk_count, new long[0], new long[0]);
		}

		// For non-empty list, pass our arrays

		return new CompactEqkRupList (eqk_count, lat_lon_depth_list, mag_time_list);
	}




	/**
	 * set_rupture_list - Set the earthquake rupture list for this catalog snapshot.
	 * @param rupture_list = Rupture list. Cannot be null.
	 */
	private void set_rupture_list (CompactEqkRupList rupture_list) {
		eqk_count = rupture_list.get_eqk_count();

		// For empty list, use one-element lists

		if (eqk_count == 0) {
			lat_lon_depth_list = new long[1];
			lat_lon_depth_list[0] = 0L;
			mag_time_list = new long[1];
			mag_time_list[0] = 0L;
			return;
		}

		// For non-empty list, pull the existing arrays, and re-size them if needed

		lat_lon_depth_list = rupture_list.get_lat_lon_depth_list();
		mag_time_list = rupture_list.get_mag_time_list();

		if (lat_lon_depth_list.length != eqk_count) {
			lat_lon_depth_list = Arrays.copyOf (lat_lon_depth_list, eqk_count);
		}
		if (mag_time_list.length != eqk_count) {
			mag_time_list = Arrays.copyOf (mag_time_list, eqk_count);
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
	//  	MongoDBCollRet coll_ret = MongoDBUtil.getCollection ("catalog");
	//  
	//  	// Create the indexes
	//  
	//  	// <none>
	//  
	//  	// Return the collection
	//  
	//  	return coll_ret.collection;
	//  }




	// Get the collection handle.
	// If db_handle is null or empty, then use the current default database.

	private static MongoDBCollHandle get_coll_handle (String db_handle) {
		return MongoDBUtil.get_coll_handle (db_handle, "catalog");
	}




	// Make indexes for our collection.

	public static void make_indexes () {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Production code does no queries or sorts, and therefore no indexes are needed.

		// Create the collection (must be done explicitly because we're not making indexes)

		coll_handle.createCollection();

		// Make the indexes

		// <none>

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

		// Convert the arrays to lists

		// Construct the document

		Document doc = new Document ("_id", id)
						.append ("event_id"          , event_id)
						.append ("start_time"        , Long.valueOf(start_time))
						.append ("end_time"          , Long.valueOf(end_time))
						.append ("eqk_count"         , Integer.valueOf(eqk_count))
						.append ("lat_lon_depth_list", MongoDBUtil.long_array_to_list (lat_lon_depth_list))
						.append ("mag_time_list"     , MongoDBUtil.long_array_to_list (mag_time_list));

		return doc;
	}




	// Fill this object from a document.
	// Throws an exception if conversion error.

	private CatalogSnapshot from_bson_doc (Document doc) {

		id                 = MongoDBUtil.doc_get_object_id  (doc, "_id"               );
		event_id           = MongoDBUtil.doc_get_string     (doc, "event_id"          );
		start_time         = MongoDBUtil.doc_get_long       (doc, "start_time"        );
		end_time           = MongoDBUtil.doc_get_long       (doc, "end_time"          );
		eqk_count          = MongoDBUtil.doc_get_int        (doc, "eqk_count"         );
		lat_lon_depth_list = MongoDBUtil.doc_get_long_array (doc, "lat_lon_depth_list");
		mag_time_list      = MongoDBUtil.doc_get_long_array (doc, "mag_time_list"     );

		return this;
	}




	// Our record iterator class.

	private static class MyRecordIterator extends RecordIteratorMongo<CatalogSnapshot> {

		// Constructor passes thru the cursor.

		public MyRecordIterator (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
			super (mongo_cursor, coll_handle);
		}

		// Hook routine to convert a Document to a T.

		@Override
		protected CatalogSnapshot hook_convert (Document doc) {
			return (new CatalogSnapshot()).from_bson_doc (doc);
		}
	}




	//  // Make the natural sort for this collection.
	//  // The natural sort is in decreasing order of end_time (most recent first).
	//  
	//  private static Bson natural_sort () {
	//  	return Sorts.descending ("end_time");
	//  }




	// Sort order options.

	public static final int ASCENDING = 1;
	public static final int DESCENDING = -1;
	public static final int UNSORTED = 0;

	private static final int DEFAULT_SORT = DESCENDING;




	// Make the natural sort for this collection.
	// The natural sort is in ascending or descending order of end_time.
	// If sort_order is UNSORTED, then return null, which means unsorted.

	private static Bson natural_sort (int sort_order) {
		switch (sort_order) {
		case UNSORTED:
			return null;
		case DESCENDING:
			return Sorts.descending ("end_time");
		case ASCENDING:
			return Sorts.ascending ("end_time");
		}
		throw new IllegalArgumentException ("CatalogSnapshot.natural_sort: Invalid sort order: " + sort_order);
	}




	// Make a filter on the id field.

	private static Bson id_filter (ObjectId the_id) {
		return Filters.eq ("_id", the_id);
	}




	// Make the natural filter for this collection.
	// @param end_time_lo = Minimum end time, in milliseconds since the epoch.
	//                      Can be 0L for no minimum.
	// @param end_time_hi = Maximum end time, in milliseconds since the epoch.
	//                      Can be 0L for no maximum.
	// @param event_id = Event id. Can be null to return entries for all events.
	// Return the filter, or null if no filter is required.
	// Note: An alternative to returning null would be to return new Document(),
	// which is an empty document, and which when used as a filter matches everything.

	private static Bson natural_filter (long end_time_lo, long end_time_hi, String event_id) {
		ArrayList<Bson> filters = new ArrayList<Bson>();

		// Select by event_id

		if (event_id != null) {
			filters.add (Filters.eq ("event_id", event_id));
		}

		// Select entries with end_time >= end_time_lo

		if (end_time_lo > 0L) {
			filters.add (Filters.gte ("end_time", Long.valueOf(end_time_lo)));
		}

		// Select entries with end_time <= end_time_hi

		if (end_time_hi > 0L) {
			filters.add (Filters.lte ("end_time", Long.valueOf(end_time_hi)));
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
	 * submit_catalog_shapshot - Submit a catalog snapshot.
	 * @param key = Record key associated with this catalog snapshot. Can be null to assign a new one.
	 * @param event_id = Event associated with this catalog snapshot, or "" if none. Cannot be null.
	 * @param start_time = Start time of this earthquake sequence, in milliseconds
	 *                     since the epoch. Must be positive.
	 * @param end_time = End time of this earthquake sequence, in milliseconds
	 *                   since the epoch. Must be positive. Must be >= start_time.
	 * @param rupture_list = Rupture list. Cannot be null.
	 * @return
	 * Returns the new entry.
	 */
	public static CatalogSnapshot submit_catalog_shapshot (RecordKey key, String event_id,
			long start_time, long end_time, CompactEqkRupList rupture_list) {

		// Check conditions

		if (!( event_id != null
			&& start_time > 0L
			&& end_time >= start_time
			&& rupture_list != null )) {
			throw new IllegalArgumentException("CatalogSnapshot.submit_catalog_shapshot: Invalid catalog snapshot parameters");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Construct the catalog snapshot object

		CatalogSnapshot catsnap = new CatalogSnapshot();
		catsnap.set_record_key (key);
		catsnap.set_event_id (event_id);
		catsnap.set_start_time (start_time);
		catsnap.set_end_time (end_time);
		catsnap.set_rupture_list (rupture_list);

		// Call MongoDB to store into database

		coll_handle.insertOne (catsnap.to_bson_doc());
		
		return catsnap;
	}




	/**
	 * store_catalog_shapshot - Store a catalog snapshot into the database.
	 * This is primarily for restoring from backup.
	 */
	public static CatalogSnapshot store_catalog_shapshot (CatalogSnapshot catsnap) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Call MongoDB to store into database

		coll_handle.insertOne (catsnap.to_bson_doc());
		
		return catsnap;
	}




	/**
	 * store_entity - Store this entity into the database.
	 * This is primarily for restoring from backup.
	 */
	@Override
	public CatalogSnapshot store_entity () {
		store_catalog_shapshot (this);
		return this;
	}




	/**
	 * get_catalog_shapshot_for_key - Get the catalog snapshot with the given key.
	 * @param key = Record key. Cannot be null or empty.
	 * Returns the catalog snapshot, or null if not found.
	 *
	 * Current usage: Production.
	 */
	public static CatalogSnapshot get_catalog_shapshot_for_key (RecordKey key) {

		if (!( key != null && key.getId() != null )) {
			throw new IllegalArgumentException("CatalogSnapshot.get_catalog_shapshot_for_key: Missing or empty record key");
		}

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Filter: id == key.getId()

		Bson filter = id_filter (key.getId());

		// Get the document

		Document doc = coll_handle.find_first (filter);

		// Convert to catalog snapshot

		if (doc == null) {
			return null;
		}

		return (new CatalogSnapshot()).from_bson_doc (doc);
	}




	/**
	 * get_catalog_snapshot_range - Get a range of catalog snapshots, reverse-sorted by end time.
	 * @param end_time_lo = Minimum end time, in milliseconds since the epoch.
	 *                      Can be 0L for no minimum.
	 * @param end_time_hi = Maximum end time, in milliseconds since the epoch.
	 *                      Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param sort_order = DESCENDING to sort in descending order by end_time (most recent first),
	 *                     ASCENDING to sort in ascending order by end_time (oldest first),
	 *                     UNSORTED to return unsorted results.  Defaults to DESCENDING.
	 *
	 * Current usage: Test only.
	 */
	public static List<CatalogSnapshot> get_catalog_snapshot_range (long end_time_lo, long end_time_hi, String event_id) {
		return get_catalog_snapshot_range (end_time_lo, end_time_hi, event_id, DEFAULT_SORT);
	}

	public static List<CatalogSnapshot> get_catalog_snapshot_range (long end_time_lo, long end_time_hi, String event_id, int sort_order) {
		ArrayList<CatalogSnapshot> entries = new ArrayList<CatalogSnapshot>();

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (end_time_lo, end_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order));
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
	 * fetch_catalog_snapshot_range - Iterate a range of catalog snapshots, reverse-sorted by end time.
	 * @param end_time_lo = Minimum end time, in milliseconds since the epoch.
	 *                      Can be 0L for no minimum.
	 * @param end_time_hi = Maximum end time, in milliseconds since the epoch.
	 *                      Can be 0L for no maximum.
	 * @param event_id = Event id. Can be null to return entries for all events.
	 * @param sort_order = DESCENDING to sort in descending order by end_time (most recent first),
	 *                     ASCENDING to sort in ascending order by end_time (oldest first),
	 *                     UNSORTED to return unsorted results.  Defaults to DESCENDING.
	 *
	 * Current usage: Backup only, with no filters and UNSORTED.
	 */
	public static RecordIterator<CatalogSnapshot> fetch_catalog_snapshot_range (long end_time_lo, long end_time_hi, String event_id) {
		return fetch_catalog_snapshot_range (end_time_lo, end_time_hi, event_id, DEFAULT_SORT);
	}

	public static RecordIterator<CatalogSnapshot> fetch_catalog_snapshot_range (long end_time_lo, long end_time_hi, String event_id, int sort_order) {

		// Get collection handle

		MongoDBCollHandle coll_handle = get_coll_handle (null);

		// Get the cursor and iterator

		Bson filter = natural_filter (end_time_lo, end_time_hi, event_id);
		MongoCursor<Document> cursor = coll_handle.find_iterator (filter, natural_sort (sort_order));
		return new MyRecordIterator (cursor, coll_handle);
	}




	/**
	 * delete_catalog_snapshot - Delete a catalog snapshot.
	 * @param entry = Existing catalog snapshot to delete.
	 * @return
	 */
	public static void delete_catalog_snapshot (CatalogSnapshot entry) {

		// Check conditions

		if (!( entry != null && entry.get_id() != null )) {
			throw new IllegalArgumentException("CatalogSnapshot.delete_catalog_snapshot: Invalid parameters");
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

	private static final int MARSHAL_VER_1 = 10001;

	private static final String M_VERSION_NAME = "CatalogSnapshot";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_CATALOG_SNAPSHOT;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		MongoDBUtil.marshal_object_id (writer, "id", id);
		writer.marshalString      ("event_id"          , event_id          );
		writer.marshalLong        ("start_time"        , start_time        );
		writer.marshalLong        ("end_time"          , end_time          );
		writer.marshalInt         ("eqk_count"         , eqk_count         );
		writer.marshalLongArray   ("lat_lon_depth_list", lat_lon_depth_list);
		writer.marshalLongArray   ("mag_time_list"     , mag_time_list     );
	
		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		id                 = MongoDBUtil.unmarshal_object_id (reader, "id");
		event_id           = reader.unmarshalString      ("event_id"          );
		start_time         = reader.unmarshalLong        ("start_time"        );
		end_time           = reader.unmarshalLong        ("end_time"          );
		eqk_count          = reader.unmarshalInt         ("eqk_count"         );
		lat_lon_depth_list = reader.unmarshalLongArray   ("lat_lon_depth_list");
		mag_time_list      = reader.unmarshalLongArray   ("mag_time_list"     );

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

	public CatalogSnapshot unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, CatalogSnapshot obj) {

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

	public static CatalogSnapshot unmarshal_poly (MarshalReader reader, String name) {
		CatalogSnapshot result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("CatalogSnapshot.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CATALOG_SNAPSHOT:
			result = new CatalogSnapshot();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
