package org.opensha.oaf.pdl;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.distribution.SocketProductSender;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import gov.usgs.util.CryptoUtils;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Arrays;



/**
 * Represents one file in a PDL product.
 * Author: Michael Barall 08/24/2018.
 *
 * A file can be supplied in four ways:
 * 1. As an array of byte in memory, which may contain arbitrary binary data.
 *    You need to supply the filename used by PDL, and the MIME type.
 * 2. As a String in memory, which may contain arbitrary text.
 *    You need to supply the filename used by PDL, and the MIME type.
 * 3. As a disk file.
 *    You need to supply the filename used by PDL, and the local filename where
 *    the file is located on disk.
 *    The MIME type is inferred from the filename.
 * 4. As all the files in a disk directory.
 *    You need to supply the local directory name where the directory is located on disk.
 *    The filename used by PDL is the local filename, including the directory path
 *    (so if the directory name is "dirname" and it contains a file named "file.txt"
 *    then the PDL filename is "dirname/file.txt").
 *    The MIME type is inferred from the filename.
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
public class PDLProductFile {

	// Kind of file.

	private int kind = 0;

	private static final int KIND_BYTES = 1;		// In-memory bytes
	private static final int KIND_DISK = 2;			// Disk file
	private static final int KIND_DIRECTORY = 3;	// All the files in a disk directory

	// Filename for this product.
	// If the kind is KIND_DIRECTORY, it is inferred from the disk filename.

	private String filename = null;

	// If the kind is KIND_BYTES, these are the bytes.

	private byte[] bytes = null;

	// If the kind is KIND_BYTES, this is the MIME type.
	// (For disk files, the MIME type is inferred from the filename extension.)

	private String mime_type = null;

	// The location of the data on disk.
	// If the kind is KIND_DISK, it is the name of the disk file.
	// If the kind is KIND_DIRECTORY, it is the name of the disk directory.

	private String disk_location = null;

	// Some common values of the MIME type.

	public static final String TEXT_PLAIN = "text/plain";
	//public static final String TEXT_XML = "text/xml";		// alias for application/xml
	public static final String TEXT_HTML = "text/html";
	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_XML = "application/xml";
	public static final String APPLICATION_KML = "application/vnd.google-earth.kml+xml";
	public static final String APPLICATION_KMZ = "application/vnd.google-earth.kmz";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_GIF = "image/gif";

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	public static final String APPLICATION_POSTSCRIPT = "application/postscript";
	public static final String APPLICATION_X_TEX = "application/x-tex";
	public static final String IMAGE_TIFF = "image/tiff";
	public static final String TEXT_CSS = "text/css";
	public static final String TEXT_XML = "text/xml";		// alias for application/xml

	// The filename for contents.xml

	public static final String CONTENTS_XML = "contents.xml";




	// Mapping of file extensions to MIME types.
	// (Compare to list in gov.usgs.earthquake.product.FileContent)

	private static Map<String, String> extension_to_mime = new HashMap<String, String>();
	static {
		extension_to_mime.put("atom", "application/atom+xml");
		extension_to_mime.put("css", "text/css");
		extension_to_mime.put("gif", "image/gif");
		extension_to_mime.put("gz", "application/gzip");
		extension_to_mime.put("htm", "text/html");
		extension_to_mime.put("html", "text/html");
		extension_to_mime.put("jpeg", "image/jpeg");
		extension_to_mime.put("jpg", "image/jpeg");
		extension_to_mime.put("js", "text/javascript");
		extension_to_mime.put("json", "application/json");
		extension_to_mime.put("kml", "application/vnd.google-earth.kml+xml");
		extension_to_mime.put("kmz", "application/vnd.google-earth.kmz");
		extension_to_mime.put("pdf", "application/pdf");
		extension_to_mime.put("png", "image/png");
		extension_to_mime.put("ps", "application/postscript");
		extension_to_mime.put("tex", "application/x-tex");
		extension_to_mime.put("tif", "image/tiff");
		extension_to_mime.put("tiff", "image/tiff");
		extension_to_mime.put("txt", "text/plain");
		extension_to_mime.put("xml", "application/xml");
		extension_to_mime.put("zip", "application/zip");
	}





	// Return the MIME type for a given extension.
	// Parameters:
	//  extension = The filename extension (not including the ".").
	//  def_mime = The value to return if the extension is unrecognized.

	public static String get_mime_for_extension (String extension, String def_mime) {
		if (extension != null) {
			String my_extension = extension.trim().toLowerCase();
			if (extension_to_mime.containsKey (my_extension)) {
				return extension_to_mime.get (my_extension);
			}
		}
		return def_mime;
	}




	// Return the MIME type for a given extension.
	// Parameters:
	//  extension = The filename extension (not including the ".").
	// Returns "application/octet-stream" if the extension is unknown.

	public static String get_mime_for_extension (String extension) {
		return get_mime_for_extension (extension, "application/octet-stream");
	}




	// Return the MIME type for a given filename.
	// Parameters:
	//  filename = The filename.
	//  def_mime = The value to return if the extension is unrecognized.

	public static String get_mime_for_filename (String filename, String def_mime) {
		if (filename != null) {
			int index = filename.lastIndexOf ('.');
			if (index != -1) {
				String extension = filename.substring (index + 1);
				return get_mime_for_extension (extension, def_mime);
			}
		}
		return def_mime;
	}




	// Return the MIME type for a given filename.
	// Parameters:
	//  filename = The filename.
	// Returns "application/octet-stream" if the filename's extension is unknown.

	public static String get_mime_for_filename (String filename) {
		return get_mime_for_filename (filename, "application/octet-stream");
	}




	// Return the MIME type for a given file.
	// Parameters:
	//  file = The file.
	//  def_mime = The value to return if the extension is unrecognized.

	public static String get_mime_for_file (File file, String def_mime) {
		if (file != null) {
			return get_mime_for_filename (file.getName(), def_mime);
		}
		return def_mime;
	}




	// Return the MIME type for a given file.
	// Parameters:
	//  file = The file.
	// Returns "application/octet-stream" if the filename's extension is unknown.

	public static String get_mime_for_file (File file) {
		return get_mime_for_file (file, "application/octet-stream");
	}




	// Build the PDL directory tree, containing the files in a disk directory tree.
	// Parameters:
	//  disk_dirname = Name of a disk directory, containing all the files.
	//  pdl_dirname = Name of the top-level PDL directory, often "", can be null for "".
	// Returns: A map of <pdl_filename, disk_file> pairs.
	// Note: The returned map has a specific order: All the files in a disk directory in
	//  collation order, followed by the contents of subdirectories in depth-first order.

	public static LinkedHashMap<String, File> build_pdl_dir_tree (String disk_dirname, String pdl_dirname) {

		// Separator character used for PDL paths

		final String pdl_separator = "/";

		// Maximum number of files and directories to process.

		final int max_items = 1000;

		// The PDL directory tree to return

		LinkedHashMap<String, File> pdl_dir_tree = new LinkedHashMap<String, File>();

		// Queues of disk and pdl directories waiting to be processed

		Queue<File> disk_dirs = new ArrayDeque<File>();
		Queue<String> pdl_dirs = new ArrayDeque<String>();

		// Put the first directory into the list

		File base_dir = new File (disk_dirname);

		if (!( base_dir.isDirectory() )) {
			throw new IllegalArgumentException ("PDLProductFile.build_pdl_dir_tree: Directory not found: " + disk_dirname);
		}
		if (base_dir.isHidden() || base_dir.getName().trim().startsWith(".")) {
			throw new IllegalArgumentException ("PDLProductFile.build_pdl_dir_tree: Directory is hidden: " + disk_dirname);
		}

		disk_dirs.add (base_dir);
		if (pdl_dirname == null) {
			pdl_dirs.add ("");
		} else {
			pdl_dirs.add (pdl_dirname.trim());
		}

		// Comparator to sort files, based only on the name (not path)

		Comparator<File> fcomp = new Comparator<File>() {
			@Override
			public int compare (File file1, File file2) {
				return file1.getName().compareTo (file2.getName());
			}
		};

		// Loop until all directories processed

		int item_count = 0;

		while (!( disk_dirs.isEmpty() )) {

			// Get the current disk directory and pdl path

			File disk_dir = disk_dirs.remove();
			String pdl_dir = pdl_dirs.remove();

			// Get the list of files and subdirectories in the current directory

			File[] flist = disk_dir.listFiles();
			if (flist != null && flist.length > 0) {

				// Count the number of items

				item_count += flist.length;
				if (item_count > max_items) {
					throw new RuntimeException ("PDLProductFile.build_pdl_dir_tree: Exceeded maximum item count, max_items = " + max_items);
				}

				// Sort the list

				Arrays.sort (flist, fcomp);

				// Append separator to pdl directory if needed

				if (!( pdl_dir.isEmpty() || pdl_dir.endsWith(pdl_separator) )) {
					pdl_dir = pdl_dir + pdl_separator;
				}

				// Scan the list

				for (File list_file : flist) {

					// If it's a directory and not hidden, add to the directory queue

					if (list_file.isDirectory()) {
						if (!( list_file.isHidden() || list_file.getName().trim().startsWith(".") )) {
							disk_dirs.add (list_file);
							pdl_dirs.add (pdl_dir + (list_file.getName()));
						}
					}

					// Otherwise, if it's a file and not hidden, add to the tree

					else if (list_file.isFile()) {
						if (!( list_file.isHidden() || list_file.getName().trim().startsWith(".") )) {
							pdl_dir_tree.put (pdl_dir + (list_file.getName()), list_file);
						}
					}
				}
			}
		}

		return pdl_dir_tree;
	}




	// Given a pdl directory tree as above, build the list of pdl files.
	// Parameters:
	//  pdl_dir_tree = Mapping of pdl filenames to File objects.
	// Returns a list of PDLProductFile objects, one for each file.
	// Note: Files appear in the list according to the iteration order of the Map.
	// Note: If r is the returned list, you can get an array with:
	//  PDLProductFile[] y = r.toArray(new PDLProductFile[0]);

	public static List<PDLProductFile> build_file_list_from_tree (Map<String, File> pdl_dir_tree) {
		List<PDLProductFile> file_list = new ArrayList<PDLProductFile>();
		for (Map.Entry<String, File> entry : pdl_dir_tree.entrySet()) {
			String mime_type = get_mime_for_file (entry.getValue());
			file_list.add ((new PDLProductFile()).set_bytes (entry.getValue(), entry.getKey(), mime_type));
		}
		return file_list;
	}




	// Display our contents, all on one line, with no line terminator.

	@Override
	public String toString() {
		return "PDLProductFile"
		+ ": kind = " + kind
		+ ", filename = " + ((filename == null) ? "<null>" : filename)
		+ ", bytes = " + ((bytes == null) ? "<null>" : bytes.length)
		+ ", mime_type = " + ((mime_type == null) ? "<null>" : mime_type)
		+ ", disk_location = " + ((disk_location == null) ? "<null>" : disk_location);
	}




	// Set up byte contents.
	// Parameters:
	//  bytes = The data.
	//  filename = The filename to use in PDL.
	//  mime_type = The MIME type for this data.

	public PDLProductFile set_bytes (byte[] bytes, String filename, String mime_type) {

		if (bytes == null) {
			throw new IllegalArgumentException ("PDLProductFile: Byte data is not specified");
		}
		if (filename == null) {
			throw new IllegalArgumentException ("PDLProductFile: Filename is not specified");
		}
		if (mime_type == null || mime_type.isEmpty()) {
			throw new IllegalArgumentException ("PDLProductFile: MIME type is not specified");
		}

		this.kind = KIND_BYTES;
		this.filename = filename;
		this.bytes = bytes;
		this.mime_type = mime_type;
		this.disk_location = null;
		return this;
	}




	// Set up byte contents.
	// Parameters:
	//  text = The data, as a string.
	//  filename = The filename to use in PDL.
	//  mime_type = The MIME type for this data.

	public PDLProductFile set_bytes (String text, String filename, String mime_type) {

		if (text == null) {
			throw new IllegalArgumentException ("PDLProductFile: Text data is not specified");
		}

		set_bytes (text.getBytes(), filename, mime_type);
		return this;
	}




	// Set up byte contents.
	// Parameters:
	//  disk_file = A disk file that contains the data.
	//  filename = The filename to use in PDL.
	//  mime_type = The MIME type for this data.

	public PDLProductFile set_bytes (File disk_file, String filename, String mime_type) {

		if (disk_file == null) {
			throw new IllegalArgumentException ("PDLProductFile: Disk file is not specified");
		}

		byte[] bytes;
		try {
			bytes = Files.readAllBytes (disk_file.toPath());
		}
		catch (IOException e) {
			throw new IllegalArgumentException ("PDLProductFile: Cannot read disk file: " + disk_file.getPath(), e);
		}

		set_bytes (bytes, filename, mime_type);
		return this;
	}




	// Set up disk contents.
	// Parameters:
	//  filename = The filename to use in PDL.
	//  disk_location = The file location on disk.

	public PDLProductFile set_disk (String filename, String disk_location) {

		if (filename == null) {
			throw new IllegalArgumentException ("PDLProductFile: Filename is not specified");
		}
		if (disk_location == null || disk_location.isEmpty()) {
			throw new IllegalArgumentException ("PDLProductFile: Disk location is not specified");
		}

		this.kind = KIND_DISK;
		this.filename = filename;
		this.bytes = null;
		this.mime_type = null;
		this.disk_location = disk_location;
		return this;
	}




	// Set up disk directory contents.
	// Parameters:
	//  disk_location = The directory location on disk.

	public PDLProductFile set_directory (String disk_location) {

		if (disk_location == null || disk_location.isEmpty()) {
			throw new IllegalArgumentException ("PDLProductFile: Disk location is not specified");
		}

		this.kind = KIND_DIRECTORY;
		this.filename = null;
		this.bytes = null;
		this.mime_type = null;
		this.disk_location = disk_location;
		return this;
	}




	// Attach the file to a product.
	// Parameters:
	//  contents = Product contents.

	public void attach_to_product (Map<String, Content> contents) {

		// Select kind of file

		switch (kind) {

		default:
			throw new IllegalStateException ("PDLProductFile: Unknown file kind: " + kind);

		case KIND_BYTES:
			try {
				ByteContent diskContent = new ByteContent(bytes);
				diskContent.setContentType(mime_type);		// `null` implies "text/plain"
				diskContent.setLastModified(/*Date*/ null);	// `null` implies NOW
				contents.put(filename, diskContent);
			}
			catch (Exception e) {
				throw new RuntimeException ("PDLProductFile: Error attaching byte data to product: " + filename, e);
			}
			break;

		case KIND_DISK:
			try {
				FileContent fileContent = new FileContent(new File(disk_location));
				contents.put(filename, fileContent);
			}
			catch (Exception e) {
				throw new RuntimeException ("PDLProductFile: Error attaching disk file to product: " + disk_location, e);
			}
			break;


		case KIND_DIRECTORY:
			try {
				contents.putAll(FileContent.getDirectoryContents(new File(disk_location)));
			}
			catch (Exception e) {
				throw new RuntimeException ("PDLProductFile: Error attaching disk directory to product: " + disk_location, e);
			}
			break;
		}

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("PDLProductFile : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  disk_dirname
		// Build the pdl directory tree from the given disk directory, and display it.
		// Then build the list of pdl files, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional argument

			if (args.length != 2) {
				System.err.println ("PDLProductFile : Invalid 'test1' subcommand");
				return;
			}

			try {

				String disk_dirname = args[1];

				// Say hello

				System.out.println ("Building PDL directory tree and file list");
				System.out.println ("disk_dirname = " + disk_dirname);

				// Build the tree

				String pdl_dirname = null;

				Map<String, File> pdl_dir_tree = PDLProductFile.build_pdl_dir_tree (disk_dirname, pdl_dirname);

				// Iterate over the tree and display it

				System.out.println ();
				System.out.println ("PDL directory tree");

				for (Map.Entry<String, File> entry : pdl_dir_tree.entrySet()) {
					System.out.println (entry.getKey() + " ==> " + entry.getValue().getPath());
				}

				// Build the list of pdl files

				List<PDLProductFile> pdl_file_list = PDLProductFile.build_file_list_from_tree (pdl_dir_tree);

				// Iterate over the list and display it

				System.out.println ();
				System.out.println ("PDL file list");

				for (PDLProductFile pdl_file : pdl_file_list) {
					System.out.println (pdl_file.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("PDLProductFile : Unrecognized subcommand : " + args[0]);
		return;

	}

}
