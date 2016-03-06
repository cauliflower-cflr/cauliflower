#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                              exp_report.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-06                                                       #
#                                                                         #
# collates several experimental runs into a report                        #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Return the relative path of the argument to the current directory
#
function relative(){
    python -c "import os.path; print os.path.relpath('$1', '.')"
}

#
# like paste, but vertically (only works with stdin)
#
function vpaste(){
    COUNT=0
    for i in $PASTE_ARGS; do
        COUNT=$(($COUNT + 1))
    done
    TMPS=""
    while read LINE; do
        TMP=`mktemp`
        echo "$LINE" > $TMP
        for L in `seq 2 $COUNT`; do
            read LIN
            echo "$LIN" >> $TMP
        done
        TMPS="$TMPS $TMP"
    done
    paste $TMPS
    rm $TMPS
}

#
# convert each line of values into the numeric average of those values (as a float)
#
function avg(){
    sed -e 's/\t/,/g' -e 's/^/a=['/ -e 's/$/];print sum(a)\/float(len(a))/' | python
}

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh

PASTE_ARGS=`sed -e 's/  */ /g' -e 's/[^ ]*/-/g' <<< $SPEC_BENCHES`

#echo `find $EXPER_DIR -name "*.exp.cauli"`
#echo `find $EXPER_DIR -name "*.exp.giga"`
cat `find $EXPER_DIR -name "*.exp.times"` | grep "solve semi-naive" | sed 's/.*: //' | vpaste $PASTE_ARGS | avg
cat `find $EXPER_DIR -name "*.exp.count"` | awk '{print $1}' | vpaste $PASTE_ARGS | cut -f 1
