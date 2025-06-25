package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_ForecastHolder;


// Holds parameters that can be adjusted after the forecast is computed.
// Author: Michael Barall.

public class AdjustableParameters {


	//----- Internal -----


	// Return true if the string is non-null and non-empty.

	private static boolean has_text (String s) {
		return (s != null && s.length() > 0);
	}





	//----- Adjustable Parameters -----


	// True if injectable text is being adjusted.
	// Used for: forecast JSON, forecast parameters, analyst options.

	public boolean f_adj_injectable_text = false;

	// The injectable text to use, or "" if none, or null to select the system default.

	public String injectable_text = null;

	// Return true if we are adjusting injectable text to a non-default value.

	public boolean is_adj_nondef_injectable_text () {
		return (f_adj_injectable_text && injectable_text != null);
	}

	// Get the injectable text, converting null to "".

	public String get_injectable_text_non_null () {
		return ((injectable_text == null) ? "" : injectable_text);
	}

	// Set the injectable text, converting "" to null.

	public void set_injectable_text_non_empty (String s) {
		if (s.isEmpty()) {
			injectable_text = null;
		} else {
			injectable_text = s;
		}
		return;
	}

	// Get the effective injectable text.
	// If injectable_text is null, the default injectable text is obtained from the action configuration.
	// Note: The return value is always non-null, and is "" if no injectable text is desired.

	public String get_eff_injectable_text () {
		if (injectable_text == null) {
			return (new ActionConfig()).get_def_injectable_text();
		}
		return injectable_text;
	}



	// True if analyst text is being adjusted.
	// Used for: analyst options.

	public boolean f_adj_analyst_text = false;

	// Analyst that most recently reviewed this event, or "" if none.

	public String analyst_id = "";

	// Analyst remark for this event, or "" if none.

	public String analyst_remark = "";

	// Return true if we are adjusting analyst text to a non-default value.

	public boolean is_adj_nondef_analyst_text () {
		return (f_adj_analyst_text && (
			has_text (analyst_id) || has_text (analyst_remark)
		));
	}



	// True if adjusting next forecast time.
	// Used for: forecast json.

	public boolean f_adj_next_forecast_time = false;

	// Scheduled time of next forecast, or 0L if unknown, or -1L if none, or -2L if not specified.

	public long nextForecastTime = 0L;



	// True if adjusting advisory duration.
	// Used for: forecast json.

	public boolean f_adj_advisory_time_frame = false;

	// Describes the amount of time that the advisory is in effect.
	// See ActionConfig.json: "adv_window_names": [ "1 Day", "1 Week", "1 Month", "1 Year" ]
	// See: USGS_AftershockForecast.Duration

	public String advisoryTimeFrame = null;

	// Set the advisory time frame from a USGS_AftershockForecast.Duration.

	public void set_advisoryTimeFrame (USGS_AftershockForecast.Duration the_duration) {
		advisoryTimeFrame = the_duration.toString();
		return;
	}



	// True if adjusing the product template.
	// Used for: forecast json.
	// Note: Not recommended.

	public boolean f_adj_template = false;

	// Identifies the product template.
	// See: USGS_AftershockForecast.Template

	public String template = null;

	// Set the product template from a USGS_AftershockForecast.Template.

	public void set_template (USGS_AftershockForecast.Template the_template) {
		template = the_template.toString();
		return;
	}



	// True if adjusting event-sequence parameters.
	// Used for: forecast parameters, analyst options.

	public boolean f_adj_evseq = false;

	// The event-sequence parameters to use, or null for system default.

	public EventSequenceParameters evseq_cfg_params = null;

	// Return true if adjusting event=sequence parameters to a non-default value.

	public boolean is_adj_nondef_evseq () {
		return f_adj_evseq && evseq_cfg_params != null;
	}



	// True if adjusting forecast model selected for PDL.
	// Used for: forecast results.

	public boolean f_adj_pdl_model = false;

	// The code for the selected PDL model.
	// See: ForecastResults.PMCODE_XXXXX.

	public int pdl_model_pmcode = ForecastResults.PMCODE_INVALID;



	// True if adjusting maximum forecast lag.
	// Used for: analyst options.

	public boolean f_adj_max_forecast_lag = false;

	// The maximum forecast lag, in milliseconds since the mainshock, or 0L for system default.

	public long max_forecast_lag = 0L;

	// Return true if adjusting maximum forecast lag to a non-default value.

	public boolean is_adj_nondef_max_forecast_lag () {
		return (f_adj_max_forecast_lag && max_forecast_lag != 0L);
	}



	// True if adjusting intake options.
	// Used for: analyst options.

	public boolean f_adj_intake = false;

