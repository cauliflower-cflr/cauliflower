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

#include <algorithm>
#include <array>
#include <iostream>
#include <iomanip>
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

struct concise_tree_order{
    typedef std::pair<ident, ident> import_t;
    const unsigned height;
    concise_tree_order(unsigned h) : height(h) {}
    bool operator()(const import_t& a, import_t& b){
        unsigned aq, bq;
        // can be made even faster: max(leading_zero(a.first ^ b.first), leading_zero(a.second ^ b.second))
        for(int i=height; i>=0; i--){
            //the quadrant of a row/col pair at height h depends on wether bit [h] is set
            aq = quadrant(a.first, a.second, i);
            bq = quadrant(b.first, b.second, i);
            if(aq != bq) break;
        }
        return aq < bq;
    }
    static inline constexpr unsigned quadrant(ident row, ident col, unsigned height){
        return 2*((row>>height)&1) + ((col>>height)&1);
    }
};

typedef ident index_t;
typedef std::array<index_t, 4> node_t;

struct concise_tree;

struct concise_iterator {
    typedef std::pair<ident, ident> value_type;

    static const unsigned index_size = sizeof(index_t)*8;
    static const unsigned index_sl = csqrt(index_size);
    static const unsigned min_h = log<2>(index_sl*2);

    std::vector<node_t>::const_iterator it;
    std::vector<node_t>::const_iterator end;
    std::array<ident, sizeof(ident)*8*4> rstack;
    std::array<ident, sizeof(ident)*8*4> cstack;
    std::array<uint8_t, sizeof(ident)*8*4> hstack;
    uint8_t q;
    uint8_t b;
    unsigned d;
    value_type val;

    concise_iterator(const std::vector<node_t>& ct, unsigned h) : it(ct.begin()), end(ct.end()), q(4), b(index_size), d(1) {
        rstack[0] = 0;
        cstack[0] = 0;
        hstack[0] = h;
        advance();
    }

    concise_iterator(const std::vector<node_t>& ct) : it(ct.end()), d(0) {}

    bool operator==(const concise_iterator& other) const {
        return it == other.it && ((it == end) || (q == other.q && b == other.b));
    }

    bool operator!=(const concise_iterator& other) const {
        return !(*this == other);
    }

    value_type operator*() const {
        return val;
    }

    const value_type* operator->() const {
        return &val;
    }

    concise_iterator& operator++(){
        advance();
        return *this;
    }

private:

    void advance(){
        while(true){
            if (b < index_size) {
                if(((it->at(q)) >> b)&1){
                    val = {rstack[d] + b/index_sl + ((q/2)*index_sl), cstack[d] + b%index_sl + ((q%2)*index_sl)};
                    b++;
                    if(b == index_size) q++;
                    return;
                } else b++;
                if(b == index_size) q++;
            } else if (q < 4) {
                if (it->at(q) != 0){
                    b = 0;
                } else q++;
            } else {
                d--;
                unsigned h = hstack[d];
                ident r = rstack[d];
                ident c = cstack[d];
                if(h == min_h) q = 0;
                else {
                    unsigned sl = 1<<(h-1);
                    for(int qu=3; qu>=0; qu--) if (it->at(qu) != 0) { // push to stack in reverse order
                        rstack[d] = r + ((qu/2)*sl);
                        cstack[d] = c + ((qu%2)*sl);
                        hstack[d] = h-1;
                        d++;
                    }
                }
            }
            if(q == 4 && b == index_size){
                it++;
                if(it == end) return;
            }
        }
    }
};

/// concise_tree, the concise storage representation (contiguous) of quadtrees
struct concise_tree{
    
    static const unsigned index_bits = sizeof(index_t)*8;
    static const unsigned node_bits = sizeof(node_t)*8;
    static const unsigned index_sl = csqrt(index_bits);
    static const unsigned index_height = log<2>(index_sl);
    static const unsigned node_sl = index_sl*2;
    static const unsigned node_height = log<2>(node_sl);
    static const index_t identity_index = cidentity_mat<index_t>();
    static const index_t index_rc_mask = (1<<index_height)-1;

    unsigned height;
    std::vector<concise_tree_order::import_t> import_buffer;
    std::vector<node_t> tree;

    concise_tree() : height(node_height), import_buffer(), tree(1, node_t()) {}

