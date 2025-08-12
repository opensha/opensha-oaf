package org.opensha.oaf.aafs;

//import java.util.GregorianCalendar;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;

import java.time.Instant;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.catalog.ObsEqkRupMinTimeComparator;
import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.catalog.EventIDGenerator;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.GenericRJ_Parameters;
import org.opensha.oaf.rj.MagCompPage_Parameters;
import org.opensha.oaf.rj.RJ_AftershockModel;
import org.opensha.oaf.rj.RJ_AftershockModel_Bayesian;
import org.opensha.oaf.rj.RJ_AftershockModel_Generic;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;
import org.opensha.oaf.rj.RJ_Summary;
import org.opensha.oaf.rj.RJ_Summary_Bayesian;
import org.opensha.oaf.rj.RJ_Summary_Generic;
import org.opensha.oaf.rj.RJ_Summary_SequenceSpecific;
import org.opensha.oaf.rj.SeqSpecRJ_Parameters;
import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_ForecastInfo;

import org.opensha.oaf.oetas.env.OEExecEnvironment;
import org.opensha.oaf.oetas.env.OEtasOutcome;
import org.opensha.oaf.oetas.env.OEtasLogInfo;
import org.opensha.oaf.oetas.env.OEtasCatalogInfo;

import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_AUTO_PDL;
import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_AUTO_NO_PDL;
import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_SUPPRESS;

import static org.opensha.oaf.aafs.ForecastParameters.SEARCH_PARAM_OMIT;
import static org.opensha.oaf.aafs.ForecastParameters.SEARCH_PARAM_TEST;

import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_NOT_ELIGIBLE;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_MAG_COMP_FORM;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_NO_DATA;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_UNSUPPORTED;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_NO_PARAMS;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_DISABLED;
import static org.opensha.oaf.oetas.env.OEExecEnvironment.ETAS_RESCODE_NOT_ATTEMPTED;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

/**
 * Results of a forecast.
 * Author: Michael Barall 04/26/2018.
 *
 * All fields are public, since there is little benefit to having lots of getters and setters.
 */
public class ForecastResults implements Marshalable {

	//----- Constants -----

	// Standard values of the advisory duration.

	public static final long ADVISORY_LAG_DAY   = 86400000L;	// 1 day
	public static final long ADVISORY_LAG_WEEK  = 604800000L;	// 1 week = 7 days
	public static final long ADVISORY_LAG_MONTH = 2592000000L;	// 1 month = 30 days
	public static final long ADVISORY_LAG_YEAR  = 31536000000L;	// 1 year = 365 days

	// Convert a forecast lag into an advisory lag, using the ActionConfig thresholds.

	public static long forecast_lag_to_advisory_lag (long the_forecast_lag, ActionConfig action_config) {
		long the_advisory_lag;

		if (the_forecast_lag >= action_config.get_advisory_dur_year()) {
			the_advisory_lag = ADVISORY_LAG_YEAR;
		} else if (the_forecast_lag >= action_config.get_advisory_dur_month()) {
			the_advisory_lag = ADVISORY_LAG_MONTH;
		} else if (the_forecast_lag >= action_config.get_advisory_dur_week()) {
			the_advisory_lag = ADVISORY_LAG_WEEK;
		} else {
			the_advisory_lag = ADVISORY_LAG_DAY;
		}
		
		return the_advisory_lag;
	}

	// Convert an advisory lag into an advisory name, using the advisory windows in ActionConfig.
	// Returns null if there is no name for the given advisory lag.

	public static String advisory_lag_to_name (long the_advisory_lag, ActionConfig action_config) {
		int n = action_config.get_adv_window_count();
		for (int j = 0; j < n; ++j) {
			if (action_config.get_adv_window_start_off(j) == 0L && action_config.get_adv_window_end_off(j) == the_advisory_lag) {
				return action_config.get_adv_window_name(j);
			}
		}
		return null;
	}

	// Convert an advisory lag into an advisory name, using USGS_AftershockForecast.Duration.
	// Returns null if there is no name for the given advisory lag.

	public static String advisory_lag_to_name_via_enum (long the_advisory_lag) {
		String name = null;

		if (the_advisory_lag == ADVISORY_LAG_DAY) {
			name = USGS_AftershockForecast.Duration.ONE_DAY.toString();
		} else if (the_advisory_lag == ADVISORY_LAG_WEEK) {
			name = USGS_AftershockForecast.Duration.ONE_WEEK.toString();
		} else if (the_advisory_lag == ADVISORY_LAG_MONTH) {
			name = USGS_AftershockForecast.Duration.ONE_MONTH.toString();
		} else if (the_advisory_lag == ADVISORY_LAG_YEAR) {
			name = USGS_AftershockForecast.Duration.ONE_YEAR.toString();
		}

		return name;
	}

	// Convert an advisory name to an advisory lag, using USGS_AftershockForecast.Duration.
	// Comparison is case-insensitive and ignores leading and trailing white space.
	// Returns 0L if the name is not recognized.

	public static long advisory_name_to_lag_via_enum (String name) {
		String s = name.trim();
		long the_advisory_lag = 0L;

		if (s.equalsIgnoreCase (USGS_AftershockForecast.Duration.ONE_DAY.toString())) {
			the_advisory_lag = ADVISORY_LAG_DAY;
		} else if (s.equalsIgnoreCase (USGS_AftershockForecast.Duration.ONE_WEEK.toString())) {
			the_advisory_lag = ADVISORY_LAG_WEEK;
		} else if (s.equalsIgnoreCase (USGS_AftershockForecast.Duration.ONE_MONTH.toString())) {
			the_advisory_lag = ADVISORY_LAG_MONTH;
		} else if (s.equalsIgnoreCase (USGS_AftershockForecast.Duration.ONE_YEAR.toString())) {
			the_advisory_lag = ADVISORY_LAG_YEAR;
		}

		return the_advisory_lag;
	}

	// Convert a forecast lag to a flag indicating if sequence specific results should be calculated.

	public static boolean forecast_lag_to_f_seq_spec (long the_forecast_lag, ActionConfig action_config) {
		return the_forecast_lag >= action_config.get_seq_spec_min_lag();
	}


	//----- Root parameters -----

	// Time results were prepared, in milliseconds since the epoch.
	// This is used as the start time for the forecasts, and typically equals the mainshock time plus the forecast lag.

	public long result_time = 0L;

	// Advisory duration, in milliseconds.

	public long advisory_lag = ADVISORY_LAG_WEEK;

	// Injectable text for PDL JSON files, or "" for none.

	public String injectable_text = "";


	//----- Catalog results -----

	// Catalog result available flag.

	public boolean catalog_result_avail = false;

	// Start time of aftershock sequence, in milliseconds since the epoch.
	// This is not the time of the first aftershock, because there may be an interval of no aftershock.

	public long catalog_start_time = 0L;

	// End time of aftershock sequence, in milliseconds since the epoch.
	// This is not the time of the last aftershock, because there may be an interval of no aftershock.

	public long catalog_end_time = 0L;

	// Number of aftershocks.

	public int catalog_eqk_count = 0;

	// Maximum magnitude of any aftershock (0.0 if there are no aftershocks).

	public double catalog_max_mag = 0.0;

	// Event id of the aftershock with maximum magnitude ("" if there are no aftershocks).
	// Note: It is prefixed with "-" if the event is a foreshock.

	public String catalog_max_event_id = "";

	// catalog_aftershocks - List of aftershocks.
	// Note: This field is not marshaled, because aftershock lists are stored in separate database records.

	public CompactEqkRupList catalog_aftershocks = null;

	// catalog_comcat_aftershocks - List of aftershocks, as returned by ComCat.
	// Note: This field is not marshaled, and is not rebuilt, because it is intended to be used
	// only immediately after the results are calculated;  mainly for finding foreshocks.

	public ObsEqkRupList catalog_comcat_aftershocks = null;

	// catalog_fit_start_days - Start time of fitting interval in days since the mainshock. [v2]
	// Can be SEARCH_PARAM_OMIT if unknown.

	public double catalog_fit_start_days = SEARCH_PARAM_OMIT;

	// catalog_fit_end_days - End time of fitting interval in days since the mainshock. [v2]
	// Can be SEARCH_PARAM_OMIT if unknown.
	// (May extend slightly past the end of data if needed to ensure fitting interval has a positive length.)

	public double catalog_fit_end_days = SEARCH_PARAM_OMIT;

	// Get the start time of the fitting interval in days since the mainshock.
	// Returns the default value if the time is unknown.

	public double get_catalog_fit_start_days (double def_start) {
		return ((catalog_fit_start_days < SEARCH_PARAM_TEST) ? catalog_fit_start_days : def_start);
	}

	// Get the end time of the fitting interval in days since the mainshock.
	// Returns the default value if the time is unknown.

	public double get_catalog_fit_end_days (double def_end) {
		return ((catalog_fit_end_days < SEARCH_PARAM_TEST) ? catalog_fit_end_days : def_end);
	}

	// The smallest allowed fitting interval, in days.
	// In practice this should not come into play.  It exists to ensure preconditions are satisfied.

	public static final double TINY_FIT_INTERVAL_DAYS = 0.001;

	// Calculate the start and end of the fitting interval from the forecast parameters.

	public void calc_catalog_fit_interval (ForecastMainshock fcmain, ForecastParameters params) {

		// Parameters must have mainshock, aftershock search region

		if (!( fcmain.mainshock_avail && params.aftershock_search_avail )) {
			catalog_fit_start_days = SEARCH_PARAM_OMIT;
			catalog_fit_end_days = SEARCH_PARAM_OMIT;
			return;
		}

		// The requested minimum duration of the fitting interval

		double req_min_dur = SimpleUtils.millis_to_days ((new ActionConfig()).get_data_fit_dur_min());

		// The latest time that allows minumum duration before the end of the data,
		// coerced to be after the mainshock

		double x = Math.max(0.0, params.max_days - req_min_dur);

		// Reduce the fit start to be no later than x, and then coerce into the data range

		catalog_fit_start_days = SimpleUtils.clip_max_min (params.min_days, params.max_days,
			Math.min(x, params.fit_start_inset)
		);

		// The earliest time that allows minimum duration after both the fit start and mainshock

		double y = Math.max(0.0, catalog_fit_start_days) + req_min_dur;

		// Increase the fit end to be no earlier than y, and then coerce to be
		// after the fit start and before the data end
		// (The extra TINY_FIT_INTERVAL_DAYS ensures that the fitting interval has non-zero length,
		// even if it requires extending the fitting interval slightly past the end of the data;
		// in practice this should not happen.)

		catalog_fit_end_days = SimpleUtils.clip_max_min (catalog_fit_start_days + TINY_FIT_INTERVAL_DAYS, params.max_days,
			Math.max(y, params.max_days - params.fit_end_inset)
		);

		return;
	}

