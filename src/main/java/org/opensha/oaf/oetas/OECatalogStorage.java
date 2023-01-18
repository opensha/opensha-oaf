package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store an Operational ETAS catalog.
// Author: Michael Barall 11/29/2019.
//
// Per-generation information is stored in a set of arrays of primitive type.
// The arrays are expanded if the number of generations exceeds the array size;
// however, this is unlikely to happen.
//
// Per-rupture information is stored in two-dimensional arrays of primitive type.
// Each second-level array is called a block.  Blocks are of fixed size.
// More blocks are added as needed;  the first-level array is expanded if
// the required number of blocks exceeds the array size.  Some values are stored
// as float rather than double to conserve memory.
//
// Design notes: It is not feasible to use object-per-rupture storage (such as
// a list of OERupture objects) due to the large number of ruptures that must be
// held in memory, which may range into many millions of ruptures.  This storage
// scheme reduces the number of Java objects to about one per 1000 ruptures.

public class OECatalogStorage implements OECatalogBuilder {

	//----- Per-Catalog storage -----

	// Parameters for this catalog.

	private OECatalogParams cat_params;

	// Time at which the catalog stops, defaults to HUGE_TIME_DAYS.

	private double cat_stop_time;

	// Catalog result code, defaults to CAT_RESULT_OK.

	private int cat_result_code;


	// Initialize the per-catlog storage, except the catalog parameters.

	private void init_cat () {
		cat_stop_time = OEConstants.HUGE_TIME_DAYS;
		cat_result_code = OEConstants.CAT_RESULT_OK;
		return;
	}


	// Re-initialize the per-catlog storage, except the catalog parameters.

	private void re_init_cat () {
		cat_stop_time = OEConstants.HUGE_TIME_DAYS;
		cat_result_code = OEConstants.CAT_RESULT_OK;
		return;
	}




	//----- Per-Generation storage -----

	// The current number of generations.

	private int gen_count;

	// The capacity of the per-generation arrays.

	private int gen_capacity;

	// The initial capacity.

	private static final int INIT_GEN_CAPACITY = 128;

	// Per-generation array containing the initial rupture number for the generation.

	private int[] gen_start;

	// Per-generation array containing the number of ruptures in the generation.

	private int[] gen_size;

	// Per-generation array containing the number of ruptures in the generation before the stop time.

	private int[] gen_valid_size;

	// Per-generation array containing the minimum magnitude for the generation.

	private double[] gen_mag_min;

	// Per-generation array containing the maximum magnitude for the generation.

	private double[] gen_mag_max;


	// Initialize the per-generation storage.

	private void init_gen () {
		gen_count = 0;
		gen_capacity = INIT_GEN_CAPACITY;
		gen_start = new int[INIT_GEN_CAPACITY];
		gen_size = new int[INIT_GEN_CAPACITY];
		gen_valid_size = new int[INIT_GEN_CAPACITY];
		gen_mag_min = new double[INIT_GEN_CAPACITY];
		gen_mag_max = new double[INIT_GEN_CAPACITY];
		return;
	}


	// Re-initialize the per-generation storage.

	private void re_init_gen () {
		gen_count = 0;
		return;
	}


	// Ensure there is sufficient capacity for per-generation storage.
	// Allocates additional storage, if needed, so that there is sufficient
	// storage to hold gen_count generations.
	// Newly-allocated storage is not initialized in any way.

	private void ensure_capacity_gen () {

		// If insufficient capacity for the number of generations needed ...

		if (gen_count > gen_capacity) {
			
			// Get the capacity we need

			do {
				gen_capacity = gen_capacity * 2;
			} while (gen_count > gen_capacity);

			// Re-allocate the arrays at the required size

			gen_start = Arrays.copyOf (gen_start, gen_capacity);
			gen_size = Arrays.copyOf (gen_size, gen_capacity);
			gen_valid_size = Arrays.copyOf (gen_valid_size, gen_capacity);
			gen_mag_min = Arrays.copyOf (gen_mag_min, gen_capacity);
			gen_mag_max = Arrays.copyOf (gen_mag_max, gen_capacity);
		}

		return;
	}




	//----- Per-Rupture storage -----

	// The current total number of ruptures.

	private int rup_count;

	// The current total number of ruptures before the stop time.

	private int rup_valid_count;

	// The number of blocks currently allocated.

	private int rup_block_count;

	// The capacity of the per-rupture arrays, in blocks.

	private int rup_block_capacity;

	// The block size, block size minus 1, block mask, block shift count, and initial block count.

