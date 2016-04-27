#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                                sh_ho.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Apr-26                                                       #
#                                                                         #
# Run the shappiro-horwitz algorithm on the given java-points-to          #
# directory                                                               #
#-------------------------------------------------------------------------#

function usage(){
    grep "^#.*#$" $0
}

set -e
set -u

SELF=$(dirname $0)
DIR=$(readlink -f $1)
WORK="${DIR}_$2"

echo "Solving: " $DIR
echo "Shrink: " $2

rm -rf $WORK
cp -r $DIR $WORK
$SELF/shrink.py -d $WORK -s $2 Alloc.csv:1 VarPointsTo.csv:1
for D in $WORK/shrink_sh_*; do
    $SELF/experiment/java_gigascale/java_gigascale_s $D VarPointsTo | tail -n +2 | sort -u > $D/VarPointsTo.out
done
$SELF/unshrink.py -d $WORK VarPointsTo.out:1
