#! /usr/bin/env bash

#=========================================================================#
#                              experiment.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-29                                                       #
#=========================================================================#

set -e
set -u

DIR="$(dirname "$0")"
EXP="$DIR/EXPERIMENT"

mkdir -p "$EXP"

for SUITE in lb sf bd z3; do
    for CASE in "$@"; do
        for AL in `find "$CASE" -name "Alloc.csv" | sort`; do
            CDIR="$(dirname "$AL")"
            CNAM="$(basename "$CDIR")"
            for RUN in `seq 1 3`; do
                FIL="$EXP/$SUITE.$CNAM.$RUN.log"
                if [ -s "$FIL" ]; then
                    echo "skip $FIL"
                else
                    echo "== RUN $FIL =="
                    "$DIR/$SUITE.sh" "$CDIR" 600 > $FIL 2>&1
                fi
            done
        done
    done
done
