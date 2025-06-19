package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;


// Parameters for generating an event-sequence product associated with a forecast.
// Author: Michael Barall 10/25/2022.
//
// This is an immutable object after non-default construction, fetch, or unmarshal.

public class EventSequenceParameters {

	//----- Contents -----

	// Event-sequence reporting option, see ActionConfigFile.ESREP_XXXXX.

	private int evseq_cfg_report;

	public final int get_evseq_cfg_report () {
		return evseq_cfg_report;
	}

	// Event-sequence time before mainshock when an event sequence begins.
	// Required to be in range ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK to ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK.

	private long evseq_cfg_lookback;

	public final long get_evseq_cfg_lookback () {
		return evseq_cfg_lookback;
	}

//	// This version returns 0L if the lookback time is less than or equal to the minimum value.
//
//	public final long get_evseq_cfg_lookback_min_to_zero (long minval) {
//		return ((evseq_cfg_lookback <= minval) ? 0L : evseq_cfg_lookback);
//	}
//
//	public final long get_evseq_cfg_lookback_min_to_zero () {
//		return get_evseq_cfg_lookback_min_to_zero (ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK);
//	}


	//----- Construction -----
	

	// Default constructor.

	public EventSequenceParameters () {
		this.evseq_cfg_report = ActionConfigFile.DEFAULT_EVSEQ_REPORT;
		this.evseq_cfg_lookback = ActionConfigFile.DEFAULT_EVSEQ_LOOKBACK;
	}


	// Construct an object with specified parameters.
	// Parameters:
	//  evseq_cfg_report = Event-sequence reporting option, see ActionConfigFile.ESREP_XXXXX.
	//  evseq_cfg_lookback = Event-sequence time before mainshock when an event sequence begins.
	// Note: This constructor validates its parameters.

	public EventSequenceParameters (int evseq_cfg_report, long evseq_cfg_lookback) {
		if (!( evseq_cfg_report >= ActionConfigFile.ESREP_MIN && evseq_cfg_report <= ActionConfigFile.ESREP_MAX )) {
			throw new IllegalArgumentException ("EventSequenceParameters.EventSequenceParameters: Invalid event-sequence report option: evseq_cfg_report = " + evseq_cfg_report);
		}
		if (!( evseq_cfg_lookback >= ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK && evseq_cfg_lookback <= ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK )) {
			throw new IllegalArgumentException ("EventSequenceParameters.EventSequenceParameters: Invalid event-sequence lookback time: evseq_cfg_lookback = " + evseq_cfg_lookback);
		}
		this.evseq_cfg_report = evseq_cfg_report;
		this.evseq_cfg_lookback = evseq_cfg_lookback;
	}


	// Fetch parameter values.

	public EventSequenceParameters fetch () {

		ActionConfig evseq_action_config = new ActionConfig();

		evseq_cfg_report = evseq_action_config.get_evseq_report();
		evseq_cfg_lookback = evseq_action_config.get_evseq_lookback();
		if (evseq_cfg_lookback < ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK) {
			evseq_cfg_lookback = ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK;
		}
		if (evseq_cfg_lookback > ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK) {
			evseq_cfg_lookback = ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK;
		}

		return this;
	}


	// Make an object with the specified values.
	// This funcction coerces the lookback time to be in range.

	public static EventSequenceParameters make_coerce (int evseq_cfg_report, long evseq_cfg_lookback) {
		return new EventSequenceParameters (evseq_cfg_report, SimpleUtils.clip_max_min_l (
			ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK, ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK, evseq_cfg_lookback));
	}


	// Produce string representation, multi-line and not ending in newline.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("EventSequenceParameters:" + "\n");
		result.append ("  evseq_cfg_report = " + evseq_cfg_report + "\n");
		result.append ("  evseq_cfg_lookback = " + SimpleUtils.duration_raw_and_string (evseq_cfg_lookback));

