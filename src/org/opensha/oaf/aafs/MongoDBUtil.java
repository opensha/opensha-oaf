package org.opensha.oaf.aafs;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import org.opensha.oaf.util.TestMode;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * This class holds utilities for access to MongoDB.
 * Author: Michael Barall 03/15/2018.
 *
 * Any operation that uses MongoDB must create one of these objects.
 * It is strongly advised to create the object in a try-with-resources
 * statement, to ensure it is closed upon exit from the program.
 */
public class MongoDBUtil implements AutoCloseable {




	//----- Connection and content management -----




	// MongoDB connection access, which is shared among all threads.

	private static MongoDBConnect cached_mongo_connect = null;

	// MongoDB content access, which is separate for each thread.

	private static final ThreadLocal<MongoDBContent> cached_mongo_content =
		new ThreadLocal<MongoDBContent>() {
			@Override protected MongoDBContent initialValue () {
				return null;
			}
		};




	// Get the connection access, create it if necessary.

	private static synchronized MongoDBConnect get_mongo_connect () {

		// If not created yet ...

		if (cached_mongo_connect == null) {

			// Get the server configuration, which has the database configuration.

			ServerConfig config = new ServerConfig();

			// Create the connection

			MongoDBConnect mongo_connect = new MongoDBConnect (config.get_mongo_config());

			// Save the connection

			cached_mongo_connect = mongo_connect;
		}
		
		// Return it

		return cached_mongo_connect;
	}




	// Get the content access for this thread, create it if necessary.

	private static MongoDBContent get_mongo_content () {

		// Get thread-local value

		MongoDBContent mongo_content = cached_mongo_content.get();

		// If not created yet ...

		if (mongo_content == null) {

			// Create the content

			mongo_content = new MongoDBContent (get_mongo_connect());

			// Save the content
		
			cached_mongo_content.set (mongo_content);
		}
	
		// Return it

		return mongo_content;
	}




	// Return true if the content for this thread is null (not allocated yet).

	private static boolean is_mongo_content_null () {

		// Get thread-local value

		MongoDBContent mongo_content = cached_mongo_content.get();

		if (mongo_content == null) {
			return true;
		}

		return false;
	}




	// Close the content for this thread and discard it, if it exists.

	private static void close_mongo_content () {

		// Get thread-local value

		MongoDBContent mongo_content = cached_mongo_content.get();

		// If created ...

		if (mongo_content != null) {

			// Discard it
		
			cached_mongo_content.set (null);

			// Close it

			mongo_content.close();
		}
	
		return;
	}




	//----- Application access RAII -----




	// Connection options.

	public static final int	CONOPT_NONE = 0;				// Do not make any connection
	public static final int	CONOPT_CONNECT = 1;				// Connect to database
	public static final int	CONOPT_SESSION = 2;				// Start a session for the database
	public static final int	CONOPT_TRANSACT_COMMIT = 3;		// Start a transaction for the database, with default action = commit
	public static final int	CONOPT_TRANSACT_ABORT = 4;		// Start a transaction for the database, with default action = abort

	// Default database options.

	public static final int	DDBOPT_NONE = 0;				// Take no action
	public static final int	DDBOPT_SAVE = 1;				// Save and restore current default database
	public static final int	DDBOPT_SET = 2;					// Set default database
	public static final int	DDBOPT_SAVE_SET = 3;			// Save and restore current default database, and set default database


	// Default connection option.

	private static int def_conopt = CONOPT_CONNECT;

	// Default default database option.

	private static int def_ddbopt = DDBOPT_SAVE_SET;


	// Saved value of the default database handle.

	private String saved_default_db_handle;

	// The connection option.

	private int conopt;

	// The default database option.

	private int ddbopt;

	// The effective database handle for this operation.

	private String eff_db_handle;

	// Flag which indicates if the content was null at the time this object was created.

	private boolean f_content_null;




	// Set the default connection option.
	// This is intended only for use in testing, and only before the first connection.

