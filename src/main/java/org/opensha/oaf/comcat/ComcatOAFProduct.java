package org.opensha.oaf.comcat;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.math.BigDecimal;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatEventWebService;
import org.opensha.commons.data.comcat.ComcatVisitor;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import gov.usgs.earthquake.event.EventQuery;
//import gov.usgs.earthquake.event.EventWebService;
//import gov.usgs.earthquake.event.Format;
//import gov.usgs.earthquake.event.JsonEvent;
//import gov.usgs.earthquake.event.JsonUtil;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphRegionWorld;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;

import gov.usgs.earthquake.event.JsonEvent;


/**
 * An OAF product, as retrieved from Comcat.
 * Author: Michael Barall 12/06/2018.
 */
public class ComcatOAFProduct {

	// Flag indicating if this is a delete product (derived from "status" in the product).

	public boolean isDelete;

	// Event ID, used as the "code" for the product (for example, "us10006jv5") ("code" in the product).

	public String eventID;

	// Source ID, used as the "source" for the product (we always send "us") ("source" in the product).

	public String sourceID;
	
	// Network identifier for the event (for example, "us") ("properties/eventsource" in the product).
	
	public String eventNetwork;
	
	// Network code for the event (for example, "10006jv5") ("properties/eventsourcecode" in the product).
	
	public String eventCode;

	// True if this product has been reviewed ("properties/review-status" in the product).

	public boolean isReviewed;
	
	// Time the product was submitted to PDL ("updateTime" in the product).
	
	public long updateTime;
	
	// JSON text that contains the product (null if this is a delete product).
	
	public String jsonText;

	// Information about a product file.

	public static class ProductFile {
	
		// Mime type ("contentType" in the product).

		public String contentType;

		// URL to file ("url" in the product).

		public String url;
	}

	// Map of product files (empty if this is a delete product) ("contents" in the product).

	public HashMap<String, ProductFile> productFiles;




	// Read contents from a GeoJson product.
	// Returns true if success, false if data missing or mis-formatted.
	// Returns false if it is a delete product and delete_ok is false.

	private boolean read_from_gj (JSONObject gj_product, boolean delete_ok) {

		// Delete flag

		isDelete = false;
		String product_status = GeoJsonUtils.getString (gj_product, "status");
		if (product_status != null) {
			if (product_status.equalsIgnoreCase ("DELETE")) {
				isDelete = true;
				if (!( delete_ok )) {
					return false;
				}
			}
		}
	
		// Our code

		eventID = GeoJsonUtils.getString (gj_product, "code");
		if (eventID == null || eventID.isEmpty()) {
			return false;
		}

		// Our source

		sourceID = GeoJsonUtils.getString (gj_product, "source");
		if (sourceID == null || sourceID.isEmpty()) {
			return false;
		}

		// Event network

		eventNetwork = GeoJsonUtils.getString (gj_product, "properties", "eventsource");
		if (eventNetwork == null || eventNetwork.isEmpty()) {
			return false;
		}

		// Event network code

		eventCode = GeoJsonUtils.getString (gj_product, "properties", "eventsourcecode");
		if (eventCode == null || eventCode.isEmpty()) {
			return false;
		}

		// Review status, note that a missing item means not-reviewed

		isReviewed = false;
		String review_status = GeoJsonUtils.getString (gj_product, "properties", "review-status");
		if (review_status != null) {
			if (review_status.equalsIgnoreCase ("reviewed")) {
				isReviewed = true;
			}
		}

		// Update time

		Long the_update_time = GeoJsonUtils.getTimeMillis (gj_product, "updateTime");
		if (the_update_time == null) {
			return false;
		}
		updateTime = the_update_time;

		// JSON text

		jsonText = null;

		if (!( isDelete )) {

			jsonText = GeoJsonUtils.getString (gj_product, "contents", "", "bytes");
			if (jsonText == null || jsonText.isEmpty()) {
				return false;
			}

		}

		// Contents

		productFiles = new HashMap<String, ProductFile>();

		if (!( isDelete )) {

			JSONObject gj_contents = GeoJsonUtils.getJsonObject (gj_product, "contents");
			if (gj_contents == null) {
				return false;
			}

			for (Object key : gj_contents.keySet()) {

				// The filename is the key, an empty filename is the inline data

				String filename = key.toString();
				if (!( filename.isEmpty() )) {

					ProductFile the_file = new ProductFile();

					// MIME type

					the_file.contentType = GeoJsonUtils.getString (gj_contents, filename, "contentType");
					if (the_file.contentType == null || the_file.contentType.isEmpty()) {
						return false;
					}

					// File URL

					the_file.url = GeoJsonUtils.getString (gj_contents, filename, "url");
					if (the_file.url == null || the_file.url.isEmpty()) {
						return false;
					}

					// Add to the map

					productFiles.put (filename, the_file);
				}
			}

		}

		return true;
	}




