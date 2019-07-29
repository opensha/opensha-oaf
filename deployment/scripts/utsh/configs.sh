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

# Tests for action configuration

runut server01 aafs.ServerConfigFile test1
runut server02 aafs.ServerConfigFile test2
runut server03 aafs.ServerConfigFile test3

runut server04 aafs.ServerConfig test1
runut server05 aafs.ServerConfig test2


