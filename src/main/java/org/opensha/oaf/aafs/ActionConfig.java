package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;

import java.time.Duration;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;

/**
 * Configuration for AAFS server actions.
 * Author: Michael Barall 04/29/2018.
 *
 * To use, create an object of this class, and then call its methods to obtain configuration parameters.
 *
 * Parameters come from a configuration file, in the format of ActionConfigFile.
 */
public final class ActionConfig {

	//----- Parameter set -----

	// Cached parameter set.

	private static ActionConfigFile cached_param_set = null;

	// Parameter set.

	private ActionConfigFile param_set;

	// Get the parameter set.

	private static synchronized ActionConfigFile get_param_set () {

		// If we have a cached parameter set, return it

		if (cached_param_set != null) {
			return cached_param_set;
		}

		// Working data

		ActionConfigFile wk_param_set = null;

		// Any error reading the parameters aborts the program

		try {

			// Read the configuation file

			wk_param_set = ActionConfigFile.unmarshal_config ("ActionConfig.json", ActionConfig.class);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ActionConfig: Error loading parameter file ActionConfig.json, unable to continue");
			System.exit(0);
			//throw new RuntimeException("ActionConfig: Error loading parameter file ActionConfig.json", e);
		}

		// Save the parameter set

		cached_param_set = wk_param_set;
		return cached_param_set;
	}

	// unload_data - Remove the cached data from memory.
	// The data will be reloaded the next time one of these objects is created.
	// Any existing objects will continue to use the old data.
	// This makes it possible to load new parameter values without restarting the program.

	public static synchronized void unload_data () {
		cached_param_set = null;
		return;
	}


	//----- Construction -----

	// Default constructor.

	public ActionConfig () {
		param_set = get_param_set ();
	}

	// Display our contents

	@Override
	public String toString() {
		return "ActionConfig:\n" + param_set.toString();
	}


	//----- Parameter access -----

