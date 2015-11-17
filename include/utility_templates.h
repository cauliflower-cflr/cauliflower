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

#include <type_traits>

namespace cflr {

/// ulist, a list of unsigned integers
/// utility functions like sorting/selecting/contains are defined externally,
/// hopefully this will prevent blowup
template<unsigned...Is> struct ulist {
    static const unsigned size = sizeof...(Is);
};

/// tlist, a list of type(name)s
/// like ulist, the tlist's utility functions are defined externally
template<typename...Ts> struct tlist {
    static const unsigned size = sizeof...(Ts);
};

/// index_tm, store the element/type of the list at index Idx in index_tm::result
/// workds for both ulist and tlist
template<typename, unsigned> struct index_tm;
template<unsigned Cur, unsigned...Rest, unsigned Idx> struct index_tm<ulist<Cur, Rest...>, Idx> {
    static const unsigned result = index_tm<ulist<Rest...>, Idx-1>::result;
};
template<unsigned Cur, unsigned...Rest> struct index_tm<ulist<Cur, Rest...>, 0> {
    static const unsigned result = Cur;
};
template<typename Cur, typename...Rest, unsigned Idx> struct index_tm<tlist<Cur, Rest...>, Idx> {
    typedef typename index_tm<tlist<Rest...>, Idx-1>::result result;
};
template<typename Cur, typename...Rest> struct index_tm<tlist<Cur, Rest...>, 0> {
    typedef Cur result;
};

/// tcontains_tm, true if the list contains an element with the typename
template<typename, typename> struct tcontains_tm;
template<typename T, typename...Ts, typename Con> struct tcontains_tm<tlist<T, Ts...>, Con> {
    static const bool result = std::is_same<T, Con>::value || tcontains_tm<tlist<Ts...>, Con>::result;
};
template<typename Con> struct tcontains_tm<tlist<>, Con> {
    static const bool result = false;
};

/// ucontains_tm, true if the ulist contains the element
template<typename, unsigned> struct ucontains_tm;
namespace template_internals {
    template<unsigned> struct u_t {};
}
template<unsigned...Us, unsigned Con> struct ucontains_tm<ulist<Us...>, Con> {
    static const bool result = tcontains_tm<tlist<template_internals::u_t<Us>...>, template_internals::u_t<Con>>::result;
};

/// cat_tm, concatenate two lists
template<typename, typename> struct cat_tm;
template<typename...As, typename...Bs> struct cat_tm<tlist<As...>, tlist<Bs...>> {
    typedef tlist<As..., Bs...> result;
};
template<unsigned...As, unsigned...Bs> struct cat_tm<ulist<As...>, ulist<Bs...>> {
    typedef ulist<As..., Bs...> result;
};

/// if_tm, defines ::result to be the first type, if the bool==true, otherwise the second
template<bool, typename, typename> struct if_tm;
template<typename A, typename B> struct if_tm<true, A, B> {
    typedef A result;
};
template<typename A, typename B> struct if_tm<false, A, B> {
    typedef B result;
};


/// project_tm, project a new list be selecting the elements of the first list
/// indexed by the second (a ulist)
template<typename, typename> struct project_tm;
template<typename...Ts, unsigned I, unsigned...Rest> struct project_tm<tlist<Ts...>, ulist<I, Rest...>> {
    typedef typename cat_tm<tlist<typename index_tm<tlist<Ts...>, I>::result>, typename project_tm<tlist<Ts...>, ulist<Rest...>>::result>::result result;
};
template<unsigned...Is, unsigned I, unsigned...Rest> struct project_tm<ulist<Is...>, ulist<I, Rest...>> {
    typedef typename cat_tm<ulist<index_tm<ulist<Is...>, I>::result>, typename project_tm<ulist<Is...>, ulist<Rest...>>::result>::result result;
};
template<typename...Ts> struct project_tm<tlist<Ts...>, ulist<>> {
    typedef tlist<> result; 
};
template<unsigned...Is> struct project_tm<ulist<Is...>, ulist<>> {
    typedef ulist<> result; 
};

namespace template_internals {

} // end namespace template_internals

} // end namespace cflr

#endif /* __UTILITY_TEMPLATES_H__ */
