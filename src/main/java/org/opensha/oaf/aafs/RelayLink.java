package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;

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
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Link management functions for server relay.
 * Author: Michael Barall 05/30/2019.
 */
public class RelayLink extends ServerComponent {


	//=====[ Parameters ]=====


	// The interval between heartbeats, in milliseconds.

	public long heartbeat_interval = 300000L;			// 5 minutes

	// The time after which heartbeat is considered stale, in milliseconds.

	public long heartbeat_stale = 1800000L;				// 30 minutes

	// The short retry interval for repeating calls, in milliseconds.

	//public long call_retry_short_interval = 300000L;	// 5 minutes
	public long call_retry_short_interval = 120000L;	// 2 minutes

	// The long retry interval for repeating calls, in milliseconds.

	//public long call_retry_long_interval = 900000L;	// 15 minutes
	public long call_retry_long_interval = 300000L;	// 5 minutes

	// The number of short call retries.
	// (Value 3L means that after an initial call, try 3 more times quickly, then more slowly afterwards.)

	public long call_retry_short_limit = 3L;

	// The number of call retries after which connection is considered lost or unobtainable.
	// (Value 1L means that after an initial call plus 1 retry, the connection is lost.)

	public long call_retry_loss_conn = 1L;

	// The resync interval, in milliseconds.

	public long resync_interval = 1800000L;	// 30 minutes

	// The short resync lookback time, in milliseconds.

	public long resync_short_lookback = 86400000L;	// 24 hours

	// The long resync lookback time, in milliseconds.

	public long resync_long_lookback = 63072000000L;	// 2 years

	// The number of resyncs in a cycle, with one long lookback per cycle.
	// (Value 48 means that after the initial sync and after the quick resyncs controlled by
	// resync_quick_count, each 48-th sync uses resync_long_lookback, others use resync_short_lookback.)

	public int resync_cycle_length = 48;	// 24 hours

	// The quick resync interval, in milliseconds; used when connection is first established.

	public long resync_quick_interval = 600000L;	// 10 minutes

	// The number of quick resyncs when connection is first established.
	// (Value 2 means that after initial sync, do 2 additional syncs, spaced resync_quick_interval
	// apart, each using resync_long_lookback.)

	public int resync_quick_count = 2;

	// Timeout for primary state negotiation at startup.

	public long prist_init_timeout = 600000L;		// 10 minutes




	// Get the time after which heartbeat is considered stale, in milliseconds.

	public static long get_heartbeat_stale () {
		return 1800000L;				// 30 minutes
	}




	//=====[ Globals ]=====




	//----- Status structures -----




	// Our current server status.
	// This is considered as volatile.  At startup, it is created anew, not read from the database.
	// It is stored in the database as a reley item, so it is visible to the partner server and
	// to management tools.
	// This is null before initialization and after shutdown.
	
	private RiServerStatus local_status;


	// The status received from the partner server.
	// The is the status most recently received from the partner server.
	// Unlike other relay items, items received from the partner server are not stored in the database.
	// This is null in the following link states: shutdown, solo, disconnected, and calling.

	private RiServerStatus remote_status;


	// The relay item received from the partner server, containing its server status.
	// The relay item is retained here so ordering of remote statuses can be done.
	// The payload of this relay item is in remote_status.
	// This is null in the following link states: shutdown, solo, disconnected, and calling.

	private RelayItem remote_status_item;




	//----- Status change variables -----




	// A flag that indicates if the status has changed.
	// If true, then local_status needs to be written to the database.

	private boolean f_status_changed;

	// A flag that indicates if significant work has been done.
	// Currently, we set this flag when the local database is touched.

	private boolean f_did_work;




	//----- Status change functions -----




	// Get the flag indicating if local_status is changed.

	private boolean is_status_changed () {
		return f_status_changed;
	}

	// Clear the flag indicating if local_status is changed.
	// This should be called after writing server status to the database.

	private void clear_status_changed () {
		f_status_changed = false;
		return;
	}

	// Set the flag indicating local_status is changed.

	private void set_status_changed () {
		f_status_changed = true;
		return;
	}




	// Get the flag indicating if significant work has been done.

	private boolean is_did_work () {
		return f_did_work;
	}

	// Clear the flag indicating if significant work has been done.

	private void clear_did_work () {
		f_did_work = false;
		return;
	}

	// Set the flag indicating significant work has been done.

	private void set_did_work () {
		f_did_work = true;
		return;
	}




	// Load the initial local status from the database.
	// Note: An exception likely is a problem accessing the local database.
	// Note: The initial local status is forced into shutdown state.
	// Note: The initial local status is not written to the database here;
	// that is deferred until the initial link state is determined.
	// The change flag is set to ensure a write at that time.
	// Note: This also clears the remote status to null.

	private void load_initial_status (long time_now) {

		// No local status at this point

		local_status = null;

		// Get server status from database

		RiServerStatus sstat_payload = new RiServerStatus();
		RelayItem sstat_item = sg.relay_sup.get_sstat_relay_item (sstat_payload);

		// If we got it, fill in fixed fields and shutdown state

		if (sstat_item != null) {
			sstat_payload.fill_fixed_info();
			sstat_payload.fill_shutdown_state();
		}

		// Otherwise, initialize to default (solo primary)

		else {

			// Use a very small mode_timestamp so the configuration can be overriden by any new one

			sstat_payload.setup (new RelayConfig (1L));
		}

		// Set local status

		local_status = sstat_payload;

		// Mark it changed

		set_status_changed();

		// Dump remote status

		remote_status = null;
		remote_status_item = null;
	
		return;
	}




	// Store local status into the database, if it has changed since the last call.
	// Also generates the heartbeat.

	private void store_local_status (long time_now) {

		// If local status has changed, or it's time for a heartbeat ...

		if (is_status_changed() || is_heartbeat_needed (time_now)) {

			// Update heartbeat time

			insert_heartbeat_time (time_now);

			// Update health status

			insert_health_status (time_now);
		
			// Store the local status

			long relay_time = time_now;
			boolean f_force = true;
			boolean f_log = true;

			sg.relay_sup.submit_sstat_relay_item (
				time_now,						// relay_time
				true,							// f_force
				true,							// f_log
				local_status);					// sstat_payload

			// Clear the change flag

			clear_status_changed();

			// Set the work flag

			set_did_work();
		}
	
		return;
	}




	// Store local status into the database, after setting it to shutdown state.

	private void store_shutdown_status (long time_now) {

		// Set shutdown state

		local_status.fill_shutdown_state();
		
		// Store the local status

		long relay_time = time_now;
		boolean f_force = true;
		boolean f_log = true;

		sg.relay_sup.submit_sstat_relay_item (
			time_now,						// relay_time
			true,							// f_force
			true,							// f_log
			local_status);					// sstat_payload

		// Clear the change flag

		clear_status_changed();

		// Set the work flag

		set_did_work();
	
		return;
	}




//	// Change the status in the database to shutdown state.
//	// Also updates the fixed fields in the server status.
//
//	private void shutdown_db_status (long time_now) {
//
//		// Get server status from database
//
//		RiServerStatus sstat_payload = new RiServerStatus();
//		RelayItem sstat_item = sg.relay_sup.get_sstat_relay_item (sstat_payload);
//
//		// If we got it, fill in fixed fields and shutdown state
//
//		if (sstat_item != null) {
//			sstat_payload.fill_fixed_info();
//			sstat_payload.fill_shutdown_state();
//		}
//
//		// Otherwise, initialize to default (solo primary)
//
//		else {
//
//			// Use a very small mode_timestamp so the configuration can be overriden by any new one
//
//			sstat_payload.setup (new RelayConfig (1L));
//		}
//		
//		// Store the status
//
//		long relay_time = time_now;
//		boolean f_force = true;
//		boolean f_log = true;
//
//		sg.relay_sup.submit_sstat_relay_item (
//			time_now,						// relay_time
//			true,							// f_force
//			true,							// f_log
//			sstat_payload);					// sstat_payload
//	
//		return;
//	}




	//=====[ Server Information ]=====




	//----- Server information variables -----




	// Software major version.

	// private int local_status.sw_major_version;

	// Software minor version.

	// private int local_status.sw_minor_version;

	// Software build number.

	// private int local_status.sw_build;

	// Relay protocol version number.
	// This is used to verify that the partner servers use compatible protocols.

	// private int local_status.protocol_version;

	// The server number, 1 or 2.

	// private int server_number;




	//----- Protocol compatibility -----




	// Our protocol version number. (Always 3 digits)

	public static final int OUR_PROTOCOL_VERSION		= 101;




	//----- Server information functions -----




	// Get the software major version.

	private int get_sw_major_version () {
		return local_status.sw_major_version;
	}

	// Get the software minor version.

	private int get_sw_minor_version () {
		return local_status.sw_minor_version;
	}

	// Get the software build number.

	private int get_sw_build () {
		return local_status.sw_build;
	}

	// Get the protocol compatibility number.

	private int get_protocol_version () {
		return local_status.protocol_version;
	}

	// Get the server number.

	private int get_server_number () {
		return local_status.server_number;
	}




	//=====[ Heartbeat ]=====




	//----- Heartbeat variables -----




	// The time of the last heartbeat, in milliseconds since the epoch.
	// This is set to the current time whenever the status changes.
	// It is also set to the current time periodically if the status remains unchanged,
	// to signal that the system is alive.

	// private long local_status.heartbeat_time;




	//----- Heartbeat functions -----




	// Set the heartbeat time.
	// Note: This always is considered to be a status change.

