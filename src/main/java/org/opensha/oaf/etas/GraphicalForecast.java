package org.opensha.oaf.etas;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.TimeZone;

import javax.swing.JTable;

import org.apache.commons.io.FileUtils;
import org.opensha.commons.param.Parameter;
import org.opensha.oaf.util.SphRegion;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

// template SVG file

public class GraphicalForecast{

	private static boolean D = true;
	
	// initialize with dummy values for prototyping
	private String location = "13 km SE of Scenariotown";
	private double lat0 = 18;
	private double lon0 = -67;
	private double mag0 = 6.4;
	
	private double mapRadiusDeg = 0.5;
	
	private double aftershockRadiusKM;
	
	private SphRegion region;
	
	private double depth0 = 9.0;
	private double tPredStart = 1.0;
	private GregorianCalendar forecastStartDate = new GregorianCalendar();
	private GregorianCalendar eventDate = new GregorianCalendar();
	private String msName = "MS_NAME";
	private String versionNumber = "";
	private GregorianCalendar versionDate = new GregorianCalendar();
	private ETAS_AftershockModel aftershockModel;
	private File outFile;
	private double b;
	private String shakeMapURL = null;
	private String eventURL = "#";
	
	private final static int DAY = 1;
	private final static int WEEK = 7;
	private final static int MONTH = 31;
	private final static int YEAR = 366;
			
	//	parameters for word wrap algorithm and text summary
	private boolean smartRoundPercentages = true;   //give rounded range of probabilities?
	private double feltMag = 3;  //% magnitude to use for calculating number of 'felt' earthquakes
	private double headlineMag = 5.0;   // magnitude to use for headline magnitude (one number forecast)
	private double damageMag = 6.0;   // magnitude to use for damaging earthquake
	private double pHeadline_display = 0;
	private double pDamage_display = 0;
	private double[] nfelt_display = new double[3];
	private double pScenario1;
	private double pScenario2;
	private double pScenario3;

	private Color scenarioOneColor;
	private Color scenarioTwoColor;
	private Color scenarioThreeColor;
		
	private double[] predictionMagnitudes = new double[]{3,4,5,6,7,8};
	private double[] predictionIntervals = new double[]{DAY,WEEK,MONTH,YEAR}; //day,week,month,year
	private int preferredForecastInterval = 1; //week
	double fracIncrement = 0.0025;
	
//	private String[] predictionIntervalStrings = new String[]{"day","week","month","year"}; //day,week,month,year
	private double[][][] number;  //dimensions: [predMag][predInterval][range: exp, lower, upper];
	private String[][] numberString;
	private double[][] probability;
	private String[][] probString;
	private String forecastHorizon;
	
	private String[] MMIcolors = {"#abe0ff", "#81fff3", "#aaff63", "#ffd100", "#EC2516", "#800000"};
	private HashMap<String, String> tags = new HashMap<String, String>();
	
	public GraphicalForecast(){
		// run with dummy data. This is all made up for debugging.
		location = "13 km SE of Scenariotown";
		lat0 = 18;
		lon0 = -67;
		mag0 = 7.4;
		aftershockRadiusKM = 120;
		depth0 = 9.0;
		tPredStart = 1.0;
		mapRadiusDeg = 0.5;
		eventDate.set(2020, 0, 7, 06, 30, 46); //dummy values
		
		
		number = new double[predictionMagnitudes.length][predictionIntervals.length][3];
		probability = new double[predictionMagnitudes.length][predictionIntervals.length];
		
		switch (predictionIntervals.length) {
			case 1:
				forecastHorizon = "day";
				break;
			case 2:
				forecastHorizon = "week";
				break;
			case 3:
				forecastHorizon = "month";
				break;
			case 4:
				forecastHorizon = "year";
				break;
			default:
				forecastHorizon = "year";
				break;
		}
		
		setPreferredForecastInterval(2); //month
		
		double[] fractiles = new double[3];
		
		double baseRate = 100; //dummy values
		for (int i = 0; i < predictionMagnitudes.length; i++){
			for (int j = 0; j < predictionIntervals.length; j++){
				double rate = baseRate*(Math.pow(2.0,(j-1d))/Math.pow(10d, i)) ; //dummy values
				probability[i][j] = 1 - Math.exp( -rate ); //dummy values
				
				fractiles[0] = rate; //dummy values
				fractiles[1] = rate - Math.sqrt(rate); //dummy values
				fractiles[2] = rate*10d; //dummy values
				
				number[i][j][0] = fractiles[0]; //dummy values
				number[i][j][1] = fractiles[1]; //dummy values
				number[i][j][2] = fractiles[2]; //dummy values
			}
		}
		
		// do weekly probabilities of felt and damaging quakes
		double rate = baseRate*(1+1d)/Math.pow(10d, 3); //dummy values
		pDamage_display = 1 - Math.exp( -rate ); //dummy values
		pHeadline_display = 1 - Math.exp( -11.0*rate ); //dummy values
		
		rate = baseRate*(1+1d)/Math.pow(10d, 0); //dummy values
		fractiles[0] = rate; //dummy values
		fractiles[1] = rate - Math.sqrt(rate); //dummy values
		fractiles[2] = rate*10d; //dummy values
		
		nfelt_display[0] = Math.max(fractiles[0],0); //dummy values
		nfelt_display[1] = Math.max(fractiles[1],0); //dummy values
		nfelt_display[2] = Math.max(fractiles[2],1); //dummy values
		
		//set scenario probabilities
		pScenario1 = 0.96;
		pScenario2 = 0.04;
		pScenario3 = 0.005;
	}
	
	public GraphicalForecast(File outFile, ETAS_AftershockModel aftershockModel, GregorianCalendar eventDate,
			GregorianCalendar startDate) {
		this(outFile, aftershockModel, eventDate, startDate, 4, null);
	}
	
	public GraphicalForecast(File outFile, ETAS_AftershockModel aftershockModel, GregorianCalendar eventDate,
			GregorianCalendar startDate, SphRegion region) {
		this(outFile, aftershockModel, eventDate, startDate, 4, region);
	}
	
	public GraphicalForecast(File outFile, ETAS_AftershockModel aftershockModel, GregorianCalendar eventDate,
			GregorianCalendar startDate, int numberOfTimeIntervals, SphRegion region) {
		
		startDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		eventDate.setTimeZone(TimeZone.getTimeZone("UTC"));

		if(D) System.out.println("numberOfTimeIntervals = " + numberOfTimeIntervals); 
			
		this.predictionIntervals = Arrays.copyOf(this.predictionIntervals, numberOfTimeIntervals);
		this.region = region;
		this.forecastStartDate = startDate;
		this.eventDate = eventDate;
		this.aftershockModel = aftershockModel;
		this.outFile = outFile;
		this.msName = aftershockModel.mainShock.getEventId(); //get USGS name
		this.eventURL = "https://earthquake.usgs.gov/earthquakes/eventpage/" + msName + "#executive";
		versionDate.setTimeInMillis(System.currentTimeMillis());
		this.location = getStringParameter(aftershockModel.mainShock, "description");
		this.lat0 = aftershockModel.mainShock.getHypocenterLocation().getLatitude();
		this.lon0 = aftershockModel.mainShock.getHypocenterLocation().getLongitude();
		this.mag0 = aftershockModel.mainShock.getMag();
		this.depth0 = aftershockModel.mainShock.getHypocenterLocation().getDepth();
		this.tPredStart = getDateDelta(eventDate, forecastStartDate);
		this.b = aftershockModel.get_b();
//		setPreferredForecastInterval(2);
		
	}

	private String getStringParameter(ObsEqkRupture rup, String paramName){
		ListIterator<?> iter = rup.getAddedParametersIterator();
		if (iter != null) {
			while (iter.hasNext()){
				Parameter<?> param = (Parameter<?>) iter.next();
				if (param.getName().equals(paramName))
					return param.getValue().toString();
			}
		} 
		return "";
	}
	public void constructForecast(){
		number = new double[predictionMagnitudes.length][predictionIntervals.length][3];
		probability = new double[predictionMagnitudes.length][predictionIntervals.length];
		
		double tMinDays = getDateDelta(eventDate, forecastStartDate);
		double[] calcFractiles = new double[]{0.5,0.025,0.975};
		double[] fractiles = new double[3];
		double tMaxDays;
		
		for (int i = 0; i < predictionMagnitudes.length; i++){
			for (int j = 0; j < predictionIntervals.length; j++){
				tMaxDays = tMinDays + predictionIntervals[j];
				
				fractiles = aftershockModel.getCumNumFractileWithAleatory(calcFractiles, predictionMagnitudes[i], tMinDays, tMaxDays);
				number[i][j][0] = fractiles[0];
				number[i][j][1] = fractiles[1];
				number[i][j][2] = fractiles[2];
				probability[i][j] = aftershockModel.getProbabilityWithAleatory(predictionMagnitudes[i], tMinDays, tMaxDays);
			}
		}
		
		// do probabilities of felt and damaging quakes
		if (preferredForecastInterval > predictionIntervals.length)
			preferredForecastInterval = predictionIntervals.length-1;
		
		tMaxDays = tMinDays + predictionIntervals[preferredForecastInterval];
		
		pDamage_display = aftershockModel.getProbabilityWithAleatory(damageMag, tMinDays, tMaxDays);
		pHeadline_display = aftershockModel.getProbabilityWithAleatory(headlineMag, tMinDays, tMaxDays);
		fractiles = aftershockModel.getCumNumFractileWithAleatory(calcFractiles, feltMag, tMinDays, tMaxDays);
		nfelt_display[0] = Math.max(fractiles[0],0);
		nfelt_display[1] = Math.max(fractiles[1],0);
		nfelt_display[2] = Math.max(fractiles[2],0);

		processForecastStrings();
		setForecastHorizon();
		assignForecastStrings();
		
//		writeHTML(outFile); //call it explicitly
	}
	
	private void processForecastStrings(){
		
		// numbers and probabilities
		// extract numbers and probabilities from ETAS_forecast
		numberString = new String[predictionMagnitudes.length][predictionIntervals.length];
		probString = new String[predictionMagnitudes.length][predictionIntervals.length];
		
		for (int i = 0; i < predictionMagnitudes.length; i++){
			for (int j = 0; j < predictionIntervals.length; j++){
				if (number[i][j][1] == 0 && number[i][j][2] == 0)
					numberString[i][j] = "0*";
				else
					numberString[i][j] = numberRange(number[i][j][1], number[i][j][2]);

				if (probability[i][j] < 0.001)
					probString[i][j] = "";
				else if (probability[i][j] <= 0.005)
					probString[i][j] = "<1%";
//				else if (probability[i][j] < 0.01)
//					probString[i][j] = String.format("%2.1f%%", 100*probability[i][j]);
				else if (probability[i][j] > 0.99)
					probString[i][j] = ">99%";
				else
					probString[i][j] = String.format("%1.0f%%", 100*probability[i][j]);
				
				if(D){System.out.println("M" + predictionMagnitudes[i] + " " + predictionIntervals[j] 
						+ " " + String.format("%5.4f", probability[i][j]) +" " + probString[i][j] + " " + numberString[i][j]);}
			}
		}
	}

	private void setForecastHorizon(){
		
		if (predictionIntervals.length == 4 && (number[0][3][0] >= number[0][2][0] + 1 || //number[predMagIndex][predIntervalIndex][median, lower,  upper]
				number[0][3][2] >= number[0][2][2] + 1))
		    forecastHorizon = "year";
		else if (predictionIntervals.length == 3 && (number[0][2][0] >= number[0][1][0] + 1 ||
				number[0][2][2] >= number[0][1][2] + 1))
			forecastHorizon = "month";
		else if (predictionIntervals.length == 2)
			forecastHorizon = "week";
		else 
			forecastHorizon = "week";
	}
	
