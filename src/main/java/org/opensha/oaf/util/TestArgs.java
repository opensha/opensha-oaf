package org.opensha.oaf.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


 // Class to assist in parsing command-line arguments for test code.
 // Author: Michael Barall 01/23/2023.

public class TestArgs {

	// The class name.

	private String cl_name;

	private boolean has_cl_name () {
		if (cl_name == null || cl_name.isEmpty()) {
			return false;
		}
		return true;
	}

	// The test name.

	private String test_name;

	private boolean has_test_name () {
		if (test_name == null || test_name.isEmpty()) {
			return false;
		}
		return true;
	}

	// Flag to enable echo.

	private boolean f_echo;

	public TestArgs set_echo (boolean f_echo) {
		this.f_echo = f_echo;
		return this;
	}

	// The arguments being parsed.

	private String[] my_args;

	// The current index into the arguments.

	private int arg_index;




	// Constructor supplies the argument list.

	public TestArgs (String[] my_args) {
		this.cl_name = null;
		this.test_name = null;
		this.f_echo = true;
		this.my_args = my_args;
		this.arg_index = 0;
	}




	// Constructor supplies the argument list and class name.
	// The class name can be empty or null to mean unspecified.

	public TestArgs (String[] my_args, String cl_name) {
		this.cl_name = cl_name;
		this.test_name = null;
		this.f_echo = true;
		this.my_args = my_args;
		this.arg_index = 0;
	}




	// Pattern used to test if an argument needs to be quoted.
	// This recognizes a non-empty string of printable non-blank ASCII characters other than quote.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern arg_quote_pattern = Pattern.compile ("[\\x21\\x23-\\x7E\\xA1-\\xFF]+");




	// Test if the given string is printable without being quoted and escaped.
	// Returns true if quoting and escaping is not needed.
	// The string s must be non-null.

	public static boolean arg_is_printable (String s) {
		return arg_quote_pattern.matcher(s).matches();
	}




	// Append the argument to the string builder.
	// It is enclosed in quotes and escaped if necessary.
	// The string s can be null, in which case the result is "<null>".

	public static void print_arg (StringBuilder sb, String s) {

		// Null argument

		if (s == null) {
			sb.append ("<null>");
		}

		// Printable argument

		else if (arg_quote_pattern.matcher(s).matches()) {
			sb.append (s);
		}

		// Enclose in quotes and produce escapes

		else {
			sb.append ('"');

			int len = s.length();
			for (int i = 0; i < len; ++i) {

				// Switch on character

				char ch = s.charAt(i);
				switch(ch) {

				case '"':
					sb.append ("\\\"");
					break;

				case '\\':
					sb.append ("\\\\");
					break;

				case '\b':
					sb.append ("\\b");
					break;

				case '\f':
					sb.append ("\\f");
					break;

				case '\n':
					sb.append ("\\n");
					break;

				case '\r':
					sb.append ("\\r");
					break;

				case '\t':
					sb.append ("\\t");
					break;

				default:

					// Printable ASCII characters, including space

					if ((ch >= '\u0020' && ch <= '\u007E') || (ch >= '\u00A1' && ch <= '\u00FF')) {
						sb.append (ch);
					}

					// Otherwise, write a Unicode escape

					else {
						int k = ch;
						sb.append (String.format ("\\u%04X", k));
					}
					break;
				}
			}

			sb.append ('"');
		}

		return;
	}




	// Get a string containing the command line.
	// Arguments a printed on a single line, separated by blanks, each quoted and escaped if needed.

