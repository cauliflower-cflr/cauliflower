#include <string>
#include <set>

#include <boost/test/unit_test.hpp>

#include "utilities.h"

using namespace cflr;
using namespace std;

BOOST_AUTO_TEST_SUITE( registrar_test )

    template<typename T>
    void registrar_type_test(T a, T b){
        BOOST_CHECK(a != b /*Dont hand this test the same things*/);
        registrar<T> reg;
        auto i = reg.get_or_add(a);
        auto j = reg.get_or_add(b);
        BOOST_CHECK(j == reg.get_or_add(b));
        BOOST_CHECK(i == reg.get_or_add(a));
        BOOST_CHECK(i != j);
        BOOST_CHECK(a == reg.get(i));
        BOOST_CHECK(b == reg.get(j));
    }

    BOOST_AUTO_TEST_CASE( typed_registrars ){
        registrar_type_test("Hello", "World");
        registrar_type_test(string("Hello"), string("World"));
        registrar_type_test('a', 'b');
        registrar_type_test('X', 'x');
        registrar_type_test(1, 0);
        registrar_type_test(0l, 1l);
    }

    BOOST_AUTO_TEST_CASE( registrar_coverage ){
        registrar<int> reg;
        int itms[6] = {-9, 4, 5, 3498, -10, 0};
        for(auto i : itms) reg.get_or_add(i);
        BOOST_CHECK(reg.size() == 6);
        for(auto i : itms) reg.get_or_add(i);
        BOOST_CHECK(reg.size() == 6);
        set<int> itmSet(itms, itms+6);
        set<int> regSet;
        for(unsigned j=0; j<reg.size(); j++) regSet.insert(reg.get(j));
        BOOST_CHECK(itmSet == regSet);

    }

BOOST_AUTO_TEST_SUITE_END()

