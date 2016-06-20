/* 
 *                              utilities.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-23
 * 
 * implementations for the utilities
 */

#include <algorithm>
#include <deque>

#include <boost/config.hpp>
#include <boost/graph/strong_components.hpp>
#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/topological_sort.hpp>

#include "utilities.h"

using namespace std;
using namespace cflr;


dependency_info::dep_res_t dependency_info::find_dependencies(const dep_list_t& deps){
    typedef boost::adjacency_list < boost::vecS, boost::vecS, boost::directedS > dgraph_t;
    // find the size of the dependency graph
    unsigned size = 0;
    for(const auto& d : deps) size = max(size, max(d.first+1, d.second+1));
    // init the strong components graph
    dgraph_t sccG(size);
    for(const auto& d : deps) add_edge(d.first, d.second, sccG);
    // get an integer mapping for the vertex index to a strong component for that index
    vector<int> comps(size);
    int numSCC = strong_components(sccG, make_iterator_property_map(comps.begin(), get(boost::vertex_index, sccG), comps[0]));
    // init the topological dependency graph
    dgraph_t topG(numSCC);
    for(const auto& d : deps){
        int fc = comps[d.first];
        int sc = comps[d.second];
        if(fc != sc) add_edge(fc, sc, topG);
    }
    // sort the graph into a topological order
    vector<int> topol;
    topological_sort(topG, back_inserter(topol), vertex_index_map(boost::identity_property_map()));
    dep_res_t ret;
    for(auto top : topol){
        vector<unsigned> ord;
        size_t n = 0;
        for(auto comp : comps){
            if(comp == top) ord.push_back(n);
            n++;
        }
        ret.push_back(ord);
    }
    return ret;
}

