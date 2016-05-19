/* 
 *                               Utilities.h
 * 
 * Author: Nic H.
 * Date: 2015-Sep-07
 * 
 * Miscelaneous non-cflr kind of utilities (though still in my namespace
 */

#ifndef __UTILITIES_H__
#define __UTILITIES_H__

#include <unordered_map>
#include <vector>

namespace cflr {

// Globally used identifier type, 64-bit unsigned int
typedef uint64_t ident;

// Generates and stores a mapping of T to the ident-type
template<typename T>
struct registrar{
    registrar() : ti(), it() {}
    size_t size() const { return it.size(); }
    T get(ident i) const { return it[i]; }
    ident get_or_add(const T& t) {
        if(ti.find(t) == ti.end()){
            ti.insert(std::pair<T, ident>(t, it.size()));
            it.push_back(t);
            return it.size()-1;
        }
        return ti.find(t)->second;
    }
private:
    std::unordered_map<T, ident> ti;
    std::vector<T> it;
};

/// statically compute the logarithm of i to base BASE
template<unsigned BASE>
constexpr unsigned log(unsigned i){
    return i < BASE ? 0 : log<BASE>(i/BASE) + 1;
}

/// statically compute the square root of i
template<typename I> constexpr I csqrt_idx(unsigned idx, I cur){
    return cur + (1 << (idx-1));
}
template<typename I> constexpr I csqrt_help(unsigned idx, I cur, I target){
    return idx == 0 ? cur : csqrt_help(idx-1, csqrt_idx(idx, cur)*csqrt_idx(idx, cur) > target ? cur: csqrt_idx(idx, cur), target);
}
template<typename I> constexpr I csqrt(I num){
    return csqrt_help<I>(sizeof(I)*4, 0, num);
}

/// dependency_info, generate the dependency ordering, given a list of dependencies
/// this is runtme information (template subsystem cant handle Tarjan's)
struct dependency_info {
    typedef std::pair<unsigned, unsigned> dep_t;
    typedef std::vector<std::pair<unsigned, unsigned>> dep_list_t;
    typedef std::vector<std::vector<unsigned>> dep_res_t;
    static dep_res_t find_dependencies(const dep_list_t&);
};

} // end namespace cflr

#endif /* __UTILITIES_H__ */
