package org.opensha.oaf.oetas.gui;

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
//import org.opensha.commons.param.impl.ParameterListParameter;
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
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.gui.GUIParameterListParameter;
import org.opensha.oaf.util.gui.GUIButtonParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opensha.oaf.util.LineConsumerFile;
import org.opensha.oaf.util.LineSupplierFile;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Controller implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the operational ETAS model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the controller.
// It holds the controls that appear on the left side of the main window.


public class OEGUIController extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_DATA_SOURCE = 101;		// Data source parameters
	private static final int PARMGRP_FETCH_BUTTON = 102;	// Button to fetch data
	private static final int PARMGRP_SAVE_CATALOG = 103;	// Button to save catalog
	private static final int PARMGRP_PARAMS_BUTTON = 104;	// Button to compute aftershock parameters
	private static final int PARMGRP_FORECAST_BUTTON = 105;	// Button to compute forecast
	private static final int PARMGRP_SERVER_STATUS = 106;	// Button to fetch server status


	//----- Sub-controllers -----


	// Floating panel for data source.

	private OEGUISubDataSource sub_ctl_data_source;

	// Floating panel for Reasenberg & Jones parameters.

	private OEGUISubRJValue sub_ctl_rj_param;

	// Floating panel for common parameters.

	private OEGUISubCommonValue sub_ctl_common_param;

	// Floating panel for ETAS parameters.

	private OEGUISubETASValue sub_ctl_etas_param;

	// Floating panel for forecast parameters.

	private OEGUISubForecast sub_ctl_forecast_param;

	// Floating panel for analyst options.

	private OEGUISubAnalyst sub_ctl_analyst_option;




	//----- Controls for the Data Parameters column -----


	// Fetch Data button.

	private GUIButtonParameter fetchButton;

	private GUIButtonParameter init_fetchButton () throws GUIEDTException {
		fetchButton = new GUIButtonParameter("Catalog", "Fetch Data");
		fetchButton.setButtonForeground(gui_top.get_button_highlight());
		fetchButton.setInfo("Fetch catalog from the selected data source");
		register_param (fetchButton, "fetchButton", PARMGRP_FETCH_BUTTON);
		return fetchButton;
	}




	//----- Controls for the Aftershock Parameters column -----


	private GUIButtonParameter computeAftershockParamsButton;

	private GUIButtonParameter init_computeAftershockParamsButton () throws GUIEDTException {
		computeAftershockParamsButton = new GUIButtonParameter("Aftershock Params", "Fit Parameters");
		computeAftershockParamsButton.setButtonForeground(gui_top.get_button_highlight());
		//computeAftershockParamsButton.setButtonBackground(new Color(255,170,170));
		register_param (computeAftershockParamsButton, "computeAftershockParamsButton", PARMGRP_PARAMS_BUTTON);
		return computeAftershockParamsButton;
	}




	//----- Controls for the Forecast column -----


	// Compute Aftershock Forecast button.

	private GUIButtonParameter computeAftershockForecastButton;

	private GUIButtonParameter init_computeAftershockForecastButton () throws GUIEDTException {
		computeAftershockForecastButton = new GUIButtonParameter("Aftershock Forecast", "Compute");
		computeAftershockForecastButton.setButtonForeground(gui_top.get_button_highlight());
		register_param (computeAftershockForecastButton, "computeAftershockForecastButton", PARMGRP_FORECAST_BUTTON);
		return computeAftershockForecastButton;
	}




	//----- Controls for the More column -----


	// Save Catalog button.

	private ButtonParameter saveCatalogButton;

	private ButtonParameter init_saveCatalogButton () throws GUIEDTException {
		saveCatalogButton = new ButtonParameter("Aftershock Catalog", "Save Catalog...");
		saveCatalogButton.setInfo("Save catalog to file in 10 column format");
		register_param (saveCatalogButton, "saveCatalogButton", PARMGRP_SAVE_CATALOG);
		return saveCatalogButton;
	}


	// Fetch Server Status button.

	private ButtonParameter fetchServerStatusButton;

	private ButtonParameter init_fetchServerStatusButton () throws GUIEDTException {
		fetchServerStatusButton = new ButtonParameter("AAFS Server", "Fetch Server Status");
		register_param (fetchServerStatusButton, "fetchServerStatusButton", PARMGRP_SERVER_STATUS);
		return fetchServerStatusButton;
	}




	//----- Control containers -----


	// Container for data parameters.

	private ParameterListEditor dataEditor;

	public final ParameterListEditor get_dataEditor () {
		return dataEditor;
	}

	private int dataEditorHeight;

	private ParameterListEditor init_dataEditor () throws GUIEDTException {

		// Controls in the "Data Source" column

		ParameterList dataParams = new ParameterList();

		sub_ctl_data_source = new OEGUISubDataSource (this);
		dataParams.addParameter(sub_ctl_data_source.get_eventIDParam());
		dataParams.addParameter(sub_ctl_data_source.get_dataSourceTypeParam());
		dataParams.addParameter(sub_ctl_data_source.get_dataSourceEditParam());
		
		dataParams.addParameter(init_fetchButton());

		// Create the container

		dataEditorHeight = (gui_top.get_height() * 4) / 10;

		dataEditor = new ParameterListEditor(dataParams);
		dataEditor.setTitle("Data Source");
		dataEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), dataEditorHeight));
		return dataEditor;
	}


	// Container for aftershock parameters.

	private ParameterListEditor fitEditor;

	public final ParameterListEditor get_fitEditor () {
		return fitEditor;
	}

	private int fitEditorHeight;

	private ParameterListEditor init_fitEditor () throws GUIEDTException {

		// Controls in the "Aftershock Parameters" column
		
		ParameterList fitParams = new ParameterList();
	
		sub_ctl_rj_param = new OEGUISubRJValue (this);
		fitParams.addParameter(sub_ctl_rj_param.get_rjValueEditParam());
	
		sub_ctl_common_param = new OEGUISubCommonValue (this);
		fitParams.addParameter(sub_ctl_common_param.get_commonValueEditParam());
	
		sub_ctl_etas_param = new OEGUISubETASValue (this);
		fitParams.addParameter(sub_ctl_etas_param.get_etasValueEditParam());
		
		fitParams.addParameter(init_computeAftershockParamsButton());

		// Create the container

		fitEditorHeight = (gui_top.get_height() * 4) / 10;

		fitEditor = new ParameterListEditor(fitParams);
		fitEditor.setTitle("Aftershock Parameters");
		fitEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), fitEditorHeight));
		return fitEditor;
	}


	// Container for forecast parameters.

	private ParameterListEditor fcastEditor;

	public final ParameterListEditor get_fcastEditor () {
		return fcastEditor;
	}

	private int fcastEditorHeight;

	private ParameterListEditor init_fcastEditor () throws GUIEDTException {

		// Controls in the "Forecasts" column
		
		ParameterList fcastParams = new ParameterList();
	
		sub_ctl_forecast_param = new OEGUISubForecast (this);
		fcastParams.addParameter(sub_ctl_forecast_param.get_fcValueEditParam());
		
		fcastParams.addParameter(init_computeAftershockForecastButton());

		// Create the container

		fcastEditorHeight = gui_top.get_height() - (dataEditorHeight + fitEditorHeight);

		fcastEditor = new ParameterListEditor(fcastParams);
		fcastEditor.setTitle("Forecasts");
		fcastEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), fcastEditorHeight));
		return fcastEditor;
	}


	// Container for more functions.

	private ParameterListEditor aafsEditor;

	public final ParameterListEditor get_aafsEditor () {
		return aafsEditor;
	}

	private int aafsEditorHeight;

	private ParameterListEditor init_aafsEditor () throws GUIEDTException {

		// Controls in the "More" column
		
		ParameterList aafsParams = new ParameterList();

		sub_ctl_analyst_option = new OEGUISubAnalyst(this);
		aafsParams.addParameter(sub_ctl_analyst_option.get_analystEditParam());
		
		aafsParams.addParameter(init_saveCatalogButton());
		aafsParams.addParameter(init_fetchServerStatusButton());

		// Create the container

		aafsEditorHeight = (gui_top.get_height() * 3) / 10;

		aafsEditor = new ParameterListEditor(aafsParams);
		aafsEditor.setTitle("More");
		aafsEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), aafsEditorHeight));
		return aafsEditor;
	}


	// Container for filler.

	private ParameterListEditor fillerEditor;

	public final ParameterListEditor get_fillerEditor () {
		return fillerEditor;
	}

	private int fillerEditorHeight;

	private ParameterListEditor init_fillerEditor () throws GUIEDTException {

		// Controls in the "Filler" column
		
		ParameterList fillerParams = new ParameterList();

		// Create the container

		fillerEditorHeight = gui_top.get_height() - aafsEditorHeight;

		fillerEditor = new ParameterListEditor(fillerParams);
		fillerEditor.setTitle("...");
		fillerEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), fillerEditorHeight));
		return fillerEditor;
	}




	//----- Debugging support -----


	// Add all parameters to the symbol table.

	private void setup_symbol_table () {

		// Set up the symbol table

		add_symbol (dataEditor , "dataEditor");
		add_symbol (fitEditor , "fitEditor");
		add_symbol (fcastEditor , "fcastEditor");
		add_symbol (aafsEditor , "aafsEditor");
		add_symbol (fillerEditor , "fillerEditor");

		return;
	}




	//----- Control enable/disable -----




	// Adjust the enable/disable state of all controls, according to the supplied parameters.
	// Parameters:
	//  f_catalog = Catalog is available.
	//  f_params = Aftershock parameters are available.
	//  f_forecast = Forecast is available.
	//  f_edit_region = Edit region option is available. TODO - Remove
	//  f_time_dep_mc = Time dependent parameters are being used. TODO - Remove
	//  f_fetched = Catalog is available, and data was fetched from Comcat.
	// Note: Controls are enabled by default, so controls that are always
	// enabled do not need to be mentioned.
	// Note: Also clears some controls, according to the supplied parameters.

	private void adjust_enable (boolean f_catalog, boolean f_params, boolean f_forecast, boolean f_edit_region, boolean f_time_dep_mc, boolean f_fetched) throws GUIEDTException {

		// Data parameters that are always enabled

		sub_ctl_data_source.sub_data_source_enable(true);

		// Data parameters that are enabled when fetch is possible
		// (Doesn't work because the next click after, e.g., typing the event ID,
		// might be the fetch button, which would not be enabled yet.)

		//boolean f_can_fetch = sub_ctl_data_source.can_fetch_data();
		//enableParam(fetchButton, f_can_fetch);
		enableParam(fetchButton, true);

		// Data parameters that become enabled when the catalog is loaded

		enableParam(saveCatalogButton, f_catalog);

		// Aftershock parameters that become enabled when the catalog is loaded

		sub_ctl_rj_param.sub_rj_value_enable (true, f_catalog);
		sub_ctl_common_param.sub_common_value_enable (true, f_catalog);
		sub_ctl_etas_param.sub_etas_value_enable (true, f_catalog);

		// Aftershock parameters that become enabled when the catalog is loaded

		enableParam(computeAftershockParamsButton, f_catalog);

		// Aftershock parameters that become enabled when parameters have been computed

		sub_ctl_forecast_param.sub_fc_value_enable (true, f_catalog && f_params);

		enableParam(computeAftershockForecastButton, f_catalog && f_params);

		// Analyst parameters

		sub_ctl_analyst_option.sub_analyst_enable (true, f_catalog && f_params && f_fetched, f_catalog && f_fetched);

		// Special functions

		//enableParam(fetchServerStatusButton, true);

		return;
	}




	// Adjust the enable/disable state of all controls, according to the current state.

	private void adjust_enable () throws GUIEDTException {
		boolean f_catalog = gui_model.modstate_has_catalog();
		boolean f_params = gui_model.modstate_has_aftershock_params();
		boolean f_forecast = gui_model.modstate_has_forecast();
		boolean f_edit_region = true;	// TODO - Remove
		boolean f_time_dep_mc = true;	// TODO - Remove
		boolean f_fetched = gui_model.modstate_has_catalog() && gui_model.get_has_fetched_catalog();
		adjust_enable (f_catalog, f_params, f_forecast, f_edit_region, f_time_dep_mc, f_fetched);
		return;
	}




	// Options for filter_state.

	public static final int FILTOPT_MIN = 101;
	public static final int FILTOPT_TEST = 101;
	public static final int FILTOPT_NORMAL = 102;
	public static final int FILTOPT_REPAINT = 103;
	public static final int FILTOPT_MAX = 103;




	// Convert a filter option to a string.

	public static String get_filtopt_as_string (int x) {
		switch (x) {
		case FILTOPT_TEST: return "FILTOPT_TEST";
		case FILTOPT_NORMAL: return "FILTOPT_NORMAL";
		case FILTOPT_REPAINT: return "FILTOPT_REPAINT";
		}
		return "FILTOPT_INVALID(" + x + ")";
	}




	// Filter event according to current state.
	// Parameters:
	//  ctrl_modstate = State in which the control is active, MODSTATE_XXXXX.
	//  filtopt = Filter options, FILTOPT_XXXXX, defaults to FILTOPT_NORMAL.
	// Returns true if the event should be discarded or ignored.
	// Action depends on option:
	//  FILTOPT_TEST - Test if the control can be active in the current
	//    state, return false if so, true if not.  Do not change anything.
	//  FILTOPT_NORMAL - Test if the control can be active in the current
	//    state, return false if so, true if not.  If the return is false,
	//    then change the model state to ctrl_modstate if it is not already
	//    ctrl_modstate.  If the model state changed, then repaint all
	//    controls reflecting their enable/disable state.
	//  FILTOPT_REPAINT - Test if the control can be active in the current
	//    state, return false if so, true if not.  If the return is false,
	//    then change the model state to ctrl_modstate if it is not already
	//    ctrl_modstate.  Then repaint all controls reflecting their
	//    enable/disable state.

	private boolean filter_state (int ctrl_modstate, int filtopt) throws GUIEDTException {

		// Check for valid state

		if (!( ctrl_modstate >= MODSTATE_MIN && ctrl_modstate <= MODSTATE_MAX )) {
			throw new IllegalArgumentException ("OEGUIController.filter_state - Invalid control state: " + get_modstate_as_string(ctrl_modstate));
		}

		// Switch based on filter option

		switch (filtopt) {

		case FILTOPT_TEST:

			// Test only

			if (gui_model.can_retreat_modstate (ctrl_modstate)) {
				return false;
			}

			break;

		case FILTOPT_NORMAL:

			// Test and change state, repainting if needed

			if (gui_model.can_retreat_modstate (ctrl_modstate)) {
				if (gui_model.retreat_modstate (ctrl_modstate)) {
					adjust_enable();
				}
				return false;
			}

			break;

		case FILTOPT_REPAINT:

			// Test and change state, always repainting

			if (gui_model.can_retreat_modstate (ctrl_modstate)) {
				gui_model.retreat_modstate (ctrl_modstate);
				adjust_enable();
				return false;
			}

			break;

		default:

			// Invalid filter option

			throw new IllegalArgumentException ("OEGUIController.filter_state - Invalid filter option: " + get_filtopt_as_string(filtopt));
		}

		// Event should be filtered

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Filter event, control state = " + get_modstate_as_string(ctrl_modstate) + ", model state = " + gui_model.cur_modstate_string());
		}

		return true;
	}


	private boolean filter_state (int ctrl_modstate) throws GUIEDTException {
		return filter_state (ctrl_modstate, FILTOPT_NORMAL);
	}




	// Advance the current state.
	// Parameters:
	//  new_modstate = State in which the control is active, MODSTATE_XXXXX.
	// If the state changes, then repaint the controls with new enable/disable state.

	private void advance_state (int new_modstate) throws GUIEDTException {

		// Check for valid state

		if (!( new_modstate >= MODSTATE_MIN && new_modstate <= MODSTATE_MAX )) {
			throw new IllegalArgumentException ("OEGUIController.advance_state - Invalid control state: " + get_modstate_as_string(new_modstate));
		}

		// Advance state, repainting if state changed

		if (gui_model.advance_modstate (new_modstate)) {
			adjust_enable();
		}

		return;
	}




	//----- Parameter handling for catalog fetch or load -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferCatalogView {

		// Data source parameters.
		// Only includes values used for the selected options (source, region type, ...).
		// Included values are checked for validity

		public OEGUISubDataSource.XferDataSourceView x_dataSource;	// Data source parameters

		// Get the implementation class.

		public abstract XferCatalogImpl xfer_get_impl ();
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferCatalogMod extends XferCatalogView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferCatalogImpl extends XferCatalogMod implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferCatalogImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction.

		public XferCatalogImpl () {
			x_dataSource = sub_ctl_data_source.make_data_source_xfer();
			internal_clean();
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			x_dataSource.xfer_get_impl().xfer_clean();
			return;
		}


		// Load values.
		// f_fetch is true to load values for a fetch operation

		@Override
		public XferCatalogImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Data source

			x_dataSource.xfer_get_impl().xfer_load();

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// Data source

			x_dataSource.xfer_get_impl().xfer_store();

			return;
		}
	}




	// Update parameters after catalog fetch or load.
	// The purpose is to initialize parameters for aftershock parameter computation.

	private void post_fetch_param_update () throws GUIEDTException {

		// Update parameter values

		sub_ctl_rj_param.update_rj_value_from_model();

		sub_ctl_common_param.update_common_value_from_model();

		sub_ctl_etas_param.update_etas_value_from_model();

		sub_ctl_forecast_param.update_fc_value_from_model();

		sub_ctl_analyst_option.update_analyst_from_model();

		return;
	}




	//----- Parameter handling for aftershock parameter fitting -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferFittingView {

		// Reasenberg-Jones parameters.
		// Included values are checked for validity

		public OEGUISubRJValue.XferRJValueView x_rjValue;	// Reasenberg-Jones parameter values

		// Common parameters.
		// Included values are checked for validity

		public OEGUISubCommonValue.XferCommonValueView x_commonValue;	// Common parameter values

		// ETAS parameters.
		// Included values are checked for validity

		public OEGUISubETASValue.XferETASValueView x_etasValue;	// ETAS parameter values

		// Get the implementation class.

		public abstract XferFittingImpl xfer_get_impl ();
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferFittingMod extends XferFittingView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferFittingImpl extends XferFittingMod implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferFittingImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferFittingImpl () {
			x_rjValue = sub_ctl_rj_param.make_rj_value_xfer();
			x_commonValue = sub_ctl_common_param.make_common_value_xfer();
			x_etasValue = sub_ctl_etas_param.make_etas_value_xfer();
			internal_clean();
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			x_rjValue.xfer_get_impl().xfer_clean();
			x_commonValue.xfer_get_impl().xfer_clean();
			x_etasValue.xfer_get_impl().xfer_clean();
			return;
		}


		// Load values.

		@Override
		public XferFittingImpl xfer_load () {

			// Clean state

			xfer_clean();

			// RJ parameter values

			x_rjValue.xfer_get_impl().xfer_load();

			// Common parameter values

			x_commonValue.xfer_get_impl().xfer_load();

			// ETAS parameter values

			x_etasValue.xfer_get_impl().xfer_load();

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// RJ parameter values

			x_rjValue.xfer_get_impl().xfer_store();

			// Common parameter values

			x_commonValue.xfer_get_impl().xfer_store();

			// ETAS parameter values

			x_etasValue.xfer_get_impl().xfer_store();

			return;
		}
	}




	// Update parameters after aftershock parameter fitting.

	private void post_fitting_param_update () throws GUIEDTException {

		// Here goes any parameter updating that should occur after parameter fitting

		return;
	}




	//----- Parameter handling for generating forecasts -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferForecastView {

		// Forecast parameters.
		// Included values are checked for validity

		public OEGUISubForecast.XferFCValueView x_fcValue;	// Forecast parameter values

		// Get the implementation class.

		public abstract XferForecastImpl xfer_get_impl ();
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferForecastMod extends XferForecastView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferForecastImpl extends XferForecastMod implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferForecastImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferForecastImpl () {
			x_fcValue = sub_ctl_forecast_param.make_fc_value_xfer();
			internal_clean();
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			return;
		}


		// Load values.

		@Override
		public XferForecastImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Forecast parameter values

			x_fcValue.xfer_get_impl().xfer_load();

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// Forecast parameter values

			x_fcValue.xfer_get_impl().xfer_store();

			return;
		}
	}




	// Update parameters after aftershock parameter fitting.

	private void post_forecast_update () throws GUIEDTException {

		return;
	}




	//----- Special parameter support -----




	// Make a transfer object for analyst parameters.

	public OEGUISubAnalyst.XferAnalystImpl make_analyst_xfer () {
		return sub_ctl_analyst_option.make_analyst_xfer();
	}




