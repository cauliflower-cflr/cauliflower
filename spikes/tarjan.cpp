/* 
 *                                tarjan.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-23
 */

#include <iostream>
#include <vector>

#include "utility_templates.h"

namespace cflr {

template<typename G, unsigned Idx, typename Index, typename Low, typename Onstack, typename S> struct tarjan_scc{
    // typedef sub_prob;
    // typedef typename std::conditional<
    //     uindex_tm<Index, Idx-1>::result < G::size, 
    //     >::type result;
};
template<typename G, typename Index, typename Low, typename Onstack, typename S> struct tarjan_scc<G, 0, Index, Low, Onstack, S> {
    typedef tlist<> result;
};

}

using namespace std;
using namespace cflr;

unsigned sc(vector<vector<unsigned>> succs, unsigned v, unsigned idx, int* index, int* low, bool* onstack, vector<unsigned>& s){
    index[v] = idx;
    low[v] = idx;
    idx++;
    s.push_back(v);
    onstack[v] = true;

    for(unsigned w : succs[v]){
        if(index[w] == -1){
            idx = sc(succs, w, idx, index, low, onstack, s);
            low[v] = min(low[v], low[w]);
        } else if (onstack[w]) {
            low[v] = min(low[v], index[w]);
        }
    }

    if(low[v] == index[v]){
        cout << "SCC[" << s.size() << "]: ";
        unsigned w;
        do{
            w = s.back();
            s.pop_back();
            onstack[w] = false;
            cout << w << ",";
        } while(w != v);
        cout << endl;
    }

    return idx;
}

void tarjans(vector<vector<unsigned>> succs){
    const unsigned vs = succs.size();
    unsigned idx = 0;
    int index[vs];
    int low[vs];
    bool onstack[vs];
    vector<unsigned> s;

    for(unsigned i=0; i<vs; i++){
        index[i] = -1;
        low[i] = -1;
        onstack[i] = false;
    }

    for(unsigned v=0; v<vs; v++) if(index[v] == -1) idx = sc(succs, v, idx, index, low, onstack, s);
}

int main(){
    vector<vector<unsigned>> graph = {{}, {2}, {0, 2, 1}};
    unsigned ni = 0;
    for(auto& n : graph){
        for(auto i : n){
            cout << ni << "-" << i << endl;
        }
        ni++;
    }
    tarjans(graph);
    //cout << graph_t::size << endl;
    return 0;
}
