"""
+-------------------------------------------------------------------------+
|                             countercheck.py                             |
|                                                                         |
| Author: nic                                                             |
| Date: 2017-Feb-15                                                       |
+-------------------------------------------------------------------------+
"""

__doc__ = __doc__.strip()

import random

def show_mat(m):
    for s in m:
        for t in s:
            print t,
        print

def rdm_mat(n, e):
    return rdm_mat_rect(n, n, e)

def rdm_mat_rect(n, m, e):
    ret = [[0 for i in xrange(m)] for i in xrange(n)]
    for i in xrange(e):
        ret[random.randint(0, n-1)][random.randint(0,m-1)] = 1
    return ret

def mat_mult(a, b):
    if len(a[0]) != len(b):
        raise Exception("Multiplying matrices of incorrect size: %dx%d %dx%d" % (len(a), len(a[0]), len(b), len(b[0])))
    ret = [[0 for j in xrange(len(b[0]))] for i in xrange(len(a))]
    for r in xrange(len(a)):
        for c in xrange(len(b[0])):
            for k in xrange(len(b)):
                ret[r][c] += a[r][k]*b[k][c]
    return ret

def mat_sub(a, b):
    if len(a) != len(b):
        raise Exception("Subtracting matrices of incorrect size: %dx%d %dx%d" % (len(a), len(a[0]), len(b), len(b[0])))
    if not len(a):
        return []
    if len(a[0]) != len(b[0]):
        raise Exception("Subtracting matrices of incorrect size: %dx%d %dx%d" % (len(a), len(a[0]), len(b), len(b[0])))
    return [[c - b[i][j] for j, c in enumerate(r)] for i, r in enumerate(a)]

def mat_transp(a):
    if not len(a):
        return []
    return [[a[r][c] for r in xrange(len(a))] for c in xrange(len(a[0]))]

def mat1(n, m):
    return [[1 for i in xrange(m)] for j in xrange(n)]

def vec1(n):
    return mat1(1, n)

def cardinality(a):
    tot=0
    for r in a:
        for c in r:
            tot += c
    return tot

def signum(a):
    return [[min(1, c) for c in r] for r in a]

class Adjs:
    def __init__(self, m):
        self.preds={}
        self.succs={}
        for i, r in enumerate(m):
            for j, c in enumerate(r):
                if c != 0:
                    if not self.succs.has_key(i):
                        self.succs[i] = set()
                    if not self.preds.has_key(j):
                        self.preds[j] = set()
                    self.succs[i].add(j)
                    self.preds[j].add(i)
    def all(self):
        for s in self.succs.keys():
            for t in self.succs[s]:
                yield (s, t)
    def succs_of(self, s):
        if not self.succs.has_key(s):
            return
        for t in self.succs[s]:
            yield t
    def preds_of(self, t):
        if not self.preds.has_key(t):
            return
        for s in self.preds[t]:
            yield s
    def includes(self, s, t):
        return self.succs.has_key(s) and t in self.succs[s]
    def has_src(self, s):
        return self.succs.has_key(s)
    def has_snk(self, t):
        return self.preds.has_key(t)
    def __str__(self):
        return str([x for x in self.all()])

def search_internal(links, order, current, count):
    if count == len(order):
        #print current
        return 0
    cur = order[count]
    errs = 0
    if cur-1 in order[:count]:
        if cur+1 in order[:count]:
            if links[cur].includes(current[cur], current[cur+1]):
                errs += search_internal(links, order, current, count+1)
            else:
                errs += 1
        else:
            if not links[cur].has_src(current[cur]):
                errs += 1
            for t in links[cur].succs_of(current[cur]):
                current[cur+1] = t
                errs += search_internal(links, order, current, count+1)
    else:
        if cur+1 in order[:count]:
            if not links[cur].has_snk(current[cur+1]):
                errs += 1
            for s in links[cur].preds_of(current[cur+1]):
                current[cur] = s
                errs += search_internal(links, order, current, count+1)
        else:
            for (s, t) in links[cur].all():
                current[cur] = s
                current[cur+1] = t
                errs += search_internal(links, order, current, count+1)
    return errs

def search(links, order):
    return search_internal(links, order, [-1 for i in xrange(len(links)+1)], 0)

def chains(mats, known):
    ret = []
    for i in known:
        if ret:
            (f, t, m) = ret[-1]
            if t == i-1:
                ret[-1] = (f, i, mat_mult(m, mats[i]))
            else:
                ret.append((i, i, mats[i]))
        else:
            ret.append((i, i, mats[i]))
    return ret

def found(mats, known, nxt):
    cns = chains(mats, known)
    bias = 1
    pre = None
    pos = None
    v1 = mat1(1, len(mats[0]))
    v1t = mat1(len(mats[0]), 1)
    for (f, t, m) in cns:
        if f == nxt+1:
            pos = (f, t, m)
        elif t == nxt-1:
            pre = (f, t, m)
        else:
            bias *= cardinality(m)
    de = [[0]]
    if not pre and not pos:
        de = [[0]]
    elif not pre:
        (l, u, m) = pos
        de = mat_mult(mat_sub(v1, signum(mat_mult(v1, mats[nxt]))), mat_mult(m, v1t))
    elif not pos:
        (l, u, m) = pre
        de = mat_mult(mat_mult(v1, m), mat_sub(v1t, signum(mat_mult(mats[nxt], v1t))))
    else:
        (lb, ub, bef) = pre
        (la, ua, aft) = pos
        de = mat_mult(
                v1,
                mat_mult(
                    bef,
                    mat_mult(
                        mat_sub(mat1(len(v1t), len(v1t)), mats[nxt]),
                        mat_mult(
                            aft,
                            v1t
                            )
                        )
                    )
                )
    return de[0][0]*bias

def cost(mats, order):
    return sum([found(mats, sorted(order[:i]), x) for i, x in enumerate(order)])

def permutations(num, cur=[]):
    for i in xrange(num):
        if i not in cur:
            tmp = cur + [i]
            if(len(tmp) == num):
                yield tmp
            else:
                for p in permutations(num, tmp):
                    yield p

m1 = rdm_mat_rect(2, 3, 4)
# m2 = rdm_mat_rect(3, 2, 4)
show_mat(m1)
print
show_mat(mat_sub(mat1(2, 3), m1))
print
show_mat(vec1(2))
print
show_mat(mat_mult(vec1(2), m1))
print
print cardinality(m1)
# print Adjs(m1)
# show_mat(m2)
# print Adjs(m2)
# print
# show_mat(mat_mult(m1, m2))
# show_mat(signum(mat_mult(m1, m2)))
# print
# show_mat(mat_mult(m2, m1))
# show_mat(signum(mat_mult(m2, m1)))

mats = [rdm_mat(10, e) for e in xrange(10, 20, 3)]
# mats = [rdm_mat(3, 0) for e in xrange(20, 62, 20)]
# mats[0][0][1] = 1
# mats[0][0][0] = 1
# mats[1][1][2] = 1
# mats[1][1][1] = 1
# mats[1][0][0] = 1
# mats[1][0][1] = 1
# mats[2][2][0] = 1
adjs = [Adjs(m) for m in mats]
for a in adjs:
    print str(a)
    print

for p in permutations(len(adjs)):
    print p
    print search(adjs, p)
    print cost(mats, p)

