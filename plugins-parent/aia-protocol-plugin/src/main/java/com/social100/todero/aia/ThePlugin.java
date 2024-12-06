package com.social100.todero.aia;

import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.PluginInterface;
import com.social100.todero.console.base.ApiCommandLineInterface;

public class ThePlugin implements PluginInterface {
    ApiCommandLineInterface apiCommandLineInterface;

    public ThePlugin() {
        this.apiCommandLineInterface = new ApiCommandLineInterface(null, true);
        this.apiCommandLineInterface.setOutputDataHandler(this::outputDataHandler);
    }

    public void outputDataHandler(byte[] data) {
        //String line = new String(data);
        System.out.print(new String(data));
    }

    @Override
    public Component getComponent() {
        /*Component.builder()
                .name(componentName)
                .description(componentDescription)
                .commands()
         */
        this.apiCommandLineInterface.writeAsync("help\n".getBytes());
        // Build the component with the available/supported commands according to the help returned and local policies.
        return null;
    }

    @Override
    public Object execute(String command, String[] commandArgs) {
        // re-arm the line to be sent
        this.apiCommandLineInterface.writeAsync(command.getBytes());
        return null;
    }
}
