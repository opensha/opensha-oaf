package org.opensha.oaf.aafs;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;

import com.mongodb.MongoException;

/**
 * Iterator for iterating through records in a table (collection), using MongoDB Java driver.
 * Author: Michael Barall 05/08/2018.
 *
 * This is used to iterate through database entries, when the records already exist
 * in memory as a List<T>.  (Although records are read from the list, this iterator
 * nontheless registers itself with a MongoDB collection.)
 *
 * It implements Iterator<T> so that it is possible to step through the database
 * entries with standard iterator operations hasNext() and next().  The remove()
 * operation is unsupported.
 *
 * It implements Iterable<T> so that it can be used in the foreach statement.
 *
 * It implements AuotCloseable so that it can be created in a try-with-resources
 * statement, which is recommended to ensure the MongoDB iterator is closed.
 *
 * Only code very close to the database engine should create these objects or
 * access their contents.  All other code should treat these objects as opaque.
 */
public class RecordListIteratorMongo<T> implements RecordIterator<T> {

	//----- Opaque contents -----

	// The record list, or null if this iterator has been closed.

	private List<T> record_list;

	// The MongoDB collection handle.

	private MongoDBCollHandle coll_handle;

	// The current index into the record list.

	private int record_index;

	// Constructor saves the record list and collection handle, and registers the iterator.

	public RecordListIteratorMongo (List<T> record_list, MongoDBCollHandle coll_handle) {
		this.record_list = record_list;
		this.coll_handle = coll_handle;
		this.record_index = 0;

		// Register so we are auto-closed when the database is closed

		try {
			coll_handle.add_resource (this);
		} catch (DBDriverException e) {
			close_record_list_nx();
			throw new DBDriverException (e.get_locus(), "RecordListIteratorMongo.RecordListIteratorMongo: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			close_record_list_nx();
			throw new DBDriverException (make_locus(e), "RecordListIteratorMongo.RecordListIteratorMongo: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			close_record_list_nx();
			throw new DBException ("RecordListIteratorMongo.RecordListIteratorMongo: Exception during setup: " + make_cursor_id_message(), e);
		}
	}

	// Get the record list.

	public List<T> get_record_list () {
		return record_list;
	}

	// Get the MongoDB collection handle.

	public MongoDBCollHandle get_coll_handle () {
		return coll_handle;
	}

	// Get the record index.

	public int get_record_index () {
		return record_index;
	}

	// Restart the iterator from the beginning of the list.

	public void restart () {
		record_index = 0;
		return;
	}

	// Close the record list, catching all exceptions.

	private void close_record_list_nx () {
		record_list = null;
		return;
	}

	// Make a string that identifies the collection, for use in exception messages.

	private String make_cursor_id_message () {
		return "coll_name = " + coll_handle.get_coll_name()
			+ ", db_name = " + coll_handle.get_db_name()
			+ ", db_handle = " + coll_handle.get_db_handle();
	}

	// Make the error locus for the collection, for use in exceptions.

	private MongoDBErrorLocus make_locus (MongoException mongo_exception) {
		return new MongoDBErrorLocus (
			coll_handle.get_host_handle(),
			coll_handle.get_db_handle(),
			coll_handle.get_coll_name(),
			mongo_exception);
	}

	//----- Implementation of Iterator<T> -----

	// Returns true if the iteration has more elements.
	// (In other words, returns true if next() would return an element rather than throwing an exception.)

	@Override
	public boolean hasNext() {
		boolean result = true;

		if (record_list == null) {
			throw new DBClosedIteratorException ("RecordListIteratorMongo.hasNext: Iterator has been closed: " + make_cursor_id_message());
		}

		if (record_index >= record_list.size()) {
			result = false;
		}

		return result;
	}

	// Returns the next element in the iteration.
	// Throws NoSuchElementException if the iteration has no more elements.

	@Override
	public T next() {
		T result;

		if (record_list == null) {
			throw new DBClosedIteratorException ("RecordListIteratorMongo.next: Iterator has been closed: " + make_cursor_id_message());
		}

		if (record_index >= record_list.size()) {
			throw new NoSuchElementException ("RecordListIteratorMongo.next: Iterator has been exhausted: " + make_cursor_id_message());
		}

		result = record_list.get (record_index);
		++record_index;

		return result;
	}

	// Removes from the underlying collection the last element returned by this iterator (optional operation).
	// This method can be called only once per call to next().
	// The behavior of an iterator is unspecified if the underlying collection is modified while the iteration
	// is in progress in any way other than by calling this method.
	// Throws UnsupportedOperationException if the remove operation is not supported by this iterator.
	// Throws IllegalStateException if the next method has not yet been called, or the remove method
	// has already been called after the last call to the next method

	@Override
	public void remove() {
		throw new UnsupportedOperationException ("RecordListIteratorMongo does not support remove");
	}

	//----- Implementation of Iterable<T> -----

	// Returns an iterator over a set of elements of type T.

	@Override
	public Iterator<T> iterator() {
		return this;
	}

	//----- Implementation of AutoCloseable -----

	// Closes this resource, relinquishing any underlying resources.
	// This method is invoked automatically on objects managed by the try-with-resources statement. 

	@Override
	public void close() {

		// Unregister, note this cannot throw an exception

		coll_handle.remove_resource (this);

		// Close the record list, if it is open

		record_list = null;
		return;
	}

}
