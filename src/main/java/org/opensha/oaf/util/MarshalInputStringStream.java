package org.opensha.oaf.util;

import java.io.IOException;


// Abstract class that represents a stream of strings from which objects can be read (unmarshaled).
// Author: Michael Barall.
//
// This class provides the functionality to convert from strings to primitive types.
//
// The subclass must implement: readString, readName, atEOF, readPrimitive.

public abstract class MarshalInputStringStream implements MarshalInput {

	// Read primitive types.
	// Each must return the requested value or throw IOException (or subclass thereof).

	@Override
	public boolean readBoolean() throws IOException {
		String s = readPrimitive();
		if (s.equalsIgnoreCase ("true")) {
			return true;
		}
		if (s.equalsIgnoreCase ("false")) {
			return false;
		}
		throw new IODataFormatException ("Invalid boolean value");
	}

	@Override
	public int readInt() throws IOException {
		String s = readPrimitive();
		int x;
		try {
			x = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new IODataFormatException ("Invalid integer value", e);
		}
		return x;
	}

	@Override
	public long readLong() throws IOException {
		String s = readPrimitive();
		long x;
		try {
			x = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new IODataFormatException ("Invalid long value", e);
		}
		return x;
	}

	@Override
	public float readFloat() throws IOException {
		String s = readPrimitive();
		float x;
		try {
			x = Float.parseFloat(s);
		} catch (NumberFormatException e) {
			throw new IODataFormatException ("Invalid float value", e);
		}
		return x;
	}

	@Override
	public double readDouble() throws IOException {
		String s = readPrimitive();
		double x;
		try {
			x = Double.parseDouble(s);
		} catch (NumberFormatException e) {
			throw new IODataFormatException ("Invalid double value", e);
		}
		return x;
	}

	// Read strings.
	// Each must return a non-null string or throw IOException (or subclass thereof).

	@Override
	public abstract String readString() throws IOException;

	@Override
	public abstract String readName() throws IOException;

	// Check for end-of-file.
	// Returns 1 if end-of-file, 0 if not end-of-file, -1 if status cannot be determined.

	@Override
	public abstract int atEOF() throws IOException;

	// Read a string that is expected to contain a primitive type.
	// Must return a non-null string or throw IOException (or subclass thereof).

	protected abstract String readPrimitive() throws IOException;

}
