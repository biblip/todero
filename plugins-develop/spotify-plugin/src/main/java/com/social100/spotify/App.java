package com.social100.spotify;

import com.social100.spotify.core.SpotifyPkceService;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class App {
    public static void main(String[] args) {
        SpotifyPkceService service = new SpotifyPkceService();

        String query = (args.length > 0) ? String.join(" ", args) : "daft punk get lucky";
        Paging<Track> results = service.searchTracks(query, 5);

        System.out.println("Top results for: " + query);
        Track[] tracks = results.getItems();
        for (int i = 0; i < tracks.length; i++) {
            Track t = tracks[i];
            String artist = t.getArtists().length > 0 ? t.getArtists()[0].getName() : "Unknown";
            System.out.printf("%d) %s — %s [uri=%s]%n", i + 1, t.getName(), artist, t.getUri());
        }

        if (tracks.length > 0) {
            Track first = tracks[0];
            String artist = first.getArtists().length > 0 ? first.getArtists()[0].getName() : "Unknown";
            System.out.println("\nPlaying first result: " + first.getName() + " — " + artist);
            service.playTrackUri(first.getUri());
        } else {
            System.out.println("No results.");
        }
    }
}
