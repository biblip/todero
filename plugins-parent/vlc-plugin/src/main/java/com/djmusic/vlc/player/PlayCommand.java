package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.io.File;

public class PlayCommand implements Command {

    private static final String[] mediaOptions = {
            ":audio-filter=normvol",
            ":norm-buff-size=20",  // Buffer size for normalization
            ":norm-max-level=1.0"  // Maximum level for normalized audio
    };

    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
        MediaPlayer mediaPlayer = audioPlayer.mediaPlayer();

        String currentMediaPath = mediaPlayer.media().info() != null ? mediaPlayer.media().info().mrl() : null;

        if (args.length > 0) {
            String mediaPathToPlay = args[0];
            File file = new File(mediaPathToPlay);

            if (!file.exists()) {
                return "File not found: " + mediaPathToPlay;
            }

            if (!mediaPathToPlay.equals(currentMediaPath)) {
                mediaPlayer.media().play(mediaPathToPlay, mediaOptions);
                return "Playing new media: \"" + mediaPathToPlay + "\"";
            } else {
                mediaPlayer.controls().play();
                return "Resuming current media.";
            }
        } else if (mediaPlayer.media().isValid()) {
            mediaPlayer.controls().play();
            return "Resuming playback of current media.";
        } else {
            return "No media file specified and no current media to play.";
        }
    }

    @Override
    public String name() {
        return "play";
    }

    @Override
    public String description() {
        return "Plays the specified media file. If no file is specified, resumes the current one.";
    }
}
