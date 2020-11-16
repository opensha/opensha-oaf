package org.opensha.oaf.oetas.fit;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.oetas.OEStatsCalc;


// Class to compute and store marginals for a grid of likelihoods.
// Author: Michael Barall 11/14/2020.

public class OEGridMarginal {

	//----- Grid description -----

	// The number of variables.
	// Note: Variables are numbered from 0 to n_vars-1.

	public int n_vars;

	// The number of discrete values for each variable.
	// Dimensionality: n_values[n_vars]
	// n_values[n] is the number of values for the n-th variable.
	// Note: The discrete values for the n-th variable are identified by indexes
	// ranging from 0 to n_values[n]-1.  Each point in the grid can be identified
	// by an integer array of length n_vars.  If such an array is called ix, then
	// the condition is that 0 <= ix[n] < n_values[n] for each n with 0 <= n < n_vars.

	public int[] n_values;

	// The minimum allowed probability.
	// Probabilities less than this are forced to zero.

	public double min_probability;

	// Default minimum probability, chosen to avoid three-digit exponents.

	public static final double DEF_MIN_PROBABILITY = 1.0e-98;

	//----- Marginals -----

	// The largest probability anywhere in the grid.
	// Note: Unnormalized probabilities are scaled so that this is 1.

	public double peak_probability;
	
	// The indexes of the grid entry where the largest probability occurs.
	// Dimensionality: peak_indexes[n_vars]
	// peak_indexes[n] is the index of the value for the n-th variable where the peak occurs.
	// Note: Any entry in the grid can be uniquely identified by giving an index
	// for each of the n variables.  If the n-th index is i, it means that the n-th variable
	// assumes its i-th value.  This list of indexes is stored in an array of length n_vars.
	// If such an array is called ix, then 0 <= ix[n] < n_values[n] for each n with
	// 0 <= n < n_vars.

	public int[] peak_indexes;

	// The total of all the probabilities in the grid.
	// Note: Normalized probabilities are scaled so that this is 1.

	public double total_probability;

	// The marginal probability distributions, for each variable.
	// Dimensionality: marginal_probability[n_vars][n_values[n]]
	// (In the above line, n is the first index, and so the array is non-rectangular).
	// marginal_probability[n][i] is the marginal probability that the n-th variable has its i-th value.
	// In other words, marginal_probability[n][i] is the sum of the probabilities of
	// all grid entries where the n-th variable has its i-th value.

	public double[][] marginal_probability;

	// The indexes for the mode of each marginal probability distribution.
	// Dimensionality: marginal_mode_index[n_vars]
	// For each n, marginal_mode_index[n] maximizes the value of marginal_probability[n][marginal_mode_index[n]].

	public int[] marginal_mode_index;

	// The largest probability of any grid entry, that has a given index for a given variable.
	// Dimensionality: marginal_peak_probability[n_vars][n_values[n]]
	// (In the above line, n is the first index, and so the array is non-rectangular).
	// marginal_peak_probability[n][i] is the maximum probability of all grid entries
	// where the n-th variable has its i-th value.
	// Note: If we identify grid entries by an array ix of length n_vars satisfying the
	// condition 0 <= ix[n] < n_values[n], then marginal_peak_probability[n][i] is
	// the largest probability of any grid entry that also satisfies ix[n] == i.

	public double[][] marginal_peak_probability;

	// The indexes of the grid entry that has the largest probability of any grid entry
	// that has a given index for a given variable.
	// Dimensionality: marginal_peak_indexes[n_vars][n_values[n]][n_vars]
	// (In the above line, n is the first index, and so the array is non-rectangular).
	// marginal_peak_indexes[n][i] is an array of length n_vars, which contains the
	// indexes of the grid entry that has the largest probability of any grid entry
	// where the n-th variable has its i-th value.
	// Note: marginal_peak_indexes[n][i] identifies the grid entry which contains
	// the value of marginal_peak_probability[n][i].
	// Note: Because of this definition, marginal_peak_indexes[n][i][n] ==  i.

	public int[][][] marginal_peak_indexes;

	// The marginal 2D probability distribution, for each pair of variables.
	// Dimensionality: marginal_2d_probability[n_vars][n_vars][n_values[n1]][n_values[n2]]
	// (In the above line, n1 is the first index and n2 is the second index, and so
	// the array is non-rectangular).
	// If n1 < n2 then marginal_2d_probability[n1][n2] is non-null; otherwise it is null.
	// This is because we only need one marginal for each unique pair of variables.
	// marginal_2d_probability[n1][n2][i1][i2] is the marginal probability that the n1-th
	// variable has its i1-th value, and the n2-th variable has its i2-th value.
	// In other words, marginal_2d_probability[n1][n2][i1][i2] is the sum of the
	// probabilities of all grid entries where the n1-th variable has it i1-th value
	// and the n2-the variable has its i2-th value.

