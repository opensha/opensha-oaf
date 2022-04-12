package org.opensha.oaf.oetas;

import static org.opensha.oaf.oetas.OEConstants.TINY_OMORI_RATE;
import static org.opensha.oaf.oetas.OEConstants.SMALL_EXPECTED_COUNT;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Class for generating an Operational ETAS catalog.
// Author: Michael Barall 12/04/2019.
//
// After a catalog is seeded (that is, after the first generation is
// filled in), an object of this class is used to generate the succeeding
// generations.
//
// Only one thread at a time can use one of these objects.
//
// After a catalog has been generated, this object can be re-used
// to generate another catalog.

public class OECatalogGenerator {

	//----- Constants -----

	// Default size of workspace arrays.

	private static final int DEF_WORKSPACE_CAPACITY = 1000;




	//----- Workspace established at setup -----

	// Random number generator to use.

	private OERandomGenerator rangen;

	// Catalog builder to use.

	private OECatalogBuilder cat_builder;

	// True to select verbose mode.

	private boolean f_verbose;

	// Parameters for the catalog.

	private OECatalogParams cat_params;




	//----- Workspace used to create the next generation -----

	// Information about the current generation.

	private OEGenerationInfo cur_gen_info;

	// Information about the next generation.

	private OEGenerationInfo next_gen_info;

	// A rupture in the current generation.

	private OERupture cur_rup;

	// A rupture in the next generation.

	private OERupture next_rup;

	// Current workspace capacity.

	private int workspace_capacity;

	// Cumulative Omori rate per unit magnitude, for each rupture in the current generation.
	// The Omori rate is equal to:
	//
	//   k * Integral(max(tbegin, t0), tend, ((t-t0+c)^(-p))*dt)
	//
	//   k = Corrected productivity.
	//   p = Omori exponent.
	//   c = Omori offset.
	//   tbegin = Time when forecast interval begins.
	//   tend = Time when forecast interval ends.
	//   t0 = Time of rupture.
	//
	// With this definition, the expected number of direct aftershocks in
	// a magnitude range [m1, m2] during the forecast interval is:
	//
	//   omori_rate * Integral(m1, m2, b*log(10)*(10^(-b*(m - mref)))*dm)
	//
	//   b = Gutenberg-Richter parameter
	//   mref = Reference magnitude = minimum considered magnitude.
	//
	// This array contains cumulative Omori rate, meaning that the Omori rate
	// for rupture j is work_omori_rate[j] - work_omori_rate[j-1].

	private double[] work_omori_rate;

	// The child rupture count, for each rupture in the current generation.
	// The is the value of a Poisson random variable, with mean equal to:
	//
	//   omori_rate * Integral(m1, m2, (b*log(10)*10^(-b*(m - mref)))*dm)
	//
	//   b = Gutenberg-Richter exponent.
	//   m1 = Minumum magnitude for the next generation.
	//   m2 = Maximum magnitude for the next generation.

	private int[] work_child_count;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		rangen = null;
		cat_builder = null;
		f_verbose = false;
		cat_params = new OECatalogParams();

