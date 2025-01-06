package org.opensha.oaf.oetas.env;

import java.util.Map;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEOrigin;

import org.opensha.oaf.oetas.bay.OEBayFactoryParams;

import org.opensha.oaf.rj.USGS_ForecastInfo;

import org.opensha.commons.geo.Location;


// Class to hold information describing an observed catalog being passed in to operational ETAS.
// Author: Michael Barall 05/04/2022.

public class OEtasCatalogInfo implements Marshalable {

	//----- Information -----

	// Catalog magnitude of completeness, for the input catalog.

	public double magCat;

	// Largest magnitude represented in the input catalog.
	// For observed data, this can be the mainshock magnitude.
	// For simulated data, this can be the upper magnitude of the simulatin.
	// If <= magCat, then the ETAS code attempts to determine it from the supplied catalog.

	public double magTop;

	// Helmstetter Parameters.
	// Use capG = 100.0 (== HELM_CAPG_DISABLE) to disable time-dependent magnitude of completeness.

	public double capF;
	public double capG;
	public double capH;

	// Beginning and end of the time range covered by data, in days.
	// Rupture times should be within this time range.
	// Note: The mainshock time does not have to lie within this range.

	public double t_data_begin;
	public double t_data_end;

	// The time at which parameter fitting should begin, in days.
	// Parameter fitting is done over the range from t_fitting to t_data_end.
	// Note: Often t_fitting is the time of the mainshock, which is typically 0.0.

	public double t_fitting;

	// The time at which the forecast should begin, in days.
	// Note: Times must satisfy:
	//  t_data_begin <= t_fitting < t_data_end <= t_forecast

	public double t_forecast;


	//----- Mainshock information -----

	// True if mainshock magnitude is available. [v2]

	public boolean mag_main_avail;

	// Mainshock magnitude (or another magnitude representing the sequence). [v2]

	public double mag_main;

	// True if mainshock location is available. [v2]

	public boolean loc_main_avail;

	// Mainshock location (latitude, longitude, depth) (or another location representing the sequence). [v2]

	public double lat_main;
	public double lon_main;
	public double depth_main;




	//----- Construction -----




	// Clear the mainshock info.

	private void clear_main_info () {
		mag_main_avail	= false;
		mag_main		= OEConstants.NO_MAG_NEG;
		loc_main_avail	= false;
		lat_main		= 0.0;
		lon_main		= 0.0;
		depth_main		= 0.0;
		return;
	}




	// Clear contents.

	public final void clear () {
		magCat			= 0.0;
		magTop			= 0.0;
		capF			= 0.0;
		capG			= 0.0;
		capH			= 0.0;
		t_data_begin	= 0.0;
		t_data_end		= 0.0;
		t_fitting		= 0.0;
		t_forecast		= 0.0;

		clear_main_info();
		return;
	}




	// Default constructor.

	public OEtasCatalogInfo () {
		clear();
	}




	// Set the values.
	// This sets the mainshock magnitude and location to unavailable.

	public final OEtasCatalogInfo set (
		double magCat,
		double magTop,
		double capF,
		double capG,
		double capH,
		double t_data_begin,
		double t_data_end,
		double t_fitting,
		double t_forecast
	) {
		this.magCat			= magCat;
		this.magTop			= magTop;
		this.capF			= capF;
		this.capG			= capG;
		this.capH			= capH;
		this.t_data_begin	= t_data_begin;
		this.t_data_end		= t_data_end;
		this.t_fitting		= t_fitting;
		this.t_forecast		= t_forecast;

		clear_main_info();
		return this;
	}




	// Set the values.
	// This sets the mainshock magnitude and location to available.

	public final OEtasCatalogInfo set (
		double magCat,
		double magTop,
		double capF,
		double capG,
		double capH,
		double t_data_begin,
		double t_data_end,
		double t_fitting,
		double t_forecast,
		double mag_main,
		double lat_main,
		double lon_main,
		double depth_main
	) {
		this.magCat			= magCat;
		this.magTop			= magTop;
		this.capF			= capF;
		this.capG			= capG;
		this.capH			= capH;
		this.t_data_begin	= t_data_begin;
		this.t_data_end		= t_data_end;
		this.t_fitting		= t_fitting;
		this.t_forecast		= t_forecast;

		this.mag_main_avail	= true;
		this.mag_main		= mag_main;
		this.loc_main_avail	= true;
		this.lat_main		= lat_main;
		this.lon_main		= lon_main;
		this.depth_main		= depth_main;

		try {
			Location loc_main = get_loc_main();
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("OEtasCatalogInfo.set: Invalid location: lat = " + this.lat_main + ", lon = " + this.lon_main + ", depth = " + this.depth_main, e);
		}

		return this;
	}




