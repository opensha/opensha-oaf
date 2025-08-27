package org.opensha.oaf.pdl;

import java.io.IOException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import gov.usgs.util.Config;
import gov.usgs.earthquake.aws.AwsProductSender;


// Configuration settings for a PDL AwsProductSender.
// Author: Michael Barall.
//
// This is written based on PDL client version 3.5.0.

public class PDLAwsSenderConfig extends PDLAnySenderConfig implements Marshalable {


	//----- Parameters -----


	// Connection timeout, in milliseconds (0 selects default).

	public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
	public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
	public static final int TEST_CONNECT_TIMEOUT = 15000;

	protected int connectTimeout = 0;

	protected int get_connectTimeout() {
		return ((connectTimeout == 0) ? DEFAULT_CONNECT_TIMEOUT : connectTimeout);
	}


	// Base URL for the PDL hub (empty string selects default).

	public static final String HUB_URL_PROPERTY = "url";
	public static final String HUB_URL_DEFAULT = "https://earthquake.usgs.gov";
	public static final String TEST_HUB_URL = "https://earthquake.usgs.gov";

	protected String url = "";

	protected String get_url() {
		return ((url.isEmpty()) ? HUB_URL_DEFAULT : url);
	}


	// Private key file (not stored in this object).

	public static final String PRIVATE_KEY_PROPERTY = "privateKey";
	public static final String TEST_PRIVATE_KEY = "./MyPrivateKeyFile";


	// Flag indicating whether to sign products.

	public static final String SIGN_PRODUCTS_PROPERTY = "signProducts";
	public static final boolean DEFAULT_SIGN_PRODUCTS = false;
	public static final boolean TEST_SIGN_PRODUCTS = true;

	protected boolean signProducts = DEFAULT_SIGN_PRODUCTS;


	// Relative path for sending product (empty string selects default).

	public static final String SEND_PRODUCT_PATH_PROPERTY = "sendProductPath";
	public static final String SEND_PRODUCT_PATH_DEFAULT = "/pdl/east/products/{urn}";
	public static final String TEST_SEND_PRODUCT_PATH = "/pdl/west/send_product";

	protected String sendProductPath = "";

	protected String get_sendProductPath() {
		return ((sendProductPath.isEmpty()) ? SEND_PRODUCT_PATH_DEFAULT : sendProductPath);
	}

	protected boolean include_sendProductPath() {
		return !(sendProductPath.isEmpty());
	}


	// Relative path to get URLs for uploading files (empty string selects default).

	public static final String GET_UPLOAD_URLS_PATH_PROPERTY = "getUploadUrlsPath";
	public static final String GET_UPLOAD_URLS_PATH_DEFAULT = "/pdl/east/products/{urn}/uploads";
	public static final String TEST_GET_UPLOAD_URLS_PATH = "/pdl/west/get_upload_urls";

	protected String getUploadUrlsPath = "";

	protected String get_getUploadUrlsPath() {
		return ((getUploadUrlsPath.isEmpty()) ? GET_UPLOAD_URLS_PATH_DEFAULT : getUploadUrlsPath);
	}

	protected boolean include_getUploadUrlsPathh() {
		return !(getUploadUrlsPath.isEmpty());
	}


	// Content format to post products (empty string selects default).

	public static final String CONTENT_FORMAT_PROPERTY = "contentFormat";
	public static final String DEFAULT_CONTENT_FORMAT = "MAP";
	public static final String TEST_CONTENT_FORMAT = "ARRAY";

	protected String contentFormat = "";

	protected String get_contentFormat() {
		return ((contentFormat.isEmpty()) ? DEFAULT_CONTENT_FORMAT : contentFormat);
	}

	protected boolean include_contentFormat() {
		return !(contentFormat.isEmpty());
	}




	//----- Readout -----




	// Read out the configuration properties.
	// Parameters:
	//  keys = List that receives the property keys.
	//  props = Receives the property (key, value) pairs, with all values converted to String.
	//  privateKey = The private key file, if null or empty then products are not signed.