	//  public void set_heartbeat_time (long time_now) {
	//  	local_status.heartbeat_time = time_now;
	//  	set_status_changed();
	//  	return;
	//  }

	// Check if it is time for a heartbeat, and impose it if so.

	//  private void check_heartbeat (long time_now) {
	//  	if (time_now >= local_status.heartbeat_time + heartbeat_interval) {
	//  		set_heartbeat_time (time_now);
	//  	}
	//  	return;
	//  }

	// Return true if heartbeat is needed.

	private boolean is_heartbeat_needed (long time_now) {
		if (time_now >= local_status.heartbeat_time + heartbeat_interval) {
			return true;
		}
		return false;
	}

	// Insert the current time into the heartbeat time field.

	private void insert_heartbeat_time (long time_now) {
		local_status.heartbeat_time = time_now;
		return;
	}




	//----- Health status functions -----




	// The subsystem health status flags, see HealthSupport.HS_XXXXX.
	// This field is updated whenever the local status is written to the database.

	// private long local_status.health_status;




	// Insert the current health status into the health status field.

	private void insert_health_status (long time_now) {
		local_status.health_status = sg.health_sup.get_health_status (time_now);
		return;
	}




	//=====[ Link Management ]=====




	//----- Link state variables -----




	// Current link state code, see LINK_XXXXX.

	// private int local_status.link_state;

	// The inferred remote server state, see ISR_XXXXX.

	// private int local_status.inferred_state;

	// The thread for pulling relay items from the partner server.
	// Null if no thread currently exists.

	private RelayThread relay_thread;

	// Output buffer for currently pending fetch operation, or null if none.

	private ByteArrayOutputStream fetch_output_buffer;

	// Writer for currently pending fetch operation, or null if none.

	private MarshalImpDataWriter fetch_writer;

	// The time at which the next call will occur, or 0L if not set yet.

	private long link_next_call_time;

	// The number of call attempts that have occurred, without a successful connection.
	// This is set to 0 at initialization, and when successfully completing a call,
	// and when in shutdown or solo mode.
	// It is incremented when entering the disconnected state.
	// This is used for two purposes:
	// - It selects the time to wait before the next call attempt, based on pre-incremented value:
	//   A value of 0 selects an immediate call (on the next poll).
	//   A positive value <= call_retry_short_limit selects call after a short interval.
	//   A value > call_retry_short_limit selects call after a long interval.
	// - It determines if enough calls have occurred so the connection is considered lost,
	//   based on the post-incremented value:
	//   A value > call_retry_loss_conn + 1 means connection is lost or unobtainable.

	private long link_call_retry_count;

	// The result of the last failed call attempt, one of the ISR_XXXX values.
	// It contains the reason why the call failed.
	// It is ISR_DEAD_NOT_CALLABLE if no call has occurred since initialization,
	// or re-initialization, or if the most recent call succeeded.
	// It should be set to ISR_DEAD_NOT_CALLABLE whenever link_call_retry_count is set to zero.

	private int link_failed_call_reason;

	// The time at which the next resync will occur, or 0L if not set yet.

	private long link_next_resync_time;

	// The counter used for the resync cycle.
	// It is initialized to -resync_quick_count when connection is established.
	// It is incremented at the end of each fetch, wrapping aroung from resync_cycle_length to 0.
	// This is used for two purposes:
	// - It selects 
	// Values <= 0 select the long resync lookback.
	// Values < 0 select the quick resync interval.

	private int link_resync_cycle_counter;




	//----- Link states -----




	// Link state values. (Always 3 digits)

	public static final int LINK_MIN					= 101;
	public static final int LINK_SHUTDOWN				= 101;
		// The server is currently shut down.  This state is set when the
		// dispatcher exits.
	public static final int LINK_SOLO					= 102;
		// The server is running in standalone mode.  No attempt is made to
		// communicate with a partner server.  This state is set when the
		// server is configured to run in standalone mode.
	public static final int LINK_DISCONNECTED			= 103;
		// There is no connection to a partner server, and the relay thread does
		// not exist or is shut down.  This is the initial state for a server in
		// watch or pair mode.  On entry to this state, a timer is set for when the
		// next connection attempt will be made, which is "now" at initial entry,
		// or after a retry interval if this state is entered due to loss of
		// connection.  Receipt of a new configuration also sets the timer to
		// "now" so the system will respond rapidly to configuration changes.
	public static final int LINK_CALLING				= 104;
		// The relay thread has been created, and a connection to the partner
		// MongoDB is being created.
	public static final int LINK_INITIAL_SYNC			= 105;
		// The relay thread has successfully established a connection to MongoDB.
		// The server is currently performing an initial sync by reading relay
		// items from the partner server.
	public static final int LINK_CONNECTED				= 106;
		// The relay thread has a connection to MongoDB, and the initial sync
		// is complete.
	public static final int LINK_RESYNC					= 107;
		// The relay thread has a connection to MongoDB, and a re-sync is in
		// progress.  Re-sync is done periodically to pick up any relay items
		// not delivered through the change stream.
	public static final int LINK_MAX					= 107;


	// Return a string describing a link state.

	public static String get_link_state_as_string (int state) {
		switch (state) {
		case LINK_SHUTDOWN: return "LINK_SHUTDOWN";
		case LINK_SOLO: return "LINK_SOLO";
		case LINK_DISCONNECTED: return "LINK_DISCONNECTED";
		case LINK_CALLING: return "LINK_CALLING";
		case LINK_INITIAL_SYNC: return "LINK_INITIAL_SYNC";
		case LINK_CONNECTED: return "LINK_CONNECTED";
		case LINK_RESYNC: return "LINK_RESYNC";
		}
		return "LINK_INVALID(" + state + ")";
	}

	// Test if user string matches the link state.
	// Parameters:
	//  s = User string.
	//  v_link_state = Link state value to test.
	// Returns one of the following:
	//  -1 = Invalid user string.
	//  0 = Not a match.
	//  1 = Match.
	// Note: If check_user_string_link_state returns true for the user string,
	// then this function will return 0 or 1.

