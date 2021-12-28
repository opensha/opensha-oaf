package org.opensha.oaf.pdl;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;




/**
 * Used to build a contents.xml file to include in a PDL product.
 * Author: Michael Barall 12/13/2021.
 *
 * This class can also, optionally, store the contents of all the files.
 *
 * In order for files to appear in the download section of the event page,
 * you have to supply a contents.xml file, with MIME type application/xml.
 * The download section of the event page is divided into subsections.
 * Each subsection has a title, an optional subtitle, and a list of downloadable files.
 * The contents.xml file describes these subsections.  Here is an example:
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <contents>
 *   <file title="Download subsection 1" id="firstSubsection">
 *     <caption><![CDATA[Subtitle for subsection 1]]></caption>
 *     <format href="file_1.txt" type="text/plain" />
 *     <format href="file_2.xml" type="application/xml" />
 *     <format href="file_3.json" type="application/json" />
 *   </file>
 *   <file title="Download subsection 2" id="secondSubsection">
 *     <caption><![CDATA[Subtitle for subsection 2]]></caption>
 *     <format href="image_1.jpg" type="image/jpeg" />
 *     <format href="image_2.png" type="image/png" />
 *   </file>
 * </contents>
 *
 * The first line is the required XML file header.
 * The remainder of the file consists of a single <contents>...</contents> item.
 * Each subsection is described by a <file>...</file> item.
 * The title= attribute gives the subsection title.
 * The id= attribute gives a unique identifier for the subsection, which should
 * be an alphanumeric string beginning with a lowercase letter.  (The purpose of
 * the id= attribute is unclear.)
 * If a subtitle is desired, it is included in a <caption>...</caption> item;
 * otherwise <caption>...</caption> should be omitted.  The actual text of the
 * subtitle is contained in a <![CDATA[...]]> construct (which in XML is a way
 * to insert inline text that is not itself parsed as XML).
 * Each file in the subsection is described in a <format... /> item.
 * The href= attribute gives the PDL filename.
 * The type= attribute gives the MIME type.
 *
 * The contents.xml file can contain comments like this:
 * <!-- This is a comment -->
 * Blank lines are also permitted (except that there may not be a blank line
 * or a comment before the <?xml...?> header).
 *
 */
public class PDLContentsXmlBuilder {

	// Nested class to hold information about one file.

	private static class file_info {

		// The filename, cannot be null or blank.

		public String filename;

		// The mime type, cannot be null or blank.

		public String mime_type;

		// The file contents, or null if not supplied.

		public PDLProductFile file_contents;

		// Constructor, given the filename and mime type.

		public file_info (String filename, String mime_type) {
			if (filename == null || filename.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.file_info.file_info: Filename is not specified");
			}
			if (mime_type == null || mime_type.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.file_info.file_info: MIME type is not specified");
			}
			this.filename = filename;
			this.mime_type = mime_type;
			this.file_contents = null;
			return;
		}

		// Constructor, given the filename, mime type, and contents.

		public file_info (String filename, String mime_type, PDLProductFile file_contents) {
			if (filename == null || filename.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.file_info.file_info: Filename is not specified");
			}
			if (mime_type == null || mime_type.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.file_info.file_info: MIME type is not specified");
			}
			this.filename = filename;
			this.mime_type = mime_type;
			this.file_contents = file_contents;
			return;
		}

		// Append a string representation, containing the XML respesentation, to the String.

		public void append_string (StringBuilder sb) {
			sb.append ("    <format href=\"" + filename + "\" type=\"" + mime_type + "\" />\n");
			return;
		}

		// Append file contents to the collection.

		public void append_contents (Collection<PDLProductFile> contents) {
			if (file_contents != null) {
				contents.add (file_contents);
			}
			return;
		}
	}




	// Nested class to hold information about one section.

	private static class section_info {

		// The section title, cannot be null or blank.

		public String title;

		// The section id, cannot be null or blank, must consist of letters and digits beginning with a letter.

		public String section_id;

		// The subtitle, can be null or blank to omit.

		public String subtitle;

		// The list of files.

		public ArrayList<file_info> files;

		// Constructor, given the title, section id, and subtitle.

		public section_info (String title, String section_id, String subtitle) {
			if (title == null || title.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.section_info.section_info: Title is not specified");
			}
			if (section_id == null || section_id.isEmpty()) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.section_info.section_info: Section ID is not specified");
			}
			if (!( section_id.matches("[A-Za-z][0-9A-Za-z]*") )) {
				throw new IllegalArgumentException ("PDLContentsXmlBuilder.section_info.section_info: Section ID is invalid: section_id = " + section_id);
			}
			this.title = title;
			this.section_id = section_id;
			this.subtitle = subtitle;
			this.files = new ArrayList<file_info>();
			return;
		}

		// Add a file.

		public void add_file (file_info file) {
			files.add (file);
			return;
		}

		// Append a string representation, containing the XML respesentation, to the String.

		public void append_string (StringBuilder sb) {
			sb.append ("  <file title=\"" + title + "\" id=\"" + section_id + "\">\n");
			if (!( subtitle == null || subtitle.isEmpty() )) {
				sb.append ("    <caption><![CDATA[" + subtitle + "]]></caption>\n");
			}
			for (file_info file : files) {
				file.append_string (sb);
			}
			sb.append ("  </file>\n");
			return;
		}

		// Append file contents to the collection.