	private static final int RUP_BLOCK_SIZE = 0x2000;
	private static final int RUP_BLOCK_SIZE_MINUS_1 = 0x1FFF;
	private static final int RUP_BLOCK_MASK = 0x1FFF;
	private static final int RUP_BLOCK_SHIFT = 13;

	private static final int INIT_RUP_BLOCK_COUNT = 16;

	// Rupture time, in days.
	// Note: This is double (rather than float) to allow sufficient precision so
	// that times can be relative to the epoch (Jan 1, 1970).

	private double[][] t_day;

	// Rupture magnitude.

	private float[][] rup_mag;

	// Productivity "k" value.

	private float[][] k_prod;

	// The parent rupture number, relative to the start of the prior generation.

	private int[][] rup_parent;

	// The x coordinate, in km.

	private float[][] x_km;

	// The y coordinate, in km.

	private float[][] y_km;


	// Initialize the per-rupture storage.

	private void init_rup () {
		rup_count = 0;
		rup_valid_count = 0;
		rup_block_count = 0;
		rup_block_capacity = INIT_RUP_BLOCK_COUNT;
		t_day = new double[INIT_RUP_BLOCK_COUNT][];
		rup_mag = new float[INIT_RUP_BLOCK_COUNT][];
		k_prod = new float[INIT_RUP_BLOCK_COUNT][];
		rup_parent = new int[INIT_RUP_BLOCK_COUNT][];
		x_km = new float[INIT_RUP_BLOCK_COUNT][];
		y_km = new float[INIT_RUP_BLOCK_COUNT][];
		return;
	}


	// Re-initialize the per-rupture storage.

	private void re_init_rup () {
		rup_count = 0;
		rup_valid_count = 0;
		return;
	}


	// Ensure there is sufficient capacity for per-rupture storage.
	// Allocates additional storage, if needed, so that there is sufficient
	// storage to hold rup_count ruptures.
	// Newly-allocated storage is not initialized in any way.

	private void ensure_capacity_rup () {

		// The number of blocks needed

		int blocks_needed = (rup_count + RUP_BLOCK_SIZE_MINUS_1) >> RUP_BLOCK_SHIFT;

		// If insufficient blocks ...

		if (blocks_needed > rup_block_count) {

			// If insufficient capacity for the number of blocks needed ...

			if (blocks_needed > rup_block_capacity) {
			
				// Get the capacity we need

				do {
					rup_block_capacity = rup_block_capacity * 2;
				} while (blocks_needed > rup_block_capacity);

				// Re-allocate the top-level arrays at the required size

				t_day = Arrays.copyOf (t_day, rup_block_capacity);
				rup_mag = Arrays.copyOf (rup_mag, rup_block_capacity);
				k_prod = Arrays.copyOf (k_prod, rup_block_capacity);
				rup_parent = Arrays.copyOf (rup_parent, rup_block_capacity);
				x_km = Arrays.copyOf (x_km, rup_block_capacity);
				y_km = Arrays.copyOf (y_km, rup_block_capacity);
			}

			// Allocate the additional blocks needed

			do {
				t_day[rup_block_count] = new double[RUP_BLOCK_SIZE];
				rup_mag[rup_block_count] = new float[RUP_BLOCK_SIZE];
				k_prod[rup_block_count] = new float[RUP_BLOCK_SIZE];
				rup_parent[rup_block_count] = new int[RUP_BLOCK_SIZE];
				x_km[rup_block_count] = new float[RUP_BLOCK_SIZE];
				y_km[rup_block_count] = new float[RUP_BLOCK_SIZE];

				++rup_block_count;
			} while (blocks_needed > rup_block_count);
		}

		return;
	}




	//----- Construction -----




	// Clear to default values, and perform initial memory allocation.
	// Any previously-allocated memory is discarded.

	public void clear () {
		cat_params.clear();
		init_cat();
		init_gen();
		init_rup();
		return;
	}




	// Default constructor.

	public OECatalogStorage () {
		cat_params = new OECatalogParams();
		clear();
	}




	// Re-initialize to an empty catalog.
	// Previously-allocated memory is retained and re-used.

	public void re_init () {
		re_init_cat();
		re_init_gen();
		re_init_rup();
		return;
	}




	//----- Implementation of OECatalogView -----




	// Get parameters for the catalog.
	// Parameters:
	//  cat_params = Structure to receive the catalog parameters.

	@Override
	public void get_cat_params (OECatalogParams cat_params) {
		cat_params.copy_from (this.cat_params);
		return;
	}




	// Get the total number of ruptures in the catalog.

	@Override
	public int size () {
		return rup_count;
	}




	// Get the total number of ruptures in the catalog, excluding seed ruptures.

	@Override
	public int etas_size () {
		if (gen_count < 1) {
			return rup_count;
		}
		return rup_count - gen_size[0];
	}




