#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                           opt_pass_finder.sh                            #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2015-Dec-08                                                       #
#-------------------------------------------------------------------------#

OPT_S=`opt -help | grep -n "Optimizations available:" | sed 's/:.*$//'`
OPT_E=`opt -help | tail -n +$((OPT_S + 1)) | grep -nv "^    -" | head -1 | sed 's/:.*$//'`
for PASS in `opt -help | tail -n +$((OPT_S + 1)) | head -$((OPT_E - 1)) | sed 's/ *-//' | sed 's/ .*$//'`; do
    AAS=`echo | opt --debug-pass=Structure -$PASS 2>&1 > /dev/null | grep -i "alias"`
    if [ "x$AAS" != "x" ]; then
        echo -n "="
        opt -help | tail -n +$((OPT_S + 1)) | head -$((OPT_E - 1)) | grep "    -$PASS "
    fi
done