	public double[][][][] marginal_2d_probability;




	//----- Internal functions -----




	// Allocate and initialize marginal arrays and variables.
	// Assumes that n_vars and n_values are already set up.
	// Note: Variables and array elements that contain a maximum are initialized
	// to -Double.MAX_VALUE; those that contain a sum are initialized to 0.0; and
	// those that contain an index are initialized to -1.

	private void alloc_marginal () {
	
		peak_probability = -Double.MAX_VALUE;

		peak_indexes = new int[n_vars];
		for (int n = 0; n < n_vars; ++n) {
			peak_indexes[n] = -1;
		}

		total_probability = 0.0;

		marginal_probability = new double[n_vars][];
		for (int n = 0; n < n_vars; ++n) {
			marginal_probability[n] = new double[n_values[n]];
			for (int i = 0; i < n_values[n]; ++i) {
				marginal_probability[n][i] = 0.0;
			}
		}

		marginal_mode_index = new int[n_vars];
		for (int n = 0; n < n_vars; ++n) {
			marginal_mode_index[n] = -1;
		}

		marginal_peak_probability = new double[n_vars][];
		for (int n = 0; n < n_vars; ++n) {
			marginal_peak_probability[n] = new double[n_values[n]];
			for (int i = 0; i < n_values[n]; ++i) {
				marginal_peak_probability[n][i] = -Double.MAX_VALUE;
			}
		}

		marginal_peak_indexes = new int[n_vars][][];
		for (int n = 0; n < n_vars; ++n) {
			marginal_peak_indexes[n] = new int[n_values[n]][];
			for (int i = 0; i < n_values[n]; ++i) {
				marginal_peak_indexes[n][i] = new int[n_vars];
				for (int n2 = 0; n2 < n_vars; ++n2) {
					marginal_peak_indexes[n][i][n2] = -1;
				}
			}
		}

		marginal_2d_probability = new double[n_vars][][][];
		for (int n1 = 0; n1 < n_vars; ++n1) {
			marginal_2d_probability[n1] = new double[n_vars][][];
			for (int n2 = 0; n2 < n_vars; ++n2) {
				if (n1 < n2) {
					marginal_2d_probability[n1][n2] = new double[n_values[n1]][];
					for (int i1 = 0; i1 < n_values[n1]; ++i1) {
						marginal_2d_probability[n1][n2][i1] = new double[n_values[n2]];
						for (int i2 = 0; i2 < n_values[n2]; ++i2) {
							marginal_2d_probability[n1][n2][i1][i2] = 0.0;
						}
					}
				}
				else {
					marginal_2d_probability[n1][n2] = null;
				}
			}
		}

		return;
	}




	// Finish calculating the marginals.
	// Calculate the mode for each marginal distribution.
	// Apply minimum probability to the marginals.

	private void finish_marginal () {

		// Calculate the modes

		for (int n = 0; n < n_vars; ++n) {
			int j = 0;
			double x = marginal_probability[n][0];
			for (int i = 1; i < n_values[n]; ++i) {
				if (x < marginal_probability[n][i]) {
					j = i;
					x = marginal_probability[n][i];
				}
			}
			marginal_mode_index[n] = j;
		}

		// Apply min probability to marginal

		for (int n = 0; n < n_vars; ++n) {
			for (int i = 1; i < n_values[n]; ++i) {
				if (marginal_probability[n][i] < min_probability) {
					marginal_probability[n][i] = 0.0;
				}
			}
		}

		// Apply min probability to marginal peak

		for (int n = 0; n < n_vars; ++n) {
			for (int i = 1; i < n_values[n]; ++i) {
				if (marginal_peak_probability[n][i] < min_probability) {
					marginal_peak_probability[n][i] = 0.0;
				}
			}
		}

		// Apply min probability to 2D marginal

		for (int n1 = 0; n1 < n_vars; ++n1) {
			for (int n2 = 0; n2 < n_vars; ++n2) {
				if (n1 < n2) {
					for (int i1 = 0; i1 < n_values[n1]; ++i1) {
						for (int i2 = 0; i2 < n_values[n2]; ++i2) {
							if (marginal_2d_probability[n1][n2][i1][i2] < min_probability) {
								marginal_2d_probability[n1][n2][i1][i2] = 0.0;
							}
						}
					}
				}
			}
		}
	
		return;
	}




