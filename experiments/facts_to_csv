#! /usr/bin/env bash

#=========================================================================#
#                             facts_to_csv.sh                             #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-24                                                       #
#                                                                         #
# Convert .facts files in this directory to CSV format                    #
#=========================================================================#

set -e
set -u

function usage(){
    grep "^#.*#$" $0
}

function swappr(){
    sed -e "s/$1/$2/g"
}

QUOT=''

while getopts "hq" opt; do
    case $opt in
        h)
            usage
            exit 0
            ;;
        q)
            QUOT='"'
            ;;
        \?)
            usage
            exit 1
            ;;
    esac
done
shift $(($OPTIND -1))

for FI in $1/*.facts; do
    echo $FI
    cat $FI | swappr "_" "__" | swappr '"' '_Q' | swappr "'" "q" | swappr " " "_s" | swappr "," "_c" | swappr "\t" "$QUOT,$QUOT" | sed -e "s/^/$QUOT/" -e "s/$/$QUOT/" > ${FI%.facts}.csv
done
