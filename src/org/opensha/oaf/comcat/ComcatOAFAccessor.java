package org.opensha.oaf.comcat;

//import gov.usgs.earthquake.event.EventQuery;
//import gov.usgs.earthquake.event.EventWebService;
//import gov.usgs.earthquake.event.Format;
//import gov.usgs.earthquake.event.JsonEvent;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.opensha.commons.geo.GeoTools;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.impl.StringParameter;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.net.UnknownServiceException;
import java.util.zip.ZipException;

import org.opensha.oaf.util.SphLatLon;
//import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionCircle;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.ObsEqkRupMaxTimeComparator;

import org.opensha.oaf.rj.AftershockVerbose;
import org.opensha.oaf.aafs.ServerConfig;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatEventWebService;
import org.opensha.commons.data.comcat.ComcatVisitor;


/**
 * Class for making queries to Comcat, with extended capabilities.
 * Author: Michael Barall.
 */
public class ComcatOAFAccessor extends ComcatAccessor {

	// Flag is set true to write progress to System.out. -- inherited
	
	//protected boolean D = true;

	// The Comcat service provider. -- inherited
	// This is never null, even if a local catalog is in use.
	
	//protected EventWebService service;

	// The list of HTTP status codes for the current operation. -- inherited

	//protected ArrayList<Integer> http_statuses;

	// HTTP status code for a locally completed operation. -- inherited
	// If zero, then HTTP status must be obtained from the service provider.

	//protected int local_http_status;

	// Simulated error rate, or 0.0 if none.
	// Simulated errors are generated with this probability.

	protected double sim_error_rate;

	// Set of event IDs that are hidden, can be null if none.

	protected Set<String> hidden_ids;

	// The local catalog being used, or null if none.
	// If null, then operations are performed by calling Comcat.

	protected ComcatLocalCatalog local_catalog;

	// The cached local catalog, or null if none.

	protected static ComcatLocalCatalog cached_local_catalog = null;

	// The list of filenames for the cached local catalog, or null if no local catalog.
	// This is used to detect if different filenames are selected.

	protected static String[] cached_locat_filenames = null;




	// Check if a simulated error is desired.
	// Throws an exception if a simulated error, also simulates unable to connect.
	// Implementation note: Math.random() is a bad random number generator,
	// but it is sufficient for this purpose.

	protected void check_simulated_error () {
		if (sim_error_rate > 1.0e-6) {
			if (sim_error_rate > Math.random()) {
				local_http_status = -2;
				http_statuses.add (new Integer(get_http_status_code()));
				throw new ComcatSimulatedException ("ComcatOAFAccessor: Simulated Comcat error");
			}
		}
		return;
	}




	// Check if the given id is hidden.

	protected boolean is_hidden_id (String id) {
		if (hidden_ids != null) {
			if (hidden_ids.contains (id)) {
				return true;
			}
		}
		return false;
	}




	// Get or create the cached local catalog.
	// Parameters:
	//  locat_filenames = List of filenames, or null or empty if no local catalog is requested.
	//  locat_bins = Number of latitude bins for the local catalog, or 0 for default.

	protected static synchronized ComcatLocalCatalog get_cached_local_catalog (List<String> locat_filenames, int locat_bins) {
	
		// If no local catalog is requested ...

		if (locat_filenames == null || locat_filenames.isEmpty()) {
		
			// Discard any existing local catalog

			cached_local_catalog = null;
			cached_locat_filenames = null;
		}

		// Otherwise ...

		else {

			// Get the array of filenames

			String[] the_local_filenames = locat_filenames.toArray (new String[0]);

			// If we don't have the local catalog cached already ...

			if (!( cached_local_catalog != null
				&& cached_locat_filenames != null
				&& Arrays.equals (the_local_filenames, cached_locat_filenames) )) {

				// Discard any existing local catalog

				cached_local_catalog = null;
				cached_locat_filenames = null;

				// Load a new local catalog

				boolean f_verbose = AftershockVerbose.get_verbose_mode();

				if (f_verbose) {
					System.out.println ("Loading catalog: " + "[" + String.join (", ", the_local_filenames) + "]");
				}

				ComcatLocalCatalog the_local_catalog = new ComcatLocalCatalog();
				try {
					the_local_catalog.load_catalog (locat_bins, the_local_filenames);
				} catch (Exception e) {
					throw new RuntimeException ("ComcatOAFAccessor: Error loading local catalog: " + "[" + String.join (", ", the_local_filenames) + "]");
				}

				if (f_verbose) {
					System.out.println (the_local_catalog.get_summary_string());
				}

				// Establish the new local catalog

				cached_local_catalog = the_local_catalog;
				cached_locat_filenames = the_local_filenames;
			}
		}

		// Return the cached local catalog, or null

		return cached_local_catalog;
	}




