#! /usr/bin/env python

'''
+-------------------------------------------------------------------------+
|                                shrink.py                                |
|                                                                         |
| Author: Nic H.                                                          |
| Date: 2016-Apr-21                                                       |
|                                                                         |
| read the domains of input CSV columns and project onto a smaller domain |
+-------------------------------------------------------------------------+
for DIR in shrink_sh_*; do ../../../../java_gigascale_s $DIR VarPointsTo | tail -n +2 | sort -u > $DIR/VarPointsTo.out; done
'''

__doc__ = __doc__.strip()

import csv
import getopt
import os
import re
import shutil
import sys

map_name = "conversion.map"

# convert your file handles into csv streams
csv_write = lambda f: csv.writer(f, quoting=csv.QUOTE_ALL, lineterminator="\n")
csv_read = lambda f: csv.reader(f)

def write_conversion(in_dir, out_dir, shrink, conv):
    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.mkdir(out_dir)

    # convert the input domains
    for fi in filter(lambda n: os.path.isfile(os.path.join(in_dir, n)), os.listdir(in_dir)):
        if shrink.has_key(fi):
            with open(os.path.join(in_dir, fi), 'rb') as csvfile, open(os.path.join(out_dir, fi), 'wb') as csv_out:
                writ = csv_write(csv_out)
                for row in csv_read(csvfile):
                    writ.writerow([conv[val] if idx in shrink[fi] else val for idx, val in enumerate(row)])
        else:
            shutil.copyfile(os.path.join(in_dir, fi), os.path.join(out_dir, fi))

    # write the conversion mapping
    with open(os.path.join(out_dir, map_name), "wb") as mapfile:
        writ = csv_write(mapfile)
        for k, v in conv.items():
            writ.writerow([k, v])

def shapiro_horwitz(domain, numbr, direct, shrink):
    ctr = 0
    log_len = 0
    glob={}

    #convert each domain into its repr
    for e in domain:
        cur=ctr
        digis=[]
        while cur != 0:
            digis.append(cur%numbr)
            cur /= numbr
        glob[e] = digis
        log_len = max(log_len, len(digis))
        ctr += 1

    # pad 'leading' zeros for small indices
    for k in glob.keys():
        glob[k] = glob[k] + [0]*(log_len-len(glob[k]))

    # make a conversion
    for c in xrange(0, log_len):
        write_conversion(direct, os.path.join(direct, "shrink_sh_%d" % c), shrink, { k : str(v[c]) for k, v in glob.items()})

def discover_domain(direct, shrink):
    ret=set()
    for fi in filter(lambda n : shrink.has_key(n), os.listdir(direct)):
        with open(os.path.join(direct, fi), 'rb') as csvfile:
            for row in csv_read(csvfile):
                for idx in shrink[fi]:
                    ret.add(row[idx])
    return ret

def parse_col(m, s):
    k = s[:s.rfind(":")]
    if not m.has_key(k):
        m[k] = []
    m[k] = m[k] + [int(s[1+s.rfind(":"):])]
    return m

def init(args, usages):
    direct = "."
    size=2
    try:
        opts, args = getopt.getopt(args, "d:s:h", ["help"])
    except getopt.error, msg:
        print msg
        print "for help use --help"
        sys.exit(2)
    for o, a in opts:
        if o in ("-h", "--help"):
            print usages
            sys.exit(0)
        elif o == "-d":
            direct = a
        elif o == "-s":
            size = int(a)
    if len(args) == 0 or not reduce(lambda x, y : x and re.match("^.+:[0-9]+$", y), args, True):
        print usages
        sys.exit(1)
    return (direct, reduce(parse_col, args, {}), size)

def shrink():
    direct, shrink, size = init(sys.argv[1:], __doc__)
    dom = discover_domain(direct, shrink)
    shapiro_horwitz(dom, size, direct, shrink)

if __name__ == "__main__":
    shrink()
