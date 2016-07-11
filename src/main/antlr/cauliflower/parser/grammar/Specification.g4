grammar Specification; // rename to distinguish from Expr.g4

spec
    :   (def ';')+                      # specification
    |                                   # emptySpecification
    ;

def
    :   lbl '<-' from=dom '.' to=dom    # typeDef
    |   lbl '->' expr                   # ruleDef
    ;

expr
    :   lhs=expr ',' rhs=term   # chainExpr
    |   term                    # unitExpr
    ;

term
    :   '(' expr ')'            # subTerm
    |   '!' term                # negateTerm
    |   '-' term                # reverseTerm
    |   lhs=term '&' rhs=term   # intersectTerm
    |   lbl (priority)?         # labelTerm
    |   '~'                     # epsilonTerm
    ;

lbl
    :   ID fld*                 # label
    ;

dom
    :   ID                      # domain
    ;

fld
    :'[' ID ']'                 # field
    ;

priority
    :'{' INT '}'                # prior
    ;

ID  :   [a-zA-Z_][a-zA-Z0-9_]* ; // identifiers
INT :   '-'?[0-9][0-9]* ;
WS  :   [ \t\n\r]+ -> skip ; // toss out whitespace
