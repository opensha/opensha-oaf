package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;

import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.oaf.rj.CompactEqkRupList;


/**
 * Status entry in the event timeline.
 * Author: Michael Barall 04/10/2018.
 *
 * The AAFS server maintains a timeline for each event it is tracking.
 *
 * The actcode defines the most recent action in the timeline.  It is copied
 * into TimelineEntry.  In principle, actcode could select amont different
 * classes, but currently TimelineStatus is used for all timeline entries.
 *
 * The action_time that appears in a TimelineEntry containing a forecast is equal to
 * the mainshock_time plus forecast_lag (in forecast_parameters).  It is necessary
 * that the action_time be strictly increasing so that timeline entries are
 * properly ordered in the database.  The first timeline entry has forecast_lag
 * equal to zero.  The action_time should always be less than the current time.
 * For non-forecast TimelineEntry object, the action_time is not necessarily
 * related to the current time, but is just set to maintain monotonicity.  These
 * rules may be bent if needed to maintain monotonicity.
 *
 * The event_id that appears in TimelineEntry is a string that uniquely identifies
 * an earthquake throughout its history, even if the Comcat IDs change.  All
 * entries in a timeline have the same event_id.
 * 
 */
public class TimelineStatus extends DBPayload {

	//----- Envelope information -----
	//
	// These variables are copied to/from TimelineEntry rather than being marshaled,
	// so they are exposed to database code.

	// Event ID for this timeline.
	// Despite the name, this holds a timeline ID.

	public String event_id;

	// Action code for this timeline entry.

	public int actcode;

	public static final int ACTCODE_MIN                 = 1;
	public static final int ACTCODE_TRACK               = 1;
		// This is the first entry in the timeline, when event tracking begins.
		// The cause is recorded in fc_origin below.  It may also contain analsyt
		// parameter values.  Only the first timeline entry can be ACTCODE_TRACK.
	public static final int ACTCODE_FORECAST            = 2;
		// The entry contains a forecast.
	public static final int ACTCODE_ANALYST             = 3;
		// The entry contains an analyst intervention.  The analyst may assign
		// parameter values and turn tracking on or off.
	public static final int ACTCODE_PDL_UPDATE          = 4;
		// The entry contains an update to PDL status, usually the final result of a
		// delayed PDL report.  This is recorded in pdl_status below.
	public static final int ACTCODE_ERROR               = 5;
		// The entry is the last timeline entry due to an unrecoverable error.
		// In this case, the payload may not parse correctly.
	public static final int ACTCODE_WITHDRAWN           = 6;
		// The entry is the last timeline entry due to the automatic system
		// determining that the event is not eligible to be tracked.
	public static final int ACTCODE_COMCAT_FAIL         = 7;
		// The entry is the last timeline entry due to inability to retrieve
		// necessary data from ComCat.
	public static final int ACTCODE_SHADOWED            = 8;
		// The entry is the last timeline entry because the automatic system
		// has determined it is shadowed.
	public static final int ACTCODE_EXPIRED             = 9;
		// The entry is the last timeline entry because the automatic system
		// has determined it is expired. Note that an expired action may not
		// appear if expired status was set on the final forecast action.
	public static final int ACTCODE_STATUS_UPDATE       = 10;
		// The entry contains an update to forecast status.  This is recorded
		// in fc_status below.  There may also be a change to pdl_status.
	public static final int ACTCODE_SKIPPED             = 11;
		// The entry represents a forecast that was skipped, typically because
		// the earthquake is shadowed or does not pass the intake filter.
	public static final int ACTCODE_MAX                 = 11;

	// Return a string describing the actcode.

	public String get_actcode_as_string () {
		switch (actcode) {
		case ACTCODE_TRACK: return "ACTCODE_TRACK";
		case ACTCODE_FORECAST: return "ACTCODE_FORECAST";
		case ACTCODE_ANALYST: return "ACTCODE_ANALYST";
		case ACTCODE_PDL_UPDATE: return "ACTCODE_PDL_UPDATE";
		case ACTCODE_ERROR: return "ACTCODE_ERROR";
		case ACTCODE_WITHDRAWN: return "ACTCODE_WITHDRAWN";
		case ACTCODE_COMCAT_FAIL: return "ACTCODE_COMCAT_FAIL";
		case ACTCODE_SHADOWED: return "ACTCODE_SHADOWED";
		case ACTCODE_EXPIRED: return "ACTCODE_EXPIRED";
		case ACTCODE_STATUS_UPDATE: return "ACTCODE_STATUS_UPDATE";
		case ACTCODE_SKIPPED: return "ACTCODE_SKIPPED";
		}
		return "ACTCODE_INVALID(" + actcode + ")";
	}

	// Time stamp for this timeline entry, in milliseconds since the epoch.
	// This must be strictly monotone increasing within the timeline.
	// For forecasts (ACTCODE_FORECAST), this is the nominal forecast time (mainshock_time + forecast_lag),
	//  rounded down to a whole number of seconds; but increased if necessary to preserve monotonicity.
	// For the initial timeline entry (ACTCODE_TRACK), this is the mainshock time (mainshock_time),
	//  rounded down to a whole number of seconds.
	// For other entries, this is one more than in the prior entry.

	public long action_time;

	// List of Comcat IDs associated with this timeline.
	// The list is updated when the timeline is created, and at each forecast.

	public String[] comcat_ids;


	//----- Status variables -----
	//
	// These hold the current status of the timeline.
	// They are generally copied from one timeline entry to the next,
	// modified as necessary to accomodate state changes.

	// Time at which this entry was created, in milliseconds since the epoch.

	public long entry_time;

	// Origin of this timeline.

	public int fc_origin;

