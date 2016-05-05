#! /usr/bin/env bash

#=========================================================================#
#                                solve.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-May-05                                                       #
#                                                                         #
# Run the approx and tc glps for the given problem                        #
#=========================================================================#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

function solve(){
    local STEM="$DIR/r$1b$2-"
    [ -f ${STEM}tc.ans ] || glpsol -m $SELF/tc.mod -d $DIR/*.dat -y ${STEM}tc.ans -o ${STEM}tc.log 2>&1 | tee ${STEM}tc.out
    [ -f ${STEM}sh.ans ] || cat $DIR/*.dat | sed -e "s/\\(^.*Rc.*:=\\).*;/\\1$1;/" -e "s/\\(^.*Bc.*:=\\).*;/\\1$2;/" | glpsol -m $SELF/sh.mod -d /dev/stdin -y ${STEM}sh.ans -o ${STEM}sh.log 2>&1 | tee ${STEM}sh.out
    echo ----------------------------------------------------------
    grep "PL$" ${STEM}sh.ans | sed -e 's/ PL//' -e 's/ //g' -e 's/|$//' | sort -nt '|' | column -ts '|'
    echo
    diff -y <(grep 'TC$' ${STEM}sh.ans | sort -u) <(grep 'TC$' ${STEM}tc.ans | sort -u) | tr '[:blank:]' ' ' | sed -e 's/ TC//g' -e 's/   */,/' | column -ts ','
}

BINS=""
RUNS=""
while getopts "b:hnr:" opt; do
    case $opt in
        b)
            BINS=$OPTARG
            ;;
        h)
            usage
            exit 0
            ;;
        r)
            RUNS=$OPTARG
            ;;
        \?)
            usage
            exit 1
            ;;
    esac
done
shift $(($OPTIND -1))

[ $# -ge 1 -a -d $1 ] || (usage >&2; exit 1)

SELF=$(dirname $0)
DIR=${1%/}

if [ -z "$BINS" ]; then BINS=$(grep "param Bc" $DIR/*.dat | sed 's/.*:=\(.*\);/\1/'); fi
if [ -z "$RUNS" ]; then RUNS=$(grep "param Rc" $DIR/*.dat | sed 's/.*:=\(.*\);/\1/'); fi

for R in $RUNS; do
    for B in $BINS; do
        solve $R $B
    done
done

