package com.social100.todero;

import com.social100.todero.web.WebServer;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        WebServer server = new WebServer(8080);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}