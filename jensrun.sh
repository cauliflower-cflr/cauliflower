#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                               jensrun.sh                                #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2015-Dec-02                                                       #
#-------------------------------------------------------------------------#

function usage() {
    grep "^#.*#$" $0
}

function jensrun() {
    make -j4
    for AL in $1/*/Alloc.csv; do
        DIR=$(dirname $AL)
        echo $DIR
        mv $DIR/VarPointsTo.{csv,ans}
        time ./bin/jvpt $DIR VarPointsTo > $DIR/VPT.csv
        grep -v "^__VarPointsTo__$" $DIR/VPT.csv | sort -u | diff $DIR/VarPointsTo.ans -
        #wc $DIR/VPT.csv $DIR/VarPointsTo.ans
        rm $DIR/VPT.csv
        mv $DIR/VarPointsTo.{ans,csv}
    done
}

if [ "x$1" == x ]; then
    jensrun $HOME/jens-cfl/artifact/datasets/dacapo9
else
    jensrun $1
fi
