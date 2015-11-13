/* 
 *                                 label.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-13
 * 
 * Logical reference for a label used in the CFLR problem definition.
 * Labels are templated by the field (indices) which they refer to, i.e.
 * elemnts of the tuple of all field registrars (so that they can work out
 * their own field_volume)
 */

#ifndef __LABEL_H__
#define __LABEL_H__

namespace cflr{

template<unsigned...Fs> struct label {};

} // end namespace cflr

#endif /* __LABEL_H__ */
