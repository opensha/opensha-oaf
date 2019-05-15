package org.opensha.oaf.aafs;

import java.util.List;
import java.util.Set;

import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.oaf.pdl.PDLSenderConfig;

/**
 * Configuration for AAFS server actions.
 * Author: Michael Barall 06/03/2018.
 *
 * To use, create an object of this class, and then call its methods to obtain configuration parameters.
 *
 * Parameters come from a configuration file, in the format of ServerConfigFile.
 */
public class ServerConfig {

	//----- Parameter set -----

	// Cached parameter set.

	private static ServerConfigFile cached_param_set = null;

	// Parameter set.

	private ServerConfigFile param_set;

	// Get the parameter set.

	private static synchronized ServerConfigFile get_param_set () {

		// If we have a cached parameter set, return it

		if (cached_param_set != null) {
			return cached_param_set;
		}

		// Working data

		ServerConfigFile wk_param_set = null;

		// Any error reading the parameters aborts the program

		try {

			// Read the configuation file

			wk_param_set = ServerConfigFile.unmarshal_config ("ServerConfig.json", ServerConfig.class);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ServerConfig: Error loading parameter file ServerConfig.json, unable to continue");
			System.exit(0);
			//throw new RuntimeException("ServerConfig: Error loading parameter file ServerConfig.json", e);
		}

		// Save the parameter set

		cached_param_set = wk_param_set;
		return cached_param_set;
	}

	// unload_data - Remove the cached data from memory.
	// The data will be reloaded the next time one of these objects is created.
	// Any existing objects will continue to use the old data.
	// This makes it possible to load new parameter values without restarting the program.

	public static synchronized void unload_data () {
		cached_param_set = null;
		return;
	}


	//----- Construction -----

	// Default constructor.

	public ServerConfig () {
		param_set = get_param_set ();
	}

	// Display our contents

	@Override
	public String toString() {
		return "ServerConfig:\n" + param_set.toString();
	}


	//----- Parameter access -----

	// MongoDB configuration.

	public MongoDBConfig get_mongo_config() {
		return param_set.mongo_config;
	}

	// ActiveMQ host name or IP address.

	public String getActivemq_host() {
		return param_set.activemq_host;
	}

	// ActiveMQ port number.

	public String getActivemq_port() {
		return String.valueOf (param_set.activemq_port);
	}

	// ActiveMQ user name.

	public String getActivemq_user() {
		return param_set.activemq_user;
	}

	// ActiveMQ password.

	public String getActivemq_password() {
		return param_set.activemq_password;
	}

