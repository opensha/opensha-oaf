package org.opensha.oaf.oetas.env;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import java.io.IOException;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleThreadLoopHelper;
import org.opensha.oaf.util.SimpleThreadLoopResult;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.util.gui.GUIExternalCatalog;

import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogParamsMags;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEForecastGrid;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESimulationParams;
import org.opensha.oaf.oetas.OESimulator;

import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;

import org.opensha.oaf.oetas.fit.OEDisc2ExtFit;
import org.opensha.oaf.oetas.fit.OEDisc2Grouping;
import org.opensha.oaf.oetas.fit.OEDisc2History;
import org.opensha.oaf.oetas.fit.OEDisc2InitFitInfo;
import org.opensha.oaf.oetas.fit.OEDisc2InitVoxBuilder;
import org.opensha.oaf.oetas.fit.OEDisc2InitVoxSet;
import org.opensha.oaf.oetas.fit.OEDiscFGHParams;
import org.opensha.oaf.oetas.fit.OEGridParams;
import org.opensha.oaf.oetas.fit.OEGridOptions;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccum;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccumMarginal;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccumMulti;

import org.opensha.oaf.oetas.util.OEMarginalDistSet;

import org.opensha.oaf.oetas.except.OEException;
import org.opensha.oaf.oetas.except.OEDataInvalidException;
import org.opensha.oaf.oetas.except.OEFitException;
import org.opensha.oaf.oetas.except.OEFitConvergenceException;
import org.opensha.oaf.oetas.except.OEFitThreadAbortException;
import org.opensha.oaf.oetas.except.OEFitTimeoutException;
import org.opensha.oaf.oetas.except.OERangeException;
import org.opensha.oaf.oetas.except.OERangeConvergenceException;
import org.opensha.oaf.oetas.except.OERangeThreadAbortException;
import org.opensha.oaf.oetas.except.OERangeTimeoutException;
import org.opensha.oaf.oetas.except.OESimException;
import org.opensha.oaf.oetas.except.OESimForecastException;
import org.opensha.oaf.oetas.except.OESimThreadAbortException;
import org.opensha.oaf.oetas.except.OESimTimeoutException;

import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_ForecastException;
import org.opensha.oaf.rj.USGS_ForecastInfo;
import org.opensha.oaf.rj.USGS_ForecastModel;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


// Class to hold the execution environment for running operational ETAS.
// Author: Michael Barall 05/03/2022.

public class OEExecEnvironment {


	//---- Constants -----


	// ETAS result codes.
	// Zero indicates ETAS successful.
	// Negative indicates ETAS not attempted or not completed.
	// Positive indicates ETAS failed.

	public static final int ETAS_RESCODE_MIN = -8;
	public static final int ETAS_RESCODE_NOT_ELIGIBLE = -8;			// Not eligible for an ETAS forecast
	public static final int ETAS_RESCODE_MAG_COMP_FORM = -7;		// Unsupported form of magnitude completeness function
	public static final int ETAS_RESCODE_NO_DATA = -6;				// Required data not available
	public static final int ETAS_RESCODE_UNSUPPORTED = -5;			// ETAS not supported by the implementation
	public static final int ETAS_RESCODE_NO_PARAMS = -4;			// No ETAS parameters available
	public static final int ETAS_RESCODE_DISABLED = -3;				// ETAS is disabled in the config file
	public static final int ETAS_RESCODE_IN_PROGRESS = -2;			// ETAS code is running
	public static final int ETAS_RESCODE_NOT_ATTEMPTED = -1;		// ETAS forecast not attempted
	public static final int ETAS_RESCODE_OK = 0;					// Success
	public static final int ETAS_RESCODE_GENERAL_ABORT = 1;			// Exception in ETAS code
	public static final int ETAS_RESCODE_FIT_ABORT = 2;				// Exception while fitting parameters
	public static final int ETAS_RESCODE_FIT_CONVERGENCE = 3;		// Parameter fitting failed to converge
	public static final int ETAS_RESCODE_FIT_THREAD_ABORT = 4;		// Compute thread exception while fitting parameters
	public static final int ETAS_RESCODE_FIT_TIMEOUT = 5;			// Timeout while fitting parameters
	public static final int ETAS_RESCODE_RANGE_ABORT = 6;			// Exception while ranging
	public static final int ETAS_RESCODE_RANGE_CONVERGENCE = 7;		// Ranging failed to converge
	public static final int ETAS_RESCODE_RANGE_THREAD_ABORT = 8;	// Compute thread exception while ranging
	public static final int ETAS_RESCODE_RANGE_TIMEOUT = 9;			// Timeout while ranging
	public static final int ETAS_RESCODE_SIM_ABORT = 10;			// Exception while simulating
	public static final int ETAS_RESCODE_SIM_FORECAST = 11;			// Unable to format simulation result as a forecast
	public static final int ETAS_RESCODE_SIM_THREAD_ABORT = 12;		// Compute thread exception while simulating
	public static final int ETAS_RESCODE_SIM_TIMEOUT = 13;			// Timeout while simulating
	public static final int ETAS_RESCODE_UNEXPECTED_ERROR = 14;		// Unexpected exception in ETAS code
	public static final int ETAS_RESCODE_IO_ERROR = 15;				// I/O error
	public static final int ETAS_RESCODE_DATA_INVALID = 16;			// Received invalid data
	public static final int ETAS_RESCODE_UNKNOWN_FAILURE = 17;		// Failed for an unknown reason
	public static final int ETAS_RESCODE_MAX = 17;

	// Return a string identifying the result code

