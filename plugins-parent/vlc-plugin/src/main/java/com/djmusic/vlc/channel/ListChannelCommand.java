package com.djmusic.vlc.channel;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;

public class ListChannelCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        ChannelManager channelManager = (ChannelManager) object;
        return channelManager.listChannels();
    }

    @Override
    public String name() {
        return "list-channels";
    }

    @Override
    public String description() {
        return "Lists all available channels.";
    }
}
