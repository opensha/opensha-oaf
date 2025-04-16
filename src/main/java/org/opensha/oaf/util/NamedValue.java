package org.opensha.oaf.util;


// Class to hold a value of type T, and a String that is the name of the value.
// Author: Michael Barall.
//
// This is an immutable object.

public class NamedValue<T> {

	//----- Contents -----

	// Name.

	private String name;

	// Value.

	private T value;




	//----- Construction -----

	// Construct an object with given name and value.
	// Note: Retains value.

	public NamedValue (String name, T value) {
		this.name = name;
		this.value = value;
	}


	// Display as a string.

	@Override
	public String toString() {
		return "(" + name + ", " + value.toString() +  ")";
	}




	//----- Getters -----

	// Get name.

	public final String get_name () {
		return name;
	}

	// Get value.

	public final T get_value () {
		return value;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "NamedValue");




		// Subcommand : Test #1
		// Command format:
		//  test1  the_name  the_value
		// Construct an object of type NamedValue<String> and display it.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing NamedValue<String>");
			String the_name = testargs.get_string ("the_name");
			String the_value = testargs.get_string ("the_value");
			testargs.end_test();

			// Create the named value

			NamedValue<String> named_value = new NamedValue<String>(the_name, the_value);

			// Display

			System.out.println ();
			System.out.println ("name = " + named_value.get_name());
			System.out.println ("value = " + named_value.get_value());
			System.out.println ("pair = " + named_value.toString());

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
