#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                                rdmake.sh                                #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Feb-11                                                       #
#                                                                         #
# Generates a random table of size  from the domain [1-$2] to [1-$3]      #
#-------------------------------------------------------------------------#

set -u
set -e

python -c "
import random
for i in xrange(0, $1):
    print \"%d,%d\" % (random.randint(1,$2), random.randint(1,$3))
"
