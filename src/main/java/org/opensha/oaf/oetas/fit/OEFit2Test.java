package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Arrays;

import org.opensha.oaf.oetas.util.OEArraysCalc;
import org.opensha.oaf.oetas.OECatalogBuilder;
import org.opensha.oaf.oetas.OECatalogExaminer;
import org.opensha.oaf.oetas.OECatalogGenerator;
import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogParamsStats;
import org.opensha.oaf.oetas.OECatalogSeedComm;
import org.opensha.oaf.oetas.OECatalogSeeder;
import org.opensha.oaf.oetas.OECatalogStorage;
import org.opensha.oaf.oetas.OECatalogView;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEExaminerPrintSummary;
import org.opensha.oaf.oetas.OEExaminerSaveList;
import org.opensha.oaf.oetas.OEGenerationInfo;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OEOrigin;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OESeedParamsStats;
import org.opensha.oaf.oetas.OESimulationParams;
import org.opensha.oaf.oetas.OESimulator;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.oetas.except.OEException;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.rj.CompactEqkRupList;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;

import static org.opensha.oaf.oetas.OEConstants.DEF_GR_LO_RATIO;		// group rupture default low ratio
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_HI_RATIO;		// group rupture default high ratio


// Tests of parameter fitting methods.
// Author: Michael Barall 12/15/2022.

public class OEFit2Test {

	//----- Constants -----




	//----- Test subroutines -----




	// Run a smoke test on the fitting code.

	public static void fit_smoke_test (OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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

			double ten_ams_q = Math.pow(10.0, seed_params.ams);

			double loglike;
			if (f_background) {
				loglike = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q, seed_params.mu);
			} else {
				loglike = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
			}

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
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are not used.
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.

	public static void fit_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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

			double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

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

