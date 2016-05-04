
/* Inputs */

param Bc, integer, >= 2;
param Rc, integer, >= 1;
set E, dimen 2;

/* Constants */

set V := (setof{(i,j) in E} i) union (setof{(i,j) in E} j);
set B, default {1..Bc};
set R, default {1..Rc};


/*-----------*/

var Inter{u in V, v in V}, binary;

s.t. reflexive{v in V}:
    Inter[v,v] = 1;

s.t. transitive{u in V, v in V, w in V}:
    Inter[u,w] >= Inter[u,v] + Inter[v,w] - 1;

s.t. closure{(u, v) in E}:
    Inter[u,v] = 1;

minimize size_of_output_tc:
    sum{u in V, v in V} Inter[u,v];

solve;

printf "TCSIZE=%d\n", size_of_output_tc;

for{s in V, t in V : Inter[s,t] > 0}{
    printf "(%s,%s) TC\n", s, t;
}

