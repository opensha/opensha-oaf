package org.opensha.oaf.aafs;

import java.util.List;
import java.util.Set;

import java.io.File;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.oaf.pdl.PDLSenderConfig;
import org.opensha.oaf.pdl.PDLAwsSenderConfig;
import org.opensha.oaf.pdl.PDLAnySenderConfig;

/**
 * Configuration for AAFS server actions.
 * Author: Michael Barall 06/03/2018.
 *
 * To use, create an object of this class, and then call its methods to obtain configuration parameters.
 *
 * Parameters come from a configuration file, in the format of ServerConfigFile.
 */
public final class ServerConfig {

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

	// Name used to manage the server.

	public String get_server_name() {
		return param_set.server_name;
	}

	// Number of this server, 1 or 2.

	public int get_server_number() {
		return param_set.server_number;
	}

	// Return the number of the partner server, 1 or 2.

	public int get_partner_server_number () {
		return param_set.get_partner_server_number();
	}

	// Return true if this is a dual-server configuration.

	public boolean is_dual_server () {
		return param_set.is_dual_server();
	}

	// List of server database handles, 0 = local, 1 = server #1, 2 = server #2.

	public List<String> get_server_db_handles() {
		return param_set.server_db_handles;
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

	// Pattern for a diagnostic filename prefix, in the format of SimpleDateFormat. [v2]

	public String get_diag_fn_prefix() {
		return param_set.diag_fn_prefix;
	}

	// Lower limit of diag file sequence number, inclusive, must be >= 0. [v2]

	public int get_diag_seq_lo() {
		return param_set.diag_seq_lo;
	}

	// Upper limit of diag file sequence number, exclusive, must be >= diag_seq_lo. [v2]

	public int get_diag_seq_hi() {
		return param_set.diag_seq_hi;
	}

	// Pattern for a forecast filename prefix, in the format of SimpleDateFormat. [v2]

	public String get_forecast_fn_prefix() {
		return param_set.forecast_fn_prefix;
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

	public String get_pdl_enable_as_string () {
		return ServerConfigFile.get_pdlopt_as_string (get_pdl_enable());
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

	// PDL target option.

	public int get_pdl_target() {
		return param_set.pdl_target;
	}

	// Get the currently selected list of PDL senders.
	// This returns a copy of the list, so the original cannot be modified.

	public List<PDLAnySenderConfig> get_pdl_senders() {
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

	// Get true if PDL is configured to be down, false if not.

	public boolean get_is_pdl_down () {
		return param_set.get_is_pdl_down();
	}

	// Get true if PDL senders should sign products.
	// We return true if there is at least one sender and all are able to sign products.

	public boolean get_is_pdl_sender_sign () {
		List<PDLAnySenderConfig> sender_list = get_pdl_senders();
		if (sender_list.isEmpty()) {
			return false;
		}
		for (PDLAnySenderConfig sender : sender_list) {
			if (!( sender.sender_can_sign() )) {
				return false;
			}
		}
		return true;
	}

	// Get true if we should sign PDL products.
	// We return true if senders do not sign products.

	public boolean get_is_pdl_we_sign () {
		return !(get_is_pdl_sender_sign());
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




	// Set the operational mode for PDL and event-sequence.
	// Parameters:
	//  opmode = Desired operational mode.
	//    The ones digit selects the PDL destination:
	//      0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
	//    The tens digit selects the event-sequence configuration:
	//      00 = default, 10 = report, 20 = delete, 30 = no report, 40 = disable
	//    The hundreds digit selects ETAS enable:
	//      000 = default, 100 = disable ETAS (force R&J), 200 = enable ETAS
	//  key_filename = PDL signing key filename.
	//    If null, blank, "-", or omitted, the default is not changed.
	// Throws an exception if invalid arguments.
	// Note: This function is for testing purposes.
	// Note: The mode is reset to default if the server or action configuration file is reloaded.

	public static void set_opmode (int opmode) {
		set_opmode (opmode, null);
		return;
	}

	public static void set_opmode (int opmode, String key_filename) {

		ServerConfig server_config = new ServerConfig();
		ActionConfig action_config = new ActionConfig();

		// PDL option

		int pdlopt = opmode % 10;
		if (!( pdlopt >= ServerConfigFile.PDLOPT_MIN && pdlopt <= ServerConfigFile.PDLOPT_MAX )) {
			throw new IllegalArgumentException ("ServerConfig.set_opmode: Invalid mode (PDL): opmode = " + opmode);
		}
			
		server_config.get_server_config_file().pdl_enable = pdlopt;

		// Event-sequence option

		int evsopt = (opmode % 100) - (opmode % 10);
		boolean set_evsopt = false;
		int esena = -1;
		int esrep = -1;
		switch (evsopt) {
		case 0:
			break;
		case 10:
			set_evsopt = true;
			esena = ActionConfigFile.ESENA_ENABLE;
			esrep = ActionConfigFile.ESREP_REPORT;
			break;
		case 20:
			set_evsopt = true;
			esena = ActionConfigFile.ESENA_ENABLE;
			esrep = ActionConfigFile.ESREP_DELETE;
			break;
		case 30:
			set_evsopt = true;
			esena = ActionConfigFile.ESENA_ENABLE;
			esrep = ActionConfigFile.ESREP_NO_REPORT;
			break;
		case 40:
			set_evsopt = true;
			esena = ActionConfigFile.ESENA_DISABLE;
			esrep = ActionConfigFile.ESREP_REPORT;
			break;
		default:
			throw new IllegalArgumentException ("ServerConfig.set_opmode: Invalid mode (event-sequence): opmode = " + opmode);
		}

		if (set_evsopt) {
			action_config.get_action_config_file().evseq_enable = esena;
			action_config.get_action_config_file().evseq_report = esrep;
		}

		// ETAS enable option

		int etasopt = (opmode % 1000) - (opmode % 100);
		boolean set_etasopt = false;
		int etasena = -1;
		switch (etasopt) {
		case 0:
			break;
		case 100:
			set_etasopt = true;
			etasena = ActionConfigFile.ETAS_ENA_DISABLE;
			break;
		case 200:
			set_etasopt = true;
			etasena = ActionConfigFile.ETAS_ENA_ENABLE;
			break;
		default:
			throw new IllegalArgumentException ("ServerConfig.set_opmode: Invalid mode (ETAS enable): opmode = " + opmode);
		}

		if (set_etasopt) {
			action_config.get_action_config_file().etas_enable = etasena;
		}

		// PDL key file

		if (!( key_filename == null || key_filename.isEmpty() || key_filename.equals("-") )) {

			if (!( (new File (key_filename)).canRead() )) {
				throw new IllegalArgumentException ("ServerConfig.set_opmode: Unreadable PDL key filename: key_filename = " + key_filename);
			}

			server_config.get_server_config_file().pdl_key_filename = key_filename;
		}

		return;
	}




	// Get a string describing the current operational mode.

	public static String get_opmode_as_string () {
		StringBuilder result = new StringBuilder();

		ServerConfig server_config = new ServerConfig();
		ActionConfig action_config = new ActionConfig();

		result.append ("pdl_enable = " + server_config.get_pdl_enable_as_string());
		result.append (", evseq_enable = " + action_config.get_evseq_enable_as_string());
		result.append (", evseq_report = " + action_config.get_evseq_report_as_string());
		result.append (", etas_enable = " + action_config.get_etas_enable_as_string());

		String key_filename = server_config.get_pdl_key_filename();
		if (!( key_filename == null || key_filename.isEmpty() || key_filename.equals("-") )) {
			result.append (", pdl_key_filename = " + key_filename);
		}

		return result.toString();
	}




	//----- Diagnostic file output -----




	// The current diagnostic file sequence number.
	// In order to ensure that rapid-fire errors cannot fill the disk with diagnostic files,
	// only a limited number are allowed during any execution of the program.
	// The sequence number is incorporated into the filename prefix, ensuring that all
	// filenames are unique within each run.
	// If the filename pattern contains the time, then filenames will also be unique across runs.

	private static int current_diag_seq_num = 0;


	// Build the diagnostic filename prefix.
	// Parameters:
	//  prefix_pattern = Pattern for making the prefix, in the format of SimpleDateFormat.
	//  diag_seq_lo = Lower limit of sequence number, inclusive.
	//  diag_seq_hi = Upper limit of sequence number, exclusive.
	// Returns the filename prefix, or null if none.

	private static synchronized String build_diag_filename_prefix (String prefix_pattern, int diag_seq_lo, int diag_seq_hi) {

		// Get and increment the sequence number

		int diag_seq = current_diag_seq_num;
		if (diag_seq < diag_seq_lo) {
			diag_seq = diag_seq_lo;
		}
		if (diag_seq >= diag_seq_hi) {
			return null;
		}

		current_diag_seq_num = diag_seq + 1;

		// Any exception causes a null return

		String filename_prefix = null;

		try {

			// Make the prefix

			long the_time = System.currentTimeMillis();

			SimpleDateFormat fmt = new SimpleDateFormat (prefix_pattern);
			fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
			String trial_prefix = fmt.format (new Date (the_time)) + diag_seq + "-";

			// Make the containing directory, if needed

			Path file_path = Paths.get (trial_prefix + "abcd.txt");	// trial filename
			Path dir_path = file_path.getParent();

			if (dir_path != null) {
				Files.createDirectories (dir_path);
			}

			// Return this prefix

			filename_prefix = trial_prefix;
		}
		catch (Exception e) {
			filename_prefix = null;
		}

		// Return prefix

		return filename_prefix;
	}




	// Get the filename prefix to use for writing diagnostic files.
	// Returns null if no file should be written.

	public static String get_diag_filename_prefix () {

		// No diagnostic file if we are running a GUI

		if (GUICmd.is_gui_mode()) {
			return null;
		}

		// Pattern from the configuration file

		//String prefix_pattern = "'/data/aafs/diag/'yyyy-MM'/'yyyy-MM-dd-HH-mm-ss'-'";
		//int diag_seq_lo = 100;
		//int diag_seq_hi = 200;
		ServerConfig server_config = new ServerConfig();
		String prefix_pattern = server_config.get_diag_fn_prefix();
		int diag_seq_lo = server_config.get_diag_seq_lo();
		int diag_seq_hi = server_config.get_diag_seq_hi();

		// Build the prefix, or null if none

		String filename_prefix = build_diag_filename_prefix (prefix_pattern, diag_seq_lo, diag_seq_hi);

		return filename_prefix;
	}




	//----- Forecast save file output -----




	// The most recent directory used to store forecast files, or null if none.
	// This is used to avoid calling Files.createDirectories for every file.

	private static Path last_fcsave_dir_path = null;


	// Build the forecast filename prefix.
	// Parameters:
	//  prefix_pattern = Pattern for making the prefix, in the format of SimpleDateFormat.
	// Returns the filename prefix, or null if none.

	private static synchronized String build_fcsave_filename_prefix (String prefix_pattern) {

		// Any exception causes a null return

		String filename_prefix = null;

		try {

			// Make the prefix

			long the_time = System.currentTimeMillis();

			SimpleDateFormat fmt = new SimpleDateFormat (prefix_pattern);
			fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
			String trial_prefix = fmt.format (new Date (the_time));

			// Make the containing directory, if needed

			Path file_path = Paths.get (trial_prefix + "abcd.txt");	// trial filename
			Path dir_path = file_path.getParent();

			if (dir_path != null) {
				if (!( last_fcsave_dir_path != null && dir_path.equals (last_fcsave_dir_path) )) {
					Files.createDirectories (dir_path);
					last_fcsave_dir_path = dir_path;
				}
			}

			// Return this prefix

			filename_prefix = trial_prefix;
		}
		catch (Exception e) {
			filename_prefix = null;
		}

		// Return prefix

		return filename_prefix;
	}




	// Get the filename prefix to use for writing forecast save files.
	// Returns null if no file should be written.

	public static String get_fcsave_filename_prefix () {

		// Pattern from the configuration file

		//String prefix_pattern = "'/data/aafs/forecasts/'yyyy-MM'/'yyyy-MM-dd-HH-mm-ss'-'";
		String prefix_pattern = (new ServerConfig()).get_forecast_fn_prefix();

		// Build the prefix, or null if none

		String filename_prefix = build_fcsave_filename_prefix (prefix_pattern);

		return filename_prefix;
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

			System.out.println("server_name = " + server_config.get_server_name());
			System.out.println("server_number = " + server_config.get_server_number());
			System.out.println("partner_server_number = " + server_config.get_partner_server_number());
			System.out.println("is_dual_server = " + server_config.is_dual_server());
			System.out.println("log_con_aafs = " + server_config.get_log_con_aafs());
			System.out.println("log_con_intake = " + server_config.get_log_con_intake());
			System.out.println("log_con_control = " + server_config.get_log_con_control());
			System.out.println("log_summary = " + server_config.get_log_summary());
			System.out.println("diag_fn_prefix = " + server_config.get_diag_fn_prefix());
			System.out.println("diag_seq_lo = " + server_config.get_diag_seq_lo());
			System.out.println("diag_seq_hi = " + server_config.get_diag_seq_hi());
			System.out.println("forecast_fn_prefix = " + server_config.get_forecast_fn_prefix());
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
			System.out.println("pdl_target = " + server_config.get_pdl_target());

			List<PDLAnySenderConfig> pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to development, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DEV) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to production, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_PROD;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (PROD) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to simulated development, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_SIM_DEV;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (SIM DEV) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to simulated production, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_SIM_PROD;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (SIM PROD) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to down development, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DOWN_DEV;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DOWN DEV) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to down production, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DOWN_PROD;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DOWN PROD) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to development + socket, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_SOCKET;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DEV, SOCKET) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to development + aws, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_AWS;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DEV, AWS) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to development + both, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_BOTH;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (DEV, BOTH) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to production + socket, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_PROD;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_SOCKET;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (PROD, SOCKET) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to production + aws, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_PROD;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_AWS;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (PROD, AWS) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			// Adjust PDL enable to production + both, and display senders

			server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_PROD;
			server_config.get_server_config_file().pdl_target = ServerConfigFile.PDLTARG_BOTH;
			pdl_senders = server_config.get_pdl_senders();
			System.out.println("pdl_senders (PROD, BOTH) = [");
			for (int i = 0; i < pdl_senders.size(); ++i) {
				PDLAnySenderConfig pdl_sender = pdl_senders.get(i);
				System.out.println("  " + i + ":  " + pdl_sender.toString());
			}
			System.out.println("]");
			System.out.println("is_pdl_permitted = " + server_config.get_is_pdl_permitted());
			System.out.println("is_pdl_readback_prod = " + server_config.get_is_pdl_readback_prod());
			System.out.println("is_pdl_down = " + server_config.get_is_pdl_down());
			System.out.println("is_pdl_sender_sign = " + server_config.get_is_pdl_sender_sign());
			System.out.println("is_pdl_we_sign = " + server_config.get_is_pdl_we_sign());

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  count
		// Make a diagnostic filename prefix, the requested number of times.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 1 additional argument

			if (args.length != 2) {
				System.err.println ("ServerConfig : Invalid 'test3' subcommand");
				return;
			}

			try {
				int count = Integer.parseInt(args[1]);

				for (int n = 0; n < count; ++n) {
					String filename_prefix = ServerConfig.get_diag_filename_prefix();
					if (filename_prefix == null) {
						System.out.println (n + ": " + "<null>");
					} else {
						System.out.println (n + ": " + filename_prefix);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  count
		// Make a forecast filename prefix, the requested number of times.
		// Delay 5 seconds in between prefixes.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 1 additional argument

			if (args.length != 2) {
				System.err.println ("ServerConfig : Invalid 'test4' subcommand");
				return;
			}

			try {
				int count = Integer.parseInt(args[1]);

				for (int n = 0; n < count; ++n) {
					if (n != 0) {
						try {
							Thread.sleep (5000L);	// wait 5 seconds
						} catch (InterruptedException e) {
						}
					}
					String filename_prefix = ServerConfig.get_fcsave_filename_prefix();
					if (filename_prefix == null) {
						System.out.println (n + ": " + "<null>");
					} else {
						System.out.println (n + ": " + filename_prefix);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ServerConfig : Unrecognized subcommand : " + args[0]);
		return;

	}

}