	// Get the total number of ruptures in the catalog before the stop time.
	// This cannot be called until after the catalog is fully built.

	@Override
	public int valid_size () {
		return rup_valid_count;
	}




	// Get the number of generations in the catalog.

	@Override
	public int get_gen_count () {
		return gen_count;
	}




	// Get the number of ruptures in the i-th generation.
	// Parameters:
	//  i_gen = Generation number.

	@Override
	public int get_gen_size (int i_gen) {
		return gen_size[i_gen];
	}




	// Get the number of ruptures in the i-th generation before the stop time.
	// Parameters:
	//  i_gen = Generation number.
	// This cannot be called until after the catalog is fully built.

	@Override
	public int get_gen_valid_size (int i_gen) {
		return gen_valid_size[i_gen];
	}




	// Get information about the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	@Override
	public void get_gen_info (int i_gen, OEGenerationInfo gen_info) {
		gen_info.set (
			gen_mag_min[i_gen],
			gen_mag_max[i_gen]
		);
		return;
	}




	// Get the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.

	@Override
	public void get_rup_full (int i_gen, int j_rup, OERupture rup) {
		int index = gen_start[i_gen] + j_rup;

		int block = index >> RUP_BLOCK_SHIFT;
		int offset = index & RUP_BLOCK_MASK;

		rup.set (
			t_day[block][offset],
			(double)(rup_mag[block][offset]),
			(double)(k_prod[block][offset]),
			rup_parent[block][offset],
			(double)(x_km[block][offset]),
			(double)(y_km[block][offset])
		);

		return;
	}




	// Get the time of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day.
	// Other fields may or may not be modified.

	@Override
	public void get_rup_time (int i_gen, int j_rup, final OERupture rup) {
		int index = gen_start[i_gen] + j_rup;

		rup.t_day = t_day[index >> RUP_BLOCK_SHIFT][index & RUP_BLOCK_MASK];

		return;
	}




	// Get the time and productivity of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day and rup.k_prod.
	// Other fields may or may not be modified.

	@Override
	public void get_rup_time_prod (int i_gen, int j_rup, final OERupture rup) {
		int index = gen_start[i_gen] + j_rup;

		int block = index >> RUP_BLOCK_SHIFT;
		int offset = index & RUP_BLOCK_MASK;

		rup.t_day = t_day[block][offset];
		rup.k_prod = (double)(k_prod[block][offset]);

		return;
	}




	// Get the time and location of the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.
	// This function fills in rup.t_day, rup.x_km, and rup.y_km.
	// Other fields may or may not be modified.

	@Override
	public void get_rup_time_x_y (int i_gen, int j_rup, final OERupture rup) {
		int index = gen_start[i_gen] + j_rup;

		int block = index >> RUP_BLOCK_SHIFT;
		int offset = index & RUP_BLOCK_MASK;

		rup.t_day = t_day[block][offset];
		rup.x_km = (double)(x_km[block][offset]);
		rup.y_km = (double)(y_km[block][offset]);

		return;
	}




	// Get the time at which the catalog stops.
	// The return value need not satisfy stop_time <= cat_params.tend; however,
	// the catalog does not extend past cat_params.tend regardless of stop_time.
	// If stop_time < cat_params.tend, then the catalog ended before the full time interval.

	@Override
	public double get_cat_stop_time () {
		return cat_stop_time;
	}




	// Get the catalog result code, see OEConstants.CAT_RESULT_XXXX.

	@Override
	public int get_cat_result_code () {
		return cat_result_code;
	}





	//----- Implementation of OECatalogBuilder -----




	// Begin construction of a catalog.
	// Parameters:
	//  cat_params = Parameters to use for this catalog.
	// This method clears the internal data structures and sets up
	// an empty catalog with zero generations.
	// Note: This allows re-using a catalog object to generate a new catalog.
	// Note: This function does not retain cat_params; it copies the contents.

	@Override
	public void begin_catalog (OECatalogParams cat_params) {
	
		// Re-initialize, re-using existing memory

		re_init();

		// Save the parameters

		this.cat_params.copy_from (cat_params);
		return;
	}




	// End construction of a catalog.

