package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class VolumeDownCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
        int volume = audioPlayer.mediaPlayer().audio().volume();
        int newVolume = Math.max(0, volume - 5);  // Ensure volume does not go below 0
        audioPlayer.mediaPlayer().audio().setVolume(newVolume);

        if (newVolume == volume) {
            return "Volume is already at the minimum level.";
        } else {
            return "Volume decreased to " + newVolume + ".";
        }
    }

    @Override
    public String name() {
        return "volume-down";
    }

    @Override
    public String description() {
        return "Decreases the volume by 5 units.";
    }
}
