#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                              exp_report.sh                              #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-06                                                       #
#                                                                         #
# collates several experimental runs into a report                        #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Define some useful vars
#
. $(dirname $0)/exp_common.sh

