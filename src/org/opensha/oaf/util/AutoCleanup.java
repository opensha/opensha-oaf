package org.opensha.oaf.util;


/**
 * Perform a cleanup action automatically at the end of a block.
 * Author: Michael Barall 01/30/2020.
 *
 * This class can be used in a try-with-resources to automatically call
 * a cleaup() function when the object is closed at the end of the block.
 *
 * It is particularly appropriate for defining cleanup operations in
 * an anonymous class.
 *
 * The close() and cleanup() functions throw no unchecked exceptions,
 * removing the need to write exception handlers for the block.
 *
 * The class can be enabled or disabled, to control whether or not
 * the cleanup() function is called.  The class is disabled when close()
 * is called, thereby making close() idempotent.
 */
public abstract class AutoCleanup implements AutoCloseable {

	// Flag indicates if we are currently enabled.

	private boolean f_cleanup_enabled;

	// Make an object with the selected initial value of the enabled flag.
	// Note that the anonymous class syntax allows passing arguments to a constructor.

	public AutoCleanup (boolean the_f_cleanup_enabled) {
		f_cleanup_enabled = the_f_cleanup_enabled;
	}

	// Make an object with the initial value of the enabled flag equal to true.

	public AutoCleanup () {
		f_cleanup_enabled = true;
	}

	// Get the current enabled flag.

	public boolean get_cleanup_enabled () {
		return f_cleanup_enabled;
	}

	// Set the enabled flag

	public void set_cleanup_enabled (boolean the_f_cleanup_enabled) {
		f_cleanup_enabled = the_f_cleanup_enabled;
		return;
	}

	// The cleanup function, to be overridden by the subclass.

	protected abstract void cleanup ();

	// Closing invokes the cleanup function, if we are enabled.

	@Override
	public void close() {
		if (f_cleanup_enabled) {
			f_cleanup_enabled = false;
			cleanup();
		}
		return;
	}

}
