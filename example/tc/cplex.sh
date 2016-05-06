#! /usr/bin/env bash

#=========================================================================#
#                                cplex.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-May-06                                                       #
#=========================================================================#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

which oplrun 2>/dev/null 1>/dev/null || (echo -e "oplrun not in path\nexport PATH=\"\$PATH:/home/nic/soft/cplex-12.6.3/opl/bin/x86-64_linux\""; exit 1)

function solve(){
    cat $DIR/*.dat | sed -e 's/\([0-9][0-9]*\) \([0-9][0-9]*\)/<\1,\2>/' | tr '\n' ' ' | sed -e 's/ param //g' -e 's/ set //g' -e 's/:=/=/g' -e 's/data;//' -e 's/>[^,]</>,</g' | tr -d ' ' | sed 's/=\(<.*>\);/={\1};/' | LD_LIBRARY_PATH=`which oplrun | xargs dirname` oplrun $1 /dev/stdin
}

BINS="2"
RUNS="2"
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

for SOLV in tc sh; do
    for DAT in $DIR/*.dat; do
        OUT=$DIR/r${RUNS}b${BINS}_${DAT%.dat}_${SOLV}.out
        #solve $SELF/opl_sh.mod $DAT $OUT
        echo $OUT
    done
done
