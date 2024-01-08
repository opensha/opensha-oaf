package org.opensha.oaf.aafs;

import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;

import java.net.URL;
import java.net.URI;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import com.mongodb.MongoClientSettings;


// MongoDB parameters for SSL/TLS connections.
// Author: Michael Barall 12/12/2023.
//
// This class holds MongoDB SSL/TLS parameters retrieved from the server config file.
// This class is not intended to be instantiated.
// All data and functions are static, because the trust and key store properties are global.
//
// It also manages the system trust store and key store, if needed.
// The trust store and key store are global entities.

public class MongoDBSSLParams {

	// True to enable tracing output, for debugging.

	private static boolean f_trace = false;




	//----- Trust Store and Key Store Definitions -----




	// System property names for the SSL trust store and its password and type.

	public static final String SPROP_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
	public static final String SPROP_SSL_TRUST_STORE_PASS = "javax.net.ssl.trustStorePassword";
	public static final String SPROP_SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

	// System property names for the SSL key store and its password and type.

	public static final String SPROP_SSL_KEY_STORE = "javax.net.ssl.keyStore";
	public static final String SPROP_SSL_KEY_STORE_PASS = "javax.net.ssl.keyStorePassword";
	public static final String SPROP_SSL_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";

	// Default password for the trust store.
	// (This is a well-known default password, so there is no harm in including it in the code.)

	public static final String DEF_TRUST_STORE_PASS = "changeit";

	// If true, we can set the trust store properties before we have the key store password.

	private static final boolean f_allow_trust_only = true;




	//----- SSL Parameter Definitions -----




	// SSL options for SSL enable, client authentication, and accept invalid host address.

	public static final String OPT_SSL_ENABLE = "sslEnable";
	public static final String OPT_SSL_CLIENT_AUTH = "sslClientAuth";
	public static final String OPT_INVALID_HOST_ADDR = "sslInvalidHostAddr";


	// Default SSL option string.

	//public static final String DEF_SSL_OPTION_STRING = "sslEnable=auto&sslClientAuth=auto";
	public static final String DEF_SSL_OPTION_STRING = "auto";


	// Filenames for OAF trust store, key store, and key store password

	public static final String FN_OAF_TRUST_STORE = "oafcert_cacert";
	public static final String FN_OAF_KEY_STORE = "oafcert_app.p12";
	public static final String FN_OAF_KEY_STORE_PASS = "oafcert_app_p12_pass.txt";


	// Encoding for a parameter that takes true/false/auto values, plus an invalid value.

	private static final int TFA_INVALID = -1;
	private static final int TFA_FALSE = 0;
	private static final int TFA_TRUE = 1;
	private static final int TFA_AUTO = 2;


	// Convert a true/false/auto value to a string.

	private static String tfa_to_string (int tfa) {
		switch (tfa) {
		case TFA_INVALID: return "TFA_INVALID";
		case TFA_FALSE: return "TFA_FALSE";
		case TFA_TRUE: return "TFA_TRUE";
		case TFA_AUTO: return "TFA_AUTO";
		}
		return "TFA_UNKNOWN(" + tfa + ")";
	}


	// Parse a string to a true/false/auto value.
	// Returns TFA_INVALID if the parse fails.

	private static int parse_tfa (String s) {
		String x = s.trim();
		if (x.equalsIgnoreCase ("false")) {
			return TFA_FALSE;
		}
		if (x.equalsIgnoreCase ("true")) {
			return TFA_TRUE;
		}
		if (x.equalsIgnoreCase ("auto")) {
			return TFA_AUTO;
		}
		return TFA_INVALID;
	}


	// Parse a string to a true/false value.
	// Returns TFA_INVALID if the parse fails.

	private static int parse_true_false (String s) {
		String x = s.trim();
		if (x.equalsIgnoreCase ("false")) {
			return TFA_FALSE;
		}
		if (x.equalsIgnoreCase ("true")) {
			return TFA_TRUE;
		}
		return TFA_INVALID;
	}




	// Class that holds parsed values for SSL options.

	private static class ParsedSSLOptions {

		// SSL enable option - true/false/auto

		public int tfa_ssl_enable;

		// SSL client authentication option - true/false/auto

		public int tfa_client_auth;

		// SSL accept invalid host option - true/false/auto

		public int tfa_invalid_host_addr;

		// Return true if SSL enable is selected.

		public final boolean is_ssl_enable () {
			if (tfa_ssl_enable == TFA_TRUE) {
				return true;
			}
			return false;
		}

		// Return true if client authentication is selected.

		public final boolean is_client_auth () {
			if (tfa_client_auth == TFA_TRUE) {
				return true;
			}
			return false;
		}

		// Return true if invalid host address is selected.

		public final boolean is_invalid_host_addr () {
			if (tfa_invalid_host_addr == TFA_TRUE) {
				return true;
			}
			return false;
		}

		// Constructor sets default options which are all auto

		public ParsedSSLOptions () {
			tfa_ssl_enable = TFA_AUTO;
			tfa_client_auth = TFA_AUTO;
			tfa_invalid_host_addr = TFA_AUTO;
		}

		// Display our contents

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append ("ParsedSSLOptions:" + "\n");
		
			result.append ("tfa_ssl_enable = " + tfa_to_string (tfa_ssl_enable) + "\n");
			result.append ("tfa_client_auth = " + tfa_to_string (tfa_client_auth) + "\n");
			result.append ("tfa_invalid_host_addr = " + tfa_to_string (tfa_invalid_host_addr) + "\n");

			return result.toString();
		}

		// Parse an SSL option string.
		// Returns true if success, false if parse error.

		public final boolean parse_option_string (String options) {

			// Default options are all auto

			tfa_ssl_enable = TFA_AUTO;
			tfa_client_auth = TFA_AUTO;
			tfa_invalid_host_addr = TFA_AUTO;

			// An empty string selects default

			String options_trimmed = options.trim();
			if (options_trimmed.isEmpty()) {
				return true;
			}

			// If the entire string is true/false/auto, it is the enable option with all others defaulted

			int def_ssl_enable = parse_tfa (options);
			if (def_ssl_enable != TFA_INVALID) {
				tfa_ssl_enable = def_ssl_enable;
				return true;
			}

			// Split option string by option separator character &

			String[] opt_array = options.split ("&");

			// Scan the option array

			boolean seen_ssl_enable = false;
			boolean seen_client_auth = false;
			boolean seen_invalid_host_addr = false;

			for (String opt : opt_array) {

				// Split the option into key and value separated by =

				String[] key_value = opt.split ("=", 2);

				if (key_value.length != 2) {
					return false;
				}

				// Check for recognized key, and process its value

				String key = key_value[0].trim();

				if (key.equalsIgnoreCase (OPT_SSL_ENABLE)) {
					if (seen_ssl_enable) {
						return false;
					}
					seen_ssl_enable = true;
					tfa_ssl_enable = parse_tfa (key_value[1]);
					if (tfa_ssl_enable == TFA_INVALID) {
						return false;
					}
				}

				else if (key.equalsIgnoreCase (OPT_SSL_CLIENT_AUTH)) {
					if (seen_client_auth) {
						return false;
					}
					seen_client_auth = true;
					tfa_client_auth = parse_tfa (key_value[1]);
					if (tfa_client_auth == TFA_INVALID) {
						return false;
					}
				}

				else if (key.equalsIgnoreCase (OPT_INVALID_HOST_ADDR)) {
					if (seen_invalid_host_addr) {
						return false;
					}
					seen_invalid_host_addr = true;
					tfa_invalid_host_addr = parse_tfa (key_value[1]);
					if (tfa_invalid_host_addr == TFA_INVALID) {
						return false;
					}
				}

				else {
					return false;
				}
			}

			// Successful parse

			return true;
		}

