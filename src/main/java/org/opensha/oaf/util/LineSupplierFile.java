package org.opensha.oaf.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Reader;
import java.io.Writer;


// Class to read a file as a supplier of lines.
// Author: Michael Barall 10/16/2021.
//
// Each call to get() reads the next line of the file.
// Lines are delimited as defined by BufferedReader.readLine, that is, "\n", "\r", or "\r\n".
// Returned lines do not include the line termination, but may include
// leading and trailing white space.
// At end of file, get() returns null.
// After end of file, additional calls to get() continue to return null.
//
// It is possible to read from a string with:
//   LineSupplierFile lsf = new LineSupplierFile (new StringReader (s));
//
// This class can be used in try-with-resources.
// This class catches all checked exceptions and re-throws them as unchecked exceptions.

public class LineSupplierFile implements Supplier<String>, AutoCloseable {

	// The file, or null if not open.

	private BufferedReader br;

	// The name of the file, or null if unknown.

	private String name;

	// The number of lines read so far.

	private long line_count;


	// Get the name of the file, or null if unknown.

	public final String get_name () {
		return name;
	}


	// Get the number of lines read so far.

	public final long get_line_count () {
		return line_count;
	}


	// Get a string that can be used to describe where an error occurred.

	public final String error_locus () {
		if (name == null) {
			return "line_count = " + line_count;
		}
		return "line_count = " + line_count + ", filename = " + name;
	}


	// Constructor opens the file.

	public LineSupplierFile (String filename) {
		name = filename;
		line_count = 0L;
		try {
			br = new BufferedReader (new FileReader (filename));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineSupplierFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineSupplierFile: Unable to open file: filename = " + name, e);
		}
	}

	public LineSupplierFile (File file) {
		name = file.getPath();
		line_count = 0L;
		try {
			br = new BufferedReader (new FileReader (file));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineSupplierFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineSupplierFile: Unable to open file: filename = " + name, e);
		}
	}


	// Constructor opens a reader.

	public LineSupplierFile (Reader reader) {
		name = null;
		line_count = 0L;
		try {
			if (reader instanceof BufferedReader) {
				br = (BufferedReader)reader;
			} else {
				br = new BufferedReader (reader);
			}
		}
		//catch (IOException e) {
		//	throw new RuntimeException ("LineSupplierFile: I/O error whle attempting to open reader", e);
		//}
		catch (Exception e) {
			throw new RuntimeException ("LineSupplierFile: Unable to open reader", e);
		}
	}


	// Close the file.
	// Performs no operation if the file is already closed.

	@Override
	public void close () {
		if (br != null) {
			BufferedReader my_br = br;
			br = null;
			try {
				my_br.close();
			}
			catch (IOException e) {
				throw new RuntimeException ("LineSupplierFile: I/O error whle attempting to close file: " + error_locus(), e);
			}
			catch (Exception e) {
				throw new RuntimeException ("LineSupplierFile: Unable to close file: " + error_locus(), e);
			}
		}
		return;
	}


	// Close the file, discarding any exception.
	// Performs no operation if the file is already closed.

	private void close_no_except () {
		if (br != null) {
			BufferedReader my_br = br;
			br = null;
			try {
				my_br.close();
			}
			catch (Exception e) {
			}
		}
		return;
	}


	// Read the next line.

	@Override
	public String get () {
		String line = null;

		// If open ...

		if (br != null) {

			// Read the next line, in case of error close the file

			try {
				line = br.readLine();
			}
			catch (IOException e) {
				close_no_except();
				throw new RuntimeException ("LineSupplierFile: I/O error whle attempting to read file: " + error_locus(), e);
			}
			catch (Exception e) {
				close_no_except();
				throw new RuntimeException ("LineSupplierFile: Unable to read file: " + error_locus(), e);
			}

			// If end of file, close the file

			if (line == null) {
				close();
			}

			// If not end of file, count the line

			else {
				++line_count;
			}
		}

		return line;
	}

}
