package org.opensha.oaf.aafs.entity;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bson.types.ObjectId;

import org.opensha.oaf.aafs.DBCorruptException;
import org.opensha.oaf.aafs.DBException;
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
 * Common superclass for objects that are stored in the MongoDB database.
 * Author: Michael Barall 07/11/2019.
 *
 * This class is defined to contain no instance variables.
 */
public abstract class DBEntity implements java.io.Serializable {


	//----- MongoDB Java driver access -----




	/**
	 * store_entity - Store this entity into the database.
	 * This is primarily for restoring from backup.
	 */
	public abstract DBEntity store_entity ();




	//----- Utilities -----




	// The number of collections defined.

	public static final int COLL_DEFINED = 6;




	// Make indexes for all collections.
	// Parameters:
	//  f_verbose = True to write progress messages.
	// Note: This function also creates the collections.

	public static void make_all_indexes (boolean f_verbose) {

		// Create the indexes
		
		if (f_verbose) {
			System.out.println ("Creating indexes for task entries...");
		}
		PendingTask.make_indexes();
		
		if (f_verbose) {
			System.out.println ("Creating indexes for log entries...");
		}
		LogEntry.make_indexes();
		
		if (f_verbose) {
			System.out.println ("Creating indexes for catalog snapshot entries...");
		}
		CatalogSnapshot.make_indexes();
		
		if (f_verbose) {
			System.out.println ("Creating indexes for timeline entries...");
		}
		TimelineEntry.make_indexes();
		
		if (f_verbose) {
			System.out.println ("Creating indexes for alias family entries...");
		}
		AliasFamily.make_indexes();
		
		if (f_verbose) {
			System.out.println ("Creating indexes for relay items...");
		}
		RelayItem.make_indexes();
			
		if (f_verbose) {
			System.out.println ("All indexes were created successfully.");
		}
	
		return;
	}




	// Drop indexes for all collections.
	// Parameters:
	//  f_verbose = True to write progress messages.

