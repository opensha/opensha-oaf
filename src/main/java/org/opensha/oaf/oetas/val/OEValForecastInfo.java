package org.opensha.oaf.oetas.val;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.AutoCleanup;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.env.OEtasResults;

import org.opensha.commons.data.siteData.impl.TectonicRegime;

import static org.opensha.oaf.aafs.ForecastResults.PMCODE_GENERIC;			// RJ generic model
import static org.opensha.oaf.aafs.ForecastResults.PMCODE_SEQ_SPEC;			// RJ sequence specific model
import static org.opensha.oaf.aafs.ForecastResults.PMCODE_BAYESIAN;			// RJ bayesian model
import static org.opensha.oaf.aafs.ForecastResults.PMCODE_ETAS;				// ETAS model

import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;


// Class to hold information for a single forecast.
// Author: Michael Barall.

public class OEValForecastInfo implements Marshalable {

	//------ Data output version -----

	// Line format number, to be written to each line in the flat file.

	public int line_format;

	public static final int LINE_FORMAT_MIN = 1;
	public static final int LINE_FORMAT_1 = 1;
	public static final int LINE_FORMAT_MAX = 1;

	// Time format, determines how to display times in the flat file.

	public int time_format;

	public static final int TIME_FORMAT_MIN = 1;
	public static final int TIME_FORMAT_ABS_MILLIS = 1;				// Absolute time in milliseconds
	public static final int TIME_FORMAT_ABS_ISO = 2;				// Absolute time in ISO-8601 format
	public static final int TIME_FORMAT_REL_MILLIS = 3;				// Relative time in milliseconds (absolute time in milliseconds)
	public static final int TIME_FORMAT_REL_DAYS = 4;				// Relative time in days (absolute time in ISO-8601 format)
	public static final int TIME_FORMAT_MAX = 4;


	// Clear the data output version.

	public final void clear_out_version () {
		line_format = 0;
		time_format = 0;
		return;
	}

	// Set default output version.

	public final void set_default_out_version () {
		line_format = LINE_FORMAT_1;
		time_format = TIME_FORMAT_ABS_MILLIS;
		return;
	}

	// Copy the data output version.

	public final void copy_out_version (OEValForecastInfo other) {
		this.line_format = other.line_format;
		this.time_format = other.time_format;
		return;
	}

	// Set the data output version.
	// Either parameter can be 0 or negative to indicate no change.

	public final void set_out_version (int new_line_format, int new_time_format) {
		if (new_line_format > 0) {
			if (!( new_line_format >= LINE_FORMAT_MIN && new_line_format <= LINE_FORMAT_MAX )) {
				throw new IllegalArgumentException ("OEValForecastInfo.set_out_version: Invalid line format: new_line_format = " + new_line_format);
			}
		}
		if (new_time_format > 0) {
			if (!( new_time_format >= TIME_FORMAT_MIN && new_time_format <= TIME_FORMAT_MAX )) {
				throw new IllegalArgumentException ("OEValForecastInfo.set_out_version: Invalid time format: new_time_format = " + new_time_format);
			}
		}

		if (new_line_format > 0) {
			line_format = new_line_format;
		}
		if (new_time_format > 0) {
			time_format = new_time_format;
		}
		return;
	}

	// Write the data output version to a string.

	public final void out_version_to_string (StringBuilder sb) {
		sb.append ("line_format = " + line_format + "\n");
		sb.append ("time_format = " + time_format + "\n");
		return;
	}

	// Marshal output version.

	public final void marshal_out_version (MarshalWriter writer) {
		writer.marshalInt ("line_format", line_format);
		writer.marshalInt ("time_format", time_format);
		return;
	}

	// Unmarshal output version.

	public final void unmarshal_out_version (MarshalReader reader) {
		line_format = reader.unmarshalInt ("line_format", LINE_FORMAT_MIN, LINE_FORMAT_MAX);
		time_format = reader.unmarshalInt ("time_format", TIME_FORMAT_MIN, TIME_FORMAT_MAX);
		return;
	}


	// This class can be used in a try-with-resources to change the output format
	// and then restore it at the end of the block.

	public class FormatSetter extends AutoCleanup {

		// Saved format values to restore.

		private int saved_line_format;
		private int saved_time_format;

		// Constructor sets the formats, values of zero or negative can be used to leave formats unchanged.

		public FormatSetter (int new_line_format, int new_time_format) {
			saved_line_format = line_format;
			saved_time_format = time_format;
			set_out_version (new_line_format, new_time_format);
		}

		// Cleanup function restores the saved format values.

		@Override
		public void cleanup () {
			line_format = saved_line_format;
			time_format = saved_time_format;
			return;
		}
	}




	//----- Mainshock information -----

	// Mainshock event id, as received from Comcat.

	public String mainshock_event_id = null;

	// Mainshock time, in milliseconds since the epoch.

	public long mainshock_time = 0L;

	// Mainshock magnitude.

	public double mainshock_mag = 0.0;

	// Mainshock latitude, in degrees, from -90 to +90.

	public double mainshock_lat = 0.0;

	// Mainshock longitude, in degrees, from -180 to +180.

	public double mainshock_lon = 0.0;

	// Mainshock depth, in kilometers, positive underground.

	public double mainshock_depth = 0.0;


	// Clear the mainshock info.

	public final void clear_mainshock_info () {
		mainshock_event_id = null;
		mainshock_time = 0L;
		mainshock_mag = 0.0;
		mainshock_lat = 0.0;
		mainshock_lon = 0.0;
		mainshock_depth = 0.0;
		return;
	}

	// Copy the mainshock info.

	public final void copy_mainshock_info (OEValForecastInfo other) {
		this.mainshock_event_id = other.mainshock_event_id;
		this.mainshock_time = other.mainshock_time;
		this.mainshock_mag = other.mainshock_mag;
		this.mainshock_lat = other.mainshock_lat;
		this.mainshock_lon = other.mainshock_lon;
		this.mainshock_depth = other.mainshock_depth;
		return;
	}

	// Set the mainshock info.

