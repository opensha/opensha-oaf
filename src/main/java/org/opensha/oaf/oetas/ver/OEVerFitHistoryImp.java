package org.opensha.oaf.oetas.ver;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.oetas.fit.OEDisc2History;
import org.opensha.oaf.oetas.fit.OEMagCompFnDisc;
import org.opensha.oaf.oetas.fit.OEDiscFGHParams;

// Imports for smoke test

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.fit.OEGridOptions;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;
import org.opensha.oaf.oetas.bay.OEBayPriorVerFit;
import org.opensha.oaf.oetas.bay.OEBayPriorValue;
import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayFactoryVerFit;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.util.OEValueElement;


// Simple implementation of a history for fitting verification.
// Author: Michael Barall.
//
// A history contains:
// - A set of source ruptures (each represented by OEVerFitRupture), which can act
//   as sources of seismicity.
// - A set of target ruptures (each represented by OEVerFitRupture), which can
//   participate in the calculation of log-likelihood.
// - A set of intervals (each represented by OEVerFitInterval), which can act as
//   sources of seismicity and also participate in the calculation of log-likelihood;
//   each interval is both source and target.
// - A catalog magnitude of completeness.
//
// This implementation is intended for a history originally created by OEDisc2History.
// Accordingly, there is a single array of ruptures and a single array of intervals.
// The set of source ruptures, the subset of source ruptures that are mainshock/foreshock,
// and the set of target ruptures are each a contiguous portion of the rupture array.
// The set of source/target intervals is a contiguous portion of the interval array.

public class OEVerFitHistoryImp implements OEVerFitHistory {

	//----- Values -----

	// Catalog magnitude of completeness.

	private double magCat;

	// The ruptures.
	// Ruptures are stored in order of increasing time.

	private OEVerFitRuptureImp[] a_rupture_list;

	// The intervals.
	// Intervals are stored in order of increasing time.

	private OEVerFitIntervalImp[] a_interval_list;

	// Likelihood rupture range, the beginning and ending+1 of ruptures to include in likelihood calculation.
	// This is also the beginning and ending+1 of ruptures to use as targets.

	private int like_rup_begin;
	private int like_rup_end;

	// Likelihood interval range, the beginning and ending+1 of intervals to include in likelihood calculation.
	// These intervals are used as both sources and targets.

	private int like_int_begin;
	private int like_int_end;

	// Source rupture range, the beginning and ending+1 of ruptures to use as sources for the intensity function.
	// Requires  source_rup_begin <= like_rup_begin  and  source_rup_end <= like_rup_end.
	// Typically main_rup_end == like_rup_begin.

	private int source_rup_begin;
	private int source_rup_end;

	// Mainshock rupture range, the beginning and ending+1 of ruptures to use 'ams' as productivity.
	// Requires main_rup_begin <= main_rup_end <= like_rup_begin <= like_rup_end.
	// Typically main_rup_end == like_rup_begin.

	private int main_rup_begin;
	private int main_rup_end;




	//----- Access -----

	// Get the catalog magnitude of completeness.

	@Override
	public double get_hist_mag_cat () {
		return magCat;
	}

	// Get the number of ruptures.
	// Rupture indexes can range from 0 to one less than the number of ruptures.

	@Override
	public int get_rupture_count () {
		return a_rupture_list.length;
	}

	// Get the number of intervals.
	// Interval indexes can range from 0 to one less than the number of intervals.

	@Override
	public int get_interval_count () {
		return a_interval_list.length;
	}




	//----- Scanning -----


	// Call the supplied consumer once for each rupture that appears in, and is a target for, the likelihood calculation.
	// A rupture that lies exactly at the beginning or end of the fitting interval is not included.

	@Override
	public void for_each_like_rupture (Consumer<OEVerFitRupture> rup_consumer) {
		for (int ir = like_rup_begin; ir < like_rup_end; ++ir) {
			rup_consumer.accept (a_rupture_list[ir]);
		}
		return;
	}


	// Call the supplied consumer once for each rupture that can be a source for any likelihood calculation.
	// This includes ruptures before and during the fitting interval.

	@Override
	public void for_each_source_rupture (Consumer<OEVerFitRupture> rup_consumer) {
		for (int ir = source_rup_begin; ir < source_rup_end; ++ir) {
			rup_consumer.accept (a_rupture_list[ir]);
		}
		return;
	}


	// Call the supplied consumer once for each interval that appears in, and is a target for, the likelihood calculation.

	@Override
	public void for_each_like_interval (Consumer<OEVerFitInterval> int_consumer) {
		for (int ii = like_int_begin; ii < like_int_end; ++ii) {
			int_consumer.accept (a_interval_list[ii]);
		}
		return;
	}


	//--- Scanning sources for a given target


	// Call the supplied consumer once for each rupture that is prior to, and a source for, the given target rupture.

	@Override
	public void for_each_source_rupture_before (Consumer<OEVerFitRupture> rup_consumer, OEVerFitRupture target_rup) {
		int rup_index = target_rup.get_rup_index();
		int ir_top = Math.min (rup_index, source_rup_end);
		for (int ir = source_rup_begin; ir < ir_top; ++ir) {
			rup_consumer.accept (a_rupture_list[ir]);
		}
		return;
	}


	// Call the supplied consumer once for each rupture that is prior to, and a source for, the given target interval.
	// Ruptures interior to target_int are included.

	@Override
	public void for_each_source_rupture_before (Consumer<OEVerFitRupture> rup_consumer, OEVerFitInterval target_int) {
		int int_index = target_int.get_int_index();
		for (int ir = source_rup_begin; ir < source_rup_end; ++ir) {
			if (int_index < a_rupture_list[ir].get_rup_aligned_int()) {
				break;
			}
			rup_consumer.accept (a_rupture_list[ir]);
		}
		return;
	}


	// Call the supplied consumer once for each likelihood interval that is prior to, and a source for, the given target rupture.
	// If target_rup is interior to an interval, that interval is not included.

	@Override
	public void for_each_source_interval_before (Consumer<OEVerFitInterval> int_consumer, OEVerFitRupture target_rup) {
		int aligned_int_index = ((OEVerFitRuptureImp)target_rup).get_rup_aligned_int();	// number of intervals entirely before the rupture
		int ii_top = Math.min (aligned_int_index, like_int_end);
		for (int ii = like_int_begin; ii < ii_top; ++ii) {
			int_consumer.accept (a_interval_list[ii]);
		}
		return;
	}


	// Call the supplied consumer once for each likelihood interval that is prior to, and a source for, the given target interval.

	@Override
	public void for_each_source_interval_before (Consumer<OEVerFitInterval> int_consumer, OEVerFitInterval target_int) {
		int int_index = target_int.get_int_index();
		int ii_top = Math.min (int_index, like_int_end);
		for (int ii = like_int_begin; ii < ii_top; ++ii) {
			int_consumer.accept (a_interval_list[ii]);
		}
		return;
	}


	//--- Scanning targets for a given source


	// Call the supplied consumer once for each rupture that is after, and a target for, the given source rupture.

