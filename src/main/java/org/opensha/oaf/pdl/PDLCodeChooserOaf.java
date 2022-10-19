package org.opensha.oaf.pdl;

import java.util.Map;
import java.util.List;
import java.util.Date;

import java.io.File;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
//import org.opensha.oaf.comcat.ComcatOAFProduct;
import org.opensha.oaf.comcat.ComcatProductOaf;
import org.opensha.oaf.comcat.GeoJsonUtils;
import org.opensha.oaf.comcat.GeoJsonHolder;
import org.opensha.commons.data.comcat.ComcatException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;

import org.opensha.oaf.util.SimpleUtils;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.distribution.SocketProductSender;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;



/**
 * Routines to select the "code" used for the PDL product of type "oaf".
 * Author: Michael Barall 12/09/2018.
 *
 * A PDL product is identified by "source" and "code".  A product with the
 * same source and code as an earlier product will replace the earlier product.
 * But a product with a different source and code as an earlier product is
 * kept in Comcat in addition to the earlier product, even if it is the same
 * type and associated to the same earthquake.
 *
 * We always use "oaf" as the type and "us" as the source.  We would like to
 * use the authoritative ID as the code, but this creates problems when the
 * authoritative ID changes.  The automatic system tries to handle with By
 * using the timeline ID, but the GUI has no simple solution.
 *
 * (The strings "oaf" and "us" now come from ServerConfig.json.  This allows
 * them to be changed for use in scenario earthquakes.)
 *
 * Our approach is to choose the code by reading the OAF product that already
 * exists in Comcat and re-using its code.  If none exist, then we choose the
 * code suggested by the caller, which will be a timeline ID, query ID, or
 * authoritative ID.
 *
 * This also addresses some other issues:
 *
 * (a) The code should always be one of the Comcat IDs currently assigned to
 * the earthquake (to avoid conflict with other earthquakes when there is a split).
 *
 * (b) If there is more than one existing OAF product, then the extra ones
 * are deleted (which is necessary to ensure that the new product is displayed
 * on the event page).
 *
 * (c) Because the set of products (and even the authoritative ID) can be
 * different in the production and development servers, we read the OAF product
 * from the server that is the intended destination of the new product.
 *
 * (d) There is a mechanism to avoid replacing reviewed with non-reviewed
 * products, if that is desired.
 */
public class PDLCodeChooserOaf {

	// Our type.

	//public static final String TYPE_OAF = "oaf";

	// Our source.

	//public static final String SOURCE_US = "us";




	// Delete an existing OAF product.
	// Parameters:
	//  oafProduct = Product to delete.
	//  eventNetwork = Network identifier for the event (for example, "us").
	//  eventCode = Network code for the event (for example, "10006jv5").
	//  isReviewed = True if this product has been reviewed.
	// If the source in oafProduct is not "us" then no action is performed.
	// Otherwise, the product is deleted using the code from within the product.
	// Throws an exception in case of error.

	public static void deleteOafProduct (ComcatProductOaf oafProduct,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		// If not our source ID, do nothing

		if (!( oafProduct.sourceID.equals ((new ServerConfig()).get_pdl_oaf_source()) )) {
			return;
		}
			
		System.out.println ("Deleting existing OAF product: " + oafProduct.summary_string());

		// The code

		String code = oafProduct.eventID;

		// Modification time, 0 means now

		long modifiedTime = 0L;

		// Build the product

		Product product = PDLProductBuilderOaf.createDeletionProduct (code, eventNetwork, eventCode, isReviewed, modifiedTime);

		// Sign the product

		PDLSender.signProduct (product);

		// Send the product, true means it is text

		PDLSender.sendProduct (product, true);

		return;
	}




	// Delete a list of existing OAF products.
	// Parameters:
	//  oafProducts = List of products to delete.
	//  keep = Index of a product to keep (that is, not delete), or -1 to delete all.
	//  eventNetwork = Network identifier for the event (for example, "us").
	//  eventCode = Network code for the event (for example, "10006jv5").
	//  isReviewed = True if this product has been reviewed.
	// If the source in a product is not "us" then it is not deleted.
	// Otherwise, except for the kept product (if any), the product is deleted
	// using the code from within the product.
	// Throws an exception in case of error.
	//
	// Implementation note: Products are deleted from last to first.  The first
	// product is the one currently displayed on the event page, so deleting the
	// first product first could permit another about-to-be-deleted product to
	// briefly appear on the event page.

