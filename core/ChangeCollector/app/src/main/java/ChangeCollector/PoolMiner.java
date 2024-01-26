package ChangeCollector;

import java.io.*;
import java.util.*;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import ChangeCollector.Extractor;
import ChangeCollector.GitFunctions;

public class PoolMiner {
    public String commit_file_path; // path to the file containing the commit ids
    public String target; // directory path to clone git url repository
    public String hash_id; // hash id of the current execution
    public String result_file_path; // path to the file containing the result
    public String extracted_commit_file_path;
    private ArrayList<String> repo_init_commits = new ArrayList<String>(); // list of initial commits of a repository.
                                                                           // if
                                                                           // a commit is in list, skip the process and
                                                                           // continue to the next commit
    private HashMap<String, String> file_commit_map = new HashMap<String, String>(); // map of commit id and file path
    private String working_repo_name; // name of the working repository

    public static Logger pmLogger = LogManager.getLogger(PoolMiner.class.getName()); // log4j2 logger

    public PoolMiner(String commit_file_path, String hash_id, String target, String result_file_path) {
        super();
        this.commit_file_path = commit_file_path;
        this.hash_id = hash_id;
        this.target = target;
        this.result_file_path = result_file_path;
        extracted_commit_file_path = target + "/commit_file.csv";
    }

