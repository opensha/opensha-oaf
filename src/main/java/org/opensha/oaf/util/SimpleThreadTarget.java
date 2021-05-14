package org.opensha.oaf.util;


// Interface that is a target for threads managed by SimpleThreadManager.
// Author: Michael Barall 11/06/2020.
//
// A single object of this type is called by all the threads in the pool.

public interface SimpleThreadTarget {

	// Entry point for a thread.
	// Parameters:
	//  thread_manager = The thread manager.
	//  thread_number = The thread number, which ranges from 0 to the number of
	//                  threads in the pool minus 1.
	// Note: Threads may (or may not) be re-used, so any thread-local variables
	// may be uninitialized or may contain values left over from previous usage.
	// Note: This function can call thread_manager.get_num_threads() to get the
	// number of threads in the pool.
	// Note: Throwing an exception from this function causes an abort message
	// to be written (as if by thread_manager.add_abort_message()), and sets
	// the flag to request that other threads terminate promptly (as if by
	// thread_manager.request_termination()).
	// Note: It is suggested that this function periodically poll
	// thread_manager.get_req_termination() to check if prompt termination
	// has been requested.
	// Threading: This function is called by all the threads in the pool, and
	// so must be thread-safe and use any needed synchronization.

	public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception;

}
