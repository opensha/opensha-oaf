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
import org.opensha.oaf.util.GUIConsoleWindow;
import org.opensha.oaf.util.GUICalcStep;
import org.opensha.oaf.util.GUICalcRunnable;
import org.opensha.oaf.util.GUICalcProgressBar;
import org.opensha.oaf.util.GUIEDTException;
import org.opensha.oaf.util.GUIEDTRunnable;
import org.opensha.oaf.util.GUIEventAlias;
import org.opensha.oaf.util.GUIExternalCatalog;
import org.opensha.oaf.util.GUIParameterListParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Reasenberg & Jones GUI - Controller implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the controller.
// It holds the controls that appear on the left side of the main window.


public class OEGUIController extends OEGUIListener {


	//----- Controls for the Data Parameters column -----


	// Event ID; edit box containing a string.

	private StringParameter eventIDParam;

	private StringParameter init_eventIDParam () throws GUIEDTException {
		eventIDParam = new StringParameter("USGS Event ID");
		eventIDParam.setValue("");
		eventIDParam.setInfo("Get IDs from https://earthquake.usgs.gov/earthquakes/");
		eventIDParam.addParameterChangeListener(this);
		return eventIDParam;
	}


	// Data start time, in days since the mainshock; edit box containing a number.

	private DoubleParameter dataStartTimeParam;

	private DoubleParameter init_dataStartTimeParam () throws GUIEDTException {
		dataStartTimeParam = new DoubleParameter("Data Start Time", 0d, 36500d, new Double(0d));
		dataStartTimeParam.setUnits("Days");
		dataStartTimeParam.setInfo("Data start relative to main shock origin time");
		dataStartTimeParam.addParameterChangeListener(this);
		return dataStartTimeParam;
	}


	// Data end time, in days since the mainshock; edit box containing a number.

	private DoubleParameter dataEndTimeParam;

	private DoubleParameter init_dataEndTimeParam () throws GUIEDTException {
		dataEndTimeParam = new DoubleParameter("Data End Time", 0d, 36500d, new Double(7d));
		dataEndTimeParam.setUnits("Days");
		dataEndTimeParam.setInfo("Data end relative to main shock origin time");
		dataEndTimeParam.addParameterChangeListener(this);
		return dataEndTimeParam;
	}


	// Region type; dropdown containing an enumeration to select type of search region.

	private EnumParameter<RegionType> regionTypeParam;

	private EnumParameter<RegionType> init_regionTypeParam () throws GUIEDTException {
		regionTypeParam = new EnumParameter<RegionType>(
				"Region Type", EnumSet.allOf(RegionType.class), RegionType.STANDARD, null);
		regionTypeParam.setInfo("Type of region for collecting aftershocks");
		regionTypeParam.addParameterChangeListener(this);
		return regionTypeParam;
	}


	// Edit region; pop-up dialog to select search region location, size, and shape.
	
	private DoubleParameter radiusParam;		// Search radius; appears when Region Type is Circular
	private DoubleParameter minLatParam;		// Minimum latitude; appears when Region Type is Rectangular
	private DoubleParameter maxLatParam;		// Maximum latitude; appears when Region Type is Rectangular
	private DoubleParameter minLonParam;		// Minimum longitude; appears when Region Type is Rectangular
	private DoubleParameter maxLonParam;		// Maximum longitude; appears when Region Type is Rectangular
	private DoubleParameter minDepthParam;		// Minimum depth; always appears
	private DoubleParameter maxDepthParam;		// Maximum depth; always appears
	private DoubleParameter wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
	private DoubleParameter minRadiusParam;		// Minimum radius; appears for WC regions
	private DoubleParameter maxRadiusParam;		// Maximum radius; appears for WC regions
	private DoubleParameter centerLatParam;		// Circle center latitude; appears for custom circle
	private DoubleParameter centerLonParam;		// Circle center longitude; appears for custom circle
	
	private ParameterList regionList;			// List of parameters in the Edit Region pop-up
	private GUIParameterListParameter regionEditParam;
	

	private void updateRegionParamList (RegionType type) throws GUIEDTException {
		regionList.clear();
		
		switch (type) {

		case STANDARD:
			// do nothing
			regionEditParam.setListTitleText ("Standard");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(0));
			break;

		case CENTROID_WC_CIRCLE:
			regionList.addParameter(wcMultiplierParam);
			regionList.addParameter(minRadiusParam);
			regionList.addParameter(maxRadiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Centroid WC Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5));
			break;

