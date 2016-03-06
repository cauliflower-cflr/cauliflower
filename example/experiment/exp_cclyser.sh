#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                             run_cclyser.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-05                                                       #
#                                                                         #
# Run the uninitialised variable experiments                              #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh
CCLYS_COUNT=`mktemp --tmpdir=$CCLYS_DIR --suffix=".exp.count"`
CCLYS_TIMES="${CCLYS_COUNT%count}times"

#
# Run the analysis 
#
for DIR in $CCLYS_DIR/*_rels; do
    echo $DIR | tee -a "$CCLYS_TIMES" "$CCLYS_COUNT"
    $CCLYS_DIR/$CCLYS_NAME $DIR uninit_mem 2>>$CCLYS_TIMES | wc >>$CCLYS_COUNT 2>/dev/null
done
