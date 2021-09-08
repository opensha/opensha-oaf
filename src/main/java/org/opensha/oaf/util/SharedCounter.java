package org.opensha.oaf.util;


/**
 * A counter that can be shared among multiple objects and threads.
 * Author: Michael Barall 08/16/2021.
 *
 * All functions in this class are synchronized and hence atomic.
 */
public class SharedCounter {

	// The counter value.

	private int count;

	// Create  counter, initialized to zero.

	public SharedCounter () {
		count = 0;
	}

	// Create a counter with the given initial value.

	public SharedCounter (int initial_count) {
		count = initial_count;
	}

	// Get the current count.

	public final synchronized int get_count () {
		return count;
	}

	// Set the count, and return the new value.

	public final synchronized int set_count (int x) {
		count = x;
		return count;
	}

	// Increment the count, and return the new value.

	public final synchronized int inc_count () {
		++count;
		return count;
	}

	// Decrement the count, and return the new value.

	public final synchronized int dec_count () {
		--count;
		return count;
	}

	// Add a value to the count, and return the new value.

	public final synchronized int add_count (int x) {
		count += x;
		return count;
	}

	// Subtract a value from the count, and return the new value.

	public final synchronized int sub_count (int x) {
		count -= x;
		return count;
	}

}
