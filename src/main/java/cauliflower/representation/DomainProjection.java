package cauliflower.representation;

import cauliflower.util.CFLRException;

import java.io.IOException;

/**
 * Records the projection of a (field)domain via a variable
 */
public class DomainProjection extends Piece{

    public final Domain referencedField;

    public DomainProjection(Pieces<DomainProjection> group, String nm, Domain field) throws CFLRException{
        super(group, nm);
        this.referencedField = field;
    }
}
