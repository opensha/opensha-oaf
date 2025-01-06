package org.opensha.oaf.oetas.env;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

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
import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;

import org.opensha.oaf.oetas.OEConstants;

import org.opensha.oaf.oetas.fit.OEGridParams;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccum;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccumMarginal;
import org.opensha.oaf.oetas.fit.OEDisc2VoxStatAccumMulti;

import org.opensha.oaf.oetas.util.OEMarginalDistSetBuilder;
import org.opensha.oaf.oetas.util.OEMarginalDistSet;


// Class to load the log-density file for operational ETAS.
// Author: Michael Barall 08/10/2022.
//
// Each line in the log-density file contains the following:
//   b  alpha  c  p  n  zams  zmu  bay_log_density  bay_vox_volume  log_likelihood

public class OEtasLogDensityFile {


	//----- Line -----


	// Class to hold one line in the file.

	public static class LDFLine {

		// ETAS parameters

		double b;
		double alpha;
		double c;
		double p;
		double n;

		double zams;
		double zmu;

		// Log of Bayesian prior

		double bay_log_density;

		// Voxel volume

		double bay_vox_volume;

		// Log likelihood

		double log_likelihood;




		// Get the log-density.
		// Parameters:
		//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.

		public final double get_log_density (double bay_weight) {
			return (
				(bay_weight <= 1.0)
				? ((bay_log_density * bay_weight) + log_likelihood)
				: (bay_log_density + (log_likelihood * (2.0 - bay_weight)))
			);
		}




		// Get the probability.
		// Parameters:
		//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
		//  max_log_density = Maximum log-density.

		public final double get_probability (double bay_weight, double max_log_density) {
			final double norm_log_density = get_log_density (bay_weight) - max_log_density;
			return Math.exp(norm_log_density) * bay_vox_volume;
		}




		// Accumulate marginal distribution.
		// Parameters:
		//  dist_set_builder = Marginal distribution set builder.
		//  gen_max_log_density = Maximum log density for generic.
		//  seq_max_log_density = Maximum log density for sequence specific.
		//  bay_max_log_density = Maximum log density for bayesian.

		public final void accum_marginal (
			OEMarginalDistSetBuilder dist_set_builder,
			double gen_max_log_density,
			double seq_max_log_density,
			double bay_max_log_density
		) {
			// Calculate probabilities

			double gen = get_probability (OEConstants.BAY_WT_GENERIC, gen_max_log_density);
			double seq = get_probability (OEConstants.BAY_WT_SEQ_SPEC, seq_max_log_density);
			double bay = get_probability (OEConstants.BAY_WT_BAYESIAN, bay_max_log_density);

			// Accumulate

			dist_set_builder.set_etas_var_b_alpha_c_p_n (
				b,
				alpha,
				c,
				p,
				n
			);
			dist_set_builder.set_etas_var_zams_zmu (
				zams,
				zmu
			);
			dist_set_builder.set_etas_data_gen_seq_bay (
				gen,
				seq,
				bay
			);
			dist_set_builder.accum();

			return;
		}




		// Accumulate marginal distribution.
		// Parameters:
		//  stat_accum = Statistics accumulator.

		public final void accum_marginal (
			OEDisc2VoxStatAccum stat_accum
		) {
			// Accumulate

			stat_accum.vsaccum_set_b_alpha_c_p_n (
				b,
				alpha,
				c,
				p,
				n
			);

			stat_accum.vsaccum_add_data (
				zams,
				zmu,
				bay_log_density,
				bay_vox_volume,
				log_likelihood
			);

			return;
		}




		// Append a line to the supplied string builder.
		// Returns the string builder.
		// (See original in OEDisc2InitStatVox.dump_log_density_to_string().)

		public final StringBuilder append_line (StringBuilder sb) {
			final String prefix = rndf(b) + " " + rndf(alpha) + " " + rndf(c) + " " + rndf(p) + " " + rndf(n) + " ";

			sb.append (prefix);
			sb.append (rndf(zams));
			sb.append (" ");
			sb.append (rndf(zmu));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", bay_log_density));
			//sb.append (rndf(bay_log_density));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", bay_vox_volume));
			//sb.append (rndf(bay_vox_volume));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", log_likelihood));
			//sb.append (rndf(log_likelihood));
			sb.append ("\n");

			return sb;
		}




