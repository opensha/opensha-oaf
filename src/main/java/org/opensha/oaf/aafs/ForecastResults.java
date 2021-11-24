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
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.catalog.ObsEqkRupMinTimeComparator;

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

import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_AUTO_PDL;
import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_AUTO_NO_PDL;
import static org.opensha.oaf.aafs.ForecastParameters.CALC_METH_SUPPRESS;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

/**
 * Results of a forecast.
 * Author: Michael Barall 04/26/2018.
 *
 * All fields are public, since there is little benefit to having lots of getters and setters.
 */
public class ForecastResults {

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

	public String catalog_max_event_id = "";

	// catalog_aftershocks - List of aftershocks.
	// Note: This field is not marshaled, because aftershock lists are stored in separate database records.

	public CompactEqkRupList catalog_aftershocks = null;

	// catalog_comcat_aftershocks - List of aftershocks, as returned by ComCat.
	// Note: This field is not marshaled, and is not rebuilt, because it is intended to be used
	// only immediately after the results are calculated;  mainly for finding foreshocks.

	public ObsEqkRupList catalog_comcat_aftershocks = null;

	// set_default_catalog_results - Set catalog results to default values.

	public void set_default_catalog_results () {
		catalog_start_time = 0L;
		catalog_end_time = 0L;
		catalog_eqk_count = 0;
		catalog_max_mag = 0.0;
		catalog_max_event_id = "";
		catalog_aftershocks = null;
		catalog_comcat_aftershocks = null;
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
		catalog_start_time = eventTime + (long)(params.min_days * ComcatOAFAccessor.day_millis);
		catalog_end_time = eventTime + (long)(params.max_days * ComcatOAFAccessor.day_millis);

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
	// Note: The purpose of this function is to inject simulated aftershock sequences of known
	// statistical properties, for testing purposes.  The supplied aftershock sequence is
	// filtered by time and magnitude.  It is not filtered by location or depth, because
	// simulated sequences are unlikely to contain that information.

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
		catalog_start_time = eventTime + (long)(params.min_days * ComcatOAFAccessor.day_millis);
		catalog_end_time = eventTime + (long)(params.max_days * ComcatOAFAccessor.day_millis);

		// Loop over supplied aftershocks

		catalog_comcat_aftershocks = new ObsEqkRupList();

		int event_num = 0;

		for (ObsEqkRupture rup : known_as) {

			// Extract time, magnitude, event id, hypocenter

			String rup_event_id = rup.getEventId();
			long rup_time = rup.getOriginTime();
			double rup_mag = rup.getMag();
			Location hypo = rup.getHypocenterLocation();

			// Count it

			++event_num;

			// If it passes the filter ...

			if (rup_time >= catalog_start_time
				&& rup_time <= catalog_end_time
				&& rup_mag >= params.min_mag) {

				// If no hypocenter supplied, replace with mainshock hypocenter

				if (hypo == null) {
					hypo = fcmain.get_eqk_location();
				}

				// If no event id supplied, replace with a generated id

				if (rup_event_id == null) {
					rup_event_id = "kas_" + Integer.toString(event_num);
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

			Instant eventDate = Instant.ofEpochMilli(mainshock.getOriginTime());
			Instant startDate = Instant.ofEpochMilli(Math.max (result_time, mainshock.getOriginTime() + 1000L));
			USGS_AftershockForecast forecast = new USGS_AftershockForecast (generic_model, catalog_aftershocks, eventDate, startDate);

			if (advisory_lag >= ADVISORY_LAG_YEAR) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_YEAR);
			} else if (advisory_lag >= ADVISORY_LAG_MONTH) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_MONTH);
			} else if (advisory_lag >= ADVISORY_LAG_WEEK) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_WEEK);
			} else {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_DAY);
			}

			String the_injectable_text = injectable_text;
			if (the_injectable_text.length() == 0) {
				the_injectable_text = null;		// convention for USGS_AftershockForecast
			}
			forecast.setInjectableText (the_injectable_text);

			if (params.next_scheduled_lag > 0L) {
				forecast.setNextForecastMillis (mainshock.getOriginTime() + params.next_scheduled_lag);
			} else if (params.next_scheduled_lag < 0L) {
				forecast.setNextForecastMillis (-1L);
			} else {
				forecast.setNextForecastMillis (0L);
			}

			LinkedHashMap<String, Object> userParamMap = new LinkedHashMap<String, Object>();
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (userParamMap);
			}
			forecast.setUserParamMap (userParamMap);

			// Get the JSON String

			generic_json = forecast.buildJSONString(result_time);
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
				params.min_days, params.max_days, params.mag_comp_params, params.seq_spec_params);

			// Save the summary

			seq_spec_summary = new RJ_Summary_SequenceSpecific (seq_spec_model);

			// Build the forecast

			Instant eventDate = Instant.ofEpochMilli(mainshock.getOriginTime());
			Instant startDate = Instant.ofEpochMilli(Math.max (result_time, mainshock.getOriginTime() + 1000L));
			USGS_AftershockForecast forecast = new USGS_AftershockForecast (seq_spec_model, catalog_aftershocks, eventDate, startDate);

			if (advisory_lag >= ADVISORY_LAG_YEAR) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_YEAR);
			} else if (advisory_lag >= ADVISORY_LAG_MONTH) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_MONTH);
			} else if (advisory_lag >= ADVISORY_LAG_WEEK) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_WEEK);
			} else {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_DAY);
			}

			String the_injectable_text = injectable_text;
			if (the_injectable_text.length() == 0) {
				the_injectable_text = null;		// convention for USGS_AftershockForecast
			}
			forecast.setInjectableText (the_injectable_text);

			if (params.next_scheduled_lag > 0L) {
				forecast.setNextForecastMillis (mainshock.getOriginTime() + params.next_scheduled_lag);
			} else if (params.next_scheduled_lag < 0L) {
				forecast.setNextForecastMillis (-1L);
			} else {
				forecast.setNextForecastMillis (0L);
			}

			LinkedHashMap<String, Object> userParamMap = new LinkedHashMap<String, Object>();
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (userParamMap);
			}
			forecast.setUserParamMap (userParamMap);

			// Get the JSON String

			seq_spec_json = forecast.buildJSONString(result_time);
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
					params.min_days, params.max_days, params.mag_comp_params, params.seq_spec_params);

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

			Instant eventDate = Instant.ofEpochMilli(mainshock.getOriginTime());
			Instant startDate = Instant.ofEpochMilli(Math.max (result_time, mainshock.getOriginTime() + 1000L));
			USGS_AftershockForecast forecast = new USGS_AftershockForecast (bayesian_model, catalog_aftershocks, eventDate, startDate);

			if (advisory_lag >= ADVISORY_LAG_YEAR) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_YEAR);
			} else if (advisory_lag >= ADVISORY_LAG_MONTH) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_MONTH);
			} else if (advisory_lag >= ADVISORY_LAG_WEEK) {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_WEEK);
			} else {
				forecast.setAdvisoryDuration (USGS_AftershockForecast.Duration.ONE_DAY);
			}

			String the_injectable_text = injectable_text;
			if (the_injectable_text.length() == 0) {
				the_injectable_text = null;		// convention for USGS_AftershockForecast
			}
			forecast.setInjectableText (the_injectable_text);

			if (params.next_scheduled_lag > 0L) {
				forecast.setNextForecastMillis (mainshock.getOriginTime() + params.next_scheduled_lag);
			} else if (params.next_scheduled_lag < 0L) {
				forecast.setNextForecastMillis (-1L);
			} else {
				forecast.setNextForecastMillis (0L);
			}

			LinkedHashMap<String, Object> userParamMap = new LinkedHashMap<String, Object>();
			if (params.aftershock_search_avail && params.aftershock_search_region != null) {
				params.aftershock_search_region.get_display_params (userParamMap);
			}
			forecast.setUserParamMap (userParamMap);

			// Get the JSON String

			bayesian_json = forecast.buildJSONString(result_time);
			if (bayesian_json == null) {
				throw new RuntimeException("ForecastResults.calc_bayesian_results: Unable to generate JSON");
			}

		} catch (Exception e) {
			//throw new RuntimeException("ForecastResults.calc_bayesian_results: Exception building bayesian forecast", e);

			// In case of any error, just don't to the Bayesian

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
		return;
	}

	// Rebuild all transient results.

	public void rebuild_all (ForecastMainshock fcmain, ForecastParameters params, CompactEqkRupList the_catalog_aftershocks) {
		rebuild_catalog_results (fcmain, params, the_catalog_aftershocks);
		rebuild_generic_results (fcmain, params);
		rebuild_seq_spec_results (fcmain, params);
		rebuild_bayesian_results (fcmain, params);
		return;
	}

	// Pick one of the models to be sent to PDL, and set the corresponding xxxx_pdl flag.
	// Models are picked in priority order: Bayesian, sequence specific, and generic.
	// Returns the JSON to be sent to PDL, or null if none.

	public String pick_pdl_model () {

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

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 23001;

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

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

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
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

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

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ForecastResults : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastResults : Invalid 'test1' subcommand");
				return;
			}

			try {

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
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "test1 injectable.", fcmain, params, true);

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
		//  test2  event_id
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// Then rebuild transient data, and display the results.

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ForecastResults : Invalid 'test2' subcommand");
				return;
			}

			try {

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
				results.calc_all (fcmain.mainshock_time + the_forecast_lag, ADVISORY_LAG_WEEK, "", fcmain, params, true);

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
				System.out.println (json_string);

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
		//  test3  event_id  lag_days
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastResults : Invalid 'test3' subcommand");
				return;
			}

			try {

				String the_event_id = args[1];
				double lag_days = Double.parseDouble (args[2]);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be lag_days days after the mainshock

				long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * lag_days);

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
		//  test4  event_id  lag_days
		// Get parameters for the event, and display them.
		// Then get results for the event, and display them.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// Then rebuild transient data, and display the results.
		// Then display the stored catalog.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("ForecastResults : Invalid 'test4' subcommand");
				return;
			}

			try {

				String the_event_id = args[1];
				double lag_days = Double.parseDouble (args[2]);

				// Fetch just the mainshock info

				ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_only (the_event_id);

				System.out.println ("");
				System.out.println (fcmain.toString());

				// Set the forecast time to be lag_days days after the mainshock

				long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * lag_days);

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
				System.out.println (json_string);

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




		// Unrecognized subcommand.

		System.err.println ("ForecastResults : Unrecognized subcommand : " + args[0]);
		return;

	}

}
