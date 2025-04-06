package com.royalcaribs.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PersistentConnection {
    private Socket socket;
    private final String offshoreHost;
    private final int offshorePort;
    // Use a lock to ensure sequential access.
    private final Object lock = new Object();
    private DataOutputStream output;
    private DataInputStream input;

    public PersistentConnection(String offshoreHost, int offshorePort) throws IOException {
        this.offshoreHost = offshoreHost;
        this.offshorePort = offshorePort;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(offshoreHost, offshorePort);
        output = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());
        System.out.println("Connected to offshore proxy at " + offshoreHost + ":" + offshorePort);
    }

    /**
     * Sends the given request bytes (framed with an 8‑byte header) and returns the response bytes.
     */
    public byte[] sendRequest(byte[] requestBytes) throws IOException {
        synchronized (lock) {
            String header = String.format("%08d", requestBytes.length);
            output.write(header.getBytes(StandardCharsets.UTF_8));
            output.write(requestBytes);
            output.flush();

            byte[] respHeaderBytes = input.readNBytes(8);
            if (respHeaderBytes.length < 8) {
                throw new IOException("Incomplete response header");
            }
            int respLength = Integer.parseInt(new String(respHeaderBytes, StandardCharsets.UTF_8));
            byte[] responseData = input.readNBytes(respLength);
            if (responseData.length < respLength) {
                throw new IOException("Incomplete response data");
            }
            return responseData;
        }
    }
    
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
