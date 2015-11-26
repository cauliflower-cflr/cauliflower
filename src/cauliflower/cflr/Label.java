package cauliflower.cflr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Label.java
 *
 * Specification of the label and its domains
 *
 * Created by nic on 25/11/15.
 */
public class Label {

    public final List<Integer> fDomains;

    public Label(int...fDomains){
        this.fDomains = Arrays.stream(fDomains).boxed().collect(Collectors.toList());
    }

}
