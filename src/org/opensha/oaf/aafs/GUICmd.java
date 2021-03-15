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
// Convert a forecast data file from the website/database JSON
// format to a friendly human-understandable JSON format.
// Command format:
//  fcdata_convert  in_filename  out_filename
//
//
// ***** Configuration commands *****
//
// These commands control program configuration options.
// Currently, these control how to send products to PDL.
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
// If the --pdl option is not specified, the default is dryrun.
//
//
// To specify a PDL key file, use:
// --privateKey=PRIVATEKEYFILE
// If the --privateKey option is not specified, then products are sent unsigned.
//
//
// ***** PDL commands *****
//
// These commands can be used to send or delete a PDL product.
// The --pdl and --privateKey options may be used in combination with
// these commands to specify the PDL destination and signing options.
//
//
// To delete a product, then in addition to the above you should also include all of these:
// --delete
// --code=PRODUCTCODE
// --eventsource=EVENTNETWORK
// --eventsourcecode=EVENTCODE
// --source=PRODUCTSOURCE     [optional, defaults to configured value, which should be "us"]
// --type=PRODUCTTYPE         [optional, defaults to configured value, which should be "oaf"]
// --reviewed=ISREVIEWED      [optional, defaults to "true"]
// The value of --code identifies the product that is to be deleted.  The value of --code is typically an event ID.
// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
// these determine which event page displays the product.
// The optional parameters --source and --type identify the source (typically a network) and type of the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the deletion has been reviewed.
//
//
// If a JSON file exists on disk, then it can be sent as a product by including:
// --update=JSONFILENAME
// --code=PRODUCTCODE
// --eventsource=EVENTNETWORK
// --eventsourcecode=EVENTCODE
// --source=PRODUCTSOURCE     [optional, defaults to configured value, which should be "us"]
// --type=PRODUCTTYPE         [optional, defaults to configured value, which should be "oaf"]
// --reviewed=ISREVIEWED      [optional, defaults to "true"]
// The value of --code identifies the product that is to be sent.  The value of --code is typically an event ID.
// The product replaces any prior product that was sent with the same --code.
// The values of --eventsource and --eventsourcecode identify the event with which the product is associated;
// these determine which event page displays the product.
// The optional parameters --source and --type identify the source (typically a network) and type of the product.
// The optional parameter --reviewed, which must be "true" or "false", indicates if the product has been reviewed.

public class GUICmd {


	// Execute GUI command line.
	// Parameters:
	//  args = Command line parameters.
	//  caller_name = Name of the caller; used when reporting errors to the user;
	//                cannot be null or empty or blank.
	// Returns true if the command has been consumed.
	// Returns false if the command has not been consumed. This may indicate that
	//  configuration options are changed, e.g., by updating ServerConfig.

	public static boolean exec_gui_cmd (String[] args, String caller_name) {
	
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
