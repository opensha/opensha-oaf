# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for local earthquake catalog (Comcat simulation) functions.

n=100

# Display the catalog

runut locat$n comcat.ComcatLocalCatalog statistics "$TSDIR/locat/cat_2015_m4.txt"

# Tests using M7.8 Nepal - us20002926

runut locat$n comcat.ComcatLocalCatalog test1 "$TSDIR/locat/cat_2015_m4.txt" us20002926

runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 -0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926  0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002926 30.000  90.0 200.0 3.5

# Tests using M7.8 Nepal - gcmt20150425061126

runut locat$n comcat.ComcatLocalCatalog test1 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126

runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 -0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126  0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" gcmt20150425061126 30.000  90.0 200.0 3.5

# Tests using M7.8 Chichi-shima, Japan - us20002ki3

runut locat$n comcat.ComcatLocalCatalog test1 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3

runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001   1.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001   7.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  30.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  90.0  50.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  90.0 200.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 -0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3  0.001  90.0 200.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20002ki3 30.000  90.0 200.0 3.5

# Tests using M8.3 Illapel, Chile - us20003k7a

runut locat$n comcat.ComcatLocalCatalog test1 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a

runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001   1.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001   1.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001   7.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001   7.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  30.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  30.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 400.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 200.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 100.0 3.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 400.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 400.0 4.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 400.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 400.0 5.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 400.0 6.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 400.0 6.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a -0.001  90.0 400.0 7.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a  0.001  90.0 400.0 7.5
runut locat$n comcat.ComcatLocalCatalog test3 "$TSDIR/locat/cat_2015_m4.txt" us20003k7a 30.000  90.0 400.0 3.5

# Tests using a bad event ID - badevent

runut locat$n comcat.ComcatLocalCatalog test1 "$TSDIR/locat/cat_2015_m4.txt" badevent