	// Set all the variable bits of text 
	private void assignForecastStrings(){
		tags.put("DISCLAIMER", "For Official Use Only - Not For Public Distribution");
		
		if (predictionIntervals.length > 0) {
			tags.put("N1_DA", numberString[0][0]);
			tags.put("P1_DA", probString[0][0]);
			tags.put("N2_DA", numberString[1][0]);
			tags.put("P2_DA", probString[1][0]);
			tags.put("N3_DA", numberString[2][0]);
			tags.put("P3_DA", probString[2][0]);
			tags.put("N4_DA", numberString[3][0]);
			tags.put("P4_DA", probString[3][0]);
			tags.put("N5_DA", numberString[4][0]);
			tags.put("P5_DA", probString[4][0]);
			tags.put("N6_DA", numberString[5][0]);
			tags.put("P6_DA", probString[5][0]);
//			tags.put("N7_DA", numberString[6][0]);
//			tags.put("P7_DA", probString[6][0]);
		} 
		if (predictionIntervals.length > 1) {
			tags.put("N1_WK", numberString[0][1]);
			tags.put("P1_WK", probString[0][1]);
			tags.put("N2_WK", numberString[1][1]);
			tags.put("P2_WK", probString[1][1]);
			tags.put("N3_WK", numberString[2][1]);
			tags.put("P3_WK", probString[2][1]);
			tags.put("N4_WK", numberString[3][1]);
			tags.put("P4_WK", probString[3][1]);
			tags.put("N5_WK", numberString[4][1]);
			tags.put("P5_WK", probString[4][1]);
			tags.put("N6_WK", numberString[5][1]);
			tags.put("P6_WK", probString[5][1]);
//			tags.put("N7_WK", numberString[6][1]);
//			tags.put("P7_WK", probString[6][1]);
		}
		if (predictionIntervals.length > 2) {
			tags.put("N1_MO", numberString[0][2]);
			tags.put("P1_MO", probString[0][2]);
			tags.put("N2_MO", numberString[1][2]);
			tags.put("P2_MO", probString[1][2]);
			tags.put("N3_MO", numberString[2][2]);
			tags.put("P3_MO", probString[2][2]);
			tags.put("N4_MO", numberString[3][2]);
			tags.put("P4_MO", probString[3][2]);
			tags.put("N5_MO", numberString[4][2]);
			tags.put("P5_MO", probString[4][2]);
			tags.put("N6_MO", numberString[5][2]);
			tags.put("P6_MO", probString[5][2]);
//			tags.put("N7_MO", numberString[6][2]);
//			tags.put("P7_MO", probString[6][2]);
		}
		if (predictionIntervals.length > 3) {
			tags.put("N1_YR", numberString[0][3]);
			tags.put("P1_YR", probString[0][3]);
			tags.put("N2_YR", numberString[1][3]);
			tags.put("P2_YR", probString[1][3]);
			tags.put("N3_YR", numberString[2][3]);
			tags.put("P3_YR", probString[2][3]);
			tags.put("N4_YR", numberString[3][3]);
			tags.put("P4_YR", probString[3][3]);
			tags.put("N5_YR", numberString[4][3]);
			tags.put("P5_YR", probString[4][3]);
			tags.put("N6_YR", numberString[5][3]);
			tags.put("P6_YR", probString[5][3]);
//			tags.put("N7_YR", numberString[6][3]);
//			tags.put("P7_YR", probString[6][3]);
		}
		
//		tags.put("M1_R", String.format("%2.1f", predictionMagnitudes[0]));
//		tags.put("M2_R", String.format("%2.1f", predictionMagnitudes[1]));
//		tags.put("M3_R", String.format("%2.1f", predictionMagnitudes[2]));
		
		tags.put("NFELT_DISP", numberRange(nfelt_display[1], nfelt_display[2]));
		
		if (smartRoundPercentages) {
			if (pDamage_display < 0.001)
				tags.put("PDAMAGE_DISP", "much less than 1");
			else if (pDamage_display < 0.005)
				tags.put("PDAMAGE_DISP", "less than 1");
			else if (pDamage_display < 0.01)
				tags.put("PDAMAGE_DISP", "around 1");
			else if (pDamage_display < 0.05)
				tags.put("PDAMAGE_DISP", "1-5");
			else if (pDamage_display < 0.95)
				tags.put("PDAMAGE_DISP", String.format("%d", smartRound(Math.floor(pDamage_display*100/5)*5)) + " - " +
						String.format("%d", (int) Math.ceil(pDamage_display*100/5)*5));
			else if (pDamage_display < 0.99)
				tags.put("PDAMAGE_DISP", "greater than 95");
			else if (pDamage_display <= 1.0) 
				tags.put("PDAMAGE_DISP", "greater than 99");
			else
				tags.put("PDAMAGE_DISP", "NaN");
			
		} else {
			String formatStr;
			if (pDamage_display < 0.01)
				formatStr = "%2.1f";
			else
				formatStr = "%1.0f";
			tags.put("PDAMAGE_DISP", String.format(formatStr, pDamage_display*100));
		}
		
		if (pHeadline_display < 0.0005)
			tags.put("PHEADLINE_DISP", "<0.1");
		else if (pHeadline_display < 0.01) 
			tags.put("PHEADLINE_DISP", String.format("%2.1f", pHeadline_display*100));
		else if (pHeadline_display < 0.995)
			tags.put("PHEADLINE_DISP", String.format( "%1.0f", pHeadline_display*100));
		else if (pHeadline_display <= 1.0) 
			tags.put("PHEADLINE_DISP", "99");
		else 
			tags.put("PHEADLINE_DISP", "NaN");
		
		if (D) System.out.println(preferredForecastInterval);
		
		switch (preferredForecastInterval) {
			case 0:
				tags.put("FORECAST_INTERVAL", "day");
				break;
			case 1:
				tags.put("FORECAST_INTERVAL", "week");
				break;
			case 2:
				tags.put("FORECAST_INTERVAL", "month");
				break;
			case 3:
				tags.put("FORECAST_INTERVAL", "year");
				break;
			default:
				tags.put("FORECAST_INTERVAL", "year");
				break;
		}
		
		//lat lon of mainshock
		String degSym = "&deg;";
		String tag;
		if (lat0 >= 0)
			tag = degSym + "N";
		else
			tag = degSym + "S";
		tags.put("MS_LAT", String.format("%4.3f" + tag, Math.abs(lat0)));
		
		lon0 = unwrap(lon0);
		
		if (lon0 >= 0) 
			tag = degSym + "E";
		else
			tag = degSym + "W";
		tags.put("MS_LON", String.format("%4.3f" + tag, Math.abs(lon0)));
	
		//		construct descriptive text
		SimpleDateFormat formatter=new SimpleDateFormat("d MMM yyyy, HH:mm");  
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		tags.put("MS_MAG", String.format("M%2.1f", mag0));
		tags.put("MS_NAME", msName);
		tags.put("MS_DATETIME", formatter.format(eventDate.getTime()));
		tags.put("MS_DEPTH", String.format("%2.1f km", depth0));
		tags.put("V_NUM", versionNumber);
		tags.put("V_DATE", formatter.format(versionDate.getTime()));
		
		formatter = new SimpleDateFormat("d MMM yyyy, HH:mm");  
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		tags.put("F_START_ABS", formatter.format(forecastStartDate.getTime()));
		
		String MS_LOC = location;
		String F_START_REL_DAYS = String.format("%2.1f", tPredStart);
		String DAMAGE_MAG = String.format("%.0f",  damageMag);
		String HEADLINE_MAG = String.format("%.0f",  headlineMag);
		String FELT_MAG = String.format("%.0f",  feltMag);
		String F_HORIZON = forecastHorizon;
		String DAYS;
		
		tags.put("MS_LOC", location);
		tags.put("DAMAGE_MAG", DAMAGE_MAG);
		tags.put("FELT_MAG", FELT_MAG);
		tags.put("HEADLINE_MAG", HEADLINE_MAG);
		tags.put("F_HORIZON", forecastHorizon);
		
		if (tPredStart == 1)
		    DAYS = "day";
		else
		    DAYS = "days";
		
		String DESCRIPTIVE_TEXT = 
		    "An earthquake of magnitude " + tags.get("MS_MAG") + " occurred " + F_START_REL_DAYS + " " + DAYS + " ago " + MS_LOC + ". " +
		    "More earthquakes than usual will continue to occur in the area, decreasing " +
		    "in frequency over the following " + F_HORIZON + " or longer. During the next " + tags.get("FORECAST_INTERVAL") +
		    " there are likely to be " + tags.get("NFELT_DISP") + " aftershocks large enough to be felt locally, " +
		    "and there is a " + tags.get("PDAMAGE_DISP") + "% chance of at least one damaging M" + DAMAGE_MAG + " (or larger) aftershock." +
		    " The earthquake rate may be re-invigorated in response to large aftershocks, should they occur. ";
		
		tags.put("DESCRIPTIVE_TEXT", DESCRIPTIVE_TEXT);
	
		String BULLET_TEXT1 = "Expect more earthquakes in and around the area currently affected by aftershocks.";
		String BULLET_TEXT2 = "Over the next " + tags.get("FORECAST_INTERVAL") +  " there may be " + tags.get("NFELT_DISP") + 
				" aftershocks of M" + FELT_MAG + " or larger, which could be felt nearby.";
		String BULLET_TEXT3 = "Over the next " + tags.get("FORECAST_INTERVAL") +  " there is a " + tags.get("PDAMAGE_DISP") + 
				"% chance of at least one damaging M" + DAMAGE_MAG + " (or larger) aftershock.";
		String BULLET_TEXT4 = "Aftershock rates will decrease over time, but may remain elevated over the following year or longer.";
		String BULLET_TEXT5 = "This forecast will be updated as the sequences progresses and more information becomes available.";
		
		tags.put("BULLET_TEXT1", BULLET_TEXT1);
		tags.put("BULLET_TEXT2", BULLET_TEXT2);
		tags.put("BULLET_TEXT3", BULLET_TEXT3);
		tags.put("BULLET_TEXT4", BULLET_TEXT4);
		tags.put("BULLET_TEXT5", BULLET_TEXT5);
		
		double geomFactor = Math.cos(lat0*Math.PI/180.0);
		double minLat = lat0 - mapRadiusDeg; 
		double maxLat = lat0 + mapRadiusDeg;
		double minLon = lon0 - mapRadiusDeg/geomFactor;
		double maxLon = lon0 + mapRadiusDeg/geomFactor;
		
		GregorianCalendar forecastEndDate = new GregorianCalendar();
	
		forecastEndDate.setTimeInMillis(forecastStartDate.getTimeInMillis() + (long) (predictionIntervals[preferredForecastInterval]*ETAS_StatsCalc.MILLISEC_PER_DAY));
		
		formatter=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");  
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String DATE_START_SEARCH = formatter.format(eventDate.getTime());
		System.out.println(DATE_START_SEARCH);
		char[] dateStart = DATE_START_SEARCH.toCharArray();		
		dateStart[10]='T';
		
		String DATE_END_SEARCH = formatter.format(forecastEndDate.getTime());
		char[] dateEnd = DATE_END_SEARCH.toCharArray();		
		dateEnd[10]='T';

		String mapLink = "\"https://earthquake.usgs.gov/earthquakes/map/?"
		+ "extent=" + String.format("%4.4f", minLat) + "," + String.format("%4.4f", minLon) + "&amp;"
		+ "extent=" + String.format("%4.4f", maxLat) + "," + String.format("%4.4f", maxLon) + "&amp;"
		+ "range=search&amp;"
			+ "baseLayer=terrain&amp;"
			+ "timeZone=utc&amp;"
			+ "list=false&amp;"
			+ "search=%7B%22"
				+ "name%22:%22" + msName + "%22,%22"
				+ "params%22:%7B%22"
				+ "endtime%22:%22" +  String.copyValueOf(dateEnd) + "Z%22,%22"
				+ "latitude%22:" + lat0 + ",%22"
				+ "longitude%22:" + lon0 + ",%22"
				+ "maxradiuskm%22:" + aftershockRadiusKM + ",%22"
				+ "minmagnitude%22:" + feltMag + ",%22"
				+ "starttime%22:%22" + String.copyValueOf(dateStart) + "Z%22%7D%7D\"";
	
		tags.put("MAP_LINK", mapLink);
		if (D) System.out.println(mapLink);
	}
	
	public void setFeltMag(double mag){
		this.feltMag = mag;
	}

	public double getFeltMag(){
		return feltMag;
	}

	public void setDamageMag(int mag){
		this.damageMag = mag;
	}

	public double getDamageMag(){
		return damageMag;
	}

	public void setHeadlineMag(int mag){
		this.headlineMag = mag;
	}

	public double getHeadlineMag(){
		return headlineMag;
	}

	public void setAftershockRadiusKM(double r){
		this.aftershockRadiusKM = r;
	}

	public double getAftershockRadiusKM(){
		return aftershockRadiusKM;
	
	}
	
	public void setPreferredForecastInterval(int interval){
		if (interval < 4)
			this.preferredForecastInterval = interval;
		else
			this.preferredForecastInterval = 3;
		if(D) System.out.println("forecast duration set to " + this.preferredForecastInterval);
	}

	public int getPreferredForecastInterval(){
		return preferredForecastInterval;
	}
	public void setMapRadiusDeg(double deg){
		this.mapRadiusDeg = deg;
	}

	public double getMapRadiusDeg(){
		return mapRadiusDeg;
	}

//	This is disabled until the rest of the code can accomodate a flexible magnitude range
//	public void setPredictionMagnitudes(double[] predictionMagnitudes){
//		this.predictionMagnitudes = predictionMagnitudes;
//	}

	//	returns a string token of form "N1 - N2" where N1 and N2 represent a range of values
	private String numberRange(double number1, double number2){
		if (smartRound(number2) == 0)
			return "0*";
		else
			return smartRound(number1) + " - " + smartRound(number2);
		
	}
	
	// rounds the number to an conversational level of accuracy
	private int smartRound(double number){
		long roundNumber;
		
		if (number < 20)
			roundNumber = Math.round(number);
		else if (number < 100)
			roundNumber = Math.round(number/5d)*5;
		else if (number < 200)
			roundNumber = Math.round(number/10d)*10;
		else if (number < 1000)
			roundNumber = Math.round(number/50d)*50;
		else 
			roundNumber = Math.round(number/100d)*100;

		return (int) roundNumber;
	}

	// return the time difference between two GregorianCalendar objects. why is this not a GregCal.method()?
	private static double getDateDelta(GregorianCalendar start, GregorianCalendar end) {
		long diff = end.getTimeInMillis() - start.getTimeInMillis();
		return (double)diff/ETAS_ComcatAccessor.day_millis;
	}
	
	public void writeHTMLTable(File outputFile){
		StringBuilder tableString = new StringBuilder();

		//header
		tableString.append(""
				+"<html>\n"
				+"	<head>\n"
				+"		<style>\n"
		        +"			body {font-family:helvetica; background-color: white; page-break-inside:avoid}\n"
		        +"	        .tableForecast {font-size:14px; border:0px solid gray; border-collapse:collapse; margin:0px; text-align:center}\n"
		        +"			.tfElem2 {font-size:14px; border:0px solid gray; border-collapse:collapse; padding:1px; margin:0; text-align:center}\n"
		        +"			.tfElem1 {font-size:14px; background-color:#eeeeee; border-collapse:collapse; padding:1px; margin:0; text-align:center}\n"
		        +" 			.tableFootnote {font-size:12px;text-align:right;}\n"
		        +"			.forecastHeader {font-weight:bold; padding:4px; width:50px}\n"
		        +"		</style>\n"
		        +"	</head>\n"
		        +"	<body>\n");

		//generate forecast table
		tableString.append(""
				+" 	<table class=\"tableForecast\">\n"
				+"		<tr class=\"forecastHeader\">\n"
				+ "			<th style=\"width:150px; padding:4px\">Forecast Interval</th>\n"
				+ "			<th style=\"width:75px; padding:4px\">Magnitude M</th>\n"
				+ "			<th style=\"width:75px; padding:4px\">Expected Number</th>\n"
				+ "			<th style=\"width:150px; padding:4px\">Possible Number (95% confidence)</th>\n"
				+ "			<th style=\"width:100px; padding:4px\">Probability of at least one</th>\n"
				+ "		</tr>\n"
				+"		<tr><td colspan=\"5\"></td></tr>\n");
			
		String DATE_START = tags.get("F_START_ABS");
		String DATE_END;
		SimpleDateFormat formatter=new SimpleDateFormat("d MMM yyyy, HH:mm");  
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		GregorianCalendar forecastEndDate = new GregorianCalendar();
		
		// find the largest magnitude to plot
		// assign variables
		int minMag = 3;
		double maxObsMag = mag0;
		
		if (aftershockModel != null) {
			for (int i = 0; i < aftershockModel.magAftershocks.length; i++) {
				if (aftershockModel.magAftershocks[i] > maxObsMag)
					maxObsMag = aftershockModel.magAftershocks[i];
			}
		}
		int maxMag = Math.max(minMag + 3, Math.min(9, (int) Math.ceil(maxObsMag + 0.5)));
		if(D) System.out.println("maxMag: " + maxMag + " largestShockMag: " + maxObsMag);

		int numTableEntries = (maxMag - minMag) + 1;
		
		String[] durString = new String[]{"Day","Week","Month","Year"};
		for (int j = 0; j<predictionIntervals.length; j++){
			forecastEndDate.setTimeInMillis(forecastStartDate.getTimeInMillis() + (long) (predictionIntervals[j]*ETAS_StatsCalc.MILLISEC_PER_DAY));
			DATE_END = formatter.format(forecastEndDate.getTime());
			tableString.append(""
					+"		<tr class=\"tfElem2\"><td rowspan=\"" + numTableEntries + "\"><div style=\"font-weight:bold;display:inline\">1 " + durString[j]
			 		+"		</div><br>"+ DATE_START +"<br>through<br>"+ DATE_END +"</td>\n");
			 		
			int n = 0;
			for (int i = 0; i < predictionMagnitudes.length; i++) {
				String classStr = (Math.floorMod(n, 2) == 0)?"tfElem1":"tfElem2";
				int mag = (int) predictionMagnitudes[i];
//				if( mag == 3 || i >= predictionMagnitudes.length-4) {//always plot the M3s and then the last four
//				if (mag == 3 || (mag > 3 && mag > maxMag - 4 && mag <= maxMag)) {
				if (mag >= 3 && mag <= maxMag) {
				
					tableString.append(""
							+" 		<td class=\""+ classStr +"\" styly=\"width:50px\">"
							+ "M &#8805 " + mag + "</td><td class=\""+ classStr +"\">"
							+ Math.round(number[i][j][0]) + "</td><td class=\""+ classStr +"\">"
							+ numberRange(number[i][j][1], number[i][j][2]) +"</td><td class=\""+ classStr +"\">"
							+ ((probability[i][j] >= 0.001)?probString[i][j]:"<0.1%") +"</td></tr>\n");
					
					n++;
				}
			}
			if (j == predictionIntervals.length - 1)
				tableString.append("<tr><td colspan=\"5\" class=\"tableFootnote\">*Earthquake possible but with low probability</td><tr>\n");
			else
				tableString.append("	<tr><td colspan=\"5\"><br></td></tr>\n");
				
		}
		tableString.append(""
	            +"		</table>\n"
	            +" 	</body>\n"
	            +"</html>\n");
		
		//write to a file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile, false);
		} catch (IOException e1) {
			//				e1.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
			return;
		}

		try {
			fw.append(tableString);
		} catch (IOException e) {
			//					e.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
		}