		// Make a line as a string.

		public final String make_line () {
			return append_line(new StringBuilder()).toString();
		}




		// Parse a line.
		// Return true if success, false if line is blank or a comment.
		// Throw an exception if invalid parse.

		public final boolean parse_line (String line) {
			String trimline = line.trim();
			if (trimline.isEmpty() || trimline.startsWith("#")) {
				return false;
			}
			String[] words = trimline.split("\\s+");
			if (words.length != 10) {
				throw new RuntimeException ("OEtasLogDensityFile.LDFLine.parse_line: Invalid line: " + trimline);
			}
			try {
				b				= Double.parseDouble (words[0]);
				alpha			= Double.parseDouble (words[1]);
				c				= Double.parseDouble (words[2]);
				p				= Double.parseDouble (words[3]);
				n				= Double.parseDouble (words[4]);
				zams			= Double.parseDouble (words[5]);
				zmu				= Double.parseDouble (words[6]);
				bay_log_density	= Double.parseDouble (words[7]);
				bay_vox_volume	= Double.parseDouble (words[8]);
				log_likelihood	= Double.parseDouble (words[9]);
			}
			catch (NumberFormatException e) {
				throw new RuntimeException ("OEtasLogDensityFile.LDFLine.parse_line: Invalid line: " + trimline, e);
			}
			return true;
		}

	}




	//----- File -----




	// The entire file, as a list of lines.

	private List<LDFLine> ldf_file = null;




	// Load the file.
	// Returns the nummber of lines loaded.
	// Throws exception if error.

