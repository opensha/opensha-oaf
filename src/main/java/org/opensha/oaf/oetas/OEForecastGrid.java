package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import java.time.Instant;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.oetas.util.OEArraysCalc;

import org.opensha.oaf.aafs.ActionConfig;

import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_ForecastModel;
import org.opensha.oaf.rj.USGS_ForecastException;

import org.opensha.oaf.comcat.GeoJsonUtils;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


// Class to build the time and magnitude grid for accumulating an Operational ETAS catalog.
// Author: Michael Barall 03/09/2022.

public class OEForecastGrid implements USGS_ForecastModel {

	//--- Advisory settings ---

	// The advisory times, in days (relative values, with zero origin time).
	// This is sorted in increasing order.

	protected double[] adv_time_values;

	// The advisory times, in milliseconds (relative values, with zero origin time).
	// This is sorted in increasing order.
	// Note: All entries in this array are multiples of TIME_DELTA_MILLIS.

	protected long[] adv_time_values_millis;

	// The advisory magnitudes.
	// This is sorted in increasing order.

	protected double[] adv_mag_values;

	// The advisory fractiles.
	// This is sorted in increasing order.

	protected double[] adv_fractile_values;

	// The advisory exceedence counts.
	// This is sorted in increasing order.
	// Note: At present, only 0 is supported.

	protected int[] adv_xcount_values;

	// Epsilon used for matching magnitude values.
	// Note this must be smaller than 1/3 the spacing between rounded magnitudes (see round_adv_mag()).

	protected static final double MATCH_MAG_EPS = 0.0001;

	// Epsilon used for matching time values, in days.
	// Note this must be smaller than 1/3 the spacing between acceptable times (see convert_adv_time() and TIME_DELTA_MILLIS).

	protected static final double MATCH_TIME_EPS = 0.00035;	// 30.24 seconds

	// Epsilon used for matching fractile values.
	// Note this must be smaller than 1/3 the spacing between acceptable fractiles (see round_adv_fractile()).

	protected static final double MATCH_FRACTILE_EPS = 0.00001;

	// Spacing between acceptable times, in milliseconds.

	protected static final long TIME_DELTA_MILLIS = 300000L;	// 5 minutes = 300 seconds

	//--- Mainshock settings ---

	// True if we have a mainshock magnitude.

	protected boolean has_mainshock_mag;

	// Mainshock magnitude, or 0.0 if we don't have one.
	// Note: This is rounded (see round_mainshock_mag()).

	protected double rounded_mainshock_mag;

	// The advisory magnitudes, including the rounded mainshock magnitude.
	// If the rounded mainshock magnitude coincides with one of the advisory
	// magnitudes, then this is the same as adv_mag_values.  Otherwise, it
	// contains one extra element, which is the rounded mainshock magnitude.
	// This is sorted in increasing order.

	protected double[] adv_merged_mag_values;

	// The index into adv_merged_mag_values where the mainshock magnitude is located, or -1 if none.

	protected int adv_mainshock_index;

	// The default size of the last magnitude bin.

	public static final double DEF_MAG_HEADROOM = 3.0;

	// The default size of a time bin used for catalog ranging, in days.

	public static final double DEF_TIME_RANGING_DELTA = 1.0;

	//--- Model information ---

	// The model name, to be inserted into the forecast, or empty string if none.

	protected String model_name;

	// The model parameters, to be inserted into the forecast.

	protected LinkedHashMap<String, Object> model_params;

	// Model names.

	public static final String MNAME_UNSPECIFIED = "ETAS (Ogata, 1988) aftershock model";
	public static final String MNAME_GENERIC = "ETAS (Ogata, 1988) aftershock model (Generic)";
	public static final String MNAME_SEQ_SPEC = "ETAS (Ogata, 1988) aftershock model (Sequence Specific)";
	public static final String MNAME_BAYESIAN = "ETAS (Ogata, 1988) aftershock model (Bayesian Combination)";

	//--- Model results ---

	// True if we have results.

	protected boolean has_results;

	// The array of fractiles for each bin.
	// Dimension: fractile_array[adv_fractile_values.length][adv_time_values.length][adv_merged_mag_values.length]
	// Each element corresponds to one fractile and one bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of N or fewer
	// aftershocks is approximately equal to the fractile value.

	protected int[][][] fractile_array;

	// The array of probabilities of occurrence for each bin.
	// Dimension: prob_occur_array[adv_xcount_values.length][adv_time_values.length][adv_merged_mag_values.length]
	// Each element corresponds to one exceedence count and one bin (which is cumulative by definition).
	// Each value is a real number v, such that v is the probability that
	// the number of aftershocks is greater than the exceedence count.

	protected double[][][] prob_occur_array;

	// True if we have catalog size information.

	protected boolean has_cat_size;

	// The catalog size fractiles.
	// This is sorted in increasing order.

	protected double[] cat_size_fractile_values;

	// The array of catalog sizes for each bin.
	// Dimension: cat_size_array[cat_size_fractile_values.length][adv_time_values.length]
	// Each element corresponds to one fractile and one time bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of a catalog containing
	// N or fewer ruptures is approximately equal to fractile.

	protected int[][] cat_size_array;




	//----- Construction -----




	// Round an advisory magnitude.
	// We require advisory magnitudes to be multiples of 0.001 magnitude,
	// matching the precision in CompactEqkRupList.
	// Throw an exception if magnitude is not acceptable.
	// Note: The spacing between acceptable magnitudes must be at least 4 times OEConstants.GEN_MAG_EPS.

	private static double round_adv_mag (double mag) {
		double x = SimpleUtils.round_double_via_string ("%.3f", mag);
		if (Math.abs(x - mag) > MATCH_MAG_EPS * 0.1) {
			throw new IllegalArgumentException ("OEForecastGrid.round_adv_mag: Magnitude is not a multiple of 0.001: mag = " + mag);
		}
		return x;
	}

	// Check invariant for round_adv_mag.

	private static void check_inv_round_adv_mag () {
		if (!( 0.001 >= 4.0 * OEConstants.GEN_MAG_EPS )) {
			throw new InvariantViolationException ("OEForecastGrid.check_inv_round_adv_mag: Magnitude spacing is too small: spacing = 0.001, GEN_MAG_EPS = " + OEConstants.GEN_MAG_EPS);
		}
		return;
	}




	// Round a mainshock magnitude.
	// This is also used to round the magnitude headroom.
	// We round to 2 decimal places.
	// Note: Any possible return value must also be a possible return value of round_adv_mag.
	// So for example, if round_adv_mag rounds to 3 decimal places, then this function could
	// round to 1, 2, or 3 decimal places, but not 4 decimal places.

	private static double round_mainshock_mag (double mag) {
		return SimpleUtils.round_double_via_string ("%.2f", mag);
	}




