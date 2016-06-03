package cauliflower.parser;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import cauliflower.util.Registrar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Simple hand-written parser
 * TODO use a real antlr LLR parser
 *
 * Created by nic on 1/12/15.
 */
public class SimpleParser implements CFLRParser{

    private Rule.Lbl readLabel(String s, Registrar lblReg, Registrar fldReg){
        int idx = s.indexOf('[');
        String lab = idx == -1 ? s : s.substring(0, idx);
        List<String> flds = new ArrayList<>();
        while(idx != -1){
            int idx2 = s.indexOf(']', idx);
            flds.add(s.substring(idx+1, idx2));
            idx = s.indexOf('[', idx2);
        }
        int[] fld = new int[flds.size()];
        Arrays.setAll(fld, i -> fldReg.toIndex(flds.get(i)));
        return new Rule.Lbl(lblReg.toIndex(lab), fld);
    }

    private Rule.Clause readClause(String s, Registrar lblReg, Registrar fldReg){
        switch(s.charAt(0)){
            case '(' :{
                int depth = -1;
                int loc = -1;
                for(int i=0; i<s.length(); i++){
                    char c = s.charAt(i);
                    if(c == '&' && depth == 0){
                        loc = i;
                        break;
                    }
                    else if (c == '(') depth++;
                    else if (c == ')') depth--;
                }
                return new Rule.And(readClause(s.substring(1, loc), lblReg, fldReg), readClause(s.substring(loc + 1, s.lastIndexOf(")")), lblReg, fldReg));
            }
            case '-' :{
                return new Rule.Rev(readClause(s.substring(1), lblReg, fldReg));
            }
            case '!' :{
                return new Rule.Neg(readClause(s.substring(1), lblReg, fldReg));
            }
            default :{
                return readLabel(s, lblReg, fldReg);
            }
        }
    }

    @Override
    public ParserOutputs parse(InputStream is) throws CFLRException {
        Registrar lReg = new Registrar();
        Registrar fReg = new Registrar();
        Registrar vReg = new Registrar();
        List<Registrar> rfRegs = new ArrayList<>();
        Scanner sca = new Scanner(is);
        sca.useDelimiter(";");
        List<Label> labels = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();
        while(sca.hasNext()){
            String ln = sca.next().replaceAll("\\s", "") + ";";
            if(ln.contains("->")){
                Registrar ruleFieldReg = new Registrar();
                Rule.Lbl head = readLabel(ln.substring(0, ln.indexOf("->")), lReg, ruleFieldReg);
                if(head.label >= labels.size()) throw new CFLRException("Label used before definition: " + lReg.fromIndex(head.label));
                String[] clauses = ln.substring(ln.indexOf("->") + 2, ln.indexOf(";", ln.indexOf("->") + 2)).split(",");
                List<Rule.Clause> cl2 = Arrays.asList(clauses).stream().filter(s -> !s.isEmpty()).map(s -> readClause(s, lReg, ruleFieldReg)).collect(Collectors.toList());
                Rule.Clause[] cla = cl2.toArray(new Rule.Clause[cl2.size()]);
                rfRegs.add(ruleFieldReg);
                rules.add(new Rule(head, cla));
                for(Rule.Lbl l : rules.get(rules.size()-1).dependencies) {
                    if (l.label >= labels.size()) throw new CFLRException("Label used before definition: " + lReg.fromIndex(l.label));
                }
            } else if(ln.contains("<-")){
                Rule.Lbl lbl = readLabel(ln.substring(0, ln.indexOf("<-")), lReg, fReg);
                String[] vds = ln.substring(ln.indexOf("<-")+2, ln.indexOf(";", ln.indexOf("<-"))).split("\\.");
                int vd1 = vReg.toIndex(vds[0]);
                int vd2 = vReg.toIndex(vds[1]);
                if(lbl.label != labels.size()) throw new CFLRException("Label defined twice: " + lReg.fromIndex(lbl.label));
                int[] flds = new int[lbl.fields.size()];
                Arrays.setAll(flds, lbl.fields::get);
                labels.add(new Label(vd1, vd2, flds));
            }
        }
        ParserOutputs ret = new ParserOutputs(new Problem(fReg.size() + vReg.size(), labels, rules), lReg, fReg, vReg, rfRegs);
        Logs.forClass(SimpleParser.class).debug("parsed: {}", ret);
        return ret;
    }
}

