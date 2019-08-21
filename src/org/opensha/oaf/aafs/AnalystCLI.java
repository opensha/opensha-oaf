package org.opensha.oaf.aafs;

import java.util.List;
import java.util.Scanner;

import java.io.Closeable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import org.opensha.oaf.aafs.MongoDBUtil;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;
import org.opensha.oaf.aafs.entity.DBEntity;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalImpDataReader;
import org.opensha.oaf.util.MarshalImpDataWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TimeSplitOutputStream;
import org.opensha.oaf.util.ConsoleRedirector;


/**
 * Analyst intervention command-line interface.
 * Author: Michael Barall 08/20/2019.
 */
public class AnalystCLI {




	//----- Constants and variables -----




	// CLI states.

	public static final int CST_EXIT			=  1;		// Exit program
	public static final int CST_EVENT_ID		=  2;		// Ask for event id
	public static final int CST_EVENT_CONFIRM	=  3;		// Confirm event selection
	public static final int CST_B				=  4;		// b
	public static final int CST_NUM_A			=  5;		// num_a
	public static final int CST_MIN_A			=  6;		// min_a
	public static final int CST_MAX_A			=  7;		// max_a
	public static final int CST_NUM_P			=  8;		// num_p
	public static final int CST_MIN_P			=  9;		// min_p
	public static final int CST_MAX_P			= 10;		// max_p
	public static final int CST_NUM_C			= 11;		// num_c
	public static final int CST_MIN_C			= 12;		// min_c
	public static final int CST_MAX_C			= 13;		// max_c
	public static final int CST_MAG_CAT			= 14;		// magCat
	public static final int CST_CAP_F			= 15;		// capF
	public static final int CST_CAP_G			= 16;		// capG
	public static final int CST_CAP_H			= 17;		// capH
	//public static final int CST_RAD_FIXED		= 18;		// ask if radius is fixed
	public static final int CST_WC_MULT			= 19;		// wc_mult
	public static final int CST_RAD_MIN			= 20;		// min_radius
	public static final int CST_RAD_MAX			= 21;		// max_radius
	public static final int CST_RAD_SINGLE		= 22;		// single radius-value
	public static final int CST_SINGLE_A		= 23;		// single a-value
	public static final int CST_SINGLE_P		= 24;		// single p-value
	public static final int CST_SINGLE_C		= 25;		// single c-value
	public static final int CST_REVIEW			= 26;		// review values
	public static final int CST_SEND			= 27;		// send values
	public static final int CST_CONFIRM			= 28;		// confirm results
	public static final int CST_FORECAST		= 29;		// generate forecasts for this event
	public static final int CST_FC_SEL			= 30;		// fc_sel


	// Discrete responses.

	public static final int DVAL_NO				=  0;		// No
	public static final int DVAL_YES			=  1;		// Yes
	public static final int DVAL_AUTO			=  2;		// Automatic


	// Maximum number of grid points.

	public static int MAX_GRID_POINTS_SINGLE	=  1000;
	public static int MAX_GRID_POINTS_TOTAL		= 10000;


	// Operation result codes.

	public static final int RES_OK				=  1;		// Success
	public static final int RES_BACK			=  2;		// Go back
	public static final int RES_RESTART			=  3;		// Start over


	// Command strings.

	public static final String CS_YES_1			= "yes";
	public static final String CS_YES_2			= "y";
	public static final String CS_NO_1			= "no";
	public static final String CS_NO_2			= "n";
	public static final String CS_AUTO_1		= "auto";
	public static final String CS_AUTO_2		= "a";
	public static final String CS_BACK_1		= "back";
	public static final String CS_BACK_2		= "b";
	public static final String CS_RESTART_1		= "restart";
	public static final String CS_RESTART_2		= "r";



	// The current state.

	private int cst;

	// The current selection.

	private Selection cur_sel;

	// The default selection.

	//private Selection def_sel;

	// The input scanner.
	// Note: This should not be closed.

	private Scanner scanner;

	// Value from current conversion.

	private int value_int;
	private double value_real;
	private String value_string;

	// The event ID.

	private String event_id;

	// Mainshock parameters.

	ForecastMainshock fcmain;

	// Forecast parameters.

	ForecastParameters fcparams;




	// A class that holds a set of analyst selections.

	public static class Selection {

		// Forecast selection (DVAL_NO, DVAL_YES, DVAL_AUTO).

		public int fc_sel;

		// b-value.

		public double b;

		// a-value count and range.

