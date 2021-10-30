package org.opensha.oaf.gamma;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import java.io.Closeable;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.AftershockStatsShadow;
import org.opensha.oaf.rj.AftershockVerbose;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.RJ_AftershockModel;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.ConsoleRedirector;
import org.opensha.oaf.util.SphRegionWorld;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionCircle;
import org.opensha.oaf.util.catalog.ObsEqkRupEventIdComparator;
import org.opensha.oaf.util.catalog.ObsEqkRupMaxMagComparator;
import org.opensha.oaf.util.catalog.ObsEqkRupMaxTimeComparator;
import org.opensha.oaf.util.catalog.ObsEqkRupMinMagComparator;
import org.opensha.oaf.util.catalog.ObsEqkRupMinTimeComparator;

import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ServerComponent;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;



/**
 * Gamma statistical test command-line interface.
 * Author: Michael Barall 10/03/2018.
 */
public class GammaCmd {




	// cmd_list_events - List the events to use for the gamma test.
	// Command format:
	//  list_events  log_filename  event_list_filename  start_time  end_time  min_mag  max_mag
	// Get a world-wide catalog, and write a list of event ids to the given output file.
	// Events that are shadowed are excluded.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.

	public static void cmd_list_events(String[] args) {

		// 6 additional arguments

		if (args.length != 7) {
			System.err.println ("GammaCmd : Invalid 'list_events' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				long startTime = SimpleUtils.string_to_time (args[3]);
				long endTime = SimpleUtils.string_to_time (args[4]);
				double min_mag = Double.parseDouble (args[5]);
				double max_mag = Double.parseDouble (args[6]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + min_mag);
				System.out.println ("Maximum magnitude: " + max_mag);
				System.out.println ("");

				// Get configuration

				ActionConfig action_config = new ActionConfig();

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Construct the Region

				SphRegionWorld region = new SphRegionWorld ();

				// Call Comcat

				String null_event_id = null;
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;

				ObsEqkRupList rup_list = accessor.fetchEventList (null_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				// Open the output file

				int events_accepted = 0;
				int events_shadowed = 0;
				int events_tested = 0;

				try (
					Writer writer = new BufferedWriter (new FileWriter (event_list_filename));
				){
					// Loop over ruptures

					for (ObsEqkRupture rup : rup_list) {

						// Silently discard events exceeding maximum magnitude

						if (rup.getMag() >= max_mag) {
							continue;
						}

						++events_tested;

						// Get the event id and time

						String rup_event_id = rup.getEventId();
						long rup_time = rup.getOriginTime();

						System.out.println ("Checking event: " + rup_event_id + " at " + SimpleUtils.time_to_string (rup_time));

						// Get find shadow parameters

						long max_forecast_lag = action_config.get_def_max_forecast_lag();
						long max_window_end_off = action_config.get_max_adv_window_end_off();

						long time_now = rup_time + max_forecast_lag + max_window_end_off;
						double search_radius = action_config.get_shadow_search_radius();
						long search_time_lo = rup_time - action_config.get_shadow_lookback_time();
						long search_time_hi = rup_time + max_forecast_lag + max_window_end_off;
						long centroid_rel_time_lo = 0L;
						long centroid_rel_time_hi = ServerComponent.DURATION_HUGE;
						double centroid_mag_floor = action_config.get_shadow_centroid_mag();
						double large_mag = action_config.get_shadow_large_mag();
						double[] separation = new double[2];

						// Run find_shadow

						ObsEqkRupture shadow;

						shadow = AftershockStatsShadow.find_shadow (rup, time_now,
							search_radius, search_time_lo, search_time_hi,
							centroid_rel_time_lo, centroid_rel_time_hi,
							centroid_mag_floor, large_mag, separation);

						// If we are shadowed ...

						if (shadow != null) {

							// Count it

							++events_shadowed;
						}

						// Otherwise, we are not shadowed ...

						else {

							// Count it

							++events_accepted;

							// Write to file
						
							writer.write (rup_event_id + "\n");
						}
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events retrieved = " + rup_list.size());
				System.out.println ("Events tested = " + events_tested);
				System.out.println ("Events shadowed = " + events_shadowed);
				System.out.println ("Events accepted = " + events_accepted);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_list_events had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_list_events had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_gamma_table - Write the gamma table for a list of earthquakes.
	// Command format:
	//  gamma_table  log_filename  event_list_filename  gamma_table_filename
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_gamma_table(String[] args) {

		// 3 additional arguments

		if (args.length != 4) {
			System.err.println ("GammaCmd : Invalid 'gamma_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Fetch the mainshock info

						ForecastMainshock fcmain = new ForecastMainshock();
						fcmain.setup_mainshock_only (the_event_id);

						// Compute models

						EqkForecastSet eqk_forecast_set = new EqkForecastSet();
						eqk_forecast_set.run_simulations (gamma_config,
							gamma_config.simulation_count, fcmain, false);

						// Add in to total

						total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Compute the gamma table and statistics table

					String gamma_table = total.single_event_gamma_to_string (gamma_config);
					String stats_table = total.compute_count_stats_to_string (gamma_config);

					// Write to file

					writer.write (gamma_table);
					writer.write ("\n");
					writer.write (stats_table);
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_gamma_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_gamma_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_zepi_gamma_table - Write the gamma table for a list of earthquakes,
	// using zero epistemic uncertainty when running simulations.
	// Command format:
	//  gamma_table  log_filename  event_list_filename  gamma_table_filename
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	// Simulations are run with zero epistemic uncertainty.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_zepi_gamma_table(String[] args) {

		// 3 additional arguments

		if (args.length != 4) {
			System.err.println ("GammaCmd : Invalid 'zepi_gamma_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Establish zero epistemic uncertainty

				gamma_config.no_epistemic_uncertainty = true;

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Fetch the mainshock info

						ForecastMainshock fcmain = new ForecastMainshock();
						fcmain.setup_mainshock_only (the_event_id);

						// Compute models

						EqkForecastSet eqk_forecast_set = new EqkForecastSet();
						eqk_forecast_set.run_simulations (gamma_config,
							gamma_config.simulation_count, fcmain, false);

						// Add in to total

						total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Compute the gamma table and statistics table

					String gamma_table = total.single_event_gamma_to_string (gamma_config);
					String stats_table = total.compute_count_stats_to_string (gamma_config);

					// Write to file

					writer.write (gamma_table);
					writer.write ("\n");
					writer.write (stats_table);
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_zepi_gamma_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_zepi_gamma_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_stacked_gui_cat - Write a stacked aftershock catalog in GUI format.
	// Command format:
	//  stacked_gui_cat  log_filename  event_list_filename  gui_cat_filename  the_end_lag  main_mag
	// Read the list of events, create a stacked aftershock catalog, and write it in GUI catalog format.
	// The aftershock sequence extends from 0 to the_end_lag, which is given in java.time.Duration format.
	// The fictitious mainshock is at time 0 (Jan 1, 1970) with the given magnitude.

	public static void cmd_stacked_gui_cat(String[] args) {

		// 5 additional arguments

		if (args.length != 6) {
			System.err.println ("GammaCmd : Invalid 'stacked_gui_cat' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gui_cat_filename = args[3];
				long the_end_lag = SimpleUtils.string_to_duration (args[4]);
				double main_mag = Double.parseDouble (args[5]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("GUI catalog filename: " + gui_cat_filename);
				System.out.println ("Aftershock end lag: " + SimpleUtils.duration_raw_and_string_2 (the_end_lag));
				System.out.println ("Mainshock magnitude: " + main_mag);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Get the stacked aftershock sequence

				long origin_time = 0L;

				List<ObsEqkRupture> stacked_as = GammaUtils.get_stacked_aftershocks (
					event_list_filename, the_end_lag, origin_time, true);

				// Construct a fictitious mainshock

				double main_lat = 0.0;
				double main_lon = 0.0;
				double main_depth = 0.0;
				Location hypoLoc = new Location(main_lat, main_lon, main_depth);
				String eventId = "mainshock";
		
				ObsEqkRupture mainshock = new ObsEqkRupture (eventId, origin_time, hypoLoc, main_mag);

				// Write it in GUI catalog format

				GammaUtils.writeGUICatalogText (gui_cat_filename, mainshock, stacked_as);

				// Display the result

				System.out.println ("");
				System.out.println ("Number of stacked aftershocks = " + stacked_as.size());

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_stacked_gui_cat had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_stacked_gui_cat had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_sim_gamma_table - Write the gamma table for a list of earthquakes, using simulated aftershock sequences.
	// Command format:
	//  sim_gamma_table  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
	// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
	// Each event is repeated num_rep times.
	// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
	// If f_randsum is true, then randomize sum over earthquakes.  (default true)
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_sim_gamma_table(String[] args) {

		// 8 additional arguments

		if (args.length != 9) {
			System.err.println ("GammaCmd : Invalid 'sim_gamma_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];
				double cat_min_mag = Double.parseDouble (args[4]);
				boolean f_epi = Boolean.parseBoolean (args[5]);
				int num_rep = Integer.parseInt (args[6]);
				boolean f_discard = Boolean.parseBoolean (args[7]);
				boolean f_randsum = Boolean.parseBoolean (args[8]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("Minimum magnitude for simulated catalog: " + cat_min_mag);
				System.out.println ("Use epistemic uncertainty for simulation parameters: " + f_epi);
				System.out.println ("Number of repetitions: " + num_rep);
				System.out.println ("Discard simulations with a large aftershock: " + f_discard);
				System.out.println ("Randomize simulation sum over earthquakes: " + f_randsum);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				gamma_config.no_epistemic_uncertainty = !f_epi;
				//gamma_config.sim_start_off = 0L;
				gamma_config.discard_sim_with_large_as = f_discard;

				if (!( f_randsum )) {
					gamma_config.eqk_summation_count = gamma_config.simulation_count;
					gamma_config.eqk_summation_randomize = false;
				}

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Repeat desired number of times

						for (int i_rep = 0; i_rep < num_rep; ++i_rep) {

							if (i_rep > 0) {
								System.out.println ("Processing event " + events_processed + " repetition " + (i_rep + 1) + ": " + the_event_id);
							}

							// Fetch the mainshock info

							ForecastMainshock fcmain = new ForecastMainshock();
							fcmain.setup_mainshock_only (the_event_id);

							// Compute models

							EqkForecastSet eqk_forecast_set = new EqkForecastSet();
							eqk_forecast_set.run_sim_simulations (gamma_config,
								gamma_config.simulation_count, fcmain, cat_min_mag, f_epi, false, false);

							// Add in to total

							total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
						}
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Compute the gamma table and statistics table

					String gamma_table = total.single_event_gamma_to_string (gamma_config);
					String stats_table = total.compute_count_stats_to_string (gamma_config);

					// Write to file

					writer.write (gamma_table);
					writer.write ("\n");
					writer.write (stats_table);
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_sim_gamma_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_sim_gamma_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_seq_gamma_table - Write the gamma table for a single earthquakes, using simulated aftershock sequences.
	// Command format:
	//  seq_gamma_table  log_filename   gamma_table_filename  time  mag  lat  lon  cat_min_mag  f_epi  num_rep  f_discard  f_randsum
	// Create a simulated mainshock with the given time, magnitude, latitude, and longitude.
	// Simulate the event num_rep times, sum over all events, and write the combined tables.
	// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
	// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
	// If f_randsum is true, then randomize sum over earthquakes.  (default true)
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_seq_gamma_table(String[] args) {

		// 11 additional arguments

		if (args.length != 12) {
			System.err.println ("GammaCmd : Invalid 'seq_gamma_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String gamma_table_filename = args[2];
				long time = SimpleUtils.string_to_time (args[3]);
				double mag = Double.parseDouble (args[4]);
				double lat = Double.parseDouble (args[5]);
				double lon = Double.parseDouble (args[6]);
				double cat_min_mag = Double.parseDouble (args[7]);
				boolean f_epi = Boolean.parseBoolean (args[8]);
				int num_rep = Integer.parseInt (args[9]);
				boolean f_discard = Boolean.parseBoolean (args[10]);
				boolean f_randsum = Boolean.parseBoolean (args[11]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("Time: " + SimpleUtils.time_raw_and_string(time));
				System.out.println ("Magnitude: " + mag);
				System.out.println ("Latitude: " + lat);
				System.out.println ("Longitude: " + lon);
				System.out.println ("Minimum magnitude for simulated catalog: " + cat_min_mag);
				System.out.println ("Use epistemic uncertainty for simulation parameters: " + f_epi);
				System.out.println ("Number of repetitions: " + num_rep);
				System.out.println ("Discard simulations with a large aftershock: " + f_discard);
				System.out.println ("Randomize simulation sum over earthquakes: " + f_randsum);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				gamma_config.no_epistemic_uncertainty = !f_epi;
				gamma_config.sim_start_off = 0L;
				gamma_config.discard_sim_with_large_as = f_discard;

				if (!( f_randsum )) {
					gamma_config.eqk_summation_count = gamma_config.simulation_count;
					gamma_config.eqk_summation_randomize = false;
				}

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Loop over repetitions

				int events_processed = 0;

				for (int i_rep = 0; i_rep < num_rep; ++i_rep) {

					// Count it

					++events_processed;

					// Read the event id

					System.out.println ("Processing event " + events_processed);

					// Create the mainshock info

					String network = "simnet";
					String code = "code" + events_processed;
					double depth = 0.0;

					ForecastMainshock fcmain = new ForecastMainshock();
					fcmain.setup_sim_mainshock (network, code, time, mag, lat, lon, depth);

					// Compute models

					EqkForecastSet eqk_forecast_set = new EqkForecastSet();
					eqk_forecast_set.run_sim_simulations (gamma_config,
						gamma_config.simulation_count, fcmain, cat_min_mag, f_epi, false, false);

					// Add in to total

					total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Compute the gamma table and statistics table

					String gamma_table = total.single_event_gamma_to_string (gamma_config);
					String stats_table = total.compute_count_stats_to_string (gamma_config);

					// Write to file

					writer.write (gamma_table);
					writer.write ("\n");
					writer.write (stats_table);
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_seq_gamma_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_seq_gamma_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_zeta_table - Write the zeta table for a list of earthquakes.
	// Command format:
	//  zeta_table  log_filename  event_list_filename  zeta_table_filename  f_data_fmt  f_keep_empty
	// Read the list of events, and for each event compute the cumulative probabilities.
	// Write the results for all earthquakes.
	// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
	// The boolean f_keep_empty is true to write lines that contain no data.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_zeta_table(String[] args) {

		// 5 additional arguments

		if (args.length != 6) {
			System.err.println ("GammaCmd : Invalid 'zeta_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String zeta_table_filename = args[3];
				boolean f_data_fmt = Boolean.parseBoolean (args[4]);
				boolean f_keep_empty = Boolean.parseBoolean (args[5]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + zeta_table_filename);
				System.out.println ("Write data format: " + f_data_fmt);
				System.out.println ("Keep lines with no data: " + f_keep_empty);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Open the input file and output file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
					Writer writer = new BufferedWriter (new FileWriter (zeta_table_filename));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Fetch the mainshock info

						ForecastMainshock fcmain = new ForecastMainshock();
						fcmain.setup_mainshock_only (the_event_id);

						// Compute models

						CumProbEqkSet cum_prob_eqk_set = new CumProbEqkSet();
						cum_prob_eqk_set.run_simulations (gamma_config,
							gamma_config.simulation_count, fcmain, false);

						// Write output

						String lines;
						if (f_data_fmt) {
							lines = cum_prob_eqk_set.single_event_zeta_to_lines (gamma_config, events_processed, f_keep_empty);
						} else {
							lines = cum_prob_eqk_set.single_event_zeta_to_string (gamma_config, f_keep_empty);
						}

						if (!( lines.isEmpty() )) {
							writer.write (lines);
						}
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_zeta_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_zeta_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_sim_zeta_table - Write the gamma table for a list of earthquakes, using simulated aftershock sequences.
	// Command format:
	//  sim_zeta_table  log_filename  event_list_filename  zeta_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_data_fmt  f_keep_empty
	// Read the list of events, and for each event compute the cumulative probabilities.
	// Write the results for all earthquakes.
	// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
	// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
	// Each event is repeated num_rep times.
	// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
	// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
	// The boolean f_keep_empty is true to write lines that contain no data.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_sim_zeta_table(String[] args) {

		// 9 additional arguments

		if (args.length != 10) {
			System.err.println ("GammaCmd : Invalid 'sim_zeta_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String zeta_table_filename = args[3];
				double cat_min_mag = Double.parseDouble (args[4]);
				boolean f_epi = Boolean.parseBoolean (args[5]);
				int num_rep = Integer.parseInt (args[6]);
				boolean f_discard = Boolean.parseBoolean (args[7]);
				boolean f_data_fmt = Boolean.parseBoolean (args[8]);
				boolean f_keep_empty = Boolean.parseBoolean (args[9]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + zeta_table_filename);
				System.out.println ("Minimum magnitude for simulated catalog: " + cat_min_mag);
				System.out.println ("Use epistemic uncertainty for simulation parameters: " + f_epi);
				System.out.println ("Number of repetitions: " + num_rep);
				System.out.println ("Discard simulations with a large aftershock: " + f_discard);
				System.out.println ("Write data format: " + f_data_fmt);
				System.out.println ("Keep lines with no data: " + f_keep_empty);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				gamma_config.no_epistemic_uncertainty = !f_epi;
				//gamma_config.sim_start_off = 0L;
				gamma_config.discard_sim_with_large_as = f_discard;

				//if (!( f_randsum )) {
				//	gamma_config.eqk_summation_count = gamma_config.simulation_count;
				//	gamma_config.eqk_summation_randomize = false;
				//}

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Open the input file and output file

				int events_processed = 0;
				int sims_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
					Writer writer = new BufferedWriter (new FileWriter (zeta_table_filename));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Repeat desired number of times

						for (int i_rep = 0; i_rep < num_rep; ++i_rep) {

							if (i_rep > 0) {
								System.out.println ("Processing event " + events_processed + " repetition " + (i_rep + 1) + ": " + the_event_id);
							}

							// Fetch the mainshock info

							ForecastMainshock fcmain = new ForecastMainshock();
							fcmain.setup_mainshock_only (the_event_id);

							// Compute models

							CumProbEqkSet cum_prob_eqk_set = new CumProbEqkSet();
							cum_prob_eqk_set.run_sim_simulations (gamma_config,
								gamma_config.simulation_count, fcmain, cat_min_mag, f_epi, false, false);

							// Write output

							++sims_processed;

							String lines;
							if (f_data_fmt) {
								lines = cum_prob_eqk_set.single_event_zeta_to_lines (gamma_config, sims_processed, f_keep_empty);
							} else {
								lines = cum_prob_eqk_set.single_event_zeta_to_string (gamma_config, f_keep_empty);
							}

							if (!( lines.isEmpty() )) {
								writer.write (lines);
							}
						}
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_sim_zeta_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_sim_zeta_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_agg_zeta_table - Write the aggregated zeta table for a list of earthquakes.
	// Command format:
	//  agg_zeta_table  log_filename  event_list_filename  gamma_table_filename  f_data_fmt  f_keep_empty
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
	// The boolean f_keep_empty is true to write lines that contain no data.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_agg_zeta_table(String[] args) {

		// 5 additional arguments

		if (args.length != 6) {
			System.err.println ("GammaCmd : Invalid 'agg_zeta_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];
				boolean f_data_fmt = Boolean.parseBoolean (args[4]);
				boolean f_keep_empty = Boolean.parseBoolean (args[5]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("Write data format: " + f_data_fmt);
				System.out.println ("Keep lines with no data: " + f_keep_empty);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Fetch the mainshock info

						ForecastMainshock fcmain = new ForecastMainshock();
						fcmain.setup_mainshock_only (the_event_id);

						// Compute models

						EqkForecastSet eqk_forecast_set = new EqkForecastSet();
						eqk_forecast_set.run_simulations (gamma_config,
							gamma_config.simulation_count, fcmain, false);

						// Add in to total

						total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Write to file

					String lines;
					if (f_data_fmt) {
						lines = total.single_event_zeta_to_lines (gamma_config, 0, f_keep_empty);
					} else {
						lines = total.single_event_zeta_to_string (gamma_config, f_keep_empty);
					}

					if (!( lines.isEmpty() )) {
						writer.write (lines);
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_agg_zeta_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_agg_zeta_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_sim_agg_zeta_table - Write the aggregated zeta table for a list of earthquakes, using simulated aftershock sequences.
	// Command format:
	//  sim_agg_zeta_table  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum  f_data_fmt  f_keep_empty
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
	// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
	// Each event is repeated num_rep times.
	// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
	// If f_randsum is true, then randomize sum over earthquakes.  (default true)
	// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
	// The boolean f_keep_empty is true to write lines that contain no data.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_sim_agg_zeta_table(String[] args) {

		// 10 additional arguments

		if (args.length != 11) {
			System.err.println ("GammaCmd : Invalid 'sim_agg_zeta_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];
				double cat_min_mag = Double.parseDouble (args[4]);
				boolean f_epi = Boolean.parseBoolean (args[5]);
				int num_rep = Integer.parseInt (args[6]);
				boolean f_discard = Boolean.parseBoolean (args[7]);
				boolean f_randsum = Boolean.parseBoolean (args[8]);
				boolean f_data_fmt = Boolean.parseBoolean (args[9]);
				boolean f_keep_empty = Boolean.parseBoolean (args[10]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("Minimum magnitude for simulated catalog: " + cat_min_mag);
				System.out.println ("Use epistemic uncertainty for simulation parameters: " + f_epi);
				System.out.println ("Number of repetitions: " + num_rep);
				System.out.println ("Discard simulations with a large aftershock: " + f_discard);
				System.out.println ("Randomize simulation sum over earthquakes: " + f_randsum);
				System.out.println ("Write data format: " + f_data_fmt);
				System.out.println ("Keep lines with no data: " + f_keep_empty);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				gamma_config.no_epistemic_uncertainty = !f_epi;
				//gamma_config.sim_start_off = 0L;
				gamma_config.discard_sim_with_large_as = f_discard;

				if (!( f_randsum )) {
					gamma_config.eqk_summation_count = gamma_config.simulation_count;
					gamma_config.eqk_summation_randomize = false;
				}

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Repeat desired number of times

						for (int i_rep = 0; i_rep < num_rep; ++i_rep) {

							if (i_rep > 0) {
								System.out.println ("Processing event " + events_processed + " repetition " + (i_rep + 1) + ": " + the_event_id);
							}

							// Fetch the mainshock info

							ForecastMainshock fcmain = new ForecastMainshock();
							fcmain.setup_mainshock_only (the_event_id);

							// Compute models

							EqkForecastSet eqk_forecast_set = new EqkForecastSet();
							eqk_forecast_set.run_sim_simulations (gamma_config,
								gamma_config.simulation_count, fcmain, cat_min_mag, f_epi, false, false);

							// Add in to total

							total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
						}
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Write to file

					String lines;
					if (f_data_fmt) {
						lines = total.single_event_zeta_to_lines (gamma_config, 0, f_keep_empty);
					} else {
						lines = total.single_event_zeta_to_string (gamma_config, f_keep_empty);
					}

					if (!( lines.isEmpty() )) {
						writer.write (lines);
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_sim_agg_zeta_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_sim_agg_zeta_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_sim_agg_zeta_table_2 - Write the aggregated zeta table for a list of earthquakes, using simulated aftershock sequences.
	// Command format:
	//  sim_agg_zeta_table_2  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum  f_data_fmt  f_keep_empty
	// Read the list of events, and for each event compute the log-likelihoods and event counts.
	// Sum over all events, and write the combined tables.
	// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
	// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
	// Each event is repeated num_rep times.
	// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
	// If f_randsum is true, then randomize sum over earthquakes.  (default true)
	// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
	// The boolean f_keep_empty is true to write lines that contain no data.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.
	// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
	// Typical ActionConfig.json setup is:
	//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
	//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
	//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
	//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

	public static void cmd_sim_agg_zeta_table_2(String[] args) {

		// 10 additional arguments

		if (args.length != 11) {
			System.err.println ("GammaCmd : Invalid 'sim_agg_zeta_table_2' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String gamma_table_filename = args[3];
				double cat_min_mag = Double.parseDouble (args[4]);
				boolean f_epi = Boolean.parseBoolean (args[5]);
				int num_rep = Integer.parseInt (args[6]);
				boolean f_discard = Boolean.parseBoolean (args[7]);
				boolean f_randsum = Boolean.parseBoolean (args[8]);
				boolean f_data_fmt = Boolean.parseBoolean (args[9]);
				boolean f_keep_empty = Boolean.parseBoolean (args[10]);

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Function: " + "cmd_sim_agg_zeta_table_2");
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Gamma table filename: " + gamma_table_filename);
				System.out.println ("Minimum magnitude for simulated catalog: " + cat_min_mag);
				System.out.println ("Use epistemic uncertainty for simulation parameters: " + f_epi);
				System.out.println ("Number of repetitions: " + num_rep);
				System.out.println ("Discard simulations with a large aftershock: " + f_discard);
				System.out.println ("Randomize simulation sum over earthquakes: " + f_randsum);
				System.out.println ("Write data format: " + f_data_fmt);
				System.out.println ("Keep lines with no data: " + f_keep_empty);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				gamma_config.no_epistemic_uncertainty = !f_epi;
				//gamma_config.sim_start_off = 0L;
				gamma_config.discard_sim_with_large_as = f_discard;

				if (!( f_randsum )) {
					gamma_config.eqk_summation_count = gamma_config.simulation_count;
					gamma_config.eqk_summation_randomize = false;
				}

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Total earthquake forecast set

				EqkForecastSet total = new EqkForecastSet();
				total.zero_init (gamma_config, gamma_config.eqk_summation_count);

				// Open the input file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Repeat desired number of times

						for (int i_rep = 0; i_rep < num_rep; ++i_rep) {

							if (i_rep > 0) {
								System.out.println ("Processing event " + events_processed + " repetition " + (i_rep + 1) + ": " + the_event_id);
							}

							// Fetch the mainshock info

							ForecastMainshock fcmain = new ForecastMainshock();
							fcmain.setup_mainshock_only (the_event_id);

							// Compute models

							EqkForecastSet eqk_forecast_set = new EqkForecastSet();
							eqk_forecast_set.run_sim_simulations (gamma_config,
								gamma_config.simulation_count, fcmain, cat_min_mag, f_epi, false, false);

							// Add in to total

							total.add_from (gamma_config, eqk_forecast_set, gamma_config.eqk_summation_randomize);
						}
					}
				}

				// Open the output file

				try (
					Writer writer = new BufferedWriter (new FileWriter (gamma_table_filename));
				){
					// Compute the gamma table and statistics table

					String gamma_table = total.single_event_gamma_to_string (gamma_config);
					String stats_table = total.compute_count_stats_to_string (gamma_config);

					// Write to file

					writer.write (gamma_table);
					writer.write ("\n");
					writer.write (stats_table);
					writer.write ("\n");

					// Write to file

					String lines;
					if (f_data_fmt) {
						lines = total.single_event_zeta_to_lines (gamma_config, 0, f_keep_empty);
					} else {
						lines = total.single_event_zeta_to_string (gamma_config, f_keep_empty);
					}

					if (!( lines.isEmpty() )) {
						writer.write (lines);
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_sim_agg_zeta_table_2 had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_sim_agg_zeta_table_2 had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_split_by_time - Split an event list into two or more parts, by time.
	// Command format:
	//  split_by_time  log_filename  event_list_filename  out_filename_pattern  num_parts
	// Read the list of events, and split into num_parts according to event time.
	// Write each part to a separate file.
	// The out_filename_pattern must be suitable as the first argument to String.format,
	// for a single integer value which is the part number (1, 2, ...).
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.

	public static void cmd_split_by_time(String[] args) {

		// 4 additional arguments

		if (args.length != 5) {
			System.err.println ("GammaCmd : Invalid 'split_by_time' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String out_filename_pattern = args[3];
				int num_parts = Integer.parseInt (args[4]);

				if (num_parts < 1) {
					throw new RuntimeException ("Illegal number of parts: " + num_parts);
				}

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Function: " + "cmd_split_by_time");
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Output filename pattern: " + out_filename_pattern);
				System.out.println ("Number of parts: " + num_parts);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// List of earthquakes

				ArrayList<ObsEqkRupture> rup_list = new ArrayList<ObsEqkRupture>();

				// Open the input file

				int events_retrieved = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_retrieved;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Retrieving event " + events_retrieved + ": " + the_event_id);

						// Fetch the rupture

						ObsEqkRupture rup = accessor.fetchEvent (the_event_id, false, false);

						if (rup == null) {
							throw new RuntimeException ("Unable to retrieve catalog entry for event: " + the_event_id);
						}

						// Add to the List

						rup_list.add (rup);
					}
				}

				// Display the count

				System.out.println ("");
				System.out.println ("Events retrieved = " + events_retrieved);

				// Sort list by time

				rup_list.sort (new ObsEqkRupMinTimeComparator());

				// Loop over parts

				for (int i_part = 1; i_part <= num_parts; ++i_part) {

					// Index range for this part

					int k_lo = ((i_part - 1) * rup_list.size()) / num_parts;
					int k_hi = (i_part * rup_list.size()) / num_parts;

					System.out.println ("");
					System.out.println ("Writing part " + i_part);

					// Open the output file

					try (
						Writer writer = new BufferedWriter (new FileWriter (String.format (out_filename_pattern, i_part)));
					){
						// Write the events to the file

						for (int k = k_lo; k < k_hi; ++k) {
							ObsEqkRupture rup = rup_list.get(k);
							String the_event_id = rup.getEventId();
							System.out.println ("Writing part " + i_part + " event " + (k + 1 - k_lo) + ": " + the_event_id + ", time = " + SimpleUtils.time_raw_and_string(rup.getOriginTime()));
							writer.write (the_event_id + "\n");
						}
					}

					// Display the count

					System.out.println ("");
					System.out.println ("Part " + i_part + " events written = " + (k_hi - k_lo));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_split_by_time had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_split_by_time had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_split_by_mag - Split an event list into two or more parts, by magnitude.
	// Command format:
	//  split_by_mag  log_filename  event_list_filename  out_filename_pattern  num_parts
	// Read the list of events, and split into num_parts according to event magnitude.
	// Write each part to a separate file.
	// The out_filename_pattern must be suitable as the first argument to String.format,
	// for a single integer value which is the part number (1, 2, ...).
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.

	public static void cmd_split_by_mag(String[] args) {

		// 4 additional arguments

		if (args.length != 5) {
			System.err.println ("GammaCmd : Invalid 'split_by_mag' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String out_filename_pattern = args[3];
				int num_parts = Integer.parseInt (args[4]);

				if (num_parts < 1) {
					throw new RuntimeException ("Illegal number of parts: " + num_parts);
				}

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Function: " + "cmd_split_by_time");
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Output filename pattern: " + out_filename_pattern);
				System.out.println ("Number of parts: " + num_parts);
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// List of earthquakes

				ArrayList<ObsEqkRupture> rup_list = new ArrayList<ObsEqkRupture>();

				// Open the input file

				int events_retrieved = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_retrieved;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Retrieving event " + events_retrieved + ": " + the_event_id);

						// Fetch the rupture

						ObsEqkRupture rup = accessor.fetchEvent (the_event_id, false, false);

						if (rup == null) {
							throw new RuntimeException ("Unable to retrieve catalog entry for event: " + the_event_id);
						}

						// Add to the List

						rup_list.add (rup);
					}
				}

				// Display the count

				System.out.println ("");
				System.out.println ("Events retrieved = " + events_retrieved);

				// Sort list by magnitude

				rup_list.sort (new ObsEqkRupMinMagComparator());

				// Loop over parts

				for (int i_part = 1; i_part <= num_parts; ++i_part) {

					// Index range for this part

					int k_lo = ((i_part - 1) * rup_list.size()) / num_parts;
					int k_hi = (i_part * rup_list.size()) / num_parts;

					System.out.println ("");
					System.out.println ("Writing part " + i_part);

					// Open the output file

					try (
						Writer writer = new BufferedWriter (new FileWriter (String.format (out_filename_pattern, i_part)));
					){
						// Write the events to the file

						for (int k = k_lo; k < k_hi; ++k) {
							ObsEqkRupture rup = rup_list.get(k);
							String the_event_id = rup.getEventId();
							System.out.println ("Writing part " + i_part + " event " + (k + 1 - k_lo) + ": " + the_event_id + ", mag = " + String.format("%.3f", rup.getMag()));
							writer.write (the_event_id + "\n");
						}
					}

					// Display the count

					System.out.println ("");
					System.out.println ("Part " + i_part + " events written = " + (k_hi - k_lo));
				}

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_split_by_mag had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_split_by_mag had an exception");
			e.printStackTrace();
		}

		return;
	}




	// cmd_comp_param_win - Write the table comparing parameters in different windows, for a list of earthquakes.
	// Command format:
	//  comp_param_table  log_filename  event_list_filename  comp_table_filename  f_keep  start_lag_1  end_lag_1  [start_lag_2  end_lag_2]...
	// Read the list of events, and for each event compute the model parameters in each time window.
	// Write the results for all earthquakes.
	// The boolean f_keep is true to write lines that do not contain full data.
	// Each start and end lag is in java.time.Duration format.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.

	public static void cmd_comp_param_table(String[] args) {

		// At least 7 arguments, and an odd number

		if (args.length < 7 || args.length % 2 != 1) {
			System.err.println ("GammaCmd : Invalid 'comp_param_table' subcommand");
			return;
		}

		String log_filename = args[1];

		// Redirect to the log file

		try (

			// Console redirection and log

			ConsoleRedirector con_red = ConsoleRedirector.make_redirector (
				new BufferedOutputStream (new FileOutputStream (log_filename)), true, true);

		){

			try {

				// Parse arguments

				String event_list_filename = args[2];
				String comp_table_filename = args[3];
				boolean f_keep = Boolean.parseBoolean (args[4]);

				int my_num_win = (args.length - 5) / 2;
				CompParamWin.StartEnd[] my_win_list = new CompParamWin.StartEnd[my_num_win];
				for (int i_win = 0; i_win < my_num_win; ++i_win) {
					long my_start_lag = SimpleUtils.string_to_duration (args[2*i_win + 5]);
					long my_end_lag = SimpleUtils.string_to_duration (args[2*i_win + 6]);
					my_win_list[i_win] = new CompParamWin.StartEnd (my_start_lag, my_end_lag);
				}

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Event list filename: " + event_list_filename);
				System.out.println ("Comparison table filename: " + comp_table_filename);
				System.out.println ("Keep lines without complete data: " + f_keep);
				for (int i_win = 0; i_win < my_num_win; ++i_win) {
					System.out.println ("Window " + i_win + ": "
						+ SimpleUtils.duration_to_string_2 (my_win_list[i_win].get_start_lag()) + " to "
						+ SimpleUtils.duration_to_string_2 (my_win_list[i_win].get_end_lag()));
				}
				System.out.println ("");

				// Adjust verbosity

				ComcatOAFAccessor.load_local_catalog();	// So catalog in use is displayed
				AftershockVerbose.set_verbose_mode (false);
				System.out.println ("");

				// Get configuration

				GammaConfig gamma_config = new GammaConfig();

				System.out.println (gamma_config.toString());
				System.out.println ("");

				// Open the input file and output file

				int events_processed = 0;

				try (
					Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
					Writer writer = new BufferedWriter (new FileWriter (comp_table_filename));
				){
					// Loop over earthquakes

					while (scanner.hasNext()) {

						// Count it

						++events_processed;

						// Read the event id

						String the_event_id = scanner.next();
						System.out.println ("Processing event " + events_processed + ": " + the_event_id);

						// Fetch the mainshock info

						ForecastMainshock fcmain = new ForecastMainshock();
						fcmain.setup_mainshock_only (the_event_id);

						// Compute models

						CompParamWin comp_param_win = new CompParamWin();
						comp_param_win.calc_params (gamma_config, my_win_list, fcmain, false);

						// Write output

						String lines = comp_param_win.single_event_comp_to_lines (gamma_config, events_processed, f_keep);

						if (!( lines.isEmpty() )) {
							writer.write (lines);
						}
					}
				}

				// Display the result

				System.out.println ("");
				System.out.println ("Events processed = " + events_processed);

			}

			// Report any uncaught exceptions

			catch (Exception e) {
				System.out.println ("cmd_comp_param_table had an exception");
				e.printStackTrace();
			}
		}

		// Report any uncaught exceptions

		catch (Exception e) {
			System.out.println ("cmd_comp_param_table had an exception");
			e.printStackTrace();
		}

		return;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GammaCmd : Missing subcommand");
			return;
		}

		switch (args[0].toLowerCase()) {


		// Subcommand : cmd_list_events
		// Command format:
		//  list_events  log_filename  event_list_filename  start_time  end_time  min_mag  max_mag
		// Get a world-wide catalog, and write a list of event ids to the given output file.
		// Events that are shadowed are excluded.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		case "list_events":
			try {
				cmd_list_events(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_gamma_table
		// Command format:
		//  gamma_table  log_filename  event_list_filename  gamma_table_filename
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "gamma_table":
			try {
				cmd_gamma_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_zepi_gamma_table
		// Command format:
		//  gamma_table  log_filename  event_list_filename  gamma_table_filename
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		// Simulations are run with zero epistemic uncertainty.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "zepi_gamma_table":
			try {
				cmd_zepi_gamma_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_stacked_gui_cat
		// Command format:
		//  stacked_gui_cat  log_filename  event_list_filename  gui_cat_filename  the_end_lag  main_mag
		// Read the list of events, create a stacked aftershock catalog, and write it in GUI catalog format.
		// The aftershock sequence extends from 0 to the_end_lag, which is given in java.time.Duration format.
		// The fictitious mainshock is at time 0 (Jan 1, 1970) with the given magnitude.

		case "stacked_gui_cat":
			try {
				cmd_stacked_gui_cat(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_sim_gamma_table
		// Command format:
		//  sim_gamma_table  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
		// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.
		// Each event is repeated num_rep times.
		// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
		// If f_randsum is true, then randomize sum over earthquakes.  (default true)
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "sim_gamma_table":
			try {
				cmd_sim_gamma_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_seq_gamma_table
		// Command format:
		//  seq_gamma_table  log_filename   gamma_table_filename  time  mag  lat  lon  cat_min_mag  f_epi  num_rep  f_discard  f_randsum
		// Create a simulated mainshock with the given time, magnitude, latitude, and longitude.
		// Simulate the event num_rep times, sum over all events, and write the combined tables.
		// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
		// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
		// If f_randsum is true, then randomize sum over earthquakes.  (default true)
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "seq_gamma_table":
			try {
				cmd_seq_gamma_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_zeta_table
		// Command format:
		//  zeta_table  log_filename  event_list_filename  zeta_table_filename  f_data_fmt  f_keep_empty
		// Read the list of events, and for each event compute the cumulative probabilities.
		// Write the results for all earthquakes.
		// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
		// The boolean f_keep_empty is true to write lines that contain no data.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "zeta_table":
			try {
				cmd_zeta_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_sim_zeta_table
		// Command format:
		//  sim_zeta_table  log_filename  event_list_filename  zeta_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_data_fmt  f_keep_empty
		// Read the list of events, and for each event compute the cumulative probabilities.
		// Write the results for all earthquakes.
		// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
		// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
		// Each event is repeated num_rep times.
		// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
		// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
		// The boolean f_keep_empty is true to write lines that contain no data.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "sim_zeta_table":
			try {
				cmd_sim_zeta_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_agg_zeta_table
		// Command format:
		//  agg_zeta_table  log_filename  event_list_filename  gamma_table_filename  f_data_fmt  f_keep_empty
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
		// The boolean f_keep_empty is true to write lines that contain no data.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "agg_zeta_table":
			try {
				cmd_agg_zeta_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_sim_agg_zeta_table
		// Command format:
		//  sim_agg_zeta_table  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum  f_data_fmt  f_keep_empty
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
		// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
		// Each event is repeated num_rep times.
		// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
		// If f_randsum is true, then randomize sum over earthquakes.  (default true)
		// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
		// The boolean f_keep_empty is true to write lines that contain no data.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "sim_agg_zeta_table":
			try {
				cmd_sim_agg_zeta_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_sim_agg_zeta_table_2
		// Command format:
		//  sim_agg_zeta_table_2  log_filename  event_list_filename  gamma_table_filename  cat_min_mag  f_epi  num_rep  f_discard  f_randsum  f_data_fmt  f_keep_empty
		// Read the list of events, and for each event compute the log-likelihoods and event counts.
		// Sum over all events, and write the combined tables.
		// Aftershock sequences are simulated using generic models, with minimum magnitude cat_min_mag.
		// If f_epi is true, then epistemic uncertaintly is used when sampling the generic model.  (default true)
		// Each event is repeated num_rep times.
		// if f_discard is true, then discard simulations that have an aftershock larger than the mainshock. (default true)
		// If f_randsum is true, then randomize sum over earthquakes.  (default true)
		// The boolean f_data_fmt is true to write data file format, false for human-oriented format.
		// The boolean f_keep_empty is true to write lines that contain no data.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.
		// Set up ActionConfig.json to contain the desired forecast advisory windows and magnitude bins.
		// Typical ActionConfig.json setup is:
		//  "adv_min_mag_bins": [ 5.00, 6.00, 7.00 ],
		//  "adv_window_start_offs": [ "P0D", "P0D", "P0D", "P0D", "-P365D" ],
		//  "adv_window_end_offs": [ "P1D", "P7D", "P30D", "P365D", "P0D" ],
		//  "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year", "Retro" ],

		case "sim_agg_zeta_table_2":
			try {
				cmd_sim_agg_zeta_table_2(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_split_by_time
		// Command format:
		//  split_by_time  log_filename  event_list_filename  out_filename_pattern  num_parts
		// Read the list of events, and split into num_parts according to event time.
		// Write each part to a separate file.
		// The out_filename_pattern must be suitable as the first argument to String.format,
		// for a single integer value which is the part number (1, 2, ...).
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.

		case "split_by_time":
			try {
				cmd_split_by_time(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_split_by_mag
		// Command format:
		//  split_by_mag  log_filename  event_list_filename  out_filename_pattern  num_parts
		// Read the list of events, and split into num_parts according to event magnitude.
		// Write each part to a separate file.
		// The out_filename_pattern must be suitable as the first argument to String.format,
		// for a single integer value which is the part number (1, 2, ...).
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.

		case "split_by_mag":
			try {
				cmd_split_by_mag(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		// Subcommand : cmd_comp_param_table
		// Command format:
		//  comp_param_table  log_filename  event_list_filename  comp_table_filename  f_keep  start_lag_1  end_lag_1  [start_lag_2  end_lag_2]...
		// Read the list of events, and for each event compute the model parameters in each time window.
		// Write the results for all earthquakes.
		// The boolean f_keep is true to write lines that do not contain full data.
		// Each start and end lag is in java.time.Duration format.
		//
		// Usage requirements:
		// Set up ServerConfig.json to read from the desired catalog.

		case "comp_param_table":
			try {
				cmd_comp_param_table(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;


		}

		// Unrecognized subcommand.

		System.err.println ("GammaCmd : Unrecognized subcommand : " + args[0]);
		return;
	}
}
