package org.opensha.oaf.rj;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
//import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
import org.opensha.oaf.util.JSONOrderedObject;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.aafs.ServerConfig;

import org.opensha.oaf.util.SimpleUtils;


public class USGS_AftershockForecast {
	
	private static final double[] min_mags_default = { 3d, 4d, 5d, 6d, 7d };
	private static final double fractile_lower = 0.025;
	private static final double fractile_upper = 0.975;
	private static final double fractile_median = 0.500;
	private static final double mag_bin_half_width_default = 0.05;

	private static final boolean include_fractile_list = true;

	//  private static final long[] sdround_thresholds = {86400000L, 259200000L, 604800000L};
	//  		// 24 hours, 3 days, 7 days
	//  private static final long[] sdround_snaps = {300000L, 600000L, 1800000L, 3600000L};
	//  		// 5 minutes, 10 minutes, 30 minutes, 60 minutes (should have 1 more element than above)

	private static final long[] sdround_thresholds = {86400000L};
			// 24 hours
	private static final long[] sdround_snaps = {600000L, 3600000L};
			// 10 minutes, 60 minutes (should have 1 more element than above)
	
	public enum Duration {
		ONE_DAY("1 Day", ChronoUnit.DAYS, 1L),
		ONE_WEEK("1 Week", ChronoUnit.DAYS, 7L),
		ONE_MONTH("1 Month", ChronoUnit.DAYS, 30L),
		ONE_YEAR("1 Year", ChronoUnit.DAYS, 365L);
		
		private final String label;
		private final ChronoUnit calendarField;
		private final long calendarAmount;
		
		private Duration(String label, ChronoUnit calendarField, long calendarAmount) {
			this.label = label;
			this.calendarField = calendarField;
			this.calendarAmount = calendarAmount;
		}
		
		public Instant getEndDate (Instant startDate) {
			return startDate.plus (calendarAmount, calendarField);
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum Template {
		MAINSOCK("Mainshock"),
		EQ_OF_INTEREST("Earthquake of Interest"),
		SWARM("Swarm");
		
		private String title;

		private Template(String title) {
			this.title = title;
		}
		
		@Override
		public String toString() {
			return title;
		}
	}

	public static class FractileArray {
		public double[] values;

		// Copy entire array
		public FractileArray (double[] the_values) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOf (the_values, the_values.length);
			}
		}

