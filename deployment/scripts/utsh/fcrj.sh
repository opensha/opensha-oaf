# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for Reasenberg and Jones forecasts.

n=100

# Tests using M7.8 Nepal - us20002926

runut fcrj$n aafs.ForecastResults test1 us20002926
runut fcrj$n aafs.ForecastResults test2 us20002926

runut fcrj$n aafs.ForecastResults test3 us20002926 0.014
runut fcrj$n aafs.ForecastResults test4 us20002926 0.014
runut fcrj$n aafs.ForecastResults test3 us20002926 3.0
runut fcrj$n aafs.ForecastResults test4 us20002926 3.0
runut fcrj$n aafs.ForecastResults test3 us20002926 30.0
runut fcrj$n aafs.ForecastResults test4 us20002926 30.0
runut fcrj$n aafs.ForecastResults test3 us20002926 300.0
runut fcrj$n aafs.ForecastResults test4 us20002926 300.0

# Tests using M7.8 Nepal - gcmt20150425061126

runut fcrj$n aafs.ForecastResults test1 gcmt20150425061126
runut fcrj$n aafs.ForecastResults test2 gcmt20150425061126

# Tests using M6.0 South Napa - nc72282711

runut fcrj$n aafs.ForecastResults test1 nc72282711
runut fcrj$n aafs.ForecastResults test2 nc72282711

runut fcrj$n aafs.ForecastResults test3 nc72282711 0.014
runut fcrj$n aafs.ForecastResults test4 nc72282711 0.014
runut fcrj$n aafs.ForecastResults test3 nc72282711 3.0
runut fcrj$n aafs.ForecastResults test4 nc72282711 3.0
runut fcrj$n aafs.ForecastResults test3 nc72282711 30.0
runut fcrj$n aafs.ForecastResults test4 nc72282711 30.0
runut fcrj$n aafs.ForecastResults test3 nc72282711 300.0
runut fcrj$n aafs.ForecastResults test4 nc72282711 300.0

# Tests using M7.5 Indonesia - us1000h3p4

runut fcrj$n aafs.ForecastResults test1 us1000h3p4
runut fcrj$n aafs.ForecastResults test2 us1000h3p4

runut fcrj$n aafs.ForecastResults test3 us1000h3p4 0.014
runut fcrj$n aafs.ForecastResults test4 us1000h3p4 0.014
runut fcrj$n aafs.ForecastResults test3 us1000h3p4 3.0
runut fcrj$n aafs.ForecastResults test4 us1000h3p4 3.0
runut fcrj$n aafs.ForecastResults test3 us1000h3p4 30.0
runut fcrj$n aafs.ForecastResults test4 us1000h3p4 30.0
runut fcrj$n aafs.ForecastResults test3 us1000h3p4 300.0
runut fcrj$n aafs.ForecastResults test4 us1000h3p4 300.0

# Tests using a few other large earthquakes

runut fcrj$n aafs.ForecastResults test1 usc000rki5
runut fcrj$n aafs.ForecastResults test2 usc000rki5

runut fcrj$n aafs.ForecastResults test1 official20100227063411530_30
runut fcrj$n aafs.ForecastResults test2 official20100227063411530_30

runut fcrj$n aafs.ForecastResults test1 us20003k7a
runut fcrj$n aafs.ForecastResults test2 us20003k7a

runut fcrj$n aafs.ForecastResults test1 usp000eg5g
runut fcrj$n aafs.ForecastResults test2 usp000eg5g

runut fcrj$n aafs.ForecastResults test1 usc000nzvd
runut fcrj$n aafs.ForecastResults test2 usc000nzvd

runut fcrj$n aafs.ForecastResults test1 nc216859
runut fcrj$n aafs.ForecastResults test2 nc216859

runut fcrj$n aafs.ForecastResults test1 nc10089897
runut fcrj$n aafs.ForecastResults test2 nc10089897

runut fcrj$n aafs.ForecastResults test1 usp000hvnu
runut fcrj$n aafs.ForecastResults test2 usp000hvnu

runut fcrj$n aafs.ForecastResults test1 usp000hzf6
runut fcrj$n aafs.ForecastResults test2 usp000hzf6


