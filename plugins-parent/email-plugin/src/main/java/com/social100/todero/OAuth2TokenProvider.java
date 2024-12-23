package com.social100.todero;

import java.io.IOException;

public class OAuth2TokenProvider {
    public static String getToken(String provider) {
        try {
            String scope = OAuth2Config.getScope(provider);
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault().createScoped(scope);

            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();

            return token.getTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get OAuth2 token", e);
        }
    }
}