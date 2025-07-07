package com.social100.todero.common.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

public class JsonUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

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
}
