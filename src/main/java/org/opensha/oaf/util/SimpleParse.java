package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


// Class to hold some simple functions for parsing, scanning, and formatting.
// Author: Michael Barall.
//
// All functions in this class are static.

public class SimpleParse {




	// Convert a string using JSON escape codes.
	// Parameters:
	//  input = Input string.
	//  f_unicode = Option to allow Unicode >= U+0100 in the output.
	//              false = Output consists only of ASCII characters 0x20 - 0x7E.
	//              true = Output can also contain Unicode characters U+0080 - U+FFFF.
	//  quote_opt = Option for enclosing the output in quotes.
	//              1 = Always enclose in quotes.
	//              0 = Enclose in quotes only if it contains white space or escapes, or is empty.
	//              -1 = Never enclose in quotes.
	// Returns the converted string.
	// Note: Returns the input string if quote_opt <= 0 and the string does not contain
	// white space or any characters requring an escape sequence and is not empty.

	public static String escape_json_string (String input, boolean f_unicode, int quote_opt) {
		StringBuilder sb = new StringBuilder();
		int length = input.length();

		boolean f_escape = (length == 0);

		if (quote_opt >= 0) {
			sb.append("\"");
		}

		for	(int i = 0;	i <	length;	i++) {
			char c = input.charAt(i);

			switch (c) {
				case ' ':
					sb.append(" ");
					f_escape = true;
					break;
				case '"':
					sb.append("\\\"");
					f_escape = true;
					break;
				case '\\':
					sb.append("\\\\");
					f_escape = true;
					break;
				case '\b':
					sb.append("\\b");
					f_escape = true;
					break;
				case '\f':
					sb.append("\\f");
					f_escape = true;
					break;
				case '\n':
					sb.append("\\n");
					f_escape = true;
					break;
				case '\r':
					sb.append("\\r");
					f_escape = true;
					break;
				case '\t':
					sb.append("\\t");
					f_escape = true;
					break;
				default:
					if ((c >= 0x20 && c <= 0x7E) || (f_unicode && c >= 0x80)) {
						sb.append(c);
					} else {
						sb.append(String.format("\\u%04X", (int) c));
						f_escape = true;
					}
					break;
			}
		}

		if (quote_opt >= 0) {
			sb.append("\"");
		}

		if (quote_opt <= 0 && !f_escape) {
			return input;
		}

		return sb.toString();
	}




	// Convert a string that may contain JSON escape sequences.
	// Parameters:
	//  input = Input string (not enclosed in quotes).
	// Returns the unescaped string.

	public static String unescape_json_string (String input) {
		StringBuilder sb = new StringBuilder();
		int	length = input.length();

		for	(int i = 0;	i <	length;	i++) {
			char c = input.charAt(i);

			if (c == '\\' && i + 1 < length) {
				char next =	input.charAt(i + 1);
				switch (next) {
					case '"': sb.append('"'); i++; break;
					case '\\': sb.append('\\');	i++; break;
					case '/': sb.append('/'); i++; break;
					case 'b': sb.append('\b'); i++;	break;
					case 'f': sb.append('\f'); i++;	break;
					case 'n': sb.append('\n'); i++;	break;
					case 'r': sb.append('\r'); i++;	break;
					case 't': sb.append('\t'); i++;	break;
					case 'u':
						if (i +	5 <	length)	{
							String hex = input.substring(i + 2,	i +	6);
							try	{
								int	codePoint =	Integer.parseInt(hex, 16);
								sb.append((char) codePoint);
								i += 5;
							} catch	(NumberFormatException e) {
								// Invalid escape sequence,	keep as-is
								sb.append('\\').append('u');
								i++;
							}
						} else {
							// Incomplete unicode escape
							sb.append('\\').append('u');
							i++;
						}
						break;
					default:
						// Unknown escape sequence,	keep as-is
						sb.append('\\').append(next);
						i++;
						break;
				}
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}




	// Split a string into a list of words, allowing JSON escape sequencs within quotes.
	// Parameters:
	//  output = Receives the words.
	//  input = Input string.
	// Returns the number of words found.
	// Regions within the input encosed in quotes may contain JSON escapes.
	// Note: Words are separted by unquoted JSON whitespace: space, tab, linefeed, carriage return.

	public static int split_words_with_json_escapes (Collection<String> output, String input) {
		StringBuilder sb = null;
		int	length = input.length();
		int word_count = 0;

		for	(int i = 0;	i <	length;	i++) {
			char c = input.charAt(i);

			// If white space, check for end of a word

			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				if (sb != null) {
					++word_count;
					output.add (sb.toString());
					sb = null;
				}
			}

			// Otherwise, not white space

			else {

				// Check for start of a word

				if (sb == null) {
					sb = new StringBuilder();
				}

				// Check for start of a quoted region

				if (c == '"') {

					// Scan forward to find the closing quote or end-of-string

					boolean f_even_bs = true;		// true if we have seen an even number of consecutive backslashes

					int j;
					for (j = i + 1; j < length; ++j) {
						char d = input.charAt(j);
						if (f_even_bs && d == '"') {
							// found closing quote
							break;
						} else if (d == '\\') {
							// found backslash, adjust even-or-odd flag
							f_even_bs = !f_even_bs;
						} else {
							// non-backslash, reset even-or-odd flag
							f_even_bs = true;
						}
					}

					// Unescape the quoted region

					sb.append (unescape_json_string (input.substring (i + 1, j)));
					i = j;	// could be the final quote or one past the end of the input
				}

				// Otherwise, append the character to the word

				else {
					sb.append (c);
				}
			}
		}

		// Output the final word if any

		if (sb != null) {
			++word_count;
			output.add (sb.toString());
		}

		return word_count;
	}




	// Iterator to iterate over the words in a string, in the manner of split_words_with_json_escapes.

	public static class SWWJEIterator implements Iterator<String> {

		// The input string

		private String input;

		// The current index into the string (first char of next word, or past the end).

		private int index;

		// Advance the index to the start of the next word, or past the end.

		private void skip_white () {
			int length = input.length();
			while (index < length) {
				char c = input.charAt(index);

				// If white space, advance to the next character, else stop

				if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
					++index;
				} else {
					return;
				}
			}
			return;
		}

		// We have another word if the index is not past the end.

		@Override
		public boolean hasNext () {
			return index < input.length();
		}

		// Get the next word.
		// We can assume that index points to a non-whitespace character, or past the end.

		@Override
		public String next () {
			int length = input.length();
			if (index >= length) {
				throw new NoSuchElementException ("SWWJEIterator: Attemt to read past end of line");
			}

			StringBuilder sb = new StringBuilder();
			while (index < length) {
				char c = input.charAt(index);
				++index;

				// If white space, we have reached the end of the word

				if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
					skip_white();
					return sb.toString();
				}

				// Check for start of a quoted region

				if (c == '"') {

					// Scan forward to find the closing quote or end-of-string

					boolean f_even_bs = true;		// true if we have seen an even number of consecutive backslashes

					int j;
					for (j = index; j < length; ++j) {
						char d = input.charAt(j);
						if (f_even_bs && d == '"') {
							// found closing quote
							break;
						} else if (d == '\\') {
							// found backslash, adjust even-or-odd flag
							f_even_bs = !f_even_bs;
						} else {
							// non-backslash, reset even-or-odd flag
							f_even_bs = true;
						}
					}

					// Unescape the quoted region

					sb.append (unescape_json_string (input.substring (index, j)));
					index = j + 1;	// could be one past the final quote or past the end of the input
				}

				// Otherwise, append the character to the word

				else {
					sb.append (c);
				}
			}

			// Reached end of string while scanning the word

			return sb.toString();
		}