		// Resolve auto values, assuming an SSL environment is available.
		// Parameters:
		//  f_has_keystore = True if a keystore is supplied in the SSL environment.
		// If the result is that SSL is disabled, then all fields are set accordingly.

		public final void resolve_for_ssl_env (boolean f_has_keystore) {
			if (tfa_ssl_enable == TFA_AUTO) {
				tfa_ssl_enable = TFA_TRUE;
			}
			if (tfa_ssl_enable == TFA_FALSE) {
				tfa_client_auth = TFA_FALSE;
				tfa_invalid_host_addr = TFA_FALSE;
			}
			else {
				if (tfa_client_auth == TFA_AUTO) {
					tfa_client_auth = (f_has_keystore ? TFA_TRUE : TFA_FALSE);
				}
				if (tfa_invalid_host_addr == TFA_AUTO) {
					tfa_invalid_host_addr = TFA_FALSE;
				}
			}
			return;
		}

		// Resolve auto values, assuming no SSL environment is available.
		// If the result is that SSL is disabled, then all fields are set accordingly.

		public final void resolve_for_no_ssl_env () {
			if (tfa_ssl_enable == TFA_AUTO) {
				tfa_ssl_enable = TFA_FALSE;
			}
			if (tfa_ssl_enable == TFA_FALSE) {
				tfa_client_auth = TFA_FALSE;
				tfa_invalid_host_addr = TFA_FALSE;
			}
			else {
				if (tfa_client_auth == TFA_AUTO) {
					tfa_client_auth = TFA_FALSE;
				}
				if (tfa_invalid_host_addr == TFA_AUTO) {
					tfa_invalid_host_addr = TFA_FALSE;
				}
			}
			return;
		}

		// Apply SSL options to the MongoDB connection builder.
		// Returns the resulting modified builder.
		// If SSL is disabled, the function just returns its argument.

