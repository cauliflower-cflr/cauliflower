package cauliflower.application;

import java.io.File;
import java.io.IOException;

/**
 * Interfaces with the system compiler to compile
 * generated code into executables.
 */
public class CompilerInterface {

    public final File execFile;
    public final File buildDir;
    public final File frontEnd;
    public final File backEnd;


    public CompilerInterface(String execPath) throws IOException {
        this.execFile = new File(execPath);
        this.buildDir = new File(execFile.getParentFile(), execFile.getName() + "_build");
        if(execFile.exists()) throw new IOException("Executable " + execFile.getPath() + " already exists.");
        if(buildDir.exists()) throw new IOException("Build directory " + buildDir.getPath() + " already exists.");
        if (!buildDir.mkdirs()) throw new IOException("Failed to create a build directory at: " + buildDir.getAbsolutePath());
        this.backEnd = new File(buildDir, execFile.getName() + ".h");
        this.frontEnd = new File(buildDir, execFile.getName() + ".cpp");
    }

    public void compile() throws IOException{
        if(!frontEnd.exists()) throw new IOException("Could not compile " + execFile.getName() + ", front end doesnt exist yet: " + frontEnd.getAbsolutePath());
        if(!backEnd.exists()) throw new IOException("Could not compile " + execFile.getName() + ", back end doesnt exist yet: " + backEnd.getAbsolutePath());
    }

}
