#include <string>
#include <set>
#include <sstream>

#include <boost/test/unit_test.hpp>

#include "utilities.h"
#include "relation_buffer.h"

using namespace cflr;
using namespace std;

BOOST_AUTO_TEST_SUITE( relation_buffer_test )

    BOOST_AUTO_TEST_CASE( int_buffer ){
        registrar<int> reg;
        relation_buffer<int, 2> buf({&reg, &reg});
        buf.add({5, 6});
        buf.add({6, 6});
        buf.add({6, 5});
        buf.add({5, 5});
        BOOST_CHECK(buf.size() == 4);
        auto i2 = buf.retrieve(2);
        BOOST_CHECK(i2[0] == 6 && i2[1] == 5);
        BOOST_CHECK(reg.get(buf[2][0]) == 6);
        BOOST_CHECK(reg.get(buf[2][1]) == 5);

        relation_buffer<int, 3> buf2({&reg, &reg, &reg});
        buf2.add({4, 5, 6});
        buf2.add({6, 7, 4});
        BOOST_CHECK(reg.get_or_add(6) == buf2[0][2]);
        BOOST_CHECK(reg.get_or_add(6) == buf2[1][0]);

        stringstream ss;
        buf2.to_csv(ss);
        BOOST_CHECK(ss.str() == "4,5,6\n6,7,4\n");
    }

BOOST_AUTO_TEST_SUITE_END()
