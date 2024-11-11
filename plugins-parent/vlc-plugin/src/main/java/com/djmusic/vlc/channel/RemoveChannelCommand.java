package com.djmusic.vlc.channel;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;

public class RemoveChannelCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        // Retrieve the ChannelManager from shared objects
        ChannelManager channelManager = (ChannelManager) object;

        if (args.length > 0) {
            String channelName = args[0];
            // Use the removeChannel method and return its message
            return channelManager.removeChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: remove-channel <channelName>";
        }
    }

    @Override
    public String name() {
        return "remove-channel";
    }

    @Override
    public String description() {
        return "Removes an existing channel. Usage: remove-channel <channelName>";
    }
}
