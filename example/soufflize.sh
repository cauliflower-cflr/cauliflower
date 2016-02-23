#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                              soufflize.sh                               #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Feb-23                                                       #
#                                                                         #
# Convert a directory of .csv files to a souffle-compatible directory of  #
# .facts files                                                            #
#                                                                         #
# ./soufflize.sh <dir>                                                    #
#     Converts .csv files in <dir> to .facts                              #
#-------------------------------------------------------------------------#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

[ $# == 1 ] || ( usage && exit 1 )

for FI in $1/*.csv; do
    readlink -f $FI
    sed 's/^"//' $FI | sed 's/"$//' | sed 's/","/\t/g' | sed -e '$a\' > ${FI%.csv}.facts
done