	// Copy the values.

	public final OEtasCatalogInfo copy_from (OEtasCatalogInfo other) {
		this.magCat			= other.magCat;
		this.magTop			= other.magTop;
		this.capF			= other.capF;
		this.capG			= other.capG;
		this.capH			= other.capH;
		this.t_data_begin	= other.t_data_begin;
		this.t_data_end		= other.t_data_end;
		this.t_fitting		= other.t_fitting;
		this.t_forecast		= other.t_forecast;

		this.mag_main_avail	= other.mag_main_avail;
		this.mag_main		= other.mag_main;
		this.loc_main_avail	= other.loc_main_avail;
		this.lat_main		= other.lat_main;
		this.lon_main		= other.lon_main;
		this.depth_main		= other.depth_main;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasCatalogInfo:" + "\n");

		result.append ("magCat = "       + magCat       + "\n");
		result.append ("magTop = "       + magTop       + "\n");
		result.append ("capF = "         + capF         + "\n");
		result.append ("capG = "         + capG         + "\n");
		result.append ("capH = "         + capH         + "\n");
		result.append ("t_data_begin = " + t_data_begin + "\n");
		result.append ("t_data_end = "   + t_data_end   + "\n");
		result.append ("t_fitting = "    + t_fitting    + "\n");
		result.append ("t_forecast = "   + t_forecast   + "\n");

		result.append ("mag_main_avail = " + mag_main_avail + "\n");
		if (mag_main_avail) {
			result.append ("mag_main = " + mag_main + "\n");
		}
		result.append ("loc_main_avail = " + loc_main_avail + "\n");
		if (loc_main_avail) {
			result.append ("lat_main = " + lat_main + "\n");
			result.append ("lon_main = " + lon_main + "\n");
			result.append ("depth_main = " + depth_main + "\n");
		}

		return result.toString();
	}



	//----- Operations -----




	// Get mainshock location as a Location.
	// Caller must check that mainshock location is available.

	public final Location get_loc_main () {
		return new Location (lat_main, lon_main, depth_main);
	}




	// Get mainshock location as a Location, or null if not available..

	public final Location get_loc_main_or_null () {
		if (loc_main_avail) {
			return new Location (lat_main, lon_main, depth_main);
		}
		return null;
	}




	// Get mainshock magnitude, or the special value OEConstants.NO_MAG_NEG if not available.
	// Note: Special value must agree with the unknown value in OEBayFactoryParams.

	public final double get_mag_main_or_special () {
		if (mag_main_avail) {
			return mag_main;
		}
		return OEConstants.NO_MAG_NEG;
	}




	// Get the Bayesian prior factory, using the mainshock info.

	public final OEBayFactoryParams get_bay_factory_params () {
		return new OEBayFactoryParams (get_mag_main_or_special(), get_loc_main_or_null());
	}




	// Make a forecast information object to use for testing.
	// Parameters:
	//  origin = Used to convert times between milliseconds and days.
	//  t_main_day = Time of mainshock, in days.  Typically 0.0 if origin is at mainshock.

