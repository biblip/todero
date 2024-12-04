package com.social100.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("com.social100.processor.AIADependencies")
@SupportedSourceVersion(SourceVersion.RELEASE_17) // Replace with your Java version
public class AIADependenciesProcessor extends AbstractProcessor {
    String COMMAND_MANAGER_INTERFACE = "com.social100.todero.cli.base.CommandManager";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(AIADependencies.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@AIADependencies can only be applied to classes", element);
                continue;
            }

            TypeElement classElement = (TypeElement) element;

            // Use TypeMirror to retrieve the class types from the annotation
            List<? extends TypeMirror> classTypeMirrors = getClassTypeMirrors(classElement);
            if (classTypeMirrors == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Could not process @AIADependencies annotation", element);
                continue;
            }

            // Check if the annotated class has a matching constructor
            boolean hasMatchingConstructor = classElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                    .anyMatch(constructor -> hasSingleParameterImplementingInterface((ExecutableElement) constructor, COMMAND_MANAGER_INTERFACE));

            if (!hasMatchingConstructor) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Class " + classElement.getSimpleName() +
                                " does not have a constructor matching the class: " + COMMAND_MANAGER_INTERFACE, element);
            }
        }
        return true;
    }

    private List<? extends TypeMirror> getClassTypeMirrors(TypeElement classElement) {
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

    private boolean hasMatchingParameters(ExecutableElement constructor, List<? extends TypeMirror> requiredTypes) {
        List<? extends VariableElement> parameters = constructor.getParameters();

        if (parameters.size() != requiredTypes.size()) {
            return false; // Parameter count mismatch
        }

        for (int i = 0; i < parameters.size(); i++) {
            TypeMirror parameterType = parameters.get(i).asType();
            TypeMirror requiredType = requiredTypes.get(i);

            if (!processingEnv.getTypeUtils().isSameType(parameterType, requiredType)) {
                return false; // Type mismatch
            }
        }
        return true; // All parameters match
    }

    private boolean hasSingleParameterImplementingInterface(ExecutableElement constructor, String interfaceName) {
        List<? extends VariableElement> parameters = constructor.getParameters();

        // Ensure the constructor has only one parameter
        if (parameters.size() != 1) {
            return false;
        }

        // Get the type of the parameter
        TypeMirror parameterType = parameters.get(0).asType();

        // Get the interface TypeMirror from its name
        TypeElement interfaceElement = processingEnv.getElementUtils().getTypeElement(interfaceName);
        if (interfaceElement == null) {
            throw new IllegalArgumentException("Interface not found: " + interfaceName);
        }
        TypeMirror interfaceType = interfaceElement.asType();

        // Check if the parameter implements the interface
        return processingEnv.getTypeUtils().isAssignable(parameterType, interfaceType);
    }

}
