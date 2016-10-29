#! /usr/bin/env bash

#=========================================================================#
#                                  z3.sh                                  #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-29                                                       #
#=========================================================================#

set -e
set -u

pushd "$(dirname $0)"
python ./convert.py "$1"
(timeout $2 ~/soft/z3/build/z3 -st -v:0 -dl ./z3.datalog || echo "TIMEOUT") | grep -v "(.*=.*))$" 
trap popd EXIT
