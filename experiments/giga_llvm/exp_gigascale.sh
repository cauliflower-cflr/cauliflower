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
for DIR in `find $GIGAS_DIR -name "Alloc.csv" | grep -v "datasets/\." | grep -v "/docs/" | xargs dirname 2>/dev/null | sort -ur`; do
    DS=`sed -e 's/^.*\///' <<< $DIR`
    echo $DS
    if [ $DS != "openjdk" ]; then
        $GIGAS_DIR/${GIGAS_NAME}_s $DIR 2>>`tempo $DS.s.cauli`
    fi
    for T in `seq 1 $MAX_THREADS`; do
        echo "$T"
        OUTF1=`tempo $DS.$T.cauli`
        OMP_NUM_THREADS=$T $GIGAS_DIR/${GIGAS_NAME}_p $DIR 2>>$OUTF1
        if [ $T == 1 ] || [ $T == $MAX_THREADS ]; then
            OMP_NUM_THREADS=$T $MEM_MONITOR $OUTF1 $GIGAS_DIR/${GIGAS_NAME}_p $DIR
            OMP_NUM_THREADS=$T $GIGAS_DIR/${GIGAS_NAME}_t $DIR 2>>`tempo $DS.t$T.cauli`
        fi
    done
done