	public final void set_mainshock_info (ForecastMainshock fcmain) {
		if (!( fcmain.mainshock_avail )) {
			throw new InvariantViolationException ("OEValForecastInfo.set_mainshock_info: Mainshock info is not available");
		}
		this.mainshock_event_id = fcmain.mainshock_event_id;
		this.mainshock_time = fcmain.mainshock_time;
		this.mainshock_mag = fcmain.mainshock_mag;
		this.mainshock_lat = fcmain.mainshock_lat;
		this.mainshock_lon = fcmain.mainshock_lon;
		this.mainshock_depth = fcmain.mainshock_depth;
		return;
	}

	// Set the mainshock info to test values.

	public final void set_test_mainshock_info () {
		this.mainshock_event_id = "testevent";
		this.mainshock_time = SimpleUtils.string_to_time ("2024-01-01T00:00:00Z");
		this.mainshock_mag = 7.1;
		this.mainshock_lat = 37.0;
		this.mainshock_lon = -120.0;
		this.mainshock_depth = 10.0;
		return;
	}

	// Write the mainshock info to a string.

	public final void mainshock_info_to_string (StringBuilder sb) {
		sb.append ("mainshock_event_id = " + mainshock_event_id + "\n");
		sb.append ("mainshock_time = " + abs_time_to_string (mainshock_time) + "\n");
		sb.append ("mainshock_mag = " + mainshock_mag + "\n");
		sb.append ("mainshock_lat = " + mainshock_lat + "\n");
		sb.append ("mainshock_lon = " + mainshock_lon + "\n");
		sb.append ("mainshock_depth = " + mainshock_depth + "\n");
		return;
	}

	// Convert a relative time to a string.

	public final String rel_time_to_string (long the_time) {
		return SimpleUtils.time_raw_and_parseable_string (the_time) + " (" + SimpleUtils.millis_to_days (the_time - mainshock_time) + " days)";
	}

	// Convert an absolute time to a string.

	public final String abs_time_to_string (long the_time) {
		return SimpleUtils.time_raw_and_parseable_string (the_time);
	}

	// Marshal a relative or absolute time, according to the current time format.

	public final void marshal_rel_time (MarshalWriter writer, String name, long the_time) {
		long rel_millis;
		double rel_days;

		switch (time_format) {
		default:
			throw new MarshalException ("OEValForecastInfo.marshal_rel_time: Unknown time format: time_format = " + time_format);
		case TIME_FORMAT_ABS_MILLIS:
			writer.marshalLong (name, the_time);
			break;
		case TIME_FORMAT_ABS_ISO:
			writer.marshalTime (name, the_time);
			break;
		case TIME_FORMAT_REL_MILLIS:
			rel_millis = the_time - mainshock_time;
			writer.marshalLong (name, rel_millis);
			break;
		case TIME_FORMAT_REL_DAYS:
			rel_days = SimpleUtils.millis_to_days (the_time - mainshock_time);
			writer.marshalDouble (name, rel_days);
			break;
		}
		return;
	}

	// Unmarshal a relative or absolute time, according to the current time format.

	public final long unmarshal_rel_time (MarshalReader reader, String name) {
		long the_time;
		long rel_millis;
		double rel_days;

		switch (time_format) {
		default:
			throw new MarshalException ("OEValForecastInfo.unmarshal_rel_time: Unknown time format: time_format = " + time_format);
		case TIME_FORMAT_ABS_MILLIS:
			the_time = reader.unmarshalLong (name);
			break;
		case TIME_FORMAT_ABS_ISO:
			the_time = reader.unmarshalTime (name);
			break;
		case TIME_FORMAT_REL_MILLIS:
			rel_millis = reader.unmarshalLong (name);
			the_time = mainshock_time + rel_millis;
			break;
		case TIME_FORMAT_REL_DAYS:
			rel_days = reader.unmarshalDouble (name);
			the_time = mainshock_time + SimpleUtils.days_to_millis (rel_days);
			break;
		}
		return the_time;
	}

	// Marshal an absolute time, according to the current time format.

	public final void marshal_abs_time (MarshalWriter writer, String name, long the_time) {
		switch (time_format) {
		default:
			throw new MarshalException ("OEValForecastInfo.marshal_abs_time: Unknown time format: time_format = " + time_format);
		case TIME_FORMAT_ABS_MILLIS:
		case TIME_FORMAT_REL_MILLIS:
			writer.marshalLong (name, the_time);
			break;
		case TIME_FORMAT_ABS_ISO:
		case TIME_FORMAT_REL_DAYS:
			writer.marshalTime (name, the_time);
			break;
		}
		return;
	}

	// Unmarshal an absolute time, according to the current time format.

	public final long unmarshal_abs_time (MarshalReader reader, String name) {
		long the_time;

		switch (time_format) {
		default:
			throw new MarshalException ("OEValForecastInfo.unmarshal_abs_time: Unknown time format: time_format = " + time_format);
		case TIME_FORMAT_ABS_MILLIS:
		case TIME_FORMAT_REL_MILLIS:
			the_time = reader.unmarshalLong (name);
			break;
		case TIME_FORMAT_ABS_ISO:
		case TIME_FORMAT_REL_DAYS:
			the_time = reader.unmarshalTime (name);
			break;
		}
		return the_time;
	}

	// Marshal mainshock info.

	public final void marshal_mainshock_info (MarshalWriter writer) {
		writer.marshalString     ("mainshock_event_id", mainshock_event_id);
		marshal_abs_time (writer, "mainshock_time"    , mainshock_time    );
		writer.marshalDouble     ("mainshock_mag"     , mainshock_mag     );
		writer.marshalDouble     ("mainshock_lat"     , mainshock_lat     );
		writer.marshalDouble     ("mainshock_lon"     , mainshock_lon     );
		writer.marshalDouble     ("mainshock_depth"   , mainshock_depth   );
		return;
	}

	// Unmarshal mainshock info.

	public final void unmarshal_mainshock_info (MarshalReader reader) {
		mainshock_event_id = reader.unmarshalString     ("mainshock_event_id");
		mainshock_time     = unmarshal_abs_time (reader, "mainshock_time"    );
		mainshock_mag      = reader.unmarshalDouble     ("mainshock_mag"     );
		mainshock_lat      = reader.unmarshalDouble     ("mainshock_lat"     );
		mainshock_lon      = reader.unmarshalDouble     ("mainshock_lon"     );
		mainshock_depth    = reader.unmarshalDouble     ("mainshock_depth"   );
		return;
	}




