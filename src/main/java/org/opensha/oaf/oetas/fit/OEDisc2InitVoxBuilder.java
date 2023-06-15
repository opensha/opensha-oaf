package org.opensha.oaf.oetas.fit;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.AutoCloseList;
import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleThreadLoopHelper;
import org.opensha.oaf.util.SimpleThreadLoopResult;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.oetas.except.OEException;
import org.opensha.oaf.oetas.except.OEFitException;
import org.opensha.oaf.oetas.except.OEFitThreadAbortException;
import org.opensha.oaf.oetas.except.OEFitTimeoutException;


// Operational ETAS catalog initializer builder.
// Author: Michael Barall 03/15/2023.

public class OEDisc2InitVoxBuilder {


	//----- Shared objects -----

	// The voxel consumer, shared by all threads.

	private OEDisc2InitVoxConsumer voxel_consumer;

	// Parameter fitter, shared by all threads.

	private OEDisc2ExtFit fitter;

	// Fitting information, shared by all threads.

	private OEDisc2InitFitInfo fit_info;

	// The Bayesian prior, shared by all threads.

	private OEBayPrior bay_prior;




	// Set up the shared objects.
	// Parameters:
	//  voxel_consumer = The voxel consumer.
	//  fitter = Parameter fitter, which has been already set up.
	//  bay_prior = The Bayesian prior.

	public final OEDisc2InitVoxBuilder setup_vbld (
		OEDisc2InitVoxConsumer voxel_consumer,
		OEDisc2ExtFit fitter,
		OEBayPrior bay_prior
	) {
		this.voxel_consumer = voxel_consumer;
		this.fitter = fitter;
		this.bay_prior = bay_prior;

		this.fit_info = fitter.get_fit_info();

		return this;
	}




	//----- Grid parameters -----

	// Combinations of (b, alpha) parameters.

	private OEDisc2InitDefVoxBAlpha b_alpha_def;

	// Combinations of (c, p) parameters.

	private OEDisc2InitDefVoxCP c_p_def;

	// Combinations of (n) parameter.

	private OEDisc2InitDefVoxN n_def;

	// Combinations of (zams, zmu) parameters, which define the sub-voxels.

	private OEDisc2InitSubVoxDef sub_vox_def;




	// Set up the grid by providing the desired combinations.
	// Parameters:
	//  b_alpha_def = Combinations of (b, alpha) parameters.
	//  c_p_def = Combinations of (c, p) parameters.
	//  n_def = Combinations of (n) parameter.
	//  sub_vox_def = Combinations of (zams, zmu) parameters, which define the sub-voxels.
	// Returns this object.

	public final OEDisc2InitVoxBuilder setup_grid (
		OEDisc2InitDefVoxBAlpha b_alpha_def,
		OEDisc2InitDefVoxCP c_p_def,
		OEDisc2InitDefVoxN n_def,
		OEDisc2InitSubVoxDef sub_vox_def
	) {
		this.b_alpha_def = b_alpha_def;
		this.c_p_def = c_p_def;
		this.n_def = n_def;
		this.sub_vox_def = sub_vox_def;

		return this;
	}




	// Set up the grid from a set of separate parameter ranges.
	// Parameters:
	//  grid_params = Contains separate ranges for each parameter.
	// Returns this object.

	public final OEDisc2InitVoxBuilder setup_grid (
		OEGridParams grid_params
	) {
		this.b_alpha_def = (new OEDisc2InitDefVoxBAlpha()).set_from_sep_ranges (
			grid_params.b_range,
			grid_params.alpha_range
		);
		this.c_p_def = (new OEDisc2InitDefVoxCP()).set_from_sep_ranges (
			grid_params.c_range,
			grid_params.p_range
		);
		this.n_def = (new OEDisc2InitDefVoxN()).set_from_sep_ranges (
			grid_params.n_range
		);
		this.sub_vox_def = (new OEDisc2InitSubVoxDef()).set_from_sep_ranges (
			grid_params.zams_range,
			grid_params.zmu_range
		);

		return this;
	}




	//----- Partial Voxels -----




	// Partial voxel for (b, alpha) pairs.

	private static class PartialVoxBAlpha implements AutoCloseable {

		// The mexp handle.

		private OEDisc2ExtFit.MagExponentHandle mexp;