	// Convert an advisory time in milliseconds to a time in days.
	// We require advisory times to be multiples of 5 minutes (see TIME_DELTA_MILLIS).
	// Throw an exception if time is not acceptable.
	// Note: The spacing between acceptable times must be at least 4 times OEConstants.GEN_TIME_EPS.

	private static double convert_adv_time (long millis) {
		if (millis % TIME_DELTA_MILLIS != 0L) {
			throw new IllegalArgumentException ("OEForecastGrid.convert_adv_time: Time is not a multiple of 5 minutes: millis = " + millis);
		}
		double x = ((double)millis) / SimpleUtils.DAY_MILLIS_D;
		return x;
	}

	// Check invariant for convert_adv_time.

	private static void check_inv_convert_adv_time () {
		double time_spacing = ((double)TIME_DELTA_MILLIS) / SimpleUtils.DAY_MILLIS_D;
		if (!( time_spacing >= 4.0 * OEConstants.GEN_TIME_EPS )) {
			throw new InvariantViolationException ("OEForecastGrid.check_inv_convert_adv_time: Time spacing is too small: spacing = " + time_spacing + ", GEN_TIME_EPS = " + OEConstants.GEN_TIME_EPS);
		}
		return;
	}




	// Round an advisory fractile.
	// We require advisory fractiles to be multiples of 0.0001.
	// Throw an exception if fractile is not acceptable.

	private static double round_adv_fractile (double frac) {
		double x = SimpleUtils.round_double_via_string ("%.4f", frac);
		if (Math.abs(x - frac) > MATCH_FRACTILE_EPS * 0.1) {
			throw new IllegalArgumentException ("OEForecastGrid.round_adv_fractile: Fractile is not a multiple of 0.0001: frac = " + frac);
		}
		return x;
	}




	// Set up advisory settings.
	// Also set up for no mainshock.
	// Throw an exception if advisory settings cannot be set up.

	public void setup_advisory () {

		// Get action configuration

		ActionConfig action_config = new ActionConfig();

		// Get the advisory time values

		adv_time_values = new double[action_config.get_adv_window_count()];
		adv_time_values_millis = new long[adv_time_values.length];
		for (int time_ix = 0; time_ix < adv_time_values.length; ++time_ix) {
			adv_time_values_millis[time_ix] = action_config.get_adv_window_end_off (time_ix);
		}
		Arrays.sort (adv_time_values_millis);		// should already be sorted, but just in case
		for (int time_ix = 0; time_ix < adv_time_values.length; ++time_ix) {
			adv_time_values[time_ix] = convert_adv_time (adv_time_values_millis[time_ix]);
		}

		// Get the advisory magnitude values

		adv_mag_values = new double[action_config.get_adv_min_mag_bin_count()];
		for (int mag_ix = 0; mag_ix < adv_mag_values.length; ++mag_ix) {
			adv_mag_values[mag_ix] = round_adv_mag (action_config.get_adv_min_mag_bin (mag_ix));
		}
		Arrays.sort (adv_mag_values);		// should already be sorted, but just in case

		// Get the advisory fractile values
		// Note: There are currenly no advisory fractiles in ActionConfig, so we use multiples of 1.25%.

		adv_fractile_values = new double[79];
		for (int frac_ix = 0; frac_ix < adv_fractile_values.length; ++frac_ix) {
			adv_fractile_values[frac_ix] = round_adv_fractile (((double)(frac_ix + 1)) / 80.0);
		}

		// Get the advisory exceedence count values
		// Note: At present, only 0 is supported,

		adv_xcount_values = new int[1];
		for (int xcnt_ix = 0; xcnt_ix < adv_xcount_values.length; ++xcnt_ix) {
			adv_xcount_values[xcnt_ix] = xcnt_ix;
		}

		// Set up for no mainshock

		has_mainshock_mag = false;
		rounded_mainshock_mag = 0.0;
		adv_merged_mag_values = Arrays.copyOf (adv_mag_values, adv_mag_values.length);
		adv_mainshock_index = -1;

		// No model information

		model_name = MNAME_UNSPECIFIED;
		model_params = new LinkedHashMap<String, Object>();

		// No model results

		has_results = false;
		fractile_array = null;
		prob_occur_array = null;
		has_cat_size = false;
		cat_size_fractile_values = null;
		cat_size_array = null;

		return;
	}




	// Set up mainshock settings.
	// Assumes that do_setup_advisory() has already been called.

	public void setup_mainshock (double raw_mainshock_mag) {

		// Check that advisory settings are set up

		if (!( adv_time_values != null
			&& adv_time_values_millis != null
			&& adv_mag_values != null
			&& adv_fractile_values != null
			&& adv_xcount_values != null
		)) {
			throw new IllegalStateException ("OEForecastGrid.setup_mainshock: Advisory settings are not set up");
		}

		// Check that mainshock is not already set up

		if (has_mainshock_mag) {
			throw new IllegalStateException ("OEForecastGrid.setup_mainshock: Mainshock is already set up");
		}

		// Save mainshock magnitude, rounded

		has_mainshock_mag = true;
		rounded_mainshock_mag = round_mainshock_mag (raw_mainshock_mag);

		// Search for mainshock mag in advisory magnitudes

		adv_mainshock_index = OEArraysCalc.bfind_array (adv_mag_values, rounded_mainshock_mag, MATCH_MAG_EPS);

		// If not found ...

		if (adv_mainshock_index < 0) {

			// Append rounded mainshock mag to the list of mags

			adv_merged_mag_values = Arrays.copyOf (adv_mag_values, adv_mag_values.length + 1);
			adv_merged_mag_values[adv_mag_values.length] = rounded_mainshock_mag;

			// Move mainshock mag into position in the list

			Arrays.sort (adv_merged_mag_values);

			// Get the resulting index of the mainshock mag

			adv_mainshock_index = OEArraysCalc.bfind_array (adv_merged_mag_values, rounded_mainshock_mag, MATCH_MAG_EPS);
			if (adv_mainshock_index < 0) {
				throw new IllegalStateException ("OEForecastGrid.setup_mainshock: Unable to add mainshock magnitude to list");
			}
		}

		// If found ...

		else {

			// Use just the advisory magnitudes

			adv_merged_mag_values = Arrays.copyOf (adv_mag_values, adv_mag_values.length);
		}

		// Add mainshock magnitude parameter

		model_params.put (OEConstants.MKEY_MAG_MAIN, rounded_mainshock_mag);
		return;
	}




	// Clear to default values.

	public final void clear () {

		// Static invariant checks

		check_inv_round_adv_mag();
		check_inv_convert_adv_time();

		// Advisory settings

		adv_time_values = null;
		adv_time_values_millis = null;
		adv_mag_values = null;
		adv_fractile_values = null;
		adv_xcount_values = null;

		// Mainshock settings

		has_mainshock_mag = false;
		rounded_mainshock_mag = 0.0;
		adv_merged_mag_values = null;
		adv_mainshock_index = -1;

		// Model information

		model_name = null;
		model_params = null;

		// Model results

		has_results = false;
		fractile_array = null;
		prob_occur_array = null;
		has_cat_size = false;
		cat_size_fractile_values = null;
		cat_size_array = null;

		return;
	}




