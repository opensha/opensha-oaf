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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
import org.opensha.commons.data.xyz.ArbDiscrXYZ_DataSet;
import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
//import org.opensha.commons.geo.Region;
//import org.opensha.commons.gui.ConsoleWindow;
import org.opensha.commons.gui.plot.GraphPanel;
import org.opensha.commons.gui.plot.GraphWidget;
import org.opensha.commons.gui.plot.PlotPreferences;
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
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.ForecastResults;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opensha.oaf.util.LineConsumerFile;
import org.opensha.oaf.util.LineSupplierFile;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEMarginalDistSet;
import org.opensha.oaf.oetas.util.OEMarginalDistSetBuilder;

import org.opensha.oaf.oetas.env.OEtasIntegratedIntensityFile;


// Reasenberg & Jones GUI - View implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the view.
// It holds the displays that appear on the right side of the main window.


public class OEGUIView extends OEGUIComponent {


	//----- Tabs -----


	// Index numbers for tabs. [Deprecated]

	//private static final int console_tab_index = 0;
	//private static final int epicenter_tab_index = 1;
	//private static final int mag_num_tab_index = 2;
	//private static final int mag_time_tab_index = 3;
	//private static final int cml_num_tab_index = 4;
	//private static final int catalog_tab_index = 5;
	//private static final int pdf_tab_index = 6;
	//private static final int aftershock_expected_index = 7;
	//private static final int forecast_table_tab_index = 8;


	// Components for tabs.

	private JScrollPane consoleScroll;				// Console window [MODSTATE_INITIAL]

	private JTextArea infoText;						// System information window [MODSTATE_INITIAL]
	private JScrollPane infoTextPane;				// This is the component added to the tabbed pane.

	private GraphWidget epicenterGraph;				// Map of aftershock epicenters [MODSTATE_CATALOG]

	private GraphWidget magNumGraph;				// Plot of number vs magnitude, re-plottable [MODSTATE_CATALOG]

	private GraphWidget magTimeGraph;				// Plot of magnitude vs time [MODSTATE_CATALOG]

	private GraphWidget cmlNumGraph;				// Plot of cumulative number vs time, re-plottable [MODSTATE_CATALOG]

	private JTextArea catalogText;					// List of aftershock time, locations, magnitudes [MODSTATE_CATALOG]
	private JScrollPane catalogTextPane;			// This is the component added to the tabbed pane.

	private JTabbedPane pdfGraphsPane;				// Tabbed plot of probability distribution function [MODSTATE_PARAMETERS]

	private JTabbedPane etasNumGraphsPane;			// Tabbed plot of ETAS number graphs [MODSTATE_PARAMETERS]

	//private GraphWidget aftershockExpectedGraph;	// Plot of forecast number vs magnitude [MODSTATE_FORECAST]

	private JTabbedPane forecastTablePane;			// Tabbed display of forecast tables and controls [MODSTATE_FORECAST]


	// Tabbed view container.

	private JTabbedPane tabbedPane;

	public final JTabbedPane get_tabbedPane () {
		return tabbedPane;
	}

	private JTabbedPane init_tabbedPane () throws GUIEDTException {

		// Null out all tabs

		consoleScroll = null;

		infoText = null;
		infoTextPane = null;

		epicenterGraph = null;

		magNumGraph = null;

		magTimeGraph = null;

		cmlNumGraph = null;

		catalogText = null;
		catalogTextPane = null;

		pdfGraphsPane = null;

		etasNumGraphsPane = null;

		//aftershockExpectedGraph = null;

		forecastTablePane = null;

		// Make the tabbed container
		
		tabbedPane = new JTabbedPane();

		// Make the console window

		GUIConsoleWindow console = new GUIConsoleWindow(true);
		consoleScroll = console.getScrollPane();
		consoleScroll.setSize(gui_top.get_consoleWidth(), gui_top.get_consoleHeight());
		JTextArea text = console.getTextArea();
		text.setFont(gui_top.get_consoleFont());
		text.setCaretPosition(0);
		text.setCaretPosition(text.getText().length());

		tabbedPane.addTab("Console", null, consoleScroll, "View Console");

		// Plot the system information

		plotInfoText();

		// Make sure the console window is displayed

		tabbedPane.setSelectedComponent(consoleScroll);

		return tabbedPane;
	}





	//----- Plotting definitions and subroutines -----


	// Colors for plotting the different models.

	private static final Color generic_color = Color.GREEN.darker();
	private static final Color bayesian_color = Color.RED;
	private static final Color sequence_specific_color = Color.BLUE.darker();
	private static final Color active_color = Color.ORANGE;


	// Get the time since the mainshock, in days.

	private double getTimeSinceMainshock(ObsEqkRupture rup) {
		long ms = gui_model.get_cur_mainshock().getOriginTime();
		long as = rup.getOriginTime();
		long delta = as - ms;
		return ((double)delta)/(ComcatOAFAccessor.day_millis);
	}


	// Font sizes for graphs.

	private static final int tickLabelFontSize = 22;
	private static final int axisLabelFontSize = 24;
	private static final int plotLabelFontSize = 24;


	// Setup graph font sizes and background color.

	private static void setupGP(GraphWidget widget) {
		widget.setPlotLabelFontSize(plotLabelFontSize);
		widget.setAxisLabelFontSize(axisLabelFontSize);
		widget.setTickLabelFontSize(tickLabelFontSize);
		widget.setBackgroundColor(Color.WHITE);
		return;
	}




	// Discretized function, mapping magnitude into symbol size.
	// Symbols are binned into 16 sizes.
	
	private EvenlyDiscretizedFunc saved_magSizeFunc = null;
	
	private EvenlyDiscretizedFunc getMagSizeFunc() {
		if (saved_magSizeFunc != null)
			return saved_magSizeFunc;
		
		// size function
		double minMag = 1.25;
		double magDelta = 0.5;
		int numMag = 2*8;
		saved_magSizeFunc = new EvenlyDiscretizedFunc(minMag, numMag, magDelta);
//		double maxMag = saved_magSizeFunc.getMaxX();
//		double minSize = 1d;
//		double maxSize = 20d;
//		double sizeMult = 1.4;
//		double size = minSize;
		
		double dS = 3;
		for (int i=0; i<saved_magSizeFunc.size(); i++) {
			double mag = saved_magSizeFunc.getX(i);
//			double fract = (mag - minMag)/(maxMag - minMag);
//			double size = minSize + fract*(maxSize - minSize);
			
//			saved_magSizeFunc.set(i, size);
//			double radius = Math.pow((7d/16d)*Math.pow(10, 1.5*mag + 9)/(dS*1e6), 1d/3d) / 1000 / 111.111;
			// scale with stress drop, from Nicholas via e-mail 10/26/2015
			double radius = Math.pow((7d/16d)*Math.pow(10, 1.5*mag + 9)/(dS*1e6), 1d/3d) / 300d;
			saved_magSizeFunc.set(i, radius);
//			System.out.println("Mag="+mag+", radius="+radius);
//			size *= sizeMult;
		}
		
		return saved_magSizeFunc;
	}



	
	// A function that maps magnitudes to colors.
	// It covers the same range of magnitudes as saved_magSizeFunc.

	private CPT saved_magCPT = null;
	
	private CPT getMagCPT() {
		if (saved_magCPT != null)
			return saved_magCPT;
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();
		try {
			saved_magCPT = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale(
					my_magSizeFunc.getMinX(), my_magSizeFunc.getMaxX());
		} catch (IOException e) {
			throw ExceptionUtils.asRuntimeException(e);
		}
		return saved_magCPT;
	}




	// Discretized function, with "well-placed" bins, with distance as the X-value.
	// Distances are binned into 20 values from 0 to 200 km.
	// Only the X-values of this function are used.
	
	private EvenlyDiscretizedFunc saved_distFunc = null;
	
	private EvenlyDiscretizedFunc getDistFunc() {
		if (saved_distFunc == null)
			saved_distFunc = HistogramFunction.getEncompassingHistogram(0d, 199d, 20d);
		
		return saved_distFunc; 
	}




	// Color mapping, converting distance-from-mainshock to color.
	// It covers the same range of distances as saved_distFunc.
	
	private CPT saved_distCPT = null;
	
	private CPT getDistCPT() {

		// Lazy allocation

		if (saved_distCPT != null) {
			return saved_distCPT;
		}

		EvenlyDiscretizedFunc my_distFunc = getDistFunc();
		double halfDelta = 0.5*my_distFunc.getDelta();
		try {
			saved_distCPT = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale(
					my_distFunc.getMinX()-halfDelta, my_distFunc.getMaxX()+halfDelta);
		} catch (IOException e) {
			throw ExceptionUtils.asRuntimeException(e);
		}
		return saved_distCPT;
	}
	



	private void buildFuncsCharsForBinned(XY_DataSet[] binnedFuncs,
			List<PlotElement> funcs, List<PlotCurveCharacterstics> chars, PlotSymbol sym) {
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();
		double magDelta = my_magSizeFunc.getDelta();
		CPT magColorCPT = getMagCPT();
		for (int i=0; i<binnedFuncs.length; i++) {
			double mag = my_magSizeFunc.getX(i);
			XY_DataSet xy = binnedFuncs[i];
			if (xy.size() == 0)
				continue;
			xy.setName((float)(mag-0.5*magDelta)+" < M < "+(float)(mag+0.5*magDelta)
					+": "+xy.size()+" EQ");
			float size = (float)my_magSizeFunc.getY(i);
			Color c = magColorCPT.getColor((float)my_magSizeFunc.getX(i));
			funcs.add(xy);
			chars.add(new PlotCurveCharacterstics(sym, size, c));
		}
	}
	



	/**
	 * Uses scalars from func2 to color funcs
	 * 
	 * @param binnedFuncs
	 * @param funcs
	 * @param chars
	 * @param cpt
	 * @param name2
	 * @param func2
	 */
	private void buildFuncsCharsForBinned2D(XY_DataSet[][] binnedFuncs, List<PlotElement> funcs,
			List<PlotCurveCharacterstics> chars, CPT cpt, String name2, EvenlyDiscretizedFunc func2, PlotSymbol sym) {
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();
		double magDelta = my_magSizeFunc.getDelta();
		double func2Delta = func2.getDelta();
		for (int i=0; i<binnedFuncs.length; i++) {
			double mag = my_magSizeFunc.getX(i);
			for (int j=0; j<binnedFuncs[i].length; j++) {
				XY_DataSet xy = binnedFuncs[i][j];
				if (xy.size() == 0)
					continue;
				double scalar2 = func2.getX(j);
				String name = (float)(mag-0.5*magDelta)+" < M < "+(float)(mag+0.5*magDelta);
				name += ", "+(float)(scalar2-0.5*func2Delta)+" < "+name2+" < "+(float)(scalar2+0.5*func2Delta);
				name += ": "+xy.size()+" EQ";
				xy.setName(name);
				float size = (float)my_magSizeFunc.getY(i);
				Color c = cpt.getColor((float)func2.getX(j));
				funcs.add(xy);
				chars.add(new PlotCurveCharacterstics(sym, size, c));
			}
		}
	}
	



	private void buildFuncsCharsForBinnedFS(XY_DataSet[] binnedFuncs,
			List<PlotElement> funcs, List<PlotCurveCharacterstics> chars, PlotSymbol sym, Color c) {
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();
		double magDelta = my_magSizeFunc.getDelta();
		for (int i=0; i<binnedFuncs.length; i++) {
			double mag = my_magSizeFunc.getX(i);
			XY_DataSet xy = binnedFuncs[i];
			if (xy.size() == 0)
				continue;
			xy.setName((float)(mag-0.5*magDelta)+" < M < "+(float)(mag+0.5*magDelta)
					+": "+xy.size()+" FS");
			float size = (float)my_magSizeFunc.getY(i);
			funcs.add(xy);
			chars.add(new PlotCurveCharacterstics(sym, size, c));
		}
	}




	//----- System information text tab -----




	// Plot the system information.
	// This routine can re-plot an existing tab.
	// Can be called in any model state.
	
