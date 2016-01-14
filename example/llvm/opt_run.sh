#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                               opt_run.sh                                #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Jan-14                                                       #
#-------------------------------------------------------------------------#

set -u
set -e

make -C build_cauli/ -j4
echo "-------------------------------------"
cat ${1%.bc}.ll
echo "-------------------------------------"
opt --load build_cauli/cauli/libCauli.so -cauli-aa -aa-eval $1 2>&1 >/dev/null | sed 's/  */ /g' | grep "#.*#" | column -ts "#"
