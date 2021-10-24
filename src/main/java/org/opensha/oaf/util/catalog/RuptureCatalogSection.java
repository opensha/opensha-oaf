package org.opensha.oaf.util.catalog;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;


// One section of a catalog file.
// Author: Michael Barall 10/14/2021.

public class RuptureCatalogSection {


	//----- Linkage -----


	// Parent of this section, or null if there is no parent.
	// This is set during construction and cannot be modified.
	// Note: Although a parent link opens up the possibiity of a tree
	// of sections and subsections, in practice we expect just three
	// levels of section:
	// - A root section, which is the top level and is not written to
	//   or read from the file.  It holds user-supplied settings.
	// - A default section, whose parent is the root section. it
	//   represents settings that apply at file scope, and can be
	//   inherited by the data sections.
	// - Data sections, all of which have the default section as parent.

	private RuptureCatalogSection parent;

	public final RuptureCatalogSection get_parent () {
		return parent;
	}


	// Name of the section.
	// This is set during construction and cannot be modified.

	private String name;

	public final String get_name () {
		return name;
	}


	// Pattern that recognizes valid section names.
	// Valid names consist of letters, digits, and underscores, and begin with a letter or underscore.

	private static final Pattern section_name_pattern = Pattern.compile ("[a-zA-Z_][a-zA-Z0-9_]*");


	// Return true if the argument is a valid section name.
	// Note: Returns false if the argument has leading or trailing white space.

	public static boolean is_valid_section_name (String the_name) {
		return section_name_pattern.matcher(the_name).matches();
	}




	//----- Common section names -----


	// Root section name.

	public static final String SECTION_NAME_ROOT = "root";

	// Default section name.

	public static final String SECTION_NAME_DEFAULT = "default";

	// Mainshock section name.

	public static final String SECTION_NAME_MAINSHOCK = "mainshock";

	// Aftershock section name.

	public static final String SECTION_NAME_AFTERSHOCK = "aftershock";




	//----- Section convenience functions -----


	// Return true if this is the root section.
	// Note: Test is done by checking the name.

	public final boolean is_root_section () {
		return name.equals (SECTION_NAME_ROOT);
	}


	// Return true if this is the root section.
	// Note: Test is done by checking the name.

	public final boolean is_default_section () {
		return name.equals (SECTION_NAME_DEFAULT);
	}




	//----- Input options -----


	// True if this section has been seen during input.

	private boolean f_input_seen = false;

	// True if this section can be split during input.

	private boolean f_input_splittable = false;

	// Set of definition names that are allowed during input, or null if no restriction.
	// Note: This has no effect on definitions added programmatically.

	private HashSet<String> input_allowed_def_names = null;

	// True if absolute/relative converter is allowed during input.

	private boolean f_input_abs_rel_conv = true;

	// True if line formatter is allowed during input.

	private boolean f_input_line_formatter = true;


	// Get flag indicating if this section has been seen during input.

	public final boolean is_input_seen () {
		return f_input_seen;
	}


	// Set flag indicating if this section can be split during input.
	// Default is that the section cannot be split.

	public final RuptureCatalogSection set_input_splittable (boolean f_splittable) {
		f_input_splittable = f_splittable;
		return this;
	}


	// Set the definition names that are allowed during input, or null if no restriction.
	// Default is that all names are allowed.
	// The names can be supplied as a collection, as an array, or as arguments to the call.
	// Note: Calling with no arguments disallows all definitions during input.

	public final RuptureCatalogSection set_allowed_def_names (Collection<String> allowed_def_names) {
		if (allowed_def_names == null) {
			input_allowed_def_names = null;
		}
		else {
			input_allowed_def_names = new HashSet<String> (allowed_def_names);
		}
		return this;
	}


	public final RuptureCatalogSection set_allowed_def_names (String... allowed_def_names) {
		if (allowed_def_names == null) {
			input_allowed_def_names = null;
		}
		else {
			input_allowed_def_names = new HashSet<String> ();
			for (String s : allowed_def_names) {
				input_allowed_def_names.add (s);
			}
		}
		return this;
	}


	// Set flag indicating if absolute/relative converter is allowed during input.
	// Default is true.

	public final RuptureCatalogSection set_input_abs_rel_conv (boolean f_allowed) {
		f_input_abs_rel_conv = f_allowed;
		return this;
	}


	// Set flag indicating if line formatter is allowed during input.
	// Default is true.