	// Option for intake filtering.

	public int intake_option = AnalystOptions.OPT_INTAKE_NORMAL;

	// Option for shadowing.

	public int shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;

	// Return true if adjusting intake options to a non-default value.

	public boolean is_adj_nondef_intake () {
		return (f_adj_intake && (intake_option != AnalystOptions.OPT_INTAKE_NORMAL || shadow_option != AnalystOptions.OPT_SHADOW_NORMAL));
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("f_adj_injectable_text = " + f_adj_injectable_text + "\n");
		if (f_adj_injectable_text) {
			result.append ("injectable_text = " + ((injectable_text == null) ? "<null>" : injectable_text) + "\n");
		}

		result.append ("f_adj_analyst_text = " + f_adj_analyst_text + "\n");
		if (f_adj_analyst_text) {
			result.append ("analyst_id = " + ((analyst_id == null) ? "<null>" : analyst_id) + "\n");
			result.append ("analyst_remark = " + ((analyst_remark == null) ? "<null>" : analyst_remark) + "\n");
		}

		result.append ("f_adj_next_forecast_time = " + f_adj_next_forecast_time + "\n");
		if (f_adj_next_forecast_time) {
			result.append ("nextForecastTime = " + SimpleUtils.time_raw_and_string_with_cutoff (nextForecastTime, 0L) + "\n");
		}

		result.append ("f_adj_advisory_time_frame = " + f_adj_advisory_time_frame + "\n");
		if (f_adj_advisory_time_frame) {
			result.append ("advisoryTimeFrame = " + ((advisoryTimeFrame == null) ? "<null>" : advisoryTimeFrame) + "\n");
		}

		result.append ("f_adj_template = " + f_adj_template + "\n");
		if (f_adj_template) {
			result.append ("template = " + ((template == null) ? "<null>" : template) + "\n");
		}

		result.append ("f_adj_evseq = " + f_adj_evseq + "\n");
		if (f_adj_evseq) {
			result.append ("evseq_cfg_params = " + ((evseq_cfg_params == null) ? "<null>" : evseq_cfg_params.toString()) + "\n");
		}

		result.append ("f_adj_pdl_model = " + f_adj_pdl_model + "\n");
		if (f_adj_pdl_model) {
			result.append ("pdl_model_pmcode = " + Integer.toString (pdl_model_pmcode) + "\n");
		}

		result.append ("f_adj_max_forecast_lag = " + f_adj_max_forecast_lag + "\n");
		if (f_adj_max_forecast_lag) {
			result.append ("max_forecast_lag = " + SimpleUtils.duration_raw_and_string_2 (max_forecast_lag) + "\n");
		}

		result.append ("f_adj_intake = " + f_adj_intake + "\n");
		if (f_adj_intake) {
			result.append ("intake_option = " + intake_option + "\n");
			result.append ("shadow_option = " + shadow_option + "\n");
		}

		return result.toString();
	}




	// Copy from another object.
	// Returns this object.

	public AdjustableParameters copy_from (AdjustableParameters other) {
		this.f_adj_injectable_text		= other.f_adj_injectable_text;
		this.injectable_text			= other.injectable_text;

		this.f_adj_analyst_text			= other.f_adj_analyst_text;
		this.analyst_id					= other.analyst_id;
		this.analyst_remark				= other.analyst_remark;

		this.f_adj_next_forecast_time	= other.f_adj_next_forecast_time;
		this.nextForecastTime			= other.nextForecastTime;

		this.f_adj_advisory_time_frame	= other.f_adj_advisory_time_frame;
		this.advisoryTimeFrame			= other.advisoryTimeFrame;

		this.f_adj_template				= other.f_adj_template;
		this.template					= other.template;

		this.f_adj_evseq				= other.f_adj_evseq;
		this.evseq_cfg_params			= other.evseq_cfg_params;

		this.f_adj_pdl_model			= other.f_adj_pdl_model;
		this.pdl_model_pmcode			= other.pdl_model_pmcode;

		this.f_adj_max_forecast_lag		= other.f_adj_max_forecast_lag;
		this.max_forecast_lag			= other.max_forecast_lag;

		this.f_adj_intake				= other.f_adj_intake;
		this.intake_option				= other.intake_option;
		this.shadow_option				= other.shadow_option;

		return this;
	}




	//----- Adjustments to ForecastParameters -----




	// Return true if we are adjusting forecast parameters.

	public boolean is_adj_forecast_parameters () {
		return (f_adj_injectable_text || f_adj_evseq);
	}


	// Return true if we are adjusting forecast parameters to non-default values.

