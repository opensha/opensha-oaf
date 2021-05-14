package org.opensha.oaf.aafs;

// Imports for Java logger (JUL)

import java.util.logging.Logger;
import java.util.logging.Level;

// Imports for SLF4J

//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.LoggerContext;
//import org.slf4j.LoggerFactory;



/**
 * This class is used to control MongoDB logging.
 * Author: Michael Barall 08/13/2019.
 */
public class MongoDBLogControl {


	// Disable excessive MongoDB log messages (JUL version).

	public static void disable_excessive () {
	
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.SEVERE);	 // or Log.WARNING,...
	
		return;
	}


	// Disable excessive MongoDB log messages (SLF4J version).

	//  public static void disable_excessive () {
	//  
	//  	LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	//  	Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
	//  	rootLogger.setLevel(Level.ERROR);		// or Level.OFF,...
	//  
	//  	return;
	//  }

}
