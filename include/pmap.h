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
#include "BTree.h"

namespace cflr {

struct pmap : public adt<pmap, btree_set<std::pair<ident, ident>>::const_iterator>{

    typedef std::pair<ident, ident> value_type;
    typedef btree_set<value_type> tree_t;
    typedef tree_t::const_iterator iterator;

    tree_t forwards;
    tree_t backwards;

    bool empty() const {
        return forwards.empty();
    }

    void clear() {
        forwards.clear();
        backwards.clear();
    }

    void initialise_import() {} // do nothing
    void import(ident from, ident to){
        forwards.insert({from, to});
        forwards.insert({to, from});
    }
    void finalise_import() {} // do nothing

    iterator begin() const {
        return forwards.begin();
    }
    iterator end() const {
        return forwards.end();
    }

    void deep_copy(pmap& into) const {
        into.forwards = forwards;
        into.backwards = backwards;
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
    }

    template<bool TMe, bool TOther> void compose(const pmap& other, pmap& into) const{
    }

    void difference(const pmap& other){
        tree_t newf;
        tree_t newb;

        auto oi = other.forwards.begin();
        auto oe = other.forwards.end();
        for(auto i=forwards.begin(), e=forwards.end(); i!=e; ++i){
            while(oi != oe && *i > *oi) ++oi;
            if((*oi).first != (*i).first && (*oi).second != (*i).second){
                newf.insert(*i);
                newb.insert({(*i).second, (*i).first});
            }
        }

        forwards.swap(newf);
        backwards.swap(newb);
    }

    bool query(ident from, ident to){
        return forwards.find({from, to}) != forwards.end();
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
        return pmap();
    }

};

}

#endif /* __PMAP_H__ */
