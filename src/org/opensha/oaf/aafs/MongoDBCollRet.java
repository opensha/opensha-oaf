package org.opensha.oaf.aafs;

import com.mongodb.client.MongoCollection;
import org.bson.Document;


/**
 * This class is used to return a MongoDB collection.
 * Author: Michael Barall 11/11/2018.
 */
public class MongoDBCollRet {

	// The collection.

	public MongoCollection<Document> collection;

	// Flag is true if the collection was newly retrieved from MongoDB, false if it was retrieved from cache.

	public boolean f_new;




	// Constructor.
	
	public MongoDBCollRet (MongoCollection<Document> collection, boolean f_new) {
		this.collection = collection;
		this.f_new = f_new;
	}

}
