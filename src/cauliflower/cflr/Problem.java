package cauliflower.cflr;

import cauliflower.util.TarjanScc;

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

    /**
     * Determine the order of label dependencies for this problem, using
     * Tarjan's strongly connected components algorithm
     *
     * @return a List of Lists of Integers, where label indices in list A may
     * depend on those in B if B <= A
     */
    public List<List<Integer>> getLabelDependencyOrdering(){
        return new TarjanScc(labels.size(), Stream.generate(ArrayList<Integer>::new).limit(labels.size()).collect(Collectors.toList())).result;
    }

}
