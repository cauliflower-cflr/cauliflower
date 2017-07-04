#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                                common.sh                                #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-05                                                       #
#                                                                         #
# Defines some common variables for use in the build and run* scripts     #
#-------------------------------------------------------------------------#

export SPEC_BENCHES="astar bzip2 h264ref hmmer libquantum mcf omnetpp sjeng"
#export SPEC_BENCHES="mcf sjeng"
export CCLYS_NAME="llvm_cclyser"
export GIGAS_NAME="java_gigascale"

export CAULI_DIR=`readlink -f $(dirname $0)/../..`
export EXPER_DIR="$CAULI_DIR/example/experiment"
export SPOUT_DIR=`grep "output_root" $EXPER_DIR/spec_config.cfg | sed -e 's/^.*= *//'`
export CCLYS_DIR="$EXPER_DIR/$CCLYS_NAME"
export GIGAS_DIR="$EXPER_DIR/$GIGAS_NAME"

export MEM_MONITOR="$EXPER_DIR/exp_mem_monitor.sh"
export BUILT_ARCHIVE="$EXPER_DIR/build.zip"

export RESULT_ARCHI="results.`date +%F`.zip"
export RESULT_GIGAS="results.giga.csv"
export RESULT_CCLYS="results.ccly.csv"
export RESULT_PARAL="results.para.csv"
export RESULT_SEQUE="results.sequ.csv"
export RESULT_COMPO="results.comp.csv"

# Function to bail out if the given program is not in the path
function which_or_bail(){
    which $1 >/dev/null 2>/dev/null || (echo "Unable to locate binary: $1" >&2 && exit 1)
}

# Creates a temporary directory in the expected location with $1.explog as suffix
function tempo(){
    mktemp --tmpdir=$EXPER_DIR --suffix=".$1.explog"
}

