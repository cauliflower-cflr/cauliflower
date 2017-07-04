
/* Inputs */

param Bc, integer, >= 2;
param Rc, integer, >= 1;
set E, dimen 2;

/* Constants */

set V := (setof{(i,j) in E} i) union (setof{(i,j) in E} j);
set B, default {1..Bc};
set R, default {1..Rc};


/*------------------*/

var Place{r in R, v in V, b in B}, binary;
var BEdge{r in R, b in B, c in B}, >= 0;
var BPath{r in R, b in B, c in B}, >= 0;
var Appro{r in R, u in V, v in V}, >= 0;
var Inter{u in V, v in V}, >= 0;

var WBin{r in R, b in B}, >= 0;
var WRun{r in R}, >= 0;

/*------------------*/

s.t. all_places{r in R, v in V}:
    sum{b in B} Place[r,v,b] = 1;

s.t. bin_edges{r in R, (u,v) in E, b in B, c in B}:
    BEdge[r,b,c] >= Place[r,u,b] + Place[r,v,c] - 1;

s.t. transitive_base{r in R, b in B, c in B}:
    BPath[r,b,c] >= BEdge[r,b,c];

s.t. transitive_induc{r in R, b in B, c in B, d in B}:
    BPath[r,b,d] >= BPath[r,b,c] + BPath[r,c,d] - 1;

s.t. approx_internal{r in R, u in V, v in V, b in B}:
    Appro[r,u,v] >= Place[r,u,b] + Place[r,v,b] - 1;

s.t. approx_external{r in R, u in V, v in V, b in B, c in B}:
    Appro[r,u,v] >= Place[r,u,b] + Place[r,v,c] + BPath[r,b,c] - 2;

s.t. intersection{u in V, v in V}:
    Inter[u,v] >= (sum{r in R} Appro[r,u,v]) - (Rc - 1);

/*------------------*/

s.t. bin_weight{r in R, b in B}:
    WBin[r,b] = sum{v in V} Place[r,v,b];

s.t. run_weight{r in R}:
    WRun[r] = sum{v in V, b in B} (v * Bc + b) * Place[r,v,b];

s.t. bin_order{r in R, b in B, c in B : b < c}:
    WBin[r,b] >= WBin[r,c];

s.t. run_order{r1 in R, r2 in R : r1 < r2}:
    WRun[r1] >= WRun[r2];

/*------------------*/

minimize size_of_output_tc:
    sum{u in V, v in V} Inter[u,v];

/*------------------*/

solve;
printf "      run |       var |       bin |\n";
for{v in V, r in R, b in B : Place[r,v,b] > 0}{
    printf "%10s| %10s| %10s| PL\n", r, v, b;
}

printf "TCSIZE=%d\n", size_of_output_tc;

for{s in V, t in V : Inter[s,t] > 0}{
    printf "(%s,%s) TC\n", s, t;
}

