package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Queue;

import java.io.Closeable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;
import org.opensha.oaf.aafs.entity.DBEntity;

import org.opensha.oaf.util.MarshalImpDataReader;
import org.opensha.oaf.util.MarshalImpDataWriter;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Thread for pulling relay items from the partner server.
 * Author: Michael Barall 08/05/2018.
 */
public class RelayThread implements Runnable {

	//----- Constants -----


	// Relay thread status values.

	public static final int RTSTAT_IDLE = 1;			// Thread is idle, initial state.
	public static final int RTSTAT_STARTING = 2;		// Thread is in the process of starting.  [Active state]
	public static final int RTSTAT_CONNECTING = 3;		// Thread is in the process of connecting to remote server.  [Active state]
	public static final int RTSTAT_CONNECT_FAIL = 4;	// Thread stopped because it was never able to connect.
	public static final int RTSTAT_RUNNING = 5;			// Thread is running normally.  [Active state]
	public static final int RTSTAT_SHUTDOWN = 6;		// Thread shut down normally.
	public static final int RTSTAT_CONNECT_LOSS = 7;	// Thread stopped because connection was lost or database issue.
	public static final int RTSTAT_QUEUE_OVERFLOW = 8;	// Thread stopped because of queue overflow or internal error.

	// Return true if the relay status is active (meaning that the thread has not exited).

	public static boolean is_relay_active (int rtstat) {
		switch (rtstat) {
		case RTSTAT_IDLE:
		case RTSTAT_CONNECT_FAIL:
		case RTSTAT_SHUTDOWN:
		case RTSTAT_CONNECT_LOSS:
		case RTSTAT_QUEUE_OVERFLOW:
			return false;
		case RTSTAT_STARTING:
		case RTSTAT_CONNECTING:
		case RTSTAT_RUNNING:
			return true;
		}
		throw new IllegalArgumentException ("RelayThread.is_relay_active: Unknown relay status: " + rtstat);
	}

	// Fetch item status values.

	public static final int FISTAT_IDLE = 1;			// Fetch idle, waiting for fetch command.
	public static final int FISTAT_PENDING = 2;			// Fetch request pending.  [Active state]
	public static final int FISTAT_FETCHING = 3;		// Fetch active, fetching items.  [Active state]
	public static final int FISTAT_FINISHED = 4;		// Fetch finished normally, fetch results are available.
	public static final int FISTAT_CANCELED = 5;		// Fetch stopped because it was canceled.
	public static final int FISTAT_IO_FAIL = 6;			// Fetch failed because of I/O error.
	public static final int FISTAT_EXITED = 7;			// Fetch failed because thread exited.

	// Return true if the fetch status is active (meaning that the thread can undertake additional fetch actions).

	public static boolean is_fetch_active (int fistat) {
		switch (fistat) {
		case FISTAT_IDLE:
		case FISTAT_FINISHED:
		case FISTAT_CANCELED:
		case FISTAT_IO_FAIL:
		case FISTAT_EXITED:
			return false;
		case FISTAT_PENDING:
		case FISTAT_FETCHING:
			return true;
		}
		throw new IllegalArgumentException ("RelayThread.is_fetch_active: Unknown fetch status: " + fistat);
	}

	// Default polling interval, in milliseconds.

	public static final long DEFAULT_POLLING_INTERVAL = 30000;		// 30 seconds

	// Default maximum queue size.

	public static final int DEFAULT_MAX_QUEUE_SIZE = 10000;

	// An exception that can be thrown to terminate the thread.

	public static class RelayTerminateException extends RuntimeException {
	
		// The termination status

		int term_relay_status;

		// The termination exception, which can be null.

		Exception term_relay_exception;

		// Constructor.

		RelayTerminateException (int term_relay_status, Exception term_relay_exception) {
			this.term_relay_status = term_relay_status;
			this.term_relay_exception = term_relay_exception;
		}
	}




