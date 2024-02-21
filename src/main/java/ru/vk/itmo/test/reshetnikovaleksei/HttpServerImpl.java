package ru.vk.itmo.test.reshetnikovaleksei;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import ru.vk.itmo.ServiceConfig;
import ru.vk.itmo.dao.BaseEntry;
import ru.vk.itmo.dao.Config;
import ru.vk.itmo.dao.Entry;
import ru.vk.itmo.test.reference.dao.ReferenceDao;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

public class HttpServerImpl extends HttpServer {
    private final Config daoConfig;
    private ReferenceDao dao;

    public HttpServerImpl(ServiceConfig config) throws IOException {
        super(createConfig(config));

        this.daoConfig = createDaoConfig(config);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) String id) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        Entry<MemorySegment> result = dao.get(parseToMemorySegment(id));
        if (result == null) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(result.value().toArray(ValueLayout.JAVA_BYTE));
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(value = "id", required = true) String id, Request request) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        dao.upsert(new BaseEntry<>(parseToMemorySegment(id), MemorySegment.ofArray(request.getBody())));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) String id) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        dao.upsert(new BaseEntry<>(parseToMemorySegment(id), null));
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        if (request.getMethod() == Request.METHOD_GET) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
        session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
    }

    private MemorySegment parseToMemorySegment(String input) {
        return MemorySegment.ofArray(input.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpServerConfig createConfig(ServiceConfig config) {
        HttpServerConfig httpServerConfig = new HttpServerConfig();
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = config.selfPort();
        acceptorConfig.reusePort = true;

        httpServerConfig.acceptors = new AcceptorConfig[] {acceptorConfig};
        httpServerConfig.closeSessions = true;
        return httpServerConfig;
    }

    private static Config createDaoConfig(ServiceConfig serviceConfig) {
        return new Config(serviceConfig.workingDir(), 1024);
    }

    @Override
    public synchronized void start() {
        try {
            dao = new ReferenceDao(daoConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();

        try {
            dao.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