	// set_default_catalog_results - Set catalog results to default values.

	public void set_default_catalog_results () {
		catalog_start_time = 0L;
		catalog_end_time = 0L;
		catalog_eqk_count = 0;
		catalog_max_mag = 0.0;
		catalog_max_event_id = "";
		catalog_aftershocks = null;
		catalog_comcat_aftershocks = null;
		catalog_fit_start_days = SEARCH_PARAM_OMIT;
		catalog_fit_end_days = SEARCH_PARAM_OMIT;
		return;
	}

	// calc_catalog_results - Calculate catalog results.

	public void calc_catalog_results (ForecastMainshock fcmain, ForecastParameters params) {

		// Parameters must have mainshock, aftershock search region

		if (!( fcmain.mainshock_avail && params.aftershock_search_avail )) {
			set_default_catalog_results();
			catalog_result_avail = false;
			return;
		}

		// Retrieve list of aftershocks in the search region

		ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
		//ObsEqkRupList catalog_comcat_aftershocks;		// if this isn't an object field

		try {
			ComcatOAFAccessor accessor = new ComcatOAFAccessor();
			catalog_comcat_aftershocks = accessor.fetchAftershocks(mainshock, params.min_days, params.max_days,
				params.min_depth, params.max_depth, params.aftershock_search_region, false, params.min_mag);
		} catch (Exception e) {
			throw new RuntimeException("ForecastResults.calc_catalog_results: Comcat exception", e);
		}

		// Save catalog and info

		long eventTime = mainshock.getOriginTime();
		catalog_start_time = eventTime + SimpleUtils.days_to_millis (params.min_days);
		catalog_end_time = eventTime + SimpleUtils.days_to_millis (params.max_days);

		calc_catalog_fit_interval (fcmain, params);

		catalog_eqk_count = catalog_comcat_aftershocks.size();
		catalog_aftershocks = new CompactEqkRupList (catalog_comcat_aftershocks);

		if (catalog_eqk_count == 0) {
			catalog_max_mag = 0.0;
			catalog_max_event_id = "";
		} else {
			catalog_max_mag = -Double.MAX_VALUE;
			for (ObsEqkRupture rup : catalog_comcat_aftershocks) {
				double mag = rup.getMag();
				if (mag > catalog_max_mag) {
					catalog_max_mag = mag;
					//catalog_max_event_id = rup.getEventId();
					set_cmei_fsev (rup.getEventId(), rup.getOriginTime() < eventTime);
				}
			}
		}

		catalog_result_avail = true;
		return;
	}

	// calc_catalog_results_from_known_as - Calculate catalog results from a known aftershock sequence.
	// Note: This function does not retain the known_as list or any of the ObsEqkRupture objects
	//  in the list.  It creates a new list of newly-allocated ObsEqkRupture objects.
	// Note: If any supplied aftershock does not contain a hypocenter, then the hypocenter of
	//  the mainshock is used for that aftershock.
	// Note: If any supplied aftershock does not contain an event ID (non-null and non-empty),
	//  then an event ID is generated and used for that aftershock.
	// Note: The supplied aftershock sequence is filtered by time and magnitude, according to
	//  params.min_days, params.max_days, and params.min_mag.  It is not filtered by location or depth.
	// Note: This function can be used by the GUI to calculate forecasts when the catalog has
	//  already been fetched from Comcat or another source.
	// Note: This function can be used to inject simulated aftershock sequences for testing.

	public void calc_catalog_results_from_known_as (ForecastMainshock fcmain,
		ForecastParameters params, List<ObsEqkRupture> known_as) {

		// Parameters must have mainshock

		if (!( fcmain.mainshock_avail )) {
			set_default_catalog_results();
			catalog_result_avail = false;
			return;
		}

		// Get the time range

		ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
		//ObsEqkRupList catalog_comcat_aftershocks;		// if this isn't an object field

		long eventTime = mainshock.getOriginTime();
		catalog_start_time = eventTime + SimpleUtils.days_to_millis (params.min_days);
		catalog_end_time = eventTime + SimpleUtils.days_to_millis (params.max_days);

		calc_catalog_fit_interval (fcmain, params);

		// Loop over supplied aftershocks

		catalog_comcat_aftershocks = new ObsEqkRupList();

		//int event_num = 0;

		for (ObsEqkRupture rup : known_as) {

			// Extract time, magnitude, event id, hypocenter

			String rup_event_id = rup.getEventId();
			long rup_time = rup.getOriginTime();
			double rup_mag = rup.getMag();
			Location hypo = rup.getHypocenterLocation();

			// Count it

			//++event_num;

			// If it passes the filter ...

			if (rup_time >= catalog_start_time
				&& rup_time <= catalog_end_time
				&& rup_mag >= params.min_mag) {

				// If no hypocenter supplied, replace with mainshock hypocenter

				if (hypo == null) {
					hypo = fcmain.get_eqk_location();
				}

				// If no event id supplied, replace with a generated id

				if (rup_event_id == null || rup_event_id.trim().isEmpty()) {
					//rup_event_id = "kas_" + Integer.toString(event_num);
					rup_event_id = EventIDGenerator.generate_id();
				}

				// Add to our catalog

				catalog_comcat_aftershocks.add (new ObsEqkRupture (rup_event_id, rup_time, hypo, rup_mag));
			}
		}

		// Save catalog and info

		catalog_eqk_count = catalog_comcat_aftershocks.size();
		catalog_aftershocks = new CompactEqkRupList (catalog_comcat_aftershocks);

		if (catalog_eqk_count == 0) {
			catalog_max_mag = 0.0;
			catalog_max_event_id = "";
		} else {
			catalog_max_mag = -Double.MAX_VALUE;
			for (ObsEqkRupture rup : catalog_comcat_aftershocks) {
				double mag = rup.getMag();
				if (mag > catalog_max_mag) {
					catalog_max_mag = mag;
					//catalog_max_event_id = rup.getEventId();
					set_cmei_fsev (rup.getEventId(), rup.getOriginTime() < eventTime);
				}
			}
		}

		catalog_result_avail = true;
		return;
	}

	// copy_catalog_results_from - Copy catalog results from another object.
	// Note: This is a deep copy, except for catalog_comcat_aftershocks which is shared
	// between this object and the other object (because it is not intended to be
	// modified, and there is no simple way to deep-copy it).

	public void copy_catalog_results_from (ForecastResults other) {

		// Handle case where catalog results are not available

		if (!( other.catalog_result_avail )) {
			set_default_catalog_results();
			this.catalog_result_avail = false;
			return;
		}

		// Catalog results are available, copy them
			
		this.catalog_result_avail = true;

		this.catalog_start_time = other.catalog_start_time;
		this.catalog_end_time = other.catalog_end_time;
		this.catalog_eqk_count = other.catalog_eqk_count;
		this.catalog_max_mag = other.catalog_max_mag;
		this.catalog_max_event_id = other.catalog_max_event_id;
		this.catalog_aftershocks = other.catalog_aftershocks.make_deep_copy();
		this.catalog_comcat_aftershocks = other.catalog_comcat_aftershocks;
		this.catalog_fit_start_days = other.catalog_fit_start_days;
		this.catalog_fit_end_days = other.catalog_fit_end_days;
		return;
	}

	// rebuild_catalog_results - Rebuild transient catalog results.

	public void rebuild_catalog_results (ForecastMainshock fcmain, ForecastParameters params, CompactEqkRupList the_catalog_aftershocks) {

		// If there are results to rebuild ...

		if (catalog_result_avail) {

			// Parameters must have mainshock, aftershock search region

			if (!( fcmain.mainshock_avail && params.aftershock_search_avail )) {
				throw new RuntimeException("ForecastResults.rebuild_catalog_results: Invalid preconditions");
			}

			// Check for supplied catalog

			if (!( the_catalog_aftershocks != null )) {
				throw new RuntimeException("ForecastResults.rebuild_catalog_results: No aftershock catalog supplied");
			}

			if (!( the_catalog_aftershocks.size() == catalog_eqk_count )) {
				throw new RuntimeException("ForecastResults.rebuild_catalog_results: Aftershock catalog size mismatch, expecting " + catalog_eqk_count + ", got " + the_catalog_aftershocks.size());
			}

			// Save catalog

			catalog_aftershocks = the_catalog_aftershocks;
		}

		return;
	}


	// Prefix applied to event name to indicate it is a foreshock.

	public static final String FSEV_PREFIX = "-";

	// Construct an event name, possibly containing a foreshock prefix.

	public static String make_fsev (String event_id, boolean f_foreshock) {
		return f_foreshock ? (FSEV_PREFIX + event_id) : event_id;
	}

	// Extract the event id from an event name that may contain a foreshock prefix.

	public static String extract_fsev_event_id (String fsev) {
		return (fsev.startsWith (FSEV_PREFIX)) ? (fsev.substring (FSEV_PREFIX.length())) : fsev;
	}

	// Extract the foreshock flag from an event name that may contain a foreshock prefix.

	public static boolean extract_fsev_f_foreshock (String fsev) {
		return fsev.startsWith (FSEV_PREFIX);
	}

	// Set catalog_max_event_id to an event name, possibly containing a foreshock prefix.

	public void set_cmei_fsev (String event_id, boolean f_foreshock) {
		catalog_max_event_id = make_fsev (event_id, f_foreshock);
		return;
	}

	// Extract the event id from catalog_max_event_id, which may contain a foreshock prefix.

	public String extract_cmei_event_id () {
		return extract_fsev_event_id (catalog_max_event_id);
	}

	// Extract the foreshock flag from catalog_max_event_id, which may contain a foreshock prefix.

	public boolean extract_cmei_f_foreshock () {
		return extract_fsev_f_foreshock (catalog_max_event_id);
	}


	//----- Generic results -----

	// Generic result available flag.

	public boolean generic_result_avail = false;

	// Generic results summary.

	public RJ_Summary_Generic generic_summary = null;

	// Generic results JSON.
	// Must be non-null, but can be empty to indicate not available.

	public String generic_json = "";

	// True if results sent to PDL.

	public boolean generic_pdl = false;

	// Generic aftershock model.
	// This field is not marshaled.

	public RJ_AftershockModel_Generic generic_model = null;

	// set_default_generic_results - Set generic results to default values.

	public void set_default_generic_results () {
		generic_summary = null;
		generic_json = "";
		generic_pdl = false;
		generic_model = null;
		return;
	}

	// calc_generic_results - Calculate generic results.

