package org.opensha.oaf.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.charset.StandardCharsets;


// Class that represents a binary stream from which objects can be read (unmarshaled).
// Author: Michael Barall.

public class MarshalInputBinaryStream implements MarshalInput, Closeable {

	//----- Data storage -----

	// The data source.

	private DataInput data_in;


	//----- Implementation of MarshalInput -----

	// Read primitive types.
	// Each must return the requested value or throw IOException (or subclass thereof).

	@Override
	public boolean readBoolean() throws IOException {
		return data_in.readBoolean();
	}

	@Override
	public int readInt() throws IOException {
		return data_in.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return data_in.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return data_in.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return data_in.readDouble();
	}

	// Read strings.
	// Each must return a non-null string or throw IOException (or subclass thereof).

	@Override
	public String readString() throws IOException {
		//return data_in.readUTF();		// limit of 65535 bytes
		int n = data_in.readInt();
		if (n < 0) {
			throw new IODataFormatException ("String has negative length");
		}
		byte[] s_bytes = new byte[n];
		data_in.readFully (s_bytes);
		return new String (s_bytes, StandardCharsets.UTF_8);
	}

	@Override
	public String readName() throws IOException {
		return data_in.readUTF();
	}

	// Check for end-of-file.
	// Returns 1 if end-of-file, 0 if not end-of-file, -1 if status cannot be determined.

	@Override
	public int atEOF() throws IOException {
		return -1;
	}


	//----- Construction -----


	// Create an object that reads from the given source.

	public MarshalInputBinaryStream (DataInput data_in) {
		this.data_in = data_in;
	}


	// Create an object that reads from the given binary file.

	public MarshalInputBinaryStream (String filename) throws IOException {
		this (new DataInputStream (new BufferedInputStream (new FileInputStream (filename))));
	}


	//----- Control -----


	// Get the data source.

	public DataInput get_data_in () {
		return data_in;
	}


	// Close the data store, if it is closeable.

	@Override
	public void close () throws IOException {
		if (data_in != null) {
			if (data_in instanceof Closeable) {
				((Closeable)data_in).close();
			}
		}
		data_in = null;
		return;
	}

}
