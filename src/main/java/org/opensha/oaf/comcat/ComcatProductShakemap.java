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

import org.opensha.oaf.pdl.PDLProductFile;

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

		// For shakemap, the fault filename ends in "fault.txt" or "rupture.json"
		// (There should only be one such file, but if there is more than one
		// then we use the lexicographically first so as to have a repeatable result)

		faultFilename = null;

		if (!( isDelete )) {

			for (String filename : productFiles.keySet()) {
				if (filename.endsWith ("fault.txt") || filename.endsWith ("rupture.json")) {
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




	// Make a list of products from a GeoJson event.
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
	
		// Get the file info from the contents list.

		ProductFile product_file = productFiles.get (faultFilename);
		if (product_file == null) {
			return null;
		}

		// Read text file ...

		if (product_file.contentType.equals (PDLProductFile.TEXT_PLAIN)) {
			return parse_finite_fault_text_from_contents (faultFilename);
		}

		// Read json file ...

		if (product_file.contentType.equals (PDLProductFile.APPLICATION_JSON)) {
			return parse_finite_fault_json_from_contents (faultFilename);
		}

		// Accept octet-stream as text file if the filename ends in .txt ...

		if (product_file.contentType.equals (PDLProductFile.APPLICATION_OCTET_STREAM) && faultFilename.endsWith (".txt")) {
			return parse_finite_fault_text_from_contents (faultFilename);
		}

		// Accept octet-stream as json file if the filename ends in .json ...

		if (product_file.contentType.equals (PDLProductFile.APPLICATION_OCTET_STREAM) && faultFilename.endsWith (".json")) {
			return parse_finite_fault_json_from_contents (faultFilename);
		}

		// Unknown file type

		return null;
	}




	// Parse a finite fault file, in text format, from a content file.
	// Returns a list of lists of Location.
	// Each top-level list entry is a polygon in 3D defining part of the fault surface.
	// The size of the top-level list is the number of polygons.
	// Each second-level list entry is one vertex of a polygon.
	// The size of a second-level list is the number of vertices in the polygon plus 1,
	// because the initial vertex is repeated at the end of the list.
	// The return is null if the file is successfully read but does not parse.
	// Throws ComcatException if there is an error reading the file.

	public List<LocationList> parse_finite_fault_text_from_contents (String filename) {

		// Read all the lines of the file

		List<String> lines = read_all_lines_from_contents (filename);

		if (lines == null) {		// should never happen
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

					Location loc = permissive_make_loc_from_string (words[0], words[1], words[2]);

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




	// Parse a finite fault file, in json format, from a contents file.
	// Returns a list of lists of Location.
	// Each top-level list entry is a polygon in 3D defining part of the fault surface.
	// The size of the top-level list is the number of polygons.
	// Each second-level list entry is one vertex of a polygon.
	// The size of a second-level list is the number of vertices in the polygon plus 1,
	// because the initial vertex is repeated at the end of the list.
	// The return is null if the file is successfully read but does not parse.
	// Throws ComcatException if there is an error reading the file.

	public List<LocationList> parse_finite_fault_json_from_contents (String filename) {

		// Read the json file

		JSONObject file = read_json_obj_from_contents (filename);

		if (file == null) {		// means the JSON parse failed
			//System.out.println ("JSON parse failed");
			return null;
		}

		// The result list

		ArrayList<LocationList> result = new ArrayList<LocationList>();

		// Get the array of features

		JSONArray features = GeoJsonUtils.getJsonArray (file, "features");

		if (features == null) {
			//System.out.println ("Failed to find 'features' array");
			return null;
		}

		// Loop over features ...

		for (int m = 0; m < features.size(); ++m) {

			// Get the geometry type

			String geom_type = GeoJsonUtils.getString (features, m, "geometry", "type");

			// If it's multi-polygon (the expected type) ...

			if (geom_type != null && geom_type.equals ("MultiPolygon")) {

				// Get the array of polygons

				JSONArray polygons = GeoJsonUtils.getJsonArray (features, m, "geometry", "coordinates", 0);

				if (polygons == null) {
					//System.out.println ("Failed to find 'polygons' array");
					return null;
				}

				// Loop over polygons ...

				for (int k = 0; k < polygons.size(); ++k) {
				
					// Get the polygon

					JSONArray polygon = GeoJsonUtils.getJsonArray (polygons, k);

					if (polygon == null) {
						//System.out.println ("Failed to find 'polygon' array");
						return null;
					}

					// Read the polygon

					try {
						result.add (parse_polygon_from_json_array (polygon));
					}
					catch (Exception e) {
						//System.out.println ("Polygon parse error");
						//System.out.println (SimpleUtils.getStackTraceAsString(e));
						return null;
					}
				}
			}

			// If it's polygon ...

			else if (geom_type != null && geom_type.equals ("Polygon")) {
				
				// Get the polygon

				JSONArray polygon = GeoJsonUtils.getJsonArray (features, m, "geometry", "coordinates", 0);

				if (polygon == null) {
					//System.out.println ("Failed to find 'polygon' array");
					return null;
				}

				// Read the polygon

				try {
					result.add (parse_polygon_from_json_array (polygon));
				}
				catch (Exception e) {
					//System.out.println ("Polygon parse error");
					//System.out.println (SimpleUtils.getStackTraceAsString(e));
					return null;
				}
			}
		}
	
		return result;
	}




	// Parse a polygon from a JSON array.
	// Parameters:
	//  arr = A JSON array, in which each element is a subarray, each of which
	//        contains three numbers (not strings), which are lon lat depth(km).
	// Returns a list of Location, containing the vertices of the polygon.
	// Throws an exception if any error.
	// Note: This function never returns null.
	// Note: It is expected that the first and last vertices are the same,
	// although this function does not check it.

	private static LocationList parse_polygon_from_json_array (JSONArray arr) {
		if (arr == null) {
			throw new IllegalArgumentException ("ComcatProductShakemap.parse_polygon_from_json_array: Missing array.");
		}

		// The resulting list

		LocationList result = new LocationList();

		// Loop over subarrays

		for (int k = 0; k < arr.size(); ++k) {
		
			// Get the subarray, which should have 3 elements

			JSONArray subarr = GeoJsonUtils.getJsonArray (arr, k);
			if (subarr == null) {
				throw new IllegalArgumentException ("ComcatProductShakemap.parse_polygon_from_json_array: Missing sub-array.");
			}
			if (subarr.size() != 3) {
				throw new IllegalArgumentException ("ComcatProductShakemap.parse_polygon_from_json_array: Sub-array does not have 3 elements.");
			}

			// Extract coordinates from sub-array

			Double lon = GeoJsonUtils.getDouble (subarr, 0);
			Double lat = GeoJsonUtils.getDouble (subarr, 1);
			Double depth = GeoJsonUtils.getDouble (subarr, 2);

			// Construct the Location object

			Location loc = permissive_make_loc_from_obj (lat, lon, depth);

			// Add to the list

			result.add (loc);
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

				List<ComcatProductShakemap> product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				//  List<ComcatProductShakemap> product_list = make_list_from_gj (product_type, accessor.get_last_geojson(), delete_ok);

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




		// Subcommand : Test #5
		// Command format:
		//  test5  f_use_prod  f_use_feed  event_id  superseded  delete_ok  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the list of products for the event, and display it.
		// Display the finite fault for each product.
		// Same as test #2 except with addition of finite fault.

		if (args[0].equalsIgnoreCase ("test5")) {

			// Additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("ComcatProductShakemap : Invalid 'test5' subcommand");
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

				List<ComcatProductShakemap> product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				//  List<ComcatProductShakemap> product_list = make_list_from_gj (product_type, accessor.get_last_geojson(), delete_ok);

				for (int k = 0; k < product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("Product " + k);
					System.out.println (product_list.get(k).toString());
					System.out.println ("Summary: " + product_list.get(k).summary_string());

					// Get the finite fault

					List<LocationList> finite_fault = product_list.get(k).parse_finite_fault();
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
				System.err.println ("ComcatProductShakemap : Invalid 'test6' subcommand");
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
				System.err.println ("ComcatProductShakemap : Invalid 'test7' subcommand");
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

		System.err.println ("ComcatProductShakemap : Unrecognized subcommand : " + args[0]);
		return;

	}

}
