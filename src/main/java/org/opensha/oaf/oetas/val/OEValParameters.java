package org.opensha.oaf.oetas.val;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.util.TestArgs;

import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;


// Class to hold parameters for a validation run.
// Author: Michael Barall.

public class OEValParameters implements Marshalable {

	//----- Parameters -----


	// Start and end times for validation testing, in milliseconds since the epoch.

	private long val_start_time;

	private long val_end_time;

	public final long get_val_start_time () {
		return val_start_time;
	}

	public final long get_val_end_time () {
		return val_end_time;
	}


	// Region for validation testing.

	private SphRegion val_region;

	public final SphRegion get_val_region () {
		return val_region;
	}


	// Forecast lags to use for computing forecasts.

	private long[] val_forecast_lags;

	public final int get_val_forecast_lags_length () {
		return val_forecast_lags.length;
	}

	public final long get_val_forecast_lag (int i) {
		return val_forecast_lags[i];
	}




	//----- Construction -----




	// Get the default region for validation testing.

	public final SphRegion get_def_val_region () {
		return SphRegion.makeWorld();
	}




	// Get the default list of forecast lags for validation testing.
	// The returned array is newly-allocated.

	public final long[] get_def_val_forecast_lags () {
		long[] lags = new long[] {
			SimpleUtils.string_to_duration ("P0DT0H10M0S"),
			SimpleUtils.string_to_duration ("P0DT2H0M0S"),
			SimpleUtils.string_to_duration ("P0DT6H0M0S"),
			SimpleUtils.string_to_duration ("P1D"),
			SimpleUtils.string_to_duration ("P2D"),
			SimpleUtils.string_to_duration ("P4D"),
			SimpleUtils.string_to_duration ("P8D"),
			SimpleUtils.string_to_duration ("P16D"),
			SimpleUtils.string_to_duration ("P35D"),
			SimpleUtils.string_to_duration ("P70D"),
			SimpleUtils.string_to_duration ("P140D"),
			SimpleUtils.string_to_duration ("P280D")
		};
		return lags;
	}




	// Clear to default values.

	public final void clear () {
		val_start_time = 0L;
		val_end_time = 0L;
		val_region = null;
		val_forecast_lags = null;
		return;
	}




	// Default constructor, sets up default values.

	public OEValParameters () {
		clear();
	}




	// Constructor that sets the supplied values.
	// The region can be null, in which case the default (world) region is supplied.
	// The forecast_lags can be null or empty, in which case the default forecast lags are supplied.
	// If forecast_lags is supplied, it is copied.

	public OEValParameters (
		long start_time,
		long end_time,
		SphRegion region,
		long[] forecast_lags
	) {
		this.val_start_time = start_time;
		this.val_end_time = end_time;
		this.val_region = ((region == null) ? get_def_val_region() : region);
		this.val_forecast_lags = ((forecast_lags == null || forecast_lags.length == 0) ? get_def_val_forecast_lags() : forecast_lags.clone());
	}




	// Set up the options with the supplied values.
	// The region can be null, in which case the default (world) region is supplied.
	// The forecast_lags can be null or empty, in which case the default forecast lags are supplied.
	// If forecast_lags is supplied, it is copied.
	// Returns this object.

	public final OEValParameters set (
		long start_time,
		long end_time,
		SphRegion region,
		long[] forecast_lags
	) {
		this.val_start_time = start_time;
		this.val_end_time = end_time;
		this.val_region = ((region == null) ? get_def_val_region() : region);
		this.val_forecast_lags = ((forecast_lags == null || forecast_lags.length == 0) ? get_def_val_forecast_lags() : forecast_lags.clone());
		return this;
	}




	// Copy values from another object.
	// Returns this object.

