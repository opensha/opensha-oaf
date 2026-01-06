package org.opensha.oaf.oetas.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;
import java.util.function.Predicate;
import java.util.Comparator;
import java.util.Arrays;

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
import org.opensha.oaf.rj.OAFRegimeParams;

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
import org.opensha.oaf.util.gui.GUIExternalCatalogV2;
import org.opensha.oaf.util.AutoExecutorService;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
//import org.opensha.oaf.comcat.ComcatOAFProduct;
import org.opensha.oaf.comcat.ComcatProduct;
import org.opensha.oaf.comcat.ComcatProductOaf;
import org.opensha.oaf.comcat.GeoJsonUtils;

import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.BufferedReader;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;
import org.opensha.oaf.aafs.ForecastData;
import org.opensha.oaf.aafs.ServerCmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.ServerClock;
import org.opensha.oaf.aafs.AdjustableParameters;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.aafs.VersionInfo;

import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.AsciiTable;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opensha.oaf.util.catalog.RuptureCatalogFile;
import org.opensha.oaf.util.catalog.RuptureCatalogSection;
import org.opensha.oaf.util.catalog.AbsRelTimeLocConverter;
import org.opensha.oaf.util.catalog.RuptureLineFormatter;
import org.opensha.oaf.util.catalog.RuptureLineFormatGUIObserved;
import org.opensha.oaf.util.catalog.EventIDGenerator;
import org.opensha.oaf.util.catalog.ObsEqkRupFilter;
import org.opensha.oaf.util.LineConsumerFile;
import org.opensha.oaf.util.LineSupplierFile;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEMarginalDistSet;
import org.opensha.oaf.oetas.env.OEtasParameters;
import org.opensha.oaf.oetas.env.OEtasResults;
import org.opensha.oaf.oetas.env.OEtasConfig;
import org.opensha.oaf.oetas.env.OEtasIntegratedIntensityFile;
import org.opensha.oaf.oetas.fit.OEGridPoint;


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


	// Return true if the model state includes a mainshock.

	public final boolean modstate_has_mainshock () {
		return modstate >= MODSTATE_MAINSHOCK;
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

		// Update information

		info_advance_state (old_modstate, new_modstate);

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

		// Retreat information even if state is not changing.

		retreat_info_modstate (new_modstate);

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




	// The catalog file.
	// Available when model state >= MODSTATE_CATALOG.

	private GUIExternalCatalogV2 din_catalog;

	public final GUIExternalCatalogV2 get_din_catalog () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.din_catalog while in state " + cur_modstate_string());
		}
		return din_catalog;
	}


	// The mainshock structure.
	// Available when model state >= MODSTATE_MAINSHOCK.

	private ForecastMainshock fcmain;

	public final ForecastMainshock get_fcmain () {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.fcmain while in state " + cur_modstate_string());
		}
		return fcmain;
	}

	// The AAFS parameters for the mainshock.
	// This contains all default parameters for the mainshock, except the search region.
	// Available when model state >= MODSTATE_CATALOG.
	//
	// When loaded from Comcat, contains the forecast parameters for the mainshock location
	// (excluding search region or forecast lag, see ForecastParameters.fetch_forecast_params()).
	// If analyst parameters are available (if the checkbox to use analyst parameters is checked,
	// and the most recent forecast has a ForecastData file that contains analyst parameters),
	// then the analyst parameters are used in the call to ForecastParameters.fetch_forecast_params().
	//
	// When loaded from a catalog file, the contents are the same as if loaded from Comcat,
	// except that analyst parameters are available only if the user selected to load mainshock
	// info from Comcat, and the checkbox to use analyst parameters is checked, and the most
	// recent forecast has a ForecastData file that contains analyst parameters.
	//
	// When loaded from a ForecastData file, contains the forecast parameters from the file.
	// (Analyst parameters are not relevant because any analyst parameters in the file were
	// already used to create the forecast parameters.)

	private ForecastParameters aafs_fcparams;

	// The parameters used during the fetch operation.
	// Available when model state >= MODSTATE_CATALOG.

	private ForecastParameters fetch_fcparams;

	// Custom search region, or null if none.
	// Available when model state >= MODSTATE_CATALOG.
	// This is used when creating analyst options to send to the server, with custom parameters.
	// If custom_search_region is not null, then the aftershock search parameters are set to
	// use this specific region.
	//
	// When loaded from Comcat, with the STANDARD region: If analyst parameters are available
	// and they contain a custom region, then custom_search_region is set to that region;
	// otherwise custom_search_region is null.
	//
	// When loaded from Comcat with another region option: If the region option is CUSTOM_CIRCLE
	// or CUSTOM_RECTANGLE then custom_search_region is set to that region, otherwise it is null.
	//
	// When loaded from a catalog file: custom_search_region is set to the aftershock search
	// region associated with the file (which may be contained in the file or supplied by the user).
	//
	// Whe loaded from a ForecastData file: If analyst parameters are available
	// and they contain a custom region, then custom_search_region is set to that region;
	// otherwise custom_search_region is null.

	private SphRegion custom_search_region;

	// Resolved search region, or null if none.
	// Available when model state >= MODSTATE_CATALOG.
	// This is the region used for the aftershock search in Comcat.
	// If custom_search_region is non-null, then this should have the same value.
	// This is mainly for display on the info tab.

	private SphRegion resolved_search_region;

	public final SphRegion get_resolved_search_region () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.resolved_search_region while in state " + cur_modstate_string());
		}
		return resolved_search_region;
	}

	// Resolved search region specification, or null if none.
	// Available when model state >= MODSTATE_CATALOG.
	// This specifies the aftershock search region, as given by GUI parameters.
	// This is mainly for display on the info tab or in the region sub-panel,
	// but is also used to transfer the region spec from analyst options to the region sub-panel.

	private OEGUISubRegion.RegionSpec resolved_region_spec;

	public final OEGUISubRegion.RegionSpec get_resolved_region_spec () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.resolved_region_spec while in state " + cur_modstate_string());
		}
		return resolved_region_spec;
	}

	// True if the search region was loaded from a forecast.
	// Available when model state >= MODSTATE_CATALOG.

	private boolean loaded_region_from_fc;

	public final boolean get_loaded_region_from_fc () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.loaded_region_from_fc while in state " + cur_modstate_string());
		}
		return loaded_region_from_fc;
	}

	// True if parameters were loaded from a forecast.
	// Available when model state >= MODSTATE_CATALOG.

	private boolean loaded_params_from_fc;

	public final boolean get_loaded_params_from_fc () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.loaded_params_from_fc while in state " + cur_modstate_string());
		}
		return loaded_params_from_fc;
	}

	// Analyst adjustable parameters.
	// Available when model state >= MODSTATE_MAINSHOCK.
	// If a download file is available at the time the catalog is loaded, this is initialized
	// from the analyst options in the download file.  Otherwise, it is initialized from
	// a default set of analyst options.

	private AdjustableParameters analyst_adj_params;

	public final AdjustableParameters get_analyst_adj_params () {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.analyst_adj_params while in state " + cur_modstate_string());
		}
		return analyst_adj_params;
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

	// True if the mainshock is fetched frm Comcat.
	// Available when model state >= MODSTATE_MAINSHOCK.
	// Note: Returns true if the mainshock has an event id that is not generated, a network, and a code.
	// Note: A true return means fcmain contains the info needed for sending a product to PDL.

	public final boolean get_has_fetched_mainshock () {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.has_fetched_mainshock while in state " + cur_modstate_string());
		}
		if (
			fcmain.mainshock_event_id == null
			|| fcmain.mainshock_event_id.trim().isEmpty()
			|| EventIDGenerator.is_generated_id (fcmain.mainshock_event_id.trim())
			|| fcmain.mainshock_network == null
			|| fcmain.mainshock_network.trim().isEmpty()
			|| fcmain.mainshock_code == null
			|| fcmain.mainshock_code.trim().isEmpty()
		) {
			return false;
		}
		return true;
	}

	// Get the mainshock event id to be displayed to the user.
	// Available when model state >= MODSTATE_MAINSHOCK.
	// Returns an empty string if the mainshock hs an event id that is missing or generated.

	public final String get_mainshock_display_id () {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.mainshock_display_id while in state " + cur_modstate_string());
		}
		if (
			fcmain.mainshock_event_id == null
			|| fcmain.mainshock_event_id.trim().isEmpty()
			|| EventIDGenerator.is_generated_id (fcmain.mainshock_event_id.trim())
		) {
			return "";
		}
		return fcmain.mainshock_event_id.trim();
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
	// Available when model state >= MODSTATE_MAINSHOCK.

	private ObsEqkRupture cur_mainshock;

	public final ObsEqkRupture get_cur_mainshock () {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_mainshock while in state " + cur_modstate_string());
		}
		return cur_mainshock;
	}

	// The list of aftershocks.
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.
	// Note: This is the list of earthquakes that feed into the forecast, and includes
	// both foreshocks and aftershocks, but only within the aftershock region (cur_region).

	private ObsEqkRupList cur_aftershocks;

	public final ObsEqkRupList get_cur_aftershocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_aftershocks while in state " + cur_modstate_string());
		}
		return cur_aftershocks;
	}

	// The list of strict aftershocks (occurring at or after the time of the mainshock).
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.
	// Note: This is a subset of cur_aftershocks.

	private ObsEqkRupList strict_aftershocks;

	public final ObsEqkRupList get_strict_aftershocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.strict_aftershocks while in state " + cur_modstate_string());
		}
		return strict_aftershocks;
	}

	// The list of strict foreshocks (occurring before the time of the mainshock).
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.
	// Note: This is a subset of cur_aftershocks.

	private ObsEqkRupList strict_foreshocks;

	public final ObsEqkRupList get_strict_foreshocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.strict_foreshocks while in state " + cur_modstate_string());
		}
		return strict_foreshocks;
	}

	// The outer region used for plotting regional aftershocks, or null if none.
	// Available when model state >= MODSTATE_CATALOG.

	private SphRegion outer_region;

	public final SphRegion get_outer_region () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.outer_region while in state " + cur_modstate_string());
		}
		return outer_region;
	}

	// The list of aftershocks to plot.
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.
	// Note: This is a superset of cur_aftershocks.

	private ObsEqkRupList plot_aftershocks;

	public final ObsEqkRupList get_plot_aftershocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.plot_aftershocks while in state " + cur_modstate_string());
		}
		return plot_aftershocks;
	}


	// The analyst options for the existing forecast.
	// Available when model state >= MODSTATE_CATALOG.
	// Can be null if analyst options were not retrieved.

	private AnalystOptions existing_analyst_opts;

	public final AnalystOptions get_existing_analyst_opts () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.existing_analyst_opts while in state " + cur_modstate_string());
		}
		return existing_analyst_opts;
	}


	// The parameters in the analyst options for the existing forecast.
	// Available when model state >= MODSTATE_CATALOG.
	// Can be null if analyst options were not retrieved, or if they do not contain custom parameters.

	public final ForecastParameters get_existing_analyst_fcparams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.existing_analyst_fcparams while in state " + cur_modstate_string());
		}
		if (existing_analyst_opts == null) {
			return null;
		}
		return existing_analyst_opts.analyst_params;
	}


	// The parameters used to create the forecast.
	// Available when model state >= MODSTATE_PARAMETERS.

	private ForecastParameters forecast_fcparams;

	public final ForecastParameters get_forecast_fcparams () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.forecast_fcparams while in state " + cur_modstate_string());
		}
		return forecast_fcparams;
	}


	// The results of the forecast.
	// Available when model state >= MODSTATE_PARAMETERS.

	private ForecastResults forecast_fcresults;

	public final ForecastResults get_forecast_fcresults () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.forecast_fcresults while in state " + cur_modstate_string());
		}
		return forecast_fcresults;
	}


	// The analyst options for the forecast.
	// Available when model state >= MODSTATE_PARAMETERS.

	private AnalystOptions forecast_analyst_opts;

	public final AnalystOptions get_forecast_analyst_opts () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.forecast_analyst_opts while in state " + cur_modstate_string());
		}
		return forecast_analyst_opts;
	}


	// The data for the forecast.
	// Available when model state >= MODSTATE_PARAMETERS.

	private ForecastData forecast_fcdata;

	public final ForecastData get_forecast_fcdata () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.forecast_fcdata while in state " + cur_modstate_string());
		}
		return forecast_fcdata;
	}


	// If catalog was obtained from a download file, this is it.  Otherwise, it is null.
	// Available when model state >= MODSTATE_CATALOG.

	private ForecastData loaded_fcdata;

	public final ForecastData get_loaded_fcdata () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {		// should be MODSTATE_CATALOG?
			throw new IllegalStateException ("Access to OEGUIModel.loaded_fcdata while in state " + cur_modstate_string());
		}
		return loaded_fcdata;
	}


	// The RJ sequence-specific model.
	// Available when model state >= MODSTATE_CATALOG.
	// It is non-null when state >= MODSTATE_PARAMETERS.
	// Note: We always generate the sequence-specific model, so it is always non-null when state >= MODSTATE_PARAMETERS.

	public final RJ_AftershockModel_SequenceSpecific get_cur_model () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cur_model while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		if (!( forecast_fcresults.seq_spec_result_avail )) {
			return null;
		}
		return forecast_fcresults.seq_spec_model;
	}


	// The RJ generic model.
	// Available when model state >= MODSTATE_CATALOG.
	// It is non-null when state >= MODSTATE_PARAMETERS.

	public final RJ_AftershockModel_Generic get_genericModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericModel while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		if (!( forecast_fcresults.generic_result_avail )) {
			return null;
		}
		return forecast_fcresults.generic_model;
	}


	// The RJ Bayesian model.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.
	
	public final RJ_AftershockModel_Bayesian get_bayesianModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.bayesianModel while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		if (!( forecast_fcresults.bayesian_result_avail )) {
			return null;
		}
		return forecast_fcresults.bayesian_model;
	}


	// The RJ generic JSON.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.

	public final String get_genericJSON () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericJSON while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		return forecast_fcresults.get_pdl_model_json (ForecastResults.PMCODE_GENERIC);
	}


	// The RJ sequence-specific JSON.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.

	public final String get_seqSpecJSON () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.seqSpecJSON while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		return forecast_fcresults.get_pdl_model_json (ForecastResults.PMCODE_SEQ_SPEC);
	}


	// The RJ bayesian JSON.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.

	public final String get_bayesianJSON () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.bayesianJSON while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		return forecast_fcresults.get_pdl_model_json (ForecastResults.PMCODE_BAYESIAN);
	}


	// The ETAS JSON.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.

	public final String get_etasJSON () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.etasJSON while in state " + cur_modstate_string());
		}
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			return null;
		}
		return forecast_fcresults.get_pdl_model_json (ForecastResults.PMCODE_ETAS);
	}


	// The ETAS full marginal distribution.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEMarginalDistSet get_etas_marginals () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_marginals while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).full_marginals;
		}
		return null;
	}


	// The ETAS integrated intensity function.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEtasIntegratedIntensityFile get_etas_ii_file () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_ii_file while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).ii_file;
		}
		return null;
	}


	// The ETAS MLE grid point for the active model.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEGridPoint get_etas_mle_grid_point () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_mle_grid_point while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).mle_grid_point;
		}
		return null;
	}


	// The ETAS MLE grid point for the generic model.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEGridPoint get_etas_gen_mle_grid_point () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_gen_mle_grid_point while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).gen_mle_grid_point;
		}
		return null;
	}


	// The ETAS MLE grid point for the sequence-specific model.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEGridPoint get_etas_seq_mle_grid_point () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_seq_mle_grid_point while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).seq_mle_grid_point;
		}
		return null;
	}


	// The ETAS MLE grid point for the Bayesian model.
	// Available when model state >= MODSTATE_PARAMETERS.
	// Returns null if not available.

	public final OEGridPoint get_etas_bay_mle_grid_point () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etas_bay_mle_grid_point while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return null;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return null;
		}
		if (forecast_fcresults.etas_outcome instanceof OEtasResults) {
			return ((OEtasResults)(forecast_fcresults.etas_outcome)).bay_mle_grid_point;
		}
		return null;
	}


	// True if the active ETAS model was created with a custom weight.
	// Available when model state >= MODSTATE_PARAMETERS.

	private boolean f_etas_active_custom = false;

	public final boolean get_f_etas_active_custom () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.boolean while in state " + cur_modstate_string());
		}
		if (!( forecast_fcresults.etas_result_avail )) {
			return false;
		}
		if (forecast_fcresults.etas_outcome == null) {
			return false;
		}
		return f_etas_active_custom;
	}


	// Return true if RJ generic model is selected.
	// Available when model state >= MODSTATE_PARAMETERS.

	public final boolean get_isGenericSelected () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.isGenericSelected while in state " + cur_modstate_string());
		}
		return (forecast_fcresults.get_selected_pdl_model() == ForecastResults.PMCODE_GENERIC);
	}


	// Return true if RJ sequence specific model is selected.
	// Available when model state >= MODSTATE_PARAMETERS.

	public final boolean get_isSeqSpecSelected () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.isSeqSpecSelected while in state " + cur_modstate_string());
		}
		return (forecast_fcresults.get_selected_pdl_model() == ForecastResults.PMCODE_SEQ_SPEC);
	}


	// Return true if RJ Bayesian model is selected.
	// Available when model state >= MODSTATE_PARAMETERS.

	public final boolean get_isBayesianSelected () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.isBayesianSelected while in state " + cur_modstate_string());
		}
		return (forecast_fcresults.get_selected_pdl_model() == ForecastResults.PMCODE_BAYESIAN);
	}


	// Return true if ETAS model is selected.
	// Available when model state >= MODSTATE_PARAMETERS.

	public final boolean get_isEtasSelected () {
		if (!( modstate >= MODSTATE_PARAMETERS )) {
			throw new IllegalStateException ("Access to OEGUIModel.isEtasSelected while in state " + cur_modstate_string());
		}
		return (forecast_fcresults.get_selected_pdl_model() == ForecastResults.PMCODE_ETAS);
	}




	// The generic parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final GenericRJ_Parameters get_genericParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.genericParams while in state " + cur_modstate_string());
		}
		return aafs_fcparams.generic_params;
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

	// The ETAS parameters for the mainshock.
	// Can be null if there are no ETAS parameters in the forecast parameters.
	// Available when model state >= MODSTATE_CATALOG.

	public final OEtasParameters get_etasParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_etasParams while in state " + cur_modstate_string());
		}
		if (!( aafs_fcparams.etas_avail )) {
			return null;
		}
		return aafs_fcparams.etas_params;
	}

	// The start of fitting interval for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final double get_fitStartInset () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_fitStartInset while in state " + cur_modstate_string());
		}
		if (!( aafs_fcparams.aftershock_search_avail )) {
			return ForecastParameters.DEFAULT_FIT_START_INSET;
		}
		return aafs_fcparams.fit_start_inset;
	}

	// The end of fitting interval for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	public final double get_fitEndInset () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_fitEndInset while in state " + cur_modstate_string());
		}
		if (!( aafs_fcparams.aftershock_search_avail )) {
			return ForecastParameters.DEFAULT_FIT_END_INSET;
		}
		return aafs_fcparams.fit_end_inset;
	}

	// The custom minimum magnitude bins for the mainshock.
	// Can be null if there are no custom minimum magnitude bins in the forecast parameters.
	// If the return is non-null, then it is a non-empty array.
	// Available when model state >= MODSTATE_CATALOG.

	public final double[] get_customMinMagBins () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.get_customMinMagBins while in state " + cur_modstate_string());
		}
		return aafs_fcparams.get_custom_fcopt_min_mag_bins();
	}

