package org.opensha.oaf.oetas;

import static org.opensha.oaf.oetas.OEConstants.TINY_OMORI_RATE;
import static org.opensha.oaf.oetas.OEConstants.SMALL_EXPECTED_COUNT;


// Class for scanning an Operational ETAS catalog and sending it to a set of accumulators.
// Author: Michael Barall 12/31/2019.

public class OECatalogScanner {

	//----- Workspace -----

	// Communication area used for communicating with consumers.

	private OECatalogScanComm comm;

	// The number of consumers.

	private int consumer_count;

	// The set of consumers.
	// The length of this array equals consumer_count.

	private OECatalogConsumer[] consumers;

	// True if currently open.

	private boolean f_open;




	//----- Workspace for generating sterile ruptures -----
	//
	// Note: Data variables are a subset of those in OECatalogGenerator.

	// Rupture information for the parent of the current sterile rupture.

	private OERupture cur_rup;

	// Default size of workspace arrays.

	private static final int DEF_WORKSPACE_CAPACITY = 1000;

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




	// Clear to no consumers.

	public void clear () {
		consumer_count = 0;
		consumers = null;
		return;
	}




	// Default constructor.

	public OECatalogScanner () {

		// Allocate the communication area

		comm = new OECatalogScanComm();

		// Initialize to no consumers

		consumer_count = 0;
		consumers = null;

		// Allocate workspace for generating sterile ruptures

		cur_rup = new OERupture();
		workspace_capacity = DEF_WORKSPACE_CAPACITY;
		work_omori_rate = new double[workspace_capacity];
		work_child_count = new int[workspace_capacity];

		// Not open

		f_open = false;
	}




	// Set up to perform scanning.
	// Parameters:
	//  accumulators = One or more accumulators to receive the scan.

	public void setup (OECatalogAccumulator... accumulators) {

		// Obtain a list of consumers

		consumer_count = accumulators.length;
		consumers = new OECatalogConsumer[consumer_count];

		for (int i = 0; i < consumer_count; ++i) {
			consumers[i] = accumulators[i].make_consumer();
		}

		// Not open

		f_open = false;
	
		return;
	}




	//----- Scanning -----




	// Open all the catalog consumers.
	// Perform any setup needed to begin consuming a catalog.
	// Note: All the accumulators must be in an appropriate state for their consumers
	// to be opened (e.g., OECatalogAccumulator.begin_accumulation() has been called).

	public void open () {
	
		// Error if already open

		if (f_open) {
			throw new IllegalStateException ("OECatalogScanner.open -- Scanner is already open");
		}

		// Error if not set up

		if (consumers == null) {
			throw new IllegalStateException ("OECatalogScanner.open -- Scanner is not set up");
		}

		// Open all the consumers

		for (OECatalogConsumer consumer : consumers) {
			consumer.open();
		}

		// Mark open

		f_open = true;

		return;
	}




	// Close all the catalog consumers.
	// Perform any final tasks needed to finish consuming a catalog,
	// such as storing results into an accumulator.

	public void close () {
	
		// If we're open ...

		if (f_open) {
		
			// Mark closed

			f_open = false;

			// Close all the consumers

			for (OECatalogConsumer consumer : consumers) {
				consumer.close();
			}
		}

		return;
	}




	// Scan the catalog.
	// Parameters:
	//  view = Catalog view.
	//  rangen = Random number generator to use.

	public void scan (OECatalogView view, OERandomGenerator rangen) {

		// There must be at least one generation

		if (view.get_gen_count() < 1) {
			throw new IllegalArgumentException ("OECatalogScanner.scan -- Empty catalog");
		}

		// Set up the per-catalog information

		comm.setup_cat_from_view (view, rangen);

		// Tell the consumers we are beginning a catalog

		for (OECatalogConsumer consumer : consumers) {
			consumer.begin_catalog (comm);
		}

		// Set up the per-generation information for the seeds

		comm.setup_gen_from_view (view, 0);

		// Tell the consumers we are beginning the seed generation

		for (OECatalogConsumer consumer : consumers) {
			consumer.begin_seed_generation (comm);
		}

		// Loop over ruptures in the seed generation

		for (int j_rup = 0; j_rup < comm.gen_size; ++j_rup) {
		
			// Set up the per-rupture information for the seeds

			comm.setup_rup_from_view (view, j_rup);

			// Pass seed rupture to the consumers

			for (OECatalogConsumer consumer : consumers) {
				consumer.next_seed_rup (comm);
			}
		}

		// Tell the consumers we are ending the seed generation

		for (OECatalogConsumer consumer : consumers) {
			consumer.end_seed_generation (comm);
		}

		// Loop over ETAS generations...

		for (int i_gen = 1; i_gen < view.get_gen_count(); ++i_gen) {

			// Set up the per-generation information

			comm.setup_gen_from_view (view, i_gen);

			// Tell the consumers we are beginning the generation

			for (OECatalogConsumer consumer : consumers) {
				consumer.begin_generation (comm);
			}

			// Loop over ruptures in the generation

			for (int j_rup = 0; j_rup < comm.gen_size; ++j_rup) {
		
				// Set up the per-rupture information

				comm.setup_rup_from_view (view, j_rup);

				// Pass rupture to the consumers

				for (OECatalogConsumer consumer : consumers) {
					consumer.next_rup (comm);
				}
			}

			// If sterile ruptures are needed ...

			if (comm.is_sterile_mag()) {

				// Calculate the sterile ruptures

				calc_sterile_rups (view);
			}

			// Tell the consumers we are ending the generation

			for (OECatalogConsumer consumer : consumers) {
				consumer.end_generation (comm);
			}
		}

		// Tell the consumers we are ending a catalog

		for (OECatalogConsumer consumer : consumers) {
			consumer.end_catalog (comm);
		}

		// Forget retained objects

		comm.forget();

		return;
	}




