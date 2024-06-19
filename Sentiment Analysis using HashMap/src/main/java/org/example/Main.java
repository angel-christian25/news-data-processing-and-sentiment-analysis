package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        Map<String, Integer> positiveWords = loadWordsFromFile("positive-words.txt");
        Map<String, Integer> negativeWords = loadWordsFromFile("negative-words.txt");

        String[] files = {
                "reut2-009.sgm",
                "reut2-014.sgm"
        };

        String csvFilePath = "output.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
            writer.write("\"News#\",\"Title Content\",\"Matched Words\",\"Score\",\"Polarity\"\n");

            int count = 1;

            for (int i = 0; i < files.length; i++) {
                String content = readHtmlFile(files[i]);

                String[] titles = extractAllTitles(content);

                for (String title : titles) {
                    Map<String, Integer> bagOfWords = createBagOfWords(title);
                    Map<String, Integer> matchedWords = findMatchedWords(bagOfWords, positiveWords, negativeWords);

                    int score = calculateScore(matchedWords);
                    String polarity = (score > 0) ? "Positive" : (score < 0) ? "Negative" : "Neutral";
                    writer.write("\"" + count + "\",\"" + title + "\",\"" + matchedWords + "\",\"" + score + "\",\"" + polarity + "\"\n");
                    count++;
                }
            }

            System.out.println("CSV file created successfully at " + csvFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> loadWordsFromFile(String filename) {
        Map<String, Integer> wordMap = new HashMap<>();
        try {
            Files.lines(Paths.get(filename)).forEach(line -> wordMap.put(line.trim(), 1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wordMap;
    }

    private static String readHtmlFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String[] extractAllTitles(String content) {
        Pattern pattern = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        StringBuilder titles = new StringBuilder();
        while (matcher.find()) {
            titles.append(matcher.group(1).trim()).append("\n");
        }

        return titles.toString().split("\\n");
    }

    private static Map<String, Integer> createBagOfWords(String text) {
        Map<String, Integer> bagOfWords = new HashMap<>();
        Pattern pattern = Pattern.compile("\\b\\w+\\b");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            bagOfWords.put(word, bagOfWords.getOrDefault(word, 0) + 1);
        }
        return bagOfWords;
    }

    private static Map<String, Integer> findMatchedWords(Map<String, Integer> bagOfWords, Map<String, Integer> positiveWords,
                                                         Map<String, Integer> negativeWords) {
        Map<String, Integer> matchedWords = new HashMap<>();
        for (String word : bagOfWords.keySet()) {
            if (positiveWords.containsKey(word)) {
                matchedWords.put(word, positiveWords.get(word));
            } else if (negativeWords.containsKey(word)) {
                matchedWords.put(word, -negativeWords.get(word));
            }
        }
        return matchedWords;
    }

    private static int calculateScore(Map<String, Integer> matchedWords) {
        int score = 0;
        for (int value : matchedWords.values()) {
            score += value;
        }
        return score;
    }
}
