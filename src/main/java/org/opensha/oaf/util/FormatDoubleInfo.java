package org.opensha.oaf.util;

import static org.opensha.oaf.util.SimpleUtils.TRAILZ_OK;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_REMOVE;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_PAD_LEFT;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_PAD_RIGHT;


/**
 * Holds information for formatting a double, including suppression of trailing zeros.
 * Author: Michael Barall 10/09/2021.
 */
public class FormatDoubleInfo {

	// Format code, to use for String.format.

	public String fmt;

	// Trailing zero removal option, TRAILZ_XXXX.

	public int trailz;

	// Field width, or 0 if width is variable or unspecified.

	public int field_width;




	// Clear to Java defaults.

	public void clear () {
		fmt = "%e";
		trailz = TRAILZ_OK;
		field_width = 0;
		return;
	}

	// Construct with Java defaults.

	public FormatDoubleInfo () {
		clear();
	}




	// Set format info.

	public void set (String the_fmt, int the_trailz, int the_field_width) {
		fmt = the_fmt;
		trailz = the_trailz;
		field_width = the_field_width;
		return;
	}

	// Construct and set format info.

	public FormatDoubleInfo (String the_fmt, int the_trailz, int the_field_width) {
		set (the_fmt, the_trailz, the_field_width);
	}




	// Set format info.
	// Parameters:
	//  form = Output form: "f", "e", "E", "g", "G", "s".
	//  prec = Precision.
	//    For "f", "e", or "E", the number of digits after the decimal point.
	//    For "g" or "G", the number of significant digits.
	//    For "s", ignored.
	//  f_trailz = True to remove trailing zeros.
	//  f_plus = True to prefix positive numbers with plus sign (except for "s").
	//  f_pad = True to add padding.
	//    For "f", pad on left with field-specific amount if available;
	//      if trailing zeros are removed then add padding on right.
	//    For "e" or "E", if f_plus is false then insert space before positive values;
	//      if trailing zeros are removed then add padding on right.
	//    For "g" or "G", if f_plus is false then insert space before positive values.
	//    For "s", ignored.
	//  zd_width = Field width, not counting the digits appearing after the decimal point,
	//    required to hold any allowed value in "f" format; or 0 if not available.
	//  f_big_exp = True to allow space for 3-digit exponent, for "e" or "E" with f_pad true.

	public void set (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad, int zd_width, boolean f_big_exp) {

		// Default is variable width

		field_width = 0;

		// Default trailing-zero option

		trailz = (f_trailz ? TRAILZ_REMOVE : TRAILZ_OK);

		// String builder to accumulate the format code

		StringBuilder sb = new StringBuilder();
		sb.append ("%");

		// Switch on form

		switch (form) {

		// Fixed point format

		case "f": {
			if (f_plus) {
				sb.append ("+");
			}
			if (prec > 0) {
				if (f_pad && zd_width > 0) {
					field_width = prec + zd_width;
					sb.append (field_width);
				}
				sb.append (".");
				sb.append (prec);
			}
			if (f_pad && f_trailz) {
				trailz = TRAILZ_PAD_RIGHT;
			}
		}
		break;

		// Floating point format

		case "e": case "E": {
			if (f_plus) {
				sb.append ("+");
			}
			else if (f_pad) {
				sb.append (" ");
			}
			if (prec > 0) {
				if (f_pad) {
					if (f_big_exp) {
						sb.append ("-");
						field_width = prec + 8;
					} else {
						field_width = prec + 7;
					}
					sb.append (field_width);
				}
				sb.append (".");
				sb.append (prec);
			}
			if (f_pad && f_trailz) {
				trailz = TRAILZ_PAD_RIGHT;
			}
		}
		break;

		// Generic format

		case "g": case "G": {
			if (f_plus) {
				sb.append ("+");
			}
			else if (f_pad) {
				sb.append (" ");
			}
		}
		break;

		// Java default format

		case "s":
		break;

		// Invalid form

		default:
			throw new IllegalArgumentException ("FormatDoubleInfo.set - Invalid numeric form: form = " + form);
		}

		// Append the form and save the format

		sb.append (form);
		fmt = sb.toString();
		return;
	}

	// Construct and set format info.

	public FormatDoubleInfo (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad, int zd_width, boolean f_big_exp) {
		set (form, prec, f_trailz, f_plus, f_pad, zd_width, f_big_exp);
	}




	// Format a double using this information.

	public String format_double (double x) {
		return SimpleUtils.double_to_string_trailz (fmt, trailz, x);
	}




	// Parse a double using this information.
	// Parse ignores leading and trailing white space (as defined by String.trim).

	public double parse_double (String s) {
		double x;
		try {
			x = Double.parseDouble(s);
		} catch (Exception e) {
			throw new RuntimeException ("FormatDoubleInfo.parse_double: Error parsing double value: s = " + s, e);
		}
		return x;
	}




	//  // Private function to prepare a string for display.
	//  
	//  private static String disp_string (String s) {
	//  	if (s == null) {
	//  		return "<null>";
	//  	}
	//  	if (s.isEmpty() || s.matches (".*\\s.*")) {
	//  		return "\"" + s.replaceAll ("\t", "\\\\t") + "\"";
	//  	}
	//  	return s;
	//  }




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("FormatDoubleInfo:" + "\n");

		result.append ("fmt = " + SimpleUtils.disp_string_for_user (fmt) + "\n");
		result.append ("trailz = " + SimpleUtils.trailz_to_string (trailz) + "\n");
		result.append ("field_width = " + field_width + "\n");

		return result.toString();
	}

	// One-line string.

	public final String one_line_string () {
		return "["
			+ "fmt = " + SimpleUtils.disp_string_for_user (fmt)
			+ ", trailz = " + SimpleUtils.trailz_to_string (trailz)
			+ ", field_width = " + field_width
			+ "]";
	}

	// One-line string for a given object, which can be null.

	public static String one_line_string (FormatDoubleInfo fdi) {
		return ((fdi == null) ? "<null>" : fdi.one_line_string());
	}

}
