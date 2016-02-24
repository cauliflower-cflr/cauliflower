// rdm_c
//
// Semi-naive method fore evaluating CFLR solutions
//
// Generated on: 24/02/2016
//           by: v0.0.1
#include <array>
#include <omp.h>
#include "pmap.h"
#include "relation.h"
namespace cflr {
struct rdm_c_semi_naive {
    // Definitions
    static const unsigned num_lbls = 5;
    static const unsigned num_domains = 1;
    typedef pmap adt_t;
    typedef std::array<relation<adt_t>, num_lbls> rels_t;
    typedef std::array<size_t, num_domains> vols_t;
    // Delta expansion rules
    static void delta_0(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[0].volume());
        cur_delta.swap_contents(deltas[0]);
        // Label 0, occurance 0, rule 4 -> 0 1 2 3
        if(!relations[1].adts[0].empty()) if(!relations[2].adts[0].empty()) if(!relations[3].adts[0].empty()) {
            auto part = cur_delta.adts[0].forwards.partition(400);
#pragma omp parallel
            {
                adt_t::tree_t::op_context r1f;
                adt_t::tree_t::op_context r2f;
                adt_t::tree_t::op_context r3f;
                adt_t::tree_t::op_context r4f;
                adt_t::tree_t::op_context r4b;
                adt_t::tree_t::op_context d4f;
                adt_t::tree_t::op_context d4b;
#pragma omp for schedule(dynamic)
                for (auto i = part.begin(); i<part.end(); ++i){
                    //std::cerr << "thread " << omp_get_thread_num() << std::endl;
                    for(const auto& r0 : *i){
                        auto r1_range = relations[1].adts[0].forwards.getBoundaries<1>({{r0[1], 0}}, r1f);
                        for(const auto& r1 : r1_range){
                            auto r2_range = relations[2].adts[0].forwards.getBoundaries<1>({{r1[1], 0}}, r2f);
                            for(const auto& r2 : r2_range){
                                auto r3_range = relations[3].adts[0].forwards.getBoundaries<1>({{r2[1], 0}}, r3f);
                                for(const auto& r3 : r3_range){
                                    adt_t::tree_t::entry_type ent({{r0[0], r3[1]}});
                                    if(!relations[4].adts[0].forwards.contains(ent, r4f)){
                                        deltas[4].adts[0].forwards.insert(ent, d4f);
                                        deltas[4].adts[0].backwards.insert({{r3[1], r0[0]}}, d4b);
                                        relations[4].adts[0].forwards.insert(ent, r4f);
                                        relations[4].adts[0].backwards.insert({{r3[1], r0[0]}}, r4b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    static void delta_1(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[1].volume());
        cur_delta.swap_contents(deltas[1]);
    }
    static void delta_2(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[2].volume());
        cur_delta.swap_contents(deltas[2]);
    }
    static void delta_3(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[3].volume());
        cur_delta.swap_contents(deltas[3]);
    }
    static void delta_4(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[4].volume());
        cur_delta.swap_contents(deltas[4]);
    }
    // Solver definition
    static void solve(vols_t& volume, rels_t& relations) {
        // Epsilon initialisation
        size_t largest_vertex_domain = 0;
        largest_vertex_domain = std::max(largest_vertex_domain, volume[0]);
        const adt_t epsilon = adt_t::identity(largest_vertex_domain);
        // Delta initialisation
        rels_t deltas {relation<adt_t>(relations[0].adts.size()), relation<adt_t>(relations[1].adts.size()), relation<adt_t>(relations[2].adts.size()), relation<adt_t>(relations[3].adts.size()), relation<adt_t>(relations[4].adts.size())};
        for(unsigned i=0; i<relations[0].adts.size(); ++i) relations[0].adts[i].deep_copy(deltas[0].adts[i]);
        for(unsigned i=0; i<relations[1].adts.size(); ++i) relations[1].adts[i].deep_copy(deltas[1].adts[i]);
        for(unsigned i=0; i<relations[2].adts.size(); ++i) relations[2].adts[i].deep_copy(deltas[2].adts[i]);
        for(unsigned i=0; i<relations[3].adts.size(); ++i) relations[3].adts[i].deep_copy(deltas[3].adts[i]);
        for(unsigned i=0; i<relations[4].adts.size(); ++i) relations[4].adts[i].deep_copy(deltas[4].adts[i]);
        // SCC [0]
        while(true) {
            if (!deltas[0].empty()) {
                delta_0(volume, relations, deltas);
                continue;
            }
            break;
        }
        // SCC [1]
        while(true) {
            if (!deltas[1].empty()) {
                delta_1(volume, relations, deltas);
                continue;
            }
            break;
        }
        // SCC [2]
        while(true) {
            if (!deltas[2].empty()) {
                delta_2(volume, relations, deltas);
                continue;
            }
            break;
        }
        // SCC [3]
        while(true) {
            if (!deltas[3].empty()) {
                delta_3(volume, relations, deltas);
                continue;
            }
            break;
        }
        // SCC [4]
        while(true) {
            if (!deltas[4].empty()) {
                delta_4(volume, relations, deltas);
                continue;
            }
            break;
        }
    }
}; // end struct rdm_c_semi_naive
} // end namespace cflr
