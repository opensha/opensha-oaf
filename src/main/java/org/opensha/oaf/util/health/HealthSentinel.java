package org.opensha.oaf.util.health;


// Sentinel to report success or failure of a function call.
// Author: Michael Barall 12/27/2020.
//
// This object is intended to be used in try-with-resources statement.
// It holds a reference to a HealthMonitor.
//
// It starts in "neutral" state, but can be switched to "success" or
// "failure" state (the state can be switched multiple times).  When the
// close method is called at the end of block, if the state is not neutral
// then a report is sent to the HealthMonitor.
//
// Note that try-with-resources makes it unnecessary for the monitored
// function to catch and re-throw all exceptions just to send failure reports.
//
// This object can also hold a time of occurrence to use in the reports.
//
// Many functions return this object to allow fluent calls.
//
// Threading: This is a single-thread object.

public class HealthSentinel implements AutoCloseable {


	// The contained health monitor, can be null.

	private HealthMonitor health_monitor;

	// Get the current health monitor.

	public final HealthMonitor get_health_monitor () {
		return health_monitor;
	}

	// Set the health monitor, can be null for none.

	public final HealthSentinel set_health_monitor (HealthMonitor the_health_monitor) {
		health_monitor = the_health_monitor;
		return this;
	}


	// The current state.

	private int state;

	private static final int STATE_NEUTRAL = 0;		// Neutral state
	private static final int STATE_SUCCESS = 1;		// Success state
	private static final int STATE_FAILURE = 2;		// Failure state

	// Set neutral state.

	public final HealthSentinel set_neutral () {
		state = STATE_NEUTRAL;
		return this;
	}

	// Set success state.

	public final HealthSentinel set_success () {
		state = STATE_SUCCESS;
		return this;
	}

	// Set failure state.

	public final HealthSentinel set_failure () {
		state = STATE_FAILURE;
		return this;
	}


	// The time of occurrence, or 0L if not specified (in which case System.currentTimeMillis() is used).

	private long time_now;

	// Set the time of occurrence, can be 0L if not specified.

	public final HealthSentinel set_time (long the_time_now) {
		time_now = the_time_now;
		return this;
	}


	// Flag is true if the object has been closed.

	private boolean is_closed;


	// Construct a sentinel with no health monitor.

	public HealthSentinel () {
		this (null);
	}

	// Construct a sentinel with the given health monitor, which can be null.

	public HealthSentinel (HealthMonitor the_health_monitor) {
		health_monitor = the_health_monitor;
		state = STATE_NEUTRAL;
		time_now = 0L;
		is_closed = false;
	}


	// Closing sends a report to the health monitor, if any.

	@Override
	public void close() {
		if (!( is_closed )) {
			is_closed = true;
			if (health_monitor != null) {
				switch (state) {
				case STATE_SUCCESS:
					health_monitor.report_success (time_now);
					break;
				case STATE_FAILURE:
					health_monitor.report_failure (time_now);
					break;
				}
			}
		}
		return;
	}

}
