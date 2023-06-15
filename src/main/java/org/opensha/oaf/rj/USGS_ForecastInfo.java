package org.opensha.oaf.rj;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import java.time.Instant;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


// USGS_ForecastInfo encapsulates the information needed to produce a forecast,
// other than the information in the forecast model.
// Author: Michael Barall 06/06/2023.

public class USGS_ForecastInfo implements Marshalable {

	//----- Constants -----

	// Standard values of the advisory duration.

	public static final long ADVISORY_LAG_DAY   = 86400000L;	// 1 day
	public static final long ADVISORY_LAG_WEEK  = 604800000L;	// 1 week = 7 days
	public static final long ADVISORY_LAG_MONTH = 2592000000L;	// 1 month = 30 days
	public static final long ADVISORY_LAG_YEAR  = 31536000000L;	// 1 year = 365 days

	// Minimum start time of the forecast, relative to the mainshock time.

	public static final long MIN_START_LAG = 1000L;				// 1 second




	//----- Information -----

	// Time of the event, in milliseconds since the epoch.
	// Typically this is the origin time of the mainshock.

	public long event_time;

	// Start time of the forecast, in milliseconds since the epoch.
	// Must be after the time of the event.
	// Typically this is the origin time of the mainshock plus the forecast lag,
	// but must be >= event_time + MIN_START_LAG.

	public long start_time;

	// Flag which indicates if the start time should be rounded.
	// True indicates that the start time should be advanced to the next "round" time,
	// which depends in the difference between start and event time.
	// False indicates the start time should be used exactly as given.

	public boolean f_round_start;

	// Nominal time of the forecast, in milliseconds since the epoch.
	// When building JSON, this is used as the creation time of the forecast.
	// Typically this is the origin time of the mainshock plus the forecast lag.
	// Typically this is also the end time of the data (aftershock sequence).

	public long result_time;

	// The duration of the advisory to insert into the forecast, in milliseconds.
	// Typically this is one of the ADVISORY_LAG_XXX values.

	public long advisory_lag;

	// Injectable text to insert into the forecast.
	// Must be non-null.  Can be an empty string if there is no injectable text.

	public String injectable_text;

	// Time of the next scheduled forecast, in milliseconds since the epoch.
	// Can be 0L if unknown, or -1L if no more forecasts are scheduled

	public long next_forecast_time;

	// Additional model parameters to insert into the forecast.  Must be non-null.
	// Each value must be one of:  Integer, Long, Float, Double, Boolean, or String.
	// Parameters appear in the order of the iterator of the map, so typically it is LinkedHashMap.

