#! /usr/bin/env bash

#=========================================================================#
#                                 EXPE.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-31                                                       #
#=========================================================================#

set -e
set -u

DIR="$(dirname $0)"
SPEC=`find "$DIR" -iname jvpt.cflr`
CASES="$1"

pushd $DIR
trap "popd" EXIT


if [ ! -f "bin/EXPE_Souffle" ]; then
    for DS in Std Souffle Btree; do
        java -cp "build/classes/main" cauliflower.Main -a $DS -r -sn "spikes/EXPE_$DS.h" -cs "spikes/EXPE_$DS.cpp" "$SPEC"
    done
    make -j4
fi