	public void calc_generic_results (ForecastMainshock fcmain, ForecastParameters params) {

		// We need to have catalog results, mainshock parameters, and generic parameters

		if (!( (params.generic_calc_meth != CALC_METH_SUPPRESS)
				&& catalog_result_avail
				&& fcmain.mainshock_avail 
				&& params.generic_avail )) {
			set_default_generic_results();
			generic_result_avail = false;
			return;
		}

		try {

			// Build the generic model

			ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
			generic_model = new RJ_AftershockModel_Generic (mainshock.getMag(), params.generic_params);

			// Save the summary

			generic_summary = new RJ_Summary_Generic (generic_model);

			// Build the forecast

			USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
				mainshock.getOriginTime(),		// event_time
				result_time,					// result_time
				advisory_lag,					// advisory_lag
				injectable_text,				// injectable_text
				params.next_scheduled_lag,		// next_scheduled_lag
				null,							// user_param_map
				params.get_resolved_fcopt_min_mag_bins()	// min_mag_bins
			);

			// Add parameters for magnitude of completeness and search region

			if (params.mag_comp_avail && params.mag_comp_params != null) {
				params.mag_comp_params.get_magCompFn().get_display_params (
					fc_info.user_param_map, params.mag_comp_params.get_magCat (fcmain.mainshock_mag));
			}
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (fc_info.user_param_map);
			}

			// Get the JSON String

