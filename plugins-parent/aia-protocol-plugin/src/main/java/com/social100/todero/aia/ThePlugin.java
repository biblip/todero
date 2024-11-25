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
        return null;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public String[] getAllCommandNames() {
        return null;
    }

    @Override
    public String getHelpMessage() {
        return null;
    }
}
