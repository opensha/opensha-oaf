package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Arrays;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opensha.oaf.oetas.OECatalogBuilder;
import org.opensha.oaf.oetas.OECatalogExaminer;
import org.opensha.oaf.oetas.OECatalogGenerator;
import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogSeedComm;
import org.opensha.oaf.oetas.OECatalogSeeder;
import org.opensha.oaf.oetas.OECatalogStorage;
import org.opensha.oaf.oetas.OECatalogView;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEDiscreteRange;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEGenerationInfo;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;


// Tests of parameter fitting methods.
// Author: Michael Barall 10/08/2020.

public class OEFitTest {

	//----- Constants -----




	//----- Test subroutines -----




	// Make a fixed initializer for given parameters, branch ratio, and seed ruptures.
	// The productivity is specified as a branch ratio.
	// Parameters:
	//  cat_params = Catalog parameters.
	//  time_mag_array = Array with N pairs of elements.  In each pair, the first
	//                   element is time in days, the second element is magnitude.
	//                   It is an error if this array has an odd number of elements.
	//  f_verbose = True to print out the catalog parameters and seed ruptures.

	public static OEEnsembleInitializer make_fixed_initializer (
		OECatalogParams cat_params, double[] time_mag_array, boolean f_verbose) {

		// Print the catalog parameters

		if (f_verbose) {
			System.out.println ();
			System.out.println (cat_params.toString());
		}

		// Make info for the seed generation

		OEGenerationInfo seed_gen_info = (new OEGenerationInfo()).set (
			cat_params.mref,	// gen_mag_min
			cat_params.msup		// gen_mag_max
		);

		// Make the rupture list

		ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
		OERupture.make_time_mag_list (rup_list, time_mag_array);

		// Calculate the productivity for each seed rupture

		for (OERupture rup : rup_list) {
			rup.k_prod = OEStatsCalc.calc_k_corr (
				rup.rup_mag,		// m0
				cat_params,			// cat_params
				seed_gen_info		// gen_info
			);
		}

		// Print the seed rupture list

		if (f_verbose) {
			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_prod_string());
			}
		}

		// Put all of it into an initializer (subclass of OEEnsembleInitializer)

		OEInitFixedState initializer = new OEInitFixedState();
		initializer.setup (cat_params, seed_gen_info, rup_list);

		return initializer;
	}




	// A catalog examiner that prints out a summary of the catalog.

	public static class ExaminerPrintSummary implements OECatalogExaminer {

		// Examine the catalog.
		// Parameters:
		//  view = Catalog view.
		//  rangen = Random number generator to use.

		@Override
		public void examine_cat (OECatalogView view, OERandomGenerator rangen) {

			// Print a catalog summary

			System.out.println ();
			System.out.println (view.summary_and_gen_list_string());
			return;
		}
	}




	// A catalog examiner that saves the catalog into a list.
	// Note: The ordering of the ruptures is unspecified.

	public static class ExaminerSaveList implements OECatalogExaminer {

		// The list to receive the catalog contents.

		public Collection<OERupture> rup_list;

		// Flag to also print out a summary of the list.

		public boolean f_verbose;

		// Constructor specifies the list.

		public ExaminerSaveList (Collection<OERupture> rup_list, boolean f_verbose) {
			this.rup_list = rup_list;
			this.f_verbose = f_verbose;
		}

		// Examine the catalog.
		// Parameters:
		//  view = Catalog view.
		//  rangen = Random number generator to use.

		@Override
		public void examine_cat (OECatalogView view, OERandomGenerator rangen) {

			// Print a catalog summary, if desired

			if (f_verbose) {
				System.out.println ();
				System.out.println (view.summary_and_gen_list_string());
			}

			// Add all the ruptures to the list

			view.dump_to_collection (rup_list);
			return;
		}
	}




	// Generate a single catalog, using the given initializer and examiner.

	public static void gen_single_catalog (OEEnsembleInitializer initializer, OECatalogExaminer examiner) {

		// Tell the initializer to begin initializing catalogs

		initializer.begin_initialization();

		// Here begins code which could be per-thread

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Create a seeder for our initializer, which we re-use for each catalog

		OECatalogSeeder seeder = initializer.make_seeder();

		// Allocate a seeder communication area, which we re-use for each catalog

		OECatalogSeedComm seed_comm = new OECatalogSeedComm();

		// Allocate the storage (subclass of OECatalogBuilder and OECatalogView), which we re-use for each catalog

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Allocate a generator, which we re-use for each catalog

		OECatalogGenerator cat_generator = new OECatalogGenerator();

		// Here begins code which could be per-catalog

		// Set up the seeder communication area

		seed_comm.setup_seed_comm (cat_storage, rangen);

		// Open the seeder

		seeder.open();

		// Seed the catalog

		seeder.seed_catalog (seed_comm);

		// Close the seeder

		seeder.close();

		// Set up the catalog generator
				
		cat_generator.setup (rangen, cat_storage, false);

		// Calculate all generations and end the catalog

		cat_generator.calc_all_gen();

		// Tell the generator to forget the catalog

		cat_generator.forget();

		// Examine the catalog

		examiner.examine_cat (cat_storage, rangen);

		// Here ends code which could be per-catalog

		// Here ends code which could be per-thread

		// Tell the initializer to end initializing catalogs

		initializer.end_initialization();

		return;
	}




	// Show the current memory status.

	public static void show_memory_status () {
		long max_memory = Runtime.getRuntime().maxMemory();
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		if (max_memory == Long.MAX_VALUE) {
			System.out.println ("max_memory = unlimited");
		} else {
			System.out.println ("max_memory = " + (max_memory / 1048576L) + " M");
		}
			
		System.out.println ("total_memory = " + (total_memory / 1048576L) + " M");
		System.out.println ("free_memory = " + (free_memory / 1048576L) + " M");
		System.out.println ("used_memory = " + (used_memory / 1048576L) + " M");
		return;
	}




	// Run a smoke test on the fitting code.

	public static void fit_smoke_test (OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		show_memory_status();

		// Create the fitter

		OEDiscExtFit fitter = new OEDiscExtFit();

		boolean f_likelihood = true;
		int i_start_rup = history.i_mainshock;
		fitter.dfit_build (history, cat_params, f_intervals, f_likelihood, lmr_opt, i_start_rup);

		// Allocate the data structures and obtain their handles

		try (
			OEDiscExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDiscExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDiscExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDiscExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
		) {

			// Display memory status

			System.out.println();
			System.out.println ("Memory status, after allocation:");
			show_memory_status();

			// Build the magnitude-exponent data structures

			mexp.mexp_build (cat_params.b, cat_params.alpha);

			// Build the Omori matrix data structures

			omat.omat_build (cat_params.p, cat_params.c);

			// Build the magnitude-Omori pair data structures

			pmom.pmom_build (mexp, omat);

			// Build the a-value-productivity data structures

			double ten_aint_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			avpr.avpr_build (pmom, ten_aint_q);

			// Display the fitter contents summary

			System.out.println();
			System.out.println(fitter.toString());
			System.out.println(avpr.toString());

			// Display memory status

			System.out.println();
			System.out.println ("Memory status, after building:");
			show_memory_status();

			// Calculate a likelihood

			double ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();
			double ten_ams_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			double loglike = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);

			System.out.println();
			System.out.println ("loglike = " + loglike);
		}

		// Discard the fitter

		fitter = null;
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, final:");
		show_memory_status();

		return;
	}




	// Lay out an a/ams likelihood grid.
	// grid[i][j] contains the likelihood value for a_range[i] and ams_range[j].

	public static String layout_a_ams_like_grid (double[][] grid, double[] a_range, double[] ams_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEStatsCalc.find_array_max (grid, ix);

		result.append (String.format("max_like = %.7e at a_range[%d] = %.3e and ams_range[%d] = %.3e",
				max_like, ix[0], a_range[ix[0]], ix[1], ams_range[ix[1]]));
		result.append ("\n");

		// Header line, space followed by the ams-values

		result.append ("           |");
		for (int j = 0; j < ams_range.length; ++j) {
			result.append (String.format(" % .3e", ams_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+");
		for (int j = 0; j < ams_range.length; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, a-value followed by scaled likelihoods

		for (int i = 0; i < a_range.length; ++i) {
			result.append (String.format("% .3e |", a_range[i]));
			for (int j = 0; j < ams_range.length; ++j) {
				result.append (String.format(" % .3e", grid[i][j] - max_like));
			}
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Use the fitting code to generate and display an a-ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].

	public static void fit_a_ams_like_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		show_memory_status();

		// Create the fitter

		OEDiscExtFit fitter = new OEDiscExtFit();

		boolean f_likelihood = true;
		int i_start_rup = history.i_mainshock;
		fitter.dfit_build (history, cat_params, f_intervals, f_likelihood, lmr_opt, i_start_rup);

		// Allocate the data structures and obtain their handles

		try (
			OEDiscExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDiscExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDiscExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDiscExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
		) {

			// Display memory status

			System.out.println();
			System.out.println ("Memory status, after allocation:");
			show_memory_status();

			// Build the magnitude-exponent data structures

			mexp.mexp_build (cat_params.b, cat_params.alpha);

			// Build the Omori matrix data structures

			omat.omat_build (cat_params.p, cat_params.c);

			// Build the magnitude-Omori pair data structures

			pmom.pmom_build (mexp, omat);

			// Display the fitter contents summary

			System.out.println();
			System.out.println(fitter.toString());
			System.out.println(pmom.toString());

			// Likelihood base values

			double base_ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();
			double base_ten_ams_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			// Likelihood matrix

			double[][] grid = new double[a_range.length][ams_range.length];

			// Loop over a-values

			for (int aix = 0; aix < a_range.length; ++aix) {

				double ten_a_q = base_ten_a_q * a_range[aix];

				// Build the a-value-productivity data structures

				double ten_aint_q = ten_a_q;

				avpr.avpr_build (pmom, ten_aint_q);

				// Loop over ams-values

				for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
					double ten_ams_q = base_ten_ams_q * ams_range[amsix];

					// Compute the log-likelihood

					grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
				}
			}

			// Display the result

			System.out.println();
			System.out.println(layout_a_ams_like_grid (grid, a_range, ams_range));
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Lay out an a-value likelihood vector and and a/ams likelihood grid.
	// vec[i] contains the likelihood value for a_range[i].
	// grid[i][j] contains the likelihood value for a_range[i] and ams_range[j].

	public static String layout_a_ams_like_vec_grid (double[] vec, double[][] grid, double[] a_range, double[] ams_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the vector

		int[] ix_v = new int[1];
		double max_like_v = OEStatsCalc.find_array_max (vec, ix_v);

		result.append (String.format("vector: max_like = %.7e at a_range[%d] = %.3e",
				max_like_v, ix_v[0], a_range[ix_v[0]]));
		result.append ("\n");

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEStatsCalc.find_array_max (grid, ix);

		result.append (String.format("grid: max_like = %.7e at a_range[%d] = %.3e and ams_range[%d] = %.3e",
				max_like, ix[0], a_range[ix[0]], ix[1], ams_range[ix[1]]));
		result.append ("\n");

		// Header line, space followed by the ams-values

		result.append ("           |            |");
		for (int j = 0; j < ams_range.length; ++j) {
			result.append (String.format(" % .3e", ams_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+------------+");
		for (int j = 0; j < ams_range.length; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, a-value followed by scaled vector likelihood followed by scaled grid likelihoods

		for (int i = 0; i < a_range.length; ++i) {
			result.append (String.format("% .3e | % .3e |", a_range[i], vec[i] - max_like_v));
			for (int j = 0; j < ams_range.length; ++j) {
				result.append (String.format(" % .3e", grid[i][j] - max_like));
			}
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Use the fitting code to generate and display an a-value likelihood vector and an a-ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].

	public static void fit_a_ams_like_vec_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		show_memory_status();

		// Create the fitter

		OEDiscExtFit fitter = new OEDiscExtFit();

		boolean f_likelihood = true;
		int i_start_rup = history.i_mainshock;
		fitter.dfit_build (history, cat_params, f_intervals, f_likelihood, lmr_opt, i_start_rup);

		// Allocate the data structures and obtain their handles

		try (
			OEDiscExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDiscExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDiscExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDiscExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
		) {

			// Display memory status

			System.out.println();
			System.out.println ("Memory status, after allocation:");
			show_memory_status();

			// Build the magnitude-exponent data structures

			mexp.mexp_build (cat_params.b, cat_params.alpha);

			// Build the Omori matrix data structures

			omat.omat_build (cat_params.p, cat_params.c);

			// Build the magnitude-Omori pair data structures

			pmom.pmom_build (mexp, omat);

			// Display the fitter contents summary

			System.out.println();
			System.out.println(fitter.toString());
			System.out.println(pmom.toString());

			// Likelihood base values

			double base_ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();
			double base_ten_ams_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			// Likelihood vector

			double[] vec = new double[a_range.length];

			// Likelihood matrix

			double[][] grid = new double[a_range.length][ams_range.length];

			// Loop over a-values

			for (int aix = 0; aix < a_range.length; ++aix) {

				double ten_a_q = base_ten_a_q * a_range[aix];

				// Build the a-value-productivity data structures

				double ten_aint_q = ten_a_q;

				avpr.avpr_build (pmom, ten_aint_q);

				// Compute the log-likelihood for vector

				vec[aix] = avpr.avpr_calc_log_like (ten_a_q, ten_a_q);

				// Loop over ams-values

				for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
					double ten_ams_q = base_ten_ams_q * ams_range[amsix];

					// Compute the log-likelihood for grid

					grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
				}
			}

			// Display the result

			System.out.println();
			System.out.println(layout_a_ams_like_vec_grid (vec, grid, a_range, ams_range));
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Use the fitting code to find maximum-likelihood values for an a-value likelihood vector and an a-ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].
	// ix_vec is a one-element array that returns the MLE index into a_range, for the case of a == ams.
	// ix_grid is a two-element array that returns the MLE indexes into a_range and ams_range.

	public static void mle_a_ams_like_vec_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range,
		int[] ix_vec, int[] ix_grid
	) {

		// Create the fitter

		OEDiscExtFit fitter = new OEDiscExtFit();

		boolean f_likelihood = true;
		int i_start_rup = history.i_mainshock;
		fitter.dfit_build (history, cat_params, f_intervals, f_likelihood, lmr_opt, i_start_rup);

		// Allocate the data structures and obtain their handles

		try (
			OEDiscExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDiscExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDiscExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDiscExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
		) {

			// Build the magnitude-exponent data structures

			mexp.mexp_build (cat_params.b, cat_params.alpha);

			// Build the Omori matrix data structures

			omat.omat_build (cat_params.p, cat_params.c);

			// Build the magnitude-Omori pair data structures

			pmom.pmom_build (mexp, omat);

			// Likelihood base values

			double base_ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();
			double base_ten_ams_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			// Likelihood vector

			double[] vec = new double[a_range.length];

			// Likelihood matrix

			double[][] grid = new double[a_range.length][ams_range.length];

			// Loop over a-values

			for (int aix = 0; aix < a_range.length; ++aix) {

				double ten_a_q = base_ten_a_q * a_range[aix];

				// Build the a-value-productivity data structures

				double ten_aint_q = ten_a_q;

				avpr.avpr_build (pmom, ten_aint_q);

				// Compute the log-likelihood for vector

				vec[aix] = avpr.avpr_calc_log_like (ten_a_q, ten_a_q);

				// Loop over ams-values

				for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
					double ten_ams_q = base_ten_ams_q * ams_range[amsix];

					// Compute the log-likelihood for grid

					grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
				}
			}

			// Find the maximum value in the vector

			double max_like_vec = OEStatsCalc.find_array_max (vec, ix_vec);

			// Find the maximum value in the grid

			double max_like_grid = OEStatsCalc.find_array_max (grid, ix_grid);
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Class to use fitting to for a, ams, p, and c; version 1.
	
	public static class MLE_fit_a_ams_p_c_v1 implements SimpleThreadTarget {

		// --- Input parameters ---

		// Catalog history.

		public OEDiscHistory history;

		// Catalog parameters.

		public OECatalogParams cat_params;

		// Interval option.

		public boolean f_intervals;

		// Log-likelihood magnitude range option.

		public int lmr_opt;

		// Range of a-values.
		// a-values are a_base + log10(a_range[aix]).

		public double[] a_range;

		// Range of ams-values.
		// ams-values are a_base + log10(ams_range[amsix]).

		public double[] ams_range;

		// Range of p-values.
		// p-values are cat_params.p + p_range[pix].

		public double[] p_range;

		// Range of c-values.
		// c-values are cat_params.c * c_range[cix].

		public double[] c_range;

		// Set the input parameters.

		public void setup (
			OEDiscHistory history,
			OECatalogParams cat_params,
			boolean f_intervals,
			int lmr_opt,
			double[] a_range,
			double[] ams_range,
			double[] p_range,
			double[] c_range
		) {
			this.history     = history;
			this.cat_params  = cat_params;
			this.f_intervals = f_intervals;
			this.lmr_opt     = lmr_opt;
			this.a_range     = a_range;
			this.ams_range   = ams_range;
			this.p_range     = p_range;
			this.c_range     = c_range;
			return;
		}

		//--- Output parameters ---

		// The value of a_base for each p,c combination.
		// The indexing is a_base[cix][pix].
		// The value a_base is:
		//   cat_params.a + log10(I(cat_params.p, cat_params.c) / I(p, c))
		// where here we define
		//   I(p, c) = Integral(0, cat_params.tend - cat_params.tbegin, ((t+c)^(-p))*dt)
		// This choice causes each value of a_base to correspond to the same branch ratio, because
		//   10^a_base * I(p, c) == 10^cat_params.a * I(cat_params.p, cat_params.c)

		public double[][] a_base;

		// Log-likelihood values under the constraint a == ams.
		// The indexing is like_vec[cix][pix][aix].

		public double [][][] like_vec;

		// Log-likelihood values.
		// The indexing is like_vec[cix][pix][aix][amsix].

		public double [][][][] like_grid;

		// Calculate the Omori scale factor used for a_base.
		// This is I(p, c) as described above.

		private double calc_omori_scale (double p, double c) {
			return OERandomGenerator.omori_rate (p, c, 0.0, cat_params.tend - cat_params.tbegin);
		}

		//--- Internal variables ---

		// Parameter fitter, shared by all threads.

		private OEDiscExtFit fitter;

		// Magnitude-exponent handle, shared by all threads.

		private OEDiscExtFit.MagExponentHandle mexp;

		//--- Work units ---

		// Work unit.
		// Note: This could be a static class.

		private class WorkUnit {
		
			// The p-index.

			public int pix;

			// The c-index.

			public int cix;

			// Constructor sets up the work unit.

			public WorkUnit (int pix, int cix) {
				this.pix = pix;
				this.cix = cix;
			}
		}

		// List of work units.

		private ArrayList<WorkUnit> work_units;

		// Current index into the list of work units.

		private int work_unit_index;

		// Get the next work unit.
		// Returns null if no more work.

		private synchronized WorkUnit get_work_unit () {
			if (work_unit_index >= work_units.size()) {
				return null;
			}
			return work_units.get (work_unit_index++);
		}

		// Get the work unit index.

		private synchronized int get_work_unit_index () {
			return work_unit_index;
		}

		// Get the total number of work units.

		private synchronized int get_work_unit_count () {
			return work_units.size();
		}

		//--- Log-likelihood calculation ---

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			// Allocate the per-thread data structures and obtain their handles

			try (
				OEDiscExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
				OEDiscExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
				OEDiscExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
			) {

				// Loop until prompt termination is requested

				while (!( thread_manager.get_req_termination() )) {

					// Get next work unit, end loop if none

					WorkUnit work_unit = get_work_unit();
					if (work_unit == null) {
						break;
					}

					// Get p and c indexes and values

					int pix = work_unit.pix;
					int cix = work_unit.cix;

					double p = cat_params.p + p_range[pix];
					double c = cat_params.c * c_range[cix];

					// Get scaled a-value

					double a_scaled = cat_params.a + Math.log10(calc_omori_scale(cat_params.p, cat_params.c) / calc_omori_scale(p, c));
					a_base[cix][pix] = a_scaled;

					// Build the Omori matrix data structures

					omat.omat_build (p, c);

					// Build the magnitude-Omori pair data structures

					pmom.pmom_build (mexp, omat);

					// Likelihood base values

					double base_ten_a_q = Math.pow(10.0, a_scaled) * mexp.get_q_correction();
					double base_ten_ams_q = Math.pow(10.0, a_scaled) * mexp.get_q_correction();

					// Likelihood vector

					double[] vec = like_vec[cix][pix];

					// Likelihood matrix

					double[][] grid = like_grid[cix][pix];

					// Loop over a-values

					for (int aix = 0; aix < a_range.length; ++aix) {

						double ten_a_q = base_ten_a_q * a_range[aix];

						// Build the a-value-productivity data structures

						double ten_aint_q = ten_a_q;

						avpr.avpr_build (pmom, ten_aint_q);

						// Compute the log-likelihood for vector

						vec[aix] = avpr.avpr_calc_log_like (ten_a_q, ten_a_q);

						// Loop over ams-values

						for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
							double ten_ams_q = base_ten_ams_q * ams_range[amsix];

							// Compute the log-likelihood for grid

							grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
						}
					}
				}
			}

			return;
		}

		// Show the current state.

		private void show_state (long start_time) {
			long elapsed_time = System.currentTimeMillis() - start_time;
			String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
			System.out.println ("Time = " + s_elapsed_time + ", work units = " + get_work_unit_index() + " of " + get_work_unit_count());
			return;
		}

		// Calculate the log-likelihood values, using multiple threads.
		// Parameters:
		//  num_threads = Number of threads to use, can be -1 to use default number of threads.
		//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
		//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
		// Returns true if success, false if abort or timeout.
	
		public boolean calc_prime_count (int num_threads, long max_runtime, long progress_time) {

			boolean result = false;

			// Allocate arrays for the output parameters

			a_base = new double[c_range.length][p_range.length];
			like_vec = new double[c_range.length][p_range.length][a_range.length];
			like_grid = new double[c_range.length][p_range.length][a_range.length][ams_range.length];

			// Create the work units

			work_units = new ArrayList<WorkUnit>();
			work_unit_index = 0;

			for (int cix = 0; cix < c_range.length; ++cix) {
				for (int pix = 0; pix < p_range.length; ++pix) {
					work_units.add (new WorkUnit (pix, cix));
				}
			}

			// Create the fitter

			fitter = new OEDiscExtFit();

			boolean f_likelihood = true;
			int i_start_rup = history.i_mainshock;
			fitter.dfit_build (history, cat_params, f_intervals, f_likelihood, lmr_opt, i_start_rup);

			// Allocate the executor and global data structures

			try (
				AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
				OEDiscExtFit.MagExponentHandle my_mexp = fitter.make_MagExponentHandle();
			){

				// Save global data structures

				mexp = my_mexp;

				// Build the magnitude-exponent data structures

				mexp.mexp_build (cat_params.b, cat_params.alpha);

				// Display executor info

				System.out.println ();
				System.out.println (auto_executor.toString());
				System.out.println ();

				// Make the thread manager

				SimpleThreadManager thread_manager = new SimpleThreadManager();

				// Launch the threads

				thread_manager.launch_threads (this, auto_executor);

				// Get the start time

				long start_time = thread_manager.get_start_time();

				// Loop until terminated

				while (!( thread_manager.await_termination (max_runtime, progress_time) )) {

					// Display progress message

					show_state (start_time);
				}

				// Check for thread abort

				if (thread_manager.is_abort()) {
					System.out.println ("Stopped because of thread abort");
					System.out.println (thread_manager.get_abort_message_string());
				}

				// Otherwise, check for timeout

				else if (thread_manager.is_timeout()) {
					System.out.println ("Stopped because of timeout");
				}

				// Otherwise, normal termination

				else {
					System.out.println ("Normal termination");
					result = true;
				}

				// Show final state
					
				show_state (start_time);
			}

			// Return the success flag

			return result;
		}

	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEFitTest : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Display catalog summary.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 or more additional arguments

			if (!( args.length >= 12 && args.length % 2 == 0 )) {
				System.err.println ("OEFitTest : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double[] time_mag_array = new double[args.length - 10];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 10]);
				}

				// Say hello

				System.out.println ("Generating catalog with given parameters and seeds");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("tend = " + tend);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	true);

				// Make the catalog examiner

				ExaminerPrintSummary examiner = new ExaminerPrintSummary();

				// Generate a catalog

				gen_single_catalog (initializer, examiner);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Display catalog summary and history contents.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 20 or more additional arguments

			if (!( args.length >= 21 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double magCat = Double.parseDouble (args[10]);
				int helm_param = Integer.parseInt (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				int mag_cat_count = Integer.parseInt (args[13]);
				double eligible_mag = Double.parseDouble (args[14]);
				int eligible_count = Integer.parseInt (args[15]);

				double durlim_ratio = Double.parseDouble (args[16]);
				double durlim_min = Double.parseDouble (args[17]);
				double durlim_max = Double.parseDouble (args[18]);

				double[] time_mag_array = new double[args.length - 19];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 19]);
				}

				// Say hello

				System.out.println ("Generating catalog and history with given parameters and seeds");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("tend = " + tend);

				System.out.println ("magCat = " + magCat);
				System.out.println ("helm_param = " + helm_param);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("mag_cat_count = " + mag_cat_count);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				ExaminerSaveList examiner = new ExaminerSaveList (rup_list, true);

				// Generate a catalog

				gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,			// magCat
					helm_param,		// helm_param
					tbegin,			// t_range_begin
					tend,			// t_range_end
					disc_delta,		// disc_delta
					mag_cat_count,	// mag_cat_count
					eligible_mag,	// eligible_mag
					eligible_count,	// eligible_count
					split_fn		// split_fn
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Display memory status.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 0 or additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test3' subcommand");
				return;
			}

			try {

				System.out.println ("Displaying memory status");

				show_memory_status();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to run a smoke test on the fitting code.
		// Display catalog summary, history contents, and smoke test results.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test4' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double magCat = Double.parseDouble (args[10]);
				int helm_param = Integer.parseInt (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				int mag_cat_count = Integer.parseInt (args[13]);
				double eligible_mag = Double.parseDouble (args[14]);
				int eligible_count = Integer.parseInt (args[15]);

				double durlim_ratio = Double.parseDouble (args[16]);
				double durlim_min = Double.parseDouble (args[17]);
				double durlim_max = Double.parseDouble (args[18]);

				boolean f_intervals = Boolean.parseBoolean (args[19]);
				int lmr_opt = Integer.parseInt (args[20]);

				double[] time_mag_array = new double[args.length - 21];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 21]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and running fit smoke test");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("tend = " + tend);

				System.out.println ("magCat = " + magCat);
				System.out.println ("helm_param = " + helm_param);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("mag_cat_count = " + mag_cat_count);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("f_intervals = " + f_intervals);
				System.out.println ("lmr_opt = " + lmr_opt);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				ExaminerSaveList examiner = new ExaminerSaveList (rup_list, true);

				// Generate a catalog

				gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,			// magCat
					helm_param,		// helm_param
					tbegin,			// t_range_begin
					tend,			// t_range_end
					disc_delta,		// disc_delta
					mag_cat_count,	// mag_cat_count
					eligible_mag,	// eligible_mag
					eligible_count,	// eligible_count
					split_fn		// split_fn
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Run the smoke test

				fit_smoke_test (history, cat_params, f_intervals, lmr_opt);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test5' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double magCat = Double.parseDouble (args[10]);
				int helm_param = Integer.parseInt (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				int mag_cat_count = Integer.parseInt (args[13]);
				double eligible_mag = Double.parseDouble (args[14]);
				int eligible_count = Integer.parseInt (args[15]);

				double durlim_ratio = Double.parseDouble (args[16]);
				double durlim_min = Double.parseDouble (args[17]);
				double durlim_max = Double.parseDouble (args[18]);

				boolean f_intervals = Boolean.parseBoolean (args[19]);
				int lmr_opt = Integer.parseInt (args[20]);

				double[] time_mag_array = new double[args.length - 21];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 21]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("tend = " + tend);

				System.out.println ("magCat = " + magCat);
				System.out.println ("helm_param = " + helm_param);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("mag_cat_count = " + mag_cat_count);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("f_intervals = " + f_intervals);
				System.out.println ("lmr_opt = " + lmr_opt);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				ExaminerSaveList examiner = new ExaminerSaveList (rup_list, true);

				// Generate a catalog

				gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,			// magCat
					helm_param,		// helm_param
					tbegin,			// t_range_begin
					tend,			// t_range_end
					disc_delta,		// disc_delta
					mag_cat_count,	// mag_cat_count
					eligible_mag,	// eligible_mag
					eligible_count,	// eligible_count
					split_fn		// split_fn
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				fit_a_ams_like_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test6' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double magCat = Double.parseDouble (args[10]);
				int helm_param = Integer.parseInt (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				int mag_cat_count = Integer.parseInt (args[13]);
				double eligible_mag = Double.parseDouble (args[14]);
				int eligible_count = Integer.parseInt (args[15]);

				double durlim_ratio = Double.parseDouble (args[16]);
				double durlim_min = Double.parseDouble (args[17]);
				double durlim_max = Double.parseDouble (args[18]);

				boolean f_intervals = Boolean.parseBoolean (args[19]);
				int lmr_opt = Integer.parseInt (args[20]);

				double[] time_mag_array = new double[args.length - 21];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 21]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("tend = " + tend);

				System.out.println ("magCat = " + magCat);
				System.out.println ("helm_param = " + helm_param);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("mag_cat_count = " + mag_cat_count);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("f_intervals = " + f_intervals);
				System.out.println ("lmr_opt = " + lmr_opt);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				ExaminerSaveList examiner = new ExaminerSaveList (rup_list, true);

				// Generate a catalog

				gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,			// magCat
					helm_param,		// helm_param
					tbegin,			// t_range_begin
					tend,			// t_range_end
					disc_delta,		// disc_delta
					mag_cat_count,	// mag_cat_count
					eligible_mag,	// eligible_mag
					eligible_count,	// eligible_count
					split_fn		// split_fn
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the vector and grid

				fit_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         num_cat
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for an a-value vector and an a-ams grid with the fitting code.
		// Then for each catalog, write a line containing 11 values:
		//  catalog number
		//  acon - atrue
		//  a - atrue
		//  ams - atrue
		//  ams - a
		//  acon index (vector)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count

		if (args[0].equalsIgnoreCase ("test7")) {

			// 23 or more additional arguments

			if (!( args.length >= 24 && args.length % 2 == 0 )) {
				System.err.println ("OEFitTest : Invalid 'test7' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tbegin = Double.parseDouble (args[8]);
				double tend = Double.parseDouble (args[9]);

				double magCat = Double.parseDouble (args[10]);
				int helm_param = Integer.parseInt (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				int mag_cat_count = Integer.parseInt (args[13]);
				double eligible_mag = Double.parseDouble (args[14]);
				int eligible_count = Integer.parseInt (args[15]);

				double durlim_ratio = Double.parseDouble (args[16]);
				double durlim_min = Double.parseDouble (args[17]);
				double durlim_max = Double.parseDouble (args[18]);

				boolean f_intervals = Boolean.parseBoolean (args[19]);
				int lmr_opt = Integer.parseInt (args[20]);

				int num_cat = Integer.parseInt (args[21]);

				double[] time_mag_array = new double[args.length - 22];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 22]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFitTest");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates for a/ams");
				System.out.println ("# n = " + n);
				System.out.println ("# p = " + p);
				System.out.println ("# c = " + c);
				System.out.println ("# b = " + b);
				System.out.println ("# alpha = " + alpha);
				System.out.println ("# mref = " + mref);
				System.out.println ("# msup = " + msup);
				System.out.println ("# tbegin = " + tbegin);
				System.out.println ("# tend = " + tend);

				System.out.println ("# magCat = " + magCat);
				System.out.println ("# helm_param = " + helm_param);
				System.out.println ("# disc_delta = " + disc_delta);
				System.out.println ("# mag_cat_count = " + mag_cat_count);
				System.out.println ("# eligible_mag = " + eligible_mag);
				System.out.println ("# eligible_count = " + eligible_count);

				System.out.println ("# durlim_ratio = " + durlim_ratio);
				System.out.println ("# durlim_min = " + durlim_min);
				System.out.println ("# durlim_max = " + durlim_max);

				System.out.println ("# f_intervals = " + f_intervals);
				System.out.println ("# lmr_opt = " + lmr_opt);

				System.out.println ("# num_cat = " + num_cat);

				System.out.println ("# time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("#   time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				System.out.println ("#");

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend	// tend
				);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = make_fixed_initializer (cat_params, time_mag_array,	false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,			// magCat
					helm_param,		// helm_param
					tbegin,			// t_range_begin
					tend,			// t_range_end
					disc_delta,		// disc_delta
					mag_cat_count,	// mag_cat_count
					eligible_mag,	// eligible_mag
					eligible_count,	// eligible_count
					split_fn		// split_fn
				);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Arrays to receive maximum likelihood estimates

				int[] ix_vec = new int[1];
				int[] ix_grid = new int[2];

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					ExaminerSaveList examiner = new ExaminerSaveList (rup_list, false);

					// Generate a catalog

					gen_single_catalog (initializer, examiner);

					// Make a history

					OEDiscHistory history = new OEDiscHistory();

					history.build_from_fgh (hist_params, rup_list);

					// Get the maximum likelihood estimate

					mle_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range, ix_vec, ix_grid);

					// Display line

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (a_range[ix_vec[0]]),
						Math.log10 (a_range[ix_grid[0]]),
						Math.log10 (ams_range[ix_grid[1]]),
						Math.log10 (ams_range[ix_grid[1]]) - Math.log10 (a_range[ix_grid[0]]),
						ix_vec[0],
						ix_grid[0],
						ix_grid[1],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEFitTest : Unrecognized subcommand : " + args[0]);
		return;

	}

}
