"""
+-------------------------------------------------------------------------+
|                               convert.py                                |
|                                                                         |
| Author: Nic H.                                                          |
| Date: 2016-Oct-29                                                       |
+-------------------------------------------------------------------------+
"""

__doc__ = __doc__.strip()

import sys
import getopt
import os

def ngpo2(x):
    return 2**(x-1).bit_length()

class bddb():
    def getName(self, nm):
        return nm + ".datalog"
    def printLine(self, fil, name, line):
        fil.write(name + "(\"" + "\",\"".join(line) + "\").\n")
    def useAtoms(self, atoms, od, pat):
        with open(os.path.join(od, "sizes.datalog"), "w") as domainFile:
            for (k,v) in atoms.items():
                domainFile.write("%s %d %s.map\n" % (k, ngpo2(len(v)), k))
                with open(os.path.join(od, k + ".map"), "w") as atomList:
                    for a in v:
                        atomList.write(a + "\n")

class fact(bddb):
    def getName(self, nm):
        return nm + ".facts"
    def printLine(self, fil, name, line):
        fil.write("\t".join(line) + "\n")
    def useAtoms(self, atoms, od, pat):
        with open(os.path.join(od, "sizes.dl"), "w") as sizesFile, open(os.path.join(od, "facts.import"), "w") as importFile:
            for (k, v) in atoms.items():
                sizesFile.write("lang:physical:capacity[`%sDomain] = %d.\n" % (k, ngpo2(len(v))))
            for k in atoms.keys():
                sizesFile.write("%sDomain(?x), %sDomain:Value(?x:?s) -> string(?s).\n" % (k, k))
            importFile.write("option,delimiter,\"\t\"\n")
            importFile.write("option,hasColumnNames,false\n")
            for p in pat:
                grp = p.split(":")
                name = grp[0]
                domains = grp[1:]
                idxes=[str(i) for (i,e) in enumerate(domains)]
                dsets = ["%sDomain(v%d)" % (e, i) for (i,e) in enumerate(domains)]
                sizesFile.write("%s(%s) -> %s.\n" % (name, ",".join(["v%d"%i for (i,e) in enumerate(domains)]), ",".join(dsets)))
                froms="".join([",column:%s,%s:%s" % (i,name,i) for i in idxes])
                tos="".join([",%s:%s" % (name,i) for i in idxes])
                importFile.write("fromFile,\"%s\"%s\n" % (os.path.join(os.path.abspath(od), self.getName(name)), froms))
                importFile.write("toPredicate,%s%s\n" % (name, tos))


def go(directory, patterns, outer, outputDir):
    atoms={}
    for p in patterns:
        grp = p.split(":")
        domains = grp[1:]
        for d in domains:
            if not atoms.has_key(d):
                atoms[d] = set([])
        with open(os.path.join(directory, grp[0] + ".csv"), "r") as csvFile, open(os.path.join(outputDir, outer.getName(grp[0])), "w") as outFile:
            for ln in csvFile.readlines():
                lin = ln[1:-2].split("\",\"")
                for (i, e) in enumerate(lin):
                    atoms[domains[i]].add(e)
                outer.printLine(outFile, grp[0], lin)
    outer.useAtoms(atoms, outputDir, patterns)

def convert():
    patterns=["Alloc:V:H", "Assign:V:V", "Load:V:V:F", "Store:V:V:F"]
    output="."
    conv=bddb()
    try:
        opts, args = getopt.getopt(sys.argv[1:], "fh", ["help"])
    except getopt.error, msg:
        print msg
        print "for help use --help"
        sys.exit(2)
    for o, a in opts:
        if o in ("-h", "--help"):
            print __doc__
            sys.exit(0)
        if o == "-f":
            conv=fact()
    go(args[0], patterns, conv, output)

if __name__ == "__main__":
    convert()
