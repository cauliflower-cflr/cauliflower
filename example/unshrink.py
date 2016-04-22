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
'''

__doc__ = __doc__.strip()

import os
import re
import shrink
import sys

def read_shapiro_horwitz_mapping(base_dir, target, column):
    conv = {}
    res = {}
    print target, column
    for shrink_i in xrange(0, len(filter(lambda p: os.path.isdir(p) and re.match("^shrink_sh_[0-9]+$", p), os.listdir(base_dir)))):
        shrink_m = os.path.join(base_dir, "shrink_sh_%d" % shrink_i, shrink.map_name)
        with open(shrink_m) as map_file:
            for row in shrink.csv_read(map_file):
                if not conv.has_key(row[0]):
                    conv[row[0]] = []
                conv[row[0]].append(int(row[1]))
            
    return (conv, res)


def unshrink():
    direct, cols = shrink.init(sys.argv[1:], __doc__)
    conv, res = read_shapiro_horwitz_mapping(direct, cols.keys()[0], cols[cols.keys()[0]][0])

if __name__ == "__main__":
    unshrink()