		public void append_contents (Collection<PDLProductFile> contents) {
			for (file_info file : files) {
				file.append_contents (contents);
			}
			return;
		}
	}




	// The list of sections.

	private ArrayList<section_info> sections;




	// Constructor begins with an empty list of sections.

	public PDLContentsXmlBuilder () {
		sections = new ArrayList<section_info>();
	}




	// Begin a new section.
	// Parameters:
	//  title = The section title, cannot be null or blank.
	//  section_id = The section id, cannot be null or blank, must consist of letters and digits beginning with a letter.
	//  subtitle = The subtitle, can be null or blank to omit.
	// Note: Each section should have a different id, however PDL does not seem to enforce this rule.

	public void begin_section (String title, String section_id, String subtitle) {
		sections.add (new section_info (title, section_id, subtitle));
		return;
	}




	// Add a file to the current section.
	// Parameters:
	//  filename = The filename, cannot be null or blank.
	//  mime_type = The mime type, cannot be null or blank.
	// Note: Various mime types are defined in PDLProductFile.

	public void add_file (String filename, String mime_type) {
		if (sections.isEmpty()) {
			throw new IllegalStateException ("PDLContentsXmlBuilder.add_file: Attempt to add a file when there is no current section");
		}
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException ("PDLContentsXmlBuilder.add_file: Filename is not specified");
		}
		if (mime_type == null || mime_type.isEmpty()) {
			throw new IllegalArgumentException ("PDLContentsXmlBuilder.add_file: MIME type is not specified");
		}
		sections.get(sections.size() - 1).add_file (new file_info (filename, mime_type));
		return;
	}




	// Add a file to the current section.
	// Parameters:
	//  filename = The filename, cannot be null or blank.
	//  mime_type = The mime type, cannot be null or blank.
	//  file_contents = The file contents, or null if not supplied.
	// Note: Various mime types are defined in PDLProductFile.

	public void add_file (String filename, String mime_type, PDLProductFile file_contents) {
		if (sections.isEmpty()) {
			throw new IllegalStateException ("PDLContentsXmlBuilder.add_file: Attempt to add a file when there is no current section");
		}
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException ("PDLContentsXmlBuilder.add_file: Filename is not specified");
		}
		if (mime_type == null || mime_type.isEmpty()) {
			throw new IllegalArgumentException ("PDLContentsXmlBuilder.add_file: MIME type is not specified");
		}
		sections.get(sections.size() - 1).add_file (new file_info (filename, mime_type, file_contents));
		return;
	}




	// Add a file to the current section.
	// Parameters:
	//  filename = The filename, cannot be null or blank.
	//  mime_type = The mime type, cannot be null or blank.
	//  bytes = The file contents, cannot be null.
	// Note: Various mime types are defined in PDLProductFile.

	public void add_file (String filename, String mime_type, byte[] bytes) {
		PDLProductFile file_contents = (new PDLProductFile()).set_bytes (bytes, filename, mime_type);
		add_file (filename, mime_type, file_contents);
		return;
	}




	// Add a file to the current section.
	// Parameters:
	//  filename = The filename, cannot be null or blank.
	//  mime_type = The mime type, cannot be null or blank.
	//  text = The file contents, cannot be null.
	// Note: Various mime types are defined in PDLProductFile.

	public void add_file (String filename, String mime_type, String text) {
		PDLProductFile file_contents = (new PDLProductFile()).set_bytes (text, filename, mime_type);
		add_file (filename, mime_type, file_contents);
		return;
	}




	// Make a string containing the contents.xml file.

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder();

		sb.append ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append ("<contents>\n");
		for (section_info section : sections) {
			section.append_string (sb);
		}
		sb.append ("</contents>\n");
		
		return sb.toString();
	}




	// Make the PDL product file for contents.xml.

	public PDLProductFile make_product_file_contents_xml () {
		return (new PDLProductFile()).set_bytes (
			toString(), PDLProductFile.CONTENTS_XML, PDLProductFile.APPLICATION_XML);
	}




	// Append all file contents to the collection.
	// The contents.xml itself is the first element added to the collection.
	// Then, other file contents are added in the order they were added here.

	public void append_all_contents (Collection<PDLProductFile> contents) {
		contents.add (make_product_file_contents_xml());
		for (section_info section : sections) {
			section.append_contents (contents);
		}
		return;
	}




	// Make an array containing all the file contents.

	public PDLProductFile[] make_product_file_array () {
		ArrayList<PDLProductFile> contents = new ArrayList<PDLProductFile>();
		append_all_contents (contents);
		return contents.toArray (new PDLProductFile[0]);
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("PDLContentsXmlBuilder : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Unmarshal from the configuration file, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("PDLContentsXmlBuilder : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Build the file

				PDLContentsXmlBuilder content_builder = new PDLContentsXmlBuilder();

				content_builder.begin_section ("Download subsection 1", "firstSubsection", "Subtitle for subsection 1");
				content_builder.add_file ("file_1.txt", PDLProductFile.TEXT_PLAIN);
				content_builder.add_file ("file_2.xml", PDLProductFile.APPLICATION_XML);
				content_builder.add_file ("file_3.json", PDLProductFile.APPLICATION_JSON);

				content_builder.begin_section ("Download subsection 2", "secondSubsection", "Subtitle for subsection 2");
				content_builder.add_file ("image_1.jpg", PDLProductFile.IMAGE_JPEG);
				content_builder.add_file ("image_2.png", PDLProductFile.IMAGE_PNG);

				// Display it

				System.out.println (content_builder.toString());

				// Make the product file, just to see it goes without exception

				PDLProductFile product_file = content_builder.make_product_file_contents_xml();

				// Make the product file array, just to see it goes without exception

				PDLProductFile[] product_file_array = content_builder.make_product_file_array();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("PDLContentsXmlBuilder : Unrecognized subcommand : " + args[0]);
		return;

	}

}
