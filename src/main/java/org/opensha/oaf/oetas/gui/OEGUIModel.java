package org.opensha.oaf.oetas.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
//import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.chart.ui.RectangleEdge;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DefaultXY_DataSet;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.data.function.HistogramFunction;
import org.opensha.commons.data.function.XY_DataSet;
import org.opensha.commons.data.function.XY_DatasetBinner;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.GenericRJ_Parameters;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;
import org.opensha.oaf.rj.MagCompFn;
import org.opensha.oaf.rj.MagCompPage_Parameters;
import org.opensha.oaf.rj.MagCompPage_ParametersFetch;
import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.RJ_AftershockModel;
import org.opensha.oaf.rj.RJ_AftershockModel_Bayesian;
import org.opensha.oaf.rj.RJ_AftershockModel_Generic;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;
import org.opensha.oaf.rj.SearchMagFn;
import org.opensha.oaf.rj.SearchRadiusFn;
import org.opensha.oaf.rj.SeqSpecRJ_Parameters;
import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_AftershockForecast.Duration;
import org.opensha.oaf.rj.USGS_AftershockForecast.Template;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
//import org.opensha.commons.param.Parameter;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupListCalc;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.faultSurface.FaultTrace;
import org.opensha.sha.faultSurface.RuptureSurface;
//import org.opensha.sha.gui.infoTools.CalcProgressBar;
import org.opensha.sha.magdist.GutenbergRichterMagFreqDist;
import org.opensha.sha.magdist.IncrementalMagFreqDist;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;
import java.awt.Font;

import gov.usgs.earthquake.product.Product;
//import org.opensha.oaf.pdl.OAF_Publisher;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.AutoExecutorService;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;

import java.io.BufferedWriter;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ServerCmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ServerClock;

import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.SimpleUtils;


// Operational ETAS GUI - Model implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the operational ETAS model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the model.
// It holds the earthquake information, catalog, and forecasts.


public class OEGUIModel extends OEGUIComponent {


	//----- Model state -----

	// The model proceeds through a series of states.
	// Initial state: The model is empty.
	// Catalog state: The model contains a mainshock, a search region,
	//  a list of aftershocks, and a generic model (which is used to
	//  obtain default parameters).
	// Aftershock parameter state: The model contains a sequence specific
	//  model, and, if applicable, a Bayesian model.
	// Forecast state: The model contains foreacast tables.


	// The model state.

	private int modstate;


	// Return the current model state.

	public final int get_modstate () {
		return modstate;
	}


	// Return the current model state as a string.

	public final String cur_modstate_string () {
		return get_modstate_as_string (modstate);
	}


	// Return true if the model state includes a catalog.

	public final boolean modstate_has_catalog () {
		return modstate >= MODSTATE_CATALOG;
	}


	// Return true if the model state includes aftershock parameters.

	public final boolean modstate_has_aftershock_params () {
		return modstate >= MODSTATE_PARAMETERS;
	}


	// Return true if the model state includes a forecast.

	public final boolean modstate_has_forecast () {
		return modstate >= MODSTATE_FORECAST;
	}




	// Advance the model state.
	// Parameters:
	//  new_modstate = Desired new model state.
	// Returns true if state has changed, false if not.
	// Requires:  modstate <= new_modstate <= modstate + 1
	// In other words, you can only advance one state at a time,
	// and this function is idempotent.
	// The model must be set up with all information required by
	// the new state before calling this function.
	// This function must be called on the EDT.

	public final boolean advance_modstate (int new_modstate) throws GUIEDTException {

		// Check for valid state

		if (!( new_modstate >= MODSTATE_MIN && new_modstate <= MODSTATE_MAX )) {
			throw new IllegalArgumentException ("OEGUIModel.advance_modstate - Invalid new model state: " + get_modstate_as_string(new_modstate));
		}

		// Check for valid transition

		if (!( new_modstate >= modstate && new_modstate <= modstate + 1 )) {
			throw new IllegalStateException ("OEGUIModel.advance_modstate - Invalid model state transition: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
		}

		// If state is not changing, just return

		if (new_modstate == modstate) {
			return false;
		}

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ advance_modstate: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
		}

		// Set the new state

		int old_modstate = modstate;
		modstate = new_modstate;

		// Notify the view

		gui_view.view_advance_state (old_modstate, new_modstate);

		// Return state changed

		return true;
	}




	// Retreat the model state.
	// Parameters:
	//  new_modstate = Desired new model state.
	// Returns true if state has changed, false if not.
	// Requires:  new_modstate <= modstate
	// In other words, you can retreat any number of states,
	// but cannot advance the state, and this function is idempotent.
	// This function must be called on the EDT.

	public final boolean retreat_modstate (int new_modstate) throws GUIEDTException {

		// Check for valid state

		if (!( new_modstate >= MODSTATE_MIN && new_modstate <= MODSTATE_MAX )) {
			throw new IllegalArgumentException ("OEGUIModel.retreat_modstate - Invalid new model state: " + get_modstate_as_string(new_modstate));
		}

		// Check for valid transition

		if (!( new_modstate <= modstate )) {
			throw new IllegalStateException ("OEGUIModel.retreat_modstate - Invalid model state transition: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
		}

		// If state is not changing, just return

		if (new_modstate == modstate) {
			return false;
		}

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ retreat_modstate: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
		}

		// Set the new state

		int old_modstate = modstate;
		modstate = new_modstate;

		// Clear data structures not valid in the new state

		clear_to_state();

		// Notify the view

		gui_view.view_retreat_state (old_modstate, new_modstate);

		// Return state changed

		return true;
	}




	// Return true if it is possible to retreat to the given model state.
	// Parameters:
	//  new_modstate = Desired new model state.
	// Returns true if it is possible to retreat to new_modstate, false if not.
	// Returns true if new_modstate <= modstate.
	// In other words, you can retreat any number of states,
	// but cannot advance the state.

	public final boolean can_retreat_modstate (int new_modstate) throws GUIEDTException {
		return new_modstate <= modstate;
	}
	




	//----- Model data structures -----


