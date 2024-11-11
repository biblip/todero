package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class PauseCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
        State state = audioPlayer.mediaPlayer().status().state();

        // If the player is currently playing, it will be paused
        if (state == State.PLAYING) {
            audioPlayer.mediaPlayer().controls().pause();
            return "Playback paused.";
        }
        // If the player is already paused, it might be intended to resume playback
        else if (state == State.PAUSED) {
            audioPlayer.mediaPlayer().controls().play();
            return "Playback resumed.";
        } else {
            return "Playback is not active. Current state: " + state;
        }
    }

    @Override
    public String name() {
        return "pause";
    }

    @Override
    public String description() {
        return "Pauses the playback if it is currently playing.";
    }
}
