package org.opensha.oaf.aafs;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;

import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.USGS_ForecastHolder;


// PDL command-line interface.
// Author: Michael Barall 07/23/2018.
//
// Revised to support event-sequence, support sending forecast_data.json,
// and use sending routines in PDLSupport.

// This class contains a static routine that interprets command-line options.
// Then it sets the PDL configuration, and/or sends a PDL product.
//
// To select the PDL destination, use one of these:
// --pdl=dryrun
// --pdl=dev
// --pdl=prod
// --pdl=simdev
// --pdl=simprod
// --pdl=downdev
// --pdl=downprod
// If the --pdl option is not specified, the default is taken from the pdl_default function parameter;
// if pdl_default is not specified then the default is taken from the server configuration file.
//
// To specify a PDL key file, use:
// --privateKey=PRIVATEKEYFILE
// If the --privateKey option is not specified, then products are sent unsigned.
//
// To select the PDL target (which sender to use), use one of these:
// --target=socket
// --target=aws
// --target=both
// If the --target option is not specified, the default is taken from the server configuration file.
// This option exists primarily for testing.
//
// To select the event-sequence configuration option, use one of these:
// --evseq-config=enable
// --evseq-config=disable
// If the --evseq-config option is not specified, the default is taken from the action configuration file.
// This option exists primarily for testing.
//
// To select the ETAS configuration option, use one of these:
// --etas-config=enable
// --etas-config=disable
// If the --etas-config option is not specified, the default is taken from the action configuration file.
// This option exists primarily for testing.
//
// To delete a product, then in addition to the above you should also include all of these:
// --delete
// --eventid=EVENTID
// --evseq=EVSEQOPTION         [optional, defaults to "delete"]
// --lookbacktime=EVSLOOKBACK  [optional, defaults to 30 days]
// --captime=EVSEQCAPTIME      [required when --evseq=cap, ignored otherwise]
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [deprecated, see below]
// --eventsourcecode=EVENTCODE [deprecated, see below]
// --source=PRODUCTSOURCE      [optional, defaults to configured value, which should be "us"]
// --type=PRODUCTTYPE          [optional, defaults to configured value, which should be "oaf"]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// The value of --evseq must be "delete" (to delete any eventy-sequence product), "ignore" (to ignore any
//  event-sequence product), or "cap" (to cap an existing event-sequence product).  If "cap", then
//  --captime must be used to specify the end time of the sequence, preferably in ISO8601 format, although
//  other formats are recognized.  If "delete" then an existing event-sequence product will be deleted
//  even if there is no existing OAF product.
// The value of --lookbacktime is the time, in days before the mainshock, when a sequence begins.
// If --eventid is omitted, but --eventsource and --eventsourcecode are given, then the event ID is
//  constructed by concatenating --eventsource and --eventsourcecode.
// The optional parameters --source and --type identify the source (typically a network) and type of the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the deletion has been reviewed.
//
// If a forecast.json file exists on disk, then it can be sent as a product by including:
// --update=JSONFILENAME
// --eventid=EVENTID
// --evseq=EVSEQOPTION         [optional, defaults to "update"]
// --lookbacktime=EVSLOOKBACK  [optional, defaults to 30 days]
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [deprecated, see below]
// --eventsourcecode=EVENTCODE [deprecated, see below]
// --source=PRODUCTSOURCE      [optional, defaults to configured value, which should be "us"]
// --type=PRODUCTTYPE          [optional, defaults to configured value, which should be "oaf"]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// The value of --evseq must be "update" (to send an event-sequence product), "delete" (to delete any existing
//  event-sequence product), or "ignore" (to neither send nor delete an event-sequence product).
// The value of --lookbacktime is the time, in days before the mainshock, when a sequence begins.
// If --eventid is omitted, but --eventsource and --eventsourcecode are given, then the event ID is
//  constructed by concatenating --eventsource and --eventsourcecode.
// The optional parameters --source and --type identify the source (typically a network) and type of the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.
//
// If a forecast_data.json file exists on disk, then it can be sent as a product by including:
// --send=JSONFILENAME
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [obsolote, ignored]
// --eventsourcecode=EVENTCODE [obsolote, ignored]
// --source=PRODUCTSOURCE      [optional, defaults to configured value, which should be "us"]
// --type=PRODUCTTYPE          [optional, defaults to configured value, which should be "oaf"]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// All information, including the event ID and the event-sequence option, is taken from the forecast_data.json file.
// The optional parameters --source and --type identify the source (typically a network) and type of the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.
//
// The command line is considered to be "consumed" if a product or delete was sent to PDL,
// or if an error was reported to the user.  If the command line is consumed, then the caller
// should exit without taking further action.  If the command line is not consumed, then the
// caller should continue normally;  this permits the user to configure PDL access.

