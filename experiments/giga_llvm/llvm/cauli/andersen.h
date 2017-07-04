// andersen
//
// Semi-naive method fore evaluating CFLR solutions
//
// Generated on: 14/01/2016
//           by: v0.0.1
#include <array>
#include "btree_map.h"
#include "btree_set.h"
#include "neighbourhood_map.h"
#include "relation.h"
namespace cflr {
struct andersen_semi_naive {
    // Definitions
    static const unsigned num_lbls = 6;
    static const unsigned num_domains = 2;
    typedef neighbourhood_map<btree::btree_map<ident, btree::btree_set<ident>>, btree::btree_set<ident>> adt_t;
    typedef std::array<relation<adt_t>, num_lbls> rels_t;
    typedef std::array<size_t, num_domains> vols_t;
    // Delta expansion rules
    static void delta_0(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[0].volume());
        cur_delta.swap_contents(deltas[0]);
        // Label 0, occurance 0, rule 4 -> 0
        {
            adt_t tmp0;
            cur_delta.adts[0].deep_copy(tmp0);
            tmp0.difference(relations[4].adts[0]);
            deltas[4].adts[0].union_copy(tmp0);
            relations[4].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_1(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[1].volume());
        cur_delta.swap_contents(deltas[1]);
        // Label 1, occurance 0, rule 4 -> 1 4
        if(!relations[4].adts[0].empty()) {
            adt_t tmp0;
            cur_delta.adts[0].compose<false, false>(relations[4].adts[0], tmp0);
            tmp0.difference(relations[4].adts[0]);
            deltas[4].adts[0].union_copy(tmp0);
            relations[4].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_2(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[2].volume());
        cur_delta.swap_contents(deltas[2]);
        // Label 2, occurance 0, rule 4 -> 2 4 -4 3 4
        if(!relations[4].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[3].adts[0].empty()) if(!relations[4].adts[0].empty()) {
                        adt_t tmp0;
                        cur_delta.adts[0].compose<false, false>(relations[4].adts[0], tmp0);
                        adt_t tmp1;
                        tmp0.compose<false, true>(relations[4].adts[0], tmp1);
                        adt_t tmp2;
                        tmp1.compose<false, false>(relations[3].adts[0], tmp2);
                        adt_t tmp3;
                        tmp2.compose<false, false>(relations[4].adts[0], tmp3);
                        tmp3.difference(relations[4].adts[0]);
                        deltas[4].adts[0].union_copy(tmp3);
                        relations[4].adts[0].union_absorb(tmp3);
                    }
    }
    static void delta_3(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[3].volume());
        cur_delta.swap_contents(deltas[3]);
        // Label 3, occurance 0, rule 4 -> 2 4 -4 3 4
        if(!relations[2].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[4].adts[0].empty()) {
                        adt_t tmp0;
                        cur_delta.adts[0].compose<false, false>(relations[4].adts[0], tmp0);
                        adt_t tmp1;
                        relations[4].adts[0].compose<true, false>(tmp0, tmp1);
                        adt_t tmp2;
                        relations[4].adts[0].compose<false, false>(tmp1, tmp2);
                        adt_t tmp3;
                        relations[2].adts[0].compose<false, false>(tmp2, tmp3);
                        tmp3.difference(relations[4].adts[0]);
                        deltas[4].adts[0].union_copy(tmp3);
                        relations[4].adts[0].union_absorb(tmp3);
                    }
    }
    static void delta_4(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[4].volume());
        cur_delta.swap_contents(deltas[4]);
        // Label 4, occurance 0, rule 4 -> 1 4
        if(!relations[1].adts[0].empty()) {
            adt_t tmp0;
            relations[1].adts[0].compose<false, false>(cur_delta.adts[0], tmp0);
            tmp0.difference(relations[4].adts[0]);
            deltas[4].adts[0].union_copy(tmp0);
            relations[4].adts[0].union_absorb(tmp0);
        }
        // Label 4, occurance 0, rule 4 -> 2 4 -4 3 4
        if(!relations[2].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[3].adts[0].empty()) if(!relations[4].adts[0].empty()) {
                        adt_t tmp0;
                        cur_delta.adts[0].compose<false, true>(relations[4].adts[0], tmp0);
                        adt_t tmp1;
                        tmp0.compose<false, false>(relations[3].adts[0], tmp1);
                        adt_t tmp2;
                        tmp1.compose<false, false>(relations[4].adts[0], tmp2);
                        adt_t tmp3;
                        relations[2].adts[0].compose<false, false>(tmp2, tmp3);
                        tmp3.difference(relations[4].adts[0]);
                        deltas[4].adts[0].union_copy(tmp3);
                        relations[4].adts[0].union_absorb(tmp3);
                    }
        // Label 4, occurance 1, rule 4 -> 2 4 -4 3 4
        if(!relations[2].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[3].adts[0].empty()) if(!relations[4].adts[0].empty()) {
                        adt_t tmp0;
                        cur_delta.adts[0].compose<true, false>(relations[3].adts[0], tmp0);
                        adt_t tmp1;
                        tmp0.compose<false, false>(relations[4].adts[0], tmp1);
                        adt_t tmp2;
                        relations[4].adts[0].compose<false, false>(tmp1, tmp2);
                        adt_t tmp3;
                        relations[2].adts[0].compose<false, false>(tmp2, tmp3);
                        tmp3.difference(relations[4].adts[0]);
                        deltas[4].adts[0].union_copy(tmp3);
                        relations[4].adts[0].union_absorb(tmp3);
                    }
        // Label 4, occurance 2, rule 4 -> 2 4 -4 3 4
        if(!relations[2].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[4].adts[0].empty()) if(!relations[3].adts[0].empty()) {
                        adt_t tmp0;
                        relations[3].adts[0].compose<false, false>(cur_delta.adts[0], tmp0);
                        adt_t tmp1;
                        relations[4].adts[0].compose<true, false>(tmp0, tmp1);
                        adt_t tmp2;
                        relations[4].adts[0].compose<false, false>(tmp1, tmp2);
                        adt_t tmp3;
                        relations[2].adts[0].compose<false, false>(tmp2, tmp3);
                        tmp3.difference(relations[4].adts[0]);
                        deltas[4].adts[0].union_copy(tmp3);
                        relations[4].adts[0].union_absorb(tmp3);
                    }
        // Label 4, occurance 0, rule 5 -> 4 -4
        if(!relations[4].adts[0].empty()) {
            adt_t tmp0;
            cur_delta.adts[0].compose<false, true>(relations[4].adts[0], tmp0);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
        }
        // Label 4, occurance 1, rule 5 -> 4 -4
        if(!relations[4].adts[0].empty()) {
            adt_t tmp0;
            relations[4].adts[0].compose<false, true>(cur_delta.adts[0], tmp0);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_5(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[5].volume());
        cur_delta.swap_contents(deltas[5]);
    }
    // Solver definition
    static void solve(vols_t& volume, rels_t& relations) {
        // Epsilon initialisation
        size_t largest_vertex_domain = 0;
        largest_vertex_domain = std::max(largest_vertex_domain, volume[0]);
        largest_vertex_domain = std::max(largest_vertex_domain, volume[1]);
        const adt_t epsilon = adt_t::identity(largest_vertex_domain);
        // Delta initialisation
        rels_t deltas {relation<adt_t>(relations[0].adts.size()), relation<adt_t>(relations[1].adts.size()), relation<adt_t>(relations[2].adts.size()), relation<adt_t>(relations[3].adts.size()), relation<adt_t>(relations[4].adts.size()), relation<adt_t>(relations[5].adts.size())};
        for(unsigned i=0; i<relations[0].adts.size(); ++i) relations[0].adts[i].deep_copy(deltas[0].adts[i]);
        for(unsigned i=0; i<relations[1].adts.size(); ++i) relations[1].adts[i].deep_copy(deltas[1].adts[i]);
        for(unsigned i=0; i<relations[2].adts.size(); ++i) relations[2].adts[i].deep_copy(deltas[2].adts[i]);
        for(unsigned i=0; i<relations[3].adts.size(); ++i) relations[3].adts[i].deep_copy(deltas[3].adts[i]);
        for(unsigned i=0; i<relations[4].adts.size(); ++i) relations[4].adts[i].deep_copy(deltas[4].adts[i]);
        for(unsigned i=0; i<relations[5].adts.size(); ++i) relations[5].adts[i].deep_copy(deltas[5].adts[i]);
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
        // SCC [5]
        while(true) {
            if (!deltas[5].empty()) {
                delta_5(volume, relations, deltas);
                continue;
            }
            break;
        }
    }
}; // end struct andersen_semi_naive
} // end namespace cflr
