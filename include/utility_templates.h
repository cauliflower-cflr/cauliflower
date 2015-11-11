/* 
 *                           utility_templates.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-11
 * 
 * Meta-programming utilities.
 */

#ifndef __UTILITY_TEMPLATES_H__
#define __UTILITY_TEMPLATES_H__

#include <tuple>

namespace cflr {

namespace template_internals {

template<template<typename> class App, typename Tup, typename...Tys> struct t_apply_ptr_help;
template<template<typename> class App, typename...Cur, typename New, typename...Rest>
struct t_apply_ptr_help<App, std::tuple<Cur...>, New, Rest...>{
    typedef typename t_apply_ptr_help<App, std::tuple<Cur..., App<New>*>, Rest...>::value value;
};
template<template<typename> class App, typename...Cur>
struct t_apply_ptr_help<App, std::tuple<Cur...>>{
    typedef std::tuple<Cur...> value;
};

} // end namespace template_internals

// Stores as its value a typedef for a tuple of A<Ts*> for all of Ts...
template<template<typename> class A, typename...Ts>
struct t_apply_ptr{
    typedef typename template_internals::t_apply_ptr_help<A, std::tuple<>, Ts...>::value value;
};

} // end namespace cflr

#endif /* __UTILITY_TEMPLATES_H__ */
