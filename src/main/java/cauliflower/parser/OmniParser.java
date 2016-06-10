package cauliflower.parser;

import cauliflower.representation.Problem;
import cauliflower.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OmniParser
 * A wrapper for the different kinds of parsers and a cache for parsed outputs
 * This is questionably useful, since the use case parses a single file once
 * <p>
 * Author: nic
 * Date: 10/06/16
 */
public class OmniParser {

    private List<Pair<Path, Pair<CFLRParser.ParserOutputs, Problem>>> parses;
    private OmniParser() {
        this.parses = new ArrayList<>();
    }

    private int findOrNew(Path file) throws IOException {
        int c=0;
        //naive iteration because we need the 'is same file' method
        for(Pair<Path, Pair<CFLRParser.ParserOutputs, Problem>> p : parses){
            if(Files.isSameFile(p.first, file)) return c;
            c++;
        }
        parses.add(new Pair<>(file, new Pair<>(null, null)));
        return c;
    }

    private Problem findOrParse(Path file) throws IOException{
        int idx = findOrNew(file);
        if(parses.get(idx).second.second ==  null){
            AntlrParser ap = new AntlrParser();
            new ParseFile(ap).read(file.toFile());
            parses.get(idx).second.second = ap.problem;
        }
        return parses.get(idx).second.second;
    }

    private CFLRParser.ParserOutputs findOrParseLegacy(Path file) throws IOException{
        int idx = findOrNew(file);
        if(parses.get(idx).second.first ==  null){
            parses.get(idx).second.first = new ParseFile(new SimpleParser()).read(file.toFile());
        }
        return parses.get(idx).second.first;
    }

    //singleton stuff
    private static final OmniParser instance = new OmniParser();

    public static CFLRParser.ParserOutputs getLegacy(Path file) throws IOException{
        return instance.findOrParseLegacy(file);
    }

    public static Problem get(Path file) throws IOException{
        return instance.findOrParse(file);
    }

}