	// Make an OAF product from a GeoJson product.
	// Returns null if data missing or mis-formatted.
	// Returns null if it is a delete product and delete_ok is false or omitted.

	public static ComcatOAFProduct make_from_gj (JSONObject gj_product) {
		return make_from_gj (gj_product, false);
	}

	public static ComcatOAFProduct make_from_gj (JSONObject gj_product, boolean delete_ok) {
		ComcatOAFProduct oaf_product = new ComcatOAFProduct();
		if (oaf_product.read_from_gj (gj_product, delete_ok)) {
			return oaf_product;
		}
		return null;
	}




	// Make a list OAF product from a GeoJson event.
	// Returns empty list if OAF products do not exist, or if data missing or mis-formatted.
	// Omits delete products if delete_ok is false or omitted.

	public static List<ComcatOAFProduct> make_list_from_gj (JSONObject event) {
		return make_list_from_gj (event, false);
	}

	public static List<ComcatOAFProduct> make_list_from_gj (JSONObject event, boolean delete_ok) {
		ArrayList<ComcatOAFProduct> oaf_product_list = new ArrayList<ComcatOAFProduct>();

		// Get the array of OAF products

		//JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", "oaf");
		JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", (new ServerConfig()).get_pdl_oaf_type());
		if (gj_product_array != null) {

			// Loop over products in the array

			for (int k = 0; k < gj_product_array.size(); ++k) {

				// Make the product

				JSONObject gj_product = GeoJsonUtils.getJsonObject (gj_product_array, k);
				if (gj_product != null) {
					ComcatOAFProduct oaf_product = make_from_gj (gj_product, delete_ok);

					// Add product to the List

					if (oaf_product != null) {
						oaf_product_list.add (oaf_product);
					}
				}
			}
		}

		return oaf_product_list;
	}




