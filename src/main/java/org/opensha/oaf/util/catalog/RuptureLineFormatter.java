package org.opensha.oaf.util.catalog;


// Interface for formatting and parsing one line, that describes a rupture.
// Author: Michael Barall 10/11/2021.
//
// The function of a line formatter is to convert rupture parameters to and
// from a string, which can be one line in a file.
// The string should not include a line terminator (e.g., newline character).
// The rupture parameters are stored in a RuptureFormatter object.
// Note: A line formatter can be used with multiple RuptureFormatter objects.
// Note: A line formatter can be used by only one thread at a time.

public interface RuptureLineFormatter {

	// Format a line, taking parameter values from rf.
	// Parameters:
	//  rf = Rupture formatter object that contains rupture parameters.
	// Returns a line containing the formatted parameters.

	public String format_line (RuptureFormatter rf);


	// Parse a line, storing parameter value into rf.
	// Parameters:
	//  rf = Rupture formatter object that receives the rupture parameters.
	//  line = The line to parse.

	public void parse_line (RuptureFormatter rf, String line);

}

