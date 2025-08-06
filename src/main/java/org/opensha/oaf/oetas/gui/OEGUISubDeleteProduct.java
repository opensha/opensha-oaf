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
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
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
import org.opensha.oaf.util.gui.GUIPredicateStringParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.aafs.PDLSupport;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for analyst functions.
// Michael Barall
//
// This is a modeless dialog for deleting OAF and event-sequence products.


public class OEGUISubDeleteProduct extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_DELPROD_EVSEQ_OPTION = 1101;		// Event-sequence option
	private static final int PARMGRP_DELPROD_CAP_TIME = 1102;			// Event-sequence cap time
	private static final int PARMGRP_DELPROD_SEND_TO_PDL = 1103;		// Button to send delete product to PDL
	private static final int PARMGRP_DELPROD_EDIT = 1104;				// Button to open the delete product dialog




	//----- Controls for the Delete Product dialog -----




	// Event-sequence delete option; drop-down list.

	private EnumParameter<EvSeqDeleteOption> evseqDeleteOptionParam;

	private EnumParameter<EvSeqDeleteOption> init_evseqDeleteOptionParam () throws GUIEDTException {
		evseqDeleteOptionParam = new EnumParameter<EvSeqDeleteOption>(
				"Event-Sequence Option", EnumSet.allOf(EvSeqDeleteOption.class), EvSeqDeleteOption.DELETE, null);
		evseqDeleteOptionParam.setInfo ("Select the action to take for an existing event-sequence product");
		register_param (evseqDeleteOptionParam, "evseqDeleteOptionParam", PARMGRP_DELPROD_EVSEQ_OPTION);
		return evseqDeleteOptionParam;
	}




	// Set event-sequence cap time; edit box.

	private GUIPredicateStringParameter evseqCapTimeParam = null;

	private GUIPredicateStringParameter init_evseqCapTimeParam () throws GUIEDTException {
		evseqCapTimeParam = makeTimeParam ("Cap Time", "");
		evseqCapTimeParam.setInfo ("Enter the time when the earthquake sequence ends, yyyy-mm-dd hh:mm");
		register_param (evseqCapTimeParam, "evseqCapTimeParam", PARMGRP_DELPROD_CAP_TIME);
		enableParam (evseqCapTimeParam, false);
		return evseqCapTimeParam;
	}


	// Get the event-sequence cap time from the parameters, throw exception if not available.
	// Note: Caller must check that event-sequence is enabled in the action configuration, or
	// that use of event-sequence parameters is forced, before calling.

	private long get_evseqCapTime () {
		long cap_time = 0L;
		EvSeqDeleteOption evseq_delete_option = validParam(evseqDeleteOptionParam);
		switch (evseq_delete_option) {

		case DELETE: {
			cap_time = PDLCodeChooserEventSequence.CAP_TIME_DELETE;
		}
		break;

		case IGNORE: {
			cap_time = PDLCodeChooserEventSequence.CAP_TIME_NOP;
		}
		break;

		case CAP: {
			if (!( definedTimeParam(evseqCapTimeParam) )) {
				throw new RuntimeException ("Event-sequence cap time is not specified");
			}
			cap_time = validTimeParam(evseqCapTimeParam);
		}
		break;

		default:
			throw new RuntimeException ("Event-sequence option is invalid");
		}

		return cap_time;
	}


	// Pre-check the event-sequence cap time.
	// Return false if invalid, in which case a message is displayed.
	// Note: Caller must check that event-sequence is enabled in the action configuration, or
	// that use of event-sequence parameters is forced, before calling.

	private boolean precheck_evseqCapTime ()  throws GUIEDTException {
		EvSeqDeleteOption evseq_delete_option = validParam(evseqDeleteOptionParam);
		switch (evseq_delete_option) {

		case DELETE: {
		}
		break;

		case IGNORE: {
		}
		break;

		case CAP: {
			if (!( definedTimeParam(evseqCapTimeParam) )) {
				String message = "Event-sequence cap time is not specified";
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Invalid event-sequence cap time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			long cap_time = validTimeParam(evseqCapTimeParam);
			long time_now = System.currentTimeMillis();
			long mainshock_time = gui_model.get_fcmain().mainshock_time;
			if (cap_time <= mainshock_time) {
				String message = "Event-sequence cap time cannot be before the mainshock time";
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Invalid event-sequence cap time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (cap_time > mainshock_time + (SimpleUtils.DAY_MILLIS * 365L * 12L)) {
				String message = "Event-sequence cap time cannot be more than 12 years after the mainshock time";
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Invalid event-sequence cap time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (cap_time > time_now) {
				String message = "Event-sequence cap time cannot be after the current time";
				JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "Invalid event-sequence cap time", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		break;

		default:
			throw new RuntimeException ("Event-sequence option is invalid");
		}

		return true;
	}




	// Publish delete to PDL; button.

	private ButtonParameter publishDeleteButton;

	private ButtonParameter init_publishDeleteButton () throws GUIEDTException {
		String publish_delete = "Send Delete to PDL...";
		publishDeleteButton = new ButtonParameter("PDL Delete", publish_delete);
		publishDeleteButton.setInfo ("Send an OAF DELETE product to " + get_pdl_dest());
		register_param (publishDeleteButton, "publishDeleteButton", PARMGRP_DELPROD_SEND_TO_PDL);
		return publishDeleteButton;
	}




	// Delete product dialog: initialize parameters within the dialog.

	private void init_deleteProductDialogParam () throws GUIEDTException {
		
		init_evseqDeleteOptionParam();
		
		init_evseqCapTimeParam();
		
		init_publishDeleteButton();

		return;
	}




	// Analyst edit: button to activate the dialog.

	private ParameterList deleteProductList;			// List of parameters in the dialog
	private GUIParameterListParameter deleteProductEditParam;
	

	private void updatDeleteProductParamList () throws GUIEDTException {
		deleteProductList.clear();

		boolean f_button_row = false;

		deleteProductList.addParameter(evseqDeleteOptionParam);
		deleteProductList.addParameter(evseqCapTimeParam);

		deleteProductList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));
		deleteProductList.addParameter(new GUISeparatorParameter("Separator2", gui_top.get_separator_color()));

		deleteProductList.addParameter(publishDeleteButton);

		deleteProductEditParam.setListTitleText ("Delete Product");
		deleteProductEditParam.setDialogDimensions (gui_top.get_dialog_dims(3, f_button_row, 2));
		
		deleteProductEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_deleteProductEditParam () throws GUIEDTException {
		deleteProductList = new ParameterList();
		deleteProductEditParam = new GUIParameterListParameter("Delete Product", deleteProductList, "Delete OAF Product...",
							"Delete OAF Product", "Delete Product", null, null, false, gui_top.get_trace_events());
		deleteProductEditParam.setInfo("Delete OAF and event-sequence products from PDL");
		register_param (deleteProductEditParam, "deleteProductEditParam", PARMGRP_DELPROD_EDIT);

		updatDeleteProductParamList();

		return deleteProductEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable delete product controls.

	private boolean f_delete_product_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		if (gui_model.get_config_is_evseq_enabled() || gui_top.get_force_evseq_params()) {
			enableDefaultParam(evseqDeleteOptionParam, f_delete_product_enable, EvSeqDeleteOption.DELETE);
			enableDefaultParam(evseqCapTimeParam, f_delete_product_enable && validParam(evseqDeleteOptionParam) == EvSeqDeleteOption.CAP, null);
		} else {
			enableDefaultParam(evseqDeleteOptionParam, false, null);
			enableDefaultParam(evseqCapTimeParam, false, null);
		}

		enableParam(publishDeleteButton, f_delete_product_enable);

		enableParam(deleteProductEditParam, f_sub_enable);
		return;
	}




	//----- Parameter transfer -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferDeleteProductView {

		// Event-sequence cap time, or a special value

		public long x_evseqCapTime;		// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferDeleteProductImpl xfer_get_impl ();
	}




	// Transfer parameters for catalog fetch or load.

	public class XferDeleteProductImpl extends XferDeleteProductView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferDeleteProductImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferDeleteProductImpl () {
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
		public XferDeleteProductImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Event-sequence cap time

			if (gui_model.get_config_is_evseq_enabled() || gui_top.get_force_evseq_params()) {
				x_evseqCapTime = get_evseqCapTime();
			} else {
				x_evseqCapTime = PDLCodeChooserEventSequence.CAP_TIME_NOP;
			}

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

	public OEGUISubDeleteProduct (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = true;
		f_delete_product_enable = false;

		// Create and initialize controls

		init_deleteProductDialogParam();
		init_deleteProductEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the delete product edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_deleteProductEditParam () throws GUIEDTException {
		return deleteProductEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_delete_product_enable (boolean f_sub_enable, boolean f_delete_product_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_delete_product_enable = f_delete_product_enable;
		adjust_enable();
		return;
	}


	// Make a delete product transfer object.

	public XferDeleteProductImpl make_delete_product_xfer () {
		return new XferDeleteProductImpl();
	}


	// Private function, used to report that the delete product options have changed.

	private void report_delete_product_change () throws GUIEDTException {
		//gui_controller.notify_delete product_option_change();
		return;
	}


	// Update analyst values from model.

	public void update_delete_product_from_model () throws GUIEDTException {
		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Event-sequence option changed.
		// - Adjust enable of cap time parameter
		// - Report to top-level controller.

		case PARMGRP_DELPROD_EVSEQ_OPTION: {
			if (!( f_sub_enable && f_delete_product_enable )) {
				return;
			}
			if (!( gui_model.get_config_is_evseq_enabled() || gui_top.get_force_evseq_params() )) {
				return;
			}
			if (evseqCapTimeParam != null) {
				enableDefaultParam(evseqCapTimeParam, validParam(evseqDeleteOptionParam) == EvSeqDeleteOption.CAP, null);
			}
			report_delete_product_change();
		}
		break;




		// Event-sequence cap time changed.
		// - Report to top-level controller.

		case PARMGRP_DELPROD_CAP_TIME: {
			if (!( f_sub_enable && f_delete_product_enable )) {
				return;
			}
			if (!( gui_model.get_config_is_evseq_enabled() || gui_top.get_force_evseq_params() )) {
				return;
			}
			report_delete_product_change();
		}
		break;




		// Button to send delete product to PDL.
		// - Check the event-sequence cap time
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send delete product(s) to PDL.
		//   2. Pop up a dialog box to report the result.

		case PARMGRP_DELPROD_SEND_TO_PDL: {
			if (!( f_sub_enable && f_delete_product_enable )) {
				return;
			}

			// Pre-check the event-sequence cap time

			if (gui_model.get_config_is_evseq_enabled() || gui_top.get_force_evseq_params()) {
				if (!( precheck_evseqCapTime() )) {
					return;
				}
			}

			// Load the delete product parameters

			final XferDeleteProductImpl xfer_delete_product_impl = new XferDeleteProductImpl();
			if (!( gui_top.call_xfer_load (xfer_delete_product_impl, "Incorrect delete product parameters") )) {
				return;
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(gui_top.get_top_window(), "You are sending an OAF DELETE product to " + get_pdl_dest() + ".\nType \"PDL\" and press OK to send the DELETE product.", "Confirm delete", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("PDL"))) {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: DELETE product has NOT been sent to PDL", "Delete canceled", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			// Get our ForecastMainshock

			final ForecastMainshock fcmain = gui_model.get_fcmain();

			// Parameters for sending to PDL

			final boolean isReviewed = true;
			long cap_time = xfer_delete_product_impl.x_evseqCapTime;
			final boolean f_keep_reviewed = false;

			// In case of error, this contains the error message (element 0) and the stack trace (element 1)

			final String[] pdl_result = new String[2];
			pdl_result[0] = null;
			pdl_result[1] = null;

			// This receives the delete result:
			//   del_result[0] = True if an oaf product was deleted.
			//   del_result[1] = True if an event-sequence product was deleted.
			//   del_result[2] = True if an event-sequence product was capped.

			final boolean[] del_result = new boolean[3];

			// Step to send OAF and event-sequence DELETE products to PDL.

			GUICalcStep pdlSendStep_1 = new GUICalcStep("Sending DELETE product to PDL", "...", new Runnable() {
				@Override
				public void run() {
					try {
						boolean[] result = PDLSupport.static_delete_oaf_products (
							fcmain,
							isReviewed,
							cap_time,
							f_keep_reviewed,
							null		// gj_holder
						);
						del_result[0] = result[0];
						del_result[1] = result[1];
						del_result[2] = result[2];
					} catch (Exception e) {
						pdl_result[0] = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
						pdl_result[1] = SimpleUtils.getStackTraceAsString(e);
					}
				}
			});

			// Step to write back any parameter changes (we don't have any)

			GUICalcStep pdlSendStep_2 = new GUICalcStep("Sending DELETE product to PDL", "...", new GUIEDTRunnable() {
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_delete_product_impl.xfer_store();
				}
			});

			// Step to report result to user

			GUIEDTRunnable postSendStep = new GUIEDTRunnable() {
				@Override
				public void run_in_edt() throws GUIEDTException {

					// If success, report what we did

					if (pdl_result[0] == null) {
						String message = "";
						if (del_result[0] || del_result[1] || del_result[2]) {
							message = message + "Success:";
							if (del_result[0]) {
								message = message + " An OAF DELETE product has been successfully sent to PDL.";
							}
							if (del_result[1]) {
								message = message + " An existing event-sequence product has been successfully deleted.";
							} else if (del_result[2]) {
								message = message + " An existing event-sequence product has been successfully capped.";
							}
						} else {
							message = message + "Success, but no products were deleted.";
						}
						JOptionPane.showMessageDialog(gui_top.get_top_window(), message, "DELETE succeeded", JOptionPane.INFORMATION_MESSAGE);
					}

					// If error, report the exception
							
					else {
						System.err.println (pdl_result[1]);
						JOptionPane.showMessageDialog(gui_top.get_top_window(), pdl_result[0], "Error sending DELETE product", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// Run the steps

			GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlSendStep_1, pdlSendStep_2);

		}
		break;




		// Delete product edit button.
		// - Do nothing.

		case PARMGRP_DELPROD_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubDeleteProduct: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