	// Get minimum gap between forecasts, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_forecast_min_gap () {
		return param_set.forecast_min_gap;
	}

	// Get maximum delay in reporting a forecast to PDL, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_forecast_max_delay () {
		return param_set.forecast_max_delay;
	}

	// Get assumed maximum difference between our clock and ComCat's clock, in milliseconds.
	// (Specifically, if an earthquake occurs at time T then it should be visible in
	// ComCat by the time our clock reads T + comcat_clock_skew.)
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_comcat_clock_skew () {
		return param_set.comcat_clock_skew;
	}

	// Get assumed maximum change in mainshock origin time, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_comcat_origin_skew () {
		return param_set.comcat_origin_skew;
	}

	// Get minimum gap between ComCat retries, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_comcat_retry_min_gap () {
		return param_set.comcat_retry_min_gap;
	}

	// Get minimum ComCat retry lag for missing events, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_comcat_retry_missing () {
		return param_set.comcat_retry_missing;
	}

	// Get minimum time after an earthquake at which sequence-specific forecasts can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_seq_spec_min_lag () {
		return param_set.seq_spec_min_lag;
	}

	// Get minimum time after an earthquake at which one-week advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_advisory_dur_week () {
		return param_set.advisory_dur_week;
	}

	// Get minimum time after an earthquake at which one-month advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_advisory_dur_month () {
		return param_set.advisory_dur_month;
	}

	// Get minimum time after an earthquake at which one-year advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_advisory_dur_year () {
		return param_set.advisory_dur_year;
	}

	// Get default value of the maximum forecast lag.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_def_max_forecast_lag () {
		return param_set.def_max_forecast_lag;
	}

	// Get forecast lag at which a timeline not passing the intake filter can be withdrawn.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_withdraw_forecast_lag () {
		return param_set.withdraw_forecast_lag;
	}

	// Get option selecting how to handle stale forecasts.
	// (A forecast is stale if another forecast could be issued immediately.)

	public int get_stale_forecast_option () {
		return param_set.stale_forecast_option;
	}

	// Get flag, indicating if stale forecasts should be skipped.

	public boolean get_skip_stale_forecasts () {
		return param_set.stale_forecast_option == ActionConfigFile.SFOPT_SKIP;
	}

	// Get flag, indicating if stale forecasts should be omitted.

	public boolean get_omit_stale_forecasts () {
		return param_set.stale_forecast_option == ActionConfigFile.SFOPT_OMIT;
	}

	// Get search radius to search for shadowing events, in km.

	public double get_shadow_search_radius () {
		return param_set.shadow_search_radius;
	}

	// Get amount of time to look back from the mainshock, to search for shadowing events.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_shadow_lookback_time () {
		return param_set.shadow_lookback_time;
	}

	// Get minimum magnitude to use for computing centroids, when searching for shadowing events.

	public double get_shadow_centroid_mag () {
		return param_set.shadow_centroid_mag;
	}

	// Get minimum magnitude for a candidate shadowing event to be considered large.

	public double get_shadow_large_mag () {
		return param_set.shadow_large_mag;
	}

	// Get period for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_short_period () {
		return param_set.poll_short_period;
	}

	// Get lookback time for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_short_lookback () {
		return param_set.poll_short_lookback;
	}

	// Get time gap between intake actions for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_short_intake_gap () {
		return param_set.poll_short_intake_gap;
	}

	// Get period for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_long_period () {
		return param_set.poll_long_period;
	}

	// Get lookback time for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_long_lookback () {
		return param_set.poll_long_lookback;
	}

	// Get time gap between intake actions for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_poll_long_intake_gap () {
		return param_set.poll_long_intake_gap;
	}

	// Get maximum allowed age for PDL intake.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_pdl_intake_max_age () {
		return param_set.pdl_intake_max_age;
	}

	// Get maximum allowed time in future for PDL intake.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_pdl_intake_max_future () {
		return param_set.pdl_intake_max_future;
	}

	// Get age at which forecasts should be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_forecast_age () {
		return param_set.removal_forecast_age;
	}

	// Get update time clock skew allowance for PDL forecasts.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_update_skew () {
		return param_set.removal_update_skew;
	}

	// Get maximum time before present to search for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_lookback_tmax () {
		return param_set.removal_lookback_tmax;
	}

	// Get minimum time before present to search for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_lookback_tmin () {
		return param_set.removal_lookback_tmin;
	}

	// Get minimum magnitude to search for forecasts that need to be removed from PDL

	public double get_removal_lookback_mag () {
		return param_set.removal_lookback_mag;
	}

	// Get period for checking for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_check_period () {
		return param_set.removal_check_period;
	}

	// Get retry interval for checking for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_retry_period () {
		return param_set.removal_retry_period;
	}

	// Get gap between processing events with forecasts that may need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_event_gap () {
		return param_set.removal_event_gap;
	}

	// Get time after observing a foreign forecast that removal checks are suppressed.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_removal_foreign_block () {
		return param_set.removal_foreign_block;
	}

	// Get default value of injectable text for PDL JSON files, or "" for none.

	public String get_def_injectable_text () {
		return param_set.def_injectable_text;
	}

	// Get option to enable event-sequence products. [v2]

	public int get_evseq_enable () {
		return param_set.evseq_enable;
	}

	public String get_evseq_enable_as_string () {
		return ActionConfigFile.get_esena_as_string (get_evseq_enable());
	}

	// Get flag, indicating if event-sequence products are enabled. [v2]

	public boolean get_is_evseq_enabled () {
		return param_set.evseq_enable == ActionConfigFile.ESENA_ENABLE;
	}

	// Get option to send event-sequence reports by default. [v2]

	public int get_evseq_report () {
		return param_set.evseq_report;
	}

	public String get_evseq_report_as_string () {
		return ActionConfigFile.get_esrep_as_string (get_evseq_report());
	}

	// Get flag, indicating if event-sequence reports are sent by default. [v2]

	private boolean get_is_evseq_reported () {
		return param_set.evseq_report == ActionConfigFile.ESREP_REPORT;
	}

	// Get time before mainshock when an event sequence begins. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_evseq_lookback () {
		return param_set.evseq_lookback;
	}

	// Get time after forecast when an event sequence ends. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_evseq_lookahead () {
		return param_set.evseq_lookahead;
	}

	// Get minimum duration of an event sequence (after mainshock) when it is capped. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_evseq_cap_min_dur () {
		return param_set.evseq_cap_min_dur;
	}

	// Get time before a capping event when an event sequence ends. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_evseq_cap_gap () {
		return param_set.evseq_cap_gap;
	}

	// Get option to enable ETAS forecasts. [v3]

	public int get_etas_enable () {
		return param_set.etas_enable;
	}

	public String get_etas_enable_as_string () {
		return ActionConfigFile.get_etas_ena_as_string (get_etas_enable());
	}

	// Get flag, indicating if ETAS forecasts are enabled. [v3]

	public boolean get_is_etas_enabled () {
		return param_set.etas_enable == ActionConfigFile.ETAS_ENA_ENABLE;
	}

	// Get the number of advisory magnitude bins.

	public int get_adv_min_mag_bin_count () {
		return param_set.adv_min_mag_bins.size();
	}

	// Get the minimum magnitude for the i-th advisory magnitude bin.

	public double get_adv_min_mag_bin (int i) {
		return param_set.adv_min_mag_bins.get(i).doubleValue();
	}

	// Get the number of advisory forecast windows.

	public int get_adv_window_count () {
		return param_set.adv_window_start_offs.size();
	}

	// Get the start offset for the i-th advisory forecast window.

	public long get_adv_window_start_off (int i) {
		return param_set.adv_window_start_offs.get(i).longValue();
	}

	// Get the end offset for the i-th advisory forecast window.

	public long get_adv_window_end_off (int i) {
		return param_set.adv_window_end_offs.get(i).longValue();
	}

	// Get the name for the i-th advisory forecast window.

	public String get_adv_window_name (int i) {
		return param_set.adv_window_names.get(i);
	}

	// Get the number of advisory fractile probabilities. [v3]

	public int get_adv_fractile_value_count () {
		return param_set.adv_fractile_values.size();
	}

	// Get the i-th advisory fractile probability. [v3]

	public double get_adv_fractile_value (int i) {
		return param_set.adv_fractile_values.get(i).doubleValue();
	}

	// Get the i-th advisory fractile probability, rounded. [v3]

	public double get_adv_fractile_value_rounded (int i) {
		return ActionConfigFile.round_fractile_value (get_adv_fractile_value(i));
	}

	// Get the number of advisory bar counts. [v3]

	public int get_adv_bar_count_count () {
		return param_set.adv_bar_counts.size();
	}

	// Get the i-th advisory bar count. [v3]

	public int get_adv_bar_count (int i) {
		return param_set.adv_bar_counts.get(i).intValue();
	}

	// Get the first element of forecast_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_forecast_lag (long min_lag) {
		return param_set.get_next_forecast_lag (min_lag);
	}

	// Get the first element of forecast_lags that is >= the supplied min_lag and <= the supplied max_lag.
	// The return is -1 if there is no element in the given range.
	// If max_lag <= 0, then def_max_forecast_lag is used as the upper bound.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_forecast_lag (long min_lag, long max_lag) {
		return param_set.get_next_forecast_lag (min_lag, max_lag);
	}

	// Get the first element of comcat_retry_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_comcat_retry_lag (long min_lag) {
		return param_set.get_next_comcat_retry_lag (min_lag);
	}

	// Get the first element of comcat_intake_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_comcat_intake_lag (long min_lag) {
		return param_set.get_next_comcat_intake_lag (min_lag);
	}

	// Get the first element of pdl_report_retry_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_pdl_report_retry_lag (long min_lag) {
		return param_set.get_next_pdl_report_retry_lag (min_lag);
	}

	// Get the pdl intake region that satisfies the min_mag criterion.
	// If found, the region is returned.
	// If not found, null is returned.

	public IntakeSphRegion get_pdl_intake_region_for_min_mag (double lat, double lon, double mag) {
		return param_set.get_pdl_intake_region_for_min_mag (lat, lon, mag);
	}

	// Get the pdl intake region that satisfies the intake_mag criterion.
	// If found, the region is returned.
	// If not found, null is returned.

	public IntakeSphRegion get_pdl_intake_region_for_intake_mag (double lat, double lon, double mag) {
		return param_set.get_pdl_intake_region_for_intake_mag (lat, lon, mag);
	}

	// Get the minimum magnitude for the min_mag criterion in any intake region.
	// The result is 10.0 if there are no intake regions.

	public double get_pdl_intake_region_min_min_mag () {
		return param_set.get_pdl_intake_region_min_min_mag ();
	}

	// Get the minimum magnitude for the intake_mag criterion in any intake region.
	// The result is 10.0 if there are no intake regions.

	public double get_pdl_intake_region_min_intake_mag () {
		return param_set.get_pdl_intake_region_min_intake_mag ();
	}


	//----- Derived convenience functions -----

	// Return all advisory magnitude bins in a newly-allocated array.
	// The return values are in increasing order.

	public double[] get_adv_min_mag_bins_array () {
		int n = get_adv_min_mag_bin_count();
		double[] result = new double[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_min_mag_bin(i);
		}
		return result;
	}

	// Return all advisory forecast window start offsets in a newly-allocated array.

	public long[] get_adv_window_start_offs_array () {
		int n = get_adv_window_count();
		long[] result = new long[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_window_start_off(i);
		}
		return result;
	}

	// Return all advisory forecast window end offsets in a newly-allocated array.

	public long[] get_adv_window_end_offs_array () {
		int n = get_adv_window_count();
		long[] result = new long[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_window_end_off(i);
		}
		return result;
	}

	// Return all advisory forecast window names in a newly-allocated array.

	public String[] get_adv_window_names_array () {
		int n = get_adv_window_count();
		String[] result = new String[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_window_name(i);
		}
		return result;
	}

	// Return all advisory fractile probabilities in a newly-allocated array. [v3]
	// The return values are in increasing order.

	public double[] get_adv_fractile_values_array () {
		int n = get_adv_fractile_value_count();
		double[] result = new double[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_fractile_value(i);
		}
		return result;
	}

	// Return all advisory fractile probabilities, rounded, in a newly-allocated array. [v3]
	// The return values are in increasing order.

	public double[] get_adv_fractile_values_rounded_array () {
		int n = get_adv_fractile_value_count();
		double[] result = new double[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_fractile_value_rounded(i);
		}
		return result;
	}

	// Return all advisory bar counts in a newly-allocated array. [v3]
	// The return values are in increasing order.

	public int[] get_adv_bar_counts_array () {
		int n = get_adv_bar_count_count();
		int[] result = new int[n];
		for (int i = 0; i < n; ++i) {
			result[i] = get_adv_bar_count(i);
		}
		return result;
	}

	// Get the maximum end offset for any advisory forecast window.

	public long get_max_adv_window_end_off () {
		long result = 0L;
		int n = get_adv_window_count();
		for (int i = 0; i < n; ++i) {
			long end_off = get_adv_window_end_off (i);
			if (result < end_off) {
				result = end_off;
			}
		}
		return result;
	}

	// Get the number of forecast_lags that are <= the supplied max_lag.
	// If max_lag <= 0, then def_max_forecast_lag is used as the upper bound.

	public int get_forecast_lag_count (long max_lag) {
		int n = 0;
		long min_lag = 0L;
		for (;;) {
			long forecast_lag = get_next_forecast_lag (min_lag, max_lag);
			if (forecast_lag < 0L) {
				break;
			}
			++n;
			min_lag = forecast_lag + 1L;
		}
		return n;
	}

	// Get a newly-allocated array containing the forecast_lags that are <= the supplied max_lag.
	// If max_lag <= 0, then def_max_forecast_lag is used as the upper bound.
	// The return values are in increasing order.

	public long[] get_forecast_lag_array (long max_lag) {
		int n = get_forecast_lag_count (max_lag);
		long[] result = new long[n];
		int i = 0;
		long min_lag = 0L;
		for (;;) {
			long forecast_lag = get_next_forecast_lag (min_lag, max_lag);
			if (forecast_lag < 0L) {
				break;
			}
			result[i] = forecast_lag;
			++i;
			min_lag = forecast_lag + 1L;
		}
		if (i != n) {
			throw new IllegalStateException ("ActionConfig.get_forecast_lag_array: Length mismatch: i = " + i + ", n = " + n);
		}
		return result;
	}

	// Get the extended maximum forecast lag.
	// The returned value is always >= def_max_forecast_lag.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long get_extended_max_forecast_lag () {
		long result = param_set.forecast_lags.get (param_set.forecast_lags.size() - 1);
		result = Math.max (result, param_set.def_max_forecast_lag);
		return result;
	}

	// Get the event-sequence cap time to use when a forecast is blocked.
	// Parameters:
	//  report = Report option, ESREP_XXXXX.

	public long get_cap_time_for_block (int report) {
		if (get_is_evseq_enabled()) {
			switch (report) {
			case ActionConfigFile.ESREP_REPORT:
			case ActionConfigFile.ESREP_DELETE:
				return PDLCodeChooserEventSequence.CAP_TIME_DELETE;
			}
		}
		return PDLCodeChooserEventSequence.CAP_TIME_NOP;
	}

	// Get the event-sequence cap time to use when a forecast is shadowed.
	// Parameters:
	//  report = Report option, ESREP_XXXXX.
	//  mainshock_time = Time of the mainshock, in milliseconds.
	//  shadow_time = Time of the shadowing event, in milliseconds.

	public long get_cap_time_for_shadow (int report, long mainshock_time, long shadow_time) {
		if (get_is_evseq_enabled()) {
			switch (report) {
			case ActionConfigFile.ESREP_REPORT:
				long cap_time = PDLCodeChooserEventSequence.nudge_cap_time (shadow_time - get_evseq_cap_gap());
				if (cap_time >= mainshock_time + get_evseq_cap_min_dur()) {
					return cap_time;
				}
				return PDLCodeChooserEventSequence.CAP_TIME_DELETE;
			case ActionConfigFile.ESREP_DELETE:
				return PDLCodeChooserEventSequence.CAP_TIME_DELETE;
			}
		}
		return PDLCodeChooserEventSequence.CAP_TIME_NOP;
	}


	//----- Service functions -----

	private static final long UNIT_LAG = 1000L;

	// Convert a lag value so it can be stored in an int.
	// Note: This should be applied only to timing parameters returned by functions
	// in this class, which are guaranteed to be multiples of UNIT_LAG, and (after
	// division by UNIT_LAG) representable as an int.

	public int lag_to_int (long lag) {
		return (int)(lag / UNIT_LAG);
	}

	// Convert an int to a lag value.

	public long int_to_lag (int k) {
		return ((long)k) * UNIT_LAG;
	}

	// Given a lag value, round it down to a multiple of the lag unit (which is 1 second).
	// Note: Assumes lag >= 0L.

	public long floor_unit_lag (long lag) {
		return lag - (lag % UNIT_LAG);
	}

	// Same, except make the return value greater than prior_lag.

	public long floor_unit_lag (long lag, long prior_lag) {
		long x = Math.max (lag, prior_lag + UNIT_LAG);
		return x - (x % UNIT_LAG);
	}

	// Given a lag value, round it up to a multiple of the lag unit (which is 1 second).
	// Note: Assumes lag >= 0L.

	public long ceil_unit_lag (long lag) {
		long x = lag + UNIT_LAG - 1L;
		return x - (x % UNIT_LAG);
	}

	// Same, except make the return value greater than prior_lag.

	public long ceil_unit_lag (long lag, long prior_lag) {
		long x = Math.max (lag + UNIT_LAG - 1L, prior_lag + UNIT_LAG);
		return x - (x % UNIT_LAG);
	}


	//----- Parameter modification -----

	// Get the internal action configuration file.
	// Note: This is provided so that command-line code can adjust parameters.
	// Note: Calling unload_data will revert all parameters to the values in
	// the configuration file.

	public ActionConfigFile get_action_config_file () {
		return param_set;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ActionConfig : Missing subcommand");
			return;
		}

		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create an object, and display the parameters.
		// Then read out the time lag lists.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ActionConfig : Invalid 'test1' subcommand");
				return;
			}

			// Create a configuration object

			ActionConfig action_config = new ActionConfig();

			// Display it

			System.out.println (action_config.toString());

			// Display some calculated values

			System.out.println ("");
			System.out.println ("skip_stale_forecasts = " + action_config.get_skip_stale_forecasts());
			System.out.println ("omit_stale_forecasts = " + action_config.get_omit_stale_forecasts());
			System.out.println ("is_evseq_enabled = " + action_config.get_is_evseq_enabled());
			System.out.println ("is_evseq_reported = " + action_config.get_is_evseq_reported());
			System.out.println ("is_etas_enabled = " + action_config.get_is_etas_enabled());
			System.out.println ("pdl_intake_region_min_min_mag = " + action_config.get_pdl_intake_region_min_min_mag());
			System.out.println ("pdl_intake_region_min_intake_mag = " + action_config.get_pdl_intake_region_min_intake_mag());
			System.out.println ("max_adv_window_end_off = " + action_config.get_max_adv_window_end_off());

			// Display the list of advisory magnitude bins

			System.out.println ("");

			int n_bin = action_config.get_adv_min_mag_bin_count();
			for (int i = 0; i < n_bin; ++i) {
				System.out.println ("advisory bin " + i + ": mag = " + action_config.get_adv_min_mag_bin(i));
			}

			// Display the list of advisory forecast windows

			System.out.println ("");

			int n_window = action_config.get_adv_window_count();
			for (int i = 0; i < n_window; ++i) {
				System.out.println ("advisory window " + i
					+ ": start = " + Duration.ofMillis(action_config.get_adv_window_start_off(i)).toString()
					+ ", end = " + Duration.ofMillis(action_config.get_adv_window_end_off(i)).toString()
					+ ", name = " + action_config.get_adv_window_name(i)
					);
			}

			// Display the list of fractile probabilities

			System.out.println ("");

			int n_fractile = action_config.get_adv_fractile_value_count();
			for (int i = 0; i < n_fractile; ++i) {
				System.out.println ("fractile " + i
					+ ": probability = " + action_config.get_adv_fractile_value(i)
					+ ", rounded = " + action_config.get_adv_fractile_value_rounded(i)
					);
			}

			// Display the list of bar counts

			System.out.println ("");

			int n_bar = action_config.get_adv_bar_count_count();
			for (int i = 0; i < n_bar; ++i) {
				System.out.println ("bar " + i + ": count = " + action_config.get_adv_bar_count(i));
			}

			// Display list of forecast time lags

			System.out.println ("");

			long min_lag = 0L;
			for (;;) {
				long forecast_lag = action_config.get_next_forecast_lag (min_lag);
				if (forecast_lag < 0L) {
					break;
				}
				System.out.println (Duration.ofMillis(forecast_lag).toString() + "  " + forecast_lag);
				min_lag = forecast_lag + action_config.get_forecast_min_gap ();
			}

			// Display list of forecast time lags, to default limit only

			System.out.println ("");

			min_lag = 0L;
			for (;;) {
				long forecast_lag = action_config.get_next_forecast_lag (min_lag, 0L);
				if (forecast_lag < 0L) {
					break;
				}
				System.out.println (Duration.ofMillis(forecast_lag).toString() + "  " + forecast_lag);
				min_lag = forecast_lag + action_config.get_forecast_min_gap ();
			}

			// Display list of ComCat retry time lags

			System.out.println ("");

			min_lag = 0L;
			for (;;) {
				long comcat_retry_lag = action_config.get_next_comcat_retry_lag (min_lag);
				if (comcat_retry_lag < 0L) {
					break;
				}
				System.out.println (Duration.ofMillis(comcat_retry_lag).toString() + "  " + comcat_retry_lag);
				min_lag = comcat_retry_lag + action_config.get_comcat_retry_min_gap ();
			}

			// Display list of ComCat intake time lags

			System.out.println ("");

			min_lag = 0L;
			for (;;) {
				long comcat_intake_lag = action_config.get_next_comcat_intake_lag (min_lag);
				if (comcat_intake_lag < 0L) {
					break;
				}
				System.out.println (Duration.ofMillis(comcat_intake_lag).toString() + "  " + comcat_intake_lag);
				min_lag = comcat_intake_lag + action_config.get_comcat_retry_min_gap ();
			}

			// Display list of PDL report retry time lags

			System.out.println ("");

			min_lag = 0L;
			for (;;) {
				long pdl_report_retry_lag = action_config.get_next_pdl_report_retry_lag (min_lag);
				if (pdl_report_retry_lag < 0L) {
					break;
				}
				System.out.println (Duration.ofMillis(pdl_report_retry_lag).toString() + "  " + pdl_report_retry_lag);
				min_lag = pdl_report_retry_lag + action_config.get_comcat_retry_min_gap ();
			}

			return;
		}

		// Unrecognized subcommand.

		System.err.println ("ActionConfig : Unrecognized subcommand : " + args[0]);
		return;

	}

}
