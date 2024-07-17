package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);

        System.out.println("Enter path to directory with epub files");
        String directoryPath = scan.nextLine();

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid directory path.");
            return;
        }

        File[] files = directory.listFiles((dir, name) -> name.startsWith("page") && name.endsWith(".html"));

        for (File file : files) {
            System.out.println(file.getName());
        }

        if (files == null || files.length == 0) {
            System.out.println("No matching files found.");
            return;
        }

        for (File file : files) {
            try {
                processFile(file);
            } catch (IOException e) {
                System.out.println("Failed to process file: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("Processing complete.");
    }

    private static void processFile(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()));

        // Patterns to remove <p>...</p> and <b>...</b> content
        String[] patterns = {
                "<p[^>]*>.*?</p>",
                "<b[^>]*>.*?</b>"
        };

        for (String pattern : patterns) {
            content = removeTagContent(content, pattern);
        }

        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String removeTagContent(String content, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = compiledPattern.matcher(content);
        return matcher.replaceAll("");
    }

}