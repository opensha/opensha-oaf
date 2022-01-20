package org.opensha.oaf.aafs;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.ArrayDeque;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;

import org.opensha.oaf.aafs.MongoDBUtil;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;
import org.opensha.oaf.rj.MagCompFn;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TimeSplitOutputStream;
import org.opensha.oaf.util.ConsoleRedirector;
import org.opensha.oaf.util.TestMode;

import org.opensha.oaf.util.health.HealthMonitor;
import org.opensha.oaf.util.health.SimpleHealthCounter;

import gov.usgs.earthquake.product.Product;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;


/**
 * Holds a set of tests for the AAFS server code.
 * Author: Michael Barall 03/16/2018.
 */
public class ServerTest {




	// Test #1 - Add a few elements to the task pending queue.

	public static void test1(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test1' or 'task_add_some' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
		
			String event_id;
			long sched_time;
			long submit_time;
			String submit_id;
			int opcode;
			int stage;
			MarshalWriter details;
		
			event_id = "Event_2";
			sched_time = 20100L;
			submit_time = 20000L;
			submit_id = "Submitter_2";
			opcode = 102;
			stage = 2;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_2");
			details.marshalLong (null, 21010L);
			details.marshalLong (null, 21020L);
			details.marshalDouble (null, 21030.0);
			details.marshalDouble (null, 21040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_4";
			sched_time = 40100L;
			submit_time = 40000L;
			submit_id = "Submitter_4_no_details";
			opcode = 104;
			stage = 4;
			details = null;
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_1";
			sched_time = 10100L;
			submit_time = 10000L;
			submit_id = "Submitter_1";
			opcode = 101;
			stage = 1;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_1");
			details.marshalLong (null, 11010L);
			details.marshalLong (null, 11020L);
			details.marshalDouble (null, 11030.0);
			details.marshalDouble (null, 11040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_5";
			sched_time = 50100L;
			submit_time = 50000L;
			submit_id = "Submitter_5";
			opcode = 105;
			stage = 5;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_5");
			details.marshalLong (null, 51010L);
			details.marshalLong (null, 51020L);
			details.marshalDouble (null, 51030.0);
			details.marshalDouble (null, 51040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_3";
			sched_time = 30100L;
			submit_time = 30000L;
			submit_id = "Submitter_3";
			opcode = 103;
			stage = 3;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_3");
			details.marshalLong (null, 31010L);
			details.marshalLong (null, 31020L);
			details.marshalDouble (null, 31030.0);
			details.marshalDouble (null, 31040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);

		}

		return;
	}




	// Test #2 - Display the pending task queue, unsorted.

	public static void test2(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test2' or 'task_display_unsorted' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks_unsorted();

			// Display them

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #3 - Display the pending task queue, sorted by execution time.

	public static void test3(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test3' or 'task_display_list' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #4 - Display the first task in the pending task queue, according to execution time.

	public static void test4(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test4' or 'task_display_first' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.get_first_task();

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #5 - Display the first task in the pending task queue, before cutoff time, according to execution time.

	public static void test5(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test5' or 'task_cutoff_first' subcommand");
			return;
		}

		long cutoff_time = Long.parseLong(args[1]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.get_first_ready_task (cutoff_time);

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #6 - Activate the first document before the cutoff time, and display the retrieved document.

	public static void test6(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test6' or 'task_cutoff_activate' subcommand");
			return;
		}

		long cutoff_time = Long.parseLong(args[1]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.activate_first_ready_task (cutoff_time);

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #7 - Activate the first document before the cutoff time, and stage it.

	public static void test7(String[] args) {

		// Three or four additional arguments

		if (args.length != 4 && args.length != 5) {
			System.err.println ("ServerTest : Invalid 'test7' or 'task_cutoff_activate_stage' subcommand");
			return;
		}

		long cutoff_time = Long.parseLong(args[1]);
		long exec_time = Long.parseLong(args[2]);
		int stage = Integer.parseInt(args[3]);
		String event_id = null;
		if (args.length >= 5) {
			event_id = args[4];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.activate_first_ready_task (cutoff_time);

			// Stage it

			if (task != null) {
				PendingTask.stage_task (task, exec_time, stage, event_id);
			}

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #8 - Activate the first document before the cutoff time, and delete it.

	public static void test8(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test8' or 'task_cutoff_activate_delete' subcommand");
			return;
		}

		long cutoff_time = Long.parseLong(args[1]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.activate_first_ready_task (cutoff_time);

			// Delete it

			if (task != null) {
				PendingTask.delete_task (task);
			}

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #9 - Run task dispatcher.

	public static void test9(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test9' or 'run_dispatcher' subcommand");
			return;
		}

		// Get a task dispatcher

		TaskDispatcher dispatcher = new TaskDispatcher();

		// Run it

		dispatcher.run();

		// Display final status

		System.out.println ("Dispatcher final state: " + TaskDispatcher.get_dispatcher_state_as_string (dispatcher.get_dispatcher_state()));

		return;
	}




	// Test #10 - Post a shutdown task.

	public static void test10(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test10' or 'post_shutdown' subcommand");
			return;
		}

		// Get a task dispatcher

		TaskDispatcher dispatcher = new TaskDispatcher();

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Post the shutdown task

		boolean result = dispatcher.post_shutdown ("ServerTest");

		// Display result

		System.out.println ("Post shutdown result: " + result);

		return;
	}




	// Test #11 - Scan the pending task queue, sorted, and write a log entry for each.

	public static void test11(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test11' or 'log_add_from_tasks' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Write the log entries

			for (PendingTask task : tasks) {
				LogEntry.submit_log_entry (task, task.get_sched_time() + 100L, task.get_opcode() + 1000, "Result_for_" + task.get_opcode());
			}

		}

		return;
	}




	// Test #12 - Scan the pending task queue, sorted, and search the log for each.

	public static void test12(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test12' or 'log_search_for_tasks' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Search for the log entries, display task and matching log entry

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
				LogEntry entry = LogEntry.get_log_entry_for_key (task.get_record_key());
				if (entry == null) {
					System.out.println ("LogEntry: null");
				} else {
					System.out.println (entry.toString());
				}
			}

		}

		return;
	}




	// Test #13 - Search the log for log time and/or event id.

	public static void test13(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test13' or 'log_query_list' subcommand");
			return;
		}

		long log_time_lo = Long.parseLong(args[1]);
		long log_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching log entries

			List<LogEntry> entries = LogEntry.get_log_entry_range (log_time_lo, log_time_hi, event_id);

			// Display them

			for (LogEntry entry : entries) {
				System.out.println (entry.toString());
			}

		}

		return;
	}




	// Test #14 - Search the log for log time and/or event id, and delete the entries.

	public static void test14(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test14' or 'log_query_list_delete' subcommand");
			return;
		}

		long log_time_lo = Long.parseLong(args[1]);
		long log_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching log entries

			List<LogEntry> entries = LogEntry.get_log_entry_range (log_time_lo, log_time_hi, event_id);

			// Display them, and delete

			for (LogEntry entry : entries) {
				System.out.println (entry.toString());
				LogEntry.delete_log_entry (entry);
			}

		}

		return;
	}




	// Test #15 - Post a task with given event id, opcode, stage, and details.

	public static void test15(String[] args) {

		// Three or four additional arguments

		if (args.length != 4 && args.length != 5) {
			System.err.println ("ServerTest : Invalid 'test15' or 'post_task' subcommand");
			return;
		}

		String event_id = args[1];
		if (event_id.equalsIgnoreCase ("-")) {
			event_id = "";
		}
		int opcode = Integer.parseInt(args[2]);
		int stage = Integer.parseInt(args[3]);
		MarshalWriter details = PendingTask.begin_details();
		if (args.length == 5) {
			details.marshalMapBegin (null);
			details.marshalString ("value", args[4]);
			details.marshalMapEnd ();
		}

		// Post the task

		long the_time = ServerClock.get_time();

		TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, details);

		return;
	}




	// Test #16 - Execute the next task.

	public static void test16(String[] args) {

		// Zero or one additional arguments

		if (args.length < 1 || args.length > 2) {
			System.err.println ("ServerTest : Invalid 'test16' or 'exec_task' subcommand");
			return;
		}

		boolean f_adjust_time = false;
		if (args.length >= 2) {
			f_adjust_time = Boolean.parseBoolean (args[1]);
		}

		// Get a task dispatcher

		TaskDispatcher dispatcher = new TaskDispatcher();

		// Run one task

		boolean f_verbose = true;

		dispatcher.run_next_task (f_verbose, f_adjust_time);

		return;
	}




	// Test #17 - Write a catalog snapshot.

	public static void test17(String[] args) {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test17' or 'catsnap_add' subcommand");
			return;
		}

		double start_time_days = Double.parseDouble (args[1]);
		double end_time_days   = Double.parseDouble (args[2]);
		String event_id = args[3];

		long start_time = Math.round(start_time_days * 86400000L);
		long end_time   = Math.round(end_time_days   * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Create a simulated aftershock sequence, using the method of RJ_AftershockModel_SequenceSpecific
			// (Start and end times should be in the range of 0 to 30 days)
			
			double a = -1.67;
			double b = 0.91;
			double c = 0.05;
			double p = 1.08;
			double magMain = 7.5;
			double magCat = 2.5;
			double capF = 0.5;
			double capG = 1.25;
			double capH = 0.75;

			MagCompFn magCompFn = MagCompFn.makePageOrConstant (capF, capG, capH);

			ObsEqkRupList aftershockList = AftershockStatsCalc.simAftershockSequence(a, b, magMain, magCat, magCompFn, p, c, start_time_days, end_time_days);

			CompactEqkRupList rupture_list = new CompactEqkRupList (aftershockList);

			// Write the rupture sequence

			CatalogSnapshot entry_in = CatalogSnapshot.submit_catalog_shapshot (null, event_id, start_time, end_time, rupture_list);

			System.out.println (entry_in.toString());

			// Search for it

			CatalogSnapshot entry_out = CatalogSnapshot.get_catalog_shapshot_for_key (entry_in.get_record_key());

			System.out.println (entry_out.toString());

			// Use the retrieved rupture sequence to make a sequence-specific model
		
			double min_a = -2.0;
			double max_a = -1.0;
			int num_a = 101;

			double min_p = 0.9; 
			double max_p = 1.2; 
			int num_p = 31;
		
			double min_c=0.05;
			double max_c=0.05;
			int num_c=1;

			ObsEqkRupture mainShock = new ObsEqkRupture("0", 0L, null, magMain);

			CompactEqkRupList rupture_list_out = entry_out.get_rupture_list();

			// Make the model, it will output some information

			RJ_AftershockModel_SequenceSpecific seqModel =
				new RJ_AftershockModel_SequenceSpecific(mainShock, rupture_list_out,
			 								magCat, magCompFn,
											b, start_time_days, end_time_days,
											min_a, max_a, num_a, 
											min_p, max_p, num_p, 
											min_c, max_c, num_c);

		}

