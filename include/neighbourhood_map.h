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

namespace template_internals {
template<typename, typename, bool, bool> struct compose_h;
template<typename M, typename S> struct compose_h<M, S, false, false>{
    static inline void compose(const neighbourhood_map<M, S>& me, const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into){
        auto end = other.forwards.end();
        for(const auto& pvt : me.backwards){
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
};
template<typename M, typename S> struct compose_h<M, S, false, true>{
    static inline void compose(const neighbourhood_map<M, S>& me, const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into){
        auto end = other.backwards.end();
        for(const auto& pvt : me.backwards){
            auto oth = other.backwards.find(pvt.first);
            if (oth != end) {
                for(ident from : pvt.second){
                    for(ident to : oth->second){
                        into.import(from, to);
                    }
                }
            }
        }
    }
};
template<typename M, typename S> struct compose_h<M, S, true, false>{
    static inline void compose(const neighbourhood_map<M, S>& me, const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into){
        auto end = other.forwards.end();
        for(const auto& pvt : me.forwards){
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
};
template<typename M, typename S> struct compose_h<M, S, true, true>{
    static inline void compose(const neighbourhood_map<M, S>& me, const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into){
        auto end = other.backwards.end();
        for(const auto& pvt : me.forwards){
            auto oth = other.backwards.find(pvt.first);
            if (oth != end) {
                for(ident from : pvt.second){
                    for(ident to : oth->second){
                        into.import(from, to);
                    }
                }
            }
        }
    }
};
} // end namespace template_internals

template<typename M, typename S>
struct neighbourhood_map : public adt<neighbourhood_map<M, S>, neighbourhood_iterator<M, S>>{

    //typedef std::set<ident> S;
    //typedef std::map<ident, S> M;
    typedef neighbourhood_iterator<M, S> iterator;

    // Attributes
    M forwards;
    M backwards;

    bool empty() const {
        return forwards.empty();
    }

    void clear() {
        forwards.clear();
        backwards.clear();
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

    template<bool TMe, bool TOther> void compose(const neighbourhood_map<M, S>& other, neighbourhood_map<M, S>& into) const{
        template_internals::compose_h<M, S, TMe, TOther>::compose(*this, other, into);
    }

    void difference(const neighbourhood_map<M, S>& other){
        // auto end = forwards.end();
        // for(const auto& oth : other.forwards){
        //     auto ms = forwards.find(oth.first);
        //     if(ms != end){
        //         for(ident to : oth.second){
        //             auto fnd = ms->second.find(to);
        //             if(fnd != ms->second.end()){
        //                 auto bck = backwards.find(to);
        //                 ms->second.erase(fnd);
        //                 bck->second.erase(oth.first);
        //                 if(ms->second.empty()) forwards.erase(ms);
        //                 if(bck->second.empty()) backwards.erase(bck);
        //             }
        //         }
        //     }
        // }
        for(typename M::iterator lit = forwards.begin(); lit != forwards.end(); ++lit){
            std::vector<typename M::key_type> rms;
            for(typename S::const_iterator rit = lit->second.begin(); rit != lit->second.end(); ++rit){
                typename M::const_iterator f = other.forwards.find(lit->first);
                if(f != other.forwards.end()){
                    if(f->second.find(*rit) != f->second.end()){
                        backwards[*rit].erase(lit->first);
                        rms.push_back(*rit);
                    }
                }
            }
            for(auto id : rms){
                lit->second.erase(id);
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
            std::pair<typename M::iterator, bool> ins = ret.forwards.insert({i, S()});
            ins.first->second.insert(i);
        }
        return ret;
    }

};

} // end namespace cflr

#endif /* __NEIGHBOURHOOD_MAP_H__ */
