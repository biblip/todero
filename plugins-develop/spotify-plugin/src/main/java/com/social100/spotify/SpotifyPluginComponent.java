package com.social100.spotify;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.spotify.core.SpotifyCommandService;
import com.social100.spotify.core.SpotifyPkceService;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

@AIAController(name = "com.shellaia.verbatim.plugin.spotify",
        type = ServerType.AIA,
        visible = true,
        description = "description")
public class SpotifyPluginComponent {
    SpotifyPkceService spotifyPkceService;
    SpotifyCommandService spotifyCommandService;

    public SpotifyPluginComponent() {
        this.spotifyPkceService = null;
        this.spotifyCommandService = null;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "login",
            description = "Authorize current device to playback music into user´s devices - no parameters needed")
    public Boolean login(CommandContext context) {
        String[] args = context.getArgs();
        SpotifyPkceService service = new SpotifyPkceService();

        this.spotifyCommandService = new SpotifyCommandService(service.getSpotifyApi(), null);
        /*
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
        }*/
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "move",
            description = "Moves the playback to the specified time. Usage: move <HH:MM:SS|MM:SS|SS>")
    public Boolean moveCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please specify the time to move to. Usage: move <time>");
            return true;
        }
        String moveTo = args[0];
        context.respond(this.spotifyCommandService.move(moveTo));
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "mute",
            description = "Toggles the mute state of the playback if valid media is loaded.")
    public Boolean muteCommand(CommandContext context) {
        context.respond(this.spotifyCommandService.muteToggle());
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "pause",
            description = "Pauses the playback if it is currently playing.")
    public Boolean pauseCommand(CommandContext context) {
        context.respond(this.spotifyCommandService.pause());
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "play",
            description = "Plays the specified media file. If no file is specified, resumes the current one. Usage: play [media]")
    public Boolean playCommand(CommandContext context) {
        String[] args = context.getArgs();
        String mediaPathToPlay = "";
        if (args.length > 0) {
            mediaPathToPlay = args[0];
        }
        context.respond(this.spotifyCommandService.play(mediaPathToPlay));
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "skip",
            description = "Skips the playback forward or backward by the specified number of seconds. Usage: skip <+/-seconds>")
    public Boolean skipCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please specify the number of seconds to skip. Usage: skip <+/-seconds>");
            return true;
        }
        int skipTime = Integer.parseInt(args[0]);
        context.respond(this.spotifyCommandService.skipSeconds(skipTime));
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "status",
            description = "Displays the current status of VLC. Use 'status all' for all media info available.")
    public Boolean statusCommand(CommandContext context) {
        String[] args = context.getArgs();
        boolean all = (args.length > 0 && "all".equalsIgnoreCase(args[0]));
        context.respond(this.spotifyCommandService.status(all));
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "stop",
            description = "Stops the playback if it is currently active.")
    public Boolean stopCommand(CommandContext context) {
        context.respond(this.spotifyCommandService.stop());
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "volume",
            description = "Sets the volume to a specified level between 0 and 150. Usage: volume <level>")
    public Boolean volumeCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("No volume level provided. Please specify a volume level between 0 and 150.");
            return true;
        }
        int volume = Integer.parseInt(args[0]);
        context.respond(this.spotifyCommandService.volume(volume));
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "volume-down",
            description = "Decreases the volume by 5 units.")
    public Boolean volumeDownCommand(CommandContext context) {
        context.respond(this.spotifyCommandService.volumeDown());
        return true;
    }

    @Action(group = SpotifyCommandService.MAIN_GROUP,
            command = "volume-up",
            description = "Increases the volume by 5 units.")
    public Boolean volumeUpCommand(CommandContext context) {
        context.respond(this.spotifyCommandService.volumeUp());
        return true;
    }

    /*
    @Action(group = SpotifyCommandService.PLAYLIST_GROUP,
            command = "playlist-add",
            description = "Adds the specified media file to the playlist. Usage: add-playlist [media]")
    public Boolean playlistAdd(CommandContext context) {
        String[] args = context.getArgs();
        String mediaPathToAdd = "";
        if (args.length > 0) {
            mediaPathToAdd = args[0];
        }
        context.respond(this.spotifyCommandService.playlistAdd(mediaPathToAdd));
        return true;
    }*/

    @Action(group = SpotifyCommandService.PLAYLIST_GROUP,
            command = "playlist-remove",
            description = "Remove current paying media from the playlist Usage: playlist-remove, if there is no current media playing then does nothing")
    public Boolean playlistRemove(CommandContext context) {
        context.respond(this.spotifyCommandService.playlistRemoveCurrentIfFromPlaylist());
        return true;
    }

    @Action(group = SpotifyCommandService.PLAYLIST_GROUP,
            command = "playlist-next",
            description = "Play the next media in the playlist. Usage: playlist-next")
    public Boolean playlistNext(CommandContext context) {
        context.respond(this.spotifyCommandService.playlistNext());
        return true;
    }
/*
    @Action(group = SpotifyCommandService.PLAYLIST_GROUP,
            command = "playlist-list",
            description = "Inform the user of the playlist items and which is the current item. Usage: playlist-list")
    public Boolean playlistList(CommandContext context) {
        context.respond(this.spotifyCommandService.playlistList());
        return true;
    }*/
}
