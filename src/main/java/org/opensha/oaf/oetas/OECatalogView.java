package org.opensha.oaf.oetas;

import java.util.Collection;


// Interface to provide access to a catalog of Operational ETAS ruptures.
// Author: Michael Barall 11/03/2019.
//
// The catalog appears as a read-only list of OERupture objects.
//
// The ordering of ruptures in the catalog is not specified by this interface,
// except that ruptures are organized into a series of generations.
//
// It is required that multiple threads must be able to access a catalog through
// this interface, without the use of locks.
//
// Design note: The interface is designed to fill in a caller-supplied OERupture object.
// This avoids the need to create very large numbers of OERupture objects, while still
// allowing for a thread-safe implementation.
//
// Implementation note: Typically, the catalog implementation stores ruptures in a
// compact form, not as a collection of OERupture objects.

public interface OECatalogView {

	// Get parameters for the catalog.
	// Parameters:
	//  cat_params = Structure to receive the catalog parameters.

	public void get_cat_params (OECatalogParams cat_params);

	// Get the total number of ruptures in the catalog.

	public int size ();

	// Get the total number of ruptures in the catalog before the stop time.
	// This cannot be called until after the catalog is fully built.

	public int valid_size ();

	// Get the number of generations in the catalog.

	public int get_gen_count ();

	// Get the number of ruptures in the i-th generation.
	// Parameters:
	//  i_gen = Generation number.

	public int get_gen_size (int i_gen);

	// Get the number of ruptures in the i-th generation before the stop time.
	// Parameters:
	//  i_gen = Generation number.
	// This cannot be called until after the catalog is fully built.

	public int get_gen_valid_size (int i_gen);

	// Get information about the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	public void get_gen_info (int i_gen, OEGenerationInfo gen_info);

	// Get the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.

	public void get_rup_full (int i_gen, int j_rup, OERupture rup);

	// Get the time of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day.
	// Other fields may or may not be modified.

	public void get_rup_time (int i_gen, int j_rup, OERupture rup);

	// Get the time and productivity of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day and rup.k_prod.
	// Other fields may or may not be modified.

	public void get_rup_time_prod (int i_gen, int j_rup, OERupture rup);

	// Get the time and location of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day, rup.x_km, and rup.y_km.
	// Other fields may or may not be modified.

	public void get_rup_time_x_y (int i_gen, int j_rup, OERupture rup);

	// Get the time at which the catalog stops.
	// The return value need not satisfy stop_time <= cat_params.tend; however,
	// the catalog does not extend past cat_params.tend regardless of stop_time.
	// If stop_time < cat_params.tend, then the catalog ended before the full time interval.

	public double get_cat_stop_time ();

	// Get the catalog result code, see OEConstants.CAT_RESULT_XXXX.

	public int get_cat_result_code ();




	// Construct a string that summarizes the catalog contents.
	// This displays the catalog size and generation count,
	// parameters, and info for each generation.

	public default String summary_and_gen_list_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogView:" + "\n");

		// Catalog globals

		result.append ("cat_result_code = " + get_cat_result_code()  + "\n");
		result.append ("cat_stop_time = " + get_cat_stop_time()  + "\n");

		// Size and generation count

		int the_size = size();
		int the_valid_size = valid_size();
		int gen_count = get_gen_count();
		result.append ("size = "       + the_size       + "\n");
		result.append ("valid_size = " + the_valid_size + "\n");
		result.append ("gen_count = "  + gen_count      + "\n");

		// Catalog parameters

		OECatalogParams cat_params = new OECatalogParams();
		get_cat_params (cat_params);
		result.append (cat_params.toString());

		// List of generation info

		OEGenerationInfo gen_info = new OEGenerationInfo();
		for (int i_gen = 0; i_gen < gen_count; ++i_gen) {
			int gen_size = get_gen_size (i_gen);
			int gen_valid_size = get_gen_valid_size (i_gen);
			get_gen_info (i_gen, gen_info);
			result.append (gen_info.one_line_string (i_gen, gen_size, gen_valid_size) + "\n");
		}

