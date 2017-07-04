#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                             opt_cbuilder.sh                             #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2015-Dec-10                                                       #
#-------------------------------------------------------------------------#

set -u
set -e

which opt > /dev/null || (echo "cannot find llvm executable"; exit 1)
which opt > /dev/null
rm -rf build_cauli
mkdir build_cauli
(cd build_cauli && cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Debug .. && make -j4)