	private void plotInfoText() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotInfoText, tab count = " + tabbedPane.getTabCount());
		}

		// Create or modify the text pane

		if (infoText == null) {
			String info_window_text = gui_model.get_info_window_text (true);
			infoText = new JTextArea(info_window_text);
			infoText.setEditable(false);
			infoText.setFont(gui_top.get_consoleFont());
			infoTextPane = new JScrollPane(infoText);
			infoText.setCaretPosition(0);		// scroll to top of window
			tabbedPane.addTab("Information", null, infoTextPane, "System Information");
		} else {
			String info_window_text = gui_model.get_info_window_text (false);
			if (info_window_text != null) {
				infoText.setText(info_window_text);
				infoText.setCaretPosition(0);		// scroll to top of window
			}
		}
		return;
	}




	// Remove the system information text.
	// Performs no operation if the system information text is not currently in the tabbed pane.

	private void removeInfoText () throws GUIEDTException {
		tabbedPane.remove(infoTextPane);
		infoText = null;
		infoTextPane = null;
		return;
	}




	// Get the system information text from the tab.
	// Must be called after the system information text has been displayed.

	public String get_info_text () throws GUIEDTException {
		return infoText.getText();
	}




	//----- Epicenter plot tab -----




	// Plot the epicenters.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.
	
	private void plotAftershockHypocs() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotAftershockHypocs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIView.plotAftershockHypocs - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// The outer region, or null if none, to plot the regional aftershocks

		SphRegion outer_region = gui_model.get_outer_region();

		// The inner region, or null if none, to plot the aftershock region

		SphRegion inner_region = gui_model.get_cur_region();

		// Flag, true if we need to plot in the 0 to +360 domain, false for the -180 to +180 domain

		boolean f_wrap_lon = false;

		if (outer_region != null) {
			f_wrap_lon = outer_region.getPlotWrap();
		} else if (inner_region != null) {
			f_wrap_lon = inner_region.getPlotWrap();
		} else {

			// TODO: At this point we should scan the aftershock longitudes and determine which
			// domain is a better fit, but for now we just check if the mainshock is closer to The
			// date line than to the prime meridian

			double main_lon = gui_model.get_cur_mainshock().getHypocenterLocation().getLongitude();
			if (main_lon < -90.0 || main_lon > 90.0) {
				f_wrap_lon = true;
			}
		}

		// Functions and characteristics

		List<PlotElement> funcs = Lists.newArrayList();
		List<PlotCurveCharacterstics> chars = Lists.newArrayList();

		// Function mapping magnitude to symbol size
		
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();

		// Function mapping time to color
		
		boolean colorByTime = true;
		CPT timeCPT = null;
		
		if (colorByTime) {
			try {
				timeCPT = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale(0d, gui_model.get_cat_dataEndTimeParam());
				//timeCPT.setBelowMinColor (Color.BLACK);		// color for foreshocks
			} catch (IOException e) {
				throw ExceptionUtils.asRuntimeException(e);
			}
		}

		PaintScaleLegend subtitle = null;

		// If we have a rupture surface for the mainshock, plot the fault trace
		
		RuptureSurface mainSurf = gui_model.get_cur_mainshock().getRuptureSurface();
		if (mainSurf != null && !mainSurf.isPointSurface()) {
			FaultTrace trace = gui_model.get_cur_mainshock().getRuptureSurface().getEvenlyDiscritizedUpperEdge();
			DefaultXY_DataSet traceFunc = new DefaultXY_DataSet();
			traceFunc.setName("Main Shock Trace");
			for(Location loc:trace) {
				traceFunc.set(SphLatLon.get_lon (loc, f_wrap_lon), loc.getLatitude());
			}
			funcs.add(traceFunc);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));

		// Otherwise, draw a symbol at the mainshock Location

		} else {
			Location hypo = gui_model.get_cur_mainshock().getHypocenterLocation();
			DefaultXY_DataSet xy = new DefaultXY_DataSet(new double[] {SphLatLon.get_lon (hypo, f_wrap_lon)},
					new double[] {hypo.getLatitude()});
			xy.setName("Main Shock Location");
			funcs.add(xy);
			float size = (float)my_magSizeFunc.getY(my_magSizeFunc.getClosestXIndex(gui_model.get_cur_mainshock().getMag()));
			Color c;
			if (colorByTime) {
				c = timeCPT.getMinColor();		//TODO: Use color for mainshock time, not necessarily min color
			} else {
				c = Color.BLACK;
			}
			//chars.add(new PlotCurveCharacterstics(PlotSymbol.FILLED_CIRCLE, size, c));
			chars.add(new PlotCurveCharacterstics(PlotSymbol.CIRCLE, size, c));
		}
		
		// Create lists of aftershock epicenters, magnitudes, and times after mainshock in days
		// Make separate lists for aftershocks and foreshocks

		List<Point2D> points = Lists.newArrayList();
		List<Double> mags = Lists.newArrayList();
		List<Double> timeDeltas = Lists.newArrayList();

		List<Point2D> points_fs = Lists.newArrayList();
		List<Double> mags_fs = Lists.newArrayList();
		List<Double> timeDeltas_fs = Lists.newArrayList();

		for (ObsEqkRupture rup : gui_model.get_plot_aftershocks()) {
			Location loc = rup.getHypocenterLocation();
			double tdelta = getTimeSinceMainshock(rup);
			if (tdelta < 0.0) {
				points_fs.add(new Point2D.Double(SphLatLon.get_lon (loc, f_wrap_lon), loc.getLatitude()));
				mags_fs.add(rup.getMag());
				timeDeltas_fs.add(tdelta);
			} else {
				points.add(new Point2D.Double(SphLatLon.get_lon (loc, f_wrap_lon), loc.getLatitude()));
				mags.add(rup.getMag());
				timeDeltas.add(tdelta);
			}
		}

		// Plot foreshocks

		boolean plot_foreshocks = true;

		if (plot_foreshocks) {

			// Bin foreshocks by magnitude

			XY_DataSet[] foreshockDatasets = XY_DatasetBinner.bin(points_fs, mags_fs, my_magSizeFunc);
			
			buildFuncsCharsForBinnedFS(foreshockDatasets, funcs, chars, PlotSymbol.CIRCLE, Color.DARK_GRAY);
		}

		// If coloring aftershock by time ...
		
		if (colorByTime) {

			// Bin aftershocks by magnitude and time

			EvenlyDiscretizedFunc timeFunc = HistogramFunction.getEncompassingHistogram(0d, timeCPT.getMaxValue()*0.99, 1d);	//TODO: Allow for time to extend before mainshock
			XY_DataSet[][] aftershockDatasets = XY_DatasetBinner.bin2D(points, mags, timeDeltas, my_magSizeFunc, timeFunc);

			// Bin aftershocks by magnitude and time.
			
			//buildFuncsCharsForBinned2D(aftershockDatasets, funcs, chars, timeCPT, "time", timeFunc, PlotSymbol.FILLED_CIRCLE);
			buildFuncsCharsForBinned2D(aftershockDatasets, funcs, chars, timeCPT, "time", timeFunc, PlotSymbol.CIRCLE);
			
			// The graph legend

			double cptInc = 0d;
			if ((timeCPT.getMaxValue() - timeCPT.getMinValue()) < 10) {
				cptInc = 1d;
			}
			subtitle = GraphPanel.getLegendForCPT(timeCPT, "Time (days)", axisLabelFontSize, tickLabelFontSize,
					cptInc, RectangleEdge.RIGHT);

		// Otherwise, color by magnitude ...

		} else {
			// Bin aftershocks by magnitude

			XY_DataSet[] aftershockDatasets = XY_DatasetBinner.bin(points, mags, my_magSizeFunc);
			
			buildFuncsCharsForBinned(aftershockDatasets, funcs, chars, PlotSymbol.CIRCLE);
		}
		
		// Now add outline of region, if we have one

		if (inner_region != null) {
			DefaultXY_DataSet outline = new DefaultXY_DataSet();
			for (Location loc : inner_region.getBorder()) {
				outline.set(SphLatLon.get_lon (loc, f_wrap_lon), loc.getLatitude());
			}
			outline.set(outline.get(0));
			outline.setName("Region Outline");
			
			funcs.add(outline);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.GRAY));
		}

		if (outer_region != null && outer_region != inner_region) {
			DefaultXY_DataSet outline = new DefaultXY_DataSet();
			for (Location loc : outer_region.getBorder()) {
				outline.set(SphLatLon.get_lon (loc, f_wrap_lon), loc.getLatitude());
			}
			outline.set(outline.get(0));
			outline.setName("Area Outline");
			
			funcs.add(outline);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.GRAY));
		}

		// Reverse ordering
		// (Causes early aftershocks to overwrite later ones -- is this desired?)
		// (Tests with filled circles show that reversal does cause early aftershocks
		// to overwrite later ones, although this is not 100% consistent -- either
		// choice gives some cases where early overwrites late, and some cases where
		// late overwrites early.  Not reversing seems to give a higher proportion
		// of late overwriting early.  It is not clear why this is inconsistent.)
		
		//Collections.reverse(funcs);
		//Collections.reverse(chars);

		// Create a plot with the given functions and characteristics
		
		PlotSpec spec = new PlotSpec(funcs, chars, "Aftershock Epicenters", "Longitude", "Latitude");

		boolean new_tab = false;

		if (epicenterGraph == null) {
			epicenterGraph = new GraphWidget(spec);
			new_tab = true;
		} else {
			epicenterGraph.setPlotSpec(spec);
		}

		// If we have a region, adjust axis ranges to hold it
		
		double regBuff = 0.05;

		if (outer_region != null) {
			epicenterGraph.setAxisRange(outer_region.getMinLon()-regBuff, outer_region.getMaxLon()+regBuff,
					outer_region.getMinLat()-regBuff, outer_region.getMaxLat()+regBuff);

		} else if (inner_region != null) {
			epicenterGraph.setAxisRange(inner_region.getMinLon()-regBuff, inner_region.getMaxLon()+regBuff,
					inner_region.getMinLat()-regBuff, inner_region.getMaxLat()+regBuff);

		// Otherwise, adjust axis ranges to hold all the aftershocks

		} else {
			 MinMaxAveTracker latTrack = new MinMaxAveTracker();
			 MinMaxAveTracker lonTrack = new MinMaxAveTracker();
			 latTrack.addValue(gui_model.get_cur_mainshock().getHypocenterLocation().getLatitude());
			 lonTrack.addValue(SphLatLon.get_lon (gui_model.get_cur_mainshock().getHypocenterLocation(), f_wrap_lon));
			 for (ObsEqkRupture rup : gui_model.get_plot_aftershocks()) {
				 Location loc = rup.getHypocenterLocation();
				 latTrack.addValue(loc.getLatitude());
				 lonTrack.addValue(SphLatLon.get_lon (loc, f_wrap_lon));
			 }
			 epicenterGraph.setAxisRange(lonTrack.getMin()-regBuff, lonTrack.getMax()+regBuff,
						latTrack.getMin()-regBuff, latTrack.getMax()+regBuff);
		}

		// Set font sizes
		
		setupGP(epicenterGraph);
		
		if (subtitle != null) {
			epicenterGraph.getGraphPanel().addSubtitle(subtitle);
		}

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Epicenters", null, epicenterGraph, "Epicenter Map");
		}

		return;
	}




	// Remove the epicenter plot.
	// Performs no operation if the epicenter plot is not currently in the tabbed pane.

	private void removeAftershockHypocs () throws GUIEDTException {
		tabbedPane.remove(epicenterGraph);
		epicenterGraph = null;
		return;
	}




	//----- Magnitude number (magnitude frequency distribution) tab -----




	// Plot the magnitude number graph.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	// Note: This plot depends on the Mc and b-value parameters,
	// and so should be re-plotted when they change.

	private void plotMFDs () throws GUIEDTException {
		plotMFDs (gui_model.get_aftershockMND(), gui_model.get_mnd_mmaxc());
		return;
	}


	private void plotMFDs (IncrementalMagFreqDist mfd, double mmaxc) throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotMFDs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIView.plotMFDs - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Functions and characteristics

		List<XY_DataSet> funcs = Lists.newArrayList();
		List<PlotCurveCharacterstics> chars = Lists.newArrayList();

		// Incremental plot is blue circles, for the given MFD
		
		mfd.setName("Incremental");
		funcs.add(mfd);
//		chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, Color.BLUE));
		chars.add(new PlotCurveCharacterstics(PlotSymbol.CIRCLE, 6f, Color.BLUE));

		// Get the magnitude range, for the X-axis
		
		double plotMinMag = Double.POSITIVE_INFINITY;
		double plotMaxMag = gui_model.get_cur_mainshock().getMag();
		for (int i=0; i<mfd.size(); i++) {
			if (mfd.getY(i) > 0) {
				double mag = mfd.getX(i);
				plotMinMag = Math.min(plotMinMag, mag);
				plotMaxMag = Math.max(plotMaxMag, mag);
			}
		}
		if (Double.isInfinite(plotMinMag))
			plotMinMag = 0d;
		plotMinMag = Math.floor(plotMinMag);
		plotMaxMag = Math.ceil(plotMaxMag);

		// Cumulative plot is black cirles
		
		EvenlyDiscretizedFunc cmlMFD =  mfd.getCumRateDistWithOffset();
		cmlMFD.setName("Cumulative");
		funcs.add(cmlMFD);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));

		// Add black curve following the cumulative plot
		
		//boolean addCmlPoints = true;
		boolean addCmlPoints = (cmlMFD.size() > 0);
		
		if (addCmlPoints) {
			ArbitrarilyDiscretizedFunc cmlPoints = new ArbitrarilyDiscretizedFunc();
			cmlPoints.setName(null); // don't show legend
			
			double prevVal = cmlMFD.getY(0);
			
			for (int i=1; i<cmlMFD.size(); i++) {
				double val = cmlMFD.getY(i);
				if (val != prevVal) {
					cmlPoints.set(cmlMFD.getX(i-1), prevVal);
					
					prevVal = val;
				}
			}
			
			funcs.add(cmlPoints);
			chars.add(new PlotCurveCharacterstics(PlotSymbol.CIRCLE, 6f, Color.BLACK));
		}

		// Get Y-axis range
		
		double plotMinY = 0.9d;
		double plotMaxY = plotMinY+2d;
		if (cmlMFD.size() > 0) {
			plotMaxY = Math.max(plotMaxY, cmlMFD.getMaxY()+2d);
		}

		// Y-values used to draw vertical lines (why do we need more than two?)
		
		List<Double> yValsForVerticalLines = Lists.newArrayList(0d, 1e-16, plotMinY, 1d, plotMaxY, 1e3, 2e3, 3e3 ,4e3, 5e3);
		
		// Add vertical line at mainshock mag

		DefaultXY_DataSet xy = new DefaultXY_DataSet();
		for (double y : yValsForVerticalLines)
			xy.set(gui_model.get_cur_mainshock().getMag(), y);
		xy.setName("Mainshock Mag ("+(float)gui_model.get_cur_mainshock().getMag()+")");
		funcs.add(xy);
		chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.RED));
		
		// Add vertical line at Mmaxc mag (magnitude with max # of aftershocks)

		xy = new DefaultXY_DataSet();
		for (double y : yValsForVerticalLines)
			xy.set(mmaxc, y);
		xy.setName("Mmaxc ("+(float)mmaxc+")");
		funcs.add(xy);
		chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.GREEN));

		// If we have a b-value ...

		boolean has_b_value = true;
		
		if (has_b_value) {
			System.out.println("********************Calculating MFD with b: " + gui_model.get_cat_bParam());

			// Add vertical line for Mc used for b-value calculation

			double mc = gui_model.get_cat_mcParam();
			xy = new DefaultXY_DataSet();
			for (double y : yValsForVerticalLines)
				xy.set(mc, y);
			xy.setName("Mc ("+(float)mc+")");
			funcs.add(xy);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.CYAN));
			
			// Add best fitting G-R sloped line

			double b = gui_model.get_cat_bParam();
			GutenbergRichterMagFreqDist gr = new GutenbergRichterMagFreqDist(b, 1d, mfd.getMinX(), mfd.getMaxX(), mfd.size());

			// Scale (adjust vertical position of line) to match the aftershock rate at Mc

			int index = mfd.getClosestXIndex(mc);
			gr.scaleToCumRate(index, cmlMFD.getY(index));
//			gr.scaleToIncrRate(index, cmlMFD.getY(index));

			// Add sloped line to graph
			
			gr.setName("G-R b="+(float)b);
