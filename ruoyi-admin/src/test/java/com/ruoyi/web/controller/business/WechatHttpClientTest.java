package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class WechatHttpClientTest
{
    private HttpServer server;
    private String base;
    private final RestTemplate client = WechatHttpClient.create();
    private final AtomicReference<String> requestType = new AtomicReference<>();

    @Before
    public void start() throws Exception
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        response("/token", "text/plain", "{\"access_token\":\"test-token\",\"expires_in\":7200}");
        response("/error", "text/plain; charset=utf-8", "{\"errcode\":40001,\"errmsg\":\"凭据无效\"}");
        response("/json", "application/json", "{\"errcode\":0}");
        response("/invalid", "text/plain", "upstream unavailable");
        server.createContext("/image", exchange -> {
            byte[] bytes = new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void response(String path, String type, String body)
    {
        server.createContext(path, exchange -> {
            requestType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @After
    public void stop() { server.stop(0); }

    @Test
    public void readsJsonTokensAndPlatformErrorsLabelledAsPlainText()
    {
        assertEquals("test-token", client.getForObject(base + "/token", JsonNode.class).path("access_token").asText());
        JsonNode error = client.getForObject(base + "/error", JsonNode.class);
        assertEquals(40001, error.path("errcode").asInt());
        assertEquals("凭据无效", error.path("errmsg").asText());
        assertEquals(0, client.getForObject(base + "/json", JsonNode.class).path("errcode").asInt());
    }

    @Test
    public void stillSendsJsonAndReadsBinaryCodeResponses()
    {
        assertEquals(0, client.postForObject(base + "/json", Collections.singletonMap("scene", "TEST"), JsonNode.class).path("errcode").asInt());
        assertTrue(requestType.get().startsWith("application/json"));
        assertArrayEquals(new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 },
                client.getForObject(base + "/image", byte[].class));
    }

    @Test
    public void refusesNonJsonPlainTextInsteadOfTreatingItAsSuccess()
    {
        assertThrows(RestClientException.class, () -> client.getForObject(base + "/invalid", JsonNode.class));
    }
}
