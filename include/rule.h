/* 
 *                                  rule.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-13
 * 
 * Meta-templates for the rule.  Note how this rule forces us to be in IN-
 * nf (i.e. intersection and negation clauses appear alone).
 */

#ifndef __RULE_H__
#define __RULE_H__

#include "label.h"

namespace cflr{

//the significand templates
template<unsigned, unsigned> struct isect {};
template<unsigned> struct ngate {};
template<unsigned> struct rev {};
template<unsigned> struct fwd {};
//rule base type

namespace template_internals {

template <unsigned...> struct  rule_deps {};
template<typename Rd, typename...Ds> struct rule_dep_helper;
template<unsigned...Rds, unsigned I, typename...Ds>
struct rule_dep_helper<rule_deps<Rds...>, fwd<I>, Ds...> {
    typedef typename rule_dep_helper<template_internals::rule_deps<Rds..., I>, Ds...>::dependencies dependencies;
};
template<unsigned...Rds>
struct rule_dep_helper<rule_deps<Rds...>> {
    typedef rule_deps<Rds...> dependencies;
};

} // end namespace template_internals

template<unsigned L, typename...Rs> struct rule {
    typedef typename template_internals::rule_dep_helper<template_internals::rule_deps<>, Rs...>::dependencies dependencies;
};


// -// epsilon rule
// -template<unsigned LI, unsigned...LFs>
// -struct rule<label<LI, LFs...>> {
// -};
// -// sequence rule
// -template<unsigned LI, unsigned...LFs, unsigned R1I, unsigned...R1Fs, typename...Rest>
// -struct rule<label<LI, LFs...>, label<R1I, R1Fs...>, Rest...> {
// -};
// -// intersection rule
// -template<unsigned LI, unsigned...LFs, unsigned RAI, unsigned...RAFs, unsigned RBI, unsigned...RBFs>
// -struct rule<label<LI, LFs...>, isect<label<RAI, RAFs...>, label<RBI, RBFs...>>> {
// -};
// -// negation rule
// -template<unsigned LI, unsigned...LFs, unsigned RI, unsigned...RFs>
// -struct rule<label<LI, LFs...>, ngate<label<RI, RFs...>>> {
// -};
// -// transitive-rule (Future Work)

} // end namespace cflr

#endif /* __RULE_H__ */
