package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class Main {
    public static void main(String[] args) {

        String zipFilePath = "C:\\SZYMON\\berserk_demo\\Berserk v03 (2004).epub";
        String extractDir = "C:\\SZYMON\\berserk_extract";
        String outputZipFilePath = "C:\\SZYMON\\berserk_output\\Berserk v03 (2004).epub";

        try {
            // Step 1: Unzip the EPUB file
            unzip(zipFilePath, extractDir);

            // Step 2: Process html files and gather IDs of removed files
            List<Path> htmlFiles = listhtmlFiles(extractDir);
            List<String> removedFileIds = new ArrayList<>();
            for (Path htmlFile : htmlFiles) {
                if (!containsJpegImages(htmlFile)) {
                    removedFileIds.add(getFileId(htmlFile));
                    Files.delete(htmlFile);
                } else {
                    processhtmlFile(htmlFile);
                }
            }

            // Step 3: Update content.opf
            updateContentOpf(extractDir, removedFileIds);

            // Step 4: Repackage the EPUB file
            zip(extractDir, outputZipFilePath);

            // Step 6: Clean up the extract directory
            cleanExtractDirectory(extractDir);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // Ensure parent directories are created
                    File parentDir = new File(filePath).getParentFile();
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private static List<Path> listhtmlFiles(String startDir) throws IOException {
        List<Path> htmlFiles = new ArrayList<>();
        Files.walk(Paths.get(startDir))
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().startsWith("page") && path.toString().endsWith(".html"))
                .forEach(htmlFiles::add);
        return htmlFiles;
    }

    private static boolean containsJpegImages(Path htmlFile) throws IOException {
        String content = new String(Files.readAllBytes(htmlFile));
        Pattern pattern = Pattern.compile("<img[^>]+src=\"[^\"]+\\.jpeg\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find();
    }

    private static String getFileId(Path htmlFile) throws IOException {
        String content = new String(Files.readAllBytes(htmlFile));
        Pattern pattern = Pattern.compile("id=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static void processhtmlFile(Path htmlFile) throws IOException {
        String content = new String(Files.readAllBytes(htmlFile));

        // Patterns to remove <p>...</p> and <b>...</b> content
        String[] patterns = {
                "<p[^>]*>.*?</p>",
                "<b[^>]*>.*?</b>"
        };

        for (String pattern : patterns) {
            content = removeTagContent(content, pattern);
        }

        Files.write(htmlFile, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String removeTagContent(String content, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = compiledPattern.matcher(content);
        return matcher.replaceAll("");
    }

    private static void updateContentOpf(String extractDir, List<String> removedFileIds) throws IOException {
        Path contentOpfPath = Paths.get(extractDir, "EPUB", "content.opf");
        String content = new String(Files.readAllBytes(contentOpfPath));

        for (String id : removedFileIds) {
            content = content.replaceAll("<item[^>]+id=\"" + id + "\"[^>]*>", "");
        }

        Files.write(contentOpfPath, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void zip(String sourceDirPath, String outputZipPath) throws IOException {
        Path zipFilePath = Files.createFile(Paths.get(outputZipPath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
    }

    private static void cleanExtractDirectory(String extractDir) throws IOException {
        Path directory = Paths.get(extractDir);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}