	public final RuptureCatalogSection set_input_line_formatter (boolean f_allowed) {
		f_input_line_formatter = f_allowed;
		return this;
	}




	//----- Locking -----


	// Locking flag, true if this section is locked.
	// Note: When a section is locked, no changes can be made
	// except for adding ruptures and comments.  The rationale
	// for this restriction is to simplify client code and
	// avoid order dependencies in the catalog file.

	private boolean f_locked = false;


	// Return true if section is locked.

	public final boolean is_section_locked () {
		return f_locked;
	}


	// Lock the section.

	public final void lock_section () {

		// If already locked, ignore call

		if (f_locked) {
			return;
		}

		// Lock our parent first, if not already locked

		if (parent != null) {
			parent.lock_section();
		}

		// Lock the absolute/relative converter

		lock_abs_rel_conv();

		// Lock the line formatter

		lock_line_formatter();

		// Set flag indicating we are locked

		f_locked = true;
		return;
	}




	//----- Definitions -----


	// Pattern that recognizes valid definition names.
	// Valid names consist of letters, digits, and underscores, and begin with a letter or underscore.

	private static final Pattern definition_name_pattern = Pattern.compile ("[a-zA-Z_][a-zA-Z0-9_]*");


	// Pattern that recognizes valid definition values.
	// Valid values consist of characters other than \n and \r, with no leading or trailing white space.

	private static final Pattern definition_value_pattern = Pattern.compile ("\\S(?:[^\\n\\r]*\\S)?");


	// Return true if the argument is a valid definition name.
	// Note: Returns false if the argument has leading or trailing white space.

	public static boolean is_valid_definition_name (String def_name) {
		return definition_name_pattern.matcher(def_name).matches();
	}


	// Return true if the argument is a valid definition value.
	// Note: Returns false if the argument has leading or trailing white space.

	public static boolean is_valid_definition_value (String def_value) {
		return definition_value_pattern.matcher(def_value).matches();
	}


	// Known definition names.

	public static final String DEF_NAME_WRAP_LON = "wrap_lon";		// The wrapLon parameter.

	public static final String DEF_NAME_START_TIME = "start_time";	// The timeStart parameter.

	public static final String DEF_NAME_END_TIME = "end_time";		// The timeEnd parameter.


	// The definitions for this section.
	// Each definition has a name for a key, and an arbitrary string for a value.
	// LinkedHashMap is used to preserve the ordering.

	private final LinkedHashMap<String, String> definition_map = new LinkedHashMap<String, String>();


	// Add a <name, value> pair to the dictionary of definitions in this section.
	// It is an error to call this function after the section is locked.
	// It is an error to have an invalid name.
	// If def_value is null or empty, then remove any definition.