    public boolean run() {
        pmLogger.debug(App.ANSI_PURPLE + "[debug] > PoolMiner.run() called" + App.ANSI_RESET);
        Extractor extractor = new Extractor();
        GitFunctions gitFunctions = new GitFunctions();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(commit_file_path));
            String line;
            while ((line = reader.readLine()) != null) {
                // commit id configuration
                String[] elements = line.split(",");
                String bic = elements[0];
                String bfc = elements[1];
                String bic_path = elements[2];
                String bfc_path = elements[3];
                int lineBlame = Integer.parseInt(elements[4]);
                int lineFix = Integer.parseInt(elements[5]);
                String git_url = elements[6];
                String jira_key = elements[7];
                // git repository configuration
                String repo_name = GitFunctions.get_repo_name_from_url(git_url);
                String workspace_dir = this.target + "/" + hash_id;
                String repo_dir = workspace_dir + "/" + repo_name;
                File repo = new File(repo_dir);

                if (!repo.exists()) {
                    gitFunctions.clone(git_url, workspace_dir);
                }
                // if working_repo_name is not equal to repo_name reset the file_commit_map
                if (!repo_name.equals(working_repo_name)) {
                    file_commit_map.clear();
                    working_repo_name = repo_name;

                    // then add file path and bic to the map
                    file_commit_map.put(bic_path, bic);
                } else {
                    // if file is already in the map, and the bic is not equal to the bic in the
                    // map,
                    // replace the bic in the map with the new bic
                    if (file_commit_map.containsKey(bic_path)) {
                        if (!file_commit_map.get(bic_path).equals(bic)) {
                            file_commit_map.replace(bic_path, bic);
                        }
                    } else {
                        // if file is not in the map, add the file and bic to the map
                        file_commit_map.put(bic_path, bic);
                    }
                }

                // target file name configuration
                String file_name = bfc_path.split("/")[bfc_path.split("/").length - 1];
                // check if bic is initial commit of the repository
                if (repo_init_commits.contains(bic)) {
                    pmLogger.debug(
                            App.ANSI_PURPLE + "[debug] > bic is initial commit of the repository" + App.ANSI_RESET);
                    continue;
                } else if (GitFunctions.isInit(repo_dir, bic)) {
                    pmLogger.debug(
                            App.ANSI_PURPLE + "[debug] > bic is initial commit of the repository" + App.ANSI_RESET);
                    repo_init_commits.add(bic);
                    continue;
                }

                // searching commit id before bic
                String bbic = gitFunctions.blame(repo_dir, bic_path, lineBlame, lineBlame, bic);
                if (bbic.equals(bic) || isIdenticalCommit(bic, bbic)) {
                    pmLogger.error(App.ANSI_RED + "[error] > the bic is the first commit that modified the file"
                            + App.ANSI_RESET);
                    continue;
                }
                // if bbic contains "^" as first character, remove it
                if (bbic.charAt(0) == '^') {
                    bbic = bbic.substring(1);
                }

                if (bbic == null) {
                    ArrayList<String> hash_list = gitFunctions.log(repo_dir, bic_path);
                    // check if the bic is the first commit
                    if (hash_list.size() == 1) {
                        pmLogger.error(App.ANSI_RED + "[error] > The bic is the first commit" + App.ANSI_RESET);
                        continue;
                    } else if (hash_list.size() == 0) {
                        pmLogger.error(App.ANSI_RED + "[error] > The bic is not in the repository" + App.ANSI_RESET);
                        continue;
                    }
                }

                String[] diff = gitFunctions.extract_diff(repo_dir, bic_path, bic, bbic);

                if (diff == null || diff.length == 0) {
                    pmLogger.error(App.ANSI_RED + "[error] > diff is null or empty" + App.ANSI_RESET);
                    continue;
                }

                pmLogger.info(App.ANSI_BLUE + "[info] > diff extracted successfully" + App.ANSI_RESET);

                String diff_file_path = repo_dir + "/diff.txt";

                BufferedWriter diff_writer = new BufferedWriter(new FileWriter(diff_file_path));
                BufferedWriter commit_writer = new BufferedWriter(new FileWriter(extracted_commit_file_path, true));

                for (String diff_line : diff) {
                    if (diff_line == null || diff_line.equals("")) {
                        pmLogger.error(App.ANSI_RED + "[error] > diff_line is null or empty" + App.ANSI_RESET);
                        continue;
                    }
                    pmLogger.info(App.ANSI_BLUE + "[info] > diff_line: " + diff_line + App.ANSI_RESET);
                    diff_writer.write(diff_line);
                    commit_writer.write(diff_line);
                    diff_writer.write(" ");
                    commit_writer.write(",");
                }
                commit_writer.write(git_url);
                commit_writer.write(",");
                commit_writer.write(jira_key);
                diff_writer.newLine();
                commit_writer.newLine();
                diff_writer.close();
                commit_writer.close();

                pmLogger.info(App.ANSI_BLUE + "[info] > diff file generated successfully");// diff file written
                                                                                           // successfully
                if (!extractor.extract_gumtree_log(repo_dir, bic, bbic, bic_path, bic_path, workspace_dir)) {
                    pmLogger.error(App.ANSI_RED + "[error] > extractor failed to extract gumtree log" + App.ANSI_RESET);
                    return false;
                }

                pmLogger.info(App.ANSI_BLUE + "[info] > gumtree log generated successfully" + App.ANSI_RESET);

                String gumtree_log = workspace_dir + "/gumtree_log.txt";

                int cv_extraction_result = extractor.extract_vector_pool(gumtree_log, result_file_path);
                if (cv_extraction_result == -1) {
                    pmLogger.error(App.ANSI_RED + "[error] > extractor failed to extract cv" + App.ANSI_RESET);
                    continue;
                } else if (cv_extraction_result == 1) {
                    pmLogger.error(App.ANSI_RED + "[error] > cv is empty" + App.ANSI_RESET);
                    continue;
                }
                pmLogger.info(App.ANSI_GREEN + "[info] > cv generated successfully" + App.ANSI_RESET);

            }
            reader.close();

            pmLogger.debug(App.ANSI_PURPLE + "[debug] > PoolMiner.run() finished" + App.ANSI_RESET);
        } catch (Exception e) {
            pmLogger.error(App.ANSI_RED + "[error] > Exception : " + e + App.ANSI_RESET);
            return false;
        }
        return true;
    }

    private boolean isIdenticalCommit(String cid, String cidhat) {
        // check if cid and cidhat are identical
        // remove cidhat's first character
        String cidhat_ = cidhat.substring(1);
        // remove cid's last character
        String cid_ = cid.substring(0, cid.length() - 1);
        if (cid_.equals(cidhat_)) {
            return true;
        }
        return false;
    }
}