package com.royalcaribs.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class OffshoreProxy {
    public static void main(String[] args) throws IOException {
        int port = 9090;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Offshore Proxy running on port " + port);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
            new Thread(new RequestHandler(clientSocket)).start();
        }
    }
}
