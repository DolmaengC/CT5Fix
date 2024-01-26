package csvPP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class App {

    public static void main(String[] args) {
        String inputFilePath = "/home/newwin0189/SPI_2.0/data/eli/eli_spark_commit.csv"; // Specify your input CSV file path
        String outputFilePath = "/home/newwin0189/SPI_2.0/data/eli/eli_formatted_spark_commit.csv"; // Specify the desired output CSV file path
        String gitHubLink = "https://github.com/apache/spark"; // Replace with your GitHub link
        String projectName = "SPARK"; // Replace with your project name

        convertCsvFormat(inputFilePath, outputFilePath, gitHubLink, projectName);

        System.out.println("CSV format converted successfully.");
    }

    public static void convertCsvFormat(String inputFilePath, String outputFilePath, String gitHubLink, String projectName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                
                String biSha1 = parts[0].trim();
                String fixSha1 = parts[3].trim();
                String oldPath = parts[1].trim();
                String path = parts[2].trim();
                String newLine = String.format("%s,%s,%s,%s,%s,%s%n", biSha1, fixSha1, oldPath, path, gitHubLink, projectName);
                writer.write(newLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
