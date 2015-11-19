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

template<typename, typename> struct semi_naive;
template<typename A, typename...Ts> struct semi_naive<A, problem<Ts...>> {
    typedef A adt_t;
    typedef std::array<relation<A>, problem_labels<problem<Ts...>>::result::size> relations_t;
    template<typename Vol> static void solve(const Vol& vol, relations_t& rels){
    }
};

} // end namespace cflr

#endif /* __SEMI_NAIVE_H__ */
