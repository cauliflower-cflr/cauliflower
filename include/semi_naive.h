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

} // end namespace template_internals

template<typename, unsigned, typename> struct semi_naive;
template<typename A, unsigned Vl, typename...Ts> struct semi_naive<A, Vl, problem<Ts...>> {
    typedef A adt_t;
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
        relations_t delta = delta_copy<A, lbls_t::size>::init(rels);
        for(auto& da : delta){
            std::cout << " []" << da.adts.size() << std::endl;
        }
    }
};

} // end namespace cflr

#endif /* __SEMI_NAIVE_H__ */
