#!/usr/bin/env bash
# memusg -- Measure memory usage of processes, 
# writing the used memory to <outfile>
#   Usage: memusg <outfile> COMMAND [ARGS]...
#
# Author: Jaeho Shin <netj@sparcs.org>
# Created: 2010-08-16
# Modified: 2015-06-02 - Nic H.
set -um
 
# check input
[ $# -gt 1 ] || { sed -n '2,/^#$/ s/^# //p' <"$0"; exit 1; }

# Modified by Nic H:
OUTFI="$1"
# End Modification
 
# TODO support more options: peak, footprint, sampling rate, etc.
 
pgid=`ps -o pgid= $$`
# make sure we're in a separate process group
if [ $pgid = $(ps -o pgid= $(ps -o ppid= $$)) ]; then
cmd=
set -- "$0" "$@"
for a; do cmd+="'${a//"'"/"'\\''"}' "; done
exec bash -i -c "$cmd"
fi
 
# detect operating system and prepare measurement
case `uname` in
Darwin|*BSD) sizes() { /bin/ps -o rss= -g $1; } ;;
Linux) sizes() { /bin/ps -o rss= -$1; } ;;
*) echo "`uname`: unsupported operating system" >&2; exit 2 ;;
esac
 
# monitor the memory usage in the background.
(
peak=0
while sizes=`sizes $pgid`
do
set -- $sizes
sample=$((${@/#/+}))
let peak="sample > peak ? sample : peak"
sleep 0.1
done
echo "memory(kb)=$peak" >> $OUTFI
sleep 0.1
) &
monpid=$!
 
shift # Remove the initial outfi file
# run the given command
exec "$@"

