package com.social100.todero.web;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class TemplateProcessor {

    private final Configuration freemarkerConfig;

    public TemplateProcessor(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    public String processTemplateWithScript(String templateName, Map<String, Object> model) throws IOException, TemplateException {
        // Load the template
        Template template = freemarkerConfig.getTemplate(templateName);

        // Read the original template content
        StringWriter originalContentWriter = new StringWriter();
        template.process(model, originalContentWriter);
        String originalContent = originalContentWriter.toString();

        if (originalContent.contains("<script src=\"/ws.js\">")) {
            return originalContent; // Skip modification if the script is already present
        }

        // Inject the JavaScript
        Template modifiedTemplate = getModifiedTemplate(originalContent);

        StringWriter modifiedContentWriter = new StringWriter();
        modifiedTemplate.process(model, modifiedContentWriter);

        return modifiedContentWriter.toString();
    }

    private Template getModifiedTemplate(String originalContent) throws IOException {
        String scriptTag = "<script src=\"/ws.js\"></script>";
        String injectedContent;

        if (originalContent.contains("</head>")) {
            // Inject before the closing </head> tag
            injectedContent = originalContent.replace("</head>", scriptTag + "\n</head>");
        } else {
            // Add a <head> tag if it doesn't exist and inject the script
            injectedContent = originalContent.replace("<html", "<html>\n<head>\n" + scriptTag + "\n</head>");
        }

        // Render the modified template
        return new Template("modified", new StringReader(injectedContent), freemarkerConfig);
    }
}