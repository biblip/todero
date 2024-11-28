package com.social100.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes({"com.social100.processor.AIAController", "com.social100.processor.Action"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InterfaceProcessor extends AbstractProcessor {
    boolean processCompleted = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (isSelfProcessing()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Skipping processing for todero-plugin-dev module.");
            return false;
        }

        if (processCompleted) {
            return false;
        }

        Map<String, List<Map<String, String>>> classToMethodsMap = new HashMap<>();

        for (Element classElement : roundEnv.getElementsAnnotatedWith(AIAController.class)) {
            if (classElement.getKind() == ElementKind.CLASS) {
                TypeElement classTypeElement = (TypeElement) classElement;
                AIAController classAnnotation = classElement.getAnnotation(AIAController.class);

                String pluginClassQualifiedName = classTypeElement.getQualifiedName().toString();
                String pluginName = classAnnotation.name();
                String pluginDescription = classAnnotation.description();

                validateUniqueCommands(classTypeElement);

                List<Map<String, String>> methodDetailsList = new ArrayList<>();

                for (Element enclosedElement : classTypeElement.getEnclosedElements()) {
                    if (enclosedElement.getKind() == ElementKind.METHOD &&
                            enclosedElement.getAnnotation(Action.class) != null) {
                        Action methodAnnotation = enclosedElement.getAnnotation(Action.class);

                        // Determine if the method is static
                        boolean isStatic = enclosedElement.getModifiers().contains(Modifier.STATIC);

                        // Extract attributes from MethodAnnotation
                        Map<String, String> methodDetails = new HashMap<>();
                        methodDetails.put("method", enclosedElement.getSimpleName().toString());
                        methodDetails.put("static", isStatic?"TRUE":"FALSE");
                        methodDetails.put("group", methodAnnotation.group());
                        methodDetails.put("command", methodAnnotation.command());
                        methodDetails.put("description", methodAnnotation.description());

                        methodDetailsList.add(methodDetails);
                    }
                }

                if (!methodDetailsList.isEmpty()) {
                    classToMethodsMap.put(classTypeElement.getQualifiedName().toString(), methodDetailsList);
                    // Generate a summary file
                    generateRegistryClass("com.social100.todero.generated", pluginClassQualifiedName, pluginName, methodDetailsList);
                }
            }
        }


        Map<String, Map<String, MethodDetails>> nameToMethodDetails = new HashMap<>();

        for (Element classElement : roundEnv.getElementsAnnotatedWith(AIAController.class)) {
            if (classElement.getKind() == ElementKind.CLASS) {
                TypeElement classTypeElement = (TypeElement) classElement;
                AIAController classAnnotation = classElement.getAnnotation(AIAController.class);

                String pluginClassQualifiedName = classTypeElement.getQualifiedName().toString();
                String pluginName = classAnnotation.name();
                String pluginDescription = classAnnotation.description();

                Map<String, MethodDetails> methodDetails = new HashMap<>();

                for (Element enclosedElement : classTypeElement.getEnclosedElements()) {
                    if (enclosedElement.getKind() == ElementKind.METHOD &&
                            enclosedElement.getAnnotation(Action.class) != null) {
                        Action methodAnnotation = enclosedElement.getAnnotation(Action.class);

                        // Determine if the method is static
                        boolean isStatic = enclosedElement.getModifiers().contains(Modifier.STATIC);

                        // Add to the map
                        methodDetails.put(
                                methodAnnotation.command(),
                                new MethodDetails(isStatic, classTypeElement.getQualifiedName().toString(), enclosedElement.getSimpleName().toString())
                        );
                    }
                }

                nameToMethodDetails.put(classTypeElement.getQualifiedName().toString(), methodDetails);
                // Generate the registry class

                generateMethodRegistry("com.social100.todero.generated", pluginClassQualifiedName, pluginName, methodDetails);
            }
        }

        for (Element classElement : roundEnv.getElementsAnnotatedWith(AIAController.class)) {
            if (classElement.getKind() == ElementKind.CLASS) {
                TypeElement classTypeElement = (TypeElement) classElement;
                AIAController classAnnotation = classElement.getAnnotation(AIAController.class);

                String pluginClassQualifiedName = classTypeElement.getQualifiedName().toString();
                String pluginName = classAnnotation.name();
                String pluginDescription = classAnnotation.description();

                generatePluginInterfaceImplementation("com.social100.todero.generated", pluginClassQualifiedName, pluginName, pluginDescription);
            }
        }


        processCompleted = true;

        return true;
    }
/*
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("com.social100.processor.AIAController", "com.social100.processor.Action");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("isSelfProcessing");
    }*/

    private void generateRegistryClass(String packageName, String pluginClassQualifiedName, String pluginName, List<Map<String, String>> classToMethodsMap) {

        String classSimpleName = getSimpleName(pluginClassQualifiedName);
        String classVariableName = classToClassVariableName(classSimpleName);
        String generatedClassName = classSimpleName + pluginName + "AR";

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "\n" +
                "public class " + generatedClassName + " {\n");
        classContent.append("    public static final List<Map<String, String>> REGISTRY = List.of(\n");
            for (Map<String, String> methodDetails : classToMethodsMap) {
                classContent.append("            Map.of(");
                methodDetails.forEach((key, value) ->
                        classContent.append("\"").append(key).append("\", \"").append(value).append("\", ")
                );
                // Remove trailing ", " and close Map
                classContent.setLength(classContent.length() - 2);
                classContent.append("),\n");
            }
            // Remove trailing ", " and close List
            classContent.setLength(classContent.length() - 2);
            classContent.append("\n    );\n");
        //}
        classContent.append("}");

        try {
            // Write the generated file
            Filer filer = processingEnv.getFiler();
            JavaFileObject fileObject = filer.createSourceFile(packageName + "." + generatedClassName);

            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating registry class: " + e.getMessage());
        }
    }

    private void generateMethodRegistry(String packageName, String pluginClassQualifiedName, String pluginName, Map<String, MethodDetails> nameToMethodDetails) {

        String classSimpleName = getSimpleName(pluginClassQualifiedName);
        String classVariableName = classToClassVariableName(classSimpleName);
        String generatedClassName = classSimpleName + pluginName + "MR";

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n" +
                "import java.util.function.BiFunction;\n" +
                "import java.util.function.Function;\n" +
                "\n" +
                "public class " + generatedClassName + " {\n\n" +
                "    public static final Map<String, Map<String, Function<String[], Object>>> STATIC_REGISTRY = new HashMap<>();\n" +
                "    public static final Map<String, Map<String, BiFunction<Object, String[], Object>>> INSTANCE_REGISTRY = new HashMap<>();\n" +
                "\n" +
                "    static {\n");
        //for (Map.Entry<String, Map<String, MethodDetails>> nameToMethodDetail : nameToMethodDetails.entrySet()) {
            String instanceRegistryName = "instance" + getSimpleName(pluginClassQualifiedName) + "Registry";
            String staticRegistryName = "static" + getSimpleName(pluginClassQualifiedName) + "Registry";
            classContent.append("        Map<String, BiFunction<Object, String[], Object>> " +  instanceRegistryName + " = new HashMap<>();\n");
            classContent.append("        Map<String, Function<String[], Object>> " + staticRegistryName + " = new HashMap<>();\n");
            // Populate the STATIC_REGISTRY for static methods
            for (Map.Entry<String, MethodDetails> entry : nameToMethodDetails.entrySet()) {
                String methodName = entry.getKey();
                MethodDetails details = entry.getValue();

                if (details.isStatic) {
                    // Populate the STATIC_REGISTRY for static methods
                    classContent.append("        ")
                            .append(staticRegistryName)
                            .append(".put(\"")
                            .append(methodName)
                            .append("\", ")
                            .append("args -> ")
                            .append(details.className)
                            .append(".")
                            .append(details.methodName)
                            .append("(args));\n");
                } else {
                    // Populate the INSTANCE_REGISTRY for instance methods
                    classContent.append("        ")
                            .append(instanceRegistryName)
                            .append(".put(\"")
                            .append(methodName)
                            .append("\", ")
                            .append("(instance, args) -> ((")
                            .append(details.className)
                            .append(") instance).")
                            .append(details.methodName)
                            .append("(args));\n");
                }
            }
            classContent.append("        STATIC_REGISTRY.put(\"" + pluginClassQualifiedName + "\", " + staticRegistryName + ");\n");
            classContent.append("        INSTANCE_REGISTRY.put(\"" + pluginClassQualifiedName + "\", " + instanceRegistryName + ");\n\n");
        //}

        classContent.append("    }\n\n" +
                "    public static Object executeStatic(String plugin, String command, String[] args) {\n" +
                "        Function<String[], Object> function = STATIC_REGISTRY.get(plugin).get(command);\n" +
                "        if (function != null) {\n" +
                "            return function.apply(args);\n" +
                "        } else {\n" +
                "            throw new IllegalArgumentException(\"No static method found for command: \" + command);\n" +
                "        }\n" +
                "    }\n\n" +
                "    public static Object executeInstance(String plugin, String command, Object instance, String[] args) {\n" +
                "        BiFunction<Object, String[], Object> function = INSTANCE_REGISTRY.get(plugin).get(command);\n" +
                "        if (function != null) {\n" +
                "            return function.apply(instance, args);\n" +
                "        } else {\n" +
                "            throw new IllegalArgumentException(\"No instance method found for command: \" + command);\n" +
                "        }\n" +
                "    }\n\n" +
                "    public static Object execute(String plugin, String command, Object instance, String[] args) {\n" +
                "        BiFunction<Object, String[], Object> instanceFunction = INSTANCE_REGISTRY.get(plugin).get(command);\n" +
                "        if (instanceFunction != null) {\n" +
                "            return instanceFunction.apply(instance, args);\n" +
                "        }\n" +
                "        Function<String[], Object> staticFunction = STATIC_REGISTRY.get(plugin).get(command);\n" +
                "        if (staticFunction != null) {\n" +
                "            return staticFunction.apply(args);\n" +
                "        }\n" +
                "        throw new IllegalArgumentException(\"No method found for command: \" + command);\n" +
                "    }" +
                "}\n");
        try {
            // Write the generated file
            Filer filer = processingEnv.getFiler();
            JavaFileObject fileObject = filer.createSourceFile(packageName + "." + generatedClassName);

            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating MethodRegistry: " + e.getMessage());
        }
    }

    private void generatePluginInterfaceImplementation(String packageName, String pluginClassQualifiedName, String pluginName, String pluginDescription) {

        String classSimpleName = getSimpleName(pluginClassQualifiedName);
        String classVariableName = classToClassVariableName(classSimpleName);
        String generatedClassName = classSimpleName + "Impl";

        String generatedAnnotationRegistryClassName = classSimpleName + pluginName + "AR";
        String generatedMethodRegistryClassName = classSimpleName + pluginName + "MR";

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "\n" +
                "import com.social100.todero.common.model.plugin.Command;\n" +
                "import com.social100.todero.common.model.plugin.Component;\n" +
                "import com.social100.todero.common.model.plugin.PluginInterface;\n" +
                "import com.social100.todero.generated." + generatedAnnotationRegistryClassName + ";\n" +
                "import com.social100.todero.generated." + generatedMethodRegistryClassName + ";\n" +
                "\n" +
                "import java.util.stream.Collectors;\n" +
                "\n" +
                "public class " + generatedClassName + " implements PluginInterface {\n" +
                "\n" +
                "    private static " + pluginClassQualifiedName + " " + classVariableName + ";\n" +
                "    private final Component component;\n" +
                "\n" +
                "    public " + generatedClassName + "() {\n" +
                "        " + classVariableName + " = new " + pluginClassQualifiedName + "();\n" +
                "        component = Component\n" +
                "                .builder()\n" +
                "                .name(\"" + pluginName + "\")\n" +
                "                .description(\"" + pluginDescription + "\")\n" +
                "                .commands(" + generatedAnnotationRegistryClassName + ".REGISTRY.stream()\n" +
                "                        .map(entry -> new Command(\n" +
                "                                entry.get(\"static\"),\n" +
                "                                entry.get(\"method\"),\n" +
                "                                entry.get(\"description\"),\n" +
                "                                entry.get(\"command\"),\n" +
                "                                entry.get(\"group\")\n" +
                "                        ))\n" +
                "                        .collect(Collectors.toMap(\n" +
                "                                Command::getCommand, // Key: Command name\n" +
                "                                commandInfo -> commandInfo // Value: CommandInfo object\n" +
                "                        )))\n" +
                "                .build();" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Component getComponent() {\n" +
                "        return component;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Object execute(String command, String[] commandArgs) {\n" +
                "        return " + generatedMethodRegistryClassName + ".execute(\"" + pluginClassQualifiedName + "\", command, " + classVariableName + ", commandArgs);\n" +
                "    }\n" +
                "}\n");
        try {
            // Write the generated file
            Filer filer = processingEnv.getFiler();
            JavaFileObject fileObject = filer.createSourceFile(packageName + "." + generatedClassName);

            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating MethodRegistry: " + e.getMessage());
        }
    }

    private String classToClassVariableName(String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        // Convert the first character to lowercase and append the rest of the string
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    public static String getSimpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new IllegalArgumentException("Qualified name cannot be null or empty");
        }

        // Find the last '.' in the qualified name
        int lastDotIndex = qualifiedName.lastIndexOf('.');

        // If no '.' is found, the input is already a simple name
        if (lastDotIndex == -1) {
            return qualifiedName;
        }

        // Extract the substring after the last '.'
        return qualifiedName.substring(lastDotIndex + 1);
    }

    private boolean isSelfProcessing() {
        String isSelfProcessing = processingEnv.getOptions().get("isSelfProcessing");
        return Boolean.parseBoolean(isSelfProcessing);
    }

    private void validateUniqueCommands(TypeElement classElement) {
        List<String> seenCommands = new ArrayList<>();

        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                Action annotation = enclosed.getAnnotation(Action.class);
                if (annotation != null) {
                    String command = annotation.command();

                    // Check for duplicates
                    if (seenCommands.contains(command)) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "Duplicate @Action 'command' value: \"" + command + "\" found : " + classElement.getQualifiedName() + " " + enclosed.getSimpleName(),
                                enclosed
                        );
                    } else {
                        seenCommands.add(command);
                    }
                }
            }
        }
    }

    public static class MethodDetails {
        public final boolean isStatic;
        public final String className;
        public final String methodName;

        public MethodDetails(boolean isStatic, String className, String methodName) {
            this.isStatic = isStatic;
            this.className = className;
            this.methodName = methodName;
        }
    }
}
