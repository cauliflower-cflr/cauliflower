package cauliflower.application;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Info {

    static {
        Properties prop = new Properties();
        try {
            prop.load(Info.class.getResourceAsStream("info.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        buildVersion = prop.getProperty("build.version", "UNKNOWN");
        buildDate = prop.getProperty("build.date", "UNKNOWN");

        // Determine where the Cauliflower 'include' directory is
        // - in a distribution it is ../include
        // - in the IDE it is ../../../src/dist/include
        File classBytecodeFile = new File(Info.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File includePath = new File(classBytecodeFile.getParentFile().getParentFile(), "include");
        if(!includePath.exists() || !includePath.isDirectory()){
            includePath = new File(classBytecodeFile.getParentFile().getParentFile().getParentFile(), "src/dist/include");
        }
        cauliDistributionDirectory = includePath.getParentFile().getAbsolutePath();
    }

    public static final String buildVersion;
    public static final String buildDate;
    public static final String cauliDistributionDirectory;
}
