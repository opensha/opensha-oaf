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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

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
import org.opensha.oaf.util.MarshalUtils;
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


// Reasenberg & Jones GUI - Aftershock table implementation.
// Michael Barall 04/22/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is the aftershock forecast table.
// Although within the view, it acts like a small controller.

	
public class OEGUIForecastTable extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_FCTAB_EXPORT = 901;				// Export button
	private static final int PARMGRP_FCTAB_PUBLISH = 902;				// Publish to PDL button
	private static final int PARMGRP_FCTAB_ADVISORY_DUR_PARAM = 903;	// Advisory duration parameter
	private static final int PARMGRP_FCTAB_TEMPLATE_PARAM = 904;		// Forecast template parameter
	private static final int PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM = 905;	// Include probability above mainshock parameter
	private static final int PARMGRP_FCTAB_INJECTABLE_TEXT = 906;		// Injectable text button
	

	//----- Parameters within the panel -----


	// Export JSON to file; button.

	private ButtonParameter exportButton;

	private ButtonParameter init_exportButton () throws GUIEDTException {
		exportButton = new ButtonParameter("JSON", "Export JSON...");
		register_param (exportButton, "exportButton" + my_suffix, PARMGRP_FCTAB_EXPORT);
		return exportButton;
	}


	// Publish forecast to PDL; button.

	private ButtonParameter publishButton;

	private ButtonParameter init_publishButton () throws GUIEDTException {
		String publish_forecast = "Publish Forecast (Dry Run)...";
		switch ((new ServerConfig()).get_pdl_enable()) {
		case ServerConfigFile.PDLOPT_DEV:
			publish_forecast = "Publish Forecast to PDL-Development...";
			break;
		case ServerConfigFile.PDLOPT_PROD:
			publish_forecast = "Publish Forecast to PDL-PRODUCTION...";
			break;
		case ServerConfigFile.PDLOPT_SIM_DEV:
			publish_forecast = "Publish Forecast to PDL-Dev [SIMULATED]...";
			break;
		case ServerConfigFile.PDLOPT_SIM_PROD:
			publish_forecast = "Publish Forecast to PDL-PROD [SIMULATED]...";
			break;
		case ServerConfigFile.PDLOPT_DOWN_DEV:
			publish_forecast = "Publish Forecast to PDL-Dev [SIM DOWN]...";
			break;
		case ServerConfigFile.PDLOPT_DOWN_PROD:
			publish_forecast = "Publish Forecast to PDL-PROD [SIM DOWN]...";
			break;
		}
		//publishButton = new ButtonParameter("USGS PDL", "Publish Forecast");
		publishButton = new ButtonParameter("USGS PDL", publish_forecast);
		register_param (publishButton, "publishButton" + my_suffix, PARMGRP_FCTAB_PUBLISH);
		return publishButton;
	}


	// Set forecast advisory duration; drop-down list.

	private EnumParameter<Duration> advisoryDurationParam;

	private EnumParameter<Duration> init_advisoryDurationParam () throws GUIEDTException {
		advisoryDurationParam = new EnumParameter<USGS_AftershockForecast.Duration>(
				"Advisory Duration", EnumSet.allOf(Duration.class), my_fc_holder.get_advisory_time_frame_as_enum(), null);
		register_param (advisoryDurationParam, "advisoryDurationParam" + my_suffix, PARMGRP_FCTAB_ADVISORY_DUR_PARAM);
		return advisoryDurationParam;
	}


	// Set forecast template; drip-down list.

	private EnumParameter<Template> templateParam;

	private EnumParameter<Template> init_templateParam () throws GUIEDTException {
		templateParam = new EnumParameter<USGS_AftershockForecast.Template>(
				"Template", EnumSet.allOf(Template.class), my_fc_holder.get_template_as_enum(), null);
		register_param (templateParam, "templateParam" + my_suffix, PARMGRP_FCTAB_TEMPLATE_PARAM);
		return templateParam;
	}


	// Include probability of aftershock larger than mainshock; checkbox.

	private BooleanParameter probAboveMainParam;

	private BooleanParameter init_probAboveMainParam () throws GUIEDTException {
		probAboveMainParam = new BooleanParameter("Include Prob \u2265 Main", my_fc_holder.get_include_above_mainshock());
		register_param (probAboveMainParam, "probAboveMainParam" + my_suffix, PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM);
		return probAboveMainParam;
	}


	// Set injectable text; button.

	private ButtonParameter injectableTextButton;

	private ButtonParameter init_injectableTextButton () throws GUIEDTException {
		injectableTextButton = new ButtonParameter("Injectable Text", "Set text...");
		register_param (injectableTextButton, "injectableTextButton" + my_suffix, PARMGRP_FCTAB_INJECTABLE_TEXT);
		return injectableTextButton;
	}




	//--- Parameter container ---


	// Parameter list occupies the top of the panel and holds our parameters.

	private GriddedParameterListEditor paramsEditor;

	private GriddedParameterListEditor init_paramsEditor () throws GUIEDTException {

		// Controls in the container

		ParameterList params = new ParameterList();

		params.addParameter(init_exportButton());

		params.addParameter(init_publishButton());

		params.addParameter(init_advisoryDurationParam());

		params.addParameter(init_templateParam());

		params.addParameter(init_probAboveMainParam());

		params.addParameter(init_injectableTextButton());

		// Create the container

		paramsEditor = new GriddedParameterListEditor(params, -1, 2);
		add_symbol (paramsEditor , "paramsEditor" + my_suffix);
		return paramsEditor;
	}




	//----- Internal variables -----


	// The forecast holder for this panel.
	// Note: This is a private copy, so we can modify it.

	private USGS_ForecastHolder my_fc_holder;


	// The name of this panel.

	private String my_name;


	// The suffix applied to parameter names in this panel.

	private String my_suffix;


	// Our panel.

	private JPanel my_panel;

	public JPanel get_my_panel () {
		return my_panel;
	}


	// File chooser dialog, for exporting JSON.
		
	private JFileChooser chooser;


	// PDL product and exception, for publishing forecast to PDL.

	private Product pdl_product;
	private Exception pdl_exception;




	//----- Debugging support -----


