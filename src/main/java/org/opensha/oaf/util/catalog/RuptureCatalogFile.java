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

import java.io.StringReader;
import java.io.StringWriter;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.LineConsumerFile;
import org.opensha.oaf.util.LineSupplierFile;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.oetas.OEOrigin;

//import static org.opensha.oaf.util.catalog.RuptureCatalogSection.SECTION_NAME_ROOT;
//import static org.opensha.oaf.util.catalog.RuptureCatalogSection.SECTION_NAME_DEFAULT;
//import static org.opensha.oaf.util.catalog.RuptureCatalogSection.SECTION_NAME_MAINSHOCK;
//import static org.opensha.oaf.util.catalog.RuptureCatalogSection.SECTION_NAME_AFTERSHOCK;


// A catalog file, composed from a sequence of sections.
// Author: Michael Barall 10/18/2021.

public class RuptureCatalogFile {


	//----- Sections -----


	// Index of the root section.

	//private static final int ROOT_SECTION_INDEX = 0;

	// Index of the default section.

	//private static final int DEFAULT_SECTION_INDEX = 1;


	// The list of sections.

	private final ArrayList<RuptureCatalogSection> section_list = new ArrayList<RuptureCatalogSection>();

	// Map of section names to section indexes.

	private final HashMap<String, Integer> section_names = new HashMap<String, Integer>();

	// True if sections can be automatically created when reading a file.

	private boolean f_auto_section = false;

	// True if sections created automatically can be split during input.

	private boolean f_auto_input_splittable = false;

	// List of definition names allowed during input of automatically created sections, or null if no restriction.

	private ArrayList<String> auto_input_allowed_def_names = null;

	// True if absolute/relative converter is allowed during input of automatically created sections.

	private boolean f_auto_input_abs_rel_conv = true;

	// True if line formatter is allowed during input of automatically created sections.

	private boolean f_auto_input_line_formatter = true;

	// True if section line is written for default section.

	private boolean f_default_section_line = false;

	// Format for writing sections.

	private RuptureCatalogSection.SectionFormat section_format = null;

	// Name of the next section to read, or null if end of file.
	// Initial value is the default section, which applies at file scope.

	private String next_input_section = RuptureCatalogSection.SECTION_NAME_DEFAULT;


	// Make a new section.
	// Parameters:
	//  parent_name = Name of the parent section, or null if no parent.
	//  section_name = Name of the section to create.
	// Returns the new section.

	public final RuptureCatalogSection make_section (String parent_name, String section_name) {

		// If parent name is not-null, retrieve the parent section, exception if error

		RuptureCatalogSection the_parent = null;
		if (parent_name != null) {
			if (!( RuptureCatalogSection.is_valid_section_name (parent_name) )) {
				throw new IllegalArgumentException ("RuptureCatalogFile.make_section: Invalid parent name, name = " + parent_name);
			}
			Integer parent_index = section_names.get (parent_name);
			if (parent_index == null) {
				throw new IllegalArgumentException ("RuptureCatalogFile.make_section: Parent not found, name = " + parent_name);
			}
			the_parent = section_list.get (parent_index);
		}

		// Check the section name, and that it is not already used

		if (!( RuptureCatalogSection.is_valid_section_name (section_name) )) {
			throw new IllegalArgumentException ("RuptureCatalogFile.make_section: Invalid section name, name = " + section_name);
		}
		if (section_names.containsKey (section_name)) {
			throw new IllegalArgumentException ("RuptureCatalogFile.make_section: Section already exists, name = " + section_name);
		}

		// Make the section and add to our list

		section_names.put (section_name, section_list.size());
		RuptureCatalogSection rcs = new RuptureCatalogSection (the_parent, section_name);
		section_list.add (rcs);

		return rcs;
	}


	// Make the initial sections.
	// Creates the root section and the default section.

