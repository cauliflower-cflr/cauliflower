package cauliflower.util;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.*;

public class Streamer {

    @SafeVarargs
    public static <T> List<T> concat(List<? extends T> ... ins){
        return Stream.of(ins).flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Zips two streams according to a provided BiFunction
     * Code stolen shamelessly from http://stackoverflow.com/a/23529010
     * @param a The 'left' stream
     * @param b The 'right' stream
     * @param zipper A bi-function for elements of left/right
     * @param <A> The type of the left stream
     * @param <B> The type of the right stream
     * @param <C> The type of the zipped streams
     * @return A stream of values from left and right streams applied to the zipper bifunction
     */
    public static<A, B, C> Stream<C> zip(Stream<? extends A> a,
                                         Stream<? extends B> b,
                                         BiFunction<? super A, ? super B, ? extends C> zipper) {
        Objects.requireNonNull(zipper);
        @SuppressWarnings("unchecked")
        Spliterator<A> aSpliterator = (Spliterator<A>) Objects.requireNonNull(a).spliterator();
        @SuppressWarnings("unchecked")
        Spliterator<B> bSpliterator = (Spliterator<B>) Objects.requireNonNull(b).spliterator();

        // Zipping looses DISTINCT and SORTED characteristics
        int both = aSpliterator.characteristics() & bSpliterator.characteristics() &
                ~(Spliterator.DISTINCT | Spliterator.SORTED);
        int characteristics = both;

        long zipSize = ((characteristics & Spliterator.SIZED) != 0)
                ? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
                : -1;

        Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
        Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
        Iterator<C> cIterator = new Iterator<C>() {
            @Override
            public boolean hasNext() {
                return aIterator.hasNext() && bIterator.hasNext();
            }

            @Override
            public C next() {
                return zipper.apply(aIterator.next(), bIterator.next());
            }
        };

        Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
        return (a.isParallel() || b.isParallel())
                ? StreamSupport.stream(split, true)
                : StreamSupport.stream(split, false);
    }

    /**
     * Specialisation for zipping with a stream element's index
     * @param a the stream to enumerate
     * @param user the function taking arg1= elemnts of "a", and arg2=the index
     * @param <A> the type of the input stream
     * @param <B> the type of the output stream
     * @return a stream of elements zipped with their index
     */
    public static <A, B> Stream<B> enumerate(Stream<? extends A> a, BiFunction<? super A, ? super Integer, ? extends B> user){
        return zip(a, IntStream.iterate(0, i->i+1).boxed(), user);
    }

    private static <A> LinkedList<A> permuteInternal(long curPerm, LinkedList<A> base){
        if(base.size() == 0){
            return new LinkedList<>();
        } else {
            A elem = base.remove((int) (curPerm % base.size()));
            LinkedList<A> ret = permuteInternal(curPerm / (base.size()+1), base);
            ret.add(0, elem);
            return ret;
        }
    }

    public static <A> List<A> permute(long permuteIdx, List<A> base){
        //ArrayList<A> ret = Stream.generate(() -> (A)null).limit(base.size()).collect(Collectors.toCollection(ArrayList::new));
        //zip(permuteIndices(permuteIdx, base.size(), new ArrayList<>(base.size())).stream(),
        //        base.stream(), Pair::new)
        //        .peek(p -> System.out.println(p.first + " - " + p.second))
        //        .forEach(p -> ret.set(p.first, p.second));
        return permuteInternal(permuteIdx, new LinkedList<>(base));
    }

    public static <A> Stream<List<A>> choices(List<A> base){
        long lim = 1;
        for(int i=0; i<base.size() && i < 62; i++){ // magic number prevents going outside of long bounds
            lim*=2;
        }
        return Stream.iterate(BigInteger.ZERO, i -> i.add(BigInteger.ONE)).limit(lim).map(i -> choice(i, base));
    }

    public static <A> List<A> choice(BigInteger chosen, List<? extends A> base){
        return zip(IntStream.range(0, base.size()).mapToObj(chosen::testBit), base.stream(), Pair::new).filter(p -> p.first).map(p -> p.second).collect(Collectors.toList());
    }

    /**
     * returns the list of indicies to draw the permuations from a hypothetical list
     */
    public static List<Integer> permuteIndices(long permutation, int count){
        return permuteInternal(permutation, IntStream.range(0, count).boxed().collect(Collectors.toCollection(LinkedList::new)));
    }

    /**
     * @param size the number of items in the desired index stream
     * @return a stream of all the permutations of the numbers 0 to (size-1), like [0,1,2],[0,2,1],[1,0,2]...
     */
    public static Stream<List<Integer>> streamPermutations(int size){
        return LongStream.range(0, factorial(size)).mapToObj(l -> permuteIndices(l, size));
    }

    /**
     * When the number of permutations is too large, we restrict to left-deep permutations a'la Selinger's technique
     * TODO actually enumerate the LDP's and dont just filter all permutations
     * @param size the number of items in the desired index stream
     * @return a stream of all the joined permutations of the numbers 0 to (size-1), like [0,1,2],[1,0,2],[1,2,0]...
     */
    public static Stream<List<Integer>> streamJoinedPermutations(int size){
        return streamPermutations(size).filter(Streamer::isJoinedPermutation);
    }
    private static boolean isJoinedPermutation(List<Integer> perm){
        int l=-1, h=-1;
        for(int i: perm){
            if(l == -1){
                l = i;
                h = i;
            } else if(i == l-1){
                l = i;
            } else if(i == h+1){
                h = i;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * @param i The number you want the factorial of
     * @return the factorial of i, or throws a runtime exception if overflow would occur
     */
    public static long factorial(int i){
        if(i > 20) throw new RuntimeException("Factorial of " + i + " is too big");
        long ret = 1;
        for(int j=2; j<=i; j++) ret *= j;
        return ret;
    }
}
