package com.djmusic.vlc;

import com.social100.todero.common.model.plugin.PluginInterface;
import com.social100.todero.generated.AnnotationRegistry;
import com.social100.todero.generated.MethodRegistry;

import java.util.Arrays;

public class VlcPlugin implements PluginInterface {

    private static VlcPluginComponent vlcPluginComponent;

    public VlcPlugin() {
        vlcPluginComponent = new VlcPluginComponent();
    }

    @Override
    public Boolean hasCommand(String command) {
        return Arrays.asList(getAllCommandNames()).contains(command);
    }

    @Override
    public Object execute(String command, String[] commandArgs) {
        return MethodRegistry.executeInstance(command, vlcPluginComponent, commandArgs);
    }

    @Override
    public String name() {
        return "vlc";
    }

    @Override
    public String description() {
        return "description";
    }

    @Override
    public String[] getAllCommandNames() {
        return AnnotationRegistry.REGISTRY.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(map -> map.keySet().stream().filter("command"::equals).map(map::get)))
                .toArray(String[]::new);
    }

    @Override
    public String getHelpMessage() {
        StringBuilder sb = new StringBuilder();
        AnnotationRegistry.REGISTRY.forEach((key, value) -> {
            sb.append(key).append("\n");
            value.forEach(v -> sb.append("       - ").append(v.get("command")).append("\n           ").append(v.get("description")).append("\n"));
        });
        return sb.toString();
    }
}
