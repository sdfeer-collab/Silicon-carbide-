package com.mindplus.optimizer.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 高性能 IPC 处理器
 * 使用 TCP Socket 实现高效的进程间通信
 */
public class IPCHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("IPCHandler");

    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final BlockingQueue<IPCMessage> messageQueue;
    private final ConcurrentHashMap<String, Socket> clients;

    public IPCHandler(int port) {
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(false);
        this.messageQueue = new LinkedBlockingQueue<>();
        this.clients = new ConcurrentHashMap<>();
    }

    /**
     * 启动 IPC 服务器
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);

            executor.submit(this::acceptConnections);
            executor.submit(this::processMessages);

            LOGGER.info("IPC server started on port {}", port);
        } catch (IOException e) {
            LOGGER.error("Failed to start IPC server on port {}", port, e);
        }
    }

    /**
     * 连接到 IPC 服务器
     */
    public void connect(String host, int port) {
        executor.submit(() -> {
            try {
                Socket socket = new Socket(host, port);
                clients.put("client-" + System.currentTimeMillis(), socket);
                LOGGER.info("Connected to IPC server {}:{}", host, port);

                // 启动接收线程
                executor.submit(() -> receiveMessages(socket));
            } catch (IOException e) {
                LOGGER.error("Failed to connect to IPC server {}:{}", host, port, e);
            }
        });
    }

    /**
     * 发送消息
     */
    public void sendMessage(IPCMessage message) {
        messageQueue.offer(message);
    }

    /**
     * 接受客户端连接
     */
    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = "client-" + System.currentTimeMillis();
                clients.put(clientId, clientSocket);
                LOGGER.info("Accepted IPC connection: {}", clientId);

                executor.submit(() -> receiveMessages(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    LOGGER.error("Error accepting connection", e);
                }
            }
        }
    }

    /**
     * 接收消息
     */
    private void receiveMessages(Socket socket) {
        try (DataInputStream input = new DataInputStream(socket.getInputStream())) {
            while (running.get() && !socket.isClosed()) {
                // 读取消息长度
                int length = input.readInt();
                if (length <= 0 || length > 10 * 1024 * 1024) { // 最大 10MB
                    break;
                }

                // 读取消息数据
                byte[] data = new byte[length];
                input.readFully(data);

                IPCMessage message = IPCMessage.deserialize(data);
                handleMessage(message);
            }
        } catch (IOException e) {
            if (running.get()) {
                LOGGER.debug("Connection closed", e);
            }
        } finally {
            clients.values().remove(socket);
            closeQuietly(socket);
        }
    }

    /**
     * 处理消息
     */
    private void handleMessage(IPCMessage message) {
        switch (message.type) {
            case RENDER_TASK:
                handleRenderTask(message);
                break;
            case RENDER_RESULT:
                handleRenderResult(message);
                break;
            case SHUTDOWN:
                running.set(false);
                break;
            default:
                LOGGER.warn("Unknown message type: {}", message.type);
        }
    }

    /**
     * 处理渲染任务
     */
    private void handleRenderTask(IPCMessage message) {
        // TODO: 处理渲染任务
    }

    /**
     * 处理渲染结果
     */
    private void handleRenderResult(IPCMessage message) {
        // TODO: 处理渲染结果
    }

    /**
     * 处理消息队列
     */
    private void processMessages() {
        while (running.get()) {
            try {
                IPCMessage message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    broadcastMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 广播消息到所有客户端
     */
    private void broadcastMessage(IPCMessage message) {
        byte[] data = message.serialize();

        for (Socket socket : clients.values()) {
            try {
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                output.writeInt(data.length);
                output.write(data);
                output.flush();
            } catch (IOException e) {
                closeQuietly(socket);
                clients.values().remove(socket);
            }
        }
    }

    /**
     * 关闭连接
     */
    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * 关闭服务器套接字
     */
    private void closeQuietly(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * 关闭 IPC 处理器
     */
    public void shutdown() {
        running.set(false);
        executor.shutdown();

        // 关闭所有客户端连接
        for (Socket socket : clients.values()) {
            closeQuietly(socket);
        }
        clients.clear();

        // 关闭服务器套接字
        if (serverSocket != null && !serverSocket.isClosed()) {
            closeQuietly(serverSocket);
        }

        LOGGER.info("IPC server shutdown");
    }

    /**
     * 消息类型
     */
    public enum MessageType {
        RENDER_TASK,
        RENDER_RESULT,
        SHUTDOWN
    }

    /**
     * IPC 消息
     */
    public static class IPCMessage {
        public final MessageType type;
        public final byte[] data;
        public final long timestamp;

        public IPCMessage(MessageType type, byte[] data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * 序列化消息
         */
        public byte[] serialize() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(baos)) {
                output.writeInt(type.ordinal());
                output.writeInt(data.length);
                output.write(data);
                output.writeLong(timestamp);
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize message", e);
            }
            return baos.toByteArray();
        }

        /**
         * 反序列化消息
         */
        public static IPCMessage deserialize(byte[] bytes) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                int typeOrdinal = input.readInt();
                MessageType type = MessageType.values()[typeOrdinal];
                int dataLength = input.readInt();
                byte[] data = new byte[dataLength];
                input.readFully(data);
                long timestamp = input.readLong();

                IPCMessage message = new IPCMessage(type, data);
                // timestamp 是 final 的，无法设置
                return message;
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize message", e);
            }
        }
    }
}