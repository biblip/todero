package com.social100.todero.common.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  // Matches fenced markdown code blocks like ```json { ... } ```
  private static final Pattern MARKDOWN_JSON_PATTERN = Pattern.compile(
      "(?s)```json\\s*(\\{.*?})\\s*```"
  );

  // Fallback pattern to find any JSON-like object
  private static final Pattern JSON_FALLBACK_PATTERN = Pattern.compile(
      "(?s)(\\{.*?})"
  );

  /**
   * Parses raw JSON into a JsonNode.
   */
  public static JsonNode parse(String json) throws Exception {
    return mapper.readTree(json);
  }

  /**
   * Safely gets a nested value by dot-path: e.g. "plan.action"
   */
  public static Optional<String> getValue(JsonNode root, String path) {
    String[] parts = path.split("\\.");
    JsonNode current = root;

    for (String part : parts) {
      if (current.has(part)) {
        current = current.get(part);
      } else {
        return Optional.empty();
      }
    }

    return current.isValueNode() ? Optional.of(current.asText()) : Optional.empty();
  }

  /**
   * Extracts the first valid JSON object from a model response,
   * optionally stripping <think> blocks or markdown fences.
   */
  public static JsonNode extractFirstJsonBlock(String responseText) throws Exception {
    // Step 1: Remove <think>...</think> blocks (optional reasoning noise)
    String cleaned = responseText.replaceAll("(?s)<think>.*?</think>", "").trim();

    // Step 2: Try to find a markdown-fenced JSON block
    Matcher markdown = MARKDOWN_JSON_PATTERN.matcher(cleaned);
    if (markdown.find()) {
      String fencedJson = markdown.group(1).trim();
      try {
        return mapper.readTree(fencedJson);
      } catch (Exception e) {
        // Fallback to loose matching
      }
    }

    // Step 3: Try to find any JSON object using a fallback pattern
    Matcher fallback = JSON_FALLBACK_PATTERN.matcher(cleaned);
    while (fallback.find()) {
      String candidate = fallback.group(1).trim();
      try {
        return mapper.readTree(candidate);
      } catch (Exception e) {
        // Optional: uncomment to log failed attempts
        // System.err.println("Skipping invalid candidate:\n" + candidate);
      }
    }

    // Step 4: No valid JSON found
    throw new RuntimeException("No valid JSON object found in response.");
  }
}