	private final void make_initial_sections () {
		make_section (null, RuptureCatalogSection.SECTION_NAME_ROOT);
		make_section (RuptureCatalogSection.SECTION_NAME_ROOT, RuptureCatalogSection.SECTION_NAME_DEFAULT);
		return;
	}


	// Add a new section underneath the default section.
	// Returns the new section.

	public final RuptureCatalogSection add_section (String section_name) {
		return make_section (RuptureCatalogSection.SECTION_NAME_DEFAULT, section_name);
	}


	// Set flag indicating if sections can be automatically created when reading a file.
	// Default is false.

	public final RuptureCatalogFile set_auto_section (boolean the_f_auto_section) {
		f_auto_section = the_f_auto_section;
		return this;
	}


	// Set flag indicating if sections created automatically can be split during input.
	// Default is false.

	public final RuptureCatalogFile set_auto_input_splittable (boolean the_f_auto_input_splittable) {
		f_auto_input_splittable = the_f_auto_input_splittable;
		return this;
	}


	// Set the definition names that are allowed during input of automatically created sections, or null if no restriction.
	// Default is that all names are allowed.
	// The names can be supplied as a collection, as an array, or as arguments to the call.
	// Note: Calling with no arguments disallows all definitions during input of automatically created sections.

	public final RuptureCatalogFile set_allowed_def_names (Collection<String> allowed_def_names) {
		if (allowed_def_names == null) {
			auto_input_allowed_def_names = null;
		}
		else {
			auto_input_allowed_def_names = new ArrayList<String> (allowed_def_names);
		}
		return this;
	}


	public final RuptureCatalogFile set_allowed_def_names (String... allowed_def_names) {
		if (allowed_def_names == null) {
			auto_input_allowed_def_names = null;
		}
		else {
			auto_input_allowed_def_names = new ArrayList<String> ();
			for (String s : allowed_def_names) {
				auto_input_allowed_def_names.add (s);
			}
		}
		return this;
	}


	// Set flag indicating if absolute/relative converter is allowed during input of automatically created sections.
	// Default is true.

	public final RuptureCatalogFile set_auto_input_abs_rel_conv (boolean f_allowed) {
		f_auto_input_abs_rel_conv = f_allowed;
		return this;
	}


	// Set flag indicating if line formatter is allowed during input of automatically created sections.
	// Default is true.

	public final RuptureCatalogFile set_auto_input_line_formatter (boolean f_allowed) {
		f_auto_input_line_formatter = f_allowed;
		return this;
	}


	// Set flag indicating if section line is written for default section.

	public final RuptureCatalogFile set_default_section_line (boolean the_f_default_section_line) {
		f_default_section_line = the_f_default_section_line;
		return this;
	}


	// Set the section format.

	public final RuptureCatalogFile set_section_format (RuptureCatalogSection.SectionFormat the_section_format) {
		section_format = the_section_format;
		return this;
	}


	// Get the named section.
	// Return null if section does not exist.

	public final RuptureCatalogSection get_section (String section_name) {
		Integer section_index = section_names.get (section_name);
		if (section_index == null) {
			return null;
		}
		return section_list.get (section_index);
	}


	// Get the named section.
	// Throw exception if section does not exist.

	public final RuptureCatalogSection req_section (String section_name) {
		Integer section_index = section_names.get (section_name);
		if (section_index == null) {
			throw new IllegalArgumentException ("RuptureCatalogFile.req_section: Section not found, name = " + section_name);
		}
		return section_list.get (section_index);
	}


	// Get the named section.
	// If section does not exist and f_auto is true, add a new section under the default section.
	// If section does not exist and f_auto is false, throw exception.

	public final RuptureCatalogSection get_or_add_section (String section_name, boolean f_auto) {
		Integer section_index = section_names.get (section_name);
		if (section_index == null) {
			if (f_auto) {
				return add_section (section_name);
			}
			throw new IllegalArgumentException ("RuptureCatalogFile.get_or_add_section: Section not found and section creation is not allowed, name = " + section_name);
		}
		return section_list.get (section_index);
	}


