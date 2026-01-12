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
import org.opensha.oaf.util.gui.GUIParameterListParameter;
import org.opensha.oaf.util.gui.GUISeparatorParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.aafs.ForecastData;
import org.opensha.oaf.aafs.EventSequenceResult;
import org.opensha.oaf.aafs.PDLSupport;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for file operations.
// Michael Barall
//
// This is a modeless dialog for file operations.


public class OEGUISubFileOps extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_FILEOPS_OPTION = 1201;			// Controls to select file operation options
	private static final int PARMGRP_FILEOPS_EDIT = 1202;			// Button to open the file operations dialog
	private static final int PARMGRP_FILEOPS_SEND_ANALYST = 1203;	// Button to send analyst options from a file
	private static final int PARMGRP_FILEOPS_SEND_FORECAST = 1204;	// Button to send forecast from a file
	private static final int PARMGRP_FILEOPS_SEND_FCJSON = 1205;	// Button to send forecast.json from a file




	//----- Controls for the file operations dialog -----




	// Send analyst options from file button.

	private ButtonParameter sendAnalystFileButton;

	private ButtonParameter init_sendAnalystFileButton () throws GUIEDTException {
		sendAnalystFileButton = new ButtonParameter("Send Analyst Options File", "Send to Server...");
		sendAnalystFileButton.setInfo("Read analyst options from a file and send them to the AAFS server");
		register_param (sendAnalystFileButton, "sendAnalystFileButton", PARMGRP_FILEOPS_SEND_ANALYST);
		return sendAnalystFileButton;
	}


	// Send forecast from a file button.

	private ButtonParameter sendForecastFileButton;

	private ButtonParameter init_sendForecastFileButton () throws GUIEDTException {
		sendForecastFileButton = new ButtonParameter("Send Forecast File", "Send to PDL...");
		sendForecastFileButton.setInfo("Read a forecast from a file and send it to PDL");
		register_param (sendForecastFileButton, "sendForecastFileButton", PARMGRP_FILEOPS_SEND_FORECAST);
		return sendForecastFileButton;
	}


	// Event-sequence option; dropdown containing an enumeration to select event-sequence enable option.

	private EnumParameter<EvSeqReportOption> evSeqOptionParam;

	private EnumParameter<EvSeqReportOption> init_evSeqOptionParam () throws GUIEDTException {
		evSeqOptionParam = new EnumParameter<EvSeqReportOption>(
				"Event-Sequence Option", EnumSet.allOf(EvSeqReportOption.class), EvSeqReportOption.AUTO, null);
		evSeqOptionParam.setInfo("Controls whether to send an event-sequence product along with forecast.json");
		register_param (evSeqOptionParam, "evSeqOptionParam", PARMGRP_FILEOPS_OPTION);
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
		register_param (evSeqLookbackParam, "evSeqLookbackParam", PARMGRP_FILEOPS_OPTION);
		return evSeqLookbackParam;
	}


	// Send forecast.json from a file button.

	private ButtonParameter sendFcJsonFileButton;

	private ButtonParameter init_sendFcJsonFileButton () throws GUIEDTException {
		sendFcJsonFileButton = new ButtonParameter("Send forecast.json", "Send to PDL...");
		sendFcJsonFileButton.setInfo("Reads forecast.json from a file and sends it to PDL");
		register_param (sendFcJsonFileButton, "sendFcJsonFileButton", PARMGRP_FILEOPS_SEND_FCJSON);
		return sendFcJsonFileButton;
	}




	// Analyst dialog: initialize parameters within the dialog.

	private void init_fileOpsDialogParam () throws GUIEDTException {
		
		init_sendAnalystFileButton();
		
		init_sendForecastFileButton();

		init_evSeqOptionParam();

		init_evSeqLookbackParam();
		
		init_sendFcJsonFileButton();

		return;
	}




	// File operations: button to activate the dialog.

	private ParameterList fileOpsList;			// List of parameters in the dialog
	private GUIParameterListParameter fileOpsEditParam;
	

	private void updateFileOpsParamList () throws GUIEDTException {
		fileOpsList.clear();

		boolean f_button_row = false;

		fileOpsList.addParameter(sendAnalystFileButton);

		fileOpsList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));
		fileOpsList.addParameter(new GUISeparatorParameter("Separator2", gui_top.get_separator_color()));

		fileOpsList.addParameter(sendForecastFileButton);

		fileOpsList.addParameter(new GUISeparatorParameter("Separator3", gui_top.get_separator_color()));
		fileOpsList.addParameter(new GUISeparatorParameter("Separator4", gui_top.get_separator_color()));

		fileOpsList.addParameter(evSeqOptionParam);
		fileOpsList.addParameter(evSeqLookbackParam);
		fileOpsList.addParameter(sendFcJsonFileButton);

		fileOpsEditParam.setListTitleText ("File Operations");
		fileOpsEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row, 4));
		
		fileOpsEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_fileOpsEditParam () throws GUIEDTException {
		fileOpsList = new ParameterList();
		fileOpsEditParam = new GUIParameterListParameter("File Operations", fileOpsList, "File Operations...",
							"Perform File Operations", "File Operations", null, null, false, gui_top.get_trace_events(),
							gui_top.make_help ("help_mtool_file_ops.html"));
		fileOpsEditParam.setInfo("Read analyst options or forecast from a file, and send to AAFS server or to PDL");
		register_param (fileOpsEditParam, "fileOpsEditParam", PARMGRP_FILEOPS_EDIT);

		updateFileOpsParamList();

		return fileOpsEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable file operation controls.

	private boolean f_file_ops_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(sendAnalystFileButton, f_file_ops_enable);
		enableParam(sendForecastFileButton, f_file_ops_enable);

		enableParam(evSeqOptionParam, f_file_ops_enable);
		enableParam(evSeqLookbackParam, f_file_ops_enable);

		enableParam(sendFcJsonFileButton, f_file_ops_enable);

		enableParam(fileOpsEditParam, f_sub_enable);
		return;
	}




	//----- Parameter transfer -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferFileOpsView {

		// Event-sequence option.

		public EvSeqReportOption x_evSeqOptionParam;	// parameter value, checked for validity

		// Event-sequence lookback.

		public double x_evSeqLookbackParam;	// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferFileOpsImpl xfer_get_impl ();
	}




	// Transfer parameters.

	public class XferFileOpsImpl extends XferFileOpsView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferFileOpsImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferFileOpsImpl () {
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
		public XferFileOpsImpl xfer_load () {

			// Clean state

			xfer_clean();

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

	public OEGUISubFileOps (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = true;
		f_file_ops_enable = false;

		// Create and initialize controls

		init_fileOpsDialogParam();
		init_fileOpsEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the file operations button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_fileOpsEditParam () throws GUIEDTException {
		return fileOpsEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_file_ops_enable (boolean f_sub_enable, boolean f_file_ops_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_file_ops_enable = f_file_ops_enable;
		adjust_enable();
		return;
	}


	// Make an analyst transfer object.

	public XferFileOpsImpl make_file_ops_xfer () {
		return new XferFileOpsImpl();
	}


	// Private function, used to report that the file operation options have changed.

	private void report_file_ops_change () throws GUIEDTException {
		//gui_controller.notify_file_ops_option_change();
		return;
	}


	// Update file operation values from model.

	public void update_file_ops_from_model () throws GUIEDTException {

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

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// File operation edit button.
		// - Do nothing.

		case PARMGRP_FILEOPS_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// File operation option changed.
		// - Report to top-level controller.

		case PARMGRP_FILEOPS_OPTION: {
			if (!( f_sub_enable && f_file_ops_enable )) {
				return;
			}
			report_file_ops_change();
		}
		break;




		// Button to send analyst options to server.
		// - Ask user to select file.
		// - Load file.
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send analyst options to server.
		//   2. Pop up a dialog box to report the result.

		case PARMGRP_FILEOPS_SEND_ANALYST: {
			if (!( f_sub_enable && f_file_ops_enable )) {
				return;
			}

			// Check for server access available

			if (!( gui_top.get_defer_pin_request() )) {
				if (!( gui_top.server_access_available() )) {
					JOptionPane.showMessageDialog(gui_top.get_top_window(), "Server access is not available because no PIN was entered", "No server access", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}

			// Load the file operation parameters

			final XferFileOpsImpl xfer_file_ops_impl = new XferFileOpsImpl();
			if (!( gui_top.call_xfer_load (xfer_file_ops_impl, "Incorrect file operation parameters") )) {
				return;
			}

			// Ask user to select a file

			File file = gui_top.showSelectFileDialog (sendAnalystFileButton, "Select");
			if (file == null) {
				return;
			}

			// Load the analyst options

			AnalystOptions temp_anopt = null;
			try {
				temp_anopt = gui_model.readAnalystOptions (file);

				if (temp_anopt == null) {
					throw new RuntimeException ("File contains null analyst options");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				//String message = "Error reading analyst options from file: " + file.getPath() + "\n" + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				String message = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Error reading analyst options from file", JOptionPane.ERROR_MESSAGE);
				return;
			}

			final AnalystOptions anopt = temp_anopt;

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
					send_success[0] = gui_model.sendAnalystOptions(progress, anopt);
				}
			});
			GUICalcStep computeStep_3 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_file_ops_impl.xfer_store();
				}
			});
			GUIEDTRunnable postComputeStep = new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					if (send_success[0]) {
						String message = "Success: Analyst options have been successfully sent to server.";
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Send Succeeded", JOptionPane.INFORMATION_MESSAGE);
					} else {
						gui_view.view_show_console();
						String message = "Error: Unable to send analyst options to any server.\n\nMore information may be available in the Console window.";
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Send Failed", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// If we have the info needed to sent to PDL ...

			if (gui_model.get_has_fetched_mainshock()) {

				// Run the steps

				GUICalcRunnable.run_steps (progress, postComputeStep, computeStep_2, computeStep_3);

			}

			// Otherwise (should never happen) ...

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




		// Button to send forecast to PDL.
		// - Ask user to select file.
		// - Load file.
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send forecast to PDL.
		//   2. Pop up a dialog box to report the result.

		case PARMGRP_FILEOPS_SEND_FORECAST: {
			if (!( f_sub_enable && f_file_ops_enable )) {
				return;
			}

			// Load the file operation parameters

			final XferFileOpsImpl xfer_file_ops_impl = new XferFileOpsImpl();
			if (!( gui_top.call_xfer_load (xfer_file_ops_impl, "Incorrect file operation parameters") )) {
				return;
			}

			// Ask user to select a file

			File file = gui_top.showSelectFileDialog (sendAnalystFileButton, "Select");
			if (file == null) {
				return;
			}

			// Load the ForecastData

			final ForecastData fcdata = new ForecastData();
			try {
				MarshalUtils.from_json_file (fcdata, file);
			}
			catch (Exception e) {
				e.printStackTrace();
				//String message = "Error reading forecast from file: " + file.getPath() + "\n" + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				String message = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Error reading forecast from file", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// If we have the info needed to sent to PDL ...

			if (gui_model.get_has_fetched_mainshock()) {

				// Check if the event ID in the ForecastData matches an event ID of the current mainshock

				String event_id = fcdata.mainshock.mainshock_event_id;
				boolean f_match = gui_model.get_fcmain().mainshock_event_id.equals (event_id);

				for (String id : gui_model.get_fcmain().mainshock_id_list) {
					if (id.equals (event_id)) {
						f_match = true;
					}
				}

				// If no match, warn the user

				if (!( f_match )) {
					String message = "The event ID in the forecast file does not match an event ID of the current mainshock. Continue anyway?";
					int conf = JOptionPane.showConfirmDialog(gui_top.get_top_window(), message, "Confirm file selection", JOptionPane.YES_NO_OPTION);
					if (conf != JOptionPane.YES_OPTION) {
						return;
					}
				}
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(gui_top.get_top_window(), "You are sending the forecast to " + get_pdl_dest() + ".\nType \"PDL\" and press OK to publish forecast.", "Confirm publication", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("PDL"))) {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

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

			// Step to send to PDL.

			GUICalcStep pdlSendStep_1 = new GUICalcStep("Sending product to PDL", "...", new Runnable() {
				@Override
				public void run() {
					try {
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

			// Step to write back parameters

			GUICalcStep pdlSendStep_2 = new GUICalcStep("Sending product to PDL", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_file_ops_impl.xfer_store();
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
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Publication succeeded", JOptionPane.INFORMATION_MESSAGE);
					}

					// If error, report the exception
							
					else {
						gui_view.view_show_console();
						System.err.println (pdl_result[1]);
						JOptionPane.showMessageDialog(gui_top.get_top_window(), pdl_result[0] + "\n\nMore information may be available in the Console window.", "Error sending product", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// If we have the info needed to sent to PDL ...

			if (gui_model.get_has_fetched_mainshock()) {

				// Run the steps

				GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlSendStep_1, pdlSendStep_2);

			}

			// Otherwise ...

			else {

				// Ask user for Comcat event id

				String user_query_id = null;

				for (;;) {
					user_query_id = JOptionPane.showInputDialog (gui_top.get_top_window(), "Enter ComCat event ID for the mainshock", gui_model.get_mainshock_display_id());

					// If user canceled

					if (user_query_id == null) {
						JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);
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

				GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlInfoStep, pdlSendStep_1, pdlSendStep_2);

			}

		}
		break;




		// Button to send forecast.json to PDL.
		// - Ask user to select file.
		// - Load file.
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send forecast to PDL.
		//   2. Pop up a dialog box to report the result.

		case PARMGRP_FILEOPS_SEND_FCJSON: {
			if (!( f_sub_enable && f_file_ops_enable )) {
				return;
			}

			// Load the file operation parameters

			final XferFileOpsImpl xfer_file_ops_impl = new XferFileOpsImpl();
			if (!( gui_top.call_xfer_load (xfer_file_ops_impl, "Incorrect file operation parameters") )) {
				return;
			}

			// Ask user to select a file

			File file = gui_top.showSelectFileDialog (sendAnalystFileButton, "Select");
			if (file == null) {
				return;
			}

			// Load the forecast.json

			final USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
			try {
				MarshalUtils.from_json_file (fc_holder, file);
			}
			catch (Exception e) {
				e.printStackTrace();
				//String message = "Error reading forecast from file: " + file.getPath() + "\n" + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				String message = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Error reading forecast.json from file", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(gui_top.get_top_window(), "You are sending the forecast to " + get_pdl_dest() + ".\nType \"PDL\" and press OK to publish forecast.", "Confirm publication", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("PDL"))) {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			// Parameters for sending to PDL

			final boolean isReviewed = true;
			final EventSequenceResult evseq_res = new EventSequenceResult();

			// In case of error, this contains the error message (element 0) and the stack trace (element 1)

			final String[] pdl_result = new String[2];
			pdl_result[0] = null;
			pdl_result[1] = null;

			// Step to send to PDL.

			GUICalcStep pdlSendStep_1 = new GUICalcStep("Sending product to PDL", "...", new Runnable() {
				@Override
				public void run() {
					try {

						// Get event-sequence configuration
						
						EventSequenceParameters evseq_cfg_params = null;
						switch (xfer_file_ops_impl.x_evSeqOptionParam) {
						case IGNORE:
							evseq_cfg_params = EventSequenceParameters.make_coerce (
								ActionConfigFile.ESREP_NO_REPORT, SimpleUtils.days_to_millis (xfer_file_ops_impl.x_evSeqLookbackParam));
							break;
						case UPDATE:
							evseq_cfg_params = EventSequenceParameters.make_coerce (
								ActionConfigFile.ESREP_REPORT, SimpleUtils.days_to_millis (xfer_file_ops_impl.x_evSeqLookbackParam));
							break;
						case DELETE:
							evseq_cfg_params = EventSequenceParameters.make_coerce (
								ActionConfigFile.ESREP_DELETE, SimpleUtils.days_to_millis (xfer_file_ops_impl.x_evSeqLookbackParam));
							break;
						default:		// actually will be AUTO
							evseq_cfg_params = (new EventSequenceParameters()).fetch();
							break;
						}

						// Send to PDL

						PDLSupport.static_send_pdl_report (
							isReviewed,
							evseq_res,
							gui_model.get_fcmain(),
							evseq_cfg_params,
							fc_holder
						);
					} catch (Exception e) {
						pdl_result[0] = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
						pdl_result[1] = SimpleUtils.getStackTraceAsString(e);
					}
				}
			});

			// Step to write back parameters

			GUICalcStep pdlSendStep_2 = new GUICalcStep("Sending product to PDL", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_file_ops_impl.xfer_store();
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
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Publication succeeded", JOptionPane.INFORMATION_MESSAGE);
					}

					// If error, report the exception
							
					else {
						gui_view.view_show_console();
						System.err.println (pdl_result[1]);
						JOptionPane.showMessageDialog(gui_top.get_top_window(), pdl_result[0] + "\n\nMore information may be available in the Console window.", "Error sending product", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// If we have the info needed to sent to PDL ...

			if (gui_model.get_has_fetched_mainshock()) {

				// Run the steps

				GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlSendStep_1, pdlSendStep_2);

			}

			// Otherwise ...

			else {

				// Ask user for Comcat event id

				String user_query_id = null;

				for (;;) {
					user_query_id = JOptionPane.showInputDialog (gui_top.get_top_window(), "Enter ComCat event ID for the mainshock", gui_model.get_mainshock_display_id());

					// If user canceled

					if (user_query_id == null) {
						JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);
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

				GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlInfoStep, pdlSendStep_1, pdlSendStep_2);

			}

		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubFileOps: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
