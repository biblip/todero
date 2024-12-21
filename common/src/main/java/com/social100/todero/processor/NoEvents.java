package com.social100.todero.processor;

public enum NoEvents implements EventDefinition {
    INSTANCE("instance");

    private final String description;

    NoEvents(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