		return result.toString();
	}




	// Construct a string that summarizes the catalog contents, version 2.
	// This displays the catalog size and generation count,
	// parameters, and info for each generation.
	// This version adds actual rupture count and time/mag range for each generation.

	public default String summary_and_gen_list_string_2() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogView:" + "\n");

		// Catalog globals

		result.append ("cat_result_code = " + get_cat_result_code()  + "\n");
		result.append ("cat_stop_time = " + get_cat_stop_time()  + "\n");

		// Size and generation count

		int the_size = size();
		int the_valid_size = valid_size();
		int gen_count = get_gen_count();
		result.append ("size = "       + the_size       + "\n");
		result.append ("valid_size = " + the_valid_size + "\n");
		result.append ("gen_count = "  + gen_count      + "\n");

		// Catalog parameters

		OECatalogParams cat_params = new OECatalogParams();
		get_cat_params (cat_params);
		result.append (cat_params.toString());

		// List of generation info

		OEGenerationInfo gen_info = new OEGenerationInfo();
		OERupture rup = new OERupture();
		double stop_time = get_cat_stop_time();

		for (int i_gen = 0; i_gen < gen_count; ++i_gen) {
			int gen_size = get_gen_size (i_gen);
			int gen_valid_size = get_gen_valid_size (i_gen);
			get_gen_info (i_gen, gen_info);

			int count = 0;
			double t_lo = 0.0;
			double t_hi = 0.0;
			double mag_lo = 0.0;
			double mag_hi = 0.0;

			for (int j_rup = 0; j_rup < gen_size; ++j_rup) {
				get_rup_full (i_gen, j_rup, rup);
				if (rup.t_day <= stop_time) {
					if (count == 0) {
						t_lo = rup.t_day;
						t_hi = rup.t_day;
						mag_lo = rup.rup_mag;
						mag_hi = rup.rup_mag;
					} else {
						t_lo = Math.min (t_lo, rup.t_day);
						t_hi = Math.max (t_hi, rup.t_day);
						mag_lo = Math.min (mag_lo, rup.rup_mag);
						mag_hi = Math.max (mag_hi, rup.rup_mag);
					}
					++count;
				}
			}

			result.append (gen_info.one_line_string (i_gen, gen_size, gen_valid_size, count, t_lo, t_hi, mag_lo, mag_hi) + "\n");
		}

		return result.toString();
	}




	// Construct a string that dumps the entire catalog contents.
	// Caution: This can be very large!

	public default String dump_to_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogView:" + "\n");

		// Catalog globals

		result.append ("cat_result_code = " + get_cat_result_code() + "\n");
		result.append ("cat_stop_time = "   + get_cat_stop_time()   + "\n");

		// Size and generation count

		int the_size = size();
		int the_valid_size = valid_size();
		int gen_count = get_gen_count();
		result.append ("size = "       + the_size       + "\n");
		result.append ("valid_size = " + the_valid_size + "\n");
		result.append ("gen_count = "  + gen_count      + "\n");

		// Catalog parameters

		OECatalogParams cat_params = new OECatalogParams();
		get_cat_params (cat_params);
		result.append (cat_params.toString());

		// List of generation info

		OEGenerationInfo gen_info = new OEGenerationInfo();
		for (int i_gen = 0; i_gen < gen_count; ++i_gen) {
			int gen_size = get_gen_size (i_gen);
			int gen_valid_size = get_gen_valid_size (i_gen);
			get_gen_info (i_gen, gen_info);
			result.append (gen_info.one_line_string (i_gen, gen_size, gen_valid_size) + "\n");
		}

		// Ruptures

		OERupture rup = new OERupture();
		for (int i_gen = 0; i_gen < gen_count; ++i_gen) {
			int gen_size = get_gen_size (i_gen);
			for (int j_rup = 0; j_rup < gen_size; ++j_rup) {
				get_rup_full (i_gen, j_rup, rup);
				result.append (rup.one_line_string (i_gen, j_rup) + "\n");
			}
		}

		return result.toString();
	}




	// Dump the entire catalog contents into a collection of OERupture objects.
	// Parameters:
	//  coll = Collection to receive ruptures.
	//  f_seed = True to include seed ruptures (generation 0).
	//  f_background = True to include ruptures representing a background rate.
	// Note: The OERupture objects are newly allocated.
	// Note: The ordering of the ruptures is unspecified.

	public default void dump_to_collection (Collection<OERupture> coll, boolean f_seed, boolean f_background) {

		// Generation count

		final int gen_count = get_gen_count();

		// Ruptures

		for (int i_gen = (f_seed ? 0 : 1); i_gen < gen_count; ++i_gen) {
			final int gen_size = get_gen_size (i_gen);
			for (int j_rup = 0; j_rup < gen_size; ++j_rup) {
				OERupture rup = new OERupture();
				get_rup_full (i_gen, j_rup, rup);
				if (f_background || rup.t_day > OEConstants.BKGD_TIME_DAYS_CHECK) {
					coll.add (rup);
				}
			}
		}

		return;
	}

}
