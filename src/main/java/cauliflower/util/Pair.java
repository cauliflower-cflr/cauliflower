package cauliflower.util;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public T getFirst(){
        return first;
    }

    public U getSecond(){
        return second;
    }

    @Override
    public String toString(){
        return String.format("(%s,%s)", first, second);
    }

    public <X> X map(BiFunction<T, U, X> f){
        return f.apply(first, second);
    }

    public static <A, B, X> Function<Pair<A, B>, X> forward(BiFunction<A, B, X> bf){
        return (Pair<A, B> p) -> p.map(bf);
    }

    public static <X, Y> int primaryOrder(Pair<? extends Comparable<X>, Y> a, Pair<? extends X,Y> b){
        return a.first.compareTo(b.first);
    }

    public static <X, Y> int secondaryOrder(Pair<X,? extends Comparable<Y>> a, Pair<X,? extends Y> b){
        return a.second.compareTo(b.second);
    }

    public static <X, Y> int InversePrimaryOrder(Pair<? extends X,Y> a, Pair<? extends Comparable<X>, Y> b){
        return primaryOrder(b, a);
    }

    public static <X, Y> int InverseSecondaryOrder(Pair<X,? extends Y> a, Pair<X,? extends Comparable<Y>> b){
        return secondaryOrder(b, a);
    }
}
