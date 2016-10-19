#! /usr/bin/env bash

#=========================================================================#
#                            experimentblob.sh                            #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Sep-15                                                       #
#                                                                         #
# create an experimental blob using the current snapshot                  #
#=========================================================================#

set -e
set -u

if [ `readlink -f $(pwd)` != `dirname $(readlink -f $0)` ] ; then
    echo "run from the current directory"
    exit 1
fi

rm -rf EXPERIMENT
mkdir EXPERIMENT

pushd ..

rm -rf build
./gradlew assembleDist

#./build/install/cauliflower/bin/cauliflower -O 10 src/test/examples/java_vpt/slow.cflr -n OPTI -o tools/EXPERIMENT "$@"

cp build/distributions/*.zip src/test/examples/java_vpt/*.cflr tools/timings tools/EXPERIMENT

popd

scp -r EXPERIMENT $SSH_C1:
rm -rf EXPERIMENT

