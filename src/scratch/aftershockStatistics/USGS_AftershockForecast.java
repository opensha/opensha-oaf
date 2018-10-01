package scratch.aftershockStatistics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
//import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;

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
import scratch.aftershockStatistics.util.JSONOrderedObject;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import scratch.UCERF3.erf.utils.ProbabilityModelsCalc;

public class USGS_AftershockForecast {
	
	private static final double[] min_mags_default = { 3d, 5d, 6d, 7d };
	private static final double fractile_lower = 0.025;
	private static final double fractile_upper = 0.975;
	
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
	private Table<Duration, Double, Double> numEventsLower;
	private Table<Duration, Double, Double> numEventsUpper;
	private Table<Duration, Double, Double> probs;
	
	private boolean includeProbAboveMainshock;
	
	// custom text which can be added
	private String injectableText = null;
	
	private Template template = Template.MAINSOCK;
	
	public USGS_AftershockForecast(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks,
			Instant eventDate, Instant startDate) {
		this(model, aftershocks, min_mags_default, eventDate, startDate, true);
	}
	
	public USGS_AftershockForecast(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean includeProbAboveMainshock) {
		compute(model, aftershocks, minMags, eventDate, startDate, includeProbAboveMainshock);
	}
	
	private static final DateFormat df = new SimpleDateFormat();
	private static final TimeZone utc = TimeZone.getTimeZone("UTC");
	
	private void compute(RJ_AftershockModel model, List<ObsEqkRupture> aftershocks, double[] minMags,
			Instant eventDate, Instant startDate, boolean includeProbAboveMainshock) {
		Preconditions.checkArgument(minMags.length > 0);
		
		this.model = model;
		this.minMags = minMags;
		this.eventDate = eventDate;
		this.startDate = startDate;
		this.includeProbAboveMainshock = includeProbAboveMainshock;
		
		// calcualte number of observations for each bin
		aftershockCounts = new int[minMags.length];
		for (int m=0; m<minMags.length; m++) {
			for (ObsEqkRupture eq : aftershocks)
				if (eq.getMag() >= minMags[m])
					aftershockCounts[m]++;
		}
		
		numEventsLower = HashBasedTable.create();
		numEventsUpper = HashBasedTable.create();
		probs = HashBasedTable.create();
		
		durations = Duration.values();
		advisoryDuration = Duration.ONE_WEEK;
		endDates = new Instant[durations.length];
		
		double[] calcFractiles = {fractile_lower, fractile_upper};
		
		double[] calcMags = minMags;
		if (includeProbAboveMainshock) {
			// also calculate for mainshock mag
			calcMags = Arrays.copyOf(minMags, minMags.length+1);
			calcMags[calcMags.length-1] = model.getMainShockMag();
		}
		
		df.setTimeZone(utc);
		System.out.println("Start date: "+df.format(Date.from(startDate)));
		for (int i=0; i<durations.length; i++) {
			Duration duration = durations[i];
			Instant endDate = duration.getEndDate(startDate);
			System.out.println(duration.label+" end date: "+df.format(Date.from(endDate)));
			
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
					poissonProb = Double.parseDouble (String.format ("%.3e", poissonProb));	// limit to 4 significant digits
				}

				probs.put(duration, minMag, poissonProb);
			}
		}
	}
	
	static double getDateDelta(Instant start, Instant end) {
		long diff = end.toEpochMilli() - start.toEpochMilli();
		return (double)diff/(double)ProbabilityModelsCalc.MILLISEC_PER_DAY;
	}
	
	public void setAdvisoryDuration(Duration advisoryDuration) {
		this.advisoryDuration = advisoryDuration;
	}
	
	public void setInjectableText(String injectableText) {
		this.injectableText = injectableText;
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

	private static String[] headers = {"Time Window For Analysis", "Magnitude Range",
			"Most Likely Number Of Aftershocks (95% confidence)", "Probability of one or more aftershocks"};
	
	public TableModel getTableModel() {
		final int numEach = minMags.length+1;
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
					if (m >= minMags.length)
						return "";
					double mag = minMags[m];
					if (columnIndex == 1) {
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
	
	@SuppressWarnings("unchecked")
	public JSONOrderedObject buildJSON () {
		return buildJSON (System.currentTimeMillis());
	}
	
	@SuppressWarnings("unchecked")
	public JSONOrderedObject buildJSON (long creation_time) {
		JSONOrderedObject json = new JSONOrderedObject();
		
		json.put("creationTime", creation_time);
		long maxEndDate = 0L;
		for (Instant endDate : endDates)
			if (endDate.toEpochMilli() > maxEndDate)
				maxEndDate = endDate.toEpochMilli();
		json.put("expireTime", maxEndDate);
		
		json.put("advisoryTimeFrame", advisoryDuration.label);
		
		json.put("template", template.title);
		
		String injectableText = this.injectableText;
		if (injectableText == null)
			injectableText = "";
		json.put("injectableText", injectableText);
		
		// OBSERVATIONS
		JSONOrderedObject obsJSON = new JSONOrderedObject();
		JSONArray obsMagBins = new JSONArray();
		for (int m=0; m<minMags.length; m++) {
			JSONOrderedObject magBin = new JSONOrderedObject();
			magBin.put("magnitude", minMags[m]);
			magBin.put("count", aftershockCounts[m]);
			obsMagBins.add(magBin);
		}
		obsJSON.put("bins", obsMagBins);
		json.put("observations", obsMagBins);

		// MODEL
		JSONOrderedObject modelJSON = new JSONOrderedObject();
		modelJSON.put("name", model.getModelName());
		modelJSON.put("reference", "#url");
		JSONOrderedObject modelParams = new JSONOrderedObject();
		// return AftershockStatsCalc.getExpectedNumEvents(getMaxLikelihood_a(), b, magMain, magMin, getMaxLikelihood_p(), getMaxLikelihood_c(), tMinDays, tMaxDays);
		modelParams.put("a", model.getMaxLikelihood_a());
		modelParams.put("b", model.get_b());
		modelParams.put("magMain", model.getMainShockMag());
		modelParams.put("p", model.getMaxLikelihood_p());
		modelParams.put("c", model.getMaxLikelihood_c());
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
				magBin.put("p95minimum", Math.round(numEventsLower.get(durations[i], minMags[m])));
				magBin.put("p95maximum", Math.round(numEventsUpper.get(durations[i], minMags[m])));
				magBin.put("probability", probs.get(durations[i], minMags[m]));
				magBins.add(magBin);
			}
			
			forecastJSON.put("bins", magBins);
			
			if (includeProbAboveMainshock) {
				JSONOrderedObject magBin = new JSONOrderedObject();
				double mainMag = model.getMainShockMag();
				magBin.put("magnitude", mainMag);
				magBin.put("probability", probs.get(durations[i], mainMag));
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
