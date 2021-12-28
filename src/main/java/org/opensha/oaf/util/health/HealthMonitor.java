package org.opensha.oaf.util.health;


// Interface that monitors the health of a subsystem.
// Author: Michael Barall 12/27/2020.
//
// An object of this type receives a series of reports, each of which is
// "success" or "failure", and from that assesses the health of a subsystem.
// It may also accumulate statistics.
//
// It is strongly recommended that none of these functions ever throw an exception.
//
// Threading: If a monitor can be used in a subsystem that executes in
// multiple threads, then all functions must be thread-safe.

public interface HealthMonitor {


	// Report a success.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// If time_now is zero or omitted, then System.currentTimeMillis() is used.

	public default void report_success () {
		report_success (0L);
		return;
	}

	public void report_success (long time_now);


	// Report a failure.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// If time_now is zero or omitted, then System.currentTimeMillis() is used.

	public default void report_failure () {
		report_failure (0L);
		return;
	}

	public void report_failure (long time_now);


	// Reset the monitor.

	public void reset_monitor ();


	// Check whether the monitor is in alert state, indicating lack of health.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// Returns true if there is an alert.
	// Note: Not all monitors may provide alerts.

	public default boolean check_alert () {
		return check_alert (0L);
	}

	public boolean check_alert (long time_now);

}
