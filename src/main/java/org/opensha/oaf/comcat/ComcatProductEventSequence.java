package org.opensha.oaf.comcat;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.math.BigDecimal;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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

import org.opensha.oaf.pdl.PDLProductFile;

import gov.usgs.earthquake.event.JsonEvent;


/**
 * A Comcat event sequence product, as retrieved from Comcat.
 * Author: Michael Barall 01/08/2022.
 *
 * This class is preliminary.
 */
public class ComcatProductEventSequence extends ComcatProduct {

	// The event sequence properties, or null if none.

	public PropertiesEventSequence evs_properties;


	// Return true if the given product type is valid for this class.

	public static boolean is_valid_product_type (String product_type) {
		switch (product_type) {
			case PRODTYPE_EVENT_SEQUENCE:
				return true;
		}
		return false;
	}


	// Default product type.
	// (In subclasses this should be the product type of the subclass)

	private static final String default_product_type = PRODTYPE_EVENT_SEQUENCE;




	// Read contents from a GeoJson product.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	//  delete_ok = True if delete products are OK, false if not.
	// Returns true if success, false if data missing or mis-formatted.
	// Returns false if it is a delete product and delete_ok is false.
	//
	// Note: A subclass may override this method to add additional fields
	// or additional error checks.  The subclass should first pass thru
	// to this class.  If this function returns false then the subclass
	// should immediately return false; otherwise, the subclass should
	// perform its own processing.

	@Override
	protected boolean read_from_gj (JSONObject gj_product, boolean delete_ok) {

		// Pass thru

		if (!( super.read_from_gj (gj_product, delete_ok) )) {
			return false;
		}

		// For event sequence, there must be event sequence properties

		if (!( isDelete )) {

			evs_properties = new PropertiesEventSequence();
			if (!( evs_properties.read_from_product_gj (gj_product) )) {
				return false;
			}
		
		} else {

			evs_properties = new PropertiesEventSequence();
			if (!( evs_properties.read_from_product_gj_for_delete (gj_product) )) {
				return false;
			}
		
		}

		return true;
	}




