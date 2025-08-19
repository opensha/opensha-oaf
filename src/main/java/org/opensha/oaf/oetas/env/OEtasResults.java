package org.opensha.oaf.oetas.env;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.fit.OEDisc2History;
import org.opensha.oaf.oetas.fit.OEDisc2InitFitInfo;
import org.opensha.oaf.oetas.fit.OEDisc2InitVoxSet;
import org.opensha.oaf.oetas.fit.OEGridPoint;

import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;

import org.opensha.oaf.oetas.OECatalogRange;
import org.opensha.oaf.oetas.OESimulator;
import org.opensha.oaf.oetas.OEConstants;

import org.opensha.oaf.oetas.util.OEMarginalDistSet;


// Class to hold results for operational ETAS.
// Author: Michael Barall 05/04/2022.
//
// This class contains results that are saved in the database and the download file.

public class OEtasResults extends OEtasOutcome implements Marshalable {


	//----- Inputs -----

	public OEtasCatalogInfo cat_info;


	//----- History -----

	// Catalog magnitude of completeness.

	public double magCat;

	// Number of ruptures.

	public int rupture_count;

	// Number of intervals.

	public int interval_count;

	// Number of ruptures that were accepted.
	// Note: This is primarily an output to support testing.

	public int accept_count;

	// Number of ruptures that were rejected.
	// Note: This is primarily an output to support testing.

	public int reject_count;

	// Number of accepted ruptures in the excitation interval. [v3]
	// Note: This is primarily an output to support testing.
	// (Set to min(1, accept_count) when loaded from a v1 or v2 file.)

	public int excitation_count;

	// Number of accepted ruptures in the fitting interval. [v3]
	// Note: This is primarily an output to support testing.
	// (Set to max(0, accept_count-1) when loaded from a v1 or v2 file.)

	public int fitting_count;


	//----- Fitting -----

	// Number of groups.  (Zero if grouping was not enabled.)

	public int group_count;

	// Mainshock magnitude, the largest magnitude among ruptures considered mainshocks, or NO_MAG_NEG if none.

	public double mag_main;

	// Time interval for interpreting branch ratios, in days.

	public double tint_br;

	// Fitting reference magnitude, also the minimum considered magnitude, for parameter definition. [v2]
	// Currently this is always OEConstants.DEF_MREF = 3.0 (see OEtasParameters.get_fmag_range()).

	public double fitting_mref;

	// Fitting maximum considered magnitude, for parameter definition. [v2]
	// Currently this is always OEConstants.DEF_MSUP = 9.5 (see OEtasParameters.get_fmag_range()).

	public double fitting_msup;

	// Fitting assumed minimum magnitude to use for the simulation. [v2]
	// Currently this is the magnitude of completeness after history construction (see OEtasParameters.get_fmag_range()).
	// (Set to OEConstants.DEF_MREF when loaded from a v1 file).

	public double fitting_mag_min;

	// Fitting assumed maximum magnitude to use for the simulation. [v2]
	// Currently this is the mag_top as modified by the catalog maximum magnitudes (see OEtasParameters.get_fmag_range()).
	// (Set to OEConstants.DEF_MSUP when loaded from a v1 file).

	public double fitting_mag_max;

	// The Bayesian prior. [v2]
	// (Set to a uniform prior if loaded from a v1 file).

	public OEBayPrior bay_prior;


	//----- Grid -----

	// The MLE grid point.

	public OEGridPoint mle_grid_point;

	// The MLE grid points for generic, sequence-specific, and Bayesian models.

	public OEGridPoint gen_mle_grid_point;
	public OEGridPoint seq_mle_grid_point;
	public OEGridPoint bay_mle_grid_point;

	// Bayesian prior weight (0 = Sequence-specific, 1 = Bayesian, 2 = Generic, see OEConstants.BAY_WT_XXX).

	public double bay_weight;


	//----- Simulation -----

	// The range of time and magnitude used for simulation.

	public OECatalogRange sim_range;

	// Number of simulations.

	public int sim_count;


	//----- Forecast -----

	// ETAS results JSON.
	// Must be non-null, but can be empty to indicate not available.

	public String etas_json = "";

