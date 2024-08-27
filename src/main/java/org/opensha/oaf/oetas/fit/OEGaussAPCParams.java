package org.opensha.oaf.oetas.fit;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.util.OEArraysCalc;

// Used for 3x3 matrix inversion and determinant
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;

// Used only for initial import from the CSV file
import org.opensha.commons.data.CSVFile;
import org.opensha.oaf.etas.GenericETAS_ParametersFetch;


// Class to hold parameters for a Gauss distribution on a/p/c.
// Author: Michael Barall 05/04/2022.
// Based on code by Nicholas van der Elst.

public class OEGaussAPCParams implements Marshalable {

	//----- Configurable parameters -----

	// Name of the tectonic regime (must not be null).

	private String regimeName;

	// Mean and standard deviation of a, also used for ams.

	private double aValue_mean;
	private double aValue_sigma;

	// Mean of log c, but not used in calculating the prior.

	private double log_cValue;
	
	// Mean of p.

	private double pValue;

	// Covariance matrix among a, p, and c.

	private double covaa;
	private double covpp;
	private double covcc;
	private double covap;
	private double covac;
	private double covcp;

	// Number of sequences upon which the data is based.

	private int numSequences;

	// Mean values of alpha and b, apparently always 1.0.

	private double alpha;
	private double bValue;

	// Magnitude range.

	private double refMag;
	private double maxMag;





	//----- Assumed parameters -----

	// Central value of c (c value corresponding to the mean of log c).

	private double cValue;

	// Standard deviation of p.
				
	private double pValue_sigma;

	// Standard deviation of log c.

	private double logcValue_sigma;

	// Standard deviation of b.
				
	private double bValue_sigma;

	// Mean and standard deviation of ams.

	private double mean_ams;
	private double sigma_ams;




	// Set up assumed parameters, version 1.
	// Assumes that the configurable parameters are already set up.

	private void setup_assumed_params_v1 () {

		//	cValue = Math.pow(10.0, log_cValue);
		// From NvdE: due to memory concerns, we're replacing the generic c-value with the global average c-value
		// Note: GenericETAS_Parameters says it should be Math.pow(10,-2.565)
		cValue = Math.pow(10.0, -2.5);
				
		// Comes out to 0.127 for global, but GenericETAS_Parameters says it should be 0.2 for global
		pValue_sigma = Math.sqrt(covpp*numSequences);	// From NvdE: this needs to be replaced with a real estimate

		// From NvdE: due to memory concerns, we're replacing logcValue_sigma with a value that makes 3*sigma value equal to -5, 0
		// Note: GenericETAS_Parameters says it should be 0.7
		logcValue_sigma = 0.8333;		// From NvdE: this needs to be replaced with a real estimate
				
		bValue_sigma = 0.1;	// From NvdE: this needs to be replaced with a real estimate

		// Mean and standard deviation of ams is assumed to be the same as a

		mean_ams = aValue_mean;
		sigma_ams = aValue_sigma;

		return;
	}




	//----- Derived parameters -----

	// Mean value of log c.

	private double mean_logc;

	// Covariance matrix, inverse, and determinant, as obtained from the parameters.

	private double[][] covariance;
	private double[][] covInverse;
	private double covDeterminant;

	// Coveriance matrix, inverse, and determinant, scaled for computing the prior.

	private double[][] priorCovariance;
	private double[][] priorCovInverse;
	private double priorCovDeterminant;

	// Log normalizaton factor for the prior of a, p, c.

	private double log_norm_prior_a_p_c;

	// Log normalization factor for the prior of ams.

	private double log_norm_prior_ams;




	// Set up derived parameters.
	// Assumes that the configurable and assumed parameters are already set up.

