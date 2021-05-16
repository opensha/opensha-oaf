# opensha-oaf
Operational Aftershock Forecasting (OAF) code and apps

Depends on the [upstream OpenSHA](https://github.com/opensha/opensha) project which should be cloned into the same directory:

```bash
cd ~/opensha    # or whatever directory you choose
git clone https://github.com/opensha/opensha.git
git clone https://github.com/opensha/opensha-oaf.git
```

To build the Reasenberg-Jones GUI:

```bash
./gradlew appOAFJar
```

To build the ETAS GUI:

```bash
./gradlew appOAF_ETAS_Jar
```

To build and manage the OAF server, refer to the documentation in [deployment/doc](deployment/doc).