			generic_json = fc_info.make_forecast_json (generic_model, catalog_aftershocks, null);
			if (generic_json == null) {
				throw new RuntimeException("ForecastResults.calc_generic_results: Unable to generate JSON");
			}

		} catch (Exception e) {
			throw new RuntimeException("ForecastResults.calc_generic_results: Exception building generic forecast", e);
		}

		// Done

		generic_pdl = false;
		generic_result_avail = true;
		return;
	}

	// rebuild_generic_results - Rebuild transient generic results.

	public void rebuild_generic_results (ForecastMainshock fcmain, ForecastParameters params) {

		// If there are results to rebuild ...

		if (generic_result_avail) {

			// We need to have catalog results, mainshock parameters, and generic parameters

			if (!( (params.generic_calc_meth != CALC_METH_SUPPRESS)
					&& catalog_result_avail
					&& fcmain.mainshock_avail 
					&& params.generic_avail )) {
				throw new RuntimeException("ForecastResults.rebuild_generic_results: Invalid preconditions");
			}

			try {

				// Build the generic model

				ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
				generic_model = new RJ_AftershockModel_Generic (mainshock.getMag(), params.generic_params);

			} catch (Exception e) {
				throw new RuntimeException("ForecastResults.rebuild_generic_results: Exception building generic forecast", e);
			}
		}

		return;
	}


	//----- Sequence specific results -----

	// Sequence specific result available flag.

	public boolean seq_spec_result_avail = false;

	// Sequence specific results summary.

	public RJ_Summary_SequenceSpecific seq_spec_summary = null;

	// Sequence specific results JSON.
	// Must be non-null, but can be empty to indicate not available.

	public String seq_spec_json = "";

	// True if results sent to PDL.

	public boolean seq_spec_pdl = false;

	// Sequence specific aftershock model.
	// This field is not marshaled.

	public RJ_AftershockModel_SequenceSpecific seq_spec_model = null;

	// set_default_seq_spec_results - Set sequence specific results to default values.

	public void set_default_seq_spec_results () {
		seq_spec_summary = null;
		seq_spec_json = "";
		seq_spec_pdl = false;
		seq_spec_model = null;
		return;
	}

	// calc_seq_spec_results - Calculate sequence specific results.

	public void calc_seq_spec_results (ForecastMainshock fcmain, ForecastParameters params, boolean f_seq_spec) {

		// We need to have catalog results, mainshock parameters, magnitude of completeness parameters, and sequence specific parameters

		if (!( f_seq_spec
				&& (params.seq_spec_calc_meth != CALC_METH_SUPPRESS)
				&& catalog_result_avail
				&& fcmain.mainshock_avail 
				&& params.mag_comp_avail
				&& params.seq_spec_avail )) {
			set_default_seq_spec_results();
			seq_spec_result_avail = false;
			return;
		}

		try {

			// Build the sequence specific model

			ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
			seq_spec_model = new RJ_AftershockModel_SequenceSpecific (mainshock, catalog_aftershocks,
				get_catalog_fit_start_days (params.min_days), get_catalog_fit_end_days (params.max_days),
				params.mag_comp_params, params.seq_spec_params);

			// Save the summary

			seq_spec_summary = new RJ_Summary_SequenceSpecific (seq_spec_model);

			// Build the forecast

			USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
				mainshock.getOriginTime(),		// event_time
				result_time,					// result_time
				advisory_lag,					// advisory_lag
				injectable_text,				// injectable_text
				params.next_scheduled_lag,		// next_scheduled_lag
				null,							// user_param_map
				params.get_resolved_fcopt_min_mag_bins()	// min_mag_bins
			);

			// Add parameters for magnitude of completeness and search region

			if (params.mag_comp_avail && params.mag_comp_params != null) {
				params.mag_comp_params.get_magCompFn().get_display_params (
					fc_info.user_param_map, params.mag_comp_params.get_magCat (fcmain.mainshock_mag));
			}
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (fc_info.user_param_map);
			}

			// Get the JSON String

			seq_spec_json = fc_info.make_forecast_json (seq_spec_model, catalog_aftershocks, null);
			if (seq_spec_json == null) {
				throw new RuntimeException("ForecastResults.calc_seq_spec_results: Unable to generate JSON");
			}

		} catch (Exception e) {
			throw new RuntimeException("ForecastResults.calc_seq_spec_results: Exception building sequence specific forecast", e);
		}

		// Done

		seq_spec_pdl = false;
		seq_spec_result_avail = true;
		return;
	}

	// rebuild_seq_spec_results - Rebuild transient sequence specific results.

	public void rebuild_seq_spec_results (ForecastMainshock fcmain, ForecastParameters params) {

		// If there are results to rebuild ...

		if (seq_spec_result_avail) {

			// We need to have catalog results, mainshock parameters, magnitude of completeness parameters, and sequence specific parameters

			if (!( (params.seq_spec_calc_meth != CALC_METH_SUPPRESS)
					&& catalog_result_avail
					&& fcmain.mainshock_avail 
					&& params.mag_comp_avail
					&& params.seq_spec_avail )) {
				throw new RuntimeException("ForecastResults.rebuild_seq_spec_results: Invalid preconditions");
			}

			try {

				// Build the sequence specific model

				ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
				seq_spec_model = new RJ_AftershockModel_SequenceSpecific (mainshock, catalog_aftershocks,
					get_catalog_fit_start_days (params.min_days), get_catalog_fit_end_days (params.max_days),
					params.mag_comp_params, params.seq_spec_params);

			} catch (Exception e) {
				throw new RuntimeException("ForecastResults.rebuild_seq_spec_results: Exception building sequence specific forecast", e);
			}
		}

		return;
	}


	//----- Bayesian results -----

	// Bayesian result available flag.

	public boolean bayesian_result_avail = false;

	// Bayesian results summary.

	public RJ_Summary_Bayesian bayesian_summary = null;

	// Bayesian results JSON.
	// Must be non-null, but can be empty to indicate not available.

	public String bayesian_json = "";

	// True if results sent to PDL.

	public boolean bayesian_pdl = false;

	// Bayesian aftershock model.
	// This field is not marshaled.

	public RJ_AftershockModel_Bayesian bayesian_model = null;

	// set_default_bayesian_results - Set bayesian results to default values.

	public void set_default_bayesian_results () {
		bayesian_summary = null;
		bayesian_json = "";
		bayesian_pdl = false;
		bayesian_model = null;
		return;
	}

	// calc_bayesian_results - Calculate bayesian results.

	public void calc_bayesian_results (ForecastMainshock fcmain, ForecastParameters params) {

		// We need to have catalog results, mainshock parameters, compatible generic and sequence specific models

		if (!( (params.bayesian_calc_meth != CALC_METH_SUPPRESS)
				&& catalog_result_avail
				&& fcmain.mainshock_avail 
				&& generic_result_avail
				&& seq_spec_result_avail
				&& RJ_AftershockModel_Bayesian.areModelsEquivalent(generic_model, seq_spec_model) )) {
			set_default_bayesian_results();
			bayesian_result_avail = false;
			return;
		}

		try {

			// Build the bayesian model

			ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
			bayesian_model = new RJ_AftershockModel_Bayesian (generic_model, seq_spec_model);

			// Save the summary

			bayesian_summary = new RJ_Summary_Bayesian (bayesian_model);

			// Build the forecast

			USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
				mainshock.getOriginTime(),		// event_time
				result_time,					// result_time
				advisory_lag,					// advisory_lag
				injectable_text,				// injectable_text
				params.next_scheduled_lag,		// next_scheduled_lag
				null,							// user_param_map
				params.get_resolved_fcopt_min_mag_bins()	// min_mag_bins
			);

			// Add parameters for magnitude of completeness and search region

			if (params.mag_comp_avail && params.mag_comp_params != null) {
				params.mag_comp_params.get_magCompFn().get_display_params (
					fc_info.user_param_map, params.mag_comp_params.get_magCat (fcmain.mainshock_mag));
			}
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (fc_info.user_param_map);
			}

			// Get the JSON String

			bayesian_json = fc_info.make_forecast_json (bayesian_model, catalog_aftershocks, null);
			if (bayesian_json == null) {
				throw new RuntimeException("ForecastResults.calc_bayesian_results: Unable to generate JSON");
			}

		} catch (Exception e) {
			//throw new RuntimeException("ForecastResults.calc_bayesian_results: Exception building bayesian forecast", e);

			// In case of any error, just don't do the Bayesian

			set_default_bayesian_results();
			bayesian_result_avail = false;
			return;
		}

		// Done

		bayesian_pdl = false;
		bayesian_result_avail = true;
		return;
	}

	// rebuild_bayesian_results - Rebuild transient bayesian results.

	public void rebuild_bayesian_results (ForecastMainshock fcmain, ForecastParameters params) {

		// If there are results to rebuild ...

		if (bayesian_result_avail) {

			// We need to have catalog results, mainshock parameters, compatible generic and sequence specific models

			if (!( (params.bayesian_calc_meth != CALC_METH_SUPPRESS)
					&& catalog_result_avail
					&& fcmain.mainshock_avail 
					&& generic_result_avail
					&& seq_spec_result_avail
					&& RJ_AftershockModel_Bayesian.areModelsEquivalent(generic_model, seq_spec_model) )) {
				throw new RuntimeException("ForecastResults.rebuild_bayesian_results: Invalid preconditions");
			}

			try {

				// Build the bayesian model

				ObsEqkRupture mainshock = fcmain.get_eqk_rupture();
				bayesian_model = new RJ_AftershockModel_Bayesian (generic_model, seq_spec_model);

			} catch (Exception e) {
				throw new RuntimeException("ForecastResults.rebuild_bayesian_results: Exception building bayesian forecast", e);
			}
		}

		return;
	}


	//----- ETAS results -----

	// ETAS result available flag. [v2]

	public boolean etas_result_avail = false;

	// ETAS result code, see ETAS_RESCODE_XXXXX. [v2]

	public int etas_rescode = ETAS_RESCODE_UNSUPPORTED;

	// ETAS forecast outcome, can be null if none. [v2]
	// Note that even if etas_result_avail is true, this can be null, and this can be non-null but not contain a forecast JSON.
	// Note that if etas_rescode does not indicate success, then any forecast in etas_outcome should not be sent to PDL.

	public OEtasOutcome etas_outcome = null;

	// ETAS log information, can be null if none. [v2]
	// This field is not marshaled.

	public OEtasLogInfo etas_log_info = null;

	// True if results sent to PDL. [v2]

	public boolean etas_pdl = false;

	// Return true if we have an ETAS JSON.

	public final boolean has_etas_json () {
		return (etas_result_avail && OEExecEnvironment.is_etas_successful(etas_rescode) && (etas_outcome != null) && etas_outcome.has_etas_json());
	}

	// Return the ETAS JSON.
	// Before calling, check if the ETAS JSON is available by calling has_etas_json().

	public final String get_etas_json () {
		return etas_outcome.get_etas_json();
	}

	// set_default_etas_results - Set ETAS results to default values.

	public void set_default_etas_results () {
		etas_rescode = ETAS_RESCODE_UNSUPPORTED;
		etas_outcome = null;
		etas_log_info = null;
		etas_pdl = false;
		return;
	}

	// Set the reason why ETAS is being skipped.
	// The log_string can be null or empty if none.

	private void set_etas_skip_reason (int rescode, String log_string) {
		set_default_etas_results();
		etas_rescode = rescode;
		etas_log_info = OEExecEnvironment.make_etas_log_info (rescode, log_string);
		etas_result_avail = true;
		return;
	}

	// calc_etas_results - Calculate ETAS results.

	public void calc_etas_results (ForecastMainshock fcmain, ForecastParameters params) {

		ActionConfig my_action_config = new ActionConfig();

		// If ETAS is disabled, then we can't have ETAS results

		if (!( my_action_config.get_is_etas_enabled() )) {
			set_default_etas_results();
			etas_result_avail = false;
			return;
		}

		// We need to have ETAS parameters

		if (!( (params.etas_fetch_meth != CALC_METH_SUPPRESS)
				&& params.etas_avail )) {
			set_etas_skip_reason (ETAS_RESCODE_NO_PARAMS, OEExecEnvironment.etas_result_to_string (ETAS_RESCODE_NO_PARAMS));
			return;
		}

		// We need to have magnitude of completeness parameters with Helmstetter function

		if (!( params.mag_comp_avail
				&& params.mag_comp_params.get_magCompFn().is_page_or_constant() )) {
			set_etas_skip_reason (ETAS_RESCODE_MAG_COMP_FORM, OEExecEnvironment.etas_result_to_string (ETAS_RESCODE_MAG_COMP_FORM));
			return;
		}

		// We need to have catalog results and mainshock parameters

		if (!( catalog_result_avail
				&& fcmain.mainshock_avail )) {
			set_etas_skip_reason (ETAS_RESCODE_NO_DATA, OEExecEnvironment.etas_result_to_string (ETAS_RESCODE_NO_DATA));
			return;
		}

		// We need to be eligible based on magnitude
			
		double eli_mag_cat = params.mag_comp_params.get_magCat (fcmain.mainshock_mag);

		if (!( params.etas_params.check_eligible (fcmain.mainshock_mag, catalog_max_mag, eli_mag_cat) )) {
			set_etas_skip_reason (ETAS_RESCODE_NOT_ELIGIBLE, OEExecEnvironment.etas_result_to_string (ETAS_RESCODE_NOT_ELIGIBLE) + ", mainshock_mag = " + fcmain.mainshock_mag + ", catalog_max_mag = " + catalog_max_mag);
			System.out.println ("Not eligible for ETAS: mainshock_mag = " + fcmain.mainshock_mag + ", catalog_max_mag = " + catalog_max_mag);
			return;
		}

		try {
		
			// Build the forecast information
		
			USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
				fcmain.mainshock_time,			// event_time
				result_time,					// result_time
				advisory_lag,					// advisory_lag
				injectable_text,				// injectable_text
				params.next_scheduled_lag,		// next_scheduled_lag
				null,							// user_param_map
				params.get_resolved_fcopt_min_mag_bins()	// min_mag_bins
			);
		
			// Add parameters for magnitude of completeness and search region
		
			if (params.mag_comp_avail && params.mag_comp_params != null) {
				params.mag_comp_params.get_magCompFn().get_display_params (
					fc_info.user_param_map, params.mag_comp_params.get_magCat (fcmain.mainshock_mag));
			}
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (fc_info.user_param_map);
			}

			// Build the catalog information

			double the_mag_cat = params.mag_comp_params.get_magCat (fcmain.mainshock_mag);
			double the_mag_top = params.etas_params.suggest_mag_top (the_mag_cat, Math.max(fcmain.mainshock_mag, catalog_max_mag));

			double t_data_begin = params.min_days;
			double t_data_end = get_catalog_fit_end_days (params.max_days);
			double t_fitting = get_catalog_fit_start_days (Math.max(0.0, params.min_days));
			double t_forecast = Math.max(t_data_end, SimpleUtils.millis_to_days (fc_info.start_time - fcmain.mainshock_time));

			OEtasCatalogInfo catalog_info = (new OEtasCatalogInfo()).set (
				the_mag_cat,												// double magCat
				the_mag_top,												// double magTop
				params.mag_comp_params.get_magCompFn().getDefaultGUICapF(),	// double capF
				params.mag_comp_params.get_magCompFn().getDefaultGUICapG(),	// double capG
				params.mag_comp_params.get_magCompFn().getDefaultGUICapH(),	// double capH
				t_data_begin,												// double t_data_begin
				t_data_end,													// double t_data_end
				t_fitting,													// double t_fitting
				t_forecast,													// double t_forecast
				fcmain.mainshock_mag,										// double mag_main
				fcmain.mainshock_lat,										// double lat_main
				fcmain.mainshock_lon,										// double lon_main
				fcmain.mainshock_depth										// double depth_main
			);

			// Create multi-thread context

			int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1

			long max_runtime = my_action_config.get_etas_time_limit();
			if (max_runtime == ActionConfigFile.NO_ETAS_TIME_LIMIT) {
				max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
			}

			long progress_time = my_action_config.get_etas_progress_time();
			if (progress_time == ActionConfigFile.NO_ETAS_PROGRESS_TIME) {
				progress_time = SimpleExecTimer.NO_PROGRESS_TIME;	// -1L
			}

			try (
				AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
			){
				SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
				exec_timer.start_timer();

				// Make the ETAS execution environment

				OEExecEnvironment exec_env = new OEExecEnvironment();

				// Create ETAS context

				try {

					// Set up the communication area

					exec_env.setup_comm_area (exec_timer);

					// Select files we want

					exec_env.filename_accepted = null;
					exec_env.filename_mag_comp = null;
					exec_env.filename_log_density = null;
					exec_env.filename_intensity_calc = null;
					exec_env.filename_results = null;
					exec_env.filename_fc_json = null;
					exec_env.filename_marginals = null;

					// Set up the input area

					exec_env.setup_input_area_from_compact (
						params.etas_params,			// OEtasParameters the_etas_params,
						fc_info,					// USGS_ForecastInfo the_forecast_info,
						catalog_info,				// OEtasCatalogInfo the_catalog_info,
						fcmain.get_eqk_rupture(),	// ObsEqkRupture the_obs_mainshock,
						catalog_aftershocks			// CompactEqkRupList the_compact_rup_list
					);

					// Run ETAS!

					exec_env.run_etas();
				}

				// Pass exceptions into the ETAS execution environment

				catch (Exception e) {
					exec_env.report_exception (e);
				}

				// Save results

				etas_rescode = exec_env.etas_rescode;
				etas_log_info = exec_env.make_etas_log_info();

				etas_outcome = null;
				if (exec_env.is_etas_completed()) {
					etas_outcome = exec_env.etas_results;
				}

				// Display result

				System.out.println ();

				if (exec_env.is_etas_successful()) {
					System.out.println ();
					System.out.println (exec_env.get_forecast_summary_string());
					System.out.println ("ETAS succeeded");
				}
				else if (exec_env.is_etas_completed()) {
					System.out.println ();
					//System.out.println (exec_env.get_forecast_summary_string());
					System.out.println ("ETAS forecast rejected");
				}
				else {
					System.out.println ("ETAS failed, result code = " + exec_env.get_rescode_as_string());
				}

				if (etas_log_info.has_etas_log_string()) {
					System.out.println ("ETAS Log: " + etas_log_info.etas_log_string);
				}

				if (etas_log_info.has_etas_abort_message()) {
					System.out.println (etas_log_info.etas_abort_message);
				}

				// Stop timer

				exec_timer.stop_timer();

				long elapsed_time = exec_timer.get_total_runtime();
				long elapsed_seconds = (elapsed_time + 500L) / 1000L;

				System.out.println ();
				System.out.println ("Elapsed time = " + elapsed_seconds + " seconds");
			}

		
		} catch (Exception e) {
			throw new RuntimeException("ForecastResults.calc_etas_results: Exception building ETAS forecast", e);
		}
		
		// Done
		
		etas_pdl = false;
		etas_result_avail = true;

		return;
	}

	// rebuild_etas_results - Rebuild transient ETAS results.

	public void rebuild_etas_results (ForecastMainshock fcmain, ForecastParameters params) {

		// Nothing to do (we can't rebuild the log info) ...

		etas_log_info = null;
		return;
	}

	// calc_etas_catalog_info - Calculate ETAS OEtasCatalogInfo structure.
	// Also calculate the USGS_ForecastInfo structure.
	// Return null if the structures cannot be calculated.
	// An exception also indicates the structure cannot be calculated.
	// Note: This function supports testing.  It lets the caller obtain the
	// same OEtasCatalogInfo structure that would be used by the sutomatic
	// system.  It requires only that catalog info be loaded (along with
	// result time, advisory lag, and injectable text).
	// Note: The code is the same as the first part of calc_etas_results().
	// Note: A successful return implies that we have ETAS parameters,
	// mainshock parameters, and catalog results.

	public static class ForecastAndCatalogInfo {
		public USGS_ForecastInfo fc_info;
		public OEtasCatalogInfo catalog_info;

		public ForecastAndCatalogInfo (USGS_ForecastInfo fc_info,OEtasCatalogInfo catalog_info) {
			this.fc_info = fc_info;
			this.catalog_info = catalog_info;
		}
	}

	public ForecastAndCatalogInfo calc_etas_catalog_info (ForecastMainshock fcmain, ForecastParameters params) {

		ActionConfig my_action_config = new ActionConfig();

		// If ETAS is disabled, then we can't have ETAS results

		if (!( my_action_config.get_is_etas_enabled() )) {
			return null;
		}

		// We need to have ETAS parameters

		if (!( (params.etas_fetch_meth != CALC_METH_SUPPRESS)
				&& params.etas_avail )) {
			return null;
		}

		// We need to have magnitude of completeness parameters with Helmstetter function

		if (!( params.mag_comp_avail
				&& params.mag_comp_params.get_magCompFn().is_page_or_constant() )) {
			return null;
		}

		// We need to have catalog results and mainshock parameters

		if (!( catalog_result_avail
				&& fcmain.mainshock_avail )) {
			return null;
		}

		// We need to be eligible based on magnitude (we skip this test)

		//	double eli_mag_cat = params.mag_comp_params.get_magCat (fcmain.mainshock_mag);
		//
		//  if (!( params.etas_params.check_eligible (fcmain.mainshock_mag, catalog_max_mag, eli_mag_cat) )) {
		//  	return null;
		//  }

		// Build the forecast information
		
		USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
			fcmain.mainshock_time,			// event_time
			result_time,					// result_time
			advisory_lag,					// advisory_lag
			injectable_text,				// injectable_text
			params.next_scheduled_lag,		// next_scheduled_lag
			null,							// user_param_map
			params.get_resolved_fcopt_min_mag_bins()	// min_mag_bins
		);
		
		// Add parameters for magnitude of completeness and search region
		
		if (params.mag_comp_avail && params.mag_comp_params != null) {
			params.mag_comp_params.get_magCompFn().get_display_params (
				fc_info.user_param_map, params.mag_comp_params.get_magCat (fcmain.mainshock_mag));
		}
		if (params.aftershock_search_avail && params.aftershock_search_region != null) {
			params.aftershock_search_region.get_display_params (fc_info.user_param_map);
		}

		// Build the catalog information

		double the_mag_cat = params.mag_comp_params.get_magCat (fcmain.mainshock_mag);
		double the_mag_top = params.etas_params.suggest_mag_top (the_mag_cat, Math.max(fcmain.mainshock_mag, catalog_max_mag));

		double t_data_begin = params.min_days;
		double t_data_end = get_catalog_fit_end_days (params.max_days);
		double t_fitting = get_catalog_fit_start_days (Math.max(0.0, params.min_days));
		double t_forecast = Math.max(t_data_end, SimpleUtils.millis_to_days (fc_info.start_time - fcmain.mainshock_time));

		OEtasCatalogInfo catalog_info = (new OEtasCatalogInfo()).set (
			the_mag_cat,												// double magCat
			the_mag_top,												// double magTop
			params.mag_comp_params.get_magCompFn().getDefaultGUICapF(),	// double capF
			params.mag_comp_params.get_magCompFn().getDefaultGUICapG(),	// double capG
			params.mag_comp_params.get_magCompFn().getDefaultGUICapH(),	// double capH
			t_data_begin,												// double t_data_begin
			t_data_end,													// double t_data_end
			t_fitting,													// double t_fitting
			t_forecast,													// double t_forecast
			fcmain.mainshock_mag,										// double mag_main
			fcmain.mainshock_lat,										// double lat_main
			fcmain.mainshock_lon,										// double lon_main
			fcmain.mainshock_depth										// double depth_main
		);

		return new ForecastAndCatalogInfo (fc_info, catalog_info);
	}


	//----- Construction -----

	// Default constructor.

	public ForecastResults () {}

	// Calculate all results.
	// If f_seq_spec is false, then sequence specific results are not calculated.

	public void calc_all (long the_result_time, long the_advisory_lag, String the_injectable_text, ForecastMainshock fcmain, ForecastParameters params, boolean f_seq_spec) {
		result_time = the_result_time;
		advisory_lag = the_advisory_lag;
		injectable_text = ((the_injectable_text == null) ? "" : the_injectable_text);
		calc_catalog_results (fcmain, params);
		calc_generic_results (fcmain, params);
		calc_seq_spec_results (fcmain, params, f_seq_spec);
		calc_bayesian_results (fcmain, params);
		calc_etas_results (fcmain, params);
		return;
	}

	// Calculate all results from a known aftershock sequence.
	// If f_seq_spec is false, then sequence specific results are not calculated.
	// See comments for calc_catalog_results_from_known_as() regarding known_as.

	public void calc_all_from_known_as (long the_result_time, long the_advisory_lag, String the_injectable_text,
		ForecastMainshock fcmain, ForecastParameters params, boolean f_seq_spec, List<ObsEqkRupture> known_as) {

		result_time = the_result_time;
		advisory_lag = the_advisory_lag;
		injectable_text = ((the_injectable_text == null) ? "" : the_injectable_text);
		calc_catalog_results_from_known_as (fcmain, params, known_as);
		calc_generic_results (fcmain, params);
		calc_seq_spec_results (fcmain, params, f_seq_spec);
		calc_bayesian_results (fcmain, params);
		calc_etas_results (fcmain, params);
		return;
	}

	// Rebuild all transient results.

	public void rebuild_all (ForecastMainshock fcmain, ForecastParameters params, CompactEqkRupList the_catalog_aftershocks) {
		rebuild_catalog_results (fcmain, params, the_catalog_aftershocks);
		rebuild_generic_results (fcmain, params);
		rebuild_seq_spec_results (fcmain, params);
		rebuild_bayesian_results (fcmain, params);
		rebuild_etas_results (fcmain, params);
		return;
	}

	// These variables are used to pass information from calc_catalog_only to calc_after_catalog.
	// These variables are not marshaled.

	private boolean saved_f_seq_spec = true;		// saved value of f_seq_spec
	private boolean f_did_catalog_only = false;		// true if calc_catalog_only completed successfully

	// Calculate only the catalog, not doing any forecasts.
	// Note: For consistency, parameters are the same as calc_all, although
	// not all affect the result.  This function supports testing, and can also
	// be used to examine the catalog before decided whether to compute forecasts.

	public void calc_catalog_only (long the_result_time, long the_advisory_lag, String the_injectable_text, ForecastMainshock fcmain, ForecastParameters params, boolean f_seq_spec) {
		result_time = the_result_time;
		advisory_lag = the_advisory_lag;
		injectable_text = ((the_injectable_text == null) ? "" : the_injectable_text);
		calc_catalog_results (fcmain, params);

		saved_f_seq_spec = f_seq_spec;
		f_did_catalog_only = true;
		return;
	}

	// Calculate forecasts, after a call to calc_catalog_only.
	// Calling calc_catalog_only followed by calc_after_catalog is equivalent to calling calc_all.
	// This allows the caller to examine the catalog before deciding whether to compute forecasts.

	public void calc_after_catalog (ForecastMainshock fcmain, ForecastParameters params) {
		if (!( f_did_catalog_only )) {
			throw new IllegalStateException ("ForecastResults.calc_after_catalog: Did not complete call to calc_catalog_only");
		}
		boolean f_seq_spec = saved_f_seq_spec;

		calc_generic_results (fcmain, params);
		calc_seq_spec_results (fcmain, params, f_seq_spec);
		calc_bayesian_results (fcmain, params);
		calc_etas_results (fcmain, params);
		return;
	}

	// Copy only the catalog, not any forecasts, from another object, after a call to calc_catalog_only on the other object.
	// Note: This can be used to create multiple objects containing the same catalog,
	// allowing multiple forecasts to be computed on the same catalog (presumably with
	// different parameters), without having to re-fetch the catalog each time.
	// Note: This is a deep copy, except for catalog_comcat_aftershocks which is shared
	// between this object and the other object (because it is not intended to be
	// modified, and there is no simple way to deep-copy it).

	public void copy_catalog_only_from (ForecastResults other) {
		if (!( other.f_did_catalog_only )) {
			throw new IllegalStateException ("ForecastResults.copy_catalog_only_from: Did not complete call to calc_catalog_only on the other object");
		}

		this.result_time = other.result_time;
		this.advisory_lag = other.advisory_lag;
		this.injectable_text = other.injectable_text;
		copy_catalog_results_from (other);

		this.saved_f_seq_spec = other.saved_f_seq_spec;
		this.f_did_catalog_only = other.f_did_catalog_only;
		return;
	}

	// Pick one of the models to be sent to PDL, and set the corresponding xxxx_pdl flag.
	// Models are picked in priority order: ETAS, Bayesian, sequence specific, and generic.
	// Returns the JSON to be sent to PDL, or null if none.

	public String pick_pdl_model () {

		if (has_etas_json()) {
			etas_pdl = true;
			return get_etas_json();
		}

		if (bayesian_result_avail) {
			if (bayesian_json.length() > 0) {
				bayesian_pdl = true;
				return bayesian_json;
			}
		}

		// This section was previously commented out, to disallow sequence specific.
		// Perhaps the fully automatic mode should never send sequence specific to PDL,
		// however it is required for analyst-supplied parameters.

		if (seq_spec_result_avail) {
			if (seq_spec_json.length() > 0 && seq_spec_summary.get_numAftershocks() >= 1) {
				seq_spec_pdl = true;
				return seq_spec_json;
			}
		}

		if (generic_result_avail) {
			if (generic_json.length() > 0) {
				generic_pdl = true;
				return generic_json;
			}
		}
	
		return null;
	}

	// Get the model prevously picked to be sent to PDL.
	// This function looks at the xxxx_pdl flags to find the model to return.
	// Returns the JSON to be sent to PDL, or null if none.

	public String get_pdl_model () {

		if (etas_pdl) {
			return get_etas_json();
		}

		if (bayesian_pdl) {
			return bayesian_json;
		}

		if (seq_spec_pdl) {
			return seq_spec_json;
		}

		if (generic_pdl) {
			return generic_json;
		}
	
		return null;
	}

	// Code numbers that identify the possible models selected for PDL.

	public static final int PMCODE_INVALID	= -2;			// a code guaranteed to be invalid
	public static final int PMCODE_NONE		= -1;			// no model selected

	public static final int PMCODE_GENERIC	= 0;			// RJ generic model
	public static final int PMCODE_SEQ_SPEC	= 1;			// RJ sequence specific model
	public static final int PMCODE_BAYESIAN	= 2;			// RJ bayesian model
	public static final int PMCODE_ETAS		= 3;			// ETAS model

	// Return true if the model JSON is available.

	public boolean is_pdl_model_available (int pmcode) {
		switch (pmcode) {

		case PMCODE_GENERIC:
			return (generic_result_avail && generic_json.length() > 0);

		case PMCODE_SEQ_SPEC:
			return (seq_spec_result_avail && seq_spec_json.length() > 0);

		case PMCODE_BAYESIAN:
			return (bayesian_result_avail && bayesian_json.length() > 0);

		case PMCODE_ETAS:
			return (has_etas_json());
		}

		throw new IllegalArgumentException ("ForecastResults.is_pdl_model_available: Invalid PDL model code: " + pmcode);
	}

	// Return true if the model is selectable for use by PDL.

	public boolean is_pdl_model_selectable (int pmcode) {
		switch (pmcode) {

		case PMCODE_GENERIC:
			return (generic_result_avail && generic_json.length() > 0);

		case PMCODE_SEQ_SPEC:
			return (seq_spec_result_avail && seq_spec_json.length() > 0 && seq_spec_summary.get_numAftershocks() >= 1);

		case PMCODE_BAYESIAN:
			return (bayesian_result_avail && bayesian_json.length() > 0);

		case PMCODE_ETAS:
			return (has_etas_json());
		}

		throw new IllegalArgumentException ("ForecastResults.is_pdl_model_selectable: Invalid PDL model code: " + pmcode);
	}

	// Return the model JSON, or null if not available.
	// A non-null return will always be non-empty.

	public String get_pdl_model_json (int pmcode) {
		switch (pmcode) {

		case PMCODE_GENERIC:
			if (generic_result_avail && generic_json.length() > 0) {
				return generic_json;
			}
			return null;

		case PMCODE_SEQ_SPEC:
			if (seq_spec_result_avail && seq_spec_json.length() > 0) {
				return seq_spec_json;
			}
			return null;

		case PMCODE_BAYESIAN:
			if (bayesian_result_avail && bayesian_json.length() > 0) {
				return bayesian_json;
			}
			return null;

		case PMCODE_ETAS:
			if (has_etas_json()) {
				return get_etas_json();
			}
			return null;
		}

		throw new IllegalArgumentException ("ForecastResults.get_pdl_model_json: Invalid PDL model code: " + pmcode);
	}

	// Set the model JSON.
	// The supplied json_text must be non-null and non-empty.
	// A non-null return will always be non-empty.

	public void set_pdl_model_json (int pmcode, String json_text) {
		switch (pmcode) {

		case PMCODE_GENERIC:
			if (generic_result_avail && generic_json.length() > 0) {
				generic_json = json_text;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_pdl_model_json: Attempt to set RJ generic JSON text when it is not available");

		case PMCODE_SEQ_SPEC:
			if (seq_spec_result_avail && seq_spec_json.length() > 0) {
				seq_spec_json = json_text;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_pdl_model_json: Attempt to set RJ sequence specific JSON text when it is not available");

		case PMCODE_BAYESIAN:
			if (bayesian_result_avail && bayesian_json.length() > 0) {
				bayesian_json = json_text;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_pdl_model_json: Attempt to set RJ bayesian JSON text when it is not available");

		case PMCODE_ETAS:
			if (has_etas_json()) {
				etas_outcome.set_etas_json (json_text);
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_pdl_model_json: Attempt to set ETAS JSON text when it is not available");
		}

		throw new IllegalArgumentException ("ForecastResults.set_pdl_model_json: Invalid PDL model code: " + pmcode);
	}

	// Return the code of the currently selected PDL model, or PMCODE_NONE if none is currently selected.

	public int get_selected_pdl_model () {

		// Note the order of checking must match get_pdl_model()

		if (etas_pdl) {
			return PMCODE_ETAS;
		}

		if (bayesian_pdl) {
			return PMCODE_BAYESIAN;
		}

		if (seq_spec_pdl) {
			return PMCODE_SEQ_SPEC;
		}

		if (generic_pdl) {
			return PMCODE_GENERIC;
		}
	
		return PMCODE_NONE;
	}

	// Set the selected PDL model.
	// Throws exception if the model is not selectable.

	public void set_selected_pdl_model (int pmcode) {
		switch (pmcode) {

		case PMCODE_NONE:
			etas_pdl = false;
			bayesian_pdl = false;
			seq_spec_pdl = false;
			generic_pdl = false;
			return;

		case PMCODE_GENERIC:
			if (generic_result_avail && generic_json.length() > 0) {
				etas_pdl = false;
				bayesian_pdl = false;
				seq_spec_pdl = false;
				generic_pdl = true;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_selected_pdl_model: Attempt to select RJ generic model when it is not selectable");

		case PMCODE_SEQ_SPEC:
			if (seq_spec_result_avail && seq_spec_json.length() > 0 && seq_spec_summary.get_numAftershocks() >= 1) {
				etas_pdl = false;
				bayesian_pdl = false;
				seq_spec_pdl = true;
				generic_pdl = false;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_selected_pdl_model: Attempt to select RJ sequence specific model when it is not selectable");

		case PMCODE_BAYESIAN:
			if (bayesian_result_avail && bayesian_json.length() > 0) {
				etas_pdl = false;
				bayesian_pdl = true;
				seq_spec_pdl = false;
				generic_pdl = false;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_selected_pdl_model: Attempt to select RJ bayesian model when it is not selectable");

		case PMCODE_ETAS:
			if (has_etas_json()) {
				etas_pdl = true;
				bayesian_pdl = false;
				seq_spec_pdl = false;
				generic_pdl = false;
				return;
			}
			throw new IllegalStateException ("ForecastResults.set_selected_pdl_model: Attempt to select ETAS model when it is not selectable");
		}

		throw new IllegalArgumentException ("ForecastResults.set_selected_pdl_model: Invalid PDL model code: " + pmcode);
	}

	// Log calculation results, and also write some logging info to standard out.
	// If sg is null, just write info to standard out.

	public void write_calc_log (ServerGroup sg) {

		// If we have ETAS log info ...

		if (etas_result_avail && etas_log_info != null) {

			// Write log entry

			if (sg != null) {
				sg.log_sup.report_etas_results (etas_log_info);
			}

			// Accumulate global ETAS log info, and display it

			String global_stats = etas_log_info.apply_to_global_accum();
			if (global_stats != null && global_stats.length() > 0) {
				System.out.println ();
				System.out.println (global_stats);
			}
		}

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("ForecastResults:" + "\n");

		result.append ("result_time = " + result_time + "\n");
		result.append ("advisory_lag = " + advisory_lag + "\n");
		result.append ("injectable_text = " + injectable_text + "\n");

		result.append ("catalog_result_avail = " + catalog_result_avail + "\n");
		if (catalog_result_avail) {
			result.append ("catalog_start_time = " + catalog_start_time + "\n");
			result.append ("catalog_end_time = " + catalog_end_time + "\n");
			result.append ("catalog_eqk_count = " + catalog_eqk_count + "\n");
			result.append ("catalog_max_mag = " + catalog_max_mag + "\n");
			result.append ("catalog_max_event_id = " + catalog_max_event_id + "\n");
			result.append ("catalog_aftershocks = " + ((catalog_aftershocks == null) ? "null" : "available") + "\n");
			result.append ("catalog_comcat_aftershocks = " + ((catalog_comcat_aftershocks == null) ? "null" : "available") + "\n");
			result.append ("catalog_fit_start_days = " + catalog_fit_start_days + "\n");
			result.append ("catalog_fit_end_days = " + catalog_fit_end_days + "\n");
		}

		result.append ("generic_result_avail = " + generic_result_avail + "\n");
		if (generic_result_avail) {
			result.append ("generic_summary:\n" + generic_summary.toString() + "\n");
			result.append ("generic_json = " + generic_json + "\n");
			result.append ("generic_pdl = " + generic_pdl + "\n");
			result.append ("generic_model = " + ((generic_model == null) ? "null" : "available") + "\n");
		}

		result.append ("seq_spec_result_avail = " + seq_spec_result_avail + "\n");
		if (seq_spec_result_avail) {
			result.append ("seq_spec_summary:\n" + seq_spec_summary.toString() + "\n");
			result.append ("seq_spec_json = " + seq_spec_json + "\n");
			result.append ("seq_spec_pdl = " + seq_spec_pdl + "\n");
			result.append ("seq_spec_model = " + ((seq_spec_model == null) ? "null" : "available") + "\n");
		}

		result.append ("bayesian_result_avail = " + bayesian_result_avail + "\n");
		if (bayesian_result_avail) {
			result.append ("bayesian_summary:\n" + bayesian_summary.toString() + "\n");
			result.append ("bayesian_json = " + bayesian_json + "\n");
			result.append ("bayesian_pdl = " + bayesian_pdl + "\n");
			result.append ("bayesian_model = " + ((bayesian_model == null) ? "null" : "available") + "\n");
		}

		result.append ("etas_result_avail = " + etas_result_avail + "\n");
		if (etas_result_avail) {
			result.append ("etas_rescode = " + OEExecEnvironment.etas_result_to_string (etas_rescode) + "\n");
			result.append ("etas_outcome" + ((etas_outcome == null) ? " = null\n" : (":\n" + etas_outcome.toString())));
			result.append ("etas_log_info" + ((etas_log_info == null) ? " = null\n" : (":\n" + etas_log_info.toString())));
			result.append ("etas_pdl = " + etas_pdl + "\n");
		}

		return result.toString();
	}

	// Display our contents, with the JSON strings in display format.

	public String to_display_string () {
		StringBuilder result = new StringBuilder();

		result.append ("ForecastResults:" + "\n");

		result.append ("result_time = " + result_time + "\n");
		result.append ("advisory_lag = " + advisory_lag + "\n");
		result.append ("injectable_text = " + injectable_text + "\n");

		result.append ("catalog_result_avail = " + catalog_result_avail + "\n");
		if (catalog_result_avail) {
			result.append ("catalog_start_time = " + catalog_start_time + "\n");
			result.append ("catalog_end_time = " + catalog_end_time + "\n");
			result.append ("catalog_eqk_count = " + catalog_eqk_count + "\n");
			result.append ("catalog_max_mag = " + catalog_max_mag + "\n");
			result.append ("catalog_max_event_id = " + catalog_max_event_id + "\n");
			result.append ("catalog_aftershocks = " + ((catalog_aftershocks == null) ? "null" : "available") + "\n");
			result.append ("catalog_comcat_aftershocks = " + ((catalog_comcat_aftershocks == null) ? "null" : "available") + "\n");
			result.append ("catalog_fit_start_days = " + catalog_fit_start_days + "\n");
			result.append ("catalog_fit_end_days = " + catalog_fit_end_days + "\n");
		}

		result.append ("generic_result_avail = " + generic_result_avail + "\n");
		if (generic_result_avail) {
			result.append ("generic_summary:\n" + generic_summary.toString() + "\n");
			result.append ("generic_pdl = " + generic_pdl + "\n");
			result.append ("generic_model = " + ((generic_model == null) ? "null" : "available") + "\n");
			result.append ("generic_json:" + "\n");
			if (generic_json.length() > 0) {
				result.append (MarshalUtils.display_json_string (generic_json));
			}
		}

		result.append ("seq_spec_result_avail = " + seq_spec_result_avail + "\n");
		if (seq_spec_result_avail) {
			result.append ("seq_spec_summary:\n" + seq_spec_summary.toString() + "\n");
			result.append ("seq_spec_pdl = " + seq_spec_pdl + "\n");
			result.append ("seq_spec_model = " + ((seq_spec_model == null) ? "null" : "available") + "\n");
			result.append ("seq_spec_json:" + "\n");
			if (seq_spec_json.length() > 0) {
				result.append (MarshalUtils.display_json_string (seq_spec_json));
			}
		}

		result.append ("bayesian_result_avail = " + bayesian_result_avail + "\n");
		if (bayesian_result_avail) {
			result.append ("bayesian_summary:\n" + bayesian_summary.toString() + "\n");
			result.append ("bayesian_pdl = " + bayesian_pdl + "\n");
			result.append ("bayesian_model = " + ((bayesian_model == null) ? "null" : "available") + "\n");
			result.append ("bayesian_json:" + "\n");
			if (bayesian_json.length() > 0) {
				result.append (MarshalUtils.display_json_string (bayesian_json));
			}
		}

		result.append ("etas_result_avail = " + etas_result_avail + "\n");
		if (etas_result_avail) {
			result.append ("etas_rescode = " + OEExecEnvironment.etas_result_to_string (etas_rescode) + "\n");
			result.append ("etas_log_info" + ((etas_log_info == null) ? " = null\n" : (":\n" + etas_log_info.toString())));
			result.append ("etas_pdl = " + etas_pdl + "\n");
			result.append ("etas_outcome" + ((etas_outcome == null) ? " = null\n" : (":\n" + etas_outcome.to_display_string())));
		}

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 23001;
	private static final int MARSHAL_VER_2 = 23002;

	private static final String M_VERSION_NAME = "ForecastResults";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 23000;
	protected static final int MARSHAL_FCAST_RESULT = 23001;

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

		case MARSHAL_VER_1:

			writer.marshalLong   ("result_time"    , result_time    );
			writer.marshalLong   ("advisory_lag"   , advisory_lag   );
			writer.marshalString ("injectable_text", injectable_text);

			writer.marshalBoolean ("catalog_result_avail", catalog_result_avail);
			if (catalog_result_avail) {
				writer.marshalLong   ("catalog_start_time"  , catalog_start_time  );
				writer.marshalLong   ("catalog_end_time"    , catalog_end_time    );
				writer.marshalInt    ("catalog_eqk_count"   , catalog_eqk_count   );
				writer.marshalDouble ("catalog_max_mag"     , catalog_max_mag     );
				writer.marshalString ("catalog_max_event_id", catalog_max_event_id);
			}

			writer.marshalBoolean ("generic_result_avail", generic_result_avail);
			if (generic_result_avail) {
				generic_summary.marshal (writer, "generic_summary");
				writer.marshalJsonString ("generic_json", generic_json);
				writer.marshalBoolean    ("generic_pdl" , generic_pdl );
			}

			writer.marshalBoolean ("seq_spec_result_avail", seq_spec_result_avail);
			if (seq_spec_result_avail) {
				seq_spec_summary.marshal (writer, "seq_spec_summary");
				writer.marshalJsonString ("seq_spec_json", seq_spec_json);
				writer.marshalBoolean    ("seq_spec_pdl" , seq_spec_pdl );
			}

			writer.marshalBoolean ("bayesian_result_avail", bayesian_result_avail);
			if (bayesian_result_avail) {
				bayesian_summary.marshal (writer, "bayesian_summary");
				writer.marshalJsonString ("bayesian_json", bayesian_json);
				writer.marshalBoolean    ("bayesian_pdl" , bayesian_pdl );
			}
	
			break;

		case MARSHAL_VER_2:

			writer.marshalLong   ("result_time"    , result_time    );
			writer.marshalLong   ("advisory_lag"   , advisory_lag   );
			writer.marshalString ("injectable_text", injectable_text);

			writer.marshalBoolean ("catalog_result_avail", catalog_result_avail);
			if (catalog_result_avail) {
				writer.marshalLong   ("catalog_start_time"  , catalog_start_time  );
				writer.marshalLong   ("catalog_end_time"    , catalog_end_time    );
				writer.marshalInt    ("catalog_eqk_count"   , catalog_eqk_count   );
				writer.marshalDouble ("catalog_max_mag"     , catalog_max_mag     );
				writer.marshalString ("catalog_max_event_id", catalog_max_event_id);

				writer.marshalDouble ("catalog_fit_start_days", catalog_fit_start_days);
				writer.marshalDouble ("catalog_fit_end_days"  , catalog_fit_end_days  );
			}

			writer.marshalBoolean ("generic_result_avail", generic_result_avail);
			if (generic_result_avail) {
				generic_summary.marshal (writer, "generic_summary");
				writer.marshalJsonString ("generic_json", generic_json);
				writer.marshalBoolean    ("generic_pdl" , generic_pdl );
			}

			writer.marshalBoolean ("seq_spec_result_avail", seq_spec_result_avail);
			if (seq_spec_result_avail) {
				seq_spec_summary.marshal (writer, "seq_spec_summary");
				writer.marshalJsonString ("seq_spec_json", seq_spec_json);
				writer.marshalBoolean    ("seq_spec_pdl" , seq_spec_pdl );
			}

			writer.marshalBoolean ("bayesian_result_avail", bayesian_result_avail);
			if (bayesian_result_avail) {
				bayesian_summary.marshal (writer, "bayesian_summary");
				writer.marshalJsonString ("bayesian_json", bayesian_json);
				writer.marshalBoolean    ("bayesian_pdl" , bayesian_pdl );
			}

			writer.marshalBoolean ("etas_result_avail", etas_result_avail);
			if (etas_result_avail) {
				writer.marshalInt     ("etas_rescode"   , etas_rescode   );
				OEtasOutcome.marshal_poly (writer, "etas_outcome", etas_outcome);
				writer.marshalBoolean ("etas_pdl"       , etas_pdl       );
			}

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

		case MARSHAL_VER_1:

			result_time     = reader.unmarshalLong   ("result_time"    );
			advisory_lag    = reader.unmarshalLong   ("advisory_lag"   );
			injectable_text = reader.unmarshalString ("injectable_text");

			catalog_result_avail = reader.unmarshalBoolean ("catalog_result_avail");
			if (catalog_result_avail) {
				catalog_start_time   = reader.unmarshalLong   ("catalog_start_time"  );
				catalog_end_time     = reader.unmarshalLong   ("catalog_end_time"    );
				catalog_eqk_count    = reader.unmarshalInt    ("catalog_eqk_count"   );
				catalog_max_mag      = reader.unmarshalDouble ("catalog_max_mag"     );
				catalog_max_event_id = reader.unmarshalString ("catalog_max_event_id");
				catalog_aftershocks = null;
				catalog_comcat_aftershocks = null;

				catalog_fit_start_days = SEARCH_PARAM_OMIT;
				catalog_fit_end_days = SEARCH_PARAM_OMIT;
			} else {
				set_default_catalog_results();
			}

			generic_result_avail = reader.unmarshalBoolean ("generic_result_avail");
			if (generic_result_avail) {
				generic_summary = (new RJ_Summary_Generic()).unmarshal (reader, "generic_summary");
				generic_json    = reader.unmarshalJsonString ("generic_json");
				generic_pdl     = reader.unmarshalBoolean    ("generic_pdl" );
				generic_model   = null;
			} else {
				set_default_generic_results();
			}

			seq_spec_result_avail = reader.unmarshalBoolean ("seq_spec_result_avail");
			if (seq_spec_result_avail) {
				seq_spec_summary = (new RJ_Summary_SequenceSpecific()).unmarshal (reader, "seq_spec_summary");
				seq_spec_json    = reader.unmarshalJsonString ("seq_spec_json");
				seq_spec_pdl     = reader.unmarshalBoolean    ("seq_spec_pdl" );
				seq_spec_model   = null;
			} else {
				set_default_seq_spec_results();
			}

			bayesian_result_avail = reader.unmarshalBoolean ("bayesian_result_avail");
			if (bayesian_result_avail) {
				bayesian_summary = (new RJ_Summary_Bayesian()).unmarshal (reader, "bayesian_summary");
				bayesian_json    = reader.unmarshalJsonString ("bayesian_json");
				bayesian_pdl     = reader.unmarshalBoolean    ("bayesian_pdl" );
				bayesian_model   = null;
			} else {
				set_default_bayesian_results();
			}

			etas_result_avail = false;
			set_default_etas_results();

			break;

		case MARSHAL_VER_2:

			result_time     = reader.unmarshalLong   ("result_time"    );
			advisory_lag    = reader.unmarshalLong   ("advisory_lag"   );
			injectable_text = reader.unmarshalString ("injectable_text");

			catalog_result_avail = reader.unmarshalBoolean ("catalog_result_avail");
			if (catalog_result_avail) {
				catalog_start_time   = reader.unmarshalLong   ("catalog_start_time"  );
				catalog_end_time     = reader.unmarshalLong   ("catalog_end_time"    );
				catalog_eqk_count    = reader.unmarshalInt    ("catalog_eqk_count"   );
				catalog_max_mag      = reader.unmarshalDouble ("catalog_max_mag"     );
				catalog_max_event_id = reader.unmarshalString ("catalog_max_event_id");
				catalog_aftershocks = null;
				catalog_comcat_aftershocks = null;

				catalog_fit_start_days = reader.unmarshalDouble ("catalog_fit_start_days");
				catalog_fit_end_days   = reader.unmarshalDouble ("catalog_fit_end_days"  );
			} else {
				set_default_catalog_results();
			}

			generic_result_avail = reader.unmarshalBoolean ("generic_result_avail");
			if (generic_result_avail) {
				generic_summary = (new RJ_Summary_Generic()).unmarshal (reader, "generic_summary");
				generic_json    = reader.unmarshalJsonString ("generic_json");
				generic_pdl     = reader.unmarshalBoolean    ("generic_pdl" );
				generic_model   = null;
			} else {
				set_default_generic_results();
			}

			seq_spec_result_avail = reader.unmarshalBoolean ("seq_spec_result_avail");
			if (seq_spec_result_avail) {
				seq_spec_summary = (new RJ_Summary_SequenceSpecific()).unmarshal (reader, "seq_spec_summary");
				seq_spec_json    = reader.unmarshalJsonString ("seq_spec_json");
				seq_spec_pdl     = reader.unmarshalBoolean    ("seq_spec_pdl" );
				seq_spec_model   = null;
			} else {
				set_default_seq_spec_results();
			}

			bayesian_result_avail = reader.unmarshalBoolean ("bayesian_result_avail");
			if (bayesian_result_avail) {
				bayesian_summary = (new RJ_Summary_Bayesian()).unmarshal (reader, "bayesian_summary");
				bayesian_json    = reader.unmarshalJsonString ("bayesian_json");
				bayesian_pdl     = reader.unmarshalBoolean    ("bayesian_pdl" );
				bayesian_model   = null;
			} else {
				set_default_bayesian_results();
			}

			etas_result_avail = reader.unmarshalBoolean ("etas_result_avail");
			if (etas_result_avail) {
				etas_rescode  = reader.unmarshalInt     ("etas_rescode");
				etas_outcome  = OEtasOutcome.unmarshal_poly (reader, "etas_outcome");
				etas_log_info = null;
				etas_pdl      = reader.unmarshalBoolean ("etas_pdl"    );
			} else {
				set_default_etas_results();
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
	public ForecastResults unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, ForecastResults obj) {

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

	public static ForecastResults unmarshal_poly (MarshalReader reader, String name) {
		ForecastResults result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("ForecastResults.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FCAST_RESULT:
			result = new ForecastResults();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	// Subroutine to display probability distribution for a model.
	// The output is returned as a string.

	private static String show_prob_dist (RJ_AftershockModel model, double mag, double start_time) {
		StringBuilder result = new StringBuilder();

		double[] pd_year = model.getDistFuncWithAleatory (mag, start_time, start_time + 365.0);
		double[] pd_month = model.getDistFuncWithAleatory (mag, start_time, start_time + 30.0);
		double[] pd_week = model.getDistFuncWithAleatory (mag, start_time, start_time + 7.0);
		double[] pd_day = model.getDistFuncWithAleatory (mag, start_time, start_time + 1.0);

		int len = 0;
		len = Math.max (len, pd_year.length);
		len = Math.max (len, pd_month.length);
		len = Math.max (len, pd_week.length);
		len = Math.max (len, pd_day.length);

		len = Math.min (len, 500);		// max length we allow

		for (int n = 0; n < len; ++n) {
			result.append (String.format (
				"%4d:  % .4e  % .4e  % .4e  % .4e\n",
				n,
				((n < pd_day.length) ? pd_day[n] : 0.0),
				((n < pd_week.length) ? pd_week[n] : 0.0),
				((n < pd_month.length) ? pd_month[n] : 0.0),
				((n < pd_year.length) ? pd_year[n] : 0.0)
			));
		}

		return result.toString();
	}





	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ForecastResults : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  pdl_enable  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastResults : Invalid 'test1' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be 7 days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (7.0);

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "test1 injectable.", fcmain, params, true);
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  pdl_enable  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// Then rebuild transient data, and display the results.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastResults : Invalid 'test2' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be 7 days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (7.0);

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "", fcmain, params, true);
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

				// Save catalog

				CompactEqkRupList saved_catalog_aftershocks = results.catalog_aftershocks;

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				ForecastResults.marshal_poly (store, null, results);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (MarshalUtils.display_valid_json_string (json_string));

				// Unmarshal from JSON
			
				results = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				results = ForecastResults.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (results.toString());

				// Rebuild transient data

				results.rebuild_all (fcmain, params, saved_catalog_aftershocks);

				System.out.println ("");
				System.out.println (results.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  event_id  lag_days
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("ForecastResults : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];
				double lag_days = Double.parseDouble (args[3]);

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be lag_days days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (lag_days);

				// Get the advisory lag

				ActionConfig action_config = new ActionConfig();

				long advisory_lag;

				if (the_forecast_lag >= action_config.get_advisory_dur_year()) {
					advisory_lag = ADVISORY_LAG_YEAR;
				} else if (the_forecast_lag >= action_config.get_advisory_dur_month()) {
					advisory_lag = ADVISORY_LAG_MONTH;
				} else if (the_forecast_lag >= action_config.get_advisory_dur_week()) {
					advisory_lag = ADVISORY_LAG_WEEK;
				} else {
					advisory_lag = ADVISORY_LAG_DAY;
				}

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, advisory_lag, "test3 injectable.", fcmain, params, the_forecast_lag >= action_config.get_seq_spec_min_lag());
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  pdl_enable  event_id  lag_days
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// Then rebuild transient data, and display the results.
		// Then display the stored catalog.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("ForecastResults : Invalid 'test4' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];
				double lag_days = Double.parseDouble (args[3]);

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be lag_days days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (lag_days);

				// Get the advisory lag

				ActionConfig action_config = new ActionConfig();
				long the_advisory_lag = forecast_lag_to_advisory_lag (the_forecast_lag, action_config);

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, the_advisory_lag, "", fcmain, params, forecast_lag_to_f_seq_spec (the_forecast_lag, action_config));
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

				// Save catalog

				CompactEqkRupList saved_catalog_aftershocks = results.catalog_aftershocks;

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				ForecastResults.marshal_poly (store, null, results);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (MarshalUtils.display_valid_json_string (json_string));

				// Unmarshal from JSON
			
				results = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				results = ForecastResults.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (results.toString());

				// Rebuild transient data

				results.rebuild_all (fcmain, params, saved_catalog_aftershocks);

				System.out.println ("");
				System.out.println (results.toString());

				// Display the catalog, in increasing order by time

				ObsEqkRupList rup_list = saved_catalog_aftershocks.as_ObsEqkRupList();
				Collections.sort (rup_list, new ObsEqkRupMinTimeComparator());

				System.out.println ("");
				System.out.println ("Saved catalog size = " + rup_list.size());

				int n = 0;
				for (ObsEqkRupture r : rup_list) {
					long r_time = r.getOriginTime();
					double r_mag = r.getMag();
					Location r_hypo = r.getHypocenterLocation();
					double r_lat = r_hypo.getLatitude();
					double r_lon = r_hypo.getLongitude();
					double r_depth = r_hypo.getDepth();

					String event_info = SimpleUtils.event_info_one_line (r_time, r_mag, r_lat, r_lon, r_depth);
					System.out.println (n + ": " + event_info);
					++n;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  pdl_enable  event_id  mag  start_time
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then, for each forecast, get the probability distribution for the given
		// magnitude, starting at the given time in days, for day, week, month, and year,
		// and display them.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("ForecastResults : Invalid 'test5' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];
				double mag = Double.parseDouble (args[3]);
				double start_time = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Getting results and probability distributions");
				System.out.println ("pdl_enable = " + pdl_enable);
				System.out.println ("the_event_id = " + the_event_id);
				System.out.println ("mag = " + mag);
				System.out.println ("start_time = " + start_time);

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be 7 days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (7.0);

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "test1 injectable.", fcmain, params, true);
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

				// Generic forecast probabilities

				if (results.generic_result_avail) {
					System.out.println ("");
					System.out.println ("Generic forecast probability distribution");
					System.out.println ("");
					System.out.println (show_prob_dist (results.generic_model, mag, start_time));
				}

				// Sequence specific forecast probabilities

				if (results.seq_spec_result_avail) {
					System.out.println ("");
					System.out.println ("Sequence specific forecast probability distribution");
					System.out.println ("");
					System.out.println (show_prob_dist (results.seq_spec_model, mag, start_time));
				}

				// Bayesian forecast probabilities

				if (results.bayesian_result_avail) {
					System.out.println ("");
					System.out.println ("Bayesian forecast probability distribution");
					System.out.println ("");
					System.out.println (show_prob_dist (results.bayesian_model, mag, start_time));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  pdl_enable  event_id  f_apc
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then, for each RJ model, compute and display the marginals (may produce lengthy output).
		// If f_apc is true, then sequence specific parameters allow all of a/p/c to vary.
		// The pdl_enable can be used to control ETAS: 0 = default, 100 = disable, 200 = enable.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 4 additional arguments

			if (args.length != 4) {
				System.err.println ("ForecastResults : Invalid 'test6' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);	// 0 = ETAS default, 100 = disable ETAS, 200 = enable ETAS
				String the_event_id = args[2];
				boolean f_apc = Boolean.parseBoolean (args[3]);

				// Say hello

				System.out.println ("Getting results and marginal distributions");
				System.out.println ("pdl_enable = " + pdl_enable);
				System.out.println ("the_event_id = " + the_event_id);
				System.out.println ("f_apc = " + f_apc);

				// Set the PDL enable code (ETAS enable or disable)

				ServerConfig.set_opmode (pdl_enable);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be 7 days after the mainshock

				long the_forecast_lag = SimpleUtils.days_to_millis (7.0);

				// Get parameters

				ForecastParameters params = new ForecastParameters();
				params.fetch_all_params (the_forecast_lag, fcmain, null);

				if (f_apc) {
					params.seq_spec_params = new SeqSpecRJ_Parameters (
						1.0,	// double b
						-4.5,	// double min_a
						-0.5,	// double max_a
						45,		// int    num_a
						0.5,	// double min_p
						2.0,	// double max_p
						37,		// int    num_p
						0.01,	// double min_c
						1.00,	// double max_c
						17		// int    num_c
					);
				}

				// Display them

				System.out.println ("");
				System.out.println (params.toString());

				// Get results

				ForecastResults results = new ForecastResults();
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "test1 injectable.", fcmain, params, true);
				results.write_calc_log (null);

				// Display them

				System.out.println ("");
				System.out.println (results.toString());

				// Generic forecast marginals

				if (results.generic_result_avail) {
					System.out.println ("");
					System.out.println ("***** Generic forecast marginals *****");
					System.out.println ("");
					System.out.println (results.generic_model.make_rj_marginals(true).extended_string_2());
				}

				// Sequence specific forecast marginals

				if (results.seq_spec_result_avail) {
					System.out.println ("");
					System.out.println ("***** Sequence specific forecast marginals *****");
					System.out.println ("");
					System.out.println (results.seq_spec_model.make_rj_marginals(true).extended_string_2());
				}

				// Bayesian forecast marginals

				if (results.bayesian_result_avail) {
					System.out.println ("");
					System.out.println ("***** Bayesian forecast marginals *****");
					System.out.println ("");
					System.out.println (results.bayesian_model.make_rj_marginals(true).extended_string_2());
				}

				// Done

				System.out.println ();
				System.out.println ("Done");

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ForecastResults : Unrecognized subcommand : " + args[0]);
		return;

	}

}
