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
GIGAS_REPORT=`mktemp --tmpdir=$GIGAS_DIR --suffix=".exp.cauli"`
echo $MAX_THREADS > /dev/null

#
# Run the gigascale software
#
(cd $GIGAS_DIR/gigascale; ./Table3.sh -wdl )
mv $GIGAS_DIR/gigascale/Table3.csv ${GIGAS_REPORT%cauli}giga

#
# Run the cauliflower software
#
echo $GIGAS_DIR
for DIR in `find $GIGAS_DIR -name "Alloc.csv" | grep -v "openjdk" | grep -v "datasets/\." | grep -v "/docs/" | xargs dirname 2>/dev/null | sort -ur`; do
    echo "dataset=$DIR threads=1" | tee -a $GIGAS_REPORT
    $GIGAS_DIR/${GIGAS_NAME}_s $DIR 2>>$GIGAS_REPORT
done

OJDK=`find $GIGAS_DIR -type d -name "openjdk"`
for T in `seq 1 $MAX_THREADS`; do
    echo "dataset=$OJDK threads=$T" | tee -a $GIGAS_REPORT
    OMP_NUM_THREADS=$T $GIGAS_DIR/${GIGAS_NAME}_p $OJDK 2>>$GIGAS_REPORT
done

