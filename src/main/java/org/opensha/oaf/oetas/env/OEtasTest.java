package org.opensha.oaf.oetas.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;

import org.opensha.oaf.util.gui.GUIExternalCatalog;

import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEExaminerSaveList;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OERandomGenerator;
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

import org.opensha.oaf.oetas.fit.OEGridParams;
import org.opensha.oaf.oetas.fit.OEGridOptions;

import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEMarginalDistSet;
import org.opensha.oaf.oetas.util.OEMarginalDistSetBuilder;
import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.rj.AftershockStatsCalc;
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
import org.opensha.oaf.aafs.VersionInfo;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
//import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatVisitor;


// Test functions for operational ETAS.
// Author: Michael Barall 05/04/2022.

public class OEtasTest {




	//----- Test subroutines -----




	// Get a filename argument, return null if the arguments is empty or "-".

	public static String get_filename_arg (TestArgs testargs, String name) {
		String filename = testargs.get_string (name);
		if (filename == null || filename.equals ("") || filename.equals ("-")) {
			return null;
		}
		return filename;
	}




	// Get a sstring argument, return the default value if the arguments is empty or "-".

	public static String get_omit_string_arg (TestArgs testargs, String name, String defval) {
		String s = testargs.get_string (name);
		if (s == null || s.equals ("") || s.equals ("-")) {
			return defval;
		}
		return s;
	}



	// Convert regime to string, including null.

	public static String regime_to_string (OAFTectonicRegime regime) {
		if (regime == null) {
			return "<null>";
		}
		return regime.toString();
	}




	// Fetch mainshock, and display its information.
	// If f_extended_info is true, then display additional info.
	// Throws exception if not found, or error accessing Comcat.

	public static ObsEqkRupture fetch_mainshock (String event_id, boolean f_extended_info) {

		// Say hello

		System.out.println ("Fetching event: " + event_id);

		// Create the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Get the rupture

		ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

		// Display its information

		if (rup == null) {
			throw new ComcatException ("Comcat is unable to find event: " + event_id);
		}

		System.out.println ();
		System.out.println (ComcatOAFAccessor.rupToString (rup));

		if (f_extended_info) {

			ActionConfig action_config = new ActionConfig();

			// Display tectonic regime

			Location hypo = rup.getHypocenterLocation();
		
			GenericRJ_ParametersFetch generic_fetch = new GenericRJ_ParametersFetch();
			OAFTectonicRegime generic_regime = generic_fetch.getRegion (hypo);

			//System.out.println ();
			System.out.println ("Regimes:");
			System.out.println ("Tectonic regime: " + generic_regime.toString());

			// Display magnitude of completeness regime
		
			MagCompPage_ParametersFetch mag_comp_fetch = new MagCompPage_ParametersFetch();
			OAFTectonicRegime mag_comp_regime = mag_comp_fetch.getRegion (hypo);

			System.out.println ("Magnitude of completeness regime: " + mag_comp_regime.toString());

			// Display ETAS parameter regime

			OAFRegimeParams<OEtasParameters> etas_x = (new OEtasConfig()).get_resolved_params (hypo, null);
			System.out.println ("ETAS parameter regime: " + regime_to_string (etas_x.regime));

			// Display Gaussian prior regime

			OAFRegimeParams<OEGaussAPCParams> gapc_x = (new OEGaussAPCConfig()).get_params (hypo);
			System.out.println ("Gaussian prior regime: " + gapc_x.params.get_regimeName());

			// Display magnitude of completeness info

			System.out.println ();
			System.out.println ("Magnitude info:");

			MagCompPage_Parameters mag_comp_params = get_mag_comp_params (rup);

			System.out.println ("MagCat = " + mag_comp_params.get_magCat ());
			System.out.println ("MagCat (adjusted) = " + mag_comp_params.get_magCat (rup.getMag()));

			if (mag_comp_params.get_magCompFn().is_page_or_constant()) {
				System.out.println ("F = " + mag_comp_params.get_magCompFn().getDefaultGUICapF());
				System.out.println ("G = " + mag_comp_params.get_magCompFn().getDefaultGUICapG());
				System.out.println ("H = " + mag_comp_params.get_magCompFn().getDefaultGUICapH());
			}

			double the_mag_cat = mag_comp_params.get_magCat (rup.getMag());
			double the_mag_top = etas_x.params.suggest_mag_top (the_mag_cat, rup.getMag());
			System.out.println ("MagTop = " + the_mag_top);

			// Display catalog search info

			System.out.println ();
			System.out.println ("Search info:");

			long the_start_lag = -(action_config.get_data_fetch_lookback());
			double min_days = SimpleUtils.round_double_via_string ("%.5f", SimpleUtils.millis_to_days (the_start_lag));
			System.out.println ("min_days = " + min_days);

			long the_end_lag = System.currentTimeMillis() - (rup.getOriginTime() + action_config.get_comcat_clock_skew() + action_config.get_comcat_origin_skew());
			the_end_lag = SimpleUtils.clip_min_max_l (SimpleUtils.MINUTE_MILLIS, SimpleUtils.YEAR_MILLIS * 11L, the_end_lag);	// clip to 11 years
			double max_days = SimpleUtils.round_double_via_string ("%.5f", SimpleUtils.millis_to_days (the_end_lag));
			System.out.println ("max_days = " + max_days);

			double sample_radius = mag_comp_params.get_radiusSample (rup.getMag());
			double radius_km = SimpleUtils.round_double_via_string ("%.3f", sample_radius);
			System.out.println ("radius_km = " + radius_km);

			double sample_min_mag = mag_comp_params.get_magSample (rup.getMag());
			double min_mag = SimpleUtils.round_double_via_string ("%.3f", sample_min_mag);
			System.out.println ("min_mag = " + min_mag);
		}

		return rup;
	}




	// Fetch aftershocks, using the centroid algorithm.
	// Parameters:
	//  obs_main = Mainshock.
	//  min_days = Start of time range used for sampling aftershocks, in days since the mainshock.  Can be < 0.0 to include foreshocks.
	//  max_days = End of time range used for sampling aftershocks, in days since the mainshock.
	//  radius_km = Search radius in km, can be 0.0 to use Wells and Coppersmith radius.
	//  min_mag = Minimum magnitude to use for search, or -10.0 for no minimum.
	// Throws exception if error.

	public static ObsEqkRupList fetch_aftershocks (ObsEqkRupture obs_main, double min_days, double max_days, double radius_km, double min_mag) {

		// Mainshock information

		String rup_event_id = obs_main.getEventId();
		long rup_time = obs_main.getOriginTime();
		double rup_mag = obs_main.getMag();
		Location hypo = obs_main.getHypocenterLocation();

		// Say hello

		System.out.println ("Fetching aftershocks for event: " + rup_event_id);

		// Get minimum magnitude and radius parameters

		double sample_min_mag = min_mag;
		double sample_radius = radius_km;

		double centroid_min_mag = min_mag;
		double centroid_radius = radius_km;

		// Zero radius means to use Wells and Coppersmith, in range 10 to 2000 km

		if (radius_km < 0.001) {
			SearchRadiusFn search_radius_fn = SearchRadiusFn.makeWCClip (1.0, 10.0, 2000.0);
			sample_radius = search_radius_fn.getRadius (rup_mag);
			centroid_radius = search_radius_fn.getRadius (rup_mag);
		}

		System.out.println ("Search radius in km = " + sample_radius);

		// Depth range used for sampling aftershocks, in kilometers

		double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		// Retrieve list of aftershocks in the initial region

		ObsEqkRupList aftershocks;

		if (centroid_min_mag > SearchMagFn.SKIP_CENTROID_TEST) {	// 9.9
			aftershocks = new ObsEqkRupList();
		} else {

			// The initial region is a circle centered at the epicenter

			SphRegion initial_region = SphRegion.makeCircle (new SphLatLon(hypo), centroid_radius);

			// Time range used for centroid

			double centroid_min_days = Math.max (min_days, 0.0);
			double centroid_max_days = max_days;

			// Call Comcat for centroid calculation

			try {
				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				aftershocks = accessor.fetchAftershocks (obs_main, centroid_min_days, centroid_max_days, min_depth, max_depth, initial_region, initial_region.getPlotWrap(), centroid_min_mag);
			} catch (Exception e) {
				throw new RuntimeException ("Comcat exception while performing centroid calculation for event: " + rup_event_id, e);
			}
		}

		System.out.println ("Fetched " + aftershocks.size() + " aftershocks in initial search region for event: " + rup_event_id);

		// Center of search region

		Location centroid;

		// If no aftershocks, use the hypocenter location

		if (aftershocks.isEmpty()) {
			centroid = hypo;
		}

		// Otherwise, use the centroid of the aftershocks

		else {
			centroid = AftershockStatsCalc.getSphCentroid (obs_main, aftershocks);
		}

		// Search region is a circle centered at the centroid (or hypocenter if no aftershocks)
			
		SphRegion aftershock_search_region = SphRegion.makeCircle (new SphLatLon(centroid), sample_radius);

		// Time range used for sample

		double sample_min_days = min_days;
		double sample_max_days = max_days;

		// Retrieve list of aftershocks in the search region

		ObsEqkRupList catalog_comcat_aftershocks;

		try {
			ComcatOAFAccessor accessor = new ComcatOAFAccessor();
			catalog_comcat_aftershocks = accessor.fetchAftershocks (obs_main, sample_min_days, sample_max_days,
				min_depth, max_depth, aftershock_search_region, false, sample_min_mag);
		} catch (Exception e) {
			throw new RuntimeException ("Comcat exception while fetching aftershocks for event: " + rup_event_id, e);
		}

		// Sort the aftershocks in order of increasing time

		if (catalog_comcat_aftershocks.size() > 1) {
			GUIExternalCatalog.sort_aftershocks (catalog_comcat_aftershocks);
		}

		// Say goodbye

		System.out.println ("Fetched " + catalog_comcat_aftershocks.size() + " aftershocks for event: " + rup_event_id);

		return catalog_comcat_aftershocks;
	}