	public static String etas_result_to_string (int result) {
		switch (result) {
		case ETAS_RESCODE_NOT_ELIGIBLE: return "ETAS_RESCODE_NOT_ELIGIBLE";
		case ETAS_RESCODE_MAG_COMP_FORM: return "ETAS_RESCODE_MAG_COMP_FORM";
		case ETAS_RESCODE_NO_DATA: return "ETAS_RESCODE_NO_DATA";
		case ETAS_RESCODE_UNSUPPORTED: return "ETAS_RESCODE_UNSUPPORTED";
		case ETAS_RESCODE_NO_PARAMS: return "ETAS_RESCODE_NO_PARAMS";
		case ETAS_RESCODE_DISABLED: return "ETAS_RESCODE_DISABLED";
		case ETAS_RESCODE_IN_PROGRESS: return "ETAS_RESCODE_IN_PROGRESS";
		case ETAS_RESCODE_NOT_ATTEMPTED: return "ETAS_RESCODE_NOT_ATTEMPTED";
		case ETAS_RESCODE_OK: return "ETAS_RESCODE_OK";
		case ETAS_RESCODE_GENERAL_ABORT: return "ETAS_RESCODE_GENERAL_ABORT";
		case ETAS_RESCODE_FIT_ABORT: return "ETAS_RESCODE_FIT_ABORT";
		case ETAS_RESCODE_FIT_CONVERGENCE: return "ETAS_RESCODE_FIT_CONVERGENCE";
		case ETAS_RESCODE_FIT_THREAD_ABORT: return "ETAS_RESCODE_FIT_THREAD_ABORT";
		case ETAS_RESCODE_FIT_TIMEOUT: return "ETAS_RESCODE_FIT_TIMEOUT";
		case ETAS_RESCODE_RANGE_ABORT: return "ETAS_RESCODE_RANGE_ABORT";
		case ETAS_RESCODE_RANGE_CONVERGENCE: return "ETAS_RESCODE_RANGE_CONVERGENCE";
		case ETAS_RESCODE_RANGE_THREAD_ABORT: return "ETAS_RESCODE_RANGE_THREAD_ABORT";
		case ETAS_RESCODE_RANGE_TIMEOUT: return "ETAS_RESCODE_RANGE_TIMEOUT";
		case ETAS_RESCODE_SIM_ABORT: return "ETAS_RESCODE_SIM_ABORT";
		case ETAS_RESCODE_SIM_FORECAST: return "ETAS_RESCODE_SIM_FORECAST";
		case ETAS_RESCODE_SIM_THREAD_ABORT: return "ETAS_RESCODE_SIM_THREAD_ABORT";
		case ETAS_RESCODE_SIM_TIMEOUT: return "ETAS_RESCODE_SIM_TIMEOUT";
		case ETAS_RESCODE_UNEXPECTED_ERROR: return "ETAS_RESCODE_UNEXPECTED_ERROR";
		case ETAS_RESCODE_IO_ERROR: return "ETAS_RESCODE_IO_ERRPR";
		case ETAS_RESCODE_DATA_INVALID: return "ETAS_RESCODE_DATA_INVALID";
		case ETAS_RESCODE_UNKNOWN_FAILURE: return "ETAS_RESCODE_UNKNOWN_FAILURE";
		}
		return "ETAS_RESCODE_INVALID(" + result + ")";
	}




	// Return the log entry type corresponding to a result code

	public static int etas_result_to_logtype (int result) {
		switch (result) {
		case ETAS_RESCODE_NOT_ELIGIBLE:			return OEtasLogInfo.ETAS_LOGTYPE_SKIP;
		case ETAS_RESCODE_MAG_COMP_FORM:		return OEtasLogInfo.ETAS_LOGTYPE_SKIP;
		case ETAS_RESCODE_NO_DATA:				return OEtasLogInfo.ETAS_LOGTYPE_SKIP;
		case ETAS_RESCODE_UNSUPPORTED:			return OEtasLogInfo.ETAS_LOGTYPE_OMIT;
		case ETAS_RESCODE_NO_PARAMS:			return OEtasLogInfo.ETAS_LOGTYPE_SKIP;
		case ETAS_RESCODE_DISABLED:				return OEtasLogInfo.ETAS_LOGTYPE_OMIT;
		case ETAS_RESCODE_IN_PROGRESS:			return OEtasLogInfo.ETAS_LOGTYPE_UNKNOWN;
		case ETAS_RESCODE_NOT_ATTEMPTED:		return OEtasLogInfo.ETAS_LOGTYPE_SKIP;
		case ETAS_RESCODE_OK:					return OEtasLogInfo.ETAS_LOGTYPE_OK;
		case ETAS_RESCODE_GENERAL_ABORT:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_FIT_ABORT:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_FIT_CONVERGENCE:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_FIT_THREAD_ABORT:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_FIT_TIMEOUT:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_RANGE_ABORT:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_RANGE_CONVERGENCE:	return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_RANGE_THREAD_ABORT:	return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_RANGE_TIMEOUT:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_SIM_ABORT:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_SIM_FORECAST:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_SIM_THREAD_ABORT:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_SIM_TIMEOUT:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_UNEXPECTED_ERROR:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_IO_ERROR:				return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_DATA_INVALID:			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		case ETAS_RESCODE_UNKNOWN_FAILURE:		return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		}
		if (result > 0) {
			return OEtasLogInfo.ETAS_LOGTYPE_FAIL;
		}
		return OEtasLogInfo.ETAS_LOGTYPE_UNKNOWN;
	}




	// Given an exception, get the corresponding ETAS result code.

