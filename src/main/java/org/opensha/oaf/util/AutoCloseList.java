package org.opensha.oaf.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;


// Perform a cleanup actions on a list of items automatically at the end of a block.
// Author: Michael Barall 03/14/2023.
//
// This class can be used in a try-with-resources to automatically call close()
// for each item in a list of items.  Items can be added at any time.  The close()
// function is called in reverse order from the order in which items were added.
// Each item is of type T, which must implement AutoCloseable.
//
// An exception occuring during close() is either re-thrown as a RuntimeException,
// or suppressed, according to the f_suppress_except flag.
//
// This object can be used as a List<T>.  It supports size(), get(), and other
// read-only operations, plus the add() function.

public class AutoCloseList<T extends AutoCloseable> extends AbstractList<T> implements AutoCloseable {

	// Flag indicates if we should suppress exceptions during close.

	private boolean f_suppress_except;

	// The list of items to close.

	private ArrayList<T> close_list;

	// Modification count, used to detect list modification during close.

	private int mod_count;

	// Class that provides a read-only view of the list.

	private class ReadOnlyView extends AbstractList<T> {

		// Return the number of items in the list.

		@Override
		public int size () {
			return close_list.size();
		}

		// Get an item from the list.

		@Override
		public T get (int index) {
			return close_list.get (index);
		}
	}

	private ReadOnlyView read_only_view;

	// Make an object with the selected initial value of the suppress flag.

	public AutoCloseList (boolean f_suppress_except) {
		this.f_suppress_except = f_suppress_except;
		this.close_list = new ArrayList<T>();
		this.mod_count = 0;
		this.read_only_view = new ReadOnlyView();
	}

	// Make an object with the initial value of the suppress flag equal to false.

	public AutoCloseList () {
		this.f_suppress_except = false;
		this.close_list = new ArrayList<T>();
		this.mod_count = 0;
		this.read_only_view = new ReadOnlyView();
	}

	// Get the current supporess flag.

	public boolean get_suppress_except () {
		return f_suppress_except;
	}

	// Set the suppress flag.

	public void set_suppress_except (boolean f_suppress_except) {
		this.f_suppress_except = f_suppress_except;
		return;
	}

	// The clear function empties the list, without closing any items.

	@Override
	public void clear () {
		//close_list.clear();
		close_list = new ArrayList<T>();	// ensures no references to items are kept
		++mod_count;
		return;
	}

	// Return the number of items in the list.

	@Override
	public int size () {
		return close_list.size();
	}

	// Get an item from the list.

	@Override
	public T get (int index) {
		return close_list.get (index);
	}

	// Add an item to the list.
	// Null items are permitted, and are ignored during close.
	// Always returns true.

	@Override
	public boolean add (T item) {
		close_list.add (item);
		++mod_count;
		return true;
	}

	// Get a read-only view of the list.

	public List<T> get_read_only_view () {
		return read_only_view;
	}

	// Closing calls close() on all contained items, in reverse order.
	// Implementation note: If items are added while the close is in progress,
	// the new items are closed before this function returns, but the order of
	// closing new versus old items is not guaranteed.

	@Override
	public void close() {

		// Exception to re-throw, or null if none

		Exception my_e = null;

		// Current modification count, and the next index to check

		int current_mod_count = mod_count;
		int index = close_list.size() - 1;

		// Loop while there is more to check ...

		while (index >= 0) {

			// Get the item to close, and null its list entry

			T item = close_list.set (index, null);

			// If we got an item, close it

			if (item != null) {
				try {
					item.close();
				}
				catch (Exception e) {
					if (my_e == null && !f_suppress_except) {
						my_e = e;
					}
				}
			}

			// If list was modified by the close, re-initialize

			if (current_mod_count != mod_count) {
				current_mod_count = mod_count;
				index = close_list.size() - 1;
			}

			// Otherwise, just advance the index

			else {
				--index;
			}
		}

		// Erase the list

		clear();

		// If there is an exeption, throw it

		if (my_e != null) {
			throw new RuntimeException ("AutoCloseList.close: Exception while closing item", my_e);
		}
		return;
	}




	//----- Testing -----




	// A simple auto-closeable class for testing.

	private static class test_ac implements AutoCloseable {

		// Identification string.

		private String id;

		// Flag indicates if we should throw an exception during close.

		private boolean f_except;

		// Constructor.

		public test_ac (String id, boolean f_except) {
			this.id = id;
			this.f_except = f_except;
			System.out.println ("Creating: " + this.id);
		}

		// Display.

		public void display (int index) {
			System.out.println (index + ": Displaying: " + id);
			return;
		}

		// Close.

		@Override
		public void close () throws Exception {
			if (f_except) {
				System.out.println ("Closing and throwing: " + id);
				throw new IllegalStateException ("Throwing: " + id);
			}
			else {
				System.out.println ("Closing: " + id);
			}
		}
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "AutoCloseList");




		// Subcommand : Test #1
		// Command format:
		//  test1  num  f_suppress
		// Add the specified number of test_ac objects in an auto-close list, then display, then close.
		// For index numbers ending in 9, add null instead of an object.
		// For index numbers ending in 8, configure the object to throw an exception on close.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Testing AutoCloseList using test_ac objects");
			int num = testargs.get_int ("num");
			boolean f_suppress = testargs.get_boolean ("f_suppress");
			testargs.end_test();

			try (
				AutoCloseList<test_ac> ac_list = new AutoCloseList<test_ac> (f_suppress);
			) {

				// Create the objects

				System.out.println ();
				System.out.println ("Creating objects:");

				for (int j = 0; j < num; ++j) {
					if (j % 10 == 9) {
						ac_list.add (null);
					}
					else if (j % 10 == 8) {
						ac_list.add (new test_ac ("test_ac # " + j, true));
					}
					else {
						ac_list.add (new test_ac ("test_ac # " + j, false));
					}
				}

				// Display the objects

				int the_size = ac_list.size();

				System.out.println ();
				System.out.println ("size = " + the_size);
				System.out.println ("f_suppress_except = " + ac_list.get_suppress_except());

				System.out.println ();
				System.out.println ("Displaying objects:");

				for (int j = 0; j < the_size; ++j) {
					test_ac item = ac_list.get (j);
					if (item == null) {
						System.out.println (j + ": null");
					}
					else {
						item.display (j);
					}
				}

				// Iterate the objects

				System.out.println ();
				System.out.println ("Iterating objects:");

				int i = 0;
				for (test_ac item : ac_list) {
					if (item == null) {
						System.out.println (i + ": null");
					}
					else {
						item.display (i);
					}
					++i;
				}

				// Use the read-only view

				List<test_ac> ro_view = ac_list.get_read_only_view();

				System.out.println ();
				System.out.println ("Read-only view:");
				System.out.println ();
				System.out.println ("size = " + ro_view.size());

				System.out.println ();

				for (int j = 0; j < the_size; ++j) {
					test_ac item = ro_view.get (j);
					if (item == null) {
						System.out.println (j + ": null");
					}
					else {
						item.display (j);
					}
				}

				// Iterate the read-only view

				System.out.println ();
				System.out.println ("Iterating read-only view:");

				i = 0;
				for (test_ac item : ro_view) {
					if (item == null) {
						System.out.println (i + ": null");
					}
					else {
						item.display (i);
					}
					++i;
				}

				// Close the objects (on exit from the try block)

				System.out.println ();
				System.out.println ("Closing objects:");
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
