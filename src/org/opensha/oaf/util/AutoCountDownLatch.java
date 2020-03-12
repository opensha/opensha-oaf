package org.opensha.oaf.util;

import java.util.concurrent.CountDownLatch;


/**
 * Advance a CountDownLatch automatically at the end of a block.
 * Author: Michael Barall 01/26/2020.
 *
 * This class can be used in a try-with-resources to automatically advance
 * the counter in the selected CountDownLatch when the object is closed
 * at the end of the block.
 */
public class AutoCountDownLatch implements AutoCloseable {

	// The selected CountDownLatch, or null if none currently selected.

	private CountDownLatch count_down_latch;

	// The default constructor makes an object with no CountDownLatch selected.

	public AutoCountDownLatch () {
		count_down_latch = null;
	}

	// Make an object with the selected CountDownLatch.
	// Parameters:
	//  the_count_down_latch = CountDownLatch to select, or null if none.

	public AutoCountDownLatch (CountDownLatch the_count_down_latch) {
		count_down_latch = the_count_down_latch;
	}

	// Get the selected CountDownLatch.

	public CountDownLatch get () {
		return count_down_latch;
	}

	// Change the selected CountDownLatch.
	// Parameters:
	//  the_count_down_latch = CountDownLatch to select, or null if none.
	// Returns the previously selected CountDownLatch, or null if none.

	public CountDownLatch set (CountDownLatch the_count_down_latch) {
		CountDownLatch result = count_down_latch;
		count_down_latch = the_count_down_latch;
		return result;
	}

	// Release the selected CountDownLatch.
	// Returns the previously selected CountDownLatch, or null if none.
	// Note: release() performs the same function as set(null).

	public CountDownLatch release () {
		CountDownLatch result = count_down_latch;
		count_down_latch = null;
		return result;
	}

	// Closing advances the selected CountDownLatch, if any.

	@Override
	public void close() {
		CountDownLatch latch = count_down_latch;
		if (latch != null) {
			count_down_latch = null;
			latch.countDown();
		}
		return;
	}

}
