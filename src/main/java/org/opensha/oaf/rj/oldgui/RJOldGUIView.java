package org.opensha.oaf.rj.oldgui;

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
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Reasenberg & Jones GUI - View implementation.
// Michael Barall 03/15/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the view.
// It holds the displays that appear on the right side of the main window.


public class RJOldGUIView extends RJOldGUIComponent {


	//----- Tabs -----


	// Index numbers for tabs. [Deprecated]

	private static final int console_tab_index = 0;
	private static final int epicenter_tab_index = 1;
	private static final int mag_num_tab_index = 2;
	private static final int mag_time_tab_index = 3;
	private static final int cml_num_tab_index = 4;
	private static final int catalog_tab_index = 5;
	private static final int pdf_tab_index = 6;
	private static final int aftershock_expected_index = 7;
	private static final int forecast_table_tab_index = 8;


	// Components for tabs.

	private JScrollPane consoleScroll;				// Console window [MODSTATE_INITIAL]

	private GraphWidget epicenterGraph;				// Map of aftershock epicenters [MODSTATE_CATALOG]

	private GraphWidget magNumGraph;				// Plot of number vs magnitude, re-plottable [MODSTATE_CATALOG]

	private GraphWidget magTimeGraph;				// Plot of magnitude vs time [MODSTATE_CATALOG]

	private GraphWidget cmlNumGraph;				// Plot of cumulative number vs time, re-plottable [MODSTATE_CATALOG]

	private JTextArea catalogText;					// List of aftershock time, locations, magnitudes [MODSTATE_CATALOG]
	private JScrollPane catalogTextPane;			// This is the component added to the tabbed pane.

	private JTabbedPane pdfGraphsPane;				// Tabbed plot of probability distribution function [MODSTATE_PARAMETERS]

	private GraphWidget aftershockExpectedGraph;	// Plot of forecast number vs magnitude [MODSTATE_FORECAST]

	private JTabbedPane forecastTablePane;			// Tabbed display of forecast tables and controls [MODSTATE_FORECAST]


	// Tabbed view container.

	private JTabbedPane tabbedPane;

	public final JTabbedPane get_tabbedPane () {
		return tabbedPane;
	}

	private JTabbedPane init_tabbedPane () throws GUIEDTException {

		// Null out all tabs

		consoleScroll = null;

		epicenterGraph = null;

		magNumGraph = null;

		magTimeGraph = null;

		cmlNumGraph = null;

		catalogText = null;
		catalogTextPane = null;

		pdfGraphsPane = null;

		aftershockExpectedGraph = null;

		forecastTablePane = null;

		// Make the tabbed container
		
		tabbedPane = new JTabbedPane();

		// Make the console window

		GUIConsoleWindow console = new GUIConsoleWindow(true);
		consoleScroll = console.getScrollPane();
		consoleScroll.setSize(gui_top.get_consoleWidth(), gui_top.get_consoleHeight());
		JTextArea text = console.getTextArea();
		text.setCaretPosition(0);
		text.setCaretPosition(text.getText().length());

		tabbedPane.addTab("Console", null, consoleScroll, "View Console");

		return tabbedPane;
	}





	//----- Plotting definitions and subroutines -----


	// Colors for plotting the different models.

	private static final Color generic_color = Color.GREEN.darker();
	private static final Color bayesian_color = Color.RED;
	private static final Color sequence_specific_color = Color.BLUE.darker();


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




	//----- Epicenter plot tab -----




