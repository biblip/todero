package com.social100.todero.oauth2;

import fi.iki.elonen.NanoHTTPD;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OAuth2CallbackServer extends NanoHTTPD {

    private Configuration freemarkerConfig;

    public OAuth2CallbackServer(int port) {
        super(port);
        setupFreemarker();
    }

    private void setupFreemarker() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerConfig.setClassForTemplateLoading(getClass(), "/templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    @Override
    public Response serve(IHTTPSession session) {
        TemplateProcessor processor = new TemplateProcessor(freemarkerConfig);
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            switch (uri) {
                case "/":
                    return handleIndex(processor, session);
                case "/ws.js":
                    return handleWebSocketJavascript(session);
                case "/oauth2":
                    return handleOAuth2Launch(processor, session);
                case "/wstest":
                    return handleWSTest(processor, session);
                case "/callback":
                    return handleCallback(processor, session);
                default:
                    return handleNotFound(processor, uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
        }
    }

    private Response handleWSTest(TemplateProcessor processor, IHTTPSession session) throws TemplateException, IOException {
        // Data model for the template
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Welcome to the Index Page");
        model.put("message", "This is the index page rendered dynamically.");
        model.put("year", 2024);

        // Render the template
        // Initialize TemplateProcessor

        // Process the template with the injected script
        String html = processor.processTemplateWithScript("ws_test.ftl", model);

        // Serve the rendered HTML
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response handleWebSocketJavascript(IHTTPSession session) throws TemplateException, IOException {
        String uri = session.getUri();

        // Serve static files
        if (uri.startsWith("/ws.js")) {
            try {
                InputStream inputStream = getClass().getResourceAsStream("/static/ws.js");
                if (inputStream == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
                }
                return newFixedLengthResponse(Response.Status.OK, "application/javascript", inputStream, inputStream.available());
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
            }
        }

        // Handle other requests (e.g., dynamic templates)
        return super.serve(session);
    }

    // Handler for /index
    private Response handleIndex(TemplateProcessor processor, IHTTPSession session) throws IOException, TemplateException {
        // Data model for the template
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Welcome to the Index Page");
        model.put("message", "This is the index page rendered dynamically.");
        model.put("year", 2024);

        // Render the template
        // Initialize TemplateProcessor

        // Process the template with the injected script
        String html = processor.processTemplateWithScript("index.ftl", model);

        // Serve the rendered HTML
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    // Handler for /callback
    private Response handleCallback(TemplateProcessor processor,IHTTPSession session) {
        // Extract query parameters
        Map<String, List<String>> params = session.getParameters();
        String code = params.getOrDefault("code", List.of("")).get(0);
        String state = params.getOrDefault("state", List.of("")).get(0);

        // Handle the OAuth2 callback logic
        String message = String.format("Received OAuth2 Callback! Code: %s, State: %s", code, state);

        // Respond with a simple message
        return newFixedLengthResponse(Response.Status.OK, "text/plain", message);
    }

    private Response handleOAuth2Launch(TemplateProcessor processor,IHTTPSession session) throws IOException, TemplateException {
        // Load the template
        Template template = freemarkerConfig.getTemplate("oauth2.ftl");

        // Data model for the template
        Map<String, Object> model = new HashMap<>();
        model.put("platformName", "MyApp");
        model.put("platformLogo", "/path/to/platform/logo.png"); // Replace with your logo URL or path
        model.put("platformDescription", "MyApp is a powerful platform for managing your tasks and projects.");
        model.put("serviceName", "Google Drive");
        model.put("serviceDescription", "Integrate with Google Drive to manage your files seamlessly.");
        model.put("serviceLogo", "/path/to/service/logo.png"); // Pass dynamically
        model.put("authUrl", "/auth?service=google"); // Replace with your OAuth2 URL
        model.put("year", 2024);

        // Render the template
        String html = processor.processTemplateWithScript( "oauth2.ftl", model);

        // Serve the rendered HTML
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    // Handler for not found paths
    private Response handleNotFound(TemplateProcessor processor,String uri) {
        String message = String.format("404 - Path '%s' not found!", uri);
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", message);
    }

    public static void main(String[] args) {
        try {
            OAuth2CallbackServer server = new OAuth2CallbackServer(8080);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("Server is running on http://localhost:8080/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}