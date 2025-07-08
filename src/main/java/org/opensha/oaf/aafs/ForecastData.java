package org.opensha.oaf.aafs;

import java.util.Map;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.USGS_ForecastHolder;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.commons.data.comcat.ComcatException;

import org.opensha.oaf.pdl.PDLProductFile;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import gov.usgs.earthquake.product.Product;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;
import org.opensha.oaf.pdl.PDLContentsXmlBuilder;

import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.pdl.PDLProductBuilderEventSequence;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;
import org.opensha.oaf.comcat.PropertiesEventSequence;
import org.opensha.oaf.comcat.GeoJsonHolder;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * All the data pertaining to a forecast.
 * Author: Michael Barall 08/24/2018.
 */
public class ForecastData implements Marshalable {

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




	// Convert to JSON file.

	public void to_json_file (String filename) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (filename));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			marshal (writer, null);
			writer.check_write_complete ();
			writer.write_json_file (file_writer);
		}
		catch (IOException e) {
			throw new MarshalException ("ForecastData: I/O error while writing JSON file: " + filename, e);
		}

		return;
	}




	// Set contents from JSON file.
	// Note: This also rebuilds transient data.

	public ForecastData from_json_file (String filename) {

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			unmarshal (reader, null);
			reader.check_read_complete ();
			rebuild_data();
		}
		catch (IOException e) {
			throw new MarshalException ("ForecastData: I/O error while reading JSON file: " + filename, e);
		}

		return this;
	}




	// Set contents from JSON file.
	// Note: This does not rebuild transient data.

	public ForecastData from_json_file_no_rebuild (String filename) {

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			unmarshal (reader, null);
			reader.check_read_complete ();
		}
		catch (IOException e) {
			throw new MarshalException ("ForecastData: I/O error while reading JSON file: " + filename, e);
		}

		return this;
	}




	// Set variables for standard format.
	// Note: This is the default.

	public void set_standard_format () {
		if (catalog != null) {
			catalog.set_standard_format();
		}
		return;
	}




	// Set variables for friendly format.

	public void set_friendly_format () {
		if (catalog != null) {
			catalog.set_friendly_format (mainshock);
		}
		return;
	}




	// Convert a JSON file to friendly format.

	public static ForecastData convert_json_file_to_friendly (String in_filename, String out_filename) {
		ForecastData fcdata = new ForecastData();
		fcdata.from_json_file_no_rebuild (in_filename);
		fcdata.set_friendly_format();
		fcdata.to_json_file (out_filename);
		return fcdata;
	}




	// Convert a JSON file to standard format.

	public static ForecastData convert_json_file_to_standard (String in_filename, String out_filename) {
		ForecastData fcdata = new ForecastData();
		fcdata.from_json_file_no_rebuild (in_filename);
		fcdata.set_standard_format();
		fcdata.to_json_file (out_filename);
		return fcdata;
	}




	//----- Adjustments -----




	// Saved values from adjustments, null if no adjustments have been done.

	private AdjustableParameters.AdjSaveForecastData adj_save_forecast_data = null;




	// Adjust values, after forecast has been computed.

	public void adjust_post_forecast (AdjustableParameters adj_params) {
		if (adj_save_forecast_data == null) {
			adj_save_forecast_data = new AdjustableParameters.AdjSaveForecastData();
		}
		adj_params.adjust_forecast_data (adj_save_forecast_data, this);
		return;
	}




	// Revert any adjustments made after the forecast has been computed.

	public void revert_post_forecast () {
		if (adj_save_forecast_data != null) {
			adj_save_forecast_data.revert_to (this);
			adj_save_forecast_data = null;
		}
		return;
	}




	// Return an auto closeable class that reverts adjustments when closed.

	public class AutoRevert implements AutoCloseable {
		@Override
		public void close() {
			revert_post_forecast();
			return;
		}
	}

	public AutoRevert get_auto_revert () {
		return new AutoRevert();
	}




	//----- PDL functions -----




	// Filename that we use.

	public static final String FORECAST_DATA_FILENAME = "forecast_data.json";


	// Append the forecast data file to PDL contents.

	public static void attach_forecast_data (PDLContentsXmlBuilder content_builder, String text) {
		content_builder.begin_section ("Forecast Data", "forecastData", "Technical data and parameters used to compute the forecast");
		//content_builder.add_file (FORECAST_DATA_FILENAME, PDLProductFile.APPLICATION_JSON, text);
		content_builder.add_file_pad (FORECAST_DATA_FILENAME, PDLProductFile.APPLICATION_JSON, text, PDLProductFile.PAD_SPACE);
		return;
	}




//	// Make a contents.xml file for PDL.
//
//	public String make_contents_xml () {
//		StringBuilder result = new StringBuilder();
//
//		result.append ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//		result.append ("<contents>\n");
//		result.append ("  <file title=\"Forecast Data\" id=\"forecastData\">\n");
//		result.append ("    <caption><![CDATA[Technical data and parameters used to compute the forecast]]></caption>\n");
//		result.append ("    <format href=\"" + FORECAST_DATA_FILENAME + "\" type=\"application/json\" />\n");
//		result.append ("  </file>\n");
//		result.append ("</contents>\n");
//		
//		return result.toString();
//	}




