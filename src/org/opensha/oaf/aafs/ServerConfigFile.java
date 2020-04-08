package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.TimeSplitOutputStream;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.oaf.pdl.PDLSenderConfig;

/**
 * Configuration file for AAFS server.
 * Author: Michael Barall 06/03/2018.
 *
 * All fields are public, since this is just a buffer for reading and writing files.
 *
 * JSON file format:
 *
 *	"ServerConfigFile" = Integer giving file version number, currently 34001.
 *	"mongo_config" = { Structure giving MongoDB configuration, see MongoDBConfig.java.
 *   }
 *  "server_name" = String giving the name used to manage the server.
 *  "server_number" = Integer giving number of this server, 1 or 2.
 *  "server_db_handles" = [ Array giving server database handles, 0 = local, 1 = server #1, 2 = server #2 ]
 *	"log_con_aafs" = String giving pattern for AAFS console log filenames, in the format of SimpleDateFormat, or "" if none.
 *	"log_con_intake" = String giving pattern for intake console log filenames, in the format of SimpleDateFormat, or "" if none.
 *	"log_con_control" = String giving pattern for control console log filenames, in the format of SimpleDateFormat, or "" if none.
 *	"log_summary" = String giving pattern for summary log filenames, in the format of SimpleDateFormat, or "" if none.
 *	"comcat_url" = String giving Comcat URL.
 *	"feed_url" = String giving real-time feed URL, or empty string to not use the feed.
 *	"comcat_dev_url" = String giving Comcat development URL.
 *	"feed_dev_url" = String giving real-time feed development URL, or empty string to not use the feed.
 *  "comcat_err_rate" = Real number giving rate of simulated Comcat errors.
 *  "comcat_exclude" = [ Array giving list of Comcat ids to exclude ]
 *  "locat_bins" = Integer giving number of latitude bins in local catalog, or 0 for default.
 *  "locat_filenames" = [ Array giving filenames for local catalog, empty if no local catalog ]
 *	"block_pdl_intake" = Integer giving PDL intake blocking option: 0 = don't block, 1 = block.
 *	"block_poll_intake" = Integer giving poll intake blocking option: 0 = don't block, 1 = block.
 *	"block_fc_content" = Integer giving forecast content blocking option: 0 = don't block, 1 = block.
 *  "db_err_rate" = Real number giving rate of simulated database errors.
 *	"pdl_enable" = Integer giving PDL enable option: 0 = none, 1 = development, 2 = production, 3 =  simulated development, 4 = simulated production, 5 = down development, 6 = down production.
 *	"pdl_key_filename" = String giving PDL signing key filename, can be empty string for none.
 *  "pdl_err_rate" = Real number giving rate of simulated PDL errors.
 *	"pdl_oaf_source" = String giving creator (source network) for OAF PDL products.
 *	"pdl_oaf_type" = String giving product type for OAF PDL products.
 *	"pdl_dev_senders" = [ Array giving a list of PDL sender configurations for development PDL, in priority order.
 *		element = { Structure giving PDL server configuration.
 *			"host" = String giving PDL sender host name or IP address.
 *			"port" = Integer giving PDL sender port number.
 *			"connectTimeout" = Integer giving PDL sender connection timeout, in milliseconds.
 *		}
 *	]
 *	"pdl_prod_senders" = [ Array giving a list of PDL sender configurations for production PDL, in priority order.
 *		element = { Structure giving PDL server configuration.
 *			"host" = String giving PDL sender host name or IP address.
 *			"port" = Integer giving PDL sender port number.
 *			"connectTimeout" = Integer giving PDL sender connection timeout, in milliseconds.
 *		}
 *	]
 */
public class ServerConfigFile {

	//----- Parameter values -----

	// MongoDB configuration.

	public MongoDBConfig mongo_config;

	// Name used to manage the server.

	public String server_name;

	// Number of this server, 1 or 2.

	public static final int SRVNUM_MIN = 1;
	public static final int SRVNUM_MAX = 2;

	public int server_number;

	// List of server database handles, 0 = local, 1 = server #1, 2 = server #2.

	public ArrayList<String> server_db_handles;

	// Pattern for AAFS console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String log_con_aafs;

	// Pattern for intake console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String log_con_intake;

	// Pattern for control console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String log_con_control;

