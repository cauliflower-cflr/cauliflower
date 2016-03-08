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
echo $MAX_THREADS > /dev/null

#
# Run the gigascale software
#
(cd $GIGAS_DIR/gigascale; ./Table3.sh -wdl )
mv $GIGAS_DIR/gigascale/Table3.csv `tempo giga`

#
# Run the cauliflower software
#
for DIR in `find $GIGAS_DIR -name "Alloc.csv" | grep -v "openjdk" | grep -v "datasets/\." | grep -v "/docs/" | xargs dirname 2>/dev/null | sort -ur`; do
    DS=`sed -e 's/^.*\///' <<< $DIR`
    echo $DS
    $GIGAS_DIR/${GIGAS_NAME}_s $DIR 2>>`tempo $DS.s.cauli`
    OMP_NUM_THREADS=1 $GIGAS_DIR/${GIGAS_NAME}_p $DIR 2>>`tempo $DS.1.cauli`
    OMP_NUM_THREADS=$MAX_THREADS $GIGAS_DIR/${GIGAS_NAME}_p $DIR 2>>`tempo $DS.$MAX_THREADS.cauli`
    OMP_NUM_THREADS=1 $GIGAS_DIR/${GIGAS_NAME}_t $DIR 2>>`tempo $DS.t.cauli`
done

OJDK=`find $GIGAS_DIR -type d -name "openjdk"`
for T in `seq 1 $MAX_THREADS`; do
    echo "ojdk t=$T"
    OMP_NUM_THREADS=$T $GIGAS_DIR/${GIGAS_NAME}_p $OJDK 2>>`tempo openjdk.$T.cauli`
done

