package com.social100.todero;

import com.social100.todero.annotation.AIAController;
import com.social100.todero.annotation.Action;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SupportedAnnotationTypes({"com.social100.todero.annotation.AIAController", "com.social100.todero.annotation.Action"})
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
                List<Map<String, String>> methodDetailsList = new ArrayList<>();

                for (Element enclosedElement : classTypeElement.getEnclosedElements()) {
                    if (enclosedElement.getKind() == ElementKind.METHOD &&
                            enclosedElement.getAnnotation(Action.class) != null) {
                        Action methodAnnotation = enclosedElement.getAnnotation(Action.class);

                        // Extract attributes from MethodAnnotation
                        Map<String, String> methodDetails = new HashMap<>();
                        methodDetails.put("method", enclosedElement.getSimpleName().toString());
                        methodDetails.put("group", methodAnnotation.group());
                        methodDetails.put("command", methodAnnotation.command());
                        methodDetails.put("description", methodAnnotation.description());

                        methodDetailsList.add(methodDetails);
                    }
                }

                if (!methodDetailsList.isEmpty()) {
                    classToMethodsMap.put(classTypeElement.getQualifiedName().toString(), methodDetailsList);
                }
            }
        }

        // Generate a summary file
        generateRegistryClass("com.social100.todero.generated", classToMethodsMap);


        Map<String, Map<String, MethodDetails>> nameToMethodDetails = new HashMap<>();

        for (Element classElement : roundEnv.getElementsAnnotatedWith(AIAController.class)) {
            if (classElement.getKind() == ElementKind.CLASS) {
                TypeElement classTypeElement = (TypeElement) classElement;

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
            }
        }

        generateMethodRegistry("com.social100.todero.generated", nameToMethodDetails);

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

    private void generateRegistryClass(String packageName, Map<String, List<Map<String, String>>> classToMethodsMap) {

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "\n" +
                "public class AnnotationRegistry {\n");
        classContent.append("    public static final Map<String, List<Map<String, String>>> REGISTRY = Map.of(\n");

        for (Map.Entry<String, List<Map<String, String>>> entry : classToMethodsMap.entrySet()) {
            String className = entry.getKey();
            List<Map<String, String>> methods = entry.getValue();

            classContent.append("        \"").append(className).append("\", List.of(\n");
            for (Map<String, String> methodDetails : methods) {
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
            classContent.append("\n        ),\n");
        }

        // Remove trailing ", " and close REGISTRY
        classContent.setLength(classContent.length() - 2);
        classContent.append("\n    );\n}");

        try {
            // Write the generated file
            Filer filer = processingEnv.getFiler();
            JavaFileObject fileObject = filer.createSourceFile(packageName + ".AnnotationRegistry");

            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating registry class: " + e.getMessage());
        }
    }

    private void generateMethodRegistry(String packageName, Map<String, Map<String, MethodDetails>> nameToMethodDetails) {
        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n" +
                "import java.util.function.BiFunction;\n" +
                "import java.util.function.Function;\n" +
                "\n" +
                "public class MethodRegistry {\n\n" +
                "    public static final Map<String, Map<String, Function<String[], Object>>> STATIC_REGISTRY = new HashMap<>();\n" +
                "    public static final Map<String, Map<String, BiFunction<Object, String[], Object>>> INSTANCE_REGISTRY = new HashMap<>();\n" +
                "\n" +
                "    static {\n");
        for (Map.Entry<String, Map<String, MethodDetails>> nameToMethodDetail : nameToMethodDetails.entrySet()) {
            String instanceRegistryName = "instance" + getSimpleName(nameToMethodDetail.getKey()) + "Registry";
            String staticRegistryName = "static" + getSimpleName(nameToMethodDetail.getKey()) + "Registry";
            classContent.append("        Map<String, BiFunction<Object, String[], Object>> " +  instanceRegistryName + " = new HashMap<>();\n");
            classContent.append("        Map<String, Function<String[], Object>> " + staticRegistryName + " = new HashMap<>();\n");
            // Populate the STATIC_REGISTRY for static methods
            for (Map.Entry<String, MethodDetails> entry : nameToMethodDetail.getValue().entrySet()) {
                String methodName = entry.getKey();
                MethodDetails details = entry.getValue();

                if (details.isStatic) {
                    classContent.append("        STATIC_REGISTRY.put(\"").append(methodName).append("\", ")
                            .append("args -> ").append(details.className).append(".").append(details.methodName).append("(args));\n");
                } else {
                    // Populate the INSTANCE_REGISTRY for instance methods
                    classContent.append("        " + instanceRegistryName + ".put(\"").append(methodName).append("\", ")
                            .append("(instance, args) -> ((").append(details.className).append(") instance).")
                            .append(details.methodName).append("(args));\n");
                }
            }
            classContent.append("        STATIC_REGISTRY.put(\"" + nameToMethodDetail.getKey() + "\", " + staticRegistryName + ");\n");
            classContent.append("        INSTANCE_REGISTRY.put(\"" + nameToMethodDetail.getKey() + "\", " + instanceRegistryName + ");\n");
        }

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
                "    }\n" +
                "}\n");
        try {
            // Write the generated file
            Filer filer = processingEnv.getFiler();
            JavaFileObject fileObject = filer.createSourceFile(packageName + ".MethodRegistry");

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

        StringBuilder classContent = new StringBuilder("package " + packageName + ";\n\n" +
                "\n" +
                "import com.social100.todero.common.model.plugin.PluginInterface;\n" +
                "import com.social100.todero.generated.AnnotationRegistry;\n" +
                "import com.social100.todero.generated.MethodRegistry;\n" +
                "\n" +
                "import java.util.Arrays;\n" +
                "import java.util.Map;\n" +
                "\n" +
                "public class " + generatedClassName + " implements PluginInterface {\n" +
                "\n" +
                "    private static " + pluginClassQualifiedName + " " + classVariableName + ";\n" +
                "\n" +
                "    public " + generatedClassName + "() {\n" +
                "        " + classVariableName + " = new " + pluginClassQualifiedName + "();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Boolean hasCommand(String command) {\n" +
                "        return Arrays.asList(getAllCommandNames()).contains(command);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public Object execute(String command, String[] commandArgs) {\n" +
                "        return MethodRegistry.executeInstance(\"" + pluginClassQualifiedName + "\", command, " + classVariableName + ", commandArgs);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String name() {\n" +
                "        return \"" + pluginName + "\";\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String description() {\n" +
                "        return \"" + pluginDescription + "\";\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String[] getAllCommandNames() {\n" +
                "        return AnnotationRegistry.REGISTRY.get(\"" + pluginClassQualifiedName + "\")\n" +
                "                .stream()\n" +
                "                .flatMap(map -> map.keySet().stream().filter(\"command\"::equals).map(map::get))\n" +
                "                .toArray(String[]::new);" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String getHelpMessage() {\n" +
                "        StringBuilder sb = new StringBuilder();\n" +
                "        sb.append(\"" + pluginClassQualifiedName + "\\n\");\n" +
                "        AnnotationRegistry.REGISTRY.get(\"" + pluginClassQualifiedName + "\")\n" +
                "                .stream()\n" +
                "                .forEach(v -> printCommandHelp(sb, v));\n" +
                "        return sb.toString();" +
                "    }\n" +
                "\n" +
                "    private void printCommandHelp(StringBuilder sb, Map<String, String> entry) {\n" +
                "        sb.append(\"       - \" + entry.get(\"command\") + \n" +
                "                \":  \" + entry.get(\"description\") + \"\\n\"\n" +
                "        );\n" +
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
}