	@Override
	public void for_each_target_rupture_after (Consumer<OEVerFitRupture> rup_consumer, OEVerFitRupture source_rup) {
		int rup_index = source_rup.get_rup_index();
		for (int ir = Math.max (rup_index + 1, like_rup_begin); ir < like_rup_end; ++ir) {
			rup_consumer.accept (a_rupture_list[ir]);
		}
		return;
	}
	

	// Call the supplied consumer once for each rupture that is after, and a target for, the given source interval.
	// Ruptures interior to source_int are not included.

	@Override
	public void for_each_target_rupture_after (Consumer<OEVerFitRupture> rup_consumer, OEVerFitInterval source_int) {
		int int_index = source_int.get_int_index();
		for (int ir = like_rup_begin; ir < like_rup_end; ++ir) {
			if (int_index < a_rupture_list[ir].get_rup_aligned_int()) {
				rup_consumer.accept (a_rupture_list[ir]);
			}
		}
		return;
	}


	// Call the supplied consumer once for each likelihood interval that is after, and a target for, the given source rupture.
	// If source_rup is interior to an interval, that interval is included.

	@Override
	public void for_each_target_interval_after (Consumer<OEVerFitInterval> int_consumer, OEVerFitRupture source_rup) {
		int aligned_int_index = ((OEVerFitRuptureImp)source_rup).get_rup_aligned_int();	// number of intervals entirely before the rupture
		for (int ii = Math.max (aligned_int_index, like_int_begin); ii < like_int_end; ++ii) {
			int_consumer.accept (a_interval_list[ii]);
		}
		return;
	}


	// Call the supplied consumer once for each likelihood interval that is after, and a target for, the given source interval.

	@Override
	public void for_each_target_interval_after (Consumer<OEVerFitInterval> int_consumer, OEVerFitInterval source_int) {
		int int_index = source_int.get_int_index();
		for (int ii = Math.max (int_index + 1, like_int_begin); ii < like_int_end; ++ii) {
			int_consumer.accept (a_interval_list[ii]);
		}
		return;
	}


	//--- Scanning interactions


	// Call the supplied consumers for all source-target interactions.
	// Parameters:
	//  rup_rup_action = Called for each interaction with a rupture source and rupture target.
	//  int_rup_action = Called for each interaction with an interval source and rupture target.
	//  rup_int_action = Called for each interaction with a rupture source and interval target.
	//  int_int_action = Called for each interaction with an interval source and interval target.
	//  rup_target_done = Called for each target rupture when all its target interactions are done.
	//  int_target_done = Called for each target interval when all its target interactions are done.
		// If a rupture is interior to an interval, then the rupture serves as a source for the
	// interval, but the interval is not a source for the rupture (rup_int_action is called but
	// int_rup_action is not called).
// The ordering of calls is unspecified, except as follows:
	//  * For any entity (rupture or interval) that is used as both source and target, all action calls
	//    with that entity as a target occur before any action calls with that entity as a source.
	//  * For any entity (rupture or interval) that is used as a target, the done call for that entity
	//    occurs after any action calls with that entity as a target and before any action calls with
	//    that entity as a source.  (Note the second rule implies the first rule.)

	@Override
	public void for_each_interaction (
		BiConsumer<OEVerFitRupture, OEVerFitRupture> rup_rup_action,
		BiConsumer<OEVerFitInterval, OEVerFitRupture> int_rup_action,
		BiConsumer<OEVerFitRupture, OEVerFitInterval> rup_int_action,
		BiConsumer<OEVerFitInterval, OEVerFitInterval> int_int_action,
		Consumer<OEVerFitRupture> rup_target_done,
		Consumer<OEVerFitInterval> int_target_done
	) {

		// Loop over possible targets in time order

		for (int irt = like_rup_begin, iit = like_int_begin; irt < like_rup_end || iit < like_int_end; ) {

			// If rupture is the next target ...

			if (irt < like_rup_end && iit >= Math.min (like_int_end, a_rupture_list[irt].get_rup_aligned_int())) {

				// Scan rupture sources for the target rupture

				final int irs_top = Math.min (source_rup_end, irt);
				for (int irs = source_rup_begin; irs < irs_top; ++irs) {
					rup_rup_action.accept (a_rupture_list[irs], a_rupture_list[irt]);
				}

				// Scan interval sources for the target rupture

				final int iis_top = Math.min (like_int_end, a_rupture_list[irt].get_rup_aligned_int());
				for (int iis = like_int_begin; iis < iis_top; ++iis) {
					int_rup_action.accept (a_interval_list[iis], a_rupture_list[irt]);
				}

				// Consumed this target rupture

				rup_target_done.accept (a_rupture_list[irt]);
				++irt;
			}

			// Otherwise, interval is the next target ...

			else {

				// Scan rupture sources for the target interval

				for (int irs = source_rup_begin; irs < source_rup_end; ++irs) {
					if (iit < a_rupture_list[irs].get_rup_aligned_int()) {
						break;
					}
					rup_int_action.accept (a_rupture_list[irs], a_interval_list[iit]);
				}

				// Scan interval sources for the target interval

				final int iis_top = Math.min (iit, like_int_end);	// could be simplified because here iit < like_int_end
				for (int iis = like_int_begin; iis < iis_top; ++iis) {
					int_int_action.accept (a_interval_list[iis], a_interval_list[iit]);
				}

				// Consumed this target interval

				int_target_done.accept (a_interval_list[iit]);
				++iit;
			}
		}

		return;
	}




	// Call the supplied consumers for all source-target interactions, version 2.
	// Parameters:
	//  rup_rup_action = Called for each interaction with a rupture source and rupture target.
	//  int_rup_action = Called for each interaction with an interval source and rupture target.
	//  rup_int_action = Called for each interaction with a rupture source and interval target.
	//  int_int_action = Called for each interaction with an interval source and interval target.
	//  int_target_done = Called for each target interval when all its target interactions are done.
	// If a rupture is interior to an interval, then the rupture and the interval each serve
	// as a source for the other (rup_int_action and int_rup_action are both called).
	// The ordering of calls is unspecified, except as follows:
	//  * For each interval, all calls with that interval as a target (rup_int_action and int_int_action)
	//    occur before the target done call (int_target_done).
	//  * For each interval, all calls with that interval as a source (int_rup_action and int_int_action)
	//    occur after the target done call (int_target_done).