	// Default constructor.

	public OEForecastGrid () {
		clear();
	}




	// Set the name of the model.

	public final void set_model_name (String name) {
		model_name = name;
		return;
	}




	// Add a model parameter.
	// The value should be one of: Integer, Long, Double, Float, Boolean, or String.

	public final void add_model_param (String key, Object value) {
		model_params.put (key, value);
		return;
	}




	// Add all the model parameters in the supplied map.
	// Each value should be one of: Integer, Long, Double, Float, Boolean, or String.

	public final void add_model_param (Map<String, Object> params) {
		model_params.putAll (params);
		return;
	}




	// Supply results.
	// Parameters:
	//  accum = Accumulator able to supply results for a time/magnitude grid.

	public final void supply_results (OEAccumReadoutTimeMag accum) {

		// Check that advisory settings are set up

		if (!( adv_time_values != null
			&& adv_time_values_millis != null
			&& adv_mag_values != null
			&& adv_fractile_values != null
			&& adv_xcount_values != null
		)) {
			throw new IllegalStateException ("OEForecastGrid.supply_results: Advisory settings are not set up");
		}

		// Get fractiles

		fractile_array = new int[adv_fractile_values.length][][];
		for (int frac_ix = 0; frac_ix < adv_fractile_values.length; ++frac_ix) {
			fractile_array[frac_ix] = accum.get_fractile_array (adv_fractile_values[frac_ix]);
		}

		// Get probabilities

		prob_occur_array = new double[adv_xcount_values.length][][];
		for (int xcnt_ix = 0; xcnt_ix < adv_xcount_values.length; ++xcnt_ix) {
			prob_occur_array[xcnt_ix] = accum.get_prob_occur_array (adv_xcount_values[xcnt_ix]);
		}

		// We have results

		has_results = true;

		// If the accumulator can provide catalog size information ...

		if (accum.has_cat_size_info()) {

			// Set up the list of catalog size fractiles

			cat_size_fractile_values = new double[]{0.000, 0.025, 0.150, 0.400, 0.500, 0.600, 0.850, 0.975, 1.000};

			// Get catalog size fractiles

			cat_size_array = new int[cat_size_fractile_values.length][];
			for (int csfr_ix = 0; csfr_ix < cat_size_fractile_values.length; ++csfr_ix) {
				cat_size_array[csfr_ix] = accum.get_cat_size_fractile_array (cat_size_fractile_values[csfr_ix]);
			}

			// We have catalog size information

			has_cat_size = true;
		}

		// Otherwise, no catalog size information ...

		else {
			has_cat_size = false;
			cat_size_fractile_values = null;
			cat_size_array = null;
		}

		return;
	}




	//----- Implementation of USGS_ForecastModel -----




	// Return true if the model contains a mainshock magnitude.

	@Override
	public boolean hasMainShockMag () {
		return has_mainshock_mag;
	}

	// Return the magnitude of the mainshock.
	// Throw USGS_ForecastException if the model does not contain a mainshock magnitude.
	
	@Override
	public double getMainShockMag () {
		if (!( has_mainshock_mag )) {
			throw new USGS_ForecastException ("OEForecastGrid.getMainShockMag: No mainshock magnitude available");
		}
		return rounded_mainshock_mag;
	}




	// Get fractiles for the probability distribution of forecasted number of aftershocks.
	// Parameters:
	//  fractileArray = Desired fractiles (percentile/100) of the probability distribution.
	//  mag = Minimum magnitude of aftershocks considered.
	//  tMinDays = Start of time range, in days after some origin time.
	//  tMaxDays = End of time range, in days after some origin time.
	// The return value is an array whose length equals fractileArray.length.
	// The i-th element of the return value is the fractileArray[i] fractile of the
	// probability distribution, which is defined to be the minimum number n of
	// aftershocks with magnitude >= mag, occurring between times tMinDays and tMaxDays,
	// such that the probability of n or fewer aftershocks is >= fractileArray[i].
	// Note that although the return type is double[], the return values are integers.
	//
	// Note: The fractiles should account for both aleatory and epistemic uncertaintly.
	//
	// Note: It is acceptable for the function to return a value based on the duration
	// (defined to be tMaxDays - tMinDays), rather than examining tMinDays and tMaxDays
	// separately.  In this case, the start time of the forecasts must be coordinated
	// externally to this object.
	//
	// Note: Some models (e.g., RJ) are able to calculate results on-the-fly for any
	// given parameter values, while others (e.g., ETAS) can supply results only for
	// values that were pre-selected during model creation.  In the latter case,
	// external coordination is needed to ensure that the model can supply all
	// requested results.  This function should throw USGS_ForecastException if it
	// cannot supply the requested results.

	@Override
	public double[] getCumNumFractileWithAleatory (double[] fractileArray, double mag, double tMinDays, double tMaxDays) {

		// We need to have results

		if (!( has_results )) {
			throw new USGS_ForecastException ("OEForecastGrid.getCumNumFractileWithAleatory: No results available");
		}

		// Find time bin for supplied duration

		double dur_days = tMaxDays - tMinDays;
		int time_ix = OEArraysCalc.bfind_array (adv_time_values, dur_days, MATCH_TIME_EPS);
		if (time_ix < 0) {
			throw new USGS_ForecastException ("OEForecastGrid.getCumNumFractileWithAleatory: Unrecognized forecast duration: dur_days = " + dur_days);
		}

		// Find magnitude bin for supplied magnitude

		int mag_ix = OEArraysCalc.bfind_array (adv_merged_mag_values, mag, MATCH_MAG_EPS);
		if (mag_ix < 0) {
			throw new USGS_ForecastException ("OEForecastGrid.getCumNumFractileWithAleatory: Unrecognized forecast magnitude: mag = " + mag);
		}

		// Loop over supplied fractiles

		double[] result = new double[fractileArray.length];
		for (int n = 0; n < fractileArray.length; ++n) {

			// Find fractile index for the requested fractile

			int frac_ix = OEArraysCalc.bfind_array (adv_fractile_values, fractileArray[n], MATCH_FRACTILE_EPS);
			if (frac_ix < 0) {
				throw new USGS_ForecastException ("OEForecastGrid.getCumNumFractileWithAleatory: Unrecognized fractile: fractile = " + fractileArray[n]);
			}

			// Save the fractile

			result[n] = (double)(fractile_array[frac_ix][time_ix][mag_ix]);
		}

		return result;
	}
	



