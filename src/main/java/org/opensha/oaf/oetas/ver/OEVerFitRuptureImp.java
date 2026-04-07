package org.opensha.oaf.oetas.ver;

import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.oetas.fit.OEDisc2History;


// Simple implementation of a rupture for fitting verification.
// Author: Michael Barall.

public class OEVerFitRuptureImp implements OEVerFitRupture {

	//----- Values -----

	// The rupture time, in days.
	// This time should be used for interactions between two ruptures.

	private double rup_t_day;

	// Get the rupture magnitude.

	private double rup_mag;

	// Get the rupture magnitude of completeness.

	private double rup_mc;

	// Get the rupture x and y coordinates, in km.

	private double rup_x_km;
	private double rup_y_km;

	// The interval index aligned with this rupture, which is:
	// - The number of intervals that lie entirely before this rupture.
	// - The index+1 of the last interval that lies before this rupture.
	// - The index of the first interval, any part of which lies after this rupture.
	// - If rup_is_interior is false, the index of the first interval that lies after this rupture.
	// - If rup_is_interior is true, the index of the interval that contains this rupture.
	// - If rup_is_interior is true, the index-1 of the first interval that lies after this rupture.
	// Note this value is not exposed through OEVerFitRupture.

	private int rup_aligned_int;

	// True if this rupture is interior to the interval identified by rup_aligned_int.
	// Note this value is not exposed through OEVerFitRupture.

	private boolean rup_is_interior;

	// Get the rupture time aligned with intervals, in days.
	// This time should be used for interactions between a rupture and an interval.
	// This time should be the same as or very close to get_rup_t_day();
	// If this rupture lies at an interval endpoint, then rup_aligned_time equals the
	// ending time of the prior interval and the beginning time of the succeeding interval.

	private double rup_aligned_time;

	// True if this rupture is categorized as a mainshock or foreshock.

	private boolean rup_is_main;

	// An index number for the rupture.
	// The index ranges from 0 to one less than the number of ruptures in the history.
	// This can be used for access to working storage per-rupture.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each rupture has a different index.
	
	private int rup_index;




	//----- Access -----

	// Get the rupture time, in days.
	// This time should be used for interactions between two ruptures.

	@Override
	public double get_rup_t_day () {
		return rup_t_day;
	}

	// Get the rupture magnitude.

	@Override
	public double get_rup_mag () {
		return rup_mag;
	}

	// Get the rupture magnitude of completeness.

	@Override
	public double get_rup_mc () {
		return rup_mc;
	}

	// Get the rupture x and y coordinates, in km.

	@Override
	public double get_rup_x_km () {
		return rup_x_km;
	}

	@Override
	public double get_rup_y_km () {
		return rup_y_km;
	}

	// Get the interval index aligned with this rupture.
	// Note this value is not exposed through OEVerFitRupture.

	public int get_rup_aligned_int () {
		return rup_aligned_int;
	}

	// Return true if this rupture is interior to the interval identified by rup_aligned_int.
	// Note this value is not exposed through OEVerFitRupture.

	public boolean get_rup_is_interior () {
		return rup_is_interior;
	}

	// Get the rupture time aligned with intervals, in days.
	// This time should be used for interactions between a rupture and an interval.
	// This time should be the same as or very close to get_rup_t_day();

	@Override
	public double get_rup_aligned_time () {
		return rup_aligned_time;
	}

	// Return true if this rupture is categorized as a mainshock or foreshock.

	@Override
	public boolean get_rup_is_main () {
		return rup_is_main;
	}

	// Compare this rupture to the given interval.
	// Returns:
	//  < 0 if the rupture is before the interval (including at the start time of the interval).
	//  = 0 if the rupture is interior to the interval.
	//  > 0 if the rupture is after the interval (including at the end time of the interval).

	@Override
	public int compare_to_interval (OEVerFitInterval interval) {
		return compare_to_interval (interval.get_int_index());
	}

	// Compare this rupture to the given interval, specified by its index.
	// Returns:
	//  < 0 if the rupture is before the interval (including at the start time of the interval).
	//  = 0 if the rupture is interior to the interval.
	//  > 0 if the rupture is after the interval (including at the end time of the interval).
	// Note this function is not exposed through OEVerFitRupture.

	public final int compare_to_interval (int it) {
		if (it < rup_aligned_int) {
			return 1;
		}
		if (rup_is_interior && it == rup_aligned_int) {
			return 0;
		}
		return -1;
	}




	//----- Storage -----

	// Get an index number for the rupture.
	// The index ranges from 0 to one less than the number of ruptures in the history.
	// This can be used for access to working storage per-rupture.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each rupture has a different index.
	
