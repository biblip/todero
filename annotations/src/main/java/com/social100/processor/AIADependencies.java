package com.social100.processor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AIADependencies {
    Class<?>[] components();
}