	// Return true if the named section exists.

	public final boolean contains_section (String section_name) {
		return section_names.containsKey (section_name);
	}


	// Get the root section.
	// This always returns non-null.

	public final RuptureCatalogSection get_root_section () {
		return get_section (RuptureCatalogSection.SECTION_NAME_ROOT);
	}


	// Get the default section.
	// This always returns non-null.

	public final RuptureCatalogSection get_default_section () {
		return get_section (RuptureCatalogSection.SECTION_NAME_DEFAULT);
	}


	// Get an iterator over the list of sections.
	// Sections are iterated in the same order that they were added, with root first and default second.

	public final Iterator<RuptureCatalogSection> get_section_iterator () {
		return section_list.iterator();
	}


	// Get the number of sections.

	public final int get_section_list_size () {
		return section_list.size();
	}


	// Get the section given an index number.
	
	public final RuptureCatalogSection get_section_by_index (int n) {
		return section_list.get (n);
	}




	//----- Construction -----


	// Constructor creates the rood and default sections, and sets a default format.

	public RuptureCatalogFile () {

		// Make the initial sections

		make_initial_sections();

		// Establish a default section format

		set_section_format (RuptureCatalogSection.get_common_format());
	}




	//----- I/O -----




	// Write all sections.
	// Parameters:
	//  dest = Destination.

	public void write_all_sections (Consumer<String> dest) {

		// Write all sections

		for (Iterator<RuptureCatalogSection> it = get_section_iterator(); it.hasNext(); ) {
			RuptureCatalogSection rcs = it.next();

			// Don't write the root section

			if (rcs.is_root_section()) {
				continue;
			}

			// Flag indicates if we should write the section line
			// (Always write it, unless it is the default section)

			boolean f_section_line = true;
			if (rcs.is_default_section()) {
				f_section_line = f_default_section_line;
			}

			// Write the section

			rcs.write_section (dest, section_format, f_section_line);
		}

		return;
	}




	// Read the next section.
	// Parameters:
	//  src = Source.
	// Returns the name of the section that was just read.
	// Returns null if end of file.
	// Note: If this is the first read call, then this function reads the default section.
	// Note: Be aware that the section read may be a continuation of a
	// previously-read section, unless the section is marked as non-splittable.
	// Note: Performs no operation if already at end of file.

	public String read_next_section (Supplier<String> src) {

		// Read one section, if not end of file

		String result = next_input_section;

		if (next_input_section != null) {
			//RuptureCatalogSection rcs = get_or_add_section (next_input_section, f_auto_section);
			RuptureCatalogSection rcs = get_section (next_input_section);
			if (rcs == null) {
				if (f_auto_section) {
					rcs = add_section (next_input_section);
					rcs.set_input_splittable (f_auto_input_splittable);
					rcs.set_allowed_def_names (auto_input_allowed_def_names);
					rcs.set_input_abs_rel_conv (f_auto_input_abs_rel_conv);
					rcs.set_input_line_formatter (f_auto_input_line_formatter);
				} else {
					throw new RuntimeException ("RuptureCatalogFile.read_next_section: Section not found and section creation is not allowed, section name = " + next_input_section);
				}
			}
			next_input_section = rcs.read_section (src, section_format);
		}

		// Return the section name we just read

		return result;
	}




	// Get the name of the next section that will be read by read_next_section, or null if at end of file.
	// Note: The name returned may refer to a section that does not exist.

	public String get_next_input_section () {
		return next_input_section;
	}




	// Return true if there is a section remaining to read.

	public boolean has_next_input_section () {
		return next_input_section != null;
	}




	// Read all sections.
	// Parameters:
	//  src = Source.
	// Note: If this is called after read_next_section, it reads all remaining sections.

