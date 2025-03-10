package org.opensha.oaf.rj;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Arrays;

//import java.time.Instant;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

//import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


// USGS_ForecastHolder holds the contents of a forecast.json file.
// Author: Michael Barall.
//
// This class is capable of reading and writing the forecast.json file,
// and storing its contents.
//
// The marshaling code in this class uses extended JSON functions, and so
// can only read or write to JSON format.
//
// For now, USGS_AftershockForecast is used for initial creation of a
// forecast.json file.  Note that USGS_AftershockForecast can write, but
// cannot read, the forecast.json file.

public class USGS_ForecastHolder implements Marshalable {

	//----- Configuration -----

	// OAF product specification versions.

	public static final int SPEC_VER_1 = 1;	// Specification version 1
	public static final int SPEC_VER_2 = 2;	// Specification version 2

	// Specification version used by this object.
	// Note: This value is not marshaled, but controls what version the marshaling
	// code expects to receive or produces.
	// Note: This only affect marshaling, it is not to be used to select which
	// fields in the object (and sub-objects) contain valid data.

	public int spec_ver = SPEC_VER_2;




	//----- Contained classes -----




	// C_observation - Holds one observation.

	public static class C_observation {

		// Magnitude.

		public double magnitude;

		// Count of observed aftershocks.

		public int count;

		// Clear.

		public final void clear () {
			magnitude = 0.0;
			count = 0;
			return;
		}

		// Constructor.

		public C_observation () {
			clear();
		}

		// Set.

		public final C_observation set (
			double magnitude,
			int count
		) {
			this.magnitude = magnitude;
			this.count = count;
			return this;
		}

		// Copy.