	// Remove any cached local catalog from memory.
	// A fresh local catalog will be loaded (if requested) the next time an object is allocated.
	// Existing objects will continue to use the old catalog.

	public static synchronized void unload_local_catalog () {
		cached_local_catalog = null;
		cached_locat_filenames = null;
		return;
	}




	// Load the local catalog into memory.
	// If not explicitly loaded, then the local catalog will be loaded in the constructor.
	// If the local catalog is already loaded, or if no local catalog is specified
	// in the system configuration, then perform no operation.

	public static void load_local_catalog () {

		// Obtain program configuration

		ServerConfig server_config = new ServerConfig();

		// Load the catalog

		get_cached_local_catalog (server_config.get_locat_filenames(), server_config.get_locat_bins());
		return;
	}



	
	// Construct an object to be used for accessing Comcat.
	// If f_use_config is true, then program configuration information is used
	// to control simulated errors, hidden events, and use of a local catalog.
	// If f_use_config is false, then program configuration information is
	// ignored and the accessor will pass all requests to Comcat.
	// If f_use_config is omitted, the default is true.
	// If f_use_prod is true, then use production servers, otherwise use development servers.
	// If f_use_prod is omitted, the default is true.
	// If f_use_feed is true, then use the real-time feed for single-event queries.
	// If f_use_feed is omited, the default is true.

	public ComcatOAFAccessor () {
		this (true, true, true);
	}

	public ComcatOAFAccessor (boolean f_use_config) {
		this (f_use_config, true, true);
	}

	public ComcatOAFAccessor (boolean f_use_config, boolean f_use_prod) {
		this (f_use_config, f_use_prod, true);
	}

	public ComcatOAFAccessor (boolean f_use_config, boolean f_use_prod, boolean f_use_feed) {

		// We create the service

		super (false);

		// Establish verbose mode

		D = AftershockVerbose.get_verbose_mode();

		// Obtain program configuration

		ServerConfig server_config = new ServerConfig();

		// Get the Comcat service provider

		try {
			//service = new EventWebService(new URL("https://earthquake.usgs.gov/fdsnws/event/1/"));
			//service = new ComcatEventWebService(new URL("https://earthquake.usgs.gov/fdsnws/event/1/"));
			//service = new ComcatEventWebService(new URL(server_config.get_comcat_url()));

			URL serviceURL = null;
			URL feedURL = null;
			if (f_use_prod) {
				serviceURL = new URL (server_config.get_comcat_url());
				if (f_use_feed) {
					if (!( server_config.get_feed_url().isEmpty() )) {
						feedURL = new URL (server_config.get_feed_url());
					}
				}
			} else {
				serviceURL = new URL (server_config.get_comcat_dev_url());
				if (f_use_feed) {
					if (!( server_config.get_feed_dev_url().isEmpty() )) {
						feedURL = new URL (server_config.get_feed_dev_url());
					}
				}
			}
			service = new ComcatEventWebService (serviceURL, feedURL);

		} catch (MalformedURLException e) {
			ExceptionUtils.throwAsRuntimeException(e);
		}

		// Set up HTTP status reporting  -- done in superclass

		//http_statuses = new ArrayList<Integer>();
		//local_http_status = -1;
		//
		//last_geojson = null;

		// If we're using program configuration ...

		if (f_use_config) {
		
			// Set simulated error rate

			sim_error_rate = server_config.get_comcat_err_rate();

			// Set hidden event IDs

			hidden_ids = server_config.get_comcat_exclude();

			// Set local catalog (and load it if needed)

			local_catalog = get_cached_local_catalog (server_config.get_locat_filenames(), server_config.get_locat_bins());
		}

		// Otherwise, direct Comcat access

		else {
			sim_error_rate = 0.0;
			hidden_ids = null;
			local_catalog = null;
		}
	}
	



