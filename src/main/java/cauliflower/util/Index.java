package cauliflower.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Index<T> {
    private final Map<T, Integer> si =  new HashMap<>();
    private final List<T> is = new ArrayList<>();
    public int size(){
        return is.size();
    }
    public int toIndex(T s){
        if(!si.containsKey(s)){
            si.put(s, is.size());
            is.add(s);
        }
        return si.get(s);
    }
    public T fromIndex(int i){
        return is.get(i);
    }
    @Override
    public String toString(){
        return is.toString();
    }
}
