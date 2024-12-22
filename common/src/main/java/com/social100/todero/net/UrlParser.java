package com.social100.todero.net;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

public class UrlParser {
    // Public method to parse URL and return host/port as HostInfo
    public static HostInfo parse(String url) {
        String protocol = "aia"; // Default protocol is ftp
        String host;
        int port;

        try {
            // Check if the protocol is defined
            int protocolEndIndex = url.indexOf("://");
            if (protocolEndIndex != -1) {
                protocol = url.substring(0, protocolEndIndex);
                url = url.substring(protocolEndIndex + 3); // Remove protocol part from URL
            }

            // Extract the server and port
            int colonIndex = url.indexOf(":");
            int slashIndex = url.indexOf("/");

            if (colonIndex != -1) {
                // Port is explicitly defined
                host = url.substring(0, colonIndex);
                String portStr = url.substring(colonIndex + 1, slashIndex != -1 ? slashIndex : url.length());
                port = Integer.parseInt(portStr);
            } else {
                // Default ports based on the protocol
                host = slashIndex != -1 ? url.substring(0, slashIndex) : url;
                if (protocol.equalsIgnoreCase("aia")) {
                    port = 9876;
                } else {
                    throw new IllegalArgumentException("Unsupported protocol: " + protocol);
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }

        // Return the result
        return new HostInfo(host, port);
    }

    @Getter
    @ToString
    @AllArgsConstructor
    public static class HostInfo {
        private final String host;
        private final int port;
    }
}