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
    sed 's/\([0-9]*\) \([0-9]*\)/<\1,\2>/' $2 | tr '\n' ' ' | sed -e 's/> </>,</g' -e 's/^/E={/' -e 's/$/};/' | cat - <(echo -e "\nRc=$RUNS; Bc=$BINS;") | LD_LIBRARY_PATH=`which oplrun | xargs dirname` oplrun $1 /dev/stdin | tee $3
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

[ $# -ge 1 -a -f $1 ] || (usage >&2; exit 1)

SELF=$(dirname $0)
DAT="$1"
ODIR=${DAT%.dat}
mkdir -p $ODIR
OSH=$ODIR/sh_r${RUNS}_b${BINS}.out
OTC=$ODIR/tc_r${RUNS}_b${BINS}.out

[ -f $OSH ] || solve $SELF/opl_sh.mod $DAT $OSH
[ -f $OTC ] || solve $SELF/opl_tc.mod $DAT $OTC

echo ----------------------------------------------------------
grep "PL$" $OSH | sed -e 's/ PL//' | sort -n | column -ts ' '
echo
diff -y <(grep 'TC$' $OSH | sort -u) <(grep 'TC$' $OTC | sort -u) | tr '[:blank:]' ' ' | sed -e 's/ TC//g' -e 's/   */,/' | column -ts ','

