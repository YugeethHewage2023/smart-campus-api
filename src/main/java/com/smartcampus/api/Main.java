package com.smartcampus.api;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

public final class Main {
    private static final String DEFAULT_BIND_HOST = "0.0.0.0";
    private static final String DEFAULT_PUBLIC_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final String API_PATH = "/api/v1";

    private Main() {
    }

    public static URI getBaseUri() {
        return URI.create(String.format("http://%s:%d%s/", getBindHost(), getPort(), API_PATH));
    }

    public static String getPublicApiUrl() {
        return String.format("http://%s:%d%s", getPublicHost(), getPort(), API_PATH);
    }

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), new ApplicationConfig());
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println("Smart Campus API started at " + getPublicApiUrl());
        System.out.println("Press ENTER to stop the server.");
        System.in.read();
        server.shutdownNow();
    }

    private static String getBindHost() {
        return System.getProperty("smartcampus.host", DEFAULT_BIND_HOST);
    }

    private static String getPublicHost() {
        return DEFAULT_PUBLIC_HOST;
    }

    private static int getPort() {
        String portValue = System.getProperty(
                "smartcampus.port",
                System.getenv().getOrDefault("SMART_CAMPUS_PORT", String.valueOf(DEFAULT_PORT)));
        return Integer.parseInt(portValue);
    }
}
