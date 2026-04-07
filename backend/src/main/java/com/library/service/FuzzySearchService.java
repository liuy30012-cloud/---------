package com.library.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FuzzySearchService {

    private static final int MAX_EDIT_DISTANCE = 2;
    private static final double SIMILARITY_THRESHOLD = 0.6;

    /**
     * 计算两个字符串的编辑距离（Levenshtein距离）
     */
    public int calculateEditDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 计算相似度分数 (0-1)
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int distance = calculateEditDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 从候选列表中找到最相似的匹配
     */
    public List<SimilarityMatch> findSimilarMatches(String query, List<String> candidates) {
        if (query == null || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return candidates.stream()
            .map(candidate -> {
                double similarity = calculateSimilarity(query, candidate);
                return new SimilarityMatch(candidate, similarity);
            })
            .filter(match -> match.similarity >= SIMILARITY_THRESHOLD)
            .sorted(Comparator.comparingDouble(SimilarityMatch::similarity).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * 智能纠错建议
     */
    public Optional<String> suggestCorrection(String query, List<String> dictionary) {
        if (query == null || dictionary == null || dictionary.isEmpty()) {
            return Optional.empty();
        }

        return dictionary.stream()
            .map(word -> new SimilarityMatch(word, calculateSimilarity(query, word)))
            .filter(match -> match.similarity > SIMILARITY_THRESHOLD)
            .max(Comparator.comparingDouble(SimilarityMatch::similarity))
            .map(SimilarityMatch::text);
    }

    /**
     * 检查是否可能是拼写错误
     */
    public boolean isPossibleTypo(String query, String candidate) {
        int distance = calculateEditDistance(query, candidate);
        return distance > 0 && distance <= MAX_EDIT_DISTANCE;
    }

    public record SimilarityMatch(String text, double similarity) {}
}
