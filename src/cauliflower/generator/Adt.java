package cauliflower.generator;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract Data Types for the solver
 *
 * Created by nic on 11/12/15.
 */
public enum Adt {
    Std("neighbourhood_map<std::map<ident, std::set<ident>>, std::set<ident>>", "neighbourhood_map.h", "<map>", "<set>"),
    Btree("neighbourhood_map<btree::btree_map<ident, btree::btree_set<ident>>, btree::btree_set<ident>>", "neighbourhood_map.h", "\"btree_map.h\"", "\"btree_set.h\"");
    // TODO Quadtree("concise_tree", "concise_tree.h");

    public final String typename;
    public final String importLoc;
    public final List<String> imports;
    Adt(String typename, String importLoc, String... imports){
        this.typename = typename;
        this.importLoc = importLoc;
        this.imports = Arrays.asList(imports);
    }
}
