package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import java.time.Duration;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.OAFParameterSet;

/**
 * Configuration file for AAFS server actions.
 * Author: Michael Barall 04/29/2018.
 *
 * All fields are public, since this is just a buffer for reading and writing files.
 *
 * JSON file format:
 *
 *	"ActionConfigFile" = Integer giving file version number, currently 24003.
 *	"forecast_min_gap" = String giving minimum allowed gap between forcasts, in java.time.Duration format.
 *	"forecast_max_delay" = String giving maximum allowed delay in reporting a forecast to PDL, in java.time.Duration format.
 *	"comcat_clock_skew" = Assumed maximum difference between our clock and ComCat clock, in java.time.Duration format.
 *	"comcat_origin_skew" = Assumed maximum change in mainshock origin time, in java.time.Duration format.
 *	"comcat_retry_min_gap" = String giving minimum allowed gap between ComCat retries, in java.time.Duration format.
 *	"comcat_retry_missing" = String giving minimum ComCat retry lag for missing events, in java.time.Duration format.
 *  "seq_spec_min_lag" = String giving minimum time lag at which sequence-specific forecasts can be generated, in java.time.Duration format.
 *  "advisory_dur_week" = String giving minimum time lag at which one-week advisories can be generated, in java.time.Duration format.
 *  "advisory_dur_month" = String giving minimum time lag at which one-month advisories can be generated, in java.time.Duration format.
 *  "advisory_dur_year" = String giving minimum time lag at which one-year advisories can be generated, in java.time.Duration format.
 *  "def_max_forecast_lag" = String giving default maximum forecast lag, in java.time.Duration format.
 *  "withdraw_forecast_lag" = Forecast lag at which a timeline not passing the intake filter can be withdrawn, in java.time.Duration format.
 *  "stale_forecast_option" = Option for stale forecasts: 1 = generate forecast, 2 = skip forecast, 3 = omit from timeline.
 *  "shadow_search_radius" = Real value giving the radius to search for shadowing events, in km.
 *  "shadow_lookback_time" = String giving maximum time before the mainshock to search for shadowing events, in java.time.Duration format.
 *  "shadow_centroid_mag" = Real value giving the minimum magnitude to use for computing centroids, when searching for shadowing events.
 *  "shadow_large_mag" = Real value giving the minimum magnitude for a candidate shadowing event to be considered large.
 *  "poll_short_period" = String giving period for the short polling cycle, in java.time.Duration format.
 *  "poll_short_lookback" = String giving lookback time for the short polling cycle, in java.time.Duration format.
 *  "poll_short_intake_gap" = String giving time gap between intake actions for the short polling cycle, in java.time.Duration format.
 *  "poll_long_period" = String giving period for the long polling cycle, in java.time.Duration format.
 *  "poll_long_lookback" = String giving lookback time for the long polling cycle, in java.time.Duration format.
 *  "poll_long_intake_gap" = String giving time gap between intake actions for the long polling cycle, in java.time.Duration format.
 *  "pdl_intake_max_age" = String giving maximum allowed age for PDL intake, in java.time.Duration format.
 *  "pdl_intake_max_future" = String giving maximum allowed time in future for PDL intake, in java.time.Duration format.
 *  "removal_forecast_age" = String giving age at which forecasts should be removed from PDL, in java.time.Duration format.
 *  "removal_update_skew" = String giving update time clock skew allowance for PDL forecasts, in java.time.Duration format.
 *  "removal_lookback_tmax" = String giving maximum time before present to search for forecasts that need to be removed from PDL, in java.time.Duration format.
 *  "removal_lookback_tmin" = String giving minimum time before present to search for forecasts that need to be removed from PDL, in java.time.Duration format.
 *  "removal_lookback_mag" = Real value giving the minimum magnitude to search for forecasts that need to be removed from PDL.
 *  "removal_check_period" = String giving period for checking for forecasts that need to be removed from PDL, in java.time.Duration format.
 *  "removal_retry_period" = String giving retry interval for checking for forecasts that need to be removed from PDL, in java.time.Duration format.
 *  "removal_event_gap" = String giving gap between processing events with forecasts that may need to be removed from PDL, in java.time.Duration format.
 *  "removal_foreign_block" = String giving time after observing a foreign forecast that removal checks are suppressed, in java.time.Duration format.
 *  "def_injectable_text" = String giving default value of injectable text for PDL JSON files, or "" for none.
 *  [v2] "evseq_enable" = Option to enable event-sequence: 0 = disable, 1 = enable
 *  [v2] "evseq_report" = Option for sending event-sequence reports to PDL by default: 0 = no report, 1 = send report, 2 = delete report.
 *  [v2] "evseq_lookback" = String giving event-sequence lookback time, in java.time.Duration format.
 *  [v2] "evseq_lookahead" = String giving event-sequence lookahead time, in java.time.Duration format.
 *  [v2] "evseq_cap_min_dur" = String giving event-sequence minimum duration when capping, in java.time.Duration format.
 *  [v2] "evseq_cap_gap" = String giving event-sequence gap before capping event, in java.time.Duration format.
 *  [v3] "etas_enable" = Option to enable ETAS forecasts: 0 = disable, 1 = enable, >1 = select alternate ETAS method. 
 *  [v4] "etas_time_limit" = String giving ETAS calculation time limit, in java.time.Duration format.
 *  [v4] "etas_progress_time" = String giving ETAS progress reporting time, in java.time.Duration format.
 *  [v4] "data_fetch_lookback" = String giving data fetch lookback time, in java.time.Duration format.
 *  [v4] "data_fit_dur_min" = String giving minimum duration of a fitting interval, in java.time.Duration format.
 *  [v4] "comcat_cache_1_mag" = Real value giving the minimum magnitude for ComCat cache #1.
 *  [v4] "comcat_cache_1_time" = String giving lookback time for ComCat cache #1, in java.time.Duration format.
 *  [v4] "comcat_cache_2_mag" = Real value giving the minimum magnitude for ComCat cache #2.
 *  [v4] "comcat_cache_2_time" = String giving lookback time for ComCat cache #2, in java.time.Duration format.
 *	"adv_min_mag_bins" = [ Array giving a list of minimum magnitudes for which forecasts are generated, in increasing order.
 *		element = Real value giving minimum magnitude for the bin.
 *	]
 *	"adv_window_start_offs" = [ Array giving a list of time offsets, from the forecast generation time, at which the forecast window starts.
 *		element = String giving offset from forecast generation time, in java.time.Duration format.
 *	]
 *	"adv_window_end_offs" = [ Array giving a list of time offsets, from the forecast generation time, at which the forecast window ends.
 *		element = String giving offset from forecast generation time, in java.time.Duration format.
 *	]
 *	"adv_window_names" = [ Array giving a list of names for the forecast windows.
 *		element = String giving the forecast window name.
 *	]
 *	[v3] "adv_fractile_values" = [ Array giving a list of fractile probabilities to include in the forecast.
 *		element = Real value giving a fractile probability, must be a multiple of 0.0001.
 *	]
 *	[v3] "adv_bar_counts" = [ Array giving a list of counts for a bar graph.
 *		element = Integer giving the lower bound number of expected aftershocks..
 *	]
 *	"forecast_lags" = [ Array giving a list of time lags at which forecasts are generated, in increasing order.
 *		element = String giving time lag since mainshock, in java.time.Duration format.
 *	]
 *	"comcat_retry_lags" = [ Array giving a list of time lags at which ComCat forecast operations are retried, in increasing order.
 *		element = String giving time lag since first attempt, in java.time.Duration format.
 *	]
 *	"comcat_intake_lags" = [ Array giving a list of time lags at which ComCat intake operations are retried, in increasing order.
 *		element = String giving time lag since first attempt, in java.time.Duration format.
 *	]
 *	"pdl_report_retry_lags" = [ Array giving a list of time lags at which PDL report attempts are retried, in increasing order.
 *		element = String giving time lag since first attempt, in java.time.Duration format.
 *	]
 *	"pdl_intake_regions" = [ Array giving a list of intake regions, in the order they are checked.
 *		element = IntakeSphRegion JSON representation, giving region and magnitude thresholds.
 *	]
 */
public class ActionConfigFile implements Marshalable {

	//----- Parameter values -----

	// Minimum gap between forecasts, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long forecast_min_gap;

	// Maximum delay in reporting a forecast to PDL, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long forecast_max_delay;

	// Assumed maximum difference between our clock and ComCat's clock, in milliseconds.
	// (Specifically, if an earthquake occurs at time T then it should be visible in
	// ComCat by the time our clock reads T + comcat_clock_skew.)
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long comcat_clock_skew;

	// Assumed maximum change in mainshock origin time, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long comcat_origin_skew;

	// Minimum gap between ComCat retries, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long comcat_retry_min_gap;

	// Minimum ComCat retry lag for missing events, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long comcat_retry_missing;

	// Minimum time after an earthquake at which sequence-specific forecasts can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long seq_spec_min_lag;

	// Minimum time after an earthquake at which one-week advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long advisory_dur_week;

	// Minimum time after an earthquake at which one-month advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long advisory_dur_month;

	// Minimum time after an earthquake at which one-year advisories can be generated, in milliseconds.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long advisory_dur_year;

	// Default value of the maximum forecast lag.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long def_max_forecast_lag;

	// Forecast lag at which a timeline not passing the intake filter can be withdrawn.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long withdraw_forecast_lag;

	// Option selecting how to handle stale forecasts.
	// (A forecast is stale if another forecast could be issued immediately.)

	public static final int SFOPT_MIN = 1;
	public static final int SFOPT_FORECAST = 1;		// Generate forecast
	public static final int SFOPT_SKIP = 2;			// Skip forecast
	public static final int SFOPT_OMIT = 3;			// Omit from timeline
	public static final int SFOPT_MAX = 3;

	public int stale_forecast_option;

	// Search radius to search for shadowing events, in km.

	public static final double MIN_SEARCH_RADIUS = 1.0;

	public double shadow_search_radius;

	// Amount of time to look back from the mainshock, to search for shadowing events.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long shadow_lookback_time;

	// Minimum magnitude to use for computing centroids, when searching for shadowing events.

	public double shadow_centroid_mag;

	// Minimum magnitude for a candidate shadowing event to be considered large.

	public double shadow_large_mag;

	// Period for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_short_period;

	// Lookback time for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_short_lookback;

	// Time gap between intake actions for the short polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_short_intake_gap;

	// Period for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_long_period;

	// Lookback time for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_long_lookback;

	// Time gap between intake actions for the long polling cycle.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long poll_long_intake_gap;

	// Maximum allowed age for PDL intake.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long pdl_intake_max_age;

	// Maximum allowed time in future for PDL intake.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long pdl_intake_max_future;

	// Age at which forecasts should be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_forecast_age;

	// Update time clock skew allowance for PDL forecasts.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_update_skew;

	// Maximum time before present to search for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_lookback_tmax;

	// Minimum time before present to search for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_lookback_tmin;

	// Minimum magnitude to search for forecasts that need to be removed from PDL

	public double removal_lookback_mag;

	// Period for checking for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_check_period;

	// Retry interval for checking for forecasts that need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_retry_period;

	// Gap between processing events with forecasts that may need to be removed from PDL.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_event_gap;

	// Time after observing a foreign forecast that removal checks are suppressed.
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public long removal_foreign_block;

	// Default value of injectable text for PDL JSON files, or "" for none.

	public String def_injectable_text;

	// Option to enable event-sequence products. [v2]

	public static final int ESENA_MIN = 0;
	public static final int ESENA_DISABLE = 0;		// Completely disable event-sequence products
	public static final int ESENA_ENABLE = 1;		// Enable sending and deleting event-sequence products
	public static final int ESENA_MAX = 1;

	private static final int V1_EVSEQ_ENABLE = 0;	// Default value for v1 files

	public int evseq_enable;

	// Option to send event-sequence reports by default. [v2]

	public static final int ESREP_MIN = 0;
	public static final int ESREP_NO_REPORT = 0;	// Disable sending reports by default
	public static final int ESREP_REPORT = 1;		// Enable sending reports by default
	public static final int ESREP_DELETE = 2;		// Delete existing reports by default
	public static final int ESREP_MAX = 2;

	public static final int DEFAULT_EVSEQ_REPORT = 1;	// Default value for forecast parameters

	private static final int V1_EVSEQ_REPORT = 1;	// Default value for v1 files

	public int evseq_report;

	// Time before mainshock when an event sequence begins. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public static final long DEFAULT_EVSEQ_LOOKBACK = 2592000000L;	// Default value for forecast parameters = 30 days
	public static final long REC_MIN_EVSEQ_LOOKBACK = 60000L;		// Recommended minimum value for forecast parameters = 1 minute
	public static final long REC_MAX_EVSEQ_LOOKBACK = 315360000000L;	// Recommended maximum value for forecast parameters = 10 years