	// The mainshock structure.
	// Available when model state >= MODSTATE_CATALOG.

	private ForecastMainshock fcmain;

	public final ForecastMainshock get_fcmain () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.fcmain while in state " + cur_modstate_string());
		}
		return fcmain;
	}

	// The AAFS parameters for the mainshock.
	// This contains all default parameters for the mainshock, except the search region.
	// Available when model state >= MODSTATE_CATALOG.

	private ForecastParameters aafs_fcparams;

	// The parameters used during the fetch operation.
	// Available when model state >= MODSTATE_CATALOG.

	private ForecastParameters fetch_fcparams;

	// Custom search region, or null if none.
	// Available when model state >= MODSTATE_CATALOG.

	private SphRegion custom_search_region;

	// Analyst injectable text, or null if none.
	// Available when model state >= MODSTATE_CATALOG.
	// An empty string means no injectable text.

	private String analyst_inj_text;

	public final String get_analyst_inj_text () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.analyst_inj_text while in state " + cur_modstate_string());
		}
		return analyst_inj_text;
	}

	// True if the stored catalog is fetched from Comcat.
	// Available when model state >= MODSTATE_CATALOG.

	private boolean has_fetched_catalog;

	public final boolean get_has_fetched_catalog () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.has_fetched_catalog while in state " + cur_modstate_string());
		}
		return has_fetched_catalog;
	}



	// The search region, or null if none.
	// It is null if a catalog is loaded from a file.
	// Available when model state >= MODSTATE_CATALOG.

	public final SphRegion get_cur_region () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_region while in state " + cur_modstate_string());
		}
		return fetch_fcparams.aftershock_search_region;
	}

	// The mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	private ObsEqkRupture cur_mainshock;

	public final ObsEqkRupture get_cur_mainshock () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_mainshock while in state " + cur_modstate_string());
		}
		return cur_mainshock;
	}

	// The list of aftershocks.
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.

	private ObsEqkRupList cur_aftershocks;

	public final ObsEqkRupList get_cur_aftershocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_aftershocks while in state " + cur_modstate_string());
		}
		return cur_aftershocks;
	}


	// The sequence-specific model.
	// Available when model state >= MODSTATE_CATALOG.
	// It is non-null when state >= MODSTATE_PARAMETERS.
	
	private RJ_AftershockModel_SequenceSpecific cur_model = null;

	public final RJ_AftershockModel_SequenceSpecific get_cur_model () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_model while in state " + cur_modstate_string());
		}
		return cur_model;
	}

	// The generic parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final GenericRJ_Parameters get_genericParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericParams while in state " + cur_modstate_string());
		}
		return aafs_fcparams.generic_params;
	}

	// The generic model for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	private RJ_AftershockModel_Generic genericModel = null;

	public final RJ_AftershockModel_Generic get_genericModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericModel while in state " + cur_modstate_string());
		}
		return genericModel;
	}

	// The magnitude-of-completeness parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final MagCompPage_Parameters get_magCompParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.magCompParams while in state " + cur_modstate_string());
		}
		return aafs_fcparams.mag_comp_params;
	}

	// The sequence-specific parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final SeqSpecRJ_Parameters get_seqSpecParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.seqSpecParams while in state " + cur_modstate_string());
		}
		return aafs_fcparams.seq_spec_params;
	}

	// The Bayesian model.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.
	
	private RJ_AftershockModel_Bayesian bayesianModel = null;

	public final RJ_AftershockModel_Bayesian get_bayesianModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.bayesianModel while in state " + cur_modstate_string());
		}
		return bayesianModel;
	}

	// Magnitude-number distribution of the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private IncrementalMagFreqDist aftershockMND = null;

	public final IncrementalMagFreqDist get_aftershockMND () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.aftershockMND while in state " + cur_modstate_string());
		}
		return aftershockMND;
	}

	// Magnitude where the magnitude-number distribtion peaks.
	// Available when model state >= MODSTATE_CATALOG.

	private double mnd_mmaxc;

	public final double get_mnd_mmaxc () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.mnd_mmaxc while in state " + cur_modstate_string());
		}
		return mnd_mmaxc;
	}

	// The generic forecast.
	// Available when model state >= MODSTATE_FORECAST.

	private USGS_AftershockForecast genericForecast = null;

	public final USGS_AftershockForecast get_genericForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericForecast while in state " + cur_modstate_string());
		}
		return genericForecast;
	}

	// The sequence-specific forecast.
	// Available when model state >= MODSTATE_FORECAST.

	private USGS_AftershockForecast seqSpecForecast = null;

	public final USGS_AftershockForecast get_seqSpecForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to OEGUIModel.seqSpecForecast while in state " + cur_modstate_string());
		}
		return seqSpecForecast;
	}

	// The bayesian forecast.
	// Available when model state >= MODSTATE_FORECAST.
	// Will be null if there is no bayesian model.

	private USGS_AftershockForecast bayesianForecast = null;

	public final USGS_AftershockForecast get_bayesianForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to OEGUIModel.bayesianForecast while in state " + cur_modstate_string());
		}
		return bayesianForecast;
	}



	// Minimum, default, and maximum forecast duration.

	private long min_fc_duration;
	private long def_fc_duration;
	private long max_fc_duration;

	public double get_min_fc_duration_days () {
		if (min_fc_duration % SimpleUtils.DAY_MILLIS == 0L) {
			return (double)(min_fc_duration / SimpleUtils.DAY_MILLIS);
		}
		return ((double)min_fc_duration) / SimpleUtils.DAY_MILLIS_D;
	}

	public double get_def_fc_duration_days () {
		if (def_fc_duration % SimpleUtils.DAY_MILLIS == 0L) {
			return (double)(def_fc_duration / SimpleUtils.DAY_MILLIS);
		}
		return ((double)def_fc_duration) / SimpleUtils.DAY_MILLIS_D;
	}

	public double get_max_fc_duration_days () {
		if (max_fc_duration % SimpleUtils.DAY_MILLIS == 0L) {
			return (double)(max_fc_duration / SimpleUtils.DAY_MILLIS);
		}
		return ((double)max_fc_duration) / SimpleUtils.DAY_MILLIS_D;
	}




	//----- Controller parameters for the model -----


	// Event ID, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private String cat_eventIDParam;

	public final String get_cat_eventIDParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_eventIDParam while in state " + cur_modstate_string());
		}
		return cat_eventIDParam;
	}

	// Data start time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_dataStartTimeParam;

	public final double get_cat_dataStartTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_dataStartTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataStartTimeParam;
	}

	// Data end time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_dataEndTimeParam;

	public final double get_cat_dataEndTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_dataEndTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataEndTimeParam;
	}

	// Mc for sequence, for the catalog, when first loaded.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_load_mcParam;

	public final double get_cat_load_mcParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_load_mcParam while in state " + cur_modstate_string());
		}
		return cat_load_mcParam;
	}

	// Mc for sequence, for the catalog.
	// Initially the same as cat_load_mcParam, but can be changed by the user.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_mcParam;

	public final double get_cat_mcParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_mcParam while in state " + cur_modstate_string());
		}
		return cat_mcParam;
	}

	// b-value, for the catalog, when first loaded.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_load_bParam;

	public final double get_cat_load_bParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_load_bParam while in state " + cur_modstate_string());
		}
		return cat_load_bParam;
	}

	// b-value, for the catalog.
	// Initially the same as cat_load_bParam, but can be changed by the user.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_bParam;

	public final double get_cat_bParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_bParam while in state " + cur_modstate_string());
		}
		return cat_bParam;
	}

	// Forecast start time, in days since the mainshock.
	// Available when model state >= MODSTATE_FORECAST.

	private double cat_forecastStartTimeParam;

	public final double get_cat_forecastStartTimeParam () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_forecastStartTimeParam while in state " + cur_modstate_string());
		}
		return cat_forecastStartTimeParam;
	}

	// Forecast start time, in days since the mainshock.
	// Available when model state >= MODSTATE_FORECAST.

	private double cat_forecastEndTimeParam;

	public final double get_cat_forecastEndTimeParam () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_forecastEndTimeParam while in state " + cur_modstate_string());
		}
		return cat_forecastEndTimeParam;
	}




	// Clear all data structures that are not valid in the current state.

	private void clear_to_state () {

		// Structures not valid if we don't have a forecast

		if (modstate < MODSTATE_FORECAST) {
			genericForecast = null;
			seqSpecForecast = null;
			bayesianForecast = null;
		}

		// Structures not valid if we don't have parameers

		if (modstate < MODSTATE_PARAMETERS) {
			cur_model = null;
			bayesianModel = null;
		}

		// Structures not valid if we don't have a catalog

		if (modstate < MODSTATE_CATALOG) {
			fcmain = null;
			aafs_fcparams = null;
			fetch_fcparams = null;
			custom_search_region = null;
			analyst_inj_text = null;
			has_fetched_catalog = false;

			cur_mainshock = null;
			cur_aftershocks = null;
			genericModel = null;
			aftershockMND = null;
			mnd_mmaxc = 0.0;
		}

		return;
	}




	//----- Parameter change functions -----




	// Change the stored value of mcParam.
	// Can be called only in state MODSTATE_CATALOG.
	// Note: Caller should check parameter validity.
	// Note: Must be called on the EDT.

	public void change_mcParam (double new_mcParam) throws GUIEDTException {
		cat_mcParam = new_mcParam;

		// Notify the view

		gui_view.view_notify_changed_mcParam();
		return;
	}




	// Change the stored value of bParam.
	// Can be called only in state MODSTATE_CATALOG.
	// Note: Caller should check parameter validity.
	// Note: Must be called on the EDT.

	public void change_bParam (double new_bParam) throws GUIEDTException {
		cat_bParam = new_bParam;

		// Notify the view

		gui_view.view_notify_changed_bParam();
		return;
	}




	//----- Construction -----


	// Construct the model.
	// This should not access other components.

	public OEGUIModel () {
	}


	// Perform initialization after all components are linked.
	// The call order is: model, view, controller.

	public void post_link_init () throws GUIEDTException {

		// Initial state

		modstate = MODSTATE_INITIAL;

		// No data structures yet

		fcmain = null;
		aafs_fcparams = null;
		fetch_fcparams = null;
		custom_search_region = null;
		analyst_inj_text = null;
		has_fetched_catalog = false;

		cur_mainshock = null;
		cur_aftershocks = null;
		cur_model = null;
		genericModel = null;
		bayesianModel = null;
		aftershockMND = null;
		mnd_mmaxc = 0.0;

		genericForecast = null;
		seqSpecForecast = null;
		bayesianForecast = null;

		// Get info from the configuration

		ActionConfig action_config = new ActionConfig();

		def_fc_duration = action_config.get_def_max_forecast_lag();
		max_fc_duration = action_config.get_extended_max_forecast_lag();
		min_fc_duration = Math.min (SimpleUtils.DAY_MILLIS, def_fc_duration);

		return;
	}




	//----- Functions for fetching or loading a catalog -----




	// Get the amount of time left in the UTC day, after the mainshock.

	private double getTimeRemainingInUTCDay(){
		double daysLeftInDay;

		Instant origin = Instant.ofEpochMilli (cur_mainshock.getOriginTime());
		ZonedDateTime zdt = ZonedDateTime.ofInstant (origin, ZoneOffset.UTC);
		ZonedDateTime zdt2 = zdt.toLocalDate().atStartOfDay (ZoneOffset.UTC);
		Instant daybreak = zdt2.toInstant();

//		SimpleDateFormat formatter=new SimpleDateFormat("d MMM yyyy, HH:mm:ss");
//		formatter.setTimeZone(utc); //utc=TimeZone.getTimeZone("UTC"));
//		System.out.println(formatter.format(Date.from (origin)));
//		System.out.println(formatter.format(Date.from (daybreak)));
		
		daysLeftInDay = 1.0 - ((double)(origin.toEpochMilli() - daybreak.toEpochMilli()))/ComcatOAFAccessor.day_millis;
		if (daysLeftInDay == 1.0) {
			daysLeftInDay = 0.0;
		}
		return daysLeftInDay;
	}




	// Perform setup given the mainshock.
	// On entry, fcmain and cur_mainshock both contain the mainshock.
	// On return:
	//  - aafs_fcparams contains the default forecast parameters.
	//  - fetch_fcparams contains a copy of aafs_fcparams;
	//    in particular, fetch_fcparams.aftershock_search_region == null.
	//  - custom_search_region is null.
	//  - analyst_inj_text is an empty string
	//  - Generic model is computed.

	private void setup_for_mainshock (OEGUIController.XferCatalogMod xfer) {

		// Display mainshock information

		System.out.println (fcmain.toString());

		// Display list of OAF products for this mainshock

		if (fcmain.mainshock_geojson != null) {
			List<ComcatOAFProduct> oaf_product_list = ComcatOAFProduct.make_list_from_gj (fcmain.mainshock_geojson);
			for (int k = 0; k < oaf_product_list.size(); ++k) {
				System.out.println ("OAF product: " + oaf_product_list.get(k).summary_string());
			}
			if (oaf_product_list.size() > 0) {
				System.out.println ();
			}
		}

		// Set up aafs_fcparams to hold the forecast parameters

		aafs_fcparams = new ForecastParameters();

		try {
			if (!( aafs_fcparams.fetch_forecast_params (fcmain, null) )) {
				throw new IllegalStateException ("OEGUIModel.setup_for_mainshock - Error fetching forecast params");
			}
		} catch (RuntimeException e) {
			System.err.println ("Error fetching forecast params");
			throw new IllegalStateException ("OEGUIModel.setup_for_mainshock - Error fetching forecast params", e);
			//e.printStackTrace();
			//genericParams = null;
		}
		
		// Announce regimes and parameters

		System.out.println ("Generic parameters for regime " + aafs_fcparams.generic_regime + ":");
		System.out.println (aafs_fcparams.generic_params.toString());

		System.out.println ("Magnitude-of-completeness parameters for regime " + aafs_fcparams.mag_comp_regime + ":");
		System.out.println (aafs_fcparams.mag_comp_params.toString());

		System.out.println ("Default sequence-specific parameters:");
		System.out.println (aafs_fcparams.seq_spec_params.toString());

		// Copy to fetch_fcparams

		fetch_fcparams = new ForecastParameters();
		fetch_fcparams.copy_from (aafs_fcparams);

		// No custom search region

		custom_search_region = null;

		// No injectable text

		analyst_inj_text = "";

		// Make the generic RJ model

		genericModel = new RJ_AftershockModel_Generic(fcmain.mainshock_mag, aafs_fcparams.generic_params);

		// As a courtesy, spit out the decimal days remaining in the origin day

		System.out.println("The mainshock occurred " + String.format("%.4f", getTimeRemainingInUTCDay()) + " days before midnight (UTC)\n");
		return;
	}




	// Set up the search region.
	// On return:
	// - fetch_fcparams contains the search region and search parameters.
	// - fetch_fcparams.mag_comp_params contains the magnitude of completeness
	//   parameters, with the appropriate radius functions for the selected region,
	//   and the magnitude functions set to no minimum.
	// - xfer.x_dataSource.x_dataEndTimeParam adjusted if necessary to limit fetch to before now.

	private void setup_search_region (OEGUIController.XferCatalogMod xfer) {

		// Time range for aftershock search
		
		double minDays = xfer.x_dataSource.x_dataStartTimeParam;
		double maxDays = xfer.x_dataSource.x_dataEndTimeParam;
		
		long startTime = fcmain.mainshock_time + Math.round(minDays*ComcatOAFAccessor.day_millis);
		long endTime = fcmain.mainshock_time + Math.round(maxDays*ComcatOAFAccessor.day_millis);

		// Check that start date is before current time

		long time_now = System.currentTimeMillis();		// must be the actual system time, not ServerClock
		
		Preconditions.checkState(startTime < time_now, "Start time is after now!");

		// Check that end date is before current time, shrink the time range if not
		
		if (endTime > time_now) {
			double calcMaxDays = (time_now - startTime)/ComcatOAFAccessor.day_millis;
			System.out.println("WARNING: End time after current time. Setting max days to: " + calcMaxDays);
			xfer.x_dataSource.modify_dataEndTimeParam(calcMaxDays);
			maxDays = xfer.x_dataSource.x_dataEndTimeParam;
		}

		// The magnitude-of-completeness parameters

		double magCat = fetch_fcparams.mag_comp_params.get_magCat();
		MagCompFn magCompFn = fetch_fcparams.mag_comp_params.get_magCompFn();
		SearchMagFn magSample = fetch_fcparams.mag_comp_params.get_fcn_magSample();
		SearchRadiusFn radiusSample = fetch_fcparams.mag_comp_params.get_fcn_radiusSample();
		SearchMagFn magCentroid = fetch_fcparams.mag_comp_params.get_fcn_magCentroid();
		SearchRadiusFn radiusCentroid = fetch_fcparams.mag_comp_params.get_fcn_radiusCentroid();

		// No custom region

		custom_search_region = null;

		// Depth range for aftershock search, assume default
		
		double minDepth = ForecastParameters.SEARCH_PARAM_OMIT;
		double maxDepth = ForecastParameters.SEARCH_PARAM_OMIT;

		// Minimum magnitude is default, set from the mag-of-comp parameters

		double min_mag = ForecastParameters.SEARCH_PARAM_OMIT;

		// Switch on region type

		switch (xfer.x_dataSource.x_region.x_regionTypeParam) {

		case STANDARD:

			// Standard region, just change to no minimum magnitude

			magSample = magSample.makeRemovedMinMag();
			magCentroid = magCentroid.makeRemovedMinMag();
			break;

		case CENTROID_WC_CIRCLE:

			// WC circle around centroid, set multiplier and limits, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			radiusSample = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			magCentroid = SearchMagFn.makeNoMinMag();
			radiusCentroid = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CENTROID_CIRCLE:

			// Circle around centroid, set constant radius, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			radiusSample = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			magCentroid = SearchMagFn.makeNoMinMag();
			radiusCentroid = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case EPICENTER_WC_CIRCLE:

			// WC circle around epicenter, set multiplier and limits, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			radiusSample = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			magCentroid = SearchMagFn.makeSkipCentroid();
			radiusCentroid = SearchRadiusFn.makeConstant (0.0);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case EPICENTER_CIRCLE:

			// Circle around epicenter, set constant radius, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			radiusSample = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			magCentroid = SearchMagFn.makeSkipCentroid();
			radiusCentroid = SearchRadiusFn.makeConstant (0.0);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CUSTOM_CIRCLE:

			// Custom circle, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			magCentroid = SearchMagFn.makeSkipCentroid();
			custom_search_region = SphRegion.makeCircle (
				new SphLatLon(xfer.x_dataSource.x_region.x_centerLatParam, xfer.x_dataSource.x_region.x_centerLonParam),
				xfer.x_dataSource.x_region.x_radiusParam
			);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CUSTOM_RECTANGLE:

			// Custom rectangle, and no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag();
			magCentroid = SearchMagFn.makeSkipCentroid();
			custom_search_region = SphRegion.makeMercRectangle (
				new SphLatLon(xfer.x_dataSource.x_region.x_minLatParam, xfer.x_dataSource.x_region.x_minLonParam),
				new SphLatLon(xfer.x_dataSource.x_region.x_maxLatParam, xfer.x_dataSource.x_region.x_maxLonParam)
			);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		default:
			throw new IllegalStateException("Unknown region type: " + xfer.x_dataSource.x_region.x_regionTypeParam);
		}

		// Make revised magnitude-of-completeness parameters

		fetch_fcparams.mag_comp_params = new MagCompPage_Parameters (
			magCat,
			magCompFn,
			magSample,
			radiusSample,
			magCentroid,
			radiusCentroid
		);

		// Make the search region

		fetch_fcparams.set_aftershock_search_region (
			fcmain,					// ForecastMainshock fcmain,
			0L,						// long the_start_lag,
			0L,						// long the_forecast_lag,
			custom_search_region,	// SphRegion the_aftershock_search_region,
			minDays,				// double the_min_days,
			maxDays,				// double the_max_days,
			minDepth,				// double the_min_depth,
			maxDepth,				// double the_max_depth,
			min_mag					// double the_min_mag
		);

		if (!( fetch_fcparams.aftershock_search_avail )) {
			throw new IllegalStateException("Failed to build aftershock search region");
		}

		// If the event (i.e. cur_mainshock) is outside the plotting domain, change its
		// hypocenter so it is inside the plotting domain

		Location hypo = cur_mainshock.getHypocenterLocation();
		if (fetch_fcparams.aftershock_search_region.getPlotWrap()) {
			if (hypo.getLongitude() < 0.0) {
				cur_mainshock.setHypocenterLocation (new Location (
					hypo.getLatitude(), hypo.getLongitude() + 360.0, hypo.getDepth() ));
			}
		} else {
			if (hypo.getLongitude() > 180.0) {
				cur_mainshock.setHypocenterLocation (new Location (
					hypo.getLatitude(), hypo.getLongitude() - 360.0, hypo.getDepth() ));
			}
		}

		return;
	}




	// Perform actions after fetching or loading the catalog.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.

	private void postFetchActions (OEGUIController.XferCatalogMod xfer) {

		// Sort the aftershocks by time

		cur_aftershocks.sortByOriginTime();

		// Compute the magnitude-number distribution of aftershocks (in bins)

		aftershockMND = ObsEqkRupListCalc.getMagNumDist(cur_aftershocks, 1.05, 81, 0.1);

		// Find the magnitude bin with the maximum number of aftershocks

		mnd_mmaxc = AftershockStatsCalc.getMmaxC(aftershockMND);

		// Set the default magnitude of completeness to the peak of the mag-num distribution, plus 0.5 magnitude

		cat_load_mcParam = mnd_mmaxc + 0.5;

		// Set the default b-value from the generic parameters

		cat_load_bParam = aafs_fcparams.generic_params.get_bValue();

		// Save the catalog parameters

		cat_eventIDParam = xfer.x_dataSource.x_eventIDParam;
		cat_dataStartTimeParam = xfer.x_dataSource.x_dataStartTimeParam;
		cat_dataEndTimeParam = xfer.x_dataSource.x_dataEndTimeParam;
		cat_mcParam = cat_load_mcParam;
		cat_bParam = cat_load_bParam;

		return;
	}




	// Fetch events from Comcat.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate catalog fetch.

	public void fetchEvents (OEGUIController.XferCatalogMod xfer) {

		System.out.println("Fetching Events");
		cur_mainshock = null;

		// Will be fetching from Comcat

		has_fetched_catalog = true;

		// See if the event ID is an alias, and change it if so

		String xlatid = GUIEventAlias.query_alias_dict (xfer.x_dataSource.x_eventIDParam);
		if (xlatid != null) {
			System.out.println("Translating Event ID: " + xfer.x_dataSource.x_eventIDParam + " -> " + xlatid);
			xfer.x_dataSource.modify_eventIDParam(xlatid);
		}

		// Fetch mainshock into our data structures
		
		String eventID = xfer.x_dataSource.x_eventIDParam;
		fcmain = new ForecastMainshock();
		fcmain.setup_mainshock_poll (eventID);
		Preconditions.checkState(fcmain.mainshock_avail, "Event not found: %s", eventID);

		cur_mainshock = fcmain.get_eqk_rupture();

		// Finish setting up the mainshock

		setup_for_mainshock (xfer);

		// Make the search region

		setup_search_region (xfer);

		// Get the aftershocks

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		cur_aftershocks = accessor.fetchAftershocks (
			cur_mainshock,
			fetch_fcparams.min_days,
			fetch_fcparams.max_days,
			fetch_fcparams.min_depth,
			fetch_fcparams.max_depth,
			fetch_fcparams.aftershock_search_region,
			fetch_fcparams.aftershock_search_region.getPlotWrap(),
			fetch_fcparams.min_mag
		);

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}
	



	// Load catalog from a file.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	//  catalogFile = File to read from.
	// This is called by the controller to initiate catalog load.

	public void loadCatalog(OEGUIController.XferCatalogMod xfer, File catalogFile) {

		// Will not be fetching from Comcat

		has_fetched_catalog = false;

		// List of aftershocks, and mainshock

		ObsEqkRupList myAftershocks = new ObsEqkRupList();
		ObsEqkRupture myMainshock = null;

		// Read the file

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();
		ext_cat.read_from_file (catalogFile, myAftershocks);
		myMainshock = ext_cat.mainshock;

		// Display search result
		
		System.out.println("Loaded " + myAftershocks.size() + " aftershocks from file");

		// Check to make sure we got a mainshock
		
		Preconditions.checkState(myMainshock != null, "Did not find mainshock in file");

		// If we didn't get an event ID, set it

		if (myMainshock.getEventId() == null || myMainshock.getEventId().isEmpty()) {
			myMainshock.setEventId ("<custom>");
		}

		// Store mainshock into our data structures

		cur_mainshock = myMainshock;

		fcmain = new ForecastMainshock();
		fcmain.setup_local_mainshock (cur_mainshock);

		// Finish setting up the mainshock

		setup_for_mainshock (xfer);

		// Catalog time range

		double minDays = xfer.x_dataSource.x_dataStartTimeParam;
		double maxDays = xfer.x_dataSource.x_dataEndTimeParam;

		// Here we need to trim the catalog to be only events that lie within the selected time interval
		
		ObsEqkRupList trimmedAftershocks = new ObsEqkRupList();
		long eventTime = myMainshock.getOriginTime();
		long startTime = eventTime + (long)(minDays*ComcatOAFAccessor.day_millis);
		long endTime = eventTime + (long)(maxDays*ComcatOAFAccessor.day_millis);
		for (ObsEqkRupture as : myAftershocks) {
			long asTime = as.getOriginTime();
			if (asTime >= startTime && asTime <= endTime) {
				trimmedAftershocks.add(as);
			}
		}
		
		System.out.println("Found " + trimmedAftershocks.size() + " aftershocks between selected start and end times");

		// Establish name of custom mainshock

		xfer.x_dataSource.modify_eventIDParam("<custom>");

		// Take the trimmed list of aftershocks as our aftershocks

		cur_aftershocks = trimmedAftershocks;

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}




	//----- Functions for fitting aftershock parameters -----




	// Fit aftershock parameters, and construct the R&J models.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate aftershock parameter fitting.

	public void fitParams (OEGUIController.XferFittingMod xfer) {

		// Get sequence-specific parameters

		Range aRange = xfer.x_rjValue.x_aValRangeParam;
		int aNum = xfer.x_rjValue.x_aValNumParam;
		//validateRange(aRange, aNum, "a-value");
		Range pRange = xfer.x_rjValue.x_pValRangeParam;
		int pNum = xfer.x_rjValue.x_pValNumParam;
		//validateRange(pRange, pNum, "p-value");
		Range cRange = xfer.x_rjValue.x_cValRangeParam;
		int cNum = xfer.x_rjValue.x_cValNumParam;
		//validateRange(cRange, cNum, "c-value");
					
		double mc = xfer.x_commonValue.x_mcParam;
		//double mc = cat_mcParam;
									
		double b = xfer.x_commonValue.x_bParam;
		//double b = cat_bParam;

		// Save the sequence-specific parameters for possible use in analyst options

		fetch_fcparams.seq_spec_params = new SeqSpecRJ_Parameters (
			b,
			aRange.getLowerBound(),
			aRange.getUpperBound(),
			aNum,
			pRange.getLowerBound(),
			pRange.getUpperBound(),
			pNum,
			cRange.getLowerBound(),
			cRange.getUpperBound(),
			cNum
		);

		// Magnitude of completeness info
						
		double mCat;

		MagCompFn magCompFn;

		// If doing time-dependent magnitude of completeness
					
		if (xfer.x_commonValue.x_timeDepMcParam) {

			double f = xfer.x_commonValue.x_fParam;
						
			double g = xfer.x_commonValue.x_gParam;
						
			double h = xfer.x_commonValue.x_hParam;
						
			mCat = xfer.x_commonValue.x_mCatParam;

			magCompFn = MagCompFn.makePageOrConstant (f, g, h);
						
			cur_model = new RJ_AftershockModel_SequenceSpecific(get_cur_mainshock(), get_cur_aftershocks(), mCat, magCompFn, b,
					cat_dataStartTimeParam, cat_dataEndTimeParam,
					aRange.getLowerBound(), aRange.getUpperBound(), aNum,
					pRange.getLowerBound(), pRange.getUpperBound(), pNum,
					cRange.getLowerBound(), cRange.getUpperBound(), cNum);

		// Otherwise, time-independent magnitude of completeness

		} else {
						
			mCat = mc;

			magCompFn = MagCompFn.makeConstant();

			cur_model = new RJ_AftershockModel_SequenceSpecific(get_cur_mainshock(), get_cur_aftershocks(), mc, b,
					cat_dataStartTimeParam, cat_dataEndTimeParam,
					aRange.getLowerBound(), aRange.getUpperBound(), aNum,
					pRange.getLowerBound(), pRange.getUpperBound(), pNum,
					cRange.getLowerBound(), cRange.getUpperBound(), cNum);
		}

		// Save the magnitude-of-completeness parameters for possible use in analyst options

		SearchMagFn magSample = aafs_fcparams.mag_comp_params.get_fcn_magSample();	// the original value
		SearchRadiusFn radiusSample = fetch_fcparams.mag_comp_params.get_fcn_radiusSample();
		SearchMagFn magCentroid = aafs_fcparams.mag_comp_params.get_fcn_magCentroid();	// the original value
		SearchRadiusFn radiusCentroid = fetch_fcparams.mag_comp_params.get_fcn_radiusCentroid();

		fetch_fcparams.mag_comp_params = new MagCompPage_Parameters (
			mCat,
			magCompFn,
			magSample.makeForAnalystMagCat (mCat),
			radiusSample,
			magCentroid.makeForAnalystMagCat (mCat),
			radiusCentroid
		);

		// Make the Bayesian model if possible
					
		bayesianModel = null;
		if (genericModel != null) {
			if (RJ_AftershockModel_Bayesian.areModelsEquivalent(cur_model, genericModel))
				bayesianModel = new RJ_AftershockModel_Bayesian(cur_model, genericModel);
			else
				System.out.println("Could not create Bayesian model as sequence specifc and "
						+ "generic models are not equivalent");
		}

		// Save the catalog parameters (we should already have these values)

		//cat_dataStartTimeParam = xfer.x_dataSource.x_dataStartTimeParam;
		//cat_dataEndTimeParam = xfer.x_dataSource.x_dataEndTimeParam;
		cat_mcParam = xfer.x_commonValue.x_mcParam;
		cat_bParam = xfer.x_commonValue.x_bParam;

		return;
	}




	//----- Functions for computing forecasts -----




	// Compute forecasts.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate forecast computation.

	public void computeForecasts (GUICalcProgressBar progress, OEGUIController.XferForecastMod xfer) {

		// Save the catalog parameters

		cat_forecastStartTimeParam = xfer.x_fcValue.x_forecastStartTimeParam;
		cat_forecastEndTimeParam = xfer.x_fcValue.x_forecastEndTimeParam;

		// Compute the aftershock forecasts

		seqSpecForecast = makeForecast (progress, cat_forecastStartTimeParam, cur_model, "Seq. Specific");
		genericForecast = makeForecast (progress, cat_forecastStartTimeParam, genericModel, "Generic");
		bayesianForecast = makeForecast (progress, cat_forecastStartTimeParam, bayesianModel, "Bayesian");

		// Remove the model-specific message
		
		if (progress != null) {
			//progress.setIndeterminate(true);
			//progress.setProgressMessage("Plotting...");
			progress.setIndeterminate(true, "Plotting...");
		}

		// Pre-compute elements for expected aftershock MFDs.

		gui_view.precomputeEAMFD (progress, cat_forecastStartTimeParam, cat_forecastEndTimeParam);

		return;
	}




	// Make the forecast for a model.
	// Returns null if the model is null.

	private USGS_AftershockForecast makeForecast (GUICalcProgressBar progress, double minDays, RJ_AftershockModel the_model, String name) {

		// Check for null model

		if (the_model == null) {
			return null;
		}

		// Start time of forecast is from forecastStartTimeParam
		
		Instant eventDate = Instant.ofEpochMilli(get_cur_mainshock().getOriginTime());
		double startTime = eventDate.toEpochMilli() + minDays*ComcatOAFAccessor.day_millis;
		Instant startDate = Instant.ofEpochMilli((long)startTime);

		// Begin timer

		Stopwatch watch = Stopwatch.createStarted();

		// Announce we are processing the model
			
		if (progress != null) {
			progress.setIndeterminate(true, "Calculating " + name + "...");
		}

		// Calculate the forecast for the model

		USGS_AftershockForecast forecast;
		if (gui_top.get_include_m4()) {
			double[] min_mags = new double[5];
			min_mags[0] = 3.0;
			min_mags[1] = 4.0;
			min_mags[2] = 5.0;
			min_mags[3] = 6.0;
			min_mags[4] = 7.0;
			forecast = new USGS_AftershockForecast(the_model, gui_model.get_cur_aftershocks(), min_mags, eventDate, startDate);
		} else {
			forecast = new USGS_AftershockForecast(the_model, gui_model.get_cur_aftershocks(), eventDate, startDate);
		}

		// Display timing
			
		System.out.println("Took " + watch.elapsed(TimeUnit.SECONDS) + "s to compute aftershock table for " + name);
		watch.stop();

		// Return the forecast

		return forecast;
	}




	//----- Functions for analyst operations -----




	// Make the new forecast parameters for analyst options.
	// See AnalystCLI.make_new_fcparams.

	public ForecastParameters make_analyst_fcparams (OEGUISubAnalyst.XferAnalystView xfer) {

		ForecastParameters new_fcparams = new ForecastParameters();
		new_fcparams.setup_all_default();

		if (xfer.x_autoEnableParam == AutoEnable.DISABLE) {
			return new_fcparams;
		}

		new_fcparams.set_analyst_control_params (
			ForecastParameters.CALC_METH_AUTO_PDL,		// generic_calc_meth
			ForecastParameters.CALC_METH_AUTO_PDL,		// seq_spec_calc_meth
			ForecastParameters.CALC_METH_AUTO_PDL,		// bayesian_calc_meth
			analyst_inj_text.isEmpty() ? ForecastParameters.INJ_TXT_USE_DEFAULT : analyst_inj_text
		);

		if (!( xfer.x_useCustomParamsParam )) {
			return new_fcparams;
		}

		new_fcparams.set_analyst_mag_comp_params (
			true,										// mag_comp_avail
			aafs_fcparams.mag_comp_regime,				// mag_comp_regime
			fetch_fcparams.mag_comp_params				// mag_comp_params
		);

		new_fcparams.set_analyst_seq_spec_params (
			true,										// seq_spec_avail
			fetch_fcparams.seq_spec_params				// seq_spec_params
		);

		if (custom_search_region == null) {
			return new_fcparams;
		}

		new_fcparams.set_analyst_aftershock_search_params (
			true,										// the_aftershock_search_avail
			custom_search_region,						// the_aftershock_search_region
			ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_days
			ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_days
			ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_depth
			ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_depth
			ForecastParameters.SEARCH_PARAM_OMIT		// the_min_mag
		);
		
		return new_fcparams;
	}




	// Make the analyst options.
	// See AnalystCLI.make_new_analyst_options.

	public AnalystOptions make_analyst_options (OEGUISubAnalyst.XferAnalystView xfer) {

		// Action configuration

		ActionConfig action_config = new ActionConfig();

		// The first forecast lag, note is is a multiple of 1 second

		long first_forecast_lag = action_config.get_next_forecast_lag (0L);
		if (first_forecast_lag < 0L) {
			first_forecast_lag = 0L;
		}

		// Time now

		long time_now = ServerClock.get_time();

		// The forecast lag that would cause a forecast to be issued now,
		// as a multiple of 1 seconds, but after the first forecast

		long current_lag = action_config.floor_unit_lag (
				time_now - (fcmain.mainshock_time
							+ action_config.get_comcat_clock_skew()
							+ action_config.get_comcat_origin_skew()),
				first_forecast_lag + 1000L);

		// Parameters that vary based on forecast selection
		
		long extra_forecast_lag;
		int intake_option;
		int shadow_option;

		switch (xfer.x_autoEnableParam) {

		case DISABLE:
			//extra_forecast_lag = -1L;
			extra_forecast_lag = current_lag;	// needed so any existing forecast is removed promptly
			intake_option = AnalystOptions.OPT_INTAKE_BLOCK;
			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
			break;

		case ENABLE:
			extra_forecast_lag = current_lag;
			intake_option = AnalystOptions.OPT_INTAKE_IGNORE;
			shadow_option = AnalystOptions.OPT_SHADOW_IGNORE;
			break;

		default:
			extra_forecast_lag = current_lag;
			intake_option = AnalystOptions.OPT_INTAKE_NORMAL;
			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
			break;
		}

		// Other parameters

		String analyst_id = "";
		String analyst_remark = "";
		long analyst_time = time_now;
		ForecastParameters analyst_params = make_analyst_fcparams (xfer);

		long max_forecast_lag = Math.round (xfer.x_forecastDuration * SimpleUtils.DAY_MILLIS_D);
		if (max_forecast_lag >= def_fc_duration - 1000L && max_forecast_lag <= def_fc_duration + 1000L) {
			max_forecast_lag = 0L;
		}
		else if (max_forecast_lag >= max_fc_duration - 1000L) {
			max_forecast_lag = max_fc_duration;
		}
		else if (max_forecast_lag <= min_fc_duration + 1000L) {
			max_forecast_lag = min_fc_duration;
		}

		// Create the analyst options

		AnalystOptions anopt = new AnalystOptions();

		anopt.setup (
			analyst_id,
			analyst_remark,
			analyst_time,
			analyst_params,
			extra_forecast_lag,
			max_forecast_lag,
			intake_option,
			shadow_option
		);

		return anopt;
	}




	// Fetch server status.
	// This is called by the controller to fetch AAFS server status.
	// Returns 1000*T+H where T is the number of servers and H is the number that are healthy.
	// An exception is thrown if the operation could not be performed.

	public int fetchServerStatus (GUICalcProgressBar progress) {

		// Call the standard function in ServerCmd

		System.out.println ();
		int result = ServerCmd.gui_show_relay_status (progress);

		return result;
	}




	// Set the analyst injectable text.
	// Can be called when model state >= MODSTATE_PARAMETERS.

	public void setAnalystInjText (String inj_text) {
		analyst_inj_text = inj_text;
		return;
	}




	// Export analyst options.
	// Can be called when model state >= MODSTATE_PARAMETERS.
	// An exception is thrown if the file could not be written.

	public void exportAnalystOptions (OEGUISubAnalyst.XferAnalystView xfer, File the_file) throws IOException {

		// Make the analyst options

		AnalystOptions anopt = make_analyst_options (xfer);

		// Marshal to JSON

		MarshalImpJsonWriter store = new MarshalImpJsonWriter();
		AnalystOptions.marshal_poly (store, null, anopt);
		store.check_write_complete ();
		String json_string = store.get_json_string();

		// Write to file

		try (
			BufferedWriter bw = new BufferedWriter (new FileWriter (the_file));
		){
			bw.write (json_string);
		}

		return;
	}




	// Send analyst options to server.
	// Can be called when model state >= MODSTATE_PARAMETERS.
	// Returns true if success, false if unable to send to any server.
	// An exception is thrown if the operation could not be performed.

	public boolean sendAnalystOptions (GUICalcProgressBar progress, OEGUISubAnalyst.XferAnalystView xfer) {

		// Make the analyst options

		AnalystOptions anopt = make_analyst_options (xfer);

		// Send to server

		String event_id = fcmain.mainshock_event_id;
		boolean f_disable = (xfer.x_autoEnableParam == AutoEnable.DISABLE);

		boolean f_success = ServerCmd.gui_send_analyst_opts (progress, event_id, f_disable, anopt);

		return f_success;
	}




	//----- Functions for system information -----




	// Get system information.

	public String get_system_information () {

		StringBuilder sb = new StringBuilder();
		int section_count = 0;

		// RJ parameter values

		if (modstate >= MODSTATE_PARAMETERS) {
			if (section_count > 0) {
				sb.append ("\n");
			}
			++section_count;

			sb.append ("Fitted RJ Parameters\n");
			sb.append ("a = " + get_cur_model().getMaxLikelihood_a() + "\n");
			sb.append ("p = " + get_cur_model().getMaxLikelihood_p() + "\n");
			sb.append ("c = " + get_cur_model().getMaxLikelihood_c() + "\n");
		}

		// Memory

		if (section_count > 0) {
			sb.append ("\n");
		}
		++section_count;

		sb.append ("Memory Usage\n");
		long max_memory = Runtime.getRuntime().maxMemory();
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		if (max_memory == Long.MAX_VALUE) {
			sb.append ("max_memory = unlimited\n");
		} else {
			sb.append ("max_memory = " + (max_memory / 1048576L) + " M\n");
		}
			
		sb.append ("total_memory = " + (total_memory / 1048576L) + " M\n");
		sb.append ("free_memory = " + (free_memory / 1048576L) + " M\n");
		sb.append ("used_memory = " + (used_memory / 1048576L) + " M\n");

		// Processor

		if (section_count > 0) {
			sb.append ("\n");
		}
		++section_count;

		sb.append ("Processor\n");
		sb.append ("num_threads = " + AutoExecutorService.get_default_num_threads() + "\n");

		return sb.toString();
	}







	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
