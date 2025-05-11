package com.social100.todero.console.base;

import com.social100.todero.common.Constants;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.channels.process.ChannelHandler;
import com.social100.todero.common.channels.process.ChannelProcessor;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.ControlPayload;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.HiddenDataPayload;
import com.social100.todero.common.message.channel.impl.NotificationPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class ConsoleCommandLineInterface implements CommandLineInterface {
    private final CommandProcessor commandProcessor;
    ChannelProcessor processor = ChannelProcessor.builder()
            .registerHandler(ChannelType.PUBLIC_DATA, new PublicDataHandler())
            .registerHandler(ChannelType.HIDDEN_DATA, new HiddenDataHandler())
            .registerHandler(ChannelType.NOTIFICATION, new NotificationHandler())
            .registerHandler(ChannelType.EVENT, new EventHandler())
            .registerHandler(ChannelType.CONTROL, new ControlHandler())
            .build();
    EventChannel.EventListener eventListener = new EventChannel.EventListener() {
        @Override
        public void onEvent(String eventName, MessageContainer message) {
            processor.processAllMessages(message.getMessages());
        }
    };

    public ConsoleCommandLineInterface(AppConfig appConfig, boolean aiaProtocol) {
        this.commandProcessor = CommandProcessorFactory.createProcessor(appConfig, eventListener, aiaProtocol);
        this.commandProcessor.open();
    }

    @Override
    public void run(String[] args) {
        boolean useScanner = args != null && Arrays.asList(args).contains("--useScanner");

        try {
            if (useScanner) {
                processInputWithScanner();
            } else {
                processInputWithTerminal();
            }
        } catch (IOException e) {
            System.out.println("Error during CLI execution: " + e.getMessage());
        } finally {
            try {
                commandProcessor.close();
            } catch (IOException ignore) {
            }
        }
    }

    private void processInputWithScanner() {
        try (Scanner scanner = new Scanner(System.in)) {
            String line;
            System.out.print("> ");
            while (!(line = scanner.nextLine()).equals(Constants.CLI_COMMAND_EXIT)) {
                if (!line.trim().isEmpty()) {
                    MessageContainer messageContainer = MessageContainer.builder()
                            .addChannelMessage(ChannelMessage.builder()
                                    .channel(ChannelType.PUBLIC_DATA)
                                    .payload(PublicDataPayload.builder()
                                            .message(line)
                                            .build())
                                    .build())
                            .build();
                    commandProcessor.process(messageContainer);
                }
                System.out.print("\n> ");
            }
        }
    }

    private void processInputWithTerminal() throws IOException {
        String[] autocompleteStrings = null;
        if (commandProcessor.getCommandManager() != null) {
            autocompleteStrings = commandProcessor.getCommandManager().generateAutocompleteStrings();
        }
        TerminalInputHandler inputHandler = new TerminalInputHandler(autocompleteStrings);
        try {
            inputHandler.processInput( line -> {
                MessageContainer messageContainer = MessageContainer.builder()
                        .addChannelMessage(ChannelMessage.builder()
                                .channel(ChannelType.PUBLIC_DATA)
                                .payload(PublicDataPayload.builder()
                                        .message(line)
                                        .build())
                                .build())
                        .build();
                commandProcessor.process(messageContainer);
            });
        } finally {
            inputHandler.close();
        }
    }

    private static class PublicDataHandler implements ChannelHandler<PublicDataPayload> {
        @Override
        public void process(PublicDataPayload payload) {
            System.out.println(payload.getMessage());
        }

        @Override
        public Class<PublicDataPayload> getPayloadType() {
            return PublicDataPayload.class;
        }
    }

    private static class HiddenDataHandler implements ChannelHandler<HiddenDataPayload> {
        @Override
        public void process(HiddenDataPayload payload) {
            System.out.println("Processing Hidden Data: " + payload.getMessage());
        }

        @Override
        public Class<HiddenDataPayload> getPayloadType() {
            return HiddenDataPayload.class;
        }
    }

    private static class NotificationHandler implements ChannelHandler<NotificationPayload> {
        @Override
        public void process(NotificationPayload payload) {
            System.out.println("Processing Notification: " + payload.getMessage());
        }

        @Override
        public Class<NotificationPayload> getPayloadType() {
            return NotificationPayload.class;
        }
    }

    private static class EventHandler implements ChannelHandler<EventPayload> {
        @Override
        public void process(EventPayload payload) {
            System.out.println("Processing Event: " + payload.getName() + " : " + payload.getMessage());
        }

        @Override
        public Class<EventPayload> getPayloadType() {
            return EventPayload.class;
        }
    }

    private static class ControlHandler implements ChannelHandler<ControlPayload> {
        @Override
        public void process(ControlPayload payload) {
            System.out.println("Processing Control: " + payload.getMessage());
        }

        @Override
        public Class<ControlPayload> getPayloadType() {
            return ControlPayload.class;
        }
    }
}