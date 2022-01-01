package org.opensha.oaf.aafs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.opensha.oaf.util.health.HealthMonitor;
import org.opensha.oaf.util.health.SimpleHealthCounter;
import org.opensha.oaf.util.health.SimpleHealthAlerter;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.pdl.PDLSender;


/**
 * Support functions for subsystem health monitoring.
 * Author: Michael Barall 12/28/2021.
 */
public class HealthSupport extends ServerComponent {




	//----- Health status flags -----


	// The bits within a long that are valid to use as status flags.
	// Note: The encoding of RiServerStatus allows a maximum of 52 bits.

	public static final long HS_VALID_BITS		= 0xFFFFFFFFFFFFFL;

	// The bits that currently have known interpretations.

	public static final long HS_KNOWN_BITS		= 0x0000000000F03L;

	// Indicates that health monitoring is active.
	// Note: If this bit is zero, clients should assume OK status.

	public static final long HS_ACTIVE			= 0x0000000000001L;

	// Indicates that a health status alert is in progress.
	// Note: When an alert is in progress, this bit is set in addtion to other
	// bits that specify the cause(s) of the alert.
	// Note: This bit must be zero if HS_ACTIVE is zero.

	public static final long HS_ALERT			= 0x0000000000002L;

	// An alert for Comcat, indicating an inability to communicate with Comcat.

	public static final long HS_COMCAT			= 0x0000000000100L;

	// An alert for PDL, indicating an inability to communicate with PDL.

	public static final long HS_PDL				= 0x0000000000200L;

	// An alert for forecasts, indicating an inability to generate forecasts.

	public static final long HS_FORECAST		= 0x0000000000400L;

	// An alert for polling, indicating an inability to perform Comcat polls.

	public static final long HS_POLL			= 0x0000000000800L;

	// Value to use for status not available.

	public static final long HS_NOT_AVAILABLE	= 0L;




	// Return true if the status flags indicate good status.
	// A false return indicates that the server should be considered dead.

	public static boolean hs_good_status (long hs) {
		return (hs & HS_ALERT) == 0L;
	}




	// Produce a short summary of the status flags.

	public static String hs_short_summary (long hs) {
		if ((hs & HS_ALERT) == 0L) {
			if (hs == 0L) {
				return "HS_NONE";
			}
			if (hs == HS_ACTIVE) {
				return "HS_OK";
			}
			return String.format ("HS_OK 0x%X", hs);
		}
		return String.format ("HS_ALERT 0x%X", hs);
	}



	// Produce a list of alert causes.
	// Parameters:
	//  hs = Status code.
	//  prefix = String to insert before the first cause.
	//  suffix = String to insert after the last cause.
	//  infix = String to insert between causes.
	//  altfix = String to return if there are no causes, if null then use prefix followed by suffix.

	public static String hs_alert_causes (long hs, String prefix, String suffix, String infix, String altfix) {
		StringBuilder sb = new StringBuilder();

		boolean f_first = true;

		if ((hs & HS_COMCAT) != 0L) {
			sb.append (f_first ? prefix : infix);
			sb.append ("COMCAT");
			f_first = false;
		}

		if ((hs & HS_PDL) != 0L) {
			sb.append (f_first ? prefix : infix);
			sb.append ("PDL");
			f_first = false;
		}

		if ((hs & HS_FORECAST) != 0L) {
			sb.append (f_first ? prefix : infix);
			sb.append ("FORECAST");
			f_first = false;
		}

		if ((hs & HS_POLL) != 0L) {
			sb.append (f_first ? prefix : infix);
			sb.append ("POLL");
			f_first = false;
		}

		long unknown_bits = (hs & ~HS_KNOWN_BITS);
		if (unknown_bits != 0L) {
			sb.append (f_first ? prefix : infix);
			sb.append (String.format ("UNKNOWN(0x%X)", unknown_bits));
			f_first = false;
		}

		if (f_first) {
			if (altfix == null) {
				sb.append (prefix);
				sb.append (suffix);
			} else {
				sb.append (altfix);
			}
		} else {
			sb.append (suffix);
		}

		return sb.toString();
	}