	public static int get_rescode_for_exception (Exception e) {
		if (e instanceof OESimTimeoutException) {return ETAS_RESCODE_SIM_TIMEOUT;}
		if (e instanceof OESimThreadAbortException) {return ETAS_RESCODE_SIM_THREAD_ABORT;}
		if (e instanceof OESimForecastException) {return ETAS_RESCODE_SIM_FORECAST;}
		if (e instanceof OESimException) {return ETAS_RESCODE_SIM_ABORT;}
		if (e instanceof OERangeTimeoutException) {return ETAS_RESCODE_RANGE_TIMEOUT;}
		if (e instanceof OERangeThreadAbortException) {return ETAS_RESCODE_RANGE_THREAD_ABORT;}
		if (e instanceof OERangeConvergenceException) {return ETAS_RESCODE_RANGE_CONVERGENCE;}
		if (e instanceof OERangeException) {return ETAS_RESCODE_RANGE_ABORT;}
		if (e instanceof OEFitTimeoutException) {return ETAS_RESCODE_FIT_TIMEOUT;}
		if (e instanceof OEFitThreadAbortException) {return ETAS_RESCODE_FIT_THREAD_ABORT;}
		if (e instanceof OEFitConvergenceException) {return ETAS_RESCODE_FIT_CONVERGENCE;}
		if (e instanceof OEFitException) {return ETAS_RESCODE_FIT_ABORT;}
		if (e instanceof OEDataInvalidException) {return ETAS_RESCODE_DATA_INVALID;}
		if (e instanceof OEException) {return ETAS_RESCODE_GENERAL_ABORT;}
		if (e instanceof IOException) {return ETAS_RESCODE_IO_ERROR;}
		return ETAS_RESCODE_UNEXPECTED_ERROR;
	}




	//----- Communication area -----




	//--- Inputs from external environment

	// The execution timer, which contains the maximum run time, progress report time, and executor.

	public SimpleExecTimer exec_timer = null;


	//--- Outputs to external environment

	// The ETAS result code, see ETAS_RESCODE_XXXXX.

	public int etas_rescode = ETAS_RESCODE_NOT_ATTEMPTED;

	// The abort message from an exception, or null if none.

	public String abort_message = null;

	// Performance data from fitting.

	public final SimpleThreadLoopResult fit_perf_data = new SimpleThreadLoopResult();;

	// Performance data from simulation.

	public final SimpleThreadLoopResult sim_perf_data = new SimpleThreadLoopResult();;


	//--- Filenames for test outputs

	// Filename for writing the accepted list of ruptures, or null if not requested.

	public String filename_accepted = null;

	// Filename for writing the magnitude of completeness function, or null if not requested.

	public String filename_mag_comp = null;

	// Filename for writing the log-density grid, or null if not required.

	public String filename_log_density = null;

	// Filename for writing the integrated intensity function, or null if not required.

	public String filename_intensity_calc = null;

	// Filename for writing the ETAS results (including forecast JSON), or null if not requested.

	public String filename_results = null;

	// Filename for writing the forecast JSON, or null if not requested.

	public String filename_fc_json = null;




	// Report an exception, set the abort message and result code.
	// Usage note: The client can call this function when any ETAS function throws
	// an exception.  This design allows the client to wrap the entire ETAS operation
	// inside a try-catch block, to prevent any exception from propagating into
	// higher-level code.

	public void report_exception (Exception e) {
		etas_rescode = get_rescode_for_exception (e);
		abort_message = SimpleUtils.getStackTraceAsString (e);
		return;
	}




	// Set up the communication area.
	// Parameters:
	//  the_exec_timer = Execution timer, which contains the maximum run time, progress report time, and executor.
	// Note: The client needs to start the timer.

	public final void setup_comm_area (SimpleExecTimer the_exec_timer) {
		exec_timer = the_exec_timer;

		etas_rescode = ETAS_RESCODE_IN_PROGRESS;
		abort_message = null;
		return;
	}




	// Return true if the given result code indicates ETAS has completed successfully.

	public static boolean is_etas_successful (int rescode) {
		return rescode == ETAS_RESCODE_OK;
	}




	// Return true if ETAS has completed successfully.

	public final boolean is_etas_successful () {
		return etas_rescode == ETAS_RESCODE_OK;
	}




	// Return true if the given result code indicates ETAS has been attempted.

	public static boolean is_etas_attempted (int rescode) {
		return rescode >= 0;
	}




	// Return true if ETAS has been attempted.

	public final boolean is_etas_attempted () {
		return etas_rescode >= 0;
	}




	// Get the ETAS result code as a string.

	public final String get_rescode_as_string () {
		return etas_result_to_string (etas_rescode);
	}




	// Return true if there is an abort message.

	public final boolean has_abort_message () {
		return abort_message != null;
	}




	// Get the abort message, or null if none.

	public final String get_abort_message () {
		return abort_message;
	}




	// Make a string suitable for a log entry.
	// It is a one-line string, not ending in newline.
	// Note: This can be used for both success and failure.

	public final String make_etas_log_string () {
		StringBuilder result = new StringBuilder();

		// If ETAS was attempted ...

		if (is_etas_attempted()) {

			// If not successful, start with the result code

			if (!( is_etas_successful() )) {
				result.append (get_rescode_as_string() + ", ");
			}

			// Display the total time

			long seconds = (exec_timer.get_split_runtime() + 500L) / 1000L;
			result.append ("time = " + seconds + " s");

			// Display fitting performance

			result.append (", fit = {" + fit_perf_data.one_line_string() + "}");

			// Display simulation performance

			result.append (", sim = {" + sim_perf_data.one_line_string() + "}");
		}

		// Otherwise, ETAS was not attempted ...

		else {

			// Just display the result code

			result.append (get_rescode_as_string());
		}

		return result.toString();
	}



	// Make the log information.

	public final OEtasLogInfo make_etas_log_info () {
		OEtasLogInfo log_info = new OEtasLogInfo (etas_result_to_logtype (etas_rescode), make_etas_log_string(), abort_message);

		// If ETAS was attempted ...

		if (is_etas_attempted()) {

			// If successful, save global log info for ETAS success

			if (is_etas_successful()) {
				log_info.set_global_accum_success (
					etas_rescode,						// int rescode,
					exec_timer.get_split_runtime(),		// long total_time,
					fit_perf_data.elapsed_time,			// long fit_time,
					sim_perf_data.elapsed_time,			// long sim_time,
					fit_perf_data.used_memory,			// long fit_memory,
					sim_perf_data.used_memory			// long sim_memory
				);
			}

			// If not successful, save global log info for ETAS failure

			else {
				log_info.set_global_accum_failure (etas_rescode);
			}
		}

		return log_info;
	}




