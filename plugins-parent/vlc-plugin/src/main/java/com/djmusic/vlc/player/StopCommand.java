package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class StopCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
        State currentState = audioPlayer.mediaPlayer().status().state();

        if (currentState != State.STOPPED) {
            audioPlayer.mediaPlayer().controls().stop();
            return "Playback stopped.";
        } else {
            return "Playback is already stopped.";
        }
    }

    @Override
    public String name() {
        return "stop";
    }

    @Override
    public String description() {
        return "Stops the playback if it is currently active.";
    }
}