	/**
	 * Fetches an event with the given ID, e.g. "ci37166079"
	 * @param eventID = Earthquake event id.
	 * @param wrapLon = Desired longitude range: false = -180 to 180; true = 0 to 360.
	 * @param extendedInfo = True to return extended information, see eventToObsRup below.
	 * @return
	 * The return value can be null if the event could not be obtained.
	 * A null return means the event is either not found or deleted in Comcat.
	 * A ComcatException means that there was an error accessing Comcat.
	 */
	@Override
	public ObsEqkRupture fetchEvent (String eventID, boolean wrapLon, boolean extendedInfo) {

		// Initialize HTTP statuses

		http_statuses.clear();
		local_http_status = -1;

		last_geojson = null;

		// Test for simulated error

		check_simulated_error();

		// If the event id is hidden ...

		if (is_hidden_id (eventID)) {

			// Set up HTTP status for event not found

			local_http_status = 404;	// not found
			http_statuses.add (new Integer(get_http_status_code()));
			return null;
		}

		// If we are using a local catalog ...

		if (local_catalog != null) {

			// Fetch event from local catalog, or null if none

			ObsEqkRupture locrup = local_catalog.fetchEvent (eventID, wrapLon, extendedInfo);

			// Set up resulting HTTP status

			if (locrup == null) {
				local_http_status = 404;	// not found
			} else {
				local_http_status = 200;	// success
			}
			http_statuses.add (new Integer(get_http_status_code()));
			return locrup;
		}

		// Pass thru to superclass

		return super.fetchEvent (eventID, wrapLon, extendedInfo);
	}



	
	/**
	 * Visit a list of events satisfying the given conditions.
	 * @param visitor = The visitor that is called for each event, cannot be null.
	 * @param exclude_id = An event id to exclude from the results, or null if none.
	 * @param startTime = Start of time interval, in milliseconds after the epoch.
	 * @param endTime = End of time interval, in milliseconds after the epoch.
	 * @param minDepth = Minimum depth, in km.  Comcat requires a value from -100 to +1000.
	 * @param maxDepth = Maximum depth, in km.  Comcat requires a value from -100 to +1000.
	 * @param region = Region to search.  Events not in this region are filtered out.
	 * @param wrapLon = Desired longitude range: false = -180 to 180; true = 0 to 360.
	 * @param extendedInfo = True to return extended information, see eventToObsRup below.
	 * @param minMag = Minimum magnitude, or -10.0 for no minimum.
	 * @param limit_per_call = Maximum number of events to fetch in a single call to Comcat, or 0 for default.
	 * @param max_calls = Maximum number of calls to ComCat, or 0 for default.
	 * @return
	 * Returns the result code from the last call to the visitor.
	 * Note: As a special case, if endTime == startTime, then the end time is the current time.
	 * Note: This function can retrieve a maximum of about 150,000 earthquakes.  Comcat will
	 * time out if the query matches too many earthquakes, typically with HTTP status 504.
	 * Note: This function is overridden in org.opensha.oaf.comcat.ComcatOAFAccessor.
	 */
	@Override
	public int visitEventList (ComcatVisitor visitor, String exclude_id, long startTime, long endTime,
			double minDepth, double maxDepth, ComcatRegion region, boolean wrapLon, boolean extendedInfo,
			double minMag, int limit_per_call, int max_calls) {

		// Initialize HTTP statuses

		http_statuses.clear();
		local_http_status = -1;

		// Check the visitor

		if (visitor == null) {
			throw new IllegalArgumentException ("ComcatAccessor.visitEventList: No visitor supplied");
		}

		// Test for simulated error

		check_simulated_error();

		// If we are using a local catalog ...

		if (local_catalog != null) {

			// Display the query

			if (D) {
				StringBuilder sb = new StringBuilder();
				sb.append ("Local query");
				sb.append (String.format(": starttime=%s", Instant.ofEpochMilli(startTime).toString()));
				sb.append (String.format(", endtime=%s", Instant.ofEpochMilli(endTime).toString()));
				if (region.isCircular()) {
					sb.append (String.format(", latitude=%.5f", region.getCircleCenterLat()));
					sb.append (String.format(", longitude=%.5f", region.getCircleCenterLon()));
					sb.append (String.format(", maxradius=%.5f", region.getCircleRadiusDeg()));
				}
				else {
					sb.append (String.format(", minlatitude=%.5f", region.getMinLat()));
					sb.append (String.format(", maxlatitude=%.5f", region.getMaxLat()));
					sb.append (String.format(", minlongitude=%.5f", region.getMinLon()));
					sb.append (String.format(", maxlongitude=%.5f", region.getMaxLon()));
				}
				sb.append (String.format(", mindepth=%.3f", minDepth));
				sb.append (String.format(", maxdepth=%.3f", maxDepth));
				sb.append (String.format(", minmagnitude=%.3f", minMag));
				System.out.println (sb.toString());
			}

			// Do the local query

			int result = local_catalog.visitEventList (visitor, exclude_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo, minMag);

			// Set up resulting HTTP status

			local_http_status = 200;	// success
			http_statuses.add (new Integer(get_http_status_code()));
			return result;
		}

		// Pass thru to superclass

		return super.visitEventList (visitor, exclude_id, startTime, endTime,
			minDepth, maxDepth, region, wrapLon, extendedInfo,
			minMag, limit_per_call, max_calls);
	}