	// Get the probability that one or more aftershocks will occur.
	// Parameters:
	//  mag = Minimum magnitude of aftershocks considered.
	//  tMinDays = Start of time range, in days after some origin time.
	//  tMaxDays = End of time range, in days after some origin time.
	// The return value is the probability that one or more aftershocks with
	// magnitude >= mag will occur between times tMinDays and tMaxDays.
	//
	// Note: The function should account for both aleatory and epistemic uncertaintly.
	//
	// Note: It is acceptable for the function to return a value based on the duration
	// (defined to be tMaxDays - tMinDays), rather than examining tMinDays and tMaxDays
	// separately.  In this case, the start time of the forecasts must be coordinated
	// externally to this object.
	//
	// Note: Some models (e.g., RJ) are able to calculate results on-the-fly for any
	// given parameter values, while others (e.g., ETAS) can supply results only for
	// values that were pre-selected during model creation.  In the latter case,
	// external coordination is needed to ensure that the model can supply all
	// requested results.  This function should throw USGS_ForecastException if it
	// cannot supply the requested results.

	@Override
	public double getProbOneOrMoreEvents (double magMin, double tMinDays, double tMaxDays) {

		// We need to have results

		if (!( has_results )) {
			throw new USGS_ForecastException ("OEForecastGrid.getProbOneOrMoreEvents: No results available");
		}

		// Find time bin for supplied duration

		double dur_days = tMaxDays - tMinDays;
		int time_ix = OEArraysCalc.bfind_array (adv_time_values, dur_days, MATCH_TIME_EPS);
		if (time_ix < 0) {
			throw new USGS_ForecastException ("OEForecastGrid.getProbOneOrMoreEvents: Unrecognized forecast duration: dur_days = " + dur_days);
		}

		// Find magnitude bin for supplied magnitude

		int mag_ix = OEArraysCalc.bfind_array (adv_merged_mag_values, magMin, MATCH_MAG_EPS);
		if (mag_ix < 0) {
			throw new USGS_ForecastException ("OEForecastGrid.getProbOneOrMoreEvents: Unrecognized forecast magnitude: magMin = " + magMin);
		}

		// Find exceedence count index for the requested exceedence count

		int xcount = 0;
		int xcnt_ix = OEArraysCalc.bfind_array (adv_xcount_values, xcount);
		if (xcnt_ix < 0) {
			throw new USGS_ForecastException ("OEForecastGrid.getProbOneOrMoreEvents: Unrecognized exceedence count: xcount = " + xcount);
		}

		// Get the result

		double result = prob_occur_array[xcnt_ix][time_ix][mag_ix];
		return result;
	}




	// Return the name of this model.
	// The return value must be non-null and non-empty.

	@Override
	public String getModelName () {
		return model_name;
	}




	// Return the reference for this model.
	// The return value must be non-null and non-empty.
	// The value "#url" should be returned if there is no reference.

	//@Override
	//public default String getModelReference () {
	//	return "#url";
	//}




	// Get the list of model parameters.
	// The function returns a map, where each key is the name of a parameter, and
	// each value is the value of the parameter.
	// The return value can be null if there are no parameters.
	//
	// Note: Parameters are listed on the event page in the order that they are
	// supplied by the iterator of the map.  It is therefore recommended to use a
	// map such as LinkedHashMap to control the iteration order.
	//
	// Note: For consistency, it is recommended that parameter names use camelCase.
	//
	// Note: For floating-point parameter values, it is usually desireable to round
	// values to a small number of significant digits.

	@Override
	public Map<String, Object> getModelParamMap () {
		return model_params;
	}




	// Return the list of probabilities for the additional list of fractiles in the forecast.
	// Probabilities must be between 0 and 1, and listed in increasing order.
	// The return can be null to indicate that no additional list of fractiles is supported.

	@Override
	public double[] getFractileProbabilities () {
		return adv_fractile_values;
	}




	//----- Display -----




	// Convert a fractile array to a string.
	// Parameters:
	//  data_array = Data array.
	// Table layout is:
	//  7 cols for magnitude
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension int[adv_time_values.length][adv_merged_mag_values.length].

	public final String fractile_array_to_string (int[][] data_array) {
		StringBuilder result = new StringBuilder();

		// Number of time and magnitude bins

		int time_bins = adv_time_values.length;
		int mag_bins = adv_merged_mag_values.length;

		// Header line

		result.append (" M \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", adv_time_values[time_ix]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", adv_merged_mag_values[mag_ix]));
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result.append (String.format ("  %10d", data_array[time_ix][mag_ix]));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Convert a probability of occurrence array, as percentages, to a string.
	// Parameters:
	//  prob_array = Probability of occurence array.
	// Table layout is:
	//  7 cols for magnitude
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension double[adv_time_values.length][adv_merged_mag_values.length]
	// where fixed-point display to 5 decimal places as percentages (data * 100) is appropriate.

	public final String prob_occur_array_to_string (double[][] prob_array) {
		StringBuilder result = new StringBuilder();

		// Number of time and magnitude bins

		int time_bins = adv_time_values.length;
		int mag_bins = adv_merged_mag_values.length;

		// Header line

		result.append (" M \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", adv_time_values[time_ix]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", adv_merged_mag_values[mag_ix]));
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result.append (String.format ("  %10.5f", prob_array[time_ix][mag_ix] * 100.0));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Convert a catalog size fractile array to a string.
	// Parameters:
	//  data_array = Data array.
	// Table layout is:
	//  7 cols for magnitude
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension int[cat_size_fractile_values.length][adv_time_values.length].

	public final String cat_size_fractile_array_to_string (int[][] data_array) {
		StringBuilder result = new StringBuilder();

		// Number of time and catalog size fractile bins

		int time_bins = adv_time_values.length;
		int csfr_bins = cat_size_fractile_values.length;

		// Header line

		result.append (" % \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", adv_time_values[time_ix]));
		}
		result.append ("\n");

		// Data lines

		for (int csfr_ix = 0; csfr_ix < csfr_bins; ++csfr_ix) {
			result.append (String.format ("%7.3f", cat_size_fractile_values[csfr_ix] * 100.0));
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result.append (String.format ("  %10d", cat_size_array[csfr_ix][time_ix]));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Convert an array to a string with one element per line, including indexes.
	// Parameters:
	//  name = Name of this array, which appears on the header line.
	//  data = Array of data.
	// Each array element appears on a separate line, preceded by its index number.

	public static String array_to_one_per_line_string (String name, int[] data) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (name);
		result.append (" = {\n");

		// Data lines

		for (int ix = 0; ix < data.length; ++ix) {
			result.append (String.format ("%6d: %s\n", ix, Integer.toString (data[ix])));
		}

		// Trailer line

		result.append ("}\n");

		return result.toString();
	}


	public static String array_to_one_per_line_string (String name, long[] data) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (name);
		result.append (" = {\n");

		// Data lines

		for (int ix = 0; ix < data.length; ++ix) {
			result.append (String.format ("%6d: %s\n", ix, Long.toString (data[ix])));
		}

		// Trailer line

		result.append ("}\n");

		return result.toString();
	}


	public static String array_to_one_per_line_string (String name, double[] data) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (name);
		result.append (" = {\n");

		// Data lines

		for (int ix = 0; ix < data.length; ++ix) {
			result.append (String.format ("%6d: %s\n", ix, Double.toString (data[ix])));
		}

		// Trailer line

		result.append ("}\n");

		return result.toString();
	}




	// Convert a map to a string with one element per line, including indexes.
	// Parameters:
	//  name = Name of this array, which appears on the header line.
	//  data = Map containing data.
	// Each map entry appears on a separate line, including key and value.

	public static <K, V> String map_to_one_per_line_string (String name, Map<K, V> data) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (name);
		result.append (" = {\n");

		// Data lines

		for (Map.Entry<K, V> entry : data.entrySet()) {
			result.append ("  " + entry.getKey().toString() + " = " + entry.getValue().toString() + "\n");
		}

		// Trailer line

		result.append ("}\n");

		return result.toString();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEForecastGrid:" + "\n");

		result.append (array_to_one_per_line_string ("adv_time_values", adv_time_values));
		result.append (array_to_one_per_line_string ("adv_time_values_millis", adv_time_values_millis));
		result.append (array_to_one_per_line_string ("adv_mag_values", adv_mag_values));
		result.append (array_to_one_per_line_string ("adv_fractile_values", adv_fractile_values));
		result.append (array_to_one_per_line_string ("adv_xcount_values", adv_xcount_values));

		result.append ("has_mainshock_mag = " + has_mainshock_mag + "\n");
		result.append ("rounded_mainshock_mag = " + rounded_mainshock_mag + "\n");
		result.append (array_to_one_per_line_string ("adv_merged_mag_values", adv_merged_mag_values));
		result.append ("adv_mainshock_index = " + adv_mainshock_index + "\n");

		result.append ("model_name = " + model_name + "\n");
		result.append (map_to_one_per_line_string ("model_params", model_params));

		result.append ("has_results = " + has_results + "\n");
		if (has_results) {
			result.append ("has_cat_size = " + has_cat_size + "\n");
			if (has_cat_size) {
				result.append ("\n");
				result.append ("Catalog sizes\n");
				result.append (cat_size_fractile_array_to_string (cat_size_array));
			}
			for (int frac_ix = 0; frac_ix < adv_fractile_values.length; ++frac_ix) {
				result.append ("\n");
				result.append ("Fractile: " + adv_fractile_values[frac_ix] + "\n");
				result.append (fractile_array_to_string (fractile_array[frac_ix]));
			}
			for (int xcnt_ix = 0; xcnt_ix < adv_xcount_values.length; ++xcnt_ix) {
				result.append ("\n");
				result.append ("Probability of exceeding: " + adv_xcount_values[xcnt_ix] + "\n");
				result.append (prob_occur_array_to_string (prob_occur_array[xcnt_ix]));
			}
		}

		return result.toString();
	}




