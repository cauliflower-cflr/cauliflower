#! /usr/bin/env bash

#=========================================================================#
#                                  cl.sh                                  #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-29                                                       #
#=========================================================================#

set -e
set -u

pushd "$(dirname $0)"
trap popd EXIT
echo "$1 - $2"
timeout $2 ../../../CAUL/EXPE.sh `readlink -f "$1"`