	// Pattern for AAFS console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String get_log_con_aafs() {
		return param_set.log_con_aafs;
	}

	// Pattern for intake console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String get_log_con_intake() {
		return param_set.log_con_intake;
	}

	// Pattern for control console log filenames, in the format of SimpleDateFormat, or "" if none.

	public String get_log_con_control() {
		return param_set.log_con_control;
	}

	// Pattern for summary log filenames, in the format of SimpleDateFormat, or "" if none.

	public String get_log_summary() {
		return param_set.log_summary;
	}

	// Comcat URL.

	public String get_comcat_url() {
		return param_set.comcat_url;
	}

	// Real-time feed URL, or empty string to not use the feed.

	public String get_feed_url() {
		return param_set.feed_url;
	}

	// Comcat development URL.

	public String get_comcat_dev_url() {
		return param_set.comcat_dev_url;
	}

	// Real-time feed development URL, or empty string to not use the feed.

	public String get_feed_dev_url() {
		return param_set.feed_dev_url;
	}

	// Simulated error rate for Comcat.

	public double get_comcat_err_rate() {
		return param_set.comcat_err_rate;
	}

	// List of Comcat ids to exclude.

	public Set<String> get_comcat_exclude() {
		return param_set.comcat_exclude;
	}

	// Number of latitude bins for local catalog, or 0 for default.

	public int get_locat_bins() {
		return param_set.locat_bins;
	}

	// List of filenames for local catalog, empty if no local catalog.

	public List<String> get_locat_filenames() {
		return param_set.locat_filenames;
	}

	// PDL intake blocking option: 0 = don't block, 1 = block.

	public int get_block_pdl_intake() {
		return param_set.block_pdl_intake;
	}

	// Poll intake blocking option: 0 = don't block, 1 = block.

	public int get_block_poll_intake() {
		return param_set.block_poll_intake;
	}

	// Forecast content blocking option: 0 = don't block, 1 = block.

	public int get_block_fc_content() {
		return param_set.block_fc_content;
	}

	// Get true if PDL intake is blocked, false if not.

	public boolean get_is_pdl_intake_blocked () {
		return param_set.get_is_pdl_intake_blocked();
	}

	// Get true if poll intake is blocked, false if not.

	public boolean get_is_poll_intake_blocked () {
		return param_set.get_is_poll_intake_blocked();
	}

	// Get true if forecast content is blocked, false if not.

	public boolean get_is_fc_content_blocked () {
		return param_set.get_is_fc_content_blocked();
	}

	// Simulated error rate for database.

	public double get_db_err_rate() {
		return param_set.db_err_rate;
	}

	// PDL enable option.

	public int get_pdl_enable() {
		return param_set.pdl_enable;
	}

	// PDL signing key filename, can be empty string for none.

	public String get_pdl_key_filename() {
		return param_set.pdl_key_filename;
	}

	// Simulated error rate for PDL.

	public double get_pdl_err_rate() {
		return param_set.pdl_err_rate;
	}

	// Creator (source network) for OAF PDL products.

	public String get_pdl_oaf_source() {
		return param_set.pdl_oaf_source;
	}

	// Product type for OAF PDL products.

	public String get_pdl_oaf_type() {
		return param_set.pdl_oaf_type;
	}

	// Get the currently selected list of PDL senders.
	// This returns a copy of the list, so the original cannot be modified.

	public List<PDLSenderConfig> get_pdl_senders() {
		return param_set.get_pdl_senders();
	}

	// Get true if sending to PDL is permitted, false if not..

	public boolean get_is_pdl_permitted () {
		return param_set.get_is_pdl_permitted();
	}

	// Get true if readback of PDL products should come from production, false if not.

	public boolean get_is_pdl_readback_prod () {
		return param_set.get_is_pdl_readback_prod();
	}


	//----- Parameter modification -----

	// Get the internal server configuration file.
	// Note: This is provided so that the GUI can adjust parameters based on
	// command-line parameters or user input.
	// Note: Calling unload_data will revert all parameters to the values in
	// the configuration file.

	public ServerConfigFile get_server_config_file () {
		return param_set;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ServerConfig : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create an object, and display the parameters in the underlying file.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ServerConfig : Invalid 'test1' subcommand");
				return;
			}

			// Create a configuration object

			ServerConfig server_config = new ServerConfig();

			// Display it

			System.out.println (server_config.toString());

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Create an object, and display the parameters fetched through the object.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("ServerConfig : Invalid 'test2' subcommand");
				return;
			}

			// Create a configuration object

			ServerConfig server_config = new ServerConfig();

			// Display it

			System.out.println("mongo_config = {\n" + server_config.get_mongo_config().toString("  ") + "}");

			System.out.println("activemq_host = " + server_config.getActivemq_host());
			System.out.println("activemq_port = " + server_config.getActivemq_port());
			System.out.println("activemq_user = " + server_config.getActivemq_user());
			System.out.println("activemq_password = " + server_config.getActivemq_password());
			System.out.println("log_con_aafs = " + server_config.get_log_con_aafs());
			System.out.println("log_con_intake = " + server_config.get_log_con_intake());
			System.out.println("log_con_control = " + server_config.get_log_con_control());
			System.out.println("log_summary = " + server_config.get_log_summary());
			System.out.println("comcat_url = " + server_config.get_comcat_url());
			System.out.println("feed_url = " + server_config.get_feed_url());
			System.out.println("comcat_dev_url = " + server_config.get_comcat_dev_url());
			System.out.println("feed_dev_url = " + server_config.get_feed_dev_url());
			System.out.println("comcat_err_rate = " + server_config.get_comcat_err_rate());

			System.out.println("comcat_exclude = [");
			for (String s : server_config.get_comcat_exclude()) {
				System.out.println("  " + s);
			}
			System.out.println("]");

			System.out.println("locat_bins = " + server_config.get_locat_bins());

			System.out.println("locat_filenames = [");
			for (String s : server_config.get_locat_filenames()) {
				System.out.println("  " + s);
			}
			System.out.println("]");

			System.out.println("block_pdl_intake = " + server_config.get_block_pdl_intake());
			System.out.println("block_poll_intake = " + server_config.get_block_poll_intake());
			System.out.println("block_fc_content = " + server_config.get_block_fc_content());
			System.out.println("is_pdl_intake_blocked = " + server_config.get_is_pdl_intake_blocked());
			System.out.println("is_poll_intake_blocked = " + server_config.get_is_poll_intake_blocked());
			System.out.println("is_fc_content_blocked = " + server_config.get_is_fc_content_blocked());
			System.out.println("db_err_rate = " + server_config.get_db_err_rate());

			System.out.println("pdl_enable = " + server_config.get_pdl_enable());
			System.out.println("pdl_key_filename = " + server_config.get_pdl_key_filename());
			System.out.println("pdl_err_rate = " + server_config.get_pdl_err_rate());
			System.out.println("pdl_oaf_source = " + server_config.get_pdl_oaf_source());
			System.out.println("pdl_oaf_type = " + server_config.get_pdl_oaf_type());

			List<PDLSenderConfig> pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLSenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());

			// Adjust PDL enable to development, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DEV) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLSenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());

			// Adjust PDL enable to production, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_PROD;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (PROD) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLSenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());

			// Adjust PDL enable to simulated development, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_SIM_DEV;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (SIM DEV) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLSenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());

			// Adjust PDL enable to simulated production, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_SIM_PROD;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (SIM PROD) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLSenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ServerConfig : Unrecognized subcommand : " + args[0]);
		return;

	}

}
