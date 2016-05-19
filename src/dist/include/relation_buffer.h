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
    static inline unsigned field_volume(const TReg& regs){
        return buffer_helper<TReg, TArr, I-1, Tups...>::field_volume(regs)*std::get<I+1>(regs)->size();
    }
    static inline unsigned field_volume_index(const TReg& regs, const TArr& arr){
        return buffer_helper<TReg, TArr, I-1, Tups...>::field_volume_index(regs, arr)*std::get<I+1>(regs)->size() + arr[I+1];
    }
    static inline void field_index_to_volume(unsigned amt, const TReg& regs, TArr& arr){
        arr[I+1] = amt%std::get<I+1>(regs)->size();
        buffer_helper<TReg, TArr, I-1, Tups...>::field_index_to_volume(amt/std::get<I+1>(regs)->size(), regs, arr);
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
    static inline unsigned field_volume(const TReg& regs){
        return 1;
    }
    static inline unsigned field_volume_index(const TReg& regs, const TArr& arr){
        return 0;
    }
    static inline void field_index_to_volume(unsigned idx, const TReg& regs, TArr& arr){
        // do nothing
    }
};
} // end namespace template_internals

template<typename...Ts>
struct relation_buffer{

    static const unsigned cardinality = sizeof...(Ts);
    static_assert(cardinality >= 2, "Relations must have at least binary cardinality");

    typedef std::tuple<Ts...> outer_type; // The external representation
    typedef std::tuple<registrar<Ts>*...> reg_type; // The group of registrars
    typedef std::array<ident, cardinality> value_type; // The internal representation

    reg_type registrars;
    std::vector<value_type> data;

    relation_buffer() = delete;
    relation_buffer(const reg_type& regs) : registrars(regs), data() {}

    size_t size() const {
        return data.size();
    }

    size_t field_volume() const {
        return template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts)-2, Ts...>
            ::field_volume(registrars);
    }
    size_t index_volume(size_t idx) const {
        return template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts)-2, Ts...>
            ::field_volume_index(registrars, data[idx]);
    }

    void add(const outer_type& row){
        std::array<ident, cardinality> tmp;
        template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts), Ts...>
            ::register_tuple(row, registrars, tmp);
        data.push_back(tmp);
    }
    void add_internal(ident from, ident to, size_t volume){
        value_type add;
        add[0] = from;
        add[1] = to;
        template_internals::buffer_helper<reg_type, value_type, sizeof...(Ts)-2, Ts...>
            ::field_index_to_volume(volume, registrars, add);
        data.push_back(add);
    }

    outer_type retrieve(size_t idx) const {
        outer_type ret;
        template_internals
            ::buffer_helper<reg_type, value_type, sizeof...(Ts), Ts...>
            ::unregister_array(ret, registrars, data[idx]);
        return ret;
    }

    const value_type& operator[](const size_t i) const {
        return data[i];
    }

    typename std::vector<value_type>::const_iterator begin() const {
        return data.cbegin();
    }

    typename std::vector<value_type>::const_iterator end() const {
        return data.cend();
    }

    bool from_csv(const std::string& csv_path){
        try{
            io::CSVReader<cardinality, io::trim_chars<>,
                io::no_quote_escape<','>, io::throw_on_overflow,
                io::empty_line_comment> in(csv_path);
                outer_type tmp;
            try{
                while(true){
                    if(!in.read_row_tuple(tmp)) break;
                    add(tmp);
                }
                return true;
            } catch(const io::error::base& err){
                std::cerr << "CSV Read error: " <<  err.what() << std::endl;
                return false;
            }
        } catch(const io::error::base& err){
            // silently fail when file does not exist
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

namespace template_internals {
template<typename, unsigned> struct rg_select_h;
template<unsigned If, unsigned...IfRest, unsigned It> struct rg_select_h<ulist<If, IfRest...>, It> {
    template<typename Tf, typename Tt> static inline void select(Tf& from, Tt& to){
        std::get<It>(to) = &(std::get<If>(from));
        rg_select_h<ulist<IfRest...>, It+1>::select(from, to);
    }
};
template<unsigned It> struct rg_select_h<ulist<>, It> {
    template<typename Tf, typename Tt> static inline void select(Tf& from, Tt& to){
        // do nothing
    }
};
template<typename Ul, typename...Ts> struct rg_select_return {
    typedef typename as_tm<typename project_tm<tlist<registrar<Ts>*...>, Ul>::result, std::tuple>::result type;
};
template<unsigned Cur> struct rg_volumes_h {
    template<typename T, unsigned Len> static inline void volumes(const T& group, std::array<size_t, Len>& arr){
        arr[Len-Cur] = std::get<Len-Cur>(group).size();
        rg_volumes_h<Cur-1>::template volumes<T, Len>(group, arr);
    }
};
template<> struct rg_volumes_h<0> {
    template<typename T, unsigned Len> static inline void volumes(const T& group, std::array<size_t, Len>& arr){}
};
}

/// registrar_group, used to store the group of all registrars used by a probelem
/// There are utility functions for projecting a subset of the registrars to a new tuple
/// TODO, this should be a tlist
template<typename...Ts> struct registrar_group {
    static const unsigned size = sizeof...(Ts);
    typedef std::tuple<registrar<Ts>...> group_t;
    typedef std::array<size_t, size> volume_t;
    group_t group;
    template<unsigned...Is> 
    inline typename template_internals::rg_select_return<ulist<Is...>, Ts...>::type select(){ 
        typename template_internals::rg_select_return<ulist<Is...>, Ts...>::type ret;
        template_internals::rg_select_h<ulist<Is...>, 0>::select(group, ret);
        return ret;
    }
    inline volume_t volumes(){
        volume_t ret;
        template_internals::rg_volumes_h<size>::template volumes<group_t, size>(group, ret);
        return ret;
    }
};

} // end namespace cflr

#endif /* __RELATION_BUFFER_H__ */
