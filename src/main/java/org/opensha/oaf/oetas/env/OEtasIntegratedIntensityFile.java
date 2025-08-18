package org.opensha.oaf.oetas.env;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import java.util.function.Consumer;
import java.util.function.Supplier;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.LineSupplierFile;
import org.opensha.oaf.util.LineConsumerFile;
import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;

import org.opensha.oaf.oetas.OEConstants;
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG

import org.opensha.oaf.oetas.fit.OEDisc2IntensityCalc;
import static org.opensha.oaf.oetas.fit.OEDisc2IntensityCalc.ILKIND_RUPTURE;	// = 1
import static org.opensha.oaf.oetas.fit.OEDisc2IntensityCalc.ILKIND_FILL;		// = 2
import static org.opensha.oaf.oetas.fit.OEDisc2IntensityCalc.ILKIND_SUMMARY;	// = 3

//import org.opensha.oaf.oetas.util.OEArraysCalc;



// Class to load the integrated-intensity file for operational ETAS.
// Author: Michael Barall.
//
// This file type holds integrated intensiry values from any number of models.
// Usually there are 4 models:  active, generic, seq-spec, and bayesian.
//
// The first line in the file is a summary line:
//
//   ilkind  t_first  t_last  num_lines  num_eqks  [split_mean  split_variance]...
//
// Each succeeding line is either an earthquake or a filler line:
//
//   ilkind  t_day  rup_mag  cum_num  [cum_integral  split_integral  incr_integral]...
//
// The value of "ilkind" is ILKIND_SUMMARY in the summary line, ILKIND_RUPTURE in
// an earthquake line, or ILKIND_FILL in a filler line.
//
// The value of "num_lines" is the total number of data lines in the file.
//
// The value of "num_eqks" is the number earthquake lines in the file.
//
// The value of "rup_mag" is the earthquake magnitude or NO_MAG_NEG = -11.875 in a filler line.
//
// The value of "cum_num" is the number of earthquakes so far.  In an earthquake line,
// it includes that earthquake (hence the first earthquake line has cum_num = 1).
//
// Typically the first and last data lines are filler lines to mmark the beginning and end
// of the time interval.  The values of "t_first" and "t_last" should equal the value of
// "t_day" for the first and last data lines in the file.

public class OEtasIntegratedIntensityFile {




	//----- Summary line -----




	// Class to hold the summary line.

	public static class IISummaryLine {

		//--- Contents

		// The kind, must be ILKIND_SUMMARY.

		public int ilkind;

		// The start of the time interval, in days since the mainshock.

		public double t_first;

		// The end of the time interval, in days since the mainshock.

		public double t_last;

		// The number of data lines in the file.

		public int num_lines;

		// The number of earthquakes in the file, equals the number of earthquake data lines.

		public int num_eqks;

		// The mean of the split integrals, one for each model.

		public double[] split_mean;

		// The variance of the split integrals, one for each model.

		public double[] split_variance;

		//--- Access

		// Get the number of models.

		public final int get_num_models () {
			return split_mean.length;
		}

		//--- Setup

		// Set data common to all models.
		// Note: Can use t_first = 0.0, t_last = 0.0, num_lines = 0, and num_eqks = 0 if these are to be filled in later.

		public final IISummaryLine set_common (
			int num_models,
			double t_first,
			double t_last,
			int num_lines,
			int num_eqks
		) {
			this.ilkind = ILKIND_SUMMARY;
			this.t_first = t_first;
			this.t_last = t_last;
			this.num_lines = num_lines;
			this.num_eqks = num_eqks;
			this.split_mean = new double[num_models];
			this.split_variance = new double[num_models];
			return this;
		}

		// Set per-model data.

		public final IISummaryLine set_per_model (
			int i_model,
			double split_mean,
			double split_variance
		) {
			this.split_mean[i_model] = split_mean;
			this.split_variance[i_model] = split_variance;
			return this;
		}

		//--- Parsing and formatting

		// Parse the line.
		// This determines the number of models from the number of words on the line.

		public final IISummaryLine parse_line (String line) {
			String[] words = line.trim().split("\\s+");
			if (!( words.length >= 5 && (words.length - 5) % 2 == 0 )) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IISummaryLine.parse_line: Invalid line: " + line);
			}

