package com.itestra.software_analyse_challenge;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SourceCodeAnalyser {
    private static String getRelativePath(File baseDir, File file) {
        Path basePath = baseDir.toPath();
        Path filePath = file.toPath();
        return basePath.relativize(filePath).toString().replace(File.separatorChar, '/');
    }

    private static List<File> findAllJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return javaFiles;
        for (File file : files) {
            if (file.isDirectory()) {
                javaFiles.addAll(findAllJavaFiles(file));
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
        return javaFiles;
    }

    private static int computeLineNumber(File file) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//")) {
                    continue;
                }
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static List<String> computeDependencies(File file, File inputDir) {
        Set<String> projects = new HashSet<>();
        Pattern staticImportPattern = Pattern.compile("^import static .*");
        Pattern wildcardPattern = Pattern.compile(".*\\.\\*;$");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("import ")) {
                    if (staticImportPattern.matcher(line).matches() || wildcardPattern.matcher(line).matches()) {
                        continue;
                    }
                    String importStmt = line.substring("import ".length(), line.length() - 1);
                    String[] parts = importStmt.split("\\.");
                    if (parts.length == 0) continue;
                    String firstPart = parts[0];
                    switch (firstPart) {
                        case "cronutils":
                            projects.add("cron-utils");
                            break;
                        case "fig":
                            projects.add("fig");
                            break;
                        case "spark":
                            projects.add("spark");
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(projects);
    }

    private static int computeLineNumberBonus(File file) {
        int count = 0;
        boolean inBlockComment = false;
        boolean inGetter = false;
        int braceDepth = 0;
        Pattern getterPattern = Pattern.compile("^\\s*public\\s+[\\w<>\\[\\]]+\\s+get\\w+\\s*\\(\\s*\\)\\s*\\{?\\s*$");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (inBlockComment) {
                    int endIndex = line.indexOf("*/");
                    if (endIndex != -1) {
                        inBlockComment = false;
                        line = line.substring(endIndex + 2).trim();
                    } else {
                        continue;
                    }
                }

                int startIndex = line.indexOf("/*");
                if (startIndex != -1) {
                    inBlockComment = true;
                    int endIndex = line.indexOf("*/", startIndex + 2);
                    if (endIndex != -1) {
                        inBlockComment = false;
                        line = line.substring(0, startIndex) + line.substring(endIndex + 2);
                    } else {
                        line = line.substring(0, startIndex);
                    }
                    line = line.trim();
                }

                if (inBlockComment) continue;

                if (line.isEmpty() || line.startsWith("//")) continue;

                if (inGetter) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        else if (c == '}') braceDepth--;
                    }
                    if (braceDepth <= 0) {
                        inGetter = false;
                        braceDepth = 0;
                    }
                    continue;
                }

                if (getterPattern.matcher(line).matches()) {
                    inGetter = true;
                    braceDepth = countBraces(line);
                    continue;
                }

                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static int countBraces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '{') count++;
            else if (c == '}') count--;
        }
        return count;
    }

    /**
     * Your implementation
     *
     * @param input {@link Input} object.
     * @return mapping from filename -> {@link Output} object.
     */
    public static Map<String, Output> analyse(Input input) {
        Map<String, Output> result = new HashMap<>();
        File inputDir = input.getInputDirectory();
        List<File> javaFiles = findAllJavaFiles(inputDir);
        for (File file : javaFiles) {
            String fileName = getRelativePath(inputDir, file);
            int lineNumber = computeLineNumber(file);
            List<String> dependencies = computeDependencies(file, inputDir);
            int lineNumberBonus = computeLineNumberBonus(file);
            Output output = new Output(lineNumber, dependencies);
            if (lineNumberBonus != lineNumber) {
                output.lineNumberBonus(lineNumberBonus);
            }
            result.put(fileName, output);
        }
        return result;
    }

    /**
     * INPUT - OUTPUT
     *
     * No changes below here are necessary!
     */
    public static final Option INPUT_DIR = Option.builder("i")
            .longOpt("input-dir")
            .hasArg(true)
            .desc("input directory path")
            .required(false)
            .build();

    public static final String DEFAULT_INPUT_DIR = String.join(File.separator , Arrays.asList("..", "CodeExamples", "src", "main", "java"));

    private static Input parseInput(String[] args) {
        Options options = new Options();
        Collections.singletonList(INPUT_DIR).forEach(options::addOption);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine commandLine = parser.parse(options, args);
            return new Input(commandLine);
        } catch (ParseException e) {
            formatter.printHelp("help", options);
            throw new IllegalStateException("Could not parse Command Line", e);
        }
    }

    private static void printOutput(Map<String, Output> outputMap) {
        System.out.println("Result: ");
        List<OutputLine> outputLines =
                outputMap.entrySet().stream()
                        .map(e -> new OutputLine(e.getKey(), e.getValue().getLineNumber(), e.getValue().getLineNumberBonus(), e.getValue().getDependencies()))
                        .sorted(Comparator.comparing(OutputLine::getFileName))
                        .collect(Collectors.toList());
        outputLines.add(0, new OutputLine("File", "Source Lines", "Source Lines without Getters and Block Comments", "Dependencies"));
        int maxDirectoryName = outputLines.stream().map(OutputLine::getFileName).mapToInt(String::length).max().orElse(100);
        int maxLineNumber = outputLines.stream().map(OutputLine::getLineNumber).mapToInt(String::length).max().orElse(100);
        int maxLineNumberWithoutGetterAndSetter = outputLines.stream().map(OutputLine::getLineNumberWithoutGetterSetter).mapToInt(String::length).max().orElse(100);
        int maxDependencies = outputLines.stream().map(OutputLine::getDependencies).mapToInt(String::length).max().orElse(100);
        String lineFormat = "| %"+ maxDirectoryName+"s | %"+maxLineNumber+"s | %"+maxLineNumberWithoutGetterAndSetter+"s | %"+ maxDependencies+"s |%n";
        outputLines.forEach(line -> System.out.printf(lineFormat, line.getFileName(), line.getLineNumber(), line.getLineNumberWithoutGetterSetter(), line.getDependencies()));
    }

    public static void main(String[] args) {
        Input input = parseInput(args);
        Map<String, Output> outputMap = analyse(input);
        printOutput(outputMap);
    }
}