	private static final long V1_EVSEQ_LOOKBACK = 2592000000L;	// Default value for v1 files = 30 days

	public long evseq_lookback;

	// Time after forecast when an event sequence ends. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	private static final long V1_EVSEQ_LOOKAHEAD = 31536000000L;	// Default value for v1 files = 365 days

	public long evseq_lookahead;

	// Minimum duration of an event sequence (after mainshock) when it is capped. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	private static final long V1_EVSEQ_CAP_MIN_DUR = 259200000L;	// Default value for v1 files = 3 days

	public long evseq_cap_min_dur;

	// Time before a capping event when an event sequence ends. [v2]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	private static final long V1_EVSEQ_CAP_GAP = 3600000L;	// Default value for v1 files = 1 hour

	public long evseq_cap_gap;

	// Option to enable ETAS forecasts. [v3]

	public static final int ETAS_ENA_MIN = 0;
	public static final int ETAS_ENA_DISABLE = 0;	// Completely disable ETAS fprecasts
	public static final int ETAS_ENA_ENABLE = 1;	// Enable ETAS forecasts, original ETAS method
	public static final int ETAS_ENA_MAX = 1;

	private static final int V2_ETAS_ENABLE = 0;	// Default value for v2 and earlier files

	public int etas_enable;

	// Time limit for an ETAS calculation. [v4]
	// Must be a whole number of seconds, between 60 and 10^9 seconds, or 0 for no time limit.

	public static final long DEFAULT_ETAS_TIME_LIMIT = 240000L;	// Default value for forecast parameters = 4 minutes
	public static final long NO_ETAS_TIME_LIMIT = 0L;			// Value for no time limit
	public static final long REC_MIN_ETAS_TIME_LIMIT = 60000L;	// Recommended minimum value for forecast parameters =  1 minute

	private static final long V3_ETAS_TIME_LIMIT = 240000L;		// Default value for v3 and earlier files = 4 minutes

	public long etas_time_limit;

	// Time interval between ETAS progress messages. [v4]
	// Must be a whole number of seconds, between 2 and 10^9 seconds, or 0 for no progress messages.
	
	public static final long DEFAULT_ETAS_PROGRESS_TIME = 10000L;	// Default value for forecast parameters = 10 seconds
	public static final long NO_ETAS_PROGRESS_TIME = 0L;			// Value for no progress messages
	public static final long REC_MIN_ETAS_PROGRESS_TIME = 2000L;	// Recommended minimum value for forecast parameters =  2 seconds
	
	private static final long V3_ETAS_PROGRESS_TIME = 10000L;		// Default value for v3 and earlier files = 10 seconds
	
	public long etas_progress_time;

	// Time before mainshock to fetch data from ComCat. [v4]
	// Must be a whole number of seconds, between 0 and 10 years

	public static final long DEFAULT_DATA_FETCH_LOOKBACK = 5184000000L;		// Default value for forecast parameters = 60 days
	public static final long REC_MIN_DATA_FETCH_LOOKBACK = 0L;				// Recommended minimum value for forecast parameters = 0
	public static final long REC_MAX_DATA_FETCH_LOOKBACK = 315360000000L;	// Recommended maximum value for forecast parameters = 10 years

	private static final long V3_DATA_FETCH_LOOKBACK = 5184000000L;	// Default value for v3 and earlier files = 60 days

	public long data_fetch_lookback;

	// Minimum fitting interval duration. [v4]
	// Must be a whole number of seconds, between 2 minutes and 1 year.

	public static final long DEFAULT_DATA_FIT_DUR_MIN = 600000L;	// Default value for forecast parameters = 10 minutes
	public static final long REC_MIN_DATA_FIT_DUR_MIN = 120000L;		// Recommended minimum value for forecast parameters = 2 minutes
	public static final long REC_MAX_DATA_FIT_DUR_MIN = 31536000000L;	// Recommended maximum value for forecast parameters = 1 year

	private static final long V3_DATA_FIT_DUR_MIN = 600000L;	// Default value for v3 and later files = 10 minutes

	public long data_fit_dur_min;

	// Minimum magnitude for ComCat cache #1. [v4]

	public static final double DEFAULT_COMCAT_CACHE_1_MAG = 2.00;	// Default value for forecast parameters

	private static final double V3_COMCAT_CACHE_1_MAG = 2.00;	// Default value for v3 and earlier files

	public double comcat_cache_1_mag;

	// Lookback time for ComCat cache #1. [v4]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public static final long DEFAULT_COMCAT_CACHE_1_TIME = 7862400000L;	// Default value for forecast parameters = 91 days

	private static final long V3_COMCAT_CACHE_1_TIME = 7862400000L;	// Default value for v3 and earlier files = 91 days

	public long comcat_cache_1_time;

	// Minimum magnitude for ComCat cache #2. [v4]

	public static final double DEFAULT_COMCAT_CACHE_2_MAG = 4.95;	// Default value for forecast parameters

	private static final double V3_COMCAT_CACHE_2_MAG = 4.95;	// Default value for v3 and earlier files

	public double comcat_cache_2_mag;

	// Lookback time for ComCat cache #2. [v4]
	// Must be a whole number of seconds, between 1 and 10^9 seconds.

	public static final long DEFAULT_COMCAT_CACHE_2_TIME = 34214400000L;	// Default value for forecast parameters = 396 days

	private static final long V3_COMCAT_CACHE_2_TIME = 34214400000L;	// Default value for v3 and earlier files = 396 days

	public long comcat_cache_2_time;

	// Minimum magnitude for advisory magnitude bins.  Must be in increasing order.

	public ArrayList<Double> adv_min_mag_bins;

	// Time offsets, from forecast generation time, at which the forecast window starts, in milliseconds.
	// Each element must be a whole number of seconds, between -10^9 and 10^9 seconds.
	// The length must equal the number of forecast windows.

	public ArrayList<Long> adv_window_start_offs;

	// Time offsets, from forecast generation time, at which the forecast window ends, in milliseconds.
	// Each element must be a whole number of seconds, between 1 and 10^9 seconds,
	// and must be larger than the corresponding start offset.
	// The length must equal the number of forecast windows.

	public ArrayList<Long> adv_window_end_offs;

	// The name assigned to each forecast window.
	// The length must equal the number of forecast windows.

	public ArrayList<String> adv_window_names;

	// Fractile probabilities to include in the foreast.  Must be in increasing order. [v3]
	// Each value must be a multiple of 0.0001 (requirement set by round_fractile_value()).
	// Must contain the values 0.025, 0.5 and 0.975 for compatibility with forecast tables.
	// Note: ADV_FRACTILE_EPS must be less than 1/3 the minimum spacing between rounded fractile values.

	public ArrayList<Double> adv_fractile_values;

	public static double round_fractile_value (double fractile_value) {
		return SimpleUtils.round_double_via_string ("%.4f", fractile_value);
	}

	private static void def_adv_fractile_values (Collection<Double> x) {	// default for v2 and earlier files 

		//  for (int frac_ix = 0; frac_ix < 79; ++frac_ix) {
		//  	x.add (Double.valueOf ( round_fractile_value (((double)(frac_ix + 1)) / 80.0) ));
		//  }

		for (int frac_ix = 0; frac_ix < 197; ++frac_ix) {
			x.add (Double.valueOf ( round_fractile_value (((double)(frac_ix + 2)) / 200.0) ));
		}

		return;
	}

	private static final double ADV_FRACTILE_EPS = 0.00001;		// Epsilon for matching fractile values
																// Should match OEForecastGrid.MATCH_FRACTILE_EPS

	// Counts for a bar graph in the forecast.  Must be in increasing order, and first element must be zero. [v3]

	public ArrayList<Integer> adv_bar_counts;

	private static void def_adv_bar_counts (Collection<Integer> x) {	// default for v2 and earlier files 

		x.add (Integer.valueOf (0));
		x.add (Integer.valueOf (1));
		x.add (Integer.valueOf (2));
		x.add (Integer.valueOf (5));
		x.add (Integer.valueOf (10));
		x.add (Integer.valueOf (20));
		x.add (Integer.valueOf (50));
		x.add (Integer.valueOf (100));
		x.add (Integer.valueOf (200));
		x.add (Integer.valueOf (500));

		return;
	}

	// Time lags at which forecasts are generated, in milliseconds.  Must be in increasing order.
	// This is time lag since the mainshock.  Must have at least 1 element.
	// The difference between successive elements must be at least the minimum gap.
	// Each element must be a whole number of seconds, between 1 and 10^9 seconds.

	public ArrayList<Long> forecast_lags;

	// Time lags at which ComCat forecast retries are generated, in milliseconds.  Must be in increasing order.
	// This is time lag since the initial attempt.
	// The difference between successive elements must be at least the minimum gap.
	// Each element must be a whole number of seconds, between 1 and 10^9 seconds.

	public ArrayList<Long> comcat_retry_lags;

	// Time lags at which ComCat intake retries are generated, in milliseconds.  Must be in increasing order.
	// This is time lag since the initial attempt.
	// This is used when looking for initial appearance in ComCat of an event reported by PDL.
	// The difference between successive elements must be at least the minimum gap.
	// Each element must be a whole number of seconds, between 1 and 10^9 seconds.

	public ArrayList<Long> comcat_intake_lags;

	// Time lags at which PDL report retries are generated, in milliseconds.  Must be in increasing order.
	// This is time lag since the initial attempt.
	// Each element must be a whole number of seconds, between 1 and 10^9 seconds.

	public ArrayList<Long> pdl_report_retry_lags;

	// Regions for filtering events received from PDL.

	public ArrayList<IntakeSphRegion> pdl_intake_regions;


	//----- Construction -----

	// Default constructor.

	public ActionConfigFile () {
		clear();
	}

	// Clear the contents.

	public void clear () {
		forecast_min_gap = 0L;
		forecast_max_delay = 0L;
		comcat_clock_skew = 0L;
		comcat_origin_skew = 0L;
		comcat_retry_min_gap = 0L;
		comcat_retry_missing = 0L;
		seq_spec_min_lag = 0L;
		advisory_dur_week = 0L;
		advisory_dur_month = 0L;
		advisory_dur_year = 0L;
		def_max_forecast_lag = 0L;
		withdraw_forecast_lag = 0L;
		stale_forecast_option = SFOPT_FORECAST;
		shadow_search_radius = 0.0;
		shadow_lookback_time = 0L;
		shadow_centroid_mag = 0.0;
		shadow_large_mag = 0.0;
		poll_short_period = 0L;
		poll_short_lookback = 0L;
		poll_short_intake_gap = 0L;
		poll_long_period = 0L;
		poll_long_lookback = 0L;
		poll_long_intake_gap = 0L;
		pdl_intake_max_age = 0L;
		pdl_intake_max_future = 0L;
		removal_forecast_age = 0L;
		removal_update_skew = 0L;
		removal_lookback_tmax = 0L;
		removal_lookback_tmin = 0L;
		removal_lookback_mag = 0.0;
		removal_check_period = 0L;
		removal_retry_period = 0L;
		removal_event_gap = 0L;
		removal_foreign_block = 0L;
		def_injectable_text = "";
		evseq_enable = ESENA_DISABLE;
		evseq_report = ESREP_NO_REPORT;
		evseq_lookback = 0L;
		evseq_lookahead = 0L;
		evseq_cap_min_dur = 0L;
		evseq_cap_gap = 0L;
		etas_enable = ETAS_ENA_DISABLE;
		etas_time_limit = 0L;
		etas_progress_time = 0L;
		data_fetch_lookback = 0L;
		data_fit_dur_min = 0L;
		comcat_cache_1_mag = 0.0;
		comcat_cache_1_time = 0L;
		comcat_cache_2_mag = 0.0;
		comcat_cache_2_time = 0L;
		adv_min_mag_bins = new ArrayList<Double>();
		adv_window_start_offs = new ArrayList<Long>();
		adv_window_end_offs = new ArrayList<Long>();
		adv_window_names = new ArrayList<String>();
		adv_fractile_values = new ArrayList<Double>();
		adv_bar_counts = new ArrayList<Integer>();
		forecast_lags = new ArrayList<Long>();
		comcat_retry_lags = new ArrayList<Long>();
		comcat_intake_lags = new ArrayList<Long>();
		pdl_report_retry_lags = new ArrayList<Long>();
		pdl_intake_regions = new ArrayList<IntakeSphRegion>();
		return;
	}

	// Check that a lag time is valid: a whole number of seconds, between 1 and 10^9 seconds.

	private static final long MIN_LAG = 1000L;
	private static final long MAX_LAG = 1000000000000L;
	private static final long UNIT_LAG = 1000L;

	private boolean is_valid_lag (long lag) {
		return (lag >= MIN_LAG && lag <= MAX_LAG && lag % UNIT_LAG == 0L);
	}

	private boolean is_valid_lag (long lag, long min_lag) {
		return (lag >= min_lag && lag <= MAX_LAG && lag % UNIT_LAG == 0L);
	}