	public static void set_def_conopt (int conopt) {

		// Validate the option

		switch (conopt) {
			case CONOPT_NONE:
			case CONOPT_CONNECT:
			case CONOPT_SESSION:
			case CONOPT_TRANSACT_COMMIT:
			case CONOPT_TRANSACT_ABORT:
				break;

			default:
				throw new IllegalArgumentException ("MongoDBUtil.set_def_conopt: Invalid connection option: conopt = " + conopt);
		}

		// Set the default

		def_conopt = conopt;
		return;
	}




	// Undo-able content allocation.

	public class UndoAlloc implements AutoCloseable {

		// Flag indicates if undo is desired.

		public boolean f_undo;

		// Turn off undo, to keep the connection.

		public void keep () {
			f_undo = false;
		}
		
		// Constructor argument specifies if this is setup (true) or tear-down (false).

		public UndoAlloc (boolean f_make) {
			f_undo = true;
		}

		// Close content during close, if requested.

		@Override
		public void close () {
			if (f_undo) {
				f_undo = false;
				if (f_content_null) {
					close_mongo_content();
				}
			}

			return;
		}
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
				get_mongo_content().connect_database (eff_db_handle);
			}
		}

		// Disconnect during close, if requested.

		@Override
		public void close () {
			if (f_undo) {
				f_undo = false;
				get_mongo_content().disconnect_database (eff_db_handle);
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
		
		// Open session during constructor, if requested.

		public UndoSession (boolean f_make) {
			f_undo = true;
			if (f_make) {
				get_mongo_content().start_session (eff_db_handle);
			}
		}

		// Close session during close, if requested.

		@Override
		public void close () {
			if (f_undo) {
				f_undo = false;
				get_mongo_content().stop_session (eff_db_handle);
			}

			return;
		}
	}




	// Undo-able transaction establishment.

	public class UndoTransact implements AutoCloseable {

		// Flag indicates if undo is desired.

		public boolean f_undo;

		// Turn off undo, to keep the connection.

		public void keep () {
			f_undo = false;
		}
		
		// Start transaction during constructor, if requested.

		public UndoTransact (boolean f_make, boolean f_commit) {
			f_undo = true;
			if (f_make) {
				get_mongo_content().start_transact (eff_db_handle, f_commit);
			}
		}

		// Stop transaction during close, if requested.

		@Override
		public void close () {
			if (f_undo) {
				f_undo = false;
				get_mongo_content().stop_transact (eff_db_handle);
			}

			return;
		}
	}




	// Connect to the MongoDB database.
	// Parameters:
	//  conopt = Connect option.
	//  ddbopt = Default database option.
	//  db_handle = Database handle, can be null or empty to select default database.

	public MongoDBUtil (int conopt, int ddbopt, String db_handle) {

		// Validate the options

		switch (conopt) {
			case CONOPT_NONE:
			case CONOPT_CONNECT:
			case CONOPT_SESSION:
			case CONOPT_TRANSACT_COMMIT:
			case CONOPT_TRANSACT_ABORT:
				break;

			default:
				throw new IllegalArgumentException ("MongoDBUtil: Invalid connection option: conopt = " + conopt);
		}

		switch (ddbopt) {
			case DDBOPT_NONE:
			case DDBOPT_SAVE:
			case DDBOPT_SET:
			case DDBOPT_SAVE_SET:
				break;

			default:
				throw new IllegalArgumentException ("MongoDBUtil: Invalid default database option: ddbopt = " + ddbopt);
		}

		// Save options

		this.conopt = conopt;
		this.ddbopt = ddbopt;

		// Save flag indicating if content is null at entry

		f_content_null = is_mongo_content_null();

		// Save default database handle

		MongoDBContent mongo_content = get_mongo_content();
		saved_default_db_handle = mongo_content.get_default_db_handle();

		// Get the database handle this applies to

		eff_db_handle = db_handle;
		if (eff_db_handle == null || eff_db_handle.isEmpty()) {
			eff_db_handle = saved_default_db_handle;
		}

		// Do requested connections

		switch (this.conopt) {
			case CONOPT_NONE:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (true);
				) {
					undo_alloc.keep();
				}
				break;

			case CONOPT_CONNECT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (true);
					UndoConnect undo_connect = new UndoConnect (true);
				) {
					undo_alloc.keep();
					undo_connect.keep();
				}
				break;

			case CONOPT_SESSION:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (true);
					UndoConnect undo_connect = new UndoConnect (true);
					UndoSession undo_session = new UndoSession (true);
				) {
					undo_alloc.keep();
					undo_connect.keep();
					undo_session.keep();
				}
				break;

			case CONOPT_TRANSACT_COMMIT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (true);
					UndoConnect undo_connect = new UndoConnect (true);
					UndoSession undo_session = new UndoSession (true);
					UndoTransact undo_transact = new UndoTransact (true, true);
				) {
					undo_alloc.keep();
					undo_connect.keep();
					undo_session.keep();
					undo_transact.keep();
				}
				break;

			case CONOPT_TRANSACT_ABORT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (true);
					UndoConnect undo_connect = new UndoConnect (true);
					UndoSession undo_session = new UndoSession (true);
					UndoTransact undo_transact = new UndoTransact (true, false);
				) {
					undo_alloc.keep();
					undo_connect.keep();
					undo_session.keep();
					undo_transact.keep();
				}
				break;
		}

		// Set new default database if requested

		switch (ddbopt) {
			case DDBOPT_SET:
			case DDBOPT_SAVE_SET:
				mongo_content.set_default_db_handle (eff_db_handle);
				break;
		}
	}



	
	// Attach to the MongoDB database.

	public MongoDBUtil () {
		this (def_conopt, def_ddbopt, null);
	}



	
	// Close the MongoDB database.

	@Override
	public void close () {

		// Restore default database if requested

		MongoDBContent mongo_content = get_mongo_content();

		switch (ddbopt) {
			case DDBOPT_SAVE:
			case DDBOPT_SAVE_SET:
				mongo_content.set_default_db_handle (saved_default_db_handle);
				break;
		}

		// Do requested disconnections

		switch (this.conopt) {
			case CONOPT_NONE:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (false);
				) {
				}
				break;

			case CONOPT_CONNECT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (false);
					UndoConnect undo_connect = new UndoConnect (false);
				) {
				}
				break;

			case CONOPT_SESSION:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (false);
					UndoConnect undo_connect = new UndoConnect (false);
					UndoSession undo_session = new UndoSession (false);
				) {
				}
				break;

			case CONOPT_TRANSACT_COMMIT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (false);
					UndoConnect undo_connect = new UndoConnect (false);
					UndoSession undo_session = new UndoSession (false);
					UndoTransact undo_transact = new UndoTransact (false, false);
				) {
				}
				break;

			case CONOPT_TRANSACT_ABORT:
				try (
					UndoAlloc undo_alloc = new UndoAlloc (false);
					UndoConnect undo_connect = new UndoConnect (false);
					UndoSession undo_session = new UndoSession (false);
					UndoTransact undo_transact = new UndoTransact (false, false);
				) {
				}
				break;
		}

		return;
	}




	// Set the transaction commit flag.
	// There must be a transaction in progress on our database, or it will throw exception.

	public void set_transact_commit (boolean f_commit) {
		get_mongo_content().set_transact_commit (eff_db_handle, f_commit);
		return;
	}




	// Return true if sessions are enabled.

	public boolean is_session_enabled () {
		return get_mongo_content().is_session_enabled (eff_db_handle);
	}


	// Return true if sessions are enabled on the given database.
	// If db_handle is null or empty, then the default database handle is used.

	public static boolean is_session_enabled (String db_handle) {
		return get_mongo_content().is_session_enabled (db_handle);
	}




	// Return true if transactions are enabled.

	public boolean is_transaction_enabled () {
		return get_mongo_content().is_transaction_enabled (eff_db_handle);
	}


	// Return true if transactions are enabled on the given database.
	// If db_handle is null or empty, then the default database handle is used.

	public static boolean is_transaction_enabled (String db_handle) {
		return get_mongo_content().is_transaction_enabled (db_handle);
	}




	//----- Entity access -----




	// Get the collection handle, given the database handle and the collection name.
	// If db_handle is null or empty, then the default database handle is used.

	public static MongoDBCollHandle get_coll_handle (String db_handle, String coll_name) {

		// Get the content accessor

		MongoDBContent mongo_content = get_mongo_content();

		// Get the handle

		return mongo_content.get_coll_handle (db_handle, coll_name);
	}




	//  // Get a collection, in the default database.
	//  
	//  public static MongoDBCollRet getCollection (String name) {
	//  
	//  	// Get the collection handle
	//  
	//  	MongoDBCollHandle coll_handle = get_coll_handle (null, name);
	//  
	//  	// Return the MongoDB collection
	//  
	//  	return new MongoDBCollRet (coll_handle.get_mongo_collection(), false);
	//  }




	// Make an index on one field, with the given name.

	public static void make_simple_index (MongoCollection<Document> c, String field, String name) {
		c.createIndex (Indexes.ascending (field), new IndexOptions().name (name));
		return;
	}




	//----- BSON Document access -----




	// Read an ObjectId from a document.
	// If name is omitted, then "_id" is the default.
	// An exception is thrown if the object is null.

	public static ObjectId doc_get_object_id (Document doc, String name) {

		ObjectId x;
		try {
			x = doc.getObjectId(name);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x;
	}

	public static ObjectId doc_get_object_id (Document doc) {
		return doc_get_object_id (doc, "_id");
	}


	// Read a string from a document.
	// An exception is thrown if the string is null.

	public static String doc_get_string (Document doc, String name) {

		String x;
		try {
			x = doc.getString(name);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x;
	}


	// Read a long from a document.
	// An exception is thrown if the Long is null.

	public static long doc_get_long (Document doc, String name) {

		Long x;
		try {
			x = doc.getLong(name);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x.longValue();
	}


	// Read an int from a document.
	// An exception is thrown if the Integer is null.

	public static int doc_get_int (Document doc, String name) {

		Integer x;
		try {
			x = doc.getInteger(name);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x.intValue();
	}


	// Read a double from a document.
	// An exception is thrown if the Double is null.

	public static double doc_get_double (Document doc, String name) {

		Double x;
		try {
			x = doc.getDouble(name);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x.doubleValue();
	}


	// Read a document from a document.
	// An exception is thrown if the Document is null.

	public static Document doc_get_document (Document doc, String name) {

		Document x;
		try {
			x = doc.get (name, Document.class);
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + name, e);
		}
		if (x == null) {
			throw new DBCorruptException ("MongoDB document read: Null field: " + name);
		}

		return x;
	}


	// Read a string array from a document.
	// An exception is thrown if the array or any contained string is null.

	public static String[] doc_get_string_array (Document doc, String name) {

		String[] r;
		int i = -1;
		try {
			List<?> w = doc.get (name, List.class);
			if (w == null) {
				throw new NullPointerException ("Null list");
			}
			int n = w.size();
			r = new String[n];
			for (Object o : w) {
				++i;
				if (o == null) {
					throw new NullPointerException ("Null list element");
				}
				r[i] = (String)o;
			}
			++i;
			if (i != n) {
				throw new IndexOutOfBoundsException ("List underrun: expecting " + n + ", got " + i);
			}
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + ((i < 0) ? (name) : (name + "[" + i + "]")), e);
		}

		return r;
	}


	// Read a long array from a document.
	// An exception is thrown if the array or any contained Long is null.

	public static long[] doc_get_long_array (Document doc, String name) {

		long[] r;
		int i = -1;
		try {
			List<?> w = doc.get (name, List.class);
			if (w == null) {
				throw new NullPointerException ("Null list");
			}
			int n = w.size();
			r = new long[n];
			for (Object o : w) {
				++i;
				if (o == null) {
					throw new NullPointerException ("Null list element");
				}
				r[i] = ((Long)o).longValue();
			}
			++i;
			if (i != n) {
				throw new IndexOutOfBoundsException ("List underrun: expecting " + n + ", got " + i);
			}
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + ((i < 0) ? (name) : (name + "[" + i + "]")), e);
		}

		return r;
	}


	// Read an int array from a document.
	// An exception is thrown if the array or any contained Integer is null.

	public static int[] doc_get_int_array (Document doc, String name) {

		int[] r;
		int i = -1;
		try {
			List<?> w = doc.get (name, List.class);
			if (w == null) {
				throw new NullPointerException ("Null list");
			}
			int n = w.size();
			r = new int[n];
			for (Object o : w) {
				++i;
				if (o == null) {
					throw new NullPointerException ("Null list element");
				}
				r[i] = ((Integer)o).intValue();
			}
			++i;
			if (i != n) {
				throw new IndexOutOfBoundsException ("List underrun: expecting " + n + ", got " + i);
			}
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + ((i < 0) ? (name) : (name + "[" + i + "]")), e);
		}

		return r;
	}


	// Read a double array from a document.
	// An exception is thrown if the array or any contained Double is null.

	public static double[] doc_get_double_array (Document doc, String name) {

		double[] r;
		int i = -1;
		try {
			List<?> w = doc.get (name, List.class);
			if (w == null) {
				throw new NullPointerException ("Null list");
			}
			int n = w.size();
			r = new double[n];
			for (Object o : w) {
				++i;
				if (o == null) {
					throw new NullPointerException ("Null list element");
				}
				r[i] = ((Double)o).doubleValue();
			}
			++i;
			if (i != n) {
				throw new IndexOutOfBoundsException ("List underrun: expecting " + n + ", got " + i);
			}
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + ((i < 0) ? (name) : (name + "[" + i + "]")), e);
		}

		return r;
	}


	// Read a document array from a document.
	// An exception is thrown if the array or any contained Document is null.

	public static Document[] doc_get_document_array (Document doc, String name) {

		Document[] r;
		int i = -1;
		try {
			List<?> w = doc.get (name, List.class);
			if (w == null) {
				throw new NullPointerException ("Null list");
			}
			int n = w.size();
			r = new Document[n];
			for (Object o : w) {
				++i;
				if (o == null) {
					throw new NullPointerException ("Null list element");
				}
				r[i] = (Document)o;
			}
			++i;
			if (i != n) {
				throw new IndexOutOfBoundsException ("List underrun: expecting " + n + ", got " + i);
			}
		} catch (Exception e) {
			throw new DBCorruptException ("MongoDB document read: Error converting field: " + ((i < 0) ? (name) : (name + "[" + i + "]")), e);
		}

		return r;
	}




	// Convert a long array to a list of Long.

	public static List<Long> long_array_to_list (long[] arr) {
		ArrayList<Long> w = new ArrayList<Long>();
		for (long x: arr) {
			w.add (new Long(x));
		}
		return w;
	}


	// Convert an int array to a list of Integer.

	public static List<Integer> int_array_to_list (int[] arr) {
		ArrayList<Integer> w = new ArrayList<Integer>();
		for (int x: arr) {
			w.add (new Integer(x));
		}
		return w;
	}


	// Convert a double array to a list of Double.

	public static List<Double> double_array_to_list (double[] arr) {
		ArrayList<Double> w = new ArrayList<Double>();
		for (double x: arr) {
			w.add (new Double(x));
		}
		return w;
	}




	//----- Testing support -----




	// Construct a repeatable ObjectId given a time in milliseconds.
	// This uses a counter which is incremented on every call.

	// Implementation note: As of MongoDB Java driver 3.10, the ObjectId constructor used
	// below is deprecated.  Without using deprecated APIs, the only way to make a repeatable
	// ObjectId now is to construct it from a hex string or array of bytes.  It is 12 bytes
	// as follows:
	//  bytes 0 - 3 = timestamp = (int)(date.getTime() / 1000L)   [byte 0 is high-order, byte 3 is low-order]
	//  bytes 4 - 6 = machineIdentifier low 3 bytes   [byte 4 is high-order, byte 6 is low-order]
	//  bytes 7 - 8 = processIdentifier 2 bytes   [byte 7 is high-order, byte 8 is low-order]
	//  bytes 9 - 11 = counter low 3 bytes   [byte 9 is high-order, byte 11 is low-order]
	// The hex string is byte 0 as the first two characters, byte 1 as the third and fourth characters, etc.
	// Apparently any 24-character hex string is acceptable for constructing an ObjectId.
	// ObjectId produces hex strings with lower case letters, but accepts both upper and lower case.
	// The machineIdentifier and counter each must be between 0 and 16777215.

	private static int sim_key_counter = 0;