	//----- Synchronized variables -----
	//
	// These are variables that can be accessed from both the dispatcher thread and the relay thread.


	private static class RelaySyncVar {


		//--- Parameters

		// The database handle from which relay items are retrieved, or null to use the default database.

		private String ri_db_handle;

		public synchronized String get_ri_db_handle () {
			return ri_db_handle;
		}

		public synchronized void set_ri_db_handle (String the_ri_db_handle) {
			ri_db_handle = the_ri_db_handle;
			return;
		}

		// The polling interval when watching the change stream iterator, in milliseconds.

		private long ri_polling_interval;

		public synchronized long get_ri_polling_interval () {
			return ri_polling_interval;
		}

		public synchronized void set_ri_polling_interval (long the_ri_polling_interval) {
			ri_polling_interval = ((the_ri_polling_interval > 0L) ? the_ri_polling_interval : DEFAULT_POLLING_INTERVAL);
			return;
		}

		// Maximum allowed size of the relay item change stream queue.

		private int max_ri_queue_size;

		public synchronized int get_max_ri_queue_size () {
			return max_ri_queue_size;
		}

		public synchronized void set_max_ri_queue_size (int the_max_ri_queue_size) {
			max_ri_queue_size = ((the_max_ri_queue_size > 0) ? the_max_ri_queue_size : DEFAULT_MAX_QUEUE_SIZE);
			return;
		}




		//--- Relay item queue

		// Queue used to send relay items from the relay thread to the dispatcher thread.

		private ArrayDeque<RelayItem> ri_queue;


		// Return the size of the relay item queue

		public synchronized int ri_queue_size () {
			return ri_queue.size();
		}


		//  // Return true if the relay item queue is full.
		//  
		//  public synchronized boolean ri_queue_is_full () {
		//  	boolean f_result = false;
		//  	if (ri_queue.size() >= max_ri_queue_size) {
		//  		f_result = true;
		//  	}
		//  	return f_result;
		//  }


		// Return true if the relay item queue has room.

		public synchronized boolean ri_queue_has_room () {
			boolean f_result = true;
			if (ri_queue.size() >= max_ri_queue_size) {
				f_result = false;
			}
			return f_result;
		}


		// Insert an item in the relay item queue.
		// The insertion proceeds regardless of the current size of the queue.

		public synchronized void ri_queue_insert (RelayItem relit) {
			ri_queue.add (relit);
			return;
		}


		// Remove an item from the relay item queue.
		// Returns null if the queue is empty.

		public synchronized RelayItem ri_queue_remove () {
			RelayItem relit = ri_queue.poll();
			return relit;
		}


		// Clear the relay item queue.

		//public synchronized void ri_queue_clear () {
		//	ri_queue.clear();
		//	return;
		//}




		//--- Relay thread status

		// Relay status, one of the RTSTAT_XXXXX values.

		private int ri_relay_status;

		public synchronized int get_ri_relay_status () {
			return ri_relay_status;
		}

		public synchronized void set_ri_relay_status (int the_ri_relay_status) {
			ri_relay_status = the_ri_relay_status;
			return;
		}

		// Relay exception, or null if none.

		private Exception ri_relay_exception;

		public synchronized Exception get_ri_relay_exception () {
			return ri_relay_exception;
		}

		public synchronized void set_ri_relay_exception (Exception the_ri_relay_exception) {
			ri_relay_exception = the_ri_relay_exception;
			return;
		}

		// Flag indicating that a shutdown request has been received.

		private boolean ri_shutdown_req;

		public synchronized boolean get_ri_shutdown_req () {
			return ri_shutdown_req;
		}

		public synchronized void set_ri_shutdown_req (boolean the_ri_shutdown_req) {
			ri_shutdown_req = the_ri_shutdown_req;
			return;
		}




		//--- Fetch operation status

		// Fetch status, one of the FISTAT_XXXXX values.

		private int ri_fetch_status;

		public synchronized int get_ri_fetch_status () {
			return ri_fetch_status;
		}

