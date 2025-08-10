package com.social100.spotify.core;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;

public class TokenStore {
    private static final Gson GSON = new Gson();
    private final Path path;

    public static class TokenData {
        @SerializedName("access_token") public String accessToken;
        @SerializedName("refresh_token") public String refreshToken;
        @SerializedName("expires_at_epoch") public long expiresAtEpoch; // seconds
    }

    public TokenStore(Path path) {
        this.path = path;
    }

    public Optional<TokenData> read() {
        try {
            if (!Files.exists(path)) return Optional.empty();
            String json = Files.readString(path, StandardCharsets.UTF_8);
            TokenData td = GSON.fromJson(json, TokenData.class);
            return Optional.ofNullable(td);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void write(TokenData td) throws IOException {
        Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
        String json = GSON.toJson(td);
        Files.writeString(path, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            // best effort: owner-only
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (Exception ignored) {}
    }

    public static boolean isExpired(TokenData td) {
        return Instant.now().getEpochSecond() >= td.expiresAtEpoch - 30; // 30s skew
    }
}
