#! /usr/bin/env bash

#=========================================================================#
#                             gen_dataset.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-26                                                       #
# Generates a vdispatch test case                                         #
#                                                                         #
#=========================================================================#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

VDSCR="$(dirname $0)/vdispatch.dl"
VDSP="/tmp/VDISPATCH"

while getopts "hr" opt; do
    case $opt in
        h)
            usage
            exit 0
            ;;
        r)
            rm -f "$VDSP"
            ;;
        \?)
            usage
            exit 1
            ;;
    esac
done
shift $(($OPTIND -1))

SRC="$1"
DST="$2"

if [ ! -f $VDSP ]; then
    which souffle || (echo "please put souffle in the path" && exit 1)
    souffle -j8 -o "$VDSP" "$VDSCR"
    echo "finished building the executable"
    mv ./VDISPATCH "$VDSP"
fi

rm -rf "$DST"
mkdir -p "$DST"
"$VDSP" -j8 -F "$SRC" -D "$DST" 2>&1 | tee "$DST/log.txt"

for FI in "$DST"/*.csv; do
    sed -i -e 's/_/__/g' -e 's/,/_c/g' -e 's/"/_q/g' -e 's/\t/,/' -e 's/\t/,/' -e 's/\t/:/g' $FI
done
