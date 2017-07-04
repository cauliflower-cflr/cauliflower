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

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh

#
# Run the analysis 
#
for DIR in $CCLYS_DIR/*_rels; do
    BMK=`sed 's/^.*\/\([^\/]*\).bc_rels.*$/\1/' <<< $DIR`
    echo $BMK
    $CCLYS_DIR/$CCLYS_NAME $DIR 2>>`tempo $BMK.ccl`
done