//	// Load the transfer object for analyst parameters.
//	// Returns null if object could not be loaded due to some error (in which case a message is displayed). 
//
//	public OEGUISubAnalyst.XferAnalystImpl load_analyst_xfer () throws GUIEDTException {
//		OEGUISubAnalyst.XferAnalystImpl xfer_analyst_impl = sub_ctl_analyst_option.make_analyst_xfer();
//		if (!( gui_top.call_xfer_load (xfer_analyst_impl, "Incorrect analyst parameters") )) {
//			return null;
//		}
//		return xfer_analyst_impl;
//	}




	//----- Construction -----


	// Construct the controller.
	// This should not access other components.

	public OEGUIController () {

	}


	// Perform initialization after all components are linked.
	// The call order is: model, view, controller.

	public void post_link_init () throws GUIEDTException {

		// Initialize the data parameters column
		
		init_dataEditor();

		// Initialize the aftershock parameters column

		init_fitEditor();

		// Initialize the forecast/aafs column, top to bottom

		init_fcastEditor();
		init_aafsEditor();

		init_fillerEditor();

		// Set up the symbol table

		setup_symbol_table();
		
		// Enable controls

		adjust_enable();

		return;
	}




	//----- Parameter change actions ------




	// Notification of data source parameter changed.

	public boolean notify_data_source_change () throws GUIEDTException {
		if (filter_state(MODSTATE_INITIAL)) {
			return true;
		}
		return false;
	}




	// Notification of data source type.

	public void notify_data_source_type (DataSource type) throws GUIEDTException {
		
		switch (type) {

		case COMCAT:
			fetchButton.setButtonText ("Fetch Data");
			break;

		case CATALOG_FILE:
			fetchButton.setButtonText ("Load Catalog");
			break;

		case PUBLISHED_FORECAST:
			fetchButton.setButtonText ("Fetch Forecast");
			break;

		case RJ_SIMULATION:
			fetchButton.setButtonText ("Run RJ Simulation");
			break;

		case ETAS_SIMULATION:
			fetchButton.setButtonText ("Run ETAS Simulation");
			break;

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		
		fetchButton.getEditor().refreshParamEditor();
		return;
	}




	// Notification of aftershock parameter changed.

	public boolean notify_aftershock_param_change () throws GUIEDTException {
		if (filter_state(MODSTATE_CATALOG)) {
			return true;
		}
		return false;
	}




	// Notification of forecast parameter changed.

	public boolean notify_forecast_param_change () throws GUIEDTException {
		if (filter_state(MODSTATE_PARAMETERS)) {
			return true;
		}
		return false;
	}




	// Parameter change entry point.

	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Data source parameters (Event ID, time range, region, etc.).
		// - Dump any catalog that has been fetched.

		case PARMGRP_DATA_SOURCE: {
			if (filter_state(MODSTATE_INITIAL)) {
				return;
			}
		}
		break;




		// Fetch catalog button.
		// - Dump any catalog that has been fetched.
		// - Load the data source parameters.
		// - In backgound:
		//   1. Fetch the data.
		//   2. Construct the plots, and enable controls for the catalog.

		case PARMGRP_FETCH_BUTTON: {
			if (filter_state(MODSTATE_INITIAL)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferCatalogImpl xfer_catalog_impl = new XferCatalogImpl();

			// Load the data source parameters

			if (!( gui_top.call_xfer_load (xfer_catalog_impl, "Incorrect data source parameters") )) {
				return;
			}

			// Switch on data source type

			switch (xfer_catalog_impl.x_dataSource.x_dataSourceTypeParam) {


			// Fetch from Comcat

			case COMCAT: {

				// Call model to fetch from Comcat

				GUICalcStep fetchStep_1 = new GUICalcStep(
					"Fetching Events",
					"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
					new Runnable() {
						
					@Override
					public void run() {
						gui_model.fetchEvents(xfer_catalog_impl);
					}
				});

				// Store back transfer parameters, update model state, and create plots

				GUICalcStep postFetchPlotStep = new GUICalcStep("Plotting Events/Data", "...", new GUIEDTRunnable() {
						
					@Override
					public void run_in_edt() throws GUIEDTException {
						xfer_catalog_impl.xfer_store();
						advance_state(MODSTATE_CATALOG);
						post_fetch_param_update();
					}
				});

				// Run in threads

				GUICalcRunnable.run_steps (progress, null, fetchStep_1, postFetchPlotStep);
			}
			break;


			// Load catalog file

			case CATALOG_FILE: {

				// Call model to load catalog from file

				GUICalcStep loadStep_1 = new GUICalcStep("Loading Events", "...", new Runnable() {
						
						@Override
						public void run() {

							//gui_model.loadCatalog(xfer_catalog_impl, new File (xfer_catalog_impl.x_dataSource.x_catalogFileParam));

							File file = new File (xfer_catalog_impl.x_dataSource.x_catalogFileParam);
							try (
								LineSupplierFile lsf = new LineSupplierFile (file);
							){
								try {
									gui_model.loadCatalog_1 (xfer_catalog_impl, lsf);
								} catch (Exception e) {
									throw new RuntimeException ("Error reading catalog file: " + lsf.error_locus(), e);
								}
							}

							gui_model.loadCatalog_2 (xfer_catalog_impl);

						}
					});

				// Call model to finish the load, possibly including a call to Comcat

				boolean f_comcat = xfer_catalog_impl.x_dataSource.x_useComcatForMainshockParam;

				GUICalcStep loadStep_2 = new GUICalcStep(
					f_comcat ? "Fetching Mainshock" : "Loading Events",
					f_comcat ? "Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works." : "...",
					new Runnable() {
						
						@Override
						public void run() {
							gui_model.loadCatalog_3 (xfer_catalog_impl);
						}
					});

				// Store back transfer parameters, update model state, and create plots

				GUICalcStep postFetchPlotStep = new GUICalcStep("Plotting Events/Data", "...", new GUIEDTRunnable() {
					
						@Override
						public void run_in_edt() throws GUIEDTException {
							xfer_catalog_impl.xfer_store();
							advance_state(MODSTATE_CATALOG);
							post_fetch_param_update();
						}
					});

				// Run in threads

				GUICalcRunnable.run_steps (progress, null, loadStep_1, loadStep_2, postFetchPlotStep);
			}
			break;


			// Retrieve catalog from published forecast

			case PUBLISHED_FORECAST: {

				// Call model to fetch from Comcat

				GUICalcStep fetchStep_1 = new GUICalcStep(
					"Fetching Data From Forecast",
					"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
					new Runnable() {
						
					@Override
					public void run() {
						gui_model.loadCatFromForecast (xfer_catalog_impl);
					}
				});

				// Store back transfer parameters, update model state, and create plots

				GUICalcStep postFetchPlotStep = new GUICalcStep("Plotting Events/Data", "...", new GUIEDTRunnable() {
						
					@Override
					public void run_in_edt() throws GUIEDTException {
						xfer_catalog_impl.xfer_store();
						advance_state(MODSTATE_CATALOG);
						post_fetch_param_update();
					}
				});

				// Run in threads

				GUICalcRunnable.run_steps (progress, null, fetchStep_1, postFetchPlotStep);
			}
			break;


			// Run an RJ simulation

			case RJ_SIMULATION: {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Generate catalog with RJ simulation is not supported yet", "Unsupported Operation", JOptionPane.ERROR_MESSAGE);
			}
			break;


			// Run an ETAS simulation

			case ETAS_SIMULATION: {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Generate catalog with ETAS simulation is not supported yet", "Unsupported Operation", JOptionPane.ERROR_MESSAGE);
			}
			break;


			// Unknown data source type

			default:
				throw new IllegalStateException("Unknown data source type: " + xfer_catalog_impl.x_dataSource.x_dataSourceTypeParam);
			}
		}
		break;




		// Save catalog button.
		// - Display the file chooser.
		// - Write the catalog file.

		case PARMGRP_SAVE_CATALOG: {
			if (filter_state(MODSTATE_CATALOG, FILTOPT_TEST)) {
				return;
			}

			// Display the file chooser

			final File file = gui_top.showSaveFileDialog (saveCatalogButton);
			if (file == null) {
				return;
			}

			//  // Write catalog
			//  
			//  try {
			//  	GUIExternalCatalog ext_cat = new GUIExternalCatalog();
			//  	ext_cat.setup_catalog (
			//  		gui_model.get_cur_aftershocks(),
			//  		gui_model.get_cur_mainshock()
			//  	);
			//  	ext_cat.write_to_file (file);
			//  } catch (Exception e) {
			//  	e.printStackTrace();
			//  	JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
			//  			"Error Saving Catalog", JOptionPane.ERROR_MESSAGE);
			//  }

			// Write catalog

			try (
				LineConsumerFile lcf = new LineConsumerFile (file);
			){
				try {
					gui_model.saveCatalog (lcf);
				} catch (Exception e) {
					throw new RuntimeException ("Error writing catalog file: " + lcf.error_locus(), e);
				}
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
						"Error Saving Catalog", JOptionPane.ERROR_MESSAGE);
			}

		}
		break;




		// Button to compute aftershock parameters.
		// - Dump any aftershock parameters that have been computed.
		// - In backgound:
		//   1. Construct sequence-specific model, with or without time-dependent Mc.
		//   2. Construct Bayesian model, if possible.
		//   3. Update controls displaying maximum-likelihood a, p, and c;
		//      plot probability density functions;
		//      re-plot the cumulative number plot;
		//      select the tab for the probability density functions.
		//      enable controls that require aftershock parameters;

		case PARMGRP_PARAMS_BUTTON: {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferFittingImpl xfer_fitting_impl = new XferFittingImpl();

			// Load the aftershock parameters

			if (!( gui_top.call_xfer_load (xfer_fitting_impl, "Incorrect aftershock parameters") )) {
				return;
			}

			// Fit parameters

			GUICalcStep computeStep_2 = new GUICalcStep("Computing Aftershock Params", "...", new Runnable() {
						
				@Override
				public void run() {
					gui_model.fitParams(xfer_fitting_impl);
				}
			});

			// Make plots and finalize

			GUICalcStep postComputeStep = new GUICalcStep("Plotting Model PDFs", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_fitting_impl.xfer_store();
					advance_state(MODSTATE_PARAMETERS);
					post_fitting_param_update();
				}
			});

			// Run in threads

			GUICalcRunnable.run_steps (progress, null, computeStep_2, postComputeStep);
		}
		break;




		// Button to compute aftershock forecast.
		// - Dump any forecast that has been computed.
		// - In backgound:
		//   1. Calculate expected mag-freq distributions for the models.
		//   2. Plot them as expected number with magnitude >= Mc or Mc(t).
		//   3. Remove the forecast pane from the tabbed panel, if it exists.
		//   4. Calculate the forecast tables.
		//   5. Plot them in the forecast pane.

		case PARMGRP_FORECAST_BUTTON: {
			if (filter_state(MODSTATE_PARAMETERS)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferForecastImpl xfer_forecast_impl = new XferForecastImpl();

			// Load the forecast parameters

			if (!( gui_top.call_xfer_load (xfer_forecast_impl, "Incorrect forecast parameters") )) {
				return;
			}

			// Compute the forecasts

			GUICalcStep computeStep_2 = new GUICalcStep("Computing Forecast", "...", new Runnable() {
						
				@Override
				public void run() {
					gui_model.computeForecasts(progress, xfer_forecast_impl);
				}
			});

			// Make plots and finalize

			GUICalcStep postComputeStep = new GUICalcStep("Plotting Forecast", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_forecast_impl.xfer_store();
					advance_state(MODSTATE_FORECAST);
					post_forecast_update();
				}
			});

			// Run in threads

			GUICalcRunnable.run_steps (progress, null, computeStep_2, postComputeStep);
		}
		break;




		// Server status.
		// - In backgound:
		//   1. Switch view to the console.
		//   2. Fetch server status.
		//   3. Pop up a dialog box to report the result.

		case PARMGRP_SERVER_STATUS: {
			if (filter_state(MODSTATE_INITIAL, FILTOPT_TEST)) {
				return;
			}

			// Check for server access available

			if (!( gui_top.get_defer_pin_request() )) {
				if (!( gui_top.server_access_available() )) {
					JOptionPane.showMessageDialog(gui_top.get_top_window(), "Server access is not available because no PIN was entered", "No server access", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			// Request PIN if needed

			if (gui_top.get_defer_pin_request()) {
				if (!( gui_top.request_server_pin() )) {
					return;
				}
			}

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final int[] status_success = new int[1];
			status_success[0] = 0;
			GUICalcStep computeStep_1 = new GUICalcStep("Fetching AAFS Server Status", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					gui_view.view_show_console();
				}
			});
			GUICalcStep computeStep_2 = new GUICalcStep("Fetching AAFS Server Status", "...", new Runnable() {
						
				@Override
				public void run() {
					status_success[0] = gui_model.fetchServerStatus(progress);
				}
			});
			GUIEDTRunnable postComputeStep = new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					String title = "Server Status";
					String message;
					int message_type;
					int total = status_success[0]/1000;
					int healthy = status_success[0]%1000;
					if (total < 1) {
						message = "Configuration error: No servers were contacted";
						message_type = JOptionPane.ERROR_MESSAGE;
					}
					else if (healthy == total) {
						switch (total) {
						case 1: message = "The server is ALIVE"; break;
						case 2: message = "Both servers are ALIVE"; break;
						default: message = "All servers are ALIVE"; break;
						}
						message_type = JOptionPane.INFORMATION_MESSAGE;
					}
					else if (healthy == 0) {
						switch (total) {
						case 1: message = "The server is DEAD"; break;
						case 2: message = "Both servers are DEAD"; break;
						default: message = "All servers are DEAD"; break;
						}
						message_type = JOptionPane.ERROR_MESSAGE;
					}
					else {
						switch (healthy) {
						case 1: message = "1 server is ALIVE, and "; break;
						default: message = healthy + " servers are ALIVE, and "; break;
						}
						switch (total - healthy) {
						case 1: message = message + "1 server is DEAD"; break;
						default: message = message + (total - healthy) + " servers are DEAD"; break;
						}
						message_type = JOptionPane.WARNING_MESSAGE;
					}
					JOptionPane.showMessageDialog(gui_top.get_top_window(), message, title, message_type);
				}
			};
			GUICalcRunnable.run_steps (progress, postComputeStep, computeStep_1, computeStep_2);
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUIController: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}




	//----- Range parameter checking -----




