package org.opensha.oaf.rj.oldgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.geom.Point2D;
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

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

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

import org.opensha.commons.data.xyz.EvenlyDiscrXYZ_DataSet;
import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
//import org.opensha.commons.geo.Region;
//import org.opensha.commons.gui.ConsoleWindow;
import org.opensha.commons.gui.plot.GraphWidget;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotElement;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.gui.plot.PlotSpec;
import org.opensha.commons.gui.plot.PlotSymbol;
import org.opensha.commons.gui.plot.jfreechart.xyzPlot.XYZGraphPanel;
import org.opensha.commons.gui.plot.jfreechart.xyzPlot.XYZPlotSpec;
import org.opensha.commons.mapping.gmt.elements.GMT_CPT_Files;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.editor.impl.GriddedParameterListEditor;
import org.opensha.commons.param.editor.impl.ParameterListEditor;
import org.opensha.commons.param.event.ParameterChangeEvent;
import org.opensha.commons.param.event.ParameterChangeListener;
import org.opensha.commons.param.impl.BooleanParameter;
import org.opensha.commons.param.impl.ButtonParameter;
import org.opensha.commons.param.impl.DoubleParameter;
import org.opensha.commons.param.impl.EnumParameter;
import org.opensha.commons.param.impl.IntegerParameter;
import org.opensha.commons.param.impl.LocationParameter;
import org.opensha.commons.param.impl.ParameterListParameter;
import org.opensha.commons.param.impl.RangeParameter;
import org.opensha.commons.param.impl.StringParameter;
import org.opensha.commons.util.ClassUtils;
import org.opensha.commons.util.DataUtils.MinMaxAveTracker;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.commons.util.cpt.CPT;
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
import org.opensha.oaf.util.GUIConsoleWindow;
import org.opensha.oaf.util.GUICalcStep;
import org.opensha.oaf.util.GUICalcRunnable;
import org.opensha.oaf.util.GUICalcProgressBar;
import org.opensha.oaf.util.GUIEDTException;
import org.opensha.oaf.util.GUIEDTRunnable;
import org.opensha.oaf.util.GUIEventAlias;
import org.opensha.oaf.util.GUIExternalCatalog;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Reasenberg & Jones GUI - Model implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the model.
// It holds the earthquake information, catalog, and forecasts.