	private void setup_derived_params () {

		// Mean of log c (Note we do not use log_cValue from the comfigurable parameters)

		mean_logc = Math.log10(cValue);

		// Covariance matrice from the parameters

		covariance = new double[][]{
			{covaa, covap, covac},
			{covap, covpp, covcp},
			{covac, covcp, covcc}
		};

		// Invert covariance matrix

		RealMatrix COV = MatrixUtils.createRealMatrix(covariance);
		RealMatrix iCOV = MatrixUtils.blockInverse(COV, 1);
		double[][] icovariance = new double[3][3];
		for(int i = 0; i < 3 ; i++){
			for(int j = 0; j < 3 ; j++){
				icovariance[i][j] = iCOV.getEntry(i, j);
			}
		}
		covInverse = icovariance;
		
		// Find determinant of covariance matrix

		double determinant = new CholeskyDecomposition(COV).getDeterminant();
		covDeterminant = determinant;

		// Coveriance matrix used for priors
		
		//  priorCovariance = new double[][]{
		//  	{covaa*numSequences, covap*numSequences, covac*numSequences},
		//  	{covap*numSequences, covpp*numSequences, covcp*numSequences},
		//  	{covac*numSequences, covcp*numSequences, covcc*numSequences}
		//  };

		priorCovariance = new double[][]{
			{aValue_sigma*aValue_sigma, covap*numSequences, covac*numSequences},
			{covap*numSequences, covpp*numSequences, covcp*numSequences},
			{covac*numSequences, covcp*numSequences, covcc*numSequences}
		};

		// Invert covariance matrix

		RealMatrix pCOV = MatrixUtils.createRealMatrix(priorCovariance);
		RealMatrix ipCOV = MatrixUtils.blockInverse(pCOV, 1);
		double[][] ipCovariance = new double[3][3];
		for(int i = 0; i < 3 ; i++){
			for(int j = 0; j < 3 ; j++){
				ipCovariance[i][j] = ipCOV.getEntry(i, j);
			}
		}
		priorCovInverse = ipCovariance;

		// Find determinant of covariance matrix

		double pDeterminant = new CholeskyDecomposition(pCOV).getDeterminant();
		priorCovDeterminant = pDeterminant;

		// Log normalization factors for the Gaussians
		// Note: These could be set to zero, because we provide unnormalized priors.

		log_norm_prior_a_p_c = Math.log( 1.0/Math.sqrt(Math.pow(2.0*Math.PI,3.0) * priorCovDeterminant) );

		log_norm_prior_ams = Math.log ( 1.0/Math.sqrt(2.0*Math.PI)/sigma_ams );

		return;
	}





	//----- Evaluation -----




	// Get the regime name.

	public final String get_regimeName () {
		return regimeName;
	}




	// Accessor functions.

	public final double get_aValue_mean () {
		return aValue_mean;
	}

	public final double get_aValue_sigma () {
		return aValue_sigma;
	}

	public final double get_log_cValue () {
		return log_cValue;
	}

	public final double get_pValue () {
		return pValue;
	}

	public final double get_covaa () {
		return covaa;
	}

	public final double get_covpp () {
		return covpp;
	}

	public final double get_covcc () {
		return covcc;
	}

	public final double get_covap () {
		return covap;
	}

	public final double get_covac () {
		return covac;
	}

	public final double get_covcp () {
		return covcp;
	}

	public final int get_numSequences () {
		return numSequences;
	}

	public final double get_alpha () {
		return alpha;
	}

	public final double get_bValue () {
		return bValue;
	}

	public final double get_refMag () {
		return refMag;
	}

	public final double get_maxMag () {
		return maxMag;
	}

	public final double get_cValue () {
		return cValue;
	}

	public final double get_pValue_sigma () {
		return pValue_sigma;
	}

	public final double get_logcValue_sigma () {
		return logcValue_sigma;
	}

	public final double get_bValue_sigma () {
		return bValue_sigma;
	}

	public final double get_mean_ams () {
		return mean_ams;
	}

	public final double get_sigma_ams () {
		return sigma_ams;
	}




	// Calculate the log-prior likelhood for given a, p, and c.
	// Note: The value of a must be for the magnitude range [refMag, maxMag];