		return result.toString();
	}


	//----- Special functions -----


	// Get the event-sequence cap time to use when a forecast is blocked.
	// Parameters:
	//  fcmain = Mainshock information.
	//  prior_params = Analyst parameters, can be null if none.

	public static long get_cap_time_for_block (ForecastMainshock fcmain, ForecastParameters prior_params) {
		
		ActionConfig evseq_action_config = new ActionConfig();

		// If event-sequence products are enabled ...

		if (evseq_action_config.get_is_evseq_enabled()) {

			// Try to fetch just the event-sequence parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_evseq_cfg_params (fcmain, prior_params);

			// If we got the parameters ...

			if (params.evseq_cfg_avail) {

				// Delete product if we are in charge of event-sequence

				switch (params.evseq_cfg_params.get_evseq_cfg_report()) {
				case ActionConfigFile.ESREP_REPORT:
				case ActionConfigFile.ESREP_DELETE:
					return PDLCodeChooserEventSequence.CAP_TIME_DELETE;
				}
			}
		}

		// No operation

		return PDLCodeChooserEventSequence.CAP_TIME_NOP;
	}


	// Get the event-sequence cap time to use when a forecast is shadowed.
	// Parameters:
	//  fcmain = Mainshock information.
	//  prior_params = Analyst parameters, can be null if none.
	//  shadow_time = Time of the shadowing event, in milliseconds.

	public static long get_cap_time_for_shadow (ForecastMainshock fcmain, ForecastParameters prior_params, long shadow_time) {
		
		ActionConfig evseq_action_config = new ActionConfig();

		// If event-sequence products are enabled ...

		if (evseq_action_config.get_is_evseq_enabled()) {

			// Try to fetch just the event-sequence parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_evseq_cfg_params (fcmain, prior_params);

			// If we got the parameters ...

			if (params.evseq_cfg_avail) {

				switch (params.evseq_cfg_params.get_evseq_cfg_report()) {

				// If we're reporting, calculate cap time before shadow time,
				// but delete if it is too soon after mainshock time

				case ActionConfigFile.ESREP_REPORT:
					long cap_time = PDLCodeChooserEventSequence.nudge_cap_time (shadow_time - evseq_action_config.get_evseq_cap_gap());
					if (cap_time >= fcmain.mainshock_time + evseq_action_config.get_evseq_cap_min_dur()) {
						return cap_time;
					}
					return PDLCodeChooserEventSequence.CAP_TIME_DELETE;

				// If we're deleting, force delete

				case ActionConfigFile.ESREP_DELETE:
					return PDLCodeChooserEventSequence.CAP_TIME_DELETE;
				}
			}
		}

		// No operation

		return PDLCodeChooserEventSequence.CAP_TIME_NOP;
	}


	// Convert an event-sequence lookback from milliseconds to days.
	// The floating-point version is rounded to 5 decimal places.

	public static double convert_lookback_to_days (long lookback_millis) {
		double lookback_days = ((double)(lookback_millis)) / SimpleUtils.DAY_MILLIS_D;
		return SimpleUtils.round_double_via_string ("%.5f", lookback_days);
	}


	// Convert an event-sequence lookback from days to milliseconds.
	// The result is coerced into an acceptable range.

	public static long convert_lookback_to_millis (double lookback_days) {
		long lookback_millis = Math.round (lookback_days * SimpleUtils.DAY_MILLIS_D);
		if (lookback_millis < ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK) {
			lookback_millis = ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK;
		}
		if (lookback_millis > ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK) {
			lookback_millis = ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK;
		}
		return lookback_millis;
	}


	// Coerce an event-sequence lookback time in days to be in an acceptable range.
	// This is done by converting to milliseconds and back to days.

	public static double coerce_lookback_in_days (double lookback_days) {
		return convert_lookback_to_days (convert_lookback_to_millis (lookback_days));
	}


	// If the supplied parameters contain a lookback value, return it.
	// Otherwise, return the default lookback value from the action configuration.

	public static long get_param_lookback (ForecastParameters fcparams) {
		EventSequenceParameters evseq_param = null;
		if (fcparams != null && fcparams.evseq_cfg_avail) {
			evseq_param = fcparams.evseq_cfg_params;
		}
		if (evseq_param == null) {
			evseq_param = (new EventSequenceParameters()).fetch();
		}
		return evseq_param.get_evseq_cfg_lookback();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 104001;

	private static final String M_VERSION_NAME = "EventSequenceParameters";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 104000;
	protected static final int MARSHAL_EVSEQ_PARAM = 104001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_EVSEQ_PARAM;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("EventSequenceParameters.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			writer.marshalInt  ("evseq_cfg_report"  , evseq_cfg_report  );
			writer.marshalLong ("evseq_cfg_lookback", evseq_cfg_lookback);

		}
		break;

		}
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			evseq_cfg_report   = reader.unmarshalInt  ("evseq_cfg_report", ActionConfigFile.ESREP_MIN, ActionConfigFile.ESREP_MAX);
			evseq_cfg_lookback = reader.unmarshalLong ("evseq_cfg_lookback", ActionConfigFile.REC_MIN_EVSEQ_LOOKBACK, ActionConfigFile.REC_MAX_EVSEQ_LOOKBACK);

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

	public EventSequenceParameters unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, EventSequenceParameters obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static EventSequenceParameters unmarshal_poly (MarshalReader reader, String name) {
		EventSequenceParameters result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("EventSequenceParameters.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_EVSEQ_PARAM:
			result = new EventSequenceParameters();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}
	
}