	public void read_all_sections (Supplier<String> src) {

		// Read sections until end of file

		while (has_next_input_section()) {
			read_next_section (src);
		}

		// Reached end of file

		return;
	}




	//----- Convenience functions -----




	// Set up a mainshock section.
	// Add a mainshock section under the default section, and set it to have a
	// maximum of 1 rupture, not splittable, with no definitons, converter, or formatter.
	// Does not lock the section.
	// Returns the section that was created.

	public RuptureCatalogSection setup_mainshock_section () {
		RuptureCatalogSection rcs = add_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
		rcs.set_max_ruptures (1);
		rcs.set_input_splittable (false);
		rcs.set_allowed_def_names (new ArrayList<String>());
		rcs.set_input_abs_rel_conv (false);
		rcs.set_input_line_formatter (false);
		return rcs;
	}


	// Get the mainshock section.
	// Throws an exception if the mainshock section does not exist.

	public RuptureCatalogSection req_mainshock_section () {
		return req_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
	}




	// Set up an aftershock section.
	// Add an aftershock section under the default section, and set it to be
	// not splittable, with no definitons, converter, or formatter.
	// Does not lock the section.
	// Returns the section that was created.

	public RuptureCatalogSection setup_aftershock_section () {
		RuptureCatalogSection rcs = add_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
		rcs.set_input_splittable (false);
		rcs.set_allowed_def_names (new ArrayList<String>());
		rcs.set_input_abs_rel_conv (false);
		rcs.set_input_line_formatter (false);
		return rcs;
	}


	// Get the aftershock section.
	// Throws an exception if the aftershock section does not exist.