	public final USGS_ForecastInfo make_fc_info_for_test (OEOrigin origin, double t_main_days) {

		// Time of the event

		long event_time = origin.convert_time_rel_to_abs (t_main_days);

		// Start time of the forecast

		long start_time = origin.convert_time_rel_to_abs (t_forecast);

		// No rounding of start time

		boolean f_round_start = false;

		// Nominal time of the forecast, we use the end time of the data.

		long result_time = origin.convert_time_rel_to_abs (t_data_end);

		// Advisory lag, we use a default based on data end time relative to mainshock time

		long advisory_lag = USGS_ForecastInfo.ADVISORY_LAG_WEEK;
		if (t_data_end - t_main_days >= 343.0) {
			advisory_lag = USGS_ForecastInfo.ADVISORY_LAG_YEAR;
		}
		else if (t_data_end - t_main_days >= 26.0) {
			advisory_lag = USGS_ForecastInfo.ADVISORY_LAG_MONTH;
		}

		// No injectable text

		String injectable_text = null;

		// Next forecast time unknown.

		long next_forecast_time = 0L;

		// No user parameter map

		Map<String, Object> user_param_map = null;

		// Make the information object

		USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set (
			event_time,
			start_time,
			f_round_start,
			result_time,
			advisory_lag,
			injectable_text,
			next_forecast_time,
			user_param_map
		);

		return fc_info;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 125001;
	private static final int MARSHAL_VER_2 = 125002;

	private static final String M_VERSION_NAME = "OEtasCatalogInfo";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("magCat", magCat);
			writer.marshalDouble ("magTop", magTop);
			writer.marshalDouble ("capF", capF);
			writer.marshalDouble ("capG", capG);
			writer.marshalDouble ("capH", capH);
			writer.marshalDouble ("t_data_begin", t_data_begin);
			writer.marshalDouble ("t_data_end", t_data_end);
			writer.marshalDouble ("t_fitting", t_fitting);
			writer.marshalDouble ("t_forecast", t_forecast);

		}
		break;

		case MARSHAL_VER_2: {

			writer.marshalDouble ("magCat", magCat);
			writer.marshalDouble ("magTop", magTop);
			writer.marshalDouble ("capF", capF);
			writer.marshalDouble ("capG", capG);
			writer.marshalDouble ("capH", capH);
			writer.marshalDouble ("t_data_begin", t_data_begin);
			writer.marshalDouble ("t_data_end", t_data_end);
			writer.marshalDouble ("t_fitting", t_fitting);
			writer.marshalDouble ("t_forecast", t_forecast);

			writer.marshalBoolean ("mag_main_avail", mag_main_avail);
			if (mag_main_avail) {
				writer.marshalDouble ("mag_main", mag_main);
			}

			writer.marshalBoolean ("loc_main_avail", loc_main_avail);
			if (loc_main_avail) {
				writer.marshalDouble ("lat_main", lat_main);
				writer.marshalDouble ("lon_main", lon_main);
				writer.marshalDouble ("depth_main", depth_main);
			}

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			magCat = reader.unmarshalDouble ("magCat");
			magTop = reader.unmarshalDouble ("magTop");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");
			t_data_begin = reader.unmarshalDouble ("t_data_begin");
			t_data_end = reader.unmarshalDouble ("t_data_end");
			t_fitting = reader.unmarshalDouble ("t_fitting");
			t_forecast = reader.unmarshalDouble ("t_forecast");

			clear_main_info();

		}
		break;

