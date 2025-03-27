package org.opensha.oaf.pdl;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.distribution.SocketProductSender;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import gov.usgs.util.CryptoUtils;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.net.MalformedURLException;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.util.health.HealthMonitor;


// Used for dumping products
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


/**
 * Code to send products to PDL.
 * Author: Michael Barall 06/05/2018.
 */
public class PDLSender {

	// The index number of the last sender that was used successfully.

	private static int last_sender_index = 0;

	// The time at which the last change in sender occurred, in milliseconds since the epoch.

	private static long last_sender_change_time = 0L;

	// The amount of time that secondary senders are used before trying the primary sender again, in milliseconds.

	public static final long SECONDARY_SENDER_TIME = 259200000L;		// 3 days

	// Get the index number of the last sender that was used successfully.

	public static synchronized int get_last_sender_index () {
		return last_sender_index;
	}

	// Get the index number of the first sender to try.
	// Parameters:
	//  sender_count = Number of senders available.
	//  time_now = Current time, in milliseconds since the epoch.

	private static synchronized int get_first_sender_index (int sender_count, long time_now) {
		int index = last_sender_index;
		if (index < 0 || index >= sender_count || time_now - last_sender_change_time > SECONDARY_SENDER_TIME) {
			index = 0;
		}
		return index;
	}

	// Set the index number of the last successful sender.
	// Parameters:
	//  index = Index number of the successful sender.
	//  time_now = Current time, in milliseconds since the epoch.

	private static synchronized void set_last_sender_index (int index, long time_now) {
		if (index != last_sender_index) {
			last_sender_index = index;
			last_sender_change_time = time_now;
		}
		return;
	}




	// The health monitor for the PDL service, or null if none is installed.
	// Note: Always use the get/set functions to ensure proper synchronization.

	private static HealthMonitor pdl_health_monitor = null;

	public static synchronized HealthMonitor get_pdl_health_monitor () {
		return pdl_health_monitor;
	}

	public static synchronized void set_pdl_health_monitor (HealthMonitor the_pdl_health_monitor) {
		pdl_health_monitor = the_pdl_health_monitor;
		return;
	}



	
	// Set the tracker URL in the product.
	// Parameters:
	//  product = The product to use.
	// This is a legacy requirement in PDL client 2.X.
	// The tracker URL is not used any more, but still must be set.  Any URL will do.
	// The function is removed in PDL client 3.X.

	// Version for PDL client 2.X.

	public static void product_setTrackerURL (Product product) {
		try {
			product.setTrackerURL(new URL("http://www.google.com/"));
		}
		catch (MalformedURLException e) {
			throw new RuntimeException ("Unable to create or set tracker URL", e);
		}
		return;
	}

	// Version for PDL client 3.X.

//	public static void product_setTrackerURL (Product product) {
//		return;
//	}




	
	// Sign the product.
	// Parameters:
	//  product = The product to sign.
	// Throws exception if unable to sign product.
	// Note: If no PDL key file has been specified, the function leaves the product unsigned.

	// Version for PDL client 2.X.

	public static void signProduct (Product product) {

		// Get the PDL key filename

		ServerConfig server_config = new ServerConfig();
		String pdl_key_filename = server_config.get_pdl_key_filename();

		// If no key supplied, then leave the product unsigned

		if (pdl_key_filename.isEmpty()) {
			return;
		}

		// Attempt to sign the product

		try {
			File privateKey = new File(pdl_key_filename); // OpenSSH private key file
			product.sign(CryptoUtils.readOpenSSHPrivateKey(StreamUtils.readStream(
				StreamUtils.getInputStream(privateKey)), null));
		}

		// Signing failed

		catch (Exception e) {
			throw new PDLSigningException ("PDLSender: Unable to sign PDL product", e);
		}

		return;
	}

	// Version for PDL client 3.X.

//	public static void signProduct (Product product) {
//
//		// Get the PDL key filename
//
//		ServerConfig server_config = new ServerConfig();
//		String pdl_key_filename = server_config.get_pdl_key_filename();
//
//		// If no key supplied, then leave the product unsigned
//
//		if (pdl_key_filename.isEmpty()) {
//			return;
//		}
//
//		// Attempt to sign the product
//
//		try {
//			File privateKey = new File(pdl_key_filename); // OpenSSH private key file
//			product.sign(CryptoUtils.readOpenSSHPrivateKey(StreamUtils.readStream(
//				StreamUtils.getInputStream(privateKey)), null), CryptoUtils.Version.SIGNATURE_V2);
//		}
//
//		// Signing failed
//
//		catch (Exception e) {
//			throw new PDLSigningException ("PDLSender: Unable to sign PDL product", e);
//		}
//
//		return;
//	}



	
	// Send the product to PDL.
	// Parameters:
	//  product = The product to send, which should be signed.
	//  is_text = True if the product consists primarily of text data.
	// Throws exception if unable to send product to any available sender.
	// Note: If no senders have been specified, then the function simulates success.