//			funcs.add(gr);
			EvenlyDiscretizedFunc cmlGR = gr.getCumRateDistWithOffset();
			funcs.add(cmlGR);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.ORANGE));

			// Adjust Y-range to show highest point on the sloped line
			
			plotMaxY = Math.max(plotMaxY, cmlGR.getY(cmlGR.getClosestXIndex(plotMinMag)));

		// Otherwise, no b-value

		} else {
			System.out.println("********************Calculating MFD with b unspecified");
		}

		// Remove any unused functions
		
		removeEmptyFuncs(funcs, chars);

		// Create a plot with the given functions and characteristics
		
		PlotSpec spec = new PlotSpec(funcs, chars, "Aftershock Mag Num Dist", "Magnitude", "Count");
		spec.setLegendVisible(true);

		boolean new_tab = false;
		
		if (magNumGraph == null) {
			magNumGraph = new GraphWidget(spec);
			new_tab = true;
		} else {
			magNumGraph.setPlotSpec(spec);
		}

		// Set axis type and range, and font sizes

		magNumGraph.setY_Log(true);
		
		magNumGraph.setY_AxisRange(plotMinY, plotMaxY);
		magNumGraph.setX_AxisRange(plotMinMag, plotMaxMag);

		setupGP(magNumGraph);

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Mag/Num Dist", null, magNumGraph,
					"Aftershock Magnitude vs Number Distribution");
		}

		return;
	}




	// Remove any function entries that are empty, and remove the corresponding characteristics.
	
	private static void removeEmptyFuncs(List<? extends XY_DataSet> funcs, List<PlotCurveCharacterstics> chars) {
		for (int i=funcs.size(); --i>=0;) {
			if (funcs.get(i).size() == 0) {
				funcs.remove(i);
				chars.remove(i);
			}
		}
	}




	// Remove the magnitude number plot.
	// Performs no operation if the magnitude number plot is not currently in the tabbed pane.

	private void removeMFDs () throws GUIEDTException {
		tabbedPane.remove(magNumGraph);
		magNumGraph = null;
		return;
	}




	//----- Magnitude vs time plot tab -----




	// Plot the magnitude vs time graph.
	// Must be called with model state >= MODSTATE_CATALOG.
	
	private void plotMagVsTime() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotMagVsTime, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIView.plotMagVsTime - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// List of points and their corresponding magnitude, each point is (time, mag).

		List<Point2D> points = Lists.newArrayList();
		List<Double> mags = Lists.newArrayList();
		
		points.add(new Point2D.Double(getTimeSinceMainshock(gui_model.get_cur_mainshock()), gui_model.get_cur_mainshock().getMag()));
		mags.add(gui_model.get_cur_mainshock().getMag());
		
		MinMaxAveTracker magTrack = new MinMaxAveTracker();
		magTrack.addValue(gui_model.get_cur_mainshock().getMag());
		for (int i=0; i<gui_model.get_cur_aftershocks().size(); i++) {
			ObsEqkRupture aftershock = gui_model.get_cur_aftershocks().get(i);
			points.add(new Point2D.Double(getTimeSinceMainshock(aftershock), aftershock.getMag()));
			mags.add(aftershock.getMag());
			magTrack.addValue(aftershock.getMag());
		}
		
		boolean colorByDist = true;

		// List of functions and characteristics
		
		List<PlotElement> funcs = Lists.newArrayList();
		List<PlotCurveCharacterstics> chars = Lists.newArrayList();

		// Function mapping magnitude to symbol size
		
		EvenlyDiscretizedFunc my_magSizeFunc = getMagSizeFunc();
		PaintScaleLegend subtitle = null;

		// If coloring symbols by distance from mainshock ...
		
		if (colorByDist) {

			// Make a list containing each earthquake's distance to the hypocnter

			List<Double> dists = Lists.newArrayList();
			// TODO horizontal, correct?
			dists.add(0d); // mainshock distance from itself, thus zero
			
			for (int i=0; i<gui_model.get_cur_aftershocks().size(); i++) {
				ObsEqkRupture aftershock = gui_model.get_cur_aftershocks().get(i);
				dists.add(SphLatLon.horzDistanceFast(gui_model.get_cur_mainshock().getHypocenterLocation(), aftershock.getHypocenterLocation()));
			}

			// Histogram of distances
			
			EvenlyDiscretizedFunc my_distFunc = getDistFunc();
			
			XY_DataSet[][] binnedFuncs = XY_DatasetBinner.bin2D(points, mags, dists, my_magSizeFunc, my_distFunc);
				// binnedFuncs[i][j] is a list of points, where i identifies a magnitude bin, and
				// j identifies a distance bin.  The point points[k] is placed into the bin (i,j)
				// so that: i is the index into my_magSizeFunc whose X-value is closest to mags[k], and
				// j is the index into my_distFunc whose X-value is closest to dists[k].

			// Function mapping distance to colors, same number of X-values as my_distFunc
			
			CPT my_distCPT = getDistCPT();
			
			buildFuncsCharsForBinned2D(binnedFuncs, funcs, chars, my_distCPT, "dist", my_distFunc, PlotSymbol.FILLED_CIRCLE);
			
			subtitle = GraphPanel.getLegendForCPT(my_distCPT, "Distance (km)", axisLabelFontSize, tickLabelFontSize,
					0d, RectangleEdge.RIGHT);

		// Otherwise, color by magnitude ...

		} else {
			XY_DataSet[] magBinnedFuncs = XY_DatasetBinner.bin(points, mags, my_magSizeFunc);
				// magBinnedFuncs[i] is a list of points, where i identifies a magnitude bin.
				// The point points[k] is placed into the bin i so that: i is the index into
				// my_magSizeFunc whose X-value is closest to mags[k].
			
			buildFuncsCharsForBinned(magBinnedFuncs, funcs, chars, PlotSymbol.CIRCLE);
		}

		// Create a plot with the given functions and characteristics
		
		PlotSpec spec = new PlotSpec(funcs, chars, "Magnitude Vs Time", "Days Since Mainshock", "Magnitude");

		boolean new_tab = false;
		
		if (magTimeGraph == null) {
			magTimeGraph = new GraphWidget(spec);
			new_tab = true;
		} else {
			magTimeGraph.setPlotSpec(spec);
		}

		// Set axis ranges

		magTimeGraph.setX_AxisRange(-0.75, gui_model.get_cat_dataEndTimeParam() + 0.75);
		magTimeGraph.setY_AxisRange(Math.max(0, magTrack.getMin()-1d), magTrack.getMax()+1d);

		// Set font sizes
		
		setupGP(magTimeGraph);

		if (subtitle != null)
			magTimeGraph.getGraphPanel().addSubtitle(subtitle);

		// Add to the view
		
		if (new_tab) {
			tabbedPane.addTab("Mag/Time Plot", null, magTimeGraph,
					"Aftershock Magnitude vs Time Plot");
		}

		return;
	}




	// Remove the magnitude vs time plot.
	// Performs no operation if the magnitude vs time plot is not currently in the tabbed pane.

	private void removeMagVsTime () throws GUIEDTException {
		tabbedPane.remove(magTimeGraph);
		magTimeGraph = null;
		return;
	}




	//----- Cumulative number plot tab -----
	



	// Plot the cumulative number graph.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	// Note: This plot include all of the models (generic, seq-spec, bayesian)
	// that exist.  Because the available models change with state, this should
	// be re-plotted on transition between MODSTATE_CATALOG and MODSTATE_PARAMETERS.
	// Note: This plot depends on the forecast time range, and so should be
	// re-plotted when the range changes.
	// Note: This plot depends on the Mc parameter in state MODSTATE_CATALOG,
	// and so should be replotted when Mc changes.

	private void plotCumulativeNum () throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotCumulativeNum, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIView.plotCumulativeNum - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Determine whether to use a constant or time-varying magnitude of completeness,
		// and if the latter then get the magnitude-of-completeness function

		double magMin;
		MagCompFn magCompFn;
		MagCompFn magCompFnPlot;
		double magMain = gui_model.get_cur_mainshock().getMag();
		String magMinCaption;

		// If we have a seq-spec model, use its magnitude-of-completeness function
		// TODO: Take magnitude-of-completeness function from forecast parameters?
		// TODO: For ETAS, the m-of-c function is more complicated, maybe a separate plot for ETAS?
		
		if (gui_model.get_cur_model() != null) {
			magMin = gui_model.get_cur_model().get_magCat();
			magCompFn = gui_model.get_cur_model().get_magCompFn();
			if (magCompFn.is_constant()) {
				magCompFnPlot = null;
				magMinCaption = String.format ("%.3f", magMin);
			} else {
				magCompFnPlot = magCompFn;
				magMinCaption = "Mc(t)";
			}

		// Otherwise, use constant taken from controller parameters

		} else {
			magMin = gui_model.get_cat_mcParam();
			magCompFn = MagCompFn.makeConstant();
			magCompFnPlot = null;
			magMinCaption = String.format ("%.3f", magMin);
		}

		// Function representing cumulative count of aftershocks
		
		ArbitrarilyDiscretizedFunc countFunc = new ArbitrarilyDiscretizedFunc();
		double count = 0;

		// Sort aftershock list by time
		//TODO: Do sort when aftershock list is read, or work on a temp copy of the list
		
		//gui_model.get_cur_aftershocks().sortByOriginTime();		// sort is now done in the model

		// Scan the list, and for each aftershock above the magnitude of completeness,
		// add an point to the Function

		for (int i=0; i<gui_model.get_cur_aftershocks().size(); i++) {
			ObsEqkRupture aftershock = gui_model.get_cur_aftershocks().get(i);
			double time = getTimeSinceMainshock(aftershock);	// time in days
			if (time < 0.0) {
				continue;		// foreshock fix
			}
			if (aftershock.getMag() < magCompFn.getMagCompleteness (magMain, magMin, time)) {
				continue;
			}
			count++;
			countFunc.set(time, count);
		}
		countFunc.set(gui_model.get_cat_dataEndTimeParam(), count);
		countFunc.setName("Data: "+(int)countFunc.getMaxY());

		// Save the final count
		
		double maxY = Math.max (3.0, count);	// Max prevents creation of a plot with an empty y-range equal to (0,0)

		// Create lists of functions and characteristics, and add the count function to the lists
		
		List<PlotElement> funcs = Lists.newArrayList();
		List<PlotCurveCharacterstics> chars = Lists.newArrayList();
		
		funcs.add(countFunc);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, Color.BLACK));

		// If we have a seq-spec model, graph it
		
		if (gui_model.get_cur_model() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_cur_model(), magMin, magCompFnPlot);
			
			maxY = Math.max(maxY, expected.getMaxY());
			
			funcs.add(expected);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, sequence_specific_color));
			
			expected.setName("Seq Specific: "+new DecimalFormat("0.#").format(expected.getMaxY()));
		}

		// If we have a generic model, graph it
		
		if (gui_model.get_genericModel() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_genericModel(), magMin, magCompFnPlot);
			
			maxY = Math.max(maxY, expected.getMaxY());
			
			funcs.add(expected);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 3f, generic_color));
			
			expected.setName("Generic: "+new DecimalFormat("0.#").format(expected.getMaxY()));
		}

		// If we have a bayesian model, graph it
			
		if (gui_model.get_bayesianModel() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_bayesianModel(), magMin, magCompFnPlot);
				
			maxY = Math.max(maxY, expected.getMaxY());
				
			funcs.add(expected);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 3f, bayesian_color));
				
			expected.setName("Bayesian: "+new DecimalFormat("0.#").format(expected.getMaxY()));
		}

		// Create a plot with the given functions and characteristics
		
		PlotSpec spec = new PlotSpec(funcs, chars, "Cumulative M\u2265"+magMinCaption, "Days Since Mainshock",
				"Cumulative Number of Aftershocks");
		spec.setLegendVisible(true);

		boolean new_tab = false;
		
		if (cmlNumGraph == null) {
			cmlNumGraph = new GraphWidget(spec);
			new_tab = true;
		} else {
			cmlNumGraph.setPlotSpec(spec);
		}

		// Set font sizes and axis ranges
		
		setupGP(cmlNumGraph);
