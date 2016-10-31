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

for SUITE in cl lb sf bd z3; do
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

function xmap() {
    filemap -r -f X=cl "$EXP/cl.__Y__.__I__.log" "solve semi.*=__T__"
    filemap -r -f X=bd "$EXP/bd.__Y__.__I__.log" "SOLVE_TIME=__T__" | filereduce --scale T:0.001
    filemap -r -f X=lb "$EXP/lb.__Y__.__I__.log" ".*system 0:__T__elapsed"
    filemap -r -f X=sf "$EXP/sf.__Y__.__I__.log" "@runtime;__T__"
    filemap -r -f X=z3 "$EXP/z3.__Y__.__I__.log" ".*other: __T__ms" | filereduce --scale T:0.001
}

xmap | filereduce --remove I --collect T:avg --table X:Y:T:benchmark::-

