package cauliflower.util;

import java.util.Arrays;

/**
 * Travelling Salesman Problem
 *
 * This implementation is for solving the field alignment problem
 * The input is a complete bi-directed graph
 * the output is the maximally weighted circuit
 *
 * Created by nic on 27/11/15.
 */
public class TSP {

    private final int size;
    private final int[][] problem;

    public TSP(int[][] problem){
        this.problem = problem;
        this.size = problem.length;
    }

    public int[] opt2(){
        int[] ret = new int[size];
        Arrays.setAll(ret, i -> i);
        return ret;
    }

    public static void main(String[] args){
        int[][] t = {
                {0, 3, 2, 0},
                {3, 0, 0, 1},
                {2, 0, 0, 1},
                {0, 1, 1, 0}};
        for(int i : new TSP(t).opt2()){
            // TODO THIS
            System.out.println(i);
        }
    }

}