	// Get the magnitude of completeness parameters for a location.

	public static MagCompPage_Parameters get_mag_comp_params (Location loc) {
		MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
		OAFTectonicRegime regime = fetch.getRegion (loc);
		return fetch.get (regime);
	}




	// Get the magnitude of completeness parameters for a rupture.

	public static MagCompPage_Parameters get_mag_comp_params (ObsEqkRupture rup) {
		return get_mag_comp_params (rup.getHypocenterLocation());
	}




	// Get the ETAS parameters for a location.
	// A null location returns default parameters.

	public static OEtasParameters get_etas_params (Location loc) {
		OAFRegimeParams<OEtasParameters> x = (new OEtasConfig()).get_resolved_params (loc, null);
		return x.params;
	}




	// Get the ETAS parameters for a rupture.

	public static OEtasParameters get_etas_params (ObsEqkRupture rup) {
		return get_etas_params (rup.getHypocenterLocation());
	}




	// Make catalog information, given a catalog.
	// Parameters:
	//  mainshock = The mainshock.
	//  aftershock = List of aftershocks (and foreshocks), should not include the mainshock.
	//  forecast_lag = Time after mainshock at which data ends, in milliseconds.
	//  f_gap = True to insert a gap after end of data so forecast begins at a "round" time.

	public static OEtasCatalogInfo make_cat_info_for_cat (
		ObsEqkRupture mainshock,
		List<ObsEqkRupture> aftershocks,
		long forecast_lag,
		boolean f_gap
	) {
		// Configuration file

		ActionConfig action_config = new ActionConfig();

		// Mainshock parameters

		long mainshock_time = mainshock.getOriginTime();
		double mainshock_mag = mainshock.getMag();
		Location hypo = mainshock.getHypocenterLocation();
		double mainshock_lat = hypo.getLatitude();
		double mainshock_lon = hypo.getLongitude();
		double mainshock_depth = hypo.getDepth();

		// Forecast information

		long result_time = mainshock_time + forecast_lag;
		long advisory_lag = ForecastResults.forecast_lag_to_advisory_lag (forecast_lag, action_config);
		String injectable_text = "";
		long next_scheduled_lag = 0L;
		
		USGS_ForecastInfo fc_info = (new USGS_ForecastInfo()).set_typical (
			mainshock_time,					// event_time
			result_time,					// result_time
			advisory_lag,					// advisory_lag
			injectable_text,				// injectable_text
			next_scheduled_lag,				// next_scheduled_lag
			null,							// user_param_map
			null							// min_mag_bins
		);

		// Magnitude of completeness parameters

		MagCompPage_Parameters mag_comp_params = get_mag_comp_params (hypo);

		// We need to have magnitude of completeness parameters with Helmstetter function

		if (!( mag_comp_params.get_magCompFn().is_page_or_constant() )) {
			throw new IllegalArgumentException ("OEtasTest.make_cat_info_for_cat: Magnitude of completeness parameters are not Helmstetter parameters");
		}

		// ETAS parameters

		OEtasParameters etas_params = get_etas_params (hypo);

		// Get catalog maximum magnitude and minimum time

		double catalog_max_mag = mainshock_mag;
		long catalog_min_time = mainshock_time;

		for (ObsEqkRupture rup : aftershocks) {
			catalog_max_mag = Math.max (catalog_max_mag, rup.getMag());
			catalog_min_time = Math.min (catalog_min_time, rup.getOriginTime());
		}

		// Infer data fetch min and max times

		double min_days = (catalog_min_time - mainshock_time) - SimpleUtils.DAY_MILLIS;	// back up one day
		double max_days = SimpleUtils.millis_to_days (forecast_lag);

		// Catalog information
		// Note: By comparison to the code in ForecastResults, this is special-cased by
		// assuing fit_start_inset == 0.0 and fit_end_inset == 0.0; this results in using
		// the default values for get_catalog_fit_end_days and get_catalog_fit_start_days.

		double the_mag_cat = mag_comp_params.get_magCat (mainshock_mag);
		double the_mag_top = etas_params.suggest_mag_top (the_mag_cat, catalog_max_mag);

		double t_data_begin = min_days;
		double t_data_end = max_days;
		double t_fitting = Math.max(0.0, min_days);
		double t_forecast = t_data_end;
		if (f_gap) {
			t_forecast = Math.max(t_data_end, SimpleUtils.millis_to_days (fc_info.start_time - mainshock_time));
		}

		OEtasCatalogInfo catalog_info = (new OEtasCatalogInfo()).set (
			the_mag_cat,												// double magCat
			the_mag_top,												// double magTop
			mag_comp_params.get_magCompFn().getDefaultGUICapF(),		// double capF
			mag_comp_params.get_magCompFn().getDefaultGUICapG(),		// double capG
			mag_comp_params.get_magCompFn().getDefaultGUICapH(),		// double capH
			t_data_begin,												// double t_data_begin
			t_data_end,													// double t_data_end
			t_fitting,													// double t_fitting
			t_forecast,													// double t_forecast
			mainshock_mag,												// double mag_main
			mainshock_lat,												// double lat_main
			mainshock_lon,												// double lon_main
			mainshock_depth												// double depth_main
		);

		return catalog_info;
	}




	// Make the marginal distribution for a Bayesian prior.
	// Parameters:
	//  bay_prior = Bayesian prior.
	//  bay_params = Parameters for Bayesian prior.
	//  grid_params = Specifies parameter ranges.
	//  f_bivar_marg = True to include bivariate marginals.
	// Returns the marginals of the Bayesian prior.
	// Note: Creates marginals for two data values, "by_bin" does not
	// make use of voxel volumes, and "by_volume" uses voxel volumes.

