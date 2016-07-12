package cauliflower.optimiser;

import cauliflower.representation.Domain;
import cauliflower.representation.Label;
import cauliflower.representation.LabelUse;
import cauliflower.util.FileSystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Profile
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class Profile {

    // these patterns are used to match lines of profiler output
    public static final Pattern TIME_UPDATE = Pattern.compile("TIME [0-9]* upd .* [0-9]*");
    public static final Pattern TIME_EXPAND = Pattern.compile("TIME [0-9]* exp .* [0-9]*");
    public static final Pattern SIZE_PARTS = Pattern.compile("SIZE final .* [0-9]* [0-9]* [0-9]*");
    public static final Pattern DOM_FIELD = Pattern.compile("f:.*=[0-9]*");
    public static final Pattern DOM_VERTEX = Pattern.compile("v:.*=[0-9]*");

    Map<String, Integer> data = new HashMap<>();

    /*local*/ Profile(Path logPath) throws IOException {
        FileSystem.getLineStream(logPath).forEach(l -> {
            if(TIME_UPDATE.matcher(l).matches()){
                int time = Integer.parseInt(l.substring(l.lastIndexOf(" ") + 1));
                String var = "u:" + l.substring(l.indexOf(" upd ") + 5, l.lastIndexOf(" "));
                if(!data.containsKey(var)) data.put(var, 0);
                data.put(var, data.get(var) + time);
            } else if (TIME_EXPAND.matcher(l).matches()){
                int time = Integer.parseInt(l.substring(l.lastIndexOf(" ") + 1));
                String var = "x:" + l.substring(l.indexOf(" exp ") + 5, l.lastIndexOf(" "));
                if(!data.containsKey(var)) data.put(var, 0);
                data.put(var, data.get(var) + time);
            } else if (SIZE_PARTS.matcher(l).matches()){
                String[] ps = l.split(" ");
                data.put("st:" + ps[2], Integer.parseInt(ps[3]));
                data.put("s:" + ps[2], Integer.parseInt(ps[4]));
                data.put("t:" + ps[2], Integer.parseInt(ps[5]));
            } else if (DOM_VERTEX.matcher(l).matches() || DOM_FIELD.matcher(l).matches()){
                data.put("d" + l.substring(0, l.indexOf("=")), Integer.parseInt(l.substring(l.lastIndexOf("=") + 1)));
            }
        });
    }

    public int getDeltaExpansionTime(LabelUse lu){
        String s = "x:" + lu.toString();
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public int getRelationSize(Label l){
        String s = "st:" + l.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public int getRelationSources(Label l){
        String s = "s:" + l.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public int getRelationSinks(Label l){
        String s = "t:" + l.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public int getFieldDomainSize(Domain d){
        String s = "df:" + d.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public int getVertexDomainSize(Domain d){
        String s = "dv:" + d.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
}