		// Construct from the input line.

		public SWWJEIterator (String input) {
			this.input = input;
			this.index = 0;
			skip_white();
		}
	}




	// Iterate over words in a string, allowing JSON escape sequencs within quotes.
	// Parameters:
	//  input = Input string.
	// Returns the iterator.

	public static Iterator<String> iterate_words_with_json_escapes (String input) {
		return new SWWJEIterator (input);
	}




	//----- Testing -----




	// Show a string and then each character in Unicode.

	private static void test_show_unicode (String s) {
		System.out.println (s);
		System.out.println ();
		for (int j = 0; j < s.length(); ++j) {
			char ch = s.charAt(j);
			int ich = (int)ch;
			System.out.println ("U+" + String.format("%04X", ich) + "  " + s.substring(j, j+1));
		}
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "SimpleParse");




		// Subcommand : Test #1
		// Command format:
		//  test1  str  f_unicode  quote_opt
		// Expand JSON escapes within the string, and display the result.
		// Then convert back to JSON escapes, with given unicode and quote options, and display the result.
		// Note: On Bash command line, enclose str in single quotes to avoid Bash recognizing escapes.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("JSON unescape and escape.");
			String str = testargs.get_string ("str");
			boolean f_unicode = testargs.get_boolean ("f_unicode");
			int quote_opt = testargs.get_int ("quote_opt");
			testargs.end_test();

			System.out.println ();
			System.out.println ("********** Unescape JSON string **********");
			System.out.println ();

			String s2 = unescape_json_string (str);
			test_show_unicode (s2);

			System.out.println ();
			System.out.println ("********** Escape JSON string **********");
			System.out.println ();

			String s3 = escape_json_string (s2, f_unicode, quote_opt);
			test_show_unicode (s3);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  line
		// Split the line into words, that may contain quoted regions with white space and JSON escapes.
		// Note: On Bash command line, enclose line in single quotes to avoid Bash recognizing escapes.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Split line into words with JSON escapes.");
			String line = testargs.get_string ("line");
			testargs.end_test();

			System.out.println ();
			System.out.println ("********** Split into words **********");
			System.out.println ();

			List<String> words = new ArrayList<String>();
			int word_count = split_words_with_json_escapes (words, line);

			System.out.println ("Number of words = " + word_count);

			for (String word : words) {
				System.out.println ();
				test_show_unicode (word);
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  line
		// Iterate over words in the line, that may contain quoted regions with white space and JSON escapes.
		// Note: On Bash command line, enclose line in single quotes to avoid Bash recognizing escapes.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Iterate over words with JSON escapes.");
			String line = testargs.get_string ("line");
			testargs.end_test();

			System.out.println ();
			System.out.println ("********** Iterate over words **********");
			System.out.println ();

			Iterator<String> iter = iterate_words_with_json_escapes (line);
			int word_count = 0;

			while (iter.hasNext()) {
				++word_count;
				System.out.println ();
				System.out.println ("word_count = " + word_count);
				test_show_unicode (iter.next());
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
