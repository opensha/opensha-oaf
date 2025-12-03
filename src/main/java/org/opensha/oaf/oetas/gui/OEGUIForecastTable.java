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
import java.util.function.Predicate;

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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

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
import org.opensha.oaf.rj.USGS_ForecastHolder;

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
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.gui.GUIPredicateStringParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AdjustableParameters;
import org.opensha.oaf.aafs.ForecastData;
import org.opensha.oaf.aafs.EventSequenceResult;
import org.opensha.oaf.aafs.PDLSupport;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// RJ & ETAS GUI - Aftershock table implementation.
// Michael Barall 04/22/2021
//
// GUI for working with the RJ and ETAS models.
//
// The GUI follows the model-view-controller design pattern.
// This class is the aftershock forecast table.
// Although within the view, it acts like a small controller.

	
public class OEGUIForecastTable extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_FCTAB_EXPORT = 901;				// Export button (forecast.json only)
	private static final int PARMGRP_FCTAB_PUBLISH = 902;				// Publish to PDL button
	private static final int PARMGRP_FCTAB_ADVISORY_DUR_PARAM = 903;	// Advisory duration parameter
	private static final int PARMGRP_FCTAB_TEMPLATE_PARAM = 904;		// Forecast template parameter
//	private static final int PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM = 905;	// Include probability above mainshock parameter
//	private static final int PARMGRP_FCTAB_INJECTABLE_TEXT = 906;		// Injectable text button
	private static final int PARMGRP_FCTAB_FULL_EXPORT = 907;			// Export button (forecast_data.json)
	private static final int PARMGRP_FCTAB_NEXT_FC_OPTION = 908;		// Next forecast option
	private static final int PARMGRP_FCTAB_NEXT_FC_TIME = 909;			// Next forecast time
	private static final int PARMGRP_FCTAB_HELP = 910;					// Help button
	

	//----- Parameters within the panel -----


	// Export JSON to file (forecast.json only); button.

	private ButtonParameter exportButton;

	private ButtonParameter init_exportButton () throws GUIEDTException {
		exportButton = new ButtonParameter("forecast.json", "Export forecast.json only...");
		exportButton.setInfo ("Export the forecast to a forecast.json file");
		register_param (exportButton, "exportButton" + my_suffix, PARMGRP_FCTAB_EXPORT);
		return exportButton;
	}


	// Export JSON to file (forecast_data.json); button.

	private ButtonParameter exportFullButton;

	private ButtonParameter init_exportFullButton () throws GUIEDTException {
		exportFullButton = new ButtonParameter("JSON", "Export JSON...");
		exportFullButton.setInfo ("Export the forecast to a forecast_data.json file");
		register_param (exportFullButton, "exportFullButton" + my_suffix, PARMGRP_FCTAB_FULL_EXPORT);
		return exportFullButton;
	}


	// Publish forecast to PDL; button.

	private ButtonParameter publishButton;

	private ButtonParameter init_publishButton () throws GUIEDTException {
		String publish_forecast = "Publish Forecast to " + get_pdl_dest() + "...";
		//publishButton = new ButtonParameter("USGS PDL", "Publish Forecast");
		publishButton = new ButtonParameter("USGS PDL", publish_forecast);
		publishButton.setInfo ("Send the forecast to " + get_pdl_dest());
		register_param (publishButton, "publishButton" + my_suffix, PARMGRP_FCTAB_PUBLISH);
		return publishButton;
	}


	// Set forecast advisory duration; drop-down list.

	private EnumParameter<Duration> advisoryDurationParam;

	private EnumParameter<Duration> init_advisoryDurationParam () throws GUIEDTException {
		advisoryDurationParam = new EnumParameter<USGS_AftershockForecast.Duration>(
				"Advisory Duration", EnumSet.allOf(Duration.class), my_fc_holder.get_advisory_time_frame_as_enum(), null);
		advisoryDurationParam.setInfo ("Select the advisory time frame for the forecast");
		register_param (advisoryDurationParam, "advisoryDurationParam" + my_suffix, PARMGRP_FCTAB_ADVISORY_DUR_PARAM);
		return advisoryDurationParam;
	}

	private void sync_advisoryDurationParam () throws GUIEDTException {
		if (f_sync_enabled && definedParam(advisoryDurationParam)) {
			USGS_AftershockForecast.Duration value = validParam(advisoryDurationParam);
			for (OEGUIForecastTable other : sync_list) {
				if (other != this) {
					try {
						other.updateParam (other.advisoryDurationParam, value);
					} catch (Exception e) {
					}
				}
			}
		}
		return;
	}


	// Set forecast template; drop-down list.

	private EnumParameter<Template> templateParam;

	private EnumParameter<Template> init_templateParam () throws GUIEDTException {
		templateParam = new EnumParameter<USGS_AftershockForecast.Template>(
				"Template", EnumSet.allOf(Template.class), my_fc_holder.get_template_as_enum(), null);
		templateParam.setInfo ("Select the template for the OAF product");
		register_param (templateParam, "templateParam" + my_suffix, PARMGRP_FCTAB_TEMPLATE_PARAM);
		return templateParam;
	}

	private void sync_templateParam () throws GUIEDTException {
		if (f_sync_enabled && definedParam(templateParam)) {
			USGS_AftershockForecast.Template value = validParam(templateParam);
			for (OEGUIForecastTable other : sync_list) {
				if (other != this) {
					try {
						other.updateParam (other.templateParam, value);
					} catch (Exception e) {
					}
				}
			}
		}
		return;
	}


