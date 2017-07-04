tuple Edge{
	key int s;
	key int t; 
}

{Edge} E = ...;
{int} V = {s | <s,t> in E} union {t | <s,t> in E};

int Bc = ...;
int Rc = ...;

dvar float+ reaches[V][V];
dexpr float tc_size = sum(i,j in V) reaches[i][j];

minimize tc_size;
subject to {

	forall(v in V) reflexive:
	  reaches[v][v] >= 1;
	  
	forall(e in E) transitive:
	  reaches[e.s][e.t] >= 1;
	  
	forall(i,j,k in V) closure:
	  reaches[i][k] >= reaches[i][j] + reaches[j][k] - 1;

}

execute {
	writeln("RUNS=",Rc)
	writeln("BINS=",Bc)
	writeln("TC_SIZE=",tc_size)
	for(var s in V){
		for(var t in V){
			if(reaches[s][t] != 0){
				writeln("(", s, ", ", t, ") TC");
 			}
		}
	}
}