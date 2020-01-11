package org.opensha.oaf.aafs;

import java.util.List;

import java.io.Closeable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import org.opensha.oaf.aafs.MongoDBUtil;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;
import org.opensha.oaf.aafs.entity.DBEntity;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalImpDataReader;
import org.opensha.oaf.util.MarshalImpDataWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TimeSplitOutputStream;
import org.opensha.oaf.util.ConsoleRedirector;


/**
 * AAFS server command-line interface.
 * Author: Michael Barall 05/23/2018.
 */
public class ServerCmd {




	// cmd_start - Run the server.

	public static void cmd_start(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'start' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_aafs(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

			// Summary log

			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_summary(), start_time);

		){

			try {

				// Say hello
			
				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println (VersionInfo.get_title());
				System.out.println ("");
				System.out.println ("AAFS server is starting at " + SimpleUtils.time_to_string (start_time));

				// Get a task dispatcher

				TaskDispatcher dispatcher = new TaskDispatcher();

				// Install the log files

				dispatcher.set_console_log_tsop (con_tsop);
				dispatcher.set_summary_log_tsop (sum_tsop);

				// Run it

				dispatcher.run();

				// Display final status

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);

				int dispatcher_state = dispatcher.get_dispatcher_state();
				if (dispatcher_state == TaskDispatcher.STATE_SHUTDOWN) {
					System.out.println ("AAFS server exited normally at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				} else {
					System.out.println ("Server exited abnormally at " + SimpleUtils.time_to_string (ServerClock.get_true_time()) + ", final state code = " + TaskDispatcher.get_dispatcher_state_as_string (dispatcher_state));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("AAFS server had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("AAFS server had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_stop - Stop the server.

	public static void cmd_stop(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'stop' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_control(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				// Say hello
			
				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println ("Sending shutdown command to AAFS server at " + SimpleUtils.time_to_string (start_time));

				// Post the shutdown task

				boolean result = TaskDispatcher.post_shutdown ("ServerCmd");

				// Display result

				if (result) {
					System.out.println ("Shutdown command was sent to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
					System.out.println ("It takes about 30 seconds for the shutdown to be complete.");
				} else {
					System.out.println ("Unable to send shutdown command to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("Shutdown command had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("Shutdown command had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_pdl_intake - Intake an event from PDL.

	public static void cmd_pdl_intake(String[] args) {

		// At least one additional argument

		if (args.length < 2) {
			System.err.println ("ServerCmd : Invalid 'intake' subcommand");
			return;
		}

		OpIntakePDL payload = new OpIntakePDL();

		try {
			payload.setup (args, 1, args.length);
		}

		// If PDL arguments do not parse, just drop it silently
		
		catch (Exception e) {
			return;
		}

		// If no event id, just drop it silently

		if (!( payload.has_event_id() )) {
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_intake(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				// Get the timestamp

				String timestamp = SimpleUtils.time_to_string_no_z (start_time);

				// If we don't have location, magnitude, and time, drop it

				if (!( payload.has_lat_lon_depth_mag() && payload.has_event_time() )) {
					System.out.println (timestamp + " DROP " + payload.event_id + " (incomplete data)");
					return;
				}

				// Build event info

				String event_info = SimpleUtils.event_id_and_info_one_line (
										payload.event_id,
										payload.mainshock_time,
										payload.mainshock_mag,
										payload.mainshock_lat,
										payload.mainshock_lon,
										payload.mainshock_depth);

				// Process event no earlier than the time of first forecast
				// (Intake is delayed as long as possible, because Comcat authoritative event ID
				// changes typically occur within the first 20 minutes after an earthquake, and
				// we attempt to avoid intake until the authoritative ID is fixed.)

				ActionConfig action_config = new ActionConfig();

				long the_time = ServerClock.get_time();

				long first_forecast = payload.mainshock_time
										+ action_config.get_next_forecast_lag(0L)
										+ action_config.get_comcat_clock_skew()
										+ action_config.get_comcat_origin_skew();

				long sched_time = Math.max (the_time, first_forecast);

				// Test if event passes the intake filter, using the minimum magnitude criterion

				IntakeSphRegion intake_region = action_config.get_pdl_intake_region_for_min_mag (
						payload.mainshock_lat, payload.mainshock_lon, payload.mainshock_mag);

				if (intake_region == null) {

					// Didn't pass, check using the intake magnitude criterion

					intake_region = action_config.get_pdl_intake_region_for_intake_mag (
							payload.mainshock_lat, payload.mainshock_lon, payload.mainshock_mag);

					// Schedule task for projected time of first forecast

					sched_time = first_forecast;

					// If we didn't pass, or if the scheduled time has already passed, then drop event

					if (intake_region == null || sched_time < the_time) {
						System.out.println (timestamp + " INTAKE-FILTER " + event_info);
						return;
					}
				}

				// Test if event is in acceptable age range

				long the_min_time = start_time - action_config.get_pdl_intake_max_age();
				long the_max_time = start_time + action_config.get_pdl_intake_max_future();

				if (!( payload.mainshock_time >= the_min_time && payload.mainshock_time <= the_max_time )) {
				
					// Out of acceptable age range, drop event

					System.out.println (timestamp + " AGE-RANGE " + event_info);
					return;
				}

				// Post the task

				String event_id = payload.event_id;

				int opcode = TaskDispatcher.OPCODE_INTAKE_PDL;
				int stage = 0;

				boolean result = TaskDispatcher.post_task (event_id, sched_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				// Display result

				if (result) {
					System.out.println (timestamp + " ACCEPT " + event_info);
				} else {
					System.out.println (timestamp + " SUBMISSION-FAILURE " + event_info);
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("PDL intake had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("PDL intake had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_add_event - Tell the server to watch an event.

	public static void cmd_add_event(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'add_event' subcommand");
			return;
		}

		String event_id = args[1];

		OpIntakeSync payload = new OpIntakeSync();
		payload.setup ();

		// Say hello

		System.out.println ("Add event, event_id = " + event_id);

		// Post the task

		int opcode = TaskDispatcher.OPCODE_INTAKE_SYNC;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

		// Display result

		if (result) {
			System.out.println ("Event was sent to AAFS server.");
		} else {
			System.out.println ("Unable to send event to AAFS server.");
		}

		return;
	}




	// cmd_start_with_pdl - Run the server, and accept PDL options on the command line.

	public static void cmd_start_with_pdl(String[] args) {

		// Any number of additional arguments

		if (args.length < 1) {
			System.err.println ("ServerCmd : Invalid 'start_with_pdl' subcommand");
			return;
		}

		// Read PDL options

		int lo = 1;
		boolean f_config = true;
		boolean f_send = false;
		int pdl_default = ServerConfigFile.PDLOPT_UNSPECIFIED;

		if (PDLCmd.exec_pdl_cmd (args, lo, f_config, f_send, pdl_default)) {
			System.out.println ("AAFS server not started due to error in PDL options.");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_aafs(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

			// Summary log

			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_summary(), start_time);

		){

			try {

				// Say hello
			
				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println (VersionInfo.get_title());
				System.out.println ("");
				System.out.println ("AAFS server is starting at " + SimpleUtils.time_to_string (start_time));

				// Get a task dispatcher

				TaskDispatcher dispatcher = new TaskDispatcher();

				// Install the log files

				dispatcher.set_console_log_tsop (con_tsop);
				dispatcher.set_summary_log_tsop (sum_tsop);

				// Run it

				dispatcher.run();

				// Display final status

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);

				int dispatcher_state = dispatcher.get_dispatcher_state();
				if (dispatcher_state == TaskDispatcher.STATE_SHUTDOWN) {
					System.out.println ("AAFS server exited normally at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				} else {
					System.out.println ("Server exited abnormally at " + SimpleUtils.time_to_string (ServerClock.get_true_time()) + ", final state code = " + TaskDispatcher.get_dispatcher_state_as_string (dispatcher_state));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("AAFS server had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("AAFS server had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_start_comcat_poll - Tell the server to start polling Comcat.

	public static void cmd_start_comcat_poll(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'start_comcat_poll' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_control(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				String event_id = ServerComponent.EVID_POLL;

				OpPollComcatStart payload = new OpPollComcatStart();
				payload.setup ();

				// Say hello

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println ("Sending command to start polling Comcat at " + SimpleUtils.time_to_string (start_time));

				// Post the task

				int opcode = TaskDispatcher.OPCODE_POLL_COMCAT_START;
				int stage = 0;

				long the_time = ServerClock.get_time();

				boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				// Display result

				if (result) {
					System.out.println ("Command to start polling Comcat was sent to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
					System.out.println ("It takes about 30 seconds for the command to take effect.");
				} else {
					System.out.println ("Unable to send AAFS server command to start polling Comcat at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("Command to start polling had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("Command to start polling had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_stop_comcat_poll - Tell the server to stop polling Comcat.

	public static void cmd_stop_comcat_poll(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'stop_comcat_poll' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_control(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				String event_id = ServerComponent.EVID_POLL;

				OpPollComcatStop payload = new OpPollComcatStop();
				payload.setup ();

				// Say hello

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println ("Sending command to stop polling Comcat at " + SimpleUtils.time_to_string (start_time));

				// Post the task

				int opcode = TaskDispatcher.OPCODE_POLL_COMCAT_STOP;
				int stage = 0;

				long the_time = ServerClock.get_time();

				boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				// Display result

				if (result) {
					System.out.println ("Command to stop polling Comcat was sent to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
					System.out.println ("It takes about 30 seconds for the command to take effect.");
				} else {
					System.out.println ("Unable to send AAFS server command to stop polling Comcat at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("Command to stop polling had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("Command to stop polling had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_start_pdl_cleanup - Tell the server to start PDL cleanup.

	public static void cmd_start_pdl_cleanup(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'start_pdl_cleanup' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_control(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				String event_id = ServerComponent.EVID_CLEANUP;

				OpCleanupPDLStart payload = new OpCleanupPDLStart();
				payload.setup ();

				// Say hello

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println ("Sending command to start PDL cleanup at " + SimpleUtils.time_to_string (start_time));

				// Post the task

				int opcode = TaskDispatcher.OPCODE_CLEANUP_PDL_START;
				int stage = 0;

				long the_time = ServerClock.get_time();

				boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				// Display result

				if (result) {
					System.out.println ("Command to start PDL cleanup was sent to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
					System.out.println ("It takes about 30 seconds for the command to take effect.");
				} else {
					System.out.println ("Unable to send AAFS server command to start PDL cleanup at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("Command to start cleanup had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("Command to start cleanup had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_stop_pdl_cleanup - Tell the server to stop PDL cleanup.

	public static void cmd_stop_pdl_cleanup(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'stop_pdl_cleanup' subcommand");
			return;
		}

		// Get current time

		long start_time = ServerClock.get_true_time();

		try (

			// Console redirection and log

			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (
										(new ServerConfig()).get_log_con_control(), start_time);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));

		){

			try {

				String event_id = ServerComponent.EVID_CLEANUP;

				OpCleanupPDLStop payload = new OpCleanupPDLStop();
				payload.setup ();

				// Say hello

				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				System.out.println ("Sending command to stop PDL cleanup at " + SimpleUtils.time_to_string (start_time));

				// Post the task

				int opcode = TaskDispatcher.OPCODE_CLEANUP_PDL_STOP;
				int stage = 0;

				long the_time = ServerClock.get_time();

				boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				// Display result

				if (result) {
					System.out.println ("Command to stop PDL cleanup was sent to AAFS server at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
					System.out.println ("It takes about 30 seconds for the command to take effect.");
				} else {
					System.out.println ("Unable to send AAFS server command to stop PDL cleanup at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("Command to stop cleanup had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("Command to stop cleanup had an uncaught exception at " + SimpleUtils.time_to_string (ServerClock.get_true_time()));
			e.printStackTrace();
		}

		return;
	}




	// cmd_show_version - Print version info to the standard output.

	public static void cmd_show_version(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'show_version' subcommand");
			return;
		}

		// Say hello

		System.out.println (VersionInfo.get_title());

		return;
	}




	// cmd_make_indexes - Make the indexes for our MongoDB collections.

	public static void cmd_make_indexes(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'make_indexes' subcommand");
			return;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Create the indexes

			DBEntity.make_all_indexes (true);
		}

		return;
	}




	// cmd_drop_indexes - Drop the indexes for our MongoDB collections.

	public static void cmd_drop_indexes(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'drop_indexes' subcommand");
			return;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Drop the indexes

			DBEntity.drop_all_indexes (true);
		}

		return;
	}




	// cmd_erase_database - Erase our MongoDB collections.

	public static void cmd_erase_database(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'erase_database_this_is_irreversible' subcommand");
			return;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Drop the collections

			DBEntity.drop_all_collections (true);
		}

		return;
	}




	// cmd_check_collections - Check for existence of our MongoDB collections.

	public static void cmd_check_collections(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'check_collections' subcommand");
			return;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Check for existence of all collections

			DBEntity.check_all_collections (true);
		}

		return;
	}




	// cmd_backup_database - Back up all of our MongoDB collections.

	public static void cmd_backup_database(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'backup_database' subcommand");
			return;
		}

		String filename = args[1];

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		try {

			// Connect to MongoDB and then open the file

			try (
				MongoDBUtil mongo_instance = new MongoDBUtil();
				MarshalImpDataWriter writer = new MarshalImpDataWriter (filename, true);
			){

				// Back up all collections

				DBEntity.backup_database (writer, true);

				writer.check_write_complete();
			}

		}

		// Report any exceptions

		catch (FileNotFoundException e) {
			System.out.println ("Failed to backup database because file was not accessible: " + filename);
			e.printStackTrace();
		}

		catch (IOException e) {
			System.out.println ("Failed to backup database due to I/O error on file: " + filename);
			e.printStackTrace();
		}

		catch (Exception e) {
			System.out.println ("Failed to backup database to file: " + filename);
			e.printStackTrace();
		}

		return;
	}




	// cmd_backup_database_gzip - Back up all of our MongoDB collections, to gzipped file.

	public static void cmd_backup_database_gzip(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'backup_database_gzip' subcommand");
			return;
		}

		String filename = args[1];

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		try {

			// Connect to MongoDB and then open the file

			try (
				MongoDBUtil mongo_instance = new MongoDBUtil();
				MarshalImpDataWriter writer = new MarshalImpDataWriter (
					new DataOutputStream (new GZIPOutputStream (new BufferedOutputStream (new FileOutputStream (filename)))),
					true);
			){

				// Back up all collections

				DBEntity.backup_database (writer, true);

				writer.check_write_complete();
			}

		}

		// Report any exceptions

		catch (FileNotFoundException e) {
			System.out.println ("Failed to backup database because file was not accessible: " + filename);
			e.printStackTrace();
		}

		catch (IOException e) {
			System.out.println ("Failed to backup database due to I/O error on file: " + filename);
			e.printStackTrace();
		}

		catch (Exception e) {
			System.out.println ("Failed to backup database to file: " + filename);
			e.printStackTrace();
		}

		return;
	}




	// cmd_restore_database - Restore all of our MongoDB collections.

	public static void cmd_restore_database(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'restore_database' subcommand");
			return;
		}

		String filename = args[1];

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		try {

			// Open the file and then connect to MongoDB

			try (
				MarshalImpDataReader reader = new MarshalImpDataReader (filename, true);
				MongoDBUtil mongo_instance = new MongoDBUtil();
			){

				// Back up all collections

				DBEntity.restore_database (reader, true);

				reader.check_read_complete();
			}

		}

		// Report any exceptions

		catch (FileNotFoundException e) {
			System.out.println ("Failed to restore database because file was not accessible: " + filename);
			e.printStackTrace();
		}

		catch (IOException e) {
			System.out.println ("Failed to restore database due to I/O error on file: " + filename);
			e.printStackTrace();
		}

		catch (Exception e) {
			System.out.println ("Failed to restore database from file: " + filename);
			e.printStackTrace();
		}

		return;
	}




	// cmd_restore_database_gzip - Restore all of our MongoDB collection, from gzipped file.

	public static void cmd_restore_database_gzip(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'restore_database_gzip' subcommand");
			return;
		}

		String filename = args[1];

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		try {

			// Open the file and then connect to MongoDB

			try (
				MarshalImpDataReader reader = new MarshalImpDataReader (
					new DataInputStream (new GZIPInputStream (new BufferedInputStream (new FileInputStream (filename)))),
					true);
				MongoDBUtil mongo_instance = new MongoDBUtil();
			){

				// Back up all collections

				DBEntity.restore_database (reader, true);

				reader.check_read_complete();
			}

		}

		// Report any exceptions

		catch (FileNotFoundException e) {
			System.out.println ("Failed to restore database because file was not accessible: " + filename);
			e.printStackTrace();
		}

		catch (IOException e) {
			System.out.println ("Failed to restore database due to I/O error on file: " + filename);
			e.printStackTrace();
		}

		catch (Exception e) {
			System.out.println ("Failed to restore database from file: " + filename);
			e.printStackTrace();
		}

		return;
	}




	// cmd_init_relay_mode - Initialize the relay mode, on the local server.

	public static void cmd_init_relay_mode(String[] args) {

		// 2 additional arguments

		if (args.length != 3) {
			System.err.println ("ServerCmd : Invalid 'init_relay_mode' subcommand");
			return;
		}

		int relay_mode = RelayLink.convert_user_string_to_mode (args[1]);
		if (relay_mode == 0) {
			System.out.println ("Invalid relay mode: " + args[1]);
			return;
		}

		int configured_primary;
		try {
			configured_primary = Integer.parseInt (args[2]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid primary server number: " + args[2]);
			return;
		}
		if (!( configured_primary >= ServerConfigFile.SRVNUM_MIN && configured_primary <= ServerConfigFile.SRVNUM_MAX )) {
			System.out.println ("Invalid primary server number: " + args[2]);
			return;
		}

		// Construct the relay configuration

		long time_now = ServerClock.get_time();

		RelayConfig relay_config = new RelayConfig (time_now, relay_mode, configured_primary);

		// Construct the server status structure

		RiServerStatus sstat_payload = new RiServerStatus();
		sstat_payload.setup (relay_config);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Write the server status

			long relay_time = time_now;
			boolean f_force = true;

			RelayItem relit = RelaySupport.static_submit_sstat_relay_item (relay_time, f_force, sstat_payload);

			if (relit != null) {
				System.out.println ("Initial server status was written successfully");
			} else {
				System.out.println ("Unable to write initial server status");
			}
		}

		return;
	}




	// cmd_show_relay_status - Display the relay status, on a local or remote server.

	public static void cmd_show_relay_status(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'show_relay_status' subcommand");
			return;
		}

		int srvnum;
		try {
			srvnum = Integer.parseInt (args[1]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}
		if (!( (srvnum >= 0 && srvnum <= ServerConfigFile.SRVNUM_MAX) || srvnum == 9 )) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}

		// Get list of database handles

		ServerConfig server_config = new ServerConfig();
		List<String> db_handles = server_config.get_server_db_handles();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Loop over possible database handles

		for (int n = 0; n < db_handles.size(); ++n) {
			String db_handle = db_handles.get(n);

			// If we want this one ...

			if (n == srvnum || (srvnum == 9 && n >= ServerConfigFile.SRVNUM_MIN && n <= ServerConfigFile.SRVNUM_MAX)) {
				System.out.println ("Fetching status for server " + n);

				RiServerStatus sstat_payload = new RiServerStatus();
				RelayItem relit = RelaySupport.get_remote_sstat_relay_item (db_handle, sstat_payload);
			
				if (relit == null) {
					System.out.println ("Unable to fetch status for server " + n);
				} else {
					System.out.println (sstat_payload.toString());
				}

				System.out.println ();
			}
		}

		return;
	}




	// cmd_change_relay_mode - Change the relay mode, on a running local or remote server.

	public static void cmd_change_relay_mode(String[] args) {

		// 3 additional arguments

		if (args.length != 4) {
			System.err.println ("ServerCmd : Invalid 'change_relay_mode' subcommand");
			return;
		}

		int srvnum;
		try {
			srvnum = Integer.parseInt (args[1]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}
		if (!( (srvnum >= 0 && srvnum <= ServerConfigFile.SRVNUM_MAX) || srvnum == 9 )) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}

		int relay_mode = RelayLink.convert_user_string_to_mode (args[2]);
		if (relay_mode == 0) {
			System.out.println ("Invalid relay mode: " + args[2]);
			return;
		}

		int configured_primary;
		try {
			configured_primary = Integer.parseInt (args[3]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid primary server number: " + args[3]);
			return;
		}
		if (!( configured_primary >= ServerConfigFile.SRVNUM_MIN && configured_primary <= ServerConfigFile.SRVNUM_MAX )) {
			System.out.println ("Invalid primary server number: " + args[3]);
			return;
		}

		// Construct the relay configuration

		long time_now = ServerClock.get_time();

		RelayConfig relay_config = new RelayConfig (time_now, relay_mode, configured_primary);

		// Get list of database handles

		ServerConfig server_config = new ServerConfig();
		List<String> db_handles = server_config.get_server_db_handles();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Loop over possible database handles

		for (int n = 0; n < db_handles.size(); ++n) {
			String db_handle = db_handles.get(n);

			// If we want this one ...

			if (n == srvnum || (srvnum == 9 && n >= ServerConfigFile.SRVNUM_MIN && n <= ServerConfigFile.SRVNUM_MAX)) {
				System.out.println ("Sending relay mode to server " + n);

				String event_id = ServerComponent.EVID_RELAY;

				OpSetRelayMode payload = new OpSetRelayMode();
				payload.setup (relay_config);

				// Post the task

				int opcode = TaskDispatcher.OPCODE_SET_RELAY_MODE;
				int stage = 0;

				long the_time = time_now;

				boolean result = TaskDispatcher.post_remote_task (db_handle, event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				if (result) {
					System.out.println ("Successfully sent new relay mode to server " + n);
				} else {
					System.out.println ("Unable to send new relay mode to server " + n);
				}

				System.out.println ();
			}
		}

		return;
	}




	// cmd_server_health - Display server health, on a local or remote server.

	public static void cmd_server_health(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'server_health' subcommand");
			return;
		}

		int srvnum;
		try {
			srvnum = Integer.parseInt (args[1]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}
		if (!( (srvnum >= 0 && srvnum <= ServerConfigFile.SRVNUM_MAX) || srvnum == 9 )) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}

		// Buffer to construct result

		StringBuilder sb = new StringBuilder();

		// Get list of database handles

		ServerConfig server_config = new ServerConfig();
		List<String> db_handles = server_config.get_server_db_handles();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Loop over possible database handles

		for (int n = 0; n < db_handles.size(); ++n) {
			String db_handle = db_handles.get(n);

			// If we want this one ...

			if (n == srvnum || (srvnum == 9 && n >= ServerConfigFile.SRVNUM_MIN && n <= ServerConfigFile.SRVNUM_MAX)) {
				System.out.println ();
				System.out.println ("Fetching status for server " + n);

				RiServerStatus sstat_payload = new RiServerStatus();
				RelayItem relit = RelaySupport.get_remote_sstat_relay_item (db_handle, sstat_payload);
			
				if (relit == null
					|| sstat_payload.primary_state == RelayLink.PRIST_SHUTDOWN
					|| sstat_payload.heartbeat_time < System.currentTimeMillis() - 2700000L /* 45 minutes */ ) {
					sb.append ("Server " + n + " is DEAD" + "\n");

				} else {
					sb.append ("Server " + n + " is ALIVE, ");
					switch (sstat_payload.primary_state) {
					case RelayLink.PRIST_PRIMARY:
						sb.append ("Primary");
						break;
					case RelayLink.PRIST_SECONDARY:
						sb.append ("Secondary");
						break;
					case RelayLink.PRIST_INITIALIZING:
						sb.append ("Initializing");
						break;
					}
					sb.append (", last heartbeat = " + sstat_payload.get_heartbeat_time_as_string() + "\n");
				}
			}
		}

		// Display result

		System.out.println ();
		System.out.println (sb.toString());

		return;
	}




	// cmd_init_analyst_cli - Initialize analyst options on the local server, using the analyst CLI.

	public static void cmd_init_analyst_cli(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerCmd : Invalid 'init_analyst_cli' subcommand");
			return;
		}

		// Allocate the CLI

		AnalystCLI analyst_cli = new AnalystCLI();

		// Run it

		boolean selres = analyst_cli.get_selections();

		if (!( selres )) {
			System.out.println ("Operation canceled");
			return;
		}

		// Build the analyst selection relay item

		RelayItem relit = analyst_cli.build_ansel_relay_item();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Write the analyst selection relay item

			boolean f_force = false;
			long relay_stamp = 0L;

			// Note the following can change relit.id and relit.relay_stamp, but no other fields
			// (if f_force were true, then relit.relay_time could also be changed)

			int chkres = RelayItem.submit_relay_item (relit, f_force, relay_stamp);

			if (chkres > 0) {
				System.out.println ("Initial analyst options were written successfully");
			} else if (chkres == 0) {
				System.out.println ("The same initial analyst options were previously written");
			} else {
				System.out.println ("Unable to write initial analyst options");
			}
		}

		return;
	}




	// cmd_change_analyst_cli - Change the analyst options, on a running local or remote server, using the analyst CLI.

	public static void cmd_change_analyst_cli(String[] args) {

		// 1 additional argument

		if (args.length != 2) {
			System.err.println ("ServerCmd : Invalid 'change_analyst_cli' subcommand");
			return;
		}

		int srvnum;
		try {
			srvnum = Integer.parseInt (args[1]);
		} catch (NumberFormatException e) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}
		if (!( (srvnum >= 0 && srvnum <= ServerConfigFile.SRVNUM_MAX) || srvnum == 9 )) {
			System.out.println ("Invalid server number: " + args[1]);
			return;
		}

		// Allocate the CLI

		AnalystCLI analyst_cli = new AnalystCLI();

		// Run it

		boolean selres = analyst_cli.get_selections();

		if (!( selres )) {
			System.out.println ("Operation canceled");
			return;
		}

		// Build the analyst selection relay item

		RelayItem relit = analyst_cli.build_ansel_relay_item();

		// Get current time

		long time_now = ServerClock.get_time();
		boolean f_success = false;

		// Get list of database handles

		ServerConfig server_config = new ServerConfig();
		List<String> db_handles = server_config.get_server_db_handles();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Loop over possible database handles

		for (int n = 0; n < db_handles.size(); ++n) {
			String db_handle = db_handles.get(n);

			// If we want this one ...

			if (n == srvnum || (srvnum == 9 && n >= ServerConfigFile.SRVNUM_MIN && n <= ServerConfigFile.SRVNUM_MAX)) {
				System.out.println ("Sending analyst options to server " + n);

				long relay_stamp = 0L;
				int relay_write_option = OpAnalystSelection.RWOPT_WRITE_NEW;

				OpAnalystSelection payload = new OpAnalystSelection();
				payload.setup (relit, relay_stamp, relay_write_option);

				// Post the task

				String event_id = payload.event_id;
				int opcode = TaskDispatcher.OPCODE_ANALYST_SELECTION;
				int stage = 0;

				long the_time = time_now;

				boolean result = TaskDispatcher.post_remote_task (db_handle, event_id, the_time, the_time, "ServerCmd", opcode, stage, payload.marshal_task());

				if (result) {
					f_success = true;
					System.out.println ("Successfully sent new analyst options to server " + n);
				} else {
					System.out.println ("Unable to send new analyst options to server " + n);
				}

				System.out.println ();
			}
		}

		if (f_success) {
			System.out.println ("The operation was successful");
		} else {
			System.out.println ("The operation was not successful");
		}

		return;
	}




	// cmd_fcdata_convert - Convert a forecast data download file to friendly format.

	public static void cmd_fcdata_convert(String[] args) {

		// 2 additional arguments

		if (args.length != 3) {
			System.err.println ("ServerCmd : Invalid 'fcdata_convert' subcommand");
			return;
		}

		String in_filename = args[1];
		String out_filename = args[2];

		// Convert to friendly format

		ForecastData fcdata = ForecastData.convert_json_file_to_friendly (in_filename, out_filename);

		System.out.println ("The conversion was successful");

		return;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ServerCmd : Missing subcommand");
			return;
		}

		switch (args[0].toLowerCase()) {

		// Subcommand : start
		// Command format:
		//  start
		// Run the server.

		case "start":
			try {
				cmd_start(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : stop
		// Command format:
		//  stop
		// Stop the server.

		case "stop":
			try {
				cmd_stop(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : pdl_intake
		// Command format:
		//  pdl_intake  arg...
		// Post a PDL intake command for the given command line.
		//
		// This command is normally invoked from a bash script, which in turn is
		// invoked from a PDL indexer listener.  It expects to receive the command
		// line parameters from the indexer listener.  The command in the script
		// file should look like this:
		//
		// java -cp jar-file-list org.opensha.oaf.aafs.ServerCmd pdl_intake "$@"
		//
		// Notice that $@ appears inside quotes.  The quotes are necessary for bash
		// to correctly pass the script parameters to Java.

		case "pdl_intake":
			try {
				cmd_pdl_intake(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : add_event
		// Command format:
		//  add_event  event_id
		// Post a sync intake command for the given event.

		case "add_event":
			try {
				cmd_add_event(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : start_with_pdl
		// Command format:
		//  start_with_pdl  [pdl_option...]
		// Run the server, after parsing PDL options.

		case "start_with_pdl":
			try {
				cmd_start_with_pdl(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : start_comcat_poll
		// Command format:
		//  start_comcat_poll 
		// Post a command to start polling Comcat.

		case "start_comcat_poll":
			try {
				cmd_start_comcat_poll(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : stop_comcat_poll
		// Command format:
		//  stop_comcat_poll
		// Post a command to stop polling Comcat.

		case "stop_comcat_poll":
			try {
				cmd_stop_comcat_poll(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : start_pdl_cleanup
		// Command format:
		//  start_pdl_cleanup 
		// Post a command to start PDL cleanup.

		case "start_pdl_cleanup":
			try {
				cmd_start_pdl_cleanup(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : stop_pdl_cleanup
		// Command format:
		//  stop_pdl_cleanup
		// Post a command to stop PDL cleanup.

		case "stop_pdl_cleanup":
			try {
				cmd_stop_pdl_cleanup(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : show_version
		// Command format:
		//  show_version
		// Print version info to the standard output.

		case "show_version":
			try {
				cmd_show_version(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : make_indexes
		// Command format:
		//  make_indexes
		// Make indexes for all local database collections.
		// Note: It is not an error to make indexes that already exist; the existing indexes are unchanged.

		case "make_indexes":
			try {
				cmd_make_indexes(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : drop_indexes
		// Command format:
		//  drop_indexes
		// Drop indexes for all local database collections.
		// Note: It is not an error to drop indexes that have already been dropped.
		// Note: This does not delete any data.
		// Note: Indexes can be rebuilt by calling make_indexes afterward.

		case "drop_indexes":
			try {
				cmd_drop_indexes(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : erase_database_this_is_irreversible
		// Command format:
		//  erase_database_this_is_irreversible
		// Drop all local database collections, thereby erasing the database.
		// Note: This action is irreversible

		case "erase_database_this_is_irreversible":
			try {
				cmd_erase_database(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : check_collections
		// Command format:
		//  check_collections
		// Check for existence of our MongoDB collections.

		case "check_collections":
			try {
				cmd_check_collections(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : backup_database
		// Command format:
		//  backup_database  filename
		// Back up all local database collections.
		// Note: The entire database contents is stored into the given file.

		case "backup_database":
			try {
				cmd_backup_database(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : backup_database_gzip
		// Command format:
		//  backup_database_gzip  filename
		// Back up all local database collections, and gzip the file.
		// Note: The entire database contents is stored into the given file.

		case "backup_database_gzip":
			try {
				cmd_backup_database_gzip(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : restore_database
		// Command format:
		//  restore_database  filename
		// Restore all local database collections.
		// Note: The entire database contents is restored from the given file.
		// Note: The database must be empty (none of our collections can exist).
		// Note: This command also creates the collections and indexes.

		case "restore_database":
			try {
				cmd_restore_database(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : restore_database_gzip
		// Command format:
		//  restore_database_gzip  filename
		// Restore all local database collections, and gunzip the file.
		// Note: The entire database contents is restored from the given file.
		// Note: The database must be empty (none of our collections can exist).
		// Note: This command also creates the collections and indexes.

		case "restore_database_gzip":
			try {
				cmd_restore_database_gzip(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : init_relay_mode
		// Command format:
		//  init_relay_mode  relay_mode  configured_primary
		// Initialize the relay mode and configured primary server number.
		// The relay_mode can be: "solo", "watch", or "pair".
		// The configured_primary can be: 1 or 2.

		case "init_relay_mode":
			try {
				cmd_init_relay_mode(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : show_relay_status
		// Command format:
		//  show_relay_status  srvnum
		// Display the relay status, on a local or remote server.
		// The srvnum can be: 0 = local server, 1 or 2 = remote server, 9 = both remote servers.

		case "show_relay_status":
			try {
				cmd_show_relay_status(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : change_relay_mode
		// Command format:
		//  change_relay_mode  srvnum  relay_mode  configured_primary
		// Change the relay mode, on a running local or remote server.
		// The srvnum can be: 0 = local server, 1 or 2 = remote server, 9 = both remote servers.
		// The relay_mode can be: "solo", "watch", or "pair".
		// The configured_primary can be: 1 or 2.

		case "change_relay_mode":
			try {
				cmd_change_relay_mode(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : server_health
		// Command format:
		//  server_health  srvnum
		// Display the server health, on a local or remote server.
		// The srvnum can be: 0 = local server, 1 or 2 = remote server, 9 = both remote servers.

		case "server_health":
			try {
				cmd_server_health(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : init_analyst_cli
		// Command format:
		//  init_analyst_cli
		// Initialize analyst options on the local server, using the analyst CLI.
		// This is intended to be used when the server is not running (but MongoDB is started).

		case "init_analyst_cli":
			try {
				cmd_init_analyst_cli(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : change_analyst_cli
		// Command format:
		//  change_analyst_cli  srvnum
		// Change the analyst options, on a running local or remote server, using the analyst CLI.
		// The srvnum can be: 0 = local server, 1 or 2 = remote server, 9 = both remote servers.

		case "change_analyst_cli":
			try {
				cmd_change_analyst_cli(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		// Subcommand : fcdata_convert
		// Command format:
		//  fcdata_convert  in_filename  out_filename
		// Convert a forecast data file from the website/database JSON
		// format to a friendly human-understandable JSON format.

		case "fcdata_convert":
			try {
				cmd_fcdata_convert(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		}

		// Unrecognized subcommand.

		System.err.println ("ServerCmd : Unrecognized subcommand : " + args[0]);
		return;
	}
}
