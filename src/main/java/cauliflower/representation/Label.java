package cauliflower.representation;

import cauliflower.util.CFLRException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A single relation which is stored as a terminal/nonterminal in memory
 */
public class Label extends Piece {

    public final Domain srcDomain;
    public final Domain dstDomain;
    public final int fieldDomainCount;
    public final List<Domain> fieldDomains;

    /* local */ Label(Pieces<Label> group, String nm, Domain src, Domain dst, List<Domain> fld) throws CFLRException{
        super(group, nm);
        this.srcDomain = src;
        this.dstDomain = dst;
        this.fieldDomains = fld;
        this.fieldDomainCount = fieldDomains.size();
    }

    @Override
    public String toStringDesc() {
        return name + fieldDomains.stream().map(d -> "[" + d.name + "]").collect(Collectors.joining()) + "<-" + srcDomain.name + "." + dstDomain.name;
    }
}