	@Override
	public void for_each_interaction_v2 (
		BiConsumer<OEVerFitRupture, OEVerFitRupture> rup_rup_action,
		BiConsumer<OEVerFitInterval, OEVerFitRupture> int_rup_action,
		BiConsumer<OEVerFitRupture, OEVerFitInterval> rup_int_action,
		BiConsumer<OEVerFitInterval, OEVerFitInterval> int_int_action,
		Consumer<OEVerFitInterval> int_target_done
	) {

		// Loop over target intervals

		for (int iit = like_int_begin; iit < like_int_end; ++iit) {

			// Scan rupture sources for the target interval

			for (int irs = source_rup_begin; irs < source_rup_end && a_rupture_list[irs].compare_to_interval(iit) <= 0; ++irs) {
				rup_int_action.accept (a_rupture_list[irs], a_interval_list[iit]);
			}

			// Scan interval sources for the target interval

			final int iis_top = Math.min (iit, like_int_end);	// could be simplified because here iit < like_int_end
			for (int iis = like_int_begin; iis < iis_top; ++iis) {
				int_int_action.accept (a_interval_list[iis], a_interval_list[iit]);
			}

			// Consumed this target interval

			int_target_done.accept (a_interval_list[iit]);
		}

		// Loop over target ruptures

		for (int irt = like_rup_begin; irt < like_rup_end; ++irt) {

			// Scan rupture sources for the target rupture

			final int irs_top = Math.min (source_rup_end, irt);
			for (int irs = source_rup_begin; irs < irs_top; ++irs) {
				rup_rup_action.accept (a_rupture_list[irs], a_rupture_list[irt]);
			}

			// Scan interval sources for the target rupture

			final int iis_top = Math.min (like_int_end, a_rupture_list[irt].get_rup_aligned_int() + (a_rupture_list[irt].get_rup_is_interior() ? 1 : 0) );
			for (int iis = like_int_begin; iis < iis_top; ++iis) {
				int_rup_action.accept (a_interval_list[iis], a_rupture_list[irt]);
			}
		}

		return;
	}




	//----- Display -----


	// Dump the history to a string.
	// Parameters:
	//  max_ruptures = Maximum number of ruptures to display, or -1 for no limit.
	//  max_intervals = Maximum number of intervals to display, or -1 for no limit.

	public String dump_history_to_string (int max_ruptures, int max_intervals) {
		StringBuilder sb = new StringBuilder();

		sb.append ("OEVerFitHistoryImp:" + "\n");
		sb.append ("magCat = " + magCat + "\n");
		sb.append ("rupture_count = " + a_rupture_list.length + "\n");
		sb.append ("interval_count = " + a_interval_list.length + "\n");
		sb.append ("like_rup_begin = " + like_rup_begin + "\n");
		sb.append ("like_rup_end = " + like_rup_end + "\n");
		sb.append ("like_int_begin = " + like_int_begin + "\n");
		sb.append ("like_int_end = " + like_int_end + "\n");
		sb.append ("source_rup_begin = " + source_rup_begin + "\n");
		sb.append ("source_rup_end = " + source_rup_end + "\n");
		sb.append ("main_rup_begin = " + main_rup_begin + "\n");
		sb.append ("main_rup_end = " + main_rup_end + "\n");

		sb.append ("Ruptures:" + "\n");
		int ir_top = a_rupture_list.length;
		if (max_ruptures >= 0 && max_ruptures < ir_top) {
			ir_top = max_ruptures;
		}
		for (int ir = 0; ir < ir_top; ++ir) {
			sb.append (a_rupture_list[ir].one_line_string() + "\n");
		}
		if (ir_top < a_rupture_list.length) {
			sb.append (" . . ." + "\n");
		}

		sb.append ("Intervals:" + "\n");
		int ii_top = a_interval_list.length;
		if (max_intervals >= 0 && max_intervals < ii_top) {
			ii_top = max_intervals;
		}
		for (int ii = 0; ii < ii_top; ++ii) {
			sb.append (a_interval_list[ii].one_line_string() + "\n");
		}
		if (ii_top < a_interval_list.length) {
			sb.append (" . . ." + "\n");
		}

		return sb.toString();
	}




	//----- Construction -----


	// Construct from an OEDisc2History.

	public OEVerFitHistoryImp (
		OEDisc2History disc_history
	) {

		// Set the magnitude of completeness

		this.magCat = disc_history.magCat;

		// Set the likelihood rupture and interval ranges

		this.like_rup_begin = disc_history.i_inside_begin;
		this.like_rup_end = disc_history.i_inside_end;
		this.like_int_begin = 0;
		this.like_int_end = disc_history.interval_count;

		// Set the source and mainshock rupture ranges

		this.source_rup_begin = 0;
		this.source_rup_end = disc_history.i_inside_end;
		this.main_rup_begin = 0;
		this.main_rup_end = disc_history.i_inside_begin;

		// Get the ruptures

		a_rupture_list = OEVerFitRuptureImp.make_array (
			disc_history,
			main_rup_begin,
			main_rup_end
		);

		// Get the intervals

		a_interval_list = OEVerFitIntervalImp.make_array (
			disc_history
		);

	}




	//----- Testing -----




	// Calculate the expected number of rupture-rupture interactions.

	public static int test_expected_rup_rup (OEVerFitHistoryImp history) {
		int count = 0;
		for (int irs = history.source_rup_begin; irs < history.source_rup_end; ++irs) {
			for (int irt = history.like_rup_begin; irt < history.like_rup_end; ++irt) {
				if (irs < irt) {
					++count;
				}
			}
		}
		return count;
	}


	// Calculate the expected number of rupture-interval interactions.
	// This includes cases where the rupture is interior to the interval.

	public static int test_expected_rup_int (OEVerFitHistoryImp history) {
		int count = 0;
		for (int irs = history.source_rup_begin; irs < history.source_rup_end; ++irs) {
			for (int iit = history.like_int_begin; iit < history.like_int_end; ++iit) {
				if (history.a_rupture_list[irs].get_rup_aligned_int() <= iit) {
					++count;
				}
			}
		}
		return count;
	}


	// Calculate the expected number of interval-rupture interactions.
	// This excludes cases where the rupture is interior to the interval.

	public static int test_expected_int_rup (OEVerFitHistoryImp history) {
		int count = 0;
		for (int iis = history.like_int_begin; iis < history.like_int_end; ++iis) {
			for (int irt = history.like_rup_begin; irt < history.like_rup_end; ++irt) {
				if (iis < history.a_rupture_list[irt].get_rup_aligned_int()) {
					++count;
				}
			}
		}
		return count;
	}


	// Calculate the expected number of interval-rupture interactions.
	// This includes cases where the rupture is interior to the interval.

	public static int test_expected_int_rup_v2 (OEVerFitHistoryImp history) {
		int count = 0;
		for (int iis = history.like_int_begin; iis < history.like_int_end; ++iis) {
			for (int irt = history.like_rup_begin; irt < history.like_rup_end; ++irt) {
				if (iis < history.a_rupture_list[irt].get_rup_aligned_int()
					|| (iis == history.a_rupture_list[irt].get_rup_aligned_int() && history.a_rupture_list[irt].get_rup_is_interior())) {
					++count;
				}
			}
		}
		return count;
	}


	// Calculate the expected number of interval-interval interactions.

	public static int test_expected_int_int (OEVerFitHistoryImp history) {
		int count = 0;
		for (int iis = history.like_int_begin; iis < history.like_int_end; ++iis) {
			for (int iit = history.like_int_begin; iit < history.like_int_end; ++iit) {
				if (iis < iit) {
					++count;
				}
			}
		}
		return count;
	}


	// Calculate the expected number of done-rupture.

	public static int test_expected_done_rup (OEVerFitHistoryImp history) {
		return history.like_rup_end - history.like_rup_begin;
	}


