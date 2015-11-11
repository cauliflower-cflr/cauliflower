#include <fstream>
#include <set>
#include <sstream>
#include <string>

#include <boost/test/unit_test.hpp>

#include "utilities.h"
#include "relation_buffer.h"

using namespace cflr;
using namespace std;

BOOST_AUTO_TEST_SUITE( relation_buffer_test )

    BOOST_AUTO_TEST_CASE( int_buffer ){
        typedef relation_buffer<int, int> i2;
        typedef relation_buffer<int, int, int> i3;
        registrar<int> reg;
        i2 buf(i2::reg_type{&reg, &reg});
        buf.add(i2::outer_type{5, 6});
        buf.add(i2::outer_type{6, 6});
        buf.add(i2::outer_type{6, 5});
        buf.add(i2::outer_type{5, 5});
        BOOST_CHECK(buf.size() == 4);
        auto idx2 = buf.retrieve(2);
        BOOST_CHECK(get<0>(idx2) == 6 && get<1>(idx2) == 5);
        BOOST_CHECK(reg.get(buf[2][0]) == 6);
        BOOST_CHECK(reg.get(buf[2][1]) == 5);

        i3 buf2{i3::reg_type{&reg, &reg, &reg}};
        buf2.add(i3::outer_type{4, 5, 6});
        buf2.add(i3::outer_type{6, 7, 4});
        BOOST_CHECK(reg.get_or_add(6) == buf2[0][2]);
        BOOST_CHECK(reg.get_or_add(6) == buf2[1][0]);

        stringstream ss;
        buf2.to_csv(ss);
        BOOST_CHECK(ss.str() == "4,5,6\n6,7,4\n");
    }

    BOOST_AUTO_TEST_CASE( multiple_domains ){
        typedef relation_buffer<string, string, string> s3;
        registrar<string> a;
        registrar<string> b;
        registrar<string> c;
        s3 buf(s3::reg_type{&a, &b, &c});
        buf.add(s3::outer_type{"cat","sat","mat"});
        BOOST_CHECK(a.size() == 1);
        BOOST_CHECK(b.size() == 1);
        BOOST_CHECK(c.size() == 1);
        buf.add(s3::outer_type{"cat", "sat", "sat"});
        buf.add(s3::outer_type{"cat", "cat", "cat"});
        BOOST_CHECK(a.size() == 1);
        BOOST_CHECK(b.size() == 2);
        BOOST_CHECK(c.size() == 3);
    }

    template<typename T, typename Tup, unsigned I>
    struct tuple_filler {
        inline static void fill(T t, Tup& tuple){
            std::get<I-1>(tuple) = t;
            tuple_filler<T, Tup, I-1>::fill(t, tuple);
        }
    };
    template<typename T, typename Tup>
    struct tuple_filler<T, Tup, 0>{
        inline static void fill(T t, Tup& tuple){
        }
    };

    template<typename T, typename...Ts>
    void csv_test(const string& csv_path){
        typedef relation_buffer<T, Ts...> RT;
        // Read the CSV file to a relation, and abck out
        registrar<T> reg;
        typename RT::reg_type reg_array;
        tuple_filler<registrar<T>*, typename RT::reg_type, sizeof...(Ts)+1>::fill(&reg, reg_array);
        RT buf(reg_array);
        buf.from_csv(csv_path);
        stringstream ss;
        buf.to_csv(ss);

        // Read the file straight int a string
        ifstream ifs(csv_path);
        string contents((istreambuf_iterator<char>(ifs)), (istreambuf_iterator<char>())); // most vexing parse
        auto idx = contents.find("\n\n");//double newlines are removed by the relation
        while(idx != string::npos){
            contents.replace(idx, 2, "\n");
            idx = contents.find("\n\n");
        }

        BOOST_CHECK(ss.str() == contents);//make sure they are the same
    }

    BOOST_AUTO_TEST_CASE( csv_io ){
        csv_test<string, string>("example/csv/foo.csv");
        csv_test<int, int, int, int>("example/csv/bar.csv");
        csv_test<char, char, char>("example/csv/baz.csv");
    }

BOOST_AUTO_TEST_SUITE_END()
