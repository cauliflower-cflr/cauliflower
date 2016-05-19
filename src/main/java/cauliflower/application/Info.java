package cauliflower.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    }

    public static final String buildVersion;
    public static final String buildDate;
    public static final String cauliJarPath = new File(Info.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
}