	public static final int FCORIG_MIN               = 1;
	public static final int FCORIG_PDL               = 1;
		// Timeline created in response to PDL notification.
	public static final int FCORIG_ANALYST           = 2;
		// Timeline created in response to analyst intervention.
	public static final int FCORIG_SYNC              = 3;
		// Timeline created while synchronizing with ComCat, backup, or another server.
	public static final int FCORIG_UNKNOWN           = 4;
		// Timeline created for unknown reason.  This should only be the case when
		// the timeline is in an error state.
	public static final int FCORIG_POLL              = 5;
		// Timeline created in response to Comcat poll.
	public static final int FCORIG_SPLIT             = 6;
		// Timeline created in response to alias timeline split.
	public static final int FCORIG_RECOVERY          = 7;
		// Timeline created during a recovery operation.
	public static final int FCORIG_MAX               = 7;

	// Return a string describing the fc_origin.

	public String get_fc_origin_as_string () {
		switch (fc_origin) {
		case FCORIG_PDL: return "FCORIG_PDL";
		case FCORIG_ANALYST: return "FCORIG_ANALYST";
		case FCORIG_SYNC: return "FCORIG_SYNC";
		case FCORIG_UNKNOWN: return "FCORIG_UNKNOWN";
		case FCORIG_POLL: return "FCORIG_POLL";
		case FCORIG_SPLIT: return "FCORIG_SPLIT";
		case FCORIG_RECOVERY: return "FCORIG_RECOVERY";
		}
		return "FCORIG_INVALID(" + fc_origin + ")";
	}

	// Status of this timeline.

	public int fc_status;

	public static final int FCSTAT_MIN               = 1;
	public static final int FCSTAT_ACTIVE_NORMAL     = 1;
		// Normal active state.  Forecasts are being generated automatically,
		// according to the configured schedule.
	public static final int FCSTAT_ACTIVE_INTAKE     = 2;
		// Active state, for an event that has never passed the intake filter.
		// This timeline can be withdrawn after sufficient time.
	public static final int FCSTAT_STOP_EXPIRED      = 3;
		// Stopped state.  All forecasts have been completed.
	public static final int FCSTAT_STOP_ANALYST      = 4;
		// Stopped state.  An analyst intervention requested that no more forecasts
		// be generated.
	public static final int FCSTAT_STOP_SHADOWED     = 5;
		// Stopped state.  An close-by earthquake was found with greater magnitude than the
		// mainshock, and so this timeline is now considered to be shadowed.
	public static final int FCSTAT_STOP_ERROR        = 6;
		// Stopped state.  An error was encountered that makes further use of this
		// timeline impossible.
	public static final int FCSTAT_STOP_WITHDRAWN    = 7;
		// Stopped state.  The automatic system decided to stop tracking this event,
		// due to it being of insufficient magnitude or outside the intake region.
	public static final int FCSTAT_STOP_COMCAT_FAIL  = 8;
		// Stopped state.  The system is unable to retrieve information from ComCat.
	public static final int FCSTAT_MAX               = 8;

	// Return a string describing the fc_status.

