package scratch.aftershockStatistics.gamma;

import java.util.List;

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

import scratch.aftershockStatistics.AftershockStatsCalc;
import scratch.aftershockStatistics.AftershockStatsShadow;
import scratch.aftershockStatistics.ComcatAccessor;
import scratch.aftershockStatistics.CompactEqkRupList;
import scratch.aftershockStatistics.RJ_AftershockModel_SequenceSpecific;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import scratch.aftershockStatistics.util.MarshalReader;
import scratch.aftershockStatistics.util.MarshalWriter;
import scratch.aftershockStatistics.util.SimpleUtils;
import scratch.aftershockStatistics.util.ConsoleRedirector;
import scratch.aftershockStatistics.util.SphRegionWorld;
import scratch.aftershockStatistics.util.SphLatLon;
import scratch.aftershockStatistics.util.SphRegion;
import scratch.aftershockStatistics.util.SphRegionCircle;

import scratch.aftershockStatistics.aafs.ActionConfig;
import scratch.aftershockStatistics.aafs.ServerComponent;



/**
 * Gamma statistical test command-line interface.
 * Author: Michael Barall 10/03/2018.
 */
public class GammaCmd {




	// cmd_list_events - List the events to use for the gamma test.
	// Command format:
	//  list_events  log_filename  event_list_filename  start_time  end_time  min_mag
	// Get a world-wide catalog, and write a list of event ids to the given output file.
	// Events that are shadowed are excluded.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	//
	// Usage requirements:
	// Set up ServerConfig.json to read from the desired catalog.

	public static void cmd_list_events(String[] args) {

		// 5 additional arguments

		if (args.length != 6) {
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

				// Say hello

				System.out.println ("Command line:");
				System.out.println (String.join ("  ", args));
				System.out.println ("");

				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + min_mag);
				System.out.println ("");

				// Get configuration

				ActionConfig action_config = new ActionConfig();

				// Create the accessor

				ComcatAccessor accessor = new ComcatAccessor();

				// Construct the Region

				SphRegionWorld region = new SphRegionWorld ();

				// Call Comcat

				String null_event_id = null;
				double minDepth = ComcatAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatAccessor.DEFAULT_MAX_DEPTH;
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

				try (
					Writer writer = new BufferedWriter (new FileWriter (event_list_filename));
				){
					// Loop over ruptures

					for (ObsEqkRupture rup : rup_list) {

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
				System.out.println ("Events tested = " + rup_list.size());
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
		//  list_events  log_filename  event_list_filename  start_time  end_time  min_mag
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

		}

		// Unrecognized subcommand.

		System.err.println ("GammaCmd : Unrecognized subcommand : " + args[0]);
		return;
	}
}
