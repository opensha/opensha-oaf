package org.opensha.oaf.oetas.bay;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

import java.io.IOException;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionWorld;

import org.opensha.oaf.util.gui.GUIExternalCatalog;

import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEExaminerSaveList;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OESimulator;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;
import org.opensha.oaf.oetas.bay.OEBayPriorValue;
import org.opensha.oaf.oetas.bay.OEGaussAPCParams;
import org.opensha.oaf.oetas.bay.OEGaussAPCConfig;

import org.opensha.oaf.oetas.env.OEtasConfig;
import org.opensha.oaf.oetas.env.OEtasResults;

import org.opensha.oaf.oetas.fit.OEGridParams;
import org.opensha.oaf.oetas.fit.OEGridOptions;

import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEMarginalDistSet;
import org.opensha.oaf.oetas.util.OEMarginalDistSetBuilder;
import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.AftershockStatsShadow;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;
import org.opensha.oaf.rj.MagCompPage_Parameters;
import org.opensha.oaf.rj.MagCompPage_ParametersFetch;
import org.opensha.oaf.rj.OAFRegimeParams;
import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.SearchMagFn;
import org.opensha.oaf.rj.SearchRadiusFn;
import org.opensha.oaf.rj.USGS_ForecastInfo;

import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;
import org.opensha.oaf.aafs.IntakeSphRegion;
import org.opensha.oaf.aafs.ServerComponent;
import org.opensha.oaf.aafs.VersionInfo;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
//import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatVisitor;

import gov.usgs.earthquake.event.JsonEvent;


// Test and generation functions for Bayesian priors for ETAS.
// Author: Michael Barall 12/02/2024.

public class OEBayTest {




	//----- Subroutines -----




	// Get a list of earthquakes satisfying intake criteria in a given range of time.
	// Parameters:
	//  startTime = Start of time interval, in milliseconds after the epoch.
	//  endTime = End of time interval, in milliseconds after the epoch.
	//  wrapLon = Desired longitude range: false = -180 to 180; true = 0 to 360.
	//  extendedInfo = True to return extended information.
	// Returns a list of ruptures that satisfy intake criteria.

	public static ObsEqkRupList fetchIntakeEventList (long startTime, long endTime, boolean wrapLon, boolean extendedInfo) {

		// Create the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Construct the region

		SphRegionWorld region = new SphRegionWorld ();

		// The list of ruptures we are going to build

		final ObsEqkRupList rups = new ObsEqkRupList();

		// The action configuration

		final ActionConfig action_config = new ActionConfig();

		// Count of events checked

		final int[] events_checked = new int[1];
		events_checked[0] = 0;

		// The visitor that builds the list, checking that each rupture passes intake

		ComcatVisitor visitor = new ComcatVisitor() {
			@Override
			public int visit (ObsEqkRupture rup, JsonEvent geojson) {

				//String rup_event_id = rup.getEventId();
				//long rup_time = rup.getOriginTime();
				double rup_mag = rup.getMag();
				Location rup_hypo = rup.getHypocenterLocation();
				double rup_lat = rup_hypo.getLatitude();
				double rup_lon = rup_hypo.getLongitude();

				// Search intake regions, using the minimum magnitude criterion

				IntakeSphRegion intake_region = action_config.get_pdl_intake_region_for_min_mag (
					rup_lat, rup_lon, rup_mag);

				// If we passed the intake filter, add to the list

				if (intake_region != null) {
					rups.add(rup);
				}

				// Count the event

				events_checked[0] = events_checked[0] + 1;

				return 0;
			}
		};

		// Minimum magnitude for any intake region

		double minMag = action_config.get_pdl_intake_region_min_min_mag();

		// Call Comcat

		String rup_event_id = null;
		double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
		String productType = null;
		boolean includeDeleted = false;

		int visit_result = accessor.visitEventList (visitor, rup_event_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo,
				minMag, productType, includeDeleted);

		System.out.println ("Count of events checked for intake = " + events_checked[0]);
		System.out.println ("Count of events passing intake criterion = " + rups.size());

		return rups;
	}




	// Test if an event is shadowed.
	// Parameters:
	//  event_id = Mainshock event ID.
	//  next_forecast_lag = Relative time at which forecast is computed, in milliseconds since the mainshock.
	//  analyst_params = Analyst parameters used for computing forecast parameters, or null if none.
	//  fcmain = Receives mainshock parameters.
	//  forecast_params = Received forecast parameters.
	//  forecast_results = Receives forecast results, catalog only, following a call to forecast_results.calc_catalog_only().
	// If the event is shadowed, returns true, and fcmain, forecast_params, and forecast_results are not useable.
	// If the event is not shadowed, return false, and fill in fcmain, forecast_params, and forecast_results.
	// Note: fcmain, forecast_params, and forecast_results should be newly-allocated.
	// Note: Throws exception if the event is not found, or if there is a Comcat access error.
	// Note: Much of the code is copied from ExGenerateForecast.java.