	//------ Forecast parameters -----

	// The time of the forecast, in milliseconds since the epoch.
	// This equals the mainshock time plus the forecast lag.

	public long forecast_time;

	public final long get_forecast_lag () {
		return forecast_time - mainshock_time;
	}

	// Generic regime.
	// Cannot be null or empty, cannot have leading/trailing white space.
	// Externally it is URL-encoded so it appears as a single word.

	public String generic_regime;

	public static final String EMPTY_REGIME = "NONE";	// value to use if parameter is empty or null

	// Magnitude of completeness regime.
	// Cannot be null or empty, cannot have leading/trailing white space.
	// Externally it is URL-encoded so it appears as a single word.

	public String mag_comp_regime;

	// Catalog magnitude of completeness.

	public double mag_cat;

	// Helmstetter flag.

	public int helm_flag;

	public static final int HELM_FLAG_MIN = 1;
	public static final int HELM_FLAG_CONSTANT = 1;
	public static final int HELM_FLAG_WORLD = 2;
	public static final int HELM_FLAG_CAL = 3;
	public static final int HELM_FLAG_CUSTOM = 4;
	public static final int HELM_FLAG_MAX = 4;

	// Helmstetter parameters.

	public double capF;
	public double capG;
	public double capH;

	// Aftershock region, assumed to be a circle.

	public double region_lat;
	public double region_lon;
	public double region_radius;		// radius in km


	// Clear the forecast parameters.

	public final void clear_forecast_params () {
		forecast_time = 0L;
		generic_regime = null;
		mag_comp_regime = null;
		mag_cat = 0.0;
		helm_flag = 0;
		capF = 0.0;
		capG = 0.0;
		capH = 0.0;
		region_lat = 0.0;
		region_lon = 0.0;
		region_radius = 0.0;
		return;
	}

	// Copy the forecast parameters.

	public final void copy_forecast_params (OEValForecastInfo other) {
		this.forecast_time = other.forecast_time;
		this.generic_regime = other.generic_regime;
		this.mag_comp_regime = other.mag_comp_regime;
		this.mag_cat = other.mag_cat;
		this.helm_flag = other.helm_flag;
		this.capF = other.capF;
		this.capG = other.capG;
		this.capH = other.capH;
		this.region_lat = other.region_lat;
		this.region_lon = other.region_lon;
		this.region_radius = other.region_radius;
		return;
	}

	// Set the forecast parameters.
	// Note: See OEGUISubCommonValue.update_common_value_from_model() for Helmstetter parsing.

