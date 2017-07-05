package cauliflower.optimiser;

public enum Passes {

    promote_terminals,
    promote_redundant,
    promote_summary,
    promote_chomsky,
    filter,
    order;

    // the manual mapping is more flexible, since i can map different human
    // readable names to different pass constructor calls
    public Transform getTransform(){
        switch (this){
            case promote_terminals: return new SubexpressionTransformation.TerminalChain();
            case promote_redundant: return new SubexpressionTransformation.RedundantChain();
            case promote_summary:   return new SubexpressionTransformation.SummarisingChain();
            case promote_chomsky:   return new SubexpressionTransformation.ChomskyChain();
            case filter:            return new RelationFilterTransformation();
            case order:             return new EvaluationOrderTransformation(true, true);
            default:                throw new RuntimeException("Unreachable");
        }
    }

}