	public void readout (List<String> keys, Properties props, String privateKey) {

		keys.add (CONNECT_TIMEOUT_PROPERTY);
		props.setProperty (CONNECT_TIMEOUT_PROPERTY, Integer.toString(get_connectTimeout()));

		keys.add (HUB_URL_PROPERTY);
		props.setProperty (HUB_URL_PROPERTY, get_url());

		boolean f_signing = signProducts && privateKey != null && !(privateKey.isEmpty());

		if (f_signing) {
			keys.add (PRIVATE_KEY_PROPERTY);
			props.setProperty (PRIVATE_KEY_PROPERTY, privateKey);
		}

		keys.add (SIGN_PRODUCTS_PROPERTY);
		props.setProperty (SIGN_PRODUCTS_PROPERTY, Boolean.toString(f_signing));

		if (include_sendProductPath()) {
			keys.add (SEND_PRODUCT_PATH_PROPERTY);
			props.setProperty (SEND_PRODUCT_PATH_PROPERTY, get_sendProductPath());
		}

		if (include_getUploadUrlsPathh()) {
			keys.add (GET_UPLOAD_URLS_PATH_PROPERTY);
			props.setProperty (GET_UPLOAD_URLS_PATH_PROPERTY, get_getUploadUrlsPath());
		}

		if (include_contentFormat()) {
			keys.add (CONTENT_FORMAT_PROPERTY);
			props.setProperty (CONTENT_FORMAT_PROPERTY, get_contentFormat());
		}

		return;
	}




	// Display our contents.

	@Override
	public String toString () {
		StringBuilder result = new StringBuilder();

		result.append ("PDLAwsSenderConfig:" + "\n");

		result.append ("connectTimeout = " + connectTimeout + "\n");
		result.append ("url = \"" + url + "\"\n");
		result.append ("signProducts = " + signProducts + "\n");
		result.append ("sendProductPath = \"" + sendProductPath + "\"\n");
		result.append ("getUploadUrlsPath = \"" + getUploadUrlsPath + "\"\n");
		result.append ("contentFormat = \"" + contentFormat + "\"\n");

		return result.toString();
	}




	// Return true if this PDL sender is able to sign products.

	@Override
	public boolean sender_can_sign () {
		return signProducts;
	}




	// Display our contents as properties.

	public String show_props (String privateKey) {
		StringBuilder result = new StringBuilder();

		List<String> keys = new ArrayList<String>();
		Properties props = new Properties();
		readout (keys, props, privateKey);

		for (String key : keys) {
			result.append (key + " = " + props.getProperty(key) + "\n");
		}

		return result.toString();
	}




	// Display our contents as code for creating AwsProductSender..

	public String show_code (String privateKey) {
		StringBuilder result = new StringBuilder();

		List<String> keys = new ArrayList<String>();
		Properties props = new Properties();
		readout (keys, props, privateKey);

		result.append ("Config config = new Config();\n");

		for (String key : keys) {
			result.append ("config.setProperty (\"" + key + "\", \"" + props.getProperty(key) + "\");\n");
		}

		result.append ("AwsProductSender sender = new AwsProductSender();\n");
		result.append ("sender.configure (config);\n");
		result.append ("sender.sendProduct (product);\n");

		return result.toString();
	}




	// Get a string describing the destination.

	public String show_destination () {
		return get_url() + get_sendProductPath();
	}




	// Make and configure an AwsProductSender.
	// If f_log is true, attach logging output to console.

	public AwsProductSender make_sender (String privateKey) throws Exception {
		List<String> keys = new ArrayList<String>();
		Config config = new Config();
		readout (keys, config, privateKey);
		AwsProductSender sender = new AwsProductSender();
		sender.configure (config);
		return sender;
	}




	//----- Construction -----




	// Default constructor, sets all properties to default.

	public PDLAwsSenderConfig () {
		clear();
	}




	// Clear all properties to default.