	public static void drop_all_indexes (boolean f_verbose) {

		// Drop the indexes
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for task entries...");
		}
		PendingTask.drop_indexes();
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for log entries...");
		}
		LogEntry.drop_indexes();
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for catalog snapshot entries...");
		}
		CatalogSnapshot.drop_indexes();
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for timeline entries...");
		}
		TimelineEntry.drop_indexes();
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for alias family entries...");
		}
		AliasFamily.drop_indexes();
		
		if (f_verbose) {
			System.out.println ("Dropping indexes for relay items...");
		}
		RelayItem.drop_indexes ();
			
		if (f_verbose) {
			System.out.println ("All indexes were dropped successfully.");
		}

		return;
	}




	// Drop all collections, thereby erasing all data.
	// Parameters:
	//  f_verbose = True to write progress messages.

	public static void drop_all_collections (boolean f_verbose) {

		// Drop the collections
		
		if (f_verbose) {
			System.out.println ("Erasing collection of task entries...");
		}
		PendingTask.drop_collection();
		
		if (f_verbose) {
			System.out.println ("Erasing collection of log entries...");
		}
		LogEntry.drop_collection();
		
		if (f_verbose) {
			System.out.println ("Erasing collection of catalog snapshot entries...");
		}
		CatalogSnapshot.drop_collection();
		
		if (f_verbose) {
			System.out.println ("Erasing collection of timeline entries...");
		}
		TimelineEntry.drop_collection();
		
		if (f_verbose) {
			System.out.println ("Erasing collection of alias family entries...");
		}
		AliasFamily.drop_collection();
		
		if (f_verbose) {
			System.out.println ("Erasing collection of relay items...");
		}
		RelayItem.drop_collection ();
			
		if (f_verbose) {
			System.out.println ("All collections were erased successfully.");
		}

		return;
	}




	// Check existence of all collections.
	// Parameters:
	//  f_verbose = True to write progress messages.
	// Returns the number of collections that exist.
	// Note: If the return value is COLL_DEFINED then all collections exist.

	public static int check_all_collections (boolean f_verbose) {

		// Check existence of each collection

		int count = 0;

		if (PendingTask.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of task entries... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of task entries... NO");
			}
		}

		if (LogEntry.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of log entries... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of log entries... NO");
			}
		}

		if (CatalogSnapshot.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of catalog snapshot entries... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of catalog snapshot entries... NO");
			}
		}

		if (TimelineEntry.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of timeline entries... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of timeline entries... NO");
			}
		}

		if (AliasFamily.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of alias family entries... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of alias family entries... NO");
			}
		}

		if (RelayItem.collection_exists()) {
			++count;
			if (f_verbose) {
				System.out.println ("Existing collection of relay items... YES");
			}
		} else {
			if (f_verbose) {
				System.out.println ("Existing collection of relay items... NO");
			}
		}
			
		if (f_verbose) {
			System.out.println (count + " of " + COLL_DEFINED + " collections exist.");
		}

		return count;
	}




	// Back up all collections.
	// Parameters:
	//  writer = Destination for backup, must be able to accept multiple top-level objects.
	//  f_verbose = True to write progress messages.
	// Note: In case of error, throws an exception.

	public static void backup_database (MarshalWriter writer, boolean f_verbose) {

		// Feedback counters

		long rec_count = 0L;			// number of records so far
		long dot_count = 0L;			// number of dots so far

		long rec_freq = 128L;			// frequency at which records produce a dot_count
		long dot_freq = 128L;			// frequency at which dots product a doubling of the record frequency

		// Backup the collections

		//  if (f_verbose) {
		//  	System.out.print ("Backing up database...");
		//  }

		try {

			// Backup pending tasks

			try (
				RecordIterator<PendingTask> tasks = PendingTask.fetch_task_entry_range (0L, 0L, null, PendingTask.UNSORTED);
			){
				for (PendingTask task : tasks) {
					DBEntity.marshal_poly (writer, null, task);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// Backup log entries

			try (
				RecordIterator<LogEntry> entries = LogEntry.fetch_log_entry_range (0L, 0L, null, LogEntry.UNSORTED);
			){
				for (LogEntry entry : entries) {
					DBEntity.marshal_poly (writer, null, entry);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// Backup catalog snapshots

			try (
				RecordIterator<CatalogSnapshot> entries = CatalogSnapshot.fetch_catalog_snapshot_range (0L, 0L, null, CatalogSnapshot.UNSORTED);
			){
				for (CatalogSnapshot entry : entries) {
					DBEntity.marshal_poly (writer, null, entry);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// Backup timeline entries

			try (
				RecordIterator<TimelineEntry> entries = TimelineEntry.fetch_timeline_entry_range (0L, 0L, null, null, null, TimelineEntry.UNSORTED);
			){
				for (TimelineEntry entry : entries) {
					DBEntity.marshal_poly (writer, null, entry);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// Backup alias families

			try (
				RecordIterator<AliasFamily> entries = AliasFamily.fetch_alias_family_range (0L, 0L, null, null, null, AliasFamily.UNSORTED);
			){
				for (AliasFamily entry : entries) {
					DBEntity.marshal_poly (writer, null, entry);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// Backup relay items

			try (
				RecordIterator<RelayItem> items = RelayItem.fetch_relay_item_range (RelayItem.UNSORTED, 0L, 0L);
			){
				for (RelayItem relit : items) {
					DBEntity.marshal_poly (writer, null, relit);
					if (f_verbose) {
						if (rec_count % rec_freq == 0L) {
							System.out.print ((rec_count == 0L) ? "Backing up database..." : ".");
							++dot_count;
							if (dot_count % dot_freq == 0L) {
								rec_freq = rec_freq * 2L;
							}
						}
						++rec_count;
					}
				}
			}

			// End-of-file indicator

			if (f_verbose) {
				if (rec_count == 0L) {
					System.out.print ("Backing up database...");
				}
			}

			DBEntity.marshal_poly (writer, null, null);

		}

		// Handle any exceptions

		catch (DBCorruptException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Backup FAILED due to exception");
			}
			throw new DBCorruptException ("Backup FAILED due to exception", e);
		}

		catch (DBException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Backup FAILED due to exception");
			}
			throw new DBException ("Backup FAILED due to exception", e);
		}

		catch (MarshalException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Backup FAILED due to exception");
			}
			throw new MarshalException ("Backup FAILED due to exception", e);
		}

		catch (Exception e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Backup FAILED due to exception");
			}
			throw new RuntimeException ("Backup FAILED due to exception", e);
		}

		// Done

		if (f_verbose) {
			System.out.println();
			System.out.println ("Backup successfully saved " + rec_count + " records");
		}
	
		return;
	}




	// Restore all collections.
	// Parameters:
	//  reader = Source for restore, must be able to supply multiple top-level objects.
	//  f_verbose = True to write progress messages.
	// Note: In case of error, throws an exception.

	public static void restore_database (MarshalReader reader, boolean f_verbose) {

		// Feedback counters

		long rec_count = 0L;			// number of records so far
		long dot_count = 0L;			// number of dots so far

		long rec_freq = 128L;			// frequency at which records produce a dot_count
		long dot_freq = 128L;			// frequency at which dots product a doubling of the record frequency

		// Restore the collections

		//  if (f_verbose) {
		//  	System.out.print ("Restoring database...");
		//  }

		try {

			// Abort if any collection already exists

			int coll_count = check_all_collections (false);
			if (coll_count != 0) {
				throw new MarshalException ("Unable to restore database because the database is non-empty");
			}

			// Create the collections and indexes

			make_all_indexes (false);

			// Restore all records until end-of-file marker

			for (;;) {
				DBEntity record = unmarshal_poly (reader, null);
				if (record == null) {
					break;
				}
				record.store_entity();

				if (f_verbose) {
					if (rec_count % rec_freq == 0L) {
						System.out.print ((rec_count == 0L) ? "Restoring database..." : ".");
						++dot_count;
						if (dot_count % dot_freq == 0L) {
							rec_freq = rec_freq * 2L;
						}
					}
					++rec_count;
				}
			}

		}

		// Handle any exceptions

		catch (DBCorruptException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Restore FAILED due to exception");
			}
			throw new DBCorruptException ("Restore FAILED due to exception", e);
		}

		catch (DBException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Restore FAILED due to exception");
			}
			throw new DBException ("Restore FAILED due to exception", e);
		}

		catch (MarshalException e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Restore FAILED due to exception");
			}
			throw new MarshalException ("Restore FAILED due to exception", e);
		}

		catch (Exception e) {
			if (f_verbose) {
				System.out.println();
				System.out.println ("Restore FAILED due to exception");
			}
			throw new RuntimeException ("Restore FAILED due to exception", e);
		}

		// Done

		if (f_verbose) {
			System.out.println();
			System.out.println ("Restore successfully saved " + rec_count + " records");
		}
	
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 64001;

	private static final String M_VERSION_NAME = "DBEntity";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 64000;
	protected static final int MARSHAL_PENDING_TASK = 8001;
	protected static final int MARSHAL_LOG_ENTRY = 9001;
	protected static final int MARSHAL_CATALOG_SNAPSHOT = 10001;
	protected static final int MARSHAL_TIMELINE_ENTRY = 11001;
	protected static final int MARSHAL_ALIAS_FAMILY = 65001;
	protected static final int MARSHAL_RELAY_ITEM = 55001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected abstract void do_marshal (MarshalWriter writer);

	// Unmarshal object, internal.

	protected abstract void do_umarshal (MarshalReader reader);

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public DBEntity unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, DBEntity obj) {

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

	public static DBEntity unmarshal_poly (MarshalReader reader, String name) {
		DBEntity result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("DBEntity.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_PENDING_TASK:
			result = new PendingTask();
			result.do_umarshal (reader);
			break;

		case MARSHAL_LOG_ENTRY:
			result = new LogEntry();
			result.do_umarshal (reader);
			break;

		case MARSHAL_CATALOG_SNAPSHOT:
			result = new CatalogSnapshot();
			result.do_umarshal (reader);
			break;

		case MARSHAL_TIMELINE_ENTRY:
			result = new TimelineEntry();
			result.do_umarshal (reader);
			break;

		case MARSHAL_ALIAS_FAMILY:
			result = new AliasFamily();
			result.do_umarshal (reader);
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
