package org.opensha.oaf.util.catalog;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.opensha.oaf.util.SimpleUtils;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.impl.StringParameter;

import org.opensha.oaf.comcat.ComcatOAFAccessor;


// Holds the strings for a rupture.
// Author: Michael Barall 09/28/2021.
//
// This is a modifiable object.

public class RuptureStrings {

	// Earthquake event id, as received from Comcat.

	public String eqk_event_id;

	// Earthquake network.

	public String eqk_network;

	// Earthquake code.

	public String eqk_code;

	// Earthquake description.

	public String eqk_description;

	// Earthquake id list.

	public String[] eqk_id_list;




	// Clear to null.

	public final RuptureStrings clear () {
		eqk_event_id    = null;
		eqk_network     = null;
		eqk_code        = null;
		eqk_description = null;
		eqk_id_list     = null;
		return this;
	}

	// Consructor - clear to null.

	public RuptureStrings () {
		clear();
	}




	// Set values.

	public final RuptureStrings set (String event_id, String network, String code, String description, String[] id_list) {
		eqk_event_id    = event_id;
		eqk_network     = network;
		eqk_code        = code;
		eqk_description = description;
		eqk_id_list     = id_list;
		return this;
	}

	// Consructor - set values.

	public RuptureStrings (String event_id, String network, String code, String description, String[] id_list) {
		set (event_id, network, code, description, id_list);
	}




	// Set from a rupture.

	public final RuptureStrings set (ObsEqkRupture rup) {

		eqk_event_id = rup.getEventId();

		Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);
		eqk_network = eimap.get (ComcatOAFAccessor.PARAM_NAME_NETWORK);
		eqk_code = eimap.get (ComcatOAFAccessor.PARAM_NAME_CODE);
		eqk_description = eimap.get (ComcatOAFAccessor.PARAM_NAME_DESCRIPTION);