	// Make a product from a GeoJson product.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns the constructed product, or null if product is missing or mis-formatted.
	// Returns null if it is a delete product and delete_ok is false or omitted.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static ComcatProductEventSequence make_from_gj (JSONObject gj_product) {
		return make_from_gj (gj_product, false);
	}

	public static ComcatProductEventSequence make_from_gj (JSONObject gj_product, boolean delete_ok) {
		ComcatProductEventSequence the_product = new ComcatProductEventSequence();
		if (the_product.read_from_gj (gj_product, delete_ok)) {
			return the_product;
		}
		return null;
	}




	// Make a preferred product from a GeoJson event.
	// Parameters:
	//  event = GeoJson containing the event.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns the constructed product, or null if product is missing or mis-formatted.
	// Returns null if it is a delete product and delete_ok is false or omitted.
	// Note: The preferred product is the first entry in the array of products.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static ComcatProductEventSequence make_preferred_from_gj (JSONObject event) {
		return make_preferred_from_gj (event, false);
	}

	public static ComcatProductEventSequence make_preferred_from_gj (JSONObject event, boolean delete_ok) {
		int index = 0;
		JSONObject gj_product = GeoJsonUtils.getJsonObject (event, "properties", "products", default_product_type, index);
		if (gj_product != null) {
			return make_from_gj (gj_product, delete_ok);
		}
		return null;
	}




	// Make a list of products from a GeoJson event.
	//  event = GeoJson containing the event.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns empty list if products do not exist, or if data missing or mis-formatted.
	// Omits delete products if delete_ok is false or omitted.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static List<ComcatProductEventSequence> make_list_from_gj (JSONObject event) {
		return make_list_from_gj (event, false);
	}

	public static List<ComcatProductEventSequence> make_list_from_gj (JSONObject event, boolean delete_ok) {
		ArrayList<ComcatProductEventSequence> the_product_list = new ArrayList<ComcatProductEventSequence>();

		// Get the array of products

		JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", default_product_type);
		if (gj_product_array != null) {

			// Loop over products in the array

			for (int k = 0; k < gj_product_array.size(); ++k) {

				// Make the product

				JSONObject gj_product = GeoJsonUtils.getJsonObject (gj_product_array, k);
				if (gj_product != null) {
					ComcatProductEventSequence the_product = make_from_gj (gj_product, delete_ok);

					// Add product to the List

					if (the_product != null) {
						the_product_list.add (the_product);
					}
				}
			}
		}

		return the_product_list;
	}




	// Make a string representation.

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder();
		sb.append (super.toString());

		if (evs_properties != null) {
			sb.append (evs_properties.toString());
		} else {
			sb.append ("evs_properties = null\n");
		}

		return sb.toString();
	}




	// Scan Comcat and build a list of events that have the given type of product.
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

	public static List<String> findProductEvents (Boolean f_prod,
			long startTime, long endTime, double minDepth, double maxDepth, ComcatRegion region,
			double minMag, boolean includeDeleted) {

		// Pass thru

		return ComcatProduct.findProductEvents (default_product_type, f_prod,
			startTime, endTime, minDepth, maxDepth, region,
			minMag, includeDeleted);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatProductEventSequence : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  f_use_prod  f_use_feed  event_id  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test1' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			String product_type = default_product_type;
			if (args.length >= 5) {
				product_type = args[4];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

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

				// Get the preferred product

				ComcatProductEventSequence preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				//  ComcatProductEventSequence preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  f_use_prod  f_use_feed  event_id  superseded  delete_ok  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the list of products for the event, and display it.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test2' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];
			boolean superseded = Boolean.parseBoolean (args[4]);
			boolean delete_ok = Boolean.parseBoolean (args[5]);

			String product_type = default_product_type;
			if (args.length >= 7) {
				product_type = args[6];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("superseded: " + superseded);
				System.out.println ("delete_ok: " + delete_ok);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

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

				// Get the product list

				List<ComcatProductEventSequence> product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				//  List<ComcatProductEventSequence> product_list = make_list_from_gj (product_type, accessor.get_last_geojson(), delete_ok);

				for (int k = 0; k < product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("Product " + k);
					System.out.println (product_list.get(k).toString());
					System.out.println ("Summary: " + product_list.get(k).summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  start_time  end_time  min_mag  [product_type]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call findProductEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);

				String product_type = default_product_type;
				if (args.length >= 6) {
					product_type = args[5];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

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
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
		
				boolean includeDeleted = false;

				List<String> eventList = findProductEvents (null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				//  List<String> eventList = findProductEvents (product_type, null,
				//  	startTime, endTime, minDepth, maxDepth, region,
				//  	minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findProductEvents: " + eventList.size());

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
		//  test4  f_use_prod  f_use_feed  event_id  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.
		// Then display any text files in the contents.
		// Same as test #1 except with addition of text file contents.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test4' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			String product_type = default_product_type;
			if (args.length >= 5) {
				product_type = args[4];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

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

				// Get the preferred product

				ComcatProductEventSequence preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				//  ComcatProductEventSequence preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

				// Scan for text files

				if (preferred_product != null) {
					for (String fname : preferred_product.productFiles.keySet()) {
						if (preferred_product.productFiles.get(fname).contentType.equals (PDLProductFile.TEXT_PLAIN)) {
							List<String> lines = preferred_product.read_all_lines_from_contents (fname);
							System.out.println ();
							int nlines = lines.size();
							if (nlines <= 100) {
								System.out.println ("Text file: " + fname + " (" + nlines + " lines)");
							} else {
								System.out.println ("Text file: " + fname + " (showing 100 of " + nlines + " lines)");
								nlines = 100;
							}
							System.out.println ();
							for (int nline = 0; nline < nlines; ++nline) {
								System.out.println (lines.get(nline));
							}
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Skipped.




		// Subcommand : Test #6
		// Command format:
		//  test6  pdl_enable  start_time  end_time  min_mag  include_deleted  [product_type]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call findProductEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Same as test #3 with the include_deleted option added.

		if (args[0].equalsIgnoreCase ("test6")) {

			// Additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test6' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);
				boolean includeDeleted = Boolean.parseBoolean (args[5]);

				String product_type = default_product_type;
				if (args.length >= 7) {
					product_type = args[6];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

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
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

				List<String> eventList = findProductEvents (null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				//  List<String> eventList = findProductEvents (product_type, null,
				//  	startTime, endTime, minDepth, maxDepth, region,
				//  	minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findProductEvents: " + eventList.size());

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




		// Subcommand : Test #7
		// Command format:
		//  test7  f_use_prod  f_use_feed  event_id  filename  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.
		// Then display any text files in the contents.
		// Same as test #4 except user can select a file to display.

		if (args[0].equalsIgnoreCase ("test7")) {

			// Additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ComcatProductEventSequence : Invalid 'test7' subcommand");
				return;
			}

			try {

				boolean f_use_prod = Boolean.parseBoolean (args[1]);
				boolean f_use_feed = Boolean.parseBoolean (args[2]);
				String event_id = args[3];
				String filename = args[4];

				String product_type = default_product_type;
				if (args.length >= 6) {
					product_type = args[5];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("filename: " + filename);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

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

				// Get the preferred product

				ComcatProductEventSequence preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				//  ComcatProductEventSequence preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

				// See if product contains our filename
					
				boolean f_contains_file = preferred_product.contains_file (filename);
				System.out.println ();
				System.out.println ("Contains file: " + f_contains_file);

				// Read file contents as a string

				String file_contents = preferred_product.read_string_from_contents (filename);

				System.out.println ();
				System.out.println (file_contents);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatProductEventSequence : Unrecognized subcommand : " + args[0]);
		return;

	}

}
