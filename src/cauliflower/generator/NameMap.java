package cauliflower.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NameMap.java
 *
 * Maps various kinds of indices (label, field, domains) to the name used to describe them
 *
 * Created by nic on 25/11/15.
 */
public class NameMap {

    private final Registrar labels = new Registrar();
    private final Registrar fields = new Registrar();
    private final Registrar domains = new Registrar();

    public int label(String s){ return labels.toIndex(s); }
    public String label(int i){ return labels.fromIndex(i); }
    public int field(String s){ return fields.toIndex(s); }
    public String field(int i){ return fields.fromIndex(i); }
    public int domain(String s){ return domains.toIndex(s); }
    public String domain(int i){ return domains.fromIndex(i); }

    private class Registrar {
        public final Map<String, Integer> si =  new HashMap<>();
        public final List<String> is = new ArrayList<>();
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
    }

}
