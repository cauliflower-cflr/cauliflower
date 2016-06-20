#! /usr/bin/env python

'''----------------------------------------------------------------------+ 
 |                              testvis.py                               |
 |                                                                       |
 | Author: Nic H.                                                        |
 | Date: 2015-Nov-13                                                     |
 |                                                                       |
 | A command-line visualiser for boost test cases                        |
 +----------------------------------------------------------------------'''

import sys
import os
import xml.etree.ElementTree as ET

def command(cmd):
    return os.popen(cmd).read()

def commandArr(cmd):
    return os.popen(cmd).readlines()

ESC_R = command("tput setf 4")
ESC_G = command("tput setf 2")
ESC_Y = command("tput setf 6")
ESC__ = command("tput sgr0")

def esc(p, x = False):
    return (ESC_Y if x else ESC_G) if p else ESC_R

def tcase(tc):
    (p, e, f) = (tc.attrib["assertions_passed"], tc.attrib["expected_failures"], tc.attrib["assertions_failed"])
    passed = tc.attrib["result"] == "passed"
    return (passed, "%s    %d/%d@%s%s" % (esc(passed, e != "0"), int(p), int(p)+int(f), tc.attrib["name"], ESC__))

def suite(sui):
    sub = [tcase(i) for i in sui.getchildren()]
    passed = len([x for x in sub if x[0]])
    return (passed == len(sub), ["%s  %d/%d@%s%s" % (esc(passed == len(sub)), passed, len(sub), sui.attrib["name"], ESC__)] + [x[1] for x in sub])


def omni(omn):
    sub = [suite(i) for i in omn.getchildren()]
    passed = len([x for x in sub if x[0]])
    return (["%s%d/%d@%s%s" % (esc(passed == len(sub)), passed, len(sub), omn.attrib["name"], ESC__)] + reduce(lambda x, y: x+y, [x[1] for x in sub]))

def topl(out):
    spcs = max([o.find("@") for o in out if o.strip()[0] != "!"])
    return [o[:o.find("@")] + " " + "-"*(spcs-o.find("@")) + "- " + o[o.find("@")+1:] for o in out]

def testvis():
    out = topl(omni(ET.parse(sys.stdin).find("TestSuite")))
    for l in out:
        print l

if __name__ == "__main__":
    testvis()