	// Produce a long summary of the status flags.
	// The summary is a single line, and does not end with newline.
	// If an alert is in progress, the result lists the alert cause(s).

	public static String hs_long_summary (long hs) {
		if ((hs & HS_ALERT) == 0L) {
			if (hs == 0L) {
				return "HS_NONE";
			}
			if (hs == HS_ACTIVE) {
				return "HS_OK";
			}
			return String.format ("HS_OK 0x%X", hs);
		}
		return String.format ("HS_ALERT 0x%X%s", hs, hs_alert_causes (hs, " ", "", " ", ""));
	}




	// Produce a user alert of the status flags.
	// The user alert is a single line, and does not end with newline.
	// If an alert is in progress, the result lists the alert cause(s).

	public static String hs_user_alert (long hs) {
		if ((hs & HS_ALERT) == 0L) {
			if (hs == 0L) {
				return "Health status = NONE";
			}
			if (hs == HS_ACTIVE) {
				return "Health status = OK";
			}
			return String.format ("Health status = OK, code = 0x%X", hs);
		}
		return String.format ("Health status = ALERT, code = 0x%X%s", hs, hs_alert_causes (hs, ", cause = ", "", " ", ""));
	}




	//----- Health monitors -----


	// Health monitor for forecast, or null if none.

	private HealthMonitor forecast_health_monitor;

	public HealthMonitor get_forecast_health_monitor () {
		return forecast_health_monitor;
	}


	// Health monitor for poll, or null if none.

	private HealthMonitor poll_health_monitor;

	public HealthMonitor get_poll_health_monitor () {
		return poll_health_monitor;
	}




	// Set up all health monitors.
	// Note: Can also be used to reload health monitoring parameters.
	//
	// Implementation note: Eventually parameters will come from ServerConfig.

	private void setup_health_monitors () {

		// Number of milliseconds per minute, hour, day

		long minute = 60L * 1000L;
		long hour = 60L * minute;
		long day = 24L * hour;

		// Comcat

		long[] comcat_alert_rules = new long[4];
		comcat_alert_rules[0] = 1L * hour;
		comcat_alert_rules[1] = 10L;
		comcat_alert_rules[2] = 3L * hour;
		comcat_alert_rules[3] = 2L;
		ComcatOAFAccessor.set_comcat_health_monitor (new SimpleHealthAlerter (comcat_alert_rules));

		// PDL

		long[] pdl_alert_rules = new long[4];
		pdl_alert_rules[0] = 1L * hour;
		pdl_alert_rules[1] = 6L;
		pdl_alert_rules[2] = 3L * hour;
		pdl_alert_rules[3] = 2L;
		PDLSender.set_pdl_health_monitor (new SimpleHealthAlerter (pdl_alert_rules));

		// Forecast

		long[] forecast_alert_rules = new long[4];
		forecast_alert_rules[0] = 1L * hour;
		forecast_alert_rules[1] = 6L;
		forecast_alert_rules[2] = 3L * hour;
		forecast_alert_rules[3] = 2L;
		forecast_health_monitor = new SimpleHealthAlerter (forecast_alert_rules);

		// Poll

		long[] poll_alert_rules = new long[4];
		poll_alert_rules[0] = 1L * hour;
		poll_alert_rules[1] = 6L;
		poll_alert_rules[2] = 3L * hour;
		poll_alert_rules[3] = 2L;
		poll_health_monitor = new SimpleHealthAlerter (poll_alert_rules);

		return;
	}




	// Remove all health monitors.

	private void remove_health_monitors () {

		// Comcat

		ComcatOAFAccessor.set_comcat_health_monitor (null);

		// PDL

		PDLSender.set_pdl_health_monitor (null);

		// Forecast

		forecast_health_monitor = null;

		// Poll

		poll_health_monitor = null;

		return;
	}




