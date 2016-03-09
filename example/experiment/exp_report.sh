#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                              exp_report.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-06                                                       #
#                                                                         #
# collates several experimental runs into a report                        #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Return the relative path of the argument to the current directory
#
function relative(){
    python -c "import os.path; print os.path.relpath('$1', '.')"
}

#
# like paste, but vertically (only works with stdin)
#
function vpaste(){
    COUNT=0
    for i in $PASTE_ARGS; do
        COUNT=$(($COUNT + 1))
    done
    TMPS=""
    while read LINE; do
        TMP=`mktemp`
        echo "$LINE" > $TMP
        for L in `seq 2 $COUNT`; do
            read LIN
            echo "$LIN" >> $TMP
        done
        TMPS="$TMPS $TMP"
    done
    paste $TMPS
    rm $TMPS
}

function avg_raw(){
    sed -e 's/^/a=['/ -e 's/$/];print sum(a)\/float(len(a))/' | python
}

#
# convert each line of values into the numeric average of those values (as a float)
#
function avg(){
    tr '\n' '\t' | sed -e 's/\t/,/g' | avg_raw
}

#
# capture the output csv and print it with a pretty border to the screen
#
function tee_and_out(){
    echo
    echo "| $1 |"
    tee $1 | column -ts","
}

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh
RES_DIR="."
if [ $# == 1 ]; then
    RES_DIR=$1
fi
[ -d $RES_DIR ] || (echo "please specify the result directory (. by default)" >&2 && exit 1)
rm -f $RESULT_ARCHI $RESULT_CCLYS $RESULT_GIGAS

TMP_GIGA_TIMES=`mktemp`
TMP_GIGA_CAULI=`mktemp`
TMP_GIGA_NAMES=`mktemp`
TMP_GIGA=`mktemp`
TMP_PARA=`mktemp`
TMP_CCLY=`mktemp`

# create an archive of results
zip -q $RESULT_ARCHI $RES_DIR/*.explog

# gigascale
find $RES_DIR -name "*giga.explog" | xargs paste -d "," | sort | grep -v "benchmark,TC-time,TC-mem" | cut -d "," -f 2,5,8,11,14 | avg_raw > $TMP_GIGA_TIMES
find $RES_DIR -name "*giga.explog" | xargs paste -d "," | sort | grep -v "benchmark,TC-time,TC-mem" | cut -d "," -f 1 > $TMP_GIGA_NAMES
for N in `cat $TMP_GIGA_NAMES`; do
    B1=`find $RES_DIR -name "*.$N.1.cauli.explog"`
    BM=`find $RES_DIR -name "*.$N.8.cauli.explog"` # TODO work out the MAX_THREADS yourself
    BS=`find $RES_DIR -name "*.$N.s.cauli.explog"`
    ([ -n "$BS" ] && echo $(cat $BS | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg)) || echo " "
    ([ -n "$B1" ] && echo $(cat $B1 | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg)) || echo " "
    ([ -n "$BM" ] && echo $(cat $BM | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg)) || echo " "
    echo $(cat $BS $B1 | grep "|VarPointsTo|=" | sed -e 's/|VarPointsTo|=//g' | head -n 1)
    echo $(grep -E "\|(Alloc|Assign|Load|Store)\|" $BS $B1 | sed 's/.*=//' | paste -d "+" - - - - | sed 's/^/print /' | python | head -n 1)
done | paste -d "," - - - - - > $TMP_GIGA_CAULI
paste -d "," $TMP_GIGA_NAMES $TMP_GIGA_TIMES $TMP_GIGA_CAULI > $TMP_GIGA
cat - $TMP_GIGA <<< "benchmark,tgigascale,tscauli,t1cauli,tmcauli,sizevpt,size" | tee_and_out $RESULT_GIGAS

# parallel
echo "threads,perfect,time,speedup" > $TMP_PARA
SINGLE=0
for T in `find $RES_DIR -name "*openjdk.*.cauli.explog" | sed 's/.cauli.explog//' | sed 's/^.*openjdk\.//' | sort -un`; do
    BMKF=`find $RES_DIR -name "*.openjdk.$T.cauli.explog"`
    echo $T
    python -c "print 1.0/$T"
    TIME=$(cat $BMKF | grep "solve semi-naive=" | sed -e 's/solve semi-naive=//g' | avg)
    if [ $T == 1 ]; then
        SINGLE=$TIME
    fi
    echo $TIME
    python -c "print $TIME/float($SINGLE)"
done | paste -d "," - - - - >> $TMP_PARA
cat $TMP_PARA | tee_and_out $RESULT_PARAL

# cclyser
for BMK in $SPEC_BENCHES; do
    BMKF=`find $RES_DIR -name "*.$BMK.ccl.explog"`
    echo -n $BMK,
    echo -n $(paste $BMKF | grep "solve semi-naive=" | sed -e 's/solve semi-naive=//g' | avg),
    echo -n $(paste $BMKF | grep "|pt|=" | sed -e 's/|pt|=//g' | cut -f 1),
    echo -n $(paste $BMKF | grep "|uninit_mem|=" | sed -e 's/|uninit_mem|=//g' | cut -f 1),
    echo -n $(grep -E "\|(alloc|assign|load|store)\|" $BMKF | sed 's/.*=//' | paste -d "+" - - - - | sed 's/^/print /' | python | head -n 1)
    echo
done > $TMP_CCLY
cat - $TMP_CCLY <<< "benchmark,tcclyser,sizept,sizeum,size" | tee_and_out $RESULT_CCLYS

rm $TMP_GIGA_TIMES $TMP_GIGA_CAULI $TMP_GIGA_NAMES $TMP_GIGA $TMP_CCLY $TMP_PARA

