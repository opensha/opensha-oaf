# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for Comcat functions.

n=100
delaytime=4

# Tests using M7.8 Nepal - us20002926

runut comcat$n comcat.ComcatOAFAccessor test1 us20002926
runut comcat$n comcat.ComcatOAFAccessor test3 us20002926
runut comcat$n comcat.ComcatOAFAccessor test7 true true us20002926
runut comcat$n comcat.ComcatOAFAccessor test7 true false us20002926

runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 30.000 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926 -0.001  30.0 200.0 3.5 100
runut comcat$n comcat.ComcatOAFAccessor test10 us20002926  0.001  30.0 200.0 3.5 100

runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 30.000 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926 -0.001  30.0 200.0 3.5 100
runut comcat$n comcat.ComcatOAFAccessor test11 us20002926  0.001  30.0 200.0 3.5 100

# Tests using M7.8 Nepal - gcmt20150425061126

runut comcat$n comcat.ComcatOAFAccessor test1 gcmt20150425061126
runut comcat$n comcat.ComcatOAFAccessor test3 gcmt20150425061126
runut comcat$n comcat.ComcatOAFAccessor test7 true true gcmt20150425061126
runut comcat$n comcat.ComcatOAFAccessor test7 true false gcmt20150425061126

runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 30.000 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126 -0.001  30.0 200.0 3.5 100
runut comcat$n comcat.ComcatOAFAccessor test10 gcmt20150425061126  0.001  30.0 200.0 3.5 100

runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001   1.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001   7.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001  30.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001 365.0  50.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001 365.0 200.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001 365.0 200.0 5.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 30.000 365.0 200.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126 -0.001  30.0 200.0 3.5 100
runut comcat$n comcat.ComcatOAFAccessor test11 gcmt20150425061126  0.001  30.0 200.0 3.5 100

# Tests using M6.0 South Napa - nc72282711

runut comcat$n comcat.ComcatOAFAccessor test1 nc72282711
runut comcat$n comcat.ComcatOAFAccessor test3 nc72282711
runut comcat$n comcat.ComcatOAFAccessor test7 true true nc72282711
runut comcat$n comcat.ComcatOAFAccessor test7 true false nc72282711

runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001   1.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001   1.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001   7.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001   7.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001  30.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001  30.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001 365.0  50.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001 365.0  50.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001 365.0  25.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001 365.0  25.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 -0.001 365.0 100.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.001 365.0 100.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711 30.000 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test10 nc72282711  0.900   1.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001   1.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001   1.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001   7.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001   7.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001  30.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001  30.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001 365.0  50.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001 365.0  50.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001 365.0  25.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001 365.0  25.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 -0.001 365.0 100.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.001 365.0 100.0 4.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711 30.000 365.0 100.0 2.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc72282711  0.900   1.0 100.0 3.5 0

# Tests using M7.5 Indonesia - us1000h3p4

runut comcat$n comcat.ComcatOAFAccessor test1 us1000h3p4
runut comcat$n comcat.ComcatOAFAccessor test3 us1000h3p4
runut comcat$n comcat.ComcatOAFAccessor test7 true true us1000h3p4
runut comcat$n comcat.ComcatOAFAccessor test7 true false us1000h3p4

runut comcat$n comcat.ComcatOAFAccessor test1 us1000h4p4
runut comcat$n comcat.ComcatOAFAccessor test3 us1000h4p4
runut comcat$n comcat.ComcatOAFAccessor test7 true true us1000h4p4
runut comcat$n comcat.ComcatOAFAccessor test7 true false us1000h4p4

# Tests using a bad event ID - badevent

runut comcat$n comcat.ComcatOAFAccessor test1 badevent
runut comcat$n comcat.ComcatOAFAccessor test3 badevent
runut comcat$n comcat.ComcatOAFAccessor test7 true true badevent
runut comcat$n comcat.ComcatOAFAccessor test7 true false badevent

# Tests using a few other large earthquakes

runut comcat$n comcat.ComcatOAFAccessor test1 usc000rki5
runut comcat$n comcat.ComcatOAFAccessor test3 usc000rki5
runut comcat$n comcat.ComcatOAFAccessor test7 true true usc000rki5
runut comcat$n comcat.ComcatOAFAccessor test7 true false usc000rki5
runut comcat$n comcat.ComcatOAFAccessor test10 usc000rki5 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 usc000rki5 -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 official20100227063411530_30
runut comcat$n comcat.ComcatOAFAccessor test3 official20100227063411530_30
runut comcat$n comcat.ComcatOAFAccessor test7 true true official20100227063411530_30
runut comcat$n comcat.ComcatOAFAccessor test7 true false official20100227063411530_30
runut comcat$n comcat.ComcatOAFAccessor test10 official20100227063411530_30 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 official20100227063411530_30 -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 us20003k7a
runut comcat$n comcat.ComcatOAFAccessor test3 us20003k7a
runut comcat$n comcat.ComcatOAFAccessor test7 true true us20003k7a
runut comcat$n comcat.ComcatOAFAccessor test7 true false us20003k7a
runut comcat$n comcat.ComcatOAFAccessor test10 us20003k7a -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 us20003k7a -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 usp000eg5g
runut comcat$n comcat.ComcatOAFAccessor test3 usp000eg5g
runut comcat$n comcat.ComcatOAFAccessor test7 true true usp000eg5g
runut comcat$n comcat.ComcatOAFAccessor test7 true false usp000eg5g
runut comcat$n comcat.ComcatOAFAccessor test10 usp000eg5g -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 usp000eg5g -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 usc000nzvd
runut comcat$n comcat.ComcatOAFAccessor test3 usc000nzvd
runut comcat$n comcat.ComcatOAFAccessor test7 true true usc000nzvd
runut comcat$n comcat.ComcatOAFAccessor test7 true false usc000nzvd
runut comcat$n comcat.ComcatOAFAccessor test10 usc000nzvd -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 usc000nzvd -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 nc216859
runut comcat$n comcat.ComcatOAFAccessor test3 nc216859
runut comcat$n comcat.ComcatOAFAccessor test7 true true nc216859
runut comcat$n comcat.ComcatOAFAccessor test7 true false nc216859
runut comcat$n comcat.ComcatOAFAccessor test10 nc216859 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc216859 -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 nc10089897
runut comcat$n comcat.ComcatOAFAccessor test3 nc10089897
runut comcat$n comcat.ComcatOAFAccessor test7 true true nc10089897
runut comcat$n comcat.ComcatOAFAccessor test7 true false nc10089897
runut comcat$n comcat.ComcatOAFAccessor test10 nc10089897 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 nc10089897 -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 usp000hvnu
runut comcat$n comcat.ComcatOAFAccessor test3 usp000hvnu
runut comcat$n comcat.ComcatOAFAccessor test7 true true usp000hvnu
runut comcat$n comcat.ComcatOAFAccessor test7 true false usp000hvnu
runut comcat$n comcat.ComcatOAFAccessor test10 usp000hvnu -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 usp000hvnu -0.001 365.0 100.0 3.5 0

runut comcat$n comcat.ComcatOAFAccessor test1 usp000hzf6
runut comcat$n comcat.ComcatOAFAccessor test3 usp000hzf6
runut comcat$n comcat.ComcatOAFAccessor test7 true true usp000hzf6
runut comcat$n comcat.ComcatOAFAccessor test7 true false usp000hzf6
runut comcat$n comcat.ComcatOAFAccessor test10 usp000hzf6 -0.001 365.0 100.0 3.5 0
runut comcat$n comcat.ComcatOAFAccessor test11 usp000hzf6 -0.001 365.0 100.0 3.5 0


