package com.djmusic.vlc;

import com.djmusic.vlc.service.VlcService;
import com.social100.processor.Action;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.scheduler.TaskScheduler;

/*@AIAController(name = "vlc",
        type = "",
        description = "description",
        events = VlcService.VlcPluginEvents.class)*/
//@AIADependencies(components = {DjyPluginComponent.class, SimplePluginComponent.class})
public class VlcPluginComponent {
    private final VlcService vlcService;
    private final TaskScheduler scheduler = new TaskScheduler();
    CommandContext globalContext = null;

    public VlcPluginComponent() {
        this.vlcService = new VlcService();

        // Create an instance of the TaskScheduler


        scheduler.scheduleTask(() -> {
            if (globalContext != null) {
                globalContext.event(VlcService.VlcPluginEvents.VOLUME_CHANGE, "The Volume has changed");
            }
        }, 10000);

        // Add a shutdown hook to gracefully stop the scheduler
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stop));
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "events",
            description = "Start / Stop Sending events. Usage: events ON|OFF")
    public Boolean eventsCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond(context.getInstance().getAvailableEvents().toString());
        } else {
            boolean eventsOn = "on".equalsIgnoreCase(args[0]);
            if (eventsOn) {
                this.globalContext = context;
                context.respond("events are now ON");
            } else {
                context.respond("events are now OFF");
                this.globalContext = null;
            }
        }
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "move",
            description = "Moves the playback to the specified time. Usage: move <HH:MM:SS|MM:SS|SS>")
    public Boolean moveCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please specify the time to move to. Usage: move <time>");
            return true;
        }
        String moveTo = args[0];
        context.respond(this.vlcService.moveCommand(moveTo));
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "mute",
            description = "Toggles the mute state of the playback if valid media is loaded.")
    public Boolean muteCommand(CommandContext context) {
        context.respond(this.vlcService.muteCommand());
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "pause",
            description = "Pauses the playback if it is currently playing.")
    public Boolean pauseCommand(CommandContext context) {
        context.respond(this.vlcService.pauseCommand());
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "play",
            description = "Plays the specified media file. If no file is specified, resumes the current one.")
    public Boolean playCommand(CommandContext context) {
        String[] args = context.getArgs();
        String mediaPathToPlay = "";
        if (args.length > 0) {
            mediaPathToPlay = args[0];
        }
        context.respond(this.vlcService.playCommand(mediaPathToPlay));
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "skip",
            description = "Skips the playback forward or backward by the specified number of seconds. Usage: skip <+/-seconds>")
    public Boolean skipCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please specify the number of seconds to skip. Usage: skip <+/-seconds>");
            return true;
        }
        long skipTime = Long.parseLong(args[0]);
        context.respond(this.vlcService.skipCommand(skipTime));
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "status",
            description = "Displays the current status of VLC. Use 'status all' for all media info available.")
    public Boolean statusCommand(CommandContext context) {
        String[] args = context.getArgs();
        boolean all = (args.length > 0 && "all".equalsIgnoreCase(args[0]));
        context.respond(this.vlcService.statusCommand(all));
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "stop",
            description = "Stops the playback if it is currently active.")
    public Boolean stopCommand(CommandContext context) {
        context.respond(this.vlcService.stopCommand());
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "volume",
            description = "Sets the volume to a specified level between 0 and 150. Usage: volume <level>")
    public Boolean volumeCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("No volume level provided. Please specify a volume level between 0 and 150.");
            return true;
        }
        int volume = Integer.parseInt(args[0]);
        context.respond(this.vlcService.volumeCommand(volume));
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "volume-down",
            description = "Decreases the volume by 5 units.")
    public Boolean volumeDownCommand(CommandContext context) {
        context.respond(this.vlcService.volumeDownCommand());
        return true;
    }

    @Action(group = VlcService.MAIN_GROUP,
            command = "volume-up",
            description = "Increases the volume by 5 units.")
    public Boolean volumeUpCommand(CommandContext context) {
        context.respond(this.vlcService.volumeUpCommand());
        return true;
    }

    @Action(group = VlcService.CHANNELS_GROUP,
            command = "add-channel",
            description = "Adds a new channel. Usage: add-channel <channelName>")
    public Boolean addChannelCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please provide a channel name. Usage: add-channel <channelName>");
            return true;
        }
        String channelName = args[0];
        context.respond(this.vlcService.addChannelCommand(channelName));
        return true;
    }

    @Action(group = VlcService.CHANNELS_GROUP,
            command = "list-channels",
            description = "Lists all available channels.")
    public Boolean listChannelCommand(CommandContext context) {
        context.respond(this.vlcService.listChannelCommand());
        return true;
    }

    @Action(group = VlcService.CHANNELS_GROUP,
            command = "remove-channel",
            description = "Removes an existing channel. Usage: remove-channel <channelName>")
    public Boolean removeChannelCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please provide a channel name. Usage: remove-channel <channelName>");
            return true;
        }
        String channelName = args[0];
        context.respond(this.vlcService.removeChannelCommand(channelName));
        return true;
    }

    @Action(group = VlcService.CHANNELS_GROUP,
            command = "select-channel",
            description = "Selects an existing channel. Usage: select-channel <channelName>")
    public Boolean selectChannelCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond("Error: Please provide a channel name. Usage: select-channel <channelName>");
            return true;
        }
        String channelName = args[0];
        context.respond(this.vlcService.selectChannelCommand(channelName));
        return true;
    }
}
