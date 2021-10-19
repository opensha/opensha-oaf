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


// Class to write a file as a consumer of lines.
// Author: Michael Barall 10/16/2021.
//
// Each call to accept() writes the next line of the file.
// Lines are delimited as defined by BufferedWriter.newLine, which is the 
// system default, or by a specified line terminator string.
// By default, we use "\n" as the terminator string.
// Supplied lines do not include the line termination, but may include
// leading and trailing white space.
// Writing a null string to accept() has no effect.
//
// It is possible to write to a string with:
//   StringWriter sw = new StringWriter();
//   LineConsumerFile lcf = new LineConsumerFile (sw);
// After lcf is closed (or flushed), the resulting string can be obtained with:
//   String s = sw.toString();
//
// This class can be used in try-with-resources.
// This class catches all checked exceptions and re-throws them as unchecked exceptions.

public class LineConsumerFile implements Consumer<String>, AutoCloseable {

	// The file, or null if not open.

	private BufferedWriter bw;

	// The name of the file, or null if unknown.

	private String name;

	// The number of lines read so far.

	private long line_count;

	// The line terminator string, or null for the system default.
	// Defaults to "\n".

	private String line_terminator;


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


	// Get the line terminator string, which can be null for system default.

	public final String get_line_terminator () {
		return line_terminator;
	}


	// Set the line terminator string, which can be null for system default.

	public final LineConsumerFile set_line_terminator (String terminator) {
		line_terminator = terminator;
		return this;
	}


	// Constructor opens the file.

	public LineConsumerFile (String filename) {
		name = filename;
		line_count = 0L;
		line_terminator = "\n";
		try {
			bw = new BufferedWriter (new FileWriter (filename));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineConsumerFile: Unable to open file: filename = " + name, e);
		}
	}

	public LineConsumerFile (String filename, boolean append) {
		name = filename;
		line_count = 0L;
		line_terminator = "\n";
		try {
			bw = new BufferedWriter (new FileWriter (filename, append));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineConsumerFile: Unable to open file: filename = " + name, e);
		}
	}

	public LineConsumerFile (File file) {
		name = file.getPath();
		line_count = 0L;
		line_terminator = "\n";
		try {
			bw = new BufferedWriter (new FileWriter (file));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineConsumerFile: Unable to open file: filename = " + name, e);
		}
	}

	public LineConsumerFile (File file, boolean append) {
		name = file.getPath();
		line_count = 0L;
		line_terminator = "\n";
		try {
			bw = new BufferedWriter (new FileWriter (file, append));
		}
		catch (IOException e) {
			throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to open file: filename = " + name, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("LineConsumerFile: Unable to open file: filename = " + name, e);
		}
	}


	// Constructor opens a writer.

	public LineConsumerFile (Writer writer) {
		name = null;
		line_count = 0L;
		line_terminator = "\n";
		try {
			if (writer instanceof BufferedWriter) {
				bw = (BufferedWriter)writer;
			} else {
				bw = new BufferedWriter (writer);
			}
		}
		//catch (IOException e) {
		//	throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to open writer", e);
		//}
		catch (Exception e) {
			throw new RuntimeException ("LineConsumerFile: Unable to open writer", e);
		}
	}


	// Close the file.
	// Performs no operation if the file is already closed.

	@Override
	public void close () {
		if (bw != null) {
			BufferedWriter my_bw = bw;
			bw = null;
			try {
				my_bw.close();
			}
			catch (IOException e) {
				throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to close file: " + error_locus(), e);
			}
			catch (Exception e) {
				throw new RuntimeException ("LineConsumerFile: Unable to close file: " + error_locus(), e);
			}
		}
		return;
	}


	// Close the file, discarding any exception.
	// Performs no operation if the file is already closed.

	private void close_no_except () {
		if (bw != null) {
			BufferedWriter my_bw = bw;
			bw = null;
			try {
				my_bw.close();
			}
			catch (Exception e) {
			}
		}
		return;
	}


	// Write the next line.

	@Override
	public void accept (String line) {

		// If line has been supplied ...

		if (line != null) {

			// If open ...

			if (bw != null) {

				// Write the next line, followed by the terminator, in case of error close the file

				try {
					int len = line.length();
					if (len > 0) {
						bw.write (line, 0, len);
					}
					if (line_terminator == null) {
						bw.newLine();
					}
					else {
						int tlen = line_terminator.length();
						if (tlen > 0) {
							bw.write (line_terminator, 0, tlen);
						}
					}
				}
				catch (IOException e) {
					close_no_except();
					throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to write file: " + error_locus(), e);
				}
				catch (Exception e) {
					close_no_except();
					throw new RuntimeException ("LineConsumerFile: Unable to write file: " + error_locus(), e);
				}

				// Count the line

				++line_count;
			}

			// If closed, throw an exception

			else {
				throw new IllegalStateException ("LineConsumerFile: Unable to write file because it is already closed: " + error_locus());
			}
		}

		return;
	}


	// Flush buffered data to the file.
	// Performs no operation if the file is not open.

	public void flush () {
		if (bw != null) {
			try {
				bw.flush();
			}
			catch (IOException e) {
				close_no_except();
				throw new RuntimeException ("LineConsumerFile: I/O error whle attempting to flush file: " + error_locus(), e);
			}
			catch (Exception e) {
				close_no_except();
				throw new RuntimeException ("LineConsumerFile: Unable to flush file: " + error_locus(), e);
			}
		}
		return;
	}
}