public class PDLCmd {


	// Parameter names

	public static final String PNAME_PDL = "--pdl";						// PDL access option
	public static final String PNAME_KEYFILE = "--privateKey";			// File containing private key
	public static final String PNAME_CODE = "--code";					// Product identifier [obsolote, ignored]
	public static final String PNAME_EVENT_NETWORK = "--eventsource";	// Network for the event [deprecated]
	public static final String PNAME_EVENT_CODE = "--eventsourcecode";	// Network's code for the event [deprecated]
	public static final String PNAME_UPDATE = "--update";				// Send product update (forecast.json)
	public static final String PNAME_DELETE = "--delete";				// Delete product
	public static final String PNAME_SOURCE = "--source";				// Product source code (network code)
	public static final String PNAME_TYPE = "--type";					// Product type
	public static final String PNAME_REVIEWED = "--reviewed";			// Product is reviewed

	public static final String PNAME_EVENT_ID = "--eventid";			// Event ID
	public static final String PNAME_EVSEQ = "--evseq";					// Event-sequence options
	public static final String PNAME_LOOKBACK_TIME = "--lookbacktime";	// Event-sequence lookback time in days
	public static final String PNAME_CAP_TIME = "--captime";			// Event-sequence cap date/time
	public static final String PNAME_SEND = "--send";					// Send product update (forecast_data.json)

	public static final String PNAME_TARGET = "--target";				// PDL target option

	public static final String PNAME_EVSEQ_CONFIG = "--evseq-config";	// Event-sequence configuration option
	public static final String PNAME_ETAS_CONFIG = "--etas-config";		// ETAS configuration option

	// Values for the PNAME_PDL parameter

	public static final String PVAL_DRYRUN = "dryrun";					// No PDL access (dry run)
	public static final String PVAL_DEV = "dev";						// PDL development server
	public static final String PVAL_PROD = "prod";						// PDL production server
	public static final String PVAL_SIM_DEV = "simdev";					// Simulated PDL development server
	public static final String PVAL_SIM_PROD = "simprod";				// Simulated PDL production server
	public static final String PVAL_DOWN_DEV = "downdev";				// Down PDL development server
	public static final String PVAL_DOWN_PROD = "downprod";				// Down PDL production server

	// Values for PNAME_EVSEQ parameter

	public static final String PVAL_EVS_UPDATE = "update";				// Update (send) event-sequence product
	public static final String PVAL_EVS_IGNORE = "ignore";				// Ignore (neither send nor delete) event-sequence product
	public static final String PVAL_EVS_DELETE = "delete";				// Delete event-sequence product
	public static final String PVAL_EVS_CAP = "cap";					// Cap (change end time of) event-sequence product

	// Values for the PNAME_TARGET parameter

	public static final String PVAL_TARG_SOCKET = "socket";				// SocketProductSender
	public static final String PVAL_TARG_AWS = "aws";					// AwsProductSender
	public static final String PVAL_TARG_BOTH = "both";					// Both

	// Values for PNAME_EVSEQ_CONFIG parameter

	public static final String PVAL_EVSCFG_ENABLE = "enable";			// Enable event-sequence product
	public static final String PVAL_EVSCFG_DISABLE = "disable";			// Disable event-sequence product

