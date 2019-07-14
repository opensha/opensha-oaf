package org.opensha.oaf.aafs;

import java.util.ArrayDeque;
import java.util.Queue;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;


/**
 * Thread for pulling relay items from the partner server.
 * Author: Michael Barall 08/05/2018.
 */
public class RelayThread implements Runnable {

	//----- Constants -----




	//----- Synchronized variables -----
	//
	// These are variables that can be accessed from both the dispatcher thread and the relay thread.


	private static class RelaySyncVar {


		//--- Parameters

		// Maximum allowed size of the relay item queue.

		private int max_ri_queue_size;

		public synchronized int get_max_ri_queue_size () {
			return max_ri_queue_size;
		}

		public synchronized void set_max_ri_queue_size (int the_max_ri_queue_size) {
			max_ri_queue_size = the_max_ri_queue_size;
			return;
		}

		// The database handle from which relay items are retrieved.

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
			ri_polling_interval = the_ri_polling_interval;
			return;
		}

		// Minimum amount of time to wait between reconnection attempts, in milliseconds.

		private long ri_reconnect_delay_min;

		public synchronized long get_ri_reconnect_delay_min () {
			return ri_reconnect_delay_min;
		}

		public synchronized void set_ri_reconnect_delay_min (long the_ri_reconnect_delay_min) {
			ri_reconnect_delay_min = the_ri_reconnect_delay_min;
			return;
		}

		// Maximum amount of time to wait between reconnection attempts, in milliseconds.

		private long ri_reconnect_delay_max;

		public synchronized long get_ri_reconnect_delay_max () {
			return ri_reconnect_delay_max;
		}

		public synchronized void set_ri_reconnect_delay_max (long the_ri_reconnect_delay_max) {
			ri_reconnect_delay_max = the_ri_reconnect_delay_max;
			return;
		}

		// The interval between queries when connection is first established, in milliseconds.

		private long ri_initial_query_interval;

		public synchronized long get_ri_initial_query_interval () {
			return ri_initial_query_interval;
		}

		public synchronized void set_ri_initial_query_interval (long the_ri_initial_query_interval) {
			ri_initial_query_interval = the_ri_initial_query_interval;
			return;
		}

		// The interval between queries, after the initial burst, in milliseconds.

		private long ri_query_interval;

		public synchronized long get_ri_query_interval () {
			return ri_query_interval;
		}

		public synchronized void set_ri_query_interval (long the_ri_query_interval) {
			ri_query_interval = the_ri_query_interval;
			return;
		}

		// The time to look back before the current completion time, in milliseconds.

		private long ri_query_lookback_time;

		public synchronized long get_ri_query_lookback_time () {
			return ri_query_lookback_time;
		}

		public synchronized void set_ri_query_lookback_time (long the_ri_query_lookback_time) {
			ri_query_lookback_time = the_ri_query_lookback_time;
			return;
		}

		// The number of queries in the initial burst after the thread is started.

		private int ri_initial_query_count;

		public synchronized int get_ri_initial_query_count () {
			return ri_initial_query_count;
		}

		public synchronized void set_ri_initial_query_count (int the_ri_initial_query_count) {
			ri_initial_query_count = the_ri_initial_query_count;
			return;
		}

		// The number of queries which are presumed to read all relay items.

		private int ri_complete_query_count;

		public synchronized int get_ri_complete_query_count () {
			return ri_complete_query_count;
		}

		public synchronized void set_ri_complete_query_count (int the_ri_complete_query_count) {
			ri_complete_query_count = the_ri_complete_query_count;
			return;
		}




		//--- Communication variables

		// Flag indicating that a shutdown request has been received.

		private boolean ri_shutdown_req;

		public synchronized boolean get_ri_shutdown_req () {
			return ri_shutdown_req;
		}

		public synchronized void set_ri_shutdown_req (boolean the_ri_shutdown_req) {
			ri_shutdown_req = the_ri_shutdown_req;
			return;
		}

		// The time before which the local relay item table is considered complete, in milliseconds since the epoch.

		private long ri_complete_time;

		public synchronized long get_ri_complete_time () {
			return ri_complete_time;
		}