	// Return true if there is a (non-null and non-empty) ETAS results JSON.

	@Override
	public boolean has_etas_json () {
		if (etas_json == null || etas_json.isEmpty()) {
			return false;
		}
		return true;
	}

	// Get the ETAS results JSON, or null or empty if none.

	@Override
	public String get_etas_json () {
		return etas_json;
	}

	// Set the ETAS results JSON.
	// The purpose of this function is to be able to adjust forecast info after the
	// computation is complete (such as injectable text). It must only be called after
	// a call to get_etas_json that returns a non-null non-empty result.  The supplied
	// json_text must be non-null and non-empty.

	public void set_etas_json (String json_text) {
		etas_json = json_text;
		return;
	}


	//----- Marginals -----

	// True if the marginals should be saved during marshaling. [v2]

	public boolean save_marginals;

	// Marginals (single variate for active Bayesian weight), or null if none. [v2]
	// If this is null, then same_marginals must be false.

	public OEMarginalDistSet marginals;

	// True if the full marginals should be saved during marshaling. [v2]

	public boolean save_full_marginals;

	// Full marginals (bivariate for multiple Bayesian weights), or null if none. [v2]
	// If this is null, then save_full_marginals must be false.

	public OEMarginalDistSet full_marginals;


	//----- GUI Support -----

	// Integrated intensity function.
	// This field is not marshaled.

	public OEtasIntegratedIntensityFile ii_file;




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		cat_info = null;

		magCat = 0.0;
		rupture_count = 0;
		interval_count = 0;
		accept_count = 0;
		reject_count = 0;
		excitation_count = 0;
		fitting_count = 0;

		group_count = 0;
		mag_main = 0.0;
		tint_br = 0.0;
		fitting_mref = 0.0;
		fitting_msup = 0.0;
		fitting_mag_min = 0.0;
		fitting_mag_max = 0.0;
		bay_prior = null;

		mle_grid_point = null;
		gen_mle_grid_point = null;
		seq_mle_grid_point = null;
		bay_mle_grid_point = null;
		bay_weight = 0.0;

		sim_range = null;
		sim_count = 0;

		etas_json = "";

		save_marginals = false;
		marginals = null;
		save_full_marginals = false;
		full_marginals = null;

