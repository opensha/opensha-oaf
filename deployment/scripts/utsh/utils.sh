# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for utility functions.

# Tests for console redirection and time-split output streams
# (Should be first to ensure date does not turn over during execution)

n=100

delscratch

runut conred$n aafs.ServerTest conred_tsop "$SCRATCHDIR1"
runut conred$n aafs.ServerTest dump_files_in_dir "$SCRATCHDIR1"

runut conred$n aafs.ServerTest conred_tsop "$SCRATCHDIR1"
runut conred$n aafs.ServerTest dump_files_in_dir "$SCRATCHDIR1"

delscratch
mkdir "$SCRATCHDIR1"

# Tests for marshaling

n=100

runut marshal$n util.MarshalImpArray test1 100 100 100
runut marshal$n util.MarshalImpArray test1 1000 1000 1000
runut marshal$n util.MarshalImpArray test1 10000 10000 10000
runut marshal$n util.MarshalImpArray test1 100000 100000 100000

runut marshal$n util.MarshalImpJsonWriter test1 100 100 100
runut marshal$n util.MarshalImpJsonWriter test1 1000 1000 1000
runut marshal$n util.MarshalImpJsonWriter test1 10000 10000 10000
runut marshal$n util.MarshalImpJsonWriter test1 100000 100000 100000

runut marshal$n util.MarshalImpJsonWriter test2 100 100 100
runut marshal$n util.MarshalImpJsonWriter test2 1000 1000 1000
runut marshal$n util.MarshalImpJsonWriter test2 10000 10000 10000
runut marshal$n util.MarshalImpJsonWriter test2 100000 100000 100000

runut marshal$n util.MarshalImpJsonWriter test3
runut marshal$n util.MarshalImpJsonWriter test4

runut marshal$n util.MarshalImpDataWriter test1 "$SCRATCHFILE1" 100 100 100 100
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test1 "$SCRATCHFILE1" 1000 1000 1000 1000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test1 "$SCRATCHFILE1" 10000 10000 10000 10000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test1 "$SCRATCHFILE1" 100000 100000 100000 100000
rm "$SCRATCHFILE1"

runut marshal$n util.MarshalImpDataWriter test2 "$SCRATCHFILE1" 100 100 100 100
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test2 "$SCRATCHFILE1" 1000 1000 1000 1000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test2 "$SCRATCHFILE1" 10000 10000 10000 10000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test2 "$SCRATCHFILE1" 100000 100000 100000 100000
rm "$SCRATCHFILE1"

runut marshal$n util.MarshalImpDataWriter test3 "$SCRATCHFILE1" 100 100 100 100
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test3 "$SCRATCHFILE1" 1000 1000 1000 1000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test3 "$SCRATCHFILE1" 10000 10000 10000 10000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test3 "$SCRATCHFILE1" 100000 100000 100000 100000
rm "$SCRATCHFILE1"

runut marshal$n util.MarshalImpDataWriter test4 "$SCRATCHFILE1" 100 100 100 100
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test4 "$SCRATCHFILE1" 1000 1000 1000 1000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test4 "$SCRATCHFILE1" 10000 10000 10000 10000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test4 "$SCRATCHFILE1" 100000 100000 100000 100000
rm "$SCRATCHFILE1"

runut marshal$n util.MarshalImpDataWriter test5 "$SCRATCHFILE1" 100 100 100 100
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test5 "$SCRATCHFILE1" 1000 1000 1000 1000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test5 "$SCRATCHFILE1" 10000 10000 10000 10000
rm "$SCRATCHFILE1"
runut marshal$n util.MarshalImpDataWriter test5 "$SCRATCHFILE1" 100000 100000 100000 100000
rm "$SCRATCHFILE1"

# Tests for regions

n=100

runut region$n rj.OAFParameterSet test1 10000 false
runut region$n rj.OAFParameterSet test1 10000 true
runut region$n rj.OAFParameterSet test1 100000 false
runut region$n rj.OAFParameterSet test1 100000 true
runut region$n rj.OAFParameterSet test1 1000000 false
runut region$n rj.OAFParameterSet test1 1000000 true

# Tests for rupture lists

n=100

runut ruplist$n rj.CompactEqkRupList test1 1000
runut ruplist$n rj.CompactEqkRupList test1 10000
runut ruplist$n rj.CompactEqkRupList test1 100000
runut ruplist$n rj.CompactEqkRupList test1 1000000

runut ruplist$n rj.CompactEqkRupList test2 50
runut ruplist$n rj.CompactEqkRupList test2 500
runut ruplist$n rj.CompactEqkRupList test2 5000
runut ruplist$n rj.CompactEqkRupList test2 20000