		public synchronized void set_ri_complete_time (long the_ri_complete_time) {
			ri_complete_time = Math.max (1L, the_ri_complete_time);

			// Also clear the maximum time queue so this value cannot be overriden until after several queries.
			ri_query_maxtime_internal_clear();
			return;
		}

		// Flag indicating that the relay thread is currently connected to the partner server (with hysteresis).

		private boolean ri_is_connected;

		public synchronized boolean get_ri_is_connected () {
			return ri_is_connected;
		}

		public synchronized void set_ri_is_connected (boolean the_ri_is_connected) {
			ri_is_connected = the_ri_is_connected;
			return;
		}

		// Flag indicating that the relay thread is currently polling.

		private boolean ri_is_polling;

		public synchronized boolean get_ri_is_polling () {
			return ri_is_polling;
		}

		public synchronized void set_ri_is_polling (boolean the_ri_is_polling) {
			ri_is_polling = the_ri_is_polling;
			return;
		}




		//--- Relay item queue

		// Queue used to send relay items from the relay thread to the dispatcher thread.

		private ArrayDeque<RelayItem> ri_queue;


		// Return the size of the relay item queue

		public synchronized int ri_queue_size () {
			return ri_queue.size();
		}


		// Return true if the relay item queue is full.

		public synchronized boolean ri_queue_is_full () {
			boolean f_result = false;
			if (ri_queue.size() >= max_ri_queue_size) {
				f_result = true;
			}
			return f_result;
		}


		// Return true if the relay item queue is non-empty.

		//public synchronized boolean ri_queue_has_data () {
		//	boolean f_result = false;
		//	if (ri_queue.size() > 0) {
		//		f_result = true;
		//	}
		//	return f_result;
		//}


		// Insert an item in the relay item queue.
		// The insertion proceeds regardless of the current size of the queue.

		public synchronized void ri_queue_insert (RelayItem relit) {
			ri_queue.add (relit);
			return;
		}


		// Remove an item from the relay item queue.
		// Returns null if the queue is empty.
		// Note: If the queue is or becomes empty, then ri_complete_time is updated
		//  from the query maximum time queue.

		public synchronized RelayItem ri_queue_remove () {
			RelayItem relit = ri_queue.poll();
			ri_query_maxtime_apply();
			return relit;
		}


		// Clear the relay item queue.

		//public synchronized void ri_queue_clear () {
		//	ri_queue.clear();
		//	return;
		//}




		//--- Query maximum time queue

		// Queue used to hold maximum time seen in recent queries, each in milliseconds since the epoch.

		private ArrayDeque<Long> ri_query_maxtime;


		// Clear the query maximum time queue.

		//public synchronized void ri_query_maxtime_clear () {
		//	ri_query_maxtime.clear();
		//	return;
		//}


		// Insert a maximum time into the queue.
		// If after insertion the queue size is greater or equal to ri_complete_query_count, then
		//  the oldest elements are popped from the queue and applied to ri_complete_time.

		public synchronized void ri_query_maxtime_insert (long maxtime) {
			ri_query_maxtime.add (maxtime);
			ri_query_maxtime_apply();
			return;
		}


		// Apply items from the query maximum time queue to ri_complete_time (internal subroutine).
		// Items are applied only when the relay item queue is empty, so that changes in
		//  ri_complete_time are not visible to the dispatcher thread until after the
		//  relevant relay items have been processed by the dispatcher thread.

		private void ri_query_maxtime_apply () {
			if (ri_queue.isEmpty()) {
				while (ri_query_maxtime.size() >= ri_complete_query_count) {
					long new_complete_time = ri_query_maxtime.poll();
					ri_complete_time = Math.max (ri_complete_time, new_complete_time);
				}
			}
		}

		// Clear the query maximum time queue (internal subroutine).

		private void ri_query_maxtime_internal_clear () {
			ri_query_maxtime.clear();
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

			max_ri_queue_size = 1000;
			ri_db_handle = null;
			ri_polling_interval = 30000L;				// 30 seconds
			ri_reconnect_delay_min = 30000L;			// 30 seconds
			ri_reconnect_delay_max = 300000L;			// 5 minutes
			ri_initial_query_interval = 300000L;		// 5 minutes
			ri_query_interval = 3600000L;				// 1 hour
			ri_query_lookback_time = 86400000L;			// 24 hours
			ri_initial_query_count = 3;
			ri_complete_query_count = 3;

			ri_shutdown_req = false;
			ri_complete_time = 1L;
			ri_is_connected = false;
			ri_is_polling = false;

			ri_queue = new ArrayDeque<RelayItem>();

			ri_query_maxtime = new ArrayDeque<Long>();

			return;
		}


