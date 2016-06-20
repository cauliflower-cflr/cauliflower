#include <map>
#include <set>
#include <unordered_map>
#include <unordered_set>

#include <boost/test/unit_test.hpp>

#include "neighbourhood_map.h"

using namespace cflr;
using namespace std;

BOOST_AUTO_TEST_SUITE( neighbourhood_map_test )

    typedef neighbourhood_map<map<ident, set<ident>>, set<ident>> std_nmap;
    typedef neighbourhood_map<unordered_map<ident, unordered_set<ident>>, unordered_set<ident>> hash_nmap;

    template<typename NM>
    struct nmap_helper {
        static void empty_test() {
            NM m;
            BOOST_CHECK(m.empty());
            m.initialise_import();
            m.import(0, 0);
            m.finalise_import();
            BOOST_CHECK(!m.empty());
        }

        static void iter_test() {
            NM m;
            BOOST_CHECK(m.begin() == m.end());
            m.initialise_import();
            m.import(0, 0);
            m.import(1, 1);
            m.import(1, 0);
            m.import(1, 1); // duplicate insert
            m.finalise_import();
            BOOST_CHECK(m.begin() != m.end());
            unsigned count = 0;
            for(auto i=m.begin(); i!=m.end(); ++i){
                ++count;
            }
            BOOST_CHECK(count == 3);
        }
    };

    BOOST_AUTO_TEST_CASE( empty_test ){
        nmap_helper<std_nmap>::empty_test();
        nmap_helper<hash_nmap>::empty_test();
    }

    BOOST_AUTO_TEST_CASE( iter_test ){
        nmap_helper<std_nmap>::iter_test();
        nmap_helper<hash_nmap>::iter_test();
    }

BOOST_AUTO_TEST_SUITE_END()