		public final OEDisc2ExtFit.MagExponentHandle get_mexp () {
			if (!( mexp != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxBAlpha.get_mexp: Handle not built yet.");
			}
			return mexp;
		}

		// Value element for b.

		private OEValueElement b_velt;

		public final OEValueElement get_b_velt () {
			return b_velt;
		}

		// Value element for alpha, can be null to force alpha == b.

		private OEValueElement alpha_velt;

		public final OEValueElement get_alpha_velt () {
			return alpha_velt;
		}

		// Clear the contents.

		public final void clear () {
			mexp = null;
			b_velt = null;
			alpha_velt = null;
			return;
		}

		// Constructor creates an empty object.

		public PartialVoxBAlpha () {
			clear();
		}

		// Constructor creates an object with given parameters.
		// Parameters:
		//  def_vox = (b, alpha) definitions.
		//  index = Index within def_vox.

		public PartialVoxBAlpha (OEDisc2InitDefVoxBAlpha def_vox, int index) {
			clear();
			b_velt = def_vox.get_b_velt (index);
			alpha_velt = def_vox.get_alpha_velt (index);
		}

		// Build the mexp object, assuming parameters are already set.
		// Parameters:
		//  the_fitter = Parameter fitter.

		public final void mexp_build (OEDisc2ExtFit the_fitter) {

			if (!( b_velt != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxBAlpha.mexp_build: Parameters not yet.");
			}

			// Get the parameters

			final double b = b_velt.get_ve_value();
			final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

			// Allocate the handle if needed

			if (mexp == null) {
				mexp = the_fitter.make_MagExponentHandle();
			}

			// Build the handle

			mexp.mexp_build (b, alpha);
			return;
		}

		// Build the mexp object.
		// Parameters:
		//  the_fitter = Parameter fitter.
		//  def_vox = (b, alpha) definitions.
		//  index = Index within def_vox.

		public final void mexp_build (OEDisc2ExtFit the_fitter, OEDisc2InitDefVoxBAlpha def_vox, int index) {

			// Get the parameters

			b_velt = def_vox.get_b_velt (index);
			alpha_velt = def_vox.get_alpha_velt (index);

			mexp_build (the_fitter);
			return;
		}

		// Closing closes the contained handle.

		@Override
		public void close () {
			OEDisc2ExtFit.MagExponentHandle my_mexp = mexp;
			clear();
			if (my_mexp != null) {
				my_mexp.close();
			}
			return;
		}
	}




	// Partial voxel for (c, p) pairs.

	private static class PartialVoxCP implements AutoCloseable {

		// The omat handle.

		private OEDisc2ExtFit.OmoriMatrixHandle omat;

		public final OEDisc2ExtFit.OmoriMatrixHandle get_omat () {
			if (!( omat != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxCP.get_omat: Handle not built yet.");
			}
			return omat;
		}

		// Value element for c.

		private OEValueElement c_velt;

		public final OEValueElement get_c_velt () {
			return c_velt;
		}

		// Value element for p.

		private OEValueElement p_velt;

		public final OEValueElement get_p_velt () {
			return p_velt;
		}

		// Clear the contents.

		public final void clear () {
			omat = null;
			c_velt = null;
			p_velt = null;
			return;
		}

		// Constructor creates an empty object.

		public PartialVoxCP () {
			clear();
		}

		// Constructor creates an object with given parameters.
		// Parameters:
		//  def_vox = (c, p) definitions.
		//  index = Index within def_vox.

		public PartialVoxCP (OEDisc2InitDefVoxCP def_vox, int index) {
			clear();
			c_velt = def_vox.get_c_velt (index);
			p_velt = def_vox.get_p_velt (index);
		}

		// Build the omat object, assuming parameters are already set.
		// Parameters:
		//  the_fitter = Parameter fitter.

		public final void omat_build (OEDisc2ExtFit the_fitter) {

			if (!( c_velt != null && p_velt != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxCP.omat_build: Parameters not yet.");
			}
			// Get the parameters

			final double p = p_velt.get_ve_value();
			final double c = c_velt.get_ve_value();

			// Allocate the handle if needed

			if (omat == null) {
				omat = the_fitter.make_OmoriMatrixHandle();
			}

			// Build the handle

			omat.omat_build (p, c);
			return;
		}

		// Build the omat object.
		// Parameters:
		//  the_fitter = Parameter fitter.
		//  def_vox = (c, p) definitions.
		//  index = Index within def_vox.

		public final void omat_build (OEDisc2ExtFit the_fitter, OEDisc2InitDefVoxCP def_vox, int index) {

			// Get the parameters

			c_velt = def_vox.get_c_velt (index);
			p_velt = def_vox.get_p_velt (index);

			omat_build (the_fitter);
			return;
		}

		// Closing closes the contained handle.

		@Override
		public void close () {
			OEDisc2ExtFit.OmoriMatrixHandle my_omat = omat;
			clear();
			if (my_omat != null) {
				my_omat.close();
			}
			return;
		}
	}




	// Partial voxel for (b, alpha, c, p) quadruples.

	private static class PartialVoxQuad implements AutoCloseable {

		// The pmom handle.

		public OEDisc2ExtFit.PairMagOmoriHandle pmom;

		public final OEDisc2ExtFit.PairMagOmoriHandle get_pmom () {
			if (!( pmom != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxQuad.get_pmom: Handle not built yet.");
			}
			return pmom;
		}

		// Partial voxel for (b, alpha)

		public PartialVoxBAlpha pvox_b_alpha;

		public final PartialVoxBAlpha get_pvox_b_alpha () {
			return pvox_b_alpha;
		}

		// Partial voxel for (c, p)

		public PartialVoxCP pvox_c_p;

		public final PartialVoxCP get_pvox_c_p () {
			return pvox_c_p;
		}

		// Clear the contents.

		public final void clear () {
			pmom = null;
			pvox_b_alpha = null;
			pvox_c_p = null;
			return;
		}

		// Constructor creates an empty object.

		public PartialVoxQuad () {
			clear();
		}

		// Constructor creates an object with given parameters.
		// Parameters:
		//  pvox_b_alpha = Partial voxel for (b, alpha).
		//  pvox_c_p = Partial voxel for (c, p).

		public PartialVoxQuad (PartialVoxBAlpha pvox_b_alpha, PartialVoxCP pvox_c_p) {
			clear();
			this.pvox_b_alpha = pvox_b_alpha;
			this.pvox_c_p = pvox_c_p;
		}

		// Build the pmom object, assuming parameters are already set.
		// Parameters:
		//  the_fitter = Parameter fitter.

		public final void pmom_build (OEDisc2ExtFit the_fitter) {

			if (!( pvox_b_alpha != null && pvox_c_p != null )) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.PartialVoxQuad.pmom_build: Parameters not yet.");
			}

			// Allocate the handle if needed

			if (pmom == null) {
				pmom = the_fitter.make_PairMagOmoriHandle();
			}

			// Build the handle

			pmom.pmom_build (pvox_b_alpha.get_mexp(), pvox_c_p.get_omat());
			return;
		}

		// Build the pmom object.
		// Parameters:
		//  the_fitter = Parameter fitter.
		//  pvox_b_alpha = Partial voxel for (b, alpha).
		//  pvox_c_p = Partial voxel for (c, p).

		public final void pmom_build (OEDisc2ExtFit the_fitter, PartialVoxBAlpha pvox_b_alpha, PartialVoxCP pvox_c_p) {

			// Get the parameters

			this.pvox_b_alpha = pvox_b_alpha;
			this.pvox_c_p = pvox_c_p;

			pmom_build (the_fitter);
			return;
		}

		// Closing closes the contained handle.

		@Override
		public void close () {
			OEDisc2ExtFit.PairMagOmoriHandle my_pmom = pmom;
			clear();
			if (my_pmom != null) {
				my_pmom.close();
			}
			return;
		}
	}




	//----- Result info -----


	

	// The loop result.

	private SimpleThreadLoopResult loop_result = new SimpleThreadLoopResult();




	// Get the loop result for the last operation.
	// The returned object is newly-allocated.

	public final SimpleThreadLoopResult get_loop_result () {
		return (new SimpleThreadLoopResult()).copy_from (loop_result);
	}




	//----- Thread managers for building lists of partial voxels -----




	// Progress message format while running.

	private static final String PMFMT_RUNNING = "Completed %C of %L steps (%P%%) in %E seconds using %U";

	// Progress message format after completion.

	private static final String PMFMT_DONE = "Completed all %L steps in %E seconds";

	// Progress message format for timeout.

	private static final String PMFMT_TIMEOUT = "Stopped after completing %C of %L steps in %E seconds";

	// Progress message format for abort.

	private static final String PMFMT_ABORT = "Aborted after completing %C of %L steps in %E seconds";




	// Class to build a list of mexp objects, one for each (b, alpha) pair.

	private class TM_mexp_list_builder implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (b, alpha) pairs (read-only).

		private List<PartialVoxBAlpha> l_pvox_b_alpha;

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			// Loop until loop completed or prompt termination is requested

			for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

				// Build the mexp object

				l_pvox_b_alpha.get(index).mexp_build (fitter);
			}

			return;
		}

		// Build the list of partial voxels for (b, alpha) pairs.
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.
		//  auto_l_pvox_b_alpha = An empty list, that we will fill with allocated and built partial voxels for (b, alpha) pairs.

		public void build_mexp_list (SimpleExecTimer exec_timer, AutoCloseList<PartialVoxBAlpha> auto_l_pvox_b_alpha) throws OEException {

			// Say hello

			System.out.println ("Start caching data for (b, alpha) pairs");

			// Create the partial voxels

			int b_alpha_count = b_alpha_def.get_combo_count();

			for (int j = 0; j < b_alpha_count; ++j) {
				auto_l_pvox_b_alpha.add (new PartialVoxBAlpha (b_alpha_def, j));
			}

			// Pass read-only list to the threads

			l_pvox_b_alpha = auto_l_pvox_b_alpha.get_read_only_view();

			// Run the loop

			loop_helper.run_loop (this, exec_timer, 0, b_alpha_count);

			// Capture the result

			loop_result.accum_loop (loop_helper);

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort caching data for (b, alpha) pairs because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort caching data for (b, alpha) pairs because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish caching data for (b, alpha) pairs");

			return;
		}

	}