	public final double log_prior_likelihood_a_p_c (double a, double p, double c) {
		
		double logc = Math.log10(c);
		
		double[] delta = {a - aValue_mean, p - pValue, logc - mean_logc};
		
		double cid = 0.0;
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < 3; j++){
				cid += delta[i] * delta[j] * priorCovInverse[j][i];
			}
		}

		double log_like = -0.5*cid + log_norm_prior_a_p_c;
		return log_like;
	}




	// Calculate the log-prior likelhood for given ams.
	// Note: The value of ams must be for the magnitude range [refMag, maxMag];

	public final double log_prior_likelihood_ams (double ams) {

		double delta = ams - mean_ams;

		double log_like = (-0.5*(delta*delta) / (sigma_ams*sigma_ams)) + log_norm_prior_ams;
		return log_like;
	}




	// Calculate the log-prior likelhood for given ams, a, p, and c.
	// Note: The values of ams and a must be for the magnitude range [refMag, maxMag];

	public final double log_prior_likelihood_ams_a_p_c (double ams, double a, double p, double c) {

		double log_like = log_prior_likelihood_a_p_c (a, p, c) + log_prior_likelihood_ams (ams);
		return log_like;
	}




	//----- Construction -----



	// Clear contents.

	public final void clear () {

		// Configurable parameters

		regimeName = "";
		aValue_mean = 0.0;
		aValue_sigma = 0.0;
		log_cValue = 0.0;
		pValue = 0.0;
		covaa = 0.0;
		covpp = 0.0;
		covcc = 0.0;
		covap = 0.0;
		covac = 0.0;
		covcp = 0.0;
		numSequences = 0;
		alpha = 0.0;
		bValue = 0.0;
		refMag = 0.0;
		maxMag = 0.0;

		// Assumed parameters

		cValue = 0.0;
		pValue_sigma = 0.0;
		logcValue_sigma = 0.0;
		bValue_sigma = 0.0;
		mean_ams = 0.0;
		sigma_ams = 0.0;

		// Derived parameters

		mean_logc = 0.0;
		covariance = null;
		covInverse = null;
		covDeterminant = 0.0;
		priorCovariance = null;
		priorCovInverse = null;
		priorCovDeterminant = 0.0;
		log_norm_prior_a_p_c = 0.0;
		log_norm_prior_ams = 0.0;

		return;
	}




	// Default constructor.

	public OEGaussAPCParams () {
		clear();
	}




	// Set the values.
	// This sets configurable parameters, and computes the rest.

	public final OEGaussAPCParams set (
		String regimeName,
		double aValue_mean,
		double aValue_sigma,
		double log_cValue,
		double pValue,
		double covaa,
		double covpp,
		double covcc,
		double covap,
		double covac,
		double covcp,
		int numSequences,
		double alpha,
		double bValue,
		double refMag,
		double maxMag
	) {
		this.regimeName		= regimeName;
		this.aValue_mean	= aValue_mean;
		this.aValue_sigma	= aValue_sigma;
		this.log_cValue		= log_cValue;
		this.pValue			= pValue;
		this.covaa			= covaa;
		this.covpp			= covpp;
		this.covcc			= covcc;
		this.covap			= covap;
		this.covac			= covac;
		this.covcp			= covcp;
		this.numSequences	= numSequences;
		this.alpha			= alpha;
		this.bValue			= bValue;
		this.refMag			= refMag;
		this.maxMag			= maxMag;

		setup_assumed_params_v1();
		setup_derived_params();
		return this;
	}




	// Set the values.
	// This sets configurable and assumed parameters, and computes the rest.

	public final OEGaussAPCParams set (
		String regimeName,
		double aValue_mean,
		double aValue_sigma,
		double log_cValue,
		double pValue,
		double covaa,
		double covpp,
		double covcc,
		double covap,
		double covac,
		double covcp,
		int numSequences,
		double alpha,
		double bValue,
		double refMag,
		double maxMag,
		double cValue,
		double pValue_sigma,
		double logcValue_sigma,
		double bValue_sigma,
		double mean_ams,
		double sigma_ams
	) {
		this.regimeName		= regimeName;
		this.aValue_mean	= aValue_mean;
		this.aValue_sigma	= aValue_sigma;
		this.log_cValue		= log_cValue;
		this.pValue			= pValue;
		this.covaa			= covaa;
		this.covpp			= covpp;
		this.covcc			= covcc;
		this.covap			= covap;
		this.covac			= covac;
		this.covcp			= covcp;
		this.numSequences	= numSequences;
		this.alpha			= alpha;
		this.bValue			= bValue;
		this.refMag			= refMag;
		this.maxMag			= maxMag;

		this.cValue				= cValue;
		this.pValue_sigma		= pValue_sigma;
		this.logcValue_sigma	= logcValue_sigma;
		this.bValue_sigma		= bValue_sigma;
		this.mean_ams			= mean_ams;
		this.sigma_ams			= sigma_ams;

		setup_derived_params();
		return this;
	}




	// Name of global regime.

	public static final String GLOBAL_REGIME = "GLOBAL-AVERAGE";

	// Name of California regime.

	public static final String CALIFORNIA_REGIME = "CALIFORNIA";

	// Regime name for analyst-supplied parameters.

	public static final String ANALYST_REGIME = "ANALYST";


	// Set to global values.

	public final OEGaussAPCParams set_to_global () {
		set (
			GLOBAL_REGIME,	// regimeName
			-2.43,			// aValue_mean
			0.395,			// aValue_sigma
			-2.565,			// log_cValue
			0.966,			// pValue
			0.00000713,		// covaa
			0.00000767,		// covpp
			0.000899,		// covcc
			0.00000263,		// covap
			0.0000375,		// covac
			0.000045,		// covcp
			2099,			// numSequences
			1.0,			// alpha
			1.0,			// bValue
			4.5,			// refMag
			9.5				// maxMag
		);

		return this;
	}




	// Copy a 2D array, or null.

	private double[][] copy_2d_array_or_null (double[][] x) {
		if (x == null) {
			return null;
		}
		return OEArraysCalc.array_copy (x);
	}




	// Copy the values.

	public final OEGaussAPCParams copy_from (OEGaussAPCParams other) {
		this.regimeName		= other.regimeName;
		this.aValue_mean	= other.aValue_mean;
		this.aValue_sigma	= other.aValue_sigma;
		this.log_cValue		= other.log_cValue;
		this.pValue			= other.pValue;
		this.covaa			= other.covaa;
		this.covpp			= other.covpp;
		this.covcc			= other.covcc;
		this.covap			= other.covap;
		this.covac			= other.covac;
		this.covcp			= other.covcp;
		this.numSequences	= other.numSequences;
		this.alpha			= other.alpha;
		this.bValue			= other.bValue;
		this.refMag			= other.refMag;
		this.maxMag			= other.maxMag;

		this.cValue				= other.cValue;
		this.pValue_sigma		= other.pValue_sigma;
		this.logcValue_sigma	= other.logcValue_sigma;
		this.bValue_sigma		= other.bValue_sigma;
		this.mean_ams			= other.mean_ams;
		this.sigma_ams			= other.sigma_ams;

		this.mean_logc				= other.mean_logc;
		this.covariance				= copy_2d_array_or_null (other.covariance);
		this.covInverse				= copy_2d_array_or_null (other.covInverse);
		this.covDeterminant			= other.covDeterminant;
		this.priorCovariance		= copy_2d_array_or_null (other.priorCovariance);
		this.priorCovInverse		= copy_2d_array_or_null (other.priorCovInverse);
		this.priorCovDeterminant	= other.priorCovDeterminant;
		this.log_norm_prior_a_p_c	= other.log_norm_prior_a_p_c;
		this.log_norm_prior_ams		= other.log_norm_prior_ams;

		return this;
	}




	// Display a 2D array, or null.
	// Assumes that second-level arrays are not null.

	private void display_2d_array_or_null (StringBuilder sb, String name, double[][] x) {
		if (x == null) {
			sb.append (name + " = null" + "\n");
		}
		else {
			for(int i = 0; i < x.length ; i++){
				for(int j = 0; j < x[i].length ; j++){
					sb.append (name + "[" + i + "][" + j + "] = " + x[i][j] + "\n");
				}
			}
		}
		return;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGaussAPCParams:" + "\n");

		result.append ("regimeName = "		+ regimeName + "\n");
		result.append ("aValue_mean = "		+ aValue_mean + "\n");
		result.append ("aValue_sigma = "	+ aValue_sigma + "\n");
		result.append ("log_cValue = "		+ log_cValue + "\n");
		result.append ("pValue = "			+ pValue + "\n");
		result.append ("covaa = "			+ covaa + "\n");
		result.append ("covpp = "			+ covpp + "\n");
		result.append ("covcc = "			+ covcc + "\n");
		result.append ("covap = "			+ covap + "\n");
		result.append ("covac = "			+ covac + "\n");
		result.append ("covcp = "			+ covcp + "\n");
		result.append ("numSequences = "	+ numSequences + "\n");
		result.append ("alpha = "			+ alpha + "\n");
		result.append ("bValue = "			+ bValue + "\n");
		result.append ("refMag = "			+ refMag + "\n");
		result.append ("maxMag = "			+ maxMag + "\n");

		result.append ("cValue = "				+ cValue + "\n");
		result.append ("pValue_sigma = "		+ pValue_sigma + "\n");
		result.append ("logcValue_sigma = "		+ logcValue_sigma + "\n");
		result.append ("bValue_sigma = "		+ bValue_sigma + "\n");
		result.append ("mean_ams = "			+ mean_ams + "\n");
		result.append ("sigma_ams = "			+ sigma_ams + "\n");

		result.append ("mean_logc = "				+ mean_logc + "\n");
		result.append ("covDeterminant = "			+ covDeterminant + "\n");
		result.append ("priorCovDeterminant = "		+ priorCovDeterminant + "\n");
		result.append ("log_norm_prior_a_p_c = "	+ log_norm_prior_a_p_c + "\n");
		result.append ("log_norm_prior_ams = "		+ log_norm_prior_ams + "\n");

		display_2d_array_or_null (result, "covariance", covariance);
		display_2d_array_or_null (result, "covInverse", covInverse);
		display_2d_array_or_null (result, "priorCovariance", priorCovariance);
		display_2d_array_or_null (result, "priorCovInverse", priorCovInverse);

		return result.toString();
	}




	// Display our contents, showing only adjustable parameters

	public String to_string_2 () {
		StringBuilder result = new StringBuilder();

		result.append ("OEGaussAPCParams:" + "\n");

		result.append ("regimeName = "		+ regimeName + "\n");
		result.append ("aValue_mean = "		+ aValue_mean + "\n");
		result.append ("aValue_sigma = "	+ aValue_sigma + "\n");
		result.append ("log_cValue = "		+ log_cValue + "\n");
		result.append ("pValue = "			+ pValue + "\n");
		result.append ("covaa = "			+ covaa + "\n");
		result.append ("covpp = "			+ covpp + "\n");
		result.append ("covcc = "			+ covcc + "\n");
		result.append ("covap = "			+ covap + "\n");
		result.append ("covac = "			+ covac + "\n");
		result.append ("covcp = "			+ covcp + "\n");
		result.append ("numSequences = "	+ numSequences + "\n");
		result.append ("alpha = "			+ alpha + "\n");
		result.append ("bValue = "			+ bValue + "\n");
		result.append ("refMag = "			+ refMag + "\n");
		result.append ("maxMag = "			+ maxMag + "\n");

		result.append ("cValue = "				+ cValue + "\n");
		result.append ("pValue_sigma = "		+ pValue_sigma + "\n");
		result.append ("logcValue_sigma = "		+ logcValue_sigma + "\n");
		result.append ("bValue_sigma = "		+ bValue_sigma + "\n");
		result.append ("mean_ams = "			+ mean_ams + "\n");
		result.append ("sigma_ams = "			+ sigma_ams + "\n");

		return result.toString();
	}




	// Produce a one-line summary string.

	public final String summary_string () {
		String result = "OEGaussAPCParams["
			+ regimeName + ", "
			+ aValue_mean + ", "
			+ aValue_sigma + ", "
			+ log_cValue + ", "
			+ pValue + ", "
			+ covaa + ", "
			+ covpp + ", "
			+ covcc + ", "
			+ covap + ", "
			+ covac + ", "
			+ covcp + ", "
			+ numSequences + ", "
			+ alpha + ", "
			+ bValue + ", "
			+ refMag + ", "
			+ maxMag + "]";
		return result;
	}




	// Produce a one-line summary string, including assumed parameters.

	public final String summary_string_2 () {
		String result = "OEGaussAPCParams["
			+ regimeName + ", "
			+ aValue_mean + ", "
			+ aValue_sigma + ", "
			+ log_cValue + ", "
			+ pValue + ", "
			+ covaa + ", "
			+ covpp + ", "
			+ covcc + ", "
			+ covap + ", "
			+ covac + ", "
			+ covcp + ", "
			+ numSequences + ", "
			+ alpha + ", "
			+ bValue + ", "
			+ refMag + ", "
			+ maxMag + ", "
			+ cValue + ", "
			+ pValue_sigma + ", "
			+ logcValue_sigma + ", "
			+ bValue_sigma + ", "
			+ mean_ams + ", "
			+ sigma_ams + "]";
		return result;
	}




	//----- Import from CSV -----




	// Import a row from the CSV.
	// Returns this object.

	private OEGaussAPCParams import_csv_row (CSVFile<String> csv, int row) {

		// Read configurable parameters

		regimeName = csv.get(row, 0).trim();
		aValue_mean = Double.parseDouble(csv.get(row, 1));
		aValue_sigma = Double.parseDouble(csv.get(row, 2));
		log_cValue = Double.parseDouble(csv.get(row, 3));			
		pValue = Double.parseDouble(csv.get(row, 4));
		covaa = Double.parseDouble(csv.get(row, 5));
		covpp = Double.parseDouble(csv.get(row, 6));
		covcc = Double.parseDouble(csv.get(row, 7));
		covap = Double.parseDouble(csv.get(row, 8));
		covac = Double.parseDouble(csv.get(row, 9));
		covcp = Double.parseDouble(csv.get(row, 10));
		numSequences = Integer.parseInt(csv.get(row, 11));
		alpha = Double.parseDouble(csv.get(row, 12));
		bValue = Double.parseDouble(csv.get(row, 13));
		refMag = Double.parseDouble(csv.get(row, 14));
		maxMag = Double.parseDouble(csv.get(row, 15));

		// Compute remaining parameters

		setup_assumed_params_v1();
		setup_derived_params();
		return this;
	}




	// Import all the rows from the CSV.
	// Returns a list of the resulting objects.
	// Note: The first row (row 0) consists of column headings, and so is skipped.

	private static List<OEGaussAPCParams> import_csv_rows (CSVFile<String> csv) {
		if (!( csv.getNumCols() == 16 )) {
			throw new MarshalException ("OEGaussAPCParams.import_csv_rows: Incorrect number of columns: " + csv.getNumCols());
		}

		List<OEGaussAPCParams> result = new ArrayList<OEGaussAPCParams>();

		for (int row = 1; row < csv.getNumRows(); row++) {
			result.add ((new OEGaussAPCParams()).import_csv_row (csv, row));
		}

		return result;
	}




	// Import all the rows from the embedded CSV.
	// Returns a list of the resulting objects.

	public static List<OEGaussAPCParams> import_embedded_csv () {
		List<OEGaussAPCParams> result;

		try {
			URL paramsURL = GenericETAS_ParametersFetch.class.getResource ("resources/vdEGenericETASParams_080919.csv");
			CSVFile<String> csv = CSVFile.readURL (paramsURL, true);
			result = import_csv_rows (csv);
		}
		catch (Exception e) {
			throw new MarshalException ("OEGaussAPCParams.import_embedded_csv: Error importing embedded CSV", e);
		}

		return result;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 133001;	// marshals only configurable parameters
	private static final int MARSHAL_VER_2 = 133002;	// marshals configurable and assumed parameters

	private static final String M_VERSION_NAME = "OEGaussAPCParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalString ("regimeName", regimeName);
			writer.marshalDouble ("aValue_mean", aValue_mean);
			writer.marshalDouble ("aValue_sigma", aValue_sigma);
			writer.marshalDouble ("log_cValue", log_cValue);
			writer.marshalDouble ("pValue", pValue);
			writer.marshalDouble ("covaa", covaa);
			writer.marshalDouble ("covpp", covpp);
			writer.marshalDouble ("covcc", covcc);
			writer.marshalDouble ("covap", covap);
			writer.marshalDouble ("covac", covac);
			writer.marshalDouble ("covcp", covcp);
			writer.marshalInt ("numSequences", numSequences);
			writer.marshalDouble ("alpha", alpha);
			writer.marshalDouble ("bValue", bValue);
			writer.marshalDouble ("refMag", refMag);
			writer.marshalDouble ("maxMag", maxMag);

		}
		break;

		case MARSHAL_VER_2: {

			writer.marshalString ("regimeName", regimeName);
			writer.marshalDouble ("aValue_mean", aValue_mean);
			writer.marshalDouble ("aValue_sigma", aValue_sigma);
			writer.marshalDouble ("log_cValue", log_cValue);
			writer.marshalDouble ("pValue", pValue);
			writer.marshalDouble ("covaa", covaa);
			writer.marshalDouble ("covpp", covpp);
			writer.marshalDouble ("covcc", covcc);
			writer.marshalDouble ("covap", covap);
			writer.marshalDouble ("covac", covac);
			writer.marshalDouble ("covcp", covcp);
			writer.marshalInt ("numSequences", numSequences);
			writer.marshalDouble ("alpha", alpha);
			writer.marshalDouble ("bValue", bValue);
			writer.marshalDouble ("refMag", refMag);
			writer.marshalDouble ("maxMag", maxMag);

			writer.marshalDouble ("cValue", cValue);
			writer.marshalDouble ("pValue_sigma", pValue_sigma);
			writer.marshalDouble ("logcValue_sigma", logcValue_sigma);
			writer.marshalDouble ("bValue_sigma", bValue_sigma);
			writer.marshalDouble ("mean_ams", mean_ams);
			writer.marshalDouble ("sigma_ams", sigma_ams);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			regimeName = reader.unmarshalString ("regimeName");
			aValue_mean = reader.unmarshalDouble ("aValue_mean");
			aValue_sigma = reader.unmarshalDouble ("aValue_sigma");
			log_cValue = reader.unmarshalDouble ("log_cValue");
			pValue = reader.unmarshalDouble ("pValue");
			covaa = reader.unmarshalDouble ("covaa");
			covpp = reader.unmarshalDouble ("covpp");
			covcc = reader.unmarshalDouble ("covcc");
			covap = reader.unmarshalDouble ("covap");
			covac = reader.unmarshalDouble ("covac");
			covcp = reader.unmarshalDouble ("covcp");
			numSequences = reader.unmarshalInt ("numSequences");
			alpha = reader.unmarshalDouble ("alpha");
			bValue = reader.unmarshalDouble ("bValue");
			refMag = reader.unmarshalDouble ("refMag");
			maxMag = reader.unmarshalDouble ("maxMag");

			try {
				setup_assumed_params_v1();
				setup_derived_params();
			}
			catch (Exception e) {
				throw new MarshalException ("OEGaussAPCParams.do_umarshal: Error completing setup", e);
			}

		}
		break;

		case MARSHAL_VER_2: {

			regimeName = reader.unmarshalString ("regimeName");
			aValue_mean = reader.unmarshalDouble ("aValue_mean");
			aValue_sigma = reader.unmarshalDouble ("aValue_sigma");
			log_cValue = reader.unmarshalDouble ("log_cValue");
			pValue = reader.unmarshalDouble ("pValue");
			covaa = reader.unmarshalDouble ("covaa");
			covpp = reader.unmarshalDouble ("covpp");
			covcc = reader.unmarshalDouble ("covcc");
			covap = reader.unmarshalDouble ("covap");
			covac = reader.unmarshalDouble ("covac");
			covcp = reader.unmarshalDouble ("covcp");
			numSequences = reader.unmarshalInt ("numSequences");
			alpha = reader.unmarshalDouble ("alpha");
			bValue = reader.unmarshalDouble ("bValue");
			refMag = reader.unmarshalDouble ("refMag");
			maxMag = reader.unmarshalDouble ("maxMag");

			cValue = reader.unmarshalDouble ("cValue");
			pValue_sigma = reader.unmarshalDouble ("pValue_sigma");
			logcValue_sigma = reader.unmarshalDouble ("logcValue_sigma");
			bValue_sigma = reader.unmarshalDouble ("bValue_sigma");
			mean_ams = reader.unmarshalDouble ("mean_ams");
			sigma_ams = reader.unmarshalDouble ("sigma_ams");

			try {
				setup_derived_params();
			}
			catch (Exception e) {
				throw new MarshalException ("OEGaussAPCParams.do_umarshal: Error completing setup", e);
			}

		}
		break;

		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public OEGaussAPCParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEGaussAPCParams gauss_apc_params) {
		gauss_apc_params.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEGaussAPCParams static_unmarshal (MarshalReader reader, String name) {
		return (new OEGaussAPCParams()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEGaussAPCParams make_test_value () {
		OEGaussAPCParams gauss_apc_params = new OEGaussAPCParams();

		gauss_apc_params.set_to_global ();

		return gauss_apc_params;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEGaussAPCParams");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying catalog info");
			testargs.end_test();

			// Create the values

			OEGaussAPCParams gauss_apc_params = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (gauss_apc_params.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (gauss_apc_params);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (gauss_apc_params);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEGaussAPCParams gauss_apc_params2 = new OEGaussAPCParams();
			MarshalUtils.from_json_string (gauss_apc_params2, json_string);

			// Display the contents

			System.out.println (gauss_apc_params2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEGaussAPCParams gauss_apc_params3 = new OEGaussAPCParams();
			gauss_apc_params3.copy_from (gauss_apc_params2);

			// Display the contents

			System.out.println (gauss_apc_params3.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename
		// Construct test values, and write them to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Writing test values to a file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEGaussAPCParams gauss_apc_params = make_test_value();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_compact_json_string (gauss_apc_params);

			// Write the file

			SimpleUtils.write_string_as_file (filename, formatted_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename
		// Construct test values, and write them to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Writing test values to a file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEGaussAPCParams gauss_apc_params = make_test_value();

			// Write to file

			MarshalUtils.to_json_file (gauss_apc_params, filename);

			// Read back the file and display it

			OEGaussAPCParams gauss_apc_params2 = new OEGaussAPCParams();
			MarshalUtils.from_json_file (gauss_apc_params2, filename);

			System.out.println ();
			System.out.println (gauss_apc_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename
		// Construct typical parameters, and write them to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Writing test values to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEGaussAPCParams gauss_apc_params = make_test_value();

			// Write to file

			MarshalUtils.to_formatted_json_file (gauss_apc_params, filename);

			// Read back the file and display it

			OEGaussAPCParams gauss_apc_params2 = new OEGaussAPCParams();
			MarshalUtils.from_json_file (gauss_apc_params2, filename);

			System.out.println ();
			System.out.println (gauss_apc_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Import values from the embedded CSV file, and display the resulting list..

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Read embedded CSV file and display list");
			testargs.end_test();

			// Read embedded CSV file

			System.out.println ();
			System.out.println ("********** Read embedded CSV file **********");
			System.out.println ();

			List<OEGaussAPCParams> params_list = import_embedded_csv();

			// Display the list

			System.out.println ();
			System.out.println ("********** Display list **********");
			System.out.println ();

			for (OEGaussAPCParams params : params_list) {
				System.out.println (params.summary_string());
			}

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
