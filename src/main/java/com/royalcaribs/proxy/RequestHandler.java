package com.royalcaribs.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

public class RequestHandler implements Runnable {
    private final Socket socket;
    private final HttpClient httpClient;

    public RequestHandler(Socket socket) {
        this.socket = socket;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            while (true) {
                byte[] headerBytes = readBytes(in, 8);
                if (headerBytes == null) break;
                int reqLength = Integer.parseInt(new String(headerBytes, StandardCharsets.UTF_8));
                byte[] requestBytes = readBytes(in, reqLength);
                if (requestBytes == null) break;
                byte[] responseBytes = processRequest(requestBytes);
                String respHeader = String.format("%08d", responseBytes.length);
                out.write(respHeader.getBytes(StandardCharsets.UTF_8));
                out.write(responseBytes);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private byte[] readBytes(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int bytesRead = 0;
        while (bytesRead < length) {
            int count = in.read(data, bytesRead, length - bytesRead);
            if (count == -1) return null;
            bytesRead += count;
        }
        return data;
    }

    private byte[] processRequest(byte[] requestBytes) {
        try {
            String requestStr = new String(requestBytes, StandardCharsets.UTF_8);
            String[] lines = requestStr.split("\r\n");
            if (lines.length == 0) {
                return badGatewayResponse();
            }
            String requestLine = lines[0];
            String[] requestLineParts = requestLine.split(" ");
            if (requestLineParts.length < 3) {
                return badGatewayResponse();
            }
            String method = requestLineParts[0];
            String url = requestLineParts[1];
            URI uri = URI.create(url);
            
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10));
            if (method.equalsIgnoreCase("GET")) {
                reqBuilder.GET();
            } else if (method.equalsIgnoreCase("POST")) {
                String delim = "\r\n\r\n";
                int idx = requestStr.indexOf(delim);
                byte[] body = new byte[0];
                if (idx != -1) {
                    body = Arrays.copyOfRange(requestBytes, idx + delim.getBytes(StandardCharsets.UTF_8).length, requestBytes.length);
                }
                reqBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
            } else {
                return generateResponse("HTTP/1.1 501 Not Implemented\r\nContent-Length: 16\r\n\r\nNot Implemented");
            }
            HttpRequest httpRequest = reqBuilder.build();
            HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 ")
                           .append(httpResponse.statusCode())
                           .append(" ")
                           .append(getReasonPhrase(httpResponse.statusCode()))
                           .append("\r\n");
            httpResponse.headers().map().forEach((k, v) -> {
                responseBuilder.append(k).append(": ").append(String.join(",", v)).append("\r\n");
            });
            responseBuilder.append("\r\n");
            byte[] headerBytesResponse = responseBuilder.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bodyBytes = httpResponse.body();
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            responseStream.write(headerBytesResponse);
            responseStream.write(bodyBytes);
            return responseStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return badGatewayResponse();
        }
    }
    
    private byte[] badGatewayResponse() {
        String response = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 11\r\n\r\nBad Gateway";
        return response.getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] generateResponse(String response) {
        return response.getBytes(StandardCharsets.UTF_8);
    }
    
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            default -> "";
        };
    }
}