//	// The ETAS parameters for the mainshock.
//	// If there are no ETAS parameters in the forecast parameters,
//	// make a set of ETAS parameters for the mainshock location.
//	// Caller should handle the case of null parameters returned
//	// within the OAFRegimeParams, although this should not happen.
//	// Available when model state >= MODSTATE_CATALOG.
//
//	public final OAFRegimeParams<OEtasParameters> get_or_make_etasParams () {
//		if (!( modstate >= MODSTATE_CATALOG )) {
//			throw new IllegalStateException ("Access to OEGUIModel.get_or_make_etasParams while in state " + cur_modstate_string());
//		}
//
//		// If present in forecast parameters, return it
//
//		if (aafs_fcparams.etas_avail) {
//			return new OAFRegimeParams<OEtasParameters> (OAFTectonicRegime.forName (aafs_fcparams.etas_regime), aafs_fcparams.etas_params);
//		}
//
//		// If no mainshock, return null (should never happen)
//
//		if (!( fcmain.mainshock_avail )) {
//			return new OAFRegimeParams<OEtasParameters> (null, null);
//		}
//
//		// Otherwise, return parameters based on mainshock location
//
//		return (new OEtasConfig()).get_resolved_params (fcmain.get_eqk_location(), null);
//	}

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

	// Convert a forecast duration to days.

	public static double fc_duration_to_days (long fc_duration) {
		if (fc_duration % SimpleUtils.DAY_MILLIS == 0L) {
			return (double)(fc_duration / SimpleUtils.DAY_MILLIS);
		}
		return ((double)fc_duration) / SimpleUtils.DAY_MILLIS_D;
	}




	// Configured, minimum, default, and maximum event-sequence lookback.

	private long config_evseq_lookback;
	private long min_evseq_lookback;
	private long def_evseq_lookback;
	private long max_evseq_lookback;

	public double get_config_evseq_lookback_days () {
		return fc_duration_to_days (config_evseq_lookback);
	}

	public double get_max_evseq_lookback_days () {
		return fc_duration_to_days (max_evseq_lookback);
	}

	public long get_min_evseq_lookback () {
		return min_evseq_lookback;
	}

	public long get_max_evseq_lookback () {
		return max_evseq_lookback;
	}


	// Configured setting for event-sequence enable.

	private boolean config_is_evseq_enabled;

	public boolean get_config_is_evseq_enabled () {
		return config_is_evseq_enabled;
	}


	// Configured setting for ETAS enable.

	private boolean config_is_etas_enabled;

	public boolean get_config_is_etas_enabled () {
		return config_is_etas_enabled;
	}




	//----- Controller parameters for the model (used by the view) -----


	// Data start time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.
	// TODO: Obtain value from fetch_fcparams? Or from din_catalog?
	//
	// Notes: cat_dataStartTimeParam and cat_dataEndTimeParam are the catalog
	// start and end times used by the view for plotting.  They are reported to
	// the user through the data start and end time fields in the data source subpanel.
	// - If the catalog is loaded from Comcat, they are the start and end times
	//    used to fetch data.  They can be default values (-60 days to now), or
	//    specified by the user.  User-specified values may be adjusted.
	// - If the catalog is loaded from a file, they are the start and end times
	//    in the catalog file, or user-supplied values.
	// - If the catalog is obtained from a download file, they are the min_days
	//    and max_days values in the contained parameters, or user-supplied values.
	// In any case, these will be the start and end times in din_catalog.

	private double cat_dataStartTimeParam;

	public final double get_cat_dataStartTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_dataStartTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataStartTimeParam;
	}

	// Data end time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.
	// TODO: Obtain value from fetch_fcparams? Or from din_catalog?

	private double cat_dataEndTimeParam;

	public final double get_cat_dataEndTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_dataEndTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataEndTimeParam;
	}

	// Mc for sequence, for the catalog, when first loaded.
	// Available when model state >= MODSTATE_CATALOG.
	//
	// Note: At the time the catalog is loaded, this is computed as the magnitude
	// where the binned magnitude-number distribution reaches is peak, plus 0.5.
	// It is not subsequently changed.

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
	//
	// Note: At the time the catalog is loaded, this is initialized to cat_load_mcParam.
	// It can be changed by the user.  At the time of parameter fitting, it is set to
	// the value of mc used in the fitting.  It is used by the view in plotting the
	// magnitude-number distribution.  When parameters have not yet been fit, it is
	// used by the view in plotting the cumulative number distribution (earthquakes
	// with magnitude >= mc).

	private double cat_mcParam;

	public final double get_cat_mcParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_mcParam while in state " + cur_modstate_string());
		}
		return cat_mcParam;
	}

	// b-value, for the catalog, when first loaded.
	// Available when model state >= MODSTATE_CATALOG.
	//
	// Note: At the time the catalog is loaded, this is set to the b-value from the
	// generic RJ parameters.  It is not subsequently changed.

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
	//
	// Note: At the time the catalog is loaded, this is initialized to cat_load_bParam.
	// It can be changed by the user.  At the time of parameter fitting, it is set to
	// the value of b used in the fitting.  It is used by the view in plotting the
	// magnitude-number distribution.

	private double cat_bParam;

	public final double get_cat_bParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to OEGUIModel.cat_bParam while in state " + cur_modstate_string());
		}
		return cat_bParam;
	}