	public static int test_user_string_link_state (String s, int v_link_state) {
		int result = -1;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = 1;
		}
		else if (s.equalsIgnoreCase ("unlinked")) {
			switch (v_link_state) {
			case LINK_SHUTDOWN:
			case LINK_SOLO:
			case LINK_DISCONNECTED:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("linking")) {
			switch (v_link_state) {
			case LINK_CALLING:
			case LINK_INITIAL_SYNC:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("linked")) {
			switch (v_link_state) {
			case LINK_CONNECTED:
			case LINK_RESYNC:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("shutdown")) {
			switch (v_link_state) {
			case LINK_SHUTDOWN:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("solo")) {
			switch (v_link_state) {
			case LINK_SOLO:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("disconnected")) {
			switch (v_link_state) {
			case LINK_DISCONNECTED:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
	
		return result;
	}

	// Check for valid user string for matching the configured primary.
	// Parameters:
	//  s = User string.
	// Returns true if valid, false if not.

	public static boolean check_user_string_link_state (String s) {
		boolean result = false;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("unlinked")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("linking")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("linked")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("shutdown")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("solo")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("disconnected")) {
			result = true;
		}
	
		return result;
	}




	// Inferred remote state values. (Always 3 digits)

	public static final int ISR_MIN						= 101;
	public static final int ISR_NONE					= 101;
		// There is no information about the remote server state, because
		// this server in a state which does not obtain remote server
		// information (shutdown or solo).
	public static final int ISR_OK						= 102;
		// The remote server is inferred to have a normal connection with
		// this server.
	public static final int ISR_NOT_SUPPLIED			= 103;
		// The server status read from the database does not contain an
		// inferred remote state.
	public static final int ISR_LISTENING				= 104;
		// The remote server is inferred to be listening for an incoming
		// connection.
		// This is reported during the first few attempts to establish or
		// re-establish a connection.
	public static final int ISR_DEAD_UNREACHABLE		= 105;
		// The remote server is inferred to be dead because it is
		// unreachable, or not running MongoDB, or unable to accept a
		// connection to MongoDB.
		// This can be reported after the first few attempts to establish
		// or re-establish a connection.
	public static final int ISR_DEAD_BAD_STATE			= 106;
		// The remote server is inferred to be dead because it is in a
		// state that prevents this server from connecting to it
		// (see RiServerStatus.is_connectable()).
		// This can be reported after the first few attempts to establish
		// or re-establish a connection.
	public static final int ISR_DEAD_BAD_SYNC_DATA		= 107;
		// The remote server is inferred to be dead because its database
		// contents does not allow this server to complete synchronization.
		// This can be reported after the first few attempts to establish
		// or re-establish a connection.
	public static final int ISR_DEAD_NOT_CALLABLE		= 108;
		// The remote server is inferred to be dead because attempts to
		// call it failed for an unspecified reason.
		// This can be reported after the first few attempts to establish
		// or re-establish a connection.
	public static final int ISR_DEAD_STALE_HEARTBEAT	= 109;
		// The remote server is inferred to be dead because it has a stale
		// heartbeat.
		// This can be reported when this server is connected to the remote
		// server, or performing initial sync or re-sync.
	public static final int ISR_UNSYNCED_MODE			= 110;
		// The remote server is inferred to not be synced with this server
		// because it has a different relay configuration.
		// This can be reported when this server is connected to the remote
		// server, or performing initial sync or re-sync.
	public static final int ISR_NOT_CONNECTED			= 111;
		// The remote server is inferred to not be connected with this server
		// because of its link status (disconnected, calling, initial sync).
		// This can be reported when this server is connected to the remote
		// server, or performing initial sync or re-sync.
	public static final int ISR_UNAVAILABLE				= 112;
		// Information about the remote server is unexpectedly not available.
		// This can be reported when this server is connected to the remote
		// server, or performing initial sync or re-sync.
	public static final int ISR_DEAD_BAD_HEALTH			= 113;
		// The remote server is inferred to be dead because it has a bad
		// health status.
		// This can be reported when this server is connected to the remote
		// server, or performing initial sync or re-sync.
	public static final int ISR_MAX						= 113;


	// Return a string describing an inferred remote state.

	public static String get_inferred_state_as_string (int state) {
		switch (state) {
		case ISR_NONE: return "ISR_NONE";
		case ISR_OK: return "ISR_OK";
		case ISR_NOT_SUPPLIED: return "ISR_NOT_SUPPLIED";
		case ISR_LISTENING: return "ISR_LISTENING";
		case ISR_DEAD_UNREACHABLE: return "ISR_DEAD_UNREACHABLE";
		case ISR_DEAD_BAD_STATE: return "ISR_DEAD_BAD_STATE";
		case ISR_DEAD_BAD_SYNC_DATA: return "ISR_DEAD_BAD_SYNC_DATA";
		case ISR_DEAD_NOT_CALLABLE: return "ISR_DEAD_NOT_CALLABLE";
		case ISR_DEAD_STALE_HEARTBEAT: return "ISR_DEAD_STALE_HEARTBEAT";
		case ISR_UNSYNCED_MODE: return "ISR_UNSYNCED_MODE";
		case ISR_NOT_CONNECTED: return "ISR_NOT_CONNECTED";
		case ISR_UNAVAILABLE: return "ISR_UNAVAILABLE";
		case ISR_DEAD_BAD_HEALTH: return "ISR_DEAD_BAD_HEALTH";
		}
		return "ISR_UNKNOWN(" + state + ")";
	}




	//----- Link status functions -----




	// Get the current link state.

	private int get_link_state () {
		return local_status.link_state;
	}

	// Set the link state.
	// The change flag is set if state is changed.
	// Returns true if state changed.

	private boolean set_link_state (int new_link_state) {
		if (local_status.link_state != new_link_state) {
			local_status.link_state = new_link_state;
			set_status_changed();
			return true;
		}
		return false;
	}




	// Get the current inferred state.

	private int get_inferred_state () {
		return local_status.inferred_state;
	}

	// Set the inferred state.
	// The change flag is set if state is changed.
	// Returns true if state changed.

	private boolean set_inferred_state (int new_inferred_state) {
		if (local_status.inferred_state != new_inferred_state) {
			local_status.inferred_state = new_inferred_state;
			set_status_changed();
			return true;
		}
		return false;
	}




	//----- Link subroutines -----




	// Insert a remote relay item into the database, if possible.
	// Parameters:
	//  relit = Relay item from partner server.
	// Returns 1 if relay item was copied into local database.
	// Returns 0 if relay item was not copied into local database.
	// Returns -1 if there was an error interpreting the remote relay item,
	// in which case the appropriate action is to disconnect.
	// An exception is thrown if there is a problem with the local database.

	private int copy_remote_relay_item (RelayItem relit, long time_now) {

		// Switch on relay item type, and unmarshal its payload; unknown types are dropped

		switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

		case RelaySupport.RITYPE_PDL_COMPLETION:
		{
			RiPDLCompletion payload = new RiPDLCompletion();
			try {
				payload.unmarshal_relay (relit);
			} catch (Exception e) {
				return -1;
			}

			// Set the work flag

			set_did_work();

			// Attempt insertion into local database

			if (RelayItem.submit_relay_item (relit, false, -1L) > 0) {
				int log_op = LogSupport.RIOP_COPY;
				String event_id = RelaySupport.pdl_relay_id_to_event_id (relit.get_relay_id());
				long log_relay_time = relit.get_relay_time();
				sg.log_sup.report_pdl_relay_set (log_op, event_id, log_relay_time, payload);
				return 1;
			}
		}
		break;

		case RelaySupport.RITYPE_PDL_REMOVAL:
		{
			RiPDLRemoval payload = new RiPDLRemoval();
			try {
				payload.unmarshal_relay (relit);
			} catch (Exception e) {
				return -1;
			}

			// Set the work flag

			set_did_work();

			// Attempt insertion into local database

			if (RelayItem.submit_relay_item (relit, false, -1L) > 0) {
				int log_op = LogSupport.RIOP_COPY;
				String event_id = RelaySupport.prem_relay_id_to_event_id (relit.get_relay_id());
				long log_relay_time = relit.get_relay_time();
				sg.log_sup.report_prem_relay_set (log_op, event_id, log_relay_time, payload);
				return 1;
			}
		}
		break;

		case RelaySupport.RITYPE_PDL_FOREIGN:
		{
			RiPDLForeign payload = new RiPDLForeign();
			try {
				payload.unmarshal_relay (relit);
			} catch (Exception e) {
				return -1;
			}

			// Set the work flag

			set_did_work();

			// Attempt insertion into local database

			if (RelayItem.submit_relay_item (relit, false, -1L) > 0) {
				int log_op = LogSupport.RIOP_COPY;
				String event_id = RelaySupport.pfrn_relay_id_to_event_id (relit.get_relay_id());
				long log_relay_time = relit.get_relay_time();
				sg.log_sup.report_pfrn_relay_set (log_op, event_id, log_relay_time, payload);
				return 1;
			}
		}
		break;

		case RelaySupport.RITYPE_ANALYST_SELECTION:
		{
			//  RiAnalystSelection payload = new RiAnalystSelection();
			//  try {
			//  	payload.unmarshal_relay (relit);
			//  } catch (Exception e) {
			//  	return -1;
			//  }
			//  
			//  // Set the work flag
			//  
			//  set_did_work();
			//  
			//  // Attempt insertion into local database
			//  
			//  if (RelayItem.submit_relay_item (relit, false, -1L) > 0) {
			//  	int log_op = LogSupport.RIOP_COPY;
			//  	String event_id = RelaySupport.analyst_relay_id_to_event_id (relit.get_relay_id());
			//  	long log_relay_time = relit.get_relay_time();
			//  	sg.log_sup.report_ansel_relay_set (log_op, event_id, log_relay_time, payload);
			//  	return 1;
			//  }

			// Construct the task payload (note it retains relit)

			OpAnalystSelection task_payload = new OpAnalystSelection();
			try {
				task_payload.setup (relit, -1L, OpAnalystSelection.RWOPT_WRITE_NEW);
			} catch (Exception e) {
				return -1;
			}

			// Set the work flag

			set_did_work();

			// If the relay item could be inserted into the database ...

			if (RelayItem.check_relay_item (relit, false) > 0) {

				// Submit the task
				// (Scheduled time is set early so following resynchronization it will
				// execute ahead of any queued forecast or intake tasks)

				PendingTask.submit_task (
					task_payload.event_id,										// event id
					Math.max (relit.get_relay_time() - DURATION_YEAR, EXEC_TIME_SHUTDOWN + 1000L),		// sched_time
					time_now,													// submit_time
					"RelayLink",												// submit_id
					OPCODE_ANALYST_SELECTION,									// opcode
					0,															// stage (must be 0 here!)
					task_payload.marshal_task());								// details
			
				int log_op = LogSupport.RIOP_COPY_TASK;
				String event_id = task_payload.event_id;
				long log_relay_time = relit.get_relay_time();
				sg.log_sup.report_ansel_relay_set (log_op, event_id, log_relay_time, task_payload.ansel_payload);
				return 1;
			}
		}
		break;

		case RelaySupport.RITYPE_SERVER_STATUS:
		{
			RiServerStatus payload = new RiServerStatus();
			try {
				payload.unmarshal_relay (relit);
			} catch (Exception e) {
				return -1;
			}

			// Save status into our variables, if it is newer than the status we have

			if (remote_status_item == null || RelayItem.compare (relit, remote_status_item) > 0) {
				remote_status_item = relit;
				remote_status = payload;

				int log_op = LogSupport.RIOP_COPY;
				long log_relay_time = relit.get_relay_time();
				sg.log_sup.report_sstat_relay_set (log_op, log_relay_time, payload);
				return 1;
			}
		}
		break;
		}
	
		// Did not save item

		return 0;
	}




	// Copy all items in the queue into the database, if possible.
	// Returns the number of items copied.
	// Returns -1 if there was an error interpreting the remote relay item,
	// in which case the appropriate action is to disconnect.
	// An exception is thrown if there is a problem with the local database.

	private int copy_queued_relay_items (long time_now) {
	
		// Number of items copied

		int items_copied = 0;

		// Loop until queue is empty

		for (;;) {

			// Get the first item on the queue

			RelayItem relit = relay_thread.ri_queue_remove();

			// If none, stop

			if (relit == null) {
				break;
			}

			// Copy the item, if possible

			int n = copy_remote_relay_item (relit, time_now);

			// Stop if error

			if (n < 0) {
				return -1;
			}

			// Count the copy

			if (n > 0) {
				++items_copied;
			}
		}

		return items_copied;
	}




	// Begin a fetch operation.
	// Returns true if success, false if operation could not be started.
	// Note: A false return likely indicates loss of connection.
	// Note: Make sure link_resync_cycle_counter is set up.

	private boolean link_fetch_begin (long time_now) {

		long lookback = ( (link_resync_cycle_counter <= 0) ? resync_long_lookback : resync_short_lookback );

		sg.log_sup.report_relay_sync_begin (lookback);

		try {

			// The output buffer

			fetch_output_buffer = new ByteArrayOutputStream();

			// The writer

			fetch_writer = new MarshalImpDataWriter (
							new DataOutputStream (new GZIPOutputStream (fetch_output_buffer)),
							true);

			// Start the fetch operation

			long relay_time_lo = time_now - lookback;
			long relay_time_hi = 0L;

			if (!( relay_thread.request_fetch (fetch_writer, relay_time_lo, relay_time_hi) )) {
				throw new RuntimeException ("RelayLink.link_fetch_begin: Unable to start fetch operation");
			}

		} catch (Exception e) {
			
			// An exception here is an error

			return false;
		}

		// Successful initiation

		return true;
	}




	// End a fetch operation.
	// Returns the number of items copied.
	// Returns -1 if there was an error interpreting the remote relay item,
	// in which case the appropriate action is to disconnect.
	// An exception is thrown if there is a problem with the local database.
	// This function updates link_resync_cycle_counter and link_next_resync_time,
	// provided that the return value is >= 0.
	// This function always clears fetch_output_buffer and fetch_writer.

	private int link_fetch_end (long time_now) {

		int item_count = relay_thread.get_ri_fetch_item_count();
		sg.log_sup.report_relay_sync_copy (item_count);

		int items_processed = 0;
		int items_copied = 0;

		// The output buffer

		ByteArrayOutputStream output_buffer = fetch_output_buffer;
		fetch_output_buffer = null;

		// The writer

		try (
			MarshalImpDataWriter writer = fetch_writer;
		){
			fetch_writer = null;

			// Advance the resync counter and compute the next resync time

			link_next_resync_time = time_now + ( (link_resync_cycle_counter < 0) ? resync_quick_interval : resync_interval );

			++link_resync_cycle_counter;
			if (link_resync_cycle_counter >= resync_cycle_length) {
				link_resync_cycle_counter = 0;
			}

			// Check for write complete

			try {
				writer.check_write_complete();
			} catch (Exception e) {
				return -1;
			}
		}
		catch (IOException e) {
			// Can only be from closing writer
			return -1;
		}

		// Input buffer

		ByteArrayInputStream input_buffer = new ByteArrayInputStream (output_buffer.toByteArray());
		output_buffer = null;

		// Create the reader, note this is entirely in memory

		try (
			MarshalImpDataReader reader = new MarshalImpDataReader (
						new DataInputStream (new GZIPInputStream (input_buffer)),
						true);
		){

			// Read items until we find the terminating null

			for (;;) {
				RelayItem relit;

				try {
					relit = RelayItem.unmarshal_poly (reader, null);
				} catch (Exception e) {
					return -1;
				}
			
				if (relit == null) {
					break;
				}

				// Process the item

				++items_processed;

				// Copy the item, if possible

				int n = copy_remote_relay_item (relit, time_now);

				// Stop if error

				if (n < 0) {
					return -1;
				}

				// Count the copy

				if (n > 0) {
					++items_copied;
				}

				// Now process any items on the queue

				if (copy_queued_relay_items(time_now) < 0) {
					return -1;
				}
			}

			// Check for read complete

			try {
				reader.check_read_complete();
			} catch (Exception e) {
				return -1;
			}
		}
		catch (IOException e) {
			// Can only be from opening or closing reader
			return -1;
		}

		sg.log_sup.report_relay_sync_end (items_processed, items_copied, link_next_resync_time);

		return items_copied;
	}




	// Return true if the remote server is known to be dead.
	// This means one of the following:
	//  Link state is disconnected, and there have been enough call retries to qualify as loss of connection.
	//  Link state is connected, and the remote heartbeat is stale or the remote server is in bad health.
	// Note we do not attempt to make a determination in other link states,
	// because they are transient states.

	private boolean is_remote_known_dead (long time_now) {
	
		// Switch on link state

		switch (get_link_state()) {

		case LINK_DISCONNECTED:

			// If call has failed enough times to qualify as loss of connection, consider it dead

			if (link_call_retry_count > call_retry_loss_conn + 1) {
				return true;
			}
			break;

		case LINK_CONNECTED:

			// If it is not the case that the partner has a current heartbeat and good health, consider it dead

			if (!( remote_status.heartbeat_time >= time_now - heartbeat_stale
				&& HealthSupport.hs_good_status (remote_status.health_status) )) {
				return true;
			}
			break;
		}

		// In all other case, we don't know that it's dead

		return false;
	}




	// Return true if the remote server is known to be alive and synced.
	// This means all of the following:
	//  Link state is connected.
	//  The remote status contains the same relay configuration as local status.
	//  The remote heartbeat is not stale.
	// Note we do not attempt to make a determination in other link states.
	// Note that this function does not look at the remote server health status.

	private boolean is_remote_known_alive_and_synced (long time_now) {
	
		// Switch on link state

		switch (get_link_state()) {

		case LINK_CONNECTED:

			// If partner has the same configuration and a current heartbeat, consider it alive and synced

			if (local_status.is_same_relay_config (remote_status)					// remote server has the same configuration
				&& remote_status.heartbeat_time >= time_now - heartbeat_stale) {	// remote heartbeat is not stale

				return true;
			}
		}

		// In all other case, we don't know that it's alive and synced

		return false;
	}




	// Return true if the remote server is known to be dead or synced.
	// This means one of the following:
	//  Link state is disconnected, and there have been enough call retries to qualify as loss of connection.
	//  Link state is connected, and the remote heartbeat is stale.
	//  Link state is connected, and the remote status contains the same relay configuration as local status.
	// Note we do not attempt to make a determination if the partner is dead in other link states,
	// because they are transient states.
	// Note that this function does not look at the remote server health status.

	private boolean is_remote_known_dead_or_synced (long time_now) {
	
		// Switch on link state

		switch (get_link_state()) {

		case LINK_DISCONNECTED:

			// If call has failed enough times to qualify as loss of connection, consider it dead

			if (link_call_retry_count > call_retry_loss_conn + 1) {
				return true;
			}
			break;

		case LINK_CONNECTED:

			// If it is not the case that the partner has a current heartbeat, consider it dead

			if (!( remote_status.heartbeat_time >= time_now - heartbeat_stale )) {
				return true;
			}

			// If partner has the same configuration, consider it synced

			if (local_status.is_same_relay_config (remote_status)) {
				return true;
			}
			break;
		}

		// In all other case, we don't know that it's dead

		return false;
	}




	// Return true if the link is in the connected or disconnected state.

	private boolean is_link_conn_or_disc () {
	
		// Switch on link state

		switch (get_link_state()) {

		case LINK_DISCONNECTED:
		case LINK_CONNECTED:
			return true;
		}

		return false;
	}




	//----- Link state machine -----




	// Establish the initial link state, which is LINK_SHUTDOWN.
	// Note: This may incur a delay, waiting for the relay thread to terminate.
	// Note: The initial state must be loaded before establishing the initial link state.

	private void set_link_init () {

		// Terminate the relay thread, in case it is active

		relay_thread.terminate_relay_thread();
	
		// Dump any fetch buffer
		// (This is not a resource leak because fetch is buffered in memory)

		fetch_output_buffer = null;
		fetch_writer = null;

		// No remote status

		remote_status = null;
		remote_status_item = null;

		// No call retries

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;

		// Establish shutdown state

		set_link_state (LINK_SHUTDOWN);
		set_inferred_state (ISR_NONE);
		return;
	}




	// Ensure that the link is active (not in LINK_SHUTDOWN or LINK_SOLO state).

	private void ensure_link_active (long time_now) {

		// Change state if currently shutdown or solo

		switch (get_link_state()) {
		case LINK_SHUTDOWN:
		case LINK_SOLO:
			set_link_disconnected (time_now);
			break;
		}
	
		return;
	}




	// Ensure that the next call will occur without a delay.
	// Clear the call retry variables, so that the next call (if needed) will occur immediately.
	// This may be called in any link state.
	// This takes no action except setting local variables; in particular,
	// it does not modify local_status.

	private void ensure_link_quick_call (long time_now) {

		// No call retries

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;
	
		return;
	}




	// Set link state to LINK_SHUTDOWN.
	// Note: This may incur a delay, waiting for the relay thread to terminate.

	private void set_link_shutdown () {

		// If already shutdown, do nothing

		if (get_link_state() == LINK_SHUTDOWN) {
			return;
		}

		// Terminate the relay thread

		relay_thread.terminate_relay_thread();
	
		// Dump any fetch buffer
		// (This is not a resource leak because fetch is buffered in memory)

		fetch_output_buffer = null;
		fetch_writer = null;

		// Dump remote status

		remote_status = null;
		remote_status_item = null;

		// No call retries

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;

		// Establish shutdown state

		set_link_state (LINK_SHUTDOWN);
		return;
	}




	// Set link state to LINK_SOLO.

	private void set_link_solo (long time_now) {

		// If already solo, do nothing

		if (get_link_state() == LINK_SOLO) {
			return;
		}

		// If the thread is active, stop it

		relay_thread.shutdown_relay_thread();
	
		// Dump any fetch buffer
		// (This is not a resource leak because fetch is buffered in memory)

		fetch_output_buffer = null;
		fetch_writer = null;

		// Dump remote status

		remote_status = null;
		remote_status_item = null;

		// No call retries

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;

		// Establish solo state

		set_link_state (LINK_SOLO);
		return;
	}




	// Set link state to LINK_DISCONNECTED.

	private void set_link_disconnected (long time_now) {

		// If already disconnected, do nothing

		if (get_link_state() == LINK_DISCONNECTED) {
			return;
		}

		// If the thread is active, stop it

		relay_thread.shutdown_relay_thread();
	
		// Dump any fetch buffer
		// (This is not a resource leak because fetch is buffered in memory)

		fetch_output_buffer = null;
		fetch_writer = null;

		// Dump remote status

		remote_status = null;
		remote_status_item = null;

		// If first call, make the call on the next poll

		if (link_call_retry_count <= 0L) {
			link_next_call_time = time_now;
		}

		// Otherwise, if not exhausted all short retires, use the short retry interval

		else if (link_call_retry_count <= call_retry_short_limit) {
			link_next_call_time = time_now + call_retry_short_interval;
		}

		// Otherwise, use the long retry interval

		else {
			link_next_call_time = time_now + call_retry_long_interval;
		}

		// Advance the call count

		++link_call_retry_count;

		// Establish disconnected state

		set_link_state (LINK_DISCONNECTED);

		// And immediately poll it

		//poll_link_disconnected (time_now);
		return;
	}




	// Poll link when state is LINK_DISCONNECTED.

	private void poll_link_disconnected (long time_now) {

		// If it is time for the next call ...

		if (time_now >= link_next_call_time) {
			
			// Get the database handle for the partner

			ServerConfig server_config = new ServerConfig();
			int server_number = server_config.get_server_number();
			List<String> server_db_handles = server_config.get_server_db_handles();

			int partner_number = 3 - server_number;
			String partner_db_handle = server_db_handles.get (partner_number);

			// Attempt to call the partner, if unable to initiate then wait for next poll
			// (Inability probably means that a thread shutdown has not completed yet)

			if (relay_thread.start_relay_thread (partner_db_handle, true)) {

				// Call in progress, change state

				set_link_state (LINK_CALLING);
			}
		}
	
		return;
	}




	// Poll link when state is LINK_CALLING.

	private void poll_link_calling (long time_now) {

		// Get the relay thread status
	
		int rtstat = relay_thread.get_ri_relay_status();

		// If the connect attempt has failed, go back to disconnected state

		if (!( RelayThread.is_relay_active (rtstat) )) {
			if (rtstat == RelayThread.RTSTAT_REMOTE_STATUS) {
				link_failed_call_reason = ISR_DEAD_BAD_STATE;
			}
			else if (rtstat == RelayThread.RTSTAT_REMOTE_NO_STATUS) {
				link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			}
			else {
				link_failed_call_reason = ISR_DEAD_UNREACHABLE;
			}
			set_link_disconnected (time_now);
			return;
		}

		// If the thread is not running, then the call is still in progress

		if (rtstat != RelayThread.RTSTAT_RUNNING) {
			return;
		}

		// Get the first item on the queue, it should be remote status

		RelayItem relit = relay_thread.ri_queue_remove();

		// If nothing in queue, assume failure and go back to disconnected state

		if (relit == null) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// If item is not server status, assume failure and go back to disconnected state

		if (!( relit.get_relay_id().equals (RelaySupport.RI_STATUS_ID) )) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// Get the partner server status, if error assume failure and go back to disconnected state

		RiServerStatus sstat_payload = new RiServerStatus();
		try {
			sstat_payload.unmarshal_relay (relit);
		} catch (Exception e) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// If not connectable, go back to disconnected state

		if (!( sstat_payload.is_connectable() )) {
			link_failed_call_reason = ISR_DEAD_BAD_STATE;
			set_link_disconnected (time_now);
			return;
		}
						
		// Save remote status

		remote_status = sstat_payload;
		remote_status_item = relit;

		// Set up fetch for initial sync, if error assume failure and go back to disconnected state

		link_resync_cycle_counter = -resync_quick_count;

		if (!( link_fetch_begin (time_now) )) {

			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// Change to initial sync state

		set_link_state (LINK_INITIAL_SYNC);
		return;
	}




	// Poll link when state is LINK_INITIAL_SYNC.

	private void poll_link_initial_sync (long time_now) {

		// Process any items on the queue

		if (copy_queued_relay_items(time_now) < 0) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// If fetch is not complete, remain in this state

		int fetch_status = relay_thread.get_ri_fetch_status();

		if (RelayThread.is_fetch_active (fetch_status)) {
			return;
		}

		// If it did not finish normally ...

		if (fetch_status != RelayThread.FISTAT_FINISHED) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// End the fetch operation, handle error

		if (link_fetch_end (time_now) < 0) {
			link_failed_call_reason = ISR_DEAD_BAD_SYNC_DATA;
			set_link_disconnected (time_now);
			return;
		}

		// If the remote status is not connectable ...

		if (!( remote_status.is_connectable() )) {
			link_failed_call_reason = ISR_DEAD_BAD_STATE;
			set_link_disconnected (time_now);
			return;
		}

		// On first entry to connected state, set no call retries

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;

		// Change to the connected state

		set_link_state (LINK_CONNECTED);
		return;
	}




	// Poll link when state is LINK_CONNECTED.

	private void poll_link_connected (long time_now) {

		// Get the relay thread status
	
		int rtstat = relay_thread.get_ri_relay_status();

		// If the thread is not running, then connection is lost

		if (rtstat != RelayThread.RTSTAT_RUNNING) {
			set_link_disconnected (time_now);
			return;
		}

		// Process any items on the queue

		if (copy_queued_relay_items(time_now) < 0) {
			set_link_disconnected (time_now);
			return;
		}

		// If the remote status is not connectable ...

		if (!( remote_status.is_connectable() )) {
			set_link_disconnected (time_now);
			return;
		}

		// If not time for resync, remain in this state

		if (time_now < link_next_resync_time) {
			return;
		}

		// Set up fetch for resync, if error assume failure and go back to disconnected state

		if (!( link_fetch_begin (time_now) )) {

			set_link_disconnected (time_now);
			return;
		}

		// Change to resync state

		set_link_state (LINK_RESYNC);
		return;
	}




	// Poll link when state is LINK_RESYNC.

	private void poll_link_resync (long time_now) {

		// Process any items on the queue

		if (copy_queued_relay_items(time_now) < 0) {
			set_link_disconnected (time_now);
			return;
		}

		// If fetch is not complete, remain in this state

		int fetch_status = relay_thread.get_ri_fetch_status();

		if (RelayThread.is_fetch_active (fetch_status)) {
			return;
		}

		// If it did not finish normally ...

		if (fetch_status != RelayThread.FISTAT_FINISHED) {
			set_link_disconnected (time_now);
			return;
		}

		// End the fetch operation, handle error

		if (link_fetch_end (time_now) < 0) {
			set_link_disconnected (time_now);
			return;
		}

		// If the remote status is not connectable ...

		if (!( remote_status.is_connectable() )) {
			set_link_disconnected (time_now);
			return;
		}

		// Change to the connected state

		set_link_state (LINK_CONNECTED);
		return;
	}




	// Poll link state.

	private void poll_link (long time_now) {

		// Switch according to current link state

		switch (get_link_state()) {

		default:
			throw new IllegalStateException ("Invalid link state: " + get_link_state_as_string (get_link_state()));

		// In shutdown state, nothing to do
		
		case LINK_SHUTDOWN:
			break;

		// In solo state, nothing to do

		case LINK_SOLO:
			break;

		// In disconnected state, check if it is time to make a new connection attempt

		case LINK_DISCONNECTED:
			poll_link_disconnected (time_now);
			break;

		// In calling state, check for successful call and start initial sync

		case LINK_CALLING:
			poll_link_calling (time_now);
			break;

		// In initial sync state, check for completion of sync

		case LINK_INITIAL_SYNC:
			poll_link_initial_sync (time_now);
			break;

		// In connected state, monitor changes in partner server relay items

		case LINK_CONNECTED:
			poll_link_connected (time_now);
			break;

		// In resync state, check for completion of sync

		case LINK_RESYNC:
			poll_link_resync (time_now);
			break;

		}

		return;
	}




	// Poll inferred state.

	private void poll_inferred (long time_now) {

		// Switch according to current link state

		switch (get_link_state()) {

		default:
			throw new IllegalStateException ("Invalid link state: " + get_link_state_as_string (get_link_state()));

		// In shutdown or solo state, no information about remote server
		
		case LINK_SHUTDOWN:
		case LINK_SOLO:
			set_inferred_state (ISR_NONE);
			break;

		// In disconnected or calling state, supply reason last call failed if the call has failed
		// enough times to qualify as loss of connection, otherwise infer remote server is listening

		case LINK_DISCONNECTED:
		case LINK_CALLING:
			if (link_call_retry_count > call_retry_loss_conn + 1) {
				set_inferred_state (link_failed_call_reason);
			} else {
				set_inferred_state (ISR_LISTENING);
			}
			break;

		// In initial sync, connected, or resync state ...

		case LINK_INITIAL_SYNC:
		case LINK_CONNECTED:
		case LINK_RESYNC:
			// If no remote status (should never happen), unavailable
			if (remote_status == null) {
				set_inferred_state (ISR_UNAVAILABLE);
			}
			// Check for stale heartbeat
			else if (!( remote_status.heartbeat_time >= time_now - heartbeat_stale )) {
				set_inferred_state (ISR_DEAD_STALE_HEARTBEAT);
			}
			// Check for bad health
			else if (!( HealthSupport.hs_good_status (remote_status.health_status) )) {
				set_inferred_state (ISR_DEAD_BAD_HEALTH);
			}
			// Check for relay configuration mismatch
			else if (!( local_status.is_same_relay_config (remote_status) )) {
				set_inferred_state (ISR_UNSYNCED_MODE);
			}
			// Check for remote server not connected
			else if (!( remote_status.link_state == LINK_CONNECTED || remote_status.link_state == LINK_RESYNC )) {
				set_inferred_state (ISR_NOT_CONNECTED);
			}
			// Otherwise, connection is OK
			else {
				set_inferred_state (ISR_OK);
			}
			break;
		}

		return;
	}




	//=====[ Relay Configuration ]=====




	//----- Relay configuration variables -----

	// Relay mode timestamp.

	// private long local_status.relay_config.mode_timestamp;

	// Configured relay mode, see RMODE_XXXXX.

	// private int local_status.relay_config.relay_mode;

	// Configured primary server number.

	// private int local_status.relay_config.configured_primary;




	//----- Relay modes -----




	// Relay mode values. (Always 3 digits)

	public static final int RMODE_MIN					= 101;
	public static final int RMODE_SOLO					= 101;
		// The server is configured to run in standalone mode.  No attempt
		// is made to communicate with a partner server, and other servers
		// should not connect to this server.
	public static final int RMODE_WATCH					= 102;
		// The server is configured to run in watch mode.  The server communicates
		// with a partner server.  There is no negotiation for primary server;
		// the configuration selects the primary.
	public static final int RMODE_PAIR					= 103;
		// The server is running in pair mode.  The server communicates
		// with a partner server.  Primary server is negotiated.
	public static final int RMODE_MAX					= 103;


	// Return a string describing a relay mode.

	public static String get_relay_mode_as_string (int mode) {
		switch (mode) {
		case RMODE_SOLO: return "RMODE_SOLO";
		case RMODE_WATCH: return "RMODE_WATCH";
		case RMODE_PAIR: return "RMODE_PAIR";
		}
		return "RMODE_INVALID(" + mode + ")";
	}

	// Convert a user string to a relay mode.
	// The return value is 0 if the string is invalid.

	public static int convert_user_string_to_mode (String s) {
		if (s.equalsIgnoreCase ("solo")) {
			return RMODE_SOLO;
		}
		if (s.equalsIgnoreCase ("watch")) {
			return RMODE_WATCH;
		}
		if (s.equalsIgnoreCase ("pair")) {
			return RMODE_PAIR;
		}
		return 0;
	}

	// Test if user string matches the relay mode.
	// Parameters:
	//  s = User string.
	//  v_relay_mode = Relay mode value to test.
	// Returns one of the following:
	//  -1 = Invalid user string.
	//  0 = Not a match.
	//  1 = Match.
	// Note: If check_user_string_relay_mode returns true for the user string,
	// then this function will return 0 or 1.
	// Note: Any string acceptable to convert_user_string_to_mode() must be accepted here.

	public static int test_user_string_relay_mode (String s, int v_relay_mode) {
		int result = -1;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = 1;
		}
		else if (s.equalsIgnoreCase ("solo")) {
			switch (v_relay_mode) {
			case RMODE_SOLO:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("watch")) {
			switch (v_relay_mode) {
			case RMODE_WATCH:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("pair")) {
			switch (v_relay_mode) {
			case RMODE_PAIR:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
	
		return result;
	}

	// Check for valid user string for matching the configured primary.
	// Parameters:
	//  s = User string.
	// Returns true if valid, false if not.
	// Note: Any string acceptable to convert_user_string_to_mode() must be accepted here.

	public static boolean check_user_string_relay_mode (String s) {
		boolean result = false;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("solo")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("watch")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("pair")) {
			result = true;
		}
	
		return result;
	}

	// Convert a user string to a server number.
	// Parameters:
	//  s = User string.
	//  f_0_ok = True if return value can be 0.
	//  f_9_ok = True if return value can be 9.
	// Returns one of the following:
	//  -1 = Invalid string.
	//  0 = Local server (accessed through handle 0).
	//  SRVNUM_MIN through SRVNUM_MAX (1 or 2) = Selected remote server.
	//  9 = All servers.

	public static int convert_user_string_to_server_number (String s, boolean f_0_ok, boolean f_9_ok) {
		int srvnum = -1;

		// Interpret string as an integer, or one of a few special values

		if (s.equalsIgnoreCase ("this")) {
			srvnum = (new ServerConfig()).get_server_number();
		}
		else if (s.equalsIgnoreCase ("other")) {
			srvnum = (new ServerConfig()).get_partner_server_number();
		}
		else if (s.equalsIgnoreCase ("both")) {
			srvnum = 9;
		}
		else if (s.equalsIgnoreCase ("local")) {
			srvnum = 0;
		}
		else {
			try {
				srvnum = Integer.parseInt (s);
			} catch (NumberFormatException e) {
				srvnum = -1;
			}
		}

		// Check for valid result

		if (!(
			(srvnum >= ServerConfigFile.SRVNUM_MIN && srvnum <= ServerConfigFile.SRVNUM_MAX)
			|| (f_0_ok && srvnum == 0)
			|| (f_9_ok && srvnum == 9)
		)) {
			srvnum = -1;
		}
	
		return srvnum;
	}

	// Convert a user string to a configured primary.
	// Parameters:
	//  s = User string.
	// Returns one of the following:
	//  -1 = Invalid string.
	//  SRVNUM_MIN through SRVNUM_MAX (1 or 2) = Selected remote server.

	public static int convert_user_string_to_configured_primary (String s) {
		return convert_user_string_to_server_number (s, false, false);
	}

	// Test if user string matches the configured primary.
	// Parameters:
	//  s = User string.
	//  v_configured_primary = Configured primary value to test.
	// Returns one of the following:
	//  -1 = Invalid user string.
	//  0 = Not a match.
	//  1 = Match.
	// Note: If check_user_string_configured_primary returns true for the user string,
	// then this function will return 0 or 1.
	// Note: Any string acceptable to convert_user_string_to_configured_primary()
	// must be accepted here.

	public static int test_user_string_configured_primary (String s, int v_configured_primary) {
		int result = -1;

		// Interpret string as an integer, or one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = 1;
		}
		else if (s.equalsIgnoreCase ("this")) {
			if (v_configured_primary == (new ServerConfig()).get_server_number()) {
				result = 1;
			} else {
				result = 0;
			}
		}
		else if (s.equalsIgnoreCase ("other")) {
			if (v_configured_primary == (new ServerConfig()).get_partner_server_number()) {
				result = 1;
			} else {
				result = 0;
			}
		}
		else {
			try {
				int srvnum = Integer.parseInt (s);
				if (srvnum >= ServerConfigFile.SRVNUM_MIN && srvnum <= ServerConfigFile.SRVNUM_MAX) {
					if (v_configured_primary == srvnum) {
						result = 1;
					} else {
						result = 0;
					}
				}
			} catch (NumberFormatException e) {
				result = -1;
			}
		}
	
		return result;
	}

	// Check for valid user string for matching the configured primary.
	// Parameters:
	//  s = User string.
	// Returns true if valid, false if not.
	// Note: Any string acceptable to convert_user_string_to_configured_primary()
	// must be accepted here.

	public static boolean check_user_string_configured_primary (String s) {
		boolean result = false;

		// Interpret string as an integer, or one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("this")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("other")) {
			result = true;
		}
		else {
			try {
				int srvnum = Integer.parseInt (s);
				if (srvnum >= ServerConfigFile.SRVNUM_MIN && srvnum <= ServerConfigFile.SRVNUM_MAX) {
					result = true;
				}
			} catch (NumberFormatException e) {
				result = false;
			}
		}
	
		return result;
	}




	//----- Relay mode functions -----




	// Get the relay configuration.

	private RelayConfig get_relay_config () {
		return local_status.relay_config;
	}

	// Set the mode timestamp.
	// The change flag is set if state is changed.
	// Returns true if state changed.

	private boolean set_relay_config (RelayConfig new_relay_config) {
		if (!( local_status.relay_config.is_equal_to (new_relay_config) )) {
			local_status.relay_config = new_relay_config;
			set_status_changed();
			return true;
		}
		return false;
	}


	// Get the mode timestemp.

	private long get_mode_timestamp () {
		return local_status.relay_config.get_mode_timestamp();
	}


	// Get the configured relay mode.

	private int get_relay_mode () {
		return local_status.relay_config.get_relay_mode();
	}


	// Get the configured primary server number.

	private int get_configured_primary () {
		return local_status.relay_config.get_configured_primary();
	}



	// Set the link state that corresponds to the current relay mode.

	public void set_link_state_for_mode (long time_now) {

		// Switch on current relay mode

		switch (get_relay_mode()) {

		default:
			throw new IllegalStateException ("Invalid relay mode: " + get_relay_mode_as_string (get_relay_mode()));
		
		// Solo mode requires solo link state

		case RMODE_SOLO:
			set_link_solo (time_now);
			break;

		// Watch or pair mode requires active link state

		case RMODE_WATCH:
		case RMODE_PAIR:
			ensure_link_active (time_now);
			break;
		}

		return;
	}



	// Initialize the relay mode variables.
	// Note: The link should be initialized before initializing the relay mode.

	private void init_mode (long time_now) {

		// Set the link state for the initial mode

		set_link_state_for_mode (time_now);

		return;
	}



	// Shut down the relay mode variables.
	// Note: The link should be shut down after shutting down the relay mode.

	private void shutdown_mode (long time_now) {

		return;
	}




	// Poll the relay mode.
	// Note: The link should be polled before polling the relay mode.
	// Note: Relay configuration can change only in poll_mode and set_server_relay_mode.

	private void poll_mode (long time_now) {

		// If we have remote status ...

		if (remote_status != null) {

			// Update the relay configuration, if the one in remote status is newer

			if (local_status.fill_relay_config_if_later (remote_status.relay_config)) {
		
				// Set flag indicating status changed

				set_status_changed();

				// Update the link state for the new mode

				set_link_state_for_mode (time_now);
			}
		}

		return;
	}




	//=====[ Primary Negotiation ]=====




	//----- Primary negotiation variables -----




	// Current primary state, see PRIST_XXXXX.

	// private int local_status.primary_state;

	// Time that the server was started (or restarted after error).

	// private long local_status.start_time;




	//----- Primary states -----




	// Primary state values.  (Always 3 digits)

	public static final int PRIST_MIN					= 101;
	public static final int PRIST_PRIMARY				= 101;
		// The server is currently running as a primary server.
	public static final int PRIST_SECONDARY				= 102;
		// The server is currently running as a secondary server.
	public static final int PRIST_INITIALIZING			= 103;
		// The server is initializing and has not yet selected its initial state.
	public static final int PRIST_SHUTDOWN				= 104;
		// The server is currently shut down.
	public static final int PRIST_MAX					= 104;


	// Return a string describing a relay mode.

	public static String get_primary_state_as_string (int state) {
		switch (state) {
		case PRIST_PRIMARY: return "PRIST_PRIMARY";
		case PRIST_SECONDARY: return "PRIST_SECONDARY";
		case PRIST_INITIALIZING: return "PRIST_INITIALIZING";
		case PRIST_SHUTDOWN: return "PRIST_SHUTDOWN";
		}
		return "PRIST_INVALID(" + state + ")";
	}

	// Test if user string matches the primary state.
	// Parameters:
	//  s = User string.
	//  v_primary_state = Primary state value to test.
	// Returns one of the following:
	//  -1 = Invalid user string.
	//  0 = Not a match.
	//  1 = Match.
	// Note: If check_user_string_primary_state returns true for the user string,
	// then this function will return 0 or 1.

	public static int test_user_string_primary_state (String s, int v_primary_state) {
		int result = -1;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = 1;
		}
		else if (s.equalsIgnoreCase ("primary")) {
			switch (v_primary_state) {
			case PRIST_PRIMARY:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("secondary")) {
			switch (v_primary_state) {
			case PRIST_SECONDARY:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("initializing")) {
			switch (v_primary_state) {
			case PRIST_INITIALIZING:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("shutdown")) {
			switch (v_primary_state) {
			case PRIST_SHUTDOWN:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
		else if (s.equalsIgnoreCase ("initialized")) {
			switch (v_primary_state) {
			case PRIST_PRIMARY:
			case PRIST_SECONDARY:
				result = 1;
				break;
			default:
				result = 0;
				break;
			}
		}
	
		return result;
	}

	// Check for valid user string for matching the primary state.
	// Parameters:
	//  s = User string.
	// Returns true if valid, false if not.

	public static boolean check_user_string_primary_state (String s) {
		boolean result = false;

		// Interpret string as one of a few special values

		if (s.equalsIgnoreCase ("any")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("primary")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("secondary")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("initializing")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("shutdown")) {
			result = true;
		}
		else if (s.equalsIgnoreCase ("initialized")) {
			result = true;
		}
	
		return result;
	}




	//----- Primary negotiation functions -----




	// Get the current primary state.

	private int get_primary_state () {
		return local_status.primary_state;
	}

	// Set the current primary state.
	// The change flag is set if state is changed.
	// Returns true if state changed.

	private boolean set_primary_state (int new_primary_state) {
		if (local_status.primary_state != new_primary_state) {
			local_status.primary_state = new_primary_state;
			set_status_changed();
			return true;
		}
		return false;
	}


	// Get the current start time.

	private long get_start_time () {
		return local_status.start_time;
	}

	// Set the current start time.
	// The change flag is set if state is changed.
	// Returns true if state changed.

	private boolean set_start_time (long new_start_time) {
		if (local_status.start_time != new_start_time) {
			local_status.start_time = new_start_time;
			set_status_changed();
			return true;
		}
		return false;
	}




	// Get the next primary state.
	// Return value is:
	//  PRIST_PRIMARY if this server should be running in primary state.
	//  PRIST_SECONDARY if this server should be running in secondary state.
	//  PRIST_INITIALIZING if this server is currently initializing, and state is not yet determined.

	private int prist_get_next_state (long time_now) {
		
		// Switch according to current relay mode

		switch (get_relay_mode()) {

		default:
			throw new IllegalStateException ("Invalid relay mode: " + get_relay_mode_as_string (get_relay_mode()));
		
		// In solo mode, just return configured primary state

		case RMODE_SOLO:
			return ( local_status.is_configured_primary() ? PRIST_PRIMARY : PRIST_SECONDARY );
		
		// In watch mode, just return configured primary state

		case RMODE_WATCH:
			if (get_primary_state() == PRIST_INITIALIZING) {

				// Come here in watch mode during initialization

				// If configured primary ...

				if (local_status.is_configured_primary()) {

					//>>> Watch mode, configured primary, running as initializing

					// If the partner server is known to be dead or synced, start as primary

					if (is_remote_known_dead_or_synced (time_now)) {
						return PRIST_PRIMARY;
					}

					// If initialization timeout expired, force start as primary

					if (time_now > get_start_time() + prist_init_timeout && is_link_conn_or_disc()) {
						return PRIST_PRIMARY;
					}

					// Otherwise, stay in initialization

					return PRIST_INITIALIZING;
				}

				//>>> Watch mode, configured secondary, running as initializing

				// Otherwise we are configured secondary ...

				// When in watch mode and configured secondary, do not exit from
				// initialization until synchronized with the partner server, no
				// matter how long it takes.  This mode can then be used to restart
				// one server of a pair during maintenance, in a way that it does
				// not process any forecasts until after resynchronizing.  A shutdown
				// task can be used to end the wait if synchronization fails.

				//  // If the partner server is known to be dead, start as secondary
				//  
				//  if (is_remote_known_dead (time_now)) {
				//  	return PRIST_SECONDARY;
				//  	//return PRIST_PRIMARY;
				//  }

				// If the partner server is known to be alive and synced, start as secondary

				if (is_remote_known_alive_and_synced (time_now)) {
					return PRIST_SECONDARY;
				}

				//  // If initialization timeout expired, force start as secondary
				//  
				//  if (time_now > get_start_time() + prist_init_timeout && is_link_conn_or_disc()) {
				//  	return PRIST_SECONDARY;
				//  }

				// Otherwise, stay in initialization

				return PRIST_INITIALIZING;
			}
			return ( local_status.is_configured_primary() ? PRIST_PRIMARY : PRIST_SECONDARY );

		// In pair mode, continue ...

		case RMODE_PAIR:
			break;
		}

		// Come here in pair mode

		// Switch according to current primary state

		switch (get_primary_state()) {

		default:
			throw new IllegalStateException ("Invalid primary state: " + get_primary_state_as_string (get_primary_state()));

		// In primary state ...

		case PRIST_PRIMARY:

			// If configured primary, continue to be primary

			if (local_status.is_configured_primary()) {

				//>>> Pair mode, configured primary, running as primary

				return PRIST_PRIMARY;
			}

			//>>> Pair mode, configured secondary, running as primary

			// Otherwise we are configured secondary ...
			// If the partner server is known to be dead, stay in primary

			if (is_remote_known_dead (time_now)) {
				return PRIST_PRIMARY;
			}

			// If the partner server is known to be alive and synced, step down to secondary

			if (is_remote_known_alive_and_synced (time_now)) {
				return PRIST_SECONDARY;
			}

			// Otherwise, stay in primary (even though we are configured secondary)

			return PRIST_PRIMARY;

		// In secondary state ...

		case PRIST_SECONDARY:

			// If configured primary ...

			if (local_status.is_configured_primary()) {

				//>>> Pair mode, configured primary, running as secondary

				// If the partner server is known to be dead or synced, step up to primary

				if (is_remote_known_dead_or_synced (time_now)) {
					return PRIST_PRIMARY;
				}

				// Otherwise, stay in secondary

				return PRIST_SECONDARY;
			}

			//>>> Pair mode, configured secondary, running as secondary

			// Otherwise we are configured secondary ...
			// If the partner server is known to be dead, step up to primary

			if (is_remote_known_dead (time_now)) {
				return PRIST_PRIMARY;
			}

			// Otherwise, stay in secondary

			return PRIST_SECONDARY;

		// In initializing state, continue

		case PRIST_INITIALIZING:
			break;
		}

		// Come here in pair mode during initialization

		// If configured primary ...

		if (local_status.is_configured_primary()) {

			//>>> Pair mode, configured primary, running as initializing

			// If the partner server is known to be dead or synced, start as primary

			if (is_remote_known_dead_or_synced (time_now)) {
				return PRIST_PRIMARY;
			}

			// If initialization timeout expired, force start as primary

			if (time_now > get_start_time() + prist_init_timeout && is_link_conn_or_disc()) {
				return PRIST_PRIMARY;
			}

			// Otherwise, stay in initialization

			return PRIST_INITIALIZING;
		}

		//>>> Pair mode, configured secondary, running as initializing

		// Otherwise we are configured secondary ...
		// If the partner server is known to be dead, start as primary

		if (is_remote_known_dead (time_now)) {
			return PRIST_PRIMARY;
		}

		// If the partner server is known to be alive and synced, start as secondary

		if (is_remote_known_alive_and_synced (time_now)) {
			return PRIST_SECONDARY;
		}

		// If initialization timeout expired, force start as secondary

		if (time_now > get_start_time() + prist_init_timeout && is_link_conn_or_disc()) {
			return PRIST_SECONDARY;
		}

		// Otherwise, stay in initialization

		return PRIST_INITIALIZING;
	}




	// Poll the primary state.
	// Returns the new primary state.
	// Note: This function should be called during idle time processing.
	// Note: This function switches the timeline between primary and secondary when the state changes.
	// Note: The relay mode should be polled before polling the primary state.

	private int poll_prist (long time_now) {

		// Get the next primary state

		int next_prist = prist_get_next_state (time_now);

		// If it is changing ...

		if (get_primary_state() != next_prist) {

			// Convert the timeline as needed

			if (next_prist == PRIST_PRIMARY) {
				
				// Set the work flag

				set_did_work();

				// Set timeline to primary

				sg.timeline_sup.set_timeline_to_primary();
			}

			if (next_prist == PRIST_SECONDARY) {
				
				// Set the work flag

				set_did_work();

				// Set timeline to secondary

				sg.timeline_sup.set_timeline_to_secondary();
			}

			// Write new state into status

			set_primary_state (next_prist);
		}

		return next_prist;
	}




	// Initialize the primary state variables.
	// Note: The relay mode should be initialized before initializing the primary state.

	private void init_prist (long time_now) {

		// Set initializing state

		set_primary_state (PRIST_INITIALIZING);

		// Set the start time

		set_start_time (time_now);

		return;
	}




	// Shut down the primary state variables.
	// Note: The relay mode should be shut down after shutting down the primary state.

	private void shutdown_prist (long time_now) {

		// Set shutdown state

		set_primary_state (PRIST_SHUTDOWN);

		// Set the start time

		set_start_time (0L);

		return;
	}




	//=====[ Subsystem Interface ]=====




	// The LinkSentinel class stops the relay thread when it is closed.
	// It can be used in a try-with-resources statement to ensure the thread is terminated.
	// Note that it may incur polling delay, because close() waits for the relay thread to die.
	// There is no error if the thread is not running when the sentinel is closed.
	// Note: If an orderly shutdown has not occurred, the close() function leaves the relay
	// link in an inconsistent state.  A new initialization must be done to resume operation.
	// Implementation note: This must work correctly even if there is an exception during initializetion.

	public class LinkSentinel implements AutoCloseable {
		@Override
		public void close () {

			// If we have local status ...

			if (local_status != null) {
			
				// If not currently shut down ...

				if (get_link_state() != LINK_SHUTDOWN) {

					// Go to shutdown state, which terminates the thread

					set_link_shutdown();
				}

				// Set shutdown state in local status

				local_status.fill_shutdown_state();
			}

			// Make certain that the relay thread is terminated in all cases
			// (Extra calls to terminate the thread are ignored.)

			relay_thread.terminate_relay_thread();

			return;
		}
	}


	// Make a sentinal.

	public LinkSentinel make_link_sentinel () {
		return new LinkSentinel();
	}




	// Set the server relay mode.
	// On entry, these task dispatcher context variables must set up:
	//  dispatcher_time, dispatcher_true_time, dispatcher_action_config.
	// This can be run during idle time, or during task execution within a MongoDB transaction.
	// Note: Because this can be called within a MongoDB transaction, it cannot perform
	// extensive actions.

	public void set_server_relay_mode (RelayConfig the_relay_config) {

		// Get the current time

		long time_now = sg.task_disp.get_time();

		// Ensure that the next call will occur quickly

		ensure_link_quick_call (time_now);

		// Update the relay configuration, if the one we have is newer

		if (local_status.fill_relay_config_if_later (the_relay_config)) {
		
			// Set flag indicating status changed

			set_status_changed();

			// Update the link state for the new mode

			set_link_state_for_mode (time_now);

			// Poll inferred remote server state

			poll_inferred (time_now);

			// Store status into database

			store_local_status (time_now);
		}

		return;
	}




	// Run poll operation during task idle time.
	// This should be run during idle time, not during a MongoDB transaction.
	// This may switch the timeline to primary or secondary state.
	// Returns true if it did significant work, false if not.

	public boolean poll_relay_link () {

		// Clear the work flag

		clear_did_work();

		// Get the current time

		long time_now = ServerClock.get_time();

		// Poll the link state

		poll_link (time_now);

		// Poll the relay mode

		poll_mode (time_now);

		// Poll the primary state, switch between primary or secondary if needed

		poll_prist (time_now);

		// Poll inferred remote server state

		poll_inferred (time_now);

		// Store local status, if it changed

		store_local_status (time_now);

		// Return the work flag

		return is_did_work();
	}




	// Initialize the relay link.
	// This must be called before anything else (except the sentinel and init_db_status()).
	// This needs to be called after connecting to MongoDB.
	// Note: This does not determine the initial primary or secondary state.
	// You can use poll_until_primary_known to wait for primary state to be determined.

	public void init_relay_link () {

		// Get the current time

		long time_now = ServerClock.get_time();

		// Load the initial status

		load_initial_status (time_now);

		// Establish the initial link state (shutdown)

		set_link_init ();

		// Initialize the relay mode
		
		init_mode (time_now);

		// Initialize the primary state

		init_prist (time_now);

		// Poll inferred remote server state

		poll_inferred (time_now);

		// Store local status (it will always have changed)

		store_local_status (time_now);
	
		return;
	}




	// Poll until primary state is known.
	// This should be run during idle time, not during a MongoDB transaction.
	// This will switch the timeline to primary or secondary state.

	public void poll_until_primary_known () {

		// Poll until initial primary state negotiation is complete

		long wait_time = 0L;

		while (get_primary_state() == PRIST_INITIALIZING) {

			// Wait if needed

			if (wait_time > 0L) {
				try {
					Thread.sleep (wait_time);
				} catch (InterruptedException e) {
				}
			}

			// Do the poll

			boolean f_work = poll_relay_link();

			// Next wait time

			if (f_work) {
				wait_time = 2000L;			// 2 seconds
			} else {
				wait_time = 5000L;			// 5 seconds
			}
		}
	
		return;
	}




	// Return true if primary state is being negotiated.

	public boolean is_prist_initializing () {
		if (local_status != null && get_primary_state() == PRIST_INITIALIZING) {
			return true;
		}
		return false;
	}




	// Shut down the relay link.
	// This performs an orderly shutdown.
	// This must be called while still connected to MongoDB.
	// After calling this, no relay link functions may be called (except the sentinel),
	// unless a new initialization is performed.

	public void shutdown_relay_link () {

		// Get the current time

		long time_now = ServerClock.get_time();

		// Shut down the primary state

		shutdown_prist (time_now);

		// Shut down the relay mode

		shutdown_mode (time_now);

		// Shut down the link, this also terminates the relay thread

		set_link_shutdown ();

		// Store shutdown status

		store_shutdown_status (time_now);

		return;
	}




	// Return true if currently in primary state.
	// This may be called at any time, even if the relay link is not initialized.
	// Note: Anything other than secondary state returns true,
	// including the case where the relay link has not been initialized.

	public boolean is_primary_state () {
		if (local_status == null || get_primary_state() != PRIST_SECONDARY) {
			return true;
		}
		return false;
	}




//	// Initialize the status in the database to shutdown state.
//	// Also updates the fixed fields in the server status.
//	// Note: This may be called only before initialization.
//
//	public void init_db_status () {
//
//		// Get the current time
//
//		long time_now = ServerClock.get_time();
//
//		// Change status in the database to shutdown state
//
//		shutdown_db_status (time_now);
//	
//		return;
//	}




	//=====[ Testing ]=====




	// Run poll operation during task idle time, without updating the primary state.
	// This should be run during idle time, not during a MongoDB transaction.
	// This never switches the timeline to primary or secondary state.
	// Returns true if it did significant work, false if not.

	public boolean poll_relay_link_no_prist () {

		// Clear the work flag

		clear_did_work();

		// Get the current time

		long time_now = ServerClock.get_time();

		// Poll the link state

		poll_link (time_now);

		// Poll the relay mode

		poll_mode (time_now);

		//  // Poll the primary state, switch between primary or secondary if needed
		//  
		//  poll_prist (time_now);

		// Poll inferred remote server state

		poll_inferred (time_now);

		// Store local status, if it changed

		store_local_status (time_now);

		// Return the work flag

		return is_did_work();
	}




	// Get a one-line summary of local status.

	public String get_local_status_summary () {
		if (local_status == null) {
			return "No local status";
		}
		return local_status.one_line_summary();
	}




	// Get a one-line summary of remote status.

	public String get_remote_status_summary () {
		if (remote_status == null) {
			return "No remote status";
		}
		return remote_status.one_line_summary();
	}




	//=====[ Setup ]=====




	//----- Construction -----


	// Default constructor.

	public RelayLink () {

		relay_thread = new RelayThread();

		init_all_vars();

	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		relay_thread.setup();

		init_all_vars();

		return;
	}


	// Initialize all variables.

	private void init_all_vars () {

		// Status structures

		local_status = null;
		remote_status = null;
		remote_status_item = null;

		// Status change variables

		f_status_changed = false;
		f_did_work = false;

		// Link state variables

		fetch_output_buffer = null;
		fetch_writer = null;

		link_next_call_time = 0L;
		link_call_retry_count = 0L;
		link_failed_call_reason = ISR_DEAD_NOT_CALLABLE;

		link_next_resync_time = 0L;
		link_resync_cycle_counter = 0;
	
		return;
	}

}