	public final void clear () {
		connectTimeout = 0;
		url = "";
		signProducts = DEFAULT_SIGN_PRODUCTS;
		sendProductPath = "";
		getUploadUrlsPath = "";
		contentFormat = "";
		return;
	}




	// Set to test values.

	public final void setup_test () {
		connectTimeout = TEST_CONNECT_TIMEOUT;
		url = TEST_HUB_URL;
		signProducts = TEST_SIGN_PRODUCTS;
		sendProductPath = TEST_SEND_PRODUCT_PATH;
		getUploadUrlsPath = TEST_GET_UPLOAD_URLS_PATH;
		contentFormat = TEST_CONTENT_FORMAT;
		return;
	}




	// Set to test values.
	// Parameters:
	//  hubaddr = Hub address, not ending in slash.
	//  subpath = Hub sub-path, beginning but not ending in slash, or empty to use defaults.

	public final void setup_test_2 (String hubaddr, String subpath) {
		connectTimeout = TEST_CONNECT_TIMEOUT;
		url = hubaddr;
		signProducts = TEST_SIGN_PRODUCTS;
		if (!( subpath.isEmpty() )) {
			sendProductPath = subpath + TEST_SEND_PRODUCT_PATH.substring (TEST_SEND_PRODUCT_PATH.lastIndexOf ("/"));
			getUploadUrlsPath = subpath + TEST_GET_UPLOAD_URLS_PATH.substring (TEST_GET_UPLOAD_URLS_PATH.lastIndexOf ("/"));
			contentFormat = TEST_CONTENT_FORMAT;
		}
		return;
	}




	//----- Logging -----




	// The handler for sending log messages to System.err, note it is static.

	protected static ConsoleHandler log_handler = null;

	// The current log level.

	protected static Level log_level = null;


	// Install a handler for AwsProductSender with the given log level.

