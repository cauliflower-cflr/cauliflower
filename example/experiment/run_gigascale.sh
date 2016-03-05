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
. $(dirname $0)/common.sh
GIGA_REPORT=`tempfile -d $GIGAS_DIR -p GIGA -s ".cauli"`
echo $MAX_THREADS > /dev/null

#
# Run the gigascale software
#
(cd $GIGAS_DIR/gigascale; ./Table3.sh -wdl )
mv $GIGAS_DIR/gigascale/Table3.csv ${GIGA_REPORT%cauli}giga

#
# Run the cauliflower software
#
for DIR in `find $GIGAS_DIR -name "Alloc.csv" | grep -v "openjdk" | grep -v "/\." | grep -v "/docs/" | xargs dirname 2>/dev/null | sort -ur`; do
    echo $DIR
    OMP_NUM_THREADS=$MAX_THREADS /usr/bin/time -ao $GIGA_REPORT $GIGAS_DIR/$GIGAS_NAME $DIR
done

OJDK=`find $GIGAS_DIR -type d -name "openjdk"`
for T in `seq 1 $MAX_THREADS`; do
    OMP_NUM_THREADS=$T /usr/bin/time -ao $GIGA_REPORT $GIGAS_DIR/$GIGAS_NAME $OJDK
done

