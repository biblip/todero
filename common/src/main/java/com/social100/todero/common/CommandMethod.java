package com.social100.todero.common;

public interface CommandMethod {
    String execute(Object object, String[] args);
    String name();
    default String description() { return "No description available."; }
    default boolean isCli() {
        return false;
    }
}
