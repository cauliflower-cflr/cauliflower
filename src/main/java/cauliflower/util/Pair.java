package cauliflower.util;

/**
 * C++4lyf
 */
public class Pair<T, U> {
    public T first;
    public U second;
    public Pair(T f, U s){
        first = f;
        second = s;
    }
    @Override
    public String toString(){
        return String.format("(%s,%s)", first.toString(), second.toString());
    }
}
