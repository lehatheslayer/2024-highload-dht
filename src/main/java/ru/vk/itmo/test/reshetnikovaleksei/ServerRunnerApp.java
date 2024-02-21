package ru.vk.itmo.test.reshetnikovaleksei;

import ru.vk.itmo.ServiceConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ServerRunnerApp {
    private ServerRunnerApp() {

    }

    public static void main(String[] args) throws IOException {
        Path path = Path.of("/Users/alreshetnikov/IdeaProjects/2024-highload-dht/data");

        ServiceConfig serviceConfig = new ServiceConfig(8080, "http:localhost", List.of("http:localhost"), path);
        HttpServerImpl server = new HttpServerImpl(serviceConfig);

        server.start();
    }
}