	public boolean is_adj_nondef_forecast_parameters () {
		return (is_adj_nondef_injectable_text() || is_adj_nondef_evseq());
	}




	// Class to hold saved values from forecast parameters.

	public static class AdjSaveForecastParameters {

		// True if values have been saved.

		public boolean f_saved = false;

		// Saved injectable text.

		public String saved_injectable_text = ForecastParameters.INJ_TXT_USE_DEFAULT;

		// Saved event-sequence configuration parameters.

		public int saved_evseq_cfg_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
		public boolean saved_evseq_cfg_avail = false;
		public EventSequenceParameters saved_evseq_cfg_params = null;

		// Clear to nothing saved.

		public void clear () {
			f_saved = false;

			saved_injectable_text = ForecastParameters.INJ_TXT_USE_DEFAULT;

			saved_evseq_cfg_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
			saved_evseq_cfg_avail = false;
			saved_evseq_cfg_params = null;

			return;
		}

		// Save from the forecast parameters, if not previously saved.

		public void save_from (ForecastParameters fc_params) {
			if (!( f_saved )) {
				f_saved = true;

				saved_injectable_text = fc_params.injectable_text;

				saved_evseq_cfg_fetch_meth = fc_params.evseq_cfg_fetch_meth;
				saved_evseq_cfg_avail = fc_params.evseq_cfg_avail;
				saved_evseq_cfg_params = fc_params.evseq_cfg_params;
			}
			return;
		}

		// Revert saved values to the forecast parameters, if they were previously saved.

		public void revert_text_to (ForecastParameters fc_params) {
			if (f_saved) {
				fc_params.injectable_text = saved_injectable_text;
			}
			return;
		}

		public void revert_evseq_to (ForecastParameters fc_params) {
			if (f_saved) {
				fc_params.evseq_cfg_fetch_meth = saved_evseq_cfg_fetch_meth;
				fc_params.evseq_cfg_avail = saved_evseq_cfg_avail;
				fc_params.evseq_cfg_params = saved_evseq_cfg_params;
			}
			return;
		}

		public void revert_to (ForecastParameters fc_params) {
			revert_text_to (fc_params);
			revert_evseq_to (fc_params);
			return;
		}
	}




	// Adjust forecast parameters.

	public void adjust_forecast_parameters (AdjSaveForecastParameters adj_save, ForecastParameters fc_params, boolean f_analyst_params) {

		// If adjusting forecast parameters ...

		if (is_adj_forecast_parameters()) {

			// Save if needed

			adj_save.save_from (fc_params);

			// Injectable text

			if (f_adj_injectable_text) {
				fc_params.set_eff_injectable_text (injectable_text, null);
			} else {
				adj_save.revert_text_to (fc_params);
			}

			// Event-sequence

			if (f_adj_evseq) {
				fc_params.set_or_fetch_evseq_cfg_params (evseq_cfg_params, f_analyst_params);
			} else {
				adj_save.revert_evseq_to (fc_params);
			}
		}

		// Otherwise, revert

		else {
			adj_save.revert_to (fc_params);
		}

		return;
	}




	// Class to adjust ForecastParameters and automatically revert on close.

	public class AutoAdjForecastParameters implements AutoCloseable {
		private ForecastParameters fc_params;
		private boolean f_analyst_params;
		private AdjSaveForecastParameters adj_save;

		// Adjust parameters, can be called after construction to re-adjust.

		public final void adjust () {
			adjust_forecast_parameters (adj_save, fc_params, f_analyst_params);
			return;
		}

		// Constructor saves the object being adjusted.

		public AutoAdjForecastParameters (ForecastParameters fc_params, boolean f_analyst_params) {
			this.fc_params = fc_params;
			this.f_analyst_params = f_analyst_params;
			this.adj_save = new AdjSaveForecastParameters();
			adjust();
		}

		// Revert on close.

		@Override
		public void close() {
			adj_save.revert_to (fc_params);
			return;
		}
	}

	// Make an auto-closeable object that adjusts parameters and reverts on close.

	public AutoAdjForecastParameters get_auto_adj (ForecastParameters fc_params, boolean f_analyst_params) {
		return new AutoAdjForecastParameters (fc_params, f_analyst_params);
	}




	//----- Adjustments to AnalystOptions -----




	// Return true if we are adjusting analyst options.

	public boolean is_adj_analyst_options () {
		return (f_adj_analyst_text || is_adj_forecast_parameters() || f_adj_max_forecast_lag || f_adj_intake);
	}


	// Return true if we are adjusting analyst options to non-default values.

	public boolean is_adj_nondef_analyst_options () {
		return (is_adj_nondef_analyst_text() || is_adj_nondef_forecast_parameters() || is_adj_nondef_max_forecast_lag() || is_adj_nondef_intake());
	}




