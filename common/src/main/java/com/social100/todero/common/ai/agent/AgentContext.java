package com.social100.todero.common.ai.agent;

import java.util.HashMap;
import java.util.Map;

public class AgentContext {
  private final Map<String, Object> data = new HashMap<>();

  public void set(String key, Object value) {
    data.put(key, value);
  }

  public Object get(String key) {
    return data.get(key);
  }

  public String getAsString(String key) {
    Object val = data.get(key);
    return val != null ? val.toString() : null;
  }

  public Map<String, Object> getAll() {
    return data;
  }
}
