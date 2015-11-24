/* 
 *                               semi_naive.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-19
 * 
 * Framework for solving according to the semi-naive method.
 */

#ifndef __SEMI_NAIVE_H__
#define __SEMI_NAIVE_H__

#include <array>

#include "cflr.h"
#include "relation.h"
#include "utility_templates.h"

namespace cflr {

namespace template_internals {

/// non_field_domains, find a list of all domains (lower than the unsigned)
/// which do not appear in any of the label fields
template<unsigned, typename> struct non_field_domains;
template<unsigned D, typename...Ls> struct non_field_domains<D, tlist<Ls...>> {
private:
    typedef typename cat_tm<ulist<>, Ls...>::result all_lbls; // hack to force cat_tm to return a ulist
    typedef typename non_field_domains<D-1, tlist<Ls...>>::result sub_list;
public:
    typedef typename std::conditional<!ucontains_tm<all_lbls, D-1>::result,
            typename cat_tm<sub_list, ulist<D-1>>::result,
            sub_list >::type result;
};
template<typename...Ls> struct non_field_domains<0, tlist<Ls...>> {
    typedef ulist<> result;
};

/// volume_max, utility for finding the maximum in a subset of the volume array
/// Note the result is undefined when the subset list is empty
template<unsigned, typename> struct volume_max;
template<unsigned Len, unsigned I, unsigned I2, unsigned...Rest> struct volume_max<Len, ulist<I, I2, Rest...>> {
    static inline size_t max(const std::array<size_t, Len>& vol){
        return std::max(vol[I], volume_max<Len, ulist<I2, Rest...>>::max(vol));
    }
};
template<unsigned Len, unsigned I> struct volume_max<Len, ulist<I>> {
    static inline size_t max(const std::array<size_t, Len>& vol){
        return vol[I];
    }
};

/// epsilon_union, controls unioning the epsilon relation into the necessary relations
template<typename, typename> struct epsilon_union;
template<typename A, unsigned L, unsigned...Fs, typename...Rest> struct epsilon_union<A, tlist<rule<rule_clauses::clause<L, Fs...>>, Rest...>> {
    template<typename Rel> static inline void update(const A& eps, Rel& rels) {
        for(A& a : rels[L].adts) a.union_copy(eps);
        epsilon_union<A, tlist<Rest...>>::update(eps, rels);
    }
};
template<typename A> struct epsilon_union<A, tlist<>> {
    template<typename Rel> static inline void update(const A& eps, Rel& rels) {}
};

/// delta_copy, creates the initial delta relations by deep-copying the input
template<typename A, unsigned I> struct delta_copy{
    template<typename Arr, typename...Cur> static inline Arr init(const Arr& base, Cur...cur){
        return delta_copy<A, I-1>::init(base, I-1, cur...);
    }
};
template<typename A> struct delta_copy<A, 0>{
    template<typename Arr, typename...Cur> static inline Arr init(const Arr& base, Cur...cur){
        return {relation<A>(base[cur].adts.size())...};
    }
};

/// list_dependencies, write the list of rule dependencies into a dependency_info::dep_list_t
/// trialing this evaluation mechanic, partially preprocess the front element of the tlist
/// TODO thats a horrible idea, fix it
template<typename> struct list_dependencies;
template<unsigned RHL, unsigned...RHFs, typename...RB, typename...Rest> struct list_dependencies<tlist<rule<rule_clauses::clause<RHL, RHFs...>, RB...>, Rest...>> {
    static inline void list(dependency_info::dep_list_t& lst) {
        //swaps the leading rule for a ulist<head>, ulist<>body...
        list_dependencies<tlist<ulist<RHL>, typename rule_dependencies<rule<rule_clauses::clause<RHL, RHFs...>, RB...>>::result, Rest...>>::list(lst);
    }
};
template<unsigned D, unsigned I, unsigned...Is, typename...Rest> struct list_dependencies<tlist<ulist<D>, ulist<I, Is...>, Rest...>> {
    static inline void list(dependency_info::dep_list_t& lst) {
        lst.push_back({D, I});
        list_dependencies<tlist<ulist<D>, ulist<Is...>, Rest...>>::list(lst);
    }
};
template<unsigned D, typename...Rest> struct list_dependencies<tlist<ulist<D>, ulist<>, Rest...>> {
    static inline void list(dependency_info::dep_list_t& lst) {
        list_dependencies<tlist<Rest...>>::list(lst);
    }
};
template<typename RH, typename...RB, typename...Rest> struct list_dependencies<tlist<rule<RH, RB...>, Rest...>> {
    static inline void list(dependency_info::dep_list_t& lst) {
        list_dependencies<tlist<rule_dependencies<rule<RH, RB...>>, Rest...>>::list(lst);
    }
};
template<> struct list_dependencies<tlist<>> {
    static inline void list(dependency_info::dep_list_t& lst) {}
};

/// eval_clause, return 

/// eval_body, find the solution to a compose chain, using the delta at occurance Occ
template<typename, unsigned, unsigned, int, typename...> struct eval_body;
template<typename A, unsigned Len, unsigned Lbl, int Occ, typename RCur, typename RNxt, typename...Rest> struct eval_body<A, Len, Lbl, Occ, RCur, RNxt, Rest...> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta, A& res, A& tmp1, A& tmp2){

    }
};
template<typename A, unsigned Len, unsigned Lbl, int Occ, typename RCur> struct eval_body<A, Len, Lbl, Occ, RCur> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta, A& res, A& tmp1, A& tmp2){

    }
};

