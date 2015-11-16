/* 
 *                                  adt.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 * 
 * Base class for Abstract Data Types.  Note that the problem is templated
 * with the concrete ADT, so publicly inheriting this class is purely
 * cosmetic, and allows certain operations to be default-defined.
 */

#ifndef __ADT_H__
#define __ADT_H__

#include "utilities.h"

namespace cflr{

template<typename Self, typename Iter>
struct adt{
    typedef Iter iterator;
    // Queries
    virtual bool empty() = 0;
    // Methods for importing data
    virtual void initialise_import() = 0;
    virtual void import(ident, ident) = 0;
    virtual void finalise_import() = 0;
    // Methods for exporting data
    virtual iterator begin() const = 0;
    virtual iterator end() const = 0;
    // Relational Methods
    virtual void union_copy(const Self&) = 0;
    virtual void union_absorb(Self&) = 0;
    virtual void compose(const Self&, Self&) const = 0;
    virtual void difference(const Self&) = 0;
};

} // end namespace cflr

#endif /* __ADT_H__ */