		ii_file = null;
		return;
	}




	// Default constructor.

	public OEtasResults () {
		clear();
	}




	// Copy the values.

	public final OEtasResults copy_from (OEtasResults other) {
		this.cat_info = (new OEtasCatalogInfo()).copy_from (other.cat_info);

		this.magCat = other.magCat;
		this.rupture_count = other.rupture_count;
		this.interval_count = other.interval_count;
		this.accept_count = other.accept_count;
		this.reject_count = other.reject_count;
		this.excitation_count = other.excitation_count;
		this.fitting_count = other.fitting_count;

		this.group_count = other.group_count;
		this.mag_main = other.mag_main;
		this.tint_br = other.tint_br;
		this.fitting_mref = other.fitting_mref;
		this.fitting_msup = other.fitting_msup;
		this.fitting_mag_min = other.fitting_mag_min;
		this.fitting_mag_max = other.fitting_mag_max;
		this.bay_prior = other.bay_prior;		// OEBayPrior is immutable

		this.mle_grid_point = (new OEGridPoint()).copy_from (other.mle_grid_point);
		this.gen_mle_grid_point = (new OEGridPoint()).copy_from (other.gen_mle_grid_point);
		this.seq_mle_grid_point = (new OEGridPoint()).copy_from (other.seq_mle_grid_point);
		this.bay_mle_grid_point = (new OEGridPoint()).copy_from (other.bay_mle_grid_point);
		this.bay_weight = other.bay_weight;

		this.sim_range = (new OECatalogRange()).copy_from (other.sim_range);
		this.sim_count = other.sim_count;

		this.etas_json = other.etas_json;

		this.save_marginals = other.save_marginals;
		if (other.marginals == null) {
			this.marginals = null;
		} else {
			this.marginals = (new OEMarginalDistSet()).copy_from (other.marginals);
		}
		this.save_full_marginals = other.save_full_marginals;
		if (other.full_marginals == null) {
			this.full_marginals = null;
		} else {
			this.full_marginals = (new OEMarginalDistSet()).copy_from (other.full_marginals);
		}

		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasResults:" + "\n");

		result.append ("cat_info = {" + cat_info.toString() + "}\n");

		result.append ("magCat = " + magCat + "\n");
		result.append ("rupture_count = " + rupture_count + "\n");
		result.append ("interval_count = " + interval_count + "\n");
		result.append ("accept_count = " + accept_count + "\n");
		result.append ("reject_count = " + reject_count + "\n");
		result.append ("excitation_count = " + excitation_count + "\n");
		result.append ("fitting_count = " + fitting_count + "\n");

		result.append ("group_count = " + group_count + "\n");
		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = " + tint_br + "\n");
		result.append ("fitting_mref = " + fitting_mref + "\n");
		result.append ("fitting_msup = " + fitting_msup + "\n");
		result.append ("fitting_mag_min = " + fitting_mag_min + "\n");
		result.append ("fitting_mag_max = " + fitting_mag_max + "\n");
		result.append ("bay_prior = {" + bay_prior.toString() + "}\n");

		result.append ("mle_grid_point = {" + mle_grid_point.toString() + "}\n");
		result.append ("gen_mle_grid_point = {" + gen_mle_grid_point.toString() + "}\n");
		result.append ("seq_mle_grid_point = {" + seq_mle_grid_point.toString() + "}\n");
		result.append ("bay_mle_grid_point = {" + bay_mle_grid_point.toString() + "}\n");
		result.append ("bay_weight = " + bay_weight + "\n");

		result.append ("sim_range = {" + sim_range.toString() + "}\n");
		result.append ("sim_count = " + sim_count + "\n");

		result.append ("etas_json = " + etas_json + "\n");

		result.append ("save_marginals = " + save_marginals + "\n");
		result.append ("marginals = " + ((marginals == null) ? "null" : (marginals.toString())) + "\n");
		result.append ("save_full_marginals = " + save_full_marginals + "\n");
		result.append ("full_marginals = " + ((full_marginals == null) ? "null" : (full_marginals.toString())) + "\n");

		return result.toString();
	}




	// Display our contents, with the JSON string in display format.

	@Override
	public String to_display_string () {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasResults:" + "\n");

		result.append ("cat_info = {" + cat_info.toString() + "}\n");

		result.append ("magCat = " + magCat + "\n");
		result.append ("rupture_count = " + rupture_count + "\n");
		result.append ("interval_count = " + interval_count + "\n");
		result.append ("accept_count = " + accept_count + "\n");
		result.append ("reject_count = " + reject_count + "\n");
		result.append ("excitation_count = " + excitation_count + "\n");
		result.append ("fitting_count = " + fitting_count + "\n");

		result.append ("group_count = " + group_count + "\n");
		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = " + tint_br + "\n");
		result.append ("fitting_mref = " + fitting_mref + "\n");
		result.append ("fitting_msup = " + fitting_msup + "\n");
		result.append ("fitting_mag_min = " + fitting_mag_min + "\n");
		result.append ("fitting_mag_max = " + fitting_mag_max + "\n");
		result.append ("bay_prior = {" + bay_prior.toString() + "}\n");

		result.append ("mle_grid_point = {" + mle_grid_point.toString() + "}\n");
		result.append ("gen_mle_grid_point = {" + gen_mle_grid_point.toString() + "}\n");
		result.append ("seq_mle_grid_point = {" + seq_mle_grid_point.toString() + "}\n");
		result.append ("bay_mle_grid_point = {" + bay_mle_grid_point.toString() + "}\n");
		result.append ("bay_weight = " + bay_weight + "\n");

		result.append ("sim_range = {" + sim_range.toString() + "}\n");
		result.append ("sim_count = " + sim_count + "\n");

		result.append ("etas_json:" + "\n");
		if (etas_json.length() > 0) {
			result.append (MarshalUtils.display_json_string (etas_json));
		}

		result.append ("save_marginals = " + save_marginals + "\n");
		result.append ("marginals = " + ((marginals == null) ? "null" : (marginals.toString())) + "\n");
		result.append ("save_full_marginals = " + save_full_marginals + "\n");
		result.append ("full_marginals = " + ((full_marginals == null) ? "null" : (full_marginals.toString())) + "\n");

		return result.toString();
	}




	// Set input data.
	// Parameters:
	//  the_cat_info = Catalog information.  This object is not retained.

	public void set_inputs (OEtasCatalogInfo the_cat_info) {
		cat_info = (new OEtasCatalogInfo()).copy_from (the_cat_info);
		return;
	}




	// Set history data.
	// Parameters:
	//  history = The history.

	public void set_history (OEDisc2History history) {
		magCat = history.magCat;
		rupture_count = history.rupture_count;
		interval_count = history.interval_count;
		accept_count = history.accept_count;
		reject_count = history.reject_count;
		excitation_count = history.i_inside_begin;
		fitting_count = history.i_inside_end - history.i_inside_begin;
		return;
	}




	// Set fitting data.
	// Parameters:
	//  fit_info = Fitting information.
	//  the_bay_prior = Bayesian prior.

	public void set_fitting (OEDisc2InitFitInfo fit_info, OEBayPrior the_bay_prior) {
		group_count = fit_info.group_count;
		mag_main = fit_info.mag_main;
		tint_br = fit_info.tint_br;
		fitting_mref = fit_info.mref;
		fitting_msup = fit_info.msup;
		fitting_mag_min = fit_info.mag_min;
		fitting_mag_max = fit_info.mag_max;
		bay_prior = the_bay_prior;		// OEBayPrior is immutable
		return;
	}




	// Set grid data.
	// Parameters:
	//  voxel_set = Voxel set containing the grid.

	public void set_grid (OEDisc2InitVoxSet voxel_set) {
		mle_grid_point = voxel_set.get_mle_grid_point();
		gen_mle_grid_point = voxel_set.get_gen_mle_grid_point();
		seq_mle_grid_point = voxel_set.get_seq_mle_grid_point();
		bay_mle_grid_point = voxel_set.get_bay_mle_grid_point();
		bay_weight = voxel_set.get_bay_weight();
		return;
	}




	// Set simulation data.
	// Parameters:
	//  simulator = The simulator.

	public void set_simulation (OESimulator simulator) {
		sim_range = (new OECatalogRange()).copy_from (simulator.sim_catalog_range);
		sim_count = simulator.sim_count;
		return;
	}




	// Set forecast data.
	// Parameters:
	//  json_string = String containing forecast JSON.

	public void set_forecast (String json_string) {
		etas_json = json_string;
		return;
	}




	// Set marginals.
	// Parameters:
	//  save_marginals = True if the marginals should be saved during marshaling.
	//  marginals = Marginals (single variate for active Bayesian weight), or null if none.  (If null, then save_marginals must be false.)
	//  save_full_marginals = True if the full marginals should be saved during marshaling.
	//  full_marginals = Full marginals (bivariate for multiple Bayesian weights), or null if none.  (If null, then save_full_marginals must be false.)

	public void set_marginals (
		boolean save_marginals,
		OEMarginalDistSet marginals,
		boolean save_full_marginals,
		OEMarginalDistSet full_marginals
	) {
		this.save_marginals = save_marginals;
		this.marginals = marginals;
		this.save_full_marginals = save_full_marginals;
		this.full_marginals = full_marginals;
		return;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 126001;
	private static final int MARSHAL_VER_2 = 126002;
	private static final int MARSHAL_VER_3 = 126003;

	private static final String M_VERSION_NAME = "OEtasResults";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_ETAS_RESULTS;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_3;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			OEtasCatalogInfo.static_marshal (writer, "cat_info", cat_info);

			writer.marshalDouble ("magCat", magCat);
			writer.marshalInt ("rupture_count", rupture_count);
			writer.marshalInt ("interval_count", interval_count);
			writer.marshalInt ("accept_count", accept_count);
			writer.marshalInt ("reject_count", reject_count);

			writer.marshalInt ("group_count", group_count);
			writer.marshalDouble ("mag_main", mag_main);
			writer.marshalDouble ("tint_br", tint_br);

			OEGridPoint.static_marshal (writer, "mle_grid_point", mle_grid_point);
			OEGridPoint.static_marshal (writer, "gen_mle_grid_point", gen_mle_grid_point);
			OEGridPoint.static_marshal (writer, "seq_mle_grid_point", seq_mle_grid_point);
			OEGridPoint.static_marshal (writer, "bay_mle_grid_point", bay_mle_grid_point);
			writer.marshalDouble ("bay_weight", bay_weight);

			OECatalogRange.static_marshal (writer, "sim_range", sim_range);
			writer.marshalInt ("sim_count", sim_count);

			writer.marshalJsonString ("etas_json", etas_json);

		}
		break;

		case MARSHAL_VER_2: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			OEtasCatalogInfo.static_marshal (writer, "cat_info", cat_info);

			writer.marshalDouble ("magCat", magCat);
			writer.marshalInt ("rupture_count", rupture_count);
			writer.marshalInt ("interval_count", interval_count);
			writer.marshalInt ("accept_count", accept_count);
			writer.marshalInt ("reject_count", reject_count);

			writer.marshalInt ("group_count", group_count);
			writer.marshalDouble ("mag_main", mag_main);
			writer.marshalDouble ("tint_br", tint_br);
			writer.marshalDouble ("fitting_mref", fitting_mref);
			writer.marshalDouble ("fitting_msup", fitting_msup);
			writer.marshalDouble ("fitting_mag_min", fitting_mag_min);
			writer.marshalDouble ("fitting_mag_max", fitting_mag_max);
			OEBayPrior.marshal_poly (writer, "bay_prior", bay_prior);

			OEGridPoint.static_marshal (writer, "mle_grid_point", mle_grid_point);
			OEGridPoint.static_marshal (writer, "gen_mle_grid_point", gen_mle_grid_point);
			OEGridPoint.static_marshal (writer, "seq_mle_grid_point", seq_mle_grid_point);
			OEGridPoint.static_marshal (writer, "bay_mle_grid_point", bay_mle_grid_point);
			writer.marshalDouble ("bay_weight", bay_weight);

			OECatalogRange.static_marshal (writer, "sim_range", sim_range);
			writer.marshalInt ("sim_count", sim_count);

			writer.marshalJsonString ("etas_json", etas_json);

			writer.marshalBoolean ("save_marginals", save_marginals);
			if (save_marginals) {
				OEMarginalDistSet.static_marshal (writer, "marginals", marginals);
			}
			writer.marshalBoolean ("save_full_marginals", save_full_marginals);
			if (save_full_marginals) {
				OEMarginalDistSet.static_marshal (writer, "full_marginals", full_marginals);
			}

		}
		break;

		case MARSHAL_VER_3: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			OEtasCatalogInfo.static_marshal (writer, "cat_info", cat_info);

			writer.marshalDouble ("magCat", magCat);
			writer.marshalInt ("rupture_count", rupture_count);
			writer.marshalInt ("interval_count", interval_count);
			writer.marshalInt ("accept_count", accept_count);
			writer.marshalInt ("reject_count", reject_count);
			writer.marshalInt ("excitation_count", excitation_count);
			writer.marshalInt ("fitting_count", fitting_count);

			writer.marshalInt ("group_count", group_count);
			writer.marshalDouble ("mag_main", mag_main);
			writer.marshalDouble ("tint_br", tint_br);
			writer.marshalDouble ("fitting_mref", fitting_mref);
			writer.marshalDouble ("fitting_msup", fitting_msup);
			writer.marshalDouble ("fitting_mag_min", fitting_mag_min);
			writer.marshalDouble ("fitting_mag_max", fitting_mag_max);
			OEBayPrior.marshal_poly (writer, "bay_prior", bay_prior);

			OEGridPoint.static_marshal (writer, "mle_grid_point", mle_grid_point);
			OEGridPoint.static_marshal (writer, "gen_mle_grid_point", gen_mle_grid_point);
			OEGridPoint.static_marshal (writer, "seq_mle_grid_point", seq_mle_grid_point);
			OEGridPoint.static_marshal (writer, "bay_mle_grid_point", bay_mle_grid_point);
			writer.marshalDouble ("bay_weight", bay_weight);

			OECatalogRange.static_marshal (writer, "sim_range", sim_range);
			writer.marshalInt ("sim_count", sim_count);

			writer.marshalJsonString ("etas_json", etas_json);

			writer.marshalBoolean ("save_marginals", save_marginals);
			if (save_marginals) {
				OEMarginalDistSet.static_marshal (writer, "marginals", marginals);
			}
			writer.marshalBoolean ("save_full_marginals", save_full_marginals);
			if (save_full_marginals) {
				OEMarginalDistSet.static_marshal (writer, "full_marginals", full_marginals);
			}

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_3);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cat_info = OEtasCatalogInfo.static_unmarshal (reader, "cat_info");

			magCat = reader.unmarshalDouble ("magCat");
			rupture_count = reader.unmarshalInt ("rupture_count");
			interval_count = reader.unmarshalInt ("interval_count");
			accept_count = reader.unmarshalInt ("accept_count");
			reject_count = reader.unmarshalInt ("reject_count");
			excitation_count = Math.min(1, accept_count);
			fitting_count = Math.max(0, accept_count-1);

			group_count = reader.unmarshalInt ("group_count");
			mag_main = reader.unmarshalDouble ("mag_main");
			tint_br = reader.unmarshalDouble ("tint_br");
			fitting_mref = OEConstants.DEF_MREF;
			fitting_msup = OEConstants.DEF_MSUP;
			fitting_mag_min = OEConstants.DEF_MREF;
			fitting_mag_max = OEConstants.DEF_MSUP;
			bay_prior = OEBayPrior.makeUniform();

			mle_grid_point = OEGridPoint.static_unmarshal (reader, "mle_grid_point");
			gen_mle_grid_point = OEGridPoint.static_unmarshal (reader, "gen_mle_grid_point");
			seq_mle_grid_point = OEGridPoint.static_unmarshal (reader, "seq_mle_grid_point");
			bay_mle_grid_point = OEGridPoint.static_unmarshal (reader, "bay_mle_grid_point");
			bay_weight = reader.unmarshalDouble ("bay_weight");

			sim_range = OECatalogRange.static_unmarshal (reader, "sim_range");
			sim_count = reader.unmarshalInt ("sim_count");

			etas_json = reader.unmarshalJsonString ("etas_json");

			save_marginals = false;
			marginals = null;
			save_full_marginals = false;
			full_marginals = null;

		}
		break;

		case MARSHAL_VER_2: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cat_info = OEtasCatalogInfo.static_unmarshal (reader, "cat_info");

			magCat = reader.unmarshalDouble ("magCat");
			rupture_count = reader.unmarshalInt ("rupture_count");
			interval_count = reader.unmarshalInt ("interval_count");
			accept_count = reader.unmarshalInt ("accept_count");
			reject_count = reader.unmarshalInt ("reject_count");
			excitation_count = Math.min(1, accept_count);
			fitting_count = Math.max(0, accept_count-1);

			group_count = reader.unmarshalInt ("group_count");
			mag_main = reader.unmarshalDouble ("mag_main");
			tint_br = reader.unmarshalDouble ("tint_br");
			fitting_mref = reader.unmarshalDouble ("fitting_mref");
			fitting_msup = reader.unmarshalDouble ("fitting_msup");
			fitting_mag_min = reader.unmarshalDouble ("fitting_mag_min");
			fitting_mag_max = reader.unmarshalDouble ("fitting_mag_max");
			bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");

			mle_grid_point = OEGridPoint.static_unmarshal (reader, "mle_grid_point");
			gen_mle_grid_point = OEGridPoint.static_unmarshal (reader, "gen_mle_grid_point");
			seq_mle_grid_point = OEGridPoint.static_unmarshal (reader, "seq_mle_grid_point");
			bay_mle_grid_point = OEGridPoint.static_unmarshal (reader, "bay_mle_grid_point");
			bay_weight = reader.unmarshalDouble ("bay_weight");

			sim_range = OECatalogRange.static_unmarshal (reader, "sim_range");
			sim_count = reader.unmarshalInt ("sim_count");

			etas_json = reader.unmarshalJsonString ("etas_json");

			save_marginals = reader.unmarshalBoolean ("save_marginals");
			if (save_marginals) {
				marginals = OEMarginalDistSet.static_unmarshal (reader, "marginals");
			} else {
				marginals = null;
			}
			save_full_marginals = reader.unmarshalBoolean ("save_full_marginals");
			if (save_full_marginals) {
				full_marginals = OEMarginalDistSet.static_unmarshal (reader, "full_marginals");
			} else {
				full_marginals = null;
			}

		}
		break;

		case MARSHAL_VER_3: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cat_info = OEtasCatalogInfo.static_unmarshal (reader, "cat_info");

			magCat = reader.unmarshalDouble ("magCat");
			rupture_count = reader.unmarshalInt ("rupture_count");
			interval_count = reader.unmarshalInt ("interval_count");
			accept_count = reader.unmarshalInt ("accept_count");
			reject_count = reader.unmarshalInt ("reject_count");
			excitation_count = reader.unmarshalInt ("excitation_count");
			fitting_count = reader.unmarshalInt ("fitting_count");

			group_count = reader.unmarshalInt ("group_count");
			mag_main = reader.unmarshalDouble ("mag_main");
			tint_br = reader.unmarshalDouble ("tint_br");
			fitting_mref = reader.unmarshalDouble ("fitting_mref");
			fitting_msup = reader.unmarshalDouble ("fitting_msup");
			fitting_mag_min = reader.unmarshalDouble ("fitting_mag_min");
			fitting_mag_max = reader.unmarshalDouble ("fitting_mag_max");
			bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");

			mle_grid_point = OEGridPoint.static_unmarshal (reader, "mle_grid_point");
			gen_mle_grid_point = OEGridPoint.static_unmarshal (reader, "gen_mle_grid_point");
			seq_mle_grid_point = OEGridPoint.static_unmarshal (reader, "seq_mle_grid_point");
			bay_mle_grid_point = OEGridPoint.static_unmarshal (reader, "bay_mle_grid_point");
			bay_weight = reader.unmarshalDouble ("bay_weight");

			sim_range = OECatalogRange.static_unmarshal (reader, "sim_range");
			sim_count = reader.unmarshalInt ("sim_count");

			etas_json = reader.unmarshalJsonString ("etas_json");

			save_marginals = reader.unmarshalBoolean ("save_marginals");
			if (save_marginals) {
				marginals = OEMarginalDistSet.static_unmarshal (reader, "marginals");
			} else {
				marginals = null;
			}
			save_full_marginals = reader.unmarshalBoolean ("save_full_marginals");
			if (save_full_marginals) {
				full_marginals = OEMarginalDistSet.static_unmarshal (reader, "full_marginals");
			} else {
				full_marginals = null;
			}

		}
		break;

		}

		return;
	}