	public static void install_log_handler (Level level) {

		// If we have a handler, just change its level (not sure if this works)

		if (log_handler != null) {
			if (!( level.equals (log_level) )) {
				log_level = level;
				log_handler.setLevel (level);
				AwsProductSender.LOGGER.setLevel (level);
			}
			return;
		}

		// If the level is OFF, do nothing

		if (level.equals (Level.OFF)) {
			return;
		}

		// Create and install the handler

		log_handler = new ConsoleHandler();
		log_level = level;
		log_handler.setLevel (level);
		AwsProductSender.LOGGER.setLevel (level);
		AwsProductSender.LOGGER.addHandler (log_handler);

		return;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 150001;

	private static final String M_VERSION_NAME = "PDLAwsSenderConfig";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt ("connectTimeout", connectTimeout);
			writer.marshalString ("url", url);
			writer.marshalBoolean ("signProducts", signProducts);
			writer.marshalString ("sendProductPath", sendProductPath);
			writer.marshalString ("getUploadUrlsPath", getUploadUrlsPath);
			writer.marshalString ("contentFormat", contentFormat);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			connectTimeout = reader.unmarshalInt ("connectTimeout");
			url = reader.unmarshalString ("url");
			signProducts = reader.unmarshalBoolean ("signProducts");
			sendProductPath = reader.unmarshalString ("sendProductPath");
			getUploadUrlsPath = reader.unmarshalString ("getUploadUrlsPath");
			contentFormat = reader.unmarshalString ("contentFormat");

		}
		break;

		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public PDLAwsSenderConfig unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, PDLAwsSenderConfig x) {
		x.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static PDLAwsSenderConfig static_unmarshal (MarshalReader reader, String name) {
		return (new PDLAwsSenderConfig()).unmarshal (reader, name);
	}

	// Marshal array of objects.

	public static void marshal_array (MarshalWriter writer, String name, PDLAwsSenderConfig[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal array of objects.

	public static PDLAwsSenderConfig[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		PDLAwsSenderConfig[] x = new PDLAwsSenderConfig[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "PDLAwsSenderConfig");




		// Subcommand : Test #1
		// Command format:
		//  test1  f_test  f_log
		// Construct configuration, then display as string, properties, and code.
		// Marshal to JSON and display.
		// Unmarshal from JSON and display.
		// Construct sender.
		// If f_test is true, use test values (otherwise, use default values).
		// If f_log is true, make logger output to console.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling AwsProductSender configuration");
			boolean f_test = testargs.get_boolean ("f_test");
			boolean f_log = testargs.get_boolean ("f_log");
			testargs.end_test();

			// Create the values

			PDLAwsSenderConfig sender_config = new PDLAwsSenderConfig();

			if (f_test) {
				sender_config.setup_test();
			}

			// Display the contents

			System.out.println ();
			System.out.println ("********** Display as string **********");
			System.out.println ();

			System.out.println (sender_config.toString());

			System.out.println ();
			System.out.println ("********** Display as properties **********");
			System.out.println ();

			System.out.println (sender_config.show_props (TEST_PRIVATE_KEY));

			System.out.println ();
			System.out.println ("********** Display as code **********");
			System.out.println ();

			System.out.println (sender_config.show_code (TEST_PRIVATE_KEY));

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (sender_config);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (sender_config);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			PDLAwsSenderConfig sender_config2 = new PDLAwsSenderConfig();
			MarshalUtils.from_json_string (sender_config2, json_string);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Display as string **********");
			System.out.println ();

			System.out.println (sender_config2.toString());

			System.out.println ();
			System.out.println ("********** Display as properties **********");
			System.out.println ();

			System.out.println (sender_config2.show_props (TEST_PRIVATE_KEY));

			System.out.println ();
			System.out.println ("********** Display as code **********");
			System.out.println ();

			System.out.println (sender_config2.show_code (TEST_PRIVATE_KEY));

			// Make sender

			System.out.println ();
			System.out.println ("********** Make sender **********");
			System.out.println ();

			if (f_log) {
				install_log_handler (Level.FINER);
			}

			//AwsProductSender sender = sender_config.make_sender (TEST_PRIVATE_KEY);	// throws FileNotFoundException
			AwsProductSender sender = sender_config.make_sender ("");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  hubaddr  subpath  f_log
		// Same as test #1 except you can enter the hub address and sub-path (see setup_test_2()).
		// If f_log is true, make logger output to console.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling AwsProductSender configuration");
			String hubaddr = testargs.get_string ("hubaddr");
			String subpath = testargs.get_string ("subpath");
			boolean f_log = testargs.get_boolean ("f_log");
			testargs.end_test();

			// Create the values

			PDLAwsSenderConfig sender_config = new PDLAwsSenderConfig();
			sender_config.setup_test_2 (hubaddr, subpath);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Display as string **********");
			System.out.println ();

			System.out.println (sender_config.toString());

			System.out.println ();
			System.out.println ("********** Display as properties **********");
			System.out.println ();

			System.out.println (sender_config.show_props (TEST_PRIVATE_KEY));

			System.out.println ();
			System.out.println ("********** Display as code **********");
			System.out.println ();

			System.out.println (sender_config.show_code (TEST_PRIVATE_KEY));

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (sender_config);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (sender_config);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			PDLAwsSenderConfig sender_config2 = new PDLAwsSenderConfig();
			MarshalUtils.from_json_string (sender_config2, json_string);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Display as string **********");
			System.out.println ();

			System.out.println (sender_config2.toString());

			System.out.println ();
			System.out.println ("********** Display as properties **********");
			System.out.println ();

			System.out.println (sender_config2.show_props (TEST_PRIVATE_KEY));

			System.out.println ();
			System.out.println ("********** Display as code **********");
			System.out.println ();

			System.out.println (sender_config2.show_code (TEST_PRIVATE_KEY));

			// Make sender

			System.out.println ();
			System.out.println ("********** Make sender **********");
			System.out.println ();

			if (f_log) {
				install_log_handler (Level.FINER);
			}

			//AwsProductSender sender = sender_config.make_sender (TEST_PRIVATE_KEY);	// throws FileNotFoundException
			AwsProductSender sender = sender_config.make_sender ("");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
