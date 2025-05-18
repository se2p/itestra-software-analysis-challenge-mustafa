package com.itestra.software_analyse_challenge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class LineNumberCounter {
    public static int computeLineNumber(File file) {
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

    public static int computeLineNumberBonus(File file) {
        int count = 0;
        boolean inBlockComment = false;
        boolean inGetter = false;
        int braceDepth = 0;
        Pattern getterPattern = Pattern.compile("^\\s*public\\s+[\\w<>\\[\\]]+\\s+get\\w+\\s*\\(\\s*\\)\\s*\\{?\\s*$");
        Pattern getterBodyPattern = Pattern.compile("^\\s*return\\s+(this\\.)?\\w+\\s*;\\s*}?\\s*$");

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
                    if (!getterBodyPattern.matcher(line).matches() && !(line.equals("{") || line.equals("}"))) {
                        inGetter = false;
                        count += 2;
                        continue;
                    }

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
}
