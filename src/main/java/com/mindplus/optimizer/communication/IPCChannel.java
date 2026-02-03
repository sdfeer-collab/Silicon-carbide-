package com.mindplus.optimizer.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class IPCChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger("IPCChannel");

    private final ZContext context;
    private final int type;
    private final String address;
    private Socket socket;

    public IPCChannel(int type, String address) {
        this.context = new ZContext();
        this.type = type;
        this.address = address;
    }
    
    public void connect() {
        try {
            socket = context.createSocket(type);
            socket.connect(address);
            LOGGER.info("Connected to {}", address);
        } catch (Exception e) {
            LOGGER.error("Failed to connect to {}", address, e);
        }
    }
    
    public void bind() {
        try {
            socket = context.createSocket(type);
            socket.bind(address);
            LOGGER.info("Bound to {}", address);
        } catch (Exception e) {
            LOGGER.error("Failed to bind to {}", address, e);
        }
    }
    
    public void send(byte[] data) {
        send(data, 0);
    }
    
    public void send(byte[] data, int flags) {
        if (socket != null) {
            socket.send(data, flags);
        }
    }
    
    public byte[] receive() {
        return receive(0);
    }
    
    public byte[] receive(int flags) {
        if (socket != null) {
            return socket.recv(flags);
        }
        return null;
    }
    
    public void close() {
        if (socket != null) {
            socket.close();
        }
        context.close();
    }
}