/// eval_rule, main component of the rule evaluation, which creates the temporaries and
/// assigns them to the relation/delta of the rule head
/// Occ marks the specific occurance of the delta relation
/// cur_delta is only used when Occ == 0, otherwise the rel[Lbl] is used
/// TODO fields
template<typename, unsigned, unsigned, unsigned, typename> struct eval_rule;
template<typename A, unsigned Len, unsigned Lbl, unsigned Occ, unsigned HL, unsigned...HFs, typename...Bs> struct eval_rule<A, Len, Lbl, Occ, rule<rule_clauses::clause<HL, HFs...>, Bs...>> {
    typedef std::array<relation<A>, Len> rel_t;
    static void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta){ // NOT INLINE
        std::cout << "EVAL " << Lbl << " occ " << Occ << " into " << HL << std::endl;
        A res;
        A tmp1;
        A tmp2;
        eval_body<A, Len, Lbl, static_cast<int>(Occ), Bs...>::eval(rels, deltas, cur_delta, res, tmp1, tmp2);
        res.difference(rels[HL].adts[0]);
        deltas[HL].adts[0].union_copy(res);
        rels[HL].adts[0].union_absorb(res);
    }
};

/// eval_rule_occ, evaluate the result of the rule for a certain occurance of the delta
template<typename A, unsigned Len, unsigned Lbl, unsigned Occ, typename Rule> struct eval_rule_occ {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta){
        eval_rule<A, Len, Lbl, Occ-1, Rule>::eval(rels, deltas, cur_delta);
        eval_rule_occ<A, Len, Lbl, Occ-1, Rule>::eval(rels, deltas, cur_delta);
    }
};
template<typename A, unsigned Len, unsigned Lbl, typename Rule> struct eval_rule_occ<A, Len, Lbl, 0, Rule> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta) {}
};

/// eval_rules, use the delta relation Lbl to evaluate all the rules in the list
/// i.e. only provide rules dependant on Lbl to this template
template<typename, unsigned, unsigned, typename> struct eval_rules;
template<typename A, unsigned Len, unsigned Lbl, typename Rule, typename...Rest> struct eval_rules<A, Len, Lbl, tlist<Rule, Rest...>> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta){
        eval_rule_occ<A, Len, Lbl, rule_occurances<Rule, Lbl>::result, Rule>::eval(rels, deltas, cur_delta);
        eval_rules<A, Len, Lbl, tlist<Rest...>>::eval(rels, deltas, cur_delta);
    }
};
template<typename A, unsigned Len, unsigned Lbl> struct eval_rules<A, Len, Lbl, tlist<>> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline void eval(rel_t& rels, rel_t& deltas, const relation<A>& cur_delta) {}
};


