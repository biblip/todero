package com.social100.todero.aia;

import com.social100.processor.Action;

/*
@AIAController(name = "aia",
        type = "",
        description = "AIA Protocol Plugin")
@Events({
        @EventDefinition( name = "door_open", description = "The door_open description" ),
        @EventDefinition( name = "window_broken", description = "The window_broken description" ),
        @EventDefinition( name = "high_temp", description = "The high_temp description" )})
 */
// While creating a EventDefinition, ensure name is one word with acceptable characters.
// should it be uppercase only (additional check)?
public class AIAProtocolPluginComponent { // extends AIAProtocolPluginComponentTools {
    final static String MAIN_GROUP = "Main";

    public AIAProtocolPluginComponent() {
        /*
        This registration process is not required here, it can be done by the PluginContext when creating the instance.
        this.registerEvent("door_open", description = "The door_open description");
        this.registerEvent("window_broken", description = "The window_broken description");
        this.registerEvent("high_temp", description = "The high_temp description");
         */
    }

    @Action(group = MAIN_GROUP,
            command = "test",
            description = "Does the test")
    public String testCommand(String[] commandArgs) {
        //this.triggerEvent("door_open", "the Message");
        return "Test Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }

    @Action(group = "Reserved",
            command = "subscribe",
            description = "Subscribe to an event in this component")
    public String subscribeToEvent(String[] commandArgs) {
        return "Done";
    }

    @Action(group = "Reserved",
            command = "unsubscribe",
            description = "Unubscribe from an event in this component")
    public String unsubscribeFromEvent(String[] commandArgs) {
        return "Done";
    }

    /*
    This action can be automatically added while generating the Pluginclass.
    not need to create an actual additional method, just use the subscribeToEvent we already
    have in this DynamicEventChannel class ( extended ).

    additinal check while creating actions:   no command "subscribe" could be added because it is a reserved word.

    @Action(group = MAIN_GROUP,
    command = "subscribe",
    description = "Subscribe to a ")
    public String subscribeToComponentEvent(String[] commandArgs) {
        this.subscribeToEvent("door_open", listener);
        return "";
    }

     */

}