	// Class to hold saved values from analyst options.

	public static class AdjSaveAnalystOptions {

		// True if values have been saved.

		public boolean f_saved = false;

		// Saved analyst text.

		public String saved_analyst_id = "";
		public String saved_analyst_remark = "";

		// True if there were non-null analyst_params.

		public boolean saved_has_analyst_params = false;

		// Saved values from analyst_params.

		public AdjSaveForecastParameters adj_save_analyst_params = new AdjSaveForecastParameters();

		// Saved maximum forecast lag.

		public long saved_max_forecast_lag = 0L;

		// Saved intake options.

		public int saved_intake_option = AnalystOptions.OPT_INTAKE_NORMAL;
		public int saved_shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;

		// Clear to nothing saved.

		public void clear () {
			f_saved = false;

			saved_analyst_id = "";
			saved_analyst_remark = "";

			saved_has_analyst_params = false;
			adj_save_analyst_params.clear();

			saved_max_forecast_lag = 0L;

			saved_intake_option = AnalystOptions.OPT_INTAKE_NORMAL;
			saved_shadow_option = AnalystOptions.OPT_SHADOW_NORMAL;

			return;
		}

		// Save from the analyst options, if not previously saved.

		public void save_from (AnalystOptions fc_analyst_opts) {
			if (!( f_saved )) {
				f_saved = true;

				saved_analyst_id = fc_analyst_opts.analyst_id;
				saved_analyst_remark = fc_analyst_opts.analyst_remark;

				if (fc_analyst_opts.analyst_params == null) {
					saved_has_analyst_params = false;
					adj_save_analyst_params.clear();
				} else {
					saved_has_analyst_params = true;
					adj_save_analyst_params.save_from (fc_analyst_opts.analyst_params);
				}

				saved_max_forecast_lag = fc_analyst_opts.max_forecast_lag;

				saved_intake_option = fc_analyst_opts.intake_option;
				saved_shadow_option = fc_analyst_opts.shadow_option;
			}
			return;
		}

		// Revert saved values to the forecast parameters, if they were previously saved.

		public void revert_analyst_text_to (AnalystOptions fc_analyst_opts) {
			if (f_saved) {
				fc_analyst_opts.analyst_id = saved_analyst_id;
				fc_analyst_opts.analyst_remark = saved_analyst_remark;
			}
			return;
		}

		public void revert_analyst_params_to (AnalystOptions fc_analyst_opts) {
			if (f_saved) {
				if (saved_has_analyst_params) {
					adj_save_analyst_params.revert_to (fc_analyst_opts.analyst_params);
				} else {
					fc_analyst_opts.analyst_params = null;
				}
			}
			return;
		}

		public void revert_max_forecast_lag_to (AnalystOptions fc_analyst_opts) {
			if (f_saved) {
				fc_analyst_opts.max_forecast_lag = saved_max_forecast_lag;
			}
			return;
		}

		public void revert_intake_to (AnalystOptions fc_analyst_opts) {
			if (f_saved) {
				fc_analyst_opts.intake_option = saved_intake_option;
				fc_analyst_opts.shadow_option = saved_shadow_option;
			}
			return;
		}

		public void revert_to (AnalystOptions fc_analyst_opts) {
			revert_analyst_text_to (fc_analyst_opts);
			revert_analyst_params_to (fc_analyst_opts);
			revert_max_forecast_lag_to (fc_analyst_opts);
			revert_intake_to (fc_analyst_opts);
			return;
		}
	}




	// Adjust analyst options.

