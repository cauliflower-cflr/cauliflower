package cauliflower.representation;

import cauliflower.util.CFLRException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Problem
 * <p>
 * Author: nic
 * Date: 30/05/16
 */
public class Problem {

    private final String SPACE = " ";

    public final Piece.Pieces<Domain> vertexDomains = new Piece.Pieces<>();
    public final Piece.Pieces<Domain> fieldDomains = new Piece.Pieces<>();
    public final Piece.Pieces<Label> labels = new Piece.Pieces<>();

    private final List<Rule> rules = new ArrayList<>();

    public Domain addVertexDomain(String domainName) throws CFLRException{
        return new Domain(vertexDomains, domainName);
    }

    public Domain addFieldDomain(String domainName) throws CFLRException{
        return new Domain(fieldDomains, domainName);
    }

    public Label addLabel(String name, String srcD, String dstD, List<String> fds) throws CFLRException {
        List<Domain> flst = new ArrayList<>();
        for(String s : fds) flst.add(fieldDomains.get(s));
        return new Label(labels, name, vertexDomains.get(srcD), vertexDomains.get(dstD), flst);
    }

    /**
     * This method is package local, to make a rule use the Rule.RuleBuilder
     * @param r the rule to be added
     */
    /* local */ void addRule(Rule r){
        rules.add(r);
    }

    public int getNumRules(){
        return rules.size();
    }

    public Rule getRule(int i){
        return rules.get(i);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("V=").append(vertexDomains.toString()).append(SPACE)
                .append("F=").append(fieldDomains.toString()).append(SPACE)
                .append("L=").append(labels.toString()).append(SPACE)
                .append("R=").append(rules.toString()).append(SPACE)
                .toString();
    }
}
