#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                           opt_stat_runner.sh                            #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2015-Dec-08                                                       #
#-------------------------------------------------------------------------#

for T in $@; do
    opt $T -gvn $T -licm -stats ./ct-mtr.bc > /dev/null
    opt $T -licm $T -gvn -stats ./ct-mtr.bc > /dev/null
done

echo "Optimisations are"
echo "    $@"