	public void adjust_analyst_options (AdjSaveAnalystOptions adj_save, AnalystOptions fc_analyst_opts) {

		// If adjusting analyst options ...

		if (is_adj_analyst_options()) {

			// Save if needed

			adj_save.save_from (fc_analyst_opts);

			// Analyst text

			if (f_adj_analyst_text) {
				if (has_text (analyst_id)) {
					fc_analyst_opts.analyst_id = analyst_id;
				} else {
					fc_analyst_opts.analyst_id = "";
				}
				if (has_text (analyst_remark)) {
					fc_analyst_opts.analyst_remark = analyst_remark;
				} else {
					fc_analyst_opts.analyst_remark = "";
				}
			} else {
				adj_save.revert_analyst_text_to (fc_analyst_opts);
			}

			// If adjusting analyst parameters (but not if we're setting default parameters and they were originally null) ...

			if (is_adj_forecast_parameters() && (is_adj_nondef_forecast_parameters() || adj_save.saved_has_analyst_params)) {

				// If there are currently no analyst parameters, create and initialize them

				if (fc_analyst_opts.analyst_params == null) {
					fc_analyst_opts.analyst_params = new ForecastParameters();
					fc_analyst_opts.analyst_params.setup_all_default();
					adj_save.adj_save_analyst_params.clear();
					adj_save.adj_save_analyst_params.save_from (fc_analyst_opts.analyst_params);
				}

				// Adjust the analyst parameters

				adjust_forecast_parameters (adj_save.adj_save_analyst_params, fc_analyst_opts.analyst_params, true);

			} else {
				adj_save.revert_analyst_params_to (fc_analyst_opts);
			}

			// Maximum forecast lag

			if (f_adj_max_forecast_lag) {
				fc_analyst_opts.max_forecast_lag = max_forecast_lag;
			} else {
				adj_save.revert_max_forecast_lag_to (fc_analyst_opts);
			}

			// Intake options

			if (f_adj_intake) {
				fc_analyst_opts.intake_option = intake_option;
				fc_analyst_opts.shadow_option = shadow_option;
			} else {
				adj_save.revert_intake_to (fc_analyst_opts);
			}
		}

		// Otherwise, revert

		else {
			adj_save.revert_to (fc_analyst_opts);
		}

		return;
	}




	// Class to adjust AnalystOptions and automatically revert on close.

	public class AutoAdjAnalystOptions implements AutoCloseable {
		private AnalystOptions fc_analyst_opts;
		private AdjSaveAnalystOptions adj_save;

		// Adjust parameters, can be called after construction to re-adjust.

		public final void adjust () {
			adjust_analyst_options (adj_save, fc_analyst_opts);
			return;
		}

		// Constructor saves the object being adjusted.

		public AutoAdjAnalystOptions (AnalystOptions fc_analyst_opts) {
			this.fc_analyst_opts = fc_analyst_opts;
			this.adj_save = new AdjSaveAnalystOptions();
			adjust();
		}

		// Revert on close.

		@Override
		public void close() {
			adj_save.revert_to (fc_analyst_opts);
			return;
		}
	}

	// Make an auto-closeable object that adjusts parameters and reverts on close.

	public AutoAdjAnalystOptions get_auto_adj (AnalystOptions fc_analyst_opts) {
		return new AutoAdjAnalystOptions (fc_analyst_opts);
	}




	//----- Adjustments to ForecastResults -----




	// Return true if we are adjusting forecast JSON.

	public boolean is_adj_forecast_json () {
		return (f_adj_injectable_text || f_adj_next_forecast_time || f_adj_advisory_time_frame || f_adj_template);
	}


	// Return true if we are adjusting forecast results.

	public boolean is_adj_forecast_results () {
		return (is_adj_forecast_json() || f_adj_pdl_model);
	}




	// Class to hold saved values from forecast results.

	public static class AdjSaveForecastResults {

		// True if values have been saved.

		public boolean f_saved = false;

		// Saved advisory lag.

		public long saved_advisory_lag = 0L;

		// Saved injectable text.

		public String saved_injectable_text = "";

		// Saved forecast JSONs, can be null if none was available.
		// If non-null, it is non-empty.

		public String saved_generic_json = null;
		public String saved_seq_spec_json = null;
		public String saved_bayesian_json = null;
		public String saved_etas_json = null;

		// Saved code for the selected PDL model.

		public int saved_pdl_model_pmcode = ForecastResults.PMCODE_INVALID;;

		// Clear to nothing saved.

		public void clear () {
			f_saved = false;

			saved_advisory_lag = 0L;

			saved_injectable_text = "";

			saved_generic_json = null;
			saved_seq_spec_json = null;
			saved_bayesian_json = null;
			saved_etas_json = null;

			saved_pdl_model_pmcode = ForecastResults.PMCODE_INVALID;;

			return;
		}

		// Save from the forecast results, if not previously saved.