//	// This function is called when there is a change in one of the grid range
//	// parameter pairs, either the range (lower/upper bounds) or the number.
//	// If the range is non-empty but the number is 1, then the number is set to defaultNum.
//	// If the range is empty but the number is > 1, then the number is set to 1.
//	// Otherwise, or if the range is invalid, then do nothing.
//	
//	private void updateRangeParams(RangeParameter rangeParam, IntegerParameter numParam, int defaultNum) throws GUIEDTException {
//		if (gui_top.get_trace_events()) {
//			System.out.println ("@@@@@ updateRangeParams (" + get_symbol(rangeParam) + ", " + get_symbol(numParam) + ", " + defaultNum + ")");
//		}
//
//		Preconditions.checkState(defaultNum > 1);
//		if (nonNullParam(rangeParam)) {
//			Range range = validParam(rangeParam);
//			boolean same = range.getLowerBound() == range.getUpperBound();
//			if (same && ((!definedParam(numParam)) || validParam(numParam) > 1)) {
//				updateParam(numParam, 1);
//			}
//			else if (!same && ((!definedParam(numParam)) || validParam(numParam) == 1)) {
//				updateParam(numParam, defaultNum);
//			}
//		}
//		return;
//	}




	// This function is called when there is a change in one of the grid range
	// parameter pairs, either the range (lower/upper bounds) or the number.
	// If the range is null, change it to the default range.  This is to work around a
	// problem with the range parameter, which is that if the user clears the parameter
	// then there is no obvious way for the user to enter a new value.
	// If the number is null, change it to 1 or the default number depending on
	// whether the range is empty or non-empty.
	// We do not attempt to force correspondence between range and number, because
	// doing so has proved to be awkward in use.
	
	public void updateRangeParams(RangeParameter rangeParam, IntegerParameter numParam,
			int defaultNum, double defaultLower, double defaultUpper) throws GUIEDTException {
		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ updateRangeParams (" + get_symbol(rangeParam) + ", " + get_symbol(numParam) + ", " + defaultNum + ", " + defaultLower + ", " + defaultUpper + ")");
		}

		Preconditions.checkState(defaultNum > 1);
		Preconditions.checkState(defaultUpper >= defaultLower);

		if (!( definedParam(rangeParam) )) {
			updateParam(rangeParam, new Range(defaultLower, defaultUpper));
		}

		if (!( definedParam(numParam) )) {
			Range range = validParam(rangeParam);
			if (range.getLowerBound() == range.getUpperBound()) {
				updateParam(numParam, 1);
			} else {
				updateParam(numParam, defaultNum);
			}
		}

		return;
	}
	



//	// Check thet the grid range number is consistent with the range limits,
//	// throw an exception if not.
//	// validateRange must run on worker threads, and so must not set any parameters or write to the screen.
//
//	private void validateRange(Range range, int num, String name) {
//		Preconditions.checkState(range != null, "Must supply "+name+" range");
//		boolean same = range.getLowerBound() == range.getUpperBound();
//		if (same)
//			Preconditions.checkState(num == 1, "Num must equal 1 for fixed "+name);
//		else
//			Preconditions.checkState(num > 1, "Num must be >1 for variable "+name);
//		return;
//	}
	



	// Check thet the grid range number is consistent with the range limits,
	// throw an exception if not.

	public void validateRange(Range range, int num, String name) {
		Preconditions.checkState(range != null, "Must supply " + name + " range");
		if (range.getLowerBound() == range.getUpperBound())
			Preconditions.checkState(num == 1, "Number must equal 1 for empty " + name + " range");
		else
			Preconditions.checkState(num > 1, "Number must be >1 for non-empty " + name + " range");
		return;
	}
	



	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
