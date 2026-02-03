package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class EntitySpawner {
    private static final Logger LOGGER = LoggerFactory.getLogger("EntitySpawner");
    
    private final IPCChannel channel;
    private volatile boolean running = true;
    
    public EntitySpawner() {
        this.channel = new IPCChannel(ZMQ.REP, "tcp://*:5558");
    }
    
    public void start() {
        channel.bind();
        LOGGER.info("Entity Spawner started");
        
        while (running) {
            byte[] request = channel.receive();
            if (request != null) {
                processRequest(request);
            }
        }
        
        channel.close();
    }
    
    private void processRequest(byte[] request) {
        try {
            String chunkData = new String(request);
            LOGGER.info("Processing entity spawn for chunk: {}", chunkData);
            
            byte[] response = spawnEntities(chunkData);
            channel.send(response);
        } catch (Exception e) {
            LOGGER.error("Error processing entity spawn", e);
        }
    }
    
    private byte[] spawnEntities(String chunkData) {
        return "ENTITIES_SPAWNED".getBytes();
    }
    
    public void stop() {
        running = false;
    }
    
    public static void main(String[] args) {
        EntitySpawner spawner = new EntitySpawner();
        spawner.start();
    }
}