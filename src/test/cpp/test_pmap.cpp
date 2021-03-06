#include "pmap.h"
#include <boost/test/unit_test.hpp>

using namespace cflr;

BOOST_AUTO_TEST_SUITE( pmap_test )

    BOOST_AUTO_TEST_CASE ( create_test ){
        pmap pm;
        BOOST_CHECK(pm.profile().first == 0);
        BOOST_CHECK(pm.profile().second == 0);
        BOOST_CHECK(pm.size() == 0);
        pm.initialise_import();
        pm.import(0, 0);
        pm.import(0, 1);
        pm.import(1, 2);
        pm.finalise_import();
        BOOST_CHECK(pm.profile().first == 2);
        BOOST_CHECK(pm.profile().second == 3);
        BOOST_CHECK(pm.size() == 3);
    }

BOOST_AUTO_TEST_SUITE_END()