	// Display a summary of our contents.

	public String summary_string() {
		StringBuilder result = new StringBuilder();

		result.append ("has_mainshock_mag = " + has_mainshock_mag + "\n");
		result.append ("rounded_mainshock_mag = " + rounded_mainshock_mag + "\n");

		result.append ("has_results = " + has_results + "\n");
		if (has_results) {
			result.append ("has_cat_size = " + has_cat_size + "\n");
			if (has_cat_size) {
				result.append ("\n");
				result.append ("Catalog sizes\n");
				result.append (cat_size_fractile_array_to_string (cat_size_array));
			}
			double[] summary_fractile_values = {0.025, 0.500, 0.975};	// these need to be elements of adv_fractile_values
			for (double frac : summary_fractile_values) {
				int frac_ix = OEArraysCalc.bfind_array (adv_fractile_values, frac, MATCH_FRACTILE_EPS);
				if (frac_ix < 0) {
					throw new USGS_ForecastException ("OEForecastGrid.summary_string: Unrecognized fractile: fractile = " + frac);
				}
				result.append ("\n");
				result.append ("Fractile: " + adv_fractile_values[frac_ix] + "\n");
				result.append (fractile_array_to_string (fractile_array[frac_ix]));
			}
			int[] summary_xcount_values = {0};	// these need to be elements of adv_xcount_values
			for (int xcount : summary_xcount_values) {
				int xcnt_ix = OEArraysCalc.bfind_array (adv_xcount_values, xcount);
				if (xcnt_ix < 0) {
					throw new USGS_ForecastException ("OEForecastGrid.summary_string: Unrecognized exceedence count: xcount = " + xcount);
				}
				result.append ("\n");
				result.append ("Probability of exceeding: " + adv_xcount_values[xcnt_ix] + "\n");
				result.append (prob_occur_array_to_string (prob_occur_array[xcnt_ix]));
			}
		}

		return result.toString();
	}




	//----- Bin functions -----




	// Get the time values that delimit the time bins, in days.
	// Parameters:
	//  tbegin = Start time of the first time bin, in days.
	// The returned value has dimension:  time_values[time_bins + 1]
	// Here: time_bins = adv_time_values.length
	// Each time bin represents an interval between two successive time values.