	// Class to build a list of omat objects, one for each (c, p) pair.

	private class TM_omat_list_builder implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (c, p) pairs (read-only).

		private List<PartialVoxCP> l_pvox_c_p;

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			// Loop until loop completed or prompt termination is requested

			for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

				// Build the omat object

				l_pvox_c_p.get(index).omat_build (fitter);
			}

			return;
		}

		// Build the list of partial voxels for (c, p) pairs.
		// Parameters:
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.
		//  auto_l_pvox_c_p = An empty list, that we will fill with allocated and built partial voxels for (c, p) pairs.

		public void build_omat_list (SimpleExecTimer exec_timer, AutoCloseList<PartialVoxCP> auto_l_pvox_c_p) throws OEException {

			// Say hello

			System.out.println ("Start caching data for (c, p) pairs");

			// Create the partial voxels

			int c_p_count = c_p_def.get_combo_count();

			for (int j = 0; j < c_p_count; ++j) {
				auto_l_pvox_c_p.add (new PartialVoxCP (c_p_def, j));
			}

			// Pass read-only list to the threads

			l_pvox_c_p = auto_l_pvox_c_p.get_read_only_view();

			// Run the loop

			loop_helper.run_loop (this, exec_timer, 0, c_p_count);

			// Capture the result

			loop_result.accum_loop (loop_helper);

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort caching data for (c, p) pairs because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort caching data for (c, p) pairs because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish caching data for (c, p) pairs");

			return;
		}

	}




	// Class to build a list of pmom objects, one for each (b, alpha, c, p) quadruple.

	private class TM_pmom_list_builder implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (b, alpha) pairs (read-only).

		private List<PartialVoxBAlpha> l_pvox_b_alpha;

		// List of partial voxels for (c, p) pairs (read-only).

		private List<PartialVoxCP> l_pvox_c_p;

		// List of partial voxels for (b, alpha, c, p) quadruples (read-only).

		private List<PartialVoxQuad> l_pvox_quad;

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			// Loop until loop completed or prompt termination is requested

			for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

				// Build the pmom object

				l_pvox_quad.get(index).pmom_build (fitter);
			}

			return;
		}

		// Build the list of partial voxels for (b, alpha, c, p) quadruples.
		// Parameters:
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.
		//  the_l_pvox_b_alpha = List of partial voxels for (b, alpha) pairs (read-only).
		//  the_l_pvox_c_p = List of partial voxels for (c, p) pairs (read-only).
		//  auto_l_pvox_quad = An empty list, that we will fill with allocated and built partial voxels for (b, alpha, c, p) quadruples.

		public void build_pmom_list (SimpleExecTimer exec_timer, List<PartialVoxBAlpha> the_l_pvox_b_alpha,
			List<PartialVoxCP> the_l_pvox_c_p, AutoCloseList<PartialVoxQuad> auto_l_pvox_quad) throws OEException
		{

			// Say hello

			System.out.println ("Start caching data for (b, alpha, c, p) quadruples");

			// Create the partial voxels

			for (PartialVoxBAlpha pvox_b_alpha : the_l_pvox_b_alpha) {
				for (PartialVoxCP pvox_c_p : the_l_pvox_c_p) {
					auto_l_pvox_quad.add (new PartialVoxQuad (pvox_b_alpha, pvox_c_p));
				}
			}

			int quad_count = auto_l_pvox_quad.size();

			// Pass read-only lists to the threads

			l_pvox_b_alpha = the_l_pvox_b_alpha;
			l_pvox_c_p = the_l_pvox_c_p;
			l_pvox_quad = auto_l_pvox_quad.get_read_only_view();

			// Run the loop

			loop_helper.run_loop (this, exec_timer, 0, quad_count);

			// Capture the result

			loop_result.accum_loop (loop_helper);

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort caching data for (b, alpha, c, p) quadruples because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort caching data for (b, alpha, c, p) quadruples because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish caching data for (b, alpha, c, p) quadruples");

			return;
		}

	}




	//----- Thread managers for likelihood calculations -----




	// Build a statistics voxel.
	// Parameters:
	//  avpr = Thread-local a-value fitting handle to use.
	//  pvox_quad = Partial voxel for (b, alpha, c, p), with the pmom structure built.
	//  n_index = Index into the branch ratio list.
	// Returns a statistic voxel, with Bayesian prior and likelihoods calculated.

	private OEDisc2InitStatVox build_stat_vox (OEDisc2ExtFit.AValueProdHandle avpr, PartialVoxQuad pvox_quad, int n_index) {
		
		// Get the branch ratio value element
	
		OEValueElement n_velt = n_def.get_n_velt (n_index);

		// Create the voxel

		OEDisc2InitStatVox stat_vox = new OEDisc2InitStatVox();

		stat_vox.set_voxel_def (
			pvox_quad.get_pvox_b_alpha().get_b_velt(),
			pvox_quad.get_pvox_b_alpha().get_alpha_velt(),
			pvox_quad.get_pvox_c_p().get_c_velt(),
			pvox_quad.get_pvox_c_p().get_p_velt(),
			n_velt
		);

		// Pass in the sub-voxels

		sub_vox_def.set_subvox_def (stat_vox);

		// Apply the Bayesian prior

		stat_vox.apply_bay_prior (bay_prior, fit_info.bay_prior_params);

		// Calculate the log-likelihoods

		stat_vox.calc_likelihood (
			fit_info,
			avpr,
			pvox_quad.get_pmom()
		);

		return stat_vox;
	}




	// Likelihood calculation using one thread loop iteration per omat object, one for each (c, p) pair.

	private class TM_omat_like_calc implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (b, alpha) pairs (read-only).

		private List<PartialVoxBAlpha> l_pvox_b_alpha;

		// Total number of voxels created by the threads.

		private AtomicInteger thread_voxel_count = new AtomicInteger();

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			try (
				PartialVoxCP pvox_c_p = new PartialVoxCP();
				PartialVoxQuad pvox_quad = new PartialVoxQuad();
				OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
			) {

				// List of voxels we created

				ArrayList<OEDisc2InitStatVox> voxels = new ArrayList<OEDisc2InitStatVox>();

				// Loop until loop completed or prompt termination is requested

				for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

					// Build the Omori matrix data structures

					pvox_c_p.omat_build (fitter, c_p_def, index) ;

					// Loop over partial voxels for (b, alpha) pairs ...

					int b_alpha_count = l_pvox_b_alpha.size();

					for (int b_alpha_index = 0; b_alpha_index < b_alpha_count; ++b_alpha_index) {

						PartialVoxBAlpha pvox_b_alpha = l_pvox_b_alpha.get (b_alpha_index);

						// Build the magnitude-Omori pair data structures

						pvox_quad.pmom_build (fitter, pvox_b_alpha, pvox_c_p);

						// Loop over branch ratios ...

						final int n_count = n_def.get_combo_count();

						for (int n_index = 0; n_index < n_count; ++n_index) {

							// Create the voxel and add to the list

							voxels.add (build_stat_vox (avpr, pvox_quad, n_index));
						}
					}
				}

				// Pass the voxels to the consumer

				voxel_consumer.add_voxels (voxels);
				thread_voxel_count.addAndGet (voxels.size());
			}

			return;
		}

		// Build the list of voxels for (c, p) pairs.
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.

		public void omat_calc_like (SimpleExecTimer exec_timer) throws OEException {

			// Initialize result

			loop_result.clear();

			// Open the consumer

			voxel_consumer.begin_voxel_consume (fit_info, b_alpha_def.get_b_scaling());
			thread_voxel_count.set (0);

			try (
				AutoCloseList<PartialVoxBAlpha> auto_l_pvox_b_alpha = new AutoCloseList<PartialVoxBAlpha>();
			) {

				// Cache the list of mexp objects, one for each (b, alpha) pair

				(new TM_mexp_list_builder()).build_mexp_list (exec_timer, auto_l_pvox_b_alpha);

				// Pass read-only list to the threads

				l_pvox_b_alpha = auto_l_pvox_b_alpha.get_read_only_view();

				// Say hello

				System.out.println ("Start calculating likelihoods for (c, p) pairs");

				// The number of (c, p) pairs

				final int c_p_count = c_p_def.get_combo_count();

				// Run the loop

				loop_helper.run_loop (this, exec_timer, 0, c_p_count);

				// Capture the result

				loop_result.accum_loop (loop_helper);

			}

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort calculating likelihoods for (c, p) pairs because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort calculating likelihoods for (c, p) pairs because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish calculating likelihoods for (c, p) pairs");

			// Check the number of voxels

			final int expected_voxel_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count() * n_def.get_combo_count();
			final int got_voxel_count = thread_voxel_count.get();
			if (got_voxel_count != expected_voxel_count) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.TM_omat_like_calc.omat_calc_like: Voxel count mismatch: got " + got_voxel_count + ", expected " + expected_voxel_count);
			}

			// Close the consumer

			voxel_consumer.end_voxel_consume();

			return;
		}

	}




	// Likelihood calculation using one thread loop iteration per pmom object, one for each (b, alpha, c, p) quadruple.

	private class TM_pmom_like_calc implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (b, alpha) pairs (read-only).

		private List<PartialVoxBAlpha> l_pvox_b_alpha;

		// List of partial voxels for (c, p) pairs (read-only).

		private List<PartialVoxCP> l_pvox_c_p;

		// Total number of voxels created by the threads.

		private AtomicInteger thread_voxel_count = new AtomicInteger();

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			try (
				PartialVoxQuad pvox_quad = new PartialVoxQuad();
				OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
			) {

				// List of voxels we created

				ArrayList<OEDisc2InitStatVox> voxels = new ArrayList<OEDisc2InitStatVox>();

				// Loop until loop completed or prompt termination is requested

				for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

					// Split the index into separate (b, alpha) and (c, p) indexes

					final int b_alpha_count = l_pvox_b_alpha.size();
					final int b_alpha_index = index % b_alpha_count;
					final int c_p_index = index / b_alpha_count;

					// Get the partial voxels

					final PartialVoxCP pvox_c_p = l_pvox_c_p.get (c_p_index);
					final PartialVoxBAlpha pvox_b_alpha = l_pvox_b_alpha.get (b_alpha_index);

					// Build the magnitude-Omori pair data structures

					pvox_quad.pmom_build (fitter, pvox_b_alpha, pvox_c_p);

					// Loop over branch ratios ...

					final int n_count = n_def.get_combo_count();

					for (int n_index = 0; n_index < n_count; ++n_index) {

						// Create the voxel and add to the list

						voxels.add (build_stat_vox (avpr, pvox_quad, n_index));
					}
				}

				// Pass the voxels to the consumer

				voxel_consumer.add_voxels (voxels);
				thread_voxel_count.addAndGet (voxels.size());
			}

			return;
		}

		// Build the list of voxels for (b, alpha, c, p) quadruples.
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.

		public void pmom_calc_like (SimpleExecTimer exec_timer) throws OEException {

			// Initialize result

			loop_result.clear();

			// Open the consumer

			voxel_consumer.begin_voxel_consume (fit_info, b_alpha_def.get_b_scaling());
			thread_voxel_count.set (0);

			try (
				AutoCloseList<PartialVoxBAlpha> auto_l_pvox_b_alpha = new AutoCloseList<PartialVoxBAlpha>();
				AutoCloseList<PartialVoxCP> auto_l_pvox_c_p = new AutoCloseList<PartialVoxCP>();
			) {

				// Cache the list of mexp objects, one for each (b, alpha) pair

				(new TM_mexp_list_builder()).build_mexp_list (exec_timer, auto_l_pvox_b_alpha);

				// Pass read-only list to the threads

				l_pvox_b_alpha = auto_l_pvox_b_alpha.get_read_only_view();

				// Cache the list of omat objects, one for each (c, p) pair

				(new TM_omat_list_builder()).build_omat_list (exec_timer, auto_l_pvox_c_p);

				// Pass read-only list to the threads

				l_pvox_c_p = auto_l_pvox_c_p.get_read_only_view();

				// Say hello

				System.out.println ("Start calculating likelihoods for (b, alpha, c, p) quadruples");

				// The number of (b, alpha, c, p) quadruples

				final int quad_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count();

				// Run the loop

				loop_helper.run_loop (this, exec_timer, 0, quad_count);

				// Capture the result

				loop_result.accum_loop (loop_helper);

			}

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort calculating likelihoods for (b, alpha, c, p) quadruples because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort calculating likelihoods for (b, alpha, c, p) quadruples because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish calculating likelihoods for (b, alpha, c, p) quadruples");

			// Check the number of voxels

			final int expected_voxel_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count() * n_def.get_combo_count();
			final int got_voxel_count = thread_voxel_count.get();
			if (got_voxel_count != expected_voxel_count) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.TM_pmom_like_calc.pmom_calc_like: Voxel count mismatch: got " + got_voxel_count + ", expected " + expected_voxel_count);
			}

			// Close the consumer

			voxel_consumer.end_voxel_consume();

			return;
		}

	}




	// Likelihood calculation using one thread loop iteration per pmom object, one for each (b, alpha, c, p, n) quintuple.

	private class TM_avpr_like_calc implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// List of partial voxels for (b, alpha, c, p) quadruples (read-only).

		private List<PartialVoxQuad> l_pvox_quad;

		// Total number of voxels created by the threads.

		private AtomicInteger thread_voxel_count = new AtomicInteger();

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			try (
				OEDisc2ExtFit.AValueProdHandle avpr = fitter.make_AValueProdHandle();
			) {

				// List of voxels we created

				ArrayList<OEDisc2InitStatVox> voxels = new ArrayList<OEDisc2InitStatVox>();

				// Loop until loop completed or prompt termination is requested

				for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

					// Split the index into separate (b, alpha, c, p) and n indexes

					final int n_count = n_def.get_combo_count();
					final int n_index = index % n_count;
					final int quad_index = index / n_count;

					// Get the partial voxel and value element

					PartialVoxQuad pvox_quad = l_pvox_quad.get (quad_index);

					// Create the voxel and add to the list

					voxels.add (build_stat_vox (avpr, pvox_quad, n_index));
				}

				// Pass the voxels to the consumer

				voxel_consumer.add_voxels (voxels);
				thread_voxel_count.addAndGet (voxels.size());
			}

			return;
		}

		// Build the list of voxels for (b, alpha, c, p, n) quintuples.
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.

		public void avpr_calc_like (SimpleExecTimer exec_timer) throws OEException {

			// Initialize result

			loop_result.clear();

			// Open the consumer

			voxel_consumer.begin_voxel_consume (fit_info, b_alpha_def.get_b_scaling());
			thread_voxel_count.set (0);

			try (
				AutoCloseList<PartialVoxBAlpha> auto_l_pvox_b_alpha = new AutoCloseList<PartialVoxBAlpha>();
				AutoCloseList<PartialVoxCP> auto_l_pvox_c_p = new AutoCloseList<PartialVoxCP>();
				AutoCloseList<PartialVoxQuad> auto_l_pvox_quad = new AutoCloseList<PartialVoxQuad>();
			) {

				// Cache the list of mexp objects, one for each (b, alpha) pair

				(new TM_mexp_list_builder()).build_mexp_list (exec_timer, auto_l_pvox_b_alpha);

				// Read-only list

				final List<PartialVoxBAlpha> l_pvox_b_alpha = auto_l_pvox_b_alpha.get_read_only_view();

				// Cache the list of omat objects, one for each (c, p) pair

				(new TM_omat_list_builder()).build_omat_list (exec_timer, auto_l_pvox_c_p);

				// Read-only list

				final List<PartialVoxCP> l_pvox_c_p = auto_l_pvox_c_p.get_read_only_view();

				// Cache the list of pmom objects, one for each (b, alpha, c, p) quadruple

				(new TM_pmom_list_builder()).build_pmom_list (exec_timer, l_pvox_b_alpha, l_pvox_c_p, auto_l_pvox_quad);

				// Pass read-only list to the threads

				l_pvox_quad = auto_l_pvox_quad.get_read_only_view();

				// Say hello

				System.out.println ("Start calculating likelihoods for (b, alpha, c, p, n) quintuples");

				// The number of (b, alpha, c, p, n) quintuples

				final int quint_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count() * n_def.get_combo_count();

				// Run the loop

				loop_helper.run_loop (this, exec_timer, 0, quint_count);

				// Capture the result

				loop_result.accum_loop (loop_helper);

			}

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort calculating likelihoods for (b, alpha, c, p, n) quintuples because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort calculating likelihoods for (b, alpha, c, p, n) quintuples because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));
			System.out.println ("Finish calculating likelihoods for (b, alpha, c, p, n) quintuples");

			// Check the number of voxels

			final int expected_voxel_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count() * n_def.get_combo_count();
			final int got_voxel_count = thread_voxel_count.get();
			if (got_voxel_count != expected_voxel_count) {
				throw new InvariantViolationException ("OEDisc2InitVoxBuilder.TM_avpr_like_calc.avpr_calc_like: Voxel count mismatch: got " + got_voxel_count + ", expected " + expected_voxel_count);
			}

			// Close the consumer

			voxel_consumer.end_voxel_consume();

			return;
		}

	}




	// Build the list of voxels.
	// Parameters:
	//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.
	// Before calling, must set up the shared objects and the grid parameters.
	// Implementation note: Selects one of the three threading strategies above,
	// depending on the structure of the grid.

	public void build_voxels (SimpleExecTimer exec_timer) throws OEException {

		// Get the number of threads, or 0 if no fixed limit

		final int num_threads = exec_timer.get_executor().get_num_threads();

		// Say hello

		System.out.println ();
		System.out.println ("Fitting ETAS parameters");
		System.out.println ();
		final long max_runtime = exec_timer.get_remaining_time();
		System.out.println (
			"Fitting "
			+ (b_alpha_def.get_combo_count() * c_p_def.get_combo_count() * n_def.get_combo_count() * sub_vox_def.get_subvox_count())
			+ " parameter combinations, using "
			+ num_threads
			+ " threads, with "
			+ ((max_runtime < 0L) ? ("unlimited runtime") : (((max_runtime + 500L) / 1000L) + " seconds maximum runtime"))
		);

		// The number of (c, p) pairs

		final int c_p_count = c_p_def.get_combo_count();

		// The number of (b, alpha, c, p) quadruples

		final int quad_count = b_alpha_def.get_combo_count() * c_p_def.get_combo_count();

		// If there are enough (c, p) pairs to occupy all threads, use thread loop iteration per (c, p) pair
		// (This also handles the case of no fixed limit on number of threads)

		if (num_threads <= c_p_count) {
			(new TM_omat_like_calc()).omat_calc_like (exec_timer);
		}

		// Otherwise, if there are enough (b, alpha, c, p) quadruples to occupy all threads, use thread loop iteration per (b, alpha, c, p) quadruple

		else if (num_threads <= quad_count) {
			(new TM_pmom_like_calc()).pmom_calc_like (exec_timer);
		}

		// Otherwise, use thread loop iteration per (b, alpha, c, p, n) quintuple

		else {
			(new TM_avpr_like_calc()).avpr_calc_like (exec_timer);
		}

		return;
	}




	//----- Construction -----




	// Erase the shared data.

	private final void clear_shared () {
		voxel_consumer = null;
		fitter = null;
		fit_info = null;
		bay_prior = null;
		return;
	}




	// Erse the grid parameters.

	public final void clear_grid () {
		b_alpha_def = null;
		c_p_def = null;
		n_def = null;
		sub_vox_def = null;
		return;
	}




	// Erase the contents.

	public final void clear () {
		clear_shared();
		clear_grid();
		return;
	}




	// Default constructor.

	public OEDisc2InitVoxBuilder () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2InitVoxBuilder:" + "\n");

		if (fitter != null) {
			result.append ("fitter = {" + fitter.toString() + "}\n");
		}
		if (fit_info != null) {
			result.append ("fit_info = {" + fit_info.toString() + "}\n");
		}
		if (bay_prior != null) {
			result.append ("bay_prior = {" + bay_prior.toString() + "}\n");
		}

		return result.toString();
	}




	//----- Testing -----




	// Get the fitting info.
	// This function is for testing.

	public final OEDisc2InitFitInfo test_get_fit_info () {
		return fit_info;
	}




	// Get the combinations of (b, alpha) parameters.
	// This function is for testing.

	public final OEDisc2InitDefVoxBAlpha test_get_b_alpha_def () {
		return b_alpha_def;
	}




	// Get the combinations of (c, p) parameters.
	// This function is for testing.

	public final OEDisc2InitDefVoxCP test_get_c_p_def () {
		return c_p_def;
	}




	// Get the combinations of (n) parameters.
	// This function is for testing.

	public final OEDisc2InitDefVoxN test_get_n_def () {
		return n_def;
	}




	// Get the combinations of (zams, zmu) parameters.
	// This function is for testing.

	public final OEDisc2InitSubVoxDef test_get_sub_vox_def () {
		return sub_vox_def;
	}




	// If (b, alpha) combinations were constructed from separate ranges, these functions dump the ranges.

	public final String test_dump_sep_b_velt() {
		return b_alpha_def.dump_sep_b_velt();
	}

	public final String test_dump_sep_alpha_velt() {
		return b_alpha_def.dump_sep_alpha_velt();
	}




	// If (c, p) combinations were constructed from separate ranges, these functions dump the ranges.

	public final String test_dump_sep_c_velt() {
		return c_p_def.dump_sep_c_velt();
	}

	public final String test_dump_sep_p_velt() {
		return c_p_def.dump_sep_p_velt();
	}




	// If (n) combinations were constructed from separate ranges, these functions dump the ranges.

	public final String test_dump_sep_n_velt() {
		return n_def.dump_sep_n_velt();
	}




	// If (zams, zmu) combinations were constructed from separate ranges, these functions dump the ranges.

	public final String test_dump_sep_zams_velt() {
		return sub_vox_def.dump_sep_zams_velt();
	}

	public final String test_dump_sep_zmu_velt() {
		return sub_vox_def.dump_sep_zmu_velt();
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDisc2InitVoxBuilder : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEDisc2InitVoxBuilder : Unrecognized subcommand : " + args[0]);
		return;

	}

}
