package com.social100.todero.aia;

import com.social100.todero.annotation.AIAController;
import com.social100.todero.annotation.Action;

@AIAController(name = "",
        type = "",
        description = "AIA Protocol Plugin")
public class AIAProtocolPluginComponent {
    final static String MAIN_GROUP = "Main";

    public AIAProtocolPluginComponent() {
    }

    @Action(group = MAIN_GROUP, 
            command = "test",
            description = "Does the test")
    public String testCommand(String[] commandArgs) {
        return "Test Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }
}
