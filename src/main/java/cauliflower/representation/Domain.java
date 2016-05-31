package cauliflower.representation;

import cauliflower.util.CFLRException;

/**
 * Vertex and field domains
 */
public class Domain extends Piece{

    public Domain(Pieces<Domain> group, String nm) throws CFLRException{
        super(group, nm);
    }
}