	// Calculate the expected number of done-interval.

	public static int test_expected_done_int (OEVerFitHistoryImp history) {
		return history.like_int_end - history.like_int_begin;
	}




	// Class for testing the for_each_interaction function.

	private static class test_interaction {

		// Latest done rupture index, -1 if none. 

		public int latest_done_rup;

		// Latest done interval index, -1 if none. 

		public int latest_done_int;

		// Call counts

		public int count_rup_rup;
		public int count_int_rup;
		public int count_rup_int;
		public int count_int_int;

		public int count_done_rup;
		public int count_done_int;

		// Expected counts

		public int expected_rup_rup;
		public int expected_int_rup;
		public int expected_rup_int;
		public int expected_int_int;

		public int expected_done_rup;
		public int expected_done_int;

		// Most recent source and target ruptures and intervals

		public OEVerFitRupture last_rups;
		public OEVerFitRupture last_rupt;
		public OEVerFitInterval last_ints;
		public OEVerFitInterval last_intt;

		// Test result

		public boolean test_ok;
		public String test_output;

		// Interaction consumers

		BiConsumer<OEVerFitRupture, OEVerFitRupture> my_rup_rup_action = (OEVerFitRupture rups, OEVerFitRupture rupt) -> {
			last_rups = rups;
			last_rupt = rupt;
			last_ints = null;
			last_intt = null;
			++count_rup_rup;
			if (!( rups.get_rup_index() < rupt.get_rup_index() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture index out-of-order");
			}
			if (!( rups.get_rup_t_day() <= rupt.get_rup_t_day() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture time out-of-order");
			}
			if (!( rups.get_rup_aligned_time() <= rupt.get_rup_aligned_time() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture aligned time out-of-order");
			}
			if (!( rups.get_rup_index() <= latest_done_rup )) {
				throw new InvariantViolationException ("my_rup_rup_action: Source rupture too early");
			}
			if (!( rupt.get_rup_index() > latest_done_rup )) {
				throw new InvariantViolationException ("my_rup_rup_action: Target rupture too late");
			}
			return;
		};

		BiConsumer<OEVerFitInterval, OEVerFitRupture> my_int_rup_action = (OEVerFitInterval ints, OEVerFitRupture rupt) -> {
			last_rups = null;
			last_rupt = rupt;
			last_ints = ints;
			last_intt = null;
			++count_int_rup;
			if (!( ints.get_int_index() < ((OEVerFitRuptureImp)rupt).get_rup_aligned_int() )) {
				throw new InvariantViolationException ("my_int_rup_action: Interval-rupture index out-of-order");
			}
			if (!( ints.get_int_time_2() <= rupt.get_rup_aligned_time() )) {
				throw new InvariantViolationException ("my_int_rup_action: Interval-rupture aligned time out-of-order");
			}
			if (!( ints.get_int_index() <= latest_done_int )) {
				throw new InvariantViolationException ("my_int_rup_action: Source interval too early");
			}
			if (!( rupt.get_rup_index() > latest_done_rup )) {
				throw new InvariantViolationException ("my_int_rup_action: Target rupture too late");
			}
			return;
		};

		BiConsumer<OEVerFitRupture, OEVerFitInterval> my_rup_int_action = (OEVerFitRupture rups, OEVerFitInterval intt) -> {
			last_rups = rups;
			last_rupt = null;
			last_ints = null;
			last_intt = intt;
			++count_rup_int;
			if (!( ((OEVerFitRuptureImp)rups).get_rup_aligned_int() <= intt.get_int_index() )) {
				throw new InvariantViolationException ("my_rup_int_action: Rupture-interval index out-of-order");
			}
			if (!( rups.get_rup_aligned_time() < intt.get_int_time_2() )) {
				throw new InvariantViolationException ("my_rup_int_action: Rupture-interval aligned time out-of-order");
			}
			if (!( rups.get_rup_index() <= latest_done_rup )) {
				throw new InvariantViolationException ("my_rup_int_action: Source rupture too early");
			}
			if (!( intt.get_int_index() > latest_done_int )) {
				throw new InvariantViolationException ("my_rup_int_action: Target interval too late");
			}
			return;
		};

		BiConsumer<OEVerFitInterval, OEVerFitInterval> my_int_int_action = (OEVerFitInterval ints, OEVerFitInterval intt) -> {
			last_rups = null;
			last_rupt = null;
			last_ints = ints;
			last_intt = intt;
			++count_int_int;
			if (!( ints.get_int_index() < intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_int_action: Interval index out-of-order");
			}
			if (!( ints.get_int_time_2() <= intt.get_int_time_1() )) {
				throw new InvariantViolationException ("my_int_int_action: Interval time out-of-order");
			}
			if (!( ints.get_int_index() <= latest_done_int )) {
				throw new InvariantViolationException ("my_int_int_action: Source interval too early");
			}
			if (!( intt.get_int_index() > latest_done_int )) {
				throw new InvariantViolationException ("my_int_int_action: Target interval too late");
			}
			return;
		};

		// Done consumers

		Consumer<OEVerFitRupture> my_rup_target_done = (OEVerFitRupture rupt) -> {
			last_rups = null;
			last_rupt = rupt;
			last_ints = null;
			last_intt = null;
			++count_done_rup;
			if (!( latest_done_rup < rupt.get_rup_index() )) {
				throw new InvariantViolationException ("my_rup_target_done: Rupture index out-of-order");
			}
			if (!( latest_done_rup + 1 == rupt.get_rup_index() )) {
				throw new InvariantViolationException ("my_rup_target_done: Rupture index not sequential");
			}
			latest_done_rup = rupt.get_rup_index();
			return;
		};

		Consumer<OEVerFitInterval> my_int_target_done = (OEVerFitInterval intt) -> {
			last_rups = null;
			last_rupt = null;
			last_ints = null;
			last_intt = intt;
			++count_done_int;
			if (!( latest_done_int < intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_target_done: Interval index out-of-order");
			}
			if (!( latest_done_int + 1 == intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_target_done: Interval index not sequential");
			}
			latest_done_int = intt.get_int_index();
			return;
		};

		// Run tests, return true if success.

		public boolean run_test (OEVerFitHistoryImp history) {
			StringBuilder sb = new StringBuilder();

			test_ok = false;
			test_output = "";

			try {

				// Initialize for test

				latest_done_rup = history.like_rup_begin - 1;
				latest_done_int = history.like_int_begin - 1;

				count_rup_rup = 0;
				count_int_rup = 0;
				count_rup_int = 0;
				count_int_int = 0;

				count_done_rup = 0;
				count_done_int = 0;

				expected_rup_rup = test_expected_rup_rup (history);
				expected_int_rup = test_expected_int_rup (history);
				expected_rup_int = test_expected_rup_int (history);
				expected_int_int = test_expected_int_int (history);

				expected_done_rup = test_expected_done_rup (history);
				expected_done_int = test_expected_done_int (history);

				last_rups = null;
				last_rupt = null;
				last_ints = null;
				last_intt = null;

				// Scan interactions

				history.for_each_interaction (
					my_rup_rup_action,
					my_int_rup_action,
					my_rup_int_action,
					my_int_int_action,
					my_rup_target_done,
					my_int_target_done
				);

				// Check if we got expected numbers

				last_rups = null;
				last_rupt = null;
				last_ints = null;
				last_intt = null;

				if (count_rup_rup != expected_rup_rup) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of rupture-rupture interactions");
				}
				if (count_int_rup != expected_int_rup) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval-rupture interactions");
				}
				if (count_rup_int != expected_rup_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of rupture-interval interactions");
				}
				if (count_int_int != expected_int_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval-interval interactions");
				}

				if (count_done_rup != expected_done_rup) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of rupture targets");
				}
				if (count_done_int != expected_done_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval targets");
				}
				if (latest_done_rup != history.like_rup_end - 1) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get reach last rupture target");
				}
				if (latest_done_int != history.like_int_end - 1) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get reach last interval target");
				}

				// All test successful

				test_ok = true;

			}
			catch (InvariantViolationException e) {
				test_ok = false;
				sb.append ("Invariant Violation:" + "\n");
				sb.append (e.getMessage() + "\n");
				sb.append ("Stack Trace:" + "\n");
				sb.append (SimpleUtils.getStackTraceAsString(e) + "\n");
				sb.append ("Involved Entities:" + "\n");
				if (last_rups != null) {
					sb.append ("last_rups: " + last_rups.one_line_string() + "\n");
				}
				if (last_ints != null) {
					sb.append ("last_ints: " + last_ints.one_line_string() + "\n");
				}
				if (last_rupt != null) {
					sb.append ("last_rupt: " + last_rupt.one_line_string() + "\n");
				}
				if (last_intt != null) {
					sb.append ("last_intt: " + last_intt.one_line_string() + "\n");
				}
				sb.append ("\n");
			}
			catch (Exception e) {
				test_ok = false;
				sb.append ("Exception:" + "\n");
				sb.append (e.getMessage() + "\n");
				sb.append ("Stack Trace:" + "\n");
				sb.append (SimpleUtils.getStackTraceAsString(e) + "\n");
				sb.append ("\n");
			}

			// Final outputs

			sb.append ("count_rup_rup = " + count_rup_rup + ", expected_rup_rup = " + expected_rup_rup + "\n");
			sb.append ("count_int_rup = " + count_int_rup + ", expected_int_rup = " + expected_int_rup + "\n");
			sb.append ("count_rup_int = " + count_rup_int + ", expected_rup_int = " + expected_rup_int + "\n");
			sb.append ("count_int_int = " + count_int_int + ", expected_int_int = " + expected_int_int + "\n");
			sb.append ("count_done_rup = " + count_done_rup + ", expected_done_rup = " + expected_done_rup + "\n");
			sb.append ("count_done_int = " + count_done_int + ", expected_done_int = " + expected_done_int + "\n");
			sb.append ("latest_done_rup = " + latest_done_rup + "\n");
			sb.append ("latest_done_int = " + latest_done_int + "\n");
			sb.append ("test_ok = " + test_ok + "\n");

			test_output = sb.toString();
			return test_ok;
		}
	}