		// Copy portion of array starting at index lo
		public FractileArray (double[] the_values, int lo) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOfRange (the_values, lo, the_values.length);
			}
		}

		// Copy portion of array from index lo, inclusive, to index hi, exclusive
		public FractileArray (double[] the_values, int lo, int hi) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOfRange (the_values, lo, hi);
			}
		}

		public final boolean has_values () {
			return values != null;
		}

		public final void sort_values () {
			if (values != null) {
				Arrays.sort (values);
			}
			return;
		}

		public final JSONArray as_probabilities () {
			JSONArray result = new JSONArray();
			if (values != null) {
				for (int j = 0; j < values.length; ++j) {
					result.add (Double.parseDouble (String.format (Locale.US, "%.12f", values[j])));
				};
			}
			return result;
		}

		public final JSONArray as_values () {
			JSONArray result = new JSONArray();
			if (values != null) {
				for (int j = 0; j < values.length; ++j) {
					result.add (Math.round (values[j]));
				};
			}
			return result;
		}
	}
	
	private USGS_ForecastModel model;
	
	private int[] aftershockCounts;
	
	private Instant eventDate;
	private Instant startDate;
	private Instant[] endDates;
	
	private Duration[] durations;
	private Duration advisoryDuration; // duration for the advisory paragraph
	private double[] minMags;
	private double[] calcMags;
	private Table<Duration, Double, Double> numEventsLower;
	private Table<Duration, Double, Double> numEventsUpper;
	private Table<Duration, Double, Double> numEventsMedian;
	private Table<Duration, Double, Double> probs;

	private FractileArray fractile_probabilities;
	private Table<Duration, Double, FractileArray> fractile_values;
	
	private boolean includeProbAboveMainshock;
	
	// custom text which can be added
	private String injectableText = null;

	// the time of the next scheduled forecast, or 0L if unknown, or -1L if no more forecasts are scheduled
	private long nextForecastMillis = 0L;

	// Additional parameters to be displayed on the Model tab, or null if none.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.
	private LinkedHashMap<String, Object> userParamMap = null;
	
	private Template template = Template.MAINSOCK;
	
	public USGS_AftershockForecast(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks,
			Instant eventDate, Instant startDate) {
		this(model, aftershocks, min_mags_default, eventDate, startDate, true, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks,
			Instant eventDate, Instant startDate, boolean roundStart) {
		this(model, aftershocks, min_mags_default, eventDate, startDate, roundStart, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate) {
		this(model, aftershocks, minMags, eventDate, startDate, true, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean roundStart) {
		this(model, aftershocks, minMags, eventDate, startDate, roundStart, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean roundStart, boolean includeProbAboveMainshock, double mag_bin_half_width) {
		compute(model, aftershocks, minMags, eventDate, startDate, roundStart, includeProbAboveMainshock, mag_bin_half_width);
	}
	
	private static final DateFormat df = new SimpleDateFormat();
	private static final TimeZone utc = TimeZone.getTimeZone("UTC");
	
	private void compute(USGS_ForecastModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean roundStart, boolean includeProbAboveMainshock, double mag_bin_half_width) {
		Preconditions.checkArgument(minMags.length > 0);

		// Round the start time

		if (roundStart) {
			startDate = sdround (eventDate, startDate);
		}
		
		this.model = model;
		this.minMags = minMags;
		this.eventDate = eventDate;
		this.startDate = startDate;
		this.includeProbAboveMainshock = includeProbAboveMainshock;
				
		boolean f_verbose = AftershockVerbose.get_verbose_mode();
		
		// calculate number of observations for each bin
		aftershockCounts = new int[minMags.length];

//		for (int m=0; m<minMags.length; m++) {
//			for (ObsEqkRupture eq : aftershocks) {
//				if (eq.getMag() >= minMags[m] - mag_bin_half_width) {
//					aftershockCounts[m]++;
//				}
//			}
//		}

		for (int m=0; m<minMags.length; m++) {
			aftershockCounts[m] = 0;
		}
		final long event_time = eventDate.toEpochMilli();
		for (ObsEqkRupture eq : aftershocks) {
			// ignore events that occurred before the mainshock
			if (eq.getOriginTime() >= event_time + 3L) {
				for (int m=0; m<minMags.length; m++) {
					if (eq.getMag() >= minMags[m] - mag_bin_half_width) {
						aftershockCounts[m]++;
					}
				}
			}
		}
		
		numEventsLower = HashBasedTable.create();
		numEventsUpper = HashBasedTable.create();
		numEventsMedian = HashBasedTable.create();
		probs = HashBasedTable.create();
		
		durations = Duration.values();
		advisoryDuration = Duration.ONE_WEEK;
		endDates = new Instant[durations.length];
		
		double[] calcFractiles = {fractile_lower, fractile_upper, fractile_median};
		
		calcMags = minMags;
		if (includeProbAboveMainshock && model.hasMainShockMag()) {
			// also calculate for mainshock mag
			calcMags = Arrays.copyOf(minMags, minMags.length+1);
			calcMags[calcMags.length-1] = model.getMainShockMag();
		}

		// If we will use extended fractile lists, add them to the list of fractiles we need to calculate
		double[] combinedCalcFractiles = calcFractiles;
		fractile_probabilities = null;
		fractile_values = null;
		if (include_fractile_list) {
			double[] the_probabilities = model.getFractileProbabilities();
			if (the_probabilities != null) {
				fractile_probabilities = new FractileArray (the_probabilities);
				fractile_values = HashBasedTable.create();
				// We need fractiles for both our list, and the extended lists
				combinedCalcFractiles = new double[calcFractiles.length + fractile_probabilities.values.length];
				int j = 0;
				for (int i = 0; i < calcFractiles.length; ++i) {
					combinedCalcFractiles[j++] = calcFractiles[i];
				}
				for (int i = 0; i < fractile_probabilities.values.length; ++i) {
					combinedCalcFractiles[j++] = fractile_probabilities.values[i];
				}
			}
		}
		
		df.setTimeZone(utc);
		if (f_verbose) {
			System.out.println("Start date: "+df.format(Date.from(startDate)));
		}
		for (int i=0; i<durations.length; i++) {
			Duration duration = durations[i];
			Instant endDate = duration.getEndDate(startDate);
			if (f_verbose) {
				System.out.println(duration.label+" end date: "+df.format(Date.from(endDate)));
			}
			
			double tMinDays = getDateDelta(eventDate, startDate);
			Preconditions.checkState(tMinDays >= 0d, "tMinDays must be positive: %s", tMinDays);
			double tMaxDays = getDateDelta(eventDate, endDate);
			Preconditions.checkState(tMaxDays > tMinDays,
					"tMaxDays must be greter than tMinDays: %s <= %s", tMaxDays, tMinDays);
			
			endDates[i] = endDate;
			
			for (int m=0; m<calcMags.length; m++) {
				double minMag = calcMags[m];
				
				double[] fractiles = model.getCumNumFractileWithAleatory(combinedCalcFractiles, minMag, tMinDays, tMaxDays);
				
				numEventsLower.put(duration, minMag, fractiles[0]);
				numEventsUpper.put(duration, minMag, fractiles[1]);
				numEventsMedian.put(duration, minMag, fractiles[2]);
//				double rate = model.getModalNumEvents(minMag, tMinDays, tMaxDays);

//				double expectedVal = model.getModalNumEvents(minMag, tMinDays, tMaxDays);
//				double poissonProb = 1 - Math.exp(-expectedVal);
				double poissonProb = model.getProbOneOrMoreEvents(minMag, tMinDays, tMaxDays);

				if (poissonProb < 1.0e-12) {
					poissonProb = 0.0;	// fewer than 4 significant digits available
				} else {
					poissonProb = Double.parseDouble (String.format (Locale.US, "%.3e", poissonProb));	// limit to 4 significant digits
				}

				probs.put(duration, minMag, poissonProb);

				if (fractile_probabilities != null) {
					fractile_values.put (duration, minMag, new FractileArray (fractiles, calcFractiles.length));
				}
			}
		}
	}
	
	public static double getDateDelta(Instant start, Instant end) {
		long diff = end.toEpochMilli() - start.toEpochMilli();
		return ((double)diff)/ComcatOAFAccessor.day_millis;
	}
	
	public void setAdvisoryDuration(Duration advisoryDuration) {
		this.advisoryDuration = advisoryDuration;
	}
	
	public void setInjectableText(String injectableText) {
		this.injectableText = injectableText;
	}
	
	public void setNextForecastMillis(long nextForecastMillis) {
		this.nextForecastMillis = nextForecastMillis;
	}
	
	public void setUserParamMap(Map<String, Object> userParamMap) {
		if (userParamMap == null || userParamMap.isEmpty()) {
			this.userParamMap = null;
		} else {
			this.userParamMap = new LinkedHashMap<String, Object> (userParamMap);
		}
	}
	
	public boolean isIncludeProbAboveMainshock() {
		return includeProbAboveMainshock;
	}

	public void setIncludeProbAboveMainshock(boolean includeProbAboveMainshock) {
		this.includeProbAboveMainshock = includeProbAboveMainshock;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	public Duration getAdvisoryDuration() {
		return advisoryDuration;
	}

	public String getInjectableText() {
		return injectableText;
	}

	public long getNextForecastMillis() {
		return nextForecastMillis;
	}

	private static String[] headers = {"Time Window For Analysis", "Magnitude Range",
			"Most Likely Number Of Aftershocks (95% confidence)", "Probability of one or more aftershocks"};
	
	// In this function, change calcMags to minMags if you don't want to display the M>=mainshock row
	public TableModel getTableModel() {
		final int numEach = calcMags.length+1;
		final int rows = probs.rowKeySet().size()*numEach;
		final int cols = 4;
		return new AbstractTableModel() {

			@Override
			public int getRowCount() {
				return rows;
			}

			@Override
			public int getColumnCount() {
				return cols;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (rowIndex == 0)
					return headers[columnIndex];
				rowIndex -= 1;
				int m = rowIndex % numEach;
				int d = rowIndex / numEach;
				
				if (columnIndex == 0) {
					if (m == 0)
						return durations[d].label;
					else if (m == 1)
						return df.format(Date.from(startDate));
					else if (m == 2)
						return "through";
					else if (m == 3)
						return df.format(Date.from(endDates[d]));
					else
						return "";
				} else {
					if (m >= calcMags.length)
						return "";
					double mag = calcMags[m];
					if (columnIndex == 1) {
						if (m == minMags.length) {
							return "M ≥ Main (" + ((float)mag) + ")";
						}
						return "M ≥ "+(float)mag;
					} else if (columnIndex == 2) {
						int lower = (int)(numEventsLower.get(durations[d], mag)+0.5);
						int upper = (int)(numEventsUpper.get(durations[d], mag)+0.5);
						int median = (int)(numEventsMedian.get(durations[d], mag)+0.5);
						if (upper == 0)
							return "*";
						return lower + " to " + upper + ", median " + median;
					} else if (columnIndex == 3) {

						double prob = 100.0 * probs.get(durations[d], mag);
						String probFormatted;
						if (prob < 1.0e-6 && prob > 0.0) {
							probFormatted = SimpleUtils.double_to_string ("%.2e", prob);
						} else {
							double probRounded = SimpleUtils.round_double_via_string ("%.2e", prob);
							probFormatted = SimpleUtils.double_to_string_trailz ("%.8f", SimpleUtils.TRAILZ_REMOVE, probRounded);
						}
						return probFormatted + " %";

						//int prob = (int)(100d*probs.get(durations[d], mag) + 0.5);
						//if (prob == 0)
						//	return "*";
						//else if (prob == 100)
						//	return ">99 %";
						//return prob+" %";

					}
				}
				
				return "";
			}
			
		};
	}

	// Round the start date to a "nice" time.
	public static Instant sdround (Instant theEventDate, Instant theStartDate) {

		// Convert to milliseconds since the epoch

		long event_millis = theEventDate.toEpochMilli();
		long start_millis = theStartDate.toEpochMilli();
		long delta_millis = start_millis - event_millis;

		// Find the rounding threshold
		int n;
		for (n = 0; n < sdround_thresholds.length; ++n) {
			if (delta_millis <= sdround_thresholds[n]) {
				break;
			}
		}
		long snap = sdround_snaps[n];

		// Round up to the next multiple of the snap

		long snap_units = (start_millis + snap - 1L) / snap;
		return Instant.ofEpochMilli (snap_units * snap);
	}

	// Round the start date to a "nice" time, with time given in milliseconds since the epoch.
	public static long sdround_millis (long event_millis, long start_millis) {

		// Delta in milliseconds since the epoch

		long delta_millis = start_millis - event_millis;

		// Find the rounding threshold
		int n;
		for (n = 0; n < sdround_thresholds.length; ++n) {
			if (delta_millis <= sdround_thresholds[n]) {
				break;
			}
		}
		long snap = sdround_snaps[n];

		// Round up to the next multiple of the snap

		long snap_units = (start_millis + snap - 1L) / snap;
		return (snap_units * snap);
	}

	// Round a displayed parameter value to look "nice".
	public static double dparm_round (double value) {
		double result = value;
		try {
			result = Double.parseDouble (String.format (Locale.US, "%.2e", value));	// limit to 3 significant digits
		} catch (Exception e) {
			result = value;
		}
		return result;
	}

	
	@SuppressWarnings("unchecked")
	public JSONOrderedObject buildJSON () {
		return buildJSON (System.currentTimeMillis());
	}
	
	@SuppressWarnings("unchecked")
	public JSONOrderedObject buildJSON (long creation_time) {
		JSONOrderedObject json = new JSONOrderedObject();

		boolean is_blocked = (new ServerConfig()).get_is_fc_content_blocked();
		
		json.put("creationTime", creation_time);
		long maxEndDate = 0L;
		for (Instant endDate : endDates) {
			if (endDate.toEpochMilli() > maxEndDate) {
				maxEndDate = endDate.toEpochMilli();
			}
		}
		json.put("expireTime", maxEndDate);
		
		json.put("advisoryTimeFrame", advisoryDuration.label);
		
		json.put("template", template.title);
		
		String injectableText = this.injectableText;
		if (injectableText == null) {
			injectableText = "";
		}
		if (is_blocked) {
			injectableText = "*** THIS IS A SYSTEM TEST. IT IS NOT AN ACTUAL FORECAST. ***";
		}
		json.put("injectableText", injectableText);
		
		// OBSERVATIONS
		JSONOrderedObject obsJSON = new JSONOrderedObject();
		JSONArray obsMagBins = new JSONArray();
		for (int m=0; m<minMags.length; m++) {
			JSONOrderedObject magBin = new JSONOrderedObject();
			magBin.put("magnitude", minMags[m]);
			if (is_blocked) {
				magBin.put("count", 0);
			} else {
				magBin.put("count", aftershockCounts[m]);
			}
			obsMagBins.add(magBin);
		}
		obsJSON.put("bins", obsMagBins);
		json.put("observations", obsMagBins);

		// MODEL
		JSONOrderedObject modelJSON = new JSONOrderedObject();
		modelJSON.put("name", model.getModelName());
		modelJSON.put("reference", model.getModelReference());
		JSONOrderedObject modelParams = new JSONOrderedObject();
		if (is_blocked) {
			modelParams.put("test", "yes");
		} else {
			Map<String, Object> modelParamMap = model.getModelParamMap();
			if (modelParamMap != null) {
				for (Map.Entry<String, Object> entry : modelParamMap.entrySet()) {
					modelParams.put(entry.getKey(), entry.getValue());
				}
			}

			if (userParamMap != null) {
				for (Map.Entry<String, Object> entry : userParamMap.entrySet()) {
					modelParams.put(entry.getKey(), entry.getValue());
				}
			}
		}
		modelJSON.put("parameters", modelParams);
		json.put("model", modelJSON);
		
		// FORECAST
		if (fractile_probabilities != null) {
			json.put("fractileProbabilities", fractile_probabilities.as_probabilities());
		}
		JSONArray forecastsJSON = new JSONArray();
		for (int i=0; i<durations.length; i++) {
			JSONOrderedObject forecastJSON = new JSONOrderedObject();
			
			forecastJSON.put("timeStart", startDate.toEpochMilli());
			forecastJSON.put("timeEnd", endDates[i].toEpochMilli());
			forecastJSON.put("label", durations[i].label);
			
			JSONArray magBins = new JSONArray();
			for (int m=0; m<minMags.length; m++) {
				JSONOrderedObject magBin = new JSONOrderedObject();
				magBin.put("magnitude", minMags[m]);
				if (is_blocked) {
					magBin.put("p95minimum", -1L);
					magBin.put("p95maximum", -1L);
					magBin.put("probability", -1.0);
					magBin.put("median", -1L);
				} else {
					magBin.put("p95minimum", Math.round(numEventsLower.get(durations[i], minMags[m])));
					magBin.put("p95maximum", Math.round(numEventsUpper.get(durations[i], minMags[m])));
					magBin.put("probability", probs.get(durations[i], minMags[m]));
					magBin.put("median", Math.round(numEventsMedian.get(durations[i], minMags[m])));
					if (fractile_probabilities != null) {
						magBin.put("fractileValues", fractile_values.get(durations[i], minMags[m]).as_values());
					}
				}
				magBins.add(magBin);
			}
			
			forecastJSON.put("bins", magBins);
			
			if (includeProbAboveMainshock && model.hasMainShockMag()) {
				JSONOrderedObject magBin = new JSONOrderedObject();
				double mainMag = model.getMainShockMag();
				magBin.put("magnitude", mainMag);
				if (is_blocked) {
					magBin.put("probability", -1.0);
				} else {
					magBin.put("probability", probs.get(durations[i], mainMag));
					if (fractile_probabilities != null) {
						magBin.put("fractileValues", fractile_values.get(durations[i], mainMag).as_values());
					}
				}
				forecastJSON.put("aboveMainshockMag", magBin);
			}
			
			forecastsJSON.add(forecastJSON);
		}
		json.put("forecast", forecastsJSON);

		// Next forecast time, if unknown then add advisory duration to the start time, and round up to whole number of minutes

		long next_forecast_time = nextForecastMillis;
		if (next_forecast_time < 0L) {		// if forecast will not be updated
			next_forecast_time = -1L;
		} else {
			if (next_forecast_time == 0L) {	// if time unknown
				next_forecast_time = advisoryDuration.getEndDate(startDate).toEpochMilli();
			}
			long minute = 60000L;			// one minute in milliseconds
			long time_in_minutes = (next_forecast_time + minute - 1L) / minute;
			next_forecast_time = time_in_minutes * minute;
		}
		json.put("nextForecastTime", next_forecast_time);
		
		return json;
	}
	
	public String buildJSONString () {
		return buildJSON().toJSONString();
	}
	
	public String buildJSONString (long creation_time) {
		return buildJSON(creation_time).toJSONString();
	}

}
