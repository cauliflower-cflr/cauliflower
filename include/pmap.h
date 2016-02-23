/* 
 *                                  pmap.h
 * 
 * Author: Nic H.
 * Date: 2016-Feb-09
 * 
 * wrapper to allow us to use souffle's BTree as a relation in cauliflower
 */

#ifndef __PMAP_H__
#define __PMAP_H__

#include "adt.h"
#include "Trie.h"

namespace cflr {

struct pmap;

struct pmap_iterator{
    typedef Trie<2>::iterator ty;
    ty internal;
    std::pair<ident, ident> cur;
    pmap_iterator(ty inter) : internal(inter) {}
    pmap_iterator operator++(){ return ++internal; }
    bool operator==(const pmap_iterator& other){ return other.internal == internal; }
    bool operator!=(const pmap_iterator& other){ return other.internal != internal; }
    const std::pair<ident, ident>* operator->() {
        cur = {(*internal)[0], (*internal)[1]};
        return &cur;
    }
};

namespace template_internals {

/// Helper template to perform intersection when transpose-configuration is known statically
template<bool, bool> inline void compose_h(const pmap&, const pmap&, pmap&);

/// Helper template to perform intersection when transpose-configuration is known statically
template<bool, bool> inline void intersect_h(const pmap&, const pmap&, pmap&);

} // end namespace template_internals

struct pmap : public adt<pmap, pmap_iterator>{

    typedef std::pair<ident, ident> value_type;
    typedef Trie<2> tree_t;
    typedef pmap_iterator iterator;

    tree_t forwards;
    tree_t backwards;

    bool empty() const {
        return forwards.empty();
    }

    size_t size() const {
        return forwards.size();
    }

    void clear() {
        forwards.clear();
        backwards.clear();
    }

    void initialise_import() {} // do nothing
    void import(ident from, ident to){
        forwards.insert({{from, to}});
        backwards.insert({{to,  from}});
    }
    void finalise_import() {} // do nothing

    iterator begin() const {
        return pmap_iterator(forwards.begin());
    }
    iterator end() const {
        return pmap_iterator(forwards.end());
    }

    void deep_copy(pmap& into) const {
        into.forwards.insertAll(forwards);
        into.backwards.insertAll(backwards);
    }

    void union_copy(const pmap& other) {
        forwards.insertAll(other.forwards);
        backwards.insertAll(other.backwards);
    }

    void union_absorb(pmap& other) {
        forwards.insertAll(other.forwards);
        backwards.insertAll(other.backwards);
    }

    template<bool TMe, bool TOther> void intersect(const pmap& other, pmap& into) const{
        template_internals::intersect_h<TMe, TOther>(*this, other, into);
    }

    template<bool TMe, bool TOther> void compose(const pmap& other, pmap& into) const{
        template_internals::compose_h<TMe, TOther>(*this, other, into);
    }

    void difference(const pmap& other){
        tree_t newf;
        tree_t newb;

        for(auto i=forwards.begin(), e=forwards.end(); i!=e; ++i){
            if(!other.forwards.contains(*i)){
                newf.insert(*i);
                newb.insert({{i->data[1], i->data[0]}});
            }
        }

        std::swap(forwards, newf);
        std::swap(backwards, newb);
    }

    bool query(ident from, ident to){
        return forwards.contains({{from, to}});
    }

    void dump(std::ostream& os) const {
    }

    static pmap identity(unsigned max_ident){
        pmap ret;
        ret.initialise_import();
        for(ident i=0; i<max_ident; i++){
            ret.import(i, i);
        }
        ret.finalise_import();
        return ret;
    }

};

namespace template_internals {

template<> inline void intersect_h<false, false>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    for(const auto& m : me.forwards){
        if(other.forwards.contains(m)){ // TODO use pair of iterators
            into.import(m[0], m[1]);
        }
    }
    into.finalise_import();
}
template<> inline void intersect_h<false, true>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    for(const auto& m : me.forwards){
        if(other.backwards.contains(m)){ // TODO use pair of iterators
            into.import(m[0], m[1]);
        }
    }
    into.finalise_import();
}
template<> inline void intersect_h<true, false>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    for(const auto& m : me.backwards){
        if(other.forwards.contains(m)){ // TODO use pair of iterators
            into.import(m[0], m[1]);
        }
    }
    into.finalise_import();
}
template<> inline void intersect_h<true, true>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    for(const auto& m : me.backwards){
        if(other.backwards.contains(m)){ // TODO use pair of iterators
            into.import(m[0], m[1]);
        }
    }
    into.finalise_import();
}

template<> inline void compose_h<false, false>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    auto parts = me.backwards.partition(400); // because random number thats why!
#pragma omp parallel for schedule(auto)
    for(unsigned i=0; i<parts.size(); ++i){
        for(const auto& m : parts[i]){
            for(const auto& o : other.forwards.getBoundaries<1>(m)){
                into.import(m[1], o[1]);
            }
        }
    }
    into.finalise_import();
}
template<> inline void compose_h<false, true>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    auto parts = me.backwards.partition(400); // because random number thats why!
#pragma omp parallel for schedule(auto)
    for(unsigned i=0; i<parts.size(); ++i){
        for(const auto& m : parts[i]){
            for(const auto& o : other.backwards.getBoundaries<1>(m)){
                into.import(m[1], o[1]);
            }
        }
    }
    into.finalise_import();
}
template<> inline void compose_h<true, false>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    auto parts = me.forwards.partition(400); // because random number thats why!
#pragma omp parallel for schedule(auto)
    for(unsigned i=0; i<parts.size(); ++i){
        for(const auto& m : parts[i]){
            for(const auto& o : other.forwards.getBoundaries<1>(m)){
                into.import(m[1], o[1]);
            }
        }
    }
    into.finalise_import();
}
template<> inline void compose_h<true, true>(const pmap& me, const pmap& other, pmap& into){
    into.initialise_import();
    auto parts = me.forwards.partition(400); // because random number thats why!
#pragma omp parallel for schedule(auto)
    for(unsigned i=0; i<parts.size(); ++i){
        for(const auto& m : parts[i]){
            for(const auto& o : other.backwards.getBoundaries<1>(m)){
                into.import(m[1], o[1]);
            }
        }
    }
    into.finalise_import();
}

} // end namespace template_internals

}

#endif /* __PMAP_H__ */