	@Override
	public void end_catalog () {

		// Remove any trailing zero-size generations, but not the seed generation

		while (gen_count > 1 && gen_size[gen_count - 1] == 0) {
			--gen_count;
		}

		// Assume all ruptures are valid

		rup_valid_count = rup_count;

		// If stop time is before end time, then the catalog ended early ...

		if (cat_stop_time < cat_params.tend) {

			// Initialize count

			rup_valid_count = 0;

			// Loop over generations

			for (int i_gen = 0; i_gen < gen_count; ++i_gen) {

				// Initialize count for this generation

				int count = 0;

				// Index for start of generation

				int start_index = gen_start[i_gen];

				int start_block = start_index >> RUP_BLOCK_SHIFT;
				int start_offset = start_index & RUP_BLOCK_MASK;

				// Index for end of generation

				int end_index = gen_start[i_gen] + gen_size[i_gen];

				int end_block = end_index >> RUP_BLOCK_SHIFT;
				int end_offset = end_index & RUP_BLOCK_MASK;

				// Loop over blocks within generation

				for (int block = start_block; block <= end_block; ++block) {

					// Loop over entries within block

					int lo = ((block > start_block) ? 0 : start_offset);
					int hi = ((block < end_block) ? RUP_BLOCK_SIZE : end_offset);
					for (int offset = lo; offset < hi; ++offset) {

						// If before stop time, count it

						if (t_day[block][offset] < cat_stop_time) {
							++count;
						}
					}
				}

				// Save count

				gen_valid_size[i_gen] = count;
				rup_valid_count += count;
			}
		}

		return;
	}




	// Begin a new generation of a catalog.
	// Parameters:
	//  gen_info = Structure containing the generation information to set.
	// This method increments the number of generations, and creates a
	// new empty generation.
	// Note: This function does not retain gen_info; it copies the contents.

	@Override
	public void begin_generation (OEGenerationInfo gen_info) {

		// Get the index of the new generation

		int i_gen = gen_count;

		// Count the new generation, and allocate storage if needed

		++gen_count;
		ensure_capacity_gen();

		// Initialize rupture start index, and zero size

		gen_start[i_gen] = rup_count;
		gen_size[i_gen] = 0;
		gen_valid_size[i_gen] = 0;

		// Save generation information

		gen_mag_min[i_gen] = gen_info.gen_mag_min;
		gen_mag_max[i_gen] = gen_info.gen_mag_max;

		return;
	}




	// End a generation of a catalog.

	@Override
	public void end_generation () {

		// Get the index of the new generation

		int i_gen = gen_count - 1;

		// Assume valid size equals size

		gen_valid_size[i_gen] = gen_size[i_gen];
		return;
	}




	// Add a rupture to the current generation of a catalog.
	// Parameters:
	//  rup = Structure containing the rupture information to set.
	// Note: Ruptures can only be added to the generation currently being built.
	// Note: This function does not retain rup; it copies the contents.

	@Override
	public void add_rup (OERupture rup) {

		// Get the index of the new rupture, and break it into block and offset

		int index = rup_count;

		int block = index >> RUP_BLOCK_SHIFT;
		int offset = index & RUP_BLOCK_MASK;

		// Count the new rupture, and allocate storage if needed

		++rup_count;
		ensure_capacity_rup();

		// Count this rupture in the current generation

		gen_size[gen_count - 1]++;

		// Save rupture information

		t_day[block][offset] = rup.t_day;
		rup_mag[block][offset] = (float)(rup.rup_mag);
		k_prod[block][offset] = (float)(rup.k_prod);
		rup_parent[block][offset] = rup.rup_parent;
		x_km[block][offset] = (float)(rup.x_km);
		y_km[block][offset] = (float)(rup.y_km);

		return;
	}




	// Set the time at which the catalog stops.
	// Defaults to HUGE_TIME_DAYS if it is never set.
	// If stop_time < cat_params.tend, then the catalog ended before the full time interval.

	@Override
	public void set_cat_stop_time (double stop_time) {
		cat_stop_time = stop_time;
		return;
	}




	// Set the catalog result code, CAT_RESULT_OK indicates success.
	// Defaults to CAT_RESULT_OK if it is never set.

