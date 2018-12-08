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




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatOAFProduct : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatOAFProduct : Unrecognized subcommand : " + args[0]);
		return;

	}

}