	// Check health monitors, and get the status flags.
	// Parameters:
	//  time_now = Time at which to check the health monitors, cannot be zero.
	//  is_primary = True if this machine is primary for sending to PDL.
	// Returns:
	//  HS_ACTIVE set.
	//  HS_ALERT set if any health monitor is alerting.
	//  Flags for individual alert sources are set.

	private long check_health_monitors (long time_now, boolean is_primary) {
		long status = HS_ACTIVE;

		// Comcat

		HealthMonitor comcat_health_monitor = ComcatOAFAccessor.get_comcat_health_monitor ();
		if (comcat_health_monitor != null) {
			if (comcat_health_monitor.check_alert (time_now)) {
				status |= (HS_COMCAT | HS_ALERT);
			}
		}

		// PDL

		HealthMonitor pdl_health_monitor = PDLSender.get_pdl_health_monitor ();
		if (pdl_health_monitor != null) {
			if (is_primary) {
				if (pdl_health_monitor.check_alert (time_now)) {
					status |= (HS_PDL | HS_ALERT);
				}
			} else {
				pdl_health_monitor.reset_monitor();
			}
		}

		// Forecast

		if (forecast_health_monitor != null) {
			if (forecast_health_monitor.check_alert (time_now)) {
				status |= (HS_FORECAST | HS_ALERT);
			}
		}

		// Poll

		if (poll_health_monitor != null) {
			if (poll_health_monitor.check_alert (time_now)) {
				status |= (HS_POLL | HS_ALERT);
			}
		}

		return status;
	}




	// Reset all health monitors.

	private void reset_health_monitors () {

		// Comcat

		HealthMonitor comcat_health_monitor = ComcatOAFAccessor.get_comcat_health_monitor ();
		if (comcat_health_monitor != null) {
			comcat_health_monitor.reset_monitor();
		}

		// PDL

		HealthMonitor pdl_health_monitor = PDLSender.get_pdl_health_monitor ();
		if (pdl_health_monitor != null) {
			pdl_health_monitor.reset_monitor();
		}

		// Forecast

		if (forecast_health_monitor != null) {
			forecast_health_monitor.reset_monitor();
		}

		// Poll

		if (poll_health_monitor != null) {
			poll_health_monitor.reset_monitor();
		}

		return;
	}




	//----- Service functions -----


	// True if health status monitoring is enabled.

	private boolean f_heath_status_enabled;


	// Get the current health status.
	// Parameters:
	//  time_now = Time at which to check the health monitors, cannot be zero.
	// This function calls PDL support to check if this machine is primary for sending to PDL.

	public long get_health_status (long time_now) {
		long hs = HS_NOT_AVAILABLE;

		// If enabled, check the health monitors

		if (f_heath_status_enabled) {
			hs = check_health_monitors (time_now, sg.pdl_sup.is_pdl_primary());
		}

		return hs;
	}


	// Get the current health status.
	// Parameters:
	//  time_now = Time at which to check the health monitors, cannot be zero.
	//  is_primary = True if this machine is primary for sending to PDL.

	public long get_health_status (long time_now, boolean is_primary) {
		long hs = HS_NOT_AVAILABLE;

		// If enabled, check the health monitors

		if (f_heath_status_enabled) {
			hs = check_health_monitors (time_now, is_primary);
		}

		return hs;
	}


	// Enable health status monitoring.
	// Performs no operation if health status monitoring is already enabled.

	public void enable_health_status () {
		if (!( f_heath_status_enabled )) {
			setup_health_monitors();
			f_heath_status_enabled = true;
		}
		return;
	}


	// Disable health status monitoring.
	// Performs no operation if health status monitoring is already disabled.

	public void disable_health_status () {
		if (f_heath_status_enabled) {
			f_heath_status_enabled = false;
			remove_health_monitors();
		}
		return;
	}


	// Reset health status monitoring.
	// Performs no operation if health status monitoring is disabled.