		public synchronized void set_ri_fetch_status (int the_ri_fetch_status) {
			ri_fetch_status = the_ri_fetch_status;
			return;
		}

		// Fetch exception, or null if none.

		private Exception ri_fetch_exception;

		public synchronized Exception get_ri_fetch_exception () {
			return ri_fetch_exception;
		}

		public synchronized void set_ri_fetch_exception (Exception the_ri_fetch_exception) {
			ri_fetch_exception = the_ri_fetch_exception;
			return;
		}

		// Destination for fetch operation.

		private MarshalWriter ri_fetch_writer;

		public synchronized MarshalWriter get_ri_fetch_writer () {
			return ri_fetch_writer;
		}

		public synchronized void set_ri_fetch_writer (MarshalWriter the_ri_fetch_writer) {
			ri_fetch_writer = the_ri_fetch_writer;
			return;
		}

		// Lower time range for fetch operation, or 0L if no limit.

		private long ri_fetch_relay_time_lo;

		public synchronized long get_ri_fetch_relay_time_lo () {
			return ri_fetch_relay_time_lo;
		}

		public synchronized void set_ri_fetch_relay_time_lo (long the_ri_fetch_relay_time_lo) {
			ri_fetch_relay_time_lo = the_ri_fetch_relay_time_lo;
			return;
		}

		// Upper time range for fetch operation, or 0L if no limit.

		private long ri_fetch_relay_time_hi;

		public synchronized long get_ri_fetch_relay_time_hi () {
			return ri_fetch_relay_time_hi;
		}

		public synchronized void set_ri_fetch_relay_time_hi (long the_ri_fetch_relay_time_hi) {
			ri_fetch_relay_time_hi = the_ri_fetch_relay_time_hi;
			return;
		}

		// List of relay item ids for fetch operation, or null or empty for no restriction.

		private String[] ri_fetch_relay_id;

		public synchronized String[] get_ri_fetch_relay_id () {
			if (ri_fetch_relay_id == null) {
				return null;
			}
			return ri_fetch_relay_id.clone();
		}

		public synchronized void set_ri_fetch_relay_id (String[] the_ri_fetch_relay_id) {
			if (the_ri_fetch_relay_id == null) {
				ri_fetch_relay_id = null;
			} else {
				ri_fetch_relay_id = the_ri_fetch_relay_id.clone();
			}
			return;
		}

		// Flag indicating that a cancel request has been received.

		private boolean ri_fetch_cancel_req;

		public synchronized boolean get_ri_fetch_cancel_req () {
			return ri_fetch_cancel_req;
		}

		public synchronized void set_ri_fetch_cancel_req (boolean the_ri_fetch_cancel_req) {
			ri_fetch_cancel_req = the_ri_fetch_cancel_req;
			return;
		}




		//--- Setup

		// Constructor.
		// Note: The constructor and setup() do exactly the same thing.

		public RelaySyncVar () {
			setup();
		}


		// Set up (initialize or re-initialize).
		// This establishes default parameter values.
		// Note: The constructor and setup() do exactly the same thing.

		public synchronized void setup () {

			// Parameters

			ri_db_handle = null;
			ri_polling_interval = DEFAULT_POLLING_INTERVAL;
			max_ri_queue_size = DEFAULT_MAX_QUEUE_SIZE;

			// Relay item queue

			ri_queue = new ArrayDeque<RelayItem>();

			// Relay thread status

			ri_relay_status = RTSTAT_IDLE;
			ri_relay_exception = null;
			ri_shutdown_req = false;

			// Fetch operation status

			ri_fetch_status = FISTAT_IDLE;
			ri_fetch_exception = null;
			ri_fetch_writer = null;
			ri_fetch_relay_time_lo = 0L;
			ri_fetch_relay_time_hi = 0L;
			ri_fetch_relay_id = null;
			ri_fetch_cancel_req = false;

			return;
		}


