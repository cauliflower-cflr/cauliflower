#! /usr/bin/env bash

#=========================================================================#
#                                 run.sh                                  #
#                                                                         #
# Author: nic                                                             #
# Date: 2017-Jul-04                                                       #
#                                                                         #
# run the experiments                                                     #
#                                                                         #
# Options:                                                                #
#   -h           Display this help message                                #
#=========================================================================#

set -e # error on non-zero exit
set -u # undefined variables are an error

CAULI="../../build/install/cauliflower/bin/cauliflower"
VDSCR="../optimise_vdispatch/vdispatch.dl"
VDSP="./D_exes/virtual_dispatch_csv_converter"

function usage(){
    grep "^#.*#$" $0
}

function errxit(){
    [ $# -gt 0 ] && echo "Error: $@" >&2
    echo "Re-run with -h for help" >&2
    exit 1
}

function stage(){
    echo -n "--------------"
    echo -n $@
    echo "--------------"
}

function thread_count(){
    RET=$(lscpu | grep "^CPU(s):" | tr -d '[:blank:]' | cut -f 2 -d ':')
    ([ $RET -gt 4 ] && echo 8 ) || echo $RET
}

function souffle_compile(){
    SFL_DST="$1"
    SFL_SRC="$2"
    if [ "$SFL_SRC" -nt "$SFL_DST" ]; then
        stage "souffle compiling $SFL_SRC -> $SFL_DST"
        which souffle || (echo "please put souffle in the path" && exit 1)
        mkdir -p $(dirname "$SFL_DST")
        souffle -j8 -o "$SFL_DST" "$SFL_SRC" | tee "$SFL_DST.log"
        mv $(basename "$SFL_DST") "$SFL_DST"
    fi
}

# convert all the facts files into a sane version
function sanitise(){
    DIR="./D_sane/$2/"
    if [ ! -d "$DIR" ]; then
        stage sanitise $2
        mkdir -p "$DIR"
        for FI in "$1"/*.facts; do
            sed -e 's/_/__/g' -e 's/"/_Q/g' \
                -e "s/'/_q/g" -e 's/,/_c/g' \
                -e 's/ /_s/g' < "$FI" > "$DIR"/$(basename "$FI") 
        done
        [ -f "$1/meta" ] && cp "$1/meta" "$DIR/meta"
    fi
}
function sanitise_wrapper(){
    sanitise $(paste <(cases) <(cases -d) | grep "^$1" | cut -f 2) "$1"
}

# convert the input files to csv
function convert_to_csv(){
    SRC="D_sane/$1"
    DST="D_converted/$1"
    
    if [ ! -d "$DST" ]; then
        souffle_compile "$VDSP" "$VDSCR"
        stage "convert" $1
        mkdir -p "$DST"
        "$VDSP" -j${OMP_NUM_THREADS} -F "$SRC" -D "$DST" 2>&1 | tee "$DST/log.txt"
        for CSV in "$DST/"*.csv; do
            sed -i -e 's/\t/,/' -e 's/\t/,/' -e 's/\t/:/g' $CSV
        done
    fi
}

# optimise $2 with $3 rounds on cases $@
function optimised_execution(){
    CASE="$1"
    SPEC="$2"
    ROUNDS="$3"
    shift 3
    LOGIC=$(basename "$SPEC" | sed 's/[^a-zA-Z].*//')
    OUT="D_specs/${LOGIC}_opt_${ROUNDS}.cflr"
    if [ ! -f "$OUT" ]; then
        stage optimising $OUT
        "$CAULI" -O "$ROUNDS" "$SPEC" "$@"
        mkdir -p $(dirname "$OUT")
        mv $(basename "$SPEC") "$OUT"
        rm cauliflower.log
    fi
    timed_execution "$CASE" "$OUT"
}

# run case $1 on the spec in $2, optionally re-run this $3 times
function timed_execution(){
    CASE="$1"
    SPEC="$2"
    RUNS="${3-3}"
    for i in $(seq 1 $RUNS); do
        INPUT="D_converted/$CASE"
        LOGIC=$(basename "${SPEC%.cflr}")
        EXE="D_exes/${LOGIC}"
        OUT="D_results/$LOGIC/${CASE}_$i"
        if [ ! -f "$OUT" ]; then
            if [ ! -x "$EXE" ]; then
                stage "compiling $EXE"
                mkdir -p $(dirname "$EXE")
                rm -f cauliflower.log
                "$CAULI" -c -r -p -o $(dirname "$EXE") "$SPEC"
                mv cauliflower.log "$EXE.log"
            fi
            stage "run ($i) $LOGIC $CASE"
            mkdir -p $(dirname "$OUT")
            timeout 600 "$EXE" "$INPUT" 2>&1 | tee "$OUT" || echo "solve semi-naive=TIMEOUT" > "$OUT"
            [ -s "$OUT" ] || echo "solve semi-naive=TIMEOUT" > "$OUT"
            echo "=============" >> "$OUT"
            echo "threads=$OMP_NUM_THREADS" >> "$OUT"
        fi
    done
}

function timed_souffle(){
    CASE="$1"
    DL="$2"
    RUNS="${3-3}"
    S_EXE=./D_exes/$(basename "${DL%.dl}")
    for i in $(seq 1 $RUNS); do
        OUT=./D_results/$(basename "$S_EXE")/${CASE}_$i
        if [ ! -f "$OUT" ]; then
            souffle_compile "$S_EXE" "$DL"
            stage "souffle run ($i) $CASE" $(basename "$S_EXE")
            mkdir -p $(dirname "$OUT")
            /usr/bin/time -f "command=%C\nsolve semi-naive=%e\nsaturation=%P" \
                "$S_EXE" -j${OMP_NUM_THREADS} -F "./D_converted/$CASE" 2>&1 | tee "$OUT"
        fi
    done
}

#====================================#
# CaseRun, the important case runner #
#====================================#

# determine the experimental cases
function cases() {
    if [ $# == 0 ]; then 
        cases -d | sed -e 's/.*DATASETS\///' -e 's/[ \t]/_/g' -e 's/\//_/g'
    else
        find ./DATASETS -type f -name "AssignHeapAllocation.facts" | xargs dirname | sort -u
    fi
}

function case_run(){
    ACTION="$1"
    shift 1
    for C in $(cases); do
        "$ACTION" "$C" "$@"
    done
}

#===================#
# The actual script #
#===================#

while getopts "h" opt; do
    case $opt in
        h)
            usage
            exit 0
            ;;
        \?)
            errxit Unrecognised command
            ;;
    esac
done
shift $(($OPTIND -1))

[ $# == 0 ] || (echo "no arguments" && exit 1)
[ $(dirname "$0") == "." ] || (echo "run from this directory, i.e.\"./run.sh\"" && exit 1)
[ -d "./DATASETS" ] || (echo "put (or symlink) test cases in ./DATASETS" && exit 1)

OMP_NUM_THREADS=${OMP_NUM_THREADS-`thread_count`}
echo "Threads = $OMP_NUM_THREADS"

# build cauliflower
(pushd ../.. && ./gradlew installDist && popd)

case_run sanitise_wrapper
case_run convert_to_csv
for DISP in ../../src/test/examples/dispatch/*; do
    case_run timed_execution "$DISP"
    case_run optimised_execution "$DISP" 1 ./D_converted/2006_antlr
    case_run optimised_execution "$DISP" 999 ./D_converted/2006_antlr
done
case_run timed_souffle "./souffle_virtual.dl"
