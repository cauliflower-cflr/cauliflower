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

}//end namespace cflr

#endif /* __UTILITIES_H__ */
