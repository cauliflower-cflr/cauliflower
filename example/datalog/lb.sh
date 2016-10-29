#! /usr/bin/env bash

#=========================================================================#
#                                  lb.sh                                  #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Oct-29                                                       #
#=========================================================================#

set -e
set -u

export LB_PAGER_FORCE_START="True"
export LB_MEM_NOWARN="True"
export LOGICBLOX_HOME=`echo $HOME/soft/LogicBlox-*`
export PATH="$PATH:$LOGICBLOX_HOME/bin"

pushd "$(dirname $0)"
python ./convert.py -f "$1"

DB="`mktemp -d`/TMP_DB"
LB_LOGIC="./logi.dl"
LB_IMPORT="./facts.import"
LB_SIZE="./sizes.dl"

echo "logic_dir = $1"
echo "lb_home = $LOGICBLOX_HOME"
echo -n "lb_exe ="
which bloxbatch
echo "db =" $DB

bloxbatch -db $DB -create -overwrite -blocks base
bloxbatch -db $DB -addBlock -file $LB_SIZE
bloxbatch -db $DB -import $LB_IMPORT
timeout $2 /usr/bin/time bloxbatch -db $DB -addBlock -file $LB_LOGIC

bloxbatch -db $DB -popCount `bloxbatch -db $DB -list 2>&1 | grep -v ":" | sed 's/ //g' | tr '\n' ',' | sed 's/,$//'`

trap "echo cleaning up; rm -rf $DB; popd" EXIT
