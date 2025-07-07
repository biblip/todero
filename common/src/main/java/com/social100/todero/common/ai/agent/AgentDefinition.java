package com.social100.todero.common.ai.agent;

import lombok.Builder;
import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
public class AgentDefinition {
  private String name;
  private String role;
  private String description;
  private String model;
  private String systemPrompt;

  @Builder.Default
  private final Map<String, Object> metadata = new HashMap<>();

  public void setMetadata(String key, Object value) {
    metadata.put(key, value);
  }

  public Object getMetadata(String key) {
    return metadata.get(key);
  }

  @Override
  public String toString() {
    return "AgentDefinition{" +
        "name='" + name + '\'' +
        ", role='" + role + '\'' +
        ", description='" + description + '\'' +
        ", model='" + model + '\'' +
        ", systemPrompt='" + systemPrompt + '\'' +
        ", metadata=" + metadata +
        '}';
  }

  public static String loadSystemPromptFromResource(String path) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        Objects.requireNonNull(AgentDefinition.class.getClassLoader().getResourceAsStream(path))
    ))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException("Failed to load system prompt from " + path, e);
    }
  }
}