package com.social100.todero.aia;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.observer.PublisherManager;

@AIAController(name = "aia",
        type = "",
        description = "AIA Protocol Plugin")
public class AIAProtocolPluginComponent {
    final PublisherManager publisherManager;
    final static String MAIN_GROUP = "Main";

    public AIAProtocolPluginComponent(PublisherManager publisherManager) {
        this.publisherManager = publisherManager;
    }

    @Action(group = MAIN_GROUP,
            command = "test",
            description = "Does the test")
    public String testCommand(String[] commandArgs) {
        return "Test Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }
}