		public void save_from (ForecastResults fc_results) {
			if (!( f_saved )) {
				f_saved = true;

				saved_advisory_lag = fc_results.advisory_lag;

				saved_injectable_text = fc_results.injectable_text;

				saved_generic_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_GENERIC);
				saved_seq_spec_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_SEQ_SPEC);
				saved_bayesian_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_BAYESIAN);
				saved_etas_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_ETAS);

				saved_pdl_model_pmcode = fc_results.get_selected_pdl_model();
			}
			return;
		}

		// Revert saved values to the forecast results, if they were previously saved.

		public void revert_advisory_lag_to (ForecastResults fc_results) {
			if (f_saved) {
				fc_results.advisory_lag = saved_advisory_lag;
			}
			return;
		}

		public void revert_injectable_text_to (ForecastResults fc_results) {
			if (f_saved) {
				fc_results.injectable_text = saved_injectable_text;
			}
			return;
		}

		public void revert_forecast_json_to (ForecastResults fc_results) {
			if (f_saved) {
				if (saved_generic_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_GENERIC, saved_generic_json);
				}
				if (saved_seq_spec_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_SEQ_SPEC, saved_seq_spec_json);
				}
				if (saved_bayesian_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_BAYESIAN, saved_bayesian_json);
				}
				if (saved_etas_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_ETAS, saved_etas_json);
				}
			}
			return;
		}

		public void revert_pdl_model_to (ForecastResults fc_results) {
			if (f_saved) {
				fc_results.set_selected_pdl_model (saved_pdl_model_pmcode);
			}
			return;
		}

		public void revert_to (ForecastResults fc_results) {
			revert_advisory_lag_to (fc_results);
			revert_injectable_text_to (fc_results);
			revert_forecast_json_to (fc_results);
			revert_pdl_model_to (fc_results);
			return;
		}
	}




	// Apply parameters to a forecast JSON.
	// Returns the argument if it is null or empty, or if we are not adjusting the forecast JSON.

	public String adjust_forecast_json (String json_text) {

		// Return the argument if it contains no text or there is nothing to adjust

		if (!( has_text (json_text) && is_adj_forecast_json() )) {
			return json_text;
		}

		// Parse the JSON

		USGS_ForecastHolder fc_holder = new USGS_ForecastHolder();
		MarshalUtils.from_json_string (fc_holder, json_text);
		
		// Adjust injectable text

		if (f_adj_injectable_text) {
			fc_holder.set_injectable_text (get_eff_injectable_text());
		}

		// Adjust next forecast time

		if (f_adj_next_forecast_time) {
			fc_holder.set_next_forecast_time (nextForecastTime);
		}

		// Adjust advisory duration

		if (f_adj_advisory_time_frame) {
			fc_holder.set_advisory_time_frame (advisoryTimeFrame);
		}

		// Adjust product template

		if (f_adj_template) {
			fc_holder.set_template (template);
		}

		// Convert back to JSON

		return MarshalUtils.to_json_string (fc_holder);
	}




	// Adjust forecast results.

	public void adjust_forecast_results (AdjSaveForecastResults adj_save, ForecastResults fc_results) {

		// If adjusting forecast results ...

		if (is_adj_forecast_results()) {

			// Save if needed

			adj_save.save_from (fc_results);

			// Advisory lag

			if (f_adj_advisory_time_frame) {
				long the_advisory_lag = ForecastResults.advisory_name_to_lag_via_enum (advisoryTimeFrame);
				if (the_advisory_lag != 0L) {
					fc_results.advisory_lag = the_advisory_lag;
				} else {
					adj_save.revert_advisory_lag_to (fc_results);
				}
			} else {
				adj_save.revert_advisory_lag_to (fc_results);
			}

			// Injectable text

			if (f_adj_injectable_text) {
				fc_results.injectable_text = get_eff_injectable_text();
			} else {
				adj_save.revert_injectable_text_to (fc_results);
			}

			// Forecast JSON files

			if (is_adj_forecast_json()) {
				if (adj_save.saved_generic_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_GENERIC, adjust_forecast_json (adj_save.saved_generic_json));
				}
				if (adj_save.saved_seq_spec_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_SEQ_SPEC, adjust_forecast_json (adj_save.saved_seq_spec_json));
				}
				if (adj_save.saved_bayesian_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_BAYESIAN, adjust_forecast_json (adj_save.saved_bayesian_json));
				}
				if (adj_save.saved_etas_json != null) {
					fc_results.set_pdl_model_json (ForecastResults.PMCODE_ETAS, adjust_forecast_json (adj_save.saved_etas_json));
				}
			} else {
				adj_save.revert_forecast_json_to (fc_results);
			}

			// Selected PDL model

			if (f_adj_pdl_model) {
				fc_results.set_selected_pdl_model (pdl_model_pmcode);
			} else {
				adj_save.revert_pdl_model_to (fc_results);
			}
		}

		// Otherwise, revert

		else {
			adj_save.revert_to (fc_results);
		}

		return;
	}




	// Class to adjust ForecastResults and automatically revert on close.

	public class AutoAdjForecastResults implements AutoCloseable {
		private ForecastResults fc_results;
		private AdjSaveForecastResults adj_save;

		// Adjust parameters, can be called after construction to re-adjust.

		public final void adjust () {
			adjust_forecast_results (adj_save, fc_results);
			return;
		}

		// Constructor saves the object being adjusted.

		public AutoAdjForecastResults (ForecastResults fc_results) {
			this.fc_results = fc_results;
			this.adj_save = new AdjSaveForecastResults();
			adjust();
		}

		// Revert on close.

		@Override
		public void close() {
			adj_save.revert_to (fc_results);
			return;
		}
	}

	// Make an auto-closeable object that adjusts parameters and reverts on close.

	public AutoAdjForecastResults get_auto_adj (ForecastResults fc_results) {
		return new AutoAdjForecastResults (fc_results);
	}




	//----- Adjustments to ForecastData -----




	// Return true if we are adjusting forecast data.

	public boolean is_adj_forecast_data () {
		return (is_adj_forecast_parameters() || is_adj_forecast_results() || is_adj_analyst_options());
	}




	// Class to hold saved values from forecast data.

	public static class AdjSaveForecastData {

		// True if values have been saved.

		public boolean f_saved = false;

		// Saved parameters, results, analyst options.

		public AdjSaveForecastParameters adj_save_parameters = new AdjSaveForecastParameters();
		public AdjSaveForecastResults adj_save_results = new AdjSaveForecastResults();
		public AdjSaveAnalystOptions adj_save_analyst = new AdjSaveAnalystOptions();

		// Clear to nothing saved.

		public void clear () {
			f_saved = false;

			adj_save_parameters.clear();
			adj_save_results.clear();
			adj_save_analyst.clear();

			return;
		}

		// Save from the forecast data, if not previously saved.

		public void save_from (ForecastData fc_data) {
			if (!( f_saved )) {
				f_saved = true;

				adj_save_parameters.save_from (fc_data.parameters);
				adj_save_results.save_from (fc_data.results);
				adj_save_analyst.save_from (fc_data.analyst);
			}
			return;
		}

		// Revert saved values to the forecast parameters, if they were previously saved.

		public void revert_to (ForecastData fc_data) {
			if (f_saved) {
				adj_save_parameters.revert_to (fc_data.parameters);
				adj_save_results.revert_to (fc_data.results);
				adj_save_analyst.revert_to (fc_data.analyst);
			}
			return;
		}
	}




	// Adjust forecast data.

	public void adjust_forecast_data (AdjSaveForecastData adj_save, ForecastData fc_data) {

		// If adjusting forecast results ...

		if (is_adj_forecast_data()) {

			// Save if needed

			adj_save.save_from (fc_data);

			// Parameters

			adjust_forecast_parameters (adj_save.adj_save_parameters, fc_data.parameters, false);

			// Results

			adjust_forecast_results (adj_save.adj_save_results, fc_data.results);

			// Analyst options

			adjust_analyst_options (adj_save.adj_save_analyst, fc_data.analyst);
		}

		// Otherwise, revert

		else {
			adj_save.revert_to (fc_data);
		}

		return;
	}




	// Class to adjust ForecastData and automatically revert on close.

	public class AutoAdjForecastData implements AutoCloseable {
		private ForecastData fc_data;
		private AdjSaveForecastData adj_save;

		// Adjust parameters, can be called after construction to re-adjust.

		public final void adjust () {
			adjust_forecast_data (adj_save, fc_data);
			return;
		}

		// Constructor saves the object being adjusted.

		public AutoAdjForecastData (ForecastData fc_data) {
			this.fc_data = fc_data;
			this.adj_save = new AdjSaveForecastData();
			adjust();
		}

		// Revert on close.

		@Override
		public void close() {
			adj_save.revert_to (fc_data);
			return;
		}
	}

	// Make an auto-closeable object that adjusts parameters and reverts on close.

	public AutoAdjForecastData get_auto_adj (ForecastData fc_data) {
		return new AutoAdjForecastData (fc_data);
	}




	//----- GUI Support -----




	// Set values from the analyst options of an existing forecast.
	// Parameters:
	//  fc_analyst_opts = Anslyst options of an existing forecast, or null if none.
	// Note: The intent is that the GUI can use this object to capture analyst
	// options from an existing forecast, as a starting point for user selections.

	public void setup_from_analyst_opts (AnalystOptions fc_analyst_opts) {

		// If null, set up using a default analyst options

		AnalystOptions my_opts = fc_analyst_opts;

		if (my_opts == null) {
			my_opts = new AnalystOptions();
			my_opts.setup_all_default();
		}

		// Injectable text

		f_adj_injectable_text = true;
		if (my_opts.analyst_params == null) {
			injectable_text = null;
		} else {
			injectable_text = my_opts.analyst_params.get_raw_injectable_text();
		}

		// Analyst text

		f_adj_analyst_text = true;
		analyst_id = my_opts.analyst_id;
		analyst_remark = my_opts.analyst_remark;

		// Next forecast time, set to default

		f_adj_next_forecast_time = false;
		nextForecastTime = 0L;

		// Advisory duration, set to default

		f_adj_advisory_time_frame = false;
		advisoryTimeFrame = null;

		// Product template, set to default

		f_adj_template = false;
		template = null;

		// Event-sequence parameters

		f_adj_evseq = true;
		if (my_opts.analyst_params == null) {
			evseq_cfg_params = null;
		} else {
			if (my_opts.analyst_params.evseq_cfg_avail) {
				evseq_cfg_params = my_opts.analyst_params.evseq_cfg_params;
			} else {
				evseq_cfg_params = null;
			}
		}

		// PDL model, set to not available

		f_adj_pdl_model = false;
		pdl_model_pmcode = ForecastResults.PMCODE_INVALID;

		// Maximum forecast lag

		f_adj_max_forecast_lag = true;
		max_forecast_lag = my_opts.max_forecast_lag;

		// Intake options

		f_adj_intake = true;
		intake_option = my_opts.intake_option;
		shadow_option = my_opts.shadow_option;

		return;
	}




	// Set values from user-supplied analyst options.
	// Note: The intent is that the GUI can use this object to consolidate analyst
	// options selected by the user..

	public void set_all_analyst_opts (
		String injectable_text,
		String analyst_id,
		String analyst_remark,
		EventSequenceParameters evseq_cfg_params,
		long max_forecast_lag,
		int intake_option,
		int shadow_option
	) {

		// Injectable text

		this.f_adj_injectable_text = true;
		this.injectable_text = injectable_text;

		// Analyst text

		this.f_adj_analyst_text = true;
		this.analyst_id = analyst_id;
		this.analyst_remark = analyst_remark;

		// Next forecast time, set to default

		this.f_adj_next_forecast_time = false;
		this.nextForecastTime = 0L;

		// Advisory duration, set to default

		this.f_adj_advisory_time_frame = false;
		this.advisoryTimeFrame = null;

		// Product template, set to default

		this.f_adj_template = false;
		this.template = null;

		// Event-sequence parameters

		this.f_adj_evseq = true;
		this.evseq_cfg_params = evseq_cfg_params;

		// PDL model, set to not available

		this.f_adj_pdl_model = false;
		this.pdl_model_pmcode = ForecastResults.PMCODE_INVALID;

		// Maximum forecast lag

		this.f_adj_max_forecast_lag = true;
		this.max_forecast_lag = max_forecast_lag;

		// Intake options

		this.f_adj_intake = true;
		this.intake_option = intake_option;
		this.shadow_option = shadow_option;

		return;
	}




	// Set values from user-supplied non-analyst options.
	// Parameters:
	//  nextForecastTime = Time of next forecast, 0L if unknown, -1L if none, -2L if not specified, or Long.MIN_VALUE if not adjusting.
	//  the_duration = Duration for advisory time frame, or null if not adjusting.
	//  the_template = Product template, or null if not adjusting.
	//  pdl_model_pmcode = Code for the selected PDL model, or ForecastResults.PMCODE_INVALID if not adjusting.
	// Note: This function does not set or change any analyst options,
	// so typically you should call set_all_analyst_opts first.

	public void set_all_non_analyst_opts (
		long nextForecastTime,
		USGS_AftershockForecast.Duration the_duration,
		USGS_AftershockForecast.Template the_template,
		int pdl_model_pmcode
	) {

		// Next forecast time, set to default if Long.MIN_VALUE

		if (nextForecastTime == Long.MIN_VALUE) {
			this.f_adj_next_forecast_time = false;
			this.nextForecastTime = 0L;
		} else {
			this.f_adj_next_forecast_time = true;
			this.nextForecastTime = nextForecastTime;
		}

		// Advisory duration, set to default if null

		if (the_duration == null) {
			this.f_adj_advisory_time_frame = false;
			this.advisoryTimeFrame = null;
		} else {
			this.f_adj_advisory_time_frame = true;
			this.advisoryTimeFrame = the_duration.toString();
		}

		// Product template, set to default if null

		if (the_template == null) {
			this.f_adj_template = false;
			this.template = null;
		} else {
			this.f_adj_template = true;
			this.template = the_template.toString();
		}

		// PDL model, set to not available if PMCODE_INVALID

		if (pdl_model_pmcode == ForecastResults.PMCODE_INVALID) {
			this.f_adj_pdl_model = false;
			this.pdl_model_pmcode = ForecastResults.PMCODE_INVALID;
		} else {
			this.f_adj_pdl_model = true;
			this.pdl_model_pmcode = pdl_model_pmcode;
		}

		return;
	}
		
}