	// Make a string representation.

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder();
		sb.append ("status = " + (isDelete ? "DELETE" : "UPDATE") + "\n");
		sb.append ("eventID = " + eventID + "\n");
		sb.append ("sourceID = " + sourceID + "\n");
		sb.append ("eventNetwork = " + eventNetwork + "\n");
		sb.append ("eventCode = " + eventCode + "\n");
		sb.append ("isReviewed = " + isReviewed + "\n");
		sb.append ("updateTime = " + updateTime + "\n");
		if (jsonText != null) {
			sb.append ("jsonText = " + jsonText + "\n");
		}
		for (String filename : productFiles.keySet()) {
			sb.append ("File " + filename + ":" + "\n");
			ProductFile the_file = productFiles.get (filename);
			sb.append ("  contentType = " + the_file.contentType + "\n");
			sb.append ("  url = " + the_file.url + "\n");
		}
		return sb.toString();
	}




	// Make a one-line summary string.

	public String summary_string () {
		StringBuilder sb = new StringBuilder();
		if (isDelete) {
			sb.append ("DELETE, ");
		}
		sb.append ("code = " + eventID);
		if (!( sourceID.equalsIgnoreCase ("us") )) {
			sb.append (", source = " + sourceID);
		}
		sb.append (", reviewed = " + isReviewed);
		sb.append (", time = " + SimpleUtils.time_raw_and_string (updateTime));
		return sb.toString();
	}




	// Scan Comcat and build a list of events that have OAF products.
	// Parameters:
	//  f_prod = True to read from prod-Comcat, false to read from dev-Comcat,
	//           null to read from the configured PDL destination.
	//  startTime = Start of time interval, in milliseconds after the epoch.
	//  endTime = End of time interval, in milliseconds after the epoch.
	//  minDepth = Minimum depth, in km.
	//  maxDepth = Maximum depth, in km.
	//  region = Region to search.
	//  minMag = Minimum magnitude.
	//  includeDeleted = True to include deleted events and events where all products were deleted.
	// Returns a list of event IDs that contain an OAF product.

	public static List<String> findOAFEvents (Boolean f_prod,
			long startTime, long endTime, double minDepth, double maxDepth, ComcatRegion region,
			double minMag, boolean includeDeleted) {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		if (f_prod != null) {
			f_use_prod = f_prod.booleanValue();
		}

		// Get our source code (typically "us")

		//final String our_source = server_config.get_pdl_oaf_source();
		
		// Get the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

		// Make an empty list

		final List<String> eventList = new ArrayList<String>();

		// Visitor for building the list

		ComcatVisitor visitor = new ComcatVisitor() {
			@Override
			public int visit (ObsEqkRupture rup, JsonEvent geojson) {
				eventList.add (rup.getEventId());
				return 0;
			}
		};

		// Call Comcat

		String rup_event_id = null;
		boolean wrapLon = false;
		boolean extendedInfo = true;

		String productType = server_config.get_pdl_oaf_type();

		int visit_result = accessor.visitEventList (visitor, rup_event_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo,
				minMag, productType, includeDeleted);

		System.out.println ("Count of events with OAF products = " + eventList.size());

		// Return the list

		return eventList;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatOAFProduct : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  f_use_prod  f_use_feed  event_id
		// Fetch information for an event, and display it.
		// Then construct the list of OAF products for the event, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Three additional arguments

			if (args.length != 4) {
				System.err.println ("ComcatOAFProduct : Invalid 'test1' subcommand");
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

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the OAF product list

				List<ComcatOAFProduct> oaf_product_list = make_list_from_gj (accessor.get_last_geojson());

				for (int k = 0; k < oaf_product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("OAF product " + k);
					System.out.println (oaf_product_list.get(k).toString());
					System.out.println ("Summary: " + oaf_product_list.get(k).summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  f_use_prod  f_use_feed  event_id  superseded  delete_ok
		// Fetch information for an event, and display it.
		// Then construct the list of OAF products for the event, and display it.
		// Same as test #1, except with an option to include superseded events,
		// and an option to include delete products.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("ComcatOAFProduct : Invalid 'test2' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];
			boolean superseded = Boolean.parseBoolean (args[4]);
			boolean delete_ok = Boolean.parseBoolean (args[5]);

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true, superseded);

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

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the OAF product list

				List<ComcatOAFProduct> oaf_product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				for (int k = 0; k < oaf_product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("OAF product " + k);
					System.out.println (oaf_product_list.get(k).toString());
					System.out.println ("Summary: " + oaf_product_list.get(k).summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  start_time  end_time  min_mag
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call findOAFEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Four additional arguments

			if (args.length != 5) {
				System.err.println ("ComcatOAFProduct : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				// Say hello

				System.out.println ("PDL enable: " + pdl_enable);
				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + minMag);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

				boolean includeDeleted = false;

				List<String> eventList = findOAFEvents (null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findOAFEvents: " + eventList.size());

				// Display the list, up to a maximum size

				int nmax = 100;
				int n = Math.min (nmax, eventList.size());

				for (int i = 0; i < n; ++i) {
					System.out.println (eventList.get(i));
				}

				if (n < eventList.size()) {
					System.out.println ("Plus " + (eventList.size() - n) + " more");
				}

			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  pdl_enable  start_time  end_time  min_mag  include_deleted
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call findOAFEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Same as test #3 with the include_deleted flag added.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 5 additional arguments

			if (args.length != 6) {
				System.err.println ("ComcatOAFProduct : Invalid 'test4' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);
				boolean includeDeleted = Boolean.parseBoolean (args[5]);

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				// Say hello

				System.out.println ("PDL enable: " + pdl_enable);
				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + minMag);
				System.out.println ("include_deleted: " + includeDeleted);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

				List<String> eventList = findOAFEvents (null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findOAFEvents: " + eventList.size());

				// Display the list, up to a maximum size

				int nmax = 100;
				int n = Math.min (nmax, eventList.size());

				for (int i = 0; i < n; ++i) {
					System.out.println (eventList.get(i));
				}

				if (n < eventList.size()) {
					System.out.println ("Plus " + (eventList.size() - n) + " more");
				}

			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatOAFProduct : Unrecognized subcommand : " + args[0]);
		return;

	}

}