	public int load_file (String filename) {
		ldf_file = new ArrayList<LDFLine>();

		try (
			BufferedReader br = new BufferedReader (new FileReader (filename));
		){
			LDFLine ldf_line = new LDFLine();
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (ldf_line.parse_line (line)) {
					ldf_file.add (ldf_line);
					ldf_line = new LDFLine();
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException ("OEtasLogDensityFile.load_file: I/O error reading file: " + filename, e);
		}

		if (ldf_file.isEmpty()) {
			throw new RuntimeException ("OEtasLogDensityFile.load_file: No data found in file: " + filename);
		}

		return ldf_file.size();
	}




	// Save the file.
	// Returns the nummber of lines saved.
	// Throws exception if error.

	public int save_file (String filename) {
		if (ldf_file == null || ldf_file.isEmpty()) {
			throw new RuntimeException ("OEtasLogDensityFile.save_file: No data to save into file: " + filename);
		}

		try (
			BufferedWriter buf = new BufferedWriter (new FileWriter (filename));
		) {
			for (LDFLine ldf_line : ldf_file) {
				buf.write (ldf_line.make_line());
			}
		}
		catch (IOException e) {
			throw new RuntimeException ("OEtasLogDensityFile.save_file: I/O error writing file: " + filename, e);
		}

		return ldf_file.size();
	}




	// Make the marginal distribution.
	// Parameters:
	//  grid_params = Defines the variable ranges.
	//  f_bivar_marg = True to generate bivariate marginals.
	// Note: This produces marginals that include out-of-range bins.

	public OEMarginalDistSet make_marginal_dist (OEGridParams grid_params, boolean f_bivar_marg) {
		if (ldf_file == null || ldf_file.isEmpty()) {
			throw new RuntimeException ("OEtasLogDensityFile.make_marginal_dist: No data loaded");
		}

		// Loop thru to find the maximum log likelihoods

		double gen_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_GENERIC);
		double seq_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_SEQ_SPEC);
		double bay_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_BAYESIAN);

		for (LDFLine ldf_line : ldf_file) {
			gen_max_log_density = Math.max (gen_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_GENERIC));
			seq_max_log_density = Math.max (seq_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_SEQ_SPEC));
			bay_max_log_density = Math.max (bay_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_BAYESIAN));
		}

		// Make the marginal distributions

		OEMarginalDistSetBuilder dist_set_builder = new OEMarginalDistSetBuilder();
		dist_set_builder.add_etas_vars (grid_params, true);
		dist_set_builder.add_etas_data_gen_seq_bay();
		dist_set_builder.begin_accum (f_bivar_marg);

		for (LDFLine ldf_line : ldf_file) {
			ldf_line. accum_marginal (
				dist_set_builder,
				gen_max_log_density,
				seq_max_log_density,
				bay_max_log_density
			);
		}

		OEMarginalDistSet dist_set = dist_set_builder.end_etas_accum();
		return dist_set;
	}




	// Make the marginal distribution.
	// Parameters:
	//  grid_params = Defines the variable ranges.
	//  f_full = True to generate full marginals (including bivariate), false for slim marginals.
	//  bay_weight = Bayesian prior weight: 0.0 = sequence-specific 1.0 = Bayesian, 2.0 = generic.
	// Note: This version uses the statistics accumulator instead of the distribution set builder.
	// Note: This produces marginals that include out-of-range bins.

	public OEMarginalDistSet make_marginal_dist (OEGridParams grid_params, boolean f_full, double bay_weight) {
		if (ldf_file == null || ldf_file.isEmpty()) {
			throw new RuntimeException ("OEtasLogDensityFile.make_marginal_dist: No data loaded");
		}

		// Loop thru to find the maximum log likelihoods

		double act_max_log_density = ldf_file.get(0).get_log_density (bay_weight);
		double gen_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_GENERIC);
		double seq_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_SEQ_SPEC);
		double bay_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_BAYESIAN);

		for (LDFLine ldf_line : ldf_file) {
			act_max_log_density = Math.max (act_max_log_density, ldf_line.get_log_density (bay_weight));
			gen_max_log_density = Math.max (gen_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_GENERIC));
			seq_max_log_density = Math.max (seq_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_SEQ_SPEC));
			bay_max_log_density = Math.max (bay_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_BAYESIAN));
		}

		// Make the marginal distributions

		OEDisc2VoxStatAccumMarginal stat_accum = new OEDisc2VoxStatAccumMarginal (grid_params, f_full, true);

		stat_accum.vsaccum_begin (
			bay_weight,
			act_max_log_density,
			gen_max_log_density,
			seq_max_log_density,
			bay_max_log_density
		);

		for (LDFLine ldf_line : ldf_file) {
			ldf_line. accum_marginal (
				stat_accum
			);
		}

		stat_accum.vsaccum_end();

		OEMarginalDistSet dist_set = stat_accum.get_dist_set();
		return dist_set;
	}




	// Make the marginal distribution.
	// Parameters:
	//  grid_params = Defines the variable ranges.
	//  f_full = True to generate both slim and full marginals (including bivariate), false for slim marginals only.
	//  bay_weight = Bayesian prior weight: 0.0 = sequence-specific 1.0 = Bayesian, 2.0 = generic.
	// Returns a 2-element array.  The first is the slim marginal set, and the seconds is
	// the full marginal set (including bivariate) or null.
	// Note: This version uses the statistics multi-accumulator.
	// Note: This produces marginals that do not include out-of-range bins.

	public OEMarginalDistSet[] make_marginal_dist_2 (OEGridParams grid_params, boolean f_full, double bay_weight) {
		if (ldf_file == null || ldf_file.isEmpty()) {
			throw new RuntimeException ("OEtasLogDensityFile.make_marginal_dist: No data loaded");
		}

		// Loop thru to find the maximum log likelihoods

		double act_max_log_density = ldf_file.get(0).get_log_density (bay_weight);
		double gen_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_GENERIC);
		double seq_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_SEQ_SPEC);
		double bay_max_log_density = ldf_file.get(0).get_log_density (OEConstants.BAY_WT_BAYESIAN);

		for (LDFLine ldf_line : ldf_file) {
			act_max_log_density = Math.max (act_max_log_density, ldf_line.get_log_density (bay_weight));
			gen_max_log_density = Math.max (gen_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_GENERIC));
			seq_max_log_density = Math.max (seq_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_SEQ_SPEC));
			bay_max_log_density = Math.max (bay_max_log_density, ldf_line.get_log_density (OEConstants.BAY_WT_BAYESIAN));
		}

		// Make the marginal distributions

		OEDisc2VoxStatAccumMarginal stat_accum_slim = new OEDisc2VoxStatAccumMarginal (grid_params, false, false);

		OEDisc2VoxStatAccumMarginal stat_accum_full = null;
		if (f_full) {
			stat_accum_full = new OEDisc2VoxStatAccumMarginal (grid_params, true, false);
		}

		OEDisc2VoxStatAccum stat_accum = (new OEDisc2VoxStatAccumMulti (stat_accum_slim, stat_accum_full)).get_stat_accum();

		stat_accum.vsaccum_begin (
			bay_weight,
			act_max_log_density,
			gen_max_log_density,
			seq_max_log_density,
			bay_max_log_density
		);

		for (LDFLine ldf_line : ldf_file) {
			ldf_line. accum_marginal (
				stat_accum
			);
		}

		stat_accum.vsaccum_end();

		OEMarginalDistSet[] dist_sets = new OEMarginalDistSet[2];
		dist_sets[0] = stat_accum_slim.get_dist_set();
		if (f_full) {
			dist_sets[1] = stat_accum_full.get_dist_set();
		} else {
			dist_sets[1] = null;
		}

		return dist_sets;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasLogDensityFile");




		// Subcommand : Test #1
		// Command format:
		//  test1  load_filename
		// Load log-density from a file.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Load file");
			String load_filename = testargs.get_string ("load_filename");
			testargs.end_test();

			// Load the file.

			System.out.println ();
			System.out.println ("***** Loading file *****");
			System.out.println ();

			OEtasLogDensityFile density_file = new OEtasLogDensityFile();
			int line_count = density_file.load_file (load_filename);
			System.out.println ("Loaded " + line_count + " lines");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  load_filename  save_filename
		// Load log-density from a file, then save it to a file.

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

			OEtasLogDensityFile density_file = new OEtasLogDensityFile();
			int line_count = density_file.load_file (load_filename);
			System.out.println ("Loaded " + line_count + " lines");

			System.out.println ();
			System.out.println ("***** Saving file *****");
			System.out.println ();

			line_count = density_file.save_file (save_filename);
			System.out.println ("Saved " + line_count + " lines");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  load_filename  param_filename  f_bivar_marg  dist_filename
		// Load log-density from a file, compute marginal distributions, and save them to a file.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Load file, then compute marginal distributions");
			String load_filename = testargs.get_string ("load_filename");
			String param_filename = testargs.get_string ("param_filename");
			boolean f_bivar_marg = testargs.get_boolean ("f_bivar_marg");
			String dist_filename = testargs.get_string ("dist_filename");
			testargs.end_test();

			// Load the parameter file

			System.out.println ();
			System.out.println ("***** Loading parameter file *****");
			System.out.println ();

			OEtasParameters param_file = new OEtasParameters();
			MarshalUtils.from_json_file (param_file, param_filename);

			OEGridParams grid_params = param_file.make_grid_params();

			// Load the log-density file.

			System.out.println ();
			System.out.println ("***** Loading log-density file *****");
			System.out.println ();

			OEtasLogDensityFile density_file = new OEtasLogDensityFile();
			int line_count = density_file.load_file (load_filename);
			System.out.println ("Loaded " + line_count + " lines");

			// Make the marginal distribution

			System.out.println ();
			System.out.println ("***** Making marginal distribution *****");
			System.out.println ();

			long start_time = System.currentTimeMillis();

			OEMarginalDistSet dist_set = density_file.make_marginal_dist (grid_params, f_bivar_marg);

			long end_time = System.currentTimeMillis();
			long elapsed_time = end_time - start_time;
			String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
			System.out.println ("Elapsed time = " + s_elapsed_time);
			System.out.println ();

			System.out.println (dist_set.toString());

			// Save the marginal distribution

			System.out.println ();
			System.out.println ("***** Saving marginal distribution *****");
			System.out.println ();

			MarshalUtils.to_formatted_compact_json_file (dist_set, dist_filename);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  load_filename  param_filename  f_full  bay_weight  dist_filename
		// Load log-density from a file, compute marginal distributions, and save them to a file.
		// This test uses the voxel statistics accumulator.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Load file, then compute marginal distributions");
			String load_filename = testargs.get_string ("load_filename");
			String param_filename = testargs.get_string ("param_filename");
			boolean f_full = testargs.get_boolean ("f_full");
			double bay_weight = testargs.get_double ("bay_weight");
			String dist_filename = testargs.get_string ("dist_filename");
			testargs.end_test();

			// Load the parameter file

			System.out.println ();
			System.out.println ("***** Loading parameter file *****");
			System.out.println ();

			OEtasParameters param_file = new OEtasParameters();
			MarshalUtils.from_json_file (param_file, param_filename);

			OEGridParams grid_params = param_file.make_grid_params();

			// Load the log-density file.

			System.out.println ();
			System.out.println ("***** Loading log-density file *****");
			System.out.println ();

			OEtasLogDensityFile density_file = new OEtasLogDensityFile();
			int line_count = density_file.load_file (load_filename);
			System.out.println ("Loaded " + line_count + " lines");

			// Make the marginal distribution

			System.out.println ();
			System.out.println ("***** Making marginal distribution *****");
			System.out.println ();

			long start_time = System.currentTimeMillis();

			OEMarginalDistSet dist_set = density_file.make_marginal_dist (grid_params, f_full, bay_weight);
	
			long end_time = System.currentTimeMillis();
			long elapsed_time = end_time - start_time;
			String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
			System.out.println ("Elapsed time = " + s_elapsed_time);
			System.out.println ();
			
			System.out.println (dist_set.toString());

			// Save the marginal distribution

			System.out.println ();
			System.out.println ("***** Saving marginal distribution *****");
			System.out.println ();

			MarshalUtils.to_formatted_compact_json_file (dist_set, dist_filename);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  load_filename  param_filename  bay_weight  slim_filename  full_filename
		// Load log-density from a file, compute marginal distributions, and save them to files.
		// The full_filename can be "-" if full (bivariate) output is not requested.
		// This test uses the voxel statistics multi-accumulator, and does not produce out-of-range bins.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Load file, then compute marginal distributions");
			String load_filename = testargs.get_string ("load_filename");
			String param_filename = testargs.get_string ("param_filename");
			double bay_weight = testargs.get_double ("bay_weight");
			String slim_filename = testargs.get_string ("slim_filename");
			String full_filename = testargs.get_string ("full_filename");
			testargs.end_test();

			boolean f_full = true;
			if (full_filename.equals ("-")) {
				f_full = false;
			}

			// Load the parameter file

			System.out.println ();
			System.out.println ("***** Loading parameter file *****");
			System.out.println ();

			OEtasParameters param_file = new OEtasParameters();
			MarshalUtils.from_json_file (param_file, param_filename);

			OEGridParams grid_params = param_file.make_grid_params();

			// Load the log-density file.

			System.out.println ();
			System.out.println ("***** Loading log-density file *****");
			System.out.println ();

			OEtasLogDensityFile density_file = new OEtasLogDensityFile();
			int line_count = density_file.load_file (load_filename);
			System.out.println ("Loaded " + line_count + " lines");

			// Make the marginal distributions

			System.out.println ();
			System.out.println ("***** Making marginal distributions *****");
			System.out.println ();

			long start_time = System.currentTimeMillis();

			OEMarginalDistSet[] dist_sets = density_file.make_marginal_dist_2 (grid_params, f_full, bay_weight);

			long end_time = System.currentTimeMillis();
			long elapsed_time = end_time - start_time;
			String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
			System.out.println ("Elapsed time = " + s_elapsed_time);
			System.out.println ();

			System.out.println ("Slim distribution set");
			System.out.println ();
			System.out.println (dist_sets[0].toString());

			if (f_full) {
				System.out.println ();
				System.out.println ("Full distribution set");
				System.out.println ();
				System.out.println (dist_sets[1].toString());
			}

			// Save the marginal distribution

			System.out.println ();
			System.out.println ("***** Saving marginal distribution *****");
			System.out.println ();

			MarshalUtils.to_formatted_compact_json_file (dist_sets[0], slim_filename);

			if (f_full) {
				MarshalUtils.to_formatted_compact_json_file (dist_sets[1], full_filename);
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