	public static OEMarginalDistSet make_marginal_for_prior (
		OEBayPrior bay_prior,
		OEBayPriorParams bay_params,
		OEGridParams grid_params,
		boolean f_bivar_marg
	) {
		// Set up the marginal distribution builder

		OEMarginalDistSetBuilder dist_set_builder = new OEMarginalDistSetBuilder();
		dist_set_builder.add_etas_vars (grid_params, false);
		int ix_density = dist_set_builder.add_data ("density");
		int ix_prob = dist_set_builder.add_data ("probability");
		dist_set_builder.begin_accum (f_bivar_marg);

		// Get volume element arrays for each variable, noting that alpha_velt_array and zmu_velt_array can be null.

		OEValueElement[] b_velt_array = grid_params.b_range.get_velt_array();
		int b_velt_count = b_velt_array.length;

		OEValueElement[] alpha_velt_array = new OEValueElement[1];
		alpha_velt_array[0] = null;
		if (grid_params.alpha_range != null) {
			alpha_velt_array = grid_params.alpha_range.get_velt_array();
		}
		int alpha_velt_count = alpha_velt_array.length;

		OEValueElement[] c_velt_array = grid_params.c_range.get_velt_array();
		int c_velt_count = c_velt_array.length;

		OEValueElement[] p_velt_array = grid_params.p_range.get_velt_array();
		int p_velt_count = p_velt_array.length;

		OEValueElement[] n_velt_array = grid_params.n_range.get_velt_array();
		int n_velt_count = n_velt_array.length;

		OEValueElement[] zams_velt_array = grid_params.zams_range.get_velt_array();
		int zams_velt_count = zams_velt_array.length;

		OEValueElement[] zmu_velt_array = new OEValueElement[1];
		zmu_velt_array[0] = null;
		if (grid_params.zmu_range != null) {
			zmu_velt_array = grid_params.zmu_range.get_velt_array();
		}
		int zmu_velt_count = zmu_velt_array.length;

		// Loop over all voxels to get the maximum log density

		double max_log_density = -Double.MAX_VALUE;

		OEBayPriorValue bay_value = new OEBayPriorValue();

		for (int b_ix = 0; b_ix < b_velt_count; ++b_ix) {
			final OEValueElement b_velt = b_velt_array[b_ix];

			for (int alpha_ix = 0; alpha_ix < alpha_velt_count; ++alpha_ix) {
				final OEValueElement alpha_velt = alpha_velt_array[alpha_ix];

				for (int c_ix = 0; c_ix < c_velt_count; ++c_ix) {
					final OEValueElement c_velt = c_velt_array[c_ix];

					for (int p_ix = 0; p_ix < p_velt_count; ++p_ix) {
						final OEValueElement p_velt = p_velt_array[p_ix];

						for (int n_ix = 0; n_ix < n_velt_count; ++n_ix) {
							final OEValueElement n_velt = n_velt_array[n_ix];

							for (int zams_ix = 0; zams_ix < zams_velt_count; ++zams_ix) {
								final OEValueElement zams_velt = zams_velt_array[zams_ix];

								for (int zmu_ix = 0; zmu_ix < zmu_velt_count; ++zmu_ix) {
									final OEValueElement zmu_velt = zmu_velt_array[zmu_ix];

									bay_prior.get_bay_value (
										bay_params,
										bay_value,
										b_velt,
										alpha_velt,
										c_velt,
										p_velt,
										n_velt,
										zams_velt,
										zmu_velt
									);

									max_log_density = Math.max (max_log_density, bay_value.log_density);
								}
							}
						}
					}
				}
			}
		}

		// Loop ovar all voxels to accumulate the marginals

		for (int b_ix = 0; b_ix < b_velt_count; ++b_ix) {
			final OEValueElement b_velt = b_velt_array[b_ix];
			final double b = b_velt.get_ve_value();

			for (int alpha_ix = 0; alpha_ix < alpha_velt_count; ++alpha_ix) {
				final OEValueElement alpha_velt = alpha_velt_array[alpha_ix];
				final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

				for (int c_ix = 0; c_ix < c_velt_count; ++c_ix) {
					final OEValueElement c_velt = c_velt_array[c_ix];
					final double c = c_velt.get_ve_value();

					for (int p_ix = 0; p_ix < p_velt_count; ++p_ix) {
						final OEValueElement p_velt = p_velt_array[p_ix];
						final double p = p_velt.get_ve_value();

						for (int n_ix = 0; n_ix < n_velt_count; ++n_ix) {
							final OEValueElement n_velt = n_velt_array[n_ix];
							final double n = n_velt.get_ve_value();

							dist_set_builder.set_etas_var_b_alpha_c_p_n (
								b,
								alpha,
								c,
								p,
								n
							);

							for (int zams_ix = 0; zams_ix < zams_velt_count; ++zams_ix) {
								final OEValueElement zams_velt = zams_velt_array[zams_ix];
								final double zams = zams_velt.get_ve_value();

								for (int zmu_ix = 0; zmu_ix < zmu_velt_count; ++zmu_ix) {
									final OEValueElement zmu_velt = zmu_velt_array[zmu_ix];
									final double zmu = ((zmu_velt == null) ? 0.0 : zmu_velt.get_ve_value());

									dist_set_builder.set_etas_var_zams_zmu (
										zams,
										zmu
									);

									bay_prior.get_bay_value (
										bay_params,
										bay_value,
										b_velt,
										alpha_velt,
										c_velt,
										p_velt,
										n_velt,
										zams_velt,
										zmu_velt
									);

									final double density = Math.exp (bay_value.log_density - max_log_density);

									dist_set_builder.set_data (ix_density, density);
									dist_set_builder.set_data (ix_prob, density * bay_value.vox_volume);

									dist_set_builder.accum();
								}
							}
						}
					}
				}
			}
		}

		// Return the marginal distribution

		OEMarginalDistSet dist_set = dist_set_builder.end_etas_accum();
		return dist_set;
	}




	// A structure that holds the inputs needed from observed data.
	// See setup_input_area_from_obs() and setup_input_area_from_compact() for parameters,
	// noting that CompactEqkRupList is a subclass of List<ObsEqkRupture>.

	public static class ObsInputs {
		public OEtasParameters etas_params;
		public USGS_ForecastInfo forecast_info;
		public OEtasCatalogInfo catalog_info;
		public ObsEqkRupture obs_mainshock;
		public CompactEqkRupList the_rup_list;

		public ObsInputs (
			OEtasParameters etas_params,
			USGS_ForecastInfo forecast_info,
			OEtasCatalogInfo catalog_info,
			ObsEqkRupture obs_mainshock,
			CompactEqkRupList the_rup_list
		) {
			this.etas_params   = etas_params;
			this.forecast_info = forecast_info;
			this.catalog_info  = catalog_info;
			this.obs_mainshock = obs_mainshock;
			this.the_rup_list  = the_rup_list;
		}
	}




	// Get observed inputs for a given mainshock and forecast lag.
	// Parameters:
	//  event_id = Mainshock event ID.
	//  next_forecast_lag = The forecast lag, in milliseconds since the mainshock.
	//  analyst_params = Analyst parameters, or null if none.
	// Returns the same inputs that the automatic system would use.
	// Returns null or throws an exception if inputs cannot be obtained.
	// Note: Much of the code is copied from ExGenerateForecast.java.

	public static ObsInputs get_obs_inputs (String event_id, long next_forecast_lag, ForecastParameters analyst_params) {

		// Get action parameters and enable ETAS

		ActionConfig action_config = new ActionConfig();

		action_config.get_action_config_file().etas_enable = ActionConfigFile.ETAS_ENA_ENABLE;

		// Get mainshock parameters

		ForecastMainshock fcmain = new ForecastMainshock();

		fcmain.setup_mainshock_only (event_id);		// throws exception if not found or other error

		System.out.println ();
		System.out.println (fcmain.toString());

		// Fetch parameters (model and search parameters), and calculate catalog
		// Note: This section of code comes from ExGenerateForecast.java,
		// calling calc_catalog_only() instead of calc_all().

		ForecastParameters forecast_params = new ForecastParameters();
		ForecastResults forecast_results = new ForecastResults();

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

		// Read out the forecast and catalog info

		ForecastResults.ForecastAndCatalogInfo fc_and_catalog_info = forecast_results.calc_etas_catalog_info (fcmain, forecast_params);

		if (fc_and_catalog_info == null) {
			throw new IllegalStateException ("OEtasTest.get_obs_inputs: Unable to obtain forecast and catalog information");
		}

		// Collect the inputs

		ObsInputs obs_inputs = new ObsInputs (
			forecast_params.etas_params,			// OEtasParameters etas_params,
			fc_and_catalog_info.fc_info,			// USGS_ForecastInfo forecast_info,
			fc_and_catalog_info.catalog_info,		// OEtasCatalogInfo catalog_info,
			fcmain.get_eqk_rupture(),				// ObsEqkRupture obs_mainshock,
			forecast_results.catalog_aftershocks	// CompactEqkRupList the_rup_list
		);

		return obs_inputs;
	}




	//----- Test functions -----




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




	// test2/write_sample_params
	// Command line arguments:
	//  filename
	// Write sample ETAS parameters to a file.
	// This command sets a uniform Bayesian prior.