//		cmlNumGraph.setX_AxisRange(-0.75, gui_model.get_cat_dataEndTimeParam() + 0.75);
//		magTimeGraph.setY_AxisRange(Math.max(0, magTrack.getMin()-1d), magTrack.getMax()+1d);
		cmlNumGraph.setY_AxisRange(0d, maxY*1.1);

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Cumulative Num Plot", null, cmlNumGraph,
					"Cumulative Number Of Aftershocks Plot");
		}

		return;
	}




	// Create a function which gives the expected cumulative number of aftershocks
	// as a function of time.  The maximum-likelihood parameter values are used
	// in the R&J formula.  The count includes aftershocks with magnitude >= the
	// given magnitude-of-completeness function;  or >= magMin if null magnitude-
	// of-completeness function is supplied.  The returned function has 1000 points.
	
	private EvenlyDiscretizedFunc getModelCumNumWithTimePlot(RJ_AftershockModel the_model, double magMin, MagCompFn magCompFnPlot) {
		double tMin = Math.max (0.0, gui_model.get_cat_dataStartTimeParam());	// foreshock fix
		double tMax = gui_model.get_cat_dataEndTimeParam();
		if (gui_model.modstate_has_forecast()) {
			//tMax = Math.max(tMax, gui_model.get_cat_forecastEndTimeParam());	// if we have a forecast, extend at least to end of forecast
			//TODO: For now, instead of a user parameter, use the data end time plus advisory duration
			double end_days = gui_model.get_cat_dataEndTimeParam();
			long end_millis = SimpleUtils.days_to_millis (end_days);
			long adv_millis = ForecastResults.forecast_lag_to_advisory_lag (end_millis, new ActionConfig());
			tMax = Math.max (tMax, end_days + SimpleUtils.millis_to_days (adv_millis));
		}
		Preconditions.checkState(tMax > tMin);
		double tDelta = (tMax - tMin)/1000d;
		if (magCompFnPlot == null) {
			return the_model.getModalCumNumEventsWithTime(magMin, tMin, tMax, tDelta);
		}
		return the_model.getPageModalCumNumEventsWithTime(magMin, magCompFnPlot, tMin, tMax, tDelta);
	}




	// Remove the cumulative number plot.
	// Performs no operation if the cumulative number plot is not currently in the tabbed pane.

	private void removeCumulativeNum () throws GUIEDTException {
		tabbedPane.remove(cmlNumGraph);
		cmlNumGraph = null;
		return;
	}




	//----- Catalog text tab -----




	//  // Plot the current catalog.
	//  // This routine can re-plot an existing tab.
	//  // Must be called with model state >= MODSTATE_CATALOG.
	//  
	//  private void plotCatalogText() throws GUIEDTException {
	//  
	//  	if (gui_top.get_trace_events()) {
	//  		System.out.println ("@@@@@ Entry: OEGUIView.plotCatalogText, tab count = " + tabbedPane.getTabCount());
	//  	}
	//  
	//  	if (!( gui_model.modstate_has_catalog() )) {
	//  		throw new IllegalStateException ("OEGUIView.plotCatalogText - Invalid model state: " + gui_model.cur_modstate_string());
	//  	}
	//  
	//  	StringBuilder sb = new StringBuilder();
	//  
	//  	// Header line
	//  
	//  	sb.append("# Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude\n");
	//  	
	//  	// Mainshock
	//  	
	//  	sb.append("# Main Shock:\n");
	//  	sb.append("# ").append(GUIExternalCatalog.getCatalogLine(gui_model.get_cur_mainshock())).append("\n");
	//  	
	//  	// Aftershocks
	//  	
	//  	for (ObsEqkRupture rup : gui_model.get_cur_aftershocks()) {
	//  		sb.append(GUIExternalCatalog.getCatalogLine(rup)).append("\n");
	//  	}
	//  
	//  	// Create or modify the text pane
	//  
	//  	if (catalogText == null) {
	//  		catalogText = new JTextArea(sb.toString());
	//  		catalogText.setEditable(false);
	//  		catalogTextPane = new JScrollPane(catalogText);
	//  		tabbedPane.addTab("Catalog", null, catalogTextPane, "Aftershock Catalog");
	//  	} else {
	//  		catalogText.setText(sb.toString());
	//  	}
	//  	return;
	//  }




	// Plot the current catalog.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	
	private void plotCatalogText() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotCatalogText, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("OEGUIView.plotCatalogText - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Write the file to a string

		StringWriter sw = new StringWriter();
		try (
			LineConsumerFile lcf = new LineConsumerFile (sw);
		){
			try {
				gui_model.saveCatalog (lcf);
			} catch (Exception e) {
				throw new RuntimeException ("Error writing catalog file: " + lcf.error_locus(), e);
			}
		}

		// Create or modify the text pane

		if (catalogText == null) {
			catalogText = new JTextArea(sw.toString());
			catalogText.setEditable(false);
			catalogText.setFont(gui_top.get_consoleFont());
			catalogTextPane = new JScrollPane(catalogText);
			tabbedPane.addTab("Catalog", null, catalogTextPane, "Aftershock Catalog");
		} else {
			catalogText.setText(sw.toString());
		}
		return;
	}




	// Remove the catalog text.
	// Performs no operation if the catalog text is not currently in the tabbed pane.

	private void removeCatalogText () throws GUIEDTException {
		tabbedPane.remove(catalogTextPane);
		catalogText = null;
		catalogTextPane = null;
		return;
	}




	// Get the catalog text from the tab.
	// Must be called with model state >= MODSTATE_CATALOG,
	// and after the catalog text has been displayed.

	public String get_catalog_text () throws GUIEDTException {
		return catalogText.getText();
	}




	//----- Probability density function tab -----




	// Plot the probability density functions.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_PARAMETERS.
	
	private void plotPDFs() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotPDFs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_aftershock_params() )) {
			throw new IllegalStateException ("OEGUIView.plotPDFs - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Allocate a new component, or remove all the tabs from an existing component

		boolean new_tab = false;

		if (pdfGraphsPane == null) {
			pdfGraphsPane = new JTabbedPane();
			new_tab = true;
		} else {
			//  while (pdfGraphsPane.getTabCount() > 0) {
			//  	pdfGraphsPane.removeTabAt(0);
			//  }
			pdfGraphsPane.removeAll();
		}

		// Construct histograms for the generic and bayesian a-values, if they exist
		
		HistogramFunction[] aValExtras = null;
		if (gui_model.get_genericModel() != null) {
			HistogramFunction genericA = gui_model.get_genericModel().getPDF_a();
			genericA.setName("Generic");
			HistogramFunction bayesianA;
			if (gui_model.get_bayesianModel() != null) {
				bayesianA = gui_model.get_bayesianModel().getPDF_a();
				bayesianA.setName("Bayesian");
				aValExtras = new HistogramFunction[] { genericA, bayesianA };
			} else {
				aValExtras = new HistogramFunction[] { genericA };
			}
		}

		// Add tabs for 1D PDFs

		add1D_PDF(gui_model.get_cur_model().getPDF_a(), "a-value", aValExtras);
		add1D_PDF(gui_model.get_cur_model().getPDF_p(), "p-value");
		add1D_PDF(gui_model.get_cur_model().getPDF_c(), "c-value");

		// Add tabs for 2D PDFs

		add2D_PDF(gui_model.get_cur_model().get2D_PDF_for_a_and_c(), "a-value", "c-value");
		add2D_PDF(gui_model.get_cur_model().get2D_PDF_for_a_and_p(), "a-value", "p-value");
		add2D_PDF(gui_model.get_cur_model().get2D_PDF_for_c_and_p(), "c-value", "p-value");

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Model PDFs", null, pdfGraphsPane,
					"Aftershock Model Prob Dist Funcs");
		}

		return;
	}




	// Add a 1D probability density function to the view.
	
	private static Color[] extra_colors = {Color.GRAY, Color.BLUE, Color.ORANGE, Color.GREEN};
	
	private void add1D_PDF(HistogramFunction pdf, String name, HistogramFunction... extras) throws GUIEDTException {

		// If we did not receive a PDF from the model, do nothing

		if (pdf == null)
			return;
		
		Preconditions.checkState(Doubles.isFinite(pdf.getMaxY()), "NaN found in "+pdf.getName());
		
		// Add PDF to list of functions, as histogram bars

		List<DiscretizedFunc> funcs = Lists.newArrayList();
		funcs.add(pdf);
		List<PlotCurveCharacterstics> chars = Lists.newArrayList(
				new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, sequence_specific_color));
		
		// If additional histogram functions are provided, add them as solid curves,
		// using specified colors for generic and bayesian modesl

		if (extras != null && extras.length > 0) {
			for (int i=0; i<extras.length; i++) {
				funcs.add(extras[i]);
				Color c;
				String extraName = extras[i].getName().toLowerCase();
				if (extraName.contains("generic"))
					c = generic_color;
				else if (extraName.contains("bayesian"))
					c = bayesian_color;
				else
					c = extra_colors[i % extra_colors.length];
				chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, c));
			}
		}

		// Create a plot with the given functions and characteristics, and font sizes
		
		PlotSpec spec = new PlotSpec(funcs, chars, pdf.getName(), name, "Density");
		spec.setLegendVisible(funcs.size() > 1);
		
		GraphWidget widget = new GraphWidget(spec);
		setupGP(widget);

		// Add to the view

		pdfGraphsPane.addTab(name, null, widget);
		return;
	}




	// Add a 2D probability density function to the view.
	
	private void add2D_PDF(EvenlyDiscrXYZ_DataSet pdf, String name1, String name2) throws GUIEDTException {

		// If we did not receive a PDF from the model, do nothing

		if (pdf == null)
			return;
		
		String title = "PDF for "+name1+" vs "+name2;
		
		Preconditions.checkState(Doubles.isFinite(pdf.getMaxZ()), "NaN found in "+title);
		
		// Get a color scale, for the Z-values in the pdf

		CPT cpt;
		try {
			cpt = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale(pdf.getMinZ(), pdf.getMaxZ());
		} catch (IOException e) {
			throw ExceptionUtils.asRuntimeException(e);
		}

		// Create a plot and add it to the view
		
		XYZPlotSpec spec = new XYZPlotSpec(pdf, cpt, title, name1, name2, "Density");
		
		//XYZGraphPanel xyzGP = new XYZGraphPanel();
		//pdfGraphsPane.addTab(name1+" vs "+name2, null, xyzGP);
		GraphWidget widget = new GraphWidget(spec);
		setupGP(widget);
		pdfGraphsPane.addTab(name1+" vs "+name2, null, widget);

		// Draw the PDF

		double xDelta = pdf.getGridSpacingX();
		double yDelta = pdf.getGridSpacingY();
		widget.setAxisRange(
				new Range(pdf.getMinX()-0.5*xDelta, pdf.getMaxX()+0.5*xDelta),
				new Range(pdf.getMinY()-0.5*yDelta, pdf.getMaxY()+0.5*yDelta));

		return;
	}




	// Remove the probability density function plot.
	// Performs no operation if the probability density function plot is not currently in the tabbed pane.

	private void removePDFs () throws GUIEDTException {
		tabbedPane.remove(pdfGraphsPane);
		pdfGraphsPane = null;
		return;
	}




	// Make a function from arrays of arguments and values, and a name.
	// Parameters:
	//  args = Function arguments.
	//  vals = Function values.
	//  name = Name to assign, can be null for none.

	private ArbitrarilyDiscretizedFunc func_from_args_vals (double[] args, double[] vals, String name) {
		if (!( args.length == vals.length )) {
			throw new IllegalArgumentException ("OEGUIView.func_from_args_vals: Array length mismatch: args.length = " + args.length + ", vals.length = " + vals.length);
		}
		ArbitrarilyDiscretizedFunc func = new ArbitrarilyDiscretizedFunc();

		// Add all the points

		for (int j = 0; j < args.length; ++j) {
			func.set (args[j], vals[j]);
		}

		// If a name is supplied, set it

		if (name != null) {
			func.setName (name);
		}

		return func;
	}




	// Make a 1D PDF function.
	// Parameters:
	//  dist_set = Marginal distribution set. Can be null.
	//  var_name = Variable name.
	//  data_name = Data name.
	//  func_name = Function name, can be null for none.
	//  rscale = Used to return the natural scale (OEDiscreteRange.RSCALE_XXXX).
	// Returns the function, or null if it cannot be generated.
	// The caller should initialize rscale[0] to RSCALE_NULL before the first call to
	// this function.  On a non-null return, rscale[0] is updated to the natural scale
	// if its current value is RSCALE_NULL.  If rscale[0] is not RSCALE_NULL, then the
	// current scale is checked for compatibility (log vs linear) and null is returned
	// if they are not compatible (which should not happen).

	private ArbitrarilyDiscretizedFunc make_1d_pdf_func (
		OEMarginalDistSet dist_set, String var_name, String data_name, String func_name, int[] rscale) {

		// Must have a distribution set with all grid info

		if (dist_set == null) {
			return null;
		}
		if (!( dist_set.has_grid_def_info() )) {
			return null;
		}

		// Find the variable index

		int var_index = dist_set.find_var_index (var_name);
		if (var_index < 0) {
			return null;
		}

		// Find the data index

		int data_index = dist_set.find_data_index (data_name);
		if (data_index < 0) {
			return null;
		}

		// Make the univariate density array

		double mass = 1.0;
		double[] density_array = dist_set.make_univar_density (var_index, data_index, mass);
		if (density_array == null) {
			return null;
		}

		// Get the variable value array

		double[] value_array = dist_set.get_value_array (var_index);
		if (value_array.length <= 1) {
			return null;		// should not happen
		}
		if (value_array.length != density_array.length) {
			throw new InvariantViolationException ("OEGUIView.make_1d_pdf_func: Array length mismatch: density_array.length = " + density_array.length + ", value_array.length = " + value_array.length);
		}

		// Get the discrete range

		OEDiscreteRange range = dist_set.get_discrete_range(var_index);
		if (range.get_range_size() != value_array.length) {
			throw new InvariantViolationException ("OEGUIView.make_1d_pdf_func: Range length mismatch: range.get_range_size() = " + range.get_range_size() + ", value_array.length = " + value_array.length);
		}

		// This is used to test log-range plotting when working with RJ, deactivate when not testing

		if (gui_top.get_force_c_log() && var_name.equals("c")) {
			range = OEDiscreteRange.makeLog (range.get_range_size(), range.get_range_min(), range.get_range_max());
			value_array = range.get_range_array();
		}

		// Update the scale, check for log

		int my_rscale = range.get_natural_scale();

		if (rscale[0] == OEDiscreteRange.RSCALE_NULL) {
			rscale[0] = my_rscale;
		} else {
			if (!( OEDiscreteRange.is_rscale_log (rscale[0]) == OEDiscreteRange.is_rscale_log (my_rscale) )) {
				return null;	// should not happen
			}
			if (rscale[0] == OEDiscreteRange.RSCALE_UNKNOWN) {
				rscale[0] = my_rscale;
			}
		}

		// Here we could take the log of each element of value_array, if the range
		// is a log scale and we wanted to make plots in linear scale

		//  if (OEDiscreteRange.is_rscale_log (my_rscale)) {
		//  	for (int j = 0; j < value_array.length; ++j) {
		//  		value_array[j] = Math.log10 (value_array[j]);
		//  	}
		//  }

		// Make the function

		ArbitrarilyDiscretizedFunc func = func_from_args_vals (value_array, density_array, func_name);

		return func;
	}




	// Make a 1D PDF given marginals.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  var_name = Variable name.
	//  gen_dist_set = Marginal distribution set, for generic. Can be null.
	//  gen_data_name = Data name, for generic.
	//  seq_dist_set = Marginal distribution set, for sequence specific. Can be null.
	//  seq_data_name = Data name, for sequence specific.
	//  bay_dist_set = Marginal distribution set, for bayesian. Can be null.
	//  bay_data_name = Data name, for bayesian.
	//  act_dist_set = Marginal distribution set, for active. Can be null.
	//  act_data_name = Data name, for bayesian.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.

	private void make_1d_pdf (
		String title, String tab_text, String var_name,
		OEMarginalDistSet gen_dist_set, String gen_data_name,
		OEMarginalDistSet seq_dist_set, String seq_data_name,
		OEMarginalDistSet bay_dist_set, String bay_data_name,
		OEMarginalDistSet act_dist_set, String act_data_name) throws GUIEDTException
	{
		// Natural scale

		int[] rscale = new int[1];
		rscale[0] = OEDiscreteRange.RSCALE_NULL;

		// List of functions and characteristics
		
		//List<PlotElement> funcs = new ArrayList<PlotElement>();
		List<ArbitrarilyDiscretizedFunc> funcs = new ArrayList<ArbitrarilyDiscretizedFunc>();
		List<PlotCurveCharacterstics> chars = new ArrayList<PlotCurveCharacterstics>();

		// Sequence-specific PDF

		ArbitrarilyDiscretizedFunc seq_func = make_1d_pdf_func (
			seq_dist_set, var_name, seq_data_name, "SeqSpec", rscale);

		if (seq_func != null) {
			funcs.add (seq_func);
			chars.add (new PlotCurveCharacterstics (PlotLineType.HISTOGRAM, 1f, sequence_specific_color));
		}

		// Generic PDF

		ArbitrarilyDiscretizedFunc gen_func = make_1d_pdf_func (
			gen_dist_set, var_name, gen_data_name, "Generic", rscale);

		if (gen_func != null) {
			funcs.add (gen_func);
			chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 2f, generic_color));
		}

		// Bayesian PDF

		ArbitrarilyDiscretizedFunc bay_func = make_1d_pdf_func (
			bay_dist_set, var_name, bay_data_name, "Bayesian", rscale);

		if (bay_func != null) {
			funcs.add (bay_func);
			chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 2f, bayesian_color));
		}

		// Active PDF

		ArbitrarilyDiscretizedFunc act_func = make_1d_pdf_func (
			act_dist_set, var_name, act_data_name, "Custom", rscale);

		if (act_func != null) {
			funcs.add (act_func);
			chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 2f, active_color));
		}

		// If we got at least one function ...

		if (funcs.size() > 0) {

			// Get the x and y ranges (min y is zero)

			double min_x = Double.MAX_VALUE;
			double max_x = -Double.MAX_VALUE;
			double max_y = -Double.MAX_VALUE;
			double min_y = 0.0;

			for (ArbitrarilyDiscretizedFunc func : funcs) {
				min_x = Math.min (min_x, func.getMinX());
				max_x = Math.max (max_x, func.getMaxX());
				max_y = Math.max (max_y, func.getMaxY());
			}

			// Create a plot with the given functions and characteristics, and font sizes
		
			PlotSpec spec = new PlotSpec(funcs, chars, title, var_name, "Density");
			//spec.setLegendVisible(funcs.size() > 1);
			spec.setLegendVisible(true);	// always show legend even if just one curve

			GraphWidget widget;

			// If log scale, set log on horizontal axis, and specify ranges so
			// auto-ranging won't make min x below zero

			if (OEDiscreteRange.is_rscale_log (rscale[0])) {

				PlotPreferences plotPrefs = PlotPreferences.getDefault();
				boolean xLog = true;
				boolean yLog = false;
				Range xRange = new Range (min_x * Math.pow(max_x / min_x, -0.06), max_x * Math.pow(max_x / min_x, 0.06));
				Range yRange = new Range (0.0, max_y * 1.06);

				widget = new GraphWidget(spec, plotPrefs, xLog, yLog, xRange, yRange);
			}

			// If linear scale, auto-ranging is OK
		
			else {
				widget = new GraphWidget(spec);
			}

			// Adjust font sizes

			setupGP(widget);

			// Set log scale on horizontal axis if needed

			if (rscale[0] == OEDiscreteRange.RSCALE_LOG) {
				widget.setX_Log (true);
			}

			// Add to the view

			pdfGraphsPane.addTab (tab_text, null, widget);
		}

		return;
	}




	// Make a 1D PDF given marginals.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  var_name = Variable name.
	//  gen_dist_set = Marginal distribution set, for generic. Can be null.
	//  gen_data_name = Data name, for generic.
	//  seq_dist_set = Marginal distribution set, for sequence specific. Can be null.
	//  seq_data_name = Data name, for sequence specific.
	//  bay_dist_set = Marginal distribution set, for bayesian. Can be null.
	//  bay_data_name = Data name, for bayesian.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.

	private void make_1d_pdf (
		String title, String tab_text, String var_name,
		OEMarginalDistSet gen_dist_set, String gen_data_name,
		OEMarginalDistSet seq_dist_set, String seq_data_name,
		OEMarginalDistSet bay_dist_set, String bay_data_name) throws GUIEDTException
	{
		OEMarginalDistSet act_dist_set = null;
		String act_data_name = "";

		make_1d_pdf (
			title, tab_text, var_name,
			gen_dist_set, gen_data_name,
			seq_dist_set, seq_data_name,
			bay_dist_set, bay_data_name,
			act_dist_set, act_data_name
		);

		return;
	}




	// Rasterize a discrete range.
	// Parameters:
	//  range = Discrete range to rasterize
	//  identity_length = Max length of identity mapping, or -1 for no limit.
	//  raster_length = Length of the returned array, if it is not an identity mapping.
	// Returns an integer array that maps from uniformly-spaced integer indexes
	// to indexes within the discrete range.  Uniform spacing is in a linear or log
	// scale depending on whether the supplied range is linear or log.
	// If the given range is uniform, and its length does not exceed identity_length
	// (or identity_length is -1), then an identity mapping is returned.
	// Setting identity_length to 0 prevents any identity mapping from being returned.
	// This function can be used to plot non-uniform data ranges onto evenly spaced plots,
	// and also to limit the number of points in the plot.

	private int[] rasterize_range (OEDiscreteRange range, int identity_length, int raster_length) {

		// Length of the array we will return

		int len = raster_length;
		if (range.is_natural_uniform()) {
			if (identity_length < 0 || range.get_range_size() <= identity_length) {
				len = range.get_range_size();
			}
		}

		// Make a uniform range of the desired length, with the same limits

		OEDiscreteRange uniform;
		if (range.is_natural_log()) {
			uniform = OEDiscreteRange.makeLog (len, range.get_range_min(), range.get_range_max());
		} else {
			uniform = OEDiscreteRange.makeLinear (len, range.get_range_min(), range.get_range_max());
		}

		// Use the bin finder for the given range to find the index for each element of the uniform range

		return range.make_bin_finder(false, false).find_bins (uniform.get_range_array());
	}




	// Make a 2D PDF function.
	// Parameters:
	//  dist_set = Marginal distribution set. Can be null.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  data_name = Data name.
	//  rscale = 2-element array used to return the natural scales (OEDiscreteRange.RSCALE_XXXX).
	// Returns the function, or null if it cannot be generated.
	// The return is an evenly-discretized function of two variables.
	// In case of a log range, the x and/or y values are the log10 of the variables,
	// so the function may be plotted on a linear scale.
	// On a non-null return, rscale[0] and rscale[1] are set to the RSCALE_XXXX value
	// for the x and y ranges respectively.

	private EvenlyDiscrXYZ_DataSet make_2d_pdf_func (
		OEMarginalDistSet dist_set, String var_name1, String var_name2, String data_name, int[] rscale) {

		// Must have a distribution set with all grid info

		if (dist_set == null) {
			return null;
		}
		if (!( dist_set.has_grid_def_info() )) {
			return null;
		}

		// Find the variable indexes

		int var_index1 = dist_set.find_var_index (var_name1);
		if (var_index1 < 0) {
			return null;
		}

		int var_index2 = dist_set.find_var_index (var_name2);
		if (var_index2 < 0) {
			return null;
		}

		// Find the data index

		int data_index = dist_set.find_data_index (data_name);
		if (data_index < 0) {
			return null;
		}

		// Make the biivariate density matrix

		double mass = 1.0;
		double[][] density_matrix = dist_set.make_bivar_density (var_index1, var_index2, data_index, mass);
		if (density_matrix == null) {
			return null;
		}

		// Get the variable value arrays

		double[] value_array1 = dist_set.get_value_array (var_index1);
		if (value_array1.length <= 1) {
			return null;		// should not happen
		}
		if (value_array1.length != density_matrix.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Array length mismatch: density_matrix.length = " + density_matrix.length + ", value_array1.length = " + value_array1.length);
		}

		double[] value_array2 = dist_set.get_value_array (var_index2);
		if (value_array2.length <= 1) {
			return null;		// should not happen
		}
		if (value_array2.length != density_matrix[0].length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Array length mismatch: density_matrix[0].length = " + density_matrix[0].length + ", value_array2.length = " + value_array2.length);
		}

		// Get the discrete ranges

		OEDiscreteRange range1 = dist_set.get_discrete_range(var_index1);
		if (range1.get_range_size() != value_array1.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Range length mismatch: range1.get_range_size() = " + range1.get_range_size() + ", value_array1.length = " + value_array1.length);
		}

		OEDiscreteRange range2 = dist_set.get_discrete_range(var_index2);
		if (range2.get_range_size() != value_array2.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Range length mismatch: range2.get_range_size() = " + range2.get_range_size() + ", value_array2.length = " + value_array2.length);
		}

		// This is used to test log-range plotting when working with RJ, deactivate when not testing

		if (gui_top.get_force_c_log() && var_name1.equals("c")) {
			range1 = OEDiscreteRange.makeLog (range1.get_range_size(), range1.get_range_min(), range1.get_range_max());
			value_array1 = range1.get_range_array();
		}

		if (gui_top.get_force_c_log() && var_name2.equals("c")) {
			range2 = OEDiscreteRange.makeLog (range2.get_range_size(), range2.get_range_min(), range2.get_range_max());
			value_array2 = range2.get_range_array();
		}

		// Get the scales

		int my_rscale1 = range1.get_natural_scale();
		int my_rscale2 = range2.get_natural_scale();

		rscale[0] = my_rscale1;
		rscale[1] = my_rscale2;

		// Take the log of each element in the value array, if the range is log scale

		if (OEDiscreteRange.is_rscale_log (my_rscale1)) {
			for (int j = 0; j < value_array1.length; ++j) {
				value_array1[j] = Math.log10 (value_array1[j]);
			}
		}

		if (OEDiscreteRange.is_rscale_log (my_rscale2)) {
			for (int j = 0; j < value_array2.length; ++j) {
				value_array2[j] = Math.log10 (value_array2[j]);
			}
		}

		// Rasterize the two ranges

		int identity_length = 350;
		int raster_length = 320;

		int[] raster1 = rasterize_range (range1, identity_length, raster_length);
		int[] raster2 = rasterize_range (range2, identity_length, raster_length);

		// Make the function

		int num_1 = raster1.length;
		int num_2 = raster2.length;

		double min_1 = value_array1[raster1[0]];
		double min_2 = value_array2[raster2[0]];

		double max_1 = value_array1[raster1[num_1 - 1]];
		double max_2 = value_array2[raster2[num_2 - 1]];

		double delta_1 = (max_1 - min_1) / ((double)(num_1 - 1));
		double delta_2 = (max_2 - min_2) / ((double)(num_2 - 1));

		EvenlyDiscrXYZ_DataSet func = new EvenlyDiscrXYZ_DataSet (num_1, num_2, min_1, min_2, delta_1, delta_2);

		for (int j1 = 0; j1 < num_1; ++j1) {
			for (int j2 = 0; j2 < num_2; ++j2) {
				func.set (j1, j2, density_matrix[raster1[j1]][raster2[j2]]);
			}
		}

		return func;
	}




	// Make a 2D PDF given a marginal.
	// Parameters:
	//  title = Plot title.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  dist_set = Marginal distribution set.
	//  data_name = Data name.
	// Returns the graph widget, or null if unable to create.

	private GraphWidget make_2d_pdf (
		String title, String var_name1, String var_name2,
		OEMarginalDistSet dist_set, String data_name)
	{
		// Natural scale

		int[] rscale = new int[2];
		rscale[0] = OEDiscreteRange.RSCALE_NULL;
		rscale[1] = OEDiscreteRange.RSCALE_NULL;

		// Get the Function

		EvenlyDiscrXYZ_DataSet func = make_2d_pdf_func (
			dist_set, var_name1, var_name2, data_name, rscale);

		if (func == null) {
			return null;
		}
		
		// Get a color scale, for the Z-values in the pdf

		CPT cpt;
		try {
			//cpt = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale (func.getMinZ(), func.getMaxZ());
			cpt = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale (0.0, func.getMaxZ() * 1.001);
		} catch (IOException e) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf: Failed to make color scale", e);
		}

		// Create a plot and add it to the view

		String name1 = var_name1;
		if (OEDiscreteRange.is_rscale_log (rscale[0])) {
			name1 = "log10 " + name1;
		}

		String name2 = var_name2;
		if (OEDiscreteRange.is_rscale_log (rscale[1])) {
			name2 = "log10 " + name2;
		}
		
		XYZPlotSpec spec = new XYZPlotSpec (func, cpt, title, name1, name2, "Density");
		
		GraphWidget widget = new GraphWidget(spec);
		setupGP(widget);

		// Draw the PDF

		double xDelta = func.getGridSpacingX();
		double yDelta = func.getGridSpacingY();
		widget.setAxisRange(
			new Range(func.getMinX()-0.5*xDelta, func.getMaxX()+0.5*xDelta),
			new Range(func.getMinY()-0.5*yDelta, func.getMaxY()+0.5*yDelta));

		return widget;
	}




	// Make a 2D PDF given a marginal.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  dist_set = Marginal distribution set.
	//  data_name = Data name.

	private void make_2d_pdf (
		String title, String tab_text, String var_name1, String var_name2,
		OEMarginalDistSet dist_set, String data_name)
	{
		// Make the widget for the 2D PDF

		GraphWidget widget = make_2d_pdf (
			title, var_name1, var_name2,
			dist_set, data_name
		);

		// If we got one, add to the view

		if (widget != null) {
			pdfGraphsPane.addTab (tab_text, null, widget);
		}

		return;
	}




	// True to produce multiple 2D PDFs for ETAS.

	private boolean f_multi_pdf_etas = true;




	// Make a 2D PDF given a marginal.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  dist_set = Marginal distribution set.
	//  data_name = Data name.
	//  f_etas_active = True to display active mocel (as "custom").
	// Note: If f_multi_pdf_etas, make a tabbed plot for multiple models,
	// and ignore title and data_name.  Otherwise, make a single plot.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.

	private void make_2d_pdf_etas (
		String title, String tab_text, String var_name1, String var_name2,
		OEMarginalDistSet dist_set, String data_name, boolean f_etas_active) throws GUIEDTException
	{
		// Handle case of single plot

		if (!( f_multi_pdf_etas )) {
			make_2d_pdf (
				title, tab_text, var_name1, var_name2,
				dist_set, data_name
			);
			return;
		}

		// Tabbed pane and selected widget

		JTabbedPane tab_pane = null;
		GraphWidget selected_widget = null;

		// Sequence specific

		GraphWidget seq_widget = make_2d_pdf (
			"ETAS (SeqSpec) PDF for " + var_name1 + " vs " + var_name2, var_name1, var_name2,
			dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC
		);

		if (seq_widget != null) {
			if (tab_pane == null) {
				tab_pane = new JTabbedPane();
				selected_widget = seq_widget;
			}
			tab_pane.addTab ("SeqSpec", null, seq_widget);
		}

		// Bayesian

		GraphWidget bay_widget = make_2d_pdf (
			"ETAS (Bayesian) PDF for " + var_name1 + " vs " + var_name2, var_name1, var_name2,
			dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN
		);

		if (bay_widget != null) {
			if (tab_pane == null) {
				tab_pane = new JTabbedPane();
				selected_widget = bay_widget;
			}
			tab_pane.addTab ("Bayesian", null, bay_widget);
		}

		// Generic

		GraphWidget gen_widget = make_2d_pdf (
			"ETAS (Generic) PDF for " + var_name1 + " vs " + var_name2, var_name1, var_name2,
			dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC
		);

		if (gen_widget != null) {
			if (tab_pane == null) {
				tab_pane = new JTabbedPane();
				selected_widget = gen_widget;
			}
			tab_pane.addTab ("Generic", null, gen_widget);
		}

		// Active

		if (f_etas_active) {
			GraphWidget act_widget = make_2d_pdf (
				"ETAS (Custom) PDF for " + var_name1 + " vs " + var_name2, var_name1, var_name2,
				dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
			);

			if (act_widget != null) {
				if (tab_pane == null) {
					tab_pane = new JTabbedPane();
					selected_widget = act_widget;
				}
				tab_pane.addTab ("Custom", null, act_widget);
			}
		}

		// If we made a graph, add to the view

		if (tab_pane != null) {
			tab_pane.setSelectedComponent (selected_widget);
			pdfGraphsPane.addTab (tab_text, null, tab_pane);
		}

		return;
	}




	// Uniformize a discrete range.
	// Parameters:
	//  range = Discrete range to uniformize.
	//  uniform_length = Desired length of uniform range.
	//  min_match_length = Minimum range size for matching lemgth.
	//  max_match_length = Maximum range size for matching length, or -1 for no limit.
	//  f_expand = True to expand the range slightly if the number of points is increasing.
	// Returns a discrete range that is uniform (linear or log) and spans (approximately)
	// the same range of values.
	// If the supplied range is uniform and its size is between min_match_length and
	// max_match_length (inclusive), then the returned range has the same size as the
	// supplied range.  Otherwise, the size of the returned range is uniform_length.
	// If the size of the returned range is larger than the size of the supplied range, and
	// f_expand is true, then the range is slightly enlarged to avoid undersampling the endpoints.

	private OEDiscreteRange uniformize_range (OEDiscreteRange range, int uniform_length, int min_match_length, int max_match_length, boolean f_expand) {

		// Length of the range we will return

		int rsize = range.get_range_size();

		int len = uniform_length;
		if (range.is_natural_uniform()) {
			if (rsize >= min_match_length && (max_match_length < 0 || rsize <= max_match_length)) {
				len = rsize;
			}
		}

		// Minimum and maximum values of the uniform range

		double umin = range.get_range_min();
		double umax = range.get_range_max();

		// If the number of points is increasing, adjust endpoints

		if (f_expand && len > rsize) {
			double[] rbins = range.get_bin_array();

			double ratio = ((double)len) / ((double)rsize);
			double r = (ratio - 1.0) / ratio;

			if (range.is_natural_log()) {
				double umin_diff = umin / rbins[0];
				double umax_diff = rbins[rsize] / umax;

				if (umin_diff > 1.0) {
					umin = umin / Math.pow (umin_diff, r);
				}
				if (umax_diff > 1.0) {
					umax = umax * Math.pow (umax_diff, r);
				}
			} else {
				double umin_diff = umin - rbins[0];
				double umax_diff = rbins[rsize] - umax;

				if (umin_diff > 0.0) {
					umin = umin - (umin_diff * r);
				}
				if (umax_diff > 0.0) {
					umax = umax + (umax_diff * r);
				}
			}
		}

		// Make a uniform range of the desired length

		OEDiscreteRange uniform;
		if (range.is_natural_log()) {
			uniform = OEDiscreteRange.makeLog (len, umin, umax);
		} else {
			uniform = OEDiscreteRange.makeLinear (len, umin, umax);
		}

		return uniform;
	}




	// Make a 2D PDF function.
	// Parameters:
	//  dist_set = Marginal distribution set. Can be null.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  data_name = Data name.
	//  rscale = 2-element array used to return the natural scales (OEDiscreteRange.RSCALE_XXXX).
	//  x_bounds = 2-element array used to return the min and max values of x.
	//  y_bounds = 2-element array used to return the min and max values of y.
	//  z_bounds = 2-element array used to return the min and max values of z.
	// Returns the function, or null if it cannot be generated.
	// The return is an arbitrarily-deiscretized function of two variables.
	// It has a product structure, with each axis being uniformly discretized
	// in either linear or log space.
	// On a non-null return, rscale[0] and rscale[1] are set to the RSCALE_XXXX value
	// for the x and y ranges respectively, and the bounds are filled in.

	private ArbDiscrXYZ_DataSet make_2d_pdf_func_v2 (
		OEMarginalDistSet dist_set, String var_name1, String var_name2, String data_name, int[] rscale,
		double[] x_bounds, double[] y_bounds, double[] z_bounds) {

		// Must have a distribution set with all grid info

		if (dist_set == null) {
			return null;
		}
		if (!( dist_set.has_grid_def_info() )) {
			return null;
		}

		// Find the variable indexes

		int var_index1 = dist_set.find_var_index (var_name1);
		if (var_index1 < 0) {
			return null;
		}

		int var_index2 = dist_set.find_var_index (var_name2);
		if (var_index2 < 0) {
			return null;
		}

		// Find the data index

		int data_index = dist_set.find_data_index (data_name);
		if (data_index < 0) {
			return null;
		}

		// Make the biivariate density matrix

		double mass = 1.0;
		double[][] density_matrix = dist_set.make_bivar_density (var_index1, var_index2, data_index, mass);
		if (density_matrix == null) {
			return null;
		}

		// Get the variable value arrays

		double[] value_array1 = dist_set.get_value_array (var_index1);
		if (value_array1.length <= 1) {
			return null;		// should not happen
		}
		if (value_array1.length != density_matrix.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Array length mismatch: density_matrix.length = " + density_matrix.length + ", value_array1.length = " + value_array1.length);
		}

		double[] value_array2 = dist_set.get_value_array (var_index2);
		if (value_array2.length <= 1) {
			return null;		// should not happen
		}
		if (value_array2.length != density_matrix[0].length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Array length mismatch: density_matrix[0].length = " + density_matrix[0].length + ", value_array2.length = " + value_array2.length);
		}

		// Get the discrete ranges

		OEDiscreteRange range1 = dist_set.get_discrete_range(var_index1);
		if (range1.get_range_size() != value_array1.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Range length mismatch: range1.get_range_size() = " + range1.get_range_size() + ", value_array1.length = " + value_array1.length);
		}

		OEDiscreteRange range2 = dist_set.get_discrete_range(var_index2);
		if (range2.get_range_size() != value_array2.length) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf_func: Range length mismatch: range2.get_range_size() = " + range2.get_range_size() + ", value_array2.length = " + value_array2.length);
		}

		// This is used to test log-range plotting when working with RJ, deactivate when not testing

		if (gui_top.get_force_c_log() && var_name1.equals("c")) {
			range1 = OEDiscreteRange.makeLog (range1.get_range_size(), range1.get_range_min(), range1.get_range_max());
			value_array1 = range1.get_range_array();
		}

		if (gui_top.get_force_c_log() && var_name2.equals("c")) {
			range2 = OEDiscreteRange.makeLog (range2.get_range_size(), range2.get_range_min(), range2.get_range_max());
			value_array2 = range2.get_range_array();
		}

		// Get the scales

		int my_rscale1 = range1.get_natural_scale();
		int my_rscale2 = range2.get_natural_scale();

		rscale[0] = my_rscale1;
		rscale[1] = my_rscale2;

		// Uniformize the two ranges

		int uniform_length = 320;
		int min_match_length = 300;
		int max_match_length = 350;

		OEDiscreteRange uniform1 = uniformize_range (range1, uniform_length, min_match_length, max_match_length, true);
		OEDiscreteRange uniform2 = uniformize_range (range2, uniform_length, min_match_length, max_match_length, true);

		// Get the values of the uniform ranges

		double[] uniform_values1 = uniform1.get_range_array();
		double[] uniform_values2 = uniform2.get_range_array();

		// Get raster maps from the uniform ranges to the original ranges

		int[] raster1 = range1.make_bin_finder(false, false).find_bins (uniform_values1);
		int[] raster2 = range2.make_bin_finder(false, false).find_bins (uniform_values2);

		// Make the function

		int num_1 = uniform_values1.length;
		int num_2 = uniform_values2.length;

		double min_x = uniform_values1[0];
		double min_y = uniform_values2[0];

		double max_x = uniform_values1[num_1 - 1];
		double max_y = uniform_values2[num_2 - 1];

		double min_z = Double.MAX_VALUE;
		double max_z = -Double.MAX_VALUE;

		ArbDiscrXYZ_DataSet func = new ArbDiscrXYZ_DataSet ();

		for (int j1 = 0; j1 < num_1; ++j1) {
			for (int j2 = 0; j2 < num_2; ++j2) {
				double z = density_matrix[raster1[j1]][raster2[j2]];
				min_z = Math.min (min_z, z);
				max_z = Math.max (max_z, z);
				func.set (uniform_values1[j1], uniform_values2[j2], z);
			}
		}

		// Return the bounds

		x_bounds[0] = min_x;
		x_bounds[1] = max_x;

		y_bounds[0] = min_y;
		y_bounds[1] = max_y;

		z_bounds[0] = min_z;
		z_bounds[1] = max_z;

		return func;
	}




	// Make a 2D PDF given a marginal.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  var_name1 = Variable name #1.
	//  var_name2 = Variable name #2.
	//  dist_set = Marginal distribution set.
	//  data_name = Data name.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.
	//
	// Note: Testing shows that ArbDiscrXYZ_DataSet does not plot properly when one
	// of the axes is on a log scale.  So, use of this function is not recommended.

	private void make_2d_pdf_v2 (
		String title, String tab_text, String var_name1, String var_name2,
		OEMarginalDistSet dist_set, String data_name) throws GUIEDTException
	{
		// Natural scale

		int[] rscale = new int[2];
		rscale[0] = OEDiscreteRange.RSCALE_NULL;
		rscale[1] = OEDiscreteRange.RSCALE_NULL;

		// Get the Function

		double[] x_bounds = new double[2];
		double[] y_bounds = new double[2];
		double[] z_bounds = new double[2];

		ArbDiscrXYZ_DataSet func = make_2d_pdf_func_v2 (
			dist_set, var_name1, var_name2, data_name, rscale,
			x_bounds, y_bounds, z_bounds);

		if (func == null) {
			return;
		}
		
		// Get a color scale, for the Z-values in the pdf

		CPT cpt;
		try {
			cpt = GMT_CPT_Files.MAX_SPECTRUM.instance().rescale (z_bounds[0], z_bounds[1]);
		} catch (IOException e) {
			throw new InvariantViolationException ("OEGUIView.make_2d_pdf: Failed to make color scale", e);
		}

		// Create a plot and add it to the view

		String name1 = var_name1;
		String name2 = var_name2;
		
		XYZPlotSpec spec = new XYZPlotSpec (func, cpt, title, name1, name2, "Density");

		// Make the graph widget with appropriate scales

		double min_x = x_bounds[0];
		double max_x = x_bounds[1];

		double min_y = y_bounds[0];
		double max_y = y_bounds[1];

		PlotPreferences plotPrefs = PlotPreferences.getDefault();
		boolean xLog = OEDiscreteRange.is_rscale_log (rscale[0]);
		boolean yLog = OEDiscreteRange.is_rscale_log (rscale[1]);

		Range xRange;
		if (xLog) {
			xRange = new Range (min_x * Math.pow(max_x / min_x, -0.03), max_x * Math.pow(max_x / min_x, 0.03));
		} else {
			xRange = new Range (min_x - ((max_x - min_x) * 0.03), max_x + ((max_x - min_x) * 0.03));
		}

		Range yRange;
		if (yLog) {
			yRange = new Range (min_y * Math.pow(max_y / min_y, -0.03), max_y * Math.pow(max_y / min_y, 0.03));
		} else {
			yRange = new Range (min_y - ((max_y - min_y) * 0.03), max_y + ((max_y - min_y) * 0.03));
		}

		GraphWidget widget = new GraphWidget (spec, plotPrefs, xLog, yLog, xRange, yRange);

		// Set fonts

		setupGP(widget);

		// Add tab

		pdfGraphsPane.addTab (tab_text, null, widget);

		return;
	}




	// Plot the probability density functions.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_PARAMETERS.
	
	private void plotPDFs_v2() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotPDFs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_aftershock_params() )) {
			throw new IllegalStateException ("OEGUIView.plotPDFs - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Allocate a new component, or remove all the tabs from an existing component

		boolean new_tab = false;

		if (pdfGraphsPane == null) {
			pdfGraphsPane = new JTabbedPane();
			new_tab = true;
		} else {
			//  while (pdfGraphsPane.getTabCount() > 0) {
			//  	pdfGraphsPane.removeTabAt(0);
			//  }
			pdfGraphsPane.removeAll();
		}

		// Get RJ marginal distribution sets

		OEMarginalDistSet gen_dist_set = null;
		OEMarginalDistSet seq_dist_set = null;
		OEMarginalDistSet bay_dist_set = null;

		if (gui_model.get_genericModel() != null) {
			gen_dist_set = (new OEMarginalDistSetBuilder()).make_rj_marginals (gui_model.get_genericModel(), true);
		}

		if (gui_model.get_cur_model() != null) {
			seq_dist_set = (new OEMarginalDistSetBuilder()).make_rj_marginals (gui_model.get_cur_model(), true);
		}

		if (gui_model.get_bayesianModel() != null) {
			bay_dist_set = (new OEMarginalDistSetBuilder()).make_rj_marginals (gui_model.get_bayesianModel(), true);
		}

		// Get ETAS marginal distribution set

		OEMarginalDistSet etas_dist_set = gui_model.get_etas_marginals();

		boolean f_etas_active = gui_model.get_f_etas_active_custom();
		OEMarginalDistSet etas_act_dist_set = null;
		if (f_etas_active) {
			etas_act_dist_set = etas_dist_set;
		}

		// Add tabs for RJ 1D marginals

		make_1d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_A, "RJ " + OEMarginalDistSetBuilder.VNAME_RJ_A, OEMarginalDistSetBuilder.VNAME_RJ_A,
			gen_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			bay_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		make_1d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_P, "RJ " + OEMarginalDistSetBuilder.VNAME_RJ_P, OEMarginalDistSetBuilder.VNAME_RJ_P,
			gen_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			bay_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		make_1d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_C, "RJ " + OEMarginalDistSetBuilder.VNAME_RJ_C, OEMarginalDistSetBuilder.VNAME_RJ_C,
			gen_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB,
			bay_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		// Add tabs for RJ 2D PDFs

		make_2d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_A + " vs " + OEMarginalDistSetBuilder.VNAME_RJ_C,
			"RJ " + OEMarginalDistSetBuilder.VNAME_RJ_A + "/" + OEMarginalDistSetBuilder.VNAME_RJ_C,
			OEMarginalDistSetBuilder.VNAME_RJ_A,
			OEMarginalDistSetBuilder.VNAME_RJ_C,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		make_2d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_A + " vs " + OEMarginalDistSetBuilder.VNAME_RJ_P,
			"RJ " + OEMarginalDistSetBuilder.VNAME_RJ_A + "/" + OEMarginalDistSetBuilder.VNAME_RJ_P,
			OEMarginalDistSetBuilder.VNAME_RJ_A,
			OEMarginalDistSetBuilder.VNAME_RJ_P,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		make_2d_pdf (
			"RJ PDF for " + OEMarginalDistSetBuilder.VNAME_RJ_C + " vs " + OEMarginalDistSetBuilder.VNAME_RJ_P,
			"RJ " + OEMarginalDistSetBuilder.VNAME_RJ_C + "/" + OEMarginalDistSetBuilder.VNAME_RJ_P,
			OEMarginalDistSetBuilder.VNAME_RJ_C,
			OEMarginalDistSetBuilder.VNAME_RJ_P,
			seq_dist_set, OEMarginalDistSetBuilder.DNAME_RJ_PROB
		);

		// Add tabs for ETAS 1D marginals

		make_1d_pdf (
			"ETAS PDF for " + OEMarginalDistSetBuilder.VNAME_N, "E " + OEMarginalDistSetBuilder.VNAME_N, OEMarginalDistSetBuilder.VNAME_N,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN,
			etas_act_dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
		);

		make_1d_pdf (
			"ETAS PDF for " + OEMarginalDistSetBuilder.VNAME_ZAMS, "E " + OEMarginalDistSetBuilder.VNAME_ZAMS, OEMarginalDistSetBuilder.VNAME_ZAMS,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN,
			etas_act_dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
		);

		make_1d_pdf (
			"ETAS PDF for " + OEMarginalDistSetBuilder.VNAME_P, "E " + OEMarginalDistSetBuilder.VNAME_P, OEMarginalDistSetBuilder.VNAME_P,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN,
			etas_act_dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
		);

		make_1d_pdf (
			"ETAS PDF for " + OEMarginalDistSetBuilder.VNAME_C, "E " + OEMarginalDistSetBuilder.VNAME_C, OEMarginalDistSetBuilder.VNAME_C,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN,
			etas_act_dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
		);

		make_1d_pdf (
			"ETAS PDF for " + OEMarginalDistSetBuilder.VNAME_ZMU, "E " + OEMarginalDistSetBuilder.VNAME_ZMU, OEMarginalDistSetBuilder.VNAME_ZMU,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_GENERIC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_BAYESIAN,
			etas_act_dist_set, OEMarginalDistSetBuilder.DNAME_ACTIVE
		);

		// Add tabs for ETAS 2D PDFs

		String var1;
		String var2;

		var1 = OEMarginalDistSetBuilder.VNAME_N;
		var2 = OEMarginalDistSetBuilder.VNAME_ZAMS;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_N;
		var2 = OEMarginalDistSetBuilder.VNAME_P;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_N;
		var2 = OEMarginalDistSetBuilder.VNAME_C;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_N;
		var2 = OEMarginalDistSetBuilder.VNAME_ZMU;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_ZAMS;
		var2 = OEMarginalDistSetBuilder.VNAME_P;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_ZAMS;
		var2 = OEMarginalDistSetBuilder.VNAME_C;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_ZAMS;
		var2 = OEMarginalDistSetBuilder.VNAME_ZMU;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_P;
		var2 = OEMarginalDistSetBuilder.VNAME_C;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_P;
		var2 = OEMarginalDistSetBuilder.VNAME_ZMU;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		var1 = OEMarginalDistSetBuilder.VNAME_C;
		var2 = OEMarginalDistSetBuilder.VNAME_ZMU;

		make_2d_pdf_etas (
			"ETAS PDF for " + var1 + " vs " + var2,
			"E " + var1 + "/" + var2,
			var1,
			var2,
			etas_dist_set, OEMarginalDistSetBuilder.DNAME_SEQ_SPEC, f_etas_active
		);

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Model PDFs", null, pdfGraphsPane,
					"Aftershock Model Prob Dist Funcs");
		}

		return;
	}




	//----- ETAS number graph tab -----




	// Remove the ETAS number graphs tab.
	// Performs no operation if the probability density function plot is not currently in the tabbed pane.

	private void removeEtasNumGraphs () throws GUIEDTException {
		if (etasNumGraphsPane != null) {
			tabbedPane.remove (etasNumGraphsPane);
			etasNumGraphsPane = null;
		}
		return;
	}




	// Plot the ETAS number graphs.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_PARAMETERS.
	
	private void plotEtasNumGraphs() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotEtasNumGraphs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_aftershock_params() )) {
			throw new IllegalStateException ("OEGUIView.plotEtasNumGraphs - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Get the integrated intensity function, abort if we don't have it

		OEtasIntegratedIntensityFile ii_file = gui_model.get_etas_ii_file();

		if (ii_file == null) {
			System.out.println ("@@@@@ Exit: OEGUIView.plotEtasNumGraphs: Integrated intensity function not available");
			removeEtasNumGraphs();
			return;
		}

		boolean f_etas_active = gui_model.get_f_etas_active_custom();

		// Allocate a new component, or remove all the tabs from an existing component

		boolean new_tab = false;

		if (etasNumGraphsPane == null) {
			etasNumGraphsPane = new JTabbedPane();
			new_tab = true;
		} else {
			//  while (pdfGraphsPane.getTabCount() > 0) {
			//  	pdfGraphsPane.removeTabAt(0);
			//  }
			etasNumGraphsPane.removeAll();
		}

		// Make cumulative number and sequence specific v time

		make_etas_num_v_time (
			"ETAS Sequence Specific Number v Time",		// title
			"E Seq Number",								// tab_text
			ii_file,									// ii_file
			OEtasIntegratedIntensityFile.IIRANGE_ALL,	// ii_range
			true,										// f_cum
			false,										// f_gen
			true,										// f_seq
			false,										// f_bay
			false										// f_act
		);

		// Make cumulative number and all models v time

		make_etas_num_v_time (
			"ETAS Number v Time",						// title
			"E Number",									// tab_text
			ii_file,									// ii_file
			OEtasIntegratedIntensityFile.IIRANGE_ALL,	// ii_range
			true,										// f_cum
			true,										// f_gen
			true,										// f_seq
			true,										// f_bay
			f_etas_active								// f_act
		);

		// Make sequence specific v transformed time

		make_etas_num_v_transformed_time (
			"ETAS Sequence Specific Number v Intensity",	// title
			"E Seq T.Time",									// tab_text
			ii_file,										// ii_file
			OEtasIntegratedIntensityFile.IIRANGE_BORDERED,	// ii_range
			true,											// f_diag
			false,											// f_gen
			true,											// f_seq
			false,											// f_bay
			false											// f_act
		);

		// Make generic v transformed time

		//make_etas_num_v_transformed_time (
		//	"ETAS Generic Number v Intensity",				// title
		//	"E Gen T.Time",									// tab_text
		//	ii_file,										// ii_file
		//	OEtasIntegratedIntensityFile.IIRANGE_BORDERED,	// ii_range
		//	true,											// f_diag
		//	true,											// f_gen
		//	false,											// f_seq
		//	false,											// f_bay
		//	false											// f_act
		//);

		// Make Bayesian v transformed time

		//make_etas_num_v_transformed_time (
		//	"ETAS Bayesian Number v Intensity",				// title
		//	"E Bay T.Time",									// tab_text
		//	ii_file,										// ii_file
		//	OEtasIntegratedIntensityFile.IIRANGE_BORDERED,	// ii_range
		//	true,											// f_diag
		//	false,											// f_gen
		//	false,											// f_seq
		//	true,											// f_bay
		//	false											// f_act
		//);

		// Make active v transformed time

		//if (f_etas_active) {
		//	make_etas_num_v_transformed_time (
		//		"ETAS Custom Number v Intensity",				// title
		//		"E Custom T.Time",								// tab_text
		//		ii_file,										// ii_file
		//		OEtasIntegratedIntensityFile.IIRANGE_BORDERED,	// ii_range
		//		true,											// f_diag
		//		false,											// f_gen
		//		false,											// f_seq
		//		false,											// f_bay
		//		true											// f_act
		//	);
		//}

		// Make all models v transformed time

		make_etas_num_v_transformed_time (
			"ETAS Number v Intensity",						// title
			"E Transformed Time",							// tab_text
			ii_file,										// ii_file
			OEtasIntegratedIntensityFile.IIRANGE_BORDERED,	// ii_range
			true,											// f_diag
			true,											// f_gen
			true,											// f_seq
			true,											// f_bay
			f_etas_active									// f_act
		);

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("ETAS Number Plots", null, etasNumGraphsPane,
					"Plots of ETAS intensity and transformed time");
		}

		return;
	}




	// Make a function from two variables in the integrated intensity function.
	// Parameters:
	//  ii_file = Integrated intensity function.
	//  ii_range = Function range, from OEtasIntegratedIntensityFile.IIRANGE_XXXX.
	//  arg_var = Argument (x) variable,  from OEtasIntegratedIntensityFile.IIVAR_XXXX.
	//  arg_model = Argument (x) model,  from OEtasIntegratedIntensityFile.IIMODEL_XXXX.
	//  val_var = Value (y) variable,  from OEtasIntegratedIntensityFile.IIVAR_XXXX.
	//  val_model = Value (y) model,  from OEtasIntegratedIntensityFile.IIMODEL_XXXX.
	//  name = Name to assign, can be null for none.

	private ArbitrarilyDiscretizedFunc func_from_ii_file (
		OEtasIntegratedIntensityFile ii_file,
		int ii_range,
		int arg_var,
		int arg_model,
		int val_var,
		int val_model,
		String name
	) {
		// Get x and y ranges

		double[] args = ii_file.get_var_values (ii_range, arg_var, arg_model);
		double[] vals = ii_file.get_var_values (ii_range, val_var, val_model);

		// Make the function

		return func_from_args_vals (args, vals, name);
	}




	// Make an ETAS number versus time plot.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  ii_file = Integrated intensity function.
	//  ii_range = Function range, from OEtasIntegratedIntensityFile.IIRANGE_XXXX.
	//  f_cum = True to include cumulative number.
	//  f_gen = True to include generic model.
	//  f_seq = True to include sequence specific model.
	//  f_bay = True to include Bayesian model.
	//  f_act = True to include active model.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.

	private void make_etas_num_v_time (
		String title,
		String tab_text,
		OEtasIntegratedIntensityFile ii_file,
		int ii_range,
		boolean f_cum,
		boolean f_gen,
		boolean f_seq,
		boolean f_bay,
		boolean f_act
	) throws GUIEDTException {
		// List of functions and characteristics
		
		//List<PlotElement> funcs = new ArrayList<PlotElement>();
		List<ArbitrarilyDiscretizedFunc> funcs = new ArrayList<ArbitrarilyDiscretizedFunc>();
		List<PlotCurveCharacterstics> chars = new ArrayList<PlotCurveCharacterstics>();

		// Cumulative number

		if (f_cum) {

			double final_val = ii_file.get_final_var_value (
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,
				OEtasIntegratedIntensityFile.IIMODEL_COMMON
			);

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_T_DAY,			// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,			// val_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// val_model
				"Data: " + Math.round(final_val)					// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, Color.BLACK));
			}
		}

		// Sequence specific

		if (f_seq) {

			double final_val = ii_file.get_final_var_value (
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,
				OEtasIntegratedIntensityFile.IIMODEL_SEQSPEC
			);

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_T_DAY,			// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// val_var
				OEtasIntegratedIntensityFile.IIMODEL_SEQSPEC,		// val_model
				"SeqSpec: " + SimpleUtils.round_double_via_string ("%.3e", final_val)	// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, sequence_specific_color));
			}
		}

		// Generic

		if (f_gen) {

			double final_val = ii_file.get_final_var_value (
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,
				OEtasIntegratedIntensityFile.IIMODEL_GENERIC
			);

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_T_DAY,			// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// val_var
				OEtasIntegratedIntensityFile.IIMODEL_GENERIC,		// val_model
				"Generic: " + SimpleUtils.round_double_via_string ("%.3e", final_val)	// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, generic_color));
			}
		}

		// Bayesian

		if (f_bay) {

			double final_val = ii_file.get_final_var_value (
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,
				OEtasIntegratedIntensityFile.IIMODEL_BAYESIAN
			);

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_T_DAY,			// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// val_var
				OEtasIntegratedIntensityFile.IIMODEL_BAYESIAN,		// val_model
				"Bayesian: " + SimpleUtils.round_double_via_string ("%.3e", final_val)	// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, bayesian_color));
			}
		}

		// Active

		if (f_act) {

			double final_val = ii_file.get_final_var_value (
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,
				OEtasIntegratedIntensityFile.IIMODEL_ACTIVE
			);

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_T_DAY,			// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// val_var
				OEtasIntegratedIntensityFile.IIMODEL_ACTIVE,		// val_model
				"Custom: " + SimpleUtils.round_double_via_string ("%.3e", final_val)	// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, active_color));
			}
		}

		// If we got at least one function ...

		if (funcs.size() > 0) {

			// Get the x and y ranges (min y is zero)

			double min_x = Double.MAX_VALUE;
			double max_x = -Double.MAX_VALUE;
			double max_y = -Double.MAX_VALUE;
			double min_y = 0.0;

			for (ArbitrarilyDiscretizedFunc func : funcs) {
				min_x = Math.min (min_x, func.getMinX());
				max_x = Math.max (max_x, func.getMaxX());
				max_y = Math.max (max_y, func.getMaxY());
			}

			// Create a plot with the given functions and characteristics, and font sizes
		
			PlotSpec spec = new PlotSpec(funcs, chars, title, "Time (Days)", "Cumulative Number or Intensity");
			//spec.setLegendVisible(funcs.size() > 1);
			spec.setLegendVisible(true);	// always show legend even if just one curve

			GraphWidget widget;

			// Linear scale with auto-ranging
				
			widget = new GraphWidget(spec);

			// Adjust font sizes

			setupGP(widget);

			// Prevent the createion of a plot with an empty y-range equal to (0,0)

			if (max_y < 2.7) {
				widget.setY_AxisRange (0.0, 3.0);
			}

			// Add to the view

			etasNumGraphsPane.addTab (tab_text, null, widget);
		}

		return;
	}




	// Make an ETAS number versus transformed time plot.
	// Parameters:
	//  title = Plot title.
	//  tab_text = Text to appear on the tab.
	//  ii_file = Integrated intensity function.
	//  ii_range = Function range, from OEtasIntegratedIntensityFile.IIRANGE_XXXX.
	//  f_diag = True to include diagonal line.
	//  f_gen = True to include generic model.
	//  f_seq = True to include sequence specific model.
	//  f_bay = True to include Bayesian model.
	//  f_act = True to include active model.
	// Note: Throws GUIEDTException because it calls JTabbedPane.addTab.

	private void make_etas_num_v_transformed_time (
		String title,
		String tab_text,
		OEtasIntegratedIntensityFile ii_file,
		int ii_range,
		boolean f_diag,
		boolean f_gen,
		boolean f_seq,
		boolean f_bay,
		boolean f_act
	) throws GUIEDTException {
		// List of functions and characteristics
		
		//List<PlotElement> funcs = new ArrayList<PlotElement>();
		List<ArbitrarilyDiscretizedFunc> funcs = new ArrayList<ArbitrarilyDiscretizedFunc>();
		List<PlotCurveCharacterstics> chars = new ArrayList<PlotCurveCharacterstics>();

		// Sequence specific

		if (f_seq) {

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_SEQSPEC,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,			// val_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// val_model
				"SeqSpec"											// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, sequence_specific_color));
			}
		}

		// Generic

		if (f_gen) {

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_GENERIC,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,			// val_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// val_model
				"Generic"											// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, generic_color));
			}
		}

		// Bayesian

		if (f_bay) {

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_BAYESIAN,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,			// val_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// val_model
				"Bayesian"											// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, bayesian_color));
			}
		}

		// Active

		if (f_act) {

			ArbitrarilyDiscretizedFunc func = func_from_ii_file (
				ii_file,
				ii_range,
				OEtasIntegratedIntensityFile.IIVAR_CUM_INTEGRAL,	// arg_var
				OEtasIntegratedIntensityFile.IIMODEL_ACTIVE,		// arg_model
				OEtasIntegratedIntensityFile.IIVAR_CUM_NUM,			// val_var
				OEtasIntegratedIntensityFile.IIMODEL_COMMON,		// val_model
				"Custom"											// name
			);

			if (func != null) {
				funcs.add (func);
				chars.add (new PlotCurveCharacterstics (PlotLineType.SOLID, 3f, active_color));
			}
		}

		// If we got at least one function ...

		if (funcs.size() > 0) {

			// Get the x and y ranges (min x and min y are zero)

			double min_x = 0.0;
			double max_x = -Double.MAX_VALUE;
			double max_y = -Double.MAX_VALUE;
			double min_y = 0.0;

			for (ArbitrarilyDiscretizedFunc func : funcs) {
				max_x = Math.max (max_x, func.getMaxX());
				max_y = Math.max (max_y, func.getMaxY());
			}

			// Create diagonal line

			if (f_diag) {
				ArbitrarilyDiscretizedFunc func = new ArbitrarilyDiscretizedFunc();
				double diag_lo = 0.0;
				double diag_hi = Math.max (Math.min (3.0, max_x), max_y);
				func.set (diag_lo, diag_lo);
				func.set (diag_hi, diag_hi);
				func.setName ("Diagonal");

				funcs.add (0, func);
				chars.add (0, new PlotCurveCharacterstics (PlotLineType.DASHED, 3f, Color.BLACK));
				
				max_x = Math.max (max_x, func.getMaxX());
				max_y = Math.max (max_y, func.getMaxY());
			}

			// Create a plot with the given functions and characteristics, and font sizes
		
			PlotSpec spec = new PlotSpec(funcs, chars, title, "Cumulative Intensity (Transformed Time)", "Cumulative Number");
			//spec.setLegendVisible(funcs.size() > 1);
			spec.setLegendVisible(true);	// always show legend even if just one curve

			GraphWidget widget;

			// Linear scale with auto-ranging
				
			widget = new GraphWidget(spec);

			// Adjust font sizes

			setupGP(widget);

			// Prevent the createion of a plot with an empty y-range equal to (0,0)

			if (max_y < 2.7) {
				widget.setY_AxisRange (0.0, 3.0);
			}

			// Add to the view

			etasNumGraphsPane.addTab (tab_text, null, widget);
		}

		return;
	}




	//----- Forecast MFD tab -----




