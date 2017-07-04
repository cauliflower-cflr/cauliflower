#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                               gigarun.sh                                #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Feb-02                                                       #
#                                                                         #
# executes jvpt on the provided discovered dataset                        #
#-------------------------------------------------------------------------#

set -u
set -e

function usage(){
    grep "^#.*#$" $0
}

if [ $# != 1 ] || [ ! -d $1 ]; then
    usage
    exit 1
fi

[ -f $(dirname $0)/../bin/jvpt ] || $(dirname $0)/egrun.sh

for DIR in `find $1 -name "VarPointsTo.ans" | xargs dirname`; do
    echo $DIR
    time $(dirname $0)/../bin/jvpt $DIR VarPointsTo | grep -v "^__VarPointsTo__$" | sort -u > $DIR/VarPointsTo.cauli
    diff $DIR/VarPointsTo.*
done