		case CENTROID_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Centroid Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(3));
			break;

		case EPICENTER_WC_CIRCLE:
			regionList.addParameter(wcMultiplierParam);
			regionList.addParameter(minRadiusParam);
			regionList.addParameter(maxRadiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Epicenter WC Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5));
			break;

		case EPICENTER_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Epicenter Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(3));
			break;

		case CUSTOM_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(centerLatParam);
			regionList.addParameter(centerLonParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Custom Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5));
			break;

		case CUSTOM_RECTANGLE:
			regionList.addParameter(minLatParam);
			regionList.addParameter(maxLatParam);
			regionList.addParameter(minLonParam);
			regionList.addParameter(maxLonParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Custom Rectangle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(6));
			break;

		default:
			throw new IllegalStateException("Unknown region type: " + type);
		}
		
		regionEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_regionEditParam () throws GUIEDTException {
		radiusParam = new DoubleParameter("Radius", 0d, 20000d, new Double(20d));
		radiusParam.setUnits("km");
		radiusParam.setInfo("Radius of circular region");

		minLatParam = new DoubleParameter("Min Lat", -90d, 90d, new Double(32d));
		minLatParam.setInfo("Minimum latitude of rectangular region");

		maxLatParam = new DoubleParameter("Max Lat", -90d, 90d, new Double(36d));
		maxLatParam.setInfo("Maximum latitude of rectangular region");

		minLonParam = new DoubleParameter("Min Lon", -180d, 180d, new Double(32d));
		minLonParam.setInfo("Minimum longitude of rectangular region");

		maxLonParam = new DoubleParameter("Max Lon", -180d, 180d, new Double(36d));
		maxLonParam.setInfo("Maximum longitude of rectangular region");

		minDepthParam = new DoubleParameter("Min Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, new Double(ComcatOAFAccessor.DEFAULT_MIN_DEPTH));
		minDepthParam.setUnits("km");
		minDepthParam.setInfo("Minimum depth of region");
		
		maxDepthParam = new DoubleParameter("Max Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, new Double(ComcatOAFAccessor.DEFAULT_MAX_DEPTH));
		maxDepthParam.setUnits("km");
		maxDepthParam.setInfo("Maximum depth of region");
		
		wcMultiplierParam = new DoubleParameter("WC Multiplier", 0d, 100d, new Double(1d));
		wcMultiplierParam.setInfo("Multiplier for WC radius");

		minRadiusParam = new DoubleParameter("Min Radius", 0d, 20000d, new Double(10d));
		minRadiusParam.setUnits("km");
		minRadiusParam.setInfo("Minimum radius of circular region");
		
		maxRadiusParam = new DoubleParameter("Max Radius", 0d, 20000d, new Double(2000d));
		maxRadiusParam.setUnits("km");
		maxRadiusParam.setInfo("Maximum radius of circular region");
		
		centerLatParam = new DoubleParameter("Center Lat", -90d, 90d, new Double(34d));
		centerLatParam.setInfo("Latitude of center of circular region");

		centerLonParam = new DoubleParameter("Center Lon", -180d, 180d, new Double(34d));
		centerLonParam.setInfo("Longitude of center of circular region");
		
		regionList = new ParameterList();
		regionEditParam = new GUIParameterListParameter("Region", regionList, "Edit Region...",
							"Edit Region", "Region Parameters", "Done", true, gui_top.get_trace_events());
		regionEditParam.setInfo("Select the region to search for aftershocks");
		regionEditParam.addParameterChangeListener(this);

		updateRegionParamList(regionTypeParam.getValue());

		return regionEditParam;
	}


	// Fetch Data button.

	private ButtonParameter fetchButton;

	private ButtonParameter init_fetchButton () throws GUIEDTException {
		fetchButton = new ButtonParameter("USGS Event Webservice", "Fetch Data");
		fetchButton.setInfo("Fetch data from USGS ComCat");
		fetchButton.addParameterChangeListener(this);
		return fetchButton;
	}


	// Load Catalog button.

	private JFileChooser loadCatalogChooser;		// the file chooser dialog, lazy-allocated
	private ButtonParameter loadCatalogButton;

	private ButtonParameter init_loadCatalogButton () throws GUIEDTException {
		loadCatalogChooser = null;
		loadCatalogButton = new ButtonParameter("External Catalog", "Load Catalog...");
		loadCatalogButton.setInfo("Load catalog from file in 10 column format");
		loadCatalogButton.addParameterChangeListener(this);
		return loadCatalogButton;
	}


	// Save Catalog button.

	private JFileChooser saveCatalogChooser;		// the file chooser dialog, lazy-allocated
	private ButtonParameter saveCatalogButton;

	private ButtonParameter init_saveCatalogButton () throws GUIEDTException {
		saveCatalogChooser = null;
		saveCatalogButton = new ButtonParameter("Aftershock Catalog", "Save Catalog...");
		saveCatalogButton.setInfo("Save catalog to file in 10 column format");
		saveCatalogButton.addParameterChangeListener(this);
		return saveCatalogButton;
	}


	// Mc for sequence; edit box containing a number.
	// The default is obtained by putting the aftershocks in magnitude bins;
	// Mmaxc is the magnitude bin that contains the largest count of aftershocks;
	// the default Mc is Mmaxc+0.5.  User can edit the field to override the default.

	private DoubleParameter mcParam;

	private DoubleParameter init_mcParam () throws GUIEDTException {
		mcParam = new DoubleParameter("Mc For Sequence", 0d, 9d);
		mcParam.getConstraint().setNullAllowed(true);
		mcParam.setInfo("Default is Mmaxc+0.5, but user can modify");
		mcParam.addParameterChangeListener(this);
		return mcParam;
	}


	// Precision of magnitudes in the catalog; edit box containing a number.
	// Used in the algorithm to estimate the b-value.  Defaults to 0.1.

	private DoubleParameter magPrecisionParam;

	private DoubleParameter init_magPrecisionParam () throws GUIEDTException {
		magPrecisionParam = new DoubleParameter("Mag Precision", 0d, 1d, new Double(0.1));
		magPrecisionParam.setInfo("Magnitude rounding applied by network");;
		magPrecisionParam.addParameterChangeListener(this);
		return magPrecisionParam;
	}


	// Compute b-value button.

	private ButtonParameter computeBButton;

	private ButtonParameter init_computeBButton () throws GUIEDTException {
		computeBButton = new ButtonParameter("Seq. Specific GR b-value", "Compute b (optional)");
		computeBButton.addParameterChangeListener(this);
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
		bParam.setInfo("Default is from generic parameters, but user can modify");
		bParam.addParameterChangeListener(this);
		return bParam;
	}




	//----- Controls for the Aftershock Parameters column -----


	// Range of a-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter aValRangeParam;

	private RangeParameter init_aValRangeParam () throws GUIEDTException {
		aValRangeParam = new RangeParameter("a-value Range", new Range(-4.5, -0.5));
		aValRangeParam.addParameterChangeListener(this);
		return aValRangeParam;
	}


	// Number of a-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter aValNumParam;

	private IntegerParameter init_aValNumParam () throws GUIEDTException {
		aValNumParam = new IntegerParameter("a-value Number", 1, 10000, new Integer(101));
		aValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		aValNumParam.addParameterChangeListener(this);
		return aValNumParam;
	}


	// Range of p-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter pValRangeParam;

	private RangeParameter init_pValRangeParam () throws GUIEDTException {
		pValRangeParam = new RangeParameter("p-value Range", new Range(0.5, 2.0));
		pValRangeParam.addParameterChangeListener(this);
		return pValRangeParam;
	}


	// Number of p-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter pValNumParam;

	private IntegerParameter init_pValNumParam () throws GUIEDTException {
		pValNumParam = new IntegerParameter("p-value Number", 1, 10000, new Integer(45));
		pValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		pValNumParam.addParameterChangeListener(this);
		return pValNumParam;
	}


	// Range of c-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter cValRangeParam;

	private RangeParameter init_cValRangeParam () throws GUIEDTException {
		cValRangeParam = new RangeParameter("c-value range", new Range(0.018, 0.018));
		cValRangeParam.addParameterChangeListener(this);
		return cValRangeParam;
	}


	// Number of c-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter cValNumParam;

	private IntegerParameter init_cValNumParam () throws GUIEDTException {
		cValNumParam = new IntegerParameter("c-value num", 1, 10000, new Integer(1));
		cValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		cValNumParam.addParameterChangeListener(this);
		return cValNumParam;
	}


	// Option to apply time-dependent Mc; check box.
	// Default is obtained from generic parameters when the data is loaded (typically true).

	private BooleanParameter timeDepMcParam;

	private BooleanParameter init_timeDepMcParam () throws GUIEDTException {
		timeDepMcParam = new BooleanParameter("Apply time dep. Mc", true);
		timeDepMcParam.addParameterChangeListener(this);
		return timeDepMcParam;
	}


	// Helmstetter F parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter fParam;

	private DoubleParameter init_fParam () throws GUIEDTException {
		fParam = new DoubleParameter("F", 0.0, 2.0, new Double(0.5));
		fParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		fParam.addParameterChangeListener(this);
		return fParam;
	}


	// Helmstetter G parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter gParam;

	private DoubleParameter init_gParam () throws GUIEDTException {
		gParam = new DoubleParameter("G", -10.0, 100.0, new Double(0.25));
		gParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		gParam.addParameterChangeListener(this);
		return gParam;
	}


	// Helmstetter H parameter; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter hParam;

	private DoubleParameter init_hParam () throws GUIEDTException {
		hParam = new DoubleParameter("H", 0.0, 10.0, new Double(1.0));
		hParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		hParam.addParameterChangeListener(this);
		return hParam;
	}


	// Magnitude of completeness for time-dependent Mc; edit box containing a number.
	// Default is obtained from generic parameters when the data is loaded.

	private DoubleParameter mCatParam;

	private DoubleParameter init_mCatParam () throws GUIEDTException {
		mCatParam = new DoubleParameter("Mcat", 1.0, 7.0, new Double(4.5));
		mCatParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		mCatParam.addParameterChangeListener(this);
		return mCatParam;
	}


	// Compute Aftershock Parameters button.

	private ButtonParameter computeAftershockParamsButton;

	private ButtonParameter init_computeAftershockParamsButton () throws GUIEDTException {
		computeAftershockParamsButton = new ButtonParameter("Aftershock Params", "Compute");
		computeAftershockParamsButton.addParameterChangeListener(this);
		return computeAftershockParamsButton;
	}


	// Maximum likelihood a-value; read-only edit box containing a number.
	// Value is obtained from aftershock parameters.

	private DoubleParameter aValParam;

	private DoubleParameter init_aValParam () throws GUIEDTException {
		aValParam = new DoubleParameter("a-value", new Double(0d));	// can't use DoubleParameter("a-value", null) because the call would be ambiguous
		aValParam.setValue(null);
		aValParam.addParameterChangeListener(this);
		return aValParam;
	}


	// Maximum likelihood p-value; read-only edit box containing a number.
	// Value is obtained from aftershock parameters.

	private DoubleParameter pValParam;

	private DoubleParameter init_pValParam () throws GUIEDTException {
		pValParam = new DoubleParameter("p-value", new Double(0d));	// can't use DoubleParameter("p-value", null) because the call would be ambiguous
		pValParam.setValue(null);
		pValParam.addParameterChangeListener(this);
		return pValParam;
	}


	// Maximum likelihood c-value; read-only edit box containing a number.
	// Value is obtained from aftershock parameters.

	private DoubleParameter cValParam;

	private DoubleParameter init_cValParam () throws GUIEDTException {
		cValParam = new DoubleParameter("c-value", new Double(0d));	// can't use DoubleParameter("c-value", null) because the call would be ambiguous
		cValParam.setValue(null);
		cValParam.addParameterChangeListener(this);
		return cValParam;
	}


	// Set To Now button.
	// Sets forecast start time so the forecast starts now.
	// Sets forecast end time to preserve end minus start.

	private ButtonParameter forecastStartTimeNowParam;

	private ButtonParameter init_forecastStartTimeNowParam () throws GUIEDTException {
		forecastStartTimeNowParam = new ButtonParameter("Set Forecast Start Time", "Set to Now");
		forecastStartTimeNowParam.addParameterChangeListener(this);
		return forecastStartTimeNowParam;
	}


	// Forecast start time, in days since the mainshock; edit box containing a number.
	// Default is 0.0.

	private DoubleParameter forecastStartTimeParam;

	private DoubleParameter init_forecastStartTimeParam () throws GUIEDTException {
		forecastStartTimeParam = new DoubleParameter("Forecast Start Time", 0d, 36500d, new Double(0d));
		forecastStartTimeParam.setUnits("Days");
		forecastStartTimeParam.setInfo("Forecast start relative to main shock origin time");
		forecastStartTimeParam.addParameterChangeListener(this);
		return forecastStartTimeParam;
	}


	// Forecast end time, in days since the mainshock; edit box containing a number.
	// Default is 7.0.

	private DoubleParameter forecastEndTimeParam;

	private DoubleParameter init_forecastEndTimeParam () throws GUIEDTException {
		forecastEndTimeParam = new DoubleParameter("Forecast End Time", 0d, 36500d, new Double(7d));
		forecastEndTimeParam.setUnits("Days");
		forecastEndTimeParam.setInfo("Forecast end relative to main shock origin time");
		forecastEndTimeParam.addParameterChangeListener(this);
		return forecastEndTimeParam;
	}


	// Compute Aftershock Forecast button.

	private ButtonParameter computeAftershockForecastButton;

	private ButtonParameter init_computeAftershockForecastButton () throws GUIEDTException {
		computeAftershockForecastButton = new ButtonParameter("Aftershock Forecast", "Compute");
		computeAftershockForecastButton.addParameterChangeListener(this);
		return computeAftershockForecastButton;
	}


	// Fetch Server Status button.

	private ButtonParameter fetchServerStatusButton;

	private ButtonParameter init_fetchServerStatusButton () throws GUIEDTException {
		fetchServerStatusButton = new ButtonParameter("AAFS Server", "Fetch Status");
		fetchServerStatusButton.addParameterChangeListener(this);
		return fetchServerStatusButton;
	}


	// Automatic forecast enable; dropdown containing an enumeration to select automatic enable option.

	private EnumParameter<AutoEnable> autoEnableParam;

	private EnumParameter<AutoEnable> init_autoEnableParam () throws GUIEDTException {
		autoEnableParam = new EnumParameter<AutoEnable>(
				"Automatic Forecasts", EnumSet.allOf(AutoEnable.class), AutoEnable.NORMAL, null);
		autoEnableParam.setInfo("Controls whether the automatic system generates forecasts");
		autoEnableParam.addParameterChangeListener(this);
		return autoEnableParam;
	}


	// Option to supply custom parameters to automatic system; check box.
	// Default is obtained from generic parameters when the data is loaded (typically true).

	private BooleanParameter useCustomParamsParam;

	private BooleanParameter init_useCustomParamsParam () throws GUIEDTException {
		useCustomParamsParam = new BooleanParameter("Use custom parameters", true);
		useCustomParamsParam.addParameterChangeListener(this);
		return useCustomParamsParam;
	}


	// Forecast duration, in days since the mainshock; edit box containing a number.
	// Default is obtained from the action configuratin, typically 365.0.

	private DoubleParameter forecastDurationParam;

	private DoubleParameter init_forecastDurationParam () throws GUIEDTException {
		double duration_min = gui_model.get_min_fc_duration_days();
		double duration_max = gui_model.get_max_fc_duration_days();
		double duration_def = gui_model.get_def_fc_duration_days();

		forecastDurationParam = new DoubleParameter("Forecast Duration", duration_min, duration_max, new Double(duration_def));
		forecastDurationParam.setUnits("Days");
		forecastDurationParam.setInfo("Forecast duration relative to main shock origin time");
		forecastDurationParam.addParameterChangeListener(this);
		return forecastDurationParam;
	}


	// Set Injectable Text button.

	private ButtonParameter setInjTextButton;

	private ButtonParameter init_setInjTextButton () throws GUIEDTException {
		setInjTextButton = new ButtonParameter("Injectable Text", "Set Text...");
		setInjTextButton.addParameterChangeListener(this);
		return setInjTextButton;
	}


	// Export Analyst Options button.

	private JFileChooser exportAnalystOptionsChooser;		// the file chooser dialog, lazy-allocated
	private ButtonParameter exportAnalystOptionsButton;

	private ButtonParameter init_exportAnalystOptionsButton () throws GUIEDTException {
		exportAnalystOptionsChooser = null;
		exportAnalystOptionsButton = new ButtonParameter("Export Analyst Options", "Export to JSON...");
		exportAnalystOptionsButton.addParameterChangeListener(this);
		return exportAnalystOptionsButton;
	}


	// Send Analyst Options to Server button.

	private ButtonParameter sendAnalystOptionsButton;

	private ButtonParameter init_sendAnalystOptionsButton () throws GUIEDTException {
		sendAnalystOptionsButton = new ButtonParameter("Send Analyst Options", "Send to Server...");
		sendAnalystOptionsButton.addParameterChangeListener(this);
		return sendAnalystOptionsButton;
	}




	//----- Control containers -----


	// Container for data parameters.

	private ParameterListEditor dataEditor;

	public final ParameterListEditor get_dataEditor () {
		return dataEditor;
	}

	private ParameterListEditor init_dataEditor () throws GUIEDTException {

		// Controls in the "Data Parameters" column

		ParameterList dataParams = new ParameterList();
		
		dataParams.addParameter(init_eventIDParam());
		
		dataParams.addParameter(init_dataStartTimeParam());
		
		dataParams.addParameter(init_dataEndTimeParam());
		
		dataParams.addParameter(init_regionTypeParam());

		dataParams.addParameter(init_regionEditParam());
		
		dataParams.addParameter(init_fetchButton());
		
		dataParams.addParameter(init_loadCatalogButton());
		
		dataParams.addParameter(init_saveCatalogButton());
		
		dataParams.addParameter(init_mcParam());
		
		dataParams.addParameter(init_magPrecisionParam());
		
		dataParams.addParameter(init_computeBButton());
		
		dataParams.addParameter(init_bParam());

		// Create the container

		dataEditor = new ParameterListEditor(dataParams);
		dataEditor.setTitle("Data Parameters");
		dataEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), gui_top.get_height()));
		return dataEditor;
	}


	// Container for aftershock parameters.

	private ParameterListEditor fitEditor;

	public final ParameterListEditor get_fitEditor () {
		return fitEditor;
	}

	private ParameterListEditor init_fitEditor () throws GUIEDTException {

		// Controls in the "Aftershock Parameters" column
		
		ParameterList fitParams = new ParameterList();
		
		fitParams.addParameter(init_aValRangeParam());
		
		fitParams.addParameter(init_aValNumParam());
		
		fitParams.addParameter(init_pValRangeParam());
		
		fitParams.addParameter(init_pValNumParam());
		
		fitParams.addParameter(init_cValRangeParam());
		
		fitParams.addParameter(init_cValNumParam());
		
		fitParams.addParameter(init_timeDepMcParam());
		
		fitParams.addParameter(init_fParam());
		
		fitParams.addParameter(init_gParam());
		
		fitParams.addParameter(init_hParam());
		
		fitParams.addParameter(init_mCatParam());
		
		fitParams.addParameter(init_computeAftershockParamsButton());
		
		fitParams.addParameter(init_aValParam());
		
		fitParams.addParameter(init_pValParam());
		
		fitParams.addParameter(init_cValParam());

		// Create the container

		fitEditor = new ParameterListEditor(fitParams);
		fitEditor.setTitle("Aftershock Parameters");
		fitEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), gui_top.get_height()));
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
		
		fcastParams.addParameter(init_forecastStartTimeNowParam());
		
		fcastParams.addParameter(init_forecastStartTimeParam());
		
		fcastParams.addParameter(init_forecastEndTimeParam());
		
		fcastParams.addParameter(init_computeAftershockForecastButton());

		// Create the container

		fcastEditorHeight = (gui_top.get_height() * 4) / 11;

		fcastEditor = new ParameterListEditor(fcastParams);
		fcastEditor.setTitle("Forecasts");
		fcastEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), fcastEditorHeight));
		return fcastEditor;
	}


	// Container for AAFS parameters.

	private ParameterListEditor aafsEditor;

	public final ParameterListEditor get_aafsEditor () {
		return aafsEditor;
	}

	private int aafsEditorHeight;

	private ParameterListEditor init_aafsEditor () throws GUIEDTException {

		// Controls in the "Automatic System" column
		
		ParameterList aafsParams = new ParameterList();
		
		aafsParams.addParameter(init_fetchServerStatusButton());
		
		aafsParams.addParameter(init_autoEnableParam());
		
		aafsParams.addParameter(init_useCustomParamsParam());
		
		aafsParams.addParameter(init_forecastDurationParam());
		
		aafsParams.addParameter(init_setInjTextButton());
		
		aafsParams.addParameter(init_exportAnalystOptionsButton());
		
		aafsParams.addParameter(init_sendAnalystOptionsButton());

		// Create the container

		aafsEditorHeight = gui_top.get_height() - fcastEditorHeight;

		aafsEditor = new ParameterListEditor(aafsParams);
		aafsEditor.setTitle("Automatic System");
		aafsEditor.setPreferredSize(new Dimension(gui_top.get_paramWidth(), aafsEditorHeight));
		return aafsEditor;
	}




	//----- Debugging support -----


	// Add all parameters to the symbol table.

	private void setup_symbol_table () {

		// Set up the symbol table

		add_symbol (eventIDParam , "eventIDParam");
		add_symbol (dataStartTimeParam , "dataStartTimeParam");
		add_symbol (dataEndTimeParam , "dataEndTimeParam");
		add_symbol (regionTypeParam , "regionTypeParam");

		add_symbol (radiusParam , "radiusParam");
		add_symbol (minLatParam , "minLatParam");
		add_symbol (maxLatParam , "maxLatParam");
		add_symbol (minLonParam , "minLonParam");
		add_symbol (maxLonParam , "maxLonParam");
		add_symbol (minDepthParam , "minDepthParam");
		add_symbol (maxDepthParam , "maxDepthParam");
		add_symbol (wcMultiplierParam , "wcMultiplierParam");
		add_symbol (minRadiusParam , "minRadiusParam");
		add_symbol (maxRadiusParam , "maxRadiusParam");
		add_symbol (centerLatParam , "centerLatParam");
		add_symbol (centerLonParam , "centerLonParam");

		add_symbol (regionList , "regionList");
		add_symbol (regionEditParam , "regionEditParam");
		add_symbol (fetchButton , "fetchButton");
		add_symbol (loadCatalogButton , "loadCatalogButton");
		add_symbol (saveCatalogButton , "saveCatalogButton");
		add_symbol (mcParam , "mcParam");
		add_symbol (magPrecisionParam , "magPrecisionParam");
		add_symbol (computeBButton , "computeBButton");
		add_symbol (bParam , "bParam");

		add_symbol (aValRangeParam , "aValRangeParam");
		add_symbol (aValNumParam , "aValNumParam");
		add_symbol (pValRangeParam , "pValRangeParam");
		add_symbol (pValNumParam , "pValNumParam");
		add_symbol (cValRangeParam , "cValRangeParam");
		add_symbol (cValNumParam , "cValNumParam");
		add_symbol (timeDepMcParam , "timeDepMcParam");
		add_symbol (fParam , "fParam");
		add_symbol (gParam , "gParam");
		add_symbol (hParam , "hParam");
		add_symbol (mCatParam , "mCatParam");
		add_symbol (computeAftershockParamsButton , "computeAftershockParamsButton");
		add_symbol (aValParam , "aValParam");
		add_symbol (pValParam , "pValParam");
		add_symbol (cValParam , "cValParam");
		add_symbol (forecastStartTimeParam , "forecastStartTimeParam");
		add_symbol (forecastEndTimeParam , "forecastEndTimeParam");
		add_symbol (forecastStartTimeNowParam , "forecastStartTimeNowParam");
		add_symbol (computeAftershockForecastButton , "computeAftershockForecastButton");

		add_symbol (fetchServerStatusButton , "fetchServerStatusButton");
		add_symbol (autoEnableParam , "autoEnableParam");
		add_symbol (useCustomParamsParam , "useCustomParamsParam");
		add_symbol (forecastDurationParam , "forecastDurationParam");
		add_symbol (setInjTextButton , "setInjTextButton");
		add_symbol (exportAnalystOptionsButton , "exportAnalystOptionsButton");
		add_symbol (sendAnalystOptionsButton , "sendAnalystOptionsButton");

		add_symbol (dataEditor , "dataEditor");
		add_symbol (fitEditor , "fitEditor");
		add_symbol (fcastEditor , "fcastEditor");
		add_symbol (aafsEditor , "aafsEditor");

		return;
	}




	//----- Control enable/disable -----




	// Adjust the enable/disable state of all controls, according to the supplied parameters.
	// Parameters:
	//  f_catalog = Catalog is available.
	//  f_params = Aftershock parameters are available.
	//  f_forecast = Forecast is available.
	//  f_edit_region = Edit region option is available.
	//  f_time_dep_mc = Time dependent parameters are being used.
	//  f_fetched = Catalog is available, and data was fetched from Comcat.
	// Note: Controls are enabled by default, so controls that are always
	// enabled do not need to be mentioned.
	// Note: Also clears some controls, according to the supplied parameters.

	private void adjust_enable (boolean f_catalog, boolean f_params, boolean f_forecast, boolean f_edit_region, boolean f_time_dep_mc, boolean f_fetched) throws GUIEDTException {

		// Data parameters that are always enabled

		enableParam(eventIDParam, true);
		enableParam(dataStartTimeParam, true);
		enableParam(dataEndTimeParam, true);
		enableParam(regionTypeParam, true);
		enableParam(regionEditParam, f_edit_region);
		enableParam(fetchButton, true);
		enableParam(loadCatalogButton, true);

		// Data parameters that become enabled when the catalog is loaded

		enableParam(saveCatalogButton, f_catalog);
		enableParam(mcParam, f_catalog);
		enableParam(magPrecisionParam, f_catalog);
		enableParam(computeBButton, f_catalog);
		enableParam(bParam, f_catalog);

		// Aftershock parameters that become enabled when the catalog is loaded

		enableParam(aValRangeParam, f_catalog);
		enableParam(aValNumParam, f_catalog);
		enableParam(pValRangeParam, f_catalog);
		enableParam(pValNumParam, f_catalog);
		enableParam(cValRangeParam, f_catalog);
		enableParam(cValNumParam, f_catalog);
		enableParam(timeDepMcParam, f_catalog);
		
		// Aftershock parameters that are enabled when the catalog is loaded, and time-dependent Mc is used

		enableParam(fParam, f_catalog && f_time_dep_mc);
		enableParam(gParam, f_catalog && f_time_dep_mc);
		enableParam(hParam, f_catalog && f_time_dep_mc);
		enableParam(mCatParam, f_catalog && f_time_dep_mc);

		// Aftershock parameters that become enabled when the catalog is loaded

		enableParam(computeAftershockParamsButton, f_catalog);

		// Aftershock parameters that are never enabled

		enableParam(aValParam, false);
		enableParam(pValParam, false);
		enableParam(cValParam, false);

		// Aftershock parameters that become enabled when parameters have been computed

		enableParam(forecastStartTimeNowParam, f_catalog && f_params);
		enableParam(forecastStartTimeParam, f_catalog && f_params);
		enableParam(forecastEndTimeParam, f_catalog && f_params);
		enableParam(computeAftershockForecastButton, f_catalog && f_params);

		// Analyst parameters

		enableParam(fetchServerStatusButton, true);
		enableParam(autoEnableParam, f_catalog && f_params && f_fetched);
		enableParam(useCustomParamsParam, f_catalog && f_params && f_fetched);
		enableParam(forecastDurationParam, f_catalog && f_params && f_fetched);
		enableParam(setInjTextButton, f_catalog && f_params && f_fetched);
		enableParam(exportAnalystOptionsButton, f_catalog && f_params && f_fetched);
		enableParam(sendAnalystOptionsButton, f_catalog && f_params && f_fetched);

		if (!( f_catalog && f_params && f_fetched )) {
			updateParam(autoEnableParam, AutoEnable.NORMAL);
			updateParam(useCustomParamsParam, true);
			updateParam(forecastDurationParam, gui_model.get_def_fc_duration_days());
		}

		// Data parameters that are cleared when there is no catalog loaded

		if (!( f_catalog )) {
			updateParam(mcParam, null);
			updateParam(bParam, null);
		}

		// Aftershock parameters that are cleared when there is no catalog loaded

		if (!( f_catalog )) {
			updateParam(aValRangeParam, null);
			updateParam(aValNumParam, null);
			updateParam(pValRangeParam, null);
			updateParam(pValNumParam, null);
			updateParam(cValRangeParam, null);
			updateParam(cValNumParam, null);
			updateParam(fParam, null);
			updateParam(gParam, null);
			updateParam(hParam, null);
			updateParam(mCatParam, null);
		}

		// Aftershock parameters that are cleared when parameters have not been computed

		if (!( f_catalog && f_params )) {
			updateParam(aValParam, null);
			updateParam(pValParam, null);
			updateParam(cValParam, null);
		}

		return;
	}




	// Adjust the enable/disable state of all controls, according to the current state.

	private void adjust_enable () throws GUIEDTException {
		boolean f_catalog = gui_model.modstate_has_catalog();
		boolean f_params = gui_model.modstate_has_aftershock_params();
		boolean f_forecast = gui_model.modstate_has_forecast();
		boolean f_edit_region = (validParam(regionTypeParam) != RegionType.STANDARD);
		boolean f_time_dep_mc = validParam(timeDepMcParam);
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

	public static class XferCatalogView {

		// Flag, true if we have the fetch parameters (event ID and edit region).

		public boolean has_fetch_params;

		// Event ID, present if has_fetch_params is true.

		public String x_eventIDParam;				// parameter value, checked for validity

		// Data start time, in days since the mainshock.

		public double x_dataStartTimeParam;			// parameter value, checked for validity

		// Data end time, in days since the mainshock.

		public double x_dataEndTimeParam;			// parameter value, checked for validity

		// Search region, present if has_fetch_params is true.
		// Only includes values used for the region type and region center type.
		// Included values are checked for validity

		public RegionType x_regionTypeParam;	// Region type

		public double x_radiusParam;		// Search radius; appears when Region Type is Circular
		public double x_minLatParam;		// Minimum latitude; appears when Region Type is Rectangular
		public double x_maxLatParam;		// Maximum latitude; appears when Region Type is Rectangular
		public double x_minLonParam;		// Minimum longitude; appears when Region Type is Rectangular
		public double x_maxLonParam;		// Maximum longitude; appears when Region Type is Rectangular
		public double x_minDepthParam;		// Minimum depth; always appears
		public double x_maxDepthParam;		// Maximum depth; always appears
		public double x_wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
		public double x_minRadiusParam;		// Minimum radius; appears for WC regions
		public double x_maxRadiusParam;		// Maximum radius; appears for WC regions
		public double x_centerLatParam;		// Circle center latitude; appears for custom circle
		public double x_centerLonParam;		// Circle center longitude; appears for custom circle

		// Mc for sequence, can be null.

		public Double x_mcParam;			// parameter value, checked for null or validity

		// b-value, can be null.

		public Double x_bParam;				// parameter value, checked for null or validity
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferCatalogMod extends XferCatalogView {

		// Event ID.

		public abstract void modify_eventIDParam (String x);

		// Data start time, in days since the mainshock.

		public abstract void modify_dataStartTimeParam (double x);

		// Data end time, in days since the mainshock.

		public abstract void modify_dataEndTimeParam (double x);

		// Mc for sequence, can be null.

		public abstract void modify_mcParam (Double x);

		// b-value, can be null.

		public abstract void modify_bParam (Double x);
	}




	// Transfer parameters for catalog fetch or load.

	public class XferCatalogImpl extends XferCatalogMod {

		// Event ID.

		private boolean dirty_eventIDParam;	// true if needs to be written back

		@Override
		public void modify_eventIDParam (String x) {
			x_eventIDParam = x;
			dirty_eventIDParam = true;
		}

		// Data start time, in days since the mainshock.

		private boolean dirty_dataStartTimeParam;	// true if needs to be written back

		@Override
		public void modify_dataStartTimeParam (double x) {
			x_dataStartTimeParam = x;
			dirty_dataStartTimeParam = true;
		}

		// Data end time, in days since the mainshock.

		private boolean dirty_dataEndTimeParam;		// true if needs to be written back

		@Override
		public void modify_dataEndTimeParam (double x) {
			x_dataEndTimeParam = x;
			dirty_dataEndTimeParam = true;
		}

		// Mc for sequence, can be null.

		private boolean dirty_mcParam;		// true if needs to be written back

		@Override
		public void modify_mcParam (Double x) {
			x_mcParam = x;
			dirty_mcParam = true;
		}

		// b-value, can be null.

		private boolean dirty_bParam;		// true if needs to be written back

		@Override
		public void modify_bParam (Double x) {
			x_bParam = x;
			dirty_bParam = true;
		}


		// Load values.
		// f_fetch is true to load values for a fetch operation

		public XferCatalogImpl xfer_load (boolean f_fetch) {

			has_fetch_params = f_fetch;

			// Event ID

			if (f_fetch) {
				x_eventIDParam = validParam(eventIDParam);
			}
			dirty_eventIDParam = false;

			// Data start and end time

			x_dataStartTimeParam = validParam(dataStartTimeParam);
			dirty_dataStartTimeParam = false;

			x_dataEndTimeParam = validParam(dataEndTimeParam);
			dirty_dataEndTimeParam = false;

			// Search region

			if (f_fetch) {
				x_regionTypeParam = validParam(regionTypeParam);
		
				switch (x_regionTypeParam) {

				case STANDARD:
					// do nothing
					break;

				case CENTROID_WC_CIRCLE:
					x_wcMultiplierParam = validParam(wcMultiplierParam);
					x_minRadiusParam = validParam(minRadiusParam);
					x_maxRadiusParam = validParam(maxRadiusParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				case CENTROID_CIRCLE:
					x_radiusParam = validParam(radiusParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				case EPICENTER_WC_CIRCLE:
					x_wcMultiplierParam = validParam(wcMultiplierParam);
					x_minRadiusParam = validParam(minRadiusParam);
					x_maxRadiusParam = validParam(maxRadiusParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				case EPICENTER_CIRCLE:
					x_radiusParam = validParam(radiusParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				case CUSTOM_CIRCLE:
					x_radiusParam = validParam(radiusParam);
					x_centerLatParam = validParam(centerLatParam);
					x_centerLonParam = validParam(centerLonParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				case CUSTOM_RECTANGLE:
					x_minLatParam = validParam(minLatParam);
					x_maxLatParam = validParam(maxLatParam);
					x_minLonParam = validParam(minLonParam);
					x_maxLonParam = validParam(maxLonParam);
					x_minDepthParam = validParam(minDepthParam);
					x_maxDepthParam = validParam(maxDepthParam);
					break;

				default:
					throw new IllegalStateException("Unknown region type: " + x_regionTypeParam);
				}
			}

			// Mc for sequence

			x_mcParam = null;
			if (nonNullParam(mcParam)) {
				x_mcParam = validParam(mcParam);
			}
			dirty_mcParam = false;

			// b-value

			x_bParam = null;
			if (nonNullParam(bParam)) {
				x_bParam = validParam(bParam);
			}
			dirty_bParam = false;

			return this;
		}


		// Store modified values back into the parameters.

		public void xfer_store () throws GUIEDTException {

			// Event ID

			if (dirty_eventIDParam) {
				dirty_eventIDParam = false;
				updateParam(eventIDParam, x_eventIDParam);
			}

			// Data start and end time

			if (dirty_dataStartTimeParam) {
				dirty_dataStartTimeParam = false;
				updateParam(dataStartTimeParam, x_dataStartTimeParam);
			}

			if (dirty_dataEndTimeParam) {
				dirty_dataEndTimeParam = false;
				updateParam(dataEndTimeParam, x_dataEndTimeParam);
			}

			// Mc for sequence

			if (dirty_mcParam) {
				dirty_mcParam = false;
				updateParam(mcParam, x_mcParam);
			}

			// b-value

			if (dirty_bParam) {
				dirty_bParam = false;
				updateParam(bParam, x_bParam);
			}

			return;
		}
	}




	// Update parameters after catalog fetch or load.
	// The purpose is to initialize parameters for aftershock parameter computation.

	private void post_fetch_param_update () throws GUIEDTException {

		// Initialize aftershock parameters from the model or parameters

		updateParam(aValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_a(), gui_model.get_seqSpecParams().get_max_a()));
		updateParam(aValNumParam, gui_model.get_seqSpecParams().get_num_a());
		updateParam(pValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_p(), gui_model.get_seqSpecParams().get_max_p()));
		updateParam(pValNumParam, gui_model.get_seqSpecParams().get_num_p());
		updateParam(cValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_c(), gui_model.get_seqSpecParams().get_max_c()));
		updateParam(cValNumParam, gui_model.get_seqSpecParams().get_num_c());

		//updateParam(bParam, gui_model.get_seqSpecParams().get_b());		// done in the model

		updateParam(timeDepMcParam, !(gui_model.get_magCompParams().get_magCompFn().is_constant()));
		updateParam(fParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapF());
		updateParam(gParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapG());
		updateParam(hParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapH());
		updateParam(mCatParam, gui_model.get_magCompParams().get_magCat());

		return;
	}


//	private void post_fetch_param_update () throws GUIEDTException {
//
//		// Initialize aftershock parameters from the model or parameters
//		// Note: The a-value parameters are taken from the generic model instead of the
//		// generic parameters, because the model can slightly alter the a-value parameters.
//
//		updateParam(aValRangeParam, new Range(gui_model.get_genericModel().getMin_a(), gui_model.get_genericModel().getMax_a()));
//		updateParam(aValNumParam, gui_model.get_genericModel().getNum_a());
//		updateParam(pValRangeParam, new Range(gui_model.get_genericParams().get_pValue(), gui_model.get_genericParams().get_pValue()));
//		updateParam(pValNumParam, 1);
//		updateParam(cValRangeParam, new Range(gui_model.get_genericParams().get_cValue(), gui_model.get_genericParams().get_cValue()));
//		updateParam(cValNumParam, 1);
//
//		//updateParam(bParam, gui_model.get_genericModel().get_b());		// done in the model
//
//		updateParam(timeDepMcParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapG() <= 99.999);
//		updateParam(fParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapF());
//		updateParam(gParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapG());
//		updateParam(hParam, gui_model.get_magCompParams().get_magCompFn().getDefaultGUICapH());
//		updateParam(mCatParam, gui_model.get_magCompParams().get_magCat());
//
//		return;
//	}




	//----- Parameter handling for aftershock parameter fitting -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static class XferFittingView {

		// Data start time, in days since the mainshock.

		public double x_dataStartTimeParam;		// parameter value, checked for validity

		// Data end time, in days since the mainshock.

		public double x_dataEndTimeParam;		// parameter value, checked for validity

		// Mc for sequence, can be null.

		public double x_mcParam;				// parameter value, checked for validity

		// b-value, can be null.

		public double x_bParam;					// parameter value, checked for validity

		// Range of a-values.
	
		public Range x_aValRangeParam;			// parameter value, checked for validity

		// Number of a-values.

		public int x_aValNumParam;				// parameter value, checked for validity

		// Range of p-values.
	
		public Range x_pValRangeParam;			// parameter value, checked for validity

		// Number of p-values.

		public int x_pValNumParam;				// parameter value, checked for validity

		// Range of c-values.
	
		public Range x_cValRangeParam;			// parameter value, checked for validity

		// Number of c-values.

		public int x_cValNumParam;				// parameter value, checked for validity

		// Option to apply time-dependent Mc.

		public boolean x_timeDepMcParam;		// parameter value, checked for validity

		// Helmstetter F parameter, present if using time-dependent Mc.

		public double x_fParam;					// parameter value, checked for validity

		// Helmstetter G parameter, present if using time-dependent Mc.

		public double x_gParam;					// parameter value, checked for validity

		// Helmstetter H parameter, present if using time-dependent Mc.

		public double x_hParam;					// parameter value, checked for validity

		// Magnitude of completeness, present if using time-dependent Mc.

		public double x_mCatParam;				// parameter value, checked for validity
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferFittingMod extends XferFittingView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferFittingImpl extends XferFittingMod {


		// Load values.

		public XferFittingImpl xfer_load () {

			// Data start and end time

			x_dataStartTimeParam = validParam(dataStartTimeParam);

			x_dataEndTimeParam = validParam(dataEndTimeParam);

			// Mc for sequence

			x_mcParam = validParam(mcParam);

			// b-value

			x_bParam = validParam(bParam);

			// a-values

			x_aValRangeParam = validParam(aValRangeParam);
			x_aValNumParam = validParam(aValNumParam);
			validateRange(x_aValRangeParam, x_aValNumParam, "a-value");

			// p-values

			x_pValRangeParam = validParam(pValRangeParam);
			x_pValNumParam = validParam(pValNumParam);
			validateRange(x_pValRangeParam, x_pValNumParam, "p-value");

			// c-values

			x_cValRangeParam = validParam(cValRangeParam);
			x_cValNumParam = validParam(cValNumParam);
			validateRange(x_cValRangeParam, x_cValNumParam, "c-value");

			int max_grid = 100000;
			max_grid = max_grid / x_aValNumParam;
			max_grid = max_grid / x_pValNumParam;
			max_grid = max_grid / x_cValNumParam;
			Preconditions.checkState(max_grid > 0, "Parameter search grid exceeds 100,000 entries");

			// Option for time-dependent Mc

			x_timeDepMcParam = validParam(timeDepMcParam);

			// Parameters for time-dependent Mc

			if (x_timeDepMcParam) {
				x_fParam = validParam(fParam);
				x_gParam = validParam(gParam);
				x_hParam = validParam(hParam);
				x_mCatParam = validParam(mCatParam);
			}

			return this;
		}


		// Store modified values back into the parameters.

		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	// Update parameters after aftershock parameter fitting.

	private void post_fitting_param_update () throws GUIEDTException {

		// Display the maximum likelihood parameter values

		updateParam(aValParam, gui_model.get_cur_model().getMaxLikelihood_a());
		updateParam(pValParam, gui_model.get_cur_model().getMaxLikelihood_p());
		updateParam(cValParam, gui_model.get_cur_model().getMaxLikelihood_c());

		return;
	}




	//----- Parameter handling for aftershock parameter fitting -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static class XferForecastView {

		// Forecast start time, in days since the mainshock.

		public double x_forecastStartTimeParam;	// parameter value, checked for validity

		// Forecast start time, in days since the mainshock.

		public double x_forecastEndTimeParam;	// parameter value, checked for validity
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferForecastMod extends XferForecastView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferForecastImpl extends XferForecastMod {


		// Load values.

		public XferForecastImpl xfer_load () {

			// Forecast start and end time

			x_forecastStartTimeParam = validParam(forecastStartTimeParam);

			x_forecastEndTimeParam = validParam(forecastEndTimeParam);

			return this;
		}


		// Store modified values back into the parameters.

		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	// Update parameters after aftershock parameter fitting.

	private void post_forecast_update () throws GUIEDTException {

		return;
	}




	//----- Parameter handling for analyst options -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static class XferAnalystView {

		// Forecast generation option.

		public AutoEnable x_autoEnableParam;	// parameter value, checked for validity

		// Flag to use custom parameters.

		public boolean x_useCustomParamsParam;	// parameter value, checked for validity

		// Forecast duration, in days since the mainshock.

		public double x_forecastDuration;		// parameter value, checked for validity
	}




	// Class to view or modify relevant parameters.
	// Modifications act on the copies of parameters, hence EDT is not required.

	public static abstract class XferAnalystMod extends XferAnalystView {

	}




	// Transfer parameters for catalog fetch or load.

	public class XferAnalystImpl extends XferAnalystMod {


		// Load values.

		public XferAnalystImpl xfer_load () {

			// Forecast generation option.

			x_autoEnableParam = validParam(autoEnableParam);
		
			// Flag to use custom parameters.

			x_useCustomParamsParam = validParam(useCustomParamsParam);

			// Forecast duration, in days since the mainshock.

			x_forecastDuration = validParam(forecastDurationParam);

			return this;
		}


		// Store modified values back into the parameters.

		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	// Update parameters after analyst options.

	private void post_analyst_update () throws GUIEDTException {

		return;
	}




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

		// Set up the symbol table

		setup_symbol_table();
		
		// Enable controls

		adjust_enable();

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		

		// Event ID, data start time, data end time, region selection.
		// - Dump any catalog that has been fetched.
		
		if (param == eventIDParam || param == dataStartTimeParam || param == dataEndTimeParam
				|| param == regionEditParam) {
			if (filter_state(MODSTATE_INITIAL)) {
				return;
			}


		// Region type.
		// - Update the edit region dialog, to include the controls for the selected region type.
		// - Dump any catalog that has been fetched.

		} else if (param == regionTypeParam) {
			updateRegionParamList(validParam(regionTypeParam));
			if (filter_state(MODSTATE_INITIAL, FILTOPT_REPAINT)) {
				return;
			}


		// Fetch catalog button.
		// - Dump any catalog that has been fetched.
		// - In backgoung:
		//   1. Fetch the mainshock.
		//   2. Get time and depth intervals.
		//   3. Construct search region and fetch afteshocks.
		//   4. Construct the plots, and enable controls for the catalog.

		} else if (param == fetchButton) {

			if (filter_state(MODSTATE_INITIAL)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferCatalogImpl xfer_catalog_impl = new XferCatalogImpl();
			GUICalcStep fetchStep_1 = new GUICalcStep("Fetching Events",
				"Contacting USGS ComCat Webservice. This is occasionally slow. "
				+ "If it fails, trying again often works.", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_catalog_impl.xfer_load(true);
				}
			}, true);
			GUICalcStep fetchStep_2 = new GUICalcStep("Fetching Events",
				"Contacting USGS ComCat Webservice. This is occasionally slow. "
				+ "If it fails, trying again often works.", new Runnable() {
						
				@Override
				public void run() {
					gui_model.fetchEvents(xfer_catalog_impl);
				}
			}, gui_top.get_forceWorkerEDT());
			GUICalcStep postFetchPlotStep = new GUICalcStep("Plotting Events/Data", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_catalog_impl.xfer_store();
					advance_state(MODSTATE_CATALOG);
					post_fetch_param_update();
				}
			}, true);
			GUICalcRunnable run = new GUICalcRunnable(progress, fetchStep_1, fetchStep_2, postFetchPlotStep);
			new Thread(run).start();


		// Load catalog button.
		// - Display the file chooser.
		// - Dump any catalog that has been fetched.
		// - In backgound:
		//   1. Read catalog from file.
		//   2. Construct the plots, and enable controls for the catalog.

		} else if (param == loadCatalogButton) {
			if (filter_state(MODSTATE_INITIAL, FILTOPT_TEST)) {
				return;
			}
			if (loadCatalogChooser == null)
				loadCatalogChooser = new JFileChooser();
			int ret = loadCatalogChooser.showOpenDialog(gui_top.get_top_window());
			if (ret == JFileChooser.APPROVE_OPTION) {
				final File file = loadCatalogChooser.getSelectedFile();

				if (filter_state(MODSTATE_INITIAL)) {
					return;
				}
				final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
				final XferCatalogImpl xfer_catalog_impl = new XferCatalogImpl();
				GUICalcStep loadStep_1 = new GUICalcStep("Loading Events", "...", new GUIEDTRunnable() {
					
						@Override
						public void run_in_edt() throws GUIEDTException {
							xfer_catalog_impl.xfer_load(false);
						}
					}, true);
				GUICalcStep loadStep_2 = new GUICalcStep("Loading Events", "...", new Runnable() {
						
						@Override
						public void run() {
							gui_model.loadCatalog(xfer_catalog_impl, file);
						}
					}, gui_top.get_forceWorkerEDT());
				GUICalcStep postFetchPlotStep = new GUICalcStep("Plotting Events/Data", "...", new GUIEDTRunnable() {
					
						@Override
						public void run_in_edt() throws GUIEDTException {
							xfer_catalog_impl.xfer_store();
							advance_state(MODSTATE_CATALOG);
							post_fetch_param_update();
						}
					}, true);
				GUICalcRunnable run = new GUICalcRunnable(progress, loadStep_1, loadStep_2, postFetchPlotStep);
				new Thread(run).start();
			}


		// Save catalog button.
		// - Display the file chooser.
		// - Write the catalog file.

		} else if (param == saveCatalogButton) {
			if (filter_state(MODSTATE_CATALOG, FILTOPT_TEST)) {
				return;
			}
			if (saveCatalogChooser == null)
				saveCatalogChooser = new JFileChooser();
			int ret = saveCatalogChooser.showSaveDialog(gui_top.get_top_window());
			if (ret == JFileChooser.APPROVE_OPTION) {

				//  FileWriter fw = null;
				//  try {
				//  	File file = saveCatalogChooser.getSelectedFile();
				//  	fw = new FileWriter(file);
				//  	fw.write(gui_view.get_catalog_text());
				//  } catch (IOException e) {
				//  	e.printStackTrace();
				//  	JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
				//  			"Error Saving Catalog", JOptionPane.ERROR_MESSAGE);
				//  } finally {
				//  	if (fw != null) {
				//  		try {
				//  			fw.close();
				//  		} catch (IOException e) {
				//  			e.printStackTrace();
				//  		}
				//  	}
				//  }

				try {
					File file = saveCatalogChooser.getSelectedFile();
					GUIExternalCatalog ext_cat = new GUIExternalCatalog();
					ext_cat.setup_catalog (
						gui_model.get_cur_aftershocks(),
						gui_model.get_cur_mainshock()
					);
					ext_cat.write_to_file (file);
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
							"Error Saving Catalog", JOptionPane.ERROR_MESSAGE);
				}
			}


		// Magnitude of completeness.
		// - Dump any aftershock parameters that have been computed.
		// - Set b-parameter to default from generic model, or unspecified if none.
		// - If b-value was changed, re-plot the magnitude-frequency distribution.
		// - Re-plot the cumulative number plot (which only show earthquakes above Mc).

		} else if (param == mcParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}

			if (definedParam(mcParam)) {
				gui_model.change_mcParam(validParam(mcParam));
			}


		// Precision of magnitudes in the catalog.
		// - Dump any aftershock parameters that have been computed.
		// - Set b-parameter to default from generic model, or unspecified if none.
		// - If b-value was changed, re-plot the magnitude-frequency distribution.

		} else if (param == magPrecisionParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}


		// Compute b-value button.
		// - Dump any aftershock parameters that have been computed.
		// - In backgound:
		//   1. Compute maximum likelhood b-value, for aftershocks above Mc with specified precision.
		//   2. Re-plot the magnitude-frequency distribution, and set it as the selected tab.

		} else if (param == computeBButton) {

			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final double[] b = new double[1];
			GUICalcStep bStep_1 = new GUICalcStep("Computing b", "...", new Runnable() {
				
				@Override
				public void run() {
					double mc = validParam(mcParam);
					
					double magPrecision = validParam(magPrecisionParam);
					
					ObsEqkRupList filteredRupList = gui_model.get_cur_aftershocks().getRupsAboveMag(mc);
					b[0] = AftershockStatsCalc.getMaxLikelihood_b_value(filteredRupList, mc, magPrecision);
					System.out.println("Num rups \u2265 Mc = "+filteredRupList.size());
					System.out.println("Computed b-value: "+b[0]);
				}
			}, gui_top.get_forceWorkerEDT());
			GUICalcStep bStep_2 = new GUICalcStep("Computing b", "...", new GUIEDTRunnable() {
				
				@Override
				public void run_in_edt() throws GUIEDTException {
					updateParam(bParam, b[0]);

					if (definedParam(bParam)) {
						gui_model.change_bParam(validParam(bParam));
					}
				}
			}, true);
			GUICalcRunnable run = new GUICalcRunnable(progress, bStep_1, bStep_2);
			new Thread(run).start();


		// b-value.
		// - Dump any aftershock parameters that have been computed.
		// - Re-plot the magnitude-frequency distribution.

		} else if (param == bParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}

			if (definedParam(bParam)) {
				gui_model.change_bParam(validParam(bParam));
			}


		// a-value range or number.
		// - Adjust range and number to be consistent with each other.
		// - Dump any aftershock parameters that have been computed.

		} else if (param == aValRangeParam || param == aValNumParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			updateRangeParams(aValRangeParam, aValNumParam, 51);


		// p-value range or number.
		// - Adjust range and number to be consistent with each other.
		// - Dump any aftershock parameters that have been computed.

		} else if (param == pValRangeParam || param == pValNumParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			updateRangeParams(pValRangeParam, pValNumParam, 45);


		// c-value range or number.
		// - Adjust range and number to be consistent with each other.
		// - Dump any aftershock parameters that have been computed.

		} else if (param == cValRangeParam || param == cValNumParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			updateRangeParams(cValRangeParam, cValNumParam, 45);


		// Use time-dependent magnitude of completeness option.
		// - Dump any aftershock parameters that have been computed, and force contol enable/disable to be plotted.

		} else if (param == timeDepMcParam) {
			if (filter_state(MODSTATE_CATALOG, FILTOPT_REPAINT)) {
				return;
			}


		// F-value, G-value, H-value, or magCat-value.
		// - Dump any aftershock parameters that have been computed.

		} else if (param == fParam || param == gParam || param == hParam || param == mCatParam) {
			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}


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

		} else if (param == computeAftershockParamsButton) {

			if (filter_state(MODSTATE_CATALOG)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferFittingImpl xfer_fitting_impl = new XferFittingImpl();
			GUICalcStep computeStep_1 = new GUICalcStep("Computing Aftershock Params", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_fitting_impl.xfer_load();
				}
			}, true);
			GUICalcStep computeStep_2 = new GUICalcStep("Computing Aftershock Params", "...", new Runnable() {
						
				@Override
				public void run() {
					gui_model.fitParams(xfer_fitting_impl);
				}
			}, gui_top.get_forceWorkerEDT());
			GUICalcStep postComputeStep = new GUICalcStep("Plotting Model PDFs", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_fitting_impl.xfer_store();
					advance_state(MODSTATE_PARAMETERS);
					post_fitting_param_update();
				}
			}, true);
			GUICalcRunnable run = new GUICalcRunnable(progress, computeStep_1, computeStep_2, postComputeStep);
			new Thread(run).start();


		// Button to set forecast time to now.
		// - Dump any forecast that has been computed.
		// - Update forecast start time to now.
		// - Update forecast end time to preserve the delta.

		} else if (param == forecastStartTimeNowParam) {
			if (filter_state(MODSTATE_PARAMETERS)) {
				return;
			}
			SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
			Instant now = Instant.now();
			System.out.println("Computing delta from mainshock time ("
					+df.format(new Date (gui_model.get_cur_mainshock().getOriginTime()))+") to now ("+df.format(Date.from(now))+")");
			double delta = USGS_AftershockForecast.getDateDelta(Instant.ofEpochMilli(gui_model.get_cur_mainshock().getOriginTime()), now);
			System.out.println("Delta: "+delta+" days");
			double prevDiff = validParam(forecastEndTimeParam) - validParam(forecastStartTimeParam);
			if (prevDiff <= 0)
				prevDiff = 7;
			updateParam(forecastStartTimeParam, delta);
			updateParam(forecastEndTimeParam, delta+prevDiff);


		// Button to compute aftershock forecast.
		// - Dump any forecast that has been computed.
		// - In backgound:
		//   1. Calculate expected mag-freq distributions for the models.
		//   2. Plot them as expected number with magnitude >= Mc or Mc(t).
		//   3. Remove the forecast pane from the tabbed panel, if it exists.
		//   4. Calculate the forecast tables.
		//   5. Plot them in the forecast pane.

		} else if (param == computeAftershockForecastButton) {

			if (filter_state(MODSTATE_PARAMETERS)) {
				return;
			}
			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferForecastImpl xfer_forecast_impl = new XferForecastImpl();
			GUICalcStep computeStep_1 = new GUICalcStep("Computing Forecast", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_forecast_impl.xfer_load();
				}
			}, true);
			GUICalcStep computeStep_2 = new GUICalcStep("Computing Forecast", "...", new Runnable() {
						
				@Override
				public void run() {
					gui_model.computeForecasts(progress, xfer_forecast_impl);
				}
			}, gui_top.get_forceWorkerEDT());
			GUICalcStep postComputeStep = new GUICalcStep("Plotting Forecast", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_forecast_impl.xfer_store();
					advance_state(MODSTATE_FORECAST);
					post_forecast_update();
				}
			}, true);
			GUICalcRunnable run = new GUICalcRunnable(progress, computeStep_1, computeStep_2, postComputeStep);
			new Thread(run).start();


		// Button to fetch server status.
		// - In backgound:
		//   1. Switch view to the console.
		//   2. Fetch server status.
		//   3. Pop up a dialog box to report the result.

		} else if (param == fetchServerStatusButton) {

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final int[] status_success = new int[1];
			status_success[0] = 0;
			GUICalcStep computeStep_1 = new GUICalcStep("Fetching AAFS Server Status", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					gui_view.view_show_console();
				}
			}, true);
			GUICalcStep computeStep_2 = new GUICalcStep("Fetching AAFS Server Status", "...", new Runnable() {
						
				@Override
				public void run() {
					status_success[0] = gui_model.fetchServerStatus(progress);
				}
			}, gui_top.get_forceWorkerEDT());
			Runnable postComputeStep = new GUIEDTRunnable() {
						
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
			GUICalcRunnable run = new GUICalcRunnable(progress, computeStep_1, computeStep_2);
			run.set_reporter(postComputeStep);
			new Thread(run).start();


		// Drop-down to select automatic forecast enable.

		} else if (param == autoEnableParam) {
			if (filter_state(MODSTATE_PARAMETERS, FILTOPT_TEST)) {
				return;
			}
			if (!( gui_model.get_has_fetched_catalog() )) {
				return;
			}


		// Boolean to select whether to use custom parameters.

		} else if (param == useCustomParamsParam) {
			if (filter_state(MODSTATE_PARAMETERS, FILTOPT_TEST)) {
				return;
			}
			if (!( gui_model.get_has_fetched_catalog() )) {
				return;
			}


		// Button to edit Injectable text.
		// - Pop up a dialog box to edit the text.

		} else if (param == setInjTextButton) {
			if (filter_state(MODSTATE_PARAMETERS, FILTOPT_TEST)) {
				return;
			}
			if (!( gui_model.get_has_fetched_catalog() )) {
				return;
			}

			//  // Show a dialog containing the existing injectable text
			//  
			//  String prevText = gui_model.get_analyst_inj_text();
			//  if (prevText == null) {	// should never happen
			//  	prevText = "";
			//  }
			//  JTextArea area = new JTextArea(prevText);
			//  Dimension size = new Dimension(300, 200);
			//  area.setPreferredSize(size);
			//  area.setMinimumSize(size);
			//  area.setLineWrap(true);
			//  area.setWrapStyleWord(true);
			//  int ret = JOptionPane.showConfirmDialog(gui_top.get_top_window(), area, "Set Injectable Text", JOptionPane.OK_CANCEL_OPTION);

			// Show a dialog containing the existing injectable text

			String prevText = gui_model.get_analyst_inj_text();
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
				gui_model.setAnalystInjText(text);
			}


		// Export analyst options button.
		// - Display the file chooser.
		// - Write the analyst options.

		} else if (param == exportAnalystOptionsButton) {
			if (filter_state(MODSTATE_PARAMETERS, FILTOPT_TEST)) {
				return;
			}
			if (exportAnalystOptionsChooser == null) {
				exportAnalystOptionsChooser = new JFileChooser();
			}
			int ret = exportAnalystOptionsChooser.showSaveDialog(gui_top.get_top_window());
			if (ret == JFileChooser.APPROVE_OPTION) {
				try {
					File file = exportAnalystOptionsChooser.getSelectedFile();
					final XferAnalystImpl xfer_analyst_impl = new XferAnalystImpl();
					xfer_analyst_impl.xfer_load();
					gui_model.exportAnalystOptions (xfer_analyst_impl, file);
					xfer_analyst_impl.xfer_store();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(gui_top.get_top_window(), e.getMessage(),
							"Error Exporting Analyst Options", JOptionPane.ERROR_MESSAGE);
				}
			}


		// Button to send analyst options to server.
		// - Ask user for confirmation.
		// - In backgound:
		//   1. Send analyst options to server.
		//   2. Pop up a dialog box to report the result.

		} else if (param == sendAnalystOptionsButton) {
			if (filter_state(MODSTATE_PARAMETERS, FILTOPT_TEST)) {
				return;
			}
			if (exportAnalystOptionsChooser == null) {
				exportAnalystOptionsChooser = new JFileChooser();
			}

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(gui_top.get_top_window(), "Type \"AAFS\" and press OK to send analyst options to server", "Confirm sending analyst options", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("AAFS"))) {
				JOptionPane.showMessageDialog(gui_top.get_top_window(), "Canceled: Analyst options have NOT been sent to server", "Send canceled", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferAnalystImpl xfer_analyst_impl = new XferAnalystImpl();
			final boolean[] send_success = new boolean[1];
			send_success[0] = false;
			GUICalcStep computeStep_1 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_analyst_impl.xfer_load();
				}
			}, true);
			GUICalcStep computeStep_2 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new Runnable() {
						
				@Override
				public void run() {
					send_success[0] = gui_model.sendAnalystOptions(progress, xfer_analyst_impl);
				}
			}, gui_top.get_forceWorkerEDT());
			GUICalcStep computeStep_3 = new GUICalcStep("Sending Analyst Options to AAFS Server", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_analyst_impl.xfer_store();
				}
			}, true);
			Runnable postComputeStep = new GUIEDTRunnable() {
						
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
			GUICalcRunnable run = new GUICalcRunnable(progress, computeStep_1, computeStep_2, computeStep_3);
			run.set_reporter(postComputeStep);
			new Thread(run).start();


		}

		return;
	}




	// This function is called when there is a change in one of the grid range
	// parameter pairs, either the range (lower/upper bounds) or the number.
	// If the range is non-empty but the number is 1, then the number is set to defaultNum.
	// If the range is empty but the number is > 1, then the number is set to 1.
	// Otherwise, or if the range is invalid, then do nothing.
	
	private void updateRangeParams(RangeParameter rangeParam, IntegerParameter numParam, int defaultNum) throws GUIEDTException {
		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ updateRangeParams (" + get_symbol(rangeParam) + ", " + get_symbol(numParam) + ", " + defaultNum + ")");
		}

		Preconditions.checkState(defaultNum > 1);
		if (nonNullParam(rangeParam)) {
			Range range = validParam(rangeParam);
			boolean same = range.getLowerBound() == range.getUpperBound();
			if (same && ((!definedParam(numParam)) || validParam(numParam) > 1)) {
				updateParam(numParam, 1);
			}
			else if (!same && ((!definedParam(numParam)) || validParam(numParam) == 1)) {
				updateParam(numParam, defaultNum);
			}
		}
		return;
	}
	



	// Check thet the grid range number is consistent with the range limits,
	// throw an exception if not.
	// validateRange must run on worker threads, and so must not set any parameters or write to the screen.

	private void validateRange(Range range, int num, String name) {
		Preconditions.checkState(range != null, "Must supply "+name+" range");
		boolean same = range.getLowerBound() == range.getUpperBound();
		if (same)
			Preconditions.checkState(num == 1, "Num must equal 1 for fixed "+name);
		else
			Preconditions.checkState(num > 1, "Num must be >1 for variable "+name);
		return;
	}
	



	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
