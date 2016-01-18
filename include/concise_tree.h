/* 
 *                              concise_tree.h
 * 
 * Author: Nic H.
 * Date: 2016-Jan-18
 * 
 * Definition of the concise-quadtree abstract data-structure.  This
 * implementation features leaf-nodes, which encode multiple layers of
 * information in the data-structure's leaves.
 */

#ifndef __CONCISE_TREE_H__
#define __CONCISE_TREE_H__

#include <array>
#include <tuple>
#include <vector>

#include "adt.h"

namespace cflr {

struct concise_tree{

    typedef ident index_t;
    typedef std::array<index_t, 4> node_t;
    
    static const unsigned index_bits = sizeof(index_t)*8;
    static const unsigned node_bits = sizeof(node_t)*8;
    static const unsigned node_height = log<4>(node_bits);

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
};

} // end namespace cflr

#endif /* __CONCISE_TREE_H__ */