	// Class for testing the for_each_interaction_v2 function.

	private static class test_interaction_v2 {

		// Latest done interval index, -1 if none. 

		public int latest_done_int;

		// Call counts

		public int count_rup_rup;
		public int count_int_rup;
		public int count_rup_int;
		public int count_int_int;

		public int count_done_int;

		// Expected counts

		public int expected_rup_rup;
		public int expected_int_rup;
		public int expected_rup_int;
		public int expected_int_int;

		public int expected_done_int;

		// Most recent source and target ruptures and intervals

		public OEVerFitRupture last_rups;
		public OEVerFitRupture last_rupt;
		public OEVerFitInterval last_ints;
		public OEVerFitInterval last_intt;

		// Test result

		public boolean test_ok;
		public String test_output;

		// Interaction consumers

		BiConsumer<OEVerFitRupture, OEVerFitRupture> my_rup_rup_action = (OEVerFitRupture rups, OEVerFitRupture rupt) -> {
			last_rups = rups;
			last_rupt = rupt;
			last_ints = null;
			last_intt = null;
			++count_rup_rup;
			if (!( rups.get_rup_index() < rupt.get_rup_index() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture index out-of-order");
			}
			if (!( rups.get_rup_t_day() <= rupt.get_rup_t_day() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture time out-of-order");
			}
			if (!( rups.get_rup_aligned_time() <= rupt.get_rup_aligned_time() )) {
				throw new InvariantViolationException ("my_rup_rup_action: Rupture aligned time out-of-order");
			}
			return;
		};

		BiConsumer<OEVerFitInterval, OEVerFitRupture> my_int_rup_action = (OEVerFitInterval ints, OEVerFitRupture rupt) -> {
			last_rups = null;
			last_rupt = rupt;
			last_ints = ints;
			last_intt = null;
			++count_int_rup;
			if ( ints.get_int_index() == ((OEVerFitRuptureImp)rupt).get_rup_aligned_int() && ((OEVerFitRuptureImp)rupt).get_rup_is_interior() ) {
				// Tests for rupture interior to interval
				if (!( rupt.compare_to_interval(ints) == 0 )) {
					throw new InvariantViolationException ("my_int_rup_action: Comparison does not show rupture interior to interval");
				}
				if (!( ints.get_int_time_1() < rupt.get_rup_aligned_time() && rupt.get_rup_aligned_time() < ints.get_int_time_2() )) {
					throw new InvariantViolationException ("my_int_rup_action: Aligned time not inside interval");
				}
			} else {
				// Test for rupture exterior to interval
				if (!( rupt.compare_to_interval(ints) > 0 )) {
					throw new InvariantViolationException ("my_int_rup_action: Comparison does not show rupture after interval");
				}
				if (!( ints.get_int_index() < ((OEVerFitRuptureImp)rupt).get_rup_aligned_int() )) {
					throw new InvariantViolationException ("my_int_rup_action: Interval-rupture index out-of-order");
				}
				if (!( ints.get_int_time_2() <= rupt.get_rup_aligned_time() )) {
					throw new InvariantViolationException ("my_int_rup_action: Interval-rupture aligned time out-of-order");
				}
			}
			if (!( ints.get_int_index() <= latest_done_int )) {
				throw new InvariantViolationException ("my_int_rup_action: Source interval too early");
			}
			return;
		};

		BiConsumer<OEVerFitRupture, OEVerFitInterval> my_rup_int_action = (OEVerFitRupture rups, OEVerFitInterval intt) -> {
			last_rups = rups;
			last_rupt = null;
			last_ints = null;
			last_intt = intt;
			++count_rup_int;
			if ( intt.get_int_index() == ((OEVerFitRuptureImp)rups).get_rup_aligned_int() && ((OEVerFitRuptureImp)rups).get_rup_is_interior() ) {
				// Tests for rupture interior to interval
				if (!( rups.compare_to_interval(intt) == 0 )) {
					throw new InvariantViolationException ("my_rup_int_action: Comparison does not show rupture interior to interval");
				}
				if (!( intt.get_int_time_1() < rups.get_rup_aligned_time() && rups.get_rup_aligned_time() < intt.get_int_time_2() )) {
					throw new InvariantViolationException ("my_rup_int_action: Aligned time not inside interval");
				}
			} else {
				// Test for rupture exterior to interval
				if (!( rups.compare_to_interval(intt) < 0 )) {
					throw new InvariantViolationException ("my_rup_int_action: Comparison does not show rupture before interval");
				}
				if (!( ((OEVerFitRuptureImp)rups).get_rup_aligned_int() <= intt.get_int_index() )) {
					throw new InvariantViolationException ("my_rup_int_action: Rupture-interval index out-of-order");
				}
				if (!( rups.get_rup_aligned_time() <= intt.get_int_time_1() )) {
					throw new InvariantViolationException ("my_rup_int_action: Rupture-interval aligned time out-of-order");
				}
			}
			if (!( intt.get_int_index() > latest_done_int )) {
				throw new InvariantViolationException ("my_rup_int_action: Target interval too late");
			}
			return;
		};

		BiConsumer<OEVerFitInterval, OEVerFitInterval> my_int_int_action = (OEVerFitInterval ints, OEVerFitInterval intt) -> {
			last_rups = null;
			last_rupt = null;
			last_ints = ints;
			last_intt = intt;
			++count_int_int;
			if (!( ints.get_int_index() < intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_int_action: Interval index out-of-order");
			}
			if (!( ints.get_int_time_2() <= intt.get_int_time_1() )) {
				throw new InvariantViolationException ("my_int_int_action: Interval time out-of-order");
			}
			if (!( ints.get_int_index() <= latest_done_int )) {
				throw new InvariantViolationException ("my_int_int_action: Source interval too early");
			}
			if (!( intt.get_int_index() > latest_done_int )) {
				throw new InvariantViolationException ("my_int_int_action: Target interval too late");
			}
			return;
		};

		// Done consumers

		Consumer<OEVerFitInterval> my_int_target_done = (OEVerFitInterval intt) -> {
			last_rups = null;
			last_rupt = null;
			last_ints = null;
			last_intt = intt;
			++count_done_int;
			if (!( latest_done_int < intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_target_done: Interval index out-of-order");
			}
			if (!( latest_done_int + 1 == intt.get_int_index() )) {
				throw new InvariantViolationException ("my_int_target_done: Interval index not sequential");
			}
			latest_done_int = intt.get_int_index();
			return;
		};

		// Run tests, return true if success.

		public boolean run_test (OEVerFitHistoryImp history) {
			StringBuilder sb = new StringBuilder();

			test_ok = false;
			test_output = "";

			try {

				// Initialize for test

				latest_done_int = history.like_int_begin - 1;

				count_rup_rup = 0;
				count_int_rup = 0;
				count_rup_int = 0;
				count_int_int = 0;

				count_done_int = 0;

				expected_rup_rup = test_expected_rup_rup (history);
				expected_int_rup = test_expected_int_rup_v2 (history);
				expected_rup_int = test_expected_rup_int (history);
				expected_int_int = test_expected_int_int (history);

				expected_done_int = test_expected_done_int (history);

				last_rups = null;
				last_rupt = null;
				last_ints = null;
				last_intt = null;

				// Scan interactions

				history.for_each_interaction_v2 (
					my_rup_rup_action,
					my_int_rup_action,
					my_rup_int_action,
					my_int_int_action,
					my_int_target_done
				);

				// Check if we got expected numbers

				last_rups = null;
				last_rupt = null;
				last_ints = null;
				last_intt = null;

				if (count_rup_rup != expected_rup_rup) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of rupture-rupture interactions");
				}
				if (count_int_rup != expected_int_rup) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval-rupture interactions");
				}
				if (count_rup_int != expected_rup_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of rupture-interval interactions");
				}
				if (count_int_int != expected_int_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval-interval interactions");
				}

				if (count_done_int != expected_done_int) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get expected number of interval targets");
				}
				if (latest_done_int != history.like_int_end - 1) {
					throw new InvariantViolationException ("test_interaction.run_test: Did not get reach last interval target");
				}

				// All test successful

				test_ok = true;

			}
			catch (InvariantViolationException e) {
				test_ok = false;
				sb.append ("Invariant Violation:" + "\n");
				sb.append (e.getMessage() + "\n");
				sb.append ("Stack Trace:" + "\n");
				sb.append (SimpleUtils.getStackTraceAsString(e) + "\n");
				sb.append ("Involved Entities:" + "\n");
				if (last_rups != null) {
					sb.append ("last_rups: " + last_rups.one_line_string() + "\n");
				}
				if (last_ints != null) {
					sb.append ("last_ints: " + last_ints.one_line_string() + "\n");
				}
				if (last_rupt != null) {
					sb.append ("last_rupt: " + last_rupt.one_line_string() + "\n");
				}
				if (last_intt != null) {
					sb.append ("last_intt: " + last_intt.one_line_string() + "\n");
				}
				sb.append ("\n");
			}
			catch (Exception e) {
				test_ok = false;
				sb.append ("Exception:" + "\n");
				sb.append (e.getMessage() + "\n");
				sb.append ("Stack Trace:" + "\n");
				sb.append (SimpleUtils.getStackTraceAsString(e) + "\n");
				sb.append ("\n");
			}

			// Final outputs

			sb.append ("count_rup_rup = " + count_rup_rup + ", expected_rup_rup = " + expected_rup_rup + "\n");
			sb.append ("count_int_rup = " + count_int_rup + ", expected_int_rup = " + expected_int_rup + "\n");
			sb.append ("count_rup_int = " + count_rup_int + ", expected_rup_int = " + expected_rup_int + "\n");
			sb.append ("count_int_int = " + count_int_int + ", expected_int_int = " + expected_int_int + "\n");
			sb.append ("count_done_int = " + count_done_int + ", expected_done_int = " + expected_done_int + "\n");
			sb.append ("latest_done_int = " + latest_done_int + "\n");
			sb.append ("test_ok = " + test_ok + "\n");

			test_output = sb.toString();
			return test_ok;
		}
	}




