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


// Class that represents a binary stream to which objects can be written (marshaled).
// Author: Michael Barall.

public class MarshalOutputBinaryStream implements MarshalOutput, Closeable {

	//----- Data storage -----

	// The data destination.

	private DataOutput data_out;


	//----- Implementation of MarshalOutput -----

	// Write primitive types.
	// Each must write the requested value or throw IOException (or subclass thereof).

	@Override
	public void writeBoolean(boolean v) throws IOException {
		data_out.writeBoolean (v);
		return;
	}

	@Override
	public void writeInt(int v) throws IOException {
		data_out.writeInt (v);
		return;
	}

	@Override
	public void writeLong(long v) throws IOException {
		data_out.writeLong (v);
		return;
	}

	@Override
	public void writeFloat(float v) throws IOException {
		data_out.writeFloat (v);
		return;
	}

	@Override
	public void writeDouble(double v) throws IOException {
		data_out.writeDouble (v);
		return;
	}

	// Write strings.
	// Each must write a non-null string or throw IOException (or subclass thereof).

	@Override
	public void writeString(String s) throws IOException {
		if (s == null) {
			throw new IODataFormatException ("Attempt to write null string");
		}
		//data_out.writeUTF (s);		// limit of 65535 bytes
		byte[] s_bytes = s.getBytes (StandardCharsets.UTF_8);
		int n = s_bytes.length;
		data_out.writeInt (n);
		data_out.write (s_bytes);
		return;
	}

	@Override
	public void writeName(String s) throws IOException {
		if (s == null) {
			throw new IODataFormatException ("Attempt to write null name");
		}
		data_out.writeUTF (s);
		return;
	}


	//----- Construction -----


	// Create an object that writes to the given destination.

	public MarshalOutputBinaryStream (DataOutput data_out) {
		this.data_out = data_out;
	}


	// Create an object that writes to the given file.

	public MarshalOutputBinaryStream (String filename) throws IOException {
		this (new DataOutputStream (new BufferedOutputStream (new FileOutputStream (filename))));
	}


	//----- Control -----


	// Get the data destination.

	public DataOutput get_data_out () {
		return data_out;
	}


	// Close the data source, if it is closeable.

	@Override
	public void close () throws IOException {
		if (data_out != null) {
			if (data_out instanceof Closeable) {
				((Closeable)data_out).close();
			}
		}
		data_out = null;
		return;
	}

}
