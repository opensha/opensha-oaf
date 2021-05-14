package org.opensha.oaf.rj;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
//import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;
import java.util.Locale;

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

public class USGS_AftershockForecast {
	
	private static final double[] min_mags_default = { 3d, 4d, 5d, 6d, 7d };
	private static final double fractile_lower = 0.025;
	private static final double fractile_upper = 0.975;
	private static final double mag_bin_half_width_default = 0.05;

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
	
	private RJ_AftershockModel model;
	
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
	private Table<Duration, Double, Double> probs;
	
	private boolean includeProbAboveMainshock;
	
	// custom text which can be added
	private String injectableText = null;

	// the time of the next scheduled forecast, or 0L if unknown, or -1L if no more forecasts are scheduled
	private long nextForecastMillis = 0L;
	
	private Template template = Template.MAINSOCK;
	
	public USGS_AftershockForecast(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks,
			Instant eventDate, Instant startDate) {
		this(model, aftershocks, min_mags_default, eventDate, startDate, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate) {
		this(model, aftershocks, minMags, eventDate, startDate, true, mag_bin_half_width_default);
	}
	
	public USGS_AftershockForecast(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean includeProbAboveMainshock, double mag_bin_half_width) {
		compute(model, aftershocks, minMags, eventDate, startDate, includeProbAboveMainshock, mag_bin_half_width);
	}
	
	private static final DateFormat df = new SimpleDateFormat();
	private static final TimeZone utc = TimeZone.getTimeZone("UTC");
	
	private void compute(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean includeProbAboveMainshock, double mag_bin_half_width) {
		Preconditions.checkArgument(minMags.length > 0);

		// Round the start time

		startDate = sdround (eventDate, startDate);
		
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
		long event_time = eventDate.toEpochMilli();
		for (ObsEqkRupture eq : aftershocks) {
			// ignore events that occurred before the mainshock
			if (eq.getOriginTime() >= event_time) {
				for (int m=0; m<minMags.length; m++) {
					if (eq.getMag() >= minMags[m] - mag_bin_half_width) {
						aftershockCounts[m]++;
					}
				}
			}
		}
		
		numEventsLower = HashBasedTable.create();
		numEventsUpper = HashBasedTable.create();
		probs = HashBasedTable.create();
		
		durations = Duration.values();
		advisoryDuration = Duration.ONE_WEEK;
		endDates = new Instant[durations.length];
		
		double[] calcFractiles = {fractile_lower, fractile_upper};
		
		calcMags = minMags;
		if (includeProbAboveMainshock) {
			// also calculate for mainshock mag
			calcMags = Arrays.copyOf(minMags, minMags.length+1);
			calcMags[calcMags.length-1] = model.getMainShockMag();
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
				
				double[] fractiles = model.getCumNumFractileWithAleatory(calcFractiles, minMag, tMinDays, tMaxDays);
				
				numEventsLower.put(duration, minMag, fractiles[0]);
				numEventsUpper.put(duration, minMag, fractiles[1]);
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
						if (upper == 0)
							return "*";
						return lower+" to "+upper;
					} else if (columnIndex == 3) {
						int prob = (int)(100d*probs.get(durations[d], mag) + 0.5);
						if (prob == 0)
							return "*";
						else if (prob == 100)
							return ">99 %";
						return prob+" %";
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
		modelJSON.put("reference", "#url");
		JSONOrderedObject modelParams = new JSONOrderedObject();
		if (is_blocked) {
			modelParams.put("a", 0.0);
			modelParams.put("b", 0.0);
			modelParams.put("magMain", 0.0);
			modelParams.put("p", 0.0);
			modelParams.put("c", 0.0);
			modelParams.put("aSigma", 0.0);
			modelParams.put("pSigma", 0.0);
		} else {
			modelParams.put("a", dparm_round (model.getMaxLikelihood_a()));
			modelParams.put("b", dparm_round (model.get_b()));
			modelParams.put("magMain", dparm_round (model.getMainShockMag()));
			modelParams.put("p", dparm_round (model.getMaxLikelihood_p()));
			modelParams.put("c", dparm_round (model.getMaxLikelihood_c()));
			modelParams.put("aSigma", dparm_round (model.getStdDev_a()));
			modelParams.put("pSigma", dparm_round (model.getStdDev_p()));
		}
		modelJSON.put("parameters", modelParams);
		json.put("model", modelJSON);
		
		// FORECAST
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
				} else {
					magBin.put("p95minimum", Math.round(numEventsLower.get(durations[i], minMags[m])));
					magBin.put("p95maximum", Math.round(numEventsUpper.get(durations[i], minMags[m])));
					magBin.put("probability", probs.get(durations[i], minMags[m]));
				}
				magBins.add(magBin);
			}
			
			forecastJSON.put("bins", magBins);
			
			if (includeProbAboveMainshock) {
				JSONOrderedObject magBin = new JSONOrderedObject();
				double mainMag = model.getMainShockMag();
				magBin.put("magnitude", mainMag);
				if (is_blocked) {
					magBin.put("probability", -1.0);
				} else {
					magBin.put("probability", probs.get(durations[i], mainMag));
				}
				forecastJSON.put("aboveMainshockMag", magBin);
			}
			
			forecastsJSON.add(forecastJSON);
		}
		json.put("forecast", forecastsJSON);
		
		return json;
	}
	
	public String buildJSONString () {
		return buildJSON().toJSONString();
	}
	
	public String buildJSONString (long creation_time) {
		return buildJSON(creation_time).toJSONString();
	}

}
