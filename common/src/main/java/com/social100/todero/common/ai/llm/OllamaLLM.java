package com.social100.todero.common.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class OllamaLLM implements LLMClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  private final String baseUrl;
  private final String model;

  public OllamaLLM(String baseUrl, String model) {
    this(baseUrl, model, 60, 60, 60);
  }

  public OllamaLLM(String baseUrl, String model, int connectTimeoutSeconds, int readTimeoutSeconds, int writeTimeoutSeconds) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    this.model = model;
    this.client = new OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .build();
  }

  @Override
  public String chat(String systemPrompt, String userPrompt, String contextJson) throws IOException {
    // Merge system prompt and user prompt with context
    String fullPrompt = systemPrompt + "\n" + userPrompt + "\nContext:\n" + contextJson;

    // Build JSON request body
    String body = mapper.createObjectNode()
        .put("model", model)
        .put("prompt", fullPrompt)
        .put("stream", false)
        .toString();

    Request request = new Request.Builder()
        .url(baseUrl + "api/generate")
        .post(RequestBody.create(body, JSON))
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code: " + response);
      }

      String responseBody = response.body().string();
      JsonNode jsonNode = mapper.readTree(responseBody);
      return jsonNode.get("response").asText().trim();
    }
  }
}
