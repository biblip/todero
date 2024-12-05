package com.social100.todero.cli.base;

import com.social100.todero.common.config.AppConfig;

public abstract class CommandLineInterface {
    protected final AppConfig appConfig;

    protected CommandLineInterface(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    void run(String[] args) {
        throw new RuntimeException("Not implemented");
    }
}
