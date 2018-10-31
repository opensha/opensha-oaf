package scratch.aftershockStatistics.aafs;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for iterating through records in a table (collection).
 * Author: Michael Barall 04/09/2018.
 *
 * This is used to iterate through database entries, when it may not be possible
 * to bring all the entries into memory as a List<T>.
 *
 * It implements Iterator<T> so that it is possible to step through the database
 * entries with standard iterator operations hasNext() and next().  The remove()
 * operation is passed thru to the database but I'm not sure if it works.
 *
 * It implements Iterable<T> so that it can be used in the foreach statement.
 *
 * It implements AuotCloseable so that it can be created in a try-with-resources
 * statement, which is recommended to ensure the database iterator is closed.
 *
 * Only code very close to the database engine should create these objects or
 * access their contents.  All other code should treat these objects as opaque.
 */
public interface RecordIterator<T> extends Iterator<T>, Iterable<T>, AutoCloseable {

	//----- Implementation of AutoCloseable -----

	// Closes this resource, relinquishing any underlying resources.
	// This method is invoked automatically on objects managed by the try-with-resources statement. 

	@Override
	public void close();

}
