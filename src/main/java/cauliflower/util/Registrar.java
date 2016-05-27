package cauliflower.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates unique IDs for strings
 *
 * Created by nic on 1/12/15.
 */
public class Registrar {
    private final Map<String, Integer> si =  new HashMap<>();
    private final List<String> is = new ArrayList<>();
    public int size(){
        return is.size();
    }
    public int toIndex(String s){
        if(!si.containsKey(s)){
            si.put(s, is.size());
            is.add(s);
        }
        return si.get(s);
    }
    public String fromIndex(int i){
        return is.get(i);
    }
    @Override
    public String toString(){
        return is.toString();
    }

}