	@Override
	public void set_cat_result_code (int result_code) {
		cat_result_code = result_code;
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 75001;

	private static final String M_VERSION_NAME = "OECatalogStorage";

	// Marshal a per-generation array.

	private void marshal_gen_array (MarshalWriter writer, String name, int[] x) {
		writer.marshalArrayBegin (name, gen_count);
		for (int i = 0; i < gen_count; ++i) {
			writer.marshalInt (null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	private void marshal_gen_array (MarshalWriter writer, String name, double[] x) {
		writer.marshalArrayBegin (name, gen_count);
		for (int i = 0; i < gen_count; ++i) {
			writer.marshalDouble (null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a per-generation array (assumes storage is pre-allocated)

	private void unmarshal_gen_array (MarshalReader reader, String name, int[] x) {
		int n = reader.unmarshalArrayBegin (name);
		if (n != gen_count) {
			throw new MarshalException ("Array length mismatch: name = " + name + ", got = " + n + ", expected = " + gen_count);
		}
		for (int i = 0; i < gen_count; ++i) {
			x[i] = reader.unmarshalInt (null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	private void unmarshal_gen_array (MarshalReader reader, String name, double[] x) {
		int n = reader.unmarshalArrayBegin (name);
		if (n != gen_count) {
			throw new MarshalException ("Array length mismatch: name = " + name + ", got = " + n + ", expected = " + gen_count);
		}
		for (int i = 0; i < gen_count; ++i) {
			x[i] = reader.unmarshalDouble (null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	// Marshal a per-rupture array.

	private void marshal_rup_array (MarshalWriter writer, String name, int[][] x) {
		writer.marshalArrayBegin (name, rup_count);
		for (int i = 0; i < rup_count; ++i) {
			writer.marshalInt (null, x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	private void marshal_rup_array (MarshalWriter writer, String name, double[][] x) {
		writer.marshalArrayBegin (name, rup_count);
		for (int i = 0; i < rup_count; ++i) {
			writer.marshalDouble (null, x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	private void marshal_rup_array (MarshalWriter writer, String name, float[][] x) {
		writer.marshalArrayBegin (name, rup_count);
		for (int i = 0; i < rup_count; ++i) {
			writer.marshalFloat (null, x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a per-rupture array (assumes storage is pre-allocated)

	private void unmarshal_rup_array (MarshalReader reader, String name, int[][] x) {
		int n = reader.unmarshalArrayBegin (name);
		if (n != rup_count) {
			throw new MarshalException ("Array length mismatch: name = " + name + ", got = " + n + ", expected = " + rup_count);
		}
		for (int i = 0; i < rup_count; ++i) {
			x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK] = reader.unmarshalInt (null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	private void unmarshal_rup_array (MarshalReader reader, String name, double[][] x) {
		int n = reader.unmarshalArrayBegin (name);
		if (n != rup_count) {
			throw new MarshalException ("Array length mismatch: name = " + name + ", got = " + n + ", expected = " + rup_count);
		}
		for (int i = 0; i < rup_count; ++i) {
			x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK] = reader.unmarshalDouble (null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	private void unmarshal_rup_array (MarshalReader reader, String name, float[][] x) {
		int n = reader.unmarshalArrayBegin (name);
		if (n != rup_count) {
			throw new MarshalException ("Array length mismatch: name = " + name + ", got = " + n + ", expected = " + rup_count);
		}
		for (int i = 0; i < rup_count; ++i) {
			x[i >> RUP_BLOCK_SHIFT][i & RUP_BLOCK_MASK] = reader.unmarshalFloat (null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			cat_params.marshal (writer, "cat_params");

			writer.marshalDouble ("cat_stop_time", cat_stop_time);
			writer.marshalInt ("cat_result_code", cat_result_code);

			writer.marshalInt ("gen_count", gen_count);

			marshal_gen_array (writer, "gen_start"     , gen_start     );
			marshal_gen_array (writer, "gen_size"      , gen_size      );
			marshal_gen_array (writer, "gen_valid_size", gen_valid_size);
			marshal_gen_array (writer, "gen_mag_min"   , gen_mag_min   );
			marshal_gen_array (writer, "gen_mag_max"   , gen_mag_max   );

			writer.marshalInt ("rup_count"      , rup_count      );
			writer.marshalInt ("rup_valid_count", rup_valid_count);

			marshal_rup_array (writer, "t_day"      , t_day      );
			marshal_rup_array (writer, "rup_mag"    , rup_mag    );
			marshal_rup_array (writer, "k_prod"     , k_prod     );
			marshal_rup_array (writer, "rup_parent" , rup_parent );
			marshal_rup_array (writer, "x_km"       , x_km       );
			marshal_rup_array (writer, "y_km"       , y_km       );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			re_init();

			cat_params.unmarshal (reader, "cat_params");

			cat_stop_time = reader.unmarshalDouble ("cat_stop_time");
			cat_result_code = reader.unmarshalInt ("cat_result_code");

			gen_count = reader.unmarshalInt ("gen_count");
			ensure_capacity_gen();

			unmarshal_gen_array (reader, "gen_start"     , gen_start     );
			unmarshal_gen_array (reader, "gen_size"      , gen_size      );
			unmarshal_gen_array (reader, "gen_valid_size", gen_valid_size);
			unmarshal_gen_array (reader, "gen_mag_min"   , gen_mag_min   );
			unmarshal_gen_array (reader, "gen_mag_max"   , gen_mag_max   );

			rup_count       = reader.unmarshalInt ("rup_count"      );
			rup_valid_count = reader.unmarshalInt ("rup_valid_count");
			ensure_capacity_rup();

			unmarshal_rup_array (reader, "t_day"      , t_day      );
			unmarshal_rup_array (reader, "rup_mag"    , rup_mag    );
			unmarshal_rup_array (reader, "k_prod"     , k_prod     );
			unmarshal_rup_array (reader, "rup_parent" , rup_parent );
			unmarshal_rup_array (reader, "x_km"       , x_km       );
			unmarshal_rup_array (reader, "y_km"       , y_km       );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OECatalogStorage unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogStorage catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogStorage static_unmarshal (MarshalReader reader, String name) {
		OECatalogStorage catalog = new OECatalogStorage();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Truncate a rupture to what is expected to be returned from storage.
	// This is a test function.

	public static void test_trunc_rup_as_if_stored (OERupture src, OERupture dest) {
		float rup_mag_f = (float)(src.rup_mag);
		float k_prod_f = (float)(src.k_prod);
		float x_km_f = (float)(src.x_km);
		float y_km_f = (float)(src.y_km);

		dest.t_day = src.t_day;
		dest.rup_mag = (double)(rup_mag_f);
		dest.k_prod = (double)(k_prod_f);
		dest.rup_parent = src.rup_parent;
		dest.x_km = (double)(x_km_f);
		dest.y_km = (double)(y_km_f);

		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OECatalogStorage : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  test_gen_count  test_gen_size
		// Build a catalog with test_gen_count generations.
		// Each generation has a mean of test_gen_size ruptures, but varies randomly.
		// Ruptures are generated randomly.
		// Then, scan the catalog and compare to the data used to build it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OECatalogStorage : Invalid 'test1' subcommand");
				return;
			}

			try {

				int test_gen_count = Integer.parseInt (args[1]);
				int test_gen_size = Integer.parseInt (args[2]);

				// Say hello

				System.out.println ("Generating catalog with random data");
				System.out.println ("test_gen_count = " + test_gen_count);
				System.out.println ("test_gen_size = " + test_gen_size);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Input catalog parameters

				OECatalogParams in_cat_params = (new OECatalogParams()).set_to_random (rangen);
				System.out.println ();
				System.out.println (in_cat_params.toString());

				// Begin the catalog

				cat_storage.begin_catalog (in_cat_params);

				// Input parameters for size, generation size, generation info, and rupture

				int in_size = 0;
				int[] in_gen_size = new int[test_gen_count];
				OEGenerationInfo[] in_gen_info = new OEGenerationInfo[test_gen_count];
				OERupture[][] in_rup = new OERupture[test_gen_count][];

				// Loop over generations

				for (int i_gen = 0; i_gen < test_gen_count; ++i_gen) {
				
					// Select a size for this generation

					in_gen_size[i_gen] = rangen.uniform_int_sample (test_gen_size/2 + 1, 3*test_gen_size/2 + 1);
					in_size += in_gen_size[i_gen];
					in_rup[i_gen] = new OERupture[in_gen_size[i_gen]];

					// Input generation info

					in_gen_info[i_gen] = (new OEGenerationInfo()).set_to_random (rangen);

					if (i_gen < 5) {
						System.out.println ();
						System.out.println (in_gen_info[i_gen].one_line_string (i_gen, in_gen_size[i_gen]));
					}

					// Begin the generation

					cat_storage.begin_generation (in_gen_info[i_gen]);

					// Loop over ruptures

					for (int j_rup = 0; j_rup < in_gen_size[i_gen]; ++j_rup) {
					
						// Input rupture

						in_rup[i_gen][j_rup] = (new OERupture()).set_to_random (rangen);

						if (i_gen < 5 && j_rup < 10) {
							System.out.println (in_rup[i_gen][j_rup].one_line_string (j_rup));
						}

						// Insert rupture into catalog

						cat_storage.add_rup (in_rup[i_gen][j_rup]);
					}

					// End the generation

					cat_storage.end_generation();
				}

				// End the catalog

				cat_storage.end_catalog();

				// Begin catalog check

				System.out.println ();
				System.out.println ("Checking catalog...");

				// Structures for output and comparison

				OECatalogParams out_cat_params = new OECatalogParams();
				int out_size;
				int out_gen_count;
				int out_gen_size;
				OEGenerationInfo out_gen_info = new OEGenerationInfo();
				OERupture out_rup = new OERupture();
				OERupture cmp_rup = new OERupture();

				// Error count

				int err_count = 0;

				// Check catalog parameters

				cat_storage.get_cat_params (out_cat_params);
				if (!( out_cat_params.check_param_equal(in_cat_params) )) {
					System.out.println ("MISMATCH for catalog parameters");
					System.out.println ("Expected: " + in_cat_params.toString());
					System.out.println ("Got: " + out_cat_params.toString());
					++err_count;
				}

				// Check catalog total size

				out_size = cat_storage.size();
				if (!( out_size == in_size )) {
					System.out.println ("MISMATCH for catalog total size");
					System.out.println ("Expected: " + in_size);
					System.out.println ("Got: " + out_size);
					++err_count;
				}

				// Check catalog generation count

				out_gen_count = cat_storage.get_gen_count();
				if (!( out_gen_count == test_gen_count )) {
					System.out.println ("MISMATCH for catalog generation count");
					System.out.println ("Expected: " + test_gen_count);
					System.out.println ("Got: " + out_gen_count);
					++err_count;
				}

				// Loop over generations

				for (int i_gen = 0; i_gen < test_gen_count; ++i_gen) {

					// Check catalog generation size

					out_gen_size = cat_storage.get_gen_size (i_gen);
					if (!( out_gen_size == in_gen_size[i_gen] )) {
						System.out.println ("MISMATCH for generation " + i_gen + " size");
						System.out.println ("Expected: " + in_gen_size[i_gen]);
						System.out.println ("Got: " + out_gen_size);
						++err_count;
					}

					// Check catalog generation information

					cat_storage.get_gen_info (i_gen, out_gen_info);	
					if (!( out_gen_info.check_gen_equal(in_gen_info[i_gen]) )) {
						System.out.println ("MISMATCH for generation " + i_gen + " information");
						System.out.println ("Expected: " + in_gen_info[i_gen].one_line_string());
						System.out.println ("Got: " + out_gen_info.one_line_string());
						++err_count;
					}

					if (err_count >= 10) {
						System.out.println ("Early termination, error count = " + err_count);
						return;
					}

					// Loop over ruptures

					for (int j_rup = 0; j_rup < in_gen_size[i_gen]; ++j_rup) {

						// Check catalog rupture

						cat_storage.get_rup_full (i_gen, j_rup, out_rup);
						test_trunc_rup_as_if_stored (in_rup[i_gen][j_rup], cmp_rup);
						if (!( out_rup.check_rup_equal(cmp_rup) )) {
							System.out.println ("MISMATCH for generation " + i_gen + " rupture " + j_rup);
							System.out.println ("Original: " + in_rup[i_gen][j_rup].one_line_string());
							System.out.println ("Expected: " + cmp_rup.one_line_string());
							System.out.println ("Got: " + out_rup.one_line_string());
							++err_count;
							if (err_count >= 10) {
								System.out.println ("Early termination, error count = " + err_count);
								return;
							}
						}
					}
				}

				// Final result

				System.out.println ();
				System.out.println ("Error count = " + err_count);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  test_gen_count  test_gen_size
		// Build a catalog with test_gen_count generations.
		// Each generation has a mean of test_gen_size ruptures, but varies randomly.
		// Ruptures are generated randomly.
		// Then, display the catalog summary and generation list.
		// Then, scan the catalog and compare to the data used to build it.
		// Note: Same as test #1 with the addition of the catalog summary and generation list,
		// and with generation size selected using a Poisson distribution.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OECatalogStorage : Invalid 'test2' subcommand");
				return;
			}

			try {

				int test_gen_count = Integer.parseInt (args[1]);
				int test_gen_size = Integer.parseInt (args[2]);

				// Say hello

				System.out.println ("Generating catalog with random data");
				System.out.println ("test_gen_count = " + test_gen_count);
				System.out.println ("test_gen_size = " + test_gen_size);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Allocate the storage

				OECatalogStorage cat_storage = new OECatalogStorage();

				// Input catalog parameters

				OECatalogParams in_cat_params = (new OECatalogParams()).set_to_random (rangen);
				System.out.println ();
				System.out.println (in_cat_params.toString());

				// Begin the catalog

				cat_storage.begin_catalog (in_cat_params);

				// Input parameters for size, generation size, generation info, and rupture

				int in_size = 0;
				int[] in_gen_size = new int[test_gen_count];
				OEGenerationInfo[] in_gen_info = new OEGenerationInfo[test_gen_count];
				OERupture[][] in_rup = new OERupture[test_gen_count][];

				// Loop over generations

				for (int i_gen = 0; i_gen < test_gen_count; ++i_gen) {
				
					// Select a size for this generation

					//in_gen_size[i_gen] = rangen.uniform_int_sample (test_gen_size/2 + 1, 3*test_gen_size/2 + 1);
					in_gen_size[i_gen] = rangen.poisson_sample ((double)test_gen_size);

					in_size += in_gen_size[i_gen];
					in_rup[i_gen] = new OERupture[in_gen_size[i_gen]];

					// Input generation info

					in_gen_info[i_gen] = (new OEGenerationInfo()).set_to_random (rangen);

					if (i_gen < 5) {
						System.out.println ();
						System.out.println (in_gen_info[i_gen].one_line_string (i_gen, in_gen_size[i_gen]));
					}

					// Begin the generation

					cat_storage.begin_generation (in_gen_info[i_gen]);

					// Loop over ruptures

					for (int j_rup = 0; j_rup < in_gen_size[i_gen]; ++j_rup) {
					
						// Input rupture

						in_rup[i_gen][j_rup] = (new OERupture()).set_to_random (rangen);

						if (i_gen < 5 && j_rup < 10) {
							System.out.println (in_rup[i_gen][j_rup].one_line_string (j_rup));
						}

						// Insert rupture into catalog

						cat_storage.add_rup (in_rup[i_gen][j_rup]);
					}

					// End the generation

					cat_storage.end_generation();
				}

				// End the catalog

				cat_storage.end_catalog();

				// Display catalog summary and generation list

				System.out.println ();
				System.out.println ("Catalog summary...");
				System.out.println ();
				System.out.println (cat_storage.summary_and_gen_list_string());

				// Begin catalog check

				System.out.println ();
				System.out.println ("Checking catalog...");

				// Structures for output and comparison

				OECatalogParams out_cat_params = new OECatalogParams();
				int out_size;
				int out_gen_count;
				int out_gen_size;
				OEGenerationInfo out_gen_info = new OEGenerationInfo();
				OERupture out_rup = new OERupture();
				OERupture cmp_rup = new OERupture();

				// Error count

				int err_count = 0;

				// Check catalog parameters

				cat_storage.get_cat_params (out_cat_params);
				if (!( out_cat_params.check_param_equal(in_cat_params) )) {
					System.out.println ("MISMATCH for catalog parameters");
					System.out.println ("Expected: " + in_cat_params.toString());
					System.out.println ("Got: " + out_cat_params.toString());
					++err_count;
				}

				// Check catalog total size

				out_size = cat_storage.size();
				if (!( out_size == in_size )) {
					System.out.println ("MISMATCH for catalog total size");
					System.out.println ("Expected: " + in_size);
					System.out.println ("Got: " + out_size);
					++err_count;
				}

				// Check catalog generation count

				out_gen_count = cat_storage.get_gen_count();
				if (!( out_gen_count == test_gen_count )) {
					System.out.println ("MISMATCH for catalog generation count");
					System.out.println ("Expected: " + test_gen_count);
					System.out.println ("Got: " + out_gen_count);
					++err_count;
				}

				// Loop over generations

				for (int i_gen = 0; i_gen < test_gen_count; ++i_gen) {

					// Check catalog generation size

					out_gen_size = cat_storage.get_gen_size (i_gen);
					if (!( out_gen_size == in_gen_size[i_gen] )) {
						System.out.println ("MISMATCH for generation " + i_gen + " size");
						System.out.println ("Expected: " + in_gen_size[i_gen]);
						System.out.println ("Got: " + out_gen_size);
						++err_count;
					}

					// Check catalog generation information

					cat_storage.get_gen_info (i_gen, out_gen_info);	
					if (!( out_gen_info.check_gen_equal(in_gen_info[i_gen]) )) {
						System.out.println ("MISMATCH for generation " + i_gen + " information");
						System.out.println ("Expected: " + in_gen_info[i_gen].one_line_string());
						System.out.println ("Got: " + out_gen_info.one_line_string());
						++err_count;
					}

					if (err_count >= 10) {
						System.out.println ("Early termination, error count = " + err_count);
						return;
					}

					// Loop over ruptures

					for (int j_rup = 0; j_rup < in_gen_size[i_gen]; ++j_rup) {

						// Check catalog rupture

						cat_storage.get_rup_full (i_gen, j_rup, out_rup);
						test_trunc_rup_as_if_stored (in_rup[i_gen][j_rup], cmp_rup);
						if (!( out_rup.check_rup_equal(cmp_rup) )) {
							System.out.println ("MISMATCH for generation " + i_gen + " rupture " + j_rup);
							System.out.println ("Original: " + in_rup[i_gen][j_rup].one_line_string());
							System.out.println ("Expected: " + cmp_rup.one_line_string());
							System.out.println ("Got: " + out_rup.one_line_string());
							++err_count;
							if (err_count >= 10) {
								System.out.println ("Early termination, error count = " + err_count);
								return;
							}
						}
					}
				}

				// Final result

				System.out.println ();
				System.out.println ("Error count = " + err_count);

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
