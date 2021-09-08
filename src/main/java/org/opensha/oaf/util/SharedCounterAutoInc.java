package org.opensha.oaf.util;


/**
 * Auto-incrementer for a shared counter.
 * Author: Michael Barall 08/16/2021.
 *
 * This class holds a SharedCounter.
 * The counter is incremented when this is constructed, and decremented
 * when this is closed.
 *
 * This class can be used in a try-with-resources to automatically
 * increment and decrement a counter on entry and exit from a block.
 */
public class SharedCounterAutoInc implements AutoCloseable {

	// The shared counter.

	private SharedCounter counter;

	// True if this object has been closed.

	private boolean is_closed;

	// Count value saved during construction, after the increment.

	private int saved_count;

	// Get the counter.

	public SharedCounter get_counter () {
		return counter;
	}

	// Get the current count (which may change over time).
	// Same value as get_counter().get_count().

	public int get_current_count () {
		return counter.get_count();
	}

	// Get the count value saved during initialization, after the increment,
	// which remains fixed for the life of this object.

	public int get_saved_count () {
		return saved_count;
	}

	// On construction, increment the counter.
	// The argument can be null if no counter is desired.

	public SharedCounterAutoInc (SharedCounter the_counter) {
		counter = the_counter;
		if (counter != null) {
			saved_count = counter.inc_count();
		} else {
			saved_count = -1;
		}
		is_closed = false;
	}

	// Closing decrements the counter.

	@Override
	public void close() {
		if (!( is_closed )) {
			is_closed = true;
			if (counter != null) {
				counter.dec_count();
			}
		}
		return;
	}

}