	public static void deleteOafProducts (List<ComcatProductOaf> oafProducts, int keep,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		// Delete products from last to first

		for (int k = oafProducts.size() - 1; k >= 0; --k) {
			if (k != keep) {
				deleteOafProduct (oafProducts.get(k), eventNetwork, eventCode, isReviewed);
			}
		}

		return;
	}




	// Choose the code.
	// Parameters:
	//  suggestedCode = Code to use, if none is available from PDL.
	//  reviewOverwrite = Controls what happens if the most recent product from "us"
	//   is a reviewed product with a valid code:
	//     -1L = No special treatment.
	//      0L = Use the code from the most recent product.
	//      1L = Skip the PDL update (return null).
	//    > 1L = Skip the PDL update if the most recent product time is >= reviewOverwrite,
	//           otherwise use the code from the most recent product.
	//  geojson = GeoJSON for the event, or null if not available.  If not supplied,
	//   or if f_gj_prod does not correspond to the PDL destination, then this
	//   function retrieves the geojson from Comcat.
	//  f_gj_prod = True if geojson was retrieved from Comcat-production, false if it
	//   was retrieved from Comcat-development.  This is only used if geojson is non-null.
	//  queryID = Event ID used to query Comcat.  This is used if geojson is not supplied
	//   or if f_gj_prod does not correspond to the PDL destination.
	//  eventNetwork = Network identifier for the event (for example, "us").
	//  eventCode = Network code for the event (for example, "10006jv5").
	//  isReviewed = True if this product has been reviewed.
	//  gj_used = If included and non-null, returns the GeoJSON used for reading back from
	//   PDL.  Note the returned GeoJSON can be null if the event was not found, and may
	//   be the same as or different from the supplied geojson.
	// Returns the code to use as eventID when constructing the PDL product.
	// Returns null if the PDL send should be skipped.  This will never occur if
	//  reviewOverwrite equals -1L or 0L.

	public static String chooseOafCode (String suggestedCode, long reviewOverwrite,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		GeoJsonHolder gj_used = null;

		return chooseOafCode (suggestedCode, reviewOverwrite,
			geojson, f_gj_prod, queryID,
			eventNetwork, eventCode, isReviewed, gj_used);
	}

