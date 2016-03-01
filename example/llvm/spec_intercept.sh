#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                            spec_intercept.sh                            #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-01                                                       #
#                                                                         #
# Designed to intercept the specmake make commands and re-interperet them #
# in ~mysterious ways~                                                    #
#-------------------------------------------------------------------------#

function usage(){
    grep "^#.*#$" $0
}

function compile(){
    local OUTPUT=`echo "$@" | sed -e 's/\t/ /g' -e 's/  */ /g' -e 's/^.*-o *\([^ ]*\) .*$/\1/'`
    $@
    #llvm-dis $OUTPUT
}

function link(){
    # Process the arguments
    local COMPILER=$1
    shift
    echo COMPILER=$COMPILER
    local INPUTS=`echo "$@" | sed -e 's/\t/ /g' -e 's/  */ /g' | tr ' ' '\n' | grep -v "^-" | grep "\.o$" | tr '\n' ' '`
    echo INPUTS=$INPUTS
    local OUTPUT=`echo "$@" | sed -e 's/\t/ /g' -e 's/  */ /g' -e 's/^.*-o *\([^ ]*\).*$/\1/'`
    echo OUTPUT=$OUTPUT
    local FLAGS="$@"
    for IN in $INPUTS -o $OUTPUT; do FLAGS=`echo $FLAGS | sed "s/${IN//\//\\\/}//"`; done
    echo FLAGS=$FLAGS
    # Combine the bc files into one large file
    llvm-link $INPUTS -o ${OUTPUT}.all.bc
    opt -mem2reg ${OUTPUT}.all.bc -o ${OUTPUT}.bc
    $COMPILER $FLAGS -O0 ${OUTPUT}.bc -o $OUTPUT
}

set -e
set -u

[ $# -gt 0 ] || (usage && exit 1)

SELF=`readlink -f $0`
MODE=$1
shift
case $MODE in
    "c")
        compile clang -emit-llvm $@
        ;;
    "c++")
        compile clang++ -emit-llvm $@
        ;;
    "ldc")
        link clang $@
        ;;
    "ldc++")
        link clang++ $@
        ;;
    *)
        echo "UNRECOGNISED SPEC_INTERCEPT MODE: \"$MODE\"" >&2
        exit 2
        ;;
esac
