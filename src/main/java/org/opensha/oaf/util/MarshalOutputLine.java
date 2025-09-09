package org.opensha.oaf.util;

import java.io.IOException;


// Class that represents a line of output to which objects can be written (marshaled).
// Author: Michael Barall.
//
// Words are separted by a single space.
// Quoted words can contain whitespace and JSON escape sequences.

public class MarshalOutputLine extends MarshalOutputStringStream {

	//----- Data storage -----

	// The line being constructed.

	private StringBuilder sb;

	// Flag indicating if Unicode (>= U+0080) is allowed.

	private boolean f_unicode;

	// Flag indicating if all strings should be quoted (true), or only quote if necessary (false).

	private boolean f_quote_all;

	// The word separator string.

	private String separator;

	// The current word count.

	private int word_count;


	//----- Implementation of MarshalOutputStringStream -----

	// Write strings.
	// Each must write a non-null string or throw IOException (or subclass thereof).

	@Override
	public void writeString(String s) throws IOException {
		if (s == null) {
			throw new IODataFormatException ("Attempt to write null string");
		}
		if (word_count != 0) {
			sb.append (separator);
		}
		sb.append (SimpleParse.escape_json_string (s, f_unicode, f_quote_all ? 1 : 0));
		++word_count;
		return;
	}

	@Override
	public void writeName(String s) throws IOException {
		if (s == null) {
			throw new IODataFormatException ("Attempt to write null name");
		}
		if (word_count != 0) {
			sb.append (separator);
		}
		sb.append (SimpleParse.escape_json_string (s, f_unicode, f_quote_all ? 1 : 0));
		++word_count;
		return;
	}

	// Write a string that is expected to contain a primitive type.
	// Must write the string or throw IOException (or subclass thereof).

	@Override
	protected void writePrimitive(String s) throws IOException {
		if (s == null) {
			throw new IODataFormatException ("Attempt to write null primitive value");
		}
		if (word_count != 0) {
			sb.append (separator);
		}
		sb.append (s);
		++word_count;
		return;
	}


	//----- Construction -----


	// Construct with given flags, placing output in the given string builder.
	// Parameters:
	//  sb = Destination for output.
	//  f_unicode = True if output can contain Unicode characters U+0080 to U+FFFF.
	//  f_quote_all = True to wrap all strings in quotes, false to use quotes only when necessary.

	public MarshalOutputLine (StringBuilder sb, boolean f_unicode, boolean f_quote_all) {
		this.sb = sb;
		this.f_unicode = f_unicode;
		this.f_quote_all = f_quote_all;
		separator = " ";
		word_count = 0;
	}


	//----- Control -----


	// Get the line.

	public String get_line () {
		return sb.toString();
	}


	// Get the word count.

	public int get_word_count () {
		return word_count;
	}

}
