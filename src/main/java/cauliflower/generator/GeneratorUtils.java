package cauliflower.generator;

import cauliflower.application.Info;
import cauliflower.representation.*;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GeneratorUtils
 * <p>
 * Author: nic
 * Date: 3/06/16
 */
public class GeneratorUtils {

    public static void generatePreBlock(String problemName, String desc, Class<?> generator, PrintStream out){
        out.println("// " + problemName);
        out.println("//");
        out.println("// " + desc);
        out.println("//");
        out.println("// Generated on: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        out.println("//           by: " + generator.getSimpleName());
        out.println("//      version: " + Info.buildVersion);
    }

}
