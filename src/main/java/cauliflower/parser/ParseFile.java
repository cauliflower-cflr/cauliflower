package cauliflower.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads an input file into a problem
 *
 * Created by nic on 11/12/15.
 */
public class ParseFile {
    private final CFLRParser parser;
    public ParseFile(CFLRParser parser){
        this.parser = parser;
    }
    public CFLRParser.ParserOutputs read(File fi) throws IOException {
        return parser.parse(new FileInputStream(fi));
    }
}
