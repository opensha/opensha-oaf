package org.opensha.oaf.aftershockStatistics.aafs;

import java.util.List;

import org.opensha.oaf.aftershockStatistics.aafs.entity.PendingTask;
import org.opensha.oaf.aftershockStatistics.aafs.entity.LogEntry;
import org.opensha.oaf.aftershockStatistics.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aftershockStatistics.aafs.entity.TimelineEntry;
import org.opensha.oaf.aftershockStatistics.aafs.entity.AliasFamily;

import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;

import org.opensha.oaf.aftershockStatistics.comcat.ComcatException;
import org.opensha.oaf.aftershockStatistics.CompactEqkRupList;

import org.opensha.oaf.aftershockStatistics.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.aftershockStatistics.pdl.PDLSender;
import gov.usgs.earthquake.product.Product;


/**
 * Support functions for PDL reporting.
 * Author: Michael Barall 06/24/2018.
 */
public class PDLSupport extends ServerComponent {




	//----- Task execution subroutines : PDL operations -----




//	// Send a report to PDL.
//	// Throw an exception if the report failed.
//
//	public void send_pdl_report (TimelineStatus tstatus) throws Exception {
//
//		// The JSON file to send
//
//		String jsonText = tstatus.forecast_results.get_pdl_model();
//
//		// The event network and code
//
//		String eventNetwork = tstatus.forecast_mainshock.mainshock_network;
//		String eventCode = tstatus.forecast_mainshock.mainshock_code;
//
//		// The event ID, which for us identifies the timeline
//
//		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);
//
//		// Modification time, 0 means now
//
//		long modifiedTime = 0L;
//
//		// Review status, false means automatically generated
//
//		boolean isReviewed = false;
//
//		// Build the product
//
//		Product product = PDLProductBuilderOaf.createProduct (eventID, eventNetwork, eventCode, isReviewed, jsonText, modifiedTime);
//
//		// Sign the product
//
//		PDLSender.signProduct(product);
//
//		// Send the product, true means it is text
//
//		PDLSender.sendProduct(product, true);
//
//		return;
//	}




	// Send a report to PDL.
	// Throw an exception if the report failed.
	// Use this version if the catalog is in tstatus.forecast_results.

	public void send_pdl_report (TimelineStatus tstatus) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (eventID, isReviewed);

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		return;
	}




	// Send a report to PDL.
	// Throw an exception if the report failed.
	// Use this version to supply the catalog separately.

	public void send_pdl_report (TimelineStatus tstatus, CompactEqkRupList catalog) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options, catalog);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (eventID, isReviewed);

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		return;
	}




	// Return true if this machine is primary for sending reports to PDL, false if secondary

	public boolean is_pdl_primary () {

		// For now, just assume primary

		return true;
	}



	//----- Construction -----


	// Default constructor.

	public PDLSupport () {}

}
