package org.opensha.oaf.rj;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;

import static org.opensha.commons.geo.GeoTools.TO_DEG;
import static org.opensha.commons.geo.GeoTools.TO_RAD;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionCircle;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.util.catalog.ObsEqkRupMaxMagComparator;

import org.opensha.oaf.comcat.ComcatOAFAccessor;



/**
 * Calculate aftershock event shadowing.
 * Author: Michael Barall 07/26/2018.
 *
 * It is generally not desireable to generate aftershock forecasts for an
 * earthquake that is an aftershock or foreshock of another, larger earthquake.
 * Such an earthquake is said to be shadowed by the larger earthquake.
 *
 * This class contains the machinery to determine if an earthquake is shadowed.
 */
public class AftershockStatsShadow {




	// This class holds a candidate shadowing event.

	public static class CandidateShadow {
	
		// The rupture.

		public ObsEqkRupture rupture;

		// The centroid radius, in kilometers.
		// Events that contribute to the centroid must lie within this distance of the hypocenter.

		public double centroid_radius;

		// The minimum magnitude for events that contribute to the centroid.

		public double centroid_min_mag;

		// The lower and upper limits of origin time for events that contribute to the centroid.
		// These are times in milliseconds since the epoch.

		public long centroid_time_lo;
		public long centroid_time_hi;

		// The The sample radius, in kilometers.
		// An event is considered to be an aftershock if it lies within this distance of the centroid.

		public double sample_radius;

		// Accumulators for vectors that define the centroid.

		public double x;
		public double y;
		public double z;

		// Computed centroid

		public Location candidate_centroid;

		// Candidate parameters: event ID, origin time, magnitude, hypocenter.

		public String candidate_event_id;
		public long candidate_time;
		public double candidate_mag;
		public Location candidate_hypo;

		// Constructor.

		public CandidateShadow (
			ObsEqkRupture rupture,
			double centroid_radius,
			double centroid_min_mag,
			long centroid_time_lo,
			long centroid_time_hi,
			double sample_radius) {
			
			this.rupture = rupture;
			this.centroid_radius = centroid_radius;
			this.centroid_min_mag = centroid_min_mag;
			this.centroid_time_lo = centroid_time_lo;
			this.centroid_time_hi = centroid_time_hi;
			this.sample_radius = sample_radius;

			this.x = 0.0;
			this.y = 0.0;
			this.z = 0.0;

			this.candidate_centroid = null;

			this.candidate_event_id = rupture.getEventId();
			this.candidate_time = rupture.getOriginTime();
			this.candidate_mag = rupture.getMag();
			this.candidate_hypo = rupture.getHypocenterLocation();

			// Accumulate the unit vector for the rupture itself (see AftershockStatsCalc.getSphCentroid)

			double lat = this.candidate_hypo.getLatRad();
			double lon = this.candidate_hypo.getLonRad();

			this.x += (Math.cos(lat) * Math.cos(lon));
			this.y += (Math.cos(lat) * Math.sin(lon));
			this.z += Math.sin(lat);
		}

		// Call Comcat to obtain a list of possible aftershocks, and accumulate their unit vectors.
		// An exception from this function likely indicates a problem with Comcat.

		public void accum_from_comcat (ComcatOAFAccessor accessor, long system_time_now, boolean f_verbose) {

			// If there is a nonempty time and space region in which we need to look for aftershocks ...

			if (centroid_time_lo < system_time_now
				&& centroid_time_lo < centroid_time_hi
				&& centroid_radius > 0.0
				&& centroid_min_mag <= 9.9) {

				// Construct a circle around the rupture hypocenter with the centroid radius

				SphLatLon candidate_sph_hypo = new SphLatLon (candidate_hypo);
				SphRegion centroid_region = SphRegion.makeCircle (candidate_sph_hypo, centroid_radius);

				// Get a list of possible aftershocks by calling Comcat
				// Aftershocks must lie in the centroid region, within the centroid times,
				// and have magnitude at least equal to the centroid magnitude

				double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

				boolean wrapLon = false;
				boolean extendedInfo = false;
				int limit_per_call = 0;
				int max_calls = 0;

				ObsEqkRupList aftershocks = accessor.fetchEventList (candidate_event_id,
							centroid_time_lo, centroid_time_hi,
							min_depth, max_depth,
							centroid_region, wrapLon, extendedInfo,
							centroid_min_mag, limit_per_call, max_calls);

				if (f_verbose) {
					System.out.println ("AftershockStatsShadow.accum_from_comcat: Found " + aftershocks.size() + " aftershocks within " + String.format ("%.3f", centroid_radius) + " km of candidate event " + candidate_event_id);
				}
		
				// For each aftershock ...

				for (ObsEqkRupture aftershock : aftershocks) {

					// Get the aftershock parameters

					Location aftershock_hypo = aftershock.getHypocenterLocation();

					// Add unit vector for centroid calculation (see AftershockStatsCalc.getSphCentroid)

					double lat = aftershock_hypo.getLatRad();
					double lon = aftershock_hypo.getLonRad();

					x += (Math.cos(lat) * Math.cos(lon));
					y += (Math.cos(lat) * Math.sin(lon));
					z += Math.sin(lat);
				}
			}

			return;
		}

		// Get the centroid, as determined by the accumulators (see AftershockStatsCalc.getSphCentroid).
		// Also save the result into candidate_centroid.

		public Location get_centroid () {

			double lat;
			double lon;

			// If the vector is very small, just return the candidate location

			if (x*x + y*y + z*z < 1.0e-4) {
				lat = candidate_hypo.getLatitude();
				lon = candidate_hypo.getLongitude();
				if (lon > 180.0) {
					lon -= 360.0;
				}
			}

			// Otherwise, convert rectangular to spherical coordinates

			else {
				lat = Math.atan2 (z, Math.hypot(x, y)) * TO_DEG;
				lon = Math.atan2 (y, x) * TO_DEG;
			}

			// Make sure the angles are in range, since they were converted from radians

			if (lat > 90.0) {lat = 90.0;}
			if (lat < -90.0) {lat = -90.0;}
			if (lon > 180.0) {lon = 180.0;}
			if (lon < -180.0) {lon = -180.0;}

			// Centroid

			candidate_centroid = new Location (lat, lon);
			return candidate_centroid;
		}
	}



	// Comparator for sorting candidates, with the "best" candidates appearing first.

	public static class CandidateComparator implements Comparator<CandidateShadow> {
	
		// Compares its two arguments for order. Returns a negative integer, zero, or a positive
		// integer as the first argument is less than, equal to, or greater than the second.

		@Override
		public int compare (CandidateShadow candidate1, CandidateShadow candidate2) {

			// Order by magnitude, largest first

			int result = Double.compare (candidate2.candidate_mag, candidate1.candidate_mag);

			if (result == 0) {

				// Order by time, earliest first

				result = Long.compare (candidate1.candidate_time, candidate2.candidate_time);

				if (result == 0) {

					// Order by event ID, lexicographically

					String eid1 = candidate1.candidate_event_id;
					String eid2 = candidate2.candidate_event_id;
					result = ( (eid1 == null)
								? ((eid2 == null) ? 0 : -1)
								: ((eid2 == null) ? 1 : (eid1.compareTo(eid2))) );
				}
			}

			return result;
		}
	}




	// Default parameter values for find_shadow.

	public static final long YEAR_IN_MILLIS = 31536000000L;		// 1 year = 365 days

	public static final double DEF_SEARCH_RADIUS = 2000.0;		// default search radius = 2000 km

	public static final double DEF_CENTROID_MAG_FLOOR = 2.5;	// default centroid magnitude floor

	public static final double DEF_LARGE_MAG = 8.0;				// default large magnitude

	public static final double DEF_V3_LARGE_MAG = 7.0;			// default large magnitude for version 3

	public static final double DEF_V3_CENTROID_MULT = 0.5;		// default centroid radius multiplier for version 3

	public static final double DEF_V3_SAMPLE_MULT = 1.0;		// default sample radius multiplier for version 3

	


