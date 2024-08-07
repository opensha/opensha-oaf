package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Arrays;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opensha.oaf.oetas.util.OEArraysCalc;
import org.opensha.oaf.oetas.OECatalogBuilder;
import org.opensha.oaf.oetas.OECatalogExaminer;
import org.opensha.oaf.oetas.OECatalogGenerator;
import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogSeedComm;
import org.opensha.oaf.oetas.OECatalogSeeder;
import org.opensha.oaf.oetas.OECatalogStorage;
import org.opensha.oaf.oetas.OECatalogView;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEExaminerPrintSummary;
import org.opensha.oaf.oetas.OEExaminerSaveList;
import org.opensha.oaf.oetas.OEGenerationInfo;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESimulator;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.rj.CompactEqkRupList;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;


// Tests of parameter fitting methods.
// Author: Michael Barall 10/08/2020.

public class OEFitTest {

	//----- Constants -----




	//----- Test subroutines -----




	// Run a smoke test on the fitting code.

	public static void fit_smoke_test (OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

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
			SimpleUtils.show_memory_status();

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
			SimpleUtils.show_memory_status();

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
		SimpleUtils.show_memory_status();

		return;
	}




	// Lay out an a/ams likelihood grid.
	// grid[i][j] contains the likelihood value for a_range[i] and ams_range[j].

