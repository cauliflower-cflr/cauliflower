/* 
 *                                relation.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 * 
 * Interface to the group of ADTs that make up a relation.  Composition of
 * relations is defined for the whole relation, therefore the composition
 * would occur if any relation is non-empty
 */

#ifndef __RELATION_H__
#define __RELATION_H__

#include <iostream>
#include <vector>

#include"relation_buffer.h"

namespace cflr{

template<typename A>
struct relation {

    std::vector<A> adts;

    relation() : adts() {}
    relation(unsigned fv) : adts(fv, A()) {}

    template<typename...Ts>
    void import_buffer(const relation_buffer<Ts...>& buf){
        // assert(adts.size() == buf.field_volumne());
        for(auto& a : adts) a.initialise_import();
        unsigned size = buf.size();
        for(unsigned i=0; i<size; i++){
            adts[buf.index_volume(i)].import(buf[i][0], buf[i][1]);
        }
        for(auto& a : adts) a.finalise_import();
    }

    template<typename...Ts>
    void export_buffer(relation_buffer<Ts...>& buf) const {
        unsigned vol = 0;
        for(const A& adt : adts){
            auto i = adt.begin();
            auto e = adt.end();
            for(; i!=e; ++i){
                buf.add_internal(i->first, i->second, vol);
            }
            ++vol;
        }
    }

    void dump(std::ostream& os) const {
        unsigned idx=0;
        for(const auto& a : adts){
            os << idx << ":" << std::endl;
            a.dump(os);
            ++idx;
        }
    }

};

} // end namespace cflr

#endif /* __RELATION_H__ */
