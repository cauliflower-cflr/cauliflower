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

#include <array>
#include <iostream>
#include <string>
#include <vector>

#include "csv.h"
#include "logger.h"
#include "utilities.h"
#include "utility_templates.h"

namespace cflr{

namespace template_internals {

// Buffer Helper
// A utility template specifically for packing/unpacking things from tuples
template<typename TReg, typename TArr, unsigned I, typename...Tups>
struct buffer_helper{
    static inline void register_tuple(const std::tuple<Tups...>& tup, TReg& regs, TArr& arr){
        arr[sizeof...(Tups)-I] = std::get<sizeof...(Tups)-I>(regs)->get_or_add(std::get<sizeof...(Tups)-I>(tup));
        buffer_helper<TReg, TArr, I-1, Tups...>::register_tuple(tup, regs, arr);
    }
    static inline void unregister_array(std::tuple<Tups...>& tup, const TReg& regs, const TArr& arr){
        std::get<sizeof...(Tups)-I>(tup) = std::get<sizeof...(Tups)-I>(regs)->get(arr[sizeof...(Tups)-I]);
        buffer_helper<TReg, TArr, I-1, Tups...>::unregister_array(tup, regs, arr);
    }
    // Since the tuples are indexed with I, this cannot be called with sizeof...(Ts)
    static inline void stream_tuple(std::ostream& os, const std::tuple<Tups...>& tup){
        buffer_helper<TReg, TArr, I-1, Tups...>::stream_tuple(os, tup);
        os << "," << std::get<I>(tup);
    }
};
template<typename TReg, typename TArr, typename...Tups>
struct buffer_helper<TReg, TArr, 0, Tups...>{
    static inline void register_tuple(const std::tuple<Tups...>& tup, TReg& regs, TArr& arr){
        //do nothing
    };
    static inline void unregister_array(std::tuple<Tups...>& tup, const TReg& regs, const TArr& arr){
        //do nothing
    };
    // Obviously it is an error to call this when sizeof...(Ts) == 0
    static inline void stream_tuple(std::ostream& os, const std::tuple<Tups...>& tup){
        os << std::get<0>(tup);
    }
};
} // end namespace template_internals

template<typename...Ts>
struct relation_buffer{

    static const unsigned cardinality = sizeof...(Ts);
    static_assert(cardinality >= 2, "Relations must have at least binary cardinality");

    typedef std::tuple<Ts...> outer_type; // The external representation
    typedef typename t_apply_ptr<registrar, Ts...>::value reg_type; // The group of registrars
    typedef std::array<ident, cardinality> value_type; // The internal representation

    reg_type registrars;
    std::vector<value_type> data;

    relation_buffer() = delete;
    relation_buffer(const reg_type& regs) : registrars(regs), data() {}

    size_t size() const {
        return data.size();
    }

    void add(const outer_type& row){
        std::array<ident, cardinality> tmp;
        template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts), Ts...>
            ::register_tuple(row, registrars, tmp);
        data.push_back(tmp);
    }

    outer_type retrieve(size_t idx) const {
        outer_type ret;
        template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts), Ts...>
            ::unregister_array(ret, registrars, data[idx]);
        return ret;
    }

    value_type& operator[](const size_t i){
        return data[i];
    }

    bool from_csv(const std::string& csv_path){
        io::CSVReader<cardinality, io::trim_chars<>,
            io::no_quote_escape<','>, io::throw_on_overflow,
            io::empty_line_comment> in(csv_path);
        try{
            outer_type tmp;
            while(true){
                if(!in.read_row_tuple(tmp)) break;
                add(tmp);
            }
            return true;
        } catch(const io::error::base& err){
            logger::error("CSV Read error: ", err.what());
            return false;
        }
    }

    void to_csv(std::ostream& os) const {
        for(size_t i=0; i<size(); i++){
            template_internals
                ::buffer_helper<reg_type, value_type, sizeof...(Ts)-1, Ts...>
                ::stream_tuple(os, retrieve(i));
            os << std::endl;
        }
    }

};

} // end namespace cflr

#endif /* __RELATION_BUFFER_H__ */
