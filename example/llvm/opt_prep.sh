#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                               opt_prep.sh                               #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Jan-12                                                       #
#-------------------------------------------------------------------------#

if [ "x$1" != "x" ]; then
    echo cleaning
    rm -vf tests/*.0 tests/*.ll tests/*.bc tests/*.exe
    rm -rf tests/*_rels
else
    echo compiling:
    for CF in tests/*.c; do
        echo " - $CF"
        clang -c $CF -emit-llvm -o $CF.0
        opt -mem2reg $CF.0 > $CF.bc
        llvm-dis $CF.0
        llvm-dis $CF.bc
        clang $CF -o $CF.exe
        [ -n "$CCLYSER_HOME" ] && `dirname $0`/cclyser_convert.sh $CF.0 $CF.bc
    done
fi