	//----- Computations -----




	// Compute marginals from a grid of log-likelihoods.
	// Parameters:
	//  grid = Grid of log-likelihood values.
	//  f_convert = True to convert grid entries to (unnormalized) probabilities.

	public void marginals_from_log_like (double[][][][] grid, boolean f_convert) {

		// Set up 4-dimensional grid description
		
		n_vars = 4;

		final int v0 = grid.length;
		final int v1 = grid[0].length;
		final int v2 = grid[0][0].length;
		final int v3 = grid[0][0][0].length;

		n_values = new int[n_vars];
		n_values[0] = v0;
		n_values[1] = v1;
		n_values[2] = v2;
		n_values[3] = v3;

		min_probability = DEF_MIN_PROBABILITY;

		// Allocate and initialize the marginals

		alloc_marginal();

		// Get the maximum likelihood value, which is needed to convert to probabilities
	
		peak_probability = 1.0;
		final double max_like = OEStatsCalc.find_array_max (grid, peak_indexes);

		// Loop over all grid elements

		for (int i0 = 0; i0 < v0; ++i0) {
			double tot1 = 0.0;
			for (int i1 = 0; i1 < v1; ++i1) {
				double tot2 = 0.0;
				for (int i2 = 0; i2 < v2; ++i2) {
					double tot3 = 0.0;
					for (int i3 = 0; i3 < v3; ++i3) {

						// Convert grid element to probability

						double prob = Math.exp(grid[i0][i1][i2][i3] - max_like);

						if (prob < min_probability) {
							prob = 0.0;
						}

						// If converting the grid, save it back

						if (f_convert) {
							grid[i0][i1][i2][i3] = prob;
						}

						// Add to total probability

						tot3 += prob;

						// Add to the marginal probability for each variable

						marginal_probability[0][i0] += prob;
						marginal_probability[1][i1] += prob;
						marginal_probability[2][i2] += prob;
						marginal_probability[3][i3] += prob;

						// Add to the marginal peak for each variable

						if (marginal_peak_probability[0][i0] < prob) {
							marginal_peak_probability[0][i0] = prob;
							marginal_peak_indexes[0][i0][0] = i0;
							marginal_peak_indexes[0][i0][1] = i1;
							marginal_peak_indexes[0][i0][2] = i2;
							marginal_peak_indexes[0][i0][3] = i3;
						}

						if (marginal_peak_probability[1][i1] < prob) {
							marginal_peak_probability[1][i1] = prob;
							marginal_peak_indexes[1][i1][0] = i0;
							marginal_peak_indexes[1][i1][1] = i1;
							marginal_peak_indexes[1][i1][2] = i2;
							marginal_peak_indexes[1][i1][3] = i3;
						}

						if (marginal_peak_probability[2][i2] < prob) {
							marginal_peak_probability[2][i2] = prob;
							marginal_peak_indexes[2][i2][0] = i0;
							marginal_peak_indexes[2][i2][1] = i1;
							marginal_peak_indexes[2][i2][2] = i2;
							marginal_peak_indexes[2][i2][3] = i3;
						}

						if (marginal_peak_probability[3][i3] < prob) {
							marginal_peak_probability[3][i3] = prob;
							marginal_peak_indexes[3][i3][0] = i0;
							marginal_peak_indexes[3][i3][1] = i1;
							marginal_peak_indexes[3][i3][2] = i2;
							marginal_peak_indexes[3][i3][3] = i3;
						}

						// Add to the marginal 2D probability for each pair of variables

						marginal_2d_probability[0][1][i0][i1] += prob;
						marginal_2d_probability[0][2][i0][i2] += prob;
						marginal_2d_probability[0][3][i0][i3] += prob;
						marginal_2d_probability[1][2][i1][i2] += prob;
						marginal_2d_probability[1][3][i1][i3] += prob;
						marginal_2d_probability[2][3][i2][i3] += prob;
					}
					tot2 += tot3;
				}
				tot1 += tot2;
			}
			total_probability += tot1;
		}

		// Finish computing marginals

		finish_marginal();
		return;
	}




	// Compute marginals from a grid of log-likelihoods.
	// Parameters:
	//  grid = Grid of log-likelihood values.
	//  f_convert = True to convert grid entries to (unnormalized) probabilities.

