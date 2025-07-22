# opensha-oaf
Operational Aftershock Forecasting (OAF) code and apps

Depends on the [upstream OpenSHA](https://github.com/opensha/opensha) project which should be cloned into the same directory:

```bash
cd ~/opensha    # or whatever directory you choose
git clone https://github.com/opensha/opensha
git clone https://github.com/opensha/opensha-oaf
```

Building and running the OAF software requires Java version 17 or higher.

To build the Reasenberg-Jones GUI app:

```bash
cd opensha-oaf
./gradlew appOAFJar
```

To build the ETAS GUI app:

```bash
cd opensha-oaf
./gradlew appOAF_ETAS_Jar
```

The GUI app jar file is created in opensha-oaf/build/libs.

To build and manage the OAF server, refer to the documentation in [deployment/doc](deployment/doc).