//	// Include probability of aftershock larger than mainshock; checkbox.
//
//	private BooleanParameter probAboveMainParam;
//
//	private BooleanParameter init_probAboveMainParam () throws GUIEDTException {
//		probAboveMainParam = new BooleanParameter("Include Prob \u2265 Main", my_fc_holder.get_include_above_mainshock());
//		probAboveMainParam.setInfo ("Select whether to include probability of an aftershock larger than the mainshock");
//		register_param (probAboveMainParam, "probAboveMainParam" + my_suffix, PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM);
//		return probAboveMainParam;
//	}
//
//	private void sync_probAboveMainParam () throws GUIEDTException {
//		if (f_sync_enabled && definedParam(probAboveMainParam)) {
//			Boolean value = validParam(probAboveMainParam);
//			for (OEGUIForecastTable other : sync_list) {
//				if (other != this) {
//					try {
//						other.updateParam (other.probAboveMainParam, value);
//					} catch (Exception e) {
//					}
//				}
//			}
//		}
//		return;
//	}


//	// Set injectable text; button.
//
//	private ButtonParameter injectableTextButton;
//
//	private ButtonParameter init_injectableTextButton () throws GUIEDTException {
//		injectableTextButton = new ButtonParameter("Injectable Text", "Set text...");
//		register_param (injectableTextButton, "injectableTextButton" + my_suffix, PARMGRP_FCTAB_INJECTABLE_TEXT);
//		return injectableTextButton;
//	}


	// Set next forecast option; drop-down list.

	private EnumParameter<NextForecastOption> nextForecastOptionParam;

	private EnumParameter<NextForecastOption> init_nextForecastOptionParam () throws GUIEDTException {
		nextForecastOptionParam = new EnumParameter<NextForecastOption>(
				"Next Forecast", EnumSet.allOf(NextForecastOption.class), NextForecastOption.OMIT, null);
		nextForecastOptionParam.setInfo ("Select an option for when the next forecast will be issued");
		register_param (nextForecastOptionParam, "nextForecastOptionParam" + my_suffix, PARMGRP_FCTAB_NEXT_FC_OPTION);
		return nextForecastOptionParam;
	}

	private void sync_nextForecastOptionParam () throws GUIEDTException {
		if (f_sync_enabled && definedParam(nextForecastOptionParam)) {
			NextForecastOption value = validParam(nextForecastOptionParam);
			for (OEGUIForecastTable other : sync_list) {
				if (other != this) {
					try {
						other.updateParam (other.nextForecastOptionParam, value);
						other.enableDefaultParam (other.nextForecastOptionTime, value == NextForecastOption.SET_TIME, null);
					} catch (Exception e) {
					}
				}
			}
		}
		return;
	}


	// Set next forecast time; edit box.

	private GUIPredicateStringParameter nextForecastOptionTime = null;

	private GUIPredicateStringParameter init_nextForecastOptionTime () throws GUIEDTException {
		nextForecastOptionTime = makeTimeParam ("Next Forecast Time", "");
		nextForecastOptionTime.setInfo ("Enter the time when the next forecast will be issued, yyyy-mm-dd hh:mm");
		register_param (nextForecastOptionTime, "nextForecastOptionTime" + my_suffix, PARMGRP_FCTAB_NEXT_FC_TIME);
		enableParam (nextForecastOptionTime, false);
		return nextForecastOptionTime;
	}

	private void sync_nextForecastOptionTime () throws GUIEDTException {
		if (f_sync_enabled && definedParam(nextForecastOptionTime)) {
			String value = validParam(nextForecastOptionTime);
			for (OEGUIForecastTable other : sync_list) {
				if (other != this) {
					try {
						other.updateParam (other.nextForecastOptionTime, value);
					} catch (Exception e) {
					}
				}
			}
		}
		return;
	}


	// Get the next forecast time from the parameters, throw exception if not available.

	public long get_nextForecastTime () {
		long time = 0L;
		NextForecastOption forecast_option = validParam(nextForecastOptionParam);
		switch (forecast_option) {

		case OMIT: {
			time = -2L;
		}
		break;

		case UNKNOWN: {
			time = 0L;
		}
		break;

		case NONE: {
			time = -1L;
		}
		break;

		case SET_TIME: {
			if (!( definedTimeParam(nextForecastOptionTime) )) {
				throw new RuntimeException ("Next forecast time is not specified");
			}
			time = validTimeParam(nextForecastOptionTime);
		}
		break;

		default:
			throw new RuntimeException ("Next forecast option is invalid");
		}

		return time;
	}


	// Pre-check the next forecast time.
	// Return false if invalid, in which case a message is displayed.

	public boolean precheck_nextForecastTime ()  throws GUIEDTException {
		NextForecastOption forecast_option = validParam(nextForecastOptionParam);
		switch (forecast_option) {

		case OMIT: {
		}
		break;

		case UNKNOWN: {
		}
		break;

		case NONE: {
		}
		break;

		case SET_TIME: {
			if (!( definedTimeParam(nextForecastOptionTime) )) {
				String message = "Next forecast time is not specified";
				JOptionPane.showMessageDialog(my_panel, message, "Invalid next forecast time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			long time = validTimeParam(nextForecastOptionTime);
			long time_now = System.currentTimeMillis();
			if (time <= time_now) {
				String message = "Next forecast time cannot be before the current time";
				JOptionPane.showMessageDialog(my_panel, message, "Invalid next forecast time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (time > time_now + (SimpleUtils.DAY_MILLIS * 400L)) {
				String message = "Next forecast time cannot be more than 400 days in the future";
				JOptionPane.showMessageDialog(my_panel, message, "Invalid next forecast time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		break;

		default:
			throw new RuntimeException ("Next forecast option is invalid");
		}

		return true;
	}


	// Show help; button.

	private ButtonParameter helpButton;

	private ButtonParameter init_helpButton () throws GUIEDTException {
		helpButton = new ButtonParameter("Show Help", "Help");
		helpButton.setInfo ("Show help for the forecast table");
		register_param (helpButton, "helpButton" + my_suffix, PARMGRP_FCTAB_HELP);
		return helpButton;
	}




	//--- Parameter container ---


	// Parameter list occupies the top of the panel and holds our parameters.

	private GriddedParameterListEditor paramsEditor;

	private GriddedParameterListEditor init_paramsEditor () throws GUIEDTException {

		// Controls in the container

		ParameterList params = new ParameterList();

		params.addParameter(init_exportFullButton());

		params.addParameter(init_publishButton());

		params.addParameter(init_advisoryDurationParam());

		params.addParameter(init_templateParam());

//		params.addParameter(init_probAboveMainParam());

		params.addParameter(init_nextForecastOptionParam());

		params.addParameter(init_nextForecastOptionTime());

//		params.addParameter(init_injectableTextButton());

		params.addParameter(init_exportButton());

		if (gui_top.get_provide_help()) {
			params.addParameter(init_helpButton());
		}

		// Enable synchronization

		if (sync_list != null) {
			sync_list.add (this);
			f_sync_enabled = true;
		}

		// Create the container

		paramsEditor = new GriddedParameterListEditor(params, -1, 2);
		add_symbol (paramsEditor , "paramsEditor" + my_suffix);
		return paramsEditor;
	}




	//----- Internal variables -----


	// The JSON string containing the forecast, as supplied in the constructor.

	private String my_json_string;


	// The forecast holder for this panel.
	// Note: This is a private copy, so we can modify it.

	private USGS_ForecastHolder my_fc_holder;


	// The name of this panel.

	private String my_name;


	// The PDL model code for this panel, see ForecastResults.PMCODE_XXXXX.

	private int my_pmcode;


	// The suffix applied to parameter names in this panel.

	private String my_suffix;


	// List used to synchronize across tabs.

	private List<OEGUIForecastTable> sync_list = null;


	// True if sync is enabled.

	private boolean f_sync_enabled = false;


	// Our panel.

	private JPanel my_panel;

	public JPanel get_my_panel () {
		return my_panel;
	}




	//----- Construction -----




//	// [DEPRECATED]
//	// Constructor, accepts the forecast for this panel.
//		
//	public OEGUIForecastTable (OEGUIComponent gui_comp, USGS_AftershockForecast my_forecast, String my_name) throws GUIEDTException {
//		this (gui_comp, my_forecast.buildJSONString(), my_name);
//	}




	// Constructor, accepts the forecast for this panel.
		
	public OEGUIForecastTable (OEGUIComponent gui_comp, String my_json_string, String my_name, int my_pmcode, List<OEGUIForecastTable> sync_list) throws GUIEDTException {

		// Link components

		link_components (gui_comp);

		// Save the forecast and name

		this.my_json_string = my_json_string;

		this.my_fc_holder = new USGS_ForecastHolder();
		MarshalUtils.from_json_string (this.my_fc_holder, my_json_string);

		this.my_name = my_name;

		this.my_pmcode = my_pmcode;

		this.my_suffix = "@" + my_name.replaceAll("\\s", "");	// remove white space from name

		this.sync_list = sync_list;
		this.f_sync_enabled = false;

		// Allocate the panel

		my_panel = new JPanel();
		my_panel.setLayout(new BorderLayout());

		// Initialize parameters, which occupy the top of the panel
			
		init_paramsEditor();

		// Set up symbol table

		//setup_symbol_table();

		// Set up the panel
			
		my_panel.add(paramsEditor, BorderLayout.NORTH);
		JTable jTable = new JTable(make_table_model (this.my_fc_holder));
		jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(Font.BOLD));
		my_panel.add(jTable, BorderLayout.CENTER);
	}




	// Make a table model in swing format.

	public static TableModel make_table_model (USGS_ForecastHolder fch) {
		final USGS_ForecastHolder.GUITable gui_table = fch.make_gui_table();
		return new AbstractTableModel() {

			@Override
			public int getRowCount() {
				return gui_table.get_row_count();
			}

			@Override
			public int getColumnCount() {
				return gui_table.get_col_count();
			}

			@Override
			public Object getValueAt (int rowIndex, int columnIndex) {
				return gui_table.get_gui_text (rowIndex, columnIndex);
			}
			
		};
	}




	//----- Parameter transfer -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferForecastTableView {

		// Next forecast time (the value Long.MIN_VALUE may be used to indicate no change from default).

		public long x_nextForecastTime;

		// Advisory duration.

		public USGS_AftershockForecast.Duration x_advisoryDurationParam;

		// Template.

		public USGS_AftershockForecast.Template x_templateParam;

		// PDL model code.

		public int x_pmcode;

		// Analyst parameters.

		public OEGUISubAnalyst.XferAnalystView x_analyst;

		// Get the implementation class.

		public abstract XferForecastTableImpl xfer_get_impl ();
	}




	// Transfer parameters for catalog fetch or load.

	public class XferForecastTableImpl extends XferForecastTableView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferForecastTableImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferForecastTableImpl () {
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
		public XferForecastTableImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Next forecast time

			//x_nextForecastTime = Long.MIN_VALUE;
			x_nextForecastTime = get_nextForecastTime();

			// Advisory duration

			x_advisoryDurationParam = validParam(advisoryDurationParam);

			// Template

			x_templateParam = validParam(templateParam);

			// PDL model code

			x_pmcode = my_pmcode;

			// Analyst parameters

			x_analyst = gui_controller.make_analyst_xfer();
			x_analyst.xfer_get_impl().xfer_load();

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	// Make a forecast table transfer object.

	public XferForecastTableImpl make_forecast_table_xfer () {
		return new XferForecastTableImpl();
	}




	// Make adjustable parameters for this forecast.
	// Returns null if the parameters could not be loaded, in which case a message is displayed.

	public AdjustableParameters make_fc_adj_params () throws GUIEDTException {

		// Load the parameters

		XferForecastTableImpl xfer = make_forecast_table_xfer();
		if (!( gui_top.call_xfer_load (xfer, "Incorrect forecast publishing parameters") )) {
			return null;
		}

		// Set the analyst parameters

		AdjustableParameters adj_params = gui_model.make_analyst_adj_params (xfer.x_analyst);

		// Add in the non-analyst parameters

		adj_params.set_all_non_analyst_opts (
			xfer.x_nextForecastTime,
			xfer.x_advisoryDurationParam,
			xfer.x_templateParam,
			xfer.x_pmcode
		);

		return adj_params;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// *** Export to JSON file - forecast.json.

		case PARMGRP_FCTAB_EXPORT: {

			// Pre-check the next forecast time

			if (!( precheck_nextForecastTime() )) {
				return;
			}

			// Ask user to select file

			File file = gui_top.showSaveFileDialog (exportButton);
			if (file != null) {

				// Get adjustable parameters

				AdjustableParameters adj_params = make_fc_adj_params();
				if (adj_params != null) {

					// Apply to our JSON text

					String jsonText = null;
					try {
						jsonText = adj_params.adjust_forecast_json (my_json_string);
					} catch (Exception e) {
						jsonText = null;
						e.printStackTrace();
						String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
						JOptionPane.showMessageDialog(my_panel, message, "Error building JSON", JOptionPane.ERROR_MESSAGE);
					}

					// If succeeded, write to file, display error message if I/O error

					if (jsonText != null) {
						boolean f_success = false;
						try {
							FileWriter fw = new FileWriter(file);
							fw.write(jsonText);
							fw.close();
							f_success = true;
						} catch (IOException e) {
							e.printStackTrace();
							String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
							JOptionPane.showMessageDialog(my_panel, message, "Error writing JSON to file", JOptionPane.ERROR_MESSAGE);
						}

						if (f_success) {
							String message = "Forecast has been written to " + file.getPath();
							JOptionPane.showMessageDialog(my_panel, message, "Export succeeded", JOptionPane.INFORMATION_MESSAGE);
						}
					}
				}
			}
		}
		break;




		// *** Export to JSON file - forecast_data.json.

		case PARMGRP_FCTAB_FULL_EXPORT: {

			// Pre-check the next forecast time

			if (!( precheck_nextForecastTime() )) {
				return;
			}

			// Ask user to select file

			final File file = gui_top.showSaveFileDialog (exportFullButton);
			if (file != null) {

				// Get adjustable parameters

				final AdjustableParameters adj_params = make_fc_adj_params();
				if (adj_params != null) {

					// Get our ForecastData

					final ForecastData fcdata = gui_model.get_forecast_fcdata();

					// Set for not sent to PDL

					fcdata.pdl_event_id = "";
					fcdata.pdl_is_reviewed = false;

					// In case of error, this contains the error message (element 0) and the stack trace (element 1)

					final String[] export_result = new String[2];
					export_result[0] = null;
					export_result[1] = null;

					// Step to adjust parameters and export to file.

					GUICalcStep fileExportStep = new GUICalcStep("Writing forecast to JSON file", "...", new Runnable() {
						@Override
						public void run() {
							try (
								AdjustableParameters.AutoAdjForecastData auto_adj = adj_params.get_auto_adj (fcdata);
							) {
								MarshalUtils.to_json_file (fcdata, file);
							} catch (Exception e) {
								export_result[0] = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
								export_result[1] = SimpleUtils.getStackTraceAsString(e);
							}
						}
					});

					// Step to report result to user

					GUIEDTRunnable postExportStep = new GUIEDTRunnable() {
						@Override
						public void run_in_edt() throws GUIEDTException {

							// If success, report what we did

							if (export_result[0] == null) {
								String message = "Forecast has been written to " + file.getPath();
								JOptionPane.showMessageDialog(my_panel, message, "Export succeeded", JOptionPane.INFORMATION_MESSAGE);
							}

							// If error, report the exception
							
							else {
								System.err.println (export_result[1]);
								JOptionPane.showMessageDialog(my_panel, export_result[0], "Error exporting forecast to file", JOptionPane.ERROR_MESSAGE);
							}
						}
					};

					// If we have the info needed to sent to PDL ...

					if (gui_model.get_has_fetched_mainshock()) {

						// Run the steps

						GUICalcRunnable.run_steps (gui_top.get_top_window(), postExportStep, fileExportStep);

					}

					// Otherwise ...

					else {

						// Ask user for Comcat event id

						String user_query_id = null;

						for (;;) {
							user_query_id = JOptionPane.showInputDialog (my_panel, "Enter ComCat event ID for the mainshock", gui_model.get_mainshock_display_id());

							// If user canceled

							if (user_query_id == null) {
								return;
							}

							// If it's not an empty string, stop looping

							if (!( user_query_id.trim().isEmpty() )) {
								break;
							}
						}

						final String query_id = user_query_id.trim();

						// Step to retrieve PDL info from Comcat

						GUICalcStep pdlInfoStep = new GUICalcStep(
							"Fetching Mainshock Information",
							"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
							new Runnable() {
						
							@Override
							public void run() {
								gui_model.fetch_mainshock_pdl_info (query_id);
							}
						});

						// Run the steps

						GUICalcRunnable.run_steps (gui_top.get_top_window(), postExportStep, pdlInfoStep, fileExportStep);

					}
				}
			}
		}
		break;




		//*** Publish forecast to PDL

		case PARMGRP_FCTAB_PUBLISH: {

			// Pre-check the next forecast time

			if (!( precheck_nextForecastTime() )) {
				return;
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(my_panel, "You are sending the forecast to " + get_pdl_dest() + ".\nType \"PDL\" and press OK to publish forecast.", "Confirm publication", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("PDL"))) {
				JOptionPane.showMessageDialog(my_panel, "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);

			// User confirmed ...

			} else {

				// Get adjustable parameters

				final AdjustableParameters adj_params = make_fc_adj_params();
				if (adj_params != null) {

					// Get our ForecastData

					final ForecastData fcdata = gui_model.get_forecast_fcdata();

					// Set for not sent to PDL

					fcdata.pdl_event_id = "";
					fcdata.pdl_is_reviewed = false;

					// Parameters for sending to PDL

					final boolean isReviewed = true;
					final EventSequenceResult evseq_res = new EventSequenceResult();

					// In case of error, this contains the error message (element 0) and the stack trace (element 1)

					final String[] pdl_result = new String[2];
					pdl_result[0] = null;
					pdl_result[1] = null;

					// Step to adjust parameters and send to PDL.

					GUICalcStep pdlSendStep = new GUICalcStep("Sending product to PDL", "...", new Runnable() {
						@Override
						public void run() {
							try (
								AdjustableParameters.AutoAdjForecastData auto_adj = adj_params.get_auto_adj (fcdata);
							) {
								String event_id = PDLSupport.static_send_pdl_report (
									isReviewed,
									evseq_res,
									fcdata
								);
							} catch (Exception e) {
								pdl_result[0] = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
								pdl_result[1] = SimpleUtils.getStackTraceAsString(e);
							}
						}
					});

					// Step to report result to user

					GUIEDTRunnable postSendStep = new GUIEDTRunnable() {
						@Override
						public void run_in_edt() throws GUIEDTException {

							// If success, report what we did

							if (pdl_result[0] == null) {
								String message = "Success: Forecast has been successfully sent to PDL.";
								if (evseq_res.was_evseq_sent_ok()) {
									message = message + " An event-sequence product has been successfully sent to PDL.";
								} else if (evseq_res.was_evseq_deleted()) {
									message = message + " An existing event-sequence product has been successfully deleted.";
								} else if (evseq_res.was_evseq_capped()) {
									message = message + " An existing event-sequence product has been successfully capped.";
								}
								JOptionPane.showMessageDialog(my_panel, message, "Publication succeeded", JOptionPane.INFORMATION_MESSAGE);
							}

							// If error, report the exception
							
							else {
								System.err.println (pdl_result[1]);
								JOptionPane.showMessageDialog(my_panel, pdl_result[0], "Error sending product", JOptionPane.ERROR_MESSAGE);
							}
						}
					};

					// If we have the info needed to sent to PDL ...

					if (gui_model.get_has_fetched_mainshock()) {

						// Run the steps

						GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlSendStep);

					}

					// Otherwise ...

					else {

						// Ask user for Comcat event id

						String user_query_id = null;

						for (;;) {
							user_query_id = JOptionPane.showInputDialog (my_panel, "Enter ComCat event ID for the mainshock", gui_model.get_mainshock_display_id());

							// If user canceled

							if (user_query_id == null) {
								JOptionPane.showMessageDialog(my_panel, "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);
								return;
							}

							// If it's not an empty string, stop looping

							if (!( user_query_id.trim().isEmpty() )) {
								break;
							}
						}

						final String query_id = user_query_id.trim();

						// Step to retrieve PDL info from Comcat

						GUICalcStep pdlInfoStep = new GUICalcStep(
							"Fetching Mainshock Information",
							"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
							new Runnable() {
						
							@Override
							public void run() {
								gui_model.fetch_mainshock_pdl_info (query_id);
							}
						});

						// Run the steps

						GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlInfoStep, pdlSendStep);

					}
				}
			}
		}
		break;




		//*** Set advisory duration, from dropdown list

		case PARMGRP_FCTAB_ADVISORY_DUR_PARAM: {
			//my_fc_holder.set_advisory_time_frame_from_enum (validParam(advisoryDurationParam));
			sync_advisoryDurationParam();
		}
		break;




		//*** Set PDL template, from dropdown list

		case PARMGRP_FCTAB_TEMPLATE_PARAM: {
			//my_fc_holder.set_template_from_enum (validParam(templateParam));
			sync_templateParam();
		}
		break;




//		//*** Select whether to include probability of an aftershock larger than mainshock, from checkbox
//
//		case PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM: {
//			//my_fc_holder.set_include_above_mainshock (validParam(probAboveMainParam));
//			sync_probAboveMainParam();
//		}
//		break;




		//*** Set next forecast option, from dropdown list

		case PARMGRP_FCTAB_NEXT_FC_OPTION: {
			if (nextForecastOptionTime != null) {
				enableDefaultParam(nextForecastOptionTime, validParam(nextForecastOptionParam) == NextForecastOption.SET_TIME, null);
			}
			sync_nextForecastOptionParam();
		}
		break;




		//*** Set next forecast time

		case PARMGRP_FCTAB_NEXT_FC_TIME: {
			sync_nextForecastOptionTime();
		}
		break;




		//*** Show help

		case PARMGRP_FCTAB_HELP: {
			gui_top.show_help (null, "help_forecast_table.html");
		}
		break;




//		//*** Enter the injectable text
//
//		case PARMGRP_FCTAB_INJECTABLE_TEXT: {
//
//			//  // Show a dialog containing the existing injectable text
//			//  
//			//  String prevText = my_fc_holder.get_injectable_text();
//			//  if (prevText == null)
//			//  	prevText = "";
//			//  JTextArea area = new JTextArea(prevText);
//			//  Dimension size = new Dimension(300, 200);
//			//  area.setPreferredSize(size);
//			//  area.setMinimumSize(size);
//			//  area.setLineWrap(true);
//			//  area.setWrapStyleWord(true);
//			//  int ret = JOptionPane.showConfirmDialog(my_panel, area, "Set Injectable Text", JOptionPane.OK_CANCEL_OPTION);
//			//  
//			//  // If user entered new text, store it in the forecast
//			//  
//			//  if (ret == JOptionPane.OK_OPTION) {
//			//  	String text = area.getText().trim();
//			//  	if (text.length() == 0)
//			//  		text = null;
//			//  	my_fc_holder.set_injectable_text(text);
//			//  }
//
//			// Show a dialog containing the existing injectable text
//			// (Same code as in OEGUIController.)
//
//			String prevText = gui_model.get_analyst_adj_params().get_injectable_text_non_null();
//			if (prevText == null) {	// should never happen
//				prevText = "";
//			}
//			JTextArea area = new JTextArea(prevText);
//			JScrollPane scroll = new JScrollPane(area);
//			Dimension size = new Dimension(600, 200);
//			scroll.setPreferredSize(size);
//			scroll.setMinimumSize(size);
//			scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//			area.setLineWrap(true);
//			area.setWrapStyleWord(true);
//			int ret = JOptionPane.showConfirmDialog(gui_top.get_top_window(), scroll, "Set Injectable Text", JOptionPane.OK_CANCEL_OPTION);
//
//			// If user entered new text, store it
//
//			if (ret == JOptionPane.OK_OPTION) {
//				String text = area.getText().trim();
//				gui_model.get_analyst_adj_params().set_injectable_text_non_empty(text);
//			}
//		}
//		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUIForecastTable: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
