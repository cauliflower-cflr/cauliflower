package cauliflower;

import cauliflower.util.Streamer;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * TestMisc
 * <p>
 * Author: nic
 * Date: 11/07/16
 */
public class TestMisc {

    @Test
    public void testPermuteIndices(){
        assertEquals(Arrays.asList(0,1), Streamer.permuteIndices(0, 2));
        assertEquals(Arrays.asList(1,0), Streamer.permuteIndices(1, 2));
        assertEquals(Arrays.asList(0,1,2), Streamer.permuteIndices(0, 3));
        assertEquals(Arrays.asList(1,0,2), Streamer.permuteIndices(1, 3));
        assertEquals(Arrays.asList(2,0,1), Streamer.permuteIndices(2, 3));
        assertEquals(Arrays.asList(0,2,1), Streamer.permuteIndices(3, 3));
        assertEquals(Arrays.asList(1,2,0), Streamer.permuteIndices(4, 3));
        assertEquals(Arrays.asList(2,1,0), Streamer.permuteIndices(5, 3));
    }

    @Test
    public void testPermutations(){
        List<String> l = Arrays.asList("a", "b", "c");
        assertEquals(Streamer.permute(0, l), l);
        assertEquals(Streamer.permute(1, l), Arrays.asList("b", "a", "c"));
        assertEquals(Streamer.permute(2, l), Arrays.asList("c", "a", "b"));
        assertEquals(Streamer.permute(3, l), Arrays.asList("a", "c", "b"));
        assertEquals(Streamer.permute(4, l), Arrays.asList("b", "c", "a"));
        assertEquals(Streamer.permute(5, l), Arrays.asList("c", "b", "a"));
    }

    @Test
    public void testChoices(){
        List<String> l = Arrays.asList("a", "b", "c");
        assertEquals(Streamer.choice(BigInteger.valueOf(0), l), Collections.emptyList());
        assertEquals(Streamer.choice(BigInteger.valueOf(1), l), Arrays.asList("a"));
        assertEquals(Streamer.choice(BigInteger.valueOf(2), l), Arrays.asList("b"));
        assertEquals(Streamer.choice(BigInteger.valueOf(3), l), Arrays.asList("a","b"));
        assertEquals(Streamer.choice(BigInteger.valueOf(4), l), Arrays.asList("c"));
        assertEquals(Streamer.choice(BigInteger.valueOf(5), l), Arrays.asList("a","c"));
        assertEquals(Streamer.choice(BigInteger.valueOf(6), l), Arrays.asList("b","c"));
        assertEquals(Streamer.choice(BigInteger.valueOf(7), l), Arrays.asList("a","b","c"));
        assertEquals(Streamer.choice(BigInteger.valueOf(8), l), Collections.emptyList());
    }

}
