package org.opensha.oaf.aftershockStatistics.aafs;

import org.opensha.oaf.aftershockStatistics.util.MarshalImpArray;
import org.opensha.oaf.aftershockStatistics.util.MarshalImpJsonReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalImpJsonWriter;
import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.MarshalException;
import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;

import org.opensha.oaf.aftershockStatistics.CompactEqkRupList;
import org.opensha.oaf.aftershockStatistics.ComcatAccessor;
import org.opensha.oaf.aftershockStatistics.ComcatException;

import org.opensha.oaf.aftershockStatistics.pdl.PDLProductFile;
import org.opensha.oaf.aftershockStatistics.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.aftershockStatistics.pdl.PDLSender;
import gov.usgs.earthquake.product.Product;

/**
 * All the data pertaining to a forecast.
 * Author: Michael Barall 08/24/2018.
 */
public class ForecastData {

	//----- Forecast data -----

	// Header data (introduced in version 2).

	public String creation_time = "";
	public long creation_time_millis = 0L;

	public String code_version = "";
	public int code_major_version = 0;
	public int code_minor_version = 0;
	public int code_build = 0;

	// Mainshock data, cannot be null.

	public ForecastMainshock mainshock = null;

	// Parameters, cannot be null.

	public ForecastParameters parameters = null;

	// Results, cannot be null.

	public ForecastResults results = null;

	// Analyst options, cannot be null.

	public AnalystOptions analyst = null;

	// Catalog, cannot be null.

	public ForecastCatalog catalog = null;

	// The ID used to send to PDL.

	public String pdl_event_id = "";

	// The PDL reviewed status.

	public boolean pdl_is_reviewed = false;




	//----- Construction -----




	// Default constructor.

	public ForecastData () {
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "ForecastData\n"
			+ "creation_time = " + creation_time + "\n"
			+ "creation_time_millis = " + creation_time_millis + "\n"
			+ "code_version = " + code_version + "\n"
			+ "code_major_version = " + code_major_version + "\n"
			+ "code_minor_version = " + code_minor_version + "\n"
			+ "code_build = " + code_build + "\n"
			+ "pdl_event_id = " + pdl_event_id + "\n"
			+ "pdl_is_reviewed = " + pdl_is_reviewed + "\n"
			+ mainshock.toString()
			+ parameters.toString()
			+ results.toString()
			+ analyst.toString()
			+ catalog.toString()
			;
		return str;
	}




	// Set the data.

	public ForecastData set_data (long creation_time, ForecastMainshock mainshock, ForecastParameters parameters,
							ForecastResults results, AnalystOptions analyst, CompactEqkRupList catalog) {
		this.mainshock = mainshock;
		this.parameters = parameters;
		this.results = results;
		this.analyst = analyst;
		this.catalog = (new ForecastCatalog()).set_rupture_list (catalog);
		this.pdl_event_id = "";
		this.pdl_is_reviewed = false;

		this.creation_time_millis = creation_time;
		this.creation_time = SimpleUtils.time_to_string (creation_time);
		this.code_version = VersionInfo.get_one_line_version();
		this.code_major_version = VersionInfo.major_version;
		this.code_minor_version = VersionInfo.minor_version;
		this.code_build = VersionInfo.build;
		return this;
	}




	// Set the data.
	// Note: This should be called soon after results are generated, so the catalog is available.

	public ForecastData set_data (long creation_time, ForecastMainshock mainshock, ForecastParameters parameters,
							ForecastResults results, AnalystOptions analyst) {
		CompactEqkRupList catalog = null;
		if (results.catalog_result_avail) {
			catalog = results.catalog_aftershocks;
		}
		return set_data (creation_time, mainshock, parameters, results, analyst, catalog);
	}




	// Rebuild any transient data.

	public void rebuild_data () {
		results.rebuild_all (mainshock, parameters, catalog.get_rupture_list());
		return;
	}




	// Convert to JSON string.

	public String to_json () {
		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		marshal (writer, null);
		writer.check_write_complete ();
		String json_string = writer.get_json_string();
		return json_string;
	}




	// Set contents from JSON string.
	// Note: This also rebuilds transient data.