	// Determine if a given mainshock is shadowed.
	// Parameters:
	//  mainshock = The mainshock to check for shadowing.
	//  time_now = The time at which the check is to be done.  Typically time_now is close
	//    to the current time.  But it can also be in the past, to determine if the
	//    mainshock was shadowed at a past time.
	//  search_radius = The radius of a circle surrounding the mainshock, in km.  The
	//    program searches within the circle to find events larger than the mainshock that
	//    might be shadowing the mainshock.  A recommended value is 2000 km, which is likely
	//    large enough to find the largest possible earthquake (assuming that the centroid
	//    radius multiplier in the magnitude-of-completeness parameters is equal to 1.0).
	//    The value must be positive.
	//  search_time_lo, search_time_hi = Time interval, expressed in milliseconds since
	//    the epoch.  The program searches for possible shadowing events that lie between
	//    the two times.  Typically these are chosen to bracket the mainshock origin time,
	//    and indicate how far apart in time events must be so they don't shadow each other.
	//    Typical values are 1 year before and after the mainshock origin time.  It is
	//    permitted for search_time_hi to be larger than time_now;  the effect is as if
	//    search_time_hi were set equal to time_now.  The value of search_time_lo must be
	//    strictly less than search_time_hi, time_now, and the current time as reported by
	//    System.currentTimeMillis().
	//  centroid_rel_time_lo, centroid_rel_time_hi = Relative time interval, expressed in
	//    milliseconds.  For each candidate shadowing event, the program determines a time
	//    interval by adding these values to the event origin time;  aftershocks occurring
	//    during that time interval are used to compute the centroid (except that
	//    aftershocks occuring after time_now are not considered).  Typical values are 0
	//    and 1 year.  It is required that centroid_rel_time_lo be non-negative, and that
	//    centroid_rel_time_hi be greater than centroid_rel_time_lo.
	//  centroid_mag_floor = Minimum magnitude for aftershocks that are used to compute
	//    the centroid.  The program also considers that centroid magnitude in the
	//    magnitude-of-completeness parameters, and uses the larger of that centroid
	//    magnitude and centroid_mag_floor.  Typical values are 2.5 (suitable for California)
	//    to 3.5 (suitable world-wide).  Note that it is not necessary to vary this with
	//    the mainshock location, if the appropriate variation is set in the magnitude-of-
	//    completeness parameters.  Be cautious about using values smaller than 3.0, as
	//    the amount of data to process increases very rapidly with reduced magnitude.
	//  large_mag = Minimum magnitude for a candidate shadowing event to be considered large.
	//    To perform the centroid algorithm, a separate call to Comcat is made for each large
	//    candidate.  All small candidates are grouped together, and the possible aftershocks
	//    for all of them are retrieved in a single call to Comcat.  The latter call to
	//    to Comcat may cover a region whose radius is as large as three times the Wells and
	//    Coppersmith radius of large_mag (assuming the centroid radius multiplier in the
	//    magnitude-of-completeness parameters is 1.0).  A typical value is 8.0, which gives
	//    a W&C radius of 200 km, and so limits the combined call to a radius of 600 km.
	//    This mechanism avoids the possiblity that the combined call might attempt to
	//    retrieve all small earthquakes over a very large area.  Set to 10.0 to disable.
	//  separation = A 2-element array that is used to return the separation between
	//    the mainshock and the shadowing event.  If the mainshock is shadowed, then
	//    separation[0] receives the separation in kilometers, and separation[1] receives
	//    the separation in days (positive means the mainshock occurs after the shadowing
	//    event).  Can be null if separation is not required.
	// Returns:
	// If the mainshock is not shadowed, then the return value is null.
	// If the mainshock is shadowed, then the return value is the shadowing earthquake.
	//   If there are multiple shadowing earthquakes, then the one with largest magnitude
	//   is returned.  If magnitudes are tied, then the earliest earthquake is returned.
	// An exception from this function likely means a Comcat failure.

	public static ObsEqkRupture find_shadow (ObsEqkRupture mainshock, long time_now,
					double search_radius, long search_time_lo, long search_time_hi,
					long centroid_rel_time_lo, long centroid_rel_time_hi,
					double centroid_mag_floor, double large_mag, double[] separation) {

		// Parameter validation

		if (!( mainshock != null )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow: No mainshock supplied");
		}

		long system_time_now = System.currentTimeMillis();

		if (!( search_time_lo < system_time_now
			&& search_time_lo < search_time_hi
			&& search_time_lo < time_now )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow: Invalid search times"
				+ ": search_time_lo = " + search_time_lo
				+ ", search_time_hi = " + search_time_hi
				+ ", time_now = " + time_now
				+ ", system_time_now = " + system_time_now
				);
		}