	// Pattern for summary log filenames, in the format of SimpleDateFormat, or "" if none.

	public String log_summary;

	// Comcat URL.

	public String comcat_url;

	// Real-time feed URL, or empty string to not use the feed.

	public String feed_url;

	// Comcat development URL.

	public String comcat_dev_url;

	// Real-time feed development URL, or empty string to not use the feed.

	public String feed_dev_url;

	// Simulated error rate for Comcat.

	public double comcat_err_rate;

	// List of Comcat ids to exclude.

	public LinkedHashSet<String> comcat_exclude;

	// Number of latitude bins for local catalog, or 0 for default.

	public int locat_bins;

	// List of filenames for local catalog, empty if no local catalog.

	public ArrayList<String> locat_filenames;

	// PDL intake blocking option: 0 = don't block, 1 = block.

	public int block_pdl_intake;

	// Poll intake blocking option: 0 = don't block, 1 = block.

	public int block_poll_intake;

	// Forecast content blocking option: 0 = don't block, 1 = block.

	public int block_fc_content;

	// Simulated error rate for database.

	public double db_err_rate;

	// PDL enable option.

	public static final int PDLOPT_MIN = 0;
	public static final int PDLOPT_NONE = 0;		// No PDL access
	public static final int PDLOPT_DEV = 1;			// PDL development server
	public static final int PDLOPT_PROD = 2;		// PDL production server
	public static final int PDLOPT_SIM_DEV = 3;		// Simulated PDL development server
	public static final int PDLOPT_SIM_PROD = 4;	// Simulated PDL production server
	public static final int PDLOPT_DOWN_DEV = 5;	// Down PDL development server
	public static final int PDLOPT_DOWN_PROD = 6;	// Down PDL production server
	public static final int PDLOPT_MAX = 6;

	public static final int PDLOPT_UNSPECIFIED = -1;	// PDL access is unspecified

	public int pdl_enable;

	// PDL signing key filename, can be empty string for none.  Cannot be null.

	public String pdl_key_filename;

	// Simulated error rate for PDL.

	public double pdl_err_rate;

	// Creator (source network) for OAF PDL products.

	public String pdl_oaf_source;

	// Product type for OAF PDL products.

	public String pdl_oaf_type;

	// List of PDL development senders.

	public ArrayList<PDLSenderConfig> pdl_dev_senders;

	// List of PDL production senders.

	public ArrayList<PDLSenderConfig> pdl_prod_senders;


	//----- Construction -----

	// Default constructor.

	public ServerConfigFile () {
		clear();
	}

	// Clear the contents.

	public void clear () {
		mongo_config = null;
		server_name = "";
		server_number = 0;
		server_db_handles = new ArrayList<String>();
		log_con_aafs = "";
		log_con_intake = "";
		log_con_control = "";
		log_summary = "";
		comcat_url = "";
		feed_url = "";
		comcat_dev_url = "";
		feed_dev_url = "";
		comcat_err_rate = 0.0;
		comcat_exclude = new LinkedHashSet<String>();
		locat_bins = 0;
		locat_filenames = new ArrayList<String>();
		block_pdl_intake = 0;
		block_poll_intake = 0;
		block_fc_content = 0;
		db_err_rate = 0.0;
		pdl_enable = PDLOPT_NONE;
		pdl_key_filename = "";
		pdl_err_rate = 0.0;
		pdl_oaf_source = "";
		pdl_oaf_type = "";
		pdl_dev_senders = new ArrayList<PDLSenderConfig>();
		pdl_prod_senders = new ArrayList<PDLSenderConfig>();
		return;
	}

	// Check that values are valid, throw an exception if not.