					if (f_background) {
						grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q, seed_params.mu);
					} else {
						grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
					}
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




	// Lay out an a/mu likelihood grid.
	// grid[i][j] contains the likelihood value for a_range[i] and mu_range[j].

	public static String layout_a_mu_like_grid (double[][] grid, double[] a_range, double[] mu_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEArraysCalc.find_array_max (grid, ix);

		result.append (String.format("max_like = %.7e at a_range[%d] = %.3e and mu_range[%d] = %.3e",
				max_like, ix[0], a_range[ix[0]], ix[1], mu_range[ix[1]]));
		result.append ("\n");

		// Header line, space followed by the mu-values

		result.append ("           |");
		for (int j = 0; j < mu_range.length; ++j) {
			result.append (String.format(" % .3e", mu_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+");
		for (int j = 0; j < mu_range.length; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, a-value followed by scaled likelihoods

		for (int i = 0; i < a_range.length; ++i) {
			result.append (String.format("% .3e |", a_range[i]));
			for (int j = 0; j < mu_range.length; ++j) {
				result.append (String.format(" % .3e", grid[i][j] - max_like));
			}
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Use the fitting code to generate and display an a-mu likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// mu-values have the form seed_params.mu * mu_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are not used.
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.

	public static void fit_a_mu_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] mu_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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

			double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

			// Likelihood matrix

			double[][] grid = new double[a_range.length][mu_range.length];

			// Loop over a-values

			for (int aix = 0; aix < a_range.length; ++aix) {

				double ten_a_q = base_ten_a_q * a_range[aix];

				// Build the a-value-productivity data structures

				double ten_aint_q = ten_a_q;

				avpr.avpr_build (pmom, ten_aint_q);

				// Loop over mu-values

				for (int muix = 0; muix < mu_range.length; ++muix) {
				
					double mu_value = seed_params.mu * mu_range[muix];

					// Compute the log-likelihood

					if (f_background) {
						grid[aix][muix] = avpr.avpr_calc_log_like (ten_a_q, base_ten_ams_q, mu_value);
					} else {
						grid[aix][muix] = avpr.avpr_calc_log_like (ten_a_q, base_ten_ams_q);
					}
				}
			}

			// Display the result

			System.out.println();
			System.out.println(layout_a_mu_like_grid (grid, a_range, mu_range));
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Lay out an ams/mu likelihood grid.
	// grid[i][j] contains the likelihood value for ams_range[i] and mu_range[j].

	public static String layout_ams_mu_like_grid (double[][] grid, double[] ams_range, double[] mu_range) {
		StringBuilder result = new StringBuilder();

		// Find the maximum value in the grid

		int[] ix = new int[2];
		double max_like = OEArraysCalc.find_array_max (grid, ix);

		result.append (String.format("max_like = %.7e at ams_range[%d] = %.3e and mu_range[%d] = %.3e",
				max_like, ix[0], ams_range[ix[0]], ix[1], mu_range[ix[1]]));
		result.append ("\n");

		// Header line, space followed by the mu-values

		result.append ("           |");
		for (int j = 0; j < mu_range.length; ++j) {
			result.append (String.format(" % .3e", mu_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+");
		for (int j = 0; j < mu_range.length; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, ams-value followed by scaled likelihoods

		for (int i = 0; i < ams_range.length; ++i) {
			result.append (String.format("% .3e |", ams_range[i]));
			for (int j = 0; j < mu_range.length; ++j) {
				result.append (String.format(" % .3e", grid[i][j] - max_like));
			}
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Lay out a display of grouped productivities.

	public static String layout_grouped_prod (int group_count, double[] a_group_time,
			double[] prod_scnd, double[] prod_main, double[] prod_bkgd, double ten_a_q, double ten_ams_q, double mu_value)
	{
		StringBuilder result = new StringBuilder();

		// Header line with productivity parameters

		result.append (String.format ("ten_a_q = %.4e, ten_ams_q = %.4e, mu_value = %.4e",
				ten_a_q, ten_ams_q, mu_value));
		result.append ("\n");

		// Data lines, one for each group

		for (int i = 0; i < group_count; ++i) {
			result.append (String.format ("t = %12.6f", a_group_time[i]));
			result.append (String.format ("    scnd = %.3e", prod_scnd[i]));
			result.append (String.format ("  main = %.3e", prod_main[i]));
			result.append (String.format ("  bkgd = %.3e", prod_bkgd[i]));
			result.append (String.format ("    scnd_k = %.3e", prod_scnd[i] * ten_a_q));
			result.append (String.format ("  main_k = %.3e", prod_main[i] * ten_ams_q));
			result.append (String.format ("  bkgd_k = %.3e", prod_bkgd[i] * mu_value));
			result.append (String.format ("   k = %.3e", (prod_scnd[i] * ten_a_q) + (prod_main[i] * ten_ams_q) + (prod_bkgd[i] * mu_value) ));
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Use the fitting code to generate and display an ams-mu likelihood grid.
	// Also display the grouped productivities.
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// mu-values have the form seed_params.mu * mu_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are not used.
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.
	// Note that the span and rupture width functions can be null to use default.

	public static void fit_ams_mu_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] ams_range, double[] mu_range, OEDisc2Grouping.SpanWidthFcn span_width_fcn, OEDisc2Grouping.RupWidthFcn rup_width_fcn
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Set up grouping

		fitter.setup_grouping (span_width_fcn, rup_width_fcn);

		int group_count = fitter.get_group_count();
		double[] a_group_time = fitter.get_a_group_time();

		double[] prod_scnd = new double[group_count];
		double[] prod_main = new double[group_count];
		double[] prod_bkgd = new double[group_count];

		// Display fitter info

		System.out.println();
		System.out.println (fitter.toString());

		// Dump the grouping

		System.out.println();
		System.out.println (fitter.get_grouping().dump_string());

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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

			double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

			// Likelihood matrix

			double[][] grid = new double[ams_range.length][mu_range.length];

			// Use a single a-value

			double ten_a_q = base_ten_a_q;

			// Build the a-value-productivity data structures

			double ten_aint_q = ten_a_q;

			avpr.avpr_build (pmom, ten_aint_q);

			// Loop over mu-values

			for (int muix = 0; muix < mu_range.length; ++muix) {
				
				double mu_value = seed_params.mu * mu_range[muix];

				// Loop over ams-values

				for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
					double ten_ams_q = base_ten_ams_q * ams_range[amsix];

					// Compute the log-likelihood

					if (f_background) {
						grid[amsix][muix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q, mu_value);
					} else {
						grid[amsix][muix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
					}
				}
			}

			// Get and display the group productivities

			avpr.avpr_get_grouped_unscaled_prod_all (prod_scnd, prod_main, prod_bkgd);

			System.out.println();
			System.out.println(layout_grouped_prod (group_count, a_group_time,
				prod_scnd, prod_main, prod_bkgd, base_ten_a_q, base_ten_ams_q, seed_params.mu)
			);

			// Display the result

			System.out.println();
			System.out.println(layout_ams_mu_like_grid (grid, ams_range, mu_range));
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Use the fitting code and statistics voxel to generate and display an ams-mu likelihood grid.
	// Also display the grouped productivities.
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// mu-values have the form seed_params.mu * mu_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are used to define the branch ratio time interval..
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.
	// Note that the span and rupture width functions can be null to use default.
	// Note that parameters are the same as fit_ams_mu_like_grid(), with the addition of
	// the last three parameters to specify intended n, zams, and zmu.

	public static void fit_vox_ams_mu_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] ams_range, double[] mu_range, OEDisc2Grouping.SpanWidthFcn span_width_fcn, OEDisc2Grouping.RupWidthFcn rup_width_fcn,
		double n_br, OEDiscreteRange zams_range, OEDiscreteRange zmu_range
	) {

		if (!( zams_range.get_range_size() == ams_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_ams_mu_like_grid: Mismatched ams range length");
		}
		if (!( zmu_range.get_range_size() == mu_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_ams_mu_like_grid: Mismatched mu range length");
		}
	
		// Display memory status

		final String sepline = "----------------------------------------------------------------------";
		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Display the incoming catalog parameters

		System.out.println();
		System.out.println (cat_params.toString());

		// Display the incoming seed parameters

		System.out.println();
		System.out.println (seed_params.toString());

		// Display the parameter ranges

		System.out.println();
		System.out.println ("Parameter values:");
		System.out.println ("n_br = " + n_br);
		System.out.println ("zams_range = " + zams_range.toString());
		System.out.println ("zmu_range = " + zmu_range.toString());

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Set up grouping

		fitter.setup_grouping (span_width_fcn, rup_width_fcn);

		//int group_count = fitter.get_group_count();
		//double[] a_group_time = fitter.get_a_group_time();
		//
		//double[] prod_scnd = new double[group_count];
		//double[] prod_main = new double[group_count];
		//double[] prod_bkgd = new double[group_count];

		// Set up time interval for branch ratio calculation

		fitter.set_tint_br (cat_params.tend - cat_params.tbegin);

		// Display fitter info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (fitter.toString());

		// Dump the grouping

		System.out.println();
		System.out.println (fitter.get_grouping().dump_string());

		// Get the fitting info

		OEDisc2InitFitInfo fit_info = fitter.get_fit_info();

		System.out.println();
		System.out.println (fit_info.toString());

		// Create the arrays defining sub-voxels

		OEDisc2InitSubVoxDef sub_vox_def = new OEDisc2InitSubVoxDef();
		sub_vox_def.set_from_sep_ranges (
			zams_range,
			zmu_range
		);

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (sub_vox_def.dump_sep_zams_velt());

		System.out.println();
		System.out.println (sub_vox_def.dump_sep_zmu_velt());

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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
			System.out.println (sepline);

			System.out.println();
			System.out.println(fitter.toString());
			System.out.println(pmom.toString());

			//// Likelihood base values
			//
			//double base_ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();
			//
			//double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

			// Likelihood matrix

			double[][] grid = new double[ams_range.length][mu_range.length];

			// Create the voxel

			OEDisc2InitStatVox stat_vox = new OEDisc2InitStatVox();

			OEValueElement b_velt = new OEValueElement (cat_params.b);
			OEValueElement alpha_velt = new OEValueElement (cat_params.alpha);
			OEValueElement c_velt = new OEValueElement (cat_params.c);
			OEValueElement p_velt = new OEValueElement (cat_params.p);
			OEValueElement n_velt = new OEValueElement (n_br);

			stat_vox.set_voxel_def (
				b_velt,
				alpha_velt,
				c_velt,
				p_velt,
				n_velt
			);

			// Pass in the sub-voxels

			sub_vox_def.set_subvox_def (stat_vox);

			// Apply a uniform Bayesian prior

			OEBayPrior bay_prior = OEBayPrior.makeUniform();
			stat_vox.apply_bay_prior (bay_prior, fit_info.bay_prior_params);

			// Calculate the log-likelihoods

			stat_vox.calc_likelihood (
				fit_info,
				avpr,
				pmom
			);

			// Dump the voxel

			System.out.println();
			System.out.println (sepline);

			System.out.println();
			System.out.println (stat_vox.dump_string());

			// Get the grid of likelihood values

			sub_vox_def.get_log_density_grid (stat_vox, OEConstants.BAY_WT_BAYESIAN, grid);

			// Display grouped probabilities

			System.out.println();
			System.out.println (sepline);

			final int center_subvox_index = sub_vox_def.get_center_subvox_index();
			System.out.println();
			System.out.println ("center_subvox_index = " + center_subvox_index);

			System.out.println();
			System.out.println (stat_vox.layout_grouped_prod (
				fit_info,
				center_subvox_index
			));

			// Seed the catalog and display it, for the center sub-voxel

			System.out.println();
			System.out.println (sepline);

			System.out.println();
			System.out.println (stat_vox.test_seed_catalog (
				fit_info,
				center_subvox_index
			));

			// Display the result

			System.out.println();
			System.out.println (sepline);

			System.out.println();
			System.out.println(layout_ams_mu_like_grid (grid, ams_range, mu_range));
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Seed a catalog and display it.

	public static String seed_catalog_and_display (OEEnsembleInitializer ensemble_initializer) {
		StringBuilder result = new StringBuilder();

		// Begin initializing catalogs

		ensemble_initializer.begin_initialization();

		// Get a seeder

		OECatalogSeeder seeder = ensemble_initializer.make_seeder();

		// Open the seeder.

		seeder.open();

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Allocate the storage

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Seed communication area

		OECatalogSeedComm comm = new OECatalogSeedComm();
		comm.setup_seed_comm (cat_storage, rangen);

		// Seed the catalog

		seeder.seed_catalog (comm);

		// End the catalog

		comm.cat_builder.end_catalog();

		// Dump it

		result.append (cat_storage.dump_to_string());

		// Close the seeder

		seeder.close();

		// End initializing catalogs

		ensemble_initializer.end_initialization();

		return result.toString();
	}




	// Use the fitting code and statistics voxel set to generate and display an ams-mu likelihood grid.
	// Also display the grouped productivities.
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// mu-values have the form seed_params.mu * mu_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are used to define the branch ratio time interval..
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.
	// Note that the span and rupture width functions can be null to use default.
	// Note that parameters are the same as fit_ams_mu_like_grid(), with the addition of
	// the last three parameters to specify intended n, zams, and zmu.
	// Note: Performs the same function as fit_vox_ams_mu_like_grid, except uses the voxel set
	// and builder to create the voxel.

	public static OEDisc2InitVoxSet fit_vox_set_ams_mu_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] ams_range, double[] mu_range, OEDisc2Grouping.SpanWidthFcn span_width_fcn, OEDisc2Grouping.RupWidthFcn rup_width_fcn,
		double n_br, OEDiscreteRange zams_range, OEDiscreteRange zmu_range
	) {

		if (!( zams_range.get_range_size() == ams_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_ams_mu_like_grid: Mismatched ams range length");
		}
		if (!( zmu_range.get_range_size() == mu_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_ams_mu_like_grid: Mismatched mu range length");
		}
	
		// Display memory status

		final String sepline = "----------------------------------------------------------------------";
		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Display the incoming catalog parameters

		System.out.println();
		System.out.println (cat_params.toString());

		// Display the incoming seed parameters

		System.out.println();
		System.out.println (seed_params.toString());

		// Display the parameter ranges

		System.out.println();
		System.out.println ("Parameter values:");
		System.out.println ("n_br = " + n_br);
		System.out.println ("zams_range = " + zams_range.toString());
		System.out.println ("zmu_range = " + zmu_range.toString());

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Set up grouping

		fitter.setup_grouping (span_width_fcn, rup_width_fcn);

		// Set up time interval for branch ratio calculation

		fitter.set_tint_br (cat_params.tend - cat_params.tbegin);

		// Display fitter info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (fitter.toString());

		// Dump the grouping

		System.out.println();
		System.out.println (fitter.get_grouping().dump_string());

		// Create the grid ranges

		OEGridParams grid_params = new OEGridParams (
			OEDiscreteRange.makeSingle (cat_params.b),		// b_range
			OEDiscreteRange.makeSingle (cat_params.alpha),	// alpha_range
			OEDiscreteRange.makeSingle (cat_params.c),		// c_range
			OEDiscreteRange.makeSingle (cat_params.p),		// p_range
			OEDiscreteRange.makeSingle (n_br),				// n_range
			zams_range,										// zams_range
			zmu_range										// zmu_range
		);

		// Display grid range info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (grid_params.toString());

		// Make a uniform Bayesian prior

		OEBayPrior bay_prior = OEBayPrior.makeUniform();

		// Make the voxel set

		OEDisc2InitVoxSet voxel_set = new OEDisc2InitVoxSet();

		// Make the voxel builder

		OEDisc2InitVoxBuilder voxel_builder = new OEDisc2InitVoxBuilder();

		// Set up shared objects in the builder

		voxel_builder.setup_vbld (
			voxel_set,
			fitter,
			bay_prior
		);

		// Set up grid ranges in the builder

		voxel_builder.setup_grid (
			grid_params
		);

		// Display builder info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_builder.toString());

		// Display the parameter ranges

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_b_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_alpha_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_c_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_p_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_n_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_zams_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_zmu_velt());

		// Create multi-thread context

		int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
		long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
		long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

		try (
			AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
		){
			SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);

			// Build the voxels

			try {
				voxel_builder.build_voxels (exec_timer);
			}
			catch (OEException e) {
				throw new RuntimeException ("OEFit2Test.fit_vox_set_ams_mu_like_grid: Operational ETAS exception: Voxel build failed", e);
			}
		}

		// Display memory status

		System.out.println();
		System.out.println ("Memory status, after fitting:");
		SimpleUtils.show_memory_status();

		// Get the fitting information

		OEDisc2InitFitInfo fit_info = voxel_set.get_fit_info();

		// Create prototype catalog parameters, with statistics zeroed out

		double sim_tbegin = fit_info.group_t_interval_end;
		double t_forecast = fit_info.group_t_interval_end;
		double sim_tend = t_forecast + 365.0;

//		OECatalogParams proto_cat_params = new OECatalogParams();
//		proto_cat_params.set_to_fixed_mag (
//			0.0,				// a
//			0.0,				// p
//			0.0,				// c
//			0.0,				// b
//			0.0,				// alpha
//			fit_info.mref,		// mref
//			fit_info.msup,		// msup
//			sim_tbegin,			// tbegin
//			sim_tend			// tend
//		);
//		proto_cat_params.set_fixed_mag_min (fit_info.mag_min);
//		proto_cat_params.set_fixed_mag_max (fit_info.mag_max);

		OECatalogParams proto_cat_params = new OECatalogParams();
		proto_cat_params.set_to_fixed_mag_limited (
			0.0,				// a
			0.0,				// p
			0.0,				// c
			0.0,				// b
			0.0,				// alpha
			fit_info.mref,		// mref
			fit_info.msup,		// msup
			sim_tbegin,			// tbegin
			sim_tend,			// tend
			fit_info.mag_min,	// mag_min_sim
			fit_info.mag_max	// mag_max_sim
		);

		// Statistics accumulator, full marginal distribution

		OEDisc2VoxStatAccumMarginal stat_accum = new OEDisc2VoxStatAccumMarginal (grid_params, true);

		// Complete setting up the voxel set

		voxel_set.setup_post_fitting (
			proto_cat_params,						// the_cat_params
			t_forecast,								// the_t_forecast
			OEConstants.BAY_WT_BAYESIAN,			// the_bay_weight
			OEConstants.DEF_DENSITY_BIN_SIZE_LNU,	// density_bin_size_lnu
			OEConstants.DEF_DENSITY_BIN_COUNT,		// density_bin_count
			OEConstants.DEF_PROB_TAIL_TRIM,			// prob_tail_trim
			OEConstants.DEF_SEED_SUBVOX_COUNT,		// the_seed_subvox_count
			stat_accum								// stat_accum
		);


		// Display voxel set results

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_set.toString());

		// Get some internals for testing

		OEDisc2InitSubVoxDef sub_vox_def = voxel_builder.test_get_sub_vox_def();
		OEDisc2InitStatVox stat_vox = voxel_set.test_get_voxel (0);

		// Likelihood matrix

		double[][] grid = new double[ams_range.length][mu_range.length];

		// Dump the voxel

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (stat_vox.dump_string());

		// Get the grid of likelihood values

		sub_vox_def.get_log_density_grid (stat_vox, OEConstants.BAY_WT_BAYESIAN, grid);

		// Display grouped probabilities

		System.out.println();
		System.out.println (sepline);

		final int center_subvox_index = sub_vox_def.get_center_subvox_index();
		System.out.println();
		System.out.println ("center_subvox_index = " + center_subvox_index);

		System.out.println();
		System.out.println (stat_vox.layout_grouped_prod (
			fit_info,
			center_subvox_index
		));

		// Seed the catalog and display it, for the center sub-voxel

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (seed_catalog_and_display (voxel_set));

		// Display the result

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println(layout_ams_mu_like_grid (grid, ams_range, mu_range));

		// Discard the fitter

		voxel_builder = null;
		fitter = null;

		return voxel_set;
	}




	// Use the fitting code and statistics voxel set to generate and display a c-p-a-ams likelihood grid.
	// Also display the grouped productivities.
	// a-values have the form a_base(p,c) + log10(a_range[i]).  So the powers of ten are (10^a_base(p,c)) * a_range[i].
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// Note: a_base(p,c) == cat_params.a for p == cat_params.p and c == cat_params.c.  For other values of p and c, 
	//  a_base(p,c) is chosen to have the same branch ratio on the time interval tint_br (or cat_params.tend - cat_params.tbegin if tint_br == 0)
	//  and magnitude range cat_params.msup - cat_params.mref.  So the branch ratio is cat_params.get_br(tint_br) * a_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- if tint_br == 0, then tbegin and tend are used to define the branch ratio time interval.
	// In seed_params:
	//  -- ams and mu provide the central or only value of the corresponding parameter.
	// Note that the span and rupture width functions can be null to use default.
	// Note: Performs the samea similar function as fit_c_p_a_ams_like_grid, except uses the voxel set.

	public static OEDisc2InitVoxSet fit_vox_set_c_p_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range, OEDisc2Grouping.SpanWidthFcn span_width_fcn, OEDisc2Grouping.RupWidthFcn rup_width_fcn,
		OEDiscreteRange v_c_range, OEDiscreteRange v_p_range, OEDiscreteRange v_n_range, OEDiscreteRange v_zams_range, double zmu, double tint_br
	) {

		if (!( v_n_range.get_range_size() == a_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_c_p_a_ams_like_grid: Mismatched n/a range length");
		}
		if (!( v_zams_range.get_range_size() == ams_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_c_p_a_ams_like_grid: Mismatched ams range length");
		}
		if (!( v_p_range.get_range_size() == p_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_c_p_a_ams_like_grid: Mismatched p range length");
		}
		if (!( v_c_range.get_range_size() == c_range.length )) {
			throw new IllegalArgumentException ("OEFit2Test.fit_vox_set_c_p_a_ams_like_grid: Mismatched c range length");
		}
	
		// Display memory status

		final String sepline = "----------------------------------------------------------------------";
		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Display the incoming catalog parameters

		System.out.println();
		System.out.println (cat_params.toString());

		// Display the incoming seed parameters

		System.out.println();
		System.out.println (seed_params.toString());

		// Display the parameter ranges

		System.out.println();
		System.out.println ("Parameter values:");
		System.out.println ("v_c_range = " + v_c_range.toString());
		System.out.println ("v_p_range = " + v_p_range.toString());
		System.out.println ("v_n_range = " + v_n_range.toString());
		System.out.println ("v_zams_range = " + v_zams_range.toString());
		System.out.println ("zmu = " + zmu);

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Set up grouping

		fitter.setup_grouping (span_width_fcn, rup_width_fcn);

		// Set up time interval for branch ratio calculation

		if (tint_br > 0.001) {
			fitter.set_tint_br (tint_br);
		} else {
			fitter.set_tint_br (cat_params.tend - cat_params.tbegin);
		}

		// Display fitter info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (fitter.toString());

		// Dump the grouping

		System.out.println();
		System.out.println (fitter.get_grouping().dump_string());

		// Create the grid ranges

		OEGridParams grid_params = new OEGridParams (
			OEDiscreteRange.makeSingle (cat_params.b),		// b_range
			OEDiscreteRange.makeSingle (cat_params.alpha),	// alpha_range
			v_c_range,										// c_range
			v_p_range,										// p_range
			v_n_range,										// n_range
			v_zams_range,									// zams_range
			OEDiscreteRange.makeSingle (zmu)				// zmu_range
		);

		// Display grid range info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (grid_params.toString());

		// Make a uniform Bayesian prior

		OEBayPrior bay_prior = OEBayPrior.makeUniform();

		// Make the voxel set

		OEDisc2InitVoxSet voxel_set = new OEDisc2InitVoxSet();

		// Make the voxel builder

		OEDisc2InitVoxBuilder voxel_builder = new OEDisc2InitVoxBuilder();

		// Set up shared objects in the builder

		voxel_builder.setup_vbld (
			voxel_set,
			fitter,
			bay_prior
		);

		// Set up grid ranges in the builder

		voxel_builder.setup_grid (
			grid_params
		);

		// Display builder info

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_builder.toString());

		// Display the parameter ranges

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_b_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_alpha_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_c_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_p_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_n_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_zams_velt());

		System.out.println();
		System.out.println (voxel_builder.test_dump_sep_zmu_velt());

		// Create multi-thread context

		int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
		long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
		long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

		try (
			AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
		){
			SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);

			// Build the voxels

			try {
				voxel_builder.build_voxels (exec_timer);
			}
			catch (OEException e) {
				throw new RuntimeException ("OEFit2Test.fit_vox_set_ams_mu_like_grid: Operational ETAS exception: Voxel build failed", e);
			}
		}

		// Display memory status

		System.out.println();
		System.out.println ("Memory status, after fitting:");
		SimpleUtils.show_memory_status();

		// Get the fitting information

		OEDisc2InitFitInfo fit_info = voxel_set.get_fit_info();

		// Create prototype catalog parameters, with statistics zeroed out

		double sim_tbegin = fit_info.group_t_interval_end;
		double t_forecast = fit_info.group_t_interval_end;
		double sim_tend = t_forecast + 365.0;

//		OECatalogParams proto_cat_params = new OECatalogParams();
//		proto_cat_params.set_to_fixed_mag (
//			0.0,				// a
//			0.0,				// p
//			0.0,				// c
//			0.0,				// b
//			0.0,				// alpha
//			fit_info.mref,		// mref
//			fit_info.msup,		// msup
//			sim_tbegin,			// tbegin
//			sim_tend			// tend
//		);
//		proto_cat_params.set_fixed_mag_min (fit_info.mag_min);
//		proto_cat_params.set_fixed_mag_max (fit_info.mag_max);

		OECatalogParams proto_cat_params = new OECatalogParams();
		proto_cat_params.set_to_fixed_mag_limited (
			0.0,				// a
			0.0,				// p
			0.0,				// c
			0.0,				// b
			0.0,				// alpha
			fit_info.mref,		// mref
			fit_info.msup,		// msup
			sim_tbegin,			// tbegin
			sim_tend,			// tend
			fit_info.mag_min,	// mag_min_sim
			fit_info.mag_max	// mag_max_sim
		);

		// Statistics accumulator, full marginal distribution

		OEDisc2VoxStatAccumMarginal stat_accum = new OEDisc2VoxStatAccumMarginal (grid_params, true);

		// Complete setting up the voxel set

		voxel_set.setup_post_fitting (
			proto_cat_params,						// the_cat_params
			t_forecast,								// the_t_forecast
			OEConstants.BAY_WT_BAYESIAN,			// the_bay_weight
			OEConstants.DEF_DENSITY_BIN_SIZE_LNU,	// density_bin_size_lnu
			OEConstants.DEF_DENSITY_BIN_COUNT,		// density_bin_count
			OEConstants.DEF_PROB_TAIL_TRIM,			// prob_tail_trim
			OEConstants.DEF_SEED_SUBVOX_COUNT,		// the_seed_subvox_count
			stat_accum								// stat_accum
		);


		// Display voxel set results

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (voxel_set.toString());

		// Get some internals for testing

		OEDisc2InitSubVoxDef sub_vox_def = voxel_builder.test_get_sub_vox_def();
		final int center_voxel_index = ((((c_range.length / 2) * p_range.length) + (p_range.length / 2)) * a_range.length) + (a_range.length / 2);
		OEDisc2InitStatVox stat_vox = voxel_set.test_get_voxel (center_voxel_index);

		// Log-likelihood values, the indexing is like_grid[cix][pix][aix][amsix]

		double [][][][] like_grid = new double[c_range.length][p_range.length][a_range.length][ams_range.length];

		// Dump the center voxel

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (stat_vox.dump_string());

		// Get the grid of likelihood values

		voxel_set.test_get_c_p_a_ams_log_density_grid (OEConstants.BAY_WT_BAYESIAN, like_grid);

		// Display grouped probabilities

		System.out.println();
		System.out.println (sepline);
		
		final int center_subvox_index = sub_vox_def.get_center_subvox_index();
		System.out.println();
		System.out.println ("center_voxel_index = " + center_voxel_index);
		System.out.println ("center_subvox_index = " + center_subvox_index);
		
		System.out.println();
		System.out.println (stat_vox.layout_grouped_prod (
			fit_info,
			center_subvox_index
		));

		// Seed the catalog and display it, for the center sub-voxel

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println (seed_catalog_and_display (voxel_set));

		// Display the result

		System.out.println();
		System.out.println (sepline);

		System.out.println();
		System.out.println(layout_c_p_a_ams_like_grid (like_grid, a_range, ams_range, p_range, c_range));

		// Discard the fitter

		voxel_builder = null;
		fitter = null;

		return voxel_set;
	}




	// Lay out a c/p/a/ams likelihood grid.
	// grid[k][l][i][j] contains the likelihood value for c_range[k], p_range[l], a_range[i], ams_range[j].

	public static String layout_c_p_a_ams_like_grid (double[][][][] grid,
				double[] a_range, double[] ams_range, double[] p_range, double[] c_range) {
		StringBuilder result = new StringBuilder();

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

		result.append ("           |");
		for (int j = amsix_lo; j < amsix_hi; ++j) {
			result.append (String.format(" % .3e", ams_range[j]));
		}
		result.append ("\n");

		// Separator line

		result.append ("-----------+");
		for (int j = amsix_lo; j < amsix_hi; ++j) {
			result.append ("-----------");
		}
		result.append ("\n");

		// Data lines, a-value followed by followed by scaled grid likelihoods

		for (int i = 0; i < a_range.length; ++i) {
			result.append (String.format("% .3e |", a_range[i]));
			for (int j = amsix_lo; j < amsix_hi; ++j) {
				result.append (String.format(" % .3e", grid[ix[0]][ix[1]][i][j] - max_like));
			}
			result.append ("\n");
		}
	
		return result.toString();
	}




	// Use the fitting code to find maximum-likelihood values for an a-ams likelihood grid.
	// a-values have the form cat_params.a + log10(a_range[i]).  So the powers of ten are (10^cat_params.a) * a_range[i].
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// In cat_params:
	//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
	//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
	//     (which are also the magnitude ranges used for Q-correction of a).
	//  -- tbegin and tend are not used.
	// In seed_params:
	//  -- ams provides the central value of the corresponding parameter.
	//  -- mu provides the only value of the background rate.
	// ix_grid is a two-element array that returns the MLE indexes into a_range and ams_range.

	public static void mle_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range,
		int[] ix_grid
	) {

		// Create the fitter

		OEDisc2ExtFit fitter = new OEDisc2ExtFit();

		boolean f_likelihood = true;
		boolean f_background = seed_params.has_background_rate();
		fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

		// Allocate the data structures and obtain their handles

		try (
			OEDisc2ExtFit.MagExponentHandle mexp = fitter.make_MagExponentHandle();
			OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
			OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
			OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
		) {

			// Build the magnitude-exponent data structures

			mexp.mexp_build (cat_params.b, cat_params.alpha);

			// Build the Omori matrix data structures

			omat.omat_build (cat_params.p, cat_params.c);

			// Build the magnitude-Omori pair data structures

			pmom.pmom_build (mexp, omat);

			// Likelihood base values

			double base_ten_a_q = Math.pow(10.0, cat_params.a) * mexp.get_q_correction();

			double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

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

					// Compute the log-likelihood for grid

					if (f_background) {
						grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q, seed_params.mu);
					} else {
						grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
					}
				}
			}

			// Find the maximum value in the grid

			double max_like_grid = OEArraysCalc.find_array_max (grid, ix_grid);
		}

		// Discard the fitter

		fitter = null;

		return;
	}




	// Class to use fitting to for a, ams, p, and c; version 3.
	
	public static class MLE_fit_a_ams_p_c_v3 implements SimpleThreadTarget {

		// --- Input parameters ---

		// Catalog history.

		public OEDisc2History history;

		// Catalog parameters.
		// In cat_params:
		//  -- a, p, c, b, and alpha provide the central or only value of the corresponding parameter.
		//  -- mref, msup, mag_min_sim, and mag_max_sim provide the magnitude ranges used by the fitter
		//     (which are also the magnitude ranges used for Q-correction of a).
		//  -- tbegin and tend are are the time range used for scaling a to preserve the branch ratio
		//     (only the difference tend - tbegin is relevant).

		public OECatalogParamsStats cat_params;

		// Seed parameters
		// In seed_params:
		//  -- ams and mu provide the central or only value of the corresponding parameter.

		public  OESeedParamsStats seed_params;

		// Interval option.

		public boolean f_intervals;

		// Log-likelihood magnitude range option.

		public int lmr_opt;

		// Background option.

		public boolean f_background;

		// Range of a-values.
		// a-values are a_base + log10(a_range[aix]).

		public double[] a_range;

		// Range of ams-values.
		// ams-values are seed_params.ams + log10(ams_range[amsix]).

		public double[] ams_range;

		// Range of p-values.
		// p-values are cat_params.p + p_range[pix].

		public double[] p_range;

		// Range of c-values.
		// c-values are cat_params.c * c_range[cix].

		public double[] c_range;

		// Set the input parameters.

		public void setup (
			OEDisc2History history,
			OECatalogParamsStats cat_params,
			OESeedParamsStats seed_params,
			boolean f_intervals,
			int lmr_opt,
			double[] a_range,
			double[] ams_range,
			double[] p_range,
			double[] c_range
		) {
			this.history     = history;
			this.cat_params  = cat_params;
			this.seed_params = seed_params;
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

		// Log-likelihood values.
		// The indexing is like_grid[cix][pix][aix][amsix].

		public double [][][][] like_grid;

		// Calculate the Omori scale factor used for a_base.
		// This is I(p, c) as described above.

		private double calc_omori_scale (double p, double c) {
			return OERandomGenerator.omori_rate (p, c, 0.0, cat_params.tend - cat_params.tbegin);
		}

		// Calculate the average Omori scale factor used for a_base.
		// This is I(p, c)/(tend - tbegin) as described above.
		// The calculation is done in a way that works even if tend == tbegin.
		// Note that ratios of this value are the same as for calc_omori_scale.

		private double calc_average_omori_scale (double p, double c) {
			return OERandomGenerator.omori_average_rate (p, c, 0.0, cat_params.tend - cat_params.tbegin);
		}

		//--- Internal variables ---

		// Parameter fitter, shared by all threads.

		private OEDisc2ExtFit fitter;

		// Magnitude-exponent handle, shared by all threads.

		private OEDisc2ExtFit.MagExponentHandle mexp;

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
				OEDisc2ExtFit.OmoriMatrixHandle omat = fitter.make_OmoriMatrixHandle();
				OEDisc2ExtFit.PairMagOmoriHandle pmom = fitter.make_PairMagOmoriHandle();
				OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
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

					double a_scaled = cat_params.a + Math.log10(calc_average_omori_scale(cat_params.p, cat_params.c) / calc_average_omori_scale(p, c));
					a_base[cix][pix] = a_scaled;

					// Build the Omori matrix data structures

					omat.omat_build (p, c);

					// Build the magnitude-Omori pair data structures

					pmom.pmom_build (mexp, omat);

					// Likelihood base values

					double base_ten_a_q = Math.pow(10.0, a_scaled) * mexp.get_q_correction();

					double base_ten_ams_q = Math.pow(10.0, seed_params.ams);

					// Likelihood matrix

					double[][] grid = like_grid[cix][pix];

					// With background rate ...

					if (f_background) {

						double mu = seed_params.mu;

						// Loop over a-values

						for (int aix = 0; aix < a_range.length; ++aix) {

							double ten_a_q = base_ten_a_q * a_range[aix];

							// Build the a-value-productivity data structures

							double ten_aint_q = ten_a_q;

							avpr.avpr_build (pmom, ten_aint_q);

							// Loop over ams-values

							for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
								double ten_ams_q = base_ten_ams_q * ams_range[amsix];

								// Compute the log-likelihood for grid

								grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q, mu);
							}
						}

					// No background rate ...

					} else {

						// Loop over a-values

						for (int aix = 0; aix < a_range.length; ++aix) {

							double ten_a_q = base_ten_a_q * a_range[aix];

							// Build the a-value-productivity data structures

							double ten_aint_q = ten_a_q;

							avpr.avpr_build (pmom, ten_aint_q);

							// Loop over ams-values

							for (int amsix = 0; amsix < ams_range.length; ++amsix) {
				
								double ten_ams_q = base_ten_ams_q * ams_range[amsix];

								// Compute the log-likelihood for grid

								grid[aix][amsix] = avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
							}
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

			fitter = new OEDisc2ExtFit();

			boolean f_likelihood = true;
			f_background = seed_params.has_background_rate();
			fitter.dfit_build (history, cat_params.get_params_mags(), f_intervals, f_likelihood, lmr_opt, f_background);

			// Allocate the executor and global data structures

			try (
				AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
				OEDisc2ExtFit.MagExponentHandle my_mexp = fitter.make_MagExponentHandle();
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




	// Use the fitting code to generate and display a c/p/a/ams likelihood grid.
	// a-values have the form a_base(p,c) + log10(a_range[i]).  So the powers of ten are (10^a_base(p,c)) * a_range[i].
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// Note: a_base(p,c) == cat_params.a for p == cat_params.p and c == cat_params.c.  For other values of p and c, 
	//  a_base(p,c) is chosen to have the same branch ratio on the time interval cat_params.tend - cat_params.tbegin
	//  and magnitude range cat_params.msup - cat_params.mref.  So the branch ratio is cat_params.get_br() * a_range[i].
	// This uses the multi-threaded code.

	public static void fit_c_p_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range
	) {
	
		// Display memory status

		System.out.println();
		System.out.println ("Memory status, initial:");
		SimpleUtils.show_memory_status();

		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v3 multi_fitter = new MLE_fit_a_ams_p_c_v3();

		multi_fitter.setup (
			history,
			cat_params,
			seed_params,
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
		System.out.println(layout_c_p_a_ams_like_grid (multi_fitter.like_grid, a_range, ams_range, p_range, c_range));

		// Discard the fitter

		multi_fitter = null;

		return;
	}




	// Use the fitting code to find the maximum-likelihood values of a c/p/a/ams likelihood grid.
	// a-values have the form a_base(p,c) + log10(a_range[i]).  So the powers of ten are (10^a_base(p,c)) * a_range[i].
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// Note: a_base(p,c) == cat_params.a for p == cat_params.p and c == cat_params.c.  For other values of p and c, 
	//  a_base(p,c) is chosen to have the same branch ratio on the time interval cat_params.tend - cat_params.tbegin
	//  and magnitude range cat_params.msup - cat_params.mref.  So the branch ratio is cat_params.get_br() * a_range[i].
	// This uses the multi-threaded code.
	// ix_grid is a 4-element array that returns the MLE indexes into c_range, p_range, a_range, and ams_range.

	public static void mle_c_p_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range,
		int[] ix_grid
	) {
	
		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v3 multi_fitter = new MLE_fit_a_ams_p_c_v3();

		multi_fitter.setup (
			history,
			cat_params,
			seed_params,
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

		// Find the maximum value in the grid

		double max_like = OEArraysCalc.find_array_max (multi_fitter.like_grid, ix_grid);

		// Discard the fitter

		multi_fitter = null;

		return;
	}




	// Use the fitting code to find marginals for a c/p/a/ams likelihood grid.
	// a-values have the form a_base(p,c) + log10(a_range[i]).  So the powers of ten are (10^a_base(p,c)) * a_range[i].
	// ams-values have the form seed_params.ams + log10(ams_range[i]).  So the powers of ten are (10^seed_params.ams) * ams_range[i].
	// p-values have the form cat_params.p + p_range[i].
	// c-values have the form cat_params.c * c_range[i].
	// Note: a_base(p,c) == cat_params.a for p == cat_params.p and c == cat_params.c.  For other values of p and c, 
	//  a_base(p,c) is chosen to have the same branch ratio on the time interval cat_params.tend - cat_params.tbegin
	//  and magnitude range cat_params.msup - cat_params.mref.  So the branch ratio is cat_params.get_br() * a_range[i].
	// This uses the multi-threaded code.
	// marg_grid receives a 4-dimensional marginal for indexes into c_range, p_range, a_range, and ams_range.

	public static void marg_c_p_a_ams_like_grid (
		OEDisc2History history, OECatalogParamsStats cat_params, OESeedParamsStats seed_params, boolean f_intervals, int lmr_opt,
		double[] a_range, double[] ams_range, double[] p_range, double[] c_range,
		OEGridMarginal marg_grid
	) {
	
		// Create the multi-threaded fitter

		MLE_fit_a_ams_p_c_v3 multi_fitter = new MLE_fit_a_ams_p_c_v3();

		multi_fitter.setup (
			history,
			cat_params,
			seed_params,
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

		// Find the marginals for the grid

		boolean f_has_prob = false;
		boolean f_convert = false;

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
			System.err.println ("OEFit2Test : Missing subcommand");
			return;
		}

		TestArgs testargs = new TestArgs (args, "OEFit2Test");




		// Subcommand : Test #1
		// Command format:
		//  test1  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Display catalog summary.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 12 or more additional arguments

			if (!( args.length >= 13 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test1' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog with given parameters and seeds");
				System.out.println ("zams = " + zams);
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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

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
		//  test2  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Display catalog summary and history contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 24 or more additional arguments

			if (!( args.length >= 25 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test2' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history with given parameters and seeds");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				System.out.println ();
				System.out.println ("Catalog size = " + rup_list.size());

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

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
				System.err.println ("OEFit2Test : Invalid 'test3' subcommand");
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
		//  test4  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to run a smoke test on the fitting code.
		// Display catalog summary, history contents, and smoke test results.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test4' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and running fit smoke test");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Run the smoke test

				fit_smoke_test (history, cat_params.get_params_stats(), seed_params.get_params_stats(), f_intervals, lmr_opt);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test5' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				fit_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5b
		// Command format:
		//  test5b  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #5 except it fits each catalog two ways, once with interval joining and once without
		// (ignoring the mag_cat_int_join parameter), and displays both.

		if (args[0].equalsIgnoreCase ("test5b")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test5b' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params_0 = new OEDiscFGHParams();

				hist_params_0.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					0	// mag_cat_int_join
				);

				OEDiscFGHParams hist_params_1 = new OEDiscFGHParams();

				hist_params_1.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					1	// mag_cat_int_join
				);

				// Make a history

				OEDisc2History history_0 = new OEDisc2History();

				history_0.build_from_fgh (hist_params_0, rup_list);

				OEDisc2History history_1 = new OEDisc2History();

				history_1.build_from_fgh (hist_params_1, rup_list);

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats_0 = cat_params.get_params_stats();
				cat_params_stats_0.set_fixed_mag_min (history_0.magCat);

				OECatalogParamsStats cat_params_stats_1 = cat_params.get_params_stats();
				cat_params_stats_1.set_fixed_mag_min (history_1.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				System.out.println();
				System.out.println ("Results for mag_cat_int_join = " + hist_params_0.mag_cat_int_join + ", ruptures = " + history_0.rupture_count + ", intervals = " + history_0.interval_count);
				System.out.println();

				fit_a_ams_like_grid (history_0, cat_params_stats_0, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range);

				System.out.println();
				System.out.println ("Results for mag_cat_int_join = " + hist_params_1.mag_cat_int_join + ", ruptures = " + history_1.rupture_count + ", intervals = " + history_1.interval_count);
				System.out.println();

				fit_a_ams_like_grid (history_1, cat_params_stats_1, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         num_cat
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for an a-value vector and an a-ams grid with the fitting code.
		// Then for each catalog, write a line containing 11 values:
		//  catalog number
		//  a - atrue
		//  ams - amstrue
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count

		if (args[0].equalsIgnoreCase ("test7")) {

			// 27 or more additional arguments

			if (!( args.length >= 28 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test7' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				int num_cat = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates for a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Array to receive maximum likelihood estimates

				int[] ix_grid = new int[2];

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDisc2History history = new OEDisc2History();

					history.build_from_fgh (hist_params, rup_list);

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
					cat_params_stats.set_fixed_mag_min (history.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the maximum likelihood estimate

					mle_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, ix_grid);

					// Display line

					System.out.println (String.format (
						"%6d  % .4f  % .4f  %6d %6d  %6d %6d %6d",
						i_cat,
						Math.log10 (a_range[ix_grid[0]]),
						Math.log10 (ams_range[ix_grid[1]]),
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




		// Subcommand : Test #7b
		// Command format:
		//  test7b  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         num_cat
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for an a-value vector and an a-ams grid with the fitting code.
		// Then for each catalog, write a line containing 11 values:
		//  catalog number
		//  a - atrue
		//  ams - amstrue
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Same as test #7 except it fits each catalog two ways, once with interval joining and once without
		// (ignoring the mag_cat_int_join parameter), and displays both on a single line.

		if (args[0].equalsIgnoreCase ("test7b")) {

			// 27 or more additional arguments

			if (!( args.length >= 28 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test7b' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				int num_cat = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates for a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params_0 = new OEDiscFGHParams();

				hist_params_0.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					0	// mag_cat_int_join
				);

				OEDiscFGHParams hist_params_1 = new OEDiscFGHParams();

				hist_params_1.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					1	// mag_cat_int_join
				);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Array to receive maximum likelihood estimates

				int[] ix_grid_0 = new int[2];
				int[] ix_grid_1 = new int[2];

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDisc2History history_0 = new OEDisc2History();

					history_0.build_from_fgh (hist_params_0, rup_list);

					OEDisc2History history_1 = new OEDisc2History();

					history_1.build_from_fgh (hist_params_1, rup_list);

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats_0 = cat_params.get_params_stats();
					cat_params_stats_0.set_fixed_mag_min (history_0.magCat);

					OECatalogParamsStats cat_params_stats_1 = cat_params.get_params_stats();
					cat_params_stats_1.set_fixed_mag_min (history_1.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the maximum likelihood estimate

					mle_a_ams_like_grid (history_0, cat_params_stats_0, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, ix_grid_0);

					mle_a_ams_like_grid (history_1, cat_params_stats_1, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, ix_grid_1);

					// Display line

					String same_diff = ((ix_grid_0[0] == ix_grid_1[0] && ix_grid_0[1] == ix_grid_1[1]) ? "same" : "diff");

					System.out.println (String.format (
						"%6d  % .4f  % .4f  %6d %6d  %6d %6d %6d     % .4f  % .4f  %6d %6d  %6d %6d %6d     %s",
						i_cat,
						Math.log10 (a_range[ix_grid_0[0]]),
						Math.log10 (ams_range[ix_grid_0[1]]),
						ix_grid_0[0],
						ix_grid_0[1],
						rup_list.size(),
						history_0.rupture_count,
						history_0.interval_count,
						Math.log10 (a_range[ix_grid_1[0]]),
						Math.log10 (ams_range[ix_grid_1[1]]),
						ix_grid_1[0],
						ix_grid_1[1],
						rup_list.size(),
						history_1.rupture_count,
						history_1.interval_count,
						same_diff
					));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5, except using the multi-threading code
		// (with default number of threads).

		if (args[0].equalsIgnoreCase ("test8")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test8' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid, multi-threaded");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (1, 0.0, 0.0)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         f_intervals  lmr_opt
		//         t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5, except using the multi-threading code
		// (with default number of threads).
		// Note: Same as text #8, except p is allowed to vary.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test9' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating p/a/ams grid, multi-threaded");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5, except using the multi-threading code
		// (with default number of threads).
		// Note: Same as text #8, except p and c are allowed to vary.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 26 or more additional arguments

			if (!( args.length >= 27 && args.length % 2 == 1 )) {
				System.err.println ("OEFit2Test : Invalid 'test10' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating c/p/a/ams grid, multi-threaded");
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #11
		// Command format:
		//  test11  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          num_cat
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Note that the parameters are the same as for test #7.
		// Note that this function uses the multi-threaded code.
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a/ams grid with the fitting code.
		// Then for each catalog, write a line containing 15 values:
		//  catalog number
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - amstrue
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count

		if (args[0].equalsIgnoreCase ("test11")) {

			// 27 or more additional arguments

			if (!( args.length >= 28 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test11' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				int num_cat = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates for c/p/a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Arrays to receive maximum likelihood estimates

				int[] ix_grid = new int[4];

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDisc2History history = new OEDisc2History();

					history.build_from_fgh (hist_params, rup_list);

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
					cat_params_stats.set_fixed_mag_min (history.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the maximum likelihood estimate

					mle_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, ix_grid);

					// Display line

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
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
		//  test12  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          num_cat
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Note that the parameters are the same as for test #7.
		// Note that this function uses the multi-threaded code.
		// Generate the requested number of catalogs with the given parameters.
		// Each catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate maximum likelihood estimates for a c/p/a/ams grid with the fitting code.
		// Then for each catalog, write a series of lines.
		// Line 1 contains 12 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - amstrue
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 6 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		// Line 7 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In line 7, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test12")) {

			// 27 or more additional arguments

			if (!( args.length >= 28 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test12' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				int num_cat = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
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

				OEGridMarginal marg_grid = new OEGridMarginal();

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDisc2History history = new OEDisc2History();

					history.build_from_fgh (hist_params, rup_list);

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
					cat_params_stats.set_fixed_mag_min (history.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the marginals

					marg_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_grid);

					// Display line, derived from global MLE

					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
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

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

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
		//  test13  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
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
		// Line 1 contains 12 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - amstrue
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 26 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  four lines with the corresponding marginal peak indexes in order c/p/a/ams.
		// Line 27 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In line 27, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test13")) {

			// 27 or more additional arguments

			if (!( args.length >= 28 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test13' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				int num_cat = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Generating catalogs, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, false);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
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

				OEGridMarginal marg_grid = new OEGridMarginal();

				// Loop over catalogs ...

				for (int i_cat = 0; i_cat < num_cat; ++i_cat) {

					// Make the catalog examiner

					ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
					OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, false);

					// Generate a catalog

					OESimulator.gen_single_catalog (initializer, examiner);

					// Make a history

					OEDisc2History history = new OEDisc2History();

					history.build_from_fgh (hist_params, rup_list);

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
					cat_params_stats.set_fixed_mag_min (history.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the marginals

					marg_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_grid);

					// Display line, derived from global MLE

					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display grid marginal probabilities

					String prefix = String.format ("%6d  ", i_cat);
					String infix = "\n" + prefix;
					String suffix = "";

					int max_els = 19;

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (3, max_els, prefix, infix, suffix));

					// Means and covariances

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

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
				System.err.println ("OEFit2Test : Invalid 'test14' subcommand");
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
				System.err.println ("OEFit2Test : Invalid 'test15' subcommand");
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
		//  test16  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
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
		// Line 1 contains 12 values, based on the global maximum likelihood estimate:
		//  catalog number
		//  log(c) - log(ctrue)
		//  p - ptrue
		//  a - atrue
		//  ams - amstrue
		//  c index (grid)
		//  p index (grid)
		//  a index (grid)
		//  ams index (grid)
		//  catalog rupture count
		//  history rupture count
		//  history interval count
		// Line 2 is the same, except based on the mode of each marginal distribution.
		// Lines 3 - 26 contain a segment of the marginal probability distributions for c/p/a/ams respectively.
		//  For each variable, this consists of one line of marginal probabilities,
		//  then one line with the corresponding marginal peak probabilities, then
		//  four lines with the corresponding marginal peak indexes in order c/p/a/ams.
		// Line 27 contains mean log(c), p, a, ams; then std dev; then cov log(c)/p, log(c)/a, log(c)/ams, p/a, p/ams, a/ams.
		// (In line 27, the values are the deviations from true values.)

		if (args[0].equalsIgnoreCase ("test16")) {

			// 25 additional arguments

			if (!( args.length == 26 )) {
				System.err.println ("OEFit2Test : Invalid 'test16' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				String event_id = args[ii++];

				// Echo the command line

				StringBuilder argecho = new StringBuilder();
				argecho.append ("# ");
				argecho.append ("oetas.fit.OEFit2Test");
				for (int iarg = 0; iarg < args.length; ++iarg) {
					argecho.append (" ");
					argecho.append (args[iarg]);
				}
				System.out.println (argecho.toString());
				System.out.println ("#");

				// Say hello

				System.out.println ("# Retrieving aftershock catalog, and generating list of maximum likelihood estimates and marginals for c/p/a/ams");
				System.out.println ("# zams = " + zams);
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
				System.out.println ("# t_interval_begin = " + t_interval_begin);
				System.out.println ("# before_max_count = " + before_max_count);
				System.out.println ("# mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams (zams, cat_params);

				if (f_verbose) {
					System.out.println (seed_params.toString());
				}

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					tbegin,				// t_range_begin
					tend,				// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
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

					OEDisc2History history = new OEDisc2History();

					history.build_from_fgh (hist_params, rup_list);
					
					//System.out.println (history.dump_string());
					//if (fcmain.mainshock_avail) {	// avoids unreachable code error
					//	return;
					//}

					if (f_verbose) {
						System.out.println (history.toString());
						System.out.println ();
					}

					// Adjust the minimum simulation magnitude to be the history's magCat

					OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
					cat_params_stats.set_fixed_mag_min (history.magCat);

					// Statistics from seed parameters

					OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

					// Get the marginals

					marg_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
						a_range, ams_range, p_range, c_range, marg_grid);

					// Display line, derived from global MLE

					int[] ix_grid = marg_grid.peak_indexes;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display line, derived from marginal mode

					ix_grid = marg_grid.marginal_mode_index;

					System.out.println (String.format (
						"%6d  % .4f  % .4f  % .4f  % .4f  %6d %6d %6d %6d %6d %6d %6d",
						i_cat,
						Math.log10 (c_range[ix_grid[0]]),
						            p_range[ix_grid[1]],
						Math.log10 (a_range[ix_grid[2]]),
						Math.log10 (ams_range[ix_grid[3]]),
						ix_grid[0],
						ix_grid[1],
						ix_grid[2],
						ix_grid[3],
						rup_list.size(),
						history.rupture_count,
						history.interval_count
					));

					// Display grid marginal probabilities

					String prefix = String.format ("%6d  ", i_cat);
					String infix = "\n" + prefix;
					String suffix = "";

					int max_els = 19;

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (0, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (1, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (2, max_els, prefix, infix, suffix));

					System.out.println (marg_grid.marginal_probability_and_peak_as_string (3, max_els, prefix, infix, suffix));

					// Means and covariances

					double[] mean_grid = new double[4];
					double[][] cov_grid = new double[4][4];

					marg_grid.calc_mean_covariance (range_cpa, mean_grid, cov_grid);

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




		// Subcommand : Test #17
		// Command format:
		//  test17  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Display catalog summary.
		// Same as test #1, except with a background rate, and seed ruptures are optional.

		if (args[0].equalsIgnoreCase ("test17")) {

			// 11 or more additional arguments

			if (!( args.length >= 12 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test17' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog with given parameters and seeds");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				OEExaminerPrintSummary examiner = new OEExaminerPrintSummary();

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #18
		// Command format:
		//  test18  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #5, except with a background rate, and seed ruptures are optional.

		if (args[0].equalsIgnoreCase ("test18")) {

			// 25 or more additional arguments

			if (!( args.length >= 26 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test18' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				fit_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #19
		// Command format:
		//  test19  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-mu grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #10, except it fits a-mu.

		if (args[0].equalsIgnoreCase ("test19")) {

			// 25 or more additional arguments

			if (!( args.length >= 26 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test19' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] mu_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				fit_a_mu_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, mu_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #20
		// Command format:
		//  test20  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5/#18, except using the multi-threading code
		// (with default number of threads).
		// Same as test #8, except with a background rate, and seed ruptures are optional.

		if (args[0].equalsIgnoreCase ("test20")) {

			// 25 or more additional arguments

			if (!( args.length >= 26 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test20' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating a/ams grid, multi-threaded");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (1, 0.0, 0.0)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #21
		// Command format:
		//  test21  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5/#18, except using the multi-threading code
		// (with default number of threads).
		// Note: Same as text #21, except p is allowed to vary.
		// Same as test #9, except with a background rate, and seed ruptures are optional.

		if (args[0].equalsIgnoreCase ("test21")) {

			// 25 or more additional arguments

			if (!( args.length >= 26 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test21' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating p/a/ams grid, multi-threaded");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #22
		// Command format:
		//  test22  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          t_day  rup_mag  [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-value vector and an a-ams grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Note: This is essentially the same as test #5/#18, except using the multi-threading code
		// (with default number of threads).
		// Note: Same as text #20, except p and c are allowed to vary.
		// Same as test #10, except with a background rate, and seed ruptures are optional.

		if (args[0].equalsIgnoreCase ("test22")) {

			// 25 or more additional arguments

			if (!( args.length >= 26 && args.length % 2 == 0 )) {
				System.err.println ("OEFit2Test : Invalid 'test22' subcommand");
				return;
			}

			try {

				int ii = 1;
				double zmu = Double.parseDouble (args[ii++]);
				double zams = Double.parseDouble (args[ii++]);
				double n = Double.parseDouble (args[ii++]);
				double p = Double.parseDouble (args[ii++]);
				double c = Double.parseDouble (args[ii++]);
				double b = Double.parseDouble (args[ii++]);
				double alpha = Double.parseDouble (args[ii++]);
				double mref = Double.parseDouble (args[ii++]);
				double msup = Double.parseDouble (args[ii++]);
				double tbegin = Double.parseDouble (args[ii++]);
				double tend = Double.parseDouble (args[ii++]);

				double magCat = Double.parseDouble (args[ii++]);
				int helm_param = Integer.parseInt (args[ii++]);
				double disc_delta = Double.parseDouble (args[ii++]);
				int mag_cat_count = Integer.parseInt (args[ii++]);
				double eligible_mag = Double.parseDouble (args[ii++]);
				int eligible_count = Integer.parseInt (args[ii++]);

				double durlim_ratio = Double.parseDouble (args[ii++]);
				double durlim_min = Double.parseDouble (args[ii++]);
				double durlim_max = Double.parseDouble (args[ii++]);
				double t_interval_begin = Double.parseDouble (args[ii++]);
				int before_max_count = Integer.parseInt (args[ii++]);
				int mag_cat_int_join = Integer.parseInt (args[ii++]);

				boolean f_intervals = Boolean.parseBoolean (args[ii++]);
				int lmr_opt = Integer.parseInt (args[ii++]);

				double[] time_mag_array = new double[args.length - ii];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + ii]);
				}

				// Say hello

				System.out.println ("Generating catalog and history, and generating c/p/a/ams grid, multi-threaded");
				System.out.println ("zmu = " + zmu);
				System.out.println ("zams = " + zams);
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
				System.out.println ("t_interval_begin = " + t_interval_begin);
				System.out.println ("before_max_count = " + before_max_count);
				System.out.println ("mag_cat_int_join = " + mag_cat_int_join);

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				// Make and display the vector and grid

				fit_c_p_a_ams_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, a_range, ams_range, p_range, c_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #23
		// Command format:
		//  test23  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then construct a grouping from the history.
		// Display catalog summary, history contents, and grouping contents.

		if (testargs.is_test ("test23")) {
			try {

				System.out.println ("Generating catalog and history, and generating grouping");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Make the grouping

				double int_mc_thresh = OEDisc2Grouping.calc_int_mc_thresh (history.magCat, history.magCat, f_intervals, lmr_opt);
				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				OEDisc2Grouping grouping = new OEDisc2Grouping();
				grouping.build_grouping_from_history (
					history,
					int_mc_thresh,
					span_width_fcn,
					rup_width_fcn
				);

				// Display the grouping

				System.out.println ();
				System.out.println (grouping.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #24
		// Command format:
		//  test24  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then construct a grouping from the history.
		// Display catalog summary, history contents, and grouping contents.
		// Same as test #23 except dumps the entire contents of the grouping (could be large).

		if (testargs.is_test ("test24")) {
			try {

				System.out.println ("Generating catalog and history, and generating grouping, with full dump");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Make the grouping

				double int_mc_thresh = OEDisc2Grouping.calc_int_mc_thresh (history.magCat, history.magCat, f_intervals, lmr_opt);
				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				OEDisc2Grouping grouping = new OEDisc2Grouping();
				grouping.build_grouping_from_history (
					history,
					int_mc_thresh,
					span_width_fcn,
					rup_width_fcn
				);

				// Display the grouping

				System.out.println ();
				System.out.println (grouping.dump_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #25
		// Command format:
		//  test25  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-mu grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24.  (Same as test #19 plus extra parameters for grouping.)
		// Like test #19, except makes an ams-mu grid, and displays grouped productivities
		// for the central (or base) values of a, ams, mu.

		if (testargs.is_test ("test25")) {
			try {

				System.out.println ("Generating catalog and history, generating grouping, and doing ams/mu fitting, with productivity output");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] mu_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				// Make and display the grid

				fit_ams_mu_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, ams_range, mu_range, span_width_fcn, rup_width_fcn);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #26
		// Command format:
		//  test26  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate an a-mu grid with the fitting code.
		// Display catalog summary, history contents, and grid contents.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24.  (Same as test #19 plus extra parameters for grouping.)
		// Like test #19, except makes an ams-mu grid, and displays grouped productivities
		// for the central (or base) values of a, ams, mu.
		// Same as test #25, except using the statistics voxel.

		if (testargs.is_test ("test26")) {
			try {

				System.out.println ("Generating catalog and history, generating grouping, and doing ams/mu fitting, with productivity output, using the statistics voxel");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] mu_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				OEDiscreteRange zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange zmu_range = OEDiscreteRange.makeLog (11, zmu * Math.sqrt(0.1), zmu * Math.sqrt(10.0));

				// Make and display the grid

				fit_vox_ams_mu_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, ams_range, mu_range, span_width_fcn, rup_width_fcn, n, zams_range, zmu_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #27
		// Command format:
		//  test27  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and an a-mu grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24.  (Same as test #19 plus extra parameters for grouping.)
		// Like test #19, except makes an ams-mu grid, and displays grouped productivities
		// for the central (or base) values of a, ams, mu.
		// Same as test #25 and #26, except using the statistics voxel set.

		if (testargs.is_test ("test27")) {
			try {

				System.out.println ("Generating catalog and history, generating grouping, and doing ams/mu fitting, with productivity output, using the statistics voxel set");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] mu_range = (OEDiscreteRange.makeLog (11, Math.sqrt(0.1), Math.sqrt(10.0))).get_range_array();

				OEDiscreteRange zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange zmu_range = OEDiscreteRange.makeLog (11, zmu * Math.sqrt(0.1), zmu * Math.sqrt(10.0));

				// Make and display the grid

				fit_vox_set_ams_mu_like_grid (history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt, ams_range, mu_range, span_width_fcn, rup_width_fcn, n, zams_range, zmu_range);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #28
		// Command format:
		//  test28  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24, #25, #26, and #27.
		// Same as test #29 except that c is fixed.

		if (testargs.is_test ("test28")) {
			try {

				System.out.println ("Generating catalog and history, generating grouping, and doing p/a/ams fitting, with productivity output, using the statistics voxel set");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();
				//double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange v_p_range = OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3);
				OEDiscreteRange v_c_range = OEDiscreteRange.makeSingle (cat_params.c);
				//OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, 0.0
				);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #29
		// Command format:
		//  test29  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24, #25, #26, and #27.
		// Same as test #28 except it allows c to vary.

		if (testargs.is_test ("test29")) {
			try {

				System.out.println ("Generating catalog and history, generating grouping, and doing c/p/a/ams fitting, with productivity output, using the statistics voxel set");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				//double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange v_p_range = OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3);
				//OEDiscreteRange v_c_range = OEDiscreteRange.makeSingle (cat_params.c);
				OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, 0.0
				);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #30
		// Command format:
		//  test30  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same parameters as text #24, #25, #26, and #27.
		// Same as test #28 except also does forecast.
		// Same as test #31 except that c is fixed.

		if (testargs.is_test ("test30")) {
			try {

				System.out.println ("Generating catalog and history and forecast, generating grouping, and doing p/a/ams fitting, with productivity output, using the statistics voxel set");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				double br_max_mult = 1.0 / (Math.min (n, 1.0));

				//double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, br_max_mult)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (OEDiscreteRange.makeLog (1, 1.0, 1.0)).get_range_array();
				//double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				//OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * br_max_mult);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange v_p_range = OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3);
				OEDiscreteRange v_c_range = OEDiscreteRange.makeSingle (cat_params.c);
				//OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				OEDisc2InitVoxSet vox_set = fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, 0.0
				);

				// Perform ranging and simulation

				final String sepline = "----------------------------------------------------------------------";
				System.out.println();
				System.out.println (sepline);
				System.out.println (sepline);

				boolean f_prod = true;
				int num_cats = 0;
				int target_size = 0;
				long max_runtime = -1L;
				long progress_time = 10000L;

				OESimulator.test_run_simulation_ex (vox_set, f_prod, num_cats, target_size, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #32
		// Command format:
		//  test32  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          f_fix_p  f_fix_c  max_br  tint_br
		//          num_cats  min_rel_mag  max_rel_mag  exceed_fraction  target_size  target_fractile  mag_lim_fraction  mag_lim_time
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #30 and #31 except with simulation options.

		if (testargs.is_test ("test32")) {
			try {

				System.out.println ("Generating catalog and history and forecast, generating grouping, and doing c/p/a/ams fitting, with productivity output, using the statistics voxel set, plus simulation and forecast");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				boolean f_fix_p = testargs.get_boolean ("f_fix_p");
				boolean f_fix_c = testargs.get_boolean ("f_fix_c");
				double max_br = testargs.get_double ("max_br");
				double tint_br = testargs.get_double ("tint_br");

				int num_cats = testargs.get_int ("num_cats");
				double min_rel_mag = testargs.get_double ("min_rel_mag");
				double max_rel_mag = testargs.get_double ("max_rel_mag");
				double exceed_fraction = testargs.get_double ("exceed_fraction");
				int target_size = testargs.get_int ("target_size");
				double target_fractile = testargs.get_double ("target_fractile");
				double mag_lim_fraction = testargs.get_double ("mag_lim_fraction");
				double mag_lim_time = testargs.get_double ("mag_lim_time");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

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

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				//double br_max_mult = 1.0 / (Math.min (n, 1.0));
				double br_max_mult = max_br / (Math.min (n, 1.0));

				//double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, br_max_mult)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();
				//double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();

				//OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * br_max_mult);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3));
				OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0));
				//OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				OEDisc2InitVoxSet vox_set = fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, tint_br
				);

				// Perform ranging and simulation

				final String sepline = "----------------------------------------------------------------------";
				System.out.println();
				System.out.println (sepline);
				System.out.println (sepline);

				// Create the simulation parameters

				boolean f_prod = true;
				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				test_sim_parameters.range_min_rel_mag = min_rel_mag;
				test_sim_parameters.range_max_rel_mag = max_rel_mag;
				test_sim_parameters.range_exceed_fraction = exceed_fraction;
				test_sim_parameters.range_target_fractile = target_fractile;
				test_sim_parameters.range_mag_lim_fraction = mag_lim_fraction;
				test_sim_parameters.range_mag_lim_time = mag_lim_time;

				long max_runtime = -1L;
				long progress_time = 10000L;

				OESimulator.test_run_simulation_ex (vox_set, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #33
		// Command format:
		//  test33  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          f_fix_p  f_fix_c  max_br  tint_br
		//          num_cats  min_rel_mag  max_rel_mag  exceed_fraction  target_size  target_fractile  mag_lim_fraction  mag_lim_time
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #32 except with consistent handling of branch ratio time interval, and larger parameter ranges.

		if (testargs.is_test ("test33")) {
			try {

				System.out.println ("Generating catalog and history and forecast, generating grouping, and doing c/p/a/ams fitting, with productivity output, using the statistics voxel set, plus simulation and forecast");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				boolean f_fix_p = testargs.get_boolean ("f_fix_p");
				boolean f_fix_c = testargs.get_boolean ("f_fix_c");
				double max_br = testargs.get_double ("max_br");
				double tint_br = testargs.get_double ("tint_br");

				int num_cats = testargs.get_int ("num_cats");
				double min_rel_mag = testargs.get_double ("min_rel_mag");
				double max_rel_mag = testargs.get_double ("max_rel_mag");
				double exceed_fraction = testargs.get_double ("exceed_fraction");
				int target_size = testargs.get_int ("target_size");
				double target_fractile = testargs.get_double ("target_fractile");
				double mag_lim_fraction = testargs.get_double ("mag_lim_fraction");
				double mag_lim_time = testargs.get_double ("mag_lim_time");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend,	// tend
					tint_br	// tint_br
				);

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				int num_br = 41;
				double min_br = 0.01;
				//double max_br = 1.00;

				int num_zams = 81;
				double min_zams = -4.50;
				double max_zams = -0.50;

				int num_p = 37;
				double min_p = 0.50;
				double max_p = 2.00;

				int num_c = 21;
				double min_c = 0.00001;
				double max_c = 1.00000;

				double[] a_range = (OEDiscreteRange.makeLog (num_br, min_br / n, max_br / n)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (num_zams, Math.pow (10.0, min_zams - zams), Math.pow (10.0, max_zams - zams))).get_range_array();
				double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (num_p, min_p - cat_params.p, max_p - cat_params.p)).get_range_array();
				double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (num_c, min_c / cat_params.c, max_c / cat_params.c)).get_range_array();

				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (num_br, min_br, max_br);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (num_zams, min_zams, max_zams);
				OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (num_p, min_p, max_p));
				OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (num_c, min_c, max_c));

				//////double br_max_mult = 1.0 / (Math.min (n, 1.0));
				////double br_max_mult = max_br / (Math.min (n, 1.0));
				////
				//////double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				////double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, br_max_mult)).get_range_array();
				////double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				////double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				////double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();
				//////double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();
				////
				//////OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				////OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * br_max_mult);
				////OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				////OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3));
				////OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0));
				//////OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				OEDisc2InitVoxSet vox_set = fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, tint_br
				);

				// Perform ranging and simulation

				final String sepline = "----------------------------------------------------------------------";
				System.out.println();
				System.out.println (sepline);
				System.out.println (sepline);

				// Create the simulation parameters

				boolean f_prod = true;
				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				test_sim_parameters.range_min_rel_mag = min_rel_mag;
				test_sim_parameters.range_max_rel_mag = max_rel_mag;
				test_sim_parameters.range_exceed_fraction = exceed_fraction;
				test_sim_parameters.range_target_fractile = target_fractile;
				test_sim_parameters.range_mag_lim_fraction = mag_lim_fraction;
				test_sim_parameters.range_mag_lim_time = mag_lim_time;

				long max_runtime = -1L;
				long progress_time = 10000L;

				OESimulator.test_run_simulation_ex (vox_set, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #34
		// Command format:
		//  test34  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          f_fix_p  f_fix_c  max_br  tint_br
		//          num_cats  prob_nonempty  direct_size  exceed_fraction  accum_option  accum_param_1  mag_excess
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #33 except uses ranging version #2.

		if (testargs.is_test ("test34")) {
			try {

				System.out.println ("Generating catalog and history and forecast, generating grouping, and doing c/p/a/ams fitting, with productivity output, using the statistics voxel set, plus simulation and forecast");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				boolean f_fix_p = testargs.get_boolean ("f_fix_p");
				boolean f_fix_c = testargs.get_boolean ("f_fix_c");
				double max_br = testargs.get_double ("max_br");
				double tint_br = testargs.get_double ("tint_br");

				int num_cats = testargs.get_int ("num_cats");
				double prob_nonempty = testargs.get_double ("prob_nonempty");
				int direct_size = testargs.get_int ("direct_size");
				double exceed_fraction = testargs.get_double ("exceed_fraction");
				int accum_option = testargs.get_int ("accum_option");
				double accum_param_1 = testargs.get_double ("accum_param_1");
				double mag_excess = testargs.get_double ("mag_excess");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend,	// tend
					tint_br	// tint_br
				);

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				int num_br = 41;
				double min_br = 0.01;
				//double max_br = 1.00;

				int num_zams = 81;
				double min_zams = -4.50;
				double max_zams = -0.50;

				int num_p = 37;
				double min_p = 0.50;
				double max_p = 2.00;

				int num_c = 21;
				double min_c = 0.00001;
				double max_c = 1.00000;

				double[] a_range = (OEDiscreteRange.makeLog (num_br, min_br / n, max_br / n)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (num_zams, Math.pow (10.0, min_zams - zams), Math.pow (10.0, max_zams - zams))).get_range_array();
				double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (num_p, min_p - cat_params.p, max_p - cat_params.p)).get_range_array();
				double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (num_c, min_c / cat_params.c, max_c / cat_params.c)).get_range_array();

				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (num_br, min_br, max_br);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (num_zams, min_zams, max_zams);
				OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (num_p, min_p, max_p));
				OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (num_c, min_c, max_c));

				//////double br_max_mult = 1.0 / (Math.min (n, 1.0));
				////double br_max_mult = max_br / (Math.min (n, 1.0));
				////
				//////double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				////double[] a_range = (OEDiscreteRange.makeLog (51, 0.1, br_max_mult)).get_range_array();
				////double[] ams_range = (OEDiscreteRange.makeLog (51, 0.1, 10.0)).get_range_array();
				////double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (31, -0.3, 0.3)).get_range_array();
				////double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();
				//////double[] c_range = (OEDiscreteRange.makeLog (17, 0.01, 100.0)).get_range_array();
				////
				//////OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * 10.0);
				////OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (51, n * 0.1, n * br_max_mult);
				////OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (51, zams + Math.log10(0.1), zams + Math.log10(10.0));
				////OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (31, cat_params.p - 0.3, cat_params.p + 0.3));
				////OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0));
				//////OEDiscreteRange v_c_range = OEDiscreteRange.makeLog (17, cat_params.c * 0.01, cat_params.c * 100.0);

				// Make and display the grid

				OEDisc2InitVoxSet vox_set = fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, tint_br
				);

				// Perform ranging and simulation

				final String sepline = "----------------------------------------------------------------------";
				System.out.println();
				System.out.println (sepline);
				System.out.println (sepline);

				// Create the simulation parameters

				boolean f_prod = true;
				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				test_sim_parameters.ranv2_prob_nonempty = prob_nonempty;
				test_sim_parameters.ranv2_direct_size = direct_size;
				test_sim_parameters.ranv2_exceed_fraction = exceed_fraction;

				test_sim_parameters.range_method = OEConstants.RANGING_METH_SEED_EST;

				if (accum_option >= 0) {
					test_sim_parameters.sim_accum_option = accum_option;
				}
				test_sim_parameters.sim_accum_param_1 = accum_param_1;
				test_sim_parameters.ranv2_mag_excess = mag_excess;

				long max_runtime = -1L;
				long progress_time = 10000L;

				OESimulator.test_run_simulation_ex (vox_set, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #35
		// Command format:
		//  test35  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//          magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//          durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//          f_intervals  lmr_opt
		//          gs_rel_base_time  gs_ratio  gs_min_width  gr_hi_mag_delta  gr_taper_mag_delta  gr_init_mag
		//          f_fix_p  f_fix_c  max_br  tint_br
		//          num_cats  direct_size  mag_excess  gen_br  derate_br  exceed_fraction  direct_size_hi  accum_option  accum_param_1
		//          [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then use the history to generate a voxel set and a c-p-a-ams grid with the fitting code.
		// Then use the voxel set to seed a catalog.
		// Display catalog summary, history contents, grid contents, and catalog seeds.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitter, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.
		// Same as test #33 and #34 except uses ranging version #3.

		if (testargs.is_test ("test35")) {
			try {

				System.out.println ("Generating catalog and history and forecast, generating grouping, and doing c/p/a/ams fitting, with productivity output, using the statistics voxel set, plus simulation and forecast");
				double zmu = testargs.get_double ("zmu");
				double zams = testargs.get_double ("zams");
				double n = testargs.get_double ("n");
				double p = testargs.get_double ("p");
				double c = testargs.get_double ("c");
				double b = testargs.get_double ("b");
				double alpha = testargs.get_double ("alpha");
				double mref = testargs.get_double ("mref");
				double msup = testargs.get_double ("msup");
				double tbegin = testargs.get_double ("tbegin");
				double tend = testargs.get_double ("tend");

				double magCat = testargs.get_double ("magCat");
				int helm_param = testargs.get_int ("helm_param");
				double disc_delta = testargs.get_double ("disc_delta");
				int mag_cat_count = testargs.get_int ("mag_cat_count");
				double eligible_mag = testargs.get_double ("eligible_mag");
				int eligible_count = testargs.get_int ("eligible_count");

				double durlim_ratio = testargs.get_double ("durlim_ratio");
				double durlim_min = testargs.get_double ("durlim_min");
				double durlim_max = testargs.get_double ("durlim_max");
				double t_interval_begin = testargs.get_double ("t_interval_begin");
				int before_max_count = testargs.get_int ("before_max_count");
				int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

				boolean f_intervals = testargs.get_boolean ("f_intervals");
				int lmr_opt = testargs.get_int ("lmr_opt");

				double gs_rel_base_time = testargs.get_double ("gs_rel_base_time");
				double gs_ratio = testargs.get_double ("gs_ratio");
				double gs_min_width = testargs.get_double ("gs_min_width");
				double gr_hi_mag_delta = testargs.get_double ("gr_hi_mag_delta");
				double gr_taper_mag_delta = testargs.get_double ("gr_taper_mag_delta");
				double gr_init_mag = testargs.get_double ("gr_init_mag");

				boolean f_fix_p = testargs.get_boolean ("f_fix_p");
				boolean f_fix_c = testargs.get_boolean ("f_fix_c");
				double max_br = testargs.get_double ("max_br");
				double tint_br = testargs.get_double ("tint_br");

				int num_cats = testargs.get_int ("num_cats");
				int direct_size = testargs.get_int ("direct_size");
				double mag_excess = testargs.get_double ("mag_excess");
				int gen_br = testargs.get_int ("gen_br");
				double derate_br = testargs.get_double ("derate_br");
				double exceed_fraction = testargs.get_double ("exceed_fraction");
				int direct_size_hi = testargs.get_int ("direct_size_hi");

				int accum_option = testargs.get_int ("accum_option");
				double accum_param_1 = testargs.get_double ("accum_param_1");

				double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
				testargs.end_test();

				// Make the catalog parameters

				OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
					n,		// n
					p,		// p
					c,		// c
					b,		// b
					alpha,	// alpha
					mref,	// mref
					msup,	// msup
					tbegin,	// tbegin
					tend,	// tend
					tint_br	// tint_br
				);

				// Make the seed parameters

				OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

				// Make the catalog initializer

				OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

				// Make the catalog examiner

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

				// Generate a catalog

				OESimulator.gen_single_catalog (initializer, examiner);

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the history parameters

				double t_range_begin = Math.min (tbegin, t_interval_begin);
				double t_range_end = tend;
				for (int itm = 0; itm < time_mag_array.length; itm += 2) {
					t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
				}

				OEDiscFGHParams hist_params = new OEDiscFGHParams();

				hist_params.set_sim_history_typical (
					magCat,				// magCat
					helm_param,			// helm_param
					t_range_begin,		// t_range_begin
					t_range_end,		// t_range_end
					disc_delta,			// disc_delta
					mag_cat_count,		// mag_cat_count
					eligible_mag,		// eligible_mag
					eligible_count,		// eligible_count
					split_fn,			// split_fn
					t_interval_begin,	// t_interval_begin
					before_max_count,	// before_max_count
					mag_cat_int_join	// mag_cat_int_join
				);

				// Display the history parameters

				System.out.println ();
				System.out.println (hist_params.toString());

				// Make a history

				OEDisc2History history = new OEDisc2History();

				history.build_from_fgh (hist_params, rup_list);

				// Display the history

				System.out.println ();
				System.out.println (history.toString());

				// Adjust the minimum simulation magnitude to be the history's magCat

				OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
				cat_params_stats.set_fixed_mag_min (history.magCat);

				// Statistics from seed parameters

				OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

				// Grouping parameters

				OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
				OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);

				// Parameter ranges

				int num_br = 41;
				double min_br = 0.01;
				//double max_br = 1.00;

				int num_zams = 81;
				double min_zams = -4.50;
				double max_zams = -0.50;

				int num_p = 37;
				double min_p = 0.50;
				double max_p = 2.00;

				int num_c = 21;
				double min_c = 0.00001;
				double max_c = 1.00000;

				double[] a_range = (OEDiscreteRange.makeLog (num_br, min_br / n, max_br / n)).get_range_array();
				double[] ams_range = (OEDiscreteRange.makeLog (num_zams, Math.pow (10.0, min_zams - zams), Math.pow (10.0, max_zams - zams))).get_range_array();
				double[] p_range = (f_fix_p ? OEDiscreteRange.makeSingle (0.0) : OEDiscreteRange.makeLinear (num_p, min_p - cat_params.p, max_p - cat_params.p)).get_range_array();
				double[] c_range = (f_fix_c ? OEDiscreteRange.makeSingle (1.0) : OEDiscreteRange.makeLog (num_c, min_c / cat_params.c, max_c / cat_params.c)).get_range_array();

				OEDiscreteRange v_n_range = OEDiscreteRange.makeLog (num_br, min_br, max_br);
				OEDiscreteRange v_zams_range = OEDiscreteRange.makeLinear (num_zams, min_zams, max_zams);
				OEDiscreteRange v_p_range = (f_fix_p ? OEDiscreteRange.makeSingle (cat_params.p) : OEDiscreteRange.makeLinear (num_p, min_p, max_p));
				OEDiscreteRange v_c_range = (f_fix_c ? OEDiscreteRange.makeSingle (cat_params.c) : OEDiscreteRange.makeLog (num_c, min_c, max_c));

				// Make and display the grid

				OEDisc2InitVoxSet vox_set = fit_vox_set_c_p_a_ams_like_grid (
					history, cat_params_stats, seed_params_stats, f_intervals, lmr_opt,
					a_range, ams_range,p_range, c_range, span_width_fcn, rup_width_fcn,
					v_c_range, v_p_range, v_n_range, v_zams_range, zmu, tint_br
				);

				// Perform ranging and simulation

				final String sepline = "----------------------------------------------------------------------";
				System.out.println();
				System.out.println (sepline);
				System.out.println (sepline);

				// Create the simulation parameters

				boolean f_prod = true;
				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				test_sim_parameters.ranv2_direct_size = direct_size;
				test_sim_parameters.ranv2_mag_excess = mag_excess;
				test_sim_parameters.ranv2_gen_br = gen_br;
				test_sim_parameters.ranv2_derate_br = derate_br;
				test_sim_parameters.ranv2_exceed_fraction = exceed_fraction;
				test_sim_parameters.ranv3_direct_size_hi = direct_size_hi;

				test_sim_parameters.range_method = OEConstants.RANGING_METH_VAR_SEED_EST;

				if (accum_option >= 0) {
					test_sim_parameters.sim_accum_option = accum_option;
				}
				test_sim_parameters.sim_accum_param_1 = accum_param_1;

				long max_runtime = -1L;
				long progress_time = 10000L;

				OESimulator.test_run_simulation_ex (vox_set, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEFit2Test : Unrecognized subcommand : " + args[0]);
		return;

	}

}