	public RuptureCatalogSection req_aftershock_section () {
		return req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("RuptureCatalogFile : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  mainshock_event_id  [aftershock_event_id]...
		// Read the mainshock from Comcat, and display it.
		// Read the aftershock list from Comcat, and display it.
		// Store into a RuptureCatalogFile object.
		// Write to a string with GUI legacy line format, and display it.
		// Display the string.
		// Read into a new RuptureCatalogFile object.
		// Obtain mainshock and aftershock list, and display them.
		// Write back out into a new string, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 or more additional arguments

			if (!( args.length >= 2 )) {
				System.err.println ("RuptureCatalogFile : Invalid 'test1' subcommand");
				return;
			}

			try {

				String mainshock_event_id = args[1];
				int num_aftershock = args.length - 2;
				String[] aftershock_event_ids = new String[num_aftershock];
				for (int i = 0; i < num_aftershock; ++i) {
					aftershock_event_ids[i] = args[i + 2];
				}

				// Say hello

				System.out.println ("Testing file write and read with GUI legacy format.");
				System.out.println ("mainshock_event_id = " + mainshock_event_id);
				System.out.println ("aftershock_event_ids = " + String.join (" ", aftershock_event_ids));

				// Load mainshock from Comcat

				System.out.println();
				System.out.println("***** Mainshock");

				boolean wrapLon = false;
				boolean extendedInfo = true;

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture mainshock_rup = accessor.fetchEvent (mainshock_event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (mainshock_rup));

				// Make the origin object

				OEOrigin origin = (new OEOrigin()).set_from_rupture_horz(mainshock_rup);

				System.out.println();
				System.out.println(origin.toString());

				// Make a line formatter.

				RuptureLineFormatGUILegacy line_fmt = (new RuptureLineFormatGUILegacy()).set_abs_rel_conv(origin);

				System.out.println();
				System.out.println(line_fmt.show_format_info());

				// Load aftershocks from Comcat

				System.out.println();
				System.out.println("***** Aftershocks");

				ArrayList<ObsEqkRupture> aftershock_list = new ArrayList<ObsEqkRupture>();

				for (String id : aftershock_event_ids) {
					aftershock_list.add (accessor.fetchEvent (id, wrapLon, extendedInfo));
				}

				for (ObsEqkRupture rup : aftershock_list) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Set up the catalog file

				RuptureCatalogFile rcf = new RuptureCatalogFile();

				RuptureCatalogSection rcs = rcf.get_root_section();
				rcs.add_comment (" Comment for root section.");
				rcs.add_definition_string ("root_var1", "Value should not appear");
				rcs.add_definition_double ("root_var2", 3.14159);
				rcs.lock_section();

				rcs = rcf.get_default_section();
				rcs.add_comment (" Comment for default section.");
				rcs.add_definition_string ("def_var1", "Value should appear");
				rcs.add_definition_double ("def_var2", 2.71828);
				rcs.set_abs_rel_conv (origin, true, false);
				rcs.set_line_formatter (line_fmt, true, false);
				rcs.lock_section();

				rcs = rcf.add_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				rcs.set_max_ruptures(1);
				rcs.lock_section();
				rcs.add_eqk_rupture (mainshock_rup);

				rcs = rcf.add_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
				rcs.add_comment (" Comment for aftershock section.");
				rcs.add_comment (" Another comment for aftershock section.");
				rcs.add_definition_string ("as_var1", "Values for aftershock");
				rcs.add_definition_double ("as_var2", 1.4142);
				rcs.lock_section();
				rcs.add_eqk_rupture_list (aftershock_list);

				// Read definitions from default section

				System.out.println();
				System.out.println("***** Definitions From Default Section");

				rcs = rcf.get_default_section();

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs.get_definition_double ("no_var2", -1000.0));

				// Read definitions from aftershock section

				System.out.println();
				System.out.println("***** Definitions From Aftershock Section");

				rcs = rcf.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs.get_definition_double ("no_var2", -1000.0));

				// Write the file to a string

				StringWriter sw = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw);
				){
					try {
						rcf.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text = sw.toString();

				System.out.println();
				System.out.println("***** File Text");

				System.out.println();
				System.out.println(file_text);

				// Make a new file for input

				rcf = null;
				rcs = null;
				sw = null;

				RuptureCatalogFile rcf_2 = new RuptureCatalogFile();
				rcf_2.set_auto_section (true);

				RuptureCatalogSection rcs_2 = rcf_2.add_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				rcs_2.set_max_ruptures(1);

				// Read the file from a string

				try (
					LineSupplierFile lsf = new LineSupplierFile (new StringReader (file_text));
				){
					try {
						rcf_2.read_all_sections (lsf);
					} catch (Exception e) {
						throw new RuntimeException ("Error reading file: " + lsf.error_locus(), e);
					}
				}

				file_text = null;

				// Read the mainshock

				System.out.println();
				System.out.println("***** Mainshock Read-In");

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				ObsEqkRupture mainshock_rup_2 = rcs_2.get_eqk_rupture (0, wrapLon);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (mainshock_rup_2));

				// Read the aftershocks

				System.out.println();
				System.out.println("***** Aftershocks Read-In");

				ArrayList<ObsEqkRupture> aftershock_list_2 = new ArrayList<ObsEqkRupture>();

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
				rcs_2.get_eqk_rupture_list (aftershock_list_2, wrapLon);

				for (ObsEqkRupture rup : aftershock_list_2) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Read definitions from default section

				System.out.println();
				System.out.println("***** Definitions From Default Section, Re-Read");

				rcs_2 = rcf_2.get_default_section();

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs_2.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs_2.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs_2.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs_2.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs_2.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs_2.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs_2.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs_2.get_definition_double ("no_var2", -1000.0));

				// Read definitions from aftershock section

				System.out.println();
				System.out.println("***** Definitions From Aftershock Section, Re-Read");

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs_2.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs_2.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs_2.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs_2.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs_2.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs_2.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs_2.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs_2.get_definition_double ("no_var2", -1000.0));

				// Write the file to a string

				StringWriter sw_2 = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw_2);
				){
					try {
						rcf_2.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text_2 = sw_2.toString();

				System.out.println();
				System.out.println("***** File Text Re-Written");

				System.out.println();
				System.out.println(file_text_2);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  mainshock_event_id  [aftershock_event_id]...
		// Read the mainshock from Comcat, and display it.
		// Read the aftershock list from Comcat, and display it.
		// Store into a RuptureCatalogFile object.
		// Write to a string with GUI observed line format, and display it.
		// Display the string.
		// Read into a new RuptureCatalogFile object.
		// Obtain mainshock and aftershock list, and display them.
		// Write back out into a new string, and display it.
		// Note: Same as test #1 except using the GUI observed format.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 1 or more additional arguments

			if (!( args.length >= 2 )) {
				System.err.println ("RuptureCatalogFile : Invalid 'test2' subcommand");
				return;
			}

			try {

				String mainshock_event_id = args[1];
				int num_aftershock = args.length - 2;
				String[] aftershock_event_ids = new String[num_aftershock];
				for (int i = 0; i < num_aftershock; ++i) {
					aftershock_event_ids[i] = args[i + 2];
				}

				// Say hello

				System.out.println ("Testing file write and read with GUI observed format.");
				System.out.println ("mainshock_event_id = " + mainshock_event_id);
				System.out.println ("aftershock_event_ids = " + String.join (" ", aftershock_event_ids));

				// Load mainshock from Comcat

				System.out.println();
				System.out.println("***** Mainshock");

				boolean wrapLon = false;
				boolean extendedInfo = true;

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture mainshock_rup = accessor.fetchEvent (mainshock_event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (mainshock_rup));

				// Make the origin object

				OEOrigin origin = (new OEOrigin()).set_from_rupture_horz(mainshock_rup);

				System.out.println();
				System.out.println(origin.toString());

				// Make a line formatter.

				RuptureLineFormatGUIObserved line_fmt = (new RuptureLineFormatGUIObserved()).set_abs_rel_conv(origin);

				System.out.println();
				System.out.println(line_fmt.show_format_info());

				// Load aftershocks from Comcat

				System.out.println();
				System.out.println("***** Aftershocks");

				ArrayList<ObsEqkRupture> aftershock_list = new ArrayList<ObsEqkRupture>();

				for (String id : aftershock_event_ids) {
					aftershock_list.add (accessor.fetchEvent (id, wrapLon, extendedInfo));
				}

				for (ObsEqkRupture rup : aftershock_list) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Set up the catalog file

				RuptureCatalogFile rcf = new RuptureCatalogFile();

				RuptureCatalogSection rcs = rcf.get_root_section();
				rcs.add_comment (" Comment for root section.");
				rcs.add_definition_string ("root_var1", "Value should not appear");
				rcs.add_definition_double ("root_var2", 3.14159);
				rcs.lock_section();

				rcs = rcf.get_default_section();
				rcs.add_comment (" Comment for default section.");
				rcs.add_definition_string ("def_var1", "Value should appear");
				rcs.add_definition_double ("def_var2", 2.71828);
				rcs.set_abs_rel_conv (origin, true, false);
				rcs.set_line_formatter (line_fmt, true, false);
				rcs.lock_section();

				rcs = rcf.add_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				rcs.set_max_ruptures(1);
				rcs.lock_section();
				rcs.add_eqk_rupture (mainshock_rup);

				rcs = rcf.add_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
				rcs.add_comment (" Comment for aftershock section.");
				rcs.add_comment (" Another comment for aftershock section.");
				rcs.add_definition_string ("as_var1", "Values for aftershock");
				rcs.add_definition_double ("as_var2", 1.4142);
				rcs.lock_section();
				rcs.add_eqk_rupture_list (aftershock_list);

				// Read definitions from default section

				System.out.println();
				System.out.println("***** Definitions From Default Section");

				rcs = rcf.get_default_section();

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs.get_definition_double ("no_var2", -1000.0));

				// Read definitions from aftershock section

				System.out.println();
				System.out.println("***** Definitions From Aftershock Section");

				rcs = rcf.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs.get_definition_double ("no_var2", -1000.0));

				// Write the file to a string

				StringWriter sw = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw);
				){
					try {
						rcf.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text = sw.toString();

				System.out.println();
				System.out.println("***** File Text");

				System.out.println();
				System.out.println(file_text);

				// Make a new file for input

				rcf = null;
				rcs = null;
				sw = null;

				RuptureCatalogFile rcf_2 = new RuptureCatalogFile();
				rcf_2.set_auto_section (true);

				RuptureCatalogSection rcs_2 = rcf_2.add_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				rcs_2.set_max_ruptures(1);

				// Read the file from a string

				try (
					LineSupplierFile lsf = new LineSupplierFile (new StringReader (file_text));
				){
					try {
						rcf_2.read_all_sections (lsf);
					} catch (Exception e) {
						throw new RuntimeException ("Error reading file: " + lsf.error_locus(), e);
					}
				}

				file_text = null;

				// Read the mainshock

				System.out.println();
				System.out.println("***** Mainshock Read-In");

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_MAINSHOCK);
				ObsEqkRupture mainshock_rup_2 = rcs_2.get_eqk_rupture (0, wrapLon);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (mainshock_rup_2));

				// Read the aftershocks

				System.out.println();
				System.out.println("***** Aftershocks Read-In");

				ArrayList<ObsEqkRupture> aftershock_list_2 = new ArrayList<ObsEqkRupture>();

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);
				rcs_2.get_eqk_rupture_list (aftershock_list_2, wrapLon);

				for (ObsEqkRupture rup : aftershock_list_2) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Read definitions from default section

				System.out.println();
				System.out.println("***** Definitions From Default Section, Re-Read");

				rcs_2 = rcf_2.get_default_section();

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs_2.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs_2.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs_2.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs_2.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs_2.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs_2.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs_2.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs_2.get_definition_double ("no_var2", -1000.0));

				// Read definitions from aftershock section

				System.out.println();
				System.out.println("***** Definitions From Aftershock Section, Re-Read");

				rcs_2 = rcf_2.req_section (RuptureCatalogSection.SECTION_NAME_AFTERSHOCK);

				System.out.println();
				System.out.println("root_var1" + " --> " + rcs_2.get_definition_string ("root_var1", "not found"));
				System.out.println("root_var2" + " --> " + rcs_2.get_definition_double ("root_var2", -1000.0));
				System.out.println("def_var1" + " --> " + rcs_2.get_definition_string ("def_var1", "not found"));
				System.out.println("def_var2" + " --> " + rcs_2.get_definition_double ("def_var2", -1000.0));
				System.out.println("as_var1" + " --> " + rcs_2.get_definition_string ("as_var1", "not found"));
				System.out.println("as_var2" + " --> " + rcs_2.get_definition_double ("as_var2", -1000.0));
				System.out.println("no_var1" + " --> " + rcs_2.get_definition_string ("no_var1", "not found"));
				System.out.println("no_var2" + " --> " + rcs_2.get_definition_double ("no_var2", -1000.0));

				// Write the file to a string

				StringWriter sw_2 = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw_2);
				){
					try {
						rcf_2.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text_2 = sw_2.toString();

				System.out.println();
				System.out.println("***** File Text Re-Written");

				System.out.println();
				System.out.println(file_text_2);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  earthquake_event_id...
		// Read the earthquake list from Comcat, and display it.
		// Store into a RuptureCatalogFile object.
		// Write to a string with local catalog line format, and display it.
		// Display the string.
		// Read into a new RuptureCatalogFile object.
		// Obtain earthquake list, and display them.
		// Write back out into a new string, and display it.
		// Note: Unlike tests #1 and #2, all earthquakes are stored in the
		// default section, and there are no comments or definitions, and no origin.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 1 or more additional arguments

			if (!( args.length >= 2 )) {
				System.err.println ("RuptureCatalogFile : Invalid 'test3' subcommand");
				return;
			}

			try {

				int num_earthquake = args.length - 1;
				String[] earthquake_event_ids = new String[num_earthquake];
				for (int i = 0; i < num_earthquake; ++i) {
					earthquake_event_ids[i] = args[i + 1];
				}

				// Say hello

				System.out.println ("Testing file write and read with local catalog format.");
				System.out.println ("earthquake_event_ids = " + String.join (" ", earthquake_event_ids));

				// Preliminaries

				System.out.println();
				System.out.println("***** Preliminaries");

				boolean wrapLon = false;
				boolean extendedInfo = true;

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Make a line formatter.

				RuptureLineFormatLocalCatalog line_fmt = new RuptureLineFormatLocalCatalog();

				System.out.println();
				System.out.println(line_fmt.show_format_info());

				// Load earthquakes from Comcat

				System.out.println();
				System.out.println("***** Earthquakes");

				ArrayList<ObsEqkRupture> earthquake_list = new ArrayList<ObsEqkRupture>();

				for (String id : earthquake_event_ids) {
					earthquake_list.add (accessor.fetchEvent (id, wrapLon, extendedInfo));
				}

				for (ObsEqkRupture rup : earthquake_list) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Set up the catalog file

				RuptureCatalogFile rcf = new RuptureCatalogFile();

				RuptureCatalogSection rcs = rcf.get_root_section();
				rcs.lock_section();

				rcs = rcf.get_default_section();
				rcs.set_line_formatter (line_fmt, false, false);
				rcs.lock_section();
				rcs.add_eqk_rupture_list (earthquake_list);

				// Write the file to a string

				StringWriter sw = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw);
				){
					try {
						rcf.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text = sw.toString();

				System.out.println();
				System.out.println("***** File Text");

				System.out.println();
				System.out.println(file_text);

				// Make a new file for input

				rcf = null;
				rcs = null;
				sw = null;

				RuptureCatalogFile rcf_2 = new RuptureCatalogFile();
				rcf_2.set_auto_section (false);

				RuptureCatalogSection rcs_2 = rcf_2.get_default_section ();
				rcs_2.set_line_formatter (line_fmt, false, false);
				rcs_2.lock_section();

				// Read the file from a string

				try (
					LineSupplierFile lsf = new LineSupplierFile (new StringReader (file_text));
				){
					try {
						rcf_2.read_all_sections (lsf);
					} catch (Exception e) {
						throw new RuntimeException ("Error reading file: " + lsf.error_locus(), e);
					}
				}

				file_text = null;

				// Read the earthquakes

				System.out.println();
				System.out.println("***** Earthquakes Read-In");

				ArrayList<ObsEqkRupture> earthquake_list_2 = new ArrayList<ObsEqkRupture>();

				rcs_2 = rcf_2.get_default_section ();
				rcs_2.get_eqk_rupture_list (earthquake_list_2, wrapLon);

				for (ObsEqkRupture rup : earthquake_list_2) {
					System.out.println();
					System.out.println(ComcatOAFAccessor.rupToString (rup));
				}

				// Write the file to a string

				StringWriter sw_2 = new StringWriter();
				try (
					LineConsumerFile lcf = new LineConsumerFile (sw_2);
				){
					try {
						rcf_2.write_all_sections (lcf);
					} catch (Exception e) {
						throw new RuntimeException ("Error writing file: " + lcf.error_locus(), e);
					}
				}

				// Show the String

				String file_text_2 = sw_2.toString();

				System.out.println();
				System.out.println("***** File Text Re-Written");

				System.out.println();
				System.out.println(file_text_2);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("RuptureCatalogFile : Unrecognized subcommand : " + args[0]);
		return;

	}
}
