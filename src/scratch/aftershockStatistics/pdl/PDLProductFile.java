package scratch.aftershockStatistics.pdl;

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
import java.net.URL;
import java.util.Map;
import java.util.List;



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

	// The filename for contents.xml

	public static final String CONTENTS_XML = "contents.xml";




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

}
