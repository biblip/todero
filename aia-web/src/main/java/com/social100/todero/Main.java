package com.social100.todero;

import com.social100.todero.oauth2.OAuth2CallbackServer;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        OAuth2CallbackServer server = new OAuth2CallbackServer(8080);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}