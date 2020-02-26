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
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.GeoTools;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphRegionWorld;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;

import gov.usgs.earthquake.event.JsonEvent;


/**
 * A Comcat Shakemap product, as retrieved from Comcat.
 * Author: Michael Barall 02/25/2020.
 */
public class ComcatProductShakemap extends ComcatProduct {


	// Return true if the given product type is valid for this class.

	public static boolean is_valid_product_type (String product_type) {
		switch (product_type) {
			case PRODTYPE_SHAKEMAP:
				return true;
		}
		return false;
	}


	// Default product type.
	// (In subclasses this should be the product type of the subclass)

	private static final String default_product_type = PRODTYPE_SHAKEMAP;
	



	// Filename for the finite fault file in contents, or null if none (null if this is a delete product).
	
	public String faultFilename;




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

		// For shakemap, the fault filename ends in "fault.txt"
		// (There should only be one such file, but if there is more than one
		// then we use the lexicographically first so as to have a repeatable result)

		faultFilename = null;

		if (!( isDelete )) {

			for (String filename : productFiles.keySet()) {
				if (filename.endsWith ("fault.txt")) {
					if (faultFilename == null || filename.compareTo (faultFilename) < 0) {
						faultFilename = filename;
					}
				}
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

	public static ComcatProductShakemap make_from_gj (JSONObject gj_product) {
		return make_from_gj (gj_product, false);
	}

	public static ComcatProductShakemap make_from_gj (JSONObject gj_product, boolean delete_ok) {
		ComcatProductShakemap the_product = new ComcatProductShakemap();
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

	public static ComcatProductShakemap make_preferred_from_gj (JSONObject event) {
		return make_preferred_from_gj (event, false);
	}

	public static ComcatProductShakemap make_preferred_from_gj (JSONObject event, boolean delete_ok) {
		int index = 0;
		JSONObject gj_product = GeoJsonUtils.getJsonObject (event, "properties", "products", default_product_type, index);
		if (gj_product != null) {
			return make_from_gj (gj_product, delete_ok);
		}
		return null;
	}




	// Make a list products from a GeoJson event.
	//  event = GeoJson containing the event.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns empty list if products do not exist, or if data missing or mis-formatted.
	// Omits delete products if delete_ok is false or omitted.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static List<ComcatProductShakemap> make_list_from_gj (JSONObject event) {
		return make_list_from_gj (event, false);
	}

	public static List<ComcatProductShakemap> make_list_from_gj (JSONObject event, boolean delete_ok) {
		ArrayList<ComcatProductShakemap> the_product_list = new ArrayList<ComcatProductShakemap>();

		// Get the array of products

		JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", default_product_type);
		if (gj_product_array != null) {

			// Loop over products in the array

			for (int k = 0; k < gj_product_array.size(); ++k) {

				// Make the product

				JSONObject gj_product = GeoJsonUtils.getJsonObject (gj_product_array, k);
				if (gj_product != null) {
					ComcatProductShakemap the_product = make_from_gj (gj_product, delete_ok);

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
		if (faultFilename != null) {
			sb.append ("faultFilename = " + faultFilename + "\n");
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
	// Returns a list of event IDs that contain an OAF product.

	public static List<String> findProductEvents (Boolean f_prod,
			long startTime, long endTime, double minDepth, double maxDepth, ComcatRegion region,
			double minMag) {

		// Pass thru

		return ComcatProduct.findProductEvents (default_product_type, f_prod,
			startTime, endTime, minDepth, maxDepth, region,
			minMag);
	}




	// Parse the finite fault file.
	// Returns a list of lists of Location.
	// Each top-level list entry is a polygon in 3D defining part of the fault surface.
	// The size of the top-level list is the number of polygons.
	// Each second-level list entry is one vertex of a polygon.
	// The size of a second-level list is the number of vertices in the polygon plus 1,
	// because the initial vertex is repeated at the end of the list.
	// The return is null if there is no finite fault file, or if it does not parse.
	// Throws ComcatException if there is an error reading the file.

	public List<LocationList> parse_finite_fault () {

		// If no finite fault file

		if (faultFilename == null || faultFilename.isEmpty()) {
			return null;
		}

		// Read all the lines of the file

		List<String> lines = read_all_lines_from_contents (faultFilename);

		if (lines == null) {
			return null;
		}

		// The result list

		ArrayList<LocationList> result = new ArrayList<LocationList>();

		// The current sub-list, or null if none

		LocationList sublist = null;

		// Loop over lines

		for (String s : lines) {
			String line = s.trim();

			// If empty line or comment ...

			if (line.isEmpty() || line.startsWith ("#") || line.startsWith (">") || line.startsWith ("//")) {
			
				// If there is an open sublist, close it

				if (sublist != null) {
					result.add (sublist);
					sublist = null;
				}
			}

			// Otherwise, we have a line ...

			else {

				try {

					// The line is expected to contain: lat lon depth(km)

					String[] words = line.split ("\\s+");

					if (words.length != 3) {
						return null;
					}

					double lat = Double.parseDouble (words[0]);
					double lon = Double.parseDouble (words[1]);
					double depth = Double.parseDouble (words[2]);

					if (lat < -90.001 || lat > 90.001) {
						return null;
					}
					if (lat > 90.0) {
						lat = 90.0;
					}
					if (lat < -90.0) {
						lat = -90.0;
					}

					if (lon < -360.001 || lon > 360.001) {
						return null;
					}
					if (lon > 180.0) {
						lon -= 360.0;
					}
					if (lon < -180.0) {
						lon += 360.0;
					}

					if (depth < 0.0) {
						depth = 0.0;
					}
					if (depth > GeoTools.DEPTH_MAX) {
						depth = GeoTools.DEPTH_MAX;
					}

					Location loc = new Location (lat, lon, depth);

					if (sublist == null) {
						sublist = new LocationList();
					}

					sublist.add (loc);
				}
				catch (Exception e) {
					return null;
				}
			}
		}
			
		// If there is an open sublist, close it

		if (sublist != null) {
			result.add (sublist);
			sublist = null;
		}
	
		return result;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatProductShakemap : Missing subcommand");
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
				System.err.println ("ComcatProductShakemap : Invalid 'test1' subcommand");
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

				ComcatProductShakemap preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				//  ComcatProductShakemap preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

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
				System.err.println ("ComcatProductShakemap : Invalid 'test2' subcommand");
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

				List<ComcatProductShakemap> oaf_product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				//  List<ComcatProductShakemap> oaf_product_list = make_list_from_gj (product_type, accessor.get_last_geojson(), delete_ok);

				for (int k = 0; k < oaf_product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("Product " + k);
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
		//  test3  pdl_enable  start_time  end_time  min_mag  [product_type]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call findProductEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ComcatProductShakemap : Invalid 'test3' subcommand");
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

				List<String> eventList = findProductEvents (null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag);

				//  List<String> eventList = findProductEvents (product_type, null,
				//  	startTime, endTime, minDepth, maxDepth, region,
				//  	minMag);

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
		// Then read and display the parsed finite fault.
		// Same as test #1 except with addition of text file contents and finite fault.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ComcatProductShakemap : Invalid 'test4' subcommand");
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

				ComcatProductShakemap preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				//  ComcatProductShakemap preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

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
						if (preferred_product.productFiles.get(fname).contentType.equals ("text/plain")) {
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

				// Get the finite fault

				if (preferred_product != null) {
					List<LocationList> finite_fault = preferred_product.parse_finite_fault();
					System.out.println ();
					if (finite_fault == null) {
						System.out.println ("Finite fault file: None");
					} else {
						System.out.println ("Finite fault file:");
						for (LocationList loc_list : finite_fault) {
							System.out.println ();
							for (Location loc : loc_list) {
								System.out.println (loc.getLatitude() + " " + loc.getLongitude() + " " + loc.getDepth());
							}
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatProductShakemap : Unrecognized subcommand : " + args[0]);
		return;

	}

}