//	// Forecast start time, in days since the mainshock.
//	// Available when model state >= MODSTATE_FORECAST.
//
//	private double cat_forecastStartTimeParam;
//
//	public final double get_cat_forecastStartTimeParam () {
//		if (!( modstate >= MODSTATE_FORECAST )) {
//			throw new IllegalStateException ("Access to OEGUIModel.cat_forecastStartTimeParam while in state " + cur_modstate_string());
//		}
//		return cat_forecastStartTimeParam;
//	}
//
//	// Forecast start time, in days since the mainshock.
//	// Available when model state >= MODSTATE_FORECAST.
//
//	private double cat_forecastEndTimeParam;
//
//	public final double get_cat_forecastEndTimeParam () {
//		if (!( modstate >= MODSTATE_FORECAST )) {
//			throw new IllegalStateException ("Access to OEGUIModel.cat_forecastEndTimeParam while in state " + cur_modstate_string());
//		}
//		return cat_forecastEndTimeParam;
//	}




	// Clear all data structures that are not valid in the current state.

	private void clear_to_state () {

		// Structures not valid if we don't have a forecast

		if (modstate < MODSTATE_FORECAST) {
		}

		// Structures not valid if we don't have parameers

		if (modstate < MODSTATE_PARAMETERS) {
			forecast_fcparams = null;
			forecast_fcresults = null;
			forecast_analyst_opts = null;
			forecast_fcdata = null;
		}

		// Structures not valid if we don't have a catalog

		if (modstate < MODSTATE_CATALOG) {
			din_catalog = null;
			aafs_fcparams = null;
			fetch_fcparams = null;
			custom_search_region = null;
			resolved_search_region = null;
			resolved_region_spec = null;
			loaded_region_from_fc = false;
			loaded_params_from_fc = false;
			has_fetched_catalog = false;

			cur_aftershocks = null;
			strict_aftershocks = null;
			strict_foreshocks = null;
			outer_region = null;
			plot_aftershocks = null;
			existing_analyst_opts = null;
			forecast_fcparams = null;
			forecast_fcresults = null;
			forecast_analyst_opts = null;
			forecast_fcdata = null;
			loaded_fcdata = null;
			aftershockMND = null;
			mnd_mmaxc = 0.0;
		}

		// Structures not valid if we don't have a mainshock

		if (modstate < MODSTATE_MAINSHOCK) {
			fcmain = null;
			analyst_adj_params = null;

			cur_mainshock = null;
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

		din_catalog = null;
		fcmain = null;
		aafs_fcparams = null;
		fetch_fcparams = null;
		custom_search_region = null;
		resolved_search_region = null;
		resolved_region_spec = null;
		loaded_region_from_fc = false;
		loaded_params_from_fc = false;
		analyst_adj_params = null;
		has_fetched_catalog = false;

		cur_mainshock = null;
		cur_aftershocks = null;
		strict_aftershocks = null;
		strict_foreshocks = null;
		outer_region = null;
		plot_aftershocks = null;
		existing_analyst_opts = null;
		forecast_fcparams = null;
		forecast_fcresults = null;
		forecast_analyst_opts = null;
		forecast_fcdata = null;
		loaded_fcdata = null;
		aftershockMND = null;
		mnd_mmaxc = 0.0;

		// Get info from the configuration

		ActionConfig action_config = new ActionConfig();

		def_fc_duration = action_config.get_def_max_forecast_lag();
		max_fc_duration = action_config.get_extended_max_forecast_lag();
		min_fc_duration = Math.min (SimpleUtils.DAY_MILLIS, def_fc_duration);

		config_evseq_lookback = action_config.get_evseq_lookback();
		min_evseq_lookback = ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK;
		def_evseq_lookback = ActionConfigFile.DEFAULT_EVSEQ_LOOKBACK;
		max_evseq_lookback = ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK;

		config_is_evseq_enabled = action_config.get_is_evseq_enabled();

		config_is_etas_enabled = action_config.get_is_etas_enabled();

		// Set up information view

		register_all_info_items();

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




	// Clear the parameters that describe what is loaded from a forecast.
	// On return:
	//  - resolved_search_region is null.
	//  - resolved_region_spec contains a Standard region.
	//  - loaded_region_from_fc is false.
	//  - loaded_params_from_fc is false.

	private void clear_fc_load_params () {
		resolved_search_region = null;
		resolved_region_spec = new OEGUISubRegion.RegionSpec();		// sets to Standard region
		loaded_region_from_fc = false;
		loaded_params_from_fc = false;
	}




	// Set the parameters that describe what is loaded from a forecast, from a download file.
	// On return:
	//  - resolved_search_region is the search region from analyst parameters or forecast parameters, or null.
	//  - resolved_region_spec contains the region in analyst parameters, or a Standard region if no analyst parameters.
	//  - loaded_region_from_fc is false.
	//  - loaded_params_from_fc is true if there are analyst parameters.

	private void set_fc_load_params (ForecastData dlf) {
		resolved_search_region = null;
		resolved_region_spec = new OEGUISubRegion.RegionSpec();		// sets to Standard region
		loaded_region_from_fc = false;
		loaded_params_from_fc = false;

		// If we have analyst parameters ...

		if (dlf.analyst.analyst_params != null) {

			// Loaded params from forecast

			loaded_params_from_fc = true;

			// Get the search region spec from the analyst parameters

			resolved_region_spec.set_region_for_analyst (dlf.analyst.analyst_params, dlf.mainshock.get_eqk_location());

			// Get the custom search region from the analyst parameters, if any

			if (dlf.analyst.analyst_params.aftershock_search_avail) {
				resolved_search_region = dlf.analyst.analyst_params.aftershock_search_region;
			}
		}

		// If we didn't get the resolved search region, then look in the forecast parameters

		if (resolved_search_region == null && dlf.parameters.aftershock_search_avail) {
			resolved_search_region = dlf.parameters.aftershock_search_region;
		}

		return;
	}




	// Perform setup given the mainshock.
	// On entry, fcmain and cur_mainshock both contain the mainshock.
	// On return:
	//  - aafs_fcparams contains the default forecast parameters.
	//  - fetch_fcparams contains a copy of aafs_fcparams;
	//    in particular, fetch_fcparams.aftershock_search_region == null.
	//  - custom_search_region is the current analyst-supplied region, or null if none.
	//  - resolved_search_region is the region for the most recent forecast, or null if none.
	//  - resolved_region_spec is the spec for the analyst-supplied region in the most recent forecast, or the Standard region if none.
	//  - loaded_region_from_fc is false.
	//  - loaded_params_from_fc is true if there are analyst parameters in the most recent forecast.
	//  - analyst_adj_params is the current analyst-supplied adjustable parameters.
	//  //- Generic model is computed.

	private void setup_for_mainshock (OEGUIController.XferCatalogMod xfer) {

		// Display mainshock information

		System.out.println (fcmain.toString());

		// Existing analyst options, or null if none

		existing_analyst_opts = null;

		// Analyst-supplied forecast parameters, or null if none

		ForecastParameters analyst_fcparms = null;

		// Initialize load parameters

		clear_fc_load_params();

		// Display list of OAF products for this mainshock

		if (fcmain.mainshock_geojson != null) {

			// Most recent product, or null if none

			ComcatProductOaf oaf_product = null;

			// Loop to display all products and find the most recent

			List<ComcatProductOaf> oaf_product_list = ComcatProductOaf.make_list_from_gj (fcmain.mainshock_geojson);
			for (int k = 0; k < oaf_product_list.size(); ++k) {
				System.out.println ("OAF product: " + oaf_product_list.get(k).summary_string());
				if (oaf_product == null || oaf_product.updateTime < oaf_product_list.get(k).updateTime) {
					oaf_product = oaf_product_list.get(k);
				}
			}
			if (oaf_product_list.size() > 0) {
				System.out.println ();
			}

			// If we want to use analyst options ...

			if (xfer.x_dataSource.x_useAnalystOptionsParam) {

				// If the most recent product contains a ForecastData file ...

				if (oaf_product != null && oaf_product.contains_file (ForecastData.FORECAST_DATA_FILENAME)) {

					// Get the download file

					ForecastData dlf = new ForecastData();

					try {
						String s = oaf_product.read_string_from_contents (ForecastData.FORECAST_DATA_FILENAME);
						dlf.from_json_no_rebuild (s);
					}
					catch (Exception e) {
						throw new RuntimeException ("Error: Unable to read download file from Comcat", e);
					}

					// Set the load parameters

					set_fc_load_params (dlf);

					// Save the analyst options
						
					existing_analyst_opts = (new AnalystOptions()).copy_from (dlf.analyst);

					// If there are analyst-supplied parameters, save them

					if (dlf.analyst.analyst_params != null) {
						analyst_fcparms = dlf.analyst.analyst_params;
						System.out.println ("Using analyst-supplied parameters");
					}
				}
			}
		}

		// Set up aafs_fcparams to hold the forecast parameters

		aafs_fcparams = new ForecastParameters();

		try {
			if (!( aafs_fcparams.fetch_forecast_params (fcmain, analyst_fcparms) )) {
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

		// No custom search region, or analyst-supplied custom search region

		custom_search_region = null;

		if (analyst_fcparms != null && analyst_fcparms.aftershock_search_avail) {
			custom_search_region = analyst_fcparms.aftershock_search_region;
		}

		// Save adjustable parameters from analyst options

		analyst_adj_params = new AdjustableParameters();
		analyst_adj_params.setup_from_analyst_opts (existing_analyst_opts);

		// Make the generic RJ model

		//genericModel = new RJ_AftershockModel_Generic(fcmain.mainshock_mag, aafs_fcparams.generic_params);

		// As a courtesy, spit out the decimal days remaining in the origin day

		System.out.println("The mainshock occurred " + String.format("%.4f", getTimeRemainingInUTCDay()) + " days before midnight (UTC)\n");
		return;
	}




	// Perform setup given the mainshock and a download file
	// On entry, fcmain and cur_mainshock both contain the mainshock.
	// On return:
	//  - aafs_fcparams contains the default forecast parameters, from the download file.
	//  - fetch_fcparams contains a copy of aafs_fcparams;
	//    in particular, fetch_fcparams.aftershock_search_region contains the search region.
	//  - custom_search_region is the analyst-specified search region, or null if none.
	//  - resolved_search_region is the region for the supplied forecast, or null if none.
	//  - resolved_region_spec is the spec for the analyst-supplied region in the supplied forecast, or the Standard region if none.
	//  - loaded_region_from_fc is false.
	//  - loaded_params_from_fc is true if there are analyst parameters in the download file.
	//  - analyst_adj_params is the analyst-supplied adjustable parameters from the download file.
	//  //- Generic model is computed.
	// Note: This function should not be followed by a call to setup_search_region.

	private void setup_for_mainshock (OEGUIController.XferCatalogMod xfer, ForecastData dlf) {

		// Display mainshock information

		System.out.println (fcmain.toString());

		// Set up aafs_fcparams to hold the forecast parameters

		aafs_fcparams = new ForecastParameters();
		aafs_fcparams.copy_from (dlf.parameters);
		
		// Announce regimes and parameters

		System.out.println ("Generic parameters for regime " + aafs_fcparams.generic_regime + " used in forecast:");
		System.out.println (aafs_fcparams.generic_params.toString());

		System.out.println ("Magnitude-of-completeness parameters for regime " + aafs_fcparams.mag_comp_regime + " used in forecast:");
		System.out.println (aafs_fcparams.mag_comp_params.toString());

		System.out.println ("Sequence-specific parameters used in forecast:");
		System.out.println (aafs_fcparams.seq_spec_params.toString());

		// Copy to fetch_fcparams

		fetch_fcparams = new ForecastParameters();
		fetch_fcparams.copy_from (aafs_fcparams);

		// Save the analyst options
						
		existing_analyst_opts = (new AnalystOptions()).copy_from (dlf.analyst);

		// Analyst-specified custom search region, or null if none

		custom_search_region = null;
		if (dlf.analyst.analyst_params != null && dlf.analyst.analyst_params.aftershock_search_avail) {
			custom_search_region = dlf.analyst.analyst_params.aftershock_search_region;
		}

		// Set the load parameters

		set_fc_load_params (dlf);

		// Save adjustable parameters from analyst options

		analyst_adj_params = new AdjustableParameters();
		analyst_adj_params.setup_from_analyst_opts (existing_analyst_opts);

		// Make the generic RJ model
		// Note: Compare to ForecastResults.rebuild_generic_results, this is how the generic
		// model is rebuilt when the download file is loaded.  (Probably we could just use
		// the generic model in the download file, if the download file is rebuilt when loaded).

		//genericModel = new RJ_AftershockModel_Generic(fcmain.mainshock_mag, aafs_fcparams.generic_params);

		return;
	}




	// Set up the search region.
	// On entry, setup_for_mainshock(xfer) has been called.
	// On return:
	// - fetch_fcparams contains the search region and search parameters.
	// - fetch_fcparams.mag_comp_params contains the magnitude of completeness
	//   parameters, with the appropriate radius functions for the selected region,
	//   and the magnitude functions set to no minimum.
	// - xfer.x_dataSource.x_dataEndTimeParam adjusted if necessary to limit fetch to before now.

	private void setup_search_region (OEGUIController.XferCatalogMod xfer) {

		// Time range for aftershock search
		
		double minDays;
		double maxDays;

		// If we are using user-supplied time range ...

		if (xfer.x_dataSource.x_useStartEndTimeParam) {

			// User-supplied time range
		
			minDays = xfer.x_dataSource.x_dataStartTimeParam;
			maxDays = xfer.x_dataSource.x_dataEndTimeParam;
		
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
		}

		// Otherwise, default time range is data fetch lookback time to now ...

		else {

			// Start at data fetch lookback time

			long the_start_lag = -((new ActionConfig()).get_data_fetch_lookback());
			xfer.x_dataSource.modify_dataStartTimeParam (SimpleUtils.millis_to_days (the_start_lag));

			// End at now

			long time_now = System.currentTimeMillis();		// must be the actual system time, not ServerClock
			double calcMaxDays = (time_now - fcmain.mainshock_time)/ComcatOAFAccessor.day_millis;
			Preconditions.checkState(calcMaxDays > 0.00005, "Mainshock time is after now!");
			xfer.x_dataSource.modify_dataEndTimeParam(calcMaxDays);
		
			minDays = xfer.x_dataSource.x_dataStartTimeParam;
			maxDays = xfer.x_dataSource.x_dataEndTimeParam;
		}

		// The magnitude-of-completeness parameters

		double magCat = fetch_fcparams.mag_comp_params.get_magCat();
		MagCompFn magCompFn = fetch_fcparams.mag_comp_params.get_magCompFn();
		SearchMagFn magSample = fetch_fcparams.mag_comp_params.get_fcn_magSample();
		SearchRadiusFn radiusSample = fetch_fcparams.mag_comp_params.get_fcn_radiusSample();
		SearchMagFn magCentroid = fetch_fcparams.mag_comp_params.get_fcn_magCentroid();
		SearchRadiusFn radiusCentroid = fetch_fcparams.mag_comp_params.get_fcn_radiusCentroid();

		// Depth range for aftershock search, assume default
		
		double minDepth = ForecastParameters.SEARCH_PARAM_OMIT;
		double maxDepth = ForecastParameters.SEARCH_PARAM_OMIT;

		// Minimum magnitude is default, set from the mag-of-comp parameters

		double min_mag = ForecastParameters.SEARCH_PARAM_OMIT;

		// Fitting interval, assume default
		
		double fit_start_inset = ForecastParameters.SEARCH_PARAM_OMIT;
		double fit_end_inset = ForecastParameters.SEARCH_PARAM_OMIT;

		// Minimum magnitude to use for search, user-supplied or none

		double search_min_mag = SearchMagFn.NO_MIN_MAG;
		if (xfer.x_dataSource.x_useMinMagFetchParam) {
			search_min_mag = xfer.x_dataSource.x_minMagFetchParam;
		}

		// Synchronize resolved region spec with user-selected region

		if (resolved_region_spec == null) {
			resolved_region_spec = new OEGUISubRegion.RegionSpec();		// constructor defaults to Standard region
		}

		if (xfer.x_dataSource.x_region.is_standard_region()) {
			if (!( resolved_region_spec.is_standard_region() )) {
				xfer.x_dataSource.x_region.load_from_region_spec (resolved_region_spec);

				// We got the search region from the forecast

				loaded_region_from_fc = true;
			}
		} else {
			xfer.x_dataSource.x_region.save_to_region_spec (resolved_region_spec);
		}

		// Switch on region type
		// Note: Any region type other than STANDARD overrides an analyst-supplied region

		switch (xfer.x_dataSource.x_region.x_regionTypeParam) {

		case STANDARD:

			// Standard region, just change to selected or no minimum magnitude

			magSample = magSample.makeRemovedMinMag(search_min_mag);
			magCentroid = magCentroid.makeRemovedMinMag(search_min_mag);
			// Keep the existing custom_search_region
			break;

		case CENTROID_WC_CIRCLE:

			// WC circle around centroid, set multiplier and limits, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusSample = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			magCentroid = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusCentroid = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			custom_search_region = null;
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CENTROID_CIRCLE:

			// Circle around centroid, set constant radius, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusSample = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			magCentroid = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusCentroid = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			custom_search_region = null;
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case EPICENTER_WC_CIRCLE:

			// WC circle around epicenter, set multiplier and limits, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusSample = SearchRadiusFn.makeWCClip (
				xfer.x_dataSource.x_region.x_wcMultiplierParam,
				xfer.x_dataSource.x_region.x_minRadiusParam,
				xfer.x_dataSource.x_region.x_maxRadiusParam
			);
			magCentroid = SearchMagFn.makeSkipCentroid();
			radiusCentroid = SearchRadiusFn.makeConstant (0.0);
			custom_search_region = null;
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case EPICENTER_CIRCLE:

			// Circle around epicenter, set constant radius, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
			radiusSample = SearchRadiusFn.makeConstant (xfer.x_dataSource.x_region.x_radiusParam);
			magCentroid = SearchMagFn.makeSkipCentroid();
			radiusCentroid = SearchRadiusFn.makeConstant (0.0);
			custom_search_region = null;
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CUSTOM_CIRCLE:

			// Custom circle, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
			magCentroid = SearchMagFn.makeSkipCentroid();
			custom_search_region = SphRegion.makeCircle (
				new SphLatLon(xfer.x_dataSource.x_region.x_centerLatParam, xfer.x_dataSource.x_region.x_centerLonParam),
				xfer.x_dataSource.x_region.x_radiusParam
			);
			minDepth = xfer.x_dataSource.x_region.x_minDepthParam;
			maxDepth = xfer.x_dataSource.x_region.x_maxDepthParam;
			break;

		case CUSTOM_RECTANGLE:

			// Custom rectangle, and selected or no minimum magnitude

			magSample = SearchMagFn.makeNoMinMag(search_min_mag);
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

		fetch_fcparams.mag_comp_avail = true;

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
			min_mag,				// double the_min_mag,
			fit_start_inset,		// double the_fit_start_inset,
			fit_end_inset			// double the_fit_end_inset
		);

		if (!( fetch_fcparams.aftershock_search_avail )) {
			throw new IllegalStateException("Failed to build aftershock search region");
		}

		// Save the resolved region

		resolved_search_region = fetch_fcparams.aftershock_search_region;

		return;
	}




	// Perform actions after fetching or loading the catalog.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.

	private void postFetchActions (OEGUIController.XferCatalogMod xfer) {

		// Extract sorted aftershock lists from the catalog

		cur_aftershocks = din_catalog.get_joined_rup_list (true,
			GUIExternalCatalogV2.EQCAT_FORESHOCK, GUIExternalCatalogV2.EQCAT_AFTERSHOCK);

		strict_aftershocks = din_catalog.get_joined_rup_list (true,
			GUIExternalCatalogV2.EQCAT_AFTERSHOCK);

		strict_foreshocks = din_catalog.get_joined_rup_list (true,
			GUIExternalCatalogV2.EQCAT_FORESHOCK);

		plot_aftershocks = din_catalog.get_joined_rup_list (true,
			GUIExternalCatalogV2.EQCAT_FORESHOCK, GUIExternalCatalogV2.EQCAT_AFTERSHOCK, GUIExternalCatalogV2.EQCAT_REGIONAL);
		
		System.out.println ("Obtained " + strict_aftershocks.size() + " aftershocks");
		System.out.println ("Obtained " + strict_foreshocks.size() + " foreshocks");
		System.out.println ("Obtained " + (plot_aftershocks.size() - strict_aftershocks.size() - strict_foreshocks.size()) + " regional earthquakes");

		// Compute the magnitude-number distribution of aftershocks (in bins)

		aftershockMND = ObsEqkRupListCalc.getMagNumDist(strict_aftershocks, 1.05, 81, 0.1);

		// Find the magnitude bin with the maximum number of aftershocks

		mnd_mmaxc = AftershockStatsCalc.getMmaxC(aftershockMND);

		// Set the default magnitude of completeness to the peak of the mag-num distribution, plus 0.5 magnitude

		cat_load_mcParam = mnd_mmaxc + 0.5;

		// Set the default b-value from the generic parameters

		cat_load_bParam = aafs_fcparams.generic_params.get_bValue();

		// Save the catalog parameters

		cat_dataStartTimeParam = xfer.x_dataSource.x_dataStartTimeParam;
		cat_dataEndTimeParam = xfer.x_dataSource.x_dataEndTimeParam;
		cat_mcParam = cat_load_mcParam;
		cat_bParam = cat_load_bParam;

		return;
	}




	// Fetch mainshock PDL info from Comcat.
	// Parameters:
	//  query_id = Event ID used to query Comcat.
	// Throws exception if event not found or Comcat error.
	// The PDL info is added to the existing fcmain, leaving time, mag, and location unchanged.

	public void fetch_mainshock_pdl_info (String query_id) {
		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.fetch_mainshock_pdl_info while in state " + cur_modstate_string());
		}

		ForecastMainshock my_fcmain = new ForecastMainshock();
		my_fcmain.setup_mainshock_poll (query_id);
		Preconditions.checkState(my_fcmain.mainshock_avail, "Event not found: %s", query_id);

		fcmain.copy_from_no_time_mag_loc (my_fcmain);
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

		// Allocate the catalog

		din_catalog = new GUIExternalCatalogV2();

		// Get the aftershocks

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		boolean my_wrapLon = false;

		// If we can make an outer region ...

		if (xfer.x_dataSource.x_useOuterRegionParam && fetch_fcparams.aftershock_search_region.isCircular() && fetch_fcparams.aftershock_search_region.getCircleRadiusKm() <= 3000.0) {

			// Make the outer region as a larger circle

			double radius_delta = Math.min (500.0, Math.max (15.0, fetch_fcparams.aftershock_search_region.getCircleRadiusKm() * 0.70));
			outer_region = SphRegion.makeCircle (fetch_fcparams.aftershock_search_region.getCircleCenter(), radius_delta + fetch_fcparams.aftershock_search_region.getCircleRadiusKm());

			// Set symbols in the catalog

			din_catalog.symdef_add_comcat (
				cur_mainshock,
				fetch_fcparams.min_days,
				fetch_fcparams.max_days,
				fetch_fcparams.min_depth,
				fetch_fcparams.max_depth,
				fetch_fcparams.aftershock_search_region,
				outer_region,
				my_wrapLon,
				fetch_fcparams.min_mag
			);

			// Fetch everything in the outer region

			ObsEqkRupList my_aftershocks = accessor.fetchAftershocks (
				cur_mainshock,
				fetch_fcparams.min_days,
				fetch_fcparams.max_days,
				fetch_fcparams.min_depth,
				fetch_fcparams.max_depth,
				outer_region,
				my_wrapLon,
				fetch_fcparams.min_mag
			);

			// Add them to the catalog

			din_catalog.add_all_quakes (cur_mainshock, my_aftershocks, fetch_fcparams.aftershock_search_region);
		}

		// Otherwise, just fetch within the aftershock region ...

		else {

			// No outer region

			outer_region = null;

			// Set symbols in the catalog

			din_catalog.symdef_add_comcat (
				cur_mainshock,
				fetch_fcparams.min_days,
				fetch_fcparams.max_days,
				fetch_fcparams.min_depth,
				fetch_fcparams.max_depth,
				fetch_fcparams.aftershock_search_region,
				outer_region,
				my_wrapLon,
				fetch_fcparams.min_mag
			);

			// Fetch everything in the aftershock search region

			ObsEqkRupList my_aftershocks = accessor.fetchAftershocks (
				cur_mainshock,
				fetch_fcparams.min_days,
				fetch_fcparams.max_days,
				fetch_fcparams.min_depth,
				fetch_fcparams.max_depth,
				fetch_fcparams.aftershock_search_region,
				my_wrapLon,
				fetch_fcparams.min_mag
			);

			// Add them to the catalog

			din_catalog.add_all_quakes (cur_mainshock, my_aftershocks, null);
		}

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}




	// Fetch only the mainshock from Comcat.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate mainshock fetch.

	public void fetchMainshockOnly (OEGUIController.XferCatalogMod xfer) {

		System.out.println("Fetching Mainshock");
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

		return;
	}




	// Load catalog from a file.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	//  src = Source to read lines from the file.
	// This is called by the controller to initiate catalog load.
	// Part 1 reads the catalog file.
	// Part 2 performs parameter validation.
	// Part 3 contacts Comcat for mainshock info (if selected) and completes the load.

	public void loadCatalog_1 (OEGUIController.XferCatalogMod xfer, Supplier<String> src) {

		// Will not be fetching from Comcat

		has_fetched_catalog = false;

		// Allocate the catalog

		din_catalog = new GUIExternalCatalogV2();

		// Load the catalog

		din_catalog.read_from_supplier (src);

		return;
	}

	public void loadCatalog_2 (OEGUIController.XferCatalogMod xfer) {

		// Check parameters

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION)) {
			try {
				SphRegion x = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION, e);
			}
		} else {
			if (!( xfer.x_dataSource.x_useRegionForLoadParam )) {
				throw new RuntimeException ("File does not contain an aftershock search region, and the user did not supply one");
			}
		}

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_OUTER_REGION)) {
			try {
				SphRegion x = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_OUTER_REGION);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_OUTER_REGION, e);
			}
		}

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_WRAP_LON)) {
			try {
				boolean x = din_catalog.get_symbol_as_boolean (GUIExternalCatalogV2.SSYM_WRAP_LON);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_WRAP_LON, e);
			}
		}

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_TIME_RANGE)) {
			try {
				double[] x = din_catalog.get_symbol_as_double_array (GUIExternalCatalogV2.SSYM_TIME_RANGE, 2);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_TIME_RANGE, e);
			}
		} else {
			if (!( xfer.x_dataSource.x_useStartEndTimeParam )) {
				throw new RuntimeException ("File does not contain a time range, and the user did not supply one");
			}
		}

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_DEPTH_RANGE)) {
			try {
				double[] x = din_catalog.get_symbol_as_double_array (GUIExternalCatalogV2.SSYM_DEPTH_RANGE, 2);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_DEPTH_RANGE, e);
			}
		}

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_MIN_MAG)) {
			try {
				double x = din_catalog.get_symbol_as_double (GUIExternalCatalogV2.SSYM_MIN_MAG);
			}
			catch (Exception e) {
				throw new RuntimeException ("File contains invalid parameter: " + GUIExternalCatalogV2.SSYM_MIN_MAG, e);
			}
		}

		// Check mainshock

		int main_count = din_catalog.get_rup_list_size (GUIExternalCatalogV2.EQCAT_MAINSHOCK);

		if (main_count > 1) {
			throw new RuntimeException ("File contains more than one mainshock");
		}
		else if (main_count < 1) {
			if (xfer.x_dataSource.x_useComcatForMainshockParam) {
				if (xfer.x_dataSource.x_eventIDParam == null) {
					throw new RuntimeException ("File does not contain a mainshock, and the user did not supply an event ID to load a mainshock from Comcat");
				}
			} else {
				throw new RuntimeException ("File does not contain a mainshock, and the option to load a mainshock from ComCat was not selected");
			}
		}
		else {
			if (xfer.x_dataSource.x_useComcatForMainshockParam) {
				if (xfer.x_dataSource.x_eventIDParam == null) {
					String id = din_catalog.get_mainshock().getEventId();
					if (id == null || id.trim().isEmpty() || EventIDGenerator.is_generated_id(id.trim())) {
						throw new RuntimeException ("File does not contain a mainshock event ID, and the user did not supply an event ID to load a mainshock from Comcat");
					}
				}
			}
		}

		// Display load result
		
		System.out.println ("Loaded " + din_catalog.get_total_size() + " earthquakes from file");

		// Install user-supplied region, if any

		if (xfer.x_dataSource.x_useRegionForLoadParam) {

			// Compare it to an outer region if available, or an aftershock region if available

			SphRegion compare_region = null;
			if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_OUTER_REGION)) {
				compare_region = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_OUTER_REGION);
			}
			else if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION)) {
				compare_region = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION);
			}

			// The user region

			SphRegion user_region = xfer.x_dataSource.x_aftershockRegionParam;

			// Warn if it is outside the region in the file

			if (compare_region != null) {
				if (SphRegion.is_known_non_subset (user_region.compare_to (compare_region))) {
					System.out.println ("WARNING: User-selected region is not a subset of the region in the file");
				}
			}

			// Insert region into file, replacing any existing region

			din_catalog.symdef_remove (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION);
			din_catalog.symdef_add_region (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION, user_region);
		}

		// Install user-supplied time range, if any

		if (xfer.x_dataSource.x_useStartEndTimeParam) {

			// Compare it to a time range in the file, if available

			double[] compare_time_range = null;
			if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_TIME_RANGE)) {
				compare_time_range = din_catalog.get_symbol_as_double_array (GUIExternalCatalogV2.SSYM_TIME_RANGE, 2);
			}

			// The user time range

			double the_min_days = xfer.x_dataSource.x_dataStartTimeParam;
			double the_max_days = xfer.x_dataSource.x_dataEndTimeParam;

			// Warn if it is outside the range in the file

			if (compare_time_range != null) {
				if (the_min_days < compare_time_range[0] || the_max_days > compare_time_range[1]) {
					System.out.println ("WARNING: User-selected time range is not a subset of the time range in the file");
				}
			}

			// Insert time range into file, replacing any existing time range

			din_catalog.symdef_remove (GUIExternalCatalogV2.SSYM_TIME_RANGE);
			din_catalog.symdef_add_double_array (GUIExternalCatalogV2.SSYM_TIME_RANGE, the_min_days, the_max_days);
		}

		return;
	}

	public void loadCatalog_3 (OEGUIController.XferCatalogMod xfer) {

		// If we want to load mainshock from Comcat ...

		boolean f_used_event_id = false;

		if (xfer.x_dataSource.x_useComcatForMainshockParam) {

			// Get the user-supplied event id

			String query_id = xfer.x_dataSource.x_eventIDParam;

			// If none, use the event id for the mainshock in the file (we already checked it exists)

			if (query_id == null) {
				query_id = din_catalog.get_mainshock().getEventId();
			}

			// Otherwise, see if the event id is an alias, and change it if so

			else {
				String xlatid = GUIEventAlias.query_alias_dict (query_id);
				if (xlatid != null) {
					System.out.println("Translating Event ID: " + query_id + " -> " + xlatid);
					xfer.x_dataSource.modify_eventIDParam(xlatid);
					query_id = xlatid;
				}

				f_used_event_id = true;
			}

			// Fetch the mainshock from Comcat

			fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_poll (query_id);
			Preconditions.checkState(fcmain.mainshock_avail, "Event not found: %s", query_id);

			// If the file contains a mainshock, use its time, mag, and location in place of Comcat's

			int main_count = din_catalog.get_rup_list_size (GUIExternalCatalogV2.EQCAT_MAINSHOCK);
			if (main_count == 1) {
				fcmain.override_time_mag_loc (din_catalog.get_mainshock());
			}

			// The mainshock

			cur_mainshock = fcmain.get_eqk_rupture();
		}

		// Otherwise, use mainshock from the file

		else {
			cur_mainshock = din_catalog.get_mainshock();

			fcmain = new ForecastMainshock();
			fcmain.setup_local_mainshock (cur_mainshock);
		}

		// If we didn't use the event ID field, establish name of mainshock

		if (!( f_used_event_id )) {
			if (cur_mainshock.getEventId() == null || cur_mainshock.getEventId().trim().isEmpty() || EventIDGenerator.is_generated_id(cur_mainshock.getEventId())) {
				xfer.x_dataSource.modify_eventIDParam ("_catalog_file");
			} else {
				xfer.x_dataSource.modify_eventIDParam (cur_mainshock.getEventId().trim());
			}
		}

		// Finish setting up the mainshock

		setup_for_mainshock (xfer);

		// Get parameters and create a filter

		ObsEqkRupFilter my_din_filter = new ObsEqkRupFilter();

		// Parameter: aftershock search region, already in the file

		SphRegion the_aftershock_search_region = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_AFTERSHOCK_REGION);

		// Parameter: outer region

		outer_region = null;
		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_OUTER_REGION)) {
			outer_region = din_catalog.get_symbol_as_region (GUIExternalCatalogV2.SSYM_OUTER_REGION);
		}

		// If aftershock region was user-supplied, apply it as a filter with no outer region

		//  if (xfer.x_dataSource.x_useRegionForLoadParam) {
		//  	my_din_filter.set_include_region_filter (the_aftershock_search_region);
		//  	outer_region = null;
		//  }

		// Parameter: time range, already in the file

		double[] time_range = din_catalog.get_symbol_as_double_array (GUIExternalCatalogV2.SSYM_TIME_RANGE, 2);
		double the_min_days = time_range[0];
		double the_max_days = time_range[1];
			
		// Set filter to accept ruptures in the time range
		
		my_din_filter.set_time_range_filter (cur_mainshock, the_min_days, the_max_days);

		// If time range does not come from the user, write it back to the user

		if (!( xfer.x_dataSource.x_useStartEndTimeParam )) {
			xfer.x_dataSource.modify_dataStartTimeParam(the_min_days);
			xfer.x_dataSource.modify_dataEndTimeParam(the_max_days);
		}

		// Parameter: depth range

		double the_min_depth;
		double the_max_depth;

		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_DEPTH_RANGE)) {
			double[] depth_range = din_catalog.get_symbol_as_double_array (GUIExternalCatalogV2.SSYM_DEPTH_RANGE, 2);
			the_min_depth = depth_range[0];
			the_max_depth = depth_range[1];
		} else {
			the_min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
			the_max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
		}

		// Parameter: minimum magnitude

		double the_min_mag;
		if (din_catalog.has_symbol (GUIExternalCatalogV2.SSYM_MIN_MAG)) {
			the_min_mag = din_catalog.get_symbol_as_double (GUIExternalCatalogV2.SSYM_MIN_MAG);

			// If the catalog supplies a minimum magnitude, add it to the filter

			my_din_filter.set_min_mag_filter (the_min_mag);

		} else {
			the_min_mag = ComcatOAFAccessor.COMCAT_NO_MIN_MAG;		// -10.0 = no minimum
		}

		// Parameter: insets

		double the_fit_start_inset = ForecastParameters.DEFAULT_FIT_START_INSET;
		double the_fit_end_inset = ForecastParameters.DEFAULT_FIT_END_INSET;

		// Save the parameters

		boolean the_aftershock_search_avail = true;

		fetch_fcparams.set_analyst_aftershock_search_params (
			the_aftershock_search_avail,
			the_aftershock_search_region,
			the_min_days,
			the_max_days,
			the_min_depth,
			the_max_depth,
			the_min_mag,
			the_fit_start_inset,
			the_fit_end_inset
		);

		// The region from a file is treated as a custom region

		custom_search_region = the_aftershock_search_region;

		// Resolved region and spec

		clear_fc_load_params();
		resolved_search_region = custom_search_region;
		resolved_region_spec = null;

		// Reclassify earthquakes and apply filter

		din_catalog.reclassify_earthquakes (the_aftershock_search_region, my_din_filter);

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}




	// Save the catalog to a file.
	// Parameters:
	//  dest = Destination to write the lines of the catalog.

	public void saveCatalog (Consumer<String> dest) {

		if (!( modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIModel.saveCatalog - Invalid model state: " + cur_modstate_string());
		}

		// Write the file

		get_din_catalog().write_to_consumer (dest);
		return;
	}




	// Load catalog from a published forecast.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate catalog load from a published forecast.

	public void loadCatFromForecast (OEGUIController.XferCatalogMod xfer) {

		// Indicate if we are fetching from Comcat; controls whether server ops are allowed

		//has_fetched_catalog = false;
		has_fetched_catalog = true;

		// See if the event ID is an alias, and change it if so

		String xlatid = GUIEventAlias.query_alias_dict (xfer.x_dataSource.x_eventIDParam);
		if (xlatid != null) {
			System.out.println("Translating Event ID: " + xfer.x_dataSource.x_eventIDParam + " -> " + xlatid);
			xfer.x_dataSource.modify_eventIDParam(xlatid);
		}

		// The OAF product

		ComcatProductOaf oaf_product = xfer.x_dataSource.x_oaf_product;

		// If we want the current product for our event ...

		if (oaf_product == null) {

			// Get the event id

			String event_id = xfer.x_dataSource.x_eventIDParam;

			// Get the superseded flag

			boolean f_superseded = false;

			// Get the event information

			ComcatOAFAccessor accessor = new ComcatOAFAccessor ();
			ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true, f_superseded);

			if (rup == null) {
				throw new RuntimeException ("Earthquake not found: id = " + event_id);
			}

			// Get the list of products

			boolean delete_ok = false;
			List<ComcatProductOaf> product_list = ComcatProductOaf.make_list_from_gj (accessor.get_last_geojson(), delete_ok);

			// Find the most recent product with a ForecastData file (should only be one product in list)

			for (ComcatProductOaf prod : product_list) {
				if (prod.contains_file (ForecastData.FORECAST_DATA_FILENAME)) {
					if (oaf_product == null || oaf_product.updateTime < prod.updateTime) {
						oaf_product = prod;
					}
				}
			}

			// Exception if no product available

			if (oaf_product == null) {
				throw new RuntimeException ("No forecast with a download file was found");
			}
		}

		// Tell user which forecast we are using

		System.out.println ("Using forecast for earthquake " + xfer.x_dataSource.x_eventIDParam + " issued at " + SimpleUtils.time_to_string (oaf_product.updateTime));

		// Get the download file

		ForecastData dlf = new ForecastData();

		try {
			String s = oaf_product.read_string_from_contents (ForecastData.FORECAST_DATA_FILENAME);
			dlf.from_json_no_rebuild (s);
		}
		catch (Exception e) {
			throw new RuntimeException ("Error: Unable to read download file from Comcat", e);
		}

		if (!( dlf.mainshock.mainshock_avail )) {
			throw new RuntimeException ("Error: Download file does not contain a mainshock");
		}

		if (!( dlf.parameters.aftershock_search_avail
			&& dlf.parameters.aftershock_search_region != null
		)) {
			throw new RuntimeException ("Error: Download file does not contain a search region");
		}

		// Get the catalog

		CompactEqkRupList cat = dlf.catalog.get_rupture_list();
		if (cat == null) {
			throw new RuntimeException ("Error: Download file does not contain a catalog");
		}

		// Get the catalog time range, in days since the mainshock

		xfer.x_dataSource.modify_dataStartTimeParam (dlf.parameters.min_days);
		xfer.x_dataSource.modify_dataEndTimeParam (dlf.parameters.max_days);

		// Store mainshock into our data structures

		boolean my_wrapLon = false;

		cur_mainshock = dlf.mainshock.get_eqk_rupture();

		fcmain = new ForecastMainshock();
		fcmain.copy_from (dlf.mainshock);

		// Save the download file

		loaded_fcdata = dlf;

		// Finish setting up the mainshock

		setup_for_mainshock (xfer, dlf);

		// Allocate the catalog

		din_catalog = new GUIExternalCatalogV2();

		// Set symbols in the catalog

		din_catalog.symdef_add_comcat (
			cur_mainshock,
			fetch_fcparams.min_days,
			fetch_fcparams.max_days,
			fetch_fcparams.min_depth,
			fetch_fcparams.max_depth,
			fetch_fcparams.aftershock_search_region,
			null,
			my_wrapLon,
			fetch_fcparams.min_mag
		);

		// Get the list of aftershocks and foreshocks

		ObsEqkRupList my_aftershocks = new ObsEqkRupList();
		int neqk = cat.get_eqk_count();
		for (int index = 0; index < neqk; ++index) {
			my_aftershocks.add (cat.get_wrapped (index, "", my_wrapLon));
		}

		// Add them to the catalog

		din_catalog.add_all_quakes (cur_mainshock, my_aftershocks, null);

		// Display catalog result
		
		System.out.println ("Loaded " + din_catalog.get_total_size() + " earthquakes from published forecast");

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}




	// Load catalog from a saved download file.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate catalog load from a saved download file.
	// Part 1 reads the download file.
	// Part 2 contacts Comcat for mainshock info (if selected) and completes the load.

	public void loadCatFromDownload_1 (OEGUIController.XferCatalogMod xfer) {

		// Get the download file

		ForecastData dlf = new ForecastData();

		try {
			String filename = xfer.x_dataSource.x_catalogFileParam;
			dlf.from_json_file_no_rebuild (filename);
		}
		catch (Exception e) {
			throw new RuntimeException ("Error: Unable to read download file", e);
		}

		if (!( dlf.mainshock.mainshock_avail )) {
			throw new RuntimeException ("Error: Download file does not contain a mainshock");
		}

		if (!( dlf.parameters.aftershock_search_avail
			&& dlf.parameters.aftershock_search_region != null
		)) {
			throw new RuntimeException ("Error: Download file does not contain a search region");
		}

		// Save download file for use in part 2

		loaded_fcdata = dlf;

		return;
	}

	public void loadCatFromDownload_2 (OEGUIController.XferCatalogMod xfer) {

		// The download file

		ForecastData dlf = loaded_fcdata;

		// If we want to update mainshock info ...

		if (xfer.x_dataSource.x_useComcatForMainshockParam) {

			// Get the mainshock id from the file

			String query_id = dlf.mainshock.mainshock_event_id;

			// Fetch the mainshock from Comcat

			ForecastMainshock my_fcmain = new ForecastMainshock();
			my_fcmain.setup_mainshock_poll (query_id);
			Preconditions.checkState(my_fcmain.mainshock_avail, "Event not found: %s", query_id);

			// Update mainshock info in the file
		
			dlf.mainshock.copy_from_no_time_mag_loc (my_fcmain);
		}

		// Update event id from the file (must come after mainshock update which might alter the id)

		xfer.x_dataSource.modify_eventIDParam (dlf.mainshock.mainshock_event_id);

		// Get the catalog

		CompactEqkRupList cat = dlf.catalog.get_rupture_list();
		if (cat == null) {
			throw new RuntimeException ("Error: Download file does not contain a catalog");
		}

		// Get the catalog time range, in days since the mainshock

		xfer.x_dataSource.modify_dataStartTimeParam (dlf.parameters.min_days);
		xfer.x_dataSource.modify_dataEndTimeParam (dlf.parameters.max_days);

		// Store mainshock into our data structures

		boolean my_wrapLon = false;

		cur_mainshock = dlf.mainshock.get_eqk_rupture();

		fcmain = new ForecastMainshock();
		fcmain.copy_from (dlf.mainshock);

		// Save the download file (already done)

		//loaded_fcdata = dlf;

		// Finish setting up the mainshock

		setup_for_mainshock (xfer, dlf);

		// Allocate the catalog

		din_catalog = new GUIExternalCatalogV2();

		// Set symbols in the catalog

		din_catalog.symdef_add_comcat (
			cur_mainshock,
			fetch_fcparams.min_days,
			fetch_fcparams.max_days,
			fetch_fcparams.min_depth,
			fetch_fcparams.max_depth,
			fetch_fcparams.aftershock_search_region,
			null,
			my_wrapLon,
			fetch_fcparams.min_mag
		);

		// Get the list of aftershocks and foreshocks

		ObsEqkRupList my_aftershocks = new ObsEqkRupList();
		int neqk = cat.get_eqk_count();
		for (int index = 0; index < neqk; ++index) {
			my_aftershocks.add (cat.get_wrapped (index, "", my_wrapLon));
		}

		// Add them to the catalog

		din_catalog.add_all_quakes (cur_mainshock, my_aftershocks, null);

		// Display catalog result
		
		System.out.println ("Loaded " + din_catalog.get_total_size() + " earthquakes from published forecast");

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

		// Allocate parameter structure, initialized to the catalog fetch parameters

		forecast_fcparams = new ForecastParameters();
		forecast_fcparams.copy_from (fetch_fcparams);

		// Allocate structure to hold results

		forecast_fcresults = new ForecastResults();

		// Allocate analyst options, and the parameters within, initialized to the starting point for analyst parameters

		forecast_analyst_opts = new AnalystOptions();
		forecast_analyst_opts.analyst_params = new ForecastParameters();
		forecast_analyst_opts.analyst_params.setup_all_default();

		// Allocate data structure

		forecast_fcdata = new ForecastData();

		// Get RJ sequence-specific parameters

		Range aRange = xfer.x_rjValue.x_aValRangeParam;
		int aNum = xfer.x_rjValue.x_aValNumParam;
		//validateRange(aRange, aNum, "a-value");
		Range pRange = xfer.x_rjValue.x_pValRangeParam;
		int pNum = xfer.x_rjValue.x_pValNumParam;
		//validateRange(pRange, pNum, "p-value");
		Range cRange = xfer.x_rjValue.x_cValRangeParam;
		int cNum = xfer.x_rjValue.x_cValNumParam;
		//validateRange(cRange, cNum, "c-value");

		// Get common forecast parameters
					
		double mc = xfer.x_commonValue.x_mcParam;
		//double mc = cat_mcParam;
									
		double b = xfer.x_commonValue.x_bParam;
		//double b = cat_bParam;

		// Form the RJ sequence-specific parameters, save into both forecast and analyst parameters

		SeqSpecRJ_Parameters user_seq_spec_params = new SeqSpecRJ_Parameters (
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

		forecast_fcparams.set_analyst_seq_spec_params (
			true,							// seq_spec_avail
			user_seq_spec_params			// seq_spec_params
		);

		forecast_analyst_opts.analyst_params.set_analyst_seq_spec_params (
			true,							// seq_spec_avail
			user_seq_spec_params			// seq_spec_params
		);

		fetch_fcparams.seq_spec_params = user_seq_spec_params;	// so it will be seen by make_analyst_options()

		//  fetch_fcparams.set_analyst_seq_spec_params (
		//  	true,							// seq_spec_avail
		//  	user_seq_spec_params			// seq_spec_params
		//  );								// so it will be seen by make_analyst_options()

		// Magnitude of completeness info
						
		double mCat;

		MagCompFn magCompFn;

		// Switch on time-dependent magnitude of completeness option

		switch (xfer.x_commonValue.x_timeDepOptionParam) {

		// If doing time-dependent magnitude of completeness

		case ENABLE:
		case WORLD:
		case CALIFORNIA:

			double f = xfer.x_commonValue.x_fParam;
						
			double g = xfer.x_commonValue.x_gParam;
						
			double h = xfer.x_commonValue.x_hParam;
						
			mCat = xfer.x_commonValue.x_mCatParam;

			magCompFn = MagCompFn.makePageOrConstant (f, g, h);

			break;

		// Otherwise, time-independent magnitude of completeness

		case EQUALS_MCAT:
						
			mCat = xfer.x_commonValue.x_mCatParam;

			magCompFn = MagCompFn.makeConstant();

			break;

		case EQUALS_MC:
						
			//mCat = xfer.x_commonValue.x_mCatParam;
			mCat = mc;

			magCompFn = MagCompFn.makeConstant();

			break;

		default:
			throw new IllegalArgumentException ("OEGUIModel.fitParams: Invalid magnitude of completeness option");
		}

		// Form magnitude-of-completeness parameters, save into both forecast and analyst parameters

		SearchMagFn magSample = aafs_fcparams.mag_comp_params.get_fcn_magSample();	// the original value
		SearchRadiusFn radiusSample = forecast_fcparams.mag_comp_params.get_fcn_radiusSample();
		SearchMagFn magCentroid = aafs_fcparams.mag_comp_params.get_fcn_magCentroid();	// the original value
		SearchRadiusFn radiusCentroid = forecast_fcparams.mag_comp_params.get_fcn_radiusCentroid();

		MagCompPage_Parameters user_mag_comp_params = new MagCompPage_Parameters (
			mCat,
			magCompFn,
			magSample.makeForAnalystMagCat (mCat),
			radiusSample,
			magCentroid.makeForAnalystMagCat (mCat),
			radiusCentroid
		);

		forecast_fcparams.set_analyst_mag_comp_params (
			true,										// mag_comp_avail
			aafs_fcparams.mag_comp_regime,				// mag_comp_regime
			user_mag_comp_params						// mag_comp_params
		);

		forecast_analyst_opts.analyst_params.set_analyst_mag_comp_params (
			true,										// mag_comp_avail
			aafs_fcparams.mag_comp_regime,				// mag_comp_regime
			user_mag_comp_params						// mag_comp_params
		);

		fetch_fcparams.mag_comp_params = user_mag_comp_params;	// so it will be seen by make_analyst_options()

		//  fetch_fcparams.set_analyst_mag_comp_params (
		//  	true,										// mag_comp_avail
		//  	aafs_fcparams.mag_comp_regime,				// mag_comp_regime
		//  	user_mag_comp_params						// mag_comp_params
		//  );										// so it will be seen by make_analyst_options()

		// Adjust the minimum magnitude to the value for forecasting
		// (GUI often uses a very low min mag for fetching aftershocks, we need to limit the number of aftershocks used)

		double new_min_mag = forecast_fcparams.mag_comp_params.get_magSample (fcmain.mainshock_mag);
		if (new_min_mag < forecast_fcparams.min_mag) {
			System.out.println ("WARNING: Minimum magnitude needed for forecast (" + new_min_mag + ") is less than minimum magnitude in catalog (" + forecast_fcparams.min_mag + ")");
		}
		forecast_fcparams.min_mag = new_min_mag;

		// Set next scheduled forecast time to unknown

		forecast_fcparams.next_scheduled_lag = 0L;

		// Set inset parameters

		double the_fit_start_inset = xfer.x_commonValue.x_fitStartInsetParam;
		boolean f_has_start_inset = (Math.abs(the_fit_start_inset - ForecastParameters.DEFAULT_FIT_START_INSET) > 0.000001);
		if (!( f_has_start_inset )) {
			the_fit_start_inset = ForecastParameters.DEFAULT_FIT_START_INSET;
		}
		forecast_fcparams.fit_start_inset = the_fit_start_inset;
		fetch_fcparams.fit_start_inset = the_fit_start_inset;	// so it will be seen by make_analyst_options()

		double the_fit_end_inset = xfer.x_commonValue.x_fitEndInsetParam;
		boolean f_has_end_inset = (Math.abs(the_fit_end_inset - ForecastParameters.DEFAULT_FIT_END_INSET) > 0.000001);
		if (!( f_has_end_inset )) {
			the_fit_end_inset = ForecastParameters.DEFAULT_FIT_END_INSET;
		}
		forecast_fcparams.fit_end_inset = the_fit_end_inset;
		fetch_fcparams.fit_end_inset = the_fit_end_inset;	// so it will be seen by make_analyst_options()

		// If we are using a custom search region or inset parameters, record that in the analyst parameters

		if (custom_search_region != null || f_has_start_inset || f_has_end_inset) {
			forecast_analyst_opts.analyst_params.set_analyst_aftershock_search_params (
				true,										// the_aftershock_search_avail
				custom_search_region,						// the_aftershock_search_region
				ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_days
				ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_days
				ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_depth
				ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_depth
				ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_mag
				f_has_start_inset ? the_fit_start_inset : ForecastParameters.SEARCH_PARAM_OMIT,	// the_fit_start_inset
				f_has_end_inset ? the_fit_end_inset : ForecastParameters.SEARCH_PARAM_OMIT		// the_fit_end_inset
			);

			forecast_fcparams.aftershock_search_fetch_meth = ForecastParameters.FETCH_METH_ANALYST;
		} else {
			forecast_fcparams.aftershock_search_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
		}

		// If ETAS is enabled ...

		if (config_is_etas_enabled || gui_top.get_force_etas_params()) {

			// Set ETAS parameters in forecast parameters

			forecast_fcparams.set_analyst_etas_params (
				true,					// the_etas_avail
				"",						// the_etas_regime
				new OEtasParameters()	// the_etas_params
			);

			// Set ETAS parameters in analyst options

			forecast_analyst_opts.analyst_params.set_analyst_etas_params (
				true,					// the_etas_avail
				"",						// the_etas_regime
				new OEtasParameters()	// the_etas_params
			);

			fetch_fcparams.etas_params = forecast_analyst_opts.analyst_params.etas_params;	// so it will be seen by make_analyst_options()

			// Make the ETAS parameters

			boolean[] etas_active_custom = new boolean[1];

			forecast_fcparams.etas_regime = make_fit_etas_params (
				xfer,
				forecast_fcparams.etas_params,
				forecast_analyst_opts.analyst_params.etas_params,
				etas_active_custom
			);

			f_etas_active_custom = etas_active_custom[0];
		}

		// ETAS not enabled, make sure there are no ETAS parameters

		else {

			// No ETAS parameters in forecast parameters

			forecast_fcparams.etas_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
			forecast_fcparams.etas_avail = false;
			forecast_fcparams.set_default_etas_params();

			// No ETAS parameters in analyst options

			forecast_analyst_opts.analyst_params.etas_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
			forecast_analyst_opts.analyst_params.etas_avail = false;
			forecast_analyst_opts.analyst_params.set_default_etas_params();

			fetch_fcparams.etas_params = null;	// so it will be seen by make_analyst_options()

			f_etas_active_custom = false;
		}

		// Minimum magnitude bins

		double[] custom_min_mag_bins = null;

		switch (xfer.x_commonValue.x_minMagBinsOptionParam) {

		case AUTO:
			custom_min_mag_bins = null;
			break;

		case RANGE_30_70:
			custom_min_mag_bins = new double[]{3.0, 4.0, 5.0, 6.0, 7.0};
			break;

		case RANGE_30_80:
			custom_min_mag_bins = new double[]{3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
			break;

		default:
			throw new IllegalArgumentException ("OEGUIModel.fitParams: Invalid minimum magnitude bin option");
		}

		forecast_analyst_opts.analyst_params.set_analyst_fcopt_params (
			custom_min_mag_bins						// the_fcopt_min_mag_bins
		);

		forecast_fcparams.fetch_fcopt_params (
			gui_model.get_fcmain(),					// fcmain
			forecast_analyst_opts.analyst_params	// prior_params
		);

		fetch_fcparams.fcopt_min_mag_bins = custom_min_mag_bins;	// so it will be seen by make_analyst_options()

		// Run the forecast
		// Note: The aftershocks are filtered by time and magnitude in ForecastResults

		ActionConfig action_config = new ActionConfig();
		long the_forecast_lag = SimpleUtils.days_to_millis (forecast_fcparams.max_days);
		long the_result_time = fcmain.mainshock_time + the_forecast_lag;
		long the_advisory_lag = ForecastResults.forecast_lag_to_advisory_lag (the_forecast_lag, action_config);
		String the_injectable_text = null;
		boolean f_seq_spec = true;

		forecast_fcparams.forecast_lag = the_forecast_lag;

		forecast_fcresults.calc_all_from_known_as (
			the_result_time,
			the_advisory_lag,
			the_injectable_text,
			fcmain,
			forecast_fcparams,
			f_seq_spec,
			cur_aftershocks
		);

		forecast_fcresults.pick_pdl_model();

		// Put into data structure

		long creation_time = System.currentTimeMillis();

		forecast_fcdata.set_data (
			creation_time,
			fcmain,
			forecast_fcparams,
			forecast_fcresults,
			forecast_analyst_opts
		);

		// Save the catalog parameters (we should already have these values)
		// TODO: Delete?

		//cat_dataStartTimeParam = xfer.x_dataSource.x_dataStartTimeParam;
		//cat_dataEndTimeParam = xfer.x_dataSource.x_dataEndTimeParam;
		cat_mcParam = xfer.x_commonValue.x_mcParam;
		cat_bParam = xfer.x_commonValue.x_bParam;

		return;
	}




	// Make ETAS parameters to use in a forecast.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	//  forecast_etas_params = Receives ETAS parameters to use in a forecast.
	//  analyst_etas_params = Receives ETAS parameters to use in analyst options.
	//  etas_active_custom = 1-element array, returns a flag indicating if ETAS active model has a custom weight.
	// Returns the regime for the mainshock location.
	// Note: Caller must check that ETAS is enabled in the action configuration, or
	// that use of ETAS parameters is forced, before calling.

	private String make_fit_etas_params (
		OEGUIController.XferFittingMod xfer,
		OEtasParameters forecast_etas_params,
		OEtasParameters analyst_etas_params,
		boolean[] etas_active_custom
	) {

		// Start with empty analyst parameters

		analyst_etas_params.clear();

		etas_active_custom[0] = false;

		// Forecast parameters start as default parameters for the mainshock location

		String regime_name = "";
		OAFRegimeParams<OEtasParameters> loc_regime_params;
		if (fcmain.mainshock_avail) {
			loc_regime_params = (new OEtasConfig()).get_resolved_params (fcmain.get_eqk_location(), null);
		} else {
			loc_regime_params = new OAFRegimeParams<OEtasParameters> (null, null);
		}
		if (loc_regime_params.has_regime()) {
			regime_name = loc_regime_params.regime.toString();
		}
		if (loc_regime_params.has_params()) {
			forecast_etas_params.copy_from (loc_regime_params.params);
		} else {
			forecast_etas_params.set_to_typical_cv();
		}

		// Switch on ETAS eligibility option code

		switch (xfer.x_etasValue.x_etasEnableParam) {

		case DISABLE:

			// If disable, just set disable eligibility and return

			analyst_etas_params.set_eligible_params_to_analyst (
				true,
				OEConstants.ELIGIBLE_OPT_DISABLE
			);
			forecast_etas_params.merge_from (analyst_etas_params);
			return regime_name;

		case AUTO:

			// If auto, don't need eligibility in analyst parameters

			break;

		case ENABLE_FIT_ONLY:
		case ENABLE:

			// If enable or enable fit only, set eligibility to enable, copying magnitude ranges from forecast params
				
			analyst_etas_params.set_eligible_params_to_analyst (
				true,
				OEConstants.ELIGIBLE_OPT_ENABLE,
				forecast_etas_params.eligible_main_mag,
				forecast_etas_params.eligible_cat_max_mag,
				forecast_etas_params.eligible_small_mag,
				forecast_etas_params.eligible_above_mag_cat
			);
			break;

		default:
			throw new IllegalArgumentException ("OEGUIModel.make_fit_etas_params: Invalid ETAS enable option");
		}

		// Use the minimum number of catalogs if enable fit only, or if forced

		if (xfer.x_etasValue.x_etasEnableParam == EtasEnableOption.ENABLE_FIT_ONLY || gui_top.get_force_min_etas_catalogs()) {
			analyst_etas_params.set_num_catalogs_to_minimum();
		}

		// ETAS parameter ranges

		OEDiscreteRange b_range = make_single_range (
			xfer.x_etasValue.x_useCommonBParam ? xfer.x_commonValue.x_bParam : xfer.x_etasValue.x_bETASParam
		);
		
		OEDiscreteRange alpha_range = null;
		if (!( xfer.x_etasValue.x_alphaEqualsBParam )) {
			alpha_range = make_single_range (xfer.x_etasValue.x_alphaParam);
		}

		OEDiscreteRange c_range = make_log_range (
			xfer.x_etasValue.x_cETASValRangeParam,
			xfer.x_etasValue.x_cETASValNumParam
		);

		OEDiscreteRange p_range = make_linear_range (
			xfer.x_etasValue.x_pETASValRangeParam,
			xfer.x_etasValue.x_pETASValNumParam
		);

		OEDiscreteRange n_range = make_log_skew_range (
			xfer.x_etasValue.x_nETASValRangeParam,
			xfer.x_etasValue.x_nETASValNumParam,
			xfer.x_etasValue.x_nETASValSkewParam
		);

		OEDiscreteRange zams_range = make_linear_range (
			xfer.x_etasValue.x_zamsETASValRangeParam,
			xfer.x_etasValue.x_zamsETASValNumParam
		);

		OEDiscreteRange zmu_range = make_log_range (
			xfer.x_etasValue.x_zmuETASValRangeParam,
			xfer.x_etasValue.x_zmuETASValNumParam
		);

		boolean relative_zams = xfer.x_etasValue.x_zamsETASValRelativeParam;

		analyst_etas_params.set_range_to_analyst (
			true,
			b_range,
			alpha_range,
			c_range,
			p_range,
			n_range,
			zams_range,
			zmu_range,
			relative_zams
		);

		// Bayesian prior

		switch (xfer.x_etasValue.x_etasPriorParam) {

		case MIXED:
			analyst_etas_params.set_bay_prior_to_typical_mixed_rnpc();
			break;

		case GAUSSIAN:
			analyst_etas_params.set_bay_prior_to_typical_gauss_apc();
			break;

		case UNIFORM:
			analyst_etas_params.set_bay_prior_to_typical_uniform();
			break;

		default:
			throw new IllegalArgumentException ("OEGUIModel.make_fit_etas_params: Invalid Bayesian prior option");
		}

		// Bayesian weight

		switch (xfer.x_etasValue.x_etasModelParam) {

		case AUTO:
			break;

		case BAYESIAN:
			analyst_etas_params.set_bay_weight_to_analyst (
				true,							// bay_weight_avail
				OEConstants.BAY_WT_BAYESIAN,	// bay_weight
				OEConstants.BAY_WT_BAYESIAN,	// early_bay_weight
				OEConstants.DEF_EARLY_BAY_TIME	// early_bay_time
			);
			break;

		case SEQSPEC:
			analyst_etas_params.set_bay_weight_to_analyst (
				true,							// bay_weight_avail
				OEConstants.BAY_WT_SEQ_SPEC,	// bay_weight
				OEConstants.BAY_WT_SEQ_SPEC,	// early_bay_weight
				OEConstants.DEF_EARLY_BAY_TIME	// early_bay_time
			);
			break;

		case GENERIC:
			analyst_etas_params.set_bay_weight_to_analyst (
				true,							// bay_weight_avail
				OEConstants.BAY_WT_GENERIC,		// bay_weight
				OEConstants.BAY_WT_GENERIC,		// early_bay_weight
				OEConstants.DEF_EARLY_BAY_TIME	// early_bay_time
			);
			break;

		case CUSTOM:
			analyst_etas_params.set_bay_weight_to_analyst (
				true,									// bay_weight_avail
				xfer.x_etasValue.x_etasBayWeightParam,	// bay_weight
				xfer.x_etasValue.x_etasBayWeightParam,	// early_bay_weight
				OEConstants.DEF_EARLY_BAY_TIME			// early_bay_time
			);
			etas_active_custom[0] = true;
			break;

		default:
			throw new IllegalArgumentException ("OEGUIModel.make_fit_etas_params: Invalid ETAS model option");
		}

		// Marge analyst parameters into forecast parameters

		forecast_etas_params.merge_from (analyst_etas_params);
		return regime_name;
	}




	// Make a single-element range.

	public static OEDiscreteRange make_single_range (double x) {
		return OEDiscreteRange.makeSingle (x);
	}


	// Make a linear range, or single if it only contains a single element.

	public static OEDiscreteRange make_linear_range (Range range, int num) {
		if (num == 1) {
			return OEDiscreteRange.makeSingle (range.getLowerBound());
		}
		return OEDiscreteRange.makeLinear (num, range.getLowerBound(), range.getUpperBound());
	}


	// Make a log range, or single if it only contains a single element.
	// (Implies that a single-element range can have zero limits.)

	public static OEDiscreteRange make_log_range (Range range, int num) {
		if (num == 1) {
			return OEDiscreteRange.makeSingle (range.getLowerBound());
		}
		return OEDiscreteRange.makeLog (num, range.getLowerBound(), range.getUpperBound());
	}


	// Make a skewed log range, or single if it only contains a single element.
	// (If skew is 1.0 then make a log range.)

	public static OEDiscreteRange make_log_skew_range (Range range, int num, double skew) {
		if (num == 1) {
			return OEDiscreteRange.makeSingle (range.getLowerBound());
		}
		if (Math.abs (skew - 1.0) <= 1.0e-8) {
			return OEDiscreteRange.makeLog (num, range.getLowerBound(), range.getUpperBound());
		}
		return OEDiscreteRange.makeLogSkew (num, range.getLowerBound(), range.getUpperBound(), skew);
	}




	//----- Functions for computing forecasts -----




	// Compute forecasts.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate forecast computation.

	public void computeForecasts (GUICalcProgressBar progress, OEGUIController.XferForecastMod xfer) {

		// Save the catalog parameters

		//cat_forecastStartTimeParam = xfer.x_fcValue.x_forecastStartTimeParam;
		//cat_forecastEndTimeParam = xfer.x_fcValue.x_forecastEndTimeParam;

		// Remove the model-specific message
		
		if (progress != null) {
			//progress.setIndeterminate(true);
			//progress.setProgressMessage("Plotting...");
			progress.setIndeterminate(true, "Plotting...");
		}

		// Pre-compute elements for expected aftershock MFDs.

		//gui_view.precomputeEAMFD (progress, cat_forecastStartTimeParam, cat_forecastEndTimeParam);

		return;
	}




//	// Make the forecast for a model.
//	// Returns null if the model is null.
//
//	private USGS_AftershockForecast makeForecast (GUICalcProgressBar progress, double minDays, RJ_AftershockModel the_model, String name) {
//
//		// Check for null model
//
//		if (the_model == null) {
//			return null;
//		}
//
//		// Start time of forecast is from forecastStartTimeParam
//		
//		Instant eventDate = Instant.ofEpochMilli(get_cur_mainshock().getOriginTime());
//		double startTime = eventDate.toEpochMilli() + minDays*ComcatOAFAccessor.day_millis;
//		Instant startDate = Instant.ofEpochMilli((long)startTime);
//
//		// Begin timer
//
//		Stopwatch watch = Stopwatch.createStarted();
//
//		// Announce we are processing the model
//			
//		if (progress != null) {
//			progress.setIndeterminate(true, "Calculating " + name + "...");
//		}
//
//		// Calculate the forecast for the model
//
//		USGS_AftershockForecast forecast;
//		if (gui_top.get_include_m4()) {
//			double[] min_mags = new double[5];
//			min_mags[0] = 3.0;
//			min_mags[1] = 4.0;
//			min_mags[2] = 5.0;
//			min_mags[3] = 6.0;
//			min_mags[4] = 7.0;
//			forecast = new USGS_AftershockForecast(the_model, gui_model.get_cur_aftershocks(), min_mags, eventDate, startDate);
//		} else {
//			forecast = new USGS_AftershockForecast(the_model, gui_model.get_cur_aftershocks(), eventDate, startDate);
//		}
//
//		// Display timing
//			
//		System.out.println("Took " + watch.elapsed(TimeUnit.SECONDS) + "s to compute aftershock table for " + name);
//		watch.stop();
//
//		// Return the forecast
//
//		return forecast;
//	}




	//----- Functions for analyst operations -----




	// Make a set of adjustable parameters for analyst options.
	// Note: This function can be called at state >= MODSTATE_MAINSHOCK, hence must only
	//  use data available at that state.

	public AdjustableParameters make_analyst_adj_params (OEGUISubAnalyst.XferAnalystView xfer) {

		// Check we are in an acceptable state

		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.make_analyst_adj_params while in state " + cur_modstate_string());
		}

		// Event-sequence parameters

		EventSequenceParameters evseq_cfg_params = null;
		switch (xfer.x_evSeqOptionParam) {
		case IGNORE:
			evseq_cfg_params = EventSequenceParameters.make_coerce (
				ActionConfigFile.ESREP_NO_REPORT, SimpleUtils.days_to_millis (xfer.x_evSeqLookbackParam));
			break;
		case UPDATE:
			evseq_cfg_params = EventSequenceParameters.make_coerce (
				ActionConfigFile.ESREP_REPORT, SimpleUtils.days_to_millis (xfer.x_evSeqLookbackParam));
			break;
		case DELETE:
			evseq_cfg_params = EventSequenceParameters.make_coerce (
				ActionConfigFile.ESREP_DELETE, SimpleUtils.days_to_millis (xfer.x_evSeqLookbackParam));
			break;
		}

		// Maximum forecasst lag

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

		// Intake option

		int intake_option;
		int shadow_option;

		switch (xfer.x_autoEnableParam) {

		case DISABLE:
			intake_option = AnalystOptions.OPT_INTAKE_BLOCK;
			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
			break;

		case ENABLE:
			intake_option = AnalystOptions.OPT_INTAKE_IGNORE;
			shadow_option = AnalystOptions.OPT_SHADOW_IGNORE;
			break;

		default:
			intake_option = AnalystOptions.OPT_INTAKE_NORMAL;
			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
			break;
		}

		// Set up the adjustable parameters

		AdjustableParameters adj_params = new AdjustableParameters();

		adj_params.set_all_analyst_opts (
			analyst_adj_params.injectable_text,
			analyst_adj_params.analyst_id,
			analyst_adj_params.analyst_remark,
			evseq_cfg_params,
			max_forecast_lag,
			intake_option,
			shadow_option
		);

		return adj_params;
	}




	// Make the analyst options, for sending to the server.
	// If xfer.x_useCustomParamsParam is true, can be called when model state >= MODSTATE_PARAMETERS.
	// If xfer.x_useCustomParamsParam is false, can be called when model state >= MODSTATE_MAINSHOCK,
	//  hence only must use data available in that state.

	public AnalystOptions make_analyst_options (OEGUISubAnalyst.XferAnalystView xfer) {

		// Check we are in an acceptable state

		if (!( modstate >= MODSTATE_MAINSHOCK )) {
			throw new IllegalStateException ("Access to OEGUIModel.make_analyst_options while in state " + cur_modstate_string());
		}

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

		// Get the adjustable parameters

		AdjustableParameters adj_params = make_analyst_adj_params (xfer);

		// Time at which to issue an extra forecast
		
		long extra_forecast_lag = current_lag;

		// This would not issue an extra forecast if we are disabling,
		// but we need the extra forecast to remove any existing forecast promptly.

		//  if (adj_params.intake_option == AnalystOptions.OPT_INTAKE_BLOCK) {
		//  	extra_forecast_lag = -1L;
		//  }

		// Time the analyst parameters are set

		long analyst_time = time_now;

		// Analyst Parameters

		ForecastParameters analyst_params = new ForecastParameters();
		analyst_params.setup_all_default();

		// Always supply event-sequence parameters, even if we are blocking
		// (Necessary to control whether event-sequence products should be deleted)

		analyst_params.set_or_fetch_evseq_cfg_params (adj_params.evseq_cfg_params, true);

		// If not disabling ...

		if (adj_params.intake_option != AnalystOptions.OPT_INTAKE_BLOCK) {

			// Supply injectable text

			analyst_params.set_eff_injectable_text (adj_params.injectable_text, null);

			// If we want custom forecasting parameters ...

			if (xfer.x_useCustomParamsParam) {

				// Check we are in an acceptable state

				if (!( modstate >= MODSTATE_PARAMETERS )) {
					throw new IllegalStateException ("Access to OEGUIModel.make_analyst_options requesting custom parameters while in state " + cur_modstate_string());
				}

				// Magnitude of completeness parameters

				analyst_params.set_analyst_mag_comp_params (
					true,										// mag_comp_avail
					aafs_fcparams.mag_comp_regime,				// mag_comp_regime
					fetch_fcparams.mag_comp_params				// mag_comp_params
				);

				// RJ sequence specific parameters

				analyst_params.set_analyst_seq_spec_params (
					true,										// seq_spec_avail
					fetch_fcparams.seq_spec_params				// seq_spec_params
				);

				// Inset parameters

				double the_fit_start_inset = fetch_fcparams.fit_start_inset;
				boolean f_has_start_inset = (Math.abs(the_fit_start_inset - ForecastParameters.DEFAULT_FIT_START_INSET) > 0.000001);

				double the_fit_end_inset = fetch_fcparams.fit_end_inset;
				boolean f_has_end_inset = (Math.abs(the_fit_end_inset - ForecastParameters.DEFAULT_FIT_END_INSET) > 0.000001);

				// If we used a custom search region or inset parameters, set it

				if (custom_search_region != null || f_has_start_inset || f_has_end_inset) {
					analyst_params.set_analyst_aftershock_search_params (
						true,										// the_aftershock_search_avail
						custom_search_region,						// the_aftershock_search_region
						ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_days
						ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_days
						ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_depth
						ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_depth
						ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_mag
						f_has_start_inset ? the_fit_start_inset : ForecastParameters.SEARCH_PARAM_OMIT,	// the_fit_start_inset
						f_has_end_inset ? the_fit_end_inset : ForecastParameters.SEARCH_PARAM_OMIT		// the_fit_end_inset
					);
				}

				// Set ETAS parameters

				if (fetch_fcparams.etas_params != null) {
					analyst_params.set_analyst_etas_params (
						true,						// the_etas_avail
						"",							// the_etas_regime
						fetch_fcparams.etas_params	// the_etas_params
					);
				} else {
					analyst_params.etas_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
					analyst_params.etas_avail = false;
					analyst_params.set_default_etas_params();
				}

				// Minimum magnitude bins

				analyst_params.set_analyst_fcopt_params (
					fetch_fcparams.fcopt_min_mag_bins	// the_fcopt_min_mag_bins
				);
			}
		}

		// Create the analyst options

		AnalystOptions anopt = new AnalystOptions();

		anopt.setup (
			adj_params.analyst_id,
			adj_params.analyst_remark,
			analyst_time,
			analyst_params,
			extra_forecast_lag,
			adj_params.max_forecast_lag,
			adj_params.intake_option,
			adj_params.shadow_option
		);

		return anopt;
	}




//	// Make the new forecast parameters for analyst options.
//	// See AnalystCLI.make_new_fcparams.
//
//	public ForecastParameters make_analyst_fcparams (OEGUISubAnalyst.XferAnalystView xfer) {
//
//		ForecastParameters new_fcparams = new ForecastParameters();
//		new_fcparams.setup_all_default();
//
//		if (xfer.x_autoEnableParam == AutoEnable.DISABLE) {
//			return new_fcparams;
//		}
//
//		new_fcparams.set_analyst_control_params (
//			ForecastParameters.CALC_METH_AUTO_PDL,		// generic_calc_meth
//			ForecastParameters.CALC_METH_AUTO_PDL,		// seq_spec_calc_meth
//			ForecastParameters.CALC_METH_AUTO_PDL,		// bayesian_calc_meth
//			analyst_adj_params.injectable_text			// injectable_text
//		);
//
//		if (!( xfer.x_useCustomParamsParam )) {
//			return new_fcparams;
//		}
//
//		new_fcparams.set_analyst_mag_comp_params (
//			true,										// mag_comp_avail
//			aafs_fcparams.mag_comp_regime,				// mag_comp_regime
//			fetch_fcparams.mag_comp_params				// mag_comp_params
//		);
//
//		new_fcparams.set_analyst_seq_spec_params (
//			true,										// seq_spec_avail
//			fetch_fcparams.seq_spec_params				// seq_spec_params
//		);
//
//		if (custom_search_region == null) {
//			return new_fcparams;
//		}
//
//		new_fcparams.set_analyst_aftershock_search_params (
//			true,										// the_aftershock_search_avail
//			custom_search_region,						// the_aftershock_search_region
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_days
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_days
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_depth
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_max_depth
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_min_mag
//			ForecastParameters.SEARCH_PARAM_OMIT,		// the_fit_start_inset
//			ForecastParameters.SEARCH_PARAM_OMIT		// the_fit_end_inset
//		);
//		
//		return new_fcparams;
//	}




//	// Make the analyst options.
//	// See AnalystCLI.make_new_analyst_options.
//
//	public AnalystOptions make_analyst_options (OEGUISubAnalyst.XferAnalystView xfer) {
//
//		// Action configuration
//
//		ActionConfig action_config = new ActionConfig();
//
//		// The first forecast lag, note is is a multiple of 1 second
//
//		long first_forecast_lag = action_config.get_next_forecast_lag (0L);
//		if (first_forecast_lag < 0L) {
//			first_forecast_lag = 0L;
//		}
//
//		// Time now
//
//		long time_now = ServerClock.get_time();
//
//		// The forecast lag that would cause a forecast to be issued now,
//		// as a multiple of 1 seconds, but after the first forecast
//
//		long current_lag = action_config.floor_unit_lag (
//				time_now - (fcmain.mainshock_time
//							+ action_config.get_comcat_clock_skew()
//							+ action_config.get_comcat_origin_skew()),
//				first_forecast_lag + 1000L);
//
//		// Parameters that vary based on forecast selection
//		
//		long extra_forecast_lag;
//		int intake_option;
//		int shadow_option;
//
//		switch (xfer.x_autoEnableParam) {
//
//		case DISABLE:
//			//extra_forecast_lag = -1L;
//			extra_forecast_lag = current_lag;	// needed so any existing forecast is removed promptly
//			intake_option = AnalystOptions.OPT_INTAKE_BLOCK;
//			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
//			break;
//
//		case ENABLE:
//			extra_forecast_lag = current_lag;
//			intake_option = AnalystOptions.OPT_INTAKE_IGNORE;
//			shadow_option = AnalystOptions.OPT_SHADOW_IGNORE;
//			break;
//
//		default:
//			extra_forecast_lag = current_lag;
//			intake_option = AnalystOptions.OPT_INTAKE_NORMAL;
//			shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;
//			break;
//		}
//
//		// Other parameters
//
//		String analyst_id = "";
//		String analyst_remark = "";
//		long analyst_time = time_now;
//		ForecastParameters analyst_params = make_analyst_fcparams (xfer);
//
//		long max_forecast_lag = Math.round (xfer.x_forecastDuration * SimpleUtils.DAY_MILLIS_D);
//		if (max_forecast_lag >= def_fc_duration - 1000L && max_forecast_lag <= def_fc_duration + 1000L) {
//			max_forecast_lag = 0L;
//		}
//		else if (max_forecast_lag >= max_fc_duration - 1000L) {
//			max_forecast_lag = max_fc_duration;
//		}
//		else if (max_forecast_lag <= min_fc_duration + 1000L) {
//			max_forecast_lag = min_fc_duration;
//		}
//
//		// Create the analyst options
//
//		AnalystOptions anopt = new AnalystOptions();
//
//		anopt.setup (
//			analyst_id,
//			analyst_remark,
//			analyst_time,
//			analyst_params,
//			extra_forecast_lag,
//			max_forecast_lag,
//			intake_option,
//			shadow_option
//		);
//
//		return anopt;
//	}




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




	// Export analyst options.
	// If xfer.x_useCustomParamsParam is true, can be called when model state >= MODSTATE_PARAMETERS.
	// If xfer.x_useCustomParamsParam is false, can be called when model state >= MODSTATE_MAINSHOCK.
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




	// Read analyst options from a file.
	// An exception is thrown if the file could not be written.

	public AnalystOptions readAnalystOptions (File the_file) throws IOException {

		// Make the analyst options

		AnalystOptions anopt = null;

		// Read the file

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (the_file));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			anopt = AnalystOptions.unmarshal_poly (reader, null);
			reader.check_read_complete ();
		}

		return anopt;
	}




	// Send analyst options to server.
	// If xfer.x_useCustomParamsParam is true, can be called when model state >= MODSTATE_PARAMETERS.
	// If xfer.x_useCustomParamsParam is false, can be called when model state >= MODSTATE_MAINSHOCK.
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




	// Send analyst options to server.
	// Can be called when model state >= MODSTATE_MAINSHOCK.
	// Returns true if success, false if unable to send to any server.
	// An exception is thrown if the operation could not be performed.

	public boolean sendAnalystOptions (GUICalcProgressBar progress, AnalystOptions anopt) {

		// Send to server

		String event_id = fcmain.mainshock_event_id;
		boolean f_disable = (anopt.intake_option == AnalystOptions.OPT_INTAKE_BLOCK);

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




	// Information for one section of the info panel.

	public static class InfoItem {

		// The tag for this item.
		// Tags are assigned sequentially in the order that items are created.

		public int info_tag;

		// The model state in which this item can be active.

		public int info_modstate;

		// The title for this item.
		// This must be non-null and non-empty, a single line not ending in a newline.

		public String info_title;

		// The text for this item.
		// This can consist of multiple lines.
		// Each line must end in a newline.
		// This is null if the item is not active.

		public String info_text;

		// Construct an item.

		public InfoItem (int info_tag, int info_modstate, String info_title) {
			this.info_tag = info_tag;
			this.info_modstate = info_modstate;
			this.info_title = info_title;
			this.info_text = null;
		}
	}




	// Comparator that sorts info items into display order.
	// The order is by decreasing model state, and then increasing tag.

	public static class InfoItemComparator implements Comparator<InfoItem> {
		@Override
		public final int compare (InfoItem x, InfoItem y) {
			int result = Integer.compare (y.info_modstate, x.info_modstate);
			if (result == 0) {
				result = Integer.compare (x.info_tag, y.info_tag);
			}
			return result;
		}
	}




	// The list of information items.
	// Note: Access to this list is synchronized so that items can be updated from compute threads.

	private List<InfoItem> info_item_list = null;


	// True if any info item has changed since the last time the list was retrieved.

	private boolean f_info_dirty = true;


	// Object used for locking access to information items.

	private final Object info_lock_object = new Object();


	// Tags for information items.

	private int info_tag_version = -1;				// Program version
	private int info_tag_processor = -1;			// Processor information
	private int info_tag_memory = -1;				// Memory information
	private int info_tag_mainshock = -1;			// Mainshock information
	private int info_tag_region = -1;				// Aftershock search region information
	private int info_tag_rj_mle_fit = -1;			// RJ MLE and variance parameter it
	private int info_tag_etas_mle_fit = -1;			// ETAS MLE parameter fit



	// Create and register an info item.
	// Returns the assigned tag.
	// Note: Registration order determines the order in which items will be displayed
	// within a model state.

	private final int register_info_item (int info_modstate, String info_title) {
		int info_tag;
		synchronized (info_lock_object) {
			info_tag = info_item_list.size();
			info_item_list.add (new InfoItem (info_tag, info_modstate, info_title));
		}
		return info_tag;
	}


	// Register all info items.

	private final void register_all_info_items () {
		info_item_list = new ArrayList<InfoItem>();

		info_tag_etas_mle_fit = register_info_item (MODSTATE_PARAMETERS, "Fitted ETAS MLE Parameters");
		info_tag_rj_mle_fit = register_info_item (MODSTATE_PARAMETERS, "Fitted RJ Parameters");

		info_tag_region = register_info_item (MODSTATE_CATALOG, "Aftershock search region");

		info_tag_mainshock = register_info_item (MODSTATE_MAINSHOCK, "Mainshock Information");

		info_tag_memory = register_info_item (MODSTATE_INITIAL, "Memory Usage");
		info_tag_processor = register_info_item (MODSTATE_INITIAL, "Processor");
		info_tag_version = register_info_item (MODSTATE_INITIAL, "Version");

		update_version_info();
		update_processor_info();

		return;
	}




	// Get the text to display in the information window.
	// If f_force is false, return null if the dirty flag is not set.
	// Note: Must run on EDT so it can access modstate.

	public final String get_info_window_text (boolean f_force) throws GUIEDTException {
		StringBuilder sb = null;
		synchronized (info_lock_object) {

			if (f_force || f_info_dirty) {

				// Update memory information if we are getting text

				update_memory_info();

				// Scan the list to get all items active in the current model state

				List<InfoItem> active_list = new ArrayList<InfoItem>();

				for (InfoItem item : info_item_list) {
					if (item.info_modstate <= modstate && item.info_text != null) {
						active_list.add (item);
					}
				}

				active_list.sort (new InfoItemComparator());

				// Now write the active items

				sb = new StringBuilder();
				int item_count = 0;

				for (InfoItem item : active_list) {

					if (item_count > 0) {
						sb.append ("\n");
					}
					sb.append ("*** ");
					sb.append (item.info_title);
					sb.append (" ***\n\n");
					sb.append (item.info_text);

					++item_count;
				}

				// Clear the dirty flag

				f_info_dirty = false;
			}
		}

		if (sb == null) {
			return null;
		}
		return sb.toString();
	}




	// Mainshock info for the window title, can be null or empty if none.

	private String title_mainshock_info = null;

	// Program state for the window title, can be null or empty if none.

	private String title_program_state = null;

	// Update window title.
	// The model state can change up one state at a time, or down by any amount.

	private void info_window_title (int the_modstate) throws GUIEDTException {

		// If pre-mainshock, no info

		if (the_modstate < MODSTATE_MAINSHOCK) {
			title_program_state = null;
			title_mainshock_info = null;
		}

		// Otherwise, we have at least a mainshock ...

		else {

			// Program state

			switch (the_modstate) {

			default:
				title_program_state = null;
				break;

			// State for the mainshock

			case MODSTATE_MAINSHOCK:
				title_program_state = "Mainshock loaded";
				title_mainshock_info = make_title_mainshock_info (get_fcmain());
				break;

			// State for fetching the catalog

			case MODSTATE_CATALOG:
				title_program_state = "Catalog loaded";
				break;

			// State for determining aftershock parameters

			case MODSTATE_PARAMETERS:
				title_program_state = "Parameters fitted";
				break;

			// State for computing a forecast

			case MODSTATE_FORECAST:
				title_program_state = "Forecast computed";
				break;
			}
		}

		// Update the window title

		gui_top.set_window_title (title_mainshock_info, title_program_state);
		return;
	}




	// Make the title string for the mainshock.

	private static final TimeZone tz_utc = TimeZone.getTimeZone("UTC");

	private static final SimpleDateFormat time_to_string_title_fmt = new SimpleDateFormat ("yyyy-MM-dd");
	static {
		time_to_string_title_fmt.setTimeZone (tz_utc);
	}

	private String make_title_mainshock_info (ForecastMainshock the_fcmain) {
		String mainshock_info = null;
		if (the_fcmain != null && the_fcmain.mainshock_avail) {

			String event_id = null;
			if (!( the_fcmain.mainshock_event_id == null || the_fcmain.mainshock_event_id.isEmpty() || EventIDGenerator.is_generated_id(the_fcmain.mainshock_event_id) )) {
				event_id = the_fcmain.mainshock_event_id;
			}

			String place = null;
			if (!( the_fcmain.mainshock_geojson == null )) {
				place = GeoJsonUtils.getPlace (the_fcmain.mainshock_geojson);
				if (place != null) {
					String s1 = place.replaceAll ("[\\x00-\\x1F\\x7F\\xA0]", " ");	// map ASCII control chars and nbsp to space
					//place = s1.replaceAll ("[^\\x00-\\xFF]", "?").trim();			// map non-ASCII, non-Latin-1 chars to question mark
					place = s1.trim();
				}
			}

			String s_time = time_to_string_title_fmt.format (new Date (the_fcmain.mainshock_time));

			String s_mag = SimpleUtils.double_to_string ("%.1f", the_fcmain.mainshock_mag);

			if (place != null) {
				mainshock_info = s_time + " - M " + s_mag + " - " + place;
			}
			else if (event_id != null) {
				mainshock_info = s_time + " - M " + s_mag + " - " + event_id;
			}
			else {
				mainshock_info = s_time + " - M " + s_mag + " Mainshock";
			}
		}
		return mainshock_info;
	}




	// Set an information item.
	// The text can be null to explicitly deactivate an item.
	// Text can consist of multiple lines, each terminated by a newline.

	public final void set_info_item (int info_tag, String info_text) {

		// Append a newline to the text if needed

		String eff_info_text = info_text;
		if (!( eff_info_text == null || eff_info_text.endsWith ("\n") )) {
			eff_info_text = eff_info_text + "\n";
		}

		String title;

		synchronized (info_lock_object) {

			// Validate the tag

			if (!( info_tag >= 0 && info_tag < info_item_list.size() )) {
				throw new RuntimeException ("OEGUIModel.set_info_item: Invalid info item tag: info_tag = " + info_tag);
			}

			// Change text in the selected item

			InfoItem item = info_item_list.get (info_tag);
			item.info_text = eff_info_text;
			title = item.info_title;

			// Set the dirty flag

			f_info_dirty = true;
		}

		if (gui_top.get_trace_events()) {
			if (info_text == null) {
				System.out.println ("@@@@@ clearing information item: " + title);
			} else {
				System.out.println ("@@@@@ setting information item: " + title);
			}
		}

		return;
	}




	// Retreat the model state for the information display.
	// Clears the text in all items not visible at the new model state.
	// Note: This should be called even if the model state is not changing.

	private void retreat_info_modstate (int new_modstate) throws GUIEDTException {
		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ retreat_info_modstate: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
		}

		synchronized (info_lock_object) {
			for (InfoItem item : info_item_list) {
				if (item.info_modstate > new_modstate) {
					if (item.info_text != null) {
						item.info_text = null;
						f_info_dirty = true;
					}
				}
			}
		}

		// Update window title

		info_window_title (new_modstate);

		return;
	}




	// Advance the model state for the information display.
	// This sets information that can be done just from knowing the state is changing.
	// Note: This should be called after modstate is changed but before the view is updated

	private void info_advance_state (int old_modstate, int new_modstate) throws GUIEDTException {

		switch (new_modstate) {

		// Information for the mainshock

		case MODSTATE_MAINSHOCK:
			update_mainshock_info (get_fcmain());
			break;

		// Information for fetching the catalog

		case MODSTATE_CATALOG:
			update_region_info();
			break;

		// Information for determining aftershock parameters

		case MODSTATE_PARAMETERS:
			update_rj_fit_info();
			update_etas_fit_info();
			break;

		// Information for computing a forecast

		case MODSTATE_FORECAST:
			break;
		}

		// Update window title

		info_window_title (new_modstate);

		return;
	}




	// Update the memory info item.

	public final void update_memory_info () {
		StringBuilder sb = new StringBuilder();

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

		set_info_item (info_tag_memory, sb.toString());
		return;
	}




	// Update the processor info item.
	// Note: This only needs to be called once.

	public final void update_processor_info () {
		StringBuilder sb = new StringBuilder();

		sb.append ("num_threads = " + AutoExecutorService.get_default_num_threads() + "\n");

		set_info_item (info_tag_processor, sb.toString());
		return;
	}




	// Update the version info item.
	// Note: This only needs to be called once.

	public final void update_version_info () {
		StringBuilder sb = new StringBuilder();

		sb.append (VersionInfo.get_title() + "\n");

		set_info_item (info_tag_version, sb.toString());
		return;
	}




	// Update the mainshock info.

	public final void update_mainshock_info (ForecastMainshock the_fcmain) {
		if (the_fcmain != null && the_fcmain.mainshock_avail) {
			StringBuilder sb = new StringBuilder();

			if (!( the_fcmain.mainshock_event_id == null || the_fcmain.mainshock_event_id.isEmpty() || EventIDGenerator.is_generated_id(the_fcmain.mainshock_event_id) || the_fcmain.mainshock_geojson == null )) {
				String title = GeoJsonUtils.getTitle (the_fcmain.mainshock_geojson);
				if (!( title == null || title.isEmpty() )) {
					sb.append (title + "\n");
				}
			}
			if (!( the_fcmain.mainshock_event_id == null || the_fcmain.mainshock_event_id.isEmpty() || EventIDGenerator.is_generated_id(the_fcmain.mainshock_event_id) )) {
				sb.append ("event_id = " + the_fcmain.mainshock_event_id + "\n");
			}
			if (!( the_fcmain.mainshock_network == null || the_fcmain.mainshock_network.isEmpty() )) {
				sb.append ("network = " + the_fcmain.mainshock_network + "\n");
			}
			if (!( the_fcmain.mainshock_code == null || the_fcmain.mainshock_code.isEmpty() )) {
				sb.append ("code = " + the_fcmain.mainshock_code + "\n");
			}
			if (!( the_fcmain.mainshock_event_id == null || the_fcmain.mainshock_event_id.isEmpty() || EventIDGenerator.is_generated_id(the_fcmain.mainshock_event_id) || the_fcmain.mainshock_id_list == null || the_fcmain.mainshock_id_list.length == 0 )) {
				sb.append ("id_list = " + Arrays.toString (the_fcmain.mainshock_id_list) + "\n");
			}
			sb.append ("time = " + SimpleUtils.time_raw_and_string(the_fcmain.mainshock_time) + "\n");
			sb.append ("mag = " + the_fcmain.mainshock_mag + "\n");
			sb.append ("lat = " + the_fcmain.mainshock_lat + "\n");
			sb.append ("lon = " + the_fcmain.mainshock_lon + "\n");
			sb.append ("depth = " + the_fcmain.mainshock_depth + "\n");

			set_info_item (info_tag_mainshock, sb.toString());
		}
		else {
			set_info_item (info_tag_mainshock, null);
		}
		return;

	}




	// Update the aftershock search region info.

	public final void update_region_info () {
		if (resolved_search_region != null || resolved_region_spec != null) {
			StringBuilder sb = new StringBuilder();

			if (resolved_search_region != null) {
				sb.append ("region = " + SphRegion.marshal_to_line_poly (resolved_search_region) + "\n");
				if (resolved_region_spec != null) {
					sb.append ("\n");
				}
			}

			if (resolved_region_spec != null) {
				sb.append (resolved_region_spec.toString());
			}

			set_info_item (info_tag_region, sb.toString());
		}
		else {
			set_info_item (info_tag_region, null);
		}
		return;

	}




	// Format a number that appears in the RJ fit info.
	// Based on its magnitude, either convert to fixed-point with 6 decimal places
	// and trailing zero removal with right-padding, or floating-pint with 3 decimal places.

	private static String rj_fit_num_format (double x) {

		// First convert and round to floating point with 3 decimal places
		
		String result = SimpleUtils.double_to_string ("%.3e", x);

		// Convert back to obtain rounded value, and then its absolute value

		double rx = Double.parseDouble (result);
		double ax = Math.abs (rx);

		// Very small values format as exactly zero

		if (ax <= 1.0e-99) {
			result = SimpleUtils.double_to_string_trailz ("%.6f", SimpleUtils.TRAILZ_PAD_RIGHT, 0.0);
		}

		// Values within fixed-point range format as fixed point

		else if (ax <= 99.99 && ax >= 0.001) {
			result = SimpleUtils.double_to_string_trailz ("%.6f", SimpleUtils.TRAILZ_PAD_RIGHT, rx);
		}

		return result;
	}




	// Update the RJ fit info.

	public final void update_rj_fit_info () {

		// Get the models

		RJ_AftershockModel_SequenceSpecific seq_model = get_cur_model();
		RJ_AftershockModel_Generic gen_model = get_genericModel();
		RJ_AftershockModel_Bayesian bay_model = get_bayesianModel();

		// If all models are null, nothing

		if (seq_model == null && gen_model == null && bay_model == null) {
			set_info_item (info_tag_rj_mle_fit, null);
			return;
		}

		// Make a table

		AsciiTable table = new AsciiTable();

		table.add_row();
		table.add_cell_left ("");
		if (gen_model != null) {
			table.add_cell_center (" Generic");
		}
		if (seq_model != null) {
			table.add_cell_center (" SeqSpec");
		}
		if (bay_model != null) {
			table.add_cell_center ("Bayesian");
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("a");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getMaxLikelihood_a()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getMaxLikelihood_a()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getMaxLikelihood_a()
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("p");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getMaxLikelihood_p()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getMaxLikelihood_p()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getMaxLikelihood_p()
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("c");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getMaxLikelihood_c()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getMaxLikelihood_c()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getMaxLikelihood_c()
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("sigma_a");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getStdDev_a()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getStdDev_a()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getStdDev_a()
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("sigma_p");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getStdDev_p()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getStdDev_p()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getStdDev_p()
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("sigma_c");
		if (gen_model != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_model.getStdDev_c()
			));
		}
		if (seq_model != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_model.getStdDev_c()
			));
		}
		if (bay_model != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_model.getStdDev_c()
			));
		}

		table.add_row_mixed ("-");

		table.set_column_seps (" ", " |", " | ");

		set_info_item (info_tag_rj_mle_fit, table.toString());
		return;
	}




	// Update the ETAS fit info.

	public final void update_etas_fit_info () {

		// Get the MLE for each model

		OEGridPoint gen_mle_grid_point = get_etas_gen_mle_grid_point();
		OEGridPoint seq_mle_grid_point = get_etas_seq_mle_grid_point();
		OEGridPoint bay_mle_grid_point = get_etas_bay_mle_grid_point();

		OEGridPoint act_mle_grid_point = null;
		if (get_f_etas_active_custom()) {
			act_mle_grid_point = get_etas_mle_grid_point();
		}

		// If all models are null, nothing

		if (gen_mle_grid_point == null && seq_mle_grid_point == null && bay_mle_grid_point == null && act_mle_grid_point == null) {
			set_info_item (info_tag_etas_mle_fit, null);
			return;
		}

		// Make a table

		AsciiTable table = new AsciiTable();

		table.add_row();
		table.add_cell_left ("");
		if (gen_mle_grid_point != null) {
			table.add_cell_center (" Generic");
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_center (" SeqSpec");
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_center ("Bayesian");
		}
		if (act_mle_grid_point != null) {
			table.add_cell_center (" Custom ");
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("n");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.n
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.n
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.n
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.n
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("p");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.p
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.p
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.p
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.p
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("c");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.c
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.c
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.c
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.c
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("zams");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.zams
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.zams
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.zams
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.zams
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("zmu");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.zmu
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.zmu
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.zmu
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.zmu
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("b");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.b
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.b
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.b
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.b
			));
		}

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("alpha");
		if (gen_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				gen_mle_grid_point.alpha
			));
		}
		if (seq_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				seq_mle_grid_point.alpha
			));
		}
		if (bay_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				bay_mle_grid_point.alpha
			));
		}
		if (act_mle_grid_point != null) {
			table.add_cell_right (rj_fit_num_format (
				act_mle_grid_point.alpha
			));
		}

		table.add_row_mixed ("-");

		table.set_column_seps (" ", " |", " | ");

		set_info_item (info_tag_etas_mle_fit, table.toString());
		return;
	}




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
