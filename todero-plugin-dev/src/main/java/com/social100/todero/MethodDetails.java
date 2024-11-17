package com.social100.todero;

public class MethodDetails {
    public final boolean isStatic;
    public final String className;
    public final String methodName;

    public MethodDetails(boolean isStatic, String className, String methodName) {
        this.isStatic = isStatic;
        this.className = className;
        this.methodName = methodName;
    }
}