		try {
			fw.close();
		} catch (IOException e) {
			//				e.printStackTrace();
			System.err.println("Problem closing file.");
		}
	}
	
	// Build a json summary that can be sent to PDL
	public void writeSummaryJson(File outputFile) {
		writeSummaryJson( outputFile,  aftershockModel,  eventDate, forecastStartDate); 
	}
	
	public void writeSummaryJson(File outputFile, ETAS_AftershockModel aftershockModel, GregorianCalendar eventDate,
				GregorianCalendar startDate) {
		StringBuilder jsonString = new StringBuilder();
		
		long startTimeMillis = System.currentTimeMillis();
		long expireTimeMillis = startTimeMillis + (long)3.1573603746e10;
		
		//set up fractiles
		int nfrac = (int)(1.0/fracIncrement) - 1;
		double[] fractiles = ETAS_StatsCalc.linspace(fracIncrement, 1-fracIncrement, nfrac);
		
		//build a string for the json
		StringBuilder fractileProbabilityString = new StringBuilder();
		for(int k = 0; k < fractiles.length - 1; k++) {
			fractileProbabilityString.append(String.format("%5.4f,", fractiles[k]));
		}
		fractileProbabilityString.append(String.format("%5.4f", fractiles[fractiles.length-1]));
		
		
		//report number of observed earthquakes
		int num3s = 0;
		int num5s = 0;
		int num6s = 0;
		int num7s = 0;
		
		if(aftershockModel != null) {
			num3s = aftershockModel.aftershockList.getRupsAboveMag(2.99d).size();
			num5s = aftershockModel.aftershockList.getRupsAboveMag(4.99d).size();
			num6s = aftershockModel.aftershockList.getRupsAboveMag(5.99d).size();
			num7s = aftershockModel.aftershockList.getRupsAboveMag(6.99d).size();
		};
		
		
		double foreshockProbability;
		GregorianCalendar forecastEndDate = new GregorianCalendar();
		
		String[] durString = new String[]{"\"1 Day\"","\"1 Week\"","\"1 Month\"","\"1 Year\""};
		//header
		jsonString.append(""
				+ "{\n"
				+ "\"creationTime\":" + startTimeMillis + ",\n"
				+ "\"expireTime\":" + expireTimeMillis + ",\n"
				+ "\"advisoryTimeFrame\":" + durString[preferredForecastInterval] + ",\n"
				+ "\"template\":\"Mainshock\",\n"
				+ "\"injectableText\":\"\",\n"
				);

		//observations
		jsonString.append(""
				+ "\"observations\":["
				+ "\n\t{\"magnitude\":3.0,\"count\":" + num3s 
				+ "},\n\t{\"magnitude\":5.0,\"count\":" + num5s 
				+ "},\n\t{\"magnitude\":6.0,\"count\":" + num6s
				+ "},\n\t{\"magnitude\":7.0,\"count\":" + num7s 
				+ "}\n\t],\n"
				);
		 
		//model
		jsonString.append(""
				+ "\"model\":{"
				+ "\n\t\"name\":\"Epidemic-Type aftershock model (Bayesian Combination)\","
				+ "\n\t\"reference\":\"#url\","
				+ "\n\t\"parameters\":{" 
				+ "\n\t\t\"ams\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_ams())
				+ ",\n\t\t\"a\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_a()) 
				+ ",\n\t\t\"b\":" + String.format("%3.2f", aftershockModel.get_b()) 
				+ ",\n\t\t\"magMain\":" + String.format("%2.1f", aftershockModel.getMainShockMag()) 
				+ ",\n\t\t\"p\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_p()) 
				+ ",\n\t\t\"c\":" + String.format("%5.4f", aftershockModel.getMaxLikelihood_c())
				+ ",\n\t\t\"Mc\":" + String.format("%2.1f", aftershockModel.magComplete)
				);
		if(region != null) {
			jsonString.append(""
				+ ",\n\t\t\"regionType\":\"circle\""
				+ ",\n\t\t\"regionCenterLat\":" + String.format("%2.1f", region.getCircleCenterLat())
				+ ",\n\t\t\"regionCenterLon\":" + String.format("%2.1f", region.getCircleCenterLon())
				+ ",\n\t\t\"regionRadius\":" + String.format("%2.1f", region.getCircleRadiusKm())
				);
		}
	
		
		
		
		jsonString.append(""
				+ "\n\t\t}\n\t},"
				+ "\n\"fractileProbabilities\":[" + fractileProbabilityString + "],"
				+ "\n\"forecast\":["
				);
		

		for (int j = 0; j<predictionIntervals.length; j++){
			forecastEndDate.setTimeInMillis(forecastStartDate.getTimeInMillis() + (long) (predictionIntervals[j]*ETAS_StatsCalc.MILLISEC_PER_DAY));
			foreshockProbability = aftershockModel.getProbabilityWithAleatory(aftershockModel.getMainShockMag(),
					aftershockModel.getForecastMinDays(), aftershockModel.getForecastMinDays() + predictionIntervals[j]);
			
			
			jsonString.append(""
					+ "\n\t{\"timeStart\":" + forecastStartDate.getTimeInMillis() 
					+ ",\n\t\"timeEnd\":" + forecastEndDate.getTimeInMillis()
					+ ",\n\t\"label\":" + durString[j] 
					+ ",\n\t\"bins\":["
					);
			
			for (int i = 0; i < predictionMagnitudes.length; i++) {
				double mag = predictionMagnitudes[i];
				
				StringBuilder fractileString = new StringBuilder();
//				double[] fractiles = {0.0125,0.025,0.0375,0.05,0.0625,0.075,0.0875,0.1,0.1125,0.125,0.1375,0.15,0.1625,0.175,0.1875,0.2,0.2125,0.225,0.2375,0.25,0.2625,0.275,0.2875,0.3,0.3125,0.325,0.3375,0.35,0.3625,0.375,0.3875,0.4,0.4125,0.425,0.4375,0.45,0.4625,0.475,0.4875,0.5,0.5125,0.525,0.5375,0.55,0.5625,0.575,0.5875,0.6,0.6125,0.625,0.6375,0.65,0.6625,0.675,0.6875,0.7,0.7125,0.725,0.7375,0.75,0.7625,0.775,0.7875,0.8,0.8125,0.825,0.8375,0.85,0.8625,0.875,0.8875,0.9,0.9125,0.925,0.9375,0.95,0.9625,0.975,0.9875};
				
//				int nfrac = (int)(1.0/fracIncrement) - 1;
//				double[] fractiles = ETAS_StatsCalc.linspace(fracIncrement, 1-fracIncrement, nfrac);
				
				double[] fractileValues = aftershockModel.getCumNumFractileWithAleatory(fractiles, mag,
						aftershockModel.getForecastMinDays(), aftershockModel.getForecastMinDays() + predictionIntervals[j]);
				
				for(int k = 0; k < fractileValues.length - 1; k++) {
					fractileString.append(String.format("%d,", Math.round(fractileValues[k])));
				}
				fractileString.append(String.format("%d", Math.round(fractileValues[fractileValues.length-1])));
//				fractileString = String.format("%5.4f,", fractileValues);
						
						
				if (2.99 < mag && mag < 7.01) { //added M4s NvdE 7/3/2020
					jsonString.append(""
							+ "\n\t\t{\"magnitude\":" + String.format("%2.1f", mag)
							+ ",\n\t\t\"p95minimum\":" + String.format("%d", (int) number[i][j][1])
							+ ",\n\t\t\"p95maximum\":" + String.format("%d", (int) number[i][j][2])
							+ ",\n\t\t\"probability\":" + String.format("%5.4f",probability[i][j])
							+ ",\n\t\t\"median\":" + String.format("%d", Math.round(number[i][j][0]))
							+ ",\n\t\t\"fractileValues\":[" + fractileString + "]"
							+ "\n\t\t}");
					if (mag < 7) jsonString.append(",");
				}
			}
			
			jsonString.append(""
					+ "],\n\t\t\"aboveMainshockMag\":{"
					+ "\n\t\t\"magnitude\":" + String.format("%2.1f", aftershockModel.getMainShockMag())
					+ ",\n\t\t\"probability\":" + String.format("%5.4f", foreshockProbability)
					+ "\n\t\t}\n\t}"
					);
			if (j < predictionIntervals.length - 1) jsonString.append(",");
		}
		jsonString.append("\n\t]\n}");
		if(D) System.out.println(jsonString);
			
		//write to a file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile, false);
		} catch (IOException e1) {
			//				e1.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
			return;
		}

		try {
			fw.append(jsonString);
		} catch (IOException e) {
			//					e.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
		}

		try {
			fw.close();
		} catch (IOException e) {
			//				e.printStackTrace();
			System.err.println("Problem closing file.");
		}
	}
	