	public final OEValParameters copy_from (OEValParameters other) {
		this.val_start_time = other.val_start_time;
		this.val_end_time = other.val_end_time;
		this.val_region = other.val_region;
		this.val_forecast_lags = ((other.val_forecast_lags == null) ? other.val_forecast_lags : other.val_forecast_lags.clone());
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEValParameters:" + "\n");

		result.append ("val_start_time = " + SimpleUtils.time_raw_and_parseable_string (val_start_time) + "\n");
		result.append ("val_end_time = " + SimpleUtils.time_raw_and_parseable_string (val_end_time) + "\n");
		result.append ("val_region = " + val_region.toString() + "\n");
		result.append ("val_forecast_lags = [" + "\n");
		for (int i = 0; i < get_val_forecast_lags_length(); ++i) {
			result.append ("  " + i + ": " + SimpleUtils.duration_raw_and_string_2 (get_val_forecast_lag (i)) + "\n");
		}
		result.append ("]" + "\n");

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 154001;

	private static final String M_VERSION_NAME = "OEValParameters";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalTime ("val_start_time", val_start_time);
			writer.marshalTime ("val_end_time", val_end_time);
			writer.marshalObject ("val_region", val_region, SphRegion::marshal_poly);
			writer.marshalDurationArray ("val_forecast_lags", val_forecast_lags);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			val_start_time = reader.unmarshalTime ("val_start_time");
			val_end_time = reader.unmarshalTime ("val_end_time");
			val_region = reader.unmarshalObject ("val_region", SphRegion::unmarshal_poly);
			val_forecast_lags = reader.unmarshalDurationArray ("val_forecast_lags");

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
	public OEValParameters unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEValParameters obj) {
		obj.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEValParameters static_unmarshal (MarshalReader reader, String name) {
		return (new OEValParameters()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes - Default value.

	public static OEValParameters make_test_value_1 () {
		OEValParameters val_params = new OEValParameters();

		val_params.set (
			SimpleUtils.string_to_time ("1981-01-01T00:00:00Z"),
			SimpleUtils.string_to_time ("2024-01-01T00:00:00Z"),
			null,
			null
		);

		return val_params;
	}




	// Make a value to use for testing purposes - Non-default value.

	public static OEValParameters make_test_value_2 () {
		OEValParameters val_params = new OEValParameters();

		val_params.set (
			SimpleUtils.string_to_time ("2011-12-03T10:15:30Z"),
			SimpleUtils.string_to_time ("2014-12-03T10:15:30.765Z"),
			SphRegion.makeCircle (new SphLatLon (35.0, 120.0), 200.0),
			new long[] {
				SimpleUtils.string_to_duration ("P10D"),
				SimpleUtils.string_to_duration ("P20DT06H12M24S"),
				SimpleUtils.string_to_duration ("P40DT06H12M24.543S"),
				SimpleUtils.string_to_duration ("P80D"),
			}
		);

		return val_params;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEValParameters");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// This uses default options.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying validation parameters");
			testargs.end_test();

			// Create the values

			OEValParameters val_params = make_test_value_1();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Parameter Display **********");
			System.out.println ();

			System.out.println (val_params.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (val_params);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (val_params);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEValParameters val_params2 = new OEValParameters();
			MarshalUtils.from_json_string (val_params2, json_string);

			// Display the contents

			System.out.println (val_params2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEValParameters val_params3 = new OEValParameters();
			val_params3.copy_from (val_params2);

			// Display the contents

			System.out.println (val_params3.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// This uses non-default options.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying validation parameters");
			testargs.end_test();

			// Create the values

			OEValParameters val_params = make_test_value_2();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Parameter Display **********");
			System.out.println ();

			System.out.println (val_params.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (val_params);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (val_params);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEValParameters val_params2 = new OEValParameters();
			MarshalUtils.from_json_string (val_params2, json_string);

			// Display the contents

			System.out.println (val_params2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEValParameters val_params3 = new OEValParameters();
			val_params3.copy_from (val_params2);

			// Display the contents

			System.out.println (val_params3.toString());

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
