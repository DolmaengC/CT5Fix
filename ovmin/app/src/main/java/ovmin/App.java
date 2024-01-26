package ovmin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;


public class App {

    public static void main(String[] args) {


        String inputVector = "/home/newwin0189/SPI_2.0/data/spark_vector.csv"; // Specify your input CSV file path
        String outputVector = "/home/newwin0189/SPI_2.0/data/eli/eli_spark_vector.csv"; // Specify the desired output CSV file path

        String inputCommit = "/home/newwin0189/SPI_2.0/data/BIC_BSZZ_spark.csv";
        String outputCommit = "/home/newwin0189/SPI_2.0/data/eli/eli_spark_commit.csv";

        Collection<Integer> uniqueIndices = getOverlappedVectors(inputVector);
        keepLinesForCertainIndices(inputVector, uniqueIndices, outputVector);
        keepLinesForCertainIndices(inputCommit, uniqueIndices, outputCommit);


        System.out.println("Overlap lines eliminated successfully.");
    }

    public static Collection<Integer> getOverlappedVectors(String inputFilePath) {

        // Set<Integer> uniqueIndices = new HashSet<>();
        Map<String, Integer> lineIndexMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {

                // Increment line number for each line read
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }


                // Add each line to the map with its line number
                lineIndexMap.put(line, lineNumber);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return lineIndexMap.values();
    }

    public static void keepLinesForCertainIndices(String inputFilePath, Collection<Integer> indicesToKeep, String outputFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                // Increment line number for each line read
                lineNumber++;

                // Write the line to the output file if its index is in the set
                if (indicesToKeep.contains(lineNumber)) {
                    writer.write(line);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
