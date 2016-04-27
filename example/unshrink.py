#! /usr/bin/env python

'''
+-------------------------------------------------------------------------+
|                               unshrink.py                               |
|                                                                         |
| Author: Nic H.                                                          |
| Date: 2016-Apr-21                                                       |
|                                                                         |
| retrieves the results from a shrunken problem context                   |
+-------------------------------------------------------------------------+
for DIR in shrink_sh_*; do ../../../../java_gigascale_s $DIR VarPointsTo | tail -n +2 | sort -u > $DIR/VarPointsTo.out; done
'''


__doc__ = __doc__.strip()

import os
import re
import shrink
import sys

def combos(lsts):
    if len(lsts) == 0:
        return
    for v in lsts[0]:
        if len(lsts) > 1:
            for c in combos(lsts[1:]):
                yield str(v) + c
        else:
            yield str(v)

def read_shapiro_horwitz_mapping(base_dir, target, column):
    conv = {}
    res = {}
    count=len(filter(lambda p: os.path.isdir(os.path.join(base_dir, p)) and re.match("^shrink_sh_[0-9]+$", p), os.listdir(base_dir)))
    for shrink_i in xrange(0, count):
        shrink_m = os.path.join(base_dir, "shrink_sh_%d" % shrink_i, shrink.map_name)
        with open(shrink_m, 'rb') as map_file:
            for row in shrink.csv_read(map_file):
                if not conv.has_key(row[0]):
                    conv[row[0]] = []
                conv[row[0]].append(row[1])
        shrink_t = os.path.join(base_dir, "shrink_sh_%d" % shrink_i, target)
        # TODO this only works with 2nd column=result, generalise for any column
        with open(shrink_t, 'rb') as target_file:
            for row in shrink.csv_read(target_file):
                if not res.has_key(row[0]):
                    res[row[0]] = [set() for i in xrange(0, count)]
                res[row[0]][shrink_i].add(int(row[column]))
    map_key = {"".join(v) : k for k, v in conv.items()}
    for k, v in res.items():
        for c in combos(v):
            if map_key.has_key(c):
                yield [k, map_key[c]]
    


def unshrink():
    direct, cols, size = shrink.init(sys.argv[1:], __doc__)
    with open(os.path.join(direct, cols.keys()[0]), 'wb') as out_file:
        out_csv = shrink.csv_write(out_file)
        for row in read_shapiro_horwitz_mapping(direct, cols.keys()[0], cols[cols.keys()[0]][0]):
            out_csv.writerow(row)

if __name__ == "__main__":
    unshrink()