		// Prepare to start thread.
		// This should be called after setting up parameters, and before launching the thread.
		// Returns true if preparation was successful, false if unsuccessful.
		// Note: The function returns false if the thread is already active.

		public synchronized boolean prepare () {

			// Error if thread is active.

			if (is_relay_active (ri_relay_status)) {
				return false;
			}

			// Relay item queue

			ri_queue = new ArrayDeque<RelayItem>();

			// Relay thread status

			ri_relay_status = RTSTAT_STARTING;		// Thread is starting
			ri_relay_exception = null;
			ri_shutdown_req = false;

			// Fetch operation status

			ri_fetch_status = FISTAT_IDLE;
			ri_fetch_exception = null;
			ri_fetch_writer = null;
			ri_fetch_relay_time_lo = 0L;
			ri_fetch_relay_time_hi = 0L;
			ri_fetch_relay_id = null;
			ri_fetch_cancel_req = false;

			return true;
		}




		//--- Atomic operations


		// Set relay thread exit status.
		// This is intended to be called only from the relay thread, when the thread is exiting.

		public synchronized void set_relay_exit_status (int relay_status, Exception relay_exception) {

			// If a fetch operation is active, terminate it

			if (is_fetch_active (ri_fetch_status)) {
				ri_fetch_status = FISTAT_EXITED;
				ri_fetch_exception = null;
				ri_fetch_writer = null;
				ri_fetch_relay_time_lo = 0L;
				ri_fetch_relay_time_hi = 0L;
				ri_fetch_relay_id = null;
				ri_fetch_cancel_req = false;
			}

			// Relay item queue

			ri_queue = new ArrayDeque<RelayItem>();

			// Set the relay status

			ri_relay_status = relay_status;
			ri_relay_exception = relay_exception;
			ri_shutdown_req = false;

			return;
		}


		// Set fetch operation completion status.
		// This is intended to be called only from the relay thread, when the fetch operation is ending.

		public synchronized void set_fetch_completion_status (int fetch_status, Exception fetch_exception) {
		
			// Set the fetch status

			ri_fetch_status = fetch_status;
			ri_fetch_exception = fetch_exception;
			ri_fetch_writer = null;
			ri_fetch_relay_time_lo = 0L;
			ri_fetch_relay_time_hi = 0L;
			ri_fetch_relay_id = null;
			ri_fetch_cancel_req = false;

			return;
		}


		// Request a fetch operation.
		// Parameters:
		//  writer = Destination for fetch.
		//  relay_time_lo = Lower limit of relay_time range.
		//  relay_time_hi = Upper limit of relay_time range.
		//  relay_id = List of relay item ids for fetch operation, or null or empty for no restriction.
		// Returns true if successful, false if operation could not be started.
		// Note: The function returns false if a fetch operation is already active, or if the thread is not active.

		public synchronized boolean request_fetch (MarshalWriter writer, long relay_time_lo, long relay_time_hi, String... relay_id) {

			// Error if thread is not active.

			if (!( is_relay_active (ri_relay_status) )) {
				return false;
			}

			// Error if fetch is active.

			if (is_fetch_active (ri_fetch_status)) {
				return false;
			}

			// Fetch operation status

			ri_fetch_status = FISTAT_PENDING;
			ri_fetch_exception = null;
			ri_fetch_writer = writer;
			ri_fetch_relay_time_lo = relay_time_lo;
			ri_fetch_relay_time_hi = relay_time_hi;
			if (relay_id == null) {
				ri_fetch_relay_id = null;
			} else {
				ri_fetch_relay_id = relay_id.clone();
			}
			ri_fetch_cancel_req = false;
		
			return true;
		}

	}

	private RelaySyncVar sync_var;




	//----- Internal variables -----


	// The Java thread that is running the relay thread, or null if relay thread is not running.

	private Thread relay_java_thread;




	//----- Service functions (callable from the dispatcher thread) -----


	//--- Parameters


	// Set the database handle from which relay items are retrieved.
	// Can be null to select the default database.

