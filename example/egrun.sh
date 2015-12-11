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

ADT="Std"
if [ $# -gt 0 ]; then
    ADT="$1"
fi

for EG in `find $MDIR -name "*.cflr"`; do
    FN=$(basename "$EG")
    NAME=${FN%.*}
    echo java -cp $JAR cauliflower.Main -a "$ADT" -sn $MDIR/../spikes/${NAME}.h -cs $MDIR/../spikes/${NAME}.cpp $EG
    java -cp $JAR cauliflower.Main -a Std -sn $MDIR/../spikes/${NAME}.h -cs $MDIR/../spikes/${NAME}.cpp $EG
done
astyle -Yn spikes/*
make -j4