public class RJOldGUIModel extends RJOldGUIComponent {


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
			throw new IllegalArgumentException ("RJOldGUIModel.advance_modstate - Invalid new model state: " + get_modstate_as_string(new_modstate));
		}

		// Check for valid transition

		if (!( new_modstate >= modstate && new_modstate <= modstate + 1 )) {
			throw new IllegalStateException ("RJOldGUIModel.advance_modstate - Invalid model state transition: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
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
			throw new IllegalArgumentException ("RJOldGUIModel.retreat_modstate - Invalid new model state: " + get_modstate_as_string(new_modstate));
		}

		// Check for valid transition

		if (!( new_modstate <= modstate )) {
			throw new IllegalStateException ("RJOldGUIModel.retreat_modstate - Invalid model state transition: " + get_modstate_as_string(modstate) + " -> " + get_modstate_as_string(new_modstate));
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


	// Comcat accessor.
	
	private ComcatOAFAccessor accessor;

	// The search region, or null if none.
	// It is null if a catalog is loaded from a file.
	// Available when model state >= MODSTATE_CATALOG.
	
	private SphRegion cur_region;

	public final SphRegion get_cur_region () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cur_region while in state " + cur_modstate_string());
		}
		return cur_region;
	}

	// The mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	private ObsEqkRupture cur_mainshock;

	public final ObsEqkRupture get_cur_mainshock () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cur_mainshock while in state " + cur_modstate_string());
		}
		return cur_mainshock;
	}

	// The list of aftershocks.
	// Available when model state >= MODSTATE_CATALOG.
	// This list is sorted by time, earliest first.

	private ObsEqkRupList cur_aftershocks;

	public final ObsEqkRupList get_cur_aftershocks () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cur_aftershocks while in state " + cur_modstate_string());
		}
		return cur_aftershocks;
	}

	// The sequence-specific model.
	// Available when model state >= MODSTATE_CATALOG.
	// It is non-null when state >= MODSTATE_PARAMETERS.
	
	private RJ_AftershockModel_SequenceSpecific cur_model = null;

	public final RJ_AftershockModel_SequenceSpecific get_cur_model () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cur_model while in state " + cur_modstate_string());
		}
		return cur_model;
	}

	// The generic parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.
	
	private GenericRJ_ParametersFetch genericFetch = null;
	private GenericRJ_Parameters genericParams = null;

	public final GenericRJ_Parameters get_genericParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.genericParams while in state " + cur_modstate_string());
		}
		return genericParams;
	}

	// The generic model for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.

	private RJ_AftershockModel_Generic genericModel = null;

	public final RJ_AftershockModel_Generic get_genericModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.genericModel while in state " + cur_modstate_string());
		}
		return genericModel;
	}

	// The magnitude-of-completeness parameters for the mainshock.
	// Available when model state >= MODSTATE_CATALOG.
	
	private MagCompPage_ParametersFetch magCompFetch = null;
	private MagCompPage_Parameters magCompParams = null;

	public final MagCompPage_Parameters get_magCompParams () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.magCompParams while in state " + cur_modstate_string());
		}
		return magCompParams;
	}

	// The Bayesian model.
	// Available when model state >= MODSTATE_CATALOG.
	// It may (or may not) be non-null when state >= MODSTATE_PARAMETERS.
	
	private RJ_AftershockModel_Bayesian bayesianModel = null;

	public final RJ_AftershockModel_Bayesian get_bayesianModel () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.bayesianModel while in state " + cur_modstate_string());
		}
		return bayesianModel;
	}

	// Magnitude-number distribution of the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private IncrementalMagFreqDist aftershockMND = null;

	public final IncrementalMagFreqDist get_aftershockMND () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.aftershockMND while in state " + cur_modstate_string());
		}
		return aftershockMND;
	}

	// Magnitude where the magnitude-number distribtion peaks.
	// Available when model state >= MODSTATE_CATALOG.

	private double mnd_mmaxc;

	public final double get_mnd_mmaxc () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.mnd_mmaxc while in state " + cur_modstate_string());
		}
		return mnd_mmaxc;
	}

	// The generic forecast.
	// Available when model state >= MODSTATE_FORECAST.

	private USGS_AftershockForecast genericForecast = null;

	public final USGS_AftershockForecast get_genericForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.genericForecast while in state " + cur_modstate_string());
		}
		return genericForecast;
	}

	// The sequence-specific forecast.
	// Available when model state >= MODSTATE_FORECAST.

	private USGS_AftershockForecast seqSpecForecast = null;

	public final USGS_AftershockForecast get_seqSpecForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.seqSpecForecast while in state " + cur_modstate_string());
		}
		return seqSpecForecast;
	}

	// The bayesian forecast.
	// Available when model state >= MODSTATE_FORECAST.
	// Will be null if there is no bayesian model.

	private USGS_AftershockForecast bayesianForecast = null;

	public final USGS_AftershockForecast get_bayesianForecast () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.bayesianForecast while in state " + cur_modstate_string());
		}
		return bayesianForecast;
	}




	//----- Controller parameters for the model -----


	// Event ID, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private String cat_eventIDParam;

	public final String get_cat_eventIDParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_eventIDParam while in state " + cur_modstate_string());
		}
		return cat_eventIDParam;
	}

	// Data start time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_dataStartTimeParam;

	public final double get_cat_dataStartTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_dataStartTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataStartTimeParam;
	}

	// Data end time, in days since the mainshock, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_dataEndTimeParam;

	public final double get_cat_dataEndTimeParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_dataEndTimeParam while in state " + cur_modstate_string());
		}
		return cat_dataEndTimeParam;
	}

	// Mc for sequence, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_mcParam;

	public final double get_cat_mcParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_mcParam while in state " + cur_modstate_string());
		}
		return cat_mcParam;
	}

	// b-value, for the catalog.
	// Available when model state >= MODSTATE_CATALOG.

	private double cat_bParam;

	public final double get_cat_bParam () {
		if (!( modstate >= MODSTATE_CATALOG )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_bParam while in state " + cur_modstate_string());
		}
		return cat_bParam;
	}

	// Forecast start time, in days since the mainshock.
	// Available when model state >= MODSTATE_FORECAST.

	private double cat_forecastStartTimeParam;

	public final double get_cat_forecastStartTimeParam () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_forecastStartTimeParam while in state " + cur_modstate_string());
		}
		return cat_forecastStartTimeParam;
	}

	// Forecast start time, in days since the mainshock.
	// Available when model state >= MODSTATE_FORECAST.

	private double cat_forecastEndTimeParam;

	public final double get_cat_forecastEndTimeParam () {
		if (!( modstate >= MODSTATE_FORECAST )) {
			throw new IllegalStateException ("Access to RJOldGUIModel.cat_forecastEndTimeParam while in state " + cur_modstate_string());
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
			cur_region = null;
			cur_mainshock = null;
			cur_aftershocks = null;
			genericParams = null;
			genericModel = null;
			magCompParams = null;
			aftershockMND = null;
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

	public RJOldGUIModel () {
	}


	// Perform initialization after all components are linked.
	// The call order is: model, view, controller.

	public void post_link_init () throws GUIEDTException {

		// Initial state

		modstate = MODSTATE_INITIAL;

		// Allocate the accessor for all operations

		accessor = new ComcatOAFAccessor();

		// No data structures yet

		cur_region = null;
		cur_mainshock = null;
		cur_aftershocks = null;
		cur_model = null;
		genericFetch = null;
		genericParams = null;
		genericModel = null;
		magCompFetch = null;
		magCompParams = null;
		bayesianModel = null;
		aftershockMND = null;
		mnd_mmaxc = 0.0;

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
	



	// Set the mainshock.
	// Also fetch the generic and magnitude-of-completeness parameters,
	// and make the generic model.

	private void setMainshock(ObsEqkRupture the_mainshock) {

		// Save the mainshock

		cur_mainshock = the_mainshock;

		genericParams = null;
		magCompParams = null;

		try {

			// Lazy-allocate the fetch objects

			if (genericFetch == null) {
				genericFetch = new GenericRJ_ParametersFetch();
			}
			if (magCompFetch == null) {
				magCompFetch = new MagCompPage_ParametersFetch();
			}

			// Get generic parameters for the mainshock
			
			System.out.println("Determining tectonic regime for generic parameters");
			OAFTectonicRegime regime = genericFetch.getRegion(cur_mainshock.getHypocenterLocation());
			Preconditions.checkNotNull(regime, "Regime not found or server error");
			genericParams = genericFetch.get(regime);
			Preconditions.checkNotNull(genericParams, "Generic params not found or server error");
			System.out.println("Generic params for "+regime+": "+genericParams);
			
			// Get magnitude-of-completeness for the mainshock

			System.out.println("Determining tectonic regime for magnitude-of-completeness parameters");
			OAFTectonicRegime mc_regime = magCompFetch.getRegion(cur_mainshock.getHypocenterLocation());
			Preconditions.checkNotNull(mc_regime, "Regime not found or server error");
			magCompParams = magCompFetch.get(mc_regime);
			Preconditions.checkNotNull(magCompParams, "Magnitude-of-completeness params not found or server error");
			System.out.println("Magnitude-of-completeness params for "+mc_regime+": "+magCompParams);

			// Make the generic RJ model

			genericModel = new RJ_AftershockModel_Generic(cur_mainshock.getMag(), genericParams);

		} catch (RuntimeException e) {
			System.err.println("Error fetching generic or magnitude-of-completeness params");
			throw new IllegalStateException ("RJOldGUIModel.setMainshock - Error fetching generic or magnitude-of-completeness params", e);
			//e.printStackTrace();
			//genericParams = null;
		}

		// As a courtesy, spit out the decimal days remaining in the origin day

		System.out.println("The mainshock occurred " + String.format("%.4f", getTimeRemainingInUTCDay()) + " days before midnight (UTC)\n");
		return;
	}
	



	// Find the centroid of the aftershock sequence.

	private Location getCentroid () {
		return AftershockStatsCalc.getSphCentroid(cur_mainshock, cur_aftershocks);
	}




	// Make the search region.
	// Parameters:
	//  xfer = Transfer object to read control parameters.
	//  event = Event used to determine the epicenter location; in practice, the mainshock.
	//  centroid = Center to use if the region is circular and the center type is CENTROID.
	//  wc_radius = Radius to use if the region type is CIRCULAR_WC94.
	// Returns the region.
	// Note: The 2-step centroid algorithm can be done by calling this function twice,
	// with centroid equal to the hypocenter on the first call.
	// Note: The event longitude is adjusted, if necessary, so it lies inside the plotting
	// domain of the search region (either -180 to 180 if it does not straddle the date line,
	// or 0 to 360 if it does).  Adjustments are +/- 360 degrees.
	
	private SphRegion buildRegion (RJOldGUIController.XferCatalogView xfer, ObsEqkRupture event, Location centroid, double wc_radius) {
		SphRegion result;

		RegionType type = xfer.x_regionTypeParam;

		// If the region is a circle ...
		
		if (type == RegionType.CIRCULAR || type == RegionType.CIRCULAR_WC94) {

			// Select either fixed radius or WC radius

			double radius;
			if (type == RegionType.CIRCULAR) {
				radius = xfer.x_radiusParam;
			} else {
				radius = wc_radius;
				System.out.println("Using Wells & Coppersmith 94 Radius: " + String.format("%.3f", radius) + " km");
			}

			// Select circle center
			
			RegionCenterType centerType = xfer.x_regionCenterTypeParam;
			Location loc;
			if (centerType == RegionCenterType.EPICENTER)
				loc = event.getHypocenterLocation();
			else if (centerType == RegionCenterType.CENTROID)
				loc = centroid;
			else if (centerType == RegionCenterType.SPECIFIED)
				loc = xfer.x_regionCenterLocParam;
			else
				throw new IllegalStateException("Unknown Region Center Type: "+centerType);
			
			// Return new Region(loc, radius);

			result = SphRegion.makeCircle (new SphLatLon(loc), radius);

		// Otherwise, if the region is a rectangle ...

		} else  if (type == RegionType.RECTANGULAR) {
			Location lower = new Location(xfer.x_minLatParam, xfer.x_minLonParam);
			Location upper = new Location(xfer.x_maxLatParam, xfer.x_maxLonParam);

			// Return new Region(lower, upper);

			result = SphRegion.makeMercRectangle (new SphLatLon(lower), new SphLatLon(upper));

		// Otherwise, unknown shape ...

		} else {
			throw new IllegalStateException("Unknown region type: "+type);
		}

		// If the event (i.e. cur_mainshock) is outside the plotting domain, change its
		// hypocenter so it is inside the plotting domain

		Location hypo = event.getHypocenterLocation();
		if (result.getPlotWrap()) {
			if (hypo.getLongitude() < 0.0) {
				event.setHypocenterLocation (new Location (
					hypo.getLatitude(), hypo.getLongitude() + 360.0, hypo.getDepth() ));
			}
		} else {
			if (hypo.getLongitude() > 180.0) {
				event.setHypocenterLocation (new Location (
					hypo.getLatitude(), hypo.getLongitude() - 360.0, hypo.getDepth() ));
			}
		}

		return result;
	}




	// Perform actions after fetching or loading the catalog.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.

	private void postFetchActions (RJOldGUIController.XferCatalogMod xfer) {

		// Sort the aftershocks by time

		cur_aftershocks.sortByOriginTime();

		// Compute the magnitude-number distribution of aftershocks (in bins)

		aftershockMND = ObsEqkRupListCalc.getMagNumDist(cur_aftershocks, 1.05, 81, 0.1);

		// Find the magnitude bin with the maximum number of aftershocks

		mnd_mmaxc = AftershockStatsCalc.getMmaxC(aftershockMND);

		// Set the default magnitude of completeness to the peak of the mag-num distribution, plus 0.5 magnitude

		xfer.modify_mcParam(mnd_mmaxc + 0.5);

		// Set the default b-value from the generic parameters

		xfer.modify_bParam(genericParams.get_bValue());

		// Save the catalog parameters

		cat_eventIDParam = xfer.x_eventIDParam;
		cat_dataStartTimeParam = xfer.x_dataStartTimeParam;
		cat_dataEndTimeParam = xfer.x_dataEndTimeParam;
		cat_mcParam = xfer.x_mcParam;	// won't be null because it was set just above
		cat_bParam = xfer.x_bParam;		// won't be null because it was set just above

		return;
	}




	// Fetch events from Comcat.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate catalog fetch.

	public void fetchEvents (RJOldGUIController.XferCatalogMod xfer) {

		System.out.println("Fetching Events");
		cur_mainshock = null;

		// See if the event ID is an alias, and change it if so

		String xlatid = GUIEventAlias.query_alias_dict (xfer.x_eventIDParam);
		if (xlatid != null) {
			System.out.println("Translating Event ID: " + xfer.x_eventIDParam + " -> " + xlatid);
			xfer.modify_eventIDParam(xlatid);
		}

		// Fetch mainshock
		
		String eventID = xfer.x_eventIDParam;
		ObsEqkRupture my_mainshock = accessor.fetchEvent(eventID, false, true);	// need extended info for sending to PDL
		Preconditions.checkState(my_mainshock != null, "Event not found: %s", eventID);

		// Display mainshock location and information

		System.out.println("Mainshock Location: "+my_mainshock.getHypocenterLocation());
		System.out.println (ComcatOAFAccessor.rupToString (my_mainshock));

		// Display list of OAF products for this mainshock

		List<ComcatOAFProduct> oaf_product_list = ComcatOAFProduct.make_list_from_gj (accessor.get_last_geojson());
		for (int k = 0; k < oaf_product_list.size(); ++k) {
			System.out.println ("OAF product: " + oaf_product_list.get(k).summary_string());
		}
		if (oaf_product_list.size() > 0) {
			System.out.println ();
		}

		// Establish this as our mainshock
		
		setMainshock(my_mainshock);

		// Depth range for aftershock search
		
		double minDepth = xfer.x_minDepthParam;
		double maxDepth = xfer.x_maxDepthParam;

		// Time range for aftershock search
		
		double minDays = xfer.x_dataStartTimeParam;
		double maxDays = xfer.x_dataEndTimeParam;
		
		long eventTime = my_mainshock.getOriginTime();
		long startTime = eventTime + (long)(minDays*ComcatOAFAccessor.day_millis);
		long endTime = eventTime + (long)(maxDays*ComcatOAFAccessor.day_millis);

		// Check that start date is before current time

		long time_now = System.currentTimeMillis();
		
		Preconditions.checkState(startTime < time_now, "Start time is after now!");

		// Check that end date is before current time, shrink the time range if not
		
		if (endTime > time_now) {
			double calcMaxDays = (time_now - startTime)/ComcatOAFAccessor.day_millis;
			System.out.println("WARNING: End time after current time. Setting max days to: " + calcMaxDays);
			xfer.modify_dataEndTimeParam(calcMaxDays);
			maxDays = xfer.x_dataEndTimeParam;
		}

		// If we are doing the 2-step centroid algorithm ...
		
		if (xfer.x_regionTypeParam.isCircular()
			&& xfer.x_regionCenterTypeParam == RegionCenterType.CENTROID) {

			// First make circle around hypocenter to find the centroid, and find aftershocks within the circle

			cur_region = buildRegion(xfer, my_mainshock, my_mainshock.getHypocenterLocation(), magCompParams.get_radiusCentroid(my_mainshock.getMag()));
			
			cur_aftershocks = accessor.fetchAftershocks(my_mainshock, minDays, maxDays, minDepth, maxDepth, cur_region, cur_region.getPlotWrap());
			
			// Now make circle around centroid, and find aftershocks within the circle

			cur_region = buildRegion(xfer, my_mainshock, getCentroid(), magCompParams.get_radiusSample(my_mainshock.getMag()));
				
			cur_aftershocks = accessor.fetchAftershocks(my_mainshock, minDays, maxDays, minDepth, maxDepth, cur_region, cur_region.getPlotWrap());
		
		// Otherwise, make the region and find aftershocks within the region ...

		} else {
			cur_region = buildRegion(xfer, my_mainshock, null, magCompParams.get_radiusSample(my_mainshock.getMag()));
			
			cur_aftershocks = accessor.fetchAftershocks(my_mainshock, minDays, maxDays, minDepth, maxDepth, cur_region, cur_region.getPlotWrap());
		}

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}
	



	// Load catalog from a file.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	//  catalogFile = File to read from.
	// This is called by the controller to initiate catalog load.

	public void loadCatalog(RJOldGUIController.XferCatalogMod xfer, File catalogFile) {

		// Catalog time range

		double minDays = xfer.x_dataStartTimeParam;
		double maxDays = xfer.x_dataEndTimeParam;

		// List of aftershocks, and mainshock

		ObsEqkRupList myAftershocks = new ObsEqkRupList();
		ObsEqkRupture myMainshock = null;

		//  // Read the entire file
		//  
		//  List<String> lines;
		//  try {
		//  	lines = Files.readLines(catalogFile, Charset.defaultCharset());
		//  } catch (IOException e) {
		//  	throw new RuntimeException ("RJOldGUIModel.loadCatalog - I/O error reading file: " + catalogFile.getPath(), e);
		//  }
		//  
		//  // Process lines to get a list of aftershocks and a mainshock
		//  
		//  for (int i=0; i<lines.size(); i++) {
		//  	String line = lines.get(i).trim();
		//  	if (line.startsWith("#")) {
		//  		if (line.toLowerCase().startsWith("# main")
		//  				&& i < lines.size()-1 && lines.get(i+1).startsWith("#")) {
		//  			// main shock on next line, starting with a #
		//  			String mainshockLine = lines.get(i+1).substring(1).trim();
		//  			System.out.println("Detected mainshock in file: "+mainshockLine);
		//  			try {
		//  				myMainshock = GUIExternalCatalog.fromCatalogLine(mainshockLine);
		//  			} catch (Exception e) {
		//  				System.err.println("Error loading mainshock");
		//  			}
		//  		}
		//  		continue;
		//  	}
		//  	if (!line.isEmpty()) {
		//  		myAftershocks.add(GUIExternalCatalog.fromCatalogLine(line));
		//  	}
		//  }

		// Read the file

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();
		ext_cat.read_from_file (catalogFile, myAftershocks);
		myMainshock = ext_cat.mainshock;

		// Display search result
		
		System.out.println("Loaded "+myAftershocks.size()+" aftershocks from file");

		// Check to make sure we got a mainshock
		
		Preconditions.checkState(myMainshock != null, "Did not find mainshock in file");

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

		xfer.modify_eventIDParam("<custom>");

		// Establish this as our mainshock
		
		setMainshock(myMainshock);

		// Take the trimmed list of aftershocks as our aftershocks

		cur_aftershocks = trimmedAftershocks;

		// Indicate no region for plotting

		cur_region = null;

		// Perform post-fetch actions

		postFetchActions (xfer);

		return;
	}




	//----- Functions for fitting aftershock parameters -----




	// Fit aftershock parameters, and construct the R&J models.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate aftershock parameter fitting.

	public void fitParams (RJOldGUIController.XferFittingMod xfer) {

		// Get sequence-specific parameters

		Range aRange = xfer.x_aValRangeParam;
		int aNum = xfer.x_aValNumParam;
		//validateRange(aRange, aNum, "a-value");
		Range pRange = xfer.x_pValRangeParam;
		int pNum = xfer.x_pValNumParam;
		//validateRange(pRange, pNum, "p-value");
		Range cRange = xfer.x_cValRangeParam;
		int cNum = xfer.x_cValNumParam;
		//validateRange(cRange, cNum, "c-value");
					
		double mc = xfer.x_mcParam;
									
		double b = xfer.x_bParam;

		// If doing time-dependent magnitude of completeness
					
		if (xfer.x_timeDepMcParam) {
			double f = xfer.x_fParam;
						
			double g = xfer.x_gParam;
						
			double h = xfer.x_hParam;
						
			double mCat = xfer.x_mCatParam;

			MagCompFn magCompFn = MagCompFn.makePageOrConstant (f, g, h);
						
			cur_model = new RJ_AftershockModel_SequenceSpecific(get_cur_mainshock(), get_cur_aftershocks(), mCat, magCompFn, b,
					xfer.x_dataStartTimeParam, xfer.x_dataEndTimeParam,
					aRange.getLowerBound(), aRange.getUpperBound(), aNum,
					pRange.getLowerBound(), pRange.getUpperBound(), pNum,
					cRange.getLowerBound(), cRange.getUpperBound(), cNum);

		// Otherwise, time-independent magnitude of completeness

		} else {
			cur_model = new RJ_AftershockModel_SequenceSpecific(get_cur_mainshock(), get_cur_aftershocks(), mc, b,
					xfer.x_dataStartTimeParam, xfer.x_dataEndTimeParam,
					aRange.getLowerBound(), aRange.getUpperBound(), aNum,
					pRange.getLowerBound(), pRange.getUpperBound(), pNum,
					cRange.getLowerBound(), cRange.getUpperBound(), cNum);
		}

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

		cat_dataStartTimeParam = xfer.x_dataStartTimeParam;
		cat_dataEndTimeParam = xfer.x_dataEndTimeParam;
		cat_mcParam = xfer.x_mcParam;
		cat_bParam = xfer.x_bParam;

		return;
	}




	//----- Functions for computing forecasts -----




	// Compute forecasts.
	// Parameters:
	//  xfer = Transfer object to read/modify control parameters.
	// This is called by the controller to initiate forecast computation.

	public void computeForecasts (GUICalcProgressBar progress, RJOldGUIController.XferForecastMod xfer) {

		// Save the catalog parameters

		cat_forecastStartTimeParam = xfer.x_forecastStartTimeParam;
		cat_forecastEndTimeParam = xfer.x_forecastEndTimeParam;

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




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
