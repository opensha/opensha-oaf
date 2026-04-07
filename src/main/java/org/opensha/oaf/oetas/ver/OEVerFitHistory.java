package org.opensha.oaf.oetas.ver;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import org.opensha.oaf.oetas.OERupture;


// Abstraction of a history for fitting verification.
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
// A given rupture can be both source and target, or it can be one but not the other.
// A source rupture may be before the start of the first interval.
// Each source rupture is classified as either mainshock/foreshock or aftershock;
// the difference is in the productivity parameter that applies to it.
// A target rupture can be at an interval endpoint or interior to an interval.
// Each target rupture has an associated magnitude of completeness which is the magnitude
// of completeness just before the rupture occurred.
// (A source rupture also has a magnitude of completeness but it plays no role.)
//
// Intervals collectively should be a partition of the time period during which fitting occurs.
// Intervals may not overlap; they adjoin end-to-end to fill the fitting period.
// Each interval has an associated magnitude of completeness which is assumed to be constant
// within the interval, but may vary among intervals if the magnitude of completeness varies
// with time.
// Ususally, an interval can contain interior ruptures only if the interval's magnitude
// of completeness equals the catalog magnitude of completeness which in turn equals the
// minimum simulation magnitude, although the code does not require this.
//
// If extended sources are being used, then each interval has a productivity density which
// is assumed to be constant within the interval.
// The productivity density represents the productivity of unobserved earthquakes occurring
// during the interval, with magnitudes above the minimum magnitude and below the interval's
// magnitude of completeness (it is zero if the interval's magnitude of completeness is
// less than or equal to the minimum magnitude, or if extended sources are not being used).
//
// In computing the intensity function:
// - There is a contribution from each source interval that depends on its magnitude, and
//   whether it is mainshock/foreshock or aftershock.
// - There is a contribution from each interval that depends on its productivity density.
//
// In computing the log-likelihood:
// - There is a (positive, log) contribution from each target rupture that depends on the
//   intensity function and magnitude of completeness just before the rupture occurred.
// - There is a (negative) contribution from each interval that is the integral of the
//   intensity function over the duration of the interval and above the interval's
//   magnitude of completeness.
//
// To perform these computations, we recognize five types of "interactions" between
// source and target.
//
// 1. Source rupture and target rupture, where the source rupture occurs before the target
// rupture:  The source rupture contributes to the intensity function at the time of the
// target rupture.
//
// 2. Source interval and target rupture, where the rupture lies interior to or after
// the interval:  The productivity density of the source interval contributes to the
// intensity function at the time of the target rupture.  If the rupture is interior
// to the interval, then only the portion of the interval that lies before the rupture
// contributes.
//
// 3. Source rupture and target interval, where the rupture lies interior to or before
// the interval:  The rupture contributes to the integral of the intensity function
// over the interval.  If the rupture is interior to the interval, then the integral
// is taken over the part of the interval that lies after the rupture.  In addition,
// if the rupture is before the interval (not interior to it), then the rupture also
// contributes to the productivity density of the interval.  (Note: Allowing an interior
// rupture to contribute to the interval's productivity density would create a circularity
// in which the rupture induces productivity that begins before the rupture.)
//
// 4. Source interval and target interval, where the source interval is before The
// target interval: The source interval's productivity density contributes to the integral
// of the intensity function over the target interval.  In addition, the source interval's 
// productivity density contributes to the productivity density of the target interval.
//
// 5. Interval self-interaction, where an interval serves as both source and target:
// The same as an interaction between separate source and target intervals, except
// that the effect of each small part of the interval, treated as a source, is limited
// to the remainder of the interval, treated as a target.
//
// This interface provides methods for scanning interactions as well as source ruptures,
// target ruptures, and intervals.
//
// Note that some methods do not include an interaction between a source interval and a
// target rupture if the rupture is interior to the interval.  If the history follows the
// convention that an interval can contain interior ruptures only if the interval's magnitude
// of completeness equals the catalog magnitude of completeness which in turn equals the
// minimum simulation magnitude, then such interactions have no effect and so can be omitted.

public interface OEVerFitHistory {

	//----- Access -----

	// Get the catalog magnitude of completeness.

	public double get_hist_mag_cat ();

