package org.opensha.oaf.util.catalog;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	// True if section line is written for default section.

	private boolean f_default_section_line = false;

	// Format for writing sections.

	private RuptureCatalogSection.SectionFormat section_format = null;


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

	public final RuptureCatalogFile set_auto_section (boolean the_f_auto_section) {
		f_auto_section = the_f_auto_section;
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




	// Read all sections.
	// Parameters:
	//  src = Source.

	public String read_all_sections (Supplier<String> src) {

		// Read sections until end of file, starting with the default section

		String section_name = RuptureCatalogSection.SECTION_NAME_DEFAULT;

		while (section_name != null) {
			RuptureCatalogSection rcs = get_or_add_section (section_name, f_auto_section);
			section_name = rcs.read_section (src, section_format);
		}

		// Reached end of file

		return null;
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
					rcf.write_all_sections (lcf);
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
					rcf_2.read_all_sections (lsf);
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
					rcf_2.write_all_sections (lcf);
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