/// eval_if_delta, run through each relation index in the ulist, and if the delta for that relation
/// is non-empty, evaluate it, otherwise evaluate the next rule
template<typename A, unsigned Len, typename Ls, typename Rs> struct eval_if_delta;
template<typename A, unsigned Len, unsigned L, unsigned...LRest, typename Rs> struct eval_if_delta<A, Len, ulist<L, LRest...>, Rs> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline unsigned eval(rel_t& rels, rel_t& deltas, unsigned early_exit){
        if(early_exit == L) return Len; // nothing has been used, return an index outside the range of labels
        else if(!deltas[L].empty()){
            relation<A> cur_delta(deltas[L].volume());
            cur_delta.swap_contents(deltas[L]);
            eval_rules<A, Len, L, typename problem_rules_dependant<Rs, L>::result >::eval(rels, deltas, cur_delta);
            early_exit = L;
        }
        return eval_if_delta<A, Len, ulist<LRest...>, Rs>::eval(rels, deltas, early_exit);
    }
};
template<typename A, unsigned Len, typename Rs> struct eval_if_delta<A, Len, ulist<>, Rs> {
    typedef std::array<relation<A>, Len> rel_t;
    static inline unsigned eval(rel_t& rels, rel_t& deltas, unsigned early_exit){
        return early_exit; // got to the end of the list, return the last thing we used
    }
};

} // end namespace template_internals

template<typename, unsigned, typename> struct semi_naive;
template<typename A, unsigned Vl, typename...Ts> struct semi_naive<A, Vl, problem<Ts...>> {
    typedef std::array<size_t, Vl> vol_t;
    typedef typename problem_labels<problem<Ts...>>::result lbls_t;
    typedef typename problem_rules<problem<Ts...>>::result rules_t;
    typedef typename rules_splitter<rules_t>::epsilons rules_eps_t;
    typedef typename rules_splitter<rules_t>::non_emptys rules_reg_t;
    typedef std::array<relation<A>, lbls_t::size> relations_t;
    static void solve(const vol_t& vol, relations_t& rels){
        using namespace template_internals;

        // find the domain with largest volume that is not a label volume
        const size_t largest_vertex_domain = volume_max<Vl, typename non_field_domains<Vl, lbls_t>::result>::max(vol);

        // create an epsilon relation which is used to union with all the epsilon rules
        const A eps = A::identity(largest_vertex_domain);

        // union the epsilon into all epsilon productions
        epsilon_union<A, rules_eps_t>::update(eps, rels);

        // initialise delta relations from inputs
        relations_t deltas = delta_copy<A, lbls_t::size>::init(rels);
        for(unsigned r=0; r<lbls_t::size; ++r){
            unsigned a_size = rels[r].adts.size();
            for(unsigned a=0; a<a_size; ++a){
                rels[r].adts[a].deep_copy(deltas[r].adts[a]);
            }
        }

        // find the dependency ordering
        // TODO dependencies not currently used
        //dependency_info::dep_list_t deps;
        //list_dependencies<rules_reg_t>::list(deps);
        //dependency_info::dep_res_t res = dependency_info::find_dependencies(deps);
        
        // exhaust all the deltasi
        // This loop is triggered to exit when a non-label index is returned (i.e. >= the size of the label set)
        // TODO take into account dependencies, for now this is only called once with every rule
        unsigned last_used = lbls_t::size;// none of the relations have been used, set last_used to be beyond their range
        do {
            eval_if_delta<A, lbls_t::size, typename range_tm<lbls_t::size>::result, rules_reg_t>::eval(rels, deltas, last_used);
        } while(last_used < lbls_t::size);
    }
};

} // end namespace cflr

#endif /* __SEMI_NAIVE_H__ */
