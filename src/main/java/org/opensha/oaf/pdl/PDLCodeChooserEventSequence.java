package org.opensha.oaf.pdl;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Date;

import java.io.File;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatProduct;
import org.opensha.oaf.comcat.ComcatProductEventSequence;
import org.opensha.oaf.comcat.PropertiesEventSequence;
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
import org.opensha.oaf.aafs.VersionInfo;

import org.opensha.oaf.util.SimpleUtils;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.distribution.SocketProductSender;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;



/**
 * Routines to select the "code" used for the PDL product of type "event-sequence".
 * Author: Michael Barall 10/12/2022.
 *
 * A PDL product is identified by "source" and "code".  A product with the
 * same source and code as an earlier product will replace the earlier product.
 * But a product with a different source and code as an earlier product is
 * kept in Comcat in addition to the earlier product, even if it is the same
 * type and associated to the same earthquake.
 *
 * We always use "event-sequence" as the type and "us" as the source.  We would
 * like to use the authoritative ID as the code, but this creates problems when
 * the authoritative ID changes.  The automatic system tries to handle with by
 * using the timeline ID, but the GUI has no simple solution.
 *
 * (The string "us" comes from ServerConfig.json.  The string "event-sequence"
 * is ComcatProduct.PRODTYPE_EVENT_SEQUENCE.)
 *
 * Typically an event-sequence product is issued together with an OAF product.
 * In this case, the code is the same code used for the OAF product.
 *
 * If a stand-alone event-sequence product is issued, our approach is to choose
 * the code by reading the event-sequence product that already exists in Comcat
 * and re-using its code.  If none exist, then we choose the code suggested by
 * the caller.
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
 * different in the production and development servers, we read the event-sequence
 * product from the server that is the intended destination of the new product.
 *
 * (d) There is a mechanism to avoid replacing reviewed with non-reviewed
 * products, if that is desired.
 */
public class PDLCodeChooserEventSequence {

	// Our type.

	//public static final String TYPE_EVENT_SEQUENCE = "event-sequence";

	// Our source.

	//public static final String SOURCE_US = "us";




	// Delete an existing event-sequence product.
	// Parameters:
	//  evseqProduct = Product to delete.
	//  eventNetwork = Network identifier for the event (for example, "us").
	//  eventCode = Network code for the event (for example, "10006jv5").
	//  isReviewed = True if this product has been reviewed.
	// If the source in evseqProduct is not "us" then no action is performed.
	// Otherwise, the product is deleted using the code from within the product.
	// Throws an exception in case of error.

	public static void deleteEventSequenceProduct (ComcatProductEventSequence evseqProduct,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		// If not our source ID, do nothing

		if (!( evseqProduct.sourceID.equals ((new ServerConfig()).get_pdl_oaf_source()) )) {
			return;
		}
			
		System.out.println ("Deleting existing event-sequence product: " + evseqProduct.summary_string());

		// The code

		String pdl_code = evseqProduct.eventID;

		// Make event-sequence properties for a delete product

		PropertiesEventSequence props = new PropertiesEventSequence();

		if (!( props.setup_from_event_network_and_code_for_delete (eventNetwork, eventCode) )) {
			throw new IllegalArgumentException ("PDLCodeChooserEventSequence.deleteEventSequenceProduct: Failed to set up event-sequence properties for delete product");
		}

		// Modification time, 0 means now

		long modifiedTime = 0L;

		// No extra properties

		Map<String, String> extra_properties = null;

		// Build the product

		Product product = PDLProductBuilderEventSequence.createDeletionProduct (
			pdl_code, props, extra_properties, isReviewed, modifiedTime);

		// Sign the product

		PDLSender.signProduct (product);

		// Send the product, true means it is text

		PDLSender.sendProduct (product, true);

		return;
	}




	// Delete a list of existing event-sequence products.
	// Parameters:
	//  evseqProducts = List of products to delete.
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
	// product is the one currently used on the event pages, so deleting the
	// first product first could permit another about-to-be-deleted product to
	// briefly be used on the event pages.