		if (!( centroid_rel_time_lo >= 0L
			&& centroid_rel_time_lo < centroid_rel_time_hi )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow: Invalid centroid relative times"
				+ ": centroid_rel_time_lo = " + centroid_rel_time_lo
				+ ", centroid_rel_time_hi = " + centroid_rel_time_hi
			);
		}

		if (!( search_radius > 0.0 )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow: Invalid search radius"
				+ ": search_radius = " + search_radius
			);
		}

		if (separation != null) {
			if (separation.length < 2) {
				throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow: Separation array is too short");
			}
		}

		// Verbose mode flag

		boolean f_verbose = AftershockVerbose.get_verbose_mode();

		// List of large candidate shadowing earthquakes

		ArrayList<CandidateShadow> large_candidates = new ArrayList<CandidateShadow>();

		// List of small candidate shadowing earthquakes

		ArrayList<CandidateShadow> small_candidates = new ArrayList<CandidateShadow>();

		// List of candidate shadowing earthquakes that are combined into a single aftershock call to Comcat

		ArrayList<CandidateShadow> combined_candidates = new ArrayList<CandidateShadow>();

		// A Comcat accessor to use

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Fetch object for magnitude of completeness parameters

		MagCompPage_ParametersFetch mag_comp_fetch = new MagCompPage_ParametersFetch();

		// Get the mainshock parameters

		String mainshock_event_id = mainshock.getEventId();
		long mainshock_time = mainshock.getOriginTime();
		double mainshock_mag = mainshock.getMag();
		Location mainshock_hypo = mainshock.getHypocenterLocation();

		// Construct a circle around the mainshock with the search radius

		SphLatLon mainshock_sph_hypo = new SphLatLon (mainshock_hypo);
		SphRegion search_region = SphRegion.makeCircle (mainshock_sph_hypo, search_radius);

		// Get a list of potential candidates by calling Comcat
		// Potentials must lie in the search region, within the search times,
		// have magnitude at least equal to the mainshock, and not be the mainshock

		double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		boolean wrapLon = false;
		boolean extendedInfo = false;
		int limit_per_call = 0;
		int max_calls = 0;

		ObsEqkRupList potentials = accessor.fetchEventList (mainshock_event_id,
					search_time_lo, Math.min (search_time_hi, time_now),
					min_depth, max_depth,
					search_region, wrapLon, extendedInfo,
					mainshock_mag, limit_per_call, max_calls);

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow: Found " + potentials.size() + " potential shadowing events for mainshock " + mainshock_event_id);
		}

		// Combined values for centroid radius, minimum magnitude, start time, and stop time

		double combined_centroid_radius = 0.0;
		double combined_centroid_min_mag = 10.0;
		long combined_centroid_time_lo = Long.MAX_VALUE;
		long combined_centroid_time_hi = Long.MIN_VALUE;

		// Examine the potentials and accumulate the candidates

		for (ObsEqkRupture potential : potentials) {

			// Get the potential parameters

			String potential_event_id = potential.getEventId();
			long potential_time = potential.getOriginTime();
			double potential_mag = potential.getMag();
			Location potential_hypo = potential.getHypocenterLocation();

			// The accessor should have checked that event ID is different and the potential
			// magnitude and time are in range, but repeat the check anyway;
			// magnitude ties are broken by considering the earlier origin time

			if ( (!(potential_event_id.equals (mainshock_event_id)))
				&& potential_time <= Math.min (search_time_hi, time_now)
				&& potential_time >= search_time_lo
				&& ( potential_mag > mainshock_mag
					|| (potential_mag == mainshock_mag && potential_time < mainshock_time) ) ) {

				// Get the magnitude of completeness parameters

				MagCompPage_Parameters mag_comp_params = mag_comp_fetch.get (potential_hypo);

				// Get the centroid radius

				double centroid_radius = mag_comp_params.get_radiusCentroid (potential_mag);

				// Get the sample radius

				double sample_radius = mag_comp_params.get_radiusSample (potential_mag);

				// Get the distance to the mainshock

				double dist = SphLatLon.horzDistance (mainshock_hypo, potential_hypo);

				// If the potential is close enough to the mainshock so it could possibly shadow it ...

				if (sample_radius + centroid_radius > dist) {

					// Get the centroid minimum magnitude

					double centroid_min_mag = Math.max (mag_comp_params.get_magCentroid (potential_mag), centroid_mag_floor);

					// Get the centroid start and end times

					long centroid_time_lo = potential_time + centroid_rel_time_lo;
					long centroid_time_hi = Math.min (potential_time + centroid_rel_time_hi, time_now);

					// Create the candidate

					CandidateShadow candidate = new CandidateShadow (
													potential,
													centroid_radius,
													centroid_min_mag,
													centroid_time_lo,
													centroid_time_hi,
													sample_radius);

					// If the candidate is large ...

					if (potential_mag >= large_mag) {

						// Place on the list of large candidates

						large_candidates.add (candidate);
					}

					// Otherwise the candidate is small ...

					else {

						// Place on the list of small candidates

						small_candidates.add (candidate);

						// If the candidate can accept aftershocks ...

						if (centroid_time_lo < centroid_time_hi) {

							// Adjust the combined values to include what this candidate needs

							combined_centroid_radius = Math.max (combined_centroid_radius, dist + centroid_radius);
							combined_centroid_min_mag = Math.min (combined_centroid_min_mag, centroid_min_mag);
							combined_centroid_time_lo = Math.min (combined_centroid_time_lo, centroid_time_lo);
							combined_centroid_time_hi = Math.max (combined_centroid_time_hi, centroid_time_hi);

							// Place on the list of combined candidates

							combined_candidates.add (candidate);
						}
					}
				}
			}
		}

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow: Found " + (large_candidates.size() + small_candidates.size()) + " candidate shadowing events for mainshock " + mainshock_event_id);
		}

		// For each large candidate, with the best (largest) considered first ...

		large_candidates.sort (new CandidateComparator());
		for (CandidateShadow candidate : large_candidates) {

			// Accumulate its aftershocks now, in a separate call to Comcat

			candidate.accum_from_comcat (accessor, system_time_now, f_verbose);

			// Get the centroid

			Location centroid = candidate.get_centroid();

			// If the centroid is within the sample radius of the mainshock, then it's shadowing the mainshock

			if (SphLatLon.horzDistance (mainshock_hypo, centroid) <= candidate.sample_radius) {

				// Return the largest-magnitude event that shadows the mainshock

				CandidateShadow best_candidate = candidate;

				double best_distance = SphLatLon.horzDistance (mainshock_hypo, best_candidate.candidate_hypo);
				double best_time_offset = ((double)(mainshock_time - best_candidate.candidate_time))/ComcatOAFAccessor.day_millis;

				if (separation != null) {
					separation[0] = best_distance;
					separation[1] = best_time_offset;
				}

				if (f_verbose) {
					System.out.println ("AftershockStatsShadow.find_shadow: Mainshock " + mainshock_event_id + " is shadowed by event " + best_candidate.candidate_event_id);
					System.out.println (String.format ("AftershockStatsShadow.find_shadow: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_mag, best_candidate.candidate_mag));
					System.out.println (String.format ("AftershockStatsShadow.find_shadow: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
				}

				return best_candidate.rupture;
			}
		}

		// If there is a nonempty time and space region in which we need to look for aftershocks ...

		if (combined_centroid_time_lo < system_time_now
			&& combined_centroid_time_lo < combined_centroid_time_hi
			&& combined_centroid_radius > 0.0) {

			// Construct a circle around the mainshock with the combined centroid radius

			SphRegion centroid_region = SphRegion.makeCircle (mainshock_sph_hypo, combined_centroid_radius);

			// Get a list of possible aftershocks by calling Comcat
			// Aftershocks must lie in the combined centroid region, within the combined centroid times,
			// and have magnitude at least equal to the combined centroid magnitude

			ObsEqkRupList aftershocks = accessor.fetchEventList (null,
						combined_centroid_time_lo, combined_centroid_time_hi,
						min_depth, max_depth,
						centroid_region, wrapLon, extendedInfo,
						combined_centroid_min_mag, limit_per_call, max_calls);

			if (f_verbose) {
				System.out.println ("AftershockStatsShadow.find_shadow: Found " + aftershocks.size() + " possible aftershocks within " + String.format ("%.3f", combined_centroid_radius) + " km of mainshock " + mainshock_event_id);
			}
		
			// For each aftershock ...

			for (ObsEqkRupture aftershock : aftershocks) {

				// Get the aftershock parameters

				String aftershock_event_id = aftershock.getEventId();
				long aftershock_time = aftershock.getOriginTime();
				double aftershock_mag = aftershock.getMag();
				Location aftershock_hypo = aftershock.getHypocenterLocation();

				// Get unit vector for centroid calculation (see AftershockStatsCalc.getSphCentroid)

				double lat = aftershock_hypo.getLatRad();
				double lon = aftershock_hypo.getLonRad();

				double x = (Math.cos(lat) * Math.cos(lon));
				double y = (Math.cos(lat) * Math.sin(lon));
				double z = Math.sin(lat);

				// For each candidate ...

				for (CandidateShadow candidate : combined_candidates) {

					// This aftershock contributes to the candidate centroid if it lies in the
					// time interval, within the radius, has sufficient magnitude, and is not the candidate

					if (   aftershock_time >= candidate.centroid_time_lo
						&& aftershock_time <= candidate.centroid_time_hi
						&& aftershock_mag >= candidate.centroid_min_mag
						&& SphLatLon.horzDistance (aftershock_hypo, candidate.candidate_hypo) <= candidate.centroid_radius
						&& (!( aftershock_event_id.equals (candidate.candidate_event_id) )) ) {

						// Add to the centroid accumulators

						candidate.x += x;
						candidate.y += y;
						candidate.z += z;
					}
				}
			}
		}

		// For each small candidate, with the best (largest) considered first ...

		small_candidates.sort (new CandidateComparator());
		for (CandidateShadow candidate : small_candidates) {

			// Get the centroid

			Location centroid = candidate.get_centroid();

			// If the centroid is within the sample radius of the mainshock, then it's shadowing the mainshock

			if (SphLatLon.horzDistance (mainshock_hypo, centroid) <= candidate.sample_radius) {

				// Return the largest-magnitude event that shadows the mainshock

				CandidateShadow best_candidate = candidate;

				double best_distance = SphLatLon.horzDistance (mainshock_hypo, best_candidate.candidate_hypo);
				double best_time_offset = ((double)(mainshock_time - best_candidate.candidate_time))/ComcatOAFAccessor.day_millis;

				if (separation != null) {
					separation[0] = best_distance;
					separation[1] = best_time_offset;
				}

				if (f_verbose) {
					System.out.println ("AftershockStatsShadow.find_shadow: Mainshock " + mainshock_event_id + " is shadowed by event " + best_candidate.candidate_event_id);
					System.out.println (String.format ("AftershockStatsShadow.find_shadow: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_mag, best_candidate.candidate_mag));
					System.out.println (String.format ("AftershockStatsShadow.find_shadow: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
				}

				return best_candidate.rupture;
			}
		}

		// If no candidates shadow the mainshock, then return null

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow: Mainshock " + mainshock_event_id + " is not shadowed");
		}

		return null;
	}

	


	// Determine if a given mainshock is shadowed.
	// Parameters:
	//  mainshock = The mainshock to check for shadowing.
	//  time_now = The time at which the check is to be done.  Typically time_now is close
	//    to the current time.  But it can also be in the past, to determine if the
	//    mainshock was shadowed at a past time.
	//  search_radius = The radius of a circle surrounding the mainshock, in km.  The
	//    program searches within the circle to find events larger than the mainshock that
	//    might be shadowing the mainshock.  A recommended value is 2000 km, which is likely
	//    large enough to find the largest possible earthquake (assuming that the centroid
	//    radius multiplier in the magnitude-of-completeness parameters is equal to 1.0).
	//    The value must be positive.
	//  search_time_lo, search_time_hi = Time interval, expressed in milliseconds since
	//    the epoch.  The program searches for possible shadowing events that lie between
	//    the two times.  Typically these are chosen to bracket the mainshock origin time,
	//    and indicate how far apart in time events must be so they don't shadow each other.
	//    Typical values are 1 year before and after the mainshock origin time.  It is
	//    permitted for search_time_hi to be larger than time_now;  the effect is as if
	//    search_time_hi were set equal to time_now.  The value of search_time_lo must be
	//    strictly less than search_time_hi, time_now, and the current time as reported by
	//    System.currentTimeMillis().
	//  centroid_rel_time_lo, centroid_rel_time_hi = Relative time interval, expressed in
	//    milliseconds.  For each candidate shadowing event, the program determines a time
	//    interval by adding these values to the event origin time;  aftershocks occurring
	//    during that time interval are used to compute the centroid (except that
	//    aftershocks occuring after time_now are not considered).  Typical values are 0
	//    and 1 year.  It is required that centroid_rel_time_lo be non-negative, and that
	//    centroid_rel_time_hi be greater than centroid_rel_time_lo.
	//  centroid_mag_floor = Minimum magnitude for aftershocks that are used to compute
	//    the centroid.  The program also considers that centroid magnitude in the
	//    magnitude-of-completeness parameters, and uses the larger of that centroid
	//    magnitude and centroid_mag_floor.  Typical values are 2.5 (suitable for California)
	//    to 3.5 (suitable world-wide).  Note that it is not necessary to vary this with
	//    the mainshock location, if the appropriate variation is set in the magnitude-of-
	//    completeness parameters.  Be cautious about using values smaller than 3.0, as
	//    the amount of data to process increases very rapidly with reduced magnitude.
	//  large_mag = Minimum magnitude for a candidate shadowing event to be considered large.
	//    To perform the centroid algorithm, a separate call to Comcat is made for each large
	//    candidate.  All small candidates are grouped together, and the possible aftershocks
	//    for all of them are retrieved in a single call to Comcat.  The latter call to
	//    to Comcat may cover a region whose radius is as large as three times the Wells and
	//    Coppersmith radius of large_mag (assuming the centroid radius multiplier in the
	//    magnitude-of-completeness parameters is 1.0).  A typical value is 8.0, which gives
	//    a W&C radius of 200 km, and so limits the combined call to a radius of 600 km.
	//    This mechanism avoids the possiblity that the combined call might attempt to
	//    retrieve all small earthquakes over a very large area.  Set to 10.0 to disable.
	//  separation = A 2-element array that is used to return the separation between
	//    the mainshock and the shadowing event.  If the mainshock is shadowed, then
	//    separation[0] receives the separation in kilometers, and separation[1] receives
	//    the separation in days (positive means the mainshock occurs after the shadowing
	//    event).  Can be null if separation is not required.
	//  seq_end_time = A 2-element array that is used to return the absolute and relative
	//    times at which the sequence beginning with the mainshock ends.  If the mainshock
	//    is shadowed, then seq_end_time[0] receives the absolute time at which the sequence
	//    ends (in milliseconds since the epoch), and seq_end_time[1] receives the relative
	//    time (in milliseconds since the mainshock).  If the mainshock is shadowed by an
	//    earlier (or concurrent) event, then the sequence end time is the time of the
	//    mainshock (and hence the relative time is zero).  Otherwise, the sequence end time
	//    is the time of the earliest event that shadows the mainshock.
	// Returns:
	// If the mainshock is not shadowed, then the return value is null.
	// If the mainshock is shadowed, then the return value is the shadowing earthquake.
	//   If there are multiple shadowing earthquakes, then the one with largest magnitude
	//   is returned.  If magnitudes are tied, then the earliest earthquake is returned.
	// An exception from this function likely means a Comcat failure.
	// Note: This function is likely to make more Comcat calls than the original find_shadow.

	public static ObsEqkRupture find_shadow_v2 (ObsEqkRupture mainshock, long time_now,
					double search_radius, long search_time_lo, long search_time_hi,
					long centroid_rel_time_lo, long centroid_rel_time_hi,
					double centroid_mag_floor, double large_mag, double[] separation, long[] seq_end_time) {

		// Parameter validation

		if (!( mainshock != null )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: No mainshock supplied");
		}

		long system_time_now = System.currentTimeMillis();

		if (!( search_time_lo < system_time_now
			&& search_time_lo < search_time_hi
			&& search_time_lo < time_now )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: Invalid search times"
				+ ": search_time_lo = " + search_time_lo
				+ ", search_time_hi = " + search_time_hi
				+ ", time_now = " + time_now
				+ ", system_time_now = " + system_time_now
				);
		}

		if (!( centroid_rel_time_lo >= 0L
			&& centroid_rel_time_lo < centroid_rel_time_hi )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: Invalid centroid relative times"
				+ ": centroid_rel_time_lo = " + centroid_rel_time_lo
				+ ", centroid_rel_time_hi = " + centroid_rel_time_hi
			);
		}

		if (!( search_radius > 0.0 )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: Invalid search radius"
				+ ": search_radius = " + search_radius
			);
		}

		if (separation != null) {
			if (separation.length < 2) {
				throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: Separation array is too short");
			}
		}

		if (seq_end_time != null) {
			if (seq_end_time.length < 2) {
				throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v2: Sequence end time array is too short");
			}
		}

		// Verbose mode flag

		boolean f_verbose = AftershockVerbose.get_verbose_mode();

		// List of large candidate shadowing earthquakes

		ArrayList<CandidateShadow> large_candidates = new ArrayList<CandidateShadow>();

		// List of small candidate shadowing earthquakes

		ArrayList<CandidateShadow> small_candidates = new ArrayList<CandidateShadow>();

		// List of candidate shadowing earthquakes that are combined into a single aftershock call to Comcat

		ArrayList<CandidateShadow> combined_candidates = new ArrayList<CandidateShadow>();

		// A Comcat accessor to use

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Fetch object for magnitude of completeness parameters

		MagCompPage_ParametersFetch mag_comp_fetch = new MagCompPage_ParametersFetch();

		// Get the mainshock parameters

		String mainshock_event_id = mainshock.getEventId();
		long mainshock_time = mainshock.getOriginTime();
		double mainshock_mag = mainshock.getMag();
		Location mainshock_hypo = mainshock.getHypocenterLocation();

		// Construct a circle around the mainshock with the search radius

		SphLatLon mainshock_sph_hypo = new SphLatLon (mainshock_hypo);
		SphRegion search_region = SphRegion.makeCircle (mainshock_sph_hypo, search_radius);

		// Get a list of potential candidates by calling Comcat
		// Potentials must lie in the search region, within the search times,
		// have magnitude at least equal to the mainshock, and not be the mainshock

		double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		boolean wrapLon = false;
		boolean extendedInfo = false;
		int limit_per_call = 0;
		int max_calls = 0;

		ObsEqkRupList potentials = accessor.fetchEventList (mainshock_event_id,
					search_time_lo, Math.min (search_time_hi, time_now),
					min_depth, max_depth,
					search_region, wrapLon, extendedInfo,
					mainshock_mag, limit_per_call, max_calls);

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow_v2: Found " + potentials.size() + " potential shadowing events for mainshock " + mainshock_event_id);
		}

		// Combined values for centroid radius, minimum magnitude, start time, and stop time

		double combined_centroid_radius = 0.0;
		double combined_centroid_min_mag = 10.0;
		long combined_centroid_time_lo = Long.MAX_VALUE;
		long combined_centroid_time_hi = Long.MIN_VALUE;

		// Examine the potentials and accumulate the candidates

		for (ObsEqkRupture potential : potentials) {

			// Get the potential parameters

			String potential_event_id = potential.getEventId();
			long potential_time = potential.getOriginTime();
			double potential_mag = potential.getMag();
			Location potential_hypo = potential.getHypocenterLocation();

			// The accessor should have checked that event ID is different and the potential
			// magnitude and time are in range, but repeat the check anyway;
			// magnitude ties are broken by considering the earlier origin time

			if ( (!(potential_event_id.equals (mainshock_event_id)))
				&& potential_time <= Math.min (search_time_hi, time_now)
				&& potential_time >= search_time_lo
				&& ( potential_mag > mainshock_mag
					|| (potential_mag == mainshock_mag && potential_time < mainshock_time) ) ) {

				// Get the magnitude of completeness parameters

				MagCompPage_Parameters mag_comp_params = mag_comp_fetch.get (potential_hypo);

				// Get the centroid radius

				double centroid_radius = mag_comp_params.get_radiusCentroid (potential_mag);

				// Get the sample radius

				double sample_radius = mag_comp_params.get_radiusSample (potential_mag);

				// Get the distance to the mainshock

				double dist = SphLatLon.horzDistance (mainshock_hypo, potential_hypo);

				// If the potential is close enough to the mainshock so it could possibly shadow it ...

				if (sample_radius + centroid_radius > dist) {

					// Get the centroid minimum magnitude

					double centroid_min_mag = Math.max (mag_comp_params.get_magCentroid (potential_mag), centroid_mag_floor);

					// Get the centroid start and end times

					long centroid_time_lo = potential_time + centroid_rel_time_lo;
					long centroid_time_hi = Math.min (potential_time + centroid_rel_time_hi, time_now);

					// Create the candidate

					CandidateShadow candidate = new CandidateShadow (
													potential,
													centroid_radius,
													centroid_min_mag,
													centroid_time_lo,
													centroid_time_hi,
													sample_radius);

					// If the candidate is large ...

					if (potential_mag >= large_mag) {

						// Place on the list of large candidates

						large_candidates.add (candidate);
					}

					// Otherwise the candidate is small ...

					else {

						// Place on the list of small candidates

						small_candidates.add (candidate);

						// If the candidate can accept aftershocks ...

						if (centroid_time_lo < centroid_time_hi) {

							// Adjust the combined values to include what this candidate needs

							combined_centroid_radius = Math.max (combined_centroid_radius, dist + centroid_radius);
							combined_centroid_min_mag = Math.min (combined_centroid_min_mag, centroid_min_mag);
							combined_centroid_time_lo = Math.min (combined_centroid_time_lo, centroid_time_lo);
							combined_centroid_time_hi = Math.max (combined_centroid_time_hi, centroid_time_hi);

							// Place on the list of combined candidates

							combined_candidates.add (candidate);
						}
					}
				}
			}
		}

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow_v2: Found " + (large_candidates.size() + small_candidates.size()) + " candidate shadowing events for mainshock " + mainshock_event_id);
		}

		// The largest-magnitude event that shadows the mainshock found so far, or null if none

		CandidateShadow best_candidate = null;

		// The earliest event that shadows the mainshock found so far, or null if none

		CandidateShadow earliest_candidate = null;

		// For each large candidate, with the best (largest) considered first ...

		large_candidates.sort (new CandidateComparator());
		for (CandidateShadow candidate : large_candidates) {

			// If not already shadowed by an earlier candidate ...

			if (earliest_candidate == null || candidate.candidate_time < earliest_candidate.candidate_time) {

				// Accumulate its aftershocks now, in a separate call to Comcat

				candidate.accum_from_comcat (accessor, system_time_now, f_verbose);

				// Get the centroid

				Location centroid = candidate.get_centroid();

				// If the centroid is within the sample radius of the mainshock, then it's shadowing the mainshock

				if (SphLatLon.horzDistance (mainshock_hypo, centroid) <= candidate.sample_radius) {

					// Return the largest-magnitude event that shadows the mainshock

					if (best_candidate == null) {

						best_candidate = candidate;

						double best_distance = SphLatLon.horzDistance (mainshock_hypo, best_candidate.candidate_hypo);
						double best_time_offset = ((double)(mainshock_time - best_candidate.candidate_time))/ComcatOAFAccessor.day_millis;

						if (separation != null) {
							separation[0] = best_distance;
							separation[1] = best_time_offset;
						}

						if (f_verbose) {
							System.out.println ("AftershockStatsShadow.find_shadow_v2: Mainshock " + mainshock_event_id + " is shadowed by event " + best_candidate.candidate_event_id);
							System.out.println (String.format ("AftershockStatsShadow.find_shadow_v2: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_mag, best_candidate.candidate_mag));
							System.out.println (String.format ("AftershockStatsShadow.find_shadow_v2: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
						}
					}

					// New earliest candidate

					earliest_candidate = candidate;

					// If earlier than mainshock, can return now

					if (earliest_candidate.candidate_time <= mainshock_time) {

						if (seq_end_time != null) {
							seq_end_time[0] = mainshock_time;
							seq_end_time[1] = 0L;
						}

						if (f_verbose) {
							System.out.println ("AftershockStatsShadow.find_shadow_v2: Mainshock is an aftershock of event " + earliest_candidate.candidate_event_id);
						}

						return best_candidate.rupture;
					}
				}
			}
		}

		// If there is a nonempty time and space region in which we need to look for aftershocks ...

		if (combined_centroid_time_lo < system_time_now
			&& combined_centroid_time_lo < combined_centroid_time_hi
			&& combined_centroid_radius > 0.0) {

			// Construct a circle around the mainshock with the combined centroid radius

			SphRegion centroid_region = SphRegion.makeCircle (mainshock_sph_hypo, combined_centroid_radius);

			// Get a list of possible aftershocks by calling Comcat
			// Aftershocks must lie in the combined centroid region, within the combined centroid times,
			// and have magnitude at least equal to the combined centroid magnitude

			ObsEqkRupList aftershocks = accessor.fetchEventList (null,
						combined_centroid_time_lo, combined_centroid_time_hi,
						min_depth, max_depth,
						centroid_region, wrapLon, extendedInfo,
						combined_centroid_min_mag, limit_per_call, max_calls);

			if (f_verbose) {
				System.out.println ("AftershockStatsShadow.find_shadow_v2: Found " + aftershocks.size() + " possible aftershocks within " + String.format ("%.3f", combined_centroid_radius) + " km of mainshock " + mainshock_event_id);
			}
		
			// For each aftershock ...

			for (ObsEqkRupture aftershock : aftershocks) {

				// Get the aftershock parameters

				String aftershock_event_id = aftershock.getEventId();
				long aftershock_time = aftershock.getOriginTime();
				double aftershock_mag = aftershock.getMag();
				Location aftershock_hypo = aftershock.getHypocenterLocation();

				// Get unit vector for centroid calculation (see AftershockStatsCalc.getSphCentroid)

				double lat = aftershock_hypo.getLatRad();
				double lon = aftershock_hypo.getLonRad();

				double x = (Math.cos(lat) * Math.cos(lon));
				double y = (Math.cos(lat) * Math.sin(lon));
				double z = Math.sin(lat);

				// For each candidate ...

				for (CandidateShadow candidate : combined_candidates) {

					// This aftershock contributes to the candidate centroid if it lies in the
					// time interval, within the radius, has sufficient magnitude, and is not the candidate

					if (   aftershock_time >= candidate.centroid_time_lo
						&& aftershock_time <= candidate.centroid_time_hi
						&& aftershock_mag >= candidate.centroid_min_mag
						&& SphLatLon.horzDistance (aftershock_hypo, candidate.candidate_hypo) <= candidate.centroid_radius
						&& (!( aftershock_event_id.equals (candidate.candidate_event_id) )) ) {

						// Add to the centroid accumulators

						candidate.x += x;
						candidate.y += y;
						candidate.z += z;
					}
				}
			}
		}

		// For each small candidate, with the best (largest) considered first ...

		small_candidates.sort (new CandidateComparator());
		for (CandidateShadow candidate : small_candidates) {

			// If not already shadowed by an earlier candidate ...

			if (earliest_candidate == null || candidate.candidate_time < earliest_candidate.candidate_time) {

				// Get the centroid

				Location centroid = candidate.get_centroid();

				// If the centroid is within the sample radius of the mainshock, then it's shadowing the mainshock

				if (SphLatLon.horzDistance (mainshock_hypo, centroid) <= candidate.sample_radius) {

					// Return the largest-magnitude event that shadows the mainshock

					if (best_candidate == null) {

						best_candidate = candidate;

						double best_distance = SphLatLon.horzDistance (mainshock_hypo, best_candidate.candidate_hypo);
						double best_time_offset = ((double)(mainshock_time - best_candidate.candidate_time))/ComcatOAFAccessor.day_millis;

						if (separation != null) {
							separation[0] = best_distance;
							separation[1] = best_time_offset;
						}

						if (f_verbose) {
							System.out.println ("AftershockStatsShadow.find_shadow_v2: Mainshock " + mainshock_event_id + " is shadowed by event " + best_candidate.candidate_event_id);
							System.out.println (String.format ("AftershockStatsShadow.find_shadow_v2: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_mag, best_candidate.candidate_mag));
							System.out.println (String.format ("AftershockStatsShadow.find_shadow_v2: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
						}
					}

					// New earliest candidate

					earliest_candidate = candidate;

					// If earlier than mainshock, can return now

					if (earliest_candidate.candidate_time <= mainshock_time) {

						if (seq_end_time != null) {
							seq_end_time[0] = mainshock_time;
							seq_end_time[1] = 0L;
						}

						if (f_verbose) {
							System.out.println ("AftershockStatsShadow.find_shadow_v2: Mainshock is an aftershock of event " + earliest_candidate.candidate_event_id);
						}

						return best_candidate.rupture;
					}
				}
			}
		}

		// If mainshock is shadowed by a later event, return it

		if (best_candidate != null) {

			long rel_time_millis = earliest_candidate.candidate_time - mainshock_time;
			double rel_time_days = ((double)rel_time_millis)/ComcatOAFAccessor.day_millis;

			if (seq_end_time != null) {
				seq_end_time[0] = earliest_candidate.candidate_time;
				seq_end_time[1] = rel_time_millis;
			}

			if (f_verbose) {
				System.out.println (String.format ("AftershockStatsShadow.find_shadow_v2: Mainshock is a foreshock of event %s, relative time = %.3f days", earliest_candidate.candidate_event_id, rel_time_days));
			}

			return best_candidate.rupture;
		}

		// If no candidates shadow the mainshock, then return null

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow_v2: Mainshock " + mainshock_event_id + " is not shadowed");
		}

		return null;
	}




	// This class holds shadow candidate info, for the version 3 implementation.

	private static class CandidateInfo {
	
		// The rupture, or null if this object is empty..

		public ObsEqkRupture rupture;

		// The event id.

		public String event_id;

		// The time.

		public long time;

		// The magnitude.

		public double mag;

		// The hypocenter.

		public Location hypo;

		// Clear to an empty object.

		public final void clear () {
			rupture = null;
			event_id = null;
			time = Long.MAX_VALUE;
			mag = -Double.MAX_VALUE;
			hypo = null;
			return;
		}

		// Set values for the given rupture.

		public final void set (ObsEqkRupture rup) {
			rupture = rup;
			event_id = rup.getEventId();
			time = rup.getOriginTime();
			mag = rup.getMag();
			hypo = rup.getHypocenterLocation();
			return;
		}

		// Constructor sets up an empty object.

		public CandidateInfo () {
			clear();
		}

		// Constructor sets values for the given rupture.

		public CandidateInfo (ObsEqkRupture rup) {
			set (rup);
		}

		// Copy values from another object.

		public final void copy_from (CandidateInfo other) {
			this.rupture = other.rupture;
			this.event_id = other.event_id;
			this.time = other.time;
			this.mag = other.mag;
			this.hypo = other.hypo;
			return;
		}

		// Return true if this object has been set.

		public final boolean is_set () {
			return rupture != null;
		}

		// Return true if this object is empty.

		public final boolean is_empty () {
			return rupture == null;
		}

		// Calculate the exclusion radius (see find_shadow_v3 for parameter definition).

		public final double exclusion_radius (MagCompPage_ParametersFetch mag_comp_fetch, double centroid_multiplier, double sample_multiplier) {

			// Get the magnitude of completeness parameters

			MagCompPage_Parameters mag_comp_params = mag_comp_fetch.get (hypo);

			// Get the centroid radius

			double centroid_radius = mag_comp_params.get_radiusCentroid (mag);

			// Get the sample radius

			double sample_radius = mag_comp_params.get_radiusSample (mag);

			// Get the exclusion radius

			double exclusion_radius = (centroid_radius * centroid_multiplier) + (sample_radius * sample_multiplier);
			return exclusion_radius;
		}

		// Return true if this event is larger than the other event.
		// Magnitude ties are broken by considering the earlier event to be larger.
		// The event id is used as a final tie-breaker, so this is a total ordering.

		public final boolean is_larger_than (CandidateInfo other) {
			return
			( (this.mag > other.mag) || ( (this.mag == other.mag) &&
				( (this.time < other.time) || ( (this.time == other.time) &&
					(this.event_id.compareTo (other.event_id) < 0)
				))
			));
		}

		// Return true if this event is earlier than the other event.
		// Ties are broken by considering the larger event to be earlier.
		// The event id is used as a final tie-breaker, so this is a total ordering.

		public final boolean is_earlier_than (CandidateInfo other) {
			return
			( (this.time < other.time) || ( (this.time == other.time) &&
				( (this.mag > other.mag) || ( (this.mag == other.mag) &&
					(this.event_id.compareTo (other.event_id) < 0)
				))
			));
		}

	}

	


	// Determine if a given mainshock is shadowed. [Version 3]
	// Parameters:
	//  mainshock = The mainshock to check for shadowing.
	//  time_now = The time at which the check is to be done.  Typically time_now is close
	//    to the current time.  But it can also be in the past, to determine if the
	//    mainshock was shadowed at a past time.
	//  search_radius = The radius of a circle surrounding the mainshock, in km.  The
	//    program searches within the circle to find events larger than the mainshock that
	//    might be shadowing the mainshock.  A recommended value is 2000 km, which is likely
	//    large enough to find the largest possible earthquake (assuming that the centroid
	//    radius multiplier in the magnitude-of-completeness parameters is equal to 1.0).
	//    The value must be positive.
	//  search_time_lo, search_time_hi = Time interval, expressed in milliseconds since
	//    the epoch.  The program searches for possible shadowing events that lie between
	//    the two times.  Typically these are chosen to bracket the mainshock origin time,
	//    and indicate how far apart in time events must be so they don't shadow each other.
	//    Typical values are 1 year before and after the mainshock origin time.  It is
	//    permitted for search_time_hi to be larger than time_now;  the effect is as if
	//    search_time_hi were set equal to time_now.  The value of search_time_lo must be
	//    strictly less than search_time_hi, time_now, and the current time as reported by
	//    System.currentTimeMillis().
	//  centroid_multiplier, sample_multiplier = Multipliers applied to the configured
	//    centroid radius and sample radius of a candidate shadowing event.  The exclusion
	//    radius is calculated as centroid_multiplier times the configured centroid radius,
	//    plus sample_multiplier times the configured sample radius.  The candidate can
	//    shadow the mainshock if the distance between the candidate and the mainshock is
	//    within the exclusion radius.  Typical values are 0.5 and 1.0 respectively.
	//    These values must be non-negative.
	//  large_mag = Minimum magnitude for a candidate shadowing event to be considered large.
	//    An initial call to Comcat is made to find candidates with large magnitude in the
	//    entire search radius.  Then, if needed, a second call is made to find candidates
	//    with small magnitude, using a much smaller radius.  A typical value is 7.0, which
	//    gives a W&C radius of 40 km, and so typically results in the second call having
	//    a radius of 60 km.  This mechanism avoids searching for small earthquakes over
	//    a very large area.  Set to 10.0 to disable.
	//  separation = A 2-element array that is used to return the separation between
	//    the mainshock and the shadowing event.  If the mainshock is shadowed, then
	//    separation[0] receives the separation in kilometers, and separation[1] receives
	//    the separation in days (positive means the mainshock occurs after the shadowing
	//    event).  Can be null if separation is not required.
	//  seq_end_time = A 2-element array that is used to return the absolute and relative
	//    times at which the sequence beginning with the mainshock ends.  If the mainshock
	//    is shadowed, then seq_end_time[0] receives the absolute time at which the sequence
	//    ends (in milliseconds since the epoch), and seq_end_time[1] receives the relative
	//    time (in milliseconds since the mainshock).  If the mainshock is shadowed by an
	//    earlier (or concurrent) event, then the sequence end time is the time of the
	//    mainshock (and hence the relative time is zero).  Otherwise, the sequence end time
	//    is the time of the earliest event that shadows the mainshock.
	// Returns:
	// If the mainshock is not shadowed, then the return value is null.
	// If the mainshock is shadowed, then the return value is the shadowing earthquake.
	//   If there are multiple shadowing earthquakes, then the one with largest magnitude
	//   is returned.  If magnitudes are tied, then the earliest earthquake is returned.
	// An exception from this function likely means a Comcat failure.
	// Note: This function never searches for earthquakes of smaller magnitude than
	//  the mainshock.
	// Note: This function makes one or two Comcat calls.

	public static ObsEqkRupture find_shadow_v3 (ObsEqkRupture mainshock, long time_now,
					double search_radius, long search_time_lo, long search_time_hi,
					double centroid_multiplier, double sample_multiplier,
					double large_mag, double[] separation, long[] seq_end_time) {

		// Parameter validation

		if (!( mainshock != null )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: No mainshock supplied");
		}

		long system_time_now = System.currentTimeMillis();

		if (!( search_time_lo < system_time_now
			&& search_time_lo < search_time_hi
			&& search_time_lo < time_now )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: Invalid search times"
				+ ": search_time_lo = " + search_time_lo
				+ ", search_time_hi = " + search_time_hi
				+ ", time_now = " + time_now
				+ ", system_time_now = " + system_time_now
				);
		}

		if (!( centroid_multiplier >= 0.0
			&& sample_multiplier >= 0.0
			&& (centroid_multiplier > 0.0 || sample_multiplier > 0.0) )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: Invalid radius multipliers"
				+ ": centroid_multiplier = " + centroid_multiplier
				+ ", sample_multiplier = " + sample_multiplier
			);
		}

		if (!( search_radius > 0.0 )) {
			throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: Invalid search radius"
				+ ": search_radius = " + search_radius
			);
		}

		if (separation != null) {
			if (separation.length < 2) {
				throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: Separation array is too short");
			}
		}

		if (seq_end_time != null) {
			if (seq_end_time.length < 2) {
				throw new IllegalArgumentException ("AftershockStatsShadow.find_shadow_v3: Sequence end time array is too short");
			}
		}

		// Verbose mode flag

		boolean f_verbose = AftershockVerbose.get_verbose_mode();

		// A Comcat accessor to use

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Fetch object for magnitude of completeness parameters

		MagCompPage_ParametersFetch mag_comp_fetch = new MagCompPage_ParametersFetch();

		// Get the mainshock parameters, and its exclusion radius

		final CandidateInfo mainshock_info = new CandidateInfo (mainshock);
		final double mainshock_exclusion_radius = mainshock_info.exclusion_radius (mag_comp_fetch, centroid_multiplier, sample_multiplier);

		// Set of earthquakes already seen, initialize to the mainshock

		final Set<String> events_seen = new HashSet<String>();
		events_seen.add (mainshock_info.event_id);

		// The best (largest) shadowing rupture (or empty if none), its parameters, and its distance from the mainshock

		final CandidateInfo best_info = new CandidateInfo();
		double best_distance = Double.MAX_VALUE;

		// The earliest shadowing rupture (or empty if none), and its parameters

		final CandidateInfo earliest_info = new CandidateInfo();

		// Scratch storage for working with a candidate potential shadowing event

		final CandidateInfo potential_info = new CandidateInfo();

		// Create the list of candidate search magnitudes, in descending order

		List<Double> candidate_mag_list = new ArrayList<Double>();

		if (large_mag < 9.99999) {
			if (mainshock_info.mag < large_mag) {
				candidate_mag_list.add (large_mag);
			}
		}
		candidate_mag_list.add (mainshock_info.mag);

		// Loop over search magnitudes

		for (int ix_mag = 0; ix_mag < candidate_mag_list.size(); ++ix_mag) {
			double candidate_search_mag = candidate_mag_list.get (ix_mag);

			// Get the candidate search radius

			double candidate_search_radius = search_radius;

			// If not the first, use the largest possible exclusion radius of the prior search magnitude

			if (ix_mag > 0) {
				double prior_search_mag = candidate_mag_list.get (ix_mag - 1);

				// Use at least the mainshock exclusion radius

				candidate_search_radius = mainshock_exclusion_radius;

				// Loop over all magnitude of completeness parameters to find the largest possible

				for (MagCompPage_Parameters mag_comp_params : mag_comp_fetch.get_parameter_list()) {

					// Get the centroid radius

					double centroid_radius = mag_comp_params.get_radiusCentroid (prior_search_mag);

					// Get the sample radius

					double sample_radius = mag_comp_params.get_radiusSample (prior_search_mag);

					// Get the exclusion radius

					double exclusion_radius = (centroid_radius * centroid_multiplier) + (sample_radius * sample_multiplier);

					// Accumulate maximum

					candidate_search_radius = Math.max (candidate_search_radius, exclusion_radius);
				}

				// But limit to the provided search radius

				candidate_search_radius = Math.min (candidate_search_radius, search_radius);
			}

			// Construct a circle around the mainshock with the candidate search radius

			SphLatLon mainshock_sph_hypo = new SphLatLon (mainshock_info.hypo);
			SphRegion search_region = SphRegion.makeCircle (mainshock_sph_hypo, candidate_search_radius);

			// The effective end of the search time is the provided search_time_hi, but not past time_now,
			// and there is no need to search past the earliest shadowing time seen so far (because such
			// an event would be smaller in magnitude and later in time than a shadowing event already seen)

			long eff_search_time_hi = Math.min (search_time_hi, time_now);

			if (earliest_info.is_set()) {
				eff_search_time_hi = Math.min (eff_search_time_hi, Math.max (earliest_info.time, search_time_lo + 1L));
			}

			// Get a list of potential candidates by calling Comcat
			// Potentials must lie in the search region, within the search times,
			// have magnitude at least equal to the mainshock, and not be the mainshock

			double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
			double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

			boolean wrapLon = false;
			boolean extendedInfo = false;
			int limit_per_call = 0;
			int max_calls = 0;

			ObsEqkRupList potentials = accessor.fetchEventList (mainshock_info.event_id,
						search_time_lo, eff_search_time_hi,
						min_depth, max_depth,
						search_region, wrapLon, extendedInfo,
						candidate_search_mag, limit_per_call, max_calls);

			if (f_verbose) {
				System.out.println ("AftershockStatsShadow.find_shadow_v3: Found " + potentials.size()
					+ " potential shadowing events for mainshock " + mainshock_info.event_id
					+ " for magnitude " + String.format ("%.2f", candidate_search_mag)
					+ " within " + String.format ("%.3f", candidate_search_radius) + " km");
			}

			// Examine the potentials and accumulate the candidates

			for (ObsEqkRupture potential : potentials) {

				// Get the potential parameters

				potential_info.set (potential);

				// Check that the event has not been seen before, and that the time is
				// within the search times, and that the magnitude is larger than the mainshock

				if (
					events_seen.add (potential_info.event_id)
					&& potential_info.time <= eff_search_time_hi
					&& potential_info.time >= search_time_lo
					&& potential_info.is_larger_than (mainshock_info)
				) {

					// Flag if this would be a new best

					boolean f_new_best = (best_info.is_empty() || potential_info.is_larger_than (best_info));

					// Flag if this would be a new earliest, and if current earliest is after the mainshock

					boolean f_new_earliest = (earliest_info.is_empty() || (earliest_info.time > mainshock_info.time && potential_info.is_earlier_than (earliest_info) ));

					// If the event would be a new best or earliest ...

					if (f_new_best || f_new_earliest) {

						// Get the exclusion radius

						double exclusion_radius = potential_info.exclusion_radius (mag_comp_fetch, centroid_multiplier, sample_multiplier);

						// Get the distance to the mainshock

						double dist = SphLatLon.horzDistance (mainshock_info.hypo, potential_info.hypo);

						// If the potential shadows the mainshock ...

						if (dist <= Math.max (exclusion_radius, mainshock_exclusion_radius)) {

							// If it's a new best, record it

							if (f_new_best) {
								best_info.copy_from (potential_info);
								best_distance = dist;
							}

							// If it's a new earliest, record it

							if (f_new_earliest) {
								earliest_info.copy_from (potential_info);
							}
						}
					}
				}
			}

			// If we found a shadowing event and the earliest time is before (or concurrent with) the mainshock, we can stop now

			if (best_info.is_set() && earliest_info.time <= mainshock_info.time) {

				double best_time_offset = ((double)(mainshock_info.time - best_info.time))/ComcatOAFAccessor.day_millis;

				if (separation != null) {
					separation[0] = best_distance;
					separation[1] = best_time_offset;
				}

				if (f_verbose) {
					System.out.println ("AftershockStatsShadow.find_shadow_v3: Mainshock " + mainshock_info.event_id + " is shadowed by event " + best_info.event_id);
					System.out.println (String.format ("AftershockStatsShadow.find_shadow_v3: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_info.mag, best_info.mag));
					System.out.println (String.format ("AftershockStatsShadow.find_shadow_v3: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
				}

				if (seq_end_time != null) {
					seq_end_time[0] = mainshock_info.time;
					seq_end_time[1] = 0L;
				}

				String early_event_id = ((best_info.time <= mainshock_info.time) ? best_info.event_id : earliest_info.event_id);

				if (f_verbose) {
					System.out.println ("AftershockStatsShadow.find_shadow_v3: Mainshock is an aftershock of event " + early_event_id);
				}

				return best_info.rupture;
			}
		}

		// If mainshock is shadowed by a later event, return it

		if (best_info.is_set()) {

			double best_time_offset = ((double)(mainshock_info.time - best_info.time))/ComcatOAFAccessor.day_millis;

			if (separation != null) {
				separation[0] = best_distance;
				separation[1] = best_time_offset;
			}

			if (f_verbose) {
				System.out.println ("AftershockStatsShadow.find_shadow_v3: Mainshock " + mainshock_info.event_id + " is shadowed by event " + best_info.event_id);
				System.out.println (String.format ("AftershockStatsShadow.find_shadow_v3: Mainshock magnitude = %.2f, shadowing event magnitude = %.2f", mainshock_info.mag, best_info.mag));
				System.out.println (String.format ("AftershockStatsShadow.find_shadow_v3: Distance = %.3f km, time offset = %.3f days", best_distance, best_time_offset));
			}

			long rel_time_millis = earliest_info.time - mainshock_info.time;
			double rel_time_days = ((double)rel_time_millis)/ComcatOAFAccessor.day_millis;

			if (seq_end_time != null) {
				seq_end_time[0] = earliest_info.time;
				seq_end_time[1] = rel_time_millis;
			}

			if (f_verbose) {
				System.out.println (String.format ("AftershockStatsShadow.find_shadow_v3: Mainshock is a foreshock of event %s, relative time = %.3f days", earliest_info.event_id, rel_time_days));
			}

			return best_info.rupture;
		}

		// If no candidates shadow the mainshock, then return null

		if (f_verbose) {
			System.out.println ("AftershockStatsShadow.find_shadow_v3: Mainshock " + mainshock_info.event_id + " is not shadowed");
		}

		return null;
	}




	// Constructor.

	public AftershockStatsShadow () {
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("AftershockStatsShadow : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  days
		// Fetch information for an event, and display it.
		// Then, test if the event is shadowed.  The time_now is set to the specified
		//  number of days after the event, and other parameters are set to defaults.
		// Then, display the shadowing event if one is found.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("AftershockStatsShadow : Invalid 'test1' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double days = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				double rup_mag = rup.getMag();
				Location rup_hypo = rup.getHypocenterLocation();
				double rup_lat = rup_hypo.getLatitude();
				double rup_lon = rup_hypo.getLongitude();
				double rup_depth = rup_hypo.getDepth();

				System.out.println ("rup_event_id = " + rup_event_id);
				System.out.println ("rup_time = " + rup_time + " (" + SimpleUtils.time_to_string(rup_time) + ")");
				System.out.println ("rup_mag = " + rup_mag);
				System.out.println ("rup_lat = " + rup_lat);
				System.out.println ("rup_lon = " + rup_lon);
				System.out.println ("rup_depth = " + rup_depth);

				// Get find shadow parameters

				long time_now = rup_time + (long)(days*ComcatOAFAccessor.day_millis);
				double search_radius = DEF_SEARCH_RADIUS;
				long search_time_lo = rup_time - YEAR_IN_MILLIS;
				long search_time_hi = rup_time + YEAR_IN_MILLIS;
				long centroid_rel_time_lo = 0L;
				long centroid_rel_time_hi = YEAR_IN_MILLIS;
				double centroid_mag_floor = DEF_CENTROID_MAG_FLOOR;
				double large_mag = DEF_LARGE_MAG;
				double[] separation = new double[2];

				System.out.println ("");
				System.out.println ("find_shadow parameters:");
				System.out.println ("time_now = " + time_now + " (" + SimpleUtils.time_to_string(time_now) + ")");
				System.out.println ("search_radius = " + search_radius);
				System.out.println ("search_time_lo = " + search_time_lo + " (" + SimpleUtils.time_to_string(search_time_lo) + ")");
				System.out.println ("search_time_hi = " + search_time_hi + " (" + SimpleUtils.time_to_string(search_time_hi) + ")");
				System.out.println ("centroid_rel_time_lo = " + centroid_rel_time_lo);
				System.out.println ("centroid_rel_time_hi = " + centroid_rel_time_hi);
				System.out.println ("centroid_mag_floor = " + centroid_mag_floor);
				System.out.println ("large_mag = " + large_mag);

				// Run find_shadow

				System.out.println ("");
				System.out.println ("Finding shadow:");

				ObsEqkRupture shadow = find_shadow (rup, time_now,
					search_radius, search_time_lo, search_time_hi,
					centroid_rel_time_lo, centroid_rel_time_hi,
					centroid_mag_floor, large_mag, separation);

				// Display results

				System.out.println ("");

				if (shadow == null) {
					System.out.println ("Event is not shadowed");
				} else {
					System.out.println ("Event is shadowed by:");

					String shadow_event_id = shadow.getEventId();
					long shadow_time = shadow.getOriginTime();
					double shadow_mag = shadow.getMag();
					Location shadow_hypo = shadow.getHypocenterLocation();
					double shadow_lat = shadow_hypo.getLatitude();
					double shadow_lon = shadow_hypo.getLongitude();
					double shadow_depth = shadow_hypo.getDepth();

					System.out.println ("shadow_event_id = " + shadow_event_id);
					System.out.println ("shadow_time = " + shadow_time + " (" + SimpleUtils.time_to_string(shadow_time) + ")");
					System.out.println ("shadow_mag = " + shadow_mag);
					System.out.println ("shadow_lat = " + shadow_lat);
					System.out.println ("shadow_lon = " + shadow_lon);
					System.out.println ("shadow_depth = " + shadow_depth);

					System.out.println ("separation_km = " + String.format ("%.3f", separation[0]));
					System.out.println ("separation_days = " + String.format ("%.3f", separation[1]));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id  days
		// Fetch information for an event, and display it.
		// Then, test if the event is shadowed.  The time_now is set to the specified
		//  number of days after the event, and other parameters are set to defaults.
		// Then, display the shadowing event if one is found.
		// Same as test #1 except uses find_shadow_v2.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("AftershockStatsShadow : Invalid 'test2' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double days = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				double rup_mag = rup.getMag();
				Location rup_hypo = rup.getHypocenterLocation();
				double rup_lat = rup_hypo.getLatitude();
				double rup_lon = rup_hypo.getLongitude();
				double rup_depth = rup_hypo.getDepth();

				System.out.println ("rup_event_id = " + rup_event_id);
				System.out.println ("rup_time = " + rup_time + " (" + SimpleUtils.time_to_string(rup_time) + ")");
				System.out.println ("rup_mag = " + rup_mag);
				System.out.println ("rup_lat = " + rup_lat);
				System.out.println ("rup_lon = " + rup_lon);
				System.out.println ("rup_depth = " + rup_depth);

				// Get find shadow parameters

				long time_now = rup_time + (long)(days*ComcatOAFAccessor.day_millis);
				double search_radius = DEF_SEARCH_RADIUS;
				long search_time_lo = rup_time - YEAR_IN_MILLIS;
				long search_time_hi = rup_time + YEAR_IN_MILLIS;
				long centroid_rel_time_lo = 0L;
				long centroid_rel_time_hi = YEAR_IN_MILLIS;
				double centroid_mag_floor = DEF_CENTROID_MAG_FLOOR;
				double large_mag = DEF_LARGE_MAG;
				double[] separation = new double[2];
				long[] seq_end_time = new long[2];

				System.out.println ("");
				System.out.println ("find_shadow_v2 parameters:");
				System.out.println ("time_now = " + time_now + " (" + SimpleUtils.time_to_string(time_now) + ")");
				System.out.println ("search_radius = " + search_radius);
				System.out.println ("search_time_lo = " + search_time_lo + " (" + SimpleUtils.time_to_string(search_time_lo) + ")");
				System.out.println ("search_time_hi = " + search_time_hi + " (" + SimpleUtils.time_to_string(search_time_hi) + ")");
				System.out.println ("centroid_rel_time_lo = " + centroid_rel_time_lo);
				System.out.println ("centroid_rel_time_hi = " + centroid_rel_time_hi);
				System.out.println ("centroid_mag_floor = " + centroid_mag_floor);
				System.out.println ("large_mag = " + large_mag);

				// Run find_shadow_v2

				System.out.println ("");
				System.out.println ("Finding shadow:");

				ObsEqkRupture shadow = find_shadow_v2 (rup, time_now,
					search_radius, search_time_lo, search_time_hi,
					centroid_rel_time_lo, centroid_rel_time_hi,
					centroid_mag_floor, large_mag, separation, seq_end_time);

				// Display results

				System.out.println ("");

				if (shadow == null) {
					System.out.println ("Event is not shadowed");
				} else {
					System.out.println ("Event is shadowed by:");

					String shadow_event_id = shadow.getEventId();
					long shadow_time = shadow.getOriginTime();
					double shadow_mag = shadow.getMag();
					Location shadow_hypo = shadow.getHypocenterLocation();
					double shadow_lat = shadow_hypo.getLatitude();
					double shadow_lon = shadow_hypo.getLongitude();
					double shadow_depth = shadow_hypo.getDepth();

					System.out.println ("shadow_event_id = " + shadow_event_id);
					System.out.println ("shadow_time = " + shadow_time + " (" + SimpleUtils.time_to_string(shadow_time) + ")");
					System.out.println ("shadow_mag = " + shadow_mag);
					System.out.println ("shadow_lat = " + shadow_lat);
					System.out.println ("shadow_lon = " + shadow_lon);
					System.out.println ("shadow_depth = " + shadow_depth);

					System.out.println ("separation_km = " + String.format ("%.3f", separation[0]));
					System.out.println ("separation_days = " + String.format ("%.3f", separation[1]));

					System.out.println ("seq_end_time_abs = " + seq_end_time[0] + " (" + SimpleUtils.time_to_string(seq_end_time[0]) + ")");
					System.out.println ("seq_end_time_rel_days = " + String.format ("%.3f", ((double)(seq_end_time[1]))/ComcatOAFAccessor.day_millis));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id  days
		// Fetch information for an event, and display it.
		// Then, test if the event is shadowed.  The time_now is set to the specified
		//  number of days after the event, and other parameters are set to defaults.
		// Then, display the shadowing event if one is found.
		// Same as test #1 except uses find_shadow_v3.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("AftershockStatsShadow : Invalid 'test3' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				double days = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				String rup_event_id = rup.getEventId();
				long rup_time = rup.getOriginTime();
				double rup_mag = rup.getMag();
				Location rup_hypo = rup.getHypocenterLocation();
				double rup_lat = rup_hypo.getLatitude();
				double rup_lon = rup_hypo.getLongitude();
				double rup_depth = rup_hypo.getDepth();

				System.out.println ("rup_event_id = " + rup_event_id);
				System.out.println ("rup_time = " + rup_time + " (" + SimpleUtils.time_to_string(rup_time) + ")");
				System.out.println ("rup_mag = " + rup_mag);
				System.out.println ("rup_lat = " + rup_lat);
				System.out.println ("rup_lon = " + rup_lon);
				System.out.println ("rup_depth = " + rup_depth);

				// Get find shadow parameters

				long time_now = rup_time + (long)(days*ComcatOAFAccessor.day_millis);
				double search_radius = DEF_SEARCH_RADIUS;
				long search_time_lo = rup_time - YEAR_IN_MILLIS;
				long search_time_hi = rup_time + YEAR_IN_MILLIS;
				double centroid_multiplier = DEF_V3_CENTROID_MULT;
				double sample_multiplier = DEF_V3_SAMPLE_MULT;
				double large_mag = DEF_V3_LARGE_MAG;
				double[] separation = new double[2];
				long[] seq_end_time = new long[2];

				System.out.println ("");
				System.out.println ("find_shadow_v3 parameters:");
				System.out.println ("time_now = " + time_now + " (" + SimpleUtils.time_to_string(time_now) + ")");
				System.out.println ("search_radius = " + search_radius);
				System.out.println ("search_time_lo = " + search_time_lo + " (" + SimpleUtils.time_to_string(search_time_lo) + ")");
				System.out.println ("search_time_hi = " + search_time_hi + " (" + SimpleUtils.time_to_string(search_time_hi) + ")");
				System.out.println ("centroid_multiplier = " + centroid_multiplier);
				System.out.println ("sample_multiplier = " + sample_multiplier);
				System.out.println ("large_mag = " + large_mag);

				// Run find_shadow_v2

				System.out.println ("");
				System.out.println ("Finding shadow:");

				ObsEqkRupture shadow = find_shadow_v3 (rup, time_now,
					search_radius, search_time_lo, search_time_hi,
					centroid_multiplier, sample_multiplier,
					large_mag, separation, seq_end_time);

				// Display results

				System.out.println ("");

				if (shadow == null) {
					System.out.println ("Event is not shadowed");
				} else {
					System.out.println ("Event is shadowed by:");

					String shadow_event_id = shadow.getEventId();
					long shadow_time = shadow.getOriginTime();
					double shadow_mag = shadow.getMag();
					Location shadow_hypo = shadow.getHypocenterLocation();
					double shadow_lat = shadow_hypo.getLatitude();
					double shadow_lon = shadow_hypo.getLongitude();
					double shadow_depth = shadow_hypo.getDepth();

					System.out.println ("shadow_event_id = " + shadow_event_id);
					System.out.println ("shadow_time = " + shadow_time + " (" + SimpleUtils.time_to_string(shadow_time) + ")");
					System.out.println ("shadow_mag = " + shadow_mag);
					System.out.println ("shadow_lat = " + shadow_lat);
					System.out.println ("shadow_lon = " + shadow_lon);
					System.out.println ("shadow_depth = " + shadow_depth);

					System.out.println ("separation_km = " + String.format ("%.3f", separation[0]));
					System.out.println ("separation_days = " + String.format ("%.3f", separation[1]));

					System.out.println ("seq_end_time_abs = " + seq_end_time[0] + " (" + SimpleUtils.time_to_string(seq_end_time[0]) + ")");
					System.out.println ("seq_end_time_rel_days = " + String.format ("%.3f", ((double)(seq_end_time[1]))/ComcatOAFAccessor.day_millis));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("AftershockStatsShadow : Unrecognized subcommand : " + args[0]);
		return;

	}

}
