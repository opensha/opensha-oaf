#! /bin/bash

# Unit test operations.
# $1 = Unit name.
# $2 = Operation code (see below).

# Operations:
#
# test - Run tests, and compare to existing reference results.
#
# save - Save reference test results from the current test results.
#
# cleancur - Erase current test results.
#
# cleanref - Erase reference test results.
#
# cur - Show all current test results (using less).
#
# ref - Show all reference test results (using less).
#
# diff - Show differences between current and reference test results (using git diff).



# Unit name and script file.

TUNIT="$1"
TSCRIPT="opensha-oaf/deployment/scripts/utsh/$TUNIT.sh"
TSDIR="opensha-oaf/deployment/scripts/utsh"

# Directory names: Reference, Parent of current, and Current.

REFDIR="opensha-oaf/deployment/unittest/$TUNIT"
CURPARENT="opensha-oaf/build/libs/unittest"
CURDIR="opensha-oaf/build/libs/unittest/$TUNIT"

# Temporary files available for use (internally within this script).

TMPFILE1="opensha-oaf/build/libs/unittest/runut1.tmp"
TMPFILE2="opensha-oaf/build/libs/unittest/runut2.tmp"
TMPFILE3="opensha-oaf/build/libs/unittest/runut3.tmp"

# Scratch files and directory that are available for use by test scripts.

SCRATCHFILE1="opensha-oaf/build/libs/unittest/scratchfile1.tmp"
SCRATCHFILE2="opensha-oaf/build/libs/unittest/scratchfile2.tmp"
SCRATCHFILE3="opensha-oaf/build/libs/unittest/scratchfile3.tmp"

SCRATCHDIR1="opensha-oaf/build/libs/unittest/scratchdir1.tmp"

# Colors we use (bold).

NOCOL='\033[0m'
RED='\033[1;31m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'

# Connection option

CONOPT="-1"

# This counter is incremented on each test.

n=1

# Time constants.

YEAR_2018=1514764800000
YEAR_2015=1420070400000

SECOND_MILLIS=1000
MINUTE_MILLIS=60000
HOUR_MILLIS=3600000
DAY_MILLIS=86400000

# This counter starts at 01/01/2015 and is incremented by one hour on each test.

let "testtime = YEAR_2015"

# Counters for test results.

let "count_total = 0"
let "count_match = 0"
let "count_error = 0"
let "count_unchecked = 0"




# Function to delete the temporary files.

deltmp () {

    # Delete the temporary files if they exist

    if [ -f "$TMPFILE1" ]; then
        rm "$TMPFILE1"
    fi

    if [ -f "$TMPFILE2" ]; then
        rm "$TMPFILE2"
    fi

    if [ -f "$TMPFILE3" ]; then
        rm "$TMPFILE3"
    fi

}




# Function to delete the scratch files.

delscratch () {

    # Delete the scratch files and directory if they exist

    if [ -f "$SCRATCHFILE1" ]; then
        rm "$SCRATCHFILE1"
    fi

    if [ -f "$SCRATCHFILE2" ]; then
        rm "$SCRATCHFILE2"
    fi

    if [ -f "$SCRATCHFILE3" ]; then
        rm "$SCRATCHFILE3"
    fi

    if [ -d "$SCRATCHDIR1" ]; then
        rm -r "$SCRATCHDIR1"
    fi

}




# Function to run one unit test, without database access.
# $1 = Test name.
# $2 = Java class name (without the leading "org.opensha.oaf.").
# $3 and up = Parameters passed to Java class.

runut () {

    # Increment counter

    let "n += 1"
    let "testtime += HOUR_MILLIS"
    let "count_total += 1"

    # File names: Reference and Current

    local REFFILE="$REFDIR/$1.txt"
    local CURFILE="$CURDIR/$1.txt"

    # Java class

    local JCLASS="org.opensha.oaf.$2"

    # Create current directories if they don't exist

    if [ ! -d "$CURPARENT" ]; then
        mkdir "$CURPARENT"
    fi

    if [ ! -d "$CURDIR" ]; then
        mkdir "$CURDIR"
    fi

    # Delete the current file if it already exists

    if [ -f "$CURFILE" ]; then
        rm "$CURFILE"
    fi

    # Delete the temporary files if they exist

    deltmp

    # Display the command parameters

    echo "$@"
    echo "$@" > "$TMPFILE1"

    # Run the test

    shift 2
    java -Dtesttime=$testtime -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@" > "$TMPFILE2" 2>&1

    cat "$TMPFILE1" "$TMPFILE2" > "$CURFILE"

    # Delete the temporary files if they exist

    deltmp

    # Compare if the reference file exists

    if [ -f "$REFFILE" ]; then
        if cmp -s "$CURFILE" "$REFFILE"; then
            let "count_match += 1"
            # echo ">>> MATCHED <<<            less \"$REFFILE\""
            echo -e "${GREEN}>>> MATCHED <<<${NOCOL}            less \"$REFFILE\""
        else
            let "count_error += 1"
            # echo "!!! ERROR - MISMATCH !!!   git diff -U9999 \"$REFFILE\" \"$CURFILE\""
            echo -e "${RED}!!! ERROR - MISMATCH !!!${NOCOL}   git diff -U9999 \"$REFFILE\" \"$CURFILE\""
            # exit 1    # Do this to make testing stop as soon as there is a mismatch detected
        fi
    else
        let "count_unchecked += 1"
        # echo "??? NOT CHECKED ???        less \"$CURFILE\""
        echo -e "${YELLOW}??? NOT CHECKED ???${NOCOL}        less \"$CURFILE\""
    fi

}