//	public void writeSummaryJsonNexp(File outputFile, ETAS_AftershockModel aftershockModel, GregorianCalendar eventDate,
//			GregorianCalendar startDate, double[][] observedNumber, double[][] observedFractile) {
//	StringBuilder jsonString = new StringBuilder();
//	
//	long startTimeMillis = System.currentTimeMillis();
//	long expireTimeMillis = startTimeMillis + (long)3.1573603746e10;
//	
//	int num3s = aftershockModel.aftershockList.getRupsAboveMag(2.99d).size();
//	int num5s = aftershockModel.aftershockList.getRupsAboveMag(4.99d).size();
//	int num6s = aftershockModel.aftershockList.getRupsAboveMag(5.99d).size();
//	int num7s = aftershockModel.aftershockList.getRupsAboveMag(6.99d).size();
//	
//	double foreshockProbability;
//	
//	GregorianCalendar forecastEndDate = new GregorianCalendar();
//	
//	String[] durString = new String[]{"\"1 Day\"","\"1 Week\"","\"1 Month\"","\"1 Year\""};
//	
//	//header
//	jsonString.append(""
//			+ "{\"creationTime\":" + startTimeMillis + ","
//			+ "\"expireTime\":" + expireTimeMillis + ","
//			+ "\"advisoryTimeFrame\":" + durString[preferredForecastInterval] + ","
//			+ "\"template\":\"Mainshock\","
//			+ "\"injectableText\":\"\","
//			);
//
//	//observations
//	jsonString.append(""
//			+ "\"observations\":[{\"magnitude\":3.0,\"count\":" + num3s 
//			+ "},{\"magnitude\":5.0,\"count\":" + num5s 
//			+ "},{\"magnitude\":6.0,\"count\":" + num6s
//			+ "},{\"magnitude\":7.0,\"count\":" + num7s 
//			+ "}],"
//			);
//	 
//	//model
//	jsonString.append(""
//			+ "\"model\":{\"name\":\"Epidemic-Type aftershock model (Bayesian Combination)\",\"reference\":\"#url\",\"parameters\":{" 
//			+ "\"ams\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_ams())
//			+ ",\"a\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_a()) 
//			+ ",\"b\":" + String.format("%3.2f", aftershockModel.get_b()) 
//			+ ",\"magMain\":" + String.format("%2.1f", aftershockModel.getMainShockMag()) 
//			+ ",\"p\":" + String.format("%3.2f", aftershockModel.getMaxLikelihood_p()) 
//			+ ",\"c\":" + String.format("%5.4f", aftershockModel.getMaxLikelihood_c())
//			+ ",\"Mc\":" + String.format("%2.1f", aftershockModel.magComplete)
//			+ "}},\"forecast\":["
//			);
//	
//	for (int j = 0; j<predictionIntervals.length; j++){
//		forecastEndDate.setTimeInMillis(forecastStartDate.getTimeInMillis() + (long) (predictionIntervals[j]*ETAS_StatsCalc.MILLISEC_PER_DAY));
//		foreshockProbability = aftershockModel.getProbabilityWithAleatory(aftershockModel.getMainShockMag(),
//				aftershockModel.getForecastMinDays(), aftershockModel.getForecastMinDays() + predictionIntervals[j]);
//		
//		
//		jsonString.append(""
//				+ "{\"timeStart\":" + forecastStartDate.getTimeInMillis() 
//				+ ",\"timeEnd\":" + forecastEndDate.getTimeInMillis()
//				+ ",\"label\":" + durString[j] 
//						+ ",\"bins\":["
//				);
//		
//		for (int i = 0; i < predictionMagnitudes.length; i++) {
//			double mag = predictionMagnitudes[i];
//
//			if (2.99 < mag && mag < 7.01) { //added M4s NvdE 7/3/2020
//				jsonString.append(""
//						+ "{\"magnitude\":" + String.format("%2.1f", mag)
//						+ ",\"p50median\":" + String.format("%d", (int) number[i][j][0])
//						+ ",\"p95minimum\":" + String.format("%d", (int) number[i][j][1])
//						+ ",\"p95maximum\":" + String.format("%d", (int) number[i][j][2])
//						+ ",\"probability\":" + String.format("%5.4f",probability[i][j])
//						+ ",\"observedNumber\":" + String.format("%d", (int) observedNumber[i][j])
//						+ ",\"observedFractile\":" + String.format("%5.4f", observedFractile[i][j])
//						+ "}");
//				if (mag < 7) jsonString.append(",");
//			}
//		}
//		
//		jsonString.append(""
//				+ "],\"aboveMainshockMag\":{"
//				+ "\"magnitude\":" + String.format("%2.1f", aftershockModel.getMainShockMag())
//				+ ",\"probability\":" + String.format("%5.4f", foreshockProbability)
//				+ "}}"
//				);
//		if (j < predictionIntervals.length - 1) jsonString.append(",");
//	}
//	jsonString.append("]}");
//	if(D) System.out.println(jsonString);
//		
//	//write to a file
//	FileWriter fw;
//	try {
//		fw = new FileWriter(outputFile, false);
//	} catch (IOException e1) {
//		//				e1.printStackTrace();
//		System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
//		return;
//	}
//
//	try {
//		fw.append(jsonString);
//	} catch (IOException e) {
//		//					e.printStackTrace();
//		System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
//	}
//
//	try {
//		fw.close();
//	} catch (IOException e) {
//		//				e.printStackTrace();
//		System.err.println("Problem closing file.");
//	}
//}

	public String getScenarioText() {
		// write some scenario text, responsive to the mainshock magnitude
		double tMinDays = getDateDelta(eventDate, forecastStartDate);
		double tMaxDays;

		// do weekly probabilities of felt and damaging quakes
		if (preferredForecastInterval > predictionIntervals.length)
			preferredForecastInterval = predictionIntervals.length;
		
		tMaxDays = tMinDays + predictionIntervals[preferredForecastInterval];
		
		double mag1, mag2;
		
		if (mag0 > 6.9999) {
			mag1 = 6;
			mag2 = mag0;
		} else if (mag0 > 5.9999) {
			mag1 = 6;
			mag2 = 7;
		} else {
			mag1 = 5;
			mag2 = 6;
		}
	
		if (D) System.out.println(mag2 + " " + tMinDays + " " + tMaxDays);
		if (aftershockModel != null) {
			//set scenario probabilities based on magnitudes and probabilities
			pScenario3 = aftershockModel.getProbabilityWithAleatory(mag2, tMinDays, tMaxDays);
			pScenario2 = aftershockModel.getProbabilityWithAleatory(mag1, tMinDays, tMaxDays) - pScenario3;
			pScenario1 = 1 - aftershockModel.getProbabilityWithAleatory(mag1, tMinDays, tMaxDays);
		}
	
		// set scenario box colors/saturations
		int RGBvals;
		float[] HSBvals = new float[3];
		HSBvals = Color.RGBtoHSB(170,  255, 99, HSBvals);  
		RGBvals = Color.HSBtoRGB(HSBvals[0], (float)pScenario1, HSBvals[2]); //  (HSBvals[0], (float)pScenario1, HSBvals[2]); 
		scenarioOneColor = new Color(RGBvals);
		
//		"#aaff63", "#ffd100", "#EC2516"
		HSBvals = Color.RGBtoHSB(253,  16, 0, HSBvals);  
		RGBvals = Color.HSBtoRGB(HSBvals[0], (float)pScenario2, HSBvals[2]); //  (HSBvals[0], (float)pScenario1, HSBvals[2]); 
		scenarioTwoColor = new Color(RGBvals);
		
		HSBvals = Color.RGBtoHSB(194,  81, 6, HSBvals);  
		RGBvals = Color.HSBtoRGB(HSBvals[0], (float)pScenario3, HSBvals[2]); //  (HSBvals[0], (float)pScenario1, HSBvals[2]); 
		scenarioThreeColor = new Color(RGBvals);
		
		
		//"SCENARIO_ONE_PROB"
		if (pScenario1 < 0.005)
			tags.put("SCENARIO_ONE_PROB", "<1");
		else if (pScenario1 < 0.995)
			tags.put("SCENARIO_ONE_PROB", String.format( "%1.0f", pScenario1*100));
		else if (pScenario1 <= 1.0) 
			tags.put("SCENARIO_ONE_PROB", ">99");
		else 
			tags.put("SCENARIO_ONE_PROB", "NaN");
		//"SCENARIO_TWO_PROB"
		if (pScenario2 < 0.005)
			tags.put("SCENARIO_TWO_PROB", "<1");
		else if (pScenario2 < 0.995)
			tags.put("SCENARIO_TWO_PROB", String.format( "%1.0f", pScenario2*100));
		else if (pScenario2 <= 1.0) 
			tags.put("SCENARIO_TWO_PROB", ">99");
		else 
			tags.put("SCENARIO_TWO_PROB", "NaN");
		//"SCENARIO_ONE_PROB"
		if (pScenario3 < 0.005)
			tags.put("SCENARIO_THREE_PROB", "<1");
		else if (pScenario3 < 0.995)
			tags.put("SCENARIO_THREE_PROB", String.format( "%1.0f", pScenario3*100));
		else if (pScenario3 <= 1.0) 
			tags.put("SCENARIO_THREE_PROB", ">99");
		else 
			tags.put("SCENARIO_THREE_PROB", "NaN");

		String scenario1 = new String();
		String scenario2 = new String();
		String scenario3 = new String();
		
		tags.put("SCENARIO1_QUAL", "Most likely");
		tags.put("SCENARIO1_QUAL_TEXT", "The most likely");
		tags.put("SCENARIO2_QUAL", "Less likely");
		tags.put("SCENARIO2_QUAL_TEXT", "A less likely");

		tags.put("SCENARIO3_QUAL", "Least likely");

		//TODO: increase the magnitude threshold until scenario 1 is higher probability than scenario 2 
		
//		if (mag0 > 7.69999) {
//			// <67 / 7 - Mmain / Mmain+;
//			scenario1 = tags.get("SCENARIO1_QUAL_TEXT") + " scenario is that aftershocks will continue to decrease in "
//					+ "frequency with no aftershocks larger than M7 within the next " 
//					+ tags.get("FORECAST_INTERVAL") 
//					+ ". Moderately sized aftershocks (M5 and M6) are likely "
//					+ "and could still cause localized damage, particularly in weakened structures. Smaller magnitude "
//					+ "earthquakes (M3 and M4) may be felt by people close to the epicenters.";
//			
//			scenario2 = tags.get("SCENARIO2_QUAL_TEXT") + " scenario would include one or more aftershocks larger than M7, but with"
//					+ " none larger than the " + tags.get("MS_MAG") + " mainshock."
//					+ " Aftershocks of this size could cause additional damage and temporarily re-energize"
//					+ " the aftershock sequence. These aftershocks would most likely affect the area already"
//					+ " impacted by the mainshock.";
//			
//			scenario3 = "The least likely scenario is that the sequence could generate an aftershock of the same"
//					+ " size or even larger than the " + tags.get("MS_MAG") + " mainshock. While this is a small probability,"
//					+ " such an earthquake would"
//					+ " likely affect communities both in and adjacent to the areas already impacted by the mainshock,"
//					+ " and would trigger additional aftershocks.";
//		} else 
		if (mag0 > 6.9999) {
			// <6 / 6 - Mmain / Mmain+;
			scenario1 = tags.get("SCENARIO1_QUAL_TEXT") + " scenario is that aftershocks will continue to decrease in "
					+ "frequency with no aftershocks larger than M6 within the next " 
					+ tags.get("FORECAST_INTERVAL") 
					+ ". Moderately sized aftershocks (M5 and larger) "
					+ "may still cause localized damage, particularly in weak structures. Smaller magnitude "
					+ "earthquakes (M3 and M4) may be felt by people close to the epicenters.";
			
			scenario2 = tags.get("SCENARIO2_QUAL_TEXT") + " scenario would include one or more aftershocks larger than M6, but with"
					+ " none larger than the " + tags.get("MS_MAG") + " mainshock."
					+ " Aftershocks of this size could cause additional damage and temporarily re-energize"
					+ " the aftershock sequence. These aftershocks would most likely affect the area already"
					+ " impacted by the mainshock.";
			
			scenario3 = "The least likely scenario is that the sequence could generate an aftershock of the same"
					+ " size or even larger than the " + tags.get("MS_MAG") + " mainshock. Such an earthquake would"
					+ " likely affect communities both in and adjacent to the areas already impacted by the mainshock,"
					+ " and would trigger additional aftershocks.";
		} else if (mag0  > 5.9999) {
			// <6 / 6 - 7 / 7+;
			scenario1 =  tags.get("SCENARIO1_QUAL_TEXT") + " scenario is that aftershocks will continue to decrease in "
					+ "frequency with no aftershocks larger than M6 within the next "
					+ tags.get("FORECAST_INTERVAL") 
					+ ". Moderately sized aftershocks (M5 and larger) "
					+ "may still cause localized damage, particularly in weak structures. Smaller magnitude "
					+ "earthquakes (M3 and M4) may be felt by people close to the epicenters.";
			
			scenario2 = tags.get("SCENARIO2_QUAL_TEXT") + " scenario would include one or more aftershocks between M6 and M7."
					+ " Aftershocks of this size could cause additional damage and temporarily re-energize"
					+ " the aftershock sequence. These aftershocks would most likely affect the area in and adjacent to the areas already"
					+ " impacted by the mainshock.";
			
			scenario3 = "The least likely scenario is that the sequence could generate an aftershock larger than M7. While this is a "
					+ "small probability, such an earthquake would have considerable impact on communities in and adjacent to"
					+ " areas already impacted by the mainshock. Such an earthquake would likely trigger an "
					+ "aftershock sequence of its own.";
		} else {
			scenario1 = tags.get("SCENARIO1_QUAL_TEXT") + " scenario is that aftershocks will continue to decrease in "
					+ "frequency with no aftershocks larger than M5 within the next " 
					+ tags.get("FORECAST_INTERVAL")
					+ ". Smaller magnitude earthquakes (M3 and M4) may be felt by people close to the epicenters.";
			
			scenario2 = tags.get("SCENARIO2_QUAL_TEXT") + " scenario would include one or more aftershocks between M5 and M6."
					+ " Aftershocks of this size could cause additional localized damage and temporarily re-energize"
					+ " the aftershock sequence. These aftershocks would most likely affect the area in and adjacent to areas already"
					+ " impacted by the mainshock.";
			
			scenario3 = "The least likely scenario is that the sequence could generate an earthquake larger than M6. While this is a "
					+ " small probability, such an earthquake would affect communities both in and adjacent to the"
					+ " areas already impacted by the mainshock, and would likely trigger an aftershock sequence of its own.";
		}
		
		StringBuilder outputString = new StringBuilder();
		outputString.append("<!-- Scenarios -->\n"
				+ "  <div class=\"break\">\n"
				+ "	  <span class=\"disclaimer\">" + tags.get("DISCLAIMER") + "</span>\n"
				+ "    		<h1>Aftershock Sequence Scenarios</h1>\n"
				+ "    <div>\n"
				+ "      These are three likely scenarios for how the aftershock sequence will evolve <br>\n"
				+ "      over the next <span style=\"font-weight:bold\">" + tags.get("FORECAST_INTERVAL") + "</span> starting " + tags.get("F_START_ABS")+ " (UTC)</span>\n"
				+ "    </div>\n"
				+ "    <table>\n"
				+ "      <tr class=\"h_scenario\">\n"
				+ "        <td colspan=\"2\">\n"
				+ "          Scenario One (" + tags.get("SCENARIO1_QUAL") + ")\n"
				+ "        </td>\n"
				+ "      </tr>\n"
				+ "      <tr >\n"
				+ "        <td class=\"scenario_probability\">" + tags.get("SCENARIO_ONE_PROB") + "%</td>\n"
				+ "        <td class=\"scenario_text\">" + scenario1 + "</td>\n"
				+ "      </tr>\n"
				+ "      <tr class=\"h_scenario\">\n"
				+ "        <td colspan=\"2\">\n"
				+ "			Scenario Two (" + tags.get("SCENARIO2_QUAL") + ")\n"
				+ "        </td>\n"
				+ "      </tr>\n"
				+ "      <tr>\n"
				+ "        <td class=\"scenario_probability\">"  + tags.get("SCENARIO_TWO_PROB") +  "%</td>\n"
				+ "        <td class=\"scenario_text\">" + scenario2 + "</td>\n"
				+ "      </tr>\n"
				+ "      <tr class=\"h_scenario\">\n"
				+ "        <td colspan=\"2\">\n"
				+ "          Scenario Three (" + tags.get("SCENARIO3_QUAL") + ")\n"
				+ "        </td>\n"
				+ "      </tr>\n"
				+ "      <tr>\n"
				+ "        <td class=\"scenario_probability\">"  + tags.get("SCENARIO_THREE_PROB") +  "%</td>\n"
				+ "        <td class=\"scenario_text\">" + scenario3 + "</td>\n"
				+ "      </tr>\n"
				+ "    </table>\n"
				+ "  </div>\n"
				+ "  <br>");
		
		return outputString.toString();
	}
	
	// Build an html document for displaying the advisory, new style for BHA
	public void writeHTML(File outputFile){

		StringBuilder outputString = new StringBuilder();
		StringBuilder headString = new StringBuilder();
		StringBuilder infoString = new StringBuilder();

		headString.append(""
				+"    <!DOCTYPE html>\n"
				+"	  <html>\n"
				+"    <head>\n"
				+" 		<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n"
				+" 		<meta name=\"source\" content=\"USGS AftershockForecaster\">\n"
				+"		<meta name=\"software-version\" content=\"2021-10-01\">\n"
				+"		<meta name=\"software-version-date\" content=\"2021-10-01\">\n"
				+"		<meta name=\"advisory-generated-date\" content=\"" + tags.get("V_DATE") + "\">\n"
				+"		<meta name=\"disclaimer\" content=\"This advisory was generated using the USGS AftershockForecaster softare developed\n"
				+"			on the OpenSHA platform by the US Geological Survey and the US-AID Bureau of Humanitarian Assistance. The forecast\n"
				+" 			provided herein is based primarily on the range of outcomes of past aftershock sequences in similar environments.\n"
				+"			While this forecast describes the most likely outcomes for sequences like this one, it should not be treated as a\n"
				+"			a specific prediction. This document is for official use only.\">\n"
				+"		<meta name=\"OpenSHA\" content=\"www.opensha.org/apps\">\n"
				+"		<link rel=\"stylesheet\" href=\"BHAforecast.css\">\n"
				+"		<title>Aftershock Advisory and Forecast</title>\n"
				+"		</head>\n"
				+"");

		infoString.append(""
				+ "  <body>\n"
				+ "  <!-- title disclaimer and logos -->\n"
				+ "  <div>\n"
				+ "    <table class=\"header\">\n"
				+ "      <tr>\n"
				+ "        <td class=\"leftLogo\"><img src=\"USAID_logo.png\" alt=\"\" class=\"logo\"></td>\n"
				+ "        <td><span class=\"disclaimer\">" + tags.get("DISCLAIMER") + "</span>\n"
				+ "				<br><h1>Aftershock Advisory and Forecast</h1></td>\n"
				+ "        <td class=\"rightLogo\"><img src=\"Logo.png\" alt=\"\" class=\"logo\"></td>\n"
				+ "      </tr>\n"
				+ "    </table>\n"
				+ "  </div>\n"
				+"");

		infoString.append("  <!-- single number summary -->\n"
				+ "  <div>\n"
				+ "    <table class=\"headline\"> \n"
				+ "      <tr><td><span>As of <span class=\"bold\">"
				+ tags.get("F_START_ABS") +" (UTC)" 
				+ "</span> there is a</span></td></tr>\n"
				+ "      <tr><td class=\"headline_text\"><span class=\"one_number\">"
				+ tags.get("PHEADLINE_DISP")
				+"%</span> chance of an <span class=\"one_number\">"
				+"M" + tags.get("HEADLINE_MAG")
				+"</span> or larger within the next <span class=\"one_number\">"
				+ tags.get("FORECAST_INTERVAL")
				+"</span></td></tr>\n"
				+ "      <tr><td>in and around the area currently affected by aftershocks.</td></tr>\n"
				+ "    </table>\n"
				+ "  </div><br>\n"
				+ "");

		infoString.append("  <!-- Mainshock and forecast information -->\n"
				+ "  <div>\n"
				+ "    <table class=\"mainshock_information\">\n"
				+ "      <tr>\n"
				+ "        <td style=\"text-align:left\">Mainshock Magnitude: "
				+ tags.get("MS_MAG") + "</td>\n"
				+ "        <td style=\"text-align:center\">ID: <a href=\"" + eventURL + "\">"
				+ tags.get("MS_NAME") + "</a></td>\n"
				+ "        <td style=\"text-align:right\">Location: "
				+ tags.get("MS_LOC") + "</td>\n"
				+ "      </tr>\n"
				+ "    </table>\n"
				+ "    <table class=\"mainshock_information\">\n"
				+ "      <tr>\n"
				+ "        <td style=\"text-align:left\">Mainshock Date: "
				+ tags.get("MS_DATETIME") + "</td>\n"
				+ "        <td style=\"text-align:right\">Forecast last updated: "
				+ tags.get("V_DATE") + " UTC</td>\n"
				+ "      </tr>\n"
				+ "    </table>\n"
				+ "  </div>\n"
				+ "");

		infoString.append("	<!-- Descriptive forecast -->\n"
				+ "  <div>\n"
				+ "    <ul>\n"
				+ "      <li>"+ tags.get("BULLET_TEXT1") + "</li>\n"
				+ "      <li>"+ tags.get("BULLET_TEXT2") + "</li>\n"
				+ "      <!-- <li>"+ tags.get("BULLET_TEXT3") + "</li> -->\n" 
				+ "      <li>"+ tags.get("BULLET_TEXT4") + "</li>\n"
				+ "      <li>"+ tags.get("BULLET_TEXT5") + "</li>\n"
				+ "    </ul>\n"
				+ "  </div><br>\n"
				+ "");

		infoString.append("  <!-- graphical summary frame holder-->\n"
				+ "  <div>\n"
				+ "		<iframe class=\"graphical_forecast\" src=\"graphical_forecast.html\"></iframe>\n"
				+ "  </div>\n"
				+ "");

		infoString.append("  <!--Aftershock Map-->\n"
				+ "    <!--    map legend       -->\n"
				+ "    <div class=\"map_legend\">\n"
				+ "       Aftershocks so far. Colors indicate aftershocks that have occurred within the past \n"
				+ "       <span class=\"red\">hour</span>, \n"
				+ "       <span class=\"orange\">day</span>, \n"
				+ "       <span class=\"yellow\">week</span>, \n"
				+ "       <span class=\"white\">month</span>, or \n"
				+ "       <span class=\"gray\">earlier</span>.\n"
				+ "       Future aftershocks will most likely affect the area already affected by the mainshock and the aftershocks so far.\n"
				+ "    </div>\n"
				+ "  <div class=\"map\">\n"
				+ "    <a href=" + tags.get("MAP_LINK") + ">\n"
				+ "    	<img class=\"map\" src=\"aftershock_map.png\" alt=\"Follow this link, take a 2:1 screenshot, save locally as aftershock_map.png\">\n"
				+ "    </a>\n"
				+ "  </div>\n"
				+ "  <br>\n"
				+ "");
		
		infoString.append(getScenarioText());
		
		infoString.append("<!-- Table of probabilities -->\n"
				+ "  <div>\n"
				+ "    <span class=\"disclaimer\">" +  tags.get("DISCLAIMER") + "</span>\n"
				+ "    <br>\n"
				+ "    <h1>Aftershock Forecast Table</h1><iframe class=\"forecast_table\" src=\"Table.html\"></iframe>\n"
				+ "  </div>");
		
		String imageStr = new String();
		switch (preferredForecastInterval) {
		case 0:
			imageStr = "shakingday.png";
			break;
		case 1:
			imageStr = "shakingweek.png";
			break;
		case 2:
			imageStr = "shakingmonth.png";
			break;
		default:
			imageStr = "shakingyear.png";
			break;
		}
		
		infoString.append("  <!-- Shaking Map -->\n"
				+ "  <div class=\"shaking_forecast break\">\n"
				+ "    <span class=\"disclaimer\">" + tags.get("DISCLAIMER") + "</span>\n"
				+ "    <h1>Aftershock Shaking Forecast</h1>\n"
				+ "    <p style=\"margin-top:5px\">For forecast starting: " + tags.get("F_START_ABS") + " (UTC)</p><img width=\"800\" src=\""+ imageStr + "\" alt=\"Shaking Forecast\">\n"
				+ "    <div class=\"shaking_forecast_legend\">\n"
//				+ "      Small gray circles indicate locations of past aftershocks in this sequence.\n"
//				+ "      Contour lines (if shown) give the chance of experiencing potentially damaging ground motions \n"
//				+ "      (exceeding level VI on the Modified Mercalli Intensity scale).\n"
//				+ "      Modified Mercalli Intensity level VI (strong ground shaking) is likely to cause damage, even in well-engineered structures. With more poorly engineered or weakened structures, damage can occur at lower intensity levels.\n"
				+ "      This map shows the chance of experiencing strong shaking (Intensity level VI) from an aftershock within the next " + tags.get("FORECAST_INTERVAL") + "."
				+ "		 Intensity level VI shaking can cause light damage in well-built structures, and can cause moderate to severe damage in less well-built or weakened structures. Shaking is even more likely at lower intensity levels, which could still cause damage in poorly built or weakened structures.\n"
				+ "    </div>\n"
				+ "  </div>");
		
		infoString.append("</body></html>\n");
		
		outputString.append(headString);
		outputString.append(infoString);
		
		// write file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile, false);
		} catch (IOException e1) {
			//				e1.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
			return;
		}

		try {
			fw.append(outputString);
		} catch (IOException e) {
			//					e.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
		}
		
		try {
			fw.close();
		} catch (IOException e) {
			//				e.printStackTrace();
			System.err.println("Problem closing file.");
		}

	}

	public void writeCSS(File outputFile) {
		// the css file for the new HTML document for BHA
		
		StringBuilder outputString = new StringBuilder();
		outputString.append("body {font-family:helvetica; background-color: white; page-break-inside:avoid}\n"
				+ "h1 {color:black; font-family:helvetica; font-size:20pt; margin:0; text-align:center}\n"
				+ "h2 {color:black; font-family:helvetica; font-size:14pt; margin:0; text-align:center}\n"
				+ "h3 {color:black; font-family:helvetica; font-size:12pt; margin:0}\n"
				+ "th {font-weight:normal}\n"
				+ "tr {padding:0px}\n"
				+ "td {padding:0px}\n"
				+ "	\n"
				+ "ul {margin-bottom:0px; font-size:12pt; width:800px; text-align:justify}\n"
				+ "li {margin-bottom:3pt; font-weight:normal}\n"
				+ "	\n"
				+ "img.logo {max-height:40px; max-width:175px}\n"
				+ "table.header {width:800px}\n"
				+ "table.headline {width:800px; border:4px solid #116441; text-align:center; font-family:helvetica;}\n"
				+ "\n"
				+ "iframe.graphical_forecast  {width:800px;height:275px;border:none;text-align:center}\n"
				+ "table.graphical_forecast {width:700px;margin-left:50px;margin-right:0px}\n"
				+ "table.mainshock_information {width:800px; font-weight:normal; color:#333333}\n"
				+ "\n"
				+ "\n"
				+ "span.one_number {color:#116441; font-family:helvetica; font-size:28pt; margin:0px}\n"
				+ "span.bold {font-weight:bold}\n"
				+ "\n"
				+ "div {max-width:800px; border-collapse:collapse; text-align:center}\n"
				+ "div.map {text-align:center; width:700px; height:400px; margin-left:50px}\n"
				+ "img.map {min-height:50px;max-width:700px;max-height:400px}\n"
				+ "div.map_legend {background-color:#A4C1DC; width:700px; margin-left:50px; margin-bottom:0px; font-size:10pt}\n"
				+ "div.shaking_forecast {width:800px;margin-left:0px}\n"
				+ "div.shaking_forecast_legend {font-size:11pt; background-color:#ffffff; width:800px; \n"
				+ "	text-align:center; margin-left:0px}\n"
				+ ".forecast_table {width:550px;min-height:600px;border:none;text-align:center}\n"
				+ ".break {page-break-before:always; page-break-after:avoid}\n"
				+ "span.red {color:red}\n"
				+ "span.orange {color:orange}\n"
				+ "span.yellow {color:yellow}\n"
				+ "span.white {color:white}\n"
				+ "span.gray {color:gray}\n"
				+ "\n"
				+ "tr.forecastRow {border:1px solid #bbbbbb; border-collapse:collapse; padding-top:1px;}\n"
				+ "\n"
				+ "td.MMIkey {vertical-align:top}\n"
				+ "table.MMIkeyHeader {border:1px solid #bbbbbb;}\n"
				+ "tr.bold {font-weight:bold}\n"
				+ "th.wide {width:145px; font-weight:bold}\n"
				+ "th.narrow {width:50px; font-weight:bold}\n"
				+ "td.emptyKey {height:23px}\n"
				+ "\n"
				+ "\n"
				+ ".leftLogo {width:175px; text-align:left}\n"
				+ ".rightLogo {width:175px; text-align:right}\n"
				+ ".disclaimer {color:#ff6666;text-align:center;margin-bottom:5px}\n"
				+ ".headline_text {color:black; font-size:24pt;}\n"
				+ "\n"
				+ ".forecast { font-size:14px; border:0px solid gray; border-collapse:collapse; margin:0; text-align:center}\n"
				+ ".forecastHeader { font-size:16px; border:0px solid gray; border-collapse:collapse; text-align:center; vertical-align:center; font-weight:normal; height:20px;}\n"
				+ ".forecastValue { font-size:12px; border:0px solid gray; border-collapse:collapse; text-align:center; margin:0; vertical-align:bottom; color:#666666;}\n"
				+ ".forecastKey { font-size:12px; border:0px solid gray; border-collapse:collapse; text-align:center; margin:0; vertical-align:bottom; padding:0px}\n"
				+ ".forecastKeySmall { font-size:10px; border:0px solid gray; border-collapse:collapse; text-align:center; margin:0; vertical-align:bottom; padding:0px}\n"
				+ ".forecastBar { height:40px; width:50px; padding-top:1px;}\n"
				+ ".forecastBox {stroke-width:0px; x:2px; width:46px}\n"
				+ ".forecastBoxText {text-anchor:middle; fill:#666666;}\n"
				+ ".key {width:30px;height:12px}\n"
				+ ".hgov {color:#dd1111; font-family:helvetica; font-size:14pt; margin:0}\n"
				+ ".scenario_probability {text-align:center; width:100px; height:60px; color:black; background:#eeeeee;\n"
				+ "		font-family:helvetica; font-size:28pt; vertical-align:center; margin-right:5px; margin-left:0px}\n"
				+ ".scenario_text {text-align:justify; vertical-align:top; font-size:12pt}\n"
				+ ".h_scenario {color:black; background:#ffffff; height:30px; text-align:left; font-weight:bold; \n"
				+ "	font-family:helvetica; font-size:12pt; margin:0; vertical-align:bottom}\n"
				+ "\n"
				+ "");
		
		// write file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile, false);
		} catch (IOException e1) {
			//				e1.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
			return;
		}

		try {
			fw.append(outputString);
		} catch (IOException e) {
			//					e.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
		}

		try {
			fw.close();
		} catch (IOException e) {
			//				e.printStackTrace();
			System.err.println("Problem closing file.");
		}
	}
	
	
	public void writeBarGraphHTML(File outputFile) {
		StringBuilder outputString = new StringBuilder();
		StringBuilder probTableString = new StringBuilder();
		StringBuilder keyString = new StringBuilder();
		
		//parameters for the svg table
		double barHeight = 40;
//		if(predictionIntervals.length==3)
//			barHeight = 40;
//		else if(predictionIntervals.length==2)
//			barHeight = 40;
//		else if(predictionIntervals.length==1)
//			barHeight = 40;

		double barWidth = 50;

		String[][] tableTags = new String[][]{{"P1_DA","P2_DA","P3_DA","P4_DA","P5_DA","P6_DA"},
			{"P1_WK","P2_WK","P3_WK","P4_WK","P5_WK","P6_WK"},
			{"P1_MO","P2_MO","P3_MO","P4_MO","P5_MO","P6_MO"},
			{"P1_YR","P2_YR","P3_YR","P4_YR","P5_YR","P6_YR"}};

		probTableString.append("<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "	<title>Aftershock Forecast Bar Graph</title>\n"
				+ "	<link rel=\"stylesheet\" href=\"BHAforecast.css\">\n"
				+ "</head>\n"
				+ "<body>");
		
		

		probTableString.append("  <!-- Graphical summary -->\n"
				+ "  <div>\n"
				+ "    <h2>Aftershock Forecast starting " + tags.get("F_START_ABS") +  " (UTC)</h2>\n"
				+ "  </div>\n"
				+ "  <table class = \"graphical_forecast\">\n"
				+ "    <!-- Table of Probabilities -->\n"
				+ "    <tr>\n"
				+ "      <td>\n"
				+ "        <table>\n"
				+ "          <tr>\n"
				+ "            <th></th>\n"
				+ "            <th class=\"forecast\">Chance of an aftershock larger than:</th>\n"
				+ "          </tr>\n"
				+ "          <tr>\n"
				+ "            <td>\n"
				+ "              <table>\n"
				+ "                <tr>"
				+ "                  <th class=\"forecastHeader\"></th>\n"
				+ "                </tr>\n"
				+ "");

		probTableString.append("			<tr><th class=\"forecastBar\">Day</th></tr>\n");
		if (predictionIntervals.length > 1)	
			probTableString.append("			<tr><th class=\"forecastBar\">Week</th></tr>\n");
		if (predictionIntervals.length > 2)
			probTableString.append("			<tr><th class=\"forecastBar\">Month</th></tr>\n");
		if (predictionIntervals.length > 3)
			probTableString.append("			<tr><th class=\"forecastBar\">Year</th></tr>\n");
		
		probTableString.append(""
				+"				</table>\n"
				+"            </td>\n"
				+"            <td>\n"
				+"              <table class=\"forecast\">\n"
				+"                <tr>\n"
				+"  				 <th class=\"forecastHeader\">M3</th>\n"
				+"                   <th class=\"forecastHeader\">M4</th>\n"
				+"                	 <th class=\"forecastHeader\">M5</th>\n"
				+"                   <th class=\"forecastHeader\">M6</th>\n"
				+"                   <th class=\"forecastHeader\">M7</th>\n"
				+"                   <th class=\"forecastHeader\">M8</th>\n"
				+"                </tr>\n"
				+"");
		
		//generate forecast table
		int maxRow = predictionIntervals.length;
		int minRow = 0;
		//				if (predictionIntervals.length == 4) //commented out for PR earthquake
		//						minRow = 1;

		for (int j = minRow; j<maxRow; j++){
			probTableString.append(""
					+"                                    <tr class = \"forecastRow\">\n");

			for (int i = 0; i<MMIcolors.length; i++){
				double probVal = probability[i][j];
				double height = barHeight*probVal;
				String probStr = tags.get(tableTags[j][i]); 
				double yVal;
				if (probVal > 0.50) yVal = 11 + barHeight*(1 - probVal);
				else yVal = barHeight*(1 - probVal) - 3;

				probTableString.append(""
						+"                                        <td class=\"forecastValue\">\n"
						+"												<svg class=\"forecastBar\">\n"
						+"													<rect class = \"forecastBox\" y=\"" + (int) (barHeight - (int) height) + "px\" height=\"" + ((int) height) + "px\" width=\"" + barWidth + "px\" fill=\""+MMIcolors[i]+"\" />\n"
						+"													<text class = \"forecastBoxText\" x=\"25px\" y=" + String.format("\"%.0fpx\"", yVal) + ">"+probStr+"</text>\n"
						+"												</svg>\n"
						+"                                        </td>\n"
						);
			}
			probTableString.append(""
					+"                                    </tr>\n");
		}

		probTableString.append(""
				+"                                </table>\n"
				+"                            </td>\n"
				+"                        </tr>\n"
				+"                    </table>\n"
				+"                </td>\n\n");

		keyString.append("               <td class=\"MMIkey\">\n"
				+"                    <!-- MMI key-->\n"
				+"                    <table>\n"
				+"                        <tr><td class=\"emptyKey\"></td></tr>\n"
				+"                        <tr><td class=\"forecast\">Key to colors*</td></tr>\n"
				+"                        <tr><td>\n"
				+"                            <table class=\"forecastKey MMIkeyHeader\">\n"
				+"                                <tr class=\"bold\">\n"
				+"                                    <th class=\"narrow bold\"></th>\n"
				+"                                    <th class=\"wide bold\">Potential Shaking</th>\n"
				+"                                    <th class=\"wide bold\">Potential Damage</th>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[0] + "\"/></svg></td>\n"
				+"                                    <td>weak - light</td>\n"
				+"                                    <td>none</td>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[1] + "\"/></svg></td>\n"
				+"                                    <td>weak - moderate</td>\n"
				+"                                    <td>very light</td>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[2] + "\"/></svg></td>\n"
				+"                                    <td>moderate - strong</td>\n"
				+"                                    <td>light - moderate</td>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[3] + "\"/></svg></td>\n"
				+"                                    <td>strong - severe</td>\n"
				+"                                    <td>moderate - heavy</td>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[4] + "\"/></svg></td>\n"
				+"                                    <td>severe - violent</td>\n"
				+"                                    <td>heavy</td>\n"
				+"                                </tr>\n"
				+"                                <tr>\n"
				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[5] + "\"/></svg></td>\n"
				+"                                    <td>violent - extreme</td>\n"
				+"                                    <td>very heavy</td>\n"
				+"                                </tr>\n"
				+"                            </table>\n"
				+"                        </td></tr>\n"
				+"                    </table>\n"
				+"						<p class=\"forecastKey\" style=\"font-size:10px\">*This table gives typical peak shaking and intensity levels associated with the forecast magnitudes. Actual shaking is affected by many factors, and damage may be higher in vulnerable structures.</p>\n"
				+"                </td>\n"
				+"            </tr>\n"
				+"        </table>\n\n");
		outputString.append(probTableString);
		outputString.append(keyString);


				
		// write file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile, false);
		} catch (IOException e1) {
			//				e1.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
			return;
		}

		try {
			fw.append(outputString);
		} catch (IOException e) {
			//					e.printStackTrace();
			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
		}

		try {
			fw.close();
		} catch (IOException e) {
			//				e.printStackTrace();
			System.err.println("Problem closing file.");
		}
	}
		
		
	// Build an html document for displaying the advisory