//	// Pre-computed graphic elements for expected aftershock MFDs.
//		
//	private List<PlotElement> eafmd_funcs = null;
//	private List<PlotCurveCharacterstics> eafmd_chars = null;
//	private MinMaxAveTracker eafmd_yTrack = null;




//	// Pre-compute elements for expected aftershock MFDs.
//	// Parameters:
//	//  progress = Destination for progress messages, or null if none.
//	//  minDays = Start time of forecast, in days, from forecastStartTimeParam.
//	//  maxDays = End time of forecast, in days, from forecastEndTimeParam.
//	// This can execute on a worker thread.
//
//	public void precomputeEAMFD (GUICalcProgressBar progress, double minDays, double maxDays) {
//
//		if (gui_top.get_trace_events()) {
//			System.out.println ("@@@@@ Entry: OEGUIView.precomputeEAMFD");
//		}
//		
//		// Get the magnitude range to use
//
//		double minMag;
//		if (gui_model.get_cur_mainshock().getMag() < 6)
//			minMag = 3d;
//		else
//			minMag = 3d;
//		double maxMag = 9d;
//		double deltaMag = 0.1;
//		int numMag = (int)((maxMag - minMag)/deltaMag + 1.5);
//
//		// Allocate lists of functions and characteristics
//		
//		eafmd_funcs = Lists.newArrayList();
//		eafmd_chars = Lists.newArrayList();
//
//		// Lists of models, each with name and color
//		
//		List<RJ_AftershockModel> models = Lists.newArrayList();
//		List<String> names = Lists.newArrayList();
//		List<Color> colors = Lists.newArrayList();
//
//		// Add sequence-specific model
//		
//		models.add(gui_model.get_cur_model());
//		names.add("Seq. Specific");
//		colors.add(sequence_specific_color);
//		
//		if (gui_model.get_genericModel() != null) {
//
//			// Add generic model
//
//			models.add(gui_model.get_genericModel());
//			names.add("Generic");
//			colors.add(generic_color);
//			
//			if (gui_model.get_bayesianModel() != null) {
//
//				// Add Bayesian model
//
//				models.add(gui_model.get_bayesianModel());
//				names.add("Bayesian");
//				colors.add(bayesian_color);
//			}
//		}
//
//		// Fractiles for forecast uncertainty
//		
//		double[] fractiles = { 0.025, 0.975 };
//
//		// For each model ...
//		
//		for (int i=0; i<models.size(); i++) {
//
//			// Announce we are processing the model
//
//			String name = names.get(i);
//			if (progress != null) {
//				progress.updateProgress(i, models.size(), "Calculating "+name+"...");
//			}
//
//			// Calculate model mode and mean MFD
//
//			RJ_AftershockModel the_model = models.get(i);
//			EvenlyDiscretizedFunc mode = the_model.getModalCumNumMFD(minMag, maxMag, numMag, minDays, maxDays);
//			mode.setName(name+" Mode");
//			EvenlyDiscretizedFunc mean = the_model.getMeanCumNumMFD(minMag, maxMag, numMag, minDays, maxDays);
//			mean.setName("Mean");
//			Color c = colors.get(i);
//
//			// Plot the mode as a solid line
//			
//			eafmd_funcs.add(mode);
//			eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, c));
//
//			// Plot the mean as a dashed line
//			
//			eafmd_funcs.add(mean);
//			eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 1f, c));
//
//			// Calculate the fractile MFDs
//			
//			EvenlyDiscretizedFunc[] fractilesFuncs = the_model.getCumNumMFD_FractileWithAleatoryVariability(
//					fractiles, minMag, maxMag, numMag, minDays, maxDays);
//
//			// Plot the fractile MFDs as dotted lines
//			
//			for (int j= 0; j<fractiles.length; j++) {
//				double f = fractiles[j];
//				EvenlyDiscretizedFunc fractile = fractilesFuncs[j];
//				fractile.setName("p"+(float)(f*100d)+"%");
//				
//				eafmd_funcs.add(fractile);
//				eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DOTTED, 1f, c));
//			}
//		}
//
//		// Remove the model-specific message
//		
//		if (progress != null) {
//			//progress.setIndeterminate(true);
//			//progress.setProgressMessage("Plotting...");
//			progress.setIndeterminate(true, "Plotting...");
//		}
//		
//		// Mainshock mag and Bath's law, use evenly discr functions so that it shows up well at all zoom levels
//
//		double mainshockMag = gui_model.get_cur_mainshock().getMag();
//		double bathsMag = mainshockMag - 1.2;
//		DefaultXY_DataSet mainshockFunc = new DefaultXY_DataSet();
//		mainshockFunc.setName("Mainshock M="+(float)mainshockMag);
//		DefaultXY_DataSet bathsFunc = new DefaultXY_DataSet();
//		bathsFunc.setName("Bath's Law M="+(float)bathsMag);
//
//		// Determine range of Y-values
//		
//		eafmd_yTrack = new MinMaxAveTracker();
//		for (PlotElement elem : eafmd_funcs) {
//			if (elem instanceof XY_DataSet) {
//				XY_DataSet xy = (XY_DataSet)elem;
//				for (Point2D pt : xy)
//					if (pt.getY() > 0)
//						eafmd_yTrack.addValue(pt.getY());
//			}
//		}
//		System.out.println(eafmd_yTrack);
//
//		// Draw vertical dashed lines at the mainshock magnitude and Bath's law magnitude
//
//		EvenlyDiscretizedFunc yVals = new EvenlyDiscretizedFunc(eafmd_yTrack.getMin(), eafmd_yTrack.getMax(), 20);
//		for (int i=0; i<yVals.size(); i++) {
//			double y = yVals.getX(i);
//			mainshockFunc.set(mainshockMag, y);
//			bathsFunc.set(bathsMag, y);
//		}
//		eafmd_funcs.add(mainshockFunc);
//		eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.BLACK));
//		eafmd_funcs.add(bathsFunc);
//		eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.GRAY));		
//
//		return;
//	}




