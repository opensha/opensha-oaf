package org.opensha.oaf.aafs;


/**
 * GUI command-line interface.
 * Author: Michael Barall 07/23/2018.
 */

// This class contains a static routine that interprets command-line options.
// It may execute a server management command, execute a utility command,
// send or delete a PDL product, set the configuration, or do nothing.
// 
// This defines the common set of command-line operations that are supported
// by any of the aftershock GUIs.
//
// The command line is considered to be "consumed" if this routine performs the requested action,
// or if an error was reported to the user.  If the command line is consumed, then the caller
// should exit without taking further action.  If the command line is not consumed, then the
// caller should continue normally;  this permits the user to set configuration options.
//
//
// ***** Server Management commands *****
//
// These command lines execute server management commands.
// They assume that the GUI contains the connection information for
// the running servers, which may be single-server or dual-server.
// After performing the server management command, the program exits,
// that is, the GUI is not run.
//
//
// Display the server health, on the two running servers.
// Command format:
//  server_health
//
//
// Display detailed server status information, on the two running servers.
// Command format:
//  server_status
//
//
// Run the analyst CLI, and send the selected options to the two running servers.
// Command format:
//  analyst_cli
//
//
// ***** Utility commands *****
//
// These command lines perform utility functions.
// They do not interact with the servers.
//
//
// Display the software version number.
// Command formats:
//  version
//  --version
//
//
// Convert a forecast data file from the website/database JSON
// format to a friendly human-understandable JSON format.
// Command format:
//  fcdata_convert  in_filename  out_filename
//
//
// ***** Configuration commands *****
//
// These commands control program configuration options.
// Currently, these control how to send products to PDL,
// and allow for enabling event-sequence and ETAS.
// If used alone, they affect the operation of the GUI.
// If used in combination with one of the PDL commands,
// they affect the operation of the command.
// In the future, other types of configuration commands may be added.
//
//
// To select the PDL destination, include one of these on the command line:
// --pdl=dryrun
// --pdl=dev
// --pdl=prod
// --pdl=simdev
// --pdl=simprod
// --pdl=downdev
// --pdl=downprod
// If the --pdl option is not specified, the default is dryrun.
//
//
// To specify a PDL key file, use:
// --privateKey=PRIVATEKEYFILE
// If the --privateKey option is not specified, then products are sent unsigned.
//
//
// To select the PDL target (which sender to use), use one of these:
// --target=socket
// --target=aws
// --target=both
// If the --target option is not specified, the default is taken from the server configuration file.
// This option exists primarily for testing.
//
//
// To specify the product source for sending to PDL (typically a seismic network), use:
// --source=PRODUCTSOURCE
// If the --source option is not specified, it  defaults to configured value, which should be "us".
//
//
// To specify the aftershock forecast product type for sending to PDL, use:
// --type=PRODUCTTYPE
// If the --type option is not specified, it defaults to configured value, which should be "oaf".
//
//
// To select the event-sequence configuration option, use one of these:
// --evseq-config=enable
// --evseq-config=disable
// If the --evseq-config option is not specified, the default is taken from the action configuration file.
// This option exists primarily for testing.
//
//
// To select the ETAS configuration option, use one of these:
// --etas-config=enable
// --etas-config=disable
// If the --etas-config option is not specified, the default is taken from the action configuration file.
// This option exists primarily for testing.
//
//
// ***** PDL commands *****
//
// These commands can be used to send or delete a PDL product.
// The --pdl, --privateKey, and --target options may be used in combination with
// these commands to specify the PDL destination and signing options.
//
//
// To delete a product, then in addition to the above you should also include these:
// --delete
// --eventid=EVENTID
// --evseq=EVSEQOPTION         [optional, defaults to "delete"]
// --lookbacktime=EVSLOOKBACK  [optional, defaults to 30 days]
// --captime=EVSEQCAPTIME      [required when --evseq=cap, ignored otherwise]
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [deprecated, see below]
// --eventsourcecode=EVENTCODE [deprecated, see below]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// The value of --evseq must be "delete" (to delete any eventy-sequence product), "ignore" (to ignore any
//  event-sequence product), or "cap" (to cap an existing event-sequence product).  If "cap", then
//  --captime must be used to specify the end time of the sequence, preferably in ISO8601 format, although
//  other formats are recognized.  If "delete" then an existing event-sequence product will be deleted
//  even if there is no existing OAF product.
// The value of --lookbacktime is the time, in days before the mainshock, when a sequence begins.
// If --eventid is omitted, but --eventsource and --eventsourcecode are given, then the event ID is
//  constructed by concatenating --eventsource and --eventsourcecode.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the deletion has been reviewed.
//
//
// If a forecast.json file exists on disk, then it can be sent as a product by including:
// --update=JSONFILENAME
// --eventid=EVENTID
// --evseq=EVSEQOPTION         [optional, defaults to "update"]
// --lookbacktime=EVSLOOKBACK  [optional, defaults to 30 days]
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [deprecated, see below]
// --eventsourcecode=EVENTCODE [deprecated, see below]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// The value of --evseq must be "update" (to send an event-sequence product), "delete" (to delete any existing
//  event-sequence product), or "ignore" (to neither send nor delete an event-sequence product).
// The value of --lookbacktime is the time, in days before the mainshock, when a sequence begins.
// If --eventid is omitted, but --eventsource and --eventsourcecode are given, then the event ID is
//  constructed by concatenating --eventsource and --eventsourcecode.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.
//
//
// If a forecast_data.json file exists on disk, then it can be sent as a product by including:
// --send=JSONFILENAME
// --code=PRODUCTCODE          [obsolote, ignored]
// --eventsource=EVENTNETWORK  [obsolote, ignored]
// --eventsourcecode=EVENTCODE [obsolote, ignored]
// --reviewed=ISREVIEWED       [optional, defaults to "true"]
// All information, including the event ID and the event-sequence option, is taken from the forecast_data.json file.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.
//
//
// To delete an OAF product directly to PDL, then in addition to the above you should also include these:
// --delete
// --direct=true
// --code=PRODUCTCODE         [optional, defaults to --eventsource concatenated with --eventsourcecode]
// --eventsource=EVENTNETWORK
// --eventsourcecode=EVENTCODE
// --reviewed=ISREVIEWED      [optional, defaults to "true"]
// The value of --code identifies the product that is to be deleted.  The value of --code is typically an event ID.
// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
// these determine which event page displays the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the deletion has been reviewed.
// An OAF DELETE product is sent directly to PDL, without calling Comcat.
// This has no effect on any event-sequence product that may exist.
// Direct delete is intended for use with scenario products, which should use --type=oaf-scenario.
//
//
// If a forecast.json file exists on disk, then it can be sent directly to PDL as a product by including:
// --update=JSONFILENAME
// --direct=true
// --code=PRODUCTCODE         [optional, defaults to --eventsource concatenated with --eventsourcecode]
// --eventsource=EVENTNETWORK
// --eventsourcecode=EVENTCODE
// --reviewed=ISREVIEWED      [optional, defaults to "true"]
// The value of --code identifies the product that is to be sent.  The value of --code is typically an event ID.
// The product replaces any prior product that was sent with the same --code.
// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
// these determine which event page displays the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.
// An OAF product is sent directly to PDL, without calling Comcat.
// This does not send an event-sequence product and has no effect on any event-sequence product that may exist.
// Direct update is intended for use with scenario products, which should use --type=oaf-scenario.

