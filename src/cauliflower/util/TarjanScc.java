package cauliflower.util;

import cauliflower.cflr.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of Tarjan's strongly connected components
 * Also returns SCCs in dependency order
 * Created by nic on 27/11/15.
 */
public class TarjanScc {

    private static <T> ArrayList<T> dup(T t, int len){
        return Stream.generate(() -> t).limit(len).collect(Collectors.toCollection(ArrayList<T>::new));
    }
    private int curIndex = 0;
    private List<Integer> index;
    private List<Integer> lowLink;
    private List<Boolean> onStack;
    private Stack<Integer> stack = new Stack<>();
    public List<List<Integer>> result = new ArrayList<>();
    private List<List<Integer>> successors;

    public TarjanScc(int vSize, List<List<Integer>> successors){
        this.index = dup(-1, vSize);
        this.lowLink = dup(-1, vSize);
        this.onStack = dup(false, vSize);
        this.successors = successors;
        for(int v=0; v<vSize; v++) if(index.get(v) == -1) strongConnect(v);
    }

    private void strongConnect(int v){
        index.set(v, curIndex);
        lowLink.set(v, curIndex);
        curIndex++;
        stack.push(v);
        onStack.set(v, true);
        for(int w : successors.get(v)){
            if(index.get(w) == -1){
                strongConnect(w);
                lowLink.set(v, Math.min(lowLink.get(v), lowLink.get(w)));
            } else if(onStack.get(w)){
                lowLink.set(v, Math.min(lowLink.get(v), index.get(w)));
            }
        }
        if(lowLink.get(v) == index.get(v)){
            ArrayList<Integer> scc = new ArrayList<>();
            int w = -1;
            while(w != v) {
                w = stack.pop();
                onStack.set(w, false);
                scc.add(w);
            }
            result.add(scc);
        }
    }
}
