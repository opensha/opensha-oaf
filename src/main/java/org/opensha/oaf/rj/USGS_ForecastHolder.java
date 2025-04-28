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

		// Scheduled time of next forecast, or 0L if unknown, or -1L if none, or -2L if not specified.

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
			advisoryTimeFrame = adjust_case_of_advisory_time_frame (reader.unmarshalString ("advisoryTimeFrame"));
			template = adjust_case_of_template (reader.unmarshalString ("template"));
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




	//----- Table access -----

	// These functions allow forecast contents to be accessed via two indexes
	// (ix_dur, ix_mag) which select among the available forecast durations and
	// the available magnitudes within the duration.  The last magnitude is the
	// above mainshock magnitude.  They also support creation of the GUI table.




	// Get the number of durations.

	public final int get_dur_count () {
		return json_root.forecast.length;
	}


	// Get the start time for the given duration.

	public final long get_start_time (int ix_dur) {
		return json_root.forecast[ix_dur].timeStart;
	}


	// Get the end time for the given duration.

	public final long get_end_time (int ix_dur) {
		return json_root.forecast[ix_dur].timeEnd;
	}


	// Get the label for the given duration.

	public final String get_label (int ix_dur) {
		return json_root.forecast[ix_dur].label;
	}


	// Get the magnitude count for the given duration, including the above-mainshock magnitude.

	public final int get_mag_count (int ix_dur) {
		return json_root.forecast[ix_dur].bins.length + 1;
	}


	// Get the total magnitude count for all durations, including the above-mainshock magnitudes.

	public final int get_total_mag_count () {
		int total = 0;
		int dur_count = get_dur_count();
		for (int ix_dur = 0; ix_dur < dur_count; ++ix_dur) {
			total += get_mag_count (ix_dur);
		}
		return total;
	}


	// Get the magnitude for the given duration and magnitude index..

	public final double get_mag (int ix_dur, int ix_mag) {
		if (ix_mag == json_root.forecast[ix_dur].bins.length) {
			return json_root.forecast[ix_dur].aboveMainshockMag.magnitude;
		}
		return json_root.forecast[ix_dur].bins[ix_mag].magnitude;
	}


	// Return true if the given magnitude for the given duration is for the mainshock.

	public final boolean is_mag_for_mainshock (int ix_dur, int ix_mag) {
		return ix_mag == json_root.forecast[ix_dur].bins.length;
	}


	// Return the probability of one or more, for the given duration and magnitude.

	public final double get_probability (int ix_dur, int ix_mag) {
		if (is_mag_for_mainshock (ix_dur, ix_mag)) {
			return json_root.forecast[ix_dur].aboveMainshockMag.probability;
		}
		return json_root.forecast[ix_dur].bins[ix_mag].probability;
	}


	// Return the lower limit of the 95% confidence interval, for the given duration and magnitude, < 0 if not available.

	public final int get_p95minimum (int ix_dur, int ix_mag) {
		if (is_mag_for_mainshock (ix_dur, ix_mag)) {
			return -1;
		}
		return json_root.forecast[ix_dur].bins[ix_mag].p95minimum;
	}


	// Return the upper limit of the 95% confidence interval, for the given duration and magnitude, < 0 if not available.

	public final int get_p95maximum (int ix_dur, int ix_mag) {
		if (is_mag_for_mainshock (ix_dur, ix_mag)) {
			return -1;
		}
		return json_root.forecast[ix_dur].bins[ix_mag].p95maximum;
	}


	// Return the median, for the given duration and magnitude, < 0 if not available.

	public final int get_median (int ix_dur, int ix_mag) {
		if (is_mag_for_mainshock (ix_dur, ix_mag)) {
			return -1;
		}
		if (json_root.forecast[ix_dur].bins[ix_mag].has_median()) {
			return json_root.forecast[ix_dur].bins[ix_mag].median;
		}
		return -1;
	}


	// This object is used to return one line in a table representation of the forecast, for use by the GUI.

	public static class TableRow {

		// The row number relative to the first row for the duration, or -1 for a separator line.
		// This equals the magnitude index if the row has a magnitude.

		public int rel_row = -1;

		public final boolean is_separator () {
			return rel_row < 0;
		}

		// The start time, end time, and label for this duration (valid if this is not a separator line)

		public long start_time = 0L;
		public long end_time = 0L;
		public String label = "";

		// True if we have a magnitude (if true then this is neither a separator nor a filler line)

		public boolean has_mag = false;

		// True if this magnitude is for the mainshock (valid if has_mag is true).

		public boolean mag_for_mainshock = false;

		// Magnitude (valid if has_mag is true).

		public double mag = 0.0;

		// The probability of one or more aftershocks (valid if has_mag is true).

		public double probability = 0.0;

		// The lower and upper limits of the 95% confidence interval, or -1 if not available (valid if has_mag is true).

		public int p95minimum = -1;
		public int p95maximum = -1;

		public final boolean has_p95 () {
			return p95minimum >= 0 && p95maximum >= 0;
		}

		// The median, or -1 if not available (valid if has_mag is true).

		public int median = -1;

		public final boolean has_median () {
			return median >= 0;
		}

		// Constructor creates a separator line.

		public TableRow () {}

		// Constructor creates a filler line.

		public TableRow (int rel_row) {
			this.rel_row = rel_row;
		}

		// Constructor creates a table row.
		// Creates a separator line if ix_mag < 0.
		// Creates a filler line if ix_mag >= mag_count.

		public TableRow (USGS_ForecastHolder fch, int ix_dur, int ix_mag) {
			rel_row = ix_mag;

			// If this is not a separator line ...

			if (ix_mag >= 0) {
				start_time = fch.get_start_time (ix_dur);
				end_time = fch.get_end_time (ix_dur);
				label = fch.get_label (ix_dur);

				// If this line contains a magnitude ...

				if (ix_mag < fch.get_mag_count (ix_dur)) {
					has_mag = true;
					mag_for_mainshock = fch.is_mag_for_mainshock (ix_dur, ix_mag);
					mag = fch.get_mag (ix_dur, ix_mag);
					probability = fch.get_probability (ix_dur, ix_mag);
					p95minimum = fch.get_p95minimum (ix_dur, ix_mag);
					p95maximum = fch.get_p95maximum (ix_dur, ix_mag);
					median = fch.get_median (ix_dur, ix_mag);
				}
			}
		}

		// Write as a one-line string

		@Override
		public String toString () {
			return
				"rel_row = " + rel_row +
				", start_time = " + start_time +
				", end_time = " + end_time +
				", label = " + label +
				", has_mag = " + has_mag +
				", mag_for_mainshock = " + mag_for_mainshock +
				", mag = " + mag +
				", probability = " + probability +
				", p95minimum = " + p95minimum +
				", p95maximum = " + p95maximum +
				", median = " + median;
		}

		// Get the text to put in a box in the GUI table, for the given column.

		public String get_gui_text (int ix_col) {
			if (!( is_separator() )) {
				switch (ix_col) {
				case 0:
					switch (rel_row) {
					case 0:
						return label;
					case 1:
						return SimpleUtils.time_to_string (start_time);
					case 2:
						return "through";
					case 3:
						return SimpleUtils.time_to_string (end_time);
					}
					break;

				case 1:
					if (has_mag) {
						String s_mag = SimpleUtils.double_to_string_trailz ("%.3f", SimpleUtils.TRAILZ_REMOVE, mag);
						if (mag_for_mainshock) {
							return "M \u2265 Main (" + s_mag + ")";
						} else {
							return "M \u2265 " + s_mag;		// \u2265 is the greater-than-or-equal-to symbol
						}
					}
					break;

				case 2:
					if (has_mag && has_p95()) {
						if (p95maximum == 0) {
							return "*";
						} else if (has_median()) {
							return p95minimum + " to " + p95maximum + ", median " + median;
						} else {
							return p95minimum + " to " + p95maximum;
						}
					}
					break;

				case 3:
					if (has_mag) {
						double prob = probability * 100.0;
						if (prob < 1.0e-6 && prob > 0.0) {
							return SimpleUtils.double_to_string ("%.2e", prob) + " %";
						} else {
							double prob_rounded = SimpleUtils.round_double_via_string ("%.2e", prob);
							return SimpleUtils.double_to_string_trailz ("%.8f", SimpleUtils.TRAILZ_REMOVE, prob_rounded) + " %";
						}
					}
					break;

				}
			}
			return "";
		}

	}


	// Make table rows.
	// Parameters:
	//  min_rows_per_dur = Minimum number of rows for each duration.
	//  separator_rows = Number of rows between durations.

	public final TableRow[] make_table_rows (int min_rows_per_dur, int separator_rows) {

		// Calculate total number of rows

		int total_rows = 0;
		int dur_count = get_dur_count();

		for (int ix_dur = 0; ix_dur < dur_count; ++ix_dur) {
			if (ix_dur > 0) {
				total_rows += separator_rows;
			}
			total_rows += Math.max (get_mag_count (ix_dur), min_rows_per_dur);
		}

		// Allocate the array, fill with separator rows

		TableRow[] rows = new TableRow[total_rows];
		int ix_row = 0;

		// Loop over durations ...

		for (int ix_dur = 0; ix_dur < dur_count; ++ix_dur) {

			// If this is not the first duration, add the separator lines

			if (ix_dur > 0) {
				for (int i = 0; i < separator_rows; ++i) {
					rows[ix_row++] = new TableRow();
				}
			}

			// Loop over rows within this duration, and add the lines

			int mag_count = Math.max (get_mag_count (ix_dur), min_rows_per_dur);
			for (int ix_mag = 0; ix_mag < mag_count; ++ix_mag) {
				rows[ix_row++] = new TableRow (this, ix_dur, ix_mag);
			}
		}

		// Done

		return rows;
	}


	// This object holds a complete table suitable for use in the GUI.

	public static class GUITable {

		// The list of table rows.

		public TableRow[] rows;

		// Constructor builds the list of table rows.

		public GUITable (USGS_ForecastHolder fch) {
			int min_rows_per_dur = 4;
			int separator_rows = 1;
			rows = fch.make_table_rows (min_rows_per_dur, separator_rows);
		}

		// Get the number of rows.

		public int get_row_count () {
			return rows.length + 1;
		}

		// Get the number of columns.

		public int get_col_count () {
			return 4;
		}

		// Get text for the given row and column.

		public String get_gui_text (int ix_row, int ix_col) {
			if (ix_row == 0) {
				switch (ix_col) {
				case 0:
					return "Time Window";
				case 1:
					return "Magnitude Range";
				case 2:
					return "Expected Number Of Aftershocks";
				case 3:
					return "Probability Of One Or More";
				}
			}
			else if (ix_row >= 1 && ix_row <= rows.length) {
				return rows[ix_row - 1].get_gui_text (ix_col);
			}
			return "";
		}
	}


	// Make the GUI table.

	public GUITable make_gui_table () {
		return new GUITable (this);
	}




	//----- Modification -----
	//
	// A limited set of methods is provided to access and modify contents.




	// Convert a string to an advisory time frame, case-insensitive.
	// Throws IllegalArgumentException if string does not correspond to any duration label.

	public static USGS_AftershockForecast.Duration string_to_advisory_time_frame (String s) {
		for (USGS_AftershockForecast.Duration duration : USGS_AftershockForecast.Duration.values()) {
			if (s.equalsIgnoreCase (duration.toString())) {
				return duration;
			}
		}
		throw new IllegalArgumentException ("USGS_ForecastHolder.string_to_advisory_time_frame: Invalid advisory time frame: " + s);
	}


	// Adjust the case of a string that represents an advisory time frame.
	// Throws IllegalArgumentException if string does not correspond to any duration label.

	public static String adjust_case_of_advisory_time_frame (String s) {
		for (USGS_AftershockForecast.Duration duration : USGS_AftershockForecast.Duration.values()) {
			if (s.equalsIgnoreCase (duration.toString())) {
				return duration.toString();
			}
		}
		throw new IllegalArgumentException ("USGS_ForecastHolder.adjust_case_of_advisory_time_frame: Invalid advisory time frame: " + s);
	}


	// Get the advisory time frame as a string.

	public final String get_advisory_time_frame () {
		return json_root.advisoryTimeFrame;
	}


	// Get the advisory time frame as an enum.
	// Throws IllegalArgumentException if the string does not correspond to any enum value.

	public final USGS_AftershockForecast.Duration get_advisory_time_frame_as_enum () {
		return string_to_advisory_time_frame (json_root.advisoryTimeFrame);
	}


	// Set the advisory time frame from a string.
	// The value must be one of the labels defined by USGS_AftershockForecast.Duration.
	// The string is treated as case-insensitive and is converted to correct case.
	// Throws IllegalArgumentException if the value is invalid.

	public final void set_advisory_time_frame (String s) {
		json_root.advisoryTimeFrame = adjust_case_of_advisory_time_frame (s);
		return;
	}


	// Set the advisory time frame from an enum.

	public final void set_advisory_time_frame_from_enum (USGS_AftershockForecast.Duration duration) {
		json_root.advisoryTimeFrame = duration.toString();
		return;
	}




	// Convert a string to a template, case-insensitive.
	// Throws IllegalArgumentException if string does not correspond to any template title.

	public static USGS_AftershockForecast.Template string_to_template (String s) {
		for (USGS_AftershockForecast.Template template : USGS_AftershockForecast.Template.values()) {
			if (s.equalsIgnoreCase (template.toString())) {
				return template;
			}
		}
		throw new IllegalArgumentException ("USGS_ForecastHolder.string_to_template: Invalid template: " + s);
	}


	// Adjust the case of a string that represents a template.
	// Throws IllegalArgumentException if string does not correspond to any template title.

	public static String adjust_case_of_template (String s) {
		for (USGS_AftershockForecast.Template template : USGS_AftershockForecast.Template.values()) {
			if (s.equalsIgnoreCase (template.toString())) {
				return template.toString();
			}
		}
		throw new IllegalArgumentException ("USGS_ForecastHolder.adjust_case_of_template: Invalid template: " + s);
	}


	// Get the template as a string.

	public final String get_template () {
		return json_root.template;
	}


	// Get the template as an enum.
	// Throws IllegalArgumentException if the string does not correspond to any enum value.

	public final USGS_AftershockForecast.Template get_template_as_enum () {
		return string_to_template (json_root.template);
	}


	// Set the template from a string.
	// The value must be one of the titles defined by USGS_AftershockForecast.Duration.
	// The string is treated as case-insensitive and is converted to correct case.
	// Throws IllegalArgumentException if the value is invalid.

	public final void set_template (String s) {
		json_root.template = adjust_case_of_template (s);
		return;
	}


	// Set the template from an enum.

	public final void set_template_from_enum (USGS_AftershockForecast.Template template) {
		json_root.template = template.toString();
		return;
	}




	// Get flag to include magnitude above mainshock.

	public final boolean get_include_above_mainshock () {
		return true;
	}


	// Set flag to includ magnitude above mainshock.
	// This is a no-operation.

	public final void set_include_above_mainshock (boolean x) {
		return;
	}




	// Get the injectable text.

	public final String get_injectable_text () {
		return json_root.injectableText;
	}


	// Set the injectable text.
	// A null value is converted to an empty string.

	public final void set_injectable_text (String s) {
		json_root.injectableText = ((s == null) ? "" : s);
		return;
	}




	// Get the next forecast time.

	public final long get_next_forecast_time () {
		return json_root.nextForecastTime;
	}


	// Set the next forecast time.

	public final void set_next_forecast_time (long t) {
		json_root.nextForecastTime = t;
		return;
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




		// Subcommand : Test #4
		// Command format:
		//  test4  filename  min_rows_per_dur  separator_rows
		// Read the file.
		// Make a table with the given minimum rows per duration and separator rows.
		// Write out the table.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Read file and write as table");
			String filename = testargs.get_string ("filename");
			int min_rows_per_dur = testargs.get_int ("min_rows_per_dur");
			int separator_rows = testargs.get_int ("separator_rows");
			testargs.end_test();

			// Read the file

			USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			MarshalUtils.from_json_file (fc_holder, filename);

			// Make the table and write it out

			System.out.println ();
			System.out.println ("********** Making table **********");
			System.out.println ();

			TableRow[] table = fc_holder.make_table_rows (min_rows_per_dur, separator_rows);

			System.out.println ("Number of rows = " + table.length);
			System.out.println ();

			for (int j = 0; j < table.length; ++j) {
				System.out.println (table[j].toString());
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  filename
		// Read the file.
		// Make a GUI table and write it out.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Read file and write as GUI table");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Read the file

			USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			MarshalUtils.from_json_file (fc_holder, filename);

			// Make the table and write it out

			System.out.println ();
			System.out.println ("********** Making GUI table **********");
			System.out.println ();

			GUITable table = fc_holder.make_gui_table ();

			int row_count = table.get_row_count();
			int col_count = table.get_col_count();

			System.out.println ("Number of rows = " + row_count);
			System.out.println ("Number of columns = " + col_count);
			System.out.println ();

			for (int ix_row = 0; ix_row < row_count; ++ix_row) {
				StringBuilder line = new StringBuilder();
				for (int ix_col = 0; ix_col < col_count; ++ix_col) {
					if (ix_col > 0) {
						line.append ("  |  ");
					}
					line.append (table.get_gui_text (ix_row, ix_col));
				}
				System.out.println (line.toString());
			}

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