	public final void set_forecast_params (ForecastParameters fcparams) {
		if (!( fcparams.generic_avail && fcparams.mag_comp_avail && fcparams.aftershock_search_avail )) {
			throw new InvariantViolationException ("OEValForecastInfo.set_forecast_params: Forecast parameters are not available");
		}
		if (!( fcparams.aftershock_search_region.isCircular() )) {
			throw new InvariantViolationException ("OEValForecastInfo.set_forecast_params: Aftershock search region is not a circle");
		}

		// Forecast time

		forecast_time = fcparams.forecast_lag + mainshock_time;

		// Regimes

		this.generic_regime = (
			(fcparams.generic_regime == null || fcparams.generic_regime.trim().isEmpty())
			? EMPTY_REGIME
			: fcparams.generic_regime.trim()
		);

		this.mag_comp_regime = (
			(fcparams.mag_comp_regime == null || fcparams.mag_comp_regime.trim().isEmpty())
			? EMPTY_REGIME
			: fcparams.mag_comp_regime.trim()
		);

		// If the generic regime is known to TectonicRegime, replace it with the second name which is better for flat-file usage

		TectonicRegime t_regime = TectonicRegime.forName (this.generic_regime);
		if (t_regime != null) {
			String[] t_names = t_regime.getNames();
			if (t_names.length >= 2 && this.generic_regime.equals (t_names[0])) {
				this.generic_regime = t_names[1];
			}
		}

		// Magnitude of completeness

		this.mag_cat = fcparams.mag_comp_params.get_magCat();

		// Check for constant Mc

		if (fcparams.mag_comp_params.get_magCompFn().is_constant()) {
			this.helm_flag = HELM_FLAG_CONSTANT;
			this.capF = OEConstants.helm_capF (OEConstants.HELM_PARAM_NONE);
			this.capG = OEConstants.helm_capG (OEConstants.HELM_PARAM_NONE);
			this.capH = OEConstants.helm_capH (OEConstants.HELM_PARAM_NONE);
		}

		// Otherwise, check for the world and California special values

		else {
			double f = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapF();
			double g = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapG();
			double h = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapH();

			if (
				   Math.abs (f - OEConstants.helm_capF (OEConstants.HELM_PARAM_WORLD)) <= 0.001
				&& Math.abs (g - OEConstants.helm_capG (OEConstants.HELM_PARAM_WORLD)) <= 0.001
				&& Math.abs (h - OEConstants.helm_capH (OEConstants.HELM_PARAM_WORLD)) <= 0.001
			) {
				this.helm_flag = HELM_FLAG_WORLD;
				this.capF = OEConstants.helm_capF (OEConstants.HELM_PARAM_WORLD);
				this.capG = OEConstants.helm_capG (OEConstants.HELM_PARAM_WORLD);
				this.capH = OEConstants.helm_capH (OEConstants.HELM_PARAM_WORLD);
			}

			else if (
				   Math.abs (f - OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL)) <= 0.001
				&& Math.abs (g - OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL)) <= 0.001
				&& Math.abs (h - OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL)) <= 0.001
			) {
				this.helm_flag = HELM_FLAG_CAL;
				this.capF = OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL);
				this.capG = OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL);
				this.capH = OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL);
			}

			else {
				this.helm_flag = HELM_FLAG_CUSTOM;
				this.capF = f;
				this.capG = g;
				this.capH = h;
			}
		}

		// Aftershock search region

		this.region_lat = fcparams.aftershock_search_region.getCircleCenterLat();
		this.region_lon = fcparams.aftershock_search_region.getCircleCenterLon();
		this.region_radius = fcparams.aftershock_search_region.getCircleRadiusKm();
		return;
	}

	// Set the forecast parameters to test values.

	public final void set_test_forecast_params () {
		this.forecast_time = SimpleUtils.WEEK_MILLIS + mainshock_time;
		this.generic_regime = "GenericRegime";
		this.mag_comp_regime = "MagCompRegime";
		this.mag_cat = 3.0;
		this.helm_flag = HELM_FLAG_CAL;
		this.capF = OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL);
		this.capG = OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL);
		this.capH = OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL);
		this.region_lat = mainshock_lat + 0.1;
		this.region_lon = mainshock_lon + 0.1;
		this.region_radius = 200.0;
		return;
	}

	// Write the forecast parameters to a string.

	public final void forecast_params_to_string (StringBuilder sb) {
		sb.append ("forecast_time = " + rel_time_to_string (forecast_time) + "\n");
		sb.append ("generic_regime = " + generic_regime + "\n");
		sb.append ("mag_comp_regime = " + mag_comp_regime + "\n");
		sb.append ("mag_cat = " + mag_cat + "\n");
		sb.append ("helm_flag = " + helm_flag + "\n");
		sb.append ("capF = " + capF + "\n");
		sb.append ("capG = " + capG + "\n");
		sb.append ("capH = " + capH + "\n");
		sb.append ("region_lat = " + region_lat + "\n");
		sb.append ("region_lon = " + region_lon + "\n");
		sb.append ("region_radius = " + region_radius + "\n");
		return;
	}

	// Make a SphRegion for the aftershock search region.

	public final SphRegion make_search_region () {
		SphRegion search_region = SphRegion.makeCircle (
			new SphLatLon (region_lat, region_lon),
			region_radius
		);
		return search_region;
	}

	// Marshal a regime name, applying URL encoding.

	public final void marshal_regime_name (MarshalWriter writer, String name, String regime_name) {
		String encoded_regime_name = SimpleUtils.url_encode (regime_name, false);
		if (encoded_regime_name == null) {
			throw new MarshalException ("OEValForecastInfo.marshal_regime_name: Unable to URL-encode regime name: regime_name = " + regime_name);
		}
		writer.marshalString (name, encoded_regime_name);
	}

	// Unmarshal a regime name, applying URL decoding.

	public final String unmarshal_regime_name (MarshalReader reader, String name) {
		String encoded_regime_name = reader.unmarshalString (name);
		String regime_name = SimpleUtils.url_decode (encoded_regime_name, false);
		if (regime_name == null) {
			throw new MarshalException ("OEValForecastInfo.unmarshal_regime_name: Unable to URL-decode regime name: encoded_regime_name = " + encoded_regime_name);
		}
		return regime_name;
	}

	// Marshal forecast parameters.

	public final void marshal_forecast_params (MarshalWriter writer) {
		marshal_rel_time (writer, "forecast_time", forecast_time);
		marshal_regime_name (writer, "generic_regime", generic_regime);
		marshal_regime_name (writer, "mag_comp_regime", mag_comp_regime);
		writer.marshalDouble ("mag_cat", mag_cat);
		writer.marshalInt ("helm_flag", helm_flag);
		writer.marshalDouble ("capF", capF);
		writer.marshalDouble ("capG", capG);
		writer.marshalDouble ("capH", capH);
		writer.marshalDouble ("region_lat", region_lat);
		writer.marshalDouble ("region_lon", region_lon);
		writer.marshalDouble ("region_radius", region_radius);
		return;
	}

	// Unmarshal forecast parameters.

	public final void unmarshal_forecast_params (MarshalReader reader) {
		forecast_time = unmarshal_rel_time (reader, "forecast_time");
		generic_regime = unmarshal_regime_name (reader, "generic_regime");
		mag_comp_regime = unmarshal_regime_name (reader, "mag_comp_regime");
		mag_cat = reader.unmarshalDouble ("mag_cat");
		helm_flag = reader.unmarshalInt ("helm_flag");
		capF = reader.unmarshalDouble ("capF");
		capG = reader.unmarshalDouble ("capG");
		capH = reader.unmarshalDouble ("capH");
		region_lat = reader.unmarshalDouble ("region_lat");
		region_lon = reader.unmarshalDouble ("region_lon");
		region_radius = reader.unmarshalDouble ("region_radius");
		return;
	}




	//----- Model selection ------

	// Model type.

	public int model_type;

	public static final int MODEL_TYPE_MIN = 1;
	public static final int MODEL_TYPE_RJ = 1;		// RJ model
	public static final int MODEL_TYPE_ETAS = 2;	// ETAS model
	public static final int MODEL_TYPE_MAX = 2;

	// Model option.

	public int model_option;

	public static final int MODEL_OPTION_MIN = 1;
	public static final int MODEL_OPTION_GENERIC = 1;	// generic model
	public static final int MODEL_OPTION_SEQSPEC = 2;	// sequence specific model
	public static final int MODEL_OPTION_BAYESIAN = 3;	// Bayesian model
	public static final int MODEL_OPTION_CUSTOM = 4;	// custom weight model
	public static final int MODEL_OPTION_UNKNOWN = 5;	// unknown model
	public static final int MODEL_OPTION_MAX = 5;

	// Model Bayesian weight. (See OEConstants.BAY_WT_XXXX)

	public double model_bay_weight;

	// Name of the Bayesian prior.

	public String bay_prior_name;

	public static final String BAY_PRIOR_NAME_RJ = "RJGeneric";			// Name to use for RJ with Page et al priors
	public static final String BAY_PRIOR_NAME_UNKNOWN = "Unknown";		// Name to use if prior is unknown

	// Regime or region for the Bayesian prior.
	// For RJ, it is the same as generic_regime.

	public String bay_prior_regime;

	public static final String BAY_PRIOR_REGIME_UNKNOWN = "UNKNOWN";	// Regime to use if prior is unknown
	public static final String BAY_PRIOR_REGIME_GLOBAL = "GLOBAL";		// Regime to use if prior has global parameters

	// Version for the Bayesian prior.

	public String bay_prior_version;

	public static final String BAY_PRIOR_VERSION_RJ = "1.0";			// Version to use for RJ
	public static final String BAY_PRIOR_VERSION_UNKNOWN = "0.0";		// Version to use if prior is unknown


	// Clear the model selection.

	public final void clear_model_selection () {
		model_type = 0;
		model_option = 0;
		model_bay_weight = 0.0;
		bay_prior_name = null;
		bay_prior_regime = null;
		bay_prior_version = null;
		return;
	}

	// Copy the model selection.

	public final void copy_model_selection (OEValForecastInfo other) {
		this.model_type = other.model_type;
		this.model_option = other.model_option;
		this.model_bay_weight = other.model_bay_weight;
		this.bay_prior_name = other.bay_prior_name;
		this.bay_prior_regime = other.bay_prior_regime;
		this.bay_prior_version = other.bay_prior_version;
		return;
	}

	// Set the model selection according to the PDL model code.
	// Note: See OEGUISubETASValue.is_weight_bay() et seq for parsing the Bayesian weight.

	public final void set_model_selection (ForecastResults fcresults, int pmcode) {

		// Check if the model is available in the results

		if (!( fcresults.is_pdl_model_available (pmcode) )) {
			throw new InvariantViolationException ("OEValForecastInfo.set_model_selection: Selected model is not available: pmcode = " + pmcode);
		}

		// Fill in selection according to code

		switch (pmcode) {

		case PMCODE_GENERIC:
			model_type = MODEL_TYPE_RJ;
			model_option = MODEL_OPTION_GENERIC;
			model_bay_weight = OEConstants.BAY_WT_GENERIC;
			bay_prior_name = BAY_PRIOR_NAME_RJ;
			bay_prior_regime = generic_regime;
			bay_prior_version = BAY_PRIOR_VERSION_RJ;
			return;

		case PMCODE_SEQ_SPEC:
			model_type = MODEL_TYPE_RJ;
			model_option = MODEL_OPTION_SEQSPEC;
			model_bay_weight = OEConstants.BAY_WT_SEQ_SPEC;
			bay_prior_name = BAY_PRIOR_NAME_RJ;
			bay_prior_regime = generic_regime;
			bay_prior_version = BAY_PRIOR_VERSION_RJ;
			return;

		case PMCODE_BAYESIAN:
			model_type = MODEL_TYPE_RJ;
			model_option = MODEL_OPTION_BAYESIAN;
			model_bay_weight = OEConstants.BAY_WT_BAYESIAN;
			bay_prior_name = BAY_PRIOR_NAME_RJ;
			bay_prior_regime = generic_regime;
			bay_prior_version = BAY_PRIOR_VERSION_RJ;
			return;

		case PMCODE_ETAS:
			model_type = MODEL_TYPE_ETAS;
			if (fcresults.etas_outcome instanceof OEtasResults) {
				model_bay_weight = ((OEtasResults)(fcresults.etas_outcome)).bay_weight;
				if (Math.abs (model_bay_weight - OEConstants.BAY_WT_BAYESIAN) <= 0.0001) {
					model_option = MODEL_OPTION_BAYESIAN;
				}
				else if (Math.abs (model_bay_weight - OEConstants.BAY_WT_SEQ_SPEC) <= 0.0001) {
					model_option = MODEL_OPTION_SEQSPEC;
				}
				else if (Math.abs (model_bay_weight - OEConstants.BAY_WT_GENERIC) <= 0.0001) {
					model_option = MODEL_OPTION_GENERIC;
				}
				else {
					model_option = MODEL_OPTION_CUSTOM;
				}
				bay_prior_name = ((OEtasResults)(fcresults.etas_outcome)).bay_prior.get_bay_name();
				if (((OEtasResults)(fcresults.etas_outcome)).bay_prior.is_bay_regime_global()) {
					bay_prior_regime = BAY_PRIOR_REGIME_GLOBAL;
				}
				else {
					bay_prior_regime = ((OEtasResults)(fcresults.etas_outcome)).bay_prior.get_bay_regime();

					// If the regime is known to TectonicRegime, replace it with the second name which is better for flat-file usage

					TectonicRegime t_regime = TectonicRegime.forName (bay_prior_regime);
					if (t_regime != null) {
						String[] t_names = t_regime.getNames();
						if (t_names.length >= 2 && bay_prior_regime.equals (t_names[0])) {
							bay_prior_regime = t_names[1];
						}
					}
				}
				bay_prior_version = ((OEtasResults)(fcresults.etas_outcome)).bay_prior.get_bay_internal_version();
			}
			else {
				model_option = MODEL_OPTION_UNKNOWN;
				model_bay_weight = OEConstants.BAY_WT_BAYESIAN;
				bay_prior_name = BAY_PRIOR_NAME_UNKNOWN;
				bay_prior_regime = BAY_PRIOR_REGIME_UNKNOWN;
				bay_prior_version = BAY_PRIOR_VERSION_UNKNOWN;
			}
			return;
		}

		throw new IllegalArgumentException ("OEValForecastInfo.set_model_selection: Invalid PDL model code: " + pmcode);
	}

	// Set the model selection to test values.

	public final void set_test_model_selection () {
		this.model_type = MODEL_TYPE_ETAS;
		this.model_option = MODEL_OPTION_BAYESIAN;
		this.model_bay_weight = OEConstants.BAY_WT_BAYESIAN;
		this.bay_prior_name = "TestPrior";
		this.bay_prior_regime = "PriorRegime";
		this.bay_prior_version = "1.0";
		return;
	}

	// Write the model selection to a string.

	public final void model_selection_to_string (StringBuilder sb) {
		sb.append ("model_type = " + model_type + "\n");
		sb.append ("model_option = " + model_option + "\n");
		sb.append ("model_bay_weight = " + model_bay_weight + "\n");
		sb.append ("bay_prior_name = " + bay_prior_name + "\n");
		sb.append ("bay_prior_regime = " + bay_prior_regime + "\n");
		sb.append ("bay_prior_version = " + bay_prior_version + "\n");
		return;
	}

	// Marshal model selection.

	public final void marshal_model_selection (MarshalWriter writer) {
		writer.marshalInt ("model_type", model_type);
		writer.marshalInt ("model_option", model_option);
		writer.marshalDouble ("model_bay_weight", model_bay_weight);
		writer.marshalString ("bay_prior_name", bay_prior_name);
		//writer.marshalString ("bay_prior_regime", bay_prior_regime);
		marshal_regime_name (writer, "bay_prior_regime", bay_prior_regime);
		writer.marshalString ("bay_prior_version", bay_prior_version);
		return;
	}

	// Unmarshal model selection.

	public final void unmarshal_model_selection (MarshalReader reader) {
		model_type = reader.unmarshalInt ("model_type", MODEL_TYPE_MIN, MODEL_TYPE_MAX);
		model_option = reader.unmarshalInt ("model_option", MODEL_OPTION_MIN, MODEL_OPTION_MAX);
		model_bay_weight = reader.unmarshalDouble ("model_bay_weight");
		bay_prior_name = reader.unmarshalString ("bay_prior_name");
		//bay_prior_regime = reader.unmarshalString ("bay_prior_regime");
		bay_prior_regime = unmarshal_regime_name (reader, "bay_prior_regime");
		bay_prior_version = reader.unmarshalString ("bay_prior_version");
		return;
	}




	//----- Bin information ------

	// Bin start time (inclusive), in milliseconds since the epoch.

	public long bin_start_time;

	// Bin end time (inclusive), in milliseconds since the epoch.

	public long bin_end_time;

	// Bin magnitude (minimum magnitude, inclusive).
	
	public double bin_mag;

	// Bin clip status.

	public int bin_clip;

	public static final int BIN_CLIP_MIN = 1;
	public static final int BIN_CLIP_NONE = 1;			// bin is not clipped
	public static final int BIN_CLIP_SHADOWED = 2;		// clipped due to shadowing by a larger earthquake (outside aftershock region)
	public static final int BIN_CLIP_AFTERSHOCK = 3;	// clipped due to aftershock larger than the mainshock
	public static final int BIN_CLIP_MAX = 3;

	// Time at which the bin end time is clipped, in milliseconds since the epoch.
	// Note: Equals bin_end_time if not clipped.

	public long bin_clip_time;

	// Actual number of aftershocks in the bin.

	public int bin_actual_count;


	// Clear the bin information.

	public final void clear_bin_info () {
		bin_start_time = 0L;
		bin_end_time = 0L;
		bin_mag = 0.0;
		bin_clip = 0;
		bin_clip_time = 0L;
		bin_actual_count = 0;
		return;
	}

	// Copy the bin information.

	public final void copy_bin_info (OEValForecastInfo other) {
		this.bin_start_time = other.bin_start_time;
		this.bin_end_time = other.bin_end_time;
		this.bin_mag = other.bin_mag;
		this.bin_clip = other.bin_clip;
		this.bin_clip_time = other.bin_clip_time;
		this.bin_actual_count = other.bin_actual_count;
		return;
	}

	// Set the bin information.
	// Parameters:
	//  start_time = Bin start time.
	//  end_time = Bin end time.
	//  mag = Bin magnitude.
	//  seq_clip = Clip status for the sequence, see BIN_CLIP_XXXX.
	//  seq_clip_time = Clip time for the sequence, ignored if seq_clip == BIN_CLIP_NONE.
	// Returns true if success, false if bin would have zero duration.
	// Note: If seq_clip is not BIN_CLIP_NONE, but seq_clip_time > end_time,
	// then seq_clip is treated as if its value were BIN_CLIP_NONE.
	// Note: bin_actual_count is initialized to zero.

	public final boolean set_bin_info (
		long start_time,
		long end_time,
		double mag,
		int seq_clip,
		long seq_clip_time
	) {
		bin_start_time = start_time;
		bin_end_time = end_time;
		bin_mag = mag;

		switch (seq_clip) {
		default:
			throw new IllegalArgumentException ("OEValForecastInfo.set_bin_info: Invalid sequence clip status: seq_clip = " + seq_clip);

		case BIN_CLIP_NONE:
			bin_clip = BIN_CLIP_NONE;
			bin_clip_time = end_time;
			break;

		case BIN_CLIP_SHADOWED:
		case BIN_CLIP_AFTERSHOCK:
			if (seq_clip_time <= end_time) {
				bin_clip = seq_clip;
				bin_clip_time = seq_clip_time;
			} else {
				bin_clip = BIN_CLIP_NONE;
				bin_clip_time = end_time;
			}
			break;
		}

		bin_actual_count = 0;
		if (bin_end_time <= bin_start_time) {
			return false;
		}
		return true;
	}

	// Set the bin information.
	// Parameters:
	//  start_time = Bin start time.
	//  end_time = Bin end time.
	//  mag = Bin magnitude.
	// Note: bin_clip is initialized to BIN_CLIP_NONE, and bin_clip_time is initialized to end_time
	// Note: bin_actual_count is initialized to zero.

	public final void set_bin_info (
		long start_time,
		long end_time,
		double mag
	) {
		bin_start_time = start_time;
		bin_end_time = end_time;
		bin_mag = mag;
		bin_clip = BIN_CLIP_NONE;
		bin_clip_time = end_time;
		bin_actual_count = 0;
		return;
	}

	// Set the bin clipping information.
	// Parameters:
	//  seq_clip = Clip status for the sequence, see BIN_CLIP_XXXX.
	//  seq_clip_time = Clip time for the sequence, ignored if seq_clip == BIN_CLIP_NONE.
	// Returns true if success, false if bin would have zero duration.
	// Note: If seq_clip is not BIN_CLIP_NONE, but seq_clip_time > end_time,
	// then seq_clip is treated as if its value were BIN_CLIP_NONE.
	// Note: Assumes set_bin_info was previously called.

	public final boolean set_bin_clip_info (
		int seq_clip,
		long seq_clip_time
	) {
		switch (seq_clip) {
		default:
			throw new IllegalArgumentException ("OEValForecastInfo.set_bin_clip_info: Invalid sequence clip status: seq_clip = " + seq_clip);

		case BIN_CLIP_NONE:
			bin_clip = BIN_CLIP_NONE;
			bin_clip_time = bin_end_time;
			break;

		case BIN_CLIP_SHADOWED:
		case BIN_CLIP_AFTERSHOCK:
			if (seq_clip_time <= bin_end_time) {
				bin_clip = seq_clip;
				bin_clip_time = seq_clip_time;
			} else {
				bin_clip = BIN_CLIP_NONE;
				bin_clip_time = bin_end_time;
			}
			break;
		}

		if (bin_end_time <= bin_start_time) {
			return false;
		}
		return true;
	}

	// Set the bin information to test values.

	public final void set_test_bin_info () {
		this.bin_start_time = mainshock_time + (3L * SimpleUtils.DAY_MILLIS);
		this.bin_end_time = mainshock_time + (10L * SimpleUtils.DAY_MILLIS);
		this.bin_mag = 4.0;
		this.bin_clip = BIN_CLIP_NONE;
		this.bin_clip_time = this.bin_end_time;
		this.bin_actual_count = 7;
		return;
	}

	// Count a rupture, if it lies within the clipped bin.
	// Returns true if counted, false if not.

	public final boolean count_rupture (long rup_time, double rup_mag) {
		if (rup_time >= bin_start_time && rup_time <= bin_clip_time && rup_mag >= bin_mag) {
			++bin_actual_count;
			return true;
		}
		return false;
	}

	// Write the bin information to a string.

	public final void bin_info_to_string (StringBuilder sb) {
		sb.append ("bin_start_time = " + rel_time_to_string (bin_start_time) + "\n");
		sb.append ("bin_end_time = " + rel_time_to_string (bin_end_time) + "\n");
		sb.append ("bin_mag = " + bin_mag + "\n");
		sb.append ("bin_clip = " + bin_clip + "\n");
		sb.append ("bin_clip_time = " + rel_time_to_string (bin_clip_time) + "\n");
		sb.append ("bin_actual_count = " + bin_actual_count + "\n");
		return;
	}

	// Marshal bin information.

	public final void marshal_bin_info (MarshalWriter writer) {
		marshal_rel_time (writer, "bin_start_time", bin_start_time);
		marshal_rel_time (writer, "bin_end_time", bin_end_time);
		writer.marshalDouble ("bin_mag", bin_mag);
		writer.marshalInt ("bin_clip", bin_clip);
		marshal_rel_time (writer, "bin_clip_time", bin_clip_time);
		writer.marshalInt ("bin_actual_count", bin_actual_count);
		return;
	}

	// Unmarshal bin information.

	public final void unmarshal_bin_info (MarshalReader reader) {
		bin_start_time = unmarshal_rel_time (reader, "bin_start_time");
		bin_end_time = unmarshal_rel_time (reader, "bin_end_time");
		bin_mag = reader.unmarshalDouble ("bin_mag");
		bin_clip = reader.unmarshalInt ("bin_clip", BIN_CLIP_MIN, BIN_CLIP_MAX);
		bin_clip_time = unmarshal_rel_time (reader, "bin_clip_time");
		bin_actual_count = reader.unmarshalInt ("bin_actual_count");
		return;
	}




	//----- Probability information -----

	// Lower p-value.
	// Note: p-value is the inverse of the fractile probability->value mapping, applied to the actual value.

	public double p_value_low;

	// Upper p-value.
	// Note: p-value is the inverse of the fractile probability->value mapping, applied to the actual value.

	public double p_value_high;

	// Probability corresponding to the first fractile.
	// Note: Probabilities are assumed to be equally-spaced between prob_first and prob_last.

	public double prob_first;

	// Probability corresponding to the last fractile.
	// Note: Probabilities are assumed to be equally-spaced between prob_first and prob_last.

	public double prob_last;

	// Fractile values.
	// Note: In the flat file, this is the number of fractiles followed by the fractile values.

	public int[] fractile_values;


	// Clear the probability information.

	public final void clear_prob_info () {
		p_value_low = 0.0;
		p_value_high = 0.0;
		prob_first = 0.0;
		prob_last = 0.0;
		fractile_values = null;
		return;
	}

	// Copy the probability information.

	public final void copy_prob_info (OEValForecastInfo other) {
		this.p_value_low = other.p_value_low;
		this.p_value_high = other.p_value_high;
		this.prob_first = other.prob_first;
		this.prob_last = other.prob_last;
		this.fractile_values = ((other.fractile_values == null) ? other.fractile_values : other.fractile_values.clone());
		return;
	}

	// Set the probability information.

	public final void set_prob_info (
		double[] frac_probs,
		int[] frac_values
	) {
		// Save the fractiles

		prob_first = frac_probs[0];
		prob_last = frac_probs[frac_probs.length - 1];
		fractile_values = frac_values.clone();

		// Compute the p-values

		double[] p = new double[2];
		boolean hiopt = true;

		boolean single_p = OEValUtils.calc_fractile_p (frac_probs, frac_values, bin_actual_count, p, hiopt);

		p_value_low = p[0];
		p_value_high = p[1];

		return;
	}

	// Set the probability information to test values.

	public final void set_test_prob_info () {
		int flen = 197;
		double my_prob_first = 0.01;
		double my_prob_last = 0.99;
		double[] frac_probs = OEValUtils.make_prob_array (flen, my_prob_first, my_prob_last, true);
		int[] frac_values = OEValUtils.make_test_fractile_values (flen, 0, flen/11);
		set_prob_info (frac_probs, frac_values);
		return;
	}

	// Write the probability information to a string.

	public final void prob_info_to_string (StringBuilder sb) {
		sb.append ("p_value_low = " + p_value_low + "\n");
		sb.append ("p_value_high = " + p_value_high + "\n");
		sb.append ("prob_first = " + prob_first + "\n");
		sb.append ("prob_last = " + prob_last + "\n");
		sb.append ("fractile_values = [" + "\n");
		for (int ix = 0; ix < fractile_values.length; ++ix) {
			sb.append ("  " + ix + ": " + fractile_values[ix] + "\n");
		}
		sb.append ("]" + "\n");
		return;
	}

	// Marshal probability information.

	public final void marshal_prob_info (MarshalWriter writer) {
		writer.marshalDouble ("p_value_low", p_value_low);
		writer.marshalDouble ("p_value_high", p_value_high);
		writer.marshalDouble ("prob_first", prob_first);
		writer.marshalDouble ("prob_last", prob_last);
		writer.marshalIntArray ("fractile_values", fractile_values);
		return;
	}

	// Unmarshal probability information.

	public final void unmarshal_prob_info (MarshalReader reader) {
		p_value_low = reader.unmarshalDouble ("p_value_low");
		p_value_high = reader.unmarshalDouble ("p_value_high");
		prob_first = reader.unmarshalDouble ("prob_first");
		prob_last = reader.unmarshalDouble ("prob_last");
		fractile_values = reader.unmarshalIntArray ("fractile_values");
		return;
	}




	//----- Combined data functions -----

	// Clear all data.

	public final void clear_all () {
		clear_out_version();
		clear_mainshock_info();
		clear_forecast_params();
		clear_model_selection();
		clear_bin_info();
		clear_prob_info();
		return;
	}

	// Copy all data from another object.

	public final void copy_all (OEValForecastInfo other) {
		copy_out_version (other);
		copy_mainshock_info (other);
		copy_forecast_params (other);
		copy_model_selection (other);
		copy_bin_info (other);
		copy_prob_info (other);
		return;
	}

	// Set all data to test values.

	public final void set_test_all () {
		set_default_out_version();
		set_test_mainshock_info();
		set_test_forecast_params();
		set_test_model_selection();
		set_test_bin_info();
		set_test_prob_info();
		return;
	}

	// Write all data to a string.

	public final void all_to_string (StringBuilder sb) {
		out_version_to_string (sb);
		mainshock_info_to_string (sb);
		forecast_params_to_string (sb);
		model_selection_to_string (sb);
		bin_info_to_string (sb);
		prob_info_to_string (sb);
		return;
	}

	// Marshal all data.

	public final void marshal_all (MarshalWriter writer) {
		marshal_out_version (writer);
		marshal_mainshock_info (writer);
		marshal_forecast_params (writer);
		marshal_model_selection (writer);
		marshal_bin_info (writer);
		marshal_prob_info (writer);
		return;
	}

	// Unmarshal all data.

	public final void unmarshal_all (MarshalReader reader) {
		unmarshal_out_version (reader);
		unmarshal_mainshock_info (reader);
		unmarshal_forecast_params (reader);
		unmarshal_model_selection (reader);
		unmarshal_bin_info (reader);
		unmarshal_prob_info (reader);
		return;
	}




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		clear_all();
		return;
	}




	// Default constructor, sets up default values.

	public OEValForecastInfo () {
		clear();
	}




	// Copy values from another object.
	// Returns this object.

	public final OEValForecastInfo copy_from (OEValForecastInfo other) {
		copy_all (other);
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEValForecastInfo:" + "\n");

		all_to_string (result);

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 154001;

	private static final String M_VERSION_NAME = "OEValForecastInfo";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			marshal_all (writer);

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

			unmarshal_all (reader);

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
	public OEValForecastInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEValForecastInfo obj) {
		obj.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEValForecastInfo static_unmarshal (MarshalReader reader, String name) {
		return (new OEValForecastInfo()).unmarshal (reader, name);
	}

	// Marshal object to a single line.
	// The newline character is not appended.
	// The line format and time format can be overriden (zero or negative means no change).

	public String marshal_to_line (int new_line_format, int new_time_format) {
		StringBuilder sb = new StringBuilder();
		boolean f_unicode = false;
		boolean f_quote_all = false;
		boolean f_store_names = false;
		MarshalWriter writer = MarshalUtils.writer_for_line (sb, f_unicode, f_quote_all, f_store_names);
		
		try (
			FormatSetter setter = new FormatSetter (new_line_format, new_time_format);
		) {
			writer.marshalMapBegin (null);
			marshal_all (writer);
			writer.marshalMapEnd ();
		}

		writer.write_completion_check();
		return sb.toString();
	}

	// Unmarshal object from a single line.

	public OEValForecastInfo unmarshal_from_line (String line) {
		boolean f_store_names = false;
		MarshalReader reader = MarshalUtils.reader_for_line (line, f_store_names);

		reader.unmarshalMapBegin (null);
		unmarshal_all (reader);
		reader.unmarshalMapEnd ();

		boolean f_require_eof = true;
		reader.read_completion_check (f_require_eof);

		return this;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEValForecastInfo make_test_value_1 () {
		OEValForecastInfo val_fcinfo = new OEValForecastInfo();

		val_fcinfo.set_test_all();

		return val_fcinfo;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEValForecastInfo");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Copy, and display the results.
		// Marshal to flat line, then unmarshal and display the results.
		// This uses test values.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying forecast info");
			testargs.end_test();

			// Create the values

			OEValForecastInfo val_fcinfo = make_test_value_1();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Info Display **********");
			System.out.println ();

			System.out.println (val_fcinfo.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (val_fcinfo);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (val_fcinfo);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEValForecastInfo val_fcinfo2 = new OEValForecastInfo();
			MarshalUtils.from_json_string (val_fcinfo2, json_string);

			// Display the contents

			System.out.println (val_fcinfo2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy Info **********");
			System.out.println ();
			
			OEValForecastInfo val_fcinfo3 = new OEValForecastInfo();
			val_fcinfo3.copy_from (val_fcinfo2);

			// Display the contents

			System.out.println (val_fcinfo3.toString());

			// Marshal to line

			System.out.println ();
			System.out.println ("********** Marshal to Line, Relative Time in Days **********");
			System.out.println ();

			String line_string = val_fcinfo.marshal_to_line (LINE_FORMAT_1, TIME_FORMAT_REL_DAYS);

			System.out.println (line_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from Line **********");
			System.out.println ();
			
			OEValForecastInfo val_fcinfo4 = new OEValForecastInfo();
			val_fcinfo4.unmarshal_from_line (line_string);

			// Display the contents

			System.out.println (val_fcinfo4.toString());

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
