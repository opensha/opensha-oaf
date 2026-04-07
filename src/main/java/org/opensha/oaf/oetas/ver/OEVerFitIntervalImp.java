package org.opensha.oaf.oetas.ver;

import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.fit.OEDisc2History;


// Simple implementation of an interval for fitting verification.
// Author: Michael Barall.

public class OEVerFitIntervalImp implements OEVerFitInterval {

	//----- Values -----

	// The start and end time of the interval, in days.

	private double int_time_1;
	private double int_time_2;

	// The interval magnitude of completeness.

	private double int_mc;

	// Return true if the interval magnitude of completeness is the history magCat.

	private boolean int_is_magcat;

	// An index number for the interval.
	// The index ranges from 0 to one less than the number of intervals in the history.
	// This can be used for access to working storage per-interval.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each interval has a different index.
	
	private int int_index;




	//----- Access -----

	// Get the start and end time of the interval, in days.

	@Override
	public double get_int_time_1 () {
		return int_time_1;
	}

	@Override
	public double get_int_time_2 () {
		return int_time_2;
	}

	// Get the interval duration, in days.
	// The value should be get_int_time_2() - get_int_time_1().

	@Override
	public double get_int_duration () {
		return int_time_2 - int_time_1;
	}

	// Get the interval midpoint, in days.
	// The value should be (get_int_time_2() + get_int_time_1()) / 2.0.

	@Override
	public double get_int_midpoint () {
		return (int_time_2 + int_time_1) / 2.0;
	}

	// Get the interval magnitude of completeness.

	@Override
	public double get_int_mc () {
		return int_mc;
	}

	// Return true if the interval magnitude of completeness is the history magCat.

	@Override
	public boolean get_int_is_magcat () {
		return int_is_magcat;
	}




	//----- Storage -----

	// Get an index number for the interval.
	// The index ranges from 0 to one less than the number of intervals in the history.
	// This can be used for access to working storage per-interval.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each interval has a different index.
	
	@Override
	public int get_int_index () {
		return int_index;
	}




	//----- Construction -----

	// Construct from the given values.

	public OEVerFitIntervalImp (
		double int_time_1,
		double int_time_2,
		double int_mc,
		boolean int_is_magcat,
		int int_index
	) {
		this.int_time_1 = int_time_1;
		this.int_time_2 = int_time_2;
		this.int_mc = int_mc;
		this.int_is_magcat = int_is_magcat;
		this.int_index = int_index;
	}


	// Construct by obtaining the n-th interval from an OEDisc2History.
	// Parameters:
	//  int_index = Index number of the interval.
	//  disc_history = Discretized history.

	public OEVerFitIntervalImp (
		int int_index,
		OEDisc2History disc_history
	) {
		this (
			disc_history.a_interval_time[int_index],
			disc_history.a_interval_time[int_index + 1],
			disc_history.a_interval_mc[int_index],
			disc_history.a_interval_is_magcat[int_index],
			int_index
		);
	}


	// Make an array of interval objects from an OEDisc2History.
	// Parameters:
	//  disc_history = Discretized history.
	// Returns an array of length disc_history.interval_count.

	public static OEVerFitIntervalImp[] make_array (
		OEDisc2History disc_history
	) {
		int interval_count = disc_history.interval_count;
		OEVerFitIntervalImp[] result = new OEVerFitIntervalImp[interval_count];
		for (int int_index = 0; int_index < interval_count; ++int_index) {
			result[int_index] = new OEVerFitIntervalImp (int_index, disc_history);
		}
		return result;
	}




	//----- Display -----

	// Get a one-line display of the interval, not ending in newline.

	@Override
	public String one_line_string () {
		return String.format (
			"%d: t1=%.5f, t2=%.5f, mc=%.3f, ismc=%b",
			int_index,
			int_time_1,
			int_time_2,
			int_mc,
			int_is_magcat
		);
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEVerFitIntervalImp");







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