public class GUICmd {


	// Flag indicating if GUI debug mode is requested.

	public static boolean f_gui_debug = false;


	// Flag indicating if we are running a GUI.
	// This flag is set if exec_gui_cmd has been called.

	private static boolean f_gui_mode = false;

	public static boolean is_gui_mode () {
		return f_gui_mode;
	}




	// Execute GUI command line.
	// Parameters:
	//  args = Command line parameters.
	//  caller_name = Name of the caller; used when reporting errors to the user;
	//                cannot be null or empty or blank.
	// Returns true if the command has been consumed.
	// Returns false if the command has not been consumed. This may indicate that
	//  configuration options are changed, e.g., by updating ServerConfig.
	// After return, f_gui_debug can be examined to check for GUI debug mode.

	public static boolean exec_gui_cmd (String[] args, String caller_name) {

		f_gui_debug = false;

		f_gui_mode = true;
	
		// Check arguments

		if (!( args != null
			&& (caller_name != null && caller_name.trim().length() > 0) )) {
			throw new IllegalArgumentException ("GUICmd.exec_gui_cmd: Invalid arguments");
		}

		// The GUI accepts certain server management commands and utility commands.
		// These redirect to the corresponding commands in ServerCmd.

		if (args.length >= 1) {

			// Subcommand : server_health
			// Command format:
			//  server_health
			// Display the server health, on the running servers.

			if (args[0].equalsIgnoreCase ("server_health")) {

				if (args.length != 1) {
					System.out.println (caller_name.trim() + " : Invalid 'server_health' subcommand");
					return true;
				}

				String[] my_args = new String[2];
				my_args[0] = "server_health";
				my_args[1] = (new ServerConfig()).is_dual_server() ? "9" : "1";

				try {
					ServerCmd.cmd_server_health (my_args);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println ();
					System.out.println (caller_name.trim() + " : The 'server_health' subcommand failed with an exception");
				}
				return true;
			}

			// Subcommand : server_status
			// Command format:
			//  server_status
			// Display the detailed server status, on the running servers.

			if (args[0].equalsIgnoreCase ("server_status")) {

				if (args.length != 1) {
					System.out.println (caller_name.trim() + " : Invalid 'server_health' subcommand");
					return true;
				}

				String[] my_args = new String[2];
				my_args[0] = "show_relay_status";
				my_args[1] = (new ServerConfig()).is_dual_server() ? "9" : "1";

				try {
					ServerCmd.cmd_show_relay_status (my_args);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println ();
					System.out.println (caller_name.trim() + " : The 'server_status' subcommand failed with an exception");
				}
				return true;
			}

			// Subcommand : analyst_cli
			// Command format:
			//  analyst_cli
			// Run the analyst CLI, and send the selected options to the running servers.

			if (args[0].equalsIgnoreCase ("analyst_cli")) {

				if (args.length != 1) {
					System.out.println (caller_name.trim() + " : Invalid 'analyst_cli' subcommand");
					return true;
				}

				String[] my_args = new String[2];
				my_args[0] = "change_analyst_cli";
				my_args[1] = (new ServerConfig()).is_dual_server() ? "9" : "1";

				try {
					ServerCmd.cmd_change_analyst_cli (my_args);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println ();
					System.out.println (caller_name.trim() + " : The 'analyst_cli' subcommand failed with an exception");
				}
				return true;
			}

			// Subcommand : version
			// Command formats:
			//  version
			//  --version
			// Display the software version on the console, and exit.

			if (args[0].equalsIgnoreCase ("version") || args[0].equalsIgnoreCase ("--version") ) {

				if (args.length != 1) {
					System.out.println (caller_name.trim() + " : Invalid 'version' subcommand");
					return true;
				}

				String[] my_args = new String[1];
				my_args[0] = "show_version";

				try {
					ServerCmd.cmd_show_version (my_args);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println ();
					System.out.println (caller_name.trim() + " : The 'version' subcommand failed with an exception");
				}
				return true;
			}

			// Subcommand : fcdata_convert
			// Command format:
			//  fcdata_convert  in_filename  out_filename
			// Convert a forecast data file from the website/database JSON
			// format to a friendly human-understandable JSON format.

			if (args[0].equalsIgnoreCase ("fcdata_convert")) {

				if (args.length != 3) {
					System.out.println (caller_name.trim() + " : Invalid 'fcdata_convert' subcommand");
					return true;
				}

				String[] my_args = new String[3];
				my_args[0] = "fcdata_convert";
				my_args[1] = args[1];
				my_args[2] = args[2];

				try {
					ServerCmd.cmd_fcdata_convert (my_args);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println ();
					System.out.println (caller_name.trim() + " : The 'fcdata_convert' subcommand failed with an exception");
				}
				return true;
			}

		}


		// The GUI accepts command-line arguments for configuring PDL access.
		// Complete details are in PDLCmd.java.
		//
		// To select the PDL destination, use one of these:
		// --pdl=dryrun
		// --pdl=dev
		// --pdl=prod
		// If the --pdl option is not specified, the default is --pdl=dev.
		//
		// To specify a PDL key file, use:
		// --privateKey=PRIVATEKEYFILE
		// If the --privateKey option is not specified, then products are sent unsigned.
		//
		// If you want to delete a product, then in addition to the above you should also include all of these:
		// --delete
		// --code=PRODUCTCODE
		// --eventsource=EVENTNETWORK
		// --eventsourcecode=EVENTCODE
		// The value of --code identifies the product that is to be deleted.  The value of --code is typically an event ID.
		// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
		// these determine which event page displays the product.
		// When deleting a product, the delete is sent to PDL and the program exits without launching the GUI.
		//
		// If you have a JSON file, then you can send it as a product by including:
		// --update=JSONFILENAME
		// --code=PRODUCTCODE
		// --eventsource=EVENTNETWORK
		// --eventsourcecode=EVENTCODE
		// The value of --code identifies the product that is to be sent.  The value of --code is typically an event ID.
		// The product replaces any prior product that was sent with the same --code.
		// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
		// these determine which event page displays the product.
		// When sending a product, the product is sent to PDL and the program exits without launching the GUI.

		int lo = 0;
		boolean f_config = true;
		boolean f_send = true;
		int pdl_default = ServerConfigFile.PDLOPT_NONE;

		// Check for debug request

		if (args.length > lo) {
			if (args[lo].equalsIgnoreCase ("debug")) {
				f_gui_debug = true;
				++lo;
			}
		}

		// Now process PDL options

		boolean consumed = PDLCmd.exec_pdl_cmd (args, lo, f_config, f_send, pdl_default);

		if (consumed) {
			return true;
		}

		// GUI should proceed

		return false;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// Execute GUI command

		String caller_name = "GUICmd";

		exec_gui_cmd (args, caller_name);
		return;
	}
}