	public void set_ri_db_handle (String the_ri_db_handle) {
		sync_var.set_ri_db_handle (the_ri_db_handle);
		return;
	}


	// Set the polling interval, in milliseconds.
	// Can be 0L to select the default value.

	public void set_ri_polling_interval (long the_ri_polling_interval) {
		sync_var.set_ri_polling_interval (the_ri_polling_interval);
		return;
	}

	// Set the maximum allowed size of the relay item change stream queue.
	// Can be 0 to select the default value.

	public synchronized void set_max_ri_queue_size (int the_max_ri_queue_size) {
		sync_var.set_max_ri_queue_size (the_max_ri_queue_size);
		return;
	}


	//--- Relay item queue


	// Remove an item from the relay item queue.
	// Returns null if the queue is empty.

	public RelayItem ri_queue_remove () {
		return sync_var.ri_queue_remove();
	}


	// Get relay status, one of the RTSTAT_XXXXX values.

	public int get_ri_relay_status () {
		return sync_var.get_ri_relay_status();
	}


	//--- Relay thread status


	// Get relay exception, or null if none.

	public Exception get_ri_relay_exception () {
		return sync_var.get_ri_relay_exception();
	}


	// Request relay thread shutdown.

	public void set_ri_shutdown_req () {
		sync_var.set_ri_shutdown_req (true);
		return;
	}


	//--- Fetch operation status


	// Get fetch status, one of the FISTAT_XXXXX values.

	public int get_ri_fetch_status () {
		return sync_var.get_ri_fetch_status();
	}


	// Get fetch exception, or null if none.

	public Exception get_ri_fetch_exception () {
		return sync_var.get_ri_fetch_exception();
	}


	// Request fetch operation cancel.

	public void set_ri_fetch_cancel_req () {
		sync_var.set_ri_fetch_cancel_req (true);
		return;
	}


	// Request a fetch operation.
	// Parameters:
	//  writer = Destination for fetch.
	//  relay_time_lo = Lower limit of relay_time range, or 0L for no limit.
	//  relay_time_hi = Upper limit of relay_time range, or 0L for no limit.
	//  relay_id = List of relay item ids for fetch operation, or null or empty for no restriction.
	// Returns true if successful, false if operation could not be started.
	// Note: The function returns false if a fetch operation is already active, or if the thread is not active.

	public boolean request_fetch (MarshalWriter writer, long relay_time_lo, long relay_time_hi, String... relay_id) {
		return sync_var.request_fetch (writer, relay_time_lo, relay_time_hi, relay_id);
	}


	//--- Thread management


	// Start the relay thread.
	// Returns true if success, false if thread is not started.
	// Note: The function throws an exception if a thread is already running.

	public void start_relay_thread () {
		if (relay_java_thread != null) {
			throw new IllegalStateException ("RelayThread.start_relay_thread: Thread is already running");
		}
		if (!( sync_var.prepare() )) {
			throw new IllegalStateException ("RelayThread.start_relay_thread: Thread state is invalid");
		}
		relay_java_thread = new Thread (this);
		relay_java_thread.start();
		return;
	}

	
	// Stop the relay thread.
	// Performs no operation if the thread is already stopped.
	// This function waits for the relay thread to die.

	public void stop_relay_thread () {
		Thread the_thread = relay_java_thread;
		if (the_thread != null) {
			relay_java_thread = null;
			sync_var.set_ri_shutdown_req (true);
			while (the_thread.isAlive()) {
				try {
					the_thread.join();
				} catch (InterruptedException e) {
				}
			}
		}
		return;
	}


	// The Sentinel class stops the relay thread when it is closed.
	// It can be used in a try-with-resources statement to ensure the thread is stopped.
	// Note that it may incur polling delay, because close() waits for the thread to die.
	// There is no error if the thread is not running when the sentinel is closed.

	public class Sentinel implements AutoCloseable {
		@Override
		public void close () {
			stop_relay_thread();
			return;
		}
	}


	// Make a sentinal.

