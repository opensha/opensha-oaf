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
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEDiscreteRangeLogSkew;
import org.opensha.oaf.oetas.env.OEtasParameters;
import org.opensha.oaf.oetas.bay.OEBayFactoryMixedRNPC;
import org.opensha.oaf.oetas.bay.OEBayFactoryGaussAPC;
import org.opensha.oaf.oetas.bay.OEBayFactoryUniform;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for ETAS parameter values.
// Michael Barall 09/06/2021
//
// This is a modeless dialog for entering ETAS parameter values.


public class OEGUISubETASValue extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_ETAS_EDIT = 801;			// Button to open the ETAS parameter edit dialog
	private static final int PARMGRP_ETAS_VALUE = 802;			// ETAS parameter value
	private static final int PARMGRP_RANGE_ETAS_N = 804;		// ETAS parameter range n
	private static final int PARMGRP_RANGE_ETAS_P = 805;		// ETAS parameter range p
	private static final int PARMGRP_RANGE_ETAS_C = 806;		// ETAS parameter range c
	private static final int PARMGRP_ALPHA_EQUALS_B = 807;		// Option to take alpha == b
	private static final int PARMGRP_RANGE_ETAS_ZAMS = 808;		// ETAS parameter range zmas
	private static final int PARMGRP_RANGE_ETAS_ZMU = 809;		// ETAS parameter range zmu
	private static final int PARMGRP_ETAS_ENABLE = 810;			// ETAS forecast enable dropdown
	private static final int PARMGRP_ETAS_OPTION = 811;			// Button to open the ETAS parameter option dialog
	private static final int PARMGRP_ETAS_MODEL = 812;			// Option to select model
	private static final int PARMGRP_ETAS_PRIOR = 813;			// Option to select prior
	private static final int PARMGRP_USE_COMMON_B = 814;		// Option to use common b-value (same as RJ)




	// Default ranges for ETAS parameters.

	private int def_n_size;
	private double def_n_min;
	private double def_n_max;

	private double def_n_skew;

	private int def_p_size;
	private double def_p_min;
	private double def_p_max;

	private int def_c_size;
	private double def_c_min;
	private double def_c_max;

	private int def_zams_size;
	private double def_zams_min;
	private double def_zams_max;

	private boolean def_zams_relative;

	private int def_zmu_size;
	private double def_zmu_min;
	private double def_zmu_max;

	private boolean def_f_alpha_eq_b;
	private double def_alpha_value;

	private boolean def_f_use_common_b;
	private double def_b_value;

	// Initialize the default ranges using the defaults in OEConstants.

	private void init_default_ranges () {

		OEDiscreteRange def_n_range = OEConstants.def_n_range();
		def_n_size = def_n_range.get_range_size();
		def_n_min = def_n_range.get_range_min();
		def_n_max = def_n_range.get_range_max();

		if (def_n_range instanceof OEDiscreteRangeLogSkew) {
			def_n_skew = ((OEDiscreteRangeLogSkew)def_n_range).get_range_skew();
		} else {
			def_n_skew = 1.0;
		}

		OEDiscreteRange def_p_range = OEConstants.def_p_range();
		def_p_size = def_p_range.get_range_size();
		def_p_min = def_p_range.get_range_min();
		def_p_max = def_p_range.get_range_max();

		OEDiscreteRange def_c_range = OEConstants.def_c_range();
		def_c_size = def_c_range.get_range_size();
		def_c_min = def_c_range.get_range_min();
		def_c_max = def_c_range.get_range_max();

		OEDiscreteRange def_zams_range = OEConstants.def_zams_range();
		def_zams_size = def_zams_range.get_range_size();
		def_zams_min = def_zams_range.get_range_min();
		def_zams_max = def_zams_range.get_range_max();

		def_zams_relative = OEConstants.def_relative_zams();

		OEDiscreteRange def_zmu_range = OEConstants.def_zmu_range();
		if (def_zmu_range == null) {
			def_zmu_size = 1;
			def_zmu_min = 0.0;
			def_zmu_max = 0.0;
		} else {
			def_zmu_size = def_zmu_range.get_range_size();
			def_zmu_min = def_zmu_range.get_range_min();
			def_zmu_max = def_zmu_range.get_range_max();
		}

		OEDiscreteRange def_alpha_range = OEConstants.def_alpha_range();
		if (def_alpha_range == null) {
			def_f_alpha_eq_b = true;
			def_alpha_value = 1.0;
		} else {
			def_f_alpha_eq_b = false;
			def_alpha_value = def_alpha_range.get_range_middle();
		}

		OEDiscreteRange def_b_range = OEConstants.def_b_range();
		def_f_use_common_b = true;
		def_b_value = def_b_range.get_range_middle();

		return;
	}

	// Update the default ranges from ETAS parameters.
	// Use defaults if etas_params is null or ranges are not available.

	private void update_default_ranges (OEtasParameters etas_params) {

		if (!( etas_params != null && etas_params.range_avail && !(etas_params.eligible_params_avail && etas_params.eligible_option == OEConstants.ELIGIBLE_OPT_DISABLE) )) {
			init_default_ranges();
			return;
		}

		OEDiscreteRange def_n_range = etas_params.n_range;
		def_n_size = def_n_range.get_range_size();
		def_n_min = def_n_range.get_range_min();
		def_n_max = def_n_range.get_range_max();

		if (def_n_range instanceof OEDiscreteRangeLogSkew) {
			def_n_skew = ((OEDiscreteRangeLogSkew)def_n_range).get_range_skew();
		} else {
			def_n_skew = 1.0;
		}

		OEDiscreteRange def_p_range = etas_params.p_range;
		def_p_size = def_p_range.get_range_size();
		def_p_min = def_p_range.get_range_min();
		def_p_max = def_p_range.get_range_max();

		OEDiscreteRange def_c_range = etas_params.c_range;
		def_c_size = def_c_range.get_range_size();
		def_c_min = def_c_range.get_range_min();
		def_c_max = def_c_range.get_range_max();

		OEDiscreteRange def_zams_range = etas_params.zams_range;
		def_zams_size = def_zams_range.get_range_size();
		def_zams_min = def_zams_range.get_range_min();
		def_zams_max = def_zams_range.get_range_max();

		def_zams_relative = etas_params.relative_zams;

		OEDiscreteRange def_zmu_range = etas_params.zmu_range;
		if (def_zmu_range == null) {
			def_zmu_size = 1;
			def_zmu_min = 0.0;
			def_zmu_max = 0.0;
		} else {
			def_zmu_size = def_zmu_range.get_range_size();
			def_zmu_min = def_zmu_range.get_range_min();
			def_zmu_max = def_zmu_range.get_range_max();
		}

		OEDiscreteRange def_alpha_range = etas_params.alpha_range;
		if (def_alpha_range == null) {
			def_f_alpha_eq_b = true;
			def_alpha_value = 1.0;
		} else {
			def_f_alpha_eq_b = false;
			def_alpha_value = def_alpha_range.get_range_middle();
		}

		OEDiscreteRange def_b_range = etas_params.b_range;
		def_f_use_common_b = false;
		def_b_value = def_b_range.get_range_middle();

		return;
	}




	// Default values for model selection

	private EtasModelOption def_etas_model;
	private double def_bay_weight;

	// Initialize from OEConstants.

	private void init_default_model () {
		def_etas_model = EtasModelOption.AUTO;
		def_bay_weight = OEConstants.BAY_WT_BAYESIAN;
		return;
	}

	// Initialize from ETAS parameters.

	private boolean is_weight_bay (double weight) {
		return (Math.abs (weight - OEConstants.BAY_WT_BAYESIAN) <= 0.0001);
	}

	private boolean is_weight_seq (double weight) {
		return (Math.abs (weight - OEConstants.BAY_WT_SEQ_SPEC) <= 0.0001);
	}

	private boolean is_weight_gen (double weight) {
		return (Math.abs (weight - OEConstants.BAY_WT_GENERIC) <= 0.0001);
	}

	private void update_default_model (OEtasParameters etas_params) {

		if (!( etas_params != null && etas_params.bay_weight_avail && !(etas_params.eligible_params_avail && etas_params.eligible_option == OEConstants.ELIGIBLE_OPT_DISABLE) )) {
			init_default_model();
			return;
		}

		// If the bay_weight and early_bay_weight are Bayesian and generic, assume AUTO

		if (is_weight_bay(etas_params.bay_weight) && is_weight_gen(etas_params.early_bay_weight)) {
			def_etas_model = EtasModelOption.AUTO;
			def_bay_weight = OEConstants.BAY_WT_BAYESIAN;
		}
		else if (is_weight_bay(etas_params.bay_weight)) {
			def_etas_model = EtasModelOption.BAYESIAN;
			def_bay_weight = OEConstants.BAY_WT_BAYESIAN;
		}
		else if (is_weight_seq(etas_params.bay_weight)) {
			def_etas_model = EtasModelOption.SEQSPEC;
			def_bay_weight = OEConstants.BAY_WT_SEQ_SPEC;
		}
		else if (is_weight_gen(etas_params.bay_weight)) {
			def_etas_model = EtasModelOption.GENERIC;
			def_bay_weight = OEConstants.BAY_WT_GENERIC;
		}
		else {
			def_etas_model = EtasModelOption.CUSTOM;
			def_bay_weight = SimpleUtils.clip_max_min (0.0, 2.0, etas_params.bay_weight);
		}

		return;
	}




	// Default values for Bayesian prior.

	private EtasPriorOption def_etas_prior;

	// Initialize from OEConstants.

	private void init_default_prior () {
		def_etas_prior = EtasPriorOption.MIXED;
		return;
	}

	// Initialize from ETAS parameters.

	private void update_default_prior (OEtasParameters etas_params) {

		if (!( etas_params != null && etas_params.bay_prior_avail && !(etas_params.eligible_params_avail && etas_params.eligible_option == OEConstants.ELIGIBLE_OPT_DISABLE) )) {
			init_default_prior();
			return;
		}

		if (etas_params.bay_factory instanceof OEBayFactoryMixedRNPC) {
			def_etas_prior = EtasPriorOption.MIXED;
		}
		else if (etas_params.bay_factory instanceof OEBayFactoryGaussAPC) {
			def_etas_prior = EtasPriorOption.GAUSSIAN;
		}
		else if (etas_params.bay_factory instanceof OEBayFactoryUniform) {
			def_etas_prior = EtasPriorOption.UNIFORM;
		}
		else {
			def_etas_prior = EtasPriorOption.MIXED;
		}

		return;
	}




	// Default values for ETAS enable.

	private EtasEnableOption def_etas_enable;

	// Initialize from OEConstants.

	private void init_default_etas_enable () {
		def_etas_enable = EtasEnableOption.AUTO;
		return;
	}

	// Initialize from ETAS parameters.

	private void update_default_etas_enable (OEtasParameters etas_params) {

		if (!( etas_params != null && etas_params.eligible_params_avail )) {
			init_default_etas_enable();
			return;
		}

		if (etas_params.eligible_option == OEConstants.ELIGIBLE_OPT_DISABLE) {
			def_etas_enable = EtasEnableOption.DISABLE;
		} else if (etas_params.eligible_option == OEConstants.ELIGIBLE_OPT_ENABLE) {
			def_etas_enable = EtasEnableOption.ENABLE;
		} else {
			def_etas_enable = EtasEnableOption.AUTO;
		}

		return;
	}




	//----- Controls for the ETAS parameter dialog -----




	// ETAS enable; dropdown containing an enumeration to select ETAS enable option.

	private EnumParameter<EtasEnableOption> etasEnableParam;

	private EnumParameter<EtasEnableOption> init_etasEnableParam () throws GUIEDTException {
		etasEnableParam = new EnumParameter<EtasEnableOption>(
				"ETAS Forecasts", EnumSet.allOf(EtasEnableOption.class), EtasEnableOption.AUTO, null);
		//etasEnableParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		etasEnableParam.setInfo("Controls whether ETAS is used to generate forecasts");
		register_param (etasEnableParam, "etasEnableParam", PARMGRP_ETAS_ENABLE);
		return etasEnableParam;
	}




	// Range of n-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter nETASValRangeParam;

	private RangeParameter init_nETASValRangeParam () throws GUIEDTException {
		nETASValRangeParam = new RangeParameter("ETAS n Range", new Range(def_n_min, def_n_max));
		nETASValRangeParam.setUnits("Log Scale");
		nETASValRangeParam.setInfo("Branch ratio");
		register_param (nETASValRangeParam, "nETASValRangeParam", PARMGRP_RANGE_ETAS_N);
		return nETASValRangeParam;
	}


	// Number of n-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter nETASValNumParam;

	private IntegerParameter init_nETASValNumParam () throws GUIEDTException {
		nETASValNumParam = new IntegerParameter("ETAS n Number", 1, 10000, Integer.valueOf(def_n_size));
		//nETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (nETASValNumParam, "nETASValNumParam", PARMGRP_RANGE_ETAS_N);
		return nETASValNumParam;
	}


	// ETAS n skew parameter; edit box containing a number.

	private DoubleParameter nETASValSkewParam;

	private DoubleParameter init_nETASValSkewParam () throws GUIEDTException {
		nETASValSkewParam = new DoubleParameter("ETAS n Skew", Double.valueOf(def_n_skew));
		nETASValSkewParam.setInfo("Controls distribution of grid points, 1.0 for uniform, >1.0 for more points at the high end of the range");
		register_param (nETASValSkewParam, "nETASValSkewParam", PARMGRP_ETAS_VALUE);
		return nETASValSkewParam;
	}


	// Range of p-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter pETASValRangeParam;

	private RangeParameter init_pETASValRangeParam () throws GUIEDTException {
		pETASValRangeParam = new RangeParameter("ETAS p Range", new Range(def_p_min, def_p_max));
		pETASValRangeParam.setUnits("Linear Scale");
		pETASValRangeParam.setInfo("Omori exponent");
		register_param (pETASValRangeParam, "pETASValRangeParam", PARMGRP_RANGE_ETAS_P);
		return pETASValRangeParam;
	}


	// Number of p-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter pETASValNumParam;

	private IntegerParameter init_pETASValNumParam () throws GUIEDTException {
		pETASValNumParam = new IntegerParameter("ETAS p Number", 1, 10000, Integer.valueOf(def_p_size));
		//pETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (pETASValNumParam, "pETASValNumParam", PARMGRP_RANGE_ETAS_P);
		return pETASValNumParam;
	}


	// Range of c-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter cETASValRangeParam;

	private RangeParameter init_cETASValRangeParam () throws GUIEDTException {
		cETASValRangeParam = new RangeParameter("ETAS c Range", new Range(def_c_min, def_c_max));
		cETASValRangeParam.setUnits("Log Scale");
		cETASValRangeParam.setInfo("Omori offset, in days");
		register_param (cETASValRangeParam, "cETASValRangeParam", PARMGRP_RANGE_ETAS_C);
		return cETASValRangeParam;
	}


	// Number of c-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter cETASValNumParam;

	private IntegerParameter init_cETASValNumParam () throws GUIEDTException {
		cETASValNumParam = new IntegerParameter("ETAS c Number", 1, 10000, Integer.valueOf(def_c_size));
		//cETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (cETASValNumParam, "cETASValNumParam", PARMGRP_RANGE_ETAS_C);
		return cETASValNumParam;
	}


	// Range of zamx-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter zamsETASValRangeParam;

	private RangeParameter init_zamsETASValRangeParam () throws GUIEDTException {
		zamsETASValRangeParam = new RangeParameter("ETAS zams Range", new Range(def_zams_min, def_zams_max));
		zamsETASValRangeParam.setUnits("Linear Scale");
		zamsETASValRangeParam.setInfo("Mainshock/foreshock productivity, can be relative to aftershock productivity, or absolute");
		register_param (zamsETASValRangeParam, "zamsETASValRangeParam", PARMGRP_RANGE_ETAS_ZAMS);
		return zamsETASValRangeParam;
	}


	// Number of zams-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter zamsETASValNumParam;

	private IntegerParameter init_zamsETASValNumParam () throws GUIEDTException {
		zamsETASValNumParam = new IntegerParameter("ETAS zams Number", 1, 10000, Integer.valueOf(def_zams_size));
		//zamsETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (zamsETASValNumParam, "zamsETASValNumParam", PARMGRP_RANGE_ETAS_ZAMS);
		return zamsETASValNumParam;
	}


	// Option to use relative value for zams.

	private BooleanParameter zamsETASValRelativeParam;

	private BooleanParameter init_zamsETASValRelativeParam () throws GUIEDTException {
		zamsETASValRelativeParam = new BooleanParameter("Use relative zams", true);
		zamsETASValRelativeParam.setInfo("Controls if mainshock productivty is given relative to aftershock productivity");
		register_param (zamsETASValRelativeParam, "zamsETASValRelativeParam", PARMGRP_ETAS_VALUE);
		return zamsETASValRelativeParam;
	}


	// Range of zmu-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter zmuETASValRangeParam;

	private RangeParameter init_zmuETASValRangeParam () throws GUIEDTException {
		zmuETASValRangeParam = new RangeParameter("ETAS zmu Range", new Range(def_zmu_min, def_zmu_max));
		zmuETASValRangeParam.setUnits("Log Scale");
		zmuETASValRangeParam.setInfo("Background rate in M3 per day, either a log scale or any single value");
		register_param (zmuETASValRangeParam, "zmuETASValRangeParam", PARMGRP_RANGE_ETAS_ZMU);
		return zmuETASValRangeParam;
	}


	// Number of zmu-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter zmuETASValNumParam;

	private IntegerParameter init_zmuETASValNumParam () throws GUIEDTException {
		zmuETASValNumParam = new IntegerParameter("ETAS zmu Number", 1, 10000, Integer.valueOf(def_zmu_size));
		//zmuETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (zmuETASValNumParam, "zmuETASValNumParam", PARMGRP_RANGE_ETAS_ZMU);
		return zmuETASValNumParam;
	}




	// ETAS model; dropdown containing an enumeration to select ETAS enable option.

	private EnumParameter<EtasModelOption> etasModelParam;

	private EnumParameter<EtasModelOption> init_etasModelParam () throws GUIEDTException {
		etasModelParam = new EnumParameter<EtasModelOption>(
				"ETAS Model", EnumSet.allOf(EtasModelOption.class), def_etas_model, null);
		etasModelParam.setInfo("Controls the model used to generate the ETAS forecast");
		register_param (etasModelParam, "etasModelParam", PARMGRP_ETAS_MODEL);
		return etasModelParam;
	}


	// ETAS custom Bayesian weight; edit box containing a number.

	private DoubleParameter etasBayWeightParam;

	private DoubleParameter init_etasBayWeightParam () throws GUIEDTException {
		etasBayWeightParam = new DoubleParameter("ETAS Model Weight", 0.0, 2.0, Double.valueOf(def_bay_weight));
		//etasBayWeightParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		etasBayWeightParam.setInfo("Selects the model on a continuous scale: 0.0 = Sequence Specific, 1.0 = Bayesian, 2.0 = Generic");
		register_param (etasBayWeightParam, "etasBayWeightParam", PARMGRP_ETAS_VALUE);
		return etasBayWeightParam;
	}


	// ETAS prior; dropdown containing an enumeration to select ETAS enable option.

	private EnumParameter<EtasPriorOption> etasPriorParam;

	private EnumParameter<EtasPriorOption> init_etasPriorParam () throws GUIEDTException {
		etasPriorParam = new EnumParameter<EtasPriorOption>(
				"ETAS Prior", EnumSet.allOf(EtasPriorOption.class), def_etas_prior, null);
		etasPriorParam.setInfo("Selects the Bayesian prior used to generate the ETAS forecast");
		register_param (etasPriorParam, "etasPriorParam", PARMGRP_ETAS_PRIOR);
		return etasPriorParam;
	}


	// Option to use common b-value.

	private BooleanParameter useCommonBParam;

	private BooleanParameter init_useCommonBParam () throws GUIEDTException {
		useCommonBParam = new BooleanParameter("Use common b-value", true);
		useCommonBParam.setInfo("Controls if ETAS b is fixed equal to the RJ Gutenberg-Richter exponent b");
		register_param (useCommonBParam, "useCommonBParam", PARMGRP_USE_COMMON_B);
		return useCommonBParam;
	}


	// ETAS b parameter; edit box containing a number.

	private DoubleParameter bETASParam;

	private DoubleParameter init_bETASParam () throws GUIEDTException {
		bETASParam = new DoubleParameter("ETAS b", 1.0);	// can't use DoubleParameter("ETAS b", null) because the call would be ambiguous
		bETASParam.setValue(null);
		bETASParam.setInfo("The Gutenberg-Richter exponent b");
		register_param (bETASParam, "bETASParam", PARMGRP_ETAS_VALUE);
		return bETASParam;
	}


	// Option to use alpha == b.

	private BooleanParameter alphaEqualsBParam;

	private BooleanParameter init_alphaEqualsBParam () throws GUIEDTException {
		alphaEqualsBParam = new BooleanParameter("Use alpha = b", true);
		alphaEqualsBParam.setInfo("Controls if alpha is fixed equal to the Gutenberg-Richter exponent b");
		register_param (alphaEqualsBParam, "alphaEqualsBParam", PARMGRP_ALPHA_EQUALS_B);
		return alphaEqualsBParam;
	}


	// ETAS alpha parameter; edit box containing a number.

	private DoubleParameter alphaParam;

	private DoubleParameter init_alphaParam () throws GUIEDTException {
		alphaParam = new DoubleParameter("ETAS alpha", 1.0);	// can't use DoubleParameter("ETAS alpha", null) because the call would be ambiguous
		alphaParam.setValue(null);
		alphaParam.setInfo("The productivity exponent alpha, usually set equal to b");
		register_param (alphaParam, "alphaParam", PARMGRP_ETAS_VALUE);
		return alphaParam;
	}




	// ETAS parameter value dialog: initialize parameters within the dialog.

	private void init_etasValueDialogParam () throws GUIEDTException {

		init_etasEnableParam();
		
		init_nETASValRangeParam();
		
		init_nETASValNumParam();
		
		init_nETASValSkewParam();
		
		init_pETASValRangeParam();
		
		init_pETASValNumParam();
		
		init_cETASValRangeParam();
		
		init_cETASValNumParam();
		
		init_zamsETASValRangeParam();
		
		init_zamsETASValNumParam();

		init_zamsETASValRelativeParam();
		
		init_zmuETASValRangeParam();
		
		init_zmuETASValNumParam();

		init_etasModelParam();

		init_etasBayWeightParam();

		init_etasPriorParam();

		init_useCommonBParam();

		init_bETASParam();

		init_alphaEqualsBParam();

		init_alphaParam();

		return;
	}




	// ETAS parameter edit: button to activate the dialog.

	private ParameterList etasValueList;			// List of parameters in the dialog
	private GUIParameterListParameter etasValueEditParam;
	

	private void updateETASValueParamList () throws GUIEDTException {
		etasValueList.clear();

		boolean f_button_row = false;

		etasValueEditParam.setListTitleText ("ETAS Parameters");
		etasValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(12, f_button_row, 0));

		etasValueList.addParameter(nETASValRangeParam);
		etasValueList.addParameter(nETASValNumParam);
		etasValueList.addParameter(nETASValSkewParam);
		etasValueList.addParameter(pETASValRangeParam);
		etasValueList.addParameter(pETASValNumParam);
		etasValueList.addParameter(cETASValRangeParam);
		etasValueList.addParameter(cETASValNumParam);
		etasValueList.addParameter(zamsETASValRangeParam);
		etasValueList.addParameter(zamsETASValNumParam);
		etasValueList.addParameter(zamsETASValRelativeParam);
		etasValueList.addParameter(zmuETASValRangeParam);
		etasValueList.addParameter(zmuETASValNumParam);
		
		etasValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_etasValueEditParam () throws GUIEDTException {
		etasValueList = new ParameterList();
		etasValueEditParam = new GUIParameterListParameter("ETAS Parameters", etasValueList, "Edit ETAS Params...",
							"Edit ETAS Params", "ETAS Parameters", null, null, false, gui_top.get_trace_events(),
							gui_top.make_help ("help_param_etas_param.html"));
		etasValueEditParam.setInfo("Set ETAS parameter values");
		register_param (etasValueEditParam, "etasValueEditParam", PARMGRP_ETAS_EDIT);

		updateETASValueParamList();

		return etasValueEditParam;
	}




	// ETAS option edit: button to activate the dialog.

	private ParameterList etasOptionList;			// List of parameters in the dialog
	private GUIParameterListParameter etasOptionEditParam;
	

	private void updateETASOptionParamList () throws GUIEDTException {
		etasOptionList.clear();

		boolean f_button_row = false;

		etasOptionEditParam.setListTitleText ("ETAS Options");
		etasOptionEditParam.setDialogDimensions (gui_top.get_dialog_dims(7, f_button_row, 2));

		etasOptionList.addParameter(etasModelParam);
		etasOptionList.addParameter(etasBayWeightParam);

		etasOptionList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

		etasOptionList.addParameter(etasPriorParam);

		etasOptionList.addParameter(new GUISeparatorParameter("Separator2", gui_top.get_separator_color()));

		etasOptionList.addParameter(useCommonBParam);
		etasOptionList.addParameter(bETASParam);
		etasOptionList.addParameter(alphaEqualsBParam);
		etasOptionList.addParameter(alphaParam);
		
		etasOptionEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_etasOptionEditParam () throws GUIEDTException {
		etasOptionList = new ParameterList();
		etasOptionEditParam = new GUIParameterListParameter("ETAS Options", etasOptionList, "Edit ETAS Options...",
							"Edit ETAS Options", "ETAS Options", null, null, false, gui_top.get_trace_events(),
							gui_top.make_help ("help_param_etas_opt.html"));
		etasOptionEditParam.setInfo("Set ETAS options");
		register_param (etasOptionEditParam, "etasOptionEditParam", PARMGRP_ETAS_OPTION);

		updateETASOptionParamList();

		return etasOptionEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the ETAS parameter value controls.

	private boolean f_etas_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		// Enable parameters

		enableDefaultParam(nETASValRangeParam, f_etas_value_enable, null);
		enableDefaultParam(nETASValNumParam, f_etas_value_enable, null);
		enableDefaultParam(nETASValSkewParam, f_etas_value_enable, null);
		enableDefaultParam(pETASValRangeParam, f_etas_value_enable, null);
		enableDefaultParam(pETASValNumParam, f_etas_value_enable, null);
		enableDefaultParam(cETASValRangeParam, f_etas_value_enable, null);
		enableDefaultParam(cETASValNumParam, f_etas_value_enable, null);
		enableDefaultParam(zamsETASValRangeParam, f_etas_value_enable, null);
		enableDefaultParam(zamsETASValNumParam, f_etas_value_enable, null);
		enableDefaultParam(zamsETASValRelativeParam, f_etas_value_enable, true);
		enableDefaultParam(zmuETASValRangeParam, f_etas_value_enable, null);
		enableDefaultParam(zmuETASValNumParam, f_etas_value_enable, null);

		enableDefaultParam(useCommonBParam, f_etas_value_enable, true);
		boolean f_use_common_b = validParam(useCommonBParam);
		enableDefaultParam(bETASParam, !f_use_common_b, null);
		if (!( f_use_common_b )) {
			if (!( definedParam(bETASParam) )) {
				updateParam(bETASParam, def_b_value);
			}
		}

		enableDefaultParam(alphaEqualsBParam, f_etas_value_enable, true);
		boolean f_alpha_eq_b = validParam(alphaEqualsBParam);
		enableDefaultParam(alphaParam, !f_alpha_eq_b, null);
		if (!( f_alpha_eq_b )) {
			if (!( definedParam(alphaParam) )) {
				updateParam(alphaParam, def_alpha_value);
			}
		}

		enableDefaultParam(etasModelParam, f_etas_value_enable, EtasModelOption.AUTO);
		if (f_etas_value_enable) {
			adjust_for_model_option();
		} else {
			enableDefaultParam(etasBayWeightParam, f_etas_value_enable, null);
		}

		enableDefaultParam(etasPriorParam, f_etas_value_enable, EtasPriorOption.MIXED);

		enableDefaultParam(etasEnableParam, f_sub_enable, null);
		if (f_sub_enable) {
			if (!( definedParam(etasEnableParam) )) {
				updateParam(etasEnableParam, def_etas_enable);
			}
		}

		enableParam(etasValueEditParam, f_sub_enable);
		enableParam(etasOptionEditParam, f_sub_enable);

		return;
	}


	// Adjust enable/disable and values for the current model option.
	// The enable status and value of the Bayesian weight is adjusted for the model selected.

	private void adjust_for_model_option () throws GUIEDTException {

		EtasModelOption model_option = validParam(etasModelParam);
		switch (model_option) {

		case AUTO: {
			enableParam(etasBayWeightParam, false);
			updateParam(etasBayWeightParam, null);
		}
		break;

		case BAYESIAN: {
			enableParam(etasBayWeightParam, false);
			updateParam(etasBayWeightParam, OEConstants.BAY_WT_BAYESIAN);
		}
		break;

		case SEQSPEC: {
			enableParam(etasBayWeightParam, false);
			updateParam(etasBayWeightParam, OEConstants.BAY_WT_SEQ_SPEC);
		}
		break;

		case GENERIC: {
			enableParam(etasBayWeightParam, false);
			updateParam(etasBayWeightParam, OEConstants.BAY_WT_GENERIC);
		}
		break;

		case CUSTOM: {
			enableParam(etasBayWeightParam, true);
			if (!( definedParam(etasBayWeightParam) )) {
				updateParam(etasBayWeightParam, OEConstants.BAY_WT_BAYESIAN);
			}
		}
		break;

		default:
			throw new IllegalArgumentException ("OEGUISubETASValue.adjust_for_model_option: Invalid ETAS model option");
		}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferETASValueView {

		// ETAS enable.

		public EtasEnableOption x_etasEnableParam;	// parameter value, checked for validity

		// Range of n.
	
		public Range x_nETASValRangeParam;			// parameter value, checked for validity

		// Number of n.

		public int x_nETASValNumParam;				// parameter value, checked for validity

		// Skew of n.

		public double x_nETASValSkewParam;				// parameter value, checked for validity

		// Range of p.
	
		public Range x_pETASValRangeParam;			// parameter value, checked for validity

		// Number of p.

		public int x_pETASValNumParam;				// parameter value, checked for validity

		// Range of c.
	
		public Range x_cETASValRangeParam;			// parameter value, checked for validity

		// Number of c.

		public int x_cETASValNumParam;				// parameter value, checked for validity

		// Range of zams.
	
		public Range x_zamsETASValRangeParam;			// parameter value, checked for validity

		// Number of zams.

		public int x_zamsETASValNumParam;				// parameter value, checked for validity

		// Relative zams.

		public boolean x_zamsETASValRelativeParam;				// parameter value, checked for validity

		// Range of zmu.
	
		public Range x_zmuETASValRangeParam;			// parameter value, checked for validity

		// Number of zmu.

		public int x_zmuETASValNumParam;				// parameter value, checked for validity

		// Flag indicating if using common b-value.

		public boolean x_useCommonBParam;				// parameter value, checked for validity

		// Value of b, present if x_useCommonBParam is false.

		public double x_bETASParam;				// parameter value, checked for validity

		// Flag indicating if alpha == b.

		public boolean x_alphaEqualsBParam;				// parameter value, checked for validity

		// Value of alpha, present if x_alphaEqualsBParam is false.

		public double x_alphaParam;				// parameter value, checked for validity

		// Model selection.

		public EtasModelOption x_etasModelParam;	// parameter value, checked for validity

		// Bayesian weight, present if x_etasModelParam is EtasModelOption.CUSTOM.

		public double x_etasBayWeightParam;			// parameter value, checked for validity

		// ETAS prior.

		public EtasPriorOption x_etasPriorParam;	// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferETASValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferETASValueImpl extends XferETASValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferETASValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferETASValueImpl () {
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
		public XferETASValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// If subpanel is disabled, just set ETAS disable

			if (!( f_sub_enable )) {
				x_etasEnableParam = EtasEnableOption.DISABLE;
				return this;
			}

			// ETAS enable

			x_etasEnableParam = validParam(etasEnableParam);

			// n-values

			x_nETASValRangeParam = validParam(nETASValRangeParam);
			x_nETASValNumParam = validParam(nETASValNumParam);
			validateRange(x_nETASValRangeParam, x_nETASValNumParam, "ETAS n");

			x_nETASValSkewParam = validParam(nETASValSkewParam);

			// p-values

			x_pETASValRangeParam = validParam(pETASValRangeParam);
			x_pETASValNumParam = validParam(pETASValNumParam);
			validateRange(x_pETASValRangeParam, x_pETASValNumParam, "ETAS p");

			// c-values

			x_cETASValRangeParam = validParam(cETASValRangeParam);
			x_cETASValNumParam = validParam(cETASValNumParam);
			validateRange(x_cETASValRangeParam, x_cETASValNumParam, "ETAS c");

			// zams-values

			x_zamsETASValRangeParam = validParam(zamsETASValRangeParam);
			x_zamsETASValNumParam = validParam(zamsETASValNumParam);
			validateRange(x_zamsETASValRangeParam, x_zamsETASValNumParam, "ETAS zams");

			x_zamsETASValRelativeParam = validParam(zamsETASValRelativeParam);

			// zmu-values

			x_zmuETASValRangeParam = validParam(zmuETASValRangeParam);
			x_zmuETASValNumParam = validParam(zmuETASValNumParam);
			validateRange(x_zmuETASValRangeParam, x_zmuETASValNumParam, "ETAS zmu");

			int max_grid = 5000000;
			max_grid = max_grid / x_nETASValNumParam;
			max_grid = max_grid / x_pETASValNumParam;
			max_grid = max_grid / x_cETASValNumParam;
			max_grid = max_grid / x_zamsETASValNumParam;
			max_grid = max_grid / x_zmuETASValNumParam;
			Preconditions.checkState(max_grid > 0, "ETAS parameter search grid exceeds 5,000,000 entries");

			// Flag indicating if using common b-value.

			x_useCommonBParam = validParam(useCommonBParam);

			// Value of b, present if x_useCommonBParam is false.

			if (!( x_useCommonBParam )) {
				x_bETASParam = validParam(bETASParam);
			}

			// Flag indicating if alpha == b.

			x_alphaEqualsBParam = validParam(alphaEqualsBParam);

			// Value of alpha, present if x_alphaEqualsBParam is false.

			if (!( x_alphaEqualsBParam )) {
				x_alphaParam = validParam(alphaParam);
			}

			// Model selection.

			x_etasModelParam = validParam(etasModelParam);

			// Bayesian weight, present if x_etasModelParam is EtasModelOption.CUSTOM.

			if (x_etasModelParam == EtasModelOption.CUSTOM) {
				x_etasBayWeightParam = validParam(etasBayWeightParam);
			}

			// ETAS prior.

			x_etasPriorParam = validParam(etasPriorParam);

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			//  // If we're using common b-value, then update using the b-value from the model
			//  
			//  if (x_useCommonBParam) {
			//  	updateParam(bETASParam, gui_model.get_cat_bParam());
			//  }
			//  
			//  // If we're forcing alpha == b, then update alpha with b-value from the model
			//  
			//  if (x_alphaEqualsBParam) {
			//  	if (x_useCommonBParam || !definedParam(bETASParam)) {
			//  		updateParam(alphaParam, gui_model.get_cat_bParam());
			//  	} else {
			//  		updateParam(alphaParam, validParam(bETASParam));
			//  	}
			//  }

			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubETASValue (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Initialize default ranges, model, prior

		init_default_ranges();
		init_default_model();
		init_default_prior();
		init_default_etas_enable();

		// Default enable state

		f_sub_enable = false;
		f_etas_value_enable = false;

		// Create and initialize controls

		init_etasValueDialogParam();
		init_etasValueEditParam();
		init_etasOptionEditParam();

		// Set initial enable state

		adjust_enable();
		update_etas_value_from_defaults();
	}


	// Get the ETAS enable drop-down.
	// The intent is to use this only to insert the control into the client.

	public EnumParameter<EtasEnableOption> get_etasEnableParam () throws GUIEDTException {
		return etasEnableParam;
	}


	// Get the ETAS parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_etasValueEditParam () throws GUIEDTException {
		return etasValueEditParam;
	}


	// Get the ETAS parameter option edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_etasOptionEditParam () throws GUIEDTException {
		return etasOptionEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_etas_value_enable (boolean f_sub_enable, boolean f_etas_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_etas_value_enable = f_etas_value_enable;
		adjust_enable();
		return;
	}


	// Make an ETAS value transfer object.

	public XferETASValueImpl make_etas_value_xfer () {
		return new XferETASValueImpl();
	}


	// Private function, used to report that the ETAS parameter values have changed.

	private void report_etas_value_change () throws GUIEDTException {
		gui_controller.notify_aftershock_param_change();
		return;
	}


	// Update ETAS values from model.
	// This is called after catalog fetch or load.

	public void update_etas_value_from_model () throws GUIEDTException {

		OEtasParameters etas_params = gui_model.get_etasParams();
		//OEtasParameters etas_params = gui_model.get_or_make_etasParams().params;

		update_default_ranges (etas_params);
		update_default_model (etas_params);
		update_default_prior (etas_params);
		update_default_etas_enable (etas_params);

		update_etas_value_from_defaults();

		return;
	}


	// Update ETAS values from the defaults.

	private void update_etas_value_from_defaults () throws GUIEDTException {
		updateParam(etasEnableParam, def_etas_enable);

		updateParam(nETASValRangeParam, new Range(def_n_min, def_n_max));
		updateParam(nETASValNumParam, def_n_size);
		updateParam(nETASValSkewParam, def_n_skew);
		updateParam(pETASValRangeParam, new Range(def_p_min, def_p_max));
		updateParam(pETASValNumParam, def_p_size);
		updateParam(cETASValRangeParam, new Range(def_c_min, def_c_max));
		updateParam(cETASValNumParam, def_c_size);
		updateParam(zamsETASValRangeParam, new Range(def_zams_min, def_zams_max));
		updateParam(zamsETASValNumParam, def_zams_size);
		updateParam(zamsETASValRelativeParam, def_zams_relative);
		updateParam(zmuETASValRangeParam, new Range(def_zmu_min, def_zmu_max));
		updateParam(zmuETASValNumParam, def_zmu_size);

		updateParam(useCommonBParam, def_f_use_common_b);
		if (def_f_use_common_b) {
			updateParam(bETASParam, null);
		} else {
			updateParam(bETASParam, def_b_value);
		}
		updateParam(alphaEqualsBParam, def_f_alpha_eq_b);
		if (def_f_alpha_eq_b) {
			updateParam(alphaParam, null);
		} else {
			updateParam(alphaParam, def_alpha_value);
		}
		updateParam(etasModelParam, def_etas_model);
		if (def_etas_model == EtasModelOption.AUTO) {
			updateParam(etasBayWeightParam, null);
		} else {
			updateParam(etasBayWeightParam, def_bay_weight);
		}
		updateParam(etasPriorParam, def_etas_prior);

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// ETAS parameter edit button.
		// - Do nothing.

		case PARMGRP_ETAS_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// ETAS parameter option button.
		// - Do nothing.

		case PARMGRP_ETAS_OPTION: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// ETAS enable option.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_ETAS_ENABLE: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			adjust_enable();
			report_etas_value_change();
		}
		break;




		// ETAS parameter value.
		// - Report to top-level controller.

		case PARMGRP_ETAS_VALUE: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, n.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_N: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			updateRangeParams(nETASValRangeParam, nETASValNumParam, def_n_size, def_n_min, def_n_max);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, p.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_P: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			updateRangeParams(pETASValRangeParam, pETASValNumParam, def_p_size, def_p_min, def_p_max);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, c.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_C: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			updateRangeParams(cETASValRangeParam, cETASValNumParam, def_c_size, def_c_min, def_c_max);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, zams.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_ZAMS: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			updateRangeParams(zamsETASValRangeParam, zamsETASValNumParam, def_zams_size, def_zams_min, def_zams_max);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, zmu.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_ZMU: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			updateRangeParams(zmuETASValRangeParam, zmuETASValNumParam, def_zmu_size, def_zmu_min, def_zmu_max);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, use common b-value.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_USE_COMMON_B: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			adjust_enable();
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, alpha equals b.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_ALPHA_EQUALS_B: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			adjust_enable();
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, model selection.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_ETAS_MODEL: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			adjust_enable();
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, Bayesian prior selection.
		// - Report to top-level controller.

		case PARMGRP_ETAS_PRIOR: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			report_etas_value_change();
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubETASValue: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
