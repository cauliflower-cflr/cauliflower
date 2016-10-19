#! /usr/bin/env bash

#=========================================================================#
#                              csv_to_edb.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-19                                                       #
#=========================================================================#

set -e
set -u

for FI in $1/*.csv; do
    NM=$(basename "$FI")
    NM=${NM%.csv}
    echo $FI $NM
    sed -e '$a\' $FI | sed -e "s/^/$NM(/" -e "s/$/)./" > ./$NM.datalog
done

