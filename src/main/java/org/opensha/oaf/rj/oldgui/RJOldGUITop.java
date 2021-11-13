package org.opensha.oaf.rj.oldgui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.VersionInfo;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;


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
