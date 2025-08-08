package com.social100.todero.common.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.io.IOException;

public class OpenAiLLM implements LLMClient {

  private final String apiKey;
  private final String model;
  private final ObjectMapper mapper = new ObjectMapper();
  private final OpenAIClient openAiClient;

  public OpenAiLLM(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model;
    this.openAiClient = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .build();
  }

  @Override
  public String chat(String systemPrompt, String userPrompt, String contextJson) throws IOException {
    String userContent = userPrompt + "\nContext:\n" + contextJson;

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .model(ChatModel.of(model))
        .addSystemMessage(systemPrompt)
        .addUserMessage(userContent)
        .build();

    ChatCompletion completion = openAiClient
        .chat()
        .completions()
        .create(params);

    return completion.choices()
        .get(0)
        .message()
        .content()
        .orElse("")
        .trim();
  }
}