	public Map<String, Object> user_param_map;




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		event_time = 0L;
		start_time = 0L;
		f_round_start = true;
		result_time = 0L;
		advisory_lag = 0L;
		injectable_text = "";
		next_forecast_time = 0L;
		user_param_map = new LinkedHashMap<String, Object>();
		return;
	}




	// Default constructor.

	public USGS_ForecastInfo () {
		clear();
	}




	// Set the values.
	// This allows injectable_text == null and user_param_map == null.

	public final USGS_ForecastInfo set (
		long event_time,
		long start_time,
		boolean f_round_start,
		long result_time,
		long advisory_lag,
		String injectable_text,
		long next_forecast_time,
		Map<String, Object> user_param_map
	) {
		this.event_time = event_time;
		this.start_time = start_time;
		this.f_round_start = f_round_start;
		this.result_time = result_time;
		this.advisory_lag = advisory_lag;
		if (injectable_text == null) {
			this.injectable_text = "";
		} else {
			this.injectable_text = injectable_text;
		}
		this.next_forecast_time = next_forecast_time;
		if (user_param_map == null) {
			this.user_param_map = new LinkedHashMap<String, Object>();
		} else {
			this.user_param_map = new LinkedHashMap<String, Object> (user_param_map);
		}
		return this;
	}




	// Set the values for a typical application.
	// Parameters:
	//  event_time = Time of the mainshock, in milliseconds since the epoch.
	//  result_time = Nominal time of the forecast, in milliseconds since the epoch.
	//                Typically the mainshock time plus forecast lag; also the end time of the data.
	//  advisory_lag = The duration of the advisory to insert into the forecast, in milliseconds.  Typically ADVISORY_LAG_XXX.
	//  injectable_text = Injectable text to insert into the forecast.  Can be null or empty if none.
	//  next_scheduled_lag = Time of next forecast relative to mainshock, or 0 if unknown, or negative if no more forecasts.
	//  user_param_map = Additional model parameters to insert into the forecast.  Can be null or empty if none.
	// Note: After this call, start_time contains the rounded start time of the forecast, and f_round_start == false.

	public final USGS_ForecastInfo set_typical (
		long event_time,
		long result_time,
		long advisory_lag,
		String injectable_text,
		long next_scheduled_lag,
		Map<String, Object> user_param_map
	) {
		this.event_time = event_time;
		this.start_time = make_rounded_start_time (event_time, result_time);
		this.f_round_start = false;
		this.result_time = result_time;
		this.advisory_lag = advisory_lag;
		if (injectable_text == null) {
			this.injectable_text = "";
		} else {
			this.injectable_text = injectable_text;
		}
		this.next_forecast_time = convert_next_scheduled_lag (event_time, next_scheduled_lag);
		if (user_param_map == null) {
			this.user_param_map = new LinkedHashMap<String, Object>();
		} else {
			this.user_param_map = new LinkedHashMap<String, Object> (user_param_map);
		}
		return this;
	}




	// Copy the values.

	public final USGS_ForecastInfo copy_from (USGS_ForecastInfo other) {
		this.event_time = other.event_time;
		this.start_time = other.start_time;
		this.f_round_start = other.f_round_start;
		this.result_time = other.result_time;
		this.advisory_lag = other.advisory_lag;
		this.injectable_text = other.injectable_text;
		this.next_forecast_time = other.next_forecast_time;
		this.user_param_map = new LinkedHashMap<String, Object> (other.user_param_map);
		return this;
	}




	// Add a user parameter.
	// The value must be one of:  Integer, Long, Float, Double, Boolean, or String.

	public final USGS_ForecastInfo add_user_param (String param, Object value) {
		user_param_map.put (param, value);
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("USGS_ForecastInfo:" + "\n");

		result.append ("event_time = "         + SimpleUtils.time_raw_and_string (event_time) + "\n");
		result.append ("start_time = "         + SimpleUtils.time_raw_and_string (start_time) + "\n");
		result.append ("f_round_start = "      + f_round_start + "\n");
		result.append ("result_time = "        + SimpleUtils.time_raw_and_string (result_time) + "\n");
		result.append ("advisory_lag = "       + SimpleUtils.duration_raw_and_string (advisory_lag) + "\n");
		result.append ("injectable_text = "    + "\"" + injectable_text + "\"" + "\n");
		result.append ("next_forecast_time = " + SimpleUtils.time_raw_and_string (next_forecast_time) + "\n");
		result.append ("user_param_map = [" + "\n");
		for (Map.Entry<String, Object> entry : user_param_map.entrySet()) {
			result.append (entry.getKey() + " = " + entry.getValue().toString() + "\n");
		}
		result.append ("]" + "\n");

		return result.toString();
	}




	//----- Usage -----




	// Convert a lag for the next forecast into a next forecast time.
	// Parameters:
	//  the_event_time = Mainshock origin time, in milliseconds since the epoch.
	//  next_scheduled_lag = Time of next forecast relative to mainshock, or 0 if unknown, or negative if no more forecasts.
	// Returns the time of the next forecast, in milliseconds since the epoch.

	public static long convert_next_scheduled_lag (long the_event_time, long next_scheduled_lag) {
		if (next_scheduled_lag > 0L) {
			return (the_event_time + next_scheduled_lag);
		}
		if (next_scheduled_lag < 0L) {
			return -1L;
		}
		return 0L;
	}




	// Make an unrounded start time, given a nominal forecast time.
	// Parameters:
	//  the_event_time = Mainshock origin time, in milliseconds since the epoch.
	//  the_result_time = Nominal time of the forecast, in milliseconds since the epoch.
	// Returns the start time of the forecast, not advanced to a "round" time.
	// Note: The result is equal to the_result_time, coerced to be >= event_time + MIN_START_LAG.

	public static long make_unrounded_start_time (long the_event_time, long the_result_time) {
		return Math.max (the_result_time, the_event_time + MIN_START_LAG);
	}




	// Make a rounded start time, given a nominal forecast time.
	// Parameters:
	//  the_event_time = Mainshock origin time, in milliseconds since the epoch.
	//  the_result_time = Nominal time of the forecast, in milliseconds since the epoch.
	// Returns the start time of the forecast, rounded to a "round" time.
	// Note: The function first computes the unrounded start time as above,
	// then advances it to the next "round" time.

	public static long make_rounded_start_time (long the_event_time, long the_result_time) {
		long unrounded_start_time = make_unrounded_start_time (the_event_time, the_result_time);
		return USGS_AftershockForecast.sdround_millis (the_event_time, unrounded_start_time);
	}




	// Make the forecast.
	// Parameters:
	//  model = Aftershock model.
	//  aftershocks = List of aftershocks (and foreshocks).
	//  extra_param_map = Extra parameters to insert in forecast, can be null or empty if none.
	// Returns the forecast object.

	public final USGS_AftershockForecast make_forecast (USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, Map<String, Object> extra_param_map) {

		Instant eventDate = Instant.ofEpochMilli (event_time);
		Instant startDate = Instant.ofEpochMilli (start_time);
		USGS_AftershockForecast forecast = new USGS_AftershockForecast (model, aftershocks, eventDate, startDate, f_round_start);

		// Advisory duration

		if (advisory_lag >= ADVISORY_LAG_YEAR) {
			forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_YEAR);
		} else if (advisory_lag >= ADVISORY_LAG_MONTH) {
			forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_MONTH);
		} else if (advisory_lag >= ADVISORY_LAG_WEEK) {
			forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_WEEK);
		} else {
			forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_DAY);
		}

		// Injectable text

		String the_injectable_text = injectable_text;
		if (the_injectable_text.length() == 0) {
			the_injectable_text = null;		// convention for USGS_AftershockForecast
		}
		forecast.setInjectableText (the_injectable_text);

		// Next forecast time

		forecast.setNextForecastMillis (next_forecast_time);

		// Additional parameters

		LinkedHashMap<String, Object> my_param_map = new LinkedHashMap<String, Object> (user_param_map);
		if (extra_param_map != null) {
			my_param_map.putAll (extra_param_map);
		}
		forecast.setUserParamMap (my_param_map);

		return forecast;
	}




	// Make the forecast JSON.
	// Parameters:
	//  model = Aftershock model.
	//  aftershocks = List of aftershocks (and foreshocks).
	// Returns the forecast JSON as a string.
	// Returns null if the forecast JSON could not be constructed.

	public final String make_forecast_json (USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, Map<String, Object> extra_param_map) {
		USGS_AftershockForecast forecast = make_forecast (model, aftershocks, extra_param_map);
		return forecast.buildJSONString (result_time);
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 123001;

	private static final String M_VERSION_NAME = "USGS_ForecastInfo";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalLong ("event_time", event_time);
			writer.marshalLong ("start_time", start_time);
			writer.marshalBoolean ("f_round_start", f_round_start);
			writer.marshalLong ("result_time", result_time);
			writer.marshalLong ("advisory_lag", advisory_lag);
			writer.marshalString ("injectable_text", injectable_text);
			writer.marshalLong ("next_forecast_time", next_forecast_time);

			MarshalUtils.marshalMapStringObject (writer, "user_param_map", user_param_map);

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

			event_time = reader.unmarshalLong ("event_time");
			start_time = reader.unmarshalLong ("start_time");
			f_round_start = reader.unmarshalBoolean ("f_round_start");
			result_time = reader.unmarshalLong ("result_time");
			advisory_lag = reader.unmarshalLong ("advisory_lag");
			injectable_text = reader.unmarshalString ("injectable_text");
			next_forecast_time = reader.unmarshalLong ("next_forecast_time");

			user_param_map = new LinkedHashMap<String, Object>();
			MarshalUtils.unmarshalMapStringObject (reader, "user_param_map", user_param_map);

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
	public USGS_ForecastInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, USGS_ForecastInfo fc_info) {
		fc_info.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static USGS_ForecastInfo static_unmarshal (MarshalReader reader, String name) {
		return (new USGS_ForecastInfo()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	private static USGS_ForecastInfo make_test_value () {
		USGS_ForecastInfo fc_info = new USGS_ForecastInfo();

		LinkedHashMap<String, Object> my_user_param_map = new LinkedHashMap<String, Object>();
		my_user_param_map.put ("param1", "value1");
		my_user_param_map.put ("param2", "value2");
		my_user_param_map.put ("param3", 3);
		my_user_param_map.put ("param4", 4.567);
		my_user_param_map.put ("param5", true);

		fc_info.set (
			SimpleUtils.string_to_time ("2011-12-02T10:15:30Z"),	// event_time
			SimpleUtils.string_to_time ("2011-12-07T10:20:00Z"),	// start_time
			true,													// f_round_start
			SimpleUtils.string_to_time ("2011-12-07T10:15:30Z"),	// result_time
			ADVISORY_LAG_WEEK,										// advisory_lag
			"Test values for forecast info",						// injectable_text
			SimpleUtils.string_to_time ("2011-12-08T10:15:30Z"),	// next_forecast_time
			my_user_param_map										// user_param_map
		);

		return fc_info;
	}




	// Make a value to use for testing purposes, using the set_typical().

	private static USGS_ForecastInfo make_test_value_v2 () {
		USGS_ForecastInfo fc_info = new USGS_ForecastInfo();

		LinkedHashMap<String, Object> my_user_param_map = new LinkedHashMap<String, Object>();
		my_user_param_map.put ("param-1", "value-1");
		my_user_param_map.put ("param-2", "value-2");
		my_user_param_map.put ("param-3", -17);
		my_user_param_map.put ("param-4", -98.765);
		my_user_param_map.put ("param-5", false);

		fc_info.set_typical (
			SimpleUtils.string_to_time ("2011-12-04T10:15:30Z"),	// event_time
			SimpleUtils.string_to_time ("2011-12-09T10:15:30Z"),	// result_time
			ADVISORY_LAG_MONTH,										// advisory_lag
			"Test values version 2 for forecast info",				// injectable_text
			86400L * 1000L * 11L,									// next_scheduled_lag
			my_user_param_map										// user_param_map
		);

		return fc_info;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "USGS_ForecastInfo");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying forecast info");
			testargs.end_test();

			// Create the values

			USGS_ForecastInfo fc_info = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Forecast Info Display **********");
			System.out.println ();

			System.out.println (fc_info.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_json_string (fc_info);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			USGS_ForecastInfo fc_info2 = new USGS_ForecastInfo();
			MarshalUtils.from_json_string (fc_info2, json_string);

			// Display the contents

			System.out.println (fc_info2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			USGS_ForecastInfo fc_info3 = new USGS_ForecastInfo();
			fc_info3.copy_from (fc_info2);

			// Add a parameter

			fc_info3.add_user_param ("original", false);

			System.out.println (fc_info3.toString());

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

			USGS_ForecastInfo fc_info = make_test_value();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_json_string (fc_info);

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

			USGS_ForecastInfo fc_info = make_test_value();

			// Write to file

			MarshalUtils.to_json_file (fc_info, filename);

			// Read back the file and display it

			USGS_ForecastInfo fc_info2 = new USGS_ForecastInfo();
			MarshalUtils.from_json_file (fc_info2, filename);

			System.out.println ();
			System.out.println (fc_info2.toString());

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

			USGS_ForecastInfo fc_info = make_test_value();

			// Write to file

			MarshalUtils.to_formatted_json_file (fc_info, filename);

			// Read back the file and display it

			USGS_ForecastInfo fc_info2 = new USGS_ForecastInfo();
			MarshalUtils.from_json_file (fc_info2, filename);

			System.out.println ();
			System.out.println (fc_info2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Same as test #1 except uses set_typical().

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying forecast info");
			testargs.end_test();

			// Create the values

			USGS_ForecastInfo fc_info = make_test_value_v2();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Forecast Info Display **********");
			System.out.println ();

			System.out.println (fc_info.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_json_string (fc_info);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			USGS_ForecastInfo fc_info2 = new USGS_ForecastInfo();
			MarshalUtils.from_json_string (fc_info2, json_string);

			// Display the contents

			System.out.println (fc_info2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			USGS_ForecastInfo fc_info3 = new USGS_ForecastInfo();
			fc_info3.copy_from (fc_info2);

			// Add a parameter

			fc_info3.add_user_param ("original", false);

			System.out.println (fc_info3.toString());

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