			int num_models = (words.length - 5) / 2;
			split_mean = new double[num_models];
			split_variance = new double[num_models];

			try {
				int iw = 0;
				ilkind		= Integer.parseInt (words[iw++]);
				t_first		= Double.parseDouble (words[iw++]);
				t_last		= Double.parseDouble (words[iw++]);
				num_lines	= Integer.parseInt (words[iw++]);
				num_eqks	= Integer.parseInt (words[iw++]);
				for (int j = 0; j < num_models; ++j) {
					split_mean[j]		= Double.parseDouble (words[iw++]);
					split_variance[j]	= Double.parseDouble (words[iw++]);
				}
			}
			catch (NumberFormatException e) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IISummaryLine.parse_line: Invalid line: " + line, e);
			}

			if (!(
				ilkind == ILKIND_SUMMARY
				&& num_lines >= 0
				&& num_eqks >= 0
				&& num_eqks <= num_lines
			)) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IISummaryLine.parse_line: Invalid line: " + line);
			}
			return this;
		}

		// Format the line and append to the StringBuilder.
		// No terminator is appended to the line.
		// Returns the StringBuilder.

		public final StringBuilder format_line (StringBuilder sb) {
			int num_models = get_num_models();

			sb.append (ILKIND_SUMMARY);
			sb.append (" ");
			sb.append (rndd(t_first));
			sb.append (" ");
			sb.append (rndd(t_last));
			sb.append (" ");
			sb.append (num_lines);
			sb.append (" ");
			sb.append (num_eqks);
			for (int j = 0; j < num_models; ++j) {
				sb.append (" ");
				sb.append (rndd(split_mean[j]));
				sb.append (" ");
				sb.append (rndd(split_variance[j]));
			}

			return sb;
		}

		// Format the line as a string, optionally appending a terminator.

		public final String format_line_as_string (String terminator) {
			StringBuilder sb = format_line (new StringBuilder());
			if (terminator != null) {
				sb.append (terminator);
			}
			return sb.toString();
		}

	}




	//----- Data line -----




	// Class to hold one data line, can be either an earthquake line or a fill line.

	public static class IIDataLine {

		//--- Contents

		// The kind, must be ILKIND_RUPTURE or ILKIND_FILL.

		public int ilkind;

		// The time, in days since the mainshock.

		public double t_day;

		// The magnitude if this is an earthquake line; NO_MAG_NEG if this is a filler lone.

		public double rup_mag;

		// The cumulative number of earthquakes so far; if this is an earthquake line it includes this earthquake.

		public int cum_num;

		// The cumulative integral of lambda, one for each model.

		public double[] cum_integral;

		// The split integral of lambda (since the previous earthquake), one for each model.

		public double[] split_integral;

		// The incremental integral of lambda (since the previous line), one for each model.

		public double[] incr_integral;

		//--- Access

		// Get the number of models.

		public final int get_num_models () {
			return cum_integral.length;
		}

		// Return true if this data line is a rupture.

		public final boolean is_rupture () {
			return ilkind == ILKIND_RUPTURE;
		}

		//--- Setup

		// Set data common to all models, for a rupture line.

		public final IIDataLine set_common_rupture (
			int num_models,
			double t_day,
			double rup_mag,
			int cum_num
		) {
			this.ilkind = ILKIND_RUPTURE;
			this.t_day = t_day;
			this.rup_mag = rup_mag;
			this.cum_num = cum_num;
			this.cum_integral = new double[num_models];
			this.split_integral = new double[num_models];
			this.incr_integral = new double[num_models];
			return this;
		}

		// Set data common to all models, for a filler line.

		public final IIDataLine set_common_fill (
			int num_models,
			double t_day,
			int cum_num
		) {
			this.ilkind = ILKIND_FILL;
			this.t_day = t_day;
			this.rup_mag = NO_MAG_NEG;
			this.cum_num = cum_num;
			this.cum_integral = new double[num_models];
			this.split_integral = new double[num_models];
			this.incr_integral = new double[num_models];
			return this;
		}

		// Set per-model data.

		public final IIDataLine set_per_model (
			int i_model,
			double cum_integral,
			double split_integral,
			double incr_integral
		) {
			this.cum_integral[i_model] = cum_integral;
			this.split_integral[i_model] = split_integral;
			this.incr_integral[i_model] = incr_integral;
			return this;
		}

		//--- Parsing and formatting

		// Parse the line.

		public final IIDataLine parse_line (int num_models, String line) {
			String[] words = line.trim().split("\\s+");
			if (!( words.length == 4 + (3*num_models) )) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IIDataLine.parse_line: Invalid line: " + line);
			}

			cum_integral = new double[num_models];
			split_integral = new double[num_models];
			incr_integral = new double[num_models];

			try {
				int iw = 0;
				ilkind		= Integer.parseInt (words[iw++]);
				t_day		= Double.parseDouble (words[iw++]);
				rup_mag		= Double.parseDouble (words[iw++]);
				cum_num		= Integer.parseInt (words[iw++]);
				for (int j = 0; j < num_models; ++j) {
					cum_integral[j]		= Double.parseDouble (words[iw++]);
					split_integral[j]	= Double.parseDouble (words[iw++]);
					incr_integral[j]	= Double.parseDouble (words[iw++]);
				}
			}
			catch (NumberFormatException e) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IIDataLine.parse_line: Invalid line: " + line, e);
			}

			if (!(
				(ilkind == ILKIND_RUPTURE || ilkind == ILKIND_FILL)
				&& cum_num >= 0
			)) {
				throw new RuntimeException ("OEtasIntegratedIntensityFile.IIDataLine.parse_line: Invalid line: " + line);
			}
			return this;
		}

		// Format the line and append to the StringBuilder.
		// No terminator is appended to the line.
		// Returns the StringBuilder.

		public final StringBuilder format_line (StringBuilder sb) {
			int num_models = get_num_models();

			sb.append (ilkind);
			sb.append (" ");
			sb.append (rndd(t_day));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string_trailz ("%.4f", SimpleUtils.TRAILZ_REMOVE, rup_mag));
			sb.append (" ");
			sb.append (cum_num);
			for (int j = 0; j < num_models; ++j) {
				sb.append (" ");
				sb.append (rndd(cum_integral[j]));
				sb.append (" ");
				sb.append (rndd(split_integral[j]));
				sb.append (" ");
				sb.append (rndd(incr_integral[j]));
			}

			return sb;
		}

		// Format the line as a string, optionally appending a terminator.

		public final String format_line_as_string (String terminator) {
			StringBuilder sb = format_line (new StringBuilder());
			if (terminator != null) {
				sb.append (terminator);
			}
			return sb.toString();
		}

	}




	//----- Data storage -----


	// The number of models.

	public int num_models = 0;


	// The summary line.

	public IISummaryLine summary_line = null;


	// The data lines.

	public IIDataLine[] data_lines = null;


	// The data lines that contain ruptures (a subset of data_lines).

	public IIDataLine[] rupture_data_lines = null;


	// The data lines that contain ruptures, plus the first and last (a subset of data_lines).

	public IIDataLine[] bordered_data_lines = null;


	// Temporary list for building the data lines.

	private List<IIDataLine> data_line_list = null;




	//----- Construction -----




	// Clear the contents.

	public final void clear() {
		num_models = 0;
		summary_line = null;
		data_lines = null;
		rupture_data_lines = null;
		bordered_data_lines = null;
		data_line_list = null;
		return;
	}




	// Constructor makes an empty object.

	public OEtasIntegratedIntensityFile () {
		clear();
	}




	//----- Building -----




	// Begin building the contents.

	public final void begin_build (int num_models) {
		clear();
		this.num_models = num_models;
		data_line_list = new ArrayList<IIDataLine>();
		return;
	}




	// Set the common data in the summary line.
	// Note: Can use t_first = 0.0, t_last = 0.0, num_lines = 0, and num_eqks = 0 if these are to be filled in later.

	public final void set_summary_common (
		double t_first,
		double t_last,
		int num_lines,
		int num_eqks
	) {
		summary_line = (new IISummaryLine()).set_common (
			num_models,
			t_first,
			t_last,
			num_lines,
			num_eqks
		);
		return;
	}




	// Set per-model data in the summary line.

	public final void set_summary_per_model (
		int i_model,
		double split_mean,
		double split_variance
	) {
		summary_line.set_per_model (
			i_model,
			split_mean,
			split_variance
		);
		return;
	}




	// Make a new data lne and set data common to all models, for a rupture line.
	// Note: Can use cum_num = 0 if it is going to be filled in later.

	public final void set_data_common_rupture (
		double t_day,
		double rup_mag,
		int cum_num
	) {
		data_line_list.add ((new IIDataLine()).set_common_rupture (
			num_models,
			t_day,
			rup_mag,
			cum_num
		));
		return;
	}




	// Make a new data line and set data common to all models, for a filler line.
	// Note: Can use cum_num = 0 if it is going to be filled in later.

	public final void set_data_common_fill (
		double t_day,
		int cum_num
	) {
		data_line_list.add ((new IIDataLine()).set_common_fill (
			num_models,
			t_day,
			cum_num
		));
		return;
	}




	// Set per-model for the most recently created data line.
	// Note: Can use cum_integral = 0.0 and split_integral = 0.0 if they will be filled in later.

	public final void set_data_per_model (
		int i_model,
		double cum_integral,
		double split_integral,
		double incr_integral
	) {
		IIDataLine data_line = data_line_list.get (data_line_list.size() - 1);
		data_line.set_per_model (
			i_model,
			cum_integral,
			split_integral,
			incr_integral
		);
		return;
	}




	// Fill omitted data fields.
	// Fills cum_num, cum_integral, and split_integral in each data line.
	// Data lines must exist in the data_lines array.

	public final void fill_omitted_data_fields () {

		// Accumulators

		double[] split_accum = new double[num_models];	// accumulator for aplit integrals
		double[] cum_accum = new double[num_models];	// accumulator for cumulative integrals
		for (int i_model = 0; i_model < num_models; ++i_model) {
			split_accum[i_model] = 0.0;
			cum_accum[i_model] = 0.0;
		}

		int num_accum = 0;		// accumulator for number of splits = number of earthquakes

		// For each data line ...

		for (IIDataLine data_line : data_lines) {

			// Count split if it is a rupture

			if (data_line.is_rupture()) {
				++num_accum;
			}

			data_line.cum_num = num_accum;

			// For each model ...

			for (int i_model = 0; i_model < num_models; ++i_model) {

				// Accumulate within split

				split_accum[i_model] += data_line.incr_integral[i_model];
				data_line.split_integral[i_model] = split_accum[i_model];

				// Accumulation for ruptures

				if (data_line.is_rupture()) {

					cum_accum[i_model] += split_accum[i_model];
					data_line.cum_integral[i_model] = cum_accum[i_model];

					split_accum[i_model] = 0.0;
				}

				// Accumulation for filling

				else {
					data_line.cum_integral[i_model] = cum_accum[i_model] + split_accum[i_model];
				}
			}
		}

		return;
	}




	// Fill omitted summary fields.
	// Fills t_first, t_last, num_lines, num_eqks, split_mean, and split_variance in summary lines.
	// Data lines must exist in the data_lines array.
	// Note: Data fields must already be filled in.

	public final void fill_omitted_summary_fields () {

		// Accumulators

		double[] mean_accum = new double[num_models];	// accumulator for split integral mean
		double[] var_accum = new double[num_models];	// accumulator for split integral variance
		for (int i_model = 0; i_model < num_models; ++i_model) {
			mean_accum[i_model] = 0.0;
			var_accum[i_model] = 0.0;
		}

		int num_accum = 0;		// accumulator for number of splits = number of earthquakes

		// For each data line ...

		for (IIDataLine data_line : data_lines) {

			// Count split if it is a rupture

			if (data_line.is_rupture()) {
				++num_accum;
			}

			// For each model ...

			for (int i_model = 0; i_model < num_models; ++i_model) {

				// Accumulation for ruptures

				if (data_line.is_rupture()) {
					mean_accum[i_model] += data_line.split_integral[i_model];
				}
			}
		}

		// Put times in summary line

		if (data_lines.length == 0) {
			summary_line.t_first = 0.0;
			summary_line.t_last = 0.0;
		} else {
			summary_line.t_first = data_lines[0].t_day;
			summary_line.t_last = data_lines[data_lines.length - 1].t_day;
		}

		// Put counts in summary line

		summary_line.num_lines = data_lines.length;
		summary_line.num_eqks = num_accum;

		// If no splits, set means and variances to zero

		if (num_accum == 0) {
			for (int i_model = 0; i_model < num_models; ++i_model) {
				summary_line.set_per_model (
					i_model,
					0.0,
					0.0
				);
			}
		}

		// If splits, compute mean and variance

		else {

			// Divide by number of splits to get the mean

			final double r_num_accum = (double)num_accum;
			for (int i_model = 0; i_model < num_models; ++i_model) {
				mean_accum[i_model] = mean_accum[i_model] / r_num_accum;
			}

			// Compute variances

			for (IIDataLine data_line : data_lines) {

				// For each model ...

				for (int i_model = 0; i_model < num_models; ++i_model) {

					// Accumulation for ruptures

					if (data_line.is_rupture()) {
						final double delta = data_line.split_integral[i_model] - mean_accum[i_model];
						var_accum[i_model] += (delta * delta);
					}
				}
			}

			// Put means and variances in summary line

			for (int i_model = 0; i_model < num_models; ++i_model) {
				summary_line.set_per_model (
					i_model,
					mean_accum[i_model],
					var_accum[i_model] / r_num_accum
				);
			}
		}

		return;
	}




	// Select ranges from the list of data lines.

	public final void make_range_lists () {
		List<IIDataLine> rupture_data_line_list = new ArrayList<IIDataLine>();
		List<IIDataLine> bordered_data_line_list = new ArrayList<IIDataLine>();
		for (int j = 0; j < data_lines.length; ++j) {
			if (data_lines[j].is_rupture()) {
				rupture_data_line_list.add (data_lines[j]);
			}
			if (data_lines[j].is_rupture() || j == 0 || j == data_lines.length - 1) {
				bordered_data_line_list.add (data_lines[j]);
			}
		}
		rupture_data_lines = rupture_data_line_list.toArray (new IIDataLine[0]);
		bordered_data_lines = bordered_data_line_list.toArray (new IIDataLine[0]);
		return;
	}




	// End building the contents.
	// Parameters:
	//  f_fill_summary = True to fill num_lines, num_eqks, split_mean, and split_variance in summary lines.
	//  f_fill_data = True to fill cum_num, cum_integral, and split_integral in each data line.

	public final void end_build (boolean f_fill_summary, boolean f_fill_data) {
		data_lines = data_line_list.toArray (new IIDataLine[0]);
		data_line_list = null;
		make_range_lists();

		// Fill data fields if requested

		if (f_fill_data) {
			fill_omitted_data_fields();
		}

		// Fill summary fields if requested

		if (f_fill_summary) {
			fill_omitted_summary_fields();
		}

		return;
	}




	//----- I/O -----




	// Read from a supplier of lines.
	// Throws exception if error.

	public void read_from (Supplier<String> src) {
		clear();
		data_line_list = new ArrayList<IIDataLine>();

		// Read the summary line

		for (String line = src.get(); line != null; line = src.get()) {
			String trimline = line.trim();
			if (!( trimline.isEmpty() || trimline.startsWith("#") )) {
				summary_line = (new IISummaryLine()).parse_line (line);
				break;
			}
		}

		if (!( summary_line != null )) {
			throw new RuntimeException ("OEtasIntegratedIntensityFile.read_from: No summary line");
		}
		num_models = summary_line.get_num_models();

		// Read the data lines

		for (String line = src.get(); line != null; line = src.get()) {
			String trimline = line.trim();
			if (!( trimline.isEmpty() || trimline.startsWith("#") )) {
				data_line_list.add ((new IIDataLine()).parse_line (num_models, line));
			}
		}

		data_lines = data_line_list.toArray (new IIDataLine[0]);
		data_line_list = null;
		make_range_lists();

		return;
	}




	// Read contents from a file.
	// Throws exception if error.

	public void read_file (String filename) {
		try (
			LineSupplierFile src = new LineSupplierFile (filename);
		) {
			read_from (src);
		}
		return;
	}




	// Write to a consumer of lines.
	// Throws exception if error.

	public void write_to (Consumer<String> dest) {

		// Write the summary line

		dest.accept (summary_line.format_line_as_string (null));

		// Write the data lines.

		for (IIDataLine data_line : data_lines) {
			dest.accept (data_line.format_line_as_string (null));
		}

		return;
	}




	// Write contents to a file.

	public void write_file (String filename) {
		try (
			LineConsumerFile dest = new LineConsumerFile (filename);
		) {
			write_to (dest);
		}
		return;
	}




	//----- Access -----




	// Index numbers of common models.
	// (Negative values would be special models, none currently defined).

	public static final int IIMODEL_ACTIVE = 0;			// active model
	public static final int IIMODEL_GENERIC = 1;		// generic model
	public static final int IIMODEL_SEQSPEC = 2;		// sequence specific model
	public static final int IIMODEL_BAYESIAN = 3;		// Bayesian model




	// Index numbers of defined ranges.

	public static final int IIRANGE_ALL = 0;			// all data lines
	public static final int IIRANGE_RUPTURE = 1;		// data lines containing a rupture
	public static final int IIRANGE_BORDERED = 2;		// data lines containing a rupture, plus the first and last data lines

	// Get the selected range.

	public IIDataLine[] get_selected_range (int iirange) {
		switch (iirange) {
		case IIRANGE_ALL:
			return data_lines;
		case IIRANGE_RUPTURE:
			return rupture_data_lines;
		case IIRANGE_BORDERED:
			return bordered_data_lines;
		}
		throw new IllegalArgumentException ("OEtasIntegratedIntensityFile.get_selected_range: Invalid range selection: iirange = " + iirange);
	}




	// Index numbers of variables within the data line.

	public static final int IIVAR_T_DAY = 0;
	public static final int IIVAR_RUP_MAG = 1;
	public static final int IIVAR_CUM_NUM = 2;
	public static final int IIVAR_CUM_INTEGRAL = 3;
	public static final int IIVAR_SPLIT_INTEGRAL = 4;
	public static final int IIVAR_INCR_INTEGRAL = 5;




	// Return an array containing the given variable, for the given model, over the given range.
	// Note: In case of IIVAR_CUM_NUM, the number is converted to double.

	public double[] get_var_values (int iirange, int iivar, int i_model) {

		// Get the range

		IIDataLine[] range = get_selected_range (iirange);

		// Make array to hold the values

		double[] values = new double[range.length];

		// Switch on desired variable

		switch (iivar) {

		default:
			throw new IllegalArgumentException ("OEtasIntegratedIntensityFile.get_var_values: Invalid variable selection: iivar = " + iivar);

		case IIVAR_T_DAY:
			for (int j = 0; j < range.length; ++j) {
				values[j] = range[j].t_day;
			}
			break;

		case IIVAR_RUP_MAG:
			for (int j = 0; j < range.length; ++j) {
				values[j] = range[j].rup_mag;
			}
			break;

		case IIVAR_CUM_NUM:
			for (int j = 0; j < range.length; ++j) {
				values[j] = (double)(range[j].cum_num);
			}
			break;

		case IIVAR_CUM_INTEGRAL:
			for (int j = 0; j < range.length; ++j) {
				values[j] = range[j].cum_integral[i_model];
			}
			break;

		case IIVAR_SPLIT_INTEGRAL:
			for (int j = 0; j < range.length; ++j) {
				values[j] = range[j].split_integral[i_model];
			}
			break;

		case IIVAR_INCR_INTEGRAL:
			for (int j = 0; j < range.length; ++j) {
				values[j] = range[j].incr_integral[i_model];
			}
			break;
		}

		return values;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasIntegratedIntensityFile");




		// Subcommand : Test #1
		// Command format:
		//  test1  load_filename
		// Load integrated intensity from a file.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Load file");
			String load_filename = testargs.get_string ("load_filename");
			testargs.end_test();

			// Load the file.

			System.out.println ();
			System.out.println ("***** Loading file *****");
			System.out.println ();

			OEtasIntegratedIntensityFile ii_file = new OEtasIntegratedIntensityFile();
			ii_file.read_file (load_filename);
			System.out.println ("num_models = " + ii_file.num_models);
			System.out.println ("summary_line.t_first = " + ii_file.summary_line.t_first);
			System.out.println ("summary_line.t_last = " + ii_file.summary_line.t_last);
			System.out.println ("summary_line.num_lines = " + ii_file.summary_line.num_lines);
			System.out.println ("summary_line.num_eqks = " + ii_file.summary_line.num_eqks);
			System.out.println ("data_lines.length = " + ii_file.data_lines.length);
			System.out.println ("rupture_data_lines.length = " + ii_file.rupture_data_lines.length);
			System.out.println ("bordered_data_lines.length = " + ii_file.bordered_data_lines.length);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  load_filename  save_filename
		// Load integrated intensity from a file, then save it to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Load file, then save file");
			String load_filename = testargs.get_string ("load_filename");
			String save_filename = testargs.get_string ("save_filename");
			testargs.end_test();

			// Load the file.

			System.out.println ();
			System.out.println ("***** Loading file *****");
			System.out.println ();

			OEtasIntegratedIntensityFile ii_file = new OEtasIntegratedIntensityFile();
			ii_file.read_file (load_filename);
			System.out.println ("num_models = " + ii_file.num_models);
			System.out.println ("summary_line.t_first = " + ii_file.summary_line.t_first);
			System.out.println ("summary_line.t_last = " + ii_file.summary_line.t_last);
			System.out.println ("summary_line.num_lines = " + ii_file.summary_line.num_lines);
			System.out.println ("summary_line.num_eqks = " + ii_file.summary_line.num_eqks);
			System.out.println ("data_lines.length = " + ii_file.data_lines.length);
			System.out.println ("rupture_data_lines.length = " + ii_file.rupture_data_lines.length);
			System.out.println ("bordered_data_lines.length = " + ii_file.bordered_data_lines.length);

			System.out.println ();
			System.out.println ("***** Saving file *****");
			System.out.println ();

			ii_file.write_file (save_filename);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  load_filename  iirange  iivar  i_model
		// Load integrated intensity from a file, and display the variable for the given range, variable, and model.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Load file, then compute marginal distributions");
			String load_filename = testargs.get_string ("load_filename");
			int iirange = testargs.get_int ("iirange");
			int iivar = testargs.get_int ("iivar");
			int i_model = testargs.get_int ("i_model");
			testargs.end_test();

			// Load the file.

			System.out.println ();
			System.out.println ("***** Loading file *****");
			System.out.println ();

			OEtasIntegratedIntensityFile ii_file = new OEtasIntegratedIntensityFile();
			ii_file.read_file (load_filename);
			System.out.println ("num_models = " + ii_file.num_models);
			System.out.println ("summary_line.t_first = " + ii_file.summary_line.t_first);
			System.out.println ("summary_line.t_last = " + ii_file.summary_line.t_last);
			System.out.println ("summary_line.num_lines = " + ii_file.summary_line.num_lines);
			System.out.println ("summary_line.num_eqks = " + ii_file.summary_line.num_eqks);
			System.out.println ("data_lines.length = " + ii_file.data_lines.length);
			System.out.println ("rupture_data_lines.length = " + ii_file.rupture_data_lines.length);
			System.out.println ("bordered_data_lines.length = " + ii_file.bordered_data_lines.length);

			System.out.println ();
			System.out.println ("***** Obtaining variable *****");
			System.out.println ();

			double[] values = ii_file.get_var_values (iirange, iivar, i_model);
			System.out.println ("Obtained " + values.length + " values");
			System.out.println ();

			if (values.length <= 400) {
				for (int j = 0; j < values.length; ++j) {
					System.out.println (j + ": " + values[j]);
				}
			}
			else {
				for (int j = 0; j < 200; ++j) {
					System.out.println (j + ": " + values[j]);
				}
				System.out.println (". . . " + (values.length - 400) + " values omitted . . .");
				for (int j = values.length - 200; j < values.length; ++j) {
					System.out.println (j + ": " + values[j]);
				}
			}

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
