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

        typedef relation_buffer<int, int, int> i3;
        i3 buf2{i3::reg_type{&reg, &reg, &reg}};
        buf2.add(i3::outer_type{4, 5, 6});
        buf2.add(i3::outer_type{6, 7, 4});
        BOOST_CHECK(reg.get_or_add(6) == buf2[0][2]);
        BOOST_CHECK(reg.get_or_add(6) == buf2[1][0]);

        stringstream ss;
        buf2.to_csv(ss);
        BOOST_CHECK(ss.str() == "4,5,6\n6,7,4\n");
    }

    BOOST_AUTO_TEST_CASE( field_volumes ){
        registrar<int> r0;
        registrar<int> r1;
        r1.get_or_add(0);
        registrar<int> r2;
        r2.get_or_add(0);
        r2.get_or_add(1);
        registrar<int> r3;
        r3.get_or_add(0);
        r3.get_or_add(1);
        r3.get_or_add(2);

        // relations without fields have 1 adt
        typedef relation_buffer<int, int> i2;
        BOOST_CHECK(i2(i2::reg_type{&r3, &r3}).field_volume() == 1);
        BOOST_CHECK(i2(i2::reg_type{&r0, &r2}).field_volume() == 1);
        BOOST_CHECK(i2(i2::reg_type{&r1, &r0}).field_volume() == 1);

        // relations with 1 field have the volume of that field's registrar
        typedef relation_buffer<int, int, int> i3;
        BOOST_CHECK(i3(i3::reg_type{&r1, &r2, &r0}).field_volume() == 0);
        BOOST_CHECK(i3(i3::reg_type{&r0, &r2, &r1}).field_volume() == 1);
        BOOST_CHECK(i3(i3::reg_type{&r3, &r3, &r2}).field_volume() == 2);
        BOOST_CHECK(i3(i3::reg_type{&r1, &r0, &r3}).field_volume() == 3);

        // relations with 2 fields multiply their volumes
        typedef relation_buffer<int, int, int, int> i4;
        BOOST_CHECK(i4(i4::reg_type{&r1, &r2, &r3, &r3}).field_volume() == 9);
        BOOST_CHECK(i4(i4::reg_type{&r1, &r2, &r0, &r3}).field_volume() == 0);
        BOOST_CHECK(i4(i4::reg_type{&r1, &r2, &r1, &r2}).field_volume() == 2);

        // all fieldless relations index to volume 0
        i2 buf2(i2::reg_type{&r2, &r1});
        buf2.add(i2::outer_type{0, 0});
        buf2.add(i2::outer_type{1, 0});
        BOOST_CHECK(buf2.index_volume(0) == 0);
        BOOST_CHECK(buf2.index_volume(1) == 0);

        // identical field indices cause the same index volume
        i3 buf3(i3::reg_type{&r3, &r3, &r1});
        buf3.add(i3::outer_type{0, 2, 0});
        buf3.add(i3::outer_type{2, 0, 0});
        BOOST_CHECK(buf3.index_volume(0) == buf3.index_volume(1));
        i4 buf4(i4::reg_type{&r1, &r2, &r3, &r3});
        buf4.add(i4::outer_type{0, 0, 2, 0});
        buf4.add(i4::outer_type{0, 0, 0, 2});
        buf4.add(i4::outer_type{0, 0, 2, 1});
        buf4.add(i4::outer_type{0, 1, 0, 2});
        buf4.add(i4::outer_type{0, 1, 2, 0});
        BOOST_CHECK(buf4.index_volume(0) == buf4.index_volume(4));
        BOOST_CHECK(buf4.index_volume(1) == buf4.index_volume(3));

        // index volumes increase with lefter registrars contributing more
        typedef relation_buffer<int, int, int, int, int> i5;
        i5 buf5(i5::reg_type{&r3, &r3, &r3, &r3, &r3});
        for(int x=0; x<3*3*3*3*3; x++){
            buf5.add(i5::outer_type{x/81, (x%81)/27, (x%27)/9, (x%9)/3, x%3});
        }
        bool pass=true;
        for(unsigned i=0; pass && i<buf5.size(); ++i){
            pass = buf5.index_volume(i) == (buf5[i][2]*9 + buf5[i][3]*3 + buf5[i][4]);
        }
        BOOST_CHECK(pass);
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

        typedef relation_buffer<int, char, string> ics;
        registrar<int> ri;
        registrar<char> rc;
        registrar<string> rs;
        ics buf2(ics::reg_type{&ri, &rc, &rs});
        buf2.add(ics::outer_type{42, 'X', "Hello World!"});
        BOOST_CHECK(ri.size() == 1);
        BOOST_CHECK(rc.size() == 1);
        BOOST_CHECK(rs.size() == 1);
        buf2.add(ics::outer_type{42, '.', "Hello World!"});
        BOOST_CHECK(ri.size() == 1);
        BOOST_CHECK(rc.size() == 2);
        BOOST_CHECK(rs.size() == 1);
        
        stringstream ss;
        buf2.to_csv(ss);
        BOOST_CHECK(ss.str() == "42,X,Hello World!\n42,.,Hello World!\n");
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
