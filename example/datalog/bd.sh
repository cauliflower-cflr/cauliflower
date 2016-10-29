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
cp ~/soft/bddbddb/lib*.so .
timeout $2 java -cp ~/soft/bddbddb/bddbddb-full.jar:~/soft/bddbddb/weka.jar -Dnoisy=no net.sf.bddbddb.Solver z3.datalog || echo TIMEOUT
rm ./lib*.so
trap popd EXIT