		public double min_a;
		public double max_a;
		public int    num_a;

		// p-value count and range.

		public double min_p;
		public double max_p;
		public int    num_p;

		// c-value count and range.

		public double min_c;
		public double max_c;
		public int    num_c;

		// Catalog magnitude of completeness.

		public double magCat;

		// Magnitude of completeness parameters.

		public double capF;
		public double capG;
		public double capH;

		// Radius Wells and Coppersmith multiplier and range.

		public double wc_mult;
		public double min_radius;
		public double max_radius;

		// Set the values from forecast parameters.
		// Returns true if success, false if it cannot be done.

		public boolean set_from_fcparams (ForecastParameters fcparams) {

			// Forecast selection (DVAL_NO, DVAL_YES, DVAL_AUTO).

			fc_sel = DVAL_AUTO;

			// b-value

			b = fcparams.seq_spec_params.get_b();

			// a-value count and range.

			min_a = fcparams.seq_spec_params.get_min_a();
			max_a = fcparams.seq_spec_params.get_max_a();
			num_a = fcparams.seq_spec_params.get_num_a();

			// p-value count and range.

			min_p = fcparams.seq_spec_params.get_min_p();
			max_p = fcparams.seq_spec_params.get_max_p();
			num_p = fcparams.seq_spec_params.get_num_p();

			// c-value count and range.

			min_c = fcparams.seq_spec_params.get_min_c();
			max_c = fcparams.seq_spec_params.get_max_c();
			num_c = fcparams.seq_spec_params.get_num_c();

			// Catalog magnitude of completeness.

			magCat = fcparams.mag_comp_params.get_magCat();

			// Magnitude of completeness parameters.

			capF = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapF();
			capG = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapG();
			capH = fcparams.mag_comp_params.get_magCompFn().getDefaultGUICapH();

			// Radius Wells and Coppersmith multiplier and range.

			wc_mult = fcparams.mag_comp_params.get_fcn_radiusSample().getDefaultGUIRadiusMult();
			min_radius = fcparams.mag_comp_params.get_fcn_radiusSample().getDefaultGUIRadiusMin();
			max_radius = fcparams.mag_comp_params.get_fcn_radiusSample().getDefaultGUIRadiusMax();
		
			return true;
		}

		// Display the selections

		public void display_selections () {

			System.out.println();

			// Forecast selection

			switch (fc_sel) {
			case DVAL_YES:
				System.out.println ("generate forecasts = " + CS_YES_1);
				break;
			case DVAL_NO:
				System.out.println ("generate forecasts = " + CS_NO_1);
				System.out.println();
				return;
			case DVAL_AUTO:
				System.out.println ("generate forecasts = " + CS_AUTO_1);
				break;
			}

			// b-value

			System.out.println ("b-value = " + b);

			// a-value

			System.out.println ("a-value count = " + num_a);
			if (num_a == 1) {
				System.out.println ("a-value = " + min_a);
			} else {
				System.out.println ("a-value minimum = " + min_a);
				System.out.println ("a-value maximum = " + max_a);
			}

			// p-value

			System.out.println ("p-value count = " + num_p);
			if (num_p == 1) {
				System.out.println ("p-value = " + min_p);
			} else {
				System.out.println ("p-value minimum = " + min_p);
				System.out.println ("p-value maximum = " + max_p);
			}

			// c-value

			System.out.println ("c-value count = " + num_c);
			if (num_a == 1) {
				System.out.println ("c-value = " + min_c);
			} else {
				System.out.println ("c-value minimum = " + min_c);
				System.out.println ("c-value maximum = " + max_c);
			}

			// mag cat

			System.out.println ("mag cat = " + magCat);

			// magnitude of completeness parameters

			System.out.println ("F-value = " + capF);
			System.out.println ("G-value = " + capG);
			System.out.println ("H-value = " + capH);

			// radius multiplier and range

			System.out.println ("W&C radius multiplier = " + wc_mult);
			if (wc_mult <= 1.0e-20) {
				System.out.println ("radius = " + min_radius);
			} else {
				System.out.println ("radius minimum = " + min_radius);
				System.out.println ("radius maximum = " + max_radius);
			}

			System.out.println();

			return;
		}
	}




	//----- Functions -----




	// Read an integer from the user.

