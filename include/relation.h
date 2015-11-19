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

#include "relation_buffer.h"
#include "utility_templates.h"

namespace cflr{

/// relation_volume_index, given two arrays, the first is the volumes of the identifiers
/// (i.e. the number of identifiers in each domain), the second is the current identifiers
/// of interest, compute the index into an ADT array of a relation characterised by the
/// domains listed in the ulist: parameter 1
template<typename, unsigned> struct relation_volume_index;
template<unsigned I, unsigned...Rest, unsigned Len> struct relation_volume_index<ulist<I, Rest...>, Len> {
    static inline size_t index(const std::array<size_t, Len>& volume, const std::array<size_t, Len>& cur){
        return relation_volume_index<ulist<Rest...>, Len>::index(volume, cur)*volume[I] + cur[I];
    }
};
template<unsigned Len> struct relation_volume_index<ulist<>, Len> {
    static inline size_t index(const std::array<size_t, Len>& volume, const std::array<size_t, Len>& cur){
        return 0;
    }
};


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