	public static boolean is_event_shadowed (String event_id, long next_forecast_lag, ForecastParameters analyst_params,
		ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

		// The action configuration

		ActionConfig action_config = new ActionConfig();

		// Get mainshock parameters

		fcmain.setup_mainshock_only (event_id);		// throws exception if not found or other error

		System.out.println ();
		System.out.println (fcmain.toString());

		// Get find_shadow parameters

		ObsEqkRupture rup = fcmain.get_eqk_rupture();
		long time_now = fcmain.mainshock_time + next_forecast_lag;
		double search_radius = action_config.get_shadow_search_radius();
		long search_time_lo = fcmain.mainshock_time - action_config.get_shadow_lookback_time();
		long search_time_hi = fcmain.mainshock_time + next_forecast_lag;
		long centroid_rel_time_lo = 0L;
		long centroid_rel_time_hi = ServerComponent.DURATION_HUGE;
		double centroid_mag_floor = action_config.get_shadow_centroid_mag();
		double large_mag = action_config.get_shadow_large_mag();
		double[] separation = new double[2];
		long[] seq_end_time = new long[2];

		int shadow_method = action_config.get_shadow_method();
		double large_mag_3 = action_config.get_shadow3_large_mag();
		double centroid_multiplier = action_config.get_shadow3_centroid_mult();
		double sample_multiplier = action_config.get_shadow3_sample_mult();

		// Run find_shadow

		ObsEqkRupture shadow;

		if (shadow_method == 2) {

			shadow = AftershockStatsShadow.find_shadow_v2 (rup, time_now,
				search_radius, search_time_lo, search_time_hi,
				centroid_rel_time_lo, centroid_rel_time_hi,
				centroid_mag_floor, large_mag, separation, seq_end_time);
				
		} else {

			shadow = AftershockStatsShadow.find_shadow_v3 (rup, time_now,
				search_radius, search_time_lo, search_time_hi,
				centroid_multiplier, sample_multiplier,
				large_mag_3, separation, seq_end_time);

		}

		// If we are shadowed ...

		if (shadow != null) {

			// Return shadowed

			return true;
		}

		// Fetch parameters (model and search parameters), and calculate catalog

		forecast_params.fetch_all_params (next_forecast_lag, fcmain, analyst_params);

		forecast_params.next_scheduled_lag = action_config.get_next_forecast_lag (
			next_forecast_lag + action_config.get_forecast_min_gap(),		// min_lag
			0L																// max_lag
		);

		if (forecast_params.next_scheduled_lag > 0L) {
			forecast_params.next_scheduled_lag += (
				action_config.get_comcat_clock_skew()
				+ action_config.get_comcat_origin_skew()
			);
		}

		long advisory_lag;

		if (next_forecast_lag >= action_config.get_advisory_dur_year()) {
			advisory_lag = ForecastResults.ADVISORY_LAG_YEAR;
		} else if (next_forecast_lag >= action_config.get_advisory_dur_month()) {
			advisory_lag = ForecastResults.ADVISORY_LAG_MONTH;
		} else if (next_forecast_lag >= action_config.get_advisory_dur_week()) {
			advisory_lag = ForecastResults.ADVISORY_LAG_WEEK;
		} else {
			advisory_lag = ForecastResults.ADVISORY_LAG_DAY;
		}

		String the_injectable_text = forecast_params.get_eff_injectable_text (
				action_config.get_def_injectable_text());

		forecast_results.calc_catalog_only (
			fcmain.mainshock_time + next_forecast_lag,
			advisory_lag,
			the_injectable_text,
			fcmain,
			forecast_params,
			next_forecast_lag >= action_config.get_seq_spec_min_lag());

		// If we have an earthquake catalog ...

		if (forecast_results.catalog_result_avail) {

			// If there is an aftershock with larger magnitude than the mainshock ...

			if (forecast_results.catalog_eqk_count > 0 && forecast_results.catalog_max_mag > fcmain.mainshock_mag) {

				// Return shadowed

				return true;
			}
		}

		// Return not shadowed

		return false;
	}




	// Test if an event is shadowed.
	// Parameters:
	//  fcmain = Mainshock parameters.
	//  forecast_params = Forecast parameters.
	//  forecast_results = Forecast results, with at least the catalog loaded, (eg following a call to forecast_results.calc_catalog_only()).
	// If the event is shadowed, returns true, and fcmain, forecast_params, and forecast_results are not useable.
	// If the event is not shadowed, return false, and fill in fcmain, forecast_params, and forecast_results.
	// Note: fcmain, forecast_params, and forecast_results should be newly-allocated.
	// Note: Throws exception if the event is not found, or if there is a Comcat access error.
	// Note: Much of the code is copied from ExGenerateForecast.java.

	public static int count_aftershock_above_mcat (ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

		// The aftershock count

		int as_count = 0;

		// If we have an earthquake catalog ...

		if (forecast_results.catalog_result_avail && forecast_results.catalog_comcat_aftershocks != null) {

			// The magnitude of completeness

			double magCat = forecast_params.mag_comp_params.get_magCat (fcmain.mainshock_mag);

			// Loop over ruptures ...

			for (ObsEqkRupture rup : forecast_results.catalog_comcat_aftershocks) {

				// The rupture time and magnitude

				long rup_time = rup.getOriginTime();
				double rup_mag = rup.getMag();

				// If the rupture is after the mainshock ...

				if (rup_time > fcmain.mainshock_time) {

					// Rupture time after the mainshock, in days

					double tDays = SimpleUtils.millis_to_days (rup_time - fcmain.mainshock_time);

					// Time-dependent magnitude of completeness

					double mcat = forecast_params.mag_comp_params.get_magCompFn().getMagCompleteness (fcmain.mainshock_mag, magCat, tDays);

					// If rupture magnitude is at least the magnitude of completeness, count it

					if (rup_mag >= mcat) {
						++as_count;
					}
				}
			}
		}

		// Return the count

		return as_count;
	}




	// Test if an event is shadowed.
	// Parameters:
	//  event_id = Mainshock event ID.
	//  next_forecast_lag = Relative time at which forecast is computed, in milliseconds since the mainshock.
	//  analyst_params = Analyst parameters used for computing forecast parameters, or null if none.
	// If the event is shadowed, returns true.
	// If the event is not shadowed, return false.
	// Note: Throws exception if the event is not found, or if there is a Comcat access error.

