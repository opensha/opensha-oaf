package org.opensha.oaf.oetas.env;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OECatalogParamsMags;
import org.opensha.oaf.oetas.OESimulationParams;

import org.opensha.oaf.oetas.util.OEDiscreteRange;

import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;

import org.opensha.oaf.oetas.fit.OEGridParams;
import org.opensha.oaf.oetas.fit.OEGridOptions;
import org.opensha.oaf.oetas.fit.OEDiscFGHParams;
import org.opensha.oaf.oetas.fit.OEMagCompFnDisc;
import org.opensha.oaf.oetas.fit.OEDisc2Grouping;


// Class to hold parameters for running operational ETAS.
// Author: Michael Barall 05/04/2022.
//
// This class is designed so that multiple objects can be merged.
// Each merged-in object can override some, all, or none of the parameters.
// In use, we start with typical values, merge in global values, then
// merge in regional values, then merge in analyst-supplied values.

public class OEtasParameters implements Marshalable {




	//----- History parameters -----

	// History parameters available flag.

	public boolean hist_params_avail = false;

	//-- Discretization parameters

	// Incompleteness discretization delta between discrete values.  See OEMagCompFnDiscFGH.make_disc_even().

	public double disc_delta = 0.0;

	//-- Splitting parameters

	// Maximum number of ruptures that can have magnitudes >= magCat, or 0 if no limit (limit is flexible);
	// the value of magCat is increased if necessary to reduce the number of ruptures.

	public int mag_cat_count = 0;

	// Maximum number of eligible (to generate incompleteness) ruptures, or 0 if no limit (limit is flexible);
	// the minimum magnitude (for eligibility) is increased if necessary to reduce the number of ruptures.

	public int eligible_count = 0;

	// Duration limit ratio, minimum, and maximum.
	// These are used to calculate the maximum allowed duration of an interval according to
	// min(durlim_max, max (durlim_min, durlim_ratio*(t - tsplit)))
	// where t is the start of the interval and tsplit is the most recent split time.

	// Duration limit ratio, must be >= 0.

	public double durlim_ratio = 0.0;

	// Duration limit minimum, in days, must be > 0.

	public double durlim_min = 0.0;

	// Duration limit maximum, in days, must be >= durlim_min.

	public double durlim_max = 0.0;

	//-- Joining Parameters

	// Maximum number of ruptures allowed before the first required split (limit is flexible),
	// or 0 or -1 if no limit.  If non-zero, them mag_cat_count applies only to ruptures
	// after the first required split.  If zero, then mag_cat_count applies to all ruptures.

	public int before_max_count = 0;

	// Option to join intervals whose magnitude of completeness is magCat: 0 = no join, 1 = join.

	public int mag_cat_int_join = 0;


	//-- Operations

	// Clear history parameters.

	public final void clear_hist_params () {
		hist_params_avail = false;

		disc_delta       = 0.0;

		mag_cat_count    = 0;
		eligible_count   = 0;
		durlim_ratio     = 0.0;
		durlim_min       = 0.0;
		durlim_max       = 0.0;

		before_max_count = 0;
		mag_cat_int_join = 0;
		return;
	}

	// Set history parameters to typical values.

	public final void set_hist_params_to_typical () {
		hist_params_avail = true;

		disc_delta       = OEConstants.DEF_DISC_DELTA;

		mag_cat_count    = OEConstants.DEF_MAG_CAT_COUNT;
		eligible_count   = OEConstants.DEF_ELIGIBLE_COUNT;
		durlim_ratio     = OEConstants.DEF_DURLIM_RATIO;
		durlim_min       = OEConstants.DEF_DURLIM_MIN;
		durlim_max       = OEConstants.DEF_DURLIM_MAX;

		before_max_count = OEConstants.DEF_BEFORE_MAX_COUNT;
		mag_cat_int_join = OEConstants.DEF_MAG_CAT_INT_JOIN;
		return;
	}

	// Copy history parameters from another object.

	public final void copy_hist_params_from (OEtasParameters other) {
		hist_params_avail = other.hist_params_avail;

		disc_delta       = other.disc_delta;

		mag_cat_count    = other.mag_cat_count;
		eligible_count   = other.eligible_count;
		durlim_ratio     = other.durlim_ratio;
		durlim_min       = other.durlim_min;
		durlim_max       = other.durlim_max;

		before_max_count = other.before_max_count;
		mag_cat_int_join = other.mag_cat_int_join;
		return;
	}

	// Set the history parameters to analyst values.

	public final void set_hist_params_to_analyst (
		boolean hist_params_avail,
		double disc_delta,
		int mag_cat_count,
		int eligible_count,
		double durlim_ratio,
		double durlim_min,
		double durlim_max,
		int before_max_count,
		int mag_cat_int_join
	) {
		this.hist_params_avail = hist_params_avail;

		this.disc_delta       = disc_delta;

		this.mag_cat_count    = mag_cat_count;
		this.eligible_count   = eligible_count;
		this.durlim_ratio     = durlim_ratio;
		this.durlim_min       = durlim_min;
		this.durlim_max       = durlim_max;

		this.before_max_count = before_max_count;
		this.mag_cat_int_join = mag_cat_int_join;
		return;
	}

	// Merge history parameters from another object, if available.

	public final void merge_hist_params_from (OEtasParameters other) {
		if (other != null) {
			if (other.hist_params_avail) {
				copy_hist_params_from (other);
			}
		}
		return;
	}