	// Make a default maximum simulation magnitude, given a history.

	private static double make_default_ver_fit_mag_max (OEVerFitHistory history) {

		// Magnitude above magnitude of completeness

		final double[] my_mag_max = new double[1];	// array so can be accessed by lambdas
		my_mag_max[0] = history.get_hist_mag_cat() + OEConstants.DEF_FMAG_ABOVE_MAG_CAT;

		// Magnitude above source ruptures

		history.for_each_source_rupture ((OEVerFitRupture rups) -> {
			my_mag_max[0] = Math.max (my_mag_max[0], rups.get_rup_mag() + OEConstants.DEF_FMAG_ABOVE_MAG_MAX);
		});

		// Magnitude above target ruptures

		history.for_each_like_rupture ((OEVerFitRupture rupt) -> {
			my_mag_max[0] = Math.max (my_mag_max[0], rupt.get_rup_mag() + OEConstants.DEF_FMAG_ABOVE_MAG_MAX);
		});

		return my_mag_max[0];
	}




	// Make a default set of verification fitting options, given a history.

	private static OEVerFitOptions make_default_ver_fit_options (OEVerFitHistory history) {

		// History magnitude of completeness, will also be the minimum simulation magnitude

		double my_magCat = history.get_hist_mag_cat();

		// Maximum magnitude derived from the history

		double my_mag_max = make_default_ver_fit_mag_max (history);

		// Make verification fitting options using defaults for everything else

		return new OEVerFitOptions (
			OEConstants.DEF_MREF,				// mref
			OEConstants.DEF_MSUP,				// msup
			my_magCat,							// mag_min
			my_mag_max,							// mag_max
			OEConstants.DEF_F_INTERVALS,		// f_intervals
			OEConstants.DEF_C_CROSS_INTERVALS,	// c_cross_intervals
			OEConstants.DEF_C_SELF_INTERVALS,	// c_self_intervals
			OEConstants.DEF_LMR_OPT,			// lmr_opt
			OEConstants.DEF_TINT_BR_FITTING		// tint_br
		);
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEVerFitHistoryImp");




		// Subcommand : Test #1
		// Command format:
		//  test1  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then test scanning the verification history, and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.toString());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (20, 20));

			// Test scanning

			System.out.println ();
			test_interaction tester = new test_interaction();
			tester.run_test (ver_history);
			System.out.println ("Scan test:");
			System.out.println (tester.test_output);

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then test scanning the verification history, and display the results.
		// Same as test #1 except it dumps the entire history (caution: can be large).

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			// Test scanning

			System.out.println ();
			test_interaction tester = new test_interaction();
			tester.run_test (ver_history);
			System.out.println ("Scan test:");
			System.out.println (tester.test_output);

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Same as test #1 except it does not test scanning.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.toString());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (20, 20));

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Same as test #2 except it does not test scanning.
		// Same as test #3 except it dumps the entire history (caution: can be large).

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then test scanning the verification history using v2, and display the results.
		// Same as test #1 except using test scan v2.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.toString());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (20, 20));

			// Test scanning

			System.out.println ();
			test_interaction_v2 tester = new test_interaction_v2();
			tester.run_test (ver_history);
			System.out.println ("Scan test:");
			System.out.println (tester.test_output);

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then test scanning the verification history using v2, and display the results.
		// Same as test #2 except using test scan v2.
		// Same as test #5 except it dumps the entire history (caution: can be large).

		if (testargs.is_test ("test6")) {

			// Read arguments

			System.out.println ("Generating history and transferring to verification structures");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			// Test scanning

			System.out.println ();
			test_interaction_v2 tester = new test_interaction_v2();
			tester.run_test (ver_history);
			System.out.println ("Scan test:");
			System.out.println (tester.test_output);

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then perform a smoke test on the verification accumulator, calling it directly.
		// Caution: Dumps the entire history (can be large).

		if (testargs.is_test ("test7")) {

			// Read arguments

			System.out.println ("Generating history, transferring to verification structures, and performing verification smoke test with direct calls");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			// Set up accumulator

			System.out.println ();
			System.out.println ("***** Verification fitting accumulator *****");
			System.out.println ();

			boolean relative_zams = true;

			OEVerFitOptions fit_options = make_default_ver_fit_options (ver_history);
			OEGridOptions grid_options = new OEGridOptions (relative_zams);

			OEVerFitAccum fit_accum = new OEVerFitAccum();
			fit_accum.set_config (
				ver_history,
				fit_options,
				grid_options
			);

			System.out.println (fit_accum.toString());

			// Parameters for test

			double b = 1.0;
			double alpha = b;
			double c = 0.01;
			double p = 1.0;
			double n = 0.5;

			int zams_size = 11;
			double zams_min = -2.5;
			double zams_max = 2.5;
			double[] a_zams = (OEDiscreteRange.makeLinear (zams_size, zams_min, zams_max)).get_range_array();

			double zmu = 0.0;

			// Perform test

			System.out.println ();
			System.out.println ("***** Fitting results *****");
			System.out.println ();

			double[] a_log_like = new double[a_zams.length];

			fit_accum.ver_set_voxel (
				b,
				alpha,
				c,
				p,
				n
			);

			for (int i = 0; i < a_zams.length; ++i) {
				a_log_like[i] = fit_accum.ver_subvox_log_like (
					a_zams[i],
					zmu
				);
			}

			for (int i = 0; i < a_zams.length; ++i) {
				System.out.println (i + ": zams = " + a_zams[i] + ", log_like = " + a_log_like[i]);
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then perform a smoke test on the verification accumulator, calling it directly.
		// Caution: Dumps the entire history (can be large).
		// Same as test #7 except calls through the prior using single-point calls.

		if (testargs.is_test ("test8")) {

			// Read arguments

			System.out.println ("Generating history, transferring to verification structures, and performing verification smoke test with single-point prior calls");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			// Set up accumulator

			System.out.println ();
			System.out.println ("***** Verification fitting accumulator *****");
			System.out.println ();

			boolean relative_zams = true;

			OEVerFitOptions fit_options = make_default_ver_fit_options (ver_history);
			OEGridOptions grid_options = new OEGridOptions (relative_zams);

			OEVerFitAccum fit_accum = new OEVerFitAccum();
			fit_accum.set_config (
				ver_history,
				fit_options,
				grid_options
			);

			System.out.println (fit_accum.toString());

			// Make the prior

			System.out.println ();
			System.out.println ("***** Verification fitting prior *****");
			System.out.println ();

			OEBayFactory bay_factory = OEBayFactory.makeVerFit();
			OEBayFactoryParams bay_factory_params = new OEBayFactoryParams();	// unknown mainshock magnitude and location
			OEBayPrior bay_prior = bay_factory.make_bay_prior (bay_factory_params);
			((OEBayPriorVerFit)bay_prior).config_prior (fit_accum);
			OEBayPriorParams bay_prior_params = new OEBayPriorParams (
				bay_factory_params.get_mag_main(),		// mag_main
				fit_options.get_tint_br(),				// tint_br
				grid_options,							// grid_options
				fit_options.get_mag_min(),				// mag_min
				fit_options.get_mag_max()				// mag_max
			);

			// Parameters for test

			double b = 1.0;
			double alpha = b;
			double c = 0.01;
			double p = 1.0;
			double n = 0.5;

			int zams_size = 11;
			double zams_min = -2.5;
			double zams_max = 2.5;
			//double[] a_zams = (OEDiscreteRange.makeLinear (zams_size, zams_min, zams_max)).get_range_array();

			double zmu = 0.0;

			OEValueElement b_velt = new OEValueElement(b);
			OEValueElement alpha_velt = new OEValueElement(alpha);
			OEValueElement c_velt = new OEValueElement(c);
			OEValueElement p_velt = new OEValueElement(p);
			OEValueElement n_velt = new OEValueElement(n);

			OEValueElement[] a_zams_velt = (OEDiscreteRange.makeLinear (zams_size, zams_min, zams_max)).get_velt_array();

			OEValueElement zmu_velt = new OEValueElement(zmu);

			// Perform test

			System.out.println ();
			System.out.println ("***** Fitting results *****");
			System.out.println ();

			OEBayPriorValue bay_prior_value = new OEBayPriorValue();

			double[] a_log_like = new double[a_zams_velt.length];
			double[] a_vox_volume = new double[a_zams_velt.length];

			for (int i = 0; i < a_zams_velt.length; ++i) {
				bay_prior.get_bay_value (
					bay_prior_params,
					bay_prior_value,
					b_velt,
					alpha_velt,
					c_velt,
					p_velt,
					n_velt,
					a_zams_velt[i],
					zmu_velt
				);

				a_log_like[i] = bay_prior_value.log_density;
				a_vox_volume[i] = bay_prior_value.vox_volume;
			}

			for (int i = 0; i < a_zams_velt.length; ++i) {
				System.out.println (i + ": zams = " + a_zams_velt[i].get_ve_value() + ", log_like = " + a_log_like[i] + ", vox_volume = " + a_vox_volume[i]);
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         t_interval_begin  t_interval_end  before_max_count  mag_cat_int_join
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Then transfer to OEVerFitHistoryImp, and display the results.
		// Then perform a smoke test on the verification accumulator, calling it directly.
		// Caution: Dumps the entire history (can be large).
		// Same as test #7 except calls through the prior using multi-point calls.

		if (testargs.is_test ("test9")) {

			// Read arguments

			System.out.println ("Generating history, transferring to verification structures, and performing verification smoke test with multi-point prior calls");

			double magCat = testargs.get_double ("magCat");
			double capF = testargs.get_double ("capF");
			double capG = testargs.get_double ("capG");
			double capH = testargs.get_double ("capH");
			double t_range_begin = testargs.get_double ("t_range_begin");
			double t_range_end = testargs.get_double ("t_range_end");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double mag_eps = testargs.get_double ("mag_eps");
			double time_eps = testargs.get_double ("time_eps");
			double disc_base = testargs.get_double ("disc_base");
			double disc_delta = testargs.get_double ("disc_delta");
			double disc_round = testargs.get_double ("disc_round");
			double disc_gap = testargs.get_double ("disc_gap");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");

			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double division_mag = testargs.get_double ("division_mag");
			int division_count = testargs.get_int ("division_count");

			double t_interval_begin = testargs.get_double ("t_interval_begin");
			double t_interval_end = testargs.get_double ("t_interval_end");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 2, 2, "t_day", "rup_mag");

			// Make the rupture list

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OERupture.make_time_mag_list (rup_list, time_mag_array);

			System.out.println ();
			System.out.println ("rup_list:");
			for (OERupture rup : rup_list) {
				System.out.println ("  " + rup.u_time_mag_string());
			}

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the array of required splitting times

			double[] t_req_splits = new double[2];
			t_req_splits[0] = t_interval_begin;
			t_req_splits[1] = t_interval_end;

			// Make the history

			OEDiscFGHParams params = new OEDiscFGHParams();

			params.set (
				magCat,
				capF,
				capG,
				capH,
				t_range_begin,
				t_range_end,

				mag_eps,
				time_eps,
				disc_base,
				disc_delta,
				disc_round,
				disc_gap,

				mag_cat_count,
				eligible_mag,
				eligible_count,
				division_mag,
				division_count,
				split_fn,

				t_req_splits,
				before_max_count,
				mag_cat_int_join
			);

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (params, rup_list);

			System.out.println ();
			System.out.println (history.dump_string());

			// Transfer to verification structures

			OEVerFitHistoryImp ver_history = new OEVerFitHistoryImp (history);

			System.out.println ();
			System.out.println (ver_history.dump_history_to_string (-1, -1));

			// Set up accumulator

			System.out.println ();
			System.out.println ("***** Verification fitting accumulator *****");
			System.out.println ();

			boolean relative_zams = true;

			OEVerFitOptions fit_options = make_default_ver_fit_options (ver_history);
			OEGridOptions grid_options = new OEGridOptions (relative_zams);

			OEVerFitAccum fit_accum = new OEVerFitAccum();
			fit_accum.set_config (
				ver_history,
				fit_options,
				grid_options
			);

			System.out.println (fit_accum.toString());

			// Make the prior

			System.out.println ();
			System.out.println ("***** Verification fitting prior *****");
			System.out.println ();

			OEBayFactory bay_factory = OEBayFactory.makeVerFit();
			OEBayFactoryParams bay_factory_params = new OEBayFactoryParams();	// unknown mainshock magnitude and location
			OEBayPrior bay_prior = bay_factory.make_bay_prior (bay_factory_params);
			((OEBayPriorVerFit)bay_prior).config_prior (fit_accum);
			OEBayPriorParams bay_prior_params = new OEBayPriorParams (
				bay_factory_params.get_mag_main(),		// mag_main
				fit_options.get_tint_br(),				// tint_br
				grid_options,							// grid_options
				fit_options.get_mag_min(),				// mag_min
				fit_options.get_mag_max()				// mag_max
			);

			// Parameters for test

			double b = 1.0;
			double alpha = b;
			double c = 0.01;
			double p = 1.0;
			double n = 0.5;

			int zams_size = 11;
			double zams_min = -2.5;
			double zams_max = 2.5;
			//double[] a_zams = (OEDiscreteRange.makeLinear (zams_size, zams_min, zams_max)).get_range_array();

			double zmu = 0.0;

			OEValueElement b_velt = new OEValueElement(b);
			OEValueElement alpha_velt = new OEValueElement(alpha);
			OEValueElement c_velt = new OEValueElement(c);
			OEValueElement p_velt = new OEValueElement(p);
			OEValueElement n_velt = new OEValueElement(n);

			OEValueElement[] a_zams_velt = (OEDiscreteRange.makeLinear (zams_size, zams_min, zams_max)).get_velt_array();

			OEValueElement zmu_velt = new OEValueElement(zmu);
			OEValueElement[] a_zmu_velt = new OEValueElement[a_zams_velt.length];
			for (int i = 0; i < a_zams_velt.length; ++i) {
				a_zmu_velt[i] = zmu_velt;
			}

			// Perform test

			System.out.println ();
			System.out.println ("***** Fitting results *****");
			System.out.println ();

			OEBayPriorValue bay_prior_value = new OEBayPriorValue();

			double[] a_log_like = new double[a_zams_velt.length];
			double[] a_vox_volume = new double[a_zams_velt.length];

			bay_prior.get_bay_value (
				bay_prior_params,
				a_log_like,
				a_vox_volume,
				b_velt,
				alpha_velt,
				c_velt,
				p_velt,
				n_velt,
				a_zams_velt,
				a_zmu_velt
			);

			for (int i = 0; i < a_zams_velt.length; ++i) {
				System.out.println (i + ": zams = " + a_zams_velt[i].get_ve_value() + ", log_like = " + a_log_like[i] + ", vox_volume = " + a_vox_volume[i]);
			}

			return;
		}







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