	public static boolean is_event_shadowed (String event_id, long next_forecast_lag, ForecastParameters analyst_params) {

		ForecastMainshock fcmain = new ForecastMainshock();
		ForecastParameters forecast_params = new ForecastParameters();
		ForecastResults forecast_results = new ForecastResults();

		return is_event_shadowed (event_id, next_forecast_lag, analyst_params,
			fcmain, forecast_params, forecast_results);
	}




	// Sequence accumulator that just counts the number of sequences.

	private static class SeqAccumCount implements Marshalable {

		// The minimum magnitude delta above the magnitude of completeness, for a sequence to count.

		public double min_mag_cat_delta;

		// The minimum catalog size (in ForecastResults) for a sequence to count.

		public int min_catalog_size;

		// The count of number of sequences.

		public int seq_count;

		// Constructor.

		public SeqAccumCount (double min_mag_cat_delta, int min_catalog_size) {
			this.min_mag_cat_delta = min_mag_cat_delta;
			this.min_catalog_size = min_catalog_size;
			this.seq_count = 0;
		}

		// Construct an empty object.

		public SeqAccumCount () {
			this.min_mag_cat_delta = 0.0;
			this.min_catalog_size = 0;
			this.seq_count = 0;
		}

		// Accumulate a sequence.

		public void seq_accum (ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

			// Paramaters must contain generic and magnitude of completeness parameters

			if (!( forecast_params.generic_avail && forecast_params.mag_comp_avail )) {
				return;
			}

			// Check for magnitude of completeness delta

			if (!( fcmain.mainshock_mag >= forecast_params.mag_comp_params.get_magCat (fcmain.mainshock_mag) + min_mag_cat_delta )) {
				return;
			}

			// Check for catalog available and sufficient size

			//  if (!( forecast_results.catalog_result_avail && forecast_results.catalog_eqk_count >= min_catalog_size )) {
			//  	return;
			//  }

			if (!( forecast_results.catalog_result_avail
				&& forecast_results.catalog_comcat_aftershocks != null
				&& count_aftershock_above_mcat (fcmain, forecast_params, forecast_results) >= min_catalog_size )) {

				return;
			}

			// Count the sequence

			++seq_count;

			return;
		}

		// Marshal object, internal.

		protected void do_marshal (MarshalWriter writer) {
			writer.marshalDouble ("min_mag_cat_delta", min_mag_cat_delta);
			writer.marshalInt  ("min_catalog_size", min_catalog_size);
			writer.marshalInt  ("seq_count", seq_count);
			return;
		}

		// Unmarshal object, internal.

		protected void do_umarshal (MarshalReader reader) {
			min_mag_cat_delta = reader.unmarshalDouble ("min_mag_cat_delta");
			min_catalog_size = reader.unmarshalInt  ("min_catalog_size");
			seq_count = reader.unmarshalInt  ("seq_count");
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
		public SeqAccumCount unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			do_umarshal (reader);
			reader.unmarshalMapEnd ();
			return this;
		}
	}




	// Holds SeqAccumCount objects for each tectonic regime, plus global.

	private static class SeqAccumCountHolder implements Marshalable {

		// True to use ETAS regimes, false to use generic regimes.

		public boolean f_etas_regime;

		// The list of regime names, sorted.

		public List<String> regime_names;

		// A global accumulator.

		public SeqAccumCount global_accum;

		// A map, containing an accumulator for each tectonic regime.

		public Map<String, SeqAccumCount> regime_accums;

		// Constructor allocates the accumulators.

		public SeqAccumCountHolder (double min_mag_cat_delta, int min_catalog_size, boolean f_etas_regime) {

			// Save the parameters

			this.f_etas_regime = f_etas_regime;

			// Get action parameters and enable ETAS if we are using ETAS regimes

			ActionConfig action_config = new ActionConfig();

			if (this.f_etas_regime) {
				action_config.get_action_config_file().etas_enable = ActionConfigFile.ETAS_ENA_ENABLE;
			}

			// Allocate the global accumulator

			global_accum = new SeqAccumCount (min_mag_cat_delta, min_catalog_size);

			// Get the list of regime names

			regime_names = new ArrayList<String>();

			if (this.f_etas_regime) {
				OEtasConfig fetch = new OEtasConfig();
				for (OAFTectonicRegime regime : fetch.getRegimeSet()) {
					regime_names.add (regime.toString());
				}
			} else {
				GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();
				for (OAFTectonicRegime regime : fetch.getRegimeSet()) {
					regime_names.add (regime.toString());
				}
			}

			Collections.sort (regime_names);

			// Allocate the per-regime accumulators
			
			regime_accums = new HashMap<String, SeqAccumCount>();
			for (String regime_name : regime_names) {
				regime_accums.put (regime_name, new SeqAccumCount (min_mag_cat_delta, min_catalog_size));
			}
		}

		// Construct an empty object.

		public SeqAccumCountHolder () {
			f_etas_regime = false;
			regime_names = null;
			global_accum = null;
			regime_accums = null;
		}

		// Accumulate a sequence.

		public void seq_accum (ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

			// Paramaters must contain generic parameters

			if (!( forecast_params.generic_avail )) {
				return;
			}

			// If using ETAS regimes, parameters must contain ETAS parameters.

			if (f_etas_regime) {
				if (!( forecast_params.etas_avail )) {
					return;
				}
			}

			// Accumulate global count

			global_accum.seq_accum (fcmain, forecast_params, forecast_results);

			// Accumulate per-regime count

			String regime_name = ((f_etas_regime) ? (forecast_params.etas_regime) : (forecast_params.generic_regime));
			regime_accums.get(regime_name).seq_accum (fcmain, forecast_params, forecast_results);

			return;
		}

