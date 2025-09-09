package org.opensha.oaf.util;

import java.io.IOException;


// Interface that represents a stream to which objects can be written (marshaled).
// Author: Michael Barall.
//
// This is not intended for use with structured data storage such as JSON files.

public interface MarshalOutput {

	// Write primitive types.
	// Each must write the requested value or throw IOException (or subclass thereof).

	public void writeBoolean(boolean v) throws IOException;

	public void writeInt(int v) throws IOException;

	public void writeLong(long v) throws IOException;

	public void writeFloat(float v) throws IOException;

	public void writeDouble(double v) throws IOException;

	// Write strings.
	// Each must write a non-null string or throw IOException (or subclass thereof).

	public void writeString(String s) throws IOException;

	public void writeName(String s) throws IOException;

}
