#! /usr/bin/env bash

#=========================================================================#
#                            do_experiment.sh                             #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-26                                                       #
#=========================================================================#

set -e
set -u

function find_cases() {
    for DIR in "$@"; do
        for EG_LOG in `find $DIR -name "log.txt"`; do
            dirname "$EG_LOG"
        done
    done
}

function run() {
    EXE="$1"
    SPEC="$2"
    OPT="$3"
    shift 3
    mkdir -p $EXE
    if [ ! -f "$EXE/solver" ]; then
        if [ ! -z "$OPT" ]; then
            "$CAUL" -n optimised -o "$EXE" -O "$OPT" "$SPEC" $(choose_benches "$@")
            SPEC="$EXE/optimised.cflr"
            mv ./cauliflower.log "$EXE/opti.log"
        fi
        "$CAUL" -n solver -o "$EXE" -c -r "$SPEC"
        mv ./cauliflower.log "$EXE/compl.log"
    fi
    for DIR in "$@"; do
        for EG_LOG in `find $DIR -name "log.txt"`; do
            for RUN in `seq 1 3`; do
                EG="$(dirname "$EG_LOG")"
                NM="$(basename "$EG").$RUN"
                echo
                echo " = $EXE - $NM = "
                echo
                [ -f "$EXE/$NM" ] || timeout 600 "$EXE/solver" "$EG" 2>&1 | tee "$EXE/$NM" || echo "solve semi-naive=TIMEOUT" > "$EXE/$NM"
                [ -s "$EXE/$NM" ] || echo "solve semi-naive=TIMEOUT" > "$EXE/$NM"
            done
        done
    done
}

function choose_benches() {
    for EXP in bach-luindex 2006-antlr; do
        for EG in `find_cases "$@"`; do
            [ `basename "$EG"` == $EXP ] && echo $EG
        done
    done
}

function detail() {
    mkdir -p "META"
    for DIR in "$@"; do
        for EG_LOG in `find $DIR -name "log.txt"`; do
            EG="$(dirname "$EG_LOG")"
            NM="$(basename "$EG")"
            if [ ! -f "META/$NM" ]; then
                echo $NM
                wc -l "$EG"/*.csv | tee "META/$NM"
                sed -e 's/^\([^,]*\),\([^,]*\)$/\1\n\2/' -e 's/^\([^,]*\),\([^,]*\),.*$/\1\n\2/' "$EG"/*.csv | sort -u | wc >> "META/$NM"
            fi
        done
    done
}

ROOT="$(dirname $0)/../.."
CAUL="$ROOT/build/install/cauliflower/bin/cauliflower"
EXMP="$ROOT/src/test/examples/"

detail "$@"

# build the executables
"$ROOT"/gradlew  -p "$ROOT" installDist

run VIRT "$EXMP/dispatch/virtual_slow.cflr"                ""  "$@"
run VOP1 "$EXMP/dispatch/virtual_slow.cflr"                "1" "$@"
run VOP9 "$EXMP/dispatch/virtual_slow.cflr"                "9" "$@"
run CONS "$EXMP/dispatch/conservative_slow.cflr"           ""  "$@"
run COP1 "$EXMP/dispatch/conservative_slow.cflr"           "1" "$@"
run COP9 "$EXMP/dispatch/conservative_slow.cflr"           "9" "$@"
run VRED "$ROOT/example/experiment_eachopt/REDUNDANT.cflr" ""  "$@"
run VFIL "$ROOT/example/experiment_eachopt/FILTER.cflr" ""  "$@"
run VORD "$ROOT/example/experiment_eachopt/ORDER.cflr"     ""  "$@"


function allmap() {
    filemap -r -f X=V ./META/__Y__ " *__T__  *[0-9][0-9]* [0-9][0-9]*$"
    filemap -r -f X=E ./META/__Y__ ' *__T__  *total'
    filemap -r ./__X__/__Y__.____ "solve semi.*=__T__" | filereduce --collect T:avg
}

allmap | filereduce --table X:Y:T:benchmark:,: > RESULT.csv
cat RESULT.csv | column -s "," -t

