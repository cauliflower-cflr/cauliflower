tuple Edge{
	key int s;
	key int t; 
}

{Edge} E = ...;
{int} V = {s | <s,t> in E} union {t | <s,t> in E};

int Rc = ...;
int Bc = ...;

range R = 1..Rc;
range B = 1..Bc;

dvar boolean Place[R][V][B];
dvar float+ BEdge[R][B][B];
dvar float+ BPath[R][B][B];
dvar float+ Appro[R][V][V];
dvar float+ Final[V][V];
dexpr float place_weight[r in R][b in B] = sum(v in V) Place[r][v][b]*v;
dexpr float run_weight[r in R] = sum(b in B) (place_weight[r][b]*Bc + b);
dexpr float tc_size = sum(u,v in V) Final[u][v];

minimize tc_size;
subject to {

	forall(r in R, v in V) valid_place:
		sum(b in B) Place[r][v][b] == 1;
	
	forall(r in R, b1,b2 in B, e in E) bin_edges:
		BEdge[r][b1][b2] >= Place[r][e.s][b1] + Place[r][e.t][b2] - 1;
		
	forall(r in R, b in B) reflexive:
		BPath[r][b][b] >= 1;
		
	forall(r in R, b1,b2 in B) transitive:
		BPath[r][b1][b2] >= BEdge[r][b1][b2];
		
	forall(r in R, b1,b2,b3 in B) closure:
		BPath[r][b1][b3] >= BPath[r][b1][b2] + BPath[r][b2][b3] - 1;
		
	forall(r in R, u,v in V, b in B) internal:
		Appro[r][u][v] >= Place[r][u][b] + Place[r][v][b] - 1;
	
	forall(r in R, u,v in V, b1,b2 in B) external:
		Appro[r][u][v] >= Place[r][u][b1] + Place[r][v][b2]  + BPath[r][b1][b2] - 2;
		
	forall(u,v in V) intersection:
		Final[u][v] >= sum(r in R) Appro[r][u][v] - (Rc - 1);
		
	forall(r in R, ordered b1,b2 in B) order_bins:
		place_weight[r][b1] >= place_weight[r][b2];
		
	forall(ordered r1,r2 in R) order_runs:
		run_weight[r1] >= run_weight[r2];
}

execute {
	writeln("RUNS=",Rc)
	writeln("BINS=",Bc)
	for(var r in R){
		for(var v in V){
			for(var b in B){
				if(Place[r][v][b] != 0){
					writeln(r," ",v," ",b," PL");			
				}
			}
		}
	}
	writeln("TC_SIZE=",tc_size)
	for(var s in V){
		for(var t in V){
			if(Final[s][t] != 0){
				writeln("(", s, ", ", t, ") TC");
 			}
		}
	}
}