	// Make the log information for a given result code and log string.
	// Parameters:
	//  rescode = Result code, ETAS_RESCODE_XXXX.
	//  log_string = Log string, can be null or empty if none.
	// Note: This sets up log info for an ETAS failure prior to launching ETAS.

	public static OEtasLogInfo make_etas_log_info (int rescode, String log_string) {
		OEtasLogInfo log_info = new OEtasLogInfo (etas_result_to_logtype (rescode), log_string, null);
		log_info.set_global_accum_failure (rescode);
		return log_info;
	}




	//----- Input/Output area -----




	//--- Inputs to ETAS

	// ETAS parameters.  Must be non-null.

	public OEtasParameters etas_params = null;

	// Forecast information, used to create the forecast JSON.  Must be non-null.

	public USGS_ForecastInfo forecast_info = null;

	// Input catalog information.  Must be non-null.

	public OEtasCatalogInfo catalog_info = null;

	// The list of ruptures.  Must be non-null.

	public List<OERupture> rup_list = null;


	//--- Handling for compact or observed catalogs and returning rupture acceptance

	// Origin for converting between absolute and relative time and locations.
	// Can be null if no conversions are needed.

	public OEOrigin etas_origin = null;

	// Mainshock.
	// Can be null if none.

	public ObsEqkRupture obs_mainshock = null;

	// The list of aftershocks/foreshocks, if it is supplied as a compact list, or null if not.
	// This list should not include the mainshock, which is supplied separately.

	public CompactEqkRupList compact_rup_list = null;

	// The list of aftershocks/foreshocks, if it is supplied as an observed list, or null if not.
	// This list should not include the mainshock, which is supplied separately.

	public List<ObsEqkRupture> obs_rup_list = null;

	// Mapping from compact_rup_list or obs_rup_list to rup_list.
	// For each i, import_rup_index[i] is the index into rup_list where compact_rup_list[i]
	// or obs_rup_list[i] is located, or -1 if it is not in rup_list.

	private int[] import_rup_index = null;


	//--- Derived values, for later use

	// The earliest time among ruptures, in days, but not later than catalog_info.t_data_begin.

	private double rup_t_early;

	// The largest magnitude among ruptures, but not less than catalog_info.magCat.

	private double rup_mag_top;

	// The largest magnitude among ruptures used for fitting, but not less than catalog_info.magCat.

	private double fit_rup_mag_top;

	// The list of aftershocks to use for building the forecast JSON.

	private List<ObsEqkRupture> aftershocks_for_json = null;


	//--- Outputs from ETAS

	// ETAS results, we create this object.

	public OEtasResults etas_results = null;

	// The forecast grid from the simulator, we create this object.

	public OEForecastGrid sim_forecast_grid = null;




	// Make a rupture list from a compact catalog.
	// On entry, the following must be set up:
	//  catalog_info
	//  etas_origin
	//  obs_mainshock (can be null)
	//  compact_rup_list
	// On exit, rup_list and import_rup_index are set up.
	// Note: This function filters the contents of compact_rup_list to include only
	// ruptures in the time interval catalog_info.t_data_begin to catalog_info.t_data_end.

	private void make_rup_list_from_compact () {

		// Start the rupture list

		rup_list = new ArrayList<OERupture>();
		
		// Make the index array

		int n_rup = compact_rup_list.size();
		import_rup_index = new int[n_rup];

		// The low and high time limits, allowing a bit of slack

		final double t_lo = catalog_info.t_data_begin - OEConstants.FIT_TIME_EPS;
		final double t_hi = catalog_info.t_data_end + OEConstants.FIT_TIME_EPS;

		// Loop over ruptures

		for (int n = 0; n < n_rup; ++n) {

			// Convert to an ETAS rupture

			OERupture rup = new OERupture();
			etas_origin.convert_compact_to_etas (compact_rup_list, n, rup);

			// If it is within the time interval, add to the list

			if (rup.t_day >= t_lo && rup.t_day <= t_hi) {
				import_rup_index[n] = rup_list.size();
				rup_list.add (rup);
			}

			// Otherwise, this rupture is not in the list

			else {
				import_rup_index[n] = -1;
			}
		}

		// If there is a mainshock, add to the end of the list

		if (obs_mainshock != null) {
			++n_rup;
			OERupture rup = new OERupture();
			etas_origin.convert_obs_to_etas (obs_mainshock, rup);
			rup_list.add (rup);
		}

		// Report

		System.out.println();
		System.out.println ("Number of ruptures supplied: " + n_rup);
		System.out.println ("Number of ruptures passed to ETAS code: " + rup_list.size());

		return;
	}




	// Make a rupture list from an observed catalog.
	// On entry, the following must be set up:
	//  catalog_info
	//  etas_origin
	//  obs_mainshock (can be null)
	//  obs_rup_list
	// On exit, rup_list and import_rup_index are set up.
	// Note: This function filters the contents of obs_rup_list to include only
	// ruptures in the time interval catalog_info.t_data_begin to catalog_info.t_data_end.

