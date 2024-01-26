package GumTreeMaker;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.io.IOException;

public class GumTreeDifference {

    public static void main(String[] args) {

        // CSV pool dir path
        if (args.length >= 1) {
            String csvPoolDir = args[0];

            String[] gitUrls = {"https://github.com/apache/cassandra", "https://github.com/apache/beam.git", "https://github.com/apache/hadoop", "https://github.com/apache/juddi", "https://github.com/apache/kafka", "https://github.com/apache/spark"};
            String csvPrefix = csvPoolDir + "BIC_BSZZ_";
            String csvPostfix = ".csv";

            for (String gitUrl : gitUrls) {
                String csvFile = csvPrefix + GitFunctions.get_repo_name_from_url(gitUrl) + csvPostfix;

                try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                    br.readLine(); // skip first line, contains metadata

                    String line;

                    while ((line = br.readLine()) != null) {
                        processCSVLine(line, gitUrl);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 2 java files
            String log = "/home/newwin0189/SPI/Candidates/AtlasEntityStore_gum_diff.txt";

            try {
                
                File log_file = new File(log);
                BufferedWriter writer = new BufferedWriter(new FileWriter(log_file, false));

                // String pathBIC = "/home/newwin0189/SPI/Candidates/Closure-10/Codes/Closure-10_rank-3_old.java";
                // String pathBBIC = "/home/newwin0189/SPI/Candidates/Closure-10/Codes/Closure-10_rank-3_new.java";

                // String pathBIC = "/home/newwin0189/SPI/TabletServer_Old.java";
                // String pathBBIC = "/home/newwin0189/SPI/TabletServer_new.java";

                String pathBIC = "/home/newwin0189/SPI/AtlasEntityStore_old.java";
                String pathBBIC = "/home/newwin0189/SPI/AtlasEntityStore_new.java";

                writer.write(pathBIC+"\n");
                writer.write(pathBBIC+"\n");

                Run.initGenerators();
                // create bic and bbic java files

                Tree src = TreeGenerators.getInstance().getTree(pathBIC).getRoot();
                Tree dst = TreeGenerators.getInstance().getTree(pathBBIC).getRoot();

                Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                MappingStore mappings = defaultMatcher.match(src, dst);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript actions = editScriptGenerator.computeActions(mappings);

                String line_log = actions.asList().toString();
                writer.write(line_log + "\n");

                writer.close();

                
    
            } catch (Exception e) {
                // App.logger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);

            }


            Extractor extractor = new Extractor();

            // STEP 3 : extract change vector from diff and write it to a file
            // String diff_path = output_dir + "/diff.txt";

            String output_dir = "/home/newwin0189/SPI/Candidates/Closure-10/GumTree/vector";
            // String git_name = "TabletServer";

            String git_name = "AtlasEntityStore";
            // String gumtree_log = output_dir + "/gumtree_log.txt";
            
            // int cv_extraction_result = extractor.extract_vector(git_name, log, output_dir);
            // if (cv_extraction_result == -1) {
            //     // logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to exception" + ANSI_RESET);
            //     System.exit(1);
            // } else if (cv_extraction_result == 1) {
            //     // logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to no change" + ANSI_RESET);
            //     System.exit(1);
            // }
            // logger.info(ANSI_GREEN + "[info] > Successfully extracted change vector" + ANSI_RESET);
        }
    }

    // adds a new line at the end of the <project>_vector.csv file + skips the metadata first line
    // compared to the data/BIC_BSZZ_<project>.csv, the <project>_vector.csv file has one less line
    private static void processCSVLine(String line, String gitUrl) {
        String[] csvValues = line.split(","); // BISha1, oldPath, Path, FixSha1, BIDate, FixDate, LineNumInBI, LineNumInPreFix, isAddedLine, Line

        GitFunctions gitFunctions = new GitFunctions();

        String gitName = gitFunctions.get_repo_name_from_url(gitUrl);

        String workDir = "/home/young170";
        String gitPath = workDir + "/" + gitName + "/";
        String outputDir = workDir + "/logs/" + gitName;
        String diffPath = outputDir + "/diff.txt";
        String gumtreeDiffPath = outputDir + "/gumtree_log.txt";
        String gumtreeVectorCSVPath = outputDir + "/" + gitName + "_vector.csv";

        Extractor extractor = new Extractor();

        gitFunctions.clone(gitUrl, workDir);

        try (BufferedWriter csvWriter = new BufferedWriter(new FileWriter(gumtreeVectorCSVPath, true))) {
            // Step 1: extract diff from BBIC (blame of BIC) - BIC
            String[] diff = gitFunctions.extract_diff(gitPath, csvValues[2], csvValues[0], Integer.parseInt(csvValues[7]), Integer.parseInt(csvValues[6])); // Path, BISha1, LineNumInPreFix, LineNumInBI

            if (diff != null) {
                try (BufferedWriter diffWriter = new BufferedWriter(new FileWriter(new File(outputDir, "diff.txt")))) {
                    for (String l : diff) {
                        diffWriter.write(l);
                        diffWriter.write(" ");
                    }

                    diffWriter.newLine();
                    diffWriter.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                    csvWriter.newLine();
                    return;
                }
            } else {
                csvWriter.newLine(); // errors write new line to match index number of pool
                return;
            }

            // // Step 2: extract gumtree log and change vector from diff
            if (!extractor.extract_gumtree_log(gitPath, diffPath, outputDir)) {
                csvWriter.newLine(); // errors write new line to match index number of pool
                return;
            }

            String gumtreeVector = extractor.extract_vector(gitName, gumtreeDiffPath, outputDir);

            if (gumtreeVector != null) {
                csvWriter.write(gumtreeVector);
                csvWriter.newLine();
            } else {
                csvWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
