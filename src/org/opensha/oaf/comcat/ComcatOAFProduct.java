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


/**
 * An OAF product, as retrieved from Comcat.
 * Author: Michael Barall 12/06/2018.
 */
public class ComcatOAFProduct {

	// Event ID, used as the "code" for the product (for example, "us10006jv5") ("code" in the product).

	public String eventID;

	// Source ID, used as the "source" for the product (we always send "us") ("source" in the product).

	public String sourceID;
	
	// Network identifier for the event (for example, "us") ("eventsource" in the product).
	
	public String eventNetwork;
	
	// Network code for the event (for example, "10006jv5") ("eventsourcecode" in the product).
	
	public String eventCode;

	// True if this product has been reviewed ("review-status" in the product).

	public boolean isReviewed;
	
	// JSON text that contains the product.
	
	public String jsonText;
	
	// Time the product was submitted to PDL ("updateTime" in the product).
	
	public long updateTime;

	// Information about a product file.

	public static class ProductFile {
	
		// Mime type ("contentType" in the product).

		public String contentType;

		// URL to file ("url" in the product).

		public String url;
	}

	// Map of product files ("contents" in the product).

	public HashMap<String, ProductFile> productFiles;




	// Read contents from a GeoJson product.
	// Returns true if success, false if data missing or mis-formatted.

	private boolean read_from_gj (JSONObject gj_product) {
	
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

		// JSON text

		jsonText = GeoJsonUtils.getString (gj_product, "contents", "", "bytes");
		if (jsonText == null || jsonText.isEmpty()) {
			return false;
		}

		// Update time

		Long the_update_time = GeoJsonUtils.getTimeMillis (gj_product, "updateTime");
		if (the_update_time == null) {
			return false;
		}
		updateTime = the_update_time;

		// Contents

		productFiles = new HashMap<String, ProductFile>();

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

		return true;
	}




	// Make an OAF product from a GeoJson product.
	// Returns null if data missing or mis-formatted.

	public static ComcatOAFProduct make_from_gj (JSONObject gj_product) {
		ComcatOAFProduct oaf_product = new ComcatOAFProduct();
		if (oaf_product.read_from_gj (gj_product)) {
			return oaf_product;
		}
		return null;
	}




	// Make a list OAF product from a GeoJson event.
	// Returns empty list of OAF products doe not exist, or if data missing or mis-formatted.

	public static List<ComcatOAFProduct> make_list_from_gj (JSONObject event) {
		ArrayList<ComcatOAFProduct> oaf_product_list = new ArrayList<ComcatOAFProduct>();

		// Get the array of OAF products

		JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", "oaf");
		if (gj_product_array != null) {

			// Loop over products in the array

			for (int k = 0; k < gj_product_array.size(); ++k) {

				// Make the product

				JSONObject gj_product = GeoJsonUtils.getJsonObject (gj_product_array, k);
				if (gj_product != null) {
					ComcatOAFProduct oaf_product = make_from_gj (gj_product);

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
		sb.append ("eventID = " + eventID + "\n");
		sb.append ("sourceID = " + sourceID + "\n");
		sb.append ("eventNetwork = " + eventNetwork + "\n");
		sb.append ("eventCode = " + eventCode + "\n");
		sb.append ("isReviewed = " + isReviewed + "\n");
		sb.append ("jsonText = " + jsonText + "\n");
		sb.append ("updateTime = " + updateTime + "\n");
		for (String filename : productFiles.keySet()) {
			sb.append ("File " + filename + ":" + "\n");
			ProductFile the_file = productFiles.get (filename);
			sb.append ("  contentType = " + the_file.contentType + "\n");
			sb.append ("  url = " + the_file.url + "\n");
		}
		return sb.toString();
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
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatOAFProduct : Unrecognized subcommand : " + args[0]);
		return;

	}

}