		// Prepare to start thread.
		// This performs initialization needed prior to starting or re-starting the thread,
		// without erasing parameter values.

		public synchronized void prepare () {


			ri_shutdown_req = false;
			ri_is_connected = false;
			ri_is_polling = false;

			ri_queue = new ArrayDeque<RelayItem>();

			ri_query_maxtime = new ArrayDeque<Long>();

			return;
		}


	}

	private RelaySyncVar sync_var;




	//----- Internal variables -----


	// The Java thread that is running the relay thread, or null if relay thread is not running.

	private Thread relay_java_thread;




	//----- Service functions (callable from the dispatcher thread) -----


	// Remove an item from the relay item queue.
	// Returns null if the queue is empty.

	public RelayItem ri_queue_remove () {
		return sync_var.ri_queue_remove();
	}


	// Get the time before which the local relay table is considered complete, in milliseconds since the epoch.

	public long get_ri_complete_time () {
		return sync_var.get_ri_complete_time();
	}


	// Set the time before which the local relay table is considered complete, in milliseconds since the epoch.
	// Note: As queries are performed, this time is automatically updated.

	public void set_ri_complete_time (long the_ri_complete_time) {
		sync_var.set_ri_complete_time (the_ri_complete_time);
		return;
	}

	
	// Get flag indicating that the relay thread is currently connected to the partner server (with hysteresis).

	//public boolean get_ri_is_connected () {
	//	return sync_var.get_ri_is_connected();
	//}


	// Set maximum allowed size of the relay item queue.

	public void set_max_ri_queue_size (int the_max_ri_queue_size) {
		sync_var.set_max_ri_queue_size (the_max_ri_queue_size);
		return;
	}


	// Set the database handle from which relay items are retrieved.

	public void set_ri_db_handle (String the_ri_db_handle) {
		sync_var.set_ri_db_handle (the_ri_db_handle);
		return;
	}


	// Set the polling interval, in milliseconds.

	public void set_ri_polling_interval (long the_ri_polling_interval) {
		sync_var.set_ri_polling_interval (the_ri_polling_interval);
		return;
	}


	// Set the interval between queries when connection is first established, in milliseconds.

	public void set_ri_initial_query_interval (long the_ri_initial_query_interval) {
		sync_var.set_ri_initial_query_interval (the_ri_initial_query_interval);
		return;
	}


	// Set the interval between queries, after the initial burst, in milliseconds.

	public void set_ri_query_interval (long the_ri_query_interval) {
		sync_var.set_ri_query_interval (the_ri_query_interval);
		return;
	}


	// Set the time to look back before the current completion time, in milliseconds.

	public void set_ri_query_lookback_time (long the_ri_query_lookback_time) {
		sync_var.set_ri_query_lookback_time (the_ri_query_lookback_time);
		return;
	}


	// Start the relay thread.
	// Throws an exception if the thread already exists.

	public void start_relay_thread () {
		if (relay_java_thread != null) {
			throw new IllegalStateException ("RelayThread.start_relay_thread: Thread is already started");
		}
		sync_var.prepare();
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
			try {
				the_thread.join();
			} catch (InterruptedException e) {
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




	//----- Relay thread -----




	// Run the relay thread.

	@Override
	public void run() {

		// Time of most recent restart, 0 if none so far

		long restart_time = 0L;

		// Transaction flag and connect options

		String db_handle = sync_var.get_ri_db_handle();

		boolean is_transact = MongoDBUtil.is_transaction_enabled (db_handle);

		int conopt_connect = (is_transact ? MongoDBUtil.CONOPT_SESSION : MongoDBUtil.CONOPT_CONNECT);
		//int conopt_transact = (is_transact ? MongoDBUtil.CONOPT_TRANSACT_ABORT : MongoDBUtil.CONOPT_CONNECT);

		int ddbopt = MongoDBUtil.DDBOPT_SAVE_SET;

		// Restart loop, continue until shutdown or failure

		for (;;) {

			// Connect to MongoDB

			try (
				MongoDBUtil mongo_instance = new MongoDBUtil (conopt_connect, ddbopt, db_handle);
			){

				// Set up a change stream iterator

				try (
					RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
				){

					// Time when next query occurs

					long next_query_time = 0L;

					// Number of initial queries remaining to be performed

					int initial_query_count = sync_var.get_ri_initial_query_count();

					// Minimum relay time for query, or 0 if no query in progress

					long relay_time_lo = 0L;

					// Loop until shutdown request or exception

					while (!( sync_var.get_ri_shutdown_req() )) {

						// True if we need to delay before next loop

						boolean f_need_delay = true;

						// If there is room in the queue ...

						if (!( sync_var.ri_queue_is_full() )) {

							// Move items from change stream to the queue, up to a limit

							int n = sync_var.get_max_ri_queue_size();

							while (n != 0 && csit.hasNext()) {
								RelayItem csrelit = csit.next();
								sync_var.ri_queue_insert (csrelit);
								--n;
							}

							// If stopped because of limit, then no delay

							if (n == 0) {
								f_need_delay = false;
							}

							// Get the current time

							long current_time = System.currentTimeMillis();

							// If query not started yet ...

							if (relay_time_lo == 0L) {

								// If at or after the next scheduled query, initialize the relay time

								if (current_time >= next_query_time) {
									relay_time_lo = Math.max (1L, sync_var.get_ri_complete_time() - sync_var.get_ri_query_lookback_time());
								}
							}

							// If we want to do a query ...

							if (relay_time_lo != 0L) {

								// Limit on number of items to fetch

								n = sync_var.get_max_ri_queue_size();

								try (

									// Get an iterator over relay items

									RecordIterator<RelayItem> items = RelayItem.fetch_relay_item_range (RelayItem.ASCENDING, relay_time_lo, 0L);
								){
									// Move items from iterator to the queue, up to a limit

									while (n != 0 && items.hasNext()) {
										RelayItem relit = items.next();
										sync_var.ri_queue_insert (relit);

										// Count down, but not to zero if relay time has not changed

										if (!( n == 1 && relit.get_relay_time() == relay_time_lo )) {
											--n;
										}

										// New maximum time

										relay_time_lo = relit.get_relay_time();
									}

								}	// close iterator over relay items

								// If stopped because of limit, then no delay

								if (n == 0) {
									f_need_delay = false;
								}

								// If exhausted iterator, save new maximum time and establish next query time

								else {

									// Save maximum time seen, and indicate no query in progress

									sync_var.ri_query_maxtime_insert (relay_time_lo);
									relay_time_lo = 0L;

									// Count initial queries

									if (initial_query_count > 0) {
										--initial_query_count;
									}

									// Get next query time, using short interval if the next query is an initial query

									next_query_time = current_time + ( (initial_query_count > 0) ?
												(sync_var.get_ri_initial_query_interval()) : (sync_var.get_ri_query_interval()) );
								}

							}	// end if doing a query

						}	// end if there is room in the queue

						// If we need a delay ...

						if (f_need_delay) {

							// Wait for the polling interval
						
							sleep_checked (sync_var.get_ri_polling_interval());

						}	// end if need a delay

					}	// end loop until shutdown

				}	// close change stream iterator

			// Close connection to MongoDB

			// Exception indicates connection lost

			} catch (Exception e) {
				//e.printStackTrace();
				//sg.log_sup.report_dispatcher_exception (task, e);
			} catch (Throwable e) {
				//e.printStackTrace();
				//sg.log_sup.report_dispatcher_exception (task, e);
			}

			// If shutdown request, exit the restart loop

			if (sync_var.get_ri_shutdown_req()) {
				break;
			}

			// Calculate restart delay:
			// restart time is ri_reconnect_delay_min after the current time,
			// or ri_reconnect_delay_max after the last restart, whichever is later

			long restart_delay = Math.max (sync_var.get_ri_reconnect_delay_min(),
						sync_var.get_ri_reconnect_delay_max() + restart_time - System.currentTimeMillis());

			// Wait until time for restart

			sleep_checked (restart_delay);

			// New restart time

			restart_time = System.currentTimeMillis();

			// If shutdown request, exit the restart loop

			if (sync_var.get_ri_shutdown_req()) {
				break;
			}

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
