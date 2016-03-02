#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                           cclyser_convert.sh                            #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-02                                                       #
#                                                                         #
# Converts the output of cclyser fact-generator to cauliflower-compatible #
# .csv files, single-column tables are turned into self-loops, ternaty    #
# relations are reordered (cclyser always puts index in column 1) to be   #
# from-to-index ordered                                                   #
#-------------------------------------------------------------------------#

set -u
set -e

function usage(){
    grep "^#.*#$" $0
}

( [ $# == 1 ] && [ -d $1 ] ) || (usage && exit 1)

for FIL in `find $1 -type f -name "*.dlm"`; do
    if [ -s $FIL ]; then
        AWK_CMD=
        case `awk -F'\t' '{print NF; exit}' $FIL` in
            "1")
                AWK_CMD='$1 "," $1'
                ;;
            "2")
                AWK_CMD='$1 "," $2'
                ;;
            "3")
                AWK_CMD='$1 "," $3 "," $2'
                ;;
            *)
                echo "Bad number of columns in $FIL" >&2
                head $FIL >&2;
                exit 2
                ;;
        esac
        sed -e 's/\\/\\\\/g' -e 's/,/\\c/g' -e 's/"/\\q/g' $FIL | awk -F'\t' "{print $AWK_CMD}" > ${FIL%\.dlm}.csv
    fi
done

