package com.social100.todero.protocol.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayProtectionManager {
  private static final long MAX_SKEW_MS = 5 * 60 * 1000; // 5 min
  private final Set<String> recentNonces = ConcurrentHashMap.newKeySet();

  public boolean isValid(long timestamp, String nonce) {
    long now = System.currentTimeMillis();
    if (Math.abs(now - timestamp) > MAX_SKEW_MS) return false;
    return recentNonces.add(nonce); // false = duplicate
  }
}