		// Make a string containing the counts.

		public String count_string () {
			StringBuilder result = new StringBuilder();

			result.append ("Global count: " + global_accum.seq_count + "\n");

			result.append ("Regime counts:" + "\n");
			for (String regime_name : regime_names) {
				result.append (regime_name + ": " + regime_accums.get(regime_name).seq_count + "\n");
			}

			return result.toString();
		}

		// Marshal object, internal.

		protected void do_marshal (MarshalWriter writer) {
			writer.marshalBoolean ("f_etas_regime", f_etas_regime);
			writer.marshalStringCollection ("regime_names", regime_names);
			global_accum.marshal (writer, "global_accum");

			writer.marshalMapBegin ("regime_accums");
			for (String regime_name : regime_names) {
				regime_accums.get(regime_name).marshal (writer, regime_name);
			}
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal object, internal.

		protected void do_umarshal (MarshalReader reader) {
			f_etas_regime = reader.unmarshalBoolean ("f_etas_regime");
			regime_names = new ArrayList<String>();
			reader.unmarshalStringCollection ("regime_names", regime_names);
			global_accum = (new SeqAccumCount()).unmarshal (reader, "global_accum");

			reader.unmarshalMapBegin ("regime_accums");
			regime_accums = new HashMap<String, SeqAccumCount>();
			for (String regime_name : regime_names) {
				regime_accums.put (regime_name, (new SeqAccumCount()).unmarshal (reader, regime_name));
			}
			reader.unmarshalMapEnd ();
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
		public SeqAccumCountHolder unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			do_umarshal (reader);
			reader.unmarshalMapEnd ();
			return this;
		}
	}








	// Sequence accumulator that just counts the number of sequences.

	private static class SeqAccumStackDist implements Marshalable {

		// The count of number of sequences.

		public int seq_count;

		// The stacked marginal distribution

		public OEMarginalDistSet dist_set_stack;

		// Constructor.

		public SeqAccumStackDist () {
			this.seq_count = 0;
			dist_set_stack = new OEMarginalDistSet();
		}

		// Accumulate a sequence.

		public void seq_accum (ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

			// ETAS forecast must have completed successfully and produced expected results

			if (!( forecast_results.has_etas_json()
				&& forecast_results.etas_outcome instanceof OEtasResults )) {

				return;
			}

			OEtasResults etas_results = (OEtasResults)(forecast_results.etas_outcome);

			if (!( etas_results.full_marginals != null )) {
				return;
			}

			// If this is the first call, begin stacking

			if (seq_count == 0) {
				dist_set_stack.begin_stacking (etas_results.full_marginals);
			}

			// Confirm stacking compatibility

			if (!( dist_set_stack.is_stackable (etas_results.full_marginals) )) {
				throw new IllegalArgumentException ("OEBayTest.SeqAccumStackDist.seq_accum: Incompatible distribution set for stacking");
			}

			// Stack it

			double w = 1.0;

			dist_set_stack.add_to_stack (etas_results.full_marginals, w);

			// Count the sequence

			++seq_count;

			return;
		}

		// Write files containing the table strings.
		// Parameters:
		//  fn_prefix = Filename prefix, can be empty but not null.
		//  fn_suffix = Filename suffix, can be empty but not null.
		// If no sequences are stacked, then nothing is written.

		public void write_table_files (String fn_prefix, String fn_suffix) throws IOException {
			if (seq_count > 0) {
				dist_set_stack.write_table_files (fn_prefix, fn_suffix);
			}
			return;
		}

		// End accumulation of sequences.

		public void end_accum () {

			// If stacking has occurrend, end it

			if (seq_count != 0) {
				dist_set_stack.end_stacking (1000.0, 1000.0, "%.5e");
			}

			return;
		}

		// Marshal object, internal.

		protected void do_marshal (MarshalWriter writer) {
			writer.marshalInt  ("seq_count", seq_count);
			dist_set_stack.marshal (writer, "dist_set_stack");
			return;
		}

		// Unmarshal object, internal.

		protected void do_umarshal (MarshalReader reader) {
			seq_count = reader.unmarshalInt  ("seq_count");
			dist_set_stack = (new OEMarginalDistSet()).unmarshal (reader, "dist_set_stack");
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
		public SeqAccumStackDist unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			do_umarshal (reader);
			reader.unmarshalMapEnd ();
			return this;
		}
	}




	// Holds SeqAccumStackDist objects for each tectonic regime, plus global.

	private static class SeqAccumStackDistHolder implements Marshalable {

		// The minimum magnitude delta above the magnitude of completeness, for a sequence to count.

		public double min_mag_cat_delta;

		// The minimum catalog size (in ForecastResults) for a sequence to count.

		public int min_catalog_size;

		// The minimum fitting count (in OEtasResults) for a sequence to count.

		public int min_fitting_count;

		// True to use ETAS regimes, false to use generic regimes.

		public boolean f_etas_regime;

		// Remapping for regime names

		public Map<String, String> regime_remap;

		// The list of regime names (remapped), sorted.

		public List<String> regime_names;

		// A global accumulator.

		public SeqAccumStackDist global_accum;

		// A map, containing an accumulator for each tectonic regime.

		public Map<String, SeqAccumStackDist> regime_accums;

		// Constructor allocates the accumulators.