	// Get the number of ruptures.
	// Rupture indexes can range from 0 to one less than the number of ruptures.

	public int get_rupture_count ();

	// Get the number of intervals.
	// Interval indexes can range from 0 to one less than the number of intervals.

	public int get_interval_count ();




	//----- Scanning -----


	// Call the supplied consumer once for each rupture that appears in, and is a target for, the likelihood calculation.
	// A rupture that lies exactly at the beginning or end of the fitting interval is not included.

	public void for_each_like_rupture (Consumer<OEVerFitRupture> rup_consumer);


	// Call the supplied consumer once for each rupture that can be a source for any likelihood calculation.
	// This includes ruptures before and during the fitting interval.

	public void for_each_source_rupture (Consumer<OEVerFitRupture> rup_consumer);


	// Call the supplied consumer once for each interval that appears in, and is a target for, the likelihood calculation.

	public void for_each_like_interval (Consumer<OEVerFitInterval> int_consumer);


	//--- Scanning sources for a given target


	// Call the supplied consumer once for each rupture that is prior to, and a source for, the given target rupture.

	public void for_each_source_rupture_before (Consumer<OEVerFitRupture> rup_consumer, OEVerFitRupture target_rup);


	// Call the supplied consumer once for each rupture that is prior to, and a source for, the given target interval.
	// Ruptures interior to target_int are included.

	public void for_each_source_rupture_before (Consumer<OEVerFitRupture> rup_consumer, OEVerFitInterval target_int);


	// Call the supplied consumer once for each likelihood interval that is prior to, and a source for, the given target rupture.
	// If target_rup is interior to an interval, that interval is not included.

	public void for_each_source_interval_before (Consumer<OEVerFitInterval> int_consumer, OEVerFitRupture target_rup);


	// Call the supplied consumer once for each likelihood interval that is prior to, and a source for, the given target interval.

	public void for_each_source_interval_before (Consumer<OEVerFitInterval> int_consumer, OEVerFitInterval target_int);


	//--- Scanning targets for a given source


	// Call the supplied consumer once for each rupture that is after, and a target for, the given source rupture.

	public void for_each_target_rupture_after (Consumer<OEVerFitRupture> rup_consumer, OEVerFitRupture source_rup);


	// Call the supplied consumer once for each rupture that is after, and a target for, the given source interval.
	// Ruptures interior to source_int are not included.

	public void for_each_target_rupture_after (Consumer<OEVerFitRupture> rup_consumer, OEVerFitInterval source_int);


	// Call the supplied consumer once for each likelihood interval that is after, and a target for, the given source rupture.
	// If source_rup is interior to an interval, that interval is included.

	public void for_each_target_interval_after (Consumer<OEVerFitInterval> int_consumer, OEVerFitRupture source_rup);


	// Call the supplied consumer once for each likelihood interval that is after, and a target for, the given source interval.

	public void for_each_target_interval_after (Consumer<OEVerFitInterval> int_consumer, OEVerFitInterval source_int);


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

	public void for_each_interaction (
		BiConsumer<OEVerFitRupture, OEVerFitRupture> rup_rup_action,
		BiConsumer<OEVerFitInterval, OEVerFitRupture> int_rup_action,
		BiConsumer<OEVerFitRupture, OEVerFitInterval> rup_int_action,
		BiConsumer<OEVerFitInterval, OEVerFitInterval> int_int_action,
		Consumer<OEVerFitRupture> rup_target_done,
		Consumer<OEVerFitInterval> int_target_done
	);


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

	public void for_each_interaction_v2 (
		BiConsumer<OEVerFitRupture, OEVerFitRupture> rup_rup_action,
		BiConsumer<OEVerFitInterval, OEVerFitRupture> int_rup_action,
		BiConsumer<OEVerFitRupture, OEVerFitInterval> rup_int_action,
		BiConsumer<OEVerFitInterval, OEVerFitInterval> int_int_action,
		Consumer<OEVerFitInterval> int_target_done
	);


	//----- Display -----

	// Dump the history to a string.
	// Parameters:
	//  max_ruptures = Maximum number of ruptures to display, or -1 for no limit.
	//  max_intervals = Maximum number of intervals to display, or -1 for no limit.

	public String dump_history_to_string (int max_ruptures, int max_intervals);




}
