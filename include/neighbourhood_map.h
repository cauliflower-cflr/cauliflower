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
        } else {
            cur = {m_cur->first, *s_cur};
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

    void deep_copy(neighbourhood_map<M, S>& into) const {
        //TODO use std::copy
        auto e = end();
        for(auto i = begin(); i != e; ++i){
            into.import(i->first, i->second);
        }
    }

    void union_copy(const neighbourhood_map<M, S>& other){
        for(const auto& m : other.forwards){
            for(const auto& s : m.second){
                import(m.first, s);
            }
        }
    }
    void union_absorb(neighbourhood_map<M, S>& other){
        union_copy(other);
    }

    void compose(const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into) const{
        auto end = other.forwards.end();
        for(const auto& pvt : backwards){
            auto oth = other.forwards.find(pvt.first);
            if (oth != end) {
                for(ident from : pvt.second){
                    for(ident to : oth->second){
                        into.import(from, to);
                    }
                }
            }
        }
    }

    void difference(const neighbourhood_map<M, S>& other){
        auto end = forwards.end();
        for(const auto& oth : other.forwards){
            auto ms = forwards.find(oth.first);
            if(ms != end){
                for(ident to : oth.second){
                    auto fnd = ms->second.find(to);
                    if(fnd != ms->second.end()){
                        auto bck = backwards.find(to);
                        ms->second.erase(fnd);
                        bck->second.erase(oth.first);
                        if(ms->second.empty()) forwards.erase(ms);
                        if(bck->second.empty()) backwards.erase(bck);
                    }
                }
            }
        }
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

    static neighbourhood_map<M, S> identity(unsigned max_ident){
        neighbourhood_map<M, S> ret;
        for(ident i=0; i<max_ident; i++){
            ret.forwards.insert({i, S({i})});
        }
        return ret;
    }

};

} // end namespace cflr

#endif /* __NEIGHBOURHOOD_MAP_H__ */
