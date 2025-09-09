package org.opensha.oaf.util;

import java.io.IOException;


// Abstract class that represents a stream of strings to which objects can be written (marshaled).
// Author: Michael Barall.
//
// This class provides the functionality to convert from primitive types to strings.
//
// The subclass must implement: writeString, writeName, writePrimitive.

public abstract class MarshalOutputStringStream implements MarshalOutput {

	// Write primitive types.
	// Each must write the requested value or throw IOException (or subclass thereof).

	@Override
	public void writeBoolean(boolean v) throws IOException {
		writePrimitive (Boolean.toString (v));
		return;
	}

	@Override
	public void writeInt(int v) throws IOException {
		writePrimitive (Integer.toString (v));
		return;
	}

	@Override
	public void writeLong(long v) throws IOException {
		writePrimitive (Long.toString (v));
		return;
	}

	@Override
	public void writeFloat(float v) throws IOException {
		writePrimitive (Float.toString (v));
		return;
	}

	@Override
	public void writeDouble(double v) throws IOException {
		writePrimitive (Double.toString (v));
		return;
	}

	// Write strings.
	// Each must write a non-null string or throw IOException (or subclass thereof).

	@Override
	public abstract void writeString(String s) throws IOException;

	@Override
	public abstract void writeName(String s) throws IOException;

	// Write a string that is expected to contain a primitive type.
	// Must write the string or throw IOException (or subclass thereof).

	protected abstract void writePrimitive(String s) throws IOException;

}
