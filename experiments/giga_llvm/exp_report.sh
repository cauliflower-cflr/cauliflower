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
TMP_GIGA_MEMS=`mktemp`
TMP_GIGA_CAULI=`mktemp`
TMP_GIGA_NAMES=`mktemp`
TMP_GIGA=`mktemp`
TMP_PARA=`mktemp`
TMP_SEQU=`mktemp`
TMP_SEQU2=`mktemp`
TMP_CCLY=`mktemp`

# create an archive of results
zip -q $RESULT_ARCHI $RES_DIR/*.explog

# gigascale
find $RES_DIR -name "*giga.explog" | xargs paste -d "," | sort | grep -v "benchmark,TC-time,TC-mem" | cut -d "," -f 2,5,8,11,14 | avg_raw > $TMP_GIGA_TIMES
find $RES_DIR -name "*giga.explog" | xargs paste -d "," | sort | grep -v "benchmark,TC-time,TC-mem" | cut -d "," -f 3,6,9,12,15 | avg_raw > $TMP_GIGA_MEMS
find $RES_DIR -name "*giga.explog" | xargs paste -d "," | sort | grep -v "benchmark,TC-time,TC-mem" | cut -d "," -f 1 > $TMP_GIGA_NAMES
MAX_THREADS=`ls ${RES_DIR}/*.cauli.explog | sed -e 's/\.cauli\.explog$//' -e 's/^.*\.//' | grep '^[0-9]*$' | sort -u | tail -n 1`
for N in `cat $TMP_GIGA_NAMES`; do
    B1=`find $RES_DIR -name "*.$N.1.cauli.explog"`
    BM=`find $RES_DIR -name "*.$N.$MAX_THREADS.cauli.explog"`
    BS=`find $RES_DIR -name "*.$N.s.cauli.explog"`
    ([ -n "$BS" ] && echo $(cat $BS | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg)) || echo " "
    cat $B1 | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg # single-thread time
    cat $BM | grep 'solve semi-naive=' | sed -e 's/solve semi-naive=//g' | avg # multi-thread time
    python -c "print $(grep "memory(kb)" $B1 | sed 's/.*=//' | tr '\n' '\t' | avg)/1024" # single-thread mem
    python -c "print $(grep "memory(kb)" $BM | sed 's/.*=//' | tr '\n' '\t' | avg)/1024" # multi-thread mem
    cat $BS $B1 | grep "|VarPointsTo|=" | sed -e 's/|VarPointsTo|=//g' | head -n 1
    grep -E "\|(Alloc|Assign|Load|Store)\|" $BS $B1 | sed 's/.*=//' | paste -d "+" - - - - | sed 's/^/print /' | python | head -n 1
done | paste -d "," - - - - - - - > $TMP_GIGA_CAULI
paste -d "," $TMP_GIGA_NAMES $TMP_GIGA_TIMES $TMP_GIGA_MEMS $TMP_GIGA_CAULI > $TMP_GIGA
cat - $TMP_GIGA <<< "benchmark,tgigascale,mgigascale,tscauli,t1cauli,tmcauli,m1cauli,mmcauli,sizevpt,size" | tee_and_out $RESULT_GIGAS

# parallel
echo "threads,perfect,lutime,trtime,optime,luspeed,trspeed,opspeed" > $TMP_PARA
SINGLE=0
for T in `seq 1 $MAX_THREADS`; do
    BL=`find $RES_DIR -name "*.lusearch.$T.cauli.explog"`
    BT=`find $RES_DIR -name "*.tradesoap.$T.cauli.explog"`
    BO=`find $RES_DIR -name "*.openjdk.$T.cauli.explog"`
    echo $T
    python -c "print 1.0/$T"
    TL=$(cat $BL | grep "solve semi-naive=" | sed -e 's/solve semi-naive=//g' | avg)
    TT=$(cat $BT | grep "solve semi-naive=" | sed -e 's/solve semi-naive=//g' | avg)
    TO=$(cat $BO | grep "solve semi-naive=" | sed -e 's/solve semi-naive=//g' | avg)
    if [ $T == 1 ]; then
        SL=$TL
        ST=$TT
        SO=$TO
    fi
    echo $TL
    echo $TT
    echo $TO
    python -c "print $TL/float($SL)"
    python -c "print $TT/float($ST)"
    python -c "print $TO/float($SO)"
done | paste -d "," - - - - - - - - >> $TMP_PARA
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

# sequence profiler
grep -E "(eval)|(SIZE [^d])" `ls $RES_DIR/*openjdk.t1.* | head -n 1` | paste -d " " - - | grep "eval6_0" | sed -e 's/^.*cur_delta=//' -e 's/ .*$//' | paste - - - - | cut -f 1 | awk '{c+=$1;print $1 "," c}' > $TMP_SEQU
paste -d "," $RES_DIR/*.openjdk.t1.* | grep "eval6_0" | sed -e 's/TIME[^e]*e[^ ]* //g' | avg_raw | paste -d "+" - - - - | sed -e 's/^/print /' | python | paste -d "," $TMP_SEQU - > $TMP_SEQU2
mv $TMP_SEQU2 $TMP_SEQU
paste -d "," $RES_DIR/*.openjdk.t8.* | grep "eval6_0" | sed -e 's/TIME[^e]*e[^ ]* //g' | avg_raw | paste -d "+" - - - - | sed -e 's/^/print /' | python | paste -d "," $TMP_SEQU - > $TMP_SEQU2
mv $TMP_SEQU2 $TMP_SEQU
echo "<not shown on command line>" | tee_and_out $RESULT_SEQUE
cat - $TMP_SEQU <<< "size,total,time1,timem" > $RESULT_SEQUE

# composition profiler
echo "<not shown on command line>" | tee_and_out $RESULT_COMPO
echo "time,smaller,larger,outer,inner,updates" > $RESULT_COMPO
#grep -E "(TIME.*eval)|(SIZE [^d])" *.t1.cauli.explog | sed -e 's/^.*SIZE[^=]*=\([0-9]*\) [^=]*=/\1\t/' -e 's/^.*TIME.*eval[^ ]* //' | paste - - | awk '{print $3 "\t" ($1 < $2 ? $1 "\t" $2 : $2 "\t" $1)}' | awk '{printf("%f,%d,%d,%f\n", $1, $2, $3, sqrt($2) + sqrt($3))}' >> $RESULT_COMPO
grep -E "^(TIME.*eval)|(SIZE [^d])|(COUNT)" *.t1.cauli.explog | sed -e 's/^.*SIZE[^=]*=\([0-9]*\) [^=]*=/\1\t/' -e 's/^.*TIME.*eval[^ ]* //' -e 's/^.*outer[^=]*=//' -e 's/ inner[^=]*=/\t/' -e 's/ updates[^=]*=/\t/'| paste - - - | awk '{print $3 "," ($1 < $2 ? $1 "," $2 : $2 "," $1) "," $4 "," $5 "," $6 }' | grep -v [^0-9,.] >> $RESULT_COMPO

# create the PDFs for the scatter plots
gnuplot $EXPER_DIR/exp_gnuplot.txt < $RESULT_COMPO
for FIL in results.compo.*.pdf; do
    TMP=`mktemp`
    mv $FIL $TMP
    pdfcrop $TMP $FIL
    rm $TMP
done

rm $TMP_GIGA_TIMES $TMP_GIGA_CAULI $TMP_GIGA_NAMES $TMP_GIGA $TMP_CCLY $TMP_PARA $TMP_SEQU

