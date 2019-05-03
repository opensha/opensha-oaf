package org.opensha.oaf.aafs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.Document;

/**
 * Iterator for iterating through changed records in a table (collection), using MongoDB Java driver.
 * Author: Michael Barall 05/02/2018.
 *
 * This is used to iterate through database entries that are created, replaced, or updated.
 * This makes it possible for a program to monitor changes in a collection (excepting delete).
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
 * Usage notes: Because this iterator monitors database changes, hasNext() returns true
 * if a document is immediately available.  Unlike a normal iterator, if hasNext()
 * returns false then a later call to hasNext() may return true.  This class is desgined
 * so that if hasNext() returns true, it will continue to return true until the
 * document is retrieved via next().
 *
 * Implementation notes: The hasNext() function works by calling tryNext() on the Mongo iterator,
 * which is the recommended way of accessing a MongoDB change stream without blocking.
 * If tryNext() obtains a document, the document is cached so it can be returned on a
 * subsequent call to next().  Generally, the call to next() should come right after the
 * call to hasNext().
 *
 * Only code very close to the database engine should create these objects or
 * access their contents.  All other code should treat these objects as opaque.
 */
public abstract class RecordChangeIteratorMongo<T> implements RecordIterator<T> {

	//----- Opaque contents -----

	// The MongoDB cursor, or null if this iterator has been closed.

	private MongoCursor<ChangeStreamDocument<Document>> mongo_cursor;

	// The MongoDB collection handle.

	private MongoDBCollHandle coll_handle;

	// The cached document from the successful call to next(), or null if none.

	private ChangeStreamDocument<Document> cached_doc; 

	// Constructor saves the MongoDB cursor and collection handle, and registers the iterator.

	public RecordChangeIteratorMongo (MongoCursor<ChangeStreamDocument<Document>> mongo_cursor, MongoDBCollHandle coll_handle) {
		this.mongo_cursor = mongo_cursor;
		this.coll_handle = coll_handle;
		this.cached_doc = null;

		// Register so we are auto-closed when the database is closed

		try {
			coll_handle.add_resource (this);
		} catch (DBDriverException e) {
			close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordChangeIteratorMongo.RecordChangeIteratorMongo: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordChangeIteratorMongo.RecordChangeIteratorMongo: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			close_mongo_cursor_nx();
			throw new DBException ("RecordChangeIteratorMongo.RecordChangeIteratorMongo: Exception during setup: " + make_cursor_id_message(), e);
		}
	}

	// Get the MongoDB cursor.

	public MongoCursor<ChangeStreamDocument<Document>> get_mongo_cursor () {
		return mongo_cursor;
	}

	// Get the MongoDB collection handle.

	public MongoDBCollHandle get_coll_handle () {
		return coll_handle;
	}

	// Close the MongoDB cursor, catching all exceptions.

	private void close_mongo_cursor_nx () {
		MongoCursor<ChangeStreamDocument<Document>> my_mongo_cursor = mongo_cursor;
		mongo_cursor = null;
		cached_doc = null;
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
		boolean result = false;

		if (mongo_cursor == null) {
			throw new DBClosedIteratorException ("RecordChangeIteratorMongo.hasNext: Interator has been closed: " + make_cursor_id_message());
		}

		try {
			if (cached_doc != null) {
				result = true;
			} else {
				for (;;) {
					ChangeStreamDocument<Document> doc = mongo_cursor.tryNext();
					if (doc == null) {
						break;
					}

					switch (doc.getOperationType()) {

						// Accept document if it's insert, replace, or update

						case INSERT:
						case REPLACE:
						case UPDATE:
							cached_doc = doc;
							result = true;
							break;

						// If delete or other, ignore the document

						case DELETE:
						case OTHER:
							continue;

						// Any other event is a fatal error

						default:
							throw new DBClosedIteratorException ("RecordChangeIteratorMongo.hasNext: Interator has invalid operation type: " + doc.getOperationType().toString() + ": " + make_cursor_id_message());
					}
					break;
				}
			}
		} catch (DBDriverException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordChangeIteratorMongo.hasNext: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordChangeIteratorMongo.hasNext: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			//close_mongo_cursor_nx();
			throw new DBException ("RecordChangeIteratorMongo.hasNext: Exception during iteration: " + make_cursor_id_message(), e);
		}

		return result;
	}

	// Returns the next element in the iteration.
	// Throws NoSuchElementException if the iteration has no more elements.

	@Override
	public T next() {
		T result;

		if (mongo_cursor == null) {
			throw new DBClosedIteratorException ("RecordChangeIteratorMongo.next: Interator has been closed: " + make_cursor_id_message());
		}

		if (cached_doc == null) {
			throw new NoSuchElementException ("RecordChangeIteratorMongo.next: Interator has not been queried: " + make_cursor_id_message());
		}

		try {
			ChangeStreamDocument<Document> doc = cached_doc;
			cached_doc = null;
			result = hook_convert (doc.getFullDocument());
		} catch (DBDriverException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (e.get_locus(), "RecordChangeIteratorMongo.next: Driver exception: " + make_cursor_id_message(), e);
		} catch (MongoException e) {
			//close_mongo_cursor_nx();
			throw new DBDriverException (make_locus(e), "RecordChangeIteratorMongo.next: MongoDB exception: " + make_cursor_id_message(), e);
		} catch (Exception e) {
			//close_mongo_cursor_nx();
			throw new DBException ("RecordChangeIteratorMongo.next: Exception during iteration: " + make_cursor_id_message(), e);
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
		throw new UnsupportedOperationException ("RecordChangeIteratorMongo does not support remove");
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

		MongoCursor<ChangeStreamDocument<Document>> my_mongo_cursor = mongo_cursor;
		mongo_cursor = null;
		cached_doc = null;
		if (my_mongo_cursor != null) {
			try {
				my_mongo_cursor.close();
			} catch (DBDriverException e) {
				throw new DBDriverException (e.get_locus(), "RecordChangeIteratorMongo.close: Driver exception: " + make_cursor_id_message(), e);
			} catch (MongoException e) {
				throw new DBDriverException (make_locus(e), "RecordChangeIteratorMongo.close: MongoDB exception: " + make_cursor_id_message(), e);
			} catch (Exception e) {
				throw new DBException ("RecordChangeIteratorMongo.close: Exception during iteration: " + make_cursor_id_message(), e);
			}
		}

		return;
	}

}