	public Sentinel make_sentinel () {
		return new Sentinel();
	}


	//--- Test helpers


	// Run a fetch operation, wait for completion, and then return results in a reader.
	// Parameters:
	//  relay_time_lo = Lower limit of relay_time range, or 0L for no limit.
	//  relay_time_hi = Upper limit of relay_time range, or 0L for no limit.
	//  relay_id = List of relay item ids for fetch operation, or null or empty for no restriction.
	// Returns a reader that contain the results, in a memory buffer.
	// The results are a sequence of relay items, terminated with a null item.
	// Throws an exception if the operation cannot be done.
	// This function can be used in try-with-resources statement.
	// This function is intended for testing.

	public MarshalImpDataReader run_fetch_and_wait (long relay_time_lo, long relay_time_hi, String... relay_id) throws IOException {

		// The output buffer

		ByteArrayOutputStream output_buffer = new ByteArrayOutputStream();

		// Open the writer

		try (
			MarshalImpDataWriter writer = new MarshalImpDataWriter (
				new DataOutputStream (new GZIPOutputStream (output_buffer)),
				true);
		){

			// Start the fetch operation

			if (!( request_fetch (writer, relay_time_lo, relay_time_hi, relay_id) )) {
				throw new RuntimeException ("RelayThread.run_fetch_and_wait: Unable to start fetch operation");
			}

			// Wait for operation to complete

			int fetch_status = get_ri_fetch_status();

			while (is_fetch_active (fetch_status)) {
				try {
					Thread.sleep (2000L);
				} catch (InterruptedException e) {
				}
				fetch_status = get_ri_fetch_status();
			}

			// If it did not finish normally ...

			if (fetch_status != FISTAT_FINISHED) {

				// Get the exception, if any

				Exception fetch_exception = get_ri_fetch_exception();

				// Throw exception

				if (fetch_exception != null) {
					throw new RuntimeException ("RelayThread.run_fetch_and_wait: Fetch operation failed, status = " + fetch_status, fetch_exception);
				}
				throw new RuntimeException ("RelayThread.run_fetch_and_wait: Fetch operation failed, status = " + fetch_status);
			}

			// Check for write complete

			writer.check_write_complete();
		}

		// The input buffer

		ByteArrayInputStream input_buffer = new ByteArrayInputStream (output_buffer.toByteArray());
		output_buffer = null;

		// Create the reader

		MarshalImpDataReader reader = new MarshalImpDataReader (
			new DataInputStream (new GZIPInputStream (input_buffer)),
			true);

		return reader;
	}


	// Run a fetch operation, wait for completion, and then return results as a sorted list.
	// Parameters:
	//  relay_time_lo = Lower limit of relay_time range, or 0L for no limit.
	//  relay_time_hi = Upper limit of relay_time range, or 0L for no limit.
	//  relay_id = List of relay item ids for fetch operation, or null or empty for no restriction.
	// Returns a list that contain the results, sorted in ascending order (earliest first).
	// Throws an exception if the operation cannot be done.
	// This function is intended for testing.

	public List<RelayItem> run_fetch_and_sort (long relay_time_lo, long relay_time_hi, String... relay_id) throws IOException {

		// Allocate the list

		ArrayList<RelayItem> items = new ArrayList<RelayItem>();

		// Run the fetch operation

		try (
			MarshalImpDataReader reader = run_fetch_and_wait (relay_time_lo, relay_time_hi, relay_id);
		){
		
			// Read items and add to list until end of list

			for (;;) {
				RelayItem relit = RelayItem.unmarshal_poly (reader, null);
				if (relit == null) {
					break;
				}
				items.add (relit);
			}

			// Check for read complete

			reader.check_read_complete();
		}

		// Sort the list

		items.sort (new RelayItem.AscendingComparator());

		// Done

		return items;
	}




	//----- Relay thread -----




	// Run the relay thread.