		public final C_observation copy_from (C_observation other) {
			return set (
				other.magnitude,
				other.count
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalDouble ("magnitude", magnitude);
			writer.marshalInt ("count", count);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_observation unmarshal (MarshalReader reader, String name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (name, keys);
			magnitude = reader.unmarshalDouble ("magnitude");
			count = reader.unmarshalInt ("count", 0);
			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	// C_model - Holds the model.

	public static class C_model {

		// Name of model.

		public String name;

		// Reference URL.

		public String reference;

		// Key/value pairs for model parameters.
		// Each value must be one of: Integer, Long, Float, Double, Boolean, String.
		// Note that when unmarshaled, for numeric values, the type may be different
		// from the type that was marshaled, but the numeric value is the same.

		public LinkedHashMap<String, Object> parameters;

		// Clear.

		public final void clear () {
			name = "";
			reference = "";
			parameters = new LinkedHashMap<String, Object>();
			return;
		}

		// Constructor.

		public C_model () {
			clear();
		}

		// Set.

		public final C_model set (
			String name,
			String reference,
			Map<String, Object> parameters
		) {
			this.name = name;
			this.reference = reference;
			this.parameters = new LinkedHashMap<String, Object>();
			if (parameters != null) {
				this.parameters.putAll (parameters);
			}
			return this;
		}

		// Copy.

		public final C_model copy_from (C_model other) {
			return set (
				other.name,
				other.reference,
				other.parameters
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String m_name, int ver) {
			writer.marshalMapBegin (m_name);
			writer.marshalString ("name", name);
			writer.marshalString ("reference", reference);

			writer.marshalMapBegin ("parameters");
			for (Map.Entry<String, Object> entry : parameters.entrySet()) {
				writer.marshalJsonScalar (entry.getKey(), entry.getValue());
			}
			writer.marshalMapEnd ();

			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_model unmarshal (MarshalReader reader, String m_name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (m_name, keys);
			name = reader.unmarshalString ("name");
			reference = reader.unmarshalString ("reference");

			LinkedHashSet<String> param_keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin ("parameters", param_keys);
			parameters = new LinkedHashMap<String, Object>();
			for (String key : param_keys) {
				parameters.put (key, reader.unmarshalJsonScalarNonNull (key));
			}
			reader.unmarshalJsonMapEnd (true);

			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	// C_bin - Holds one magnitude bin for a forecast.

	public static class C_bin {

		// Magnitude.

		public double magnitude;

		// Lower limit of 95% confidence interval.

		public int p95minimum;

		// Upper limit of 95% confidence interval.

		public int p95maximum;

		// Probability of at least one aftershock.

		public double probability;

		// Median number aftershocks, or -1 if not specified.

		public int median;

		public boolean has_median () {
			return median >= 0;
		}

		// Probability distribution for number of aftershocks, or null if not specified.

		public int[] fractileValues;

		public boolean has_fractileValues () {
			return fractileValues != null;
		}

		// Percentage values for the bar graph, or null if not specified.

		public int[] barPercentages;

		public boolean has_barPercentages () {
			return barPercentages != null;
		}

		// Clear.

		public final void clear () {
			magnitude = 0.0;
			p95minimum = 0;
			p95maximum = 0;
			probability = 0.0;
			median = -1;
			fractileValues = null;
			barPercentages = null;
			return;
		}

		// Constructor.

		public C_bin () {
			clear();
		}

		// Set.

		public final C_bin set (
			double magnitude,
			int p95minimum,
			int p95maximum,
			double probability,
			int median,
			int[] fractileValues,
			int[] barPercentages
		) {
			this.magnitude = magnitude;
			this.p95minimum = p95minimum;
			this.p95maximum = p95maximum;
			this.probability = probability;
			this.median = median;
			if (fractileValues == null) {
				this.fractileValues = null;
			} else {
				this.fractileValues = Arrays.copyOf (fractileValues, fractileValues.length);
			}
			if (barPercentages == null) {
				this.barPercentages = null;
			} else {
				this.barPercentages = Arrays.copyOf (barPercentages, barPercentages.length);
			}
			return this;
		}

		// Copy.

		public final C_bin copy_from (C_bin other) {
			return set (
				other.magnitude,
				other.p95minimum,
				other.p95maximum,
				other.probability,
				other.median,
				other.fractileValues,
				other.barPercentages
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalDouble ("magnitude", magnitude);
			writer.marshalInt ("p95minimum", p95minimum);
			writer.marshalInt ("p95maximum", p95maximum);
			writer.marshalDouble ("probability", probability);
			if (has_median()) {
				writer.marshalInt ("median", median);
			}
			if (ver >= SPEC_VER_2 && has_fractileValues()) {
				writer.marshalIntArray ("fractileValues", fractileValues);
			}
			if (ver >= SPEC_VER_2 && has_barPercentages()) {
				writer.marshalIntArray ("barPercentages", barPercentages);
			}
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_bin unmarshal (MarshalReader reader, String name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (name, keys);
			magnitude = reader.unmarshalDouble ("magnitude");
			p95minimum = reader.unmarshalInt ("p95minimum", 0);
			p95maximum = reader.unmarshalInt ("p95maximum", 0);
			probability = reader.unmarshalDouble ("probability");
			if (keys.contains ("median")) {
				median = reader.unmarshalInt ("median", 0);
			} else {
				median = -1;
			}
			if (ver >= SPEC_VER_2 && keys.contains ("fractileValues")) {
				fractileValues = reader.unmarshalIntArray ("fractileValues");
			} else {
				fractileValues = null;
			}
			if (ver >= SPEC_VER_2 && keys.contains ("barPercentages")) {
				barPercentages = reader.unmarshalIntArray ("barPercentages");
			} else {
				barPercentages = null;
			}
			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	// C_aboveMainshockMag - Holds the magnitude bin for above mainshock magnitude for a forecast.

	public static class C_aboveMainshockMag {

		// Magnitude.

		public double magnitude;

		// Probability of at least one aftershock.

		public double probability;

		// Probability distribution for number of aftershocks, or null if not specified.

		public int[] fractileValues;

		public boolean has_fractileValues () {
			return fractileValues != null;
		}

		// Percentage values for the bar graph, or null if not specified.

		public int[] barPercentages;

		public boolean has_barPercentages () {
			return barPercentages != null;
		}

		// Clear.

		public final void clear () {
			magnitude = 0.0;
			probability = 0.0;
			fractileValues = null;
			barPercentages = null;
			return;
		}

		// Constructor.

		public C_aboveMainshockMag () {
			clear();
		}

		// Set.

		public final C_aboveMainshockMag set (
			double magnitude,
			double probability,
			int[] fractileValues,
			int[] barPercentages
		) {
			this.magnitude = magnitude;
			this.probability = probability;
			if (fractileValues == null) {
				this.fractileValues = null;
			} else {
				this.fractileValues = Arrays.copyOf (fractileValues, fractileValues.length);
			}
			if (barPercentages == null) {
				this.barPercentages = null;
			} else {
				this.barPercentages = Arrays.copyOf (barPercentages, barPercentages.length);
			}
			return this;
		}

		// Copy.

		public final C_aboveMainshockMag copy_from (C_aboveMainshockMag other) {
			return set (
				other.magnitude,
				other.probability,
				other.fractileValues,
				other.barPercentages
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalDouble ("magnitude", magnitude);
			writer.marshalDouble ("probability", probability);
			if (ver >= SPEC_VER_2 && has_fractileValues()) {
				writer.marshalIntArray ("fractileValues", fractileValues);
			}
			if (ver >= SPEC_VER_2 && has_barPercentages()) {
				writer.marshalIntArray ("barPercentages", barPercentages);
			}
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_aboveMainshockMag unmarshal (MarshalReader reader, String name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (name, keys);
			magnitude = reader.unmarshalDouble ("magnitude");
			probability = reader.unmarshalDouble ("probability");
			if (ver >= SPEC_VER_2 && keys.contains ("fractileValues")) {
				fractileValues = reader.unmarshalIntArray ("fractileValues");
			} else {
				fractileValues = null;
			}
			if (ver >= SPEC_VER_2 && keys.contains ("barPercentages")) {
				barPercentages = reader.unmarshalIntArray ("barPercentages");
			} else {
				barPercentages = null;
			}
			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	// C_forecast - Holds the forecast for one time period.

	public static class C_forecast {

		// Start of the time period, in milliseconds since the epoch..

		public long timeStart;

		// End of the time period, in milliseconds since the epoch..

		public long timeEnd;

		// Label describing the time period.

		public String label;

		// Forecast bins for various magnitudes.

		public C_bin[] bins;

		// Forecast bin for above mainshock magnitude.

		public C_aboveMainshockMag aboveMainshockMag;

		// Clear.

		public final void clear () {
			timeStart = 0L;
			timeEnd = 0L;
			label = "";
			bins = null;
			aboveMainshockMag = null;
			return;
		}

		// Constructor.

		public C_forecast () {
			clear();
		}

		// Set.

		public final C_forecast set (
				long timeStart,
				long timeEnd,
				String label,
				C_bin[] bins,
				C_aboveMainshockMag aboveMainshockMag
			) {
			this.timeStart = timeStart;
			this.timeEnd = timeEnd;
			this.label = label;
			if (bins == null) {
				this.bins = null;
			} else {
				this.bins = new C_bin[bins.length];
				for (int i = 0; i < bins.length; ++i) {
					this.bins[i] = (new C_bin()).copy_from (bins[i]);
				}
			}
			if (aboveMainshockMag == null) {
				this.aboveMainshockMag = null;
			} else {
				this.aboveMainshockMag = (new C_aboveMainshockMag()).copy_from (aboveMainshockMag);
			}
			return this;
		}

		// Copy.

		public final C_forecast copy_from (C_forecast other) {
			return set (
				other.timeStart,
				other.timeEnd,
				other.label,
				other.bins,
				other.aboveMainshockMag
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalLong ("timeStart", timeStart);
			writer.marshalLong ("timeEnd", timeEnd);
			writer.marshalString ("label", label);

			writer.marshalArrayBegin ("bins", bins.length);
			for (int i = 0; i < bins.length; ++i) {
				bins[i].marshal (writer, null, ver);
			}
			writer.marshalArrayEnd ();

			aboveMainshockMag.marshal (writer, "aboveMainshockMag", ver);
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_forecast unmarshal (MarshalReader reader, String name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (name, keys);
			timeStart = reader.unmarshalLong ("timeStart");
			timeEnd = reader.unmarshalLong ("timeEnd");
			label = reader.unmarshalString ("label");

			int n = reader.unmarshalArrayBegin ("bins");
			bins = new C_bin[n];
			for (int i = 0; i < n; ++i) {
				bins[i] = (new C_bin()).unmarshal (reader, null, ver);
			}
			reader.unmarshalArrayEnd ();

			aboveMainshockMag = (new C_aboveMainshockMag()).unmarshal (reader,"aboveMainshockMag", ver);
			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	// C_root - Holds the root object of forecast.json..

	public static class C_root {

		// Time forecast was created, in milliseconds since the epoch.

		public long creationTime;

		// End time of the longest forecast, in milliseconds since the epoch.

		public long expireTime;

		// Describes the amount of time that the advisory is in effect.
		// See ActionConfig.json: "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year" ]
		// See: USGS_AftershockForecast.Duration

		public String advisoryTimeFrame;

		// Identifies the product template.
		// See: USGS_AftershockForecast.Template

		public String template;

		// Text string to include in the commentary, or "" if none.

		public String injectableText;

		// Observed numbers of earthquakes.

		public C_observation[] observations;

		// Statistical model and parameters.

		public C_model model;

		// Fractiles for number of aftershocks, or null if not specified.

		public double[] fractileProbabilities;

		public boolean has_fractileProbabilities () {
			return fractileProbabilities != null;
		}

		// Labels for the bar graph, or null if not specified.

		public int[] barLabels;

		public boolean has_barLabels () {
			return barLabels != null;
		}

		// Forecasts for various time periods.

		public C_forecast[] forecast;

		// Scheduled time of next forecast, or -1L if none, or -2L if not specified.

		public long nextForecastTime;

		public boolean has_nextForecastTime () {
			return nextForecastTime != -2L;
		}

		// Clear.

		public final void clear () {
			creationTime = 0L;
			expireTime = 0L;
			advisoryTimeFrame = "";
			template = "";
			injectableText = "";
			observations = null;
			model = null;
			fractileProbabilities = null;
			barLabels = null;
			forecast = null;
			nextForecastTime = -2L;
			return;
		}

		// Constructor.

		public C_root () {
			clear();
		}

		// Set.

		public final C_root set (
			long creationTime,
			long expireTime,
			String advisoryTimeFrame,
			String template,
			String injectableText,
			C_observation[] observations,
			C_model model,
			double[] fractileProbabilities,
			int[] barLabels,
			C_forecast[] forecast,
			long nextForecastTime
		) {
			this.creationTime = creationTime;
			this.expireTime = expireTime;
			this.advisoryTimeFrame = advisoryTimeFrame;
			this.template = template;
			this.injectableText = ((injectableText == null) ? "" : injectableText);
			if (observations == null) {
				this.observations = null;
			} else {
				this.observations = new C_observation[observations.length];
				for (int i = 0; i < observations.length; ++i) {
					this.observations[i] = (new C_observation()).copy_from (observations[i]);
				}
			}
			if (model == null) {
				this.model = null;
			} else {
				this.model = (new C_model()).copy_from (model);
			}
			if (fractileProbabilities == null) {
				this.fractileProbabilities = null;
			} else {
				this.fractileProbabilities = Arrays.copyOf (fractileProbabilities, fractileProbabilities.length);
			}
			if (barLabels == null) {
				this.barLabels = null;
			} else {
				this.barLabels = Arrays.copyOf (barLabels, barLabels.length);
			}
			if (forecast == null) {
				this.forecast = null;
			} else {
				this.forecast = new C_forecast[forecast.length];
				for (int i = 0; i < forecast.length; ++i) {
					this.forecast[i] = (new C_forecast()).copy_from (forecast[i]);
				}
			}
			this.nextForecastTime = nextForecastTime;
			return this;
		}

		// Copy.

		public final C_root copy_from (C_root other) {
			return set (
				other.creationTime,
				other.expireTime,
				other.advisoryTimeFrame,
				other.template,
				other.injectableText,
				other.observations,
				other.model,
				other.fractileProbabilities,
				other.barLabels,
				other.forecast,
				other.nextForecastTime
			);
		}

		// Marshal.

		public void marshal (MarshalWriter writer, String name, int ver) {
			writer.marshalMapBegin (name);
			writer.marshalLong ("creationTime", creationTime);
			writer.marshalLong ("expireTime", expireTime);
			writer.marshalString ("advisoryTimeFrame", advisoryTimeFrame);
			writer.marshalString ("template", template);
			writer.marshalString ("injectableText", injectableText);

			writer.marshalArrayBegin ("observations", observations.length);
			for (int i = 0; i < observations.length; ++i) {
				observations[i].marshal (writer, null, ver);
			}
			writer.marshalArrayEnd ();

			model.marshal (writer, "model", ver);

			if (ver >= SPEC_VER_2 && has_fractileProbabilities()) {
				writer.marshalDoubleArray ("fractileProbabilities", fractileProbabilities);
			}

			if (ver >= SPEC_VER_2 && has_barLabels()) {
				writer.marshalIntArray ("barLabels", barLabels);
			}

			writer.marshalArrayBegin ("forecast", forecast.length);
			for (int i = 0; i < forecast.length; ++i) {
				forecast[i].marshal (writer, null, ver);
			}
			writer.marshalArrayEnd ();

			if (has_nextForecastTime()) {
				writer.marshalLong ("nextForecastTime", nextForecastTime);
			}
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal.

		public C_root unmarshal (MarshalReader reader, String name, int ver) {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			reader.unmarshalJsonMapBegin (name, keys);
			creationTime = reader.unmarshalLong ("creationTime");
			expireTime = reader.unmarshalLong ("expireTime");
			advisoryTimeFrame = reader.unmarshalString ("advisoryTimeFrame");
			template = reader.unmarshalString ("template");
			injectableText = reader.unmarshalString ("injectableText");

			int n = reader.unmarshalArrayBegin ("observations");
			observations = new C_observation[n];
			for (int i = 0; i < n; ++i) {
				observations[i] = (new C_observation()).unmarshal (reader, null, ver);
			}
			reader.unmarshalArrayEnd ();

			model = (new C_model()).unmarshal (reader, "model", ver);

			if (ver >= SPEC_VER_2 && keys.contains ("fractileProbabilities")) {
				fractileProbabilities = reader.unmarshalDoubleArray ("fractileProbabilities");
			} else {
				fractileProbabilities = null;
			}

			if (ver >= SPEC_VER_2 && keys.contains ("barLabels")) {
				barLabels = reader.unmarshalIntArray ("barLabels");
			} else {
				barLabels = null;
			}

			n = reader.unmarshalArrayBegin ("forecast");
			forecast = new C_forecast[n];
			for (int i = 0; i < n; ++i) {
				forecast[i] = (new C_forecast()).unmarshal (reader, null, ver);
			}
			reader.unmarshalArrayEnd ();

			if (keys.contains ("nextForecastTime")) {
				nextForecastTime = reader.unmarshalLong ("nextForecastTime");
			} else {
				nextForecastTime = -2L;
			}
			reader.unmarshalJsonMapEnd (false);
			return this;
		}

	}




	//----- Information -----




	// Root of the forecast.json file.

	public C_root json_root;




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		json_root = null;
		return;
	}




	// Default constructor.

	public USGS_ForecastHolder () {
		spec_ver = SPEC_VER_2;
		clear();
	}




	// Set the values.

	public final USGS_ForecastHolder set (
		C_root json_root
	) {
		this.json_root = (new C_root()).copy_from (json_root);
		return this;
	}




	// Copy.

	public final USGS_ForecastHolder copy_from (USGS_ForecastHolder other) {
		return set (
			other.json_root
		);
	}




	//----- Marshaling -----




	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		json_root.marshal (writer, name, spec_ver);
		return;
	}

	// Unmarshal object.

	@Override
	public USGS_ForecastHolder unmarshal (MarshalReader reader, String name) {
		json_root = (new C_root()).unmarshal (reader, name, spec_ver);
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, USGS_ForecastHolder fc_holder) {
		fc_holder.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static USGS_ForecastHolder static_unmarshal (MarshalReader reader, String name) {
		return (new USGS_ForecastHolder()).unmarshal (reader, name);
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "USGS_ForecastHolder");




		// Subcommand : Test #1
		// Command format:
		//  test1  filename
		// Read the file.
		// Then write to terminal as JSON.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Read and write forecast.json");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Read the file

			USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			MarshalUtils.from_json_file (fc_holder, filename);

			// Write to terminal as JSON

			System.out.println ();
			System.out.println ("********** Forecast Contents **********");
			System.out.println ();

			String json_string = MarshalUtils.to_formatted_compact_json_string (fc_holder);
			System.out.println (json_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename  ver
		// Read the file using the specified version.
		// Make a copy.
		// Then write to terminal as JSON.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Read, copy, and write forecast.json");
			String filename = testargs.get_string ("filename");
			int ver = testargs.get_int ("ver");
			testargs.end_test();

			// Read the file

			USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			fc_holder.spec_ver = ver;
			MarshalUtils.from_json_file (fc_holder, filename);

			// Copy

			USGS_ForecastHolder fc_holder2 = (new USGS_ForecastHolder()).copy_from (fc_holder);

			// Write to terminal as JSON

			System.out.println ();
			System.out.println ("********** Copied Forecast Contents **********");
			System.out.println ();

			String json_string = MarshalUtils.to_formatted_compact_json_string (fc_holder2);
			System.out.println (json_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename  ver1  ver2  ver3  ver4
		// Read the file using ver1.
		// Convert to JSON string using ver2.
		// Read the string using ver3.
		// Convert to JSON string using ver4.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Read, copy, and write forecast.json");
			String filename = testargs.get_string ("filename");
			int ver1 = testargs.get_int ("ver1");
			int ver2 = testargs.get_int ("ver2");
			int ver3 = testargs.get_int ("ver3");
			int ver4 = testargs.get_int ("ver4");
			testargs.end_test();

			// Read the file

			USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			fc_holder.spec_ver = ver1;
			MarshalUtils.from_json_file (fc_holder, filename);

			// Write to string

			System.out.println ();
			System.out.println ("********** First Forecast Contents **********");
			System.out.println ();

			fc_holder.spec_ver = ver2;
			String json_string = MarshalUtils.to_formatted_compact_json_string (fc_holder);
			System.out.println (json_string);

			// Read the string

			USGS_ForecastHolder fc_holder2 = new USGS_ForecastHolder();
			fc_holder2.spec_ver = ver3;
			MarshalUtils.from_json_string (fc_holder2, json_string);

			// Write to string

			System.out.println ();
			System.out.println ("********** Second Forecast Contents **********");
			System.out.println ();

			fc_holder2.spec_ver = ver4;
			String json_string2 = MarshalUtils.to_formatted_compact_json_string (fc_holder2);
			System.out.println (json_string2);

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
