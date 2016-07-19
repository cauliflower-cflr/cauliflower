package cauliflower.util;

import java.util.Comparator;

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
        return String.format("(%s,%s)", first, second);
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