	public static String get_cmdline (String[] args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; ++i) {
			if (i > 0) {
				sb.append (" ");
			}
			print_arg (sb, args[i]);
		}
		return sb.toString();
	}




	// Get a string containing the command line for our arguments.
	// Arguments a printed on a single line, separated by blanks, each quoted and escaped if needed.

	public final String get_cmdline () {
		return get_cmdline (my_args);
	}




	// Print an error message and throw an exception.
	// This is for an error not associated with any particular argument.

	private void signal_error (String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append (msg);
		String sep = ": ";
		if (has_cl_name()) {
			sb.append (sep);
			sb.append ("class = ");
			sb.append (cl_name);
			sep = ", ";
		}
		if (has_test_name()) {
			sb.append (sep);
			sb.append ("test = ");
			sb.append (test_name);
		}
		String err_msg = sb.toString();
		System.out.println (err_msg);
		throw new IllegalArgumentException (err_msg);
	}




	// Print an error message and throw an exception.
	// This is for an error associated with the current argument.

	private void signal_error (String msg, String arg_name) {
		StringBuilder sb = new StringBuilder();
		sb.append (msg);
		String sep = ": ";
		if (has_cl_name()) {
			sb.append (sep);
			sb.append ("class = ");
			sb.append (cl_name);
			sep = ", ";
		}
		if (has_test_name()) {
			sb.append (sep);
			sb.append ("test = ");
			sb.append (test_name);
			sep = ", ";
		}
		if (!( arg_name == null || arg_name.isEmpty() )) {
			sb.append (sep);
			sb.append ("name = ");
			sb.append (arg_name);
			sep = ", ";
		}

		sb.append (sep);
		sb.append ("index = ");
		sb.append (arg_index);
		sep = ", ";

		if (my_args != null && arg_index < my_args.length) {
			sb.append (sep);
			sb.append ("arg = ");
			print_arg (sb, my_args[arg_index]);
		}

		String err_msg = sb.toString();
		System.out.println (err_msg);
		throw new IllegalArgumentException (err_msg);
	}




	// Print an error message and throw an exception.
	// This is for an error associated with the current argument, and also links an exception

	private void signal_error (String msg, String arg_name, Exception e) {
		StringBuilder sb = new StringBuilder();
		sb.append (msg);
		String sep = ": ";
		if (has_cl_name()) {
			sb.append (sep);
			sb.append ("class = ");
			sb.append (cl_name);
			sep = ", ";
		}
		if (has_test_name()) {
			sb.append (sep);
			sb.append ("test = ");
			sb.append (test_name);
			sep = ", ";
		}
		if (!( arg_name == null || arg_name.isEmpty() )) {
			sb.append (sep);
			sb.append ("name = ");
			sb.append (arg_name);
			sep = ", ";
		}

		sb.append (sep);
		sb.append ("index = ");
		sb.append (arg_index);
		sep = ", ";

		if (my_args != null && arg_index < my_args.length) {
			sb.append (sep);
			sb.append ("arg = ");
			print_arg (sb, my_args[arg_index]);
		}

		String err_msg = sb.toString();
		System.out.println (err_msg);
		throw new IllegalArgumentException (err_msg, e);
	}




	// Return true if there are more arguments.

	public final boolean has_more () {
		return arg_index < my_args.length;
	}




	// Return the number of arguments remaining.

	public final int remaining () {
		return my_args.length - arg_index;
	}




	// Throw an exception if there are no more arguments.

	public final void require_more () {
		if (!( has_more() )) {
			signal_error ("Too few arguments");
		}
		return;
	}




	// Throw an exception if there are no more arguments, and give the name of the expected argument..

	public final void require_more (String arg_name) {
		if (!( has_more() )) {
			signal_error ("Missing argument", arg_name);
		}
		return;
	}




	// Check if the supplied test is being invoked, return true if so.

	public final boolean is_test (String... test_names) {
		if (!( has_more() )) {
			signal_error ("Missing subcommand");
		}
		for (String s : test_names) {
			if (my_args[arg_index].equalsIgnoreCase (s)) {
				this.test_name = s;
				++arg_index;
				return true;
			}
		}
		return false;
	}




	// Check if all arguments have been consumed, throw exception if not.

	public final void end_test () {
		if (has_more()) {
			signal_error ("Too many arguments");
		}
		return;
	}




	// Write a message reporting an unrecognized test.

	public final void unrecognized_test () {
		if (!( has_more() )) {
			signal_error ("Missing subcommand");
		}
		StringBuilder sb = new StringBuilder();
		if (has_cl_name()) {
			sb.append (cl_name);
			sb.append (" : ");
		}
		sb.append ("Unrecognized subcommand : ");
		sb.append (my_args[arg_index]);
		System.out.println (sb.toString());
		return;
	}




	// Get a string argument.

	public final String get_string (String arg_name) {
		require_more (arg_name);
		String x = my_args[arg_index];
		if (x == null) {
			signal_error ("String argument is null", arg_name);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + x);
		}
		return x;
	}




	// Get an optional string argument, return the optional value if not in argument list.

	public final String get_string_opt (String arg_name, String optval) {
		if (has_more()) {
			return get_string (arg_name);
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + optval);
		}
		return optval;
	}




	// Get a string argument which can be omitted.
	// Return optval if not in argument list, or if the argument is null, empty, or equal to omitarg.

	public final String get_string_omit (String arg_name, String optval, String omitarg) {
		String x = optval;
		if (has_more()) {
			String s = my_args[arg_index];
			if (!( s == null || s.equals ("") || s.equals (omitarg) )) {
				x = s;
			}
			++arg_index;
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + ((x == null) ? "<null>" : x));
		}
		return x;
	}




	// Get a double argument.

	public final double get_double (String arg_name) {
		require_more (arg_name);
		double x = 0.0;
		try {
			x = Double.parseDouble (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid double argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + x);
		}
		return x;
	}




	// Get an optional double argument, return the optional value if not in argument list.

	public final double get_double_opt (String arg_name, double optval) {
		if (has_more()) {
			return get_double (arg_name);
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + optval);
		}
		return optval;
	}




	// Get an integer argument.

	public final int get_int (String arg_name) {
		require_more (arg_name);
		int x = 0;
		try {
			x = Integer.parseInt (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid integer argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + x);
		}
		return x;
	}




	// Get an optional integer argument, return the optional value if not in argument list.

	public final int get_int_opt (String arg_name, int optval) {
		if (has_more()) {
			return get_int (arg_name);
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + optval);
		}
		return optval;
	}




	// Get a long argument.

	public final long get_long (String arg_name) {
		require_more (arg_name);
		long x = 0;
		try {
			x = Long.parseLong (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid long argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + x);
		}
		return x;
	}




	// Get an optional long argument, return the optional value if not in argument list.

	public final long get_long_opt (String arg_name, long optval) {
		if (has_more()) {
			return get_long (arg_name);
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + optval);
		}
		return optval;
	}




	// Get a boolean argument.

	public final boolean get_boolean (String arg_name) {
		require_more (arg_name);
		boolean x = false;
		try {
			x = Boolean.parseBoolean (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid boolean argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + x);
		}
		return x;
	}




	// Get an optional boolean argument, return the optional value if not in argument list.

	public final boolean get_boolean_opt (String arg_name, boolean optval) {
		if (has_more()) {
			return get_boolean (arg_name);
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + optval);
		}
		return optval;
	}




	// Get a string array.
	// Parameters:
	//  arg_name = Name of the array argument.
	//  length = Length of array, a negative value counts from the end (-1 = all remaining, etc.).
	//  min_length = Minimum required length of array.

	public final String[] get_string_array (String arg_name, int length, int min_length) {
		int rem_length = remaining();
		int len = ((length >= 0) ? (length) : (rem_length + 1 + length));
		if (len < Math.max (min_length, 0)) {
			signal_error ("Array length (" + len + ") is less than minimum required length (" + Math.max (min_length, 0) + ")", arg_name);
		}
		if (len > rem_length) {
			signal_error ("Array length (" + len + ") is greater than available length (" + rem_length + ")", arg_name);
		}
		String[] x = new String[len];
		for (int i = 0; i < len; ++i) {
			x[i] = my_args[arg_index];
			if (x[i] == null) {
				signal_error ("String array element is null", arg_name);
			}
			++arg_index;
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + ":");
			for (int i = 0; i < len; ++i) {
				System.out.println ("  " + i + ": " + x[i]);
			}
		}
		return x;
	}




	// Get a double array.
	// Parameters:
	//  arg_name = Name of the array argument.
	//  length = Length of array, a negative value counts from the end (-1 = all remaining, etc.).
	//  min_length = Minimum required length of array.

	public final double[] get_double_array (String arg_name, int length, int min_length) {
		int rem_length = remaining();
		int len = ((length >= 0) ? (length) : (rem_length + 1 + length));
		if (len < Math.max (min_length, 0)) {
			signal_error ("Array length (" + len + ") is less than minimum required length (" + Math.max (min_length, 0) + ")", arg_name);
		}
		if (len > rem_length) {
			signal_error ("Array length (" + len + ") is greater than available length (" + rem_length + ")", arg_name);
		}
		double[] x = new double[len];
		for (int i = 0; i < len; ++i) {
			try {
				x[i] = Double.parseDouble (my_args[arg_index]);
			}
			catch (Exception e) {
				signal_error ("Invalid double array element", arg_name, e);
			}
			++arg_index;
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + ":");
			for (int i = 0; i < len; ++i) {
				System.out.println ("  " + i + ": " + x[i]);
			}
		}
		return x;
	}




	// Get a double array contining tuples.
	// Parameters:
	//  arg_name = Name of the array argument.
	//  length = Length of array, a negative value counts from the end (-1 = all remaining, etc.).
	//  min_length = Minimum required length of array.
	//  n_tuple = Number of elements in each tuple.
	//  tuple_names = Names of each element of the tuple, can be omitted, null, or blank.

	public final double[] get_double_tuple_array (String arg_name, int length, int min_length, int n_tuple, String... tuple_names) {
		int rem_length = remaining();
		int len = ((length >= 0) ? (length) : (rem_length + 1 + length));
		if (len < Math.max (min_length, 0)) {
			signal_error ("Array length (" + len + ") is less than minimum required length (" + Math.max (min_length, 0) + ")", arg_name);
		}
		if (len > rem_length) {
			signal_error ("Array length (" + len + ") is greater than available length (" + rem_length + ")", arg_name);
		}
		if (len % n_tuple != 0) {
			signal_error ("Array length (" + len + ") is not divisible by tuple length (" + n_tuple + ")", arg_name);
		}
		double[] x = new double[len];
		for (int i = 0; i < len; ++i) {
			try {
				x[i] = Double.parseDouble (my_args[arg_index]);
			}
			catch (Exception e) {
				signal_error ("Invalid double array element", arg_name, e);
			}
			++arg_index;
		}
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + ":");
			for (int i = 0; i < len/n_tuple; ++i) {
				StringBuilder sb = new StringBuilder();
				sb.append ("  ");
				sb.append (i);
				sb.append (": ");
				for (int j = 0; j < n_tuple; ++j) {
					if (j > 0) {
						sb.append (", ");
					}
					if (j < tuple_names.length && tuple_names[j] != null && !(tuple_names[j].isEmpty())) {
						sb.append (tuple_names[j]);
						sb.append (" = ");
					}
					sb.append (x[i*n_tuple + j]);
				}
				System.out.println (sb.toString());
			}
		}
		return x;
	}




	// Get a time argument, as a long in milliseconds since the epochs.
	// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

	public final long get_time (String arg_name) {
		require_more (arg_name);
		long x = 0;
		try {
			x = SimpleUtils.string_to_time (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid time argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + SimpleUtils.time_to_string(x));
		}
		return x;
	}




	// Get a duration argument, as a long in milliseconds.
	// Durations are in java.time.Duration format, for example P3DT11H45M04S or P100D or PT30S.

	public final long get_duration (String arg_name) {
		require_more (arg_name);
		long x = 0;
		try {
			x = SimpleUtils.string_to_duration (my_args[arg_index]);
		}
		catch (Exception e) {
			signal_error ("Invalid duration argument", arg_name, e);
		}
		++arg_index;
		if (f_echo && arg_name != null && !(arg_name.isEmpty())) {
			System.out.println (arg_name + " = " + SimpleUtils.duration_to_string_2(x));
		}
		return x;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1  str  d  i  b  l
		// Test the obtaining of string, double, integer, boolean, and long arguments.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Testing scalar arguments");
			String str = testargs.get_string ("str");
			double d = testargs.get_double ("d");
			int i = testargs.get_int ("i");
			boolean b = testargs.get_boolean ("b");
			long l = testargs.get_long ("l");
			testargs.end_test();

			// Display the command line

			System.out.println ();
			System.out.println ("Command line:");
			System.out.println (testargs.get_cmdline());

			// Display the arguments we got

			System.out.println ();
			System.out.println ("Arguments obtained:");
			System.out.println ("str = " + str);
			System.out.println ("d = " + d);
			System.out.println ("i = " + i);
			System.out.println ("b = " + b);
			System.out.println ("l = " + l);

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  [str  [d  [i  [b  [l]]]]]
		// Test the obtaining of optional string, double, integer, boolean, and long arguments.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Testing optional scalar arguments");
			String str = testargs.get_string_opt ("str", "default_string");
			double d = testargs.get_double_opt ("d", -3.14159);
			int i = testargs.get_int_opt ("i", -271828);
			boolean b = testargs.get_boolean_opt ("b", false);
			long l = testargs.get_long_opt ("l", -987654321987654L);
			testargs.end_test();

			// Display the command line

			System.out.println ();
			System.out.println ("Command line:");
			System.out.println (testargs.get_cmdline());

			// Display the arguments we got

			System.out.println ();
			System.out.println ("Arguments obtained:");
			System.out.println ("str = " + str);
			System.out.println ("d = " + d);
			System.out.println ("i = " + i);
			System.out.println ("b = " + b);
			System.out.println ("l = " + l);

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  len1  minlen1  arr1[...]  len2  minlen2  arr2[...]
		// Test the obtaining of double arrays.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Testing double arrays");
			int len1 = testargs.get_int ("len1");
			int minlen1 = testargs.get_int ("minlen1");
			double[] arr1 = testargs.get_double_array ("arr1", len1, minlen1);
			int len2 = testargs.get_int ("len2");
			int minlen2 = testargs.get_int ("minlen2");
			double[] arr2 = testargs.get_double_array ("arr2", len2, minlen2);
			testargs.end_test();

			// Display the command line

			System.out.println ();
			System.out.println ("Command line:");
			System.out.println (testargs.get_cmdline());

			// Display the arguments we got

			System.out.println ();
			System.out.println ("arr1 [len = " + arr1.length + "]:");
			for (int i = 0; i < arr1.length; ++i) {
				System.out.println ("  " + i + ": " + arr1[i]);
			}
			System.out.println ("arr2 [len = " + arr2.length + "]:");
			for (int i = 0; i < arr2.length; ++i) {
				System.out.println ("  " + i + ": " + arr2[i]);
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  len1  minlen1  arr1[...]  len2  minlen2  arr2[...]
		// Test the obtaining of string arrays.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Testing string arrays");
			int len1 = testargs.get_int ("len1");
			int minlen1 = testargs.get_int ("minlen1");
			String[] arr1 = testargs.get_string_array ("arr1", len1, minlen1);
			int len2 = testargs.get_int ("len2");
			int minlen2 = testargs.get_int ("minlen2");
			String[] arr2 = testargs.get_string_array ("arr2", len2, minlen2);
			testargs.end_test();

			// Display the command line

			System.out.println ();
			System.out.println ("Command line:");
			System.out.println (testargs.get_cmdline());

			// Display the arguments we got

			System.out.println ();
			System.out.println ("arr1 [len = " + arr1.length + "]:");
			for (int i = 0; i < arr1.length; ++i) {
				System.out.println ("  " + i + ": " + arr1[i]);
			}
			System.out.println ("arr2 [len = " + arr2.length + "]:");
			for (int i = 0; i < arr2.length; ++i) {
				System.out.println ("  " + i + ": " + arr2[i]);
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  len1  minlen1  n1  names1[...]  arr1[...]  len2  minlen2  n2  names2[...]  arr2[...]
		// Test the obtaining of double tuple arrays.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Testing double arrays");
			int len1 = testargs.get_int ("len1");
			int minlen1 = testargs.get_int ("minlen1");
			int n1 = testargs.get_int ("n1");
			String[] names1 = testargs.get_string_array ("names1", n1, 0);
			double[] arr1 = testargs.get_double_tuple_array ("arr1", len1, minlen1, n1, names1);
			int len2 = testargs.get_int ("len2");
			int minlen2 = testargs.get_int ("minlen2");
			int n2 = testargs.get_int ("n2");
			String[] names2 = testargs.get_string_array ("names2", n2, 0);
			double[] arr2 = testargs.get_double_tuple_array ("arr2", len2, minlen2, n2, names2);
			testargs.end_test();

			// Display the command line

			System.out.println ();
			System.out.println ("Command line:");
			System.out.println (testargs.get_cmdline());

			// Display the arguments we got

			System.out.println ();
			System.out.println ("arr1 [len = " + arr1.length + "]:");
			for (int i = 0; i < arr1.length; ++i) {
				System.out.println ("  " + i + ": " + arr1[i]);
			}
			System.out.println ("arr2 [len = " + arr2.length + "]:");
			for (int i = 0; i < arr2.length; ++i) {
				System.out.println ("  " + i + ": " + arr2[i]);
			}

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