		public SeqAccumStackDistHolder (double min_mag_cat_delta, int min_catalog_size, int min_fitting_count, boolean f_etas_regime, Map<String, String> regime_remap) {

			// Save the parameters

			this.min_mag_cat_delta = min_mag_cat_delta;
			this.min_catalog_size = min_catalog_size;
			this.min_fitting_count = min_fitting_count;
			this.f_etas_regime = f_etas_regime;
			this.regime_remap = regime_remap;

			// If a null map was supplied, replace it with an empty map

			if (this.regime_remap == null) {
				this.regime_remap = new HashMap<String, String>();
			}

			// Get action parameters and enable ETAS

			ActionConfig action_config = new ActionConfig();

			action_config.get_action_config_file().etas_enable = ActionConfigFile.ETAS_ENA_ENABLE;

			// Allocate the global accumulator

			global_accum = new SeqAccumStackDist();

			// Get the list of regime names

			regime_names = new ArrayList<String>();
			Set<String> names_seen = new HashSet<String>();

			if (this.f_etas_regime) {

				OEtasConfig fetch = new OEtasConfig();
				for (OAFTectonicRegime regime : fetch.getRegimeSet()) {
					String regime_name = regime.toString();
					if (this.regime_remap.containsKey (regime_name)) {
						regime_name = this.regime_remap.get (regime_name);
					}
					if (names_seen.add (regime_name)) {
						regime_names.add (regime_name);
					}
				}

			} else {

				GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();
				for (OAFTectonicRegime regime : fetch.getRegimeSet()) {
					String regime_name = regime.toString();
					if (this.regime_remap.containsKey (regime_name)) {
						regime_name = this.regime_remap.get (regime_name);
					}
					if (names_seen.add (regime_name)) {
						regime_names.add (regime_name);
					}
				}

			}

			Collections.sort (regime_names);

			// Allocate the per-regime accumulators
			
			regime_accums = new HashMap<String, SeqAccumStackDist>();
			for (String regime_name : regime_names) {
				regime_accums.put (regime_name, new SeqAccumStackDist());
			}
		}

		// Construct an empty object.

		public SeqAccumStackDistHolder () {
			min_mag_cat_delta = 0.0;
			min_catalog_size = 0;
			min_fitting_count = 0;
			f_etas_regime = true;
			regime_remap = null;
			regime_names = null;
			global_accum = null;
			regime_accums = null;
		}

		// Accumulate a sequence.

		public void seq_accum (ForecastMainshock fcmain, ForecastParameters forecast_params, ForecastResults forecast_results) {

			// Paramaters must contain generic, magnitude of completeness, and ETAS parameters

			if (!( forecast_params.generic_avail
				&& forecast_params.mag_comp_avail
				&& forecast_params.etas_avail )) {

				return;
			}

			// Check for magnitude of completeness delta

			if (!( fcmain.mainshock_mag >= forecast_params.mag_comp_params.get_magCat (fcmain.mainshock_mag) + min_mag_cat_delta )) {
				return;
			}

			// Check for catalog available and sufficient size

			//  if (!( forecast_results.catalog_result_avail && forecast_results.catalog_eqk_count >= min_catalog_size )) {
			//  	return;
			//  }

			if (!( forecast_results.catalog_result_avail
				&& forecast_results.catalog_comcat_aftershocks != null
				&& count_aftershock_above_mcat (fcmain, forecast_params, forecast_results) >= min_catalog_size )) {

				return;
			}

			// Set number of ETAS simulations to minimum

			forecast_params.etas_params.set_num_catalogs_to_minimum();

			// Run forecast

			forecast_results.calc_after_catalog (fcmain, forecast_params);

			// ETAS forecast must have completed successfully and produced expected results

			if (!( forecast_results.has_etas_json()
				&& forecast_results.etas_outcome instanceof OEtasResults )) {

				return;
			}

			OEtasResults etas_results = (OEtasResults)(forecast_results.etas_outcome);

			if (!( etas_results.full_marginals != null )) {
				return;
			}

			// Check for sufficient number of ruptures used for fitting

			if (!( etas_results.fitting_count >= min_fitting_count )) {
				return;
			}

			// Accumulate global stack

			global_accum.seq_accum (fcmain, forecast_params, forecast_results);

			// Accumulate per-regime stack

			String regime_name = ((f_etas_regime) ? (forecast_params.etas_regime) : (forecast_params.generic_regime));
			if (regime_remap.containsKey (regime_name)) {
				regime_name = regime_remap.get (regime_name);
			}
			regime_accums.get(regime_name).seq_accum (fcmain, forecast_params, forecast_results);

			return;
		}

		// End accumulation of sequences.

		public void end_accum () {

			// End accumulation for global stack

			global_accum.end_accum();

			// End accumulation for per-regime stacks

			for (String regime_name : regime_names) {
				regime_accums.get(regime_name).end_accum();
			}

			return;
		}

		// Make a string containing the counts.

		public String count_string () {
			StringBuilder result = new StringBuilder();

			result.append ("Global count: " + global_accum.seq_count + "\n");

			result.append ("Regime counts:" + "\n");
			for (String regime_name : regime_names) {
				result.append (regime_name + ": " + regime_accums.get(regime_name).seq_count + "\n");
			}

			return result.toString();
		}

		// Write files containing the table strings.
		// Parameters:
		//  fn_prefix = Filename prefix, can be empty but not null.
		//  fn_suffix = Filename suffix, can be empty but not null.

