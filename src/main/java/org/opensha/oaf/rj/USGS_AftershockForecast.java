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
import org.opensha.oaf.aafs.ActionConfig;

import org.opensha.oaf.util.SimpleUtils;


public class USGS_AftershockForecast {
	
	private static final double[] min_mags_default = { 3d, 4d, 5d, 6d, 7d };
	private static final double fractile_lower = 0.025;
	private static final double fractile_upper = 0.975;
	private static final double fractile_median = 0.500;
	private static final double mag_bin_half_width_default = 0.05;

	private static final boolean include_fractile_list = true;

	private static final boolean include_bar_list = true;	// if true, then include_fractile_list must also be true

	private static final boolean use_bar_prob_one = true;	// if true, use the probability of one or more when constructing bars

	private static final boolean force_bar_sum_100 = false;	// if true, force bar percentages to sum to 100

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
		public int[] barvals;

		// Copy entire array
		public FractileArray (double[] the_values) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOf (the_values, the_values.length);
			}
			barvals = null;
		}

		// Copy portion of array starting at index lo
		public FractileArray (double[] the_values, int lo) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOfRange (the_values, lo, the_values.length);
			}
			barvals = null;
		}

		// Copy portion of array from index lo, inclusive, to index hi, exclusive
		public FractileArray (double[] the_values, int lo, int hi) {
			if (the_values == null) {
				values = null;
			} else {
				values = Arrays.copyOfRange (the_values, lo, hi);
			}
			barvals = null;
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

		// Set the bar values from an array
		public final void set_barvals (int[] the_barvals) {
			if (the_barvals == null) {
				barvals = null;
			} else {
				barvals = Arrays.copyOf (the_barvals, the_barvals.length);
			}
			return;
		}

		// Check if we have bar values
		public final boolean has_barvals () {
			return barvals != null;
		}

		// Make a JSON array containing the bar values
		public final JSONArray as_barvals () {
			JSONArray result = new JSONArray();
			if (barvals != null) {
				for (int j = 0; j < barvals.length; ++j) {
					result.add (barvals[j]);
				};
			}
			return result;
		}

		// Calculate the bar values, given the fractile values.
		// Parameters:
		//  fractile_probs = Fractile probabilities.
		//  bar_counts = Bar counts, this determines the number of bars.
		//  prob_one = Probability of one or more aftershocks.

		public final void calc_barvals (double[] fractile_probs, int[] bar_counts, double prob_one) {

			// If we don't have everything we need, then no bar values

			if (!( values != null && fractile_probs != null && bar_counts != null && fractile_probs.length > 0 && bar_counts.length > 0 )) {
				barvals = null;
				return;
			}

			if (!( values.length == fractile_probs.length )) {
				throw new IllegalArgumentException ("USGS_AftershockForecast.FractileArray: Fractile list length mismatch: values.length = " + values.length + ", fractile_probs.length = " + fractile_probs.length);
			}

			// Allocate and zero-fill the bar values

			int nbars = bar_counts.length;
			barvals = new int[nbars];
			double[] r_barvals = new double[nbars];

			for (int j = 0; j < nbars; ++j) {
				barvals[j] = 0;
				r_barvals[j] = 0.0;
			}

			// Degenerate case of one bar

			if (nbars == 1) {
				barvals[0] = 100;
				return;
			}

			// If the probability of one or more is less than 1%, and we're using the probability of 1+, return 100% probability of zero

			if (use_bar_prob_one && prob_one < 0.01) {
				barvals[0] = 100;
				return;
			}

			// Indexes into bars and fractiles

			int bar_ix = 0;
			int frac_ix = 0;
			int nfracs = fractile_probs.length;

			// Cumulative probability so far
			// Note: We maintain the condition that frac_ix is smallest index so that fractile_probs[frac_ix] > cprob

			double cprob = 0.0;

			// If first bar is for zero only, and we're using the probability of 1+ ...

			if (use_bar_prob_one && bar_counts[1] == 1) {

				// If the probability of one or more is greater than 99%, force the first bar to zero

				if (prob_one > 0.99) {
					barvals[0] = 0;
				}

				// Otherwise (prob of 1+ is between 1% and 99%), force the first bar to be the complement of the probabability of one or more display

				else {
					barvals[0] = 100 - (int)(Math.round (prob_one * 100.0));
				}

				// Set cumulative probability to be the complement of the probability of one or more

				cprob = 1.0 - prob_one;

				// Advance index into fractiles to the first probability greater than the cumulative probability

				while (frac_ix < nfracs && fractile_probs[frac_ix] <= cprob) {
					++frac_ix;
				}

				// Set the real bar value to exactly equal the percentage

				r_barvals[0] = (double)(barvals[0]);

				// We did the first bar

				++bar_ix;
			}

			// Loop over bars

			for ( ; bar_ix < nbars; ++bar_ix) {

				// Save the initial cumulative probability

				double saved_cprob = cprob;

				// If this is the last bar, set the cumulative probability to 1 and advance to end of fractile list

				if (bar_ix == nbars - 1) {
					cprob = 1.0;
					frac_ix = nfracs;
				}

				// Otherwise, not the last bar ...

				else {

					// End count of the current bar

					int bar_end_count = bar_counts[bar_ix + 1];

					// Interval of values in which we interpolate
					// Note: We maintain the condition bar_end_count > interval_lo

					int interval_lo = bar_counts[bar_ix];
					int interval_hi = interval_lo;

					// Scan forward until we find a fractile value greater than or equal to the end of the current bar

					int saved_frac_ix = frac_ix;
					while (frac_ix < nfracs) {
						interval_hi = Math.max (interval_hi, (int)(Math.round (values[frac_ix])));
						if (bar_end_count <= interval_hi) {
							break;
						}
						interval_lo = interval_hi;
						++frac_ix;
					}

					// If we reached the end of the fractiles, set the cumulative probability to 1

					if (frac_ix >= nfracs) {
						cprob = 1.0;
					}

					// Otherwise, if we advanced within the fractile list, interpolate between two fractiles

					else if (frac_ix > saved_frac_ix) {

						// Note: interval_lo < bar_end_count <= interval_hi

						double p_lo = fractile_probs[frac_ix - 1];
						double p_hi = fractile_probs[frac_ix];

						double v_lo = (double)(interval_lo);
						double v_hi = (double)(interval_hi);
						double v_mid = ((double)(bar_end_count)) - 0.5;

						cprob = p_lo + ( (p_hi - p_lo) * (v_mid - v_lo) / (v_hi - v_lo) );
					}

					// Otherwise, interpolate between the start of the bar and the end of the fractile

					else {

						// Note: bar_counts[bar_ix] == interval_lo < bar_end_count <= interval_hi

						double p_lo = cprob;
						double p_hi = fractile_probs[frac_ix];

						double v_lo = Math.max (0.0, ((double)(interval_lo)) - 0.5);
						double v_hi = (double)(interval_hi);
						double v_mid = ((double)(bar_end_count)) - 0.5;

						cprob = p_lo + ( (p_hi - p_lo) * (v_mid - v_lo) / (v_hi - v_lo) );
					}
				}

				// Probability for this bar

				r_barvals[bar_ix] = Math.max (0.0, (cprob - saved_cprob) * 100.0);
				barvals[bar_ix] = (int)(Math.round (r_barvals[bar_ix]));
			}

			// If we want to force percentages to sum to exactly 100 ...

			if (force_bar_sum_100) {

				// First bar eligible for adjustment

				int lo_bar_ix = ((use_bar_prob_one && bar_counts[1] == 1) ? 1 : 0);

				// Sum the percentages

				int sum = 0;
				for (int j = 0; j < nbars; ++j) {
					sum += barvals[j];
				}

				// If sum too low, increase percentages

				while (sum < 100) {

					// Find bar whose percentage is lowest compared to its real value
					// (don't go from 0 to 1 unless it's adjacent to a nonzero bar)

					int best_j = -1;
					double best_err = 0.0;

					for (int j = lo_bar_ix; j < nbars; ++j) {
						if (barvals[j] <= 99 && ( barvals[j] > 0 || (j > 0 && barvals[j-1] > 0) || (j+1 < nbars && barvals[j+1] > 0) )) {
							double err = ((double)(barvals[j])) - r_barvals[j];
							if (best_j == -1 || err <= best_err) {
								best_j = j;
								best_err = err;
							}
						}
					}

					// Stop if we didn't find any eligible bar

					if (best_j == -1) {
						break;
					}

					// Increase the percentage that produces the least error

					barvals[best_j]++;
					++sum;
				}

				// If sum too high, decrease percentages

				while (sum > 100) {

					// Find bar whose percentage is highest compared to its real value
					// (don't go from 1 to 0 unless it's the first or last bar or adjacent to a zero bar)

					int best_j = -1;
					double best_err = 0.0;

					for (int j = lo_bar_ix; j < nbars; ++j) {
						if (barvals[j] >= 1 && ( barvals[j] > 1 || j == 0 || j+1 >= nbars || barvals[j-1] == 0 || barvals[j+1] == 0 )) {
							double err = ((double)(barvals[j])) - r_barvals[j];
							if (best_j == -1 || err >= best_err) {
								best_j = j;
								best_err = err;
							}
						}
					}

					// Stop if we didn't find any eligible bar

					if (best_j == -1) {
						break;
					}

					// Decrease the percentage that produces the least error

					barvals[best_j]--;
					--sum;
				}
			}

			return;
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
				if (include_bar_list) {
					fractile_probabilities.set_barvals ((new ActionConfig()).get_adv_bar_counts_array());
				}
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
					FractileArray fractile_array = new FractileArray (fractiles, calcFractiles.length);
					if (fractile_probabilities.has_barvals()) {
						fractile_array.calc_barvals (fractile_probabilities.values, fractile_probabilities.barvals, poissonProb);
					}
					fractile_values.put (duration, minMag, fractile_array);
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
			if (fractile_probabilities.has_barvals()) {
				json.put("barLabels", fractile_probabilities.as_barvals());
			}
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
						FractileArray fractile_array = fractile_values.get(durations[i], minMags[m]);
						magBin.put("fractileValues", fractile_array.as_values());
						if (fractile_probabilities.has_barvals()) {
							magBin.put("barPercentages", fractile_array.as_barvals());
						}
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
						FractileArray fractile_array = fractile_values.get(durations[i], mainMag);
						magBin.put("fractileValues", fractile_array.as_values());
						if (fractile_probabilities.has_barvals()) {
							magBin.put("barPercentages", fractile_array.as_barvals());
						}
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