	public void marginals_from_log_like (double[][][] grid, boolean f_convert) {

		// Set up 3-dimensional grid description
		
		n_vars = 3;

		final int v0 = grid.length;
		final int v1 = grid[0].length;
		final int v2 = grid[0][0].length;

		n_values = new int[n_vars];
		n_values[0] = v0;
		n_values[1] = v1;
		n_values[2] = v2;

		min_probability = DEF_MIN_PROBABILITY;

		// Allocate and initialize the marginals

		alloc_marginal();

		// Get the maximum likelihood value, which is needed to convert to probabilities
	
		peak_probability = 1.0;
		final double max_like = OEStatsCalc.find_array_max (grid, peak_indexes);

		// Loop over all grid elements

		for (int i0 = 0; i0 < v0; ++i0) {
			double tot1 = 0.0;
			for (int i1 = 0; i1 < v1; ++i1) {
				double tot2 = 0.0;
				for (int i2 = 0; i2 < v2; ++i2) {

					// Convert grid element to probability

					double prob = Math.exp(grid[i0][i1][i2] - max_like);

					if (prob < min_probability) {
						prob = 0.0;
					}

					// If converting the grid, save it back

					if (f_convert) {
						grid[i0][i1][i2] = prob;
					}

					// Add to total probability

					tot2 += prob;

					// Add to the marginal probability for each variable

					marginal_probability[0][i0] += prob;
					marginal_probability[1][i1] += prob;
					marginal_probability[2][i2] += prob;

					// Add to the marginal peak for each variable

					if (marginal_peak_probability[0][i0] < prob) {
						marginal_peak_probability[0][i0] = prob;
						marginal_peak_indexes[0][i0][0] = i0;
						marginal_peak_indexes[0][i0][1] = i1;
						marginal_peak_indexes[0][i0][2] = i2;
					}

					if (marginal_peak_probability[1][i1] < prob) {
						marginal_peak_probability[1][i1] = prob;
						marginal_peak_indexes[1][i1][0] = i0;
						marginal_peak_indexes[1][i1][1] = i1;
						marginal_peak_indexes[1][i1][2] = i2;
					}

					if (marginal_peak_probability[2][i2] < prob) {
						marginal_peak_probability[2][i2] = prob;
						marginal_peak_indexes[2][i2][0] = i0;
						marginal_peak_indexes[2][i2][1] = i1;
						marginal_peak_indexes[2][i2][2] = i2;
					}

					// Add to the marginal 2D probability for each pair of variables

					marginal_2d_probability[0][1][i0][i1] += prob;
					marginal_2d_probability[0][2][i0][i2] += prob;
					marginal_2d_probability[1][2][i1][i2] += prob;
				}
				tot1 += tot2;
			}
			total_probability += tot1;
		}

		// Finish computing marginals

		finish_marginal();
		return;
	}




	// Compute marginals from a grid of log-likelihoods.
	// Parameters:
	//  grid = Grid of log-likelihood values.
	//  f_convert = True to convert grid entries to (unnormalized) probabilities.

	public void marginals_from_log_like (double[][] grid, boolean f_convert) {

		// Set up 2-dimensional grid description
		
		n_vars = 2;

		final int v0 = grid.length;
		final int v1 = grid[0].length;

		n_values = new int[n_vars];
		n_values[0] = v0;
		n_values[1] = v1;

		min_probability = DEF_MIN_PROBABILITY;

		// Allocate and initialize the marginals

		alloc_marginal();

		// Get the maximum likelihood value, which is needed to convert to probabilities
	
		peak_probability = 1.0;
		final double max_like = OEStatsCalc.find_array_max (grid, peak_indexes);

		// Loop over all grid elements

		for (int i0 = 0; i0 < v0; ++i0) {
			double tot1 = 0.0;
			for (int i1 = 0; i1 < v1; ++i1) {

					// Convert grid element to probability

					double prob = Math.exp(grid[i0][i1] - max_like);

					if (prob < min_probability) {
						prob = 0.0;
					}

					// If converting the grid, save it back

					if (f_convert) {
						grid[i0][i1] = prob;
					}

					// Add to total probability

					tot1 += prob;

					// Add to the marginal probability for each variable

					marginal_probability[0][i0] += prob;
					marginal_probability[1][i1] += prob;

					// Add to the marginal peak for each variable

					if (marginal_peak_probability[0][i0] < prob) {
						marginal_peak_probability[0][i0] = prob;
						marginal_peak_indexes[0][i0][0] = i0;
						marginal_peak_indexes[0][i0][1] = i1;
					}

					if (marginal_peak_probability[1][i1] < prob) {
						marginal_peak_probability[1][i1] = prob;
						marginal_peak_indexes[1][i1][0] = i0;
						marginal_peak_indexes[1][i1][1] = i1;
					}

					// Add to the marginal 2D probability for each pair of variables

					marginal_2d_probability[0][1][i0][i1] += prob;
			}
			total_probability += tot1;
		}

		// Finish computing marginals

		finish_marginal();
		return;
	}




