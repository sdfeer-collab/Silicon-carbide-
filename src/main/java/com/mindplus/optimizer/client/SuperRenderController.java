package com.mindplus.optimizer.client;

import com.mindplus.optimizer.coordinator.RuntimeCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

/**
 * 超渲染客户端控制器
 * 负责向渲染进程发送超渲染参数
 */
public class SuperRenderController {
    private static final Logger LOGGER = LoggerFactory.getLogger("SuperRenderController");

    private static final int RENDERER_PORT = 5580;
    private ZMQ.Socket socket;

    public SuperRenderController() {
        try {
            ZMQ.Context context = ZMQ.context(1);
            socket = context.socket(ZMQ.PUSH);
            socket.connect("tcp://localhost:" + RENDERER_PORT);
            LOGGER.info("SuperRenderController connected to renderer on port {}", RENDERER_PORT);
        } catch (Exception e) {
            LOGGER.warn("Failed to connect to renderer: {}", e.getMessage());
        }
    }

    /**
     * 设置渲染距离
     */
    public void setRenderDistance(int distance) {
        sendCommand("RENDER_DISTANCE:" + distance);
    }

    /**
     * 设置模拟距离
     */
    public void setSimulationDistance(int distance) {
        sendCommand("SIMULATION_DISTANCE:" + distance);
    }

    /**
     * 设置 FOV 缩放倍数
     */
    public void setFovZoom(double zoom) {
        sendCommand("FOV_ZOOM:" + zoom);
    }

    /**
     * 设置面朝方向渲染距离（/long 命令）
     */
    public void setLookAheadDistance(int distance) {
        sendCommand("LOOK_AHEAD:" + distance);
    }

    /**
     * 发送超渲染指令
     */
    private void sendCommand(String command) {
        if (socket != null) {
            try {
                String fullCommand = "SUPER_RENDER:" + command;
                socket.send(fullCommand.getBytes(), ZMQ.NOBLOCK);
                LOGGER.debug("Sent super render command: {}", fullCommand);
            } catch (Exception e) {
                LOGGER.warn("Failed to send command: {}", e.getMessage());
            }
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (socket != null) {
            socket.close();
        }
    }
}