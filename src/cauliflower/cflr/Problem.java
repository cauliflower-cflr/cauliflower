package cauliflower.cflr;

import cauliflower.util.CFLRException;
import cauliflower.util.TarjanScc;

import java.io.IOException;
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
        for(Rule r : rules){
            r.head.fieldDomains = labels.get(r.head.label).fDomains;
            for(Rule.Lbl l : r.dependencies) l.fieldDomains = labels.get(l.label).fDomains;
        }
    }

    /**
     * Determine the order of label dependencies for this problem, using
     * Tarjan's strongly connected components algorithm
     *
     * @return a List of Lists of Integers, where label indices in list A may
     * depend on those in B if B <= A
     */
    public List<List<Integer>> getLabelDependencyOrdering(){
        List<List<Integer>> successors = Stream.generate(ArrayList<Integer>::new).limit(labels.size()).collect(Collectors.toList());
        rules.stream().forEach(r -> successors.get(r.head.label).addAll(r.dependencies.stream().map(l -> l.label).collect(Collectors.toList())));
        return new TarjanScc(labels.size(), successors).result;
    }

    /**
     * Determine the which field a rule's field identifiers refer to
     * @param r the index of the rule we are checking
     * @return A map from identifier -> []{domain, frequency} indicies
     * @throws CFLRException if the mapping is invalid for any reason
     */
    public Map<Integer, int[]> ruleFieldDomainMapping(int r) throws CFLRException {
        List<Rule.Lbl> dependencies = Stream
                .concat(Stream.of(rules.get(r).head), rules.get(r).dependencies.stream())
                .collect(Collectors.toCollection(ArrayList<Rule.Lbl>::new));
        Map<Integer, int[]> ret = new HashMap<>();
        for(Rule.Lbl d : dependencies){
            if(d.fields.size() != labels.get(d.label).fDomains.size()){
                throw new CFLRException("Field cardinality missmatch in rule " + rules.get(r));
            }
            for(int i=0; i<d.fields.size(); i++){
                if(ret.containsKey(d.fields.get(i))){
                    if(ret.get(d.fields.get(i))[0] != labels.get(d.label).fDomains.get(i)){
                        throw new CFLRException("Field identifier missmatch in rule " + rules.get(r));
                    } else {
                        ret.get(d.fields.get(i))[1]++;
                    }
                } else {
                    ret.put(d.fields.get(i), new int[]{labels.get(d.label).fDomains.get(i), 1});
                }
            }
        }
        return ret;
    }

}