	@Override
	public void run() {

		// Flag indicates if we were ever connected

		boolean was_connected = false;

		try {

			// Set state to connection in progress

			sync_var.set_ri_relay_status (RTSTAT_CONNECTING);

			// Transaction flag and connect options

			String db_handle = sync_var.get_ri_db_handle();

			boolean is_transact = MongoDBUtil.is_transaction_enabled (db_handle);

			int conopt_connect = (is_transact ? MongoDBUtil.CONOPT_SESSION : MongoDBUtil.CONOPT_CONNECT);
			//int conopt_transact = (is_transact ? MongoDBUtil.CONOPT_TRANSACT_ABORT : MongoDBUtil.CONOPT_CONNECT);

			int ddbopt = MongoDBUtil.DDBOPT_SAVE_SET;

			// Connect to MongoDB

			try (
				MongoDBUtil mongo_instance = new MongoDBUtil (conopt_connect, ddbopt, db_handle);
			){

				// Set up a change stream iterator

				try (
					RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
				){

					// Fetch remote server status, this also verifies the connection

					RelayItem init_status = RelayItem.get_first_relay_item (RelayItem.DESCENDING, 0L, 0L, RelaySupport.RI_STATUS_ID);
					if (init_status != null) {
						sync_var.ri_queue_insert (init_status);
						init_status = null;
					}

					// At this point, we might do a fetch to verify the connection, or fetch remote server status

					// Set state indicating connection established

					was_connected = true;
					sync_var.set_ri_relay_status (RTSTAT_RUNNING);

					// Loop until shutdown request or exception

					while (!( sync_var.get_ri_shutdown_req() )) {

						//  // If there is something in the change stream iterator ...
						//  
						//  if (csit.hasNext()) {
						//  
						//  	// If there is room in the queue...
						//  
						//  	if (sync_var.ri_queue_has_room()) {
						//
						//			// Add the item to the queue
						//
						//  		RelayItem csrelit = csit.next();
						//  		sync_var.ri_queue_insert (csrelit);
						//  	}
						//  
						//  	// Otherwise, terminate with a queue full error
						//  
						//  	else {
						//			throw new RelayTerminateException (RTSTAT_QUEUE_OVERFLOW, null);
						//  	}
						//  }

						// If there is room in the queue and there is something in the change stream iterator ...

						if ( sync_var.ri_queue_has_room() && csit.hasNext() ) {

							// Add the item to the queue

							RelayItem csrelit = csit.next();
							sync_var.ri_queue_insert (csrelit);
						}

						// Otherwise, if there is a request to start a fetch operation ...

						else if (sync_var.get_ri_fetch_status() == FISTAT_PENDING) {

							// Do the fetch operation

							do_fetch (csit);
						}

						// Otherwise, delay to next polling time

						else {
							sleep_checked (sync_var.get_ri_polling_interval());
						}

					}	// end loop until shutdown

				}	// close change stream iterator

			}	// Close connection to MongoDB

		}

		// Exception indicates queue full or other internal error

		catch (RelayTerminateException e) {
			sync_var.set_relay_exit_status (e.term_relay_status, e.term_relay_exception);
			return;
		}

		// Exception indicates connection lost or never established

		catch (Exception e) {
			sync_var.set_relay_exit_status (was_connected ? RTSTAT_CONNECT_LOSS : RTSTAT_CONNECT_FAIL, e);
			return;
		}
		
		catch (Throwable e) {
			sync_var.set_relay_exit_status (was_connected ? RTSTAT_CONNECT_LOSS : RTSTAT_CONNECT_FAIL, null);
			return;
		}

		// Normal termination

		sync_var.set_relay_exit_status (RTSTAT_SHUTDOWN, null);
		return;
	}




	// Do a fetch operation.
	// Parameters:
	//  csit = Change stream iterator.
	// This function should be called when the fetch status is FISTAT_PENDING,
	// after checking that the change stream iterator has no data.
	// On return, fetch status is inactive.
	// An exception should be treated as a connection failure.