	// Check history parameters invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_hist_params_invariant () {
		if (hist_params_avail) {
			if (!( disc_delta > 0.0 )) {
				return "Invalid history parameters: disc_delta = " + disc_delta;
			}
			if (!( mag_cat_count >= 0 )) {
				return "Invalid history parameters: mag_cat_count = " + mag_cat_count;
			}
			if (!( eligible_count >= 0 )) {
				return "Invalid history parameters: eligible_count = " + eligible_count;
			}
			if (!( durlim_ratio >= 0.0 )) {
				return "Invalid history parameters: durlim_ratio = " + durlim_ratio;
			}
			if (!( durlim_min > 0.0 && durlim_max >= durlim_min )) {
				return "Invalid history parameters: durlim_min = " + durlim_min + ", durlim_max = " + durlim_max;
			}
			if (!( before_max_count >= -1 )) {
				return "Invalid history parameters: before_max_count = " + before_max_count;
			}
			if (!( mag_cat_int_join >= 0 && mag_cat_int_join <= 1 )) {
				return "Invalid history parameters: mag_cat_int_join = " + mag_cat_int_join;
			}
		}
		return null;
	}

	// Append a string representation of the history parameters.

	public final StringBuilder hist_params_append_string (StringBuilder sb) {
		sb.append ("hist_params_avail = " + hist_params_avail + "\n");
		if (hist_params_avail) {
			sb.append ("disc_delta = " + disc_delta + "\n");
			sb.append ("mag_cat_count = " + mag_cat_count + "\n");
			sb.append ("eligible_count = " + eligible_count + "\n");
			sb.append ("durlim_ratio = " + durlim_ratio + "\n");
			sb.append ("durlim_min = " + durlim_min + "\n");
			sb.append ("durlim_max = " + durlim_max + "\n");
			sb.append ("before_max_count = " + before_max_count + "\n");
			sb.append ("mag_cat_int_join = " + mag_cat_int_join + "\n");
		}
		return sb;
	}

	// Marshal history parameters.

	private void marshal_hist_params_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("hist_params_avail", hist_params_avail);
		if (hist_params_avail) {
			writer.marshalDouble ("disc_delta", disc_delta);
			writer.marshalInt ("mag_cat_count", mag_cat_count);
			writer.marshalInt ("eligible_count", eligible_count);
			writer.marshalDouble ("durlim_ratio", durlim_ratio);
			writer.marshalDouble ("durlim_min", durlim_min);
			writer.marshalDouble ("durlim_max", durlim_max);
			writer.marshalInt ("before_max_count", before_max_count);
			writer.marshalInt ("mag_cat_int_join", mag_cat_int_join);
		}
		return;
	}

	// Unmarshal history parameters.

	private void unmarshal_hist_params_v1 (MarshalReader reader) {
		hist_params_avail = reader.unmarshalBoolean ("hist_params_avail");
		if (hist_params_avail) {
			disc_delta = reader.unmarshalDouble ("disc_delta");
			mag_cat_count = reader.unmarshalInt ("mag_cat_count");
			eligible_count = reader.unmarshalInt ("eligible_count");
			durlim_ratio = reader.unmarshalDouble ("durlim_ratio");
			durlim_min = reader.unmarshalDouble ("durlim_min");
			durlim_max = reader.unmarshalDouble ("durlim_max");
			before_max_count = reader.unmarshalInt ("before_max_count");
			mag_cat_int_join = reader.unmarshalInt ("mag_cat_int_join");
		} else {
			clear_hist_params();
		}

		// Check the invariant

		String inv = check_hist_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_hist_params_v1: " + inv);
		}
		return;
	}

	// Make the history parameters.
	// Parameters:
	//  magCat = Catalog magnitude of completeness.
	//  capF = Helmstetter parameter F.
	//  capG = Helmstetter parameter G, or 100.0 (== HELM_CAPG_DISABLE) to disable time-dependent magnitude of completeness.
	//  capH = Helmstetter parameter H.
	//  t_range_begin = Beginning of time range covered by the catalog, in days.
	//  t_range_end = Ending of time range covered by the catalog, in days.
	//  t_interval_begin = Time within catalog where the intervals and fitting begins, in days.
	// Must satisfy:  t_range_begin <= t_interval_begin < t_range_end .
	// Note: Caller must check the history parameters are available.

	public final OEDiscFGHParams make_hist_params (
		double magCat,
		double capF,
		double capG,
		double capH,
		double t_range_begin,
		double t_range_end,
		double t_interval_begin
	) {
		if (!( hist_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_hist_params: history parameters not available");
		}

		// Make time-splitting function

		OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

		// Make the history parameters

		OEDiscFGHParams hist_params = new OEDiscFGHParams();

		hist_params.set_to_common (
			magCat,
			capF,
			capG,
			capH,
			t_range_begin,
			t_range_end,
			disc_delta,
			mag_cat_count,
			eligible_count,
			split_fn,
			t_interval_begin,
			before_max_count,
			mag_cat_int_join
		);

		return hist_params;
	}

	// Make the history parameters.
	// Parameters:
	//  etas_cat_info = Catalog information.
	//  t_early = Time of earliest rupture to include in history, in days.
	// Note: The requirement for t_early is that it be less than or equal to the time of any
	// ruptures not included in the data range (etas_cat_info.t_data_begin thru etas_cat_info.t_data_end).
	// If there are no such rupture, t_early can have the value etas_cat_info.t_data_begin.
	// Often t_early can contain the time of the mainshock, which is typically 0.0.
	// Note: Caller must check the history parameters are available.

	public final OEDiscFGHParams make_hist_params (
		OEtasCatalogInfo etas_cat_info,
		double t_early
	) {
		return make_hist_params (
			etas_cat_info.magCat,
			etas_cat_info.capF,
			etas_cat_info.capG,
			etas_cat_info.capH,
			Math.min (t_early, etas_cat_info.t_data_begin),
			etas_cat_info.t_data_end,
			etas_cat_info.t_fitting
		);
	}




	//----- Source grouping -----

	// Source grouping parameters available flag.

	public boolean group_params_avail = false;

	//-- Span width function parameters (see OEDisc2Grouping.SpanWidthFcnRatio)

	// Group span width relative base time, in days.

	public double gs_rel_base_time = 0.0;

	// Group span width ratio.

	public double gs_ratio = 0.0;

	// Group span minimum width, in days.

	public double gs_min_width = 0.0;

	//-- Rupture width function parameters (see OEDisc2Grouping.RupWidthFcnTaper)

	// Group rupture width high magnitude delta.

	public double gr_hi_mag_delta = 0.0;

	// Group rupture magnitude delta during which ratio tapers between lo and hi ratios.

	public double gr_taper_mag_delta = 0.0;

	// Group rupture width initial magnitude.

	public double gr_init_mag = 0.0;

	// Group rupture width low ratio.

	public double gr_lo_ratio = 0.0;

	// Group rupture width high ratio.

	public double gr_hi_ratio = 0.0;


	//-- Operations

	// Clear source grouping parameters.

	public final void clear_group_params () {
		group_params_avail = false;

		gs_rel_base_time   = 0.0;
		gs_ratio           = 0.0;
		gs_min_width       = 0.0;

		gr_hi_mag_delta    = 0.0;
		gr_taper_mag_delta = 0.0;
		gr_init_mag        = 0.0;
		gr_lo_ratio        = 0.0;
		gr_hi_ratio        = 0.0;
		return;
	}

	// Set source grouping parameters to typical values.

	public final void set_group_params_to_typical () {
		group_params_avail = true;

		gs_rel_base_time   = OEConstants.DEF_GS_REL_BASE_TIME;
		gs_ratio           = OEConstants.DEF_GS_RATIO;
		gs_min_width       = OEConstants.DEF_GS_MIN_WIDTH;

		gr_hi_mag_delta    = OEConstants.DEF_GR_HI_MAG_DELTA;
		gr_taper_mag_delta = OEConstants.DEF_GR_TAPER_MAG_DELTA;
		gr_init_mag        = OEConstants.DEF_GR_INIT_MAG;
		gr_lo_ratio        = OEConstants.DEF_GR_LO_RATIO;
		gr_hi_ratio        = OEConstants.DEF_GR_HI_RATIO;
		return;
	}

	// Copy source grouping parameters from another object.

	public final void copy_group_params_from (OEtasParameters other) {
		group_params_avail = other.group_params_avail;

		gs_rel_base_time   = other.gs_rel_base_time;
		gs_ratio           = other.gs_ratio;
		gs_min_width       = other.gs_min_width;

		gr_hi_mag_delta    = other.gr_hi_mag_delta;
		gr_taper_mag_delta = other.gr_taper_mag_delta;
		gr_init_mag        = other.gr_init_mag;
		gr_lo_ratio        = other.gr_lo_ratio;
		gr_hi_ratio        = other.gr_hi_ratio;
		return;
	}

	// Set the source grouping parameters to analyst values.

	public final void set_group_params_to_analyst (
		boolean group_params_avail,
		double gs_rel_base_time,
		double gs_ratio,
		double gs_min_width,
		double gr_hi_mag_delta,
		double gr_taper_mag_delta,
		double gr_init_mag,
		double gr_lo_ratio,
		double gr_hi_ratio
	) {
		this.group_params_avail = group_params_avail;

		this.gs_rel_base_time   = gs_rel_base_time;
		this.gs_ratio           = gs_ratio;
		this.gs_min_width       = gs_min_width;

		this.gr_hi_mag_delta    = gr_hi_mag_delta;
		this.gr_taper_mag_delta = gr_taper_mag_delta;
		this.gr_init_mag        = gr_init_mag;
		this.gr_lo_ratio        = gr_lo_ratio;
		this.gr_hi_ratio        = gr_hi_ratio;
		return;
	}

	// Merge source grouping parameters from another object, if available.

	public final void merge_group_params_from (OEtasParameters other) {
		if (other != null) {
			if (other.group_params_avail) {
				copy_group_params_from (other);
			}
		}
		return;
	}

	// Check source grouping parameters invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_group_params_invariant () {
		if (group_params_avail) {
			if (!( gs_ratio >= 0.0 )) {
				return "Invalid source grouping parameters: gs_ratio = " + gs_ratio;
			}
			if (!( gs_min_width >= 0.0 )) {
				return "Invalid source grouping parameters: gs_min_width = " + gs_min_width;
			}
			if (!( gr_hi_mag_delta >= 0.0 )) {
				return "Invalid source grouping parameters: gr_hi_mag_delta = " + gr_hi_mag_delta;
			}
			if (!( gr_taper_mag_delta >= 0.0 )) {
				return "Invalid source grouping parameters: gr_taper_mag_delta = " + gr_taper_mag_delta;
			}
			if (!( gr_lo_ratio >= 0.0 && gr_hi_ratio >= gr_lo_ratio && 1.0 >= gr_hi_ratio )) {
				return "Invalid source grouping parameters: gr_lo_ratio = " + gr_lo_ratio + ", gr_hi_ratio = " + gr_hi_ratio;
			}
		}
		return null;
	}

	// Append a string representation of the source grouping parameters.

	public final StringBuilder group_params_append_string (StringBuilder sb) {
		sb.append ("group_params_avail = " + group_params_avail + "\n");
		if (group_params_avail) {
			sb.append ("gs_rel_base_time = " + gs_rel_base_time + "\n");
			sb.append ("gs_ratio = " + gs_ratio + "\n");
			sb.append ("gs_min_width = " + gs_min_width + "\n");
			sb.append ("gr_hi_mag_delta = " + gr_hi_mag_delta + "\n");
			sb.append ("gr_taper_mag_delta = " + gr_taper_mag_delta + "\n");
			sb.append ("gr_init_mag = " + gr_init_mag + "\n");
			sb.append ("gr_lo_ratio = " + gr_lo_ratio + "\n");
			sb.append ("gr_hi_ratio = " + gr_hi_ratio + "\n");
		}
		return sb;
	}

	// Marshal source grouping parameters.

	private void marshal_group_params_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("group_params_avail", group_params_avail);
		if (group_params_avail) {
			writer.marshalDouble ("gs_rel_base_time", gs_rel_base_time);
			writer.marshalDouble ("gs_ratio", gs_ratio);
			writer.marshalDouble ("gs_min_width", gs_min_width);
			writer.marshalDouble ("gr_hi_mag_delta", gr_hi_mag_delta);
			writer.marshalDouble ("gr_taper_mag_delta", gr_taper_mag_delta);
			writer.marshalDouble ("gr_init_mag", gr_init_mag);
			writer.marshalDouble ("gr_lo_ratio", gr_lo_ratio);
			writer.marshalDouble ("gr_hi_ratio", gr_hi_ratio);
		}
		return;
	}

	// Unmarshal source grouping parameters.

	private void unmarshal_group_params_v1 (MarshalReader reader) {
		group_params_avail = reader.unmarshalBoolean ("group_params_avail");
		if (group_params_avail) {
			gs_rel_base_time = reader.unmarshalDouble ("gs_rel_base_time");
			gs_ratio = reader.unmarshalDouble ("gs_ratio");
			gs_min_width = reader.unmarshalDouble ("gs_min_width");
			gr_hi_mag_delta = reader.unmarshalDouble ("gr_hi_mag_delta");
			gr_taper_mag_delta = reader.unmarshalDouble ("gr_taper_mag_delta");
			gr_init_mag = reader.unmarshalDouble ("gr_init_mag");
			gr_lo_ratio = reader.unmarshalDouble ("gr_lo_ratio");
			gr_hi_ratio = reader.unmarshalDouble ("gr_hi_ratio");
		} else {
			clear_group_params();
		}

		// Check the invariant

		String inv = check_group_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_group_params_v1: " + inv);
		}
		return;
	}

	// Get the group span width function.
	// Note: Caller must check the source grouping parameters are available.

	public final OEDisc2Grouping.SpanWidthFcn get_span_width_fcn () {
		if (!( group_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_span_width_fcn: Source grouping parameters not available");
		}
		OEDisc2Grouping.SpanWidthFcn span_width_fcn = new OEDisc2Grouping.SpanWidthFcnRatio (gs_rel_base_time, gs_ratio, gs_min_width);
		return span_width_fcn;
	}

	// Get the group rupture width function.
	// Note: Caller must check the source grouping parameters are available.

	public final OEDisc2Grouping.RupWidthFcn get_rup_width_fcn () {
		if (!( group_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_rup_width_fcn: Source grouping parameters not available");
		}
		OEDisc2Grouping.RupWidthFcn rup_width_fcn = new OEDisc2Grouping.RupWidthFcnTaper (gr_lo_ratio, gr_hi_ratio, gr_hi_mag_delta, gr_taper_mag_delta, gr_init_mag);
		return rup_width_fcn;
	}




	//----- Fitting parameters -----

	// Fitting parameters available flag.

	public boolean fit_params_avail = false;

	// True to use intervals to fill in below magnitude of completeness.

	public boolean fit_f_intervals = false;

	// Option to select magnitude range for log-likelihood calculation (LMR_OPT_XXXX).

	public int fit_lmr_opt = OEConstants.LMR_OPT_MCT_INFINITY;

	// Clear fitting parameters.

	public final void clear_fit_params () {
		fit_params_avail = false;

		fit_f_intervals = false;
		fit_lmr_opt = OEConstants.LMR_OPT_MCT_INFINITY;
		return;
	}

	// Set fitting parameters to typical values.

	public final void set_fit_params_to_typical () {
		fit_params_avail = true;

		fit_f_intervals = OEConstants.DEF_F_INTERVALS;
		fit_lmr_opt = OEConstants.DEF_LMR_OPT;
		return;
	}

	// Copy fitting parameters from another object.

	public final void copy_fit_params_from (OEtasParameters other) {
		fit_params_avail = other.fit_params_avail;

		fit_f_intervals = other.fit_f_intervals;
		fit_lmr_opt = other.fit_lmr_opt;
		return;
	}

	// Set the fitting parameters to analyst values.

	public final void set_fit_params_to_analyst (
		boolean fit_params_avail,
		boolean fit_f_intervals,
		int fit_lmr_opt
	) {
		this.fit_params_avail = fit_params_avail;
		this.fit_f_intervals = fit_f_intervals;
		this.fit_lmr_opt = fit_lmr_opt;
		return;
	}

	// Merge fitting parameters from another object, if available.

	public final void merge_fit_params_from (OEtasParameters other) {
		if (other != null) {
			if (other.fit_params_avail) {
				copy_fit_params_from (other);
			}
		}
		return;
	}

	// Check fitting parameters invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_fit_params_invariant () {
		if (fit_params_avail) {
			if (!( fit_lmr_opt >= OEConstants.LMR_OPT_MIN && fit_lmr_opt <= OEConstants.LMR_OPT_MAX )) {
				return "Invalid fitting parameter: fit_lmr_opt = " + fit_lmr_opt;
			}
		}
		return null;
	}

	// Append a string representation of the fitting parameters.

	public final StringBuilder fit_params_append_string (StringBuilder sb) {
		sb.append ("fit_params_avail = " + fit_params_avail + "\n");
		if (fit_params_avail) {
			sb.append ("fit_f_intervals = " + fit_f_intervals + "\n");
			sb.append ("fit_lmr_opt = " + fit_lmr_opt + "\n");
		}
		return sb;
	}

	// Marshal fitting parameters.

	private void marshal_fit_params_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("fit_params_avail", fit_params_avail);
		if (fit_params_avail) {
			writer.marshalBoolean ("fit_f_intervals", fit_f_intervals);
			writer.marshalInt ("fit_lmr_opt", fit_lmr_opt);
		}
		return;
	}

	// Unmarshal fitting parameters.

	private void unmarshal_fit_params_v1 (MarshalReader reader) {
		fit_params_avail = reader.unmarshalBoolean ("fit_params_avail");
		if (fit_params_avail) {
			fit_f_intervals = reader.unmarshalBoolean ("fit_f_intervals");
			fit_lmr_opt = reader.unmarshalInt ("fit_lmr_opt");
		} else {
			clear_fit_params();
		}

		// Check the invariant

		String inv = check_fit_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_fit_params_v1: " + inv);
		}
		return;
	}

	// Get the fitting parameter f_interval.
	// Note: Caller must check the fitting parameters are available.

	public final boolean get_fit_f_interval () {
		if (!( fit_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_fit_f_interval: Fitting parameters not available");
		}
		return fit_f_intervals;
	}

	// Get the fitting parameter lmr_opt.
	// Note: Caller must check the fitting parameters are available.

	public final int get_fit_lmr_opt () {
		if (!( fit_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_fit_lmr_opt: Fitting parameters not available");
		}
		return fit_lmr_opt;
	}




	//----- Magnitude range for fitting -----

	// Fitting magnitude range available flag.

	public boolean fmag_range_avail = false;

	// Minimum magnitude range above magnitude of completeness, for parameter fitting.

	public double fmag_above_mag_cat = OEConstants.DEF_FMAG_ABOVE_MAG_CAT;

	// Minimum magnitude range above maximum magnitude in catalog, for parameter fitting.

	public double fmag_above_mag_max = OEConstants.DEF_FMAG_ABOVE_MAG_MAX;

	// Clear fitting magnitude range.

	public final void clear_fmag_range () {
		fmag_range_avail = false;

		fmag_above_mag_cat = OEConstants.DEF_FMAG_ABOVE_MAG_CAT;
		fmag_above_mag_max = OEConstants.DEF_FMAG_ABOVE_MAG_MAX;
		return;
	}

	// Set fitting magnitude range to typical values.

	public final void set_fmag_range_to_typical () {
		fmag_range_avail = true;

		fmag_above_mag_cat = OEConstants.DEF_FMAG_ABOVE_MAG_CAT;
		fmag_above_mag_max = OEConstants.DEF_FMAG_ABOVE_MAG_MAX;
		return;
	}

	// Copy fitting magnitude range from another object.

	public final void copy_fmag_range_from (OEtasParameters other) {
		fmag_range_avail = other.fmag_range_avail;

		fmag_above_mag_cat = other.fmag_above_mag_cat;
		fmag_above_mag_max = other.fmag_above_mag_max;
		return;
	}

	// Set the fitting magnitude range to analyst values.

	public final void set_fmag_range_to_analyst (
		boolean fmag_range_avail,
		double fmag_above_mag_cat,
		double fmag_above_mag_max
	) {
		this.fmag_range_avail = fmag_range_avail;
		this.fmag_above_mag_cat = fmag_above_mag_cat;
		this.fmag_above_mag_max = fmag_above_mag_max;
		return;
	}

	// Merge fitting magnitude range from another object, if available.

	public final void merge_fmag_range_from (OEtasParameters other) {
		if (other != null) {
			if (other.fmag_range_avail) {
				copy_fmag_range_from (other);
			}
		}
		return;
	}

	// Check fitting magnitude range invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_fmag_range_invariant () {
		if (fmag_range_avail) {
			if (!( fmag_above_mag_cat >= 1.0 )) {
				return "Invalid fitting magnitude range: fmag_above_mag_cat = " + fmag_above_mag_cat;
			}
			if (!( fmag_above_mag_max >= 0.0 )) {
				return "Invalid fitting magnitude range: fmag_above_mag_max = " + fmag_above_mag_max;
			}
		}
		return null;
	}

	// Append a string representation of the fitting magnitude range.

	public final StringBuilder fmag_range_append_string (StringBuilder sb) {
		sb.append ("fmag_range_avail = " + fmag_range_avail + "\n");
		if (fmag_range_avail) {
			sb.append ("fmag_above_mag_cat = " + fmag_above_mag_cat + "\n");
			sb.append ("fmag_above_mag_max = " + fmag_above_mag_max + "\n");
		}
		return sb;
	}

	// Marshal fitting magnitude range.

	private void marshal_fmag_range_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("fmag_range_avail", fmag_range_avail);
		if (fmag_range_avail) {
			writer.marshalDouble ("fmag_above_mag_cat", fmag_above_mag_cat);
			writer.marshalDouble ("fmag_above_mag_max", fmag_above_mag_max);
		}
		return;
	}

	// Unmarshal fitting magnitude range.

	private void unmarshal_fmag_range_v1 (MarshalReader reader) {
		fmag_range_avail = reader.unmarshalBoolean ("fmag_range_avail");
		if (fmag_range_avail) {
			fmag_above_mag_cat = reader.unmarshalDouble ("fmag_above_mag_cat");
			fmag_above_mag_max = reader.unmarshalDouble ("fmag_above_mag_max");
		} else {
			clear_fmag_range();
		}

		// Check the invariant

		String inv = check_fmag_range_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_fmag_range_v1: " + inv);
		}
		return;
	}

	// Get the magnitude range to use for fitting.
	// Parameters:
	//  cat_magCat = Magnitude of completeness, from the catalog information.
	//  cat_magTop = Top magnitude, user-supplied, from the catalog information (none if cat_magTop <= cat_magCat).
	//  rup_mag_top = Maximum magnitude among all known ruptures.
	//  fit_rup_mag_top = Maximum magnitude among ruptures in the time interval where fitting occurs.
	//  hist_magCat = Magnitude of completness, from the history used for fitting.
	// Returns the catalog range to use for fitting.
	// Use defaults for the reference magnitude and maximum considered magnitude.
	// Use the history's magCat for the minimum simulation magnitude, which allows combining intervals with mc == magCat.

	public final OECatalogParamsMags get_fmag_range (
		double cat_magCat,
		double cat_magTop,
		double rup_mag_top,
		double fit_rup_mag_top,
		double hist_magCat
	) {

		// Start with the minimum above the history magCat

		double mag_top = hist_magCat + fmag_above_mag_cat;

		// If top magnitude wasn't supplied in catalog info, apply the maximum over the entire catalog

		if (cat_magTop <= cat_magCat + OEConstants.FIT_MAG_EPS) {
			mag_top = Math.max (mag_top, rup_mag_top + fmag_above_mag_max);
		}

		// If it was supplied, apply it, and also apply the maximum in the portion of the catalog used for fitting

		else {
			mag_top = Math.max (mag_top, fit_rup_mag_top + fmag_above_mag_max);
			mag_top = Math.max (mag_top, cat_magTop);
		}

		// Make the magnitude range
		// Use defaults for the reference magnitude and maximum considered magnitude.
		// Use the history's magCat for the minimum simulation magnitude, which allows combining intervals with mc == magCat.

		OECatalogParamsMags fit_params_mags = new OECatalogParamsMags (
			OEConstants.DEF_MREF,		// mref
			OEConstants.DEF_MSUP,		// msup
			hist_magCat,				// mag_min_sim
			mag_top						// mag_max_sim
		);

		return fit_params_mags;
	}

	// Suggest a value for mag_top, given a magnitude of completness and maximum magnitude.
	// Parameters:
	//  the_mag_cat = Magnitude of completeness.
	//  the_mag_max = Maximum magnitude.
	// Returns a possible value for mag_top.

	public final double suggest_mag_top (
		double the_mag_cat,
		double the_mag_max
	) {

		// Start with a minimum above the magnitude of completeness

		double mag_top = the_mag_cat + (fmag_range_avail ? fmag_above_mag_cat : OEConstants.DEF_FMAG_ABOVE_MAG_CAT);

		// Then apply the maximum magnitude

		mag_top = Math.max (mag_top, the_mag_max + (fmag_range_avail ? fmag_above_mag_max : OEConstants.DEF_FMAG_ABOVE_MAG_MAX));

		return mag_top;
	}




	//----- Time interval for branch ratio calculation -----

	// Branch ratio time interval available flag.

	public boolean tint_br_avail = false;

	// Minimum time interval to allow for parameter fitting, for branch ratio calculation.

	public double tint_br_fitting = 0.0;

	// Additional time interval to allow for forecast simulation, for branch ratio calculation.

	public double tint_br_forecast = 0.0;

	// Clear branch ratio time interval.

	public final void clear_tint_br () {
		tint_br_avail = false;

		tint_br_fitting = 0.0;
		tint_br_forecast = 0.0;
		return;
	}

	// Set branch ratio time interval to typical values.

	public final void set_tint_br_to_typical () {
		tint_br_avail = true;

		tint_br_fitting = OEConstants.DEF_TINT_BR_FITTING;
		tint_br_forecast = OEConstants.DEF_TINT_BR_FORECAST;
		return;
	}

	// Copy branch ratio time interval from another object.

	public final void copy_tint_br_from (OEtasParameters other) {
		tint_br_avail = other.tint_br_avail;

		tint_br_fitting = other.tint_br_fitting;
		tint_br_forecast = other.tint_br_forecast;
		return;
	}

	// Set the branch ratio time interval to analyst values.

	public final void set_tint_br_to_analyst (
		boolean tint_br_avail,
		double tint_br_fitting,
		double tint_br_forecast
	) {
		this.tint_br_avail = tint_br_avail;
		this.tint_br_fitting = tint_br_fitting;
		this.tint_br_forecast = tint_br_forecast;
		return;
	}

	// Merge branch ratio time interval from another object, if available.

	public final void merge_tint_br_from (OEtasParameters other) {
		if (other != null) {
			if (other.tint_br_avail) {
				copy_tint_br_from (other);
			}
		}
		return;
	}

	// Check branch ratio time interval invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_tint_br_invariant () {
		if (tint_br_avail) {
			if (!( tint_br_fitting >= - OEConstants.TINY_DURATION_DAYS )) {
				return "Invalid branch ratio time interval: tint_br_fitting = " + tint_br_fitting;
			}
			if (!( tint_br_forecast >= - OEConstants.TINY_DURATION_DAYS )) {
				return "Invalid branch ratio time interval: tint_br_forecast = " + tint_br_forecast;
			}
		}
		return null;
	}

	// Append a string representation of the branch ratio time interval.

	public final StringBuilder tint_br_append_string (StringBuilder sb) {
		sb.append ("tint_br_avail = " + tint_br_avail + "\n");
		if (tint_br_avail) {
			sb.append ("tint_br_fitting = " + tint_br_fitting + "\n");
			sb.append ("tint_br_forecast = " + tint_br_forecast + "\n");
		}
		return sb;
	}

	// Marshal branch ratio time interval.

	private void marshal_tint_br_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("tint_br_avail", tint_br_avail);
		if (tint_br_avail) {
			writer.marshalDouble ("tint_br_fitting", tint_br_fitting);
			writer.marshalDouble ("tint_br_forecast", tint_br_forecast);
		}
		return;
	}

	// Unmarshal branch ratio time interval.

	private void unmarshal_tint_br_v1 (MarshalReader reader) {
		tint_br_avail = reader.unmarshalBoolean ("tint_br_avail");
		if (tint_br_avail) {
			tint_br_fitting = reader.unmarshalDouble ("tint_br_fitting");
			tint_br_forecast = reader.unmarshalDouble ("tint_br_forecast");
		} else {
			clear_tint_br();
		}

		// Check the invariant

		String inv = check_tint_br_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_tint_br_v1: " + inv);
		}
		return;
	}

	// Get the branch ratio time interval.
	// Parameter:
	//  t = Fitting time, typically the end time of observed data or the forecast time relative to the mainshock time, in days.
	// Note: Caller must check the branch ratio time interval is available.

	public final double get_tint_br (double t) {
		if (!( tint_br_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_tint_br: Branch ratio time interval not available");
		}
		double tint_br = Math.max (t, tint_br_fitting) + tint_br_forecast;
		return tint_br;
	}




	//----- ETAS Parameter ranges -----

	// ETAS parameter range available flag.

	public boolean range_avail = false;

	// Gutenberg-Richter parameter, b-value.

	public OEDiscreteRange b_range = null;

	// ETAS intensity parameter, alpha-value.
	// Can be null to force alpha == b.

	public OEDiscreteRange alpha_range = null;

	// The range of Omori c-values.

	public OEDiscreteRange c_range = null;

	// The range of Omori p-values.

	public OEDiscreteRange p_range = null;

	// The range of branch ratios, n-value.
	// This controls the productivity of secondary triggering.

	public OEDiscreteRange n_range = null;

	// The range of mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.

	public OEDiscreteRange zams_range = null;

	// The range of mainshock productivity, mu-value, for reference magnitude equal to ZMU_MREF.
	// Can be null to force zmu = 0.0.

	public OEDiscreteRange zmu_range = null;

	// True if the value of zams is interpreted relative to the a-value. [v2]

	public boolean relative_zams = false;

	// Clear ETAS parameter ranges.

	public final void clear_range () {
		range_avail = false;

		b_range     = null;
		alpha_range = null;
		c_range     = null;
		p_range     = null;
		n_range     = null;
		zams_range  = null;
		zmu_range   = null;

		relative_zams = false;
		return;
	}

	// Set ETAS parameter ranges to typical values.

	public final void set_range_to_typical () {
		range_avail = true;

		b_range     = OEConstants.def_b_range();
		alpha_range = OEConstants.def_alpha_range();
		c_range     = OEConstants.def_c_range();
		p_range     = OEConstants.def_p_range();
		n_range     = OEConstants.def_n_range();
		zams_range  = OEConstants.def_zams_range();
		zmu_range   = OEConstants.def_zmu_range();

		relative_zams = OEConstants.def_relative_zams();
		return;
	}

	// Copy ETAS parameter ranges from another object.
	// Note: OEDiscreteRange is an immutable object.

	public final void copy_range_from (OEtasParameters other) {
		range_avail = other.range_avail;

		b_range     = other.b_range;
		alpha_range = other.alpha_range;
		c_range     = other.c_range;
		p_range     = other.p_range;
		n_range     = other.n_range;
		zams_range  = other.zams_range;
		zmu_range   = other.zmu_range;

		relative_zams = other.relative_zams;
		return;
	}

	// Set the ETAS parameter ranges to analyst values.
	// Note: OEDiscreteRange is an immutable object.

	public final void set_range_to_analyst (
		boolean range_avail,
		OEDiscreteRange b_range,
		OEDiscreteRange alpha_range,
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange n_range,
		OEDiscreteRange zams_range,
		OEDiscreteRange zmu_range,
		boolean relative_zams
	) {
		this.range_avail = range_avail;
		this.b_range     = b_range;
		this.alpha_range = alpha_range;
		this.c_range     = c_range;
		this.p_range     = p_range;
		this.n_range     = n_range;
		this.zams_range  = zams_range;
		this.zmu_range   = zmu_range;
		this.relative_zams = relative_zams;
		return;
	}

	// Merge ETAS parameter ranges from another object, if available.

	public final void merge_range_from (OEtasParameters other) {
		if (other != null) {
			if (other.range_avail) {
				copy_range_from (other);
			}
		}
		return;
	}

	// Check ETAS parameter range invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_range_invariant () {
		if (range_avail) {
			if (!( b_range != null )) {
				return "Missing b_range";
			}
			if (!( b_range.get_range_size() == 1 )) {
				return "The b_range has multiple values: size = " + b_range.get_range_size();
			}
			if (!( alpha_range == null || alpha_range.get_range_size() == 1 )) {
				return "The alpha_range has multiple values: size = " + alpha_range.get_range_size();
			}
			if (!( c_range != null )) {
				return "Missing c_range";
			}
			if (!( p_range != null )) {
				return "Missing p_range";
			}
			if (!( n_range != null )) {
				return "Missing n_range";
			}
			if (!( zams_range != null )) {
				return "Missing zams_range";
			}
		}
		return null;
	}

	// Append a string representation of the ETAS parameter ranges.

	private static String range_to_string (OEDiscreteRange range) {
		if (range == null) {
			return "<null>";
		}
		return range.toString();
	}

	public final StringBuilder range_append_string (StringBuilder sb) {
		sb.append ("range_avail = " + range_avail + "\n");
		if (range_avail) {
			sb.append ("b_range = "     + range_to_string(b_range)     + "\n");
			sb.append ("alpha_range = " + range_to_string(alpha_range) + "\n");
			sb.append ("c_range = "     + range_to_string(c_range)     + "\n");
			sb.append ("p_range = "     + range_to_string(p_range)     + "\n");
			sb.append ("n_range = "     + range_to_string(n_range)     + "\n");
			sb.append ("zams_range = "  + range_to_string(zams_range)  + "\n");
			sb.append ("zmu_range = "   + range_to_string(zmu_range)   + "\n");

			sb.append ("relative_zams = " + relative_zams + "\n");
		}
		return sb;
	}

	// Marshal ETAS parameter ranges.

	private void marshal_range_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("range_avail", range_avail);
		if (range_avail) {
			final int xver = OEDiscreteRange.XVER_1;
			OEDiscreteRange.marshal_xver (writer, "b_range"    , xver, b_range    );
			OEDiscreteRange.marshal_xver (writer, "alpha_range", xver, alpha_range);
			OEDiscreteRange.marshal_xver (writer, "c_range"    , xver, c_range    );
			OEDiscreteRange.marshal_xver (writer, "p_range"    , xver, p_range    );
			OEDiscreteRange.marshal_xver (writer, "n_range"    , xver, n_range    );
			OEDiscreteRange.marshal_xver (writer, "zams_range" , xver, zams_range );
			OEDiscreteRange.marshal_xver (writer, "zmu_range"  , xver, zmu_range  );
		}
		return;
	}

	private void marshal_range_v2 (MarshalWriter writer) {
		writer.marshalBoolean ("range_avail", range_avail);
		if (range_avail) {
			final int xver = OEDiscreteRange.XVER_1;
			OEDiscreteRange.marshal_xver (writer, "b_range"    , xver, b_range    );
			OEDiscreteRange.marshal_xver (writer, "alpha_range", xver, alpha_range);
			OEDiscreteRange.marshal_xver (writer, "c_range"    , xver, c_range    );
			OEDiscreteRange.marshal_xver (writer, "p_range"    , xver, p_range    );
			OEDiscreteRange.marshal_xver (writer, "n_range"    , xver, n_range    );
			OEDiscreteRange.marshal_xver (writer, "zams_range" , xver, zams_range );
			OEDiscreteRange.marshal_xver (writer, "zmu_range"  , xver, zmu_range  );

			writer.marshalBoolean ("relative_zams", relative_zams);
		}
		return;
	}

	// Unmarshal ETAS parameter ranges.

	private void unmarshal_range_v1 (MarshalReader reader) {
		range_avail = reader.unmarshalBoolean ("range_avail");
		if (range_avail) {
			final int xver = OEDiscreteRange.XVER_1;
			b_range     = OEDiscreteRange.unmarshal_xver (reader, "b_range"    , xver);
			alpha_range = OEDiscreteRange.unmarshal_xver (reader, "alpha_range", xver);
			c_range     = OEDiscreteRange.unmarshal_xver (reader, "c_range"    , xver);
			p_range     = OEDiscreteRange.unmarshal_xver (reader, "p_range"    , xver);
			n_range     = OEDiscreteRange.unmarshal_xver (reader, "n_range"    , xver);
			zams_range  = OEDiscreteRange.unmarshal_xver (reader, "zams_range" , xver);
			zmu_range   = OEDiscreteRange.unmarshal_xver (reader, "zmu_range"  , xver);

			relative_zams = OEConstants.def_relative_zams();
		} else {
			clear_range();
		}

		// Check the invariant

		String inv = check_range_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_range_v1: " + inv);
		}
		return;
	}

	private void unmarshal_range_v2 (MarshalReader reader) {
		range_avail = reader.unmarshalBoolean ("range_avail");
		if (range_avail) {
			final int xver = OEDiscreteRange.XVER_1;
			b_range     = OEDiscreteRange.unmarshal_xver (reader, "b_range"    , xver);
			alpha_range = OEDiscreteRange.unmarshal_xver (reader, "alpha_range", xver);
			c_range     = OEDiscreteRange.unmarshal_xver (reader, "c_range"    , xver);
			p_range     = OEDiscreteRange.unmarshal_xver (reader, "p_range"    , xver);
			n_range     = OEDiscreteRange.unmarshal_xver (reader, "n_range"    , xver);
			zams_range  = OEDiscreteRange.unmarshal_xver (reader, "zams_range" , xver);
			zmu_range   = OEDiscreteRange.unmarshal_xver (reader, "zmu_range"  , xver);

			relative_zams = reader.unmarshalBoolean ("relative_zams");
		} else {
			clear_range();
		}

		// Check the invariant

		String inv = check_range_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_range_v1: " + inv);
		}
		return;
	}

	// Make a newly-allocated OEGridParams object containing the ranges.
	// Note: Caller must check the ranges are available.

	public final OEGridParams make_grid_params () {
		if (!( range_avail )) {
			throw new InvariantViolationException ("OEtasParameters.make_grid_params: ETAS parameter ranges not available");
		}
		OEGridParams grid_params = new OEGridParams (
			b_range,
			alpha_range,
			c_range,
			p_range,
			n_range,
			zams_range,
			zmu_range
		);
		return grid_params;
	}

	// Get the fitting parameter f_background, which indicates a non-zero background rate.
	// Note: Caller must check the ranges are available.

	public final boolean get_fit_f_background () {
		if (!( range_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_fit_f_background: ETAS parameter ranges not available");
		}
		if (zmu_range == null) {
			return false;
		}
		//  if (zmu_range.get_range_max() <= OEConstants.TINY_BACKGROUND_RATE) {
		//  	return false;
		//  }
		return true;
	}

	// Make a newly-allocated OEGridOptions object containing the grid options.
	// Note: Caller must check the ranges are available.

	public final OEGridOptions make_grid_options () {
		if (!( range_avail )) {
			throw new InvariantViolationException ("OEtasParameters.make_grid_options: ETAS parameter ranges not available");
		}
		OEGridOptions grid_options = new OEGridOptions (
			relative_zams
		);
		return grid_options;
	}




	//----- Bayesian prior -----

	// Bayesian prior available flag.

	public boolean bay_prior_avail = false;

	// Bayesian prior. [v1, removed in v2]

	//public OEBayPrior bay_prior = null;

	// Bayesian prior factory. [v2]

	public OEBayFactory bay_factory = null;

	// Clear Bayesian prior.

	public final void clear_bay_prior () {
		bay_prior_avail = false;

		//bay_prior = null;
		bay_factory = null;
		return;
	}

	// Set Bayesian prior to typical values.

	public final void set_bay_prior_to_typical () {
		bay_prior_avail = true;

		//bay_prior = OEBayPrior.makeUniform();

		//bay_factory = OEBayFactory.makeUniform();
		//bay_factory = OEBayFactory.makeGaussAPC();

		bay_factory = OEConstants.def_bay_factory();
		return;
	}

	// Set Bayesian prior to typical values for a uniform prior

	public final void set_bay_prior_to_typical_uniform () {
		bay_prior_avail = true;
		bay_factory = OEBayFactory.makeUniform();
		return;
	}

	// Set Bayesian prior to typical values for a Gaussian a/p/c prior.

	public final void set_bay_prior_to_typical_gauss_apc () {
		bay_prior_avail = true;
		bay_factory = OEBayFactory.makeGaussAPC();
		return;
	}

	// Copy Bayesian prior from another object.
	// Note: OEBayPrior is an immutable object.

	public final void copy_bay_prior_from (OEtasParameters other) {
		bay_prior_avail = other.bay_prior_avail;

		//bay_prior = other.bay_prior;
		bay_factory = other.bay_factory;
		return;
	}

	// Set the Bayesian prior to analyst values.
	// Note: OEBayPrior is an immutable object.

	//public final void set_bay_prior_to_analyst (
	//	boolean bay_prior_avail,
	//	OEBayPrior bay_prior
	//) {
	//	this.bay_prior_avail = bay_prior_avail;
	//	this.bay_prior = bay_prior;
	//	return;
	//}

	// Set the Bayesian prior to analyst values.
	// Note: OEBayFactory is an immutable object.

	public final void set_bay_prior_to_analyst (
		boolean bay_prior_avail,
		OEBayFactory bay_factory
	) {
		this.bay_prior_avail = bay_prior_avail;
		this.bay_factory = bay_factory;
		return;
	}

	// Merge Bayesian prior from another object, if available.

	public final void merge_bay_prior_from (OEtasParameters other) {
		if (other != null) {
			if (other.bay_prior_avail) {
				copy_bay_prior_from (other);
			}
		}
		return;
	}

	// Check Bayesian prior invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_bay_prior_invariant () {
		if (bay_prior_avail) {
			//if (!( bay_prior != null )) {
			//	return "Missing bay_prior";
			//}
			if (!( bay_factory != null )) {
				return "Missing bay_factory";
			}
		}
		return null;
	}

	// Append a string representation of the Bayesian prior.

	public final StringBuilder bay_prior_append_string (StringBuilder sb) {
		sb.append ("bay_prior_avail = " + bay_prior_avail + "\n");
		if (bay_prior_avail) {
			//sb.append ("bay_prior = {" + bay_prior.toString() + "}\n");
			sb.append ("bay_factory = {" + bay_factory.toString() + "}\n");
		}
		return sb;
	}

	// Marshal Bayesian prior.

	private void marshal_bay_prior_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("bay_prior_avail", bay_prior_avail);
		if (bay_prior_avail) {
			OEBayPrior bay_prior = bay_factory.make_bay_prior (new OEBayFactoryParams());
			OEBayPrior.marshal_poly (writer, "bay_prior", bay_prior);
		}
		return;
	}

	private void marshal_bay_prior_v2 (MarshalWriter writer) {
		writer.marshalBoolean ("bay_prior_avail", bay_prior_avail);
		if (bay_prior_avail) {
			OEBayFactory.marshal_poly (writer, "bay_factory", bay_factory);
		}
		return;
	}

	// Unmarshal Bayesian prior.

	private void unmarshal_bay_prior_v1 (MarshalReader reader) {
		bay_prior_avail = reader.unmarshalBoolean ("bay_prior_avail");
		if (bay_prior_avail) {
			OEBayPrior bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");
			bay_factory = OEBayFactory.makeFixed (bay_prior);
		} else {
			clear_bay_prior();
		}

		// Check the invariant

		String inv = check_bay_prior_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_bay_prior_v1: " + inv);
		}
		return;
	}

	private void unmarshal_bay_prior_v2 (MarshalReader reader) {
		bay_prior_avail = reader.unmarshalBoolean ("bay_prior_avail");
		if (bay_prior_avail) {
			bay_factory = OEBayFactory.unmarshal_poly (reader, "bay_factory");
		} else {
			clear_bay_prior();
		}

		// Check the invariant

		String inv = check_bay_prior_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_bay_prior_v2: " + inv);
		}
		return;
	}

	// Get the Bayesian prior.
	// Note: Caller must check the Bayesian prior is available.

	//public final OEBayPrior get_bay_prior () {
	//	if (!( bay_prior_avail )) {
	//		throw new InvariantViolationException ("OEtasParameters.get_bay_prior: Bayesian prior not available");
	//	}
	//	OEBayPrior bay_prior = bay_factory.make_bay_prior (new OEBayFactoryParams());
	//	return bay_prior;
	//}

	public final OEBayPrior get_bay_prior (OEBayFactoryParams factory_params) {
		if (!( bay_prior_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_bay_prior: Bayesian prior not available");
		}
		OEBayPrior bay_prior = bay_factory.make_bay_prior (factory_params);
		return bay_prior;
	}




	//----- Bayesian weight -----

	// Bayesian weight available flag.

	public boolean bay_weight_avail = false;

	// Bayesian weight (1 = Bayesian, 0 = Sequence-specific, 2 = Generic, see OEConstants.BAY_WT_XXX).

	public double bay_weight = 0.0;

	// Bayesian weight to use during early times (1 = Bayesian, 0 = Sequence-specific, 2 = Generic, see OEConstants.BAY_WT_XXX).

	public double early_bay_weight = 0.0;

	// Time during which the early weight applies, in days.

	public double early_bay_time = 0.0;

	// Clear Bayesian weight.

	public final void clear_bay_weight () {
		bay_weight_avail = false;

		bay_weight = 0.0;
		early_bay_weight = 0.0;
		early_bay_time = 0.0;
		return;
	}

	// Set Bayesian weight to typical values.

	public final void set_bay_weight_to_typical () {
		bay_weight_avail = true;

		bay_weight = OEConstants.BAY_WT_BAYESIAN;
		early_bay_weight = OEConstants.BAY_WT_GENERIC;
		early_bay_time = OEConstants.DEF_EARLY_BAY_TIME;
		return;
	}

	// Copy Bayesian weight from another object.

	public final void copy_bay_weight_from (OEtasParameters other) {
		bay_weight_avail = other.bay_weight_avail;

		bay_weight = other.bay_weight;
		early_bay_weight = other.early_bay_weight;
		early_bay_time = other.early_bay_time;
		return;
	}

	// Set the Bayesian weight to analyst values.

	public final void set_bay_weight_to_analyst (
		boolean bay_weight_avail,
		double bay_weight,
		double early_bay_weight,
		double early_bay_time
	) {
		this.bay_weight_avail = bay_weight_avail;
		this.bay_weight = bay_weight;
		this.early_bay_weight = early_bay_weight;
		this.early_bay_time = early_bay_time;
		return;
	}

	// Merge Bayesian weight from another object, if available.

	public final void merge_bay_weight_from (OEtasParameters other) {
		if (other != null) {
			if (other.bay_weight_avail) {
				copy_bay_weight_from (other);
			}
		}
		return;
	}

	// Check Bayesian weight invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_bay_weight_invariant () {
		if (bay_weight_avail) {
			if (!( bay_weight >= OEConstants.BAY_WT_MIN && bay_weight <= OEConstants.BAY_WT_MAX )) {
				return "Invalid Bayesian weight: bay_weight = " + bay_weight;
			}
			if (!( early_bay_weight >= OEConstants.BAY_WT_MIN && early_bay_weight <= OEConstants.BAY_WT_MAX )) {
				return "Invalid Bayesian weight: early_bay_weight = " + early_bay_weight;
			}
		}
		return null;
	}

	// Append a string representation of the Bayesian weight.

	public final StringBuilder bay_weight_append_string (StringBuilder sb) {
		sb.append ("bay_weight_avail = " + bay_weight_avail + "\n");
		if (bay_weight_avail) {
			sb.append ("bay_weight = " + bay_weight + "\n");
			sb.append ("early_bay_weight = " + early_bay_weight + "\n");
			sb.append ("early_bay_time = " + early_bay_time + "\n");
		}
		return sb;
	}

	// Marshal Bayesian weight.

	private void marshal_bay_weight_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("bay_weight_avail", bay_weight_avail);
		if (bay_weight_avail) {
			writer.marshalDouble ("bay_weight", bay_weight);
			writer.marshalDouble ("early_bay_weight", early_bay_weight);
			writer.marshalDouble ("early_bay_time", early_bay_time);
		}
		return;
	}

	// Unmarshal Bayesian weight.

	private void unmarshal_bay_weight_v1 (MarshalReader reader) {
		bay_weight_avail = reader.unmarshalBoolean ("bay_weight_avail");
		if (bay_weight_avail) {
			bay_weight = reader.unmarshalDouble ("bay_weight");
			early_bay_weight = reader.unmarshalDouble ("early_bay_weight");
			early_bay_time = reader.unmarshalDouble ("early_bay_time");
		} else {
			clear_bay_weight();
		}

		// Check the invariant

		String inv = check_bay_weight_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_bay_weight_v1: " + inv);
		}
		return;
	}

	// Get the Bayesian weight.
	// Parameter:
	//  t = Time, typically the end time of observed data relative to the mainshock time, in days.
	// Note: Caller must check the Bayesian weight is available.

	public final double get_bay_weight (double t) {
		if (!( bay_weight_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_bay_weight: Bayesian weight not available");
		}
		if (t <= early_bay_time) {
			return early_bay_weight;
		}
		return bay_weight;
	}




	//----- Grid post-processing -----

	// Grid post-processing available flag.

	public boolean grid_post_avail = false;

	// Size of each bin for binning sub-voxels according to log-density, in natural log units.

	double density_bin_size_lnu = 0.0;

	// Number of bins for binning sub-voxels according to log-density; must be >= 2.

	int density_bin_count = 0;

	// Fraction of the probability distribution to trim.

	double prob_tail_trim = 0.0;

	// Number of sub-voxels to use for seeding, must be a power of 2.

	int seed_subvox_count = 0;

	// Clear grid post-processing.

	public final void clear_grid_post () {
		grid_post_avail = false;

		density_bin_size_lnu = 0.0;
		density_bin_count    = 0;
		prob_tail_trim       = 0.0;
		seed_subvox_count    = 0;
		return;
	}

	// Set grid post-processing to typical values.

	public final void set_grid_post_to_typical () {
		grid_post_avail = true;

		density_bin_size_lnu = OEConstants.DEF_DENSITY_BIN_SIZE_LNU;
		density_bin_count    = OEConstants.DEF_DENSITY_BIN_COUNT;
		prob_tail_trim       = OEConstants.DEF_PROB_TAIL_TRIM;
		seed_subvox_count    = OEConstants.DEF_SEED_SUBVOX_COUNT;
		return;
	}

	// Copy grid post-processing from another object.

	public final void copy_grid_post_from (OEtasParameters other) {
		grid_post_avail = other.grid_post_avail;

		density_bin_size_lnu = other.density_bin_size_lnu;
		density_bin_count    = other.density_bin_count;
		prob_tail_trim       = other.prob_tail_trim;
		seed_subvox_count    = other.seed_subvox_count;
		return;
	}

	// Set the grid post-processing to analyst values.

	public final void set_grid_post_to_analyst (
		boolean grid_post_avail,
		double density_bin_size_lnu,
		int density_bin_count,
		double prob_tail_trim,
		int seed_subvox_count
	) {
		this.grid_post_avail = grid_post_avail;
		this.density_bin_size_lnu = density_bin_size_lnu;
		this.density_bin_count    = density_bin_count;
		this.prob_tail_trim       = prob_tail_trim;
		this.seed_subvox_count    = seed_subvox_count;
		return;
	}

	// Merge grid post-processing from another object, if available.

	public final void merge_grid_post_from (OEtasParameters other) {
		if (other != null) {
			if (other.grid_post_avail) {
				copy_grid_post_from (other);
			}
		}
		return;
	}

	// Check grid post-processing invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_grid_post_invariant () {
		if (grid_post_avail) {
			if (!( density_bin_size_lnu > 0.0 )) {
				return "Invalid grid post-processing: density_bin_size_lnu = " + density_bin_size_lnu;
			}
			if (!( density_bin_count >= 2 )) {
				return "Invalid grid post-processing: density_bin_count = " + density_bin_count;
			}
			if (!( prob_tail_trim >= 0.0 )) {
				return "Invalid grid post-processing: prob_tail_trim = " + prob_tail_trim;
			}
			if (!( seed_subvox_count >= 2 && (seed_subvox_count & (seed_subvox_count - 1)) == 0 )) {
				return "Invalid grid post-processing: seed_subvox_count = " + seed_subvox_count;
			}
		}
		return null;
	}

	// Append a string representation of the grid post-processing.

	public final StringBuilder grid_post_append_string (StringBuilder sb) {
		sb.append ("grid_post_avail = " + grid_post_avail + "\n");
		if (grid_post_avail) {
			sb.append ("density_bin_size_lnu = " + density_bin_size_lnu + "\n");
			sb.append ("density_bin_count = " + density_bin_count + "\n");
			sb.append ("prob_tail_trim = " + prob_tail_trim + "\n");
			sb.append ("seed_subvox_count = " + seed_subvox_count + "\n");
		}
		return sb;
	}

	// Marshal grid post-processing.

	private void marshal_grid_post_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("grid_post_avail", grid_post_avail);
		if (grid_post_avail) {
			writer.marshalDouble ("density_bin_size_lnu", density_bin_size_lnu);
			writer.marshalInt ("density_bin_count", density_bin_count);
			writer.marshalDouble ("prob_tail_trim", prob_tail_trim);
			writer.marshalInt ("seed_subvox_count", seed_subvox_count);
		}
		return;
	}

	// Unmarshal grid post-processing.

	private void unmarshal_grid_post_v1 (MarshalReader reader) {
		grid_post_avail = reader.unmarshalBoolean ("grid_post_avail");
		if (grid_post_avail) {
			density_bin_size_lnu = reader.unmarshalDouble ("density_bin_size_lnu");
			density_bin_count = reader.unmarshalInt ("density_bin_count");
			prob_tail_trim = reader.unmarshalDouble ("prob_tail_trim");
			seed_subvox_count = reader.unmarshalInt ("seed_subvox_count");
		} else {
			clear_grid_post();
		}

		// Check the invariant

		String inv = check_grid_post_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_grid_post_v1: " + inv);
		}
		return;
	}

	// Get the grid parameter density_bin_size_lnu.
	// Note: Caller must check the grid post-processing is available.

	public final double get_density_bin_size_lnu () {
		if (!( grid_post_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_density_bin_size_lnu: Grid post-processing not available");
		}
		return density_bin_size_lnu;
	}

	// Get the grid parameter density_bin_count.
	// Note: Caller must check the grid post-processing is available.

	public final int get_density_bin_count () {
		if (!( grid_post_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_density_bin_count: Grid post-processing not available");
		}
		return density_bin_count;
	}

	// Get the grid parameter prob_tail_trim.
	// Note: Caller must check the grid post-processing is available.

	public final double get_prob_tail_trim () {
		if (!( grid_post_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_prob_tail_trim: Grid post-processing not available");
		}
		return prob_tail_trim;
	}

	// Get the grid parameter seed_subvox_count.
	// Note: Caller must check the grid post-processing is available.

	public final int get_seed_subvox_count () {
		if (!( grid_post_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_seed_subvox_count: Grid post-processing not available");
		}
		return seed_subvox_count;
	}




	//----- Number of catalogs to simulate -----

	// Number of catalogs available flag.

	public boolean num_catalogs_avail = false;

	// Number of catalogs to simulate.

	public int num_catalogs = 0;

	// Minimum acceptable number of catalogs.

	public int min_num_catalogs = 0;

	// Clear number of catalogs.

	public final void clear_num_catalogs () {
		num_catalogs_avail = false;

		num_catalogs = 0;
		min_num_catalogs = 0;
		return;
	}

	// Set number of catalogs to typical values.

	public final void set_num_catalogs_to_typical () {
		num_catalogs_avail = true;

		num_catalogs = OEConstants.DEF_NUM_CATALOGS;
		min_num_catalogs = OEConstants.DEF_MIN_NUM_CATALOGS;
		return;
	}

	// Set number of catalogs to minimum allowed values.

	public final void set_num_catalogs_to_minimum () {
		num_catalogs_avail = true;

		num_catalogs = OEConstants.REQ_NUM_CATALOGS;
		min_num_catalogs = OEConstants.REQ_NUM_CATALOGS;
		return;
	}

	// Copy number of catalogs from another object.

	public final void copy_num_catalogs_from (OEtasParameters other) {
		num_catalogs_avail = other.num_catalogs_avail;

		num_catalogs = other.num_catalogs;
		min_num_catalogs = other.min_num_catalogs;
		return;
	}

	// Set the number of catalogs to analyst values.

	public final void set_num_catalogs_to_analyst (
		boolean num_catalogs_avail,
		int num_catalogs,
		int min_num_catalogs
	) {
		this.num_catalogs_avail = num_catalogs_avail;
		this.num_catalogs = num_catalogs;
		this.min_num_catalogs = min_num_catalogs;
		return;
	}

	// Merge number of catalogs from another object, if available.

	public final void merge_num_catalogs_from (OEtasParameters other) {
		if (other != null) {
			if (other.num_catalogs_avail) {
				copy_num_catalogs_from (other);
			}
		}
		return;
	}

	// Check number of catalogs invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_num_catalogs_invariant () {
		if (num_catalogs_avail) {
			if (!( min_num_catalogs >= OEConstants.REQ_NUM_CATALOGS && num_catalogs >= min_num_catalogs )) {
				return "Invalid number of catalogs: num_catalogs = " + num_catalogs + ", min_num_catalogs = " + min_num_catalogs;
			}
		}
		return null;
	}

	// Append a string representation of the number of catalogs.

	public final StringBuilder num_catalogs_append_string (StringBuilder sb) {
		sb.append ("num_catalogs_avail = " + num_catalogs_avail + "\n");
		if (num_catalogs_avail) {
			sb.append ("num_catalogs = " + num_catalogs + "\n");
			sb.append ("min_num_catalogs = " + min_num_catalogs + "\n");
		}
		return sb;
	}

	// Marshal number of catalogs.

	private void marshal_num_catalogs_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("num_catalogs_avail", num_catalogs_avail);
		if (num_catalogs_avail) {
			writer.marshalInt ("num_catalogs", num_catalogs);
			writer.marshalInt ("min_num_catalogs", min_num_catalogs);
		}
		return;
	}

	// Unmarshal number of catalogs.

	private void unmarshal_num_catalogs_v1 (MarshalReader reader) {
		num_catalogs_avail = reader.unmarshalBoolean ("num_catalogs_avail");
		if (num_catalogs_avail) {
			num_catalogs = reader.unmarshalInt ("num_catalogs");
			min_num_catalogs = reader.unmarshalInt ("min_num_catalogs");
		} else {
			clear_num_catalogs();
		}

		// Check the invariant

		String inv = check_num_catalogs_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_num_catalogs_v1: " + inv);
		}
		return;
	}

	// Get the number of catalogs.
	// Note: Caller must check the number of catalogs is available.

	public final int get_num_catalogs () {
		if (!( num_catalogs_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_num_catalogs: Number of catalogs not available");
		}
		return num_catalogs;
	}

	// Get the minimum acceptable number of catalogs.
	// Note: Caller must check the number of catalogs is available.

	public final int get_min_num_catalogs () {
		if (!( num_catalogs_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_num_catalogs: Number of catalogs not available");
		}
		return min_num_catalogs;
	}




	//----- Simulation parameters -----

	// Simulation parameters available flag.

	public boolean sim_params_avail = false;

	//-- Ranging

	// Target number of direct aftershocks of the seeds, for per-catalog min mag ranging, lower limit.

	public int ran_direct_size_lo = 0;

	// Target number of direct aftershocks of the seeds, for per-catalog min mag ranging, upper limit; or 0 if not used.

	public int ran_direct_size_hi = 0;

	// Magnitude excess to use during simulations, or 0.0 to disable.
	// A positive value causes catalogs to be discarded if they produce an earthquake larger than max mag.

	public double ran_mag_excess = 0.0;

	// Generation count for branch ratio handling, for per-catalog max mag ranging.  Must be >= 2.
	// Note: 2 indicates to use the direct aftershocks of the seeds.

	public int ran_gen_br = 0;

	// De-rating factor for branch ratio handling, for per-catalog max mag ranging.  Should be between 0 and 1 (and close to 1).

	public double ran_derate_br = 0.0;

	// Allowable fraction of catalogs to exceed max mag, for per-catalog max mag ranging.  Must be > 0.0 (and close to 0).

	public double ran_exceed_fraction = 0.0;

	//-- Simulation

	// The accumulator to use, for simulations.  (See OEConstants.SEL_ACCUM_XXXX.)

	public int sim_accum_selection = 0;

	// Accumulator options, for simulations.
	// for sim_accum_selection == SEL_ACCUM_RATE_TIME_MAG, it is the extrapolation options.

	public int sim_accum_option = 0;

	// Accumulator additional parameter 1, for simulations.
	// For sim_accum_selection == SEL_ACCUM_RATE_TIME_MAG, it is the proportional reduction (0.0 to 1.0) to apply to secondary productivity when computing upfill.

	public double sim_accum_param_1 = 0.0;


	//--- Operations

	// Clear simulation parameters.

	public final void clear_sim_params () {
		sim_params_avail = false;

		ran_direct_size_lo  = 0;
		ran_direct_size_hi  = 0;
		ran_mag_excess      = 0.0;
		ran_gen_br          = 0;
		ran_derate_br       = 0.0;
		ran_exceed_fraction = 0.0;

		sim_accum_selection = 0;
		sim_accum_option    = 0;
		sim_accum_param_1   = 0.0;
		return;
	}

	// Set simulation parameters to typical values.

	public final void set_sim_params_to_typical () {
		sim_params_avail = true;

		ran_direct_size_lo  = OEConstants.DEF_RAN_DIRECT_SIZE_LO;
		ran_direct_size_hi  = OEConstants.DEF_RAN_DIRECT_SIZE_HI;
		ran_mag_excess      = OEConstants.DEF_RAN_MAG_EXCESS;
		ran_gen_br          = OEConstants.DEF_RAN_GEN_BR;
		ran_derate_br       = OEConstants.DEF_RAN_DERATE_BR;
		ran_exceed_fraction = OEConstants.DEF_RAN_EXCEED_FRACTION;

		sim_accum_selection = OEConstants.def_sim_accum_selection();
		sim_accum_option    = OEConstants.def_sim_accum_option();
		sim_accum_param_1   = OEConstants.def_sim_accum_param_1();
		return;
	}

	// Copy simulation parameters from another object.

	public final void copy_sim_params_from (OEtasParameters other) {
		sim_params_avail = other.sim_params_avail;

		ran_direct_size_lo  = other.ran_direct_size_lo;
		ran_direct_size_hi  = other.ran_direct_size_hi;
		ran_mag_excess      = other.ran_mag_excess;
		ran_gen_br          = other.ran_gen_br;
		ran_derate_br       = other.ran_derate_br;
		ran_exceed_fraction = other.ran_exceed_fraction;

		sim_accum_selection = other.sim_accum_selection;
		sim_accum_option    = other.sim_accum_option;
		sim_accum_param_1   = other.sim_accum_param_1;
		return;
	}

	// Set the simulation parameters to analyst values.

	public final void set_sim_params_to_analyst (
		boolean sim_params_avail,
		int ran_direct_size_lo,
		int ran_direct_size_hi,
		double ran_mag_excess,
		int ran_gen_br,
		double ran_derate_br,
		double ran_exceed_fraction,
		int sim_accum_selection,
		int sim_accum_option,
		double sim_accum_param_1
	) {
		this.sim_params_avail = sim_params_avail;

		this.ran_direct_size_lo  = ran_direct_size_lo;
		this.ran_direct_size_hi  = ran_direct_size_hi;
		this.ran_mag_excess      = ran_mag_excess;
		this.ran_gen_br          = ran_gen_br;
		this.ran_derate_br       = ran_derate_br;
		this.ran_exceed_fraction = ran_exceed_fraction;

		this.sim_accum_selection = sim_accum_selection;
		this.sim_accum_option    = sim_accum_option;
		this.sim_accum_param_1   = sim_accum_param_1;
		return;
	}

	// Merge simulation parameters from another object, if available.

	public final void merge_sim_params_from (OEtasParameters other) {
		if (other != null) {
			if (other.sim_params_avail) {
				copy_sim_params_from (other);
			}
		}
		return;
	}

	// Check simulation parameters invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_sim_params_invariant () {
		if (sim_params_avail) {
			if (!( ran_direct_size_lo >= 1 && (ran_direct_size_hi == 0 || ran_direct_size_hi >= ran_direct_size_lo) )) {
				return "Invalid simulation parameters: ran_direct_size_lo = " + ran_direct_size_lo + ", ran_direct_size_hi = " + ran_direct_size_hi;
			}
			if (!( ran_mag_excess >= 0.0 )) {
				return "Invalid simulation parameters: ran_mag_excess = " + ran_mag_excess;
			}
			if (!( ran_gen_br >= 2 )) {
				return "Invalid simulation parameters: ran_gen_br = " + ran_gen_br;
			}
			if (!( ran_derate_br >= 0.0 && ran_derate_br <= 1.0 )) {
				return "Invalid simulation parameters: ran_derate_br = " + ran_derate_br;
			}
			if (!( ran_exceed_fraction > 0.0 )) {
				return "Invalid simulation parameters: ran_exceed_fraction = " + ran_exceed_fraction;
			}
			if (!( sim_accum_selection >= OEConstants.SEL_ACCUM_MIN && sim_accum_selection <= OEConstants.SEL_ACCUM_MAX )) {
				return "Invalid simulation parameters: sim_accum_selection = " + sim_accum_selection;
			}
			if (sim_accum_selection == OEConstants.SEL_ACCUM_RATE_TIME_MAG) {
				if (!( OEConstants.validate_rate_acc_meth (sim_accum_option) )) {
					return "Invalid simulation parameters: sim_accum_option = " + sim_accum_option;
				}
				if (!( sim_accum_param_1 >= 0.0 && sim_accum_param_1 <= 1.0 )) {
					return "Invalid simulation parameters: sim_accum_param_1 = " + sim_accum_param_1;
				}
			}
		}
		return null;
	}

	// Append a string representation of the simulation parameters.

	public final StringBuilder sim_params_append_string (StringBuilder sb) {
		sb.append ("sim_params_avail = " + sim_params_avail + "\n");
		if (sim_params_avail) {
			sb.append ("ran_direct_size_lo = " + ran_direct_size_lo + "\n");
			sb.append ("ran_direct_size_hi = " + ran_direct_size_hi + "\n");
			sb.append ("ran_mag_excess = " + ran_mag_excess + "\n");
			sb.append ("ran_gen_br = " + ran_gen_br + "\n");
			sb.append ("ran_derate_br = " + ran_derate_br + "\n");
			sb.append ("ran_exceed_fraction = " + ran_exceed_fraction + "\n");
			sb.append ("sim_accum_selection = " + sim_accum_selection + "\n");
			sb.append ("sim_accum_option = " + sim_accum_option + "\n");
			sb.append ("sim_accum_param_1 = " + sim_accum_param_1 + "\n");
		}
		return sb;
	}

	// Marshal simulation parameters.

	private void marshal_sim_params_v1 (MarshalWriter writer) {
		writer.marshalBoolean ("sim_params_avail", sim_params_avail);
		if (sim_params_avail) {
			writer.marshalInt ("ran_direct_size_lo", ran_direct_size_lo);
			writer.marshalInt ("ran_direct_size_hi", ran_direct_size_hi);
			writer.marshalDouble ("ran_mag_excess", ran_mag_excess);
			writer.marshalInt ("ran_gen_br", ran_gen_br);
			writer.marshalDouble ("ran_derate_br", ran_derate_br);
			writer.marshalDouble ("ran_exceed_fraction", ran_exceed_fraction);
			writer.marshalInt ("sim_accum_selection", sim_accum_selection);
			writer.marshalInt ("sim_accum_option", sim_accum_option);
			writer.marshalDouble ("sim_accum_param_1", sim_accum_param_1);
		}
		return;
	}

	// Unmarshal simulation parameters.

	private void unmarshal_sim_params_v1 (MarshalReader reader) {
		sim_params_avail = reader.unmarshalBoolean ("sim_params_avail");
		if (sim_params_avail) {
			ran_direct_size_lo = reader.unmarshalInt ("ran_direct_size_lo");
			ran_direct_size_hi = reader.unmarshalInt ("ran_direct_size_hi");
			ran_mag_excess = reader.unmarshalDouble ("ran_mag_excess");
			ran_gen_br = reader.unmarshalInt ("ran_gen_br");
			ran_derate_br = reader.unmarshalDouble ("ran_derate_br");
			ran_exceed_fraction = reader.unmarshalDouble ("ran_exceed_fraction");
			sim_accum_selection = reader.unmarshalInt ("sim_accum_selection");
			sim_accum_option = reader.unmarshalInt ("sim_accum_option");
			sim_accum_param_1 = reader.unmarshalDouble ("sim_accum_param_1");
		} else {
			clear_sim_params();
		}

		// Check the invariant

		String inv = check_sim_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_sim_params_v1: " + inv);
		}
		return;
	}

	// Get the simulation parameters.
	// Note: Caller must check the simulation parameters and number of catalogs are available.

	public final OESimulationParams get_sim_params (boolean f_small_eqk) {
		if (!( sim_params_avail )) {
			throw new InvariantViolationException ("OEtasParameters.get_sim_params: Simulation parameters not available");
		}

		boolean f_prod = true;
		OESimulationParams sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

		// Insert number of catalogs

		sim_parameters.sim_num_catalogs = div_x_by_2_if_small_eqk (f_small_eqk, get_num_catalogs(), OEConstants.REQ_NUM_CATALOGS);
		sim_parameters.sim_min_num_catalogs = div_x_by_2_if_small_eqk (f_small_eqk, get_min_num_catalogs(), OEConstants.REQ_NUM_CATALOGS);

		sim_parameters.range_num_catalogs = Math.max (OEConstants.REQ_NUM_CATALOGS, sim_parameters.sim_num_catalogs/10);
		sim_parameters.range_min_num_catalogs = Math.max (OEConstants.REQ_NUM_CATALOGS, sim_parameters.sim_min_num_catalogs/10);

		// Insert ranging parameters, for per-catalog ranging

		sim_parameters.ranv2_direct_size = ran_direct_size_lo;
		sim_parameters.ranv2_mag_excess = ran_mag_excess;
		sim_parameters.ranv2_gen_br = ran_gen_br;
		sim_parameters.ranv2_derate_br = ran_derate_br;
		sim_parameters.ranv2_exceed_fraction = ran_exceed_fraction;
		sim_parameters.ranv3_direct_size_hi = ran_direct_size_hi;

		sim_parameters.range_method = OEConstants.RANGING_METH_VAR_SEED_EST;

		// Insert simulation parameters

		sim_parameters.sim_accum_selection = sim_accum_selection;

		sim_parameters.sim_accum_option = sim_accum_option;
		sim_parameters.sim_accum_param_1 = sim_accum_param_1;

		return sim_parameters;
	}




	//----- Parameters for ETAS eligibility -----

	// Eligibility parameters available flag. [v2]

	public boolean eligible_params_avail = false;

	// Eligibility option (see OEConstants.ELIGIBLE_OPT_XXXXX). [v2]

	public int eligible_option = OEConstants.ELIGIBLE_OPT_AUTO;

	// Mainshock magnitude for ETAS eligibility. [v2]

	public double eligible_main_mag = OEConstants.DEF_ELIGIBLE_MAIN_MAG;

	// Catalog maximum magnitude for ETAS eligibility. [v2]

	public double eligible_cat_max_mag = OEConstants.DEF_ELIGIBLE_CAT_MAX_MAG;

	// Mainshock magnitude below which earthquake is considered small. [v2]
	// Can use OEConstants.NO_MAG_NEG (in practice zero would work) if none.

	public double eligible_small_mag = OEConstants.DEF_ELIGIBLE_SMALL_MAG;

	// Clear eligibility parameters.

	public final void clear_eligible_params () {
		eligible_params_avail = false;

		eligible_option = OEConstants.ELIGIBLE_OPT_AUTO;
		eligible_main_mag = OEConstants.DEF_ELIGIBLE_MAIN_MAG;
		eligible_cat_max_mag = OEConstants.DEF_ELIGIBLE_CAT_MAX_MAG;
		eligible_small_mag = OEConstants.DEF_ELIGIBLE_SMALL_MAG;
		return;
	}

	// Set eligibility parameters to typical values.

	public final void set_eligible_params_to_typical () {
		eligible_params_avail = true;

		eligible_option = OEConstants.ELIGIBLE_OPT_AUTO;
		eligible_main_mag = OEConstants.DEF_ELIGIBLE_MAIN_MAG;
		eligible_cat_max_mag = OEConstants.DEF_ELIGIBLE_CAT_MAX_MAG;
		eligible_small_mag = OEConstants.DEF_ELIGIBLE_SMALL_MAG;
		return;
	}

	// Copy eligibility parameters from another object.

	public final void copy_eligible_params_from (OEtasParameters other) {
		eligible_params_avail = other.eligible_params_avail;

		eligible_option = other.eligible_option;
		eligible_main_mag = other.eligible_main_mag;
		eligible_cat_max_mag = other.eligible_cat_max_mag;
		eligible_small_mag = other.eligible_small_mag;
		return;
	}

	// Set the eligibility parameters to analyst values.

	public final void set_eligible_params_to_analyst (
		boolean eligible_params_avail,
		int eligible_option,
		double eligible_main_mag,
		double eligible_cat_max_mag,
		double eligible_small_mag
	) {
		this.eligible_params_avail = eligible_params_avail;
		this.eligible_option = eligible_option;
		this.eligible_main_mag = eligible_main_mag;
		this.eligible_cat_max_mag = eligible_cat_max_mag;
		this.eligible_small_mag = eligible_small_mag;
		return;
	}

	// Set the eligibility parameters to analyst values, using default magnitudes.

	public final void set_eligible_params_to_analyst (
		boolean eligible_params_avail,
		int eligible_option
	) {
		this.eligible_params_avail = eligible_params_avail;
		this.eligible_option = eligible_option;
		this.eligible_main_mag = OEConstants.DEF_ELIGIBLE_MAIN_MAG;
		this.eligible_cat_max_mag = OEConstants.DEF_ELIGIBLE_CAT_MAX_MAG;
		this.eligible_small_mag = OEConstants.DEF_ELIGIBLE_SMALL_MAG;
		return;
	}

	// Merge eligibility parameters from another object, if available.

	public final void merge_eligible_params_from (OEtasParameters other) {
		if (other != null) {
			if (other.eligible_params_avail) {
				copy_eligible_params_from (other);
			}
		}
		return;
	}

	// Check eligibility parameters invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_eligible_params_invariant () {
		if (eligible_params_avail) {
			if (!( eligible_option >= OEConstants.ELIGIBLE_OPT_MIN && eligible_option <= OEConstants.ELIGIBLE_OPT_MAX )) {
				return "Invalid ETAS eligibility option: eligible_option = " + eligible_option;
			}
		}
		return null;
	}

	// Append a string representation of the eligibility parameters.

	public final StringBuilder eligible_params_append_string (StringBuilder sb) {
		sb.append ("eligible_params_avail = " + eligible_params_avail + "\n");
		if (eligible_params_avail) {
			sb.append ("eligible_option = " + eligible_option + "\n");
			sb.append ("eligible_main_mag = " + eligible_main_mag + "\n");
			sb.append ("eligible_cat_max_mag = " + eligible_cat_max_mag + "\n");
			sb.append ("eligible_small_mag = " + eligible_small_mag + "\n");
		}
		return sb;
	}

	// Marshal eligibility parameters.

	private void marshal_eligible_params_v1 (MarshalWriter writer) {

		// Not present in v1

		return;
	}

	private void marshal_eligible_params_v2 (MarshalWriter writer) {
		writer.marshalBoolean ("eligible_params_avail", eligible_params_avail);
		if (eligible_params_avail) {
			writer.marshalInt ("eligible_option", eligible_option);
			writer.marshalDouble ("eligible_main_mag", eligible_main_mag);
			writer.marshalDouble ("eligible_cat_max_mag", eligible_cat_max_mag);
			writer.marshalDouble ("eligible_small_mag", eligible_small_mag);
		}
		return;
	}

	// Unmarshal eligibility parameters.

	private void unmarshal_eligible_params_v1 (MarshalReader reader) {

		//// Not present in v1, set to unconditionally eligible
		//
		//eligible_params_avail = true;
		//eligible_option = OEConstants.ELIGIBLE_OPT_ENABLE;
		//eligible_main_mag = OEConstants.DEF_ELIGIBLE_MAIN_MAG;
		//eligible_cat_max_mag = OEConstants.DEF_ELIGIBLE_CAT_MAX_MAG;
		//eligible_small_mag = OEConstants.NO_MAG_NEG;

		// Not present in v1, clear it to not-available
			
		clear_eligible_params();

		// Check the invariant

		String inv = check_eligible_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_eligible_params_v1: " + inv);
		}
		return;
	}

	private void unmarshal_eligible_params_v2 (MarshalReader reader) {
		eligible_params_avail = reader.unmarshalBoolean ("eligible_params_avail");
		if (eligible_params_avail) {
			eligible_option = reader.unmarshalInt ("eligible_option");
			eligible_main_mag = reader.unmarshalDouble ("eligible_main_mag");
			eligible_cat_max_mag = reader.unmarshalDouble ("eligible_cat_max_mag");
			eligible_small_mag = reader.unmarshalDouble ("eligible_small_mag");
		} else {
			clear_eligible_params();
		}

		// Check the invariant

		String inv = check_eligible_params_invariant();
		if (inv != null) {
			throw new MarshalException ("OEtasParameters.unmarshal_eligible_params_v2: " + inv);
		}
		return;
	}

	// Check if eligible for an ETAS forecast.
	// Parameters:
	//  mainshock_mag = Mainshock magnitude, or another magnitude representative of the sequence.
	//  catalog_max_mag = Maximum magnitude in the catalog, exclusive of the mainshock.

	public final boolean check_eligible (
		double mainshock_mag,
		double catalog_max_mag
	) {
		// If not available, assume eligible

		if (!( eligible_params_avail )) {
			return true;
		}

		// Switch on option

		switch (eligible_option) {

		// Unconditional disable

		case OEConstants.ELIGIBLE_OPT_DISABLE:
			return false;

		// Unconditional enable

		case OEConstants.ELIGIBLE_OPT_ENABLE:
			return true;

		// Automatic selection based on magnitude

		case OEConstants.ELIGIBLE_OPT_AUTO:
			if (mainshock_mag >= eligible_main_mag || catalog_max_mag >= eligible_cat_max_mag) {
				return true;
			}
			return false;
		}

		// Unrecognized option (should never happen), assume eligible

		return true;
	}

	// Check if this is considered a small earthquake.
	// Parameters:
	//  catalog_info = Catalog information.
	// Return true if this is a small earthquake.

	public boolean is_small_mag (OEtasCatalogInfo catalog_info) {

		// If not available, assume not small

		if (!( eligible_params_avail )) {
			return false;
		}

		// If catalog info contains a magnitude ...

		if (catalog_info.mag_main_avail) {

			// It's small if less than the threshold

			if (catalog_info.mag_main < eligible_small_mag) {
				return true;
			}
		}

		// Not known to be small

		return false;
	}

	// If small earthquake, divide x by 2, but do not reduce below minx.
	// If not small, or if x <= minx, return x.

	private static int div_x_by_2_if_small_eqk (boolean f_small_eqk, int x, int minx) {
		if (f_small_eqk) {
			if (x > minx) {
				return Math.max (minx, x/2);
			}
		}
		return x;
	}




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		clear_hist_params();
		clear_group_params();
		clear_fit_params();
		clear_fmag_range();
		clear_tint_br();
		clear_range();
		clear_bay_prior();
		clear_bay_weight();
		clear_grid_post();
		clear_num_catalogs();
		clear_sim_params();
		clear_eligible_params();
		return;
	}




	// Default constructor.

	public OEtasParameters () {
		clear();
	}




	// Set to typical values.

	public final OEtasParameters set_to_typical () {
		set_hist_params_to_typical();
		set_group_params_to_typical();
		set_fit_params_to_typical();
		set_fmag_range_to_typical();
		set_tint_br_to_typical();
		set_range_to_typical();
		set_bay_prior_to_typical();
		set_bay_weight_to_typical();
		set_grid_post_to_typical();
		set_num_catalogs_to_typical();
		set_sim_params_to_typical();
		set_eligible_params_to_typical();
		return this;
	}




	// Copy from another object.

	public final OEtasParameters copy_from (OEtasParameters other) {
		copy_hist_params_from (other);
		copy_group_params_from (other);
		copy_fit_params_from (other);
		copy_fmag_range_from (other);
		copy_tint_br_from (other);
		copy_range_from (other);
		copy_bay_prior_from (other);
		copy_bay_weight_from (other);
		copy_grid_post_from (other);
		copy_num_catalogs_from (other);
		copy_sim_params_from (other);
		copy_eligible_params_from (other);
		return this;
	}




	// Merge from another object.
	// Parameter present in the other object replace parameters in this object.

	public final OEtasParameters merge_from (OEtasParameters other) {
		merge_hist_params_from (other);
		merge_group_params_from (other);
		merge_fit_params_from (other);
		merge_fmag_range_from (other);
		merge_tint_br_from (other);
		merge_range_from (other);
		merge_bay_prior_from (other);
		merge_bay_weight_from (other);
		merge_grid_post_from (other);
		merge_num_catalogs_from (other);
		merge_sim_params_from (other);
		merge_eligible_params_from (other);
		return this;
	}




	// Check invariant.
	// Returns null if success, error message if invariant violated.

	public final String check_invariant () {
		String result = null;
		if (result == null) {result = check_hist_params_invariant();}
		if (result == null) {result = check_group_params_invariant();}
		if (result == null) {result = check_fit_params_invariant();}
		if (result == null) {result = check_fmag_range_invariant();}
		if (result == null) {result = check_tint_br_invariant();}
		if (result == null) {result = check_range_invariant();}
		if (result == null) {result = check_bay_prior_invariant();}
		if (result == null) {result = check_bay_weight_invariant();}
		if (result == null) {result = check_grid_post_invariant();}
		if (result == null) {result = check_num_catalogs_invariant();}
		if (result == null) {result = check_sim_params_invariant();}
		if (result == null) {result = check_eligible_params_invariant();}
		return result;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasParameters:" + "\n");

		hist_params_append_string (result);
		group_params_append_string (result);
		fit_params_append_string (result);
		fmag_range_append_string (result);
		tint_br_append_string (result);
		range_append_string (result);
		bay_prior_append_string (result);
		bay_weight_append_string (result);
		grid_post_append_string (result);
		num_catalogs_append_string (result);
		sim_params_append_string (result);
		eligible_params_append_string (result);

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 121001;
	private static final int MARSHAL_VER_2 = 121002;

	private static final String M_VERSION_NAME = "OEtasParameters";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			marshal_hist_params_v1 (writer);
			marshal_group_params_v1 (writer);
			marshal_fit_params_v1 (writer);
			marshal_fmag_range_v1 (writer);
			marshal_tint_br_v1 (writer);
			marshal_range_v1 (writer);
			marshal_bay_prior_v1 (writer);
			marshal_bay_weight_v1 (writer);
			marshal_grid_post_v1 (writer);
			marshal_num_catalogs_v1 (writer);
			marshal_sim_params_v1 (writer);
			marshal_eligible_params_v1 (writer);

		}
		break;

		case MARSHAL_VER_2: {

			marshal_hist_params_v1 (writer);
			marshal_group_params_v1 (writer);
			marshal_fit_params_v1 (writer);
			marshal_fmag_range_v1 (writer);
			marshal_tint_br_v1 (writer);
			marshal_range_v2 (writer);
			marshal_bay_prior_v2 (writer);
			marshal_bay_weight_v1 (writer);
			marshal_grid_post_v1 (writer);
			marshal_num_catalogs_v1 (writer);
			marshal_sim_params_v1 (writer);
			marshal_eligible_params_v2 (writer);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			clear();	// for fields that are not marshaled

			unmarshal_hist_params_v1 (reader);
			unmarshal_group_params_v1 (reader);
			unmarshal_fit_params_v1 (reader);
			unmarshal_fmag_range_v1 (reader);
			unmarshal_tint_br_v1 (reader);
			unmarshal_range_v1 (reader);
			unmarshal_bay_prior_v1 (reader);
			unmarshal_bay_weight_v1 (reader);
			unmarshal_grid_post_v1 (reader);
			unmarshal_num_catalogs_v1 (reader);
			unmarshal_sim_params_v1 (reader);
			unmarshal_eligible_params_v1 (reader);

		}
		break;

		case MARSHAL_VER_2: {

			clear();	// for fields that are not marshaled

			unmarshal_hist_params_v1 (reader);
			unmarshal_group_params_v1 (reader);
			unmarshal_fit_params_v1 (reader);
			unmarshal_fmag_range_v1 (reader);
			unmarshal_tint_br_v1 (reader);
			unmarshal_range_v2 (reader);
			unmarshal_bay_prior_v2 (reader);
			unmarshal_bay_weight_v1 (reader);
			unmarshal_grid_post_v1 (reader);
			unmarshal_num_catalogs_v1 (reader);
			unmarshal_sim_params_v1 (reader);
			unmarshal_eligible_params_v2 (reader);

		}
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
	public OEtasParameters unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEtasParameters etas_params) {
		etas_params.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEtasParameters static_unmarshal (MarshalReader reader, String name) {
		return (new OEtasParameters()).unmarshal (reader, name);
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasParameters");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct typical parameters, and display it.
		// Check the invariant.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// Also test copy, merge, readout.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling ETAS parameters");
			testargs.end_test();

			// Create the parameters

			OEtasParameters etas_params = new OEtasParameters();
			etas_params.set_to_typical ();

			// Display the contents

			System.out.println ();
			System.out.println ("********** ETAS Parameters Display **********");
			System.out.println ();

			System.out.println (etas_params.toString());

			// Test invariant

			String inv = etas_params.check_invariant();

			System.out.println ();
			System.out.println ("Invariant = " + ((inv == null) ? "null" : inv));

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (etas_params);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (etas_params);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEtasParameters etas_params2 = new OEtasParameters();
			MarshalUtils.from_json_string (etas_params2, json_string);

			// Display the contents

			System.out.println (etas_params2.toString());

			// Display an empty object

			System.out.println ();
			System.out.println ("********** Empty ETAS Parameters Display **********");
			System.out.println ();

			OEtasParameters etas_params_empty = new OEtasParameters();
			etas_params_empty.clear();

			System.out.println (etas_params_empty.toString());

			// Test invariant

			String inv_empty = etas_params_empty.check_invariant();

			System.out.println ();
			System.out.println ("Invariant = " + ((inv_empty == null) ? "null" : inv_empty));

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Empty Marshal to JSON **********");
			System.out.println ();

			//String json_string_empty = MarshalUtils.to_json_string (etas_params_empty);
			//System.out.println (MarshalUtils.display_json_string (json_string_empty));

			String json_string_empty = MarshalUtils.to_formatted_compact_json_string (etas_params_empty);
			System.out.println (json_string_empty);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Empty Unmarshal from JSON **********");
			System.out.println ();
			
			OEtasParameters etas_params_empty2 = new OEtasParameters();
			MarshalUtils.from_json_string (etas_params_empty2, json_string_empty);

			// Display the contents

			System.out.println (etas_params_empty2.toString());

			// Set up an object by copying

			System.out.println ();
			System.out.println ("********** Copy into empty object **********");
			System.out.println ();

			OEtasParameters etas_params3 = new OEtasParameters();
			etas_params3.clear();
			etas_params3.copy_from (etas_params);

			System.out.println (etas_params3.toString());

			// Merge from empty object, nothing should change

			System.out.println ();
			System.out.println ("********** Merge from empty object **********");
			System.out.println ();

			OEtasParameters etas_params4 = new OEtasParameters();
			etas_params4.clear();
			etas_params3.merge_from (etas_params4);

			System.out.println (etas_params3.toString());

			// Merge into empty object, should be like a copy

			System.out.println ();
			System.out.println ("********** Merge into empty object **********");
			System.out.println ();

			etas_params4.clear();
			etas_params4.merge_from (etas_params);

			System.out.println (etas_params4.toString());

			// Test readout functions

			System.out.println ();
			System.out.println ("********** Readout functions **********");
			System.out.println ();

			OEDiscFGHParams fgh_params = etas_params.make_hist_params (
				3.0,		// magCat,
				1.00,		// capF,
				4.50,		// capG,
				0.75,		// capH,
				-30.0,		// t_range_begin,
				180.0,		// t_range_end,
				0.0			// t_interval_begin
			);

			System.out.println ("make_hist_params =\n" + fgh_params.toString());

			System.out.println ();
			System.out.println ("get_span_width_fcn =\n" + etas_params.get_span_width_fcn().toString());

			System.out.println ();
			System.out.println ("get_rup_width_fcn =\n" + etas_params.get_rup_width_fcn().toString());

			System.out.println ();
			System.out.println ("get_fit_f_interval =\n" + etas_params.get_fit_f_interval());

			System.out.println ();
			System.out.println ("get_fit_lmr_opt =\n" + etas_params.get_fit_lmr_opt());

			System.out.println ();
			System.out.println ("get_fmag_range(2.0, 2.0, 7.0, 6.0, 3.0) =\n" + etas_params.get_fmag_range(2.0, 2.0, 7.0, 6.0, 3.0).toString());

			System.out.println ();
			System.out.println ("get_fmag_range(2.0, 5.0, 7.0, 6.0, 3.0) =\n" + etas_params.get_fmag_range(2.0, 5.0, 7.0, 6.0, 3.0).toString());

			System.out.println ();
			System.out.println ("get_fmag_range(2.0, 8.0, 7.0, 6.0, 3.0) =\n" + etas_params.get_fmag_range(2.0, 8.0, 7.0, 6.0, 3.0).toString());

			System.out.println ();
			System.out.println ("get_tint_br(0.001) =\n" + etas_params.get_tint_br(0.001));

			System.out.println ();
			System.out.println ("get_tint_br(3.0) =\n" + etas_params.get_tint_br(3.0));

			System.out.println ();
			System.out.println ("get_tint_br(365.0) =\n" + etas_params.get_tint_br(365.0));

			System.out.println ();
			System.out.println ("get_tint_br(3650.0) =\n" + etas_params.get_tint_br(3650.0));

			System.out.println ();
			System.out.println ("make_grid_params =\n" + etas_params.make_grid_params().toString());

			System.out.println ();
			System.out.println ("get_fit_f_background =\n" + etas_params.get_fit_f_background());

			System.out.println ();
			System.out.println ("make_grid_options =\n" + etas_params.make_grid_options().toString());

			System.out.println ();
			System.out.println ("get_bay_prior =\n" + etas_params.get_bay_prior(new OEBayFactoryParams()).toString());

			System.out.println ();
			System.out.println ("get_bay_weight(0.001) =\n" + etas_params.get_bay_weight(0.001));

			System.out.println ();
			System.out.println ("get_bay_weight(3.0) =\n" + etas_params.get_bay_weight(3.0));

			System.out.println ();
			System.out.println ("get_density_bin_size_lnu =\n" + etas_params.get_density_bin_size_lnu());

			System.out.println ();
			System.out.println ("get_density_bin_count =\n" + etas_params.get_density_bin_count());

			System.out.println ();
			System.out.println ("get_prob_tail_trim =\n" + etas_params.get_prob_tail_trim());

			System.out.println ();
			System.out.println ("get_seed_subvox_count =\n" + etas_params.get_seed_subvox_count());

			System.out.println ();
			System.out.println ("get_num_catalogs =\n" + etas_params.get_num_catalogs());

			System.out.println ();
			System.out.println ("get_min_num_catalogs =\n" + etas_params.get_min_num_catalogs());

			System.out.println ();
			System.out.println ("get_sim_params(false) =\n" + etas_params.get_sim_params(false).toString());

			System.out.println ();
			System.out.println ("get_sim_params(true) =\n" + etas_params.get_sim_params(true).toString());

			System.out.println ();
			System.out.println ("check_eligible(5.0, 3.0) =\n" + etas_params.check_eligible(5.0, 3.0));

			System.out.println ();
			System.out.println ("check_eligible(4.5, 3.0) =\n" + etas_params.check_eligible(4.5, 3.0));

			System.out.println ();
			System.out.println ("check_eligible(4.5, 4.0) =\n" + etas_params.check_eligible(4.5, 4.0));

			System.out.println ();
			System.out.println ("check_eligible(4.0, 3.0) =\n" + etas_params.check_eligible(4.0, 3.0));

			System.out.println ();
			System.out.println ("check_eligible(4.0, 4.0) =\n" + etas_params.check_eligible(4.0, 4.0));

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename
		// Construct typical parameters, and write them to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Writing typical ETAS parameters to a file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the parameters

			OEtasParameters etas_params = new OEtasParameters();
			etas_params.set_to_typical ();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_json_string (etas_params);

			// Write the file

			SimpleUtils.write_string_as_file (filename, formatted_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename
		// Construct typical parameters, and write them to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Writing typical ETAS parameters to a file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the parameters

			OEtasParameters etas_params = new OEtasParameters();
			etas_params.set_to_typical ();

			// Write to file

			MarshalUtils.to_json_file (etas_params, filename);

			// Read back the file and display it

			OEtasParameters etas_params2 = new OEtasParameters();
			MarshalUtils.from_json_file (etas_params2, filename);

			System.out.println ();
			System.out.println (etas_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename
		// Construct typical parameters, and write them to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Writing typical ETAS parameters to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the parameters

			OEtasParameters etas_params = new OEtasParameters();
			etas_params.set_to_typical ();

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_params, filename);

			// Read back the file and display it

			OEtasParameters etas_params2 = new OEtasParameters();
			MarshalUtils.from_json_file (etas_params2, filename);

			System.out.println ();
			System.out.println (etas_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  filename
		// Construct empty parameters, and write them to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Writing empty ETAS parameters to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the parameters

			OEtasParameters etas_params = new OEtasParameters();
			etas_params.clear ();

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_params, filename);

			// Read back the file and display it

			OEtasParameters etas_params2 = new OEtasParameters();
			MarshalUtils.from_json_file (etas_params2, filename);

			System.out.println ();
			System.out.println (etas_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

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
