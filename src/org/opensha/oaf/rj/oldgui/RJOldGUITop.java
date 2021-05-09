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
import java.util.IdentityHashMap;

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
import org.opensha.oaf.util.GUIConsoleWindow;
import org.opensha.oaf.util.GUICalcStep;
import org.opensha.oaf.util.GUICalcRunnable;
import org.opensha.oaf.util.GUICalcProgressBar;
import org.opensha.oaf.util.GUIEDTException;
import org.opensha.oaf.util.GUIEDTRunnable;
import org.opensha.oaf.util.GUIEventAlias;
import org.opensha.oaf.util.GUIExternalCatalog;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.VersionInfo;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Reasenberg & Jones GUI - Top level window.
// Michael Barall 03/15/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the top-level window.


public class RJOldGUITop extends RJOldGUIComponent {

	//----- Program options -----

	// Setting this flag true forces all worker threads to run on the event dispatch thread.

	private boolean forceWorkerEDT = false;

	public final boolean get_forceWorkerEDT () {
		return forceWorkerEDT;
	}

	// Setting this flag true enables the patch for calculation steps that should be on
	// a worker thread, but currently must be on the EDT because they write to the screen.
	// Eventually these calculation steps should be split up.
	// ETA: All calculation steps have been split, and patches commented out.

	//private boolean patchWorkerEDT = true;

	// Setting this flag true causes PDL products to be created using the query ID
	// (contents of the eventIDParam parameter) as the eventID.  Setting this flag false
	// (which is the default) uses the authoritative event ID retrieved from Comcat.

	private boolean pdlUseEventIDParam = false;

	public final boolean get_pdlUseEventIDParam () {
		return pdlUseEventIDParam;
	}

	// Setting this flag true makes the tables include M4.
	// This is obsolete now that the standard tables include M4, but we retain it
	// as an example of how to include an alternative set of magnitudes.

	private boolean include_m4 = false;

	public final boolean get_include_m4 () {
		return include_m4;
	}

	// Setting this flag true enables tracing of GUI events.

	private boolean trace_events = true;

	public final boolean get_trace_events () {
		return trace_events;
	}




	//----- Window layout -----


	// The top-level window.

	private JFrame top_window;

	public final JFrame get_top_window () {
		return top_window;
	}


	// Width of parameter containers.

	private int paramWidth;

	public final int get_paramWidth () {
		return paramWidth;
	}


	// Width of view container.

	private int chartWidth;

	public final int get_chartWidth () {
		return chartWidth;
	}


	// Width of parameter containers.

	private int height;

	public final int get_height () {
		return height;
	}


	// Width of console text.

	private int consoleWidth;

	public final int get_consoleWidth () {
		return consoleWidth;
	}


	// Height of console text.

	private int consoleHeight;

	public final int get_consoleHeight () {
		return consoleHeight;
	}




	//----- Construction -----


	// Construct the top-level window and other components.
	// This is initialization that occurs before the GUI is displayed.
	// It executes in the EDT.
	
	public RJOldGUITop (boolean f_debug) {

		// Enable tracing in debug model

		trace_events = f_debug;

		// Set up top-level window layout

		top_window = new JFrame();

		paramWidth = 250;
		chartWidth = 800;
		height = 900;

		consoleWidth = 600;
		consoleHeight = 600;

		// Allocate the components

		gui_top = this;
		gui_model = new RJOldGUIModel();
		gui_view = new RJOldGUIView();
		gui_controller = new RJOldGUIController();

		// Link the compoents

		gui_model.link_components(this);
		gui_view.link_components(this);
		gui_controller.link_components(this);

		// Ensure we are on the event dispatch thread

		GUIEDTRunnable.check_on_edt();

		// Post-link initialization

		try {
			gui_model.post_link_init();
			gui_view.post_link_init();
			gui_controller.post_link_init();
		} catch (GUIEDTException e) {
			throw new IllegalStateException ("RJOldGUITop.RJOldGUITop - Caught GUIEDTException, which should never be thrown", e);
		}

		// Fill in the top-level window
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel paramsPanel = new JPanel(new BorderLayout());
		paramsPanel.add(gui_controller.get_dataEditor(), BorderLayout.WEST);
		paramsPanel.add(gui_controller.get_fitEditor(), BorderLayout.EAST);
		mainPanel.add(paramsPanel, BorderLayout.WEST);
		mainPanel.add(gui_view.get_tabbedPane(), BorderLayout.CENTER);
		
		get_top_window().setContentPane(mainPanel);
		get_top_window().setSize(get_paramWidth()*2 + get_chartWidth(), get_height());
		get_top_window().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		get_top_window().setTitle("Aftershock Statistics GUI");
		get_top_window().setLocationRelativeTo(null);
	}




	// Start the GUI.
	// This must run in the event dispatch thread.

	public void start () {

		// Ensure we are on the event dispatch thread

		GUIEDTRunnable.check_on_edt();

		//  try {

			// Show the window

			get_top_window().setVisible(true);

			// Say hello

			System.out.println(VersionInfo.get_title());
			System.out.println();

		//  } catch (GUIEDTException e) {
		//  	throw new IllegalStateException ("RJOldGUITop.start - Caught GUIEDTException, which should never be thrown", e);
		//  }

		return;
	}




	//----- Entry Point -----

	
	public static void main(String[] args) {

		// The GUI accepts certain command-line options and commands.
		// They are documented in GUICmd.

		String caller_name = "RJOldGUITop";

		boolean consumed = GUICmd.exec_gui_cmd (args, caller_name);

		if (consumed) {
			return;
		}

		// Run the GUI (Must run on the event dispatch thread!)

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				(new RJOldGUITop (GUICmd.f_gui_debug)).start();
			}
		});

		return;
	}

}
