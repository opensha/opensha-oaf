package org.opensha.oaf.util;

import java.util.Iterator;

import java.io.IOException;
import java.io.EOFException;


// Class that represents a line of input from which objects can be read (unmarshaled).
// Author: Michael Barall.
//
// The line is split into words, each of which is one data element.
// Words are separated by whitespace (space, tab, linefeed, carriage return).
// Quoted words can contain whitespace and JSON escape sequences.

public class MarshalInputLine extends MarshalInputStringStream {

	//----- Data storage -----

	// The words in the line.

	private Iterator<String> iter;

	// The current word count.

	private int word_count;


	//----- Implementation of MarshalInputStringStream -----

	// Read strings.
	// Each must return a non-null string or throw IOException (or subclass thereof).

	@Override
	public String readString() throws IOException {
		if (!( iter.hasNext() )) {
			throw new EOFException ("Attempt to read past end of line");
		}
		++word_count;
		return iter.next();
	}

	@Override
	public String readName() throws IOException {
		if (!( iter.hasNext() )) {
			throw new EOFException ("Attempt to read past end of line");
		}
		++word_count;
		return iter.next();
	}

	// Check for end-of-file.
	// Returns 1 if end-of-file, 0 if not end-of-file, -1 if status cannot be determined.

	@Override
	public int atEOF() throws IOException {
		return (iter.hasNext() ? 0 : 1);
	}

	// Read a string that is expected to contain a primitive type.
	// Must return a non-null string or throw IOException (or subclass thereof).

	@Override
	protected String readPrimitive() throws IOException {
		if (!( iter.hasNext() )) {
			throw new EOFException ("Attempt to read past end of line");
		}
		++word_count;
		return iter.next();
	}


	//----- Construction -----


	// Create an object that scans words in the given line, with JSON escapes.

	public MarshalInputLine (String line) {
		iter = SimpleParse.iterate_words_with_json_escapes (line);
		word_count = 0;
	}


	//----- Control -----


	// Return the number of words read.

	public int get_word_count () {
		return word_count;
	}

}
