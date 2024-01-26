package ChangeCollector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

public class Extractor {

    HashMap<String, Integer> map = new HashMap<>();
    ArrayList<Integer> AST_types = new ArrayList<>();

    public boolean extract_gumtree_log(String repo_path, String bic, String bbic, String bic_path, String bbic_path,
            String output_dir) {
        App.logger.trace(App.ANSI_BLUE + "[status] > extracting gumtree log from " + App.ANSI_RESET + " to "
                + App.ANSI_BLUE + output_dir + App.ANSI_RESET);
        App.logger.trace(App.ANSI_YELLOW + "[status] > repo path: " + repo_path + App.ANSI_RESET);

        try {
            Git git = Git.open(new File(repo_path));
            Repository repository = git.getRepository();
            RevWalk walk = new RevWalk(repository);
            File output = new File(output_dir);
            if (!output.exists()) {
                output.mkdir();
            }
            String line = "";
            String repo_name = GitFunctions.get_repo_name_from_url(repo_path);

            File log_file = new File(output_dir, "gumtree_log.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(log_file, false));
            RevCommit commitBIC = walk.parseCommit(repository.resolve(bic));
            RevCommit commitBBIC = walk.parseCommit(repository.resolve(bbic));
            String pathBIC = bic_path;
            String pathBBIC = bbic_path;

            String file_information = bic + " " + pathBIC + "\n";
            String file_information_before = bbic + " " + pathBBIC + "\n";

            writer.write(file_information);
            writer.write(file_information_before);

            Run.initGenerators();
            // create bic and bbic java files
            String src_byte = get_source(repository, commitBIC.getName(), pathBIC, "BIC.java", repo_name,
                    output_dir);
            String dst_byte = get_source(repository, commitBBIC.getName(), pathBBIC, "BBIC.java", repo_name,
                    output_dir);

            Tree src = TreeGenerators.getInstance().getTree(src_byte).getRoot();
            Tree dst = TreeGenerators.getInstance().getTree(dst_byte).getRoot();

            Matcher defaultMatcher = Matchers.getInstance().getMatcher();
            MappingStore mappings = defaultMatcher.match(src, dst);
            EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
            EditScript actions = editScriptGenerator.computeActions(mappings);

            String line_log = actions.asList().toString();
            writer.write(line_log + "\n");

            writer.close();
            walk.close();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
        return true;
    }

    // extract gumtree log from diff line
    public boolean extract_gumtree_log(String repo_path, String diff_path, String output_dir) {
        try {
            App.logger.trace(App.ANSI_BLUE + "[status] > extracting gumtree log from " + diff_path
                    + App.ANSI_RESET + " to " + App.ANSI_BLUE + output_dir + App.ANSI_RESET);
            App.logger.trace(App.ANSI_YELLOW + "[status] > repo path: " + repo_path + App.ANSI_RESET);
            Git git = Git.open(new File(repo_path));
            Repository repository = git.getRepository();
            RevWalk walk = new RevWalk(repository);
            File diff_file = new File(diff_path);
            File output = new File(output_dir);
            if (!output.exists()) {
                output.mkdir();
            }

            BufferedReader reader = new BufferedReader(new FileReader(diff_file));
            String line = "";
            String repo_name = GitFunctions.get_repo_name_from_url(repo_path);

            File log_file = new File(output_dir, "gumtree_log.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(log_file, false));

            while ((line = reader.readLine()) != null) {
                String[] token = line.split("\\s+");
                RevCommit commitBIC = walk.parseCommit(repository.resolve(token[0]));
                RevCommit commitBBIC = walk.parseCommit(repository.resolve(token[1]));
                String pathBIC = token[2];
                String pathBBIC = token[3];

                String file_information = token[0] + " " + pathBIC + "\n";
                String file_information_before = token[1] + " " + pathBBIC + "\n";

                writer.write(file_information);
                writer.write(file_information_before);

                Run.initGenerators();
                // create bic and bbic java files
                String src_byte = get_source(repository, commitBIC.getName(), pathBIC, "BIC.java", repo_name,
                        output_dir);
                String dst_byte = get_source(repository, commitBBIC.getName(), pathBBIC, "BBIC.java", repo_name,
                        output_dir);

                Tree src = TreeGenerators.getInstance().getTree(src_byte).getRoot();
                Tree dst = TreeGenerators.getInstance().getTree(dst_byte).getRoot();

                Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                MappingStore mappings = defaultMatcher.match(src, dst);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript actions = editScriptGenerator.computeActions(mappings);

                String line_log = actions.asList().toString();

                writer.write(line_log + "\n");
            }
            writer.close();
            walk.close();
            reader.close();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
        return true;
    }

    public int extract_vector_pool(String gumtree_log, String result_file) {
        App.logger.trace(App.ANSI_BLUE + "[status] > extracting change vectors from " + gumtree_log
                + App.ANSI_RESET + " to " + App.ANSI_BLUE + result_file + App.ANSI_RESET);
        File gumtree = new File(gumtree_log);

        String line = null;
        boolean no_change = false;
        boolean add = false;
        int oper = 0;

        try {
            BufferedWriter vector_writer = new BufferedWriter(new FileWriter(result_file, true));
            String write_line = "";

            BufferedReader log_reader = new BufferedReader(new FileReader(gumtree));

            while ((line = log_reader.readLine()) != null && (!no_change)) {
                // App.logger.debug(App.ANSI_PURPLE + "[debug] > reading each line ..." +
                // App.ANSI_RESET);
                StringTokenizer st = new StringTokenizer(line);
                write_line = "";
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    // App.logger.debug(App.ANSI_PURPLE + "[debug] > reading each token : " + token
                    // + App.ANSI_RESET);
                    if (token.equals("[]")) {
                        no_change = true;
                    }
                    if (token.matches("insert-node|delete-node|update-node|insert-tree|delete-tree|move-tree")) {
                        if (AST_types.size() > 0 && oper != -1) {
                            for (int i = 0; i < AST_types.size(); i++) {
                                int val = 170 * oper + AST_types.get(i);
                                write_line += val + ",";
                            }
                        }
                        AST_types.clear();
                        oper = getNodeNum(token);
                    }
                    if (token.matches("---")) {
                        add = true;
                    }
                    if (token.matches("===")) {
                        add = false;
                    }
                    if (add == true) {
                        if (!Character.isAlphabetic(token.charAt(token.length() - 1)) && add) {
                            token = token.substring(0, token.length() - 1);
                        }

                        for (int i = 0; i < ChangeVector.expanded_nodes.length; i++) {
                            if (token.equals(ChangeVector.expanded_nodes[i])) {
                                AST_types.add(i + 1);
                            }
                        }
                    }
                }
                vector_writer.write(write_line);
            }
            vector_writer.newLine();
            vector_writer.close();
            log_reader.close();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] Exception : " + e.getMessage() + App.ANSI_RESET);
            return -1;
        }
        int result = no_change ? 1 : 0;
        return result;
    }

    public int extract_vector(String repo_name, String gumtree_log, String result_path) {
        return extract_vector(repo_name, gumtree_log, result_path, false);
    }

    // extract change vectors from gumtree log
    public int extract_vector(String repo_name, String gumtree_log, String result_path, boolean all_diffs) {
        App.logger.trace(App.ANSI_BLUE + "[status] > extracting change vectors from " + gumtree_log
                + App.ANSI_RESET + " to " + App.ANSI_BLUE + result_path + App.ANSI_RESET);
        File gumtree = new File(gumtree_log);
        File result_dir = new File(result_path);
        if (!result_dir.exists()) {
            result_dir.mkdir();
        }
        File vector_file = new File(result_dir.getAbsolutePath(), repo_name + "_gumtree_vector.csv");

        String line = null;
        boolean no_change = false;
        boolean add = false;
        int oper = -1;

        try {
            BufferedWriter vector_writer = new BufferedWriter(new FileWriter(vector_file, true));
            String write_line = "";

            BufferedReader log_reader = new BufferedReader(new FileReader(gumtree));

            while ((line = log_reader.readLine()) != null && (!no_change || all_diffs)) {
                // App.logger.debug(App.ANSI_PURPLE + "[debug] > reading each line ..." +
                // App.ANSI_RESET);
                StringTokenizer st = new StringTokenizer(line);
                write_line = "";
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    // App.logger.debug(App.ANSI_PURPLE + "[debug] > reading each token : " + token
                    // + App.ANSI_RESET);
                    if (token.equals("[]")) {
                        no_change = true;
                    }
                    if (token.matches("insert-node|delete-node|update-node|insert-tree|delete-tree|move-tree")) {
                        if (AST_types.size() > 0 && oper != -1) {
                            for (int i = 0; i < AST_types.size(); i++) {
                                int val = 170 * oper + AST_types.get(i);
                                write_line += val + ",";
                            }
                        }
                        AST_types.clear();
                        oper = getNodeNum(token);
                    }
                    if (token.matches("---")) {
                        add = true;
                    }
                    if (token.matches("===")) {
                        add = false;
                    }
                    if (add) {
                        if (!Character.isAlphabetic(token.charAt(token.length() - 1)) && add) {
                            token = token.substring(0, token.length() - 1);
                        }
                        for (int i = 0; i < ChangeVector.expanded_nodes.length; i++) {
                            if (token.equals(ChangeVector.expanded_nodes[i])) {
                                AST_types.add(i + 1);
                            }
                        }
                    }
                }
                vector_writer.write(write_line);
                if (!write_line.equals("") && all_diffs) {
                    vector_writer.newLine();
                }
            }
            // write last line
            if (AST_types.size() > 0 && oper != -1) {
                for (int i = 0; i < AST_types.size(); i++) {
                    int val = 170 * oper + AST_types.get(i);

                    write_line += val + ",";
                }
                AST_types.clear();
                vector_writer.write(write_line);
            }
            vector_writer.close();
            log_reader.close();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] Exception : " + e.getMessage() + App.ANSI_RESET);
            return -1;
        }
        int result = no_change ? 1 : 0;
        return all_diffs ? 0 : result;
    }

