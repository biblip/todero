package com.social100.todero.plugin;

import com.social100.processor.AIAController;
import com.social100.processor.Action;

import java.util.Map;

@AIAController(name = "simple",
        type = "",
        description = "Simple Plugin")
public class SimplePluginComponent {
    final static String MAIN_GROUP = "Main";

    public SimplePluginComponent() {
    }

    @Action(group = MAIN_GROUP, 
            command = "ping",
            description = "Does the ping")
    public String pingCommand(String[] commandArgs) {
        return "Ping Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }

    @Action(group = MAIN_GROUP,
            command = "hello",
            description = "Does a friendly hello")
    public Map<String, Object> instanceMethod(String[] args) {
        return Map.of(
                "message", "Hello from instanceMethod",
                "args", args,
                "metadata", Map.of("key1", "value1", "key2", "value2")
        );
    }
}
