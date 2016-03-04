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

function dlm_to_csv(){
    local AWK_CMD=
    case `awk -F'\t' '{print NF; exit}' $1` in
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
            echo "Bad number of columns in $1" >&2
            head $1 >&2;
            exit 2
            ;;
    esac
    sed -e 's/\\/\\\\/g' -e 's/,/\\c/g' -e 's/"/\\q/g' $1 | awk -F'\t' "{print $AWK_CMD}" > ${1%\.dlm}.csv
}

function bc_to_dlm(){
    local CCLYSER="$CCLYSER_HOME/bin/fact-generator"
    local OUT_DIR="${1}_rels"
    
    rm -rf $OUT_DIR
    mkdir -p $OUT_DIR
    $CCLYSER $1 --out-dir $OUT_DIR || (rm -rf $OUT_DIR && exit 1)
    for FIL in `find $OUT_DIR -type f -name "*.dlm"`; do
        if [ -s $FIL ]; then
            dlm_to_csv $FIL
            rm $FIL
        fi
    done
    mv `find $OUT_DIR -type f -name "*.csv"` $OUT_DIR/
    # additionally create some filters
    echo -e "@malloc,@malloc\n@calloc,@calloc\n@realloc,@realloc" > $OUT_DIR/function_allocating-byname.csv
}

( [ $# -gt 0 ] && [ -f $1 ] ) || (usage && exit 1)

for FI in $@; do
    bc_to_dlm $FI
done