		public void write_table_files (String fn_prefix, String fn_suffix) throws IOException {

			global_accum.write_table_files (fn_prefix + "global-", fn_suffix);

			for (String regime_name : regime_names) {
				String nice_name = regime_name.trim().toLowerCase().replace(' ', '_').replace('-', '_').replace('/', '_');
				regime_accums.get(regime_name).write_table_files (fn_prefix + nice_name + "-", fn_suffix);
			}

			return;
		}

		// Marshal object, internal.

		protected void do_marshal (MarshalWriter writer) {
			writer.marshalDouble ("min_mag_cat_delta", min_mag_cat_delta);
			writer.marshalInt  ("min_catalog_size", min_catalog_size);
			writer.marshalInt  ("min_fitting_count", min_fitting_count);
			writer.marshalBoolean  ("f_etas_regime", f_etas_regime);

			MarshalUtils.marshalMapStringString (writer, "regime_remap", regime_remap);

			writer.marshalStringCollection ("regime_names", regime_names);
			global_accum.marshal (writer, "global_accum");

			writer.marshalMapBegin ("regime_accums");
			for (String regime_name : regime_names) {
				regime_accums.get(regime_name).marshal (writer, regime_name);
			}
			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal object, internal.

		protected void do_umarshal (MarshalReader reader) {
			min_mag_cat_delta = reader.unmarshalDouble ("min_mag_cat_delta");
			min_catalog_size = reader.unmarshalInt  ("min_catalog_size");
			min_fitting_count = reader.unmarshalInt  ("min_fitting_count");
			f_etas_regime = reader.unmarshalBoolean  ("f_etas_regime");

			regime_remap = new HashMap<String, String>();
			MarshalUtils.unmarshalMapStringString (reader, "regime_remap", regime_remap);

			regime_names = new ArrayList<String>();
			reader.unmarshalStringCollection ("regime_names", regime_names);
			global_accum = (new SeqAccumStackDist()).unmarshal (reader, "global_accum");

			reader.unmarshalMapBegin ("regime_accums");
			regime_accums = new HashMap<String, SeqAccumStackDist>();
			for (String regime_name : regime_names) {
				regime_accums.put (regime_name, (new SeqAccumStackDist()).unmarshal (reader, regime_name));
			}
			reader.unmarshalMapEnd ();
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
		public SeqAccumStackDistHolder unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			do_umarshal (reader);
			reader.unmarshalMapEnd ();
			return this;
		}
	}




	//----- Test or generation functions -----




	// test1/hello_world
	// Command line arguments:
	//  <empty>
	// Display a hello world message.

	public static void test1 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Displaying hello world message");
		testargs.end_test();

		// Say hello

		System.out.println ();
		System.out.println ("Hello, World");

		return;
	}




	// test2/intake_test
	// Command line arguments:
	//  start_time  end_time
	// Get the list of earthquakes satisfying intake criteria in the time range,
	// and display information about the list.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

	public static void test2 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Get the list of earthquakes satisfying intake criteria in the time range");
		long start_time = testargs.get_time ("start_time");
		long end_time = testargs.get_time ("end_time");

		testargs.end_test();

		// Get list of ruptures satisfying intake

		boolean wrapLon = false;
		boolean extendedInfo = true;

		ObsEqkRupList rups = fetchIntakeEventList (start_time, end_time, wrapLon, extendedInfo);

		// Display a few

		System.out.println ();
		System.out.println ("Displaying up to 20 ruptures");
		System.out.println ();

		for (int i = 0; i < Math.min (20, rups.size()); ++i) {
			System.out.println (ComcatOAFAccessor.rupToString (rups.get(i)));
		}

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test3/shadow_test
	// Command line arguments:
	//  start_time  end_time  forecast_lag  max_tests
	// Get the list of earthquakes satisfying intake criteria in the time range.
	// Test each if it is shadowed, up to a maximum of max_tests events.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	// Durations are in java.time.Duration format, for example P3DT11H45M04S or P100D or PT30S.

	public static void test3 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Test of shadowing for earthquakes satisfying intake criteria in the time range");
		long start_time = testargs.get_time ("start_time");
		long end_time = testargs.get_time ("end_time");
		long forecast_lag = testargs.get_duration ("forecast_lag");
		int max_tests = testargs.get_int ("max_tests");

		testargs.end_test();

		// Get list of ruptures satisfying intake

		boolean wrapLon = false;
		boolean extendedInfo = true;

		ObsEqkRupList rups = fetchIntakeEventList (start_time, end_time, wrapLon, extendedInfo);

		// Apply shadow tests

		int count_shadowed = 0;
		int count_not_shadowed = 0;

		System.out.println ();
		System.out.println ("Testing up to " + max_tests + " ruptures");
		System.out.println ();

		for (int i = 0; i < Math.min (max_tests, rups.size()); ++i) {
			System.out.println ();

			String event_id = rups.get(i).getEventId();
			ForecastParameters analyst_params = null;

			boolean f_shadowed = is_event_shadowed (event_id, forecast_lag, analyst_params);

			if (f_shadowed) {
				++count_shadowed;
				System.out.println ("Event " + event_id + " is shadowed");
			} else {
				++count_not_shadowed;
				System.out.println ("Event " + event_id + " is not shadowed");
			}
		}

		// Display final counts

		System.out.println ();
		System.out.println ("Number of events that are shadowed = " + count_shadowed);
		System.out.println ("Number of events that are not shadowed = " + count_not_shadowed);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test4/sequence_count
	// Command line arguments:
	//  start_time  end_time  forecast_lag  min_mag_cat_delta  min_catalog_size  [filename]
	// Get the list of earthquakes satisfying intake criteria in the time range.
	// Count the ones that are not shadowed and meet the specified minimum magnitude
	// above magCat and minimum catalog size.
	// Display count globally and for each tectonic regime.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	// Durations are in java.time.Duration format, for example P3DT11H45M04S or P100D or PT30S.
	// Result is written to file if filename is included and is not "-".