	private int read_int_from_user (String prompt, Integer min_value, Integer max_value, Integer default_value) {

		// Create my prompt by appending range and default value

		String my_prompt = prompt;
		if (min_value != null && max_value != null) {
			my_prompt = my_prompt + " (" + min_value.intValue() + "-" + max_value.intValue() + ")";
		}
		if (default_value != null) {
			my_prompt = my_prompt + " [" + default_value.intValue() + "]";
		}

		// Retry loop

		for (;;) {

			// Display the prompt

			System.out.println (my_prompt);

			// Get response

			String line = scanner.nextLine().trim();

			// If empty ...

			if (line.isEmpty()) {

				// If we have a default value, return it

				if (default_value != null) {
					value_int = default_value.intValue();
					break;
				}

				// Otherwise, prompt the user

				System.out.println ("Please enter a value");
				continue;
			}

			// Check for back and restart

			if (line.equalsIgnoreCase (CS_BACK_1) || line.equalsIgnoreCase (CS_BACK_2)) {
				return RES_BACK;
			}

			if (line.equalsIgnoreCase (CS_RESTART_1) || line.equalsIgnoreCase (CS_RESTART_2)) {
				return RES_RESTART;
			}

			// Attempt conversion to integer

			try {
				value_int = Integer.parseInt (line);
			} catch (Exception e) {
				System.out.println ("Please enter a valid integer");
				continue;
			}

			// Range check

			if (min_value != null) {
				if (value_int < min_value.intValue()) {
					System.out.println ("Value is less than the minimum allowed value of " + min_value.intValue());
					continue;
				}
			}

			if (max_value != null) {
				if (value_int > max_value.intValue()) {
					System.out.println ("Value is greater than the maximum allowed value of " + max_value.intValue());
					continue;
				}
			}

			// Success

			break;
		}

		// Success

		return RES_OK;
	}




	// Read an real number from the user.

	private int read_real_from_user (String prompt, Double default_value) {

		// Create my prompt by appending default value

		String my_prompt = prompt;
		if (default_value != null) {
			my_prompt = my_prompt + " [" + default_value.doubleValue() + "]";
		}

		// Retry loop

		for (;;) {

			// Display the prompt

			System.out.println (my_prompt);

			// Get response

			String line = scanner.nextLine().trim();

			// If empty ...

			if (line.isEmpty()) {

				// If we have a default value, return it

				if (default_value != null) {
					value_real = default_value.doubleValue();
					break;
				}

				// Otherwise, prompt the user

				System.out.println ("Please enter a value");
				continue;
			}

			// Check for back and restart

			if (line.equalsIgnoreCase (CS_BACK_1) || line.equalsIgnoreCase (CS_BACK_2)) {
				return RES_BACK;
			}

			if (line.equalsIgnoreCase (CS_RESTART_1) || line.equalsIgnoreCase (CS_RESTART_2)) {
				return RES_RESTART;
			}

			// Attempt conversion to double

			try {
				value_real = Double.parseDouble (line);
			} catch (Exception e) {
				System.out.println ("Please enter a valid real number");
				continue;
			}

			// Success

			break;
		}

		// Success

		return RES_OK;
	}




	// Read a string from the user.

	private int read_string_from_user (String prompt, String default_value) {

		// Create my prompt by appending default value

		String my_prompt = prompt;
		if (default_value != null) {
			my_prompt = my_prompt + " [" + default_value + "]";
		}

		// Retry loop

		for (;;) {

			// Display the prompt

			System.out.println (my_prompt);

			// Get response

			String line = scanner.nextLine().trim();

			// If empty ...

			if (line.isEmpty()) {

				// If we have a default value, return it

				if (default_value != null) {
					value_string = default_value;
					break;
				}

				// Otherwise, prompt the user

				System.out.println ("Please enter a value");
				continue;
			}

			// Check for back and restart

			if (line.equalsIgnoreCase (CS_BACK_1) || line.equalsIgnoreCase (CS_BACK_2)) {
				return RES_BACK;
			}

			if (line.equalsIgnoreCase (CS_RESTART_1) || line.equalsIgnoreCase (CS_RESTART_2)) {
				return RES_RESTART;
			}

			// Got a string

			value_string = line;

			// Success

			break;
		}

		// Success

		return RES_OK;
	}




	// Read yes/no/auto from the user.

