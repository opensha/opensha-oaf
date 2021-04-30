package org.opensha.oaf.util;

import java.awt.Component;
import java.awt.Dialog.ModalityType;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

import org.opensha.sha.gui.infoTools.CalcProgressBar;
import org.opensha.commons.util.ClassUtils;


/**
 * Runs a sequence of operations on a worker thhread, with a progress monitor.
 * Author: Michael Barall 08/18/2018.
 */
public class GUICalcRunnable implements Runnable {

	// The owner of the progress monitor window, used as the owner of error dialogs; can be null.

	private Component owner;

	// The progress bar.

	private GUICalcProgressBar progress_bar;

	// The calculation steps to perform.

	private GUICalcStep[] steps;

	// This is called in the EDT after the progress bar is removed, typically to report final status to the user.

	private Runnable reporter;

	// An exception that occurred, or null if none.
		
	private volatile Throwable exception;	// written from multiple threads

	// Setting this flag true forces all calculation steps to occur in the event dispatch thread.

	private boolean forceEDT = false;

	// To construct, specify the owner of the progress monitor window, and the calculation steps.
		
	public GUICalcRunnable(Component owner, GUICalcStep... calcSteps) {
		this.owner = owner;
		this.progress_bar = new GUICalcProgressBar (owner, "", "", false);
		this.steps = calcSteps;
		this.reporter = null;
	}

	// Or, you can pass in the progress bar.
		
	public GUICalcRunnable(GUICalcProgressBar progress_bar, GUICalcStep... calcSteps) {
		this.owner = progress_bar.get_owner();
		this.progress_bar = progress_bar;
		this.steps = calcSteps;
		this.reporter = null;
	}

	// Use this to set a final status reporter.
	// Note: The reporter executes in the EDT and should be a simple task,
	// typically just a call to showMessageDialog.

	public void set_reporter (Runnable reporter) {
		this.reporter = reporter;
		return;
	}

	// This function runs in an application thread.

	@Override
	public void run() {

		// Initialize the progress bar

		GUICalcProgressBar my_progress_bar = progress_bar.req_init();

		// No exception so far

		exception = null;
		String curTitle = "No calculation";

		// For each calculation step ...

		for (final GUICalcStep step : steps) {

			// Update the progress bar

			my_progress_bar.req_update (step.get_title(), step.get_progressMessage());

			// Save the title of the current calculation step

			curTitle = step.get_title();

			// If we want to run in the event dispatch thread ...

			if (forceEDT || step.get_runInEDT()) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							try {
								step.get_run().run();
							} catch (Throwable e) {
								exception = e;
							}
						}
					});
				} catch (Exception e) {
					exception = e;
				}
			}

			// Otherwise, we want to run in the application thread (this thread)

			else {
				try {
					step.get_run().run();
				} catch (Throwable e) {
					exception = e;
				}
			}

			// If any exception occurred, stop the sequence of operations

			if (exception != null) {
				break;
			}
		}

		// Dispose of the progress bar

		my_progress_bar.req_dispose();

		// If an exception occurred, report it to the user

		if (exception != null) {
			final String title = "Error " + curTitle;
			final String message = exception.getMessage();
			//final String message = ClassUtils.getClassNameWithoutPackage(exception.getClass())+ ": " + exception.getMessage();
			exception.printStackTrace();
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
					}
				});
			} catch (Exception e) {
				System.err.println("Error displaying error message!");
				e.printStackTrace();
			}
		}

		// Otherwise, if there is a final status reporter, call it

		else if (reporter != null) {
			try {
				final String savedCurTitle = curTitle;
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						try {
							reporter.run();
						} catch (Throwable e) {
							//String title = "Error Reporting Results"
							String title = "Error " + savedCurTitle;
							String message = e.getMessage();
							e.printStackTrace();
							JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			} catch (Exception e) {
				System.err.println("Error invoking final status reporter!");
				e.printStackTrace();
			}
		}

		return;
	}

}
