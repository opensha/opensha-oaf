# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for configuration file reading/parsing/formatting.

# Tests for action configuration

runut action01 aafs.ActionConfigFile test1
runut action02 aafs.ActionConfigFile test2
runut action03 aafs.ActionConfigFile test3

runut action04 aafs.ActionConfig test1

# Tests for magnitude of completeness configuration

runut magcomp01 rj.MagCompPage_ParametersFetch test1
runut magcomp02 rj.MagCompPage_ParametersFetch test2
runut magcomp03 rj.MagCompPage_ParametersFetch test3
runut magcomp04 rj.MagCompPage_ParametersFetch test4
runut magcomp05 rj.MagCompPage_ParametersFetch test5

# Tests for Reasenberg-Jones configuration

runut rj01 rj.GenericRJ_ParametersFetch test1
runut rj02 rj.GenericRJ_ParametersFetch test2
runut rj03 rj.GenericRJ_ParametersFetch test3
runut rj04 rj.GenericRJ_ParametersFetch test4

# Tests for server configuration

runut server01 aafs.ServerConfigFile test1
runut server02 aafs.ServerConfigFile test2
runut server03 aafs.ServerConfigFile test3

runut server04 aafs.ServerConfig test1
runut server05 aafs.ServerConfig test2

# Tests for operational ETAS configuration

runut oeconf01 oetas.env.OEtasConfigFile test1
runut oeconf02 oetas.env.OEtasConfigFile test2
runut oeconf03 oetas.env.OEtasConfigFile test3
runut oeconf04 oetas.env.OEtasConfigFile test4
runut oeconf05 oetas.env.OEtasConfigFile test5

runut oeconf06 oetas.env.OEtasConfig test1
runut oeconf07 oetas.env.OEtasConfig test2

# Tests for operational ETAS Gaussian a/p/c prior configuration

runut oegapc01 oetas.bay.OEGaussAPCConfigFile test1
runut oegapc02 oetas.bay.OEGaussAPCConfigFile test2
runut oegapc03 oetas.bay.OEGaussAPCConfigFile test3
runut oegapc04 oetas.bay.OEGaussAPCConfigFile test4
runut oegapc05 oetas.bay.OEGaussAPCConfigFile test5

runut oegapc06 oetas.bay.OEGaussAPCConfig test1
runut oegapc07 oetas.bay.OEGaussAPCConfig test2


