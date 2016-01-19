/* 
 *                              concise_tree.h
 * 
 * Author: Nic H.
 * Date: 2016-Jan-18
 * 
 * Definition of the concise-quadtree abstract data-structure.  This
 * implementation features leaf-nodes, which encode multiple layers of
 * information in the data-structure's leaves.
 *
 * sample identity matrix:
 * 1000010000100001
 * ^              ^
 * 15th           0th
 *   r3c1         r0c0
 *  r3c2         r0c1
 * r3c3         r0c2
 */

#ifndef __CONCISE_TREE_H__
#define __CONCISE_TREE_H__

#include <array>
#include <tuple>
#include <vector>

#include "adt.h"

namespace cflr {

/// generate an identity matrix for type I
template<typename I> constexpr I cidentity_mat_help(unsigned idx, I cur){
    return idx >= sizeof(I)*8 ? cur : cidentity_mat_help<I>(idx+csqrt<unsigned>(sizeof(I)*8)+1, cur | (((I)1)<<idx));
}
template<typename I> constexpr I cidentity_mat(){
    return cidentity_mat_help<I>(0, 0);
}

struct concise_tree{

    typedef ident index_t;
    typedef std::array<index_t, 4> node_t;
    
    static const unsigned index_bits = sizeof(index_t)*8;
    static const unsigned node_bits = sizeof(node_t)*8;
    static const unsigned index_sl = csqrt(index_bits);
    static const unsigned node_sl = index_sl*2;
    static const unsigned node_height = log<4>(node_bits);
    static const index_t identity_index = cidentity_mat<index_t>();

    unsigned height;
    std::vector<node_t> tree;

    concise_tree() : height(node_height), tree(1, node_t()) {}

    bool empty() const {
        return height == node_height && std::get<0>(tree[0]) == 0 && std::get<1>(tree[0]) == 0 && std::get<2>(tree[0]) == 0 && std::get<3>(tree[0]) == 0;
    }

    void clear() {
        tree.clear();
        tree.push_back(node_t());
        height = node_height;
    }

    static constexpr node_t identity_node(){
        return node_t{identity_index, 0, 0, identity_index};
    }

    // constants used by indices
    static const index_t index_mask = (((index_t)1) << index_sl) - 1;
    /// multiply two index-matrices
    static index_t index_mult(index_t a, index_t b){
        index_t c = 0;
        for(unsigned ar=0; ar<index_sl; ar++){
            for(unsigned br=0; br<index_sl; br++){
                // bit manipulations for matrix multiplication:
                // select a mask from A[r][c] and a row from B[*][c]
                // mask: multiply ~0 by A>>rc&1
                // row: b>>r&mask
                // union-assign to output
                c |= ((index_mask*((a >> (br + index_sl*ar)) & 1)) & ((b >> (index_sl*br)) & index_mask)) << (index_sl*ar);
            }
        }
        return c;
    }

    /// multiply two nodes
    static void node_mult(const node_t& a, const node_t& b, node_t& c){
        c[0] = index_mult(a[0], b[0]) | index_mult(a[1], b[2]);
        c[1] = index_mult(a[0], b[1]) | index_mult(a[1], b[3]);
        c[2] = index_mult(a[2], b[0]) | index_mult(a[3], b[2]);
        c[3] = index_mult(a[2], b[1]) | index_mult(a[3], b[3]);
    }

    template<typename OUT> static void dump_node(const node_t& n, OUT& out){
        for(unsigned r=0; r<index_sl; r++){
            for(unsigned q=0; q<4; q++){
                if(r == 0){
                    out << (std::array<std::string, 4>{"TL", "TR", "BL", "BR"}[q]);
                } else {
                    out << "  ";
                }
                for(unsigned c=0; c<index_sl; c++){
                    out << " " << ((n[q] >> (c + index_sl*r)) & 1);
                }
            }
            out << "\n";
        }
        // for(unsigned qr=0; qr<2; qr++){
        //     for(unsigned r=0; r<index_sl; r++){
        //         for(unsigned qc=0; qc<2; qc++){
        //             for(unsigned c=0; c<index_sl; c++){
        //                 out << ((n[qc + qr*2] >> (c + index_sl*r)) & 1) << " ";
        //             }
        //         }
        //         out << "\n";
        //     }
        // }
    }
};

} // end namespace cflr

#endif /* __CONCISE_TREE_H__ */
