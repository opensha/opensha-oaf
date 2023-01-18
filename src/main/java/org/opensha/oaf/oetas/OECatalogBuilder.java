package org.opensha.oaf.oetas;


// Interface for building a catalog of Operational ETAS ruptures.
// Author: Michael Barall 11/29/2019.
//
// The catalog appears as an extendible list of OERupture objects.
//
// The ordering of ruptures in the catalog is not specified by this interface,
// except that ruptures are organized into a series of generations.
//
// Only a single thread may access a catalog through this interface.
//
// Design note: The interface is designed to use a caller-supplied OERupture object.
// This avoids the need to create very large numbers of OERupture objects.
//
// Implementation note: Typically, the catalog implementation stores ruptures in a
// compact form, not as a collection of OERupture objects.

public interface OECatalogBuilder extends OECatalogView {

	//----- Methods inherited from OECatalogView -----

	// Get parameters for the catalog.
	// Parameters:
	//  cat_params = Structure to receive the catalog parameters.

	// public void get_cat_params (OECatalogParams cat_params);		// inherited

	// Get the total number of ruptures in the catalog.

	// public int size ();		// inherited

	// Get the total number of ruptures in the catalog, excluding seed ruptures.

	// public int etas_size ();		// inherited

	// Get the total number of ruptures in the catalog before the stop time.
	// This cannot be called until after the catalog is fully built.

	// public int valid_size ();		// inherited

	// Get the number of generations in the catalog.

	// public int get_gen_count ();		// inherited

	// Get the number of ruptures in the i-th generation.
	// Parameters:
	//  i_gen = Generation number.

	// public int get_gen_size (int i_gen);		// inherited

	// Get the number of ruptures in the i-th generation before the stop time.
	// Parameters:
	//  i_gen = Generation number.
	// This cannot be called until after the catalog is fully built.

	// public int get_gen_valid_size (int i_gen);		// inherited

	// Get information about the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	// public void get_gen_info (int i_gen, OEGenerationInfo gen_info);		// inherited

	// Get the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.

	// public void get_rup_full (int i_gen, int j_rup, OERupture rup);		// inherited

	// Get the time of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day.
	// Other fields may or may not be modified.

	// public void get_rup_time (int i_gen, int j_rup, OERupture rup);		// inherited

	// Get the time and productivity of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day and rup.k_prod.
	// Other fields may or may not be modified.

	// public void get_rup_time_prod (int i_gen, int j_rup, OERupture rup);		// inherited

	// Get the time and location of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day, rup.x_km, and rup.y_km.
	// Other fields may or may not be modified.

	// public void get_rup_time_x_y (int i_gen, int j_rup, OERupture rup);		// inherited

	// Get the time at which the catalog stops.
	// The return value need not satisfy stop_time <= cat_params.tend; however,
	// the catalog does not extend past cat_params.tend regardless of stop_time.
	// If stop_time < cat_params.tend, then the catalog ended before the full time interval.

	// public double get_cat_stop_time ();		// inherited

	// Get the catalog result code, see OEConstants.CAT_RESULT_XXXX.

	// public int get_cat_result_code ();		// inherited


	// Construct a string that summarizes the catalog contents.
	// This displays the catalog size and generation count,
	// parameters, and info for each generation.

	// public default String summary_and_gen_list_string();		// inherited

	// Construct a string that dumps the entire catalog contents.
	// Caution: This can be very large!

	// public default String dump_to_string();		// inherited


	//----- Methods for catalog construction -----

	// Begin construction of a catalog.
	// Parameters:
	//  cat_params = Parameters to use for this catalog.
	// This method clears the internal data structures and sets up
	// an empty catalog with zero generations.
	// Note: This allows re-using a catalog object to generate a new catalog.
	// Note: This function does not retain cat_params; it copies the contents.

	public void begin_catalog (OECatalogParams cat_params);

	// End construction of a catalog.

	public void end_catalog ();

	// Begin a new generation of a catalog.
	// Parameters:
	//  gen_info = Structure containing the generation information to set.
	// This method increments the number of generations, and creates a
	// new empty generation.
	// Note: This function does not retain gen_info; it copies the contents.

	public void begin_generation (OEGenerationInfo gen_info);

	// End a generation of a catalog.

	public void end_generation ();

	// Add a rupture to the current generation of a catalog.
	// Parameters:
	//  rup = Structure containing the rupture information to set.
	// Note: Ruptures can only be added to the generation currently being built.
	// Note: This function does not retain rup; it copies the contents.

	public void add_rup (OERupture rup);

	// Set the time at which the catalog stops.
	// Defaults to HUGE_TIME_DAYS if it is never set.
	// If stop_time < cat_params.tend, then the catalog ended before the full time interval.

	public void set_cat_stop_time (double stop_time);

	// Set the catalog result code, CAT_RESULT_OK indicates success.
	// Defaults to CAT_RESULT_OK if it is never set.

	public void set_cat_result_code (int result_code);

}