    private String get_source(Repository repo, String sha, String file_path, String file_name, String repo_name,
            String output) {
        try {
            String utf_string = "";
            String dir = output + "/" + repo_name;
            File file_content = new File(dir, file_name);

            final ObjectId id = repo.resolve(sha);
            ObjectReader reader = repo.newObjectReader();

            RevWalk walk = new RevWalk(reader);
            RevCommit commit = walk.parseCommit(id);
            walk.close();

            RevTree tree = commit.getTree();
            TreeWalk treewalk = TreeWalk.forPath(reader, file_path, tree);

            if (treewalk != null) {
                byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
                reader.close();

                utf_string = StringUtils.newStringUtf8(data);
                FileUtils.writeStringToFile(file_content, utf_string, Charset.forName("utf8"));

                return file_content.getPath();
            } else {
                App.logger.error(App.ANSI_RED + "Error writing file for " + file_name + App.ANSI_RESET);
            }
            return file_content.getPath();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + e.getMessage() + App.ANSI_RESET);
            return "";
        }
    }

    public static int getNodeNum(String str) {
        // on node types
        if (str.equals("delete-node")) {
            return 0;
        }
        if (str.equals("insert-node")) {
            return 1;
        }
        if (str.equals("update-node")) {
            return 2;
        }

        // on tree types
        if (str.equals("delete-tree")) {
            return 3;
        }
        if (str.equals("insert-tree")) {
            return 4;
        }
        if (str.equals("move-tree")) {
            return 5;
        }
        return -1;
    }
}
