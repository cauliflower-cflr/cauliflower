package cauliflower.util;

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

    public int[] greedy(){
        int mx = 0;
        int my = 1;
        for(int x=0; x<size; x++){
            for(int y=0; y<size; y++) if (y != x){
                if(problem[x][y] > problem[mx][my]){
                    mx = x;
                    my = y;
                }
            }
        }
        int[] ret = new int[size];
        ret[0] = mx;
        ret[1] = my;
        for(int i=2; i<size; i++){
            mx = my;
            my = mx == 0 ? 1 : 0;
            for(int y=0; y<size; y++) if (y != mx){
                if(problem[mx][y] > problem[mx][my]){
                    my = y;
                }
            }
            ret[i] = my;
        }
        return ret;
    }

    public static void main(String[] args){
        int[][] t = {
                {0, 3, 2, 0},
                {3, 0, 0, 1},
                {2, 0, 0, 1},
                {0, 1, 1, 0}};
        for(int i : new TSP(t).greedy()){
            // TODO THIS
            System.out.println(i);
        }
    }

}