	//----- Construction -----




	// Clear to empty values.

	public void clear () {

		n_vars = 0;
		n_values = null;
		min_probability = DEF_MIN_PROBABILITY;

		peak_probability = 0.0;
		peak_indexes = null;
		total_probability = 0.0;
		marginal_probability = null;
		marginal_mode_index = null;
		marginal_peak_probability = null;
		marginal_peak_indexes = null;
		marginal_2d_probability = null;

		return;
	}




	// Default constructor.

	public OEGridMarginal () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridMarginal:" + "\n");

		result.append ("n_vars = "              + n_vars                               + "\n");
		result.append ("n_values = "            + Arrays.toString(n_values)            + "\n");
		result.append ("min_probability = "     + min_probability                      + "\n");
		result.append ("peak_probability = "    + peak_probability                     + "\n");
		result.append ("peak_indexes = "        + Arrays.toString(peak_indexes)        + "\n");
		result.append ("total_probability = "   + total_probability                    + "\n");
		result.append ("marginal_mode_index = " + Arrays.toString(marginal_mode_index) + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 94001;

	private static final String M_VERSION_NAME = "OEGridMarginal";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt           ("n_vars"                   , n_vars                   );
			writer.marshalIntArray      ("n_values"                 , n_values                 );
			writer.marshalDouble        ("min_probability"          , min_probability          );
			writer.marshalDouble        ("peak_probability"         , peak_probability         );
			writer.marshalIntArray      ("peak_indexes"             , peak_indexes             );
			writer.marshalDouble        ("total_probability"        , total_probability        );
			writer.marshalDouble2DArray ("marginal_probability"     , marginal_probability     );
			writer.marshalIntArray      ("marginal_mode_index"      , marginal_mode_index      );
			writer.marshalDouble2DArray ("marginal_peak_probability", marginal_peak_probability);
			writer.marshalInt3DArray    ("marginal_peak_indexes"    , marginal_peak_indexes    );

			for (int n1 = 0; n1 < n_vars; ++n1) {
				for (int n2 = 0; n2 < n_vars; ++n2) {
					if (n1 < n2) {
						writer.marshalDouble2DArray ("marginal_2d_probability." + n1 + "." + n2, marginal_2d_probability[n1][n2]);
					}
				}
			}

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

			n_vars                    = reader.unmarshalInt           ("n_vars"                   );
			n_values                  = reader.unmarshalIntArray      ("n_values"                 );
			min_probability           = reader.unmarshalDouble        ("min_probability"          );
			peak_probability          = reader.unmarshalDouble        ("peak_probability"         );
			peak_indexes              = reader.unmarshalIntArray      ("peak_indexes"             );
			total_probability         = reader.unmarshalDouble        ("total_probability"        );
			marginal_probability      = reader.unmarshalDouble2DArray ("marginal_probability"     );
			marginal_mode_index       = reader.unmarshalIntArray      ("marginal_mode_index"      );
			marginal_peak_probability = reader.unmarshalDouble2DArray ("marginal_peak_probability");
			marginal_peak_indexes     = reader.unmarshalInt3DArray    ("marginal_peak_indexes"    );

			marginal_2d_probability = new double[n_vars][][][];
			for (int n1 = 0; n1 < n_vars; ++n1) {
				marginal_2d_probability[n1] = new double[n_vars][][];
				for (int n2 = 0; n2 < n_vars; ++n2) {
					if (n1 < n2) {
						marginal_2d_probability[n1][n2] = reader.unmarshalDouble2DArray ("marginal_2d_probability." + n1 + "." + n2);
					}
					else {
						marginal_2d_probability[n1][n2] = null;
					}
				}
			}

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

	public OEGridMarginal unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEGridMarginal params) {
		writer.marshalMapBegin (name);
		params.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEGridMarginal static_unmarshal (MarshalReader reader, String name) {
		OEGridMarginal params = new OEGridMarginal();
		reader.unmarshalMapBegin (name);
		params.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return params;
	}




	//----- Testing -----





}