		case MARSHAL_VER_2: {

			magCat = reader.unmarshalDouble ("magCat");
			magTop = reader.unmarshalDouble ("magTop");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");
			t_data_begin = reader.unmarshalDouble ("t_data_begin");
			t_data_end = reader.unmarshalDouble ("t_data_end");
			t_fitting = reader.unmarshalDouble ("t_fitting");
			t_forecast = reader.unmarshalDouble ("t_forecast");

			mag_main_avail = reader.unmarshalBoolean ("mag_main_avail");
			if (mag_main_avail) {
				mag_main = reader.unmarshalDouble ("mag_main");
			} else {
				mag_main = OEConstants.NO_MAG_NEG;
			}

			loc_main_avail = reader.unmarshalBoolean ("loc_main_avail");
			if (loc_main_avail) {
				lat_main = reader.unmarshalDouble ("lat_main");
				lon_main = reader.unmarshalDouble ("lon_main");
				depth_main = reader.unmarshalDouble ("depth_main");
				try {
					Location loc_main = get_loc_main();
				}
				catch (Exception e) {
					throw new MarshalException ("OEtasCatalogInfo.do_umarshal: Invalid location: lat = " + lat_main + ", lon = " + lon_main + ", depth = " + depth_main, e);
				}
			} else {
				lat_main = 0.0;
				lon_main = 0.0;
				depth_main = 0.0;
			}

		}
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
	public OEtasCatalogInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEtasCatalogInfo etas_cat_info) {
		etas_cat_info.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEtasCatalogInfo static_unmarshal (MarshalReader reader, String name) {
		return (new OEtasCatalogInfo()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEtasCatalogInfo make_test_value () {
		OEtasCatalogInfo etas_cat_info = new OEtasCatalogInfo();

		etas_cat_info.set (
			3.0,		// magCat
			8.0,		// magTop
			1.00,		// capF
			4.50,		// capG
			0.75,		// capH
			-30.0,		// t_data_begin
			7.0,		// t_data_end
			0.0,		// t_fitting
			7.02		// t_forecast
		);

		return etas_cat_info;
	}




	// Make a value to use for testing purposes, including mainshock info.
	// (Magnitude and location for Ridgecrest earthquake.)

	public static OEtasCatalogInfo make_test_value_2 () {
		OEtasCatalogInfo etas_cat_info = new OEtasCatalogInfo();

		etas_cat_info.set (
			3.0,		// magCat
			8.0,		// magTop
			1.00,		// capF
			4.50,		// capG
			0.75,		// capH
			-30.0,		// t_data_begin
			7.0,		// t_data_end
			0.0,		// t_fitting
			7.02,		// t_forecast
			7.1,		// mag_main
			35.770,		// lat_main
			-117.599,	// lon_main
			8.0			// depth_main
		);

		return etas_cat_info;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasCatalogInfo");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying catalog info");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (etas_cat_info.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_json_string (etas_cat_info);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEtasCatalogInfo etas_cat_info2 = new OEtasCatalogInfo();
			MarshalUtils.from_json_string (etas_cat_info2, json_string);

			// Display the contents

			System.out.println (etas_cat_info2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEtasCatalogInfo etas_cat_info3 = new OEtasCatalogInfo();
			etas_cat_info3.copy_from (etas_cat_info2);

			// Display the contents

			System.out.println (etas_cat_info3.toString());

			// Bayesian prior factory parameters

			System.out.println ();
			System.out.println ("********** Bayesian prior factory parameters **********");
			System.out.println ();

			System.out.println (etas_cat_info.get_bay_factory_params().toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename
		// Construct test values, and write them to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Writing test values to a file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_json_string (etas_cat_info);

			// Write the file

			SimpleUtils.write_string_as_file (filename, formatted_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename
		// Construct test values, and write them to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Writing test values to a file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value();

			// Write to file

			MarshalUtils.to_json_file (etas_cat_info, filename);

			// Read back the file and display it

			OEtasCatalogInfo etas_cat_info2 = new OEtasCatalogInfo();
			MarshalUtils.from_json_file (etas_cat_info2, filename);

			System.out.println ();
			System.out.println (etas_cat_info2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename
		// Construct typical parameters, and write them to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Writing test values to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value();

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_cat_info, filename);

			// Read back the file and display it

			OEtasCatalogInfo etas_cat_info2 = new OEtasCatalogInfo();
			MarshalUtils.from_json_file (etas_cat_info2, filename);

			System.out.println ();
			System.out.println (etas_cat_info2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Construct test values, and display it.
		// Make a forecast information object, and display it.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Create forecast info from catalog info");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (etas_cat_info.toString());

			// Make forecast information

			System.out.println ();
			System.out.println ("********** Forecast Info Display **********");
			System.out.println ();

			long origin_time = SimpleUtils.string_to_time ("2000-01-01T00:00:00Z");
			OEOrigin origin = new OEOrigin (origin_time, 0.0, 0.0, 0.0);

			double t_main_days = 0.0;

			USGS_ForecastInfo fc_info = etas_cat_info.make_fc_info_for_test (origin, t_main_days);

			System.out.println (fc_info.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Same as test #1 except includes mainshock info.

		if (testargs.is_test ("test6")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying catalog info");
			testargs.end_test();

			// Create the values

			OEtasCatalogInfo etas_cat_info = make_test_value_2();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (etas_cat_info.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_json_string (etas_cat_info);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEtasCatalogInfo etas_cat_info2 = new OEtasCatalogInfo();
			MarshalUtils.from_json_string (etas_cat_info2, json_string);

			// Display the contents

			System.out.println (etas_cat_info2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEtasCatalogInfo etas_cat_info3 = new OEtasCatalogInfo();
			etas_cat_info3.copy_from (etas_cat_info2);

			// Display the contents

			System.out.println (etas_cat_info3.toString());

			// Bayesian prior factory parameters

			System.out.println ();
			System.out.println ("********** Bayesian prior factory parameters **********");
			System.out.println ();

			System.out.println (etas_cat_info.get_bay_factory_params().toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