    bool empty() const {
        return height == node_height && std::get<0>(tree[0]) == 0 && std::get<1>(tree[0]) == 0 && std::get<2>(tree[0]) == 0 && std::get<3>(tree[0]) == 0;
    }

    void clear() {
        tree.clear();
        tree.push_back(node_t());
        height = node_height;
    }

    void initialise_import() {
        //ASSERT(import_buffer.empty());
        clear();
    }
    void import(ident row, ident col){
        import_buffer.push_back({row, col});
        height = std::max(log<2>(std::max(row, col))+1, height); // runtime log<2> is inefficient(ish)
    }
    void finalise_import() {
        unsigned stack[height];
        std::sort(import_buffer.begin(), import_buffer.end(), concise_tree_order(height));
        for(const auto& p : import_buffer){
            unsigned cur = 0;
            unsigned added = 0;
            for(unsigned h=height-1; h>=node_height; h--){
                stack[h] = cur;
                cur += node_next(tree[cur], concise_tree_order::quadrant(p.first, p.second, h)) + 1;
                if(cur == tree.size()){
                    added++;
                    tree.push_back(node_t()); // because we add in-order, this must happen
                }
            }
            tree[cur][concise_tree_order::quadrant(p.first, p.second, node_height-1)] |=
                ((index_t)1) << (
                ((p.first&index_mask)<<index_height)|
                (p.second&index_mask));
            unsigned below=0;
            for(unsigned h=node_height; h<height; h++){
                below = std::min(below+1, added);
                tree[stack[h]][concise_tree_order::quadrant(p.first, p.second, h)] += below;
            }
        }
        import_buffer.clear();
    }

    concise_iterator begin() const{
        return concise_iterator(tree, height);
    }

    concise_iterator end() const{
        return concise_iterator(tree);
    }

    void deep_copy(concise_tree& into) const {
        into.height = height;
        into.tree = tree; // vector copy operation
    }

    void union_copy(const concise_tree& other){

    }

    void union_absorb(concise_tree& other){
        union_copy(other);
    }

    void dump(std::ostream& out) const {
        unsigned prt = 0;
        for(auto i=begin(), e=end(); i!=e; ++i){
             out << "(" << i->first << "," << i->second << ")" << (++prt % 8 == 0 ? "\n" : "\t");
        }
        // ident rstack[4*height];
        // ident cstack[4*height];
        // unsigned hstack[4*height];
        // rstack[0] = 0;
        // cstack[0] = 0;
        // hstack[0] = height;
        // unsigned dpth = 1;
        // unsigned prt = 0;
        // for(const auto& n : tree) {
        //     // for(unsigned i=0; i<4; i++){
        //     //     out << (i?",":"(") << std::setw(16) << std::setfill('0') << std::hex << n[i];
        //     // }
        //     // out << ")\n";
        //     dpth--;
        //     unsigned h = hstack[dpth];
        //     ident r = rstack[dpth];
        //     ident c = cstack[dpth];
        //     if (h == node_height) {
        //         for(unsigned q=0; q<4; q++) for(unsigned b=0; b<index_bits; b++) {
        //             ident rx = b/index_sl + ((q/2)*index_sl);
        //             ident cx = b%index_sl + ((q%2)*index_sl);
        //             if((n[q] >> b) & 1){
        //                 out << "(" << (rx+r) << "," << (cx+c) << ")" << (++prt % 8 == 0 ? "\n" : "\t");
        //             }
        //         }
        //     } else {
        //         unsigned sl = 1<<(h-1);
        //         for(int q=3; q>=0; q--) if (n[q] != 0) { // push to stack in reverse order
        //             rstack[dpth] = r + ((q/2)*sl);
        //             cstack[dpth] = c + ((q%2)*sl);
        //             hstack[dpth] = h-1;
        //             dpth++;
        //         }
        //     }
        // }
    }

    void dump() const {
        dump(std::cout);
    }

    /*
     * STATIC METHODS
     */

    static constexpr node_t identity_node(){
        return node_t{identity_index, 0, 0, identity_index};
    }

    /// node_next, return the offset to the subtree at quadrant q after n
    /// for q >= 4, return the total subtree size
    static unsigned node_next(const node_t& n, unsigned q){
        switch(q){
            case 0 :
                return 0;
            case 1 :
                return n[0];
            case 2 :
                return n[0] + n[1];
            case 3 :
                return n[0] + n[1] + n[2];
            default :
                return n[0] + n[1] + n[2] + n[3];
        }
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
