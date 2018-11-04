package org.opensha.oaf.aftershockStatistics.gamma;

import java.util.List;
import java.util.Scanner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.Duration;
import java.text.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;

import org.opensha.oaf.aftershockStatistics.ComcatAccessor;
import org.opensha.oaf.aftershockStatistics.RJ_AftershockModel;

import org.opensha.oaf.aftershockStatistics.aafs.ForecastMainshock;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastParameters;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastResults;

import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;
import org.opensha.oaf.aftershockStatistics.util.ObsEqkRupMinTimeComparator;


/**
 * Utility functions for statistical tests.
 * Author: Michael Barall 10/13/2018.
 */
public class GammaUtils {




	// Get the list of all aftershocks for the given event.
	// Parameters:
	//  gamma_config = Configuration information.
	//  fcmain = Mainshock information.
	// The returned list of earthquakes includes all aftershocks from the
	// rupture time to the end of the longest advisory window of the
	// maximum forecast lag.

	public static List<ObsEqkRupture> get_all_aftershocks (GammaConfig gamma_config, ForecastMainshock fcmain) {
	
		// Get the aftershock list end lag

		long as_end_lag = gamma_config.max_forecast_lag + gamma_config.max_adv_window_end_off;

		// Get parameters

		ForecastParameters params = new ForecastParameters();
		params.fetch_all_params (as_end_lag, fcmain, null);

		// Get catalog

		ForecastResults results = new ForecastResults();
		results.calc_catalog_results (fcmain, params);

		if (!( results.catalog_result_avail )) {
			throw new RuntimeException ("GammaUtils.get_all_aftershocks: Failed to get aftershock catalog, event_id = " + fcmain.mainshock_event_id);
		}

		// Return the catalog

		return results.catalog_comcat_aftershocks;
	}




	// Get the list of all aftershocks for the given event, with the times shifted.
	// Parameters:
	//  fcmain = Mainshock information.
	//  the_end_lag = Lag for end of aftershock sequence.
	//  origin_time = Desired origin time for the returned sequence.

	public static List<ObsEqkRupture> get_shifted_aftershocks (
		ForecastMainshock fcmain, long the_end_lag, long origin_time) {

		// Get parameters

		ForecastParameters params = new ForecastParameters();
		params.fetch_all_params (the_end_lag, fcmain, null);

		// Get catalog

		ForecastResults results = new ForecastResults();
		results.calc_catalog_results (fcmain, params);

		if (!( results.catalog_result_avail )) {
			throw new RuntimeException ("GammaUtils.get_shifted_aftershocks: Failed to get aftershock catalog, event_id = " + fcmain.mainshock_event_id);
		}

		// Adjust all the times

		for (ObsEqkRupture rup : results.catalog_comcat_aftershocks) {
			long new_time = rup.getOriginTime() + (origin_time - fcmain.mainshock_time);
			rup.setOriginTime (new_time);
		}

		// Return the catalog

		return results.catalog_comcat_aftershocks;
	}




	// Stack all the aftershock sequences for the events listed in the given file.
	// Parameters:
	//  fcmain = Mainshock information.
	//  the_end_lag = Lag for end of aftershock sequence.
	//  event_list_filename = File containing list of mainshocks.
	//  verbose = True to write progress messages.
	// Note: The returned list is sorted by time.

	public static List<ObsEqkRupture> get_stacked_aftershocks (
		String event_list_filename, long the_end_lag, long origin_time, boolean verbose) throws IOException {

		// Our list of stacked aftershocks

		ObsEqkRupList stacked_as = new ObsEqkRupList();

		// Open the input file

		int events_processed = 0;

		try (
			Scanner scanner = new Scanner (new BufferedReader (new FileReader (event_list_filename)));
		){
			// Loop over earthquakes

			while (scanner.hasNext()) {

				// Read the event id

				String the_event_id = scanner.next();
				if (verbose) {
					System.out.println ("Processing event: " + the_event_id);
				}

				// Fetch the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				// Get the aftershocks, shifted to our origin time

				List<ObsEqkRupture> aftershocks = get_shifted_aftershocks (fcmain, the_end_lag, origin_time);

				// Add to the stacked list

				stacked_as.addAll (aftershocks);

				// Count it

				++events_processed;
			}
		}

		// Sort by time

		stacked_as.sort (new ObsEqkRupMinTimeComparator());

		// Display the result

		if (verbose) {
			System.out.println ("");
			System.out.println ("Events processed = " + events_processed);
		}

		// Return the stacked catalog

		return stacked_as;
	}




	// Date formatter for GUI catalog.
	
	private static SimpleDateFormat catDateFormatGUI = new SimpleDateFormat("yyyy\tMM\tdd\tHH\tmm\tss");
	private static final TimeZone utcGUI = TimeZone.getTimeZone("UTC");
	static {
		catDateFormatGUI.setTimeZone(utcGUI);
	}




	// Given a rupture, create the line for the GUI catalog.
	// Note: The returned string does not end in a newline.
	
	public static String getGUICatalogLine (ObsEqkRupture rup) {
		StringBuilder sb = new StringBuilder();
		Location hypoLoc = rup.getHypocenterLocation();
		sb.append(catDateFormatGUI.format(new Date (rup.getOriginTime()))).append("\t");
		sb.append((float)hypoLoc.getLatitude()).append("\t");
		sb.append((float)hypoLoc.getLongitude()).append("\t");
		sb.append((float)hypoLoc.getDepth()).append("\t");
		sb.append((float)rup.getMag());
		return sb.toString();
	}




	// Given a line from the GUI catalog, create the rupture.
	
	public static ObsEqkRupture fromGUICatalogLine (String line) throws ParseException {
		line = line.trim();
		String[] split = line.split("\\s+");
		if (!( split.length == 10 )) {
			throw new RuntimeException ("Unexpected number of columns. Has " + split.length + ", expected 10");
		}
		String dateStr = split[0]+"\t"+split[1]+"\t"+split[2]+"\t"+split[3]+"\t"+split[4]+"\t"+split[5];
		Date date = catDateFormatGUI.parse(dateStr);
		double lat = Double.parseDouble(split[6]);
		double lon = Double.parseDouble(split[7]);
		double depth = Double.parseDouble(split[8]);
		double mag = Double.parseDouble(split[9]);
		Location hypoLoc = new Location(lat, lon, depth);
		
		String eventId = dateStr.replaceAll("\t", "_")+"_M"+(float)mag;
		long originTimeInMillis = date.getTime();
		
		return new ObsEqkRupture(eventId, originTimeInMillis, hypoLoc, mag);
	}




	// Given a mainshock and list of aftershocks, write the GUI catalog to a file.
	
	public static void writeGUICatalogText (String filename, ObsEqkRupture mainshock, List<ObsEqkRupture> aftershocks) throws IOException {

		try (
			Writer writer = new BufferedWriter (new FileWriter (filename));
		){

			writer.write ("# Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude\n");
			writer.write ("# Main Shock:\n");
			writer.write ("# " + getGUICatalogLine(mainshock) + "\n");

			for (ObsEqkRupture rup : aftershocks) {
				writer.write (getGUICatalogLine(rup) + "\n");
			}
		}

		return;
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GammaUtils : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("GammaUtils : Unrecognized subcommand : " + args[0]);
		return;
	}
}