	public static void deleteEventSequenceProducts (List<ComcatProductEventSequence> evseqProducts, int keep,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		// Delete products from last to first

		for (int k = evseqProducts.size() - 1; k >= 0; --k) {
			if (k != keep) {
				deleteEventSequenceProduct (evseqProducts.get(k), eventNetwork, eventCode, isReviewed);
			}
		}

		return;
	}




	// Send an event-sequence product.
	// Parameters:
	//  props = Event-sequence properties.
	//  pdl_code = Code to use for sending the product.
	//  isReviewed = True if this product has been reviewed.
	//  generator = Generator property value, if null then don't write a generator,
	//    if empty string or omitted then use our default generator.
	// Note: The network identifier and network code (eventNetwork and eventCode)
	//  are in the event-sequence properties.
	// Throws an exception in case of error.

	public static void sendEventSequenceProduct (PropertiesEventSequence props,
			String pdl_code, boolean isReviewed) throws Exception {

		String generator = VersionInfo.get_generator_name();
		sendEventSequenceProduct (props, pdl_code, isReviewed, generator);
		return;
	}

	public static void sendEventSequenceProduct (PropertiesEventSequence props,
			String pdl_code, boolean isReviewed, String generator) throws Exception {

		// Set up extra properties containing the generator

		Map<String, String> extra_properties = null;
		if (generator != null) {
			extra_properties = new LinkedHashMap<String, String>();
			if (generator.isEmpty()) {
				extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, VersionInfo.get_generator_name());
			} else {
				extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, generator);
			}
		}

		// Make the PDL product

		String jsonText = null;
		long modifiedTime = 0L;

		Product product = PDLProductBuilderEventSequence.createProduct (
			pdl_code, props, extra_properties, isReviewed, jsonText, modifiedTime);

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		return;
	}




	// Cap or delete an existing event-sequence product.
	// Parameters:
	//  evseqProduct = Product to cap or delete.
	//  cap_time = Sequence cap time, in milliseconds since the epoch, or 0L.
	//  eventNetwork = Network identifier for the event (for example, "us").
	//  eventCode = Network code for the event (for example, "10006jv5").
	//  isReviewed = True if this product has been reviewed.
	// Returns PropertiesEventSequence.CET_CAPPED if the product was capped.
	// Returns PropertiesEventSequence.CET_INVALID if the product was deleted.
	// Returns PropertiesEventSequence.CET_UNCHANGED if the product retained unchanged.
	// If the source in evseqProduct is not "us" then no action is performed.
	// Otherwise, if cap_time is 0, the product is deleted.
	// Otherwise, if cap_time is at or after the existing end time of the product,
	//  then the product is left unmodified.
	// Otherwise, if cap_time is sufficiently after the start time, and also
	//  sufficiently after the event time if the product has an event time,
	//  then the product is updated with the end time equal to cap_time.
	// Otherwise, the product is deleted
	// Update and deletion are done using the code from within the product.
	// Throws an exception in case of error.

	public static int capEventSequenceProduct (ComcatProductEventSequence evseqProduct, long cap_time,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		// If not our source ID, do nothing

		if (!( evseqProduct.sourceID.equals ((new ServerConfig()).get_pdl_oaf_source()) )) {
			return PropertiesEventSequence.CET_UNCHANGED;
		}

		// If unconditional delete, do it

		if (cap_time == 0L) {
			deleteEventSequenceProduct (evseqProduct, eventNetwork, eventCode, isReviewed);
			return PropertiesEventSequence.CET_INVALID;
		}

		// Check if end time can be capped

		int cet_retval = evseqProduct.evs_properties.check_cap_end_time (cap_time);

		// If capping would be invalid, delete the product

		if (cet_retval == PropertiesEventSequence.CET_INVALID) {
			deleteEventSequenceProduct (evseqProduct, eventNetwork, eventCode, isReviewed);
			return PropertiesEventSequence.CET_INVALID;
		}

		// If we can cap the product ...

		if (cet_retval == PropertiesEventSequence.CET_CAPPED) {
			
			//System.out.println ("Capping existing event-sequence product at " + SimpleUtils.time_to_string(cap_time) + ": " + evseqProduct.summary_string());
			
			System.out.println ("Capping existing event-sequence product: " + evseqProduct.summary_string());
			System.out.println ("Cap time = " + SimpleUtils.time_to_string (cap_time));

			// Make a copy of the properties, since we need to modify it

			PropertiesEventSequence props = (new PropertiesEventSequence()).copy_from (evseqProduct.evs_properties);

			// Cap the end time

			if (!( props.set_end_time (cap_time) )) {
				throw new IllegalArgumentException ("PDLCodeChooserEventSequence.capEventSequenceProduct: Failed to set up event-sequence properties to cap product");
			}

			// Update the network and network code

			if (!( props.overwrite_event_network_and_code (eventNetwork, eventCode) )) {
				throw new IllegalArgumentException ("PDLCodeChooserEventSequence.capEventSequenceProduct: Failed to set up event-sequence properties to cap product");
			}

			// The code

			String pdl_code = evseqProduct.eventID;

			// Get the generator, or null if none

			String generator = evseqProduct.get_generator();

			// Send the updated product

			sendEventSequenceProduct (props, pdl_code, isReviewed, generator);
			return PropertiesEventSequence.CET_CAPPED;
		}

		// Otherwise, retain the product
			
		System.out.println ("Retaining (without capping) existing event-sequence product: " + evseqProduct.summary_string());
		System.out.println ("Requested cap time = " + SimpleUtils.time_to_string (cap_time));

		return PropertiesEventSequence.CET_UNCHANGED;
	}




	// Choose the code.
	// Parameters:
	//  suggestedCode = Code to use, if none is available from PDL.
	//  f_valid_ok = True if the function can return any existing valid code in PDL,
	//   false if only the suggested code can be returned (see details below).
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
	// If f_valid_ok is false, then:
	//  - If reviewOverwrite equals -1L, then the return value is always suggestedCode.
	//  - If reviewOverwrite >= 0L, and the most recent product from "us" is a reviewed
	//    product with a valid code, then the return value is either null or the code
	//    from the most recent such product.
	//  - If reviewOverwrite >= 0L, and the most recent product from "us" is not a reviewed
	//    product with a valid code, then the return value is suggestedCode.

	public static String chooseEventSequenceCode (String suggestedCode, boolean f_valid_ok, long reviewOverwrite,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			String eventNetwork, String eventCode, boolean isReviewed) throws Exception {

		GeoJsonHolder gj_used = null;

		return chooseEventSequenceCode (suggestedCode, f_valid_ok, reviewOverwrite,
			geojson, f_gj_prod, queryID,
			eventNetwork, eventCode, isReviewed, gj_used);
	}

	public static String chooseEventSequenceCode (String suggestedCode, boolean f_valid_ok, long reviewOverwrite,
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
				System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "' because event '" + queryID + "' is not found");
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
			System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "' because event '" + queryID + "' has no GeoJSON");
			return suggestedCode;
		}

		// Need to have event net, code, and ids

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() ) {
			
			System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "' because event '" + queryID + "' GeoJSON does not contain net, code, and ids");
			return suggestedCode;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of event-sequence products

		List<ComcatProductEventSequence> evseqProducts = ComcatProductEventSequence.make_list_from_gj (my_geojson);

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the product that contains the suggested code, with correct source and valid code

		int ix_suggested = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Loop over products to find indexes ...

		for (int k = 0; k < evseqProducts.size(); ++k) {
			ComcatProductEventSequence evseqProduct = evseqProducts.get (k);

			// If the product is for our source ...

			if (evseqProduct.sourceID.equals ((new ServerConfig()).get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || evseqProduct.updateTime > evseqProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// If the product has a valid code ...

				if (idlist.contains (evseqProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || evseqProduct.updateTime > evseqProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}

					// Update product with suggested code

					if (ix_suggested == -1 && evseqProduct.eventID.equals (suggestedCode)) {
						ix_suggested = k;
					}
				}
			}
		}

		// If the most recent product is reviewed and has a valid code, and we're giving such a product special treatment ...

		if (ix_recent >= 0 && reviewOverwrite >= 0L) {
			if (evseqProducts.get(ix_recent).isReviewed && idlist.contains (evseqProducts.get(ix_recent).eventID)) {

				// If we're checking overwrite block ...

				if (reviewOverwrite > 0L) {
		
					// Skip if the product is after the given time

					if (reviewOverwrite == 1L || evseqProducts.get(ix_recent).updateTime >= reviewOverwrite) {

						System.out.println ("Skipping PDL update due to existing reviewed event-sequence product: " + evseqProducts.get(ix_recent).summary_string());
						deleteEventSequenceProducts (evseqProducts, ix_recent, eventNetwork, eventCode, isReviewed);
						return null;
					}
				}

				// Delete other products and return the code from this product

				System.out.println ("Choosing code from existing reviewed event-sequence product: " + evseqProducts.get(ix_recent).summary_string());
				deleteEventSequenceProducts (evseqProducts, ix_recent, eventNetwork, eventCode, isReviewed);
				return evseqProducts.get(ix_recent).eventID;
			}
		}

		// If we found a product with suggested code and the code is valid ...

		if (ix_suggested >= 0) {

			// Delete other products and return the code from this product

			System.out.println ("Choosing suggested code from existing event-sequence product: " + evseqProducts.get(ix_suggested).summary_string());
			deleteEventSequenceProducts (evseqProducts, ix_suggested, eventNetwork, eventCode, isReviewed);
			return evseqProducts.get(ix_suggested).eventID;
		}

		// If we found any product with a valid code ...

		if (f_valid_ok && ix_valid >= 0) {

			// Delete other products and return the code from this product

			System.out.println ("Choosing code from existing event-sequence product: " + evseqProducts.get(ix_valid).summary_string());
			deleteEventSequenceProducts (evseqProducts, ix_valid, eventNetwork, eventCode, isReviewed);
			return evseqProducts.get(ix_valid).eventID;
		}

		// If the suggested code is valid ...

		if (idlist.contains (suggestedCode)) {

			// Delete all products and return the suggested code

			if (ix_valid >= 0) {
				System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "' overriding an existing code");
			} else if (f_valid_ok) {
				System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "' because there is no existing code to use");
			} else {
				System.out.println ("Choosing suggested event-sequence code '" + suggestedCode + "'");
			}
			deleteEventSequenceProducts (evseqProducts, -1, eventNetwork, eventCode, isReviewed);
			return suggestedCode;
		}

		// If all else fails, delete all products and return the authoritative ID

		System.out.println ("Choosing authoritative ID '" + idlist.get(0) + "' to use as event-sequence code");
		deleteEventSequenceProducts (evseqProducts, -1, eventNetwork, eventCode, isReviewed);
		return idlist.get(0);
	}




	// Delete or cap old event-sequence products associated with an event.
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
	//  cap_time = Sequence cap time, in milliseconds since the epoch, or 0L.
	//  f_keep_reviewed = True to keep reviewed products, as if they were foreign.
	//  gj_used = If included and non-null, returns the GeoJSON used for reading back from
	//   PDL.  Note the returned GeoJSON can be null if the event was not found, and may
	//   be the same as or different from the supplied geojson.
	// Returns one of the DOESP_XXXXX codes.
	// If there are no products from any source, the return is DOESP_NO_PRODUCTS.
	// If f_keep_reviewed is true, and there is a reviewed product (from our source),
	//  then the most recent reviewed product is treated as a foreign product, so
	//  it is never deleted.  If there are other products from our source they are
	//  deleted and the return is DOESP_DELETED; otherwise the return is DOESP_FOREIGN.
	// Otherwise, if there is a product from our source with a valid code, then an
	//  attempt is made to cap it, and all other products from our source are deleted.
	//  (Note that if cap_time is 0L then the capping attempt will always delete the
	//  product.)  If the capped product was updated then the return is DOESP_CAPPED;
	//  otherwise if any product was deleted then the return is DOESP_DELETED;
	//  otherwise the return is DOESP_RETAINED.
	// Otherwise, if there any products from our source (which must not have valid
	//  codes), they are all deleted and the return is DOESP_DELETED.
	// Otherwise there are products from other sources but none from our souce, and
	//  the return is DOESP_FOREIGN.
	// Note: Returns DOESP_CAPPED and DOESP_DELETED indicate that some product was
	//  changed; all other returns mean that nothing was changed in PDL.
	// Note: Throws ComcatException if Comcat access error.  Throws Exception if PDL
	//  error.  It is recommended that any exception be considered a transient condition.

	public static final int DOESP_NOP = 0;
		// No action was performed.
		// (Not used here, but can be used elsewhere to indicate this function was not called.)
	public static final int DOESP_CAPPED = 1;
		// An event-sequence product was capped.
		// Other event-sequence products might have been deleted.
	public static final int DOESP_DELETED = 2;
		// One or more event-sequence products were deleted.
		// No event-sequence products were capped.
		// An event-sequence product might have been retained.
	public static final int DOESP_RETAINED = 3;
		// An event-sequence product was retained because it ended before the cap time.
		// No event-sequence products were capped or deleted.
	public static final int DOESP_NOT_FOUND = 4;
		// The event was not found in Comcat.
	public static final int DOESP_NO_GEOJSON = 5;
		// The geojson could not be fetched.
	public static final int DOESP_INCOMPLETE = 6;
		// The geojson contained incomplete information.
	public static final int DOESP_NO_PRODUCTS = 7;
		// There are no OAF products (from any source) for the event.
	public static final int DOESP_FOREIGN = 8;
		// There are no event-sequence products from our source, but there exists
		// an event-sequence product from another source.

	public static int deleteOldEventSequenceProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cap_time, boolean f_keep_reviewed) throws Exception {

		GeoJsonHolder gj_used = null;

		return deleteOldEventSequenceProducts (f_prod,
			geojson, f_gj_prod, queryID,
			isReviewed, cap_time, f_keep_reviewed, gj_used);
	}

	public static int deleteOldEventSequenceProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID,
			boolean isReviewed, long cap_time, boolean f_keep_reviewed, GeoJsonHolder gj_used) throws Exception {

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
				return DOESP_NOT_FOUND;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		if (gj_used != null) {
			gj_used.set (my_geojson, f_use_prod);
		}

		// If no geojson, nothing to delete

		if (my_geojson == null) {
			return DOESP_NO_GEOJSON;
		}

		// Need to have event net, code, ids, and time

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);
		Date event_time = GeoJsonUtils.getTime (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() || event_time == null ) {
			
			return DOESP_INCOMPLETE;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Get the list of event-sequence products

		List<ComcatProductEventSequence> evseqProducts = ComcatProductEventSequence.make_list_from_gj (my_geojson);

		// If no event-sequence products, nothing to delete

		if (evseqProducts.isEmpty()) {
			return DOESP_NO_PRODUCTS;
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

		for (int k = 0; k < evseqProducts.size(); ++k) {
			ComcatProductEventSequence evseqProduct = evseqProducts.get (k);

			// If the product is for our source ...

			if (evseqProduct.sourceID.equals (server_config.get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || evseqProduct.updateTime > evseqProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// Count it

				++count_our_products;

				// Check for reviewed product that could be treated as foreign

				if (f_keep_reviewed && evseqProduct.isReviewed) {
					if (ix_reviewed == -1 || evseqProduct.updateTime > evseqProducts.get(ix_reviewed).updateTime) {
						ix_reviewed = k;
					}
				}

				// If the product has a valid code ...

				if (idlist.contains (evseqProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || evseqProduct.updateTime > evseqProducts.get(ix_valid).updateTime) {
						ix_valid = k;
					}
				}
			}
		}

		// If no products for our source, nothing to delete

		if (ix_recent == -1) {
			return DOESP_FOREIGN;
		}

		// If there is a reviewed product from our source to treat as foreign ...

		if (ix_reviewed != -1) {

			// If there are are other products from our source, delete them, but keeping the reviewed product

			if (count_our_products > 1) {
				deleteEventSequenceProducts (evseqProducts, ix_reviewed, event_net, event_code, isReviewed);
				return DOESP_DELETED;
			}

			// Otherwise, treat the reviewed product as foreign

			return DOESP_FOREIGN;
		}

		// If there is a product from our source with a valid code ...

		if (ix_valid != -1) {

			// Delete all products from our source except for the most recent one with a valid code

			deleteEventSequenceProducts (evseqProducts, ix_valid, event_net, event_code, isReviewed);

			// Attempt to cap the selected product

			int cet = capEventSequenceProduct (evseqProducts.get(ix_valid), cap_time, event_net, event_code, isReviewed);

			if (cet == PropertiesEventSequence.CET_CAPPED) {
				return DOESP_CAPPED;
			}

			if (cet == PropertiesEventSequence.CET_INVALID || count_our_products > 1) {
				return DOESP_DELETED;
			}

			return DOESP_RETAINED;
		}

		// Otherwise, if there are products frpm our source (which must have invalid codes), delete them

		if (count_our_products > 0) {
			deleteEventSequenceProducts (evseqProducts, -1, event_net, event_code, isReviewed);
			return DOESP_DELETED;
		}

		// Otherwise, there must be at least one product from another source but none from our source

		return DOESP_FOREIGN;
	}




	// Return a string describing the deleteOldEventSequenceProducts return value.

	public static String get_doesp_as_string (int doesp) {

		switch (doesp) {
		case DOESP_NOP: return "DOESP_NOP";
		case DOESP_CAPPED: return "DOESP_CAPPED";
		case DOESP_DELETED: return "DOESP_DELETED";
		case DOESP_RETAINED: return "DOESP_RETAINED";
		case DOESP_NOT_FOUND: return "DOESP_NOT_FOUND";
		case DOESP_NO_GEOJSON: return "DOESP_NO_GEOJSON";
		case DOESP_INCOMPLETE: return "DOESP_INCOMPLETE";
		case DOESP_NO_PRODUCTS: return "DOESP_NO_PRODUCTS";
		case DOESP_FOREIGN: return "DOESP_FOREIGN";
		}

		return "DOESP_INVALID(" + doesp + ")";
	}




	// Check for existence of old event-sequence products associated with an event.
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

	public static long checkOldEventSequenceProducts (Boolean f_prod,
			JSONObject geojson, boolean f_gj_prod, String queryID) {

		GeoJsonHolder gj_used = null;

		return checkOldEventSequenceProducts (f_prod,
			geojson, f_gj_prod, queryID, gj_used);
	}

	public static long checkOldEventSequenceProducts (Boolean f_prod,
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

		List<ComcatProductEventSequence> evseqProducts = ComcatProductEventSequence.make_list_from_gj (my_geojson);

		// Index of the most recent product, with correct source

		int ix_recent = -1;

		// Index of the most recent product, with correct source and valid code

		int ix_valid = -1;

		// Loop over products to find indexes ...

		for (int k = 0; k < evseqProducts.size(); ++k) {
			ComcatProductEventSequence evseqProduct = evseqProducts.get (k);

			// If the product is for our source ...

			if (evseqProduct.sourceID.equals (server_config.get_pdl_oaf_source())) {

				// Update most recent product

				if (ix_recent == -1 || evseqProduct.updateTime > evseqProducts.get(ix_recent).updateTime) {
					ix_recent = k;
				}

				// If the product has a valid code ...

				if (idlist.contains (evseqProduct.eventID)) {

					// Update most recent product with valid code

					if (ix_valid == -1 || evseqProduct.updateTime > evseqProducts.get(ix_valid).updateTime) {
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

		return evseqProducts.get(ix_recent).updateTime;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("PDLCodeChooserEventSequence : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  pdl_enable  pdl_key_filename  suggestedCode  f_valid_ok   reviewOverwrite  queryID  eventNetwork  eventCode  isReviewed
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call chooseEventSequenceCode and display the result.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 9 additional arguments

			if (args.length != 10) {
				System.err.println ("PDLCodeChooserEventSequence : Invalid 'test1' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String suggestedCode = args[3];
				boolean f_valid_ok = Boolean.parseBoolean (args[4]);
				long reviewOverwrite = Long.parseLong (args[5]);
				String queryID = args[6];
				String eventNetwork = args[7];
				String eventCode = args[8];
				boolean isReviewed = Boolean.parseBoolean (args[9]);

				// Say hello

				System.out.println ("Choosing code for event-sequence product");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("suggestedCode: " + suggestedCode);
				System.out.println ("f_valid_ok: " + f_valid_ok);
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

				String chosen_code = PDLCodeChooserEventSequence.chooseEventSequenceCode (suggestedCode, f_valid_ok, reviewOverwrite,
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
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call checkOldEventSequenceProducts and display the result.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("PDLCodeChooserEventSequence : Invalid 'test2' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String query_id = args[3];

				// Say hello

				System.out.println ("Checking for old event-sequence products");
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

				long result = PDLCodeChooserEventSequence.checkOldEventSequenceProducts (null,
					geojson, f_gj_prod, query_id);

				// Display result

				System.out.println ("PDLCodeChooserEventSequence.checkOldEventSequenceProducts returned: " + SimpleUtils.time_raw_and_string(result));
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  pdl_key_filename  query_id  isReviewed  cap_time  f_keep_reviewed
		// The PDL key filename can be "-" for none.
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call deleteOldEventSequenceProducts and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Times before 1970-01-01 are converted to zero.
		// Times can also be entered as an integer number of milliseconds, or as "now".

		if (args[0].equalsIgnoreCase ("test3")) {

			// 6 additional arguments

			if (args.length != 7) {
				System.err.println ("PDLCodeChooserEventSequence : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				String pdl_key_filename = args[2];
				String query_id = args[3];
				boolean isReviewed = Boolean.parseBoolean (args[4]);
				long cap_time = SimpleUtils.string_or_number_or_now_to_time (args[5]);
				if (cap_time < 0L) {
					cap_time = 0L;
				}
				boolean f_keep_reviewed = Boolean.parseBoolean (args[6]);

				// Say hello

				System.out.println ("Deleting or capping old event-sequence products");
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + pdl_key_filename);
				System.out.println ("query_id: " + query_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("cap_time: " + SimpleUtils.time_raw_and_string(cap_time));
				System.out.println ("f_keep_reviewed: " + f_keep_reviewed);
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

				int result = PDLCodeChooserEventSequence.deleteOldEventSequenceProducts (null,
					geojson, f_gj_prod, query_id, isReviewed, cap_time, f_keep_reviewed);

				// Display result
					
				System.out.println ("PDLCodeChooserEventSequence.deleteOldEventSequenceProducts returned: " + result + " (" + PDLCodeChooserEventSequence.get_doesp_as_string(result) + ")");
			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("PDLCodeChooserEventSequence : Unrecognized subcommand : " + args[0]);
		return;

	}















}
