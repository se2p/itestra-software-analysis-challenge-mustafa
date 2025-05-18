package com.itestra.software_analyse_challenge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DependencyAnalyser {
    public static List<String> computeDependencies(File file) {
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
}