	public static void sendProduct (Product product, boolean is_text) {

		ServerConfig server_config = new ServerConfig();

		// Check for simulated error

		if (server_config.get_is_pdl_down()) {
			report_failed_send (product, is_text);
			throw new PDLSimulatedException ("PDLSender: Simulated PDL down");
		}

		double sim_error_rate = server_config.get_pdl_err_rate();
		if (sim_error_rate > 1.0e-6) {
			if (sim_error_rate > Math.random()) {
				report_failed_send (product, is_text);
				throw new PDLSimulatedException ("PDLSender: Simulated PDL error");
			}
		}

		// Get the list of senders

		List<PDLSenderConfig> sender_list = server_config.get_pdl_senders();

		// If there are no senders, simulate success and just return

		int sender_count = sender_list.size();
		if (sender_count == 0) {
			return;
		}

		// Get the health monitor, or null if none

		HealthMonitor health_monitor = get_pdl_health_monitor();

		// Get the current time

		long time_now = System.currentTimeMillis();

		// Get the index of the first sender to try

		int first_index = get_first_sender_index (sender_count, time_now);

		// The index of the first sender that we sent to successfully, or -1 if none so far

		int success_index = -1;

		// Loop until we have sent to all destinations

		for (int i = 0; i < sender_count; ++i) {

			// The index of the current sender

			int current_index = (first_index + i) % sender_count;

			// Assume success if there is no exception

			boolean f_success = true;

			// Attempt to send using the current sender

			try {
				SocketProductSender sender;

				// Get configuration of the current sender

				PDLSenderConfig sender_config = sender_list.get (current_index);
				String host = sender_config.get_host();
				int port = sender_config.get_port();
				int connectTimeout = sender_config.get_connectTimeout();

				if (server_config.get_is_pdl_permitted()) {

					System.out.println ("Sending PDL product to " + host + ":" + port);

					// SocketProductSenders send directly to a PDL HUB and do not introduce
					// any polling latency.
					sender = new SocketProductSender(host, port, connectTimeout);

					// If product consists primarily of binary data, set this option `true`
					// to accelerate distribution.
					sender.setBinaryFormat(!is_text);

					// If product consists primarily of text data, set this option `true`
					// to accelerate distribution.
					sender.setEnableDeflate(is_text);

					// ^^ Note ^^ Typically do not set both of the above options to `true` as
					//            binary content doesn't compress efficiently but adds
					//            processing overhead.
		
					sender.sendProduct(product);

				} else {

					System.out.println ("[SIMULATED] Sending PDL product to " + host + ":" + port);
				
				}
			}

			// Send failed

			catch (Exception e) {

				// No success

				f_success = false;

				// If we have tried all available senders without success, throw

				if (success_index == -1 && (i + 1) == sender_count) {
					if (health_monitor != null) {
						health_monitor.report_failure (time_now);
					}
					report_failed_send (product, is_text);
					throw new PDLSendException ("PDLSender: Unable to send PDL product to any destination", e);
				}
			}

			// If successful, remember the sender

			if (f_success && success_index == -1) {
				success_index = current_index;
			}
		}

		// Save the index of the successful sender

		if (success_index != -1) {
			set_last_sender_index (success_index, time_now);
		}

		if (health_monitor != null) {
			health_monitor.report_success (time_now);
		}

		return;
	}



	
	// Send the product to PDL.
	// Parameters:
	//  product = The product to send, which should be signed.
	//  is_text = True if the product consists primarily of text data.
	// Throws exception if unable to send product to any available sender.
	// Note: If no senders have been specified, then the function simulates success.
	// Note: This version only sends to one sender.  If the first attempted sender
	// fails, it tries successive senders until one succeeds, and then stops.
	// In contrast, the above version always sends to all senders.  The PDL
	// maintainers recommend always sending to all senders.