//	// Plot the forecast MFDs.
//	// This routine can re-plot an existing tab.
//	// Must be called with model state >= MODSTATE_PARAMETERS.
//	
//	private void plotEAMFD() throws GUIEDTException {
//
//		if (gui_top.get_trace_events()) {
//			System.out.println ("@@@@@ Entry: OEGUIView.plotEAMFD, tab count = " + tabbedPane.getTabCount());
//		}
//
//		if (!( gui_model.modstate_has_forecast() )) {
//			throw new IllegalStateException ("OEGUIView.plotEAMFD - Invalid model state: " + gui_model.cur_modstate_string());
//		}
//
//		// Create a plot with the pre-computed functions and characteristics
//
//		final PlotSpec spec = new PlotSpec(eafmd_funcs, eafmd_chars, "Aftershock Forecast", "Magnitude", "Expected Num \u2265 Mag");
//		spec.setLegendVisible(true);
//
//		boolean new_tab = false;
//		
//		if (aftershockExpectedGraph == null) {
//			aftershockExpectedGraph = new GraphWidget(spec);
//			new_tab = true;
//		} else {
//			aftershockExpectedGraph.setPlotSpec(spec);
//		}
//
//		// Set axes and font sizes
//
//		aftershockExpectedGraph.setY_Log(true);
//		aftershockExpectedGraph.setY_AxisRange(new Range(eafmd_yTrack.getMin(), eafmd_yTrack.getMax()));
//		setupGP(aftershockExpectedGraph);
//
//		// Add to the view
//
//		if (new_tab) {
//			tabbedPane.addTab("Forecast", null, aftershockExpectedGraph,
//					"Aftershock Expected Frequency Plot");
//		}
//
//		return;
//	}