	public static void test2 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing sample ETAS parameters to a file, uniform prior");
		String filename = testargs.get_string ("filename");
		testargs.end_test();

		// Create the parameters

		OEtasParameters etas_params = new OEtasParameters();
		etas_params.set_to_typical_ov1 ();
		etas_params.set_bay_prior_to_analyst (true, OEBayFactory.makeUniform());

		// Write to file

		MarshalUtils.to_formatted_json_file (etas_params, filename);

		System.out.println ();
		System.out.println ("Wrote sample parameters to file: " + filename);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test14/write_sample_params_2
	// Command line arguments:
	//  filename
	// Write sample ETAS parameters to a file.
	// This command sets a Gaussian a/p/c Bayesian prior.

	public static void test14 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing sample ETAS parameters to a file, Gaussian prior");
		String filename = testargs.get_string ("filename");
		testargs.end_test();

		// Create the parameters

		OEtasParameters etas_params = new OEtasParameters();
		etas_params.set_to_typical_ov1 ();
		etas_params.set_bay_prior_to_analyst (true, OEBayFactory.makeGaussAPC());

		// Write to file

		MarshalUtils.to_formatted_json_file (etas_params, filename);

		System.out.println ();
		System.out.println ("Wrote sample parameters to file: " + filename);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test20/write_sample_params_3
	// Command line arguments:
	//  filename
	// Write sample ETAS parameters to a file.
	// This command sets a mixed relative-ams/n/p/c Bayesian prior.

	public static void test20 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing sample ETAS parameters to a file, mixed relative-ams/n/p/c prior");
		String filename = testargs.get_string ("filename");
		testargs.end_test();

		// Create the parameters

		OEtasParameters etas_params = new OEtasParameters();
		etas_params.set_to_typical_cv ();
		etas_params.set_bay_prior_to_analyst (true, OEBayFactory.makeMixedRNPC());

		// Write to file

		MarshalUtils.to_formatted_json_file (etas_params, filename);

		System.out.println ();
		System.out.println ("Wrote sample parameters to file: " + filename);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test3
	// Command line arguments:
	//  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend  tint_br  mag_min_sim  mag_max_sim
	//  [t_day  rup_mag]...
	// Generate a simulated ETAS catalog, and display information about it.

	public static void test3 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate simulated ETAS catalog and display info");
		double zmu = testargs.get_double ("zmu");
		double zams = testargs.get_double ("zams");
		double n = testargs.get_double ("n");
		double p = testargs.get_double ("p");
		double c = testargs.get_double ("c");
		double b = testargs.get_double ("b");
		double alpha = testargs.get_double ("alpha");
		double mref = testargs.get_double ("mref");
		double msup = testargs.get_double ("msup");
		double tbegin = testargs.get_double ("tbegin");
		double tend = testargs.get_double ("tend");
		double tint_br = testargs.get_double ("tint_br");
		double mag_min_sim = testargs.get_double ("mag_min_sim");
		double mag_max_sim = testargs.get_double ("mag_max_sim");

		double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
		testargs.end_test();

		// Make the catalog parameters

		OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
			n,				// n
			p,				// p
			c,				// c
			b,				// b
			alpha,			// alpha
			mref,			// mref
			msup,			// msup
			tbegin,			// tbegin
			tend,			// tend
			tint_br,		// tint_br
			mag_min_sim,	// mag_min_sim
			mag_max_sim		// mag_max_sim
		);

		// Make the seed parameters

		OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

		// Make the catalog initializer

		OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

		// Make the catalog examiner

		ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
		OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

		// Generate a catalog

		OESimulator.gen_single_catalog (initializer, examiner);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test15
	// Command line arguments:
	//  zmu  rel_ams  n  p  c  b  alpha  mref  msup  tbegin  tend  tint_br  mag_min_sim  mag_max_sim
	//  [t_day  rup_mag]...
	// Generate a simulated ETAS catalog, and display information about it.
	// The value of ams is specified relative to the a-value.

	public static void test15 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate simulated ETAS catalog and display info");
		double zmu = testargs.get_double ("zmu");
		double rel_ams = testargs.get_double ("rel_ams");
		double n = testargs.get_double ("n");
		double p = testargs.get_double ("p");
		double c = testargs.get_double ("c");
		double b = testargs.get_double ("b");
		double alpha = testargs.get_double ("alpha");
		double mref = testargs.get_double ("mref");
		double msup = testargs.get_double ("msup");
		double tbegin = testargs.get_double ("tbegin");
		double tend = testargs.get_double ("tend");
		double tint_br = testargs.get_double ("tint_br");
		double mag_min_sim = testargs.get_double ("mag_min_sim");
		double mag_max_sim = testargs.get_double ("mag_max_sim");

		double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
		testargs.end_test();

		// Compute zams for the given branch ratio

		double zams = rel_ams + OEStatsCalc.calc_zams_from_br (
			n,
			p,
			c,
			b,
			alpha,
			mag_min_sim,
			mag_max_sim,
			tint_br
		);

		System.out.println ();
		System.out.println ("Equivalent zams = " + zams);

		// Make the catalog parameters

		OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
			n,				// n
			p,				// p
			c,				// c
			b,				// b
			alpha,			// alpha
			mref,			// mref
			msup,			// msup
			tbegin,			// tbegin
			tend,			// tend
			tint_br,		// tint_br
			mag_min_sim,	// mag_min_sim
			mag_max_sim		// mag_max_sim
		);

		// Make the seed parameters

		OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

		// Make the catalog initializer

		OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

		// Make the catalog examiner

		ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
		OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

		// Generate a catalog

		OESimulator.gen_single_catalog (initializer, examiner);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test4/write_sim_cat
	// Command line arguments:
	//  filename  basetime  zmu  zams  n  p  c  b  alpha  mag_min  mag_max  tbegin  tend  tint_br
	//  [t_day  rup_mag]...
	// Generate a simulated ETAS catalog, display information about it, and write it to a file.
	// The basetime parameter is in ISO8601 format, or "-" to use the default of Jan 1 2000.

	public static void test4 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate simulated ETAS catalog, display info, and write to file");
		String filename_catalog = get_filename_arg (testargs, "filename_catalog");
		String basetime = get_omit_string_arg (testargs, "basetime", OEConstants.DEF_SIM_ORIGIN_TIME);
		double zmu = testargs.get_double ("zmu");
		double zams = testargs.get_double ("zams");
		double n = testargs.get_double ("n");
		double p = testargs.get_double ("p");
		double c = testargs.get_double ("c");
		double b = testargs.get_double ("b");
		double alpha = testargs.get_double ("alpha");
		double mag_min = testargs.get_double ("mref");
		double mag_max = testargs.get_double ("msup");
		double tbegin = testargs.get_double ("tbegin");
		double tend = testargs.get_double ("tend");
		double tint_br = testargs.get_double ("tint_br");

		double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
		testargs.end_test();

		// Base time in milliseconds

		long basetime_millis = SimpleUtils.string_to_time (basetime);

		// Make the catalog parameters

		double mref = OEConstants.DEF_MREF;
		double msup = OEConstants.DEF_MSUP;

		OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
			n,				// n
			p,				// p
			c,				// c
			b,				// b
			alpha,			// alpha
			mref,			// mref
			msup,			// msup
			tbegin,			// tbegin
			tend,			// tend
			tint_br,		// tint_br
			mag_min,		// mag_min_sim
			mag_max			// mag_max_sim
		);

		// Make the seed parameters

		OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

		// Make the catalog initializer

		OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

		// Make the catalog examiner

		ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
		OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

		// Generate a catalog

		OESimulator.gen_single_catalog (initializer, examiner);

		// Make origin at the basetime

		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// We take the mainshock to be the first element of the list, or no mainshock if the list is empty.

		int i_mainshock = -1;
		if (time_mag_array.length > 0) {
			i_mainshock = 0;
		}

		// Write the file

		origin.write_etas_list_to_gui_ext (
			rup_list,
			i_mainshock,
			filename_catalog
		);

		System.out.println ();
		System.out.println ("Wrote " + rup_list.size() + " simulated earthquakes to file: " + filename_catalog);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test16/write_sim_cat_2
	// Command line arguments:
	//  filename  basetime  zmu  rel_ams  n  p  c  b  alpha  mag_min  mag_max  tbegin  tend  tint_br
	//  [t_day  rup_mag]...
	// Generate a simulated ETAS catalog, display information about it, and write it to a file.
	// The basetime parameter is in ISO8601 format, or "-" to use the default of Jan 1 2000.
	// The value of ams is specified relative to the a-value.

	public static void test16 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate simulated ETAS catalog, display info, and write to file");
		String filename_catalog = get_filename_arg (testargs, "filename_catalog");
		String basetime = get_omit_string_arg (testargs, "basetime", OEConstants.DEF_SIM_ORIGIN_TIME);
		double zmu = testargs.get_double ("zmu");
		double rel_ams = testargs.get_double ("rel_ams");
		double n = testargs.get_double ("n");
		double p = testargs.get_double ("p");
		double c = testargs.get_double ("c");
		double b = testargs.get_double ("b");
		double alpha = testargs.get_double ("alpha");
		double mag_min = testargs.get_double ("mref");
		double mag_max = testargs.get_double ("msup");
		double tbegin = testargs.get_double ("tbegin");
		double tend = testargs.get_double ("tend");
		double tint_br = testargs.get_double ("tint_br");

		double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
		testargs.end_test();

		// Base time in milliseconds

		long basetime_millis = SimpleUtils.string_to_time (basetime);

		// Compute zams for the given branch ratio

		double zams = rel_ams + OEStatsCalc.calc_zams_from_br (
			n,
			p,
			c,
			b,
			alpha,
			mag_min,
			mag_max,
			tint_br
		);

		System.out.println ();
		System.out.println ("Equivalent zams = " + zams);

		// Make the catalog parameters

		double mref = OEConstants.DEF_MREF;
		double msup = OEConstants.DEF_MSUP;

		OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
			n,				// n
			p,				// p
			c,				// c
			b,				// b
			alpha,			// alpha
			mref,			// mref
			msup,			// msup
			tbegin,			// tbegin
			tend,			// tend
			tint_br,		// tint_br
			mag_min,		// mag_min_sim
			mag_max			// mag_max_sim
		);

		// Make the seed parameters

		OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

		// Make the catalog initializer

		OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

		// Make the catalog examiner

		ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
		OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

		// Generate a catalog

		OESimulator.gen_single_catalog (initializer, examiner);

		// Make origin at the basetime

		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// We take the mainshock to be the first element of the list, or no mainshock if the list is empty.

		int i_mainshock = -1;
		if (time_mag_array.length > 0) {
			i_mainshock = 0;
		}

		// Write the file

		origin.write_etas_list_to_gui_ext (
			rup_list,
			i_mainshock,
			filename_catalog
		);

		System.out.println ();
		System.out.println ("Wrote " + rup_list.size() + " simulated earthquakes to file: " + filename_catalog);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test5/write_cat_info
	// Command line arguments:
	//  filename  magCat  magTop  capF  capG  capH  t_data_begin  t_data_end  t_fitting  t_forecast
	// Write ETAS catalog information to a file (with unknown mainshock magnitude and location).
	// For no incompleteness:  F = 0.50, G = 100.0, H = 1.00
	// For World incompleteness:  F = 0.50, G = 0.25, H = 1.00
	// For California incompleteness:  F = 1.00, G = 4.50, H = 0.75

	public static void test5 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing ETAS catalog information to a file");
		String filename_cat_info = get_filename_arg (testargs, "filename_cat_info");

		double magCat = testargs.get_double ("magCat");
		double magTop = testargs.get_double ("magTop");
		double capF = testargs.get_double ("capF");
		double capG = testargs.get_double ("capG");
		double capH = testargs.get_double ("capH");
		double t_data_begin = testargs.get_double ("t_data_begin");
		double t_data_end = testargs.get_double ("t_data_end");
		double t_fitting = testargs.get_double ("t_fitting");
		double t_forecast = testargs.get_double ("t_forecast");

		testargs.end_test();

		// Make the catalog information

		OEtasCatalogInfo cat_into = (new OEtasCatalogInfo()).set (
			magCat,			// magCat
			magTop,			// magTop
			capF,			// capF
			capG,			// capG
			capH,			// capH
			t_data_begin,	// t_data_begin
			t_data_end,		// t_data_end
			t_fitting,		// t_fitting
			t_forecast		// t_forecast
		);

		// Write to file

		MarshalUtils.to_formatted_json_file (cat_into, filename_cat_info);

		System.out.println ();
		System.out.println ("Wrote catalog information to file: " + filename_cat_info);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test12/write_cat_info_2
	// Command line arguments:
	//  filename  magCat  magTop  capF  capG  capH  t_data_begin  t_data_end  t_fitting  t_forecast  mag_main  lat_main  lon_main  depth_main
	// Write ETAS catalog information to a file.
	// For no incompleteness:  F = 0.50, G = 100.0, H = 1.00
	// For World incompleteness:  F = 0.50, G = 0.25, H = 1.00
	// For California incompleteness:  F = 1.00, G = 4.50, H = 0.75

	public static void test12 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing ETAS catalog information to a file");
		String filename_cat_info = get_filename_arg (testargs, "filename_cat_info");

		double magCat = testargs.get_double ("magCat");
		double magTop = testargs.get_double ("magTop");
		double capF = testargs.get_double ("capF");
		double capG = testargs.get_double ("capG");
		double capH = testargs.get_double ("capH");
		double t_data_begin = testargs.get_double ("t_data_begin");
		double t_data_end = testargs.get_double ("t_data_end");
		double t_fitting = testargs.get_double ("t_fitting");
		double t_forecast = testargs.get_double ("t_forecast");

		double mag_main = testargs.get_double ("mag_main");
		double lat_main = testargs.get_double ("lat_main");
		double lon_main = testargs.get_double ("lon_main");
		double depth_main = testargs.get_double ("depth_main");

		testargs.end_test();

		// Make the catalog information

		OEtasCatalogInfo cat_into = (new OEtasCatalogInfo()).set (
			magCat,			// magCat
			magTop,			// magTop
			capF,			// capF
			capG,			// capG
			capH,			// capH
			t_data_begin,	// t_data_begin
			t_data_end,		// t_data_end
			t_fitting,		// t_fitting
			t_forecast,		// t_forecast
			mag_main,		// mag_main
			lat_main,		// lat_main
			lon_main,		// lon_main
			depth_main		// depth_main
		);

		// Write to file

		MarshalUtils.to_formatted_json_file (cat_into, filename_cat_info);

		System.out.println ();
		System.out.println ("Wrote catalog information to file: " + filename_cat_info);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test13/write_cat_info_3
	// Command line arguments:
	//  filename  magCat  magTop  capF  capG  capH  t_data_begin  t_data_end  t_fitting  t_forecast  event_id
	// Write ETAS catalog information to a file.
	// For no incompleteness:  F = 0.50, G = 100.0, H = 1.00
	// For World incompleteness:  F = 0.50, G = 0.25, H = 1.00
	// For California incompleteness:  F = 1.00, G = 4.50, H = 0.75

	public static void test13 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing ETAS catalog information to a file");
		String filename_cat_info = get_filename_arg (testargs, "filename_cat_info");

		double magCat = testargs.get_double ("magCat");
		double magTop = testargs.get_double ("magTop");
		double capF = testargs.get_double ("capF");
		double capG = testargs.get_double ("capG");
		double capH = testargs.get_double ("capH");
		double t_data_begin = testargs.get_double ("t_data_begin");
		double t_data_end = testargs.get_double ("t_data_end");
		double t_fitting = testargs.get_double ("t_fitting");
		double t_forecast = testargs.get_double ("t_forecast");

		String event_id = testargs.get_string ("event_id");

		testargs.end_test();

		// Get mainshock information

		System.out.println ();

		ObsEqkRupture obs_main = fetch_mainshock (event_id, false);

		double mag_main = obs_main.getMag();
		Location hypo = obs_main.getHypocenterLocation();
		double lat_main = hypo.getLatitude();
		double lon_main = hypo.getLongitude();
		double depth_main = hypo.getDepth();

		System.out.println ("mag_main = " + mag_main);
		System.out.println ("lat_main = " + lat_main);
		System.out.println ("lon_main = " + lon_main);
		System.out.println ("depth_main = " + depth_main);

		// Make the catalog information

		OEtasCatalogInfo cat_into = (new OEtasCatalogInfo()).set (
			magCat,			// magCat
			magTop,			// magTop
			capF,			// capF
			capG,			// capG
			capH,			// capH
			t_data_begin,	// t_data_begin
			t_data_end,		// t_data_end
			t_fitting,		// t_fitting
			t_forecast,		// t_forecast
			mag_main,		// mag_main
			lat_main,		// lat_main
			lon_main,		// lon_main
			depth_main		// depth_main
		);

		// Write to file

		MarshalUtils.to_formatted_json_file (cat_into, filename_cat_info);

		System.out.println ();
		System.out.println ("Wrote catalog information to file: " + filename_cat_info);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test6
	// Command line arguments:
	//  filename  basetime  t_main_day
	// Read ETAS catalog information from a file.
	// Make a forecast information object, and display it.

	public static void test6 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Reading ETAS catalog information from a file, and making forecast information object");
		String filename_cat_info = get_filename_arg (testargs, "filename_cat_info");
		String basetime = get_omit_string_arg (testargs, "basetime", OEConstants.DEF_SIM_ORIGIN_TIME);
		double t_main_day = testargs.get_double ("t_main_day");

		testargs.end_test();

		// Base time in milliseconds

		long basetime_millis = SimpleUtils.string_to_time (basetime);

		// Read the catalog information

		OEtasCatalogInfo cat_into = new OEtasCatalogInfo();
		MarshalUtils.from_json_file (cat_into, filename_cat_info);

		// Make origin at the basetime

		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// Make the forecast info

		USGS_ForecastInfo fc_info = cat_into.make_fc_info_for_test (origin, t_main_day);

		System.out.println ();
		System.out.println (fc_info.toString());

		System.out.println ();
		System.out.println (MarshalUtils.to_formatted_json_string (fc_info));

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test7/show_etas
	// Command line arguments:
	//  fn_catalog  fn_cat_info  fn_params
	// Make an ETAS forecast for the given catalog, catalog information, and parameters.
	// Forecast info is constructed assuming the mainshock is at time 0.0 days.

	public static void test7 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Display ETAS forecast");
		String fn_catalog = get_filename_arg (testargs, "fn_catalog");
		String fn_cat_info = get_filename_arg (testargs, "fn_cat_info");
		String fn_params = get_filename_arg (testargs, "fn_params");

		testargs.end_test();

		// Read the catalog

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.read_from_file (fn_catalog, null);

		if (ext_cat.mainshock == null) {
			throw new RuntimeException ("No mainshock found in catalog file: " + fn_catalog);
		}

		// Read the catalog information

		OEtasCatalogInfo cat_into = new OEtasCatalogInfo();
		MarshalUtils.from_json_file (cat_into, fn_cat_info);

		// Read the parameters

		OEtasParameters etas_params = new OEtasParameters();
		MarshalUtils.from_json_file (etas_params, fn_params);

		// Make origin at the basetime, which is assumed to be the mainshock time

		long basetime_millis = ext_cat.mainshock.getOriginTime();
		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// Make the forecast info

		double t_main_day = 0.0;
		USGS_ForecastInfo fc_info = cat_into.make_fc_info_for_test (origin, t_main_day);

		System.out.println ();
		System.out.println (fc_info.toString());

		// Create multi-thread context

		int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
		long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
		long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

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

				// Set up the input area

				exec_env.setup_input_area_from_obs (
					etas_params,			// OEtasParameters the_etas_params,
					fc_info,				// USGS_ForecastInfo the_forecast_info,
					cat_into,				// OEtasCatalogInfo the_catalog_info,
					ext_cat.mainshock,		// ObsEqkRupture the_obs_mainshock,
					ext_cat.aftershocks		// List<ObsEqkRupture> the_obs_rup_list
				);

				// Run ETAS!

				exec_env.run_etas();
			}

			// Pass exceptions into the ETAS execution environment

			catch (Exception e) {
				exec_env.report_exception (e);
			}

			// Display result

			System.out.println ();

			if (exec_env.is_etas_successful()) {
				System.out.println ();
				System.out.println (exec_env.get_forecast_summary_string());
				System.out.println ("ETAS succeeded");
			}
			else {
				System.out.println ("ETAS failed, result code = " + exec_env.get_rescode_as_string());
			}

			if (exec_env.has_abort_message()) {
				System.out.println (exec_env.get_abort_message());
			}

			System.out.println ("Log: " + exec_env.make_etas_log_string());

			exec_timer.stop_timer();

			long elapsed_time = exec_timer.get_total_runtime();
			long elapsed_seconds = (elapsed_time + 500L) / 1000L;

			System.out.println ();
			System.out.println ("Elapsed time = " + elapsed_seconds + " seconds");
		}

		// Done

		//System.out.println ();
		//System.out.println ("Done");

		return;
	}




	// test8/run_etas
	// Command line arguments:
	//  fn_catalog  fn_cat_info  fn_params  fn_accepted  fn_mag_comp  fn_log_density  fn_intensity  fn_results  fn_fc_json
	// Make an ETAS forecast for the given catalog, catalog information, and parameters.
	// Forecast info is constructed assuming the mainshock is at time 0.0 days.
	// Write any requested files.

	public static void test8 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate ETAS forecast");
		String fn_catalog = get_filename_arg (testargs, "fn_catalog");
		String fn_cat_info = get_filename_arg (testargs, "fn_cat_info");
		String fn_params = get_filename_arg (testargs, "fn_params");

		String fn_accepted = get_filename_arg (testargs, "fn_accepted");
		String fn_mag_comp = get_filename_arg (testargs, "fn_mag_comp");
		String fn_log_density = get_filename_arg (testargs, "fn_log_density");
		String fn_intensity = get_filename_arg (testargs, "fn_intensity");
		String fn_results = get_filename_arg (testargs, "fn_results");
		String fn_fc_json = get_filename_arg (testargs, "fn_fc_json");

		testargs.end_test();

		// Read the catalog

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.read_from_file (fn_catalog, null);

		if (ext_cat.mainshock == null) {
			throw new RuntimeException ("No mainshock found in catalog file: " + fn_catalog);
		}

		// Read the catalog information

		OEtasCatalogInfo cat_into = new OEtasCatalogInfo();
		MarshalUtils.from_json_file (cat_into, fn_cat_info);

		// Read the parameters

		OEtasParameters etas_params = new OEtasParameters();
		MarshalUtils.from_json_file (etas_params, fn_params);

		// Make origin at the basetime, which is assumed to be the mainshock time

		long basetime_millis = ext_cat.mainshock.getOriginTime();
		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// Make the forecast info

		double t_main_day = 0.0;
		USGS_ForecastInfo fc_info = cat_into.make_fc_info_for_test (origin, t_main_day);

		// Create multi-thread context

		int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
		long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
		long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

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

				exec_env.filename_accepted = fn_accepted;
				exec_env.filename_mag_comp = fn_mag_comp;
				exec_env.filename_log_density = fn_log_density;
				exec_env.filename_intensity_calc = fn_intensity;
				exec_env.filename_results = fn_results;
				exec_env.filename_fc_json = fn_fc_json;
				exec_env.filename_marginals = null;

				// Set up the input area

				exec_env.setup_input_area_from_obs (
					etas_params,			// OEtasParameters the_etas_params,
					fc_info,				// USGS_ForecastInfo the_forecast_info,
					cat_into,				// OEtasCatalogInfo the_catalog_info,
					ext_cat.mainshock,		// ObsEqkRupture the_obs_mainshock,
					ext_cat.aftershocks		// List<ObsEqkRupture> the_obs_rup_list
				);

				// Run ETAS!

				exec_env.run_etas();
			}

			// Pass exceptions into the ETAS execution environment

			catch (Exception e) {
				exec_env.report_exception (e);
			}

			// Display result

			System.out.println ();

			if (exec_env.is_etas_successful()) {
				System.out.println ();
				System.out.println (exec_env.get_forecast_summary_string());
				System.out.println ("ETAS succeeded");
			}
			else {
				System.out.println ("ETAS failed, result code = " + exec_env.get_rescode_as_string());
			}

			if (exec_env.has_abort_message()) {
				System.out.println (exec_env.get_abort_message());
			}

			System.out.println ("Log: " + exec_env.make_etas_log_string());

			exec_timer.stop_timer();

			long elapsed_time = exec_timer.get_total_runtime();
			long elapsed_seconds = (elapsed_time + 500L) / 1000L;

			System.out.println ();
			System.out.println ("Elapsed time = " + elapsed_seconds + " seconds");
		}

		// Done

		//System.out.println ();
		//System.out.println ("Done");

		return;
	}




	// test17/run_etas_2
	// Command line arguments:
	//  fn_catalog  fn_cat_info  fn_params  fn_accepted  fn_mag_comp  fn_log_density  fn_intensity  fn_results  fn_fc_json  fn_marginals
	// Make an ETAS forecast for the given catalog, catalog information, and parameters.
	// Forecast info is constructed assuming the mainshock is at time 0.0 days.
	// Write any requested files.

	public static void test17 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate ETAS forecast");
		String fn_catalog = get_filename_arg (testargs, "fn_catalog");
		String fn_cat_info = get_filename_arg (testargs, "fn_cat_info");
		String fn_params = get_filename_arg (testargs, "fn_params");

		String fn_accepted = get_filename_arg (testargs, "fn_accepted");
		String fn_mag_comp = get_filename_arg (testargs, "fn_mag_comp");
		String fn_log_density = get_filename_arg (testargs, "fn_log_density");
		String fn_intensity = get_filename_arg (testargs, "fn_intensity");
		String fn_results = get_filename_arg (testargs, "fn_results");
		String fn_fc_json = get_filename_arg (testargs, "fn_fc_json");
		String fn_marginals = get_filename_arg (testargs, "fn_marginals");

		testargs.end_test();

		// Read the catalog

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.read_from_file (fn_catalog, null);

		if (ext_cat.mainshock == null) {
			throw new RuntimeException ("No mainshock found in catalog file: " + fn_catalog);
		}

		// Read the catalog information

		OEtasCatalogInfo cat_into = new OEtasCatalogInfo();
		MarshalUtils.from_json_file (cat_into, fn_cat_info);

		// Read the parameters

		OEtasParameters etas_params = new OEtasParameters();
		MarshalUtils.from_json_file (etas_params, fn_params);

		// Make origin at the basetime, which is assumed to be the mainshock time

		long basetime_millis = ext_cat.mainshock.getOriginTime();
		OEOrigin origin = new OEOrigin (basetime_millis, 0.0, 0.0, 0.0);

		// Make the forecast info

		double t_main_day = 0.0;
		USGS_ForecastInfo fc_info = cat_into.make_fc_info_for_test (origin, t_main_day);

		// Create multi-thread context

		int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
		long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
		long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

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

				exec_env.filename_accepted = fn_accepted;
				exec_env.filename_mag_comp = fn_mag_comp;
				exec_env.filename_log_density = fn_log_density;
				exec_env.filename_intensity_calc = fn_intensity;
				exec_env.filename_results = fn_results;
				exec_env.filename_fc_json = fn_fc_json;
				exec_env.filename_marginals = fn_marginals;

				// Set up the input area

				exec_env.setup_input_area_from_obs (
					etas_params,			// OEtasParameters the_etas_params,
					fc_info,				// USGS_ForecastInfo the_forecast_info,
					cat_into,				// OEtasCatalogInfo the_catalog_info,
					ext_cat.mainshock,		// ObsEqkRupture the_obs_mainshock,
					ext_cat.aftershocks		// List<ObsEqkRupture> the_obs_rup_list
				);

				// Run ETAS!

				exec_env.run_etas();
			}

			// Pass exceptions into the ETAS execution environment

			catch (Exception e) {
				exec_env.report_exception (e);
			}

			// Display result

			System.out.println ();

			if (exec_env.is_etas_successful()) {
				System.out.println ();
				System.out.println (exec_env.get_forecast_summary_string());
				System.out.println ("ETAS succeeded");
			}
			else {
				System.out.println ("ETAS failed, result code = " + exec_env.get_rescode_as_string());
			}

			if (exec_env.has_abort_message()) {
				System.out.println (exec_env.get_abort_message());
			}

			System.out.println ("Log: " + exec_env.make_etas_log_string());

			exec_timer.stop_timer();

			long elapsed_time = exec_timer.get_total_runtime();
			long elapsed_seconds = (elapsed_time + 500L) / 1000L;

			System.out.println ();
			System.out.println ("Elapsed time = " + elapsed_seconds + " seconds");
		}

		// Done

		//System.out.println ();
		//System.out.println ("Done");

		return;
	}




	// test9/mainshock_info
	// Command line arguments:
	//  event_id
	// Fetch mainshock information from Comcat, and display it.

	public static void test9 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Fetch mainshock information from Comcat");
		String event_id = testargs.get_string ("event_id");
		testargs.end_test();

		// Fetch mainshock

		System.out.println ();

		ObsEqkRupture obs_main = fetch_mainshock (event_id, true);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test10/write_obs_cat
	// Command line arguments:
	//  filename  event_id  min_days  max_days  radius_km  min_mag
	// Fetch a catalog from Comcat, and write it to a file.
	// If radius_km is 0.0, then the Wells and Coppersmith radius is used.
	// The centroid algorithm is used.

	public static void test10 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Fetch aftershock (and foreshock) catalog from Comcat, and write to file");
		String filename_catalog = get_filename_arg (testargs, "filename_catalog");
		String event_id = testargs.get_string ("event_id");
		double min_days = testargs.get_double ("min_days");
		double max_days = testargs.get_double ("max_days");
		double radius_km = testargs.get_double ("radius_km");
		double min_mag = testargs.get_double ("min_mag");

		testargs.end_test();

		// Fetch mainshock

		System.out.println ();

		ObsEqkRupture obs_main = fetch_mainshock (event_id, false);

		// Make origin at the mainshock

		//OEOrigin origin = (new OEOrigin()).set_from_rupture_horz (obs_main);

		// Fetch aftershocks

		ObsEqkRupList aftershocks = fetch_aftershocks (obs_main, min_days, max_days, radius_km, min_mag);

		// Set up for an external catalog

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.setup_catalog (
			aftershocks,
			obs_main
		);

		// Write the file

		ext_cat.write_to_file (filename_catalog);

		System.out.println ();
		System.out.println ("Wrote " + (aftershocks.size() + 1) + " observed earthquakes to file: " + filename_catalog);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test11/system_info
	// Command line arguments:
	//  <empty>
	// Display number of threads and memory information.

	public static void test11 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Displaying system information");
		testargs.end_test();

		// Display program version information

		System.out.println ();
		System.out.println (VersionInfo.get_one_line_version());

		// Display threads

		System.out.println ();
		System.out.println ("Threads: " + AutoExecutorService.get_default_num_threads());

		// Display memory

		System.out.println ();
		System.out.println (SimpleUtils.one_line_memory_status_string());
		System.out.println ();

		return;
	}




	// test18/write_prior_marginals
	// Command line arguments:
	//  filename  fn_params  tint_br  f_bivar_marg
	// Write marginal file for the Bayesian prior.
	// The parameter grid and Bayesian prior are obtained from an ETAS parameter file.
	// The Bayesian factory is called with unknown mainshock magnitude and location.

	public static void test18 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing marginals for a Bayesian prior");
		String filename_marginal = get_filename_arg (testargs, "filename_marginal");
		String fn_params = get_filename_arg (testargs, "fn_params");

		double tint_br = testargs.get_double ("tint_br");
		boolean f_bivar_marg = testargs.get_boolean ("f_bivar_marg");

		testargs.end_test();

		// Mainshock magnitude and location

		double mag_main = OEBayFactoryParams.unknown_mag();
		Location loc_main = OEBayFactoryParams.unknown_loc();

		// Read the parameters

		OEtasParameters etas_params = new OEtasParameters();
		MarshalUtils.from_json_file (etas_params, fn_params);

		// Get grid parameters and options

		OEGridParams grid_params = etas_params.make_grid_params();
		OEGridOptions grid_options = etas_params.make_grid_options();

		// Bayesian factory parameters

		OEBayFactoryParams bay_factory_params = new OEBayFactoryParams (mag_main, loc_main);

		// Get Bayesian prior parameters

		OEBayPrior bay_prior = etas_params.get_bay_prior (bay_factory_params);

		// Bayesian prior Parameters

		OEBayPriorParams bay_params = new OEBayPriorParams (mag_main, tint_br, grid_options);

		// Compute the marginals

		OEMarginalDistSet dist_set = make_marginal_for_prior (
			bay_prior,
			bay_params,
			grid_params,
			f_bivar_marg
		);

		// Write to file

		MarshalUtils.to_formatted_json_file (dist_set, filename_marginal);

		System.out.println ();
		System.out.println ("Wrote Bayesian prior marginals to file: " + filename_marginal);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test19/write_event_inputs
	// Command line arguments:
	//  event_id  forecast_lag  fn_catalog  fn_cat_info  fn_params
	// Write the catalog, catalog information, and parameters files that the
	// automatic system would use for the given event and forecast lag.
	// The forecast lag can be a decimal number of days, or a string of
	// form similar to 7d8h15m.

	public static void test19 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Write input files for a given event and forecast lag");
		String event_id = get_filename_arg (testargs, "event_id");
		String s_forecast_lag = get_filename_arg (testargs, "forecast_lag");

		String fn_catalog = get_filename_arg (testargs, "fn_catalog");
		String fn_cat_info = get_filename_arg (testargs, "fn_cat_info");
		String fn_params = get_filename_arg (testargs, "fn_params");

		testargs.end_test();

		// Parse the forecast lag

		long forecast_lag = SimpleUtils.string_to_duration_3 (s_forecast_lag, "d");

		System.out.println ();
		System.out.println ("Fetching data for event_id = " + event_id + ", forecast_lag = " + SimpleUtils.duration_to_string_3 (forecast_lag));

		// Read the inputs

		ObsInputs obs_inputs = get_obs_inputs (event_id, forecast_lag, null);

		if (obs_inputs == null) {
			System.out.println ();
			System.out.println ("Unable to construct inputs");
			return;
		}

		// Write the catalog file

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.setup_catalog (
			obs_inputs.the_rup_list,
			obs_inputs.obs_mainshock
		);

		ext_cat.write_to_file (fn_catalog);

		System.out.println ();
		System.out.println ("Wrote " + (obs_inputs.the_rup_list.size() + 1) + " observed earthquakes to file: " + fn_catalog);

		// Write the catalog information file

		MarshalUtils.to_formatted_json_file (obs_inputs.catalog_info, fn_cat_info);

		System.out.println ();
		System.out.println ("Wrote catalog information to file: " + fn_cat_info);

		// Write the parameters file

		MarshalUtils.to_formatted_json_file (obs_inputs.etas_params, fn_params);

		System.out.println ();
		System.out.println ("Wrote ETAS parameters to file: " + fn_params);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test21/perturb_cat_mags
	// Command line arguments:
	//  in_filename  out_filename  mean  sdev  delta_min  delta_max  mag_min  mag_max
	// Read a catalog file, and apply a normal perturbation to each aftershock magnitude
	// (but not the mainshock magnitude).  Write the result to the output file.
	// The perturbation will not be less than delta_min or greater than delta_max
	// (typically delta_min == -delta_max).
	// A magnitude will not be increased above mag_max or reduced below mag_min.

	public static void test21 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Apply a normal perturbation to each magnitude in a catalog file");
		String in_filename = get_filename_arg (testargs, "in_filename");
		String out_filename = get_filename_arg (testargs, "out_filename");
		double mean = testargs.get_double ("mean");
		double sdev = testargs.get_double ("sdev");
		double delta_min = testargs.get_double ("delta_min");
		double delta_max = testargs.get_double ("delta_max");
		double mag_min = testargs.get_double ("mref");
		double mag_max = testargs.get_double ("msup");
		testargs.end_test();

		// Read the catalog

		GUIExternalCatalog catalog = new GUIExternalCatalog();
		catalog.read_from_file (in_filename, null);

		System.out.println ();
		System.out.println ("Read " + catalog.aftershocks.size() + " aftershocks from file: " + in_filename);

		// Get a random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Loop over aftershocks ...

		for (ObsEqkRupture rup : catalog.aftershocks) {

			// Get the magnitude

			double mag = rup.getMag();

			// Loop until success ...

			for (;;) {

				// Get a normal deviate

				double delta = rangen.normal_sample (mean, sdev);

				// If it is in range ...

				if (delta >= delta_min && delta <= delta_max) {

					// New magnitude

					double new_mag = mag + delta;

					// If it is in range, set it and stop looping

					if (new_mag >= Math.min (mag, mag_min) && new_mag <= Math.max (mag, mag_max)) {
						rup.setMag (new_mag);
						break;
					}
				}
			}
		}

		// Write the perturbed catalog to a file

		catalog.write_to_file (out_filename);

		System.out.println ();
		System.out.println ("Wrote " + catalog.aftershocks.size() + " perturbed aftershocks to file: " + out_filename);

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	// test22/gen_rj_forecasts
	// Command line arguments:
	//  event_id  lag_days  gen_filename  seq_filename  bay_filename
	// Generate RJ forecasts for the given event at the given lag (time after mainshock in days)
	// and write the forecast.json files.
	// Any filename can be "" or "-" to skip it.

	public static void test22 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Generate RJ forecasts and write the forecast.json files");
		String event_id = testargs.get_string ("event_id");
		double lag_days = testargs.get_double ("lag_days");
		String gen_filename = get_filename_arg (testargs, "gen_filename");
		String seq_filename = get_filename_arg (testargs, "seq_filename");
		String bay_filename = get_filename_arg (testargs, "bay_filename");
		testargs.end_test();

		// Perform the operation

		int pdl_enable = 100;		// disable ETAS
		String etas_filename = null;

		boolean f_success = ForecastResults.calc_and_write_fcj (
			pdl_enable,
			event_id,
			lag_days,
			gen_filename,
			seq_filename,
			bay_filename,
			etas_filename
		);

		// Display result

		System.out.println ();
		if (f_success) {
			System.out.println ("RJ forecasts were computed and written successfully");
		} else {
			System.out.println ("An error occurred while computing and writing RJ forecasts");
		}

		// Done

		System.out.println ();
		System.out.println ("Done");

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasTest");




		if (testargs.is_test ("test1", "hello_world")) {
			test1 (testargs);
			return;
		}


		if (testargs.is_test ("test2", "write_sample_params")) {
			test2 (testargs);
			return;
		}


		if (testargs.is_test ("test3")) {
			test3 (testargs);
			return;
		}


		if (testargs.is_test ("test4", "write_sim_cat")) {
			test4 (testargs);
			return;
		}


		if (testargs.is_test ("test5", "write_cat_info")) {
			test5 (testargs);
			return;
		}


		if (testargs.is_test ("test6")) {
			test6 (testargs);
			return;
		}


		if (testargs.is_test ("test7", "show_etas")) {
			test7 (testargs);
			return;
		}


		if (testargs.is_test ("test8", "run_etas")) {
			test8 (testargs);
			return;
		}


		if (testargs.is_test ("test9", "mainshock_info")) {
			test9 (testargs);
			return;
		}


		if (testargs.is_test ("test10", "write_obs_cat")) {
			test10 (testargs);
			return;
		}


		if (testargs.is_test ("test11", "system_info")) {
			test11 (testargs);
			return;
		}


		if (testargs.is_test ("test12", "write_cat_info_2")) {
			test12 (testargs);
			return;
		}


		if (testargs.is_test ("test13", "write_cat_info_3")) {
			test13 (testargs);
			return;
		}


		if (testargs.is_test ("test14", "write_sample_params_2")) {
			test14 (testargs);
			return;
		}


		if (testargs.is_test ("test15")) {
			test15 (testargs);
			return;
		}


		if (testargs.is_test ("test16", "write_sim_cat_2")) {
			test16 (testargs);
			return;
		}


		if (testargs.is_test ("test17", "run_etas_2")) {
			test17 (testargs);
			return;
		}


		if (testargs.is_test ("test18", "write_prior_marginals")) {
			test18 (testargs);
			return;
		}


		if (testargs.is_test ("test19", "write_event_inputs")) {
			test19 (testargs);
			return;
		}


		if (testargs.is_test ("test20", "write_sample_params_3")) {
			test20 (testargs);
			return;
		}


		if (testargs.is_test ("test21", "perturb_cat_mags")) {
			test21 (testargs);
			return;
		}


		if (testargs.is_test ("test22", "gen_rj_forecasts")) {
			test22 (testargs);
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
