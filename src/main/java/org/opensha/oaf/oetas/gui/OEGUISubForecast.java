package org.opensha.oaf.oetas.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.Component;
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

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for forecast parameter values.
// Michael Barall 09/01/2021
//
// This is a modeless dialog for entering forecast parameter values.


public class OEGUISubForecast extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_FCAST_EDIT = 701;		// Button to open the forecast parameter edit dialog
	private static final int PARMGRP_FCAST_VALUE = 702;		// Forecast parameter value
	private static final int PARMGRP_NOW_BUTTON = 703;		// Button to set start time to now




	//----- Controls for the forecast parameter dialog -----


	// Set To Now button.
	// Sets forecast start time so the forecast starts now.
	// Sets forecast end time to preserve end minus start.

	private ButtonParameter forecastStartTimeNowParam;

	private ButtonParameter init_forecastStartTimeNowParam () throws GUIEDTException {
		forecastStartTimeNowParam = new ButtonParameter("Set Forecast Start Time", "Set to Now");
		register_param (forecastStartTimeNowParam, "forecastStartTimeNowParam", PARMGRP_NOW_BUTTON);
		return forecastStartTimeNowParam;
	}


	// Forecast start time, in days since the mainshock; edit box containing a number.
	// Default is 0.0.

	private DoubleParameter forecastStartTimeParam;

	private DoubleParameter init_forecastStartTimeParam () throws GUIEDTException {
		forecastStartTimeParam = new DoubleParameter("Forecast Start Time", 0d, 36500d, new Double(0d));
		forecastStartTimeParam.setUnits("Days");
		forecastStartTimeParam.setInfo("Forecast start relative to main shock origin time");
		register_param (forecastStartTimeParam, "forecastStartTimeParam", PARMGRP_FCAST_VALUE);
		return forecastStartTimeParam;
	}


	// Forecast end time, in days since the mainshock; edit box containing a number.
	// Default is 7.0.

	private DoubleParameter forecastEndTimeParam;

	private DoubleParameter init_forecastEndTimeParam () throws GUIEDTException {
		forecastEndTimeParam = new DoubleParameter("Forecast End Time", 0d, 36500d, new Double(7d));
		forecastEndTimeParam.setUnits("Days");
		forecastEndTimeParam.setInfo("Forecast end relative to main shock origin time");
		register_param (forecastEndTimeParam, "forecastEndTimeParam", PARMGRP_FCAST_VALUE);
		return forecastEndTimeParam;
	}




	// Forecast parameter value dialog: initialize parameters within the dialog.

	private void init_fcValueDialogParam () throws GUIEDTException {

		init_forecastStartTimeNowParam();
		
		init_forecastStartTimeParam();
		
		init_forecastEndTimeParam();

		return;
	}




	// Forecast parameter edit: button to activate the dialog.

	private ParameterList fcValueList;			// List of parameters in the dialog
	private GUIParameterListParameter fcValueEditParam;
	

	private void updateFCValueParamList () throws GUIEDTException {
		fcValueList.clear();

		boolean f_button_row = false;

		fcValueEditParam.setListTitleText ("Forecast Parameters");
		fcValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(3, f_button_row));

		fcValueList.addParameter(forecastStartTimeNowParam);
		fcValueList.addParameter(forecastStartTimeParam);
		fcValueList.addParameter(forecastEndTimeParam);
		
		fcValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_fcValueEditParam () throws GUIEDTException {
		fcValueList = new ParameterList();
		fcValueEditParam = new GUIParameterListParameter("Forecast Parameters", fcValueList, "Forecast Params...",
							"Edit Forecast Params", "Forecast Parameters", null, null, false, gui_top.get_trace_events());
		fcValueEditParam.setInfo("Set forecast parameter values");
		register_param (fcValueEditParam, "fcValueEditParam", PARMGRP_FCAST_EDIT);

		updateFCValueParamList();

		return fcValueEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the RJ parameter value controls.

	private boolean f_fc_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(forecastStartTimeNowParam, f_fc_value_enable);
		enableParam(forecastStartTimeParam, f_fc_value_enable);
		enableParam(forecastEndTimeParam, f_fc_value_enable);

		enableParam(fcValueEditParam, f_sub_enable);

		// Parameters that are cleared when they are disabled

		//if (!( f_fc_value_enable )) {
		//}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferFCValueView {

		// Forecast start time, in days since the mainshock.

		public double x_forecastStartTimeParam;	// parameter value, checked for validity

		// Forecast start time, in days since the mainshock.

		public double x_forecastEndTimeParam;	// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferFCValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferFCValueImpl extends XferFCValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferFCValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferFCValueImpl () {
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
		public XferFCValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Forecast start and end time

			x_forecastStartTimeParam = validParam(forecastStartTimeParam);

			x_forecastEndTimeParam = validParam(forecastEndTimeParam);

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

	public OEGUISubForecast (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = false;
		f_fc_value_enable = false;

		// Create and initialize controls

		init_fcValueDialogParam();
		init_fcValueEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the forecast parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_fcValueEditParam () throws GUIEDTException {
		return fcValueEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_fc_value_enable (boolean f_sub_enable, boolean f_fc_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_fc_value_enable = f_fc_value_enable;
		adjust_enable();
		return;
	}


	// Make a forecast value transfer object.

	public XferFCValueImpl make_fc_value_xfer () {
		return new XferFCValueImpl();
	}


	// Private function, used to report that the forecast parameter values have changed.

	private void report_fc_value_change () throws GUIEDTException {
		gui_controller.notify_forecast_param_change();
		return;
	}


	// Update RJ values from model.

	public void update_fc_value_from_model () throws GUIEDTException {
		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Forecast parameter edit button.
		// - Do nothing.

		case PARMGRP_FCAST_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Forecast parameter value.
		// - Report to top-level controller.

		case PARMGRP_FCAST_VALUE: {
			if (!( f_sub_enable && f_fc_value_enable )) {
				return;
			}
			report_fc_value_change();
		}
		break;




		// Button to set forecast time to now.
		// - Dump any forecast that has been computed.
		// - Update forecast start time to now.
		// - Update forecast end time to preserve the delta.

		case PARMGRP_NOW_BUTTON: {
			if (!( f_sub_enable && f_fc_value_enable )) {
				return;
			}
			report_fc_value_change();

			SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
			Instant now = Instant.now();
			System.out.println("Computing delta from mainshock time ("
					+df.format(new Date (gui_model.get_cur_mainshock().getOriginTime()))+") to now ("+df.format(Date.from(now))+")");
			double delta = USGS_AftershockForecast.getDateDelta(Instant.ofEpochMilli(gui_model.get_cur_mainshock().getOriginTime()), now);
			System.out.println("Delta: "+delta+" days");
			double prevDiff = validParam(forecastEndTimeParam) - validParam(forecastStartTimeParam);
			if (prevDiff <= 0.0) {
				prevDiff = 7.0;
			}
			updateParam(forecastStartTimeParam, delta);
			updateParam(forecastEndTimeParam, delta+prevDiff);
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubForecast: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