	///**
	// * Convert a rupture to a string.
	// * @param rup = The ObsEqkRupture to convert.
	// * @return
	// * Returns string describing the rupture contents.
	// */
	//public static String rupToString (ObsEqkRupture rup) {
	//	StringBuilder result = new StringBuilder();
	//
	//	String rup_event_id = rup.getEventId();
	//	long rup_time = rup.getOriginTime();
	//	double rup_mag = rup.getMag();
	//	Location hypo = rup.getHypocenterLocation();
	//	double rup_lat = hypo.getLatitude();
	//	double rup_lon = hypo.getLongitude();
	//	double rup_depth = hypo.getDepth();
	//
	//	result.append ("ObsEqkRupture:" + "\n");
	//	result.append ("rup_event_id = " + rup_event_id + "\n");
	//	result.append ("rup_time = " + SimpleUtils.time_raw_and_string(rup_time) + "\n");
	//	result.append ("rup_mag = " + rup_mag + "\n");
	//	result.append ("rup_lat = " + rup_lat + "\n");
	//	result.append ("rup_lon = " + rup_lon + "\n");
	//	result.append ("rup_depth = " + rup_depth + "\n");
	//
	//	ListIterator<Parameter<?>> iter = rup.getAddedParametersIterator();
	//	if (iter != null) {
	//		while (iter.hasNext()) {
	//			Parameter<?> param = iter.next();
	//			result.append (param.getName() + " = " + param.getValue() + "\n");
	//		}
	//	}
	//
	//	return result.toString();
	//}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatOAFAccessor : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Fetch information for an event, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test1' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id  min_days  max_days  radius_km  min_mag  limit_per_call
		// Fetch information for an event, and display it.
		// Then fetch the event list for a circle surrounding the hypocenter,
		// for the specified interval in days after the origin time,
		// excluding the event itself.
		// The adjustable limit per call can test the multi-fetch logic.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Six additional arguments

