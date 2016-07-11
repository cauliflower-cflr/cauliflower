package cauliflower;

import cauliflower.util.Streamer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        checkIsSame(Streamer.permute(0, l), l);
        checkIsSame(Streamer.permute(1, l), Arrays.asList("b", "a", "c"));
        checkIsSame(Streamer.permute(2, l), Arrays.asList("c", "a", "b"));
        checkIsSame(Streamer.permute(3, l), Arrays.asList("a", "c", "b"));
        checkIsSame(Streamer.permute(4, l), Arrays.asList("b", "c", "a"));
        checkIsSame(Streamer.permute(5, l), Arrays.asList("c", "b", "a"));
    }

    public <A> void checkIsSame(List<A> a, List<A> b){
        assertEquals(a, b);
    }

}
