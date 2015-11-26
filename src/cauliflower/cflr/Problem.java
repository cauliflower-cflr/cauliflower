package cauliflower.cflr;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Problem.java
 *
 * Stores the specification of the CFLR problem,
 * specifically, stores the labels (and their field domains) as well as the rules.
 *
 * Created by nic on 25/11/15.
 */
public class Problem {

    public final int numDomains;
    public final List<Label> labels;
    public final List<Rule> rules;
    public final Set<Integer> fields;

    public Problem(int numDomains, List<Label> labels, List<Rule> rules) {
        this.numDomains = numDomains;
        this.labels = labels;
        this.rules = rules;
        this.fields = this.labels.stream().flatMap(l -> l.fDomains.stream()).collect(Collectors.toCollection(HashSet<Integer>::new));
    }

    private <T> ArrayList<T> lblDup(T t){
        return Stream.generate(() -> t).limit(labels.size()).collect(Collectors.toCollection(ArrayList<T>::new));
    }

    /**
     * Determine the order of label dependencies for this problem, using
     * Tarjan's strongly connected components algorithm
     *
     * @return a List of Lists of Integers, where label indices in list A may
     * depend on those in B if B <= A
     */
    public List<List<Integer>> getLabelDependencyOrdering(){
        return new TarjanScc().result;
    }

    private class TarjanScc{
        private int curIndex = 0;
        private List<Integer> index = lblDup(-1);
        private List<Integer> lowLink = lblDup(-1);
        private List<Boolean> onStack = lblDup(false);
        private Stack<Integer> stack = new Stack<>();
        private List<List<Integer>> result = new ArrayList<>();
        private List<List<Integer>> successors = Stream.generate(ArrayList<Integer>::new).limit(labels.size()).collect(Collectors.toList());

        private TarjanScc(){
            for( Rule r : rules) successors.get(r.head.label).addAll(r.dependencies.stream().map(l -> l.label).collect(Collectors.toList()));
            for(int v=0; v<labels.size(); v++) if(index.get(v) == -1) strongConnect(v);
        }

        private void strongConnect(int v){
            index.set(v, curIndex);
            lowLink.set(v, curIndex);
            curIndex++;
            stack.push(v);
            onStack.set(v, true);
            for(int w : successors.get(v)){
                if(index.get(w) == -1){
                    strongConnect(w);
                    lowLink.set(v, Math.min(lowLink.get(v), lowLink.get(w)));
                } else if(onStack.get(w)){
                    lowLink.set(v, Math.min(lowLink.get(v), index.get(w)));
                }
            }
            if(lowLink.get(v) == index.get(v)){
                ArrayList<Integer> scc = new ArrayList<>();
                int w = -1;
                while(w != v) {
                    w = stack.pop();
                    onStack.set(w, false);
                    scc.add(w);
                }
                result.add(scc);
            }
        }
    }

}
