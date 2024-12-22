package com.social100.processor;

import com.social100.todero.processor.EventDefinition;
import com.social100.todero.processor.NoEvents;

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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
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

                //String constructorParameters = generateNewObjectString(getClassTypeMirrorsForAIADependencies(classTypeElement));
                AIADependencies annotation = classElement.getAnnotation(AIADependencies.class);
                boolean commandManagerRequired = annotation != null;

                generatePluginInterfaceImplementation("com.social100.todero.generated", pluginClassQualifiedName, commandManagerRequired, pluginName, pluginDescription);
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(AIAController.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) element;
                AIAController annotation = classElement.getAnnotation(AIAController.class);

                String className = classElement.getSimpleName() + "Tools";
                String packageName = processingEnv.getElementUtils().getPackageOf(classElement).toString();

                String eventEnumType = getEventEnumType(annotation);

                try {
                    JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + className);
                    try (Writer writer = file.openWriter()) {
                        // Package and imports
                        writer.write("package " + packageName + ";\n\n");
                        writer.write("import com.social100.todero.common.channels.DynamicEventChannel;\n");
                        writer.write("import com.social100.todero.common.channels.ComponentEventListenerSupport;\n");
                        writer.write("import com.social100.todero.common.channels.EventChannel;\n");
                        writer.write("import com.social100.todero.common.channels.ReservedEventRegistry;\n");
                        writer.write("import com.social100.todero.processor.EventDefinition;\n");

                        if (eventEnumType != null) {
                            writer.write("import " + eventEnumType + ";\n\n"); // Import the enum class
                        }

                        // Class declaration
                        writer.write("public class " + className + " extends DynamicEventChannel implements ComponentEventListenerSupport {\n\n");

                        // Constructor
                        writer.write("    public " + className + "() {\n");
                        if (eventEnumType != null) {
                            writer.write("        for (EventDefinition event : " + eventEnumType + ".values()) {\n");
                            writer.write("            this.registerEvent(event.name(), event.getDescription());\n");
                            writer.write("        }\n");
                        }
                        writer.write("    }\n\n");

                        // Add component event listener method
                        writer.write("    public void addComponentEventListener(EventChannel.EventListener listener) {\n");
                        if (eventEnumType != null) {
                            writer.write("        for (EventDefinition event : " + eventEnumType + ".values()) {\n");
                            writer.write("            this.subscribeToEvent(event.name(), listener);\n");
                            writer.write("        }\n");
                        }
                        writer.write("        // Subscribe to reserved events directly\n");
                        writer.write("        for (EventChannel.ReservedEvent reservedEvent : EventChannel.ReservedEvent.values()) {\n");
                        writer.write("            ReservedEventRegistry.subscribe(reservedEvent, listener);\n");
                        writer.write("        }\n");
                        writer.write("    }\n\n");

                        // Close class
                        writer.write("}\n");
                    }
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, e.getMessage());
                }
            }
        }

        processCompleted = true;

        return true;
    }

    private static String getEventEnumType(AIAController annotation) {
        String eventEnumType = null;
        try {
            // Attempt to access the enum type directly
            Class<? extends EventDefinition> eventType = annotation.events();
            if (!eventType.equals(NoEvents.class)) {
                eventEnumType = eventType.getCanonicalName();
            }
        } catch (MirroredTypeException e) {
            // Handle annotation processing context
            TypeMirror typeMirror = e.getTypeMirror();
            if (!"com.social100.todero.processor.NoEvents".equals(typeMirror.toString())) {
                eventEnumType = typeMirror.toString();
            }
        }
        return eventEnumType;
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
            classContent.setLength(classContent.length() - 2);
            classContent.append("),\n");
        }
        classContent.setLength(classContent.length() - 2);
        classContent.append("\n    );\n");
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
                "import com.social100.todero.common.channels.DynamicEventChannel;\n" +
                "import com.social100.todero.common.command.CommandContext;\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n" +
                "import java.util.function.BiFunction;\n" +
                "import java.util.function.Function;\n" +
                "\n" +
                "public class " + generatedClassName + " {\n\n" +
                "    public static final Map<String, Map<String, Function<CommandContext, Boolean>>> STATIC_REGISTRY = new HashMap<>();\n" +
                "    public static final Map<String, Map<String, BiFunction<Object, CommandContext, Boolean>>> INSTANCE_REGISTRY = new HashMap<>();\n" +
                "\n" +
                "    static {\n");
        //for (Map.Entry<String, Map<String, MethodDetails>> nameToMethodDetail : nameToMethodDetails.entrySet()) {
            String instanceRegistryName = "instance" + getSimpleName(pluginClassQualifiedName) + "Registry";
            String staticRegistryName = "static" + getSimpleName(pluginClassQualifiedName) + "Registry";
            classContent.append("        Map<String, BiFunction<Object, CommandContext, Boolean>> " +  instanceRegistryName + " = new HashMap<>();\n");
            classContent.append("        Map<String, Function<CommandContext, Boolean>> " + staticRegistryName + " = new HashMap<>();\n");
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
                            .append("context -> ")
                            .append(details.className)
                            .append(".")
                            .append(details.methodName)
                            .append("(context));\n");
                } else {
                    // Populate the INSTANCE_REGISTRY for instance methods
                    classContent.append("        ")
                            .append(instanceRegistryName)
                            .append(".put(\"")
                            .append(methodName)
                            .append("\", ")
                            .append("(instance, context) -> \n")
                            .append("            ((")
                            .append(details.className)
                            .append(") instance).")
                            .append(details.methodName)
                            .append("(context));\n");
                }
            }
            classContent.append("        STATIC_REGISTRY.put(\"" + pluginClassQualifiedName + "\", " + staticRegistryName + ");\n");
            classContent.append("        INSTANCE_REGISTRY.put(\"" + pluginClassQualifiedName + "\", " + instanceRegistryName + ");\n\n");
        //}

        classContent.append("    }\n\n" +
                "    public static Boolean executeStatic(String plugin, String pluginName, String command, CommandContext context) {\n" +
                "        Function<CommandContext, Boolean> function = STATIC_REGISTRY.get(plugin).get(command);\n" +
                "        if (function != null) {\n" +
                "            return function.apply(context);\n" +
                "        } else {\n" +
                "            throw new IllegalArgumentException(\"No static method found for command: \" + command);\n" +
                "        }\n" +
                "    }\n\n" +
                "    public static Boolean executeInstance(String plugin, String pluginName, String command, Object instance, CommandContext context) {\n" +
                "        BiFunction<Object, CommandContext, Boolean> function = INSTANCE_REGISTRY.get(plugin).get(command);\n" +
                "        if (function != null) {\n" +
                "            return function.apply(instance, context);\n" +
                "        } else {\n" +
                "            throw new IllegalArgumentException(\"No instance method found for command: \" + command);\n" +
                "        }\n" +
                "    }\n\n" +
                "    public static Boolean execute(String plugin, String pluginName, String command, Object instance, CommandContext context) {\n" +
                "        BiFunction<Object, CommandContext, Boolean> instanceFunction = INSTANCE_REGISTRY.get(plugin).get(command);\n" +
                "        if (instanceFunction != null) {\n" +
                "            return instanceFunction.apply(instance, context);\n" +
                "        }\n" +
                "        Function<CommandContext, Boolean> staticFunction = STATIC_REGISTRY.get(plugin).get(command);\n" +
                "        if (staticFunction != null) {\n" +
                "            return staticFunction.apply(context);\n" +
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

    private void generatePluginInterfaceImplementation(String packageName, String pluginClassQualifiedName, boolean commandManagerRequired, String pluginName, String pluginDescription) {

        String classSimpleName = getSimpleName(pluginClassQualifiedName);
        String classVariableName = classToClassVariableName(classSimpleName);
        String generatedClassName = classSimpleName + "Impl";

        String generatedAnnotationRegistryClassName = classSimpleName + pluginName + "AR";
        String generatedMethodRegistryClassName = classSimpleName + pluginName + "MR";

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "\n" +
                "import " + pluginClassQualifiedName + ";\n" +
                "import " + pluginClassQualifiedName + "Tools;\n" +
                "import com.social100.todero.common.command.CommandContext;\n" +
                "import com.social100.todero.common.observer.PublisherManager;\n" +
                "import com.social100.todero.common.channels.ComponentEventListenerSupport;\n" +
                "import com.social100.todero.common.channels.EventChannel;\n" +
                "import com.social100.todero.console.base.CommandManager;\n" +
                "import com.social100.todero.common.model.plugin.Command;\n" +
                "import com.social100.todero.common.model.plugin.Component;\n" +
                "import com.social100.todero.common.model.plugin.PluginInterface;\n" +
                "import com.social100.todero.common.observer.Observer;\n" +
                "import com.social100.todero.generated." + generatedAnnotationRegistryClassName + ";\n" +
                "import com.social100.todero.generated." + generatedMethodRegistryClassName + ";\n" +
                "\n" +
                "import java.util.stream.Collectors;\n" +
                "\n" +
                "public class " + generatedClassName + " extends PublisherManager implements PluginInterface {\n" +
                "\n" +
                "    private static " + pluginClassQualifiedName + " " + classVariableName + ";\n" +
                "    private static " + pluginClassQualifiedName + "Tools " + classVariableName + "Tools;\n" +
                "    private final Component component;\n" +
                "\n" +
                "    public " + generatedClassName + "(EventChannel.EventListener listener" + (commandManagerRequired ? ", CommandManager commandManager" : "") + ") {\n" +
                "        " + classVariableName + " = new " + classSimpleName + "(" + (commandManagerRequired ? ", commandManager" : "") + ");\n" +
                "        " + classVariableName + "Tools = new " + classSimpleName + "Tools();\n" +
                "        if (" + classVariableName + "Tools instanceof ComponentEventListenerSupport) {\n" +
                "            ((ComponentEventListenerSupport)" + classVariableName + "Tools).addComponentEventListener(listener);\n" +
                "        }\n" +
                "        component = Component\n" +
                "                .builder()\n" +
                "                .name(\"" + pluginName + "\")\n" +
                "                .description(\"" + pluginDescription + "\")\n" +
                "                .commands(" + generatedAnnotationRegistryClassName + ".REGISTRY.stream()\n" +
                "                        .map(entry -> new Command(\n" +
                "                                entry.get(\"group\"),\n" +
                "                                entry.get(\"command\"),\n" +
                "                                entry.get(\"description\"),\n" +
                "                                entry.get(\"method\"),\n" +
                "                                entry.get(\"static\")" +
                "                        ))\n" +
                "                        .collect(Collectors.groupingBy(\n" +
                "                                Command::getGroup, // Group by the \"group\" field\n" +
                "                                Collectors.toMap(\n" +
                "                                        Command::getCommand, // Key is the \"command\" field\n" +
                "                                        command -> command // Value is the Command object\n" +
                "                                )\n" +
                "                        ))\n" +
                "                )\n" +
                "                .build();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Component getComponent() {\n" +
                "        return component;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Boolean execute(String pluginName, String command, CommandContext context) {\n" +
                "        context.setInstance(" + classVariableName + "Tools);\n" +
                "        return " + generatedMethodRegistryClassName + ".execute(\"" + pluginClassQualifiedName + "\", pluginName, command, " + classVariableName + ", context);\n" +
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

    private List<? extends TypeMirror> getClassTypeMirrorsForAIADependencies(TypeElement classElement) {
        AIADependencies annotation = classElement.getAnnotation(AIADependencies.class);
        if (annotation == null) {
            return null;
        }

        try {
            // Attempt to access `components()` to trigger MirroredTypesException
            annotation.components();
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
        return null;
    }

    private String generateNewObjectString(List<? extends TypeMirror> classTypeMirrors) {
        if (classTypeMirrors == null || classTypeMirrors.isEmpty()) {
            return "";
        }
        StringBuilder objectCreationString = new StringBuilder();
        for (TypeMirror typeMirror : classTypeMirrors) {
            String className = typeMirror.toString();
            objectCreationString.append("new ").append(className).append("(), ");
        }
        // Remove trailing comma and space, if any
        if (!objectCreationString.isEmpty()) {
            objectCreationString.setLength(objectCreationString.length() - 2);
        }
        return objectCreationString.toString();
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
