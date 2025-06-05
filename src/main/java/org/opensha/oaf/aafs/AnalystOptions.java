package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

/**
 * Analyst options for modifying a forecast.
 * Author: Michael Barall 07/31/2018.
 */
public class AnalystOptions {

	// Options for intake filtering.

	public static final int OPT_INTAKE_MIN = 1;
	public static final int OPT_INTAKE_NORMAL = 1;		// Normal, automatic intake filtering
	public static final int OPT_INTAKE_IGNORE = 2;		// Ignore intake filter and accept event
	public static final int OPT_INTAKE_BLOCK = 3;		// Treat event as not passing the intake filter
	public static final int OPT_INTAKE_MAX = 3;

	// Options for shadowing.

	public static final int OPT_SHADOW_MIN = 1;
	public static final int OPT_SHADOW_NORMAL = 1;		// Normal, automatic shadowing detection
	public static final int OPT_SHADOW_IGNORE = 2;		// Ignore shadowing and issue forecasts
	public static final int OPT_SHADOW_MAX = 2;

	// Analyst that most recently reviewed this event, or "" if none.

	public String analyst_id = "";

	// Analyst remark for this event, or "" if none.

	public String analyst_remark = "";

	// Time at which analyst reviewed this event, in milliseconds since the epoch, or 0L if none.
	// Note: For analyst options passed through relay items (see ExAnalystSelection), analyst_time
	// must equal relay_time and therefore must be non-zero.
	// Note: System default analyst options have analyst_time == 0L.
	// Note: At present, if a timeline contains analyst options with analyst_time == 0L and
	// there are no analyst selection relay items for the timeline, then the timeline's
	// analyst options are not overwritten by the relay code (see TimelineSupport.update_analyst_options_from_relay).
	// So, if no relay items are used, then analyst_time == 0L indicates that analyst options
	// are to be inherited within the timeline.

	public long analyst_time = 0L;

	// Parameters supplied by the analyst.
	// This can be null, if the analyst has not supplied any parameters,
	// or if the analyst has intervened a second time to "unsupply" parameters.

	public ForecastParameters analyst_params = null;

	// Time lag at which an extra forecast is requested, in milliseconds since the mainshock.
	// The value is -1L if there has been no extra forecast requested.
	// The value 0L can be used to request an extra forecast as soon as possible.
	// Note: In an analyst intervention, this should be close to the current time to request
	// a forecast, or -1L if no forecast is requested.  The value 0L will request a forecast
	// as soon as possible, but it will be a forecast as of approximately the last scheduled
	// forecast time rather than now.
	// Note: In a timeline, if this is not -1L then it is like an extra forecast in the
	// schedule.  Its value is increased whenever new analyst options are received that
	// request a forecast.  It never decreases.
	// Note: An analyst intervention should always have an AnalystOptions with a positive
	// extra_forecast_lag.  Receipt of an analyst intervention causes the server to abandon
	// any forecast in progress (e.g., a forecast requiring a PDL retry).  A positive
	// extra_forecast_lag ensures that the server promptly initiates a new forecast attempt
	// to replace any abandoned forecast.

	public long extra_forecast_lag = -1L;

	// The maximum lag at which to generate forecasts, in milliseconds since the mainshock.
	// The value is 0L specifies to use the configured default.

	public long max_forecast_lag = 0L;

	// Option for intake filtering.

	public int intake_option = OPT_INTAKE_NORMAL;

	// Option for shadowing.

	public int shadow_option = OPT_SHADOW_NORMAL;




	//----- Construction -----

	// Default constructor.

	public AnalystOptions () {
		setup_all_default();
	}


	// Set everything to default.

	public void setup_all_default () {
		analyst_id = "";
		analyst_remark = "";
		analyst_time = 0L;
		analyst_params = null;
		extra_forecast_lag = -1L;
		max_forecast_lag = 0L;
		intake_option = OPT_INTAKE_NORMAL;
		shadow_option = OPT_SHADOW_NORMAL;
		return;
	}


	// Set up the contents.