//	// Add all parameters to the symbol table.
//
//	private void setup_symbol_table () {
//
//		String suffix = "@" + my_name.replaceAll("\\s", "");	// remove white space from name
//
//		// Set up the symbol table
//
//		add_symbol (exportButton , "exportButton" + suffix);
//		add_symbol (publishButton , "publishButton" + suffix);
//		add_symbol (advisoryDurationParam , "advisoryDurationParam" + suffix);
//		add_symbol (templateParam , "templateParam" + suffix);
//		add_symbol (probAboveMainParam , "probAboveMainParam" + suffix);
//		add_symbol (injectableTextButton , "injectableTextButton" + suffix);
//
//		add_symbol (paramsEditor , "paramsEditor" + suffix);
//
//		return;
//	}




	//----- Construction -----




	// [DEPRECATED]
	// Constructor, accepts the forecast for this panel.
		
	public OEGUIForecastTable (OEGUIComponent gui_comp, USGS_AftershockForecast my_forecast, String my_name) throws GUIEDTException {
		this (gui_comp, my_forecast.buildJSONString(), my_name);
	}




	// Constructor, accepts the forecast for this panel.
		
	public OEGUIForecastTable (OEGUIComponent gui_comp, String my_json_string, String my_name) throws GUIEDTException {

		// Link components

		link_components (gui_comp);

		// Save the forecast and name

		this.my_fc_holder = new USGS_ForecastHolder();
		MarshalUtils.from_json_string (this.my_fc_holder, my_json_string);

		this.my_name = my_name;

		this.my_suffix = "@" + my_name.replaceAll("\\s", "");	// remove white space from name

		// Allocate the panel

		my_panel = new JPanel();
		my_panel.setLayout(new BorderLayout());

		// Initialize parameters, which occupy the top of the panel
			
		init_paramsEditor();

		// Set up symbol table

		//setup_symbol_table();

		// Set up the panel
			
		my_panel.add(paramsEditor, BorderLayout.NORTH);
		JTable jTable = new JTable(make_table_model (this.my_fc_holder));
		jTable.getTableHeader().setFont(jTable.getTableHeader().getFont().deriveFont(Font.BOLD));
		my_panel.add(jTable, BorderLayout.CENTER);
	}




	// Make a table model in swing format.

	public static TableModel make_table_model (USGS_ForecastHolder fch) {
		final USGS_ForecastHolder.GUITable gui_table = fch.make_gui_table();
		return new AbstractTableModel() {

			@Override
			public int getRowCount() {
				return gui_table.get_row_count();
			}

			@Override
			public int getColumnCount() {
				return gui_table.get_col_count();
			}

			@Override
			public Object getValueAt (int rowIndex, int columnIndex) {
				return gui_table.get_gui_text (rowIndex, columnIndex);
			}
			
		};
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {


		// *** Export to JSON file.

		case PARMGRP_FCTAB_EXPORT: {

			// Ask user to select file

			if (chooser == null) {
				chooser = new JFileChooser();
			}
			int ret = chooser.showSaveDialog(my_panel);
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();

				// Set injectable text

				String injText = gui_model.get_analyst_inj_text();
				if (injText != null && injText.length() == 0) {
					injText = null;
				}
				my_fc_holder.set_injectable_text(injText);

				// Convert forecast to JSON string, display error message if it fails

				String jsonText = null;
				try {
					jsonText = MarshalUtils.to_json_string (my_fc_holder);
				} catch (Exception e) {
					jsonText = null;
					e.printStackTrace();
					String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
					JOptionPane.showMessageDialog(my_panel, message, "Error building JSON", JOptionPane.ERROR_MESSAGE);
				}

				// If conversion succeeded, write to file, display error message if I/O error

				if (jsonText != null) {
					try {
						FileWriter fw = new FileWriter(file);
						fw.write(jsonText);
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
						String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
						JOptionPane.showMessageDialog(my_panel, message, "Error writing JSON", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		break;


		//*** Publish forecast to PDL

		case PARMGRP_FCTAB_PUBLISH: {

			// Ask user for confirmation

			String userInput = JOptionPane.showInputDialog(my_panel, "Type \"PDL\" and press OK to publish forecast", "Confirm publication", JOptionPane.PLAIN_MESSAGE);
				
			// User canceled, or did not enter correct text
				
			if (userInput == null || !(userInput.equals("PDL"))) {
				JOptionPane.showMessageDialog(my_panel, "Canceled: Forecast has NOT been sent to PDL", "Publication canceled", JOptionPane.INFORMATION_MESSAGE);

			// User confirmed ...

			} else {
				Product product = null;

				// Set injectable text

				String injText = gui_model.get_analyst_inj_text();
				if (injText != null && injText.length() == 0) {
					injText = null;
				}
				my_fc_holder.set_injectable_text(injText);

				// Build the PDL product, display error message if it failed

				try {
					//product = OAF_Publisher.createProduct(gui_model.get_cur_mainshock().getEventId(), my_forecast);
					String jsonText = MarshalUtils.to_json_string (my_fc_holder);

					//Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (gui_model.get_cur_mainshock(), ComcatOAFAccessor.EITMOPT_OMIT_NULL_EMPTY);
					//String eventNetwork = eimap.get (ComcatOAFAccessor.PARAM_NAME_NETWORK);
					//String eventCode = eimap.get (ComcatOAFAccessor.PARAM_NAME_CODE);
					String eventNetwork = gui_model.get_fcmain().mainshock_network;
					String eventCode = gui_model.get_fcmain().mainshock_code;

					String eventID = gui_model.get_cur_mainshock().getEventId();
					if (gui_top.get_pdlUseEventIDParam()) {
						eventID = gui_model.get_cat_eventIDParam();
					}
					long modifiedTime = 0L;
					boolean isReviewed = true;

					String suggestedCode = eventID;
					//long reviewOverwrite = 0L;
					long reviewOverwrite = -1L;
					String queryID = gui_model.get_cat_eventIDParam();
					JSONObject geojson = null;
					boolean f_gj_prod = true;
					eventID = PDLCodeChooserOaf.chooseOafCode (suggestedCode, reviewOverwrite,
						geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed);

					product = PDLProductBuilderOaf.createProduct (eventID, eventNetwork, eventCode, isReviewed, jsonText, modifiedTime);
				} catch (Exception e) {
					product = null;
					e.printStackTrace();
					String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
					JOptionPane.showMessageDialog(my_panel, message, "Error building product", JOptionPane.ERROR_MESSAGE);
				}

				// If we built the product, send it to PDL

				if (product != null) {

					//  boolean isSent = false;
					//  try {
					//  	//OAF_Publisher.sendProduct(product);
					//  	PDLSender.signProduct(product);
					//  	PDLSender.sendProduct(product, true);
					//  	isSent = true;
					//  } catch (Exception e) {
					//  	e.printStackTrace();
					//  	String message = ClassUtils.getClassNameWithoutPackage(e.getClass())+": "+e.getMessage();
					//  	JOptionPane.showMessageDialog(my_panel, message, "Error sending product", JOptionPane.ERROR_MESSAGE);
					//  }
					//  if (isSent) {
					//  	JOptionPane.showMessageDialog(my_panel, "Success: Forecast has been successfully sent to PDL", "Publication succeeded", JOptionPane.INFORMATION_MESSAGE);
					//  }

					pdl_product = product;
					pdl_exception = null;
					GUICalcStep pdlSendStep = new GUICalcStep("Sending product to PDL", "...", new Runnable() {
						@Override
						public void run() {
							try {
								//OAF_Publisher.sendProduct(product);
								PDLSender.signProduct(pdl_product);
								PDLSender.sendProduct(pdl_product, true);
							} catch (Exception e) {
								pdl_exception = e;
							}
						}
					});
					GUIEDTRunnable postSendStep = new GUIEDTRunnable() {
						
						@Override
						public void run_in_edt() throws GUIEDTException {
							// Pop up a message displaying the result, either success or error  TODO: Use a reporter for this
							if (pdl_exception == null) {
								JOptionPane.showMessageDialog(my_panel, "Success: Forecast has been successfully sent to PDL", "Publication succeeded", JOptionPane.INFORMATION_MESSAGE);
							} else {
								pdl_exception.printStackTrace();
								String message = ClassUtils.getClassNameWithoutPackage(pdl_exception.getClass())+": "+pdl_exception.getMessage();
								JOptionPane.showMessageDialog(my_panel, message, "Error sending product", JOptionPane.ERROR_MESSAGE);
							}
						}
					};
					GUICalcRunnable.run_steps (gui_top.get_top_window(), postSendStep, pdlSendStep);

				}
			}
		}
		break;


		//*** Set advisory duration, from dropdown list

		case PARMGRP_FCTAB_ADVISORY_DUR_PARAM: {
			my_fc_holder.set_advisory_time_frame_from_enum (validParam(advisoryDurationParam));
		}
		break;


		//*** Set PDL template, from dropdown list

		case PARMGRP_FCTAB_TEMPLATE_PARAM: {
			my_fc_holder.set_template_from_enum (validParam(templateParam));
		}
		break;


		//*** Select whether to include probability of an aftershock larger than mainshock, from checkbox

		case PARMGRP_FCTAB_PROB_ABOVE_MAIN_PARAM: {
			my_fc_holder.set_include_above_mainshock (validParam(probAboveMainParam));
		}
		break;


		//*** Enter the injectable text

		case PARMGRP_FCTAB_INJECTABLE_TEXT: {

			//  // Show a dialog containing the existing injectable text
			//  
			//  String prevText = my_fc_holder.get_injectable_text();
			//  if (prevText == null)
			//  	prevText = "";
			//  JTextArea area = new JTextArea(prevText);
			//  Dimension size = new Dimension(300, 200);
			//  area.setPreferredSize(size);
			//  area.setMinimumSize(size);
			//  area.setLineWrap(true);
			//  area.setWrapStyleWord(true);
			//  int ret = JOptionPane.showConfirmDialog(my_panel, area, "Set Injectable Text", JOptionPane.OK_CANCEL_OPTION);
			//  
			//  // If user entered new text, store it in the forecast
			//  
			//  if (ret == JOptionPane.OK_OPTION) {
			//  	String text = area.getText().trim();
			//  	if (text.length() == 0)
			//  		text = null;
			//  	my_fc_holder.set_injectable_text(text);
			//  }

			// Show a dialog containing the existing injectable text
			// (Same code as in OEGUIController.)

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
		}
		break;


		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUIForecastTable: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