	public static void test4 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Count earthquakes satisfying intake criteria in the time range, not shadowed, satisfying min magnitude and catalog size");
		long start_time = testargs.get_time ("start_time");
		long end_time = testargs.get_time ("end_time");
		long forecast_lag = testargs.get_duration ("forecast_lag");
		double min_mag_cat_delta = testargs.get_double ("min_mag_cat_delta");
		int min_catalog_size = testargs.get_int ("min_catalog_size");
		String filename = testargs.get_string_omit ("filename", null, "-");

		testargs.end_test();

		// Get list of ruptures satisfying intake

		boolean wrapLon = false;
		boolean extendedInfo = true;

		ObsEqkRupList rups = fetchIntakeEventList (start_time, end_time, wrapLon, extendedInfo);

		// Count holder

		boolean f_etas_regime = false;

		SeqAccumCountHolder count_holder = new SeqAccumCountHolder (min_mag_cat_delta, min_catalog_size, f_etas_regime);

		// Loop over ruptures

		int count_shadowed = 0;
		int count_not_shadowed = 0;

		for (int i = 0; i < rups.size(); ++i) {
			System.out.println ();

			// Shadow test

			String event_id = rups.get(i).getEventId();
			ForecastParameters analyst_params = null;

			ForecastMainshock fcmain = new ForecastMainshock();
			ForecastParameters forecast_params = new ForecastParameters();
			ForecastResults forecast_results = new ForecastResults();

			boolean f_shadowed = is_event_shadowed (event_id, forecast_lag, analyst_params, fcmain, forecast_params, forecast_results);

			// If not shadowed, accumulate it

			if (f_shadowed) {
				++count_shadowed;
				System.out.println ("Event " + event_id + " is shadowed");
			} else {
				++count_not_shadowed;
				System.out.println ("Event " + event_id + " is not shadowed");
				count_holder.seq_accum (fcmain, forecast_params, forecast_results);
			}
		}

		// Display final counts

		System.out.println ();
		System.out.println ("Number of events that are shadowed = " + count_shadowed);
		System.out.println ("Number of events that are not shadowed = " + count_not_shadowed);

		System.out.println ();
		System.out.println (count_holder.count_string());

		// Write to file if desired

		if (filename != null) {
			System.out.println ();
			System.out.println ("Writing to file: " + filename);
			MarshalUtils.to_formatted_compact_json_file (count_holder, filename);
		}

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test5/sequence_etas_count
	// Command line arguments:
	//  start_time  end_time  forecast_lag  min_mag_cat_delta  min_catalog_size  [filename]
	// Get the list of earthquakes satisfying intake criteria in the time range.
	// Count the ones that are not shadowed and meet the specified minimum magnitude
	// above magCat and minimum catalog size.
	// Display count globally and for each tectonic regime.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	// Durations are in java.time.Duration format, for example P3DT11H45M04S or P100D or PT30S.
	// Result is written to file if filename is included and is not "-".
	// Same as test4, except forces ETAS to be enabled and uses ETAS regimes.

	public static void test5 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Count earthquakes satisfying intake criteria in the time range, not shadowed, satisfying min magnitude and catalog size");
		long start_time = testargs.get_time ("start_time");
		long end_time = testargs.get_time ("end_time");
		long forecast_lag = testargs.get_duration ("forecast_lag");
		double min_mag_cat_delta = testargs.get_double ("min_mag_cat_delta");
		int min_catalog_size = testargs.get_int ("min_catalog_size");
		String filename = testargs.get_string_omit ("filename", null, "-");

		testargs.end_test();

		// Get list of ruptures satisfying intake

		boolean wrapLon = false;
		boolean extendedInfo = true;

		ObsEqkRupList rups = fetchIntakeEventList (start_time, end_time, wrapLon, extendedInfo);

		// Count holder

		boolean f_etas_regime = true;

		SeqAccumCountHolder count_holder = new SeqAccumCountHolder (min_mag_cat_delta, min_catalog_size, f_etas_regime);

		// Loop over ruptures

		int count_shadowed = 0;
		int count_not_shadowed = 0;

		for (int i = 0; i < rups.size(); ++i) {
			System.out.println ();

			// Shadow test

			String event_id = rups.get(i).getEventId();
			ForecastParameters analyst_params = null;

			ForecastMainshock fcmain = new ForecastMainshock();
			ForecastParameters forecast_params = new ForecastParameters();
			ForecastResults forecast_results = new ForecastResults();

			boolean f_shadowed = is_event_shadowed (event_id, forecast_lag, analyst_params, fcmain, forecast_params, forecast_results);

			// If not shadowed, accumulate it

			if (f_shadowed) {
				++count_shadowed;
				System.out.println ("Event " + event_id + " is shadowed");
			} else {
				++count_not_shadowed;
				System.out.println ("Event " + event_id + " is not shadowed");
				count_holder.seq_accum (fcmain, forecast_params, forecast_results);
			}
		}

		// Display final counts

		System.out.println ();
		System.out.println ("Number of events that are shadowed = " + count_shadowed);
		System.out.println ("Number of events that are not shadowed = " + count_not_shadowed);

		System.out.println ();
		System.out.println (count_holder.count_string());

		// Write to file if desired

