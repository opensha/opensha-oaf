package org.opensha.oaf.etas;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.opensha.commons.data.function.ArbDiscrEmpiricalDistFunc;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class ETAS_USGS_AftershockForecastPR {
	
	private static final boolean D = false; //debug
	private static final double[] min_mags_default = { 3d, 4d, 5d, 6d, 7d };
	private static final double fractile_lower = 0.025;
	private static final double fractile_median = 0.5;
	private static final double fractile_upper = 0.975;
	
	private enum StartTime {
		NOW("Now", GregorianCalendar.DAY_OF_MONTH,0),
		ONE_DAY("1 Day", GregorianCalendar.DAY_OF_MONTH, 1),
		TWO_DAY("2 Day", GregorianCalendar.DAY_OF_MONTH, 2),
		THREE_DAY("3 Day", GregorianCalendar.DAY_OF_MONTH, 3),
		FOUR_DAY("4 Day", GregorianCalendar.DAY_OF_MONTH, 4),
		FIVE_DAY("5 Day", GregorianCalendar.DAY_OF_MONTH, 5),
		SIX_DAY("6 Day", GregorianCalendar.DAY_OF_MONTH, 6),
		SEV_DAY("7 Day", GregorianCalendar.DAY_OF_MONTH, 7),
		ONE_WEEK("1 Week", GregorianCalendar.WEEK_OF_MONTH, 1),
		EIGHT_DAY("8 Day", GregorianCalendar.DAY_OF_MONTH, 8),
		NINE_DAY("9 Day", GregorianCalendar.DAY_OF_MONTH, 9),
		TEN_DAY("10 Day", GregorianCalendar.DAY_OF_MONTH, 10),
		ELEV_DAY("11 Day", GregorianCalendar.DAY_OF_MONTH, 11),
		TWELV_DAY("12 Day", GregorianCalendar.DAY_OF_MONTH, 12),
		THIRT_DAY("13 Day", GregorianCalendar.DAY_OF_MONTH, 13),
		TWO_WEEK("2 Week", GregorianCalendar.WEEK_OF_MONTH, 2),
		THREE_WEEK("3 Week", GregorianCalendar.WEEK_OF_MONTH, 3),
		FOUR_WEEK("4 Week", GregorianCalendar.WEEK_OF_MONTH, 4),
		ONE_MONTH("1 Month", GregorianCalendar.MONTH, 1),
		FIVE_WEEK("5 Week", GregorianCalendar.WEEK_OF_MONTH, 5),
		SIX_WEEK("6 Week", GregorianCalendar.WEEK_OF_MONTH, 6),
		SEVEN_WEEK("7 Week", GregorianCalendar.WEEK_OF_MONTH, 7),
		EIGHT_WEEK("8 Week", GregorianCalendar.WEEK_OF_MONTH, 8),
		TWO_MONTHS("2 Months", GregorianCalendar.MONTH, 2),
		THREE_MONTH("3 Month", GregorianCalendar.MONTH, 3),
		FOUR_MONTHS("4 Months", GregorianCalendar.MONTH, 4),
		FIVE_MONTH("5 Month", GregorianCalendar.MONTH, 5),
		SIX_MONTHS("6 Months", GregorianCalendar.MONTH, 6),
		SEVEN_MONTHS("7 Months", GregorianCalendar.MONTH, 7),
		EIGHT_MONTHS("8 Months", GregorianCalendar.MONTH, 8),
		NINE_MONTHS("9 Months", GregorianCalendar.MONTH, 9),
		TEN_MONTHS("10 Months", GregorianCalendar.MONTH, 10),
		ELEVEN_MONTHS("11 Months", GregorianCalendar.MONTH, 11),
		TWELVE_MONTHS("12 Months", GregorianCalendar.MONTH, 12),
		ONE_YEAR("1 Years", GregorianCalendar.YEAR, 1),
		THIRTN_MONTHS("13 Months", GregorianCalendar.MONTH, 13),
		FOURTN_MONTHS("14 Months", GregorianCalendar.MONTH, 14),
		FIFTN_MONTHS("15 Months", GregorianCalendar.MONTH, 15),
		SIXTN_MONTHS("16 Months", GregorianCalendar.MONTH, 16),
		SEVENTN_MONTHS("17 Months", GregorianCalendar.MONTH, 17),
		EIGHTN_MONTHS("18 Months", GregorianCalendar.MONTH, 18),
		TWO_YEARS("2 Years", GregorianCalendar.YEAR, 2),
		THREE_YEAR("3 Year", GregorianCalendar.YEAR, 3),
		FOUR_YEARS("4 Years", GregorianCalendar.YEAR, 4),
		FIVE_YEAR("5 Year", GregorianCalendar.YEAR, 5),
		SIX_YEARS("6 Years", GregorianCalendar.YEAR, 6),
		SEVEN_YEAR("7 Year", GregorianCalendar.YEAR, 7),
		EIGHT_YEARS("8 Years", GregorianCalendar.YEAR, 8),
		NINE_YEAR("9 Year", GregorianCalendar.YEAR, 9),
		TEN_YEARS("10 Years", GregorianCalendar.YEAR, 10);
//		ELEV_YEAR("11 Year", GregorianCalendar.YEAR, 11),
//		TWEL_YEARS("12 Years", GregorianCalendar.YEAR, 12),
//		THIRT_YEAR("13 Year", GregorianCalendar.YEAR, 13),
//		FOURT_YEARS("14 Years", GregorianCalendar.YEAR, 14),
//		FIFT_YEAR("15 Year", GregorianCalendar.YEAR, 15),
//		SIXT_YEARS("16 Years", GregorianCalendar.YEAR, 16),
//		SEVENT_YEAR("17 Year", GregorianCalendar.YEAR, 17),
//		EIGHTT_YEARS("18 Years", GregorianCalendar.YEAR, 18),
//		NINET_YEAR("19 Year", GregorianCalendar.YEAR, 19),
//		TWENTY_YEARS("20 Years", GregorianCalendar.YEAR, 20);
		
		
		
		private final String label;
		private final int calendarField;
		private final int calendarAmount;
		
		private StartTime(String label, int calendarField, int calendarAmount) {
			this.label = label;
			this.calendarField = calendarField;
			this.calendarAmount = calendarAmount;
		}
		
		public GregorianCalendar getEndDate(GregorianCalendar startDate) {
			GregorianCalendar endDate = (GregorianCalendar) startDate.clone();
			endDate.add(calendarField, calendarAmount);
			return endDate;
		}
	}
	
	private ETAS_AftershockModel model;
	
	private GregorianCalendar eventDate;
	private GregorianCalendar startDate;
	private GregorianCalendar[] endDates;	
	
	private StartTime[] startTimes;
	private double[] minMags;
	private Table<StartTime, Double, Double> numEventsLower;
	private Table<StartTime, Double, Double> numEventsUpper;
	private Table<StartTime, Double, Double> numEventsMedian;
	
	private Table<StartTime, Double, Double> probs;
	
	public ETAS_USGS_AftershockForecastPR(ETAS_AftershockModel model,
			GregorianCalendar eventDate, GregorianCalendar startDate, double forecastEndTime) {
		this(model, min_mags_default, eventDate, startDate, forecastEndTime);
	}
	
	public ETAS_USGS_AftershockForecastPR(ETAS_AftershockModel model, double[] minMags,
			GregorianCalendar eventDate, GregorianCalendar startDate, double forecastEndTime) {
		compute(model, minMags, eventDate, startDate, forecastEndTime);
	}
	
	private static final DateFormat df = new SimpleDateFormat();
	
	private void compute(ETAS_AftershockModel model, double[] minMags,
			GregorianCalendar eventDate, GregorianCalendar startDate, double forecastEndTime) {
		Preconditions.checkArgument(minMags.length > 0);
		
		this.model = model;
		this.minMags = minMags;
		this.eventDate = eventDate;
		this.startDate = startDate;
		
		numEventsMedian = HashBasedTable.create();
		numEventsLower = HashBasedTable.create();
		numEventsUpper = HashBasedTable.create();
		probs = HashBasedTable.create();
		
		startTimes = StartTime.values();
				
		GregorianCalendar forecastEndDate = new GregorianCalendar();
		forecastEndDate.setTimeInMillis(
//				(long) (startDate.getTimeInMillis() + forecastEndTime*ETAS_StatsCalc.MILLISEC_PER_DAY));
				(long) (eventDate.getTimeInMillis() + forecastEndTime*ETAS_StatsCalc.MILLISEC_PER_DAY));
		
		
		endDates = new GregorianCalendar[startTimes.length];
		
		double[] calcFractiles = {fractile_lower, fractile_median, fractile_upper};
		
		if(D) System.out.println("Start date: "+df.format(startDate.getTime()));
		
		double tMinDays, tMaxDays;
		for (int i=0; i<startTimes.length; i++) {
			StartTime startTime = startTimes[i];	  
			
			// start the forecast at the "startTime" force the endDate to be one year
			GregorianCalendar newStartDate = startTime.getEndDate(startDate);
			

//			GregorianCalendar endDate = StartTime.ONE_DAY.getEndDate(newStartDate);
//			GregorianCalendar endDate = StartTime.ONE_WEEK.getEndDate(newStartDate);
			GregorianCalendar endDate = StartTime.ONE_MONTH.getEndDate(newStartDate);
//			GregorianCalendar endDate = StartTime.ONE_YEAR.getEndDate(newStartDate);
			
			if (!endDate.after(forecastEndDate)){ //check to make sure forecast extends that far
				if(D) System.out.println(startTime.label+" end date: "+df.format(endDate.getTime())
						+ " forecastEndDate: " + df.format(forecastEndDate.getTime()) );

				// this does the probabilities incrementally.
				tMinDays = getDateDelta(eventDate, newStartDate);
				tMaxDays = getDateDelta(eventDate, endDate);
				
				
				Preconditions.checkState(tMinDays >= 0d, "tMinDays must be positive: %s", tMinDays);
				
				
				Preconditions.checkState(tMaxDays > tMinDays,
						"tMaxDays must be greter than tMinDays: %s <= %s", tMaxDays, tMinDays);

				endDates[i] = endDate;


				for (int m=0; m<minMags.length; m++) {
					double minMag = minMags[m];

					System.out.println("doing forecast for " + tMinDays + " to " + tMaxDays);
					
					double[] fractiles = model.getCumNumFractileWithAleatory(calcFractiles, minMag, tMinDays, tMaxDays);

					numEventsLower.put(startTime, minMag, fractiles[0]);
					numEventsMedian.put(startTime, minMag, fractiles[1]);
					numEventsUpper.put(startTime, minMag, fractiles[2]);

					

					//				double rate = model.getModalNumEvents(minMag, tMinDays, tMaxDays);


					ArbDiscrEmpiricalDistFunc subDist = model.computeNum_DistributionFunc(tMinDays, tMaxDays, minMag);
					double prob, probMult = 1;
					if(Math.abs(subDist.getX(0) - 0) >  1e-6) //if there is no zeros column, no sims had zero events, so: p = 1.
						prob = 1;
					else
						prob = 1 - subDist.getY(0);

//					if(minMag < model.magComplete)
//						//scale up the probability to account for events below the simulation min magnitude
//						probMult = Math.pow(10, -model.b*(minMag - model.magComplete));
					if(minMag < model.simulatedCatalog.minMagLimit)
						//scale up the probability to account for events below the simulation min magnitude
						probMult = Math.pow(10, -model.b*(minMag - model.simulatedCatalog.minMagLimit));
					
					prob = 1 - Math.pow(1-prob, probMult); //assumes probabilities are Poissonian...

					//				System.out.println(minMag +" "+ model.magComplete +" "+ prob +" "+ probMult);
					probs.put(startTime, minMag, prob);
				}
			} else {
				System.out.println("Skipping " + startTimes[i] + " forecast.");
				if(D) System.out.println("Forecast end date is " + df.format(endDate.getTime())
						+" but computation end date is "+ df.format(forecastEndDate.getTime()));
			}
		}
	}
	
	private static double getDateDelta(GregorianCalendar start, GregorianCalendar end) {
		long diff = end.getTimeInMillis() - start.getTimeInMillis();
		return (double)diff/ETAS_ComcatAccessor.day_millis;
	}
	
	private static String[] headers = {"Forecast Interval", "Magnitude Range",
			"Median Number", "95% confidence range", "Chance of at least one"};
	
	public TableModel getTableModel() {
		final int numEach = minMags.length+1;
		final int rows = probs.rowKeySet().size()*numEach;
//		final int cols = 4;
		final int cols = 5;
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
						return startTimes[d].label;
					else if (m == 1)
						return df.format(startDate.getTime());
					else if (m == 2)
						return "through";
					else if (m == 3)
						return df.format(endDates[d].getTime());
					else
						return "";
				} else {
					if (m >= minMags.length)
						return "";
					double mag = minMags[m];
					if (columnIndex == 1) {
						return "M ≥ "+(float)mag;
					} else if (columnIndex == 2) {
						// rounding bad! Doesn't make a whole lot of sense
//						int median = (int)(numEventsMedian.get(startTimes[d], mag)+0.5);
						int median = (int)(numEventsMedian.get(startTimes[d], mag).intValue());
						return median;
					} else if (columnIndex == 3) {
//						int lower = (int)(numEventsLower.get(startTimes[d], mag)+0.5);
//						int upper = (int)(numEventsUpper.get(startTimes[d], mag)+0.5);
						int lower = (int)(numEventsLower.get(startTimes[d], mag).intValue());
						int upper = (int)(numEventsUpper.get(startTimes[d], mag).intValue());
						if (upper == 0)
							return "0";
						return lower+" to "+upper;
					} else if (columnIndex == 4) {
						int prob = (int)(100d*probs.get(startTimes[d], mag) + 0.5);
						if (prob == 0)
							return "<0.1%";
						else if (prob == 100)
							return ">99 %";
						return prob+" %";
					}
				}
				
				return "";
			}
			
		};
	}

}
