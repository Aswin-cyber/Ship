package com.royalcaribs.proxy;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProxyHttpHandler implements HttpHandler {
    private final PersistentConnection persistentConnection;

    public ProxyHttpHandler(PersistentConnection persistentConnection) {
        this.persistentConnection = persistentConnection;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI requestURI = exchange.getRequestURI();
        String fullUrl = requestURI.toString();
        if (!requestURI.isAbsolute()) {
            List<String> hostHeader = exchange.getRequestHeaders().get("Host");
            if (hostHeader != null && !hostHeader.isEmpty()) {
                fullUrl = "http://" + hostHeader.get(0) + requestURI.toString();
            }
        }
        String requestLine = method + " " + fullUrl + " " + exchange.getProtocol() + "\r\n";
        
        StringBuilder headersBuilder = new StringBuilder();
        exchange.getRequestHeaders().forEach((key, values) -> {
            for (String value : values) {
                headersBuilder.append(key).append(": ").append(value).append("\r\n");
            }
        });
        headersBuilder.append("\r\n");
        
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        try (InputStream is = exchange.getRequestBody()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                bodyStream.write(buffer, 0, read);
            }
        }
        byte[] bodyBytes = bodyStream.toByteArray();
        
        ByteArrayOutputStream fullRequestStream = new ByteArrayOutputStream();
        fullRequestStream.write(requestLine.getBytes(StandardCharsets.UTF_8));
        fullRequestStream.write(headersBuilder.toString().getBytes(StandardCharsets.UTF_8));
        fullRequestStream.write(bodyBytes);
        byte[] fullRequestBytes = fullRequestStream.toByteArray();
        
        byte[] responseBytes;
        try {
            responseBytes = persistentConnection.sendRequest(fullRequestBytes);
        } catch (IOException e) {
            String errorResponse = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 11\r\n\r\nBad Gateway";
            responseBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
        }
        
        byte[] delimiter = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        int delimiterIndex = indexOf(responseBytes, delimiter);
        if (delimiterIndex == -1) {
            exchange.sendResponseHeaders(502, 0);
            exchange.close();
            return;
        }
        String headerPart = new String(responseBytes, 0, delimiterIndex, StandardCharsets.UTF_8);
        int bodyStart = delimiterIndex + delimiter.length;
        byte[] responseBody = new byte[responseBytes.length - bodyStart];
        System.arraycopy(responseBytes, bodyStart, responseBody, 0, responseBody.length);
        
        String[] headerLines = headerPart.split("\r\n");
        if (headerLines.length == 0) {
            exchange.sendResponseHeaders(502, 0);
            exchange.close();
            return;
        }
        String statusLine = headerLines[0];
        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = 502;
        if (statusParts.length >= 2) {
            try {
                statusCode = Integer.parseInt(statusParts[1]);
            } catch (NumberFormatException ignored) {}
        }
        
        Headers responseHeaders = exchange.getResponseHeaders();
        for (int i = 1; i < headerLines.length; i++) {
            String line = headerLines[i];
            if (line.isEmpty()) continue;
            int colonIndex = line.indexOf(":");
            if (colonIndex != -1) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                responseHeaders.add(key, value);
            }
        }
        
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
    
    private int indexOf(byte[] data, byte[] pattern) {
        outer: for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
