package ChangeCollector;

import org.apache.commons.cli.*;
import java.io.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import com.opencsv.*;
import java.nio.file.FileSystems;

public class Implemental {
    // log4j2 logger
    public Logger logger = App.logger;
    // Defects4J bug
    public String name = null; // Defects4J Bug Name
    public int identifier = -1; // Defects4J Bug Identifier
    // directory paths
    public String project_root;
    public String target;
    public String result_dir;
    public String jdk8_dir;
    public String workspace_dir;
    // hash id of the current execution
    public String hash_id;
    // if configured or not
    public boolean config_ready = false;
    // Defects4J bug information
    public boolean d4j_ready = false;
    public String faultyProject;
    public String faultyPath;
    public Integer faultyLineBlame;
    public Integer faultyLineFix;
    // Defects4J bug commit ids
    public String cid_fixed;
    public String cid_buggy;
    // last exit code of the executed command
    public int last_exit_code = -1;

    // constructor
    public Implemental() {
        super();
    }

    // set the values for variables to collect change vector of a defects4j bug
    // @param project_root: the root directory of the project
    // @param name: Defects4J bug name
    // @param identifier: Defects4J bug identifier
    // @param result_dir: result directory
    // @param jdk8_dir: jdk8 directory
    // @param hash_id: hash id of the current execution
    public boolean config(String project_root, String name, int identifier, String result_dir, String jdk8_dir,
            String hash_id) {
        this.project_root = project_root;
        this.name = name;
        this.identifier = identifier;
        this.target = String.format("%s/%s", this.project_root, "target");
        this.result_dir = result_dir;
        this.jdk8_dir = jdk8_dir;
        this.hash_id = hash_id;
        config_ready = true;
        return config_ready;
    }

    // extract the commit ids of the Defects4J bug
    // @param old_cid : the commit id of the buggy version
    // @param new_cid : the commit id of the fixed version
    public boolean cid_config(String cid_fixed, String cid_buggy) {
        this.cid_fixed = cid_fixed;
        this.cid_buggy = cid_buggy;
        return true;
    }

    // according to configured variables, set directories and load defects4j bug
    // information
    public boolean preprocess() {
        if (config_ready) {
            try {
                workspace_dir = String.format("%s/%s", result_dir, hash_id);
                File workspace = new File(workspace_dir);
                if (!workspace.exists()) {
                    if (!workspace.mkdirs()) {
                        // failed to create workspace directory
                        logger.error(App.ANSI_RED + "[error] > Failed to create workspace directory " + App.ANSI_RESET);
                        return false;
                    }
                }
                String output_dir = String.format("%s/outputs/ChangeCollector", workspace_dir);
                File output_path = new File(output_dir);
                if (!output_path.exists()) {
                    if (!output_path.mkdirs()) {
                        // failed to create output directory
                        logger.error(App.ANSI_RED + "[error] > Failed to create output directory " + App.ANSI_RESET);
                        return false;
                    }
                }
            } catch (Exception e) {
                logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
                return false;
            }
            return true;
        }
        logger.error(App.ANSI_RED + "[error] > Not configured yet" + App.ANSI_RESET);
        return false;
    }

    // collect the current source code of the Defects4J bug in a buggy version.
    public boolean fetch() {
        String project_dir = String.format("%s/%s", workspace_dir, name);
        if (config_ready) {
            try {
                ProcessBuilder pb = new ProcessBuilder("defects4j", "checkout", "-p", name, "-v",
                        String.format("%db", identifier),
                        "-w", project_dir);
                Process p = pb.start();
                last_exit_code = p.waitFor();
            } catch (Exception e) {
                logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
                return false;
            }
        }
        return last_exit_code == 0;
    }

    // resolve the information of given Defects4J bug with given name and identifier
    public boolean parse() {
        String project_dir = String.format("%s/%s", workspace_dir, name);
        String info = String.format("%s/components/commit_collector/Defects4J_bugs_info/%s.csv", project_root,
                name);
        try {
            CSVReader reader = new CSVReader(new FileReader(info));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine[0].startsWith("Defects4J"))
                    continue;
                if (Integer.parseInt(nextLine[0]) == identifier) {
                    faultyProject = project_dir;
                    faultyPath = nextLine[1];
                    faultyLineBlame = Integer.parseInt(nextLine[2]);
                    faultyLineFix = Integer.parseInt(nextLine[3]);
                    d4j_ready = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
        return d4j_ready;
    }
}