	// Plot the epicenters.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	
	private void plotAftershockHypocs() {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotAftershockHypocs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("RJOldGUIView.plotAftershockHypocs - Invalid model state: " + gui_model.cur_modstate_string());
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
			for(Location loc:trace)
				traceFunc.set(loc.getLongitude(), loc.getLatitude());
			funcs.add(traceFunc);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));

		// Otherwise, draw a symbol at the mainshock Location

		} else {
			Location hypo = gui_model.get_cur_mainshock().getHypocenterLocation();
			DefaultXY_DataSet xy = new DefaultXY_DataSet(new double[] {hypo.getLongitude()},
					new double[] {hypo.getLatitude()});
			xy.setName("Main Shock Location");
			funcs.add(xy);
			float size = (float)my_magSizeFunc.getY(my_magSizeFunc.getClosestXIndex(gui_model.get_cur_mainshock().getMag()));
			Color c;
			if (colorByTime)
				c = timeCPT.getMinColor();		//TODO: Use color for mainshock time, not necessarily min color
			else
				c = Color.BLACK;
			//chars.add(new PlotCurveCharacterstics(PlotSymbol.FILLED_CIRCLE, size, c));
			chars.add(new PlotCurveCharacterstics(PlotSymbol.CIRCLE, size, c));
		}
		
		// Create lists of aftershock epicenters, magnitudes, and times after mainshock in days

		List<Point2D> points = Lists.newArrayList();
		List<Double> mags = Lists.newArrayList();
		List<Double> timeDeltas = Lists.newArrayList();
		for (ObsEqkRupture rup : gui_model.get_cur_aftershocks()) {
			Location loc = rup.getHypocenterLocation();
			points.add(new Point2D.Double(loc.getLongitude(), loc.getLatitude()));
			mags.add(rup.getMag());
			timeDeltas.add(getTimeSinceMainshock(rup));
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
			if ((timeCPT.getMaxValue() - timeCPT.getMinValue()) < 10)
				cptInc = 1d;
			subtitle = XYZGraphPanel.getLegendForCPT(timeCPT, "Time (days)", axisLabelFontSize, tickLabelFontSize,
					cptInc, RectangleEdge.RIGHT);

		// Otherwise, no colors ...

		} else {
			// Bin aftershocks by magnitude

			XY_DataSet[] aftershockDatasets = XY_DatasetBinner.bin(points, mags, my_magSizeFunc);
			
			buildFuncsCharsForBinned(aftershockDatasets, funcs, chars, PlotSymbol.CIRCLE);
		}
		
		// Now add outline of region, if we have one

		if (gui_model.get_cur_region() != null) {
			DefaultXY_DataSet outline = new DefaultXY_DataSet();
			for (Location loc : gui_model.get_cur_region().getBorder())
				outline.set(loc.getLongitude(), loc.getLatitude());
			outline.set(outline.get(0));
			outline.setName("Region Outline");
			
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
		if (gui_model.get_cur_region() != null) {
			epicenterGraph.setAxisRange(gui_model.get_cur_region().getMinLon()-regBuff, gui_model.get_cur_region().getMaxLon()+regBuff,
					gui_model.get_cur_region().getMinLat()-regBuff, gui_model.get_cur_region().getMaxLat()+regBuff);

		// Otherwise, adjust axis ranges to hold all the aftershocks

		} else {
			 MinMaxAveTracker latTrack = new MinMaxAveTracker();
			 MinMaxAveTracker lonTrack = new MinMaxAveTracker();
			 latTrack.addValue(gui_model.get_cur_mainshock().getHypocenterLocation().getLatitude());
			 lonTrack.addValue(gui_model.get_cur_mainshock().getHypocenterLocation().getLongitude());
			 for (ObsEqkRupture rup : gui_model.get_cur_aftershocks()) {
				 Location loc = rup.getHypocenterLocation();
				 latTrack.addValue(loc.getLatitude());
				 lonTrack.addValue(loc.getLongitude());
			 }
			 epicenterGraph.setAxisRange(lonTrack.getMin()-regBuff, lonTrack.getMax()+regBuff,
						latTrack.getMin()-regBuff, latTrack.getMax()+regBuff);
		}

		// Set font sizes
		
		setupGP(epicenterGraph);
		
		if (subtitle != null)
			epicenterGraph.getGraphPanel().addSubtitle(subtitle);

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
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotMFDs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("RJOldGUIView.plotMFDs - Invalid model state: " + gui_model.cur_modstate_string());
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
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotMagVsTime, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("RJOldGUIView.plotMagVsTime - Invalid model state: " + gui_model.cur_modstate_string());
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
				dists.add(LocationUtils.horzDistanceFast(gui_model.get_cur_mainshock().getHypocenterLocation(), aftershock.getHypocenterLocation()));
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
			
			subtitle = XYZGraphPanel.getLegendForCPT(my_distCPT, "Distance (km)", axisLabelFontSize, tickLabelFontSize,
					0d, RectangleEdge.RIGHT);
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
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotCumulativeNum, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("RJOldGUIView.plotCumulativeNum - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Determine whether to use a constant or time-varying magnitude of completeness,
		// and if the latter then get the magnitude-of-completeness function

		double magMin;
		MagCompFn magCompFn;
		MagCompFn magCompFnPlot;
		double magMain = gui_model.get_cur_mainshock().getMag();
		String magMinCaption;

		// If we have a seq-spec model, use its magnitude-of-completeness function
		
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
			if (aftershock.getMag() < magCompFn.getMagCompleteness (magMain, magMin, time)) {
				continue;
			}
			count++;
			countFunc.set(time, count);
		}
		countFunc.set(gui_model.get_cat_dataEndTimeParam(), count);
		countFunc.setName("Data: "+(int)countFunc.getMaxY());

		// Save the final count
		
		double maxY = count;

		// Create lists of functions and characteristics, and add the count function to the lists
		
		List<PlotElement> funcs = Lists.newArrayList();
		List<PlotCurveCharacterstics> chars = Lists.newArrayList();
		
		funcs.add(countFunc);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, Color.BLACK));

		// If we have a seq-spec model, graph it
		
		if (gui_model.get_cur_model() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_cur_model(), magMin, magCompFnPlot);
			
			maxY = Math.max(count, expected.getMaxY());
			
			funcs.add(expected);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, sequence_specific_color));
			
			expected.setName("Seq Specific: "+new DecimalFormat("0.#").format(expected.getMaxY()));
		}

		// If we have a generic model, graph it
		
		if (gui_model.get_genericModel() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_genericModel(), magMin, magCompFnPlot);
			
			maxY = Math.max(count, expected.getMaxY());
			
			funcs.add(expected);
			chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 3f, generic_color));
			
			expected.setName("Generic: "+new DecimalFormat("0.#").format(expected.getMaxY()));
		}

		// If we have a bayesian model, graph it
			
		if (gui_model.get_bayesianModel() != null) {
			EvenlyDiscretizedFunc expected = getModelCumNumWithTimePlot(gui_model.get_bayesianModel(), magMin, magCompFnPlot);
				
			maxY = Math.max(count, expected.getMaxY());
				
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
		double tMin = gui_model.get_cat_dataStartTimeParam();
		double tMax = gui_model.get_cat_dataEndTimeParam();
		if (gui_model.modstate_has_forecast()) {
			tMax = Math.max(tMax, gui_model.get_cat_forecastEndTimeParam());	// if we have a forecast, extend at least to end of forecast
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




	// Plot the current catalog.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_CATALOG.
	
	private void plotCatalogText() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotCatalogText, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_catalog() )) {
			throw new IllegalStateException ("RJOldGUIView.plotCatalogText - Invalid model state: " + gui_model.cur_modstate_string());
		}

		StringBuilder sb = new StringBuilder();

		// Header line

		sb.append("# Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude\n");
		
		// Mainshock
		
		sb.append("# Main Shock:\n");
		sb.append("# ").append(GUIExternalCatalog.getCatalogLine(gui_model.get_cur_mainshock())).append("\n");
		
		// Aftershocks
		
		for (ObsEqkRupture rup : gui_model.get_cur_aftershocks()) {
			sb.append(GUIExternalCatalog.getCatalogLine(rup)).append("\n");
		}

		// Create or modify the text pane

		if (catalogText == null) {
			catalogText = new JTextArea(sb.toString());
			catalogText.setEditable(false);
			catalogTextPane = new JScrollPane(catalogText);
			tabbedPane.addTab("Catalog", null, catalogTextPane, "Aftershock Catalog");
		} else {
			catalogText.setText(sb.toString());
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
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotPDFs, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_aftershock_params() )) {
			throw new IllegalStateException ("RJOldGUIView.plotPDFs - Invalid model state: " + gui_model.cur_modstate_string());
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
		
		XYZGraphPanel xyzGP = new XYZGraphPanel();
		pdfGraphsPane.addTab(name1+" vs "+name2, null, xyzGP);

		// Draw the PDF

		double xDelta = pdf.getGridSpacingX();
		double yDelta = pdf.getGridSpacingY();
		xyzGP.drawPlot(spec, false, false,
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




	//----- Forecast MFD tab -----




	// Pre-computed graphic elements for expected aftershock MFDs.
		
	private List<PlotElement> eafmd_funcs = null;
	private List<PlotCurveCharacterstics> eafmd_chars = null;
	private MinMaxAveTracker eafmd_yTrack = null;




	// Pre-compute elements for expected aftershock MFDs.
	// Parameters:
	//  progress = Destination for progress messages, or null if none.
	//  minDays = Start time of forecast, in days, from forecastStartTimeParam.
	//  maxDays = End time of forecast, in days, from forecastEndTimeParam.
	// This can execute on a worker thread.

	public void precomputeEAMFD (GUICalcProgressBar progress, double minDays, double maxDays) {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: RJOldGUIView.precomputeEAMFD");
		}
		
		// Get the magnitude range to use

		double minMag;
		if (gui_model.get_cur_mainshock().getMag() < 6)
			minMag = 3d;
		else
			minMag = 3d;
		double maxMag = 9d;
		double deltaMag = 0.1;
		int numMag = (int)((maxMag - minMag)/deltaMag + 1.5);

		// Allocate lists of functions and characteristics
		
		eafmd_funcs = Lists.newArrayList();
		eafmd_chars = Lists.newArrayList();

		// Lists of models, each with name and color
		
		List<RJ_AftershockModel> models = Lists.newArrayList();
		List<String> names = Lists.newArrayList();
		List<Color> colors = Lists.newArrayList();

		// Add sequence-specific model
		
		models.add(gui_model.get_cur_model());
		names.add("Seq. Specific");
		colors.add(sequence_specific_color);
		
		if (gui_model.get_genericModel() != null) {

			// Add generic model

			models.add(gui_model.get_genericModel());
			names.add("Generic");
			colors.add(generic_color);
			
			if (gui_model.get_bayesianModel() != null) {

				// Add Bayesian model

				models.add(gui_model.get_bayesianModel());
				names.add("Bayesian");
				colors.add(bayesian_color);
			}
		}

		// Fractiles for forecast uncertainty
		
		double[] fractiles = { 0.025, 0.975 };

		// For each model ...
		
		for (int i=0; i<models.size(); i++) {

			// Announce we are processing the model

			String name = names.get(i);
			if (progress != null) {
				progress.updateProgress(i, models.size(), "Calculating "+name+"...");
			}

			// Calculate model mode and mean MFD

			RJ_AftershockModel the_model = models.get(i);
			EvenlyDiscretizedFunc mode = the_model.getModalCumNumMFD(minMag, maxMag, numMag, minDays, maxDays);
			mode.setName(name+" Mode");
			EvenlyDiscretizedFunc mean = the_model.getMeanCumNumMFD(minMag, maxMag, numMag, minDays, maxDays);
			mean.setName("Mean");
			Color c = colors.get(i);

			// Plot the mode as a solid line
			
			eafmd_funcs.add(mode);
			eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, c));

			// Plot the mean as a dashed line
			
			eafmd_funcs.add(mean);
			eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 1f, c));

			// Calculate the fractile MFDs
			
			EvenlyDiscretizedFunc[] fractilesFuncs = the_model.getCumNumMFD_FractileWithAleatoryVariability(
					fractiles, minMag, maxMag, numMag, minDays, maxDays);

			// Plot the fractile MFDs as dotted lines
			
			for (int j= 0; j<fractiles.length; j++) {
				double f = fractiles[j];
				EvenlyDiscretizedFunc fractile = fractilesFuncs[j];
				fractile.setName("p"+(float)(f*100d)+"%");
				
				eafmd_funcs.add(fractile);
				eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DOTTED, 1f, c));
			}
		}

		// Remove the model-specific message
		
		if (progress != null) {
			//progress.setIndeterminate(true);
			//progress.setProgressMessage("Plotting...");
			progress.setIndeterminate(true, "Plotting...");
		}
		
		// Mainshock mag and Bath's law, use evenly discr functions so that it shows up well at all zoom levels

		double mainshockMag = gui_model.get_cur_mainshock().getMag();
		double bathsMag = mainshockMag - 1.2;
		DefaultXY_DataSet mainshockFunc = new DefaultXY_DataSet();
		mainshockFunc.setName("Mainshock M="+(float)mainshockMag);
		DefaultXY_DataSet bathsFunc = new DefaultXY_DataSet();
		bathsFunc.setName("Bath's Law M="+(float)bathsMag);

		// Determine range of Y-values
		
		eafmd_yTrack = new MinMaxAveTracker();
		for (PlotElement elem : eafmd_funcs) {
			if (elem instanceof XY_DataSet) {
				XY_DataSet xy = (XY_DataSet)elem;
				for (Point2D pt : xy)
					if (pt.getY() > 0)
						eafmd_yTrack.addValue(pt.getY());
			}
		}
		System.out.println(eafmd_yTrack);

		// Draw vertical dashed lines at the mainshock magnitude and Bath's law magnitude

		EvenlyDiscretizedFunc yVals = new EvenlyDiscretizedFunc(eafmd_yTrack.getMin(), eafmd_yTrack.getMax(), 20);
		for (int i=0; i<yVals.size(); i++) {
			double y = yVals.getX(i);
			mainshockFunc.set(mainshockMag, y);
			bathsFunc.set(bathsMag, y);
		}
		eafmd_funcs.add(mainshockFunc);
		eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.BLACK));
		eafmd_funcs.add(bathsFunc);
		eafmd_chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.GRAY));		

		return;
	}




	// Plot the forecast MFDs.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_PARAMETERS.
	
	private void plotEAMFD() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotEAMFD, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_forecast() )) {
			throw new IllegalStateException ("RJOldGUIView.plotEAMFD - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Create a plot with the pre-computed functions and characteristics

		final PlotSpec spec = new PlotSpec(eafmd_funcs, eafmd_chars, "Aftershock Forecast", "Magnitude", "Expected Num \u2265 Mag");
		spec.setLegendVisible(true);

		boolean new_tab = false;
		
		if (aftershockExpectedGraph == null) {
			aftershockExpectedGraph = new GraphWidget(spec);
			new_tab = true;
		} else {
			aftershockExpectedGraph.setPlotSpec(spec);
		}

		// Set axes and font sizes

		aftershockExpectedGraph.setY_Log(true);
		aftershockExpectedGraph.setY_AxisRange(new Range(eafmd_yTrack.getMin(), eafmd_yTrack.getMax()));
		setupGP(aftershockExpectedGraph);

		// Add to the view

		if (new_tab) {
			tabbedPane.addTab("Forecast", null, aftershockExpectedGraph,
					"Aftershock Expected Frequency Plot");
		}

		return;
	}




	// Remove the expected aftershock MFDs plot.
	// Performs no operation if the expected aftershock MFDs plot is not currently in the tabbed pane.

	private void removeEAMFD () throws GUIEDTException {
		tabbedPane.remove(aftershockExpectedGraph);
		aftershockExpectedGraph = null;

		// Also remove the pre-computed elements

		eafmd_funcs = null;
		eafmd_chars = null;
		eafmd_yTrack = null;
		return;
	}




	//----- Aftershock forecast table tab -----




	// Plot the aftershock forecast table.
	// This routine can re-plot an existing tab.
	// Must be called with model state >= MODSTATE_FORECAST.
	
	private void plotAFTable() throws GUIEDTException {

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Entry: RJOldGUIView.plotAFTable, tab count = " + tabbedPane.getTabCount());
		}

		if (!( gui_model.modstate_has_forecast() )) {
			throw new IllegalStateException ("RJOldGUIView.plotAFTable - Invalid model state: " + gui_model.cur_modstate_string());
		}

		// Allocate lists of models, names, and colors

		List<RJ_AftershockModel> aft_models = Lists.newArrayList();
		List<String> aft_names = Lists.newArrayList();
		List<USGS_AftershockForecast> aft_forecasts = Lists.newArrayList();

		// Add sequence-specific model
		
		aft_models.add(gui_model.get_cur_model());
		aft_names.add("Seq. Specific");
		aft_forecasts.add(gui_model.get_seqSpecForecast());
		
		if (gui_model.get_genericModel() != null) {

			// Add generic model

			aft_models.add(gui_model.get_genericModel());
			aft_names.add("Generic");
			aft_forecasts.add(gui_model.get_genericForecast());
			
			if (gui_model.get_bayesianModel() != null) {

				// Add Bayesian model

				aft_models.add(gui_model.get_bayesianModel());
				aft_names.add("Bayesian");
				aft_forecasts.add(gui_model.get_bayesianForecast());
			}
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
		
		for (int i=0; i<aft_models.size(); i++) {
			String name = aft_names.get(i);
			USGS_AftershockForecast forecast = aft_forecasts.get(i);
			forecastTablePane.addTab(name, (new RJOldGUIForecastTable(this, forecast, name)).get_my_panel());
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
			plotPDFs();
			tabbedPane.setSelectedComponent(pdfGraphsPane);
			break;

		// Plots for computing a forecast

		case MODSTATE_FORECAST:
			plotEAMFD();
			plotAFTable();
			tabbedPane.setSelectedComponent(aftershockExpectedGraph);
			break;
		}

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
			removeEAMFD();
			if (new_modstate >= MODSTATE_PARAMETERS && old_modstate >= MODSTATE_FORECAST) {
				plotCumulativeNum();	// re-plot because time range is changed
			}
		}

		// Plots for determining aftershock parameters

		if (new_modstate < MODSTATE_PARAMETERS) {
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

		return;
	}




	//----- Construction -----


	// Construct the view.

	public RJOldGUIView () {
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
