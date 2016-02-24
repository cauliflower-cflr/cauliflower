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
#                                                                         #
# ./soufflize.sh <dir> -nq                                                #
#     Converts .csv files in <dir> to .facts where csv data isnt quoted   #
#-------------------------------------------------------------------------#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

[ $# -gt 0 ] || ( usage && exit 1 )

for FI in $1/*.csv; do
    readlink -f $FI
    if [ $# > 1 ]; then
        sed -e 's/,/\t/g' -e '$a\' $FI > ${FI%.csv}.facts
    else
        sed 's/^"//' $FI | sed 's/"$//' | sed 's/","/\t/g' | sed -e '$a\' > ${FI%.csv}.facts
    fi
done
