/* 
 *                                problem.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 * 
 * Stores a BNF definition of the CFL-R problem to be evaluated.  its most
 * important method is solve(), which iterates its current relation set to
 * a fixpoint solution.
 */

#ifndef __PROBLEM_H__
#define __PROBLEM_H__

#include "label.h"
#include "rule.h"
#include "relation.h"

namespace cflr{

namespace template_internals {

template<typename...Ts> struct problem_helper;
template<unsigned...Fs, typename...Ts>
struct problem_helper<label<Fs...>, Ts...>{
    static const unsigned label_count = 1 + problem_helper<Ts...>::label_count;
    static const unsigned rule_count = problem_helper<Ts...>::rule_count;
    template<unsigned I, typename Adt>
    static inline void compose_delta_rule(const Adt& delta){
        problem_helper<Ts...>::template compose_delta_rule<I>(delta);
    }
};
template<unsigned L, typename...Rs, typename...Ts>
struct problem_helper<rule<L, Rs...>, Ts...>{
    static const unsigned label_count = problem_helper<Ts...>::label_count;
    static const unsigned rule_count = 1 + problem_helper<Ts...>::rule_count;
    //uses enable_if to flag rules that depend on I
    template<unsigned I, typename Adt, typename std::enable_if<rule<L, Rs...>::template depends_on<I>()>::type* = nullptr >
    static inline void compose_delta_rule(const Adt& delta){
        //using if_t = typename std::enable_if<rule<L, Rs...>::template depends_on<I>(), Adt>::type;
        std::cout << "CDR " << I << std::endl;
        problem_helper<Ts...>::template compose_delta_rule<I>(delta);
    }
    // For rules that do not depend on I
    template<unsigned I, typename Adt, typename std::enable_if<!rule<L, Rs...>::template depends_on<I>()>::type* = nullptr >
    static inline void compose_delta_rule(const Adt& delta){
        problem_helper<Ts...>::template compose_delta_rule<I>(delta);
    }
};
template<>
struct problem_helper<>{
    static const unsigned label_count = 0;
    static const unsigned rule_count = 0;
    template<unsigned I, typename Adt> static inline void compose_delta_rule(const Adt& a){}
};

} // end namespace template internals

// typedef heirarchyi
template<typename...Ts> struct problem{
    static const unsigned label_count = template_internals::problem_helper<Ts...>::label_count;
    static const unsigned rule_count = template_internals::problem_helper<Ts...>::rule_count;

    template<unsigned I, typename Adt>
    static inline void compose_delta_rule(const Adt& delta){
        template_internals
            ::problem_helper<Ts...>
            ::template compose_delta_rule<I>(delta);
    }
};
} // end namespace cflr

#endif /* __PROBLEM_H__ */