	public double[] get_time_values (double tbegin) {
		int time_bins = adv_time_values.length;
		double[] time_values = new double[time_bins + 1];
		time_values[0] = tbegin;
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			time_values[time_ix + 1] = tbegin + adv_time_values[time_ix];
		}
		return time_values;
	}




	// Get the magnitude values that delimit the magnitude bins.
	// Parameters:
	//  mag_headroom = Size of the last magnitude bin, if zero or omitted defaults to DEF_MAG_HEADROOM.
	// The returned value has dimension:  mag_values[mag_bins + 1]
	// Here: mag_bins = adv_merged_mag_values.length
	// Each magnitude bin represents an interval between two successive magnitude values.
	// Note: Typically the last magnitude is very large.

	public double[] get_mag_values (double mag_headroom) {

		// Round size of last magnitude bin, and apply default

		double rounded_mag_headroom = round_mainshock_mag (mag_headroom);
		if (rounded_mag_headroom < MATCH_MAG_EPS) {
			rounded_mag_headroom = round_mainshock_mag (DEF_MAG_HEADROOM);
		}

		// Construct magnitude array, appending a new highest magnitude

		int mag_bins = adv_merged_mag_values.length;
		double[] mag_values = new double[mag_bins + 1];
		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			mag_values[mag_ix] = adv_merged_mag_values[mag_ix];
		}
		mag_values[mag_bins] = adv_merged_mag_values[mag_bins - 1] + rounded_mag_headroom;
		return mag_values;
	}


	public double[] get_mag_values () {
		return get_mag_values (DEF_MAG_HEADROOM);
	}




	// Get the time values that delimit the time bins used for catalog ranging, in days.
	// Parameters:
	//  tbegin = Start time of the first time bin, in days.
	//  time_ranging_delta = Target size for bins, in days, if zero or omitted defaults to DEF_TIME_RANGING_DELTA.
	// The returned value has dimension:  ranging_time_values[ranging_time_bins + 1]
	// Here ranging_time_bins is the number of time bins to use for ranging,
	// which is calculated in this function.
	// Each ranging time bin represents an interval between two successive ranging time values.
	// Implementation note: The array is constructed in such a way that each time value
	// returned by get_time_values() appears in the array (along with additional time values).

	public double[] get_ranging_time_values (double tbegin, double time_ranging_delta) {

		// Find the number of time units in the delta (one unit == TIME_DELTA_MILLIS), and apply default

		double time_spacing = ((double)TIME_DELTA_MILLIS) / SimpleUtils.DAY_MILLIS_D;
		long delta_units = Math.round (time_ranging_delta / time_spacing);
		if (delta_units < 1L) {
			delta_units = Math.round (DEF_TIME_RANGING_DELTA / time_spacing);
		}

		// Make arrays that contains the number of units and number of steps within each time bin

		int time_bins = adv_time_values.length;
		long[] bin_units = new long[time_bins];
		long[] steps_in_bin = new long[time_bins];
		int ranging_time_bins = 0;

		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {

			// Number of units in the current bin

			if (time_ix == 0) {
				bin_units[time_ix] = adv_time_values_millis[time_ix] / TIME_DELTA_MILLIS;
			} else {
				bin_units[time_ix] = (adv_time_values_millis[time_ix] - adv_time_values_millis[time_ix - 1]) / TIME_DELTA_MILLIS;
			}

			// Number of deltas that fit in the bin, rounded, and at least one

			steps_in_bin[time_ix] = (bin_units[time_ix] + (delta_units / 2L)) / delta_units;
			if (steps_in_bin[time_ix] < 1) {
				steps_in_bin[time_ix] = 1;
			}

			// Add to the total number of ranging time bins

			ranging_time_bins += ((int)(steps_in_bin[time_ix]));
		}

		// Now make the array of ranging time values, with the calculated number of steps within each bin

		double[] ranging_time_values = new double[ranging_time_bins + 1];
		int n = 0;
		ranging_time_values[n++] = tbegin;

		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {

			// Bin start in milliseconds

			long bin_start;
			if (time_ix == 0) {
				bin_start = 0L;
			} else {
				bin_start = adv_time_values_millis[time_ix - 1];
			}

			// Steps within the bin

			for (long j = 1; j < steps_in_bin[time_ix]; ++j) {

				// Number of units after start of bin

				long units_after_start = (j * bin_units[time_ix]) / steps_in_bin[time_ix];

				// Step in milliseconds

				long step_millis = bin_start + (units_after_start * TIME_DELTA_MILLIS);

				// Step in days

				double step_days = ((double)step_millis) / SimpleUtils.DAY_MILLIS_D;

				// Insert into array

				ranging_time_values[n++] = tbegin + step_days;
			}

			// End time of bin, this matches the value from get_time_values()

			ranging_time_values[n++] = tbegin + adv_time_values[time_ix];
		}

		// Check we filled the array

		if (!( n == ranging_time_bins + 1 )) {
			throw new InvariantViolationException ("OEForecastGrid.get_ranging_time_values: Inconsistent array index: n = " + n + ", ranging_time_bins + 1 = " + (ranging_time_bins + 1));
		}

		return ranging_time_values;
	}


	public double[] get_ranging_time_values (double tbegin) {
		return get_ranging_time_values (tbegin, DEF_TIME_RANGING_DELTA);
	}




	// Get the configured end time, for a given start time, in days.
	// Parameters:
	//  tbegin = Start time of the first time bin, in days.
	// The returned value is the end of the advisory time bins.

	public static double get_config_tend (double tbegin) {

		// Get action configuration

		ActionConfig action_config = new ActionConfig();

		// Get the largest advisory time value

		long time_value_millis = 0L;
		int count = action_config.get_adv_window_count();
		for (int time_ix = 0; time_ix < count; ++time_ix) {
			long millis = action_config.get_adv_window_end_off (time_ix);
			if (time_value_millis < millis) {
				time_value_millis = millis;
			}
		}

		// Convert to days

		double time_value = convert_adv_time (time_value_millis);
		return tbegin + time_value;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 101001;

	private static final String M_VERSION_NAME = "OEForecastGrid";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDoubleArray ("adv_time_values", adv_time_values);
			writer.marshalLongArray ("adv_time_values_millis", adv_time_values_millis);
			writer.marshalDoubleArray ("adv_mag_values", adv_mag_values);
			writer.marshalDoubleArray ("adv_fractile_values", adv_fractile_values);
			writer.marshalIntArray ("adv_xcount_values", adv_xcount_values);

			writer.marshalBoolean ("has_mainshock_mag", has_mainshock_mag);
			writer.marshalDouble ("rounded_mainshock_mag", rounded_mainshock_mag);
			writer.marshalDoubleArray ("adv_merged_mag_values", adv_merged_mag_values);
			writer.marshalInt ("adv_mainshock_index", adv_mainshock_index);

			writer.marshalString ("model_name", model_name);
			MarshalUtils.marshalMapStringObject (writer, "model_params", model_params);

			writer.marshalBoolean ("has_results", has_results);
			if (has_results) {
				writer.marshalInt3DArray ("fractile_array", fractile_array);
				writer.marshalDouble3DArray ("prob_occur_array", prob_occur_array);
				writer.marshalBoolean ("has_cat_size", has_cat_size);
				if (has_cat_size) {
					writer.marshalDoubleArray ("cat_size_fractile_values", cat_size_fractile_values);
					writer.marshalInt2DArray ("cat_size_array", cat_size_array);
				}
			}

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

			model_params = new LinkedHashMap<String, Object>();

			adv_time_values = reader.unmarshalDoubleArray ("adv_time_values");
			adv_time_values_millis = reader.unmarshalLongArray ("adv_time_values_millis");
			adv_mag_values = reader.unmarshalDoubleArray ("adv_mag_values");
			adv_fractile_values = reader.unmarshalDoubleArray ("adv_fractile_values");
			adv_xcount_values = reader.unmarshalIntArray ("adv_xcount_values");

			has_mainshock_mag = reader.unmarshalBoolean ("has_mainshock_mag");
			rounded_mainshock_mag = reader.unmarshalDouble ("rounded_mainshock_mag");
			adv_merged_mag_values = reader.unmarshalDoubleArray ("adv_merged_mag_values");
			adv_mainshock_index = reader.unmarshalInt ("adv_mainshock_index");

			model_name = reader.unmarshalString ("model_name");
			MarshalUtils.unmarshalMapStringObject (reader, "model_params", model_params);

			has_results = reader.unmarshalBoolean ("has_results");
			if (has_results) {
				fractile_array = reader.unmarshalInt3DArray ("fractile_array");
				prob_occur_array = reader.unmarshalDouble3DArray ("prob_occur_array");
				has_cat_size = reader.unmarshalBoolean ("has_cat_size");
				if (has_cat_size) {
					cat_size_fractile_values = reader.unmarshalDoubleArray ("cat_size_fractile_values");
					cat_size_array = reader.unmarshalInt2DArray ("cat_size_array");
				} else {
					cat_size_fractile_values = null;
					cat_size_array = null;
				}
			} else {
				fractile_array = null;
				prob_occur_array = null;
				has_cat_size = false;
				cat_size_fractile_values = null;
				cat_size_array = null;
			}

		}
		break;

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

	public OEForecastGrid unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEForecastGrid fcgrid) {
		writer.marshalMapBegin (name);
		fcgrid.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEForecastGrid static_unmarshal (MarshalReader reader, String name) {
		OEForecastGrid fcgrid = new OEForecastGrid();
		reader.unmarshalMapBegin (name);
		fcgrid.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return fcgrid;
	}




	//----- Testing -----




	// Test class for accumulator grid readout.

	private static class test_grid_readout implements OEAccumReadoutTimeMag {

		// Grid size.

		private int time_bins;
		private int mag_bins;

		private boolean f_cat_size;

		// Constructor accepts arrays of time and magnitude values.

		public test_grid_readout (double[] time_values, double[] mag_values, boolean use_cat_size) {
			time_bins = time_values.length - 1;
			mag_bins = mag_values.length - 1;
			f_cat_size = use_cat_size;
		}


		// Get a fractile.
		// Parameters:
		//  fractile = Fractile to find, should be between 0.0 and 1.0.
		// Returns a 2D array with dimensions r[time_bins][mag_bins].
		// Each element corresponds to one bin (which is cumulative by definition).
		// Each value is an integer N, such that the probability of N or fewer
		// ruptures is approximately equal to fractile.
		// Note: The returned array should be newly-allocated and not retained by this object.

		@Override
		public int[][] get_fractile_array (double fractile) {
			int[][] result = new int[time_bins][mag_bins];
			int base = (int)(fractile * 1000000.0);
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result[time_ix][0] = base * (time_ix + 1);
			}
			for (int mag_ix = 1; mag_ix < mag_bins; ++mag_ix) {
				for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
					result[time_ix][mag_ix] = result[time_ix][mag_ix - 1] / 10;
				}
			}
			return result;
		}


		// Get the probability of occurrence array.
		// Parameters:
		//  xcount = Number of ruptures to check, should be >= 0.
		// Returns a 2D array with dimensions r[time_bins][mag_bins].
		// Each element corresponds to one bin (which is cumulative by definition).
		// Each value is a real number v, such that v is the probability that
		// the number of ruptures that occur is greater than xcount.
		// Note: Currently, implementations are only required to support xcount == 0,
		// which gives the probability that at least one rupture occurs.
		// Note: The returned array should be newly-allocated and not retained by this object.

		@Override
		public double[][] get_prob_occur_array (int xcount) {
			double[][] result = new double[time_bins][mag_bins];
			double base = 0.80;
			for (int time_ix = time_bins - 1; time_ix >= 0; --time_ix) {
				result[time_ix][0] = base;
				base *= 0.5;
			}
			for (int mag_ix = 1; mag_ix < mag_bins; ++mag_ix) {
				for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
					result[time_ix][mag_ix] = result[time_ix][mag_ix - 1] / 10.0;
				}
			}
			return result;
		}


		// Return true if catalog size information is available.

		@Override
		public boolean has_cat_size_info () {
			return f_cat_size;
		}


		// Get a fractile for catalog size information.
		// Parameters:
		//  fractile = Fractile to find, should be between 0.0 and 1.0.
		// Returns a 1D array with dimensions r[time_bins].
		// Each element corresponds to one time bin (which is cumulative by definition).
		// Each value is an integer N, such that the probability of a catalog containing
		// N or fewer ruptures is approximately equal to fractile.
		// Note: The returned array should be newly-allocated and not retained by this object.
		// Note: Returns null if catalog size information is not available.

		@Override
		public int[] get_cat_size_fractile_array (double fractile) {
			if (f_cat_size) {
				int[] result = new int[time_bins];
				for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
					result[time_ix] = (int)Math.round(10000.0 * (fractile + 1.0 + (double)time_ix));
				}
				return result;
			}
			return null;
		}
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEForecastGrid : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  mag_main  tbegin  mag_headroom  time_ranging_delta
		// Construct the forecast grid using the given mainshock magnitude, and display it.
		// Construct and display the arrays for time value, mag values, and ranging time values.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Use mag_main = 0.0 for no mainshock.
		// Use mag_headroom = 0.0 for default magnitude headroom.
		// Use time_ranging_delta = 0.0 for default time ranging bin target size.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OEForecastGrid : Invalid 'test1' subcommand");
				return;
			}

			try {

				double mag_main = Double.parseDouble (args[1]);
				double tbegin = Double.parseDouble (args[2]);
				double mag_headroom = Double.parseDouble (args[3]);
				double time_ranging_delta = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Constructing and displaying forecast grid");
				System.out.println ("mag_main: " + mag_main);
				System.out.println ("tbegin: " + tbegin);
				System.out.println ("mag_headroom: " + mag_headroom);
				System.out.println ("time_ranging_delta: " + time_ranging_delta);
				System.out.println ();

				// Create the forecast grid

				OEForecastGrid fcgrid = new OEForecastGrid();

				// Advisory settings

				fcgrid.setup_advisory();

				// Maihshock settings, if desired

				if (mag_main > MATCH_MAG_EPS) {
					fcgrid.setup_mainshock (mag_main);
				}

				// Display the contents

				System.out.println ();
				System.out.println ("********** Forecast Grid Display **********");
				System.out.println ();

				System.out.println (fcgrid.toString());

				// Display resulting bins

				System.out.println ();
				System.out.println ("********** Forecast Bins **********");
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("time_values", fcgrid.get_time_values (tbegin)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("mag_values", fcgrid.get_mag_values (mag_headroom)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("ranging_time_values", fcgrid.get_ranging_time_values (tbegin, time_ranging_delta)));

				// Marshal to JSON

				System.out.println ();
				System.out.println ("********** Marshal to JSON **********");
				System.out.println ();

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OEForecastGrid.static_marshal (store, null, fcgrid);
				store.check_write_complete ();

				String json_string = store.get_json_string();
				//System.out.println (json_string);

				Object json_container = store.get_json_container();
				System.out.println (GeoJsonUtils.jsonObjectToString (json_container));

				// Unmarshal from JSON

				System.out.println ();
				System.out.println ("********** Unmarshal from JSON **********");
				System.out.println ();
			
				OEForecastGrid fcgrid2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				fcgrid2 = OEForecastGrid.static_unmarshal (retrieve, null);
				retrieve.check_read_complete ();

				// Display the contents

				System.out.println (fcgrid2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  mag_main  tbegin  mag_headroom  time_ranging_delta
		// Construct the forecast grid using the given mainshock magnitude, and display it.
		// Construct and display the arrays for time value, mag values, and ranging time values.
		// Add simulated forecast results and some model parameters, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Use mag_main = 0.0 for no mainshock.
		// Use mag_headroom = 0.0 for default magnitude headroom.
		// Use time_ranging_delta = 0.0 for default time ranging bin target size.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OEForecastGrid : Invalid 'test2' subcommand");
				return;
			}

			try {

				double mag_main = Double.parseDouble (args[1]);
				double tbegin = Double.parseDouble (args[2]);
				double mag_headroom = Double.parseDouble (args[3]);
				double time_ranging_delta = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Constructing and displaying forecast grid, and adding simulated forecast");
				System.out.println ("mag_main: " + mag_main);
				System.out.println ("tbegin: " + tbegin);
				System.out.println ("mag_headroom: " + mag_headroom);
				System.out.println ("time_ranging_delta: " + time_ranging_delta);
				System.out.println ();

				// Create the forecast grid

				OEForecastGrid fcgrid = new OEForecastGrid();

				// Advisory settings

				fcgrid.setup_advisory();

				// Maihshock settings, if desired

				if (mag_main > MATCH_MAG_EPS) {
					fcgrid.setup_mainshock (mag_main);
				}

				// Display the contents

				System.out.println ();
				System.out.println ("********** Forecast Grid Display **********");
				System.out.println ();

				System.out.println (fcgrid.toString());

				// Display resulting bins

				System.out.println ();
				System.out.println ("********** Forecast Bins **********");
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("time_values", fcgrid.get_time_values (tbegin)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("mag_values", fcgrid.get_mag_values (mag_headroom)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("ranging_time_values", fcgrid.get_ranging_time_values (tbegin, time_ranging_delta)));

				// Add forecast and parameters

				System.out.println ();
				System.out.println ("********** Forecast results **********");
				System.out.println ();

				test_grid_readout readout = new test_grid_readout (fcgrid.get_time_values (tbegin), fcgrid.get_mag_values (mag_headroom), true);
				fcgrid.supply_results (readout);

				fcgrid.add_model_param ("int", 17);
				fcgrid.add_model_param ("long", 987654321987654L);
				fcgrid.add_model_param ("float", 3.4567f);
				fcgrid.add_model_param ("double", 8.7654321098765);
				fcgrid.add_model_param ("boolean", true);
				fcgrid.add_model_param ("string", "this is a string\n");

				System.out.println (fcgrid.toString());

				// Marshal to JSON

				System.out.println ();
				System.out.println ("********** Marshal to JSON **********");
				System.out.println ();

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OEForecastGrid.static_marshal (store, null, fcgrid);
				store.check_write_complete ();

				String json_string = store.get_json_string();
				//System.out.println (json_string);

				Object json_container = store.get_json_container();
				System.out.println (GeoJsonUtils.jsonObjectToString (json_container));

				// Unmarshal from JSON

				System.out.println ();
				System.out.println ("********** Unmarshal from JSON **********");
				System.out.println ();
			
				OEForecastGrid fcgrid2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				fcgrid2 = OEForecastGrid.static_unmarshal (retrieve, null);
				retrieve.check_read_complete ();

				// Display the contents

				System.out.println (fcgrid2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  mag_main  tbegin  mag_headroom  time_ranging_delta
		// Construct the forecast grid using the given mainshock magnitude, and display it.
		// Construct and display the arrays for time value, mag values, and ranging time values.
		// Add simulated forecast results and some model parameters, and display it.
		// Construct a USGS forecast, and display the JSON.
		// Use mag_main = 0.0 for no mainshock.
		// Use mag_headroom = 0.0 for default magnitude headroom.
		// Use time_ranging_delta = 0.0 for default time ranging bin target size.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OEForecastGrid : Invalid 'test3' subcommand");
				return;
			}

			try {

				double mag_main = Double.parseDouble (args[1]);
				double tbegin = Double.parseDouble (args[2]);
				double mag_headroom = Double.parseDouble (args[3]);
				double time_ranging_delta = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Constructing and displaying forecast grid, and producing USGS forecast");
				System.out.println ("mag_main: " + mag_main);
				System.out.println ("tbegin: " + tbegin);
				System.out.println ("mag_headroom: " + mag_headroom);
				System.out.println ("time_ranging_delta: " + time_ranging_delta);
				System.out.println ();

				// Create the forecast grid

				OEForecastGrid fcgrid = new OEForecastGrid();

				// Advisory settings

				fcgrid.setup_advisory();

				// Maihshock settings, if desired

				if (mag_main > MATCH_MAG_EPS) {
					fcgrid.setup_mainshock (mag_main);
				}

				// Display the contents

				System.out.println ();
				System.out.println ("********** Forecast Grid Display **********");
				System.out.println ();

				System.out.println (fcgrid.toString());

				// Display resulting bins

				System.out.println ();
				System.out.println ("********** Forecast Bins **********");
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("time_values", fcgrid.get_time_values (tbegin)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("mag_values", fcgrid.get_mag_values (mag_headroom)));
				System.out.println ();

				System.out.println (array_to_one_per_line_string ("ranging_time_values", fcgrid.get_ranging_time_values (tbegin, time_ranging_delta)));

				// Add forecast and parameters

				System.out.println ();
				System.out.println ("********** Forecast results **********");
				System.out.println ();

				test_grid_readout readout = new test_grid_readout (fcgrid.get_time_values (tbegin), fcgrid.get_mag_values (mag_headroom), true);
				fcgrid.supply_results (readout);

				fcgrid.add_model_param ("int", 17);
				fcgrid.add_model_param ("long", 987654321987654L);
				fcgrid.add_model_param ("float", 3.4567f);
				fcgrid.add_model_param ("double", 8.7654321098765);
				fcgrid.add_model_param ("boolean", true);
				fcgrid.add_model_param ("string", "this is a string\n");

				System.out.println (fcgrid.toString());

				// Construct USGS forecast

				System.out.println ();
				System.out.println ("********** USGS Forecast **********");
				System.out.println ();

				Instant eventDate = Instant.parse ("2000-01-01T00:00:00Z");
				Instant startDate = Instant.parse ("2000-02-01T00:00:00Z");

				long creation_time = Instant.parse("2000-02-02T00:00:00Z").toEpochMilli();

				List<ObsEqkRupture> aftershocks = new ArrayList<ObsEqkRupture>();

				USGS_AftershockForecast usgsfc = new USGS_AftershockForecast (fcgrid, aftershocks, eventDate, startDate);

				//String json_string = usgsfc.buildJSONString (creation_time);
				//System.out.println (json_string);

				Object json_container = usgsfc.buildJSON (creation_time);
				System.out.println (GeoJsonUtils.jsonObjectToString (json_container));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEForecastGrid : Unrecognized subcommand : " + args[0]);
		return;

	}





}
