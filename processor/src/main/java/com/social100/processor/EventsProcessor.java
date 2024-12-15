package com.social100.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes({"com.social100.processor.Events", "com.social100.processor.EventDefinition"})
@SupportedSourceVersion(SourceVersion.RELEASE_17) // Use your Java version
public class EventsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Events.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) element;
                Events annotation = classElement.getAnnotation(Events.class);

                String className = classElement.getSimpleName() + "Tools";
                String packageName = processingEnv.getElementUtils().getPackageOf(classElement).toString();

                try {
                    JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + className);
                    try (Writer writer = file.openWriter()) {
                        writer.write("package " + packageName + ";\n\n");
                        writer.write("import com.social100.todero.common.channels.DynamicEventChannel;\n");
                        writer.write("import com.social100.todero.common.channels.ComponentEventListenerSupport;\n");
                        writer.write("import com.social100.todero.common.channels.EventChannel;");
                        writer.write("import com.social100.todero.common.channels.ReservedEventRegistry;\n\n");
                        writer.write("public class " + className + " extends DynamicEventChannel implements ComponentEventListenerSupport {\n");

                        // Generate static event constants
                        for (EventDefinition event : annotation.value()) {
                            writer.write("    public static final String EVENT_" + event.name().toUpperCase() + " = \"" + event.name() + "\";\n");
                        }

                        writer.write("\n");
                        // Constructor
                        writer.write("    public " + className + "() {\n");
                        for (EventDefinition event : annotation.value()) {
                            writer.write("        this.registerEvent(EVENT_" + event.name().toUpperCase() + ", \"" + event.description() + "\");\n");
                        }
                        writer.write("    }\n\n");

                        writer.write("    public void addComponentEventListener(EventChannel.EventListener listener) {\n");
                        for (EventDefinition event : annotation.value()) {
                            writer.write("        this.subscribeToEvent(EVENT_" + event.name().toUpperCase() + ", listener);\n");
                        }
                        writer.write("\n");
                        writer.write("        // Subscribe to reserved events directly\n" +
                                "        for (EventChannel.ReservedEvent reservedEvent : EventChannel.ReservedEvent.values()) {\n" +
                                "            ReservedEventRegistry.subscribe(reservedEvent, listener);\n" +
                                "        }\n");
                        writer.write("    }\n\n");

                        // Methods to trigger events
                        for (EventDefinition event : annotation.value()) {
                            writer.write("    public void " + event.name() + "(String message) {\n");
                            writer.write("        this.triggerEvent(EVENT_" + event.name().toUpperCase() + ", message);\n");
                            writer.write("    }\n\n");
                        }

                        writer.write("}\n");
                    }
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, e.getMessage());
                }
            }
        }
        return true;
    }
}