package com.social100.todero;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.console.base.CliCommandManager;
import com.social100.todero.server.RawServer;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SshServer implements RawServer {
    private static final Logger logger = LoggerFactory.getLogger(SshServer.class);
    private final CliCommandManager commandManager;
    private Integer port;

    public SshServer(AppConfig appConfig, ServerType type) {
        port = 9876; // appConfig.getApp().getServer().getPort();
        commandManager = new CliCommandManager(appConfig, type, (eventName, message) -> {
            // TODO: the goal of this is to return casual events to the intended target.
            /*
            ResponderRegistry.Responder responder = engine.getResponder(message.getResponderId());
            try {
                responder.sendMessage(MessageContainerUtils.serialize(message).getBytes(StandardCharsets.UTF_8), true);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

             */
        });
    }

    @Override
    public void start() throws IOException {
        ReceiveSshMessageCallback receiveSshMessageCallback = new ReceiveSshMessageCallback((line) -> {
            MessageContainer messageContainer = MessageContainer.builder()
                    .addChannelMessage(ChannelMessage.builder()
                            .channel(ChannelType.PUBLIC_DATA)
                            .payload(PublicDataPayload.builder()
                                    .message(line)
                                    .build())
                            .build())
                    .build();
            commandManager.process(messageContainer);
        });

        // Tell MINA‑SSHD: “do NOT auto‑register built‑ins”
        System.setProperty("org.apache.sshd.security.registrars", "none");

        // Explicitly tell SSHD to use BouncyCastle's registrar
        SecurityUtils.registerSecurityProvider(new MyBouncyRegistrar());

        // Now set up your SSH server
        org.apache.sshd.server.SshServer sshd = org.apache.sshd.server.SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setPasswordAuthenticator((username, password, session) -> true);

        ShellFactory shellFactory;
        shellFactory = new CustomShellFactory(receiveSshMessageCallback);

        sshd.setShellFactory(shellFactory);
        //sshd.setShellFactory(new ProcessShellFactory("cmd.exe", "/k", "echo Welcome to SSH"));

        sshd.start();

        logger.info("SSH server started on port {}", port);
    }
}