	private void do_fetch (RecordIterator<RelayItem> csit) {

		// Set fetch status to fetch in progress

		sync_var.set_ri_fetch_status (FISTAT_FETCHING);

		// Iterate over relay items

		try (

			// Get an iterator over matching relay items

			RecordIterator<RelayItem> items = RelayItem.fetch_relay_item_range (
					RelayItem.UNSORTED, sync_var.get_ri_fetch_relay_time_lo(), sync_var.get_ri_fetch_relay_time_hi(), sync_var.get_ri_fetch_relay_id());
		){

			// True if we are polling the change stream iterator

			boolean f_polling = false;

			// Time of next change stream poll

			long next_poll_time = System.currentTimeMillis() + sync_var.get_ri_polling_interval();

			// Iterate over returned items

			for (RelayItem relit : items) {

				// Write out the relay item

				try {
					RelayItem.marshal_poly (sync_var.get_ri_fetch_writer(), null, relit);
				}

				// Handle exception during write

				catch (Exception e) {
					sync_var.set_fetch_completion_status (FISTAT_IO_FAIL, e);
					return;
				}

				// Handle cancel request

				if (sync_var.get_ri_fetch_cancel_req()) {
					sync_var.set_fetch_completion_status (FISTAT_CANCELED, null);
					return;
				}

				// Handle shutdown request

				if (sync_var.get_ri_shutdown_req()) {
					sync_var.set_fetch_completion_status (FISTAT_EXITED, null);
					return;
				}

				// If we are polling the change stream ...

				if (f_polling) {

					// If there is room in the queue and there is something in the change stream iterator ...

					if ( sync_var.ri_queue_has_room() && csit.hasNext() ) {

						// Add the item to the queue

						RelayItem csrelit = csit.next();
						sync_var.ri_queue_insert (csrelit);
					}

					// Otherwise, reset the polling time

					else {
						f_polling = false;
						next_poll_time = System.currentTimeMillis() + sync_var.get_ri_polling_interval();
					}
				}

				// Otherwise, check if it is time to start the poll ...

				else {
					if (System.currentTimeMillis() >= next_poll_time) {
						f_polling = true;
					}
				}
			}

			// Write a null item to mark end-of-file

			try {
				RelayItem.marshal_poly (sync_var.get_ri_fetch_writer(), null, null);
			}

			// Handle exception during write

			catch (Exception e) {
				sync_var.set_fetch_completion_status (FISTAT_IO_FAIL, e);
				return;
			}

			// Successful completion

			sync_var.set_fetch_completion_status (FISTAT_FINISHED, null);
		}
						
		return;
	}




	// Sleep for given amount of time, in milliseconds, but wake early in case of shutdown request.

	private void sleep_checked (long delay) {

		// Handle zero or negative delay

		if (delay <= 0L) {
			return;
		}

		// Quantum of delay

		long quantum = 15000L;			// 15 seconds

		// Number of intervals

		long n = (delay + quantum - 1L) / quantum;

		// Length of each interval

		long m = delay / n;

		// Number of intervals that need to be longer

		long k = delay % n;

		// Loop over intervals

		for (long i = 0L; i < n; ++i) {
		
			// If shutdown requested, exit

			if (sync_var.get_ri_shutdown_req()) {
				break;
			}

			// Get current delay interval

			long interval = m;
			if (i < k) {
				++interval;
			}

			// Sleep for this interval

			try {
				Thread.sleep (interval);
			} catch (InterruptedException e) {
			}

		}

		return;
	}




	//----- Construction -----


	// Default constructor.
	// Note: The constructor and setup() leave this object in exactly the same state.

	public RelayThread () {

		sync_var = new RelaySyncVar();

		relay_java_thread = null;

	}


	// Set up (initialize or re-initialize).
	// This establishes default parameter values.
	// Note: The constructor and setup() leave this object in exactly the same state.

	public void setup () {

		sync_var.setup();

		relay_java_thread = null;
		
		return;
	}

}
