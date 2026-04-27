# Operational Aftershock Forecasting (OAF) Server Code and Desktop Applications.

The official USGS software releases are located at [https://code.usgs.gov/esc/oaf/opensha-oaf](https://code.usgs.gov/esc/oaf/opensha-oaf).
This GitHub repository is the development version.

## Downloading

The aftershock forecasting software depends on the [upstream OpenSHA](https://github.com/opensha/opensha) project which should be cloned into the same directory:

```bash
cd ~/opensha    # or whatever directory you choose
git clone https://github.com/opensha/opensha
git clone https://github.com/opensha/opensha-oaf
```

Building and running the OAF software requires Java version 17 or higher.

## OAF Server and Analyst Utilities

Developed by:
- Michael Barall, USGS Earthquake Science Center, Moffett Field, CA.
- Nicholas van der Elst, USGS Earthquake Science Center, Pasadena, CA.

The OAF server runs continuously in the cloud, monitoring the USGS ComCat earthquake catalog.
It automatically generates aftershock forecasts and publishes them on the earthquake event pages.

To build and manage the OAF server, refer to the documentation in [deployment/doc](deployment/doc).

The analyst utilities are a GUI program that is used by analysts to evaluate the operation of the OAF servers,
and make adjustments to the statistical models used by the servers to compute forecasts.

To build analyst utilities:

```bash
cd opensha-oaf
./gradlew appETAS_GUIJar
```

The analyst utility jar file is created in opensha-oaf/build/libs.

## Aftershock Forecaster

Developed by:
- Nicholas van der Elst, USGS Earthquake Science Center, Pasadena, CA.
- Michael Barall, USGS Earthquake Science Center, Moffett Field, CA.
- Kevin Milner, USGS Geologic Hazards Science Center, Pasadena, CA.

Aftershock Forecaster is a GUI program that runs on the desktop and is used to compute ETAS-based aftershock forecasts.

To build Aftershock Forecaster:

```bash
cd opensha-oaf
./gradlew appOAF_ETAS_Jar
```

The Aftershock Forecaster jar file is created in opensha-oaf/build/libs.


## Citations

For the OAF server and analyst utilities:

Barall, M., and van der Elst, N. (2025), Operational Aftershock Forecasting,
USGS Software Release, [https://doi.org/10.5066/P1FJSYVJ](https://doi.org/10.5066/P1FJSYVJ).

For Aftershock Forecaster:

van der Elst, N., Barall, M., and Milner, K. (2025), Aftershock Forecaster,
USGS Software Release, [https://doi.org/10.5066/P1LG6ZQS](https://doi.org/10.5066/P1LG6ZQS).