	public void check_invariant () {

		if (mongo_config != null) {
			try {
				mongo_config.check_invariant();
			} catch (Exception e) {
				throw new InvariantViolationException ("ServerConfigFile: Invalid mongo_config", e);
			}
		} else {
			throw new InvariantViolationException ("ServerConfigFile: Invalid mongo_config: <null>");
		}

		if (!( server_name != null && server_name.trim().length() > 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid server_name: " + ((server_name == null) ? "<null>" : server_name));
		}

		if (!( server_number >= SRVNUM_MIN && server_number <= SRVNUM_MAX )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid server_number: " + server_number);
		}

		if (!( server_db_handles != null )) {
			throw new InvariantViolationException ("ServerConfigFile: server_db_handles list is null");
		}
		if (!( server_db_handles.size() == 3 )) {
			throw new InvariantViolationException ("ServerConfigFile: server_db_handles list has wrong size, size = " + server_db_handles.size());
		}
		for (String s : server_db_handles) {
			if (!( s != null && s.trim().length() > 0 )) {
				throw new InvariantViolationException ("ServerConfigFile: Invalid server_db_handles entry: " + ((s == null) ? "<null>" : s));
			}
		}

		if (!( log_con_aafs != null && (log_con_aafs.isEmpty() || TimeSplitOutputStream.is_valid_pattern(log_con_aafs)) )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid log_con_aafs: " + ((log_con_aafs == null) ? "<null>" : log_con_aafs));
		}

		if (!( log_con_intake != null && (log_con_intake.isEmpty() || TimeSplitOutputStream.is_valid_pattern(log_con_intake)) )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid log_con_intake: " + ((log_con_intake == null) ? "<null>" : log_con_intake));
		}

		if (!( log_con_control != null && (log_con_control.isEmpty() || TimeSplitOutputStream.is_valid_pattern(log_con_control)) )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid log_con_control: " + ((log_con_control == null) ? "<null>" : log_con_control));
		}

		if (!( log_summary != null && (log_summary.isEmpty() || TimeSplitOutputStream.is_valid_pattern(log_summary)) )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid log_summary: " + ((log_summary == null) ? "<null>" : log_summary));
		}

		if (!( comcat_url != null && comcat_url.trim().length() > 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid comcat_url: " + ((comcat_url == null) ? "<null>" : comcat_url));
		}

		if (!( feed_url != null )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid feed_url: " + ((feed_url == null) ? "<null>" : feed_url));
		}

		if (!( comcat_dev_url != null && comcat_dev_url.trim().length() > 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid comcat_dev_url: " + ((comcat_dev_url == null) ? "<null>" : comcat_dev_url));
		}

		if (!( feed_dev_url != null )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid feed_dev_url: " + ((feed_dev_url == null) ? "<null>" : feed_dev_url));
		}

		if (!( comcat_err_rate >= 0.0 && comcat_err_rate <= 1.0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid comcat_err_rate: " + comcat_err_rate);
		}

		if (!( comcat_exclude != null )) {
			throw new InvariantViolationException ("ServerConfigFile: comcat_exclude list is null");
		}
		for (String s : comcat_exclude) {
			if (!( s != null && s.trim().length() > 0 )) {
				throw new InvariantViolationException ("ServerConfigFile: Invalid comcat_exclude entry: " + ((s == null) ? "<null>" : s));
			}
		}

		if (!( locat_bins >= 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid locat_bins: " + locat_bins);
		}

		if (!( locat_filenames != null )) {
			throw new InvariantViolationException ("ServerConfigFile: locat_filenames list is null");
		}
		for (String s : locat_filenames) {
			if (!( s != null && s.trim().length() > 0 )) {
				throw new InvariantViolationException ("ServerConfigFile: Invalid locat_filenames entry: " + ((s == null) ? "<null>" : s));
			}
		}

		if (!( block_pdl_intake >= 0 && block_pdl_intake <= 1 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid block_pdl_intake: " + block_pdl_intake);
		}

		if (!( block_poll_intake >= 0 && block_poll_intake <= 1 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid block_poll_intake: " + block_poll_intake);
		}

		if (!( block_fc_content >= 0 && block_fc_content <= 1 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid block_fc_content: " + block_fc_content);
		}

		if (!( db_err_rate >= 0.0 && db_err_rate <= 1.0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid db_err_rate: " + db_err_rate);
		}

		if (!( pdl_enable >= PDLOPT_MIN && pdl_enable <= PDLOPT_MAX )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid pdl_enable: " + pdl_enable);
		}

		if (!( pdl_key_filename != null )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid pdl_key_filename: " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename));
		}

		if (!( pdl_err_rate >= 0.0 && pdl_err_rate <= 1.0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid pdl_err_rate: " + pdl_err_rate);
		}

		if (!( pdl_oaf_source != null && pdl_oaf_source.trim().length() > 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid pdl_oaf_source: " + ((pdl_oaf_source == null) ? "<null>" : pdl_oaf_source));
		}

		if (!( pdl_oaf_type != null && pdl_oaf_type.trim().length() > 0 )) {
			throw new InvariantViolationException ("ServerConfigFile: Invalid pdl_oaf_type: " + ((pdl_oaf_type == null) ? "<null>" : pdl_oaf_type));
		}

		if (!( pdl_dev_senders != null )) {
			throw new InvariantViolationException ("ServerConfigFile: pdl_dev_senders list is null");
		}

		if ( (pdl_enable == PDLOPT_DEV || pdl_enable == PDLOPT_SIM_DEV || pdl_enable == PDLOPT_DOWN_DEV) && pdl_dev_senders.size() == 0 ) {
			throw new InvariantViolationException ("ServerConfigFile: pdl_dev_senders is empty, but pdl_enable = " + pdl_enable);
		}

		if (!( pdl_prod_senders != null )) {
			throw new InvariantViolationException ("ServerConfigFile: pdl_prod_senders list is null");
		}

		if ( (pdl_enable == PDLOPT_PROD || pdl_enable == PDLOPT_SIM_PROD || pdl_enable == PDLOPT_DOWN_PROD) && pdl_prod_senders.size() == 0 ) {
			throw new InvariantViolationException ("ServerConfigFile: pdl_prod_senders is empty, but pdl_enable = " + pdl_enable);
		}

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("ServerConfigFile:" + "\n");

		result.append ("mongo_config = {" + "\n");
		result.append (mongo_config.toString ("  "));
		result.append ("}" + "\n");

		result.append ("server_name = " + ((server_name == null) ? "<null>" : server_name) + "\n");

		result.append ("server_number = " + server_number + "\n");

		result.append ("server_db_handles = [" + "\n");
		for (String s : server_db_handles) {
			result.append ("  " + s + "\n");
		}
		result.append ("]" + "\n");

		result.append ("log_con_aafs = " + ((log_con_aafs == null) ? "<null>" : log_con_aafs) + "\n");
		result.append ("log_con_intake = " + ((log_con_intake == null) ? "<null>" : log_con_intake) + "\n");
		result.append ("log_con_control = " + ((log_con_control == null) ? "<null>" : log_con_control) + "\n");
		result.append ("log_summary = " + ((log_summary == null) ? "<null>" : log_summary) + "\n");
		result.append ("comcat_url = " + ((comcat_url == null) ? "<null>" : comcat_url) + "\n");
		result.append ("feed_url = " + ((feed_url == null) ? "<null>" : feed_url) + "\n");
		result.append ("comcat_dev_url = " + ((comcat_dev_url == null) ? "<null>" : comcat_dev_url) + "\n");
		result.append ("feed_dev_url = " + ((feed_dev_url == null) ? "<null>" : feed_dev_url) + "\n");
		result.append ("comcat_err_rate = " + comcat_err_rate + "\n");

		result.append ("comcat_exclude = [" + "\n");
		for (String s : comcat_exclude) {
			result.append ("  " + s + "\n");
		}
		result.append ("]" + "\n");

		result.append ("locat_bins = " + locat_bins + "\n");

		result.append ("locat_filenames = [" + "\n");
		for (String s : locat_filenames) {
			result.append ("  " + s + "\n");
		}
		result.append ("]" + "\n");

		result.append ("block_pdl_intake = " + block_pdl_intake + "\n");
		result.append ("block_poll_intake = " + block_poll_intake + "\n");
		result.append ("block_fc_content = " + block_fc_content + "\n");
		result.append ("db_err_rate = " + db_err_rate + "\n");

		result.append ("pdl_enable = " + pdl_enable + "\n");
		result.append ("pdl_key_filename = " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename) + "\n");
		result.append ("pdl_err_rate = " + pdl_err_rate + "\n");
		result.append ("pdl_oaf_source = " + ((pdl_oaf_source == null) ? "<null>" : pdl_oaf_source) + "\n");
		result.append ("pdl_oaf_type = " + ((pdl_oaf_type == null) ? "<null>" : pdl_oaf_type) + "\n");

		result.append ("pdl_dev_senders = [" + "\n");
		for (int i = 0; i < pdl_dev_senders.size(); ++i) {
			PDLSenderConfig pdl_sender = pdl_dev_senders.get(i);
			result.append ("  " + i + ":  " + pdl_sender.toString() + "\n");
		}
		result.append ("]" + "\n");

		result.append ("pdl_prod_senders = [" + "\n");
		for (int i = 0; i < pdl_prod_senders.size(); ++i) {
			PDLSenderConfig pdl_sender = pdl_prod_senders.get(i);
			result.append ("  " + i + ":  " + pdl_sender.toString() + "\n");
		}
		result.append ("]" + "\n");

		return result.toString();
	}


	//----- Service functions -----

	// Get the currently selected list of PDL senders.
	// This returns a copy of the list, so the original cannot be modified.

	public List<PDLSenderConfig> get_pdl_senders () {
		ArrayList<PDLSenderConfig> pdl_senders = new ArrayList<PDLSenderConfig>();

		switch (pdl_enable) {

		case PDLOPT_DEV:
		case PDLOPT_SIM_DEV:
		case PDLOPT_DOWN_DEV:
			for (PDLSenderConfig pdl_sender : pdl_dev_senders) {
				pdl_senders.add (pdl_sender);
			}
			break;

		case PDLOPT_PROD:
		case PDLOPT_SIM_PROD:
		case PDLOPT_DOWN_PROD:
			for (PDLSenderConfig pdl_sender : pdl_prod_senders) {
				pdl_senders.add (pdl_sender);
			}
			break;

		}

		return pdl_senders;
	}

	// Get true if sending to PDL is permitted, false if not.

	public boolean get_is_pdl_permitted () {
		boolean result = false;

		switch (pdl_enable) {

		case PDLOPT_DEV:
		case PDLOPT_PROD:
			result = true;
			break;

		}

		return result;
	}

	// Get true if readback of PDL products should come from production, false if not.

	public boolean get_is_pdl_readback_prod () {
		boolean result = true;

		switch (pdl_enable) {

		case PDLOPT_DEV:
		case PDLOPT_SIM_DEV:
		case PDLOPT_DOWN_DEV:
			result = false;
			break;

		}

		return result;
	}

	// Get true if PDL is configured to be down, false if not.

	public boolean get_is_pdl_down () {
		boolean result = false;

		switch (pdl_enable) {

		case PDLOPT_DOWN_DEV:
		case PDLOPT_DOWN_PROD:
			result = true;
			break;

		}

		return result;
	}

	// Get true if PDL intake is blocked, false if not.

	public boolean get_is_pdl_intake_blocked () {
		boolean result = false;

		if (block_pdl_intake != 0) {
			result = true;
		}

		return result;
	}

	// Get true if poll intake is blocked, false if not.

	public boolean get_is_poll_intake_blocked () {
		boolean result = false;

		if (block_poll_intake != 0) {
			result = true;
		}

		return result;
	}

	// Get true if forecast content is blocked, false if not.

	public boolean get_is_fc_content_blocked () {
		boolean result = false;

		if (block_fc_content != 0) {
			result = true;
		}

		return result;
	}

	// Return the number of the partner server, 1 or 2.

	public int get_partner_server_number () {
		int result = 3 - server_number;
		return result;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 34001;

	private static final String M_VERSION_NAME = "ServerConfigFile";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 34000;
	protected static final int MARSHAL_SERVER_CFG = 34001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_SERVER_CFG;
	}

	// Marshal a PDL sender configuration.

	public static void marshal_pdl_sender (MarshalWriter writer, String name, PDLSenderConfig pdl_sender) {
		String host = pdl_sender.get_host();
		int port = pdl_sender.get_port();
		int connectTimeout = pdl_sender.get_connectTimeout();

		writer.marshalMapBegin (name);
		writer.marshalString ("host", host);
		writer.marshalInt ("port", port);
		writer.marshalInt ("connectTimeout", connectTimeout);
		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal a PDL sender configuration.

	public static PDLSenderConfig unmarshal_pdl_sender (MarshalReader reader, String name) {

		reader.unmarshalMapBegin (name);
		String host = reader.unmarshalString ("host");
		int port = reader.unmarshalInt ("port");
		int connectTimeout = reader.unmarshalInt ("connectTimeout");
		reader.unmarshalMapEnd ();

		return new PDLSenderConfig (host, port, connectTimeout);
	}

	// Marshal a PDL sender configuration list.

	public static void marshal_pdl_sender_list (MarshalWriter writer, String name, List<PDLSenderConfig> pdl_sender_list) {
		int n = pdl_sender_list.size();
		writer.marshalArrayBegin (name, n);
		for (PDLSenderConfig pdl_sender : pdl_sender_list) {
			marshal_pdl_sender (writer, null, pdl_sender);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a PDL sender configuration list.

	public static ArrayList<PDLSenderConfig> unmarshal_pdl_sender_list (MarshalReader reader, String name) {
		ArrayList<PDLSenderConfig> pdl_sender_list = new ArrayList<PDLSenderConfig>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			pdl_sender_list.add (unmarshal_pdl_sender (reader, null));
		}
		reader.unmarshalArrayEnd ();
		return pdl_sender_list;
	}

	// Marshal a collection of strings.

	public static void marshal_string_coll (MarshalWriter writer, String name, Collection<String> x) {
		int n = x.size();
		writer.marshalArrayBegin (name, n);
		for (String s : x) {
			writer.marshalString (null, s);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a collection of strings.

	public static void unmarshal_string_coll (MarshalReader reader, String name, Collection<String> x) {
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (reader.unmarshalString (null));
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Error check

		check_invariant();

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		mongo_config.marshal    (writer, "mongo_config"                        );

		writer.marshalString    (        "server_name"      , server_name      );
		writer.marshalInt       (        "server_number"    , server_number    );
		marshal_string_coll     (writer, "server_db_handles", server_db_handles);
		writer.marshalString    (        "log_con_aafs"     , log_con_aafs     );
		writer.marshalString    (        "log_con_intake"   , log_con_intake   );
		writer.marshalString    (        "log_con_control"  , log_con_control  );
		writer.marshalString    (        "log_summary"      , log_summary      );
		writer.marshalString    (        "comcat_url"       , comcat_url       );
		writer.marshalString    (        "feed_url"         , feed_url         );
		writer.marshalString    (        "comcat_dev_url"   , comcat_dev_url   );
		writer.marshalString    (        "feed_dev_url"     , feed_dev_url     );
		writer.marshalDouble    (        "comcat_err_rate"  , comcat_err_rate  );
		marshal_string_coll     (writer, "comcat_exclude"   , comcat_exclude   );
		writer.marshalInt       (        "locat_bins"       , locat_bins       );
		marshal_string_coll     (writer, "locat_filenames"  , locat_filenames  );
		writer.marshalInt       (        "block_pdl_intake" , block_pdl_intake );
		writer.marshalInt       (        "block_poll_intake", block_poll_intake);
		writer.marshalInt       (        "block_fc_content" , block_fc_content );
		writer.marshalDouble    (        "db_err_rate"      , db_err_rate      );
		writer.marshalInt       (        "pdl_enable"       , pdl_enable       );
		writer.marshalString    (        "pdl_key_filename" , pdl_key_filename );
		writer.marshalDouble    (        "pdl_err_rate"     , pdl_err_rate     );
		writer.marshalString    (        "pdl_oaf_source"   , pdl_oaf_source   );
		writer.marshalString    (        "pdl_oaf_type"     , pdl_oaf_type     );
		marshal_pdl_sender_list (writer, "pdl_dev_senders"  , pdl_dev_senders  );
		marshal_pdl_sender_list (writer, "pdl_prod_senders" , pdl_prod_senders );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		mongo_config      = new MongoDBConfig         (reader, "mongo_config"     );

		server_name       = reader.unmarshalString    (        "server_name"      );
		server_number     = reader.unmarshalInt       (        "server_number"    );
		server_db_handles = new ArrayList<String>();
		unmarshal_string_coll                         (reader, "server_db_handles", server_db_handles);
		log_con_aafs      = reader.unmarshalString    (        "log_con_aafs"     );
		log_con_intake    = reader.unmarshalString    (        "log_con_intake"   );
		log_con_control   = reader.unmarshalString    (        "log_con_control"  );
		log_summary       = reader.unmarshalString    (        "log_summary"      );
		comcat_url        = reader.unmarshalString    (        "comcat_url"       );
		feed_url          = reader.unmarshalString    (        "feed_url"         );
		comcat_dev_url    = reader.unmarshalString    (        "comcat_dev_url"   );
		feed_dev_url      = reader.unmarshalString    (        "feed_dev_url"     );
		comcat_err_rate   = reader.unmarshalDouble    (        "comcat_err_rate"  );
		comcat_exclude = new LinkedHashSet<String>();
		unmarshal_string_coll                         (reader, "comcat_exclude"   , comcat_exclude   );
		locat_bins        = reader.unmarshalInt       (        "locat_bins"       );
		locat_filenames = new ArrayList<String>();
		unmarshal_string_coll                         (reader, "locat_filenames"  , locat_filenames  );
		block_pdl_intake  = reader.unmarshalInt       (        "block_pdl_intake" );
		block_poll_intake = reader.unmarshalInt       (        "block_poll_intake");
		block_fc_content  = reader.unmarshalInt       (        "block_fc_content" );
		db_err_rate       = reader.unmarshalDouble    (        "db_err_rate"      );
		pdl_enable        = reader.unmarshalInt       (        "pdl_enable"       );
		pdl_key_filename  = reader.unmarshalString    (        "pdl_key_filename" );
		pdl_err_rate      = reader.unmarshalDouble    (        "pdl_err_rate"     );
		pdl_oaf_source    = reader.unmarshalString    (        "pdl_oaf_source"   );
		pdl_oaf_type      = reader.unmarshalString    (        "pdl_oaf_type"     );
		pdl_dev_senders   = unmarshal_pdl_sender_list (reader, "pdl_dev_senders"  );
		pdl_prod_senders  = unmarshal_pdl_sender_list (reader, "pdl_prod_senders" );

		// Error check

		check_invariant();

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public ServerConfigFile unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, ServerConfigFile obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static ServerConfigFile unmarshal_poly (MarshalReader reader, String name) {
		ServerConfigFile result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("ServerConfigFile.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_SERVER_CFG:
			result = new ServerConfigFile();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Unmarshal object from a configuration file.
	// Parameters:
	//  filename = Name of file (not including a path).
	//  requester = Class that is requesting the file.

	public static ServerConfigFile unmarshal_config (String filename, Class<?> requester) {
		MarshalReader reader = OAFParameterSet.load_file_as_json (filename,requester);
		return (new ServerConfigFile()).unmarshal (reader, null);
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ServerConfigFile : Missing subcommand");
			return;
		}

		// Subcommand : Test #1
		// Command format:
		//  test1
		// Unmarshal from the configuration file, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ServerConfigFile : Invalid 'test1' subcommand");
				return;
			}

			// Read the configuration file

			ServerConfigFile action_cfg = unmarshal_config ("ServerConfig.json", ServerConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			return;
		}

		// Subcommand : Test #2
		// Command format:
		//  test2
		// Unmarshal from the configuration file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ServerConfigFile : Invalid 'test2' subcommand");
				return;
			}

			// Read the configuration file

			ServerConfigFile action_cfg = unmarshal_config ("ServerConfig.json", ServerConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			// Marshal to JSON

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			ServerConfigFile.marshal_poly (store, null, action_cfg);
			store.check_write_complete ();
			String json_string = store.get_json_string();

			System.out.println ("");
			System.out.println (json_string);

			// Unmarshal from JSON
			
			action_cfg = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			action_cfg = ServerConfigFile.unmarshal_poly (retrieve, null);
			retrieve.check_read_complete ();

			System.out.println ("");
			System.out.println (action_cfg.toString());

			return;
		}

		// Subcommand : Test #3
		// Command format:
		//  test3
		// Unmarshal from the configuration file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.
		// This differs from test #2 only in that it uses the non-static marshal methods.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ServerConfigFile : Invalid 'test3' subcommand");
				return;
			}

			// Read the configuration file

			ServerConfigFile action_cfg = unmarshal_config ("ServerConfig.json", ServerConfig.class);

			// Display it

			System.out.println (action_cfg.toString());

			// Marshal to JSON

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			action_cfg.marshal (store, null);
			store.check_write_complete ();
			String json_string = store.get_json_string();

			System.out.println ("");
			System.out.println (json_string);

			// Unmarshal from JSON
			
			action_cfg = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			action_cfg = (new ServerConfigFile()).unmarshal (retrieve, null);
			retrieve.check_read_complete ();

			System.out.println ("");
			System.out.println (action_cfg.toString());

			return;
		}

		// Unrecognized subcommand.

		System.err.println ("ServerConfigFile : Unrecognized subcommand : " + args[0]);
		return;

	}

}
