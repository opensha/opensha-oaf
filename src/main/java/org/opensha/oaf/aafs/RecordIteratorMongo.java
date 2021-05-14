package org.opensha.oaf.aafs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

/**
 * Iterator for iterating through records in a table (collection), using MongoDB Java driver.
 * Author: Michael Barall 04/09/2018.
 *
 * This is used to iterate through database entries, when it may not be possible
 * to bring all the entries into memory as a List<T>.
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
public abstract class RecordIteratorMongo<T> implements RecordIterator<T> {

	//----- Opaque contents -----

	// The MongoDB cursor, or null if this iterator has been closed.

	private MongoCursor<Document> mongo_cursor;

	// The MongoDB collection handle.

	private MongoDBCollHandle coll_handle;

	// Constructor saves the MongoDB cursor and collection handle, and registers the iterator.

	public RecordIteratorMongo (MongoCursor<Document> mongo_cursor, MongoDBCollHandle coll_handle) {
		this.mongo_cursor = mongo_cursor;
		this.coll_handle = coll_handle;

		// Register so we are auto-closed when the database is closed

		try {
			coll_handle.add_resource (this);
		} catch (DBDriverException e) {
			close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordIteratorMongo.RecordIteratorMongo: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordIteratorMongo.RecordIteratorMongo: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			close_mongo_cursor_nx();
			throw new DBException ("RecordIteratorMongo.RecordIteratorMongo: Exception during setup: " + make_cursor_id_message(), e);
		}
	}

	// Get the MongoDB cursor.

	public MongoCursor<Document> get_mongo_cursor () {
		return mongo_cursor;
	}

	// Get the MongoDB collection handle.

	public MongoDBCollHandle get_coll_handle () {
		return coll_handle;
	}

	// Close the MongoDB cursor, catching all exceptions.

	private void close_mongo_cursor_nx () {
		MongoCursor<Document> my_mongo_cursor = mongo_cursor;
		mongo_cursor = null;
		if (my_mongo_cursor != null) {
			try {
				my_mongo_cursor.close();
			} catch (Exception e) {
			}
		}
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

	// Hook routine to convert a Document to a T.

	protected abstract T hook_convert (Document doc);

	//----- Implementation of Iterator<T> -----

	// Returns true if the iteration has more elements.
	// (In other words, returns true if next() would return an element rather than throwing an exception.)

	@Override
	public boolean hasNext() {
		boolean result;

		if (mongo_cursor == null) {
			throw new DBClosedIteratorException ("RecordIteratorMongo.hasNext: Interator has been closed: " + make_cursor_id_message());
		}

		try {
			result = mongo_cursor.hasNext();
		} catch (DBDriverException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordIteratorMongo.hasNext: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordIteratorMongo.hasNext: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			//close_mongo_cursor_nx();
			throw new DBException ("RecordIteratorMongo.hasNext: Exception during iteration: " + make_cursor_id_message(), e);
		}

		return result;
	}

	// Returns the next element in the iteration.
	// Throws NoSuchElementException if the iteration has no more elements.

	@Override
	public T next() {
		T result;

		if (mongo_cursor == null) {
			throw new DBClosedIteratorException ("RecordIteratorMongo.next: Interator has been closed: " + make_cursor_id_message());
		}

		try {
			result = hook_convert (mongo_cursor.next());
		} catch (DBDriverException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordIteratorMongo.next: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordIteratorMongo.next: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			//close_mongo_cursor_nx();
			throw new DBException ("RecordIteratorMongo.next: Exception during iteration: " + make_cursor_id_message(), e);
		}

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
		throw new UnsupportedOperationException ("RecordIteratorMongo does not support remove");
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

		// Close the cursor, if it is open

		MongoCursor<Document> my_mongo_cursor = mongo_cursor;
		mongo_cursor = null;
		if (my_mongo_cursor != null) {
			try {
				my_mongo_cursor.close();
			} catch (DBDriverException e) {
				throw new DBDriverException (e.get_locus(), "RecordIteratorMongo.close: Driver exception: " + make_cursor_id_message(), e);
			} catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "RecordIteratorMongo.close: MongoDB exception: " + make_cursor_id_message(), e);
			} catch (Exception e) {
				throw new DBException ("RecordIteratorMongo.close: Exception during iteration: " + make_cursor_id_message(), e);
			}
		}

		return;
	}

}