	private void make_rup_list_from_obs () {

		// Start the rupture list

		rup_list = new ArrayList<OERupture>();
		
		// Make the index array

		int n_rup = obs_rup_list.size();
		import_rup_index = new int[n_rup];

		// The low and high time limits, allowing a bit of slack

		final double t_lo = catalog_info.t_data_begin - OEConstants.FIT_TIME_EPS;
		final double t_hi = catalog_info.t_data_end + OEConstants.FIT_TIME_EPS;

		// Loop over ruptures

		for (int n = 0; n < n_rup; ++n) {

			// Convert to an ETAS rupture

			OERupture rup = new OERupture();
			etas_origin.convert_obs_to_etas (obs_rup_list.get (n), rup);

			// If it is within the time interval, add to the list

			if (rup.t_day >= t_lo && rup.t_day <= t_hi) {
				import_rup_index[n] = rup_list.size();
				rup_list.add (rup);
			}

			// Otherwise, this rupture is not in the list

			else {
				import_rup_index[n] = -1;
			}
		}

		// If there is a mainshock, add to the end of the list

		if (obs_mainshock != null) {
			++n_rup;
			OERupture rup = new OERupture();
			etas_origin.convert_obs_to_etas (obs_mainshock, rup);
			rup_list.add (rup);
		}

		// Report

		System.out.println();
		System.out.println ("Number of ruptures supplied: " + n_rup);
		System.out.println ("Number of ruptures passed to ETAS code: " + rup_list.size());

		return;
	}




	// Set the acceptance flags and/or write file of accepted ruptures.
	// Parameters:
	//  a_acceptance = Acceptance flags for each rupture in rup_list.
	// This function does nothing if there is no compact_rup_list or obs_rup_list.
	// This function also writes the accepted catalog to a file, if requested.

