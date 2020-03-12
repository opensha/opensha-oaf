package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


// Class to store parameters for an ensemble of operational ETAS catalogs.
// Author: Michael Barall 02/01/2020.
//
// Holds the parameters for a suite of realizations of an ETAS catalog.

public class OEEnsembleParams {

	//----- Parameters -----

	// The initializer, used to initialize each catalog.

	public OEEnsembleInitializer initializer;

	// The accumulators, used to accumulate results from the catalogs.

	public OEEnsembleAccumulator[] accumulators;

	// The number of catalogs to generate.

	public int num_catalogs;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		initializer  = null;
		accumulators = null;
		num_catalogs = 0;
		return;
	}




	// Default constructor.

	public OEEnsembleParams () {
		clear();
	}




	// Set all values.

	public OEEnsembleParams set (
		OEEnsembleInitializer initializer,
		List<OEEnsembleAccumulator> accumulators,
		int num_catalogs
	) {
		this.initializer  = initializer;
		this.accumulators = accumulators.toArray (new OEEnsembleAccumulator[0]);
		this.num_catalogs = num_catalogs;
		return this;
	}




	// Copy all values from the other object.

	public OEEnsembleParams copy_from (OEEnsembleParams other) {
		this.initializer  = other.initializer;
		this.accumulators = other.accumulators;
		this.num_catalogs = other.num_catalogs;
		return this;
	}

}
