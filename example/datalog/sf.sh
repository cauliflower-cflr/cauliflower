#! /usr/bin/env bash

#=========================================================================#
#                                  sf.sh                                  #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-29                                                       #
#=========================================================================#

set -e
set -u

pushd "$(dirname $0)"
[ -f EXE ] || ~/uni/souffle/build/src/souffle -o EXE --auto-schedule -j1 ./souf.dl
python ./convert.py -f "$1"
timeout $2 time ./EXE -j1 || echo "TIMEOUT"
trap popd EXIT