	private void set_rup_list_acceptance (boolean[] a_acceptance) throws IOException {

		// If there is a compact list ...

		if (compact_rup_list != null) {

			// Scan list and set acceptance flags

			int n_accepted = 0;
			final int n_rup = compact_rup_list.size();

			for (int n = 0; n < n_rup; ++n) {
				int index = import_rup_index[n];
				if (index >= 0 && a_acceptance[index]) {
					compact_rup_list.set_acceptance (n, true);
					++n_accepted;
				}
			}

			// Report

			System.out.println();
			System.out.println ("Number of ruptures marked as accepted: " + n_accepted);
		}

		// If there is a compact or observed list, and file output is requested ...

		List<ObsEqkRupture> my_list = null;
		if (compact_rup_list != null) {
			my_list = compact_rup_list;
		}
		else if (obs_rup_list != null) {
			my_list = obs_rup_list;
		}

		if (my_list != null && filename_accepted != null) {

			// Make a list of ruptures containing the accepted ones

			final int n_rup = my_list.size();
			ArrayList<ObsEqkRupture> accepted_list = new ArrayList<ObsEqkRupture>();

			for (int n = 0; n < n_rup; ++n) {
				int index = import_rup_index[n];
				if (index >= 0 && a_acceptance[index]) {
					accepted_list.add (my_list.get (n));
				}
			}

			// Check if the mainshock is accepted (it should be)

			int n_accepted = accepted_list.size();

			ObsEqkRupture my_mainshock = null;
			if (obs_mainshock != null) {
				int index = rup_list.size() - 1;	// index where mainshock is, which is the last element
				if (a_acceptance[index]) {
					my_mainshock = obs_mainshock;
					++n_accepted;
				}
			}

			// Set up for an external catalog

			GUIExternalCatalog ext_cat = new GUIExternalCatalog();

			ext_cat.setup_catalog (
				accepted_list,
				my_mainshock
			);

			// Write the file

			try {
				ext_cat.write_to_file (filename_accepted);
			}
			catch (Exception e) {
				throw new IOException ("Error writing accepted catalog file: " + filename_accepted, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote list of " + n_accepted + " accepted ruptures to file: " + filename_accepted);
		}

		return;
	}




	// Finish setting up the input/output area.
	// On entry, the inputs to ETAS must be set up.
	// On return, the derived values are calculated, and the outputs from ETAS are initialized.

	private void finish_setup_input_area () {

		// Initialize for calculating earliest time and largest magnitude

		rup_t_early = catalog_info.t_data_begin;
		rup_mag_top = catalog_info.magCat;
		fit_rup_mag_top = catalog_info.magCat;

		// Scan list of ruptures

		final double t_fit = catalog_info.t_fitting + OEConstants.FIT_TIME_EPS;

		for (OERupture rup : rup_list) {
			final double t = rup.t_day;
			final double mag = rup.rup_mag;
			if (rup_t_early > t) {
				rup_t_early = t;
			}
			if (rup_mag_top < mag) {
				rup_mag_top = mag;
			}
			if (t > t_fit) {
				if (fit_rup_mag_top < mag) {
					fit_rup_mag_top = mag;
				}
			}
		}

		// Pick the list to use for inserting aftershock counts in the JSON
		// (If none is available, possibly one could be created from rup_list,
		// but that would require an OEOrigin to use.)

		if (compact_rup_list != null) {
			aftershocks_for_json = compact_rup_list;
		}
		else if (obs_rup_list != null) {
			aftershocks_for_json = obs_rup_list;
		}
		else {
			aftershocks_for_json = new ArrayList<ObsEqkRupture>();
		}

		// Initialize ETAS results

		etas_results = new OEtasResults();

		// Insert input data into ETAS results

		etas_results.set_inputs (catalog_info);

		// Report

		System.out.println();
		System.out.println (etas_params.toString());

		System.out.println();
		System.out.println (catalog_info.toString());

		return;
	}




	// Finish ETAS results.

	private void finish_etas_results () throws IOException {

		// If file output is desired ...

		if (filename_results != null) {

			// Write to file

			try {
				MarshalUtils.to_formatted_json_file (etas_results, filename_results);
			}
			catch (Exception e) {
				throw new IOException ("Error writing ETAS results file: " + filename_results, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote ETAS results to file: " + filename_results);
		}

		// If we want to write out the forecast JSON ...

		if (filename_fc_json != null) {

			// Write to file

			try {
				SimpleUtils.write_string_as_file (filename_fc_json, etas_results.etas_json);
			}
			catch (Exception e) {
				throw new IOException ("Error writing forecast JSON file: " + filename_fc_json, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote forecast JSON to file: " + filename_fc_json);
		}

		// Report

		System.out.println();
		System.out.println (etas_results.to_display_string());

		return;
	}




	// Set up the input/output area, with the catalog given in compact format.
	// Parameters:
	//  the_etas_params = ETAS parameters.  Must be non-null.
	//  the_forecast_info = Forecast information, used to create the forecast JSON.  Must be non-null.
	//  the_catalog_info = Input catalog information.  Must be non-null.
	//  the_obs_mainshock = Mainshock.  Must be non-null.
	//  the_compact_rup_list = The list of aftershocks/foreshocks.  Must be non-null.

	public void setup_input_area_from_compact (
		OEtasParameters the_etas_params,
		USGS_ForecastInfo the_forecast_info,
		OEtasCatalogInfo the_catalog_info,
		ObsEqkRupture the_obs_mainshock,
		CompactEqkRupList the_compact_rup_list
	) {
		// Save copies of parameters

		etas_params = (new OEtasParameters()).copy_from (the_etas_params);
		forecast_info = (new USGS_ForecastInfo()).copy_from (the_forecast_info);
		catalog_info = (new OEtasCatalogInfo()).copy_from (the_catalog_info);

		// Save supplied catalog (not copying)

		obs_mainshock = the_obs_mainshock;
		compact_rup_list = the_compact_rup_list;

		// Set origin at time and location of mainshock

		etas_origin = (new OEOrigin()).set_from_rupture (obs_mainshock);

		// Set up the ETAS rupture List

		make_rup_list_from_compact();

		// Finish setup

		finish_setup_input_area();

		return;
	}




	// Set up the input/output area, with the catalog given in observed format.
	// Parameters:
	//  the_etas_params = ETAS parameters.  Must be non-null.
	//  the_forecast_info = Forecast information, used to create the forecast JSON.  Must be non-null.
	//  the_catalog_info = Input catalog information.  Must be non-null.
	//  the_obs_mainshock = Mainshock.  Must be non-null.
	//  the_obs_rup_list = The list of aftershocks/foreshocks.  Must be non-null.

	public void setup_input_area_from_obs (
		OEtasParameters the_etas_params,
		USGS_ForecastInfo the_forecast_info,
		OEtasCatalogInfo the_catalog_info,
		ObsEqkRupture the_obs_mainshock,
		List<ObsEqkRupture> the_obs_rup_list
	) {
		// Save copies of parameters

		etas_params = (new OEtasParameters()).copy_from (the_etas_params);
		forecast_info = (new USGS_ForecastInfo()).copy_from (the_forecast_info);
		catalog_info = (new OEtasCatalogInfo()).copy_from (the_catalog_info);

		// Save supplied catalog (not copying)

		obs_mainshock = the_obs_mainshock;
		obs_rup_list = the_obs_rup_list;

		// Set origin at time and location of mainshock

		etas_origin = (new OEOrigin()).set_from_rupture (obs_mainshock);

		// Set up the ETAS rupture List

		make_rup_list_from_obs();

		// Finish setup

		finish_setup_input_area();

		return;
	}




	// Set up the input/output area, with the catalog given in observed format.
	// Parameters:
	//  the_etas_params = ETAS parameters.  Must be non-null.
	//  the_forecast_info = Forecast information, used to create the forecast JSON.  Must be non-null.
	//  the_catalog_info = Input catalog information.  Must be non-null.
	//  the_etas_origin = Origin for converting between absolute and relative time and locations.  Must be non-null.
	//  the_obs_rup_list = The list of earthquakes (including mainshock, if any).  Must be non-null.
	// Note: In this version, the rupture list contains all earthquake, and an origin object is supplied
	// to specify the absolute-relative conversions (instead of the origin being implied by the mainshock).

	public void setup_input_area_from_obs (
		OEtasParameters the_etas_params,
		USGS_ForecastInfo the_forecast_info,
		OEtasCatalogInfo the_catalog_info,
		OEOrigin the_etas_origin,
		List<ObsEqkRupture> the_obs_rup_list
	) {
		// Save copies of parameters

		etas_params = (new OEtasParameters()).copy_from (the_etas_params);
		forecast_info = (new USGS_ForecastInfo()).copy_from (the_forecast_info);
		catalog_info = (new OEtasCatalogInfo()).copy_from (the_catalog_info);

		// Save supplied catalog (not copying)

		obs_mainshock = null;
		obs_rup_list = the_obs_rup_list;

		// Set origin at time and location of mainshock

		etas_origin = (new OEOrigin()).copy_from (the_etas_origin);

		// Set up the ETAS rupture List

		make_rup_list_from_obs();

		// Finish setup

		finish_setup_input_area();

		return;
	}




	// Set up the input/output area, with the catalog given in ETAS format.
	// Parameters:
	//  the_etas_params = ETAS parameters.  Must be non-null.
	//  the_forecast_info = Forecast information, used to create the forecast JSON.  Must be non-null.
	//  the_catalog_info = Input catalog information.  Must be non-null.
	//  the_rup_list = The list of ruptures.  Must be non-null.

	public void setup_input_area_from_etas (
		OEtasParameters the_etas_params,
		USGS_ForecastInfo the_forecast_info,
		OEtasCatalogInfo the_catalog_info,
		List<OERupture> the_rup_list
	) {
		// Save copies of parameters

		etas_params = (new OEtasParameters()).copy_from (the_etas_params);
		forecast_info = (new USGS_ForecastInfo()).copy_from (the_forecast_info);
		catalog_info = (new OEtasCatalogInfo()).copy_from (the_catalog_info);

		// Save supplied catalog (copy the list but retain the rupture objects)

		rup_list = new ArrayList<OERupture>(the_rup_list);

		// Finish setup

		finish_setup_input_area();

		return;
	}




	// Get the forecast summary table.  (Available only after a successful completion.)

	public String get_forecast_summary_string () {
		return sim_forecast_grid.summary_string();
	}




	//----- History -----




	// History parameters.

	private OEDiscFGHParams hist_params = null;

	// History.

	private OEDisc2History history = null;




	// Make the history.

	private void make_history () throws IOException {

		// Make the history parameters

		hist_params = etas_params.make_hist_params (
			catalog_info,
			rup_t_early
		);

		// Display the history parameters

		System.out.println ();
		System.out.println (hist_params.toString());

		// Make a history

		history = new OEDisc2History();

		history.build_from_fgh (hist_params, rup_list);

		// Display the history

		System.out.println ();
		System.out.println (history.toString());

		// Save history into to the results

		etas_results.set_history (history);

		// Process acceptance information

		set_rup_list_acceptance (history.a_acceptance);

		// If we want to write the magnitude of completeness function ...

		if (filename_mag_comp != null) {

			// Get the magnitude of completeness function as a String

			String mag_comp_fcn = history.dump_raw_mc_to_string();

			// Write the file

			try {
				SimpleUtils.write_string_as_file (filename_mag_comp, mag_comp_fcn);
			}
			catch (Exception e) {
				throw new IOException ("Error writing magnitude of completeness file: " + filename_mag_comp, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote magnitude of completeness function to file: " + filename_mag_comp);
		}

		return;
	}




	//----- Fitting -----




	// Magnitude range to use for fitting.

	private OECatalogParamsMags fit_params_mags = null;

	// Grid ranges to use for fitting.

	private OEGridParams grid_params = null;

	// Fitting information.

	private OEDisc2InitFitInfo fit_info = null;




	// Do parameter fitting.

	private OEDisc2InitVoxSet do_fitting () throws OEException, IOException {

		//  // Get the top magnitude for fitting
		//  
		//  double fit_mag_top = catalog_info.magTop;
		//  
		//  // If it wasn't supplied, use the maximum over the entire catalog
		//  
		//  if (fit_mag_top <= catalog_info.magCat + OEConstants.FIT_MAG_EPS) {
		//  	fit_mag_top = rup_mag_top;
		//  }
		//  
		//  // If it was supplied, make it at least the maximum in the portion of the catalog used for fitting
		//  
		//  else {
		//  	fit_mag_top = Math.max (fit_mag_top, fit_rup_mag_top);
		//  }
		//  
		//  // Always make it at least a minimum amount above the history's magCat
		//  
		//  fit_mag_top = Math.max (fit_mag_top, history.magCat + OEConstants.GEN_MIN_MAG_RANGE);
		//  
		//  // Make the magnitude range
		//  // Use defaults for the reference magnitude and maximum considered magnitude.
		//  // Use the history's magCat for the minimum simulation magnitude, which allows combining intervals with mc == magCat.
		//  
		//  fit_params_mags = new OECatalogParamsMags (
		//  	OEConstants.DEF_MREF,		// mref
		//  	OEConstants.DEF_MSUP,		// msup
		//  	history.magCat,				// mag_min_sim
		//  	fit_mag_top					// mag_max_sim
		//  );

		// Make the magnitude range

		fit_params_mags = etas_params. get_fmag_range (
			catalog_info.magCat,	// cat_magCat
			catalog_info.magTop,	// cat_magTop
			rup_mag_top,			// rup_mag_top
			fit_rup_mag_top,		// fit_rup_mag_top
			history.magCat			// hist_magCat
		);

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_intervals = etas_params.get_fit_f_interval();
		boolean f_likelihood = true;
		int lmr_opt = etas_params.get_fit_lmr_opt();
		boolean f_background = etas_params.get_fit_f_background();

		fitter.dfit_build (history, fit_params_mags, f_intervals, f_likelihood, lmr_opt, f_background);

		// Set up grouping

		OEDisc2Grouping.SpanWidthFcn span_width_fcn = etas_params.get_span_width_fcn ();
		OEDisc2Grouping.RupWidthFcn rup_width_fcn = etas_params.get_rup_width_fcn ();

		fitter.setup_grouping (span_width_fcn, rup_width_fcn);

		// Set up time interval for branch ratio calculation

		double tint_br = etas_params.get_tint_br (catalog_info.t_forecast - catalog_info.t_fitting);

		fitter.set_tint_br (tint_br);

		// If we want the integrated intensity function, set flag to save required data

		if (filename_intensity_calc != null) {
			fitter.set_f_intensity (true);
		}

		// Pass grid options into the fitter

		OEGridOptions grid_options = etas_params.make_grid_options();

		fitter.set_grid_options (grid_options);

		// Display fitter info

		System.out.println();
		System.out.println (fitter.toString());

		// Create the grid ranges

		grid_params = etas_params.make_grid_params ();

		// Display grid range info

		System.out.println();
		System.out.println (grid_params.toString());

		// Get the Bayesian prior

		OEBayFactoryParams bay_factory_params = catalog_info.get_bay_factory_params();

		OEBayPrior bay_prior = etas_params.get_bay_prior (bay_factory_params);

		// Make the voxel set

		OEDisc2InitVoxSet voxel_set = new OEDisc2InitVoxSet();

		// Make the voxel builder

		OEDisc2InitVoxBuilder voxel_builder = new OEDisc2InitVoxBuilder();

		// Set up shared objects in the builder

		voxel_builder.setup_vbld (
			voxel_set,
			fitter,
			bay_prior
		);

		// Set up grid ranges in the builder

		voxel_builder.setup_grid (
			grid_params
		);

		// Display builder info

		System.out.println();
		System.out.println (voxel_builder.toString());

		// Build the voxels (throw exception if error or timeout)

		voxel_builder.set_upstream_loop_result (fit_perf_data);

		voxel_builder.build_voxels (exec_timer);

		// Get and display the performance data

		//fit_perf_data = voxel_builder.get_loop_result();

		System.out.println();
		System.out.println (fit_perf_data.toString());

		// Get the fitting information

		fit_info = voxel_set.get_fit_info();

		// Create prototype catalog parameters, with statistics zeroed out

		double sim_tbegin = fit_info.group_t_interval_end;
		double my_t_forecast = Math.max (fit_info.group_t_interval_end, catalog_info.t_forecast);	// ensure t_forecast >= tbegin
		double sim_tend = my_t_forecast + 365.0;

		OECatalogParams proto_cat_params = new OECatalogParams();
		proto_cat_params.set_to_fixed_mag_limited (
			0.0,				// a
			0.0,				// p
			0.0,				// c
			0.0,				// b
			0.0,				// alpha
			fit_info.mref,		// mref
			fit_info.msup,		// msup
			sim_tbegin,			// tbegin
			sim_tend,			// tend
			fit_info.mag_min,	// mag_min_sim
			fit_info.mag_max	// mag_max_sim
		);

		// Statistics accumulator

		boolean f_full_marginal = true;		// eventually the caller needs to control this, but it likely adds less than one second to make the full marginal

		OEDisc2VoxStatAccumMarginal stat_accum_slim = new OEDisc2VoxStatAccumMarginal (grid_params, false, false);

		OEDisc2VoxStatAccumMarginal stat_accum_full = null;
		if (f_full_marginal) {
			stat_accum_full = new OEDisc2VoxStatAccumMarginal (grid_params, true, false);
		}

		OEDisc2VoxStatAccum stat_accum = (new OEDisc2VoxStatAccumMulti (stat_accum_slim, stat_accum_full)).get_stat_accum();

		// Complete setting up the voxel set

		double bay_weight = etas_params.get_bay_weight (catalog_info.t_data_end - catalog_info.t_data_begin);

		voxel_set.setup_post_fitting (
			proto_cat_params,						// the_cat_params
			my_t_forecast,							// the_t_forecast
			bay_weight,								// the_bay_weight
			etas_params.get_density_bin_size_lnu(),	// density_bin_size_lnu
			etas_params.get_density_bin_count(),	// density_bin_count
			etas_params.get_prob_tail_trim(),		// prob_tail_trim
			etas_params.get_seed_subvox_count(),	// the_seed_subvox_count
			stat_accum								// stat_accum
		);

		// Display voxel set results

		System.out.println();
		System.out.println (voxel_set.toString());

		// Save fitting results

		etas_results.set_fitting (fit_info, bay_prior);

		etas_results.set_grid (voxel_set);

		boolean save_marginals = true;
		OEMarginalDistSet marginals = stat_accum_slim.get_dist_set();
		boolean save_full_marginals = false;		// maybe the caller should control this
		OEMarginalDistSet full_marginals = null;
		if (stat_accum_full != null) {
			full_marginals = stat_accum_full.get_dist_set();
		}
		etas_results.set_marginals (
			save_marginals,
			marginals,
			save_full_marginals,
			full_marginals
		);

		// If we want to write the log-density grid ...

		if (filename_log_density != null) {

			System.out.println();
			System.out.println ("Writing log-density grid to file");

			// Write the file

			try {
				voxel_set.dump_log_density_to_file (filename_log_density);
			}
			catch (Exception e) {
				throw new IOException ("Error writing log-density grid file: " + filename_log_density, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote log-density grid to file: " + filename_log_density);
		}

		// If we want to write the integrated intensity function ...

		if (filename_intensity_calc != null) {

			System.out.println();

			// Write the file

			try {
				voxel_set.write_integrated_intensity_to_file (
					filename_intensity_calc,
					OEConstants.DEF_INTEGRATED_LAMBDA_RES,
					history,
					exec_timer
				);
			}
			catch (Exception e) {
				throw new IOException ("Error writing integrated intensity function file: " + filename_intensity_calc, e);
			}

			// Report

			System.out.println();
			System.out.println ("Wrote interated intensity function to file: " + filename_intensity_calc);
		}

		// Discard the fitter

		voxel_builder = null;
		fitter = null;

		// Return the voxel set to use for initializing simulations

		return voxel_set;
	}




	//----- Simulation -----




	// Simulation parameters.

	private OESimulationParams sim_parameters = null;




	// Do simulation.

	private void do_simulation (OEEnsembleInitializer sim_initializer) throws OEException, IOException {

		// Create the simulation parameters

		sim_parameters = etas_params.get_sim_params();

		// Display them

		System.out.println();
		System.out.println (sim_parameters.toString());

		// Run the simulation

		OESimulator simulator = new OESimulator();

		simulator.set_upstream_loop_result (sim_perf_data);

		simulator.run_simulation_ex (
			sim_initializer,
			sim_parameters,
			exec_timer
		);

		// Get and display the performance data

		//sim_perf_data = simulator.get_loop_result();

		System.out.println();
		System.out.println (sim_perf_data.toString());

		// Save simulation results

		etas_results.set_simulation (simulator);

		// Get the forecast grid from the simulator

		sim_forecast_grid = simulator.sim_forecast_grid;

		// Get the JSON String

		String json_string = forecast_info.make_forecast_json (sim_forecast_grid, aftershocks_for_json, null);
		if (json_string == null) {
			throw new OESimForecastException ("Unable to generate JSON for ETAS forecast");
		}

		etas_results.set_forecast (json_string);

		// Discard the simulator

		simulator = null;

		return;
	}




	//----- Top level -----




	// Run the ETAS system.
	// Before calling this function, you must:
	// - Call setup_comm_area to supply the execution timer.
	// - Supply filenames for any desired output files (used for testing).
	// - Call one of the setup_input_area_xxxx functions to supply inputs.
	// On return, outputs are available in the communication area and the input/output area.

	public void run_etas () throws OEException, IOException {

		// Make history

		make_history();

		// Run parameter fitting

		OEDisc2InitVoxSet voxel_set = do_fitting();

		// Run simulation

		do_simulation (voxel_set);

		// Discard the voxel set

		voxel_set = null;

		// Finish forming ETAS results

		finish_etas_results();

		// Success

		etas_rescode = ETAS_RESCODE_OK;

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEExecEnvironment");







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
