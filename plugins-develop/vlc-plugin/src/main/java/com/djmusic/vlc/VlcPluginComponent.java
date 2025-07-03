package com.djmusic.vlc;

import com.djmusic.vlc.service.VlcService;
import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.scheduler.TaskScheduler;
import io.github.cdimascio.dotenv.Dotenv;

@AIAController(name = "vlc",
    type = ServerType.AIA,
    visible = true,
    description = "description",
    events = VlcService.VlcPluginEvents.class)
//@AIADependencies(components = {DjyPluginComponent.class, SimplePluginComponent.class})
public class VlcPluginComponent {
    private final String vlcMediaDirectory;
    private final VlcService vlcService;
    private final TaskScheduler scheduler = new TaskScheduler();
    CommandContext globalContext = null;

    public VlcPluginComponent() {
        Dotenv dotenv = Dotenv.configure().filename(".env-vlc").load();
        vlcMediaDirectory = dotenv.get("MEDIA");
        this.vlcService = new VlcService(vlcMediaDirectory);

        // Create an instance of the TaskScheduler


        scheduler.scheduleTask(() -> {
            if (globalContext != null) {
                globalContext.event(VlcService.VlcPluginEvents.VOLUME_CHANGE.name(), "The Volume has changed");
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
            description = "Plays the specified media file. If no file is specified, resumes the current one. Usage: play [media]")
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

    @Action(group = VlcService.PLAYLIST_GROUP,
            command = "playlist-add",
            description = "Adds the specified media file to the playlist. Usage: add-playlist [media]")
    public Boolean playlistAdd(CommandContext context) {
        String[] args = context.getArgs();
        String mediaPathToAdd = "";
        if (args.length > 0) {
            mediaPathToAdd = args[0];
        }
        context.respond(this.vlcService.playlistAdd(mediaPathToAdd));
        return true;
    }

    @Action(group = VlcService.PLAYLIST_GROUP,
            command = "playlist-remove",
            description = "Remove current paying media from the playlist Usage: playlist-remove, if there is no current media playing then does nothing")
    public Boolean playlistRemove(CommandContext context) {
        context.respond(this.vlcService.playlistRemove());
        return true;
    }

    @Action(group = VlcService.PLAYLIST_GROUP,
            command = "playlist-next",
            description = "Play the next media in the playlist. Usage: playlist-next")
    public Boolean playlistNext(CommandContext context) {
        context.respond(this.vlcService.playlistNext());
        return true;
    }

    @Action(group = VlcService.PLAYLIST_GROUP,
            command = "playlist-list",
            description = "Inform the user of the playlist items and which is the current item. Usage: playlist-list")
    public Boolean playlistList(CommandContext context) {
        context.respond(this.vlcService.playlistList());
        return true;
    }
}