	private boolean is_valid_lag (long lag, long min_lag, long max_lag) {
		return (lag >= min_lag && lag <= max_lag && lag % UNIT_LAG == 0L);
	}

	// Check that values are valid, throw an exception if not.

	public void check_invariant () {

		if (!( is_valid_lag(forecast_min_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid forecast_min_gap: " + forecast_min_gap);
		}

		if (!( is_valid_lag(forecast_max_delay) )) {
			throw new RuntimeException("ActionConfigFile: Invalid forecast_max_delay: " + forecast_max_delay);
		}

		if (!( is_valid_lag(comcat_clock_skew) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_clock_skew: " + comcat_clock_skew);
		}

		if (!( is_valid_lag(comcat_origin_skew) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_origin_skew: " + comcat_origin_skew);
		}

		if (!( is_valid_lag(comcat_retry_min_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_retry_min_gap: " + comcat_retry_min_gap);
		}

		if (!( is_valid_lag(comcat_retry_missing) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_retry_missing: " + comcat_retry_missing);
		}

		if (!( is_valid_lag(seq_spec_min_lag) )) {
			throw new RuntimeException("ActionConfigFile: Invalid seq_spec_min_lag: " + seq_spec_min_lag);
		}

		if (!( is_valid_lag(advisory_dur_week) )) {
			throw new RuntimeException("ActionConfigFile: Invalid advisory_dur_week: " + advisory_dur_week);
		}

		if (!( is_valid_lag(advisory_dur_month) )) {
			throw new RuntimeException("ActionConfigFile: Invalid advisory_dur_month: " + advisory_dur_month);
		}

		if (!( is_valid_lag(advisory_dur_year) )) {
			throw new RuntimeException("ActionConfigFile: Invalid advisory_dur_year: " + advisory_dur_year);
		}

		if (!( is_valid_lag(def_max_forecast_lag) )) {
			throw new RuntimeException("ActionConfigFile: Invalid def_max_forecast_lag: " + def_max_forecast_lag);
		}

		if (!( is_valid_lag(withdraw_forecast_lag) )) {
			throw new RuntimeException("ActionConfigFile: Invalid withdraw_forecast_lag: " + withdraw_forecast_lag);
		}

		if (!( stale_forecast_option >= SFOPT_MIN && stale_forecast_option <= SFOPT_MAX )) {
			throw new RuntimeException("ActionConfigFile: Invalid stale_forecast_option: " + stale_forecast_option);
		}

		if (!( shadow_search_radius >= MIN_SEARCH_RADIUS )) {
			throw new RuntimeException("ActionConfigFile: Invalid shadow_search_radius: " + shadow_search_radius);
		}

		if (!( is_valid_lag(shadow_lookback_time) )) {
			throw new RuntimeException("ActionConfigFile: Invalid shadow_lookback_time: " + shadow_lookback_time);
		}

		if (!( is_valid_lag(poll_short_period) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_short_period: " + poll_short_period);
		}

		if (!( is_valid_lag(poll_short_lookback) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_short_lookback: " + poll_short_lookback);
		}

		if (!( is_valid_lag(poll_short_intake_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_short_intake_gap: " + poll_short_intake_gap);
		}

		if (!( is_valid_lag(poll_long_period) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_long_period: " + poll_long_period);
		}

		if (!( is_valid_lag(poll_long_lookback) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_long_lookback: " + poll_long_lookback);
		}

		if (!( is_valid_lag(poll_long_intake_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid poll_long_intake_gap: " + poll_long_intake_gap);
		}

		if (!( is_valid_lag(pdl_intake_max_age) )) {
			throw new RuntimeException("ActionConfigFile: Invalid pdl_intake_max_age: " + pdl_intake_max_age);
		}

		if (!( is_valid_lag(pdl_intake_max_future) )) {
			throw new RuntimeException("ActionConfigFile: Invalid pdl_intake_max_future: " + pdl_intake_max_future);
		}

		if (!( is_valid_lag(removal_forecast_age) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_forecast_age: " + removal_forecast_age);
		}

		if (!( is_valid_lag(removal_update_skew) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_update_skew: " + removal_update_skew);
		}

		if (!( is_valid_lag(removal_lookback_tmax) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_lookback_tmax: " + removal_lookback_tmax);
		}

		if (!( is_valid_lag(removal_lookback_tmin) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_lookback_tmin: " + removal_lookback_tmin);
		}

		if (!( is_valid_lag(removal_check_period) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_check_period: " + removal_check_period);
		}

		if (!( is_valid_lag(removal_retry_period) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_retry_period: " + removal_retry_period);
		}

		if (!( is_valid_lag(removal_event_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_event_gap: " + removal_event_gap);
		}

		if (!( is_valid_lag(removal_foreign_block) )) {
			throw new RuntimeException("ActionConfigFile: Invalid removal_foreign_block: " + removal_foreign_block);
		}

		if (!( def_injectable_text != null )) {
			throw new RuntimeException("ActionConfigFile: Invalid def_injectable_text: " + ((def_injectable_text == null) ? "null" : def_injectable_text));
		}

		if (!( evseq_enable >= ESENA_MIN && evseq_enable <= ESENA_MAX )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_enable: " + evseq_enable);
		}

		if (!( evseq_report >= ESREP_MIN && evseq_report <= ESREP_MAX )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_report: " + evseq_report);
		}

		if (!( is_valid_lag(evseq_lookback) )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_lookback: " + evseq_lookback);
		}

		if (!( is_valid_lag(evseq_lookahead) )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_lookahead: " + evseq_lookahead);
		}

		if (!( is_valid_lag(evseq_cap_min_dur) )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_cap_min_dur: " + evseq_cap_min_dur);
		}

		if (!( is_valid_lag(evseq_cap_gap) )) {
			throw new RuntimeException("ActionConfigFile: Invalid evseq_cap_gap: " + evseq_cap_gap);
		}

		if (!( etas_enable >= ETAS_ENA_MIN && etas_enable <= ETAS_ENA_MAX )) {
			throw new RuntimeException("ActionConfigFile: Invalid etas_enable: " + etas_enable);
		}

		if (!( etas_time_limit == NO_ETAS_TIME_LIMIT || is_valid_lag(etas_time_limit, REC_MIN_ETAS_TIME_LIMIT) )) {
			throw new RuntimeException("ActionConfigFile: Invalid etas_time_limit: " + etas_time_limit);
		}

		if (!( etas_progress_time == NO_ETAS_PROGRESS_TIME || is_valid_lag(etas_progress_time, REC_MIN_ETAS_PROGRESS_TIME) )) {
			throw new RuntimeException("ActionConfigFile: Invalid etas_progress_time: " + etas_progress_time);
		}

		if (!( is_valid_lag(data_fetch_lookback, REC_MIN_DATA_FETCH_LOOKBACK, REC_MAX_DATA_FETCH_LOOKBACK) )) {
			throw new RuntimeException("ActionConfigFile: Invalid data_fetch_lookback: " + data_fetch_lookback);
		}

		if (!( is_valid_lag(data_fit_dur_min, REC_MIN_DATA_FIT_DUR_MIN, REC_MAX_DATA_FIT_DUR_MIN) )) {
			throw new RuntimeException("ActionConfigFile: Invalid data_fit_dur_min: " + data_fit_dur_min);
		}

		if (!( is_valid_lag(comcat_cache_1_time) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_cache_1_time: " + comcat_cache_1_time);
		}

		if (!( is_valid_lag(comcat_cache_2_time) )) {
			throw new RuntimeException("ActionConfigFile: Invalid comcat_cache_2_time: " + comcat_cache_2_time);
		}

		int n;
		long min_lag;

		n = adv_min_mag_bins.size();
		
		if (!( n > 0 )) {
			throw new RuntimeException("ActionConfigFile: Empty list of advisory minimum magnitude bins");
		}

		for (int i = 1; i < n; ++i) {
			if (!( adv_min_mag_bins.get(i-1).doubleValue() < adv_min_mag_bins.get(i).doubleValue() )) {
				throw new RuntimeException("ActionConfigFile: Out-of-order adv_min_mag_bin: " + adv_min_mag_bins.get(i).doubleValue() + ", index = " + i);
			}
		}

		n = adv_window_start_offs.size();
		min_lag = -MAX_LAG;
		
		if (!( n > 0 )) {
			throw new RuntimeException("ActionConfigFile: Empty list of advisory window start offsets");
		}

		for (int i = 0; i < n; ++i) {
			long adv_window_start_off = adv_window_start_offs.get(i);
			if (!( is_valid_lag(adv_window_start_off, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid adv_window_start_off: " + adv_window_start_off + ", index = " + i);
			}
		}

		n = adv_window_end_offs.size();
		
		if (!( n == adv_window_start_offs.size() )) {
			throw new RuntimeException("ActionConfigFile: Length mismatch for list of advisory window end offsets");
		}

		for (int i = 0; i < n; ++i) {
			long adv_window_end_off = adv_window_end_offs.get(i);
			min_lag = adv_window_start_offs.get(i) + 1L;
			if (!( is_valid_lag(adv_window_end_off, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid adv_window_end_off: " + adv_window_end_off + ", index = " + i);
			}
		}

		n = adv_window_names.size();
		
		if (!( n == adv_window_start_offs.size() )) {
			throw new RuntimeException("ActionConfigFile: Length mismatch for list of advisory window names");
		}

		for (int i = 0; i < n; ++i) {
			String adv_window_name = adv_window_names.get(i);
			if (!( adv_window_name != null && adv_window_name.length() > 0 )) {
				throw new RuntimeException("ActionConfigFile: Invalid adv_window_name: " + ((adv_window_name == null) ? "null" : adv_window_name) + ", index = " + i);
			}
		}

		n = adv_fractile_values.size();
		double last_frac = 0.0;
		boolean f_found_025 = false;
		boolean f_found_500 = false;
		boolean f_found_975 = false;
		
		if (!( n > 0 )) {
			throw new RuntimeException("ActionConfigFile: Empty list of fractile probabilities");
		}

		for (int i = 0; i < n; ++i) {
			double adv_fractile_value = adv_fractile_values.get(i).doubleValue();
			if (!( 0.0 < adv_fractile_value && adv_fractile_value < 1.0 )) {
				throw new RuntimeException("ActionConfigFile: Out-of-range adv_fractile_value: " + adv_fractile_value + ", index = " + i);
			}
			double x = round_fractile_value (adv_fractile_value);
			if (Math.abs(x - adv_fractile_value) > ADV_FRACTILE_EPS * 0.1) {
				throw new RuntimeException("ActionConfigFile: Fractile is not a multiple of 0.0001: Invalid adv_fractile_value: " + adv_fractile_value + ", index = " + i);
			}
			if (!( last_frac + (ADV_FRACTILE_EPS * 2.0) < x )) {
				throw new RuntimeException("ActionConfigFile: Out-of-order or too-closely-spaced adv_fractile_value: " + adv_fractile_value + ", index = " + i);
			}
			if (Math.abs(x - 0.025) <= ADV_FRACTILE_EPS * 0.1) {
				f_found_025 = true;
			}
			if (Math.abs(x - 0.500) <= ADV_FRACTILE_EPS * 0.1) {
				f_found_500 = true;
			}
			if (Math.abs(x - 0.975) <= ADV_FRACTILE_EPS * 0.1) {
				f_found_975 = true;
			}
			last_frac = x;
		}
		if (!( f_found_025 && f_found_500 && f_found_975 )) {
			throw new RuntimeException("ActionConfigFile: Fractile list adv_fractile_values does not contain required values 0.025, 0.5, and 0.975");
		}

		n = adv_bar_counts.size();
		
		if (!( n > 0 )) {
			throw new RuntimeException("ActionConfigFile: Empty list of advisory bar counts");
		}

		if (!( adv_bar_counts.get(0).intValue() == 0 )) {
			throw new RuntimeException("ActionConfigFile: Advisory bar count list adv_bar_counts does not begin with 0");
		}
		for (int i = 1; i < n; ++i) {
			if (!( adv_bar_counts.get(i-1).intValue() < adv_bar_counts.get(i).intValue() )) {
				throw new RuntimeException("ActionConfigFile: Out-of-order adv_bar_count: " + adv_bar_counts.get(i).intValue() + ", index = " + i);
			}
		}

		n = forecast_lags.size();
		min_lag = MIN_LAG;
		
		if (!( n > 0 )) {
			throw new RuntimeException("ActionConfigFile: Empty forecast_lags list");
		}

		for (int i = 0; i < n; ++i) {
			long forecast_lag = forecast_lags.get(i);
			if (!( is_valid_lag(forecast_lag, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid forecast_lag: " + forecast_lag + ", index = " + i);
			}
			min_lag = forecast_lag + forecast_min_gap;
		}

		n = comcat_retry_lags.size();
		min_lag = MIN_LAG;

		for (int i = 0; i < n; ++i) {
			long comcat_retry_lag = comcat_retry_lags.get(i);
			if (!( is_valid_lag(comcat_retry_lag, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid comcat_retry_lag: " + comcat_retry_lag + ", index = " + i);
			}
			min_lag = comcat_retry_lag + comcat_retry_min_gap;
		}

		n = comcat_intake_lags.size();
		min_lag = MIN_LAG;

		for (int i = 0; i < n; ++i) {
			long comcat_intake_lag = comcat_intake_lags.get(i);
			if (!( is_valid_lag(comcat_intake_lag, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid comcat_intake_lag: " + comcat_intake_lag + ", index = " + i);
			}
			min_lag = comcat_intake_lag + comcat_retry_min_gap;
		}

		n = pdl_report_retry_lags.size();
		min_lag = MIN_LAG;

		for (int i = 0; i < n; ++i) {
			long pdl_report_retry_lag = pdl_report_retry_lags.get(i);
			if (!( is_valid_lag(pdl_report_retry_lag, min_lag) )) {
				throw new RuntimeException("ActionConfigFile: Invalid pdl_report_retry_lag: " + pdl_report_retry_lag + ", index = " + i);
			}
			min_lag = pdl_report_retry_lag + comcat_retry_min_gap;
		}

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("ActionConfigFile:" + "\n");

		result.append ("forecast_min_gap = " + Duration.ofMillis(forecast_min_gap).toString() + "\n");
		result.append ("forecast_max_delay = " + Duration.ofMillis(forecast_max_delay).toString() + "\n");
		result.append ("comcat_clock_skew = " + Duration.ofMillis(comcat_clock_skew).toString() + "\n");
		result.append ("comcat_origin_skew = " + Duration.ofMillis(comcat_origin_skew).toString() + "\n");
		result.append ("comcat_retry_min_gap = " + Duration.ofMillis(comcat_retry_min_gap).toString() + "\n");
		result.append ("comcat_retry_missing = " + Duration.ofMillis(comcat_retry_missing).toString() + "\n");
		result.append ("seq_spec_min_lag = " + Duration.ofMillis(seq_spec_min_lag).toString() + "\n");
		result.append ("advisory_dur_week = " + Duration.ofMillis(advisory_dur_week).toString() + "\n");
		result.append ("advisory_dur_month = " + Duration.ofMillis(advisory_dur_month).toString() + "\n");
		result.append ("advisory_dur_year = " + Duration.ofMillis(advisory_dur_year).toString() + "\n");
		result.append ("def_max_forecast_lag = " + Duration.ofMillis(def_max_forecast_lag).toString() + "\n");
		result.append ("withdraw_forecast_lag = " + Duration.ofMillis(withdraw_forecast_lag).toString() + "\n");
		result.append ("stale_forecast_option = " + stale_forecast_option + "\n");
		result.append ("shadow_search_radius = " + shadow_search_radius + "\n");
		result.append ("shadow_lookback_time = " + Duration.ofMillis(shadow_lookback_time).toString() + "\n");
		result.append ("shadow_centroid_mag = " + shadow_centroid_mag + "\n");
		result.append ("shadow_large_mag = " + shadow_large_mag + "\n");
		result.append ("poll_short_period = " + Duration.ofMillis(poll_short_period).toString() + "\n");
		result.append ("poll_short_lookback = " + Duration.ofMillis(poll_short_lookback).toString() + "\n");
		result.append ("poll_short_intake_gap = " + Duration.ofMillis(poll_short_intake_gap).toString() + "\n");
		result.append ("poll_long_period = " + Duration.ofMillis(poll_long_period).toString() + "\n");
		result.append ("poll_long_lookback = " + Duration.ofMillis(poll_long_lookback).toString() + "\n");
		result.append ("poll_long_intake_gap = " + Duration.ofMillis(poll_long_intake_gap).toString() + "\n");
		result.append ("pdl_intake_max_age = " + Duration.ofMillis(pdl_intake_max_age).toString() + "\n");
		result.append ("pdl_intake_max_future = " + Duration.ofMillis(pdl_intake_max_future).toString() + "\n");
		result.append ("removal_forecast_age = " + Duration.ofMillis(removal_forecast_age).toString() + "\n");
		result.append ("removal_update_skew = " + Duration.ofMillis(removal_update_skew).toString() + "\n");
		result.append ("removal_lookback_tmax = " + Duration.ofMillis(removal_lookback_tmax).toString() + "\n");
		result.append ("removal_lookback_tmin = " + Duration.ofMillis(removal_lookback_tmin).toString() + "\n");
		result.append ("removal_lookback_mag = " + removal_lookback_mag + "\n");
		result.append ("removal_check_period = " + Duration.ofMillis(removal_check_period).toString() + "\n");
		result.append ("removal_retry_period = " + Duration.ofMillis(removal_retry_period).toString() + "\n");
		result.append ("removal_event_gap = " + Duration.ofMillis(removal_event_gap).toString() + "\n");
		result.append ("removal_foreign_block = " + Duration.ofMillis(removal_foreign_block).toString() + "\n");
		result.append ("def_injectable_text = " + ((def_injectable_text == null) ? "null" : def_injectable_text) + "\n");

		result.append ("evseq_enable = " + evseq_enable + "\n");
		result.append ("evseq_report = " + evseq_report + "\n");
		result.append ("evseq_lookback = " + Duration.ofMillis(evseq_lookback).toString() + "\n");
		result.append ("evseq_lookahead = " + Duration.ofMillis(evseq_lookahead).toString() + "\n");
		result.append ("evseq_cap_min_dur = " + Duration.ofMillis(evseq_cap_min_dur).toString() + "\n");
		result.append ("evseq_cap_gap = " + Duration.ofMillis(evseq_cap_gap).toString() + "\n");

		result.append ("etas_enable = " + etas_enable + "\n");

		result.append ("etas_time_limit = " + Duration.ofMillis(etas_time_limit).toString() + "\n");
		result.append ("etas_progress_time = " + Duration.ofMillis(etas_progress_time).toString() + "\n");
		result.append ("data_fetch_lookback = " + Duration.ofMillis(data_fetch_lookback).toString() + "\n");
		result.append ("data_fit_dur_min = " + Duration.ofMillis(data_fit_dur_min).toString() + "\n");
		result.append ("comcat_cache_1_mag = " + comcat_cache_1_mag + "\n");
		result.append ("comcat_cache_1_time = " + Duration.ofMillis(comcat_cache_1_time).toString() + "\n");
		result.append ("comcat_cache_2_mag = " + comcat_cache_2_mag + "\n");
		result.append ("comcat_cache_2_time = " + Duration.ofMillis(comcat_cache_2_time).toString() + "\n");

		result.append ("adv_min_mag_bins = [" + "\n");
		for (int i = 0; i < adv_min_mag_bins.size(); ++i) {
			double adv_min_mag_bin = adv_min_mag_bins.get(i);
			result.append ("  " + i + ":  " + adv_min_mag_bin + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_start_offs = [" + "\n");
		for (int i = 0; i < adv_window_start_offs.size(); ++i) {
			long adv_window_start_off = adv_window_start_offs.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(adv_window_start_off).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_end_offs = [" + "\n");
		for (int i = 0; i < adv_window_end_offs.size(); ++i) {
			long adv_window_end_off = adv_window_end_offs.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(adv_window_end_off).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_names = [" + "\n");
		for (int i = 0; i < adv_window_names.size(); ++i) {
			String adv_window_name = adv_window_names.get(i);
			result.append ("  " + i + ":  " + adv_window_name + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_fractile_values = [" + "\n");
		for (int i = 0; i < adv_fractile_values.size(); ++i) {
			double adv_fractile_value = adv_fractile_values.get(i);
			result.append ("  " + i + ":  " + adv_fractile_value + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_bar_counts = [" + "\n");
		for (int i = 0; i < adv_bar_counts.size(); ++i) {
			int adv_bar_count = adv_bar_counts.get(i);
			result.append ("  " + i + ":  " + adv_bar_count + "\n");
		}
		result.append ("]" + "\n");

		result.append ("forecast_lags = [" + "\n");
		for (int i = 0; i < forecast_lags.size(); ++i) {
			long forecast_lag = forecast_lags.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(forecast_lag).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("comcat_retry_lags = [" + "\n");
		for (int i = 0; i < comcat_retry_lags.size(); ++i) {
			long comcat_retry_lag = comcat_retry_lags.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(comcat_retry_lag).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("comcat_intake_lags = [" + "\n");
		for (int i = 0; i < comcat_intake_lags.size(); ++i) {
			long comcat_intake_lag = comcat_intake_lags.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(comcat_intake_lag).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("pdl_report_retry_lags = [" + "\n");
		for (int i = 0; i < pdl_report_retry_lags.size(); ++i) {
			long pdl_report_retry_lag = pdl_report_retry_lags.get(i);
			result.append ("  " + i + ":  " + Duration.ofMillis(pdl_report_retry_lag).toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("pdl_intake_regions = [" + "\n");
		for (int i = 0; i < pdl_intake_regions.size(); ++i) {
			result.append (pdl_intake_regions.get(i).toString() + "\n");
		}
		result.append ("]" + "\n");

		return result.toString();
	}


	//----- Service functions -----

	// Get the first element of forecast_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_forecast_lag (long min_lag) {

		// Binary search

		int index = Collections.binarySearch (forecast_lags, Long.valueOf(min_lag));

		// If not found, convert to index of next larger element

		if (index < 0) {
			index = -(index + 1);
		}

		// If past end of list, then return -1

		if (index >= forecast_lags.size()) {
			return -1L;
		}

		// Return the lag value from the list

		return forecast_lags.get(index).longValue();
	}

	// Get the first element of forecast_lags that is >= the supplied min_lag and <= the supplied max_lag.
	// The return is -1 if there is no element in the given range.
	// If max_lag <= 0, then def_max_forecast_lag is used as the upper bound.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_forecast_lag (long min_lag, long max_lag) {

		// Binary search

		int index = Collections.binarySearch (forecast_lags, Long.valueOf(min_lag));

		// If not found, convert to index of next larger element

		if (index < 0) {
			index = -(index + 1);
		}

		// If past end of list, then return -1

		if (index >= forecast_lags.size()) {
			return -1L;
		}

		// The value from the list

		long result = forecast_lags.get(index).longValue();

		// Compare to upper bound

		long eff_max_lag = max_lag;
		if (eff_max_lag <= 0L) {
			eff_max_lag = def_max_forecast_lag;
		}

		if (result > eff_max_lag) {
			return -1L;
		}

		// Return the lag value from the list

		return forecast_lags.get(index).longValue();
	}

	// Get the first element of comcat_retry_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_comcat_retry_lag (long min_lag) {

		// Binary search

		int index = Collections.binarySearch (comcat_retry_lags, Long.valueOf(min_lag));

		// If not found, convert to index of next larger element

		if (index < 0) {
			index = -(index + 1);
		}

		// If past end of list, then return -1

		if (index >= comcat_retry_lags.size()) {
			return -1L;
		}

		// Return the lag value from the list

		return comcat_retry_lags.get(index).longValue();
	}

	// Get the first element of comcat_intake_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_comcat_intake_lag (long min_lag) {

		// Binary search

		int index = Collections.binarySearch (comcat_intake_lags, Long.valueOf(min_lag));

		// If not found, convert to index of next larger element

		if (index < 0) {
			index = -(index + 1);
		}

		// If past end of list, then return -1

		if (index >= comcat_intake_lags.size()) {
			return -1L;
		}

		// Return the lag value from the list

		return comcat_intake_lags.get(index).longValue();
	}

	// Get the first element of pdl_report_retry_lags that is >= the supplied min_lag.
	// The return is -1 if the supplied min_lag is greater than all elements.
	// If a value is found, it is guaranteed to be a whole number of seconds, from 1 to 10^9 seconds.

	public long get_next_pdl_report_retry_lag (long min_lag) {

		// Binary search

		int index = Collections.binarySearch (pdl_report_retry_lags, Long.valueOf(min_lag));

		// If not found, convert to index of next larger element

		if (index < 0) {
			index = -(index + 1);
		}

		// If past end of list, then return -1

		if (index >= pdl_report_retry_lags.size()) {
			return -1L;
		}

		// Return the lag value from the list

		return pdl_report_retry_lags.get(index).longValue();
	}

	// Get the pdl intake region that satisfies the min_mag criterion.
	// If found, the region is returned.
	// If not found, null is returned.

	public IntakeSphRegion get_pdl_intake_region_for_min_mag (double lat, double lon, double mag) {

		// Construct the SphLatLon object

		double the_lon = lon;
		while (the_lon < -180.0) {
			the_lon += 360.0; 
		}
		while (the_lon > 180.0) {
			the_lon -= 360.0; 
		}
		SphLatLon loc = new SphLatLon (lat, the_lon);

		// Search the list of regions

		for (IntakeSphRegion intake_region : pdl_intake_regions) {
			if (intake_region.contains (loc, mag)) {
				return intake_region;
			}
		}
		return null;
	}

	// Get the pdl intake region that satisfies the intake_mag criterion.
	// If found, the region is returned.
	// If not found, null is returned.

	public IntakeSphRegion get_pdl_intake_region_for_intake_mag (double lat, double lon, double mag) {

		// Construct the SphLatLon object

		double the_lon = lon;
		while (the_lon < -180.0) {
			the_lon += 360.0; 
		}
		while (the_lon > 180.0) {
			the_lon -= 360.0; 
		}
		SphLatLon loc = new SphLatLon (lat, the_lon);

		// Search the list of regions

		for (IntakeSphRegion intake_region : pdl_intake_regions) {
			if (intake_region.contains_intake (loc, mag)) {
				return intake_region;
			}
		}
		return null;
	}

	// Get the minimum magnitude for the min_mag criterion in any intake region.
	// The result is 10.0 if there are no intake regions.

	public double get_pdl_intake_region_min_min_mag () {
		double mag = 10.0;

		for (IntakeSphRegion intake_region : pdl_intake_regions) {
			if (mag > intake_region.get_min_mag()) {
				mag = intake_region.get_min_mag();
			}
		}

		return mag;
	}

	// Get the minimum magnitude for the intake_mag criterion in any intake region.
	// The result is 10.0 if there are no intake regions.

	public double get_pdl_intake_region_min_intake_mag () {
		double mag = 10.0;

		for (IntakeSphRegion intake_region : pdl_intake_regions) {
			if (mag > intake_region.get_intake_mag()) {
				mag = intake_region.get_intake_mag();
			}
		}

		return mag;
	}




	// Return a string describing an event-sequence enable value.

	public static String get_esena_as_string (int esena) {

		switch (esena) {
		case ESENA_DISABLE: return "ESENA_DISABLE";
		case ESENA_ENABLE: return "ESENA_ENABLE";
		}

		return "ESENA_INVALID(" + esena + ")";
	}




	// Return a string describing an event-sequence reporting option value.

	public static String get_esrep_as_string (int esrep) {

		switch (esrep) {
		case ESREP_NO_REPORT: return "ESREP_NO_REPORT";
		case ESREP_REPORT: return "ESREP_REPORT";
		case ESREP_DELETE: return "ESREP_DELETE";
		}

		return "ESREP_INVALID(" + esrep + ")";
	}




	// Return a string describing an ETAS forecast enable value.

	public static String get_etas_ena_as_string (int etas_ena) {

		switch (etas_ena) {
		case ETAS_ENA_DISABLE: return "ETAS_ENA_DISABLE";
		case ETAS_ENA_ENABLE: return "ETAS_ENA_ENABLE";
		}

		return "ETAS_ENA_INVALID(" + etas_ena + ")";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 24001;
	private static final int MARSHAL_VER_2 = 24002;
	private static final int MARSHAL_VER_3 = 24003;
	private static final int MARSHAL_VER_4 = 24004;

	private static final String M_VERSION_NAME = "ActionConfigFile";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 24000;
	protected static final int MARSHAL_ACTION_CFG = 24001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_ACTION_CFG;
	}

	// Marshal a duration.

	public static void marshal_duration (MarshalWriter writer, String name, long duration) {
		writer.marshalString (name, Duration.ofMillis(duration).toString());
		return;
	}

	// Unmarshal a duration.

	public static long unmarshal_duration (MarshalReader reader, String name) {
		return Duration.parse(reader.unmarshalString(name)).toMillis();
	}

	// Marshal a duration list.

	public static void marshal_duration_list (MarshalWriter writer, String name, List<Long> durations) {
		int n = durations.size();
		writer.marshalArrayBegin (name, n);
		for (Long duration : durations) {
			marshal_duration (writer, null, duration.longValue());
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a duration list.

	public static ArrayList<Long> unmarshal_duration_list (MarshalReader reader, String name) {
		ArrayList<Long> duration_list = new ArrayList<Long>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			duration_list.add (Long.valueOf (unmarshal_duration (reader, null)));
		}
		reader.unmarshalArrayEnd ();
		return duration_list;
	}

	// Marshal an intake region.

	public static void marshal_intake_region (MarshalWriter writer, String name, IntakeSphRegion intake_region) {

		// Begin the JSON object

		writer.marshalMapBegin (name);

		// Write the name

		writer.marshalString ("name", intake_region.get_name());

		// Write the magnitudes
				
		writer.marshalDouble ("min_mag", intake_region.get_min_mag());
		writer.marshalDouble ("intake_mag", intake_region.get_intake_mag());

		// Write the spherical region

		SphRegion.marshal_poly (writer, "region", intake_region.get_region()) ;

		// End the JSON object

		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal an intake region.

	public static IntakeSphRegion unmarshal_intake_region (MarshalReader reader, String name) {

		// Begin the JSON object

		reader.unmarshalMapBegin (name);

		// Get the name

		String region_name = reader.unmarshalString ("name");

		// Get the magnitudes
				
		double min_mag = reader.unmarshalDouble ("min_mag");
		double intake_mag = reader.unmarshalDouble ("intake_mag");

		// Get the spherical region

		SphRegion sph_region = SphRegion.unmarshal_poly (reader, "region") ;

		if (sph_region == null) {
			throw new RuntimeException("ActionConfigFile: No spherical region specified");
		}

		// End the JSON object

		reader.unmarshalMapEnd ();

		// Form the region

		return new IntakeSphRegion (region_name, sph_region, min_mag, intake_mag);
	}

	// Marshal an intake region list.

	public static void marshal_intake_region_list (MarshalWriter writer, String name, List<IntakeSphRegion> intake_regions) {
		int n = intake_regions.size();
		writer.marshalArrayBegin (name, n);
		for (IntakeSphRegion intake_region : intake_regions) {
			marshal_intake_region (writer, null, intake_region);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an intake region list.

	public static ArrayList<IntakeSphRegion> unmarshal_intake_region_list (MarshalReader reader, String name) {
		ArrayList<IntakeSphRegion> intake_region_list = new ArrayList<IntakeSphRegion>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			intake_region_list.add (unmarshal_intake_region (reader, null));
		}
		reader.unmarshalArrayEnd ();
		return intake_region_list;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Error check

		check_invariant();

		// Version

		int ver = MARSHAL_VER_4;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			marshal_duration           (writer, "forecast_min_gap"     , forecast_min_gap     );
			marshal_duration           (writer, "forecast_max_delay"   , forecast_max_delay   );
			marshal_duration           (writer, "comcat_clock_skew"    , comcat_clock_skew    );
			marshal_duration           (writer, "comcat_origin_skew"   , comcat_origin_skew   );
			marshal_duration           (writer, "comcat_retry_min_gap" , comcat_retry_min_gap );
			marshal_duration           (writer, "comcat_retry_missing" , comcat_retry_missing );
			marshal_duration           (writer, "seq_spec_min_lag"     , seq_spec_min_lag     );
			marshal_duration           (writer, "advisory_dur_week"    , advisory_dur_week    );
			marshal_duration           (writer, "advisory_dur_month"   , advisory_dur_month   );
			marshal_duration           (writer, "advisory_dur_year"    , advisory_dur_year    );

			marshal_duration           (writer, "def_max_forecast_lag" , def_max_forecast_lag );
			marshal_duration           (writer, "withdraw_forecast_lag", withdraw_forecast_lag);
			writer.marshalInt          (        "stale_forecast_option", stale_forecast_option);
			writer.marshalDouble       (        "shadow_search_radius" , shadow_search_radius );
			marshal_duration           (writer, "shadow_lookback_time" , shadow_lookback_time );
			writer.marshalDouble       (        "shadow_centroid_mag"  , shadow_centroid_mag  );
			writer.marshalDouble       (        "shadow_large_mag"     , shadow_large_mag     );
			marshal_duration           (writer, "poll_short_period"    , poll_short_period    );
			marshal_duration           (writer, "poll_short_lookback"  , poll_short_lookback  );
			marshal_duration           (writer, "poll_short_intake_gap", poll_short_intake_gap);
			marshal_duration           (writer, "poll_long_period"     , poll_long_period     );
			marshal_duration           (writer, "poll_long_lookback"   , poll_long_lookback   );
			marshal_duration           (writer, "poll_long_intake_gap" , poll_long_intake_gap );
			marshal_duration           (writer, "pdl_intake_max_age"   , pdl_intake_max_age   );
			marshal_duration           (writer, "pdl_intake_max_future", pdl_intake_max_future);
			marshal_duration           (writer, "removal_forecast_age" , removal_forecast_age );
			marshal_duration           (writer, "removal_update_skew"  , removal_update_skew  );
			marshal_duration           (writer, "removal_lookback_tmax", removal_lookback_tmax);
			marshal_duration           (writer, "removal_lookback_tmin", removal_lookback_tmin);
			writer.marshalDouble       (        "removal_lookback_mag" , removal_lookback_mag );
			marshal_duration           (writer, "removal_check_period" , removal_check_period );
			marshal_duration           (writer, "removal_retry_period" , removal_retry_period );
			marshal_duration           (writer, "removal_event_gap"    , removal_event_gap    );
			marshal_duration           (writer, "removal_foreign_block", removal_foreign_block);
			writer.marshalString       (        "def_injectable_text"  , def_injectable_text  );

			writer.marshalDoubleCollection     ("adv_min_mag_bins"     , adv_min_mag_bins     );
			marshal_duration_list      (writer, "adv_window_start_offs", adv_window_start_offs);
			marshal_duration_list      (writer, "adv_window_end_offs"  , adv_window_end_offs  );
			writer.marshalStringCollection     ("adv_window_names"     , adv_window_names     );

			marshal_duration_list      (writer, "forecast_lags"        , forecast_lags        );
			marshal_duration_list      (writer, "comcat_retry_lags"    , comcat_retry_lags    );
			marshal_duration_list      (writer, "comcat_intake_lags"   , comcat_intake_lags   );
			marshal_duration_list      (writer, "pdl_report_retry_lags", pdl_report_retry_lags);
			marshal_intake_region_list (writer, "pdl_intake_regions"   , pdl_intake_regions   );
	
			break;

		case MARSHAL_VER_2:

			marshal_duration           (writer, "forecast_min_gap"     , forecast_min_gap     );
			marshal_duration           (writer, "forecast_max_delay"   , forecast_max_delay   );
			marshal_duration           (writer, "comcat_clock_skew"    , comcat_clock_skew    );
			marshal_duration           (writer, "comcat_origin_skew"   , comcat_origin_skew   );
			marshal_duration           (writer, "comcat_retry_min_gap" , comcat_retry_min_gap );
			marshal_duration           (writer, "comcat_retry_missing" , comcat_retry_missing );
			marshal_duration           (writer, "seq_spec_min_lag"     , seq_spec_min_lag     );
			marshal_duration           (writer, "advisory_dur_week"    , advisory_dur_week    );
			marshal_duration           (writer, "advisory_dur_month"   , advisory_dur_month   );
			marshal_duration           (writer, "advisory_dur_year"    , advisory_dur_year    );

			marshal_duration           (writer, "def_max_forecast_lag" , def_max_forecast_lag );
			marshal_duration           (writer, "withdraw_forecast_lag", withdraw_forecast_lag);
			writer.marshalInt          (        "stale_forecast_option", stale_forecast_option);
			writer.marshalDouble       (        "shadow_search_radius" , shadow_search_radius );
			marshal_duration           (writer, "shadow_lookback_time" , shadow_lookback_time );
			writer.marshalDouble       (        "shadow_centroid_mag"  , shadow_centroid_mag  );
			writer.marshalDouble       (        "shadow_large_mag"     , shadow_large_mag     );
			marshal_duration           (writer, "poll_short_period"    , poll_short_period    );
			marshal_duration           (writer, "poll_short_lookback"  , poll_short_lookback  );
			marshal_duration           (writer, "poll_short_intake_gap", poll_short_intake_gap);
			marshal_duration           (writer, "poll_long_period"     , poll_long_period     );
			marshal_duration           (writer, "poll_long_lookback"   , poll_long_lookback   );
			marshal_duration           (writer, "poll_long_intake_gap" , poll_long_intake_gap );
			marshal_duration           (writer, "pdl_intake_max_age"   , pdl_intake_max_age   );
			marshal_duration           (writer, "pdl_intake_max_future", pdl_intake_max_future);
			marshal_duration           (writer, "removal_forecast_age" , removal_forecast_age );
			marshal_duration           (writer, "removal_update_skew"  , removal_update_skew  );
			marshal_duration           (writer, "removal_lookback_tmax", removal_lookback_tmax);
			marshal_duration           (writer, "removal_lookback_tmin", removal_lookback_tmin);
			writer.marshalDouble       (        "removal_lookback_mag" , removal_lookback_mag );
			marshal_duration           (writer, "removal_check_period" , removal_check_period );
			marshal_duration           (writer, "removal_retry_period" , removal_retry_period );
			marshal_duration           (writer, "removal_event_gap"    , removal_event_gap    );
			marshal_duration           (writer, "removal_foreign_block", removal_foreign_block);
			writer.marshalString       (        "def_injectable_text"  , def_injectable_text  );

			writer.marshalInt          (        "evseq_enable"         , evseq_enable         );
			writer.marshalInt          (        "evseq_report"         , evseq_report         );
			marshal_duration           (writer, "evseq_lookback"       , evseq_lookback       );
			marshal_duration           (writer, "evseq_lookahead"      , evseq_lookahead      );
			marshal_duration           (writer, "evseq_cap_min_dur"    , evseq_cap_min_dur    );
			marshal_duration           (writer, "evseq_cap_gap"        , evseq_cap_gap        );

			writer.marshalDoubleCollection     ("adv_min_mag_bins"     , adv_min_mag_bins     );
			marshal_duration_list      (writer, "adv_window_start_offs", adv_window_start_offs);
			marshal_duration_list      (writer, "adv_window_end_offs"  , adv_window_end_offs  );
			writer.marshalStringCollection     ("adv_window_names"     , adv_window_names     );

			marshal_duration_list      (writer, "forecast_lags"        , forecast_lags        );
			marshal_duration_list      (writer, "comcat_retry_lags"    , comcat_retry_lags    );
			marshal_duration_list      (writer, "comcat_intake_lags"   , comcat_intake_lags   );
			marshal_duration_list      (writer, "pdl_report_retry_lags", pdl_report_retry_lags);
			marshal_intake_region_list (writer, "pdl_intake_regions"   , pdl_intake_regions   );

			break;

		case MARSHAL_VER_3:

			marshal_duration           (writer, "forecast_min_gap"     , forecast_min_gap     );
			marshal_duration           (writer, "forecast_max_delay"   , forecast_max_delay   );
			marshal_duration           (writer, "comcat_clock_skew"    , comcat_clock_skew    );
			marshal_duration           (writer, "comcat_origin_skew"   , comcat_origin_skew   );
			marshal_duration           (writer, "comcat_retry_min_gap" , comcat_retry_min_gap );
			marshal_duration           (writer, "comcat_retry_missing" , comcat_retry_missing );
			marshal_duration           (writer, "seq_spec_min_lag"     , seq_spec_min_lag     );
			marshal_duration           (writer, "advisory_dur_week"    , advisory_dur_week    );
			marshal_duration           (writer, "advisory_dur_month"   , advisory_dur_month   );
			marshal_duration           (writer, "advisory_dur_year"    , advisory_dur_year    );

			marshal_duration           (writer, "def_max_forecast_lag" , def_max_forecast_lag );
			marshal_duration           (writer, "withdraw_forecast_lag", withdraw_forecast_lag);
			writer.marshalInt          (        "stale_forecast_option", stale_forecast_option);
			writer.marshalDouble       (        "shadow_search_radius" , shadow_search_radius );
			marshal_duration           (writer, "shadow_lookback_time" , shadow_lookback_time );
			writer.marshalDouble       (        "shadow_centroid_mag"  , shadow_centroid_mag  );
			writer.marshalDouble       (        "shadow_large_mag"     , shadow_large_mag     );
			marshal_duration           (writer, "poll_short_period"    , poll_short_period    );
			marshal_duration           (writer, "poll_short_lookback"  , poll_short_lookback  );
			marshal_duration           (writer, "poll_short_intake_gap", poll_short_intake_gap);
			marshal_duration           (writer, "poll_long_period"     , poll_long_period     );
			marshal_duration           (writer, "poll_long_lookback"   , poll_long_lookback   );
			marshal_duration           (writer, "poll_long_intake_gap" , poll_long_intake_gap );
			marshal_duration           (writer, "pdl_intake_max_age"   , pdl_intake_max_age   );
			marshal_duration           (writer, "pdl_intake_max_future", pdl_intake_max_future);
			marshal_duration           (writer, "removal_forecast_age" , removal_forecast_age );
			marshal_duration           (writer, "removal_update_skew"  , removal_update_skew  );
			marshal_duration           (writer, "removal_lookback_tmax", removal_lookback_tmax);
			marshal_duration           (writer, "removal_lookback_tmin", removal_lookback_tmin);
			writer.marshalDouble       (        "removal_lookback_mag" , removal_lookback_mag );
			marshal_duration           (writer, "removal_check_period" , removal_check_period );
			marshal_duration           (writer, "removal_retry_period" , removal_retry_period );
			marshal_duration           (writer, "removal_event_gap"    , removal_event_gap    );
			marshal_duration           (writer, "removal_foreign_block", removal_foreign_block);
			writer.marshalString       (        "def_injectable_text"  , def_injectable_text  );

			writer.marshalInt          (        "evseq_enable"         , evseq_enable         );
			writer.marshalInt          (        "evseq_report"         , evseq_report         );
			marshal_duration           (writer, "evseq_lookback"       , evseq_lookback       );
			marshal_duration           (writer, "evseq_lookahead"      , evseq_lookahead      );
			marshal_duration           (writer, "evseq_cap_min_dur"    , evseq_cap_min_dur    );
			marshal_duration           (writer, "evseq_cap_gap"        , evseq_cap_gap        );

			writer.marshalInt          (        "etas_enable"          , etas_enable          );

			writer.marshalDoubleCollection     ("adv_min_mag_bins"     , adv_min_mag_bins     );
			marshal_duration_list      (writer, "adv_window_start_offs", adv_window_start_offs);
			marshal_duration_list      (writer, "adv_window_end_offs"  , adv_window_end_offs  );
			writer.marshalStringCollection     ("adv_window_names"     , adv_window_names     );

			writer.marshalDoubleCollection     ("adv_fractile_values"  , adv_fractile_values  );
			writer.marshalIntCollection        ("adv_bar_counts"       , adv_bar_counts       );

			marshal_duration_list      (writer, "forecast_lags"        , forecast_lags        );
			marshal_duration_list      (writer, "comcat_retry_lags"    , comcat_retry_lags    );
			marshal_duration_list      (writer, "comcat_intake_lags"   , comcat_intake_lags   );
			marshal_duration_list      (writer, "pdl_report_retry_lags", pdl_report_retry_lags);
			marshal_intake_region_list (writer, "pdl_intake_regions"   , pdl_intake_regions   );

			break;

		case MARSHAL_VER_4:

			marshal_duration           (writer, "forecast_min_gap"     , forecast_min_gap     );
			marshal_duration           (writer, "forecast_max_delay"   , forecast_max_delay   );
			marshal_duration           (writer, "comcat_clock_skew"    , comcat_clock_skew    );
			marshal_duration           (writer, "comcat_origin_skew"   , comcat_origin_skew   );
			marshal_duration           (writer, "comcat_retry_min_gap" , comcat_retry_min_gap );
			marshal_duration           (writer, "comcat_retry_missing" , comcat_retry_missing );
			marshal_duration           (writer, "seq_spec_min_lag"     , seq_spec_min_lag     );
			marshal_duration           (writer, "advisory_dur_week"    , advisory_dur_week    );
			marshal_duration           (writer, "advisory_dur_month"   , advisory_dur_month   );
			marshal_duration           (writer, "advisory_dur_year"    , advisory_dur_year    );

			marshal_duration           (writer, "def_max_forecast_lag" , def_max_forecast_lag );
			marshal_duration           (writer, "withdraw_forecast_lag", withdraw_forecast_lag);
			writer.marshalInt          (        "stale_forecast_option", stale_forecast_option);
			writer.marshalDouble       (        "shadow_search_radius" , shadow_search_radius );
			marshal_duration           (writer, "shadow_lookback_time" , shadow_lookback_time );
			writer.marshalDouble       (        "shadow_centroid_mag"  , shadow_centroid_mag  );
			writer.marshalDouble       (        "shadow_large_mag"     , shadow_large_mag     );
			marshal_duration           (writer, "poll_short_period"    , poll_short_period    );
			marshal_duration           (writer, "poll_short_lookback"  , poll_short_lookback  );
			marshal_duration           (writer, "poll_short_intake_gap", poll_short_intake_gap);
			marshal_duration           (writer, "poll_long_period"     , poll_long_period     );
			marshal_duration           (writer, "poll_long_lookback"   , poll_long_lookback   );
			marshal_duration           (writer, "poll_long_intake_gap" , poll_long_intake_gap );
			marshal_duration           (writer, "pdl_intake_max_age"   , pdl_intake_max_age   );
			marshal_duration           (writer, "pdl_intake_max_future", pdl_intake_max_future);
			marshal_duration           (writer, "removal_forecast_age" , removal_forecast_age );
			marshal_duration           (writer, "removal_update_skew"  , removal_update_skew  );
			marshal_duration           (writer, "removal_lookback_tmax", removal_lookback_tmax);
			marshal_duration           (writer, "removal_lookback_tmin", removal_lookback_tmin);
			writer.marshalDouble       (        "removal_lookback_mag" , removal_lookback_mag );
			marshal_duration           (writer, "removal_check_period" , removal_check_period );
			marshal_duration           (writer, "removal_retry_period" , removal_retry_period );
			marshal_duration           (writer, "removal_event_gap"    , removal_event_gap    );
			marshal_duration           (writer, "removal_foreign_block", removal_foreign_block);
			writer.marshalString       (        "def_injectable_text"  , def_injectable_text  );

			writer.marshalInt          (        "evseq_enable"         , evseq_enable         );
			writer.marshalInt          (        "evseq_report"         , evseq_report         );
			marshal_duration           (writer, "evseq_lookback"       , evseq_lookback       );
			marshal_duration           (writer, "evseq_lookahead"      , evseq_lookahead      );
			marshal_duration           (writer, "evseq_cap_min_dur"    , evseq_cap_min_dur    );
			marshal_duration           (writer, "evseq_cap_gap"        , evseq_cap_gap        );

			writer.marshalInt          (        "etas_enable"          , etas_enable          );

			marshal_duration           (writer, "etas_time_limit"      , etas_time_limit      );
			marshal_duration           (writer, "etas_progress_time"   , etas_progress_time   );
			marshal_duration           (writer, "data_fetch_lookback"  , data_fetch_lookback  );
			marshal_duration           (writer, "data_fit_dur_min"     , data_fit_dur_min     );
			writer.marshalDouble       (        "comcat_cache_1_mag"   , comcat_cache_1_mag   );
			marshal_duration           (writer, "comcat_cache_1_time"  , comcat_cache_1_time  );
			writer.marshalDouble       (        "comcat_cache_2_mag"   , comcat_cache_2_mag   );
			marshal_duration           (writer, "comcat_cache_2_time"  , comcat_cache_2_time  );

			writer.marshalDoubleCollection     ("adv_min_mag_bins"     , adv_min_mag_bins     );
			marshal_duration_list      (writer, "adv_window_start_offs", adv_window_start_offs);
			marshal_duration_list      (writer, "adv_window_end_offs"  , adv_window_end_offs  );
			writer.marshalStringCollection     ("adv_window_names"     , adv_window_names     );

			writer.marshalDoubleCollection     ("adv_fractile_values"  , adv_fractile_values  );
			writer.marshalIntCollection        ("adv_bar_counts"       , adv_bar_counts       );

			marshal_duration_list      (writer, "forecast_lags"        , forecast_lags        );
			marshal_duration_list      (writer, "comcat_retry_lags"    , comcat_retry_lags    );
			marshal_duration_list      (writer, "comcat_intake_lags"   , comcat_intake_lags   );
			marshal_duration_list      (writer, "pdl_report_retry_lags", pdl_report_retry_lags);
			marshal_intake_region_list (writer, "pdl_intake_regions"   , pdl_intake_regions   );

			break;
		}

		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_4);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			forecast_min_gap      = unmarshal_duration           (reader, "forecast_min_gap"     );
			forecast_max_delay    = unmarshal_duration           (reader, "forecast_max_delay"   );
			comcat_clock_skew     = unmarshal_duration           (reader, "comcat_clock_skew"    );
			comcat_origin_skew    = unmarshal_duration           (reader, "comcat_origin_skew"   );
			comcat_retry_min_gap  = unmarshal_duration           (reader, "comcat_retry_min_gap" );
			comcat_retry_missing  = unmarshal_duration           (reader, "comcat_retry_missing" );
			seq_spec_min_lag      = unmarshal_duration           (reader, "seq_spec_min_lag"     );
			advisory_dur_week     = unmarshal_duration           (reader, "advisory_dur_week"    );
			advisory_dur_month    = unmarshal_duration           (reader, "advisory_dur_month"   );
			advisory_dur_year     = unmarshal_duration           (reader, "advisory_dur_year"    );

			def_max_forecast_lag  = unmarshal_duration           (reader, "def_max_forecast_lag" );
			withdraw_forecast_lag = unmarshal_duration           (reader, "withdraw_forecast_lag");
			stale_forecast_option = reader.unmarshalInt          (        "stale_forecast_option");
			shadow_search_radius  = reader.unmarshalDouble       (        "shadow_search_radius" );
			shadow_lookback_time  = unmarshal_duration           (reader, "shadow_lookback_time" );
			shadow_centroid_mag   = reader.unmarshalDouble       (        "shadow_centroid_mag"  );
			shadow_large_mag      = reader.unmarshalDouble       (        "shadow_large_mag"     );
			poll_short_period     = unmarshal_duration           (reader, "poll_short_period"    );
			poll_short_lookback   = unmarshal_duration           (reader, "poll_short_lookback"  );
			poll_short_intake_gap = unmarshal_duration           (reader, "poll_short_intake_gap");
			poll_long_period      = unmarshal_duration           (reader, "poll_long_period"     );
			poll_long_lookback    = unmarshal_duration           (reader, "poll_long_lookback"   );
			poll_long_intake_gap  = unmarshal_duration           (reader, "poll_long_intake_gap" );
			pdl_intake_max_age    = unmarshal_duration           (reader, "pdl_intake_max_age"   );
			pdl_intake_max_future = unmarshal_duration           (reader, "pdl_intake_max_future");
			removal_forecast_age  = unmarshal_duration           (reader, "removal_forecast_age" );
			removal_update_skew   = unmarshal_duration           (reader, "removal_update_skew"  );
			removal_lookback_tmax = unmarshal_duration           (reader, "removal_lookback_tmax");
			removal_lookback_tmin = unmarshal_duration           (reader, "removal_lookback_tmin");
			removal_lookback_mag  = reader.unmarshalDouble       (        "removal_lookback_mag" );
			removal_check_period  = unmarshal_duration           (reader, "removal_check_period" );
			removal_retry_period  = unmarshal_duration           (reader, "removal_retry_period" );
			removal_event_gap     = unmarshal_duration           (reader, "removal_event_gap"    );
			removal_foreign_block = unmarshal_duration           (reader, "removal_foreign_block");
			def_injectable_text   = reader.unmarshalString       (        "def_injectable_text"  );

			evseq_enable          = V1_EVSEQ_ENABLE;
			evseq_report          = V1_EVSEQ_REPORT;
			evseq_lookback        = V1_EVSEQ_LOOKBACK;
			evseq_lookahead       = V1_EVSEQ_LOOKAHEAD;
			evseq_cap_min_dur     = V1_EVSEQ_CAP_MIN_DUR;
			evseq_cap_gap         = V1_EVSEQ_CAP_GAP;

			etas_enable           = V2_ETAS_ENABLE;

			etas_time_limit       = V3_ETAS_TIME_LIMIT;
			etas_progress_time    = V3_ETAS_PROGRESS_TIME;
			data_fetch_lookback   = V3_DATA_FETCH_LOOKBACK;
			data_fit_dur_min      = V3_DATA_FIT_DUR_MIN;
			comcat_cache_1_mag    = V3_COMCAT_CACHE_1_MAG;
			comcat_cache_1_time   = V3_COMCAT_CACHE_1_TIME;
			comcat_cache_2_mag    = V3_COMCAT_CACHE_2_MAG;
			comcat_cache_2_time   = V3_COMCAT_CACHE_2_TIME;

			adv_min_mag_bins = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_min_mag_bins"     , adv_min_mag_bins     );
			adv_window_start_offs = unmarshal_duration_list      (reader, "adv_window_start_offs");
			adv_window_end_offs   = unmarshal_duration_list      (reader, "adv_window_end_offs"  );
			adv_window_names = new ArrayList<String>();
			reader.unmarshalStringCollection                     (        "adv_window_names"     , adv_window_names     );

			adv_fractile_values = new ArrayList<Double>();
			def_adv_fractile_values (adv_fractile_values);
			adv_bar_counts = new ArrayList<Integer>();
			def_adv_bar_counts (adv_bar_counts);

			forecast_lags         = unmarshal_duration_list      (reader, "forecast_lags"        );
			comcat_retry_lags     = unmarshal_duration_list      (reader, "comcat_retry_lags"    );
			comcat_intake_lags    = unmarshal_duration_list      (reader, "comcat_intake_lags"   );
			pdl_report_retry_lags = unmarshal_duration_list      (reader, "pdl_report_retry_lags");
			pdl_intake_regions    = unmarshal_intake_region_list (reader, "pdl_intake_regions"   );

			break;

		case MARSHAL_VER_2:

			forecast_min_gap      = unmarshal_duration           (reader, "forecast_min_gap"     );
			forecast_max_delay    = unmarshal_duration           (reader, "forecast_max_delay"   );
			comcat_clock_skew     = unmarshal_duration           (reader, "comcat_clock_skew"    );
			comcat_origin_skew    = unmarshal_duration           (reader, "comcat_origin_skew"   );
			comcat_retry_min_gap  = unmarshal_duration           (reader, "comcat_retry_min_gap" );
			comcat_retry_missing  = unmarshal_duration           (reader, "comcat_retry_missing" );
			seq_spec_min_lag      = unmarshal_duration           (reader, "seq_spec_min_lag"     );
			advisory_dur_week     = unmarshal_duration           (reader, "advisory_dur_week"    );
			advisory_dur_month    = unmarshal_duration           (reader, "advisory_dur_month"   );
			advisory_dur_year     = unmarshal_duration           (reader, "advisory_dur_year"    );

			def_max_forecast_lag  = unmarshal_duration           (reader, "def_max_forecast_lag" );
			withdraw_forecast_lag = unmarshal_duration           (reader, "withdraw_forecast_lag");
			stale_forecast_option = reader.unmarshalInt          (        "stale_forecast_option");
			shadow_search_radius  = reader.unmarshalDouble       (        "shadow_search_radius" );
			shadow_lookback_time  = unmarshal_duration           (reader, "shadow_lookback_time" );
			shadow_centroid_mag   = reader.unmarshalDouble       (        "shadow_centroid_mag"  );
			shadow_large_mag      = reader.unmarshalDouble       (        "shadow_large_mag"     );
			poll_short_period     = unmarshal_duration           (reader, "poll_short_period"    );
			poll_short_lookback   = unmarshal_duration           (reader, "poll_short_lookback"  );
			poll_short_intake_gap = unmarshal_duration           (reader, "poll_short_intake_gap");
			poll_long_period      = unmarshal_duration           (reader, "poll_long_period"     );
			poll_long_lookback    = unmarshal_duration           (reader, "poll_long_lookback"   );
			poll_long_intake_gap  = unmarshal_duration           (reader, "poll_long_intake_gap" );
			pdl_intake_max_age    = unmarshal_duration           (reader, "pdl_intake_max_age"   );
			pdl_intake_max_future = unmarshal_duration           (reader, "pdl_intake_max_future");
			removal_forecast_age  = unmarshal_duration           (reader, "removal_forecast_age" );
			removal_update_skew   = unmarshal_duration           (reader, "removal_update_skew"  );
			removal_lookback_tmax = unmarshal_duration           (reader, "removal_lookback_tmax");
			removal_lookback_tmin = unmarshal_duration           (reader, "removal_lookback_tmin");
			removal_lookback_mag  = reader.unmarshalDouble       (        "removal_lookback_mag" );
			removal_check_period  = unmarshal_duration           (reader, "removal_check_period" );
			removal_retry_period  = unmarshal_duration           (reader, "removal_retry_period" );
			removal_event_gap     = unmarshal_duration           (reader, "removal_event_gap"    );
			removal_foreign_block = unmarshal_duration           (reader, "removal_foreign_block");
			def_injectable_text   = reader.unmarshalString       (        "def_injectable_text"  );

			evseq_enable          = reader.unmarshalInt          (        "evseq_enable"         );
			evseq_report          = reader.unmarshalInt          (        "evseq_report"         );
			evseq_lookback        = unmarshal_duration           (reader, "evseq_lookback"       );
			evseq_lookahead       = unmarshal_duration           (reader, "evseq_lookahead"      );
			evseq_cap_min_dur     = unmarshal_duration           (reader, "evseq_cap_min_dur"    );
			evseq_cap_gap         = unmarshal_duration           (reader, "evseq_cap_gap"        );

			etas_enable           = V2_ETAS_ENABLE;

			etas_time_limit       = V3_ETAS_TIME_LIMIT;
			etas_progress_time    = V3_ETAS_PROGRESS_TIME;
			data_fetch_lookback   = V3_DATA_FETCH_LOOKBACK;
			data_fit_dur_min      = V3_DATA_FIT_DUR_MIN;
			comcat_cache_1_mag    = V3_COMCAT_CACHE_1_MAG;
			comcat_cache_1_time   = V3_COMCAT_CACHE_1_TIME;
			comcat_cache_2_mag    = V3_COMCAT_CACHE_2_MAG;
			comcat_cache_2_time   = V3_COMCAT_CACHE_2_TIME;

			adv_min_mag_bins = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_min_mag_bins"     , adv_min_mag_bins     );
			adv_window_start_offs = unmarshal_duration_list      (reader, "adv_window_start_offs");
			adv_window_end_offs   = unmarshal_duration_list      (reader, "adv_window_end_offs"  );
			adv_window_names = new ArrayList<String>();
			reader.unmarshalStringCollection                     (        "adv_window_names"     , adv_window_names     );

			adv_fractile_values = new ArrayList<Double>();
			def_adv_fractile_values (adv_fractile_values);
			adv_bar_counts = new ArrayList<Integer>();
			def_adv_bar_counts (adv_bar_counts);

			forecast_lags         = unmarshal_duration_list      (reader, "forecast_lags"        );
			comcat_retry_lags     = unmarshal_duration_list      (reader, "comcat_retry_lags"    );
			comcat_intake_lags    = unmarshal_duration_list      (reader, "comcat_intake_lags"   );
			pdl_report_retry_lags = unmarshal_duration_list      (reader, "pdl_report_retry_lags");
			pdl_intake_regions    = unmarshal_intake_region_list (reader, "pdl_intake_regions"   );

			break;

		case MARSHAL_VER_3:

			forecast_min_gap      = unmarshal_duration           (reader, "forecast_min_gap"     );
			forecast_max_delay    = unmarshal_duration           (reader, "forecast_max_delay"   );
			comcat_clock_skew     = unmarshal_duration           (reader, "comcat_clock_skew"    );
			comcat_origin_skew    = unmarshal_duration           (reader, "comcat_origin_skew"   );
			comcat_retry_min_gap  = unmarshal_duration           (reader, "comcat_retry_min_gap" );
			comcat_retry_missing  = unmarshal_duration           (reader, "comcat_retry_missing" );
			seq_spec_min_lag      = unmarshal_duration           (reader, "seq_spec_min_lag"     );
			advisory_dur_week     = unmarshal_duration           (reader, "advisory_dur_week"    );
			advisory_dur_month    = unmarshal_duration           (reader, "advisory_dur_month"   );
			advisory_dur_year     = unmarshal_duration           (reader, "advisory_dur_year"    );

			def_max_forecast_lag  = unmarshal_duration           (reader, "def_max_forecast_lag" );
			withdraw_forecast_lag = unmarshal_duration           (reader, "withdraw_forecast_lag");
			stale_forecast_option = reader.unmarshalInt          (        "stale_forecast_option");
			shadow_search_radius  = reader.unmarshalDouble       (        "shadow_search_radius" );
			shadow_lookback_time  = unmarshal_duration           (reader, "shadow_lookback_time" );
			shadow_centroid_mag   = reader.unmarshalDouble       (        "shadow_centroid_mag"  );
			shadow_large_mag      = reader.unmarshalDouble       (        "shadow_large_mag"     );
			poll_short_period     = unmarshal_duration           (reader, "poll_short_period"    );
			poll_short_lookback   = unmarshal_duration           (reader, "poll_short_lookback"  );
			poll_short_intake_gap = unmarshal_duration           (reader, "poll_short_intake_gap");
			poll_long_period      = unmarshal_duration           (reader, "poll_long_period"     );
			poll_long_lookback    = unmarshal_duration           (reader, "poll_long_lookback"   );
			poll_long_intake_gap  = unmarshal_duration           (reader, "poll_long_intake_gap" );
			pdl_intake_max_age    = unmarshal_duration           (reader, "pdl_intake_max_age"   );
			pdl_intake_max_future = unmarshal_duration           (reader, "pdl_intake_max_future");
			removal_forecast_age  = unmarshal_duration           (reader, "removal_forecast_age" );
			removal_update_skew   = unmarshal_duration           (reader, "removal_update_skew"  );
			removal_lookback_tmax = unmarshal_duration           (reader, "removal_lookback_tmax");
			removal_lookback_tmin = unmarshal_duration           (reader, "removal_lookback_tmin");
			removal_lookback_mag  = reader.unmarshalDouble       (        "removal_lookback_mag" );
			removal_check_period  = unmarshal_duration           (reader, "removal_check_period" );
			removal_retry_period  = unmarshal_duration           (reader, "removal_retry_period" );
			removal_event_gap     = unmarshal_duration           (reader, "removal_event_gap"    );
			removal_foreign_block = unmarshal_duration           (reader, "removal_foreign_block");
			def_injectable_text   = reader.unmarshalString       (        "def_injectable_text"  );

			evseq_enable          = reader.unmarshalInt          (        "evseq_enable"         );
			evseq_report          = reader.unmarshalInt          (        "evseq_report"         );
			evseq_lookback        = unmarshal_duration           (reader, "evseq_lookback"       );
			evseq_lookahead       = unmarshal_duration           (reader, "evseq_lookahead"      );
			evseq_cap_min_dur     = unmarshal_duration           (reader, "evseq_cap_min_dur"    );
			evseq_cap_gap         = unmarshal_duration           (reader, "evseq_cap_gap"        );

			etas_enable           = reader.unmarshalInt          (        "etas_enable"          );

			etas_time_limit       = V3_ETAS_TIME_LIMIT;
			etas_progress_time    = V3_ETAS_PROGRESS_TIME;
			data_fetch_lookback   = V3_DATA_FETCH_LOOKBACK;
			data_fit_dur_min      = V3_DATA_FIT_DUR_MIN;
			comcat_cache_1_mag    = V3_COMCAT_CACHE_1_MAG;
			comcat_cache_1_time   = V3_COMCAT_CACHE_1_TIME;
			comcat_cache_2_mag    = V3_COMCAT_CACHE_2_MAG;
			comcat_cache_2_time   = V3_COMCAT_CACHE_2_TIME;

			adv_min_mag_bins = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_min_mag_bins"     , adv_min_mag_bins     );
			adv_window_start_offs = unmarshal_duration_list      (reader, "adv_window_start_offs");
			adv_window_end_offs   = unmarshal_duration_list      (reader, "adv_window_end_offs"  );
			adv_window_names = new ArrayList<String>();
			reader.unmarshalStringCollection                     (        "adv_window_names"     , adv_window_names     );

			adv_fractile_values = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_fractile_values"  , adv_fractile_values  );
			adv_bar_counts = new ArrayList<Integer>();
			reader.unmarshalIntCollection                        (        "adv_bar_counts"       , adv_bar_counts       );

			forecast_lags         = unmarshal_duration_list      (reader, "forecast_lags"        );
			comcat_retry_lags     = unmarshal_duration_list      (reader, "comcat_retry_lags"    );
			comcat_intake_lags    = unmarshal_duration_list      (reader, "comcat_intake_lags"   );
			pdl_report_retry_lags = unmarshal_duration_list      (reader, "pdl_report_retry_lags");
			pdl_intake_regions    = unmarshal_intake_region_list (reader, "pdl_intake_regions"   );

			break;

		case MARSHAL_VER_4:

			forecast_min_gap      = unmarshal_duration           (reader, "forecast_min_gap"     );
			forecast_max_delay    = unmarshal_duration           (reader, "forecast_max_delay"   );
			comcat_clock_skew     = unmarshal_duration           (reader, "comcat_clock_skew"    );
			comcat_origin_skew    = unmarshal_duration           (reader, "comcat_origin_skew"   );
			comcat_retry_min_gap  = unmarshal_duration           (reader, "comcat_retry_min_gap" );
			comcat_retry_missing  = unmarshal_duration           (reader, "comcat_retry_missing" );
			seq_spec_min_lag      = unmarshal_duration           (reader, "seq_spec_min_lag"     );
			advisory_dur_week     = unmarshal_duration           (reader, "advisory_dur_week"    );
			advisory_dur_month    = unmarshal_duration           (reader, "advisory_dur_month"   );
			advisory_dur_year     = unmarshal_duration           (reader, "advisory_dur_year"    );

			def_max_forecast_lag  = unmarshal_duration           (reader, "def_max_forecast_lag" );
			withdraw_forecast_lag = unmarshal_duration           (reader, "withdraw_forecast_lag");
			stale_forecast_option = reader.unmarshalInt          (        "stale_forecast_option");
			shadow_search_radius  = reader.unmarshalDouble       (        "shadow_search_radius" );
			shadow_lookback_time  = unmarshal_duration           (reader, "shadow_lookback_time" );
			shadow_centroid_mag   = reader.unmarshalDouble       (        "shadow_centroid_mag"  );
			shadow_large_mag      = reader.unmarshalDouble       (        "shadow_large_mag"     );
			poll_short_period     = unmarshal_duration           (reader, "poll_short_period"    );
			poll_short_lookback   = unmarshal_duration           (reader, "poll_short_lookback"  );
			poll_short_intake_gap = unmarshal_duration           (reader, "poll_short_intake_gap");
			poll_long_period      = unmarshal_duration           (reader, "poll_long_period"     );
			poll_long_lookback    = unmarshal_duration           (reader, "poll_long_lookback"   );
			poll_long_intake_gap  = unmarshal_duration           (reader, "poll_long_intake_gap" );
			pdl_intake_max_age    = unmarshal_duration           (reader, "pdl_intake_max_age"   );
			pdl_intake_max_future = unmarshal_duration           (reader, "pdl_intake_max_future");
			removal_forecast_age  = unmarshal_duration           (reader, "removal_forecast_age" );
			removal_update_skew   = unmarshal_duration           (reader, "removal_update_skew"  );
			removal_lookback_tmax = unmarshal_duration           (reader, "removal_lookback_tmax");
			removal_lookback_tmin = unmarshal_duration           (reader, "removal_lookback_tmin");
			removal_lookback_mag  = reader.unmarshalDouble       (        "removal_lookback_mag" );
			removal_check_period  = unmarshal_duration           (reader, "removal_check_period" );
			removal_retry_period  = unmarshal_duration           (reader, "removal_retry_period" );
			removal_event_gap     = unmarshal_duration           (reader, "removal_event_gap"    );
			removal_foreign_block = unmarshal_duration           (reader, "removal_foreign_block");
			def_injectable_text   = reader.unmarshalString       (        "def_injectable_text"  );

			evseq_enable          = reader.unmarshalInt          (        "evseq_enable"         );
			evseq_report          = reader.unmarshalInt          (        "evseq_report"         );
			evseq_lookback        = unmarshal_duration           (reader, "evseq_lookback"       );
			evseq_lookahead       = unmarshal_duration           (reader, "evseq_lookahead"      );
			evseq_cap_min_dur     = unmarshal_duration           (reader, "evseq_cap_min_dur"    );
			evseq_cap_gap         = unmarshal_duration           (reader, "evseq_cap_gap"        );

			etas_enable           = reader.unmarshalInt          (        "etas_enable"          );

			etas_time_limit       = unmarshal_duration           (reader, "etas_time_limit"      );
			etas_progress_time    = unmarshal_duration           (reader, "etas_progress_time"   );
			data_fetch_lookback   = unmarshal_duration           (reader, "data_fetch_lookback"  );
			data_fit_dur_min      = unmarshal_duration           (reader, "data_fit_dur_min"     );
			comcat_cache_1_mag    = reader.unmarshalDouble       (        "comcat_cache_1_mag"   );
			comcat_cache_1_time   = unmarshal_duration           (reader, "comcat_cache_1_time"  );
			comcat_cache_2_mag    = reader.unmarshalDouble       (        "comcat_cache_2_mag"   );
			comcat_cache_2_time   = unmarshal_duration           (reader, "comcat_cache_2_time"  );

			adv_min_mag_bins = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_min_mag_bins"     , adv_min_mag_bins     );
			adv_window_start_offs = unmarshal_duration_list      (reader, "adv_window_start_offs");
			adv_window_end_offs   = unmarshal_duration_list      (reader, "adv_window_end_offs"  );
			adv_window_names = new ArrayList<String>();
			reader.unmarshalStringCollection                     (        "adv_window_names"     , adv_window_names     );

			adv_fractile_values = new ArrayList<Double>();
			reader.unmarshalDoubleCollection                     (        "adv_fractile_values"  , adv_fractile_values  );
			adv_bar_counts = new ArrayList<Integer>();
			reader.unmarshalIntCollection                        (        "adv_bar_counts"       , adv_bar_counts       );

			forecast_lags         = unmarshal_duration_list      (reader, "forecast_lags"        );
			comcat_retry_lags     = unmarshal_duration_list      (reader, "comcat_retry_lags"    );
			comcat_intake_lags    = unmarshal_duration_list      (reader, "comcat_intake_lags"   );
			pdl_report_retry_lags = unmarshal_duration_list      (reader, "pdl_report_retry_lags");
			pdl_intake_regions    = unmarshal_intake_region_list (reader, "pdl_intake_regions"   );

			break;
		}

		// Error check

		check_invariant();

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
	public ActionConfigFile unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, ActionConfigFile obj) {

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

	public static ActionConfigFile unmarshal_poly (MarshalReader reader, String name) {
		ActionConfigFile result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("ActionConfigFile.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_ACTION_CFG:
			result = new ActionConfigFile();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Unmarshal object from a configuration file.
	// Parameters:
	//  filename = Name of file (not including a path).
	//  requester = Class that is requesting the file.

	public static ActionConfigFile unmarshal_config (String filename, Class<?> requester) {
		ActionConfigFile x = new ActionConfigFile();
		OAFParameterSet.unmarshal_file_as_json (x, filename, requester);
		return x;
	}

	//public static ActionConfigFile unmarshal_config (String filename, Class<?> requester) {
	//	MarshalReader reader = OAFParameterSet.load_file_as_json (filename,requester);
	//	return (new ActionConfigFile()).unmarshal (reader, null);
	//}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ActionConfigFile : Missing subcommand");
			return;
		}

		// Subcommand : Test #1
		// Command format:
		//  test1
		// Unmarshal from the configuration file, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ActionConfigFile : Invalid 'test1' subcommand");
				return;
			}

			// Read the configuration file

			ActionConfigFile action_cfg = unmarshal_config ("ActionConfig.json", ActionConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			return;
		}

		// Subcommand : Test #2
		// Command format:
		//  test2
		// Unmarshal from the configuration file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ActionConfigFile : Invalid 'test2' subcommand");
				return;
			}

			// Read the configuration file

			ActionConfigFile action_cfg = unmarshal_config ("ActionConfig.json", ActionConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			// Marshal to JSON

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			ActionConfigFile.marshal_poly (store, null, action_cfg);
			store.check_write_complete ();
			String json_string = store.get_json_string();

			System.out.println ("");
			System.out.println (MarshalUtils.display_valid_json_string (json_string));

			// Unmarshal from JSON
			
			action_cfg = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			action_cfg = ActionConfigFile.unmarshal_poly (retrieve, null);
			retrieve.check_read_complete ();

			System.out.println ("");
			System.out.println (action_cfg.toString());

			return;
		}

		// Subcommand : Test #3
		// Command format:
		//  test3
		// Unmarshal from the configuration file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// This differs from test #2 only in that it uses the non-static marshal methods.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ActionConfigFile : Invalid 'test3' subcommand");
				return;
			}

			// Read the configuration file

			ActionConfigFile action_cfg = unmarshal_config ("ActionConfig.json", ActionConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			// Marshal to JSON

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			action_cfg.marshal (store, null);
			store.check_write_complete ();
			String json_string = store.get_json_string();

			System.out.println ("");
			System.out.println (MarshalUtils.display_valid_json_string (json_string));

			// Unmarshal from JSON
			
			action_cfg = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			action_cfg = (new ActionConfigFile()).unmarshal (retrieve, null);
			retrieve.check_read_complete ();

			System.out.println ("");
			System.out.println (action_cfg.toString());

			return;
		}

		// Subcommand : Test #4
		// Command format:
		//  test4  filename
		// Unmarshal from the specified file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.

		if (args[0].equalsIgnoreCase ("test4")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ActionConfigFile : Invalid 'test4' subcommand");
				return;
			}

			String filename = args[1];

			// Read the configuration file

			ActionConfigFile action_cfg = new ActionConfigFile();
			MarshalUtils.from_json_file (action_cfg, filename);

			// Display it

			System.out.println (action_cfg.toString());

			// Marshal to JSON

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			ActionConfigFile.marshal_poly (store, null, action_cfg);
			store.check_write_complete ();
			String json_string = store.get_json_string();

			System.out.println ("");
			System.out.println (MarshalUtils.display_valid_json_string (json_string));

			// Unmarshal from JSON
			
			action_cfg = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			action_cfg = ActionConfigFile.unmarshal_poly (retrieve, null);
			retrieve.check_read_complete ();

			System.out.println ("");
			System.out.println (action_cfg.toString());

			return;
		}

		// Unrecognized subcommand.

		System.err.println ("ActionConfigFile : Unrecognized subcommand : " + args[0]);
		return;

	}

}
