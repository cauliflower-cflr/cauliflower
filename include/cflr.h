/* 
 *                                  cflr.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-17
 * 
 * Template definitions and structs for a CFL-R problem.  Definitions of
 * label, rule, and problem, with minimal duplication (hopefully).
 * Namespace ::template_internals is used to hide some of the messier details
 * of meta-programming from the user
 */

#ifndef __CFLR_H__
#define __CFLR_H__

#include "utility_templates.h"

namespace cflr {

/// label, used to identify the field domains of a terminal/nonterminal label
template<unsigned...Fs> using label = ulist<Fs...>;

/// rule, mapping a label index to the rule that evaluates that label
/// this structure uses indices to avoid duplicating label information, where
/// an index refers to the offset amongst the labels in the problem
/// The valid components for the body are defined in namespace: cflr::rule_clauses
template<typename, typename...> struct rule {};

/// problem, collecting the labels and rules relevant to evaluate a 
/// CFL-R solution.  The labels and rules do not need to be grouped,
/// that is performed by an auxiliary structure
template<typename...Ts> using problem = tlist<Ts...>;

namespace rule_clauses {

/// fwd, chains a label in the forwards direction
template<unsigned, unsigned...> struct clause {};

/// rev, chains a clause in the reverse direction
template<typename> struct rev {};

/// neg, chains a negated clause in either direction
template<typename> struct neg {};

/// ist, chains the intersection of two clauses in either direction
template<typename, typename> struct ist {};

} // end namespace rule_clauses

/// problem_rules, group the rules defined by a problem into a tlist
template<typename> struct problem_rules;
namespace template_internals {
    template<typename, typename...> struct problem_rules_h;
    template<typename RG, unsigned...LFs, typename...Ts> struct problem_rules_h<RG, label<LFs...>, Ts...>{
        typedef typename problem_rules_h<RG, Ts...>::result result;
    };
    template<typename...Rules, typename L, typename...Rs, typename...Ts> struct problem_rules_h<tlist<Rules...>, rule<L, Rs...>, Ts...> {
        typedef typename problem_rules_h<tlist<Rules..., rule<L, Rs...>>, Ts...>::result result;
    };
    template<typename RG> struct problem_rules_h<RG> {
        typedef RG result;
    };
}
template<typename...Ts> struct problem_rules<problem<Ts...>>{
    typedef typename template_internals::problem_rules_h<tlist<>, Ts...>::result result;
};

/// problem_labels, group the labels defined by a problem into a tlist
template<typename> struct problem_labels;
namespace template_internals {
    template<typename, typename...> struct problem_labels_h;
    template<typename...Labels, unsigned...LFs, typename...Ts> struct problem_labels_h<tlist<Labels...>, label<LFs...>, Ts...>{
        typedef typename problem_labels_h<tlist<Labels..., label<LFs...>>, Ts...>::result result;
    };
    template<typename Cur, typename L, typename...Rs, typename...Ts> struct problem_labels_h<Cur, rule<L, Rs...>, Ts...> {
        typedef typename problem_labels_h<Cur, Ts...>::result result;
    };
    template<typename RG> struct problem_labels_h<RG> {
        typedef RG result;
    };
}
template<typename...Ts> struct problem_labels<problem<Ts...>>{
    typedef typename template_internals::problem_labels_h<tlist<>, Ts...>::result result;
};

/// rule_dependencies, group the dependencies of a rule into a ulist
template<typename> struct rule_dependencies;
namespace template_internals {
    template<typename, typename...> struct rule_dependencies_h;
    template<unsigned...Cur, unsigned R, unsigned...RFs, typename...Rest> struct rule_dependencies_h<ulist<Cur...>, rule_clauses::clause<R, RFs...>, Rest...>{
        typedef typename rule_dependencies_h<ulist<Cur..., R>, Rest...>::result result;
    };
    template<unsigned...Cur> struct rule_dependencies_h<ulist<Cur...>> {
        typedef ulist<Cur...> result;
    };
}
template<typename L, typename...Rs> struct rule_dependencies<rule<L, Rs...>>{
    typedef typename template_internals::rule_dependencies_h<ulist<>, Rs...>::result result;
};

/// problem_rules_dependant, get the subset of rules in a problem which are dependant
/// on the index label
template<typename, unsigned> struct problem_rules_dependant;
namespace template_internals {
    template<typename, unsigned> struct problem_rules_dependant_h;
    template<typename Rule, typename...Rest, unsigned Idx> struct problem_rules_dependant_h<tlist<Rule, Rest...>, Idx> {
    private:
        typedef typename problem_rules_dependant_h<tlist<Rest...>, Idx>::result sub_result;
    public:
        typedef typename std::conditional<
            ucontains_tm<typename rule_dependencies<Rule>::result, Idx>::result,
            typename cat_tm<tlist<Rule>, sub_result>::result,
            sub_result
            >::type result;
    };
    template<unsigned Idx> struct problem_rules_dependant_h<tlist<>, Idx> {
        typedef tlist<> result;
    };
}
template<typename Prob, unsigned Idx> struct problem_rules_dependant {
    typedef typename template_internals
        ::problem_rules_dependant_h<typename problem_rules<Prob>::result, Idx>
        ::result result;
};

} // end namespace cflr

#endif /* __CFLR_H__ */
