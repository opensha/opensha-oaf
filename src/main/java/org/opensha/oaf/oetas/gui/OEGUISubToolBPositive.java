package org.opensha.oaf.oetas.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;
import java.util.Comparator;

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
import org.opensha.commons.param.editor.AbstractParameterEditor;
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
import gov.usgs.earthquake.event.JsonEvent;
//import org.opensha.oaf.pdl.OAF_Publisher;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionWorld;
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
import org.opensha.oaf.util.gui.GUIParameterListParameterEditor;
import org.opensha.oaf.util.gui.GUIDialogParameter;
import org.opensha.oaf.util.gui.GUISeparatorParameter;
import org.opensha.oaf.util.gui.GUIPredicateStringParameter;
import org.opensha.oaf.util.gui.GUIDropdownParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.aafs.PDLSupport;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.PickerSphRegion;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatEventWebService;
import org.opensha.commons.data.comcat.ComcatVisitor;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for b-positive.
// Michael Barall
//
// This is a modal dialog for a tool to calculate b-value using b-positive.


public class OEGUISubToolBPositive extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_BPOSITIVE_VALUE = 1501;		// Computation input value
	private static final int PARMGRP_BPOSITIVE_DISPLAY = 1502;		// Text box to display computed value
	private static final int PARMGRP_BPOSITIVE_COMPUTE = 1503;		// Button to perform computation
	private static final int PARMGRP_BPOSITIVE_EDIT = 1504;			// The dialog activation button (not used)




	//----- Controls for the b-positive dialog -----


	// Minimum magnitude earthquakes to use in computation; edit box containing a number.

	private DoubleParameter minMagParam;

	private DoubleParameter init_minMagParam () throws GUIEDTException {
		minMagParam = new DoubleParameter("Min Magnitude", 0d, 9d);
		minMagParam.setInfo("Minimum magnitude of aftershocks to include in calculating the b-value");
		register_param (minMagParam, "minMagParam", PARMGRP_BPOSITIVE_VALUE);
		return minMagParam;
	}


	// Minimum magnitude increase between successive aftershocks; edit box containing a number.
	// Used in the algorithm to estimate the b-value.  Defaults to 0.1.

	private DoubleParameter minMagIncrease;

	private DoubleParameter init_minMagIncrease () throws GUIEDTException {
		minMagIncrease = new DoubleParameter("Min Magnitude Increase", 0d, 1d, Double.valueOf(0.2));
		minMagIncrease.setInfo("Minimum magnitude increase between successive aftershocks, for inclusion in the calculation");;
		register_param (minMagIncrease, "minMagIncrease", PARMGRP_BPOSITIVE_VALUE);
		return minMagIncrease;
	}


	// Compute b-value button.

	private ButtonParameter computeBButton;

	private ButtonParameter init_computeBButton () throws GUIEDTException {
		computeBButton = new ButtonParameter("Compute b-positive", "Compute b");
		register_param (computeBButton, "computeBButton", PARMGRP_BPOSITIVE_COMPUTE);
		return computeBButton;
	}


	// Gutenberg-Richter b-value; edit box containing a number.
	// This is read-only.

	private DoubleParameter bParam;

	private DoubleParameter init_bParam () throws GUIEDTException {
		Double value = null;
		bParam = new DoubleParameter("b-value", value);
		bParam.setInfo("Displays the computed value of b-positive (read-only)");
		register_param (bParam, "bParam", PARMGRP_BPOSITIVE_DISPLAY);
		return bParam;
	}




	// b-positive dialog: initialize parameters within the dialog.

	private void init_bPositiveDialogParam () throws GUIEDTException {
		
		init_minMagParam();
		
		init_minMagIncrease();
		
		init_computeBButton();
		
		init_bParam();

		return;
	}




	// b-positive edit: button to activate the dialog.

	private ParameterList bPositiveList;			// List of parameters in the dialog
	private GUIParameterListParameter bPositiveEditParam;
	

	private void updatebPositiveParamList () throws GUIEDTException {
		bPositiveList.clear();

		boolean f_button_row = true;

		bPositiveList.addParameter(minMagParam);
		bPositiveList.addParameter(minMagIncrease);
		bPositiveList.addParameter(computeBButton);

		bPositiveList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

		bPositiveList.addParameter(bParam);

		bPositiveEditParam.setListTitleText ("Compute b-value using b-positive");
		bPositiveEditParam.setDialogDimensions (gui_top.get_dialog_dims_wide(4, f_button_row, 1, 1.0));
		
		bPositiveEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_bPositiveEditParam () throws GUIEDTException {
		bPositiveList = new ParameterList();

		bPositiveEditParam = new GUIParameterListParameter("b-positive value", bPositiveList, "Compute b-positive...",
							"Compute b-positive", "Compute b-positive", "Select", "Cancel", true, gui_top.get_trace_events(),
							gui_top.make_help_modal ("help_cptool_b_positive.html"));
		bPositiveEditParam.setInfo("Compute the value of b-positive");
		bPositiveEditParam.setOkButtonEnabled (false);
		
		register_param (bPositiveEditParam, "bPositiveEditParam", PARMGRP_BPOSITIVE_EDIT);

		updatebPositiveParamList();

		return bPositiveEditParam;
	}




	//----- Dialog state -----




	// True if we have a computed b-value.

	private boolean f_has_computed_b;

	public boolean get_f_has_computed_b () {
		return f_has_computed_b;
	}




	// The computed b-value.

	private double computed_b;

	public double get_computed_b () {
		return computed_b;
	}



	// Clear the computed b-value.

	private void clear_computed_b () {
		f_has_computed_b = false;
		computed_b = 0;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable event picker controls.

	private boolean f_b_positive_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(minMagParam, f_b_positive_enable);
		enableParam(minMagIncrease, f_b_positive_enable);
		enableParam(computeBButton, f_b_positive_enable);

		// The b-value display

		if (f_has_computed_b && f_b_positive_enable) {
			updateParam(bParam, computed_b);
		} else {
			updateParam(bParam, null);
		}
		enableParam(bParam, false);

		// The Select button

		if (f_has_computed_b && f_b_positive_enable) {
			bPositiveEditParam.setOkButtonText ("Select  " + computed_b);
			bPositiveEditParam.setOkButtonEnabled (true);
		} else {
			bPositiveEditParam.setOkButtonText ("Select");
			bPositiveEditParam.setOkButtonEnabled (false);
		}
		bPositiveEditParam.refreshButtons();

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferBPositiveCompView {

		// Minimum magnitude earthquakes to use in computation.

		public double x_minMagParam;			// parameter value, checked for validity

		// Minimum magnitude increase between successive aftershocks.

		public double x_minMagIncrease;	// parameter value, checked for validity

		// b-value, can be null.

		public Double x_bParam;				// parameter value, checked for null or validity

		public abstract void modify_bParam (Double x);

		// Get the implementation class.

		public abstract XferBPositiveCompImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferBPositiveCompImpl extends XferBPositiveCompView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferBPositiveCompImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferBPositiveCompImpl () {
			internal_clean();
		}


		// b-value, can be null.

		private boolean dirty_bParam;		// true if needs to be written back

		@Override
		public void modify_bParam (Double x) {
			x_bParam = x;
			dirty_bParam = true;
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_bParam = false;
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			return;
		}


		// Load values.

		@Override
		public XferBPositiveCompImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Minimum magnitude earthquakes to use in computation.

			x_minMagParam = validParam(minMagParam);

			// Minimum magnitude increase between successive aftershocks.
					
			x_minMagIncrease = validParam(minMagIncrease);

			// b-value

			x_bParam = null;
			if (nonNullParam(bParam)) {
				x_bParam = validParam(bParam);
			}

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// b-value

			if (dirty_bParam) {
				dirty_bParam = false;
				updateParam(bParam, x_bParam);
			}

			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubToolBPositive (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default dialog state

		f_has_computed_b = false;
		computed_b = 0.0;

		// Default enable state

		f_sub_enable = true;
		f_b_positive_enable = false;

		// Create and initialize controls

		init_bPositiveDialogParam();
		init_bPositiveEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Enable or disable the sub-controller.

	public void sub_b_positive_enable (boolean f_sub_enable, boolean f_b_positive_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_b_positive_enable = f_b_positive_enable;
		adjust_enable();
		return;
	}


	// Make a b-poairicw transfer object.

	public XferBPositiveCompImpl make_b_positive_xfer () {
		return new XferBPositiveCompImpl();
	}


	// Private function, used to report that the b-positive options have changed.

	private void report_b_positive_change () throws GUIEDTException {
		//gui_controller.notify_b_positive_option_change();
		return;
	}


	// Update b-positive values from model.

	//public void update_b_positive_from_model () throws GUIEDTException {
	//	return;
	//}


	// Open the dialog.
	// Parameters:
	//  owner_param = Parameter of the dialog's owner, typically the button that opens the dialog.
	//  initial_min_mag = Initial value for minimum magnitude to use in the computation.
	// Returns true if the user exited by pressing OK.

	public boolean open_b_positive_dialog (Parameter<?> owner_param, double initial_min_mag) throws GUIEDTException {

		// Initialize dialog state

		clear_computed_b();

		// Initialize minimum magnitude

		updateParam(minMagParam, initial_min_mag);

		// Initialize enable

		sub_b_positive_enable (true, true);

		// Set owner component for the dialog

		bPositiveEditParam.setOwnerParameter (owner_param);

		// Open the dialog

		bPositiveEditParam.openDialog();

		// If user pressed OK, return it

		if (bPositiveEditParam.isDialogTermCodeOK()) {
			return true;
		}

		// Otherwise, erase any computed b-value

		clear_computed_b();

		adjust_enable();
		return false;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// b-positive option changed.
		// - Clear the computed b-value.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_BPOSITIVE_VALUE: {
			if (!( f_sub_enable && f_b_positive_enable )) {
				return;
			}
			clear_computed_b();
			adjust_enable();
			report_b_positive_change();
		}
		break;




		// Change notification from b-value display, do nothing.

		case PARMGRP_BPOSITIVE_DISPLAY:
		break;




		// Compute b-value button.
		// - Dump any computed b-value.
		// - Adjust enable.
		// - In backgound:
		//   1. Compute b-positive, for aftershocks above min magnitude with specified min increase.
		//   2. If success, save b-value for return to caller, and enable OK button.

		case PARMGRP_BPOSITIVE_COMPUTE: {
			if (!( f_sub_enable && f_b_positive_enable )) {
				return;
			}

			clear_computed_b();
			adjust_enable();

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferBPositiveCompImpl xfer_b_value_comp_impl = new XferBPositiveCompImpl();

			// Load the parameters

			if (!( gui_top.call_xfer_load (xfer_b_value_comp_impl, "Incorrect parameters for computing b-value") )) {
				return;
			}

			// Call model to compute b-value

			GUICalcStep bStep_1 = new GUICalcStep("Computing b", "...", new Runnable() {
				
				@Override
				public void run() {
					double min_mag = xfer_b_value_comp_impl.x_minMagParam;
					
					double min_mag_diff = xfer_b_value_comp_impl.x_minMagIncrease;

					int min_num_diff = 10;

					long mainshock_time = gui_model.get_cur_mainshock().getOriginTime();

					ObsEqkRupList filteredRupList = new ObsEqkRupList();
					for (ObsEqkRupture rup : gui_model.get_cur_aftershocks()) {	// list is already sorted by time
						if (rup.getOriginTime() > mainshock_time && rup.getMag() >= min_mag) {	// foreshock fix
							filteredRupList.add (rup);
						}
					}

					double b;

					try {
						b = AftershockStatsCalc.calc_b_positive (filteredRupList, min_mag_diff, min_num_diff);
					}
					catch (Exception e) {
						throw new RuntimeException ("Insufficient data to calculate b-value", e);
					}

					if (!( b >= 0.25 && b <= 4.0 )) {
						throw new RuntimeException ("Calculated b-value is out-of-range, will not be used");
					}

					b = SimpleUtils.round_double_via_string ("%.3f", b);
					xfer_b_value_comp_impl.modify_bParam(b);

					computed_b = b;
					f_has_computed_b = true;

					System.out.println("Number of aftershocks used in the computation = " + filteredRupList.size());
					System.out.println("Computed b-value: " + b);
				}
			});

			// Store back transfer parameters, adjust enable

			GUICalcStep bStep_2 = new GUICalcStep("Computing b", "...", new GUIEDTRunnable() {
				
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_b_value_comp_impl.xfer_store();
					adjust_enable();
					report_b_positive_change();
				}
			});

			// Run in threads

			GUICalcRunnable.run_steps (progress, null, bStep_1, bStep_2);
		}
		break;




		// b-positive edit button.
		// - Do nothing.

		case PARMGRP_BPOSITIVE_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubToolBPositive: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
