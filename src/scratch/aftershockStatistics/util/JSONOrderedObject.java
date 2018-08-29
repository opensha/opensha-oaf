package scratch.aftershockStatistics.util;

import java.io.IOException;
import java.io.Writer;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;
import org.json.simple.ItemList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.Yytoken;

/**
 * Version of JSONObject that preserves the order of fields.
 * Author: Michael Barall 08/28/2018.
 *
 * Note: A JSON object by definition is an *unordered* collection of keys and values.
 * It is not legitimate for the meaning of a JSON file to depend on the order in
 * which fields appear, nor is it legitimate to require that fields in a JSON file
 * appear in a prescribed order.  The use for a class like this is to make it easier
 * for humans to read the JSON file.
 */
public class JSONOrderedObject extends LinkedHashMap implements Map, JSONAware, JSONStreamAware {

	private static final long serialVersionUID = -138469757310745446L;


	// Construct an empty object.

	public JSONOrderedObject () {
		super();
	}


	// Construct an object, and fill it with all the elements of the supplied map.

	public JSONOrderedObject (Map map) {
		super(map);
	}


	// Write this object to a file (implementation of JSONStreamAware).

	@Override
	public void writeJSONString (Writer out) throws IOException {
		JSONObject.writeJSONString (this, out);
		return;
	}


	// Write this object into a string (implementation of JSONAware).
	
	@Override
	public String toJSONString () {
		return JSONObject.toJSONString (this);
	}


	// Convert this object into a string.
	
	@Override
	public String toString(){
		return toJSONString();
	}


	// Parse JSON text from the given input and return a Java object.
	// The return value is one of the following:
	//  scratch.aftershockStatistics.util.JSONOrderedObject
	//  org.json.simple.JSONArray
	//  java.lang.String
	//  java.lang.Number
	//  java.lang.Boolean
	//  null
	// JSON objects at any level are represented by JSONOrderedObject
	// and so preserve the ordering in the input source.

	public static Object parseWithException (Reader in) throws IOException, ParseException {
		JSONParser parser = new JSONParser();

		ContainerFactory containerFactory = new ContainerFactory() {
			@Override
			public Map createObjectContainer() {
				return new JSONOrderedObject();
			}

			@Override
			public List creatArrayContainer() {
				return null;
			}
		};

		return parser.parse (in, containerFactory);
	}
	
	public static Object parseWithException (String s) throws ParseException {
		JSONParser parser = new JSONParser();

		ContainerFactory containerFactory = new ContainerFactory() {
			@Override
			public Map createObjectContainer() {
				return new JSONOrderedObject();
			}

			@Override
			public List creatArrayContainer() {
				return null;
			}
		};

		return parser.parse (s, containerFactory);
	}

}
