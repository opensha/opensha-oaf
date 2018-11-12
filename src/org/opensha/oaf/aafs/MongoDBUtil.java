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

import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;


/**
 * This class holds utilities for access to MongoDB.
 * Author: Michael Barall 03/15/2018.
 *
 * Any operation that uses MongoDB must create one of these objects.
 * It is strongly advised to create the object in a try-with-resources
 * statement, to ensure it is closed upon exit from the program.
 */
public class MongoDBUtil implements AutoCloseable {

	// The MongoDB server address.

	private static ServerAddress serverAddress = null;

	// The MongoDB security credentials, used to log in to the MongoDB server.

	private static MongoCredential credentials = null;

	// The MongoDB client options.

	private static MongoClientSettings mongoSettings = null;

	// The MongoDB client endpoint.
	// Note that there should be only one client per JVM.

	private static MongoClient mongoClient = null;

	// The MongoDB database.

	private static MongoDatabase db = null;

	// A map of collection names to MongoDB collections.

	private static HashMap<String, MongoCollection<Document>> coll_map = null;



	
	/**
	 * Attach to the MongoDB database.
	 */
	public MongoDBUtil() {

		// This cannot be called if the connection is currently open.

		if (mongoClient != null) {
			throw new RuntimeException ("MongoDBUtil: Connection to MongoDB is already open");
		}

		MongoClient saved_mongoClient = null;

		try {

			// Get the server configuration, which has the database address and credentials.

			ServerConfig config = new ServerConfig();

			// Create the address of the server, using host IP address and port.
			// Note: ServerAddress offers several ways to specify the address.

			serverAddress = new ServerAddress (config.getDb_host(), config.getDb_port());

			// Create the login credentials, for username, database name, and password.
			// Note: MongoCredential can create various other sorts of credentials.
			// Note: In MongoDB, it is necessary to authenticate to a particular database.
			//  It must be the database that was used to create the user account.
			//  This does not limit the databases that can be used, once logged in.

			credentials = MongoCredential.createCredential (config.getDb_user(), config.getDb_name(), config.getDb_password().toCharArray());

			// Create the MongoDB client settings.
			// Note: We use the default client settings.
			// Note: MongoClientSettings.Builder offers various settings, using manipulator methods.

			mongoSettings = MongoClientSettings.builder()
								.applyToClusterSettings (builder -> builder.hosts(Arrays.asList(serverAddress)))
								.credential (credentials)
								.build();

			// Create the MongoDB client endpoint.

			mongoClient = MongoClients.create (mongoSettings);
			saved_mongoClient = mongoClient;

			// Get the database, using database name.

			db = mongoClient.getDatabase (config.getDb_name());

			// Create the map for caching collections.

			coll_map = new HashMap<String, MongoCollection<Document>>();

		} catch (Exception e) {
			coll_map = null;
			db = null;
			mongoClient = null;
			if (saved_mongoClient != null) {
				try {
					saved_mongoClient.close();
				} catch (Exception e2) {
				}
			}
			saved_mongoClient = null;
			mongoSettings = null;
			credentials = null;
			serverAddress = null;
			throw new RuntimeException ("MongoDBUtil: Unable to connect to MongoDB", e);
		}
	}



	
	/**
	 * Close the MongoDB database.
	 */
	@Override
	public void close() {

		// Close the client if it is currently open.

		if (mongoClient != null) {
			coll_map = null;
			db = null;
			mongoClient.close();
			mongoClient = null;
			mongoSettings = null;
			credentials = null;
			serverAddress = null;
		}

		return;
	}



	
	/**
	 * Retrieve the MongoDB database.
	 */
	public static MongoDatabase getDB() {
		return db;
	}




	// Get or create a collection.

	public static synchronized MongoDBCollRet getCollection (String name) {

		// Try to get the collection from the cache

		MongoCollection<Document> collection = coll_map.get (name);
		if (collection != null) {
			return new MongoDBCollRet (collection, false);
		}

		// Otherwise make a new collection and store it in the cache

		collection = db.getCollection (name);
		coll_map.put (name, collection);
		return new MongoDBCollRet (collection, true);
	}




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


}
