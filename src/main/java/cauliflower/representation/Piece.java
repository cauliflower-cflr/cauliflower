package cauliflower.representation;

import cauliflower.util.CFLRException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Piece {
    public final int index;
    public final String name;

    @SuppressWarnings("unchecked") // since the type of this isnt guarantted to be T, but in practice it is
    protected <T extends Piece> Piece(Pieces<T> pieces, String nm) throws CFLRException{
        name = nm;
        index = pieces.add((T)this);
    }

    protected String toStringDesc(){
        return this.name;
    }

    protected String toStringTy(){
        return "(" + this.getClass().getSimpleName().chars().filter(i -> i >= 'A' && i <= 'Z').mapToObj(i -> "" + (char)i).collect(Collectors.joining()) + " " + index + ")";
    }

    @Override
    public String toString(){
        return toStringDesc() + toStringTy();
    }

    public static class Pieces<T extends Piece>{
        private final List<T> l = new ArrayList<>();
        private final Map<String, Integer> m = new HashMap<>();
        public boolean has(int i){
            return i < l.size() && i >= 0;
        }
        public boolean has(String n){
            return m.containsKey(n);
        }
        public T get(int i) throws CFLRException{
            if(!has(i)) throw new CFLRException("Piece beyond range: " + i);
            return l.get(i);
        }
        public T get(String n) throws CFLRException{
            if(!has(n)) throw new CFLRException("No piece with name: " + n);
            return l.get(m.get(n));
        }
        private int add(T item) throws CFLRException {
            if(m.containsKey(item.name)) throw new CFLRException("Duplicate: " + item.toString());
            m.put(item.name, l.size());
            l.add(item);
            return l.size()-1;
        }

        @Override
        public String toString() {
            return l.toString();
        }
    }
}
