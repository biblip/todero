package com.social100.spotify.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.player.GetUsersAvailableDevicesRequest;
import se.michaelthelin.spotify.requests.data.player.StartResumeUsersPlaybackRequest;
import se.michaelthelin.spotify.requests.data.player.TransferUsersPlaybackRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import spark.Spark;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class SpotifyPkceService {

    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final Gson GSON = new Gson();

    private final String clientId;
    private final URI redirectUri;
    private final String deviceIdEnv; // optional
    private final SpotifyApi spotifyApi;
    private final TokenStore tokenStore;

    private String codeVerifier;

    public SpotifyPkceService() {
        // If you want to use ".env" instead: Dotenv.load();
        Dotenv env = Dotenv.configure().filename(".env-spotify").load();
        this.clientId = required(env.get("SPOTIFY_CLIENT_ID"), "SPOTIFY_CLIENT_ID");
        this.redirectUri = URI.create(required(env.get("SPOTIFY_REDIRECT_URI"), "SPOTIFY_REDIRECT_URI"));
        this.deviceIdEnv = env.get("SPOTIFY_DEVICE_ID");

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setRedirectUri(redirectUri)
                .build();

        this.tokenStore = new TokenStore(Path.of(".spotify_tokens.json"));
        ensureAuthorized();
    }

    private static String required(String v, String key) {
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env: " + key);
        return v;
    }

    /* ===================== AUTH ===================== */

    private void ensureAuthorized() {
        Optional<TokenStore.TokenData> saved = tokenStore.read();
        if (saved.isPresent()) {
            TokenStore.TokenData td = saved.get();
            if (TokenStore.isExpired(td)) {
                try {
                    td = refresh(td.refreshToken);
                    tokenStore.write(td);
                } catch (Exception e) {
                    System.err.println("Refresh failed, re-authorizing: " + e.getMessage());
                    authorizeInteractive();
                    return;
                }
            }
            applyTokens(td);
            return;
        }
        authorizeInteractive();
    }

    private void authorizeInteractive() {
        try {
            // PKCE
            codeVerifier = PkceUtil.generateCodeVerifier();
            String codeChallenge = PkceUtil.codeChallengeS256(codeVerifier);

            String scopes = String.join(" ",
                    "user-read-playback-state",
                    "user-modify-playback-state"
            );

            String url = UriBuilder.of(AUTH_URL)
                    .add("response_type", "code")
                    .add("client_id", clientId)
                    .add("redirect_uri", redirectUri.toString())
                    .add("scope", scopes)
                    .add("code_challenge_method", "S256")
                    .add("code_challenge", codeChallenge)
                    .build();

            System.out.println("\nOpen this URL to authorize:\n" + url + "\n");
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception ignored) {}

            final CountDownLatch latch = new CountDownLatch(1);
            final String[] codeHolder = new String[1];

            // Start local callback server
            int port = extractPort(redirectUri);
            String path = redirectUri.getPath().isEmpty() ? "/" : redirectUri.getPath();

            Spark.port(port);
            Spark.get(path, (req, res) -> {
                String code = req.queryParams("code");
                String error = req.queryParams("error");
                if (error != null) {
                    res.status(400);
                    String msg = "Authorization error: " + error;
                    System.err.println(msg);
                    return msg;
                }
                if (code == null) {
                    res.status(400);
                    return "Missing 'code'";
                }
                codeHolder[0] = code;
                latch.countDown();
                return "Spotify authorization successful. You can close this tab.";
            });

            latch.await();
            Spark.stop();

            TokenStore.TokenData td = exchangeCodeForTokens(codeHolder[0], codeVerifier);
            tokenStore.write(td);
            applyTokens(td);
            System.out.println("Authorization complete.");
        } catch (Exception e) {
            throw new RuntimeException("Authorization failed", e);
        }
    }

    private static int extractPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            // default HTTP
            return 80;
        }
        return port;
    }

    private TokenStore.TokenData exchangeCodeForTokens(String code, String codeVerifier)
            throws IOException, InterruptedException {
        HttpClient http = HttpClient.newHttpClient();

        String body = UriBuilder.of("") // form body
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri.toString())
                .add("client_id", clientId)
                .add("code_verifier", codeVerifier)
                .formBody();

        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Token exchange failed: " + resp.statusCode() + " " + resp.body());
        }

        return parseTokenResponse(resp.body());
    }

    private TokenStore.TokenData refresh(String refreshToken) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newHttpClient();

        String body = UriBuilder.of("") // form body
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .formBody();

        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Refresh failed: " + resp.statusCode() + " " + resp.body());
        }

        TokenStore.TokenData td = parseTokenResponse(resp.body());
        if (td.refreshToken == null || td.refreshToken.isBlank()) {
            td.refreshToken = refreshToken; // reuse old on rotation-less refresh
        }
        return td;
    }

    private TokenStore.TokenData parseTokenResponse(String json) {
        TokenResponse tr = GSON.fromJson(json, TokenResponse.class);
        if (tr.accessToken == null) throw new RuntimeException("No access_token in response");
        TokenStore.TokenData td = new TokenStore.TokenData();
        td.accessToken = tr.accessToken;
        td.refreshToken = tr.refreshToken; // may be null on refresh
        td.expiresAtEpoch = Instant.now().getEpochSecond() + (tr.expiresIn != null ? tr.expiresIn : 3600);
        return td;
    }

    private void applyTokens(TokenStore.TokenData td) {
        spotifyApi.setAccessToken(td.accessToken);
        if (td.refreshToken != null) {
            spotifyApi.setRefreshToken(td.refreshToken);
        }
    }

    private record TokenResponse(
            @SerializedName("access_token") String accessToken,
            @SerializedName("refresh_token") String refreshToken,
            @SerializedName("expires_in") Integer expiresIn,
            @SerializedName("token_type") String tokenType,
            @SerializedName("scope") String scope
    ) {}

    /* ===================== API ===================== */

    public SpotifyApi getSpotifyApi() {
        return this.spotifyApi;
    }

    public Paging<Track> searchTracks(String q, int limit) {
        ensureFreshToken();
        try {
            SearchTracksRequest req = spotifyApi.searchTracks(q).limit(limit).build();
            return req.execute();
        } catch (Exception e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    public void playTrackUri(String trackUri) {
        ensureFreshToken();
        ensureActiveDevice(); // make sure a device is active before attempting playback

        JsonArray uris = new JsonArray();
        uris.add(new JsonPrimitive(trackUri));

        StartResumeUsersPlaybackRequest.Builder b =
                spotifyApi.startResumeUsersPlayback().uris(uris);

        if (deviceIdEnv != null && !deviceIdEnv.isBlank()) {
            b = b.device_id(deviceIdEnv);
        }
        try {
            b.build().execute();
            System.out.println("Requested playback for " + trackUri +
                    (deviceIdEnv != null ? " on device " + deviceIdEnv : ""));
        } catch (Exception e) {
            throw new RuntimeException("Playback failed. Is a Spotify device active and authorized?", e);
        }
    }

    /** List available devices for this user. */
    public Device[] listDevices() {
        ensureFreshToken();
        try {
            GetUsersAvailableDevicesRequest req = spotifyApi.getUsersAvailableDevices().build();
            return req.execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get devices", e);
        }
    }

    /** Ensure there is an active device. If SPOTIFY_DEVICE_ID is set, activate it; else activate the first available. */
    private void ensureActiveDevice() {
        ensureFreshToken();
        try {
            Device[] devices = listDevices();
            if (devices == null || devices.length == 0) {
                throw new IllegalStateException(
                        "No devices found. Open Spotify on any device (desktop/mobile/web), log in, and try again.");
            }

            // If a specific device is configured, activate it
            if (deviceIdEnv != null && !deviceIdEnv.isBlank()) {
                boolean exists = false;
                for (Device d : devices) {
                    if (deviceIdEnv.equals(d.getId())) { exists = true; break; }
                }
                if (!exists) {
                    StringBuilder sb = new StringBuilder("Configured device not found. Available devices:\n");
                    for (Device d : devices) {
                        sb.append("- ").append(d.getName()).append(" (").append(d.getId()).append(") active=")
                                .append(d.getIs_active()).append(" type=").append(d.getType()).append("\n");
                    }
                    throw new IllegalStateException(sb.toString());
                }
                transferToDevice(deviceIdEnv);
                return;
            }

            // If any device is already active, we're good
            for (Device d : devices) {
                if (Boolean.TRUE.equals(d.getIs_active())) return;
            }

            // Otherwise, activate the first available device
            String targetId = devices[0].getId();
            transferToDevice(targetId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure active device", e);
        }
    }

    private void transferToDevice(String deviceId) throws Exception {
        JsonArray ids = new JsonArray();
        ids.add(new JsonPrimitive(deviceId));

        TransferUsersPlaybackRequest req = spotifyApi
                .transferUsersPlayback(ids)
                .build();
        req.execute();
    }

    /** Convenience: print devices to stdout (ids, names, active). */
    public void printDevices() {
        Device[] devices = listDevices();
        System.out.println("Available devices:");
        for (Device d : devices) {
            System.out.printf("- %s  id=%s  active=%s  type=%s%n",
                    d.getName(), d.getId(), d.getIs_active(), d.getType());
        }
    }

    private void ensureFreshToken() {
        Optional<TokenStore.TokenData> saved = tokenStore.read();
        if (saved.isEmpty()) return;
        TokenStore.TokenData td = saved.get();
        if (TokenStore.isExpired(td)) {
            try {
                TokenStore.TokenData newTd = refresh(td.refreshToken);
                tokenStore.write(newTd);
                applyTokens(newTd);
            } catch (Exception e) {
                System.err.println("Auto-refresh failed, trying once more via interactive login...");
                authorizeInteractive();
            }
        } else {
            applyTokens(td);
        }
    }

    /* ===== simple URL/form helpers ===== */

    private static class UriBuilder {
        private final StringBuilder sb;
        private boolean firstParam = true;

        private UriBuilder(String base) {
            this.sb = new StringBuilder(base == null ? "" : base);
        }

        static UriBuilder of(String base) { return new UriBuilder(base); }

        UriBuilder add(String k, String v) {
            sb.append(firstParam ? '?' : '&');
            firstParam = false;
            sb.append(URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8));
            return this;
        }

        String build() {
            return sb.toString(); // no double base
        }

        String formBody() {
            String s = sb.toString();
            return s.startsWith("?") ? s.substring(1) : s;
        }
    }
}