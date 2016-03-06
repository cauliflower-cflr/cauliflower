#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                            run_gigascale.sh                             #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-05                                                       #
#                                                                         #
# Run the gigascale points-to experiments                                 #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh
GIGAS_REPORT=`tempfile -d $GIGAS_DIR -p GIGA -s ".cauli"`
echo $MAX_THREADS > /dev/null

#
# Run the gigascale software
#
(cd $GIGAS_DIR/gigascale; ./Table3.sh -wdl )
mv $GIGAS_DIR/gigascale/Table3.csv ${GIGAS_REPORT%cauli}giga

#
# Run the cauliflower software
#
for DIR in `find $GIGAS_DIR -name "Alloc.csv" | grep -v "openjdk" | grep -v "/\." | grep -v "/docs/" | xargs dirname 2>/dev/null | sort -ur`; do
    echo $DIR
    $GIGAS_DIR/${GIGAS_NAME}_s $DIR 2>>$GIGAS_REPORT
done

OJDK=`find $GIGAS_DIR -type d -name "openjdk"`
for T in `seq 1 $MAX_THREADS`; do
    echo $T
    OMP_NUM_THREADS=$T $GIGAS_REPORT $GIGAS_DIR/${GIGAS_NAME}_p $OJDK 2>>$GIGAS_REPORT
done