	public static String chooseOafCode (String suggestedCode, long reviewOverwrite,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			String eventNetwork, String eventCode, boolean isReviewed, GeoJsonHolder gj_used) throws Exception {

		// Get flag indicating if we should read back products from production

		boolean f_use_prod = (new ServerConfig()).get_is_pdl_readback_prod();

		// Get the geojson, reading from Comcat if necessary

		if (gj_used != null) {
			gj_used.set (geojson, f_gj_prod);
		}

		JSONObject my_geojson = geojson;

		if (f_gj_prod != f_use_prod) {
			my_geojson = null;
		}

		if (my_geojson == null) {
		
			// Get the accessor

			ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

			// Try to retrieve the event

			ObsEqkRupture rup = accessor.fetchEvent (queryID, false, true);

			// If not found, just use the suggested code

			if (rup == null) {
				if (gj_used != null) {
					gj_used.set (null, f_use_prod);
				}
				System.out.println ("Choosing suggested code '" + suggestedCode + "' because event '" + queryID + "' is not found");
				return suggestedCode;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		if (gj_used != null) {
			gj_used.set (my_geojson, f_use_prod);
		}

		// If no geojson, just use the suggested code

		if (my_geojson == null) {
			System.out.println ("Choosing suggested code '" + suggestedCode + "' because event '" + queryID + "' has no GeoJSON");
			return suggestedCode;
		}

		// Need to have event net, code, and ids

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() ) {
			
			System.out.println ("Choosing suggested code '" + suggestedCode + "' because event '" + queryID + "' GeoJSON does not contain net, code, and ids");
			return suggestedCode;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of OAF products

		List<ComcatProductOaf> oafProducts = ComcatProductOaf.make_list_from_gj (my_geojson);

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the product that contains the suggested code, with correct source and valid code

		int ix_suggested = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Loop over products to find indexes ...

		for (int k = 0; k < oafProducts.size(); ++k) {
			ComcatProductOaf oafProduct = oafProducts.get (k);

			// If the product is for our source ...

			if (oafProduct.sourceID.equals ((new ServerConfig()).get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || oafProduct.updateTime > oafProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// If the product has a valid code ...

				if (idlist.contains (oafProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || oafProduct.updateTime > oafProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}

					// Update product with suggested code

					if (ix_suggested == -1 && oafProduct.eventID.equals (suggestedCode)) {
						ix_suggested = k;
					}
				}
			}
		}

		// If the most recent product is reviewed and has a valid code, and we're giving such a product special treatment ...

		if (ix_recent >= 0 && reviewOverwrite >= 0L) {
			if (oafProducts.get(ix_recent).isReviewed && idlist.contains (oafProducts.get(ix_recent).eventID)) {

				// If we're checking overwrite block ...

				if (reviewOverwrite > 0L) {
		
					// Skip if the product is after the given time

					if (reviewOverwrite == 1L || oafProducts.get(ix_recent).updateTime >= reviewOverwrite) {

						System.out.println ("Skipping PDL update due to existing reviewed OAF product: " + oafProducts.get(ix_recent).summary_string());
						deleteOafProducts (oafProducts, ix_recent, eventNetwork, eventCode, isReviewed);
						return null;
					}
				}

				// Delete other products and return the code from this product

				System.out.println ("Choosing code from existing reviewed OAF product: " + oafProducts.get(ix_recent).summary_string());
				deleteOafProducts (oafProducts, ix_recent, eventNetwork, eventCode, isReviewed);
				return oafProducts.get(ix_recent).eventID;
			}
		}

		// If we found a product with suggested code and the code is valid ...

		if (ix_suggested >= 0) {

			// Delete other products and return the code from this product

			System.out.println ("Choosing suggested code from existing OAF product: " + oafProducts.get(ix_suggested).summary_string());
			deleteOafProducts (oafProducts, ix_suggested, eventNetwork, eventCode, isReviewed);
			return oafProducts.get(ix_suggested).eventID;
		}

		// If we found any product with a valid code ...

		if (ix_valid >= 0) {

			// Delete other products and return the code from this product

			System.out.println ("Choosing code from existing OAF product: " + oafProducts.get(ix_valid).summary_string());
			deleteOafProducts (oafProducts, ix_valid, eventNetwork, eventCode, isReviewed);
			return oafProducts.get(ix_valid).eventID;
		}

		// If the suggested code is valid ...

		if (idlist.contains (suggestedCode)) {

			// Delete all products and return the suggested code

			System.out.println ("Choosing suggested code '" + suggestedCode + "' because there is no existing code to use");
			deleteOafProducts (oafProducts, -1, eventNetwork, eventCode, isReviewed);
			return suggestedCode;
		}

		// If all else fails, delete all products and return the authoritative ID

		System.out.println ("Choosing authoritative ID '" + idlist.get(0) + "' to use as code");
		deleteOafProducts (oafProducts, -1, eventNetwork, eventCode, isReviewed);
		return idlist.get(0);
	}




	// Delete old OAF products associated with an event.
	// Parameters:
	//  f_prod = True to read from prod-Comcat, false to read from dev-Comcat,
	//           null to read from the configured PDL destination.
	//  geojson = GeoJSON for the event, or null if not available.  If not supplied,
	//   or if f_gj_prod does not correspond to the PDL destination, then this
	//   function retrieves the geojson from Comcat.
	//  f_gj_prod = True if geojson was retrieved from Comcat-production, false if it
	//   was retrieved from Comcat-development.  This is only used if geojson is non-null.
	//  queryID = Event ID used to query Comcat.  This is used only if geojson is null,
	//   or if f_gj_prod does not correspond to the PDL destination.
	//  isReviewed = True to set reviewed flag on the delete products.
	//  cutoff_time = Cutoff time in milliseconds since the epoch, or 0L if none.
	//  gj_used = If included and non-null, returns the GeoJSON used for reading back from
	//   PDL.  Note the returned GeoJSON can be null if the event was not found, and may
	//   be the same as or different from the supplied geojson.
	// Returns 0L (== DOOP_DELETED) if one or more products were deleted.
	// Returns > 0L if no product was deleted because the most recent product was
	//  updated at or after the cutoff time.  In this case, the return value is the
	//  time that the most recent product (from our source) was updated.
	// Returns < 0L if no product was deleted for some other reason.  In this case,
	//  the return value is one of the DOOP_XXXXX return codes.
	// If cutoff_time > 0L, then all products are deleted if the most recent product
	//  was updated before cutoff_time.  If cutoff_time == 0L then all products are
	//  deleted unconditionally.
	// Note: Throws ComcatException if Comcat access error.  Throws Exception if PDL
	//  error.  It is recommended that any exception be considered a transient condition.
	// Note: To be deleted, the update time of the most recent product (from our source)
	//  must be stricly less than cutoff_time (if cutoff_time > 0L).
	// Note: If the return value is > 0L and if cutoff_time > 0L, then it is guaranteed
	//  that the return value is >= cutoff_time.

	public static final long DOOP_DELETED = 0L;
		// One or more OAF products were deleted.
	public static final long DOOP_NOT_FOUND = -1L;
		// The event was not found in Comcat.
	public static final long DOOP_NO_GEOJSON = -2L;
		// The geojson could not be fetched.
	public static final long DOOP_INCOMPLETE = -3L;
		// The geojson contained incomplete information.
	public static final long DOOP_NO_PRODUCTS = -4L;
		// There are no OAF products (from any source) for the event.
	public static final long DOOP_FOREIGN = -5L;
		// There are no OAF products from our source, but there exists
		// an OAF product from another source.
	public static final long DOOP_BLOCKED = -6L;
		// A blocking condition (such as an existing relay item) prevents deletion
		// from being considered at this time
		// (not used here, but used in CleanupSupport).
	public static final long DOOP_COMCAT_EXCEPTION = -7L;
		// The operation could not be completed because of a Comcat exception
		// (not used here, but used in CleanupSupport).
	public static final long DOOP_PDL_EXCEPTION = -8L;
		// The operation could not be completed because of a PDL exception
		// (not used here, but used in CleanupSupport).
	public static final long DOOP_DB_EXCEPTION = -9L;
		// The operation could not be completed because of a database exception
		// (not used here, but used in CleanupSupport).

	public static long deleteOldOafProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cutoff_time) throws Exception {

		GeoJsonHolder gj_used = null;

		return deleteOldOafProducts (f_prod,
			geojson, f_gj_prod, queryID,
			isReviewed, cutoff_time, gj_used);
	}

	public static long deleteOldOafProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cutoff_time, GeoJsonHolder gj_used) throws Exception {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read back products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		if (f_prod != null) {
			f_use_prod = f_prod.booleanValue();
		}

		// Get the geojson, reading from Comcat if necessary

		if (gj_used != null) {
			gj_used.set (geojson, f_gj_prod);
		}

		JSONObject my_geojson = geojson;

		if (f_gj_prod != f_use_prod) {
			my_geojson = null;
		}

		if (my_geojson == null) {
		
			// Get the accessor

			ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

			// Try to retrieve the event

			ObsEqkRupture rup = accessor.fetchEvent (queryID, false, true);

			// If not found, nothing to delete

			if (rup == null) {
				if (gj_used != null) {
					gj_used.set (null, f_use_prod);
				}
				return DOOP_NOT_FOUND;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		if (gj_used != null) {
			gj_used.set (my_geojson, f_use_prod);
		}

		// If no geojson, nothing to delete

		if (my_geojson == null) {
			return DOOP_NO_GEOJSON;
		}

		// Need to have event net, code, ids, and time

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);
		Date event_time = GeoJsonUtils.getTime (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() || event_time == null ) {
			
			return DOOP_INCOMPLETE;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of OAF products

		List<ComcatProductOaf> oafProducts = ComcatProductOaf.make_list_from_gj (my_geojson);

		// If no OAF products, nothing to delete

		if (oafProducts.isEmpty()) {
			return DOOP_NO_PRODUCTS;
		}

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Loop over products to find indexes ...

		for (int k = 0; k < oafProducts.size(); ++k) {
			ComcatProductOaf oafProduct = oafProducts.get (k);

			// If the product is for our source ...

			if (oafProduct.sourceID.equals (server_config.get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || oafProduct.updateTime > oafProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// If the product has a valid code ...

				if (idlist.contains (oafProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || oafProduct.updateTime > oafProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}
				}
			}
		}

		// If no products for our source, nothing to delete

		if (ix_recent == -1) {
			return DOOP_FOREIGN;
		}

		// If not too old, nothing to delete

		if (cutoff_time > 0L) {
			if (oafProducts.get(ix_recent).updateTime >= cutoff_time) {
				return oafProducts.get(ix_recent).updateTime;	// note this must be positive and >= cutoff_time
			}
		}

		// Delete all OAF products from our source

		deleteOafProducts (oafProducts, -1, event_net, event_code, isReviewed);
		return DOOP_DELETED;
	}




	// Delete old OAF products associated with an event.
	// Parameters:
	//  f_prod = True to read from prod-Comcat, false to read from dev-Comcat,
	//           null to read from the configured PDL destination.
	//  geojson = GeoJSON for the event, or null if not available.  If not supplied,
	//   or if f_gj_prod does not correspond to the PDL destination, then this
	//   function retrieves the geojson from Comcat.
	//  f_gj_prod = True if geojson was retrieved from Comcat-production, false if it
	//   was retrieved from Comcat-development.  This is only used if geojson is non-null.
	//  queryID = Event ID used to query Comcat.  This is used only if geojson is null,
	//   or if f_gj_prod does not correspond to the PDL destination.
	//  isReviewed = True to set reviewed flag on the delete products.
	//  cutoff_time = Cutoff time in milliseconds since the epoch, or 0L if none.
	//  reviewed_time = Reviewed product time in milliseconds since the epoch, or 0L if none.
	//  gj_used = If included and non-null, returns the GeoJSON used for reading back from
	//   PDL.  Note the returned GeoJSON can be null if the event was not found, and may
	//   be the same as or different from the supplied geojson.
	// Returns 0L (== DOOP_DELETED) if one or more products were deleted.
	// Returns > 0L if no product was deleted because the most recent product was
	//  updated at or after the cutoff time.  In this case, the return value is the
	//  time that the most recent product (from our source) was updated.
	// Returns < 0L if no product was deleted for some other reason.  In this case,
	//  the return value is one of the DOOP_XXXXX return codes.
	// If cutoff_time > 0L, then all products are deleted if the most recent product
	//  was updated before cutoff_time.  If cutoff_time == 0L then all products are
	//  deleted unconditionally.
	// If reviewed_time > 0L, and there is a reviewed product (from our source) updated
	//  on or after reviewed_time, then the most recent reviewed product is treated
	//  as a foreign product.  In particular, it is never deleted, and if there are
	//  no other products to delete then the return is DOOP_FOREIGN.
	// Note: Throws ComcatException if Comcat access error.  Throws Exception if PDL
	//  error.  It is recommended that any exception be considered a transient condition.
	// Note: To be deleted, the update time of the most recent product (from our source)
	//  must be stricly less than cutoff_time (if cutoff_time > 0L).
	// Note: If the return value is > 0L and if cutoff_time > 0L, then it is guaranteed
	//  that the return value is >= cutoff_time.
	// Note: Calling deleteOldOafProducts_v2 with reviewed_time == 0L has the same
	//  effect as calling deleteOldOafProducts.

	public static long deleteOldOafProducts_v2 (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cutoff_time, long reviewed_time) throws Exception {

		GeoJsonHolder gj_used = null;

		return deleteOldOafProducts_v2 (f_prod,
			geojson, f_gj_prod, queryID,
			isReviewed, cutoff_time, reviewed_time, gj_used);
	}

	public static long deleteOldOafProducts_v2 (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cutoff_time, long reviewed_time, GeoJsonHolder gj_used) throws Exception {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read back products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		if (f_prod != null) {
			f_use_prod = f_prod.booleanValue();
		}

		// Get the geojson, reading from Comcat if necessary

		if (gj_used != null) {
			gj_used.set (geojson, f_gj_prod);
		}

		JSONObject my_geojson = geojson;

		if (f_gj_prod != f_use_prod) {
			my_geojson = null;
		}

		if (my_geojson == null) {
		
			// Get the accessor

			ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

			// Try to retrieve the event

			ObsEqkRupture rup = accessor.fetchEvent (queryID, false, true);

			// If not found, nothing to delete

			if (rup == null) {
				if (gj_used != null) {
					gj_used.set (null, f_use_prod);
				}
				return DOOP_NOT_FOUND;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		if (gj_used != null) {
			gj_used.set (my_geojson, f_use_prod);
		}

		// If no geojson, nothing to delete

		if (my_geojson == null) {
			return DOOP_NO_GEOJSON;
		}

		// Need to have event net, code, ids, and time

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);
		Date event_time = GeoJsonUtils.getTime (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() || event_time == null ) {
			
			return DOOP_INCOMPLETE;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of OAF products

		List<ComcatProductOaf> oafProducts = ComcatProductOaf.make_list_from_gj (my_geojson);

		// If no OAF products, nothing to delete

		if (oafProducts.isEmpty()) {
			return DOOP_NO_PRODUCTS;
		}

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Index of reviewed product to be treated as foreign

		int ix_reviewed = -1;

		// Count of products with our source

		int count_our_products = 0;

		// Loop over products to find indexes ...

		for (int k = 0; k < oafProducts.size(); ++k) {
			ComcatProductOaf oafProduct = oafProducts.get (k);

			// If the product is for our source ...

			if (oafProduct.sourceID.equals (server_config.get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || oafProduct.updateTime > oafProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// Count it

				++count_our_products;

				// Check for reviewed product that could be treated as foreign

				if (reviewed_time > 0L && oafProduct.isReviewed && oafProduct.updateTime >= reviewed_time) {
					if (ix_reviewed == -1 || oafProduct.updateTime > oafProducts.get(ix_reviewed).updateTime) {
						ix_reviewed = k;
					}
				}

				// If the product has a valid code ...

				if (idlist.contains (oafProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || oafProduct.updateTime > oafProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}
				}
			}
		}

		// If no products for our source, nothing to delete

		if (ix_recent == -1) {
			return DOOP_FOREIGN;
		}

		// If the only product for our source is a reviewed product treated as foreign, nothing to delete

		if (ix_reviewed != -1 && count_our_products == 1) {
			return DOOP_FOREIGN;
		}

		// If not too old, nothing to delete

		if (cutoff_time > 0L) {
			if (oafProducts.get(ix_recent).updateTime >= cutoff_time) {
				return oafProducts.get(ix_recent).updateTime;	// note this must be positive and >= cutoff_time
			}
		}

		// Delete all OAF products from our source, keeping any reviewed product treated as foreign

		//deleteOafProducts (oafProducts, -1, event_net, event_code, isReviewed);
		deleteOafProducts (oafProducts, ix_reviewed, event_net, event_code, isReviewed);
		return DOOP_DELETED;
	}




	// Return a string describing the deleteOldOafProducts return value.

	public static String get_doop_as_string (long doop) {
		if (doop > 0L) {
			return "DOOP_CURRENT updateTime = " + SimpleUtils.time_raw_and_string (doop);
		}

		//switch (doop) {
		//case DOOP_DELETED: return "DOOP_DELETED";
		//case DOOP_NOT_FOUND: return "DOOP_NOT_FOUND";
		//case DOOP_NO_GEOJSON: return "DOOP_NO_GEOJSON";
		//case DOOP_INCOMPLETE: return "DOOP_INCOMPLETE";
		//case DOOP_NO_PRODUCTS: return "DOOP_NO_PRODUCTS";
		//case DOOP_FOREIGN: return "DOOP_FOREIGN";
		//case DOOP_BLOCKED: return "DOOP_BLOCKED";
		//case DOOP_COMCAT_EXCEPTION: return "DOOP_COMCAT_EXCEPTION";
		//case DOOP_PDL_EXCEPTION: return "DOOP_PDL_EXCEPTION";
		//case DOOP_DB_EXCEPTION: return "DOOP_DB_EXCEPTION";
		//}

		if (doop == DOOP_DELETED) {return "DOOP_DELETED";}
		if (doop == DOOP_NOT_FOUND) {return "DOOP_NOT_FOUND";}
		if (doop == DOOP_NO_GEOJSON) {return "DOOP_NO_GEOJSON";}
		if (doop == DOOP_INCOMPLETE) {return "DOOP_INCOMPLETE";}
		if (doop == DOOP_NO_PRODUCTS) {return "DOOP_NO_PRODUCTS";}
		if (doop == DOOP_FOREIGN) {return "DOOP_FOREIGN";}
		if (doop == DOOP_BLOCKED) {return "DOOP_BLOCKED";}
		if (doop == DOOP_COMCAT_EXCEPTION) {return "DOOP_COMCAT_EXCEPTION";}
		if (doop == DOOP_PDL_EXCEPTION) {return "DOOP_PDL_EXCEPTION";}
		if (doop == DOOP_DB_EXCEPTION) {return "DOOP_DB_EXCEPTION";}

		return "DOOP_INVALID(" + doop + ")";
	}




	// Check for existence of old OAF products associated with an event.
	// Parameters:
	//  f_prod = True to read from prod-Comcat, false to read from dev-Comcat,
	//           null to read from the configured PDL destination.
	//  geojson = GeoJSON for the event, or null if not available.  If not supplied,
	//   or if f_gj_prod does not correspond to the PDL destination, then this
	//   function retrieves the geojson from Comcat.
	//  f_gj_prod = True if geojson was retrieved from Comcat-production, false if it
	//   was retrieved from Comcat-development.  This is only used if geojson is non-null.
	//  queryID = Event ID used to query Comcat.  This is used only if geojson is null,
	//   or if f_gj_prod does not correspond to the PDL destination.
	//  gj_used = If included and non-null, returns the GeoJSON used for reading back from
	//   PDL.  Note the returned GeoJSON can be null if the event was not found, and may
	//   be the same as or different from the supplied geojson.
	// Returns: The update time of the most recent OAF product, in milliseconds since the
	//  epoch, or 0L if there is no existing OAF product.
	// Note: Throws ComcatException if Comcat access error.  It is recommended that
	//  any exception be considered a transient condition.

	public static long checkOldOafProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID) {

		GeoJsonHolder gj_used = null;

		return checkOldOafProducts (f_prod,
			geojson, f_gj_prod, queryID, gj_used);
	}

	public static long checkOldOafProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID, GeoJsonHolder gj_used) {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read back products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		if (f_prod != null) {
			f_use_prod = f_prod.booleanValue();
		}

		// Get the geojson, reading from Comcat if necessary

		if (gj_used != null) {
			gj_used.set (geojson, f_gj_prod);
		}

		JSONObject my_geojson = geojson;

		if (f_gj_prod != f_use_prod) {
			my_geojson = null;
		}

		if (my_geojson == null) {
		
			// Get the accessor

			ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

			// Try to retrieve the event

			ObsEqkRupture rup = accessor.fetchEvent (queryID, false, true);

			// If not found, no product exists

			if (rup == null) {
				if (gj_used != null) {
					gj_used.set (null, f_use_prod);
				}
				return 0L;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		if (gj_used != null) {
			gj_used.set (my_geojson, f_use_prod);
		}

		// If no geojson, no product exists

		if (my_geojson == null) {
			return 0L;
		}

		// Need to have event net, code, ids, and time

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);
		Date event_time = GeoJsonUtils.getTime (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() || event_time == null ) {
			
			return 0L;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of OAF products

		List<ComcatProductOaf> oafProducts = ComcatProductOaf.make_list_from_gj (my_geojson);

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Loop over products to find indexes ...

		for (int k = 0; k < oafProducts.size(); ++k) {
			ComcatProductOaf oafProduct = oafProducts.get (k);

			// If the product is for our source ...

			if (oafProduct.sourceID.equals (server_config.get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || oafProduct.updateTime > oafProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// If the product has a valid code ...

				if (idlist.contains (oafProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || oafProduct.updateTime > oafProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}
				}
			}
		}

		// If no products for our source, no product exists

		if (ix_recent == -1) {
			return 0L;
		}

		// Return the time of the most recent product

		return oafProducts.get(ix_recent).updateTime;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("PDLCodeChooserOaf : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  pdl_enable  pdl_key_filename  suggestedCode   reviewOverwrite  queryID  eventNetwork  eventCode  isReviewed
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call chooseOafCode and display the result.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("PDLCodeChooserOaf : Invalid 'test1' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String suggestedCode = args[3];
				long reviewOverwrite = Long.parseLong (args[4]);
				String queryID = args[5];
				String eventNetwork = args[6];
				String eventCode = args[7];
				boolean isReviewed = Boolean.parseBoolean (args[8]);

				// Say hello

				System.out.println ("Choosing code for OAF product");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("suggestedCode: " + suggestedCode);
				System.out.println ("reviewOverwrite: " + reviewOverwrite);
				System.out.println ("queryID: " + queryID);
				System.out.println ("eventNetwork: " + eventNetwork);
				System.out.println ("eventCode: " + eventCode);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("");

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (!( pdl_key_filename.equals("-") )) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the call

				JSONObject geojson = null;
				boolean f_gj_prod = true;

				String chosen_code = PDLCodeChooserOaf.chooseOafCode (suggestedCode, reviewOverwrite,
						geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed);

				// Display result

				System.out.println ((chosen_code == null) ? "<null>" : chosen_code);

			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  pdl_enable  pdl_key_filename  query_id
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call checkOldOafProducts and display the result.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("PDLCodeChooserOaf : Invalid 'test2' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String query_id = args[3];

				// Say hello

				System.out.println ("Checking for old OAF products");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("query_id: " + query_id);
				System.out.println ("");

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (!( pdl_key_filename.equals("-") )) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the call

				JSONObject geojson = null;
				boolean f_gj_prod = true;

				long result = PDLCodeChooserOaf.checkOldOafProducts (null,
					geojson, f_gj_prod, query_id);

				// Display result

				System.out.println ("PDLCodeChooserOaf.checkOldOafProducts returned: " + SimpleUtils.time_raw_and_string(result));
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  pdl_key_filename  query_id  isReviewed  cutoff_time
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call deleteOldOafProducts and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Times before 1970-01-01 are converted to zero.
		// Times can also be entered as an integer number of milliseconds, or as "now".

		if (args[0].equalsIgnoreCase ("test3")) {

			// 5 additional arguments

			if (args.length != 6) {
				System.err.println ("PDLCodeChooserOaf : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String query_id = args[3];
				boolean isReviewed = Boolean.parseBoolean (args[4]);
				long cutoff_time = SimpleUtils.string_or_number_or_now_to_time (args[5]);
				if (cutoff_time < 0L) {
					cutoff_time = 0L;
				}

				// Say hello

				System.out.println ("Deleting old OAF products");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("query_id: " + query_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("cutoff_time: " + SimpleUtils.time_raw_and_string(cutoff_time));
				System.out.println ("");

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (!( pdl_key_filename.equals("-") )) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the call

				JSONObject geojson = null;
				boolean f_gj_prod = true;

				long result = PDLCodeChooserOaf.deleteOldOafProducts (null,
					geojson, f_gj_prod, query_id, isReviewed, cutoff_time);

				// Display result

				if (result > 0L) {
					System.out.println ("PDLCodeChooserOaf.deleteOldOafProducts returned: " + SimpleUtils.time_raw_and_string(result));
				} else {
					System.out.println ("PDLCodeChooserOaf.deleteOldOafProducts returned: " + result);
				}
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  pdl_enable  pdl_key_filename  query_id  isReviewed  cutoff_time  reviewed_time
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile).
		// Then call deleteOldOafProducts_v2 and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Times before 1970-01-01 are converted to zero.
		// Times can also be entered as an integer number of milliseconds, or as "now".

		if (args[0].equalsIgnoreCase ("test4")) {

			// 6 additional arguments

			if (args.length != 7) {
				System.err.println ("PDLCodeChooserOaf : Invalid 'test4' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String query_id = args[3];
				boolean isReviewed = Boolean.parseBoolean (args[4]);
				long cutoff_time = SimpleUtils.string_or_number_or_now_to_time (args[5]);
				if (cutoff_time < 0L) {
					cutoff_time = 0L;
				}
				long reviewed_time = SimpleUtils.string_or_number_or_now_to_time (args[6]);
				if (reviewed_time < 0L) {
					reviewed_time = 0L;
				}

				// Say hello

				System.out.println ("Deleting old OAF products");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("query_id: " + query_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("cutoff_time: " + SimpleUtils.time_raw_and_string(cutoff_time));
				System.out.println ("reviewed_time: " + SimpleUtils.time_raw_and_string(reviewed_time));
				System.out.println ("");

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (!( pdl_key_filename.equals("-") )) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the call

				JSONObject geojson = null;
				boolean f_gj_prod = true;

				long result = PDLCodeChooserOaf.deleteOldOafProducts_v2 (null,
					geojson, f_gj_prod, query_id, isReviewed, cutoff_time, reviewed_time);

				// Display result

				if (result > 0L) {
					System.out.println ("PDLCodeChooserOaf.deleteOldOafProducts returned: " + SimpleUtils.time_raw_and_string(result));
				} else {
					System.out.println ("PDLCodeChooserOaf.deleteOldOafProducts returned: " + result);
				}
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("PDLCodeChooserOaf : Unrecognized subcommand : " + args[0]);
		return;

	}















}