//	private static synchronized ObjectId make_repeatable_object_id (long the_time) {
//		++sim_key_counter;
//
//		Date date = new Date (the_time);
//		int machineIdentifier = sim_key_counter / 16777216;
//		short processIdentifier = (short)0;
//		int counter = sim_key_counter % 16777216;
//
//		return new ObjectId (date, machineIdentifier, processIdentifier, counter);
//	}

	// Implementation note: As of MongoDB Java driver 4.0, the ObjectId constructor used
	// above is removed.  We now construct the ObjectId from a hex string.

	private static synchronized ObjectId make_repeatable_object_id (long the_time) {
		++sim_key_counter;

		long timestamp = (the_time / 1000L) & 0xffffffffL;
		int machineIdentifier = (sim_key_counter & 0x7f000000) / 16777216;
		int processIdentifier = 0;
		int counter = sim_key_counter & 0x00ffffff;

		String hexstring = String.format ("%08x%06x%04x%06x", timestamp, machineIdentifier, processIdentifier, counter);

		return new ObjectId (hexstring);
	}




	// Construct a new ObjectId.
	// If time has been set in test mode, then return a repeatable ObjectId,
	// which allows a repeatable sequence of ObjectId to be generated provided
	// that each invocation of the program has a different test time.

	public static ObjectId make_object_id () {

		// Check for test mode

		long the_time = TestMode.get_test_time();

		// If test mode is in effect, return a repeatable ObjectId

		if (the_time > 0L) {
			return make_repeatable_object_id (the_time);
		}

		// Just return a new ObjectId
	
		return new ObjectId ();
	}




	//  // Construct a new ObjectId that can be used as an end-of-file marker.
	//  
	//  private static final String OID_EOF_HEX_STRING = "0e0f0e0f0e0f0e0f0e0f0e0f";
	//  
	//  public static ObjectId make_eof_object_id () {
	//  
	//  	// Return a new ObjectId constructed from a fixed hex string
	//  
	//  	return new ObjectId (OID_EOF_HEX_STRING);
	//  }




	//  // Test if the given ObjectId is the end-of-file marker.
	//  
	//  public static boolean is_eof_object_id (ObjectId id) {
	//  	return id.toHexString().equalsIgnoreCase (OID_EOF_HEX_STRING);
	//  }




	// Marshal an ObjectId.
	// The supplied ObjectId can be null.

	public static void marshal_object_id (MarshalWriter writer, String name, ObjectId id) {

		// Use an empty string for null, otherwise use the hex string representation

		String sid;
		if (id == null) {
			sid = "";
		} else {
			sid = id.toHexString();
		}

		writer.marshalString (name, sid);
		return;
	}




	// Unmarshal and ObjectId.
	// The returned ObjectId can be null.

	public static ObjectId unmarshal_object_id (MarshalReader reader, String name) {

		// Expect an empty string for null, otherwise the hex string representation

		String sid = reader.unmarshalString (name);

		ObjectId id;
		if (sid.isEmpty()) {
			id = null;
		} else {
			try {
				id = new ObjectId (sid);
			} catch (Exception e) {
				throw new MarshalException ("MongoDBUtil.unmarshal_object_id: Invalid string representation of ObjectId: " + sid, e);
			}
		}
	
		return id;
	}




	// Construct a unique string.
	// This function returns a string which is different each time it is called.
	// The string is intended to be globally unique.
	// Note: The caller should not make any assumptions about the contents or length of these strings.
	// In particular, do not assume any relationship to the time.

	// Implementation note: An implementation based on java.util.UUID is also possible,
	// and perhaps better.  This version produces repeatable results when in test mode.

	public static String get_unique_string () {
		return make_object_id().toHexString();
		// return java.util.UUID.randomUUID().toString();
	}

}
