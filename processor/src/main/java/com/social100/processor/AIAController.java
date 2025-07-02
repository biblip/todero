package com.social100.processor;

import com.social100.todero.common.config.ServerType;
import com.social100.todero.processor.EventDefinition;
import com.social100.todero.processor.NoEvents;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AIAController {
    String name();
    ServerType type();
    String description();
    Class<? extends EventDefinition> events() default NoEvents.class;
}
