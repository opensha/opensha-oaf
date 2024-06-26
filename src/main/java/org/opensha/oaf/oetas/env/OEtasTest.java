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
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OESimulator;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;
import org.opensha.oaf.rj.MagCompPage_ParametersFetch;
import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.SearchMagFn;
import org.opensha.oaf.rj.SearchRadiusFn;
import org.opensha.oaf.rj.USGS_ForecastInfo;

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




	// Fetch mainshock, and display its information.
	// Throws exception if not found, or error accessing Comcat.

	public static ObsEqkRupture fetch_mainshock (String event_id) {

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

		// Display tectonic regime

		Location hypo = rup.getHypocenterLocation();
		
		GenericRJ_ParametersFetch generic_fetch = new GenericRJ_ParametersFetch();
		OAFTectonicRegime generic_regime = generic_fetch.getRegion (hypo);

		System.out.println ();
		System.out.println ("Tectonic regime: " + generic_regime.toString());

		// Display magnitude of completeness regine
		
		MagCompPage_ParametersFetch mag_comp_fetch = new MagCompPage_ParametersFetch();
		OAFTectonicRegime mag_comp_regime = mag_comp_fetch.getRegion (hypo);

		System.out.println ("Magnitude of completeness regime: " + mag_comp_regime.toString());

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

	public static void test2 (TestArgs testargs) throws Exception {

		// Read arguments

		System.out.println ("Writing sample ETAS parameters to a file");
		String filename = testargs.get_string ("filename");
		testargs.end_test();

		// Create the parameters

		OEtasParameters etas_params = new OEtasParameters();
		etas_params.set_to_typical ();

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




	// test5/write_cat_info
	// Command line arguments:
	//  filename  magCat  magTop  capF  capG  capH  t_data_begin  t_data_end  t_fitting  t_forecast
	// Write ETAS catalog information to a file.
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

		ObsEqkRupture obs_main = fetch_mainshock (event_id);

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

		ObsEqkRupture obs_main = fetch_mainshock (event_id);

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



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
