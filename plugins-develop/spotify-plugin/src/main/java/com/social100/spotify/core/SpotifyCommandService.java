package com.social100.spotify.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.library.SaveTracksForUserRequest;
import se.michaelthelin.spotify.requests.data.player.*;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.requests.data.playlists.RemoveItemsFromPlaylistRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyCommandService {
    public final static String MAIN_GROUP = "Main";
    public final static String PLAYLIST_GROUP = "Playlist";

    private final SpotifyApi api;
    private final String preferredDeviceId;        // may be null
    private Integer cachedVolumeBeforeMute;        // for mute toggle
    private ScheduledExecutorService eventsExec;
    private ScheduledFuture<?> eventsTask;

    public interface PlaybackListener {
        void onPlayback(String statusText);
    }

    public SpotifyCommandService(SpotifyApi api, String preferredDeviceId) {
        this.api = api;
        this.preferredDeviceId = preferredDeviceId;
    }

    /* ======================== Playlist ======================== */

    // playlist-list
    public String playlistList(String playlistId, int limit) {
        try {
            GetPlaylistsItemsRequest req = api.getPlaylistsItems(playlistId)
                    .limit(Math.max(1, Math.min(limit, 100)))
                    .build();
            Paging<PlaylistTrack> page = req.execute();

            // figure out current context / track
            CurrentlyPlayingContext ctx = safeGetPlayback();
            String currentCtxUri = (ctx != null && ctx.getContext() != null) ? ctx.getContext().getUri() : null;
            String currentTrackUri = (ctx != null && ctx.getItem() instanceof Track t) ? t.getUri() : null;

            StringJoiner sj = new StringJoiner("\n");
            sj.add("Playlist: " + playlistId);
            PlaylistTrack[] items = page.getItems();
            for (int i = 0; i < items.length; i++) {
                IPlaylistItem item = items[i].getTrack();
                if (item instanceof Track t) {
                    String artist = (t.getArtists() != null && t.getArtists().length > 0)
                            ? t.getArtists()[0].getName() : "Unknown";
                    boolean isCurrent = ("spotify:playlist:" + playlistId).equals(currentCtxUri)
                            && t.getUri().equals(currentTrackUri);
                    sj.add(String.format("%2d) %s — %s [uri=%s]%s",
                            i + 1, t.getName(), artist, t.getUri(), isCurrent ? "  <-- CURRENT" : ""));
                }
            }
            return sj.toString();
        } catch (Exception e) {
            return "playlist-list failed: " + e.getMessage();
        }
    }

    // playlist-add
    public String playlistAdd(String playlistId, List<String> trackUris) {
        try {
            JsonArray uris = new JsonArray();
            for (String u : trackUris) uris.add(new JsonPrimitive(u));
            AddItemsToPlaylistRequest req = api.addItemsToPlaylist(playlistId, uris).build();
            req.execute();
            return "Added " + trackUris.size() + " item(s) to playlist " + playlistId + ".";
        } catch (Exception e) {
            return "playlist-add failed: " + e.getMessage();
        }
    }

    // playlist-next
    public String playlistNext() {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;
            api.skipUsersPlaybackToNextTrack().build().execute();
            return "Skipped to next.";
        } catch (Exception e) {
            return "playlist-next failed: " + e.getMessage();
        }
    }

    // playlist-remove (remove current playing track if playing from a playlist)
    public String playlistRemoveCurrentIfFromPlaylist() {
        try {
            CurrentlyPlayingContext ctx = safeGetPlayback();
            if (ctx == null || ctx.getContext() == null || ctx.getContext().getUri() == null) {
                return "Nothing playing from a playlist.";
            }
            if (!(ctx.getItem() instanceof Track t)) {
                return "Current item is not a track.";
            }
            String contextUri = ctx.getContext().getUri(); // spotify:playlist:ID
            if (!contextUri.startsWith("spotify:playlist:")) {
                return "Current context is not a playlist.";
            }
            String playlistId = contextUri.substring("spotify:playlist:".length());

            JsonArray items = new JsonArray();
            JsonObject obj = new JsonObject();
            obj.addProperty("uri", t.getUri());
            items.add(obj);

            RemoveItemsFromPlaylistRequest req = api.removeItemsFromPlaylist(playlistId, items).build();
            req.execute();
            return "Removed current track from playlist " + playlistId + ".";
        } catch (Exception e) {
            return "playlist-remove failed: " + e.getMessage();
        }
    }

    /* ======================== Main ======================== */

    // play [mediaOrSearch]
    // Accepts:
    //   - null/blank  -> resume current
    //   - spotify:track/album/artist/playlist URIs
    //   - "${search term}"  -> search and play best match
    //   - "search term" (no braces) -> also search and play best match
    public String play(String mediaOrSearch) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            StartResumeUsersPlaybackRequest.Builder b = api.startResumeUsersPlayback();

            // 1) Resume if empty
            if (mediaOrSearch == null || mediaOrSearch.isBlank()) {
                // resume current context
            }
            // 2) If it's a direct Spotify URI, play it as-is
            else if (mediaOrSearch.startsWith("spotify:track:")) {
                JsonArray uris = new JsonArray();
                uris.add(new JsonPrimitive(mediaOrSearch));
                b.uris(uris);
            } else if (mediaOrSearch.startsWith("spotify:album:")
                    || mediaOrSearch.startsWith("spotify:artist:")
                    || mediaOrSearch.startsWith("spotify:playlist:")) {
                b.context_uri(mediaOrSearch);
            }
            // 3) Otherwise, treat as a search (supports "${...}" or plain text)
            else {
                String q = extractPlaceholderOrRaw(mediaOrSearch);
                // Search a *short list* of candidates
                Paging<Track> page = api.searchTracks(q).limit(5).build().execute();
                Track[] items = page.getItems();
                if (items == null || items.length == 0) {
                    return "No results found for \"" + q + "\".";
                }
                // Pick the closest match
                int bestIdx = 0;
                double bestScore = -1.0;
                for (int i = 0; i < items.length; i++) {
                    double s = scoreTrackAgainstQuery(items[i], q);
                    if (s > bestScore) { bestScore = s; bestIdx = i; }
                }
                Track best = items[bestIdx];

                // Play the best match
                JsonArray uris = new JsonArray();
                uris.add(new JsonPrimitive(best.getUri()));
                b.uris(uris);

                // Optional: include a tiny list of top candidates in the feedback
                StringBuilder picked = new StringBuilder();
                picked.append("Best match: ")
                        .append(best.getName())
                        .append(" — ")
                        .append(best.getArtists() != null && best.getArtists().length > 0 ? best.getArtists()[0].getName() : "Unknown")
                        .append(" (").append(best.getUri()).append(")\n");

                picked.append("Other candidates:\n");
                for (int i = 0, shown = 0; i < items.length && shown < 2; i++) { // show up to 2 others
                    if (i == bestIdx) continue;
                    Track t = items[i];
                    picked.append(" - ")
                            .append(t.getName()).append(" — ")
                            .append(t.getArtists() != null && t.getArtists().length > 0 ? t.getArtists()[0].getName() : "Unknown")
                            .append(" (").append(t.getUri()).append(")\n");
                    shown++;
                }

                if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
                b.build().execute();
                return "Playing search for \"" + q + "\".\n" + picked.toString().trim();
            }

            if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
            b.build().execute();

            return (mediaOrSearch == null || mediaOrSearch.isBlank())
                    ? "Resumed playback."
                    : "Playing: " + mediaOrSearch;
        } catch (Exception e) {
            return "play failed: " + e.getMessage();
        }
    }

    /** If input is "${...}", return inside; else return raw trimmed string. */
    private static String extractPlaceholderOrRaw(String input) {
        String s = input.trim();
        if (s.startsWith("${") && s.endsWith("}") && s.length() >= 3) {
            return s.substring(2, s.length() - 1).trim();
        }
        return s;
    }

    /** Lightweight scoring: token overlap + contains + popularity tie-breaker. */
    private static double scoreTrackAgainstQuery(Track t, String query) {
        String q = query.toLowerCase();
        String name = t.getName() == null ? "" : t.getName().toLowerCase();
        String artist = (t.getArtists() != null && t.getArtists().length > 0 && t.getArtists()[0].getName() != null)
                ? t.getArtists()[0].getName().toLowerCase() : "";
        String combined = (name + " " + artist).trim();

        // Basic features
        boolean exactName = name.equals(q);
        boolean contains = combined.contains(q);
        int overlap = tokenOverlap(combined, q); // crude token overlap count
        int popularity = t.getPopularity() == null ? 0 : t.getPopularity();

        double score = 0.0;
        if (exactName) score += 3.0;
        if (contains)  score += 1.5;
        score += Math.min(overlap, 5) * 0.4;            // up to +2.0
        score += (popularity / 100.0) * 0.5;            // up to +0.5
        return score;
    }

    private static int tokenOverlap(String hay, String needle) {
        String[] a = hay.split("\\s+");
        String[] b = needle.split("\\s+");
        int count = 0;
        for (String bb : b) {
            for (String aa : a) {
                if (!bb.isBlank() && aa.equals(bb)) { count++; break; }
            }
        }
        return count;
    }

    public String pause() {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;
            api.pauseUsersPlayback().build().execute();
            return "Playback paused.";
        } catch (Exception e) {
            return "pause failed: " + e.getMessage();
        }
    }

    public String stop() {
        // Spotify has no hard stop; pause is closest.
        return pause();
    }

    // volume <0..150> (mapped to 0..100)
    public String volume(int level0to150) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            int pct = Math.max(0, Math.min(100, (int)Math.round(level0to150 * (100.0/150.0))));
            SetVolumeForUsersPlaybackRequest.Builder b = api.setVolumeForUsersPlayback(pct);
            if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
            b.build().execute();

            cachedVolumeBeforeMute = (pct == 0)
                    ? (cachedVolumeBeforeMute == null ? 50 : cachedVolumeBeforeMute)
                    : pct;

            return "Volume set to " + pct + "%.";
        } catch (Exception e) {
            return "volume failed: " + e.getMessage();
        }
    }

    public String volumeUp()   { return stepVolume(+5); }
    public String volumeDown() { return stepVolume(-5); }

    private String stepVolume(int delta) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            CurrentlyPlayingContext ctx = safeGetPlayback();
            int current = (ctx != null && ctx.getDevice() != null && ctx.getDevice().getVolume_percent() != null)
                    ? ctx.getDevice().getVolume_percent() : 50;
            int next = Math.max(0, Math.min(100, current + delta));
            api.setVolumeForUsersPlayback(next).build().execute();
            return "Volume " + (delta > 0 ? "increased" : "decreased") + " to " + next + "%.";
        } catch (Exception e) {
            return "volume step failed: " + e.getMessage();
        }
    }

    public String muteToggle() {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            CurrentlyPlayingContext ctx = safeGetPlayback();
            int current = (ctx != null && ctx.getDevice() != null && ctx.getDevice().getVolume_percent() != null)
                    ? ctx.getDevice().getVolume_percent() : 50;
            if (current == 0) {
                int restore = (cachedVolumeBeforeMute == null) ? 50 : Math.max(1, Math.min(100, cachedVolumeBeforeMute));
                api.setVolumeForUsersPlayback(restore).build().execute();
                return "Playback unmuted. Volume " + restore + "%.";
            } else {
                cachedVolumeBeforeMute = current;
                api.setVolumeForUsersPlayback(0).build().execute();
                return "Playback muted.";
            }
        } catch (Exception e) {
            return "mute failed: " + e.getMessage();
        }
    }

    public String move(String timestamp) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            int posMs = (int) parseTimestampToMillis(timestamp);
            api.seekToPositionInCurrentlyPlayingTrack(posMs).build().execute();
            return "Moved to " + timestamp + ".";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "move failed: " + e.getMessage();
        }
    }

    public String skipSeconds(int seconds) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            CurrentlyPlayingContext ctx = safeGetPlayback();
            if (ctx == null || !(ctx.getItem() instanceof Track t)) return "Nothing is playing.";
            int cur = (ctx.getProgress_ms() != null) ? ctx.getProgress_ms() : 0;
            int dur = (t.getDurationMs() != null) ? t.getDurationMs() : 0;
            int next = Math.max(0, Math.min(dur == 0 ? Integer.MAX_VALUE : dur - 1, cur + seconds * 1000));
            api.seekToPositionInCurrentlyPlayingTrack(next).build().execute();
            return "Skipped to " + (next / 1000) + "s.";
        } catch (Exception e) {
            return "skip failed: " + e.getMessage();
        }
    }

    // status / status all
    public String status(boolean all) {
        try {
            CurrentlyPlayingContext c = safeGetPlayback();
            if (c == null) return "No active playback.";

            StringBuilder sb = new StringBuilder();
            String deviceName = c.getDevice() != null ? c.getDevice().getName() : "<unknown device>";
            sb.append("Device: ").append(deviceName).append("\n");
            sb.append("Playing: ").append(Boolean.TRUE.equals(c.getIs_playing())).append("\n");

            if (c.getContext() != null) {
                String ctxUri = c.getContext().getUri();
                String ctxType = (c.getContext().getType() != null) ? c.getContext().getType().getType() : null;
                sb.append("Context: ").append(ctxType).append(" (").append(ctxUri).append(")\n");
            }

            if (c.getItem() instanceof Track t) {
                String artist = (t.getArtists() != null && t.getArtists().length > 0) ? t.getArtists()[0].getName() : "Unknown";
                sb.append("Track: ").append(t.getName()).append(" — ").append(artist).append("\n");
                sb.append("URI: ").append(t.getUri()).append("\n");
                int dur = (t.getDurationMs() != null) ? t.getDurationMs() : 0;
                int pos = (c.getProgress_ms() != null) ? c.getProgress_ms() : 0;
                sb.append("Position: ").append(formatTime(pos)).append(" / ").append(formatTime(dur)).append("\n");
            }

            if (all) {
                sb.append("Shuffle: ").append(Boolean.TRUE.equals(c.getShuffle_state())).append("\n");
                sb.append("Repeat: ").append(c.getRepeat_state()).append("\n");
                if (c.getDevice() != null && c.getDevice().getVolume_percent() != null) {
                    sb.append("Volume: ").append(c.getDevice().getVolume_percent()).append("%\n");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "status failed: " + e.getMessage();
        }
    }

    public String events(String onOrOff, long intervalMs, PlaybackListener listener) {
        if ("OFF".equalsIgnoreCase(onOrOff)) {
            if (eventsTask != null) eventsTask.cancel(true);
            if (eventsExec != null) eventsExec.shutdownNow();
            eventsTask = null; eventsExec = null;
            return "Events stopped.";
        }
        if (!"ON".equalsIgnoreCase(onOrOff)) return "Use events ON|OFF.";
        if (listener == null) return "Listener required.";

        if (eventsExec != null) { events("OFF", 0, null); }
        eventsExec = Executors.newSingleThreadScheduledExecutor();
        eventsTask = eventsExec.scheduleAtFixedRate(() -> {
            try {
                listener.onPlayback(status(true));
            } catch (Exception ignored) {}
        }, 0, Math.max(250, intervalMs), TimeUnit.MILLISECONDS);
        return "Events started.";
    }

    /* ================== Spotify-specific extras ================== */

    public String shuffle(boolean enabled) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            ToggleShuffleForUsersPlaybackRequest.Builder b = api.toggleShuffleForUsersPlayback(enabled);
            if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
            b.build().execute();
            return "Shuffle " + (enabled ? "enabled." : "disabled.");
        } catch (Exception e) {
            return "shuffle failed: " + e.getMessage();
        }
    }

    public String repeat(String mode /* off|context|track */) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            SetRepeatModeOnUsersPlaybackRequest.Builder b = api.setRepeatModeOnUsersPlayback(mode);
            if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
            b.build().execute();
            return "Repeat set to: " + mode + ".";
        } catch (Exception e) {
            return "repeat failed: " + e.getMessage();
        }
    }

    public String queueAdd(String trackOrEpisodeUri) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            AddItemToUsersPlaybackQueueRequest.Builder b = api.addItemToUsersPlaybackQueue(trackOrEpisodeUri);
            if (preferredDeviceId != null && !preferredDeviceId.isBlank()) b = b.device_id(preferredDeviceId);
            b.build().execute();
            return "Queued: " + trackOrEpisodeUri + ".";
        } catch (Exception e) {
            return "queue add failed: " + e.getMessage();
        }
    }

    public String devices() {
        try {
            Device[] ds = api.getUsersAvailableDevices().build().execute();
            if (ds == null || ds.length == 0) return "No devices online.";
            StringJoiner sj = new StringJoiner("\n");
            sj.add("Devices:");
            for (Device d : ds) {
                sj.add(String.format("- %s  id=%s  active=%s  type=%s  volume=%s%%",
                        d.getName(), d.getId(), d.getIs_active(), d.getType(),
                        d.getVolume_percent() == null ? "?" : d.getVolume_percent().toString()));
            }
            return sj.toString();
        } catch (Exception e) {
            return "devices failed: " + e.getMessage();
        }
    }

    public String selectDevice(String deviceId) {
        try {
            JsonArray ids = new JsonArray();
            ids.add(new JsonPrimitive(deviceId));
            TransferUsersPlaybackRequest req = api.transferUsersPlayback(ids).build();
            req.execute();
            return "Transferred playback to device " + deviceId + ".";
        } catch (Exception e) {
            return "select-device failed: " + e.getMessage();
        }
    }

    public String likeCurrentTrack() {
        try {
            CurrentlyPlayingContext c = safeGetPlayback();
            if (c == null || !(c.getItem() instanceof Track t)) return "Nothing playing.";
            SaveTracksForUserRequest req = api.saveTracksForUser(new String[]{ t.getId() }).build();
            req.execute();
            return "Saved to your library: " + t.getName() + ".";
        } catch (Exception e) {
            return "like failed: " + e.getMessage();
        }
    }

    public String playContextWithOffset(String contextUri, int index) {
        try {
            String ensure = ensureActiveDeviceString();
            if (ensure != null) return ensure;

            StartResumeUsersPlaybackRequest.Builder b = api.startResumeUsersPlayback().context_uri(contextUri);
            JsonObject offset = new JsonObject();
            offset.addProperty("position", Math.max(0, index));
            b.offset(offset);
            b.build().execute();
            return "Playing context " + contextUri + " at index " + index + ".";
        } catch (Exception e) {
            return "play-context failed: " + e.getMessage();
        }
    }

    /* ======================== Helpers ======================== */

    private String ensureActiveDeviceString() {
        try {
            ensureActiveDevice();
            return null;
        } catch (Exception e) {
            return e.getMessage() == null ? "No active device." : e.getMessage();
        }
    }

    private void ensureActiveDevice() throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        Device[] ds = api.getUsersAvailableDevices().build().execute();
        if (ds == null || ds.length == 0) {
            throw new IllegalStateException("No devices online. Open a Spotify app and try again.");
        }
        // If preferred device is set, ensure it is active
        if (preferredDeviceId != null && !preferredDeviceId.isBlank()) {
            boolean found = false, active = false;
            for (Device d : ds) {
                if (preferredDeviceId.equals(d.getId())) {
                    found = true;
                    active = Boolean.TRUE.equals(d.getIs_active());
                    break;
                }
            }
            if (!found) throw new IllegalStateException("Preferred device not found. Use 'devices' to list.");
            if (!active) {
                JsonArray ids = new JsonArray();
                ids.add(new JsonPrimitive(preferredDeviceId));
                TransferUsersPlaybackRequest req = api.transferUsersPlayback(ids).build();
                req.execute();
            }
            return;
        }
        // Otherwise, if none active, activate first
        for (Device d : ds) if (Boolean.TRUE.equals(d.getIs_active())) return;
        JsonArray ids = new JsonArray();
        ids.add(new JsonPrimitive(ds[0].getId()));
        TransferUsersPlaybackRequest req = api.transferUsersPlayback(ids).build();
        req.execute();
    }

    private CurrentlyPlayingContext safeGetPlayback() {
        try {
            return api.getInformationAboutUsersCurrentPlayback().build().execute();
        } catch (Exception e) {
            return null;
        }
    }

    private static long parseTimestampToMillis(String s) {
        // Accept HH:MM:SS | MM:SS | SS
        Pattern p = Pattern.compile("^(?:(\\d+):)?(\\d{1,2}):(\\d{2})$|^(\\d+)$");
        Matcher m = p.matcher(s.trim());
        if (!m.matches()) throw new IllegalArgumentException("Invalid time. Use HH:MM:SS, MM:SS or SS.");
        if (m.group(4) != null) { // SS
            long sec = Long.parseLong(m.group(4));
            return Duration.ofSeconds(sec).toMillis();
        }
        long h = (m.group(1) == null) ? 0 : Long.parseLong(m.group(1));
        long min = Long.parseLong(m.group(2));
        long sec = Long.parseLong(m.group(3));
        return Duration.ofHours(h).plusMinutes(min).plusSeconds(sec).toMillis();
    }

    private static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return (h > 0)
                ? String.format("%02d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }
}