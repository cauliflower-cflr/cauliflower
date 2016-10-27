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
        fi
        "$CAUL" -n solver -o "$EXE" -c -r "$SPEC"
    fi
    for DIR in "$@"; do
        for EG_LOG in `find $DIR -name "log.txt"`; do
            EG="$(dirname "$EG_LOG")"
            NM="$(basename "$EG")"
            echo
            echo " = $EXE - $NM = "
            echo
            [ -f "$EXE/$NM" ] || timeout 600 "$EXE/solver" "$EG" 2>&1 | tee "$EXE/$NM" || echo "solve semi-naive=TIMEOUT" > "$EXE/$NM"
            [ -s "$EXE/$NM" ] || echo "solve semi-naive=TIMEOUT" > "$EXE/$NM"
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

ROOT="$(dirname $0)/../.."
CAUL="$ROOT/build/install/cauliflower/bin/cauliflower"
EXMP="$ROOT/src/test/examples/"

# build the executables
"$ROOT"/gradlew  -p "$ROOT" installDist

run VIRT "$EXMP/dispatch/virtual_slow.cflr"      ""   "$@"
run VOP1 "$EXMP/dispatch/virtual_slow.cflr"      "1"  "$@"
run VOP9 "$EXMP/dispatch/virtual_slow.cflr"      "9"  "$@"
run CONS "$EXMP/dispatch/conservative_slow.cflr" ""   "$@"
run COP1 "$EXMP/dispatch/conservative_slow.cflr" "1"  "$@"
run COP9 "$EXMP/dispatch/conservative_slow.cflr" "9"  "$@"

