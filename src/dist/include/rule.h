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

#include <iostream> // TODO remove me

#include "label.h"

namespace cflr{

template<unsigned, unsigned> struct isect {};
template<unsigned> struct ngate {};
template<unsigned> struct rev {};
template<unsigned> struct fwd {};

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

template <unsigned E, typename C> struct iterative;
template<unsigned E, unsigned I, unsigned...Rest>
struct iterative<E, rule_deps<I, Rest...>>{
    // potentially inefficient because no early-exit
    static const bool contains = E == I || iterative<E, rule_deps<Rest...>>::contains;
};
template<unsigned E>
struct iterative<E, rule_deps<>>{
    static const bool contains = false;
};

template<bool, unsigned, typename, unsigned, typename...> struct evaluate_helper;
template<unsigned I, typename Adt, unsigned L, typename...Rs>
struct evaluate_helper<false, I, Adt, L, Rs...>{
    static inline void evaluate(const Adt& delta) {}
};
template<unsigned I, typename Adt, unsigned L, typename...Rs>
struct evaluate_helper<true, I, Adt, L, Rs...>{
    static inline void evaluate(const Adt& delta) {
        std::cout << "CDR " << I << std::endl;
    }
};

} // end namespace template_internals

template<unsigned L, typename...Rs> struct rule {
    typedef typename template_internals::rule_dep_helper<template_internals::rule_deps<>, Rs...>::dependencies dependencies;
    template<unsigned I>
    static inline constexpr bool depends_on(){
        return template_internals::iterative<I, typename rule<L, Rs...>::dependencies>::contains;
    }
    template<unsigned I, typename Adt>
    static inline void evaluate_delta(const Adt& delta){
        template_internals::evaluate_helper<depends_on<I>(), I, Adt, L, Rs...>::evaluate(delta);
    }
};

} // end namespace cflr

#endif /* __RULE_H__ */
