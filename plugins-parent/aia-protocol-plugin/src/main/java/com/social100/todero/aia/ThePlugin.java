package com.social100.todero.aia;

import com.social100.todero.common.model.plugin.PluginInterface;

public class ThePlugin implements PluginInterface {

    public ThePlugin() {
    }

    @Override
    public Boolean hasCommand(String command) {
        return null;
    }

    @Override
    public Object execute(String command, String[] commandArgs) {
        return null;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public String[] getAllCommandNames() {
        return new String[0];
    }

    @Override
    public String getHelpMessage() {
        return "";
    }
}
