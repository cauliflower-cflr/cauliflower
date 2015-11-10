/* 
 *                            relation_buffer.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 * 
 * The buffer to provide identifiers for arbitrary relational input types.
 * The buffer can be read directly from CSV files
 */

#ifndef __RELATION_BUFFER_H__
#define __RELATION_BUFFER_H__

#include <vector>
#include <array>
#include <string>
#include <iostream>

#include "utilities.h"

namespace cflr{

template<typename T, unsigned Cols>
struct relation_buffer{

    static_assert(Cols >= 2, "Relations must have at least binary cardinality");

    typedef std::array<T, Cols> buffered_type;
    typedef std::array<ident, Cols> value_type;

    std::array<registrar<T>*, Cols> registrars;
    std::vector<value_type> data;

    relation_buffer() = delete;
    relation_buffer(const std::array<registrar<T>*, Cols> regs) : registrars(regs), data() {}

    size_t size() const {
        return data.size();
    }

    void add(const buffered_type& row){
        std::array<ident, Cols> tmp;
        // hopefully this gets unrolled
        for(unsigned i=0; i<Cols; i++) tmp[i] = registrars[i]->get_or_add(row[i]);
        data.push_back(tmp);
    }

    buffered_type retrieve(size_t idx) const {
        buffered_type ret;
        for(unsigned i=0; i<Cols; i++) ret[i] = registrars[i]->get(data[idx][i]);
        return ret;
    }

    value_type& operator[](const size_t i){
        return data[i];
    }

    void from_csv(){
        // TODO
    }

    void to_csv(std::ostream& os){
        for(value_type& a : data){
            for(unsigned i=0; i<Cols; i++){
                if(i != 0) os << ",";
                os << registrars[i]->get(a[i]);
            }
            os << std::endl;
        }
    }

};

} // end namespace cflr

#endif /* __RELATION_BUFFER_H__ */
