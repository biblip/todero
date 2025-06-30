package com.social100.todero.plugin.ssh;

import com.social100.processor.Action;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.plugin.ssh.controller.SshController;
import com.social100.todero.plugin.ssh.service.SshService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/*@AIAController(name = "ssh",
    type = "",
    description = "description",
    events = SshService.SshEvents.class)
 */
public class SshPluginComponent {
  private final SshController controller = null;

  public SshPluginComponent() {
    //this.controller = new SshController();
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "open",
      description = "Opens a ssh connection required user and a pem file     Usage: open <id> <host> <user> <pemPath>")
  public Boolean open(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 4) {
      ctx.respond("Usage: open <id> <host> <user> <pemPath>");
      return false;
    }
    controller.openConnectionWithKey(args[0], args[1], args[2], args[3]);
    ctx.respond("Connection opened.");
    return true;
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "open-pass",
      description = "Opens a ssh connection, requires user and password     Usage: open-pass <id> <host> <user> <passwordFilePath>")
  public Boolean openWithPassword(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 4) {
      ctx.respond("Usage: open-pass <id> <host> <user> <passwordFilePath>");
      return false;
    }
    try {
      String password = Files.readString(Path.of(args[3])).strip();
      controller.openConnectionWithPassword(args[0], args[1], args[2], password);
      ctx.respond("Connection opened with password.");
      return true;
    } catch (IOException e) {
      ctx.respond("Failed to read password file: " + e.getMessage());
      return false;
    }
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "run",
      description = "Execute commands on a ssh connection     Usage: run <id> <command>")
  public Boolean run(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 2) {
      ctx.respond("Usage: run <id> <command>");
      return false;
    }
    String command = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    controller.executeCommand(args[0], command, Duration.ofSeconds(60));
    ctx.respond("Command executed.");
    return true;
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "upload",
      description = "Upload a file or script to the ssh server     Usage: upload <id> <localPath> <remotePath>")
  public Boolean upload(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 3) {
      ctx.respond("Usage: upload <id> <localPath> <remotePath>");
      return false;
    }
    controller.uploadScript(args[0], args[1], args[2]);
    ctx.respond("Upload complete.");
    return true;
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "delete",
      description = "Delete a remote file     Usage: delete <id> <remotePath>")
  public Boolean delete(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 2) {
      ctx.respond("Usage: delete <id> <remotePath>");
      return false;
    }
    controller.deleteRemoteFile(args[0], args[1]);
    ctx.respond("Remote file deleted.");
    return true;
  }

  @Action(group = SshService.MAIN_GROUP,
      command = "close",
      description = "Close a ssh connection     Usage: close <id>")
  public Boolean close(CommandContext ctx) {
    String[] args = ctx.getArgs();
    if (args.length < 1) {
      ctx.respond("Usage: close <id>");
      return false;
    }
    controller.closeConnection(args[0]);
    ctx.respond("Connection closed.");
    return true;
  }
}