	public static void sendProductOnce (Product product, boolean is_text) {

		// Get the list of senders

		ServerConfig server_config = new ServerConfig();
		List<PDLSenderConfig> sender_list = server_config.get_pdl_senders();

		// If there are no senders, simulate success and just return

		int sender_count = sender_list.size();
		if (sender_count == 0) {
			return;
		}

		// Get the health monitor, or null if none

		HealthMonitor health_monitor = get_pdl_health_monitor();

		// Get the current time

		long time_now = System.currentTimeMillis();

		// Get the index of the first sender to try

		int first_index = get_first_sender_index (sender_count, time_now);
		int current_index = first_index;

		// Loop until there is a successful send

		boolean f_success = false;

		while (!f_success) {

			// Assume success if there is no exception

			f_success = true;

			// Attempt to send using the current sender

			try {
				SocketProductSender sender;

				// Get configuration of the current sender

				PDLSenderConfig sender_config = sender_list.get (current_index);
				String host = sender_config.get_host();
				int port = sender_config.get_port();
				int connectTimeout = sender_config.get_connectTimeout();

				if (server_config.get_is_pdl_permitted()) {

					System.out.println ("Sending PDL product to " + host + ":" + port);

					// SocketProductSenders send directly to a PDL HUB and do not introduce
					// any polling latency.
					sender = new SocketProductSender(host, port, connectTimeout);

					// If product consists primarily of binary data, set this option `true`
					// to accelerate distribution.
					sender.setBinaryFormat(!is_text);

					// If product consists primarily of text data, set this option `true`
					// to accelerate distribution.
					sender.setEnableDeflate(is_text);

					// ^^ Note ^^ Typically do not set both of the above options to `true` as
					//            binary content doesn't compress efficiently but adds
					//            processing overhead.
		
					sender.sendProduct(product);

				} else {

					System.out.println ("[SIMULATED] Sending PDL product to " + host + ":" + port);
				
				}
			}

			// Send failed

			catch (Exception e) {

				// No success

				f_success = false;

				// Advance index to the next available sender

				++current_index;
				if (current_index >= sender_count) {
					current_index = 0;
				}

				// If we have tried all available senders, throw

				if (current_index == first_index) {
					if (health_monitor != null) {
						health_monitor.report_failure (time_now);
					}
					report_failed_send (product, is_text);
					throw new PDLSendException ("PDLSender: Unable to send PDL product to any destination", e);
				}
			}
		}

		// Save the index of the successful sender

		set_last_sender_index (current_index, time_now);

		if (health_monitor != null) {
			health_monitor.report_success (time_now);
		}

		return;
	}




	// Dump a product to an output stream.
	// Parameters:
	//  out = Destination output stream.
	//  product = Product to dump.
	//  is_text = True to treat the product as text.
	//  f_deflate = True to apply deflating.
	// The product is dumped in the form it would appear on the wire (without protocol headers).
	// See SocketProductSender.sendProduct().
	// This function is for debugging.

	public static void dump_product_to_stream (OutputStream dest, Product product, boolean is_text, boolean f_deflate) throws Exception {
		
		// Compression level when deflating products
		final int deflateLevel = 1;
		
		OutputStream out = dest;
		ObjectProductSource productSource = new ObjectProductSource(product);

		if (f_deflate) {
			out = new DeflaterOutputStream(out, new Deflater(deflateLevel));
		}

		// make sure product handler doesn't close stream before done
		OutputStream productOut = new StreamUtils.UnclosableOutputStream(out);
		if (!( is_text )) {
			productSource.streamTo(new BinaryProductHandler(productOut));
		} else {
			productSource.streamTo(new XmlProductHandler(productOut));
		}

		// deflate requires "finish"
		if (f_deflate) {
			((DeflaterOutputStream) out).finish();
		}

		// flush buffered output stream to socket
		out.flush();

		return;
	}




	// Dump a product to a file.
	// Parameters:
	//  filename = Filename to write.
	//  product = Product to dump.
	//  is_text = True to treat the product as text.
	//  f_deflate = True to apply deflating.
	// The product is dumped in the form it would appear on the wire (without protocol headers).
	// See SocketProductSender.sendProduct().
	// This function is for debugging.

	public static void dump_product_to_file (String filename, Product product, boolean is_text, boolean f_deflate) throws Exception {
		try (
			BufferedOutputStream buf = new BufferedOutputStream (new FileOutputStream (filename));
		) {
			dump_product_to_stream (buf, product, is_text, f_deflate);
		}
		return;
	}




	// Report that a send operation failed.
	// Parameters:
	//  product = Product that was attempted to send.
	//  is_text = True to treat the product as text.
	// Currently this function writes out the product as a diagnostic file, if possible.
	// Note: This function must never throw an exception.

	private static void report_failed_send (Product product, boolean is_text) {
		try {

			// Get a filename prefix to use

			String filename_prefix = ServerConfig.get_diag_filename_prefix();

			// If we got a prefix, write the product as a file

			if (filename_prefix != null) {
				String filename;
				if (is_text) {
					filename = filename_prefix + "pdl_product.xml";
				} else {
					filename = filename_prefix + "pdl_product.dat";
				}
				dump_product_to_file (filename, product, is_text, false);
			}

		}
		catch (Exception e) {
		}
		return;
	}

}