	// Calculate the sterile ruptures, and send them to the consumers.
	// Parameters:
	//  view = Catalog view.
	// Returns the number of sterile ruptures generated.
	// Note: This function is nearly the same as OECatalogGenerator.calc_next_gen().
	// In this function, "current generation" refers to generation comm.i_gen - 1.
	// Note: The magnitude range for sterile ruptures is from comm.sterile_mag to
	// comm.gen_info.gen_mag_min.  The caller must ensure this range is positive.

	public int calc_sterile_rups (OECatalogView view) {

		// The current generation number is the previous generation passed to consumers

		int cur_i_gen = comm.i_gen - 1;

		// Get the size of the current generation

		int cur_gen_size = view.get_gen_size (cur_i_gen);

		// It shouldn't be zero, but if it is, done

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

			view.get_rup (cur_i_gen, cur_j_rup, cur_rup);

			// Calculate its expected rate in the forecast interval

			double omori_rate = cur_rup.k_prod * OERandomGenerator.omori_rate_shifted (
				comm.cat_params.p,			// p
				comm.cat_params.c,			// c
				cur_rup.t_day,				// t0
				comm.cat_params.teps,		// teps
				comm.cat_params.tbegin,		// t1
				comm.cat_params.tend		// t2
				);

			// Accumulate the total

			total_omori_rate += omori_rate;
			work_omori_rate[cur_j_rup] = total_omori_rate;

			// Initialize child count

			work_child_count[cur_j_rup] = 0;
		}

		// Stop if total rate is extremely small

		if (total_omori_rate < TINY_OMORI_RATE) {
			return 0;
		}

		// Get total expected count of earthquakes

		double expected_count = total_omori_rate * OERandomGenerator.gr_rate (
			comm.cat_params.b,				// b
			comm.cat_params.mref,			// mref
			comm.sterile_mag,				// m1
			comm.gen_info.gen_mag_min		// m2
			);

		// Very small expected counts are treated as zero

		if (expected_count < SMALL_EXPECTED_COUNT) {
			return 0;
		}

		// The number of sterile ruptures is a Poisson random variable
		// with the expected value

		int sterile_rup_count = comm.rangen.poisson_sample_checked (expected_count);

		// If it's zero, we're done

		if (sterile_rup_count <= 0) {
			return 0;
		}

		// Distribute the child earthquakes over the possible parents
		// with probability proportional to each parent's expected rate

		for (int n = 0; n < sterile_rup_count; ++n) {
			int i_parent = comm.rangen.cumulative_sample (work_omori_rate, cur_gen_size);
			work_child_count[i_parent]++;
		}

		// Sterile rupture index, beginning after the ETAS ruptures

		comm.j_rup = comm.gen_size;

		// Scan the current generation ...

		for (int cur_j_rup = 0; cur_j_rup < cur_gen_size; ++cur_j_rup) {

			// Get the child count, and check it's non-zero

			int child_count = work_child_count[cur_j_rup];
			if (child_count > 0) {

				// Get the rupture in the current generation

				view.get_rup (cur_i_gen, cur_j_rup, cur_rup);

				// Loop over children

				for (int n = 0; n < child_count; ++n) {
				
					// Assign a time to this child

					comm.rup.t_day = comm.rangen.omori_sample_shifted (
						comm.cat_params.p,			// p
						comm.cat_params.c,			// c
						cur_rup.t_day,				// t0
						comm.cat_params.tbegin,		// t1
						comm.cat_params.tend		// t2
						);

					// Assign a magnitude to this child

					comm.rup.rup_mag = comm.rangen.gr_sample (
						comm.cat_params.b,			// b
						comm.sterile_mag,			// m1
						comm.gen_info.gen_mag_min	// m2
						);

					// Assign a productivity to this child, zero for sterile

					comm.rup.k_prod = 0.0;

					// Assign a parent to this child

					comm.rup.rup_parent = cur_j_rup;

					// Assign coordinates to this child
					// (Since this is temporal ETAS, just copy the parent coordinates)

					comm.rup.x_km = cur_rup.x_km;
					comm.rup.y_km = cur_rup.y_km;

					// Pass rupture to the consumers

					for (OECatalogConsumer consumer : consumers) {
						consumer.next_sterile_rup (comm);
					}

					// Advance sterile rupture index

					comm.j_rup++;
				}
			}
		}

		// Return the number of sterile earthquakes

		return sterile_rup_count;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OECatalogScanner : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OECatalogStorage : Unrecognized subcommand : " + args[0]);
		return;

	}

}
