package org.opensha.oaf.oetas;

import static org.opensha.oaf.oetas.OEConstants.TINY_OMORI_RATE;
import static org.opensha.oaf.oetas.OEConstants.SMALL_EXPECTED_COUNT;


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




	// Calculate the next generation.
	// Returns the number of earthquakes in the new generation.
	// If the return value is zero, then no generation was added,
	// and the catalog has reached its end.
	// Note: Before calling this function for the first time (after calling
	// setup), you must create the first generation to seed the catalog.

	public int calc_next_gen () {

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

			// Get the rupture in the current generation

			cat_builder.get_rup (cur_i_gen, cur_j_rup, cur_rup);

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

				// Get the rupture in the current generation

				cat_builder.get_rup (cur_i_gen, cur_j_rup, cur_rup);

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
					-1,				// rup_parent
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
					-1,				// rup_parent
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
					-1,				// rup_parent
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
					-1,				// rup_parent
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




		// Unrecognized subcommand.

		System.err.println ("OECatalogStorage : Unrecognized subcommand : " + args[0]);
		return;

	}

}