			if (args.length != 7) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test2' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double min_days = Double.parseDouble (args[2]);
				double max_days = Double.parseDouble (args[3]);
				double radius_km = Double.parseDouble (args[4]);
				double min_mag = Double.parseDouble (args[5]);
				int limit_per_call = Integer.parseInt(args[6]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				Location hypo = rup.getHypocenterLocation();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				// Say hello

				System.out.println ("Fetching event list");
				System.out.println ("min_days = " + min_days);
				System.out.println ("max_days = " + max_days);
				System.out.println ("radius_km = " + radius_km);
				System.out.println ("min_mag = " + min_mag);
				System.out.println ("limit_per_call = " + limit_per_call);

				// Construct the Region

				SphRegionCircle region = new SphRegionCircle (new SphLatLon(hypo), radius_km);

				// Calculate the times

				long startTime = rup_time + (long)(min_days*day_millis);
				long endTime = rup_time + (long)(max_days*day_millis);

				// Call Comcat

				double minDepth = DEFAULT_MIN_DEPTH;
				double maxDepth = DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;
				int max_calls = 0;

				ObsEqkRupList rup_list = accessor.fetchEventList (rup_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag, limit_per_call, max_calls);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				int n_status = accessor.get_http_status_count();
				for (int i = 0; i < n_status; ++i) {
					System.out.println ("http_status[" + i + "] = " + accessor.get_http_status_code(i));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id
		// Fetch information for an event, and display it.
		// Same as test #1, except using ComcatAccessor.

		if (args[0].equalsIgnoreCase ("test3")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test3' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatAccessor accessor = new ComcatAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  event_id  min_days  max_days  radius_km  min_mag  limit_per_call
		// Fetch information for an event, and display it.
		// Then fetch the event list for a circle surrounding the hypocenter,
		// for the specified interval in days after the origin time,
		// excluding the event itself.
		// The adjustable limit per call can test the multi-fetch logic.
		// Same as test #2, except using ComcatAccessor.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Six additional arguments

			if (args.length != 7) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test4' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double min_days = Double.parseDouble (args[2]);
				double max_days = Double.parseDouble (args[3]);
				double radius_km = Double.parseDouble (args[4]);
				double min_mag = Double.parseDouble (args[5]);
				int limit_per_call = Integer.parseInt(args[6]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatAccessor accessor = new ComcatAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				Location hypo = rup.getHypocenterLocation();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				// Say hello

				System.out.println ("Fetching event list");
				System.out.println ("min_days = " + min_days);
				System.out.println ("max_days = " + max_days);
				System.out.println ("radius_km = " + radius_km);
				System.out.println ("min_mag = " + min_mag);
				System.out.println ("limit_per_call = " + limit_per_call);

				// Construct the Region

				SphRegionCircle region = new SphRegionCircle (new SphLatLon(hypo), radius_km);

				// Calculate the times

				long startTime = rup_time + (long)(min_days*day_millis);
				long endTime = rup_time + (long)(max_days*day_millis);

				// Call Comcat

				double minDepth = DEFAULT_MIN_DEPTH;
				double maxDepth = DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;
				int max_calls = 0;

				ObsEqkRupList rup_list = accessor.fetchEventList (rup_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag, limit_per_call, max_calls);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				int n_status = accessor.get_http_status_count();
				for (int i = 0; i < n_status; ++i) {
					System.out.println ("http_status[" + i + "] = " + accessor.get_http_status_code(i));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  event_id
		// Fetch information for an event, and display it.
		// Then display the geojson for the event.

		if (args[0].equalsIgnoreCase ("test5")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test5' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				System.out.println (GeoJsonUtils.jsonObjectToString (accessor.get_last_geojson()));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  event_id
		// Fetch information for an event, and display it.
		// Then display the geojson for the event.
		// Same as test #5, except using ComcatAccessor.

		if (args[0].equalsIgnoreCase ("test6")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test6' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatAccessor accessor = new ComcatAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				System.out.println (GeoJsonUtils.jsonObjectToString (accessor.get_last_geojson()));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  f_use_prod  f_use_feed  event_id
		// Fetch information for an event, and display it.

		if (args[0].equalsIgnoreCase ("test7")) {

			// Three additional arguments

			if (args.length != 4) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test7' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  f_use_prod  f_use_feed  event_id  min_days  max_days  radius_km  min_mag  limit_per_call
		// Fetch information for an event, and display it.
		// Then fetch the event list for a circle surrounding the hypocenter,
		// for the specified interval in days after the origin time,
		// excluding the event itself.
		// The adjustable limit per call can test the multi-fetch logic.

		if (args[0].equalsIgnoreCase ("test8")) {

			// Eight additional arguments

			if (args.length != 9) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test8' subcommand");
				return;
			}

			try {

				boolean f_use_prod = Boolean.parseBoolean (args[1]);
				boolean f_use_feed = Boolean.parseBoolean (args[2]);
				String event_id = args[3];
				double min_days = Double.parseDouble (args[4]);
				double max_days = Double.parseDouble (args[5]);
				double radius_km = Double.parseDouble (args[6]);
				double min_mag = Double.parseDouble (args[7]);
				int limit_per_call = Integer.parseInt(args[8]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				Location hypo = rup.getHypocenterLocation();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				// Say hello

				System.out.println ("Fetching event list");
				System.out.println ("min_days = " + min_days);
				System.out.println ("max_days = " + max_days);
				System.out.println ("radius_km = " + radius_km);
				System.out.println ("min_mag = " + min_mag);
				System.out.println ("limit_per_call = " + limit_per_call);

				// Construct the Region

				SphRegionCircle region = new SphRegionCircle (new SphLatLon(hypo), radius_km);

				// Calculate the times

				long startTime = rup_time + (long)(min_days*day_millis);
				long endTime = rup_time + (long)(max_days*day_millis);

				// Call Comcat

				double minDepth = DEFAULT_MIN_DEPTH;
				double maxDepth = DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;
				int max_calls = 0;

				ObsEqkRupList rup_list = accessor.fetchEventList (rup_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag, limit_per_call, max_calls);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				int n_status = accessor.get_http_status_count();
				for (int i = 0; i < n_status; ++i) {
					System.out.println ("http_status[" + i + "] = " + accessor.get_http_status_code(i));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  f_use_prod  f_use_feed  event_id
		// Fetch information for an event, and display it.
		// Then display the geojson for the event.

		if (args[0].equalsIgnoreCase ("test9")) {

			// Three additional arguments

			if (args.length != 4) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test9' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = extendedInfoToMap (rup, EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = idsToList (eimap.get (PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				System.out.println (GeoJsonUtils.jsonObjectToString (accessor.get_last_geojson()));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  event_id  min_days  max_days  radius_km  min_mag  limit_per_call
		// Fetch information for an event, and display it.
		// Then fetch the event list for a circle surrounding the hypocenter,
		// for the specified interval in days after the origin time,
		// excluding the event itself.
		// The adjustable limit per call can test the multi-fetch logic.
		// Same as test #2, except also displays the list of events retrieved.

		if (args[0].equalsIgnoreCase ("test10")) {

			// Six additional arguments

			if (args.length != 7) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test10' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double min_days = Double.parseDouble (args[2]);
				double max_days = Double.parseDouble (args[3]);
				double radius_km = Double.parseDouble (args[4]);
				double min_mag = Double.parseDouble (args[5]);
				int limit_per_call = Integer.parseInt(args[6]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				Location hypo = rup.getHypocenterLocation();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				// Say hello

				System.out.println ("Fetching event list");
				System.out.println ("min_days = " + min_days);
				System.out.println ("max_days = " + max_days);
				System.out.println ("radius_km = " + radius_km);
				System.out.println ("min_mag = " + min_mag);
				System.out.println ("limit_per_call = " + limit_per_call);

				// Construct the Region

				SphRegionCircle region = new SphRegionCircle (new SphLatLon(hypo), radius_km);

				// Calculate the times

				long startTime = rup_time + (long)(min_days*day_millis);
				long endTime = rup_time + (long)(max_days*day_millis);

				// Call Comcat

				double minDepth = DEFAULT_MIN_DEPTH;
				double maxDepth = DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;
				int max_calls = 0;

				ObsEqkRupList rup_list = accessor.fetchEventList (rup_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag, limit_per_call, max_calls);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				int n_status = accessor.get_http_status_count();
				for (int i = 0; i < n_status; ++i) {
					System.out.println ("http_status[" + i + "] = " + accessor.get_http_status_code(i));
				}

				// If list of events is nonempty, display it, in temporal order, most recent first

				if (rup_list.size() > 0) {
					System.out.println ("List of events returned by fetchEventList:");
					Collections.sort (rup_list, new ObsEqkRupMaxTimeComparator());
					int n = 0;
					for (ObsEqkRupture r : rup_list) {
						String r_event_id = r.getEventId();
						long r_time = r.getOriginTime();
						double r_mag = r.getMag();
						Location r_hypo = r.getHypocenterLocation();
						double r_lat = r_hypo.getLatitude();
						double r_lon = r_hypo.getLongitude();
						double r_depth = r_hypo.getDepth();

						String event_info = SimpleUtils.event_id_and_info_one_line (r_event_id, r_time, r_mag, r_lat, r_lon, r_depth);
						System.out.println (n + ": " + event_info);
						++n;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #11
		// Command format:
		//  test11  event_id  min_days  max_days  radius_km  min_mag  limit_per_call
		// Fetch information for an event, and display it.
		// Then fetch the event list for a circle surrounding the hypocenter,
		// for the specified interval in days after the origin time,
		// excluding the event itself.
		// The adjustable limit per call can test the multi-fetch logic.
		// Same as test #10, except using ComcatAccessor.
		// Same as test #4, except also displays the list of events retrieved.

		if (args[0].equalsIgnoreCase ("test11")) {

			// Six additional arguments

			if (args.length != 7) {
				System.err.println ("ComcatOAFAccessor : Invalid 'test11' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double min_days = Double.parseDouble (args[2]);
				double max_days = Double.parseDouble (args[3]);
				double radius_km = Double.parseDouble (args[4]);
				double min_mag = Double.parseDouble (args[5]);
				int limit_per_call = Integer.parseInt(args[6]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatAccessor accessor = new ComcatAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				Location hypo = rup.getHypocenterLocation();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				// Say hello

				System.out.println ("Fetching event list");
				System.out.println ("min_days = " + min_days);
				System.out.println ("max_days = " + max_days);
				System.out.println ("radius_km = " + radius_km);
				System.out.println ("min_mag = " + min_mag);
				System.out.println ("limit_per_call = " + limit_per_call);

				// Construct the Region

				SphRegionCircle region = new SphRegionCircle (new SphLatLon(hypo), radius_km);

				// Calculate the times

				long startTime = rup_time + (long)(min_days*day_millis);
				long endTime = rup_time + (long)(max_days*day_millis);

				// Call Comcat

				double minDepth = DEFAULT_MIN_DEPTH;
				double maxDepth = DEFAULT_MAX_DEPTH;
				boolean wrapLon = false;
				boolean extendedInfo = false;
				int max_calls = 0;

				ObsEqkRupList rup_list = accessor.fetchEventList (rup_event_id, startTime, endTime,
						minDepth, maxDepth, region, wrapLon, extendedInfo,
						min_mag, limit_per_call, max_calls);

				// Display the information

				System.out.println ("Events returned by fetchEventList = " + rup_list.size());

				int n_status = accessor.get_http_status_count();
				for (int i = 0; i < n_status; ++i) {
					System.out.println ("http_status[" + i + "] = " + accessor.get_http_status_code(i));
				}

				// If list of events is nonempty, display it, in temporal order, most recent first

				if (rup_list.size() > 0) {
					System.out.println ("List of events returned by fetchEventList:");
					Collections.sort (rup_list, new ObsEqkRupMaxTimeComparator());
					int n = 0;
					for (ObsEqkRupture r : rup_list) {
						String r_event_id = r.getEventId();
						long r_time = r.getOriginTime();
						double r_mag = r.getMag();
						Location r_hypo = r.getHypocenterLocation();
						double r_lat = r_hypo.getLatitude();
						double r_lon = r_hypo.getLongitude();
						double r_depth = r_hypo.getDepth();

						String event_info = SimpleUtils.event_id_and_info_one_line (r_event_id, r_time, r_mag, r_lat, r_lon, r_depth);
						System.out.println (n + ": " + event_info);
						++n;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatOAFAccessor : Unrecognized subcommand : " + args[0]);
		return;

	}

}