	private int read_yna_from_user (String prompt, boolean f_yes, boolean f_no, boolean f_auto, Integer default_value) {

		// Create my prompt by appending valid values and default value

		String my_prompt = prompt + " (";
		String sep = "";
		if (f_yes) {
			my_prompt = my_prompt + sep + CS_YES_1;
			sep = "/";
		}
		if (f_no) {
			my_prompt = my_prompt + sep + CS_NO_1;
			sep = "/";
		}
		if (f_auto) {
			my_prompt = my_prompt + sep + CS_AUTO_1;
			sep = "/";
		}
		my_prompt = my_prompt + ")";
		if (default_value != null) {
			switch (default_value.intValue()) {
			case DVAL_YES:
				my_prompt = my_prompt + " [" + CS_YES_2 + "]";
				break;
			case DVAL_NO:
				my_prompt = my_prompt + " [" + CS_NO_2 + "]";
				break;
			case DVAL_AUTO:
				my_prompt = my_prompt + " [" + CS_AUTO_2 + "]";
				break;
			}
		}

		// Retry loop

		for (;;) {

			// Display the prompt

			System.out.println (my_prompt);

			// Get response

			String line = scanner.nextLine().trim();

			// If empty ...

			if (line.isEmpty()) {

				// If we have a default value, return it

				if (default_value != null) {
					value_int = default_value.intValue();
					break;
				}

				// Otherwise, prompt the user

				System.out.println ("Please enter a response");
				continue;
			}

			// Check for back and restart

			if (line.equalsIgnoreCase (CS_BACK_1) || line.equalsIgnoreCase (CS_BACK_2)) {
				return RES_BACK;
			}

			if (line.equalsIgnoreCase (CS_RESTART_1) || line.equalsIgnoreCase (CS_RESTART_2)) {
				return RES_RESTART;
			}

			// Check for allowed responses

			if (f_yes) {
				if (line.equalsIgnoreCase (CS_YES_1) || line.equalsIgnoreCase (CS_YES_2)) {
					value_int = DVAL_YES;
					break;
				}
			}

			if (f_no) {
				if (line.equalsIgnoreCase (CS_NO_1) || line.equalsIgnoreCase (CS_NO_2)) {
					value_int = DVAL_NO;
					break;
				}
			}

			if (f_auto) {
				if (line.equalsIgnoreCase (CS_AUTO_1) || line.equalsIgnoreCase (CS_AUTO_2)) {
					value_int = DVAL_AUTO;
					break;
				}
			}

			// Otherwise, try again

			System.out.println ("Please enter a valid response");
		}

		// Success

		return RES_OK;
	}




	// Fetch event information and set up the default selections.
	// Returns true if success, false if failed.

	public boolean fetch_event_info () {

		System.out.println ("Please wait while data is fetched from Comcat");

		// Fetch the mainshock info

		fcmain = new ForecastMainshock();
		fcmain.setup_mainshock_poll (event_id);

		if (!( fcmain.mainshock_avail )) {
			System.out.println ("Cannot fetch data from Comcat for event " + event_id);
			return false;
		}

		// Fetch the forecast parameters, but not the aftershock search

		fcparams = new ForecastParameters();

		ForecastParameters prior_params = null;
		fcparams.fetch_control_params (fcmain, prior_params);
		fcparams.fetch_generic_params (fcmain, prior_params);
		fcparams.fetch_mag_comp_params (fcmain, prior_params);
		fcparams.fetch_seq_spec_params (fcmain, prior_params);

		if (!( fcparams.generic_avail && fcparams.mag_comp_avail && fcparams.seq_spec_avail )) {
			System.out.println ("Cannot determine parameters for event " + event_id);
			return false;
		}

		// Extract default selections from the forecast parameters

		cur_sel = new Selection();

		if (!( cur_sel.set_from_fcparams (fcparams) )) {
			System.out.println ("Cannot determine default parameters for event " + event_id);
			return false;
		}

		// Display the event info

		System.out.println ();
		System.out.println ("mainshock_time = " + SimpleUtils.time_raw_and_string(fcmain.mainshock_time));
		System.out.println ("mainshock_mag = " + fcmain.mainshock_mag);
		System.out.println ("mainshock_lat = " + fcmain.mainshock_lat);
		System.out.println ("mainshock_lon = " + fcmain.mainshock_lon);
		System.out.println ("mainshock_depth = " + fcmain.mainshock_depth);
		System.out.println ();

		return true;
	}




	// Get the analyst selections.

