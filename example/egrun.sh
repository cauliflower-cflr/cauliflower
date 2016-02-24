#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                                egrun.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2015-Dec-11                                                       #
#-------------------------------------------------------------------------#

set -e
set -u

MDIR=$(dirname $0)

gradle -p $MDIR/.. jar

JAR=$(readlink -f `find $MDIR/.. -iname "cauliflower*.jar"`)

for EG in `find $MDIR -name "*.cflr"`; do
    for ADT in Std Btree Souffle; do
        FN=$(basename "$EG")
        NAME=${FN%.*}
        C_FI="$MDIR/../spikes/${NAME}_$ADT.cpp"
        H_FI="$MDIR/../spikes/${NAME}_$ADT.h"
        CMD="java -cp $JAR cauliflower.Main -a $ADT -sn $H_FI -cs $C_FI $EG"
        echo $CMD
        $CMD
        which astyle 2>/dev/null && astyle -Yn $H_FI $C_FI
    done
done
make -C $MDIR/.. -j4