//	// Make the PDL product file for contents.xml.
//
//	public PDLProductFile make_product_file_contents_xml () {
//		return (new PDLProductFile()).set_bytes (
//			make_contents_xml(), PDLProductFile.CONTENTS_XML, PDLProductFile.APPLICATION_XML);
//	}




//	// Make the PDL product file for forecast data.
//
//	public PDLProductFile make_product_file_forecast_data () {
//		return (new PDLProductFile()).set_bytes (
//			to_json(), FORECAST_DATA_FILENAME, PDLProductFile.APPLICATION_JSON);
//	}




	// Make a PDL product.
	// Parameters:
	//  eventID = The event ID for PDL, which for us identifies the timeline.
	//  isReviewed = Review status, false means automatically generated.
	// Returns null if the product cannot be constructed due to the presence
	// of a conflicting product in PDL.

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

		// Choose the code to use

		String suggestedCode = eventID;
		//long reviewOverwrite = (isReviewed ? 0L : 1L);		// don't overwrite reviewed forecast if we're not reviewed
		long reviewOverwrite = -1L;
		String queryID = mainshock.mainshock_event_id;
		//JSONObject geojson = null;
		JSONObject geojson = mainshock.mainshock_geojson;	// it's OK if this is null
		boolean f_gj_prod = true;
		String chosenCode = PDLCodeChooserOaf.chooseOafCode (suggestedCode, reviewOverwrite,
			geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed);

		// If no chosen code, return null to indicate conflict

		if (chosenCode == null || chosenCode.isEmpty()) {
			pdl_event_id = "";
			return null;
		}

		// Save the chosen code

		pdl_event_id = chosenCode;

		// The content builder

		PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

		// If we want forecast.json, attach it

		if (PDLProductBuilderOaf.use_forecast_json()) {
			PDLProductBuilderOaf.attach_forecast (content_builder, jsonText);
		}

		// Attach the forecast data file

		attach_forecast_data (content_builder, to_json());

		// Get the inline text

		String inlineText = null;
		if (PDLProductBuilderOaf.use_inline_text()) {
			inlineText = jsonText;
		}

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			chosenCode, eventNetwork, eventCode, isReviewed, inlineText, modifiedTime,
			content_builder.make_product_file_array());
	
		return product;
	}




	// Make a PDL product.
	// Parameters:
	//  evseq_res = Receives event-sequence pending send or deletion result, if any.
	//  eventID = The event ID for PDL, which for us identifies the timeline.
	//  isReviewed = Review status, false means automatically generated.
	// Returns null if the product cannot be constructed due to the presence
	// of a conflicting product in PDL.
	// This version is used for sending both OAF and event-sequence products.

	public Product make_pdl_product (EventSequenceResult evseq_res, String eventID, boolean isReviewed) throws Exception {

		// Default to no event-sequence operation

		evseq_res.clear();

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

		// Choose the code to use

		String suggestedCode = eventID;
		//long reviewOverwrite = (isReviewed ? 0L : 1L);		// don't overwrite reviewed forecast if we're not reviewed
		long reviewOverwrite = -1L;
		String queryID = mainshock.mainshock_event_id;
		//JSONObject geojson = null;
		JSONObject geojson = mainshock.mainshock_geojson;	// it's OK if this is null
		boolean f_gj_prod = true;
				
		PDLCodeChooserOaf.DeleteOafOp del_op = new PDLCodeChooserOaf.DeleteOafOp();
		GeoJsonHolder gj_used = new GeoJsonHolder (geojson, f_gj_prod);

		String chosenCode = PDLCodeChooserOaf.checkChooseOafCode (suggestedCode, reviewOverwrite,
			geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed, del_op, gj_used);

		// If no chosen code, return null to indicate conflict

		if (chosenCode == null || chosenCode.isEmpty()) {
			pdl_event_id = "";
			del_op.do_delete();		// probably won't be anything to delete
			return null;
		}

		// Save the chosen code

		pdl_event_id = chosenCode;

		// If event-sequence products are enabled, and we have event-sequence parameters ...

		ActionConfig evseq_action_config = new ActionConfig();
		if (evseq_action_config.get_is_evseq_enabled() && parameters.evseq_cfg_avail) {

			// If event-sequence reporting parameter is requesting event-sequence delete ...

			int evseq_report = parameters.evseq_cfg_params.get_evseq_cfg_report();
			if (evseq_report == ActionConfigFile.ESREP_DELETE) {

				// Delete all event-sequence products
					
				System.out.println ("Deleting all event-sequence products.");

				long cap_time = PDLCodeChooserEventSequence.CAP_TIME_DELETE;
				boolean f_keep_reviewed = false;

				int doesp = PDLCodeChooserEventSequence.deleteOldEventSequenceProducts (null,
					gj_used.geojson, gj_used.f_gj_prod, queryID, isReviewed, cap_time, f_keep_reviewed);

				// If succeeded (no exception), report the deletion

				evseq_res.set_for_delete (queryID, doesp, cap_time);
			}

			// If event-sequence reporting parameter is requesting event-sequence send ...

			else if (evseq_report == ActionConfigFile.ESREP_REPORT) {

				// Choose the code, which will always equal the code used for OAF

				boolean f_valid_ok = false;		// forces event-sequence code to be the code we supply, if reviewOverwrite == -1L
				GeoJsonHolder evseq_gj_used = new GeoJsonHolder (gj_used.geojson, gj_used.f_gj_prod);

				String evseq_chosenCode = PDLCodeChooserEventSequence.chooseEventSequenceCode (
					chosenCode, f_valid_ok, reviewOverwrite, gj_used.geojson, gj_used.f_gj_prod, queryID,
					eventNetwork, eventCode, isReviewed, evseq_gj_used);

				// If we didn't get a code ...

				if (evseq_chosenCode == null || evseq_chosenCode.isEmpty()) {
					System.out.println ("Bypassing event-sequence product generation due to inability to obtain a PDL code.");
				}

				// Otherwise, build the event-sequence product ...

				else {

					PropertiesEventSequence evs_props = new PropertiesEventSequence();
					String evseq_err = make_evseq_properties (evs_props, evseq_gj_used.geojson);	// checks for null geojson

					// If error during property build ...

					if (evseq_err != null) {
						System.out.println ("Bypassing event-sequence product generation: " + evseq_err + ".");
					}

					// Otherwise, we have the event-sequence properties

					else {
						System.out.println ("Created event-sequence properties.");
						System.out.println (evs_props.toString());

						evseq_res.set_for_pending_send (queryID, evs_props, evseq_chosenCode, isReviewed);
					}
				}
			}
		}

		// Now delete any OAF products needed

		del_op.do_delete();

		// The content builder

		PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

		// If we want forecast.json, attach it

		if (PDLProductBuilderOaf.use_forecast_json()) {
			PDLProductBuilderOaf.attach_forecast (content_builder, jsonText);
		}

		// Attach the forecast data file

		attach_forecast_data (content_builder, to_json());

		// Get the inline text

		String inlineText = null;
		if (PDLProductBuilderOaf.use_inline_text()) {
			inlineText = jsonText;
		}

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			chosenCode, eventNetwork, eventCode, isReviewed, inlineText, modifiedTime,
			content_builder.make_product_file_array());
	
		return product;
	}




	// Make a PDL product, given the contents of forecast.json.
	// Parameters:
	//  evseq_res = Receives event-sequence pending send or deletion result, if any.
	//  eventID = The event ID for PDL, which for us identifies the timeline (could be the mainshock event ID).
	//  isReviewed = Review status, false means automatically generated.
	//  fcmain = Mainshock information.
	//  evseq_cfg_params = Event-sequence configuration parameters, can be null.
	//  fc_holder = Holds the contents of forecast.json.
	// Returns null if the product cannot be constructed due to the presence
	// of a conflicting product in PDL.
	// This version is used for sending both OAF and event-sequence products.
	// Note: This is a static function.

	public static Product make_pdl_product (EventSequenceResult evseq_res, String eventID, boolean isReviewed,
		ForecastMainshock fcmain, EventSequenceParameters evseq_cfg_params, USGS_ForecastHolder fc_holder) throws Exception {

		// Default to no event-sequence operation

		evseq_res.clear();

		if (eventID == null || eventID.isEmpty()) {
			throw new IllegalArgumentException ("ForecastData.make_pdl_product: eventID is not specified");
		}

		// The JSON file to send

		String jsonText = MarshalUtils.to_json_string (fc_holder);

		if (jsonText == null) {
			throw new IllegalStateException ("ForecastData.make_pdl_product: No JSON file available");
		}

		// The event network and code

		String eventNetwork = fcmain.mainshock_network;
		String eventCode = fcmain.mainshock_code;

		// The event ID, which for us identifies the timeline

		//String eventID = ...;

		// Modification time, 0 means now

		long modifiedTime = 0L;

		// Review status, false means automatically generated

		//boolean isReviewed = ...;

		// Choose the code to use

		String suggestedCode = eventID;
		//long reviewOverwrite = (isReviewed ? 0L : 1L);		// don't overwrite reviewed forecast if we're not reviewed
		long reviewOverwrite = -1L;
		String queryID = fcmain.mainshock_event_id;
		//JSONObject geojson = null;
		JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
		boolean f_gj_prod = true;
				
		PDLCodeChooserOaf.DeleteOafOp del_op = new PDLCodeChooserOaf.DeleteOafOp();
		GeoJsonHolder gj_used = new GeoJsonHolder (geojson, f_gj_prod);

		String chosenCode = PDLCodeChooserOaf.checkChooseOafCode (suggestedCode, reviewOverwrite,
			geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed, del_op, gj_used);

		// If no chosen code, return null to indicate conflict

		if (chosenCode == null || chosenCode.isEmpty()) {
			del_op.do_delete();		// probably won't be anything to delete
			return null;
		}

		// If event-sequence products are enabled, and we have event-sequence parameters ...

		ActionConfig evseq_action_config = new ActionConfig();
		if (evseq_action_config.get_is_evseq_enabled() && evseq_cfg_params != null) {

			// If event-sequence reporting parameter is requesting event-sequence delete ...

			int evseq_report = evseq_cfg_params.get_evseq_cfg_report();
			if (evseq_report == ActionConfigFile.ESREP_DELETE) {

				// Delete all event-sequence products
					
				System.out.println ("Deleting all event-sequence products.");

				long cap_time = PDLCodeChooserEventSequence.CAP_TIME_DELETE;
				boolean f_keep_reviewed = false;

				int doesp = PDLCodeChooserEventSequence.deleteOldEventSequenceProducts (null,
					gj_used.geojson, gj_used.f_gj_prod, queryID, isReviewed, cap_time, f_keep_reviewed);

				// If succeeded (no exception), report the deletion

				evseq_res.set_for_delete (queryID, doesp, cap_time);
			}

			// If event-sequence reporting parameter is requesting event-sequence send ...

			else if (evseq_report == ActionConfigFile.ESREP_REPORT) {

				// Choose the code, which will always equal the code used for OAF

				boolean f_valid_ok = false;		// forces event-sequence code to be the code we supply, if reviewOverwrite == -1L
				GeoJsonHolder evseq_gj_used = new GeoJsonHolder (gj_used.geojson, gj_used.f_gj_prod);

				String evseq_chosenCode = PDLCodeChooserEventSequence.chooseEventSequenceCode (
					chosenCode, f_valid_ok, reviewOverwrite, gj_used.geojson, gj_used.f_gj_prod, queryID,
					eventNetwork, eventCode, isReviewed, evseq_gj_used);

				// If we didn't get a code ...

				if (evseq_chosenCode == null || evseq_chosenCode.isEmpty()) {
					System.out.println ("Bypassing event-sequence product generation due to inability to obtain a PDL code.");
				}

				// Otherwise, build the event-sequence product ...

				else {

					PropertiesEventSequence evs_props = new PropertiesEventSequence();
					String evseq_err = make_evseq_properties (evs_props, evseq_gj_used.geojson,
						evseq_cfg_params, fc_holder.get_evseq_region(), fc_holder.get_evseq_end_time(), false);	// checks for null geojson, evseq_region

					// If error during property build ...

					if (evseq_err != null) {
						System.out.println ("Bypassing event-sequence product generation: " + evseq_err + ".");
					}

					// Otherwise, we have the event-sequence properties

					else {
						System.out.println ("Created event-sequence properties.");
						System.out.println (evs_props.toString());

						evseq_res.set_for_pending_send (queryID, evs_props, evseq_chosenCode, isReviewed);
					}
				}
			}
		}

		// Now delete any OAF products needed

		del_op.do_delete();

		// The content builder

		PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

		// If we want forecast.json, attach it

		if (PDLProductBuilderOaf.use_forecast_json()) {
			PDLProductBuilderOaf.attach_forecast (content_builder, jsonText);
		}

		// Get the inline text

		String inlineText = null;
		if (PDLProductBuilderOaf.use_inline_text()) {
			inlineText = jsonText;
		}

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			chosenCode, eventNetwork, eventCode, isReviewed, inlineText, modifiedTime,
			content_builder.make_product_file_array());
	
		return product;
	}




	// Make a PDL product.
	// Parameters:
	//  eventID = The event ID for PDL, which for us identifies the timeline.
	//  isReviewed = Review status, false means automatically generated.
	//  pdl_code = The code to use for the product.
	// Returns null if the product cannot be constructed due to missing or empty code.
	// This function always uses the supplied code, and does not delete OAF products with different codes.

	public Product make_pdl_product_with_code (String eventID, boolean isReviewed, String pdl_code) throws Exception {

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

		// Choose the code to use

		String chosenCode = pdl_code;

		// If no chosen code, return null to indicate conflict

		if (chosenCode == null || chosenCode.isEmpty()) {
			pdl_event_id = "";
			return null;
		}

		// Save the chosen code

		pdl_event_id = chosenCode;

		// The content builder

		PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

		// If we want forecast.json, attach it

		if (PDLProductBuilderOaf.use_forecast_json()) {
			PDLProductBuilderOaf.attach_forecast (content_builder, jsonText);
		}

		// Attach the forecast data file

		attach_forecast_data (content_builder, to_json());

		// Get the inline text

		String inlineText = null;
		if (PDLProductBuilderOaf.use_inline_text()) {
			inlineText = jsonText;
		}

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			chosenCode, eventNetwork, eventCode, isReviewed, inlineText, modifiedTime,
			content_builder.make_product_file_array());
	
		return product;
	}




	// Make a PDL product, and return the files created.
	// Parameters:
	//  eventID = The event ID for PDL, which for us identifies the timeline.
	//  isReviewed = Review status, false means automatically generated.
	//  pdl_code = The code to use for the product.
	//  text_files = 3-element array that returns the text files created.
	//               text_files[0] returns contents.xml.
	//               text_files[1] returns forecast.json.
	//               text_files[2] returns forecast_data.json.
	// Returns null if the product cannot be constructed due to missing or empty code.
	// This function always uses the supplied code, and does not delete OAF products with different codes.

	public Product make_pdl_product_with_code (String eventID, boolean isReviewed, String pdl_code, String[] text_files) throws Exception {

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

		// Choose the code to use

		String chosenCode = pdl_code;

		// If no chosen code, return null to indicate conflict

		if (chosenCode == null || chosenCode.isEmpty()) {
			pdl_event_id = "";
			return null;
		}

		// Save the chosen code

		pdl_event_id = chosenCode;

		// The content builder

		PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

		// If we want forecast.json, attach it

		if (PDLProductBuilderOaf.use_forecast_json()) {
			PDLProductBuilderOaf.attach_forecast (content_builder, jsonText);
		}

		// Attach the forecast data file

		String json_data = to_json();

		attach_forecast_data (content_builder, json_data);

		// Get the inline text

		String inlineText = null;
		if (PDLProductBuilderOaf.use_inline_text()) {
			inlineText = jsonText;
		}

		// Build the product

		Product product = PDLProductBuilderOaf.createProduct (
			chosenCode, eventNetwork, eventCode, isReviewed, inlineText, modifiedTime,
			content_builder.make_product_file_array());

		// Return the files

		text_files[0] = content_builder.toString();
		text_files[1] = jsonText;
		text_files[2] = json_data;
	
		return product;
	}




	// Make event-sequence properties.
	// Parameters:
	//  evs_props = Receives the properties.
	//  geojson = GeoJSON for the event.
	// Returns null if success.
	// If error, returns an error message.

	public String make_evseq_properties (PropertiesEventSequence evs_props, JSONObject geojson) {

		ActionConfig evseq_action_config = new ActionConfig();

		// Start by clearing the property object

		evs_props.clear();

		// If no GeoJSON, we cannot proceed

		if (geojson == null) {
			return "Failed to create event-sequence properties because no GeoJSON is available";
		}

		// If no event-sequence parameters, we cannot proceed

		if (!( parameters.evseq_cfg_avail )) {
			return "Failed to create event-sequence properties because no event-sequence parameters are available";
		}

		// If no search region parameters, we cannot proceed

		if (!( parameters.aftershock_search_avail )) {
			return "Failed to create event-sequence properties because no aftershock search region is available";
		}

		// Set properties for mainshock

		if (!( evs_props.set_from_event_gj (geojson) )) {
			return "Failed to set event-sequence properties for mainshock";
		}

		// Set start time

		long start_delta = parameters.evseq_cfg_params.get_evseq_cfg_lookback();

		if (!( evs_props.set_relative_start_time_millis (-start_delta) )) {		// note negative
			return "Failed to set start time for event-sequence properties";
		}

		// Set end time

		long end_delta = parameters.forecast_lag + evseq_action_config.get_evseq_lookahead();

		if (!( evs_props.set_relative_end_time_millis (end_delta) )) {
			return "Failed to set relative end time for event-sequence properties";
		}

		// Set the region

		SphRegion sph_region = parameters.aftershock_search_region;

		if (!( evs_props.set_region_from_sph (sph_region) )) {
			return "Failed to set region for event-sequence properties";
		}

		// Check invariant

		String evs_inv = evs_props.check_invariant();

		if (evs_inv != null) {
			return "Invariant check failed for event-sequence properties: " + evs_inv;
		}

		// success

		return null;
	}




	// Make event-sequence properties.
	// Parameters:
	//  evs_props = Receives the properties.
	//  geojson = GeoJSON for the event.
	//  fcparam = Forecast parameters for the event.
	//  seq_end_time = Sequence end time, in milliseconds, either relative or absolute.
	//  f_rel_end_time = True if end time is relative, false if absolute.
	// Returns null if success.
	// If error, returns an error message.
	// Note: As a special case, if seq_end_time == -1L and f_rel_end_time == true,
	//  then the end time is the default computed from the forecast lag.
	// Note: This is a static function.

	public static String make_evseq_properties (PropertiesEventSequence evs_props, JSONObject geojson,
			ForecastParameters fcparam, long seq_end_time, boolean f_rel_end_time) {

		ActionConfig evseq_action_config = new ActionConfig();

		// Start by clearing the property object

		evs_props.clear();

		// If no GeoJSON, we cannot proceed

		if (geojson == null) {
			return "Failed to create event-sequence properties because no GeoJSON is available";
		}

		// If no parameters, we cannot proceed

		if (fcparam == null) {
			return "Failed to create event-sequence properties because no parameters are available";
		}

		// If no event-sequence parameters, we cannot proceed

		if (!( fcparam.evseq_cfg_avail )) {
			return "Failed to create event-sequence properties because no event-sequence parameters are available";
		}

		// If no search region parameters, we cannot proceed

		if (!( fcparam.aftershock_search_avail )) {
			return "Failed to create event-sequence properties because no aftershock search region is available";
		}

		// Set properties for mainshock

		if (!( evs_props.set_from_event_gj (geojson) )) {
			return "Failed to set event-sequence properties for mainshock";
		}

		// Set start time

		long start_delta = fcparam.evseq_cfg_params.get_evseq_cfg_lookback();

		if (!( evs_props.set_relative_start_time_millis (-start_delta) )) {		// note negative
			return "Failed to set start time for event-sequence properties";
		}

		// Set end time

		if (f_rel_end_time) {
			long end_delta = ( (seq_end_time != -1L) ? seq_end_time : (fcparam.forecast_lag + evseq_action_config.get_evseq_lookahead()) );

			if (!( evs_props.set_relative_end_time_millis (end_delta) )) {
				return "Failed to set relative end time for event-sequence properties";
			}
		} else {
			if (!( evs_props.set_end_time (seq_end_time) )) {
				return "Failed to set absolute end time for event-sequence properties";
			}
		}

		// Set the region

		SphRegion sph_region = fcparam.aftershock_search_region;

		if (!( evs_props.set_region_from_sph (sph_region) )) {
			return "Failed to set region for event-sequence properties";
		}

		// Check invariant

		String evs_inv = evs_props.check_invariant();

		if (evs_inv != null) {
			return "Invariant check failed for event-sequence properties: " + evs_inv;
		}

		// success

		return null;
	}




	// Make event-sequence properties.
	// Parameters:
	//  evs_props = Receives the properties.
	//  geojson = GeoJSON for the event.
	//  evseq_cfg_params = Event-sequence configuration parameters (we use the lookback time).
	//  evseq_region = Region for event-sequence.
	//  seq_end_time = Sequence end time, in milliseconds, either relative or absolute.
	//  f_rel_end_time = True if end time is relative, false if absolute.
	// Returns null if success.
	// If error, returns an error message.
	// Note: This is a static function.

	public static String make_evseq_properties (PropertiesEventSequence evs_props, JSONObject geojson,
			EventSequenceParameters evseq_cfg_params, SphRegion evseq_region, long seq_end_time, boolean f_rel_end_time) {

		ActionConfig evseq_action_config = new ActionConfig();

		// Start by clearing the property object

		evs_props.clear();

		// If no GeoJSON, we cannot proceed

		if (geojson == null) {
			return "Failed to create event-sequence properties because no GeoJSON is available";
		}

		// If no event-sequence parameters, we cannot proceed

		if (evseq_cfg_params == null) {
			return "Failed to create event-sequence properties because no event-sequence parameters are available";
		}

		// If no search region, we cannot proceed

		if (evseq_region == null) {
			return "Failed to create event-sequence properties because no aftershock search region is available";
		}

		// Set properties for mainshock

		if (!( evs_props.set_from_event_gj (geojson) )) {
			return "Failed to set event-sequence properties for mainshock";
		}

		// Set start time

		long start_delta = evseq_cfg_params.get_evseq_cfg_lookback();

		if (!( evs_props.set_relative_start_time_millis (-start_delta) )) {		// note negative
			return "Failed to set start time for event-sequence properties";
		}

		// Set end time

		if (f_rel_end_time) {
			long end_delta = seq_end_time;

			if (!( evs_props.set_relative_end_time_millis (end_delta) )) {
				return "Failed to set relative end time for event-sequence properties";
			}
		} else {
			if (!( evs_props.set_end_time (seq_end_time) )) {
				return "Failed to set absolute end time for event-sequence properties";
			}
		}

		// Set the region

		if (!( evs_props.set_region_from_sph (evseq_region) )) {
			return "Failed to set region for event-sequence properties";
		}

		// Check invariant

		String evs_inv = evs_props.check_invariant();

		if (evs_inv != null) {
			return "Invariant check failed for event-sequence properties: " + evs_inv;
		}

		// success

		return null;
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

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
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

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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
			System.out.println (MarshalUtils.display_valid_json_string (json_string));

			// Read from JSON

			ForecastData fcdata2 = new ForecastData();
			fcdata2.from_json (json_string);

			System.out.println ("");
			System.out.println (fcdata2.toString());

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product and send it to PDL-Development.

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

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Make the PDL product, marked not-reviewed

			Product product;

			try {
				product = fcdata.make_pdl_product (the_event_id, false);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Stop if conflict

			if (product == null) {
				System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  pdl_enable  event_id  isReviewed  [pdl_key_filename]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product and send it to the selected PDL destination.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 3 or 4 additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ForecastData : Invalid 'test4' subcommand");
				return;
			}

			int pdl_enable = Integer.parseInt (args[1]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
			String the_event_id = args[2];
			Boolean isReviewed = Boolean.parseBoolean (args[3]);
			String pdl_key_filename = null;
			if (args.length >= 5) {
				pdl_key_filename = args[4];
			}

			// Set the PDL enable code

			if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
				System.out.println ("Invalid pdl_enable = " + pdl_enable);
				return;
			}

			ServerConfig server_config = new ServerConfig();
			server_config.get_server_config_file().pdl_enable = pdl_enable;

			if (pdl_key_filename != null) {

				if (!( (new File (pdl_key_filename)).canRead() )) {
					System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
					return;
				}

				server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
			}

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Make the PDL product, marked reviewed or not

			Product product;

			try {
				product = fcdata.make_pdl_product (the_event_id, isReviewed);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Stop if conflict

			if (product == null) {
				System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, set friendly format, and display it.
		// Then convert to JSON, and display the JSON.
		// Then read from JSON, and display it (including rebuilt transient data).

		if (args[0].equalsIgnoreCase ("test5")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastData : Invalid 'test5' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Set friendly format

			fcdata.set_friendly_format();

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			// Convert to JSON

			String json_string = fcdata.to_json();

			System.out.println ("");
			System.out.println (MarshalUtils.display_valid_json_string (json_string));

			// Read from JSON

			ForecastData fcdata2 = new ForecastData();
			fcdata2.from_json (json_string);

			System.out.println ("");
			System.out.println (fcdata2.toString());

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  event_id  filename
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then write to JSON file.
		// Then read from JSON file, and display it (including rebuilt transient data).

		if (args[0].equalsIgnoreCase ("test6")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastData : Invalid 'test6' subcommand");
				return;
			}

			String the_event_id = args[1];
			String filename = args[2];

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Write to JSON file

			fcdata.to_json_file (filename);

			// Read from JSON file

			ForecastData fcdata2 = new ForecastData();
			fcdata2.from_json_file (filename);

			System.out.println ("");
			System.out.println (fcdata2.toString());

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  event_id  filename
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, set friendly format, and display it.
		// Then write to JSON file.
		// Then read from JSON file, and display it (including rebuilt transient data).

		if (args[0].equalsIgnoreCase ("test7")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastData : Invalid 'test7' subcommand");
				return;
			}

			String the_event_id = args[1];
			String filename = args[2];

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Set friendly format

			fcdata.set_friendly_format();

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			// Write to JSON file

			fcdata.to_json_file (filename);

			// Read from JSON file

			ForecastData fcdata2 = new ForecastData();
			fcdata2.from_json_file (filename);

			System.out.println ("");
			System.out.println (fcdata2.toString());

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  in_filename  out_filename
		// Convert a JSON file to friendly format.
		// Display the ForecastData object.

		if (args[0].equalsIgnoreCase ("test8")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastData : Invalid 'test8' subcommand");
				return;
			}

			String in_filename = args[1];
			String out_filename = args[2];

			// Convert to friendly format

			ForecastData fcdata = ForecastData.convert_json_file_to_friendly (in_filename, out_filename);

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  in_filename  out_filename
		// Convert a JSON file to standard format.
		// Display the ForecastData object.

		if (args[0].equalsIgnoreCase ("test9")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastData : Invalid 'test9' subcommand");
				return;
			}

			String in_filename = args[1];
			String out_filename = args[2];

			// Convert to standard format

			ForecastData fcdata = ForecastData.convert_json_file_to_standard (in_filename, out_filename);

			// Display them

			System.out.println ("");
			System.out.println (fcdata.toString());

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  event_id  isReviewed  pdl_code  pdl_enable  [pdl_key_filename]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Set the forecast lag to now.
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the event-sequence properties, and display them.
		// Then construct the event-sequence PDL product and send it to the selected PDL destination.
		//
		// The use of this test is to send prototype event-sequence products for development purposes.
		// Event-sequence products can be deleted using PropertiesEventSequence test #8.
		// The event-sequence time range is from 30 days before the mainshock, to 365 days after now.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 4 or 5 additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ForecastData : Invalid 'test10' subcommand");
				return;
			}

			String the_event_id = args[1];
			Boolean isReviewed = Boolean.parseBoolean (args[2]);
			String pdl_code = args[3];
			int pdl_enable = Integer.parseInt (args[4]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
			String pdl_key_filename = null;
			if (args.length >= 6) {
				pdl_key_filename = args[5];
			}

			// Set the PDL enable code

			if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
				System.out.println ("Invalid pdl_enable = " + pdl_enable);
				return;
			}

			ServerConfig server_config = new ServerConfig();
			server_config.get_server_config_file().pdl_enable = pdl_enable;

			if (pdl_key_filename != null) {

				if (!( (new File (pdl_key_filename)).canRead() )) {
					System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
					return;
				}

				server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
			}

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be now

			long the_forecast_lag = System.currentTimeMillis() - fcmain.mainshock_time;

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

			// Make the event-sequence properties

			PropertiesEventSequence evs_props = new PropertiesEventSequence();

			// Set properties for mainshock

			if (!( evs_props.set_from_event_gj (fcmain.mainshock_geojson) )) {
				System.out.println ("Failed to set event-sequence properties for mainshock");
				return;
			}

			// Set start time to 30 days before mainshock

			double start_delta_days = -30.0;

			if (!( evs_props.set_relative_start_time_days (start_delta_days) )) {
				System.out.println ("Failed to set relative start time for event-sequence properties");
				return;
			}

			// Set end time to 365 days after forecast

			double end_delta_days = (((double)(params.forecast_lag)) / SimpleUtils.DAY_MILLIS_D) + 365.0;

			if (!( evs_props.set_relative_end_time_days (end_delta_days) )) {
				System.out.println ("Failed to set relative end time for event-sequence properties");
				return;
			}

			// Set the region

			SphRegion sph_region = params.aftershock_search_region;

			if (!( evs_props.set_region_from_sph (sph_region) )) {
				System.out.println ("Failed to set region for event-sequence properties");
				return;
			}

			// Check invariant

			String evs_inv = evs_props.check_invariant();

			if (evs_inv != null) {
				System.out.println ("Invariant check failed for event-sequence properties: " + evs_inv);
				return;
			}

			// Display the contents

			System.out.println (evs_props.toString());
			System.out.println ("");

			// Display the property map

			System.out.println (evs_props.property_map_to_string (isReviewed));
			System.out.println ("");

			// Make the PDL product

			Map<String, String> extra_properties = new LinkedHashMap<String, String>();
			extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, VersionInfo.get_generator_name());

			String jsonText = null;
			long modifiedTime = 0L;

			Product product;

			try {
				product = PDLProductBuilderEventSequence.createProduct (
					pdl_code, evs_props, extra_properties, isReviewed, jsonText, modifiedTime);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Stop if unable to create product

			if (product == null) {
				System.out.println ("PDLProductBuilderEventSequence.createProduct returned null, indicating unable to create PDL product");
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Subcommand : Test #11
		// Command format:
		//  test11  pdl_enable  event_id  isReviewed  [pdl_key_filename]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product and send it to the selected PDL destination.
		// Same as test #4 except also sends the event-sequence product.

		if (args[0].equalsIgnoreCase ("test11")) {

			// 3 or 4 additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ForecastData : Invalid 'test11' subcommand");
				return;
			}

			int pdl_enable = Integer.parseInt (args[1]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
			String the_event_id = args[2];
			Boolean isReviewed = Boolean.parseBoolean (args[3]);
			String pdl_key_filename = null;
			if (args.length >= 5) {
				pdl_key_filename = args[4];
			}

			// Set the PDL enable code

			ServerConfig.set_opmode (pdl_enable, pdl_key_filename);
			System.out.println (ServerConfig.get_opmode_as_string());

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Make the PDL product, marked reviewed or not

			Product product;
			EventSequenceResult evseq_res = new EventSequenceResult();

			try {
				product = fcdata.make_pdl_product (evseq_res, the_event_id, isReviewed);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Display event-sequence results

			System.out.println ("");
			System.out.println (evseq_res.toString());
			System.out.println ("");

			// Stop if conflict

			if (product == null) {
				System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
				return;
			}

			// Send event-sequence product, if any

			evseq_res.perform_send();

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Subcommand : Test #12
		// Command format:
		//  test12  pdl_enable  event_id  isReviewed  pdl_code  [pdl_key_filename]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product and send it to the selected PDL destination.
		// Same as test #4, except uses the supplied pdl_code and does not delete any products.

		if (args[0].equalsIgnoreCase ("test12")) {

			// 4 or 5 additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ForecastData : Invalid 'test12' subcommand");
				return;
			}

			int pdl_enable = Integer.parseInt (args[1]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
			String the_event_id = args[2];
			Boolean isReviewed = Boolean.parseBoolean (args[3]);
			String pdl_code = args[4];
			String pdl_key_filename = null;
			if (args.length >= 6) {
				pdl_key_filename = args[5];
			}

			// Set the PDL enable code

			ServerConfig.set_opmode (pdl_enable, pdl_key_filename);
			System.out.println (ServerConfig.get_opmode_as_string());

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be 7 days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * 7.0);

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

			// Make the PDL product, marked reviewed or not

			Product product;

			try {
				product = fcdata.make_pdl_product_with_code (the_event_id, isReviewed, pdl_code);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Stop if conflict

			if (product == null) {
				System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			return;
		}




		// Subcommand : Test #13
		// Command format:
		//  test13  pdl_enable  event_id  isReviewed  pdl_code  lag_days  filename_prefix  [pdl_key_filename]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then put everything in a ForecastData object, and display it.
		// Then construct the PDL product and send it to the selected PDL destination.
		// Same as test #12, except with adjustable lag, and writes out the files.
		// Note: This uses the 1-month advisory duration, and no injectable text.

		if (args[0].equalsIgnoreCase ("test13")) {

			// 6 or 7 additional arguments

			if (args.length != 7 && args.length != 8) {
				System.err.println ("ForecastData : Invalid 'test13' subcommand");
				return;
			}

			int pdl_enable = Integer.parseInt (args[1]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
			String the_event_id = args[2];
			Boolean isReviewed = Boolean.parseBoolean (args[3]);
			String pdl_code = args[4];
			double lag_days = Double.parseDouble (args[5]);
			String filename_prefix = args[6];
			String pdl_key_filename = null;
			if (args.length >= 8) {
				pdl_key_filename = args[7];
			}

			// Set the PDL enable code

			ServerConfig.set_opmode (pdl_enable, pdl_key_filename);
			System.out.println (ServerConfig.get_opmode_as_string());

			// Fetch just the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Set the forecast time to be lag_days after the mainshock

			long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * lag_days);

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (the_forecast_lag, fcmain, null);

			// Display them

			System.out.println ("");
			System.out.println (params.toString());

			// Get results

			ForecastResults results = new ForecastResults();

			//results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "NOTE: This is a test, do not use this forecast.", fcmain, params, true);
			results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_MONTH, null, fcmain, params, true);

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

			// Make the PDL product, marked reviewed or not

			Product product;
			String[] text_files = new String[3];

			try {
				product = fcdata.make_pdl_product_with_code (the_event_id, isReviewed, pdl_code, text_files);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// Stop if conflict

			if (product == null) {
				System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
				return;
			}

			// Sign the product

			PDLSender.signProduct(product);

			// Send the product, true means it is text

			PDLSender.sendProduct(product, true);

			// Write the files

			System.out.println ("");

			try {
				byte[] b0 = text_files[0].getBytes();
				SimpleUtils.write_string_as_file (filename_prefix + "_contents.xml", text_files[0]);
				SimpleUtils.write_bytes_as_file (filename_prefix + "_contents_bin.xml", b0);
				System.out.println ("contents.xml string length = " + text_files[0].length() + ", binary length = " + b0.length);

				byte[] b1 = text_files[1].getBytes();
				SimpleUtils.write_string_as_file (filename_prefix + "_forecast.json", text_files[1]);
				SimpleUtils.write_bytes_as_file (filename_prefix + "_forecast_bin.json", b1);
				System.out.println ("forecast.json string length = " + text_files[1].length() + ", binary length = " + b1.length);

				byte[] b2 = text_files[2].getBytes();
				SimpleUtils.write_string_as_file (filename_prefix + "_forecast_data.json", text_files[2]);
				SimpleUtils.write_bytes_as_file (filename_prefix + "_forecast_data_bin.json", b2);
				System.out.println ("forecast_data.json string length = " + text_files[2].length() + ", binary length = " + b2.length);

				PDLSender.dump_product_to_file (filename_prefix + "_product.xml", product, true, false);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ForecastData : Unrecognized subcommand : " + args[0]);
		return;

	}

}
