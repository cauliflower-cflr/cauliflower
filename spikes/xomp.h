// jvpt
//
// Semi-naive method fore evaluating CFLR solutions
//
// Generated on: 02/02/2016
//           by: v0.0.1
#include <array>
#include <map>
#include <set>
#include <omp.h>
#include "neighbourhood_map.h"
#include "relation.h"
namespace cflr {
struct jvpt_semi_naive {
    // Definitions
    static const unsigned num_lbls = 8;
    static const unsigned num_domains = 3;
    typedef neighbourhood_map<std::map<ident, std::set<ident>>, std::set<ident>> adt_t;
    typedef std::array<relation<adt_t>, num_lbls> rels_t;
    typedef std::array<size_t, num_domains> vols_t;
    // Delta expansion rules
    static void delta_0(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[0].volume());
        cur_delta.swap_contents(deltas[0]);
        // Label 0, occurance 0, rule 5 -> 0
        {
            adt_t tmp0;
            cur_delta.adts[0].deep_copy(tmp0);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_1(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[1].volume());
        cur_delta.swap_contents(deltas[1]);
        // Label 1, occurance 0, rule 5 -> -1 5
        if(!relations[5].adts[0].empty()) {
            adt_t tmp0;
            cur_delta.adts[0].compose<true, false>(relations[5].adts[0], tmp0);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_2(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[2].volume());
        cur_delta.swap_contents(deltas[2]);
        // Label 2, occurance 0, rule 6[0] -> -2[0] 5
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!cur_delta.adts[f0].empty()) if(!relations[5].adts[0].empty()) {
                    adt_t tmp0;
                    cur_delta.adts[f0].compose<true, false>(relations[5].adts[0], tmp0);
                    tmp0.difference(relations[6].adts[f0]);
                    deltas[6].adts[f0].union_copy(tmp0);
                    relations[6].adts[f0].union_absorb(tmp0);
                }
    }
    static void delta_3(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[3].volume());
        cur_delta.swap_contents(deltas[3]);
        // Label 3, occurance 0, rule 7[0] -> 3[0] 5
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!cur_delta.adts[f0].empty()) if(!relations[5].adts[0].empty()) {
                    adt_t tmp0;
                    cur_delta.adts[f0].compose<false, false>(relations[5].adts[0], tmp0);
                    tmp0.difference(relations[7].adts[f0]);
                    deltas[7].adts[f0].union_copy(tmp0);
                    relations[7].adts[f0].union_absorb(tmp0);
                }
    }
    static void delta_4(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[4].volume());
        cur_delta.swap_contents(deltas[4]);
        // Label 4, occurance 0, rule 5 -> 4 5
        if(!relations[5].adts[0].empty()) {
            adt_t tmp0;
            cur_delta.adts[0].compose<false, false>(relations[5].adts[0], tmp0);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
        }
    }
    static void delta_5(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        omp_lock_t r5l;
        relation<adt_t> cur_delta(deltas[5].volume());
        cur_delta.swap_contents(deltas[5]);
        omp_init_lock(&r5l);
#pragma omp parallel
        {
#pragma omp single
            {
#pragma omp task
                {
        // Label 5, occurance 0, rule 5 -> -1 5
        if(!relations[1].adts[0].empty()) {
            adt_t tmp0;
            relations[1].adts[0].compose<true, false>(cur_delta.adts[0], tmp0);
            omp_set_lock(&r5l);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
            omp_unset_lock(&r5l);
        }
                //}
//#pragma omp task
                //{
        // Label 5, occurance 0, rule 5 -> 4 5
        if(!relations[4].adts[0].empty()) {
            adt_t tmp0;
            relations[4].adts[0].compose<false, false>(cur_delta.adts[0], tmp0);
            omp_set_lock(&r5l);
            tmp0.difference(relations[5].adts[0]);
            deltas[5].adts[0].union_copy(tmp0);
            relations[5].adts[0].union_absorb(tmp0);
            omp_unset_lock(&r5l);
        }
                }
        // Label 5, occurance 0, rule 6[0] -> -2[0] 5
#pragma omp task
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!relations[2].adts[f0].empty()) {
                adt_t tmp0;
                relations[2].adts[f0].compose<true, false>(cur_delta.adts[0], tmp0);
                tmp0.difference(relations[6].adts[f0]);
                deltas[6].adts[f0].union_copy(tmp0);
                relations[6].adts[f0].union_absorb(tmp0);
            }
        // Label 5, occurance 0, rule 7[0] -> 3[0] 5
#pragma omp task
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!relations[3].adts[f0].empty()) {
                adt_t tmp0;
                relations[3].adts[f0].compose<false, false>(cur_delta.adts[0], tmp0);
                tmp0.difference(relations[7].adts[f0]);
                deltas[7].adts[f0].union_copy(tmp0);
                relations[7].adts[f0].union_absorb(tmp0);
            }
            }
        }
    }
    static void delta_6(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[6].volume());
        cur_delta.swap_contents(deltas[6]);
        // Label 6, occurance 0, rule 4 -> 6[0] -7[0]
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!cur_delta.adts[f0].empty()) if(!relations[7].adts[f0].empty()) {
                    adt_t tmp0;
                    cur_delta.adts[f0].compose<false, true>(relations[7].adts[f0], tmp0);
                    tmp0.difference(relations[4].adts[0]);
                    deltas[4].adts[0].union_copy(tmp0);
                    relations[4].adts[0].union_absorb(tmp0);
                }
    }
    static void delta_7(const vols_t& volume, rels_t& relations, rels_t& deltas) {
        relation<adt_t> cur_delta(deltas[7].volume());
        cur_delta.swap_contents(deltas[7]);
        // Label 7, occurance 0, rule 4 -> 6[0] -7[0]
        for(unsigned f0=0; f0<volume[0]; ++f0) if(!relations[6].adts[f0].empty()) if(!cur_delta.adts[f0].empty()) {
                    adt_t tmp0;
                    relations[6].adts[f0].compose<false, true>(cur_delta.adts[f0], tmp0);
                    tmp0.difference(relations[4].adts[0]);
                    deltas[4].adts[0].union_copy(tmp0);
                    relations[4].adts[0].union_absorb(tmp0);
                }
    }
    // Solver definition
    static void solve(vols_t& volume, rels_t& relations) {
        double time1, time2;
        // Epsilon initialisation
        size_t largest_vertex_domain = 0;
        largest_vertex_domain = std::max(largest_vertex_domain, volume[1]);
        largest_vertex_domain = std::max(largest_vertex_domain, volume[2]);
        const adt_t epsilon = adt_t::identity(largest_vertex_domain);
        // Delta initialisation
        rels_t deltas {relation<adt_t>(relations[0].adts.size()), relation<adt_t>(relations[1].adts.size()), relation<adt_t>(relations[2].adts.size()), relation<adt_t>(relations[3].adts.size()), relation<adt_t>(relations[4].adts.size()), relation<adt_t>(relations[5].adts.size()), relation<adt_t>(relations[6].adts.size()), relation<adt_t>(relations[7].adts.size())};
        time1 = omp_get_wtime();
#pragma omp parallel
        {
#pragma omp single
            {
#pragma omp task
                for(unsigned i=0; i<relations[0].adts.size(); ++i) relations[0].adts[i].deep_copy(deltas[0].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[1].adts.size(); ++i) relations[1].adts[i].deep_copy(deltas[1].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[2].adts.size(); ++i) relations[2].adts[i].deep_copy(deltas[2].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[3].adts.size(); ++i) relations[3].adts[i].deep_copy(deltas[3].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[4].adts.size(); ++i) relations[4].adts[i].deep_copy(deltas[4].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[5].adts.size(); ++i) relations[5].adts[i].deep_copy(deltas[5].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[6].adts.size(); ++i) relations[6].adts[i].deep_copy(deltas[6].adts[i]);
#pragma omp task
                for(unsigned i=0; i<relations[7].adts.size(); ++i) relations[7].adts[i].deep_copy(deltas[7].adts[i]);
            }
        }
        time2 = omp_get_wtime();
        std::cerr << "DELTA TIME " << (time2-time1) << std::endl;
        // SCC [0]
        time1 = omp_get_wtime();
        while(true) {
            if (!deltas[0].empty()) {
                delta_0(volume, relations, deltas);
                continue;
            }
            break;
        }
        time2 = omp_get_wtime();
        std::cerr << "[ALOC] " << (time2-time1) << std::endl;
        // SCC [1]
        time1 = omp_get_wtime();
        while(true) {
            if (!deltas[1].empty()) {
                delta_1(volume, relations, deltas);
                continue;
            }
            break;
        }
        time2 = omp_get_wtime();
        std::cerr << "[ASIG] " << (time2-time1) << std::endl;
        // SCC [2]
        time1 = omp_get_wtime();
        while(true) {
            if (!deltas[2].empty()) {
                delta_2(volume, relations, deltas);
                continue;
            }
            break;
        }
        time2 = omp_get_wtime();
        std::cerr << "[LOAD] " << (time2-time1) << std::endl;
        // SCC [3]
        time1 = omp_get_wtime();
        while(true) {
            if (!deltas[3].empty()) {
                delta_3(volume, relations, deltas);
                continue;
            }
            break;
        }
        time2 = omp_get_wtime();
        std::cerr << "[STOR] " << (time2-time1) << std::endl;
        // SCC [7, 5, 6, 4]
        time1 = omp_get_wtime();
        while(true) {
            if (!deltas[7].empty()) {
                delta_7(volume, relations, deltas);
                continue;
            }
            if (!deltas[5].empty()) {
                delta_5(volume, relations, deltas);
                continue;
            }
            if (!deltas[6].empty()) {
                delta_6(volume, relations, deltas);
                continue;
            }
            if (!deltas[4].empty()) {
                delta_4(volume, relations, deltas);
                continue;
            }
            break;
        }
        time2 = omp_get_wtime();
        std::cerr << "[4567] " << (time2-time1) << std::endl;
    }
}; // end struct jvpt_semi_naive
} // end namespace cflr