	public static String layout_a_ams_like_grid (double[][] grid, double[] a_range, double[] ams_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEArraysCalc.find_array_max (grid, ix);

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
		SimpleUtils.show_memory_status();

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
			SimpleUtils.show_memory_status();

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
		double max_like_v = OEArraysCalc.find_array_max (vec, ix_v);

		result.append (String.format("vector: max_like = %.7e at a_range[%d] = %.3e",
				max_like_v, ix_v[0], a_range[ix_v[0]]));
		result.append ("\n");

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEArraysCalc.find_array_max (grid, ix);

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




	// Lay out an c/p/a likelihood vector and and c/p/a/ams likelihood grid.
	// vec[k][l][i] contains the likelihood value for c_range[k], p_range[l], a_range[i].
	// grid[k][l][i][j] contains the likelihood value for c_range[k], p_range[l], a_range[i], ams_range[j].

	public static String layout_c_p_a_ams_like_vec_grid (double[][][] vec, double[][][][] grid,
				double[] a_range, double[] ams_range, double[] p_range, double[] c_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the vector

		int[] ix_v = new int[3];
		double max_like_v = OEArraysCalc.find_array_max (vec, ix_v);

		result.append (String.format("vector: max_like = %.7e at c_range[%d] = %.3e, p_range[%d] = %.3e, a_range[%d] = %.3e",
				max_like_v, ix_v[0], c_range[ix_v[0]], ix_v[1], p_range[ix_v[1]], ix_v[2], a_range[ix_v[2]]));
		result.append ("\n");

		// Find the maximum value in the grid

		int[] ix = new int[4];
		double max_like = OEArraysCalc.find_array_max (grid, ix);

		result.append (String.format("grid: max_like = %.7e at c_range[%d] = %.3e, p_range[%d] = %.3e, a_range[%d] = %.3e, ams_range[%d] = %.3e",
				max_like, ix[0], c_range[ix[0]], ix[1], p_range[ix[1]], ix[2], a_range[ix[2]], ix[3], ams_range[ix[3]]));
		result.append ("\n");

		// Find ams index range, for a maximum of 11 columns

		int amsix_lo = Math.max(0, Math.min(ix[3] - 5, ams_range.length - 11));
		int amsix_hi = Math.min(amsix_lo + 11, ams_range.length);

		// Header line, space followed by the ams-values

		result.append ("           |            |");
		for (int j = amsix_lo; j < amsix_hi; ++j) {
			result.append (String.format(" % .3e", ams_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+------------+");
		for (int j = amsix_lo; j < amsix_hi; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, a-value followed by scaled vector likelihood followed by scaled grid likelihoods

		for (int i = 0; i < a_range.length; ++i) {
			result.append (String.format("% .3e | % .3e |", a_range[i], vec[ix_v[0]][ix_v[1]][i] - max_like_v));
			for (int j = amsix_lo; j < amsix_hi; ++j) {
				result.append (String.format(" % .3e", grid[ix[0]][ix[1]][i][j] - max_like));
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
		SimpleUtils.show_memory_status();

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
			SimpleUtils.show_memory_status();

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

			double max_like_vec = OEArraysCalc.find_array_max (vec, ix_vec);

			// Find the maximum value in the grid

			double max_like_grid = OEArraysCalc.find_array_max (grid, ix_grid);
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
	
		public boolean calc_likelihood (int num_threads, long max_runtime, long progress_time) {

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

				if (progress_time >= 0L) {
					System.out.println ();
					System.out.println (auto_executor.toString());
					System.out.println ();
				}

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
					if (progress_time >= 0L) {
						System.out.println ("Normal termination");
					}
					result = true;
				}

				// Show final state
					
				if (progress_time >= 0L) {
					show_state (start_time);
				}
			}

			// Return the success flag

			return result;
		}

	}




	// Use the fitting code to generate and display an c/p/a likelihood vector and a c/p/a/ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// This uses the multi-threaded code.

	public static void fit_c_p_a_ams_like_vec_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v1 multi_fitter = new MLE_fit_a_ams_p_c_v1();

		multi_fitter.setup (
			history,
			cat_params,
			f_intervals,
			lmr_opt,
			a_range,
			ams_range,
			p_range,
			c_range
		);

		// Calculate the log-likelihoods

		int num_threads = -1;
		long max_runtime = -1L;
		long progress_time = 30000L;
		multi_fitter.calc_likelihood (num_threads, max_runtime, progress_time);
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, final:");
		SimpleUtils.show_memory_status();

		// Display the result

		System.out.println();
		System.out.println(layout_c_p_a_ams_like_vec_grid (multi_fitter.like_vec, multi_fitter.like_grid, a_range, ams_range, p_range, c_range));

		// Discard the fitter

		multi_fitter = null;

		return;
	}




	// Use the fitting code to find the maximum-likelihood values of a c/p/a likelihood vector and a c/p/a/ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// This uses the multi-threaded code.
	// ix_vec is a 3-element array that returns the MLE indexes into c_range, p_range, and a_range, for the case of a == ams.
	// ix_grid is a 4-element array that returns the MLE indexes into c_range, p_range, a_range, and ams_range.

	public static void mle_c_p_a_ams_like_vec_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range,
		int[] ix_vec, int[] ix_grid
	) {
	
		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v1 multi_fitter = new MLE_fit_a_ams_p_c_v1();

		multi_fitter.setup (
			history,
			cat_params,
			f_intervals,
			lmr_opt,
			a_range,
			ams_range,
			p_range,
			c_range
		);

		// Calculate the log-likelihoods

		int num_threads = -1;
		long max_runtime = -1L;
		long progress_time = -1L;
		multi_fitter.calc_likelihood (num_threads, max_runtime, progress_time);

		// Find the maximum value in the vector

		double max_like_v = OEArraysCalc.find_array_max (multi_fitter.like_vec, ix_vec);

		// Find the maximum value in the grid

		double max_like = OEArraysCalc.find_array_max (multi_fitter.like_grid, ix_grid);

		// Discard the fitter

		multi_fitter = null;

		return;
	}




	// Use the fitting code to find marginals for a c/p/a likelihood vector and a c/p/a/ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form cat_params.a + log10(ams_range[i]).  So the powers of ten are (10^cat_params.a) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// This uses the multi-threaded code.
	// marg_vec receives a 3-dimensional marginal for indexes into c_range, p_range, and a_range, for the case of a == ams.
	// marg_grid receives a 4-dimensional marginal for indexes into c_range, p_range, a_range, and ams_range.

	public static void marg_c_p_a_ams_like_vec_grid (
		OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range,
		OEGridMarginal marg_vec, OEGridMarginal marg_grid
	) {
	
		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v1 multi_fitter = new MLE_fit_a_ams_p_c_v1();

		multi_fitter.setup (
			history,
			cat_params,
			f_intervals,
			lmr_opt,
			a_range,
			ams_range,
			p_range,
			c_range
		);

		// Calculate the log-likelihoods

		int num_threads = -1;
		long max_runtime = -1L;
		long progress_time = -1L;
		multi_fitter.calc_likelihood (num_threads, max_runtime, progress_time);

		// Find the marginals for the vector

		boolean f_has_prob = false;
		boolean f_convert = false;

		marg_vec.marginals_from_grid (multi_fitter.like_vec, f_has_prob, f_convert);

		// Find the marginals for the grid

		marg_grid.marginals_from_grid (multi_fitter.like_grid, f_has_prob, f_convert);

		// Discard the fitter

		multi_fitter = null;

		return;
	}




	// Fetch the mainshock data and aftershock catalog, given an event id.
	// Parameters:
	//  event_id = Event id of the mainshock.
	//  start_lag_days = Catalog start time in days since the mainshock (typically <= 0).
	//  forecast_lag_days = Catalog end time in days since the mainshock.
	//  fcmain = Receives the mainshock information.
	//  fcparams = Receives the forecast parameters, with R&J forecasts suppressed.
	//  fcresults = Receives the forecast results, including the catalog.
	//  f_verbose = True to display the mainshock, parameters, and results.
	// Note: On return, fcresults.catalog_aftershocks contains the catalog as a CompactEqkRupList.

	public static void fetch_mainshock_and_catalog (
		String event_id, double start_lag_days, double forecast_lag_days,
		ForecastMainshock fcmain, ForecastParameters fcparams, ForecastResults fcresults,
		boolean f_verbose
	) {

		// Fetch just the mainshock info

		fcmain.setup_mainshock_only (event_id);

		if (f_verbose) {
			System.out.println ("");
			System.out.println (fcmain.toString());
		}

		// Set the start time and forecast time to be the given number of days after the mainshock

		long the_start_lag = Math.round(ComcatOAFAccessor.day_millis * start_lag_days);
		long the_forecast_lag = Math.round(ComcatOAFAccessor.day_millis * forecast_lag_days);

		// Get the advisory lag

		ActionConfig action_config = new ActionConfig();
		long the_advisory_lag = ForecastResults.forecast_lag_to_advisory_lag (the_forecast_lag, action_config);

		// Get parameters, suppressing R&J forecasts

		fcparams.fetch_all_params (the_forecast_lag, fcmain, null, the_start_lag);
		fcparams.set_rj_suppress();

		if (f_verbose) {
			System.out.println ("");
			System.out.println (fcparams.toString());
		}

		// Get results

		fcresults.calc_all (fcmain.mainshock_time + the_forecast_lag, the_advisory_lag, "", fcmain, fcparams, ForecastResults.forecast_lag_to_f_seq_spec (the_forecast_lag, action_config));
	
		if (f_verbose) {
			System.out.println ("");
			System.out.println (fcresults.toString());
		}

		return;
	}




	// Fetch the ETAS rupture list, given an event id.
	// Parameters:
	//  event_id = Event id of the mainshock.
	//  start_lag_days = Catalog start time in days since the mainshock (typically <= 0).
	//  forecast_lag_days = Catalog end time in days since the mainshock.
	//  fcmain = Receives the mainshock information.
	//  fcparams = Receives the forecast parameters, with R&J forecasts suppressed.
	//  fcresults = Receives the forecast results, including the catalog.
	//  oeorigin = Receives the ETAS origin.
	//  rup_list = Receives the ETAS rupture list.
	//  f_verbose = True to display the mainshock, parameters, R&J results, and ETAS origin.
	// Note: The mainshock is included in the rupture list (whether or not it lies within the catalog time range).

	public static void fetch_etas_rup_list (
		String event_id, double start_lag_days, double forecast_lag_days,
		ForecastMainshock fcmain, ForecastParameters fcparams, ForecastResults fcresults,
		OEOrigin oe_origin, Collection<OERupture> rup_list,
		boolean f_verbose
	) {

		// Fetch the mainshock data and aftershock catalog

		fetch_mainshock_and_catalog (
			event_id, start_lag_days, forecast_lag_days,
			fcmain, fcparams, fcresults,
			f_verbose
		);

		// Set the origin to the mainshock

		oe_origin.set_from_mainshock (fcmain);

		if (f_verbose) {
			System.out.println ("");
			System.out.println (oe_origin.toString());
		}

		// Convert the compact catalog

		CompactEqkRupList compact_cat = fcresults.catalog_aftershocks;
		oe_origin.convert_compact_cat_to_etas_list (rup_list, compact_cat, fcmain);

		return;
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				OEExaminerPrintSummary examiner = new OEExaminerPrintSummary();

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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

				SimpleUtils.show_memory_status();

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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, false);

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
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

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




		// Subcommand : Test #8
		// Command format:
		//  test8  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #6, except using the multi-threading code
		// (even though only one thread will be used).

		if (args[0].equalsIgnoreCase ("test8")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test8' subcommand");
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

				System.out.println ("Generating catalog and history, and generating a/ams grid, multi-threaded");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (1, 0.0, 0.0)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #6, except using the multi-threading code
		// (even though only one thread will be used).
		// Note: Same as text #8, except p is allowed to vary.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test9' subcommand");
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

				System.out.println ("Generating catalog and history, and generating p/a/ams grid, multi-threaded");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max
		//          f_intervals  lmr_opt
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #6, except using the multi-threading code
		// (even though only one thread will be used).
		// Note: Same as text #8, except p and c are allowed to vary.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 22 or more additional arguments

			if (!( args.length >= 23 && args.length % 2 == 1 )) {
				System.err.println ("OEFitTest : Invalid 'test10' subcommand");
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

				System.out.println ("Generating catalog and history, and generating c/p/a/ams grid, multi-threaded");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

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
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #11
		// Command format:
		//  test11  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max
		//          f_intervals  lmr_opt
		//          num_cat
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Note that the parameters are the same as for test #7.
		// Note that this function uses the multi-threaded code.
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a vector and a c/p/a/ams grid with the fitting code.
		// Then for each catalog, write a line containing 19 values:
		//  catalog number
		//  log(ccon) - log(ctrue)
		//  pcon - ptrue
		//  acon - atrue
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - atrue
		//  ams - a
		//  ccon index (vector)
		//  pcon index (vector)
		//  acon index (vector)
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count

		if (args[0].equalsIgnoreCase ("test11")) {

			// 23 or more additional arguments

			if (!( args.length >= 24 && args.length % 2 == 0 )) {
				System.err.println ("OEFitTest : Invalid 'test11' subcommand");
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

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates for c/p/a/ams");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, false);

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
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Arrays to receive maximum likelihood estimates

				int[] ix_vec = new int[3];
				int[] ix_grid = new int[4];

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDiscHistory history = new OEDiscHistory();

					history.build_from_fgh (hist_params, rup_list);

					// Get the maximum likelihood estimate

					mle_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, ix_vec, ix_grid);

					// Display line

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
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




		// Subcommand : Test #12
		// Command format:
		//  test12  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max
		//          f_intervals  lmr_opt
		//          num_cat
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Note that the parameters are the same as for test #7.
		// Note that this function uses the multi-threaded code.
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a vector and a c/p/a/ams grid with the fitting code.
		// Then for each catalog, write a series of lines.
		// Line 1 contains 19 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(ccon) - log(ctrue)
		//  pcon - ptrue
		//  acon - atrue
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - atrue
		//  ams - a
		//  ccon index (vector)
		//  pcon index (vector)
		//  acon index (vector)
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 5 contain a segment of the marginal probability distributions for c/p/a respectively, under the constraint a == ams.
		// Lines 6 - 9 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		// Line 10 contains mean log(ccon), pcon, acon; then std dev; then cov log(ccon)/pcon, log(ccon)/acon, pcon/acon.
		// Line 11 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In lines 10 and 11, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test12")) {

			// 23 or more additional arguments

			if (!( args.length >= 24 && args.length % 2 == 0 )) {
				System.err.println ("OEFitTest : Invalid 'test12' subcommand");
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

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, false);

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
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Ranges we will use for means and covariances

				double[][] range_cpa = new double[4][];
				range_cpa[0] = OEArraysCalc.array_copy_log10 (c_range);
				range_cpa[1] = OEArraysCalc.array_copy (p_range);
				range_cpa[2] = OEArraysCalc.array_copy_log10 (a_range);
				range_cpa[3] = OEArraysCalc.array_copy_log10 (ams_range);

				// Marginals

				OEGridMarginal marg_vec = new OEGridMarginal();
				OEGridMarginal marg_grid = new OEGridMarginal();

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDiscHistory history = new OEDiscHistory();

					history.build_from_fgh (hist_params, rup_list);

					// Get the marginals

					marg_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_vec, marg_grid);

					// Display line, derived from global MLE

					int[] ix_vec = marg_vec.peak_indexes;
					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_vec = marg_vec.marginal_mode_index;
					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display vector marginal probabilities

					int max_els = 19;

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.marginal_probability_as_string (0, max_els)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.marginal_probability_as_string (1, max_els)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.marginal_probability_as_string (2, max_els)
					));

					// Display grid marginal probabilities

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.marginal_probability_as_string (0, max_els)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.marginal_probability_as_string (1, max_els)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.marginal_probability_as_string (2, max_els)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.marginal_probability_as_string (3, max_els)
					));

					// Means and covariances

					double[] mean_vec = new double[3];
					double[][] cov_vec = new double[3][3];

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_vec.calc_mean_covariance (range_cpa, mean_vec, cov_vec);
					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.mean_coveriance_as_string (mean_vec, cov_vec)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.mean_coveriance_as_string (mean_grid, cov_grid)
					));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #13
		// Command format:
		//  test13  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max
		//          f_intervals  lmr_opt
		//          num_cat
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Note that this is the same as test #12 except with the addition of marginal
		// peak probabilities and indexes.
		// Note that the parameters are the same as for test #7.
		// Note that this function uses the multi-threaded code.
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a vector and a c/p/a/ams grid with the fitting code.
		// Then for each catalog, write a series of lines.
		// Line 1 contains 19 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(ccon) - log(ctrue)
		//  pcon - ptrue
		//  acon - atrue
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - atrue
		//  ams - a
		//  ccon index (vector)
		//  pcon index (vector)
		//  acon index (vector)
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 17 contain a segment of the marginal probability distributions for c/p/a respectively, under the constraint a == ams.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  three lines with the corresponding marginal peak indexes in order c/p/a.
		// Lines 18 - 41 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  four lines with the corresponding marginal peak indexes in order c/p/a/ams.
		// Line 42 contains mean log(ccon), pcon, acon; then std dev; then cov log(ccon)/pcon, log(ccon)/acon, pcon/acon.
		// Line 43 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In lines 42 and 43, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test13")) {

			// 23 or more additional arguments

			if (!( args.length >= 24 && args.length % 2 == 0 )) {
				System.err.println ("OEFitTest : Invalid 'test13' subcommand");
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

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
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

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, time_mag_array, false);

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
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Ranges we will use for means and covariances

				double[][] range_cpa = new double[4][];
				range_cpa[0] = OEArraysCalc.array_copy_log10 (c_range);
				range_cpa[1] = OEArraysCalc.array_copy (p_range);
				range_cpa[2] = OEArraysCalc.array_copy_log10 (a_range);
				range_cpa[3] = OEArraysCalc.array_copy_log10 (ams_range);

				// Marginals

				OEGridMarginal marg_vec = new OEGridMarginal();
				OEGridMarginal marg_grid = new OEGridMarginal();

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDiscHistory history = new OEDiscHistory();

					history.build_from_fgh (hist_params, rup_list);

					// Get the marginals

					marg_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_vec, marg_grid);

					// Display line, derived from global MLE

					int[] ix_vec = marg_vec.peak_indexes;
					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_vec = marg_vec.marginal_mode_index;
					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display vector marginal probabilities

					String prefix = String.format ("%6d  ", i_cat);
					String infix = "\n" + prefix;
					String suffix = "";

					int max_els = 19;

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					// Display grid marginal probabilities

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (3, max_els, prefix, infix, suffix));

					// Means and covariances

					double[] mean_vec = new double[3];
					double[][] cov_vec = new double[3][3];

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_vec.calc_mean_covariance (range_cpa, mean_vec, cov_vec);
					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.mean_coveriance_as_string (mean_vec, cov_vec)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.mean_coveriance_as_string (mean_grid, cov_grid)
					));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #14
		// Command format:
		//  test14  event_id  start_lag_days  forecast_lag_days
		// Read the earthquake catalog for a given event id.
		// The start and end times are given in days relative to the time of the mainshock.
		// Use the R&J forecast code, but with the forecasts suppressed.
		// Display mainshock info, R&J forecast parameters, and R&J results.
		// List the aftershock catalog.

		if (args[0].equalsIgnoreCase ("test14")) {

			// 3 or more additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("OEFitTest : Invalid 'test14' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double start_lag_days = Double.parseDouble (args[2]);
				double forecast_lag_days = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Reading mainshock info and catalog");
				System.out.println ("event_id = " + event_id);
				System.out.println ("start_lag_days = " + start_lag_days);
				System.out.println ("forecast_lag_days = " + forecast_lag_days);

				// Fetch the mainshock data and aftershock catalog

				ForecastMainshock fcmain = new ForecastMainshock();
				ForecastParameters fcparams = new ForecastParameters();
				ForecastResults fcresults = new ForecastResults();

				boolean f_verbose = true;

				fetch_mainshock_and_catalog (
					event_id, start_lag_days, forecast_lag_days,
					fcmain, fcparams, fcresults,
					f_verbose
				);

				// Display the catalog, using the element access functions

				CompactEqkRupList fccat = fcresults.catalog_aftershocks;
				int eqk_count = fccat.get_eqk_count();

				System.out.println ("");
				System.out.println ("Aftershock count = " + eqk_count);

				for (int n = 0; n < eqk_count; ++n) {
					long r_time = fccat.get_time(n);
					double r_mag = fccat.get_mag(n);
					double r_lat = fccat.get_lat(n);
					double r_lon = fccat.get_unwrapped_lon(n);
					double r_depth = fccat.get_depth(n);

					String event_info = SimpleUtils.event_info_one_line (r_time, r_mag, r_lat, r_lon, r_depth);
					System.out.println (n + ": " + event_info);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #15
		// Command format:
		//  test15  event_id  start_lag_days  forecast_lag_days
		// Read the ETAS rupture catalog for a given event id.
		// The start and end times are given in days relative to the time of the mainshock.
		// Use the R&J forecast code, but with the forecasts suppressed.
		// Display mainshock info, R&J forecast parameters, R&J results, and ETAS origin.
		// List the ETAS rupture catalog.

		if (args[0].equalsIgnoreCase ("test15")) {

			// 3 or more additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("OEFitTest : Invalid 'test15' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double start_lag_days = Double.parseDouble (args[2]);
				double forecast_lag_days = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Reading ETAS rupture list");
				System.out.println ("event_id = " + event_id);
				System.out.println ("start_lag_days = " + start_lag_days);
				System.out.println ("forecast_lag_days = " + forecast_lag_days);

				// Fetch the ETAS rupture list

				ForecastMainshock fcmain = new ForecastMainshock();
				ForecastParameters fcparams = new ForecastParameters();
				ForecastResults fcresults = new ForecastResults();
				OEOrigin oe_origin = new OEOrigin();
				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();

				boolean f_verbose = true;

				fetch_etas_rup_list (
					event_id, start_lag_days, forecast_lag_days,
					fcmain, fcparams, fcresults,
					oe_origin, rup_list,
					f_verbose
				);

				// Display the catalog

				int eqk_count = rup_list.size();

				System.out.println ("");
				System.out.println ("Rupture count = " + eqk_count);

				int n = 0;
				for (OERupture rup : rup_list) {
					String rup_info = rup.one_line_string (n);
					System.out.println (rup_info);
					++n;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #16
		// Command format:
		//  test16  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max
		//          f_intervals  lmr_opt
		//          event_id
		// Note that this is the same as test #13 except with a catalog retrieved from Comcat.
		// Note that the parameters are the same as for test #13 except that the catalog
		// seed parameters are replaced by the event ID.
		// Note that this function uses the multi-threaded code.
		// Retrieve the aftershock catalog from Comcat.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a vector and a c/p/a/ams grid with the fitting code.
		// Then write a series of lines.
		// Line 1 contains 19 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(ccon) - log(ctrue)
		//  pcon - ptrue
		//  acon - atrue
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - atrue
		//  ams - a
		//  ccon index (vector)
		//  pcon index (vector)
		//  acon index (vector)
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 17 contain a segment of the marginal probability distributions for c/p/a respectively, under the constraint a == ams.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  three lines with the corresponding marginal peak indexes in order c/p/a.
		// Lines 18 - 41 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  four lines with the corresponding marginal peak indexes in order c/p/a/ams.
		// Line 42 contains mean log(ccon), pcon, acon; then std dev; then cov log(ccon)/pcon, log(ccon)/acon, pcon/acon.
		// Line 43 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In lines 42 and 43, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test16")) {

			// 21 additional arguments

			if (!( args.length == 22 )) {
				System.err.println ("OEFitTest : Invalid 'test16' subcommand");
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

				String event_id = args[21];

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

				System.out.println ("# Retrieving aftershock catalog, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
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

				System.out.println ("# event_id = " + event_id);

				System.out.println ("#");

				// Verbose flag

				boolean f_verbose = true;

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

				if (f_verbose) {
					System.out.println (cat_params.toString());
				}

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

				if (f_verbose) {
					System.out.println (hist_params.toString());
				}

				// Parameter ranges

				//double[] a_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				//double[] ams_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Ranges we will use for means and covariances

				double[][] range_cpa = new double[4][];
				range_cpa[0] = OEArraysCalc.array_copy_log10 (c_range);
				range_cpa[1] = OEArraysCalc.array_copy (p_range);
				range_cpa[2] = OEArraysCalc.array_copy_log10 (a_range);
				range_cpa[3] = OEArraysCalc.array_copy_log10 (ams_range);

				// Marginals

				OEGridMarginal marg_vec = new OEGridMarginal();
				OEGridMarginal marg_grid = new OEGridMarginal();

				// Loop over catalogs ...

				int num_cat = 1;

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Time range for rupture list

					double start_lag_days = tbegin;
					double forecast_lag_days = tend;

					// Fetch the ETAS rupture list

					ForecastMainshock fcmain = new ForecastMainshock();
					ForecastParameters fcparams = new ForecastParameters();
					ForecastResults fcresults = new ForecastResults();
					OEOrigin oe_origin = new OEOrigin();
					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();

					fetch_etas_rup_list (
						event_id, start_lag_days, forecast_lag_days,
						fcmain, fcparams, fcresults,
						oe_origin, rup_list,
						f_verbose
					);

					// Make a history

					OEDiscHistory history = new OEDiscHistory();

					history.build_from_fgh (hist_params, rup_list);
					
					//System.out.println (history.dump_string());
					//if (fcmain.mainshock_avail) {	// avoids unreachable code error
					//	return;
					//}

					if (f_verbose) {
						System.out.println (history.toString());
						System.out.println ();
					}

					// Get the marginals

					marg_c_p_a_ams_like_vec_grid (history, cat_params, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_vec, marg_grid);

					// Display line, derived from global MLE

					int[] ix_vec = marg_vec.peak_indexes;
					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_vec = marg_vec.marginal_mode_index;
					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f  % .4f %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_vec[0]]),
						            p_range[ix_vec[1]],
						Math.log10 (a_range[ix_vec[2]]),
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						Math.log10 (ams_range[ix_grid[3]]) - Math.log10 (a_range[ix_grid[2]]),
						ix_vec[0],
						ix_vec[1],
						ix_vec[2],
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display vector marginal probabilities

					String prefix = String.format ("%6d  ", i_cat);
					String infix = "\n" + prefix;
					String suffix = "";

					int max_els = 19;

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_vec.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					// Display grid marginal probabilities

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (3, max_els, prefix, infix, suffix));

					// Means and covariances

					double[] mean_vec = new double[3];
					double[][] cov_vec = new double[3][3];

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_vec.calc_mean_covariance (range_cpa, mean_vec, cov_vec);
					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_vec.mean_coveriance_as_string (mean_vec, cov_vec)
					));

					System.out.println (String.format (
						"%6d  %s",
						i_cat,
						marg_grid.mean_coveriance_as_string (mean_grid, cov_grid)
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