		String comcat_idlist = eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST);
		if (comcat_idlist == null) {
			eqk_id_list = null;
		} else {
			List<String> idlist = ComcatOAFAccessor.idsToList (comcat_idlist, eqk_event_id);
			eqk_id_list = idlist.toArray (new String[0]);
		}

		return this;
	}

	// Constructor - set from a rupture.

	public RuptureStrings (ObsEqkRupture rup) {
		set (rup);
	}




	// Pattern used for checking ids.
	// Note: I am not aware of a specification for the allowed characters in an id.
	// The pattern below assumes an id is a word character (English letter, digit, or underscore),
	// followed by any number of characters that are safe to use in a URL (English letter, digit,
	// underscore, dot, tilde, hyphen).  The latter is due to the fact that event ids are
	// included in Comcat query URLs, and the EventWebService library includes the event ids
	// in URLs verbatim with no escaping.

	// This form matches an id, optionally preceded or followed by white space, and capture group 1 is the id.

	//private static final Pattern id_pattern_req = Pattern.compile ("\\s*([a-zA-Z0-9_][a-zA-Z0-9_.~-]*)\\s*");
	private static final Pattern id_pattern_req = Pattern.compile ("[\\x00-\\x20]*([a-zA-Z0-9_][a-zA-Z0-9_.~-]*)[\\x00-\\x20]*");

	// This form also matches a completely blank or empty string, in which case capture group 1 is null.

	//private static final Pattern id_pattern_opt = Pattern.compile ("\\s*(?:([a-zA-Z0-9_][a-zA-Z0-9_.~-]*)\\s*)?");
	private static final Pattern id_pattern_opt = Pattern.compile ("[\\x00-\\x20]*(?:([a-zA-Z0-9_][a-zA-Z0-9_.~-]*)[\\x00-\\x20]*)?");

	// This form matches an id.  There is no capture group.

	private static final Pattern id_pattern = Pattern.compile ("[a-zA-Z0-9_][a-zA-Z0-9_.~-]*");




	// Options for missing id.

	public static final int	IDMISS_NULL = 1;		// Return null
	public static final int	IDMISS_EMPTY = 2;		// Return empty string or array
	public static final int	IDMISS_EXCEPT = 3;		// Throw exception
	public static final int	IDMISS_AFFIX = 4;		// Return concatenation of prefix with suffix (only when converting id list to string)




	// Check for a valid id.
	// If id is valid, return the id, with white space trimmed.
	// If id is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.
	// If id is invalid, throw an exception.

	public static String check_valid_id (String id, int idmiss) {
		if (id != null) {
			Matcher m = id_pattern_opt.matcher(id);
			if (m.matches()) {
				String s = m.group(1);
				if (s != null) {
					return s;
				}
			} else {
				throw new IllegalArgumentException ("RuptureStrings.check_valid_id: Invalid id: " + "\"" + id + "\"");
			}
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return "";
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.check_valid_id: Missing id");
		}
		throw new IllegalArgumentException ("RuptureStrings.check_valid_id: Invalid missing ID option: idmiss = " + idmiss);
	}




	// Throw an exception if the id is not a valid id.
	// Does not allow leading or trailing white space.

	public static void require_valid_id (String id) {
		if (!( id_pattern.matcher(id).matches() )) {
			throw new IllegalArgumentException ("RuptureStrings.require_valid_id: Invalid id: " + "\"" + id + "\"");
		}
		return;
	}




	// Check for a valid array of ids.
	// If idarr is valid, return a copy, with white space trimmed from each contained id.
	// If idarr is missing (null or empty array), take the action selected by idmiss = IDMISS_XXXX.
	// If idarr is invalid, throw an exception.

	public static String[] check_valid_id_array (String[] idarr, int idmiss) {
		if (idarr != null && idarr.length > 0) {
			String[] result = new String[idarr.length];
			for (int n = 0; n < idarr.length; ++n) {
				result[n] = check_valid_id (idarr[n], IDMISS_EXCEPT);
			}
			return result;
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return new String[0];
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.check_valid_id_array: Missing id");
		}
		throw new IllegalArgumentException ("RuptureStrings.check_valid_id_array: Invalid missing ID option: idmiss = " + idmiss);
	}




	// Check for a valid element in an array of ids.
	// If idarr is valid, return a copy, with white space trimmed from each contained id.
	// If idarr is missing (null or insufficiently long), take the action selected by idmiss = IDMISS_XXXX.
	// If idarr is invalid, throw an exception.

	public static String check_valid_id_array_element (String[] idarr, int n, int idmiss) {
		if (n < 0) {
			throw new IllegalArgumentException ("RuptureStrings.check_valid_id_array_element: Invalid element index: n = " + n);
		}
		if (idarr != null && idarr.length > n) {
			return check_valid_id (idarr[n], IDMISS_EXCEPT);
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return "";
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.check_valid_id_array_element: Missing id");
		}
		throw new IllegalArgumentException ("RuptureStrings.check_valid_id_array_element: Invalid missing ID option: idmiss = " + idmiss);
	}




	// Convert an array of ids to a string.
	// Parameters:
	//  idarr = Array of ids.
	//  sep = Separator string to insert between ids, must be non-null and non-empty.
	//  prefix = Prefix to insert at start of string, or null if none.
	//  suffix = Suffix to insert at end of string, or null if none.
	//  idmiss = Action to take if idarr is null or a zero-length array.
	// IDMISS_AFFIX is permitted.
	// Throws an exception if any element of idarr is not a valid id.
	// Removes leading and trailing white space from each id.

	public static String id_array_to_string (String[] idarr, String sep, String prefix, String suffix, int idmiss) {
		if (idarr != null && idarr.length > 0) {
			StringBuilder sb = new StringBuilder();

			// Start with the prefix, if any

			if (prefix != null) {
				sb.append (prefix);
			}

			// Now all the identifiers, checking each one and removing leading/trailing white space, with separators

			sb.append (check_valid_id (idarr[0], IDMISS_EXCEPT));
			for (int n = 1; n < idarr.length; ++n) {
				sb.append (sep);
				sb.append (check_valid_id (idarr[n], IDMISS_EXCEPT));
			}

			// Finally the suffix, if any

			if (suffix != null) {
				sb.append (suffix);
			}
			return sb.toString();
		}

		// Null or empty list

		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return "";
		case IDMISS_AFFIX:
			return ((prefix == null) ? "" : prefix) + ((suffix == null) ? "" : suffix);
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.id_array_to_string: Missing or empty id list");
		}
		throw new IllegalArgumentException ("RuptureStrings.id_array_to_string: Invalid missing ID option: idmiss = " + idmiss);
	}




	// Convert a string to an array of ids.
	// Parameters:
	//  s = String containing list of ids.
	//  sep = Separator string to insert between ids, or null if none.
	//  prefix = Prefix to insert at start of string, or null if none.
	//  suffix = Suffix to insert at end of string, or null if none.
	//  f_affix_opt = True if presence of prefix and suffix is optional.
	//  idmiss = Action to take if s is null or contains no ids.
	// Throws an exception if any element of idarr is not a valid id.
	// Removes leading and trailing white space from each id.
	// Note: If sep is null or empty, then ids in s are separated by white space (as defined for String.trim).

	public static String[] string_to_id_array (String s, String sep, String prefix, String suffix, boolean f_affix_opt, int idmiss) {

		// If s is non-null and not all whitespace ...

		if (s != null) {
			String t = s.trim();
			if (!( t.isEmpty() )) {

				// Trim the parameters, convert null to empty string

				String prefix_trim = ((prefix == null) ? "" : prefix.trim());
				int prefix_len = prefix_trim.length();

				String suffix_trim = ((suffix == null) ? "" : suffix.trim());
				int suffix_len = suffix_trim.length();

				String sep_trim = ((sep == null) ? "" : sep.trim());
				int sep_len = sep_trim.length();

				// If there is a prefix and/or suffix ...

				if (prefix_len + suffix_len > 0) {

					// If prefix and suffix are present, strip them

					if (prefix_len + suffix_len <= t.length()
						&& t.startsWith (prefix_trim)
						&& t.endsWith (suffix_trim)
					) {
						t = t.substring(prefix_len, t.length() - suffix_len).trim();
					}

					// If not present, throw exception if not optional

					else if (!( f_affix_opt )) {
						throw new IllegalArgumentException ("RuptureStrings.string_to_id_array: Required prefix and/or suffix not found");
					}
				}

				// If stripped string is non-empty ...

				if (!( t.isEmpty() )) {

					// If separator is empty ...

					if (sep_len == 0) {

						// Split string around white space (as defined for String.trim)

						String[] result = SimpleUtils.split_around_trim (t);

						// Result should always be non-empty

						if (result.length == 0) {
							throw new IllegalStateException ("RuptureStrings.string_to_id_array: Unexpectedly found no words when splitting around white space");
						}

						// Check each id for validity

						for (int n = 0; n < result.length; ++n) {
							require_valid_id (result[n]);
						}

						// Return the result

						return result;
					}

					// Otherwise, separator is non-empty ...

					else {

						// List of ids

						ArrayList<String> idlist = new ArrayList<String>();

						// Length of string to scan

						int t_len = t.length();

						// Start of current id within String

						int begin = 0;

						// Loop over ids

						while (begin < t_len) {

							// Find next occurrence of separator, end of string if not found

							int end = t.indexOf (sep_trim, begin);
							if (end < 0) {
								end = t_len;
							}

							// Check id for validity, and save to list

							idlist.add (check_valid_id (t.substring(begin, end), IDMISS_EXCEPT));

							// Advance past the separator

							begin = end + sep_len;
						}

						// Result should always be non-empty

						if (idlist.isEmpty()) {
							throw new IllegalStateException ("RuptureStrings.string_to_id_array: Unexpectedly found no words when splitting around separator");
						}

						// Return the result as an array

						return idlist.toArray (new String[0]);
					}
				}
			}
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return new String[0];
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.string_to_id_array: Null or empty string, or no ids found");
		}
		throw new IllegalArgumentException ("RuptureStrings.string_to_id_array: Invalid missing ID option: idmiss = " + idmiss);
	}



	// Add parameters to a rupture.

	public final void add_parameters (ObsEqkRupture rup) {

		String s;

		// The description

		if (eqk_description != null) {
			s = eqk_description.trim();
			if (!( s.isEmpty() )) {
				rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_DESCRIPTION, s));
			}
		}

		// The id list, in Comcat format

		s = id_array_to_string (eqk_id_list, ",", ",", ",", IDMISS_NULL);
		if (s != null) {
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_IDLIST, s));
		}

		// The network

		s = check_valid_id (eqk_network, IDMISS_NULL);
		if (s != null) {
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_NETWORK, s));
		}

		// The code

		s = check_valid_id (eqk_code, IDMISS_NULL);
		if (s != null) {
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_CODE, s));
		}

		return;
	}




	// Make an ObsEqkRupture object.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.
	// Throws an exception if unable to create the object.
	// Note: Throws an exception if there is not a valid event id.

	public final ObsEqkRupture getObsEqkRupture (AbsoluteTimeLocation tloc, double mag, boolean wrapLon) {
		ObsEqkRupture rup = new ObsEqkRupture (check_valid_id (eqk_event_id, IDMISS_EXCEPT), tloc.abs_time, tloc.getLocation (wrapLon), mag);
		add_parameters (rup);
		return rup;
	}




	//  // Private function to prepare a string for display.
	//  
	//  private static String disp_string (String s) {
	//  	if (s == null) {
	//  		return "<null>";
	//  	}
	//  	if (s.isEmpty() || s.matches (".*\\s.*")) {
	//  		return "\"" + s + "\"";
	//  	}
	//  	return s;
	//  }




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("RuptureStrings:" + "\n");

		result.append ("eqk_event_id = " + SimpleUtils.disp_string_for_user (eqk_event_id) + "\n");
		result.append ("eqk_network = " + SimpleUtils.disp_string_for_user (eqk_network) + "\n");
		result.append ("eqk_code = " + SimpleUtils.disp_string_for_user (eqk_code) + "\n");
		result.append ("eqk_description = " + SimpleUtils.disp_string_for_user (eqk_description) + "\n");

		if (eqk_id_list == null) {
			result.append ("eqk_id_list = " + "<null>" + "\n");
		} else {
			result.append ("eqk_id_list = " + "[");
			for (int n = 0; n < eqk_id_list.length; ++n) {
				if (n != 0) {
					result.append (" ");
				}
				result.append (SimpleUtils.disp_string_for_user (eqk_id_list[n]));
			}
			result.append ("]" + "\n");
		}

		return result.toString();
	}

	// Value string.

	public final String value_string() {
		StringBuilder result = new StringBuilder();

		result.append ("eqk_event_id = " + SimpleUtils.disp_string_for_user (eqk_event_id) + "\n");
		result.append ("eqk_network = " + SimpleUtils.disp_string_for_user (eqk_network) + "\n");
		result.append ("eqk_code = " + SimpleUtils.disp_string_for_user (eqk_code) + "\n");
		result.append ("eqk_description = " + SimpleUtils.disp_string_for_user (eqk_description) + "\n");

		if (eqk_id_list == null) {
			result.append ("eqk_id_list = " + "<null>" + "\n");
		} else {
			result.append ("eqk_id_list = " + "[");
			for (int n = 0; n < eqk_id_list.length; ++n) {
				if (n != 0) {
					result.append (" ");
				}
				result.append (SimpleUtils.disp_string_for_user (eqk_id_list[n]));
			}
			result.append ("]" + "\n");
		}

		return result.toString();
	}




	// Copy from another object.

	public final RuptureStrings copy_from (RuptureStrings other) {
		this.eqk_event_id = other.eqk_event_id;
		this.eqk_network = other.eqk_network;
		this.eqk_code = other.eqk_code;
		this.eqk_description = other.eqk_description;

		if (other.eqk_id_list == null) {
			this.eqk_id_list = null;
		} else {
			this.eqk_id_list = Arrays.copyOf (other.eqk_id_list, other.eqk_id_list.length);
		}

		return this;
	}

	// Constructor - copy from another object.

	public RuptureStrings (RuptureStrings other) {
		copy_from (other);
	}




	// Set coerced event id.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_event_id (String event_id, int idmiss) {
		eqk_event_id = check_valid_id (event_id, idmiss);
		return;
	}


	// Get coerced event id.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final String get_coerce_event_id (int idmiss) {
		return check_valid_id (eqk_event_id, idmiss);
	}




	// Set coerced network.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_network (String network, int idmiss) {
		eqk_network = check_valid_id (network, idmiss);
		return;
	}


	// Get coerced network.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final String get_coerce_network (int idmiss) {
		return check_valid_id (eqk_network, idmiss);
	}




	// Set coerced code.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_code (String code, int idmiss) {
		eqk_code = check_valid_id (code, idmiss);
		return;
	}


	// Get coerced code.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final String get_coerce_code (int idmiss) {
		return check_valid_id (eqk_code, idmiss);
	}




	// Set coerced id list.
	// If string array is missing (null or empty), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_id_list (String[] id_list, int idmiss) {
		eqk_id_list = check_valid_id_array (id_list, idmiss);
		return;
	}


	// Get coerced id list.
	// If string array is missing (null or empty), take the action selected by idmiss = IDMISS_XXXX.

	public final String[] get_coerce_id_list (int idmiss) {
		return check_valid_id_array (eqk_id_list, idmiss);
	}


	// Get coerced id list element.
	// If string array is missing (null or too short), take the action selected by idmiss = IDMISS_XXXX.

	public final String get_coerce_id_list_element (int n, int idmiss) {
		return check_valid_id_array_element (eqk_id_list, n, idmiss);
	}


	// Set coerced id list, from a single string.
	// See string_to_id_array for parameters.
	// If string is missing (null, empty, or contains no ids), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_id_list (String s, String sep, String prefix, String suffix, boolean f_affix_opt, int idmiss) {
		eqk_id_list = string_to_id_array (s, sep, prefix, suffix, f_affix_opt, idmiss);
		return;
	}


	// Get coerced id list, as a single string.
	// See id_array_to_string for parameters.
	// If string array is missing (null or empty), take the action selected by idmiss = IDMISS_XXXX.
	// IDMISS_AFFIX is permitted.

	public final String get_coerce_id_list (String sep, String prefix, String suffix, int idmiss) {
		return id_array_to_string (eqk_id_list, sep, prefix, suffix, idmiss);
	}




	// Coerce a description.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public static String coerce_description (String description, int idmiss) {
		if (description != null) {
			String s = SimpleUtils.trim_and_normalize (description);
			if (!( s.isEmpty() )) {
				return s;
			}
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return "";
		case IDMISS_EXCEPT:
			throw new IllegalArgumentException ("RuptureStrings.coerce_description: Missing description");
		}
		throw new IllegalArgumentException ("RuptureStrings.coerce_description: Invalid missing string option: idmiss = " + idmiss);
	} 


	// Set coerced description.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final void set_coerce_description (String description, int idmiss) {
		eqk_description = coerce_description (description, idmiss);
		return;
	}


	// Get coerced description.
	// If string is missing (null or blank), take the action selected by idmiss = IDMISS_XXXX.

	public final String get_coerce_description (int idmiss) {
		return coerce_description (eqk_description, idmiss);
	}

}