		return;
	}




	// Test #18 - Search the catalog snapshots for end time and/or event id.

	public static void test18(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test18' or 'catsnap_query_list' subcommand");
			return;
		}

		double end_time_lo_days = Double.parseDouble (args[1]);
		double end_time_hi_days = Double.parseDouble (args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		long end_time_lo = Math.round(end_time_lo_days * 86400000L);
		long end_time_hi = Math.round(end_time_hi_days * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching catalog snapshots

			List<CatalogSnapshot> entries = CatalogSnapshot.get_catalog_snapshot_range (end_time_lo, end_time_hi, event_id);

			// Display them

			for (CatalogSnapshot entry : entries) {
				System.out.println (entry.toString());
			}

		}

		return;
	}




	// Test #19 - Search the catalog snapshots for end time and/or event id, and delete the matching entries.

	public static void test19(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test19' or 'catsnap_query_delete' subcommand");
			return;
		}

		double end_time_lo_days = Double.parseDouble (args[1]);
		double end_time_hi_days = Double.parseDouble (args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		long end_time_lo = Math.round(end_time_lo_days * 86400000L);
		long end_time_hi = Math.round(end_time_hi_days * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching catalog snapshots

			List<CatalogSnapshot> entries = CatalogSnapshot.get_catalog_snapshot_range (end_time_lo, end_time_hi, event_id);

			// Display them, and delete

			for (CatalogSnapshot entry : entries) {
				System.out.println (entry.toString());
				CatalogSnapshot.delete_catalog_snapshot (entry);
			}

		}

		return;
	}




	// Test #20 - Add a few elements to the timeline.

	public static void test20(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test20' or 'tline_add_some' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
		
			String event_id;
			String[] comcat_ids;
			long action_time;
			int actcode;
			MarshalWriter details;
		
			event_id = "Event_2";
			comcat_ids = new String[]{"ccid_21", "ccid_22", "ccid_23"};
			action_time = 20102L;
			actcode = 102;
			details = TimelineEntry.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_2");
			details.marshalLong (null, 21010L);
			details.marshalLong (null, 21020L);
			details.marshalDouble (null, 21030.0);
			details.marshalDouble (null, 21040.0);
			details.marshalArrayEnd ();
			TimelineEntry.submit_timeline_entry (null, action_time, event_id,
				comcat_ids, actcode, details);
		
			event_id = "Event_4";
			comcat_ids = new String[]{"ccid_41", "ccid_42", "ccid_43", "ccid_23"};
			action_time = 40104L;
			actcode = 104;
			details = null;
			TimelineEntry.submit_timeline_entry (null, action_time, event_id,
				comcat_ids, actcode, details);
		
			event_id = "Event_1";
			comcat_ids = new String[]{"ccid_11", "ccid_12", "ccid_13"};
			action_time = 10101L;
			actcode = 101;
			details = TimelineEntry.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_1");
			details.marshalLong (null, 11010L);
			details.marshalLong (null, 11020L);
			details.marshalDouble (null, 11030.0);
			details.marshalDouble (null, 11040.0);
			details.marshalArrayEnd ();
			TimelineEntry.submit_timeline_entry (null, action_time, event_id,
				comcat_ids, actcode, details);
		
			event_id = "Event_5";
			comcat_ids = new String[]{"ccid_51", "ccid_52", "ccid_53", "ccid_23", "ccid_13"};
			action_time = 50105L;
			actcode = 105;
			details = TimelineEntry.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_5");
			details.marshalLong (null, 51010L);
			details.marshalLong (null, 51020L);
			details.marshalDouble (null, 51030.0);
			details.marshalDouble (null, 51040.0);
			details.marshalArrayEnd ();
			TimelineEntry.submit_timeline_entry (null, action_time, event_id,
				comcat_ids, actcode, details);
		
			event_id = "Event_3";
			comcat_ids = new String[]{"ccid_31", "ccid_32"};
			action_time = 30103L;
			actcode = 103;
			details = TimelineEntry.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_3");
			details.marshalLong (null, 31010L);
			details.marshalLong (null, 31020L);
			details.marshalDouble (null, 31030.0);
			details.marshalDouble (null, 31040.0);
			details.marshalArrayEnd ();
			TimelineEntry.submit_timeline_entry (null, action_time, event_id,
				comcat_ids, actcode, details);

		}

		return;
	}




	// Test #21 - Search the timeline for action time and/or event id; using list.

	public static void test21(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test21' or 'tline_query_list' subcommand");
			return;
		}

		long action_time_lo = Long.parseLong(args[1]);
		long action_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] action_time_div_rem = null;
		if (div_rem > 0L) {
			action_time_div_rem = new long[2];
			action_time_div_rem[0] = div_rem / 1000L;
			action_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching timeline entries

			List<TimelineEntry> entries = TimelineEntry.get_timeline_entry_range (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);

			// Display them

			for (TimelineEntry entry : entries) {
				System.out.println (entry.toString());
			}

		}

		return;
	}




	// Test #22 - Search the timeline for action time and/or event id; using iterator.

	public static void test22(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test22' or 'tline_query_iterate' subcommand");
			return;
		}

		long action_time_lo = Long.parseLong(args[1]);
		long action_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] action_time_div_rem = null;
		if (div_rem > 0L) {
			action_time_div_rem = new long[2];
			action_time_div_rem[0] = div_rem / 1000L;
			action_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching timeline entries

				RecordIterator<TimelineEntry> entries = TimelineEntry.fetch_timeline_entry_range (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);
			){

				// Display them

				for (TimelineEntry entry : entries) {
					System.out.println (entry.toString());
				}
			}
		}

		return;
	}




	// Test #23 - Search the timeline for action time and/or event id; and re-fetch the entries.

	public static void test23(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test23' or 'tline_query_refetch' subcommand");
			return;
		}

		long action_time_lo = Long.parseLong(args[1]);
		long action_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] action_time_div_rem = null;
		if (div_rem > 0L) {
			action_time_div_rem = new long[2];
			action_time_div_rem[0] = div_rem / 1000L;
			action_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching timeline entries

			List<TimelineEntry> entries = TimelineEntry.get_timeline_entry_range (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);

			// Display them, and re-fetch

			for (TimelineEntry entry : entries) {
				System.out.println (entry.toString());
				TimelineEntry refetch = TimelineEntry.get_timeline_entry_for_key (entry.get_record_key());
				System.out.println (refetch.toString());
			}
		}

		return;
	}




	// Test #24 - Search the timeline for action time and/or event id; and delete the entries.

	public static void test24(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test24' or 'tline_query_delete' subcommand");
			return;
		}

		long action_time_lo = Long.parseLong(args[1]);
		long action_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] action_time_div_rem = null;
		if (div_rem > 0L) {
			action_time_div_rem = new long[2];
			action_time_div_rem[0] = div_rem / 1000L;
			action_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching timeline entries

			List<TimelineEntry> entries = TimelineEntry.get_timeline_entry_range (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);

			// Display them, and delete

			for (TimelineEntry entry : entries) {
				System.out.println (entry.toString());
				TimelineEntry.delete_timeline_entry (entry);
			}
		}

		return;
	}




	// Test #25 - Display the pending task queue, sorted by execution time, using iterator.

	public static void test25(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test25' or 'task_display_iterate' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over pending tasks

				RecordIterator<PendingTask> tasks = PendingTask.fetch_all_tasks();
			){

				// Display them

				for (PendingTask task : tasks) {
					System.out.println (task.toString());
				}
			}
		}

		return;
	}




	// Test #26 - Search the log for log time and/or event id, using iterator.

	public static void test26(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test26' or 'log_query_iterate' subcommand");
			return;
		}

		long log_time_lo = Long.parseLong(args[1]);
		long log_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching log entries

				RecordIterator<LogEntry> entries = LogEntry.fetch_log_entry_range (log_time_lo, log_time_hi, event_id);
			){

				// Display them

				for (LogEntry entry : entries) {
					System.out.println (entry.toString());
				}
			}
		}

		return;
	}




	// Test #27 - Search the catalog snapshots for end time and/or event id, using iterator.

	public static void test27(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test27' or 'catsnap_query_iterate' subcommand");
			return;
		}

		double end_time_lo_days = Double.parseDouble (args[1]);
		double end_time_hi_days = Double.parseDouble (args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		long end_time_lo = Math.round(end_time_lo_days * 86400000L);
		long end_time_hi = Math.round(end_time_hi_days * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching catalog snapshots

				RecordIterator<CatalogSnapshot> entries = CatalogSnapshot.fetch_catalog_snapshot_range (end_time_lo, end_time_hi, event_id);
			){

				// Display them

				for (CatalogSnapshot entry : entries) {
					System.out.println (entry.toString());
				}
			}
		}

		return;
	}




	// Test #28 - Post a console message task with given stage and message.

	public static void test28(String[] args) {

		// Two additional arguments

		if (args.length != 3) {
			System.err.println ("ServerTest : Invalid 'test28' subcommand");
			return;
		}

		int stage = Integer.parseInt(args[1]);

		OpConsoleMessage payload = new OpConsoleMessage();
		payload.message = args[2];

		String event_id = "";
		int opcode = TaskDispatcher.OPCODE_CON_MESSAGE;

		// Post the task

		long the_time = ServerClock.get_time();

		TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		return;
	}




	// Test #29 - Search the task queue for execution time and/or event id.

	public static void test29(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test29' or 'task_query_list' subcommand");
			return;
		}

		long exec_time_lo = Long.parseLong(args[1]);
		long exec_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching tasks

			List<PendingTask> tasks = PendingTask.get_task_entry_range (exec_time_lo, exec_time_hi, event_id);

			// Display them

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #30 - Search the task queue for execution time and/or event id, using iterator.

	public static void test30(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test30' or 'task_query_iterate' subcommand");
			return;
		}

		long exec_time_lo = Long.parseLong(args[1]);
		long exec_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching tasks

				RecordIterator<PendingTask> tasks = PendingTask.fetch_task_entry_range (exec_time_lo, exec_time_hi, event_id);
			){

				// Display them

				for (PendingTask task : tasks) {
					System.out.println (task.toString());
				}
			}
		}

		return;
	}




	// Test #31 - Search the timeline for action time and/or event id; get most recent.

	public static void test31(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test31' or 'tline_query_first' subcommand");
			return;
		}

		long action_time_lo = Long.parseLong(args[1]);
		long action_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] action_time_div_rem = null;
		if (div_rem > 0L) {
			action_time_div_rem = new long[2];
			action_time_div_rem[0] = div_rem / 1000L;
			action_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the most recent matching timeline entry

			TimelineEntry entry = TimelineEntry.get_recent_timeline_entry (action_time_lo, action_time_hi, event_id, comcat_ids, action_time_div_rem);

			// Display it

			if (entry == null) {
				System.out.println ("null");
			} else {
				System.out.println (entry.toString());
			}

		}

		return;
	}




	// Test #32 - Post a sync intake command for the given event.

	public static void test32(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test32' subcommand");
			return;
		}

		String event_id = args[1];

		OpIntakeSync payload = new OpIntakeSync();
		payload.setup ();

		int opcode = TaskDispatcher.OPCODE_INTAKE_SYNC;
		int stage = 0;

		// Post the task

		long the_time = ServerClock.get_time();

		TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		return;
	}




	// Test #33 - Parse a PDL intake command for the given command line.

	public static void test33(String[] args) {

		// At least one additional argument

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test33' subcommand");
			return;
		}

		OpIntakePDL payload = new OpIntakePDL();

		payload.setup (args, 1, args.length);

		System.out.println ("PDL arguments:");
		for (String s : payload.pdl_args) {
			System.out.println (s);
		}

		System.out.println ("Parsed values:");
		System.out.println ("pdl_status = " + payload.pdl_status);
		System.out.println ("pdl_action = " + payload.pdl_action);
		System.out.println ("pdl_type = " + payload.pdl_type);
		System.out.println ("event_id = " + payload.event_id);
		System.out.println ("mainshock_time = " + payload.mainshock_time);
		System.out.println ("mainshock_mag = " + payload.mainshock_mag);
		System.out.println ("mainshock_lat = " + payload.mainshock_lat);
		System.out.println ("mainshock_lon = " + payload.mainshock_lon);
		System.out.println ("mainshock_depth = " + payload.mainshock_depth);

		return;
	}




	// Test #34 - Post a PDL intake command for the given command line.

	public static void test34(String[] args) {

		// At least one additional argument

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test34' subcommand");
			return;
		}

		OpIntakePDL payload = new OpIntakePDL();

		payload.setup (args, 1, args.length);

		System.out.println ("PDL arguments:");
		for (String s : payload.pdl_args) {
			System.out.println (s);
		}

		System.out.println ("Parsed values:");
		System.out.println ("pdl_status = " + payload.pdl_status);
		System.out.println ("pdl_action = " + payload.pdl_action);
		System.out.println ("pdl_type = " + payload.pdl_type);
		System.out.println ("event_id = " + payload.event_id);
		System.out.println ("mainshock_time = " + payload.mainshock_time);
		System.out.println ("mainshock_mag = " + payload.mainshock_mag);
		System.out.println ("mainshock_lat = " + payload.mainshock_lat);
		System.out.println ("mainshock_lon = " + payload.mainshock_lon);
		System.out.println ("mainshock_depth = " + payload.mainshock_depth);

		String event_id = payload.event_id;

		int opcode = TaskDispatcher.OPCODE_INTAKE_PDL;
		int stage = 0;

		// Post the task

		long the_time = ServerClock.get_time();

		TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		return;
	}




	// Test #35 - Add a few elements to the alias families.

	public static void test35(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test35' or 'alias_add_some' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
		
			String timeline_id;
			String[] comcat_ids;
			String[] removed_ids;
			long family_time;
			AliasAssignment assignment;
			AliasAssignmentList assignments;
		

			timeline_id = "event_1";
			comcat_ids = new String[]{"ccid_11", "ccid_12", "ccid_13"};
			removed_ids = new String[0];
			family_time = 10101L;
			assignment = new AliasAssignment();
			assignments = new AliasAssignmentList();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			AliasFamily.submit_alias_family (null, family_time, assignments);
		

			timeline_id = "event_2";
			comcat_ids = new String[]{"ccid_21", "ccid_22", "ccid_23"};
			removed_ids = new String[]{"rmid_21", "rmid_22"};
			family_time = 20102L;
			assignment = new AliasAssignment();
			assignments = new AliasAssignmentList();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			AliasFamily.submit_alias_family (null, family_time, assignments);
		

			timeline_id = "event_3";
			comcat_ids = new String[]{"ccid_31"};
			removed_ids = new String[]{"rmid_31"};
			family_time = 30103L;
			assignment = new AliasAssignment();
			assignments = new AliasAssignmentList();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			AliasFamily.submit_alias_family (null, family_time, assignments);
		

			timeline_id = "event_1";
			comcat_ids = new String[]{"ccid_42", "ccid_11"};
			removed_ids = new String[]{"ccid_12", "ccid_13"};
			family_time = 40104L;
			assignment = new AliasAssignment();
			assignments = new AliasAssignmentList();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			timeline_id = "event_4";
			comcat_ids = new String[]{"ccid_41", "ccid_12"};
			removed_ids = new String[]{"ccid_13"};
			assignment = new AliasAssignment();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			AliasFamily.submit_alias_family (null, family_time, assignments);
		

			timeline_id = "event_2";
			comcat_ids = new String[]{"ccid_21"};
			removed_ids = new String[]{"rmid_21", "rmid_22", "ccid_23", "ccid_22"};
			family_time = 50105L;
			assignment = new AliasAssignment();
			assignments = new AliasAssignmentList();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			timeline_id = "event_3";
			comcat_ids = new String[]{"ccid_31", "ccid_23"};
			removed_ids = new String[]{"rmid_31"};
			assignment = new AliasAssignment();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			timeline_id = "event_5";
			comcat_ids = new String[]{"ccid_22"};
			removed_ids = new String[0];
			assignment = new AliasAssignment();
			assignment.set_timeline_id (timeline_id);
			assignment.set_comcat_ids_from_array (comcat_ids);
			for (String id : removed_ids) {
				assignment.add_removed_id (id);
			}
			assignments.add_assignment (assignment);
			AliasFamily.submit_alias_family (null, family_time, assignments);

		}

		return;
	}




	// Test #36 - Search the alias families for family time and/or event id; using list.

	public static void test36(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test36' or 'alias_query_list' subcommand");
			return;
		}

		long family_time_lo = Long.parseLong(args[1]);
		long family_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] family_time_div_rem = null;
		if (div_rem > 0L) {
			family_time_div_rem = new long[2];
			family_time_div_rem[0] = div_rem / 1000L;
			family_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching alias family entries

			List<AliasFamily> entries = AliasFamily.get_alias_family_range (family_time_lo, family_time_hi, event_id, comcat_ids, family_time_div_rem);

			// Display them

			for (AliasFamily entry : entries) {
				System.out.println (entry.toString());
				System.out.println (entry.get_assignments().toString());
			}

		}

		return;
	}




	// Test #37 - Search the alias families for family time and/or event id; using iterator.

	public static void test37(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test37' or 'alias_query_iterate' subcommand");
			return;
		}

		long family_time_lo = Long.parseLong(args[1]);
		long family_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] family_time_div_rem = null;
		if (div_rem > 0L) {
			family_time_div_rem = new long[2];
			family_time_div_rem[0] = div_rem / 1000L;
			family_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching alias family entries

				RecordIterator<AliasFamily> entries = AliasFamily.fetch_alias_family_range (family_time_lo, family_time_hi, event_id, comcat_ids, family_time_div_rem);
			){

				// Display them

				for (AliasFamily entry : entries) {
					System.out.println (entry.toString());
					System.out.println (entry.get_assignments().toString());
				}
			}
		}

		return;
	}




	// Test #38 - Search the alias families for family time and/or event id; and re-fetch the entries.

	public static void test38(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test38' or 'alias_query_refetch' subcommand");
			return;
		}

		long family_time_lo = Long.parseLong(args[1]);
		long family_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] family_time_div_rem = null;
		if (div_rem > 0L) {
			family_time_div_rem = new long[2];
			family_time_div_rem[0] = div_rem / 1000L;
			family_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching alias family entries

			List<AliasFamily> entries = AliasFamily.get_alias_family_range (family_time_lo, family_time_hi, event_id, comcat_ids, family_time_div_rem);

			// Display them, and re-fetch

			for (AliasFamily entry : entries) {
				System.out.println (entry.toString());
				System.out.println (entry.get_assignments().toString());
				AliasFamily refetch = AliasFamily.get_alias_family_for_key (entry.get_record_key());
				System.out.println (refetch.toString());
				System.out.println (refetch.get_assignments().toString());
			}
		}

		return;
	}




	// Test #39 - Search the alias families for family time and/or event id; and delete the entries.

	public static void test39(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test39' or 'alias_query_delete' subcommand");
			return;
		}

		long family_time_lo = Long.parseLong(args[1]);
		long family_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] family_time_div_rem = null;
		if (div_rem > 0L) {
			family_time_div_rem = new long[2];
			family_time_div_rem[0] = div_rem / 1000L;
			family_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching alias family entries

			List<AliasFamily> entries = AliasFamily.get_alias_family_range (family_time_lo, family_time_hi, event_id, comcat_ids, family_time_div_rem);

			// Display them, and delete

			for (AliasFamily entry : entries) {
				System.out.println (entry.toString());
				System.out.println (entry.get_assignments().toString());
				AliasFamily.delete_alias_family (entry);
			}
		}

		return;
	}




	// Test #40 - Search the alias families for family time and/or event id; get most recent.

	public static void test40(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test40' or 'alias_query_first' subcommand");
			return;
		}

		long family_time_lo = Long.parseLong(args[1]);
		long family_time_hi = Long.parseLong(args[2]);
		long div_rem = Long.parseLong(args[3]);
		long[] family_time_div_rem = null;
		if (div_rem > 0L) {
			family_time_div_rem = new long[2];
			family_time_div_rem[0] = div_rem / 1000L;
			family_time_div_rem[1] = div_rem % 1000L;
		}
		String event_id = null;
		if (args.length >= 5) {
			if (!( args[4].equalsIgnoreCase("-") )) {
				event_id = args[4];
			}
		}
		String[] comcat_ids = null;
		if (args.length >= 6) {
			comcat_ids = Arrays.copyOfRange (args, 5, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the most recent matching alias family entry

			AliasFamily entry = AliasFamily.get_recent_alias_family (family_time_lo, family_time_hi, event_id, comcat_ids, family_time_div_rem);

			// Display it

			if (entry == null) {
				System.out.println ("null");
			} else {
				System.out.println (entry.toString());
				System.out.println (entry.get_assignments().toString());
			}

		}

		return;
	}




	// Test #41 - Delete a product from PDL-Development.

	public static void test41(String[] args) throws Exception {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test41' or 'pdl_dev_delete' subcommand");
			return;
		}

		String eventID = args[1];
		String eventNetwork = args[2];
		String eventCode = args[3];

		// Direct operation to PDL-Development

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

		// Construct the deletion product

		boolean isReviewed = false;
		long modifiedTime = 0L;
		Product product = PDLProductBuilderOaf.createDeletionProduct (eventID, eventNetwork, eventCode, isReviewed, modifiedTime);

		// Send to PDL

		PDLSender.signProduct(product);
		PDLSender.sendProduct(product, true);

		return;
	}




	// Test #42 - Read an alias list from a file, and store it in the database.

	public static void test42(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test42' or 'alias_add_from_file' subcommand");
			return;
		}

		String filename = args[1];

		// Read the alias list

		AliasAssignmentList assignments;
		long family_time;

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			
			assignments = (new AliasAssignmentList()).unmarshal (reader, null);

			family_time = assignments.get_family_time();
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Write the database entry
		
			AliasFamily.submit_alias_family (null, family_time, assignments);
		}

		return;
	}




	// Test #43 - Get current alias information for a timeline.

	public static void test43(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test43' or 'alias_get_timeline_info' subcommand");
			return;
		}

		String timeline_id = args[1];

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Get timeline alias information

			ForecastMainshock fcmain = new ForecastMainshock();

			int rescode = sg.alias_sup.get_mainshock_for_timeline_id (timeline_id, fcmain);

			// Write the result code

			System.out.println ("result code = " + sg.get_rescode_as_string (rescode));

			// Display mainshock info if we got it

			if (rescode == ServerComponent.RESCODE_SUCCESS) {
				System.out.println (fcmain.toString());
			}
		}

		return;
	}




	// Test #44 - Get current alias information for an event.

	public static void test44(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test44' or 'alias_get_event_info' subcommand");
			return;
		}

		String event_id = args[1];

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Get timeline alias information

			ForecastMainshock fcmain = new ForecastMainshock();

			int rescode = sg.alias_sup.get_mainshock_for_event_id (event_id, fcmain);

			// If event was not found in Comcat, then touch the database because get_mainshock_for_event_id didn't (needed for unit test)

			if (rescode == ServerComponent.RESCODE_ALIAS_EVENT_NOT_IN_COMCAT) {
				AliasFamily.get_recent_alias_family (0L, 100L, null, null, null);
			}

			// Write the result code

			System.out.println ("result code = " + sg.get_rescode_as_string (rescode));

			// Display mainshock info if we got it

			if (rescode == ServerComponent.RESCODE_SUCCESS || rescode == ServerComponent.RESCODE_ALIAS_NEW_EVENT) {
				System.out.println (fcmain.toString());
			}
		}

		return;
	}




	// Test #45 - Get current alias information for an event, create timeline if new event.

	public static void test45(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test45' or 'alias_create_timeline_for_event' subcommand");
			return;
		}

		String event_id = args[1];

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Get timeline alias information

			ForecastMainshock fcmain = new ForecastMainshock();

			int rescode = sg.alias_sup.get_mainshock_for_event_id (event_id, fcmain);

			// If event was not found in Comcat, then touch the database because get_mainshock_for_event_id didn't (needed for unit test)

			if (rescode == ServerComponent.RESCODE_ALIAS_EVENT_NOT_IN_COMCAT) {
				AliasFamily.get_recent_alias_family (0L, 100L, null, null, null);
			}

			// Write the result code

			System.out.println ("result code = " + sg.get_rescode_as_string (rescode));

			// If it's a new event, create the timeline

			if (rescode == ServerComponent.RESCODE_ALIAS_NEW_EVENT) {
				sg.alias_sup.write_mainshock_to_new_timeline (fcmain);
			}

			// Display mainshock info if we got it

			if (rescode == ServerComponent.RESCODE_SUCCESS || rescode == ServerComponent.RESCODE_ALIAS_NEW_EVENT) {
				System.out.println (fcmain.toString());
			}
		}

		return;
	}




	// Test #46 - Test console redirection and time split output streams.

	public static void test46(String[] args) throws IOException {

		// No additional arguments

		if (args.length != 1 && args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test46' subcommand");
			return;
		}

		long day_millis = 86400000L;

		long day_1 = SimpleUtils.get_system_time();
		long day_2 = day_1 + day_millis;
		long day_3 = day_2 + day_millis;
		long day_4 = day_3 + day_millis;
		long day_5 = day_4 + day_millis;
		long day_6 = day_5 + day_millis;
		long day_7 = day_6 + day_millis;
		long day_8 = day_7 + day_millis;

		String pattern_test = "'logtest/logs/'yyyy-MM-dd'-test.log'";
		String pattern_out = "'logtest/logs/'yyyy-MM-dd'-out.log'";
		String pattern_err = "'logtest/logs/'yyyy-MM-dd'-err.log'";

		if (args.length == 2) {
			pattern_test = "'" + args[1] + "/'yyyy-MM-dd'-test.log'";
			pattern_out = "'" + args[1] + "/'yyyy-MM-dd'-out.log'";
			pattern_err = "'" + args[1] + "/'yyyy-MM-dd'-err.log'";
		}

		System.out.println ("Start time = " + day_1 + " (" + SimpleUtils.time_to_string (day_1) + ")");

		// No redirection

		System.out.println ("con - out - line 1");
		System.err.println ("con - err - line 1");

		// Redirection and write within a single day, create directories

		try (
			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (pattern_test, day_1);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));
		){
			System.out.println ("day 1 - out - line 2");
			System.err.println ("day 1 - err - line 2");
			System.out.println ("day 1 - out - line 3");
			System.err.println ("day 1 - err - line 3");

			con_tsop.redirect (day_1);
		
			System.out.println ("day 1 - out - line 4");
			System.err.println ("day 1 - err - line 4");
			System.out.println ("day 1 - out - line 5");
			System.err.println ("day 1 - err - line 5");
		}

		// Redirection and write within two days, append to file for first day

		try (
			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (pattern_test, day_1);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));
		){
			System.out.println ("day 1 - out - line 6");
			System.err.println ("day 1 - err - line 6");
			System.out.println ("day 1 - out - line 7");
			System.err.println ("day 1 - err - line 7");

			con_tsop.redirect (day_2);
		
			System.out.println ("day 2 - out - line 8");
			System.err.println ("day 2 - err - line 8");
			System.out.println ("day 2 - out - line 9");
			System.err.println ("day 2 - err - line 9");
		}

		// Redirection and write within three days, test lazy open by not writing for two days

		try (
			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop (pattern_test, day_3);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));
		){
			con_tsop.redirect (day_4);
		
			System.out.println ("day 4 - out - line 10");
			System.err.println ("day 4 - err - line 10");

			con_tsop.redirect (day_4);
		
			System.out.println ("day 4 - out - line 11");
			System.err.println ("day 4 - err - line 11");

			con_tsop.redirect (day_5);
		}

		// Redirection and write within two days, with tee and separated files

		try (
			TimeSplitOutputStream con_tsop_out = TimeSplitOutputStream.make_tsop (pattern_out, day_6);
			TimeSplitOutputStream con_tsop_err = TimeSplitOutputStream.make_tsop (pattern_err, day_6);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop_out, con_tsop_err, false, true);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop_out,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop_err,
										ConsoleRedirector.get_new_err (con_red));
		){
			System.out.println ("day 6 - tee - out - line 12");
			System.err.println ("day 6 - tee - err - line 12");
			System.out.println ("day 6 - tee - out - line 13");
			System.err.println ("day 6 - tee - err - line 13");

			con_tsop_out.redirect (day_7);
			con_tsop_err.redirect (day_7);
		
			System.out.println ("day 7 - tee - out - line 14");
			System.err.println ("day 7 - tee - err - line 14");

			con_tsop_out.redirect (day_7);
			con_tsop_err.redirect (day_7);

			System.out.println ("day 7 - tee - out - line 15");
			System.err.println ("day 7 - tee - err - line 15");
		}

		// Redirection with empty pattern

		try (
			TimeSplitOutputStream con_tsop = TimeSplitOutputStream.make_tsop ("", day_8);
			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (con_tsop, false, false);
			Closeable auto_out = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_out (con_red));
			Closeable auto_err = TimeSplitOutputStream.add_auto_upstream (con_tsop,
										ConsoleRedirector.get_new_err (con_red));
		){
			System.out.println ("day 8 - empty - out - line 16");
			System.err.println ("day 8 - empty - err - line 16");
			System.out.println ("day 8 - empty - out - line 17");
			System.err.println ("day 8 - empty - err - line 17");
		}

		// No redirection

		System.out.println ("con - out - line 18");
		System.err.println ("con - err - line 18");

		return;
	}




	// Test #47 - Delete all database tables, allowing a fresh start.

	public static void test47(String[] args) throws Exception {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test47' or 'delete_all_tables' subcommand");
			return;
		}

		if (!( args[1].equals ("delete")
			&& args[2].equals ("all")
			&& args[3].equals ("tables") )) {
			System.err.println ("ServerTest : Wrong confirmation for 'test47' or 'delete_all_tables' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			
			System.out.println ("ServerTest : Deleting all database tables");

			// Get the list of task entries and delete

			List<PendingTask> task_entries = PendingTask.get_task_entry_range (0L, 0L, null, PendingTask.UNSORTED);
		
			System.out.println ("ServerTest : Deleting " + task_entries.size() + " task entries");

			for (PendingTask entry : task_entries) {
				PendingTask.delete_task (entry);
			}

			// Get the list of log entries and delete

			List<LogEntry> log_entries = LogEntry.get_log_entry_range (0L, 0L, null, LogEntry.UNSORTED);
		
			System.out.println ("ServerTest : Deleting " + log_entries.size() + " log entries");

			for (LogEntry entry : log_entries) {
				LogEntry.delete_log_entry (entry);
			}

			// Get the list of catalog snapshots and delete

			List<CatalogSnapshot> cat_entries = CatalogSnapshot.get_catalog_snapshot_range (0L, 0L, null, CatalogSnapshot.UNSORTED);
		
			System.out.println ("ServerTest : Deleting " + cat_entries.size() + " catalog snapshot entries");

			for (CatalogSnapshot entry : cat_entries) {
				CatalogSnapshot.delete_catalog_snapshot (entry);
			}

			// Get the list of timeline entries and delete

			List<TimelineEntry> tline_entries = TimelineEntry.get_timeline_entry_range (0L, 0L, null, null, null, TimelineEntry.UNSORTED);
		
			System.out.println ("ServerTest : Deleting " + tline_entries.size() + " timeline entries");

			for (TimelineEntry entry : tline_entries) {
				TimelineEntry.delete_timeline_entry (entry);
			}

			// Get the list of alias family entries and delete

			List<AliasFamily> alfam_entries = AliasFamily.get_alias_family_range (0L, 0L, null, null, null, AliasFamily.UNSORTED);
		
			System.out.println ("ServerTest : Deleting " + alfam_entries.size() + " alias family entries");

			for (AliasFamily entry : alfam_entries) {
				AliasFamily.delete_alias_family (entry);
			}

			// Get the list of relay item entries and delete

			List<RelayItem> relit_entries = RelayItem.get_relay_item_range (RelayItem.UNSORTED, 0L, 0L);
		
			System.out.println ("ServerTest : Deleting " + relit_entries.size() + " relay item entries");

			for (RelayItem entry : relit_entries) {
				RelayItem.delete_relay_item (entry);
			}
			
			System.out.println ("ServerTest : Deleted all database tables");

		}

		return;
	}




	// Test #48 - Post a task to start polling Comcat.

	public static void test48(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test48' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_POLL;

		OpPollComcatStart payload = new OpPollComcatStart();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_POLL_COMCAT_START;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post poll Comcat start result: " + result);

		return;
	}




	// Test #49 - Post a task to stop polling Comcat.

	public static void test49(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test49' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_POLL;

		OpPollComcatStop payload = new OpPollComcatStop();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_POLL_COMCAT_STOP;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post poll Comcat stop result: " + result);

		return;
	}




	// Test #50 - Post an analyst intervention task.

	public static void test50(String[] args) {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test50' subcommand");
			return;
		}

		String event_id = args[1];
		int state_change = Integer.parseInt(args[2]);
		boolean f_create_timeline = Boolean.parseBoolean (args[3]);

		// Set up the payload

		OpAnalystIntervene payload = new OpAnalystIntervene();
		payload.setup (state_change, f_create_timeline);

		// Post the task

		int opcode = TaskDispatcher.OPCODE_ANALYST_INTERVENE;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post analyst intervention result: " + result);

		return;
	}




	// Test 51 - Search the task queue for execution time and/or event id; and get first matching task.

	public static void test51(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test51' or 'task_query_first' subcommand");
			return;
		}

		long exec_time_lo = Long.parseLong(args[1]);
		long exec_time_hi = Long.parseLong(args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the task

			PendingTask task = PendingTask.get_first_task_entry (exec_time_lo, exec_time_hi, event_id);

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

		}

		return;
	}




	// Test #52 - Search the catalog snapshots for end time and/or event id; and refetch.

	public static void test52(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test52' or 'catsnap_query_refetch' subcommand");
			return;
		}

		double end_time_lo_days = Double.parseDouble (args[1]);
		double end_time_hi_days = Double.parseDouble (args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		long end_time_lo = Math.round(end_time_lo_days * 86400000L);
		long end_time_hi = Math.round(end_time_hi_days * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching catalog snapshots

			List<CatalogSnapshot> entries = CatalogSnapshot.get_catalog_snapshot_range (end_time_lo, end_time_hi, event_id);

			// Display them

			for (CatalogSnapshot entry : entries) {
				System.out.println (entry.toString());
				CatalogSnapshot entry_out = CatalogSnapshot.get_catalog_shapshot_for_key (entry.get_record_key());
				System.out.println (entry_out.toString());
			}

		}

		return;
	}




	// Test #53 - Search the catalog snapshots for end time and/or event id; and display entire catalog.

	public static void test53(String[] args) {

		// Two or three additional arguments

		if (args.length != 3 && args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test53' or 'catsnap_query_verbose' subcommand");
			return;
		}

		double end_time_lo_days = Double.parseDouble (args[1]);
		double end_time_hi_days = Double.parseDouble (args[2]);
		String event_id = null;
		if (args.length == 4) {
			event_id = args[3];
		}

		long end_time_lo = Math.round(end_time_lo_days * 86400000L);
		long end_time_hi = Math.round(end_time_hi_days * 86400000L);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching catalog snapshots

			List<CatalogSnapshot> entries = CatalogSnapshot.get_catalog_snapshot_range (end_time_lo, end_time_hi, event_id);

			// Display them

			for (CatalogSnapshot entry : entries) {
				System.out.println (entry.toString());
				CompactEqkRupList rups = entry.get_rupture_list();
				int n = rups.get_eqk_count();
				long[] lat_lon_depth_list = rups.get_lat_lon_depth_list();
				long[] mag_time_list = rups.get_mag_time_list();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "  " + lat_lon_depth_list[k] + "  " + mag_time_list[k]);
				}
			}

		}

		return;
	}




	// Test #54 - Read a file and write it to standard output.

	public static void test54(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test54' or 'dump_file' subcommand");
			return;
		}

		String filename = args[1];

		// Read the file

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){
			for (String line = file_reader.readLine(); line != null; line = file_reader.readLine()) {
				System.out.println (line);
			}
		}

		return;
	}




	// Test #55 - Read all the files in a directory (not recursive) and write each to standard output.

	public static void test55(String[] args) throws Exception {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test55' or 'dump_files_in_dir' subcommand");
			return;
		}

		String dirname = args[1];

		// List all the files in the directory

		File dir = new File (dirname);
		File[] dir_list = dir.listFiles();

		// Get the list of base filenames, and sort them

		List<String> base_names = new ArrayList<String>();
		for (File file : dir_list) {
			if (file.isFile()) {
				base_names.add (file.getName());
			}
		}
		base_names.sort (null);

		// Loop over files

		for (String base_name : base_names) {

			// Write the filename

			System.out.println (base_name + ":");

			// Read the file

			try (
				BufferedReader file_reader = new BufferedReader (new FileReader (new File (dirname, base_name)));
			){
				for (String line = file_reader.readLine(); line != null; line = file_reader.readLine()) {
					System.out.println (line);
				}
			}

			// Final blank line

			System.out.println ();
		}

		return;
	}




	// Test #56 - Add a few elements to the task pending queue, with connection option and wait time.

	public static void test56(String[] args) {

		// Two additional arguments

		if (args.length != 3) {
			System.err.println ("ServerTest : Invalid 'test56' or 'conopt_task_add_some' subcommand");
			return;
		}

		int conopt = Integer.parseInt (args[1]);
		int waitsec = Integer.parseInt (args[2]);

		// Enable tracing

		MongoDBConnect.set_trace_conn (true);
		MongoDBContent.set_trace_conn (true);
		MongoDBContent.set_trace_session (true);
		MongoDBContent.set_trace_transact (true);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil (conopt, MongoDBUtil.DDBOPT_SAVE_SET, null);
		){
		
			String event_id;
			long sched_time;
			long submit_time;
			String submit_id;
			int opcode;
			int stage;
			MarshalWriter details;
		
			event_id = "Event_2";
			sched_time = 20100L;
			submit_time = 20000L;
			submit_id = "Submitter_2";
			opcode = 102;
			stage = 2;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_2");
			details.marshalLong (null, 21010L);
			details.marshalLong (null, 21020L);
			details.marshalDouble (null, 21030.0);
			details.marshalDouble (null, 21040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_4";
			sched_time = 40100L;
			submit_time = 40000L;
			submit_id = "Submitter_4_no_details";
			opcode = 104;
			stage = 4;
			details = null;
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_1";
			sched_time = 10100L;
			submit_time = 10000L;
			submit_id = "Submitter_1";
			opcode = 101;
			stage = 1;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_1");
			details.marshalLong (null, 11010L);
			details.marshalLong (null, 11020L);
			details.marshalDouble (null, 11030.0);
			details.marshalDouble (null, 11040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_5";
			sched_time = 50100L;
			submit_time = 50000L;
			submit_id = "Submitter_5";
			opcode = 105;
			stage = 5;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_5");
			details.marshalLong (null, 51010L);
			details.marshalLong (null, 51020L);
			details.marshalDouble (null, 51030.0);
			details.marshalDouble (null, 51040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);
		
			event_id = "Event_3";
			sched_time = 30100L;
			submit_time = 30000L;
			submit_id = "Submitter_3";
			opcode = 103;
			stage = 3;
			details = PendingTask.begin_details();
			details.marshalArrayBegin (null, 5);
			details.marshalString (null, "Details_3");
			details.marshalLong (null, 31010L);
			details.marshalLong (null, 31020L);
			details.marshalDouble (null, 31030.0);
			details.marshalDouble (null, 31040.0);
			details.marshalArrayEnd ();
			PendingTask.submit_task (event_id, sched_time, submit_time,
				submit_id, opcode, stage, details);

			// Display modified task queue

			System.out.println ("Modified task queue:");

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task2 : tasks) {
				System.out.println (task2.toString());
			}

			if (waitsec > 0) {
				long wait_delay = ((long)waitsec) * 1000L;
				try {
					Thread.sleep (wait_delay);
				} catch (InterruptedException e) {
				}

				// Display list again at end of wait

				System.out.println ("End-of-wait task queue:");
				List<PendingTask> tasks3 = PendingTask.get_all_tasks();
				for (PendingTask task3 : tasks3) {
					System.out.println (task3.toString());
				}
			}

		}

		return;
	}




	// Test #57 - Display the pending task queue, sorted by execution time, with connection option and wait time.

	public static void test57(String[] args) {

		// Two additional arguments

		if (args.length != 3) {
			System.err.println ("ServerTest : Invalid 'test57' or 'conopt_task_display_list' subcommand");
			return;
		}

		int conopt = Integer.parseInt (args[1]);
		int waitsec = Integer.parseInt (args[2]);

		// Enable tracing

		MongoDBConnect.set_trace_conn (true);
		MongoDBContent.set_trace_conn (true);
		MongoDBContent.set_trace_session (true);
		MongoDBContent.set_trace_transact (true);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil (conopt, MongoDBUtil.DDBOPT_SAVE_SET, null);
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
			}

			if (waitsec > 0) {
				long wait_delay = ((long)waitsec) * 1000L;
				try {
					Thread.sleep (wait_delay);
				} catch (InterruptedException e) {
				}

				// Display list again at end of wait

				System.out.println ("End-of-wait task queue:");
				List<PendingTask> tasks3 = PendingTask.get_all_tasks();
				for (PendingTask task3 : tasks3) {
					System.out.println (task3.toString());
				}
			}

		}

		return;
	}




	// Test #58 - Activate the first document before the cutoff time, and display the retrieved document, with connection option and wait time.

	public static void test58(String[] args) {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test58' or 'conopt_task_cutoff_activate' subcommand");
			return;
		}

		int conopt = Integer.parseInt (args[1]);
		int waitsec = Integer.parseInt (args[2]);
		long cutoff_time = Long.parseLong(args[3]);

		// Enable tracing

		MongoDBConnect.set_trace_conn (true);
		MongoDBContent.set_trace_conn (true);
		MongoDBContent.set_trace_session (true);
		MongoDBContent.set_trace_transact (true);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil (conopt, MongoDBUtil.DDBOPT_SAVE_SET, null);
		){

			// Get the task

			PendingTask task = PendingTask.activate_first_ready_task (cutoff_time);

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

			// Display modified task queue

			System.out.println ("Modified task queue:");

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task2 : tasks) {
				System.out.println (task2.toString());
			}

			if (waitsec > 0) {
				long wait_delay = ((long)waitsec) * 1000L;
				try {
					Thread.sleep (wait_delay);
				} catch (InterruptedException e) {
				}

				// Display list again at end of wait

				System.out.println ("End-of-wait task queue:");
				List<PendingTask> tasks3 = PendingTask.get_all_tasks();
				for (PendingTask task3 : tasks3) {
					System.out.println (task3.toString());
				}
			}

		}

		return;
	}




	// Test #59 - Activate the first document before the cutoff time, and delete it, with connection option and wait time.

	public static void test59(String[] args) {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test59' or 'conopt_task_cutoff_activate_delete' subcommand");
			return;
		}

		int conopt = Integer.parseInt (args[1]);
		int waitsec = Integer.parseInt (args[2]);
		long cutoff_time = Long.parseLong(args[3]);

		// Enable tracing

		MongoDBConnect.set_trace_conn (true);
		MongoDBContent.set_trace_conn (true);
		MongoDBContent.set_trace_session (true);
		MongoDBContent.set_trace_transact (true);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil (conopt, MongoDBUtil.DDBOPT_SAVE_SET, null);
		){

			// Get the task

			PendingTask task = PendingTask.activate_first_ready_task (cutoff_time);

			// Delete it

			if (task != null) {
				PendingTask.delete_task (task);
			}

			// Display it

			if (task == null) {
				System.out.println ("null");
			} else {
				System.out.println (task.toString());
			}

			// Display modified task queue

			System.out.println ("Modified task queue:");

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task2 : tasks) {
				System.out.println (task2.toString());
			}

			if (waitsec > 0) {
				long wait_delay = ((long)waitsec) * 1000L;
				try {
					Thread.sleep (wait_delay);
				} catch (InterruptedException e) {
				}

				// Display list again at end of wait

				System.out.println ("End-of-wait task queue:");
				List<PendingTask> tasks3 = PendingTask.get_all_tasks();
				for (PendingTask task3 : tasks3) {
					System.out.println (task3.toString());
				}
			}

		}

		return;
	}




	// Test #60 - Display the pending task queue, sorted by execution time, with default connection option, and tracing.
	// This test is intended to check the setting of the default connection option.

	public static void test60(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test60' or 'traced_task_display_list' subcommand");
			return;
		}

		// Enable tracing

		MongoDBConnect.set_trace_conn (true);
		MongoDBContent.set_trace_conn (true);
		MongoDBContent.set_trace_session (true);
		MongoDBContent.set_trace_transact (true);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of pending tasks

			List<PendingTask> tasks = PendingTask.get_all_tasks();

			// Display them

			for (PendingTask task : tasks) {
				System.out.println (task.toString());
			}

			// Display parameter

			System.out.println ("test_conopt = " + TestMode.get_test_conopt());

		}

		return;
	}




	// Test #61 - Drop all database collections, allowing a fresh start.

	public static void test61(String[] args) throws Exception {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test61' or 'drop_all_collections' subcommand");
			return;
		}

		if (!( args[1].equals ("drop")
			&& args[2].equals ("all")
			&& args[3].equals ("collections") )) {
			System.err.println ("ServerTest : Wrong confirmation for 'test61' or 'drop_all_collections' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			
			System.out.println ("ServerTest : Dropping all database collections");
		
			System.out.println ("ServerTest : Dropping task collection");

			PendingTask.drop_collection ();
		
			System.out.println ("ServerTest : Dropping log collection");

			LogEntry.drop_collection ();
		
			System.out.println ("ServerTest : Dropping catalog snapshot collection");

			CatalogSnapshot.drop_collection ();
		
			System.out.println ("ServerTest : Dropping timeline collection");

			TimelineEntry.drop_collection ();
		
			System.out.println ("ServerTest : Dropping alias family collection");

			AliasFamily.drop_collection ();
		
			System.out.println ("ServerTest : Dropping relay item collection");

			RelayItem.drop_collection ();
			
			System.out.println ("ServerTest : Dropped all database collections");

		}

		return;
	}




	// Test #62 - Drop all database indexes, allowing indexes to be rebuilt.

	public static void test62(String[] args) throws Exception {

		// Three additional arguments

		if (args.length != 4) {
			System.err.println ("ServerTest : Invalid 'test62' or 'drop_all_indexes' subcommand");
			return;
		}

		if (!( args[1].equals ("drop")
			&& args[2].equals ("all")
			&& args[3].equals ("indexes") )) {
			System.err.println ("ServerTest : Wrong confirmation for 'test62' or 'drop_all_indexes' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			
			System.out.println ("ServerTest : Dropping all database indexes");
		
			System.out.println ("ServerTest : Dropping task indexes");

			PendingTask.drop_indexes ();
		
			System.out.println ("ServerTest : Dropping log indexes");

			LogEntry.drop_indexes ();
		
			System.out.println ("ServerTest : Dropping catalog snapshot indexes");

			CatalogSnapshot.drop_indexes ();
		
			System.out.println ("ServerTest : Dropping timeline indexes");

			TimelineEntry.drop_indexes ();
		
			System.out.println ("ServerTest : Dropping alias family indexes");

			AliasFamily.drop_indexes ();
		
			System.out.println ("ServerTest : Dropping relay item indexes");

			RelayItem.drop_indexes ();
			
			System.out.println ("ServerTest : Dropped all database indexes");

		}

		return;
	}




	// Test #63 - Add a few elements to the relay items.

	public static void test63(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test63' or 'relit_add_some' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a change stream iterator

			try (
				RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
			){

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}
		
				String relay_id;
				long relay_time;
				MarshalWriter details;
				boolean f_force = false;
				long relay_stamp;

				RelayItem relit;
		
				relay_id = "Event_2";
				relay_time = 20100L;
				relay_stamp = 0L;
				details = RelayItem.begin_details();
				details.marshalArrayBegin (null, 5);
				details.marshalString (null, "Details_2");
				details.marshalLong (null, 21010L);
				details.marshalLong (null, 21020L);
				details.marshalDouble (null, 21030.0);
				details.marshalDouble (null, 21040.0);
				details.marshalArrayEnd ();
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}
		
				relay_id = "Event_4";
				relay_time = 40100L;
				relay_stamp = -1L;
				details = null;
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}
		
				relay_id = "Event_1";
				relay_time = 10100L;
				relay_stamp = 1L;
				details = RelayItem.begin_details();
				details.marshalArrayBegin (null, 5);
				details.marshalString (null, "Details_1");
				details.marshalLong (null, 11010L);
				details.marshalLong (null, 11020L);
				details.marshalDouble (null, 11030.0);
				details.marshalDouble (null, 11040.0);
				details.marshalArrayEnd ();
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}
		
				relay_id = "Event_5";
				relay_time = 50100L;
				relay_stamp = -2L;
				details = RelayItem.begin_details();
				details.marshalArrayBegin (null, 5);
				details.marshalString (null, "Details_5");
				details.marshalLong (null, 51010L);
				details.marshalLong (null, 51020L);
				details.marshalDouble (null, 51030.0);
				details.marshalDouble (null, 51040.0);
				details.marshalArrayEnd ();
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}
		
				relay_id = "Event_3";
				relay_time = 30100L;
				relay_stamp = 2L;
				details = RelayItem.begin_details();
				details.marshalArrayBegin (null, 5);
				details.marshalString (null, "Details_3");
				details.marshalLong (null, 31010L);
				details.marshalLong (null, 31020L);
				details.marshalDouble (null, 31030.0);
				details.marshalDouble (null, 31040.0);
				details.marshalArrayEnd ();
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}

				// Wait 3 seconds, then dump the change stream iterator

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				System.out.println ("Begin change stream iterator");

				while (csit.hasNext()) {
					RelayItem csrelit = csit.next();
					System.out.println (csrelit.dumpString());
				}

				System.out.println ("End change stream iterator");

			}

		}

		return;
	}




	// Test #64 - Search the relay items for relay time and/or relay id; using list.

	public static void test64(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test64' or 'relit_query_list' subcommand");
			return;
		}

		boolean f_descending = Boolean.parseBoolean (args[1]);
		long relay_time_lo = Long.parseLong(args[2]);
		long relay_time_hi = Long.parseLong(args[3]);
		//String[] relay_id = null;
		String[] relay_id = new String[0];
		if (args.length >= 5) {
			relay_id = Arrays.copyOfRange (args, 4, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching relay items

			List<RelayItem> items = RelayItem.get_relay_item_range (f_descending ? RelayItem.DESCENDING : RelayItem.ASCENDING , relay_time_lo, relay_time_hi, relay_id);

			// Display them

			for (RelayItem relit : items) {
				System.out.println (relit.dumpString());
			}

		}

		return;
	}




	// Test #65 - Search the relay items for relay time and/or relay id; using iterator.

	public static void test65(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test65' or 'relit_query_iterate' subcommand");
			return;
		}

		boolean f_descending = Boolean.parseBoolean (args[1]);
		long relay_time_lo = Long.parseLong(args[2]);
		long relay_time_hi = Long.parseLong(args[3]);
		String[] relay_id = null;
		//String[] relay_id = new String[0];
		if (args.length >= 5) {
			relay_id = Arrays.copyOfRange (args, 4, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){
			try (

				// Get an iterator over matching relay items

				RecordIterator<RelayItem> items = RelayItem.fetch_relay_item_range (f_descending ? RelayItem.DESCENDING : RelayItem.ASCENDING, relay_time_lo, relay_time_hi, relay_id);
			){

				// Display them

				for (RelayItem relit : items) {
					System.out.println (relit.dumpString());
				}
			}

		}

		return;
	}




	// Test #66 - Search the relay items for relay time and/or relay id; using first.

	public static void test66(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test66' or 'relit_query_first' subcommand");
			return;
		}

		boolean f_descending = Boolean.parseBoolean (args[1]);
		long relay_time_lo = Long.parseLong(args[2]);
		long relay_time_hi = Long.parseLong(args[3]);
		//String[] relay_id = null;
		String[] relay_id = new String[0];
		if (args.length >= 5) {
			relay_id = Arrays.copyOfRange (args, 4, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching relay items

			RelayItem relit = RelayItem.get_first_relay_item (f_descending ? RelayItem.DESCENDING : RelayItem.ASCENDING, relay_time_lo, relay_time_hi, relay_id);

			// Display it

			if (relit == null) {
				System.out.println ("null");
			} else {
				System.out.println (relit.dumpString());
			}

		}

		return;
	}




	// Test #67 - Search the relay items for relay time and/or relay id; and re-fetch the items.

	public static void test67(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test67' or 'relit_query_refetch' subcommand");
			return;
		}

		boolean f_descending = Boolean.parseBoolean (args[1]);
		long relay_time_lo = Long.parseLong(args[2]);
		long relay_time_hi = Long.parseLong(args[3]);
		//String[] relay_id = null;
		String[] relay_id = new String[0];
		if (args.length >= 5) {
			relay_id = Arrays.copyOfRange (args, 4, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get the list of matching relay items

			List<RelayItem> items = RelayItem.get_relay_item_range (f_descending ? RelayItem.DESCENDING : RelayItem.ASCENDING, relay_time_lo, relay_time_hi, relay_id);

			// Display them, and re-fetch

			for (RelayItem relit : items) {
				System.out.println (relit.dumpString());
				RelayItem refetch = RelayItem.get_relay_item_for_key (relit.get_record_key());
				System.out.println (refetch.dumpString());
			}

		}

		return;
	}




	// Test #68 - Search the relay items for relay time and/or relay id; and delete the items.

	public static void test68(String[] args) {

		// Three or more additional arguments

		if (args.length < 4) {
			System.err.println ("ServerTest : Invalid 'test68' or 'relit_query_delete' subcommand");
			return;
		}

		boolean f_descending = Boolean.parseBoolean (args[1]);
		long relay_time_lo = Long.parseLong(args[2]);
		long relay_time_hi = Long.parseLong(args[3]);
		//String[] relay_id = null;
		String[] relay_id = new String[0];
		if (args.length >= 5) {
			relay_id = Arrays.copyOfRange (args, 4, args.length);
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a change stream iterator

			try (
				RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
			){

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Get the list of matching relay items

				List<RelayItem> items = RelayItem.get_relay_item_range (f_descending ? RelayItem.DESCENDING : RelayItem.ASCENDING, relay_time_lo, relay_time_hi, relay_id);

				// Display them, and delete

				for (RelayItem relit : items) {
					System.out.println (relit.dumpString());
					RelayItem.delete_relay_item (relit);
				}

				// Wait 3 seconds, then dump the change stream iterator

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				System.out.println ("Begin change stream iterator");

				while (csit.hasNext()) {
					RelayItem csrelit = csit.next();
					System.out.println (csrelit.dumpString());
				}

				System.out.println ("End change stream iterator");

			}

		}

		return;
	}




	// Test #69 - Add an element to the relay items.

	public static void test69(String[] args) {

		// Five additional arguments

		if (args.length != 6) {
			System.err.println ("ServerTest : Invalid 'test69' or 'relit_add_one' subcommand");
			return;
		}

		String relay_id = args[1];
		long relay_time = Long.parseLong(args[2]);
		String details_text = args[3];
		boolean f_force = Boolean.parseBoolean (args[4]);
		long relay_stamp = Long.parseLong(args[5]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a change stream iterator

			try (
				RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
			){

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				RelayItem relit;
		
				MarshalWriter details = RelayItem.begin_details();
				details.marshalArrayBegin (null, 1);
				details.marshalString (null, details_text);
				details.marshalArrayEnd ();
				relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
				if (relit != null) {
					System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
				} else {
					System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
				}

				// Wait 3 seconds, then dump the change stream iterator

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				System.out.println ("Begin change stream iterator");

				while (csit.hasNext()) {
					RelayItem csrelit = csit.next();
					System.out.println (csrelit.dumpString());
				}

				System.out.println ("End change stream iterator");

			}

		}

		return;
	}




	// Test #70 - Add elements to the relay items in multiple cycles.

	public static void test70(String[] args) {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test70' or 'relit_add_cycles' subcommand");
			return;
		}

		long n_cycles = Long.parseLong(args[1]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a change stream iterator

			try (
				RecordIterator<RelayItem> csit = RelayItem.watch_relay_item_changes();
			){

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Loop over cycles, and items within a cycle, adding 5 new items each cycle

				for (long cycle = 1L; cycle <= n_cycles; ++cycle) {
					for (long item = 1L; item <= cycle * 5L; ++item) {
					
						RelayItem relit;
						String relay_id = "item_" + item;
						long relay_time = cycle * 1000000L + item;
						boolean f_force = false;
						long relay_stamp = (cycle * 1000000L + item) * (((cycle + item) % 3L == 1L) ? -10L : 10L);
		
						MarshalWriter details = RelayItem.begin_details();
						details.marshalArrayBegin (null, 1);
						details.marshalString (null, "Cycle = " + cycle + ", item = " + item);
						details.marshalArrayEnd ();
						relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
						if (relit != null) {
							System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
						} else {
							System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
						}

					}

					// Wait 3 seconds, then dump the change stream iterator

					try {
						Thread.sleep (3000L);
					} catch (InterruptedException e) {
					}

					System.out.println ("Begin change stream iterator, cycle = " + cycle);

					while (csit.hasNext()) {
						RelayItem csrelit = csit.next();
						System.out.println (csrelit.dumpString());
					}

					System.out.println ("End change stream iterator, cycle = " + cycle);
				}

			}

		}

		return;
	}




	// Test #71 - Add elements to the relay items in multiple cycles, using relay thread.

	public static void test71(String[] args) throws IOException {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test71' or 'relit_thread_add_cycles' subcommand");
			return;
		}

		long n_cycles = Long.parseLong(args[1]);

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a relay thread

			RelayThread relay_thread = new RelayThread();
			relay_thread.set_ri_polling_interval (1000L);

			try (
				RelayThread.Sentinel rtsent = relay_thread.make_sentinel();
			){
				if (!( relay_thread.start_relay_thread ("", false) )) {
					System.out.println ("Relay thread failed to start");
					return;
				}

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Loop over cycles, and items within a cycle, adding 5 new items each cycle

				for (long cycle = 1L; cycle <= n_cycles; ++cycle) {
					for (long item = 1L; item <= cycle * 5L; ++item) {
					
						RelayItem relit;
						String relay_id = "item_" + item;
						long relay_time = cycle * 1000000L + item;
						boolean f_force = false;
						long relay_stamp = (cycle * 1000000L + item) * (((cycle + item) % 3L == 1L) ? -10L : 10L);
		
						MarshalWriter details = RelayItem.begin_details();
						details.marshalArrayBegin (null, 1);
						details.marshalString (null, "Cycle = " + cycle + ", item = " + item);
						details.marshalArrayEnd ();
						relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
						if (relit != null) {
							System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
						} else {
							System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
						}

					}

					// Wait 3 seconds, then dump the relay item queue

					try {
						Thread.sleep (3000L);
					} catch (InterruptedException e) {
					}

					System.out.println ("Begin relay thread item queue, cycle = " + cycle);

					for (;;) {
						RelayItem rtrelit = relay_thread.ri_queue_remove();
						if (rtrelit == null) {
							break;
						}
						System.out.println (rtrelit.dumpString());
					}

					System.out.println ("End relay thread item queue, cycle = " + cycle);

					System.out.println ("Begin relay thread fetch operation, cycle = " + cycle);

					List<RelayItem> fi_list = relay_thread.run_fetch_and_sort (0L, 0L);

					for (RelayItem fi_relit : fi_list) {
						System.out.println (fi_relit.dumpString());
					}

					System.out.println ("End relay thread fetch operation, cycle = " + cycle);
				}

			}

		}

		return;
	}




	// Test #72 - Dump all the relay items, using relay thread.

	public static void test72(String[] args) throws IOException {

		// No additional argument

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test72' or 'relit_thread_dump' subcommand");
			return;
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a relay thread

			RelayThread relay_thread = new RelayThread();
			relay_thread.set_ri_polling_interval (1000L);

			try (
				RelayThread.Sentinel rtsent = relay_thread.make_sentinel();
			){
				if (!( relay_thread.start_relay_thread ("", false) )) {
					System.out.println ("Relay thread failed to start");
					return;
				}

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Dump the relay item queue

				System.out.println ("Begin relay thread item queue");

				for (;;) {
					RelayItem rtrelit = relay_thread.ri_queue_remove();
					if (rtrelit == null) {
						break;
					}
					System.out.println (rtrelit.dumpString());
				}

				System.out.println ("End relay thread item queue");

				System.out.println ("Begin relay thread fetch operation");

				List<RelayItem> fi_list = relay_thread.run_fetch_and_sort (0L, 0L);

				int item_count = relay_thread.get_ri_fetch_item_count();
				System.out.println ("Fetch operation item count = " + item_count);

				for (RelayItem fi_relit : fi_list) {
					System.out.println (fi_relit.dumpString());
				}

				System.out.println ("End relay thread fetch operation");

			}

		}

		return;
	}




	// Test #73 - Add elements to the relay items in multiple cycles, testing thread query and change stream.

	public static void test73(String[] args) throws IOException {

		// One additional argument

		if (args.length != 2) {
			System.err.println ("ServerTest : Invalid 'test73' or 'relit_thread_add_multi' subcommand");
			return;
		}

		long n_cycles = Long.parseLong(args[1]);

		// Get the remote database handle for this server

		ServerConfig server_config = new ServerConfig();
		String remote_db_handle = server_config.get_server_db_handles().get (server_config.get_server_number());

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Set up a relay thread

			RelayThread relay_thread = new RelayThread();
			relay_thread.set_ri_polling_interval (500L);			// 0.5 seconds

			try (
				RelayThread.Sentinel rtsent = relay_thread.make_sentinel();
			){
				if (!( relay_thread.start_relay_thread (remote_db_handle, false) )) {
					System.out.println ("Relay thread failed to start");
					return;
				}

				// Wait 3 seconds to make sure it's in effect

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Loop over cycles, and items within a cycle, adding 3 new items each cycle

				for (long cycle = 1L; cycle <= n_cycles; ++cycle) {

					// Every 4 cycles, stop and restart the relay thread

					if (cycle % 4L == 0L) {

						relay_thread.shutdown_relay_thread();

						try {
							Thread.sleep (3000L);
						} catch (InterruptedException e) {
						}

						if (!( relay_thread.start_relay_thread (remote_db_handle, false) )) {
							System.out.println ("Relay thread failed to start, cycle = " + cycle);
							return;
						}

						try {
							Thread.sleep (3000L);
						} catch (InterruptedException e) {
						}
					}

					// Write the relay items for this cycle

					for (long item = cycle * 2L - 1L; item <= cycle * 5L; ++item) {
					
						RelayItem relit;
						String relay_id = "item_" + item;
						long relay_time = cycle * 1000000L + item;
						boolean f_force = false;
						long relay_stamp = (cycle * 1000000L + item) * (((cycle + item) % 3L == 1L) ? -10L : 10L);
		
						MarshalWriter details = RelayItem.begin_details();
						details.marshalArrayBegin (null, 1);
						details.marshalString (null, "Cycle = " + cycle + ", item = " + item);
						details.marshalArrayEnd ();
						relit = RelayItem.submit_relay_item (relay_id, relay_time, details, f_force, relay_stamp);
						if (relit != null) {
							System.out.println ("Added relay item, relay_id = " + relit.get_relay_id() + ", relay_time = " + relit.get_relay_time() + ", relay_stamp = " + relit.get_relay_stamp());
						} else {
							System.out.println ("Failed to add relay item, relay_id = " + relay_id + ", relay_time = " + relay_time + ", relay_stamp = " + relay_stamp);
						}

					}

					// Wait 3 seconds, then dump the relay item queue

					try {
						Thread.sleep (3000L);
					} catch (InterruptedException e) {
					}

					System.out.println ("Begin relay thread item queue, cycle = " + cycle);

					for (;;) {
						RelayItem rtrelit = relay_thread.ri_queue_remove();
						if (rtrelit == null) {
							break;
						}
						System.out.println (rtrelit.dumpString());
					}

					System.out.println ("End relay thread item queue, cycle = " + cycle);

					System.out.println ("Begin relay thread fetch operation, cycle = " + cycle);

					long relay_time_lo = ((cycle < 3L) ? 0L : ((cycle - 2L) * 1000000L));
					List<RelayItem> fi_list = relay_thread.run_fetch_and_sort (relay_time_lo , 0L);

					for (RelayItem fi_relit : fi_list) {
						System.out.println (fi_relit.dumpString());
					}

					System.out.println ("End relay thread fetch operation, cycle = " + cycle);
				}

			}

		}

		return;
	}




	// Test #74 - Use relay support to submit a pdl completion relay item.

	public static void test74(String[] args) throws Exception {

		// 7 additional arguments

		if (args.length != 8) {
			System.err.println ("ServerTest : Invalid 'test74' or 'rsup_pdl_submit' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		String event_id = args[2];
		long relay_time = Long.parseLong (args[3]);
		boolean f_force = Boolean.parseBoolean (args[4]);
		int ripdl_action = Integer.parseInt (args[5]);
		long ripdl_forecast_lag = Long.parseLong (args[6]);
		long ripdl_update_time = Long.parseLong (args[7]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			RelayItem relit = sg.relay_sup.submit_pdl_relay_item (event_id, relay_time, f_force,
				ripdl_action, new ForecastStamp (ripdl_forecast_lag), ripdl_update_time);

			// Display result

			if (relit == null) {
				System.out.println ("submit_pdl_relay_item returned null");
			} else {
				System.out.println (relit.dumpString());

				// Unmarshal the payload and display it

				RiPDLCompletion payload = new RiPDLCompletion();
				payload.unmarshal_relay (relit);
				System.out.println ("RiPDLCompletion");
				System.out.println ("\tripdl_action = " + payload.ripdl_action + " (" + payload.get_ripdl_action_as_string() + ")");
				System.out.println ("\tripdl_forecast_stamp = " + payload.get_ripdl_forecast_stamp_as_string());
				System.out.println ("\tripdl_update_time = " + payload.get_ripdl_update_time_as_string());
			}
		}

		return;
	}




	// Test #75 - Use relay support to get pdl completion relay items.

	public static void test75(String[] args) throws Exception {

		// 1 or more additional arguments

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test75' or 'rsup_pdl_get' subcommand");
			return;
		}

		String[] event_ids = Arrays.copyOfRange (args, 1, args.length);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			List<RelayItem> relits = sg.relay_sup.get_pdl_relay_items (event_ids);

			// Display result

			if (relits.isEmpty()) {
				System.out.println ("get_pdl_relay_items returned an empty list");
			} else {
				for (RelayItem relit : relits) {
					System.out.println (relit.dumpString());

					// Unmarshal the payload and display it

					RiPDLCompletion payload = new RiPDLCompletion();
					payload.unmarshal_relay (relit);
					System.out.println ("RiPDLCompletion");
					System.out.println ("\tripdl_action = " + payload.ripdl_action + " (" + payload.get_ripdl_action_as_string() + ")");
					System.out.println ("\tripdl_forecast_stamp = " + payload.get_ripdl_forecast_stamp_as_string());
					System.out.println ("\tripdl_update_time = " + payload.get_ripdl_update_time_as_string());
				}
			}
		}

		return;
	}




	// Test #76 - Use pdl support to delete OAF products.

	public static void test76(String[] args) throws Exception {

		// 5 or 6 additional arguments

		if (args.length != 6 && args.length != 7) {
			System.err.println ("ServerTest : Invalid 'test76' or 'psup_delete_oaf' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		String event_id = args[2];
		int riprem_reason = Integer.parseInt (args[3]);
		long riprem_forecast_lag = Long.parseLong (args[4]);
		int pdl_enable = Integer.parseInt (args[5]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		String pdl_key_filename = null;
		if (args.length >= 7) {
			pdl_key_filename = args[6];
		}

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		//  // Direct operation to PDL-Development
		//  
		//  ServerConfig server_config = new ServerConfig();
		//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		if (pdl_key_filename != null) {

			if (!( (new File (pdl_key_filename)).canRead() )) {
				System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
				return;
			}

			server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Fetch event

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (event_id);
			System.out.println (fcmain.toString());

			// Call pdl support

			sg.pdl_sup.delete_oaf_products (fcmain, riprem_reason, new ForecastStamp (riprem_forecast_lag));
		}

		return;
	}




	// Test #77 - Use relay support to submit a pdl removal relay item.

	public static void test77(String[] args) throws Exception {

		// 7 additional arguments

		if (args.length != 8) {
			System.err.println ("ServerTest : Invalid 'test77' or 'rsup_prem_submit' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		String event_id = args[2];
		long relay_time = Long.parseLong (args[3]);
		boolean f_force = Boolean.parseBoolean (args[4]);
		int riprem_reason = Integer.parseInt (args[5]);
		long riprem_forecast_lag = Long.parseLong (args[6]);
		long riprem_remove_time = Long.parseLong (args[7]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			RelayItem relit = sg.relay_sup.submit_prem_relay_item (event_id, relay_time, f_force,
				riprem_reason, new ForecastStamp (riprem_forecast_lag), riprem_remove_time);

			// Display result

			if (relit == null) {
				System.out.println ("submit_prem_relay_item returned null");
			} else {
				System.out.println (relit.dumpString());

				// Unmarshal the payload and display it

				RiPDLRemoval payload = new RiPDLRemoval();
				payload.unmarshal_relay (relit);
				System.out.println ("RiPDLRemoval");
				System.out.println ("\triprem_reason = " + payload.riprem_reason + " (" + payload.get_riprem_reason_as_string() + ")");
				System.out.println ("\triprem_forecast_stamp = " + payload.get_riprem_forecast_stamp_as_string());
				System.out.println ("\triprem_remove_time = " + payload.get_riprem_remove_time_as_string());
			}
		}

		return;
	}




	// Test #78 - Use relay support to get pdl removal relay items.

	public static void test78(String[] args) throws Exception {

		// 1 or more additional arguments

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test78' or 'rsup_prem_get' subcommand");
			return;
		}

		String[] event_ids = Arrays.copyOfRange (args, 1, args.length);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			List<RelayItem> relits = sg.relay_sup.get_prem_relay_items (event_ids);

			// Display result

			if (relits.isEmpty()) {
				System.out.println ("get_prem_relay_items returned an empty list");
			} else {
				for (RelayItem relit : relits) {
					System.out.println (relit.dumpString());

					// Unmarshal the payload and display it

					RiPDLRemoval payload = new RiPDLRemoval();
					payload.unmarshal_relay (relit);
					System.out.println ("RiPDLRemoval");
					System.out.println ("\triprem_reason = " + payload.riprem_reason + " (" + payload.get_riprem_reason_as_string() + ")");
					System.out.println ("\triprem_forecast_stamp = " + payload.get_riprem_forecast_stamp_as_string());
					System.out.println ("\triprem_remove_time = " + payload.get_riprem_remove_time_as_string());
				}
			}
		}

		return;
	}




	// Test #79 - Use relay support to get pdl completion, removal, and foreign relay items.

	public static void test79(String[] args) throws Exception {

		// 1 or more additional arguments

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test79 or 'rsup_pdl_prem_get' subcommand");
			return;
		}

		String[] event_ids = Arrays.copyOfRange (args, 1, args.length);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			List<RelayItem> relits = sg.relay_sup.get_pdl_prem_pfrn_relay_items (event_ids);

			// Display result

			if (relits.isEmpty()) {
				System.out.println ("get_pdl_prem_pfrn_relay_items returned an empty list");
			} else {
				for (RelayItem relit : relits) {
					System.out.println (relit.dumpString());

					// Unmarshal the payload and display it

					switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

					case RelaySupport.RITYPE_PDL_COMPLETION:
					{
						RiPDLCompletion payload = new RiPDLCompletion();
						payload.unmarshal_relay (relit);
						System.out.println ("RiPDLCompletion");
						System.out.println ("\tripdl_action = " + payload.ripdl_action + " (" + payload.get_ripdl_action_as_string() + ")");
						System.out.println ("\tripdl_forecast_stamp = " + payload.get_ripdl_forecast_stamp_as_string());
						System.out.println ("\tripdl_update_time = " + payload.get_ripdl_update_time_as_string());
					}
					break;

					case RelaySupport.RITYPE_PDL_REMOVAL:
					{
						RiPDLRemoval payload = new RiPDLRemoval();
						payload.unmarshal_relay (relit);
						System.out.println ("RiPDLRemoval");
						System.out.println ("\triprem_reason = " + payload.riprem_reason + " (" + payload.get_riprem_reason_as_string() + ")");
						System.out.println ("\triprem_forecast_stamp = " + payload.get_riprem_forecast_stamp_as_string());
						System.out.println ("\triprem_remove_time = " + payload.get_riprem_remove_time_as_string());
					}
					break;

					case RelaySupport.RITYPE_PDL_FOREIGN:
					{
						RiPDLForeign payload = new RiPDLForeign();
						payload.unmarshal_relay (relit);
						System.out.println ("RiPDLForeign");
						System.out.println ("\tripfrn_status = " + payload.ripfrn_status + " (" + payload.get_ripfrn_status_as_string() + ")");
						System.out.println ("\tripfrn_detect_time = " + payload.get_ripfrn_detect_time_as_string());
					}
					break;
					}
				}
			}
		}

		return;
	}




	// Test #80 - Use relay support to submit a pdl foreign relay item.

	public static void test80(String[] args) throws Exception {

		// 6 additional arguments

		if (args.length != 7) {
			System.err.println ("ServerTest : Invalid 'test80' or 'rsup_pfrn_submit' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		String event_id = args[2];
		long relay_time = Long.parseLong (args[3]);
		boolean f_force = Boolean.parseBoolean (args[4]);
		int ripfrn_status = Integer.parseInt (args[5]);
		long ripfrn_detect_time = Long.parseLong (args[6]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			RelayItem relit = sg.relay_sup.submit_pfrn_relay_item (event_id, relay_time, f_force,
				ripfrn_status, ripfrn_detect_time);

			// Display result

			if (relit == null) {
				System.out.println ("submit_pfrn_relay_item returned null");
			} else {
				System.out.println (relit.dumpString());

				// Unmarshal the payload and display it

				RiPDLForeign payload = new RiPDLForeign();
				payload.unmarshal_relay (relit);
				System.out.println ("RiPDLForeign");
				System.out.println ("\tripfrn_status = " + payload.ripfrn_status + " (" + payload.get_ripfrn_status_as_string() + ")");
				System.out.println ("\tripfrn_detect_time = " + payload.get_ripfrn_detect_time_as_string());
			}
		}

		return;
	}




	// Test #81 - Use relay support to get pdl foreign relay items.

	public static void test81(String[] args) throws Exception {

		// 1 or more additional arguments

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test81' or 'rsup_pfrn_get' subcommand");
			return;
		}

		String[] event_ids = Arrays.copyOfRange (args, 1, args.length);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			List<RelayItem> relits = sg.relay_sup.get_pfrn_relay_items (event_ids);

			// Display result

			if (relits.isEmpty()) {
				System.out.println ("get_pfrn_relay_items returned an empty list");
			} else {
				for (RelayItem relit : relits) {
					System.out.println (relit.dumpString());

					// Unmarshal the payload and display it

					RiPDLForeign payload = new RiPDLForeign();
					payload.unmarshal_relay (relit);
					System.out.println ("RiPDLForeign");
					System.out.println ("\tripfrn_status = " + payload.ripfrn_status + " (" + payload.get_ripfrn_status_as_string() + ")");
					System.out.println ("\tripfrn_detect_time = " + payload.get_ripfrn_detect_time_as_string());
				}
			}
		}

		return;
	}




	// Test #82 - Use cleanup support to find events needing cleanup.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

	public static void test82(String[] args) throws Exception {

		// 6 additional arguments

		if (args.length != 7) {
			System.err.println ("ServerTest : Invalid 'test82' or 'cleanup_query' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		int pdl_enable = Integer.parseInt (args[2]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		long time_now = SimpleUtils.string_to_time (args[3]);
		long startTime = SimpleUtils.string_to_time (args[4]);
		long endTime = SimpleUtils.string_to_time (args[5]);
		double minMag = Double.parseDouble (args[6]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Find events

			ArrayDeque<String> coll = new ArrayDeque<String>();
			int[] count = sg.cleanup_sup.find_events_needing_cleanup (coll, time_now, startTime, endTime, minMag);

			// Display results

			System.out.println ("Events needing cleanup = " + count[0]);
			System.out.println ("Events with OAF products = " + count[1]);
			System.out.println ("Size of list returned = " + coll.size());

			for (int n = 0; n < 100 && coll.size() > 0; ++n) {
				System.out.println (coll.remove());
			}

			if (coll.size() > 0) {
				System.out.println ("Plus " + coll.size() + " more");
			}
		}

		return;
	}




	// Test #83 - Use cleanup support to clean up an event.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

	public static void test83(String[] args) throws Exception {

		// 4 additional arguments

		if (args.length != 5) {
			System.err.println ("ServerTest : Invalid 'test83' or 'cleanup_event' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		int pdl_enable = Integer.parseInt (args[2]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		long time_now = SimpleUtils.string_to_time (args[3]);
		String event_id = args[4];

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Cleanup event

			long doop = sg.cleanup_sup.cleanup_event (event_id, time_now);

			// Display results

			System.out.println ("cleanup_event returned " + doop);
			System.out.println ("Friendly form = " + PDLCodeChooserOaf.get_doop_as_string (doop));
		}

		return;
	}




	// Test #84 - Post a task to start PDL cleanup.

	public static void test84(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test84' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_CLEANUP;

		OpCleanupPDLStart payload = new OpCleanupPDLStart();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_CLEANUP_PDL_START;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post PDL cleanup start result: " + result);

		return;
	}




	// Test #85 - Post a task to stop PDL cleanup.

	public static void test85(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test85' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_CLEANUP;

		OpCleanupPDLStop payload = new OpCleanupPDLStop();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_CLEANUP_PDL_STOP;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post PDL cleanup stop result: " + result);

		return;
	}




	// Test #86 - Run the idle time cleanup process.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

	public static void test86(String[] args) throws Exception {

		// 6 additional arguments

		if (args.length != 7) {
			System.err.println ("ServerTest : Invalid 'test86' or 'cleanup_run_idle' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		int pdl_enable = Integer.parseInt (args[2]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		long startTime = SimpleUtils.string_to_time (args[3]);
		long endTime = SimpleUtils.string_to_time (args[4]);
		double minMag = Double.parseDouble (args[5]);
		int max_calls = Integer.parseInt (args[6]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		long time_now = ServerClock.get_time();

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		// Set the action parameters to scan the selected time range and magnitude, with 0.1 second gaps between operations

		ActionConfig action_config = new ActionConfig();

		action_config.get_action_config_file().removal_event_gap = 100L;
		action_config.get_action_config_file().removal_lookback_tmax = time_now - startTime;
		action_config.get_action_config_file().removal_lookback_tmin = time_now - endTime;
		action_config.get_action_config_file().removal_lookback_mag = minMag;

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Enable cleanup

			sg.cleanup_sup.set_cleanup_enabled();

			// Execute calls until nothing to do

			for (int n = 1; n <= max_calls; ++n) {

				// Wait 1 second for timers to run

				try {
					Thread.sleep (1000L);
				} catch (InterruptedException e) {
				}

				// Set up task context (necessary to update the time)

				dispatcher.setup_task_context();

				// Execute the call

				System.out.println ("Running idle time call " + n);
				boolean did_work = sg.cleanup_sup.run_cleanup_during_idle (true);

				// Stop if no work was done

				if (!( did_work )) {
					System.out.println ("No work done on call " + n);
					break;
				}
			}
		}

		return;
	}




	// Test #87 - Execute the next task, with support for PDL and logging.

	public static void test87(String[] args) throws Exception {

		// 4 or 5 additional arguments

		if (args.length != 5 && args.length != 6) {
			System.err.println ("ServerTest : Invalid 'test87' or 'exec_next_task' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		int pdl_enable = Integer.parseInt (args[2]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		int pdl_mode = Integer.parseInt (args[3]);	// 0 = normal, 1 = force primary, 2 = force secondary
		boolean f_adjust_time = Boolean.parseBoolean (args[4]);
		String pdl_key_filename = null;
		if (args.length >= 6) {
			pdl_key_filename = args[5];
		}

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		if (pdl_mode < 0 || pdl_mode > 2) {
			System.out.println ("Invalid pdl_mode = " + pdl_mode);
			return;
		}

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		if (pdl_key_filename != null) {

			if (!( (new File (pdl_key_filename)).canRead() )) {
				System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
				return;
			}

			server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Open the summary log file

		try (
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set the PDL mode

			sg.pdl_sup.set_force_primary (pdl_mode);

			// Run one task

			boolean f_verbose = true;

			dispatcher.run_next_task (f_verbose, f_adjust_time);
		}

		return;
	}




	// Test #88 - Use timeline support to change the PDL mode.

	public static void test88(String[] args) throws Exception {

		// 2 additional arguments

		if (args.length != 3) {
			System.err.println ("ServerTest : Invalid 'test88' or 'change_pdl_mode' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		boolean f_primary = Boolean.parseBoolean (args[2]);	// true = change to primary, false = change to secondary

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Call timeline support

			int n_cancel;

			if (f_primary) {
				System.out.println ("Changing PDL mode to primary");
				n_cancel = sg.timeline_sup.set_timeline_to_primary();
			} else {
				System.out.println ("Changing PDL mode to secondary");
				n_cancel = sg.timeline_sup.set_timeline_to_secondary();
			}
				
			System.out.println ("Number of tasks canceled = " + n_cancel);
		}

		return;
	}




	// Test #89 - Run the relay link.

	public static void test89(String[] args) throws Exception {

		// 2 additional arguments

		if (args.length != 3) {
			System.err.println ("ServerTest : Invalid 'test89' or 'run_relay_link' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		boolean f_prist = Boolean.parseBoolean (args[2]);	// true = run primary state change, false = not

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Get a task dispatcher and server group

		TaskDispatcher dispatcher = new TaskDispatcher();
		ServerGroup sg = dispatcher.get_server_group();

		// Connect to MongoDB

		try (
			RelayLink.LinkSentinel rl_sentinel = sg.relay_link.make_link_sentinel();
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Initialize relay link

			sg.relay_link.init_relay_link();

			// Display status

			String loc_stat = sg.relay_link.get_local_status_summary();
			String rem_stat = sg.relay_link.get_remote_status_summary();

			System.out.println (loc_stat);
			System.out.println (rem_stat);

			// Loop until shutdown request

			while (!( TaskDispatcher.test_check_for_shutdown() )) {

				// Wait 3 seconds

				try {
					Thread.sleep (3000L);
				} catch (InterruptedException e) {
				}

				// Set up task context

				dispatcher.setup_task_context();

				// Poll the relay link

				if (f_prist) {
					sg.relay_link.poll_relay_link();
				} else {
					sg.relay_link.poll_relay_link_no_prist();
				}

				// Display status if it has changed

				String new_loc_stat = sg.relay_link.get_local_status_summary();
				String new_rem_stat = sg.relay_link.get_remote_status_summary();

				if (!( new_loc_stat.equals(loc_stat) && new_rem_stat.equals(rem_stat) )) {

					loc_stat = new_loc_stat;
					rem_stat = new_rem_stat;

					System.out.println (loc_stat);
					System.out.println (rem_stat);
				}
			}

			// Set up task context

			dispatcher.setup_task_context();

			// Shut down relay link

			sg.relay_link.shutdown_relay_link();

			// Display status

			loc_stat = sg.relay_link.get_local_status_summary();
			rem_stat = sg.relay_link.get_remote_status_summary();

			System.out.println (loc_stat);
			System.out.println (rem_stat);
		}

		return;
	}




	// Test #90 - Use relay support to submit an analyst selection relay item.

	public static void test90(String[] args) throws Exception {

		// 8 additional arguments

		if (args.length != 9) {
			System.err.println ("ServerTest : Invalid 'test90' or 'rsup_ansel_submit' subcommand");
			return;
		}

		String logfile = args[1];		// can be "-" for none
		String event_id = args[2];
		long relay_time = Long.parseLong (args[3]);
		long relay_stamp = Long.parseLong (args[4]);
		boolean f_force = Boolean.parseBoolean (args[5]);
		int state_change = Integer.parseInt (args[6]);
		boolean f_create_timeline = Boolean.parseBoolean (args[7]);
		long analyst_time = Long.parseLong (args[8]);

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		AnalystOptions analyst_options = null;
		if (analyst_time >= 0L) {
			analyst_options = new AnalystOptions();
			analyst_options.analyst_time = analyst_time;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			RelayItem relit = sg.relay_sup.submit_ansel_relay_item (event_id, relay_time, relay_stamp, f_force,
				state_change, f_create_timeline, analyst_options);

			// Display result

			if (relit == null) {
				System.out.println ("submit_ansel_relay_item returned null");
			} else {
				System.out.println (relit.dumpString());

				// Unmarshal the payload and display it

				RiAnalystSelection payload = new RiAnalystSelection();
				payload.unmarshal_relay (relit);
				System.out.println ("RiAnalystSelection");
				System.out.println ("\tstate_change = " + payload.state_change + " (" + payload.get_state_change_as_string() + ")");
				System.out.println ("\tf_create_timeline = " + payload.f_create_timeline);
				System.out.println ("\tanalyst_time = " + payload.get_analyst_time_as_string());
			}
		}

		return;
	}




	// Test #91 - Use relay support to get analyst selection relay items.

	public static void test91(String[] args) throws Exception {

		// 1 or more additional arguments

		if (args.length < 2) {
			System.err.println ("ServerTest : Invalid 'test91' or 'rsup_ansel_get' subcommand");
			return;
		}

		String[] event_ids = Arrays.copyOfRange (args, 1, args.length);

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Connect to MongoDB

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil();
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Set up task context

			dispatcher.setup_task_context();

			// Call relay support

			System.out.println ("Retrieving list of items");

			List<RelayItem> relits = sg.relay_sup.get_ansel_relay_items (event_ids);

			// Display result

			if (relits.isEmpty()) {
				System.out.println ("get_ansel_relay_items returned an empty list");
			} else {
				for (RelayItem relit : relits) {
					System.out.println (relit.dumpString());

					// Unmarshal the payload and display it

					RiAnalystSelection payload = new RiAnalystSelection();
					payload.unmarshal_relay (relit);
					System.out.println ("RiAnalystSelection");
					System.out.println ("\tstate_change = " + payload.state_change + " (" + payload.get_state_change_as_string() + ")");
					System.out.println ("\tf_create_timeline = " + payload.f_create_timeline);
					System.out.println ("\tanalyst_time = " + payload.get_analyst_time_as_string());
				}
			}

			// Call relay support

			System.out.println ("Retrieving most recent");

			RelayItem first_relit = sg.relay_sup.get_ansel_first_relay_item (event_ids);

			// Display result

			if (first_relit == null) {
				System.out.println ("get_ansel_first_relay_item returned null");
			} else {
				System.out.println (first_relit.dumpString());

				// Unmarshal the payload and display it

				RiAnalystSelection payload = new RiAnalystSelection();
				payload.unmarshal_relay (first_relit);
				System.out.println ("RiAnalystSelection");
				System.out.println ("\tstate_change = " + payload.state_change + " (" + payload.get_state_change_as_string() + ")");
				System.out.println ("\tf_create_timeline = " + payload.f_create_timeline);
				System.out.println ("\tanalyst_time = " + payload.get_analyst_time_as_string());
			}
		}

		return;
	}




	// Test #92 - Execute multiple tasks, with support for PDL and logging, until the given cutoff time.

	public static void test92(String[] args) throws Exception {

		// 5 or 6 additional arguments

		if (args.length != 6 && args.length != 7) {
			System.err.println ("ServerTest : Invalid 'test92' or 'exec_tasks_until' subcommand");
			return;
		}

		long cutoff_time = SimpleUtils.string_or_number_or_now_to_time (args[1]);		// ISO-8601 time, or number of milliseconds since epoch, or "now"
		String logfile = args[2];		// can be "-" for none
		int pdl_enable = Integer.parseInt (args[3]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		int pdl_mode = Integer.parseInt (args[4]);	// 0 = normal, 1 = force primary, 2 = force secondary
		boolean f_force_first = Boolean.parseBoolean (args[5]);
		String pdl_key_filename = null;
		if (args.length >= 7) {
			pdl_key_filename = args[6];
		}

		String my_logfile = null;
		if (!( logfile.equals ("-") )) {
			my_logfile = "'" + logfile + "'";		// makes this literal, so time is not substituted
		}

		if (pdl_mode < 0 || pdl_mode > 2) {
			System.out.println ("Invalid pdl_mode = " + pdl_mode);
			return;
		}

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		if (pdl_key_filename != null) {

			if (!( (new File (pdl_key_filename)).canRead() )) {
				System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
				return;
			}

			server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
		}

		// Turn off excessive log messages

		MongoDBLogControl.disable_excessive();

		// Open the summary log file

		try (
			TimeSplitOutputStream sum_tsop = TimeSplitOutputStream.make_tsop (my_logfile, 0L);
		){

			// Get a task dispatcher and server group

			TaskDispatcher dispatcher = new TaskDispatcher();
			ServerGroup sg = dispatcher.get_server_group();

			// Install the log file

			dispatcher.set_summary_log_tsop (sum_tsop);

			// Set the PDL mode

			sg.pdl_sup.set_force_primary (pdl_mode);

			// Run the first task if requested

			boolean f_verbose = true;
			boolean f_adjust_time = true;

			boolean f_running = true;

			if (f_force_first) {
				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				f_running = dispatcher.run_next_task (f_verbose, f_adjust_time);
			}

			// Process special cutoff time values

			if (cutoff_time == -1L) {
				cutoff_time = ServerComponent.EXEC_TIME_MAX_PROMPT;
			} 
			else if (cutoff_time == -2L) {
				cutoff_time = ServerClock.get_time();
			}
			else if (cutoff_time == -3L) {
				cutoff_time = ServerComponent.EXEC_TIME_FAR_FUTURE;
			}

			// Run tasks until cutoff time is reached

			while (f_running) {
				System.out.println (ServerComponent.LOG_SEPARATOR_LINE);
				f_running = dispatcher.run_next_task (f_verbose, f_adjust_time, cutoff_time);
			}
		}

		return;
	}




	// Test #93 - Post a task to reset health monitoring.

	public static void test93(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test93' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_HEALTH;

		OpHealthMonitorReset payload = new OpHealthMonitorReset();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_HEALTH_MON_RESET;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post health monitor reset result: " + result);

		return;
	}




	// Test #94 - Post a task to start health monitoring.

	public static void test94(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test94' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_HEALTH;

		OpHealthMonitorStart payload = new OpHealthMonitorStart();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_HEALTH_MON_START;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post health monitor start result: " + result);

		return;
	}




	// Test #95 - Post a task to stop health monitoring.

	public static void test95(String[] args) {

		// No additional arguments

		if (args.length != 1) {
			System.err.println ("ServerTest : Invalid 'test95' subcommand");
			return;
		}

		String event_id = ServerComponent.EVID_HEALTH;

		OpHealthMonitorStop payload = new OpHealthMonitorStop();
		payload.setup ();

		// Post the task

		int opcode = TaskDispatcher.OPCODE_HEALTH_MON_STOP;
		int stage = 0;

		long the_time = ServerClock.get_time();

		boolean result = TaskDispatcher.post_task (event_id, the_time, the_time, "ServerTest", opcode, stage, payload.marshal_task());

		// Display result

		System.out.println ("Post health monitor stop result: " + result);

		return;
	}




	// Test #96 - Delete a product from PDL.

	public static void test96(String[] args) throws Exception {

		// 4 or 5 additional arguments

		if (args.length != 5 && args.length != 6) {
			System.err.println ("ServerTest : Invalid 'test96' or 'pdl_x_delete' subcommand");
			return;
		}

		String eventID = args[1];
		String eventNetwork = args[2];
		String eventCode = args[3];
		int pdl_enable = Integer.parseInt (args[4]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
		String pdl_key_filename = null;
		if (args.length >= 6) {
			pdl_key_filename = args[5];
		}

		//  // Direct operation to PDL-Development
		//  
		//  ServerConfig server_config = new ServerConfig();
		//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

		// Set the PDL enable code

		if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
			System.out.println ("Invalid pdl_enable = " + pdl_enable);
			return;
		}

		ServerConfig server_config = new ServerConfig();
		server_config.get_server_config_file().pdl_enable = pdl_enable;

		if (pdl_key_filename != null) {

			if (!( (new File (pdl_key_filename)).canRead() )) {
				System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
				return;
			}

			server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
		}

		// Construct the deletion product

		boolean isReviewed = false;
		long modifiedTime = 0L;
		Product product = PDLProductBuilderOaf.createDeletionProduct (eventID, eventNetwork, eventCode, isReviewed, modifiedTime);

		// Send to PDL

		PDLSender.signProduct(product);
		PDLSender.sendProduct(product, true);

		return;
	}




	// Test dispatcher.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ServerTest : Missing subcommand");
			return;
		}

		// Establish test mode connection option

		int my_conopt = TestMode.get_test_conopt();
		if (my_conopt >= 0) {
			MongoDBUtil.set_def_conopt (my_conopt);
		}

		// Establish app or test mode time

		//  long my_app_time = TestMode.get_app_time();
		//  if (my_app_time > 0L) {
		//  	ServerClock.freeze_time (my_app_time);
		//  }

		// Subcommand : Test #1
		// Command format:
		//  test1
		// Add a few items to the pending task queue.

		if (args[0].equalsIgnoreCase ("test1") || args[0].equalsIgnoreCase ("task_add_some")) {

			try {
				test1(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #2
		// Command format:
		//  test2
		// Display the pending task queue, unsorted.

		if (args[0].equalsIgnoreCase ("test2") || args[0].equalsIgnoreCase ("task_display_unsorted")) {

			try {
				test2(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #3
		// Command format:
		//  test3
		// Display the pending task queue, sorted by execution time.

		if (args[0].equalsIgnoreCase ("test3") || args[0].equalsIgnoreCase ("task_display_list")) {

			try {
				test3(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #4
		// Command format:
		//  test4
		// Display the first task in the pending task queue, according to execution time.

		if (args[0].equalsIgnoreCase ("test4") || args[0].equalsIgnoreCase ("task_display_first")) {

			try {
				test4(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #5
		// Command format:
		//  test5  cutoff_time
		// Display the first task in the pending task queue, before cutoff time, according to execution time.

		if (args[0].equalsIgnoreCase ("test5") || args[0].equalsIgnoreCase ("task_cutoff_first")) {

			try {
				test5(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #6
		// Command format:
		//  test6  cutoff_time
		// Activate the first document before the cutoff time, and display the retrieved document.

		if (args[0].equalsIgnoreCase ("test6") || args[0].equalsIgnoreCase ("task_cutoff_activate")) {

			try {
				test6(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #7
		// Command format:
		//  test7  cutoff_time  exec_time  stage  [event_id]
		// Activate the first document before the cutoff time, and stage it.

		if (args[0].equalsIgnoreCase ("test7") || args[0].equalsIgnoreCase ("task_cutoff_activate_stage")) {

			try {
				test7(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #8
		// Command format:
		//  test6  cutoff_time
		// Activate the first document before the cutoff time, and delete it.

		if (args[0].equalsIgnoreCase ("test8") || args[0].equalsIgnoreCase ("task_cutoff_activate_delete")) {

			try {
				test8(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #9
		// Command format:
		//  test9
		// Run task dispatcher.

		if (args[0].equalsIgnoreCase ("test9") || args[0].equalsIgnoreCase ("run_dispatcher")) {

			try {
				test9(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #10
		// Command format:
		//  test10
		// Post a shutdown task.

		if (args[0].equalsIgnoreCase ("test10") || args[0].equalsIgnoreCase ("post_shutdown")) {

			try {
				test10(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #11
		// Command format:
		//  test11
		// Scan the pending task queue, sorted, and write a log entry for each.

		if (args[0].equalsIgnoreCase ("test11") || args[0].equalsIgnoreCase ("log_add_from_tasks")) {

			try {
				test11(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #12
		// Command format:
		//  test12
		// Scan the pending task queue, sorted, and search the log for each.

		if (args[0].equalsIgnoreCase ("test12") || args[0].equalsIgnoreCase ("log_search_for_tasks")) {

			try {
				test12(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #13
		// Command format:
		//  test13  log_time_lo  log_time_hi  [event_id]
		// Search the log for log time and/or event id.
		// Log times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test13") || args[0].equalsIgnoreCase ("log_query_list")) {

			try {
				test13(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #14
		// Command format:
		//  test14  log_time_lo  log_time_hi  [event_id]
		// Search the log for log time and/or event id, and delete the matching entries.
		// Log times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test14") || args[0].equalsIgnoreCase ("log_query_list_delete")) {

			try {
				test14(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #15
		// Command format:
		//  test15  event_id  opcode  stage  [details]
		// Post a task with given event id, opcode, stage, and details.
		// Event id can be "-" for an empty string.

		if (args[0].equalsIgnoreCase ("test15") || args[0].equalsIgnoreCase ("post_task")) {

			try {
				test15(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #16
		// Command format:
		//  test16  [f_adjust_time]
		// Execute the next task.
		// If f_adjust_time is "true" then adjust clock to be the execution time of the task.
		// If f_adjust_time is omitted then the default value is "false".

		if (args[0].equalsIgnoreCase ("test16") || args[0].equalsIgnoreCase ("exec_task")) {

			try {
				test16(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #17
		// Command format:
		//  test17  start_time_days  end_time_days  event_id
		// Write a catalog snapshot.

		if (args[0].equalsIgnoreCase ("test17") || args[0].equalsIgnoreCase ("catsnap_add")) {

			try {
				test17(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #18
		// Command format:
		//  test18  end_time_lo_days  end_time_hi_days  [event_id]
		// Search the catalog snapshots for end time and/or event id.
		// Times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test18") || args[0].equalsIgnoreCase ("catsnap_query_list")) {

			try {
				test18(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #19
		// Command format:
		//  test19  end_time_lo_days  end_time_hi_days  [event_id]
		// Search the catalog snapshots for end time and/or event id, and delete the matching entries.
		// Times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test19") || args[0].equalsIgnoreCase ("catsnap_query_delete")) {

			try {
				test19(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #20
		// Command format:
		//  test20
		// Add a few elements to the timeline.

		if (args[0].equalsIgnoreCase ("test20") || args[0].equalsIgnoreCase ("tline_add_some")) {

			try {
				test20(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #21
		// Command format:
		//  test21  action_time_lo  action_time_hi  action_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; using list.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The action_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test21") || args[0].equalsIgnoreCase ("tline_query_list")) {

			try {
				test21(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #22
		// Command format:
		//  test22  action_time_lo  action_time_hi  action_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; using iterator.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The action_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test22") || args[0].equalsIgnoreCase ("tline_query_iterate")) {

			try {
				test22(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #23
		// Command format:
		//  test23  action_time_lo  action_time_hi  action_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; and re-fetch the entries.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The action_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test23") || args[0].equalsIgnoreCase ("tline_query_refetch")) {

			try {
				test23(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #24
		// Command format:
		//  test24  action_time_lo  action_time_hi  action_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; and delete the entries.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The action_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test24") || args[0].equalsIgnoreCase ("tline_query_delete")) {

			try {
				test24(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #25
		// Command format:
		//  test25
		// Display the pending task queue, sorted by execution time, using iterator.

		if (args[0].equalsIgnoreCase ("test25") || args[0].equalsIgnoreCase ("task_display_iterate")) {

			try {
				test25(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #26
		// Command format:
		//  test26  log_time_lo  log_time_hi  [event_id]
		// Search the log for log time and/or event id, using iterator.
		// Log times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test26") || args[0].equalsIgnoreCase ("log_query_iterate")) {

			try {
				test26(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #27
		// Command format:
		//  test27  end_time_lo_days  end_time_hi_days  [event_id]
		// Search the catalog snapshots for end time and/or event id, using iterator.
		// Times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test27") || args[0].equalsIgnoreCase ("catsnap_query_iterate")) {

			try {
				test27(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #28
		// Command format:
		//  test28  stage  message
		// Post a console message task with given stage and message.

		if (args[0].equalsIgnoreCase ("test28")) {

			try {
				test28(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #29
		// Command format:
		//  test29  exec_time_lo  exec_time_hi  [event_id]
		// Search the task queue for execution time and/or event id.
		// Execution times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test29") || args[0].equalsIgnoreCase ("task_query_list")) {

			try {
				test29(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #30
		// Command format:
		//  test30  exec_time_lo  exec_time_hi  [event_id]
		// Search the task queue for execution time and/or event id, using iterator.
		// Execution times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test30") || args[0].equalsIgnoreCase ("task_query_iterate")) {

			try {
				test30(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #31
		// Command format:
		//  test31  action_time_lo  action_time_hi  action_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; get most recent.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The action_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test31") || args[0].equalsIgnoreCase ("tline_query_first")) {

			try {
				test31(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #32
		// Command format:
		//  test32  event_id
		// Post a sync intake command for the given event.

		if (args[0].equalsIgnoreCase ("test32")) {

			try {
				test32(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #33
		// Command format:
		//  test33  arg...
		// Parse a PDL intake command for the given command line.

		if (args[0].equalsIgnoreCase ("test33")) {

			try {
				test33(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #34
		// Command format:
		//  test34  arg...
		// Post a PDL intake command for the given command line.

		if (args[0].equalsIgnoreCase ("test34")) {

			try {
				test34(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #35
		// Command format:
		//  test35
		// Add a few elements to the alias families.

		if (args[0].equalsIgnoreCase ("test35") || args[0].equalsIgnoreCase ("alias_add_some")) {

			try {
				test35(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #36
		// Command format:
		//  test36  family_time_lo  family_time_hi  family_time_div_rem  [event_id]  [comcat_id]...
		// Search the alias families for family time and/or event id and/or comcat id; using list.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The family_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test36") || args[0].equalsIgnoreCase ("alias_query_list")) {

			try {
				test36(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #37
		// Command format:
		//  test37  family_time_lo  family_time_hi  family_time_div_rem  [event_id]  [comcat_id]...
		// Search the alias families for family time and/or event id and/or comcat id; using iterator.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The family_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test37") || args[0].equalsIgnoreCase ("alias_query_iterate")) {

			try {
				test37(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #38
		// Command format:
		//  test38  family_time_lo  family_time_hi  family_time_div_rem  [event_id]  [comcat_id]...
		// Search the alias families for family time and/or event id and/or comcat id; and re-fetch the entries.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The family_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test38") || args[0].equalsIgnoreCase ("alias_query_refetch")) {

			try {
				test38(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #39
		// Command format:
		//  test39  family_time_lo  family_time_hi  family_time_div_rem  [event_id]  [comcat_id]...
		// Search the alias families for family time and/or event id and/or comcat id; and delete the entries.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The family_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test39") || args[0].equalsIgnoreCase ("alias_query_delete")) {

			try {
				test39(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #40
		// Command format:
		//  test40  family_time_lo  family_time_hi  family_time_div_rem  [event_id]  [comcat_id]...
		// Search the timeline for action time and/or event id and/or comcat id; get most recent.
		// Times can be 0 for no bound, event id can be omitted or equal to "-" for no restriction.
		// If any comcat_id are given, the entry must match at least one of them.
		// The family_time_div_rem can be 0 for no modulus test, otherwise it is divisor * 1000 + remainder.

		if (args[0].equalsIgnoreCase ("test40") || args[0].equalsIgnoreCase ("alias_query_first")) {

			try {
				test40(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #41
		// Command format:
		//  test41  eventID  eventNetwork  eventCode
		// Delete a product from PDL-Development.
		// Note this always uses PDL-Development regardless of the ServerConfig setting.

		if (args[0].equalsIgnoreCase ("test41") || args[0].equalsIgnoreCase ("pdl_dev_delete")) {

			try {
				test41(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #41h
		// Command format:
		//  test41h  eventID  eventNetwork  eventCode
		// Delete a product from PDL-Development.
		// Note this always uses PDL-Development regardless of the ServerConfig setting.
		// Same as test #41 except installs a health monitor and displays the result.

		if (args[0].equalsIgnoreCase ("test41h") || args[0].equalsIgnoreCase ("pdl_dev_delete_h")) {

			PDLSender.set_pdl_health_monitor (new SimpleHealthCounter());

			try {
				test41(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println ();
			System.out.println (PDLSender.get_pdl_health_monitor().toString());

			return;
		}

		// Subcommand : Test #42
		// Command format:
		//  test42  filename
		// Read an alias list from a file, and store it in the database.

		if (args[0].equalsIgnoreCase ("test42") || args[0].equalsIgnoreCase ("alias_add_from_file")) {

			try {
				test42(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #43
		// Command format:
		//  test43  timeline_id
		// Get current alias information for a timeline.

		if (args[0].equalsIgnoreCase ("test43") || args[0].equalsIgnoreCase ("alias_get_timeline_info")) {

			try {
				test43(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #44
		// Command format:
		//  test44  event_id
		// Get current alias information for an event.

		if (args[0].equalsIgnoreCase ("test44") || args[0].equalsIgnoreCase ("alias_get_event_info")) {

			try {
				test44(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #45
		// Command format:
		//  test45  event_id
		// Get current alias information for an event, create timeline if new event.

		if (args[0].equalsIgnoreCase ("test45") || args[0].equalsIgnoreCase ("alias_create_timeline_for_event")) {

			try {
				test45(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #46
		// Command format:
		//  test46  [dirname]
		// Test console redirection and time split output streams.
		// If dirname is given, it must not end in / and the test files are written there.
		// If dirname is not given, the default is logtest/logs.

		if (args[0].equalsIgnoreCase ("test46") || args[0].equalsIgnoreCase ("conred_tsop")) {

			try {
				test46(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #47
		// Command format:
		//  test47 "delete" "all" "tables"
		// Delete all the database tables, allowing a fresh start.

		if (args[0].equalsIgnoreCase ("test47") || args[0].equalsIgnoreCase ("delete_all_tables")) {

			try {
				test47(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #48
		// Command format:
		//  test48
		// Post a task to start polling Comcat.

		if (args[0].equalsIgnoreCase ("test48")) {

			try {
				test48(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #49
		// Command format:
		//  test49
		// Post a task to stop polling Comcat.

		if (args[0].equalsIgnoreCase ("test49")) {

			try {
				test49(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #50
		// Command format:
		//  test50  event_id  state_change  f_create_timeline
		// Post a task for analyst intervention.
		// The event_id can be either a Comcat ID or a timeline ID.
		// The state_change is an integer:
		//  1 = no change, 2 = start timeline, 3 = stop timeline, 4 = withdraw timeline.
		// The f_create_timeline is true to create the timeline if it doesn't exist.

		if (args[0].equalsIgnoreCase ("test50")) {

			try {
				test50(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #51
		// Command format:
		//  test51  exec_time_lo  exec_time_hi  [event_id]
		// Search the task queue for execution time and/or event id; and get first matching task.
		// Execution times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test51") || args[0].equalsIgnoreCase ("task_query_first")) {

			try {
				test51(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #52
		// Command format:
		//  test52  end_time_lo_days  end_time_hi_days  [event_id]
		// Search the catalog snapshots for end time and/or event id; and refetch.
		// Times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test52") || args[0].equalsIgnoreCase ("catsnap_query_refetch")) {

			try {
				test52(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #53
		// Command format:
		//  test53  end_time_lo_days  end_time_hi_days  [event_id]
		// Search the catalog snapshots for end time and/or event id; and display entire catalog.
		// Times can be 0 for no bound, event id can be omitted for no restriction.

		if (args[0].equalsIgnoreCase ("test53") || args[0].equalsIgnoreCase ("catsnap_query_verbose")) {

			try {
				test53(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #54
		// Command format:
		//  test54  filename
		// Read a file and write it to standard output.

		if (args[0].equalsIgnoreCase ("test54") || args[0].equalsIgnoreCase ("dump_file")) {

			try {
				test54(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #55
		// Command format:
		//  test55  dirname
		// Read all the files in a directory (not recursive) and write each to standard output.

		if (args[0].equalsIgnoreCase ("test55") || args[0].equalsIgnoreCase ("dump_files_in_dir")) {

			try {
				test55(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #56
		// Command format:
		//  test56  conopt  waitsec
		// Add a few items to the pending task queue, with connection option and wait time.

		if (args[0].equalsIgnoreCase ("test56") || args[0].equalsIgnoreCase ("conopt_task_add_some")) {

			try {
				test56(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #57
		// Command format:
		//  test57  conopt  waitsec
		// Display the pending task queue, sorted by execution time, with connection option and wait time.

		if (args[0].equalsIgnoreCase ("test57") || args[0].equalsIgnoreCase ("conopt_task_display_list")) {

			try {
				test57(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #58
		// Command format:
		//  test58  conopt  waitsec  cutoff_time
		// Activate the first document before the cutoff time, and display the retrieved document, with connection option and wait time.

		if (args[0].equalsIgnoreCase ("test58") || args[0].equalsIgnoreCase ("conopt_task_cutoff_activate")) {

			try {
				test58(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #59
		// Command format:
		//  test59  conopt  waitsec  cutoff_time
		// Activate the first document before the cutoff time, and delete it, with connection option and wait time.

		if (args[0].equalsIgnoreCase ("test59") || args[0].equalsIgnoreCase ("conopt_task_cutoff_activate_delete")) {

			try {
				test59(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #60
		// Command format:
		//  test60  conopt  waitsec
		// Display the pending task queue, sorted by execution time, with default connection option, and tracing.

		if (args[0].equalsIgnoreCase ("test60") || args[0].equalsIgnoreCase ("traced_task_display_list")) {

			try {
				test60(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #61
		// Command format:
		//  test61 "drop" "all" "collections"
		// Drop all database collections, allowing a fresh start.

		if (args[0].equalsIgnoreCase ("test61") || args[0].equalsIgnoreCase ("drop_all_collections")) {

			try {
				test61(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #62
		// Command format:
		//  test62 "drop" "all" "indexes"
		// Drop all database indexes, allowing indexes to be rebuilt.

		if (args[0].equalsIgnoreCase ("test62") || args[0].equalsIgnoreCase ("drop_all_indexes")) {

			try {
				test62(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #63
		// Command format:
		//  test63
		// Add a few elements to the relay items.

		if (args[0].equalsIgnoreCase ("test63") || args[0].equalsIgnoreCase ("relit_add_some")) {

			try {
				test63(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #64
		// Command format:
		//  test64  f_descending  relay_time_lo  relay_time_hi  [relay_id]...
		// Search the relay items for relay time and/or relay id; using list.
		// Times can be 0 for no bound, relay id can be omitted for no restriction or repeated to search for several.
		// If any relay are given, the entry must match at least one of them.

		if (args[0].equalsIgnoreCase ("test64") || args[0].equalsIgnoreCase ("relit_query_list")) {

			try {
				test64(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #65
		// Command format:
		//  test65  f_descending  relay_time_lo  relay_time_hi  [relay_id]...
		// Search the relay items for relay time and/or relay id; using iterator.
		// Times can be 0 for no bound, relay id can be omitted for no restriction or repeated to search for several.
		// If any relay are given, the entry must match at least one of them.

		if (args[0].equalsIgnoreCase ("test65") || args[0].equalsIgnoreCase ("relit_query_iterate")) {

			try {
				test65(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #66
		// Command format:
		//  test66  f_descending  relay_time_lo  relay_time_hi  [relay_id]...
		// Search the relay items for relay time and/or relay id; using first.
		// Times can be 0 for no bound, relay id can be omitted for no restriction or repeated to search for several.
		// If any relay are given, the entry must match at least one of them.

		if (args[0].equalsIgnoreCase ("test66") || args[0].equalsIgnoreCase ("relit_query_first")) {

			try {
				test66(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #67
		// Command format:
		//  test67  f_descending  relay_time_lo  relay_time_hi  [relay_id]...
		// Search the relay items for relay time and/or relay id; and re-fetch the items.
		// Times can be 0 for no bound, relay id can be omitted for no restriction or repeated to search for several.
		// If any relay are given, the entry must match at least one of them.

		if (args[0].equalsIgnoreCase ("test67") || args[0].equalsIgnoreCase ("relit_query_refetch")) {

			try {
				test67(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #68
		// Command format:
		//  test68  f_descending  relay_time_lo  relay_time_hi  [relay_id]...
		// Search the relay items for relay time and/or relay id; using list.
		// Times can be 0 for no bound, relay id can be omitted for no restriction or repeated to search for several.
		// If any relay are given, the entry must match at least one of them.

		if (args[0].equalsIgnoreCase ("test68") || args[0].equalsIgnoreCase ("relit_query_delete")) {

			try {
				test68(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #69
		// Command format:
		//  test69  relay_id  relay_time  details_text  f_force  relay_stamp
		// Add an element to the relay items.
		// It may or may not succeeded depending on whether an equal or later item already exists.

		if (args[0].equalsIgnoreCase ("test69") || args[0].equalsIgnoreCase ("relit_add_one")) {

			try {
				test69(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #70
		// Command format:
		//  test70  n_cycles
		// Add elements to the relay items in multiple cycles.
		// Each cycle updates all prior items and adds 5 new items, then dumps the change stream iterator.

		if (args[0].equalsIgnoreCase ("test70") || args[0].equalsIgnoreCase ("relit_add_cycles")) {

			try {
				test70(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #71
		// Command format:
		//  test71  n_cycles
		// Add elements to the relay items in multiple cycles, using relay thread.
		// Each cycle updates all prior items and adds 5 new items, then dumps the change stream iterator.

		if (args[0].equalsIgnoreCase ("test71") || args[0].equalsIgnoreCase ("relit_thread_add_cycles")) {

			try {
				test71(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #72
		// Command format:
		//  test72
		// Dump all the relay items, using relay thread.

		if (args[0].equalsIgnoreCase ("test72") || args[0].equalsIgnoreCase ("relit_thread_dump")) {

			try {
				test72(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #73
		// Command format:
		//  test73  n_cycles
		// Add elements to the relay items in multiple cycles, using relay thread.
		// Each cycle updates some prior items and adds 3 new items, then dumps the change stream iterator and relay thread query.
		// Probably 10 cycles is enough.

		if (args[0].equalsIgnoreCase ("test73") || args[0].equalsIgnoreCase ("relit_thread_add_multi")) {

			try {
				test73(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #74
		// Command format:
		//  test74  logfile  event_id  relay_time  f_force  ripdl_action  ripdl_forecast_lag  ripdl_update_time
		// Use relay support to submit a pdl completion relay item.
		// Then display the item that was written.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test74") || args[0].equalsIgnoreCase ("rsup_pdl_submit")) {

			try {
				test74(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #75
		// Command format:
		//  test75  event_id...
		// Use relay support to get pdl completion relay items.
		// Then display the returned items, sorted most recent first.

		if (args[0].equalsIgnoreCase ("test75") || args[0].equalsIgnoreCase ("rsup_pdl_get")) {

			try {
				test75(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #76
		// Command format:
		//  test76  logfile  event_id  riprem_reason  riprem_forecast_lag  pdl_enable  [pdl_key_filename]
		// Use pdl support to delete OAF products.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test76") || args[0].equalsIgnoreCase ("psup_delete_oaf")) {

			try {
				test76(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #77
		// Command format:
		//  test77  logfile  event_id  relay_time  f_force  riprem_reason  riprem_forecast_lag  riprem_remove_time
		// Use relay support to submit a pdl removal relay item.
		// Then display the item that was written.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test77") || args[0].equalsIgnoreCase ("rsup_prem_submit")) {

			try {
				test77(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #78
		// Command format:
		//  test78  event_id...
		// Use relay support to get pdl removal relay items.
		// Then display the returned items, sorted most recent first.

		if (args[0].equalsIgnoreCase ("test78") || args[0].equalsIgnoreCase ("rsup_prem_get")) {

			try {
				test78(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #79
		// Command format:
		//  test79  event_id...
		// Use relay support to get pdl completion, removal, and foreign relay items.
		// Then display the returned items, sorted most recent first.

		if (args[0].equalsIgnoreCase ("test79") || args[0].equalsIgnoreCase ("rsup_pdl_prem_get")) {

			try {
				test79(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #80
		// Command format:
		//  test80  logfile  event_id  relay_time  f_force  ripfrn_status  ripfrn_detect_time
		// Use relay support to submit a pdl foreign relay item.
		// Then display the item that was written.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test80") || args[0].equalsIgnoreCase ("rsup_pfrn_submit")) {

			try {
				test80(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #81
		// Command format:
		//  test81  event_id...
		// Use relay support to get pdl foreign relay items.
		// Then display the returned items, sorted most recent first.

		if (args[0].equalsIgnoreCase ("test81") || args[0].equalsIgnoreCase ("rsup_pfrn_get")) {

			try {
				test81(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #82
		// Command format:
		//  test82  logfile  pdl_enable  time_now  startTime  endTime  minMag
		// Use cleanup support to find events needing cleanup.
		// The logfile can be "-" for none.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test82") || args[0].equalsIgnoreCase ("cleanup_query")) {

			try {
				test82(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #83
		// Command format:
		//  test83  logfile  pdl_enable  time_now  event_id
		// Use cleanup support to clean up an event.
		// The logfile can be "-" for none.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test83") || args[0].equalsIgnoreCase ("cleanup_event")) {

			try {
				test83(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #84
		// Command format:
		//  test84
		// Post a task to start PDL cleanup.

		if (args[0].equalsIgnoreCase ("test84")) {

			try {
				test84(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #85
		// Command format:
		//  test85
		// Post a task to stop PDL cleanup.

		if (args[0].equalsIgnoreCase ("test85")) {

			try {
				test85(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #86
		// Command format:
		//  test86  logfile  pdl_enable  startTime  endTime  minMag  max_calls
		// Run the idle time cleanup process.
		// The logfile can be "-" for none.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test86") || args[0].equalsIgnoreCase ("cleanup_run_idle")) {

			try {
				test86(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #87
		// Command format:
		//  test87  logfile  pdl_enable  pdl_mode  f_adjust_time  [pdl_key_filename]
		// Execute the next task, with support for PDL and logging.
		// The logfile can be "-" for none.
		// The pdl_mode can be: 0 = normal, 1 = force primary, 2 = force secondary.

		if (args[0].equalsIgnoreCase ("test87") || args[0].equalsIgnoreCase ("exec_next_task")) {

			try {
				test87(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #88
		// Command format:
		//  test88  logfile  f_primary
		// Use timeline support to change the PDL mode.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test88") || args[0].equalsIgnoreCase ("change_pdl_mode")) {

			try {
				test88(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #89
		// Command format:
		//  test89  logfile  f_primary
		// Run the relay link.
		// The logfile can be "-" for none.

		if (args[0].equalsIgnoreCase ("test89") || args[0].equalsIgnoreCase ("run_relay_link")) {

			try {
				test89(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #90
		// Command format:
		//  test90  logfile  event_id  relay_time  relay_stamp  f_force  state_change  f_create_timeline  analyst_time
		// Use relay support to submit a pdl completion relay item.
		// Then display the item that was written.
		// The logfile can be "-" for none.
		// If analyst_time < 0L then null analyst_options are used.

		if (args[0].equalsIgnoreCase ("test90") || args[0].equalsIgnoreCase ("rsup_ansel_submit")) {

			try {
				test90(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #91
		// Command format:
		//  test91  event_id...
		// Use relay support to get pdl completion relay items.
		// Then display the returned items, sorted most recent first.

		if (args[0].equalsIgnoreCase ("test91") || args[0].equalsIgnoreCase ("rsup_ansel_get")) {

			try {
				test91(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #92
		// Command format:
		//  test92  cutoff_time  logfile  pdl_enable  pdl_mode  f_force_first  [pdl_key_filename]
		// Execute multiple tasks, with support for PDL and logging, until the given cutoff time.
		// The logfile can be "-" for none.
		// The pdl_enable can be: 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod,
		// 5 = down dev, 6 = down prod (see ServerConfigFile).
		// The pdl_mode can be: 0 = normal, 1 = force primary, 2 = force secondary.
		// If f_force_first is true, the first task is executed even if it is after the cutoff time.
		// The time can be in ISO-8601 format (like 2011-12-03T10:15:30Z), or an integer giving milliseconds
		// since the epoch, or "now" to select the current time (according to System.currentTimeMillis).
		// The time can also be -1 to select the latest prompt execution time, or -2 to select the
		// time according to SystemClock.get_time (after the first task if f_force_first is true),
		// or -3 to select a time far in the future.
		// Note: An application time can be set to select a start time.

		if (args[0].equalsIgnoreCase ("test92") || args[0].equalsIgnoreCase ("exec_tasks_until")) {

			try {
				test92(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #93
		// Command format:
		//  test93
		// Post a task to reset health monitoring.

		if (args[0].equalsIgnoreCase ("test93")) {

			try {
				test93(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #94
		// Command format:
		//  test94
		// Post a task to start health monitoring.

		if (args[0].equalsIgnoreCase ("test94")) {

			try {
				test94(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #95
		// Command format:
		//  test95
		// Post a task to reset health monitoring.

		if (args[0].equalsIgnoreCase ("test95")) {

			try {
				test95(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Subcommand : Test #96
		// Command format:
		//  test96  eventID  eventNetwork  eventCode  pdl_enable  [pdl_key_filename]
		// Delete a product from PDL.

		if (args[0].equalsIgnoreCase ("test96") || args[0].equalsIgnoreCase ("pdl_x_delete")) {

			try {
				test96(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		// Unrecognized subcommand.

		System.err.println ("ServerTest : Unrecognized subcommand : " + args[0]);
		return;
	}
}
