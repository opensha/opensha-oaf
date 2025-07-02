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
import org.opensha.oaf.util.gui.GUISeparatorParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for analyst functions.
// Michael Barall 08/31/2021
//
// This is a modeless dialog for analyst functions, including server status.


public class OEGUISubAnalyst extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_ANALYST_OPTION = 402;		// Controls to select analyst options
	private static final int PARMGRP_ANALYST_INJ_TEXT = 403;	// Button to set injectable text
	private static final int PARMGRP_ANALYST_EXPORT = 404;		// Button to export analyst options to file
	private static final int PARMGRP_ANALYST_SEND = 405;		// Button to send analyst options to server
	private static final int PARMGRP_ANALYST_EDIT = 406;		// Button to open the analyst function dialog
	private static final int PARMGRP_ANALYST_REMARK = 407;		// Button to set analyst remark




	//----- Controls for the Server dialog -----




	// Automatic forecast enable; dropdown containing an enumeration to select automatic enable option.

	private EnumParameter<AutoEnable> autoEnableParam;

	private EnumParameter<AutoEnable> init_autoEnableParam () throws GUIEDTException {
		autoEnableParam = new EnumParameter<AutoEnable>(
				"Automatic Forecasts", EnumSet.allOf(AutoEnable.class), AutoEnable.NORMAL, null);
		autoEnableParam.setInfo("Controls whether the automatic system generates forecasts for this earthquake");
		register_param (autoEnableParam, "autoEnableParam", PARMGRP_ANALYST_OPTION);
		return autoEnableParam;
	}


	// Option to supply custom parameters to automatic system; check box.

	private BooleanParameter useCustomParamsParam;

	private BooleanParameter init_useCustomParamsParam () throws GUIEDTException {
		useCustomParamsParam = new BooleanParameter("Use custom parameters", false);
		useCustomParamsParam.setInfo("Selects whether to use custom statistical parameters");
		register_param (useCustomParamsParam, "useCustomParamsParam", PARMGRP_ANALYST_OPTION);
		return useCustomParamsParam;
	}


	// Forecast duration, in days since the mainshock; edit box containing a number.
	// Default is obtained from the action configuration, typically 365.0.

	private DoubleParameter forecastDurationParam;

	private DoubleParameter init_forecastDurationParam () throws GUIEDTException {
		double duration_min = gui_model.get_min_fc_duration_days();
		double duration_max = gui_model.get_max_fc_duration_days();
		double duration_def = gui_model.get_def_fc_duration_days();

		forecastDurationParam = new DoubleParameter("Forecast Duration", duration_min, duration_max, Double.valueOf(duration_def));
		forecastDurationParam.setUnits("Days");
		forecastDurationParam.setInfo("Controls how long the automatic system keeps issuing forecasts, in days since the mainshock");
		register_param (forecastDurationParam, "forecastDurationParam", PARMGRP_ANALYST_OPTION);
		return forecastDurationParam;
	}


	// Set Injectable Text button.

	private ButtonParameter setInjTextButton;

	private ButtonParameter init_setInjTextButton () throws GUIEDTException {
		setInjTextButton = new ButtonParameter("Injectable Text", "Set Text...");
		setInjTextButton.setInfo("Enter the injectable text that appears on the event page");
		register_param (setInjTextButton, "setInjTextButton", PARMGRP_ANALYST_INJ_TEXT);
		return setInjTextButton;
	}


	// Set Analyst Remark button.

	private ButtonParameter setAnRemarkButton;

	private ButtonParameter init_setAnRemarkButton () throws GUIEDTException {
		setAnRemarkButton = new ButtonParameter("Analyst Remark", "Edit Remark...");
		setAnRemarkButton.setInfo("Enter an analyst comment that is visible in the forecast_data.json file but does not appear on the event page");
		register_param (setAnRemarkButton, "setAnRemarkButton", PARMGRP_ANALYST_REMARK);
		return setAnRemarkButton;
	}


	// Event-sequence option; dropdown containing an enumeration to select event-sequence enable option.

	private EnumParameter<EvSeqReportOption> evSeqOptionParam;

	private EnumParameter<EvSeqReportOption> init_evSeqOptionParam () throws GUIEDTException {
		evSeqOptionParam = new EnumParameter<EvSeqReportOption>(
				"Event-Sequence Option", EnumSet.allOf(EvSeqReportOption.class), EvSeqReportOption.AUTO, null);
		evSeqOptionParam.setInfo("Controls whether the automatic system generates event-sequence products");
		register_param (evSeqOptionParam, "evSeqOptionParam", PARMGRP_ANALYST_OPTION);
		return evSeqOptionParam;
	}


	// Event-sequece lookback, in days before the mainshock; edit box containing a number.
	// Default is obtained from the action configuration, typically 30.0.

	private DoubleParameter evSeqLookbackParam;

	private DoubleParameter init_evSeqLookbackParam () throws GUIEDTException {
		double lookback_min = 0.0;
		double lookback_max = gui_model.get_max_evseq_lookback_days();
		double lookback_config = gui_model.get_config_evseq_lookback_days();

		evSeqLookbackParam = new DoubleParameter("Event-Sequence Lookback", lookback_min, lookback_max, Double.valueOf(lookback_config));
		evSeqLookbackParam.setUnits("Days");
		evSeqLookbackParam.setInfo("Selects when the earthquake sequence starts, in days before the mainshock");
		register_param (evSeqLookbackParam, "evSeqLookbackParam", PARMGRP_ANALYST_OPTION);
		return evSeqLookbackParam;
	}


	// Export Analyst Options button.

	private ButtonParameter exportAnalystOptionsButton;

	private ButtonParameter init_exportAnalystOptionsButton () throws GUIEDTException {
		exportAnalystOptionsButton = new ButtonParameter("Export Analyst Options", "Export to JSON...");
		exportAnalystOptionsButton.setInfo("Exports the analyst options into a JSON file");
		register_param (exportAnalystOptionsButton, "exportAnalystOptionsButton", PARMGRP_ANALYST_EXPORT);
		return exportAnalystOptionsButton;
	}


	// Send Analyst Options to Server button.

	private ButtonParameter sendAnalystOptionsButton;

	private ButtonParameter init_sendAnalystOptionsButton () throws GUIEDTException {
		sendAnalystOptionsButton = new ButtonParameter("Send Analyst Options", "Send to Server...");
		sendAnalystOptionsButton.setInfo("Sends the analyst options to the OAF server, and triggers a new forecast");
		register_param (sendAnalystOptionsButton, "sendAnalystOptionsButton", PARMGRP_ANALYST_SEND);
		return sendAnalystOptionsButton;
	}




	// Analyst dialog: initialize parameters within the dialog.

	private void init_analystDialogParam () throws GUIEDTException {
		
		init_autoEnableParam();
		
		init_useCustomParamsParam();
		
		init_forecastDurationParam();
		
		init_setInjTextButton();
		
		init_setAnRemarkButton();

		init_evSeqOptionParam();

		init_evSeqLookbackParam();
		
		init_exportAnalystOptionsButton();
		
		init_sendAnalystOptionsButton();

		return;
	}




	// Analyst edit: button to activate the dialog.

	private ParameterList analystList;			// List of parameters in the dialog
	private GUIParameterListParameter analystEditParam;
	

	private void updateAnalystParamList () throws GUIEDTException {
		analystList.clear();

		boolean f_button_row = false;

		analystList.addParameter(autoEnableParam);
		analystList.addParameter(forecastDurationParam);
		analystList.addParameter(setInjTextButton);
		analystList.addParameter(setAnRemarkButton);
		analystList.addParameter(evSeqOptionParam);
		analystList.addParameter(evSeqLookbackParam);

		analystList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));
		analystList.addParameter(new GUISeparatorParameter("Separator2", gui_top.get_separator_color()));

		analystList.addParameter(useCustomParamsParam);
		analystList.addParameter(exportAnalystOptionsButton);
		analystList.addParameter(sendAnalystOptionsButton);

		analystEditParam.setListTitleText ("Analyst Parameters");
		analystEditParam.setDialogDimensions (gui_top.get_dialog_dims(9, f_button_row, 2));
		
		analystEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_analystEditParam () throws GUIEDTException {
		analystList = new ParameterList();
		analystEditParam = new GUIParameterListParameter("Analyst Parameters", analystList, "Edit Analyst Params...",
							"Edit Analyst Parameters", "Analyst Parameters", null, null, false, gui_top.get_trace_events());
		analystEditParam.setInfo("Set analyst parameter values, and send to AAFS server");
		register_param (analystEditParam, "analystEditParam", PARMGRP_ANALYST_EDIT);

		updateAnalystParamList();

		return analystEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable custom parameters

	private boolean f_custom_params_enable;
	private boolean last_f_custom_params_enable;

	// True to enable analyst option controls.

	private boolean f_analyst_option_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(autoEnableParam, f_analyst_option_enable);
		enableParam(forecastDurationParam, f_analyst_option_enable);
		enableParam(setInjTextButton, f_analyst_option_enable);
		enableParam(setAnRemarkButton, f_analyst_option_enable);
		enableParam(evSeqOptionParam, f_analyst_option_enable);
		enableParam(evSeqLookbackParam, f_analyst_option_enable);

		enableParam(useCustomParamsParam, f_custom_params_enable);
		if (f_custom_params_enable != last_f_custom_params_enable) {
			updateParam(useCustomParamsParam, f_custom_params_enable);
			last_f_custom_params_enable = f_custom_params_enable;
		}

		enableParam(exportAnalystOptionsButton, f_analyst_option_enable);
		enableParam(sendAnalystOptionsButton, f_analyst_option_enable);

		enableParam(analystEditParam, f_sub_enable);
		return;
	}




	//----- Parameter transfer -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferAnalystView {

		// Forecast generation option.

		public AutoEnable x_autoEnableParam;	// parameter value, checked for validity

		// Flag to use custom parameters.

		public boolean x_useCustomParamsParam;	// parameter value, checked for validity

		// Forecast duration, in days since the mainshock.

		public double x_forecastDuration;		// parameter value, checked for validity

		// Event-sequence option.

		public EvSeqReportOption x_evSeqOptionParam;	// parameter value, checked for validity

		// Event-sequence lookback.

		public double x_evSeqLookbackParam;	// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferAnalystImpl xfer_get_impl ();
	}




	// Transfer parameters for catalog fetch or load.

	public class XferAnalystImpl extends XferAnalystView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferAnalystImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferAnalystImpl () {
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
		public XferAnalystImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Forecast generation option.

			x_autoEnableParam = validParam(autoEnableParam);
		
			// Flag to use custom parameters.

			x_useCustomParamsParam = validParam(useCustomParamsParam);

			// Forecast duration, in days since the mainshock.

			x_forecastDuration = validParam(forecastDurationParam);

			// Event-sequence option.

			x_evSeqOptionParam = validParam(evSeqOptionParam);

			// Event-sequence lookback.

			x_evSeqLookbackParam = validParam(evSeqLookbackParam);

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubAnalyst (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = true;
		f_custom_params_enable = false;
		last_f_custom_params_enable = false;
		f_analyst_option_enable = false;

		// Create and initialize controls

		init_analystDialogParam();
		init_analystEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the analyst edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_analystEditParam () throws GUIEDTException {
		return analystEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_analyst_enable (boolean f_sub_enable, boolean f_custom_params_enable,
			boolean f_analyst_option_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_custom_params_enable = f_custom_params_enable;
		this.f_analyst_option_enable = f_analyst_option_enable;
		adjust_enable();

		// When analyst options are disabled, force to normal settings

		//  if (!( f_analyst_option_enable )) {
		//  	updateParam(autoEnableParam, AutoEnable.NORMAL);
		//  	updateParam(useCustomParamsParam, true);
		//  	updateParam(forecastDurationParam, gui_model.get_def_fc_duration_days());
		//  }

		return;
	}


	// Make an analyst transfer object.

	public XferAnalystImpl make_analyst_xfer () {
		return new XferAnalystImpl();
	}


	// Private function, used to report that the analyst options have changed.

	private void report_analyst_change () throws GUIEDTException {
		//gui_controller.notify_analyst_option_change();
		return;
	}


	// Update analyst values from model.

	public void update_analyst_from_model () throws GUIEDTException {

		// Forecast enable selection

		if (gui_model.get_analyst_adj_params().f_adj_intake) {
			switch (gui_model.get_analyst_adj_params().intake_option) {
			case AnalystOptions.OPT_INTAKE_BLOCK:
				updateParam(autoEnableParam, AutoEnable.DISABLE);
				break;
			case AnalystOptions.OPT_INTAKE_IGNORE:
				updateParam(autoEnableParam, AutoEnable.ENABLE);
				break;
			case AnalystOptions.OPT_INTAKE_NORMAL:
				updateParam(autoEnableParam, AutoEnable.NORMAL);
				break;
			default:
				updateParam(autoEnableParam, AutoEnable.NORMAL);
				break;
			}
		} else {
			updateParam(autoEnableParam, AutoEnable.NORMAL);
		}

		// Maximum forecast lag

		if (gui_model.get_analyst_adj_params().f_adj_max_forecast_lag) {
			if (gui_model.get_analyst_adj_params().max_forecast_lag == 0L) {
				updateParam(forecastDurationParam, gui_model.get_def_fc_duration_days());
			} else {
				updateParam(forecastDurationParam, OEGUIModel.fc_duration_to_days (gui_model.get_analyst_adj_params().max_forecast_lag));
			}
		} else {
			updateParam(forecastDurationParam, gui_model.get_def_fc_duration_days());
		}

		// Event-sequence option and lookback

		if (gui_model.get_analyst_adj_params().f_adj_evseq && gui_model.get_analyst_adj_params().evseq_cfg_params != null) {
			switch (gui_model.get_analyst_adj_params().evseq_cfg_params.get_evseq_cfg_report()) {
			case ActionConfigFile.ESREP_NO_REPORT:
				updateParam(evSeqOptionParam, EvSeqReportOption.IGNORE);
				break;
			case ActionConfigFile.ESREP_REPORT:
				updateParam(evSeqOptionParam, EvSeqReportOption.UPDATE);
				break;
			case ActionConfigFile.ESREP_DELETE:
				updateParam(evSeqOptionParam, EvSeqReportOption.DELETE);
				break;
			default:
				updateParam(evSeqOptionParam, EvSeqReportOption.AUTO);
				break;
			}

			double lookback_days = 0.0;
			long lookback = gui_model.get_analyst_adj_params().evseq_cfg_params.get_evseq_cfg_lookback();
			if (lookback > gui_model.get_min_evseq_lookback()) {
				lookback_days = Math.min (OEGUIModel.fc_duration_to_days(lookback), gui_model.get_max_evseq_lookback_days());
			}
			updateParam(evSeqLookbackParam, lookback_days);

		} else {

			updateParam(evSeqOptionParam, EvSeqReportOption.AUTO);

			double lookback_days = 0.0;
			long lookback = (new EventSequenceParameters()).fetch().get_evseq_cfg_lookback();
			if (lookback > gui_model.get_min_evseq_lookback()) {
				lookback_days = Math.min (OEGUIModel.fc_duration_to_days(lookback), gui_model.get_max_evseq_lookback_days());
			}
			updateParam(evSeqLookbackParam, lookback_days);
		}

		// Flag to use custom parameters

		updateParam(useCustomParamsParam, f_custom_params_enable);

		// At this point one might look at gui_model.get_existing_analyst_opts().analyst_params
		// to check if it is non-null and contains any non-default parameters, and update 
		// useCustomParamsParam to false if not.  However, that is probably not what the user desires.

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Analyst option changed.
		// - Report to top-level controller.

		case PARMGRP_ANALYST_OPTION: {
			if (!( f_sub_enable && f_analyst_option_enable )) {
				return;
			}
			report_analyst_change();
		}
		break;




		// Button to edit injectable text.
		// - Pop up a dialog box to edit the text.

		case PARMGRP_ANALYST_INJ_TEXT: {
			if (!( f_sub_enable && f_analyst_option_enable )) {
				return;
			}

			// Show a dialog containing the existing injectable text

			String prevText = gui_model.get_analyst_adj_params().get_injectable_text_non_null();
			if (prevText == null) {	// should never happen
				prevText = "";
			}
			JTextArea area = new JTextArea(prevText);
			JScrollPane scroll = new JScrollPane(area);
			Dimension size = new Dimension(600, 200);
			scroll.setPreferredSize(size);
			scroll.setMinimumSize(size);
			scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			int ret = JOptionPane.showConfirmDialog(gui_top.get_top_window(), scroll, "Set Injectable Text", JOptionPane.OK_CANCEL_OPTION);

			// If user entered new text, store it

			if (ret == JOptionPane.OK_OPTION) {
				String text = area.getText().trim();
				gui_model.get_analyst_adj_params().set_injectable_text_non_empty(text);
				report_analyst_change();
			}
		}
		break;




		// Button to edit analyst remark.
		// - Pop up a dialog box to edit the text.

		case PARMGRP_ANALYST_REMARK: {
			if (!( f_sub_enable && f_analyst_option_enable )) {
				return;
			}

			// Show a dialog containing the existing analyst remark

			String prevText = gui_model.get_analyst_adj_params().analyst_remark;
			if (prevText == null) {	// should never happen
				prevText = "";
			}
			JTextArea area = new JTextArea(prevText);
			JScrollPane scroll = new JScrollPane(area);
			Dimension size = new Dimension(600, 200);
			scroll.setPreferredSize(size);
			scroll.setMinimumSize(size);
			scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			int ret = JOptionPane.showConfirmDialog(gui_top.get_top_window(), scroll, "Set Analyst Remark (Caution - Publicly Visible)", JOptionPane.OK_CANCEL_OPTION);

			// If user entered new text, store it

			if (ret == JOptionPane.OK_OPTION) {
				String text = area.getText().trim();
				gui_model.get_analyst_adj_params().analyst_remark = text;
				report_analyst_change();
			}
		}
		break;




		// Export analyst options button.
		// - Display the file chooser.
		// - Write the analyst options.

		case PARMGRP_ANALYST_EXPORT: {
			if (!( f_sub_enable && f_analyst_option_enable )) {
				return;
			}

			// Load the analyst parameters

			final XferAnalystImpl xfer_analyst_impl = new XferAnalystImpl();
			if (!( gui_top.call_xfer_load (xfer_analyst_impl, "Incorrect analyst parameters") )) {
				return;
			}

			// Display the file chooser

			final File file = gui_top.showSaveFileDialog (exportAnalystOptionsButton);
			if (file == null) {
				return;
			}

			// Export

			boolean f_success = false;
			try {
				gui_model.exportAnalystOptions (xfer_analyst_impl, file);
				xfer_analyst_impl.xfer_store();
				f_success = true;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
						"Error Exporting Analyst Options", JOptionPane.ERROR_MESSAGE);
			}

			if (f_success) {
				String message = "Analyst options have been written to " + file.getPath();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Export succeeded", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		break;




		// Button to send analyst options to server.
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send analyst options to server.
		//   2. Pop up a dialog box to report the result.

		case PARMGRP_ANALYST_SEND: {
			if (!( f_sub_enable && f_analyst_option_enable )) {
				return;
			}

			// Check for server access available

			if (!( gui_top.get_defer_pin_request() )) {
				if (!( gui_top.server_access_available() )) {
					JOptionPane.showMessageDialog(gui_top.get_top_window(), "Server access is not available because no PIN was entered", "No server access", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			// Load the analyst parameters

			final XferAnalystImpl xfer_analyst_impl = new XferAnalystImpl();
			if (!( gui_top.call_xfer_load (xfer_analyst_impl, "Incorrect analyst parameters") )) {
				return;
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(gui_top.get_top_window(), "Type \"AAFS\" and press OK to send analyst options to server", "Confirm sending analyst options", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("AAFS"))) {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Analyst options have NOT been sent to server", "Send canceled", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			// Request PIN if needed

			if (gui_top.get_defer_pin_request()) {
				if (!( gui_top.request_server_pin() )) {
					JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Analyst options have NOT been sent to server", "Send canceled", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final boolean[] send_success = new boolean[1];
			send_success[0] = false;
			GUICalcStep computeStep_2 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new Runnable() {
						
				@Override
				public void run() {
					send_success[0] = gui_model.sendAnalystOptions(progress, xfer_analyst_impl);
				}
			});
			GUICalcStep computeStep_3 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_analyst_impl.xfer_store();
				}
			});
			GUIEDTRunnable postComputeStep = new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					if (send_success[0]) {
						String message = "Success: Analyst options have been successfully sent to server";
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Send Succeeded", JOptionPane.INFORMATION_MESSAGE);
					} else {
						String message = "Error: Unable to send analyst options to any server";
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Send Failed", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// If we have the info needed to sent to PDL ...

			if (gui_model.get_has_fetched_mainshock()) {

				// Run the steps

				GUICalcRunnable.run_steps (progress, postComputeStep, computeStep_2, computeStep_3);

			}

			// Otherwise ...

			else {

				// Ask user for Comcat event id

				String user_query_id = null;

				for (;;) {
					user_query_id = JOptionPane.showInputDialog (gui_top.get_top_window(), "Enter ComCat event ID for the mainshock", gui_model.get_mainshock_display_id());

					// If user canceled

					if (user_query_id == null) {
						JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Analyst options have NOT been sent to server", "Send canceled", JOptionPane.INFORMATION_MESSAGE);
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

				GUICalcRunnable.run_steps (progress, postComputeStep, pdlInfoStep, computeStep_2, computeStep_3);

			}
		}
		break;




		// Analyst option edit button.
		// - Do nothing.

		case PARMGRP_ANALYST_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubAnalyst: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