//	// Remove the expected aftershock MFDs plot.
//	// Performs no operation if the expected aftershock MFDs plot is not currently in the tabbed pane.
//
//	private void removeEAMFD () throws GUIEDTException {
//		tabbedPane.remove(aftershockExpectedGraph);
//		aftershockExpectedGraph = null;
//
//		// Also remove the pre-computed elements
//
//		eafmd_funcs = null;
//		eafmd_chars = null;
//		eafmd_yTrack = null;
//		return;
//	}




	//----- Aftershock forecast table tab -----




	// Plot the aftershock forecast table.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_FORECAST.
	
	private void plotAFTable() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: OEGUIView.plotAFTable, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_forecast() )) {
			throw new IllegalStateException ("OEGUIView.plotAFTable - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Allocate lists of models, names, and codes

		List<String> aft_names = new ArrayList<String>();
		List<Integer> aft_pmcodes = new ArrayList<Integer>();
		List<String> aft_forecasts =new ArrayList<String>();

		// Tab number for selected forecast

		int selected_tab = -1;

		// Add ETAS model
		
		if (gui_model.get_etasJSON() != null) {
			if (gui_model.get_isEtasSelected()) {
				selected_tab = aft_forecasts.size();
			}
			aft_names.add("ETAS");
			aft_pmcodes.add(ForecastResults.PMCODE_ETAS);
			aft_forecasts.add(gui_model.get_etasJSON());
		}

		// Add sequence-specific model
		
		if (gui_model.get_seqSpecJSON() != null) {
			if (gui_model.get_isSeqSpecSelected()) {
				selected_tab = aft_forecasts.size();
			}
			aft_names.add("RJ SeqSpecc");
			aft_pmcodes.add(ForecastResults.PMCODE_SEQ_SPEC);
			aft_forecasts.add(gui_model.get_seqSpecJSON());
		}

		// Add generic model
		
		if (gui_model.get_genericJSON() != null) {
			if (gui_model.get_isGenericSelected()) {
				selected_tab = aft_forecasts.size();
			}
			aft_names.add("RJ Generic");
			aft_pmcodes.add(ForecastResults.PMCODE_GENERIC);
			aft_forecasts.add(gui_model.get_genericJSON());
		}

		// Add bayesian model
		
		if (gui_model.get_bayesianJSON() != null) {
			if (gui_model.get_isBayesianSelected()) {
				selected_tab = aft_forecasts.size();
			}
			aft_names.add("RJ Bayesian");
			aft_pmcodes.add(ForecastResults.PMCODE_BAYESIAN);
			aft_forecasts.add(gui_model.get_bayesianJSON());
		}

		// Allocate a new component, or remove all the tabs from an existing component

		boolean new_tab = false;

		if (forecastTablePane == null) {
			forecastTablePane = new JTabbedPane();
			new_tab = true;
		} else {
			//  while (forecastTablePane.getTabCount() > 0) {
			//  	forecastTablePane.removeTabAt(0);
			//  }
			forecastTablePane.removeAll();
		}

		// For each model, add a tab

		JPanel selected_comp = null;
		List<OEGUIForecastTable> sync_list = new ArrayList<OEGUIForecastTable>();
		
		for (int i = 0; i < aft_forecasts.size(); i++) {
			String name = aft_names.get(i);
			int pmcode = aft_pmcodes.get(i);
			String forecastJSON = aft_forecasts.get(i);
			JPanel modcomp = (new OEGUIForecastTable(this, forecastJSON, name, pmcode, sync_list)).get_my_panel();
			if (i == selected_tab) {
				selected_comp = modcomp;
			}
			forecastTablePane.addTab(name, modcomp);
		}

		// If we found a selected forecast, select the corresponding tab

		//if (selected_tab >= 0) {
		//	forecastTablePane.setSelectedIndex (selected_tab);
		//}

		if (selected_comp != null) {
			forecastTablePane.setSelectedComponent (selected_comp);
		}

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Forecast Table", null, forecastTablePane,
					"USGS Forecast Table");
		}

		return;
	}




	// Remove the aftershock forecast table.
	// Performs no operation if the aftershock forecast table is not currently in the tabbed pane.

	private void removeAFTable () throws GUIEDTException {
		tabbedPane.remove(forecastTablePane);
		forecastTablePane = null;
		return;
	}




	//----- State -----




	// State change notification, when state is advanced.
	// Parameters:
	//  old_modstate = Prior model state, see MODSTATE_XXXXX.
	//  new_modstate = New model state, see MODSTATE_XXXXX.
	// It is guaranteed that new_modstate = old_modstate + 1.
	// When this is called, new_modstate is the current model state.

	public void view_advance_state (int old_modstate, int new_modstate) throws GUIEDTException {

		switch (new_modstate) {

		// Plots for fetching the catalog

		case MODSTATE_CATALOG:
			plotAftershockHypocs();
			plotMFDs();
			plotMagVsTime();
			plotCumulativeNum();
			plotCatalogText();
			tabbedPane.setSelectedComponent(epicenterGraph);
			break;

		// Plots for determining aftershock parameters

		case MODSTATE_PARAMETERS:
			plotCumulativeNum();	// re-plot because set of plots is changed
			//plotPDFs();
			plotPDFs_v2();
			plotEtasNumGraphs();
			tabbedPane.setSelectedComponent(pdfGraphsPane);
			break;

		// Plots for computing a forecast

		case MODSTATE_FORECAST:
			plotCumulativeNum();	// re-plot because time range is changed
			//plotEAMFD();
			plotAFTable();
			//tabbedPane.setSelectedComponent(aftershockExpectedGraph);
			tabbedPane.setSelectedComponent(forecastTablePane);
			break;
		}

		// Always plot the system information

		plotInfoText();

		return;
	}




	// State change notification, when state is retreated.
	// Parameters:
	//  old_modstate = Prior model state, see MODSTATE_XXXXX.
	//  new_modstate = New model state, see MODSTATE_XXXXX.
	// It is guaranteed that new_modstate < old_modstate.
	// When this is called, new_modstate is the current model state.

	public void view_retreat_state (int old_modstate, int new_modstate) throws GUIEDTException {

		// Plots for computing a forecast

		if (new_modstate < MODSTATE_FORECAST) {
			removeAFTable();
			//removeEAMFD();
			if (new_modstate >= MODSTATE_PARAMETERS && old_modstate >= MODSTATE_FORECAST) {
				plotCumulativeNum();	// re-plot because time range is changed
			}
		}

		// Plots for determining aftershock parameters

		if (new_modstate < MODSTATE_PARAMETERS) {
			removeEtasNumGraphs();
			removePDFs();
			if (new_modstate >= MODSTATE_CATALOG && old_modstate >= MODSTATE_PARAMETERS) {
				plotCumulativeNum();	// re-plot because set of models is changed
			}
		}

		// Plots for fetching the catalog

		if (new_modstate < MODSTATE_CATALOG) {
			removeCatalogText();
			removeCumulativeNum();
			removeMagVsTime();
			removeMFDs();
			removeAftershockHypocs();
		}

		// Always plot the system information

		plotInfoText();

		//TODO: Set selected component (maybe)

		return;
	}




	// Notification that the model mcParam has changed.
	// This is called only in state MODSTATE_CATALOG.

	public void view_notify_changed_mcParam () throws GUIEDTException {

		// Re-plot the magnitude number distribution, if it is currently visible

		if (magNumGraph != null) {
			plotMFDs();
		}

		// Re-plot the cumulative number graph, if it is currently visible
		// Note: In MODSTATE_CATALOG, the cumulative number plot uses mcParam
		// as its minimum magnitude.  In later states, it uses the time-dependent
		// magnitude of completeness from the seq-spec model.

		if (cmlNumGraph != null) {
			plotCumulativeNum();
		}

		// Always plot the system information

		plotInfoText();

		return;
	}




	// Notification that the model bParam has changed.
	// This is called only in state MODSTATE_CATALOG.

	public void view_notify_changed_bParam () throws GUIEDTException {

		// Re-plot and select the magnitude number distribution, if it is currently visible

		if (magNumGraph != null) {
			plotMFDs();
			tabbedPane.setSelectedComponent(magNumGraph);
		}

		// Always plot the system information

		plotInfoText();

		return;
	}




	// Set the view to the console window.
	// This can be called in any state.

	public void view_show_console () throws GUIEDTException {
		tabbedPane.setSelectedComponent(consoleScroll);
		return;
	}




	//----- Construction -----


	// Construct the view.

	public OEGUIView () {
	}


	// Perform initialization after all components are linked.
	// The call order is: model, view, controller.

	public void post_link_init () throws GUIEDTException {

		// Initialize the tabbed container

		init_tabbedPane();

		return;
	}




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