# Function to run one unit test, with database access.
# $1 = Test name.
# $2 = Java class name (without the leading "org.opensha.oaf.").
# $3 and up = Parameters passed to Java class.
#
# This function must remove output lines that are generated by MongoDB.
# As of MongoDB 4.0.X, these can be identifed as lines that contain the string "mongodb.diagnostics"
# or that begin with the string "INFO: ".

rundbut () {

    # Increment counter

    let "n += 1"
    let "testtime += HOUR_MILLIS"
    let "count_total += 1"

    # File names: Reference and Current

    local REFFILE="$REFDIR/$1.txt"
    local CURFILE="$CURDIR/$1.txt"

    # Java class

    local JCLASS="org.opensha.oaf.$2"

    # Create current directories if they don't exist

    if [ ! -d "$CURPARENT" ]; then
        mkdir "$CURPARENT"
    fi

    if [ ! -d "$CURDIR" ]; then
        mkdir "$CURDIR"
    fi

    # Delete the current file if it already exists

    if [ -f "$CURFILE" ]; then
        rm "$CURFILE"
    fi

    # Delete the temporary files if they exist

    deltmp

    # Display the command parameters

    echo "$@"
    echo "$@" > "$TMPFILE1"

    # Run the test

    shift 2
    java -Dtesttime=$testtime -Dtestconopt=$CONOPT -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@" > "$TMPFILE2" 2>&1

    # Remove the lines that are generated by MongoDB

    cat "$TMPFILE2" | sed '/mongodb\.diagnostics/d' | sed '/^INFO: /d' | cat "$TMPFILE1" - > "$CURFILE"

    # Delete the temporary files if they exist

    deltmp

    # Compare if the reference file exists

    if [ -f "$REFFILE" ]; then
        if cmp -s "$CURFILE" "$REFFILE"; then
            let "count_match += 1"
            # echo ">>> MATCHED <<<            less \"$REFFILE\""
            echo -e "${GREEN}>>> MATCHED <<<${NOCOL}            less \"$REFFILE\""
        else
            let "count_error += 1"
            # echo "!!! ERROR - MISMATCH !!!   git diff -U9999 \"$REFFILE\" \"$CURFILE\""
            echo -e "${RED}!!! ERROR - MISMATCH !!!${NOCOL}   git diff -U9999 \"$REFFILE\" \"$CURFILE\""
            # exit 1    # Do this to make testing stop as soon as there is a mismatch detected
        fi
    else
        let "count_unchecked += 1"
        # echo "??? NOT CHECKED ???        less \"$CURFILE\""
        echo -e "${YELLOW}??? NOT CHECKED ???${NOCOL}        less \"$CURFILE\""
    fi

}




# Perform operation

case "$2" in


    # Run tests

    test)

        # Remove current test results directory if it exists, then create an empty one

        if [ ! -d "$CURPARENT" ]; then
            mkdir "$CURPARENT"
        fi

        if [ -d "$CURDIR" ]; then
            rm -r "$CURDIR"
        fi
        mkdir "$CURDIR"

        # Delete scratch files and directory, then make the scratch directory

        delscratch
        mkdir "$SCRATCHDIR1"

        # Run the tests

        source "$TSCRIPT"

        # Display results

        echo "Total = $count_total, matched = $count_match, error = $count_error, unchecked = $count_unchecked"

        # Delete scratch files and directory

        delscratch

        ;;


    # Run tests, with $3 = Connection option

    testcon)

        # Connection option

        CONOPT="$3"

        # Remove current test results directory if it exists, then create an empty one

        if [ ! -d "$CURPARENT" ]; then
            mkdir "$CURPARENT"
        fi

        if [ -d "$CURDIR" ]; then
            rm -r "$CURDIR"
        fi
        mkdir "$CURDIR"

        # Delete scratch files and directory, then make the scratch directory

        delscratch
        mkdir "$SCRATCHDIR1"

        # Run the tests

        source "$TSCRIPT"

        # Display results

        echo "Total = $count_total, matched = $count_match, error = $count_error, unchecked = $count_unchecked"

        # Delete scratch files and directory

        delscratch

        ;;


    # Save reference results

    save)

        # Remove reference test results directory if it exists, then copy the current test results

        if [ -d "$REFDIR" ]; then
            rm -r "$REFDIR"
        fi
        cp -pir "$CURDIR" "$REFDIR"

        ;;


    # Erase current results

    cleancur)

        # Remove current test results directory if it exists

        if [ -d "$CURDIR" ]; then
            rm -r "$CURDIR"
        fi

        ;;


    # Erase reference results

    cleanref)

        # Remove reference test results directory if it exists

        if [ -d "$REFDIR" ]; then
            rm -r "$REFDIR"
        fi

        ;;


    # Show current test results

    cur)
        less "$CURDIR"/*
        ;;


    # Show reference test results

    ref)
        less "$REFDIR"/*
        ;;


    # Show differences between current test results and reference test results

    diff)
        git diff -U9999 "$REFDIR" "$CURDIR"
        ;;


    # Unknown command

    *)
        echo "Usage: utoaf.sh unit {test|save|cleancur|cleanref|cur|ref|diff}"
        exit 1
        ;;
esac


exit 0

