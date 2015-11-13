/* 
 *                                  rule.h
 * 
 * Author: Nic H.
 * Date: 2015-Nov-13
 * 
 * Meta-templates for the rule.  Note how this rule forces us to be in IN-
 * nf (i.e. intersection and negation clauses appear alone).
 */

#ifndef __RULE_H__
#define __RULE_H__

#include "label.h"

namespace cflr{

//the significand templates
template<unsigned, unsigned> struct isect {};
template<unsigned> struct ngate {};
template<unsigned> struct rev {};
template<unsigned> struct fwd {};
//rule base type
template<unsigned, typename...> struct rule {};

// -// epsilon rule
// -template<unsigned LI, unsigned...LFs>
// -struct rule<label<LI, LFs...>> {
// -};
// -// sequence rule
// -template<unsigned LI, unsigned...LFs, unsigned R1I, unsigned...R1Fs, typename...Rest>
// -struct rule<label<LI, LFs...>, label<R1I, R1Fs...>, Rest...> {
// -};
// -// intersection rule
// -template<unsigned LI, unsigned...LFs, unsigned RAI, unsigned...RAFs, unsigned RBI, unsigned...RBFs>
// -struct rule<label<LI, LFs...>, isect<label<RAI, RAFs...>, label<RBI, RBFs...>>> {
// -};
// -// negation rule
// -template<unsigned LI, unsigned...LFs, unsigned RI, unsigned...RFs>
// -struct rule<label<LI, LFs...>, ngate<label<RI, RFs...>>> {
// -};
// -// transitive-rule (Future Work)

} // end namespace cflr

#endif /* __RULE_H__ */
