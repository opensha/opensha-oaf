package org.opensha.oaf.aafs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.InvariantViolationException;

import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.WriteConcern;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.ClientSessionOptions;
import com.mongodb.TransactionOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import static org.opensha.oaf.aafs.MongoDBConfig.CollectionConfig;
import static org.opensha.oaf.aafs.MongoDBConfig.DatabaseConfig;
import static org.opensha.oaf.aafs.MongoDBConfig.HostConfig;



/**
 * Connection pool for MongoDB.
 * Author: Michael Barall.
 *
 * This object manages a connection to each known MongoDB host.
 *
 * In a multi-threaded application, you could one of these object globally for all threads,
 * or a separate object for each thread.
 * (But our current emphasis is on single-threaded applications.)
 *
 * The internal structure is built in the constructor based on the configuration.
 */
public class MongoDBConnect implements AutoCloseable {




	//----- Tracing -----




	// Flag to trace connections.  (Not synchronized because it should only be set at startup.)

	private static boolean trace_conn = false;

	public static void set_trace_conn (boolean b) {
		trace_conn = b;
		return;
	}




	//----- Hosts -----




	// HostState - Holds current state for a host.

	public static class HostState {

		// Configuration for this host.

		private final HostConfig host_config;

		public HostConfig get_host_config () {
			return host_config;		// does not need to be synchronized because host_config never changes
		}

		// The MongoDB client, or null if none.

		private MongoClient mongo_client;

		// The current number of connections.

		private int conn_count;

		// Generation, this is incremented by 1 each time the client is created.

		private long generation;

		// Constructor.

		public HostState (HostConfig host_config) {
			this.host_config = host_config;
			mongo_client = null;
			conn_count = 0;
			generation = 0L;
		}

		// Connect to host.
		// If this throws an exception, then the connection did not occur.

		public synchronized void conn_host () {

			if (trace_conn) {
				System.out.println ("HostState.conn_host: Enter: host_handle = " + host_config.get_host_handle() + ", conn_count = " + conn_count);
			}

			// If not currently connected ...

			if (conn_count == 0) {
			
				// Make the client, throw exception if failed

				MongoClient my_client = host_config.make_client();

				// Update state

				++generation;
				mongo_client = my_client;
			}

			// Increment connection count

			++conn_count;

			if (trace_conn) {
				System.out.println ("HostState.conn_host: Exit: host_handle = " + host_config.get_host_handle() + ", conn_count = " + conn_count);
			}

			return;
		}

		// Disconnect from host.
		// If this throws an exception, then the disconnection DID occur.
		// Throws an exception if not currently connected.

		public synchronized void disc_host () {

			if (trace_conn) {
				System.out.println ("HostState.disc_host: Enter: host_handle = " + host_config.get_host_handle() + ", conn_count = " + conn_count);
			}
		
			// If not connected, error

			if (conn_count == 0) {
				throw new InvariantViolationException ("MongoDBConnect.HostState.disc_host: Not connected: conn_count = " + conn_count);
			}

			// Update state

			--conn_count;

			// Close the client if now disconnected

			if (conn_count == 0) {
				MongoClient my_client = mongo_client;
				mongo_client = null;
				my_client.close();
			}

			if (trace_conn) {
				System.out.println ("HostState.disc_host: Exit: host_handle = " + host_config.get_host_handle() + ", conn_count = " + conn_count);
			}

			return;
		}

		// Get the MongoDB client.
		// Throw an exception if not currently connected.

		public synchronized MongoClient get_mongo_client () {
			if (conn_count == 0) {
				throw new InvariantViolationException ("MongoDBConnect.HostState.get_mongo_client: Not connected: conn_count = " + conn_count);
			}
			return mongo_client;
		}

		// Return true if the client is connected.

		public synchronized boolean is_connected () {
			return conn_count != 0;
		}

		// Clear to disconnected state.
		// This does not throw any exception.

		public synchronized void clear () {
			MongoClient my_client = mongo_client;
			mongo_client = null;
			conn_count = 0;
			if (my_client != null) {
				try {
					my_client.close();
				} catch (Exception e) {
				}
			}
		}

	}




	//----- Global state -----




	// MongoDB configuration.

	private MongoDBConfig mongo_config;

	public MongoDBConfig get_mongo_config () {
		return mongo_config;
	}

	// Map of host handles to host states.

	private Map<String, HostState> map_host_state;

	// Constructor.

	public MongoDBConnect (MongoDBConfig mongo_config) {

		// Save configuration

		this.mongo_config = mongo_config;

		// Construct the host states

		map_host_state = new LinkedHashMap<String, HostState>();

		for (HostConfig host_config : mongo_config.get_hosts()) {
			if (map_host_state.put (host_config.get_host_handle(), new HostState (host_config)) != null) {
				throw new InvariantViolationException ("MongoDBConnect.MongoDBConnect: Duplicate host handle: host_handle = " + host_config.get_host_handle());
			}
		}
	}

	// Get the host state for the given host handle.
	// Throws an exception if invalid handle.
	// Note: This does not need to be synchronized because the map is not changed after construction.

	public HostState get_host_state (String host_handle) {
		HostState host_state = map_host_state.get (host_handle);
		if (host_state == null) {
			throw new InvariantViolationException ("MongoDBConnect.get_host_state: Unknown host handle: host_handle = " + host_handle);
		}
		return host_state;
	}

	// Get the collection of hosts.

	public Collection<HostState> get_host_states () {
		return Collections.unmodifiableCollection (map_host_state.values());
	}

	// Release all resources and clear all data structures.

	@Override
	public void close () {

		// If not closed ...

		if (mongo_config != null) {
		
			// Clear all hosts

			for (String host_handle : map_host_state.keySet()) {
				map_host_state.get(host_handle).clear();
			}

			// Clear global data

			mongo_config = null;
			map_host_state = null;
		}

		return;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MongoDBConnect : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("MongoDBConnect : Unrecognized subcommand : " + args[0]);
		return;

	}

}