		if (filename != null) {
			System.out.println ();
			System.out.println ("Writing to file: " + filename);
			MarshalUtils.to_formatted_compact_json_file (count_holder, filename);
		}

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test6/sequence_stack
	// Command line arguments:
	//  start_time  end_time  forecast_lag  min_mag_cat_delta  min_catalog_size  min_fitting_count  f_etas_regime  remap_option  [filename]
	// Get the list of earthquakes satisfying intake criteria in the time range.
	// Generate ETAS forecassts for the ones that are not shadowed and meet the specified minimum
	// magnitude above magCat, minimum catalog size, and minimum number of ruptures used for fitting.
	// Stack the resulting univariate and bivariate marginal distributions.
	// Display count globally and for each tectonic regime.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
	// Durations are in java.time.Duration format, for example P3DT11H45M04S or P100D or PT30S.
	// Result is written to file if filename is included and is not "-".

	public static void test6 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Stack marginal distributions for earthquakes satisfying intake criteria in the time range, not shadowed, satisfying min magnitude and catalog size");
		long start_time = testargs.get_time ("start_time");
		long end_time = testargs.get_time ("end_time");
		long forecast_lag = testargs.get_duration ("forecast_lag");
		double min_mag_cat_delta = testargs.get_double ("min_mag_cat_delta");
		int min_catalog_size = testargs.get_int ("min_catalog_size");
		int min_fitting_count = testargs.get_int ("min_fitting_count");
		boolean f_etas_regime = testargs.get_boolean ("f_etas_regime");
		int remap_option = testargs.get_int ("remap_option");
		String filename = testargs.get_string_omit ("filename", null, "-");

		testargs.end_test();

		// Make regime name remapping according to selected option

		Map<String, String> regime_remap = new HashMap<String, String>();

		switch (remap_option) {
		default:
			throw new IllegalArgumentException ("OEBayTest.test5: Invalid remapping option: remap_option = " + remap_option);

		// Remapping option 0: No remapping

		case 0:
			break;
		}

		// Get list of ruptures satisfying intake

		boolean wrapLon = false;
		boolean extendedInfo = true;

		ObsEqkRupList rups = fetchIntakeEventList (start_time, end_time, wrapLon, extendedInfo);

		// Stack holder

		SeqAccumStackDistHolder stack_holder = new SeqAccumStackDistHolder (min_mag_cat_delta, min_catalog_size, min_fitting_count, f_etas_regime, regime_remap);

		// Loop over ruptures

		int count_shadowed = 0;
		int count_not_shadowed = 0;

		for (int i = 0; i < rups.size(); ++i) {
			System.out.println ();

			// Shadow test

			String event_id = rups.get(i).getEventId();
			ForecastParameters analyst_params = null;

			ForecastMainshock fcmain = new ForecastMainshock();
			ForecastParameters forecast_params = new ForecastParameters();
			ForecastResults forecast_results = new ForecastResults();

			boolean f_shadowed = is_event_shadowed (event_id, forecast_lag, analyst_params, fcmain, forecast_params, forecast_results);

			// If not shadowed, accumulate it

			if (f_shadowed) {
				++count_shadowed;
				System.out.println ("Event " + event_id + " is shadowed");
			} else {
				++count_not_shadowed;
				System.out.println ("Event " + event_id + " is not shadowed");
				stack_holder.seq_accum (fcmain, forecast_params, forecast_results);
			}
		}

		// Finish accumulation

		stack_holder.end_accum();

		// Display final counts

		System.out.println ();
		System.out.println ("Number of events that are shadowed = " + count_shadowed);
		System.out.println ("Number of events that are not shadowed = " + count_not_shadowed);

		System.out.println ();
		System.out.println (stack_holder.count_string());

		// Write to file if desired

		if (filename != null) {
			System.out.println ();
			System.out.println ("Writing to file: " + filename);
			MarshalUtils.to_formatted_compact_json_file (stack_holder, filename);
		}

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test7/write_stack_tables
	// Command line arguments:
	//  filename  fn_prefix  fn_suffix
	// Read a set of stacks as generated by the sequence_stack command, and write it out
	// as a set of table files.
	// Filenames are constructed using the specified prefix and suffix, and incorporate
	// the names of regimes, variables, and data to create many different filenames.

	public static void test7 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Read stacked marginal distributions and write them out as a set of table files");
		String filename = testargs.get_string ("filename");
		String fn_prefix = testargs.get_string ("fn_prefix");
		String fn_suffix = testargs.get_string ("fn_suffix");

		testargs.end_test();

		// Stack holder

		SeqAccumStackDistHolder stack_holder = new SeqAccumStackDistHolder ();

		// Read it

		System.out.println ();
		System.out.println ("Reading file: " + filename);

		MarshalUtils.from_json_file (stack_holder, filename);

		// Write the table files

		System.out.println ();
		System.out.println ("Writing table files: " + fn_prefix + "..." + fn_suffix);

		stack_holder.write_table_files (fn_prefix, fn_suffix);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEBayTest");




		if (testargs.is_test ("test1", "hello_world")) {
			test1 (testargs);
			return;
		}


		if (testargs.is_test ("test2", "intake_test")) {
			test2 (testargs);
			return;
		}


		if (testargs.is_test ("test3", "shadow_test")) {
			test3 (testargs);
			return;
		}


		if (testargs.is_test ("test4", "sequence_count")) {
			test4 (testargs);
			return;
		}


		if (testargs.is_test ("test5", "sequence_etas_count")) {
			test5 (testargs);
			return;
		}


		if (testargs.is_test ("test6", "sequence_stack")) {
			test6 (testargs);
			return;
		}


		if (testargs.is_test ("test7", "write_stack_tables")) {
			test7 (testargs);
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