	// Values for PNAME_ETAS_CONFIG parameter

	public static final String PVAL_ETASCFG_ENABLE = "enable";			// Enable ETAS forecasts
	public static final String PVAL_ETASCFG_DISABLE = "disable";		// Disable ETAS forecasts

	// String for splitting parameter into name and value

	public static final String PSPLIT = "=";




	// Execute PDL command line.
	// Parameters:
	//  args = Command line parameters.
	//  lo = Index of first argument to process.  Ignore args[0] thru args[lo-1].
	//  f_config = True to permit updating the ServerConfig without sending anything.
	//  f_send = True to permit sending to PDL.
	//  pdl_default = Default PDL enable option, as defined in ServerConfigFile.
	//                Can be PDLOPT_UNSPECIFIED to use the configured value.
	// Returns true if the command has been consumed.  This is always possible even if
	//  f_send is false (e.g., if an error was reported to the user).
	// Returns false if ServerConfig has been updated without sending anything.  This is
	//  possible only if f_config is true.

	public static boolean exec_pdl_cmd (String[] args, int lo, boolean f_config, boolean f_send, int pdl_default) {
	
		// Check arguments

		if (!( args != null
			&& (lo >= 0 && lo <= args.length)
			&& (f_config || f_send)
			&& ((pdl_default >= ServerConfigFile.PDLOPT_MIN && pdl_default <= ServerConfigFile.PDLOPT_MAX)
				|| pdl_default == ServerConfigFile.PDLOPT_UNSPECIFIED) )) {
			throw new IllegalArgumentException ("PDLCmdexec_pdl_cmd: Invalid arguments");
		}

		// Parameter values

		int pdl_enable = ServerConfigFile.PDLOPT_UNSPECIFIED;
		String keyfile = null;
		String code = null;
		String event_network = null;
		String event_code = null;
		String update = null;
		boolean delete = false;
		String product_source = null;
		String product_type = null;
		boolean f_is_reviewed = true;
		boolean f_seen_reviewed = false;

		String event_id = null;
		String evseq = null;
		long lookback_time = 30L * SimpleUtils.DAY_MILLIS;	// default lookback time
		boolean f_seen_lookback_time = false;
		long cap_time = 0L;
		boolean f_seen_cap_time = false;
		String send = null;

		int pdl_target = ServerConfigFile.PDLTARG_UNSPECIFIED;

		int evseq_config = ActionConfigFile.ESENA_UNSPECIFIED;
		int etas_config = ActionConfigFile.ETAS_ENA_UNSPECIFIED;

		// Scan parameters

		for (int i = lo; i < args.length; ++i) {
		
			// Get the argument

			String arg = args[i].trim();

			// Split into name and value

			String name;
			String value;

			int splitix = arg.indexOf (PSPLIT);

			if (splitix < 0) {
				name = arg;
				value = null;
			} else {
				name = arg.substring (0, splitix).trim();
				value = arg.substring (splitix + PSPLIT.length()).trim();
			}

			// PDL access option

			if (name.equalsIgnoreCase (PNAME_PDL)) {
				if (pdl_enable != ServerConfigFile.PDLOPT_UNSPECIFIED) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				if (value.equalsIgnoreCase (PVAL_DRYRUN)) {
					pdl_enable = ServerConfigFile.PDLOPT_NONE;
				}
				else if (value.equalsIgnoreCase (PVAL_DEV)) {
					pdl_enable = ServerConfigFile.PDLOPT_DEV;
				}
				else if (value.equalsIgnoreCase (PVAL_PROD)) {
					pdl_enable = ServerConfigFile.PDLOPT_PROD;
				}
				else if (value.equalsIgnoreCase (PVAL_SIM_DEV)) {
					pdl_enable = ServerConfigFile.PDLOPT_SIM_DEV;
				}
				else if (value.equalsIgnoreCase (PVAL_SIM_PROD)) {
					pdl_enable = ServerConfigFile.PDLOPT_SIM_PROD;
				}
				else if (value.equalsIgnoreCase (PVAL_DOWN_DEV)) {
					pdl_enable = ServerConfigFile.PDLOPT_DOWN_DEV;
				}
				else if (value.equalsIgnoreCase (PVAL_DOWN_PROD)) {
					pdl_enable = ServerConfigFile.PDLOPT_DOWN_PROD;
				}
				else {
					System.out.println ("Invalid value in command-line option: " + arg);
					System.out.println ("Valid values are: " + PVAL_DRYRUN + ", " + PVAL_DEV + ", " + PVAL_PROD + ", " + PVAL_SIM_DEV + ", " + PVAL_SIM_PROD + ", " + PVAL_DOWN_DEV + ", " + PVAL_DOWN_PROD);
					return true;
				}
			}

			// PDL target option

			else if (name.equalsIgnoreCase (PNAME_TARGET)) {
				if (pdl_target != ServerConfigFile.PDLTARG_UNSPECIFIED) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				if (value.equalsIgnoreCase (PVAL_TARG_SOCKET)) {
					pdl_target = ServerConfigFile.PDLTARG_SOCKET;
				}
				else if (value.equalsIgnoreCase (PVAL_TARG_AWS)) {
					pdl_target = ServerConfigFile.PDLTARG_AWS;
				}
				else if (value.equalsIgnoreCase (PVAL_TARG_BOTH)) {
					pdl_target = ServerConfigFile.PDLTARG_BOTH;
				}
				else {
					System.out.println ("Invalid value in command-line option: " + arg);
					System.out.println ("Valid values are: " + PVAL_TARG_SOCKET + ", " + PVAL_TARG_AWS + ", " + PVAL_TARG_BOTH);
					return true;
				}
			}

			// Key file option

			else if (name.equalsIgnoreCase (PNAME_KEYFILE)) {
				if (keyfile != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				keyfile = value;
				if (!( Files.exists (Paths.get (keyfile)) )) {
					System.out.println ("Key file does not exist: " + keyfile);
					return true;
				}
			}

			// Product identifier option

			else if (name.equalsIgnoreCase (PNAME_CODE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (code != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				code = value;
			}

			// Event network option

			else if (name.equalsIgnoreCase (PNAME_EVENT_NETWORK)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (event_network != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				event_network = value;
			}

			// Event code option

			else if (name.equalsIgnoreCase (PNAME_EVENT_CODE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (event_code != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				event_code = value;
			}

			// Product source option

			else if (name.equalsIgnoreCase (PNAME_SOURCE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (product_source != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				product_source = value;
			}

			// Product type option

			else if (name.equalsIgnoreCase (PNAME_TYPE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (product_type != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				product_type = value;
			}

			// Product reviewed option

			else if (name.equalsIgnoreCase (PNAME_REVIEWED)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (f_seen_reviewed) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				if (value.equalsIgnoreCase ("true")) {
					f_is_reviewed = true;
				} else if (value.equalsIgnoreCase ("false")) {
					f_is_reviewed = false;
				} else {
					System.out.println ("Invalid value in command-line option: " + arg);
					return true;
				}
				f_seen_reviewed = true;
			}

			// Event ID option

			else if (name.equalsIgnoreCase (PNAME_EVENT_ID)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (event_id != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				event_id = value;
			}

			// Event-sequence option

			else if (name.equalsIgnoreCase (PNAME_EVSEQ)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (evseq != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				evseq = value;
			}

			// Lookback time option

			else if (name.equalsIgnoreCase (PNAME_LOOKBACK_TIME)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (f_seen_lookback_time) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				try {
					lookback_time = SimpleUtils.days_to_millis (Double.parseDouble (value));
				} catch (Exception e) {
					System.out.println ("Invalid value in command-line option: " + arg);
					return true;
				}
				f_seen_lookback_time = true;
			}

			// Cap time option

			else if (name.equalsIgnoreCase (PNAME_CAP_TIME)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (f_seen_cap_time) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				try {
					cap_time = SimpleUtils.string_to_time_permissive (value);
				} catch (Exception e) {
					System.out.println ("Invalid value in command-line option: " + arg);
					return true;
				}
				f_seen_cap_time = true;
			}

			// Event-sequence configuration option

			else if (name.equalsIgnoreCase (PNAME_EVSEQ_CONFIG)) {
				if (evseq_config != ActionConfigFile.ESENA_UNSPECIFIED) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				if (value.equalsIgnoreCase (PVAL_EVSCFG_ENABLE)) {
					evseq_config = ActionConfigFile.ESENA_ENABLE;
				}
				else if (value.equalsIgnoreCase (PVAL_EVSCFG_DISABLE)) {
					evseq_config = ActionConfigFile.ESENA_DISABLE;
				}
				else {
					System.out.println ("Invalid value in command-line option: " + arg);
					System.out.println ("Valid values are: " + PVAL_EVSCFG_ENABLE + ", " + PVAL_EVSCFG_DISABLE);
					return true;
				}
			}

			// ETAS configuration option

			else if (name.equalsIgnoreCase (PNAME_ETAS_CONFIG)) {
				if (etas_config != ActionConfigFile.ETAS_ENA_UNSPECIFIED) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				if (value.equalsIgnoreCase (PVAL_ETASCFG_ENABLE)) {
					etas_config = ActionConfigFile.ETAS_ENA_ENABLE;
				}
				else if (value.equalsIgnoreCase (PVAL_ETASCFG_DISABLE)) {
					etas_config = ActionConfigFile.ETAS_ENA_DISABLE;
				}
				else {
					System.out.println ("Invalid value in command-line option: " + arg);
					System.out.println ("Valid values are: " + PVAL_EVSCFG_ENABLE + ", " + PVAL_EVSCFG_DISABLE);
					return true;
				}
			}

			// Update option

			else if (name.equalsIgnoreCase (PNAME_UPDATE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (update != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (delete) {
					System.out.println ("Command-line options cannot include both " + PNAME_UPDATE + " and " + PNAME_DELETE);
					return true;
				}
				if (send != null) {
					System.out.println ("Command-line options cannot include both " + PNAME_UPDATE + " and " + PNAME_SEND);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				update = value;
				if (!( Files.exists (Paths.get (update)) )) {
					System.out.println ("Product file does not exist: " + update);
					return true;
				}
			}

			// Send option

			else if (name.equalsIgnoreCase (PNAME_SEND)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (send != null) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (delete) {
					System.out.println ("Command-line options cannot include both " + PNAME_SEND + " and " + PNAME_DELETE);
					return true;
				}
				if (update != null) {
					System.out.println ("Command-line options cannot include both " + PNAME_UPDATE + " and " + PNAME_SEND);
					return true;
				}
				if (value == null || value.isEmpty()) {
					System.out.println ("Missing value in command-line option: " + arg);
					return true;
				}
				send = value;
				if (!( Files.exists (Paths.get (send)) )) {
					System.out.println ("Product file does not exist: " + send);
					return true;
				}
			}

			// Delete option

			else if (name.equalsIgnoreCase (PNAME_DELETE)) {
				if (!( f_send )) {
					System.out.println ("Unrecognized command-line option: " + arg);
					return true;
				}
				if (delete) {
					System.out.println ("Duplicate command-line option: " + arg);
					return true;
				}
				if (update != null) {
					System.out.println ("Command-line options cannot include both " + PNAME_UPDATE + " and " + PNAME_DELETE);
					return true;
				}
				if (send != null) {
					System.out.println ("Command-line options cannot include both " + PNAME_SEND + " and " + PNAME_DELETE);
					return true;
				}
				if (!( value == null || value.isEmpty() )) {
					System.out.println ("Command-line option " + PNAME_DELETE + " may not have a value: " + arg);
					return true;
				}
				delete = true;
			}

			// Unrecognized option

			else {
				System.out.println ("Unrecognized command-line option: " + arg);
				return true;
			}
		}

		// If no PDL access option, apply default

		if (pdl_enable == ServerConfigFile.PDLOPT_UNSPECIFIED) {
			pdl_enable = pdl_default;
		}

		// If config-only is not permitted, then we must have either update, send, or delete

		if (!( f_config )) {
			if (!( delete || update != null || send != null )) {
				System.out.println ("Command-line options must include one of " + PNAME_UPDATE + ", " + PNAME_SEND + ", or " + PNAME_DELETE);
				return true;
			}
		}

		// If event id is not specified, but network and code are, construct the event id from network and code

		if (event_id == null && event_network != null && event_code != null) {
			event_id = event_network + event_code;
		}

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// If PDL access is specified (including by default), enter it into server configuration

		if (pdl_enable != ServerConfigFile.PDLOPT_UNSPECIFIED) {
			server_config.get_server_config_file().pdl_enable = pdl_enable;
		}

		// If PDL target is specified, enter it into server configuration

		if (pdl_target != ServerConfigFile.PDLTARG_UNSPECIFIED) {
			server_config.get_server_config_file().pdl_target = pdl_target;
		}

		// If key file is specified, enter it into server configuration

		if (keyfile != null) {
			server_config.get_server_config_file().pdl_key_filename = keyfile;
		}

		// If product source is specified, enter it into server configuration

		if (product_source != null) {
			server_config.get_server_config_file().pdl_oaf_source = product_source;
		}

		// If product type is specified, enter it into server configuration

		if (product_type != null) {
			server_config.get_server_config_file().pdl_oaf_type = product_type;
		}

		// Action configuration

		ActionConfig action_config = new ActionConfig();

		// If event-sequence configuration is specified, enter it into the action configuration

		if (evseq_config != ActionConfigFile.ESENA_UNSPECIFIED) {
			action_config.get_action_config_file().evseq_enable = evseq_config;
		}

		// If ETAS configuration is specified, enter it into the action configuration

		if (etas_config != ActionConfigFile.ETAS_ENA_UNSPECIFIED) {
			action_config.get_action_config_file().etas_enable = etas_config;
		}

		// Send update from forecast.json

		if (update != null) {

			// Check for required options

			if (!( event_id != null )) {
				System.out.println ("Cannot send PDL update because the following command-line option is missing:");
				System.out.println (PNAME_EVENT_ID);
				return true;
			}

			// Validate the event-sequence option

			if (evseq == null) {
				evseq = PVAL_EVS_UPDATE;		// default event-sequence action
			}

			if (!( evseq.equalsIgnoreCase (PVAL_EVS_UPDATE)
				|| evseq.equalsIgnoreCase (PVAL_EVS_IGNORE)
				|| evseq.equalsIgnoreCase (PVAL_EVS_DELETE) )) {
				System.out.println ("Cannot send PDL update because the following command-line option has an invalid value:");
				System.out.println (PNAME_EVSEQ);
				return true;
			}

			// Parameters for sending to PDL

			final boolean isReviewed = f_is_reviewed;
			final EventSequenceResult evseq_res = new EventSequenceResult();

			// Perform the send

			boolean send_ok = false;

			try {

				// Load the forecast.json

				final USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
				MarshalUtils.from_json_file (fc_holder, update);

				// Get the mainshock information

				final ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_poll (event_id);
				if (!( fcmain.mainshock_avail )) {
					System.out.println ("Event not found: " + event_id);
					return true;
				}

				// Get event-sequence configuration

				EventSequenceParameters evseq_cfg_params = null;
				if (evseq.equalsIgnoreCase (PVAL_EVS_IGNORE)) {
					evseq_cfg_params = EventSequenceParameters.make_coerce (
						ActionConfigFile.ESREP_NO_REPORT, lookback_time);
				}
				else if (evseq.equalsIgnoreCase (PVAL_EVS_UPDATE)) {
					evseq_cfg_params = EventSequenceParameters.make_coerce (
						ActionConfigFile.ESREP_REPORT, lookback_time);
				}
				else if (evseq.equalsIgnoreCase (PVAL_EVS_DELETE)) {
					evseq_cfg_params = EventSequenceParameters.make_coerce (
						ActionConfigFile.ESREP_DELETE, lookback_time);
				}
				else {
					throw new IllegalStateException ("Unexpected value of command-line option " + PNAME_EVSEQ);
				}

				// Send to PDL

				PDLSupport.static_send_pdl_report (
					isReviewed,
					evseq_res,
					fcmain,
					evseq_cfg_params,
					fc_holder
				);

				// Success

				send_ok = true;
			}

			catch (Exception e) {
				System.out.println ("Exception occurred while attempting to send forecast.json to PDL.");
				e.printStackTrace();
			}

			// Inform user of result

			System.out.println ();

			if (send_ok) {
				System.out.println ("Success: Forecast has been successfully sent to PDL.");
				if (evseq_res.was_evseq_sent_ok()) {
					System.out.println ("An event-sequence product has been successfully sent to PDL.");
				} else if (evseq_res.was_evseq_deleted()) {
					System.out.println ("An existing event-sequence product has been successfully deleted.");
				} else if (evseq_res.was_evseq_capped()) {
					System.out.println ("An existing event-sequence product has been successfully capped.");
				}
			} else {
				System.out.println ("Error: PDL update was NOT sent successfully.");
			}
		
			return true;
		}

		// Send update from forecast_data.json

		if (send != null) {

			// Check for incompatible options

			if (!( event_id == null && evseq == null && event_network == null && event_code == null )) {
				System.out.println ("The following command-line options cannot be used together with " + PNAME_SEND + ":");
				System.out.println (PNAME_EVENT_ID + ", " + PNAME_EVSEQ + ", " + PNAME_EVENT_NETWORK + ", " + PNAME_EVENT_CODE);
				return true;
			}

			// Parameters for sending to PDL

			final boolean isReviewed = f_is_reviewed;
			final EventSequenceResult evseq_res = new EventSequenceResult();

			// Perform the send

			boolean send_ok = false;

			try {

				// Load the ForecastData

				final ForecastData fcdata = new ForecastData();
				MarshalUtils.from_json_file (fcdata, send);

				// Set for not sent to PDL

				fcdata.pdl_event_id = "";
				fcdata.pdl_is_reviewed = false;

				// Send to PDL

				String resulting_event_id = PDLSupport.static_send_pdl_report (
					isReviewed,
					evseq_res,
					fcdata
				);

				// Success

				send_ok = true;
			}

			catch (Exception e) {
				System.out.println ("Exception occurred while attempting to send forecast to PDL.");
				e.printStackTrace();
			}

			// Inform user of result

			System.out.println ();

			if (send_ok) {
				System.out.println ("Success: Forecast has been successfully sent to PDL.");
				if (evseq_res.was_evseq_sent_ok()) {
					System.out.println ("An event-sequence product has been successfully sent to PDL.");
				} else if (evseq_res.was_evseq_deleted()) {
					System.out.println ("An existing event-sequence product has been successfully deleted.");
				} else if (evseq_res.was_evseq_capped()) {
					System.out.println ("An existing event-sequence product has been successfully capped.");
				}
			} else {
				System.out.println ("Error: PDL update was NOT sent successfully.");
			}
		
			return true;
		}

		// Send delete

		if (delete) {

			// Check for required options

			if (!( event_id != null )) {
				System.out.println ("Cannot send PDL delete because the following command-line option is missing:");
				System.out.println (PNAME_EVENT_ID);
				return true;
			}

			// Validate the event-sequence option and adjust cap time

			if (evseq == null) {
				evseq = PVAL_EVS_DELETE;		// default event-sequence action
			}

			if (evseq.equalsIgnoreCase (PVAL_EVS_IGNORE)) {
				if (f_seen_cap_time) {
					System.out.println ("Command line option " + PNAME_CAP_TIME + " cannot be used together with " + PNAME_EVSEQ + "=" + PVAL_EVS_IGNORE);
					return true;
				}
				cap_time = PDLCodeChooserEventSequence.CAP_TIME_NOP;
			}
			else if (evseq.equalsIgnoreCase (PVAL_EVS_DELETE)) {
				if (f_seen_cap_time) {
					System.out.println ("Command line option " + PNAME_CAP_TIME + " cannot be used together with " + PNAME_EVSEQ + "=" + PVAL_EVS_DELETE);
					return true;
				}
				cap_time = PDLCodeChooserEventSequence.CAP_TIME_DELETE;
			}
			else if (evseq.equalsIgnoreCase (PVAL_EVS_CAP)) {
				if (!( f_seen_cap_time )) {
					System.out.println ("Cannot send PDL delete because the following command-line option is missing:");
					System.out.println (PNAME_CAP_TIME);
					return true;
				}
			}
			else {
				System.out.println ("Cannot send PDL delete because the following command-line option has an invalid value:");
				System.out.println (PNAME_EVSEQ);
				return true;
			}

			// Parameters for sending to PDL

			final boolean isReviewed = f_is_reviewed;
			final boolean f_keep_reviewed = false;

			// This receives the delete result:
			//   del_result[0] = True if an oaf product was deleted.
			//   del_result[1] = True if an event-sequence product was deleted.
			//   del_result[2] = True if an event-sequence product was capped.

			boolean[] del_result = null;

			// Perform the send

			boolean send_ok = false;

			try {

				// Get the mainshock information

				final ForecastMainshock fcmain = new ForecastMainshock();
				fcmain.setup_mainshock_poll (event_id);
				if (!( fcmain.mainshock_avail )) {
					System.out.println ("Event not found: " + event_id);
					return true;
				}

				// Validate the cap time

				if (evseq.equalsIgnoreCase (PVAL_EVS_CAP)) {
					long time_now = System.currentTimeMillis();
					long mainshock_time = fcmain.mainshock_time;
					if (cap_time <= mainshock_time) {
						System.out.println ("Event-sequence cap time cannot be before the mainshock time");
						return true;
					}
					if (cap_time > mainshock_time + (SimpleUtils.DAY_MILLIS * 365L * 12L)) {
						System.out.println ("Event-sequence cap time cannot be more than 12 years after the mainshock time");
						return true;
					}
					if (cap_time > time_now) {
						System.out.println ("Event-sequence cap time cannot be after the current time");
						return true;
					}
				}

				// Send delete

				del_result = PDLSupport.static_delete_oaf_products (
					fcmain,
					isReviewed,
					cap_time,
					f_keep_reviewed,
					null		// gj_holder
				);

				// Success

				send_ok = true;
			}

			catch (Exception e) {
				System.out.println ("Exception occurred while attempting to send delete to PDL.");
				e.printStackTrace();
			}

			// Inform user of result

			System.out.println ();

			if (send_ok) {
				if (del_result[0] || del_result[1] || del_result[2]) {
					if (del_result[0]) {
						System.out.println ("Success: An OAF DELETE product has been successfully sent to PDL.");
					}
					if (del_result[1]) {
						System.out.println ("Success: An existing event-sequence product has been successfully deleted.");
					} else if (del_result[2]) {
						System.out.println ("Success: An existing event-sequence product has been successfully capped.");
					}
				} else {
					System.out.println ("Success, but no products were deleted.");
				}
			} else {
				System.out.println ("PDL delete was NOT sent successfully.");
			}
		
			return true;
		}

		// We only adjusted the configuration

		return false;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// Execute PDL command

		int lo = 0;
		boolean f_config = false;
		boolean f_send = true;
		int pdl_default = ServerConfigFile.PDLOPT_UNSPECIFIED;

		exec_pdl_cmd (args, lo, f_config, f_send, pdl_default);
		return;
	}
}
