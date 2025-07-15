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
import org.opensha.oaf.util.gui.GUISeparatorParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.opensha.oaf.oetas.OEConstants;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for values common to RJ and ETAS.
// Michael Barall 09/04/2021
//
// This is a modeless dialog for entering common parameter values.


public class OEGUISubCommonValue extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_COM_EDIT = 601;		// Button to open the common parameter edit dialog
	private static final int PARMGRP_COM_VALUE = 602;		// Common parameter value change
//	private static final int PARMGRP_TIME_DEP_MC = 603;		// Checkbox for time-dependent magnitude of completeness
	private static final int PARMGRP_SEQ_SPEC_MC = 604;		// Sequence-specific Mc value
	private static final int PARMGRP_MAG_PREC = 605;		// Magnitude precision value
	private static final int PARMGRP_COMPUTE_B = 606;		// Button to compute sequence-specific b-value
	private static final int PARMGRP_B_VALUE = 607;			// b-value
	private static final int PARMGRP_TIME_DEP_OPTION = 608;	// Option for time-dependent magnitude of completeness
	private static final int PARMGRP_ALPHA_EQUALS_B = 609;	// Option to take alpha == b




	//----- Controls for the common parameter dialog -----


//	// Option to apply time-dependent Mc; check box.
//	// Default is obtained from generic parameters when the data is loaded (typically true).
//
//	private BooleanParameter timeDepMcParam;
//
//	private BooleanParameter init_timeDepMcParam () throws GUIEDTException {
//		timeDepMcParam = new BooleanParameter("Apply time dep. Mc", true);
//		register_param (timeDepMcParam, "timeDepMcParam", PARMGRP_TIME_DEP_MC);
//		return timeDepMcParam;
//	}


	// Option to apply time-dependent Mc; dropdown containing an enumeration to select option.

	private EnumParameter<TimeDepMagCompOption> timeDepOptionParam;

	private EnumParameter<TimeDepMagCompOption> init_timeDepOptionParam () throws GUIEDTException {
		timeDepOptionParam = new EnumParameter<TimeDepMagCompOption>(
				"Apply time-dependent Mc", EnumSet.allOf(TimeDepMagCompOption.class), TimeDepMagCompOption.ENABLE, null);
		timeDepOptionParam.setInfo("Selects options for time-dependent magnitude of completeness");
		register_param (timeDepOptionParam, "timeDepOptionParam", PARMGRP_TIME_DEP_OPTION);
		return timeDepOptionParam;
	}


	// Helmstetter F parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter fParam;

	private DoubleParameter init_fParam () throws GUIEDTException {
		fParam = new DoubleParameter("F", 0.0, 2.0, Double.valueOf(0.5));
		fParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (fParam, "fParam", PARMGRP_COM_VALUE);
		return fParam;
	}


	// Helmstetter G parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter gParam;

	private DoubleParameter init_gParam () throws GUIEDTException {
		gParam = new DoubleParameter("G", -10.0, 100.0, Double.valueOf(0.25));
		gParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (gParam, "gParam", PARMGRP_COM_VALUE);
		return gParam;
	}


	// Helmstetter H parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter hParam;

	private DoubleParameter init_hParam () throws GUIEDTException {
		hParam = new DoubleParameter("H", 0.0, 10.0, Double.valueOf(1.0));
		hParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (hParam, "hParam", PARMGRP_COM_VALUE);
		return hParam;
	}


	// Magnitude of completeness for time-dependent Mc; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter mCatParam;

	private DoubleParameter init_mCatParam () throws GUIEDTException {
		mCatParam = new DoubleParameter("Mcat", 1.0, 7.0, Double.valueOf(4.5));
		mCatParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (mCatParam, "mCatParam", PARMGRP_COM_VALUE);
		return mCatParam;
	}


	// Mc for sequence; edit box containing a number.
	// The default is obtained by putting the aftershocks in magnitude bins;
	// Mmaxc is the magnitude bin that contains the largest count of aftershocks;
	// the default Mc is Mmaxc+0.5.  User can edit the field to override the default.

	private DoubleParameter mcParam;

	private DoubleParameter init_mcParam () throws GUIEDTException {
		mcParam = new DoubleParameter("Mc For Sequence", 0d, 9d);
		mcParam.getConstraint().setNullAllowed(true);
		mcParam.setInfo("Default is computed value of Mmaxc+0.5, but user can modify");
		register_param (mcParam, "mcParam", PARMGRP_SEQ_SPEC_MC);
		return mcParam;
	}


	// Precision of magnitudes in the catalog; edit box containing a number.
	// Used in the algorithm to estimate the b-value.  Defaults to 0.1.

	private DoubleParameter magPrecisionParam;

	private DoubleParameter init_magPrecisionParam () throws GUIEDTException {
		magPrecisionParam = new DoubleParameter("Mag Precision", 0d, 1d, Double.valueOf(0.1));
		magPrecisionParam.setInfo("Magnitude rounding applied by network, for estimation of b-value");;
		register_param (magPrecisionParam, "magPrecisionParam", PARMGRP_MAG_PREC);
		return magPrecisionParam;
	}


	// Compute b-value button.

	private ButtonParameter computeBButton;

	private ButtonParameter init_computeBButton () throws GUIEDTException {
		computeBButton = new ButtonParameter("Sequence-specific GR b-value", "Compute b (optional)");
		register_param (computeBButton, "computeBButton", PARMGRP_COMPUTE_B);
		return computeBButton;
	}


	// Gutenberg-Richter b-value; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.
	// The value can be null (obtained by clearing the edit box), which produces
	// a mag-freq plot without a b-value comparison, but cannot be used when
	// fitting aftershock parameters.

	private DoubleParameter bParam;

	private DoubleParameter init_bParam () throws GUIEDTException {
		bParam = new DoubleParameter("b-value", 1d);	// can't use DoubleParameter("b-value", null) because the call would be ambiguous
		bParam.setValue(null);
		bParam.setInfo("Default b-value is from the configured parameters, but user can modify");
		register_param (bParam, "bParam", PARMGRP_B_VALUE);
		return bParam;
	}


	// Option to use alpha == b.

	private BooleanParameter alphaEqualsBParam;

	private BooleanParameter init_alphaEqualsBParam () throws GUIEDTException {
		alphaEqualsBParam = new BooleanParameter("Use alpha = b", true);
		register_param (alphaEqualsBParam, "alphaEqualsBParam", PARMGRP_ALPHA_EQUALS_B);
		return alphaEqualsBParam;
	}


	// ETAS alpha parameter; edit box containing a number.

	private DoubleParameter alphaParam;

	private DoubleParameter init_alphaParam () throws GUIEDTException {
		alphaParam = new DoubleParameter("alpha", 0.0, 3.0, Double.valueOf(1.0));
		alphaParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		alphaParam.setValue(null);
		register_param (alphaParam, "alphaParam", PARMGRP_COM_VALUE);
		return alphaParam;
	}




	// Common parameter value dialog: initialize parameters within the dialog.

	private void init_commonValueDialogParam () throws GUIEDTException {
		
		init_timeDepOptionParam();
		
		init_fParam();
		
		init_gParam();
		
		init_hParam();
		
		init_mCatParam();
		
		init_mcParam();
		
		init_magPrecisionParam();
		
		init_computeBButton();
		
		init_bParam();

		init_alphaEqualsBParam();

		init_alphaParam();

		return;
	}




	// Common parameter edit: button to activate the dialog.

	private ParameterList commonValueList;			// List of parameters in the dialog
	private GUIParameterListParameter commonValueEditParam;
	

	private void updateCommonValueParamList () throws GUIEDTException {
		commonValueList.clear();

		boolean f_button_row = false;

		commonValueEditParam.setListTitleText ("Common Parameters");
		commonValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(11, f_button_row, 3));

		commonValueList.addParameter(timeDepOptionParam);
		commonValueList.addParameter(fParam);
		commonValueList.addParameter(gParam);
		commonValueList.addParameter(hParam);
		commonValueList.addParameter(mCatParam);

		commonValueList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

		commonValueList.addParameter(mcParam);

		commonValueList.addParameter(new GUISeparatorParameter("Separator2", gui_top.get_separator_color()));

		commonValueList.addParameter(magPrecisionParam);
		commonValueList.addParameter(computeBButton);
		commonValueList.addParameter(bParam);

		commonValueList.addParameter(new GUISeparatorParameter("Separator3", gui_top.get_separator_color()));

		commonValueList.addParameter(alphaEqualsBParam);
		commonValueList.addParameter(alphaParam);
		
		commonValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_commonValueEditParam () throws GUIEDTException {
		commonValueList = new ParameterList();
		commonValueEditParam = new GUIParameterListParameter("Common Parameters", commonValueList, "Common Params...",
							"Edit Common Params", "Common Parameters", null, null, false, gui_top.get_trace_events());
		commonValueEditParam.setInfo("Set common parameter values");
		register_param (commonValueEditParam, "commonValueEditParam", PARMGRP_COM_EDIT);

		updateCommonValueParamList();

		return commonValueEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the common parameter value controls.

	private boolean f_common_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		// Get flag indicating if alpha == b is forced

		boolean f_alpha_eq_b = true;
		if (f_common_value_enable) {
			f_alpha_eq_b = validParam(alphaEqualsBParam);
		}

		// Enable parameters

		enableParam(timeDepOptionParam, f_common_value_enable);

		if (f_common_value_enable) {
			adjust_for_time_dep_option();
		} else {
			enableParam(fParam, f_common_value_enable);
			enableParam(gParam, f_common_value_enable);
			enableParam(hParam, f_common_value_enable);
			enableParam(mCatParam, f_common_value_enable);
		}

		enableParam(mcParam, f_common_value_enable);
		enableParam(magPrecisionParam, f_common_value_enable);
		enableParam(computeBButton, f_common_value_enable);
		enableParam(bParam, f_common_value_enable);

		enableParam(alphaEqualsBParam, f_common_value_enable);
		enableParam(alphaParam, !f_alpha_eq_b);

		enableParam(commonValueEditParam, f_sub_enable);

		// Parameters that are cleared when they are disabled

		if (!( f_common_value_enable )) {
			updateParam(timeDepOptionParam, TimeDepMagCompOption.ENABLE);

			updateParam(fParam, null);
			updateParam(gParam, null);
			updateParam(hParam, null);
			updateParam(mCatParam, null);

			updateParam(mcParam, null);
			updateParam(bParam, null);

			updateParam(alphaEqualsBParam, true);

			updateParam(alphaParam, null);
		}
		else {
			if (!( f_alpha_eq_b )) {
				if (!( definedParam(alphaParam) )) {
					updateParam(alphaParam, 1.0);
				}
			}
		}

		return;
	}


	// Adjust enable/disable and values for the current time-dependent Mc option.
	// Parameters for F, G, H which are disabled also have values set.

	private void adjust_for_time_dep_option () throws GUIEDTException {

		TimeDepMagCompOption time_dep_option = validParam(timeDepOptionParam);
		switch (time_dep_option) {

		case ENABLE:
			enableParam(fParam, true);
			enableParam(gParam, true);
			enableParam(hParam, true);
			enableParam(mCatParam, true);
			break;

		case WORLD:
			enableParam(fParam, false);
			enableParam(gParam, false);
			enableParam(hParam, false);
			enableParam(mCatParam, true);

			updateParam(fParam, OEConstants.helm_capF (OEConstants.HELM_PARAM_WORLD));
			updateParam(gParam, OEConstants.helm_capG (OEConstants.HELM_PARAM_WORLD));
			updateParam(hParam, OEConstants.helm_capH (OEConstants.HELM_PARAM_WORLD));
			break;

		case CALIFORNIA:
			enableParam(fParam, false);
			enableParam(gParam, false);
			enableParam(hParam, false);
			enableParam(mCatParam, true);

			updateParam(fParam, OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL));
			updateParam(gParam, OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL));
			updateParam(hParam, OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL));
			break;

		case EQUALS_MCAT:
			enableParam(fParam, false);
			enableParam(gParam, false);
			enableParam(hParam, false);
			enableParam(mCatParam, true);

			updateParam(fParam, OEConstants.helm_capF (OEConstants.HELM_PARAM_NONE));
			updateParam(gParam, OEConstants.helm_capG (OEConstants.HELM_PARAM_NONE));
			updateParam(hParam, OEConstants.helm_capH (OEConstants.HELM_PARAM_NONE));
			break;

		case EQUALS_MC:
			enableParam(fParam, false);
			enableParam(gParam, false);
			enableParam(hParam, false);
			enableParam(mCatParam, false);

			updateParam(fParam, OEConstants.helm_capF (OEConstants.HELM_PARAM_NONE));
			updateParam(gParam, OEConstants.helm_capG (OEConstants.HELM_PARAM_NONE));
			updateParam(hParam, OEConstants.helm_capH (OEConstants.HELM_PARAM_NONE));
			break;

		default:
			throw new IllegalArgumentException ("OEGUISubCommonValue.adjust_for_time_dep_option: Invalid magnitude of completeness option");
		}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferCommonValueView {

		// Mc for sequence (cannot be null at this stage).

		public double x_mcParam;				// parameter value, checked for validity

		// b-value (cannot be null at this stage).

		public double x_bParam;					// parameter value, checked for validity

		// Option to apply time-dependent Mc.

		public TimeDepMagCompOption x_timeDepOptionParam;	// parameter value, checked for validity

		// Helmstetter F parameter, present if using time-dependent Mc.

		public double x_fParam;					// parameter value, checked for validity

		// Helmstetter G parameter, present if using time-dependent Mc.

		public double x_gParam;					// parameter value, checked for validity

		// Helmstetter H parameter, present if using time-dependent Mc.

		public double x_hParam;					// parameter value, checked for validity

		// Magnitude of completeness, present for both time-independent and time-dependent Mc.

		public double x_mCatParam;				// parameter value, checked for validity

		// Flag indicating if alpha == b.

		public boolean x_alphaEqualsBParam;		// parameter value, checked for validity

		// Value of alpha, present if x_alphaEqualsBParam is false.

		public double x_alphaParam;				// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferCommonValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferCommonValueImpl extends XferCommonValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferCommonValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferCommonValueImpl () {
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
		public XferCommonValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Mc for sequence

			x_mcParam = validParam(mcParam);

			// b-value

			x_bParam = validParam(bParam);

			// Option for time-dependent Mc

			x_timeDepOptionParam = validParam(timeDepOptionParam);

			// Parameters for time-dependent Mc

			switch (x_timeDepOptionParam) {

			case ENABLE:
				x_fParam = validParam(fParam);
				x_gParam = validParam(gParam);
				x_hParam = validParam(hParam);
				x_mCatParam = validParam(mCatParam);
				break;

			case WORLD:
				x_fParam = OEConstants.helm_capF (OEConstants.HELM_PARAM_WORLD);
				x_gParam = OEConstants.helm_capG (OEConstants.HELM_PARAM_WORLD);
				x_hParam = OEConstants.helm_capH (OEConstants.HELM_PARAM_WORLD);
				x_mCatParam = validParam(mCatParam);
				break;

			case CALIFORNIA:
				x_fParam = OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL);
				x_gParam = OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL);
				x_hParam = OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL);
				x_mCatParam = validParam(mCatParam);
				break;

			case EQUALS_MCAT:
				x_fParam = OEConstants.helm_capF (OEConstants.HELM_PARAM_NONE);
				x_gParam = OEConstants.helm_capG (OEConstants.HELM_PARAM_NONE);
				x_hParam = OEConstants.helm_capH (OEConstants.HELM_PARAM_NONE);
				x_mCatParam = validParam(mCatParam);
				break;

			case EQUALS_MC:
				x_fParam = OEConstants.helm_capF (OEConstants.HELM_PARAM_NONE);
				x_gParam = OEConstants.helm_capG (OEConstants.HELM_PARAM_NONE);
				x_hParam = OEConstants.helm_capH (OEConstants.HELM_PARAM_NONE);
				x_mCatParam = validParam(mcParam);
				break;

			default:
				throw new IllegalArgumentException ("OEGUISubCommonValue.XferCommonValueImpl.xfer_load: Invalid magnitude of completeness option");
			}

			// Flag indicating if alpha == b.

			x_alphaEqualsBParam = validParam(alphaEqualsBParam);

			// Value of alpha, present if x_alphaEqualsBParam is false.

			if (!( x_alphaEqualsBParam )) {
				x_alphaParam = validParam(alphaParam);
			}

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// If we're forcing alpha == b, then update alpha with b-value from the model

			if (x_alphaEqualsBParam) {
				updateParam(alphaParam, gui_model.get_cat_bParam());
			}

			return;
		}
	}




	//----- Parameter transfer for b-value computation -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferBValueCompView {

		// Mc for sequence.

		public double x_mcParam;			// parameter value, checked for validity

		// Magnitude precision.

		public double x_magPrecisionParam;	// parameter value, checked for validity

		// b-value, can be null.

		public Double x_bParam;				// parameter value, checked for null or validity

		public abstract void modify_bParam (Double x);

		// Get the implementation class.

		public abstract XferBValueCompImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferBValueCompImpl extends XferBValueCompView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferBValueCompImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferBValueCompImpl () {
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
		public XferBValueCompImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Mc for sequence.

			x_mcParam = validParam(mcParam);

			// Magnitude precision.
					
			x_magPrecisionParam = validParam(magPrecisionParam);

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

	public OEGUISubCommonValue (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = false;
		f_common_value_enable = false;

		// Create and initialize controls

		init_commonValueDialogParam();
		init_commonValueEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the common parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_commonValueEditParam () throws GUIEDTException {
		return commonValueEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_common_value_enable (boolean f_sub_enable, boolean f_common_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_common_value_enable = f_common_value_enable;
		adjust_enable();
		return;
	}


	// Make a common value transfer object.

	public XferCommonValueImpl make_common_value_xfer () {
		return new XferCommonValueImpl();
	}


	// Private function, used to report that the common parameter values have changed.

	private void report_common_value_change () throws GUIEDTException {
		gui_controller.notify_aftershock_param_change();
		return;
	}


	// Update common values from model.

	public void update_common_value_from_model () throws GUIEDTException {
		
		updateParam(mcParam, gui_model.get_cat_mcParam());
		updateParam(bParam, gui_model.get_cat_bParam());

		// Check for constant Mc

		if (gui_model.get_magCompParams().get_magCompFn().is_constant()) {
			updateParam(timeDepOptionParam, TimeDepMagCompOption.EQUALS_MCAT);
		}

		// Otherwise, check for the world and California special values

		else {
			double f = gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapF();
			double g = gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapG();
			double h = gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapH();

			if (
				   Math.abs (f - OEConstants.helm_capF (OEConstants.HELM_PARAM_WORLD)) <= 0.001
				&& Math.abs (g - OEConstants.helm_capG (OEConstants.HELM_PARAM_WORLD)) <= 0.001
				&& Math.abs (h - OEConstants.helm_capH (OEConstants.HELM_PARAM_WORLD)) <= 0.001
			) {
				updateParam(timeDepOptionParam, TimeDepMagCompOption.WORLD);
			}

			else if (
				   Math.abs (f - OEConstants.helm_capF (OEConstants.HELM_PARAM_CAL)) <= 0.001
				&& Math.abs (g - OEConstants.helm_capG (OEConstants.HELM_PARAM_CAL)) <= 0.001
				&& Math.abs (h - OEConstants.helm_capH (OEConstants.HELM_PARAM_CAL)) <= 0.001
			) {
				updateParam(timeDepOptionParam, TimeDepMagCompOption.CALIFORNIA);
			}

			else {
				updateParam(timeDepOptionParam, TimeDepMagCompOption.ENABLE);
				updateParam(fParam, f);
				updateParam(gParam, g);
				updateParam(hParam, h);
			}
		}

		// The magnitude of completeness

		updateParam(mCatParam, gui_model.get_magCompParams().get_magCat());

		// Alpha [TODO: Read from ETAS parameters]

		updateParam(alphaEqualsBParam, true);
		updateParam(alphaParam, 1.0);

		// Need to adjust enable to pick up enable state corresponding to time-dependent Mc option

		adjust_enable();
		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Common parameter edit button.
		// - Do nothing.

		case PARMGRP_COM_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Common parameter value.
		// - Report to top-level controller.

		case PARMGRP_COM_VALUE: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			report_common_value_change();
		}
		break;



		// Magnitude of completeness.
		// - Dump any aftershock parameters that have been computed.
		// - Set b-parameter to default from generic model, or unspecified if none.
		// - If b-value was changed, re-plot the magnitude-frequency distribution.
		// - Re-plot the cumulative number plot (which only show earthquakes above Mc).

		case PARMGRP_SEQ_SPEC_MC: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			if (definedParam(mcParam)) {
				gui_model.change_mcParam(validParam(mcParam));
			}
			report_common_value_change();
		}
		break;




		// Precision of magnitudes in the catalog.
		// - Dump any aftershock parameters that have been computed.
		// - Set b-parameter to default from generic model, or unspecified if none.
		// - If b-value was changed, re-plot the magnitude-frequency distribution.

		case PARMGRP_MAG_PREC: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			report_common_value_change();
		}
		break;




		// Compute b-value button.
		// - Dump any aftershock parameters that have been computed.
		// - In backgound:
		//   1. Compute maximum likelhood b-value, for aftershocks above Mc with specified precision.
		//   2. Re-plot the magnitude-frequency distribution, and set it as the selected tab.

		case PARMGRP_COMPUTE_B: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			report_common_value_change();

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferBValueCompImpl xfer_b_value_comp_impl = new XferBValueCompImpl();

			// Load the parameters

			if (!( gui_top.call_xfer_load (xfer_b_value_comp_impl, "Incorrect parameters for computing b-value") )) {
				return;
			}

			// Call model to compute b-value

			GUICalcStep bStep_1 = new GUICalcStep("Computing b", "...", new Runnable() {
				
				@Override
				public void run() {
					double mc = xfer_b_value_comp_impl.x_mcParam;
					
					double magPrecision = xfer_b_value_comp_impl.x_magPrecisionParam;
					
					ObsEqkRupList filteredRupList = gui_model.get_cur_aftershocks().getRupsAboveMag(mc);
					double b = AftershockStatsCalc.getMaxLikelihood_b_value(filteredRupList, mc, magPrecision);
					xfer_b_value_comp_impl.modify_bParam(b);

					System.out.println("Num rups \u2265 Mc = " + filteredRupList.size());
					System.out.println("Computed b-value: " + b);
				}
			});

			// Store back transfer parameters, re-plot and update state

			GUICalcStep bStep_2 = new GUICalcStep("Computing b", "...", new GUIEDTRunnable() {
				
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_b_value_comp_impl.xfer_store();

					if (definedParam(bParam)) {
						gui_model.change_bParam(validParam(bParam));
					}
				}
			});

			// Run in threads

			GUICalcRunnable.run_steps (progress, null, bStep_1, bStep_2);
		}
		break;




		// b-value.
		// - Dump any aftershock parameters that have been computed.
		// - Re-plot the magnitude-frequency distribution.

		case PARMGRP_B_VALUE: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			report_common_value_change();
			if (definedParam(bParam)) {
				gui_model.change_bParam(validParam(bParam));
			}
		}
		break;




//		// Use time-dependent magnitude of completeness option.
//		// - Dump any aftershock parameters that have been computed, and force control enable/disable to be adjusted.
//
//		case PARMGRP_TIME_DEP_MC: {
//			if (!( f_sub_enable && f_common_value_enable )) {
//				return;
//			}
//			adjust_enable();
//			report_common_value_change();
//		}
//		break;




		// Use time-dependent magnitude of completeness option.
		// - Dump any aftershock parameters that have been computed, and force control enable/disable to be adjusted.

		case PARMGRP_TIME_DEP_OPTION: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			adjust_enable();
			report_common_value_change();
		}
		break;




		// Alpha equal b selection.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_ALPHA_EQUALS_B: {
			if (!( f_sub_enable && f_common_value_enable )) {
				return;
			}
			adjust_enable();
			report_common_value_change();
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubCommonValue: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