	public ForecastData from_json (String json_string) {
		MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);
		unmarshal (reader, null);
		reader.check_read_complete ();
		rebuild_data();
		return this;
	}




	// Set contents from JSON string.
	// Note: This does not rebuild transient data.

	public ForecastData from_json_no_rebuild (String json_string) {
		MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);
		unmarshal (reader, null);
		reader.check_read_complete ();
		return this;
	}




	//----- PDL functions -----




	// Filename that we use.

	public static final String FORECAST_DATA_FILENAME = "forecast_data.json";




	// Make a contents.xml file for PDL.

	public String make_contents_xml () {
		StringBuilder result = new StringBuilder();

		result.append ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		result.append ("<contents>\n");
		result.append ("  <file title=\"Forecast Data\" id=\"forecastData\">\n");
		result.append ("    <caption><![CDATA[Technical data and parameters used to compute the forecast]]></caption>\n");
		result.append ("    <format href=\"" + FORECAST_DATA_FILENAME + "\" type=\"application/json\" />\n");
		result.append ("  </file>\n");
		result.append ("</contents>\n");
		
		return result.toString();
	}




	// Make the PDL product file for contents.xml.

	public PDLProductFile make_product_file_contents_xml () {
		return (new PDLProductFile()).set_bytes (
			make_contents_xml(), PDLProductFile.CONTENTS_XML, PDLProductFile.APPLICATION_XML);
	}




	// Make the PDL product file for forecast data.

	public PDLProductFile make_product_file_forecast_data () {
		return (new PDLProductFile()).set_bytes (
			to_json(), FORECAST_DATA_FILENAME, PDLProductFile.APPLICATION_JSON);
	}




	// Make a PDL product.
	// Parameters:
	//  eventID = The event ID for PDL, which for us identifies the timeline.
	//  isReviewed = Review status, false means automatically generated.

	public Product make_pdl_product (String eventID, boolean isReviewed) throws Exception {

		if (eventID == null || eventID.isEmpty()) {
			throw new IllegalArgumentException ("ForecastData.make_pdl_product: eventID is not specified");
		}

		// Save the PDL parameters

		pdl_event_id = eventID;
		pdl_is_reviewed = isReviewed;

		// The JSON file to send

		String jsonText = results.get_pdl_model();

		if (jsonText == null) {
			throw new IllegalStateException ("ForecastData.make_pdl_product: No JSON file available");
		}

		// The event network and code

		String eventNetwork = mainshock.mainshock_network;
		String eventCode = mainshock.mainshock_code;

		// The event ID, which for us identifies the timeline

		//String eventID = ...;

		// Modification time, 0 means now

		long modifiedTime = 0L;

		// Review status, false means automatically generated

		//boolean isReviewed = ...;

		// The contents.xml file

		PDLProductFile file_contents_xml = make_product_file_contents_xml();

		// The forecast data file

		PDLProductFile file_forecast_data = make_product_file_forecast_data();

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			eventID, eventNetwork, eventCode, isReviewed, jsonText, modifiedTime,
			file_contents_xml, file_forecast_data);
	
		return product;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 46001;
	private static final int MARSHAL_VER_2 = 46002;

	private static final String M_VERSION_NAME = "ForecastData";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 46000;
	protected static final int MARSHAL_FCAST_RESULT = 46001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_FCAST_RESULT;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;
		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			mainshock.marshal  (writer, "mainshock" );
			parameters.marshal (writer, "parameters");
			results.marshal    (writer, "results"   );
			analyst.marshal    (writer, "analyst"   );
			catalog.marshal    (writer, "catalog"   );

			writer.marshalString  ("pdl_event_id"   , pdl_event_id   );
			writer.marshalBoolean ("pdl_is_reviewed", pdl_is_reviewed);

			break;

		case MARSHAL_VER_2:

			writer.marshalString  ("creation_time"       , creation_time       );
			writer.marshalLong    ("creation_time_millis", creation_time_millis);
			writer.marshalString  ("code_version"        , code_version        );
			writer.marshalInt     ("code_major_version"  , code_major_version  );
			writer.marshalInt     ("code_minor_version"  , code_minor_version  );
			writer.marshalInt     ("code_build"          , code_build          );

			writer.marshalString  ("pdl_event_id"   , pdl_event_id   );
			writer.marshalBoolean ("pdl_is_reviewed", pdl_is_reviewed);

			mainshock.marshal  (writer, "mainshock" );
			parameters.marshal (writer, "parameters");
			results.marshal    (writer, "results"   );
			analyst.marshal    (writer, "analyst"   );
			catalog.marshal    (writer, "catalog"   );

			break;
		}
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			mainshock =  (new ForecastMainshock()) .unmarshal (reader, "mainshock" );
			parameters = (new ForecastParameters()).unmarshal (reader, "parameters");
			results =    (new ForecastResults())   .unmarshal (reader, "results"   );
			analyst =    (new AnalystOptions())    .unmarshal (reader, "analyst"   );
			catalog =    (new ForecastCatalog())   .unmarshal (reader, "catalog"   );

			pdl_event_id    = reader.unmarshalString  ("pdl_event_id"   );
			pdl_is_reviewed = reader.unmarshalBoolean ("pdl_is_reviewed");

			creation_time = "";
			creation_time_millis = 0L;
			code_version = "";
			code_major_version = 0;
			code_minor_version = 0;
			code_build = 0;

			break;

		case MARSHAL_VER_2:

			creation_time        = reader.unmarshalString  ("creation_time"       );
			creation_time_millis = reader.unmarshalLong    ("creation_time_millis");
			code_version         = reader.unmarshalString  ("code_version"        );
			code_major_version   = reader.unmarshalInt     ("code_major_version"  );
			code_minor_version   = reader.unmarshalInt     ("code_minor_version"  );
			code_build           = reader.unmarshalInt     ("code_build"          );

			pdl_event_id    = reader.unmarshalString  ("pdl_event_id"   );
			pdl_is_reviewed = reader.unmarshalBoolean ("pdl_is_reviewed");

			mainshock =  (new ForecastMainshock()) .unmarshal (reader, "mainshock" );
			parameters = (new ForecastParameters()).unmarshal (reader, "parameters");
			results =    (new ForecastResults())   .unmarshal (reader, "results"   );
			analyst =    (new AnalystOptions())    .unmarshal (reader, "analyst"   );
			catalog =    (new ForecastCatalog())   .unmarshal (reader, "catalog"   );

			break;
		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public ForecastData unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, ForecastData obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static ForecastData unmarshal_poly (MarshalReader reader, String name) {
		ForecastData result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("ForecastData.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FCAST_RESULT:
			result = new ForecastData();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ForecastData : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastData : Invalid 'test1' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatAccessor.day_millis * 7.0);

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (the_forecast_lag, fcmain, null);

			// Display them

			System.out.println ("");
			System.out.println (params.toString());

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "test1 injectable.", fcmain, params, true);

			// Display them

			System.out.println ("");
			System.out.println (results.toString());

			// Construct the forecast data

			AnalystOptions fc_analyst = new AnalystOptions();
			long ctime = System.currentTimeMillis();

			ForecastData fcdata = new ForecastData();
			fcdata.set_data (ctime, fcmain, params, results, fc_analyst);

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then convert to JSON, and display the JSON.
		// Then read from JSON, and display it (including rebuilt transient data).

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastData : Invalid 'test2' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatAccessor.day_millis * 7.0);

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (the_forecast_lag, fcmain, null);

			// Display them

			System.out.println ("");
			System.out.println (params.toString());

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "", fcmain, params, true);

			// Display them

			System.out.println ("");
			System.out.println (results.toString());

			// Construct the forecast data

			AnalystOptions fc_analyst = new AnalystOptions();
			long ctime = System.currentTimeMillis();

			ForecastData fcdata = new ForecastData();
			fcdata.set_data (ctime, fcmain, params, results, fc_analyst);

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			// Convert to JSON

			String json_string = fcdata.to_json();

			System.out.println ("");
			System.out.println (json_string);

			// Read from JSON

			ForecastData fcdata2 = new ForecastData();
			fcdata2.from_json (json_string);

			System.out.println ("");
			System.out.println (fcdata2.toString());

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test1  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product.

		if (args[0].equalsIgnoreCase ("test3")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastData : Invalid 'test3' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Direct operation to PDL-Development

			ServerConfig server_config = new ServerConfig();
			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatAccessor.day_millis * 7.0);

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (the_forecast_lag, fcmain, null);

			// Display them

			System.out.println ("");
			System.out.println (params.toString());

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "NOTE: This is a test, do not use this forecast.", fcmain, params, true);

			// Select report for PDL, if any

			results.pick_pdl_model();

			// Display them

			System.out.println ("");
			System.out.println (results.toString());

			// Construct the forecast data

			AnalystOptions fc_analyst = new AnalystOptions();
			long ctime = System.currentTimeMillis();

			ForecastData fcdata = new ForecastData();
			fcdata.set_data (ctime, fcmain, params, results, fc_analyst);

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());
			System.out.println ("");

			// Make the PDL product

			Product product;

			try {
				product = fcdata.make_pdl_product (the_event_id, false);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ForecastData : Unrecognized subcommand : " + args[0]);
		return;

	}

}
