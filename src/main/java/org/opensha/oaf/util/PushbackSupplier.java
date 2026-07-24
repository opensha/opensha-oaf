package org.opensha.oaf.util;

import java.util.ArrayDeque;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.opensha.oaf.util.TestArgs;


// Class to add pushback functionality supplier of T.
// Author: Michael Barall.
//
// This object holds an upstream supplier of T.
// Calls to the get() function obtain T from the upstream supplier,
// unless T has been pushed back, in which case the most recently
// pushed back T is returned.

public class PushbackSupplier<T> implements Supplier<T> {

	// The upstream supplier.
	// It is assumed that the upstream supplier returns null only at the end of its stream.

	protected Supplier<T> upstream;

	// This is true if the upstream supplier has returned null.

	protected boolean up_at_end;

	// The stack of pushed back items.
	// Note that a call to peek() or isEmpty() implicitly retrieves and pushes back an item.

	protected ArrayDeque<T> stack;


	// Construct a pushback supplier with the given upstream.

	public PushbackSupplier (Supplier<T> upstream) {
		this.upstream = upstream;
		this.up_at_end = false;
		this.stack = new ArrayDeque<T>();
	}


	// Return this object as a Supplier<T>.

	public final Supplier<T> as_supplier () {
		return this;
	}


	// Internal function to get item from upstream, or null if end of upstream.

	protected final T get_from_upstream () {

		// If already reached end, return null

		if (up_at_end) {
			return null;
		}

		// Otherwise, get the next item from upstream and check for end.

		T t = upstream.get();
		if (t == null) {
			up_at_end = true;
		}
		return t;
	}


	// Get the next item, return null if at end.

	@Override
	public T get () {

		// If nothing stacked, just read from upstream

		if (stack.isEmpty()) {
			return get_from_upstream();
		}

		// Otherwise, get item from stack

		return stack.pop();
	}


	// Return the next item, but do not remove it.

	public T peek () {

		// Try to get it from the stack

		T t = stack.peek();

		// If didn't get it, try the upstream

		if (t == null) {
			t = get_from_upstream();
			if (t != null) {
				stack.push (t);
			}
		}

		return t;
	}


	// Push back an item onto the stack.
	// Although null is permitted, pushing null will create confusion about when the end is reached.

	public void push (T t) {
		stack.push (t);
		return;
	}


	// To complete the set of stack functions, define pop to be the same as get.

	public T pop () {
		return get();
	}


	// Return true if at the end.

	public boolean isEmpty () {

		// If stack is empty, need to check the upstream

		if (stack.isEmpty()) {
			T t = get_from_upstream();
			if (t == null) {
				return true;
			}
			stack.push (t);
		}

		return false;
	}




	//----- Testing -----




	// Given an array of S, return a Supplier<S> for testing purposes.

	public static <S> Supplier<S> test_make_supplier (final S[] array) {
		return new Supplier<S>() {
			private int i = 0;

			@Override
			public S get() {
				return ((i < array.length) ? array[i++] : null);
			}
		};
	}




	// Get the size of the internal stack, for testing purposes.

	public static void test_stack_size (PushbackSupplier<String> pbs) {
		int n = pbs.stack.size();
		System.out.println ("stack_size = " + n);
		return;
	}




	// Exercise the get() function for testing purposes.

	public static void test_get (PushbackSupplier<String> pbs) {
		String s = pbs.get();
		if (s == null) {
			System.out.println ("get() = null" + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		} else {
			System.out.println ("get() = " + s + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		}
		return;
	}




	// Exercise the pop() function for testing purposes.

	public static void test_pop (PushbackSupplier<String> pbs) {
		String s = pbs.pop();
		if (s == null) {
			System.out.println ("pop() = null" + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		} else {
			System.out.println ("pop() = " + s + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		}
		return;
	}




	// Exercise the peek() function for testing purposes.

	public static void test_peek (PushbackSupplier<String> pbs) {
		String s = pbs.peek();
		if (s == null) {
			System.out.println ("peek() = null" + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		} else {
			System.out.println ("peek() = " + s + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		}
		return;
	}




	// Exercise the push() function for testing purposes.

	public static void test_push (PushbackSupplier<String> pbs, String s) {
		pbs.push (s);
		System.out.println ("push(" + s + ")" + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		return;
	}




	// Exercise the isEmpty() function for testing purposes.

	public static void test_isEmpty (PushbackSupplier<String> pbs) {
		boolean b = pbs.isEmpty();
		System.out.println ("isEmpty() = " + b + ", stack_size = " + pbs.stack.size() + ", up_at_end = " + pbs.up_at_end);
		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEValForecastInfo");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Run various tests.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Various tests");
			testargs.end_test();

			// Create a supplier

			String[] my_strings = {"one", "two", "three", "four", "five"};
			Supplier<String> my_upstream = test_make_supplier (my_strings);
			PushbackSupplier<String> pbs = new PushbackSupplier<String>(my_upstream);

			// Run tests

			test_stack_size (pbs);
			test_get (pbs);
			test_pop (pbs);
			test_peek (pbs);
			test_push (pbs, "extra");
			test_peek (pbs);
			test_isEmpty (pbs);
			test_get (pbs);
			test_peek (pbs);
			test_isEmpty (pbs);
			test_get (pbs);
			test_isEmpty (pbs);
			test_get (pbs);
			test_get (pbs);
			test_peek (pbs);
			test_isEmpty (pbs);
			test_get (pbs);
			test_push (pbs, "more");
			test_peek (pbs);
			test_isEmpty (pbs);
			test_get (pbs);
			test_peek (pbs);
			test_isEmpty (pbs);
			test_get (pbs);

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