	@Override
	public int get_rup_index () {
		return rup_index;
	}




	//----- Display -----

	// Get a one-line display of the rupture, not ending in newline.

	@Override
	public String one_line_string () {
		return String.format (
			"%d: t=%.5f, mag=%.3f, mc=%.3f, x=%.3f, y=%.3f, ai=%d, ii=%b, at=%.5f, im=%b",
			rup_index,
			rup_t_day,
			rup_mag,
			rup_mc,
			rup_x_km,
			rup_y_km,
			rup_aligned_int,
			rup_is_interior,
			rup_aligned_time,
			rup_is_main
		);
	}




	//----- Construction -----

	// Construct from the given values.

	public OEVerFitRuptureImp (
		double rup_t_day,
		double rup_mag,
		double rup_mc,
		double rup_x_km,
		double rup_y_km,
		int rup_aligned_int,
		boolean rup_is_interior,
		double rup_aligned_time,
		boolean rup_is_main,
		int rup_index
	) {
		this.rup_t_day = rup_t_day;
		this.rup_mag = rup_mag;
		this.rup_mc = rup_mc;
		this.rup_x_km = rup_x_km;
		this.rup_y_km = rup_y_km;
		this.rup_aligned_int = rup_aligned_int;
		this.rup_is_interior = rup_is_interior;
		this.rup_aligned_time = rup_aligned_time;
		this.rup_is_main = rup_is_main;
		this.rup_index = rup_index;
	}


	// Construct from the given rupture object and values.
	// It is assumed that the k_prod field contains the magnitude of completeness.

	public OEVerFitRuptureImp (
		OERupture rup,
		int rup_aligned_int,
		boolean rup_is_interior,
		double rup_aligned_time,
		boolean rup_is_main,
		int rup_index
	) {
		this (
			rup.t_day,
			rup.rup_mag,
			rup.k_prod,
			rup.x_km,
			rup.y_km,
			rup_aligned_int,
			rup_is_interior,
			rup_aligned_time,
			rup_is_main,
			rup_index
		);
	}


	// Construct by obtaining the n-th rupture from an OEDisc2History.
	// Parameters:
	//  rup_index = Index number of the rupture.
	//  disc_history = Discretized history.
	//  main_rup_begin = Beginning index of ruptures to treat as mainshock/foreshock.
	//  main_rup_end = Ending+1 index of ruptures to treat as mainshock/foreshock.

	public OEVerFitRuptureImp (
		int rup_index,
		OEDisc2History disc_history,
		int main_rup_begin,
		int main_rup_end
	) {
		this (
			disc_history.a_rupture_obj[rup_index],
			disc_history.a_rupture_int_time_index[rup_index],
			disc_history.a_rupture_int_is_interior[rup_index],
			disc_history.a_rupture_int_time_value[rup_index],
			(rup_index >= main_rup_begin && rup_index < main_rup_end),
			rup_index
		);
	}


	// Make an array of rupture objects from an OEDisc2History.
	// Parameters:
	//  disc_history = Discretized history.
	//  main_rup_begin = Beginning index of ruptures to treat as mainshock/foreshock.
	//  main_rup_end = Ending+1 index of ruptures to treat as mainshock/foreshock.
	// Returns an array of length disc_history.rupture_count.

	public static OEVerFitRuptureImp[] make_array (
		OEDisc2History disc_history,
		int main_rup_begin,
		int main_rup_end
	) {
		int rupture_count = disc_history.rupture_count;
		OEVerFitRuptureImp[] result = new OEVerFitRuptureImp[rupture_count];
		for (int rup_index = 0; rup_index < rupture_count; ++rup_index) {
			result[rup_index] = new OEVerFitRuptureImp (rup_index, disc_history, main_rup_begin, main_rup_end);
			if (rup_index > 0) {
				check_interval_order (result[rup_index - 1], result[rup_index]);
			}
		}
		return result;
	}


	// Check two ruptures to see if the interval containment is correctly ordered.
	// Throw an exception if not.

	private static void check_interval_order (OEVerFitRuptureImp rup1, OEVerFitRuptureImp rup2) {
		if (!( rup1.rup_aligned_int < rup2.rup_aligned_int
			|| (rup1.rup_aligned_int == rup2.rup_aligned_int && (!rup1.rup_is_interior || rup2.rup_is_interior))
		)) {
			throw new InvariantViolationException ("OEVerFitRuptureImp.check_interval_order: Interval containment out-of-order:\n" + rup1.one_line_string() + "\n" + rup2.one_line_string() + "\n");
		}
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEVerFitRuptureImp");







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
