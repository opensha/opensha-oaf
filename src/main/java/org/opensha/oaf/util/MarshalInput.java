package org.opensha.oaf.util;

import java.io.IOException;


// Interface that represents a stream from which objects can be read (unmarshaled).
// Author: Michael Barall.
//
// This is not intended for use with structured data storage such as JSON files.

public interface MarshalInput {

	// Read primitive types.
	// Each must return the requested value or throw IOException (or subclass thereof).

	public boolean readBoolean() throws IOException;

	public int readInt() throws IOException;

	public long readLong() throws IOException;

	public float readFloat() throws IOException;

	public double readDouble() throws IOException;

	// Read strings.
	// Each must return a non-null string or throw IOException (or subclass thereof).

	public String readString() throws IOException;

	public String readName() throws IOException;

	// Check for end-of-file.
	// Returns 1 if end-of-file, 0 if not end-of-file, -1 if status cannot be determined.

	public int atEOF() throws IOException;

}