//	// Marshal object.
//
//	@Override
//	public void marshal (MarshalWriter writer, String name) {
//		writer.marshalMapBegin (name);
//		int mytype = get_marshal_type();
//		writer.marshalInt (M_TYPE_NAME, mytype);
//		do_marshal (writer);
//		writer.marshalMapEnd ();
//		return;
//	}

//	// Unmarshal object.
//
//	@Override
//	public OEtasOutcome unmarshal (MarshalReader reader, String name) {
//		reader.unmarshalMapBegin (name);
//		int mytype = get_marshal_type();
//		int type = reader.unmarshalInt (M_TYPE_NAME, mytype, mytype);
//		do_umarshal (reader);
//		reader.unmarshalMapEnd ();
//		return this;
//	}

//	// Marshal object.
//
//	public static void static_marshal (MarshalWriter writer, String name, OEtasResults etas_results) {
//		etas_results.marshal (writer, name);
//		return;
//	}

//	// Unmarshal object.
//
//	public static OEtasResults static_unmarshal (MarshalReader reader, String name) {
//		return (new OEtasResults()).unmarshal (reader, name);
//	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEtasResults obj) {

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

	public static OEtasResults unmarshal_poly (MarshalReader reader, String name) {
		OEtasResults result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEtasResults.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_ETAS_RESULTS:
			result = new OEtasResults();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	private static OEtasResults make_test_value () {
		OEtasResults etas_results = new OEtasResults();

		etas_results.cat_info = OEtasCatalogInfo.make_test_value();

		etas_results.magCat = 3.01;
		etas_results.rupture_count = 3000;
		etas_results.interval_count = 250;
		etas_results.accept_count = 3500;
		etas_results.reject_count = 2500;
		etas_results.excitation_count = Math.min(1, etas_results.accept_count);
		etas_results.fitting_count = Math.max(0, etas_results.accept_count-1);

		etas_results.group_count = 150;
		etas_results.mag_main = 7.8;
		etas_results.tint_br = 365.0;
		etas_results.fitting_mref = OEConstants.DEF_MREF;
		etas_results.fitting_msup = OEConstants.DEF_MSUP;
		etas_results.fitting_mag_min = OEConstants.DEF_MREF;
		etas_results.fitting_mag_max = OEConstants.DEF_MSUP;
		etas_results.bay_prior = OEBayPrior.makeUniform();

		etas_results.mle_grid_point = (new OEGridPoint()).set (1.00, 1.10, 0.200, 1.20, 0.800, 2.60, 3.40);
		etas_results.gen_mle_grid_point = (new OEGridPoint()).set (1.01, 1.11, 0.201, 1.21, 0.801, 2.61, 3.41);
		etas_results.seq_mle_grid_point = (new OEGridPoint()).set (1.02, 1.12, 0.202, 1.22, 0.802, 2.62, 3.42);
		etas_results.bay_mle_grid_point = (new OEGridPoint()).set (1.03, 1.13, 0.203, 1.23, 0.803, 2.63, 3.43);
		etas_results.bay_weight = 1.0;

		etas_results.sim_range = (new OECatalogRange()).set_range_seed_est (0.0, 365.0, 3.0, 8.0, -4.0, 8.5, -3.0, 9.5, 100, 0.0, 20, 0.90, 0.02, 800);
		etas_results.sim_count = 500000;

		etas_results.etas_json = "{\"msg\" : \"This is a test\", \"code\" : 1234}";

		etas_results.save_marginals = true;
		etas_results.marginals = OEMarginalDistSet.make_test_value_2 (1000000);
		etas_results.save_full_marginals = false;
		etas_results.full_marginals = OEMarginalDistSet.make_test_value (1000000);

		return etas_results;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasResults");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying results");
			testargs.end_test();

			// Create the values

			OEtasResults etas_results = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Result Display **********");
			System.out.println ();

			System.out.println (etas_results.toString());

			// Display the contents, in display format

			System.out.println ();
			System.out.println ("********** Result Display, in Display Format **********");
			System.out.println ();

			System.out.println (etas_results.to_display_string());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (etas_results);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (etas_results);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEtasResults etas_results2 = new OEtasResults();
			MarshalUtils.from_json_string (etas_results2, json_string);

			// Display the contents

			System.out.println (etas_results2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEtasResults etas_results3 = new OEtasResults();
			etas_results3.copy_from (etas_results2);

			// Display the contents

			System.out.println (etas_results3.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal the copy to JSON **********");
			System.out.println ();

			//String json_string3 = MarshalUtils.to_json_string (etas_results3);
			//System.out.println (MarshalUtils.display_json_string (json_string3));

			String json_string3 = MarshalUtils.to_formatted_compact_json_string (etas_results3);
			System.out.println (json_string3);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename
		// Construct test values, and write them to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Writing test values to a file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasResults etas_results = make_test_value();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_json_string (etas_results);

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
		// Construct test values, and write them to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Writing test values to a file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasResults etas_results = make_test_value();

			// Write to file

			MarshalUtils.to_json_file (etas_results, filename);

			// Read back the file and display it

			OEtasResults etas_results2 = new OEtasResults();
			MarshalUtils.from_json_file (etas_results2, filename);

			System.out.println ();
			System.out.println (etas_results2.toString());

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

			System.out.println ("Writing test values to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEtasResults etas_results = make_test_value();

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_results, filename);

			// Read back the file and display it

			OEtasResults etas_results2 = new OEtasResults();
			MarshalUtils.from_json_file (etas_results2, filename);

			System.out.println ();
			System.out.println (etas_results2.toString());

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