		cur_gen_info = new OEGenerationInfo();
		next_gen_info = new OEGenerationInfo();
		cur_rup = new OERupture();
		next_rup = new OERupture();
		workspace_capacity = DEF_WORKSPACE_CAPACITY;
		work_omori_rate = new double[workspace_capacity];
		work_child_count = new int[workspace_capacity];
		return;
	}




	// Default constructor.

	public OECatalogGenerator () {
		clear();
	}




	//----- Generation -----




	// Set up to generate the catalog.
	// Parameters:
	//  rangen = Random number generator to use.
	//  cat_builder = Catalog builder to use.
	//  f_verbose = True to select verbose mode.
	// This function must be called first when generating a catalog.

	public void setup (OERandomGenerator rangen, OECatalogBuilder cat_builder, boolean f_verbose) {

		// Save the random number generator and catalog builder

		this.rangen = rangen;
		this.cat_builder = cat_builder;

		// Save the verbose mode option

		this.f_verbose = f_verbose;

		// Get the catalog parameters

		this.cat_builder.get_cat_params (cat_params);
		return;
	}




	// Forget the random number generator and catalog builder.

	public void forget () {

		// Forget the random number generator and catalog builder

		this.rangen = null;
		this.cat_builder = null;
	
		return;
	}




	// Get the random number generator.

	public OERandomGenerator get_rangen () {
		return rangen;
	}




	// Get the catalog builder.

	public OECatalogBuilder get_cat_builder () {
		return cat_builder;
	}




	// Find the time up to which the catalog is already complete.
	// This is the earliest time in the last non-empty generation, except it
	// is the start time if the last non-empty generation is the seed generation.
	// However, it is not later than the stop time.
	// Note: This should not be called while a generation is open.

	private double find_time_completed () {

		// Latest time we can report is stop time

		double result = cat_builder.get_cat_stop_time();

		// Check generations from last to first

		int cur_i_gen = cat_builder.get_gen_count();

		for (;;) {
			--cur_i_gen;

			// Stop if at seed generation, and use the start time

			if (cur_i_gen <= 0) {
				result = Math.min (result, cat_params.tbegin);
				break;
			}

			// Get the size of the current generation

			int cur_gen_size = cat_builder.get_gen_size (cur_i_gen);

			// If it's non-empty ...

			if (cur_gen_size > 0) {

				// Scan the current generation ...

				for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

					// Get the rupture, time only, in the current generation

					cat_builder.get_rup_time (cur_i_gen, cur_j_rup, cur_rup);

					// Accumulate the time

					if (cur_rup.t_day < result) {
						result = cur_rup.t_day;
					}
				}

				// Stop

				break;
			}
		}

		return result;
	}




	// Calculate the next generation.
	// Returns the number of earthquakes in the new generation.
	// If the return value is zero, then no generation was added,
	// and the catalog has reached its end.
	// Note: Before calling this function for the first time (after calling
	// setup), you must create the first generation to seed the catalog.

	public int calc_next_gen () {

		// The next generation number is the current number of generations

		int next_i_gen = cat_builder.get_gen_count();

		// The current generation number is the last generation in the catalog

		int cur_i_gen = next_i_gen - 1;

		// Get information for the current generation

		cat_builder.get_gen_info (cur_i_gen, cur_gen_info);

		// Initialize information for the next generation

		next_gen_info.clear();

		// Get the size of the current generation

		int cur_gen_size = cat_builder.get_gen_size (cur_i_gen);

		// It shouldn't be zero, but if it is, don't create another one

		if (cur_gen_size == 0) {
			return 0;
		}

		// Ensure workspace arrays are large enough for the current generation

		if (cur_gen_size > workspace_capacity) {
			do {
				workspace_capacity = workspace_capacity * 2;
			} while (cur_gen_size > workspace_capacity);

			work_omori_rate = new double[workspace_capacity];
			work_child_count = new int[workspace_capacity];
		}

		// The effective end time is the stop time, but not after the configured end time

		double eff_tend = Math.min (cat_params.tend, cat_builder.get_cat_stop_time());

		// Stop if the effective end time is before the configured start time, within epsilon

		if (eff_tend <= cat_params.tbegin + cat_params.teps) {
			return 0;
		}

		//--- Determine the Omori rate for each rupture, and the size and minimum magnitude of the next generation

		// Scan the current generation ...

		double total_omori_rate = 0.0;

		for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

			// Get the rupture in the current generation, time and productivity only

			cat_builder.get_rup_time_prod (cur_i_gen, cur_j_rup, cur_rup);

			// Calculate its expected rate in the forecast interval
			// Note omori_rate_shifted returns zero if t0 > t2 - teps

			double omori_rate = cur_rup.k_prod * OERandomGenerator.omori_rate_shifted (
				cat_params.p,			// p
				cat_params.c,			// c
				cur_rup.t_day,			// t0
				cat_params.teps,		// teps
				cat_params.tbegin,		// t1
				eff_tend				// t2
				);

			// Accumulate the total

			total_omori_rate += omori_rate;
			work_omori_rate[cur_j_rup] = total_omori_rate;

			// Initialize child count

			work_child_count[cur_j_rup] = 0;
		}

		// To avoid divide-by-zero, stop if total rate is extremely small
		// (Note that OERandomGenerator.gr_inv_rate will not overflow even if
		// the requested rate is very large, because its return is logarithmic)

		if (total_omori_rate < TINY_OMORI_RATE) {
			return 0;
		}

		// Get expected count and magnitude range for next generation,
		// adjusted so that the expected size of the next generation
		// equals the target size

		double expected_count = (double)(cat_params.gen_size_target);
		double next_mag_min = OERandomGenerator.gr_inv_rate (
			cat_params.b,						// b
			cat_params.mref,					// mref
			cat_params.mag_max_sim,				// m2
			expected_count / total_omori_rate	// rate
			);

		// If min magnitude is outside allowable range, bring it into range

		if (next_mag_min < cat_params.mag_min_lo) {
			next_mag_min = cat_params.mag_min_lo;
			expected_count = total_omori_rate * OERandomGenerator.gr_rate (
				cat_params.b,					// b
				cat_params.mref,				// mref
				next_mag_min,					// m1
				cat_params.mag_max_sim			// m2
				);
		}

		else if (next_mag_min > cat_params.mag_min_hi) {
			next_mag_min = cat_params.mag_min_hi;
			expected_count = total_omori_rate * OERandomGenerator.gr_rate (
				cat_params.b,					// b
				cat_params.mref,				// mref
				next_mag_min,					// m1
				cat_params.mag_max_sim			// m2
				);
		}

		// Very small expected counts are treated as zero

		if (expected_count < SMALL_EXPECTED_COUNT) {
			return 0;
		}

		// If the generation is too large ...

		if (cat_params.max_cat_size > 0 && expected_count > (double)(cat_params.max_cat_size)) {
			cat_builder.set_cat_stop_time (find_time_completed());
			cat_builder.set_cat_result_code (OEConstants.CAT_RESULT_GEN_TOO_LARGE);
			return 0;
		}

		// The size of the next generation is a Poisson random variable
		// with the expected value

		int next_gen_size = rangen.poisson_sample_checked (expected_count);

		// If it's zero, we're done

		if (next_gen_size <= 0) {
			return 0;
		}

		// If the catalog is too large ...

		if (cat_params.max_cat_size > 0 && cat_builder.size() > cat_params.max_cat_size) {
			cat_builder.set_cat_stop_time (find_time_completed());
			cat_builder.set_cat_result_code (OEConstants.CAT_RESULT_CAT_TOO_LARGE);
			return 0;
		}

		// If there are too many generations ...

		if (cat_params.gen_count_max > 0 && next_i_gen >= cat_params.gen_count_max) {
			cat_builder.set_cat_stop_time (find_time_completed());
			cat_builder.set_cat_result_code (OEConstants.CAT_RESULT_TOO_MANY_GEN);
			return 0;
		}

		//--- Determine stop time as minimum time of child ruptures above our maximum magnitude

		// The stop time we use to discard generated ruptures

		double stop_time = eff_tend;
		double stop_time_minus_epsilon = stop_time - cat_params.teps;

		// If we are checking for early termination ...

		if (cat_params.mag_excess > cat_params.mag_eps) {

			// Get the expected number of ruptures to check, using the ratio of
			// the G-R rates for our magnitude range and for the above-max magnitude range
			// (stopping occurs when there would be a simulated aftershock with
			// above-max magnitude).

			double expected_stop_checks;

			// If unbounded above-max magnitude range ...

			if (cat_params.mag_excess >= OEConstants.NO_MAG_POS_CHECK) {
				expected_stop_checks = expected_count * OERandomGenerator.gr_ratio_rate_unb_target (
					cat_params.b,									// b
					next_mag_min,									// sm1
					cat_params.mag_max_sim,							// sm2
					cat_params.mag_max_sim							// tm1
					);
			}

			// Otherwise, bounded above-max magnitude range ...

			else {
				expected_stop_checks = expected_count * OERandomGenerator.gr_ratio_rate (
					cat_params.b,									// b
					next_mag_min,									// sm1
					cat_params.mag_max_sim,							// sm2
					cat_params.mag_max_sim,							// tm1
					cat_params.mag_max_sim + cat_params.mag_excess	// tm2
					);
			}

			// The the number of stop checks is a Poisson random variable
			// with the expected value

			int stop_check_count = rangen.poisson_sample_checked (expected_stop_checks);

			// Distribute the child earthquakes over the possible parents
			// with probability proportional to each parent's expected rate

			for (int n = 0; n < stop_check_count; ++n) {
				int i_parent = rangen.cumulative_sample (work_omori_rate, cur_gen_size);

				// Get the rupture, time only, in the current generation

				cat_builder.get_rup_time (cur_i_gen, i_parent, cur_rup);

				// If the rupture is more than epsilon before the stop time ...

				if (cur_rup.t_day < stop_time_minus_epsilon) {
				
					// Assign a time to this child

					next_rup.t_day = rangen.omori_sample_shifted (
						cat_params.p,			// p
						cat_params.c,			// c
						cur_rup.t_day,			// t0
						cat_params.tbegin,		// t1
						eff_tend				// t2
						);

					// If it's a new earliest stop time, save it

					if (next_rup.t_day < stop_time) {
						stop_time = next_rup.t_day;
						stop_time_minus_epsilon = stop_time - cat_params.teps;
						cat_builder.set_cat_stop_time (stop_time);
						cat_builder.set_cat_result_code (OEConstants.CAT_RESULT_EARLY_STOP);
					}
				}
			}
		}

		//--- Generate child earthquakes in the next generation

		// Distribute the child earthquakes over the possible parents
		// with probability proportional to each parent's expected rate

		for (int n = 0; n < next_gen_size; ++n) {
			int i_parent = rangen.cumulative_sample (work_omori_rate, cur_gen_size);
			work_child_count[i_parent]++;
		}

		// Set up generation info for the next generation

		next_gen_info.set (
			next_mag_min,				// gen_mag_min,
			cat_params.mag_max_sim		// gen_mag_max
			);

		// Begin a new generation

		cat_builder.begin_generation (next_gen_info);

		// Actual generation size

		int actual_gen_size = 0;

		// Scan the current generation ...

		for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

			// Get the child count, and check it's non-zero

			int child_count = work_child_count[cur_j_rup];
			if (child_count > 0) {

				// Get the rupture in the current generation, time and location only

				cat_builder.get_rup_time_x_y (cur_i_gen, cur_j_rup, cur_rup);

				// If the rupture is more than epsilon before the stop time ...

				if (cur_rup.t_day < stop_time_minus_epsilon) {

					// Loop over children

					for (int n = 0; n < child_count; ++n) {
				
						// Assign a time to this child

						next_rup.t_day = rangen.omori_sample_shifted (
							cat_params.p,			// p
							cat_params.c,			// c
							cur_rup.t_day,			// t0
							cat_params.tbegin,		// t1
							eff_tend				// t2
							);

						// If the child time is before the stop time ...

						if (next_rup.t_day < stop_time) {

							// Assign a magnitude to this child

							next_rup.rup_mag = rangen.gr_sample (
								cat_params.b,				// b
								next_gen_info.gen_mag_min,	// m1
								next_gen_info.gen_mag_max	// m2
								);

							// Assign a productivity to this child

							next_rup.k_prod = OEStatsCalc.calc_k_corr (
								next_rup.rup_mag,		// m0
								cat_params,				// cat_params
								next_gen_info			// gen_info
								);

							// Assign a parent to this child

							next_rup.rup_parent = cur_j_rup;

							// Assign coordinates to this child
							// (Since this is temporal ETAS, just copy the parent coordinates)

							next_rup.x_km = cur_rup.x_km;
							next_rup.y_km = cur_rup.y_km;

							// Save the rupture

							cat_builder.add_rup (next_rup);

							// Count it

							++actual_gen_size;
						}
					}
				}
			}
		}

		// End the generation

		cat_builder.end_generation ();

		// Return the size of the new generation

		return actual_gen_size;
	}




	// Calculate the next generation.  [Original version]
	// Returns the number of earthquakes in the new generation.
	// If the return value is zero, then no generation was added,
	// and the catalog has reached its end.
	// Note: Before calling this function for the first time (after calling
	// setup), you must create the first generation to seed the catalog.

	public int calc_next_gen_original () {

		// The next generation number is the current number of generations

		int next_i_gen = cat_builder.get_gen_count();

		// If we already have the maximum number of generations, don't create any more

		if (next_i_gen >= cat_params.gen_count_max) {
			return 0;
		}

		// The current generation number is the last generation in the catalog

		int cur_i_gen = next_i_gen - 1;

		// Get information for the current generation

		cat_builder.get_gen_info (cur_i_gen, cur_gen_info);

		// Initialize information for the next generation

		next_gen_info.clear();

		// Get the size of the current generation

		int cur_gen_size = cat_builder.get_gen_size (cur_i_gen);

		// It shouldn't be zero, but if it is, don't create another one

		if (cur_gen_size == 0) {
			return 0;
		}

		// Ensure workspace arrays are large enough for the current generation

		if (cur_gen_size > workspace_capacity) {
			do {
				workspace_capacity = workspace_capacity * 2;
			} while (cur_gen_size > workspace_capacity);

			work_omori_rate = new double[workspace_capacity];
			work_child_count = new int[workspace_capacity];
		}

		// Scan the current generation ...

		double total_omori_rate = 0.0;

		for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

			// Get the rupture in the current generation, time and productivity

			cat_builder.get_rup_time_prod (cur_i_gen, cur_j_rup, cur_rup);

			// Calculate its expected rate in the forecast interval

			double omori_rate = cur_rup.k_prod * OERandomGenerator.omori_rate_shifted (
				cat_params.p,			// p
				cat_params.c,			// c
				cur_rup.t_day,			// t0
				cat_params.teps,		// teps
				cat_params.tbegin,		// t1
				cat_params.tend			// t2
				);

			// Accumulate the total

			total_omori_rate += omori_rate;
			work_omori_rate[cur_j_rup] = total_omori_rate;

			// Initialize child count

			work_child_count[cur_j_rup] = 0;
		}

		// To avoid divide-by-zero, stop if total rate is extremely small
		// (Note that OERandomGenerator.gr_inv_rate will not overflow even if
		// the requested rate is very large, because its return is logarithmic)

		if (total_omori_rate < TINY_OMORI_RATE) {
			return 0;
		}

		// Get expected count and magnitude range for next generation,
		// adjusted so that the expected size of the next generation
		// equals the target size

		double expected_count = (double)(cat_params.gen_size_target);
		double next_mag_min = OERandomGenerator.gr_inv_rate (
			cat_params.b,						// b
			cat_params.mref,					// mref
			cat_params.mag_max_sim,				// m2
			expected_count / total_omori_rate	// rate
			);

		// If min magnitude is outside allowable range, bring it into range

		if (next_mag_min < cat_params.mag_min_lo) {
			next_mag_min = cat_params.mag_min_lo;
			expected_count = total_omori_rate * OERandomGenerator.gr_rate (
				cat_params.b,					// b
				cat_params.mref,				// mref
				next_mag_min,					// m1
				cat_params.mag_max_sim			// m2
				);
		}

		else if (next_mag_min > cat_params.mag_min_hi) {
			next_mag_min = cat_params.mag_min_hi;
			expected_count = total_omori_rate * OERandomGenerator.gr_rate (
				cat_params.b,					// b
				cat_params.mref,				// mref
				next_mag_min,					// m1
				cat_params.mag_max_sim			// m2
				);
		}

		// Very small expected counts are treated as zero

		if (expected_count < SMALL_EXPECTED_COUNT) {
			return 0;
		}

		// The size of the next generation is a Poisson random variable
		// with the expected value

		int next_gen_size = rangen.poisson_sample_checked (expected_count);

		// If it's zero, we're done

		if (next_gen_size <= 0) {
			return 0;
		}

		// Distribute the child earthquakes over the possible parents
		// with probability proportional to each parent's expected rate

		for (int n = 0; n < next_gen_size; ++n) {
			int i_parent = rangen.cumulative_sample (work_omori_rate, cur_gen_size);
			work_child_count[i_parent]++;
		}

		// Set up generation info for the next generation

		next_gen_info.set (
			next_mag_min,				// gen_mag_min,
			cat_params.mag_max_sim		// gen_mag_max
			);

		// Begin a new generation

		cat_builder.begin_generation (next_gen_info);

		// Scan the current generation ...

		for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

			// Get the child count, and check it's non-zero

			int child_count = work_child_count[cur_j_rup];
			if (child_count > 0) {

				// Get the rupture in the current generation, time and location

				cat_builder.get_rup_time_x_y (cur_i_gen, cur_j_rup, cur_rup);

				// Loop over children

				for (int n = 0; n < child_count; ++n) {
				
					// Assign a time to this child

					next_rup.t_day = rangen.omori_sample_shifted (
						cat_params.p,			// p
						cat_params.c,			// c
						cur_rup.t_day,			// t0
						cat_params.tbegin,		// t1
						cat_params.tend			// t2
						);

					// Assign a magnitude to this child

					next_rup.rup_mag = rangen.gr_sample (
						cat_params.b,				// b
						next_gen_info.gen_mag_min,	// m1
						next_gen_info.gen_mag_max	// m2
						);

					// Assign a productivity to this child

					next_rup.k_prod = OEStatsCalc.calc_k_corr (
						next_rup.rup_mag,		// m0
						cat_params,				// cat_params
						next_gen_info			// gen_info
						);

					// Assign a parent to this child

					next_rup.rup_parent = cur_j_rup;

					// Assign coordinates to this child
					// (Since this is temporal ETAS, just copy the parent coordinates)

					next_rup.x_km = cur_rup.x_km;
					next_rup.y_km = cur_rup.y_km;

					// Save the rupture

					cat_builder.add_rup (next_rup);
				}
			}
		}

		// End the generation

		cat_builder.end_generation ();

		// Return the size of the new generation

		return next_gen_size;
	}




	// Calculate all generations.
	// Returns the number of generations.
	// Note: Before calling this function (after calling setup),
	// you must call cat_builder.begin_catalog() and create the first
	// generation to seed the catalog.
	// Note: This function calls cat_builder.end_catalog();

	public int calc_all_gen () {

		// Get the catalog parameters
		// (in case they have changed since setup was called)

		this.cat_builder.get_cat_params (cat_params);

		// Make generations until end of catalog

		int gen_size = cat_builder.get_gen_size (0);
		while (gen_size > 0) {
			gen_size = calc_next_gen();
		}

		// End the catalog

		cat_builder.end_catalog();

		// Return number of generations

		return cat_builder.get_gen_count();
	}




	//----- Testing -----




	// Generate a simple catalog, seeded with a single mainshock.
	// Parameters:
	//  test_cat_params = Catalog parameters.
	//  mag_main = Mainshock magnitude.
	// Returns the resulting catalog.

	public static OECatalogStorage gen_simple_catalog (OECatalogParams test_cat_params, double mag_main) {

		// Get the random number generator

		OERandomGenerator the_rangen = OERandomGenerator.get_thread_rangen();

		// Allocate the storage (which is also the builder)

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Allocate a generator

		OECatalogGenerator cat_generator = new OECatalogGenerator();

		// Begin the catalog

		cat_storage.begin_catalog (test_cat_params);

		// Begin the first generation

		OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
			test_cat_params.mref,	// gen_mag_min
			test_cat_params.msup	// gen_mag_max
		);

		cat_storage.begin_generation (test_gen_info);

		// Insert the mainshock rupture

		OERupture mainshock_rup = new OERupture();

		double k_prod = OEStatsCalc.calc_k_corr (
			mag_main,			// m0
			test_cat_params,	// cat_params
			test_gen_info		// gen_info
		);

		mainshock_rup.set (
			0.0,			// t_day
			mag_main,		// rup_mag
			k_prod,			// k_prod
			RUPPAR_SEED,	// rup_parent
			0.0,			// x_km
			0.0				// y_km
		);

		cat_storage.add_rup (mainshock_rup);

		// End the first generation

		cat_storage.end_generation();

		// Set up the catalog generator
				
		cat_generator.setup (the_rangen, cat_storage, false);

		// Calculate all generations and end the catalog

		cat_generator.calc_all_gen();

		// Tell the generator to forget the catalog

		cat_generator.forget();

		// Return the catalog

		return cat_storage;
	}




	// A version of the random generator used for testing the branch ratio.
	// Function omori_sample_shifted is overridden to always return the earliest
	// possible time for an aftershock.  This eliminates the reduction in
	// branching that occurs as aftershocks appear later in the forecast window.

	public static class TestBranchRatioRanGen extends OERandomGenerator {
		@Override
		public double omori_sample_shifted (double p, double c, double t0, double t1, double t2) {
			double u = 0.0;
			return OERandomGenerator.omori_rescale_shifted (p, c, t0, t1, t2, u);
		}
	}




	// A version of the random generator used for testing the effect of Poisson rupture counts.
	// Function poisson_sample_checked is overridden to always return the mean.
	// Function omori_sample_shifted is overridden to always return the earliest
	// possible time for an aftershock.
	// This eliminates most aleatory variability.

	public static class TestNoPoissonRanGen extends TestBranchRatioRanGen {
		@Override
		public int poisson_sample_checked (double mean) {
			return (int)Math.round (mean);
		}
	}




	// Get a special version of the random generatot that places all aftershocks
	// at the earliest possible time.
	// It can also make Poisson calls always return the mean.
	// The argument selects which random number generator to return.
	// This function is primarily for testing.

	public static final int MEAR_NORMAL = 0;	// return the normal per-thread random generator
	public static final int MEAR_EARLY = 1;		// return the special version with all aftershocks as early as possible
	public static final int MEAR_NO_POIS = 2;	// return the special version that additionally supplresses Poisson calls

	public static OERandomGenerator make_early_as_rangen (int mear_opt) {
		switch (mear_opt) {
		case MEAR_NORMAL:
			return OERandomGenerator.get_thread_rangen();
		case MEAR_EARLY:
			return new TestBranchRatioRanGen();
		case MEAR_NO_POIS:
			return new TestNoPoissonRanGen();
		}
		throw new IllegalArgumentException ("OECatalogGenerator.make_early_as_rangen: Invalid random generator option: mear_opt = " + mear_opt);
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OECatalogStorage : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OECatalogStorage : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #1 except does not allow the minimum magnitude to be adjusted.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OECatalogStorage : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Prevent minimum magnitude adjustment

				test_cat_params.mag_min_lo = test_cat_params.mag_min_sim;
				test_cat_params.mag_min_hi = test_cat_params.mag_min_sim;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #1 except dumps the entire catalog.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OECatalogStorage : Invalid 'test3' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Dump the catalog

				System.out.println ();
				System.out.println ("Catalog dump...");
				System.out.println ();
				System.out.println (cat_storage.dump_to_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #2 except dumps the entire catalog.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OECatalogStorage : Invalid 'test4' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Prevent minimum magnitude adjustment

				test_cat_params.mag_min_lo = test_cat_params.mag_min_sim;
				test_cat_params.mag_min_hi = test_cat_params.mag_min_sim;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Dump the catalog

				System.out.println ();
				System.out.println ("Catalog dump...");
				System.out.println ();
				System.out.println (cat_storage.dump_to_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #1 with control over magnitude ranges.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 13 additional arguments

			if (args.length != 14) {
				System.err.println ("OECatalogStorage : Invalid 'test5' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				double the_mag_min_sim = Double.parseDouble (args[10]);
				double the_mag_max_sim = Double.parseDouble (args[11]);
				double the_mag_min_lo = Double.parseDouble (args[12]);
				double the_mag_min_hi = Double.parseDouble (args[13]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #5 with all aftershocks set to the earliest possible time.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 13 additional arguments

			if (args.length != 14) {
				System.err.println ("OECatalogStorage : Invalid 'test6' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				double the_mag_min_sim = Double.parseDouble (args[10]);
				double the_mag_max_sim = Double.parseDouble (args[11]);
				double the_mag_min_lo = Double.parseDouble (args[12]);
				double the_mag_min_hi = Double.parseDouble (args[13]);

				// Say hello

				System.out.println ("Generating catalog with given parameters, with early aftershocks");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);

				// Get the random number generator

				OERandomGenerator rangen = new TestBranchRatioRanGen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #5 with additional information for each generation.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 13 additional arguments

			if (args.length != 14) {
				System.err.println ("OECatalogStorage : Invalid 'test7' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				double the_mag_min_sim = Double.parseDouble (args[10]);
				double the_mag_max_sim = Double.parseDouble (args[11]);
				double the_mag_min_lo = Double.parseDouble (args[12]);
				double the_mag_min_hi = Double.parseDouble (args[13]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string_2());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mear_opt
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Same as test #6 with additional information for each generation.
		// Same as test #7 with option for all aftershocks set to the earliest possible time.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 14 additional arguments

			if (args.length != 15) {
				System.err.println ("OECatalogStorage : Invalid 'test8' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				double the_mag_min_sim = Double.parseDouble (args[10]);
				double the_mag_max_sim = Double.parseDouble (args[11]);
				double the_mag_min_lo = Double.parseDouble (args[12]);
				double the_mag_min_hi = Double.parseDouble (args[13]);
				int mear_opt = Integer.parseInt (args[14]);

				// Say hello

				System.out.println ("Generating catalog with given parameters, with early aftershocks");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("mear_opt = " + mear_opt);

				// Get the random number generator

				//OERandomGenerator rangen = new TestBranchRatioRanGen();
				OERandomGenerator rangen = make_early_as_rangen (mear_opt);

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Begin the catalog

				System.out.println ();
				System.out.println ("Generating catalog...");

				cat_storage.begin_catalog (test_cat_params);

				// Begin the first generation

				OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
					test_cat_params.mref,	// gen_mag_min
					test_cat_params.msup	// gen_mag_max
				);

				cat_storage.begin_generation (test_gen_info);

				// Insert the mainshock rupture

				OERupture mainshock_rup = new OERupture();

				double k_prod = OEStatsCalc.calc_k_corr (
					mag_main,			// m0
					test_cat_params,	// cat_params
					test_gen_info		// gen_info
				);

				mainshock_rup.set (
					0.0,			// t_day
					mag_main,		// rup_mag
					k_prod,			// k_prod
					RUPPAR_SEED,	// rup_parent
					0.0,			// x_km
					0.0				// y_km
				);

				cat_storage.add_rup (mainshock_rup);

				// End the first generation

				cat_storage.end_generation();

				// Make the catalog generator
				
				OECatalogGenerator cat_generator = new OECatalogGenerator();
				cat_generator.setup (rangen, cat_storage, true);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string_2());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OECatalogStorage : Unrecognized subcommand : " + args[0]);
		return;

	}

}
