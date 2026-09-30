
/*
 * This probably can be replaced with just a ParameterList Object extension
 */
package org.opensha.oaf.etas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ETAS_Configuration {

    StringBuilder parameterList = new StringBuilder();
    boolean listStarted = false;
    
    File configFile;

    // store parameters for lookup
    Map<String, String> params = new HashMap<>();

    public ETAS_Configuration() {
    }

    public ETAS_Configuration(File file) {
    	this.configFile = file;
    }

    public void addParameter(String name, int value) {
    	addParameter(name, Integer.toString(value));
    }

    public void addParameter(String name, long value) {
    	addParameter(name, Long.toString(value));
    }

    public void addParameter(String name, double value) {
    	addParameter(name, Double.toString(value));
    }

    public void addParameter(String name, float value) {
    	addParameter(name, Float.toString(value));
    }

    public void addParameter(String name, boolean value) {
    	addParameter(name, Boolean.toString(value));
    }

    public void addParameter(String name, char value) {
    	addParameter(name, Character.toString(value));
    }

    public void addParameter(String name, Enum<?> value) {
    	addParameter(name, value.name());
    }


    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder("{\n");
    	int count = 0;
    	int size = params.size();

    	for (Map.Entry<String, String> e : params.entrySet()) {
    		sb.append("  \"").append(e.getKey()).append("\": \"")
    		.append(e.getValue()).append("\"");
    		if (++count < size) sb.append(",");
    		sb.append("\n");
    	}

    	sb.append("}");
    	return sb.toString();
    }

    
    public void addParameter(String name, String value) {
        /**
         * add a parameter to the list
         */
        if (listStarted)
            parameterList.append(",\n{" + name + "," + value + "}");
        else {
            parameterList.append("{" + name + "," + value + "}");
            listStarted = true;
        }
        params.put(name, value);
    }

    public boolean writeConfigurationFile(File outFile) {
        /**
         * writes a text list of the parameters (output of writeConfigurationFile)
         */
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(parameterList.toString());
            return true;   // success
        } catch (IOException e) {
            e.printStackTrace();
            return false;  // failure
        }
    }

    public boolean writeConfigurationFile() {
        if(configFile != null)
        	return writeConfigurationFile(configFile); 
        else {
        	System.err.println("Error: No default file specified.");
        	return false;
        }
    }

    public boolean readConfigurationFile(File inFile) {
        /**
         * reads a text list of the parameters (output of writeConfigurationFile())
         *
         * expected format:
         * {name,value},
         * {name2,value2},
         * ...
         */
        try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {

            params.clear();
            parameterList.setLength(0);
            listStarted = false;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // entries look like {name,value} possibly with trailing commas
                if (!line.startsWith("{") || !line.contains(",")) continue;

                line = line.replace(",", "");  // remove comma separators
                line = line.replace("{", "").replace("}", "");

                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                String name = parts[0].trim();
                String value = parts[1].trim();

                addParameter(name, value);
            }

            return true;   // success

        } catch (IOException e) {
            e.printStackTrace();
            return false;  // failure
        }
    }

    public String getParameter(String parameterName) {
        /**
         * finds the parameter that matches the parameterName,
         * and returns the corresponding value as a string
         */
        return params.get(parameterName);
    }
    
    // --- Integer types ---
    public int getIntParameter(String name) {
    	String v = params.get(name);
    	return (v == null ? 0 : Integer.parseInt(v));
    }

    public long getLongParameter(String name) {
    	String v = params.get(name);
    	return (v == null ? 0L : Long.parseLong(v));
    }

    // --- Floating point types ---
    public double getDoubleParameter(String name) {
    	String v = params.get(name);
    	return (v == null ? 0.0 : Double.parseDouble(v));
    }

    public float getFloatParameter(String name) {
    	String v = params.get(name);
    	return (v == null ? 0f : Float.parseFloat(v));
    }

    // --- Boolean ---
    public boolean getBooleanParameter(String name) {
    	String v = params.get(name);
    	return (v == null ? false : Boolean.parseBoolean(v));
    }

    // --- Character ---
    public char getCharParameter(String name) {
    	String v = params.get(name);
    	return (v == null || v.isEmpty() ? '\0' : v.charAt(0));
    }

    // --- Generic object/string passthrough ---
    public Object getObjectParameter(String name) {
    	return params.get(name);   // returns String or null
    }

}