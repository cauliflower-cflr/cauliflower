
#include <omp.h>
#include <chrono>
#include <random>
#include "BTree.h"

using namespace std;

int main(){
    typedef cflr::btree_set<pair<unsigned, unsigned>> BT;
    BT a, b, c;
    std::vector<BT::chunk> parts;
//    cout << b.size() << " " << b.empty() << endl;
//    unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
//#pragma omp parallel
//    {
//#pragma omp single
//        cout << "TC " << omp_get_num_threads() << endl;
//        std::default_random_engine gen(seed + omp_get_thread_num());//for some reason 0 and 1 have same sequence
//        std::uniform_int_distribution<unsigned> rdm(0, 1<<16);
//        for(unsigned i=0; i<10; i++){
//            rdm(gen);
//        }
//#pragma omp for
//        for(unsigned i=0; i<100000000; i++){
//            b.insert(rdm(gen));
//        }
//    }
//    cout << b.size() << " " << b.empty() << endl;

    unsigned limit = 50000000;
    for(unsigned i=0; i<limit; i++){
        a.insert({limit-i, i});
        b.insert({limit-i, i+1});
    }

#pragma omp parallel
    {
        unsigned tc = omp_get_num_threads();
        auto be = b.end();
#pragma omp single
        {
            parts = a.getChunks(tc*40);
        }
    auto t = omp_get_wtime();
#pragma omp barrier
#pragma omp for schedule(dynamic)
        for(unsigned i=0; i<parts.size(); i++){
            auto ai = parts[i].begin();
            auto bi = b.lower_bound({(*ai).first, 0});
            for(auto ae = parts[i].end(); ai != ae; ++ai){
                while(bi != be && (*ai).first >= (*bi).first){
                    if((*ai).first == (*bi).first) c.insert({(*ai).second, (*bi).second});
                    ++bi;
                }
            }
        }
#pragma omp single
    cout << c.size() << " t=" << (omp_get_wtime() - t) << endl;
    }
}