	public String get_fc_status_as_string () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL: return "FCSTAT_ACTIVE_NORMAL";
		case FCSTAT_ACTIVE_INTAKE: return "FCSTAT_ACTIVE_INTAKE";
		case FCSTAT_STOP_EXPIRED: return "FCSTAT_STOP_EXPIRED";
		case FCSTAT_STOP_ANALYST: return "FCSTAT_STOP_ANALYST";
		case FCSTAT_STOP_SHADOWED: return "FCSTAT_STOP_SHADOWED";
		case FCSTAT_STOP_ERROR: return "FCSTAT_STOP_ERROR";
		case FCSTAT_STOP_WITHDRAWN: return "FCSTAT_STOP_WITHDRAWN";
		case FCSTAT_STOP_COMCAT_FAIL: return "FCSTAT_STOP_COMCAT_FAIL";
		}
		return "FCSTAT_INVALID(" + fc_status + ")";
	}

	// PDL reporting status, for the current or most recent forecast.

	public int pdl_status;

	public static final int PDLSTAT_MIN              = 1;
	public static final int PDLSTAT_NONE             = 1;
		// No PDL report was requested for the forecast.
	public static final int PDLSTAT_SUCCESS          = 2;
		// Forecast was successfully reported to PDL.
	public static final int PDLSTAT_FAILURE          = 3;
		// Forecast was permanently not reported to PDL due to excessive retries.
	public static final int PDLSTAT_PENDING          = 4;
		// Report is pending.  One or more attempts to report the forecast failed,
		// but the attempt will be repeated.
	public static final int PDLSTAT_SECONDARY        = 5;
		// Forecast needs to be reported to PDL, but it was not done because this
		// is a secondary server.  PDLSTAT_SECONDARY and PDLSTAT_PENDING are the
		// same state, the only difference is how the state was entered.
	public static final int PDLSTAT_CONFIRMED        = 6;
		// Forecast needs to be reported to PDL, but it was not done because this
		// is or was a secondary server, and it has been confirmed that the other
		// server sent the report.
	public static final int PDLSTAT_BYPASSED         = 7;
		// Forecast needs to be reported to PDL, but it was not done because it
		// was too late or some other blocking condition.
	public static final int PDLSTAT_CONFLICT         = 8;
		// Forecast was not stored in PDL because of the presence of a conflicting
		// forecast. This is considered a successful PDL send.
	public static final int PDLSTAT_MAX              = 8;

	// Return a string describing the pdl_status.

	public String get_pdl_status_as_string () {
		switch (pdl_status) {
		case PDLSTAT_NONE: return "PDLSTAT_NONE";
		case PDLSTAT_SUCCESS: return "PDLSTAT_SUCCESS";
		case PDLSTAT_FAILURE: return "PDLSTAT_FAILURE";
		case PDLSTAT_PENDING: return "PDLSTAT_PENDING";
		case PDLSTAT_SECONDARY: return "PDLSTAT_SECONDARY";
		case PDLSTAT_CONFIRMED: return "PDLSTAT_CONFIRMED";
		case PDLSTAT_BYPASSED: return "PDLSTAT_BYPASSED";
		case PDLSTAT_CONFLICT: return "PDLSTAT_CONFLICT";
		}
		return "PDLSTAT_INVALID(" + pdl_status + ")";
	}

	// The code used for the last PDL product successfully sent.
	// If this is an empty string "", then the code is derived from event_id.

	public String pdl_product_code;

	// Result code, for the current or most recent forecast.

	public int fc_result;

	public static final int FCRES_MIN                = 1;
	public static final int FCRES_NONE               = 1;
		// No forecast or skip has occurred yet.
	public static final int FCRES_FORECAST_PDL       = 2;
		// Forecast was successfully generated, and produced results elibible for PDL.
		// Note: Examine pdl_status to determine if results were actually sent to PDL.
	public static final int FCRES_FORECAST_NO_PDL    = 3;
		// Forecast was successfully generated, and did not produce results elibible for PDL.
	public static final int FCRES_SKIPPED_STALE      = 4;
		// Forecast was skipped because it would be stale.
	public static final int FCRES_SKIPPED_ANALYST    = 5;
		// Forecast was skipped at analyst request.
	public static final int FCRES_SKIPPED_INTAKE     = 6;
		// Forecast was skipped because the mainshock did not pass the intake filter.
	public static final int FCRES_SKIPPED_SHADOWED   = 7;
		// Forecast was skipped because the mainshock is shadowed (i.e., is an
		// aftershock of a larger earthquake).
	public static final int FCRES_SKIPPED_FORESHOCK  = 8;
		// Forecast was skipped because the mainshock has an aftershock of
		// larger magnitude.
	public static final int FCRES_MAX                = 8;

	// Return a string describing the fc_result.

	public String get_fc_result_as_string () {
		switch (fc_result) {
		case FCRES_NONE: return "FCRES_NONE";
		case FCRES_FORECAST_PDL: return "FCRES_FORECAST_PDL";
		case FCRES_FORECAST_NO_PDL: return "FCRES_FORECAST_NO_PDL";
		case FCRES_SKIPPED_STALE: return "FCRES_SKIPPED_STALE";
		case FCRES_SKIPPED_ANALYST: return "FCRES_SKIPPED_ANALYST";
		case FCRES_SKIPPED_INTAKE: return "FCRES_SKIPPED_INTAKE";
		case FCRES_SKIPPED_SHADOWED: return "FCRES_SKIPPED_SHADOWED";
		case FCRES_SKIPPED_FORESHOCK: return "FCRES_SKIPPED_FORESHOCK";
		}
		return "FCRES_INVALID(" + fc_result + ")";
	}

	// If this earthquake is shadowed, this is the event_id of the shadowing event.
	// If this earthquake is a foreshock, this is the event_id of the larger aftershock.
	// Otherwise, this is an empty string "".  This should be an empty string unless
	// the result is FCRES_SKIPPED_SHADOWED or FCRES_SKIPPED_FORESHOCK.

	public String shadowing_event_id;


	//----- Analyst data -----
	//
	// If an analyst has intervened in the timeline, these variables hold the analyst info.
	// These are copied from one timeline entry to the next, to preserve the
	// analyst selections, unless the analyst intervenes again.

	// Parameters supplied by the analyst.
	// This must always be non-null, except in an error state.  It is set to defaults
	// when the timeline is created, and is updated whenever the analyst supplies new data.

	public AnalystOptions analyst_options;


	//----- Forecast data -----

	// Most recently obtained information for the mainshock.
	// This must always be non-null, except in an error state.  It is set when the
	// timeline is created and updated at the time of each forecast.  It may also be
	// updated at other times, if mainshock info is available.

	public ForecastMainshock forecast_mainshock;

	// Parameters for the current forecast.
	// This must be non-null if the timeline entry contains a forecast.  It is null
	// in the first timeline entry.  It may be null in other entries, unless it
	// needs to be carried forward.

	public ForecastParameters forecast_params;

	// Results for the current forecast.
	// This must be non-null if the timeline entry contains a forecast.  It is null
	// in the first timeline entry.  It may be null in other entries, unless it
	// needs to be carried forward.

	public ForecastResults forecast_results;


	//----- Scheduling data -----

	// Forecast stamp which identifies the last forecast issued.
	// The value is new ForecastStamp() if there have been no prior forecasts.
	// This is always non-null.

	public ForecastStamp last_forecast_stamp;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public TimelineStatus () {}

	// Copy from another entry.

	public void copy_from (TimelineStatus other) {

		event_id            = other.event_id;
		actcode             = other.actcode;
		action_time         = other.action_time;
		comcat_ids          = other.comcat_ids;

		entry_time          = other.entry_time;
		fc_origin           = other.fc_origin;
		fc_status           = other.fc_status;
		pdl_status          = other.pdl_status;
		pdl_product_code    = other.pdl_product_code;
		fc_result           = other.fc_result;
		shadowing_event_id  = other.shadowing_event_id;

		analyst_options     = other.analyst_options;

		forecast_mainshock  = other.forecast_mainshock;
		forecast_params     = other.forecast_params;
		forecast_results    = other.forecast_results;

		last_forecast_stamp = other.last_forecast_stamp;
		return;
	}




	//----- Service functions: Query -----

	// Return true if this timeline is in a state that requires a future forecast.

	public boolean is_forecast_state () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL:
		case FCSTAT_ACTIVE_INTAKE:
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that requires intake to be
	// reconsidered when a forecast is generated.

	public boolean is_intake_state () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_INTAKE:
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that requires a PDL report retry.

	public boolean is_pdl_retry_state () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL:
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_EXPIRED:
			if (pdl_status == PDLSTAT_PENDING || pdl_status == PDLSTAT_SECONDARY) {

				// It is an error to be in this state if there is no PDL report available

				if (!( forecast_mainshock != null
					&& forecast_mainshock.mainshock_avail
					&& forecast_params != null
					&& forecast_results != null
					&& forecast_results.get_pdl_model() != null )) {
					throw new IllegalStateException ("TimelineStatus.is_pdl_retry_state: In " + get_pdl_status_as_string() + " state but no PDL report is available");
				}

				return true;
			}
			break;
		}
		return false;
	}

	// Return true if there should be an associated catalog snapshot.

	public boolean has_catalog_snapshot () {
		switch (actcode) {
		case ACTCODE_FORECAST:
			if (forecast_results.catalog_result_avail) {
				return true;
			}
			break;
		}
		return false;
	}

	// Get the associated catalog snapshot, or null if none.
	// Note: The catalog is not marshaled, so this will onl obtain the catalog
	// when the forecast is first generated.

	public CompactEqkRupList get_catalog_snapshot () {
		switch (actcode) {
		case ACTCODE_FORECAST:
			if (forecast_results.catalog_result_avail) {
				return forecast_results.catalog_aftershocks;
			}
			break;
		}
		return null;
	}

	// Return true if this is the first timeline entry.

	public boolean is_first_entry () {
		switch (actcode) {
		case ACTCODE_TRACK:
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that can be converted to the
	// normal active state by changing fc_status.

	public boolean is_convertible_to_normal () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_WITHDRAWN:
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that an analyst can
	// start by changing fc_status (with or without updating analyst data).

	public boolean can_analyst_start () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_ANALYST:
		case FCSTAT_STOP_WITHDRAWN:
		case FCSTAT_STOP_EXPIRED:		// supports extending an already-expired timeline
		case FCSTAT_STOP_SHADOWED:		// supports restarting a shadowed timeline
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that an analyst can
	// stop by changing fc_status (with or without updating analyst data).

	public boolean can_analyst_stop () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL:
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_WITHDRAWN:
		case FCSTAT_STOP_EXPIRED:		// supports extending an already-expired timeline
		case FCSTAT_STOP_SHADOWED:		// supports restarting a shadowed timeline
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that an analyst can
	// withdraw by changing fc_status (with or without updating analyst data).
	// [Removed] Note: Upon changing fc_status to FCSTAT_STOP_WITHDRAWN, it is recommended
	// to also set analyst_options.extra_forecast_lag = -1L.

	public boolean can_analyst_withdraw () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL:
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_ANALYST:
		case FCSTAT_STOP_EXPIRED:		// supports extending an already-expired timeline
		case FCSTAT_STOP_SHADOWED:		// supports restarting a shadowed timeline
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that an analyst can
	// update (without changing fc_status).

	public boolean can_analyst_update () {
		switch (fc_status) {
		case FCSTAT_ACTIVE_NORMAL:
		case FCSTAT_ACTIVE_INTAKE:
		case FCSTAT_STOP_ANALYST:
		case FCSTAT_STOP_WITHDRAWN:
		case FCSTAT_STOP_EXPIRED:		// supports extending an already-expired timeline
		case FCSTAT_STOP_SHADOWED:		// supports restarting a shadowed timeline
			return true;
		}
		return false;
	}

	// Return true if this timeline is in a state that intake polling can
	// start by changing fc_status (with or without updating analyst data).

	public boolean can_intake_poll_start () {
		switch (fc_status) {
		case FCSTAT_STOP_WITHDRAWN:
			return true;
		}
		return false;
	}

	// Return true if the supplied actcode is one that could be in a
	// state that where can_intake_poll_start would return true.
	// This static function can be used for a quick check to avoid
	// unmarshaling the entire state.

	public static boolean can_actcode_intake_poll_start (int the_actcode) {
		switch (the_actcode) {
		case ACTCODE_WITHDRAWN:
		case ACTCODE_ANALYST:
			return true;
		}
		return false;
	}

	// Return true if this timeline has a result that includes a shadowing event.

	public boolean result_has_shadowing () {
		switch (fc_result) {
		case FCRES_SKIPPED_SHADOWED:
		case FCRES_SKIPPED_FORESHOCK:
			return true;
		}
		return false;
	}

	// Return true if this timeline has a PDL status that includes a successful send.

	public boolean is_pdl_send_successful () {
		switch (pdl_status) {
		case PDLSTAT_SUCCESS:
		case PDLSTAT_CONFLICT:
			return true;
		}
		return false;
	}

	// Return true if this timeline has a PDL product code different from the timeline event id.

	public boolean has_pdl_product_code () {
		if (pdl_product_code.isEmpty()) {
			return false;
		}
		return true;
	}

	// Return the lag of the last forecast, or -1L if there is no last forecast.

	public long get_last_forecast_lag () {
		return last_forecast_stamp.get_forecast_lag();
	}




	//----- Service functions: Insertion -----

	// Set the fc_status variable.

	public void set_fc_status (int the_fc_status) {
		fc_status = the_fc_status;
		return;
	}

	// Set the pdl_status variable.

	public void set_pdl_status (int the_pdl_status) {
		pdl_status = the_pdl_status;
		return;
	}

	// Set the analyst data variables.

	public void set_analyst_data (AnalystOptions the_analyst_options) {
		analyst_options = the_analyst_options;
		return;
	}

	// Set the analyst data variables.
	// This version enforces the rule that analyst_options.extra_forecast_lag must be
	// non-decreasing, and that a new extra forecast request must have lag after
	// the last forecast lag.
	// Note: This function may modify new_analyst_options.
	// Note: The intent is that this will eventually replace set_analyst_data().

	public void set_analyst_data_2 (AnalystOptions new_analyst_options) {

		// The new value of extra_forecast_lag, can be -1L if none requested

		long new_extra_forecast_lag;

		if (analyst_options == null) {
			new_extra_forecast_lag = -1L;
		} else {
			new_extra_forecast_lag = analyst_options.extra_forecast_lag;
		}

		// If the new options are requesting an extra forecast ...

		if (new_analyst_options.extra_forecast_lag >= 0L) {

			// Force the extra forecast lag to be non-decreasing

			new_extra_forecast_lag = Math.max (new_extra_forecast_lag, new_analyst_options.extra_forecast_lag);

			// If there is a prior forecast, also force it to be after the prior forecast lag,
			// otherwise force it to be greater than 0

			if (get_last_forecast_lag() >= 0L) {
				new_extra_forecast_lag = Math.max (new_extra_forecast_lag, get_last_forecast_lag() + 1L);
			} else {
				new_extra_forecast_lag = Math.max (new_extra_forecast_lag, 1L);
			}
		}

		// Insert new value into the new options

		new_analyst_options.extra_forecast_lag = new_extra_forecast_lag;

		// Establish the new options

		analyst_options = new_analyst_options;
		return;
	}

	// Return true if there is an extra forecast pending.
	// Note: This only examine analyst_options.extra_forecast_lag and does not
	// consider the timeline state.

	public boolean is_extra_forecast_pending () {
		if (analyst_options != null) {

			// If no prior forecast, any positive extra lag is OK;
			// otherwise, the extra lag must be greater then the prior lag

			if (get_last_forecast_lag() < 0L) {
				if (analyst_options.extra_forecast_lag >= 0L) {
					return true;
				}
			} else {
				if (analyst_options.extra_forecast_lag > get_last_forecast_lag()) {
					return true;
				}
			}
		}
		return false;
	}

	//  // Merge a value for the extra forecast lag.
	//  // Note: Merge is done using max, so that an extra forecast is generated
	//  // whether either the new or old value requests it.
	//  
	//  public void merge_extra_forecast_lag (long the_extra_forecast_lag) {
	//  	if (analyst_options != null) {
	//  		analyst_options.extra_forecast_lag = Math.max (analyst_options.extra_forecast_lag, the_extra_forecast_lag);
	//  	}
	//  	return;
	//  }

	// Return true if our forecast stamp was constructed from our current analyst options.
	// A false return indicates that analyst options have changed since the last forecast.

	public boolean is_stamp_from_options () {
		return last_forecast_stamp.is_from_options (analyst_options);
	}




	//----- Service functions: State setting -----

	// Set the state to error.

	public void set_state_error (String the_event_id, long the_action_time, long the_entry_time) {

		event_id            = the_event_id;
		actcode             = ACTCODE_ERROR;
		action_time         = the_action_time;
		comcat_ids          = new String[1];
		comcat_ids[0]       = ServerComponent.EVID_ERROR;

		entry_time          = the_entry_time;
		fc_origin           = FCORIG_UNKNOWN;
		fc_status           = FCSTAT_STOP_ERROR;
		pdl_status          = PDLSTAT_NONE;
		pdl_product_code    = "";
		fc_result           = FCRES_NONE;
		shadowing_event_id  = "";

		analyst_options     = null;

		forecast_mainshock  = null;
		forecast_params     = null;
		forecast_results    = null;

		last_forecast_stamp = new ForecastStamp();
		return;
	}

	// Set the state to ComCat failure.

	public void set_state_comcat_fail (long the_entry_time) {

		//event_id            = kept;
		actcode             = ACTCODE_COMCAT_FAIL;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		fc_status           = FCSTAT_STOP_COMCAT_FAIL;
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to shadowed.
	//
	// Note: Currently not used.  The system continues attempting to generate forecasts
	// for a shadowed event, so that the shadowing criterion is periodically re-checked.

	public void set_state_shadowed (long the_entry_time, int the_fc_result, String the_shadowing_event_id) {

		//event_id            = kept;
		actcode             = ACTCODE_SHADOWED;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		fc_status           = FCSTAT_STOP_SHADOWED;
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		fc_result           = the_fc_result;
		shadowing_event_id  = the_shadowing_event_id;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to withdrawn.
	//
	// [Removed] Note: analyst_options.extra_forecast_lag is set to -1L, consuming any analyst forecast request.
	//
	// Note: the_forecast_mainshock can be null, in which case the previous values are kept.

	public void set_state_withdrawn (long the_entry_time, ForecastMainshock the_forecast_mainshock) {

		//event_id            = kept;
		actcode             = ACTCODE_WITHDRAWN;
		action_time         = action_time + 1L;
		if (the_forecast_mainshock != null) {
			comcat_ids          = the_forecast_mainshock.mainshock_id_list;
		}

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		fc_status           = FCSTAT_STOP_WITHDRAWN;
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		if (the_forecast_mainshock != null) {
			forecast_mainshock  = the_forecast_mainshock;
		}
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;

		//analyst_options.extra_forecast_lag = -1L;
		return;
	}

	// Set the state to forecast.
	//
	// Note: fc_status is set to FCSTAT_ACTIVE_NORMAL.  The caller can change fc_status
	// to FCSTAT_STOP_EXPIRED if there are no further forecasts scheduled.
	//
	// Note: pdl_status is set to either PDLSTAT_NONE or PDLSTAT_PENDING depending on the
	// return value of the_forecast_results.get_pdl_model().  The caller should call
	// the_forecast_results.pick_pdl_model() (or set the xxxx_pdl flags in some other way)
	// before calling this function.  If PDLSTAT_PENDING results, the caller will likely
	// want to take further action (such as an immediate attempt to send a report to PDL)
	// and make a futher update to pdl_status.
	//
	// Note: shadowing_event_id is set to "".
	//
	// [Removed] Note: analyst_options.extra_forecast_lag is set to -1L, consuming any analyst forecast request.
	//
	// Note: last_forecast_stamp is set from the_forecast_params.forecast_lag and analyst_options.

	public void set_state_forecast (long the_entry_time, ActionConfig action_config,
			ForecastMainshock the_forecast_mainshock, ForecastParameters the_forecast_params, ForecastResults the_forecast_results) {

		//long min_lag = the_forecast_params.forecast_lag + action_config.get_forecast_min_gap();
		//long next_forecast_lag = action_config.get_next_forecast_lag (min_lag, analyst_options.max_forecast_lag);
		long forecast_time = the_forecast_params.forecast_lag + the_forecast_mainshock.mainshock_time;
		String pdl_json = the_forecast_results.get_pdl_model();

		//event_id            = kept;
		actcode             = ACTCODE_FORECAST;
		action_time         = action_config.floor_unit_lag (forecast_time, action_time);
		comcat_ids          = the_forecast_mainshock.mainshock_id_list;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		//fc_status           = ((next_forecast_lag >= 0L) ? FCSTAT_ACTIVE_NORMAL : FCSTAT_STOP_EXPIRED);
		fc_status           = FCSTAT_ACTIVE_NORMAL;
		pdl_status          = ((pdl_json == null) ? PDLSTAT_NONE : PDLSTAT_PENDING);
		//pdl_product_code    = kept;
		fc_result           = ((pdl_json == null) ? FCRES_FORECAST_NO_PDL : FCRES_FORECAST_PDL);
		shadowing_event_id  = "";

		//analyst_options     = kept;

		forecast_mainshock  = the_forecast_mainshock;
		forecast_params     = the_forecast_params;
		forecast_results    = the_forecast_results;

		last_forecast_stamp = new ForecastStamp (the_forecast_params.forecast_lag, analyst_options);

		//analyst_options.extra_forecast_lag = -1L;
		return;
	}

	// Set the state to skipped.
	//
	// Note: If the event is shadowed then the_shadowing_event_id is the ID of the shadowing
	// event.  Otherwise the event fails the intake filter and the_shadowing_event_id is null.
	//
	// Note: fc_status is set to FCSTAT_ACTIVE_NORMAL.  Exception: fc_status is left unchanged
	// if fc_status is FCSTAT_ACTIVE_INTAKE and the_fc_result is FCRES_SKIPPED_INTAKE or
	// FCRES_SKIPPED_STALE.  In either case, the caller can change fc_status to FCSTAT_STOP_EXPIRED
	// if there are no further forecasts scheduled.
	//
	// Note: pdl_status is set to PDLSTAT_NONE.
	//
	// Note: shadowing_event_id is set to "" if the event is not shadowed.
	//
	// [Removed] Note: analyst_options.extra_forecast_lag is set to -1L, consuming any analyst forecast request.
	//
	// Note: last_forecast_stamp is set from the_forecast_lag and analyst_options.

	public void set_state_skipped (long the_entry_time, ActionConfig action_config,
			ForecastMainshock the_forecast_mainshock, long the_forecast_lag, int the_fc_result, String the_shadowing_event_id) {

		long forecast_time = the_forecast_lag + the_forecast_mainshock.mainshock_time;

		//event_id            = kept;
		actcode             = ACTCODE_SKIPPED;
		action_time         = action_config.floor_unit_lag (forecast_time, action_time);
		comcat_ids          = the_forecast_mainshock.mainshock_id_list;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		if (!( fc_status == FCSTAT_ACTIVE_INTAKE && (the_fc_result == FCRES_SKIPPED_INTAKE || the_fc_result == FCRES_SKIPPED_STALE) )) {
			fc_status           = FCSTAT_ACTIVE_NORMAL;
		}
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		fc_result           = the_fc_result;
		shadowing_event_id  = ((the_shadowing_event_id == null) ? "" : the_shadowing_event_id);

		//analyst_options     = kept;

		forecast_mainshock  = the_forecast_mainshock;
		forecast_params     = null;
		forecast_results    = null;

		last_forecast_stamp = new ForecastStamp (the_forecast_lag, analyst_options);

		//analyst_options.extra_forecast_lag = -1L;
		return;
	}

	// Set the state to PDL update.
	//
	// Note: This keeps the forecast parameters and results, so that it is possible
	// to send a PDL report for a forecast which originally didn't send a report.
	//
	// Edit: The forecast parameters and results are now set to null, because
	// the PDL reporting code now searches back in the timeline for the forecast.

	public void set_state_pdl_update (long the_entry_time, int the_pdl_status) {

		//event_id            = kept;
		actcode             = ACTCODE_PDL_UPDATE;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		//fc_status           = kpet;
		pdl_status          = the_pdl_status;
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		//forecast_params     = kept;
		//forecast_results    = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to expired.

	public void set_state_expired (long the_entry_time) {

		//event_id            = kept;
		actcode             = ACTCODE_EXPIRED;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		if (fc_status == FCSTAT_ACTIVE_NORMAL || fc_status == FCSTAT_ACTIVE_INTAKE) {
			fc_status           = FCSTAT_STOP_EXPIRED;
		}
		if (pdl_status == PDLSTAT_PENDING || pdl_status == PDLSTAT_SECONDARY) {
			pdl_status          = PDLSTAT_NONE;
		}
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to status update.
	//
	// Note: Sets pdl_status to PDLSTAT_NONE.

	public void set_state_status_update (long the_entry_time, int the_fc_status) {

		//event_id            = kept;
		actcode             = ACTCODE_STATUS_UPDATE;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		fc_status           = the_fc_status;
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to analyst intervention.
	//
	// Note: Sets pdl_status to PDLSTAT_NONE.
	//
	// Note: The caller may use set_fc_status(), set_pdl_status(), and set_analyst_data()/set_analyst_data_2()
	// to make changes to forecast status, PDL status, and analyst data.

	public void set_state_analyst_intervention (long the_entry_time) {

		//event_id            = kept;
		actcode             = ACTCODE_ANALYST;
		action_time         = action_time + 1L;
		//comcat_ids          = kept;

		entry_time          = the_entry_time;
		//fc_origin           = kept;
		//fc_status           = kept;
		pdl_status          = PDLSTAT_NONE;
		//pdl_product_code    = kept;
		//fc_result           = kept;
		//shadowing_event_id  = kept;

		//analyst_options     = kept;

		//forecast_mainshock  = kept;
		forecast_params     = null;
		forecast_results    = null;

		//last_forecast_stamp   = kept;
		return;
	}

	// Set the state to track.
	//
	// Note: Sets pdl_status to PDLSTAT_NONE.
	//
	// Note: The caller may use set_fc_status(), set_pdl_status(), and set_analyst_data()/set_analyst_data_2()
	// to make changes to forecast status, PDL status, and analyst data.

	public void set_state_track (long the_entry_time, ActionConfig action_config,
			String the_event_id, ForecastMainshock the_forecast_mainshock, int the_fc_origin, int the_fc_status) {

		event_id            = the_event_id;
		actcode             = ACTCODE_TRACK;
		action_time         = action_config.floor_unit_lag (the_forecast_mainshock.mainshock_time);
		comcat_ids          = the_forecast_mainshock.mainshock_id_list;

		entry_time          = the_entry_time;
		fc_origin           = the_fc_origin;
		fc_status           = the_fc_status;
		pdl_status          = PDLSTAT_NONE;
		pdl_product_code    = "";
		fc_result           = FCRES_NONE;
		shadowing_event_id  = "";

		analyst_options     = new AnalystOptions();

		forecast_mainshock  = the_forecast_mainshock;
		forecast_params     = null;
		forecast_results    = null;

		last_forecast_stamp = new ForecastStamp();
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 27001;
	private static final int MARSHAL_VER_2 = 27002;
	private static final int MARSHAL_VER_3 = 27003;

	private static final String M_VERSION_NAME = "TimelineStatus";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_3;
		writer.marshalInt (M_VERSION_NAME, ver);

		// Superclass

		super.do_marshal (writer);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("TimelineStatus.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			writer.marshalLong                      ("entry_time"         , entry_time         );
			writer.marshalInt                       ("fc_origin"          , fc_origin          );
			writer.marshalInt                       ("fc_status"          , fc_status          );
			writer.marshalInt                       ("pdl_status"         , pdl_status         );
			writer.marshalInt                       ("fc_result"          , fc_result          );
			writer.marshalString                    ("shadowing_event_id" , shadowing_event_id );

			AnalystOptions.marshal_poly     (writer, "analyst_options"    , analyst_options    );

			ForecastMainshock.marshal_poly  (writer, "forecast_mainshock" , forecast_mainshock );
			ForecastParameters.marshal_poly (writer, "forecast_params"    , forecast_params    );
			ForecastResults.marshal_poly    (writer, "forecast_results"   , forecast_results   );

			long last_forecast_lag = last_forecast_stamp.get_forecast_lag();
			writer.marshalLong                      ("last_forecast_lag"  , last_forecast_lag  );

			}
			break;

		case MARSHAL_VER_2: {

			writer.marshalLong                      ("entry_time"         , entry_time         );
			writer.marshalInt                       ("fc_origin"          , fc_origin          );
			writer.marshalInt                       ("fc_status"          , fc_status          );
			writer.marshalInt                       ("pdl_status"         , pdl_status         );
			writer.marshalInt                       ("fc_result"          , fc_result          );
			writer.marshalString                    ("shadowing_event_id" , shadowing_event_id );

			AnalystOptions.marshal_poly     (writer, "analyst_options"    , analyst_options    );

			ForecastMainshock.marshal_poly  (writer, "forecast_mainshock" , forecast_mainshock );
			ForecastParameters.marshal_poly (writer, "forecast_params"    , forecast_params    );
			ForecastResults.marshal_poly    (writer, "forecast_results"   , forecast_results   );

			long last_forecast_lag = last_forecast_stamp.get_forecast_lag();
			writer.marshalLong                      ("last_forecast_lag"  , last_forecast_lag  );

			writer.marshalString                    ("pdl_product_code"   , pdl_product_code   );

			}
			break;

		case MARSHAL_VER_3:

			writer.marshalLong                      ("entry_time"         , entry_time         );
			writer.marshalInt                       ("fc_origin"          , fc_origin          );
			writer.marshalInt                       ("fc_status"          , fc_status          );
			writer.marshalInt                       ("pdl_status"         , pdl_status         );
			writer.marshalInt                       ("fc_result"          , fc_result          );
			writer.marshalString                    ("shadowing_event_id" , shadowing_event_id );

			AnalystOptions.marshal_poly     (writer, "analyst_options"    , analyst_options    );

			ForecastMainshock.marshal_poly  (writer, "forecast_mainshock" , forecast_mainshock );
			ForecastParameters.marshal_poly (writer, "forecast_params"    , forecast_params    );
			ForecastResults.marshal_poly    (writer, "forecast_results"   , forecast_results   );

			ForecastStamp.marshal           (writer, "last_forecast_stamp", last_forecast_stamp);

			writer.marshalString                    ("pdl_product_code"   , pdl_product_code   );

			break;
		}
	
		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_3);

		// Superclass

		super.do_umarshal (reader);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("TimelineStatus.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			entry_time          = reader.unmarshalLong                      ("entry_time"         );
			fc_origin           = reader.unmarshalInt                       ("fc_origin"          , FCORIG_MIN, FCORIG_MAX);
			fc_status           = reader.unmarshalInt                       ("fc_status"          , FCSTAT_MIN, FCSTAT_MAX);
			pdl_status          = reader.unmarshalInt                       ("pdl_status"         , PDLSTAT_MIN, PDLSTAT_MAX);
			fc_result           = reader.unmarshalInt                       ("fc_result"          , FCRES_MIN, FCRES_MAX);
			shadowing_event_id  = reader.unmarshalString                    ("shadowing_event_id" );

			analyst_options     = AnalystOptions.unmarshal_poly     (reader, "analyst_options"    );

			forecast_mainshock  = ForecastMainshock.unmarshal_poly  (reader, "forecast_mainshock" );
			forecast_params     = ForecastParameters.unmarshal_poly (reader, "forecast_params"    );
			forecast_results    = ForecastResults.unmarshal_poly    (reader, "forecast_results"   );

			long last_forecast_lag;
			last_forecast_lag   = reader.unmarshalLong                      ("last_forecast_lag"  );
			last_forecast_stamp = new ForecastStamp (last_forecast_lag);

			pdl_product_code = "";

			}
			break;

		case MARSHAL_VER_2: {

			entry_time          = reader.unmarshalLong                      ("entry_time"         );
			fc_origin           = reader.unmarshalInt                       ("fc_origin"          , FCORIG_MIN, FCORIG_MAX);
			fc_status           = reader.unmarshalInt                       ("fc_status"          , FCSTAT_MIN, FCSTAT_MAX);
			pdl_status          = reader.unmarshalInt                       ("pdl_status"         , PDLSTAT_MIN, PDLSTAT_MAX);
			fc_result           = reader.unmarshalInt                       ("fc_result"          , FCRES_MIN, FCRES_MAX);
			shadowing_event_id  = reader.unmarshalString                    ("shadowing_event_id" );

			analyst_options     = AnalystOptions.unmarshal_poly     (reader, "analyst_options"    );

			forecast_mainshock  = ForecastMainshock.unmarshal_poly  (reader, "forecast_mainshock" );
			forecast_params     = ForecastParameters.unmarshal_poly (reader, "forecast_params"    );
			forecast_results    = ForecastResults.unmarshal_poly    (reader, "forecast_results"   );

			long last_forecast_lag;
			last_forecast_lag   = reader.unmarshalLong                      ("last_forecast_lag"  );
			last_forecast_stamp = new ForecastStamp (last_forecast_lag);

			pdl_product_code    = reader.unmarshalString                    ("pdl_product_code"   );

			}
			break;

		case MARSHAL_VER_3:

			entry_time          = reader.unmarshalLong                      ("entry_time"         );
			fc_origin           = reader.unmarshalInt                       ("fc_origin"          , FCORIG_MIN, FCORIG_MAX);
			fc_status           = reader.unmarshalInt                       ("fc_status"          , FCSTAT_MIN, FCSTAT_MAX);
			pdl_status          = reader.unmarshalInt                       ("pdl_status"         , PDLSTAT_MIN, PDLSTAT_MAX);
			fc_result           = reader.unmarshalInt                       ("fc_result"          , FCRES_MIN, FCRES_MAX);
			shadowing_event_id  = reader.unmarshalString                    ("shadowing_event_id" );

			analyst_options     = AnalystOptions.unmarshal_poly     (reader, "analyst_options"    );

			forecast_mainshock  = ForecastMainshock.unmarshal_poly  (reader, "forecast_mainshock" );
			forecast_params     = ForecastParameters.unmarshal_poly (reader, "forecast_params"    );
			forecast_results    = ForecastResults.unmarshal_poly    (reader, "forecast_results"   );

			last_forecast_stamp = ForecastStamp.unmarshal           (reader, "last_forecast_stamp");

			pdl_product_code    = reader.unmarshalString                    ("pdl_product_code"   );

			break;
		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public TimelineStatus unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a timeline entry.

	@Override
	public TimelineStatus unmarshal_timeline (TimelineEntry tentry) {
		try {
			if (tentry.get_actcode() == ACTCODE_ERROR) {
				throw new MarshalException("Timeline is in an unrecoverable error state, event_id = " + tentry.get_event_id());
			}

			unmarshal (tentry.get_details(), null);

			event_id = tentry.get_event_id();
			actcode = tentry.get_actcode();
			action_time = tentry.get_action_time();
			comcat_ids = tentry.get_comcat_ids();		// clones the array

			if (actcode < ACTCODE_MIN || actcode > ACTCODE_MAX) {
				throw new MarshalException("Timeline entry contains invalid action code, event_id = " + tentry.get_event_id() + ", actcode = " + actcode);
			}

		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling timeline entry payload\n" + tentry.toString() + "\nDump:\n" + tentry.dump_details(), e);
		}
		return this;
	}

}
