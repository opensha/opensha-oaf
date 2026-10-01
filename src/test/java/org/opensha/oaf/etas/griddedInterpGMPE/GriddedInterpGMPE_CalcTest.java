package org.opensha.oaf.etas.griddedInterpGMPE;

import static org.junit.Assert.assertNull;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.sha.imr.AttenRelRef;
import org.opensha.sha.imr.ScalarIMR;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGA_Param;
import org.opensha.sha.util.FocalMech;

public class GriddedInterpGMPE_CalcTest {

	@Test
	public void testPrecalcWithCorrectedDippingRupturesAndMultipleDistances() {
		ScalarIMR gmpe = AttenRelRef.BSSA_2014.get();
		gmpe.setParamDefaults();
		gmpe.setIntensityMeasure(PGA_Param.NAME);

		DiscretizedFunc xVals = new ArbitrarilyDiscretizedFunc();
		xVals.set(0.01, 1d);
		xVals.set(0.1, 1d);

		DistanceInterpolator distInterp = new DistanceInterpolator(false, 1d, 10d, 2);
		GriddedInterpGMPE_Calc calc = new GriddedInterpGMPE_Calc(
				gmpe, xVals, 1d, 6d, 6.2d, 3, distInterp);

		Map<FocalMech, Double> mechWts = new EnumMap<>(FocalMech.class);
		mechWts.put(FocalMech.REVERSE, 1d);

		calc.precalc(1d, new double[] { 7d, 2d }, mechWts);

		assertNull("Precalculation should not leave a rupture installed in the GMPE",
				gmpe.getEqkRupture());
	}
}
