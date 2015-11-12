/* 
 *                           neighbourhood_map.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-12
 * 
 * Templated neighbourhood-map implementation
 */

#ifndef __NEIGHBOURHOOD_MAP_H__
#define __NEIGHBOURHOOD_MAP_H__

#include <iostream>
// #include <map>
// #include <set>

#include "adt.h"

namespace cflr {

template<typename M, typename S> struct neighbourhood_map;

template<typename M, typename S>
struct neighbourhood_iterator{

    typedef std::pair<ident, ident> value_type;

    const M* nmap;
    typename M::const_iterator m_end, m_cur;
    typename S::const_iterator s_cur;
    value_type cur;
    neighbourhood_iterator() : nmap(nullptr) {}
    neighbourhood_iterator(const M* m) : nmap(m), m_end(m->end()), m_cur(m->begin()) {
        advance();
    }
    bool operator==(const neighbourhood_iterator<M, S>& other) const {
        if(nmap == nullptr) return other.nmap == nullptr;
        return nmap == other.nmap && m_cur == other.m_cur && s_cur == other.s_cur;
    }
    bool operator!=(const neighbourhood_iterator<M, S>& other) const {
        return !(other == *this);
    }
    value_type operator*() const {
        return cur;
    }
    const value_type* operator->() const {
        return &cur;
    }
    neighbourhood_iterator<M, S>& operator++(){
        s_cur++;
        if(s_cur == m_cur->second.end()){
            m_cur++;
            advance();
        }
        return *this;
    }
    private:
    void advance(){
        if(m_cur == m_end){
            nmap = nullptr;
        } else {
            s_cur = m_cur->second.begin();
            cur = {m_cur->first, *s_cur};
        }
    }
};

template<typename M, typename S>
struct neighbourhood_map : public adt<neighbourhood_map<M, S>, neighbourhood_iterator<M, S>>{

    //typedef std::set<ident> S;
    //typedef std::map<ident, S> M;
    typedef neighbourhood_iterator<M, S> iterator;

    // Attributes
    M forwards;
    M backwards;

    bool empty(){
        return forwards.empty();
    }

    void initialise_import() {} // do nothing
    void import(ident from, ident to){
        if(forwards.find(from) == forwards.end()) forwards.insert({from, S()});
        forwards[from].insert(to);
        if(backwards.find(to) == backwards.end()) backwards.insert({to, S()});
        backwards[to].insert(from);
    }
    void finalise_import() {} // do nothing

    iterator begin() const {
        return iterator(&forwards);
    }
    iterator end() const {
        return iterator();
    }

    void dump(std::ostream& os) const {
        for(const auto& m : forwards){
            os << m.first << " ->";
            for(const auto& s : m.second){
                os << " " << s;
            }
            os << std::endl;
        }
    }

};

} // end namespace cflr

#endif /* __NEIGHBOURHOOD_MAP_H__ */