//	public void writeHTMLclassic(File outputFile){
//	
//		StringBuilder outputString = new StringBuilder();
//	
//		StringBuilder headString = new StringBuilder();
//		StringBuilder infoString = new StringBuilder();
//		StringBuilder probTableString = new StringBuilder();
//		StringBuilder keyString = new StringBuilder();
//		StringBuilder imgString = new StringBuilder(); 
//		
//		//parameters for the svg table
//		double barHeight = 40;
//		if(predictionIntervals.length==2)
//			barHeight = 60;
//		else if(predictionIntervals.length==1)
//			barHeight = 120;
//		
//		
//		double barWidth = 50;
//		double barPadding = 2;
////		String[][] tableTags = new String[][]{{"P1_DA","P2_DA","P3_DA","P4_DA","P5_DA","P6_DA","P7_DA"},
////				{"P1_WK","P2_WK","P3_WK","P4_WK","P5_WK","P6_WK","P7_WK"},
////				{"P1_MO","P2_MO","P3_MO","P4_MO","P5_MO","P6_MO","P7_MO"},
////				{"P1_YR","P2_YR","P3_YR","P4_YR","P5_YR","P6_YR","P7_YR"}};
//		String[][] tableTags = new String[][]{{"P1_DA","P2_DA","P3_DA","P4_DA","P5_DA","P6_DA"},
//				{"P1_WK","P2_WK","P3_WK","P4_WK","P5_WK","P6_WK"},
//				{"P1_MO","P2_MO","P3_MO","P4_MO","P5_MO","P6_MO"},
//				{"P1_YR","P2_YR","P3_YR","P4_YR","P5_YR","P6_YR"}};
//		
//					
//		
//		headString.append(""
//				+"    <!DOCTYPE html>\n"
//				+"	  <html>\n"
//				+"    <head>\n"
//				+" 		<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n"
//				+" 		<meta name=\"source\" content=\"USGS AftershockForecaster\">\n"
//				+"		<meta name=\"software-version\" content=\"Beta\">\n"
//				+"		<meta name=\"software-version-date\" content=\"2018-05-01\">\n"
//				+"		<meta name=\"advisory-generated-date\" content=\"" + tags.get("V_DATE") + "\">\n"
//				+"		<meta name=\"disclaimer\" content=\"This advisory was generated using softare developed on the OpenSHA platform by the\n"
//				+"			US Geological Survey and the US-AID Bureau of Humanitarian Assistance. The information provided in this document\n"
//				+"			is not an official statement of the US Geological Survey or any other US Government Entity.\">\n"
//				+"		<meta name=\"OpenSHA\" content=\"www.opensha.org/apps\">\n"
//				+"        <style>\n"
//				+"            body {font-family:helvetica; background-color: white; page-break-inside:avoid}\n"
//				+"            h1 {color: black; font-family:helvetica; font-size:20pt; margin:0}\n"
//				+"            h2 {color: black; font-family:helvetica; font-size:14pt; margin:0; text-align:center}\n"
//				+"            h3 {color: black; font-family:helvetica; font-size:12pt; margin:0;}\n"
//				+"            th {font-weight:normal}\n"
//				+"			  tr {padding:0px}\n"
//				+"            td {padding:0px}\n"
//				+"            \n"
//				+" 			  .imageButton {color:#000000;background-color:#eeeeee; border:1px solid black; width:150px; padding:2px; font-size:10pt;}\n"
//	            +"			  .durButton {color:#000000; background-color:#eeeeee; border:1px solid black; width:100px; padding:2px; font-size:10pt;}\n"
//	            +"			  .durButtonDisabled {color:#cccccc; background-color:#eeeeee; border:1px solid black; width:100px; padding:2px; font-size:10pt;}\n"
//	            +" 			  .activeImageButton {color:#dd0000; background-color:#ffeeee; border:1px solid black; width:150px; padding:2px; font-size:10pt;}\n"
//	            +"			  .activeDurButton {color:#dd0000; background-color:#ffeeee; border:1px solid black; width:100px; padding:2px; font-size:10pt;}\n"
//	            +"			  .activeDurButtonDisabled {color:#cccccc; background-color:#eeeeee; border:1px solid black; width:100px; padding:2px; font-size:10pt;}\n"
//				+"            .forecast { font-size:14px; border:0px solid gray; border-collapse:collapse; margin:0; text-align:center}\n"
//				+"            .forecastHeader { font-size:16px; border:0px solid gray; border-collapse:collapse; text-align:center; vertical-align:center; font-weight:normal; height:20px;}\n"
//				+"            .forecastValue { font-size:12px; border:0px solid gray; border-collapse:collapse; text-align:center; margin:0; vertical-align:bottom; color:#666666;}\n"
//				+"            .forecastKey { font-size:12px; border:0px solid gray; border-collapse:collapse; text-align:center; margin:0; vertical-align:bottom; padding:0px}\n"
//				+"            .forecastBar { height:"+(int)barHeight+"px; width:" + (int)barWidth + "px; padding-top:1px;}\n"
//				+"            .forecastRow {border:1px solid #dddddd; border-collapse:collapse; padding-top:1px;}\n"
//				+"\n"				
//				+"            .forecastBox {stroke-width:0px; x:"+(int)barPadding+"px; width:"+(int)(barWidth-2*barPadding)+"px}\n"
//				+" 			  .forecastBoxText {text-anchor:middle; fill:#666666;}\n"
//				+" 			  .key {width:30px;height:12px}\n"
//				+"            div {min-width:800px; max-width:800px; border-collapse:collapse}\n"
//				+"            \n"
//				+"        </style>\n"
//				+"\n"
//				+"		<script>\n"
//				+" 			// Script to crop Shakemap constant amount from bottom, accounting for different Shakemap heights\n"
//				+" 			function cropShakemap(){\n"
//				+ "				var imageData = new Image();\n"
//				+" 				imageData.src = document.getElementById('shakemap').src;\n"
//		        +" 				var shakemapHeight = imageData.height;\n"
//		        +" 				var shakemapWidth = imageData.width;\n"
//		        +" 				var shakemapCropHeight = shakemapHeight * (250/shakemapWidth) - 63;\n"
//		        +" 				document.getElementById('shakemapCrop').style.height = shakemapCropHeight + \"px\";\n"
//		        +" 			}\n"
//		        +"\n"
//		        +"		</script>\n"
//				+"\n"
//				+"	    <title>Aftershock Advisory</title>\n"
//				+"    </head>\n\n");
//
//		infoString.append("    <body onload=\"showTable()\">\n"
//				+"        <div>\n"
//				+"            <table style=\"text-align:center\">\n"
//				+"                <tr>\n"
//				+"                    <td style=\"width:150px\">\n"  
//				+"						<img id=\"logo\" src=\"Logo.png\" alt=\"\" style=\"max-height:60px;max-width:150px\">\n"
//				+"  				  </td>\n"
//				+"                    <td style=\"width:500px;height:60px\"><h1 >Aftershock Advisory and Forecast</h1></td>\n"
//				+"					  </td>\n"
//				+"                    <td style=\"width:150px\">\n"  
//				+"						<img id=\"logo\" src=\"Logo.png\" alt=\"\" style=\"max-height:60px;max-width:150px\">\n"
//				+"  				  </td>\n"
//				+"                </tr>\n"
//				+"            </table>\n"
//				+"            <br>\n"
//				+"            <table>\n"
//				+"                <tr>\n"
//				+"					  <!-- Mainshock and forecast information -->\n"	
//				+"                    <td style=\"width:400px\"><h2 style=\"text-align:left\">" + tags.get("MS_MAG") + " eventID:" + tags.get("MS_NAME") + "</h2></td>\n"
//				+"                    <td style=\"width:400px\"><h3 style=\"text-align:right\">Forecast Generated: " + tags.get("V_DATE") + " UTC</h3></td>\n"
//				+"                </tr>\n"
//				+"            </table>\n"
//				+"            <table>\n"
//				+"                <tr>\n"
//				+"					  <!-- Mainshock origin information -->\n"
//				+"                    <td style=\"margin-left:0px;width:300px\"><h3>Origin: " + tags.get("MS_DATETIME") + " UTC</h3></td>\n"
//				+"                    <td style=\"text-align:center;margin:auto;width:300px\"><h3>Location: " + tags.get("MS_LAT") + " " + tags.get("MS_LON") + "</h3></td>\n"
//				+"                    <td style=\"text-align:right;margin-right:0px;width:200px\"><h3>Depth: " + tags.get("MS_DEPTH") + "</h3></td>\n"
//				+"                </tr>\n"
//				+"            </table>\n"
//				+"        </div>\n"
//				+"        <div>\n"
//				+"			  <!-- Descriptive forecast text -->\n"
//				+"            <p style=\"text-align:justify\">" + tags.get("DESCRIPTIVE_TEXT") + "</p>\n"
//				+"        </div>\n\n");
//
//		probTableString.append("        <div style=\"width:800px;text-align:center;\">\n"
//				+"            <p><h2>Anticipated aftershock activity</h2>\n"
//				+"            <p style=\"margin-top:0px\">Forecast start date: " + tags.get("F_START_ABS") + " UTC</p>\n"
//				+"        </div>\n"
//				+"\n"
//				+"		  <!-- Forecast table with probabilities of different magnitudes -->\n"
////				+"        <table style=\"width:650px;margin-left:60px;margin-right:100px\"><!-- Table of Probabilities -->\n"
//				+"        <table style=\"width:700px;margin-left:25px;margin-right:50px\"><!-- Table of Probabilities -->\n"
//				+"            <tr>\n"
//			    +"				  <td>\n"
//                +"					<p style=\"text-align:center;font-size:14px\">\n"
//                +"					<br>\n"
//                +" 					Over the next:\n"
//                +" 					</p>\n"
//                +"	      		  </td>\n"
//				+"                <td>\n"
//				+"                    <table>\n"
//				+"                        <tr>\n"
//				+"                            <th></th>\n"
//				+"                            <th class=\"forecast\">Chance of at least one aftershock larger than:</th>\n"
//				+"                        </tr>\n"
//				+"                        <tr>\n"
//				+"                            <td>\n"
//				+"                                <table>\n"
//				+"                                    <tr><th class=\"forecastHeader\" style=\"width:60px\"></th> </tr>\n"
//		);
////		if (predictionIntervals.length < 4) //code is set up to issue one day bar graph only if the forecast max interval is set to one month. Commenting out for Puerto Rico Earthquake  
//			probTableString.append("                                     <tr><th class=\"forecastBar\">Day</th></tr>\n");
//		if (predictionIntervals.length > 1)	
//			probTableString.append("                                     <tr><th class=\"forecastBar\">Week</th></tr>\n");
//		if (predictionIntervals.length > 2)
//			probTableString.append("                                    <tr><th class=\"forecastBar\">Month</th></tr>\n");
//		if (predictionIntervals.length > 3)
//			probTableString.append("                                    <tr><th class=\"forecastBar\">Year</th></tr>\n");
//		probTableString.append("                                </table>\n"
//				+"                            </td>\n"
//				+"                            <td>\n"
//				+"                                <table class=\"forecast\">\n"
//				+"                                    <tr>\n"
//				+"                                        <th class=\"forecastHeader\">M3</th>\n"
//				+"                                        <th class=\"forecastHeader\">M4</th>\n"
//				+"                                        <th class=\"forecastHeader\">M5</th>\n"
//				+"                                        <th class=\"forecastHeader\">M6</th>\n"
//				+"                                        <th class=\"forecastHeader\">M7</th>\n"
//				+"                                        <th class=\"forecastHeader\">M8</th>\n"
////				+"                                        <th class=\"forecastHeader\">M9</th>\n"
//				+"                                    </tr>\n"
//				+"                                    \n");
//
//		
//		
//		//generate forecast table
//		int maxRow = predictionIntervals.length;
//		int minRow = 0;
////		if (predictionIntervals.length == 4) //commented out for PR earthquake
////				minRow = 1;
//		
//		for (int j = minRow; j<maxRow; j++){
//			probTableString.append(""
//					+"                                    <tr class = \"forecastRow\">\n");
//
//			for (int i = 0; i<MMIcolors.length; i++){
//				double probVal = probability[i][j];
//				double height = barHeight*probVal;
//				String probStr = tags.get(tableTags[j][i]); 
//				double yVal;
//				if (probVal > 0.50) yVal = 11 + barHeight*(1 - probVal);
//				else yVal = barHeight*(1 - probVal) - 3;
//
//				probTableString.append(""
//						+"                                        <td class=\"forecastValue\">\n"
//						+"												<svg class=\"forecastBar\">\n"
//						+"													<rect class = \"forecastBox\" y=\"" + (int) (barHeight - (int) height) + "px\" height=\"" + ((int) height) + "px\" width=\"" + barWidth + "px\" fill=\""+MMIcolors[i]+"\" />\n"
//						+"													<text class = \"forecastBoxText\" x=\"25px\" y=" + String.format("\"%.0fpx\"", yVal) + ">"+probStr+"</text>\n"
//						+"												</svg>\n"
//						+"                                        </td>\n"
//						);
//			}
//			probTableString.append(""
//					+"                                    </tr>\n");
//		}
//		
//		probTableString.append(""
//				+"                                </table>\n"
//				+"                            </td>\n"
//				+"                        </tr>\n"
//				+"                    </table>\n"
//				+"                </td>\n\n");
//		
//		keyString.append("               <td style=\"vertical-align:top\">\n"
//				+"                    <!-- MMI key-->\n"
//				+"                    <table>\n"
//				+"                        <tr><td style=\"height:23px\"></td></tr>\n"
//				+"                        <tr><td class=\"forecast\">Key to colors*</td></tr>\n"
//				+"                        <tr><td>\n"
//				+"                            <table class=\"forecastKey\" style=\"border:1px solid #dddddd;\">\n"
//				+"                                <tr style=\"font-weight:bold\">\n"
//				+"                                    <th style=\"width:50px; font-weight:bold\"></th>\n"
////				+"                                    <th style=\"width:70px; font-weight:bold\">peak MMI</th>\n"
//				+"                                    <th style=\"width:145px; font-weight:bold\">Potential Shaking</th>\n"
//				+"                                    <th style=\"width:145px; font-weight:bold\">Potential Damage</th>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[0] + "\"/></svg></td>\n"
////				+"                                    <td>II-IV</td>\n"
//				+"                                    <td>weak - light</td>\n"
//				+"                                    <td>none</td>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[1] + "\"/></svg></td>\n"
////				+"                                    <td>III-V</td>\n"
//				+"                                    <td>weak - moderate</td>\n"
//				+"                                    <td>very light</td>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[2] + "\"/></svg></td>\n"
////				+"                                    <td>V-VII</td>\n"
//				+"                                    <td>moderate - strong</td>\n"
//				+"                                    <td>light - moderate</td>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[3] + "\"/></svg></td>\n"
////				+"                                    <td>VI-VIII</td>\n"
//				+"                                    <td>strong - severe</td>\n"
//				+"                                    <td>moderate - heavy</td>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[4] + "\"/></svg></td>\n"
////				+"                                    <td>VIII-X</td>\n"
//				+"                                    <td>severe - violent</td>\n"
//				+"                                    <td>heavy</td>\n"
//				+"                                </tr>\n"
//				+"                                <tr>\n"
//				+"                                    <td><svg class=\"key\"><rect class=\"key\" width=\"30px\" height=\"12px\" fill=\"" + MMIcolors[5] + "\"/></svg></td>\n"
////				+"                                    <td>X+</td>\n"
//				+"                                    <td>violent - extreme</td>\n"
//				+"                                    <td>very heavy</td>\n"
//				+"                                </tr>\n"
//				+"                            </table>\n"
//				+"                        </td></tr>\n"
//				+"                    </table>\n"
//				+"						<p class=\"forecastKey\" style=\"font-size:10px\">*This table gives typical peak shaking and intensity levels associated with the forecast magnitudes. Actual shaking is affected by many factors, and damage may be higher in vulnerable structures.</p>\n"
//				+"                </td>\n"
//				+"            </tr>\n"
//				+"        </table>\n\n");
//
//		// set up javascript to select between different image products. Durations and styles. 
//		imgString.append(""
//				+" 		<br>\n"
//				+"		<div style=\"width:800px;height:500px;margin-left:0px;\">\n"
//		);
//		
//		
//		imgString.append(" "
//				+"  	<!-- Mainshock shakemap -->\n"
//				+"		<table style=\"width:800px;vertical-align:top;text-align:center;\">\n"
//				+"	    	<tr>\n"
//				+"	        	<td style=\"width:250px;display:inline\">\n"
//				+"			    	<br>\n"
//				+" 					<p class=\"forecast\" style=\"white-space:pre\">Mainshock ShakeMap\n(previous shaking)</p>\n"
//				+" 					<div style=\"height:230px;overflow:hidden;margin: 0 -275px 0px -275px;\" id=\"shakemapCrop\">\n"
//				+" 						<!-- Change the link URL to point to your local event summary if preferred. Default is to go to USGS summary -->\n"
//				+" 						<a href=\"" + eventURL + "\">\n");
//		if (shakeMapURL != null)
//			imgString.append(" "
//					+"	        			<img style=\"width:250px\" src=\"" + shakeMapURL + "\" alt=\"Mainshock shakemap\" id=\"shakemap\" onload=\"cropShakemap()\">\n"
//					+" 						</a>\n");
//		else 	
//			imgString.append(" "
//					+"	        			<p>EventPage"
//					+" 						</a>\n");
//		imgString.append(" "
//				+" 					</div>\n"
//				+"		 	   </td>\n"
//				+"			    <td style=\"width:550px\" id=\"imageBox\">\n"
//				+"	 				<!-- To manually specifiy the image you want to see displayed, replace src=\"...\" with the desired filename. -->\n"
//				+"	          	<img style=\"margin:auto;width:550px;max-height:480px\" src=\"ratemap.png\" alt=\"Graphical Forecast\" id=\"theimage\">\n"
//				+"	      	</td>\n"
//				+"		    </tr>\n"
//				+"		</table>\n"
//				);
//
//
//		imgString.append(""
//			
//		);
//		
//		imgString.append(""
//				+"		</div>\n"
//				+" 		<div class=\"forecast\">\n"
//				+"			<!-- This would be a good place for a link to an online source of the forecast, if applicable -->\n"
//				+"			This forecast will be updated as new information becomes available.\n"
//				+"		</div>\n"
//				+" 		<br>\n"
//				+"\n"
//				+"  <div style=\"margin-left:0px;width:800px;text-align:center\">\n"
//				+"		<!-- Set up buttons for changing which image type is displayed -->\n"
//				+" 		<input type=\"button\" class=\"imageButton\" value=\"Table\" id=\"imageButton0\" onClick=\"showTable();\">\n"
//				+"		<input type=\"button\" class=\"activeImageButton\" value=\"Shaking map\" id=\"imageButton1\" onClick=\"changeImage('1');\">\n"
//				+"		<input type=\"button\" class=\"imageButton\" value=\"Magnitude Distribution\" id=\"imageButton2\" onClick=\"changeImage('2');\">\n"
//				+"		<input type=\"button\" class=\"imageButton\" value=\"Number with time\" id=\"imageButton3\" onClick=\"changeImage('3');\">\n"
//				+"		<input type=\"button\" class=\"imageButton\" value=\"Rate map\" id=\"imageButton4\" onClick=\"changeImage('4');\">\n"
//				+"  </div>\n"
//				+"\n"
//				+"  <div style=\"margin-left:0px;width:800px;text-align:center\">\n"
//				+"  	<!-- Set up buttons for changing the image duration -->\n"
//				+"		<input type=\"button\" class=\"durButtonDisabled\" value=\"Day\" id=\"durationButton1\" onClick=\"changeDuration('1');\">\n"
//				+"		<input type=\"button\" class=\"activeDurButtonDisabled\" value=\"Week\" id=\"durationButton2\" onClick=\"changeDuration('2');\">\n"
//				+"		<input type=\"button\" class=\"durButtonDisabled\" value=\"Month\" id=\"durationButton3\" onClick=\"changeDuration('3');\">\n"
//				+"		<input type=\"button\" class=\"durButtonDisabled\" value=\"Year\" id=\"durationButton4\" onClick=\"changeDuration('4');\">\n"
//				+"  </div>\n"
//				+"\n"
//				+"	<script>\n"
//				+"		function showTable(){\n"
//				+"			document.getElementById('imageBox').innerHTML = '<iframe style=\"width:550px;height:480px;border:none\" src=\"Table.html\"></iframe>';\n"
//				+"			for (i = 1; i <= 4; i++){\n"
//				+"				document.getElementById('imageButton' + i).className = 'imageButton';\n"
//				+"			}\n"
//				+"			document.getElementById('imageButton0').className = 'activeImageButton';\n"
//				+"\n"
//				+"			for (i = 1; i <= " + (predictionIntervals.length) + "; i++){\n"
//                +"			    if (document.getElementById('durationButton' + i).className == 'durButton')\n"
//                +"			        document.getElementById('durationButton' + i).className = 'durButtonDisabled';\n"
//                +"			    if (document.getElementById('durationButton' + i).className == 'activeDurButton')\n"
//                +"			        document.getElementById('durationButton' + i).className = 'activeDurButtonDisabled';\n"
//                +"				}\n"
//				+"		}\n"
//				+"\n"
//				+" 		// Script for changing the image in response to button click\n"
//				+"		function changeImage(buttonNumber){\n"
//				+" 		// first make sure we've got an image\n"
//		        +"    	document.getElementById('imageBox').innerHTML = '<img style=\"margin:auto;width:550px;max-height:480px\" src=\"ratemap.png\" alt=\"Graphical Forecast\" id=\"theimage\">';\n"		            
//		        +"\n"
//		        +" 		// now decide which image\n"
//				+"			durationIndex = 1;\n"
//				+"			for (i = 1; i <= " + predictionIntervals.length + " ; i++){\n"
//				+"				if (document.getElementById('durationButton' + i).className.includes('active')){\n"
//				+"	 				var durationIndex = i;\n"
//				+"		   		}\n"
//				+"			}\n"
//				+"			console.log(durationIndex);\n"
//				+"\n"
//				+"			var dur;\n"
//				+"			switch (durationIndex){\n"
//				+"				case 1:\n"
//				+"			    	dur = 'day';\n"
//				+"					break;\n"
//				+"				case 2:\n"
//				+"					dur = 'week';\n"
//				+"					break;\n"
//				+"				case 3:\n"
//				+"					dur = 'month';\n"
//				+"					break;\n"
//				+"				case 4:\n"
//				+"					dur = 'year';\n"
//				+"					break;\n"
//				+"				default:\n"
//				+"					dur = '';\n"
//				+"					break;\n"
//				+"			}\n"
//				+"\n"
//				+"			var imgName = document.getElementById('theimage').src;\n"
//				+"\n"
//				+"			switch (buttonNumber){\n"
//				+"				case '1':\n"
//				+"					var imgName = 'shaking';\n"
//				+"				    var durNeeded = true;\n"
//				+"					break;\n"
//				+"				case '2':\n"
//				+"					var imgName = 'number';\n"
//				+"		            var durNeeded = true;\n"
//				+"	 				break;\n"
//				+"				case '3':\n"
//				+"					var imgName = 'forecastCmlNum';\n"
//				+"	        	    var durNeeded = false;\n"
//				+"					break;\n"
//				+"				case '4':\n"
//				+"					var imgName = 'ratemap';\n"
//				+"	        	    var durNeeded = false;\n"
//				+"					break;\n"
//				+"			}\n"
//				+"\n"
//				+"			if (durNeeded) imgName = imgName + dur;\n"
//				+"\n"
//				+"			var image = document.getElementById('theimage');\n"
//				+"			image.src = imgName + '.png';\n"
//				+"\n"
//				+"			for (i = 0; i < 5; i++){\n"
//				+"				document.getElementById('imageButton' + i).className = 'imageButton';\n"
//				+"			}\n"
//				+"			document.getElementById('imageButton' + buttonNumber).className = 'activeImageButton';\n"
//				+"\n"
//				+"			if (durNeeded){\n"
//				+"				for (i = 1; i <= " + predictionIntervals.length + "; i++){\n"
//                +"			    	if (document.getElementById('durationButton' + i).className == 'durButtonDisabled')\n"
//                +"			    	    document.getElementById('durationButton' + i).className = 'durButton';\n"
//                +"			    	if (document.getElementById('durationButton' + i).className == 'activeDurButtonDisabled')\n"
//                +"			    	    document.getElementById('durationButton' + i).className = 'activeDurButton';\n"
//                +"				}\n"
//                +"			} else {\n"
//                +"				for (i = 1; i <= " + predictionIntervals.length + "; i++){\n"
//                +"				    if (document.getElementById('durationButton' + i).className == 'durButton')\n"
//                +"				        document.getElementById('durationButton' + i).className = 'durButtonDisabled';\n"
//                +"				    if (document.getElementById('durationButton' + i).className == 'activeDurButton')\n"
//                +"				        document.getElementById('durationButton' + i).className = 'activeDurButtonDisabled';\n"
//                +"				}\n"
//                +"		 	}\n"
//				+"		}\n"
//				+"\n"
//				+"		function changeDuration(buttonNumber){\n"
//				+"			if(document.getElementById('imageButton0').className == 'imageButton' && buttonNumber <= " + predictionIntervals.length + "){\n"
//				+"				var imgName = document.getElementById('theimage').src;\n"
//				+"\n"
//				+"	        	if (imgName.includes('number')) {\n"
//				+"	    			var imgtype = 'number';\n"
//				+"	          		durNeeded = true;\n"
//				+"				} else if (imgName.includes('forecastCmlNum')){\n"
//				+"					var imgtype = 'forecastCmlNum';\n"
//				+"					durNeeded = false;\n"
//				+"				} else if (imgName.includes('rate')){\n"
//				+"					var imgtype = 'ratemap';\n"
//				+"					durNeeded = false;\n"
//				+"				} else if (imgName.includes('shaking')){\n"
//				+"					var imgtype = 'shaking';\n"
//				+"					durNeeded = true;\n"
//				+"				} else {\n"
//				+"					var imgtype = 'ImageNotRecognized';\n"
//				+"					durNeeded = false;\n"
//				+"				}\n"
//				+"\n"
//				+"				if (durNeeded) {\n"
//				+"					switch (buttonNumber){\n"
//				+"				    	case '1':\n"
//				+"	    					imgName = imgtype+'day';\n"
//				+"	 						break;\n"
//				+"						case '2':\n"
//				+"							imgName = imgtype+'week';\n"
//				+"							break;\n"
//				+"						case '3':\n"
//				+"							imgName = imgtype+'month';\n"
//				+"							break;\n"
//				+"						case '4':\n"
//				+"							imgName = imgtype+'year';\n"
//				+"							break;\n"
//				+"			    	}\n"
//				+"				} else {\n"
//				+"					imgName = imgtype;\n"
//				+"				}\n"
//				+"\n"
//				+"				var image = document.getElementById('theimage');\n"
//				+"				image.src = imgName + '.png';\n"
//				+"\n"
//				+"				if (durNeeded){\n"
//				+"        			for (i = 1; i <= " + predictionIntervals.length + "; i++){\n"
//		        +"            			document.getElementById('durationButton' + i).className = 'durButton';\n"
//		        +"        			}\n"
//		        +"        			document.getElementById('durationButton' + buttonNumber).className = 'activeDurButton';\n"
//		        +"    			} else {\n"
//		        +"       			for (i = 1; i <= " + predictionIntervals.length + "; i++){\n"
//		        +"            			document.getElementById('durationButton' + i).className = 'durButtonDisabled';\n"
//		        +"        			}\n"
//		        +"        			document.getElementById('durationButton' + buttonNumber).className = 'activeDurButtonDisabled';\n"
//		        +"    			}\n"
//		        +"			}\n"
//				+"		}\n"
//				+"	</script>\n"
//				);
//
//		imgString.append(""
//				+" 	</body>\n"
//				+"</html>\n");
//
//		outputString.append(headString);
//		outputString.append(infoString);
//		outputString.append(probTableString);
//		outputString.append(keyString);
//		outputString.append(imgString);
//		
//		// write file
//		FileWriter fw;
//		try {
//			fw = new FileWriter(outputFile, false);
//		} catch (IOException e1) {
//			//				e1.printStackTrace();
//			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
//			return;
//		}
//
//		try {
//			fw.append(outputString);
//		} catch (IOException e) {
//			//					e.printStackTrace();
//			System.err.println("Couldn't save to file " + outputFile.getAbsolutePath());
//		}
//		
//		try {
//			fw.close();
//		} catch (IOException e) {
//			//				e.printStackTrace();
//			System.err.println("Problem closing file.");
//		}
//	}

	public void setShakeMapURL(String shakeMapURL) {
		this.shakeMapURL = shakeMapURL;
	}

	private static double unwrap(double lon){
		if (lon > 180)
			lon -= 360;
		return lon;
	}
	
	
	public static void main(String[] args){
		GraphicalForecast gf = new GraphicalForecast();
		gf.processForecastStrings();
		gf.assignForecastStrings();
		gf.setShakeMapURL("https://earthquake.usgs.gov/archive/product/shakemap/atlas20100404224043/atlas/1520888708106/download/intensity.jpg");
		try{

			gf.writeHTML(new File(System.getenv("HOME") + "/example_forecast.html"));
			gf.writeHTMLTable(new File(System.getenv("HOME") + "/Table.html"));
			gf.writeBarGraphHTML(new File(System.getenv("HOME") + "/graphical_forecast.html"));
			gf.writeCSS(new File(System.getenv("HOME") + "/BHAforecast.css"));
//			gf.writeSummaryJson(new File(System.getenv("HOME") + "/forecast.json"));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

//	private static String logoUSGSxml(){
//		return new String(""
//				+ "<svg xmlns=\"http://www.w3.org/2000/svg\" xml:space=\"preserve\" height=\"60\" width=\"150\" version=\"1.1\" overflow=\"visible\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 500 200\" enable-background=\"new 0 0 500 200\">\n"
//				+ "<path id=\"USGS\" d=\"m234.95 15.44v85.037c0 17.938-10.132 36.871-40.691 36.871-27.569 0-40.859-14.281-40.859-36.871v-85.04h25.08v83.377c0 14.783 6.311 20.593 15.447 20.593 10.959 0 15.943-7.307 15.943-20.593v-83.377h25.08m40.79 121.91c-31.058 0-36.871-18.27-35.542-39.03h25.078c0 11.462 0.5 21.092 14.283 21.092 8.471 0 12.619-5.482 12.619-13.618 0-21.592-50.486-22.922-50.486-58.631 0-18.769 8.968-33.715 39.525-33.715 24.42 0 36.543 10.963 34.883 36.043h-24.419c0-8.974-1.491-18.106-11.627-18.106-8.136 0-12.952 4.486-12.952 12.787 0 22.757 50.492 20.763 50.492 58.465 0 31.06-22.75 34.72-41.85 34.72m168.6 0c-31.06 0-36.871-18.27-35.539-39.03h25.075c0 11.462 0.502 21.092 14.285 21.092 8.475 0 12.625-5.482 12.625-13.618 0-21.592-50.494-22.922-50.494-58.631 0-18.769 8.969-33.715 39.531-33.715 24.412 0 36.536 10.963 34.875 36.043h-24.412c0-8.974-1.494-18.106-11.625-18.106-8.145 0-12.955 4.486-12.955 12.787 0 22.757 50.486 20.763 50.486 58.465 0 31.06-22.75 34.72-41.85 34.72m-79.89-46.684h14.76v26.461l-1.229 0.454c-3.815 1.332-8.301 2.327-12.452 2.327-14.287 0-17.943-6.645-17.943-44.177 0-23.256 0-44.348 15.615-44.348 12.146 0 14.711 8.198 14.933 18.107h24.981c0.197-23.271-14.789-36.043-38.42-36.043-41.021 0-42.521 30.724-42.521 60.954 0 45.507 4.938 63.167 47.12 63.167 9.784 0 25.36-2.211 32.554-4.18 0.437-0.115 1.212-0.597 1.212-1.217v-59.598h-38.611v18.09\" fill=\"#007150\"/>\n"
//				+ "<path id=\"waves\" d=\"m48.736 55.595l0.419 0.403c11.752 9.844 24.431 8.886 34.092 2.464 6.088-4.049 33.633-22.367 49.202-32.718v-10.344h-116.03v27.309c7.071-1.224 18.47-0.022 32.316 12.886m43.651 45.425l-13.705-13.142c-1.926-1.753-3.571-3.04-3.927-3.313-11.204-7.867-21.646-5.476-26.149-3.802-1.362 0.544-2.665 1.287-3.586 1.869l-28.602 19.13v34.666h116.03v-24.95c-2.55 1.62-18.27 10.12-40.063-10.46m-44.677-42.322c-0.619-0.578-1.304-1.194-1.915-1.698-13.702-10.6-26.646-5.409-29.376-4.116v11.931l6.714-4.523s10.346-7.674 26.446 0.195l-1.869-1.789m16.028 15.409c-0.603-0.534-1.214-1.083-1.823-1.664-12.157-10.285-23.908-7.67-28.781-5.864-1.382 0.554-2.7 1.303-3.629 1.887l-13.086 8.754v12.288l21.888-14.748s10.228-7.589 26.166 0.054l-0.735-0.707m68.722 12.865c-4.563 3.078-9.203 6.203-11.048 7.441-4.128 2.765-13.678 9.614-29.577 2.015l1.869 1.797c0.699 0.63 1.554 1.362 2.481 2.077 11.418 8.53 23.62 7.304 32.769 1.243 1.267-0.838 2.424-1.609 3.507-2.334v-12.234m0-24.61c-10.02 6.738-23.546 15.833-26.085 17.536-4.127 2.765-13.82 9.708-29.379 2.273l1.804 1.729c0.205 0.19 0.409 0.375 0.612 0.571l-0.01 0.01 0.01-0.01c12.079 10.22 25.379 8.657 34.501 2.563 5.146-3.436 12.461-8.38 18.548-12.507l-0.01-12.165m0-24.481c-14.452 9.682-38.162 25.568-41.031 27.493-4.162 2.789-13.974 9.836-29.335 2.5l1.864 1.796c1.111 1.004 2.605 2.259 4.192 3.295 10.632 6.792 21.759 5.591 30.817-0.455 6.512-4.351 22.528-14.998 33.493-22.285v-12.344\" fill=\"#007150\"/>\n"
//				+ "<path id=\"tagline\" d=\"m22.329 172.13c-0.247 0.962-0.401 1.888-0.251 2.554 0.195 0.68 0.749 1.011 1.923 1.011 1.171 0 2.341-0.757 2.642-2.183 0.954-4.479-9.653-3.479-8.218-10.224 0.972-4.567 5.792-5.954 9.607-5.954 4.022 0 7.257 1.928 5.951 6.495h-5.783c0.312-1.466 0.33-2.347-0.007-2.722-0.298-0.379-0.783-0.463-1.413-0.463-1.297 0-2.188 0.841-2.492 2.264-0.714 3.354 9.718 3.189 8.271 9.975-0.781 3.688-4.388 6.457-9.29 6.457-5.157 0-8.316-1.306-6.724-7.21h5.784m25.284-6.85c0.667-3.141 0.093-4.188-1.75-4.188-2.513 0-3.193 2.22-4.13 6.619-1.373 6.455-1.124 7.838 1.057 7.838 1.844 0 3.08-1.676 3.667-4.439h5.909c-1.218 5.741-4.847 8.215-10.382 8.215-7.627 0-7.645-4.654-6.234-11.273 1.229-5.784 3.119-10.729 10.915-10.729 5.447 0 8.033 2.433 6.856 7.964h-5.908m18.389-15.69l-0.989 4.651h-5.909l0.989-4.651h5.909m-6.233 29.31h-5.909l4.501-21.165h5.909l-4.501 21.16zm282.77-29.31l-0.991 4.651h-5.911l0.99-4.651h5.91m-6.23 29.31h-5.906l4.496-21.165h5.911l-4.5 21.16zm-259.03-12.95c0.438-2.052 1.144-4.984-1.664-4.984-2.727 0-3.36 3.186-3.743 4.984h5.407m-6.111 3.31c-0.533 2.516-1.251 6.284 1.345 6.284 2.097 0 2.945-2.012 3.318-3.771h5.992c-0.574 2.306-1.728 4.192-3.429 5.489-1.66 1.298-3.916 2.055-6.681 2.055-7.63 0-7.645-4.654-6.239-11.273 1.229-5.784 3.12-10.729 10.915-10.729 7.965 0 7.75 5.152 6.097 11.944h-11.318zm22.462-9.38h0.083c1.575-1.886 3.31-2.557 5.534-2.557 2.808 0 4.923 1.676 4.3 4.607l-3.608 16.978h-5.909l3.099-14.584c0.401-1.887 0.38-3.353-1.507-3.353-1.886 0-2.536 1.468-2.936 3.353l-3.098 14.584h-5.909l4.5-21.165h5.905l-0.452 2.14m23.465 5.4c0.667-3.141 0.093-4.188-1.751-4.188-2.512 0-3.194 2.22-4.131 6.619-1.373 6.455-1.122 7.838 1.058 7.838 1.843 0 3.079-1.676 3.668-4.439h5.909c-1.222 5.741-4.846 8.215-10.382 8.215-7.627 0-7.644-4.654-6.235-11.273 1.229-5.784 3.116-10.729 10.912-10.729 5.45 0 8.037 2.433 6.86 7.964h-5.908m19.61 0.67c0.434-2.052 1.145-4.984-1.664-4.984-2.725 0-3.36 3.186-3.743 4.984h5.4m-6.11 3.31c-0.54 2.516-1.255 6.284 1.344 6.284 2.095 0 2.94-2.012 3.316-3.771h5.992c-0.574 2.306-1.728 4.192-3.432 5.489-1.656 1.298-3.912 2.055-6.68 2.055-7.627 0-7.647-4.654-6.237-11.273 1.231-5.784 3.12-10.729 10.915-10.729 7.961 0 7.747 5.152 6.093 11.944h-11.31zm36.12-15.9c-2.352-0.168-3.051 0.758-3.507 2.896l-0.42 1.481h2.77l-0.775 3.646h-2.768l-3.723 17.521h-5.909l3.722-17.521h-2.638l0.774-3.646h2.682c1.188-5.292 2.251-8.231 8.516-8.231 0.713 0 1.376 0.041 2.08 0.082l-0.79 3.77m9.56 14.35c0.937-4.399 1.198-6.619-1.317-6.619-2.512 0-3.196 2.22-4.13 6.619-1.373 6.455-1.122 7.838 1.057 7.838 2.17 0 3.01-1.38 4.39-7.84m-11.43 0.34c1.229-5.784 3.117-10.729 10.912-10.729s7.586 4.945 6.355 10.729c-1.409 6.619-3.403 11.274-11.032 11.274-7.63 0-7.65-4.65-6.24-11.27zm27.86-10.31l-0.577 2.723h0.082c1.607-2.431 3.77-3.143 6.162-3.143l-1.122 5.279c-5.129-0.335-5.854 2.682-6.298 4.779l-2.448 11.524h-5.909l4.496-21.165h5.62m17.83 14.58c-0.32 1.51-0.464 3.354 1.465 3.354 3.479 0 3.935-4.693 4.421-7-2.95 0.13-5.08-0.12-5.88 3.65m10.34 2.64c-0.281 1.305-0.395 2.645-0.546 3.941h-5.491l0.345-2.808h-0.082c-1.721 2.18-3.664 3.229-6.223 3.229-4.105 0-4.961-3.06-4.181-6.748 1.488-7 6.958-7.295 12.43-7.206l0.347-1.638c0.385-1.804 0.41-3.104-1.729-3.104-2.054 0-2.549 1.551-2.908 3.229h-5.784c0.545-2.558 1.69-4.19 3.278-5.151 1.553-1.011 3.561-1.388 5.827-1.388 7.5 0 7.777 3.229 6.959 7.084l-2.25 10.57zm23.39-9.68c0.667-3.141 0.093-4.188-1.749-4.188-2.515 0-3.196 2.22-4.132 6.619-1.373 6.455-1.122 7.838 1.059 7.838 1.842 0 3.08-1.676 3.668-4.439h5.909c-1.221 5.741-4.848 8.215-10.382 8.215-7.627 0-7.642-4.654-6.237-11.273 1.232-5.784 3.121-10.729 10.916-10.729 5.447 0 8.034 2.433 6.857 7.964h-5.909m32.45 7.04c-0.322 1.51-0.463 3.354 1.465 3.354 3.479 0 3.936-4.693 4.42-7-2.96 0.13-5.09-0.12-5.89 3.65m10.34 2.64c-0.281 1.305-0.396 2.645-0.547 3.941h-5.49l0.344-2.808h-0.081c-1.72 2.18-3.659 3.229-6.226 3.229-4.104 0-4.961-3.06-4.18-6.748 1.487-7 6.957-7.295 12.432-7.206l0.351-1.638c0.378-1.804 0.405-3.104-1.733-3.104-2.057 0-2.549 1.551-2.909 3.229h-5.781c0.543-2.558 1.69-4.19 3.276-5.151 1.561-1.011 3.563-1.388 5.828-1.388 7.5 0 7.776 3.229 6.957 7.084l-2.24 10.57zm12.97-15.08h0.08c1.58-1.886 3.311-2.557 5.533-2.557 2.805 0 4.924 1.676 4.297 4.607l-3.608 16.978h-5.905l3.101-14.584c0.397-1.887 0.377-3.353-1.507-3.353-1.889 0-2.534 1.468-2.936 3.353l-3.104 14.584h-5.912l4.505-21.165h5.911l-0.45 2.14m18.63 15.08c2.141 0 2.903-2.22 3.854-6.703 0.986-4.652 1.342-7.291-0.843-7.291-2.22 0-2.926 1.549-4.3 8.004-0.42 1.97-1.56 5.99 1.29 5.99m12-17.22l-4.686 22.045c-0.313 1.465-1.461 7.25-9.59 7.25-4.398 0-7.937-1.129-6.965-6.285h5.785c-0.187 0.883-0.222 1.637 0.048 2.137 0.265 0.547 0.87 0.841 1.794 0.841 1.469 0 2.473-1.388 2.93-3.521l0.862-4.063h-0.084c-1.229 1.632-3.082 2.47-5.008 2.47-6.499 0-4.946-5.949-3.928-10.729 0.992-4.65 2.33-10.563 8.49-10.563 2.096 0 3.704 0.92 4.12 2.893h0.085l0.525-2.473h5.616v-0.002h0.03zm19.78 2.14h0.092c1.572-1.886 3.306-2.557 5.525-2.557 2.809 0 4.924 1.676 4.301 4.607l-3.606 16.978h-5.912l3.104-14.584c0.397-1.887 0.377-3.353-1.51-3.353-1.889 0-2.531 1.468-2.932 3.353l-3.104 14.584h-5.91l4.5-21.165h5.91l-0.45 2.14m18.63 15.08c2.137 0 2.901-2.22 3.854-6.703 0.992-4.652 1.344-7.291-0.836-7.291-2.222 0-2.928 1.549-4.301 8.004-0.41 1.97-1.56 5.99 1.29 5.99m12-17.22l-4.687 22.045c-0.313 1.465-1.454 7.25-9.593 7.25-4.398 0-7.931-1.129-6.957-6.285h5.785c-0.191 0.883-0.226 1.637 0.043 2.137 0.264 0.547 0.874 0.841 1.791 0.841 1.471 0 2.474-1.388 2.929-3.521l0.863-4.063h-0.079c-1.23 1.632-3.086 2.47-5.011 2.47-6.497 0-4.939-5.949-3.928-10.729 0.993-4.65 2.33-10.563 8.496-10.563 2.094 0 3.695 0.92 4.117 2.893h0.085l0.525-2.473h5.615v-0.002h0.02zm9.23 0h5.869l-0.482 15.926h0.079l7.044-15.926h6.278l0.08 15.926h0.083l6.438-15.926h5.666l-10.043 21.165h-6.195l-0.608-14.039h-0.081l-7.083 14.039h-6.281l-0.78-21.16m41.29 9.97c0.937-4.399 1.196-6.619-1.313-6.619-2.521 0-3.203 2.22-4.133 6.619-1.373 6.455-1.121 7.838 1.059 7.838 2.17 0 3.01-1.38 4.38-7.84m-11.43 0.34c1.226-5.784 3.114-10.729 10.911-10.729 7.796 0 7.586 4.945 6.354 10.729-1.409 6.619-3.401 11.274-11.028 11.274s-7.64-4.65-6.23-11.27zm28.19-10.31l-0.582 2.723h0.086c1.608-2.431 3.771-3.143 6.16-3.143l-1.123 5.279c-5.125-0.335-5.851 2.682-6.297 4.779l-2.449 11.524h-5.906l4.496-21.165h5.61m-182.4-0.42c-2.219 0-3.955 0.671-5.53 2.557h-0.086l2.188-10.285h-5.91l-6.231 29.314h5.91l3.103-14.585c0.4-1.885 1.047-3.353 2.935-3.353s1.906 1.468 1.51 3.353l-3.102 14.585h5.907l3.605-16.976c0.62-2.93-1.49-4.6-4.3-4.6m192.26-7.73l-6.231 29.313h5.912l6.23-29.313h-5.91m15.8 18.54c-1.133 5.324-1.979 7.545-3.992 7.545-2.135 0-2.043-2.221-0.912-7.545 0.9-4.231 1.48-7.166 4.039-7.166 2.43 0 1.76 2.93 0.86 7.16m3.95-18.54l-2.158 10.16h-0.084c-0.834-1.804-2.165-2.433-4.301-2.433-5.953 0-7.187 6.582-8.097 10.856-0.924 4.355-2.578 11.146 3.541 11.146 2.268 0 4.051-0.715 5.574-2.768h0.087l-0.504 2.349h5.62l6.229-29.315h-5.909v-0.01z\" fill=\"#007150\"/>\n"
//				+ "</svg>\n"
//				);
//	}
	
	
}