	public boolean get_selections () {

		// Initialize

		cst = CST_EVENT_ID;
		cur_sel = null;
		//def_sel = null;
		scanner = new Scanner (System.in);

		value_int = 0;
		value_real = 0.0;
		value_string = "";

		event_id = "";
		fcmain = null;
		fcparams = null;

		// Temp for maximum number of grid points

		int max_grid = MAX_GRID_POINTS_SINGLE;

		// Loop until Exit

		while (cst != CST_EXIT) {
			switch (cst) {

			// Request event id

			case CST_EVENT_ID:
				switch (read_string_from_user ("Enter event ID", null)) {
				case RES_BACK:
					cst = CST_EVENT_ID;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					event_id = value_string;
					if (!( fetch_event_info() )) {
						cst = CST_EVENT_ID;
					} else {
						cst = CST_EVENT_CONFIRM;
					}
					break;
				}
				break;

			// Confirm event id selection

			case CST_EVENT_CONFIRM:
				switch (read_yna_from_user ("Proceed with this event?", true, true, false, DVAL_YES)) {
				case RES_BACK:
					cst = CST_EVENT_ID;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					if (value_int == DVAL_NO) {
						cst = CST_EVENT_ID;
					} else {
						cst = CST_FC_SEL;
					}
					break;
				}
				break;

			// Forecast generation

			case CST_FC_SEL:
				switch (read_yna_from_user ("Generate forecasts for this event?", true, true, true, cur_sel.fc_sel)) {
				case RES_BACK:
					cst = CST_EVENT_ID;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.fc_sel = value_int;
					if (cur_sel.fc_sel == DVAL_NO) {
						cst = CST_REVIEW;
					} else {
						cst = CST_B;
					}
					break;
				}
				break;

			// b-value

			case CST_B:
				switch (read_real_from_user ("b-value", cur_sel.b)) {
				case RES_BACK:
					cst = CST_FC_SEL;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.b = value_real;
					cst = CST_NUM_A;
					break;
				}
				break;

			// a-value

			case CST_NUM_A:
				max_grid = MAX_GRID_POINTS_SINGLE;
				switch (read_int_from_user ("a-value count", 1, max_grid, Math.min (max_grid, cur_sel.num_a))) {
				case RES_BACK:
					cst = CST_B;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.num_a = value_int;
					if (cur_sel.num_a == 1) {
						cst = CST_SINGLE_A;
					} else {
						cst = CST_MIN_A;
					}
					break;
				}
				break;

			case CST_SINGLE_A:
				switch (read_real_from_user ("a-value", cur_sel.min_a)) {
				case RES_BACK:
					cst = CST_NUM_A;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_a = value_real;
					cur_sel.max_a = value_real;
					cst = CST_NUM_P;
					break;
				}
				break;

			case CST_MIN_A:
				switch (read_real_from_user ("a-value minimum", cur_sel.min_a)) {
				case RES_BACK:
					cst = CST_NUM_A;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_a = value_real;
					cst = CST_MAX_A;
					break;
				}
				break;

			case CST_MAX_A:
				switch (read_real_from_user ("a-value maximum", cur_sel.max_a)) {
				case RES_BACK:
					cst = CST_MIN_A;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.max_a = value_real;
					cst = CST_NUM_P;
					break;
				}
				break;

			// p-value

			case CST_NUM_P:
				max_grid = Math.min (MAX_GRID_POINTS_SINGLE, MAX_GRID_POINTS_TOTAL / cur_sel.num_a);
				switch (read_int_from_user ("p-value count", 1, max_grid, Math.min (max_grid, cur_sel.num_p))) {
				case RES_BACK:
					cst = CST_NUM_A;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.num_p = value_int;
					if (cur_sel.num_p == 1) {
						cst = CST_SINGLE_P;
					} else {
						cst = CST_MIN_P;
					}
					break;
				}
				break;

			case CST_SINGLE_P:
				switch (read_real_from_user ("p-value", cur_sel.min_p)) {
				case RES_BACK:
					cst = CST_NUM_P;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_p = value_real;
					cur_sel.max_p = value_real;
					cst = CST_NUM_C;
					break;
				}
				break;

			case CST_MIN_P:
				switch (read_real_from_user ("p-value minimum", cur_sel.min_p)) {
				case RES_BACK:
					cst = CST_NUM_P;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_p = value_real;
					cst = CST_MAX_P;
					break;
				}
				break;

			case CST_MAX_P:
				switch (read_real_from_user ("p-value maximum", cur_sel.max_p)) {
				case RES_BACK:
					cst = CST_MIN_P;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.max_p = value_real;
					cst = CST_NUM_C;
					break;
				}
				break;

			// c-value

			case CST_NUM_C:
				max_grid = Math.min (MAX_GRID_POINTS_SINGLE, MAX_GRID_POINTS_TOTAL / (cur_sel.num_a * cur_sel.num_p));
				switch (read_int_from_user ("c-value count", 1, max_grid, Math.min (max_grid, cur_sel.num_c))) {
				case RES_BACK:
					cst = CST_NUM_P;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.num_c = value_int;
					if (cur_sel.num_c == 1) {
						cst = CST_SINGLE_C;
					} else {
						cst = CST_MIN_C;
					}
					break;
				}
				break;

			case CST_SINGLE_C:
				switch (read_real_from_user ("c-value", cur_sel.min_c)) {
				case RES_BACK:
					cst = CST_NUM_C;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_c = value_real;
					cur_sel.max_c = value_real;
					cst = CST_MAG_CAT;
					break;
				}
				break;

			case CST_MIN_C:
				switch (read_real_from_user ("c-value minimum", cur_sel.min_c)) {
				case RES_BACK:
					cst = CST_NUM_C;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_c = value_real;
					cst = CST_MAX_C;
					break;
				}
				break;

			case CST_MAX_C:
				switch (read_real_from_user ("c-value maximum", cur_sel.max_c)) {
				case RES_BACK:
					cst = CST_MIN_C;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.max_c = value_real;
					cst = CST_MAG_CAT;
					break;
				}
				break;

			// Catalog magnitude

			case CST_MAG_CAT:
				switch (read_real_from_user ("mag cat", cur_sel.magCat)) {
				case RES_BACK:
					cst = CST_NUM_C;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.magCat = value_real;
					cst = CST_CAP_F;
					break;
				}
				break;

			// F-value

			case CST_CAP_F:
				switch (read_real_from_user ("F-value", cur_sel.capF)) {
				case RES_BACK:
					cst = CST_MAG_CAT;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.capF = value_real;
					cst = CST_CAP_G;
					break;
				}
				break;

			// G-value

			case CST_CAP_G:
				switch (read_real_from_user ("G-value", cur_sel.capG)) {
				case RES_BACK:
					cst = CST_CAP_F;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.capG = value_real;
					cst = CST_CAP_H;
					break;
				}
				break;

			// H-value

			case CST_CAP_H:
				switch (read_real_from_user ("H-value", cur_sel.capH)) {
				case RES_BACK:
					cst = CST_CAP_G;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.capH = value_real;
					cst = CST_WC_MULT;
					break;
				}
				break;

			// radius

			case CST_WC_MULT:
				switch (read_real_from_user ("W&C radius multiplier, or 0 for fixed radius", cur_sel.wc_mult)) {
				case RES_BACK:
					cst = CST_CAP_H;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.wc_mult = value_real;
					if (cur_sel.wc_mult <= 1.0e-20) {
						cst = CST_RAD_SINGLE;
					} else {
						cst = CST_RAD_MIN;
					}
					break;
				}
				break;

			case CST_RAD_SINGLE:
				switch (read_real_from_user ("radius", cur_sel.min_radius)) {
				case RES_BACK:
					cst = CST_WC_MULT;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_radius = value_real;
					cur_sel.max_radius = value_real;
					cst = CST_REVIEW;
					break;
				}
				break;

			case CST_RAD_MIN:
				switch (read_real_from_user ("radius minimum, or 0 for none", cur_sel.min_radius)) {
				case RES_BACK:
					cst = CST_WC_MULT;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.min_radius = value_real;
					cst = CST_RAD_MAX;
					break;
				}
				break;

			case CST_RAD_MAX:
				switch (read_real_from_user ("radius maximum, or 0 for none", cur_sel.max_radius)) {
				case RES_BACK:
					cst = CST_RAD_MIN;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					cur_sel.max_radius = value_real;
					cst = CST_REVIEW;
					break;
				}
				break;

			// Review seelctions

			case CST_REVIEW:
				System.out.println ("Review selections");
				cur_sel.display_selections();
				switch (read_yna_from_user ("Continue with these selections?", true, true, false, DVAL_YES)) {
				case RES_BACK:
					cst = CST_FC_SEL;
					break;
				case RES_RESTART:
					cst = CST_EVENT_ID;
					break;
				default:
					if (value_int == DVAL_NO) {
						cst = CST_FC_SEL;
					} else {
						cst = CST_EXIT;
					}
					break;
				}
				break;

			//TODO - Send selections to servers

			}	// end switch on state

		}	// end loop until exit state

		return true;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// Allocate the object

		AnalystCLI analyst_cli = new AnalystCLI();

		// Run it

		analyst_cli.get_selections();

		return;
	}
}
