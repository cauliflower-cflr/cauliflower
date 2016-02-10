
#include <iostream>
#include <omp.h>
#include <chrono>
#include <random>
#include "Trie.h"

using namespace std;

int main(){
//    Trie<1> t;
//    cout << t.size() << " " << t.empty() << endl;
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
//            t.insert(rdm(gen));
//        }
//    }
//    cout << t.size() << " " << t.empty() << endl;

//    typedef Trie<2> BT;
//    BT a, b, c;
//    std::vector<range<BT::iterator>> parts;
//    unsigned limit = 10000000;
//    for(unsigned i=0; i<limit; i++){
//        a.insert(limit-i, i);
//        b.insert(limit-i, i+1);
//    }
//
//#pragma omp parallel
//    {
//        unsigned tc = omp_get_num_threads();
//        //auto be = b.end();
//#pragma omp single
//        {
//            parts = a.partition(tc*40);
//        }
//    auto t = omp_get_wtime();
//#pragma omp barrier
//#pragma omp for schedule(dynamic)
//        for(unsigned i=0; i<parts.size(); i++){
//            auto ai = parts[i].begin();
//            for(auto ae = parts[i].end(); ai != ae; ++ai){
//                auto bounds = b.getBoundaries<1>({{ai->data[0], 0}}); // second index doesnt matter
//                for(auto i=bounds.begin(), e=bounds.end(); i!=e; ++i){
//                    c.insert(ai->data[1], i->data[1]);
//                }
//            }
//        }
//#pragma omp single
//    cout << c.size() << " t=" << (omp_get_wtime() - t) << endl;
//    }
    Trie<2> t1, t2;
    cout << "t1 " << t1.size() << " t2 " << t2.size() << endl;
    t1.insert(1, 1);
    t1.insert(1, 0);
    t1.insert(0, 1);
    cout << "t1 " << t1.size() << " t2 " << t2.size() << endl;
    std::swap(t1, t2);
    cout << "t1 " << t1.size() << " t2 " << t2.size() << endl;
}
