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

export BUILT_ARCHIVE="$EXPER_DIR/build.zip"
export CAULI_JAR="$EXPER_DIR/cauliflower.jar"

# Function to bail out if the given program is not in the path
function which_or_bail(){
    which $1 >/dev/null 2>/dev/null || (echo "Unable to locate binary: $1" >&2 && exit 1)
}

# Creates a temporary directory in the expected location with $1.explog as suffix
function tempo(){
    mktemp --tmpdir=$EXPER_DIR --suffix=".$1.explog"
}

