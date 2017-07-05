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

function usage(){
    grep "^#.*#$" $0
}

function errxit(){
    [ $# -gt 0 ] && echo "Error: $@" >&2
    echo "Re-run with -h for help" >&2
    exit 1
}

# convert all the facts files into a sane version
function sanitise(){
    DIR="./D_sane/$2/"
    if [ ! -d "$DIR" ]; then
        echo " - sanitise"
        mkdir -p "$DIR"
        for FI in "$1"/*.facts; do
            sed -e 's/_/__/g' -e 's/"/_Q/g' \
                -e "s/'/_q/g" -e 's/,/_c/g' \
                -e 's/ /_s/g' < "$FI" > "$DIR"/$(basename "$FI") 
        done
        [ -f "$1/meta" ] && cp "$1/meta" "$DIR/meta"
    fi
}

# convert the input files to csv
function convert_to_csv(){
    VDSCR="../optimise_vdispatch/vdispatch.dl"
    VDSP="/tmp/virtual_dispatch_csv_converter"
    SRC="D_sane/$1"
    DST="D_converted/$1"
    
    if [ ! -f "$VDSP" ]; then
        echo " - build exe"
        which souffle || (echo "please put souffle in the path" && exit 1)
        souffle -j8 -o "$VDSP" "$VDSCR"
        echo "   finished building the executable"
        mv $(basename "$VDSP") "$VDSP"
    fi
   
    if [ ! -d "$DST" ]; then
        echo " - convert"
        mkdir -p "$DST"
        "$VDSP" -j8 -F "$SRC" -D "$DST" 2>&1 | tee "$DST/log.txt"
    fi
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

find ./DATASETS -type f | grep "HeapAllocation.facts$" | while read FILE; do
    DIR=$(dirname "$FILE")
    CASE=$(echo "$DIR" | sed -e 's/.*DATASETS\///' -e 's/[ \t]/_/g' -e 's/\//_/g')
    echo $DIR $CASE
    sanitise "$DIR" "$CASE"
    convert_to_csv "$CASE"
done
