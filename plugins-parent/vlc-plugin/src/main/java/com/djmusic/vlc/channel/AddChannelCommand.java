package com.djmusic.vlc.channel;


import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;

public class AddChannelCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        ChannelManager channelManager = (ChannelManager) object;
        if (args.length > 0) {
            String channelName = args[0];
            return channelManager.addChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: add-channel <channelName>";
        }
    }

    @Override
    public String name() {
        return "add-channel";
    }

    @Override
    public String description() {
        return "Adds a new channel. Usage: add-channel <channelName>";
    }
}