	public final void add_definition (String def_name, String def_value) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.add_definition: Section is already locked: section = " + name);
		}
		if (!( is_valid_definition_name (def_name) )) {
			throw new IllegalArgumentException ("RuptureCatalogSection.add_definition: Invalid definition name: section = " + name + ", def_name = " + def_name);
		}
		if (def_value == null || def_value.isEmpty()) {
			definition_map.remove (def_name);
		} else {
			if (!( is_valid_definition_value (def_value) )) {
				throw new IllegalArgumentException ("RuptureCatalogSection.add_definition: Invalid definition value: section = " + name + ", def_name = " + def_name + ", def_value = " + def_value);
			}
			definition_map.put (def_name, def_value);
		}
		return;
	}


	// Get a definition for the given name from definitions for this section.
	// Return null if there is no definition for the given name.

	public final String get_definition (String def_name) {
		return definition_map.get (def_name);
	}


	// Get a definition for the given name from definitions for this section.
	// If not found, then try to get a definition from the parent section, recursively.
	// Return null if there is no definition for the given name in this or any parent section.

	public final String get_inherit_definition (String def_name) {
		String value = definition_map.get (def_name);
		if (value == null) {
			if (parent != null) {
				value = parent.get_inherit_definition (def_name);
			}
		}
		return value;
	}


	// Get an iterator over all definitions for this section.

	public final Iterator<Map.Entry<String, String>> get_definition_iterator () {
		return definition_map.entrySet().iterator();
	}




	//----- Convenience functions for definitions -----


	// Return true if there is a definition for the given name, including in the parent.

	public final boolean contains_definition (String def_name) {
		if (definition_map.containsKey (def_name)) {
			return true;
		}
		if (parent != null) {
			return parent.contains_definition (def_name);
		}
		return false;
	}


	// Add a definition for a double.

	public final void add_definition_double (String def_name, double value) {
		String def_value = String.valueOf (value);
		add_definition (def_name, def_value);
		return;
	}


	// Get a definition as a double.
	// If not found, then try to get a definition from the parent section, recursively.
	// If still not found, return the default value if provided, else throw an exception.

	public final double get_definition_double (String def_name, double default_value) {
		double result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			return default_value;
		}

		// Perform the conversion

		try {
			result = Double.parseDouble (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_double : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	public final double get_definition_double (String def_name) {
		double result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_double : Definition not found: name = " + def_name);
		}

		// Perform the conversion

		try {
			result = Double.parseDouble (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_double : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	// Add a definition for an int.

	public final void add_definition_int (String def_name, int value) {
		String def_value = String.valueOf (value);
		add_definition (def_name, def_value);
		return;
	}


	// Get a definition as an int.
	// If not found, then try to get a definition from the parent section, recursively.
	// If still not found, return the default value if provided, else throw an exception.

	public final int get_definition_int (String def_name, int default_value) {
		int result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			return default_value;
		}

		// Perform the conversion

		try {
			result = Integer.parseInt (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_int : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	public final int get_definition_int (String def_name) {
		int result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_int : Definition not found: name = " + def_name);
		}

		// Perform the conversion

		try {
			result = Integer.parseInt (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_int : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	// Add a definition for a long.

	public final void add_definition_long (String def_name, long value) {
		String def_value = String.valueOf (value);
		add_definition (def_name, def_value);
		return;
	}


	// Get a definition as a long.
	// If not found, then try to get a definition from the parent section, recursively.
	// If still not found, return the default value if provided, else throw an exception.

	public final long get_definition_long (String def_name, long default_value) {
		long result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			return default_value;
		}

		// Perform the conversion

		try {
			result = Long.parseLong (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_long : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	public final long get_definition_long (String def_name) {
		long result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_long : Definition not found: name = " + def_name);
		}

		// Perform the conversion

		try {
			result = Long.parseLong (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_long : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	// Add a definition for a boolean.

	public final void add_definition_boolean (String def_name, boolean value) {
		String def_value = String.valueOf (value);
		add_definition (def_name, def_value);
		return;
	}


	// Get a definition as a boolean.
	// If not found, then try to get a definition from the parent section, recursively.
	// If still not found, return the default value if provided, else throw an exception.

	public final boolean get_definition_boolean (String def_name, boolean default_value) {
		boolean result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			return default_value;
		}

		// Perform the conversion

		try {
			result = Boolean.parseBoolean (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_boolean : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	public final boolean get_definition_boolean (String def_name) {
		boolean result;

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_boolean : Definition not found: name = " + def_name);
		}

		// Perform the conversion

		try {
			result = Boolean.parseBoolean (def_value);
		} catch (Exception e) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_boolean : Parse error: name = " + def_name + ", value = " + def_value, e);
		}

		return result;
	}


	// Add a definition for a string.

	public final void add_definition_string (String def_name, String value) {
		String def_value = value;
		add_definition (def_name, def_value);
		return;
	}


	// Get a definition as a string.
	// If not found, then try to get a definition from the parent section, recursively.
	// If still not found, return the default value if provided, else throw an exception.

	public final String get_definition_string (String def_name, String default_value) {

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			return default_value;
		}

		// Return it

		return def_value;
	}


	public final String get_definition_string (String def_name) {

		// Get from collection, including parent

		String def_value = get_inherit_definition (def_name);
		if (def_value == null) {
			throw new RuntimeException ("RuptureCatalogSection.get_definition_string : Definition not found: name = " + def_name);
		}

		// Return it

		return def_value;
	}




	//----- Absolute/relative converter -----


	// The absolute/relative converter, or null if none.

	private AbsRelTimeLocConverter abs_rel_conv = null;


	// The absolute/relative converter description, or null if none.

	private String abs_rel_conv_description = null;


	// True if non-null abs_rel_conv can be overridden by a description.
	// If true, and there is a description at the time the section is locked,
	// then the description replaces the value of abs_rel_conv.

	private boolean f_default_abs_rel_conv = false;


	// Get the absolute/relative converter, or null if none.

	public final AbsRelTimeLocConverter get_abs_rel_conv () {
		return abs_rel_conv;
	}


	// Get the absolute/relative converter description, or null if none.

	public final String get_abs_rel_conv_description () {
		return abs_rel_conv_description;
	}


	// Set the absolute/relative converter.
	// Parameters:
	//  the_abs_rel_conv = Converter object, can be null.
	//  f_describe = True to also write a descrption.
	//  f_default = True if converter can be overridden by a description.
	// This can only be called if the section is not locked.
	// Note: f_describe and f_default should not both be true.

	public final void set_abs_rel_conv (AbsRelTimeLocConverter the_abs_rel_conv, boolean f_describe, boolean f_default) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.set_abs_rel_conv: Section is already locked: section = " + name);
		}
		abs_rel_conv = the_abs_rel_conv;
		f_default_abs_rel_conv = f_default;

		// If desired, write a descrption, or remove descrption if converter is null

		if (f_describe) {
			if (abs_rel_conv == null) {
				abs_rel_conv_description = null;
			} else {
				abs_rel_conv_description = AbsRelTimeLocConverterFactory.describe_abs_rel_conv (abs_rel_conv);
			}
		}
		return;
	}


	// Set the absolute/relative converter description.
	// If non-null and non-empty, the description must be valid as a definition value.
	// This can only be called if the section is not locked.

	public final void set_abs_rel_conv_description (String description) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.set_abs_rel_conv_description: Section is already locked: section = " + name);
		}
		if (description == null || description.isEmpty()) {
			abs_rel_conv_description = null;
		} else {
			if (!( is_valid_definition_value (description) )) {
				throw new IllegalArgumentException ("RuptureCatalogSection.set_abs_rel_conv_description: Invalid absolute/relative converter description: section = " + name + ", description = " + description);
			}
			abs_rel_conv_description = description;
		}
		return;
	}


	// Lock the absolute/relative converter.
	// The is called from lock_section to perform locking functions.

	private final void lock_abs_rel_conv () {

		// If there is no converter, or it can be overridden by a description ...

		if (abs_rel_conv == null || f_default_abs_rel_conv) {

			// If we have a description, use it

			if (abs_rel_conv_description != null) {
				try {
					abs_rel_conv = AbsRelTimeLocConverterFactory.make_abs_rel_conv (abs_rel_conv_description);
				} catch (Exception e) {
					throw new RuntimeException ("RuptureCatalogSection.lock_abs_rel_conv: Error making absolute/relative converter: section = " + name + ", definition = " + abs_rel_conv_description, e);
				}
			}
		}

		// If there is no converter and there is a parent, inherit from parent

		if (abs_rel_conv == null && parent != null) {
			abs_rel_conv = parent.get_abs_rel_conv();
		}

		return;
	}




	//----- Line formatter -----


	// The line formatter for this section, or null if none.

	private RuptureLineFormatter line_formatter = null;


	// The line formatter description, or null if none.

	private String line_formatter_description = null;


	// True if non-null line_formatter can be overridden by a description.
	// If true, and there is a description at the time the section is locked,
	// then the description replaces the value of line_formatter.

	private boolean f_default_line_formatter = false;


	// Get the line formatter, or null if none.

	public final RuptureLineFormatter get_line_formatter () {
		return line_formatter;
	}


	// Get the line formatter description, or null if none.

	public final String get_line_formatter_description () {
		return line_formatter_description;
	}


	// Set the line formatter.
	// Parameters:
	//  the_line_formatter = Formatter object, can be null.
	//  f_describe = True to also write a description.
	//  f_default = True if formatter can be overridden by a description.
	// This can only be called if the section is not locked.
	// Note: f_describe and f_default should not both be true.
	// Note: If the line formatter holds an absolute/relative converter, it should
	// be the same one set by set_abs_rel_conv; and set_abs_rel_conv should be called
	// before this function.

	public final void set_line_formatter (RuptureLineFormatter the_line_formatter, boolean f_describe, boolean f_default) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.set_line_formatter: Section is already locked: section = " + name);
		}
		line_formatter = the_line_formatter;
		f_default_line_formatter = f_default;

		// If desired, write a description, or remove description if formatter is null

		if (f_describe) {
			if (line_formatter == null) {
				line_formatter_description = null;
			} else {
				line_formatter_description = RuptureLineFormatterFactory.describe_line_formatter (line_formatter);
			}
		}
		return;
	}


	// Set the line formatter description.
	// If non-null and non-empty, the description must be valid as a definition value.
	// This can only be called if the section is not locked.

	public final void set_line_formatter_description (String description) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.set_line_formatter_description: Section is already locked: section = " + name);
		}
		if (description == null || description.isEmpty()) {
			line_formatter_description = null;
		} else {
			if (!( is_valid_definition_value (description) )) {
				throw new IllegalArgumentException ("RuptureCatalogSection.set_line_formatter_description: Invalid line formatter description: section = " + name + ", description = " + description);
			}
			line_formatter_description = description;
		}
		return;
	}


	// Lock the line formatter.
	// The is called from lock_section to perform locking functions.
	// Note: lock_abs_rel_conv should be called before calling lock_line_formatter.

	private final void lock_line_formatter () {

		// If there is no formatter, or it can be overridden by a description ...

		if (line_formatter == null || f_default_line_formatter) {

			// If we have a description, use it

			if (line_formatter_description != null) {
				try {
					line_formatter = RuptureLineFormatterFactory.make_line_formatter (line_formatter_description, abs_rel_conv);
				} catch (Exception e) {
					throw new RuntimeException ("RuptureCatalogSection.lock_line_formatter: Error making line formatter: section = " + name + ", description = " + line_formatter_description, e);
				}
			}
		}

		// If there is no formatter and there is a parent, inherit from parent

		if (line_formatter == null && parent != null) {
			line_formatter = parent.get_line_formatter();
		}

		return;
	}




	//----- Comments -----


	// List of comments.

	private final ArrayList<String> comment_list = new ArrayList<String>();


	// Add a comment to the list of comments.

	public final void add_comment (String comment) {
		comment_list.add (comment);
		return;
	}


	// Get an iterator over the list of comments.

	public final Iterator<String> get_comment_iterator () {
		return comment_list.iterator();
	}


	// Erase all the comments.

	public final void clear_comments () {
		comment_list.clear();
		return;
	}


	// Add all comments from the collection to the list of comments.
	// If the argument is null, perform no operation.

	public final void add_all_comments (Collection<String> comments) {
		if (comments != null) {
			comment_list.addAll (comments);
		}
		return;
	}


	// Add all comments from the list of comments to the collection.
	// If the argument is null, perform no operation.

	public final void get_all_comments (Collection<String> comments) {
		if (comments != null) {
			comments.addAll (comment_list);
		}
		return;
	}




	//----- Ruptures -----


	// The maximum number of ruptures allowed in this section, or -1 for no limit.
	// Other likely values are 0 to prevent any ruptures from being added,
	// or 1 for a section that is intended to contain just a mainshock.

	private int max_ruptures = -1;


	// Get the maximum number of ruptures allowed in this section, or -1 for no limit.

	public final int get_max_ruptures () {
		return max_ruptures;
	}


	// Set the maximum number of ruptures allowed in this section, or -1 for no limit.
	// It is an error to call this function after the section is locked.

	public final RuptureCatalogSection set_max_ruptures (int the_max_ruptures) {
		if (f_locked) {
			throw new IllegalStateException ("RuptureCatalogSection.set_max_ruptures: Section is already locked: section = " + name);
		}
		max_ruptures = the_max_ruptures;
		return this;
	}


	// List of ruptures.

	private final ArrayList<RuptureFormatter> rupture_list = new ArrayList<RuptureFormatter>();


	// Add a rupture to the list of ruptures.
	// The supplied rf object is retained.
	// It is an error to call this function before the section is locked.

	public final void add_rupture (RuptureFormatter rf) {
		if (!( f_locked )) {
			throw new IllegalStateException ("RuptureCatalogSection.add_rupture: Section is not locked");
		}
		if (max_ruptures >= 0 && rupture_list.size() >= max_ruptures) {
			throw new IllegalStateException ("RuptureCatalogSection.add_rupture: Section already contains the maximum number of ruptures: max_ruptures = " + max_ruptures);
		}
		rupture_list.add (rf);
		return;
	}


	// Get the number of ruptures.

	public final int get_rupture_list_size () {
		return rupture_list.size();
	}


	// Get the n-th rupture.

	public final RuptureFormatter get_rupture (int n) {
		return rupture_list.get(n);
	}


	// Get an iterator over the list of ruptures.

	public final Iterator<RuptureFormatter> get_rupture_iterator () {
		return rupture_list.iterator();
	}




	//----- Convenience functions for ruptures -----


	// Add a rupture from an ObsEqkRupture object.
	// The supplied rup object is not retained.

	public final void add_eqk_rupture (ObsEqkRupture rup) {
		RuptureFormatter rf = new RuptureFormatter();
		rf.set_eqk_rupture (rup);
		add_rupture (rf);
		return;
	}


	// Get the n-th rupture as an ObsEqkRupture object.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.
	// The contained absolute/relative converter is used.

	public final ObsEqkRupture get_eqk_rupture (int n, boolean wrapLon) {
		ObsEqkRupture result;
		RuptureFormatter rf = get_rupture (n);
		try {
			result = rf.get_eqk_rupture (wrapLon, abs_rel_conv);
		} catch (Exception e) {
			String error_id = rf.get_error_id();
			if (error_id == null) {
				throw new RuntimeException ("RuptureCatalogSection.get_eqk_rupture: Conversion error reading rupture: n = " + n, e);
			}
			throw new RuntimeException ("RuptureCatalogSection.get_eqk_rupture: Conversion error reading rupture: n = " + n + ", id = " + error_id, e);
		}
		return result;
	}


	// Add all the ruptures from a list of ObsEqkRupture objects.
	// The supplied rup objects are not retained.

	public final void add_eqk_rupture_list (List<ObsEqkRupture> rup_list) {
		for (ObsEqkRupture rup : rup_list) {
			RuptureFormatter rf = new RuptureFormatter();
			rf.set_eqk_rupture (rup);
			add_rupture (rf);
		}
		return;
	}


	// Add all the ruptures to a list of ObsEqkRupture objects.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.
	// The contained absolute/relative converter is used.

	public final void get_eqk_rupture_list (List<ObsEqkRupture> rup_list, boolean wrapLon) {
		int n = 0;
		for (RuptureFormatter rf : rupture_list) {
			ObsEqkRupture result;
			try {
				result = rf.get_eqk_rupture (wrapLon, abs_rel_conv);
			} catch (Exception e) {
				String error_id = rf.get_error_id();
				if (error_id == null) {
					throw new RuntimeException ("RuptureCatalogSection.get_eqk_rupture_list: Conversion error reading rupture: n = " + n, e);
				}
				throw new RuntimeException ("RuptureCatalogSection.get_eqk_rupture_list: Conversion error reading rupture: n = " + n + ", id = " + error_id, e);
			}
			rup_list.add (result);
			++n;
		}
		return;
	}


	// Convert an ObsEqkRupture to a string, and then back to an ObsEqkRupture.
	// Parameters:
	//  rup = Rupture to convert.
	//  the_line_fmt = Line formatter used to format and then parse the string.
	//  wrapLon = True to produce longitude in the range 0 to +360, false for -180 to +180.
	//  the_abs_rel_conv = Converter used to convert relative to absolute coordinates
	//    when producing the final result (after parsing the string).  If no such
	//    conversion is needed (e.g., because the line formater produces absolute
	//    time and location), then the_abs_rel_conv is ignored and can be null.
	// This function can be used to determine how the rupture will appear if it is
	// written to a file and then read back in.

	public static ObsEqkRupture round_trip_eqk_rupture (ObsEqkRupture rup, RuptureLineFormatter the_line_fmt, boolean wrapLon, AbsRelTimeLocConverter the_abs_rel_conv) {
		RuptureFormatter rf_1 = new RuptureFormatter();
		rf_1.set_eqk_rupture (rup);
		String line = the_line_fmt.format_line (rf_1);
		RuptureFormatter rf_2 = new RuptureFormatter();
		the_line_fmt.parse_line (rf_2, line);
		ObsEqkRupture result = rf_2.get_eqk_rupture (wrapLon, the_abs_rel_conv);
		return result;
	}




	//----- Construction -----


	// Constructor specifies name and parent.

	public RuptureCatalogSection (RuptureCatalogSection the_parent, String the_name) {
		if (!( is_valid_section_name (the_name) )) {
			throw new IllegalArgumentException ("RuptureCatalogSection: Invalid section name: name = " + the_name);
		}

		parent = the_parent;
		name = the_name;
	}




	//----- Formatting -----


	// A class that can be used to hold section formatting.

	public static class SectionFormat {

		// String that is used to introduce a comment line.

		public String comment_prefix;

		// String that is used to introduce a control line (definition or section).

		public String control_prefix;

		// Format that is used (in String.format) to make a section line.
		// The first (and only) argument is the section name.

		public String section_fmt;

		// Pattern that is used to parse a section line.
		// Capture group 1 is the section name.

		public Pattern section_pattern;

		// Format that is used (in String.format) to make a definition line.
		// The first argument is the definition name, and the second argument is the definition value.

		public String definition_fmt;

		// Pattern that is used to parse a definition line.
		// Capture group 1 is the definition name, and capture group 2 is the definition value.

		public Pattern definition_pattern;

		// Format that is used (in String.format) to make a conversion line.
		// The first (and only) argument is the absolute/relative converter description.

		public String conversion_fmt;

		// Pattern that is used to parse a conversion line.
		// Capture group 1 is the absolute/relative converter description.

		public Pattern conversion_pattern;

		// Format that is used (in String.format) to make a format line.
		// The first (and only) argument is the line formatter description.

		public String format_fmt;

		// Pattern that is used to parse a format line.
		// Capture group 1 is the line formatter description.

		public Pattern format_pattern;



		// Constructor - Make an empty format.

		public SectionFormat () {
			comment_prefix = null;
			control_prefix = null;
			section_fmt = null;
			section_pattern = null;
			definition_fmt = null;
			definition_pattern = null;
			conversion_fmt = null;
			conversion_pattern = null;
			format_fmt = null;
			format_pattern = null;
		}


		// Constructor - Initialize all fields.

		public SectionFormat (
			String comment_prefix,
			String control_prefix,
			String section_fmt,
			Pattern section_pattern,
			String definition_fmt,
			Pattern definition_pattern,
			String conversion_fmt,
			Pattern conversion_pattern,
			String format_fmt,
			Pattern format_pattern
		) {
			this.comment_prefix = comment_prefix;
			this.control_prefix = control_prefix;
			this.section_fmt = section_fmt;
			this.section_pattern = section_pattern;
			this.definition_fmt = definition_fmt;
			this.definition_pattern = definition_pattern;
			this.conversion_fmt = conversion_fmt;
			this.conversion_pattern = conversion_pattern;
			this.format_fmt = format_fmt;
			this.format_pattern = format_pattern;
		}


		// Set all fields.

		public void set (
			String comment_prefix,
			String control_prefix,
			String section_fmt,
			Pattern section_pattern,
			String definition_fmt,
			Pattern definition_pattern,
			String conversion_fmt,
			Pattern conversion_pattern,
			String format_fmt,
			Pattern format_pattern
		) {
			this.comment_prefix = comment_prefix;
			this.control_prefix = control_prefix;
			this.section_fmt = section_fmt;
			this.section_pattern = section_pattern;
			this.definition_fmt = definition_fmt;
			this.definition_pattern = definition_pattern;
			this.conversion_fmt = conversion_fmt;
			this.conversion_pattern = conversion_pattern;
			this.format_fmt = format_fmt;
			this.format_pattern = format_pattern;
		}
	}




	// A common format.

	private static final SectionFormat common_format = new SectionFormat (
		"#",
		":",
		":begin: %s",
		Pattern.compile ("[ \\t]*:[ \\t]*begin[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*"),
		":define: %s = %s",
		Pattern.compile ("[ \\t]*:[ \\t]*define[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*=[ \\t]*(\\S(?:[^\\n\\r]*\\S)?)[ \\t]*"),
		":convert: %s",
		Pattern.compile ("[ \\t]*:[ \\t]*convert[ \\t]*:[ \\t]*(\\S(?:[^\\n\\r]*\\S)?)[ \\t]*"),
		":format: %s",
		Pattern.compile ("[ \\t]*:[ \\t]*format[ \\t]*:[ \\t]*(\\S(?:[^\\n\\r]*\\S)?)[ \\t]*")
	);

	public static SectionFormat get_common_format () {
		return common_format;
	}




	// Write the section.
	// Parameters:
	//  dest = Destination.
	//  fmt = Format.
	//  f_section_line = True to write the section control line.

	public void write_section (Consumer<String> dest, SectionFormat fmt, boolean f_section_line) {

		// Flag is true if we have not seen any ruptures yet

		boolean f_first_rupture = true;

		// Write the section line

		if (f_section_line) {
			dest.accept (String.format (fmt.section_fmt, get_name()));
		}

		// Write the comments

		for (Iterator<String> it = get_comment_iterator(); it.hasNext(); ) {
			dest.accept (fmt.comment_prefix + it.next());
		}

		// Write the conversion line

		if (get_abs_rel_conv_description() != null) {
			dest.accept (String.format (fmt.conversion_fmt, get_abs_rel_conv_description()));
		}

		// Write the format line

		if (get_line_formatter_description() != null) {
			dest.accept (String.format (fmt.format_fmt, get_line_formatter_description()));
		}

		// Write the definitions

		for (Iterator<Map.Entry<String, String>> it = get_definition_iterator(); it.hasNext(); ) {
			Map.Entry<String, String> entry = it.next();
			dest.accept (String.format (fmt.definition_fmt, entry.getKey(), entry.getValue()));
		}

		// Write the ruptures

		for (Iterator<RuptureFormatter> it = get_rupture_iterator(); it.hasNext(); ) {
			if (f_first_rupture) {
				if (line_formatter == null) {
					throw new IllegalStateException ("RuptureCatalogSection.write_section: No line formatter is available");
				}
				f_first_rupture = false;
			}
			dest.accept (line_formatter.format_line (it.next()));
		}

		return;
	}




	// Read the section.
	// Parameters:
	//  src = Source.
	//  fmt = Format.
	// Returns null if reached end of file, or the name of the next section in the file.
	// The function reads until it finds a section line with a different section name,
	// or until it reaches end of file.
	// Note: Sections in the file are open if they are splittable, which allows them to
	// be split over multiple blocks of lines.  So, this function can be used to continue
	// reading a partially-read section, and there is no check for completeness.
	// Note: The section is automatically locked when the first rupture is read.

	public String read_section (Supplier<String> src, SectionFormat fmt) {

		// If this section has been seen before, check if it is splittable

		if (f_input_seen) {
			if (!( f_input_splittable )) {
				throw new RuntimeException ("RuptureCatalogSection.read_section: Section has already been seen during input: section name = " + name);
			}
		}

		f_input_seen = true;

		// Flag is true if we have not seen any ruptures yet

		boolean f_first_rupture = true;

		// Read lines until end of file

		for (String raw_line = src.get(); raw_line != null; raw_line = src.get()) {
			String line = raw_line.trim();

			// Empty line, skip it

			if (line.isEmpty()) {
				continue;
			}

			// If a comment line, save the comment

			if (line.startsWith (fmt.comment_prefix)) {
				add_comment (line.substring (fmt.comment_prefix.length()));
				continue;
			}

			// If a control line ...

			if (line.startsWith (fmt.control_prefix)) {

				// Try to match a definition line

				Matcher matcher = fmt.definition_pattern.matcher (line);
				if (matcher.matches()) {

					// Get definition name and value

					String def_name = matcher.group(1);
					String def_value = matcher.group(2);

					// Check if this definition is allowed

					if (input_allowed_def_names != null) {
						if (!( input_allowed_def_names.contains (def_name) )) {
							throw new RuntimeException ("RuptureCatalogSection.read_section: Disallowed definition name in file: section name = " + name + ", line = " + line);
						}
					}

					// Add the definition

					add_definition (def_name, def_value);
					continue;
				}

				// Try to match a section line

				matcher = fmt.section_pattern.matcher (line);
				if (matcher.matches()) {

					// If it is for a different section, then return the section name

					String matched_name = matcher.group(1);
					if (!( matched_name.equals(name) )) {
						return matched_name;
					}
					continue;
				}

				// Try to match a conversion line

				matcher = fmt.conversion_pattern.matcher (line);
				if (matcher.matches()) {

					// Check if conversion line is allowed

					if (!( f_input_abs_rel_conv )) {
						throw new RuntimeException ("RuptureCatalogSection.read_section: Disallowed conversion description in file: section name = " + name + ", line = " + line);
					}

					// Save the converter description

					set_abs_rel_conv_description (matcher.group(1));
					continue;
				}

				// Try to match a format line

				matcher = fmt.format_pattern.matcher (line);
				if (matcher.matches()) {

					// Check if format line is allowed

					if (!( f_input_line_formatter )) {
						throw new RuntimeException ("RuptureCatalogSection.read_section: Disallowed format description in file: section name = " + name + ", line = " + line);
					}

					// Save the formatter description

					set_line_formatter_description (matcher.group(1));
					continue;
				}

				// Otherwise, an invalid control line

				throw new RuntimeException ("RuptureCatalogSection.read_section: Invalid control line in file: line = " + line);
			}

			// Otherwise, it is a rupture line

			if (f_first_rupture) {
				lock_section();
				if (line_formatter == null) {
					throw new IllegalStateException ("RuptureCatalogSection.write_section: No line formatter is available");
				}
				f_first_rupture = false;
			}

			RuptureFormatter rf = new RuptureFormatter();
			line_formatter.parse_line (rf, line);
			add_rupture (rf);
		}

		// Reached end of file

		return null;
	}

}