	public void setup (String the_analyst_id, String the_analyst_remark, long the_analyst_time,
						ForecastParameters the_analyst_params, long the_extra_forecast_lag,
						long the_max_forecast_lag, int the_intake_option, int the_shadow_option) {
		analyst_id = the_analyst_id;
		analyst_remark = the_analyst_remark;
		analyst_time = the_analyst_time;
		analyst_params = the_analyst_params;
		extra_forecast_lag = the_extra_forecast_lag;
		max_forecast_lag = the_max_forecast_lag;
		intake_option = the_intake_option;
		shadow_option = the_shadow_option;
		return;
	}


	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("AnalystOptions:" + "\n");

		result.append ("analyst_id = " + analyst_id + "\n");
		result.append ("analyst_remark = " + analyst_remark + "\n");
		result.append ("analyst_time = " + analyst_time + "\n");
		result.append ("extra_forecast_lag = " + extra_forecast_lag + "\n");
		result.append ("max_forecast_lag = " + max_forecast_lag + "\n");
		result.append ("intake_option = " + intake_option + "\n");
		result.append ("shadow_option = " + shadow_option + "\n");
		result.append ("analyst_params = " + ((analyst_params == null) ? "null\n" : analyst_params.toString()));

		return result.toString();
	}


	// Deep copy everything from the other object.
	// Returns this object.

	public AnalystOptions copy_from (AnalystOptions other) {
		this.analyst_id			= other.analyst_id;
		this.analyst_remark		= other.analyst_remark;
		this.analyst_time		= other.analyst_time;
		if (other.analyst_params == null) {
			this.analyst_params = null;
		} else {
			this.analyst_params = new ForecastParameters();
			this.analyst_params.copy_from (other.analyst_params);
		}
		this.extra_forecast_lag	= other.extra_forecast_lag;
		this.max_forecast_lag	= other.max_forecast_lag;
		this.intake_option		= other.intake_option;
		this.shadow_option		= other.shadow_option;
		return this;
	}




	//----- Service functins -----

	// Return true if intake is blocked (which forces filtering to always occur).

	public boolean is_intake_blocked () {
		return intake_option == OPT_INTAKE_BLOCK;
	}


	// Return true if intake is normal (so that normal intake filtering should occur).

	public boolean is_intake_normal () {
		return intake_option == OPT_INTAKE_NORMAL;
	}


	// Return true if shadowing is normal (so that normal shadow detection should occur).

	public boolean is_shadow_normal () {
		return shadow_option == OPT_SHADOW_NORMAL;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 40001;

	private static final String M_VERSION_NAME = "AnalystOptions";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 40000;
	protected static final int MARSHAL_ANALYST_OPT = 40001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_ANALYST_OPT;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalString                    ("analyst_id"         , analyst_id         );
		writer.marshalString                    ("analyst_remark"     , analyst_remark     );
		writer.marshalLong                      ("analyst_time"       , analyst_time       );
		ForecastParameters.marshal_poly (writer, "analyst_params"     , analyst_params     );
		writer.marshalLong                      ("extra_forecast_lag" , extra_forecast_lag );
		writer.marshalLong                      ("max_forecast_lag"   , max_forecast_lag   );
		writer.marshalInt                       ("intake_option"      , intake_option      );
		writer.marshalInt                       ("shadow_option"      , shadow_option      );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		analyst_id          = reader.unmarshalString                    ("analyst_id"         );
		analyst_remark      = reader.unmarshalString                    ("analyst_remark"     );
		analyst_time        = reader.unmarshalLong                      ("analyst_time"       );
		analyst_params      = ForecastParameters.unmarshal_poly (reader, "analyst_params"     );
		extra_forecast_lag  = reader.unmarshalLong                      ("extra_forecast_lag" );
		max_forecast_lag    = reader.unmarshalLong                      ("max_forecast_lag"   );
		intake_option       = reader.unmarshalInt                       ("intake_option"      , OPT_INTAKE_MIN, OPT_INTAKE_MAX);
		shadow_option       = reader.unmarshalInt                       ("shadow_option"      , OPT_SHADOW_MIN, OPT_SHADOW_MAX);

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

	public AnalystOptions unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, AnalystOptions obj) {

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

	public static AnalystOptions unmarshal_poly (MarshalReader reader, String name) {
		AnalystOptions result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("AnalystOptions.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_ANALYST_OPT:
			result = new AnalystOptions();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
