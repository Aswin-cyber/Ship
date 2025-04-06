package com.royalcaribs.proxy;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class ShipProxy {
    public static void main(String[] args) throws Exception {
        String offshoreHost = System.getenv().getOrDefault("OFFSHORE_HOST", "localhost");
        int offshorePort = Integer.parseInt(System.getenv().getOrDefault("OFFSHORE_PORT", "9090"));
        
        PersistentConnection persistentConnection = new PersistentConnection(offshoreHost, offshorePort);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ProxyHttpHandler(persistentConnection));
        server.setExecutor(null); // use default executor
        System.out.println("Ship Proxy running on port 8080");
        server.start();
    }
}