		public final MongoClientSettings.Builder apply_ssl_options (MongoClientSettings.Builder builder, SSLContext ssl_context) {
			MongoClientSettings.Builder my_builder = builder;

			if (is_ssl_enable()) {

				my_builder = my_builder.applyToSslSettings (builder2 -> {
					builder2.enabled (true);
					if (is_invalid_host_addr()) {
						builder2.invalidHostNameAllowed (true);
					}
					if (ssl_context != null) {
						builder2.context (ssl_context);
					}
				});

			}

			return my_builder;
		}

	}




	// Return true if the given SSL option string is valid, false if not.

	public static boolean is_valid_ssl_option_string (String options) {
		if (options == null) {
			return false;
		}
		return (new ParsedSSLOptions()).parse_option_string (options);
	}




	// Marshal an SSL option string.
	// Throws MarshalException if the option string is invalid or there is some error.

	public static void marshal_ssl_option_string (MarshalWriter writer, String name, String options) {

		if (!( is_valid_ssl_option_string (options) )) {
			throw new MarshalException ("MongoDBSSLParams: Attempt to marshal invalid SSL option string: " + options);
		}

		writer.marshalString (name, options);
		return;
	}




	// Unmarshal an SSL option string.
	// Throws MarshalException if the option string is invalid or there is some error.
	// Also loads system information, if it is not loaded yet.
	// Throws an exception if the system information cannot be loaded successfully.

	public static String unmarshal_ssl_option_string (MarshalReader reader, String name) {
		load_new_sys_info();

		String options = reader.unmarshalString (name);

		if (!( is_valid_ssl_option_string (options) )) {
			throw new MarshalException ("MongoDBSSLParams: Attempt to unmarshal invalid SSL option string: " + options);
		}

		return options;
	}




	// Unmarshal the default SSL option string.
	// Throws MarshalException if the option string is invalid or there is some error.
	// Also loads system information, if it is not loaded yet.
	// Throws an exception if the system information cannot be loaded successfully.

	public static String unmarshal_def_ssl_option_string () {
		load_new_sys_info();

		return DEF_SSL_OPTION_STRING;
	}




	// Check if a keystore password is correct.
	// Parameters:
	//  ks_type = Type of keystore.
	//  ks_filename = Filename containing keystore.
	//  ks_password = Password to check.
	// Returns null if the password is correct.
	// Returns error message (typically a stack trace) if not.

	public static String check_keystore_password (String ks_type, String ks_filename, String ks_password) {

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Beginning check_keystore_password");
			System.out.println ();
			System.out.println ("ks_type = " + ks_type);
			System.out.println ("ks_filename = " + ks_filename);
			System.out.println ("ks_password = " + ks_password);
		}

		try {

			// Convert password to character array

			char[] password = ks_password.toCharArray();

			if (f_trace) {
				System.out.print ("password =");
				for (char c : password) {
					System.out.print (" " + Integer.toString ((int)c, 16));
				}
				System.out.println ();
				System.out.println ();
			}

			// Create an implementation of the keystore type (throws KeyStoreException if no such implementation)

			KeyStore ks = KeyStore.getInstance (ks_type);

			// Attempt to load the keystore (throws exception if bad password or some other error)

			try (FileInputStream fis = new FileInputStream (ks_filename)) {
				ks.load (fis, password);
			}

		}
		catch (Exception e) {
			if (f_trace) {
				System.out.println ();
				System.out.println ("Exception occurred during check_keystore_password");
				System.out.println (SimpleUtils.getStackTraceAsString (e));
				System.out.println ();
			}

			return SimpleUtils.getStackTraceAsString (e);
		}

		return null;
	}




	// Return true if two strings match, allowing null arguments.
	// Note: Null matches null, but nothing else.

	public static boolean string_matches (String s1, String s2) {
		if (s1 == null) {
			if (s2 == null) {
				return true;
			}
			return false;
		}
		else {
			if (s2 == null) {
				return false;
			}
		}
		return s1.equals (s2);
	}




	//----- Jar Directory -----




	// Find directory containing the jar file, subroutine 1.
	// Returns null if cannot be found.

	private static String find_jar_dir_sub_1 (boolean f_verbose) {
		String jar_dir = null;

		try {
			URL url = MongoDBSSLParams.class.getProtectionDomain().getCodeSource().getLocation();
			if (url != null) {

				if (f_verbose) {
					System.out.println ();
					System.out.println ("find_jar_dir_sub_1: URL = " + url.toString());
					System.out.println ("find_jar_dir_sub_1: URI = " + url.toURI().toString());
				}

				Path parent = Paths.get(url.toURI()).getParent();
				if (parent != null) {
					jar_dir = parent.toString();

					if (f_verbose) {
						System.out.println ("find_jar_dir_sub_1: DIR = " + jar_dir);
					}
				}
			}
		}
		catch (Exception e) {
			jar_dir = null;
		}

		return jar_dir;
	}




	// Find directory containing the jar file, subroutine 2.
	// Returns null if cannot be found.

	private static String find_jar_dir_sub_2 (boolean f_verbose) {
		String jar_dir = null;

		try {
			URL url = MongoDBSSLParams.class.getResource (MongoDBSSLParams.class.getSimpleName() + ".class");

			if (f_verbose) {
				System.out.println ();
				System.out.println ("find_jar_dir_sub_2: URL = " + url.toString());
				System.out.println ("find_jar_dir_sub_2: URI = " + url.toURI().toString());
			}

			String s_url = url.toString();

			if (s_url.startsWith ("jar:file:")) {
				String s_path = s_url.replaceAll ("^jar:(file:.*[.]jar)!/.*", "$1");
				Path parent = Paths.get((new URL (s_path)).toURI()).getParent();
				if (parent != null) {
					jar_dir = parent.toString();

					if (f_verbose) {
						System.out.println ("find_jar_dir_sub_2: DIR = " + jar_dir);
					}
				}
			}

			else if (s_url.startsWith ("file:")) {
				String s_path = s_url.replaceAll ("^(file:.*[.]jar)!/.*", "$1");
				Path parent = Paths.get((new URL (s_path)).toURI()).getParent();
				if (parent != null) {
					jar_dir = parent.toString();

					if (f_verbose) {
						System.out.println ("find_jar_dir_sub_1: DIR = " + jar_dir);
					}
				}
			}
		}
		catch (Exception e) {
			jar_dir = null;
		}

		return jar_dir;
	}




	// Find directory containing the jar file.
	// Returns null if cannot be found.

	public static String find_jar_dir () {
		String jar_dir = find_jar_dir_sub_1 (false);
		if (jar_dir == null) {
			jar_dir = find_jar_dir_sub_2 (false);
		}
		return jar_dir;
	}




	//----- Global (System-Wide) Resources -----




	//--- Configuration

	// True to check jar directory if AAFS_SSL_DIR is not defined.

	private static boolean f_search_jar_dir = false;

	// True to use an SSLContext instead of setting system properties.

	private static boolean f_use_ssl_context = false;


	//--- System environment

	// True if we have loaded the system information.

	private static boolean f_loaded_sys_info = false;

	// The SSL directory, or null if none.

	private static Path oaf_ssl_dir = null;

	// The filename of the OAF trust store, or null if none.

	private static String oaf_trust_store = null;

	// The filename of the OAF key store, or null if none.

	private static String oaf_key_store = null;

	// The OAF key store password retrieved from the file, or null if none.
	// (The actual password, not the filename.)

	private static String oaf_key_store_pass = null;


	//--- User supplied

	// A user-supplied password, or null if none.

	private static String user_key_store_pass = null;


	//--- System properties

	// True if we have set the trust store property.

	private static boolean f_set_prop_trust_store = false;

	// Saved values of the trust store and password properties.

	private static String saved_prop_trust_store = null;
	private static String saved_prop_trust_store_pass = null;

	// True if we have set the key store property.

	private static boolean f_set_prop_key_store = false;

	// Saved values of the key store and password properties.

	private static String saved_prop_key_store = null;
	private static String saved_prop_key_store_pass = null;


	//--- SSL context

	// The SSL context to use for MongoDB.

	private static SSLContext mongo_ssl_context = null;

	// The trust store used to make the SSL context.

	private static String context_trust_store = null;

	// The key store used to make the SSL context.

	private static String context_key_store = null;
	private static String context_key_store_pass = null;





	//----- Subroutines for Global (System-Wide) Resources -----




	// Return true if system information is loaded and we have an SSL environment.
	// Note: Caller must synchronize.

	private static boolean do_has_ssl_env () {
		if (f_loaded_sys_info && oaf_ssl_dir != null) {
			return true;
		}
		return false;
	}




	// Resolve auto SSL options according to the loaded environment.
	// Parameters:
	//  ssl_options = Parsed SSL options.
	// On return, ssl_options is modified so it contains no auto values.
	// Throws exception if system envifonment is not loaded.
	// Note: Caller must synchronize.

	private static void do_resolve_ssl_options (ParsedSSLOptions ssl_options) {

		// Error if information is not loaded

		if (!( f_loaded_sys_info )) {
			throw new IllegalStateException ("MongoDBSSLParams.do_resolve_ssl_options: Attempt to resolve SSL options before system information is loaded");
		}

		// If we have en SSL environment ...

		if (do_has_ssl_env()) {
			ssl_options.resolve_for_ssl_env (oaf_key_store != null);
		}

		// Otherwise, no SSL environment ...

		else {
			ssl_options.resolve_for_no_ssl_env ();
		}

		return;
	}




	// Return true if we need a password for the key store.
	// We need a password if system info is loaded, we have an SSL environment,
	// and we have a key store, but there is no password.
	// Note: Caller must synchronize.

	private static boolean do_needs_password () {
		if (
			do_has_ssl_env()
			&& (oaf_key_store != null)
			&& (oaf_key_store_pass == null && user_key_store_pass == null)
		) {
			return true;
		}
		return false;
	}




	// Return true if a user password is required for the key store.
	// We require a user password if system info is loaded, we have an SSL environment,
	// and we have a key store, but there is no key store password obtained during load.
	// Note: The return value is not affected by whether a user password has been set.
	// Note: Caller must synchronize.

	private static boolean do_requires_user_password () {
		if (
			do_has_ssl_env()
			&& (oaf_key_store != null)
			&& (oaf_key_store_pass == null)
		) {
			return true;
		}
		return false;
	}




	// Set or clear a property.
	// If the value is null, then the property is cleared.
	// Returns the prior value of the property, or null if there was no prior value.

	private static String do_set_or_clear_prop (String key, String value) {
		if (value == null) {
			return System.clearProperty (key);
		}
		return System.setProperty (key, value);
	}




	// Set or unset the properties, according to our current state.
	// Note: Caller must synchronize.

	private static void do_set_props () {

		// Make or discard the SSL context, according to our current state

		do_set_ssl_context();

		// If we want to set the trust store ...

		if (
			do_has_ssl_env()
			&& (!f_use_ssl_context)
			&& oaf_trust_store != null
			&& (f_allow_trust_only || oaf_key_store == null || oaf_key_store_pass != null || user_key_store_pass != null)
		) {

			// Set trust store

			String t_store = System.setProperty (SPROP_SSL_TRUST_STORE, oaf_trust_store);
			String t_pass = System.setProperty (SPROP_SSL_TRUST_STORE_PASS, DEF_TRUST_STORE_PASS);

			// If we just replaced the system defaults, save them

			if (!( f_set_prop_trust_store )) {
				saved_prop_trust_store = t_store;
				saved_prop_trust_store_pass = t_pass;
				f_set_prop_trust_store = true;
			}
		}

		// Otherwise we don't want to set the trust store

		else {

			// If we have the system defaults, restore them

			if (f_set_prop_trust_store) {
				do_set_or_clear_prop (SPROP_SSL_TRUST_STORE, saved_prop_trust_store);
				do_set_or_clear_prop (SPROP_SSL_TRUST_STORE_PASS, saved_prop_trust_store_pass);
				f_set_prop_trust_store = false;
				saved_prop_trust_store = null;
				saved_prop_trust_store_pass = null;
			}
		}

		// If we want to set the key store ...

		if (
			do_has_ssl_env()
			&& (!f_use_ssl_context)
			&& oaf_key_store != null
			&& (oaf_key_store_pass != null || user_key_store_pass != null)
		) {

			// Set key store

			String k_store = System.setProperty (SPROP_SSL_KEY_STORE, oaf_key_store);
			String k_pass = System.setProperty (SPROP_SSL_KEY_STORE_PASS,
				(user_key_store_pass != null) ? user_key_store_pass : oaf_key_store_pass);

			// If we just replaced the system defaults, save them

			if (!( f_set_prop_key_store )) {
				saved_prop_key_store = k_store;
				saved_prop_key_store_pass = k_pass;
				f_set_prop_key_store = true;
			}
		}

		// Otherwise we don't want to set the key store

		else {

			// If we have the system defaults, restore them

			if (f_set_prop_key_store) {
				do_set_or_clear_prop (SPROP_SSL_KEY_STORE, saved_prop_key_store);
				do_set_or_clear_prop (SPROP_SSL_KEY_STORE_PASS, saved_prop_key_store_pass);
				f_set_prop_key_store = false;
				saved_prop_key_store = null;
				saved_prop_key_store_pass = null;
			}
		}

		// For debugging only, display current properties

		//System.out.println ();
		//show_all_store_props();

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** State at conclusion of do_set_props");
			System.out.println ();
			show_all_global_state();
			System.out.println ();
		}

		return;
	}




	// Make or discard the SSL context, according to our current state.
	// Note: Caller must synchronize.

	private static void do_set_ssl_context () {

		// If we want to make the SSL context ...

		if (
			do_has_ssl_env()
			&& f_use_ssl_context
			&& (oaf_trust_store != null || oaf_key_store != null)
			&& (oaf_key_store == null || oaf_key_store_pass != null || user_key_store_pass != null)
		) {

			// If we don't already have the requested SSL contest ...

			if (!(
				mongo_ssl_context != null
				&& string_matches (context_trust_store, oaf_trust_store)
				&& string_matches (context_key_store, oaf_key_store)
				&& string_matches (context_key_store_pass, oaf_key_store_pass)
			)) {

				// Any exception here means we couldn't make the context

				try {

					// List of key store and trust store managers

					KeyManager[] key_man_list = null;
					TrustManager[] trust_man_list = null;

					// If we have our own key store ...

					if (oaf_key_store != null) {

						// Convert password to character string

						char[] ks_password;
						if (user_key_store_pass != null) {
							ks_password = user_key_store_pass.toCharArray();
						} else {
							ks_password = oaf_key_store_pass.toCharArray();
						}

						// If the filename ends in .p12 then force PKCS12, otherwise use the default

						String ks_type;
						if (oaf_key_store.trim().endsWith (".p12")) {
							ks_type = "pkcs12";
						} else {
							ks_type = KeyStore.getDefaultType();
						}

						// Create an implementation of the keystore type (throws KeyStoreException if no such implementation)

						KeyStore ks = KeyStore.getInstance (ks_type);

						// Attempt to load the keystore (throws exception if bad password or some other error)

						try (FileInputStream fis = new FileInputStream (oaf_key_store)) {
							ks.load (fis, ks_password);
						}

						// Make the key manager factory

						KeyManagerFactory key_man_factory = KeyManagerFactory.getInstance (KeyManagerFactory.getDefaultAlgorithm());

						key_man_factory.init (ks, ks_password);

						// Get the list of key managers

						key_man_list = key_man_factory.getKeyManagers();
					}

					// If we have our own trust store ...

					if (oaf_trust_store != null) {

						// Convert password to character string

						char[] ts_password = DEF_TRUST_STORE_PASS.toCharArray();;

						// If the filename ends in .p12 then force PKCS12, otherwise use the default

						String ts_type;
						if (oaf_trust_store.trim().endsWith (".p12")) {
							ts_type = "pkcs12";
						} else {
							ts_type = KeyStore.getDefaultType();
						}

						// Create an implementation of the trust store type (throws KeyStoreException if no such implementation)

						KeyStore ts = KeyStore.getInstance (ts_type);

						// Attempt to load the trust store (throws exception if bad password or some other error)

						try (FileInputStream fis = new FileInputStream (oaf_trust_store)) {
							ts.load (fis, ts_password);
						}

						// Make the trust manager factory

						TrustManagerFactory trust_man_factory = TrustManagerFactory.getInstance (TrustManagerFactory.getDefaultAlgorithm());

						trust_man_factory.init (ts);

						// Get the list of trust managers

						trust_man_list = trust_man_factory.getTrustManagers();
					}

					// Make the SSL context

					SSLContext my_ssl_context = SSLContext.getInstance ("TLS");

					my_ssl_context.init (key_man_list, trust_man_list, null);

					// Context made successfully, save it

					mongo_ssl_context = my_ssl_context;
					context_trust_store = oaf_trust_store;
					context_key_store = oaf_key_store;
					context_key_store_pass = oaf_key_store_pass;
				}

				// Failed to make the context

				catch (Exception e) {

					// Discard any SSL context

					mongo_ssl_context = null;
					context_trust_store = null;
					context_key_store = null;
					context_key_store_pass = null;

					// Display the reason for the failure

					System.out.println (SimpleUtils.getStackTraceAsString (e));
				}
			}
		}

		// Otherwise, discard any SSL context

		else {
			mongo_ssl_context = null;
			context_trust_store = null;
			context_key_store = null;
			context_key_store_pass = null;
		}

		return;
	}




	// Clear the system information.
	// Also restores the system default properties, if we changed them.
	// Note: Caller must synchronize.

	private static void do_clear_sys_info () {

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Beginning do_clear_sys_info");
			System.out.println ();
		}

		// Restore not-loaded system information values

		f_loaded_sys_info = false;
		oaf_ssl_dir = null;
		oaf_trust_store = null;
		oaf_key_store = null;
		oaf_key_store_pass = null;

		// Restore properties, if we changed them

		do_set_props();
		return;
	}




	// Set the flag indicating if the jar directory should be checked if AAFS_SSL_DIR is not defined.
	// Note: Caller must synchronize.

	private static void do_set_search_jar_dir (boolean the_f_search_jar_dir) {
		f_search_jar_dir = the_f_search_jar_dir;
		return;
	}




	// Set the flag indicating if an SSLContext should be used instead of setting system properties.
	// Note: Because this sets the mode of operation, it should be used before any
	// attempt to load parameters, preferably early in the main function.
	// Note: Caller must synchronize.

	private static void do_set_use_ssl_context (boolean the_f_use_ssl_context) {
		f_use_ssl_context = the_f_use_ssl_context;
		return;
	}




	// Load the system information, overwriting any existing system information.
	// Also sets the properties, if possible.
	// If error, throws an exception and leaves system information in the unloaded state.
	// Note: Caller must synchronize.

	private static void do_load_sys_info () {

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Beginning do_load_sys_info");
			System.out.println ();
		}

		// Clear any existing system information, restore properties to system default

		do_clear_sys_info();

		// Local variables to build the system information

		Path my_oaf_ssl_dir = null;
		String my_oaf_trust_store = null;
		String my_oaf_key_store = null;
		String my_oaf_key_store_pass = null;

		// Check the environment variable AAFS_SSL_DIR to get our path, if none defined then we loaded info but found no environment

		String var = System.getenv ("AAFS_SSL_DIR");

		if (var != null) {
			var = var.trim();
			if (var.isEmpty()) {
				var = null;
			}
		}

		// If we found the environment variable ...

		if (var != null) {

			// Verify that the value is a directory

			try {
				my_oaf_ssl_dir = Paths.get (var);
			}
			catch (InvalidPathException e) {
				throw new MongoDBSSLParamsException ("MongoDBSSLParams: Environment variable AAFS_SSL_DIR contains an invalid path: " + e.getMessage(), e);
			}
			catch (Exception e) {
				throw new MongoDBSSLParamsException ("MongoDBSSLParams: Environment variable AAFS_SSL_DIR contains an invalid path: " + var, e);
			}

			boolean is_dir = false;
			try {
				is_dir = Files.isDirectory (my_oaf_ssl_dir);
			}
			catch (Exception e) {
				throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error checking for existence of directory in environment variable AAFS_SSL_DIR: " + var, e);
			}
			if (!( is_dir )) {
				throw new MongoDBSSLParamsException ("MongoDBSSLParams: Cannot find directory in environment variable AAFS_SSL_DIR: " + var);
			}

		}

		// Otherwise, no environment variable ...

		else {

			try {

				// If we want to check the jar directory ...

				if (f_search_jar_dir) {

					// Get the jar directory

					String jar_dir = find_jar_dir();

					// If we got the jar directory ...

					if (jar_dir != null) {

						// Convert to path

						Path jar_dir_path = Paths.get (jar_dir);

						// If it is a directory (it should be) ...

						if (Files.isDirectory (jar_dir_path)) {

							// If the trust store file exists and is readable, use the jar directory

							if (Files.isReadable (jar_dir_path.resolve (FN_OAF_TRUST_STORE))) {
								my_oaf_ssl_dir = jar_dir_path;
							}

							// Otherwise, if the key store file exists and is readable, use the jar directory

							else if (Files.isReadable (jar_dir_path.resolve (FN_OAF_KEY_STORE))) {
								my_oaf_ssl_dir = jar_dir_path;
							}
						}
					}
				}
			}

			// Any exception here means no jar directory

			catch (Exception e) {
				my_oaf_ssl_dir = null;
			}

			// If we didn't find the files in the jar directory, then no environment

			if (my_oaf_ssl_dir == null) {
				f_loaded_sys_info = true;
				return;
			}
		}

		// Check if the trust store file exists and is readable

		Path trust_path = null;

		try {
			trust_path = my_oaf_ssl_dir.resolve (FN_OAF_TRUST_STORE);
		}
		catch (InvalidPathException e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL trust store file: " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL trust store file: dir = " + my_oaf_ssl_dir.toString() + ", file = " + FN_OAF_TRUST_STORE, e);
		}

		try {
			if (Files.isReadable (trust_path)) {
				my_oaf_trust_store = trust_path.toString();
			}
		}
		catch (Exception e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error checking for existence of SSL trust store file: " + trust_path.toString(), e);
		}

		// Check if the key store file exists and is readable

		Path key_path = null;

		try {
			key_path = my_oaf_ssl_dir.resolve (FN_OAF_KEY_STORE);
		}
		catch (InvalidPathException e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL key store file: " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL key store file: dir = " + my_oaf_ssl_dir.toString() + ", file = " + FN_OAF_KEY_STORE, e);
		}

		try {
			if (Files.isReadable (key_path)) {
				my_oaf_key_store = key_path.toString();
			}
		}
		catch (Exception e) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error checking for existence of SSL key store file: " + key_path.toString(), e);
		}

		// If we have a key store file, try to get the password ...

		if (my_oaf_key_store != null) {

			// First try the environment variable AAFS_SSL_KEY_PASS

			my_oaf_key_store_pass = System.getenv ("AAFS_SSL_KEY_PASS");

			if (my_oaf_key_store_pass != null) {
				my_oaf_key_store_pass = my_oaf_key_store_pass.trim();
				if (my_oaf_key_store_pass.isEmpty()) {
					my_oaf_key_store_pass = null;
				}
			}

			// If not found password yet, look for a password file

			if (my_oaf_key_store_pass == null) {

				Path pass_path = null;

				try {
					pass_path = my_oaf_ssl_dir.resolve (FN_OAF_KEY_STORE_PASS);
				}
				catch (InvalidPathException e) {
					throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL key store password file: " + e.getMessage(), e);
				}
				catch (Exception e) {
					throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error constructing name of SSL key store password file: dir = " + my_oaf_ssl_dir.toString() + ", file = " + FN_OAF_KEY_STORE_PASS, e);
				}

				boolean is_readable = false;
				try {
					if (Files.isReadable (pass_path)) {
						is_readable = true;
					}
				}
				catch (Exception e) {
					throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error checking for existence of SSL key store password file: " + pass_path.toString(), e);
				}

				if (is_readable) {
					List<String> lines = null;
					try {
						lines = Files.readAllLines (pass_path);
					}
					catch (Exception e) {
						throw new MongoDBSSLParamsException ("MongoDBSSLParams: Error reading SSL key store password file: " + pass_path.toString(), e);
					}
					if (lines.isEmpty()) {
						throw new MongoDBSSLParamsException ("MongoDBSSLParams: No text found in SSL key store password file: " + pass_path.toString());
					}

					my_oaf_key_store_pass = lines.get(0);

					if (my_oaf_key_store_pass != null) {
						my_oaf_key_store_pass = my_oaf_key_store_pass.trim();
						if (my_oaf_key_store_pass.isEmpty()) {
							my_oaf_key_store_pass = null;
						}
					}
					if (my_oaf_key_store_pass == null) {
						throw new MongoDBSSLParamsException ("MongoDBSSLParams: No password found on first line of SSL key store password file: " + pass_path.toString());
					}
				}
			}

		}	// end if we have a key store file

		// We loaded the SSL environment

		oaf_ssl_dir = my_oaf_ssl_dir;
		oaf_trust_store = my_oaf_trust_store;
		oaf_key_store = my_oaf_key_store;
		oaf_key_store_pass = my_oaf_key_store_pass;
			
		f_loaded_sys_info = true;

		// Update properties

		do_set_props();

		return;
	}




	// Set the user key store password.
	// The argument can be null to remove the user password.
	// Note: The user password is persistent across info loads, and can be set at any time.
	// Note: Use do_check_and_set_user_password() if you want to check that the password is correct.
	// Note: Caller must synchronize.

	private static void do_set_user_password (String user_pass) {

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Beginning do_set_user_password");
			System.out.println ();
			System.out.println ("user_pass = " + ((user_pass == null) ? "<null>" : user_pass));
			System.out.println ();
		}

		if (user_pass == null) {
			user_key_store_pass = null;
		}
		else {
			user_key_store_pass = user_pass.trim();
		}

		// Update properties

		do_set_props();
		return;
	}




	// Check and set the user key store password.
	// The argument can be null to remove the user password.
	// The return is null if password was correct and successfully set.
	// The return is an error message (typically an exception strack trace) if
	// the password is incorrect or there is some other error.
	// Note: The user password is persistent across info loads, and can be set at any time.
	// Note: It is recommended to call this only if do_needs_password() returns true.
	// Note: Caller must synchronize.

	private static String do_check_and_set_user_password (String user_pass) {

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Beginning do_check_and_set_user_password");
			System.out.println ();
			System.out.println ("user_pass = " + ((user_pass == null) ? "<null>" : user_pass));
			System.out.println ();
			show_all_global_state();
			System.out.println ();
		}

		// If null, just remove the user password

		if (user_pass == null) {
			user_key_store_pass = null;
		}

		// Otherwise, we are setting the password ...

		else {
			String ks_password = user_pass.trim();

			// If we have an environment and a keystore filename ...

			if (do_has_ssl_env() && (oaf_key_store != null)) {

				String ks_filename = oaf_key_store;

				// If the filename ends in .p12 then force PKCS12, otherwise use the default

				String ks_type;
				if (ks_filename.trim().endsWith (".p12")) {
					ks_type = "pkcs12";
				} else {
					ks_type = KeyStore.getDefaultType();
				}

				// Check the password, return error if it's bad

				String ckpw = check_keystore_password (ks_type, ks_filename, ks_password);
				if (ckpw != null) {

					if (f_trace) {
						System.out.println ();
						System.out.println ("*** Error return from do_check_and_set_user_password");
						System.out.println ();
						//System.out.println (ckpw);
						//System.out.println ();
					}

					return ckpw;
				}
			}

			// Set the new password

			user_key_store_pass = ks_password;
		}

		// Update properties

		do_set_props();
		return null;
	}




	// Get the user key store password.
	// The returned value can be null if the user password is not set.
	// Note: Caller must synchronize.

	private static String do_get_user_password () {
		return user_key_store_pass;
	}




	//----- Service Functions -----




	// Return true if a key store password is needed.

	public static synchronized boolean needs_password () {
		return do_needs_password();
	}




	// Return true if a user password is required for the key store.
	// Note: The return value is not affected by whether a user password has been set.

	public static synchronized boolean requires_user_password () {
		return do_requires_user_password();
	}




	// Set the user key store password.
	// The argument can be null to remove the user password.
	// Note: The user password is persistent across info loads, and can be set at any time.

	public static synchronized void set_user_password (String user_pass) {
		do_set_user_password (user_pass);
		return;
	}




	// Check and set the user key store password.
	// The argument can be null to remove the user password.
	// The return is an error message (typically an exception strack trace) if
	// the password is incorrect or there is some other error.
	// Note: The user password is persistent across info loads, and can be set at any time.
	// Note: It is recommended to call this only if needs_password() returns true.

	public static synchronized String check_and_set_user_password (String user_pass) {
		return do_check_and_set_user_password (user_pass);
	}




	// Get the user key store password.
	// The returned value can be null if the user password is not set.

	public static synchronized String get_user_password () {
		return do_get_user_password();
	}




	// Parse and print an SSL option string.
	// Throws an exception if the option string is invalid.
	// Note: This function is primarily for testing.

	public static synchronized String parse_and_print_ssl_options (String options) {
		ParsedSSLOptions ssl_options = new ParsedSSLOptions();
		if (!( ssl_options.parse_option_string (options) )) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Invalid SSL option string: " + options);
		}
		return ssl_options.toString();
	}




	// Resolve and print an SSL option string.
	// Throws an exception if the option string is invalid.
	// Throws an exception if system information is not loaded.
	// Note: This function is primarily for testing.

	public static synchronized String resolve_and_print_ssl_options (String options) {
		ParsedSSLOptions ssl_options = new ParsedSSLOptions();
		if (!( ssl_options.parse_option_string (options) )) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Invalid SSL option string: " + options);
		}
		do_resolve_ssl_options (ssl_options);
		return ssl_options.toString();
	}




	// Resolve and apply an SSL option string.
	// Returns the resulting modified builder.
	// If SSL is disabled, the function just returns its argument.
	// Throws an exception if the option string is invalid.
	// Throws an exception if system information is not loaded.
	// Note: Omitting the password check allows connection to occur if no password is set and the server
	// does not actually require client authentication; if it does require client authentication then
	// the connection attempt will simply fail.  (f_allow_trust_only=true allows this behavior to occur.)

	public static synchronized MongoClientSettings.Builder resolve_and_apply_ssl_options (MongoClientSettings.Builder builder, String options) {
		ParsedSSLOptions ssl_options = new ParsedSSLOptions();
		if (!( ssl_options.parse_option_string (options) )) {
			throw new MongoDBSSLParamsException ("MongoDBSSLParams: Invalid SSL option string: " + options);
		}
		do_resolve_ssl_options (ssl_options);
		//if (!( do_needs_password() )) {
		//	throw new MongoDBSSLParamsException ("MongoDBSSLParams: SSL key store password has not been set");
		//}

		if (f_trace) {
			System.out.println ();
			System.out.println ("*** Setting MongoDB SSL options");
			System.out.println ();
			System.out.println ("options = " + options);
			System.out.println (ssl_options.toString());
			System.out.println ();
		}

		return ssl_options.apply_ssl_options (builder, mongo_ssl_context);
	}




	// Clear system information.
	// Also restores the system default properties, if we changed them.

	public static synchronized void clear_sys_info () {
		do_clear_sys_info();
		return;
	}




	// Set the flag indicating if the jar directory should be checked if AAFS_SSL_DIR is not defined.

	public static synchronized void set_search_jar_dir (boolean the_f_search_jar_dir) {
		do_set_search_jar_dir (the_f_search_jar_dir);
		return;
	}




	// Set the flag indicating if an SSLContext should be used instead of setting system properties.
	// Note: Because this sets the mode of operation, it should be used before any
	// attempt to load parameters, preferably early in the main function.

	public static synchronized void set_use_ssl_context (boolean the_f_use_ssl_context) {
		do_set_use_ssl_context (the_f_use_ssl_context);
		return;
	}




	// Load the system information, overwriting any existing system information.
	// Also sets the properties, if possible.
	// If error, throws an exception and leaves system information in the unloaded state.

	public static synchronized void load_sys_info () {
		do_load_sys_info();
		return;
	}




	// Load the system information, only if there no is existing system information.
	// Also sets the properties, if possible.
	// If error, throws an exception and leaves system information in the unloaded state.

	public static synchronized void load_new_sys_info () {
		if (!( f_loaded_sys_info )) {
			do_load_sys_info();
		}
		return;
	}




	// Return true if system information is loaded.

	public static synchronized boolean is_sys_info_loaded () {
		return f_loaded_sys_info;
	}




	// Convert the global state information to a string.
	// This is primarily for testing.

	public static synchronized String global_info_to_string () {
		StringBuilder result = new StringBuilder();
		result.append ("MongoDBSSLParams (static):" + "\n");
		
		result.append ("f_search_jar_dir = " + f_search_jar_dir + "\n");
		result.append ("f_use_ssl_context = " + f_use_ssl_context + "\n");
		
		result.append ("f_loaded_sys_info = " + f_loaded_sys_info + "\n");
		result.append ("oaf_ssl_dir = " + ((oaf_ssl_dir == null) ? "<null>" : (oaf_ssl_dir.toString())) + "\n");
		result.append ("oaf_trust_store = " + ((oaf_trust_store == null) ? "<null>" : oaf_trust_store) + "\n");
		result.append ("oaf_key_store = " + ((oaf_key_store == null) ? "<null>" : oaf_key_store) + "\n");
		result.append ("oaf_key_store_pass = " + ((oaf_key_store_pass == null) ? "<null>" : oaf_key_store_pass) + "\n");

		result.append ("user_key_store_pass = " + ((user_key_store_pass == null) ? "<null>" : user_key_store_pass) + "\n");
		
		result.append ("f_set_prop_trust_store = " + f_set_prop_trust_store + "\n");
		result.append ("saved_prop_trust_store = " + ((saved_prop_trust_store == null) ? "<null>" : saved_prop_trust_store) + "\n");
		result.append ("saved_prop_trust_store_pass = " + ((saved_prop_trust_store_pass == null) ? "<null>" : saved_prop_trust_store_pass) + "\n");
		
		result.append ("f_set_prop_key_store = " + f_set_prop_key_store + "\n");
		result.append ("saved_prop_key_store = " + ((saved_prop_key_store == null) ? "<null>" : saved_prop_key_store) + "\n");
		result.append ("saved_prop_key_store_pass = " + ((saved_prop_key_store_pass == null) ? "<null>" : saved_prop_key_store_pass) + "\n");

		result.append ("mongo_ssl_context = " + ((mongo_ssl_context == null) ? "<null>" : "<available>") + "\n");
		result.append ("context_trust_store = " + ((context_trust_store == null) ? "<null>" : context_trust_store) + "\n");
		result.append ("context_key_store = " + ((context_key_store == null) ? "<null>" : context_key_store) + "\n");
		result.append ("context_key_store_pass = " + ((context_key_store_pass == null) ? "<null>" : context_key_store_pass) + "\n");

		return result.toString();
	}




	//----- Testing -----




	// Display a system property.
	// Parameters:
	//  key = Property key.

	private static void show_sys_prop (String key) {
		String value = System.getProperty (key);
		if (value == null) {
			value = "<null>";
		}
		System.out.println (key + " = " + value);
		return;
	}




	// Display all the store properties.

	private static void show_all_store_props () {
		show_sys_prop (SPROP_SSL_TRUST_STORE);
		show_sys_prop (SPROP_SSL_TRUST_STORE_PASS);
		show_sys_prop (SPROP_SSL_KEY_STORE);
		show_sys_prop (SPROP_SSL_KEY_STORE_PASS);
		return;
	}



	// Display all global state informatoin.

	private static void show_all_global_state () {
		System.out.println (global_info_to_string());
		System.out.println ("needs_pasword = " + needs_password());
		System.out.println ("requires_user_password = " + requires_user_password());
		String upass = get_user_password();
		System.out.println ("get_user_password = " + ((upass == null) ? "<null>" : upass));
		System.out.println ();
		show_all_store_props();
		return;
	}




	// A list of SSL option strings to use for testing.

	private static String[] test_options = {
		"",
		"auto",
		"True",
		"  FALSE  ",

		"sslEnable=true",
		"sslEnable = false",
		" sslEnable=auto ",
		"sslClientAuth=TRUE",
		"sslClientAuth = FALSE",
		" sslClientAuth=AUTO ",
		"sslInvalidHostAddr=True",
		"sslInvalidHostAddr = False",
		" sslInvalidHostAddr=Auto ",

		"sslEnable=auto&sslClientAuth=auto",
		"sslEnable=true&sslClientAuth=true",
		"sslEnable=true & sslClientAuth=false",
		"sslClientAuth=true&sslEnable=false",
		" sslClientAuth = false & sslEnable = false ",
		"sslEnable=true&sslClientAuth=true&sslInvalidHostAddr=true",

		// and some bad strings

		"bad",
		"sslEnable=bad",
		"sslClientAuth = bad",
		" sslInvalidHostAddr = BAD ",
		"sslBad=true"
	};




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Display all the store properties.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Display store properties

			show_all_store_props();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Test of load environment, set user password, clear environment.

		if (testargs.is_test ("test2")) {

			// Zero additional argument

			testargs.end_test();

			// Show global state while unloaded

			System.out.println ();
			System.out.println ("*** Initial unloaded state");
			System.out.println ();

			show_all_global_state();

			// Load system information

			System.out.println ();
			System.out.println ("*** Load system information");
			System.out.println ();

			load_new_sys_info();

			show_all_global_state();

			// Set user password

			System.out.println ();
			System.out.println ("*** Set user password");
			System.out.println ();

			set_user_password ("test_user_pass");

			show_all_global_state();

			// Clear system information

			System.out.println ();
			System.out.println ("*** Clear system information");
			System.out.println ();

			clear_sys_info();

			show_all_global_state();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Test of SSL option string parsing and resolution.

		if (testargs.is_test ("test3")) {

			// Zero additional argument

			testargs.end_test();

			// Load system information

			System.out.println ();
			System.out.println ("*** Load system information");
			System.out.println ();

			load_new_sys_info();

			show_all_global_state();

			// Test option strings

			System.out.println ();
			System.out.println ("*** Option string tests ");
			System.out.println ();

			for (String options : test_options) {

				System.out.println ();
				System.out.println ("Options: \"" + options + "\"");

				boolean is_valid = is_valid_ssl_option_string (options);
				System.out.println ("is_valid = " + is_valid);

				if (is_valid) {
					System.out.println ();
					System.out.println (parse_and_print_ssl_options (options));
					System.out.println (resolve_and_print_ssl_options (options));
				}
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  ks_type  ks_filename  ks_password
		// Test if the given password is correct for the given keystore and type.
		// As a special case, ks_type="-" selects the default keystore type.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Check keystore for correct password");
			String ks_type = testargs.get_string ("ks_type");
			String ks_filename = testargs.get_string ("ks_filename");
			String ks_password = testargs.get_string ("ks_password");
			testargs.end_test();

			// Apply default type if requested

			if (ks_type.equals ("-")) {
				ks_type = KeyStore.getDefaultType();

				System.out.println ();
				System.out.println ("Using default keystore type: ks_type = " + ks_type);
			}

			// Check password

			String ckpw = check_keystore_password (ks_type, ks_filename, ks_password);

			if (ckpw == null) {
				System.out.println ();
				System.out.println ("Password is correct!");
			}
			else {
				System.out.println ();
				System.out.println ("Password is incorrect, or some other error.");
				System.out.println ();
				System.out.println ("Error reason:");
				System.out.println (ckpw);
			}

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  user_pass
		// Test of check and set user password.

		if (testargs.is_test ("test5")) {

			// Read arguments

			String user_pass = testargs.get_string ("user_pass");
			testargs.end_test();

			// Load system information

			System.out.println ();
			System.out.println ("*** Load system information");
			System.out.println ();

			load_new_sys_info();

			show_all_global_state();

			// Check and set user password

			System.out.println ();
			System.out.println ("*** Check and set user password");
			System.out.println ();

			String ckpw = check_and_set_user_password (user_pass);

			if (ckpw == null) {
				System.out.println ();
				System.out.println ("Password is correct!");
			}
			else {
				System.out.println ();
				System.out.println ("Password is incorrect, or some other error.");
				System.out.println ();
				System.out.println ("Error reason:");
				System.out.println (ckpw);
			}

			System.out.println ();
			show_all_global_state();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6
		// Test location of jar directory.

		if (testargs.is_test ("test6")) {

			// Zero additional argument

			testargs.end_test();

			// Test subroutine 1

			String jar_dir_1 = find_jar_dir_sub_1 (true);

			System.out.println ();
			System.out.println ("Jar directory sub 1 : " + ((jar_dir_1 == null) ? "<null>" : jar_dir_1));

			// Test subroutine 2

			String jar_dir_2 = find_jar_dir_sub_2 (true);

			System.out.println ();
			System.out.println ("Jar directory sub 2 : " + ((jar_dir_2 == null) ? "<null>" : jar_dir_2));

			// Test main routine

			String jar_dir = find_jar_dir();

			System.out.println ();
			System.out.println ("Jar directory : " + ((jar_dir == null) ? "<null>" : jar_dir));

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7
		// Test of load environment, set user password, clear environment.
		// Same as test #2 except sets the flag to search the jar directory.

		if (testargs.is_test ("test7")) {

			// Zero additional argument

			testargs.end_test();

			// Set the flag to search the jar directory

			set_search_jar_dir (true);

			// Show global state while unloaded

			System.out.println ();
			System.out.println ("*** Initial unloaded state");
			System.out.println ();

			show_all_global_state();

			// Load system information

			System.out.println ();
			System.out.println ("*** Load system information");
			System.out.println ();

			load_new_sys_info();

			show_all_global_state();

			// Set user password

			System.out.println ();
			System.out.println ("*** Set user password");
			System.out.println ();

			set_user_password ("test_user_pass");

			show_all_global_state();

			// Clear system information

			System.out.println ();
			System.out.println ("*** Clear system information");
			System.out.println ();

			clear_sys_info();

			show_all_global_state();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8
		// Test of load environment, clear environment, using SSLContext.
		// Similar to test #7 except sets the flag to search the jar directory.

		if (testargs.is_test ("test8")) {

			// Zero additional argument

			testargs.end_test();

			// Set the flag to use SSLContext

			set_use_ssl_context (true);

			// Set the flag to search the jar directory

			set_search_jar_dir (true);

			// Show global state while unloaded

			System.out.println ();
			System.out.println ("*** Initial unloaded state");
			System.out.println ();

			show_all_global_state();

			// Load system information

			System.out.println ();
			System.out.println ("*** Load system information");
			System.out.println ();

			load_new_sys_info();

			show_all_global_state();

			// Clear system information

			System.out.println ();
			System.out.println ("*** Clear system information");
			System.out.println ();

			clear_sys_info();

			show_all_global_state();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
