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

	// The injectable text to use, or "" if none.
	// Note: In principle, null selects the system default and "" selects empty text.
	// But, the system default is empty text, so for now both set the system default.

	public String injectable_text = "";

	// Return true if we are adjusting injectable text to a non-default value.

	public boolean is_adj_nondef_injectable_text () {
		return (f_adj_injectable_text && has_text (injectable_text));
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
			result.append (SimpleUtils.time_raw_and_string_with_cutoff (nextForecastTime, 0L) + "\n");
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
			result.append (Integer.toString (pdl_model_pmcode) + "\n");
		}

		return result.toString();
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
				if (has_text (injectable_text)) {
					fc_params.injectable_text = injectable_text;
				} else {
					fc_params.injectable_text = ForecastParameters.INJ_TXT_USE_DEFAULT;
				}
			} else {
				adj_save.revert_text_to (fc_params);
			}

			// Event-sequence

			if (f_adj_evseq) {
				if (evseq_cfg_params == null) {
					fc_params.evseq_cfg_fetch_meth = ForecastParameters.FETCH_METH_AUTO;
					if (f_analyst_params) {
						fc_params.evseq_cfg_avail = false;
						fc_params.evseq_cfg_params = null;
					} else {
						fc_params.evseq_cfg_avail = true;
						fc_params.evseq_cfg_params = (new EventSequenceParameters()).fetch();
					}
				} else {
					fc_params.set_analyst_evseq_cfg_params (true, evseq_cfg_params);
				}
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




	//----- Adjustments to AnalystOptions -----




	// Return true if we are adjusting analyst options.

	public boolean is_adj_analyst_options () {
		return (f_adj_analyst_text || is_adj_forecast_parameters());
	}


	// Return true if we are adjusting analyst options to non-default values.

	public boolean is_adj_nondef_analyst_options () {
		return (is_adj_nondef_analyst_text() || is_adj_nondef_forecast_parameters());
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

		// Saved values from analyst_params

		public AdjSaveForecastParameters adj_save_analyst_params = new AdjSaveForecastParameters();

		// Clear to nothing saved.

		public void clear () {
			f_saved = false;

			saved_analyst_id = "";
			saved_analyst_remark = "";

			saved_has_analyst_params = false;
			adj_save_analyst_params.clear();

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

		public void revert_to (AnalystOptions fc_analyst_opts) {
			revert_analyst_text_to (fc_analyst_opts);
			revert_analyst_params_to (fc_analyst_opts);
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
		}

		// Otherwise, revert

		else {
			adj_save.revert_to (fc_analyst_opts);
		}

		return;
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

				saved_generic_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_GENERIC);
				saved_seq_spec_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_SEQ_SPEC);
				saved_bayesian_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_BAYESIAN);
				saved_etas_json = fc_results.get_pdl_model_json (ForecastResults.PMCODE_ETAS);

				saved_pdl_model_pmcode = fc_results.get_selected_pdl_model();
			}
			return;
		}

		// Revert saved values to the forecast results, if they were previously saved.

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
			fc_holder.set_injectable_text (injectable_text);
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
		
}