	public void reset_health_status () {
		if (f_heath_status_enabled) {
			reset_health_monitors();
		}
		return;
	}


	// Reload health status monitoring parameters.
	// Performs no operation if health status monitoring is disabled.

	public void reload_health_status () {
		if (f_heath_status_enabled) {
			setup_health_monitors();
		}
		return;
	}




	//----- Construction -----


	// Default constructor.

	public HealthSupport () {

		forecast_health_monitor = null;
		poll_health_monitor = null;

		f_heath_status_enabled = false;
	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		forecast_health_monitor = null;
		poll_health_monitor = null;

		f_heath_status_enabled = false;

		return;
	}




	//----- Testing -----




	// Subroutine to send multiple failure reports to a health monitor.

	protected static void test_multi_failure (HealthMonitor monitor, int count, long time_now) {
		for (int n = 0; n < count; ++n) {
			monitor.report_failure (time_now);
		}
		return;
	}




	// Subroutine to display health status as hex, short string, long string, and user alert.

	protected static String test_show_status (long hs) {
		StringBuilder sb = new StringBuilder();

		sb.append (String.format ("0x%X", hs));
		sb.append ("\n");
		sb.append ("hs_good_status = " + hs_good_status (hs));
		sb.append ("\n");
		sb.append (hs_short_summary (hs));
		sb.append ("\n");
		sb.append (hs_long_summary (hs));
		sb.append ("\n");
		sb.append (hs_user_alert (hs));
		sb.append ("\n");

		return sb.toString();
	}




	// Subroutine to display health status as hex, short string, long string, and user alert.

	protected static String test_show_status (HealthSupport hsup, long time_now, boolean is_primary) {
		long hs = hsup.get_health_status (time_now, is_primary);
		return test_show_status (hs);
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("HealthSupport : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Test various health status functions of HealthSupport.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("HealthSupport : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing health status functions for HealthSupport");

				// Time now and one day ago

				long time_now = System.currentTimeMillis();
				long day_ago = time_now - 86400000L;

				// Number of failures to use to trigger alert

				int fail_count = 100;

				// Make a health support object

				HealthSupport hsup = new HealthSupport();

				// Initial state, not enabled

				System.out.println ();
				System.out.println ("Initial state");
				System.out.println (test_show_status (hsup, time_now, true));

				// Enable

				hsup.enable_health_status();

				System.out.println ("Enable");
				System.out.println (test_show_status (hsup, time_now, true));

				// Trigger a failure on each health monitor

				test_multi_failure (ComcatOAFAccessor.get_comcat_health_monitor(), fail_count, day_ago);

				System.out.println ("Comcat failure");
				System.out.println (test_show_status (hsup, time_now, true));

				test_multi_failure (PDLSender.get_pdl_health_monitor(), fail_count, day_ago);

				System.out.println ("PDL failure");
				System.out.println (test_show_status (hsup, time_now, true));

				test_multi_failure (hsup.forecast_health_monitor, fail_count, day_ago);

				System.out.println ("Forecast failure");
				System.out.println (test_show_status (hsup, time_now, true));

				test_multi_failure (hsup.poll_health_monitor, fail_count, day_ago);

				System.out.println ("Poll failure");
				System.out.println (test_show_status (hsup, time_now, true));

				// Show for secondary PDL

				System.out.println ("PDL secondary");
				System.out.println (test_show_status (hsup, time_now, false));

				// Disable

				hsup.disable_health_status();

				System.out.println ("Disable");
				System.out.println (test_show_status (hsup, time_now, true));

				// Invalid

				System.out.println ("Invalid - extra");
				System.out.println (test_show_status (HS_VALID_BITS));

				System.out.println ("Invalid - alert only");
				System.out.println (test_show_status (HS_ALERT));

				System.out.println ("Invalid - Comcat only");
				System.out.println (test_show_status (HS_COMCAT));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("HealthSupport : Unrecognized subcommand : " + args[0]);
		return;

	}

}
