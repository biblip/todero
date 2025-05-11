package com.social100.todero;

import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.MessageContainerUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        ReceiveSshMessageCallback receiveSshMessageCallback = new ReceiveSshMessageCallback((line) -> {
            MessageContainer receivedMessageContainer = MessageContainerUtils.deserialize(line);
//            MessageContainer messageContainer = MessageContainer.builder()
//                    .responderId(receivedMessage.getResponderId())
//                    .addAllMessages(receivedMessageContainer.getMessages())
//                    .build();
            //commandManager.process(messageContainer);
        });

        // Tell MINA‑SSHD: “do NOT auto‑register built‑ins”
        System.setProperty("org.apache.sshd.security.registrars", "none");

        // Explicitly tell SSHD to use BouncyCastle's registrar
        SecurityUtils.registerSecurityProvider(new MyBouncyRegistrar());

        // Now set up your SSH server
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(2222);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        //sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
        sshd.setPasswordAuthenticator((username, password, session) -> true);
        //sshd.setShellFactory(new ProcessShellFactory("cmd.exe", "/c")); // or "cmd" on Windows

        ShellFactory shellFactory;
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.contains("win")) {
//            shellFactory = new ProcessShellFactory("cmd.exe", "/k", "echo Welcome to SSH");
//        } else {
//            // Linux/macOS
//            if (new File("/bin/sh").exists()) {
//                shellFactory = new ProcessShellFactory("/bin/sh", "-i");
//            } else {
//                shellFactory = new ProcessShellFactory("/bin/bash", "-i");
//            }
//        }
        shellFactory = new CustomShellFactory(receiveSshMessageCallback);

        sshd.setShellFactory(shellFactory);
        //sshd.setShellFactory(new ProcessShellFactory("cmd.exe", "/k", "echo Welcome to SSH"));

        try {
            sshd.start();
            System.out.println("SSH server started on port 2222");
        } catch (IOException e) {
            System.out.println("SSH server error:" + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Stopping SSH server ...");
                sshd.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        // 🟢 Keep main thread alive indefinitely
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }
}
