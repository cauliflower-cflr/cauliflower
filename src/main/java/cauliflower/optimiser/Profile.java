package cauliflower.optimiser;

import cauliflower.representation.*;
import cauliflower.util.FileSystem;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public Profile(Path logPath) throws IOException {
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

    // this constructor used to make the aggregate profiles
    private Profile() {}

    public int getDeltaExpansionTime(LabelUse lu){
        String s = "x:" + lu.toString();
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public void setRelationSize(Label l, int s){
        data.put("st:" + l.name, s);
    }
    public int getRelationSize(Label l){
        String s = "st:" + l.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public void setRelationSources(Label l, int s){
        data.put("s:" + l.name, s);
    }
    public int getRelationSources(Label l){
        String s = "s:" + l.name;
        return data.containsKey(s) ? data.get(s) : 0;
    }
    public void setRelationSinks(Label l, int s){
        data.put("t:" + l.name, s);
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

    /*
     * Aggregates
     */
    public Integer ruleWeight(Rule r){
        return new Clause.InOrderVisitor<>(new Clause.VisitorBase<Integer>(){
            @Override
            public Integer visitLabelUse(LabelUse lu){
                return getDeltaExpansionTime(lu);
            }
        }).visitAllNonNull(r.ruleBody).stream().mapToInt(Integer::intValue).sum();
    }

    public static Profile emptyProfile(){
        return sumOfProfiles(Collections.emptyList());
    }

    public static Profile sumOfProfiles(List<Profile> profs){
        return weightedAverageOfProfiles(profs, profs.stream().map(p -> 1.0d).collect(Collectors.toList()));
    }

    public static Profile weightedAverageOfProfiles(List<Profile> profs, List<Double> weights){
        Profile ret = new Profile();
        Streamer.zip(profs.stream(), weights.stream(), Pair::new).forEach(p -> {
            p.first.data.forEach((k, v) -> {
                if(!ret.data.containsKey(k)) ret.data.put(k, 0);
                ret.data.put(k, ret.data.get(k) + (int)(v*p.second));
            });
        });
        return ret;
    }
}
