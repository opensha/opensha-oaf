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


/**
 * This class holds utilities for access to MongoDB.
 * Author: Michael Barall 03/15/2018.
 *
 * Any operation that uses MongoDB must create one of these objects.
 * It is strongly advised to create the object in a try-with-resources
 * statement, to ensure it is closed upon exit from the program.
 */
public class MongoDBUtil implements AutoCloseable {

	// MongoDB connection access, which is shared among all threads.

	private static MongoDBConnect cached_mongo_connect = null;

	// MongoDB content access, which is separate for each thread.

	private static final ThreadLocal<MongoDBContent> cached_mongo_content =
		new ThreadLocal<MongoDBContent>() {
			@Override protected MongoDBContent initialValue () {
				return null;
			}
		};

	// Database handle to disconnect when this object is closed, none if null or empty.

	private String saved_db_handle;




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



	
	// Attach to the MongoDB database.

	public MongoDBUtil () {

		try {

			// Get the content accessor

			MongoDBContent mongo_content = get_mongo_content();

			// Save the default database handle

			saved_db_handle = mongo_content.get_default_db_handle();

			// If it's non-empty, connect to it

			if (!( saved_db_handle == null || saved_db_handle.isEmpty() )) {
				mongo_content.connect_database (saved_db_handle);
			}

		} catch (Exception e) {
			throw new RuntimeException ("MongoDBUtil: Unable to connect to MongoDB", e);
		}
	}



	
	// Close the MongoDB database.

	@Override
	public void close () {

		try {

			// Get the content accessor

			MongoDBContent mongo_content = get_mongo_content();

			// If the saved database handle is non-empty, disconnect from it

			if (!( saved_db_handle == null || saved_db_handle.isEmpty() )) {
				mongo_content.disconnect_database (saved_db_handle);
			}

		} catch (Exception e) {
			throw new RuntimeException ("MongoDBUtil: Unable to disconnect from MongoDB", e);
		}

		return;
	}




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




	// Construct a repeatable ObjectId given a time in milliseconds.
	// This uses a counter which is incremented on every call.

	private static int sim_key_counter = 0;

	private static synchronized ObjectId make_repeatable_object_id (long the_time) {
		++sim_key_counter;

		Date date = new Date (the_time);
		int machineIdentifier = sim_key_counter / 16777216;
		short processIdentifier = (short)0;
		int counter = sim_key_counter % 16777216;

		return new ObjectId (date, machineIdentifier, processIdentifier, counter);
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


}
