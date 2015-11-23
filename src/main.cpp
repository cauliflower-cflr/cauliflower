/* 
 *                                 main.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 */

#include <map>
#include <set>
#include <iostream>

#include "adt.h"
#include "cflr.h"
#include "neighbourhood_map.h"
//#include "problem.h"
#include "relation.h"
#include "relation_buffer.h"
#include "semi_naive.h"
#include "utility_templates.h"

namespace cflr {
/// ulist_printer, stream the indices to an output stream
template<typename Ul> struct ulist_printer;
template<> struct ulist_printer<ulist<>> {
    static inline void print(std::ostream& os){
        os << std::endl;
    }
};
template<unsigned Cur, unsigned Nxt, unsigned...Rest> struct ulist_printer<ulist<Cur, Nxt, Rest...>> {
    static inline void print(std::ostream& os){
        os << Cur << ", ";
        ulist_printer<ulist<Nxt, Rest...>>::print(os);
    }
};
template<unsigned Cur> struct ulist_printer<ulist<Cur>> {
    static inline void print(std::ostream& os){
        os << Cur;
        ulist_printer<ulist<>>::print(os);
    }
};
} // end namespace cflt

using namespace std;
using namespace cflr;
using namespace cflr::rule_clauses;

typedef problem<
    label<>,
    label<>,
    label<>,
    rule<clause<2>>,
    //rule<clause<2>, clause<0>, clause<2>, clause<1>>,
    //rule<clause<0>, clause<2>>,
    //rule<clause<1>, clause<0>>> prob;
    rule<clause<2>, clause<0>, clause<2>, clause<1>>> prob;
typedef cflr::neighbourhood_map<map<ident, set<ident>>,set<ident>> nmap;
typedef registrar_group<int> registrars_t;
typedef semi_naive<nmap, registrars_t::size, prob> solver_t;

int main(){
    // Init
    registrars_t regs;
    relation_buffer<int, int> a_buf(regs.select<0, 0>());
    relation_buffer<int, int> b_buf(regs.select<0, 0>());
    relation_buffer<int, int> s_buf(regs.select<0, 0>());

    // Read inputs
    a_buf.from_csv("example/running/a.csv");
    cout << "a:" << endl;
    a_buf.to_csv(cout);
    b_buf.from_csv("example/running/b.csv");
    cout << endl << "b:" << endl;
    b_buf.to_csv(cout);
    cout << endl << "---------------------------" << endl;
    registrars_t::volume_t volumes = regs.volumes();

    // Convert to relations
    solver_t::relations_t rels = relation_group_initialiser<nmap, problem_labels<prob>::result>::init(volumes);
    rels[0].import_buffer(a_buf);
    rels[1].import_buffer(b_buf);
    rels[0].dump(cout);
    rels[1].dump(cout);
    rels[2].dump(cout);
    solver_t::solve(volumes, rels);

    cout << endl << solver_t::rules_t::size << endl;
    cout << solver_t::rules_eps_t::size << endl;
    cout << solver_t::rules_reg_t::size << endl;
    //- nmap eps = nmap::identity(5);
    //- a_rel.import_buffer(a_buf);
    //- b_rel.import_buffer(b_buf);
    //- a_rel.dump(cout);
    //- b_rel.dump(cout);
    //- prob::compose_delta_rule<0>(eps);
    //- prob::compose_delta_rule<1>(eps);
    //- cout << prob::label_count << " : " << prob::rule_count << endl << endl;
    //- s_rel.adts[0].union_copy(eps);
    //- s_rel.adts[0].dump(cout);
    //- for(unsigned i=0; i<4; i++){
    //-     nmap tmp;
    //-     a_rel.adts[0].compose(s_rel.adts[0], tmp);
    //-     tmp.compose(b_rel.adts[0], s_rel.adts[0]);
    //-     cout << endl;
    //-     s_rel.adts[0].dump(cout);
    //- }
    //- s_rel.adts[0].difference(eps);

    // Show result
    rels[2].export_buffer(s_buf);
    cout << endl << "S:" << endl;
    s_buf.to_csv(cout);
    cout << endl;
}

