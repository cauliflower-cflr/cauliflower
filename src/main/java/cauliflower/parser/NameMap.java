package cauliflower.parser;

import cauliflower.util.Registrar;

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

}
